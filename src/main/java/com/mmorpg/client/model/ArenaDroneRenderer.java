package com.mmorpg.client.model;
import com.mmorpg.client.model.imported.ImportedModel;
import com.mmorpg.competition.ArenaDrone;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

public final class ArenaDroneRenderer extends MobRenderer<ArenaDrone,LivingEntityRenderState,ImportedModel>{
    public ArenaDroneRenderer(EntityRendererProvider.Context context){super(context,new ImportedModel("yggdrasil","arena_drone"),.3f);var glow=new ImportedModel("yggdrasil","arena_drone",true);addLayer(new ImportedGlowLayer<>(this,s->glow));}
    @Override public LivingEntityRenderState createRenderState(){return new LivingEntityRenderState();}
    @Override public Identifier getTextureLocation(LivingEntityRenderState state){return model.texture;}
}
