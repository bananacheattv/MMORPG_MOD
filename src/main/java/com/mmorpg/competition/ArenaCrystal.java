package com.mmorpg.competition;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.network.syncher.*;
import net.minecraft.server.level.*;
import java.util.UUID;

public final class ArenaCrystal extends Monster {
    private static final EntityDataAccessor<Integer> TEAM=SynchedEntityData.defineId(ArenaCrystal.class,EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> HITS=SynchedEntityData.defineId(ArenaCrystal.class,EntityDataSerializers.INT);
    public UUID match;
    public int hits(){return entityData.get(HITS);}
    public void setHits(int hits){entityData.set(HITS,hits);setHealth(getMaxHealth()*Math.max(.02f,1-hits/50f));}
    public ArenaCrystal(EntityType<? extends Monster> type,Level level){super(type,level);setNoAi(true);setNoGravity(true);setPersistenceRequired();}
    @Override protected void defineSynchedData(SynchedEntityData.Builder b){super.defineSynchedData(b);b.define(TEAM,-1);b.define(HITS,0);}
    public int team(){return entityData.get(TEAM);}public void setTeam(int team){entityData.set(TEAM,team);}
    @Override public boolean hurtServer(ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float amount){return source.getEntity() instanceof ServerPlayer p&&ArenaManager.capture(this,p);}
    @Override public boolean isPushable(){return false;}
    @Override protected void registerGoals(){}
}
