package com.mmorpg.entity.mob;

import com.mmorpg.combat.RpgCombat;
import com.mmorpg.entity.FxKind;
import com.mmorpg.entity.RpgMonster;
import com.mmorpg.entity.SkillFxEntity;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

/**
 * IA commune a tous les monstres RPG (3D importes et bestiaire classique).
 * <ul>
 * <li>Chaque monstre a ses attaques ({@link ImportedSpells#TABLE}) ; chacune joue son propre clip, puis son effet tombe a l'impact du geste.
 * Les attaques de zone sont annoncees par un cercle au sol pendant l'anticipation.</li>
 * <li>Trois comportements : brute (va au contact), tireur (garde ses distances, se decale, recule si on l'approche),
 * bondisseur (tourne autour de sa proie entre deux assauts).</li>
 * <li>Sous 30 % de vie le monstre entre en frenesie : il attaque plus souvent.</li>
 * </ul>
 */
public abstract class SmartMonster extends RpgMonster {
    private static final byte EVENT_CLIP = 80;
    /** Client : clip d'attaque en cours (indice de {@link ImportedSpells#CLIPS}) et tick de depart. */
    public int clip = -1, clipStarted = -100;

    private long[] nextCast;
    private long globalCooldown;
    private ImportedSpells.Spell pending;
    private int pendingAt, lockedUntil, circleUntil, circleSide = 1;
    private boolean frenzy;

    protected SmartMonster(EntityType<? extends Monster> type, Level level, String key) {
        super(type, level, key);
    }

    protected List<ImportedSpells.Spell> spells() {
        return ImportedSpells.of(mobKey());
    }

    public boolean rangedRole() {
        return ImportedSpells.rangedRole(mobKey());
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new HoldGoal());
        this.goalSelector.addGoal(2, new KiteGoal());
        this.goalSelector.addGoal(2, new CircleGoal());
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.15, false));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 0.85));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ------------------------------------------------------------------ clips

    protected void playClip(int index) {
        this.level().broadcastEntityEvent(this, (byte) (EVENT_CLIP + index));
    }

    @Override
    public void handleEntityEvent(byte event) {
        if (event >= EVENT_CLIP && event < EVENT_CLIP + ImportedSpells.CLIPS.length) {
            clip = event - EVENT_CLIP;
            clipStarted = tickCount;
        } else {
            super.handleEntityEvent(event);
        }
    }

    /** Vrai pendant l'anticipation d'une attaque et juste apres : le monstre ne bouge pas et ne frappe pas autrement. */
    public boolean acting() {
        return pending != null || this.tickCount < lockedUntil;
    }

    /** Coup de base au corps a corps : clip attack, puis degats vanilla (convertis par RpgCombat). */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        if (acting()) return false;
        playClip(ImportedSpells.ATTACK);
        boolean hit = super.doHurtTarget(level, target);
        if (hit && ImportedSpells.skirmisher(mobKey()) && this.random.nextInt(3) != 0) {
            circleUntil = this.tickCount + 25 + this.random.nextInt(25);
            circleSide = this.random.nextBoolean() ? 1 : -1;
        }
        return hit;
    }

    // ------------------------------------------------------------------ attaques

    @Override
    public void tick() {
        super.tick();
        if (!this.level().isClientSide()) attackTick();
    }

    protected double range(ImportedSpells.Spell s) {
        return switch (s.type()) {
            case BOLT, VOLLEY, CHARGE, POUNCE -> ImportedSpells.param(mobKey(), s, "Range", s.range());
            case SLAM, NOVA, CLOUD, ROOTS, CLEAVE -> ImportedSpells.param(mobKey(), s, "Radius", s.radius());
            default -> s.range();
        };
    }

    private boolean usable(ImportedSpells.Spell s, LivingEntity target, double dist) {
        double range = range(s);
        double reach = this.getBbWidth() / 2 + target.getBbWidth() / 2;
        return switch (s.type()) {
            case BOLT, VOLLEY -> dist <= range && dist >= 2 && this.hasLineOfSight(target);
            case SLAM, NOVA, CLOUD, ROOTS -> dist <= range * 0.85 + reach * 0.5;
            case CLEAVE -> dist <= range * 0.8 + reach;
            case COMBO -> dist <= reach + 2.2;
            case CHARGE, POUNCE -> dist >= 4 && dist <= range && this.hasLineOfSight(target);
            case BLINK -> dist >= 5 && dist <= 14;
            case RETREAT -> dist <= range && this.onGround();
            case SUMMON -> dist <= 16 && this.hasLineOfSight(target);
            case ROAR -> dist <= range;
        };
    }

    private void attackTick() {
        List<ImportedSpells.Spell> list = spells();
        if (list.isEmpty() || !this.isAlive()) return;
        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive() || !RpgCombat.isEnemy(this, target)) {
            pending = null;
            return;
        }
        if (!frenzy && hpFraction() < 0.3F) {
            frenzy = true;
            this.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 60, 0));
            if (this.level() instanceof ServerLevel sl) sl.sendParticles(ParticleTypes.ANGRY_VILLAGER, this.getX(), this.getY() + this.getBbHeight() + 0.2, this.getZ(), 5, 0.3, 0.2, 0.3, 0);
        }
        if (pending != null) {
            this.getNavigation().stop();
            this.getLookControl().setLookAt(target, 60F, 60F);
            if (this.tickCount >= pendingAt) {
                ImportedSpells.Spell s = pending;
                pending = null;
                ImportedSpells.cast(this, s, target);
            }
            return;
        }
        if (this.tickCount < globalCooldown) return;
        if (nextCast == null || nextCast.length != list.size()) {
            nextCast = new long[list.size()];
            for (int i = 0; i < nextCast.length; i++) nextCast[i] = this.tickCount + 20 + this.random.nextInt(50);
        }
        double dist = this.distanceTo(target);
        List<Integer> ready = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            if (this.tickCount >= nextCast[i] && usable(list.get(i), target, dist)) ready.add(i);
        }
        if (ready.isEmpty()) return;
        int i = ready.get(this.random.nextInt(ready.size()));
        ImportedSpells.Spell s = list.get(i);
        double cd = ImportedSpells.param(mobKey(), s, "Cooldown", s.cooldown()) * (frenzy ? 0.7 : 1.0);
        nextCast[i] = this.tickCount + (long) (cd * (0.85 + this.random.nextDouble() * 0.3));
        int windup = s.windup();
        globalCooldown = this.tickCount + windup + (frenzy ? 16 : 26);
        pending = s;
        pendingAt = this.tickCount + windup;
        lockedUntil = pendingAt + 6;
        this.getNavigation().stop();
        this.getLookControl().setLookAt(target, 60F, 60F);
        playClip(s.clip());
        if (this.level() instanceof ServerLevel sl && windup >= 10) {
            switch (s.type()) {
                case SLAM, NOVA, CLOUD, ROOTS -> sl.addFreshEntity(SkillFxEntity.create(sl, FxKind.CIRCLE, this.position(),
                        ImportedSpells.param(mobKey(), s, "Radius", s.radius()), ImportedSpells.color(s.element()), windup).follow(this));
                default -> { }
            }
        }
    }

    /** Lance tout de suite une attaque (tests de la vitrine). */
    public void forceCast(ImportedSpells.Spell s) {
        pending = s;
        pendingAt = this.tickCount + s.windup();
        lockedUntil = pendingAt + 6;
        playClip(s.clip());
    }

    // ------------------------------------------------------------------ deplacements

    /** Pendant une attaque, le monstre reste sur place, tourne vers sa cible. */
    private final class HoldGoal extends Goal {
        HoldGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            return pending != null && pending.type() != ImportedSpells.Type.RETREAT;
        }

        @Override
        public void tick() {
            SmartMonster.this.getNavigation().stop();
            LivingEntity t = SmartMonster.this.getTarget();
            if (t != null) SmartMonster.this.getLookControl().setLookAt(t, 60F, 60F);
        }
    }

    /** Tireurs : restent entre 6 blocs et leur portee, se decalent de cote, s'eloignent si la cible approche. */
    private final class KiteGoal extends Goal {
        private int side = 1, switchAt, repath;

        KiteGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = SmartMonster.this.getTarget();
            return rangedRole() && t != null && t.isAlive();
        }

        @Override
        public void stop() {
            SmartMonster.this.getNavigation().stop();
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void tick() {
            SmartMonster m = SmartMonster.this;
            LivingEntity t = m.getTarget();
            if (t == null) return;
            m.getLookControl().setLookAt(t, 30F, 30F);
            double d = m.distanceTo(t);
            double max = Math.max(8, range(spells().get(0)) * 0.8);
            boolean sees = m.hasLineOfSight(t);
            if (d > max || !sees) {
                if (--repath <= 0) {
                    repath = 10;
                    m.getNavigation().moveTo(t, 1.1);
                }
                return;
            }
            if (d < 5.5) {
                if (--repath <= 0 || m.getNavigation().isDone()) {
                    repath = 12;
                    Vec3 away = DefaultRandomPos.getPosAway(m, 10, 4, t.position());
                    if (away != null) m.getNavigation().moveTo(away.x, away.y, away.z, 1.25);
                }
                return;
            }
            m.getNavigation().stop();
            if (m.tickCount >= switchAt) {
                switchAt = m.tickCount + 30 + m.getRandom().nextInt(50);
                side = m.getRandom().nextInt(4) == 0 ? 0 : -side;
            }
            m.getMoveControl().strafe(d < 7.5 ? -0.25F : 0F, side * 0.4F);
            m.setYRot(m.getYHeadRot());
        }
    }

    /** Bondisseurs : apres un coup, tournent quelques instants autour de la cible avant de revenir. */
    private final class CircleGoal extends Goal {
        CircleGoal() {
            this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
        }

        @Override
        public boolean canUse() {
            LivingEntity t = SmartMonster.this.getTarget();
            return SmartMonster.this.tickCount < circleUntil && t != null && t.isAlive() && SmartMonster.this.distanceTo(t) < 9;
        }

        @Override
        public boolean requiresUpdateEveryTick() {
            return true;
        }

        @Override
        public void start() {
            SmartMonster.this.getNavigation().stop();
        }

        @Override
        public void tick() {
            SmartMonster m = SmartMonster.this;
            LivingEntity t = m.getTarget();
            if (t == null) return;
            m.getLookControl().setLookAt(t, 40F, 40F);
            double d = m.distanceTo(t);
            m.getMoveControl().strafe(d < 3.5 ? -0.5F : d > 5.5 ? 0.4F : 0F, circleSide * 0.6F);
            m.setYRot(m.getYHeadRot());
        }
    }
}
