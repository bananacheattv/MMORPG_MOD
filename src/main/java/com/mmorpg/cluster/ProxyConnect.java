package com.mmorpg.cluster;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Message au proxy sur le canal « bungeecord:main » (BungeeCord, Velocity avec {@code bungee-plugin-message-channel = true}) : octets bruts,
 * ici « Connect » puis le nom du serveur. Le proxy lit ce message venant du serveur de jeu et ne le transmet pas au joueur.
 * Declare comme paquet du mod pour que NeoForge accepte de l'envoyer ; le client ne le recoit jamais (et l'ignorerait).
 */
public record ProxyConnect(byte[] data) implements CustomPacketPayload {
    public static final Type<ProxyConnect> TYPE = new Type<>(Identifier.parse("bungeecord:main"));
    public static final StreamCodec<ByteBuf, ProxyConnect> CODEC = StreamCodec.of(
            (buf, p) -> buf.writeBytes(p.data()),
            buf -> {
                byte[] b = new byte[buf.readableBytes()];
                buf.readBytes(b);
                return new ProxyConnect(b);
            });

    @Override
    public Type<ProxyConnect> type() {
        return TYPE;
    }
}
