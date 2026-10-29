package com.mmorpg.client.screen;
import com.mmorpg.client.ui.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.network.chat.Component;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Sacs a dos : meme cadre et meme barre de base que l'inventaire ; sacs a gauche, inventaire a droite, grandes cases. */
public final class BackpackScreen extends AbstractContainerScreen<ChestMenu>{
 private int px,py,pw,ph,cell=26,gx,gy,ix;
 private final Map<Slot,int[]> original=new HashMap<>();
 private final List<int[]> navRects=new ArrayList<>();
 private final List<Runnable> navActions=new ArrayList<>();
 public BackpackScreen(ChestMenu menu,Inventory inventory,Component title){super(menu,inventory,title,176,222);com.mmorpg.server.Backpacks.restrict(menu);inventoryLabelY=128;}
 @Override protected void init(){
  super.init();
  pw=UiFrame.width(width);ph=UiFrame.height(height);px=(width-pw)/2;py=(height-ph)/2;
  leftPos=px;topPos=py;
  int bodyH=ph-UiFrame.NAV_H-26;
  cell=Math.clamp(Math.min((pw/2-24)/9,bodyH/6),20,44);
  gx=12;gy=UiFrame.NAV_H+20;ix=pw/2+12;
  for(Slot slot:menu.slots){
   original.putIfAbsent(slot,new int[]{slot.x,slot.y});
   int sx,sy,i=slot.index;
   if(i<54){sx=gx+(i%9)*cell;sy=gy+(i/9)*cell;}
   else if(i<81){sx=ix+((i-54)%9)*cell;sy=gy+((i-54)/9)*cell;}
   else{sx=ix+(i-81)*cell;sy=gy+3*cell+8;}
   var access=(com.mmorpg.mixin.SlotPositionAccessor)slot;access.aeloria$x(sx);access.aeloria$y(sy);
  }
 }
 @Override public void removed(){for(var e:original.entrySet()){var a=(com.mmorpg.mixin.SlotPositionAccessor)e.getKey();a.aeloria$x(e.getValue()[0]);a.aeloria$y(e.getValue()[1]);}super.removed();}
 @Override protected boolean hasClickedOutside(double x,double y,int xo,int yo){return !Ui.in(x,y,px,py,pw,ph);}
 @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float a){
  extractBlurredBackground(g);g.fill(0,0,width,height,0x700D071C);Ui.navBar(g,width,py);
  Ui.panel(g,px,py,pw,ph);
  navRects.clear();navActions.clear();
  MenuScreen.drawTabs(g,font,px,py,pw,MenuScreen.Tab.PERSONNAGE,mx,my,(r,action)->{navRects.add(r);navActions.add(()->{minecraft.player.closeContainer();action.run();});});
  g.fill(px+6,py+48,px+pw-6,py+49,Ui.LINE);
  Ui.inset(g,px+8,py+UiFrame.NAV_H+4,pw/2-12,ph-UiFrame.NAV_H-12);
  Ui.inset(g,px+pw/2+4,py+UiFrame.NAV_H+4,pw/2-12,ph-UiFrame.NAV_H-12);
  for(var slot:menu.slots){int x=leftPos+slot.x-1,y=topPos+slot.y-1,s=cell-2;
   g.fillGradient(x,y,x+s,y+s,0xFF3E3858,0xFF2C2740);
   Ui.border(g,x,y,s,s,slot.index<2?Ui.GOLD:Ui.LINE);
   if(slot.index<54&&!slot.isActive())Ui.rounded(g,x+1,y+1,s-2,s-2,3,0xD0140D20);}
 }
 @Override public boolean mouseClicked(MouseButtonEvent event,boolean twice){
  for(int i=0;i<navRects.size();i++){var r=navRects.get(i);if(Ui.in(event.x(),event.y(),r[0],r[1],r[2],r[3])){MmoScreen.playClick();navActions.get(i).run();return true;}}
  return super.mouseClicked(event,twice);
 }
 @Override protected boolean isHovering(int x,int y,int w,int h,double mx,double my){if(w==16&&h==16)return super.isHovering(x-1,y-1,cell-2,cell-2,mx,my);return super.isHovering(x,y,w,h,mx,my);}
 @Override protected void renderSlotContents(GuiGraphicsExtractor g,net.minecraft.world.item.ItemStack stack,Slot slot,String count){
  g.pose().pushMatrix();g.pose().translate(slot.x,slot.y);float k=(cell-5)/16f;g.pose().scale(k,k);g.pose().translate(-slot.x,-slot.y);super.renderSlotContents(g,stack,slot,count);g.pose().popMatrix();
 }
 public void slotHighlight(GuiGraphicsExtractor g,boolean front){
  if(hoveredSlot==null||!hoveredSlot.isHighlightable())return;
  int s=cell-2,x=hoveredSlot.x-1,y=hoveredSlot.y-1;
  if(front)Ui.border(g,x,y,s,s,0xB0D4B2FF);else g.fill(x,y,x+s,y+s,0x303E2B55);
 }
 @Override protected void extractLabels(GuiGraphicsExtractor g,int mx,int my){
  g.text(font,"SACS À DOS · +"+com.mmorpg.server.Backpacks.capacity(menu.getContainer())+" cases",gx,UiFrame.NAV_H+8,Ui.GOLD,false);
  g.text(font,"Inventaire",ix,UiFrame.NAV_H+8,Ui.MUTED,false);
 }
}
