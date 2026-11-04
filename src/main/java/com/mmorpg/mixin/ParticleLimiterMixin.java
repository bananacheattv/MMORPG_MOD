package com.mmorpg.mixin;

import com.mmorpg.client.PerfClient;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleEngine;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Limiteur de particules adaptatif : ecarte une partie des nouvelles particules quand les FPS sont sous la cible. */
@Mixin(ParticleEngine.class)
public abstract class ParticleLimiterMixin {
    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void aeloria$limit(Particle particle, CallbackInfo ci) {
        if (!PerfClient.allowParticle()) ci.cancel();
    }
}
