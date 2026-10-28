package com.mmorpg.server;

import com.mmorpg.guild.*;
import com.mmorpg.competition.*;
import com.mmorpg.network.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import java.util.*;
import java.nio.file.*;

public final class DevSocialChecks {
    private static void check(boolean b,String text){if(!b)throw new AssertionError("social: "+text);}
    private static CompoundTag packet(String action,String... values){var t=new CompoundTag();t.putString("action",action);for(int i=0;i<values.length;i+=2)t.putString(values[i],values[i+1]);return t;}
    public static void run(ServerPlayer p)throws Exception{
        var original=NetworkConfig.current;var dir=Files.createTempDirectory("aeloria-guild-checks-");var cfg=new NetworkConfig();cfg.enabled=true;cfg.role=NetworkConfig.Role.MAIN;cfg.sharedDirectory=dir.resolve("players").toString();NetworkConfig.current=cfg;
        var server=p.level().getServer();var member=net.neoforged.neoforge.common.util.FakePlayerFactory.get(server.overworld(),new com.mojang.authlib.GameProfile(UUID.randomUUID(),"GuildMember"));long oldGold=RpgPlayers.get(p).gold;
        try{
            check(NetworkPlayers.login(p),"guild test profile acquired");check(NetworkPlayers.login(member),"member profile acquired");RpgPlayers.get(p).gold=25000;
            GuildService.action(p,packet("create","name","Racines du Test"));var guild=GuildStore.access(server,false,s->GuildStore.of(s,p.getUUID()));check(guild!=null&&guild.level==1,"new guild starts at one");UUID id=guild.id;
            var deposit=packet("deposit");deposit.putLong("amount",20000);GuildService.action(p,deposit);check(RpgPlayers.get(p).gold==5000,"deposit debits personal wallet");check(GuildStore.access(server,false,s->GuildStore.byId(s,id).gold)==20000,"deposit credits shared bank once");GuildService.reconcile(p);check(GuildStore.access(server,false,s->GuildStore.byId(s,id).gold)==20000,"reconciliation never duplicates deposit");
            var negative=packet("deposit");negative.putLong("amount",-100);GuildService.action(p,negative);check(RpgPlayers.get(p).gold==5000,"negative deposits refused");
            GuildService.action(p,packet("invite","target",member.getUUID().toString()));GuildService.action(member,packet("accept","target",id.toString()));check(GuildStore.access(server,false,s->GuildStore.of(s,member.getUUID()).id).equals(id),"invite and accept authoritative membership");
            var withdraw=packet("withdraw");withdraw.putLong("amount",100);long before=RpgPlayers.get(member).gold;GuildService.action(member,withdraw);check(RpgPlayers.get(member).gold==before,"member cannot withdraw treasury");
            GuildService.action(p,packet("promote","target",member.getUUID().toString()));GuildService.action(member,withdraw);check(RpgPlayers.get(member).gold==before,"officer cannot withdraw treasury");
            GuildService.action(p,packet("upgrade"));check(GuildStore.access(server,false,s->GuildStore.byId(s,id).level)==1,"unbeaten boss blocks level advance");
            GuildStore.access(server,true,s->{var g=GuildStore.byId(s,id);g.xp=1000;g.bosses=1;return null;});GuildService.action(p,packet("upgrade"));
            check(GuildStore.access(server,false,s->{var g=GuildStore.byId(s,id);return g.level==2&&g.gold==10000&&g.xp==0;}),"upgrade atomically spends 10000 gold and required XP after boss");GuildService.action(p,packet("upgrade"));check(GuildStore.access(server,false,s->GuildStore.byId(s,id).gold)==10000,"repeated upgrade never spends again");
            // Crash window: bank withdrawal is reserved, player receipt saved, final bank settlement absent.
            var pending=new GuildStore.Transfer();pending.guild=id;pending.player=p.getUUID();pending.name="Recovery";pending.delta=321;
            GuildStore.access(server,true,s->{GuildStore.byId(s,id).gold-=321;s.pending.add(pending);return null;});long wallet=RpgPlayers.get(p).gold;RpgPlayers.get(p).gold+=321;RpgPlayers.get(p).guildReceipts.putBoolean(pending.id.toString(),true);check(NetworkPlayers.save(p),"receipt durable before settlement");GuildService.reconcile(p);check(RpgPlayers.get(p).gold==wallet+321&&GuildStore.access(server,false,s->s.pending.isEmpty()),"saved receipt completes interrupted transfer without duplicate credit");
            GuildService.refresh(p);check(RpgPlayers.get(p).guildLevel==2,"guild buffs synchronized from authoritative guild");
            long bankBeforeBuff=GuildStore.access(server,false,s->GuildStore.byId(s,id).gold);GuildService.action(p,packet("buff","buff","VITALITE"));
            check(GuildStore.access(server,false,s->GuildStore.byId(s,id).gold)==bankBeforeBuff-1000,"each buff costs 1000 guild gold");GuildService.refresh(member);
            check(GuildBuffs.active(RpgPlayers.get(p),GuildBuffs.Kind.VITALITE)&&GuildBuffs.active(RpgPlayers.get(member),GuildBuffs.Kind.VITALITE),"timed guild buff applies to all members");
            long until=RpgPlayers.get(p).guildBuffs[0];check(Math.abs(until-System.currentTimeMillis()-3600000)<5000,"guild buff lasts one real hour");GuildService.action(p,packet("buff","buff","VITALITE"));check(GuildStore.access(server,false,s->GuildStore.byId(s,id).gold)==bankBeforeBuff-1000,"active buff cannot charge twice");
            GuildStore.access(server,true,s->{GuildStore.byId(s,id).buffs.put("VITALITE",System.currentTimeMillis()-1);return null;});GuildService.refresh(p);GuildService.refresh(member);check(!GuildBuffs.active(RpgPlayers.get(p),GuildBuffs.Kind.VITALITE)&&!GuildBuffs.active(RpgPlayers.get(member),GuildBuffs.Kind.VITALITE),"buff expires for every member");
            var file=GuildStore.directory(server).resolve("guilds.json");String valid=Files.readString(file);Files.writeString(file,"{broken");try{GuildStore.access(server,false,s->s.guilds.size());throw new AssertionError("corrupt guild store accepted");}catch(IllegalStateException expected){}finally{Files.writeString(file,valid);}check(GuildStore.access(server,false,s->s.guilds.size())==1,"corruption never resets guild registry");
            for(int level=1;level<=100;level++)check(ArenaRules.bracket(level)==(level-1)/20,"level bracket "+level);
            for(var kind:ArenaRules.Kind.values()){var roster=new LinkedHashMap<UUID,Integer>();for(int i=0;i<kind.size*2;i++)roster.put(UUID.randomUUID(),1+i);var teams=ArenaRules.balance(kind,0,roster);check(teams.values().stream().filter(n->n==0).count()==kind.size&&teams.values().stream().filter(n->n==1).count()==kind.size,"complete balanced "+kind);roster.put(roster.keySet().iterator().next(),21);try{ArenaRules.balance(kind,0,roster);throw new AssertionError("wrong bracket accepted");}catch(IllegalArgumentException expected){}}
            var match=new ArenaRules.Match(ArenaRules.Kind.DUNES,0);UUID red=UUID.randomUUID(),blue=UUID.randomUUID(),ally=UUID.randomUUID();match.teams.put(red,0);match.teams.put(blue,1);match.teams.put(ally,0);
            match.tick(400);check(match.red==0&&match.blue==0,"neutral crystal awards no points");
            for(int hit=0;hit<49;hit++)check(match.capture(hit%2==0?red:blue,400),"every strike accepted without capture cooldown");
            check(match.owner==-1&&match.captureHits==49,"49 blows leave white crystal");
            check(match.capture(blue,2360)&&match.owner==1,"50th hit wins for its actual team");match.tick(2399);check(match.blue==0,"no early crystal point");match.tick(2400);check(match.blue==1,"point after two seconds");
            check(!match.capture(blue,2400)&&match.owner==1,"friendly blows do not neutralize owned crystal");
            check(match.capture(red,2400)&&match.owner==-1&&match.captureHits==1,"opponent starts white contest");match.tick(4000);check(match.blue==1,"white contest pauses income");
            match.kill(red,ally);check(match.red==0,"friendly kills never score");match.kill(red,blue);check(match.red==10,"enemy kill awards ten points");match.score(0,1000);check(match.ended&&match.red==500&&match.winner()==0,"first team to 500 wins");check(!match.capture(blue,5000),"ended match ignores crystal blows");
            check(Yggdrasil.ALL.length==10&&Arrays.stream(Yggdrasil.ALL).map(Yggdrasil.Guardian::id).distinct().count()==10,"ten unique guardians");
            com.mmorpg.competition.DevArenaChecks.run(p);
            var raid=CompetitionWorld.raid(new net.minecraft.core.BlockPos(500,100,4000),0);var fire=CompetitionWorld.arena(CompetitionWorld.center(0),ArenaRules.Kind.FEU);check(raid.blocks.size()>4000&&fire.blocks.size()>15000,"real decorated arena and trial geometry generated");
            for(var type:com.mmorpg.registry.ModEntities.GUILD_GUARDIANS){var boss=type.get().create(p.level(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);check(boss!=null&&boss.getBbHeight()>=5,"giant guardian registered with large hitbox");boss.initialize();check(boss.rpgData().boss&&boss.rpgData().maxHp>=3500,"guardian RPG combat stats initialized");}
            com.mmorpg.MMORPG.LOGGER.info("[CODEX CHECKS] GUILDS_ARENAS PASS: creation, ranks, invites, treasury, crash recovery, corruption refusal, XP/boss/gold upgrades, buffs, brackets, crystal timing, kills, win and ten guardians");
        }finally{NetworkPlayers.logout(p);NetworkPlayers.logout(member);RpgPlayers.get(p).gold=oldGold;NetworkConfig.current=original;}
    }
}
