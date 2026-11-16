package com.mmorpg.client.screen;
import com.mmorpg.client.ui.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.multiplayer.ClientLevel;
public final class AeloriaPauseScreen extends PauseScreen {
 public AeloriaPauseScreen(){super(true);}
 private void button(int x,int y,int w,int h,String label,Runnable action){addRenderableWidget(new GlassButton(x,y,w,h,label,Ui.GOLD,b->action.run()));}
 private int rows(){return com.mmorpg.client.ClientData.features.getBooleanOr("wiki",true)?5:4;}
 /** Haut de la colonne de boutons : descend jusqu'a 104 mais remonte pour que toute la colonne tienne dans la fenetre. */
 private int colTop(){return Math.min(Math.max(104,height/2-28),height-8-((rows()-1)*24+22));}
 private int step(){return Math.min(32,(height-8-colTop()-22)/(rows()-1));}
 @Override protected void init(){
  button(width-122,12,110,25,"DÉCONNEXION",()->minecraft.getReportingContext().draftReportHandled(minecraft,this,()->minecraft.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE),true));
  int w=Math.min(300,width-36),x=(width-w)/2,y=colTop(),st=step(),bh=Math.min(25,st-3);
  button(x,y,w,bh,"REVENIR AU JEU",()->{minecraft.gui.setScreen(null);minecraft.mouseHandler.grabMouse();});
  button(x,y+1*st,w,bh,"MON AVENTURE",()->MenuScreen.open(MenuScreen.Tab.PERSONNAGE));
  button(x,y+2*st,w,bh,"BOUTIQUE",()->MenuScreen.open(MenuScreen.Tab.BOUTIQUE));
  button(x,y+3*st,w,bh,"OPTIONS",()->minecraft.gui.setScreen(new OptionsScreen(this,minecraft.options)));
  if(com.mmorpg.client.ClientData.features.getBooleanOr("wiki",true))button(x,y+4*st,w,bh,"WIKI",()->MenuScreen.open(MenuScreen.Tab.WIKI));
 }
 @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float a){extractBlurredBackground(g);g.fill(0,0,width,height,0x800C071A);}
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float a){int by=colTop(),lw=Math.min(240,(int)((by-45)*Logo.FULL_RATIO));Logo.full(g,width/2,by-12-(int)(lw/Logo.FULL_RATIO),lw);super.extractRenderState(g,mx,my,a);}
}
