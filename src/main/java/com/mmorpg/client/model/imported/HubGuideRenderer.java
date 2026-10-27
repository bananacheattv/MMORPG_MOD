package com.mmorpg.client.model.imported;

import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import com.mmorpg.entity.hub.HubGuideEntity;

public final class HubGuideRenderer extends MobRenderer<HubGuideEntity,HubGuideRenderer.State,ImportedModel> {
    public static final class State extends LivingEntityRenderState {public int guide;}
    private final ImportedModel[] models=new ImportedModel[5],glows=new ImportedModel[5];
    public HubGuideRenderer(EntityRendererProvider.Context c){super(c,new ImportedModel("guides","selene"),.4f);for(int i=0;i<5;i++){models[i]=new ImportedModel("guides",com.mmorpg.hub.HubGuides.MODELS[i]);glows[i]=new ImportedModel("guides",com.mmorpg.hub.HubGuides.MODELS[i],true);}addLayer(new com.mmorpg.client.model.ImportedGlowLayer<>(this,s->glows[s.guide]));}
    @Override public void submit(State s,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.SubmitNodeCollector collector,net.minecraft.client.renderer.state.level.CameraRenderState camera){model=models[s.guide];super.submit(s,pose,collector,camera);}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(HubGuideEntity e,State s,float partial){super.extractRenderState(e,s,partial);s.guide=e.guide();model=models[s.guide];}
    @Override public Identifier getTextureLocation(State s){return models[s.guide].texture;}
}
