package com.mmorpg.server;

import com.mmorpg.network.Net;
import com.mmorpg.network.Payloads;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;

/** Envoi des textes de combat flottants aux joueurs concernes. */
public final class CombatTexts {
    private CombatTexts() {
    }

    public static void send(ServerPlayer viewer, double x, double y, double z, float amount, int kind) {
        Net.toPlayer(viewer, new Payloads.CombatText(x, y, z, amount, kind));
    }

    /** Affiche un texte au-dessus d'une entite pour tous les joueurs qui la voient. */
    public static void above(Entity entity, float amount, int kind) {
        double y = entity.getY() + entity.getBbHeight() + 0.25;
        Net.toTrackingAndSelf(entity, new Payloads.CombatText(entity.getX(), y, entity.getZ(), amount, kind));
    }
}
