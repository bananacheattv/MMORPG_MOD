package com.mmorpg.replay;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;

/** External encoder, no shell command interpolation, no network downloads, no bundled executable. */
public final class ReplayMp4 {
    /** Streams one RGBA frame at a time; no unbounded queue and no PNG intermediate files. */
    public static final class StreamEncoder implements AutoCloseable {
        private final Process process;
        private final WritableByteChannel input;
        private final Path part,target;
        private final long frameBytes;
        private volatile boolean finished;
        private int frames;
        public StreamEncoder(String encoder,Path directory,int width,int height)throws IOException{
            if(width<1||height<1||width>8192||height>8192)throw new IOException("Dimensions vidéo non prises en charge");
            Files.createDirectories(directory);part=directory.resolve("clip-encoding.mp4");target=directory.resolve("clip.mp4");
            frameBytes=(long)width*height*4;
            var command=List.of(encoder,"-nostdin","-hide_banner","-y","-f","rawvideo","-pixel_format","rgba","-video_size",width+"x"+height,"-framerate","60","-i","pipe:0",
                    "-an","-c:v","libx264","-preset","fast","-crf","18","-pix_fmt","yuv420p","-vf","pad=ceil(iw/2)*2:ceil(ih/2)*2","-movflags","+faststart",part.toAbsolutePath().toString());
            process=new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(directory.resolve("encoding.log").toFile()).start();
            input=Channels.newChannel(process.getOutputStream());
        }
        public void write(ByteBuffer pixels)throws IOException{
            if(finished||!process.isAlive())throw new IOException("Encodeur arrêté ; voir encoding.log");
            if(pixels.remaining()!=frameBytes)throw new IOException("Taille de l'image vidéo invalide");
            while(pixels.hasRemaining())input.write(pixels);
            frames++;
        }
        public Path finish()throws IOException{
            if(finished)throw new IOException("Export déjà finalisé");
            input.close();
            try{
                if(!process.waitFor(10,TimeUnit.MINUTES)){abort();throw new IOException("Finalisation MP4 trop longue");}
                if(process.exitValue()!=0||frames==0||!Files.isRegularFile(part)||Files.size(part)<32)throw new IOException("Encodage échoué ; voir encoding.log. La prise reste disponible.");
                Files.move(part,target,StandardCopyOption.REPLACE_EXISTING);finished=true;return target;
            }catch(InterruptedException e){abort();Thread.currentThread().interrupt();throw new IOException("Encodage interrompu",e);}
        }
        /** Can be invoked by the render thread without waiting for a blocked encoder write. */
        public void abort(){if(!finished){process.destroyForcibly();process.onExit().thenRunAsync(()->{try{input.close();}catch(IOException ignored){}});}}
        @Override public void close(){if(!finished)abort();try{input.close();}catch(IOException ignored){}}
    }
    public static String encoder(Path game)throws IOException{
        String configured=System.getProperty("aeloria.replay.ffmpeg","").trim();
        Path settings=game.resolve("aeloria-replays/ffmpeg-path.txt");
        if(configured.isEmpty()&&Files.exists(settings))configured=Files.readString(settings).trim();
        if(configured.isEmpty()){
            Path local=game.resolve("aeloria-replays/"+(System.getProperty("os.name").startsWith("Windows")?"ffmpeg.exe":"ffmpeg"));
            configured=Files.isRegularFile(local)?local.toAbsolutePath().toString():"ffmpeg";
        }
        try{
            var p=new ProcessBuilder(configured,"-version").redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
            if(!p.waitFor(5,TimeUnit.SECONDS)){p.destroyForcibly();throw new IOException("FFmpeg ne répond pas");}
            if(p.exitValue()!=0)throw new IOException("FFmpeg non valide");return configured;
        }catch(InterruptedException e){Thread.currentThread().interrupt();throw new IOException("Vérification FFmpeg interrompue",e);}
        catch(IOException e){throw new IOException("FFmpeg introuvable ou invalide : utilisez le bouton Configurer FFmpeg",e);}
    }
    public static Path encode(String encoder,Path directory)throws IOException{
        Path part=directory.resolve("clip-encoding.mp4"),target=directory.resolve("clip.mp4");
        var command=List.of(encoder,"-nostdin","-hide_banner","-y","-framerate","60","-start_number","0","-i",directory.resolve("image-%06d.png").toAbsolutePath().toString(),
                "-an","-c:v","libx264","-preset","medium","-crf","18","-pix_fmt","yuv420p","-vf","pad=ceil(iw/2)*2:ceil(ih/2)*2","-movflags","+faststart",part.toAbsolutePath().toString());
        Process p=new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(directory.resolve("encoding.log").toFile()).start();
        try{
            if(!p.waitFor(5,TimeUnit.MINUTES)){p.destroyForcibly();throw new IOException("Délai d'encodage dépassé ; images PNG conservées");}
            if(p.exitValue()!=0||!Files.isRegularFile(part)||Files.size(part)<32)throw new IOException("Encodage MP4 échoué ; voir encoding.log. Images PNG conservées.");
            Files.move(part,target,StandardCopyOption.REPLACE_EXISTING);return target;
        }catch(InterruptedException e){p.destroyForcibly();Thread.currentThread().interrupt();throw new IOException("Encodage interrompu ; PNG conservés",e);}
    }
}
