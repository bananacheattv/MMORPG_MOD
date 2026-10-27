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
    @SubscribeEvent public static void left(PlayerEvent.PlayerLoggedOutEvent event) {
        if(Boolean.getBoolean("mmorpg.visualChecks")&&event.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)p.level().getServer().halt(false);
    }
}
