package com.mmorpg.entity.boss;

import com.mmorpg.combat.RpgCombat;
import com.mmorpg.entity.MobAbilities;
import com.mmorpg.entity.boss.anim.LicheAncienneAnims;
import com.mmorpg.item.ItemDefs;
import com.mmorpg.registry.ModAttachments;
import com.mmorpg.registry.ModEntities;
import com.mmorpg.rpg.MobData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Liche Ancienne : sorciere morte-vivante qui bombarde d'ombres, se teleporte et draine la vie. */
public class LichBoss extends RpgBoss {
    public LichBoss(EntityType<? extends Monster> type, Level level) {
        super(type, level, "liche_ancienne");
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(4, new MeleeAttackGoal(this, 0.9, true));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 16.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, false));
    }

    private static final AnimSet ANIMS = new AnimSet(LicheAncienneAnims.LENGTH_TICKS, LicheAncienneAnims.STRIKE_TICKS, LicheAncienneAnims.LOOPING,
            LicheAncienneAnims.HOLD, -1, LicheAncienneAnims.MORT, -1);

    @Override
    protected AnimSet animSet() {
        return ANIMS;
    }

    @Override
    protected void bossTick(ServerLevel level) {
        LivingEntity target = this.getTarget();
        if (this.distanceTo(target) < 4 && ready("blinkCooldown", 160)) {
            Vec3 away = this.position().subtract(target.position()).normalize().scale(10);
            level.sendParticles(ParticleTypes.SCULK_SOUL, this.getX(), this.getY() + 1, this.getZ(), 30, 0.4, 0.8, 0.4, 0.05);
            this.randomTeleport(this.getX() + away.x, this.getY(), this.getZ() + away.z, true, state -> false);
            playAnim(LicheAncienneAnims.RECUL);
            return;
        }
        if (busy()) return;
        if (ready("boltCooldown", 40)) {
            playThen(level, LicheAncienneAnims.PROJECTILE, () -> {
                if (!target.isAlive()) return;
                int bolts = phase >= 2 ? 3 : 1;
                for (int i = 0; i < bolts; i++) {
                    MobAbilities.bolt(this, target, ItemDefs.Element.OMBRE, ability("boltDamage", 0.9), 1.0F + i * 0.15F, 0).homing(target);
                }
                level.playSound(null, this.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.5f, 0.8f);
            });
            return;
        }
        if (ready("summonCooldown", 500)) {
            level.playSound(null, this.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 2f, 0.8f);
            playThen(level, LicheAncienneAnims.INVOCATION, () ->
                    MobAbilities.summon(this, ModEntities.CURSED_SKELETON.get(), (int) ability("summonCount", 2) + phase - 1, "squelette_maudit", -10));
            return;
        }
        if (phase >= 2 && ready("drainCooldown", 300)) {
            playFor(level, LicheAncienneAnims.CANALISATION, 40, () -> {
                double healed = 0;
                for (LivingEntity e : MobAbilities.enemiesAround(this, this.position(), 8)) {
                    RpgCombat.mobHit(this, e, 0.6);
                    healed += RpgCombat.mobAtk(this) * 0.6;
                    level.sendParticles(ParticleTypes.SCULK_SOUL, e.getX(), e.getY() + 1, e.getZ(), 15, 0.3, 0.5, 0.3, 0.05);
                }
                MobData d = this.getExistingDataOrNull(ModAttachments.MOB);
                if (d != null && healed > 0) {
                    d.hp = (float) Math.min(d.maxHp, d.hp + healed * 2);
                    this.syncData(ModAttachments.MOB);
                }
                level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_HEARTBEAT, SoundSource.HOSTILE, 2f, 1f);
            });
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.level().addParticle(ParticleTypes.SCULK_SOUL, this.getRandomX(0.7), this.getRandomY(), this.getRandomZ(0.7), 0, 0.02, 0);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.WITHER_SKELETON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.WITHER_SKELETON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.WITHER_DEATH;
    }
}
