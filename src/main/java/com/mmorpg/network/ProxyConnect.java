package com.mmorpg.network;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import io.netty.buffer.ByteBuf;

/** BungeeCord plugin channel, intercepted by Velocity before it reaches the client. */
public record ProxyConnect(byte[] bytes) implements CustomPacketPayload {
    public static final Type<ProxyConnect> TYPE = new Type<>(Identifier.fromNamespaceAndPath("bungeecord", "main"));
    public static final StreamCodec<ByteBuf, ProxyConnect> CODEC = new StreamCodec<>() {
        public ProxyConnect decode(ByteBuf buf) { byte[] data = new byte[buf.readableBytes()]; buf.readBytes(data); return new ProxyConnect(data); }
        public void encode(ByteBuf buf, ProxyConnect value) { buf.writeBytes(value.bytes()); }
    };
    public static ProxyConnect to(String server) {
        try {
            var bytes = new java.io.ByteArrayOutputStream();
            var out = new java.io.DataOutputStream(bytes);
            out.writeUTF("Connect"); out.writeUTF(server);
            return new ProxyConnect(bytes.toByteArray());
        } catch (java.io.IOException e) { throw new IllegalStateException(e); }
    }
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
