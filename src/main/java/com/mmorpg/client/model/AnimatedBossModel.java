package com.mmorpg.client.model;

import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import org.jspecify.annotations.Nullable;

/**
 * Modele de boss issu de Blockbench. Les boucles (repos, marche) sont pilotees par le temps et le deplacement ;
 * les animations ponctuelles (attaques, cris) par les AnimationState de l'entite et la mort par deathTime.
 * Toutes s'additionnent a la pose de repos, comme dans Blockbench.
 */
public class AnimatedBossModel extends EntityModel<AnimatedBossRenderState> {
    /**
     * Indices des animations jouant un role particulier (-1 si absente), os de la tete (suit le regard),
     * vitesse de lecture de la marche (pour que les pieds ne glissent pas) et gain d'amplitude selon la vitesse.
     */
    public record Profile(int idle, int walk, int idleAlt, int walkAlt, int death, @Nullable String head, float walkSpeed, float walkAmp) {
    }

    private static final float DEG = (float) (Math.PI / 180.0);
    private final KeyframeAnimation[] anims;
    private final Profile profile;
    private final @Nullable ModelPart head;

    public AnimatedBossModel(ModelPart root, AnimationDefinition[] definitions, Profile profile) {
        super(root, RenderTypes::entityCutout);
        this.profile = profile;
        this.anims = new KeyframeAnimation[definitions.length];
        for (int i = 0; i < definitions.length; i++) {
            this.anims[i] = definitions[i].bake(root);
        }
        this.head = profile.head() == null ? null : root.createPartLookup().apply(profile.head());
    }

    private boolean isLoop(int i) {
        return i == profile.idle() || i == profile.walk() || i == profile.idleAlt() || i == profile.walkAlt() || i == profile.death();
    }

    @Override
    public void setupAnim(AnimatedBossRenderState state) {
        super.setupAnim(state);
        if (state.deathTime > 0.0F && profile.death() >= 0) {
            anims[profile.death()].apply((long) (state.deathTime * 50.0F), 1.0F);
            return;
        }
        boolean busy = false;
        for (int i = 0; i < anims.length; i++) {
            if (!isLoop(i) && state.anims[i].isStarted()) {
                busy = true;
                break;
            }
        }
        float damp = busy ? 0.35F : 1.0F;
        float walk = Math.min(1.0F, state.walkAnimationSpeed * profile.walkAmp());
        int idle = state.altMode && profile.idleAlt() >= 0 ? profile.idleAlt() : profile.idle();
        int walkAnim = state.altMode && profile.walkAlt() >= 0 ? profile.walkAlt() : profile.walk();
        if (idle >= 0) {
            anims[idle].apply((long) (state.ageInTicks * 50.0F), (1.0F - walk) * damp);
        }
        if (walkAnim >= 0 && walk > 0.01F) {
            anims[walkAnim].apply((long) (state.walkAnimationPos * 50.0F * profile.walkSpeed()), walk * damp);
        }
        if (head != null && !busy) {
            head.yRot += state.yRot * DEG * 0.6F;
            head.xRot += state.xRot * DEG * 0.5F;
        }
        for (int i = 0; i < anims.length; i++) {
            if (!isLoop(i)) {
                anims[i].apply(state.anims[i], state.ageInTicks);
            }
        }
    }
}
