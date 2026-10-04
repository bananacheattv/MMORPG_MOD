package com.mmorpg.entity.mob;

import com.mmorpg.item.ItemDefs;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Spectre de Givre : apparition glaciale qui ralentit ses victimes avec des eclats de glace. */
public class IceWraithEntity extends CasterMonster {
    public IceWraithEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, "spectre_givre", ItemDefs.Element.GIVRE, SoundEvents.PLAYER_HURT_FREEZE);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.random.nextInt(2) == 0) {
            this.level().addParticle(ParticleTypes.SNOWFLAKE, this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6), 0, -0.02, 0);
        }
    }

    @Override
    public boolean canFreeze() {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.STRAY_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.STRAY_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.STRAY_DEATH;
    }
}
