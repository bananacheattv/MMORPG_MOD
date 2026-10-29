package com.mmorpg.client.model.ranking;

import com.mmorpg.block.ranking.RankingBoardBlock;
import com.mmorpg.block.ranking.RankingBoardBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

/** Écrit le top 10 sur la face du panneau (les titres et le décor sont dans la texture de chaque classement). */
public class RankingBoardRenderer implements BlockEntityRenderer<RankingBoardBlockEntity, RankingBoardRenderer.State> {
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final int[] RANK_COLORS = {0xFFFFD040, 0xFFD8D8E8, 0xFFD08A50};
    private static final float PANEL_FRONT = 0.125f + 0.004f;   // plan avant du panneau (z=6/16) devant le centre du bloc
    private static final float FIRST_ROW_Y = 1.55f, ROW_HEIGHT = 0.125f, SCALE = 0.0135f, HALF_WIDTH = 1.32f;

    public static class State extends BlockEntityRenderState {
        public float yaw;
        public final List<String> lines = new ArrayList<>();
    }

    private final Font font;

    public RankingBoardRenderer(BlockEntityRendererProvider.Context ctx) {
        this.font = ctx.font();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(RankingBoardBlockEntity be, State state, float partial, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partial, camera, breakProgress);
        state.yaw = be.getBlockState().getValue(RankingBoardBlock.FACING).toYRot();
        state.lines.clear();
        state.lines.addAll(be.lines);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5f, 0, 0.5f);
        pose.rotateDegrees(Axis.YP, -state.yaw);   // le texte (normale +z) regarde dans la direction du bloc
        pose.translate(0, 0, PANEL_FRONT);
        if (state.lines.isEmpty()) {
            row(pose, collector, -font.width("Aucune donnée") / 2f * SCALE, FIRST_ROW_Y, "Aucune donnée", 0xFFB9AFCC);
        }
        for (int i = 0; i < state.lines.size() && i < 10; i++) {
            String[] parts = state.lines.get(i).split("\t", 2);
            int color = i < RANK_COLORS.length ? RANK_COLORS[i] : 0xFFFFFFFF;
            float y = FIRST_ROW_Y - i * ROW_HEIGHT;
            row(pose, collector, -HALF_WIDTH, y, (i + 1) + ". " + font.plainSubstrByWidth(parts[0], 150), color);
            if (parts.length > 1) row(pose, collector, HALF_WIDTH - font.width(parts[1]) * SCALE, y, parts[1], color);
        }
        pose.popPose();
    }

    private void row(PoseStack pose, SubmitNodeCollector collector, float x, float y, String text, int color) {
        pose.pushPose();
        pose.translate(x, y, 0);
        pose.scale(SCALE, -SCALE, SCALE);
        collector.submitText(pose, 0, 0, Component.literal(text).getVisualOrderText(), false, Font.DisplayMode.NORMAL, FULL_BRIGHT, color, 0, 0);
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(RankingBoardBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(1.6, 0, 1.6).expandTowards(0, 1.2, 0);
    }

    @Override
    public int getViewDistance() {
        return 48;
    }
}
