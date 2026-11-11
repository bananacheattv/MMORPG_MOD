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
import net.minecraft.server.level.ServerPlayer;
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

import com.mmorpg.combat.RpgCombat;
import com.mmorpg.entity.FxKind;
import com.mmorpg.entity.SkillFxEntity;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Base des boss : ne disparaissent pas, possedent des phases (selon les PV restants) et des capacites
 * dont les recharges et puissances sont definies dans la configuration (section "abilities").
 * <p>
 * Les boss animes (modeles Blockbench, voir entity/boss/anim) jouent leurs animations ponctuelles via des
 * evenements d'entite ; les effets des capacites sont appliques a l'instant de l'impact de l'animation.
 */
public abstract class RpgBoss extends RpgMonster {
    /** Nombre maximal d'animations par boss. */
    public static final int MAX_ANIMS = 16;
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
    /** Client : clip en cours des boss sans jeu d'animations propre (gardiens de guilde), indice de ImportedSpells.CLIPS. */
    public int clip = -1, clipStarted = -100;
    private static final byte EVENT_CLIP = 80;
    private final BossFury.State fury=new BossFury.State();
    /** Base de la puissance offensive du boss (surchargee par les gardiens de guilde, deja tres puissants). */
    protected double baseDamage(){return 1.5;}
    /** Base de la robustesse du boss : les degats recus sont divises par ce facteur. */
    protected double baseToughness(){return 1.8;}
    public double damageMultiplier(){return BossFury.damageMultiplier(this,fury,phase,baseDamage());}
    public double toughness(){return BossFury.toughness(fury,baseToughness())*(exposed()?0.5:1.0);}
    @Override public void die(DamageSource source){if(level() instanceof ServerLevel sl)BossFury.discardAdds(sl,fury);super.die(source);}
    public int combatPhase(){return phase;}
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
        int left = cooldowns.getOrDefault(ability, (int) (defaultTicks * 0.35));
        if (left > 0) {
            cooldowns.put(ability, left - 1);
            return false;
        }
        double base = ability(ability, defaultTicks);
        double mult = phase >= 4 ? 0.42 : phase == 3 ? 0.55 : phase == 2 ? 0.7 : 0.85;
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
        if (id >= EVENT_CLIP && id < EVENT_CLIP + 5) {
            clip = id - EVENT_CLIP;
            clipStarted = tickCount;
            return;
        }
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
    protected void faceTarget(LivingEntity target, float maxStep) {
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
        // coince dans un bloc (autel, pilier...) : on le replace au plus proche point libre de sa base
        if (this.tickCount % 20 == 0 && !level.noCollision(this, this.getBoundingBox().deflate(0.02))) {
            var spot = BossManager.freeSpot(level, home, this.getBbWidth(), this.getBbHeight());
            this.teleportTo(spot.x, spot.y, spot.z);
            this.getNavigation().stop();
            this.setHomeTo(BlockPos.containing(spot), leashRadius());
            home = this.getHomePosition();
        }
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
            com.mmorpg.combat.ThreatGoal.clear(this);
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
        // cible persistante : on garde la cible tant qu'elle est valide, sinon le joueur le plus proche (sans ligne de vue exigee)
        LivingEntity cur = getTarget();
        if (tickCount % 5 == 0 && (cur == null || !cur.isAlive() || !com.mmorpg.combat.RpgCombat.isEnemy(this, cur) || distanceToSqr(cur) > 64 * 64
                || (cur instanceof ServerPlayer sp && (sp.isCreative() || sp.isSpectator())))) {
            var nearest = level.players().stream().filter(p -> p.isAlive() && !p.isCreative() && !p.isSpectator()
                    && com.mmorpg.combat.RpgCombat.isEnemy(this, p) && distanceToSqr(p) < 56 * 56)
                    .min(java.util.Comparator.comparingDouble(this::distanceToSqr));
            setTarget(nearest.orElse(null));
        }
        keepInBase(level);
        LivingEntity target = this.getTarget();
        // poursuite active : si la cible s'eloigne et que le boss est libre, il la rejoint
        if (dashLeft > 0) {
            dashTick(level);
            return;
        }
        if (target != null && target.isAlive() && !busy() && this.tickCount >= immobileEnd && this.tickCount % 5 == 0) {
            double pd = preferredDistance(), d = distanceTo(target);
            if (pd <= 0) {
                if (d > 2.5) this.getNavigation().moveTo(target, 1.3 + 0.1 * Math.min(4, phase));      // poursuite constante, de plus en plus rapide
            } else if (d > pd + 3 || !this.hasLineOfSight(target)) {
                this.getNavigation().moveTo(target, 1.1);
            } else if (d < pd - 4) {
                Vec3 away = DefaultRandomPos.getPosAway(this, 10, 4, target.position());
                if (away != null && inBase(away, 1.5)) this.getNavigation().moveTo(away.x, away.y, away.z, 1.2);
            } else {
                this.getNavigation().stop();
                this.getLookControl().setLookAt(target, 30F, 30F);
            }
        }
        if (target != null && target.isAlive() && busy()) faceTarget(target, this.tickCount < channelEnd ? channelTurn : 15.0F);
        if (this.tickCount < channelEnd && (channelEnd - this.tickCount) % channelEvery == 0 && channelPulse != null) channelPulse.run();
        if (this.tickCount < immobileEnd) {
            this.getNavigation().stop();
            this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
            return;
        }
        float frac = hpFraction();
        int newPhase = frac > 0.75f ? 1 : frac > 0.5f ? 2 : frac > 0.25f ? 3 : 4;
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
            BossFury.tick(this, level, phase, fury);
            bossTick(level);
            if (!busy()) pickMove(level);
        }
    }

    /** Mecaniques permanentes du boss (auras, reactions) ; ses attaques sont declarees dans {@link #defineMoves}. */
    protected void bossTick(ServerLevel level) {
    }

    // ------------------------------------------------------------------------------------------ attaques

    /**
     * Une attaque du boss : identifiant (cle de configuration {@code <id>Cooldown}), animation jouee, recharge (ticks), phase minimale,
     * distance mini/maxi de la cible pour la declencher, et son deroulement.
     */
    public record Move(String id, int anim, int cooldown, int minPhase, double minDist, double maxDist, Consumer<ServerLevel> run) {
    }

    /** Nombre de fois ou chaque attaque a ete lancee (cle « boss/attaque ») : lu par les controles. */
    public static final Map<String, Integer> MOVE_LOG = new java.util.concurrent.ConcurrentHashMap<>();
    private List<Move> moves;
    private long[] moveReady;
    private long nextMove = -1;
    private String lastMove = "";
    private int exposedUntil, channelEnd, channelEvery = 1;
    private float channelTurn;
    private @Nullable Runnable channelPulse;
    private int dashLeft;
    private boolean freeMove;
    private Vec3 dashDir = Vec3.ZERO;
    private double dashSpeed, dashMult;
    private final Set<UUID> dashHit = new HashSet<>();

    /** Declare les attaques du boss (au moins cinq, chacune avec sa propre animation). */
    protected void defineMoves(List<Move> out) {
    }

    public List<Move> moves() {
        if (moves == null) {
            List<Move> list = new ArrayList<>();
            defineMoves(list);
            moves = List.copyOf(list);
        }
        return moves;
    }

    /** Distance a laquelle le boss se tient de sa cible (0 = au contact). */
    protected double preferredDistance() {
        return 0;
    }

    /** Choisit une attaque prete et adaptee a la distance, jamais deux fois la meme de suite si une autre est possible. */
    private void pickMove(ServerLevel level) {
        List<Move> list = moves();
        LivingEntity target = this.getTarget();
        if (list.isEmpty() || target == null) return;
        if (moveReady == null) {
            moveReady = new long[list.size()];
            for (int i = 0; i < moveReady.length; i++) moveReady[i] = this.tickCount + this.random.nextInt(Math.max(1, list.get(i).cooldown() / 3));
            nextMove = this.tickCount + 30;
        }
        if (this.tickCount < nextMove) return;
        double dist = this.distanceTo(target);
        boolean sees = this.hasLineOfSight(target);
        List<Integer> ok = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Move m = list.get(i);
            if (this.tickCount >= moveReady[i] && phase >= m.minPhase() && dist >= m.minDist() && dist <= m.maxDist() && (sees || m.maxDist() <= 10)) ok.add(i);
        }
        if (ok.size() > 1) ok.removeIf(i -> list.get(i).id().equals(lastMove));
        if (ok.isEmpty()) return;
        launch(level, ok.get(this.random.nextInt(ok.size())));
    }

    private void launch(ServerLevel level, int index) {
        Move m = moves().get(index);
        double mult = phase >= 4 ? 0.5 : phase == 3 ? 0.62 : phase == 2 ? 0.78 : 1.0;
        if (moveReady == null) moveReady = new long[moves().size()];
        moveReady[index] = this.tickCount + (long) Math.max(20, ability(m.id() + "Cooldown", m.cooldown()) * mult);
        lastMove = m.id();
        MOVE_LOG.merge(mobKey() + "/" + m.id(), 1, Integer::sum);
        this.getNavigation().stop();
        freeMove = false;
        m.run().accept(level);
        if (!freeMove) immobileEnd = Math.max(immobileEnd, actionEnd);      // le boss ne glisse pas pendant son geste
        nextMove = Math.max(actionEnd, this.tickCount) + (phase >= 4 ? 10 : phase == 3 ? 16 : phase == 2 ? 24 : 32);
    }

    /** Lance tout de suite une attaque (controles et vitrine). */
    public boolean forceMove(ServerLevel level, String id) {
        List<Move> list = moves();
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).id().equals(id)) {
                actionEnd = 0;
                immobileEnd = 0;
                channelEnd = 0;
                dashLeft = 0;
                launch(level, i);
                return true;
            }
        }
        return false;
    }

    /** A appeler dans une attaque pendant laquelle le boss doit pouvoir se deplacer (bond, tourbillon). */
    protected void freeMove() {
        freeMove = true;
    }

    /** Ajoute une attaque a la liste (raccourci pour {@link #defineMoves}). */
    protected static void move(List<Move> out, String id, int anim, int cooldown, int minPhase, double minDist, double maxDist, Consumer<ServerLevel> run) {
        out.add(new Move(id, anim, cooldown, minPhase, minDist, maxDist, run));
    }

    /** Vrai si l'entite peut etre touchee par les attaques du boss. */
    protected boolean canHit(LivingEntity e) {
        return e != this && e.isAlive() && RpgCombat.isEnemy(this, e);
    }

    protected List<LivingEntity> victims(Vec3 center, double radius) {
        AABB box = new AABB(center.x - radius, center.y - 6, center.z - radius, center.x + radius, center.y + 6, center.z + radius);
        return this.level().getEntitiesOfClass(LivingEntity.class, box, e -> canHit(e) && Math.hypot(e.getX() - center.x, e.getZ() - center.z) <= radius + e.getBbWidth() / 2);
    }

    protected List<ServerPlayer> foes(ServerLevel level, double range) {
        return level.players().stream().filter(p -> !p.isCreative() && !p.isSpectator() && canHit(p) && distanceToSqr(p) < range * range).toList();
    }

    /** Nombre de coups portes par les attaques du boss (controles). */
    public int hitsLanded;

    protected int hit(List<LivingEntity> list, double mult, @Nullable Consumer<LivingEntity> extra) {
        hitsLanded += list.size();
        for (LivingEntity e : list) {
            RpgCombat.mobHit(this, e, mult);
            if (extra != null) extra.accept(e);
        }
        return list.size();
    }

    protected int hitCircle(Vec3 center, double radius, double mult, @Nullable Consumer<LivingEntity> extra) {
        return hit(victims(center, radius), mult, extra);
    }

    protected int hitRing(Vec3 center, double inner, double outer, double mult, @Nullable Consumer<LivingEntity> extra) {
        List<LivingEntity> list = victims(center, outer);
        list.removeIf(e -> Math.hypot(e.getX() - center.x, e.getZ() - center.z) < inner - e.getBbWidth() / 2);
        return hit(list, mult, extra);
    }

    /** Cone devant le boss : direction horizontale, portee, demi-angle en degres. */
    protected int hitCone(Vec3 dir, double range, double halfAngle, double mult, @Nullable Consumer<LivingEntity> extra) {
        List<LivingEntity> list = victims(this.position(), range);
        double cos = Math.cos(Math.toRadians(halfAngle));
        list.removeIf(e -> {
            Vec3 to = new Vec3(e.getX() - this.getX(), 0, e.getZ() - this.getZ());
            return to.lengthSqr() > 2.25 && to.normalize().dot(dir) < cos;
        });
        return hit(list, mult, extra);
    }

    /** Bande droite partant d'un point : longueur et demi-largeur. */
    protected int hitLine(Vec3 from, Vec3 dir, double length, double halfWidth, double mult, @Nullable Consumer<LivingEntity> extra) {
        Vec3 mid = from.add(dir.scale(length / 2));
        List<LivingEntity> list = victims(mid, length / 2 + halfWidth);
        list.removeIf(e -> {
            double rx = e.getX() - from.x, rz = e.getZ() - from.z;
            double along = rx * dir.x + rz * dir.z, side = Math.abs(rx * dir.z - rz * dir.x);
            return along < -0.5 || along > length || side > halfWidth + e.getBbWidth() / 2;
        });
        return hit(list, mult, extra);
    }

    /** Direction horizontale dans laquelle le corps du boss est tourne. */
    protected Vec3 facing() {
        float yaw = this.getYRot() * Mth.DEG_TO_RAD;
        return new Vec3(-Mth.sin(yaw), 0, Mth.cos(yaw));
    }

    /** Position visee : celle de la cible, a defaut un point devant le boss. */
    protected Vec3 aim() {
        LivingEntity t = this.getTarget();
        return t != null ? t.position() : this.position().add(facing().scale(6));
    }

    protected Vec3 dirTo(Vec3 point) {
        Vec3 d = new Vec3(point.x - this.getX(), 0, point.z - this.getZ());
        return d.lengthSqr() < 1e-4 ? facing() : d.normalize();
    }

    protected void fx(ServerLevel level, FxKind kind, Vec3 pos, double radius, int color, int life) {
        level.addFreshEntity(SkillFxEntity.create(level, kind, pos, radius, color, life));
    }

    /** Cercle d'annonce au sol. */
    protected void telegraph(ServerLevel level, Vec3 pos, double radius, int color, int ticks) {
        fx(level, FxKind.CIRCLE, pos, radius, color, ticks);
    }

    /** Annonce d'un cone : ses deux bords et son arc, redessines toutes les 4 ticks. */
    protected void telegraphCone(ServerLevel level, double range, double halfAngle, int ticks, ParticleOptions particle) {
        for (int t = 0; t < ticks; t += 4) {
            Scheduler.later(level, t, () -> {
                if (!this.isAlive()) return;
                Vec3 dir = facing();
                double base = Math.atan2(dir.z, dir.x), half = Math.toRadians(halfAngle);
                for (double r = 1.5; r <= range; r += 1.0) {
                    for (int side = -1; side <= 1; side += 2) {
                        level.sendParticles(particle, this.getX() + Math.cos(base + side * half) * r, this.getY() + 0.25, this.getZ() + Math.sin(base + side * half) * r, 1, 0, 0, 0, 0);
                    }
                }
                for (double a = -half; a <= half; a += 0.12) {
                    level.sendParticles(particle, this.getX() + Math.cos(base + a) * range, this.getY() + 0.25, this.getZ() + Math.sin(base + a) * range, 1, 0, 0, 0, 0);
                }
            });
        }
    }

    /** Annonce d'une ligne (charge, estoc) : trait lumineux au sol. */
    protected void telegraphLine(ServerLevel level, Vec3 dir, double length, int color, int ticks) {
        Vec3 from = this.position().add(0, 0.25, 0);
        level.addFreshEntity(SkillFxEntity.create(level, FxKind.BEAM, from, 0.35, color, ticks).points(List.of(from, from.add(dir.scale(length)))));
    }

    protected void warn(ServerLevel level, String text) {
        Component msg = Component.literal(text).withColor(0xFF8050);
        for (ServerPlayer p : foes(level, 48)) p.sendOverlayMessage(msg);
    }

    protected boolean inBase(Vec3 point, double margin) {
        if (!this.hasHome()) return true;
        BlockPos home = this.getHomePosition();
        return Math.hypot(point.x - (home.getX() + 0.5), point.z - (home.getZ() + 0.5)) <= this.getHomeRadius() - margin;
    }

    /** Point au hasard dans la base du boss, a hauteur du boss. */
    protected Vec3 randomSpot(double minFromBoss) {
        Vec3 home = this.hasHome() ? Vec3.atBottomCenterOf(this.getHomePosition()) : this.position();
        double max = Math.max(5, this.hasHome() ? this.getHomeRadius() - 2 : 10);
        for (int i = 0; i < 6; i++) {
            double a = this.random.nextDouble() * Math.PI * 2, r = 2 + this.random.nextDouble() * (max - 2);
            Vec3 p = new Vec3(home.x + Math.cos(a) * r, this.getY(), home.z + Math.sin(a) * r);
            if (p.distanceTo(this.position()) >= minFromBoss) return p;
        }
        return new Vec3(home.x, this.getY(), home.z);
    }

    /**
     * Pluie de zones annoncees : un cercle sous chaque joueur plus des cercles au hasard dans la base ; tout tombe apres le delai.
     */
    protected void rain(ServerLevel level, int extra, double radius, int delay, double mult, int color, FxKind impactFx, ParticleOptions particle,
                        @Nullable Consumer<LivingEntity> onHit) {
        List<Vec3> spots = new ArrayList<>();
        for (ServerPlayer p : foes(level, 40)) spots.add(p.position());
        for (int i = 0; i < extra; i++) spots.add(randomSpot(3));
        for (Vec3 c : spots) telegraph(level, c, radius, color, delay);
        Scheduler.later(level, delay, () -> {
            if (!this.isAlive() || this.isRemoved()) return;
            Set<LivingEntity> done = new HashSet<>();
            for (Vec3 c : spots) {
                fx(level, impactFx, c, radius, color, 14);
                level.sendParticles(particle, c.x, c.y + 0.4, c.z, 16, radius * 0.35, 0.3, radius * 0.35, 0.03);
                for (LivingEntity e : victims(c, radius)) {
                    if (!done.add(e)) continue;
                    hitsLanded++;
                    RpgCombat.mobHit(this, e, mult);
                    if (onHit != null) onHit.accept(e);
                }
            }
            level.playSound(null, this.blockPosition(), net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(), net.minecraft.sounds.SoundSource.HOSTILE, 1.2f, 0.9f);
        });
    }

    /** Projette une cible a l'oppose du boss. */
    protected void knock(LivingEntity e, double strength, double up) {
        if (!com.mmorpg.combat.CrowdControl.allow(e)) return;
        Vec3 push = new Vec3(e.getX() - this.getX(), 0, e.getZ() - this.getZ());
        push = push.lengthSqr() < 1e-4 ? facing() : push.normalize();
        e.push(push.x * strength, up, push.z * strength);
        e.needsSync = true;
    }

    /** Ruee en ligne droite : touche une fois chaque ennemi croise, s'arrete au bord de la base. */
    protected void dash(Vec3 dir, int ticks, double speed, double mult) {
        dashDir = dir;
        dashLeft = ticks;
        dashSpeed = speed;
        dashMult = mult;
        dashHit.clear();
        immobileEnd = this.tickCount;
        actionEnd = Math.max(actionEnd, this.tickCount + ticks + 4);
    }

    private void dashTick(ServerLevel level) {
        dashLeft--;
        this.getNavigation().stop();
        if (this.hasHome()) {
            BlockPos home = this.getHomePosition();
            double dx = this.getX() - (home.getX() + 0.5), dz = this.getZ() - (home.getZ() + 0.5);
            if (dx * dx + dz * dz > Math.pow(this.getHomeRadius() - 1.5, 2) && dx * dashDir.x + dz * dashDir.z > 0) {
                dashLeft = 0;
                this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
                return;
            }
        }
        this.setDeltaMovement(dashDir.x * dashSpeed, this.getDeltaMovement().y, dashDir.z * dashSpeed);
        float yaw = (float) (Mth.atan2(dashDir.z, dashDir.x) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(yaw);
        this.yBodyRot = yaw;
        this.yHeadRot = yaw;
        level.sendParticles(ParticleTypes.CLOUD, this.getX(), this.getY() + 0.2, this.getZ(), 4, 0.4, 0.1, 0.4, 0.02);
        for (LivingEntity e : victims(this.position(), this.getBbWidth() / 2 + 1.4)) {
            if (!dashHit.add(e.getUUID())) continue;
            hitsLanded++;
            RpgCombat.mobHit(this, e, dashMult);
            if (com.mmorpg.combat.CrowdControl.allow(e)) {
                e.push(dashDir.x * 1.2, 0.45, dashDir.z * 1.2);
                e.needsSync = true;
            }
        }
    }

    /**
     * Canalisation : le boss reste sur place, ne tourne plus que lentement vers sa cible (on peut le contourner) et
     * declenche l'effet a intervalle regulier.
     */
    protected void channel(int ticks, float turnPerTick, int every, Runnable pulse) {
        channelEnd = this.tickCount + ticks;
        channelTurn = turnPerTick;
        channelEvery = Math.max(1, every);
        channelPulse = pulse;
        immobile(ticks);
    }

    /** Rayon continu devant le boss, qui suit lentement sa cible. */
    protected void beam(ServerLevel level, int ticks, double length, double halfWidth, double mult, int color, ParticleOptions particle) {
        channel(ticks, 2.0F, 2, () -> {
            Vec3 dir = facing();
            Vec3 from = this.position().add(dir.scale(this.getBbWidth() / 2)).add(0, Math.min(3.0, this.getBbHeight() * 0.55), 0);
            Vec3 to = new Vec3(from.x + dir.x * length, this.getY() + 1.0, from.z + dir.z * length);
            level.addFreshEntity(SkillFxEntity.create(level, FxKind.BEAM, from, halfWidth * 0.6, color, 3).points(List.of(from, to)));
            for (double r = 1; r < length; r += 1.5) {
                level.sendParticles(particle, from.x + dir.x * r, from.y + (to.y - from.y) * r / length, from.z + dir.z * r, 1, 0.1, 0.1, 0.1, 0);
            }
            if ((channelEnd - this.tickCount) % 6 == 0) hitLine(this.position(), dir, length, halfWidth, mult, null);
        });
    }

    /** Fenetre de vulnerabilite : le boss subit le double de degats (apres une grosse attaque ratee, noyau expose...). */
    protected void expose(int ticks) {
        exposedUntil = Math.max(exposedUntil, this.tickCount + ticks);
        this.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING, ticks, 0, false, false));
    }

    public boolean exposed() {
        return this.tickCount < exposedUntil;
    }

    /** Clip des boss sans jeu d'animations propre (indice de ImportedSpells.CLIPS). */
    protected void playClip(int index) {
        this.level().broadcastEntityEvent(this, (byte) (EVENT_CLIP + index));
    }

    protected void onPhaseChange(ServerLevel level, int phase) {
        Component msg = Component.literal("☠ " + this.getName().getString() + " entre en phase " + phase + " !").withColor(0xFF5050);
        for (var p : level.players()) {
            if (p.distanceToSqr(this) < 64 * 64) p.sendOverlayMessage(msg);
        }
        BossFury.onPhase(this, level, phase, fury);
        // Terrain des boss : plus aucune destruction du sol (le pont/sol ne cède plus)
        com.mmorpg.dungeon.ArenaEvents.phase(level, this, phase);          // renforts et annonce quand le boss est dans l'antre d'un donjon
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
