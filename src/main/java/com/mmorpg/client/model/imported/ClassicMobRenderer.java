package com.mmorpg.client.model.imported;

import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

/** Authored cubic anatomy and materials replace the stock vanilla silhouettes. */
public final class ClassicMobRenderer<T extends Mob> extends MobRenderer<T,ClassicMobRenderer.State,ImportedModel> {
    public static final class State extends LivingEntityRenderState {public float attackTicks=-1,castTicks=-1;public String clip;}
    private final float size;
    public ClassicMobRenderer(EntityRendererProvider.Context context,String id){
        super(context,new ImportedModel("classic",id),id.equals("golem_cristal")?.8f:.45f);
        size=id.equals("gobelin")?.82f:1f;
        var glow=new ImportedModel("classic",id,true);addLayer(new com.mmorpg.client.model.ImportedGlowLayer<>(this,s->glow));
    }
    @Override protected void scale(State state,com.mojang.blaze3d.vertex.PoseStack pose){pose.scale(size,size,size);}
    @Override public State createRenderState(){return new State();}
    @Override public Identifier getTextureLocation(State state){return model.texture;}
    @Override public void extractRenderState(T mob,State state,float partial){
        super.extractRenderState(mob,state,partial);state.attackTicks=-1;state.castTicks=-1;state.clip=null;
        if(mob instanceof com.mmorpg.entity.mob.SmartMonster m&&m.clip>=0){String name=com.mmorpg.entity.mob.ImportedSpells.CLIPS[m.clip];float e=m.tickCount-m.clipStarted+partial;if(model.clipLength(name)<=0)name="attack";if(e<model.clipLength(name)*20+1){state.clip=name;state.attackTicks=e;}}
    }
}
