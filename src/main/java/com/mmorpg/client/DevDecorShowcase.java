package com.mmorpg.client;

import com.mmorpg.registry.DecorBlocks;
import com.mmorpg.registry.DecorBlockbenchList;
import com.mmorpg.registry.DecorCitadelList;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;

/** Vitrine des decors de la citadelle (-Pshowcase=decor_citadelle) : groupes de 5, une capture par groupe. */
final class DevDecorShowcase {
    private static final java.util.List<DecorCitadelList.Entry> ALL = java.util.stream.Stream.concat(DecorCitadelList.ALL.stream(), DecorBlockbenchList.ALL.stream()).toList();
    private static final int GROUPS = (ALL.size() + 4) / 5;
    private static int tick, wait;
    private static boolean loadingSeen;
    private static BlockPos origin;
    private static Direction look = Direction.NORTH;

    private DevDecorShowcase() {
    }

    static void phase(Minecraft mc) {
        if (com.mmorpg.client.LoadingClient.active()) {
            loadingSeen = true;
            wait = 0;
            return;
        }
        if (!loadingSeen && ++wait < 1800) return;
        var level = mc.level;
        if (level == null || mc.player == null) return;
        int t = ++tick;
        if (mc.gui.screen() != null) mc.gui.setScreen(null);
        if (t == 5) {
            mc.getWindow().setWindowed(1280, 720);
            mc.player.setYRot(0);
            mc.player.setXRot(6);
            mc.gui.hud.toggle();
        }
        if (t == 8) {
            look = Direction.fromYRot(mc.player.getYRot());
            origin = mc.player.blockPosition().relative(look, 15);
            Direction right = look.getClockWise();
            for (int dx = -22; dx <= 22; dx++)
                for (int dz = -8; dz <= 8; dz++) level.setBlock(origin.relative(right, dx).relative(look, dz).below(), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
        }
        int g = (t - 20) / 50, step = (t - 20) % 50;
        if (t < 20 || g >= GROUPS) {
            if (t == 20 + GROUPS * 50) DevShowcase.stop(mc);
            return;
        }
        Direction right = look.getClockWise();
        for (int i = 0; i < 5; i++) {
            BlockPos pos = origin.relative(right, (i - 2) * 9);
            if (step == 0) {
                var e = ALL.get(g * 5 + i);
                level.setBlock(pos, DecorBlocks.state(e.id()).setValue(com.mmorpg.block.DecorModelBlock.FACING, look.getOpposite()), 3);
            }
            if (step == 45) level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
        if (step == 30) DevShowcase.shot(mc, "decor_" + (g + 1));
    }
}
