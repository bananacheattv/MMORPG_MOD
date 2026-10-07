package com.mmorpg.client.model.mount;

import com.mmorpg.client.model.imported.ImportedModel;
import com.mmorpg.entity.MountEntity;
import com.mmorpg.mount.MountType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Rendu des montures 3D : modele voxel anime (idle / marche, vol pour l'Hippogriffe). */
public class MountRenderer extends MobRenderer<MountEntity, MountRenderer.State, ImportedModel> {
    public static class State extends LivingEntityRenderState {
        public boolean flying;
    }

    public MountRenderer(EntityRendererProvider.Context ctx, MountType type) {
        super(ctx, new ImportedModel("mounts", type.id), type.width * .55f * com.mmorpg.registry.ModEntities.MOUNT_SCALE);
    }

    @Override
    protected void scale(State state, com.mojang.blaze3d.vertex.PoseStack pose) {
        float s = com.mmorpg.registry.ModEntities.MOUNT_SCALE;
        pose.scale(s, s, s);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return model.texture;
    }

    @Override
    public void extractRenderState(MountEntity entity, State state, float partial) {
        super.extractRenderState(entity, state, partial);
        state.flying = entity.isFlying();
    }
}
