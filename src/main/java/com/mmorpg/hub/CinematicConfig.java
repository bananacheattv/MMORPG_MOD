package com.mmorpg.hub;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mmorpg.MMORPG;
import com.mmorpg.config.ConfigManager;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Definition de la cinematique d'arrivee (config/mmorpg/cinematique.json). Le fichier est lu par le serveur puis envoye tel
 * quel au client au lancement ; toutes les coordonnees sont relatives a l'origine du hub (centre de la place, au niveau du sol).
 *
 * <pre>
 * { "fondEntree": 1.4, "fondSortie": 1.2, "plans": [ {
 *     "nom": "survol", "duree": 9.0, "rythme": "inOutCubic",            // linear | inOutCubic | inOutQuint | outCubic | inCubic
 *     "camera": [[x,y,z], ...],  "cible": [[x,y,z], ...],               // trajectoires (Catmull-Rom) ; "@joueur" / "@regard" = yeux / regard du joueur
 *     "fov": [[t, degres], ...], "roulis": [[t, degres], ...],           // t de 0 a 1 ; fov -1 = champ de vision des options
 *     "titre": {"texte": "...", "sous": "...", "couleur": "#FFD060", "debut": 0.15, "fin": 0.85},
 *     "transition": "fondu" | "coupe",                                   // vers le plan suivant
 *     "sons": [{"t": 0.0, "son": "minecraft:...", "volume": 1, "hauteur": 1}],
 *     "particules": [{"t": 0.3, "type": "flame", "pos": [x,y,z], "nombre": 30, "dispersion": [1,1,1], "vitesse": 0.05}],
 *     "classe": "GUERRIER"                                               // PNJ de classe mis en scene pendant ce plan
 * } ] }
 * </pre>
 */
public final class CinematicConfig {
    private CinematicConfig() {
    }

    public static final String DEFAULT = """
            {
              "fondEntree": 1.6,
              "fondSortie": 1.4,
              "plans": [
                {
                  "nom": "survol", "duree": 10.0, "rythme": "inOutCubic", "transition": "fondu",
                  "camera": [[-34, 26, 40], [-33, 22, 14], [-27, 19, -8], [-14, 16, -24], [6, 14, -22], [20, 15, -8]],
                  "cible": [[0, 3, 4], [0, 4, 0], [0, 5, -4], [0, 4, -2]],
                  "fov": [[0, 78], [0.5, 66], [1, 62]],
                  "titre": {"texte": "ELDORIA", "sous": "Le hub des héros", "couleur": "#FFD060", "debut": 0.12, "fin": 0.9},
                  "sons": [
                    {"t": 0.0, "son": "minecraft:block.beacon.activate", "volume": 0.7, "hauteur": 0.8},
                    {"t": 0.12, "son": "minecraft:ui.toast.challenge_complete", "volume": 0.5, "hauteur": 0.7}
                  ],
                  "particules": [
                    {"t": 0.1, "type": "end_rod", "pos": [0, 3, 0], "nombre": 60, "dispersion": [6, 3, 6], "vitesse": 0.02},
                    {"t": 0.5, "type": "end_rod", "pos": [0, 4, 0], "nombre": 60, "dispersion": [8, 3, 8], "vitesse": 0.02}
                  ]
                },
                {
                  "nom": "guerrier", "duree": 6.0, "rythme": "inOutCubic", "transition": "fondu", "classe": "GUERRIER",
                  "camera": [[-10, 1.6, -1.5], [-10, 1.8, -4.8], [-10, 2.0, -7.2]],
                  "cible": [[-10, 2.3, -10], [-10, 2.5, -10], [-10, 2.7, -10]],
                  "fov": [[0, 74], [1, 52]],
                  "titre": {"texte": "LE GUERRIER", "sous": "Force et Férocité — la lame avant tout", "couleur": "#FF6A50", "debut": 0.18, "fin": 0.92},
                  "sons": [
                    {"t": 0.0, "son": "minecraft:block.anvil.use", "volume": 0.5, "hauteur": 0.6},
                    {"t": 0.35, "son": "minecraft:item.trident.thunder", "volume": 0.5, "hauteur": 0.8}
                  ],
                  "particules": [
                    {"t": 0.0, "type": "flame", "pos": [-10, 1.2, -10], "nombre": 40, "dispersion": [1.1, 1.4, 1.1], "vitesse": 0.04},
                    {"t": 0.4, "type": "lava", "pos": [-10, 2.2, -10], "nombre": 14, "dispersion": [1, 1, 1], "vitesse": 0.05},
                    {"t": 0.5, "type": "crit", "pos": [-10, 2.8, -10], "nombre": 30, "dispersion": [1, 1, 1], "vitesse": 0.3}
                  ]
                },
                {
                  "nom": "mage", "duree": 6.0, "rythme": "inOutCubic", "transition": "fondu", "classe": "MAGE",
                  "camera": [[6.5, 9.0, -3.0], [7.2, 5.0, -6.2], [7.8, 2.3, -7.6]],
                  "cible": [[10, 3.4, -10], [10, 3.6, -10], [10, 3.8, -10]],
                  "fov": [[0, 82], [1, 55]],
                  "titre": {"texte": "LE MAGE", "sous": "Intelligence et Esprit — l'arcane obéit", "couleur": "#B07CFF", "debut": 0.18, "fin": 0.92},
                  "sons": [
                    {"t": 0.0, "son": "minecraft:block.enchantment_table.use", "volume": 0.8, "hauteur": 0.9},
                    {"t": 0.4, "son": "minecraft:entity.evoker.cast_spell", "volume": 0.5, "hauteur": 1.1}
                  ],
                  "particules": [
                    {"t": 0.0, "type": "enchant", "pos": [10, 2.5, -10], "nombre": 80, "dispersion": [1.5, 1.5, 1.5], "vitesse": 0.5},
                    {"t": 0.45, "type": "witch", "pos": [10, 3.0, -10], "nombre": 40, "dispersion": [1.2, 1.2, 1.2], "vitesse": 0.05},
                    {"t": 0.55, "type": "end_rod", "pos": [10, 4.2, -10], "nombre": 30, "dispersion": [0.8, 0.8, 0.8], "vitesse": 0.08}
                  ]
                },
                {
                  "nom": "archer", "duree": 6.0, "rythme": "inOutCubic", "transition": "fondu", "classe": "ARCHER",
                  "camera": [[-10, 1.6, 1.5], [-10, 1.8, 4.6], [-10, 2.0, 7.0]],
                  "cible": [[-10, 2.3, 10], [-10, 2.7, 10], [-10, 3.0, 10]],
                  "fov": [[0, 60], [1, 68]],
                  "titre": {"texte": "L'ARCHER", "sous": "Agilité et Dextérité — jamais là où l'on frappe", "couleur": "#6CE06C", "debut": 0.18, "fin": 0.92},
                  "sons": [
                    {"t": 0.0, "son": "minecraft:entity.arrow.shoot", "volume": 0.6, "hauteur": 1.1},
                    {"t": 0.45, "son": "minecraft:block.grass.break", "volume": 0.6, "hauteur": 0.8}
                  ],
                  "particules": [
                    {"t": 0.0, "type": "happy_villager", "pos": [-10, 1.5, 10], "nombre": 30, "dispersion": [1.6, 1.4, 1.6], "vitesse": 0.02},
                    {"t": 0.4, "type": "cherry_leaves", "pos": [-10, 4.0, 10], "nombre": 40, "dispersion": [2.2, 0.5, 2.2], "vitesse": 0.02},
                    {"t": 0.5, "type": "cloud", "pos": [-9, 1.4, 10], "nombre": 12, "dispersion": [0.6, 0.3, 0.6], "vitesse": 0.03}
                  ]
                },
                {
                  "nom": "tank", "duree": 6.0, "rythme": "inOutCubic", "transition": "fondu", "classe": "TANK",
                  "camera": [[10, 1.7, 1.5], [10, 1.9, 4.6], [10, 2.0, 7.0]],
                  "cible": [[10, 2.3, 10], [10, 2.5, 10], [10, 2.7, 10]],
                  "fov": [[0, 66], [1, 54]],
                  "titre": {"texte": "LE TANK", "sous": "Vitalité et Endurance — le rempart du groupe", "couleur": "#58A8FF", "debut": 0.18, "fin": 0.92},
                  "sons": [
                    {"t": 0.0, "son": "minecraft:item.shield.block", "volume": 0.8, "hauteur": 0.8},
                    {"t": 0.4, "son": "minecraft:block.anvil.land", "volume": 0.4, "hauteur": 0.7}
                  ],
                  "particules": [
                    {"t": 0.0, "type": "wax_on", "pos": [10, 1.8, 10], "nombre": 30, "dispersion": [1.4, 1.6, 1.4], "vitesse": 0.5},
                    {"t": 0.4, "type": "electric_spark", "pos": [10, 2.2, 10], "nombre": 30, "dispersion": [1.3, 1.6, 1.3], "vitesse": 0.4},
                    {"t": 0.55, "type": "snowflake", "pos": [10, 3.4, 10], "nombre": 30, "dispersion": [1.4, 1.2, 1.4], "vitesse": 0.02}
                  ]
                },
                {
                  "nom": "retour", "duree": 8.0, "rythme": "inOutQuint", "transition": "fin",
                  "camera": [[0, 17, -4], [0, 10, 5], [0, 4.2, 14], "@joueur"],
                  "cible": [[0, 2, 0], [0, 1.8, 8], "@regard"],
                  "fov": [[0, 72], [1, -1]],
                  "titre": {"texte": "À VOUS DE JOUER", "sous": "Parlez aux Maîtres de classe : essayez, puis choisissez votre voie", "couleur": "#FFE6A0", "debut": 0.25, "fin": 0.9},
                  "sons": [
                    {"t": 0.0, "son": "minecraft:block.beacon.power_select", "volume": 0.7, "hauteur": 1.0},
                    {"t": 0.85, "son": "minecraft:entity.player.levelup", "volume": 0.5, "hauteur": 1.2}
                  ],
                  "particules": [
                    {"t": 0.5, "type": "end_rod", "pos": [0, 2, 12], "nombre": 40, "dispersion": [3, 2, 3], "vitesse": 0.03}
                  ]
                }
              ]
            }
            """;

    /** Duree totale en secondes (fondus d'entree et de sortie compris). */
    public static double totalSeconds(String json) {
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            double total = o.has("fondEntree") ? o.get("fondEntree").getAsDouble() : 1.5;
            total += o.has("fondSortie") ? o.get("fondSortie").getAsDouble() : 1.0;
            for (JsonElement e : o.getAsJsonArray("plans")) total += e.getAsJsonObject().get("duree").getAsDouble();
            return total;
        } catch (Exception e) {
            return 60;
        }
    }

    /** Verifie la structure minimale d'une definition ; renvoie null si elle est valide, sinon la raison. */
    public static String validate(String json) {
        try {
            JsonObject o = JsonParser.parseString(json).getAsJsonObject();
            JsonArray plans = o.getAsJsonArray("plans");
            if (plans == null || plans.isEmpty()) return "aucun plan";
            for (int i = 0; i < plans.size(); i++) {
                JsonObject p = plans.get(i).getAsJsonObject();
                if (!p.has("duree") || p.get("duree").getAsDouble() <= 0.1) return "plan " + (i + 1) + " : durée invalide";
                if (!p.has("camera") || p.getAsJsonArray("camera").isEmpty()) return "plan " + (i + 1) + " : trajectoire de caméra absente";
                if (!p.has("cible") || p.getAsJsonArray("cible").isEmpty()) return "plan " + (i + 1) + " : cible absente";
            }
            return null;
        } catch (Exception e) {
            return "JSON illisible (" + e.getMessage() + ")";
        }
    }

    /** Charge (ou cree) config/mmorpg/cinematique.json ; en cas d'erreur, la definition par defaut est utilisee. */
    public static String load() {
        Path file = ConfigManager.dir().resolve("cinematique.json");
        try {
            Files.createDirectories(file.getParent());
            if (!Files.exists(file)) {
                Files.writeString(file, DEFAULT, StandardCharsets.UTF_8);
                return DEFAULT;
            }
            String json = Files.readString(file, StandardCharsets.UTF_8);
            String problem = validate(json);
            if (problem != null) {
                MMORPG.LOGGER.error("[MMORPG] cinematique.json invalide ({}) : définition par défaut utilisée", problem);
                return DEFAULT;
            }
            return json;
        } catch (Exception e) {
            MMORPG.LOGGER.error("[MMORPG] Impossible de lire cinematique.json : définition par défaut utilisée", e);
            return DEFAULT;
        }
    }
}
