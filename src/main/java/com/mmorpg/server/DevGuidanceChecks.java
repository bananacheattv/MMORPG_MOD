package com.mmorpg.server;

import com.mmorpg.hub.*;
import com.mmorpg.quest.QuestDef;
import com.mmorpg.network.*;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.nbt.CompoundTag;
import java.util.*;

public final class DevGuidanceChecks {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError("guidance: "+message);}
    public static void run(ServerPlayer p){
        var original=NetworkConfig.current;var cfg=new NetworkConfig();cfg.role=NetworkConfig.Role.MAIN;NetworkConfig.current=cfg;
        var d=RpgPlayers.get(p);var saved=d.save();var level=p.level();var at=p.position();int mask=ClassTrial.data(p).tutorialMask;String id="codex_guidance_"+UUID.randomUUID().toString().replace("-","");var entities=new ArrayList<net.minecraft.world.entity.Entity>();var portals=new ArrayList<UUID>();
        try{
            for(float size:new float[]{.18f,.45f}){var display=GuidanceDisplayData.create(size);var transform=display.read("transformation",com.mojang.math.Transformation.EXTENDED_CODEC).orElseThrow();check(Math.abs(transform.scale().x()-size)<.0001&&Math.abs(transform.scale().y()-size)<.0001&&Math.abs(transform.scale().z()-size)<.0001,"3D display scale survives the complete vanilla transformation codec");check(display.getStringOr("billboard","").equals("fixed"),"3D arrow is horizontal, never a camera-facing billboard");}
            d.activeQuests.clear();d.questGivers.clear();d.trackedQuest="";d.questGuidanceMuted=false;d.level=1;d.playerClass=com.mmorpg.rpg.PlayerClass.GUERRIER;ClassTrial.data(p).tutorialMask=31;
            check(GuidanceManager.resolve(p)==null,"no target when there is no tutorial or quest");
            check(GuidanceManager.nextGuide(0)==0&&GuidanceManager.nextGuide(1)==1&&GuidanceManager.nextGuide(31)==-1,"automatic tutorial sequence");
            var q=new QuestDef();q.name="Navigation de test";q.objectives.add(new QuestDef.Objective("kill","minecraft:cow",1));q.objectives.add(new QuestDef.Objective("kill","minecraft:cow",1));
            for(int i=0;i<2;i++){var n=new QuestDef.Navigation();n.dimension=level.dimension().identifier().toString();n.backend="MAIN";n.x=at.x+20+i*10;n.y=at.y;n.z=at.z;q.objectives.get(i).navigation=n;}
            com.mmorpg.config.ConfigManager.quests().put(id,q);d.activeQuests.put(id,new int[]{0,0});d.trackedQuest=id;
            QuestManager.track(p,"");check(GuidanceManager.resolve(p)==null,"manual untracking suppresses quest arrow");QuestManager.track(p,id);
            var target=GuidanceManager.resolve(p);check(target!=null&&Math.abs(target.pos().x-at.x-20)<.01,"first unfinished objective guides automatically");
            d.activeQuests.get(id)[0]=1;check(Math.abs(GuidanceManager.resolve(p).pos().x-at.x-30)<.01,"next objective selected without command");
            var giver=new CompoundTag();giver.putString("id",UUID.randomUUID().toString());giver.putString("name","Donneur sauvegardé");giver.putString("dimension",level.dimension().identifier().toString());giver.putString("backend","MAIN");giver.putLong("pos",p.blockPosition().offset(45,0,0).asLong());d.questGivers.put(id,giver);d.activeQuests.get(id)[1]=1;
            target=GuidanceManager.resolve(p);check(target.label().contains("Donneur sauvegardé")&&target.pos()!=null,"completed quest guides back to accepted giver even after chunk unload");
            var restored=new com.mmorpg.rpg.PlayerData();restored.load(d.save());check(restored.questGivers.get(id).equals(giver),"giver survives profile save and cross-server handoff");
            d.activeQuests.put(id,new int[]{0,0});q.objectives.get(0).navigation.backend="PLAINS";check(GuidanceManager.resolve(p).pos()==null,"same dimension ID on another backend never yields false coordinates");q.objectives.get(0).navigation.backend="MAIN";
            q.objectives.get(0).navigation.x=Double.NaN;boolean refused=false;try{QuestEditor.validate(id,q,com.mmorpg.world.NpcDirectory.get(level.getServer()));}catch(IllegalArgumentException expected){refused=true;}check(refused,"invalid authored marker refused");q.objectives.get(0).navigation.x=at.x+20;
            q.objectives.get(0).navigation=null;q.objectives.remove(1);d.activeQuests.put(id,new int[]{0});
            var cow=net.minecraft.world.entity.EntityTypes.COW.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);cow.snapTo(at.x+2,at.y,at.z,0,0);cow.setNoAi(true);level.addFreshEntity(cow);entities.add(cow);
            target=GuidanceManager.resolve(p);check(target.pos()!=null&&target.pos().distanceTo(cow.position())<.1,"nearby objective mob becomes direction target");cow.discard();
            q.objectives.set(0,new QuestDef.Objective("collect","oreille_gobelin",64));d.activeQuests.put(id,new int[]{0});p.getInventory().clearContent();
            var foreign=new net.minecraft.world.entity.item.ItemEntity(level,at.x+.5,at.y,at.z,new net.minecraft.world.item.ItemStack(com.mmorpg.registry.ModItems.OREILLE_GOBELIN.get()));foreign.addTag("aeloria_loot:"+UUID.randomUUID());level.addFreshEntity(foreign);entities.add(foreign);
            var own=new net.minecraft.world.entity.item.ItemEntity(level,at.x+3,at.y,at.z,new net.minecraft.world.item.ItemStack(com.mmorpg.registry.ModItems.OREILLE_GOBELIN.get()));own.addTag(PetLoot.tag(p));level.addFreshEntity(own);entities.add(own);
            target=GuidanceManager.resolve(p);check(target.pos()!=null&&target.pos().distanceTo(own.position())<.1,"collection ignores other players’ mob loot");own.discard();foreign.discard();
            q.objectives.set(0,new QuestDef.Objective("kill","avatar_neant",1));d.level=100;d.activeQuests.put(id,new int[]{0});var portalId=UUID.randomUUID();portals.add(portalId);var marker=p.blockPosition().offset(25,0,0);com.mmorpg.world.AdventurePortalData.get(level.getServer()).put(new com.mmorpg.world.AdventurePortalData.Portal(portalId,level.dimension(),marker,true,"donjon",-1,"citadelle_neant",0));
            target=GuidanceManager.resolve(p);check(target.pos()!=null&&target.pos().distanceTo(net.minecraft.world.phys.Vec3.atCenterOf(marker))<.1,"named boss quest guides to its matching dungeon portal");d.level=1;
            d.activeQuests.clear();d.questGivers.clear();q.objectives.set(0,new QuestDef.Objective("level","*",2));var npc=com.mmorpg.registry.ModEntities.NPC.get().create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);npc.setup(com.mmorpg.entity.NpcEntity.Role.QUETES,"Donneur navigation",0,"navigation_checks");npc.snapTo(at.x+1,at.y,at.z,0,0);level.addFreshEntity(npc);entities.add(npc);q.npc=npc.getUUID().toString();q.giver="navigation_checks";
            QuestManager.accept(p,id,npc.getId());check(d.trackedQuest.equals(id)&&d.questGivers.containsKey(id),"accepted quest tracks itself and records its actual giver");
            var hub=HubData.get(level.getServer());if(hub.exists()){
                var hubLevel=hub.level(level.getServer());p.setServerLevel(hubLevel);var origin=hub.origin(0);p.snapTo(origin.getX(),origin.getY(),origin.getZ()+46,180,0);cfg.role=NetworkConfig.Role.HUB;ClassTrial.data(p).tutorialMask=0;
                target=GuidanceManager.resolve(p);check(target.kind().equals("tutorial")&&target.label().contains("Sélène"),"arrival starts tutorial guidance without slash command");ClassTrial.data(p).tutorialMask=1;check(GuidanceManager.resolve(p).label().contains("Kaël"),"next guide selected automatically");
            }
            com.mmorpg.MMORPG.LOGGER.info("[CODEX CHECKS] GUIDANCE PASS: automatic tutorial, next guide/objective, accept tracking, saved giver, return, backend isolation, foreign loot and invalid marker");
        }finally{p.setServerLevel(level);p.snapTo(at.x,at.y,at.z,0,0);d.load(saved);ClassTrial.data(p).tutorialMask=mask;entities.forEach(e->{if(!e.isRemoved())e.discard();});portals.forEach(portal->com.mmorpg.world.AdventurePortalData.get(level.getServer()).remove(portal));com.mmorpg.config.ConfigManager.quests().remove(id);NetworkConfig.current=original;}
    }
}
