package com.mmorpg.competition;

import com.mmorpg.entity.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.BossEvent;
import java.util.*;

public final class ArenaDefence {
    public static final int LOCK_TICKS=60;
    public static boolean enemyZone(int team,double x,double y,double z){return Math.abs(z)<=10&&y>=98&&y<=116&&(team==0?x>=36&&x<=52:x<=-36&&x>=-52);}
    public final List<ArenaDrone> drones=new ArrayList<>();
    private final Map<UUID,Long> since=new HashMap<>();
    int remaining(UUID id,long tick){return (int)Math.max(0,LOCK_TICKS-(tick-since.computeIfAbsent(id,key->tick)));}
    void cancel(UUID id){clear(id);}
    public int dangerRemaining(UUID id,long tick){return since.containsKey(id)?Math.max(0,LOCK_TICKS-(int)(tick-since.get(id))):-1;}
    private final Map<UUID,ServerBossEvent> warnings=new HashMap<>();
    private ServerLevel arenaLevel;
    private final Set<net.minecraft.world.level.ChunkPos> tickets=new HashSet<>();
    public void spawn(ServerLevel level,net.minecraft.core.BlockPos center){
        arenaLevel=level;
        for(int side:new int[]{-1,1})for(int z:new int[]{-7,7}){
            var chunk=new net.minecraft.world.level.ChunkPos((center.getX()+side*43)>>4,(center.getZ()+z)>>4);
            if(tickets.add(chunk))level.setChunkForced(chunk.x(),chunk.z(),true);
            level.getChunk(chunk.x(),chunk.z());
            var drone=com.mmorpg.registry.ModEntities.ARENA_DRONE.get().create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            drone.snapTo(center.getX()+side*43+.5,105,center.getZ()+z+.5,side<0?-90:90,0);drone.setCustomName(Component.literal("Drone de défense"));drone.setCustomNameVisible(true);level.addFreshEntity(drone);drones.add(drone);
        }
    }
    private void clear(UUID id){since.remove(id);var bar=warnings.remove(id);if(bar!=null)bar.removeAllPlayers();}
    public void dispose(){drones.forEach(ArenaDrone::discard);drones.clear();for(var bar:warnings.values())bar.removeAllPlayers();warnings.clear();since.clear();if(arenaLevel!=null)for(var chunk:tickets)arenaLevel.setChunkForced(chunk.x(),chunk.z(),false);tickets.clear();arenaLevel=null;}
    public void tick(ArenaManager.Session session,MinecraftServer server,long tick){
        var center=session.center();Set<UUID> danger=new HashSet<>();
        for(var entry:session.match.teams.entrySet()){
            var player=server.getPlayerList().getPlayer(entry.getKey());
            if(player==null||!player.isAlive()||player.isCreative()||player.isSpectator()||player.level()!=CompetitionWorld.level(server)
                    ||!enemyZone(entry.getValue(),player.getX()-center.getX(),player.getY(),player.getZ()-center.getZ()))continue;
            danger.add(entry.getKey());int remaining=remaining(entry.getKey(),tick);
            var bar=warnings.computeIfAbsent(entry.getKey(),id->new ServerBossEvent(java.util.UUID.randomUUID(),Component.empty(),BossEvent.BossBarColor.RED,BossEvent.BossBarOverlay.PROGRESS));
            bar.addPlayer(player);bar.setName(Component.literal(String.format(Locale.FRANCE,"Drone ennemi · Quittez le camp : %.1f s",remaining/20.0)));bar.setProgress(remaining/(float)LOCK_TICKS);
            var drone=drones.stream().filter(ArenaDrone::isAlive).min(Comparator.comparingDouble(d->d.distanceToSqr(player))).orElse(null);
            if(drone!=null&&(tick%5==0||remaining==0)){
                drone.lookAt(player,360,360);drone.setYBodyRot(drone.getYHeadRot());
                var beam=SkillFxEntity.create(player.level(),FxKind.BEAM,drone.getEyePosition(),remaining==0?1.0:.32,0xFF2040,6).points(List.of(drone.getEyePosition(),player.getEyePosition()));player.level().addFreshEntity(beam);
            }
            if(remaining==0){
                player.level().addFreshEntity(SkillFxEntity.create(player.level(),FxKind.BURST,player.position(),1.5,0xFF2040,16));
                ArenaManager.defenceKill(player,session);clear(entry.getKey());
            }
        }
        for(var id:new ArrayList<>(since.keySet()))if(!danger.contains(id))clear(id);
        for(int i=0;i<drones.size();i++){var drone=drones.get(i);if(drone.isAlive())drone.setPos(drone.getX(),105+Math.sin(tick*.045+i)*.18,drone.getZ());}
    }
}
