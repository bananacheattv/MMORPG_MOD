package com.mmorpg.client;
import com.mmorpg.client.ui.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
/** Private native renders of the same meshes and textures used in game. */
public final class DevSculptedGallery extends MmoScreen {
 public int page;private final ItemPreview3D[] previews=new ItemPreview3D[6];
 private final net.minecraft.world.entity.decoration.ArmorStand[] actors=new net.minecraft.world.entity.decoration.ArmorStand[6];
 public DevSculptedGallery(){super("Atelier 3D");for(int i=0;i<6;i++)previews[i]=new ItemPreview3D();}
 @Override protected int panelWidth(){return width-12;}@Override protected int panelHeight(){return height-12;}
 @Override protected void renderContent(GuiGraphicsExtractor g,int mx,int my,float a){Ui.panel(g,left,top,pw,ph);Ui.title(g,left+pw/2,top+8,new String[]{"ARMURERIE D’AELORIA","RELIQUAIRES DE FORTUNE","COFFRES DE LA CITADELLE","PANOPLIES ET PERSONNAGES","PROGRESSION : GUERRIER ET MAGE","PROGRESSION : ARCHER ET PALADIN"}[page],Ui.GOLD);
  String[][] ids={{"epee_recrue","excalibur_celeste","baton_apprenti","baton_archimage","marteau_egide","arc_aube_divine"},{"lucky_celeste","lucky_sylvestre","lucky_infernal","lucky_givre","lucky_neant","cube_mystere"},{"caisse_commune_bloc","caisse_rare_bloc","caisse_epique_bloc","caisse_legendaire_bloc","caisse_mythique_bloc","sac_a_dos_3"}};
  int cw=(pw-32)/3,ch=(ph-40)/2;
  for(int i=0;i<6;i++){int x=left+8+i%3*(cw+8),y=top+28+i/3*(ch+5);Ui.inset(g,x,y,cw,ch);
   if(page<3){var item=i==5&&page==1?com.mmorpg.registry.ModItems.MYSTERY_CUBE.get():net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(com.mmorpg.MMORPG.id(ids[page][i]));var stack=new ItemStack(item);g.centeredText(font,stack.getHoverName(),x+cw/2,y+5,Ui.GOLD);previews[i].draw(g,stack,x+4,y+18,cw-8,ch-22,-.12f,.5f);}
   else{if(actors[i]==null){var s=net.minecraft.world.entity.EntityTypes.ARMOR_STAND.create(minecraft.level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);s.setShowArms(true);s.setId(145000+i);if(i<3||page>=4){s.setInvisible(true);String set=page==4?new String[]{"acier_soldat","seigneur_guerre","dieu_guerre","apprenti","archimage","avatar_arcanique"}[i]:page==5?new String[]{"chasseur","tireur_elite","sylvestre","garde","paladin","egide_divine"}[i]:new String[]{"egide_divine","avatar_arcanique","sylvestre"}[i];var slots=new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,net.minecraft.world.entity.EquipmentSlot.FEET};for(int j=0;j<4;j++)s.setItemSlot(slots[j],new ItemStack(com.mmorpg.registry.ModItems.get(set+"_"+com.mmorpg.item.ItemDefs.PIECES[j])));}else{s.setData(com.mmorpg.registry.ModAttachments.STAND_LOOK,i==3?"guides:borin":i==4?"guides:master_mage":"skin:Notch");s.setRightArmPose(new net.minecraft.core.Rotations(-60,0,5));}actors[i]=s;}
    String label=page>=4?new String[]{"Débutant · Niv. 1","Confirmé · Niv. 50","Prestige · Niv. 85–100","Débutant · Niv. 1","Confirmé · Niv. 50","Prestige · Niv. 85–100"}[i]:new String[]{"Paladin","Mage","Archer","Modèle : Borin","Modèle : Mage","Skin de joueur"}[i];g.centeredText(font,label,x+cw/2,y+5,Ui.GOLD);net.minecraft.client.gui.screens.inventory.InventoryScreen.extractEntityInInventoryFollowsMouse(g,x+4,y+18,x+cw-4,y+ch-2,(ch-25)/2,.0625f,x+cw/2f-15,y+ch/2f,actors[i]);}
  }
 }
}
