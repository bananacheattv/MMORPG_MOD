package com.mmorpg.client.model.pet;

import com.mmorpg.pet.PetType;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.jspecify.annotations.Nullable;

/** Etat de rendu d'un familier : espece, vitesse lissee, temps d'immobilite, part de vol (0 au sol, 1 en vol), orientation. */
public class PetRenderState extends EntityRenderState {
    public @Nullable PetType type;
    public float speed;
    public float still;
    public float fly;
    public float yRot;
}
