package com.mmorpg.client.ui;
import com.mmorpg.MMORPG;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
public final class PortalLoadingView {
 private PortalLoadingView(){}
 public static void draw(GuiGraphicsExtractor g,int width,int height){
  float scale=Math.max(width/836f,height/470f);int dw=(int)Math.ceil(836*scale),dh=(int)Math.ceil(470*scale);
  g.blit(RenderPipelines.GUI_TEXTURED,MMORPG.id("textures/gui/illustrations/portal_loading_background.png"),(width-dw)/2,(height-dh)/2,0,0,dw,dh,836,470,836,470);
  g.fill(0,0,width,height,0x9010091F);
  int cx=width/2,cy=Math.min(height/2-45,height/4+20);double time=System.nanoTime()/1e9;
  for(int ring=0;ring<2;ring++)for(int i=0;i<40;i++){double a=i*Math.PI/20+time*(ring==0?.65:-.4);int radius=34+ring*7;int x=cx+(int)(Math.cos(a)*radius),y=cy+(int)(Math.sin(a)*radius*.82);Ui.diamond(g,x,y,i%5==0?2:1,Ui.withAlpha(ring==0?0xBD8BFF:Ui.GOLD,65+(i%8)*21));}
  Logo.emblem(g,cx-23,cy-23,46);
  Ui.scaledText(g,"Passage dans le portail",cx,cy+45,1.35f,Ui.GOLD_LIGHT,true);
  g.centeredText(Ui.font(),net.minecraft.network.chat.Component.literal("Téléportation en cours…"),cx,cy+64,Ui.TEXT);
  Ui.rounded(g,cx-76,cy+82,152,3,1,0x605E437A);
  int x=cx-76+(int)((Math.sin(time*1.9)+1)*.5*122);Ui.rounded(g,x,cy+82,30,3,1,0xFFD1A7FF);
 }
}
