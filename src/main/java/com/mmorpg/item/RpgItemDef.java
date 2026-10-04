package com.mmorpg.item;

import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.rpg.Rarity;
import com.mmorpg.rpg.StatBlock;

/** Donnees RPG d'un equipement : classe et niveau requis, rarete, statistiques, set eventuel. */
public record RpgItemDef(String id, PlayerClass playerClass, int level, Rarity rarity, StatBlock stats, String setId,
                         Slot slot, String lore) {

    public enum Slot { WEAPON, HEAD, CHEST, LEGS, FEET }

    public boolean usableBy(PlayerClass cls, int playerLevel) {
        return (playerClass == PlayerClass.NONE || playerClass == cls) && playerLevel >= level;
    }
}
