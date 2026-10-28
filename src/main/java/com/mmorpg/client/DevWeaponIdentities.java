package com.mmorpg.client;
import com.mmorpg.client.ui.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;
/** Native QA only: low, mid and prestige silhouettes of every class. */
public final class DevWeaponIdentities extends MmoScreen {
 private final ItemPreview3D[] previews=new ItemPreview3D[12];
 private final String[] ids={"epee_recrue","baton_apprenti","arc_chasseur","marteau_colosse","epee_seigneur_guerre","baton_arcanique","arc_faucon","masse_gardien","excalibur_celeste","baton_archimage","arc_aube_divine","marteau_egide"};
 public DevWeaponIdentities(){super("Armes MMORPG");for(int i=0;i<12;i++)previews[i]=new ItemPreview3D();}
 @Override protected int panelWidth(){return width-12;}@Override protected int panelHeight(){return height-12;}
 @Override protected void renderContent(GuiGraphicsExtractor g,int mx,int my,float a){Ui.panel(g,left,top,pw,ph);Ui.title(g,left+pw/2,top+7,"IDENTITÉS ET PROGRESSION DES ARMES",Ui.GOLD);int cw=(pw-35)/4,ch=(ph-39)/3;for(int i=0;i<12;i++){int x=left+7+i%4*(cw+7),y=top+25+i/4*(ch+4);Ui.inset(g,x,y,cw,ch);var stack=new ItemStack(com.mmorpg.registry.ModItems.get(ids[i]));g.centeredText(font,font.plainSubstrByWidth(stack.getHoverName().getString(),cw-6),x+cw/2,y+4,Ui.TEXT);previews[i].draw(g,stack,x+3,y+17,cw-6,ch-20,-.1f,.42f);}}
}
