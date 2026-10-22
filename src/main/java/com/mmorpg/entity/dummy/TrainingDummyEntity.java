package com.mmorpg.entity.dummy;

import com.mmorpg.config.ConfigManager;
import com.mmorpg.config.DummyConfig;
import com.mmorpg.registry.ModAttachments;
import com.mmorpg.registry.ModItems;
import com.mmorpg.rpg.MobData;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Mannequin d'entrainement : immortel, immobile, non poussable, ne riposte jamais (sauf la variante duelliste, qui
 * annonce puis porte un coup mesure a qui reste a sa portee, sans jamais achever un joueur). Il recoit les degats du mod comme un
 * monstre (defense, critiques, menace du Tank, textes flottants) mais ne perd jamais de vie : chaque coup alimente ses
 * statistiques (DPS sur une fenetre glissante, total, plus gros coup, critiques) affichees au-dessus de lui.
 * Clic droit accroupi : remise a zero. Trois variantes : standard, blinde (defense physique) et magique (resistance magique).
 */
public class TrainingDummyEntity extends PathfinderMob {
    public static final String[] VARIANTS = {"standard", "blinde", "magique", "duelliste"};
    /** Taille du tampon des coups recents (25 coups/s sur 10 s sont deja largement couverts). */
    private static final int RING = 512;
    private static final float DUMMY_HP = 999_999f;

    private static final EntityDataAccessor<Integer> VARIANT = SynchedEntityData.defineId(TrainingDummyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HIT_SEQ = SynchedEntityData.defineId(TrainingDummyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> HIT_FORCE = SynchedEntityData.defineId(TrainingDummyEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> HIT_DIR = SynchedEntityData.defineId(TrainingDummyEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> RESET_SEQ = SynchedEntityData.defineId(TrainingDummyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> DPS = SynchedEntityData.defineId(TrainingDummyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Long> TOTAL = SynchedEntityData.defineId(TrainingDummyEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Long> BEST = SynchedEntityData.defineId(TrainingDummyEntity.class, EntityDataSerializers.LONG);
    private static final EntityDataAccessor<Integer> HITS = SynchedEntityData.defineId(TrainingDummyEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> CRITS = SynchedEntityData.defineId(TrainingDummyEntity.class, EntityDataSerializers.INT);

    /** Mannequin duelliste : distance a laquelle il repere un joueur, portee de son coup, duree de l'avertissement (ticks). */
    private static final double DUEL_SIGHT = 6.0;
    private static final double DUEL_RANGE = 3.4;
    private static final int DUEL_WINDUP = 16;
    /** Un coup n'est jamais porte sous ce seuil de vie (en part de la vie max) : l'entrainement ne tue pas. */
    private static final double DUEL_MERCY = 0.35;
    private int duelClock;

    // tampon circulaire des coups recents (serveur uniquement, alloue au premier coup)
    private long[] ringTick;
    private float[] ringDamage;
    private int ringHead;
    private int ringCount;

    // etat d'animation cote client (ressort amorti, voir DummyRenderer)
    public float tiltX, tiltZ, velX, velZ;
    public int seenHitSeq = -1;
    public int seenResetSeq = -1;
    public float resetStartAge = -1000F;
    public long lastRenderNanos;

    public TrainingDummyEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setPersistenceRequired();
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
                .add(Attributes.FOLLOW_RANGE, 2);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(VARIANT, 0);
        builder.define(HIT_SEQ, 0);
        builder.define(HIT_FORCE, 0F);
        builder.define(HIT_DIR, 0F);
        builder.define(RESET_SEQ, 0);
        builder.define(DPS, 0);
        builder.define(TOTAL, 0L);
        builder.define(BEST, 0L);
        builder.define(HITS, 0);
        builder.define(CRITS, 0);
    }

    // ================================================================== configuration

    public void setup(int variant) {
        this.entityData.set(VARIANT, Math.floorMod(variant, VARIANTS.length));
        refreshData();
    }

    public int variant() {
        return this.entityData.get(VARIANT);
    }

    public String variantId() {
        return VARIANTS[variant()];
    }

    public DummyConfig.Variant config() {
        return ConfigManager.dummy().variant(variantId());
    }

    /** Defense appliquee au coup : physique, ou magique pour les sorts (reglages dans mannequin.json). */
    public double defenseFor(boolean magic) {
        DummyConfig.Variant v = config();
        return magic ? v.magicDefense : v.physicalDefense;
    }

    /** (Re)cree les donnees RPG a partir de la configuration : niveau, defense, vie fictive. */
    private void refreshData() {
        if (this.level().isClientSide()) return;
        DummyConfig.Variant v = config();
        MobData d = this.getExistingDataOrNull(ModAttachments.MOB);
        if (d == null) d = new MobData();
        d.key = "mannequin_" + variantId();
        d.level = ConfigManager.dummy().level;
        d.hp = DUMMY_HP;
        d.maxHp = DUMMY_HP;
        d.atk = 0;
        d.def = v.physicalDefense;
        d.boss = false;
        d.xpMultiplier = 0;
        this.setData(ModAttachments.MOB, d);
        this.setCustomName(Component.literal(v.name));
        this.setCustomNameVisible(false);
    }

    // ================================================================== statistiques

    public int dps() {
        return this.entityData.get(DPS);
    }

    public long total() {
        return this.entityData.get(TOTAL);
    }

    public long best() {
        return this.entityData.get(BEST);
    }

    public int hits() {
        return this.entityData.get(HITS);
    }

    public int crits() {
        return this.entityData.get(CRITS);
    }

    public int hitSeq() {
        return this.entityData.get(HIT_SEQ);
    }

    public int resetSeq() {
        return this.entityData.get(RESET_SEQ);
    }

    public float hitForce() {
        return this.entityData.get(HIT_FORCE);
    }

    public float hitDir() {
        return this.entityData.get(HIT_DIR);
    }

    /** Enregistre un coup recu (appele par RpgCombat apres defense) : statistiques, animation, particules. */
    public void recordHit(double damage, boolean crit, @Nullable Entity attacker) {
        if (!(this.level() instanceof ServerLevel level) || damage <= 0 || !Double.isFinite(damage)) return;
        long now = level.getGameTime();
        if (ringTick == null) {
            ringTick = new long[RING];
            ringDamage = new float[RING];
        }
        ringTick[ringHead] = now;
        ringDamage[ringHead] = (float) damage;
        ringHead = (ringHead + 1) % RING;
        if (ringCount < RING) ringCount++;

        long rounded = Math.round(damage);
        this.entityData.set(TOTAL, total() + rounded);
        if (rounded > best()) this.entityData.set(BEST, rounded);
        this.entityData.set(HITS, hits() + 1);
        if (crit) this.entityData.set(CRITS, crits() + 1);
        updateDps(now);

        // animation : force (degres d'inclinaison) selon le coup, direction = de l'attaquant vers le mannequin
        double dx = attacker == null ? 0 : this.getX() - attacker.getX();
        double dz = attacker == null ? 1 : this.getZ() - attacker.getZ();
        float amplitude = (float) Math.min(20.0, 3.0 + 5.5 * Math.log10(1.0 + damage / 20.0));
        this.entityData.set(HIT_FORCE, amplitude);
        this.entityData.set(HIT_DIR, (float) Math.toDegrees(Math.atan2(dz, dx)));
        this.entityData.set(HIT_SEQ, hitSeq() + 1);

        level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, Blocks.HAY_BLOCK.defaultBlockState()),
                this.getX(), this.getY() + 1.4, this.getZ(), crit ? 8 : 3, 0.22, 0.3, 0.22, 0.04);
        level.playSound(null, this.blockPosition(), crit ? SoundEvents.WOOD_BREAK : SoundEvents.WOOD_HIT, SoundSource.NEUTRAL,
                crit ? 0.7F : 0.5F, crit ? 0.9F : 1.1F + this.random.nextFloat() * 0.2F);
    }

    /** DPS sur la fenetre glissante : somme des coups recents / duree ecoulee (au moins 1 s, au plus la fenetre). */
    private void updateDps(long now) {
        long window = ConfigManager.dummy().dpsWindowSeconds * 20L;
        double sum = 0;
        long oldest = now;
        int kept = 0;
        for (int i = 0; i < ringCount; i++) {
            int idx = Math.floorMod(ringHead - 1 - i, RING);
            if (now - ringTick[idx] > window) break;
            sum += ringDamage[idx];
            oldest = ringTick[idx];
            kept++;
        }
        ringCount = kept;
        int dps = 0;
        if (kept > 0) {
            double seconds = Math.max(1.0, Math.min(window / 20.0, (now - oldest) / 20.0));
            dps = (int) Math.min(Integer.MAX_VALUE, Math.round(sum / seconds));
        }
        if (dps != dps()) this.entityData.set(DPS, dps);
    }

    public void resetStats() {
        ringHead = 0;
        ringCount = 0;
        this.entityData.set(TOTAL, 0L);
        this.entityData.set(BEST, 0L);
        this.entityData.set(HITS, 0);
        this.entityData.set(CRITS, 0);
        this.entityData.set(DPS, 0);
        this.entityData.set(RESET_SEQ, resetSeq() + 1);
        if (this.level() instanceof ServerLevel level) {
            level.playSound(null, this.blockPosition(), SoundEvents.BUNDLE_INSERT, SoundSource.NEUTRAL, 0.8F, 0.8F);
        }
    }

    // ================================================================== comportement

    @Override
    public void tick() {
        super.tick();
        if (this.level() instanceof ServerLevel level) {
            if (this.tickCount == 1) refreshData();
            // le DPS retombe tout seul quand on arrete de frapper (recalcul toutes les 5 ticks, seulement s'il reste un coup recent)
            if (ringCount > 0 && this.tickCount % 5 == 0) updateDps(level.getGameTime());
            if (variant() == 3) duelTick(level);
        }
    }

    // ================================================================== duelliste

    private static boolean canBeStruck(ServerPlayer p) {
        return p.isAlive() && !p.isCreative() && !p.isSpectator();
    }

    private @Nullable ServerPlayer nearestChallenger(ServerLevel level) {
        ServerPlayer best = null;
        for (ServerPlayer p : level.players()) {
            if (!canBeStruck(p) || p.distanceToSqr(this) > DUEL_SIGHT * DUEL_SIGHT) continue;
            if (best == null || p.distanceToSqr(this) < best.distanceToSqr(this)) best = p;
        }
        return best;
    }

    /** Le duelliste fixe le joueur le plus proche, annonce son coup (inclinaison en arriere, anneau de particules au sol), puis frappe. */
    private void duelTick(ServerLevel level) {
        DummyConfig.Variant v = config();
        if (v.strikePercent <= 0) return;
        ServerPlayer target = nearestChallenger(level);
        if (target == null) {
            duelClock = 0;
            return;
        }
        float yaw = (float) (Math.toDegrees(Math.atan2(target.getZ() - this.getZ(), target.getX() - this.getX()))) - 90F;
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);
        int period = Math.max(DUEL_WINDUP + 4, Math.round(v.strikeIntervalSeconds * 20F));
        duelClock++;
        int untilStrike = (period - duelClock % period) % period;
        if (untilStrike == DUEL_WINDUP) {
            level.playSound(null, this.blockPosition(), SoundEvents.ANVIL_PLACE, SoundSource.HOSTILE, 0.6F, 0.6F);
            swing(this.getX() - target.getX(), this.getZ() - target.getZ(), 9F);                 // se cabre en arriere
        }
        if (untilStrike > 0 && untilStrike <= DUEL_WINDUP && untilStrike % 2 == 0) {
            for (int i = 0; i < 12; i++) {                                                         // anneau d'avertissement a la portee du coup
                double a = (Math.PI * 2 * i) / 12 + untilStrike * 0.1;
                level.sendParticles(ParticleTypes.CRIT, this.getX() + Math.cos(a) * DUEL_RANGE, this.getY() + 0.1,
                        this.getZ() + Math.sin(a) * DUEL_RANGE, 1, 0, 0, 0, 0);
            }
        }
        if (untilStrike == 0) strike(level, target, v);
    }

    private void strike(ServerLevel level, ServerPlayer focus, DummyConfig.Variant v) {
        swing(focus.getX() - this.getX(), focus.getZ() - this.getZ(), 17F);                       // se jette vers l'avant
        level.playSound(null, this.blockPosition(), SoundEvents.IRON_GOLEM_ATTACK, SoundSource.HOSTILE, 1.0F, 0.8F);
        level.sendParticles(ParticleTypes.SWEEP_ATTACK, this.getX(), this.getY() + 1.0, this.getZ(), 1, 0, 0, 0, 0);
        for (ServerPlayer p : level.players()) {
            if (!canBeStruck(p) || p.distanceToSqr(this) > DUEL_RANGE * DUEL_RANGE) continue;
            var data = com.mmorpg.server.RpgPlayers.get(p);
            double max = data.stats.get(com.mmorpg.rpg.Stat.MAX_HP);
            if (max <= 0 || data.hp < max * DUEL_MERCY) {
                p.sendOverlayMessage(Component.literal("Le duelliste retient son coup : reprenez des forces.").withColor(0xA0E0A0));
                continue;
            }
            float before = data.hp;
            com.mmorpg.combat.RpgCombat.dealDamage(this, p, max * v.strikePercent / 100.0, false, false);
            com.mmorpg.MMORPG.LOGGER.debug("[MMORPG] duelliste : {} perd {} points de vie", p.getName().getString(), before - data.hp);
        }
    }

    /** Anime le ressort du mannequin sans toucher a ses statistiques : (dx, dz) = direction du mouvement. */
    private void swing(double dx, double dz, float force) {
        this.entityData.set(HIT_FORCE, force);
        this.entityData.set(HIT_DIR, (float) Math.toDegrees(Math.atan2(dz, dx)));
        this.entityData.set(HIT_SEQ, hitSeq() + 1);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        if (!this.level().isClientSide()) {
            if (player.isShiftKeyDown()) {
                resetStats();
                player.sendOverlayMessage(Component.literal("Statistiques du mannequin remises à zéro.").withColor(0xA0E0A0));
            } else {
                player.sendOverlayMessage(Component.literal("Frappez-le pour mesurer vos dégâts. Accroupi + clic droit : remise à zéro.").withColor(0xE0D8C0));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        // un joueur creatif accroupi retire le mannequin d'un coup
        if (source.is(net.minecraft.world.damagesource.DamageTypes.PLAYER_ATTACK) && source.getEntity() instanceof Player p
                && p.getAbilities().instabuild && p.isShiftKeyDown()) {
            this.discard();
            return true;
        }
        return super.hurtServer(level, source, damage);
    }

    @Override
    public ItemStack getPickResult() {
        var item = ModItems.DUMMY_ITEMS.get(variantId());
        return item == null ? ItemStack.EMPTY : new ItemStack(item.get());
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public void push(Entity entity) {
    }

    @Override
    protected void pushEntities() {
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected @Nullable SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected @Nullable SoundEvent getDeathSound() {
        return null;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("variant", variant());
        out.putLong("total", total());
        out.putLong("best", best());
        out.putInt("hits", hits());
        out.putInt("crits", crits());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        this.entityData.set(VARIANT, Math.floorMod(in.getIntOr("variant", 0), VARIANTS.length));
        this.entityData.set(TOTAL, in.getLongOr("total", 0L));
        this.entityData.set(BEST, in.getLongOr("best", 0L));
        this.entityData.set(HITS, in.getIntOr("hits", 0));
        this.entityData.set(CRITS, in.getIntOr("crits", 0));
        this.setNoAi(true);
    }
}
