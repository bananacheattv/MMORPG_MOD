package com.mmorpg.item;

import com.mmorpg.rpg.Rarity;

/** Objet d'invocation de boss, a utiliser sur un Autel d'Invocation. */
public class SummonKeyItem extends RpgMaterialItem {
    private final String bossKey;

    public SummonKeyItem(Properties properties, Rarity rarity, String bossKey, String bossName, int bossLevel) {
        super(properties.stacksTo(16), rarity, "Invocation",
                "Invoque : " + bossName + " (niveau " + bossLevel + ")\nUtilisez-le sur un Autel d'Invocation.");
        this.bossKey = bossKey;
    }

    public String bossKey() {
        return bossKey;
    }
}
