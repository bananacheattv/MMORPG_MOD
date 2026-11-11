package com.mmorpg.entity.mob;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Zombie des cryptes : mort-vivant lent et tenace, griffe, mord et crache un nuage putride. */
public final class CryptZombieEntity extends SmartMonster {
    public CryptZombieEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, "zombie_des_cryptes");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ZOMBIE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ZOMBIE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ZOMBIE_DEATH;
    }
}
