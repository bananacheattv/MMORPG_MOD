package com.mmorpg.replay;

import com.mmorpg.MMORPG;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/** Separate channel: no client-supplied positions, entity data or world edits reach the server. */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class ReplayWire {
    public record Request(String action) implements CustomPacketPayload {
        public static final Type<Request> TYPE = new Type<>(MMORPG.id("replay_request"));
        public static final StreamCodec<ByteBuf, Request> CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(24), Request::action, Request::new);
        @Override public Type<Request> type() { return TYPE; }
    }
    public record Reply(String action) implements CustomPacketPayload {
        public static final Type<Reply> TYPE = new Type<>(MMORPG.id("replay_reply"));
        public static final StreamCodec<ByteBuf, Reply> CODEC = StreamCodec.composite(
                ByteBufCodecs.stringUtf8(24), Reply::action, Reply::new);
        @Override public Type<Reply> type() { return TYPE; }
    }
    @SubscribeEvent public static void register(RegisterPayloadHandlersEvent event) {
        var r = event.registrar("1");
        r.playToClient(Reply.TYPE, Reply.CODEC);
        r.playToServer(Request.TYPE, Request.CODEC, (packet, context) -> {
            if (context.player() instanceof ServerPlayer player) ReplayServer.action(player, packet.action());
        });
    }
}
