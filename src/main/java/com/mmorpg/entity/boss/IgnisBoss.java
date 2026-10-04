package com.mmorpg.entity.boss;

import com.mmorpg.combat.RpgCombat;
import com.mmorpg.entity.MagicProjectile;
import com.mmorpg.entity.MobAbilities;
import com.mmorpg.entity.boss.anim.IgnisAnims;
import com.mmorpg.item.ItemDefs;
import com.mmorpg.server.Scheduler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Seigneur Demon Ignis : deverse des salves de feu, un anneau de flammes et fait pleuvoir des meteores. */
public class IgnisBoss extends RpgBoss {
    public IgnisBoss(EntityType<? extends Monster> type, Level level) {
        super(type, level, "seigneur_ignis");
    }

    private static final AnimSet ANIMS = new AnimSet(IgnisAnims.LENGTH_TICKS, IgnisAnims.STRIKE_TICKS, IgnisAnims.LOOPING,
            IgnisAnims.HOLD, IgnisAnims.BALAYAGE, IgnisAnims.MORT, -1);

    @Override
    protected AnimSet animSet() {
        return ANIMS;
    }

    @Override
    protected void bossTick(ServerLevel level) {
        if (busy()) return;
        LivingEntity target = this.getTarget();
        if (ready("fireballCooldown", 120)) {
            playThen(level, IgnisAnims.SOUFFLE_FEU, () -> {
                if (!target.isAlive()) return;
                int count = (int) ability("fireballCount", 5) + phase;
                Vec3 dir = target.position().subtract(this.position()).normalize();
                double baseAngle = Math.atan2(dir.z, dir.x);
                for (int i = 0; i < count; i++) {
                    double a = baseAngle + (i - (count - 1) / 2.0) * 0.18;
                    MagicProjectile p = MagicProjectile.create(level, this, ItemDefs.Element.FEU, RpgCombat.mobAtk(this) * 0.8, 2.0);
                    p.setPos(this.getX(), this.getEyeY(), this.getZ());
                    p.shoot(Math.cos(a), (target.getY() - this.getEyeY()) / Math.max(1, this.distanceTo(target)), Math.sin(a), 1.1F, 1F);
                    level.addFreshEntity(p);
                }
                level.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2f, 0.6f);
            });
            return;
        }
        if (ready("ringCooldown", 240)) {
            playThen(level, IgnisAnims.FRAPPE_SOL, () -> {
                for (int r = 1; r <= 7; r++) {
                    final int radius = r;
                    Scheduler.later(level, r * 3, () -> {
                        for (int i = 0; i < 24; i++) {
                            double a = Math.PI * 2 * i / 24;
                            level.sendParticles(ParticleTypes.FLAME, this.getX() + Math.cos(a) * radius, this.getY() + 0.2, this.getZ() + Math.sin(a) * radius, 2, 0.1, 0.1, 0.1, 0.02);
                        }
                    });
                }
                for (LivingEntity e : MobAbilities.enemiesAround(this, this.position(), 7)) {
                    RpgCombat.mobHit(this, e, 1.1);
                    e.igniteForSeconds(4);
                }
                level.playSound(null, this.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 2f, 0.5f);
            });
            return;
        }
        if (phase >= 2 && ready("meteorCooldown", 300)) {
            playThen(level, IgnisAnims.RUGISSEMENT, () -> {
                for (LivingEntity e : MobAbilities.enemiesAround(this, this.position(), 24)) {
                    Vec3 impact = e.position();
                    for (int t = 0; t < 20; t += 2) {
                        final int tt = t;
                        Scheduler.later(level, t, () -> level.sendParticles(ParticleTypes.LAVA, impact.x, impact.y + 12 - tt * 0.6, impact.z, 3, 0.2, 0.2, 0.2, 0));
                    }
                    Scheduler.later(level, 20, () -> {
                        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, impact.x, impact.y, impact.z, 1, 0, 0, 0, 0);
                        level.playSound(null, impact.x, impact.y, impact.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2f, 0.8f);
                        for (LivingEntity v : MobAbilities.enemiesAround(this, impact, 3.5)) {
                            RpgCombat.mobHit(this, v, 1.8);
                            v.igniteForSeconds(3);
                        }
                    });
                }
            });
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            this.level().addParticle(ParticleTypes.FLAME, this.getRandomX(0.8), this.getRandomY(), this.getRandomZ(0.8), 0, 0.03, 0);
            if (this.random.nextInt(3) == 0) {
                this.level().addParticle(ParticleTypes.LARGE_SMOKE, this.getRandomX(0.6), this.getY() + this.getBbHeight(), this.getRandomZ(0.6), 0, 0.03, 0);
            }
        }
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
        return SoundEvents.WITHER_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 0.5F;
    }
}
