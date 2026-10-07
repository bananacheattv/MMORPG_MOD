package com.mmorpg.quest;

import java.util.ArrayList;
import java.util.List;

/**
 * Definition d'une quete (fichier config/mmorpg/quests.json).
 *
 * <p>Types d'objectifs :</p>
 * <ul>
 *     <li>{@code kill} : vaincre des monstres. Cible : cle de monstre du mod ("gobelin"), identifiant vanilla
 *     ("minecraft:zombie"), "*" pour n'importe quel monstre, ou "boss" pour n'importe quel boss.</li>
 *     <li>{@code collect} : rapporter des objets (retires de l'inventaire a la remise). Cible : identifiant d'objet.</li>
 *     <li>{@code level} : atteindre un niveau (la quantite est le niveau).</li>
 * </ul>
 */
public class QuestDef {
    public String name = "Quête";
    public String description = "";
    public int minLevel = 1;
    /** Quetes a terminer avant de pouvoir accepter celle-ci. */
    public List<String> requires = new ArrayList<>();
    /** Quete journaliere (recommencable chaque jour civil du serveur). */
    public boolean daily = false;
    /** Groupe de PNJ qui proposent la quete (vide = tous les maitres des quetes). */
    public String giver = "";
    /** Optional exact NPC UUID; empty retains the historical group-based behaviour. */
    public String npc = "";
    public List<Objective> objectives = new ArrayList<>();
    public Rewards rewards = new Rewards();

    public static class Objective {
        public String type = "kill";
        public String target = "*";
        public int count = 1;

        public Objective() {
        }

        public Objective(String type, String target, int count) {
            this.type = type;
            this.target = target;
            this.count = count;
        }
    }

    public static class Rewards {
        public long xp = 0;
        /** Experience supplementaire en pourcentage du niveau en cours du joueur (quetes journalieres). */
        public double xpPercentOfLevel = 0;
        public long gold = 0;
        public List<ItemReward> items = new ArrayList<>();
        public String pet = "";
        public String cosmetic = "";
        /** Palier d'evolution de classe debloque (0 = aucun, 1 a 4). Quetes du Maitre des classes. */
        public int evolution = 0;
    }

    public static class ItemReward {
        public String item = "minecraft:air";
        public int count = 1;

        public ItemReward() {
        }

        public ItemReward(String item, int count) {
            this.item = item;
            this.count = count;
        }
    }
}
