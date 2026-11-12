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
 *     <li>{@code talk} : parler à un autre PNJ. Cible : UUID persistant du PNJ ; quantité : 1.</li>
 *     <li>{@code collect} : rapporter des objets (retires de l'inventaire a la remise). Cible : identifiant d'objet.</li>
 *     <li>{@code level} : atteindre un niveau (la quantite est le niveau).</li>
 *     <li>{@code action} : faire decouvrir une fonctionnalite. Cible : {@code cle|libelle} (cle d'action, voir QuestManager#onAction).</li>
 * </ul>
 */
public class QuestDef {
    /** Revision des quetes par defaut (DefaultQuests.REVISION) dont cette quete provient ; une revision plus ancienne est remplacee au chargement. */
    public int revision = 0;
    public String name = "Quête";
    public String description = "";
    public int minLevel = 1;
    /** Quetes a terminer avant de pouvoir accepter celle-ci. */
    public List<String> requires = new ArrayList<>();
    /** Classes concernees (identifiants : guerrier, mage, archer, tank) ; vide = toutes. Quetes de classe, remises au Maitre des classes. */
    public List<String> classes = new ArrayList<>();
    /** Identifiant de la scene cinematique de l'histoire principale (StoryBook) ; vide = pas une quete d'histoire. Rendue automatiquement. */
    public String story = "";
    /** Quete journaliere (recommencable chaque jour civil du serveur). */
    public boolean daily = false;
    /** Quete hebdomadaire personnelle generee par WeeklyQuests (jamais ecrite dans quests.json). */
    public boolean weekly = false;
    /** Groupe de PNJ qui proposent la quete (vide = tous les maitres des quetes). */
    public String giver = "";
    /** Optional exact NPC UUID; empty retains the historical group-based behaviour. */
    public String npc = "";
    /** Replique de la Mascotte a l'acceptation / a la remise (tutoriel ; vide = aucune). */
    public String speech = "", speechDone = "";
    public List<Objective> objectives = new ArrayList<>();
    public Rewards rewards = new Rewards();
    /** Repetition : "" (une fois), "daily" ou "weekly" (remise a zero a 4 h, lundi pour l'hebdomadaire). Les quetes journalieres historiques utilisent {@link #daily}. */
    public String repeat = "";
    /** Quete rendue automatiquement des que ses objectifs sont atteints (trame principale), sans retour au donneur. */
    public boolean auto = false;
    /** Donnees du lore d'Aeloria (null pour les quetes hors plan de quetes). */
    public Lore lore;

    /** Metadonnees du plan de quetes (Quetes_Aeloria_v1) : trame, secondaires, evenements. */
    public static class Lore {
        /** principale, secondaire ou evenement. */
        public String kind = "secondaire";
        public String code = "", donor = "", donorKey = "", type = "", objective = "", region = "", act = "", chapter = "", category = "", companion = "", gameplay = "";
        /** Texte des recompenses non gerees par le mod (monnaies, titres, coffres...). */
        public List<String> extras = new ArrayList<>();
        /** Variantes par classe (guerrier, mage, archer, tank). */
        public java.util.Map<String, String> variants = new java.util.LinkedHashMap<>();
        public String event = "", from = "", to = "", conditionText = "";
        /** Condition en jeu : night, dawn, storm, sunday ou vide. */
        public String when = "";
        /** Creneau en heure serveur : {mois (0 = chaque jour), jour, minute de debut, minute de fin}, ou vide. */
        public int[] slot;
        /** Duree limite en minutes (0 = aucune). */
        public int limit = 0;
    }

    public static class Navigation {
        public String dimension="minecraft:overworld";
        public String backend="";
        public double x,y,z;
    }
    public static class Objective {
        /** Optional authored waypoint; missing values keep automatic mob/resource guidance. */
        public Navigation navigation;

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
        /** Identifiant de monture donnee en recompense (vide = aucune). */
        public String mount = "";
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
