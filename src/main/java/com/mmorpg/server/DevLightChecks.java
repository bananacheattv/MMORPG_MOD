package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.block.LightEmitterBlock;
import com.mmorpg.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LightLayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.UUID;

/** Opt-in server checks for the invisible light block: ./gradlew runServer -PlightChecks (isolated run/ world only). */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class DevLightChecks {
    private static void check(boolean c, String label) {
        if (!c) throw new AssertionError("Light check failed: " + label);
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void run(ServerStartedEvent event) {
        if (!Boolean.getBoolean("mmorpg.checkLight")) return;
        var server = event.getServer();
        var level = server.overworld();
        var id = new com.mojang.authlib.GameProfile(UUID.randomUUID(), "LightChecks");
        var p = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level, id);
        BlockPos pos = new BlockPos(0, 150, 0);
        try {
            level.getChunk(pos);
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            check(level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.STONE), "stone control " + level.getBlockState(pos) + " minY=" + level.getMinY() + " maxY=" + level.getMaxY());
            level.setBlock(pos, ModBlocks.BLOC_LUMIERE.get().defaultBlockState().setValue(LightEmitterBlock.LEVEL, 0), 3);
            check(level.getBlockState(pos).getBlock() instanceof LightEmitterBlock, "placed " + level.getBlockState(pos));
            check(level.getBlockState(pos).getLightEmission() == 0, "level 0 emits nothing");
            server.getPlayerList().op(new net.minecraft.server.players.NameAndId(id));
            for (int lvl : new int[]{1, 7, 15, 3}) {
                var t = new CompoundTag();
                t.putLong("pos", pos.asLong());
                t.putInt("level", lvl);
                p.snapTo(pos.getX() + .5, pos.getY(), pos.getZ() + 2.5, 0, 0);
                LightEmitterBlock.apply(p, t);
                check(level.getBlockState(pos).getValue(LightEmitterBlock.LEVEL) == lvl, "op sets level " + lvl);
                check(level.getBlockState(pos).getLightEmission() == lvl, "emission " + lvl);
            }
            var t = new CompoundTag();
            t.putLong("pos", pos.asLong());
            t.putInt("level", 99);
            LightEmitterBlock.apply(p, t);
            check(level.getBlockState(pos).getValue(LightEmitterBlock.LEVEL) == 15, "level clamped to 15");
            check(LightEmitterBlock.class != null && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty(), "no collision");
            server.getPlayerList().deop(new net.minecraft.server.players.NameAndId(id));
            t.putInt("level", 2);
            LightEmitterBlock.apply(p, t);
            check(level.getBlockState(pos).getValue(LightEmitterBlock.LEVEL) == 15, "non-op rejected");
            check(DevLightChecks.class.getResource("/assets/mmorpg/blockstates/bloc_lumiere.json") != null
                    && DevLightChecks.class.getResource("/assets/mmorpg/items/bloc_lumiere.json") != null, "resources");
            MMORPG.LOGGER.info("[CODEX CHECKS] LIGHT PASS: niveaux 0-15, emission, clamp, refus non-op, sans collision");
        } catch (Throwable t) {
            MMORPG.LOGGER.error("[CODEX CHECKS] LIGHT FAIL", t);
        } finally {
            server.halt(false);
        }
    }
}
