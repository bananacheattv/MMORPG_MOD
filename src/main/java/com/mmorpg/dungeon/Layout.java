package com.mmorpg.dungeon;

/**
 * Plan commun a tous les donjons de boss, en coordonnees relatives a l'origine (le sol est a y = 0, l'interieur commence a y = 1).
 * Le joueur traverse, dans l'ordre : le hall d'entree, six salles rondes (une epreuve chacune, la derniere est un combat d'elite),
 * deux couloirs a fosse (parcours de saut), cinq couloirs d'embuscade, la galerie des gardiens, l'antre du boss puis le tresor.
 * Environ vingt minutes de jeu en solo.
 */
public final class Layout {
    private Layout() {
    }

    /** Salle ronde : centre {@code z}, rayon {@code r}, hauteur sous plafond {@code h} (avant la coupole). */
    public record Room(int z, int r, int h) {
        public int z0() {
            return z - r;
        }

        public int z1() {
            return z + r;
        }
    }

    /** Couloir : cellules libres de {@code z0} a {@code z1} comprises. */
    public record Corridor(int z0, int z1) {
        public int length() {
            return z1 - z0 + 1;
        }
    }

    public static final Room HALL = new Room(12, 11, 13);
    public static final Corridor C0 = new Corridor(24, 49);
    public static final Room R1 = new Room(65, 15, 12);
    public static final Corridor P1 = new Corridor(81, 116);
    public static final Room R2 = new Room(131, 14, 12);
    public static final Corridor C2 = new Corridor(146, 165);
    public static final Room R3 = new Room(181, 15, 12);
    public static final Corridor P2 = new Corridor(197, 232);
    public static final Room R4 = new Room(248, 15, 12);
    public static final Corridor C4 = new Corridor(264, 283);
    public static final Room R5 = new Room(298, 14, 12);
    public static final Corridor C5 = new Corridor(313, 330);
    public static final Room ELITE = new Room(350, 19, 14);
    public static final Corridor C6 = new Corridor(370, 385);
    public static final Corridor GALLERY = new Corridor(386, 405);
    public static final Room ARENA = new Room(427, 21, 16);
    public static final Room TREASURE = new Room(456, 7, 9);

    /** Les six salles d'epreuve dans l'ordre, puis l'indice du couloir qui suit chacune (voir {@link #AFTER}). */
    public static final Room[] ROOMS = {R1, R2, R3, R4, R5, ELITE};
    /** Couloir qui suit chaque salle d'epreuve. */
    public static final Corridor[] AFTER = {P1, C2, P2, C4, C5, C6};
    /** Couloir qui precede chaque salle d'epreuve. */
    public static final Corridor[] BEFORE = {C0, P1, C2, P2, C4, C5};
    /** Couloirs a fosse (parcours de saut). */
    public static final Corridor[] PITS = {P1, P2};
    /** Couloirs d'embuscade (monstres qui surgissent a l'arrivee du joueur) : tous les couloirs sans fosse apres une salle. */
    public static final Corridor[] AMBUSHES = {C2, C4, C5, C6};

    public static final int PIT_HALF = 4, PIT_DEPTH = 9, DECK = 2;
    /** Demi-largeur du berceau des couloirs. */
    public static final int COR_HALF = 4;
    /** Longueur totale (derniere cellule du tresor, bord compris). */
    public static final int LENGTH = TREASURE.z1() + 6;
    /** Aretes de la boite englobante d'un exemplaire, en x. */
    public static final int HALF_WIDTH = 34;
}
