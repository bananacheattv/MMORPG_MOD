package com.mmorpg.quest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mmorpg.config.DefaultMobs;
import com.mmorpg.config.MobConfig;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Validation des quetes par defaut : faisabilite, prerequis sans cycle, progression par niveau et espacement des PNJ. */
class QuestPlanTest {
    private static final Map<String, QuestDef> QUESTS = DefaultQuests.create(tier -> "parchemin_plan_test_" + tier);
    private static final Map<String, MobConfig> MOBS = DefaultMobs.core();
    /** Objets vanilla qu'un joueur peut se procurer (butin de bandit, marchand de minerais). */
    private static final Set<String> VANILLA_OK = Set.of("minecraft:iron_ingot");

    private static String itemKey(String id) {
        return id.startsWith("mmorpg:") ? id.substring(7) : id;
    }

    private static boolean itemExists(String id) {
        if (itemKey(id).startsWith("parchemin_plan_test_")) return true;
        return QuestPlanTest.class.getResource("/assets/mmorpg/items/" + itemKey(id) + ".json") != null;
    }

    @Test
    void everyQuestIsWellFormed() {
        assertFalse(QUESTS.isEmpty());
        for (var e : QUESTS.entrySet()) {
            var q = e.getValue();
            String id = e.getKey();
            assertFalse(q.name.isBlank(), id + " sans nom");
            assertFalse(q.description.isBlank(), id + " sans description");
            assertFalse(q.objectives.isEmpty(), id + " sans objectif");
            assertTrue(q.minLevel >= 1 && q.minLevel <= 100, id + " niveau hors bornes");
            assertEquals(DefaultQuests.REVISION, q.revision, id + " revision");
            assertTrue(q.rewards.xp > 0 || q.rewards.xpPercentOfLevel > 0 || q.rewards.gold > 0 || !q.rewards.items.isEmpty() || q.rewards.evolution > 0, id + " sans recompense");
            for (var r : q.rewards.items) {
                assertTrue(r.count > 0, id + " recompense vide");
                assertTrue(itemExists(r.item), id + " recompense inconnue : " + r.item);
            }
        }
    }

    @Test
    void objectivesAreFeasibleAtTheQuestLevel() {
        for (var e : QUESTS.entrySet()) {
            var q = e.getValue();
            for (var o : q.objectives) {
                String where = e.getKey() + " / " + o.type + " " + o.target;
                assertTrue(o.count > 0, where + " : quantite nulle");
                switch (o.type) {
                    case "kill" -> {
                        if (o.target.equals("*") || o.target.equals("boss")) break;
                        MobConfig mob = MOBS.get(o.target);
                        assertNotNull(mob, where + " : monstre inexistant");
                        if (mob.boss) {
                            assertTrue(Math.abs(mob.minLevel - q.minLevel) <= 5, where + " : niveau du boss (" + mob.minLevel + ") trop eloigne de la quete (" + q.minLevel + ")");
                        } else {
                            assertTrue(mob.spawn.enabled, where + " : monstre qui n'apparait pas");
                            assertTrue(mob.minLevel <= q.minLevel + 8, where + " : monstre trop fort (" + mob.minLevel + ") pour le niveau " + q.minLevel);
                            assertTrue(mob.maxLevel + 25 >= q.minLevel, where + " : monstre trop faible pour le niveau " + q.minLevel);
                        }
                    }
                    case "collect" -> {
                        if (VANILLA_OK.contains(o.target)) break;
                        String key = itemKey(o.target);
                        assertTrue(itemExists(o.target), where + " : objet inexistant");
                        boolean dropped = MOBS.values().stream().anyMatch(m -> !m.boss && m.spawn.enabled && m.minLevel <= q.minLevel + 15
                                && m.drops.stream().anyMatch(d -> itemKey(d.item).equals(key) && d.chance > 0));
                        assertTrue(dropped, where + " : aucun monstre accessible ne le laisse");
                    }
                    case "level" -> assertTrue(o.count <= 100 && o.count >= q.minLevel, where + " : niveau cible incoherent");
                    case "visit" -> assertTrue(q.npc != null && !q.npc.isEmpty(), where + " : lieu sans PNJ");
                    default -> throw new AssertionError(where + " : type inconnu");
                }
            }
        }
    }

    @Test
    void prerequisitesExistHaveNoCycleAndProgressByLevel() {
        for (var e : QUESTS.entrySet()) {
            var q = e.getValue();
            assertFalse(q.requires.contains(e.getKey()), e.getKey() + " se requiert lui-meme");
            for (String r : q.requires) {
                var pre = QUESTS.get(r);
                assertNotNull(pre, e.getKey() + " exige une quete inexistante : " + r);
                assertFalse(pre.daily, e.getKey() + " exige une quete journaliere");
                assertTrue(pre.minLevel <= q.minLevel, e.getKey() + " (niv. " + q.minLevel + ") exige " + r + " de niveau superieur (" + pre.minLevel + ")");
            }
        }
        // tri topologique : tout est atteignable depuis les quetes sans prerequis
        Map<String, Integer> pending = new HashMap<>();
        QUESTS.forEach((id, q) -> pending.put(id, q.requires.size()));
        var queue = new ArrayDeque<String>();
        pending.forEach((id, n) -> { if (n == 0) queue.add(id); });
        Set<String> done = new HashSet<>();
        while (!queue.isEmpty()) {
            String id = queue.poll();
            done.add(id);
            QUESTS.forEach((other, q) -> {
                if (q.requires.contains(id) && pending.merge(other, -1, Integer::sum) == 0) queue.add(other);
            });
        }
        assertEquals(QUESTS.keySet(), done, "cycle de prerequis ou quete inaccessible");
    }

    @Test
    void campaignFollowsTheIntroductionChain() {
        // La campagne commence apres la derniere quete d'introduction du spawn.
        assertTrue(QUESTS.get("premiers_pas").requires.contains("spawn_premiere_chasse"));
        assertTrue(QUESTS.get("parchemin_forge_1").requires.contains("spawn_premiere_chasse"));
        for (int i = 1; i < QuestNpcPlan.DISCOVERY_IDS.length; i++) {
            assertTrue(QUESTS.get(QuestNpcPlan.DISCOVERY_IDS[i]).requires.contains(QuestNpcPlan.DISCOVERY_IDS[i - 1]), "chaine du spawn rompue en " + i);
        }
        // Chaque chapitre se debloque apres le precedent (liche -> feu -> neant).
        assertTrue(QUESTS.get("coeur_flammes").requires.contains("liche_ancienne"));
        assertTrue(QUESTS.get("chevaliers_neant").requires.contains("titan_glace"));
        assertTrue(QUESTS.get("avatar_neant").requires.contains("chevaliers_neant"));
    }

    @Test
    void everyQuestHasAGiverThatExists() {
        Set<String> groups = new HashSet<>();
        for (var g : QuestNpcPlan.GIVERS) assertTrue(groups.add(g.group()), "groupe de PNJ en double : " + g.group());
        groups.add("maitre_classes"); // PNJ du hub (HubBuilder)
        for (var e : QUESTS.entrySet()) {
            assertFalse(e.getValue().giver.isEmpty(), e.getKey() + " sans donneur");
            assertTrue(groups.contains(e.getValue().giver), e.getKey() + " : donneur inconnu " + e.getValue().giver);
        }
        // une quete d'introduction est liee a l'identifiant exact de son PNJ
        for (int i = 0; i < QuestNpcPlan.DISCOVERY_IDS.length; i++) {
            var q = QUESTS.get(QuestNpcPlan.DISCOVERY_IDS[i]);
            assertEquals(QuestNpcPlan.GIVERS.get(i).uuid().toString(), q.npc);
            assertEquals(QuestNpcPlan.GIVERS.get(i).group(), q.giver);
            if (i < 4) assertEquals(QuestNpcPlan.GIVERS.get(i + 1).name(), q.objectives.getFirst().target, "lieu a decouvrir");
        }
        // chaque PNJ de la campagne donne au moins une quete
        for (var g : QuestNpcPlan.GIVERS) {
            assertTrue(QUESTS.values().stream().anyMatch(q -> q.giver.equals(g.group())), g.name() + " ne donne aucune quete");
        }
    }

    @Test
    void giversAreSpacedApart() {
        List<QuestNpcPlan.Giver> g = QuestNpcPlan.GIVERS;
        Set<Integer> indexes = new HashSet<>();
        for (int i = 0; i < g.size(); i++) {
            assertEquals(i, g.get(i).index());
            assertTrue(indexes.add(g.get(i).index()));
            for (int j = i + 1; j < g.size(); j++) {
                double d = QuestNpcPlan.distance(g.get(i), g.get(j));
                assertTrue(d >= QuestNpcPlan.MIN_DISTANCE, g.get(i).name() + " et " + g.get(j).name() + " ne sont qu'a " + Math.round(d) + " blocs");
            }
        }
        Set<java.util.UUID> uuids = new HashSet<>();
        g.forEach(x -> assertTrue(uuids.add(x.uuid())));
    }

    @Test
    void questIdsAreStableForExistingSaves() {
        // Les identifiants deja utilises par les joueurs ne doivent jamais disparaitre.
        for (String id : List.of("premiers_pas", "oreilles_pointues", "crocs_nuit", "ferraille", "apprentissage", "roi_gobelin", "os_maudits",
                "poussiere_ame", "brutes_orcs", "liche_ancienne", "coeur_flammes", "souffle_givre", "seigneur_ignis", "cristaux_vivants",
                "titan_glace", "chevaliers_neant", "avatar_neant", "contrat_chasse", "approvisionnement", "tueur_geants", "nettoyer_cryptes",
                "toiles_venimeuses", "routes_sures", "evolution_1", "evolution_4", "parchemin_forge_1", "parchemin_forge_5", "spawn_bienvenue")) {
            assertTrue(QUESTS.containsKey(id), id + " a disparu");
        }
    }
}
