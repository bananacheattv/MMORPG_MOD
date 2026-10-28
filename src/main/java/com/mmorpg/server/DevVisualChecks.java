package com.mmorpg.server;

import com.mmorpg.MMORPG;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Opt-in private development scene; never enabled by the deployed server command. */
@EventBusSubscriber(modid=MMORPG.MODID)
public final class DevVisualChecks {
    @SubscribeEvent public static void joined(PlayerEvent.PlayerLoggedInEvent event) {
        if(!Boolean.getBoolean("mmorpg.visualChecks")||!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer p))return;
        Scheduler.later(p.level(),40,()->{
            var server=p.level().getServer();var level=server.overworld();
            server.getPlayerList().op(new net.minecraft.server.players.NameAndId(p.getGameProfile()));
            com.mmorpg.hub.CinematicManager.finish(p);p.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
            if(Boolean.getBoolean("mmorpg.expansionVisual")){com.mmorpg.dungeon.DevExpansionVisual.install(p);return;}
            if(Boolean.getBoolean("mmorpg.guideVisual")){
                var bags=Backpacks.inventory(p);bags.setItem(0,new net.minecraft.world.item.ItemStack(com.mmorpg.registry.ModItems.BACKPACKS.get(3).get()));bags.setItem(1,new net.minecraft.world.item.ItemStack(com.mmorpg.registry.ModItems.BACKPACKS.get(2).get()));bags.setItem(2,new net.minecraft.world.item.ItemStack(com.mmorpg.registry.ModItems.get("potion_soin_majeure"),5));

                com.mmorpg.config.ConfigManager.general().customSpawning=false;
                for(int x=-10;x<=40;x++)for(int z=-5;z<=18;z++)level.setBlockAndUpdate(new BlockPos(x,219,z),net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState());
                for(int i=0;i<5;i++){
                    var guide=com.mmorpg.registry.ModEntities.HUB_GUIDE.get().create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);guide.guide(i);guide.snapTo(i*5,220,0,0,0);level.addFreshEntity(guide);
                    var master=com.mmorpg.registry.ModEntities.CLASS_MASTER.get().create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);master.setup(i);master.snapTo(i*5,220,7,0,0);master.setNoGravity(true);level.addFreshEntity(master);
                }
                if(Boolean.getBoolean("mmorpg.guidanceVisual")){
                    var q=new com.mmorpg.quest.QuestDef();q.name="Rencontrer l’éclaireur";var objective=new com.mmorpg.quest.QuestDef.Objective("kill","gobelin",1);objective.navigation=new com.mmorpg.quest.QuestDef.Navigation();objective.navigation.dimension=level.dimension().identifier().toString();objective.navigation.backend=com.mmorpg.network.NetworkConfig.current.role.name();objective.navigation.x=5;objective.navigation.y=220;objective.navigation.z=0;q.objectives.add(objective);com.mmorpg.config.ConfigManager.quests().put("codex_visual_guidance",q);var d=RpgPlayers.get(p);d.activeQuests.clear();d.activeQuests.put("codex_visual_guidance",new int[]{0});d.trackedQuest="codex_visual_guidance";d.questGuidanceMuted=false;com.mmorpg.hub.ClassTrial.data(p).tutorialMask=0;
                }
                p.teleportTo(level,10,222,15,java.util.Set.of(),180,8,true);p.getAbilities().flying=true;p.onUpdateAbilities();return;
            }
            if(Boolean.getBoolean("mmorpg.socialVisual")){
                com.mmorpg.config.ConfigManager.general().customSpawning=false;
                RpgPlayers.get(p).playerClass=com.mmorpg.rpg.PlayerClass.GUERRIER;
                for(int i=0;i<10;i++){
                    for(int cx=-1;cx<=1;cx++)for(int cz=-1;cz<=1;cz++)level.setChunkForced(((5000+i*50)>>4)+cx,cz,true);
                    for(int x=-13;x<=13;x++)for(int z=-13;z<=13;z++)level.setBlockAndUpdate(new BlockPos(5000+i*50+x,219,z),net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState());
                    var boss=com.mmorpg.registry.ModEntities.GUILD_GUARDIANS.get(i).get().create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);boss.guild=java.util.UUID.randomUUID();boss.initialize();boss.setNoAi(true);boss.snapTo(5000+i*50+.5,220,.5,0,0);level.addFreshEntity(boss);
                }
                for(int i=0;i<3;i++){
                    int x0=5550+i*50;for(int cx=-1;cx<=1;cx++)for(int cz=-1;cz<=1;cz++)level.setChunkForced((x0>>4)+cx,cz,true);
                    for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++)level.setBlockAndUpdate(new BlockPos(x0+x,219,z),net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState());
                    var crystal=com.mmorpg.registry.ModEntities.ARENA_CRYSTAL.get().create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);crystal.setTeam(i-1);crystal.snapTo(x0+.5,220,.5,0,0);level.addFreshEntity(crystal);
                }
                var arenaLevel=com.mmorpg.competition.CompetitionWorld.level(server);
                for(int i=0;i<2;i++){var job=com.mmorpg.competition.CompetitionWorld.arena(com.mmorpg.competition.CompetitionWorld.center(i),com.mmorpg.competition.ArenaRules.Kind.values()[i]);while(!job.done())job.tick(arenaLevel);}
                var cave=com.mmorpg.competition.CompetitionWorld.raid(new BlockPos(500,100,4000),0);while(!cave.done())cave.tick(arenaLevel);
                var stand=net.minecraft.world.entity.EntityTypes.ARMOR_STAND.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);stand.setShowArms(true);stand.setNoGravity(true);stand.snapTo(5015,225,15,180,0);stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new net.minecraft.world.item.ItemStack(com.mmorpg.registry.ModItems.get("excalibur_celeste")));stand.setRightArmPose(new net.minecraft.core.Rotations(-100,0,20));level.addFreshEntity(stand);
                for(int i=0;i<2;i++){
                    var comparison=net.minecraft.world.entity.EntityTypes.ARMOR_STAND.create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);comparison.setShowArms(true);comparison.setNoGravity(true);comparison.snapTo(5013+i*4,225,14,0,0);comparison.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,new net.minecraft.world.item.ItemStack(i==0?net.minecraft.world.item.Items.SHIELD:com.mmorpg.registry.ModItems.PALADIN_SHIELDS.get(2).get()));level.addFreshEntity(comparison);
                }
                RpgPlayers.get(p).playerClass=com.mmorpg.rpg.PlayerClass.TANK;RpgPlayers.get(p).level=60;RpgPlayers.recomputeAndSync(p);
                p.getInventory().clearContent();p.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,new net.minecraft.world.item.ItemStack(com.mmorpg.registry.ModItems.get("marteau_runique")));
                p.getInventory().setItem(4,new net.minecraft.world.item.ItemStack(com.mmorpg.registry.ModItems.get("pioche_verdite")));
                p.teleportTo(level,5015,226,18,java.util.Set.of(),140F,8F,true);p.getAbilities().flying=true;p.onUpdateAbilities();return;
            }
            RpgPlayers.get(p).playerClass=com.mmorpg.rpg.PlayerClass.GUERRIER;
            for(int x=-15;x<=15;x++)for(int z=-10;z<=16;z++)level.setBlockAndUpdate(new BlockPos(x,219,z),net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState());
            p.teleportTo(level,8.5,222,11.5,java.util.Set.of(),145F,10F,true);
            p.getAbilities().flying=true;p.onUpdateAbilities();
            SpawnEditor.open(p,new BlockPos(2,220,0));SpawnEditor.placeOriented(p,"forge_arcanique","moi");
            SpawnEditor.open(p,new BlockPos(-4,220,4));SpawnEditor.placeOriented(p,"caisse_commune_bloc","moi");
            var npc=com.mmorpg.world.DungeonBuilder.spawnNpc(level,new BlockPos(6,220,8),com.mmorpg.entity.NpcEntity.Role.MARCHAND,"Échoppe visuelle",0,0);
            Scheduler.later(level,40,()->MerchantEditor.open(p,npc));
        });
    }
    @SubscribeEvent public static void guideDimension(PlayerEvent.PlayerChangedDimensionEvent event){
        if(!Boolean.getBoolean("mmorpg.guideVisual")||!(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)||!p.level().dimension().equals(com.mmorpg.hub.HubManager.HUB_DIM))return;
        Scheduler.later(p.level(),20,()->{for(int i=0;i<5;i++){var actor=p.level().getEntity(com.mmorpg.hub.HubGuides.id(0,i));if(actor!=null)actor.discard();}var hub=com.mmorpg.hub.HubData.get(p.level().getServer());com.mmorpg.hub.HubGuides.install(p.level(),hub.origin(0),0);});
        Scheduler.later(p.level(),100,()->{
            var hub=com.mmorpg.hub.HubData.get(p.level().getServer());int found=0;for(int i=0;i<5;i++)if(p.level().getEntity(com.mmorpg.hub.HubGuides.id(0,i)) instanceof com.mmorpg.entity.hub.HubGuideEntity guide){found++;var at=guide.blockPosition();if(!p.level().getBlockState(at).getCollisionShape(p.level(),at).isEmpty()||!p.level().getBlockState(at.above()).getCollisionShape(p.level(),at.above()).isEmpty())throw new AssertionError("Guide embedded in hub construction: "+i);}
            if(found!=5)throw new AssertionError("Missing hub guides: "+found);var tp=hub.origin(0).offset(com.mmorpg.hub.HubLayout.TELEPORTER[0],0,com.mmorpg.hub.HubLayout.TELEPORTER[2]);if(p.level().getBlockState(tp).is(com.mmorpg.registry.ModBlocks.TELEPORTEUR.get()))throw new AssertionError("Legacy hub teleporter still present");MMORPG.LOGGER.info("[CODEX CHECKS] HUB_GUIDE_WORLD PASS: five guides accessible in actual hub, no portal block, no hub rebuild");
        });
    }
    @SubscribeEvent public static void left(PlayerEvent.PlayerLoggedOutEvent event) {
        if(Boolean.getBoolean("mmorpg.visualChecks")&&event.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)p.level().getServer().halt(false);
    }
}
