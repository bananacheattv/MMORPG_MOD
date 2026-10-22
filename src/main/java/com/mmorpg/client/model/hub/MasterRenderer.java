package com.mmorpg.client.model.hub;

import com.mmorpg.MMORPG;
import com.mmorpg.entity.hub.ClassMasterEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

/**
 * Rendu des maitres du hub : un modele, une texture et une couche lumineuse par maitre. La tete se tourne doucement vers le
 * joueur local (lissage exponentiel base sur le temps reel, donc fluide a toute cadence d'images).
 */
public class MasterRenderer extends EntityRenderer<ClassMasterEntity, MasterRenderState> {
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final float MAX_YAW = 65F;

    private record Entry(MasterModel model, Identifier texture, Identifier emissive) {
    }

    private final Entry[] entries = new Entry[ClassMasterEntity.IDS.length];

    public MasterRenderer(EntityRendererProvider.Context c) {
        super(c);
        this.shadowRadius = 0.55F;
        add(c, ClassMasterEntity.GUERRIER, MaitreGuerrierModel.LAYER, MaitreGuerrierModel.animations(), "maitre_guerrier");
        add(c, ClassMasterEntity.MAGE, MaitreMageModel.LAYER, MaitreMageModel.animations(), "maitre_mage");
        add(c, ClassMasterEntity.ARCHER, MaitreArcherModel.LAYER, MaitreArcherModel.animations(), "maitre_archer");
        add(c, ClassMasterEntity.TANK, MaitreTankModel.LAYER, MaitreTankModel.animations(), "maitre_tank");
        add(c, ClassMasterEntity.EPREUVE, GardienEpreuveModel.LAYER, GardienEpreuveModel.animations(), "gardien_epreuve");
    }

    private void add(EntityRendererProvider.Context c, int kind, ModelLayerLocation layer, AnimationDefinition[] anims, String key) {
        entries[kind] = new Entry(new MasterModel(c.bakeLayer(layer), anims), MMORPG.id("textures/entity/hub/" + key + ".png"),
                MMORPG.id("textures/entity/hub/" + key + "_emissive.png"));
    }

    /** Le nom du maitre est affiche par le jeu au-dessus de lui (nom personnalise visible), sauf a grande distance. */
    @Override
    protected boolean shouldShowName(ClassMasterEntity entity, double distanceToCameraSq) {
        return distanceToCameraSq < 24 * 24 && super.shouldShowName(entity, distanceToCameraSq);
    }

    @Override
    public MasterRenderState createRenderState() {
        return new MasterRenderState();
    }

    @Override
    public void extractRenderState(ClassMasterEntity e, MasterRenderState state, float partialTicks) {
        super.extractRenderState(e, state, partialTicks);
        state.kind = e.kind();
        state.yRot = Mth.rotLerp(partialTicks, e.yRotO, e.getYRot());
        float since = (e.tickCount + partialTicks - e.presentStart) / 20F;
        state.presentSeconds = since >= 0 && e.tickCount + partialTicks < e.presentUntil ? since : -1F;

        // regard vers le joueur local, lisse
        LocalPlayer player = Minecraft.getInstance().player;
        float targetYaw = 0F;
        float targetPitch = 0F;
        if (player != null && player.distanceToSqr(e) < 12 * 12) {
            double dx = player.getX() - e.getX();
            double dz = player.getZ() - e.getZ();
            double dy = player.getEyeY() - (e.getY() + 1.9);
            float yawToPlayer = (float) Math.toDegrees(Math.atan2(-dx, dz));
            targetYaw = Mth.clamp(Mth.wrapDegrees(yawToPlayer - state.yRot), -MAX_YAW, MAX_YAW);
            targetPitch = Mth.clamp((float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz))), -25F, 25F);
        }
        long now = System.nanoTime();
        float dt = e.lastRenderNanos == 0 ? 0F : Math.min(0.1F, (now - e.lastRenderNanos) / 1.0E9F);
        e.lastRenderNanos = now;
        float k = 1F - (float) Math.exp(-dt * 6F);
        e.headYaw += (targetYaw - e.headYaw) * k;
        e.headPitch += (targetPitch - e.headPitch) * k;
        state.headYaw = e.headYaw;
        state.headPitch = e.headPitch;
    }

    @Override
    public void submit(MasterRenderState state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        Entry e = entries[Mth.clamp(state.kind, 0, entries.length - 1)];
        poseStack.pushPose();
        poseStack.rotateDegrees(Axis.YP, 180.0F - state.yRot);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        collector.submitModel(e.model(), state, poseStack, RenderTypes.entityCutout(e.texture()), state.lightCoords,
                OverlayTexture.NO_OVERLAY, -1, null, state.outlineColor);
        collector.submitModel(e.model(), state, poseStack, RenderTypes.entityTranslucentEmissive(e.emissive()), FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY, -1, null, 0);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }
}
