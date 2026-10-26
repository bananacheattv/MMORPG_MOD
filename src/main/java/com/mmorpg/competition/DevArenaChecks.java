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
            session.crystal=com.mmorpg.registry.ModEntities.ARENA_CRYSTAL.get().create(world,net.minecraft.world.entity.EntitySpawnReason.COMMAND);session.crystal.match=session.match.id;check(session.crystal.isPersistenceRequired(),"arena crystal survives distance despawn");
            long gold=com.mmorpg.server.RpgPlayers.get(p).gold,xp=com.mmorpg.server.RpgPlayers.get(p).xp;
            check(ArenaManager.lethal(p,opponent),"lethal hit intercepted");check(p.level()==world&&Math.abs(p.getX()-(center.getX()-32.5))<.01,"respawn at own camp");check(session.match.blue==10,"enemy kill grants ten points");check(com.mmorpg.server.RpgPlayers.get(p).gold==gold&&com.mmorpg.server.RpgPlayers.get(p).xp==xp,"arena death preserves XP and gold");check(ArenaManager.protectedPlayer(p),"respawn protection applied");
            check(!session.crystal.hurtServer(world,p.damageSources().playerAttack(p),1),"protected player cannot capture");session.protectedUntil.put(p.getUUID(),0L);
            check(session.crystal.hurtServer(world,p.damageSources().playerAttack(p),1)&&session.crystal.team()==0&&session.match.owner==0,"real attack captures custom crystal");
            ArenaManager.leave(p);check(p.level()==oldLevel&&Math.abs(p.getX()-x)<.01,"leaving restores original position");
            ArenaManager.SESSIONS.remove(key);
            var identity=new net.minecraft.server.players.NameAndId(p.getGameProfile());
            var dispatcher=server.getCommands().getDispatcher();
            server.getPlayerList().deop(identity);
            try{ArenaManager.test(p,ArenaRules.Kind.FEU);throw new AssertionError("non-operator started test");}catch(IllegalArgumentException expected){}
            var denied=dispatcher.parse("aeloriatest boss 1",p.createCommandSourceStack());check(denied.getReader().canRead(),"non-operator command unavailable");
            server.getPlayerList().op(identity);
            try{
                for(var kind:ArenaRules.Kind.values()){
                    check(dispatcher.execute("aeloriatest arene "+kind.name().toLowerCase(java.util.Locale.ROOT),p.createCommandSourceStack())==1,"admin arena command");
                    var solo=ArenaManager.session(p.getUUID());check(solo!=null&&solo.test&&solo.readyToBuild()&&!solo.forfeited(),"solo starts immediately and does not forfeit");
                    check(solo.center().getX()>=CompetitionWorld.center(10).getX()&&!ArenaManager.queued(p.getUUID()),"test separate from public matchmaking");
                    solo.active=true;ArenaManager.testTeam(p,1);solo.match.capture(p.getUUID(),0);solo.match.tick(40);check(solo.match.blue==1,"solo blue capture scores");
                    solo.returns.put(p.getUUID(),new ArenaManager.Return(oldLevel.dimension(),x,y,z,yaw,pitch));CompetitionAdmin.stop(p);ArenaManager.tick(server);check(ArenaManager.session(p.getUUID())==null,"empty test disposed");
                }
                for(int tier:new int[]{0,9}){
                    check(dispatcher.execute("aeloriatest boss "+(tier+1),p.createCommandSourceStack())==1,"admin boss command");
                    var raid=GuildRaids.of(p.getUUID());check(raid!=null&&raid.test&&raid.tier==tier,"any boss without guild or level requirement");
                    while(!raid.build.done())raid.build.tick(world);
                    raid.boss=com.mmorpg.registry.ModEntities.GUILD_GUARDIANS.get(tier).get().create(world,net.minecraft.world.entity.EntitySpawnReason.COMMAND);raid.boss.testing=true;raid.boss.guild=raid.guild;raid.boss.initialize();
                    long savedGold=playerData.gold,savedXp=playerData.xp;
                    com.mmorpg.server.KillRewards.onDeath(new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(raid.boss,p.damageSources().playerAttack(p)));
                    GuildRaids.killed(raid.boss,p);check(raid.rewarded&&playerData.gold==savedGold&&playerData.xp==savedXp,"test boss completes without rewards or guild");
                    check(GuildRaids.testDeath(p)&&playerData.gold==savedGold&&playerData.xp==savedXp,"boss test respawn preserves progression");
                    CompetitionAdmin.stop(p);GuildRaids.tick(server);check(GuildRaids.of(p.getUUID())==null,"boss test stop restores player");
                }
                check(dispatcher.parse("aeloriatest boss 11",p.createCommandSourceStack()).getReader().canRead(),"boss bounds enforced");
            }catch(com.mojang.brigadier.exceptions.CommandSyntaxException ex){throw new AssertionError(ex);}finally{server.getPlayerList().deop(identity);}
            com.mmorpg.MMORPG.LOGGER.info("[CODEX CHECKS] ADMIN_SOLO PASS: permissions, commands, isolated solo arena, both teams, guardians 1 and 10, no rewards, respawn and exit");
            com.mmorpg.MMORPG.LOGGER.info("[CODEX CHECKS] ARENA_WORLD PASS: decorated geometry, clear crystal orbit, contained lava, real capture, enemy score, protected respawn, no XP/gold loss and return");
        }finally{ArenaManager.SESSIONS.remove(key);if(session.crystal!=null)session.crystal.discard();if(p.level()!=oldLevel)p.teleportTo(oldLevel,x,y,z,Set.of(),yaw,pitch,true);com.mmorpg.network.NetworkConfig.current=cfg;}
    }
}
