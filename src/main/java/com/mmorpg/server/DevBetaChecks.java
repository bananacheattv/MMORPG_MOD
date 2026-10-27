package com.mmorpg.server;

import com.mmorpg.competition.*;
import com.mmorpg.network.*;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** Regression coverage for the beta queue, cooldowns and group authority. */
public final class DevBetaChecks {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError("beta: "+message);}
    public static void run(ServerPlayer p)throws Exception {
        var old=NetworkConfig.current;var cfg=new NetworkConfig();cfg.enabled=true;cfg.role=NetworkConfig.Role.MAIN;
        cfg.sharedDirectory=java.nio.file.Files.createTempDirectory("aeloria-beta-checks-").resolve("players").toString();
        var server=p.level().getServer();var groupSnapshot=PartyManager.sharedSnapshot();
        try {
            NetworkConfig.current=cfg;
            var data=RpgPlayers.get(p);data.playerClass=com.mmorpg.rpg.PlayerClass.GUERRIER;data.level=10;
            RpgPlayers.recompute(p);double max=data.stats.get(com.mmorpg.rpg.Stat.MAX_HP);data.hp=(float)(max*.5);
            int oldTick=p.tickCount;p.tickCount=99;RpgPlayers.tick(p);check(Math.abs(data.hp-max*.5)<.001,"no continuous passive regen");
            p.tickCount=100;RpgPlayers.tick(p);check(Math.abs(data.hp-max*.51)<.001,"exact one percent pulse");p.tickCount=oldTick;
            p.getInventory().clearContent();RpgPlayers.giveStarterTools(p);check(!p.getInventory().contains(new net.minecraft.world.item.ItemStack(com.mmorpg.registry.ModItems.get("graine_lueur"))),"starter has no glow seeds");
            check(NetworkPlayers.login(p),"arrival fixture login");check(NetworkPlayers.portalBlocked(p),"saved backend position cannot trigger return portal on login");NetworkPlayers.logout(p);
            for(int region=0;region<com.mmorpg.mine.MineLayout.SECTORS;region++)com.mmorpg.MMORPG.LOGGER.info("[CODEX CHECKS] Mine entry {} {}",region,java.util.Arrays.toString(com.mmorpg.mine.MineManager.spawnPoint(region)));
            for(var destination:List.of("mine:0","plains:0","dungeon:crypte_gobeline:0")){
                cfg.role=NetworkConfig.Role.MAIN;check(NetworkPlayers.login(p),"travel source acquires session");RpgPlayers.get(p).playerClass=com.mmorpg.rpg.PlayerClass.GUERRIER;RpgPlayers.get(p).level=100;
                var role=destination.startsWith("mine")?NetworkConfig.Role.MINES:destination.startsWith("plains")?NetworkConfig.Role.PLAINS:NetworkConfig.Role.DUNGEONS;
                check(NetworkPlayers.route(p,role,destination)&&NetworkPlayers.inTransit(p),"source prepares proxy transfer");NetworkPlayers.logout(p);cfg.role=role;
                check(NetworkPlayers.login(p)&&NetworkPlayers.isArriving(p),"destination restores travel instruction before login recovery");
                var arrival=NetworkPlayers.class.getDeclaredMethod("arrive",ServerPlayer.class,String.class);arrival.setAccessible(true);arrival.invoke(null,p,destination);
                if(role==NetworkConfig.Role.MINES)check(com.mmorpg.mine.MineManager.transitioning(p),"mine arrival starts construction/loading without spawn routing");
                if(role==NetworkConfig.Role.PLAINS)check(com.mmorpg.plains.PlainsManager.transitioning(p),"plain arrival starts construction/loading without spawn routing");
                check(!NetworkPlayers.inTransit(p),"destination does not bounce to another backend");NetworkPlayers.logout(p);com.mmorpg.dungeon.BossReveal.release(p);
            }
            cfg.role=NetworkConfig.Role.MAIN;RpgPlayers.get(p).level=10;
            var before=p.position();var dimension=p.level();
            SharedArenaQueue.join(p,ArenaRules.Kind.FEU,0);check(p.level()==dimension&&p.position().equals(before),"registration never teleports");
            var players=new ArrayList<ServerPlayer>();players.add(p);
            for(int i=1;i<10;i++){
                var q=net.neoforged.neoforge.common.util.FakePlayerFactory.get(server.overworld(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"Queue"+i));RpgPlayers.get(q).level=10;players.add(q);
                SharedArenaQueue.join(q,ArenaRules.Kind.FEU,0);
                if(i==8){SharedArenaQueue.tick(server);check(SharedLedger.access(server,"arena-queue",SharedArenaQueue.State.class,SharedArenaQueue.State::new,s->s.matches.isEmpty()),"nine registrations do not start 5v5");}
            }
            SharedArenaQueue.tick(server);
            var match=SharedLedger.access(server,"arena-queue",SharedArenaQueue.State.class,SharedArenaQueue.State::new,s->s.matches.getFirst());
            check(match.teams.size()==10&&!match.travel,"full roster waits for ready acceptance");
            check(SharedArenaQueue.count(ArenaRules.Kind.FEU,0,p.getUUID())==10,"ready roster still displays ten registered players");
            check(SharedArenaQueue.kind(p.getUUID()).equals("FEU"),"registered arena remains marked in UI");
            for(int i=0;i<9;i++)check(SharedArenaQueue.ready(players.get(i),match.id.toString()),"ready accepted");
            SharedArenaQueue.tick(server);check(!SharedLedger.access(server,"arena-queue",SharedArenaQueue.State.class,SharedArenaQueue.State::new,s->s.matches.getFirst().travel),"nine acceptances do not transfer");
            SharedArenaQueue.ready(players.get(9),match.id.toString());SharedArenaQueue.tick(server);
            check(SharedLedger.access(server,"arena-queue",SharedArenaQueue.State.class,SharedArenaQueue.State::new,s->s.matches.getFirst().travel),"ten acceptances authorize transfer");
            SharedArenaQueue.finish(server,match.id);
            var group=new PartyManager.Party();group.leader=players.get(1).getUUID();group.members.add(group.leader);group.members.add(p.getUUID());SharedParties.mutate(p,()->PartyManager.installShared(List.of(group)));
            check(GroupTravel.begin(p,"mine","0"),"member entry intercepted");
            check(SharedLedger.access(server,"group-travel",GroupTravel.State.class,GroupTravel.State::new,s->s.trips.isEmpty()),"non-leader cannot schedule group departure");
            var guild=new com.mmorpg.guild.GuildStore.Guild();guild.name="Recharge";guild.level=2;guild.members.put(p.getUUID(),new com.mmorpg.guild.GuildStore.Member(p.getUUID(),"Test","CHEF"));
            com.mmorpg.guild.GuildStore.access(server,true,s->{s.guilds.add(guild);return null;});
            com.mmorpg.guild.GuildService.bossCleared(server,guild.id,0);
            long until=com.mmorpg.guild.GuildStore.access(server,false,s->com.mmorpg.guild.GuildStore.byId(s,guild.id).bossCooldowns.get(0));
            check(Math.abs(until-System.currentTimeMillis()-21600000)<5000,"replayed lower boss persists six hour cooldown");
            var permit=new AdminTravel.Ticket();permit.player=p.getUUID();permit.target=UUID.randomUUID();
            SharedLedger.access(server,"admin-travel",AdminTravel.State.class,AdminTravel.State::new,s->{s.tickets.put(permit.id,permit);return null;});
            boolean refused=false;try{AdminTravel.arrive(players.get(1),permit.id.toString());}catch(IllegalArgumentException expected){refused=true;}check(refused,"admin ticket cannot be stolen by another player");
            check(SharedLedger.access(server,"admin-travel",AdminTravel.State.class,AdminTravel.State::new,s->s.tickets.containsKey(permit.id)),"wrong player does not consume owner ticket");
            refused=false;try{AdminTravel.arrive(p,permit.id.toString());}catch(IllegalArgumentException expected){refused=true;}check(refused,"offline teleport target fails clearly");
            check(!SharedLedger.access(server,"admin-travel",AdminTravel.State.class,AdminTravel.State::new,s->s.tickets.containsKey(permit.id)),"admin ticket is consumed once");
            var expired=new AdminTravel.Ticket();expired.player=p.getUUID();expired.expires=System.currentTimeMillis()-1;
            SharedLedger.access(server,"admin-travel",AdminTravel.State.class,AdminTravel.State::new,s->{s.tickets.put(expired.id,expired);return null;});
            refused=false;try{AdminTravel.arrive(p,expired.id.toString());}catch(IllegalArgumentException expected){refused=true;}check(refused,"expired admin ticket refused");
            com.mmorpg.MMORPG.LOGGER.info("[CODEX CHECKS] BETA PASS: regen, starter, portal arrival protection, full network roster and ready gates, leader authority, persistent boss replay cooldown");
        } finally {NetworkPlayers.logout(p);NetworkConfig.current=old;PartyManager.installShared(groupSnapshot);}
    }
}
