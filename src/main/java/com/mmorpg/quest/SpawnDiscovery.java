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
    public static final String[] IDS={"spawn_bienvenue","spawn_artisans","spawn_economie","spawn_expeditions","spawn_premiere_chasse"};
    private static final String[] NAMES={"Maëlys · Accueil d’Aeloria","Orin · Artisan de la cité","Lysandre · Intendante","Érian · Éclaireur","Capitaine Vaël"};
    private static final String[] TITLES={"Bienvenue dans la cité","Les mains des artisans","L’économie d’Aeloria","Au-delà des remparts","Votre première mission"};
    private static final String[] LORE={
        "Bienvenue au spawn d’Aeloria. Rencontrez Orin, l’artisan : la flèche violette indique votre objectif. Le journal garde vos quêtes et leurs récompenses.",
        "Vos minerais deviennent des lingots, puis des équipements dans les forges. Les recettes et parchemins se débloquent progressivement. Rencontrez l’intendante pour découvrir le commerce.",
        "L’or sert aux marchands, à la banque et à l’hôtel des ventes. Les crédits de boutique sont une monnaie distincte. Parlez à l’éclaireur pour préparer votre première sortie.",
        "Les plaines accueillent les premières chasses ; les mines se débloquent par niveau. Les donjons sont privés pour votre groupe et annoncent leurs dangers. Le capitaine vous attend.",
        "Éliminez trois gobelins dans les plaines puis revenez au capitaine. Gardez quelques potions : en expédition, le butin de session peut être perdu à la mort. Les groupes facilitent les combats."};
    public static UUID id(int i){return UUID.nameUUIDFromBytes(("aeloria:spawn-discovery:"+i).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    public static Map<String,QuestDef> definitions(){
        var map=new LinkedHashMap<String,QuestDef>();for(int i=0;i<IDS.length;i++){
            var q=new QuestDef();q.name=TITLES[i];q.description=LORE[i];q.giver="spawn_decouverte_"+i;q.minLevel=1;
            if(i>0)q.requires.add(IDS[i-1]);q.objectives.add(new QuestDef.Objective(i==4?"kill":"visit",i==4?"gobelin":NAMES[i+1],i==4?3:1));q.rewards.xp=80+i*40;q.rewards.gold=10+i*5;
            q.rewards.items.add(new QuestDef.ItemReward("mmorpg:potion_soin_mineure",i==4?3:1));map.put(IDS[i],q);
        }return map;
    }
    private static BlockPos safe(ServerLevel l,BlockPos around,int index){
        for(int r=4+index*2;r<=18;r+=2)for(int a=0;a<8;a++){
            int x=around.getX()+(int)Math.round(Math.cos(a*Math.PI/4)*r),z=around.getZ()+(int)Math.round(Math.sin(a*Math.PI/4)*r);
            for(int y=around.getY()+2;y>=around.getY()-2;y--){var p=new BlockPos(x,y,z);if(l.hasChunkAt(p)&&l.isEmptyBlock(p)&&l.isEmptyBlock(p.above())&&l.getBlockState(p.below()).isSolidRender()&&l.getEntitiesOfClass(NpcEntity.class,new net.minecraft.world.phys.AABB(p).inflate(1.5)).isEmpty())return p;}
        }return null;
    }
    public static void install(ServerPlayer player){
        if(!com.mmorpg.network.NetworkConfig.hosts(com.mmorpg.network.NetworkConfig.Role.MAIN)||!player.level().dimension().equals(com.mmorpg.hub.SpawnBuilder.DIM))return;
        var l=player.level();NpcEntity[] actors=new NpcEntity[IDS.length];
        for(int i=0;i<IDS.length;i++){
            var existing=l.getEntity(id(i));if(existing instanceof NpcEntity n){actors[i]=n;continue;}
            var at=safe(l,player.blockPosition(),i);if(at==null)continue;
            var n=com.mmorpg.registry.ModEntities.NPC.get().create(l,net.minecraft.world.entity.EntitySpawnReason.COMMAND);n.setUUID(id(i));n.setup(NpcEntity.Role.QUETES,NAMES[i],new int[]{4,6,8,7,5}[i],"spawn_decouverte_"+i);n.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,180,0);n.setPersistenceRequired();if(l.addFreshEntity(n))actors[i]=n;
        }
        UUID merchantId=UUID.nameUUIDFromBytes("aeloria:spawn-premium-store".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        if(l.getEntity(merchantId)==null){var at=safe(l,player.blockPosition(),5);if(at!=null){var n=com.mmorpg.registry.ModEntities.NPC.get().create(l,net.minecraft.world.entity.EntitySpawnReason.COMMAND);n.setUUID(merchantId);n.setup(NpcEntity.Role.BOUTIQUE,"Céliane · Boutique d’Aeloria",8,"");n.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,180,0);n.setPersistenceRequired();l.addFreshEntity(n);}}
        for(int i=0;i<4;i++){
            var q=ConfigManager.quests().get(IDS[i]);if(q==null||q.objectives.isEmpty()||actors[i+1]==null)continue;var objective=q.objectives.getFirst();
            if(objective.navigation==null){var n=new QuestDef.Navigation();n.dimension=l.dimension().identifier().toString();n.backend="MAIN";n.x=actors[i+1].getX();n.y=actors[i+1].getY();n.z=actors[i+1].getZ();objective.navigation=n;try{ConfigManager.saveQuest(IDS[i],q);}catch(java.io.IOException e){MMORPG.LOGGER.error("Spawn discovery quest save",e);}}
        }
        if(actors[0]!=null&&!RpgPlayers.get(player).completedQuests.contains(IDS[0]))player.sendSystemMessage(Component.literal("Découvrez la cité avec Maëlys, l’accueil du spawn. Ses quêtes vous présentent Aeloria.").withColor(0xD5B0FF));
    }
    @SubscribeEvent public static void login(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e){if(e.getEntity() instanceof ServerPlayer p)com.mmorpg.server.Scheduler.later(p.level(),60,()->install(p));}
    @SubscribeEvent public static void dimension(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent e){if(e.getEntity() instanceof ServerPlayer p)com.mmorpg.server.Scheduler.later(p.level(),40,()->install(p));}
    @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post e){
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
