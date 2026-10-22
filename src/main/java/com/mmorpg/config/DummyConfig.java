package com.mmorpg.config;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Reglages des mannequins d'entrainement (config/mmorpg/mannequin.json).
 * Chaque variante a sa defense physique et sa resistance magique, appliquees avec la meme formule que les monstres
 * (reduction = defense / (defense + 200 + 10 x niveau de l'attaquant), plafonnee a 80 %).
 */
public class DummyConfig {
    /** Niveau affiche (cadre de cible) et utilise par les competences qui en dependent. */
    public int level = 100;
    /** Fenetre de calcul du DPS, en secondes. */
    public int dpsWindowSeconds = 10;
    /** Distance (en blocs) a partir de laquelle le panneau de statistiques s'affiche. */
    public int panelRange = 24;
    /** Prix en pieces d'or de la recette de la Forge Arcanique, par variante (niveau 1). */
    public Map<String, Variant> variants = new LinkedHashMap<>();

    public static class Variant {
        public String name = "";
        public float physicalDefense = 0;
        public float magicDefense = 0;
        /** Mannequin combattant : % de la vie maximale du joueur retiree par coup (0 = ne frappe pas). */
        public float strikePercent = 0;
        /** Delai entre deux coups, en secondes. */
        public float strikeIntervalSeconds = 3;

        public Variant() {
        }

        public Variant(String name, float physicalDefense, float magicDefense) {
            this.name = name;
            this.physicalDefense = physicalDefense;
            this.magicDefense = magicDefense;
        }

        public Variant(String name, float physicalDefense, float magicDefense, float strikePercent, float strikeIntervalSeconds) {
            this(name, physicalDefense, magicDefense);
            this.strikePercent = strikePercent;
            this.strikeIntervalSeconds = strikeIntervalSeconds;
        }
    }

    public static DummyConfig defaults() {
        DummyConfig c = new DummyConfig();
        c.variants.put("standard", new Variant("Mannequin d'entraînement", 0, 0));
        c.variants.put("blinde", new Variant("Mannequin blindé", 1500, 300));
        c.variants.put("magique", new Variant("Mannequin magique", 300, 1500));
        c.variants.put("duelliste", new Variant("Mannequin duelliste", 800, 800, 18, 3));
        return c;
    }

    /** Complete les valeurs manquantes (ancien fichier ou fichier edite a la main). */
    public DummyConfig sanitize() {
        DummyConfig d = defaults();
        if (variants == null) variants = new LinkedHashMap<>();
        d.variants.forEach(variants::putIfAbsent);
        level = Math.max(1, Math.min(100, level));
        dpsWindowSeconds = Math.max(1, Math.min(60, dpsWindowSeconds));
        panelRange = Math.max(4, Math.min(64, panelRange));
        for (Variant v : variants.values()) {
            if (v.name == null || v.name.isBlank()) v.name = "Mannequin";
            v.physicalDefense = Math.max(0, v.physicalDefense);
            v.magicDefense = Math.max(0, v.magicDefense);
            v.strikePercent = Math.max(0, Math.min(50, v.strikePercent));
            v.strikeIntervalSeconds = Math.max(1.5F, Math.min(30, v.strikeIntervalSeconds));
        }
        return this;
    }

    public Variant variant(String id) {
        Variant v = variants.get(id);
        return v != null ? v : defaults().variants.get(id);
    }
}
