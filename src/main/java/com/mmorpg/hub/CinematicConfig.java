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
 * <p>Version 2 : un "parcours" global (images cles horodatees en secondes, interpolation d'Hermite : mouvement continu, sans arret
 * entre les plans) pilote la camera ; les plans ne portent plus que les titres, sons, particules et mises en scene.</p>
 *
 * <pre>
 * { "version": 2, "fondEntree": 1.4, "fondSortie": 1.2,
 *   "parcours": [ {"t": 0, "camera": [x,y,z], "cible": [x,y,z], "fov": 70}, ... ],   // "@joueur" / "@regard" = yeux / regard du joueur
 *   "plans": [ {
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

    /** Version du format : 2 = parcours global continu. */
    public static final int VERSION = 2;

    public static final String DEFAULT = """
            {
              "version": 2,
              "fondEntree": 1.6,
              "fondSortie": 1.4,
              "parcours": [
                {"t": 0, "camera": [0, 40, 108], "cible": [0, 18, 40], "fov": 76},
                {"t": 6, "camera": [0, 35, 78], "cible": [0, 27, 36], "fov": 70},
                {"t": 10, "camera": [-2, 46, 50], "cible": [0, 16, 14], "fov": 68},
                {"t": 12.8, "camera": [-22, 46, 32], "cible": [0, 14, 6], "fov": 66},
                {"t": 15, "camera": [-38, 36, 16], "cible": [0, 12, 0], "fov": 64},
                {"t": 19, "camera": [-30, 16, -2], "cible": [-12, 8, -8], "fov": 62},
                {"t": 22.5, "camera": [-7.8, 7, -7.8], "cible": [-20, 5.5, -20], "fov": 60},
                {"t": 27.5, "camera": [-9.4, 7.8, -9.4], "cible": [-21, 5, -21], "fov": 56},
                {"t": 30.2, "camera": [0, 12, -15], "cible": [0, 7, -26], "fov": 56},
                {"t": 33, "camera": [7.8, 7, -7.8], "cible": [20, 5.5, -20], "fov": 60},
                {"t": 36.3, "camera": [9.4, 7.8, -9.4], "cible": [21, 5, -21], "fov": 56},
                {"t": 39.2, "camera": [15.5, 10, 0], "cible": [27, 6, 0], "fov": 56},
                {"t": 42, "camera": [7.8, 7, 7.8], "cible": [20, 5.5, 20], "fov": 60},
                {"t": 45.3, "camera": [9.4, 7.8, 9.4], "cible": [21, 5, 21], "fov": 56},
                {"t": 48.2, "camera": [0, 13, 15], "cible": [0, 9, 26], "fov": 56},
                {"t": 51, "camera": [-7.8, 7, 7.8], "cible": [-20, 5.5, 20], "fov": 60},
                {"t": 54.3, "camera": [-9.4, 7.8, 9.4], "cible": [-21, 5, 21], "fov": 56},
                {"t": 57.5, "camera": [-6, 20, 28], "cible": [0, 12, 6], "fov": 62},
                {"t": 59.8, "camera": [0, 42, 42], "cible": [0, 14, 10], "fov": 60},
                {"t": 62, "camera": [0, 34, 64], "cible": [0, 14, 14], "fov": 64},
                {"t": 63.6, "camera": [0, 16, 66], "cible": [0, 9, 24], "fov": 66},
                {"t": 64.9, "camera": [0, 7, 57], "cible": [0, 5, 36], "fov": 70},
                {"t": 66, "camera": "@joueur", "cible": "@regard", "fov": -1}
              ],
              "plans": [
                {
                  "nom": "survol",
                  "duree": 20,
                  "rythme": "linear",
                  "transition": "coupe",
                  "titre": {"texte": "ELDORIA", "sous": "Le hub des héros", "couleur": "#FFD060", "debut": 0.14, "fin": 0.82},
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
                  "nom": "guerrier",
                  "duree": 9,
                  "rythme": "linear",
                  "transition": "coupe",
                  "classe": "GUERRIER",
                  "titre": {"texte": "LE GUERRIER", "sous": "Force et Férocité — la lame avant tout", "couleur": "#FF6A50", "debut": 0.28, "fin": 0.97},
                  "sons": [
                    {"t": 0.0, "son": "minecraft:block.anvil.use", "volume": 0.5, "hauteur": 0.6},
                    {"t": 0.35, "son": "minecraft:item.trident.thunder", "volume": 0.5, "hauteur": 0.8}
                  ],
                  "particules": [
                    {"t": 0.0, "type": "flame", "pos": [-20, 1.4, -20], "nombre": 40, "dispersion": [1.1, 1.4, 1.1], "vitesse": 0.04},
                    {"t": 0.4, "type": "lava", "pos": [-20, 3, -20], "nombre": 14, "dispersion": [1, 1, 1], "vitesse": 0.05},
                    {"t": 0.5, "type": "crit", "pos": [-20, 4, -20], "nombre": 30, "dispersion": [1, 1, 1], "vitesse": 0.3}
                  ]
                },
                {
                  "nom": "mage",
                  "duree": 9,
                  "rythme": "linear",
                  "transition": "coupe",
                  "classe": "MAGE",
                  "titre": {"texte": "LE MAGE", "sous": "Intelligence et Esprit — l'arcane obéit", "couleur": "#B07CFF", "debut": 0.28, "fin": 0.97},
                  "sons": [
                    {"t": 0.0, "son": "minecraft:block.enchantment_table.use", "volume": 0.8, "hauteur": 0.9},
                    {"t": 0.4, "son": "minecraft:entity.evoker.cast_spell", "volume": 0.5, "hauteur": 1.1}
                  ],
                  "particules": [
                    {"t": 0.0, "type": "enchant", "pos": [20, 3, -20], "nombre": 80, "dispersion": [1.5, 1.5, 1.5], "vitesse": 0.5},
                    {"t": 0.45, "type": "witch", "pos": [20, 4, -20], "nombre": 40, "dispersion": [1.2, 1.2, 1.2], "vitesse": 0.05},
                    {"t": 0.55, "type": "end_rod", "pos": [20, 6, -20], "nombre": 30, "dispersion": [0.8, 0.8, 0.8], "vitesse": 0.08}
                  ]
                },
                {
                  "nom": "tank",
                  "duree": 9,
                  "rythme": "linear",
                  "transition": "coupe",
                  "classe": "TANK",
                  "titre": {"texte": "LE PALADIN", "sous": "Vitalité et Endurance — le rempart du groupe", "couleur": "#58A8FF", "debut": 0.28, "fin": 0.97},
                  "sons": [
                    {"t": 0.0, "son": "minecraft:item.shield.block", "volume": 0.8, "hauteur": 0.8},
                    {"t": 0.4, "son": "minecraft:block.anvil.land", "volume": 0.4, "hauteur": 0.7}
                  ],
                  "particules": [
                    {"t": 0.0, "type": "wax_on", "pos": [20, 2, 20], "nombre": 30, "dispersion": [1.4, 1.6, 1.4], "vitesse": 0.5},
                    {"t": 0.4, "type": "electric_spark", "pos": [20, 3, 20], "nombre": 30, "dispersion": [1.3, 1.6, 1.3], "vitesse": 0.4},
                    {"t": 0.55, "type": "snowflake", "pos": [20, 5, 20], "nombre": 30, "dispersion": [1.4, 1.2, 1.4], "vitesse": 0.02}
                  ]
                },
                {
                  "nom": "archer",
                  "duree": 9,
                  "rythme": "linear",
                  "transition": "coupe",
                  "classe": "ARCHER",
                  "titre": {"texte": "L'ARCHER", "sous": "Agilité et Dextérité — jamais là où l'on frappe", "couleur": "#6CE06C", "debut": 0.28, "fin": 0.97},
                  "sons": [
                    {"t": 0.0, "son": "minecraft:entity.arrow.shoot", "volume": 0.6, "hauteur": 1.1},
                    {"t": 0.45, "son": "minecraft:block.grass.break", "volume": 0.6, "hauteur": 0.8}
                  ],
                  "particules": [
                    {"t": 0.0, "type": "happy_villager", "pos": [-20, 2, 20], "nombre": 30, "dispersion": [1.6, 1.4, 1.6], "vitesse": 0.02},
                    {"t": 0.4, "type": "cherry_leaves", "pos": [-20, 6, 20], "nombre": 40, "dispersion": [2.2, 0.5, 2.2], "vitesse": 0.02},
                    {"t": 0.5, "type": "cloud", "pos": [-19, 1.6, 20], "nombre": 12, "dispersion": [0.6, 0.3, 0.6], "vitesse": 0.03}
                  ]
                },
                {
                  "nom": "retour",
                  "duree": 10,
                  "rythme": "linear",
                  "transition": "coupe",
                  "titre": {"texte": "À VOUS DE JOUER", "sous": "Parlez aux Maîtres de classe : essayez, puis choisissez votre voie", "couleur": "#FFE6A0", "debut": 0.35, "fin": 0.95},
                  "sons": [
                    {"t": 0.0, "son": "minecraft:block.beacon.power_select", "volume": 0.7, "hauteur": 1.0},
                    {"t": 0.85, "son": "minecraft:entity.player.levelup", "volume": 0.5, "hauteur": 1.2}
                  ],
                  "particules": [
                    {"t": 0.5, "type": "end_rod", "pos": [0, 4, 40], "nombre": 40, "dispersion": [3, 2, 3], "vitesse": 0.03}
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
            if (o.has("parcours") && !o.getAsJsonArray("parcours").isEmpty()) {
                JsonArray k = o.getAsJsonArray("parcours");
                return total + k.get(k.size() - 1).getAsJsonObject().get("t").getAsDouble() + 20;     // marge : pre-chauffage du rendu
            }
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
            boolean global = o.has("parcours");
            if (global) {
                JsonArray k = o.getAsJsonArray("parcours");
                if (k.size() < 2) return "parcours : au moins deux images clés";
                double prev = -1;
                for (int i = 0; i < k.size(); i++) {
                    JsonObject key = k.get(i).getAsJsonObject();
                    if (!key.has("t") || !key.has("camera") || !key.has("cible")) return "parcours " + (i + 1) + " : t, camera et cible requis";
                    double t = key.get("t").getAsDouble();
                    if (t <= prev) return "parcours " + (i + 1) + " : les temps doivent croître";
                    prev = t;
                }
            }
            for (int i = 0; i < plans.size(); i++) {
                JsonObject p = plans.get(i).getAsJsonObject();
                if (!p.has("duree") || p.get("duree").getAsDouble() <= 0.1) return "plan " + (i + 1) + " : durée invalide";
                if (global) continue;
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
            try {
                JsonObject root = JsonParser.parseString(json).getAsJsonObject();
                if (!root.has("version") || root.get("version").getAsInt() < VERSION) {      // ancien format : sauvegarde puis remplacement par la version fluide
                    Files.writeString(file.resolveSibling("cinematique.json.ancienne"), json, StandardCharsets.UTF_8);
                    Files.writeString(file, DEFAULT, StandardCharsets.UTF_8);
                    MMORPG.LOGGER.info("[MMORPG] cinematique.json mis à jour vers la version {} (ancien fichier : cinematique.json.ancienne)", VERSION);
                    return DEFAULT;
                }
            } catch (Exception ignored) {
            }
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
