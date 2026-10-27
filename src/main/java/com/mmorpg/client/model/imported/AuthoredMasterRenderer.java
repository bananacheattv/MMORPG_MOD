package com.mmorpg.client.model.imported;

import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import com.mmorpg.entity.hub.ClassMasterEntity;

/** Keeps the existing trial/selection/cinematic entity and replaces only its visual rig. */
public final class AuthoredMasterRenderer extends MobRenderer<ClassMasterEntity,AuthoredMasterRenderer.State,ImportedModel> {
    public static final class State extends LivingEntityRenderState {public int kind;public float presenting=-1;}
    private final ImportedModel[] models=new ImportedModel[5],glows=new ImportedModel[5];
    public AuthoredMasterRenderer(EntityRendererProvider.Context c){super(c,new ImportedModel("guides","master_guerrier"),.45f);String[] ids={"guerrier","mage","archer","paladin","epreuve"};for(int i=0;i<5;i++){models[i]=new ImportedModel("guides","master_"+ids[i]);glows[i]=new ImportedModel("guides","master_"+ids[i],true);}addLayer(new com.mmorpg.client.model.ImportedGlowLayer<>(this,s->glows[s.kind]));}
    @Override public State createRenderState(){return new State();}
    @Override public void extractRenderState(ClassMasterEntity e,State s,float partial){super.extractRenderState(e,s,partial);s.kind=e.kind();s.presenting=e.tickCount+partial<e.presentUntil?(e.tickCount+partial-e.presentStart):-1;model=models[s.kind];
        var player=net.minecraft.client.Minecraft.getInstance().player;if(player!=null&&player.distanceToSqr(e)<144){double dx=player.getX()-e.getX(),dz=player.getZ()-e.getZ();float target=net.minecraft.util.Mth.clamp(net.minecraft.util.Mth.wrapDegrees((float)Math.toDegrees(Math.atan2(-dx,dz))-e.getYRot()),-65,65);e.headYaw+=(target-e.headYaw)*.12f;s.yRot=e.headYaw;s.xRot=net.minecraft.util.Mth.clamp((float)-Math.toDegrees(Math.atan2(player.getEyeY()-e.getEyeY(),Math.hypot(dx,dz))),-25,25);}
    }
    @Override public void submit(State s,com.mojang.blaze3d.vertex.PoseStack pose,net.minecraft.client.renderer.SubmitNodeCollector collector,net.minecraft.client.renderer.state.level.CameraRenderState camera){model=models[s.kind];super.submit(s,pose,collector,camera);}
    @Override public Identifier getTextureLocation(State s){return models[s.kind].texture;}
}
