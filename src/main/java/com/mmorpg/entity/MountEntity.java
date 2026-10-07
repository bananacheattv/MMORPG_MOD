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
    private UUID rider;
    private final MountType mountType;

    public MountEntity(EntityType<? extends Horse> type, Level level) {
        super(type, level);
        String id = BuiltInRegistries.ENTITY_TYPE.getKey(type).getPath().replaceFirst("^monture_", "");
        MountType t = MountType.byId(id);
        this.mountType = t == null ? MountType.SANGLIER : t;
    }

    public MountType mountType() { return mountType; }

    public boolean isFlying() { return mountType.flies && !onGround() && isVehicle(); }

    // The type must be serializable for Minecraft to allow riding. Suppress instance saves instead.
    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean saveAsPassenger(net.minecraft.world.level.storage.ValueOutput output) { return false; }
    @Override public boolean hurtServer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) { return false; }
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
        return (level().isClientSide() || passenger.getUUID().equals(rider)) && super.canAddPassenger(passenger);
    }

    @Override public InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide()) return InteractionResult.SUCCESS;
        if (!player.getUUID().equals(rider) || player.isSpectator()) return InteractionResult.FAIL;
        doPlayerRide(player);
        return InteractionResult.SUCCESS;
    }

    @Override public void openCustomInventoryScreen(Player player) { }
    // pas de hennissements : ce ne sont plus des chevaux
    @Override protected net.minecraft.sounds.SoundEvent getAmbientSound() { return null; }
    @Override protected net.minecraft.sounds.SoundEvent getAngrySound() { return null; }
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
        if (mountType.flies && !isVehicle() && isNoGravity()) setNoGravity(false);
        if (level() instanceof net.minecraft.server.level.ServerLevel sl && tickCount % 20 == 0) {
            var owner = rider == null ? null : sl.getServer().getPlayerList().getPlayer(rider);
            if (owner == null || !owner.isAlive() || owner.isSpectator() || owner.level() != level()
                    || distanceToSqr(owner) > 4096 || !getUUID().equals(RpgPlayers.get(owner).mountEntity)) discard();
        }
    }
}
