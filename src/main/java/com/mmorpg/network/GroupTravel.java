package com.mmorpg.network;

import com.mmorpg.server.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import java.util.*;

/** Two-phase departure: every online member validates on their own backend before anyone is moved. */
public final class GroupTravel {
    public static final class Trip {
        public UUID id=UUID.randomUUID(),leader;
        public String kind,value,error="";
        public Set<UUID> members=new LinkedHashSet<>(),ready=new HashSet<>(),sent=new HashSet<>();
        public long expires=System.currentTimeMillis()+20000; public boolean committed;
    }
    public static final class State { public ArrayList<Trip> trips=new ArrayList<>(); }
    private static boolean individual;
    public static boolean individual(){return individual;}
    public static boolean begin(ServerPlayer p,String kind,String value){
        if(individual||NetworkPlayers.isArriving(p))return false;
        try {
            SharedParties.refresh();
            var party=PartyManager.partyOf(p);
            if(party==null)return false;
            if(!party.leader.equals(p.getUUID())){p.sendSystemMessage(Component.literal("Seul le meneur du groupe peut choisir une destination."));return true;}
            Trip trip=new Trip();trip.leader=p.getUUID();trip.kind=kind;trip.value=value;
            for(var member:com.mmorpg.guild.GuildService.online(p.level().getServer()))if(party.members.contains(member.id))trip.members.add(member.id);
            trip.members.add(p.getUUID());
            if(!NetworkConfig.current.enabled){
                var players=PartyManager.onlineMembers(p);
                for(var player:players)validate(player,trip);
                for(var player:players)dispatch(player,trip);
                return true;
            }
            SharedLedger.access(p.level().getServer(),"group-travel",State.class,State::new,s->{
                s.trips.removeIf(t->t.expires<System.currentTimeMillis());
                if(s.trips.stream().anyMatch(t->t.members.stream().anyMatch(trip.members::contains)))throw new IllegalArgumentException("Un départ de groupe est déjà en préparation.");
                s.trips.add(trip);return null;
            });
            p.sendSystemMessage(Component.literal("Préparation du départ pour les "+trip.members.size()+" membres connectés du groupe…"));
        } catch(Exception e){p.sendSystemMessage(Component.literal("Départ annulé : "+e.getMessage()));}
        return true;
    }
    private static NetworkConfig.Role role(Trip t){return switch(t.kind){case "mine"->NetworkConfig.Role.MINES;case "plaines"->NetworkConfig.Role.PLAINS;default->NetworkConfig.Role.DUNGEONS;};}
    private static void validate(ServerPlayer p,Trip t){
        var party=PartyManager.partyOf(p);
        if(party==null||!party.leader.equals(t.leader)||!party.members.contains(p.getUUID()))throw new IllegalArgumentException("Le groupe a changé.");
        var d=RpgPlayers.get(p);
        if(!p.isAlive()||!d.playerClass.isPlayable()||com.mmorpg.hub.ClassTrial.active(p)||com.mmorpg.hub.CinematicManager.isLocked(p)
                ||com.mmorpg.competition.ArenaManager.queued(p.getUUID())||com.mmorpg.competition.ArenaManager.session(p.getUUID())!=null||com.mmorpg.competition.GuildRaids.of(p.getUUID())!=null
                ||NetworkPlayers.inTransit(p)||com.mmorpg.dungeon.InstanceManager.containsPlayer(p)||d.pvpUntilTick>p.level().getServer().getTickCount())throw new IllegalArgumentException(p.getName().getString()+" n’est pas disponible.");
        int required;
        if(t.kind.equals("mine")){int k=Integer.parseInt(t.value);if(k<0||k>=com.mmorpg.mine.MineLayout.SECTORS||k>=d.minesUnlocked)throw new IllegalArgumentException(p.getName().getString()+" n’a pas débloqué cette mine.");required=com.mmorpg.mine.MineLayout.STYLES.get(k).minLevel();}
        else if(t.kind.equals("plaines")){int k=Integer.parseInt(t.value);if(k<0||k>=com.mmorpg.plains.PlainsLayout.REGIONS||k>=d.plainsUnlocked)throw new IllegalArgumentException(p.getName().getString()+" n’a pas débloqué cette plaine.");required=com.mmorpg.plains.PlainsLayout.STYLES.get(k).minLevel();}
        else required=com.mmorpg.dungeon.DungeonMaster.requiredLevel(com.mmorpg.dungeon.InstanceManager.typeOf(t.value),com.mmorpg.dungeon.InstanceManager.difficultyOf(t.value));
        if(d.level<required)throw new IllegalArgumentException(p.getName().getString()+" doit être niveau "+required+".");
        if(NetworkConfig.current.enabled&&!NetworkPlayers.canTransfer(p,role(t)))throw new IllegalArgumentException(p.getName().getString()+" ne peut pas voyager.");
    }
    private static void dispatch(ServerPlayer p,Trip t){
        individual=true;
        try{PortalAccess.grant(p,t.kind);switch(t.kind){
            case "mine"->com.mmorpg.mine.MineManager.startTransition(p,Integer.parseInt(t.value));
            case "plaines"->com.mmorpg.plains.PlainsManager.enterChosen(p,Integer.parseInt(t.value));
            default->com.mmorpg.dungeon.InstanceManager.enterPool(p,t.value);
        }}finally{individual=false;}
    }
    public static void tick(MinecraftServer server){
        if(!NetworkConfig.current.enabled||server.getTickCount()%10!=0)return;
        try {
            SharedParties.refresh();var departures=new ArrayList<Map.Entry<ServerPlayer,Trip>>();
            SharedLedger.access(server,"group-travel",State.class,State::new,s->{
                long now=System.currentTimeMillis();
                for(var t:s.trips){
                    if(t.expires<now){if(!t.committed)t.error="Un membre ne répond pas : aucun départ effectué.";}
                    for(var p:server.getPlayerList().getPlayers())if(t.members.contains(p.getUUID())&&!t.sent.contains(p.getUUID())){
                        if(!t.committed&&t.error.isEmpty())try{validate(p,t);t.ready.add(p.getUUID());}catch(Exception e){t.error=e.getMessage();}
                    }
                    if(!t.committed&&t.error.isEmpty()&&t.ready.containsAll(t.members)){t.committed=true;t.expires=now+45000;}
                    for(var p:server.getPlayerList().getPlayers())if(t.members.contains(p.getUUID())&&!t.sent.contains(p.getUUID())){
                        if(!t.error.isEmpty()){p.sendSystemMessage(Component.literal("Départ du groupe annulé : "+t.error));t.sent.add(p.getUUID());}
                        else if(t.committed&&t.expires>=now){departures.add(Map.entry(p,t));t.sent.add(p.getUUID());}
                    }
                }
                s.trips.removeIf(t->t.expires<now||t.sent.containsAll(t.members));return null;
            });
            for(var entry:departures)dispatch(entry.getKey(),entry.getValue());
        } catch(Exception e){com.mmorpg.MMORPG.LOGGER.error("Group departure coordination",e);}
    }
}
