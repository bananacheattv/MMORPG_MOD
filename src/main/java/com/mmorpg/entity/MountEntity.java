package com.mmorpg.entity;

import com.mmorpg.mount.MountType;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * Monture 3D temporaire (logique de cheval, rendu par MountRenderer). La possession est sauvegardee, pas l'entite.
 * L'Hippogriffe vole : regarder vers le haut en avancant pour monter, vers le bas pour descendre, saut = battement d'ailes.
 */
public class MountEntity extends Horse {
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return com.mmorpg.util.RenderDist.entity(distance);
    }

    private UUID rider;
    private final MountType mountType;

    public MountEntity(EntityType<? extends Horse> type, Level level) {
        super(type, level);
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath().replaceFirst("^monture_", "");
        MountType t = MountType.byId(id);
        this.mountType = t == null ? MountType.SANGLIER : t;
    }

    /** Passagers factices (vitrine de test uniquement). */
    public static final java.util.Set<UUID> TEST_PASSENGERS = java.util.concurrent.ConcurrentHashMap.newKeySet();
    public MountType mountType() { return mountType; }

    /** Un coequipier du groupe du proprietaire peut monter en passager tant que le proprietaire est en selle et qu'il reste un siege. */
    private boolean isShared(Player other) {
        if (mountType.seats.length < 2 || rider == null || !(getFirstPassenger() instanceof ServerPlayer owner) || !owner.getUUID().equals(rider)) return false;
        return com.mmorpg.server.PartyManager.sameParty(owner, other);
    }

    public boolean isFlying() { return mountType.flies && !onGround() && isVehicle(); }

    // The type must be serializable for Minecraft to allow riding. Suppress instance saves instead.
    @Override public boolean shouldBeSaved() { return display; }
    /** Monture posee en decor par un operateur (/monture poser) : immobile, invulnerable, non montable, sauvegardee. */
    private boolean display;
    public boolean isDisplay() { return display; }
    public void setDisplay() {
        display = true; setNoAi(true); setPersistenceRequired(); setSilent(true); setNoGravity(false);
        setCustomName(Component.literal(mountType.label)); setCustomNameVisible(false);
    }
    @Override protected void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        if (display) output.putBoolean("mm_display", true);
    }
    @Override protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        if (input.getBooleanOr("mm_display", false)) setDisplay();
    }
    @Override public boolean isPushable() { return !display && super.isPushable(); }
    @Override public void push(double x, double y, double z) { if (!display) super.push(x, y, z); }
    @Override public boolean saveAsPassenger(net.minecraft.world.level.storage.ValueOutput output) {
        return display && super.saveAsPassenger(output);
    }
    /**
     * La monture est invulnerable : les coups (fleches, monstres) touchent le cavalier a sa place. Les chutes, explosions et degats du
     * decor ne sont pas renvoyes : le cavalier les subit deja lui-meme.
     */
    @Override public boolean hurtServer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (display) return false;
        if (getFirstPassenger() instanceof net.minecraft.world.entity.LivingEntity rider && source.getEntity() != null && source.getEntity() != rider
                && !source.is(net.minecraft.tags.DamageTypeTags.IS_EXPLOSION)) rider.hurtServer(level, source, amount);
        return false;
    }
    /** Descendre de la monture la renvoie. */
    @Override protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (!level().isClientSide() && passenger instanceof ServerPlayer sp && getUUID().equals(RpgPlayers.get(sp).mountEntity)) {
            com.mmorpg.server.MountManager.despawn(sp);
            RpgPlayers.sync(sp);
        }
    }
    @Override protected void dropEquipment(net.minecraft.server.level.ServerLevel level) { }
    @Override public boolean canMate(net.minecraft.world.entity.animal.Animal other) { return false; }
    @Override protected void registerGoals() { }
    @Override protected void checkFallDamage(double ya, boolean onGround, BlockState state, BlockPos pos) {
        if (!mountType.flies) super.checkFallDamage(ya, onGround, state, pos);
    }

    public void setup(ServerPlayer owner, MountType type) {
        rider = owner.getUUID();
        setOwner(owner); setTamed(true); setAge(0);
        setItemSlot(EquipmentSlot.SADDLE, permanentSaddle());
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(type.speed);
        getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(type == MountType.CHEVRE_CELESTE ? 1.0 : .7);
        setCustomName(Component.literal(type.label + " • " + owner.getName().getString()));
    }

    public static ItemStack permanentSaddle() {
        var stack = new ItemStack(Items.SADDLE);
        stack.set(DataComponents.EQUIPPABLE, net.minecraft.world.item.equipment.Equippable.builder(EquipmentSlot.SADDLE)
            .setAsset(net.minecraft.world.item.equipment.EquipmentAssets.SADDLE).setCanBeSheared(false).build());
        return stack;
    }

    @Override protected boolean canAddPassenger(Entity passenger) {
        if (display || getPassengers().size() >= mountType.seats.length) return false;
        return level().isClientSide() || passenger.getUUID().equals(rider) || TEST_PASSENGERS.contains(passenger.getUUID()) || (passenger instanceof Player && isShared((Player) passenger));
    }

    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        if (display) return InteractionResult.SUCCESS;
        if (player.isSpectator()) return InteractionResult.FAIL;
        if (player.getUUID().equals(rider)) { doPlayerRide(player); return InteractionResult.SUCCESS; }
        if (!isShared(player)) return InteractionResult.FAIL;
        player.startRiding(this);
        return InteractionResult.SUCCESS;
    }

    @Override public void openCustomInventoryScreen(Player player) { }
    // pas de hennissements : ce ne sont plus des chevaux
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return com.mmorpg.registry.ModSounds.GROWL.get(); }
    @Override protected net.minecraft.sounds.SoundEvent getAngrySound() { return com.mmorpg.registry.ModSounds.GROWL.get(); }
    @Override protected net.minecraft.sounds.SoundEvent getDeathSound() { return com.mmorpg.registry.ModSounds.GROWL.get(); }
    @Override protected net.minecraft.sounds.SoundEvent getHurtSound(net.minecraft.world.damagesource.DamageSource source) { return com.mmorpg.registry.ModSounds.GROWL.get(); }
    @Override protected net.minecraft.sounds.SoundEvent getEatingSound() { return null; }
    @Override protected void playJumpSound() { playSound(com.mmorpg.registry.ModSounds.WHOOSH.get(), .25f, 1.3f); }
    @Override protected void playStepSound(BlockPos pos, BlockState state) {
        if (!state.liquid()) {
            var sound = state.getSoundType(level(), pos, this);
            playSound(sound.getStepSound(), .18f, mountType == MountType.TORTUE_CRISTAL ? .7f : 1.1f);
        }
    }
    @Override protected void playGallopSound(net.minecraft.world.level.block.SoundType type) {
        playSound(type.getStepSound(), .22f, 1f);
    }
    @Override protected net.minecraft.core.Holder<net.minecraft.sounds.SoundEvent> getEquipSound(EquipmentSlot slot, ItemStack stack, net.minecraft.world.item.equipment.Equippable equippable) {
        return com.mmorpg.registry.ModSounds.CHIME;
    }
    /** Also catches inherited landing and breathing effects: no horse samples can leak through. */
    @Override public void playSound(net.minecraft.sounds.SoundEvent sound, float volume, float pitch) {
        sound = mountSound(sound);
        if (sound == com.mmorpg.registry.ModSounds.WHOOSH.get()) volume = Math.min(volume, .3f);
        if (sound == com.mmorpg.registry.ModSounds.GROWL.get()) pitch *= .75f + mountType.ordinal() * .045f;
        super.playSound(sound, volume, pitch);
    }
    public static net.minecraft.sounds.SoundEvent mountSound(net.minecraft.sounds.SoundEvent sound) {
        var id = BuiltInRegistries.SOUND_EVENT.getKey(sound);
        if (id != null && id.getNamespace().equals("minecraft") && id.getPath().startsWith("entity.horse")) {
            return com.mmorpg.registry.ModSounds.WHOOSH.get();
        }
        return sound;
    }
    @Override public boolean canDispenserEquipIntoSlot(EquipmentSlot slot) { return false; }

    // ---------------------------------------------------------------- vol (Hippogriffe)

    @Override
    protected void tickRidden(Player controller, Vec3 riddenInput) {
        super.tickRidden(controller, riddenInput);
        if (!mountType.flies) return;
        Vec3 v = getDeltaMovement();
        boolean airborne = !onGround();
        double vy = v.y;
        float pitch = controller.getXRot();          // negatif = regard vers le haut
        if (riddenInput.z > 0 && pitch < -12) vy = Math.min(.45, .08 + -pitch / 90.0 * .5);
        else if (airborne && riddenInput.z > 0 && pitch > 20) vy = Math.max(-.6, -pitch / 90.0 * .6);
        else if (airborne) vy = Math.max(vy * .5, -.04);  // plane doucement
        setNoGravity(airborne || vy > 0);
        setDeltaMovement(v.x, vy, v.z);
        resetFallDistance();
        controller.resetFallDistance();
    }

    /** Saut charge = battement d'ailes, meme en plein vol. */
    @Override
    protected void executeRidersJump(float amount, Vec3 input) {
        if (!mountType.flies) {
            super.executeRidersJump(amount, input);
            return;
        }
        Vec3 v = getDeltaMovement();
        setDeltaMovement(v.x, .35 + .5 * amount, v.z);
        setNoGravity(true);
        needsSync = true;
    }

    @Override
    public void onPlayerJump(int jumpAmount) {
        super.onPlayerJump(jumpAmount);
        if (mountType.flies && !onGround() && playerJumpPendingScale > 0) {
            executeRidersJump(playerJumpPendingScale, Vec3.ZERO);
            playerJumpPendingScale = 0;
        }
    }

    @Override
    protected float getFlyingSpeed() {
        return isFlying() ? getSpeed() * .55f : super.getFlyingSpeed();
    }

    @Override public void tick() {
        super.tick();
        if (display) return;
        if (mountType.flies && !isVehicle() && isNoGravity()) setNoGravity(false);
        if (level() instanceof net.minecraft.server.level.ServerLevel sl && tickCount % 20 == 0) {
            var owner = rider == null ? null : sl.getServer().getPlayerList().getPlayer(rider);
            if (owner == null || !owner.isAlive() || owner.isSpectator() || owner.level() != level()
                    || distanceToSqr(owner) > 4096 || !getUUID().equals(RpgPlayers.get(owner).mountEntity)) discard();
        }
    }
}
