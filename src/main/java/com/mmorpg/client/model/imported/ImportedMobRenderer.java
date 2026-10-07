package com.mmorpg.client.model.imported;

import com.mmorpg.entity.mob.ImportedMobEntity;
import com.mmorpg.entity.mob.ImportedMobs;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public final class ImportedMobRenderer extends MobRenderer<ImportedMobEntity, ImportedMobRenderer.State, ImportedModel> {
    public static class State extends LivingEntityRenderState { public float attackTicks=-1; }
    /** Echelle du modele (la boite de collision de l'entite suit la meme echelle). */
    private final float modelScale;
    /** Hauteur de lévitation (0 pour les monstres qui marchent). */
    private final float hover;

    public ImportedMobRenderer(EntityRendererProvider.Context ctx,String key) {
        super(ctx,new ImportedModel("mobs",key),.5f);
        float s=1,h=0;
        for(ImportedMobs.Entry e:ImportedMobs.ALL) if(e.key().equals(key)) { s=e.scale(); h=e.hover(); }
        this.modelScale=s;
        this.hover=h;
        this.shadowRadius=.5f*s;
    }
    @Override public State createRenderState() { return new State(); }
    @Override public Identifier getTextureLocation(State state) { return model.texture; }
    @Override protected void scale(State state, PoseStack poseStack) {
        // l'axe Y est inverse a ce stade du rendu : une translation negative fait monter le modele
        if(hover>0) poseStack.translate(0,-(hover+.12f*(float)Math.sin(state.ageInTicks*.08f)),0);
        poseStack.scale(modelScale,modelScale,modelScale);
    }
    @Override public void extractRenderState(ImportedMobEntity entity,State state,float partial) {
        super.extractRenderState(entity,state,partial);
        float elapsed=entity.tickCount-entity.attackStarted+partial;
        state.attackTicks=elapsed<24?elapsed:-1;
    }
}
