package com.mmorpg.hub;

import com.mmorpg.entity.hub.HubGuideEntity;
import com.mmorpg.network.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.network.chat.Component;
import net.minecraft.nbt.CompoundTag;
import java.util.*;

/** Small, idempotent hub additions; never rebuilds the hub or edits the player's spawn. */
@net.neoforged.fml.common.EventBusSubscriber(modid=com.mmorpg.MMORPG.MODID)
public final class HubGuides {
    public static final String[] MODELS={"selene","kael","borin","elyra","neria"};
    public static final String[] NAMES={"Sélène · Archiviste","Kaël · Éclaireur","Borin · Maître artisan","Élyra · Gardienne des guildes","Néria · Capitaine"};
    public static final int[][] SPOTS={{-2,0,42},{-10,0,10},{8,0,24},{27,0,0},{-27,0,0}};
    public static UUID id(int copy,int guide){return UUID.nameUUIDFromBytes(("aeloria:hub-guide:"+copy+":"+guide).getBytes(java.nio.charset.StandardCharsets.UTF_8));}
    public static void install(ServerLevel level,BlockPos origin,int copy){
        HubBuilder.removeRetiredTutorialTexts(level,copy);
        // Only remove the registered teleporter block itself, preserving its plinth and all surrounding construction.
        BlockPos old=HubLayout.pos(origin,HubLayout.TELEPORTER);
        if(!level.hasChunkAt(old))return;
        if(level.getBlockState(old).is(com.mmorpg.registry.ModBlocks.TELEPORTEUR.get())){level.removeBlock(old,false);com.mmorpg.world.WaypointData.get(level.getServer()).removeAt(level.dimension(),old);}
        for(int i=0;i<5;i++){
            var uuid=id(copy,i);var at=origin.offset(SPOTS[i][0],SPOTS[i][1],SPOTS[i][2]);if(!level.areEntitiesActuallyLoadedAndTicking(new net.minecraft.world.level.ChunkPos(at.getX()>>4,at.getZ()>>4)))continue;
            if(level.getEntity(uuid)!=null)continue;
            var npc=com.mmorpg.registry.ModEntities.HUB_GUIDE.get().create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            npc.setUUID(uuid);npc.guide(i);float yaw=i==0?-27:(float)Math.toDegrees(Math.atan2(SPOTS[i][0],-SPOTS[i][2]));npc.snapTo(at.getX()+.5,at.getY(),at.getZ()+.5,yaw,0);npc.setYHeadRot(yaw);npc.setYBodyRot(yaw);npc.addTag("aeloria_hub_guide");level.addFreshEntity(npc);
        }
    }
    @net.neoforged.bus.api.SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event){
        var server=event.getServer();if(server.getTickCount()%40!=0||!NetworkConfig.hosts(NetworkConfig.Role.HUB))return;
        var hub=HubData.get(server);if(!hub.exists())return;var level=hub.level(server);if(level!=null)for(int i=0;i<hub.count();i++)install(level,hub.origin(i),i);
    }
    public static boolean near(ServerPlayer p,HubGuideEntity npc){return p.level()==npc.level()&&p.distanceToSqr(npc)<=64&&!npc.isRemoved()&&p.hasLineOfSight(npc);}
    public static void open(ServerPlayer p,HubGuideEntity npc){
        if(!near(p,npc)||CinematicManager.isLocked(p))return;
        var t=new CompoundTag();t.putInt("entity",npc.getId());t.putInt("guide",npc.guide());t.putInt("completed",ClassTrial.data(p).tutorialMask);t.putBoolean("hasClass",com.mmorpg.server.RpgPlayers.get(p).playerClass.isPlayable());
        Net.toPlayer(p,new S2COpenScreen(S2COpenScreen.HUB_GUIDE,t));
    }
    public static void complete(ServerPlayer p,int entity){
        if(!(p.level().getEntity(entity) instanceof HubGuideEntity npc)||!near(p,npc)||CinematicManager.isLocked(p))return;
        int before=ClassTrial.data(p).tutorialMask;ClassTrial.data(p).tutorialMask|=1<<npc.guide();
        if((before&31)!=31&&(ClassTrial.data(p).tutorialMask&31)==31)Net.toPlayer(p,new Payloads.Notify(Payloads.Notify.QUEST,"TUTORIEL TERMINÉ","Les guides restent disponibles. Bonne aventure !",0xFFD040));
        com.mmorpg.server.GuidanceManager.refresh(p);
    }
    public static void point(ServerPlayer p){
        var hub=HubData.get(p.level().getServer());if(!hub.exists()){p.sendSystemMessage(Component.literal("Le tutoriel se trouve à la Citadelle des héros."));return;}
        int next=0;while(next<5&&(ClassTrial.data(p).tutorialMask&(1<<next))!=0)next++;
        if(next==5){p.sendSystemMessage(Component.literal("Tutoriel terminé ! Les cinq guides restent disponibles dans le hub."));return;}
        var at=hub.originAt(p.getX()).offset(SPOTS[next][0],SPOTS[next][1],SPOTS[next][2]);
        p.sendSystemMessage(Component.literal("Guide "+(next+1)+" / 5 : "+NAMES[next]+" · Hub · X "+at.getX()+" / Y "+at.getY()+" / Z "+at.getZ()));
        if(p.level().dimension().equals(hub.dimension())&&p.level().getEntity(id(hub.indexAt(p.getX()),next)) instanceof HubGuideEntity npc){npc.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.GLOWING,300,0,false,false));}
    }
}
