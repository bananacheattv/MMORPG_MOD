package com.mmorpg.entity.mob;

import com.mmorpg.entity.MobAbilities;
import com.mmorpg.entity.RpgMonster;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Chevalier du Neant : guerrier d'outre-monde qui se teleporte derriere ses adversaires. */
public class VoidKnightEntity extends RpgMonster {
    private int blinkCooldown = 160;

    public VoidKnightEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, "chevalier_neant");
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        LivingEntity target = this.getTarget();
        if (target != null && --blinkCooldown <= 0 && this.distanceTo(target) > 4 && this.distanceTo(target) < 20) {
            blinkCooldown = 200;
            MobAbilities.blinkBehind(this, target);
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide() && this.random.nextInt(3) == 0) {
            this.level().addParticle(ParticleTypes.REVERSE_PORTAL, this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6), 0, 0.02, 0);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENDERMAN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDERMAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDERMAN_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.6F;
    }
}
