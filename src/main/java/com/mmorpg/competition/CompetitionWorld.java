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
        for(int x=-54;x<=54;x++)for(int z=-40;z<=40;z++){
            for(int y=100;y<=116;y++)j.add(c.getX()+x,y,c.getZ()+z,Blocks.AIR);
            j.add(c.getX()+x,98,c.getZ()+z,ground);j.add(c.getX()+x,99,c.getZ()+z,ground);
            boolean boundary=Math.abs(x)==54||Math.abs(z)==40;
            if(boundary)for(int y=100;y<108;y++)j.add(c.getX()+x,y,c.getZ()+z,Blocks.BARRIER);
            if(!boundary&&(Math.abs(x)>52||Math.abs(z)>36))wet.add(new BlockPos(c.getX()+x,99,c.getZ()+z));
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
        camps(j,c,fire);
        return j;
    }
    private static void camps(Job job,BlockPos center,boolean fire){
        var stone=fire?Blocks.POLISHED_BLACKSTONE_BRICKS:Blocks.SMOOTH_SANDSTONE;
        for(int side:new int[]{-1,1}){
            var team=side<0?net.minecraft.world.item.DyeColor.RED:net.minecraft.world.item.DyeColor.BLUE;
            for(int x=36;x<=51;x++)for(int z=-10;z<=10;z++){
                job.add(center.getX()+side*x,99,center.getZ()+z,stone);
                if(x==36||Math.abs(z)==10)job.add(center.getX()+side*x,99,center.getZ()+z,Blocks.STAINED_GLASS.pick(net.minecraft.world.item.DyeColor.RED));
                if(x==51||Math.abs(z)==10)for(int y=100;y<=102;y++)job.add(center.getX()+side*x,y,center.getZ()+z,stone);
            }
            // Fortified gate, watchtowers, canvas-like team roofs, supply stores and sheltered sleeping quarters.
            for(int z:new int[]{-6,6}){
                for(int y=100;y<=106;y++)job.add(center.getX()+side*37,y,center.getZ()+z,stone);
                job.add(center.getX()+side*37,107,center.getZ()+z,Blocks.SOUL_LANTERN);
            }
            for(int x=37;x<=49;x++)for(int z:new int[]{-8,8}){
                if(x%3==1)for(int y=100;y<=103;y++)job.add(center.getX()+side*x,y,center.getZ()+z,Blocks.DARK_OAK_LOG);
                job.add(center.getX()+side*x,104,center.getZ()+z,Blocks.CONCRETE.pick(team));
                job.add(center.getX()+side*x,104,center.getZ()+z+(z<0?1:-1),Blocks.CONCRETE.pick(team));
            }
            for(int x=46;x<=49;x+=3)for(int z:new int[]{-6,6}){
                job.add(center.getX()+side*x,100,center.getZ()+z,Blocks.BARREL);
                job.add(center.getX()+side*x,101,center.getZ()+z,Blocks.LANTERN);
            }
            for(int z=-3;z<=3;z++)job.add(center.getX()+side*49,100,center.getZ()+z,Blocks.DARK_OAK_PLANKS);
            job.add(center.getX()+side*50,103,center.getZ(),Blocks.CONCRETE.pick(team));
        }
    }
    private static int rockHeight(int x,int z,int cx,int cz,double rx,double rz,int height){double d=Math.pow((x-cx)/rx,2)+Math.pow((z-cz)/rz,2);return d>=1?0:Math.max(1,(int)(Math.sqrt(1-d)*height)+(Math.floorMod(x*31+z*17,3)-1));}
    /** Vast enclosed root cavern; the central 54-block fight area remains unobstructed. */
    public static Job raid(BlockPos c,int tier){
        Job j=new Job();
        for(int x=-44;x<=44;x++)for(int z=-44;z<=44;z++){
            double radius=Math.sqrt(x*x+z*z);if(radius>44)continue;
            int roof=128+(int)(12*Math.sqrt(Math.max(0,1-radius*radius/(44*44))))+Math.floorMod(x*7+z*13,3);
            for(int y=98;y<=143;y++){
                boolean wall=radius>=38+Math.floorMod(x*11+z*17+y/3,4);
                Block block=Blocks.AIR;
                if(y<100)block=radius<29?Blocks.DEEPSLATE_TILES:Blocks.MOSS_BLOCK;
                else if(wall||y>=roof)block=Math.floorMod(x*13+y*7+z*11,9)<3?Blocks.TUFF:Blocks.DEEPSLATE;
                j.add(c.getX()+x,y,c.getZ()+z,block);
            }
            // An engraved ring of buried light around the combat floor.
            if(radius>26&&radius<27&&Math.floorMod(x+z,4)==0)j.add(c.getX()+x,99,c.getZ()+z,Blocks.OCHRE_FROGLIGHT);
        }
        for(int a=0;a<10;a++){
            double angle=a*Math.PI/5;int height=14+(a%3)*3;
            // Broad angular roots climb the walls and disappear into the vault.
            for(int y=0;y<32;y++){
                int r=y<height?34:34-(y-height)/2;
                int x=(int)Math.round(Math.cos(angle)*r),z=(int)Math.round(Math.sin(angle)*r);
                for(int dx=-2;dx<=2;dx++)for(int dz=-2;dz<=2;dz++)if(Math.abs(dx)+Math.abs(dz)<=3)
                    j.add(c.getX()+x+dx,100+y,c.getZ()+z+dz,Math.floorMod(dx+dz+y,5)==0?Blocks.MOSS_BLOCK:Blocks.DARK_OAK_LOG);
            }
            int x=(int)(Math.cos(angle)*33),z=(int)(Math.sin(angle)*33);
            for(int h=0;h<5+a%4;h++)j.add(c.getX()+x,100+h,c.getZ()+z,Blocks.AMETHYST_BLOCK);
            j.add(c.getX()+x,105+a%4,c.getZ()+z,Blocks.SEA_LANTERN);
            // Short stepped stalactites stay well above the largest guardian.
            int sx=(int)(Math.cos(angle+.15)*29),sz=(int)(Math.sin(angle+.15)*29);
            for(int y=131;y<139;y++)for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)
                if(y>133||dx==0&&dz==0)j.add(c.getX()+sx+dx,y,c.getZ()+sz+dz,Blocks.DRIPSTONE_BLOCK);
        }
        // Recessed rune circles light the entire fight floor, including its center.
        j.add(c.getX(),99,c.getZ(),Blocks.OCHRE_FROGLIGHT);
        for(int ring=1;ring<=4;ring++)for(int a=0;a<ring*8;a++){
            double angle=2*Math.PI*a/(ring*8);int x=(int)Math.round(Math.cos(angle)*ring*8),z=(int)Math.round(Math.sin(angle)*ring*8);
            j.add(c.getX()+x,99,c.getZ()+z,ring%2==0?Blocks.OCHRE_FROGLIGHT:Blocks.SEA_LANTERN);
        }
        // Luminous mineral seams across the vault, visible from every corner.
        for(int x=-32;x<=32;x+=8)for(int z=-32;z<=32;z+=8){
            double radius=Math.sqrt(x*x+z*z);if(radius>37)continue;
            int roof=128+(int)(12*Math.sqrt(Math.max(0,1-radius*radius/(44*44))))+Math.floorMod(x*7+z*13,3);
            for(int d=-1;d<=1;d++)j.add(c.getX()+x+d,roof,c.getZ()+z,Blocks.OCHRE_FROGLIGHT);
        }
        for(int a=0;a<20;a++){double angle=a*Math.PI/10;int x=(int)(Math.cos(angle)*37),z=(int)(Math.sin(angle)*37);for(int y=106;y<=126;y+=7)j.add(c.getX()+x,y,c.getZ()+z,Blocks.SEA_LANTERN);}
        // Stone arrival dais, facing the guardian, clear of roots.
        for(int x=-4;x<=4;x++)for(int z=23;z<=29;z++)j.add(c.getX()+x,99,c.getZ()+z,Blocks.CHISELED_DEEPSLATE);
        return j;
    }
}
