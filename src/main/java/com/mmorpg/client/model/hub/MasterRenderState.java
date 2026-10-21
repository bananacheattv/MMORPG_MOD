package com.mmorpg.client.model.hub;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** Etat de rendu d'un maitre : variante, orientation, regard vers le joueur et temps de mise en scene. */
public class MasterRenderState extends EntityRenderState {
    public int kind;
    public float yRot;
    public float headYaw;
    public float headPitch;
    /** Secondes depuis le debut de la mise en scene (negatif : pas de mise en scene). */
    public float presentSeconds = -1.0F;
}
