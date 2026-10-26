package com.mmorpg.server;

import com.mmorpg.block.structure.MultiblockKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;

/** Isolated development world only; exercises placement and exact undo with real block entities. */
public final class DevSpawnEditorChecks {
    private static void check(boolean value,String message) { if(!value)throw new AssertionError("spawn editor: "+message); }
    public static void run(ServerPlayer p) {
        var level=p.level();var server=level.getServer();
        var identity=new net.minecraft.server.players.NameAndId(p.getGameProfile());
        for(int x=65;x<=105;x++)for(int z=65;z<=85;z++) {
            var ground=new BlockPos(x,99,z);level.getChunkAt(ground);level.setBlockAndUpdate(ground,Blocks.STONE.defaultBlockState());
            for(int y=100;y<108;y++)level.setBlockAndUpdate(new BlockPos(x,y,z),Blocks.AIR.defaultBlockState());
        }
        p.setPos(70.5,100,70.5);var at=new BlockPos(74,100,70);
        server.getPlayerList().deop(identity);
        check(!SpawnEditor.place(p,"forge_arcanique",Direction.NORTH),"non operator denied");
        server.getPlayerList().op(identity);
        try {
            check(!PortalAccess.consume(p,"mine"),"direct entry denied without portal");PortalAccess.grant(p,"mine");
            check(!PortalAccess.consume(p,"plaines"),"portal permission cannot open another activity");
            check(PortalAccess.consume(p,"mine")&&!PortalAccess.consume(p,"mine"),"portal pass single use");
            var originalSummary=RpgPlayers.get(p).adventureSummary.copy();AdventureSummary.begin(p,"mine");
            AdventureSummary.count(p,"xp",42);AdventureSummary.item(p,com.mmorpg.registry.ModItems.PIECE_OR.get(),3);
            var summaryCopy=new com.mmorpg.rpg.PlayerData();summaryCopy.load(RpgPlayers.get(p).save());
            check(summaryCopy.adventureSummary.getLongOr("xp",0)==42&&summaryCopy.adventureSummary.getCompoundOrEmpty("items").getLongOr("mmorpg:piece_or",0)==3,"reward journal survives shared profile serialization");
            var foreign=new net.minecraft.world.entity.item.ItemEntity(level,70,100,70,new net.minecraft.world.item.ItemStack(com.mmorpg.registry.ModItems.PIECE_OR.get(),2));
            foreign.addTag("aeloria_loot:"+java.util.UUID.randomUUID());PetLoot.collect(p,foreign);check(foreign.getItem().getCount()==2,"pet never takes foreign drops");
            var own=new net.minecraft.world.entity.item.ItemEntity(level,70,100,70,new net.minecraft.world.item.ItemStack(com.mmorpg.registry.ModItems.PIECE_OR.get(),2));
            own.addTag(PetLoot.tag(p));PetLoot.collect(p,own);check(!own.isAlive()&&RpgPlayers.get(p).adventureSummary.getCompoundOrEmpty("items").getLongOr("mmorpg:piece_or",0)==5,"pet transfers and counts only accepted owned loot");
            RpgPlayers.get(p).adventureSummary=originalSummary;
            var mob=com.mmorpg.registry.ModEntities.GOBLIN_KING.get().create(level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            var drops=new java.util.ArrayList<net.minecraft.world.entity.item.ItemEntity>();
            drops.add(new net.minecraft.world.entity.item.ItemEntity(level,70,100,70,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND)));
            drops.add(new net.minecraft.world.entity.item.ItemEntity(level,70,100,70,new net.minecraft.world.item.ItemStack(com.mmorpg.registry.ModItems.PIECE_OR.get())));
            VanillaMobDrops.drops(new net.neoforged.neoforge.event.entity.living.LivingDropsEvent(mob,p.damageSources().playerAttack(p),drops,true));
            check(drops.size()==1&&drops.getFirst().entityTags().contains(PetLoot.tag(p)),"vanilla drops removed, mod loot assigned to killer");
            p.getFoodData().setFoodLevel(4);ServerEvents.onPlayerTick(new net.neoforged.neoforge.event.tick.PlayerTickEvent.Post(p));
            check(p.getFoodData().getFoodLevel()==20,"hunger restored in every world");
            int kept=0;for(int i=0;i<10000;i++)if(com.mmorpg.mine.MineDensity.keep(new BlockPos(i,100,22000)))kept++;
            check(kept>1800&&kept<2200,"stable one-fifth ore density");
            check(com.mmorpg.item.RpgBowItem.rpgPowerForTime(0)==0&&com.mmorpg.item.RpgBowItem.rpgPowerForTime(9)<1&&com.mmorpg.item.RpgBowItem.rpgPowerForTime(10)==1,"bows reach full power at 10 ticks");
            var spawnLevel=server.getLevel(com.mmorpg.hub.SpawnBuilder.DIM);
            check(spawnLevel!=null,"spawn dimension available");
            var config=com.mmorpg.config.ConfigManager.spawn();boolean manual=config.manualBuild,flat=config.flatPlatform;
            var spawnData=com.mmorpg.hub.SpawnData.get(server);var oldOrigin=spawnData.origin();int oldLayout=spawnData.layout();
            try {
                config.manualBuild=true;config.flatPlatform=false;
                for(int y=0;y<256;y++)check(spawnLevel.getBlockState(new BlockPos(2048,y,2048)).isAir(),"new spawn chunks contain only air");
                com.mmorpg.hub.SpawnBuilder.ensure(server);
                check(spawnData.origin().equals(new BlockPos(0,96,0)),"manual spawn arrival point");
                var sample=new com.mmorpg.hub.SpawnData();sample.set(new BlockPos(0,96,0),Integer.MAX_VALUE);
                var exact=new net.minecraft.world.phys.Vec3(12.25,111.5,-8.75);sample.setArrival(exact,-45F,12F);
                var encoded=com.mmorpg.hub.SpawnData.CODEC.encodeStart(net.minecraft.nbt.NbtOps.INSTANCE,sample).getOrThrow();
                var decoded=com.mmorpg.hub.SpawnData.CODEC.parse(net.minecraft.nbt.NbtOps.INSTANCE,encoded).getOrThrow();
                check(decoded.arrival().equals(exact)&&decoded.yaw()==-45F&&decoded.pitch()==12F,"exact arrival coordinates and orientation persist");
                spawnLevel.setBlockAndUpdate(new BlockPos(1,96,0),Blocks.DIAMOND_BLOCK.defaultBlockState());
                check(!com.mmorpg.hub.SpawnBuilder.ensure(server)&&spawnLevel.getBlockState(new BlockPos(1,96,0)).is(Blocks.DIAMOND_BLOCK),"manual buildings survive ensure/restart path");
            } finally {config.manualBuild=manual;config.flatPlatform=flat;if(oldOrigin!=null)spawnData.set(oldOrigin,oldLayout);}
            for(var k:MultiblockKind.values()) {
                SpawnEditor.open(p,at);
                check(SpawnEditor.place(p,k.key(),Direction.EAST),"place "+k);
                check(level.getBlockState(at).is(k.masterBlock()),"master "+k);
                check(AdventurePortals.undo(p),"unified undo "+k);
                check(level.getBlockState(at).isAir(),"restore master air");
                for(int[] cell:k.cells())check(level.getBlockState(k.cellPos(at,cell,Direction.EAST)).isAir(),"restore all parts");
            }
            for(var kind:MultiblockKind.values()) for(float yaw:new float[]{45F,123.5F,-37.2F}) {
                SpawnEditor.open(p,at);check(SpawnEditor.placeOriented(p,kind.key(),Float.toString(yaw)),"free placement "+kind+" "+yaw);
                var be=(com.mmorpg.block.structure.OrientedBlockEntity)level.getBlockEntity(at);
                check(Math.abs(be.yaw-yaw)<.001,"exact yaw retained");
                var tag=be.getUpdateTag(level.registryAccess());check(Math.abs(tag.getFloatOr("placementYaw",999)-yaw)<.001,"yaw saved and synchronized");
                var loaded=new com.mmorpg.block.structure.StructureBlockEntity(at,level.getBlockState(at));
                loaded.loadWithComponents(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),tag));
                check(Math.abs(loaded.yaw-yaw)<.001&&loaded.shapes().keySet().equals(be.shapes().keySet()),"reloaded yaw rebuilds the same collision footprint");
                for(var cell:be.shapes().keySet())if(!cell.equals(BlockPos.ZERO)) {
                    var part=level.getBlockState(at.offset(cell));
                    check(com.mmorpg.block.structure.StructurePartBlock.masterPos(at.offset(cell),part).equals(at),"diagonal part points to its own master");
                    check(!part.getShape(level,at.offset(cell)).isEmpty(),"diagonal part collision");
                }
                var footprint=java.util.List.copyOf(be.shapes().keySet());check(AdventurePortals.undo(p),"free placement undo");
                for(var cell:footprint)check(level.getBlockState(at.offset(cell)).isAir(),"free footprint restored exactly");
            }
            SpawnEditor.open(p,at);check(SpawnEditor.placeOriented(p,"caisse_commune_bloc","moi"),"crate faces placer");
            var crate=(com.mmorpg.block.structure.OrientedBlockEntity)level.getBlockEntity(at);
            check(Math.abs(crate.yaw-90)<.001,"crate front faces west towards player");check(AdventurePortals.undo(p),"diagonal crate undo");
            SpawnEditor.open(p,at);check(!SpawnEditor.placeOriented(p,"portail:mine","45"),"free rotation never changes portals");
            check(SpawnEditor.textOriented(p,"Angle fixe",2,3,"123.5"),"fixed text orientation");
            var fixed=level.getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class,new net.minecraft.world.phys.AABB(at).inflate(8),e->e.entityTags().contains("aeloria_editor")).getFirst();
            check(Math.abs(fixed.getYRot()-123.5)<.001,"text angle applied");check(AdventurePortals.undo(p),"fixed text undo");
            SpawnEditor.open(p,at);
            level.setBlockAndUpdate(at,Blocks.CHEST.defaultBlockState());
            check(!SpawnEditor.place(p,"forge_arcanique",Direction.NORTH),"never replace existing inventory block");level.removeBlock(at,false);
            check(SpawnEditor.text(p,"Bienvenue \"héros\" \\ test",2,3),"escaped literal floating text");
            check(!level.getEntitiesOfClass(net.minecraft.world.entity.Display.TextDisplay.class,new net.minecraft.world.phys.AABB(at).inflate(8),e->e.entityTags().contains("aeloria_editor")).isEmpty(),"text exists");
            check(AdventurePortals.undo(p),"undo text creation");
            var portal=new com.mmorpg.world.AdventurePortalData.Portal(java.util.UUID.randomUUID(),level.dimension(),new BlockPos(94,100,74),true,"hub",-1,"",0);
            check(AdventurePortals.place(p,portal),"hub portal");
            SpawnEditor.open(p,at);check(SpawnEditor.place(p,"caisse_commune_bloc",Direction.NORTH),"crate");
            check(AdventurePortals.undo(p)&&level.getBlockState(at).isAir(),"newest structure before older portal");
            check(com.mmorpg.world.AdventurePortalData.get(server).find(portal.id())!=null,"older portal retained");
            check(AdventurePortals.undo(p)&&com.mmorpg.world.AdventurePortalData.get(server).find(portal.id())==null,"older portal then undone");
            SpawnEditor.open(p,at);check(SpawnEditor.place(p,"caisse_commune_bloc",Direction.NORTH),"crate conflict fixture");
            level.setBlockAndUpdate(at,Blocks.DIAMOND_BLOCK.defaultBlockState());
            check(!AdventurePortals.undo(p)&&level.getBlockState(at).is(Blocks.DIAMOND_BLOCK),"later builder change preserved");
            level.removeBlock(at,false);
            com.mmorpg.MMORPG.LOGGER.info("[CODEX CHECKS] SPAWN_EDITOR PASS: permissions, six multiblocks, collision checks, escaped text, crates, shared chronological undo, conflict refusal");
        } finally {server.getPlayerList().deop(identity);}
    }
}
