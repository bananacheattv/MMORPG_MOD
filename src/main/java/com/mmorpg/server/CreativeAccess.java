package com.mmorpg.server;
import com.mmorpg.MMORPG;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
@EventBusSubscriber(modid=MMORPG.MODID)
public final class CreativeAccess {
 public static boolean allowed(ServerPlayer p){return p.level().getServer().getPlayerList().isOp(new net.minecraft.server.players.NameAndId(p.getGameProfile()));}
 public static void enforce(ServerPlayer p){if(p.isCreative()&&!allowed(p))p.setGameMode(GameType.SURVIVAL);}
 @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.PlayerTickEvent.Pre e){if(e.getEntity() instanceof ServerPlayer p)enforce(p);}
 @SubscribeEvent public static void login(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)enforce(p);}
}
