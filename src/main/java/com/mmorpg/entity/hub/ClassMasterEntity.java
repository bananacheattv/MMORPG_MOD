package com.mmorpg.entity.hub;

import com.mmorpg.hub.HubManager;
import com.mmorpg.rpg.PlayerClass;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * PNJ du hub : un Maitre par classe (Guerrier, Mage, Archer, Tank), qui presente la classe, la fait essayer puis la fait choisir,
 * et le Gardien de l'epreuve (salle d'essai : changer de classe ou de niveau, choisir, quitter). Chaque maitre a son modele
 * Blockbench anime et ses effets. Immobile, invulnerable et persistant ; un clic gauche ou droit ouvre le dialogue.
 */
public class ClassMasterEntity extends PathfinderMob {
    public static final int GUERRIER = 0, MAGE = 1, ARCHER = 2, TANK = 3, EPREUVE = 4;
    public static final String[] IDS = {"guerrier", "mage", "archer", "tank", "epreuve"};
    public static final String[] NAMES = {"Maître Guerrier", "Maître Mage", "Maître Archer", "Maître Tank", "Gardien de l'épreuve"};

    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(ClassMasterEntity.class, EntityDataSerializers.INT);

    /** Mise en scene (cote client) : debut et fin, en ticks de l'entite ; animation de presentation et effets renforces. */
    public float presentStart = -1000F;
    public float presentUntil = -1F;
    /** Orientation lissee de la tete vers le joueur local (degres, cote client). */
    public float headYaw, headPitch;
    public long lastRenderNanos;

    /** Met le maitre en scene pendant {@code seconds} (cinematique, dialogue). */
    public void stage(float seconds) {
        presentStart = tickCount;
        presentUntil = tickCount + seconds * 20F;
    }
    private long lastOpen;

    public ClassMasterEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setNoAi(true);
        this.setPersistenceRequired();
        this.xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20)
                .add(Attributes.MOVEMENT_SPEED, 0.0)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(KIND, 0);
    }

    public void setup(int kind) {
        this.entityData.set(KIND, Math.floorMod(kind, IDS.length));
        this.setCustomName(Component.literal(NAMES[kind()]));
        this.setCustomNameVisible(true);
    }

    public int kind() {
        return this.entityData.get(KIND);
    }

    /** Classe presentee (null pour le Gardien de l'epreuve). */
    public @Nullable PlayerClass playerClass() {
        return switch (kind()) {
            case GUERRIER -> PlayerClass.GUERRIER;
            case MAGE -> PlayerClass.MAGE;
            case ARCHER -> PlayerClass.ARCHER;
            case TANK -> PlayerClass.TANK;
            default -> null;
        };
    }

    // ================================================================== interaction

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && player instanceof ServerPlayer sp) open(sp);
        return InteractionResult.SUCCESS;
    }

    private void open(ServerPlayer sp) {
        long now = this.level().getGameTime();
        if (now - lastOpen < 8) return;                  // evite de rouvrir le dialogue a chaque tick (bouton maintenu)
        lastOpen = now;
        HubManager.interact(sp, this);
    }

    /** Un coup (clic gauche) ouvre aussi le dialogue ; le maitre ne subit jamais de degats. */
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return super.hurtServer(level, source, damage);
        Entity attacker = source.getEntity();
        if (attacker instanceof ServerPlayer sp && !sp.isShiftKeyDown()) open(sp);
        return false;
    }

    // ================================================================== effets

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) ambient();
    }

    /** Effets propres a chaque classe, discrets (plus marques pendant la mise en scene). */
    private void ambient() {
        float age = this.tickCount;
        boolean staged = age < presentUntil;
        int period = staged ? 1 : 3;
        if (this.tickCount % period != 0) return;
        double x = getX() + (random.nextDouble() - 0.5) * 1.2;
        double z = getZ() + (random.nextDouble() - 0.5) * 1.2;
        double y = getY() + 0.3 + random.nextDouble() * 2.2;
        ParticleOptions p;
        double vy = 0.01;
        switch (kind()) {
            case GUERRIER -> {
                p = staged ? ParticleTypes.LAVA : ParticleTypes.FLAME;
                vy = 0.03;
                if (random.nextInt(3) != 0) return;
            }
            case MAGE -> p = random.nextBoolean() ? ParticleTypes.ENCHANT : ParticleTypes.WITCH;
            case ARCHER -> {
                p = random.nextInt(4) == 0 ? ParticleTypes.CHERRY_LEAVES : ParticleTypes.HAPPY_VILLAGER;
                if (random.nextInt(3) != 0) return;
            }
            case TANK -> {
                p = staged ? ParticleTypes.ELECTRIC_SPARK : ParticleTypes.WAX_ON;
                if (random.nextInt(3) != 0) return;
            }
            default -> {
                p = ParticleTypes.END_ROD;
                if (random.nextInt(4) != 0) return;
            }
        }
        this.level().addParticle(p, x, y, z, 0, vy, 0);
    }

    // ================================================================== solidite

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
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("kind", kind());
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        this.entityData.set(KIND, Math.floorMod(in.getIntOr("kind", 0), IDS.length));
        this.setNoAi(true);
    }
}
