package com.mmorpg.client.model.pet;

import com.mmorpg.MMORPG;
import com.mmorpg.entity.MascotEntity;
import com.mmorpg.pet.anim.DragonnetNeantAnims;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/** La Mascotte du tutoriel : le Dragonnet du Neant, un peu plus grand qu'en familier, avec une bulle de dialogue au-dessus de la tete. */
public class MascotRenderer extends EntityRenderer<MascotEntity, MascotRenderer.State> {
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final float SCALE = 1.1F;
    private static final long SPEECH_MS = 11_000;
    private static final int WRAP = 30;
    private static String speech = "";
    private static long speechUntil;

    public static class State extends PetRenderState {
        public List<String> bubble = List.of();
    }

    private final PetModel model;
    private final Identifier texture = MMORPG.id("textures/entity/pet/dragonnet_neant.png");
    private final Identifier emissive = MMORPG.id("textures/entity/pet/dragonnet_neant_emissive.png");
    private final boolean glow;

    public MascotRenderer(EntityRendererProvider.Context c) {
        super(c);
        this.shadowRadius = 0.5F;
        this.model = new PetModel(c.bakeLayer(DragonnetNeantModel.LAYER), DragonnetNeantModel.animations(),
                new PetModel.Profile(DragonnetNeantAnims.REPOS, DragonnetNeantAnims.MARCHE, -1, -1, -1, DragonnetNeantAnims.VOL,
                        DragonnetNeantAnims.VOL, DragonnetNeantAnims.JOIE, 0.1F, 0.0F, false, DragonnetNeantAnims.LENGTH_TICKS[DragonnetNeantAnims.JOIE]));
        this.glow = Minecraft.getInstance().getResourceManager().getResource(emissive).isPresent();
    }

    /** Replique recue du serveur : affichee dans la bulle pendant quelques secondes. */
    public static void say(String text) {
        speech = text;
        speechUntil = System.currentTimeMillis() + SPEECH_MS;
    }

    private static List<String> wrap(String text) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            if (line.length() > 0 && line.length() + 1 + word.length() > WRAP) {
                lines.add(line.toString());
                line.setLength(0);
            }
            if (line.length() > 0) line.append(' ');
            line.append(word);
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(MascotEntity entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.type = com.mmorpg.pet.PetType.DRAGONNET;
        state.speed = 0.0F;
        state.still = 0.0F;
        state.fly = 1.0F;
        state.yRot = Mth.rotLerp(partialTicks, entity.yBodyRotO, entity.yBodyRot);
        state.bubble = System.currentTimeMillis() < speechUntil ? wrap(speech) : List.of();
    }

    @Override
    public void submit(State state, PoseStack poseStack, SubmitNodeCollector collector, CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.0F, 0.25F * Mth.sin(state.ageInTicks * 0.1F) + 0.25F, 0.0F);
        poseStack.pushPose();
        poseStack.rotateDegrees(Axis.YP, -state.yRot);
        poseStack.scale(SCALE, SCALE, SCALE);
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        poseStack.translate(0.0F, -1.501F, 0.0F);
        collector.submitModel(model, state, poseStack, RenderTypes.entityTranslucent(texture), state.lightCoords, OverlayTexture.NO_OVERLAY, -1, null,
                state.outlineColor);
        if (glow) {
            collector.submitModel(model, state, poseStack, RenderTypes.entityTranslucentEmissive(emissive), FULL_BRIGHT, OverlayTexture.NO_OVERLAY, -1, null, 0);
        }
        poseStack.popPose();
        poseStack.popPose();
        if (!state.bubble.isEmpty()) bubble(state.bubble, poseStack, collector, camera);
        super.submit(state, poseStack, collector, camera);
    }

    /** Bulle de dialogue : lignes centrees au-dessus de la Mascotte, face a la camera. */
    private void bubble(List<String> lines, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        Font font = Minecraft.getInstance().font;
        float top = 3.0F + 0.115F * lines.size();
        for (int i = 0; i < lines.size(); i++) {
            pose.pushPose();
            pose.translate(0.0F, top - 0.23F * i, 0.0F);
            pose.rotate(camera.orientation);
            pose.scale(0.025F, -0.025F, 0.025F);
            Component line = Component.literal(" " + lines.get(i) + " ");
            collector.submitText(pose, -font.width(line) / 2.0F, 0, line.getVisualOrderText(), false, Font.DisplayMode.NORMAL, FULL_BRIGHT, 0xFF20103A,
                    0xE8FFF4D8, 0);
            pose.popPose();
        }
    }
}
