package com.mmorpg.competition;

import com.mmorpg.network.*;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;
import java.util.function.Function;

/** Registration and ready checks stay on the player's current server. Only a full, accepted roster travels. */
public final class SharedArenaQueue {
    public static final class Entry {
        public UUID player; public ArenaRules.Kind kind; public int bracket,level; public long seen;
        public Entry(ServerPlayer p,ArenaRules.Kind k,int b){player=p.getUUID();kind=k;bracket=b;level=RpgPlayers.get(p).level;seen=System.currentTimeMillis();}
    }
    public static final class Match {
        public UUID id=UUID.randomUUID(); public ArenaRules.Kind kind; public int bracket;
        public LinkedHashMap<UUID,Integer> teams=new LinkedHashMap<>(); public Set<UUID> accepted=new HashSet<>(),arrived=new HashSet<>();
        public long deadline; public boolean travel,started;
    }
    public static final class State { public LinkedHashMap<UUID,Entry> queue=new LinkedHashMap<>(); public ArrayList<Match> matches=new ArrayList<>(); }
    private static State cached=new State();
    private static final Map<UUID,UUID> prompted=new HashMap<>(),dispatched=new HashMap<>();
    private static <T>T access(MinecraftServer server,Function<State,T> action){return SharedLedger.access(server,"arena-queue",State.class,State::new,action);}
    private static Match match(State s,UUID player){return s.matches.stream().filter(m->m.teams.containsKey(player)).findFirst().orElse(null);}
    public static boolean queued(UUID player){return cached.queue.containsKey(player)||match(cached,player)!=null;}
    public static int count(ArenaRules.Kind kind,int bracket,UUID player){var own=match(cached,player);if(own!=null&&own.kind==kind)return own.teams.size();return (int)cached.queue.values().stream().filter(e->e.kind==kind&&e.bracket==bracket).count();}
    public static String kind(UUID player){var own=match(cached,player);if(own!=null)return own.kind.name();var entry=cached.queue.get(player);return entry==null?"":entry.kind.name();}
    public static String status(UUID player){var own=match(cached,player);if(own==null)return cached.queue.containsKey(player)?"Inscrit : tu restes sur place jusqu’au groupe complet.":"";return own.started?"Arène en cours.":own.travel?"Groupe confirmé : transfert en préparation.":"Groupe complet · "+own.accepted.size()+" / "+own.teams.size()+" confirmations.";}
    public static void join(ServerPlayer p,ArenaRules.Kind kind,int bracket){
        cached=access(p.level().getServer(),s->{if(s.queue.containsKey(p.getUUID())||match(s,p.getUUID())!=null)throw new IllegalArgumentException("Tu es déjà inscrit.");s.queue.put(p.getUUID(),new Entry(p,kind,bracket));return s;});
    }
    public static boolean ready(ServerPlayer p,String id){
        return access(p.level().getServer(),s->{var m=match(s,p.getUUID());if(m==null||!m.id.toString().equals(id))return false;
            if(System.currentTimeMillis()>m.deadline||m.travel)throw new IllegalArgumentException("Confirmation expirée.");
            m.accepted.add(p.getUUID());return true;});
    }
    public static void leave(ServerPlayer p){cached=access(p.level().getServer(),s->{s.queue.remove(p.getUUID());var m=match(s,p.getUUID());if(m!=null&&!m.started)s.matches.remove(m);return s;});prompted.remove(p.getUUID());dispatched.remove(p.getUUID());}
    public static void finish(MinecraftServer server,UUID id){cached=access(server,s->{s.matches.removeIf(m->m.id.equals(id));return s;});}
    public static void arrive(ServerPlayer p,String id){
        cached=access(p.level().getServer(),s->{var m=match(s,p.getUUID());if(m==null||!m.id.toString().equals(id)||!m.travel||m.started||System.currentTimeMillis()>m.deadline)throw new IllegalArgumentException("Inscription expirée : retour à ton point de départ.");m.arrived.add(p.getUUID());return s;});
    }
    public static void tick(MinecraftServer server){
        if(!NetworkConfig.current.enabled||server.getTickCount()%20!=0)return;
        try {
            long now=System.currentTimeMillis();
            cached=access(server,s->{
                for(var p:server.getPlayerList().getPlayers()){var e=s.queue.get(p.getUUID());if(e!=null){if(ArenaRules.bracket(RpgPlayers.get(p).level)!=e.bracket||p.isCreative()||p.isSpectator())s.queue.remove(p.getUUID());else e.seen=now;}}
                s.queue.values().removeIf(e->now-e.seen>20000);
                s.matches.removeIf(m->now>m.deadline||(m.started&&NetworkConfig.hosts(NetworkConfig.Role.DUNGEONS)&&!ArenaManager.hasShared(m.id)));
                for(var kind:ArenaRules.Kind.values())for(int bracket=0;bracket<5;bracket++){
                    int b=bracket;if(s.matches.stream().anyMatch(m->m.kind==kind&&m.bracket==b))continue;
                    var roster=s.queue.values().stream().filter(e->e.kind==kind&&e.bracket==b).limit(kind.size*2).toList();
                    if(roster.size()!=kind.size*2)continue;
                    var m=new Match();m.kind=kind;m.bracket=b;m.deadline=now+30000;var levels=new LinkedHashMap<UUID,Integer>();
                    for(var e:roster){levels.put(e.player,e.level);s.queue.remove(e.player);}m.teams.putAll(ArenaRules.balance(kind,b,levels));s.matches.add(m);
                }
                for(var m:s.matches)if(!m.travel&&m.accepted.size()==m.kind.size*2){m.travel=true;m.deadline=now+120000;}
                return s;
            });
            for(var p:server.getPlayerList().getPlayers()){
                var m=match(cached,p.getUUID());
                if(m==null){boolean wasSent=dispatched.remove(p.getUUID())!=null;boolean wasPrompted=prompted.remove(p.getUUID())!=null;if(wasPrompted&&!wasSent)p.sendSystemMessage(net.minecraft.network.chat.Component.literal("Départ d’arène annulé : une confirmation manque ou un joueur s’est retiré. Tu peux te réinscrire."));if(wasSent&&NetworkConfig.hosts(NetworkConfig.Role.DUNGEONS)&&ArenaManager.session(p.getUUID())==null)CompetitionTravel.home(p);continue;}
                if(m.started)dispatched.remove(p.getUUID());
                if(!m.travel&&!m.id.equals(prompted.get(p.getUUID()))){
                    var t=new CompoundTag();t.putString("match",m.id.toString());t.putString("arena",m.kind.label);t.putInt("seconds",Math.max(1,(int)((m.deadline-now)/1000)));Net.toPlayer(p,new S2COpenScreen(S2COpenScreen.ARENA_READY,t));prompted.put(p.getUUID(),m.id);
                }
                if(m.travel&&!m.started&&!m.id.equals(dispatched.get(p.getUUID()))){
                    if(NetworkConfig.hosts(NetworkConfig.Role.DUNGEONS)){arrive(p,m.id.toString());dispatched.put(p.getUUID(),m.id);}
                    else if(NetworkPlayers.canTransfer(p,NetworkConfig.Role.DUNGEONS)){CompetitionTravel.route(p,"arena-match:"+m.id);if(NetworkPlayers.inTransit(p))dispatched.put(p.getUUID(),m.id);}
                }
            }
            if(NetworkConfig.hosts(NetworkConfig.Role.DUNGEONS))for(var m:cached.matches){
                if(!m.started&&m.travel&&m.arrived.size()==m.kind.size*2&&m.teams.keySet().stream().allMatch(id->server.getPlayerList().getPlayer(id)!=null)){
                    ArenaManager.startShared(server,m);
                    access(server,s->{var saved=s.matches.stream().filter(x->x.id.equals(m.id)).findFirst().orElseThrow();saved.started=true;saved.deadline=now+40*60*1000;return null;});
                }
            }
        } catch(Exception e){com.mmorpg.MMORPG.LOGGER.error("Arena shared queue",e);}
    }
}
