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
        if (itemKey(id).startsWith("parchemin_plan_test_") || itemKey(id).startsWith("@mat_")) return true;
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
            assertTrue(!q.objectives.isEmpty() || q.auto, id + " sans objectif");
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
                    case "action" -> assertTrue(o.target.contains("|") && !o.target.substring(o.target.indexOf('|') + 1).isBlank(), where + " : action sans libelle");
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
        assertTrue(QUESTS.get("qp_001").requires.contains("tuto_fin"), "la trame du lore suit le tutoriel");
        assertTrue(QUESTS.get("parchemin_forge_1").requires.contains("spawn_premiere_chasse"));
        for (int i = 1; i < QuestNpcPlan.DISCOVERY_IDS.length; i++) {
            assertTrue(QUESTS.get(QuestNpcPlan.DISCOVERY_IDS[i]).requires.contains(QuestNpcPlan.DISCOVERY_IDS[i - 1]), "chaine du spawn rompue en " + i);
        }
        // Trame du lore : chaque quete exige la precedente
        for (int i = 2; i <= 250; i++) {
            assertTrue(QUESTS.get(String.format("qp_%03d", i)).requires.contains(String.format("qp_%03d", i - 1)), "trame rompue en " + i);
        }
    }

    @Test
    void everyQuestHasAGiverThatExists() {
        Set<String> groups = new HashSet<>();
        for (var g : QuestNpcPlan.GIVERS) assertTrue(groups.add(g.group()), "groupe de PNJ en double : " + g.group());
        groups.add("maitre_classes"); // PNJ du hub (HubBuilder)
        groups.add(QuestNpcPlan.GROUP_TUTORIAL); // la Mascotte du tutoriel
        groups.add("histoire"); // histoire principale : rendue automatiquement, sans PNJ
        for (var e : QUESTS.entrySet()) {
            if (e.getValue().lore != null && !e.getValue().lore.kind.equals("principale") && !e.getValue().giver.equals("maitre_classes")) continue;   // secondaires/evenements : PNJ au nom du donneur
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
            if (Set.of(QuestNpcPlan.GROUP_PLAINES, QuestNpcPlan.GROUP_MORTS, QuestNpcPlan.GROUP_ELEMENTS, QuestNpcPlan.GROUP_NEANT).contains(g.group())) continue;   // ancienne campagne, remplacee par le lore
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
            for (int j = i + 1; j < g.size() && i > 0; j++) { // le 1er PNJ (accueil) est colle au spawn : exception
                double d = QuestNpcPlan.distance(g.get(i), g.get(j));
                assertTrue(d > QuestNpcPlan.MIN_DISTANCE, g.get(i).name() + " et " + g.get(j).name() + " ne sont qu'a " + Math.round(d) + " blocs");
            }
        }
        Set<java.util.UUID> uuids = new HashSet<>();
        g.forEach(x -> assertTrue(uuids.add(x.uuid())));
    }

    @Test
    void firstGiverIsNextToSpawn() {
        var first = QuestNpcPlan.GIVERS.getFirst();
        assertTrue(Math.hypot(first.dx(), first.dz()) <= 10, "le 1er PNJ doit etre au spawn");
    }

    @Test
    void questIdsAreStableForExistingSaves() {
        // Les identifiants deja utilises par les joueurs ne doivent jamais disparaitre.
        for (String id : List.of("evolution_1", "evolution_4", "classe_mage_0", "qp_001", "qp_063", "qp_250", "qs_001", "qs_499", "ev_noel_01", "ev_paq_25", "parchemin_forge_1", "parchemin_forge_5", "spawn_bienvenue")) {
            assertTrue(QUESTS.containsKey(id), id + " a disparu");
        }
    }

    @Test
    void lorePlanIsComplete() {
        assertEquals(250, QUESTS.keySet().stream().filter(k -> k.startsWith("qp_")).count());
        assertEquals(452, QUESTS.keySet().stream().filter(k -> k.startsWith("qs_")).count());   // 500 moins les 48 quetes de classe du lore
        assertEquals(75, QUESTS.keySet().stream().filter(k -> k.startsWith("ev_")).count());
        // les classes restent celles du mod : chaines de classe et evolutions conservees, aucune classe du lore
        assertEquals(44, QUESTS.keySet().stream().filter(k -> k.startsWith("classe_")).count());
        assertEquals(1, QUESTS.get("evolution_1").rewards.evolution);
        assertTrue(QUESTS.values().stream().filter(q -> q.lore != null).allMatch(q -> q.classes.isEmpty() && q.lore.variants.isEmpty() && q.rewards.evolution == 0));
        assertTrue(QUESTS.get("qp_063").auto);
        assertEquals("2026-12-01", QUESTS.get("ev_noel_01").lore.from);
        assertTrue(QUESTS.values().stream().anyMatch(q -> "weekly".equals(q.repeat)));
        assertTrue(QUESTS.values().stream().anyMatch(q -> q.daily && q.lore != null));
        assertTrue(QUESTS.values().stream().anyMatch(q -> q.lore != null && q.lore.slot != null));
        // les etapes sans suivi reel passent par le donneur
        assertTrue(QUESTS.values().stream().filter(q -> q.lore != null && !q.auto).allMatch(q -> !q.objectives.isEmpty()));
    }

    @Test
    void tutorialIsAChainStartedByTheMascot() {
        var tuto = DefaultQuests.tutorial();
        assertTrue(tuto.size() >= 30, "tutoriel trop court : " + tuto.size());
        String previous = null;
        for (var e : tuto.entrySet()) {
            var q = e.getValue();
            assertEquals(QuestNpcPlan.GROUP_TUTORIAL, q.giver, e.getKey());
            assertEquals(QuestNpcPlan.MASCOT_ID.toString(), q.npc, e.getKey());
            assertFalse(q.speech.isBlank() || q.speechDone.isBlank(), e.getKey() + " : repliques de la Mascotte manquantes");
            assertEquals(previous == null ? List.of() : List.of(previous), q.requires, e.getKey() + " : chaine rompue");
            previous = e.getKey();
        }
        assertEquals("tuto_fin", previous);
        assertTrue(QUESTS.get("spawn_bienvenue").requires.contains("tuto_fin"), "la decouverte du spawn suit le tutoriel");
        // la premiere arme et la premiere armure se fabriquent : recettes novices presentes, aucune piece offerte
        assertTrue(tuto.get("tuto_arme").objectives.get(0).target.startsWith("craft:@arme"));
        assertEquals(4, tuto.get("tuto_armure").objectives.size());
        assertTrue(tuto.values().stream().flatMap(q -> q.rewards.items.stream()).noneMatch(r -> itemKey(r.item).matches(".*(epee_recrue|baton_apprenti|arc_chasseur|masse_garde|pioche_debutant|houe_debutant).*")),
                "le tutoriel ne doit rien offrir de fini");
    }
}
