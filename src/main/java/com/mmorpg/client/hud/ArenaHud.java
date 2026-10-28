package com.mmorpg.client.hud;
import com.mmorpg.client.*;
import com.mmorpg.client.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
public final class ArenaHud {
 public static void draw(GuiGraphicsExtractor g){var t=ClientData.arenaState;if(!t.getBooleanOr("active",false)||ClientData.now()-ClientData.arenaUpdated>40)return;var mc=Minecraft.getInstance();int w=250,x=(mc.getWindow().getGuiScaledWidth()-w)/2,y=7;
  Ui.rounded(g,x,y,w,58,8,0xDF170F27);Ui.border(g,x,y,w,58,Ui.LINE);
  g.centeredText(mc.font,net.minecraft.network.chat.Component.literal("ARÈNE · ÉQUIPE "+(t.getIntOr("team",0)==0?"ROUGE":"BLEUE")),x+w/2,y+5,Ui.GOLD);
  int red=t.getIntOr("red",0),blue=t.getIntOr("blue",0);Ui.bar(g,x+8,y+18,112,10,red/500f,0xFFFF6262,0xFF762131,"Rouges "+red+" / 500");Ui.bar(g,x+130,y+18,112,10,blue/500f,0xFF60ADFF,0xFF214576,"Bleus "+blue+" / 500");
  int owner=t.getIntOr("owner",-1),hits=t.getIntOr("hits",0),color=owner<0?Ui.GOLD:owner==0?Ui.RED:Ui.BLUE;Ui.bar(g,x+8,y+36,w-16,12,owner<0?hits/50f:1,color,Ui.darker(color,.35f),owner<0?"Capture · "+hits+" / 50 coups":"Cristal contrôlé par les "+(owner==0?"rouges":"bleus"));
  int danger=t.getIntOr("danger",-1);if(danger>=0){int wy=mc.getWindow().getGuiScaledHeight()/2+30;Ui.rounded(g,x-10,wy,w+20,45,8,0xEB3B101F);Ui.border(g,x-10,wy,w+20,45,Ui.RED);g.centeredText(mc.font,net.minecraft.network.chat.Component.literal("⚠ DRONE ENNEMI · QUITTEZ LA ZONE ROUGE"),x+w/2,wy+7,Ui.RED);Ui.bar(g,x,wy+23,w,12,danger/60f,Ui.RED,0xFF48121C,String.format(java.util.Locale.FRANCE,"Élimination dans %.1f s",danger/20f));}
 }
}
