package com.mmorpg.entity;

import com.mmorpg.mount.MountType;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.core.component.DataComponents;
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
import java.util.UUID;

/** A temporary riding entity. Ownership, not the entity or its saddle, is saved. */
public class MountEntity extends Horse {
    private UUID rider;
    public MountEntity(EntityType<? extends Horse> type, Level level) { super(type, level); }
    // The type must be serializable for Minecraft to allow riding. Suppress instance saves instead.
    @Override public boolean shouldBeSaved() { return false; }
    @Override public boolean saveAsPassenger(net.minecraft.world.level.storage.ValueOutput output) { return false; }
    @Override public boolean hurtServer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) { return false; }
    @Override protected void dropEquipment(net.minecraft.server.level.ServerLevel level) { }
    @Override public boolean canMate(net.minecraft.world.entity.animal.Animal other) { return false; }
    @Override protected void registerGoals() { }
    public void setup(ServerPlayer owner, MountType type) {
        rider = owner.getUUID();
        setOwner(owner); setTamed(true); setAge(0);
        setComponent(DataComponents.HORSE_VARIANT, type.variant);
        setItemSlot(EquipmentSlot.SADDLE, permanentSaddle());
        getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(type.speed);
        getAttribute(Attributes.JUMP_STRENGTH).setBaseValue(.7);
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
    @Override public boolean canDispenserEquipIntoSlot(EquipmentSlot slot) { return false; }
    @Override public void tick() {
        super.tick();
        if (level() instanceof net.minecraft.server.level.ServerLevel sl && tickCount % 20 == 0) {
            var owner = rider == null ? null : sl.getServer().getPlayerList().getPlayer(rider);
            if (owner == null || !owner.isAlive() || owner.isSpectator() || owner.level() != level()
                    || distanceToSqr(owner) > 4096 || !getUUID().equals(RpgPlayers.get(owner).mountEntity)) discard();
        }
    }
}
