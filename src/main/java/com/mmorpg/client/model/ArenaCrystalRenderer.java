package com.mmorpg.client.model;

import com.mmorpg.competition.ArenaCrystal;
import com.mmorpg.client.model.imported.ImportedModel;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public final class ArenaCrystalRenderer extends MobRenderer<ArenaCrystal,ArenaCrystalRenderer.State,ImportedModel> {
    public static final class State extends LivingEntityRenderState{public int team=-1;}
    private final ImportedModel[] models={new ImportedModel("yggdrasil","crystal_neutral"),new ImportedModel("yggdrasil","crystal_red"),new ImportedModel("yggdrasil","crystal_blue")};
    private final ImportedModel[] glow={new ImportedModel("yggdrasil","crystal_neutral",true),new ImportedModel("yggdrasil","crystal_red",true),new ImportedModel("yggdrasil","crystal_blue",true)};
    public ArenaCrystalRenderer(EntityRendererProvider.Context context){super(context,new ImportedModel("yggdrasil","crystal_neutral"),.8f);addLayer(new ImportedGlowLayer<>(this,s->glow[s.team+1]));}
    @Override public State createRenderState(){return new State();}
    @Override public Identifier getTextureLocation(State state){return models[state.team+1].texture;}
    @Override public void extractRenderState(ArenaCrystal entity,State state,float partial){super.extractRenderState(entity,state,partial);state.team=entity.team();}
    @Override public void submit(State state,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.SubmitNodeCollector collector,net.minecraft.client.renderer.state.level.CameraRenderState camera){model=models[state.team+1];super.submit(state,pose,collector,camera);}
}
