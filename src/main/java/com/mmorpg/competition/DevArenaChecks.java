package com.mmorpg.competition;

import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** Real-world integration, enabled only from the isolated network suite. */
public final class DevArenaChecks {
    private static final class ClientAcknowledgingPlayer extends net.neoforged.neoforge.common.util.FakePlayer {
        ClientAcknowledgingPlayer(net.minecraft.server.level.ServerLevel level){super(level,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ArenaChecks"));}
        @Override public boolean teleportTo(net.minecraft.server.level.ServerLevel level,double x,double y,double z,Set<net.minecraft.world.entity.Relative> relatives,float yaw,float pitch,boolean reset){
            boolean ok=super.teleportTo(level,x,y,z,relatives,yaw,pitch,reset);
            // FakePlayerNetHandler intentionally ignores teleport packets; emulate the client's accepted position.
            if(ok){setPos(x,y,z);setYRot(yaw);setXRot(pitch);}return ok;
        }
    }
    private static void check(boolean value,String message){if(!value)throw new AssertionError("arena integration: "+message);}
    public static void run(ServerPlayer source){
        if(!Boolean.getBoolean("mmorpg.checkNetwork"))throw new IllegalStateException("Development checks only");
        var p=new ClientAcknowledgingPlayer(source.level());var playerData=com.mmorpg.server.RpgPlayers.get(p);playerData.playerClass=com.mmorpg.rpg.PlayerClass.GUERRIER;playerData.gold=12345;playerData.xp=321;com.mmorpg.server.RpgPlayers.recompute(p);
        var server=p.level().getServer();var world=CompetitionWorld.level(server);var key=new ArenaManager.Key(ArenaRules.Kind.FEU,0);var session=new ArenaManager.Session(key,server.getTickCount());
        var oldLevel=p.level();double x=p.getX(),y=p.getY(),z=p.getZ();float yaw=p.getYRot(),pitch=p.getXRot();
        var opponent=net.neoforged.neoforge.common.util.FakePlayerFactory.get(oldLevel,new com.mojang.authlib.GameProfile(UUID.randomUUID(),"ArenaOpponent"));
        var cfg=com.mmorpg.network.NetworkConfig.current;var local=new com.mmorpg.network.NetworkConfig();com.mmorpg.network.NetworkConfig.current=local;
        try{
            var center=CompetitionWorld.center(0);var job=CompetitionWorld.arena(center,ArenaRules.Kind.FEU);while(!job.done())job.tick(world);
            for(int n=0;n<120;n++){double a=n*Math.PI/60,cx=center.getX()+Math.cos(a)*18+.5,cz=center.getZ()+Math.sin(a)*12+.5;check(world.noCollision(null,new net.minecraft.world.phys.AABB(cx-1.2,101.2,cz-1.2,cx+1.2,104.7,cz+1.2)),"crystal orbit clear of rocks "+n);}
            check(world.getBlockState(center.offset(46,-1,0)).is(net.minecraft.world.level.block.Blocks.LAVA),"lava basin exists below walking surface");check(world.getBlockState(center.offset(47,-1,0)).getFluidState().isEmpty(),"basin has retaining wall");
            session.match.teams.put(p.getUUID(),0);session.match.teams.put(opponent.getUUID(),1);session.active=true;session.returns.put(p.getUUID(),new ArenaManager.Return(oldLevel.dimension(),x,y,z,yaw,pitch));ArenaManager.SESSIONS.put(key,session);
            session.crystal=com.mmorpg.registry.ModEntities.ARENA_CRYSTAL.get().create(world,net.minecraft.world.entity.EntitySpawnReason.COMMAND);session.crystal.match=session.match.id;
            long gold=com.mmorpg.server.RpgPlayers.get(p).gold,xp=com.mmorpg.server.RpgPlayers.get(p).xp;
            check(ArenaManager.lethal(p,opponent),"lethal hit intercepted");check(p.level()==world&&Math.abs(p.getX()-(center.getX()-32.5))<.01,"respawn at own camp");check(session.match.blue==10,"enemy kill grants ten points");check(com.mmorpg.server.RpgPlayers.get(p).gold==gold&&com.mmorpg.server.RpgPlayers.get(p).xp==xp,"arena death preserves XP and gold");check(ArenaManager.protectedPlayer(p),"respawn protection applied");
            check(!session.crystal.hurtServer(world,p.damageSources().playerAttack(p),1),"protected player cannot capture");session.protectedUntil.put(p.getUUID(),0L);
            check(session.crystal.hurtServer(world,p.damageSources().playerAttack(p),1)&&session.crystal.team()==0&&session.match.owner==0,"real attack captures custom crystal");
            ArenaManager.leave(p);check(p.level()==oldLevel&&Math.abs(p.getX()-x)<.01,"leaving restores original position");
            com.mmorpg.MMORPG.LOGGER.info("[CODEX CHECKS] ARENA_WORLD PASS: decorated geometry, clear crystal orbit, contained lava, real capture, enemy score, protected respawn, no XP/gold loss and return");
        }finally{ArenaManager.SESSIONS.remove(key);if(session.crystal!=null)session.crystal.discard();if(p.level()!=oldLevel)p.teleportTo(oldLevel,x,y,z,Set.of(),yaw,pitch,true);com.mmorpg.network.NetworkConfig.current=cfg;}
    }
}
