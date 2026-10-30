package com.mmorpg.server;

import com.mmorpg.hub.*;
import com.mmorpg.quest.QuestDef;
import com.mmorpg.config.ConfigManager;
import com.mmorpg.network.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import java.util.*;

/** Per-player navigation. Never teleports, reveals inventories, loads destination chunks or accepts quest actions. */
@net.neoforged.fml.common.EventBusSubscriber(modid=com.mmorpg.MMORPG.MODID)
public final class GuidanceManager {
    public record Target(String kind,String title,String label,String dimension,Vec3 pos,String backend) {
        public Target(String kind,String title,String label,String dimension,Vec3 pos){this(kind,title,label,dimension,pos,NetworkConfig.current.role.name());}
        public CompoundTag tag(){var t=new CompoundTag();t.putBoolean("active",true);t.putString("kind",kind);t.putString("title",title);t.putString("label",label);t.putString("dimension",dimension);t.putString("backend",backend);if(pos!=null){t.putBoolean("located",true);t.putDouble("x",pos.x);t.putDouble("y",pos.y);t.putDouble("z",pos.z);}return t;}
    }
    private record Sent(CompoundTag data,long tick){}
    private static final Map<UUID,Sent> SENT=new HashMap<>();
    @net.neoforged.bus.api.SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event){
        var server=event.getServer();if(server.getTickCount()%20!=0)return;
        for(var p:server.getPlayerList().getPlayers())send(p,false);
    }
    public static void refresh(ServerPlayer p){send(p,true);}
    private static void send(ServerPlayer p,boolean force){
        long now=p.level().getServer().getTickCount();var target=resolve(p);var data=target==null?new CompoundTag():target.tag();var old=SENT.get(p.getUUID());
        if(force||old==null||!old.data.equals(data)||now-old.tick>=80){Net.toPlayer(p,new Payloads.Guidance(data));SENT.put(p.getUUID(),new Sent(data,now));}
    }
    @net.neoforged.bus.api.SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){SENT.remove(e.getEntity().getUUID());}
    @net.neoforged.bus.api.SubscribeEvent public static void stop(net.neoforged.neoforge.event.server.ServerStoppedEvent e){SENT.clear();}
    private static String dim(ServerPlayer p){return p.level().dimension().identifier().toString();}
    public static Target resolve(ServerPlayer p){
        if(!p.isAlive()||p.isSpectator()||CinematicManager.isLocked(p)||ClassTrial.active(p)||NetworkPlayers.inTransit(p))return null;
        var hub=HubData.get(p.level().getServer());
        if(hub.exists()&&p.level().dimension().equals(hub.dimension())){
            int next=nextGuide(ClassTrial.data(p).tutorialMask);
            if(next>=0){var at=hub.originAt(p.getX()).offset(HubGuides.SPOTS[next][0],HubGuides.SPOTS[next][1],HubGuides.SPOTS[next][2]);var npc=p.level().getEntity(HubGuides.id(hub.indexAt(p.getX()),next));return new Target("tutorial","Tutoriel · "+(next+1)+" / 5",HubGuides.NAMES[next],dim(p),npc==null?Vec3.atCenterOf(at):npc.position());}
        }
        var d=RpgPlayers.get(p);if(d.questGuidanceMuted&&d.trackedQuest.isEmpty())return null;String id=d.trackedQuest;
        if(!d.activeQuests.containsKey(id))id=d.activeQuests.keySet().stream().filter(x->QuestManager.def(x)!=null).sorted(Comparator.<String>comparingInt(x->QuestManager.completable(p,d,x)?0:1).thenComparing(x->x)).findFirst().orElse("");
        var q=QuestManager.def(id);if(q==null||!d.activeQuests.containsKey(id))return null;
        if(QuestManager.completable(p,d,id))return giver(p,q,id);
        for(int i=0;i<q.objectives.size();i++)if(QuestManager.progress(p,d,id,i)<q.objectives.get(i).count)return objective(p,q,q.objectives.get(i));
        return giver(p,q,id);
    }
    public static int nextGuide(int mask){for(int i=0;i<5;i++)if((mask&(1<<i))==0)return i;return -1;}
    private static Target giver(ServerPlayer p,QuestDef q,String quest){
        var binding=RpgPlayers.get(p).questGivers.get(quest);
        if(binding!=null&&(q.npc==null||q.npc.isEmpty()||q.npc.equals(binding.getStringOr("id","")))){var current=com.mmorpg.world.NpcDirectory.get(p.level().getServer()).entry(binding.getStringOr("id",""));var actor=current==null?binding:current;return route(p,new Target("quest",q.name,"Rendre : "+actor.getStringOr("name","Maître des Quêtes"),actor.getStringOr("dimension",""),Vec3.atCenterOf(BlockPos.of(actor.getLongOr("pos",0))),actor.getStringOr("backend",NetworkConfig.current.role.name())));}

        var candidates=new ArrayList<CompoundTag>();for(var e:com.mmorpg.world.NpcDirectory.get(p.level().getServer()).snapshot()){
            if(!(e instanceof CompoundTag t)||t.getIntOr("role",-1)!=com.mmorpg.entity.NpcEntity.Role.QUETES.ordinal())continue;
            if(q.npc!=null&&!q.npc.isEmpty()&&!q.npc.equals(t.getStringOr("id","")))continue;if(!q.giver.isEmpty()&&!q.giver.equalsIgnoreCase(t.getStringOr("group","")))continue;
            if(t.getBooleanOr("tutorialGuide",false))continue;
            candidates.add(t);
        }
        candidates.sort(Comparator.comparingDouble(t->t.getStringOr("dimension","").equals(dim(p))?p.position().distanceToSqr(Vec3.atCenterOf(BlockPos.of(t.getLongOr("pos",0)))):Double.MAX_VALUE));
        if(!candidates.isEmpty()){var npc=candidates.getFirst();return route(p,new Target("quest",q.name,"Rendre : "+npc.getStringOr("name","Maître des Quêtes"),npc.getStringOr("dimension",""),Vec3.atCenterOf(BlockPos.of(npc.getLongOr("pos",0)))));}
        var back=returnPortal(p,q.name);return back!=null?back:new Target("quest",q.name,"Rendre la quête à son donneur",dim(p),null);
    }
    private static Target objective(ServerPlayer p,QuestDef q,QuestDef.Objective o){
        if(o.navigation!=null){var n=o.navigation;return route(p,new Target("quest",q.name,"Rejoindre l’objectif",n.dimension,new Vec3(n.x,n.y,n.z),n.backend.isEmpty()?NetworkConfig.current.role.name():n.backend));}
        if(o.type.equals("collect")){
            var item=com.mmorpg.crafting.ForgeRecipes.resolveItem(o.target);
            var dropped=p.level().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,p.getBoundingBox().inflate(64),e->e.isAlive()&&e.getItem().is(item)&&(e.getOwner()==null||e.getOwner()==p)&&e.entityTags().stream().noneMatch(tag->tag.startsWith("aeloria_loot:")&&!tag.equals(PetLoot.tag(p)))).stream().min(Comparator.comparingDouble(p::distanceToSqr));
            if(dropped.isPresent())return new Target("quest",q.name,"Ramasser les objets",dim(p),dropped.get().position());
        }
        Set<String> wanted=new HashSet<>();if(o.type.equals("kill"))wanted.add(o.target.replace("mmorpg:",""));
        else if(o.type.equals("collect"))ConfigManager.mobs().forEach((key,cfg)->{if(cfg.drops.stream().anyMatch(drop->drop.chance>0&&!VanillaMobDrops.vanilla(drop.item)&&sameItem(drop.item,o.target)))wanted.add(key);});
        if(!wanted.isEmpty()){
            var mob=p.level().getEntitiesOfClass(net.minecraft.world.entity.Mob.class,p.getBoundingBox().inflate(80,40,80),m->m.isAlive()&&!(m instanceof com.mmorpg.entity.NpcEntity)&&!(m instanceof com.mmorpg.entity.hub.ClassMasterEntity)&&!(m instanceof com.mmorpg.entity.dummy.TrainingDummyEntity)&&matches(m,wanted)).stream().min(Comparator.comparingDouble(p::distanceToSqr));
            if(mob.isPresent())return new Target("quest",q.name,o.type.equals("collect")?"Récupérer le butin":"Vaincre la cible",dim(p),mob.get().position());
            var region=region(p,q,wanted);if(region!=null)return region;
        }
        return new Target("quest",q.name,o.type.equals("level")?"Progresser jusqu’au niveau "+o.count:o.type.equals("collect")?"Explorer pour trouver les matériaux":"Explorer pour trouver les cibles",dim(p),null);
    }
    private static boolean sameItem(String a,String b){return a.replace("mmorpg:","").equals(b.replace("mmorpg:",""));}
    private static boolean matches(net.minecraft.world.entity.Mob m,Set<String> wanted){
        var md=m.getExistingDataOrNull(com.mmorpg.registry.ModAttachments.MOB);String key=m instanceof com.mmorpg.entity.RpgMob r?r.mobKey():net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(m.getType()).toString();
        return wanted.contains(key)||wanted.contains("*")&&(md!=null&&md.initialized()||m instanceof net.minecraft.world.entity.monster.Monster)||wanted.contains("boss")&&md!=null&&md.boss;
    }
    private static Target region(ServerPlayer p,QuestDef q,Set<String> wanted){
        int plains=com.mmorpg.plains.PlainsManager.regionOf(p),mine=com.mmorpg.mine.MineManager.regionOf(p);
        for(var s:com.mmorpg.plains.PlainsLayout.STYLES)if((wanted.contains("*")||s.mobs().stream().anyMatch(wanted::contains))){
            if(plains==s.index()){var l=com.mmorpg.plains.PlainsLayout.get(plains);return new Target("quest",q.name,"Chercher les cibles dans "+s.name(),dim(p),new Vec3(l.x0()+150,l.height(150,110)+1,com.mmorpg.plains.PlainsLayout.Z0+110));}
            var portal=portal(p,"plaines",s.index());if(portal!=null)return new Target("quest",q.name,"Portail · "+s.name(),dim(p),Vec3.atCenterOf(portal.pos()));
        }
        for(var s:com.mmorpg.mine.MineLayout.STYLES)if((wanted.contains("*")||s.mobs().stream().anyMatch(wanted::contains))){
            if(mine==s.index()){var l=com.mmorpg.mine.MineLayout.get(mine);return new Target("quest",q.name,"Chercher les cibles dans "+s.name(),dim(p),new Vec3(l.x0()+180,l.floor(180,180)+1,com.mmorpg.mine.MineLayout.Z0+180));}
            var portal=portal(p,"mine",s.index());if(portal!=null)return new Target("quest",q.name,"Portail · "+s.name(),dim(p),Vec3.atCenterOf(portal.pos()));
        }
        for(var type:com.mmorpg.world.DungeonBuilder.DungeonType.values()){
            var key=com.mmorpg.registry.ModItems.get(type.bossKey);String boss=key instanceof com.mmorpg.item.SummonKeyItem k?k.bossKey():"";
            boolean bossObjective=wanted.contains("boss")||wanted.contains(boss);boolean mobs=type.mobs.get().stream().anyMatch(t->wanted.contains(net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(t).toString().replace("mmorpg:","")));
            if(!bossObjective&&!mobs)continue;
            var slot=com.mmorpg.dungeon.InstanceManager.slotAt(p.level().getServer(),p.level(),p.getX(),p.getZ());
            if(slot!=null&&com.mmorpg.dungeon.InstanceManager.typeOf(slot.pool)==type)return new Target("quest",q.name,bossObjective?"Rejoindre l’antre du boss":"Avancer dans le donjon",dim(p),new Vec3(slot.x+.5,slot.y+1,slot.z+(bossObjective?com.mmorpg.dungeon.Layout.ARENA.z():com.mmorpg.dungeon.Layout.R1.z())));
            var portal=com.mmorpg.world.AdventurePortalData.get(p.level().getServer()).all().stream().filter(a->a.dimension().equals(p.level().dimension())&&a.kind().equals("donjon")&&a.dungeon().equals(type.id)&&RpgPlayers.get(p).level>=com.mmorpg.dungeon.DungeonMaster.requiredLevel(type,a.difficulty())).min(Comparator.comparingDouble(a->p.position().distanceToSqr(Vec3.atCenterOf(a.pos()))));
            if(portal.isPresent())return new Target("quest",q.name,"Portail · "+type.label,dim(p),Vec3.atCenterOf(portal.get().pos()));
        }
        return null;
    }
    private static com.mmorpg.world.AdventurePortalData.Portal portal(ServerPlayer p,String kind,int region){return com.mmorpg.world.AdventurePortalData.get(p.level().getServer()).all().stream().filter(a->a.dimension().equals(p.level().dimension())&&a.kind().equals(kind)&&(!(kind.equals("mine")||kind.equals("plaines"))||a.region()<0||a.region()==region)).min(Comparator.comparingDouble(a->p.position().distanceToSqr(Vec3.atCenterOf(a.pos())))).orElse(null);}
    private static Target route(ServerPlayer p,Target target){
        if(target.dimension.equals(dim(p))&&target.backend.equals(NetworkConfig.current.role.name()))return target;
        String kind=target.backend.equals("PLAINS")?"plaines":target.backend.equals("MINES")?"mine":target.backend.equals("HUB")?"hub":target.backend.equals("MAIN")?"spawn":"";
        if(!kind.isEmpty()&&target.pos!=null){int region=kind.equals("plaines")?com.mmorpg.plains.PlainsLayout.regionAt(target.pos.x,target.pos.z):kind.equals("mine")?com.mmorpg.mine.MineLayout.regionAt(target.pos.x,target.pos.z):-1;var portal=portal(p,kind,region);if(portal!=null)return new Target(target.kind,target.title,"Portail vers l’objectif",dim(p),Vec3.atCenterOf(portal.pos()));}
        var back=returnPortal(p,target.title);return back!=null?back:new Target(target.kind,target.title,"Autre monde · "+target.label,target.dimension,null);
    }
    private static Target returnPortal(ServerPlayer p,String title){
        int r=com.mmorpg.plains.PlainsManager.regionOf(p);if(r>=0){var l=com.mmorpg.plains.PlainsLayout.get(r);return new Target("quest",title,"Portail de retour au spawn",dim(p),new Vec3(l.x0()+com.mmorpg.plains.PlainsLayout.PLAZA_X+.5,com.mmorpg.plains.PlainsLayout.BASE+1,com.mmorpg.plains.PlainsLayout.Z0+com.mmorpg.plains.PlainsBuilder.ARCH_Z+1));}
        r=com.mmorpg.mine.MineManager.regionOf(p);if(r>=0){var l=com.mmorpg.mine.MineLayout.get(r);return new Target("quest",title,"Portail de retour au spawn",dim(p),new Vec3(l.x0()+com.mmorpg.mine.MineLayout.PLAZA_X+.5,l.floor(com.mmorpg.mine.MineLayout.PLAZA_X,com.mmorpg.mine.MineLayout.PLAZA_Z)+1,com.mmorpg.mine.MineLayout.Z0+com.mmorpg.mine.MineLayout.PLAZA_Z-11));}
        var hub=HubData.get(p.level().getServer());if(hub.exists()&&p.level().dimension().equals(hub.dimension()))return new Target("quest",title,"Portail du spawn",dim(p),Vec3.atCenterOf(hub.originAt(p.getX()).offset(HubPortals.HUB_GATE_X,0,0)));
        var passer=p.level().getEntitiesOfClass(com.mmorpg.entity.NpcEntity.class,p.getBoundingBox().inflate(160),n->n.role()==com.mmorpg.entity.NpcEntity.Role.SORTIE).stream().min(Comparator.comparingDouble(p::distanceToSqr));if(passer.isPresent())return new Target("quest",title,"Passeur · Retour au spawn",dim(p),passer.get().position());
        if(NetworkConfig.current.role==NetworkConfig.Role.MAIN)return null;
        var placed=com.mmorpg.world.AdventurePortalData.get(p.level().getServer()).all().stream().filter(a->a.dimension().equals(p.level().dimension())&&a.kind().equals("spawn")).findFirst();return placed.map(a->new Target("quest",title,"Portail du spawn",dim(p),Vec3.atCenterOf(a.pos()))).orElse(null);
    }
}
