package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.block.DecorModelBlock;
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
 * Banc de mesure des FPS (-Pshowcase=perf) : scene chargee (decors 3D, mobs, PNJ, HUD et minimap), camera fixe,
 * puis moyenne du temps par image dans le journal ([PERF BENCH]). Sert a comparer avant/apres une optimisation.
 */
final class DevPerfBench {
    private static final List<DecorCitadelList.Entry> DECORS = Stream.concat(DecorCitadelList.ALL.stream(), DecorBlockbenchList.ALL.stream()).toList();
    private static final String[] MOBS = {"gobelin", "loup_sombre", "squelette_maudit", "orc_guerrier", "zombie_des_cryptes", "araignee_venimeuse", "golem_cristal", "pnj"};
    private static int tick, wait;
    private static boolean loadingSeen;
    private static BlockPos origin;
    private static Vec3 eye;
    private static long frames, startNanos;
    private static boolean measuring;

    private DevPerfBench() {
    }

    /** Appele a chaque image rendue. */
    static void frame() {
        if (measuring) frames++;
    }

    static void phase(Minecraft mc) {
        if (LoadingClient.active()) {
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
        if (mc.gui.screen() != null && t < 805) mc.gui.setScreen(null);
        if (t == 5) {
            mc.getWindow().setWindowed(1280, 720);
            mc.player.getAbilities().flying = true;
            origin = new BlockPos(mc.player.getBlockX(), 170, mc.player.getBlockZ() - 60);
            eye = Vec3.atCenterOf(origin.south(70).above(22));
            BlockPos o = origin;
            server.execute(() -> {
                var src = server.createCommandSourceStack().withSuppressedOutput();
                server.getCommands().performPrefixedCommand(src, "time set day");
                server.getCommands().performPrefixedCommand(src, "gamerule doMobSpawning false");
                for (int dx = -70; dx <= 70; dx++)
                    for (int dz = -50; dz <= 60; dz++) sl.setBlock(o.offset(dx, -1, dz), Blocks.SMOOTH_STONE.defaultBlockState(), 2);
                int n = 0;
                for (int gx = -5; gx <= 5; gx++)
                    for (int gz = -4; gz <= 1; gz++) {
                        var e = DECORS.get(n++ % DECORS.size());
                        sl.setBlock(o.offset(gx * 12, 0, gz * 12), DecorBlocks.state(e.id()).setValue(DecorModelBlock.FACING, Direction.SOUTH), 3);
                    }
                int m = 0;
                for (int gx = -12; gx <= 12; gx++)
                    for (int gz = 9; gz <= 14; gz++) {
                        BlockPos p = o.offset(gx * 4, 0, gz * 4);
                        String id = m % 2 == 0 ? MOBS[(m / 2) % MOBS.length] : com.mmorpg.entity.mob.ImportedMobs.ALL[(m / 2) % com.mmorpg.entity.mob.ImportedMobs.ALL.length].key();
                        m++;
                        var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(MMORPG.id(id));
                        var entity = type.create(sl, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                        if (entity == null) continue;
                        entity.setPos(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
                        if (entity instanceof net.minecraft.world.entity.Mob mob) {
                            mob.setNoAi(true);
                            mob.setPersistenceRequired();
                        }
                        sl.addFreshEntity(entity);
                    }
            });
        }
        if (t < 5) return;
        mc.player.setPos(eye.x, eye.y - mc.player.getEyeHeight(), eye.z);
        mc.player.setDeltaMovement(Vec3.ZERO);
        mc.player.getAbilities().flying = true;
        mc.player.setYRot(180);
        mc.player.yRotO = 180;
        mc.player.setXRot(18);
        mc.player.xRotO = 18;
        if (t == 200) {
            frames = 0;
            startNanos = System.nanoTime();
            measuring = true;
        }
        if (t == 500) DevShowcase.shot(mc, "perf_bench");
        if (t == 800) {
            measuring = false;
            double seconds = (System.nanoTime() - startNanos) / 1e9;
            int entities = 0;
            var kinds = new java.util.TreeMap<String, Integer>();
            for (var e : level.entitiesForRendering()) {
                entities++;
                kinds.merge(e.getType().toShortString(), 1, Integer::sum);
            }
            MMORPG.LOGGER.info("[PERF BENCH] entites : {}", kinds);
            MMORPG.LOGGER.info("[PERF BENCH] {} images en {} s : {} ms/image, {} FPS, {} entites", frames, String.format("%.1f", seconds),
                    String.format("%.2f", seconds * 1000 / frames), String.format("%.1f", frames / seconds), entities);
        }
        // controle visuel de la carte du monde (image texturee) : vue normale puis dezoom
        if (t == 810) mc.gui.setScreen(new com.mmorpg.client.screen.WorldMapScreen());
        if (t == 840) DevShowcase.shot(mc, "perf_carte");
        if (t == 850 && mc.gui.screen() != null) for (int i = 0; i < 9; i++) mc.gui.screen().mouseScrolled(0, 0, 0, -1);
        if (t == 880) DevShowcase.shot(mc, "perf_carte_dezoom");
        if (t == 890) DevShowcase.stop(mc);
    }
}
