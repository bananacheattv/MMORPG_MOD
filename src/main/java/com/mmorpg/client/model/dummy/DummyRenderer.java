package com.mmorpg.client.model.dummy;

import com.mmorpg.MMORPG;
import com.mmorpg.entity.dummy.TrainingDummyEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import org.jspecify.annotations.Nullable;

/**
 * Rendu du mannequin d'entrainement : trois variantes (texture et couche lumineuse propres).
 *
 * <p>L'inclinaison suit un ressort amorti integre a chaque image avec le temps reel (sous-pas fixes de 1/240 s), pas avec
 * le tick serveur : le mouvement reste fluide a toute cadence d'images. Chaque coup recu ajoute une impulsion dans la
 * direction du coup (de l'attaquant vers le mannequin), proportionnelle a sa force ; plusieurs coups rapproches
 * s'additionnent naturellement.</p>
 */
public class DummyRenderer extends EntityRenderer<TrainingDummyEntity, DummyRenderState> {
    private static final int FULL_BRIGHT = 0xF000F0;
    /** Raideur (omega^2, omega = 7,5 rad/s ~ 1,2 Hz) et amortissement (zeta ~ 0,16) du ressort : balancement lourd, qui s'eteint en ~1,5 s. */
    private static final float STIFFNESS = 56F;
    private static final float DAMPING = 2.4F;
    /** Impulsion initiale par degre de force : l'amplitude du premier pic vaut environ la force en degres. */
    private static final float IMPULSE = 9F;
    private static final float STEP = 1F / 240F;

    private record Entry(DummyModel model, Identifier texture, @Nullable Identifier emissive) {
    }

    private final Entry[] entries = new Entry[TrainingDummyEntity.VARIANTS.length];

    public DummyRenderer(EntityRendererProvider.Context c) {
        super(c);
        this.shadowRadius = 0.7F;
        add(c, 0, MannequinStandardModel.LAYER, MannequinStandardModel.animations(), "mannequin_entrainement", false);
        add(c, 1, MannequinBlindeModel.LAYER, MannequinBlindeModel.animations(), "mannequin_entrainement_blinde", false);
        add(c, 2, MannequinMagiqueModel.LAYER, MannequinMagiqueModel.animations(), "mannequin_entrainement_magique", true);
    }

    private void add(EntityRendererProvider.Context c, int i, ModelLayerLocation layer, net.minecraft.client.animation.AnimationDefinition[] anims,
                     String key, boolean emissive) {
        entries[i] = new Entry(new DummyModel(c.bakeLayer(layer), anims), MMORPG.id("textures/entity/dummy/" + key + ".png"),
                emissive ? MMORPG.id("textures/entity/dummy/" + key + "_emissive.png") : null);
    }

    /** Pas de nom flottant : les statistiques sont dans le panneau du HUD. */
    @Override
    protected boolean shouldShowName(TrainingDummyEntity entity, double distanceToCameraSq) {
        return false;
    }

    @Override
    public DummyRenderState createRenderState() {
        return new DummyRenderState();
    }

    @Override
    public void extractRenderState(TrainingDummyEntity e, DummyRenderState state, float partialTicks) {
        super.extractRenderState(e, state, partialTicks);
        state.variant = e.variant();
        state.yRot = Mth.rotLerp(partialTicks, e.yRotO, e.getYRot());

        // nouveaux coups : impulsion dans la direction du coup, exprimee dans le repere du mannequin
        int seq = e.hitSeq();
        if (e.seenHitSeq < 0) e.seenHitSeq = seq;                       // premiere apparition : pas de rejeu des anciens coups
        if (seq != e.seenHitSeq) {
            e.seenHitSeq = seq;
            double push = Math.toRadians(e.hitDir());
            double yaw = Math.toRadians(e.getYRot());
            // composantes avant / droite du vecteur (cos push, sin push) dans le repere du mannequin
            float forward = (float) Math.sin(push - yaw);
            float right = (float) -Math.cos(push - yaw);
            float kick = e.hitForce() * IMPULSE * (float) (Math.PI / 180.0);
            e.velX += forward * kick;
            e.velZ += right * kick;
        }
        int reset = e.resetSeq();
        if (e.seenResetSeq < 0) e.seenResetSeq = reset;
        if (reset != e.seenResetSeq) {
            e.seenResetSeq = reset;
            e.resetStartAge = e.tickCount + partialTicks;
        }

        // ressort amorti avec le temps reel (independant des ticks et du nombre d'images par seconde)
        long now = System.nanoTime();
        float dt = e.lastRenderNanos == 0 ? 0F : Math.min(0.05F, (now - e.lastRenderNanos) / 1.0E9F);
        e.lastRenderNanos = now;
        for (float t = dt; t > 0; t -= STEP) {
            float h = Math.min(STEP, t);
            e.velX += (-STIFFNESS * e.tiltX - DAMPING * e.velX) * h;
            e.velZ += (-STIFFNESS * e.tiltZ - DAMPING * e.velZ) * h;
            e.tiltX += e.velX * h;
            e.tiltZ += e.velZ * h;
        }
        state.tiltX = e.tiltX;
        state.tiltZ = e.tiltZ;
        float sinceReset = (e.tickCount + partialTicks - e.resetStartAge) / 20F;
        state.resetSeconds = sinceReset >= 0 && sinceReset < 5F ? sinceReset : -1F;
    }

    @Override
    public void submit(DummyRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        Entry e = entries[Mth.clamp(state.variant, 0, entries.length - 1)];
        poseStack.pushPose();
        poseStack.rotateDegrees(Axis.YP, 180.0F - state.yRot);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        collector.submitModel(e.model(), state, poseStack, RenderTypes.entityCutout(e.texture()), state.lightCoords,
                OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor);
        if (e.emissive() != null) {
            collector.submitModel(e.model(), state, poseStack, RenderTypes.entityTranslucentEmissive(e.emissive()), FULL_BRIGHT,
                    OverlayTexture.NO_OVERLAY, -1, null, 0);
        }
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
