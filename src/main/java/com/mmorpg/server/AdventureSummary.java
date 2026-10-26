package com.mmorpg.server;

import com.mmorpg.MMORPG;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent;

/** Acquisition counters persist inside the shared RPG profile, including proxy transfers. */
@EventBusSubscriber(modid=MMORPG.MODID)
public final class AdventureSummary {
    private static final java.util.Map<java.util.UUID,Integer> OUTSIDE=new java.util.HashMap<>();
    public static void begin(ServerPlayer p,String kind) {
        var d=RpgPlayers.get(p);var t=d.adventureSummary;
        if(t.getBooleanOr("active",false)&&t.getStringOr("kind","").equals(kind))return;
        t=new CompoundTag();t.putBoolean("active",true);t.putString("kind",kind);t.putLong("start",System.currentTimeMillis());d.adventureSummary=t;d.dirty=true;OUTSIDE.remove(p.getUUID());
    }
    public static void count(ServerPlayer p,String name,long amount) {
        var d=RpgPlayers.get(p);var t=d.adventureSummary;if(!t.getBooleanOr("active",false)||amount<=0)return;
        t.putLong(name,t.getLongOr(name,0)+amount);d.dirty=true;
    }
    public static void item(ServerPlayer p,Item item,int amount) {
        var d=RpgPlayers.get(p);var t=d.adventureSummary;if(!t.getBooleanOr("active",false)||amount<=0)return;
        String id=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString();var items=t.getCompoundOrEmpty("items");
        items.putLong(id,items.getLongOr(id,0)+amount);t.put("items",items);d.dirty=true;
    }
    @SubscribeEvent public static void pickup(ItemEntityPickupEvent.Post event) {
        if(event.getPlayer() instanceof ServerPlayer p) {
            var original=event.getOriginalStack();item(p,original.getItem(),original.getCount()-event.getCurrentStack().getCount());
        }
    }
    private static String location(ServerPlayer p) {
        if(com.mmorpg.mine.MineManager.regionOf(p)>=0)return "mine";
        if(com.mmorpg.plains.PlainsManager.regionOf(p)>=0)return "plaines";
        if(com.mmorpg.dungeon.InstanceManager.containsPlayer(p))return "donjon";
        return "";
    }
    public static void tick(ServerPlayer p) {
        var d=RpgPlayers.get(p);var t=d.adventureSummary;String here=location(p);
        if(!here.isEmpty()&&!t.getBooleanOr("active",false))begin(p,here);
        t=d.adventureSummary;
        if(!t.getBooleanOr("active",false))return;
        if(!here.isEmpty()) {t.putBoolean("entered",true);OUTSIDE.remove(p.getUUID());return;}
        if(!t.getBooleanOr("entered",false)||com.mmorpg.network.NetworkPlayers.inTransit(p))return;
        int first=OUTSIDE.computeIfAbsent(p.getUUID(),id->p.tickCount);
        if(p.tickCount-first<40||p.tickCount<80)return;
        t.putLong("duration",Math.max(0,System.currentTimeMillis()-t.getLongOr("start",System.currentTimeMillis())));
        t.putBoolean("active",false);d.dirty=true;OUTSIDE.remove(p.getUUID());
        com.mmorpg.network.Net.toPlayer(p,new com.mmorpg.network.S2COpenScreen(com.mmorpg.network.S2COpenScreen.ADVENTURE_SUMMARY,t.copy()));
    }
    @SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event) {OUTSIDE.remove(event.getEntity().getUUID());}
    @SubscribeEvent public static void stop(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {OUTSIDE.clear();}
}
