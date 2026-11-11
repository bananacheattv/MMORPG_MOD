package com.mmorpg.util;

/** Distance d'affichage des modeles 3D du mod (blocs). Vanilla : ~64 blocs pour une entite de taille moyenne. */
public final class RenderDist {
    public static final double ENTITY = 256.0;
    public static final int BLOCK = 256;

    private RenderDist() {}

    public static boolean entity(double sqDistance) {
        return sqDistance < ENTITY * ENTITY;
    }
}
