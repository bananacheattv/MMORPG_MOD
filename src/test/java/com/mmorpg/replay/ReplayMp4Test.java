package com.mmorpg.replay;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class ReplayMp4Test {
    @TempDir Path dir;
    @Test void configuredPathWithShellCharactersIsNotExecutedAsAShellCommand()throws Exception{
        Files.createDirectories(dir.resolve("aeloria-replays"));
        Files.writeString(dir.resolve("aeloria-replays/ffmpeg-path.txt"),"missing-encoder & echo should-not-run");
        assertThrows(java.io.IOException.class,()->ReplayMp4.encoder(dir));
    }
    @Test void actualEncoderProducesMp4AndRetainsFrames()throws Exception{
        // Optional integration dependency; native replay QA also exercises this with a provisioned encoder.
        assumeTrue(Files.isExecutable(Path.of("run/aeloria-replays/ffmpeg.exe")));
        String encoder=ReplayMp4.encoder(Path.of("run"));
        for(int f=0;f<6;f++){
            var im=new BufferedImage(33,19,BufferedImage.TYPE_INT_RGB);
            for(int y=0;y<19;y++)for(int x=0;x<33;x++)im.setRGB(x,y,(f*40)<<16|x*7<<8|y*10);
            ImageIO.write(im,"png",dir.resolve(String.format("image-%06d.png",f)).toFile());
        }
        Path mp4=ReplayMp4.encode(encoder,dir);byte[] b=Files.readAllBytes(mp4);
        assertTrue(b.length>32);assertEquals("ftyp",new String(b,4,4,java.nio.charset.StandardCharsets.US_ASCII));
        assertTrue(Files.exists(dir.resolve("image-000005.png")));assertFalse(Files.exists(dir.resolve("clip-encoding.mp4")));
    }
}
