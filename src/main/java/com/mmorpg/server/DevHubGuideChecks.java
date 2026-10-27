package com.mmorpg.server;
import com.mmorpg.hub.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import com.mmorpg.entity.hub.HubGuideEntity;
public final class DevHubGuideChecks {
    private static void check(boolean ok,String text){if(!ok)throw new AssertionError("Hub guides: "+text);}
    public static void run(ServerPlayer p){
        int copy=10000+new java.util.Random().nextInt(1000000000);
        var level=p.level();var origin=new BlockPos(7000,220,7000);var portal=origin.offset(HubLayout.TELEPORTER[0],HubLayout.TELEPORTER[1],HubLayout.TELEPORTER[2]);
        level.setBlockAndUpdate(portal,com.mmorpg.registry.ModBlocks.TELEPORTEUR.get().defaultBlockState());level.setBlockAndUpdate(portal.east(),net.minecraft.world.level.block.Blocks.GOLD_BLOCK.defaultBlockState());
        HubGuides.install(level,origin,copy);HubGuides.install(level,origin,copy);
        check(level.getBlockState(portal).isAir(),"only portal block removed");check(level.getBlockState(portal.east()).is(net.minecraft.world.level.block.Blocks.GOLD_BLOCK),"surrounding construction untouched");
        for(int i=0;i<5;i++){
            var npc=com.mmorpg.registry.ModEntities.HUB_GUIDE.get().create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);npc.guide(i);npc.setUUID(HubGuides.id(copy,i));npc.snapTo(p.getX()+i*.5,p.getY(),p.getZ()+2,0,0);level.addFreshEntity(npc);
            check(npc.guide()==i,"guide identity "+i);var saved=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess());npc.saveWithoutId(saved);var loaded=com.mmorpg.registry.ModEntities.HUB_GUIDE.get().create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);loaded.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),saved.buildResult()));check(loaded.guide()==i&&loaded.isNoGravity(),"kind and persistence survive reload");
        }
        var guide=(HubGuideEntity)level.getEntity(HubGuides.id(copy,0));int before=ClassTrial.data(p).tutorialMask;var departure=p.position();p.snapTo(departure.x+30,departure.y,departure.z,0,0);HubGuides.complete(p,guide.getId());check(ClassTrial.data(p).tutorialMask==before,"remote completion refused");
        var old=departure;float yaw=p.getYRot();p.snapTo(guide.getX(),guide.getY(),guide.getZ()+2,0,0);HubGuides.complete(p,guide.getId());check((ClassTrial.data(p).tutorialMask&1)!=0,"nearby conversation completion persisted");
        var out=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess());ClassTrial.data(p).serialize(out);var fresh=new HubPlayerData();fresh.deserialize(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),out.buildResult()));check(fresh.tutorialMask==ClassTrial.data(p).tutorialMask,"tutorial progress survives reconnect");ClassTrial.data(p).tutorialMask=before;p.snapTo(old.x,old.y,old.z,yaw,0);
        for(int i=0;i<5;i++)level.getEntity(HubGuides.id(copy,i)).discard();
        com.mmorpg.MMORPG.LOGGER.info("[CODEX CHECKS] HUB_GUIDES PASS: unique actors, reload, proximity permissions, saved progress, portal block only, construction preserved");
    }
}
