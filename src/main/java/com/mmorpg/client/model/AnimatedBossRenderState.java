package com.mmorpg.client.model;

import com.mmorpg.entity.boss.RpgBoss;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.AnimationState;

/** Etat de rendu d'un boss anime : copie des animations ponctuelles de l'entite et de son mode (furie...). */
public class AnimatedBossRenderState extends LivingEntityRenderState {
    public final AnimationState[] anims = new AnimationState[RpgBoss.MAX_ANIMS];
    public boolean altMode;

    public AnimatedBossRenderState() {
        for (int i = 0; i < anims.length; i++) {
            anims[i] = new AnimationState();
        }
    }
}
