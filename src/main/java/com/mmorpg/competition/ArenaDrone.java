package com.mmorpg.competition;

import net.minecraft.world.entity.*;
import net.minecraft.world.level.Level;

/** Decorative one-block guardian eye. Targeting and damage are authoritative match rules, never vanilla mob AI. */
public final class ArenaDrone extends PathfinderMob {
    public ArenaDrone(EntityType<? extends PathfinderMob> type,Level level){super(type,level);setNoAi(true);setNoGravity(true);setPersistenceRequired();}
    @Override protected void registerGoals(){}
    @Override public boolean isPushable(){return false;}
    @Override public boolean hurtServer(net.minecraft.server.level.ServerLevel level,net.minecraft.world.damagesource.DamageSource source,float amount){return false;}
    @Override public boolean removeWhenFarAway(double distance){return false;}
}
