package com.mmorpg.quest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Scenes cinematiques de l'histoire principale d'Aeloria (donnees pures, partagees par le serveur et le client). */
public final class StoryBook {
    private StoryBook() {
    }

    /** Une scene : titre de chapitre, couleur d'ambiance (ARGB) et repliques lues l'une apres l'autre. */
    public record Scene(String chapter, String title, int color, List<String> lines) {
    }

    public static final String EPILOGUE = "hist_epilogue";
    private static final Map<String, Scene> SCENES = new LinkedHashMap<>();

    private static void s(String id, String chapter, String title, int color, String... lines) {
        SCENES.put(id, new Scene(chapter, title, color, List.of(lines)));
    }

    static {
        s("hist_1", "Chapitre I", "Le murmure des plaines", 0xFF7CC76A,
                "Aeloria fut un royaume paisible, bercé par le chant des plaines.",
                "Mais depuis quelques lunes, les gobelins rôdent en nombre autour de la cité.",
                "Quelqu'un les rassemble. Quelqu'un leur a rendu l'espoir de conquête.",
                "Votre légende commence ici, voyageur.");
        s("hist_2", "Chapitre II", "Les crocs de la nuit", 0xFF8FA8C8,
                "Les gobelins reculent, mais la forêt gronde à son tour.",
                "Les loups sombres fuient quelque chose de plus grand qu'eux.",
                "Les anciens murmurent un nom oublié, que même les cryptes redoutent.");
        s("hist_3", "Chapitre III", "Le Roi des Gobelins", 0xFFB8862E,
                "Au cœur de leur camp, un trône de ferraille.",
                "Le Roi Gobelin a bâti son armée avec une patience qui n'a rien de sauvage.",
                "Abattez-le, et le royaume respirera, un instant.");
        s("hist_4", "Chapitre IV", "L'éveil des cryptes", 0xFF9A7ACC,
                "À peine le Roi tombé, les cimetières se fissurent.",
                "Des squelettes marchent en rangs, guidés par une volonté unique.",
                "Une liche ancienne réclame le royaume que les vivants ont déserté.");
        s("hist_5", "Chapitre V", "Le sceau brisé", 0xFFB070E0,
                "Les chroniques parlent d'un sceau qui retenait les anciens maîtres des ténèbres.",
                "Il a été brisé de l'intérieur, par une main qui connaissait ses secrets.",
                "Les seigneurs de guerre du royaume sont les seuls à pouvoir le refermer.");
        s("hist_6", "Chapitre VI", "Les orcs des collines", 0xFFC07040,
                "Les orcs descendent des collines, armés d'un acier qu'ils ne savent pas forger.",
                "Quelqu'un leur a fourni ces lames. Quelqu'un à l'intérieur des murs.",
                "Brisez leur avant-garde, et suivez la piste de l'acier.");
        s("hist_7", "Chapitre VII", "Les spectres du givre", 0xFF80D0F0,
                "Le froid avance plus vite que les armées.",
                "Les cols de montagne sont déjà pris par la glace, et les spectres y règnent.",
                "Quelque chose, là-haut, aspire la chaleur du monde.");
        s("hist_8", "Chapitre VIII", "Les cristaux vivants", 0xFFA0E0E8,
                "Sous la montagne, les cristaux battent comme un cœur.",
                "Les golems veillent sur un secret que la terre elle-même voulait taire.",
                "Chaque cristal brisé révèle un fragment de la vérité : le Néant n'est pas venu du dehors.");
        s("hist_9", "Chapitre IX", "La trahison du Veilleur", 0xFFE05050,
                "Le Veilleur du Néant n'a jamais été notre gardien.",
                "C'est lui qui a brisé le sceau, lui qui a armé les orcs, lui qui a réveillé la liche.",
                "Il voulait un héros assez fort pour ouvrir la dernière porte. Il vous attendait.");
        s("hist_10", "Chapitre X", "Les chevaliers du Néant", 0xFF6A4AA8,
                "Les failles s'ouvrent dans le ciel, et les chevaliers du Néant les franchissent.",
                "Leurs armures n'ont pas de visage, et leurs lames pas d'ombre.",
                "Aeloria tient encore, mais plus pour longtemps.");
        s("hist_11", "Chapitre XI", "L'Avatar du Néant", 0xFF3A1A6A,
                "L'Avatar du Néant s'éveille au cœur de la dernière faille.",
                "Il n'est ni dieu, ni démon : seulement le vide, qui a pris une forme pour vous parler.",
                "« Tu es venu jusqu'ici, voyageur. Voyons si tu repartiras. »");
        s("hist_12", "Chapitre XII", "Héros d'Aeloria", 0xFFFFD040,
                "La dernière faille se referme dans un silence de cathédrale.",
                "Les cloches de la cité sonnent pour la première fois depuis des mois.",
                "Votre légende est écrite. Mais Aeloria a toujours besoin de ses héros.");
        s(EPILOGUE, "Épilogue", "Le réveil du royaume", 0xFFFFE090,
                "Les plaines refleurissent, les routes se remplissent de voyageurs.",
                "Les enfants jouent à vous imiter, sabre en bois et cape de fortune.",
                "Et quelque part, au-delà des montagnes, de nouvelles ombres commencent à bouger.",
                "Merci d'avoir sauvé Aeloria.");
    }

    private static boolean lore;

    /** Ajoute une fois les cartes de chapitre du lore (LoreQuests). */
    private static synchronized void loadLore() {
        if (lore) return;
        lore = true;
        try {
            SCENES.putAll(LoreQuests.chapterScenes());
        } catch (RuntimeException ex) {
            lore = false;
            throw ex;
        }
    }

    public static Scene get(String id) {
        if (id != null && id.startsWith("lore_")) loadLore();
        return SCENES.get(id);
    }

    public static Map<String, Scene> all() {
        return SCENES;
    }
}
