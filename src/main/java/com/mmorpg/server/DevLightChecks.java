package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.block.LightEmitterBlock;
import com.mmorpg.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.LightLayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.UUID;

/** Opt-in server checks for the invisible light block: ./gradlew runServer -PlightChecks (isolated run/ world only). */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class DevLightChecks {
    private static void check(boolean c, String label) {
        if (!c) throw new AssertionError("Light check failed: " + label);
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void run(ServerStartedEvent event) {
        if (!Boolean.getBoolean("mmorpg.checkLight")) return;
        var server = event.getServer();
        var level = server.overworld();
        var id = new com.mojang.authlib.GameProfile(UUID.randomUUID(), "LightChecks");
        var p = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level, id);
        BlockPos pos = new BlockPos(0, 280, 0);
        try {
            level.getChunk(pos);
            level.setBlock(pos, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            check(level.getBlockState(pos).is(net.minecraft.world.level.block.Blocks.STONE), "stone control " + level.getBlockState(pos) + " minY=" + level.getMinY() + " maxY=" + level.getMaxY());
            level.setBlock(pos, ModBlocks.BLOC_LUMIERE.get().defaultBlockState().setValue(LightEmitterBlock.LEVEL, 0), 3);
            check(level.getBlockState(pos).getBlock() instanceof LightEmitterBlock, "placed " + level.getBlockState(pos));
            check(level.getBlockState(pos).getLightEmission() == 0, "level 0 emits nothing");
            server.getPlayerList().op(new net.minecraft.server.players.NameAndId(id));
            for (int lvl : new int[]{1, 7, 15, 3}) {
                var t = new CompoundTag();
                t.putLong("pos", pos.asLong());
                t.putInt("level", lvl);
                p.snapTo(pos.getX() + .5, pos.getY(), pos.getZ() + 2.5, 0, 0);
                LightEmitterBlock.apply(p, t);
                check(level.getBlockState(pos).getValue(LightEmitterBlock.LEVEL) == lvl, "op sets level " + lvl);
                check(level.getBlockState(pos).getLightEmission() == lvl, "emission " + lvl);
            }
            var t = new CompoundTag();
            t.putLong("pos", pos.asLong());
            t.putInt("level", 99);
            LightEmitterBlock.apply(p, t);
            check(level.getBlockState(pos).getValue(LightEmitterBlock.LEVEL) == 15, "level clamped to 15");
            check(LightEmitterBlock.class != null && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty(), "no collision");
            server.getPlayerList().deop(new net.minecraft.server.players.NameAndId(id));
            t.putInt("level", 2);
            LightEmitterBlock.apply(p, t);
            check(level.getBlockState(pos).getValue(LightEmitterBlock.LEVEL) == 15, "non-op rejected");
            check(DevLightChecks.class.getResource("/assets/mmorpg/blockstates/bloc_lumiere.json") != null
                    && DevLightChecks.class.getResource("/assets/mmorpg/items/bloc_lumiere.json") != null, "resources");
            // Le chemin d'enregistrement réellement utilisé par les chunks appelait saveAsPassenger.
            var mount = new com.mmorpg.entity.MountEntity(com.mmorpg.registry.ModEntities.MOUNTS.get(com.mmorpg.mount.MountType.SANGLIER).get(),level);
            mount.setDisplay(); mount.snapTo(3,150,3,72,0);
            var out = net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess());
            check(mount.save(out), "display saved through real entity save entry point");
            var restored = new com.mmorpg.entity.MountEntity(com.mmorpg.registry.ModEntities.MOUNTS.get(com.mmorpg.mount.MountType.SANGLIER).get(),level);
            restored.load(net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess(),out.buildResult()));
            check(restored.isDisplay() && restored.shouldBeSaved() && restored.isNoAi() && restored.getYRot()==72, "display restore, flags and orientation");
            for(int i=0;i<60;i++) restored.tick();
            check(!restored.isRemoved(), "restored display survives missing rider");
            var temporary = new com.mmorpg.entity.MountEntity(com.mmorpg.registry.ModEntities.MOUNTS.get(com.mmorpg.mount.MountType.SANGLIER).get(),level);
            check(!temporary.save(net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess())), "summoned mounts remain temporary");
            // Zone vide : 26 relais plus la source, aucun terrain à écraser.
            for(int x=-3;x<=3;x++)for(int z=-3;z<=3;z++)level.getChunk(x,z);
            level.setBlock(pos, ModBlocks.BLOC_LUMIERE.get().defaultBlockState(), 3);
            com.mmorpg.block.LightFieldBlock.refresh(level,pos);
            int count=0;
            for(int x=-1;x<=1;x++)for(int y=-1;y<=1;y++)for(int z=-1;z<=1;z++)
                if(level.getBlockState(pos.offset(x*16,y*16,z*16)).is(ModBlocks.LIGHT_FIELD.get()))count++;
            check(count==26,"26 relays plus emitter: "+count);
            var east=pos.offset(16,0,0);
            level.setBlock(east,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
            com.mmorpg.block.LightFieldBlock.refresh(level,pos);
            check(level.getBlockState(east).is(net.minecraft.world.level.block.Blocks.STONE),"player construction preserved");
            level.removeBlock(east,false);
            level.setBlock(pos.offset(8,0,0),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
            com.mmorpg.block.LightFieldBlock.reconcile(level,east);
            check(!level.getBlockState(east).is(ModBlocks.LIGHT_FIELD.get()),"relay does not cross wall");
            level.removeBlock(pos.offset(8,0,0),false);
            var other=pos.offset(32,0,0); level.setBlock(other,ModBlocks.BLOC_LUMIERE.get().defaultBlockState(),3);
            com.mmorpg.block.LightFieldBlock.refresh(level,other);
            level.removeBlock(pos,false);com.mmorpg.block.LightFieldBlock.refresh(level,pos);
            check(level.getBlockState(east).is(ModBlocks.LIGHT_FIELD.get()),"overlapping lamp retained: relay="+level.getBlockState(east)+" source="+level.getBlockState(other)+" clear="+com.mmorpg.block.LightFieldBlock.clear(level,other,east));
            level.removeBlock(other,false);com.mmorpg.block.LightFieldBlock.refresh(level,other);
            check(level.getBlockState(east).isAir(),"relay cleaned after last source removed");
            // Sol sûr et distance de suivi minimale de la mascotte.
            p.snapTo(0,150,0,0,0);
            for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++)level.setBlock(new BlockPos(x,149,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
            var guide=new com.mmorpg.entity.MascotEntity(com.mmorpg.registry.ModEntities.MASCOT.get(),level);
            var ground=MascotGuide.safeGround(p,guide,MascotGuide.target(p,0));
            check(ground!=null && ground.y==150 && ground.subtract(p.position()).horizontalDistanceSqr()>=9,"mascot grounded and at least three blocks away");
            check(!guide.isNoGravity() && guide.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)>0,"mascot walks with gravity");
            DevQuestTalkChecks.run(p);
            MMORPG.LOGGER.info("[CODEX CHECKS] LIGHT PASS: niveaux 0-15, emission, clamp, refus non-op, sans collision, relais, murs, chevauchement, persistence montures, mascotte au sol");
        } catch (Throwable t) {
            MMORPG.LOGGER.error("[CODEX CHECKS] LIGHT FAIL", t);
        } finally {
            server.halt(false);
        }
    }
}
