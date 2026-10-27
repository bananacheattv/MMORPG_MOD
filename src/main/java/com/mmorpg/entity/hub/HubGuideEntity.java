package com.mmorpg.entity.hub;

import com.mmorpg.entity.NpcEntity;
import net.minecraft.network.syncher.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;

/** Persistent authored tutorial actors, with no shop or travel-menu behavior. */
public final class HubGuideEntity extends NpcEntity {
    private static final EntityDataAccessor<Integer> GUIDE=SynchedEntityData.defineId(HubGuideEntity.class,EntityDataSerializers.INT);
    public HubGuideEntity(EntityType<? extends PathfinderMob> type,Level level){super(type,level);setNoGravity(true);}
    @Override protected void defineSynchedData(SynchedEntityData.Builder builder){super.defineSynchedData(builder);builder.define(GUIDE,0);}
    public int guide(){return Math.clamp(entityData.get(GUIDE),0,4);}
    public void guide(int id){entityData.set(GUIDE,Math.clamp(id,0,4));setup(Role.QUETES,com.mmorpg.hub.HubGuides.NAMES[guide()],0,"");}
    @Override protected InteractionResult mobInteract(Player player,InteractionHand hand){if(player instanceof net.minecraft.server.level.ServerPlayer p&&hand==InteractionHand.MAIN_HAND)com.mmorpg.hub.HubGuides.open(p,this);return InteractionResult.SUCCESS;}
    @Override protected void addAdditionalSaveData(ValueOutput out){super.addAdditionalSaveData(out);out.putInt("AeloriaGuide",guide());}
    @Override protected void readAdditionalSaveData(ValueInput in){super.readAdditionalSaveData(in);guide(in.getIntOr("AeloriaGuide",0));}
}
