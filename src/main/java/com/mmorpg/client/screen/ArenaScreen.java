package com.mmorpg.client.screen;

import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.*;
import com.mmorpg.competition.ArenaRules;
import com.mmorpg.network.Payloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.*;

public final class ArenaScreen extends MmoScreen {
    private final CompoundTag data;
    private int refreshTicks;
    @Override public void tick(){super.tick();if(++refreshTicks>=40){refreshTicks=0;action("open","");}}
    public ArenaScreen(CompoundTag data){super("Arènes");this.data=data;}
    @Override protected int designHeight(){return 310;}
    private void action(String action,String kind){var t=new CompoundTag();t.putString("action",action);t.putString("kind",kind);t.putInt("bracket",data.getIntOr("bracket",0));ClientNet.send(new Payloads.SocialAction("arena",t));}
    @Override protected void renderContent(GuiGraphicsExtractor g,int mx,int my,float a){Ui.frame(g,left,top,pw,ph,Ui.GOLD,false);g.centeredText(font,Component.literal("ARÈNES · NIVEAUX "+ArenaRules.range(data.getIntOr("bracket",0))),left+pw/2,top+9,Ui.GOLD);
        int cw=(pw-32)/2;for(var kind:ArenaRules.Kind.values()){int x=left+12+kind.ordinal()*(cw+8),y=top+36,color=kind==ArenaRules.Kind.FEU?0xFFE87936:0xFF65C9DF;Ui.frame(g,x,y,cw,ph-111,color,false);g.text(font,"◆",x+12,y+15,color,false);g.text(font,kind.label,x+35,y+15,color,false);g.text(font,kind.size+" CONTRE "+kind.size,x+10,y+40,Ui.TEXT,false);g.text(font,"Cristal mobile à capturer",x+10,y+61,Ui.TEXT,false);g.text(font,"Inscrits : "+data.getIntOr(kind.name(),0)+" / "+kind.size*2,x+10,y+77,Ui.GOLD,false);g.text(font,"Tu restes sur place.",x+10,y+94,Ui.MUTED,false);g.text(font,"Départ quand complet.",x+10,y+106,Ui.MUTED,false);button(g,x+8,y+ph-131,cw-16,19,kind.name().equals(data.getStringOr("queueKind",""))?"Inscrit":"S’inscrire",!data.getBooleanOr("queued",false),color,()->action("join",kind.name()));}
        g.centeredText(font,Component.literal("500 points · Cristal : 1 / 2 s · Élimination : 10 points"),left+pw/2,top+ph-65,Ui.TEXT);
        g.text(font,font.plainSubstrByWidth(data.getStringOr("status",""),pw-24),left+12,top+ph-46,Ui.GOLD,false);
        button(g,left+12,top+ph-25,110,18,"Actualiser",true,Ui.GOLD,()->action("open",""));button(g,left+pw-142,top+ph-25,130,18,"Quitter l’arène / file",data.getBooleanOr("queued",false),Ui.RED,()->action("leave",""));}
}
