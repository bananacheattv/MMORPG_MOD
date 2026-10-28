package com.mmorpg.client;
import com.mmorpg.client.ui.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
public final class DevChestProof extends MmoScreen {
 private final ItemPreview3D[] previews=new ItemPreview3D[6];
 public DevChestProof(){super("Coffres");for(int i=0;i<6;i++)previews[i]=new ItemPreview3D();}
 @Override protected int panelWidth(){return width-12;}@Override protected int panelHeight(){return height-12;}
 @Override protected void renderContent(GuiGraphicsExtractor g,int mx,int my,float a){Ui.panel(g,left,top,pw,ph);Ui.title(g,left+pw/2,top+8,"COFFRES · CHARNIÈRES ARRIÈRE",Ui.GOLD);int cw=(pw-32)/3,ch=(ph-42)/2;for(int i=0;i<6;i++){int x=left+8+i%3*(cw+8),y=top+28+i/3*(ch+5);Ui.inset(g,x,y,cw,ch);String key=new String[]{"rare","epique","legendaire"}[i%3],state=i<3?"fermee":"ouverte";var stack=new net.minecraft.world.item.ItemStack(com.mmorpg.registry.ModItems.MYSTERY_CUBE.get());stack.set(net.minecraft.core.component.DataComponents.ITEM_MODEL,com.mmorpg.MMORPG.id("ui_caisse_"+key+"_"+state));g.centeredText(font,net.minecraft.network.chat.Component.literal(key+" · "+(i<3?"Fermé":"Ouvert")),x+cw/2,y+5,Ui.GOLD);previews[i].draw(g,stack,x+5,y+20,cw-10,ch-25,-.3f,.65f);}}
}
