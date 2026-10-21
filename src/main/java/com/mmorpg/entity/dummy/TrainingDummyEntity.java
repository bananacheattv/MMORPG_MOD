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
 * Mannequin d'entrainement : immortel, immobile, non poussable, ne riposte jamais. Il recoit les degats du mod comme un
 * monstre (defense, critiques, menace du Tank, textes flottants) mais ne perd jamais de vie : chaque coup alimente ses
 * statistiques (DPS sur une fenetre glissante, total, plus gros coup, critiques) affichees au-dessus de lui.
 * Clic droit accroupi : remise a zero. Trois variantes : standard, blinde (defense physique) et magique (resistance magique).
 */
public class TrainingDummyEntity extends PathfinderMob {
    public static final String[] VARIANTS = {"standard", "blinde", "magique"};
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
        }
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
        if (source.getEntity() instanceof Player p && p.getAbilities().instabuild && p.isShiftKeyDown()) {
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
