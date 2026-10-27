package com.mmorpg.server;

import com.mmorpg.MMORPG;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.level.block.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

@EventBusSubscriber(modid=MMORPG.MODID)
public final class VanillaStations {
    public static boolean disabled(Block block){return block instanceof CraftingTableBlock||block instanceof AbstractFurnaceBlock||block instanceof AnvilBlock||block instanceof EnchantingTableBlock;}
    @SubscribeEvent public static void use(PlayerInteractEvent.RightClickBlock event){
        if(disabled(event.getLevel().getBlockState(event.getPos()).getBlock())){
            event.setUseBlock(net.minecraft.util.TriState.FALSE);
            if(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)p.sendOverlayMessage(net.minecraft.network.chat.Component.literal("Utilise la forge arcanique et les ateliers d’Aeloria."));
        }
    }
}
