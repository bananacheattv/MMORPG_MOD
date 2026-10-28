package com.mmorpg.client.model.imported;

import com.mmorpg.MMORPG;
import com.mmorpg.entity.NpcEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.resources.Identifier;

/** Appearance never changes the NPC's quest, shop or travel role. Existing four skin IDs remain stable. */
public final class AuthoredNpcRenderer extends MobRenderer<NpcEntity,AuthoredNpcRenderer.State,EntityModel<? super AuthoredNpcRenderer.State>> {
    public static final class State extends HumanoidRenderState { public int skin; }
    private final HumanoidModel<State> legacy;
    private final ImportedModel[] authored=new ImportedModel[NpcEntity.AUTHORED_MODELS.length];
    public AuthoredNpcRenderer(EntityRendererProvider.Context context) {
        super(context,new HumanoidModel<State>(context.bakeLayer(ModelLayers.ZOMBIE)),.5f);
        legacy=(HumanoidModel<State>)model;
        for(int i=0;i<authored.length;i++)authored[i]=new ImportedModel("guides",NpcEntity.AUTHORED_MODELS[i]);
    }
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(NpcEntity entity,State state,float partial){super.extractRenderState(entity,state,partial);state.skin=Math.clamp(entity.skin(),0,NpcEntity.SKINS-1);}
    @Override public Identifier getTextureLocation(State state){return state.skin<4?MMORPG.id("textures/entity/pnj_"+state.skin+".png"):authored[state.skin-4].texture;}
    @Override public void submit(State state,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.SubmitNodeCollector collector,net.minecraft.client.renderer.state.level.CameraRenderState camera){model=state.skin<4?legacy:authored[state.skin-4];super.submit(state,pose,collector,camera);}
}
