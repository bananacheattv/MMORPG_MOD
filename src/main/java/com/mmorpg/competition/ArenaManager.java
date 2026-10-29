package com.mmorpg.competition;

import com.mmorpg.network.*;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

public final class ArenaManager {
    public record Key(ArenaRules.Kind kind,int bracket){public int pool(){return kind.ordinal()*5+bracket;}}
    private static final Map<Key,LinkedHashSet<UUID>> QUEUES=new HashMap<>();
    public static final class Session {
        public final Key key;public final ArenaRules.Match match;public final Set<UUID> accepted=new HashSet<>();public final Map<UUID,Long> missing=new HashMap<>(),protectedUntil=new HashMap<>();
        public final Set<UUID> departed=new HashSet<>();
        public final Map<UUID,Return> returns=new HashMap<>();public long deadline;public boolean active,test;public UUID sharedId;public CompetitionWorld.Job build;public ArenaCrystal crystal;CrystalRoute route;public final ArenaDefence defence=new ArenaDefence();
        boolean readyToBuild(){return test||accepted.size()==key.kind.size*2;}
        boolean forfeited(){return match.teams.isEmpty()||(!test&&(!match.teams.containsValue(0)||!match.teams.containsValue(1)));}
        BlockPos center(){return CompetitionWorld.center(test?10+key.kind.ordinal()*8+key.bracket-5:key.pool());}
        Session(Key key,long tick){this.key=key;match=new ArenaRules.Match(key.kind,key.bracket);deadline=tick+600;}
    }
    public record Return(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim,double x,double y,double z,float yaw,float pitch){}
    static final Map<Key,Session> SESSIONS=new HashMap<>();private static MinecraftServer current;
    public static boolean hasShared(UUID id){return SESSIONS.values().stream().anyMatch(s->id.equals(s.sharedId));}
    public static Session session(UUID id){return SESSIONS.values().stream().filter(s->s.match.teams.containsKey(id)).findFirst().orElse(null);}
    public static boolean queued(UUID id){return SharedArenaQueue.queued(id)||QUEUES.values().stream().anyMatch(q->q.contains(id));}
    public static boolean active(ServerPlayer p){var s=session(p.getUUID());return s!=null&&s.active;}
    public static boolean enemies(net.minecraft.world.entity.player.Player a,net.minecraft.world.entity.player.Player b){var s=session(a.getUUID());return s!=null&&s.active&&s==session(b.getUUID())&&s.match.enemy(a.getUUID(),b.getUUID());}
    public static boolean ownSafe(ServerPlayer p,Session s){var team=s.match.teams.get(p.getUUID());if(team==null)return false;double x=p.getX()-s.center().getX(),z=p.getZ()-s.center().getZ();return Math.abs(z)<=10&&(team==0?x<=-36:x>=36);}
    public static boolean protectedPlayer(ServerPlayer p){var s=session(p.getUUID());return s!=null&&(!s.active||ownSafe(p,s)||s.protectedUntil.getOrDefault(p.getUUID(),0L)>p.level().getServer().getTickCount());}
    public static void open(ServerPlayer p,String status){var t=new CompoundTag();t.putInt("bracket",ArenaRules.bracket(RpgPlayers.get(p).level));t.putString("status",status.isBlank()?SharedArenaQueue.status(p.getUUID()):status);t.putString("queueKind",SharedArenaQueue.kind(p.getUUID()));var s=session(p.getUUID());t.putBoolean("active",s!=null&&s.active);t.putBoolean("queued",queued(p.getUUID())||s!=null);t.putString("match",s==null?"":s.match.id.toString());
        for(var kind:ArenaRules.Kind.values()){int count=NetworkConfig.current.enabled?SharedArenaQueue.count(kind,ArenaRules.bracket(RpgPlayers.get(p).level),p.getUUID()):QUEUES.getOrDefault(new Key(kind,ArenaRules.bracket(RpgPlayers.get(p).level)),new LinkedHashSet<>()).size();t.putInt(kind.name(),count);}Net.toPlayer(p,new S2COpenScreen(S2COpenScreen.ARENA,t));}
    public static void action(ServerPlayer p,CompoundTag t){try{switch(t.getStringOr("action","")){
        case "open"->open(p,"");case "join"->join(p,ArenaRules.Kind.valueOf(t.getStringOr("kind","")),t.getIntOr("bracket",-1));
        case "ready"->{if(NetworkConfig.current.enabled&&SharedArenaQueue.ready(p,t.getStringOr("match",""))){open(p,"Prêt : en attente des confirmations. Tu restes sur place.");return;}var s=session(p.getUUID());if(s!=null&&!s.active&&s.match.id.toString().equals(t.getStringOr("match",""))&&p.level().getServer().getTickCount()<=s.deadline){s.accepted.add(p.getUUID());open(p,"Prêt : "+s.accepted.size()+" / "+s.match.teams.size());}}
        case "leave"->{leave(p);open(p,"Inscription annulée / arène quittée.");}default->throw new IllegalArgumentException("Action inconnue.");}}catch(Exception e){open(p,e.getMessage());}}
    public static void join(ServerPlayer p,ArenaRules.Kind kind,int bracket){
        if(!com.mmorpg.server.FeatureFlags.allow(p,"arenes"))return;
        if(!RpgPlayers.get(p).playerClass.isPlayable())throw new IllegalArgumentException("Choisis d’abord ta classe.");
        if(p.isCreative()||p.isSpectator())throw new IllegalArgumentException("Passe en mode survie pour participer à une arène.");
        if(ArenaRules.bracket(RpgPlayers.get(p).level)!=bracket)throw new IllegalArgumentException("Cette arène ne correspond pas à ton niveau.");
        if(com.mmorpg.hub.ClassTrial.active(p)||session(p.getUUID())!=null||queued(p.getUUID())||GuildRaids.of(p.getUUID())!=null||com.mmorpg.dungeon.InstanceManager.containsPlayer(p))throw new IllegalArgumentException("Tu es déjà inscrit ou engagé dans une activité.");
        if(NetworkConfig.current.enabled){SharedArenaQueue.join(p,kind,bracket);open(p,"Inscrit : tu restes ici jusqu’à "+(kind.size*2)+" inscrits et leur confirmation.");return;}
        current=p.level().getServer();QUEUES.computeIfAbsent(new Key(kind,bracket),k->new LinkedHashSet<>()).add(p.getUUID());open(p,"Inscription confirmée : attente des autres joueurs.");
    }
    public static void startShared(MinecraftServer server,SharedArenaQueue.Match match){
        Key key=new Key(match.kind,match.bracket);
        if(SESSIONS.containsKey(key))throw new IllegalArgumentException("Cette arène est déjà occupée.");
        Session s=new Session(key,server.getTickCount());s.sharedId=match.id;s.match.teams.putAll(match.teams);s.accepted.addAll(match.teams.keySet());SESSIONS.put(key,s);
    }
    public static void test(ServerPlayer p,ArenaRules.Kind kind){
        CompetitionAdmin.require(p);CompetitionAdmin.available(p);
        if(CompetitionTravel.route(p,"admin-test:arena:"+kind.name()))return;
        current=p.level().getServer();int slot=5;while(slot<13&&SESSIONS.containsKey(new Key(kind,slot)))slot++;
        if(slot==13)throw new IllegalArgumentException("Les instances de test sont occupées.");
        var s=new Session(new Key(kind,slot),current.getTickCount());s.test=true;s.returns.put(p.getUUID(),new Return(p.level().dimension(),p.getX(),p.getY(),p.getZ(),p.getYRot(),p.getXRot()));s.match.teams.put(p.getUUID(),0);s.accepted.add(p.getUUID());SESSIONS.put(s.key,s);
        p.sendSystemMessage(Component.literal("Préparation du test solo : "+kind.label+". /aeloriatest equipe rouge|bleu ; /aeloriatest stop pour revenir."));
    }
    public static void testTeam(ServerPlayer p,int team){
        CompetitionAdmin.require(p);var s=session(p.getUUID());
        if(s==null||!s.test||!s.active)throw new IllegalArgumentException("Lance d’abord une arène de test et attends sa préparation.");
        s.match.teams.put(p.getUUID(),team);p.sendSystemMessage(Component.literal("Équipe de test : "+(team==0?"rouge":"bleue")+". Frappe le cristal pour le capturer."));
    }
    public static void leave(ServerPlayer p){if(NetworkConfig.current.enabled)SharedArenaQueue.leave(p);QUEUES.values().forEach(q->q.remove(p.getUUID()));var s=session(p.getUUID());if(s==null){if(CompetitionTravel.inside(p))CompetitionTravel.home(p);return;}if(s.active)PvpProgression.leaving(p,s);s.match.teams.remove(p.getUUID());s.accepted.remove(p.getUUID());restore(p,s);if(!NetworkConfig.current.enabled)PvpProgression.afterReturn(p);if(!s.active)cancel(s,p.level().getServer(),p.getUUID());}
    private static void popup(ServerPlayer p,Session s){var t=new CompoundTag();t.putString("match",s.match.id.toString());t.putString("arena",s.key.kind.label);t.putInt("seconds",30);Net.toPlayer(p,new S2COpenScreen(S2COpenScreen.ARENA_READY,t));}
    private static void cancel(Session s,MinecraftServer server,UUID excluded){SESSIONS.remove(s.key);if(s.sharedId!=null)SharedArenaQueue.finish(server,s.sharedId);for(UUID id:s.match.teams.keySet()){var p=server.getPlayerList().getPlayer(id);if(p!=null){if(s.sharedId==null&&!s.test&&!id.equals(excluded)&&s.accepted.contains(id))QUEUES.computeIfAbsent(s.key,k->new LinkedHashSet<>()).add(id);else if(NetworkConfig.current.enabled)CompetitionTravel.home(p);p.sendSystemMessage(Component.literal("Arène annulée : un joueur n’a pas confirmé. Les joueurs prêts sont réinscrits."));}}}
    private static void start(Session s,MinecraftServer server){
        var level=CompetitionWorld.level(server);s.active=true;s.match.started=server.getTickCount();var c=s.center();
        for(UUID id:s.match.teams.keySet()){var p=server.getPlayerList().getPlayer(id);if(p==null||(!s.test&&(ArenaRules.bracket(RpgPlayers.get(p).level)!=s.key.bracket||p.isCreative()||p.isSpectator()))){s.active=false;cancel(s,server,id);return;}}
        for(UUID id:s.match.teams.keySet()){var p=server.getPlayerList().getPlayer(id);s.returns.put(id,new Return(p.level().dimension(),p.getX(),p.getY(),p.getZ(),p.getYRot(),p.getXRot()));spawn(p,s);}
        s.crystal=com.mmorpg.registry.ModEntities.ARENA_CRYSTAL.get().create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);if(s.crystal==null)throw new IllegalStateException("Cristal indisponible");s.crystal.match=s.match.id;s.route=new CrystalRoute(level,c);var start=s.route.position();s.crystal.snapTo(start.x,start.y,start.z,0,0);level.addFreshEntity(s.crystal);s.defence.spawn(level,c);
    }
    private static void spawn(ServerPlayer p,Session s){s.departed.remove(p.getUUID());int team=s.match.teams.get(p.getUUID());CompetitionWorld.teleport(p,s.center().offset(team==0?-44:44,1,(int)(p.getUUID().getLeastSignificantBits()%7)),team==0?-90:90);RpgPlayers.fullRestore(p);p.setHealth(p.getMaxHealth());p.clearFire();p.removeAllEffects();s.protectedUntil.put(p.getUUID(),(long)p.level().getServer().getTickCount()+60);com.mmorpg.server.MountManager.despawn(p);}
    public static void defenceKill(ServerPlayer player,Session session){
        if(session(player.getUUID())!=session||!session.active)return;
        player.sendSystemMessage(Component.literal("Éliminé par les drones du camp ennemi."));spawn(player,session);
        Net.toPlayer(player,new Payloads.Notify(Payloads.Notify.INFO,"ÉLIMINÉ","Les drones protègent le camp ennemi. Restez hors de la zone rouge.",0xFF4040));
    }
    public static boolean lethal(ServerPlayer victim,net.minecraft.world.entity.Entity attacker){var s=session(victim.getUUID());if(s==null||!s.active)return false;if(attacker==null&&victim.getLastHurtByPlayer() instanceof ServerPlayer recent)attacker=recent;if(attacker instanceof ServerPlayer killer){if(!s.test&&s.match.enemy(killer.getUUID(),victim.getUUID()))com.mmorpg.achievement.Achievements.add(killer,com.mmorpg.achievement.Achievement.PVP_KILLS,1);s.match.kill(killer.getUUID(),victim.getUUID());}spawn(victim,s);return true;}
    public static boolean capture(ArenaCrystal crystal,ServerPlayer p){var s=session(p.getUUID());if(s==null||!s.active||s.crystal!=crystal||protectedPlayer(p))return false;if(!p.isAlive()||p.isSpectator()||p.level()!=crystal.level())return false;boolean hit=s.match.capture(p.getUUID(),p.level().getServer().getTickCount());crystal.setTeam(s.match.owner);crystal.setHits(s.match.captureHits);if(hit)p.level().broadcastEntityEvent(crystal,(byte)4);return hit;}
    private static void restore(ServerPlayer p,Session s){RpgPlayers.fullRestore(p);p.clearFire();p.removeAllEffects();var r=s.returns.remove(p.getUUID());if(NetworkConfig.current.enabled){CompetitionTravel.home(p);return;}if(r!=null){var l=p.level().getServer().getLevel(r.dim);if(l!=null){p.teleportTo(l,r.x,r.y,r.z,Set.of(),r.yaw,r.pitch,true);return;}}CompetitionTravel.home(p);}
    private static void finish(Session s,MinecraftServer server){PvpProgression.finish(s,server);if(s.sharedId!=null)SharedArenaQueue.finish(server,s.sharedId);s.defence.dispose();if(s.crystal!=null)s.crystal.discard();SESSIONS.remove(s.key);int winner=s.match.winner();for(UUID id:new ArrayList<>(s.match.teams.keySet())){var p=server.getPlayerList().getPlayer(id);if(p!=null){p.sendSystemMessage(Component.literal("Fin de l’arène : "+(winner<0?"égalité":winner==0?"victoire rouge":"victoire bleue")+" · "+s.match.red+" / "+s.match.blue));restore(p,s);Net.toPlayer(p,new S2COpenScreen(S2COpenScreen.ARENA_HUD,new CompoundTag()));if(!NetworkConfig.current.enabled&&s.match.ended)PvpProgression.afterReturn(p);}}}
    public static void tick(MinecraftServer server){
        SharedArenaQueue.tick(server);
        current=server;long tick=server.getTickCount();if(!NetworkConfig.hosts(NetworkConfig.Role.DUNGEONS))return;
        for(var key:new ArrayList<>(QUEUES.keySet())){var q=QUEUES.get(key);q.removeIf(id->{var p=server.getPlayerList().getPlayer(id);return p==null||ArenaRules.bracket(RpgPlayers.get(p).level)!=key.bracket;});
            if(!SESSIONS.containsKey(key)&&q.size()>=key.kind.size*2){Session s=new Session(key,tick);List<ServerPlayer> players=new ArrayList<>();for(UUID id:new ArrayList<>(q)){players.add(server.getPlayerList().getPlayer(id));q.remove(id);if(players.size()==key.kind.size*2)break;}var levels=new LinkedHashMap<UUID,Integer>();for(var p:players)levels.put(p.getUUID(),RpgPlayers.get(p).level);s.match.teams.putAll(ArenaRules.balance(key.kind,key.bracket,levels));SESSIONS.put(key,s);for(var p:players)popup(p,s);}}
        for(Session s:new ArrayList<>(SESSIONS.values())){
            if(!s.active){if(s.match.teams.keySet().stream().anyMatch(id->server.getPlayerList().getPlayer(id)==null)){cancel(s,server,null);continue;}if(s.readyToBuild()){if(s.build==null)s.build=CompetitionWorld.arena(s.center(),s.key.kind);s.build.tick(CompetitionWorld.level(server));if(s.build.done())start(s,server);}else if(tick>s.deadline)cancel(s,server,null);continue;}
            for(UUID id:new ArrayList<>(s.match.teams.keySet())){var p=server.getPlayerList().getPlayer(id);if(p==null){s.missing.putIfAbsent(id,tick);if(tick-s.missing.get(id)>600)s.match.teams.remove(id);}else{s.missing.remove(id);
                if(!ownSafe(p,s))s.departed.add(id);else if(s.departed.contains(id)){int team=s.match.teams.get(id);CompetitionWorld.teleport(p,s.center().offset(team==0?-34:34,1,(int)Math.clamp(p.getZ()-s.center().getZ(),-9,9)),team==0?-90:90);p.sendOverlayMessage(Component.literal("Le camp de départ ne peut plus être rejoint après sa sortie."));}
                if(!CompetitionTravel.inside(p)||Math.abs(p.getX()-s.center().getX())>54||Math.abs(p.getZ()-s.center().getZ())>40||p.getY()<97)spawn(p,s);}}
            boolean red=s.match.teams.containsValue(0),blue=s.match.teams.containsValue(1);if(s.forfeited()){s.match.red=red?500:0;s.match.blue=blue?500:0;s.match.ended=true;}
            if(com.mmorpg.server.FeatureFlags.enabled(server,"drones")){if(s.defence.drones.isEmpty())s.defence.spawn(CompetitionWorld.level(server),s.center());s.defence.tick(s,server,tick);}else if(!s.defence.drones.isEmpty())s.defence.dispose();s.match.tick(tick);if(tick-s.match.started>36000)s.match.ended=true;
            if(s.crystal!=null&&!s.crystal.isRemoved()){if(s.route==null)s.route=new CrystalRoute(CompetitionWorld.level(server),s.center());s.crystal.setPos(s.route.advance(tick));}
            if(tick%10==0)for(UUID id:s.match.teams.keySet()){var p=server.getPlayerList().getPlayer(id);if(p!=null){var hud=new CompoundTag();hud.putBoolean("active",true);hud.putInt("red",s.match.red);hud.putInt("blue",s.match.blue);hud.putInt("owner",s.match.owner);hud.putInt("hits",s.match.captureHits);hud.putInt("team",s.match.teams.get(id));hud.putInt("danger",s.defence.dangerRemaining(id,tick));Net.toPlayer(p,new S2COpenScreen(S2COpenScreen.ARENA_HUD,hud));}}
            if(s.match.ended)finish(s,server);
        }
    }
    public static void forceWin(ServerPlayer admin,ServerPlayer participant,int team){
        if(!com.mmorpg.server.CreativeAccess.allowed(admin))throw new IllegalArgumentException("Réservé aux opérateurs.");var s=session(participant.getUUID());if(s==null||!s.active)throw new IllegalArgumentException("Ce joueur n’est pas dans une arène active.");if(team<0||team>1)throw new IllegalArgumentException("Équipe invalide.");if(team==0){s.match.red=500;s.match.blue=Math.min(499,s.match.blue);}else{s.match.blue=500;s.match.red=Math.min(499,s.match.red);}s.match.ended=true;finish(s,admin.level().getServer());
    }
    public static void shutdown(MinecraftServer server){for(var s:new ArrayList<>(SESSIONS.values()))finish(s,server);QUEUES.clear();SESSIONS.clear();}
}
