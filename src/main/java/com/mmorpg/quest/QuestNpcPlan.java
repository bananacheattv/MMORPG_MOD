package com.mmorpg.quest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Plan des PNJ donneurs de quetes du spawn et chaine d'introduction. Donnees pures (aucune classe Minecraft) pour pouvoir etre validees
 * par les tests. Les positions sont des decalages (est, sud) en blocs depuis le point d'arrivee du spawn ; les PNJ sont espaces d'au
 * moins {@link #MIN_DISTANCE} blocs.
 */
public final class QuestNpcPlan {
    private QuestNpcPlan() {
    }

    /** Distance minimale (blocs) entre deux donneurs de quetes du spawn. */
    public static final int MIN_DISTANCE = 60;

    public static final String GROUP_PLAINES = "quetes_plaines", GROUP_MORTS = "quetes_morts_vivants", GROUP_ELEMENTS = "quetes_elements",
            GROUP_NEANT = "quetes_neant", GROUP_FORGE = "quetes_forge";

    /** Groupe de la Mascotte (guide du tutoriel) et identifiant fixe de son entite. */
    public static final String GROUP_TUTORIAL = "tutoriel";
    public static final UUID MASCOT_ID = UUID.nameUUIDFromBytes("aeloria:tutorial-mascot".getBytes(java.nio.charset.StandardCharsets.UTF_8));

    public record Giver(int index, String name, int skin, String group, int dx, int dz) {
        public UUID uuid() {
            return QuestNpcPlan.uuid(index);
        }
    }

    /**
     * Le tout premier (Maelys) se tient a cote du point d'arrivee, seule exception a l'espacement. Les cinq premiers forment la chaine de
     * decouverte (anneau interieur, rayon 54) ; les cinq suivants donnent la campagne (anneau
     * exterieur, rayon 100, decale de 36 degres), du garde-chasse (niveau 1) a la veilleuse du Neant (niveau 80+).
     */
    public static final List<Giver> GIVERS = List.of(
            new Giver(0, "Maëlys · Accueil d’Aeloria", 4, "spawn_decouverte_0", 0, -6),
            new Giver(1, "Orin · Artisan de la cité", 6, "spawn_decouverte_1", 51, -17),
            new Giver(2, "Lysandre · Intendante", 8, "spawn_decouverte_2", 32, 44),
            new Giver(3, "Érian · Éclaireur", 7, "spawn_decouverte_3", -32, 44),
            new Giver(4, "Capitaine Vaël", 5, "spawn_decouverte_4", -51, -17),
            new Giver(5, "Brenn · Garde-chasse", 1, GROUP_PLAINES, -59, -81),
            new Giver(6, "Sora · Veilleuse des tombes", 2, GROUP_MORTS, 59, -81),
            new Giver(7, "Ashe · Chasseur de braises", 3, GROUP_ELEMENTS, 95, 31),
            new Giver(8, "Orion · Veilleur du Néant", 13, GROUP_NEANT, 0, 100),
            new Giver(9, "Thorgal · Maître forgeron", 6, GROUP_FORGE, -95, 31));


    public static UUID uuid(int index) {
        return UUID.nameUUIDFromBytes(("aeloria:spawn-discovery:" + index).getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public static double distance(Giver a, Giver b) {
        return Math.hypot(a.dx() - b.dx(), a.dz() - b.dz());
    }

    // ------------------------------------------------------------------ chaine de decouverte

    public static final String[] DISCOVERY_IDS = {"spawn_bienvenue", "spawn_artisans", "spawn_economie", "spawn_expeditions", "spawn_premiere_chasse"};
    private static final String[] TITLES = {"Bienvenue dans la cité", "Les mains des artisans", "L’économie d’Aeloria", "Au-delà des remparts", "Votre première mission"};
    private static final String[] LORE = {
            "Bienvenue au spawn d’Aeloria. Rencontrez Orin, l’artisan : la flèche violette indique votre objectif. Le journal garde vos quêtes et leurs récompenses.",
            "Vos minerais deviennent des lingots, puis des équipements dans les forges. Les recettes et parchemins se débloquent progressivement. Rencontrez l’intendante pour découvrir le commerce.",
            "L’or sert aux marchands, à la banque et à l’hôtel des ventes. Les crédits de boutique sont une monnaie distincte. Parlez à l’éclaireur pour préparer votre première sortie.",
            "Les plaines accueillent les premières chasses ; les mines se débloquent par niveau. Les donjons sont privés pour votre groupe et annoncent leurs dangers. Le capitaine vous attend.",
            "Éliminez trois gobelins dans les plaines puis revenez au capitaine. Gardez quelques potions : en expédition, le butin de session peut être perdu à la mort. Ensuite, Brenn le garde-chasse vous confiera les vraies missions."};

    public static Map<String, QuestDef> discoveryDefinitions() {
        var map = new LinkedHashMap<String, QuestDef>();
        for (int i = 0; i < DISCOVERY_IDS.length; i++) {
            var q = new QuestDef();
            q.name = TITLES[i];
            q.description = LORE[i];
            q.giver = GIVERS.get(i).group();
            q.npc = uuid(i).toString();
            q.minLevel = 1;
            q.revision = DefaultQuests.REVISION;
            if (i > 0) q.requires.add(DISCOVERY_IDS[i - 1]);
            else q.requires.add("tuto_fin");                  // la chaîne de découverte suit le tutoriel de la Mascotte
            q.objectives.add(new QuestDef.Objective(i == 4 ? "kill" : "visit", i == 4 ? "gobelin" : GIVERS.get(i + 1).name(), i == 4 ? 3 : 1));
            q.rewards.xp = 80 + i * 40;
            q.rewards.gold = 10 + i * 5;
            q.rewards.items.add(new QuestDef.ItemReward("mmorpg:potion_soin_mineure", i == 4 ? 3 : 1));
            map.put(DISCOVERY_IDS[i], q);
        }
        return map;
    }
}
