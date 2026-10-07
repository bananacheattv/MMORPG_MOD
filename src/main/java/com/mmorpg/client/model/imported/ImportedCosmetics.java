package com.mmorpg.client.model.imported;

import com.mmorpg.MMORPG;
import com.mmorpg.client.ClientData;
import com.mmorpg.cosmetic.Cosmetic;
import com.mmorpg.registry.ModAttachments;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.ClientAvatarEntity;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.context.ContextKey;
import net.minecraft.world.entity.Avatar;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.renderstate.*;
import java.util.*;

@EventBusSubscriber(modid=MMORPG.MODID,value=Dist.CLIENT)
public final class ImportedCosmetics extends RenderLayer<AvatarRenderState,PlayerModel> {
    private static final ContextKey<List<Cosmetic>> EQUIPPED=new ContextKey<>(MMORPG.id("model_cosmetics"));
    /** Only set around synchronous inventory render-state extraction; never sent to the server. */
    public static Cosmetic preview;
    private final Map<Cosmetic,ImportedModel> models=new EnumMap<>(Cosmetic.class);
    private ImportedCosmetics(RenderLayerParent<AvatarRenderState,PlayerModel> parent) {
        super(parent);
        for(var c:Cosmetic.values()) if(c.hasModel()) models.put(c,new ImportedModel("cosmetics",c.id));
    }
    @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers event) {
        for(var skin:event.getSkins()) {
            var renderer=event.getPlayerRenderer(skin);
            if(renderer!=null) renderer.addLayer(new ImportedCosmetics(renderer));
        }
    }
    @SubscribeEvent public static void states(RegisterRenderStateModifiersEvent event) {
        event.registerAvatarEntityModifier(new AvatarRenderStateModifier() {
            @Override public <T extends Avatar & ClientAvatarEntity> void accept(T entity,AvatarRenderState state) {
                var pub=entity.getExistingDataOrNull(ModAttachments.PUBLIC);
                boolean self=entity==Minecraft.getInstance().player;
                Collection<String> ids=self?ClientData.DATA.equippedCosmetics.values():pub==null?List.of():pub.cosmetics;
                var cosmetics=new ArrayList<Cosmetic>();
                for(var id:ids) {var c=Cosmetic.byId(id);if(c!=null&&c.hasModel()) cosmetics.add(c);}
                if(self&&preview!=null) {
                    cosmetics.removeIf(c->c.category==preview.category);
                    if(preview.hasModel()) cosmetics.add(preview);
                }
                state.setRenderData(EQUIPPED,List.copyOf(cosmetics));
            }
        });
    }
    @Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,AvatarRenderState state,float yaw,float pitch) {
        if(state.isInvisible||state.isSpectator) return;
        var equipped=state.getRenderData(EQUIPPED);
        if(equipped==null) return;
        for(var c:equipped) {
            var model=models.get(c);if(model==null) continue;
            pose.pushPose();
            if(c.category==Cosmetic.Category.COIFFE||c.category==Cosmetic.Category.VISAGE) {
                getParentModel().head.translateAndRotate(pose);
                if(c.category==Cosmetic.Category.VISAGE) pose.translate(0,-.25,-.29);
                else pose.translate(0,-.5,0);
            } else {
                getParentModel().body.translateAndRotate(pose);
                switch(c) {
                    case DRAGON_EPAULE -> {pose.translate(.53,-.1,-.08);pose.scale(.72f,.72f,.72f);}
                    case EPAULIERES_GIVRE -> {pose.translate(0,-.12,0);pose.scale(1.2f,1.2f,1.2f);}
                    case CAPE_ROYALE -> pose.translate(0,.02,.22);
                    case AILES_CELESTES -> pose.translate(0,.27,.2);
                    default -> pose.translate(0,.38,.33);
                }
            }
            collector.submitModel(model,state,pose,RenderTypes.entityTranslucent(model.texture),light,OverlayTexture.NO_OVERLAY,state.outlineColor);
            pose.popPose();
        }
    }
}
