package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.config.ConfigManager;
import com.mmorpg.quest.DefaultQuests;
import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.rpg.PlayerData;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.UUID;

/** Verifications serveur (opt-in) : quetes de classe, quotidiennes aleatoires, histoire, exploration. ./gradlew runServer -PcontentQuestChecks */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class DevContentQuestChecks {
    private DevContentQuestChecks() {
    }

    private static void check(boolean c, String label) {
        if (!c) throw new AssertionError("Content quest check failed: " + label);
    }

    @SubscribeEvent
    public static void started(ServerStartedEvent event) {
        if (!Boolean.getBoolean("mmorpg.checkContentQuests")) return;
        var server = event.getServer();
        run(FakePlayerFactory.get(server.overworld(), new GameProfile(UUID.randomUUID(), "ContentQuestChecks")));
        server.halt(false);
    }

    private static void run(ServerPlayer p) {
        var d = RpgPlayers.get(p);
        var snapshot = d.save();
        try {
            d.load(new PlayerData().save());
            d.playerClass = PlayerClass.MAGE;
            d.level = 30;
            var quests = DefaultQuests.create();
            ConfigManager.quests().putAll(quests);
            // classe : seule la chaine de sa classe, dans l'ordre
            check(quests.keySet().stream().filter(k -> k.startsWith("classe_")).count() == 44, "44 quetes de classe");
            check(QuestManager.isAvailable(d, "classe_mage_0", quests.get("classe_mage_0"), "maitre_classes"), "mage : premiere epreuve");
            check(!QuestManager.isAvailable(d, "classe_guerrier_0", quests.get("classe_guerrier_0"), "maitre_classes"), "mage : pas la chaine du guerrier");
            check(!QuestManager.isAvailable(d, "classe_mage_1", quests.get("classe_mage_1"), "maitre_classes"), "epreuve 2 exige la 1");
            d.completedQuests.add("classe_mage_0");
            check(QuestManager.isAvailable(d, "classe_mage_1", quests.get("classe_mage_1"), "maitre_classes"), "epreuve 2 debloquee");
            check(quests.get("classe_mage_10").minLevel == 100, "chaine jusqu'au niveau 100");

            // quotidiennes aleatoires
            check(DailyQuests.offersTag(p, d).size() == 3, "3 offres du jour");
            String id = DailyQuests.offeredId(p, d, 0);
            DailyQuests.accept(p, id);
            check(d.activeQuests.containsKey(id), "accept quotidienne");
            DailyQuests.accept(p, DailyQuests.id(QuestManager.today() - 1, 0, 0, 0));
            check(d.activeQuests.size() == 1, "quete d'hier refusee");
            check(DailyQuests.offersTag(p, d).size() == 2, "offre prise retiree");
            DailyQuests.claim(p, id);
            check(d.activeQuests.containsKey(id), "remise refusee avant les objectifs");
            var q = DailyQuests.def(id);
            for (var o : q.objectives) {
                if (o.type.equals("kill")) for (int i = 0; i < o.count; i++) QuestManager.onKill(p, "gobelin", "mmorpg:gobelin", false);
                else p.getInventory().add(new net.minecraft.world.item.ItemStack(com.mmorpg.crafting.ForgeRecipes.resolveItem(o.target), o.count));
            }
            if (q.objectives.stream().anyMatch(o -> o.type.equals("kill") && o.target.equals("boss")))
                for (var o : q.objectives) if (o.target.equals("boss")) for (int i = 0; i < o.count; i++) QuestManager.onKill(p, "x", "x", true);
            check(QuestManager.completable(p, d, id), "objectifs quotidienne atteints");
            long gold = d.gold;
            DailyQuests.claim(p, id);
            check(!d.activeQuests.containsKey(id) && d.gold > gold, "quotidienne rendue et payee");
            DailyQuests.accept(p, id);
            check(!d.activeQuests.containsKey(id), "pas de seconde prise le meme jour");
            var reload = new PlayerData();
            reload.load(d.save());
            check(DailyQuests.offersTag(p, reload).size() == 2 || reload.dailyQuests.keySet().stream().anyMatch(k -> k.startsWith(DailyQuests.DONE_PREFIX)), "persistance etat du jour");

            var board = DailyQuests.boardTag(p, d);
            check(board.size() == 3 && board.stream().anyMatch(t -> t instanceof net.minecraft.nbt.CompoundTag c && c.getIntOr("state", 0) == 2), "pancarte : 3 quotidiennes dont une terminee");
            DailyQuests.boardAction(p, WeeklyQuests.ACT_ACCEPT, 1);
            check(DailyQuests.boardTag(p, d).stream().anyMatch(t -> t instanceof net.minecraft.nbt.CompoundTag c && c.getIntOr("state", 0) == 1), "pancarte : acceptation");
            DailyQuests.boardAction(p, WeeklyQuests.ACT_ABANDON, 1);
            // histoire : proposee dans l'ordre, rendue automatiquement
            var h1 = quests.get("histoire_1");
            check(h1 != null && h1.story.equals("hist_1"), "chapitre 1");
            check(!QuestManager.isAvailable(d, "histoire_1", h1, "histoire"), "histoire apres le tutoriel");
            d.completedQuests.add("tuto_fin");
            check(QuestManager.isAvailable(d, "histoire_1", h1, "histoire"), "chapitre 1 dispo");
            check(!QuestManager.isAvailable(d, "histoire_2", quests.get("histoire_2"), "histoire"), "chapitre 2 apres le 1");
            check(quests.keySet().stream().filter(k -> k.startsWith("histoire_")).count() == 12, "12 chapitres");
            for (String k : quests.keySet()) if (k.startsWith("histoire_")) check(com.mmorpg.quest.StoryBook.get(quests.get(k).story) != null, k + " scene");
            check(com.mmorpg.quest.StoryBook.get(com.mmorpg.quest.StoryBook.EPILOGUE) != null, "epilogue");
            d.activeQuests.put("histoire_1", new int[]{10});
            check(QuestManager.completable(p, d, "histoire_1"), "chapitre 1 termine");
            QuestManager.finish(p, d, "histoire_1", h1);
            check(d.completedQuests.contains("histoire_1") && StoryQuests.tag(p, d).size() == 1, "chapitre rejouable");

            // exploration cachee
            p.snapTo(p.getX(), 200, p.getZ(), 0, 0);
            check(ExplorationQuests.found(d) == 0, "rien de decouvert");
            check(ExplorationQuests.tag(p, d).isEmpty(), "lieux caches tant que non trouves");
            check(ExplorationQuests.discover(p, d, ExplorationQuests.SPOTS.get(0)), "decouverte");
            check(!ExplorationQuests.discover(p, d, ExplorationQuests.SPOTS.get(0)), "une seule fois");
            check(ExplorationQuests.found(d) == 1 && ExplorationQuests.tag(p, d).size() == 1, "un lieu liste");
            var re2 = new PlayerData();
            re2.load(d.save());
            check(ExplorationQuests.found(re2) == 1, "persistance exploration");
            check(QuestManager.extraTag(p, d).getListOrEmpty("story").size() == 1, "sync extra");
            MMORPG.LOGGER.info("[CODEX CHECKS] CONTENT QUESTS PASS: classe x4 (44), quotidiennes, histoire (12), exploration (20 lieux)");
        } catch (Throwable t) {
            MMORPG.LOGGER.error("[CODEX CHECKS] CONTENT QUESTS FAIL", t);
        } finally {
            d.load(snapshot);
            d.dirty = true;
        }
    }
}
