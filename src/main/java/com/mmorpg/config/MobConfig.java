package com.mmorpg.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration d'un monstre ou d'un boss (fichier config/mmorpg/mobs.json).
 * Tous les champs sont modifiables par l'administrateur du serveur.
 */
public class MobConfig {
    /** Nom affiche (vide = nom traduit par defaut). */
    public String displayName = "";
    public int minLevel = 1;
    public int maxLevel = 10;
    public double baseHealth = 100;
    public double healthPerLevel = 20;
    public double baseAttack = 8;
    public double attackPerLevel = 2;
    public double defensePerLevel = 1;
    /** Vitesse de deplacement (attribut vanilla, 0.25 = vitesse d'un zombie). */
    public double speed = 0.28;
    public double xpMultiplier = 1.0;
    /** Taille du modele (1.0 = normale). */
    public double scale = 1.0;
    public boolean boss = false;
    public Spawn spawn = new Spawn();
    public List<Drop> drops = new ArrayList<>();
    /** Parametres des capacites speciales (recharges en ticks, multiplicateurs de degats...). */
    public Map<String, Double> abilities = new LinkedHashMap<>();

    public static class Spawn {
        public boolean enabled = true;
        public int weight = 10;
        public int minGroup = 1;
        public int maxGroup = 3;
        /** Biomes autorises : identifiants ("minecraft:plains"), tags ("#minecraft:is_forest") ou "*" pour tous. */
        public List<String> biomes = new ArrayList<>();
        public List<String> dimensions = new ArrayList<>(List.of("minecraft:overworld"));
        /** N'apparait que dans l'obscurite (nuit ou grottes). */
        public boolean darkOnly = false;
        public int minY = -64;
        public int maxY = 320;
    }

    public static class Drop {
        public String item = "minecraft:air";
        public double chance = 1.0;
        public int min = 1;
        public int max = 1;

        public Drop() {
        }

        public Drop(String item, double chance, int min, int max) {
            this.item = item;
            this.chance = chance;
            this.min = min;
            this.max = max;
        }
    }

    public double ability(String key, double def) {
        Double v = abilities.get(key);
        return v == null ? def : v;
    }

    public int levelFor(java.util.Random random) {
        return minLevel >= maxLevel ? minLevel : minLevel + random.nextInt(maxLevel - minLevel + 1);
    }

    public double healthAt(int level) {
        return baseHealth + healthPerLevel * level;
    }

    public double attackAt(int level) {
        return baseAttack + attackPerLevel * level;
    }

    public double defenseAt(int level) {
        return defensePerLevel * level;
    }
}
