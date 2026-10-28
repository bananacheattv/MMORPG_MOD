package com.mmorpg.client.model;

import com.mmorpg.competition.GuildGuardian;
import com.mmorpg.guild.Yggdrasil;
import com.mmorpg.client.model.imported.ImportedModel;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public final class YggdrasilRenderer extends MobRenderer<GuildGuardian,YggdrasilRenderer.State,ImportedModel> {
    public static final class State extends LivingEntityRenderState{public float attackTicks=-1,castTicks=-1;public int phase=1;}
    public YggdrasilRenderer(EntityRendererProvider.Context context,int tier){super(context,new ImportedModel("yggdrasil",Yggdrasil.ALL[tier].id()),2.5f);var glow=new ImportedModel("yggdrasil",Yggdrasil.ALL[tier].id(),true);addLayer(new ImportedGlowLayer<>(this,s->glow));}
    @Override public State createRenderState(){return new State();}
    @Override public Identifier getTextureLocation(State state){return model.texture;}
    @Override public void extractRenderState(GuildGuardian entity,State state,float partial){super.extractRenderState(entity,state,partial);float attack=entity.tickCount-entity.attackStarted+partial,cast=entity.tickCount-entity.castStarted+partial;state.phase=entity.combatPhase();var data=entity.getExistingDataOrNull(com.mmorpg.registry.ModAttachments.MOB);if(data!=null)state.phase=Math.max(1,data.phase);state.attackTicks=attack<20?attack:-1;state.castTicks=cast<48?cast:-1;}
}
