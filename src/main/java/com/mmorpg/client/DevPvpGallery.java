package com.mmorpg.client;
import com.mmorpg.client.ui.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
/** Private native scene containing the same equipped meshes and mounted entity used by players. */
public final class DevPvpGallery extends MmoScreen {
 private final net.minecraft.world.entity.LivingEntity[] actors=new net.minecraft.world.entity.LivingEntity[6];
 public DevPvpGallery(){super("Équipements des arènes");}
 @Override protected int panelWidth(){return width-12;}@Override protected int panelHeight(){return height-12;}
 @Override protected void renderContent(GuiGraphicsExtractor g,int mx,int my,float a){Ui.panel(g,left,top,pw,ph);Ui.title(g,left+pw/2,top+8,"ÉQUIPEMENTS & CHIMÈRE DES ARÈNES",Ui.GOLD);int cw=(pw-32)/3,ch=(ph-42)/2;
  for(int i=0;i<6;i++){int x=left+8+i%3*(cw+8),y=top+28+i/3*(ch+5);Ui.inset(g,x,y,cw,ch);if(actors[i]==null){if(i==4){var type=com.mmorpg.mount.MountType.CHIMERE_ARENE;var mount=new com.mmorpg.entity.MountEntity(com.mmorpg.registry.ModEntities.MOUNTS.get(type).get(),minecraft.level);mount.setId(185004);actors[i]=mount;}else{var s=new net.minecraft.world.entity.decoration.ArmorStand(net.minecraft.world.entity.EntityTypes.ARMOR_STAND,minecraft.level){@Override public net.minecraft.world.entity.EntityDimensions getDefaultDimensions(net.minecraft.world.entity.Pose pose){return net.minecraft.world.entity.EntityDimensions.scalable(.7f,3.1f);}};s.setId(185000+i);s.setShowArms(true);s.refreshDimensions();if(i==5){s.setData(com.mmorpg.registry.ModAttachments.STAND_LOOK,"skin:Notch");s.setRightArmPose(new net.minecraft.core.Rotations(-25,0,-10));s.setLeftArmPose(new net.minecraft.core.Rotations(-15,0,10));}else s.setInvisible(true);String cls=new String[]{"guerrier","mage","archer","tank","","guerrier"}[i];var slots=new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET};for(int j=0;j<4;j++)s.setItemSlot(slots[j],new ItemStack(com.mmorpg.registry.ModItems.get("pvp_"+cls+"_4_"+com.mmorpg.item.ItemDefs.PIECES[j])));actors[i]=s;}}
   g.centeredText(font,net.minecraft.network.chat.Component.literal(new String[]{"Gladiateur · Guerrier","Gladiateur · Mage","Gladiateur · Archer","Gladiateur · Paladin","Chimère · Monture PvP","Skin + armure articulée"}[i]),x+cw/2,y+5,Ui.GOLD);
   com.mmorpg.client.ui.Ui.entityFollowsMouse(g,x+4,y+18,x+cw-4,y+ch-2,(int)((ch-23)/(i==4?5.6:3.3)),.0625f,x+cw/2f-12,y+ch/2f,actors[i]);
  }
 }
}
