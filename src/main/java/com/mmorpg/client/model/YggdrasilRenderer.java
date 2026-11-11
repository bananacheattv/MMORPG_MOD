package com.mmorpg.client.model;

import com.mmorpg.competition.GuildGuardian;
import com.mmorpg.guild.Yggdrasil;
import com.mmorpg.client.model.imported.ImportedModel;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public final class YggdrasilRenderer extends MobRenderer<GuildGuardian,YggdrasilRenderer.State,ImportedModel> {
    public static final class State extends LivingEntityRenderState{public float attackTicks=-1;public String clip;public int phase=1;}
    public YggdrasilRenderer(EntityRendererProvider.Context context,int tier){super(context,new ImportedModel("yggdrasil",Yggdrasil.ALL[tier].id()),2.5f);var glow=new ImportedModel("yggdrasil",Yggdrasil.ALL[tier].id(),true);addLayer(new ImportedGlowLayer<>(this,s->glow));}
    @Override public State createRenderState(){return new State();}
    @Override public Identifier getTextureLocation(State state){return model.texture;}
    @Override public void extractRenderState(GuildGuardian entity,State state,float partial){super.extractRenderState(entity,state,partial);state.phase=entity.combatPhase();var data=entity.getExistingDataOrNull(com.mmorpg.registry.ModAttachments.MOB);if(data!=null)state.phase=Math.max(1,data.phase);state.attackTicks=-1;state.clip=null;
        if(entity.clip>=0){String name=com.mmorpg.entity.mob.ImportedSpells.CLIPS[entity.clip];if(model.clipLength(name)<=0)name="attack";float e=entity.tickCount-entity.clipStarted+partial;if(e<model.clipLength(name)*20+1){state.clip=name;state.attackTicks=e;}}}
}
