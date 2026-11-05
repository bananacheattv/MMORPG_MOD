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
            // Portee etendue : degrade depuis un point, pas de motif repete, murs respectes, nettoyage.
            for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++)level.getChunk(x,z);
            level.setBlock(pos, ModBlocks.BLOC_LUMIERE.get().defaultBlockState().setValue(LightEmitterBlock.RANGE, 2), 3);
            LightNetwork.runNow(level,pos,40,15,true);
            var near=pos.offset(8,0,0); var far=pos.offset(36,0,0);
            check(level.getBlockState(near).is(ModBlocks.LIGHT_FIELD.get()) && level.getBlockState(far).is(ModBlocks.LIGHT_FIELD.get()),"relays up to range");
            check(level.getBlockState(near).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LEVEL)
                    > level.getBlockState(far).getValue(net.minecraft.world.level.block.state.properties.BlockStateProperties.LEVEL),"light decreases with distance");
            check(!level.getBlockState(pos.offset(44,0,0)).is(ModBlocks.LIGHT_FIELD.get()),"nothing beyond range");
            level.setBlock(pos.offset(20,0,0),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
            LightNetwork.runNow(level,pos,40,15,true);
            check(!level.getBlockState(pos.offset(24,0,0)).is(ModBlocks.LIGHT_FIELD.get()),"relay does not cross wall");
            level.removeBlock(pos.offset(20,0,0),false);
            LightNetwork.runNow(level,pos,0,0,false);
            check(!level.getBlockState(near).is(ModBlocks.LIGHT_FIELD.get()) && !level.getBlockState(far).is(ModBlocks.LIGHT_FIELD.get()),"relays cleaned");
            level.removeBlock(pos,false);
            // Sol sûr et distance de suivi minimale de la mascotte.
            p.snapTo(0,150,0,0,0);
            for(int x=-10;x<=10;x++)for(int z=-10;z<=10;z++)level.setBlock(new BlockPos(x,149,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
            var guide=new com.mmorpg.entity.MascotEntity(com.mmorpg.registry.ModEntities.MASCOT.get(),level);
            var ground=MascotGuide.safeGround(p,guide,MascotGuide.target(p,0));
            check(ground!=null && ground.y==150 && ground.subtract(p.position()).horizontalDistanceSqr()>=9,"mascot grounded and at least three blocks away");
            check(!guide.isNoGravity() && guide.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED)>0,"mascot walks with gravity");
            DevQuestTalkChecks.run(p);
            MMORPG.LOGGER.info("[CODEX CHECKS] LIGHT PASS: niveaux 0-15, emission, clamp, refus non-op, sans collision, portee etendue, persistence montures, mascotte au sol");
        } catch (Throwable t) {
            MMORPG.LOGGER.error("[CODEX CHECKS] LIGHT FAIL", t);
        } finally {
            server.halt(false);
        }
    }
}
