package com.mmorpg.client.replay;

import com.mmorpg.MMORPG;
import com.mmorpg.replay.ReplayTake;
import com.mmorpg.replay.ReplayMp4;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import java.nio.file.*;
import java.util.Locale;
import java.util.concurrent.*;

/** Deterministic PNG sequence: scene time advances only after a rendered image is written. */
@EventBusSubscriber(modid=MMORPG.MODID,value=Dist.CLIENT)
public final class ReplayExporter {
    private static Minecraft mc(){return Minecraft.getInstance();}
    private static final ExecutorService IO=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"Aeloria replay PNG");t.setDaemon(true);return t;});
    public static boolean active;
    public static boolean encoding;
    private static boolean wantMp4;
    private static String encoder;
    private static boolean pending,oldClean,oldPath,oldPause;
    private static int index,total,width,height,cropWidth,cropHeight,job,settle;
    private static double from,rate;
    private static Path output;
    public static ReplayTake.Camera fixedCamera;
    public static String progress(){return index+" / "+total;}
    public static void start(){
        start(false);
    }
    public static void startMp4(){
        if(active||encoding)return;
        encoding=true;ReplayClient.message="Vérification de FFmpeg…";
        IO.execute(()->{
            try{String executable=ReplayMp4.encoder(mc().gameDirectory.toPath());mc().execute(()->{encoding=false;encoder=executable;start(true);});}
            catch(Exception e){mc().execute(()->{encoding=false;ReplayClient.message=e.getMessage();});}
        });
    }
    private static void start(boolean mp4){
        if(active||encoding||!ReplayClient.playing||!ReplayClient.authorized())return;
        wantMp4=mp4;
        double length=Math.min(10,(ReplayClient.take.duration()-ReplayClient.cursor)/ReplayClient.speed);
        if(length<=0){ReplayClient.message="Placez le curseur avant la fin de la prise.";return;}
        try{
            width=mc().gameRenderer.mainRenderTarget().width;height=mc().gameRenderer.mainRenderTarget().height;
            double aspect=ReplayClient.framing==1?9.0/16:ReplayClient.framing==2?1:16.0/9;
            cropWidth=Math.min(width,(int)(height*aspect));cropHeight=Math.min(height,(int)(width/aspect));
            total=(int)Math.ceil(length*60);from=ReplayClient.cursor;rate=ReplayClient.speed;
            output=mc().gameDirectory.toPath().resolve("aeloria-replays/exports/"+System.currentTimeMillis());Files.createDirectories(output);
            if(Files.getFileStore(output).getUsableSpace()<(long)cropWidth*cropHeight*4*total)throw new java.io.IOException("Espace disque insuffisant (estimation prudente pour les PNG)");
            Files.writeString(output.resolve("MONTAGE.txt"),"Aeloria — séquence PNG muette, 60 images/s.\n"+
                    "Importer image-%06d.png comme séquence à 60 fps dans votre logiciel de montage.\n"+
                    "MP4 avec FFmpeg installé :\nffmpeg -framerate 60 -i image-%06d.png -c:v libx264 -crf 18 -pix_fmt yuv420p -vf pad=ceil(iw/2)*2:ceil(ih/2)*2 clip.mp4\n"+
                    "La scène utilise le décor actuel du serveur ; particules/sons et effets non pris en charge ne sont pas archivés.\n");
            oldClean=ReplayClient.clean;oldPath=ReplayClient.pathCamera;oldPause=ReplayClient.paused;
            ReplayClient.clean=true;ReplayClient.pathCamera=true;ReplayClient.paused=true;
            var c=mc().getCameraEntity();fixedCamera=new ReplayTake.Camera(from,c.getX(),c.getEyeY(),c.getZ(),c.getYRot(),c.getXRot(),mc().options.fov().get());
            index=0;pending=false;settle=3;job++;active=true;mc().gui.setScreen(null);
        }catch(Exception e){ReplayClient.message="Export impossible : "+e.getMessage();}
    }
    public static void cancel(){
        if(!active)return;
        active=false;job++;pending=false;fixedCamera=null;
        ReplayClient.clean=oldClean;ReplayClient.pathCamera=oldPath;ReplayClient.paused=oldPause;
    }
    public static void prepare(){
        if(!active)return;
        if(!ReplayClient.playing||!ReplayClient.authorized()||mc().gui.screen()!=null||mc().gameRenderer.mainRenderTarget().width!=width||mc().gameRenderer.mainRenderTarget().height!=height){
            cancel();ReplayClient.message="Export interrompu (écran ouvert, fenêtre modifiée ou session terminée).";return;
        }
        ReplayClient.cursor=from+index/60.0*rate;
    }
    @SubscribeEvent public static void rendered(RenderFrameEvent.Post e){
        if(!active||pending)return;
        // Flush the previous GUI/render-state extraction before sampling a new scene time.
        if(settle-->0)return;
        pending=true;int currentJob=job,frame=index,cw=cropWidth,ch=cropHeight;Path destination=output.resolve(String.format(Locale.ROOT,"image-%06d.png",frame));
        try{Screenshot.takeScreenshot(mc().gameRenderer.mainRenderTarget(),image->{
            IO.execute(()->{
                try(image;var cropped=new NativeImage(cw,ch,false)){
                    image.copyRect(cropped,(image.getWidth()-cw)/2,(image.getHeight()-ch)/2,0,0,cw,ch,false,false);
                    cropped.writeToFile(destination);
                    mc().execute(()->{
                        if(!active||job!=currentJob)return;
                        index++;pending=false;settle=1;
                        if(index>=total){
                            cancel();ReplayClient.paused=true;mc().gui.setScreen(new ReplayScreen());
                            if(wantMp4){
                                encoding=true;ReplayClient.message="Encodage MP4 en cours…";
                                IO.execute(()->{
                                    try{Path result=ReplayMp4.encode(encoder,destination.getParent());mc().execute(()->{encoding=false;ReplayClient.message="Export MP4 terminé : "+result;});}
                                    catch(Exception ex){mc().execute(()->{encoding=false;ReplayClient.message=ex.getMessage();});}
                                });
                            }else ReplayClient.message="Export PNG terminé : "+destination.getParent();
                        }
                    });
                }catch(Exception ex){mc().execute(()->{if(job==currentJob){cancel();ReplayClient.message="Export échoué : "+ex.getMessage();mc().gui.setScreen(new ReplayScreen());}});}
            });
        });}catch(Exception ex){cancel();ReplayClient.message="Capture impossible : "+ex.getMessage();}
    }
}
