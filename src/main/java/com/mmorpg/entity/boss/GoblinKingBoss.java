package com.mmorpg.entity.boss;

import com.mmorpg.entity.MobAbilities;
import com.mmorpg.entity.boss.anim.RoiGobelinAnims;
import com.mmorpg.entity.mob.GoblinEntity;
import com.mmorpg.registry.ModEntities;
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
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Roi Gobelin : ecrase le sol, appelle ses sujets a l'aide et entre en furie lorsqu'il est affaibli. */
public class GoblinKingBoss extends RpgBoss {
    private boolean enraged;

    public GoblinKingBoss(EntityType<? extends Monster> type, Level level) {
        super(type, level, "roi_gobelin");
    }

    @Override
    protected void populateDefaultEquipmentSlots(RandomSource random, DifficultyInstance difficulty) {
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.GOLDEN_AXE));
    }

    private static final AnimSet ANIMS = new AnimSet(RoiGobelinAnims.LENGTH_TICKS, RoiGobelinAnims.STRIKE_TICKS, RoiGobelinAnims.LOOPING,
            RoiGobelinAnims.HOLD, RoiGobelinAnims.COUP_HORIZONTAL, RoiGobelinAnims.MORT, -1);

    @Override
    protected AnimSet animSet() {
        return ANIMS;
    }

    @Override
    protected void bossTick(ServerLevel level) {
        if (!enraged && hpFraction() < ability("enrageThreshold", 0.3)) {
            enraged = true;
            setAltMode(true);
            this.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 600, 1));
            this.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 20 * 600, 1));
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER, this.getX(), this.getY() + 3, this.getZ(), 15, 1, 0.5, 1, 0);
            level.playSound(null, this.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2f, 1.3f);
        }
        if (busy()) return;
        if (ready("slamCooldown", 160) && this.distanceTo(this.getTarget()) < 6) {
            playThen(level, RoiGobelinAnims.FRAPPE_VERTICALE, () ->
                    MobAbilities.slam(this, 5, ability("slamDamage", 1.5), 0.7, ParticleTypes.CLOUD, SoundEvents.GENERIC_EXPLODE.value()));
            return;
        }
        if (ready("summonCooldown", 400)) {
            int alive = level.getEntitiesOfClass(GoblinEntity.class, this.getBoundingBox().inflate(24)).size();
            int count = (int) ability("summonCount", 3) + (phase - 1);
            if (alive < 8) {
                playThen(level, RoiGobelinAnims.CRI_RALLIEMENT, () -> {
                    MobAbilities.summon(this, ModEntities.GOBLIN.get(), count, "gobelin", -8);
                    level.playSound(null, this.blockPosition(), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0).value(), SoundSource.HOSTILE, 2f, 1.2f);
                });
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PIGLIN_BRUTE_ANGRY;
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
        return 1.2F;
    }
}
