package com.mmorpg.dungeon;

import com.mmorpg.hub.HubCtx;
import com.mmorpg.world.DungeonBuilder.DungeonType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Une ambiance de donjon : materiaux de la coque et decor de chaque espace. Le plan (hall d'entree, six salles d'epreuve reliees par des
 * couloirs, galerie des gardiens, arene du boss, tresor) est commun et defini dans {@link Layout} ; l'assemblage dans {@link StyledDungeon}.
 */
abstract class Theme {
    /** Constantes du plan (voir {@link Layout}) sous les noms utilises par les ambiances. */
    static final int HALL_Z = Layout.HALL.z(), HALL_R = Layout.HALL.r(), HALL_H = Layout.HALL.h();
    static final int PIT_HALF = Layout.PIT_HALF, PIT_DEPTH = Layout.PIT_DEPTH, DECK = Layout.DECK;
    static final int GAL_Z0 = Layout.GALLERY.z0(), GAL_Z1 = Layout.GALLERY.z1();
    static final int ARENA_Z = Layout.ARENA.z(), ARENA_R = Layout.ARENA.r(), ARENA_H = Layout.ARENA.h();
    static final int TREASURE_Z = Layout.TREASURE.z(), TREASURE_R = Layout.TREASURE.r();

    /** Blocs d'accent d'une ambiance, utilises pour decorer les salles et couloirs generiques ({@link Rooms}). */
    record Look(Object pillar, Object accent, Block stair, Block lamp, Block fire, Object floorA, Object floorB, Object prop) {
    }

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

    /** Materiaux et noms des epreuves de cette ambiance. */
    abstract Trials.Palette palette();

    /** Quelle epreuve dans quelle salle ou couloir. */
    abstract Trials.Plan plan();

    /** Monstre d'elite qui garde la derniere salle avant l'antre du boss (cle de bestiaire). */
    abstract String eliteKey();

    /** Blocs d'accent de l'ambiance. */
    abstract Look look();

    abstract void hall();

    /** Premiere salle d'epreuve (decor signature de l'ambiance). */
    abstract void roomA(Layout.Room rm);

    /** Remplit la fosse d'un couloir (lave, glace, vide...) et orne le pont. */
    abstract void pit(int z0, int z1);

    /** Deuxieme salle d'epreuve (decor signature de l'ambiance). */
    abstract void roomB(Layout.Room rm);

    abstract void gallery();

    abstract void arena();

    abstract void treasure();

    /** Nom du boss et sous-titre de l'annonce d'entree dans l'arene. */
    abstract String arenaName();

    /** Phrase d'ambiance affichee en lettres qui s'ecrivent a l'entree du donjon. */
    abstract String lore();

    // ------------------------------------------------------------------ outils

    /** La salle d'epreuve d'indice {@code i} (voir {@link Layout#ROOMS}) est-elle un lac a traverser en sautant ? */
    final boolean jumpRoom(int i) {
        return plan().roomKind(i).equals(Trials.JUMPROOM);
    }

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
