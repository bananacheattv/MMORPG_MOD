package com.mmorpg.client.screen;
import com.mmorpg.client.ui.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.multiplayer.ClientLevel;
public final class AeloriaPauseScreen extends PauseScreen {
 public AeloriaPauseScreen(){super(true);}
 private void button(int x,int y,int w,String label,Runnable action){addRenderableWidget(new GlassButton(x,y,w,25,label,Ui.GOLD,b->action.run()));}
 @Override protected void init(){
  button(12,12,135,"REVENIR AU JEU",()->{minecraft.gui.setScreen(null);minecraft.mouseHandler.grabMouse();});
  button(width-122,12,110,"DÉCONNEXION",()->minecraft.getReportingContext().draftReportHandled(minecraft,this,()->minecraft.disconnectFromWorld(ClientLevel.DEFAULT_QUIT_MESSAGE),true));
  int w=Math.min(300,width-36),x=(width-w)/2,y=Math.max(104,height/2-12);
  button(x,y,w,"MON AVENTURE",()->MenuScreen.open(MenuScreen.Tab.PERSONNAGE));
  button(x,y+32,w,"BOUTIQUE",()->MenuScreen.open(MenuScreen.Tab.BOUTIQUE));
  button(x,y+64,w,"OPTIONS",()->minecraft.gui.setScreen(new OptionsScreen(this,minecraft.options)));
  button(x,y+96,w,"TOUS LES MENUS",()->minecraft.gui.setScreen(new AeloriaDirectoryScreen()));
 }
 @Override public void extractBackground(GuiGraphicsExtractor g,int mx,int my,float a){extractBlurredBackground(g);g.fill(0,0,width,height,0x800C071A);}
 @Override public void extractRenderState(GuiGraphicsExtractor g,int mx,int my,float a){int by=Math.max(104,height/2-12),lw=Math.min(240,(int)((by-45)*Logo.FULL_RATIO));Logo.full(g,width/2,by-12-(int)(lw/Logo.FULL_RATIO),lw);super.extractRenderState(g,mx,my,a);}
}
