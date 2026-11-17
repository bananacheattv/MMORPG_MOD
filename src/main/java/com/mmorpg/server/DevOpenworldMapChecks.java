package com.mmorpg.server;

import com.google.gson.JsonParser;
import com.mmorpg.MMORPG;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.nio.file.Files;
import java.nio.file.Path;

/** Explicit opt-in verification of a completed map after a real server restart. */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class DevOpenworldMapChecks {
    private static void check(boolean value, String detail) {
        if (!value) throw new AssertionError("Openworld map: " + detail);
    }

    @SubscribeEvent
    public static void run(ServerStartedEvent event) throws Exception {
        String file = System.getProperty("mmorpg.openworldMapChecks", "");
        if (file.isBlank()) return;
        var server = event.getServer();
        var world = server.overworld();
        Path report = Path.of("openworld-map-checks.txt");
        try {
            var data = JsonParser.parseString(Files.readString(Path.of(file))).getAsJsonObject();
            check(world.getWorldBorder().getSize() == 800, "world border must be 800");
            var spawn = new BlockPos(0, 84, 0);
            world.getChunkAt(spawn);
            check(world.getBlockState(spawn).isAir() && world.getBlockState(spawn.above()).isAir(), "arrival headroom");
            check(world.getBlockState(spawn.below()).isFaceSturdy(world, spawn.below(), net.minecraft.core.Direction.UP), "arrival floor");
            int samples = 0, biomes = 0, signs = 0;
            for (var element : data.getAsJsonArray("samples")) {
                var sample = element.getAsJsonObject();
                var pos = pos(sample);world.getChunkAt(pos);
                var expected = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, sample.get("state").getAsString(), false).blockState();
                check(world.getBlockState(pos) == expected, sample.get("name").getAsString() + " at " + pos);
                samples++;
            }
            for (var element : data.getAsJsonArray("biomes")) {
                var sample = element.getAsJsonObject();var pos = pos(sample);world.getChunkAt(pos);
                check(world.getBiome(pos).getRegisteredName().equals(sample.get("biome").getAsString()), "biome at " + pos);
                biomes++;
            }
            for (var element : data.getAsJsonArray("signs")) {
                var sample = element.getAsJsonObject();var pos = pos(sample);world.getChunkAt(pos);
                check(world.getBlockEntity(pos) instanceof SignBlockEntity, "sign entity at " + pos);
                var sign = (SignBlockEntity) world.getBlockEntity(pos);
                var lines = sample.getAsJsonArray("lines");
                for (int i = 0; i < lines.size(); i++) check(sign.getFrontText().getMessage(i, false).getString().equals(lines.get(i).getAsString()), "sign text at " + pos + " line " + i);
                signs++;
            }
            String result = "PASS: saved map reload, safe arrival, 800 border, " + samples + " landmarks/buildings, " + biomes + " biomes, " + signs + " French signs\n";
            Files.writeString(report, result);MMORPG.LOGGER.info("[OPENWORLD MAP] {}", result.trim());
        } catch (Throwable error) {
            Files.writeString(report, "FAIL: " + error + "\n");throw error;
        } finally { server.halt(false); }
    }

    private static BlockPos pos(com.google.gson.JsonObject sample) {
        return new BlockPos(sample.get("x").getAsInt(), sample.get("y").getAsInt(), sample.get("z").getAsInt());
    }
}
