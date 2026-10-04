package com.mmorpg.entity.mob;

import com.mmorpg.entity.RpgMonster;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Loup Sombre : chasseur nocturne rapide qui bondit sur ses proies. */
public class DarkWolfEntity extends RpgMonster {
    public DarkWolfEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, "loup_sombre");
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(2, new LeapAtTargetGoal(this, 0.45F));
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.FOX_AGGRO;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.FOX_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.FOX_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.7F;
    }
}
