package com.mmorpg.dungeon;
import com.mmorpg.MMORPG;
import com.mmorpg.entity.boss.RpgBoss;
import com.mmorpg.entity.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import net.minecraft.world.level.block.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import java.util.*;

/** Changes only generated arena floors. A persisted journal restores cells after death or restart. */
@EventBusSubscriber(modid=MMORPG.MODID)
public final class BossTerrain {
    public static final class Cell {public String dim;public int x,y,z;public String before,after;}
    public static final class Journal {public Map<String,List<Cell>> battles=new HashMap<>();}
    private static final Map<UUID,Pending> PENDING=new HashMap<>();
    private record Pending(ServerLevel level,BlockPos center,long at,int phase,int kind){}
    private static String state(net.minecraft.world.level.block.state.BlockState s){return net.minecraft.world.level.block.state.BlockState.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE,s).getOrThrow().toString();}
    public static void phase(ServerLevel level,RpgBoss boss,int phase){
        if(phase<2)return;var slot=InstanceManager.slotAt(level.getServer(),level,boss.getX(),boss.getZ());
        boolean raid=boss instanceof com.mmorpg.competition.GuildGuardian;
        if(slot==null&&!raid)return;
        BlockPos center=slot!=null?new BlockPos(slot.x,slot.y-1,slot.z+Layout.ARENA.z()):boss.blockPosition().atY(99);
        int kind=raid?((com.mmorpg.competition.GuildGuardian)boss).tier%3:Math.floorMod(boss.mobKey().hashCode(),3);
        PENDING.put(boss.getUUID(),new Pending(level,center,level.getGameTime()+50,phase,kind));
        for(var p:level.players())if(p.distanceToSqr(boss)<64*64)p.sendOverlayMessage(net.minecraft.network.chat.Component.literal(new String[]{"⚠ Le pont va céder : rejoignez les plateformes !","⚠ La salle s’inonde : gagnez les îlots !","⚠ Le sol s’effondre : quittez les cercles !"}[kind]).withColor(0xFF9050));
        for(int i=0;i<8;i++){double a=i*Math.PI/4;var pos=net.minecraft.world.phys.Vec3.atBottomCenterOf(center).add(Math.cos(a)*9,1.05,Math.sin(a)*9);level.addFreshEntity(SkillFxEntity.create(level,FxKind.CIRCLE,pos,2.5,0xFF7040,50));}
    }
    private static void change(Pending p,UUID id){
        List<Cell> changed=new ArrayList<>();var level=p.level();var center=p.center();
        for(int x=-13;x<=13;x++)for(int z=-13;z<=13;z++){
            double r=Math.hypot(x,z);if(r<5||r>13||Math.abs(x)<2||Math.abs(z)<2)continue; // safe center and four escape lanes
            boolean cut=p.kind()==0?Math.abs(x)>5&&Math.abs(z)<7:p.kind()==1?r>7:Math.floorMod(x/3+z/3,3)==p.phase()-2;
            if(!cut)continue;for(int depth=0;depth<(p.kind()==1?1:4);depth++){var at=center.offset(x,-depth,z);var before=level.getBlockState(at);
            if(before.isAir()||before.hasBlockEntity()||!before.getFluidState().isEmpty())continue;
            Block after=p.kind()==1?Blocks.WATER:Blocks.AIR;
            var c=new Cell();c.dim=level.dimension().identifier().toString();c.x=at.getX();c.y=at.getY();c.z=at.getZ();c.before=state(before);c.after=state(after.defaultBlockState());changed.add(c);}
        }
        // Persist intent before modifying the world; an interrupted application remains recoverable.
        com.mmorpg.network.SharedLedger.access(level.getServer(),"boss-terrain",Journal.class,Journal::new,j->{j.battles.computeIfAbsent(id.toString(),k->new ArrayList<>()).addAll(changed);return null;});
        for(var c:changed)level.setBlock(new BlockPos(c.x,c.y,c.z),net.minecraft.world.level.block.state.BlockState.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,com.google.gson.JsonParser.parseString(c.after)).getOrThrow(),2);
    }
    public static void restore(net.minecraft.server.MinecraftServer server,String id){
        com.mmorpg.network.SharedLedger.access(server,"boss-terrain",Journal.class,Journal::new,j->{var cells=j.battles.get(id);if(cells==null)return null;
            for(int i=cells.size()-1;i>=0;i--){var c=cells.get(i);var level=server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.Identifier.parse(c.dim)));if(level==null)continue;
                var at=new BlockPos(c.x,c.y,c.z);if(state(level.getBlockState(at)).equals(c.after))level.setBlock(at,net.minecraft.world.level.block.state.BlockState.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE,com.google.gson.JsonParser.parseString(c.before)).getOrThrow(),2);
            }j.battles.remove(id);return null;});
    }
    @SubscribeEvent public static void start(net.neoforged.neoforge.event.server.ServerStartedEvent e){
        if(!com.mmorpg.network.NetworkConfig.hosts(com.mmorpg.network.NetworkConfig.Role.DUNGEONS))return;
        var ids=com.mmorpg.network.SharedLedger.access(e.getServer(),"boss-terrain",Journal.class,Journal::new,j->new ArrayList<>(j.battles.keySet()));for(var id:ids)restore(e.getServer(),id);
    }
    @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post e){
        if(e.getServer().getTickCount()%10!=0)return;
        for(var id:new ArrayList<>(PENDING.keySet())){var p=PENDING.get(id);var boss=p.level().getEntity(id);if(boss==null||!boss.isAlive()){PENDING.remove(id);continue;}if(p.level().getGameTime()>=p.at()){change(p,id);PENDING.remove(id);}}
        if(e.getServer().getTickCount()%100==0&&com.mmorpg.network.NetworkConfig.hosts(com.mmorpg.network.NetworkConfig.Role.DUNGEONS)){
            var ids=com.mmorpg.network.SharedLedger.access(e.getServer(),"boss-terrain",Journal.class,Journal::new,j->new ArrayList<>(j.battles.keySet()));
            for(var id:ids){boolean alive=false;for(var level:e.getServer().getAllLevels()){var boss=level.getEntity(UUID.fromString(id));if(boss!=null&&boss.isAlive()){alive=true;break;}}if(!alive)restore(e.getServer(),id);}
        }
    }
    @SubscribeEvent public static void stop(net.neoforged.neoforge.event.server.ServerStoppedEvent e){PENDING.clear();}
}
