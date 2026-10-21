package com.mmorpg.client.model.dummy;

import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * Mannequin d'entrainement (Blockbench). L'animation "idle" tourne en boucle, "reset" se joue une fois quand les statistiques
 * sont remises a zero, et l'inclinaison des coups recus (direction et force) est ajoutee au pivot du mannequin : elle vient du
 * ressort amorti simule dans {@link DummyRenderer} (image par image, donc parfaitement fluide).
 */
public class DummyModel extends EntityModel<DummyRenderState> {
    private static final int IDLE = 0;
    private static final int RESET = 2;
    private final KeyframeAnimation idle;
    private final KeyframeAnimation reset;
    private final float resetSeconds;
    private final ModelPart pivot;

    public DummyModel(ModelPart root, AnimationDefinition[] definitions) {
        super(root, RenderTypes::entityCutout);
        this.idle = definitions[IDLE].bake(root);
        this.reset = definitions[RESET].bake(root);
        this.resetSeconds = definitions[RESET].lengthInSeconds();
        this.pivot = root.createPartLookup().apply("mannequin");
    }

    @Override
    public void setupAnim(DummyRenderState state) {
        super.setupAnim(state);
        idle.apply((long) (state.ageInTicks * 50.0F), 1.0F);
        if (state.resetSeconds >= 0.0F && state.resetSeconds < resetSeconds) {
            reset.apply((long) (state.resetSeconds * 1000.0F), 1.0F);
        }
        if (pivot != null) {
            pivot.xRot += state.tiltX;
            pivot.zRot += state.tiltZ;
        }
    }
}
