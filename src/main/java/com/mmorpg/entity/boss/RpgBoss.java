package com.mmorpg.entity.boss;

import com.mmorpg.config.ConfigManager;
import com.mmorpg.config.MobConfig;
import com.mmorpg.entity.RpgMonster;
import com.mmorpg.registry.ModAttachments;
import com.mmorpg.rpg.MobData;
import com.mmorpg.server.Scheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AnimationState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Base des boss : ne disparaissent pas, possedent des phases (selon les PV restants) et des capacites
 * dont les recharges et puissances sont definies dans la configuration (section "abilities").
 * <p>
 * Les boss animes (modeles Blockbench, voir entity/boss/anim) jouent leurs animations ponctuelles via des
 * evenements d'entite ; les effets des capacites sont appliques a l'instant de l'impact de l'animation.
 */
public abstract class RpgBoss extends RpgMonster {
    /** Nombre maximal d'animations par boss. */
    public static final int MAX_ANIMS = 12;
    private static final int EVENT_START = -20;
    private static final int EVENT_STOP = -60;
    private static final EntityDataAccessor<Boolean> ALT_MODE = SynchedEntityData.defineId(RpgBoss.class, EntityDataSerializers.BOOLEAN);

    /**
     * Animations d'un boss (tableaux des classes generees XxxAnims) et role de certaines d'entre elles (-1 si absente) :
     * melee = attaque au corps a corps, death = mort, spawn = jouee a l'apparition.
     */
    protected record AnimSet(int[] lengths, int[] strikes, boolean[] looping, boolean[] hold, int melee, int death, int spawn) {
    }

    private final Map<String, Integer> cooldowns = new HashMap<>();
    protected int phase = 1;
    /** Etats des animations ponctuelles (client), lus par le rendu. */
    public final AnimationState[] animStates = new AnimationState[MAX_ANIMS];
    private int actionEnd;
    private int immobileEnd;
    private boolean spawnAnimPending;

    protected RpgBoss(EntityType<? extends Monster> type, Level level, String key) {
        super(type, level, key);
        this.setPersistenceRequired();
        this.xpReward = 50;
        for (int i = 0; i < MAX_ANIMS; i++) {
            animStates[i] = new AnimationState();
        }
    }

    public static AttributeSupplier.Builder createBossAttributes() {
        return RpgMonster.createAttributes()
                .add(Attributes.MAX_HEALTH, 500)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9)
                .add(Attributes.FOLLOW_RANGE, 48)
                .add(Attributes.ATTACK_DAMAGE, 8);
    }

    /** Animations du boss, ou null s'il n'est pas anime. */
    protected @Nullable AnimSet animSet() {
        return null;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(ALT_MODE, false);
    }

    /** Mode alternatif (ex. furie du Roi gobelin) : change les animations de repos et de marche. */
    public boolean altMode() {
        return this.entityData.get(ALT_MODE);
    }

    protected void setAltMode(boolean alt) {
        this.entityData.set(ALT_MODE, alt);
    }

    protected MobConfig config() {
        return ConfigManager.mob(mobKey());
    }

    protected double ability(String key, double def) {
        MobConfig cfg = config();
        return cfg == null ? def : cfg.ability(key, def);
    }

    /** Vrai si la capacite est prete ; la remet alors en recharge. */
    protected boolean ready(String ability, double defaultTicks) {
        int left = cooldowns.getOrDefault(ability, (int) (defaultTicks * 0.5));
        if (left > 0) {
            cooldowns.put(ability, left - 1);
            return false;
        }
        double base = ability(ability, defaultTicks);
        double mult = phase >= 3 ? 0.6 : phase == 2 ? 0.8 : 1.0;
        cooldowns.put(ability, (int) Math.max(10, base * mult));
        return true;
    }

    // ------------------------------------------------------------------------------------------ animations

    /** Lance une animation chez les clients ; renvoie le delai avant l'impact (ticks). */
    protected int playAnim(int index) {
        AnimSet a = animSet();
        if (a == null || index < 0) return 0;
        this.level().broadcastEntityEvent(this, (byte) (EVENT_START - index));
        if (!a.looping()[index] && !a.hold()[index]) {
            actionEnd = Math.max(actionEnd, this.tickCount + a.lengths()[index]);
        }
        return a.strikes()[index];
    }

    protected void stopAnim(int index) {
        if (animSet() != null && index >= 0) this.level().broadcastEntityEvent(this, (byte) (EVENT_STOP - index));
    }

    /** Lance une animation et applique son effet a l'instant de l'impact, si le boss est encore en vie. */
    protected void playThen(ServerLevel level, int index, Runnable impact) {
        int delay = playAnim(index);
        if (delay <= 0) {
            impact.run();
        } else {
            Scheduler.later(level, delay, () -> {
                if (this.isAlive() && !this.isRemoved()) impact.run();
            });
        }
    }

    /** Joue une animation en boucle pendant une duree donnee, avec un effet a l'instant de l'impact. */
    protected void playFor(ServerLevel level, int index, int ticks, Runnable impact) {
        playThen(level, index, impact);
        actionEnd = Math.max(actionEnd, this.tickCount + ticks);
        Scheduler.later(level, ticks, () -> stopAnim(index));
    }

    /** Vrai pendant une action animee : les autres capacites attendent. */
    protected boolean busy() {
        return this.tickCount < actionEnd;
    }

    /** Le boss reste sur place pendant la duree indiquee. */
    protected void immobile(int ticks) {
        immobileEnd = Math.max(immobileEnd, this.tickCount + ticks);
        actionEnd = Math.max(actionEnd, this.tickCount + ticks);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id <= EVENT_START && id > EVENT_START - MAX_ANIMS) {
            int index = EVENT_START - id;
            AnimSet a = animSet();
            for (int i = 0; i < MAX_ANIMS; i++) {
                if (i != index && a != null && i < a.lengths().length && !a.looping()[i]) animStates[i].stop();
            }
            animStates[index].start(this.tickCount);
        } else if (id <= EVENT_STOP && id > EVENT_STOP - MAX_ANIMS) {
            animStates[EVENT_STOP - id].stop();
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void tick() {
        super.tick();
        AnimSet a = animSet();
        if (a != null && this.level().isClientSide()) {
            boolean acting = false;
            for (int i = 0; i < a.lengths().length; i++) {
                if (!a.looping()[i] && !a.hold()[i] && animStates[i].isStarted()
                        && animStates[i].getTimeInMillis(this.tickCount) > (a.lengths()[i] + 5) * 50L) {
                    animStates[i].stop();
                }
                acting |= !a.looping()[i] && animStates[i].isStarted();
            }
            // pendant une action, le corps suit la tete (tournee vers la cible par le serveur)
            if (acting && this.deathTime == 0) this.yBodyRot = Mth.approachDegrees(this.yBodyRot, this.yHeadRot, 20.0F);
        }
    }

    /** Tourne progressivement le boss vers sa cible (utilise pendant les actions, ou le boss ne se deplace pas). */
    private void faceTarget(LivingEntity target, float maxStep) {
        float yaw = (float) (Mth.atan2(target.getZ() - this.getZ(), target.getX() - this.getX()) * Mth.RAD_TO_DEG) - 90.0F;
        float rot = Mth.approachDegrees(this.getYRot(), yaw, maxStep);
        this.setYRot(rot);
        this.yBodyRot = rot;
        this.yHeadRot = rot;
    }

    @Override
    public @Nullable SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty, EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
        spawnAnimPending = true;
        this.setHomeTo(this.blockPosition(), leashRadius());          // base d'invocation (sauvegardee avec le monde)
        return super.finalizeSpawn(level, difficulty, reason, groupData);
    }

    /** Rayon (blocs) de la base d'invocation dont le boss ne peut pas sortir (config : abilities.leashRadius). */
    protected int leashRadius() {
        return (int) Math.max(6, ability("leashRadius", 12));
    }

    /**
     * Le boss reste dans sa base d'invocation : il ne poursuit pas une cible au-dela (restriction vanilla des
     * attaques de melee), il y revient s'il en est pousse ou s'en est teleporte, et lache une cible trop eloignee.
     */
    private void keepInBase(ServerLevel level) {
        if (!this.hasHome()) this.setHomeTo(this.blockPosition(), leashRadius());      // boss d'avant cette regle
        BlockPos home = this.getHomePosition();
        int r = this.getHomeRadius();
        double dx = this.getX() - (home.getX() + 0.5), dz = this.getZ() - (home.getZ() + 0.5);
        double d2 = dx * dx + dz * dz;
        if (d2 > (r + 6.0) * (r + 6.0) || Math.abs(this.getY() - home.getY()) > 10) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 1, this.getZ(), 40, 0.5, 1, 0.5, 0.1);
            this.teleportTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5);
            this.getNavigation().stop();
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY() + 1, this.getZ(), 40, 0.5, 1, 0.5, 0.1);
        } else if (d2 > (double) r * r) {
            this.getNavigation().moveTo(home.getX() + 0.5, home.getY(), home.getZ() + 0.5, 1.2);
        }
        LivingEntity target = this.getTarget();
        if (target != null && target.distanceToSqr(home.getX() + 0.5, home.getY(), home.getZ() + 0.5) > (r + 14.0) * (r + 14.0)) {
            this.setTarget(null);
        }
    }

    /** Attaque au corps a corps : animation, puis degats a l'impact si la cible est toujours a portee. */
    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        AnimSet a = animSet();
        if (a == null || a.melee() < 0) return super.doHurtTarget(level, target);
        if (busy()) return false;
        playThen(level, a.melee(), () -> {
            if (target.isAlive() && target.distanceTo(this) < this.getBbWidth() / 2 + target.getBbWidth() / 2 + 2.5) {
                super.doHurtTarget(level, target);
            }
        });
        return true;
    }

    /** Mort animee : le corps reste le temps de l'animation (le rendu ne bascule pas le modele). */
    @Override
    protected void tickDeath() {
        AnimSet a = animSet();
        if (a == null || a.death() < 0) {
            super.tickDeath();
            return;
        }
        this.deathTime++;
        if (this.deathTime >= a.lengths()[a.death()] + 10 && !this.level().isClientSide() && !this.isRemoved()) {
            this.level().broadcastEntityEvent(this, (byte) 60);
            this.remove(Entity.RemovalReason.KILLED);
        }
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return false;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        AnimSet a = animSet();
        if (spawnAnimPending && this.tickCount >= 3) {
            spawnAnimPending = false;
            if (a != null && a.spawn() >= 0) {
                playAnim(a.spawn());
                immobile(a.lengths()[a.spawn()]);
            }
        }
        keepInBase(level);
        LivingEntity target = this.getTarget();
        if (target != null && target.isAlive() && busy()) faceTarget(target, 15.0F);
        if (this.tickCount < immobileEnd) {
            this.getNavigation().stop();
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
            return;
        }
        float frac = hpFraction();
        int newPhase = frac > 0.66f ? 1 : frac > 0.33f ? 2 : 3;
        if (newPhase != phase) {
            phase = newPhase;
            MobData d = this.getExistingDataOrNull(ModAttachments.MOB);
            if (d != null) {
                d.phase = phase;
                this.syncData(ModAttachments.MOB);
            }
            onPhaseChange(level, phase);
        }
        if (this.getTarget() != null && this.getTarget().isAlive()) {
            bossTick(level);
        }
    }

    protected abstract void bossTick(ServerLevel level);

    protected void onPhaseChange(ServerLevel level, int phase) {
        Component msg = Component.literal("☠ " + this.getName().getString() + " entre en phase " + phase + " !").withColor(0xFF5050);
        for (var p : level.players()) {
            if (p.distanceToSqr(this) < 64 * 64) p.sendOverlayMessage(msg);
        }
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float multiplier, DamageSource source) {
        return false;
    }

    @Override
    public boolean canUsePortal(boolean allowPassengers) {
        return false;
    }
}
