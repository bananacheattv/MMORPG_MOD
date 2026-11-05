package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.block.DecorModelBlock;
import com.mmorpg.block.DecorPartBlock;
import com.mmorpg.registry.DecorBlockbenchList;
import com.mmorpg.registry.DecorBlocks;
import com.mmorpg.registry.DecorCitadelList;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.stream.Stream;

/**
 * Vitrine des decors (-Pshowcase=decor_citadelle) : scene en plein ciel, poses cote serveur (parties de collision comprises),
 * une capture par groupe de 3, puis controle des hitbox : contour d'une partie eloignee du bloc pose, orientations nord et est.
 */
final class DevDecorShowcase {
    private static final List<DecorCitadelList.Entry> ALL = Stream.concat(DecorCitadelList.ALL.stream(), DecorBlockbenchList.ALL.stream()).toList();
    private static final int PER_GROUP = 3, STEP = 40, GROUPS = (ALL.size() + PER_GROUP - 1) / PER_GROUP;
    private static final int SPACING = 18;
    private static int tick, wait;
    private static boolean loadingSeen;
    private static BlockPos origin;
    private static Vec3 eye;
    private static float pitch = 4, yaw = 180;

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
        var server = mc.getSingleplayerServer();
        if (level == null || mc.player == null || server == null) return;
        var sl = server.getLevel(level.dimension());
        if (sl == null) return;
        int t = ++tick;
        if (mc.gui.screen() != null) mc.gui.setScreen(null);
        if (t == 5) {
            mc.getWindow().setWindowed(1280, 720);
            mc.gui.hud.toggle();
            mc.player.getAbilities().flying = true;
            origin = new BlockPos(mc.player.getBlockX(), 170, mc.player.getBlockZ() - 40);
            eye = Vec3.atCenterOf(origin.south(32).above(7));
            BlockPos o = origin;
            server.execute(() -> {
                for (int dx = -30; dx <= 30; dx++)
                    for (int dz = -10; dz <= 10; dz++) sl.setBlock(o.offset(dx, -1, dz), Blocks.SMOOTH_STONE.defaultBlockState(), 2);
            });
        }
        if (t < 5) return;
        mc.player.setPos(eye.x, eye.y - mc.player.getEyeHeight(), eye.z);
        mc.player.setDeltaMovement(Vec3.ZERO);
        mc.player.getAbilities().flying = true;
        mc.player.setYRot(yaw);
        mc.player.yRotO = yaw;
        mc.player.setXRot(pitch);
        mc.player.xRotO = pitch;
        if (t < 60) return;
        int g = (t - 60) / STEP, step = (t - 60) % STEP;
        if (g < GROUPS) {
            for (int i = 0; i < PER_GROUP && g * PER_GROUP + i < ALL.size(); i++) {
                BlockPos pos = origin.east((i - 1) * SPACING);
                var e = ALL.get(g * PER_GROUP + i);
                if (step == 0) server.execute(() -> sl.setBlock(pos, DecorBlocks.state(e.id()).setValue(DecorModelBlock.FACING, Direction.SOUTH), 3));
                if (step == 34) server.execute(() -> sl.setBlock(pos, Blocks.AIR.defaultBlockState(), 3));
            }
            if (step == 26) DevShowcase.shot(mc, "decor_" + (g + 1));
            return;
        }
        int h = t - (60 + GROUPS * STEP);
        hitboxes(mc, server, sl, h);
    }

    /** Deux gargouilles (nord, est) : on vise une partie de la tete, le contour montre la collision a cet endroit. */
    private static void hitboxes(Minecraft mc, net.minecraft.server.MinecraftServer server, net.minecraft.server.level.ServerLevel sl, int h) {
        BlockPos north = origin.west(8), east = origin.east(8);
        String id = "deco_cat_gargouille";
        if (h == 1) server.execute(() -> {
            sl.setBlock(north, DecorBlocks.state(id).setValue(DecorModelBlock.FACING, Direction.NORTH), 3);
            sl.setBlock(east, DecorBlocks.state(id).setValue(DecorModelBlock.FACING, Direction.EAST), 3);
        });
        if (h == 20) {                                // cellule de la tete : 2 blocs en avant, 4 de haut
            check(sl, north, north.offset(0, 9, -1), "nord");
            check(sl, east, east.offset(1, 9, 0), "est");
            aim(north.offset(0, 9, -1));
        }
        if (h == 40) DevShowcase.shot(mc, "decor_hitbox_nord");
        if (h == 50) aim(east.offset(1, 9, 0));
        if (h == 70) DevShowcase.shot(mc, "decor_hitbox_est");
        if (h == 80) server.execute(() -> sl.setBlock(north, Blocks.AIR.defaultBlockState(), 3));
        if (h == 100) {
            MMORPG.LOGGER.info("[DECOR] parties restantes apres retrait : {}", sl.getBlockState(north.offset(0, 9, -1)).getBlock() instanceof DecorPartBlock);
            DevShowcase.stop(mc);
        }
    }

    private static void check(net.minecraft.server.level.ServerLevel sl, BlockPos master, BlockPos part, String name) {
        var st = sl.getBlockState(part);
        MMORPG.LOGGER.info("[DECOR] {} : partie={} forme={}", name, st.getBlock() instanceof DecorPartBlock, st.getShape(sl, part).isEmpty() ? "vide" : st.getShape(sl, part).bounds());
    }

    private static void aim(BlockPos target) {
        eye = Vec3.atCenterOf(target).add(7, 2, 7);
        Vec3 d = Vec3.atCenterOf(target).subtract(eye);
        yaw = (float) (Math.toDegrees(Math.atan2(d.z, d.x)) - 90);
        pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.sqrt(d.x * d.x + d.z * d.z)));
    }
}
