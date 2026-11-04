package com.mmorpg.client.model.decor;

import com.mmorpg.MMORPG;
import com.mmorpg.block.DecorModelBlock;
import com.mmorpg.client.model.imported.ChestAnimState;
import com.mmorpg.client.model.imported.ImportedModel;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/** Rendu des decors animes (drapeau, camp) : l'animation « chest » du modele sert de boucle continue. */
public class DecorModelRenderer implements BlockEntityRenderer<DecorModelBlock.Entity, DecorModelRenderer.State> {
    private final Map<String, ImportedModel> models = new HashMap<>();

    public static class State extends BlockEntityRenderState {
        public String key = "";
        public Direction facing = Direction.NORTH;
        public final ChestAnimState anim = new ChestAnimState();
    }

    public DecorModelRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(DecorModelBlock.Entity be, State state, float partial, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partial, camera, breakProgress);
        state.key = BuiltInRegistries.BLOCK.getKey(be.getBlockState().getBlock()).getPath();
        state.facing = be.getBlockState().getValue(DecorModelBlock.FACING);
        long now = be.getLevel() == null ? 0 : be.getLevel().getGameTime();
        state.anim.ticks = (now % 1_000_000L) + partial;
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        String modelKey = state.key.equals("drapeau_aeloria") ? "drapeau" : "camp";
        ImportedModel model = models.computeIfAbsent(modelKey, k -> new ImportedModel("props", k));
        float yaw = switch (state.facing) {
            case EAST -> -90f;
            case SOUTH -> 180f;
            case WEST -> 90f;
            default -> 0f;
        };
        pose.pushPose();
        pose.translate(0.5f, 0f, 0.5f);
        pose.rotateDegrees(Axis.YP, yaw);
        pose.translate(0f, 1.5f, 0f);
        pose.scale(-1f, -1f, 1f);
        Identifier texture = model.texture;
        collector.submitModel(model, state.anim, pose, RenderTypes.entityTranslucent(texture), state.lightCoords, OverlayTexture.NO_OVERLAY, -1, null, 0);
        pose.popPose();
    }

    @Override
    public AABB getRenderBoundingBox(DecorModelBlock.Entity be) {
        return new AABB(be.getBlockPos()).inflate(4, 1, 4).expandTowards(0, 2, 0);
    }
}
