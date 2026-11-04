package com.mmorpg.client.model.imported;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

/** Etat d'animation d'un coffre (modele importe) : {@code ticks} = temps dans l'animation « chest » (20 par seconde), negatif = repos. */
public final class ChestAnimState extends LivingEntityRenderState {
    public float ticks = -1;
}
