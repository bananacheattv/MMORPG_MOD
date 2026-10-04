package com.mmorpg.client;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Envoi des paquets du client vers le serveur. */
public final class ClientNet {
    private ClientNet() {
    }

    public static void send(CustomPacketPayload payload) {
        ClientPacketDistributor.sendToServer(payload);
    }
}
