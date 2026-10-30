package com.mmorpg.client.replay;

import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.nio.file.Path;
import java.util.*;

/** Separate takes page keeps controls usable at a 300-unit GUI height. */
public final class ReplayScreen extends MmoScreen {
    private List<Path> files=ReplayClient.files();
    private int page;private boolean library;
    public ReplayScreen(){super("Atelier Replay — administrateur");}
    @Override protected boolean hasNav(){return false;}
    @Override protected int designHeight(){return 272;}
    @Override protected void renderContent(GuiGraphicsExtractor g,int mx,int my,float partial){
        Ui.panel(g,left,top,pw,ph);
        int x=left+12,y=top+10,w=pw-24,col=(w-12)/3;
        boolean allowed=ReplayClient.authorized(),rec=ReplayClient.recording,playing=ReplayClient.playing;
        Ui.header(g,x,y,w,library?"PRISES SAUVEGARDÉES":"AELORIA — PRISES ET CAMÉRA");y+=18;
        g.text(font,font.plainSubstrByWidth(ReplayClient.message,w),x,y,Ui.GOLD_LIGHT,false);y+=16;
        if(library){
            int rows=Math.max(1,(ph-94)/24);page=Math.min(page,Math.max(0,(files.size()-1)/rows));
            if(files.isEmpty())g.text(font,"Aucune prise : commencez par Enregistrer.",x,y+8,Ui.MUTED,false);
            for(int i=0;i<rows;i++){
                int index=page*rows+i;if(index>=files.size())break;Path file=files.get(index);
                button(g,x,y,w,20,font.plainSubstrByWidth(file.getFileName().toString(),w-12),allowed&&!rec&&!playing,Ui.GOLD,()->ReplayClient.load(file));y+=24;
            }
            int bottom=top+ph-32;
            button(g,x,bottom,60,20,"Précédent",page>0,Ui.GOLD,()->page--);
            button(g,x+66,bottom,60,20,"Suivant",(page+1)*rows<files.size(),Ui.GOLD,()->page++);
            button(g,x+w-180,bottom,86,20,"Actualiser",true,Ui.GOLD,()->files=ReplayClient.files());
            button(g,x+w-88,bottom,88,20,"Caméra",true,Ui.GOLD,()->library=false);return;
        }
        button(g,x,y,col,19,rec?"REC EN COURS":"Enregistrer",allowed&&!rec&&!playing,Ui.RED,()->ReplayClient.request("record"));
        button(g,x+col+6,y,col,19,"Revoir la prise",allowed&&!rec&&!playing&&ReplayClient.take!=null,Ui.GOLD,()->ReplayClient.request("play"));
        button(g,x+2*(col+6),y,col,19,"Arrêter / quitter",rec||playing,Ui.RED,()->ReplayClient.stop(true));y+=24;
        double duration=ReplayClient.take==null?0:ReplayClient.take.duration();
        g.text(font,String.format(Locale.ROOT,"%.1f / %.1f s — vitesse ×%.2f",ReplayClient.cursor,duration,ReplayClient.speed),x,y,Ui.TEXT,false);y+=13;
        int barY=y;g.fill(x,barY,x+w,barY+8,0xFF171125);int filled=duration>0?(int)(w*ReplayClient.cursor/duration):0;g.fill(x,barY,x+filled,barY+8,0xFFAE79EA);
        if(playing&&duration>0)click(x,barY,w,8,()->ReplayClient.seek(duration*Math.max(0,Math.min(1,(mouseX-x)/(double)w))),null);y+=13;
        int quarter=(w-18)/4;
        button(g,x,y,quarter,19,"−5 s",playing,Ui.GOLD,()->ReplayClient.seek(ReplayClient.cursor-5));
        button(g,x+quarter+6,y,quarter,19,ReplayClient.paused?"Lecture":"Pause",playing,Ui.GOLD,()->ReplayClient.paused=!ReplayClient.paused);
        button(g,x+2*(quarter+6),y,quarter,19,"+5 s",playing,Ui.GOLD,()->ReplayClient.seek(ReplayClient.cursor+5));
        button(g,x+3*(quarter+6),y,quarter,19,"Vitesse",playing,Ui.GOLD,()->ReplayClient.speed=ReplayClient.speed==.25?.5:ReplayClient.speed==.5?1:ReplayClient.speed==1?2:.25);y+=24;
        button(g,x,y,col,19,"Point caméra",playing,Ui.GOLD,ReplayClient::keyframe);
        button(g,x+col+6,y,col,19,ReplayClient.pathCamera?"Trajectoire : oui":"Trajectoire : non",playing,Ui.GOLD,()->ReplayClient.pathCamera=!ReplayClient.pathCamera);
        button(g,x+2*(col+6),y,col,19,"Sauver les points",playing,Ui.GOLD,ReplayClient::save);y+=24;
        button(g,x,y,col,19,"Caméra libre [F8]",playing,Ui.GOLD,()->Minecraft.getInstance().gui.setScreen(null));
        button(g,x+col+6,y,col,19,ReplayClient.clean?"HUD : masqué":"HUD : visible",playing,Ui.GOLD,()->ReplayClient.clean=!ReplayClient.clean);
        button(g,x+2*(col+6),y,col,19,"Cadre : "+new String[]{"16:9","9:16","carré"}[ReplayClient.framing],playing,Ui.GOLD,()->ReplayClient.framing=(ReplayClient.framing+1)%3);y+=24;
        button(g,x,y,w,19,"Exporter PNG · 10 s · 60 fps",playing&&!ReplayExporter.encoding,Ui.GOLD,ReplayExporter::start);y+=24;
        button(g,x,y,w-128,19,ReplayExporter.encoding?"Encodage MP4…":"Exporter MP4 · 10 s · 60 fps",playing&&!ReplayExporter.encoding,Ui.GOLD,ReplayExporter::startMp4);
        button(g,x+w-122,y,122,19,"Configurer FFmpeg",!ReplayExporter.encoding,Ui.GOLD,()->Minecraft.getInstance().gui.setScreen(new ReplayEncoderScreen()));
        button(g,x,top+ph-30,w,19,"Prises sauvegardées",true,Ui.GOLD,()->{files=ReplayClient.files();library=true;});
    }
}
