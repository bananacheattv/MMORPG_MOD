package com.mmorpg.guild;

import com.google.gson.GsonBuilder;
import com.mmorpg.network.NetworkConfig;
import net.minecraft.server.MinecraftServer;
import java.nio.file.*;
import java.nio.channels.*;
import java.util.*;
import java.util.function.Function;

/** A single authoritative guild ledger for all backends. Never replaces corrupt data with an empty store. */
public final class GuildStore {
    public static final class Member {public UUID id;public String name,rank="MEMBRE";public long joined=System.currentTimeMillis();public Member(UUID id,String name,String rank){this.id=id;this.name=name;this.rank=rank;}}
    public static final class Guild {
        public UUID id=UUID.randomUUID();public String name,motto="";public int level=1;public long xp,gold;public int bosses;
        public LinkedHashMap<UUID,Member> members=new LinkedHashMap<>();public LinkedHashMap<UUID,Long> invites=new LinkedHashMap<>();public ArrayList<String> history=new ArrayList<>();
        public LinkedHashMap<String,Long> buffs=new LinkedHashMap<>();
        public void log(String message){history.add(0,java.time.Instant.now()+" · "+message);while(history.size()>60)history.removeLast();}
    }
    public static final class Transfer {public UUID id=UUID.randomUUID(),player,guild;public long delta;public String name;}
    public static final class State {public ArrayList<Guild> guilds=new ArrayList<>();public ArrayList<Transfer> pending=new ArrayList<>();public long revision;}
    private static final com.google.gson.Gson JSON=new GsonBuilder().setPrettyPrinting().create();
    public static Path directory(MinecraftServer server){return NetworkConfig.current.enabled?Path.of(NetworkConfig.current.sharedDirectory).toAbsolutePath().resolveSibling("guilds"):server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT).resolve("data/aeloria-guilds");}
    public static Guild of(State state,UUID player){return state.guilds.stream().filter(g->g.members.containsKey(player)).findFirst().orElse(null);}
    public static Guild byId(State state,UUID id){return state.guilds.stream().filter(g->g.id.equals(id)).findFirst().orElse(null);}
    public static void validate(State s){
        if(s==null||s.guilds==null||s.pending==null)throw new IllegalStateException("Registre de guildes invalide");
        Set<UUID> ids=new HashSet<>(),players=new HashSet<>();Set<String> names=new HashSet<>();
        for(var g:s.guilds){if(g==null||g.id==null||!ids.add(g.id)||g.name==null||!names.add(g.name.toLowerCase(Locale.ROOT))||g.level<1||g.level>10||g.gold<0||g.xp<0||g.members==null||g.members.isEmpty()||g.members.size()>50||g.history==null||g.invites==null||g.members.entrySet().stream().anyMatch(e->e.getValue()==null||!e.getKey().equals(e.getValue().id)))throw new IllegalStateException("Guilde invalide");
            if(g.buffs==null||g.buffs.size()>4)throw new IllegalStateException("Buffs invalides");for(var e:g.buffs.entrySet()){GuildBuffs.Kind.valueOf(e.getKey());if(e.getValue()==null||e.getValue()<0)throw new IllegalStateException("Durée de buff invalide");}
            int leaders=0;for(var m:g.members.values()){if(m==null||m.id==null||!players.add(m.id)||!List.of("CHEF","OFFICIER","MEMBRE").contains(m.rank))throw new IllegalStateException("Membre invalide");if(m.rank.equals("CHEF"))leaders++;}if(leaders!=1)throw new IllegalStateException("Chef invalide");}
        Set<UUID> tx=new HashSet<>();for(var t:s.pending)if(t==null||t.id==null||!tx.add(t.id)||t.player==null||byId(s,t.guild)==null||t.delta==0||Math.abs(t.delta)>1_000_000_000)throw new IllegalStateException("Transaction invalide");
    }
    private static State read(Path file)throws java.io.IOException{if(!Files.exists(file))return new State();try{var s=JSON.fromJson(Files.readString(file),State.class);validate(s);return s;}catch(RuntimeException e){throw new java.io.IOException("Registre de guildes illisible : intervention administrateur nécessaire",e);}}
    private static void write(Path file,State state)throws java.io.IOException{
        validate(state);state.revision++;Path temp=Files.createTempFile(file.getParent(),"guilds-",".tmp");
        try{Files.writeString(temp,JSON.toJson(state));try(var c=FileChannel.open(temp,StandardOpenOption.WRITE)){c.force(true);}if(Files.exists(file))Files.copy(file,file.resolveSibling("guilds.json.bak"),StandardCopyOption.REPLACE_EXISTING);Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}finally{Files.deleteIfExists(temp);}
    }
    public static synchronized <T>T access(MinecraftServer server,boolean write,Function<State,T> action){
        try{Path dir=directory(server);Files.createDirectories(dir);try(var channel=FileChannel.open(dir.resolve("guilds.lock"),StandardOpenOption.CREATE,StandardOpenOption.WRITE)){
            FileLock lock=null;long until=System.nanoTime()+500_000_000L;while(lock==null&&System.nanoTime()<until){try{lock=channel.tryLock();}catch(OverlappingFileLockException ignored){}if(lock==null)Thread.sleep(5);}if(lock==null)throw new java.io.IOException("Registre occupé");
            try{Path file=dir.resolve("guilds.json");State state=read(file);T result=action.apply(state);if(write)write(file,state);return result;}finally{lock.release();}
        }}catch(java.io.IOException e){throw new IllegalStateException(e.getMessage(),e);}catch(InterruptedException e){Thread.currentThread().interrupt();throw new IllegalStateException("Opération interrompue",e);}
    }
}
