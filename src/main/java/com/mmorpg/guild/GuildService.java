package com.mmorpg.guild;

import com.mmorpg.MMORPG;
import com.mmorpg.network.*;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class GuildService {
    private static final Map<UUID,Long> XP=new HashMap<>();
    private static final Map<UUID,long[]> PROGRESS=new HashMap<>();
    private static final Set<UUID> GREETED=new HashSet<>();
    private static final Map<UUID,Set<String>> NOTIFIED_INVITES=new HashMap<>();
    private static final class Presence {long time;ArrayList<GuildStore.Member> players=new ArrayList<>();}
    public static void presence(net.minecraft.server.MinecraftServer server){try{var snapshot=new Presence();snapshot.time=System.currentTimeMillis();for(var p:server.getPlayerList().getPlayers())snapshot.players.add(new GuildStore.Member(com.mmorpg.character.CharKey.of(p),com.mmorpg.character.CharKey.name(p),"MEMBRE"));var dir=GuildStore.directory(server);java.nio.file.Files.createDirectories(dir);var file=dir.resolve("presence-"+NetworkConfig.current.role+".json");var tmp=java.nio.file.Files.createTempFile(dir,"presence-",".tmp");try{java.nio.file.Files.writeString(tmp,new com.google.gson.Gson().toJson(snapshot));java.nio.file.Files.move(tmp,file,java.nio.file.StandardCopyOption.ATOMIC_MOVE,java.nio.file.StandardCopyOption.REPLACE_EXISTING);}finally{java.nio.file.Files.deleteIfExists(tmp);}}catch(Exception e){MMORPG.LOGGER.warn("Guild presence unavailable: {}",e.getMessage());}}
    public static List<GuildStore.Member> online(net.minecraft.server.MinecraftServer server){var all=new LinkedHashMap<UUID,GuildStore.Member>();try(var files=java.nio.file.Files.list(GuildStore.directory(server))){for(var file:files.filter(f->f.getFileName().toString().startsWith("presence-")&&f.toString().endsWith(".json")).toList()){try{var snapshot=new com.google.gson.Gson().fromJson(java.nio.file.Files.readString(file),Presence.class);if(snapshot!=null&&snapshot.time>System.currentTimeMillis()-15000)for(var m:snapshot.players)all.put(m.id,m);}catch(Exception ignored){}}}catch(Exception ignored){}for(var p:server.getPlayerList().getPlayers())all.put(com.mmorpg.character.CharKey.of(p),new GuildStore.Member(com.mmorpg.character.CharKey.of(p),com.mmorpg.character.CharKey.name(p),"MEMBRE"));return new ArrayList<>(all.values());}
    /** Suppression d'un personnage : il quitte sa guilde (refus s'il en est le chef et qu'il reste d'autres membres ou de l'or en banque). */
    public static void leaveForDelete(net.minecraft.server.MinecraftServer server,UUID key){GuildStore.access(server,true,s->{var g=GuildStore.of(s,key);if(g==null)return null;var m=g.members.get(key);
        if(m.rank.equals("CHEF")){require(g.members.size()==1,"Ce personnage est chef de guilde : transfère la direction avant de le supprimer.");require(g.gold==0&&s.pending.stream().noneMatch(t->t.guild.equals(g.id)),"Vide la banque de la guilde avant de supprimer ce personnage.");s.guilds.remove(g);}
        else{g.members.remove(key);g.log(m.name+" quitte la guilde.");}return null;});}
    public static void require(boolean test,String message){if(!test)throw new IllegalArgumentException(message);}
    private static GuildStore.Guild member(GuildStore.State s,ServerPlayer p){var g=GuildStore.of(s,com.mmorpg.character.CharKey.of(p));require(g!=null,"Tu n’as pas de guilde.");return g;}
    private static void officer(GuildStore.Guild g,UUID id){require(!g.members.get(id).rank.equals("MEMBRE"),"Réservé au chef et aux officiers.");}
    private static void chief(GuildStore.Guild g,UUID id){require(g.members.get(id).rank.equals("CHEF"),"Réservé au chef de guilde.");}
    public static void open(ServerPlayer p){open(p,"");}
    public static void open(ServerPlayer p,String status){open(p,status,-1);}
    public static void open(ServerPlayer p,String status,int tab){
        try{var t=GuildStore.access(p.level().getServer(),false,s->{var data=new CompoundTag();data.putString("status",status);var g=GuildStore.of(s,com.mmorpg.character.CharKey.of(p));
            data.putString("self",com.mmorpg.character.CharKey.of(p).toString());data.putLong("wallet",RpgPlayers.get(p).gold);if(tab>=0)data.putInt("tab",tab);if(g!=null){GuildExtras.roll(g);data.putInt("vaultCount",GuildExtras.vaultCount(g));}
            data.putString("guild",g==null?"":publicJson(g));
            var invites=new ListTag();for(var x:s.guilds)if(x.invites.getOrDefault(com.mmorpg.character.CharKey.of(p),0L)>System.currentTimeMillis()){var v=new CompoundTag();v.putString("id",x.id.toString());v.putString("name",x.name);invites.add(v);}data.put("invites",invites);
            var players=new ListTag();for(var player:online(p.level().getServer())){var v=new CompoundTag();v.putString("id",player.id.toString());v.putString("name",player.name);players.add(v);}data.put("online",players);return data;});
            Net.toPlayer(p,new S2COpenScreen(S2COpenScreen.GUILD,t));
        }catch(Exception e){p.sendSystemMessage(Component.literal("Guildes indisponibles : "+e.getMessage()));}
    }
    /** JSON envoyé au client : le contenu du coffre reste côté serveur. */
    private static String publicJson(GuildStore.Guild g){var gson=new com.google.gson.Gson();var tree=gson.toJsonTree(g).getAsJsonObject();tree.add("vault",new com.google.gson.JsonArray());return gson.toJson(tree);}
    public static void action(ServerPlayer p,CompoundTag data){
        try{
            String action=data.getStringOr("action","");if(!action.equals("raidLeave")&&!com.mmorpg.server.FeatureFlags.allow(p,"guildes"))return;if(action.equals("open")){reconcile(p);open(p);return;}
            require(!com.mmorpg.hub.ClassTrial.active(p),"Termine d’abord ton essai de classe.");
            require(!NetworkPlayers.inTransit(p),"Changement de serveur en cours.");
            if(action.equals("deposit")||action.equals("withdraw")){transfer(p,data.getLongOr("amount",0),action.equals("withdraw"));open(p,"Transaction enregistrée.");return;}
            if(action.equals("hall")){GuildHall.enter(p);return;}
            if(action.equals("boss")){com.mmorpg.competition.GuildRaids.enter(p,data.getIntOr("tier",-1));return;}
            if(action.equals("raidLeave")){com.mmorpg.competition.GuildRaids.leave(p);open(p);return;}
            if(action.equals("shop")||action.equals("vault")||action.equals("vaultDeposit")){
                UUID key=com.mmorpg.character.CharKey.of(p);var found=GuildStore.access(p.level().getServer(),false,s->{var x=GuildStore.of(s,key);require(x!=null,"Tu n’as pas de guilde.");return x;});
                if(action.equals("shop")){GuildExtras.buy(p,data.getIntOr("item",-1),found);open(p,"Achat effectué.");}
                else if(action.equals("vault"))GuildExtras.openVault(p,found,!found.members.get(key).rank.equals("MEMBRE"));
                else{GuildExtras.depositHand(p,found);open(p,"Objet déposé dans le coffre.");}
                return;
            }
            reconcile(p);
            GuildStore.access(p.level().getServer(),true,s->{
                var g=GuildStore.of(s,com.mmorpg.character.CharKey.of(p));UUID self=com.mmorpg.character.CharKey.of(p);
                if(action.equals("create")){
                    require(g==null,"Tu appartiens déjà à une guilde.");String name=data.getStringOr("name","").trim();require(name.matches("[\\p{L}0-9 _'-]{3,28}"),"Nom : 3 à 28 lettres, chiffres ou espaces.");require(s.guilds.stream().noneMatch(x->x.name.equalsIgnoreCase(name)),"Nom déjà utilisé.");
                    g=new GuildStore.Guild();g.name=name;g.members.put(self,new GuildStore.Member(self,com.mmorpg.character.CharKey.name(p),"CHEF"));g.log("Guilde fondée par "+p.getName().getString());s.guilds.add(g);return null;
                }
                if(action.equals("accept")||action.equals("decline")){
                    var invited=GuildStore.byId(s,UUID.fromString(data.getStringOr("target","")));require(invited!=null&&invited.invites.getOrDefault(self,0L)>System.currentTimeMillis(),"Invitation expirée.");
                    if(action.equals("accept")){require(g==null,"Quitte d’abord ta guilde.");require(invited.members.size()<50,"Guilde complète (50 membres).");invited.members.put(self,new GuildStore.Member(self,com.mmorpg.character.CharKey.name(p),"MEMBRE"));invited.log(com.mmorpg.character.CharKey.name(p)+" rejoint la guilde.");}invited.invites.remove(self);return null;
                }
                g=member(s,p);GuildExtras.roll(g);
                switch(action){
                    case "invite"->{officer(g,self);g.invites.values().removeIf(time->time<System.currentTimeMillis());UUID target=UUID.fromString(data.getStringOr("target",""));require(GuildStore.of(s,target)==null,"Ce joueur appartient déjà à une guilde.");require(g.members.size()<50,"Guilde complète.");require(g.invites.size()<100,"Trop d’invitations en attente.");g.invites.put(target,System.currentTimeMillis()+86400000L);g.log("Invitation envoyée par "+p.getName().getString());}
                    case "leave"->{require(!g.members.get(self).rank.equals("CHEF"),"Transfère la direction ou dissous la guilde.");g.members.remove(self);g.activity.remove(self);g.log(p.getName().getString()+" quitte la guilde.");}
                    case "kick","promote","demote","leader"->{UUID target=UUID.fromString(data.getStringOr("target",""));var m=g.members.get(target);require(m!=null&&!target.equals(self),"Membre invalide.");
                        if(action.equals("kick")){officer(g,self);require(!m.rank.equals("CHEF")&&(g.members.get(self).rank.equals("CHEF")||m.rank.equals("MEMBRE")),"Rang insuffisant.");g.members.remove(target);g.activity.remove(target);g.log(m.name+" exclu par "+p.getName().getString());}
                        else{chief(g,self);if(action.equals("leader")){g.members.get(self).rank="OFFICIER";m.rank="CHEF";}else{require(!m.rank.equals("CHEF"),"Rang invalide.");m.rank=action.equals("promote")?"OFFICIER":"MEMBRE";}g.log("Rang de "+m.name+" : "+m.rank);}}
                    case "banner"->{officer(g,self);String banner=data.getStringOr("banner","");require(GuildCrest.COLORS.contains(banner),"Couleur inconnue.");g.banner=banner;g.log("Nouvelle couleur principale : "+banner);}
                    case "banner2"->{officer(g,self);String banner=data.getStringOr("banner","");require(GuildCrest.COLORS.contains(banner),"Couleur inconnue.");g.banner2=banner;g.log("Nouvelle couleur d’emblème : "+banner);}
                    case "bannerPattern"->{officer(g,self);String pattern=data.getStringOr("pattern","");require(GuildCrest.PATTERNS.contains(pattern),"Emblème inconnu.");g.bannerPattern=pattern;g.log("Emblème : "+pattern);}
                    case "hallUpgrade"->{officer(g,self);require(g.hallLevel<3,"Hall au niveau maximum.");long cost=5000L*(g.hallLevel+1);require(g.gold>=cost,"Banque insuffisante : "+cost+" or.");g.gold-=cost;g.hallLevel++;g.log("Hall amélioré au niveau "+g.hallLevel);}
                    case "motto"->{officer(g,self);String text=data.getStringOr("text","").trim();require(text.length()<=120,"Devise limitée à 120 caractères.");g.motto=text;}
                    case "motd"->{officer(g,self);String text=data.getStringOr("text","").trim();require(text.length()<=160,"Message du jour limité à 160 caractères.");g.motd=text;g.log(text.isEmpty()?"Message du jour retiré par "+p.getName().getString():"Message du jour modifié par "+p.getName().getString());}
                    case "pin"->{officer(g,self);String text=data.getStringOr("text","").trim();require(!text.isEmpty()&&text.length()<=200,"Annonce : 1 à 200 caractères.");require(g.pins.size()<3,"3 annonces épinglées au maximum : retire-en une.");g.pins.add(g.members.get(self).name+" : "+text);g.log("Annonce épinglée par "+p.getName().getString());}
                    case "unpin"->{officer(g,self);int index=data.getIntOr("index",-1);require(index>=0&&index<g.pins.size(),"Annonce inconnue.");g.pins.remove(index);g.log("Annonce retirée par "+p.getName().getString());}
                    case "upgrade"->{officer(g,self);Yggdrasil.advance(g);}
                    case "buff"->{officer(g,self);GuildBuffs.activate(g,GuildBuffs.Kind.valueOf(data.getStringOr("buff","")),System.currentTimeMillis());}
                    case "disband"->{chief(g,self);require(data.getStringOr("confirm","").equals(g.name),"Confirme avec le nom exact de la guilde.");UUID guildId=g.id;require(g.gold==0&&s.pending.stream().noneMatch(t->t.guild.equals(guildId)),"Vide la banque avant la dissolution.");s.guilds.remove(g);}
                    default->throw new IllegalArgumentException("Action inconnue.");
                }GuildExtras.trophies(g);return null;
            });refresh(p);open(p,"Guilde mise à jour.");
        }catch(Exception e){open(p,e.getMessage()==null?"Opération refusée.":e.getMessage());}
    }
    private static boolean saveWallet(ServerPlayer p){if(NetworkConfig.current.enabled)return NetworkPlayers.save(p);p.level().getServer().getPlayerList().saveAll();return true;}
    /** Write intent, save an idempotent player receipt, then settle the bank. A reconnect completes interrupted transfers. */
    private static void transfer(ServerPlayer p,long amount,boolean withdraw){
        require(amount>0&&amount<=1_000_000_000L,"Montant entre 1 et 1 000 000 000 or.");reconcile(p);
        GuildStore.access(p.level().getServer(),true,s->{var g=member(s,p);if(withdraw){chief(g,com.mmorpg.character.CharKey.of(p));require(g.gold>=amount,"Banque insuffisante.");g.gold-=amount;}else require(RpgPlayers.get(p).gold>=amount,"Or personnel insuffisant.");
            var t=new GuildStore.Transfer();t.guild=g.id;t.player=com.mmorpg.character.CharKey.of(p);t.name=com.mmorpg.character.CharKey.name(p);t.delta=withdraw?amount:-amount;s.pending.add(t);return null;});reconcile(p);
    }
    /** Dépôt d'or en banque de guilde avec reçu idempotent (utilisé par la boutique). */
    public static void deposit(ServerPlayer p,long amount){transfer(p,amount,false);}
    public static void reconcile(ServerPlayer p){
        var pending=GuildStore.access(p.level().getServer(),false,s->s.pending.stream().filter(t->t.player.equals(com.mmorpg.character.CharKey.of(p))).toList());
        for(var t:pending){var d=RpgPlayers.get(p);String key=t.id.toString();if(!d.guildReceipts.getBooleanOr(key,false)){
            if(t.delta<0&&d.gold<-t.delta)throw new IllegalStateException("Transaction en attente : conserve l’or du dépôt.");
            long old=d.gold;d.gold=Math.addExact(old,t.delta);d.guildReceipts.putBoolean(key,true);d.dirty=true;
            if(!saveWallet(p)){d.gold=old;d.guildReceipts.remove(key);throw new IllegalStateException("Sauvegarde impossible : transaction en attente.");}
        }
        GuildStore.access(p.level().getServer(),true,s->{var present=s.pending.stream().filter(x->x.id.equals(t.id)).findFirst().orElse(null);if(present!=null){var g=GuildStore.byId(s,t.guild);require(g!=null,"Guilde indisponible.");if(t.delta<0){g.gold=Math.addExact(g.gold,-t.delta);GuildExtras.add(g,t.player,"GOLD",-t.delta);}g.log(t.name+(t.delta<0?" dépose ":" retire ")+Math.abs(t.delta)+" or");s.pending.remove(present);}return null;});}
    }
    public static void earned(ServerPlayer p,long amount){if(amount>0){XP.merge(com.mmorpg.character.CharKey.of(p),Math.max(1,amount/10),Long::sum);progress(p,"XP",amount);}}
    /** Progression des quêtes de guilde (monstres, boss, XP) : cumulée en mémoire puis écrite par {@link #flush}. */
    public static void progress(ServerPlayer p,String type,long amount){int i=Arrays.asList(GuildExtras.TYPES).indexOf(type);if(i>=0&&amount>0)PROGRESS.computeIfAbsent(com.mmorpg.character.CharKey.of(p),k->new long[GuildExtras.TYPES.length])[i]+=amount;}
    public static void left(ServerPlayer p){GREETED.remove(com.mmorpg.character.CharKey.of(p));seen(p);}
    public static void seen(ServerPlayer p){try{GuildStore.access(p.level().getServer(),true,s->{var g=GuildStore.of(s,com.mmorpg.character.CharKey.of(p));if(g!=null)GuildExtras.touch(g,com.mmorpg.character.CharKey.of(p),System.currentTimeMillis());return null;});}catch(Exception e){MMORPG.LOGGER.debug("Guild presence not recorded: {}",e.getMessage());}}
    public static void flush(net.minecraft.server.MinecraftServer server){GuildExtras.announce(server);if(XP.isEmpty()&&PROGRESS.isEmpty())return;var pending=new HashMap<>(XP);var quests=new HashMap<UUID,long[]>();PROGRESS.forEach((k,v)->quests.put(k,v.clone()));
        try{GuildStore.access(server,true,s->{pending.forEach((id,xp)->{var g=GuildStore.of(s,id);if(g!=null&&g.level<10)g.xp=Math.min(1_000_000_000L,g.xp+xp);});
            quests.forEach((id,values)->{var g=GuildStore.of(s,id);if(g!=null)for(int i=0;i<values.length;i++)GuildExtras.add(g,id,GuildExtras.TYPES[i],values[i]);});return null;});
            pending.forEach((id,n)->XP.computeIfPresent(id,(k,v)->v<=n?null:v-n));
            quests.forEach((id,values)->PROGRESS.computeIfPresent(id,(k,v)->{for(int i=0;i<v.length;i++)v[i]=Math.max(0,v[i]-values[i]);return java.util.Arrays.stream(v).sum()==0?null:v;}));}
        catch(Exception e){MMORPG.LOGGER.error("Guild XP retained for retry",e);}}
    private record Membership(int level,long[] buffs){}
    public static void refresh(ServerPlayer p){try{var membership=GuildStore.access(p.level().getServer(),false,s->{var g=GuildStore.of(s,com.mmorpg.character.CharKey.of(p));
            var seen=NOTIFIED_INVITES.computeIfAbsent(com.mmorpg.character.CharKey.of(p),id->new HashSet<>());var pending=new HashSet<String>();
            if(g!=null&&GREETED.add(com.mmorpg.character.CharKey.of(p))){if(!g.motd.isEmpty())p.sendSystemMessage(Component.literal("["+g.name+"] Message du jour : "+g.motd).withColor(0xF2D26B));for(var pin:g.pins)p.sendSystemMessage(Component.literal("["+g.name+"] Annonce — "+pin).withColor(0xD5B0FF));}
            for(var invitation:s.guilds){long expires=invitation.invites.getOrDefault(com.mmorpg.character.CharKey.of(p),0L);if(expires>System.currentTimeMillis()){
                String key=invitation.id+":"+expires;pending.add(key);
                if(seen.add(key))p.sendSystemMessage(Component.literal("Invitation de guilde : "+invitation.name+". Ouvre /guilde pour accepter ou refuser. Cette invitation est distincte de ton groupe.").withColor(0xD5B0FF));
            }}seen.retainAll(pending);
            long[] buffs=new long[GuildBuffs.Kind.values().length];if(g!=null)for(var kind:GuildBuffs.Kind.values()){long until=g.buffs.getOrDefault(kind.name(),0L);if(until>System.currentTimeMillis())buffs[kind.ordinal()]=until;}return new Membership(g==null?0:g.level,buffs);});var d=RpgPlayers.get(p);if(d.guildLevel!=membership.level||!Arrays.equals(d.guildBuffs,membership.buffs)){d.guildLevel=membership.level;d.guildBuffs=membership.buffs;RpgPlayers.recomputeAndSync(p);}}catch(Exception e){MMORPG.LOGGER.warn("Guild buffs refresh unavailable: {}",e.getMessage());}}
    public static void bossCleared(net.minecraft.server.MinecraftServer server,UUID guild,int tier){GuildStore.access(server,true,s->{var g=GuildStore.byId(s,guild);require(g!=null&&tier>=0&&tier<g.level,"Palier de guilde modifié.");g.bosses|=1<<tier;g.bossCooldowns.put(tier,System.currentTimeMillis()+6*60*60*1000L);g.log(Yggdrasil.ALL[tier].name()+" vaincu !");return null;});}
}
