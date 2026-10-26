package com.mmorpg.server;

import com.mmorpg.MMORPG;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Opt-in private development scene; never enabled by the deployed server command. */
@EventBusSubscriber(modid=MMORPG.MODID)
public final class DevVisualChecks {
    @SubscribeEvent public static void joined(PlayerEvent.PlayerLoggedInEvent event) {
        if(!Boolean.getBoolean("mmorpg.visualChecks")||!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer p))return;
        Scheduler.later(p.level(),40,()->{
            var server=p.level().getServer();var level=server.overworld();
            server.getPlayerList().op(new net.minecraft.server.players.NameAndId(p.getGameProfile()));
            com.mmorpg.hub.CinematicManager.finish(p);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            RpgPlayers.get(p).playerClass=com.mmorpg.rpg.PlayerClass.GUERRIER;
            for(int x=-15;x<=15;x++)for(int z=-10;z<=16;z++)level.setBlockAndUpdate(new BlockPos(x,219,z),net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState());
            p.teleportTo(level,8.5,222,11.5,java.util.Set.of(),145F,10F,true);
            p.getAbilities().flying=true;p.onUpdateAbilities();
            SpawnEditor.open(p,new BlockPos(2,220,0));SpawnEditor.placeOriented(p,"forge_arcanique","moi");
            SpawnEditor.open(p,new BlockPos(-4,220,4));SpawnEditor.placeOriented(p,"caisse_commune_bloc","moi");
            var npc=com.mmorpg.world.DungeonBuilder.spawnNpc(level,new BlockPos(6,220,8),com.mmorpg.entity.NpcEntity.Role.MARCHAND,"Échoppe visuelle",0,0);
            Scheduler.later(level,40,()->MerchantEditor.open(p,npc));
        });
    }
    @SubscribeEvent public static void left(PlayerEvent.PlayerLoggedOutEvent event) {
        if(Boolean.getBoolean("mmorpg.visualChecks")&&event.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)p.level().getServer().halt(false);
    }
    @SubscribeEvent public static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent event) {
        if(Boolean.getBoolean("mmorpg.visualChecks"))System.exit(0);
    }
}
