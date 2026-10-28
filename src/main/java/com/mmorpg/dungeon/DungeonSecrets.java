package com.mmorpg.dungeon;
import com.mmorpg.MMORPG;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.core.*;
import net.minecraft.server.level.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.*;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import java.util.*;

/** Rotating three-dimensional runes and a second optional seal requiring distinct classes. */
@EventBusSubscriber(modid=MMORPG.MODID)
public final class DungeonSecrets {
    private static final Direction[] DIR={Direction.NORTH,Direction.EAST,Direction.SOUTH,Direction.WEST};
    private static final String[] NAMES={"Nord","Est","Sud","Ouest"};
    public static int answer(long seed,int rune){return (int)Math.floorMod((seed>>(rune*9)),4);}
    public static BlockPos rune(BlockPos o,int i){return o.offset(13,1,Layout.R2.z()-3+i*3);}
    public static void build(ServerLevel level,BlockPos o,long seed){
        for(int x=15;x<=30;x++)for(int z=Layout.R2.z()-6;z<=Layout.R2.z()+6;z++)for(int y=-1;y<=7;y++){
            boolean edge=x==30||Math.abs(z-Layout.R2.z())==6||y==-1||y==7;
            level.setBlock(o.offset(x,y,z),edge?Blocks.POLISHED_DEEPSLATE.defaultBlockState():Blocks.AIR.defaultBlockState(),2);
        }
        for(int i=0;i<3;i++){
            level.setBlock(rune(o,i).below(),Blocks.CHISELED_DEEPSLATE.defaultBlockState(),2);
            level.setBlock(rune(o,i),Blocks.END_ROD.defaultBlockState().setValue(EndRodBlock.FACING,DIR[(answer(seed,i)+1)%4]),2);
        }
        door(level,o,Blocks.CHISELED_POLISHED_BLACKSTONE);
        for(int z:new int[]{-2,2})level.setBlock(o.offset(24,-1,Layout.R2.z()+z),Blocks.CHISELED_QUARTZ_BLOCK.defaultBlockState(),2);
        for(int y=0;y<3;y++)level.setBlock(o.offset(27,y,Layout.R2.z()),Blocks.AMETHYST_BLOCK.defaultBlockState(),2);
        level.setBlock(o.offset(29,1,Layout.R2.z()),Blocks.SEA_LANTERN.defaultBlockState(),2);
    }
    private static void door(ServerLevel l,BlockPos o,Block block){for(int z=-1;z<=1;z++)for(int y=0;y<3;y++)l.setBlock(o.offset(17,y,Layout.R2.z()+z),block.defaultBlockState(),2);}
    private static void reward(ServerPlayer p,long seed,String kind){
        var d=RpgPlayers.get(p);String receipt="secret:"+seed+":"+kind;if(d.guildReceipts.getBooleanOr(receipt,false))return;
        String id=kind.equals("runes")?"aura_arcane":"trainee_etoiles";d.cosmetics.add(id);d.guildReceipts.putBoolean(receipt,true);d.dirty=true;RpgPlayers.sync(p);
        p.sendSystemMessage(Component.literal("Secret découvert : "+(kind.equals("runes")?"Aura arcanique":"Traînée d’étoiles")+" débloquée !").withColor(0xC090FF));
    }
    @SubscribeEvent public static void interact(PlayerInteractEvent.RightClickBlock e){
        if(!(e.getEntity() instanceof ServerPlayer p)||e.getHand()!=net.minecraft.world.InteractionHand.MAIN_HAND)return;
        var l=p.level();var slot=InstanceManager.slotAt(l.getServer(),l,p.getX(),p.getZ());if(slot==null||slot.owner.isEmpty())return;var o=new BlockPos(slot.x,slot.y,slot.z);
        for(int i=0;i<3;i++)if(e.getPos().equals(rune(o,i))){
            if(p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(e.getPos()))>36)return;
            var state=l.getBlockState(e.getPos());if(!state.is(Blocks.END_ROD))return;
            var dir=state.getValue(EndRodBlock.FACING);int now=Arrays.asList(DIR).indexOf(dir);l.setBlockAndUpdate(e.getPos(),state.setValue(EndRodBlock.FACING,DIR[(now+1)%4]));
            p.sendOverlayMessage(Component.literal("Gravure I–II–III : "+NAMES[answer(slot.variantSeed,0)]+" · "+NAMES[answer(slot.variantSeed,1)]+" · "+NAMES[answer(slot.variantSeed,2)]));
            boolean solved=true;for(int r=0;r<3;r++)solved&=l.getBlockState(rune(o,r)).is(Blocks.END_ROD)&&l.getBlockState(rune(o,r)).getValue(EndRodBlock.FACING)==DIR[answer(slot.variantSeed,r)];
            if(solved){door(l,o,Blocks.AIR);reward(p,slot.variantSeed,"runes");for(var ally:l.players())if(ally.distanceToSqr(p)<400)reward(ally,slot.variantSeed,"runes");}
            e.setCanceled(true);return;
        }
    }
    @SubscribeEvent public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post e){
        var server=e.getServer();if(server.getTickCount()%20!=0)return;var l=server.getLevel(InstanceManager.DIM);if(l==null)return;
        for(var slot:InstanceData.get(server).slots){if(slot.owner.isEmpty()||!slot.ready)continue;var o=new BlockPos(slot.x,slot.y,slot.z);
            if(l.getBlockState(o.offset(17,0,Layout.R2.z())).isAir()){
                var a=l.players().stream().filter(p->p.isAlive()&&!p.isSpectator()&&p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(o.offset(24,0,Layout.R2.z()-2)))<4).findFirst();
                var b=l.players().stream().filter(p->p.isAlive()&&!p.isSpectator()&&p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(o.offset(24,0,Layout.R2.z()+2)))<4).findFirst();
                if(a.isPresent()&&b.isPresent()&&RpgPlayers.get(a.get()).playerClass.isPlayable()&&RpgPlayers.get(a.get()).playerClass!=RpgPlayers.get(b.get()).playerClass){
                    for(int y=0;y<3;y++)l.setBlock(o.offset(27,y,Layout.R2.z()),Blocks.AIR.defaultBlockState(),2);
                    for(var p:l.players())if(p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(o.offset(24,0,Layout.R2.z())))<100)reward(p,slot.variantSeed,"classes");
                }
            }
        }
    }
}
