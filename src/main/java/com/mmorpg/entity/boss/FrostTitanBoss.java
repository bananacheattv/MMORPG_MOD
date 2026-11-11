package com.mmorpg.entity.boss;

import com.mmorpg.combat.RpgCombat;
import com.mmorpg.entity.MagicProjectile;
import com.mmorpg.entity.MobAbilities;
import com.mmorpg.entity.boss.anim.TitanGlaceAnims;
import com.mmorpg.item.ItemDefs;
import com.mmorpg.entity.FxKind;
import com.mmorpg.server.Scheduler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.phys.Vec3;
import java.util.List;
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

    private static final int COLOR = 0x80D0FF;

    @Override
    protected void bossTick(ServerLevel level) {
        if (phase >= 2 && this.tickCount % 20 == 0) {
            double radius = ability("auraRadius", 7);
            for (LivingEntity e : victims(this.position(), radius)) {
                e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 30, 0));
                if (phase >= 3) RpgCombat.mobHit(this, e, 0.15);
            }
        }
    }

    private void chill(LivingEntity e, int ticks, int level) {
        e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, level));
    }

    /** Six attaques : coup de poing (melee), double frappe au sol, pietinement, lancer de glace, revers, blizzard. */
    @Override
    protected void defineMoves(List<Move> out) {
        move(out, "slam", TitanGlaceAnims.DOUBLE_FRAPPE_SOL, 150, 1, 0, 8, level -> {
            telegraph(level, this.position(), 7, COLOR, TitanGlaceAnims.STRIKE_TICKS[TitanGlaceAnims.DOUBLE_FRAPPE_SOL]);
            playThen(level, TitanGlaceAnims.DOUBLE_FRAPPE_SOL, () -> {
                MobAbilities.slam(this, 7, ability("slamDamage", 1.6), 0.8, ParticleTypes.SNOWFLAKE, SoundEvents.GLASS_BREAK);
                fx(level, FxKind.CRACKS, this.position(), 7, COLOR, 60);
                for (LivingEntity e : victims(this.position(), 7)) chill(e, 80, 2);
            });
        });
        move(out, "shard", TitanGlaceAnims.PIETINEMENT, 200, 1, 0, 40, level -> playThen(level, TitanGlaceAnims.PIETINEMENT, () -> {
            int count = (int) ability("shardCount", 10) + phase * 2;
            double offset = this.random.nextDouble() * Math.PI;
            for (int i = 0; i < count; i++) {
                double a = offset + Math.PI * 2 * i / count;
                MagicProjectile p = MagicProjectile.create(level, this, ItemDefs.Element.GIVRE, RpgCombat.mobAtk(this) * 0.7, 0);
                p.setPos(this.getX(), this.getY() + 1.4, this.getZ());
                p.shoot(Math.cos(a), 0, Math.sin(a), 0.9F, 0F);
                level.addFreshEntity(p);
            }
            fx(level, FxKind.SHOCKWAVE, this.position(), 4, COLOR, 12);
            level.playSound(null, this.blockPosition(), SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.HOSTILE, 2f, 0.5f);
        }));
        move(out, "lancer", TitanGlaceAnims.LANCER_GLACE, 170, 1, 8, 36, level -> {
            Vec3 mark = aim();
            int strike = TitanGlaceAnims.STRIKE_TICKS[TitanGlaceAnims.LANCER_GLACE], flight = 10;
            telegraph(level, mark, 4, COLOR, strike + flight);
            warn(level, "⚠ Bloc de glace : quittez le cercle !");
            playThen(level, TitanGlaceAnims.LANCER_GLACE, () -> {
                Vec3 from = this.position().add(0, this.getBbHeight() + 1, 0);
                level.playSound(null, this.blockPosition(), SoundEvents.SNOW_GOLEM_SHOOT, SoundSource.HOSTILE, 2f, 0.4f);
                for (int t = 0; t < flight; t++) {
                    final double k = t / (double) flight;
                    Scheduler.later(level, t, () -> {
                        Vec3 p = from.lerp(mark, k).add(0, Math.sin(k * Math.PI) * 3, 0);
                        level.sendParticles(ParticleTypes.SNOWFLAKE, p.x, p.y, p.z, 14, 0.5, 0.5, 0.5, 0.01);
                        level.sendParticles(ParticleTypes.END_ROD, p.x, p.y, p.z, 3, 0.3, 0.3, 0.3, 0.01);
                    });
                }
                Scheduler.later(level, flight, () -> {
                    if (!this.isAlive()) return;
                    fx(level, FxKind.BURST, mark, 4, COLOR, 14);
                    fx(level, FxKind.CRACKS, mark, 4, COLOR, 60);
                    level.playSound(null, mark.x, mark.y, mark.z, SoundEvents.GLASS_BREAK, SoundSource.HOSTILE, 2f, 0.5f);
                    hitCircle(mark, 4, ability("lancerDamage", 1.7), e -> chill(e, 60, 2));
                });
            });
        });
        move(out, "revers", TitanGlaceAnims.REVERS, 130, 1, 0, 7.5, level -> {
            telegraphCone(level, 7.5, 70, TitanGlaceAnims.STRIKE_TICKS[TitanGlaceAnims.REVERS], ParticleTypes.SNOWFLAKE);
            playThen(level, TitanGlaceAnims.REVERS, () -> {
                fx(level, FxKind.SLASH, this.position().add(0, 1.5, 0), 7, COLOR, 10);
                level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 2f, 0.4f);
                hitCone(facing(), 7.5, 70, ability("reversDamage", 1.5), e -> knock(e, 1.3, 0.45));
            });
        });
        move(out, "blizzard", TitanGlaceAnims.BLIZZARD, 300, 2, 0, 18, level -> {
            int strike = TitanGlaceAnims.STRIKE_TICKS[TitanGlaceAnims.BLIZZARD];
            telegraphCone(level, 14, 30, strike, ParticleTypes.SNOWFLAKE);
            warn(level, "⚠ Blizzard : passez dans son dos !");
            immobile(TitanGlaceAnims.LENGTH_TICKS[TitanGlaceAnims.BLIZZARD]);
            playThen(level, TitanGlaceAnims.BLIZZARD, () -> {
                level.playSound(null, this.blockPosition(), SoundEvents.ELYTRA_FLYING, SoundSource.HOSTILE, 2f, 0.5f);
                channel(36, 1.4F, 6, () -> {
                    Vec3 dir = facing();
                    double base = Math.atan2(dir.z, dir.x);
                    for (int i = 0; i < 50; i++) {
                        double a = base + (this.random.nextDouble() - 0.5) * 1.05, r = 2 + this.random.nextDouble() * 12;
                        level.sendParticles(ParticleTypes.SNOWFLAKE, this.getX() + Math.cos(a) * r, this.getY() + 0.5 + this.random.nextDouble() * 2.5, this.getZ() + Math.sin(a) * r, 1, 0.1, 0.1, 0.1, 0.03);
                    }
                    hitCone(dir, 14, 30, ability("blizzardDamage", 0.45), e -> chill(e, 60, 3));
                });
            });
        });
    }

    /** A chaque changement de phase, le titan s'immobilise et expose son noyau : il subit alors le double de degats. */
    @Override
    protected void onPhaseChange(ServerLevel level, int phase) {
        super.onPhaseChange(level, phase);
        playAnim(TitanGlaceAnims.EXPOSITION_NOYAU);
        immobile(TitanGlaceAnims.LENGTH_TICKS[TitanGlaceAnims.EXPOSITION_NOYAU]);
        expose(TitanGlaceAnims.LENGTH_TICKS[TitanGlaceAnims.EXPOSITION_NOYAU]);
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
