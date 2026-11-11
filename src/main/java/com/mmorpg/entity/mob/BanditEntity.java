package com.mmorpg.entity.mob;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Bandit arbaletrier : tireur qui garde ses distances, lache des volees de carreaux et recule d'un bond quand on l'approche. */
public final class BanditEntity extends SmartMonster {
    public BanditEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, "bandit_arbaletrier");
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PILLAGER_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PILLAGER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PILLAGER_DEATH;
    }
}
