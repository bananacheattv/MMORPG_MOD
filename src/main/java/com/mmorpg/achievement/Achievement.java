package com.mmorpg.achievement;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Succes du mod : un compteur (stat) a atteindre ; certains debloquent un titre affichable apres le pseudo. */
public record Achievement(String id, Category category, String name, String description, String stat, long target, String title) {
    public enum Category {
        PROGRESSION("Progression", 0xFFE2B85A), COMBAT("Combat", 0xFFFF6B6B), DONJONS("Donjons", 0xFFB080FF),
        PVP("PvP", 0xFF62A8FF), METIERS("Métiers", 0xFF6AE06A), QUETES("Quêtes", 0xFFFFC060), RICHESSE("Richesse", 0xFFFFE680);

        public final String label;
        public final int color;

        Category(String label, int color) {
            this.label = label;
            this.color = color;
        }
    }

    /** Statistiques suivies (cle -> libelle) ; "max" = on garde la valeur la plus haute au lieu de cumuler. */
    public static final String LEVEL = "level", KILLS = "kills", BOSS_KILLS = "boss_kills", DUNGEONS = "dungeons", QUESTS = "quests",
            PVP_KILLS = "pvp_kills", PVP_WINS = "pvp_wins", GOLD_EARNED = "gold_earned";

    public static String jobStat(String jobId) {
        return "job_" + jobId;
    }

    public boolean hasTitle() {
        return !title.isEmpty();
    }

    private static final Map<String, Achievement> ALL = new LinkedHashMap<>();

    private static void add(Category c, String id, String name, String desc, String stat, long target, String title) {
        ALL.put(id, new Achievement(id, c, name, desc, stat, target, title));
    }

    static {
        add(Category.PROGRESSION, "niveau_10", "Premiers pas", "Atteindre le niveau 10.", LEVEL, 10, "Aventurier");
        add(Category.PROGRESSION, "niveau_25", "Voyageur", "Atteindre le niveau 25.", LEVEL, 25, "Voyageur");
        add(Category.PROGRESSION, "niveau_50", "Vétéran", "Atteindre le niveau 50.", LEVEL, 50, "Vétéran");
        add(Category.PROGRESSION, "niveau_75", "Héros", "Atteindre le niveau 75.", LEVEL, 75, "Héros d’Aeloria");
        add(Category.PROGRESSION, "niveau_100", "Légende", "Atteindre le niveau 100.", LEVEL, 100, "Légende vivante");

        add(Category.COMBAT, "kills_100", "Chasseur", "Vaincre 100 monstres.", KILLS, 100, "Chasseur");
        add(Category.COMBAT, "kills_1000", "Exterminateur", "Vaincre 1 000 monstres.", KILLS, 1000, "Exterminateur");
        add(Category.COMBAT, "kills_10000", "Fléau des monstres", "Vaincre 10 000 monstres.", KILLS, 10000, "Fléau des monstres");
        add(Category.COMBAT, "boss_1", "Tueur de boss", "Vaincre un boss.", BOSS_KILLS, 1, "");
        add(Category.COMBAT, "boss_25", "Pourfendeur", "Vaincre 25 boss.", BOSS_KILLS, 25, "Pourfendeur de boss");
        add(Category.COMBAT, "boss_100", "Terreur des gardiens", "Vaincre 100 boss.", BOSS_KILLS, 100, "Terreur des gardiens");

        add(Category.DONJONS, "donjon_1", "Explorateur", "Terminer un donjon.", DUNGEONS, 1, "Explorateur");
        add(Category.DONJONS, "donjon_10", "Conquérant", "Terminer 10 donjons.", DUNGEONS, 10, "Conquérant des donjons");
        add(Category.DONJONS, "donjon_50", "Maître des antres", "Terminer 50 donjons.", DUNGEONS, 50, "Maître des antres");

        add(Category.PVP, "pvp_kill_1", "Premier sang", "Vaincre un joueur en arène.", PVP_KILLS, 1, "");
        add(Category.PVP, "pvp_kill_50", "Duelliste", "Vaincre 50 joueurs en arène.", PVP_KILLS, 50, "Duelliste");
        add(Category.PVP, "pvp_kill_250", "Bourreau d’arène", "Vaincre 250 joueurs en arène.", PVP_KILLS, 250, "Bourreau d’arène");
        add(Category.PVP, "pvp_win_1", "Première victoire", "Gagner un match d’arène.", PVP_WINS, 1, "");
        add(Category.PVP, "pvp_win_25", "Champion d’arène", "Gagner 25 matchs d’arène.", PVP_WINS, 25, "Champion d’arène");
        add(Category.PVP, "pvp_win_100", "Invaincu", "Gagner 100 matchs d’arène.", PVP_WINS, 100, "L’Invaincu");

        add(Category.METIERS, "mineur_25", "Mineur confirmé", "Mineur niveau 25.", jobStat("mineur"), 25, "Mineur émérite");
        add(Category.METIERS, "mineur_50", "Maître mineur", "Mineur niveau 50.", jobStat("mineur"), 50, "Maître mineur");
        add(Category.METIERS, "fermier_25", "Fermier confirmé", "Fermier niveau 25.", jobStat("fermier"), 25, "Fermier émérite");
        add(Category.METIERS, "fermier_50", "Maître fermier", "Fermier niveau 50.", jobStat("fermier"), 50, "Maître fermier");
        add(Category.METIERS, "pecheur_25", "Pêcheur confirmé", "Pêcheur niveau 25.", jobStat("pecheur"), 25, "Pêcheur émérite");
        add(Category.METIERS, "pecheur_50", "Maître pêcheur", "Pêcheur niveau 50.", jobStat("pecheur"), 50, "Maître pêcheur");

        add(Category.QUETES, "quete_1", "Premier contrat", "Terminer une quête.", QUESTS, 1, "");
        add(Category.QUETES, "quete_25", "Homme de confiance", "Terminer 25 quêtes.", QUESTS, 25, "Homme de confiance");
        add(Category.QUETES, "quete_100", "Héros du peuple", "Terminer 100 quêtes.", QUESTS, 100, "Héros du peuple");

        add(Category.RICHESSE, "or_10000", "Bourse pleine", "Gagner 10 000 pièces d’or.", GOLD_EARNED, 10_000, "Fortuné");
        add(Category.RICHESSE, "or_1000000", "Magnat", "Gagner 1 000 000 pièces d’or.", GOLD_EARNED, 1_000_000, "Magnat d’Aeloria");
    }

    public static Map<String, Achievement> all() {
        return ALL;
    }

    public static Achievement get(String id) {
        return ALL.get(id);
    }

    public static List<Achievement> forStat(String stat) {
        var out = new ArrayList<Achievement>();
        for (var a : ALL.values()) if (a.stat.equals(stat)) out.add(a);
        return out;
    }

    /** Vrai pour les statistiques qui gardent le maximum atteint (niveaux) plutot que de cumuler. */
    public static boolean isMax(String stat) {
        return stat.equals(LEVEL) || stat.startsWith("job_");
    }
}
