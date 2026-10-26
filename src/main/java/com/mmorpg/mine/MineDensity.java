package com.mmorpg.mine;

import com.mmorpg.MMORPG;
import com.mmorpg.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import java.util.*;

/** Keep a stable fifth of deposits; migrate existing chunks once, one per tick. */
@EventBusSubscriber(modid=MMORPG.MODID)
public final class MineDensity {
    private static final java.util.Queue<LevelChunk> PENDING=new java.util.concurrent.ConcurrentLinkedQueue<>();
    public static boolean keep(BlockPos pos) {
        long hash=pos.asLong();hash=(hash^(hash>>>30))*0xbf58476d1ce4e5b9L;hash=(hash^(hash>>>27))*0x94d049bb133111ebL;hash^=hash>>>31;
        return Long.remainderUnsigned(hash,5)==0;
    }
    @SubscribeEvent public static void loaded(ChunkEvent.Load event) {
        if(event.getChunk() instanceof LevelChunk chunk && chunk.getLevel() instanceof ServerLevel level
                && level.dimension().equals(com.mmorpg.dungeon.InstanceManager.DIM)
                && chunk.getData(ModAttachments.MINE_DENSITY.get())<1) {
            var pos=chunk.getPos();
            if(pos.getMinBlockZ()<=MineLayout.Z0+MineLayout.SIZE+16&&pos.getMaxBlockZ()>=MineLayout.Z0-16
                    &&pos.getMinBlockX()>=MineLayout.X0-16&&pos.getMinBlockX()<=MineLayout.X0+MineLayout.STEP*MineLayout.SECTORS)
                PENDING.add(chunk);
        }
    }
    public static int thin(LevelChunk chunk) {
        if(chunk.getData(ModAttachments.MINE_DENSITY.get())>=1)return 0;
        int removed=0;ServerLevel level=(ServerLevel)chunk.getLevel();var cp=chunk.getPos();
        for(int y=chunk.getMinY();y<chunk.getMaxY();y++)for(int x=0;x<16;x++)for(int z=0;z<16;z++) {
            BlockPos at=new BlockPos(cp.getMinBlockX()+x,y,cp.getMinBlockZ()+z);var state=chunk.getBlockState(at);
            if((state.getBlock() instanceof MineOreBlock||state.is(MineBlocks.EPUISE.get()))&&!keep(at)) {level.removeBlock(at,false);removed++;}
        }
        chunk.setData(ModAttachments.MINE_DENSITY.get(),1);return removed;
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post event) {
        var chunk=PENDING.poll();if(chunk==null)return;
        if(chunk.getLevel() instanceof ServerLevel level&&level.hasChunk(chunk.getPos().x(),chunk.getPos().z()))thin(chunk);
    }
    @SubscribeEvent public static void stopped(ServerStoppedEvent event) { PENDING.clear(); }
}
