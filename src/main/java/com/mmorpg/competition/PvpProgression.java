package com.mmorpg.competition;
import com.mmorpg.MMORPG;
import com.mmorpg.item.*;
import com.mmorpg.network.*;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.nbt.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.*;
/** Points, result receipts and purchases share an atomic ledger across all backends. */
@EventBusSubscriber(modid=MMORPG.MODID)
public final class PvpProgression {
 public static final class State{public Map<String,Account> accounts=new HashMap<>();}
 public static final class Account{public long points;public Map<String,String> requests=new HashMap<>();public Set<String> awards=new HashSet<>(),unlocks=new HashSet<>();public List<Delivery> pending=new ArrayList<>();public Report report;public String acknowledgedReport="";}
 public static final class Delivery{public String id,item,unlock;public int count;}
 public static final class Row{public String name,id;public int team,kills,deaths,strikes;public long points;public double damage,healing;}
 public static final class Report{public String id;public boolean training,unfinished;public int winner,red,blue;public List<Row> rows=new ArrayList<>();}
 public record Product(String id,String label,String item,int count,long price,String unlock,int level,int cls){}
 public static <T>T access(MinecraftServer s,java.util.function.Function<State,T> f){return SharedLedger.access(s,"arena-progression",State.class,State::new,f);}
 public static Account account(State s,UUID id){return s.accounts.computeIfAbsent(id.toString(),k->new Account());}
 public static List<Product> products(){var out=new ArrayList<Product>();
  out.add(new Product("soin","Potion de soin majeure","potion_soin_majeure",3,30,"",1,-1));out.add(new Product("mana","Potion de mana majeure","potion_mana_majeure",3,30,"",1,-1));
  out.add(new Product("titre_combattant","Titre : Combattant de l’arène","",1,300,"title:Combattant de l’arène",1,-1));out.add(new Product("titre_gladiateur","Titre : Gladiateur d’Aeloria","",1,1200,"title:Gladiateur d’Aeloria",1,-1));out.add(new Product("monture","Monture exclusive : Chimère des arènes","",1,3500,"mount:chimere_arene",1,-1));
  for(var set:ItemDefs.SETS.values())if(set.id().startsWith("pvp_")){int bracket=(set.level()-1)/20;for(int i=0;i<4;i++)out.add(new Product(set.id()+"_"+ItemDefs.PIECES[i],set.name()+" · "+new String[]{"Casque","Plastron","Jambières","Bottes"}[i],set.id()+"_"+ItemDefs.PIECES[i],1,80+bracket*60,"",set.level(),set.cls().ordinal()));out.add(new Product(set.id()+"_arme",set.name()+" · Arme",set.id()+"_arme",1,180+bracket*100,"",set.level(),set.cls().ordinal()));}return out;
 }
 public static long reward(ArenaRules.Match.Contribution c,boolean won){long effort=c.kills*8L+c.strikes*2L+(long)(c.damage/100)+(long)(c.healing/150);return effort<=0?0:Math.min(250,10+effort)*(won?2:1);}
 public static Report snapshot(ArenaManager.Session session,MinecraftServer server,boolean unfinished){var report=new Report();report.id=session.match.id.toString()+(unfinished?":leave":"");report.training=session.test;report.unfinished=unfinished;report.winner=unfinished?-1:session.match.winner();report.red=session.match.red;report.blue=session.match.blue;
  for(var entry:session.match.teams.entrySet()){var id=entry.getKey();var p=server.getPlayerList().getPlayer(id);var c=session.match.contribution(id);var row=new Row();row.id=id.toString();row.name=p==null?id.toString().substring(0,8):p.getGameProfile().name();row.team=entry.getValue();row.kills=c.kills;row.deaths=c.deaths;row.strikes=c.strikes;row.damage=c.damage;row.healing=c.healing;row.points=report.training||unfinished?0:reward(c,row.team==report.winner);report.rows.add(row);}return report;
 }
 public static void finish(ArenaManager.Session session,MinecraftServer server){if(!session.match.ended)return;var report=snapshot(session,server,false);
  var counted=new ArrayList<Row>();
  access(server,s->{for(var row:report.rows){var a=account(s,UUID.fromString(row.id));if(a.awards.add(report.id)){a.points=Math.addExact(a.points,row.points);a.report=report;counted.add(row);}}return null;});
  if(!report.training)for(var row:counted)com.mmorpg.ranking.Rankings.arena(server,UUID.fromString(row.id),row.name,row.kills,row.team==report.winner);
 }
 public static void leaving(ServerPlayer player,ArenaManager.Session session){var report=snapshot(session,player.level().getServer(),true);access(player.level().getServer(),s->{account(s,player.getUUID()).report=report;return null;});}
 public static CompoundTag reportTag(Report r,UUID player){var t=new CompoundTag();t.putString("match",r.id);t.putBoolean("training",r.training);t.putBoolean("unfinished",r.unfinished);t.putInt("winner",r.winner);t.putInt("red",r.red);t.putInt("blue",r.blue);t.putString("player",player.toString());var rows=new ListTag();for(var row:r.rows){var n=new CompoundTag();n.putString("name",row.name);n.putString("id",row.id);n.putInt("team",row.team);n.putInt("kills",row.kills);n.putInt("deaths",row.deaths);n.putInt("strikes",row.strikes);n.putDouble("damage",row.damage);n.putDouble("healing",row.healing);n.putLong("points",row.points);rows.add(n);}t.put("rows",rows);return t;}
 public static void showReport(ServerPlayer p){showReport(p,false);}
 public static boolean showReport(ServerPlayer p,boolean replay){var report=access(p.level().getServer(),s->{var a=account(s,p.getUUID());return a.report!=null&&(replay||!a.report.id.equals(a.acknowledgedReport))?a.report:null;});if(report==null)return false;Net.toPlayer(p,new S2COpenScreen(S2COpenScreen.ARENA_RESULTS,reportTag(report,p.getUUID())));return true;}
 public static void afterReturn(ServerPlayer p){com.mmorpg.server.Scheduler.later(p.level(),50,()->{if(!p.isRemoved()&&!NetworkPlayers.inTransit(p))showReport(p);});}
 public static void buy(ServerPlayer p,String id,UUID request,long expected){
        if(!com.mmorpg.server.FeatureFlags.allow(p,"boutique_pvp"))throw new IllegalArgumentException("Marchand PvP temporairement désactivé.");var product=products().stream().filter(x->x.id().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Produit inconnu."));var d=RpgPlayers.get(p);if(product.level()>d.level||product.cls()>=0&&product.cls()!=d.playerClass.ordinal())throw new IllegalArgumentException("Cet équipement exige sa classe et son niveau.");
  access(p.level().getServer(),s->{var a=account(s,p.getUUID());String prior=a.requests.get(request.toString());if(prior!=null){if(!prior.equals(id))throw new IllegalArgumentException("Référence déjà utilisée.");return null;}if(expected!=product.price()||a.points<product.price())throw new IllegalArgumentException("Prix modifié ou points insuffisants.");if(!product.unlock().isEmpty()&&(a.unlocks.contains(product.unlock())||a.pending.stream().anyMatch(v->product.unlock().equals(v.unlock))))throw new IllegalArgumentException("Déjà débloqué ou en livraison.");var delivery=new Delivery();delivery.id=request.toString();delivery.item=product.item();delivery.count=product.count();delivery.unlock=product.unlock();a.points-=product.price();a.pending.add(delivery);a.requests.put(request.toString(),id);return null;});deliver(p);
 }
 public static void deliver(ServerPlayer p){var pending=access(p.level().getServer(),s->new ArrayList<>(account(s,p.getUUID()).pending));for(var delivery:pending){var d=RpgPlayers.get(p);String receipt="pvp:"+delivery.id;if(!d.guildReceipts.getBooleanOr(receipt,false)){
    var inventory=p.getInventory();var before=new ArrayList<ItemStack>();for(var stack:inventory)before.add(stack.copy());var oldMounts=new HashSet<>(d.mounts);
    if(!delivery.item.isEmpty()){if(inventory.getFreeSlot()<0)return;var stack=new ItemStack(com.mmorpg.crafting.ForgeRecipes.resolveItem("mmorpg:"+delivery.item),delivery.count);if(!inventory.add(stack)||!stack.isEmpty()){for(int i=0;i<before.size();i++)inventory.setItem(i,before.get(i));return;}}
    if(delivery.unlock.startsWith("mount:"))d.mounts.add(delivery.unlock.substring(6));d.guildReceipts.putBoolean(receipt,true);d.dirty=true;
    boolean saved;if(NetworkConfig.current.enabled)saved=NetworkPlayers.save(p);else{p.level().getServer().getPlayerList().saveAll();saved=true;}if(!saved){for(int i=0;i<before.size();i++)inventory.setItem(i,before.get(i));d.mounts.clear();d.mounts.addAll(oldMounts);d.guildReceipts.remove(receipt);throw new IllegalStateException("Livraison conservée en attente de sauvegarde.");}
   }access(p.level().getServer(),s->{var a=account(s,p.getUUID());if(!delivery.unlock.isEmpty())a.unlocks.add(delivery.unlock);a.pending.removeIf(v->v.id.equals(delivery.id));return null;});}
 }
 public static void open(ServerPlayer p,String status){deliver(p);var a=access(p.level().getServer(),s->account(s,p.getUUID()));var t=new CompoundTag();t.putLong("points",a.points);t.putString("status",status);t.putInt("pending",a.pending.size());var products=new ListTag();for(var product:products()){if(product.cls()>=0&&product.cls()!=RpgPlayers.get(p).playerClass.ordinal())continue;var n=new CompoundTag();n.putString("id",product.id());n.putString("label",product.label());n.putString("item",product.item());n.putString("unlock",product.unlock());n.putLong("price",product.price());n.putInt("count",product.count());n.putInt("level",product.level());n.putBoolean("owned",!product.unlock().isEmpty()&&a.unlocks.contains(product.unlock()));products.add(n);}t.put("products",products);Net.toPlayer(p,new S2COpenScreen(S2COpenScreen.PVP_STORE,t));}
 public static void action(ServerPlayer p,CompoundTag t){if(NetworkPlayers.inTransit(p)||com.mmorpg.hub.ClassTrial.active(p)||!p.isAlive())return;try{switch(t.getStringOr("action","open")){
  case "buy"->{buy(p,t.getStringOr("product",""),UUID.fromString(t.getStringOr("request","")),t.getLongOr("price",-1));open(p,"Achat enregistré. Les objets sans place restent en attente.");}
  case "title"->{String title=t.getStringOr("title","");boolean owned=title.isEmpty()||access(p.level().getServer(),s->account(s,p.getUUID()).unlocks.contains("title:"+title));if(!owned)throw new IllegalArgumentException("Titre non débloqué.");RpgPlayers.get(p).pvpTitle=title;RpgPlayers.get(p).dirty=true;RpgPlayers.updatePublic(p);open(p,"Titre équipé.");}
  case "report_ack"->access(p.level().getServer(),s->{var a=account(s,p.getUUID());if(a.report!=null&&a.report.id.equals(t.getStringOr("match","")))a.acknowledgedReport=a.report.id;return null;});
  default->open(p,"");}
 }catch(Exception ex){open(p,ex.getMessage());}}
 @SubscribeEvent public static void login(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)com.mmorpg.server.Scheduler.later(p.level(),50,()->{try{deliver(p);showReport(p);}catch(Exception ex){MMORPG.LOGGER.warn("PvP : {}",ex.getMessage());}});}
}
