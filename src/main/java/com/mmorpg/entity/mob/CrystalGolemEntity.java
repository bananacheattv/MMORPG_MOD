package com.mmorpg.entity.mob;

import com.mmorpg.entity.MobAbilities;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Golem de Cristal : colosse lent dont les frappes au sol ebranlent la terre. */
public class CrystalGolemEntity extends SmartMonster {
    public CrystalGolemEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, "golem_cristal");
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.random.nextInt(5) == 0) {
            this.level().addParticle(ParticleTypes.END_ROD, this.getRandomX(0.8), this.getRandomY(), this.getRandomZ(0.8), 0, 0.01, 0);
        }
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.AMETHYST_BLOCK_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.IRON_GOLEM_DEATH;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.AMETHYST_BLOCK_CHIME;
    }
}
