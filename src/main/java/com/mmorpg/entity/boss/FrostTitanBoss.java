package com.mmorpg.entity.boss;

import com.mmorpg.combat.RpgCombat;
import com.mmorpg.entity.MagicProjectile;
import com.mmorpg.entity.MobAbilities;
import com.mmorpg.entity.boss.anim.TitanGlaceAnims;
import com.mmorpg.item.ItemDefs;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;

/** Titan de Glace : colosse dont les frappes gelent le sol, projetant des eclats de glace tout autour de lui. */
public class FrostTitanBoss extends RpgBoss {
    private static final AnimSet ANIMS = new AnimSet(TitanGlaceAnims.LENGTH_TICKS, TitanGlaceAnims.STRIKE_TICKS, TitanGlaceAnims.LOOPING,
            TitanGlaceAnims.HOLD, TitanGlaceAnims.FRAPPE_POING, TitanGlaceAnims.MORT, TitanGlaceAnims.REVEIL);

    public FrostTitanBoss(EntityType<? extends Monster> type, Level level) {
        super(type, level, "titan_glace");
    }

    @Override
    protected AnimSet animSet() {
        return ANIMS;
    }

    @Override
    protected void bossTick(ServerLevel level) {
        if (phase >= 2 && this.tickCount % 20 == 0) {
            double radius = ability("auraRadius", 7);
            for (LivingEntity e : MobAbilities.enemiesAround(this, this.position(), radius)) {
                e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 0));
                if (phase >= 3) RpgCombat.mobHit(this, e, 0.15);
            }
        }
        if (busy()) return;
        LivingEntity target = this.getTarget();
        if (ready("slamCooldown", 140) && this.distanceTo(target) < 8) {
            playThen(level, TitanGlaceAnims.DOUBLE_FRAPPE_SOL, () -> {
                MobAbilities.slam(this, 7, ability("slamDamage", 1.6), 0.8, ParticleTypes.SNOWFLAKE, SoundEvents.GLASS_BREAK);
                for (LivingEntity e : MobAbilities.enemiesAround(this, this.position(), 7)) {
                    e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 2));
                }
            });
            return;
        }
        if (ready("shardCooldown", 200)) {
            playThen(level, TitanGlaceAnims.PIETINEMENT, () -> {
                int count = (int) ability("shardCount", 10) + phase * 2;
                for (int i = 0; i < count; i++) {
                    double a = Math.PI * 2 * i / count;
                    MagicProjectile p = MagicProjectile.create(level, this, ItemDefs.Element.GIVRE, RpgCombat.mobAtk(this) * 0.7, 0);
                    p.setPos(this.getX(), this.getY() + 2.5, this.getZ());
                    p.shoot(Math.cos(a), -0.05, Math.sin(a), 0.9F, 0F);
                    level.addFreshEntity(p);
                }
                level.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 2f, 0.5f);
            });
        }
    }

    /** A chaque changement de phase, le titan s'immobilise et expose son noyau. */
    @Override
    protected void onPhaseChange(ServerLevel level, int phase) {
        super.onPhaseChange(level, phase);
        playAnim(TitanGlaceAnims.EXPOSITION_NOYAU);
        immobile(TitanGlaceAnims.LENGTH_TICKS[TitanGlaceAnims.EXPOSITION_NOYAU]);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            for (int i = 0; i < 2; i++) {
                this.level().addParticle(ParticleTypes.SNOWFLAKE, this.getRandomX(1.2), this.getRandomY(), this.getRandomZ(1.2), 0, -0.03, 0);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.IRON_GOLEM_REPAIR;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.GLASS_HIT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GLASS_BREAK;
    }

    @Override
    public float getVoicePitch() {
        return 0.5F;
    }
}
