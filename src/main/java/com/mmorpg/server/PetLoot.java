package com.mmorpg.server;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;

/** Only killer-owned mob drops; no player-thrown items, foreign drops or full-inventory deletion. */
public final class PetLoot {
    private static boolean canAccept(ServerPlayer p,net.minecraft.world.item.ItemStack wanted) {
        if(wanted.is(com.mmorpg.registry.ModItems.PIECE_OR.get()))return true;
        for(int i=0;i<36;i++) {var slot=p.getInventory().getItem(i);if(slot.isEmpty()||net.minecraft.world.item.ItemStack.isSameItemSameComponents(slot,wanted)&&slot.getCount()<slot.getMaxStackSize())return true;}return false;
    }
    public static String tag(ServerPlayer p) { return "aeloria_loot:"+p.getUUID(); }
    public static ItemEntity target(com.mmorpg.entity.PetEntity pet,ServerPlayer owner) {
        return owner.level().getEntitiesOfClass(ItemEntity.class,owner.getBoundingBox().inflate(12),e->e.isAlive()&&e.entityTags().contains(tag(owner)))
                .stream().filter(e->canAccept(owner,e.getItem()))
                .min(java.util.Comparator.comparingDouble(pet::distanceToSqr)).orElse(null);
    }
    public static void collect(ServerPlayer owner,ItemEntity entity) {
        if(!entity.isAlive()||!entity.entityTags().contains(tag(owner)))return;
        if(CurrencyPickup.collect(owner,entity))return;
        var stack=entity.getItem();var original=stack.copy();int before=stack.getCount();
        owner.getInventory().add(stack);int taken=before-stack.getCount();
        if(taken>0) {
            AdventureSummary.item(owner,original.getItem(),taken);owner.containerMenu.broadcastChanges();
            owner.level().playSound(null,owner.blockPosition(),net.minecraft.sounds.SoundEvents.ITEM_PICKUP,net.minecraft.sounds.SoundSource.PLAYERS,.25f,1.2f);
        }
        if(stack.isEmpty())entity.discard();else entity.setItem(stack);
    }
}
