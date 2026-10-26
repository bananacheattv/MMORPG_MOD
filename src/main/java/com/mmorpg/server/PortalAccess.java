package com.mmorpg.server;

import com.mmorpg.MMORPG;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import java.util.*;

/** Server-only, single-use permission issued by a physical portal crossing. */
@EventBusSubscriber(modid=MMORPG.MODID)
public final class PortalAccess {
    private record Pass(String kind, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dimension, net.minecraft.world.phys.Vec3 origin, long expires) { }
    private static final Map<UUID,Pass> PASSES=new HashMap<>();
    public static void grant(ServerPlayer p,String kind) { PASSES.put(p.getUUID(),new Pass(kind,p.level().dimension(),p.position(),System.nanoTime()+60_000_000_000L)); }
    public static boolean has(ServerPlayer p,String kind) {
        Pass pass=PASSES.get(p.getUUID());
        return pass!=null&&pass.kind().equals(kind)&&pass.dimension().equals(p.level().dimension())&&System.nanoTime()<pass.expires()&&p.position().distanceToSqr(pass.origin())<=64;
    }
    public static boolean require(ServerPlayer p,String kind) {
        if(has(p,kind))return true;
        p.sendOverlayMessage(Component.literal("Traversez un portail pour accéder aux mines, aux plaines ou aux donjons.").withColor(0xFFD060));return false;
    }
    public static boolean consume(ServerPlayer p,String kind) { if(!require(p,kind))return false;PASSES.remove(p.getUUID());return true; }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent event) { PASSES.remove(event.getEntity().getUUID()); }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { PASSES.clear(); }
}
