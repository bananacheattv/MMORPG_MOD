package com.mmorpg.client.model.dummy;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

/** Etat de rendu d'un mannequin : variante, inclinaison du ressort (radians), temps ecoule depuis la remise a zero, orientation. */
public class DummyRenderState extends EntityRenderState {
    public int variant;
    public float tiltX;
    public float tiltZ;
    /** Secondes depuis la remise a zero des statistiques (negatif : pas d'animation en cours). */
    public float resetSeconds = -1.0F;
    public float yRot;
}
