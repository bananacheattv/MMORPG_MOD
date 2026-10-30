package com.mmorpg.replay;

import com.mmorpg.MMORPG;
import com.mojang.authlib.GameProfile;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.util.UUID;
import java.nio.file.*;

/** Explicit opt-in integration checks, run only in the isolated development server. */
@EventBusSubscriber(modid=MMORPG.MODID)
public final class DevReplayChecks {
    private static void check(boolean b,String reason){if(!b)throw new AssertionError(reason);}
    @SubscribeEvent public static void started(ServerStartedEvent e)throws Exception{
        if(!Boolean.getBoolean("mmorpg.replayChecks"))return;
        var server=e.getServer();
        var p=net.neoforged.neoforge.common.util.FakePlayerFactory.get(server.overworld(),new GameProfile(UUID.randomUUID(),"ReplaySecurity"));
        try{
            server.getPlayerList().deop(p.nameAndId());
            p.setGameMode(GameType.CREATIVE);
            check(!ReplayServer.allowed(p),"Creative does not grant replay access");
            var initialMode=p.gameMode.getGameModeForPlayer();
            ReplayServer.action(p,"play");check(p.gameMode.getGameModeForPlayer()==initialMode,"Forged request denied");
            check(!p.getPersistentData().contains("aeloria_replay_return"),"No return data created by denied request");
            server.getPlayerList().op(p.nameAndId());check(ReplayServer.allowed(p),"Operator can use tool");
            p.setGameMode(GameType.CREATIVE);
            var pos=p.position();ReplayServer.action(p,"play");
            check(p.isSpectator(),"Playback uses spectator mode");
            check(p.getPersistentData().contains("aeloria_replay_return"),"Crash recovery data saved");
            check(p.getPersistentData().getCompoundOrEmpty("aeloria_replay_return").getDoubleOr("x",Double.NaN)==pos.x,"Return position persisted");
            p.setPos(pos.x+5,pos.y+2,pos.z);ReplayServer.action(p,"stop");
            // FakePlayer's connection intentionally ignores teleports; native client checks cover that path.
            check(p.gameMode.getGameModeForPlayer()==GameType.CREATIVE,"Original mode restored");
            check(!p.getPersistentData().contains("aeloria_replay_return"),"Recovery data cleared");
            Thread.sleep(300);ReplayServer.action(p,"play");check(p.isSpectator(),"Second playback session entered");
            server.getPlayerList().deop(p.nameAndId());ReplayServer.action(p,"play");
            check(!p.isSpectator(),"Deopped player cannot reenter");
            check(p.gameMode.getGameModeForPlayer()==GameType.SURVIVAL,"Revocation cannot leave creative or spectator privileges");
            Files.createDirectories(Path.of("../build/reports"));Files.writeString(Path.of("../build/reports/replay-server-checks.txt"),"PASS: creative denial, forged request denial, OP authorization, spectator lifecycle, persisted return pose, mode restoration, deop denial\n");
            MMORPG.LOGGER.info("[REPLAY CHECKS] SERVER PASS");
        }finally{ReplayServer.action(p,"stop");server.getPlayerList().deop(p.nameAndId());if(!Boolean.getBoolean("mmorpg.replayClientChecks"))server.halt(false);}
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e){
        if(Boolean.getBoolean("mmorpg.replayClientChecks")&&e.getEntity() instanceof net.minecraft.server.level.ServerPlayer p&&p.getGameProfile().name().equals("ReplayChecks")){
            p.level().getServer().getPlayerList().op(p.nameAndId());
            p.teleportTo(0,100,0);p.setGameMode(GameType.CREATIVE);
            p.getAbilities().flying=true;p.onUpdateAbilities();
            var stand=new net.minecraft.world.entity.decoration.ArmorStand(p.level(),0,100,4);stand.setNoGravity(true);
            stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_HELMET));
            p.level().addFreshEntity(stand);
        }
    }
}
