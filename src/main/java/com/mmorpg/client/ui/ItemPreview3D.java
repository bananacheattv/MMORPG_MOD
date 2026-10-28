package com.mmorpg.client.ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.state.ItemEntityRenderState;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import org.joml.Quaternionf;
import org.joml.Vector3f;
/** Native depth-tested entity rendering: no flat GUI sprite approximation. */
public final class ItemPreview3D {
 private static int nextId=160000;private ItemEntity entity;
 public void draw(GuiGraphicsExtractor g,ItemStack stack,int x,int y,int w,int h,float rotationX,float rotationY){
  var mc=Minecraft.getInstance();if(mc.level==null)return;if(entity==null||entity.level()!=mc.level){entity=new ItemEntity(mc.level,0,0,0,stack);entity.setId(nextId++);}entity.setItem(stack.copyWithCount(1));
  var renderer=mc.getEntityRenderDispatcher().getRenderer(entity);var state=(ItemEntityRenderState)renderer.createRenderState(entity,0);state.shadowPieces.clear();state.outlineColor=0;state.shouldBob=false;state.shouldSpread=false;state.count=1;state.ageInTicks=0;state.bobOffset=0;
  mc.getItemModelResolver().updateForTopItem(state.item,stack,ItemDisplayContext.FIXED,mc.level,mc.player,0);
  float size=Math.min(w,h)*.67f;
  g.entity(state,size,new Vector3f(0,.57f,0),new Quaternionf().rotateZ((float)Math.PI).rotateY(rotationY).rotateX(rotationX),new Quaternionf(),x,y,x+w,y+h);
 }
}
