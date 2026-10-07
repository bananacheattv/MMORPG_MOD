package com.mmorpg.client.model.imported;

import com.mmorpg.entity.mob.ImportedMobEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public final class ImportedMobRenderer extends MobRenderer<ImportedMobEntity, ImportedMobRenderer.State, ImportedModel> {
    public static class State extends LivingEntityRenderState { public float attackTicks=-1; }
    public ImportedMobRenderer(EntityRendererProvider.Context ctx,String key) { super(ctx,new ImportedModel("mobs",key),.5f); }
    @Override public State createRenderState() { return new State(); }
    @Override public Identifier getTextureLocation(State state) { return model.texture; }
    @Override public void extractRenderState(ImportedMobEntity entity,State state,float partial) {
        super.extractRenderState(entity,state,partial);
        float elapsed=entity.tickCount-entity.attackStarted+partial;
        state.attackTicks=elapsed<24?elapsed:-1;
    }
}
