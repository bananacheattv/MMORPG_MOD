package com.mmorpg.server;
import com.mmorpg.MMORPG;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
@EventBusSubscriber(modid=MMORPG.MODID)
public final class LuckyMigration {
    public static void inventory(net.minecraft.world.Container inv){for(int i=0;i<inv.getContainerSize();i++){var s=inv.getItem(i);if(s.is(com.mmorpg.registry.ModItems.LUCKY_BLOCK.get()))inv.setItem(i,new net.minecraft.world.item.ItemStack(com.mmorpg.registry.ModItems.LUCKY_BLOCKS.getFirst().get(),s.getCount()));}}
    @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post e){if(e.getServer().getTickCount()%40==0)for(var p:e.getServer().getPlayerList().getPlayers()){inventory(p.getInventory());inventory(p.getEnderChestInventory());}}
}
