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
    private static int ticks,stage,wait;private static boolean done,exportStarted,finalizing;
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
                    // Key mappings do not fire while a screen is open: the workshop handles F8 itself.
                    check(mc.gui.screen() instanceof ReplayScreen,"workshop opened for playback");
                    mc.gui.screen().keyPressed(new net.minecraft.client.input.KeyEvent(com.mojang.blaze3d.platform.InputConstants.KEY_F8,0,0));
                    check(mc.gui.screen()==null,"F8 closes the workshop");
                    var p=ReplayClient.take.frames.getFirst().getFirst();
                    ReplayClient.take.cameras.add(new ReplayTake.Camera(0,p.x(),p.y()+2,p.z()-5,0,10,70));
                    ReplayClient.take.cameras.add(new ReplayTake.Camera(ReplayClient.take.duration(),p.x()+1,p.y()+2,p.z()-5,0,10,70));
                    ReplayClient.pathCamera=true;ReplayClient.seek(.5);wait=ticks;stage=4;}
                case 4 -> {if(ticks-wait<12)break;check(ReplayClient.ghostCount()>0,"client ghosts spawned");Screenshot.grab(mc.gameDirectory,"replay-controls.png",mc.gameRenderer.mainRenderTarget(),1,c->{});ReplayClient.seek(0);ReplayClient.framing=1;ReplayExporter.startMp4();check(ReplayExporter.encoding,"MP4 export started");stage=5;}
                // Leave playback as soon as the last frame is captured: the MP4 finalization must survive it.
                case 5 -> {if(ReplayExporter.active){exportStarted=true;break;}if(!exportStarted){check(ReplayExporter.encoding,"MP4 export failed: "+ReplayClient.message);break;}
                    finalizing=ReplayExporter.encoding;MMORPG.LOGGER.info("[REPLAY CHECKS] playback left during MP4 finalization: {}",finalizing);ReplayClient.stop(true);stage=6;}
                case 6 -> {if(ReplayExporter.encoding||mc.player.isSpectator())break;
                    check(!ReplayClient.playing,"playback stopped");check(ReplayClient.message.startsWith("Export MP4 terminé"),"clip kept after leaving playback: "+ReplayClient.message);
                    check(Files.size(Path.of(ReplayClient.message.substring(ReplayClient.message.indexOf(':')+1).trim()))>32,"clip.mp4 written");
                    returnPosition=mc.player.position();ReplayClient.request("play");stage=7;}
                // "/replay record" during playback: the server leaves spectator, so the client must stop playing too.
                case 7 -> {if(!ReplayClient.playing){wait=ticks;break;}if(ticks-wait<10)break;ReplayClient.request("record");stage=8;}
                case 8 -> {if(!ReplayClient.recording)break;check(!ReplayClient.playing&&ReplayClient.ghostCount()==0,"recording ends client playback");stage=9;}
                case 9 -> {if(mc.player.isSpectator()||ReplayClient.take.frames.size()<10)break;check(mc.player.position().distanceToSqr(returnPosition)<1,"position restored before recording");ReplayClient.stop(true);wait=ticks;stage=10;}
                case 10 -> {if(ticks-wait<40)break;returnPosition=mc.player.position();ReplayClient.request("play");stage=11;}
                case 11 -> {if(!ReplayClient.playing)break;mc.getConnection().sendCommand("deop ReplayChecks");wait=ticks;stage=12;}
                case 12 -> {
                    if(ReplayClient.playing||mc.player.isSpectator()){check(ticks-wait<100,"revocation restoration timeout");break;}
                    check(ReplayClient.ghostCount()==0,"ghosts cleaned up on revocation");check(!mc.player.isCreative(),"revocation falls back to survival");check(mc.player.position().distanceToSqr(returnPosition)<1,"original position restored");
                    Files.createDirectories(Path.of("../build/reports"));Files.writeString(Path.of("../build/reports/replay-client-checks.txt"),"PASS: live permission channel, record, save/reopen, spectator playback, F8 close, ghost render, seek, camera path, real MP4 export, clip kept when leaving playback (during finalization: "+finalizing+"), record during playback, OP revocation cleanup, survival fallback and position restoration\n");MMORPG.LOGGER.info("[REPLAY CHECKS] CLIENT PASS");done=true;mc.stop();}
            }
        }catch(Throwable ex){done=true;MMORPG.LOGGER.error("[REPLAY CHECKS] CLIENT FAIL",ex);try{Files.writeString(Path.of("../build/reports/replay-client-checks.txt"),"FAIL: "+ex);}catch(Exception ignored){}ReplayClient.stop(true);mc.stop();}
    }
}
