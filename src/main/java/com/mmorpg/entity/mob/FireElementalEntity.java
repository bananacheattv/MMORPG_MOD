package com.mmorpg.entity.mob;

import com.mmorpg.item.ItemDefs;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Elementaire de Feu : incarnation des flammes qui projette des boules de feu explosives. */
public class FireElementalEntity extends CasterMonster {
    public FireElementalEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, "elementaire_feu", ItemDefs.Element.FEU, SoundEvents.BLAZE_SHOOT);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            if (this.random.nextInt(2) == 0) {
                this.level().addParticle(ParticleTypes.FLAME, this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6), 0, 0.03, 0);
            }
            if (this.random.nextInt(6) == 0) {
                this.level().addParticle(ParticleTypes.LARGE_SMOKE, this.getRandomX(0.5), this.getY() + 2.0, this.getRandomZ(0.5), 0, 0.02, 0);
            }
        }
    }

    @Override
    public boolean isSensitiveToWater() {
        return true;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.BLAZE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.BLAZE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.BLAZE_DEATH;
    }
}
