package com.mmorpg.client.model.armor;
import com.mmorpg.rpg.PlayerClass;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.world.entity.EquipmentSlot;
/** Original fitted silhouettes with four texels per model unit and articulated details. */
public final class RpgArmorSculptor {
 public static int rank(int level){return level<10?0:level<25?1:level<45?2:level<65?3:level<85?4:5;}
 private static final java.util.concurrent.atomic.AtomicInteger COUNTER=new java.util.concurrent.atomic.AtomicInteger();
 private static void cube(PartDefinition parent,float x,float y,float z,float w,float h,float d,int material){cube(parent,x,y,z,w,h,d,material,0,0,0);}
 private static void cube(PartDefinition parent,float x,float y,float z,float w,float h,float d,int material,float rx,float ry,float rz){
  parent.addOrReplaceChild("detail_"+COUNTER.incrementAndGet(),CubeListBuilder.create().texOffs(material%4*256+8,material/4*256+8).addBox(-w*2,-h*2,-d*2,w*4,h*4,d*4),PartPose.offsetAndRotation(x,y,z,rx,ry,rz).withScale(.25f));
 }
 public static LayerDefinition create(PlayerClass cls,int level,EquipmentSlot slot){return create(cls,level,slot,false);}
 public static LayerDefinition create(PlayerClass cls,int level,EquipmentSlot slot,boolean pvp){
  int r=pvp?1+(level-1)/20:rank(level);boolean mage=cls==PlayerClass.MAGE,ranger=cls==PlayerClass.ARCHER,paladin=cls==PlayerClass.TANK;
  int body=mage?13:ranger?(r>=3?0:6):paladin?(r>=3?12:3):(r>=3?10:3),trim=r>=3?1:mage?2:ranger?11:3,gem=mage?4:ranger?6:paladin?5:8;
  var mesh=new MeshDefinition();var root=mesh.getRoot();var head=root.addOrReplaceChild("head",CubeListBuilder.create(),PartPose.ZERO);head.addOrReplaceChild("hat",CubeListBuilder.create(),PartPose.ZERO);
  var torso=root.addOrReplaceChild("body",CubeListBuilder.create(),PartPose.ZERO);
  var arms=new PartDefinition[]{root.addOrReplaceChild("right_arm",CubeListBuilder.create(),PartPose.offset(-5,2,0)),root.addOrReplaceChild("left_arm",CubeListBuilder.create(),PartPose.offset(5,2,0))};
  var legs=new PartDefinition[]{root.addOrReplaceChild("right_leg",CubeListBuilder.create(),PartPose.offset(-1.9f,12,0)),root.addOrReplaceChild("left_leg",CubeListBuilder.create(),PartPose.offset(1.9f,12,0))};
  if(slot==EquipmentSlot.HEAD){
   if(mage&&r>=2){cube(head,0,-7.8f,0,12.5f,.9f,11,body);for(int i=0;i<4;i++)cube(head,i*.25f,-9.2f-i*1.5f,.5f,8.8f-i*1.8f,1.7f,7.8f-i*1.6f,body);cube(head,0,-8.6f,-4.2f,8,.6f,.5f,trim);if(r>=4)cube(head,0,-10.8f,-3.3f,1.3f,1.8f,.7f,gem,0,0,.7854f);}
   else{
    cube(head,0,-8.15f,0,8.7f,1,8.7f,body);cube(head,0,-4,4,8.8f,8.7f,1.1f,body);
    for(int side:new int[]{-1,1})cube(head,side*4.2f,-3.7f,0,1.1f,8,8.2f,body);
    cube(head,0,-6.4f,-4.15f,8.7f,1.1f,1,trim);
    if(!mage&&!ranger){cube(head,0,-1.5f,-4.3f,8.4f,3.2f,1,body);cube(head,0,-3.7f,-4.9f,1,5.2f,.8f,trim);for(int side:new int[]{-1,1})cube(head,side*3.8f,-3,-4.45f,1.1f,5,1.2f,trim,0,0,side*.13f);}
    else{cube(head,0,-.4f,-3.3f,8.4f,2.2f,2,body);cube(head,0,-8.7f,.5f,6.5f,1.2f,7,body);}
    if(r>=2&&paladin){for(int side:new int[]{-1,1})cube(head,side*3.7f,-9.6f,-2.7f,1,3.7f,1.2f,trim,0,0,side*.19f);cube(head,0,-8.9f,-4.2f,1.7f,2.3f,1.1f,gem,0,0,.7854f);}
    if(r>=2&&!mage&&!ranger&&!paladin){for(int side:new int[]{-1,1})cube(head,side*5.1f,-8.1f,0,1.2f,4.8f,2.2f,trim,0,0,side*.4f);cube(head,0,-8.9f,1,1.3f,2,7,gem);}
    if(r>=3&&ranger){for(int i=0;i<3;i++)cube(head,-4.7f-i*.35f,-7.5f-i*.7f,1.7f,1,4.3f,2.7f,6,0,0,-.38f);}
   }
   if(r>=4){for(int side:new int[]{-1,1})for(int i=0;i<3;i++)cube(head,side*(1.8f+i*.85f),-6.5f,-4.8f,.3f,.5f,.3f,trim);}
   if(r==5){
    if(mage){for(int i=0;i<4;i++)cube(head,1+i*.6f,-15-i*1.3f,0,4.2f-i*.9f,1.6f,4.2f-i*.9f,13,0,0,-.12f);for(int side:new int[]{-1,1})cube(head,side*5.8f,-8.4f,0,2,1,9,trim);}
    else if(ranger){for(int side:new int[]{-1,1})for(int j=0;j<3;j++)cube(head,side*(4.9f+j*.7f),-8.2f-j*1.7f,1,1.4f,4.6f,2.3f,6,0,0,side*.35f);cube(head,0,-7.2f,-4.9f,2,2,1,gem,0,0,.7854f);}
    else for(int side:new int[]{-1,1})for(int j=0;j<3;j++)cube(head,side*(5+j*.9f),-10-j*1.4f,0,1.5f,5-j*.6f,3.3f,trim,0,0,side*.35f);
   }
  }
  if(slot==EquipmentSlot.CHEST){
   cube(torso,0,5.9f,0,8.7f,11.4f,5,body);cube(torso,0,10.8f,0,9.2f,1.2f,5.6f,mage?trim:11);cube(torso,0,10.7f,-3.15f,2,1.7f,.7f,trim);
   if(paladin){cube(torso,0,5.7f,-2.8f,5.8f,9.5f,.7f,12);cube(torso,0,4.8f,-3.35f,1,6.6f,.5f,trim);cube(torso,0,4,-3.4f,5.3f,1,.5f,trim);}
   else if(ranger){cube(torso,0,5.4f,-3.1f,1.2f,11,.7f,11,0,0,.45f);cube(torso,-3.4f,9.6f,-3,2.4f,2.4f,2,7);cube(torso,2.3f,4.9f,4.1f,3.2f,8.8f,3,11,0,0,-.12f);for(int i=0;i<3;i++)cube(torso,1.3f+i*.85f,-.4f,4.5f,.25f,3.5f,.25f,7);}
   else for(int side:new int[]{-1,1})cube(torso,side*2.6f,3,-2.9f,.9f,7.5f,.7f,trim,0,0,side*.23f);
   if(r>=2){cube(torso,0,2.7f,-3.2f,1.5f,2,.8f,gem,0,0,.7854f);if(!mage&&!ranger)for(int j=0;j<3;j++)cube(torso,0,7+j*1.2f,-3,7-j*.6f,.65f,1,body);}
   for(int i=0;i<2;i++){int side=i==0?-1:1;var arm=arms[i];cube(arm,0,5.9f,0,4.7f,12.2f,4.7f,mage||ranger?body:11);
    if(mage){cube(arm,0,9,0,5.8f,2.3f,5.8f,body);cube(arm,0,9.9f,0,6,.6f,6,trim);cube(arm,side*.7f,-.8f,0,5.5f,2,5.8f,body);}
    else if(ranger){cube(arm,side*.5f,-1,0,5.8f,2,5.7f,11);cube(arm,0,7.7f,-2.7f,4.2f,3.9f,1.1f,body);}
    else{int layers=1+r/2;for(int j=0;j<layers;j++)cube(arm,side*(.6f+j*.2f),-1.1f-j*.85f,0,5.5f+r*.25f-j*.6f,1.6f,6.4f-j*.35f,j%2==0?body:trim,0,0,side*.07f);cube(arm,0,7.5f,0,5,4.2f,5.3f,body);cube(arm,0,9.5f,0,5.3f,.7f,5.6f,trim);}
    if(r>=3){for(int j=0;j<3;j++)cube(arm,side*1.4f,5.5f+j,-2.8f,.4f,.4f,.4f,trim);cube(arm,0,7.5f,-3,1.3f,1.7f,.8f,gem,0,0,.7854f);}
    if(r>=4&&!mage){for(int j=0;j<2;j++)cube(arm,side*(1.6f+j*.6f),-1.9f-j*.6f,0,1.1f,3.2f-j*.4f,3.7f,trim,0,0,side*.2f);}
    if(r==5){
     if(mage){cube(arm,side*.6f,-2.2f,0,7.4f,1.3f,7.3f,trim);cube(arm,side*.8f,8.7f,0,7,2.1f,7,body);cube(arm,side*.8f,9.9f,0,7.4f,.65f,7.4f,trim);}
     else for(int j=0;j<3;j++)cube(arm,side*(1.9f+j*.6f),-2.4f-j*.7f,0,1.7f,4-j*.6f,5-j*.5f,ranger?6:trim,0,0,side*.3f);
    }
   }
   if(r>=3){for(int side:new int[]{-1,1})cube(torso,side*2.3f,7.7f,3.6f,3.7f,14,1,mage?13:paladin?12:ranger?6:8);}
   if(r==5){for(int side:new int[]{-1,1})for(int j=0;j<3;j++)cube(torso,side*(1.5f+j*.65f),4.5f+j*.5f,-3.5f,.35f,1.1f,.5f,trim,0,0,-side*.5f);}
  }
  if(slot==EquipmentSlot.LEGS){cube(torso,0,10.4f,0,9.1f,2.1f,5.5f,mage?13:11);
   for(int i=0;i<2;i++){var leg=legs[i];cube(leg,0,5.7f,0,mage?4.7f:4.25f,11.5f,mage?5.5f:4.7f,body);
    if(mage){cube(leg,0,11,-2.9f,4.6f,1,.65f,trim);if(r>=2)for(int j=0;j<3;j++)cube(leg,0,4+j*2,-3,.4f,.7f,.4f,gem);}
    else if(paladin){cube(leg,0,4.5f,-2.7f,3.8f,8.5f,.7f,12);cube(leg,0,8.4f,-3,4,.8f,.5f,trim);if(r>=2)cube(leg,0,3.7f,-3.2f,1.5f,1.5f,.4f,gem,0,0,.7854f);}
    else{cube(leg,0,6.8f,-2.8f,3.7f,2.9f,1.3f,ranger?11:trim);if(r>=2)cube(leg,0,9,-2.8f,1.1f,4.1f,.8f,trim);}
   }
  }
  if(slot==EquipmentSlot.FEET)for(int i=0;i<2;i++){var leg=legs[i];cube(leg,0,10.5f,-.7f,mage?4.5f:4.9f,3.3f,mage?5.2f:6.2f,mage?13:ranger?11:body);cube(leg,0,11.9f,-.6f,5.1f,.7f,6.3f,10);if(!mage){cube(leg,0,8.7f,0,5.3f,1.6f,5.5f,r>=2?trim:body);cube(leg,0,10.6f,-3.3f,3.4f,1.7f,1.2f,trim);}else cube(leg,0,10.2f,-3.1f,3.5f,.6f,.4f,trim);}
  if(pvp){
   if(slot==EquipmentSlot.CHEST){cube(torso,0,6,-3.5f,4.5f,6,.65f,8);for(int side:new int[]{-1,1})cube(torso,side*.7f,5.5f,-4, .6f,4.3f,.45f,1,0,0,side*.65f);for(var arm:arms)cube(arm,0,-2.2f,-.2f,6.5f+r*.25f,1,6.8f,3);cube(torso,0,7,3.8f,7.8f,13.5f,1,8);}
   if(slot==EquipmentSlot.HEAD){cube(head,0,-6.5f,-5,2,2,.8f,8,0,0,.7854f);for(int side:new int[]{-1,1})cube(head,side*3.8f,-7.8f,-3,1.1f,2.7f,2.7f,1);}
   if(slot==EquipmentSlot.LEGS)for(var leg:legs)cube(leg,0,6.6f,-3.5f,2.2f,3,.7f,8);
  }
  return LayerDefinition.create(mesh,1024,1024);
 }
}
