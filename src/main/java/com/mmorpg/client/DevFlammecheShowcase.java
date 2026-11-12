package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.config.ConfigManager;
import com.mmorpg.entity.MascotEntity;
import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.server.FlammecheScenes;
import com.mmorpg.server.QuestManager;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.client.Minecraft;

/**
 * Atelier de captures (dev uniquement) des mises en scene de Flammeche, jouees pour de vrai par le serveur integre :
 * quete QP-002 -> eclosion de l'oeuf, fin de l'Eveil QP-063 -> Flammeche grandit, puis depart vers le Seuil, et carte de chapitre.
 */
final class DevFlammecheShowcase {
    private static int tick, wait, phaseTick, lastEgg;
    private static boolean loadingSeen, eggSeen, hatched, awakened;
    private static int hatchAt = -1, awakenAt = -1;

    private DevFlammecheShowcase() {
    }

    private static MascotEntity mascot(Minecraft mc) {
        for (var e : mc.level.entitiesForRendering()) if (e instanceof MascotEntity m && m.ownerId() == mc.player.getId()) return m;
        return null;
    }

    private static void server(Minecraft mc, java.util.function.Consumer<net.minecraft.server.level.ServerPlayer> job) {
        var srv = mc.getSingleplayerServer();
        srv.execute(() -> job.accept(srv.getPlayerList().getPlayer(mc.player.getUUID())));
    }

    static void phase(Minecraft mc) {
        if (LoadingClient.active()) { loadingSeen = true; wait = 0; return; }
        if (!loadingSeen && ++wait < 1800) return;
        if (mc.player == null || mc.level == null) return;
        if (mc.gui.screen() != null && tick < 400 && !(mc.gui.screen() instanceof com.mmorpg.client.screen.StorySceneScreen)) mc.gui.setScreen(null);
        int t = ++tick;
        if (t == 5) {
            mc.getWindow().setWindowed(1280, 720);
            mc.player.connection.sendCommand("time set day");
            mc.player.connection.sendCommand("weather clear");
        }
        if (t == 40) server(mc, p -> {
            var d = RpgPlayers.get(p);
            d.playerClass = PlayerClass.GUERRIER;
            d.level = 3;
            d.completedQuests.addAll(com.mmorpg.quest.DefaultQuests.tutorialIds());
            d.completedQuests.add("qp_001");
            d.activeQuests.put(FlammecheScenes.HATCH_QUEST, new int[1]);
            d.dirty = true;
            // plateforme dans le ciel (loin des monstres du spawn) pour voir la scene
            var lvl = p.level();
            var base = p.blockPosition().atY(200);
            for (int dx = -7; dx <= 7; dx++) for (int dz = -7; dz <= 7; dz++) {
                lvl.setBlockAndUpdate(base.offset(dx, -1, dz), (dx + dz & 1) == 0 ? net.minecraft.world.level.block.Blocks.POLISHED_DEEPSLATE.defaultBlockState()
                        : net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState());
                for (int dy = 0; dy < 6; dy++) lvl.setBlockAndUpdate(base.offset(dx, dy, dz), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            }
            p.teleportTo(base.getX() - 3.5, base.getY(), base.getZ() + 0.5);
            p.setYRot(-90);
            MMORPG.LOGGER.info("[FLAMMECHE] QP-002 active, quetes du lore chargees : {}", ConfigManager.quests().keySet().stream().filter(k -> k.startsWith("qp_")).count());
        });
        MascotEntity m = t > 40 ? mascot(mc) : null;
        if (m != null && !(mc.gui.screen() instanceof com.mmorpg.client.screen.StorySceneScreen)) {      // regard sur Flammeche
            var to = m.position().add(0, 0.6 * m.growth(), 0).subtract(mc.player.getEyePosition());
            mc.player.setYRot((float) Math.toDegrees(Math.atan2(-to.x, to.z)));
            mc.player.setXRot((float) -Math.toDegrees(Math.atan2(to.y, Math.hypot(to.x, to.z))));
        }
        int egg = m == null ? 0 : m.egg();
        if (egg > 0 && !eggSeen) { eggSeen = true; MMORPG.LOGGER.info("[FLAMMECHE] oeuf visible"); }
        if (eggSeen && !hatched) {
            if (lastEgg < 35 && egg >= 35) Screenshotter.shot(mc, "flammeche_1_oeuf");
            if (lastEgg < 92 && egg >= 92) Screenshotter.shot(mc, "flammeche_2_fissure");
            if (lastEgg > 0 && egg == 0) { hatched = true; hatchAt = t; MMORPG.LOGGER.info("[FLAMMECHE] eclosion"); }
        }
        lastEgg = egg;
        if (hatched && t == hatchAt + 3) Screenshotter.shot(mc, "flammeche_3_eclosion");
        if (hatched && t == hatchAt + 50) {
            Screenshotter.shot(mc, "flammeche_4_ne");
            server(mc, p -> MMORPG.LOGGER.info("[FLAMMECHE] QP-002 validee : {}", QuestManager.completable(p, RpgPlayers.get(p), FlammecheScenes.HATCH_QUEST)));
        }
        // Eveil : QP-063 terminee -> Flammeche grandit (stade 2) ; QP-012 deja terminee -> il repart ensuite vers le Seuil
        if (hatched && t == hatchAt + 70) server(mc, p -> {
            var d = RpgPlayers.get(p);
            for (int i = 2; i <= 62; i++) d.completedQuests.add(String.format("qp_%03d", i));
            d.activeQuests.remove(FlammecheScenes.HATCH_QUEST);
            d.level = 25;
            var q = ConfigManager.quests().get("qp_063");
            d.activeQuests.put("qp_063", new int[q.objectives.size()]);
            com.mmorpg.server.QuestManager.finishForChecks(p, d, "qp_063", q);
            awakened = true;
        });
        if (awakened && awakenAt < 0) awakenAt = t;
        if (awakenAt > 0 && t == awakenAt + 45) Screenshotter.shot(mc, "flammeche_5_eveil_spirale");
        if (awakenAt > 0 && t == awakenAt + 100) Screenshotter.shot(mc, "flammeche_6_eveil_stade2");
        if (awakenAt > 0 && t == awakenAt + 100) MMORPG.LOGGER.info("[FLAMMECHE] taille apres l'Eveil : {}", m == null ? -1 : m.growth());
        if (awakenAt > 0 && t == awakenAt + 275) Screenshotter.shot(mc, "flammeche_7_depart");
        if (awakenAt > 0 && t == awakenAt + 330) MMORPG.LOGGER.info("[FLAMMECHE] apres le depart, mascotte presente : {}", m != null);
        if (awakenAt > 0 && t == awakenAt + 340) mc.gui.setScreen(new com.mmorpg.client.screen.StorySceneScreen("lore_ch5"));
        if (awakenAt > 0 && t == awakenAt + 420) Screenshotter.shot(mc, "flammeche_8_carte_chapitre");
        if (awakenAt > 0 && t == awakenAt + 440) { MMORPG.LOGGER.info("[FLAMMECHE] FIN"); mc.stop(); }
        if (t > 4000) { MMORPG.LOGGER.info("[FLAMMECHE] delai depasse"); mc.stop(); }
    }

    /** Capture via l'outil de l'atelier. */
    private static final class Screenshotter {
        static void shot(Minecraft mc, String name) {
            DevShowcase.shot(mc, name);
        }
    }
}
