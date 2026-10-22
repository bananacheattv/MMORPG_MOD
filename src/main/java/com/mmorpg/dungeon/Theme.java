package com.mmorpg.dungeon;

import com.mmorpg.hub.HubCtx;
import com.mmorpg.world.DungeonBuilder.DungeonType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Une ambiance de donjon : materiaux de la coque et decor de chaque espace. Le plan (salle d'entree, deux salles de combat
 * reliees par un couloir a fosse, galerie des gardiens, arene du boss, tresor) est commun et defini dans {@link StyledDungeon}.
 */
abstract class Theme {
    /** Plan commun, en coordonnees relatives a l'origine du donjon (le sol est a y = 0, l'interieur commence a y = 1). */
    static final int HALL_Z = 12, HALL_R = 11, HALL_H = 13;
    static final int A_Z = 52, A_R = 15, A_H = 12;
    static final int PIT_Z0 = 66, PIT_Z1 = 86, PIT_HALF = 4, PIT_DEPTH = 9;
    static final int B_Z = 100, B_R = 14, B_H = 12;
    static final int GAL_Z0 = 114, GAL_Z1 = 132;
    static final int ARENA_Z = 154, ARENA_R = 21, ARENA_H = 16;
    static final int TREASURE_Z = 183, TREASURE_R = 7;

    final HubCtx c;
    final DungeonType type;

    Theme(HubCtx c, DungeonType type) {
        this.c = c;
        this.type = type;
    }

    /** Bloc de la coque en (x, y, z) ; {@code back} : couche de soutien exterieure. */
    abstract BlockState shell(int x, int y, int z, Carve.Kind kind, boolean back);

    /** Ajustements de forme propres a l'ambiance (cavernes ondulees, tours...). Appele apres le plan commun. */
    void shape(Carve v) {
    }

    /** Part des cellules de plafond et de mur remplacees par une source de lumiere. */
    float lightChance() {
        return 0.045F;
    }

    /** Source de lumiere integree a la coque. */
    abstract BlockState light(int x, int y, int z);

    abstract void hall();

    abstract void roomA();

    /** Remplit la fosse du couloir central (lave, glace, vide...) et orne le pont. */
    abstract void pit();

    abstract void roomB();

    abstract void gallery();

    abstract void arena();

    abstract void treasure();

    /** Nom du boss et sous-titre de l'annonce d'entree dans l'arene. */
    abstract String arenaName();

    // ------------------------------------------------------------------ outils

    final boolean arenaRegion(int z) {
        return z >= GAL_Z1;
    }

    final double arenaDist(int x, int z) {
        return Math.hypot(x, z - ARENA_Z);
    }

    final float rnd(int x, int y, int z, int salt) {
        return c.rnd(x, y, z, salt);
    }
}
