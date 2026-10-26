package com.mmorpg.block.structure;

import net.minecraft.core.*;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.*;
import java.util.*;

/** Rotated collision-box envelopes, split into their own cells for selection and interaction. */
public final class FreeOrientation {
    public static final BooleanProperty FREE=BooleanProperty.create("free_orientation");
    public static float towards(net.minecraft.world.phys.Vec3 from,net.minecraft.world.phys.Vec3 viewer) {
        return (float)Math.toDegrees(Math.atan2(from.x-viewer.x,viewer.z-from.z));
    }
    public static Map<BlockPos,VoxelShape> shapes(Block block,float yaw) {
        List<AABB> boxes=new ArrayList<>();var kind=MultiblockKind.of(block);
        if(kind!=null) {
            boxes.addAll(kind.masterShape(Direction.NORTH).toAabbs());
            for(int[] c:kind.cells())for(AABB box:kind.shape(c[0],c[1],c[2],Direction.NORTH).toAabbs())boxes.add(box.move(c[0],c[1],c[2]));
        } else boxes.add(new AABB(.03125,0,.03125,.96875,.84375,.96875));
        Map<BlockPos,VoxelShape> result=new LinkedHashMap<>();double angle=Math.toRadians(180-yaw),co=Math.cos(angle),si=Math.sin(angle);
        for(AABB b:boxes) {
            double minX=Double.POSITIVE_INFINITY,minZ=minX,maxX=Double.NEGATIVE_INFINITY,maxZ=maxX;
            for(double x:new double[]{b.minX,b.maxX})for(double z:new double[]{b.minZ,b.maxZ}) {
                double rx=.5+(x-.5)*co+(z-.5)*si,rz=.5-(x-.5)*si+(z-.5)*co;
                minX=Math.min(minX,rx);maxX=Math.max(maxX,rx);minZ=Math.min(minZ,rz);maxZ=Math.max(maxZ,rz);
            }
            for(int x=(int)Math.floor(minX+1e-7);x<Math.ceil(maxX-1e-7);x++)for(int y=(int)Math.floor(b.minY+1e-7);y<Math.ceil(b.maxY-1e-7);y++)for(int z=(int)Math.floor(minZ+1e-7);z<Math.ceil(maxZ-1e-7);z++) {
                var pos=new BlockPos(x,y,z);
                VoxelShape part=Shapes.box(Math.max(0,minX-x),Math.max(0,b.minY-y),Math.max(0,minZ-z),Math.min(1,maxX-x),Math.min(1,b.maxY-y),Math.min(1,maxZ-z));
                result.merge(pos,part,Shapes::or);
            }
        }
        result.replaceAll((p,s)->s.optimize());return result;
    }
}
