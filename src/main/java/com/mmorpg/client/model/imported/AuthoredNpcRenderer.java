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
    @Override public void extractRenderState(NpcEntity entity,State state,float partial){super.extractRenderState(entity,state,partial);state.skin=Math.clamp(entity.skin(),0,NpcEntity.SKINS-1);faceViewer(entity,state);}
    private static final java.util.Map<Integer,float[]> FACING=new java.util.HashMap<>();
    /** Le PNJ se tourne (corps puis tete) vers le joueur local proche : calcul purement client, donc propre a chaque joueur. */
    private static void faceViewer(NpcEntity e,State state){
        var me=net.minecraft.client.Minecraft.getInstance().player;if(me==null)return;
        float dt=Math.min(0.1F,net.minecraft.client.Minecraft.getInstance().getDeltaTracker().getRealtimeDeltaTicks()/20F);float[] f=FACING.computeIfAbsent(e.getId(),k->new float[]{state.bodyRot,0,0});
        double dx=me.getX()-e.getX(),dz=me.getZ()-e.getZ(),dy=me.getEyeY()-e.getEyeY();boolean near=dx*dx+dz*dz<100&&Math.abs(dy)<6&&!me.isSpectator();
        float baseBody=state.bodyRot;float targetBody=near?(float)(Math.toDegrees(Math.atan2(-dx,dz))):baseBody;
        float k=1F-(float)Math.exp(-dt*7F);f[0]+=net.minecraft.util.Mth.wrapDegrees(targetBody-f[0])*k;
        float headTarget=near?net.minecraft.util.Mth.clamp(net.minecraft.util.Mth.wrapDegrees(targetBody-f[0]),-70F,70F):0F;f[1]+=(headTarget-f[1])*k;
        float pitch=near?(float)(-Math.toDegrees(Math.atan2(dy,Math.sqrt(dx*dx+dz*dz)))):0F;f[2]+=(net.minecraft.util.Mth.clamp(pitch,-35F,35F)-f[2])*k;
        state.bodyRot=f[0];state.yRot=f[1];state.xRot=f[2];
        if(FACING.size()>256)FACING.clear();
    }
    @Override public Identifier getTextureLocation(State state){return state.skin<4?MMORPG.id("textures/entity/pnj_"+state.skin+".png"):authored[state.skin-4].texture;}
    @Override public void submit(State state,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.SubmitNodeCollector collector,net.minecraft.client.renderer.state.level.CameraRenderState camera){model=state.skin<4?legacy:authored[state.skin-4];super.submit(state,pose,collector,camera);}
}
