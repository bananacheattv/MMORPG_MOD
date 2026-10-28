package com.mmorpg.dungeon;

import com.mmorpg.network.SharedLedger;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import java.util.*;

/** The advertised expedition is shared across backends and survives reconnects. */
public final class DungeonVariants {
    public static final class Plans {public Map<String,Long> seeds=new HashMap<>();}
    public static long preview(MinecraftServer server,String pool,String owner){
        return SharedLedger.access(server,"expedition-plans",Plans.class,Plans::new,s->s.seeds.computeIfAbsent(pool+"/"+owner,k->new Random().nextLong(1,Long.MAX_VALUE)));
    }
    public static Map<String,Long> choices(MinecraftServer server,String owner){
        return SharedLedger.access(server,"expedition-plans",Plans.class,Plans::new,s->{var out=new LinkedHashMap<String,Long>();for(var type:com.mmorpg.world.DungeonBuilder.DungeonType.values())for(int difficulty=0;difficulty<3;difficulty++){String pool=InstanceManager.pool(type,difficulty);out.put(pool,s.seeds.computeIfAbsent(pool+"/"+owner,k->new Random().nextLong(1,Long.MAX_VALUE)));}return out;});
    }
    public static void finish(MinecraftServer server,String pool,String owner){if(!owner.isEmpty())SharedLedger.access(server,"expedition-plans",Plans.class,Plans::new,s->s.seeds.remove(pool+"/"+owner));}
    public static boolean active(ServerLevel level){return level.dimension().equals(InstanceManager.DIM);}
    public static int route(long seed){return (int)Math.floorMod(seed,3);}
    public static int modifier(long seed){return (int)Math.floorMod(seed/3,3);}
    public static String description(long seed){return new String[]{"Galerie ouest","Galerie est","Galerie sinueuse"}[route(seed)]+" · "+new String[]{"Renforts : +20 % de vie","Brume : ralentissements brefs","Surcharge : éclairs annoncés"}[modifier(seed)]+" · Secrets runiques";}
    public static long seedAt(ServerLevel level,BlockPos origin){var s=InstanceManager.slotAt(level.getServer(),level,origin.getX(),origin.getZ()+12);return s==null?Math.max(1,Math.abs(origin.asLong())):s.variantSeed;}
    public static void decorate(ServerLevel level,BlockPos origin,long seed){
        // Alternate passage through the same bounded gallery. No gate, pit or authored room is bypassed.
        for(int z=Layout.C2.z0()+4;z<=Layout.C2.z1()-4;z++)for(int x=-3;x<=3;x++)for(int y=1;y<=3;y++){
            int clear=route(seed)==0?-2:route(seed)==1?2:((z/4)%2==0?-2:2);
            var at=origin.offset(x,y-1,z);
            level.setBlock(at,Blocks.AIR.defaultBlockState(),2);
        }
        // Ambush positions and difficulty change for every fresh expedition, including reused geometry.
        var slot=InstanceManager.slotAt(level.getServer(),level,origin.getX(),origin.getZ()+12);if(slot==null)return;
        var data=TrialData.get(level.getServer());String group=InstanceManager.typeOf(slot.pool).id+"_"+origin.getX()+"_"+origin.getZ();
        int index=0;for(var trial:new ArrayList<>(data.inGroup(group)))if(trial.kind().equals(TrialData.AMBUSH)){
            boolean enabled=Math.floorMod(seed+index++,4)!=0;
            data.replaceVariant(trial,enabled,(int)Math.floorMod(seed+index,3));
        }
        DungeonSecrets.build(level,origin,seed);
    }
    public static void scaleMob(net.minecraft.world.entity.Mob mob,ServerLevel level){
        var slot=InstanceManager.slotAt(level.getServer(),level,mob.getX(),mob.getZ());if(slot==null||modifier(slot.variantSeed)!=0)return;
        var d=mob.getExistingDataOrNull(com.mmorpg.registry.ModAttachments.MOB);if(d!=null&&d.initialized()&&!mob.getPersistentData().getBooleanOr("variant_scaled",false)){
            d.maxHp*=1.2f;d.hp=d.maxHp;mob.setData(com.mmorpg.registry.ModAttachments.MOB,d);mob.getPersistentData().putBoolean("variant_scaled",true);
        }
    }
}
