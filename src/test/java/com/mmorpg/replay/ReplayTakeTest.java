package com.mmorpg.replay;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.GZIPOutputStream;
import static org.junit.jupiter.api.Assertions.*;

class ReplayTakeTest {
    @TempDir Path dir;
    private ReplayTake scene(){
        var t=new ReplayTake();t.name="prise";t.server="test:25565";t.dimension="minecraft:overworld";t.registry="r1";
        t.actors.add(new ReplayTake.Actor("minecraft:zombie",UUID.randomUUID(),"Acteur",new byte[]{1,2,3}));
        t.states.add(new ReplayTake.State(new byte[]{(byte)255},new byte[]{4}));
        for(int i=0;i<21;i++)t.frames.add(List.of(new ReplayTake.Pose(0,0,i,80,0,179,-10,0,0,i,.5f,0,0,0,i)));
        t.cameras.add(new ReplayTake.Camera(0,0,82,0,179,0,70));t.cameras.add(new ReplayTake.Camera(1,20,82,0,-179,0,50));
        return t;
    }
    @Test void takeSurvivesSaveReopenAndCameraEdit()throws Exception{
        var t=scene();Path file=dir.resolve("take.aereplay");t.write(file);
        var loaded=ReplayTake.read(file);assertEquals(1,loaded.duration());assertEquals(t.actors.getFirst().uuid(),loaded.actors.getFirst().uuid());
        assertEquals(t.frames.get(10),loaded.frames.get(10));assertArrayEquals(t.states.getFirst().synced(),loaded.states.getFirst().synced());
        loaded.cameras.removeLast();loaded.write(file);assertEquals(1,ReplayTake.read(file).cameras.size());
        assertFalse(Files.exists(dir.resolve("take.aereplay.part")));
    }
    @Test void yawTakesShortRouteAcross180AndClampsOutsideKeys(){
        var t=scene();var a=t.cameras.getFirst();var b=t.cameras.getLast();
        assertEquals(180,ReplayTake.interpolate(a,b,.5).yaw(),.001);
        assertEquals(0,ReplayTake.interpolate(a,b,-2).x(),.001);assertEquals(20,ReplayTake.interpolate(a,b,4).x(),.001);
        assertTrue(Double.isFinite(ReplayTake.interpolate(a,a,0).x()));
    }
    @Test void rejectsDanglingActorReferenceInsteadOfSpawningArbitraryEntity()throws Exception{
        var t=scene();t.actors.clear();Path p=dir.resolve("bad.aereplay");t.write(p);assertThrows(IOException.class,()->ReplayTake.read(p));
    }
    @Test void rejectsNaNAndUnsortedCameraKeys()throws Exception{
        var t=scene();t.cameras.set(0,new ReplayTake.Camera(Double.NaN,0,0,0,0,0,70));Path p=dir.resolve("nan.aereplay");t.write(p);assertThrows(IOException.class,()->ReplayTake.read(p));
        t=scene();Collections.reverse(t.cameras);Path q=dir.resolve("reverse.aereplay");t.write(q);assertThrows(IOException.class,()->ReplayTake.read(q));
    }
    @Test void rejectsHugeCollectionCountBeforeAllocation()throws Exception{
        Path p=dir.resolve("huge.aereplay");try(var out=new DataOutputStream(new GZIPOutputStream(Files.newOutputStream(p)))){
            out.writeInt(0x41455250);out.writeInt(1);for(int i=0;i<4;i++)out.writeUTF("");out.writeInt(Integer.MAX_VALUE);
        }assertThrows(IOException.class,()->ReplayTake.read(p));
    }
    @Test void rejectsTruncatedArchive()throws Exception{
        Path p=dir.resolve("short.aereplay");scene().write(p);byte[] bytes=Files.readAllBytes(p);Files.write(p,Arrays.copyOf(bytes,bytes.length/2));assertThrows(IOException.class,()->ReplayTake.read(p));
    }
    @Test void exactFiveMinuteTakeCanBeSavedAndReopened()throws Exception{
        var t=scene();var pose=t.frames.getFirst();t.frames.clear();
        for(int i=0;i<ReplayTake.MAX_FRAMES;i++)t.frames.add(pose);
        assertEquals(300,t.duration());Path p=dir.resolve("five-minutes.aereplay");t.write(p);
        assertEquals(6001,ReplayTake.read(p).frames.size());assertEquals(300,ReplayTake.read(p).duration());
    }
}
