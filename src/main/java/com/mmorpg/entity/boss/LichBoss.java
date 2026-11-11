package com.mmorpg.entity.boss;

import com.mmorpg.combat.RpgCombat;
import com.mmorpg.entity.MobAbilities;
import com.mmorpg.entity.boss.anim.LicheAncienneAnims;
import com.mmorpg.item.ItemDefs;
import com.mmorpg.registry.ModAttachments;
import com.mmorpg.registry.ModEntities;
import com.mmorpg.rpg.MobData;
import com.mmorpg.entity.FxKind;
import com.mmorpg.server.Scheduler;
import net.minecraft.core.particles.ParticleTypes;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Liche Ancienne : sorciere morte-vivante qui bombarde d'ombres, se teleporte et draine la vie. */
public class LichBoss extends RpgBoss {
    public LichBoss(EntityType<? extends Monster> type, Level level) {
        super(type, level, "liche_ancienne");
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    private static final AnimSet ANIMS = new AnimSet(LicheAncienneAnims.LENGTH_TICKS, LicheAncienneAnims.STRIKE_TICKS, LicheAncienneAnims.LOOPING,
            LicheAncienneAnims.HOLD, -1, LicheAncienneAnims.MORT, -1);

    @Override
    protected AnimSet animSet() {
        return ANIMS;
    }

    private static final int COLOR = 0x9060E0;

    /** La liche combat a distance : elle se tient a une dizaine de blocs et s'eclipse si on l'approche. */
    @Override
    protected double preferredDistance() {
        return 10;
    }

    @Override
    protected void bossTick(ServerLevel level) {
        LivingEntity target = this.getTarget();
        if (!busy() && this.distanceTo(target) < 4 && ready("blinkCooldown", 160)) {
            Vec3 away = this.position().subtract(target.position()).multiply(1, 0, 1);
            away = (away.lengthSqr() < 1e-4 ? facing() : away.normalize()).scale(10);
            Vec3 dest = this.position().add(away);
            if (!inBase(dest, 1.5)) dest = randomSpot(8);
            level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 1, this.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
            this.randomTeleport(dest.x, this.getY(), dest.z, true, state -> false);
            playAnim(LicheAncienneAnims.RECUL);
        }
    }

    /** Six attaques : projectiles d'ombre, invocation, drain, frappe de baton, pluie d'ames, rayon. */
    @Override
    protected void defineMoves(List<Move> out) {
        move(out, "bolt", LicheAncienneAnims.PROJECTILE, 45, 1, 0, 40, level -> playThen(level, LicheAncienneAnims.PROJECTILE, () -> {
            LivingEntity target = this.getTarget();
            if (target == null || !target.isAlive()) return;
            int bolts = phase >= 2 ? 3 : 1;
            for (int i = 0; i < bolts; i++) {
                MobAbilities.bolt(this, target, ItemDefs.Element.OMBRE, ability("boltDamage", 0.9), 1.0F + i * 0.15F, 0).homing(target);
            }
            level.playSound(null, this.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.5f, 0.8f);
        }));
        move(out, "summon", LicheAncienneAnims.INVOCATION, 500, 1, 0, 40, level -> {
            level.playSound(null, this.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 2f, 0.8f);
            playThen(level, LicheAncienneAnims.INVOCATION, () -> {
                int alive = level.getEntitiesOfClass(com.mmorpg.entity.mob.CursedSkeletonEntity.class, this.getBoundingBox().inflate(30)).size();
                if (alive < 6) MobAbilities.summon(this, ModEntities.CURSED_SKELETON.get(), (int) ability("summonCount", 2) + phase - 1, "squelette_maudit", -10);
            });
        });
        move(out, "drain", LicheAncienneAnims.CANALISATION, 300, 2, 0, 9, level -> {
            telegraph(level, this.position(), 8, 0x60E090, 40);
            warn(level, "⚠ Drain de vie : sortez du cercle !");
            playFor(level, LicheAncienneAnims.CANALISATION, 40, () -> { });
            immobile(40);
            for (int t = 10; t <= 40; t += 10) {
                Scheduler.later(level, t, () -> {
                    if (!this.isAlive()) return;
                    int n = hitCircle(this.position(), 8, 0.3, e -> level.sendParticles(ParticleTypes.SCULK_SOUL, e.getX(), e.getY() + 1, e.getZ(), 10, 0.3, 0.5, 0.3, 0.05));
                    MobData d = this.getExistingDataOrNull(ModAttachments.MOB);
                    if (d != null && n > 0) {
                        d.hp = (float) Math.min(d.maxHp, d.hp + RpgCombat.mobAtk(this) * 0.6 * n);
                        this.syncData(ModAttachments.MOB);
                    }
                    level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 2f, 1f);
                });
            }
        });
        move(out, "baton", LicheAncienneAnims.FRAPPE_BATON, 140, 1, 0, 6.5, level -> {
            telegraph(level, this.position(), 6, COLOR, LicheAncienneAnims.STRIKE_TICKS[LicheAncienneAnims.FRAPPE_BATON]);
            playThen(level, LicheAncienneAnims.FRAPPE_BATON, () -> {
                fx(level, FxKind.NOVA, this.position(), 6, COLOR, 16);
                level.playSound(null, this.blockPosition(), SoundEvents.WITHER_BREAK_BLOCK, SoundSource.HOSTILE, 1.2f, 1.3f);
                hitCircle(this.position(), 6, ability("batonDamage", 1.3), e -> {
                    knock(e, 1.2, 0.4);
                    e.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, 40, 1));
                });
            });
        });
        move(out, "pluie", LicheAncienneAnims.PLUIE_AMES, 240, 1, 0, 40, level -> {
            warn(level, "⚠ Pluie d'âmes : quittez les cercles !");
            playThen(level, LicheAncienneAnims.PLUIE_AMES, () ->
                    rain(level, 2 + phase, 3.2, 24, ability("pluieDamage", 1.4), COLOR, FxKind.BURST, ParticleTypes.SCULK_SOUL, null));
        });
        move(out, "rayon", LicheAncienneAnims.RAYON, 280, 2, 4, 30, level -> {
            telegraphLine(level, dirTo(aim()), 26, COLOR, LicheAncienneAnims.STRIKE_TICKS[LicheAncienneAnims.RAYON]);
            warn(level, "⚠ Rayon funeste : passez derrière elle !");
            immobile(LicheAncienneAnims.LENGTH_TICKS[LicheAncienneAnims.RAYON]);
            playThen(level, LicheAncienneAnims.RAYON, () -> {
                level.playSound(null, this.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.HOSTILE, 2f, 0.6f);
                beam(level, 30, 26, 1.3, ability("rayonDamage", 0.55), COLOR, ParticleTypes.SOUL_FIRE_FLAME);
            });
        });
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.level().addParticle(ParticleTypes.SCULK_SOUL, this.getRandomX(0.7), this.getRandomY(), this.getRandomZ(0.7), 0, 0.02, 0);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITHER_SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }
}
