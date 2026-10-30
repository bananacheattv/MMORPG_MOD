package com.mmorpg.client.replay;

import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import java.nio.file.*;

public final class ReplayEncoderScreen extends MmoScreen {
    private EditBox path;private String status="Chemin vers ffmpeg.exe (vide : recherche automatique).";
    public ReplayEncoderScreen(){super("Encodeur MP4");}
    @Override protected boolean hasNav(){return false;}
    @Override protected int designHeight(){return 180;}
    @Override protected void init(){
        super.init();String value="";
        try{var p=Minecraft.getInstance().gameDirectory.toPath().resolve("aeloria-replays/ffmpeg-path.txt");if(Files.exists(p))value=Files.readString(p).trim();}catch(Exception ignored){}
        path=new EditBox(font,left+12,top+50,pw-24,20,Component.literal("Chemin FFmpeg"));path.setMaxLength(1024);path.setValue(value);addRenderableWidget(path);
    }
    @Override protected void renderContent(GuiGraphicsExtractor g,int mx,int my,float a){
        Ui.panel(g,left,top,pw,ph);
        int x=left+12,y=top+14,w=pw-24;
        Ui.header(g,x,y,w,"EXPORT MP4 — FFMPEG");
        g.text(font,font.plainSubstrByWidth(status,w),x,y+20,Ui.TEXT,false);
        g.text(font,"Exemple : C:/Outils/ffmpeg/bin/ffmpeg.exe",x,y+67,Ui.MUTED,false);
        button(g,x,y+92,w,22,"Enregistrer le chemin",true,Ui.GOLD,()->{
            try{Path p=Minecraft.getInstance().gameDirectory.toPath().resolve("aeloria-replays/ffmpeg-path.txt");Files.createDirectories(p.getParent());Files.writeString(p,path.getValue().trim());Minecraft.getInstance().gui.setScreen(new ReplayScreen());}
            catch(Exception e){status=e.getMessage();}
        });
        button(g,x,y+120,w,20,"Retour",true,Ui.GOLD,()->Minecraft.getInstance().gui.setScreen(new ReplayScreen()));
    }
}
