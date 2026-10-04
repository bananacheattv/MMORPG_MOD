package com.mmorpg.entity.boss;

import com.mmorpg.combat.RpgCombat;
import com.mmorpg.entity.MobAbilities;
import com.mmorpg.entity.boss.anim.AvatarNeantAnims;
import com.mmorpg.item.ItemDefs;
import com.mmorpg.registry.ModEntities;
import com.mmorpg.server.Scheduler;
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
import net.minecraft.world.phys.Vec3;

/** Avatar du Neant : boss final. Orbes a tete chercheuse, teleportations, serviteurs et trou noir devastateur. */
public class VoidAvatarBoss extends RpgBoss {
    private boolean summoned2;
    private boolean summoned3;

    public VoidAvatarBoss(EntityType<? extends Monster> type, Level level) {
        super(type, level, "avatar_neant");
    }

    private static final AnimSet ANIMS = new AnimSet(AvatarNeantAnims.LENGTH_TICKS, AvatarNeantAnims.STRIKE_TICKS, AvatarNeantAnims.LOOPING,
            AvatarNeantAnims.HOLD, -1, AvatarNeantAnims.MORT, -1);

    @Override
    protected AnimSet animSet() {
        return ANIMS;
    }

    @Override
    protected void bossTick(ServerLevel level) {
        if (phase >= 2 && !summoned2) {
            summoned2 = true;
            MobAbilities.summon(this, ModEntities.VOID_KNIGHT.get(), (int) ability("summonCount", 2), "chevalier_neant", -10);
        }
        if (phase >= 3 && !summoned3) {
            summoned3 = true;
            MobAbilities.summon(this, ModEntities.VOID_KNIGHT.get(), (int) ability("summonCount", 2) + 1, "chevalier_neant", -5);
            this.addEffect(new MobEffectInstance(MobEffects.SPEED, 20 * 600, 1));
        }
        if (busy()) return;
        if (ready("orbCooldown", 100)) {
            playThen(level, AvatarNeantAnims.CHARGE_ENERGIE, () -> {
                for (LivingEntity e : MobAbilities.enemiesAround(this, this.position(), 30)) {
                    for (int i = 0; i < phase; i++) {
                        MobAbilities.bolt(this, e, ItemDefs.Element.NEANT, 0.9, 0.8F + i * 0.2F, 2.0).homing(e);
                    }
                }
                level.playSound(null, this.blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 2f, 0.7f);
            });
            return;
        }
        if (ready("blinkCooldown", 180)) {
            // se disloque, reapparait derriere la cible et se reforme
            immobile(AvatarNeantAnims.STRIKE_TICKS[AvatarNeantAnims.TELEPORTATION_PREPARATION]);
            playThen(level, AvatarNeantAnims.TELEPORTATION_PREPARATION, () -> {
                LivingEntity t = this.getTarget();
                if (t != null && t.isAlive()) {
                    MobAbilities.blinkBehind(this, t);
                    RpgCombat.mobHit(this, t, 1.3);
                }
                playAnim(AvatarNeantAnims.REFORMATION);
            });
            return;
        }
        if (phase >= 2 && ready("voidCooldown", 300)) {
            level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 3f, 0.6f);
            playThen(level, AvatarNeantAnims.OUVERTURE_TORSE, () -> {
                Vec3 c = this.position();
                for (int t = 0; t < 60; t += 3) {
                    final int tt = t;
                    Scheduler.later(level, t, () -> {
                        for (int i = 0; i < 20; i++) {
                            double a = Math.PI * 2 * i / 20 + tt * 0.2;
                            double r = 12 - tt * 0.18;
                            level.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x + Math.cos(a) * r, c.y + 1, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
                        }
                        for (LivingEntity e : MobAbilities.enemiesAround(this, c, 14)) {
                            Vec3 pull = c.subtract(e.position()).normalize().scale(0.25);
                            e.push(pull.x, 0.02, pull.z);
                            e.needsSync = true;
                        }
                    });
                }
                Scheduler.later(level, 60, () -> {
                    level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 1, c.z, 2, 1, 1, 1, 0);
                    level.playSound(null, c.x, c.y, c.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3f, 0.7f);
                    for (LivingEntity e : MobAbilities.enemiesAround(this, c, 5)) {
                        RpgCombat.mobHit(this, e, 2.5);
                    }
                });
            });
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            for (int i = 0; i < 2; i++) {
                this.level().addParticle(ParticleTypes.REVERSE_PORTAL, this.getRandomX(1.0), this.getRandomY(), this.getRandomZ(1.0), 0, 0.02, 0);
            }
            if (this.random.nextInt(3) == 0) {
                this.level().addParticle(ParticleTypes.WITCH, this.getRandomX(0.8), this.getRandomY(), this.getRandomZ(0.8), 0, 0.01, 0);
            }
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENDER_DRAGON_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDER_DRAGON_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDER_DRAGON_DEATH;
    }

    @Override
    public float getVoicePitch() {
        return 1.3F;
    }
}
