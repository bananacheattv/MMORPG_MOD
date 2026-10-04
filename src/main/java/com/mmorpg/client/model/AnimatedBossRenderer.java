package com.mmorpg.client.model;

import com.mmorpg.MMORPG;
import com.mmorpg.entity.boss.RpgBoss;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.LivingEntityEmissiveLayer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Rendu d'un boss Blockbench : texture art/boss_blockbench/<cle>/<cle>.png (textures/entity/boss/) et,
 * si elle existe, couche lumineuse <cle>_emissive.png toujours eclairee (yeux, runes, lave, noyau...).
 */
public class AnimatedBossRenderer<T extends RpgBoss> extends MobRenderer<T, AnimatedBossRenderState, AnimatedBossModel> {
    private final Identifier texture;

    public AnimatedBossRenderer(EntityRendererProvider.Context c, ModelLayerLocation layer, AnimationDefinition[] animations,
                                AnimatedBossModel.Profile profile, String key, float shadow, boolean emissive) {
        super(c, new AnimatedBossModel(c.bakeLayer(layer), animations, profile), shadow);
        this.texture = MMORPG.id("textures/entity/boss/" + key + ".png");
        if (emissive) {
            Identifier glow = MMORPG.id("textures/entity/boss/" + key + "_emissive.png");
            this.addLayer(new LivingEntityEmissiveLayer<>(this, state -> glow, (state, age) -> 1.0F, this.getModel(),
                    RenderTypes::entityTranslucentEmissive, false));
        }
    }

    @Override
    public AnimatedBossRenderState createRenderState() {
        return new AnimatedBossRenderState();
    }

    @Override
    public void extractRenderState(T entity, AnimatedBossRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        for (int i = 0; i < RpgBoss.MAX_ANIMS; i++) {
            state.anims[i].copyFrom(entity.animStates[i]);
        }
        state.altMode = entity.altMode();
        state.hasRedOverlay = entity.hurtTime > 0;        // pas de teinte rouge pendant toute l'animation de mort
    }

    /** La mort est animee : pas de bascule du modele sur le cote. */
    @Override
    protected float getFlipDegrees() {
        return 0.0F;
    }

    @Override
    public Identifier getTextureLocation(AnimatedBossRenderState state) {
        return texture;
    }
}
