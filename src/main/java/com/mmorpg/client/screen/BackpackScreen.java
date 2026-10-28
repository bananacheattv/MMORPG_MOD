package com.mmorpg.client.screen;
import com.mmorpg.client.ui.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
public final class BackpackScreen extends AbstractContainerScreen<ChestMenu>{
 public BackpackScreen(ChestMenu menu,Inventory inventory,Component title){super(menu,inventory,title,176,222);com.mmorpg.server.Backpacks.restrict(menu);inventoryLabelY=128;}
 @Override protected void init(){super.init();addRenderableWidget(new GlassButton(6,6,103,20,"‹ Personnage",Ui.GOLD,b->{minecraft.player.closeContainer();minecraft.gui.setScreen(new CharacterScreen(minecraft.player));}));}
 @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float a){extractBlurredBackground(g);g.fill(0,0,width,height,0x700D071C);Ui.panel(g,leftPos-5,topPos-5,imageWidth+10,imageHeight+10);
  for(var slot:menu.slots){int x=leftPos+slot.x-1,y=topPos+slot.y-1;Ui.frame(g,x,y,18,18,slot.index<2?Ui.GOLD:Ui.LINE,slot.index<2);if(slot.index<54&&!slot.isActive())Ui.rounded(g,x+1,y+1,16,16,3,0xD0140D20);}
 }
 @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){g.text(font,"SACS À DOS · +"+com.mmorpg.server.Backpacks.capacity(menu.getContainer())+" cases",8,6,Ui.GOLD,false);g.text(font,"Inventaire",8,128,Ui.MUTED,false);}
}
