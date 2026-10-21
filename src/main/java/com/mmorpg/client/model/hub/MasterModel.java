package com.mmorpg.client.model.hub;

import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.animation.KeyframeAnimation;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.rendertype.RenderTypes;

/**
 * Maitre de classe / Gardien de l'epreuve (Blockbench) : "idle" en boucle, "present" par-dessus quand le maitre est mis en scene,
 * et la tete suit le joueur local (angles lisses fournis par le rendu).
 */
public class MasterModel extends EntityModel<MasterRenderState> {
    private static final int IDLE = 0;
    private static final int PRESENT = 1;
    private static final float DEG = (float) (Math.PI / 180.0);
    private final KeyframeAnimation idle;
    private final KeyframeAnimation present;
    private final float presentSeconds;
    private final ModelPart head;

    public MasterModel(ModelPart root, AnimationDefinition[] definitions) {
        super(root, RenderTypes::entityCutout);
        this.idle = definitions[IDLE].bake(root);
        this.present = definitions[PRESENT].bake(root);
        this.presentSeconds = definitions[PRESENT].lengthInSeconds();
        this.head = root.createPartLookup().apply("tete");
    }

    @Override
    public void setupAnim(MasterRenderState s) {
        super.setupAnim(s);
        idle.apply((long) (s.ageInTicks * 50.0F), 1.0F);
        if (s.presentSeconds >= 0.0F && s.presentSeconds < presentSeconds) {
            present.apply((long) (s.presentSeconds * 1000.0F), 1.0F);
        }
        if (head != null) {
            head.yRot += s.headYaw * DEG;
            head.xRot += s.headPitch * DEG;
        }
    }
}
