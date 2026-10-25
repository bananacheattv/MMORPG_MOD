package com.mmorpg.dungeon;

import com.mmorpg.network.NetworkConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;

/** Small safe waiting room, independent of the MAIN spawn and its economy/NPCs. */
public final class DungeonLobby {
    private static final BlockPos HOME = new BlockPos(0, 100, 0);
    public static void ensure(MinecraftServer server) {
        if (!NetworkConfig.current.enabled || NetworkConfig.current.role != NetworkConfig.Role.DUNGEONS) return;
        var level = server.overworld();
        var floor = com.mmorpg.registry.DecorBlocks.get("marbre_dore");
        if (!level.getBlockState(HOME.below()).is(floor)) {
            for (int x = -5; x <= 5; x++) for (int z = -5; z <= 5; z++) {
                level.setBlock(new BlockPos(x, 99, z), floor.defaultBlockState(), 2);
                for (int y = 100; y <= 103; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 2);
                if (Math.abs(x) == 5 || Math.abs(z) == 5) level.setBlock(new BlockPos(x, 100, z), Blocks.STONE_BRICK_WALL.defaultBlockState(), 2);
            }
        }
        level.setRespawnData(net.minecraft.world.level.storage.LevelData.RespawnData.of(Level.OVERWORLD, HOME, 0, 0));
        com.mmorpg.zone.ZoneData.get(server).add(new com.mmorpg.zone.ZoneData.Zone("dungeon_lobby", "Accueil des donjons", Level.OVERWORLD, -6, -6, 6, 6,
                true, true, true, com.mmorpg.zone.ZoneData.Zone.STYLE_ZONE, "Chargement de votre instance · /retourdonjon pour revenir à votre entrée"));
    }
    public static void arriveIfNeeded(ServerPlayer p) {
        if (NetworkConfig.current.enabled && NetworkConfig.current.role == NetworkConfig.Role.DUNGEONS
                && (com.mmorpg.network.NetworkPlayers.isArriving(p) || !p.level().dimension().equals(InstanceManager.DIM))) {
            p.teleportTo(p.level().getServer().overworld(), .5, 100, .5, java.util.Set.of(), 0, 0, true); p.resetFallDistance();
        }
    }
}
