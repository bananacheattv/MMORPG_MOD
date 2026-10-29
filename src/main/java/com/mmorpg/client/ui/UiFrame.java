package com.mmorpg.client.ui;

/**
 * Cadre commun de toutes les interfaces du mod : identique au menu d'inventaire elargi
 * (grille centree, bouton "Revenir au jeu" au centre). Taille en unites d'interface :
 * {@code min(largeur - MARGIN, MAX_W)} x {@code min(hauteur - MARGIN, MAX_H)}.
 */
public final class UiFrame {
    public static final int MAX_W = 900;
    public static final int MAX_H = 430;
    public static final int MARGIN = 12;
    /** Hauteur de conception par defaut des ecrans (ancien panneau 440 x 272). */
    public static final int DESIGN_H = 272;

    private UiFrame() {
    }

    public static int width(int guiWidth) {
        return Math.min(guiWidth - MARGIN, MAX_W);
    }

    public static int height(int guiHeight) {
        return Math.min(guiHeight - MARGIN, MAX_H);
    }

    /** Echelle de contenu : le cadre fait toujours la taille de l'inventaire, le contenu est agrandi en proportion. */
    public static float scale(int guiHeight, int designHeight) {
        return Math.max(1f, height(guiHeight) / (float) Math.max(1, designHeight));
    }
}
