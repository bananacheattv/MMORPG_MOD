package com.mmorpg.rpg;

import java.util.function.ToIntFunction;

/**
 * Vue minimale des donnees du joueur local, accessible depuis le code commun (infobulles d'objets).
 * Mise a jour par le client a chaque synchronisation ; valeurs neutres cote serveur.
 */
public final class ClientView {
    public static volatile PlayerClass playerClass = PlayerClass.NONE;
    public static volatile int level = 1;
    /** Nombre de pieces d'un set actuellement equipees par le joueur local. */
    public static volatile ToIntFunction<String> setPieceCounter = s -> 0;

    private ClientView() {
    }
}
