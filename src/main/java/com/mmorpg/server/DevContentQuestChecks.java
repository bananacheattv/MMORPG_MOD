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
            // classe : seule la chaine de sa classe, dans l'ordre (classes du mod, inchangees)
            check(quests.keySet().stream().filter(k -> k.startsWith("classe_")).count() == 44, "44 quetes de classe");
            check(QuestManager.isAvailable(d, "classe_mage_0", quests.get("classe_mage_0"), "maitre_classes"), "mage : premiere epreuve");
            check(!QuestManager.isAvailable(d, "classe_guerrier_0", quests.get("classe_guerrier_0"), "maitre_classes"), "mage : pas la chaine du guerrier");
            check(!QuestManager.isAvailable(d, "classe_mage_1", quests.get("classe_mage_1"), "maitre_classes"), "epreuve 2 exige la 1");
            d.completedQuests.add("classe_mage_0");
            check(QuestManager.isAvailable(d, "classe_mage_1", quests.get("classe_mage_1"), "maitre_classes"), "epreuve 2 debloquee");
            check(quests.values().stream().filter(x -> x.lore != null).allMatch(x -> x.classes.isEmpty() && x.rewards.evolution == 0), "aucune classe du lore");
            var evo = quests.get("qp_063");
            check(evo.auto && evo.minLevel == 25, "Eveil 25 dans la trame");

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
            // trame principale du lore : proposee dans l'ordre, rendue automatiquement
            var p1 = quests.get("qp_001");
            check(p1 != null && p1.auto && p1.giver.equals("histoire") && p1.lore.donorKey.equals("ysmeralde"), "QP-001");
            check(!QuestManager.isAvailable(d, "qp_001", p1, "histoire"), "trame apres le tutoriel");
            d.completedQuests.add("tuto_fin");
            check(QuestManager.isAvailable(d, "qp_001", p1, "histoire"), "QP-001 dispo");
            check(!QuestManager.isAvailable(d, "qp_002", quests.get("qp_002"), "histoire"), "QP-002 apres QP-001");
            d.activeQuests.put("qp_001", new int[]{0});
            check(!QuestManager.completable(p, d, "qp_001"), "dialogue non fait");
            QuestManager.onAction(p, "lore:ysmeralde", 1);
            check(QuestManager.completable(p, d, "qp_001"), "dialogue avec le donneur");
            QuestManager.finish(p, d, "qp_001", p1);
            check(d.completedQuests.contains("qp_001") && QuestManager.isAvailable(d, "qp_002", quests.get("qp_002"), "histoire"), "QP-002 apres QP-001");
            // choix de classe : deja fait, rendu d'office
            var p3 = quests.get("qp_003");
            check(p3.auto && p3.objectives.isEmpty(), "QP-003 d'office");
            // objectif suivi : chasse
            var hunt = quests.values().stream().filter(x -> x.lore != null && x.objectives.size() == 1 && x.objectives.get(0).type.equals("kill") && x.objectives.get(0).target.equals("*")
                    && x.objectives.get(0).count <= 20 && !x.daily && x.classes.isEmpty()).findFirst().orElseThrow();
            String hid = quests.entrySet().stream().filter(e -> e.getValue() == hunt).findFirst().orElseThrow().getKey();
            d.activeQuests.put(hid, new int[]{0});
            for (int i = 0; i < hunt.objectives.get(0).count; i++) QuestManager.onKill(p, "gobelin", "mmorpg:gobelin", false);
            check(QuestManager.completable(p, d, hid), "chasse suivie : " + hid);
            d.activeQuests.remove(hid);
            // quetes repetables : quotidienne et hebdomadaire
            var weekly = quests.entrySet().stream().filter(e -> "weekly".equals(e.getValue().repeat)).findFirst().orElseThrow();
            d.level = 100;
            String grp = weekly.getValue().giver;
            check(QuestManager.isAvailable(d, weekly.getKey(), weekly.getValue(), grp), "hebdomadaire disponible");
            d.activeQuests.put(weekly.getKey(), new int[weekly.getValue().objectives.size()]);
            QuestManager.finish(p, d, weekly.getKey(), weekly.getValue());
            check(!d.completedQuests.contains(weekly.getKey()) && !QuestManager.isAvailable(d, weekly.getKey(), weekly.getValue(), grp), "hebdomadaire verrouillee jusqu'a lundi");
            d.dailyQuests.put(weekly.getKey(), QuestManager.weekIndex() - 1);
            check(QuestManager.isAvailable(d, weekly.getKey(), weekly.getValue(), grp), "hebdomadaire de retour la semaine suivante");
            // evenement : fenetre de dates
            var ev = quests.get("ev_noel_01");
            check(ev.lore.from.equals("2026-12-01") && ev.lore.to.equals("2027-01-06"), "dates de l'evenement");
            ev.lore.from = "2000-01-01"; ev.lore.to = "2000-01-02";
            check(!QuestManager.windowOpen(ev) && !QuestManager.isAvailable(d, "ev_noel_01", ev, ev.giver), "evenement ferme hors dates");
            ev.lore.from = "2000-01-01"; ev.lore.to = "2999-01-01";
            check(QuestManager.windowOpen(ev) && QuestManager.isAvailable(d, "ev_noel_01", ev, ev.giver), "evenement ouvert dans les dates");
            d.level = 30;

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
            check(QuestManager.extraTag(p, d).getListOrEmpty("story").size() == 1, "sync extra : carte du chapitre 1 rejouable");
            MMORPG.LOGGER.info("[CODEX CHECKS] CONTENT QUESTS PASS: classes du mod (44), lore (Eveil, trame, chasse, hebdo, evenement), quotidiennes, exploration (20 lieux)");
        } catch (Throwable t) {
            MMORPG.LOGGER.error("[CODEX CHECKS] CONTENT QUESTS FAIL", t);
        } finally {
            d.load(snapshot);
            d.dirty = true;
        }
    }
}
