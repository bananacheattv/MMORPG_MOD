package com.mmorpg.social;

import com.mmorpg.network.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Mode photo : /photo (solo) ou /photo groupe (tous les membres du groupe proches, même compte à rebours). */
public final class GroupPhoto {
    private GroupPhoto() {}

    public static void start(ServerPlayer p, boolean group) {
        int sent = 0;
        var tag = new CompoundTag();
        tag.putInt("seconds", 8);
        Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.PHOTO, tag));
        if (!group) return;
        for (var other : com.mmorpg.server.PartyManager.onlineMembers(p)) {
            if (other == p || other.level() != p.level() || other.distanceToSqr(p) > 40 * 40) continue;
            other.sendSystemMessage(Component.literal(p.getName().getString() + " lance une photo de groupe : souriez, interface masquée !").withColor(0xF2D26B));
            Net.toPlayer(other, new S2COpenScreen(S2COpenScreen.PHOTO, tag));
            sent++;
        }
        p.sendSystemMessage(Component.literal(sent == 0 ? "Photo lancée (aucun membre du groupe à proximité)." : "Photo de groupe : " + (sent + 1) + " joueurs, capture dans 8 secondes.").withColor(0xF2D26B));
    }
}
