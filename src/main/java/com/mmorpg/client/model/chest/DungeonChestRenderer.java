package com.mmorpg.client.model.chest;

import com.mmorpg.MMORPG;
import com.mmorpg.block.chest.DungeonChestBlockEntity;
import com.mmorpg.client.model.imported.ChestAnimState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class DungeonChestRenderer implements BlockEntityRenderer<DungeonChestBlockEntity, DungeonChestRenderer.State> {
    private static final Identifier TEXTURE = MMORPG.id("textures/imported/chests/coffre_sombre.png");
    private static final float SCALE = 0.5f;
    private final ChestModels models = new ChestModels("coffre_sombre");

    public static class State extends BlockEntityRenderState {
        public Direction facing = Direction.NORTH;
        public final ChestAnimState anim = new ChestAnimState();
    }

    public DungeonChestRenderer(BlockEntityRendererProvider.Context ctx) {
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(DungeonChestBlockEntity be, State state, float partial, Vec3 camera, ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(be, state, partial, camera, breakProgress);
        state.facing = be.getBlockState().getValue(ChestBlock.FACING);
        long now = be.getLevel() == null ? 0 : be.getLevel().getGameTime();
        state.anim.ticks = ChestModels.ticks(be.open, now - be.openedAt + partial, now - be.closedAt + partial);
    }

    @Override
    public void submit(State state, PoseStack pose, SubmitNodeCollector collector, CameraRenderState camera) {
        models.submit(state.anim, pose, collector, TEXTURE, state.lightCoords, ChestModels.yaw(state.facing), SCALE);
    }

    @Override
    public AABB getRenderBoundingBox(DungeonChestBlockEntity be) {
        return new AABB(be.getBlockPos()).inflate(1, 0, 1).expandTowards(0, 1.5, 0);
    }
}
