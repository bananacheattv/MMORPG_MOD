package com.mmorpg.client.screen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import com.mmorpg.client.ui.*;
/** Second inventory page, with native creative catalogue and its server permission checks. */
public final class AeloriaCreativeScreen extends CreativeModeInventoryScreen {
 public AeloriaCreativeScreen(){super(Minecraft.getInstance().player,Minecraft.getInstance().player.connection.enabledFeatures(),Minecraft.getInstance().options.operatorItemsTab().get());}
 @Override protected void init(){super.init();int x=Math.max(6,width/2-155);addRenderableWidget(new GlassButton(x,6,150,22,"1 · PERSONNAGE",Ui.GOLD,b->minecraft.gui.setScreen(new CharacterScreen(minecraft.player))));addRenderableWidget(new GlassButton(x+156,6,150,22,"2 · CRÉATIF",Ui.GOLD,b->{}));}
}
