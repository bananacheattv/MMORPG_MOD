package com.mmorpg.dungeon;

import com.mmorpg.rpg.PlayerClass;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Equilibrage des donjons : le solo est la reference jouable avec n'importe quelle classe (Guerrier, Mage, Archer, Paladin).
 * Les facteurs de classe compensent la fragilite des classes a distance et la faible puissance offensive du Paladin ;
 * ils s'effacent progressivement quand le groupe grandit (le groupe ne devient pas trivial). Valeurs issues du modele
 * tools/balance/donjons_modele.py (voir DONJONS_EQUILIBRAGE.md).
 */
public final class DungeonBalance {
    private DungeonBalance() {
    }

    /** Facteur de degats subis (monstres de donjon -> joueur) en solo, par classe. */
    public static double takenSolo(PlayerClass c) {
        return switch (c) {
            case GUERRIER -> 0.55;
            case MAGE -> 0.22;
            case ARCHER -> 0.45;
            case TANK -> 0.55;
            default -> 1.0;
        };
    }

    /** Facteur de degats infliges (joueur -> monstre de donjon) en solo, par classe. */
    public static double dealtSolo(PlayerClass c) {
        return c == PlayerClass.TANK ? 2.5 : 1.0;
    }

    /** Regeneration de mana en donjon (les classes a sorts et a flèches vident leur reserve vite). */
    public static double manaRegen(PlayerClass c) {
        return c == PlayerClass.MAGE || c == PlayerClass.ARCHER ? 1.5 : 1.0;
    }

    public static boolean inDungeon(ServerLevel level) {
        return level.dimension().identifier().equals(com.mmorpg.MMORPG.id("donjons"));
    }

    /** Joueurs vivants a proximite (lui compris) : 1 = solo. */
    public static int partySize(ServerPlayer p) {
        int n = 0;
        for (ServerPlayer o : p.level().players()) {
            if (o.isAlive() && !o.isSpectator() && !o.isCreative() && o.distanceToSqr(p) < 64 * 64) n++;
        }
        return Math.max(1, n);
    }

    /** Le bonus de solo s'efface avec la taille du groupe : 100 % seul, 2/3 a deux, 40 % a quatre. */
    private static double fade(int party) {
        return 1.0 / (1.0 + 0.5 * (party - 1));
    }

    public static double taken(ServerPlayer p) {
        if (!inDungeon(p.level())) return 1.0;
        var cls = com.mmorpg.server.RpgPlayers.get(p).playerClass;
        return 1.0 - (1.0 - takenSolo(cls)) * fade(partySize(p));
    }

    public static double dealt(ServerPlayer p) {
        if (!inDungeon(p.level())) return 1.0;
        var cls = com.mmorpg.server.RpgPlayers.get(p).playerClass;
        return 1.0 + (dealtSolo(cls) - 1.0) * fade(partySize(p));
    }
}
