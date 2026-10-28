package com.mmorpg.client;
import com.mmorpg.client.ui.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
public final class DevRankGallery extends MmoScreen {
 public DevRankGallery(){super("Grades");}
 @Override protected int panelWidth(){return Math.min(width-12,540);}@Override protected int panelHeight(){return Math.min(height-12,300);}
 @Override protected void renderContent(GuiGraphicsExtractor g,int mx,int my,float a){Ui.panel(g,left,top,pw,ph);Ui.title(g,left+pw/2,top+12,"IDENTITÉ DES AVENTURIERS",Ui.GOLD);String[] labels={"Aelorien","Éclaireur","Gardien","Arcaniste","Élu","Fondateur","Administrateur","Modérateur","Builder"};int[] colors={0xC3B4DF,0x74CF96,0x79C9F2,0xBA8CFF,0xF4CA69,0xFF5555,0xFF5555,0x55AFFF,0x58DE87};for(int i=0;i<9;i++){int y=top+40+i*25;Ui.inset(g,left+12,y-4,pw-24,21);g.text(font,com.mmorpg.store.RankTags.name(labels[i],colors[i],"1Touchable").copy().append(net.minecraft.network.chat.Component.literal("  ›  Bonjour Aeloria !").withColor(Ui.TEXT)),left+20,y,Ui.TEXT,false);}}
}
