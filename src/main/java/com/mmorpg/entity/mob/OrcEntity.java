package com.mmorpg.entity.mob;

import com.mmorpg.combat.RpgCombat;
import com.mmorpg.entity.RpgMonster;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Orc Guerrier : brute massive qui assene des coups ecrasants et galvanise ses congeneres. */
public class OrcEntity extends RpgMonster {
    private int smashCooldown = 100;
    private int roarCooldown = 200;

    public OrcEntity(EntityType<? extends Monster> type, Level level) {
        super(type, level, "orc_guerrier");
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(random.nextBoolean() ? Items.IRON_AXE : Items.IRON_SWORD));
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        LivingEntity target = this.getTarget();
        if (target == null) return;
        if (--smashCooldown <= 0 && this.distanceTo(target) < 3.5) {
            smashCooldown = 140;
            this.swing(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
            RpgCombat.mobHit(this, target, 1.7);
            target.push(0, 0.5, 0);
            target.needsSync = true;
            level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + 1, target.getZ(), 20, 0.3, 0.3, 0.3, 0.3);
            level.playSound(null, this.blockPosition(), SoundEvents.ANVIL_LAND, SoundSource.HOSTILE, 0.6f, 0.6f);
        }
        if (--roarCooldown <= 0 && this.distanceTo(target) < 12) {
            roarCooldown = 400;
            level.playSound(null, this.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1f, 1.2f);
            for (OrcEntity orc : level.getEntitiesOfClass(OrcEntity.class, this.getBoundingBox().inflate(10))) {
                orc.addEffect(new MobEffectInstance(MobEffects.SPEED, 120, 1));
                orc.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 120, 0));
            }
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, this.getX(), this.getY() + 2.2, this.getZ(), 6, 0.5, 0.3, 0.5, 0);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PIGLIN_BRUTE_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PIGLIN_BRUTE_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PIGLIN_BRUTE_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.7F;
    }
}
