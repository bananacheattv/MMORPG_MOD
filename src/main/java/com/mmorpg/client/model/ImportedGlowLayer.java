package com.mmorpg.client.model;

import com.mmorpg.client.model.imported.ImportedModel;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import java.util.function.Function;

/** Only the authored eyes, runes and cores emit light; armor retains normal world shading. */
public final class ImportedGlowLayer<S extends LivingEntityRenderState> extends RenderLayer<S,ImportedModel> {
    private final Function<S,ImportedModel> models;
    public ImportedGlowLayer(RenderLayerParent<S,ImportedModel> parent,Function<S,ImportedModel> models){super(parent);this.models=models;}
    @Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,S state,float yaw,float pitch){var glow=models.apply(state);collector.order(1).submitModel(glow,state,pose,RenderTypes.eyes(glow.texture),0xF000F0,OverlayTexture.NO_OVERLAY,0xFFFFFFFF,null,state.outlineColor);}
}
