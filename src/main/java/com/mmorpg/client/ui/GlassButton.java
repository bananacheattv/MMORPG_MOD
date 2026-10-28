package com.mmorpg.client.ui;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
/** Native keyboard/narration behavior with Aeloria glass styling. */
public final class GlassButton extends Button {
 private final int accent;
 public GlassButton(int x,int y,int w,int h,String text,int accent,OnPress action){super(x,y,w,h,Component.literal(text),action,DEFAULT_NARRATION);this.accent=accent;}
 @Override protected void extractContents(GuiGraphicsExtractor g,int mx,int my,float a){Ui.button(g,getX(),getY(),getWidth(),getHeight(),getMessage().getString(),active,isFocused()?getX()+1:mx,isFocused()?getY()+1:my,accent);}
}
