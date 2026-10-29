package com.mmorpg.client.screen;

import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.*;
import com.mmorpg.network.Payloads;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class ArenaReadyScreen extends MmoScreen {
    private final CompoundTag data;private final long end=System.currentTimeMillis()+30000;
    public ArenaReadyScreen(CompoundTag data){super("Arène prête");this.data=data;}
    @Override protected boolean compact(){return true;}@Override protected int panelWidth(){return Math.min(width-12,440);}@Override protected int panelHeight(){return 156;}
    private void action(String action){var t=new CompoundTag();t.putString("action",action);t.putString("match",data.getStringOr("match",""));ClientNet.send(new Payloads.SocialAction("arena",t));net.minecraft.client.Minecraft.getInstance().gui.setScreen(null);}
    @Override protected void renderContent(GuiGraphicsExtractor g,int mx,int my,float a){Ui.frame(g,left,top,pw,ph,Ui.GOLD,false);g.centeredText(font,Component.literal("L’ARÈNE EST PRÊTE !"),left+pw/2,top+15,Ui.GOLD);g.centeredText(font,Component.literal(data.getStringOr("arena","")),left+pw/2,top+42,Ui.TEXT);long seconds=Math.max(0,(end-System.currentTimeMillis())/1000);g.centeredText(font,Component.literal("Confirme ton entrée : "+seconds+" s"),left+pw/2,top+66,Ui.MUTED);button(g,left+12,top+94,pw-24,21,"Rentrer dans l’arène",seconds>0,Ui.GREEN,()->action("ready"));button(g,left+12,top+122,pw-24,18,"Décliner",true,Ui.RED,()->action("leave"));}
}
