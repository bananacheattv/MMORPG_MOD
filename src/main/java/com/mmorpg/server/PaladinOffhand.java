package com.mmorpg.server;

import com.mmorpg.item.*;
import com.mmorpg.registry.ModItems;
import com.mmorpg.rpg.PlayerClass;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** The shield is a bound class visual, never a transferable second item. */
public final class PaladinOffhand {
    public static boolean shield(ItemStack stack){return ModItems.PALADIN_SHIELDS.stream().anyMatch(item->stack.is(item.get()));}
    public static boolean equipped(net.minecraft.world.entity.player.Player player){
        var main=player.getMainHandItem();var data=player.getData(com.mmorpg.registry.ModAttachments.PLAYER);
        return data.playerClass==PlayerClass.TANK&&main.getItem() instanceof RpgWeaponItem weapon&&weapon.kind()==ItemDefs.WeaponKind.HAMMER;
    }
    public static void maintain(ServerPlayer player){
        for(int i=0;i<player.getInventory().getContainerSize();i++)if(i!=40&&shield(player.getInventory().getItem(i)))player.getInventory().setItem(i,ItemStack.EMPTY);
        ItemStack current=player.getOffhandItem();boolean wanted=equipped(player);
        if(!wanted){if(shield(current))player.setItemSlot(EquipmentSlot.OFFHAND,ItemStack.EMPTY);return;}
        int tier=Math.clamp((RpgPlayers.get(player).level-1)/25,0,3);if(RpgPlayers.get(player).level>=100)tier=4;
        var item=ModItems.PALADIN_SHIELDS.get(tier).get();
        if(!current.is(item)){
            if(!current.isEmpty()&&!shield(current)){var saved=current.copy();if(!player.getInventory().add(saved))player.drop(saved,false,net.minecraft.util.Prediction.SERVER_ONLY);}
            player.setItemSlot(EquipmentSlot.OFFHAND,new ItemStack(item));
        }
    }
}
