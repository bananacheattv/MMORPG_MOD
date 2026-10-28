package com.mmorpg.client.model;

import com.mmorpg.client.model.imported.ImportedModel;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.ArmorStandRenderState;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.model.object.armorstand.ArmorStandArmorModel;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.rendertype.*;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.component.ResolvableProfile;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
@net.neoforged.fml.common.EventBusSubscriber(modid=com.mmorpg.MMORPG.MODID,value=net.neoforged.api.distmarker.Dist.CLIENT)
public final class StyledArmorStandRenderer extends ArmorStandRenderer {
 public static final class State extends ArmorStandRenderState {public String appearance="";}
 public StyledArmorStandRenderer(EntityRendererProvider.Context context){super(context);addLayer(new AppearanceLayer(this,context));}
 @Override public State createRenderState(){return new State();}
 @Override public void extractRenderState(ArmorStand e,ArmorStandRenderState s,float p){super.extractRenderState(e,s,p);((State)s).appearance=e.getData(com.mmorpg.registry.ModAttachments.STAND_LOOK);}
 @Override protected RenderType getRenderType(ArmorStandRenderState s,boolean visible,boolean transparent,boolean glowing){return ((State)s).appearance.isEmpty()?super.getRenderType(s,visible,transparent,glowing):null;}
 @net.neoforged.bus.api.SubscribeEvent public static void register(net.neoforged.neoforge.client.event.EntityRenderersEvent.RegisterRenderers e){e.registerEntityRenderer(net.minecraft.world.entity.EntityTypes.ARMOR_STAND,StyledArmorStandRenderer::new);}
 private static final class SkinModel extends HumanoidModel<ArmorStandRenderState>{SkinModel(net.minecraft.client.model.geom.ModelPart root){super(root);}@Override public void setupAnim(ArmorStandRenderState s){super.setupAnim(s);apply(head,s.headPose);apply(body,s.bodyPose);apply(leftArm,s.leftArmPose);apply(rightArm,s.rightArmPose);apply(leftLeg,s.leftLegPose);apply(rightLeg,s.rightLegPose);}private static void apply(net.minecraft.client.model.geom.ModelPart p,net.minecraft.core.Rotations r){p.xRot=(float)Math.toRadians(r.x());p.yRot=(float)Math.toRadians(r.y());p.zRot=(float)Math.toRadians(r.z());}}
 private static final class AppearanceLayer extends RenderLayer<ArmorStandRenderState,ArmorStandArmorModel>{
  private final Map<String,ImportedModel> models=new HashMap<>();private final SkinModel skin;private final PlayerSkinRenderCache skins;
  AppearanceLayer(StyledArmorStandRenderer parent,EntityRendererProvider.Context c){super(parent);skin=new SkinModel(c.bakeLayer(ModelLayers.PLAYER));skins=c.getPlayerSkinRenderCache();for(String id:com.mmorpg.entity.NpcEntity.AUTHORED_MODELS)models.put("guides:"+id,new ImportedModel("guides",id));}
  @Override public void submit(PoseStack pose,SubmitNodeCollector collector,int light,ArmorStandRenderState state,float yaw,float pitch){String look=((State)state).appearance;if(look.isEmpty()||state.isInvisible)return;
   var model=models.get(look);if(model!=null)collector.order(1).submitModel(model,state,pose,RenderTypes.entityCutout(model.texture),light,LivingEntityRenderer.getOverlayCoords(state,0),-1,null,state.outlineColor);
   else if(look.startsWith("skin:")){var info=skins.getOrDefault(ResolvableProfile.createUnresolved(look.substring(5)));collector.order(1).submitModel(skin,state,pose,info.renderType(),light,LivingEntityRenderer.getOverlayCoords(state,0),-1,null,state.outlineColor);}
  }
 }
}
