package com.mmorpg.client.model.leaderboard;

import com.mmorpg.leaderboard.Board;
import com.mmorpg.leaderboard.LeaderboardBlock;
import com.mmorpg.leaderboard.LeaderboardBlockEntity;
import com.mmorpg.leaderboard.Leaderboards;
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

/**
 * Ecrit le classement sur la face du panneau : titre dans le bandeau de couleur, puis dix lignes (rang, nom, valeur).
 * Geometrie du modele (px) : face du tableau a z = 5,5, largeur -12,5..28,5, lignes entre y = 8 et y = 26, bandeau y = 26,5..30,5.
 */
public class LeaderboardRenderer implements BlockEntityRenderer<LeaderboardBlockEntity, LeaderboardRenderer.State> {
    private static final int FULL_BRIGHT = 0xF000F0;
    private static final float PX = 1 / 16f;
    private static final float HALF_WIDTH = 19f * PX;      // moitie de la zone de texte, depuis le centre du bloc
    private static final float TEXT_Z = 3.15f * PX;        // devant la face du tableau, depuis le centre du bloc
    private static final float ROW_SCALE = 0.0125f, TITLE_SCALE = 0.02f;
    private static final int[] RANK_COLORS = {0xFFFFD24A, 0xFFD8DCE6, 0xFFD9915A};

    public static class State extends BlockEntityRenderState {
        public Board board = Board.NIVEAU;
        public float yRot;
        public final List<Leaderboards.Row> rows = new ArrayList<>();
    }

    private final Font font;

    public LeaderboardRenderer(BlockEntityRendererProvider.Context ctx) {
        this.font = ctx.font();
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(LeaderboardBlockEntity be, State state, float partial, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partial, camera, breakProgress);
        state.board = be.board();
        state.yRot = be.getBlockState().getValue(LeaderboardBlock.FACING).toYRot();
        state.rows.clear();
        state.rows.addAll(be.rows);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        pose.pushPose();
        pose.translate(0.5, 0, 0.5);
        pose.rotateDegrees(Axis.YP, -state.yRot);     // le texte se lit depuis la face avant du panneau
        // ---- titre
        Component title = Component.literal(state.board.title);
        pose.pushPose();
        pose.translate(0, (30f - 0.6f) * PX, TEXT_Z + 0.2f * PX);
        pose.scale(TITLE_SCALE, -TITLE_SCALE, TITLE_SCALE);
        collector.submitText(pose, -font.width(title) / 2f, 0, title.getVisualOrderText(), false, Font.DisplayMode.NORMAL,
                FULL_BRIGHT, 0xFFFFFFFF, 0x80000000, 0);
        pose.popPose();
        // ---- lignes
        if (state.rows.isEmpty()) {
            Component none = Component.literal("Aucun classement");
            pose.pushPose();
            pose.translate(0, 17f * PX, TEXT_Z);
            pose.scale(TITLE_SCALE, -TITLE_SCALE, TITLE_SCALE);
            collector.submitText(pose, -font.width(none) / 2f, 0, none.getVisualOrderText(), false, Font.DisplayMode.NORMAL,
                    FULL_BRIGHT, 0xFFB8B0A0, 0, 0);
            pose.popPose();
        }
        float widthPx = 2 * HALF_WIDTH / ROW_SCALE;
        for (int i = 0; i < state.rows.size() && i < Board.ROWS; i++) {
            Leaderboards.Row row = state.rows.get(i);
            pose.pushPose();
            pose.translate(-HALF_WIDTH, (26f - 0.2f - i * 1.8f) * PX, TEXT_Z);
            pose.scale(ROW_SCALE, -ROW_SCALE, ROW_SCALE);
            int rankColor = i < RANK_COLORS.length ? RANK_COLORS[i] : 0xFFC8C0B0;
            Component rank = Component.literal((i + 1) + ".");
            Component name = Component.literal(row.name());
            Component value = Component.literal(state.board.format(row.value()));
            collector.submitText(pose, 0, 0, rank.getVisualOrderText(), false, Font.DisplayMode.NORMAL, FULL_BRIGHT, rankColor, 0, 0);
            collector.submitText(pose, 14, 0, name.getVisualOrderText(), false, Font.DisplayMode.NORMAL, FULL_BRIGHT,
                    i < RANK_COLORS.length ? rankColor : 0xFFE8E2D6, 0, 0);
            collector.submitText(pose, widthPx - font.width(value), 0, value.getVisualOrderText(), false, Font.DisplayMode.NORMAL,
                    FULL_BRIGHT, 0xFF000000 | state.board.color, 0, 0);
            pose.popPose();
        }
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(LeaderboardBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(1.6, 0, 1.6).expandTowards(0, 1, 0);
    }

    @Override
    public int getViewDistance() {
        return 40;
    }
}
