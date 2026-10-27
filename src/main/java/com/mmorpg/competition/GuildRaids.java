package com.mmorpg.competition;

import com.mmorpg.guild.*;
import com.mmorpg.network.*;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class GuildRaids {
    public static final class Raid {
        public UUID guild;public int tier,slot;public long started,returnAt;public final Set<UUID> players=new HashSet<>();public final Map<UUID,ArenaManager.Return> returns=new HashMap<>();
        public CompetitionWorld.Job build;public GuildGuardian boss;public boolean rewarded,defeated,test;public BlockPos center(){return new BlockPos(500+slot*240,100,4000);}
    }
    private static final Map<UUID,Raid> RAIDS=new HashMap<>();
    public static Raid of(UUID player){return RAIDS.values().stream().filter(r->r.players.contains(player)).findFirst().orElse(null);}
    public static void died(ServerPlayer p){var r=of(p.getUUID());if(r!=null){r.players.remove(p.getUUID());r.returns.remove(p.getUUID());}}
    public static boolean allowed(GuildGuardian boss,UUID player){var r=of(player);return r!=null&&r.boss==boss;}
    public static void enter(ServerPlayer p){enter(p,-1);}
    public static void enter(ServerPlayer p,int requestedTier){
        if(ArenaManager.session(p.getUUID())!=null||ArenaManager.queued(p.getUUID())||of(p.getUUID())!=null||com.mmorpg.dungeon.InstanceManager.containsPlayer(p))throw new IllegalArgumentException("Tu participes déjà à un combat.");
        var g=GuildStore.access(p.level().getServer(),false,s->GuildStore.of(s,p.getUUID()));GuildService.require(g!=null,"Rejoins d’abord une guilde.");
        int tier=requestedTier<0?g.level-1:requestedTier;
        GuildService.require(tier>=0&&tier<g.level,"Ce gardien n’est pas encore débloqué.");
        long remaining=g.bossCooldowns.getOrDefault(tier,0L)-System.currentTimeMillis();
        GuildService.require(remaining<=0,"Gardien disponible dans "+Math.max(1,(remaining+59999)/60000)+" min (recharge de 6 heures après victoire).");
        GuildService.require(RpgPlayers.get(p).playerClass.isPlayable()&&!com.mmorpg.hub.ClassTrial.active(p),"Termine le choix de classe.");
        if(CompetitionTravel.route(p,"guild-raid:"+tier))return;
        var server=p.level().getServer();Raid r=RAIDS.get(g.id);
        if(r==null){GuildService.require(RAIDS.size()<8,"Les huit instances de l’Arbre-Monde sont occupées.");r=new Raid();r.guild=g.id;r.tier=tier;r.started=server.getTickCount();int slot=0;while(true){int n=slot;if(RAIDS.values().stream().noneMatch(v->v.slot==n))break;slot++;}r.slot=slot;r.build=CompetitionWorld.raid(r.center(),tier);RAIDS.put(g.id,r);}
        GuildService.require(r.tier==tier&&!r.rewarded&&r.players.size()<20,"Instance complète ou palier modifié.");r.players.add(p.getUUID());r.returns.put(p.getUUID(),new ArenaManager.Return(p.level().dimension(),p.getX(),p.getY(),p.getZ(),p.getYRot(),p.getXRot()));
        if(r.build.done())arrive(p,r);else p.sendSystemMessage(Component.literal("Préparation de l’instance de l’Arbre-Monde…"));
    }
    public static void test(ServerPlayer p,int tier){
        CompetitionAdmin.require(p);CompetitionAdmin.available(p);
        if(tier<0||tier>=10)throw new IllegalArgumentException("Choisis un gardien entre 1 et 10.");
        if(CompetitionTravel.route(p,"admin-test:boss:"+tier))return;
        if(RAIDS.size()>=8)throw new IllegalArgumentException("Les huit instances sont occupées.");
        Raid r=new Raid();r.test=true;r.guild=UUID.randomUUID();r.tier=tier;r.started=p.level().getServer().getTickCount();
        int slot=0;while(true){int n=slot;if(RAIDS.values().stream().noneMatch(v->v.slot==n))break;slot++;}r.slot=slot;r.build=CompetitionWorld.raid(r.center(),tier);
        r.players.add(p.getUUID());r.returns.put(p.getUUID(),new ArenaManager.Return(p.level().dimension(),p.getX(),p.getY(),p.getZ(),p.getYRot(),p.getXRot()));RAIDS.put(r.guild,r);
        p.sendSystemMessage(Component.literal("Préparation du test solo : "+Yggdrasil.ALL[tier].name()+". Sans récompense ni progression de guilde. /aeloriatest stop pour revenir."));
    }
    public static boolean testDeath(ServerPlayer p){var r=of(p.getUUID());if(r==null||!r.test)return false;arrive(p,r);p.setHealth(p.getMaxHealth());p.clearFire();p.removeAllEffects();return true;}
    private static void arrive(ServerPlayer p,Raid r){CompetitionWorld.teleport(p,r.center().offset(0,1,25),180);RpgPlayers.fullRestore(p);com.mmorpg.server.MountManager.despawn(p);}
    public static void killed(GuildGuardian boss,ServerPlayer killer){if(!allowed(boss,killer.getUUID()))return;Raid r=of(killer.getUUID());r.defeated=true;reward(r,killer.level().getServer());}
    private static void reward(Raid r,MinecraftServer server){if(r.rewarded)return;try{
        if(!r.test)GuildService.bossCleared(server,r.guild,r.tier);r.rewarded=true;
        for(UUID id:r.players){var p=server.getPlayerList().getPlayer(id);if(p!=null)p.sendSystemMessage(Component.literal(Yggdrasil.ALL[r.tier].name()+" vaincu ! "+(r.test?"Test réussi, aucune récompense ni évolution de guilde.":r.tier==9?"L’Arbre-Monde reconnaît votre guilde !":"Votre guilde peut débloquer le niveau suivant avec son XP et 10 000 or.")));}r.returnAt=server.getTickCount()+20*30;
        }catch(Exception e){com.mmorpg.MMORPG.LOGGER.error("Guild boss completion retained for retry",e);}
    }
    public static void leave(ServerPlayer p){Raid r=of(p.getUUID());if(r==null)return;r.players.remove(p.getUUID());var point=r.returns.remove(p.getUUID());if(NetworkConfig.current.enabled)CompetitionTravel.home(p);else if(point!=null){var l=p.level().getServer().getLevel(point.dim());if(l!=null)p.teleportTo(l,point.x(),point.y(),point.z(),Set.of(),point.yaw(),point.pitch(),true);}else CompetitionTravel.home(p);}
    public static void tick(MinecraftServer server){if(!NetworkConfig.hosts(NetworkConfig.Role.DUNGEONS))return;
        for(var r:new ArrayList<>(RAIDS.values())){
            if(r.players.isEmpty()){if(r.boss!=null)r.boss.discard();RAIDS.remove(r.guild);continue;}
            if(!r.build.done()){r.build.tick(CompetitionWorld.level(server));if(r.build.done()){
                var level=CompetitionWorld.level(server);r.boss=com.mmorpg.registry.ModEntities.GUILD_GUARDIANS.get(r.tier).get().create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                if(r.boss==null)throw new IllegalStateException("Gardien indisponible");r.boss.guild=r.guild;r.boss.testing=r.test;r.boss.snapTo(r.center().getX()+.5,100,r.center().getZ()+.5,0,0);
                r.boss.initialize();level.addFreshEntity(r.boss);for(UUID id:r.players){var p=server.getPlayerList().getPlayer(id);if(p!=null)arrive(p,r);}
            }continue;}
            if(server.getTickCount()%20==0){
                if(r.defeated&&!r.rewarded)reward(r,server);
                r.players.removeIf(id->{var p=server.getPlayerList().getPlayer(id);return p==null||!CompetitionTravel.inside(p);});
                if(r.boss!=null&&r.boss.isAlive()){
                    var target=r.players.stream().map(server.getPlayerList()::getPlayer).filter(Objects::nonNull).filter(p->!p.isSpectator()&&!p.isCreative()).min(Comparator.comparingDouble(r.boss::distanceToSqr)).orElse(null);r.boss.setTarget(target);
                    if(r.boss.distanceToSqr(r.center().getX(),100,r.center().getZ())>32*32)r.boss.setPos(r.center().getX()+.5,100,r.center().getZ()+.5);
                }
                for(UUID id:r.players){var p=server.getPlayerList().getPlayer(id);if(p!=null){if(p.getY()<97||p.distanceToSqr(r.center().getX(),100,r.center().getZ())>42*42)arrive(p,r);var md=r.boss==null?null:r.boss.rpgData();p.sendOverlayMessage(Component.literal(r.rewarded?"Victoire ! Retour à votre départ dans "+Math.max(0,(r.returnAt-server.getTickCount()+19)/20)+" s":"Yggdrasil · "+Yggdrasil.ALL[r.tier].name()+" · "+(md==null?"":Math.round(md.hp)+" / "+Math.round(md.maxHp)+" PV")));}}
            }
            if(r.players.isEmpty()||(r.returnAt>0&&server.getTickCount()>=r.returnAt)||server.getTickCount()-r.started>20*60*20){for(UUID id:new ArrayList<>(r.players)){var p=server.getPlayerList().getPlayer(id);if(p!=null)leave(p);}if(r.boss!=null)r.boss.discard();RAIDS.remove(r.guild);}
        }
    }
    public static void shutdown(MinecraftServer server){for(var r:new ArrayList<>(RAIDS.values())){for(UUID id:new ArrayList<>(r.players)){var p=server.getPlayerList().getPlayer(id);if(p!=null)leave(p);}if(r.boss!=null)r.boss.discard();}RAIDS.clear();}
}
