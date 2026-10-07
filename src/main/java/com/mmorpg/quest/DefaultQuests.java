package com.mmorpg.quest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Quetes par defaut : une campagne qui suit la progression des niveaux 1 a 100, plus des quetes journalieres. */
public final class DefaultQuests {
    private DefaultQuests() {
    }

    private static QuestDef q(String name, int level, String desc) {
        QuestDef d = new QuestDef();
        d.name = name;
        d.minLevel = level;
        d.description = desc;
        return d;
    }

    private static QuestDef obj(QuestDef d, String type, String target, int count) {
        d.objectives.add(new QuestDef.Objective(type, target.contains(":") || type.equals("kill") || type.equals("level") ? target : "mmorpg:" + target, count));
        return d;
    }

    private static QuestDef reward(QuestDef d, long xp, long gold, Object... items) {
        d.rewards.xp = xp;
        d.rewards.gold = gold;
        for (int i = 0; i + 1 < items.length; i += 2) {
            String id = (String) items[i];
            d.rewards.items.add(new QuestDef.ItemReward(id.contains(":") ? id : "mmorpg:" + id, (Integer) items[i + 1]));
        }
        return d;
    }

    private static QuestDef req(QuestDef d, String... ids) {
        d.requires.addAll(List.of(ids));
        return d;
    }

    public static Map<String, QuestDef> create() {
        Map<String, QuestDef> m = new LinkedHashMap<>();
        // ------------------------------------------------ chapitre 1 : les plaines
        m.put("premiers_pas", reward(obj(q("Premiers pas", 1,
                "Les gobelins rôdent autour de la ville. Prouvez votre valeur en éliminant quelques-uns de ces pillards."),
                "kill", "gobelin", 5), 150, 10, "potion_soin_mineure", 3));
        m.put("oreilles_pointues", req(reward(obj(q("Oreilles pointues", 3,
                "Le capitaine de la garde paie une prime pour chaque oreille de gobelin rapportée."),
                "collect", "oreille_gobelin", 8), 350, 25, "parchemin_teleportation", 2), "premiers_pas"));
        m.put("crocs_nuit", reward(obj(q("Les crocs de la nuit", 6,
                "Des loups sombres attaquent les voyageurs à la tombée de la nuit. Chassez-les."),
                "kill", "loup_sombre", 6), 800, 30, "potion_soin", 2));
        m.put("ferraille", req(reward(obj(q("Ferraille et pièces", 8,
                "Le forgeron manque de métal. Récupérez la ferraille que transportent les gobelins."),
                "collect", "ferraille_gobeline", 10), 1200, 40, "pierre_amelioration", 2), "oreilles_pointues"));
        m.put("apprentissage", obj(reward(q("L'heure de l'apprentissage", 1,
                "Devenez plus fort : atteignez le niveau 10 pour prouver votre engagement."), 1500, 50, "elixir_experience", 1),
                "level", "niveau", 10));
        m.put("roi_gobelin", req(reward(obj(q("Le Roi des Gobelins", 18,
                "Le Roi Gobelin unit les tribus pour attaquer la ville. Fabriquez un Sceau du Roi Gobelin et terrassez-le sur un autel."),
                "kill", "roi_gobelin", 1), 6000, 150, "cle_quete", 1), "ferraille"));
        // ------------------------------------------------ chapitre 2 : les morts-vivants
        m.put("os_maudits", reward(obj(q("Os maudits", 16,
                "Des squelettes à la flamme violette sortent des cimetières. Détruisez-les."),
                "kill", "squelette_maudit", 10), 5000, 60, "potion_mana", 2));
        m.put("poussiere_ame", req(reward(obj(q("Poussière d'âme", 20,
                "L'alchimiste a besoin de poussière d'âme pour ses potions."),
                "collect", "poussiere_ame", 12), 7000, 80, "potion_soin", 4), "os_maudits"));
        m.put("brutes_orcs", reward(obj(q("Brutes orcs", 28,
                "Les orcs ont établi un camp dans les collines. Brisez leur avant-garde."),
                "kill", "orc_guerrier", 10), 18000, 120, "pierre_amelioration", 3));
        m.put("liche_ancienne", req(reward(obj(q("La Liche Ancienne", 42,
                "Une liche ancienne commande les morts. Ouvrez le Grimoire Interdit sur un autel et mettez fin à son règne."),
                "kill", "liche_ancienne", 1), 70000, 300, "elixir_experience", 2, "cle_quete", 1), "poussiere_ame"));
        // ------------------------------------------------ chapitre 3 : feu et glace
        m.put("coeur_flammes", reward(obj(q("Cœur de flammes", 45,
                "Les élémentaires de feu menacent les récoltes. Rapportez leurs noyaux ardents."),
                "collect", "noyau_flamme", 8), 60000, 200, "potion_soin_majeure", 2));
        m.put("souffle_givre", reward(obj(q("Le souffle du givre", 55,
                "Les spectres de givre gèlent les cols de montagne. Dissipez-les."),
                "kill", "spectre_givre", 12), 100000, 250, "potion_mana_majeure", 3));
        m.put("seigneur_ignis", req(reward(obj(q("Le Seigneur Démon", 62,
                "Ignis, seigneur des flammes, s'éveille. Invoquez-le avec la Braise Éternelle et abattez-le."),
                "kill", "seigneur_ignis", 1), 220000, 600, "cle_quete", 1, "pierre_amelioration_sup", 2), "coeur_flammes"));
        m.put("cristaux_vivants", reward(obj(q("Cristaux vivants", 65,
                "Des golems de cristal gardent les profondeurs. Récoltez leurs cristaux."),
                "kill", "golem_cristal", 8), 200000, 350, "pierre_amelioration_sup", 2));
        m.put("titan_glace", req(reward(obj(q("Le Titan de Glace", 78,
                "Le Titan de Glace menace de geler le monde. Brisez-le."),
                "kill", "titan_glace", 1), 450000, 900, "cle_quete", 1), "souffle_givre"));
        // ------------------------------------------------ chapitre 4 : le Neant
        m.put("chevaliers_neant", reward(obj(q("Les chevaliers du Néant", 80,
                "Des chevaliers venus du Néant franchissent les failles. Repoussez-les."),
                "kill", "chevalier_neant", 12), 500000, 800, "pierre_amelioration_sup", 3));
        m.put("avatar_neant", req(reward(obj(q("L'Avatar du Néant", 95,
                "L'Avatar du Néant est la menace ultime. Ouvrez l'Œil du Néant et sauvez Eldoria."),
                "kill", "avatar_neant", 1), 1500000, 3000, "fragment_divin", 1, "cle_quete", 2), "chevaliers_neant", "titan_glace"));
        // ------------------------------------------------ quetes journalieres
        QuestDef daily = q("Contrat de chasse", 1, "Contrat quotidien : éliminez 20 monstres, quels qu'ils soient.");
        daily.daily = true;
        obj(daily, "kill", "*", 20);
        daily.rewards.gold = 40;
        daily.rewards.xpPercentOfLevel = 15;
        m.put("contrat_chasse", daily);
        QuestDef supply = q("Approvisionnement", 10, "Contrat quotidien : la ville a besoin de fer pour ses défenses.");
        supply.daily = true;
        obj(supply, "collect", "minecraft:iron_ingot", 10);
        supply.rewards.gold = 80;
        supply.rewards.xpPercentOfLevel = 10;
        m.put("approvisionnement", supply);
        QuestDef hunt = q("Tueur de géants", 20, "Contrat quotidien : terrassez un boss, n'importe lequel.");
        hunt.daily = true;
        obj(hunt, "kill", "boss", 1);
        hunt.rewards.gold = 200;
        hunt.rewards.xpPercentOfLevel = 30;
        hunt.rewards.items.add(new QuestDef.ItemReward("mmorpg:pierre_amelioration", 2));
        m.put("tueur_geants", hunt);
        m.put("nettoyer_cryptes", reward(obj(q("Les cryptes oubliées", 8, "La garde recherche des volontaires pour éliminer les zombies des cryptes."), "kill", "zombie_des_cryptes", 8), 1800, 60, "potion_soin", 2));
        m.put("toiles_venimeuses", req(reward(obj(q("Toiles venimeuses", 18, "Après les cryptes, débarrassez les galeries des araignées venimeuses."), "kill", "araignee_venimeuse", 10), 6500, 110, "potion_mana", 3), "nettoyer_cryptes"));
        m.put("routes_sures", req(reward(obj(q("Des routes plus sûres", 30, "Les arbalétriers pillent les caravanes. Rendez les routes aux voyageurs."), "kill", "bandit_arbaletrier", 12), 22000, 180, "cle_quete", 1), "toiles_venimeuses"));
        // ------------------------------------------------ evolutions de classe (PNJ du groupe « maitre_classes »)
        m.put("evolution_1", evolution(obj(obj(q("Épreuve de l'Initié", 25,
                "Le Maître des classes jauge votre potentiel. Prouvez votre force sur le terrain et il éveillera votre première évolution."),
                "level", "niveau", 25), "kill", "*", 60), 1, 8000, 300));
        m.put("evolution_2", req(evolution(obj(obj(obj(q("Épreuve de l'Adepte", 50,
                "Seuls ceux qui ont affronté un seigneur de guerre méritent la deuxième évolution. Terrassez un boss."),
                "level", "niveau", 50), "kill", "*", 150), "kill", "boss", 1), 2, 60000, 1200), "evolution_1"));
        m.put("evolution_3", req(evolution(obj(obj(obj(q("Épreuve du Maître", 75,
                "La troisième évolution exige une maîtrise totale : triomphez de plusieurs boss du royaume."),
                "level", "niveau", 75), "kill", "*", 300), "kill", "boss", 3), 3, 300000, 4000), "evolution_2"));
        m.put("evolution_4", req(evolution(obj(obj(q("Épreuve de la Légende", 100,
                "L'ultime évolution n'appartient qu'aux légendes. Abattez l'Avatar du Néant et revenez auprès du Maître."),
                "level", "niveau", 100), "kill", "avatar_neant", 1), 4, 1500000, 10000), "evolution_3"));
        return m;
    }

    /** Quete d'evolution : proposee par le Maitre des classes, recompense = palier d'evolution + cle de quete. */
    private static QuestDef evolution(QuestDef d, int tier, long xp, long gold) {
        reward(d, xp, gold, "cle_quete", 1);
        d.rewards.evolution = tier;
        d.giver = "maitre_classes";
        return d;
    }
}
