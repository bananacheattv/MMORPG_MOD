package com.mmorpg.client.replay;

import com.mmorpg.MMORPG;
import com.mmorpg.replay.ReplayTake;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.nio.file.*;

/** Native smoke test, opt-in only. No desktop input automation; screenshots use Minecraft's API. */
@EventBusSubscriber(modid=MMORPG.MODID,value=Dist.CLIENT)
public final class DevReplayClientChecks {
    private static int ticks,stage,wait;private static boolean done;
    private static net.minecraft.world.phys.Vec3 returnPosition;
    private static void check(boolean value,String message){if(!value)throw new AssertionError(message);}
    @SubscribeEvent public static void tick(ClientTickEvent.Post e){
        if(!Boolean.getBoolean("mmorpg.replayClientChecks")||done)return;
        var mc=Minecraft.getInstance();if(mc.player==null||mc.level==null)return;
        try{
            if(++ticks>2400)throw new AssertionError("Replay client smoke test timeout, stage "+stage+" "+ReplayClient.message);
            switch(stage){
                case 0 -> {if(ticks<40)break;mc.getWindow().setWindowed(960,600);mc.gui.setScreen(null);ReplayClient.request("record");stage=1;}
                case 1 -> {if(!ReplayClient.recording||ReplayClient.take.frames.size()<30)break;ReplayClient.stop(true);wait=ticks;stage=2;}
                case 2 -> {if(ticks-wait<40)break;var files=ReplayClient.files();check(!files.isEmpty(),"saved recording exists");var reopened=ReplayTake.read(files.getFirst());check(reopened.frames.size()>=30,"take reopened");check(!reopened.actors.isEmpty(),"actors captured");ReplayClient.take=reopened;returnPosition=mc.player.position();ReplayClient.request("play");stage=3;}
                case 3 -> {if(!ReplayClient.playing)break;
                    var p=ReplayClient.take.frames.getFirst().getFirst();
                    ReplayClient.take.cameras.add(new ReplayTake.Camera(0,p.x(),p.y()+2,p.z()-5,0,10,70));
                    ReplayClient.take.cameras.add(new ReplayTake.Camera(ReplayClient.take.duration(),p.x()+1,p.y()+2,p.z()-5,0,10,70));
                    ReplayClient.pathCamera=true;ReplayClient.seek(.5);wait=ticks;stage=4;}
                case 4 -> {if(ticks-wait<12)break;check(ReplayClient.ghostCount()>0,"client ghosts spawned");Screenshot.grab(mc.gameDirectory,"replay-controls.png",mc.gameRenderer.mainRenderTarget(),1,c->{});ReplayClient.seek(0);ReplayClient.framing=1;ReplayExporter.startMp4();check(ReplayExporter.encoding,"MP4 export started");stage=5;}
                case 5 -> {if(ReplayExporter.active||ReplayExporter.encoding)break;check(ReplayClient.message.startsWith("Export MP4 terminé"),ReplayClient.message);mc.getConnection().sendCommand("deop ReplayChecks");wait=ticks;stage=6;}
                case 6 -> {
                    if(ReplayClient.playing||mc.player.isSpectator()){check(ticks-wait<100,"revocation restoration timeout");break;}
                    check(ReplayClient.ghostCount()==0,"ghosts cleaned up on revocation");check(!mc.player.isCreative(),"revocation falls back to survival");check(mc.player.position().distanceToSqr(returnPosition)<1,"original position restored");
                    Files.createDirectories(Path.of("../build/reports"));Files.writeString(Path.of("../build/reports/replay-client-checks.txt"),"PASS: live permission channel, record, save/reopen, spectator playback, ghost render, seek, camera path, real MP4 export, OP revocation cleanup, survival fallback and position restoration\n");MMORPG.LOGGER.info("[REPLAY CHECKS] CLIENT PASS");done=true;mc.stop();}
            }
        }catch(Throwable ex){done=true;MMORPG.LOGGER.error("[REPLAY CHECKS] CLIENT FAIL",ex);try{Files.writeString(Path.of("../build/reports/replay-client-checks.txt"),"FAIL: "+ex);}catch(Exception ignored){}ReplayClient.stop(true);mc.stop();}
    }
}
