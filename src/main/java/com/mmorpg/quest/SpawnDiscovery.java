package com.mmorpg.quest;
import com.mmorpg.MMORPG;
import com.mmorpg.config.ConfigManager;
import com.mmorpg.entity.NpcEntity;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.*;

/** Adds actors and quests only: no edits to the player's spawn architecture or hub tutorial. */
@EventBusSubscriber(modid=MMORPG.MODID)
public final class SpawnDiscovery {
    public static final String[] IDS=QuestNpcPlan.DISCOVERY_IDS;
    public static UUID id(int i){return QuestNpcPlan.uuid(i);}
    public static Map<String,QuestDef> definitions(){return QuestNpcPlan.discoveryDefinitions();}
    /** Sol libre le plus proche de la cible (rayon 12, hauteur +-24 autour de la base), ou null si le monde n'est pas construit/charge a cet endroit. */
    private static BlockPos ground(ServerLevel l,BlockPos target,int baseY){return ground(l,target,baseY,12);}
    private static BlockPos ground(ServerLevel l,BlockPos target,int baseY,int maxRadius){
        for(int r=0;r<=maxRadius;r+=3)for(int a=0;a<(r==0?1:8);a++){
            int x=target.getX()+(int)Math.round(Math.cos(a*Math.PI/4)*r),z=target.getZ()+(int)Math.round(Math.sin(a*Math.PI/4)*r);
            for(int dy=0;dy<=24;dy++)for(int sign=dy==0?1:-1;sign<=1;sign+=2){
                var p=new BlockPos(x,baseY+dy*sign,z);
                if(l.hasChunkAt(p)&&l.isEmptyBlock(p)&&l.isEmptyBlock(p.above())&&l.getBlockState(p.below()).isSolidRender()&&l.getEntitiesOfClass(NpcEntity.class,new net.minecraft.world.phys.AABB(p).inflate(1.5)).isEmpty())return p;
            }
        }return null;
    }
    private static BlockPos safe(ServerLevel l,BlockPos around,int index){
        for(int r=4+index*2;r<=18;r+=2)for(int a=0;a<8;a++){
            int x=around.getX()+(int)Math.round(Math.cos(a*Math.PI/4)*r),z=around.getZ()+(int)Math.round(Math.sin(a*Math.PI/4)*r);
            for(int y=around.getY()+2;y>=around.getY()-2;y--){var p=new BlockPos(x,y,z);if(l.hasChunkAt(p)&&l.isEmptyBlock(p)&&l.isEmptyBlock(p.above())&&l.getBlockState(p.below()).isSolidRender()&&l.getEntitiesOfClass(NpcEntity.class,new net.minecraft.world.phys.AABB(p).inflate(1.5)).isEmpty())return p;}
        }return null;
    }
    private static BlockPos base(ServerPlayer player){
        var data=com.mmorpg.hub.SpawnData.get(player.level().getServer());
        return data.exists()?BlockPos.containing(data.arrival()):player.blockPosition();
    }
    /** Point vise d'un donneur : le premier se tient pres de l'arrivee, les autres sont repartis autour du centre de la cite (l'arrivee est a son bord est). */
    private static BlockPos target(net.minecraft.server.MinecraftServer server,BlockPos origin,QuestNpcPlan.Giver g){
        var city=com.mmorpg.hub.SpawnData.get(server).origin();
        if(g.index()==0||city==null)return origin.offset(g.dx(),0,g.dz());
        return new BlockPos(city.getX()+com.mmorpg.hub.HubMarket.CX+g.dx(),origin.getY(),city.getZ()+g.dz());
    }
    private static boolean tooClose(ServerLevel l,NpcEntity n,java.util.Collection<NpcEntity> others){
        for(var o:others)if(o!=null&&o!=n&&o.distanceTo(n)<QuestNpcPlan.MIN_DISTANCE)return true;return false;
    }
    private static boolean quiet;
    public static void install(ServerPlayer player){
        if(!com.mmorpg.network.NetworkConfig.hosts(com.mmorpg.network.NetworkConfig.Role.MAIN)||!player.level().dimension().equals(com.mmorpg.hub.SpawnBuilder.DIM))return;
        var l=player.level();var plan=QuestNpcPlan.GIVERS;NpcEntity[] actors=new NpcEntity[plan.size()];var origin=base(player);var missing=new ArrayList<String>();
        for(var g:plan){
            int i=g.index();var existing=l.getEntity(id(i));if(existing instanceof NpcEntity n){actors[i]=n;continue;}
            if(com.mmorpg.world.NpcDirectory.get(l.getServer()).entry(id(i).toString())!=null)continue;
            var at=ground(l,target(l.getServer(),origin,g),origin.getY());
            if(at==null&&i>=5)at=ground(l,target(l.getServer(),origin,g),origin.getY(),30);
            if(at==null&&i==0)at=safe(l,player.blockPosition(),0);
            if(at==null){missing.add(g.name()+" ("+target(l.getServer(),origin,g).getX()+", "+target(l.getServer(),origin,g).getZ()+")");continue;}
            var n=com.mmorpg.registry.ModEntities.NPC.get().create(l,net.minecraft.world.entity.EntitySpawnReason.COMMAND);n.setUUID(id(i));n.setup(NpcEntity.Role.QUETES,g.name(),g.skin(),g.group());n.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,180,0);n.setPersistenceRequired();if(l.addFreshEntity(n))actors[i]=n;
        }
        // Donneurs deja poses trop pres d'un autre : replaces a leur emplacement prevu quand le sol existe.
        var loaded=java.util.Arrays.asList(actors);
        for(var g:plan){var n=actors[g.index()];if(n==null||!tooClose(l,n,loaded))continue;
            var at=ground(l,target(l.getServer(),origin,g),origin.getY());if(at==null)continue;
            n.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,n.getYRot(),0);com.mmorpg.world.NpcDirectory.get(l.getServer()).record(n);
        }
        if(!missing.isEmpty()&&!quiet)MMORPG.LOGGER.warn("[MMORPG] Donneurs de quêtes sans sol : {}",String.join(" ; ",missing));
        if(!missing.isEmpty()&&!quiet&&player.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER))player.sendSystemMessage(Component.literal("Donneurs de quêtes sans sol à leur emplacement prévu : "+String.join(" ; ",missing)+". Construisez à cet endroit ou placez-les avec l’outil PNJ (distance minimale "+QuestNpcPlan.MIN_DISTANCE+" blocs).").withColor(0xFFB060));
        UUID merchantId=UUID.nameUUIDFromBytes("aeloria:spawn-premium-store".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if(l.getEntity(merchantId)==null&&com.mmorpg.world.NpcDirectory.get(l.getServer()).entry(merchantId.toString())==null){var at=safe(l,player.blockPosition(),5);if(at!=null){var n=com.mmorpg.registry.ModEntities.NPC.get().create(l,net.minecraft.world.entity.EntitySpawnReason.COMMAND);n.setUUID(merchantId);n.setup(NpcEntity.Role.BOUTIQUE,"Céliane · Boutique d’Aeloria",8,"");n.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,180,0);n.setPersistenceRequired();l.addFreshEntity(n);}}
        installMascot(l,origin);
        bindDefaults(l.getServer());refreshNavigation(l.getServer());
        if(actors[0]!=null&&!RpgPlayers.get(player).completedQuests.contains(IDS[0]))player.sendSystemMessage(Component.literal("Découvrez la cité avec Maëlys, l’accueil du spawn. Ses quêtes vous présentent Aeloria.").withColor(0xD5B0FF));
    }
    /** Decalage de la Mascotte (est, sud) depuis le point d'arrivee du spawn : a cote de la sortie, en x +5 / z -2. */
    public static final int MASCOT_DX=5,MASCOT_DZ=-2;
    /** Anciennes versions : une Mascotte unique etait posee au spawn ; elle est maintenant personnelle (MascotGuide). Retire l'ancienne. */
    private static void installMascot(ServerLevel l,BlockPos origin){
        var old=l.getEntity(com.mmorpg.entity.MascotEntity.ID);if(old!=null)old.discard();
        com.mmorpg.world.NpcDirectory.get(l.getServer()).remove(com.mmorpg.entity.MascotEntity.ID.toString());
    }
    public static void refreshNavigation(net.minecraft.server.MinecraftServer server){
        var directory=com.mmorpg.world.NpcDirectory.get(server);
        for(int i=0;i<IDS.length;i++){
            var q=ConfigManager.quests().get(IDS[i]);if(q==null)continue;boolean changed=false;
            if(q.npc==null||q.npc.isEmpty()){q.npc=id(i).toString();changed=true;}
            if(i<4&&!q.objectives.isEmpty()){
                var entry=directory.entry(id(i+1).toString());if(entry!=null){var at=BlockPos.of(entry.getLongOr("pos",0));var objective=q.objectives.getFirst();var n=objective.navigation;
                    if(n==null||n.x!=at.getX()+.5||n.y!=at.getY()||n.z!=at.getZ()+.5||!n.dimension.equals(entry.getStringOr("dimension",""))){
                        n=new QuestDef.Navigation();n.dimension=entry.getStringOr("dimension","");n.backend=entry.getStringOr("backend","MAIN");n.x=at.getX()+.5;n.y=at.getY();n.z=at.getZ()+.5;objective.navigation=n;changed=true;
                    }
                }
            }
            if(changed)try{ConfigManager.saveQuest(IDS[i],q);}catch(java.io.IOException e){MMORPG.LOGGER.error("Spawn discovery binding save",e);}
        }
        for(var p:server.getPlayerList().getPlayers())RpgPlayers.get(p).dirty=true;
    }
    private static void bindDefaults(net.minecraft.server.MinecraftServer server){
        var candidates=new ArrayList<net.minecraft.nbt.CompoundTag>();
        for(var t:com.mmorpg.world.NpcDirectory.get(server).snapshot())if(t instanceof net.minecraft.nbt.CompoundTag n&&n.getIntOr("role",-1)==0&&!n.getBooleanOr("tutorialGuide",false)&&!n.getStringOr("group","").equals(com.mmorpg.server.MascotGuide.GROUP)&&n.getStringOr("dimension","").equals(com.mmorpg.hub.SpawnBuilder.DIM.identifier().toString())&&!n.getStringOr("group","").startsWith("spawn_decouverte_"))candidates.add(n);
        candidates.sort(java.util.Comparator.comparing(n->n.getStringOr("id","")));if(candidates.isEmpty())return;
        int index=0;for(String key:DefaultQuests.create().keySet()){
            var q=ConfigManager.quests().get(key);if(q==null||q.npc!=null&&!q.npc.isEmpty())continue;
            var eligible=candidates.stream().filter(n->q.giver.isEmpty()||q.giver.equalsIgnoreCase(n.getStringOr("group",""))).toList();if(eligible.isEmpty())continue;
            q.npc=eligible.get(index++%eligible.size()).getStringOr("id","");try{ConfigManager.saveQuest(key,q);}catch(java.io.IOException e){MMORPG.LOGGER.error("Unique quest giver save",e);}
        }
    }
    @SubscribeEvent public static void login(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)com.mmorpg.server.Scheduler.later(p.level(),60,()->install(p));}
    @SubscribeEvent public static void dimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)com.mmorpg.server.Scheduler.later(p.level(),40,()->install(p));}
    @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post e){
        // les donneurs lointains manquent tant que leur zone n'est pas chargee a l'arrivee : nouvel essai toutes les 30 s (sans message)
        if(e.getServer().getTickCount()%600==300){quiet=true;try{for(var p:e.getServer().getPlayerList().getPlayers())if(p.level().dimension().equals(com.mmorpg.hub.SpawnBuilder.DIM)&&p.isAlive()){var found=0;for(var g:QuestNpcPlan.GIVERS)if(p.level().getEntity(id(g.index())) instanceof NpcEntity)found++;if(found<QuestNpcPlan.GIVERS.size())install(p);break;}}finally{quiet=false;}}
        if(e.getServer().getTickCount()%20!=0)return;for(var p:e.getServer().getPlayerList().getPlayers()){
            var d=RpgPlayers.get(p);for(var entry:d.activeQuests.entrySet()){
                var q=ConfigManager.quests().get(entry.getKey());if(q==null)continue;var progress=entry.getValue();
                for(int i=0;i<Math.min(progress.length,q.objectives.size());i++){var objective=q.objectives.get(i);var n=objective.navigation;if(!objective.type.equals("visit")||n==null||progress[i]>=objective.count)continue;
                    if(!p.level().dimension().identifier().toString().equals(n.dimension)||!n.backend.isEmpty()&&com.mmorpg.network.NetworkConfig.current.enabled&&!com.mmorpg.network.NetworkConfig.current.role.name().equals(n.backend))continue;
                    if(p.isAlive()&&!p.isSpectator()&&p.distanceToSqr(n.x,n.y,n.z)<=16){progress[i]=objective.count;d.dirty=true;p.sendOverlayMessage(Component.literal("Lieu découvert · "+objective.target).withColor(0x80E0B0));}
                }
            }
        }
    }
}
