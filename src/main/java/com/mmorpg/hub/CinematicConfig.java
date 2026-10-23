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
    public static final int VERSION = 3;

    public static final String DEFAULT = """
            {
              "version": 3,
              "fondEntree": 1.6,
              "fondSortie": 1.4,
              "parcours": [
                {"t": 0, "camera": [0, 40, 108], "cible": [0, 18, 40], "fov": 76},
                {"t": 6, "camera": [0, 35, 78], "cible": [0, 27, 36], "fov": 70},
                {"t": 10, "camera": [-2, 46, 50], "cible": [0, 16, 14], "fov": 68},
                {"t": 12.8, "camera": [-22, 46, 32], "cible": [-6, 12, 4], "fov": 66},
                {"t": 15.5, "camera": [-38, 34, 14], "cible": [-14, 9, -6], "fov": 64},
                {"t": 18.8, "camera": [-26, 20, -2], "cible": [-18, 8, -14], "fov": 62},
                {"t": 22.5, "camera": [-7.78, 7.0, -7.78], "cible": [-20.01, 5.5, -20.01], "fov": 60},
                {"t": 23.25, "camera": [-8.15, 7.12, -7.9], "cible": [-20.47, 5.42, -19.84], "fov": 59},
                {"t": 24.0, "camera": [-8.52, 7.24, -8.0], "cible": [-20.94, 5.35, -19.66], "fov": 59},
                {"t": 24.75, "camera": [-8.9, 7.36, -8.1], "cible": [-21.4, 5.28, -19.47], "fov": 58},
                {"t": 25.5, "camera": [-9.29, 7.48, -8.19], "cible": [-21.86, 5.2, -19.27], "fov": 58},
                {"t": 26.25, "camera": [-9.68, 7.6, -8.26], "cible": [-22.32, 5.12, -19.06], "fov": 57},
                {"t": 27.0, "camera": [-10.07, 7.72, -8.33], "cible": [-22.78, 5.05, -18.84], "fov": 56},
                {"t": 27.75, "camera": [-10.6, 8.39, -8.43], "cible": [-23.24, 5.86, -18.48], "fov": 56},
                {"t": 28.5, "camera": [-11.98, 10.0, -7.39], "cible": [-25.17, 8.29, -15.53], "fov": 56},
                {"t": 29.25, "camera": [-13.49, 11.14, -4.62], "cible": [-27.78, 10.17, -9.51], "fov": 57},
                {"t": 30.0, "camera": [-14.07, 11.61, -0.67], "cible": [-29.06, 11.15, -1.38], "fov": 58},
                {"t": 30.75, "camera": [-13.14, 11.32, 3.36], "cible": [-27.91, 11.07, 7.13], "fov": 59},
                {"t": 31.5, "camera": [-11.11, 10.32, 6.33], "cible": [-24.81, 9.94, 14.13], "fov": 59},
                {"t": 32.25, "camera": [-8.99, 8.79, 7.74], "cible": [-21.5, 7.97, 18.52], "fov": 60},
                {"t": 33.0, "camera": [-7.78, 7.0, 7.78], "cible": [-20.01, 5.5, 20.01], "fov": 60},
                {"t": 33.75, "camera": [-7.95, 7.18, 8.34], "cible": [-19.75, 5.39, 20.71], "fov": 59},
                {"t": 34.5, "camera": [-8.1, 7.36, 8.91], "cible": [-19.46, 5.27, 21.41], "fov": 58},
                {"t": 35.25, "camera": [-8.23, 7.55, 9.5], "cible": [-19.16, 5.16, 22.11], "fov": 57},
                {"t": 36.0, "camera": [-8.33, 7.73, 10.09], "cible": [-18.83, 5.05, 22.8], "fov": 56},
                {"t": 36.75, "camera": [-8.34, 8.82, 10.87], "cible": [-18.07, 6.48, 23.54], "fov": 56},
                {"t": 37.5, "camera": [-6.94, 10.29, 12.32], "cible": [-14.5, 8.74, 25.74], "fov": 56},
                {"t": 38.25, "camera": [-3.99, 11.28, 13.69], "cible": [-8.2, 10.41, 28.15], "fov": 57},
                {"t": 39.0, "camera": [-0.08, 11.62, 14.03], "cible": [-0.16, 11.21, 29.05], "fov": 58},
                {"t": 39.75, "camera": [3.73, 11.25, 12.97], "cible": [7.97, 11.0, 27.66], "fov": 59},
                {"t": 40.5, "camera": [6.48, 10.23, 10.95], "cible": [14.53, 9.83, 24.56], "fov": 59},
                {"t": 41.25, "camera": [7.77, 8.73, 8.93], "cible": [18.62, 7.89, 21.4], "fov": 60},
                {"t": 42.0, "camera": [7.78, 7.0, 7.78], "cible": [20.01, 5.5, 20.01], "fov": 60},
                {"t": 42.75, "camera": [8.34, 7.18, 7.95], "cible": [20.71, 5.39, 19.75], "fov": 59},
                {"t": 43.5, "camera": [8.91, 7.36, 8.1], "cible": [21.41, 5.27, 19.46], "fov": 58},
                {"t": 44.25, "camera": [9.5, 7.55, 8.23], "cible": [22.11, 5.16, 19.16], "fov": 57},
                {"t": 45.0, "camera": [10.09, 7.73, 8.33], "cible": [22.8, 5.05, 18.83], "fov": 56},
                {"t": 45.75, "camera": [10.87, 8.82, 8.34], "cible": [23.54, 6.48, 18.07], "fov": 56},
                {"t": 46.5, "camera": [12.32, 10.29, 6.94], "cible": [25.74, 8.74, 14.5], "fov": 56},
                {"t": 47.25, "camera": [13.69, 11.28, 3.99], "cible": [28.15, 10.41, 8.2], "fov": 57},
                {"t": 48.0, "camera": [14.03, 11.62, 0.08], "cible": [29.05, 11.21, 0.16], "fov": 58},
                {"t": 48.75, "camera": [12.97, 11.25, -3.73], "cible": [27.66, 11.0, -7.97], "fov": 59},
                {"t": 49.5, "camera": [10.95, 10.23, -6.48], "cible": [24.56, 9.83, -14.53], "fov": 59},
                {"t": 50.25, "camera": [8.93, 8.73, -7.77], "cible": [21.4, 7.89, -18.62], "fov": 60},
                {"t": 51.0, "camera": [7.78, 7.0, -7.78], "cible": [20.01, 5.5, -20.01], "fov": 60},
                {"t": 51.75, "camera": [7.88, 7.13, -8.21], "cible": [19.76, 5.42, -20.58], "fov": 59},
                {"t": 52.5, "camera": [7.97, 7.27, -8.65], "cible": [19.5, 5.33, -21.15], "fov": 59},
                {"t": 53.25, "camera": [8.05, 7.4, -9.1], "cible": [19.22, 5.25, -21.72], "fov": 58},
                {"t": 54.0, "camera": [8.11, 7.53, -9.55], "cible": [18.92, 5.17, -22.28], "fov": 57},
                {"t": 54.75, "camera": [8.16, 7.67, -10.01], "cible": [18.61, 5.08, -22.85], "fov": 57},
                {"t": 55.5, "camera": [8.19, 7.8, -10.48], "cible": [18.29, 5.0, -23.4], "fov": 56},
                {"t": 56.5, "camera": [8.19, 7.8, -10.48], "cible": [18.29, 5.0, -23.4], "fov": 56},
                {"t": 57.0, "camera": [0, 40, 104], "cible": [0, 16, 30], "fov": 76, "coupe": true},
                {"t": 61.0, "camera": [0, 26, 78], "cible": [0, 14, 30], "fov": 72},
                {"t": 64.0, "camera": [0, 12, 62], "cible": [0, 8, 36], "fov": 70},
                {"t": 66.0, "camera": "@joueur", "cible": "@regard", "fov": -1}
              ],
              "plans": [
                {
                  "nom": "survol",
                  "duree": 20,
                  "rythme": "linear",
                  "transition": "coupe",
                  "titre": {"texte": "AELORIA", "sous": "Le hub des héros", "couleur": "#FFD060", "debut": 0.14, "fin": 0.82},
                  "sons": [
                    {"t": 0.0, "son": "mmorpg:sfx.rise", "volume": 0.7, "hauteur": 0.8},
                    {"t": 0.12, "son": "mmorpg:sfx.chime", "volume": 0.5, "hauteur": 0.7}
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
                    {"t": 0.0, "son": "mmorpg:sfx.warrior", "volume": 0.5, "hauteur": 0.6},
                    {"t": 0.35, "son": "mmorpg:sfx.boom", "volume": 0.5, "hauteur": 0.8}
                  ],
                  "particules": [
                    {"t": 0.0, "type": "flame", "pos": [-20, 1.4, -20], "nombre": 40, "dispersion": [1.1, 1.4, 1.1], "vitesse": 0.04},
                    {"t": 0.4, "type": "lava", "pos": [-20, 3, -20], "nombre": 14, "dispersion": [1, 1, 1], "vitesse": 0.05},
                    {"t": 0.5, "type": "crit", "pos": [-20, 4, -20], "nombre": 30, "dispersion": [1, 1, 1], "vitesse": 0.3}
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
                    {"t": 0.0, "son": "mmorpg:sfx.arrow", "volume": 0.6, "hauteur": 1.1},
                    {"t": 0.45, "son": "mmorpg:sfx.whoosh", "volume": 0.6, "hauteur": 0.8}
                  ],
                  "particules": [
                    {"t": 0.0, "type": "happy_villager", "pos": [-20, 2, 20], "nombre": 30, "dispersion": [1.6, 1.4, 1.6], "vitesse": 0.02},
                    {"t": 0.4, "type": "cherry_leaves", "pos": [-20, 6, 20], "nombre": 40, "dispersion": [2.2, 0.5, 2.2], "vitesse": 0.02},
                    {"t": 0.5, "type": "cloud", "pos": [-19, 1.6, 20], "nombre": 12, "dispersion": [0.6, 0.3, 0.6], "vitesse": 0.03}
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
                    {"t": 0.0, "son": "mmorpg:sfx.shield", "volume": 0.8, "hauteur": 0.8},
                    {"t": 0.4, "son": "mmorpg:sfx.boom", "volume": 0.4, "hauteur": 0.7}
                  ],
                  "particules": [
                    {"t": 0.0, "type": "wax_on", "pos": [20, 2, 20], "nombre": 30, "dispersion": [1.4, 1.6, 1.4], "vitesse": 0.5},
                    {"t": 0.4, "type": "electric_spark", "pos": [20, 3, 20], "nombre": 30, "dispersion": [1.3, 1.6, 1.3], "vitesse": 0.4},
                    {"t": 0.55, "type": "snowflake", "pos": [20, 5, 20], "nombre": 30, "dispersion": [1.4, 1.2, 1.4], "vitesse": 0.02}
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
                    {"t": 0.0, "son": "mmorpg:sfx.arcane", "volume": 0.8, "hauteur": 0.9},
                    {"t": 0.4, "son": "mmorpg:sfx.chime", "volume": 0.5, "hauteur": 1.1}
                  ],
                  "particules": [
                    {"t": 0.0, "type": "enchant", "pos": [20, 3, -20], "nombre": 80, "dispersion": [1.5, 1.5, 1.5], "vitesse": 0.5},
                    {"t": 0.45, "type": "witch", "pos": [20, 4, -20], "nombre": 40, "dispersion": [1.2, 1.2, 1.2], "vitesse": 0.05},
                    {"t": 0.55, "type": "end_rod", "pos": [20, 6, -20], "nombre": 30, "dispersion": [0.8, 0.8, 0.8], "vitesse": 0.08}
                  ]
                },
                {
                  "nom": "retour",
                  "duree": 10,
                  "rythme": "linear",
                  "transition": "coupe",
                  "titre": {"texte": "À VOUS DE JOUER", "sous": "Parlez aux Maîtres de classe : essayez, puis choisissez votre voie", "couleur": "#FFE6A0", "debut": 0.35, "fin": 0.95},
                  "sons": [
                    {"t": 0.0, "son": "mmorpg:sfx.bell", "volume": 0.7, "hauteur": 1.0},
                    {"t": 0.85, "son": "mmorpg:sfx.success", "volume": 0.5, "hauteur": 1.2}
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
