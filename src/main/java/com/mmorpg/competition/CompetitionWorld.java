package com.mmorpg.competition;

import com.mmorpg.MMORPG;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import java.util.*;

/** Bounded construction jobs on the dungeon backend; no world edits on the player's spawn. */
public final class CompetitionWorld {
    public static final ResourceKey<Level> DIM=ResourceKey.create(Registries.DIMENSION,MMORPG.id("competitions"));
    public record Placement(BlockPos pos,net.minecraft.world.level.block.state.BlockState state){}
    public static final class Job {public final List<Placement> blocks=new ArrayList<>();public int next;public boolean done(){return next>=blocks.size();}public void add(int x,int y,int z,Block block){blocks.add(new Placement(new BlockPos(x,y,z),block.defaultBlockState()));}public void tick(ServerLevel level){long until=System.nanoTime()+6_000_000L;int count=0;while(!done()&&count++<1800&&System.nanoTime()<until){var b=blocks.get(next++);level.setBlock(b.pos,b.state,2);}}}
    public static ServerLevel level(MinecraftServer server){var level=server.getLevel(DIM);if(level==null)throw new IllegalStateException("Dimension des compétitions indisponible.");return level;}
    public static void teleport(ServerPlayer p,BlockPos pos,float yaw){p.teleportTo(level(p.level().getServer()),pos.getX()+.5,pos.getY(),pos.getZ()+.5,Set.of(),yaw,0,true);p.resetFallDistance();}
    public static BlockPos center(int pool){return new BlockPos(500+pool*240,100,0);}
    public static Job arena(BlockPos c,ArenaRules.Kind kind){
        Job j=new Job();List<BlockPos> wet=new ArrayList<>();boolean fire=kind==ArenaRules.Kind.FEU;var ground=fire?Blocks.POLISHED_BLACKSTONE:Blocks.SMOOTH_SANDSTONE;var rock=fire?Blocks.BASALT:Blocks.CUT_SANDSTONE;
        for(int x=-47;x<=47;x++)for(int z=-35;z<=35;z++){
            for(int y=100;y<=112;y++)j.add(c.getX()+x,y,c.getZ()+z,Blocks.AIR);
            j.add(c.getX()+x,98,c.getZ()+z,ground);j.add(c.getX()+x,99,c.getZ()+z,ground);
            boolean boundary=Math.abs(x)==47||Math.abs(z)==35;
            if(boundary)for(int y=100;y<108;y++)j.add(c.getX()+x,y,c.getZ()+z,Blocks.BARRIER);
            if(!boundary&&(Math.abs(x)>43||Math.abs(z)>31))wet.add(new BlockPos(c.getX()+x,99,c.getZ()+z));
            int h=rockHeight(x,z,0,0,7,6,9);
            for(int side:new int[]{-1,1})h=Math.max(h,rockHeight(x,z,0,side*22,8,5,7));
            for(int side:new int[]{-1,1})for(int end:new int[]{-1,1})h=Math.max(h,rockHeight(x,z,side*24,end*18,4,4,5));
            for(int y=100;y<100+h;y++)j.add(c.getX()+x,y,c.getZ()+z,fire&&Math.floorMod(x*13+z*7+y,11)==0?Blocks.BLACKSTONE:rock);
            if(h>0&&Math.floorMod(x*19+z*11,31)==0)j.add(c.getX()+x,99+h,c.getZ()+z,fire?Blocks.MAGMA_BLOCK:Blocks.SEA_LANTERN);
            if(Math.abs(x)<40&&Math.abs(z)<28&&Math.floorMod(x*3+z*5,83)==0)j.add(c.getX()+x,99,c.getZ()+z,fire?Blocks.SHROOMLIGHT:Blocks.SEA_LANTERN);
            if((Math.abs(x)==29&&Math.abs(z)<5)){j.add(c.getX()+x,100,c.getZ()+z,x<0?Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.RED):Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLUE));}
        }
        for(int x:new int[]{-37,37})for(int z:new int[]{-26,26})for(int y=100;y<=111;y++)j.add(c.getX()+x,y,c.getZ()+z,y==111?(fire?Blocks.SHROOMLIGHT:Blocks.SEA_LANTERN):rock);
        for(int x:new int[]{-18,18})for(int z:new int[]{-15,15}){for(int y=100;y<104;y++)j.add(c.getX()+x,y,c.getZ()+z,rock);j.add(c.getX()+x,104,c.getZ()+z,fire?Blocks.SOUL_LANTERN:Blocks.SEA_LANTERN);}
        for(var pos:wet)j.add(pos.getX(),pos.getY(),pos.getZ(),fire?Blocks.LAVA:Blocks.WATER);
        return j;
    }
    private static int rockHeight(int x,int z,int cx,int cz,double rx,double rz,int height){double d=Math.pow((x-cx)/rx,2)+Math.pow((z-cz)/rz,2);return d>=1?0:Math.max(1,(int)(Math.sqrt(1-d)*height)+(Math.floorMod(x*31+z*17,3)-1));}
    public static Job raid(BlockPos c,int tier){Job j=new Job();for(int x=-38;x<=38;x++)for(int z=-38;z<=38;z++)if(x*x+z*z<=38*38){j.add(c.getX()+x,99,c.getZ()+z,(x*x+z*z>35*35)?Blocks.MOSS_BLOCK:Blocks.DEEPSLATE_TILES);if(x*x+z*z>37*37)for(int y=100;y<115;y++)j.add(c.getX()+x,y,c.getZ()+z,Blocks.BARRIER);}
        // Giant roots and ten luminous branches surrounding the trial ring.
        for(int a=0;a<10;a++){double angle=a*Math.PI/5;for(int r=31;r<=36;r++)for(int y=100;y<107+(a%3)*3;y++)for(int w=-1;w<=1;w++)j.add(c.getX()+(int)(Math.cos(angle)*r)+w,y,c.getZ()+(int)(Math.sin(angle)*r),Blocks.DARK_OAK_LOG);j.add(c.getX()+(int)(Math.cos(angle)*32),112,c.getZ()+(int)(Math.sin(angle)*32),Blocks.OCHRE_FROGLIGHT);}return j;}
}
