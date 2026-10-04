package com.mmorpg.entity.mob;

import com.mmorpg.entity.MobAbilities;
import com.mmorpg.entity.RpgMonster;
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
public class CrystalGolemEntity extends RpgMonster {
    private int slamCooldown = 120;
    public int attackAnim;

    public CrystalGolemEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, "golem_cristal");
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        this.attackAnim = 10;
        level.broadcastEntityEvent(this, (byte) 4);
        return super.doHurtTarget(level, target);
    }

    @Override
    public void handleEntityEvent(byte id) {
        if (id == 4) {
            this.attackAnim = 10;
        } else {
            super.handleEntityEvent(id);
        }
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (this.attackAnim > 0) this.attackAnim--;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        LivingEntity target = this.getTarget();
        if (target != null && --slamCooldown <= 0 && this.distanceTo(target) < 5) {
            slamCooldown = 160;
            this.attackAnim = 10;
            level.broadcastEntityEvent(this, (byte) 4);
            MobAbilities.slam(this, 5, 1.4, 0.6, new DustParticleOptions(0xD090FF, 1.5F), SoundEvents.AMETHYST_BLOCK_BREAK);
        }
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
