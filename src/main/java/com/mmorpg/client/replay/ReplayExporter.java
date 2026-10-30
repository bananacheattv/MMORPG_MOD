package com.mmorpg.client.replay;

import com.mmorpg.MMORPG;
import com.mmorpg.replay.*;
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

/** One in-flight frame. MP4 consumes pixels directly; PNG remains an explicit separate export. */
@EventBusSubscriber(modid=MMORPG.MODID,value=Dist.CLIENT)
public final class ReplayExporter {
    private static Minecraft mc(){return Minecraft.getInstance();}
    private static final ExecutorService IO=Executors.newSingleThreadExecutor(r->{var t=new Thread(r,"Aeloria replay export");t.setDaemon(true);return t;});
    public static boolean active,encoding;
    public static int durationSeconds=300;
    private static String encoder;
    private static boolean pending,oldClean,oldPath,oldPause;
    private static int index,total,width,height,cropWidth,cropHeight,settle;
    private static int job;
    private static double from,rate;
    private static Path output;
    private static ReplayMp4.StreamEncoder stream;
    public static ReplayTake.Camera fixedCamera;
    public static String progress(){return index+" / "+total;}
    public static String durationLabel(){return durationSeconds<60?durationSeconds+" s":durationSeconds/60+" min";}
    public static void nextDuration(){durationSeconds=switch(durationSeconds){case 10->30;case 30->60;case 60->120;case 120->300;default->10;};}
    public static void start(){start(false);}
    public static void startMp4(){
        if(active||encoding||!ReplayClient.playing)return;
        int checkJob=++job;encoding=true;ReplayClient.message="Vérification de FFmpeg…";
        IO.execute(()->{
            try{String executable=ReplayMp4.encoder(mc().gameDirectory.toPath());mc().execute(()->{if(job!=checkJob)return;encoding=false;encoder=executable;start(true);});}
            catch(Exception e){mc().execute(()->{if(job==checkJob){encoding=false;ReplayClient.message=e.getMessage();}});}
        });
    }
    private static void start(boolean mp4){
        if(active||encoding||!ReplayClient.playing||!ReplayClient.authorized())return;
        try{
            total=ReplayExportPlan.frames(ReplayClient.take.duration(),ReplayClient.cursor,ReplayClient.speed,durationSeconds);
            if(total==0){ReplayClient.message="Placez le curseur avant la fin de la prise.";return;}
            width=mc().gameRenderer.mainRenderTarget().width;height=mc().gameRenderer.mainRenderTarget().height;
            double aspect=ReplayClient.framing==1?9.0/16:ReplayClient.framing==2?1:16.0/9;
            cropWidth=Math.min(width,(int)(height*aspect));cropHeight=Math.min(height,(int)(width/aspect));
            from=ReplayClient.cursor;rate=ReplayClient.speed;
            output=mc().gameDirectory.toPath().resolve("aeloria-replays/exports/"+System.currentTimeMillis());Files.createDirectories(output);
            if(Files.getFileStore(output).getUsableSpace()<ReplayExportPlan.diskBudget(cropWidth,cropHeight,total,mp4))throw new java.io.IOException("Espace disque insuffisant pour la durée choisie");
            Files.writeString(output.resolve("MONTAGE.txt"),"Aeloria — export muet, 60 images/s. Durée demandée : "+durationLabel()+".\n"+
                    (mp4?"MP4 encodé directement, sans PNG intermédiaires. Résultat : clip.mp4.\n":"Importer image-%06d.png comme séquence à 60 fps.\n")+
                    "Décor actuel du serveur ; sons, particules et certains effets ne sont pas archivés. La prise originale est conservée si l'export échoue.\n");
            stream=mp4?new ReplayMp4.StreamEncoder(encoder,output,cropWidth,cropHeight):null;
            oldClean=ReplayClient.clean;oldPath=ReplayClient.pathCamera;oldPause=ReplayClient.paused;
            ReplayClient.clean=true;ReplayClient.pathCamera=true;ReplayClient.paused=true;
            var c=mc().getCameraEntity();fixedCamera=new ReplayTake.Camera(from,c.getX(),c.getEyeY(),c.getZ(),c.getYRot(),c.getXRot(),mc().options.fov().get());
            index=0;pending=false;settle=3;job++;active=true;mc().gui.setScreen(null);
        }catch(Exception e){if(stream!=null){stream.abort();stream=null;}ReplayClient.message="Export impossible : "+e.getMessage();}
    }
    private static void endCapture(){
        active=false;pending=false;fixedCamera=null;
        ReplayClient.clean=oldClean;ReplayClient.pathCamera=oldPath;ReplayClient.paused=oldPause;
    }
    public static void cancel(){
        // Finalizing: every frame already reached FFmpeg, so leaving playback must not destroy the clip.
        if(!active&&stream!=null)return;
        job++;if(stream!=null){stream.abort();stream=null;}encoding=false;
        if(active)endCapture();
    }
    public static void prepare(){
        if(!active)return;
        if(!ReplayClient.playing||!ReplayClient.authorized()||mc().gui.screen()!=null||mc().gameRenderer.mainRenderTarget().width!=width||mc().gameRenderer.mainRenderTarget().height!=height){
            cancel();ReplayClient.message="Export interrompu (écran ouvert, fenêtre modifiée ou session terminée).";return;
        }
        ReplayClient.cursor=from+index/(double)ReplayExportPlan.FPS*rate;
    }
    @SubscribeEvent public static void rendered(RenderFrameEvent.Post e){
        if(!active||pending||settle-->0)return;
        pending=true;int currentJob=job,frame=index,cw=cropWidth,ch=cropHeight;
        Path directory=output,destination=directory.resolve(String.format(Locale.ROOT,"image-%06d.png",frame));
        ReplayMp4.StreamEncoder sink=stream;
        try{Screenshot.takeScreenshot(mc().gameRenderer.mainRenderTarget(),image->{
            IO.execute(()->{
                try(image;var cropped=new NativeImage(cw,ch,false)){
                    if(frame%60==0&&Files.getFileStore(directory).getUsableSpace()<32L*1024*1024)throw new java.io.IOException("Export arrêté : disque presque plein");
                    image.copyRect(cropped,(image.getWidth()-cw)/2,(image.getHeight()-ch)/2,0,0,cw,ch,false,false);
                    if(sink==null)cropped.writeToFile(destination);else sink.write(cropped.getPixelBytes());
                    mc().execute(()->{
                        if(!active||job!=currentJob)return;
                        index++;pending=false;settle=1;
                        if(index>=total){
                            endCapture();ReplayClient.paused=true;mc().gui.setScreen(new ReplayScreen());
                            if(sink==null){ReplayClient.message="Export PNG terminé : "+directory;return;}
                            encoding=true;ReplayClient.message="Finalisation du MP4…";
                            IO.execute(()->{
                                try(sink){Path result=sink.finish();mc().execute(()->{if(job==currentJob){stream=null;encoding=false;ReplayClient.message="Export MP4 terminé : "+result;}});}
                                catch(Exception ex){mc().execute(()->{if(job==currentJob){stream=null;encoding=false;ReplayClient.message=ex.getMessage();}});}
                            });
                        }
                    });
                }catch(Exception ex){if(sink!=null)sink.abort();mc().execute(()->{if(job==currentJob){cancel();ReplayClient.message="Export échoué : "+ex.getMessage();mc().gui.setScreen(new ReplayScreen());}});}
            });
        });}catch(Exception ex){cancel();ReplayClient.message="Capture impossible : "+ex.getMessage();}
    }
}
