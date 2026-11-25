package com.mmorpg.security;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

/**
 * Positions de bloc recues d'un client : on verifie la distance et le chargement du troncon AVANT toute lecture du monde.
 * Lire un bloc d'un troncon non charge force son chargement (voire sa generation) sur le thread du serveur : un client modifie
 * pourrait ainsi faire generer des milliers de troncons lointains (deni de service, monde qui grossit).
 */
public final class Reach {
    private Reach() {
    }

    /** Vrai si {@code pos} est a moins de {@code maxDistSqr} (distance au carre, centre du bloc) et deja charge. */
    public static boolean within(ServerPlayer p, BlockPos pos, double maxDistSqr) {
        return pos != null && p.distanceToSqr(Vec3.atCenterOf(pos)) < maxDistSqr && p.level().isLoaded(pos);
    }
}
