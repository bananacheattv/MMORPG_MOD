package com.mmorpg.server;
import com.mmorpg.MMORPG;
import com.mmorpg.network.SharedLedger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.*;
/** Shared runtime switches. File replacement is atomic and backends observe changes within one second. */
@EventBusSubscriber(modid=MMORPG.MODID)
public final class FeatureFlags {
 public static final Set<String> KEYS=new LinkedHashSet<>(List.of("wiki","arenes","donjons","drones","boutique_pvp","boutique","quetes","sacs","hdv","guildes","boss_guilde","cinematique","epreuves_classes","apparition_mobs","competences","bouclier","luckyblocks","coffres","montures","familiers","mines","plaines","forge","banque","marchands","cosmetiques","echange","metiers","peche","succes","classements","groupes","bestiaire","portails","quetes_hebdo","chat_guilde","anticheat","autolock"));
 public static final class State{public Map<String,Boolean> enabled=new HashMap<>();}
 private static final class Cache{State state=new State();long checked;final com.mmorpg.db.SharedDocs.Poller poller=new com.mmorpg.db.SharedDocs.Poller("ledger-features");}
 private static final Map<MinecraftServer,Cache> CACHE=new WeakHashMap<>();
 public static boolean enabled(MinecraftServer server,String key){if(!KEYS.contains(key))throw new IllegalArgumentException("Fonctionnalité inconnue : "+key);var c=CACHE.computeIfAbsent(server,s->new Cache());long now=System.currentTimeMillis();if(now-c.checked>=1000){boolean first=c.checked==0;c.checked=now;refresh(server,c,first);}applyLegacy(c.state);return c.state.enabled.getOrDefault(key,true);}
 /** Avec la base, set() ecrit dans la base (registre partage) : on la relit (tout de suite au premier acces, puis en arriere-plan) ; sinon le fichier partage. */
 private static void refresh(MinecraftServer server,Cache c,boolean first){
  var path=com.mmorpg.guild.GuildStore.directory(server).resolveSibling("coordination").resolve("features.json");
  try{String json=null;
   if(com.mmorpg.db.SharedDocs.useDb()){var doc=first?com.mmorpg.db.SharedDocs.read("ledger-features"):c.poller.poll();if(doc==null)return;json=doc.data();}
   if(json==null){if(!java.nio.file.Files.exists(path))return;json=java.nio.file.Files.readString(path);}
   var state=new com.google.gson.Gson().fromJson(json,State.class);if(state==null||state.enabled==null)throw new IllegalArgumentException("Fichier incomplet");if(!state.enabled.equals(c.state.enabled))for(var p:server.getPlayerList().getPlayers())RpgPlayers.get(p).dirty=true;c.state=state;
  }catch(Exception e){MMORPG.LOGGER.warn("Interrupteurs inchangés : {}",e.getMessage());}
 }
public static boolean allow(ServerPlayer p,String key){if(enabled(p.level().getServer(),key))return true;p.sendOverlayMessage(Component.literal("Fonctionnalité temporairement désactivée : "+key).withColor(0xFFAA77));return false;}
 public static void set(MinecraftServer server,String key,boolean value){if(!KEYS.contains(key))throw new IllegalArgumentException("Fonctionnalité inconnue.");var state=SharedLedger.access(server,"features",State.class,State::new,s->{s.enabled.put(key,value);return s;});var c=CACHE.computeIfAbsent(server,s->new Cache());c.poller.discard();c.state=state;c.checked=System.currentTimeMillis();for(var p:server.getPlayerList().getPlayers())RpgPlayers.get(p).dirty=true;}
 private static void applyLegacy(State state){var g=com.mmorpg.config.ConfigManager.general();if(g==null)return;if(state.enabled.containsKey("apparition_mobs"))g.customSpawning=state.enabled.get("apparition_mobs");if(state.enabled.containsKey("cinematique"))g.hubCinematic=state.enabled.get("cinematique");if(state.enabled.containsKey("epreuves_classes"))g.hubTrialZone=state.enabled.get("epreuves_classes");}
 @SubscribeEvent public static void commands(net.neoforged.neoforge.event.RegisterCommandsEvent e){var root=Commands.literal("fonctionnalite").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c->{for(String key:KEYS)c.getSource().sendSuccess(()->Component.literal(key+" : "+(enabled(c.getSource().getServer(),key)?"activée":"désactivée")),false);return 1;});
  var choice=Commands.argument("fonction",com.mojang.brigadier.arguments.StringArgumentType.word()).suggests((c,b)->net.minecraft.commands.SharedSuggestionProvider.suggest(KEYS,b));for(boolean value:new boolean[]{true,false})choice.then(Commands.literal(value?"activer":"desactiver").executes(c->{String key=com.mojang.brigadier.arguments.StringArgumentType.getString(c,"fonction");set(c.getSource().getServer(),key,value);c.getSource().sendSuccess(()->Component.literal(key+" "+(value?"activée":"désactivée")+" sur le réseau, sans redémarrage. Les expéditions et matchs engagés peuvent se terminer."),true);return 1;}));root.then(choice);e.getDispatcher().register(root);
 }
 @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post e){if(e.getServer().getTickCount()%20==0)enabled(e.getServer(),"arenes");}
 @SubscribeEvent public static void stopped(net.neoforged.neoforge.event.server.ServerStoppedEvent e){CACHE.remove(e.getServer());}
/** Tests : oublie l'etat en memoire, comme un autre serveur ou un redemarrage. */
static void forget(MinecraftServer server){CACHE.remove(server);}
}
