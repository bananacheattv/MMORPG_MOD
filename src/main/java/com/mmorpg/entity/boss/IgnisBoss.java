package com.mmorpg.entity.boss;

import com.mmorpg.combat.RpgCombat;
import com.mmorpg.entity.MagicProjectile;
import com.mmorpg.entity.boss.anim.IgnisAnims;
import com.mmorpg.item.ItemDefs;
import com.mmorpg.server.Scheduler;
import com.mmorpg.entity.FxKind;
import net.minecraft.core.particles.ParticleTypes;
import java.util.List;
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

    private static final int COLOR = 0xFF6020;

    /** Sept attaques : balayage (melee), souffle de feu, frappe au sol, meteores, estoc, lame ardente, eruption. */
    @Override
    protected void defineMoves(List<Move> out) {
        move(out, "souffle", IgnisAnims.SOUFFLE_FEU, 150, 1, 0, 13, level -> {
            int strike = IgnisAnims.STRIKE_TICKS[IgnisAnims.SOUFFLE_FEU];
            telegraphCone(level, 11, 35, strike, ParticleTypes.SMALL_FLAME);
            warn(level, "⚠ Souffle de feu : sortez du cône !");
            playThen(level, IgnisAnims.SOUFFLE_FEU, () -> {
                level.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2f, 0.5f);
                channel(24, 1.5F, 6, () -> {
                    Vec3 dir = facing();
                    double base = Math.atan2(dir.z, dir.x);
                    for (int i = 0; i < 40; i++) {
                        double a = base + (this.random.nextDouble() - 0.5) * 1.2, r = 1.5 + this.random.nextDouble() * 9.5;
                        level.sendParticles(ParticleTypes.FLAME, this.getX() + Math.cos(a) * r, this.getY() + 0.6 + this.random.nextDouble() * 1.5, this.getZ() + Math.sin(a) * r, 1, 0.1, 0.1, 0.1, 0.02);
                    }
                    hitCone(dir, 11, 35, ability("souffleDamage", 0.6), e -> e.igniteForSeconds(4));
                });
            });
        });
        move(out, "ring", IgnisAnims.FRAPPE_SOL, 220, 1, 0, 8, level -> {
            telegraph(level, this.position(), 9, COLOR, IgnisAnims.STRIKE_TICKS[IgnisAnims.FRAPPE_SOL]);
            playThen(level, IgnisAnims.FRAPPE_SOL, () -> {
                Vec3 c = this.position();
                level.playSound(null, this.blockPosition(), SoundEvents.FIRECHARGE_USE, SoundSource.HOSTILE, 2f, 0.5f);
                // trois vagues de flammes qui s'eloignent : on peut sauter entre deux
                for (int w = 0; w < 3; w++) {
                    final int wave = w;
                    Scheduler.later(level, w * 6, () -> {
                        if (!this.isAlive()) return;
                        double in = wave * 3, outR = in + 3;
                        fx(level, FxKind.RING, c, outR, COLOR, 10);
                        for (int i = 0; i < 28; i++) {
                            double a = Math.PI * 2 * i / 28;
                            level.sendParticles(ParticleTypes.FLAME, c.x + Math.cos(a) * (in + 1.5), c.y + 0.2, c.z + Math.sin(a) * (in + 1.5), 2, 0.4, 0.1, 0.4, 0.02);
                        }
                        hitRing(c, in, outR, ability("ringDamage", 1.1), e -> e.igniteForSeconds(4));
                    });
                }
            });
        });
        move(out, "meteor", IgnisAnims.RUGISSEMENT, 300, 2, 0, 40, level -> {
            warn(level, "⚠ Météores : quittez les cercles !");
            level.playSound(null, this.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 2f, 0.6f);
            playThen(level, IgnisAnims.RUGISSEMENT, () ->
                    rain(level, 1 + phase, 3.5, 24, ability("meteorDamage", 1.8), 0xFF4010, FxKind.BURST, ParticleTypes.LAVA, e -> e.igniteForSeconds(3)));
        });
        move(out, "estoc", IgnisAnims.ESTOC, 180, 1, 6, 24, level -> {
            Vec3 at = aim();
            telegraphLine(level, dirTo(at), Math.min(20, Math.sqrt(this.distanceToSqr(at)) + 4), 0xFF3010, IgnisAnims.STRIKE_TICKS[IgnisAnims.ESTOC]);
            warn(level, "⚠ Estoc : écartez-vous de sa trajectoire !");
            playThen(level, IgnisAnims.ESTOC, () -> {
                LivingEntity now = this.getTarget();
                dash(now == null ? facing() : dirTo(now.position()), 12, 1.25, ability("estocDamage", 1.7));
                level.playSound(null, this.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_3.value(), SoundSource.HOSTILE, 2f, 0.6f);
            });
        });
        move(out, "fireball", IgnisAnims.LAME_ARDENTE, 130, 1, 4, 32, level -> playThen(level, IgnisAnims.LAME_ARDENTE, () -> {
            LivingEntity target = this.getTarget();
            if (target == null || !target.isAlive()) return;
            int count = (int) ability("fireballCount", 5) + phase;
            Vec3 dir = dirTo(target.position());
            double baseAngle = Math.atan2(dir.z, dir.x);
            fx(level, FxKind.SLASH, this.position().add(0, 1.5, 0), 4, COLOR, 10);
            for (int i = 0; i < count; i++) {
                double a = baseAngle + (i - (count - 1) / 2.0) * 0.18;
                MagicProjectile p = MagicProjectile.create(level, this, ItemDefs.Element.FEU, RpgCombat.mobAtk(this) * 0.8, 2.0);
                p.setPos(this.getX(), this.getEyeY(), this.getZ());
                p.shoot(Math.cos(a), (target.getY() - this.getEyeY()) / Math.max(1, this.distanceTo(target)), Math.sin(a), 1.1F, 1F);
                level.addFreshEntity(p);
            }
            level.playSound(null, this.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 2f, 0.8f);
        }));
        move(out, "eruption", IgnisAnims.ERUPTION, 320, 2, 0, 40, level -> {
            warn(level, "⚠ Éruption : le sol se fissure sous vos pieds !");
            playThen(level, IgnisAnims.ERUPTION, () -> {
                rain(level, 3 + phase * 2, 2.6, 22, ability("eruptionDamage", 1.6), COLOR, FxKind.PILLAR, ParticleTypes.FLAME, e -> {
                    e.igniteForSeconds(4);
                    if (com.mmorpg.combat.CrowdControl.allow(e)) {
                        e.push(0, 0.7, 0);
                        e.needsSync = true;
                    }
                });
                if (phase >= 4) Scheduler.later(level, 16, () -> {
                    if (this.isAlive()) rain(level, 6, 2.6, 22, ability("eruptionDamage", 1.6), COLOR, FxKind.PILLAR, ParticleTypes.FLAME, e -> e.igniteForSeconds(4));
                });
            });
        });
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
