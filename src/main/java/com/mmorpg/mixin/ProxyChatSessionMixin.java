package com.mmorpg.mixin;

import com.mmorpg.network.NetworkConfig;
import net.minecraft.network.protocol.game.ServerboundChatSessionUpdatePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Velocity authenticates accounts; these local backends deliberately retain offline UUIDs. */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class ProxyChatSessionMixin {
    @Shadow public ServerPlayer player;

    @Inject(method = "handleChatSessionUpdate", at = @At("HEAD"), cancellable = true)
    private void mmorpg$localProxyChat(ServerboundChatSessionUpdatePacket packet, CallbackInfo ci) {
        var server = player.level().getServer();
        // A signed chat key belongs to the official UUID, never to our backend's offline UUID.
        // Keep the normal unsigned decoder, only on explicitly configured loopback backends.
        // Public/standalone/authenticated servers retain Minecraft's complete validation.
        if (NetworkConfig.current.enabled && server.isDedicatedServer()
                && !server.usesAuthentication() && !server.enforceSecureProfile()
                && ("127.0.0.1".equals(server.getLocalIp()) || "::1".equals(server.getLocalIp()))) {
            ci.cancel();
        }
    }
}
