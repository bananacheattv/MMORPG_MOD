package com.mmorpg.entity.boss;

import com.mmorpg.entity.MobAbilities;
import com.mmorpg.entity.boss.anim.AvatarNeantAnims;
import com.mmorpg.item.ItemDefs;
import com.mmorpg.registry.ModEntities;
import com.mmorpg.server.Scheduler;
import com.mmorpg.entity.FxKind;
import net.minecraft.core.particles.ParticleTypes;
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
import net.minecraft.world.phys.Vec3;

/** Avatar du Neant : boss final. Orbes a tete chercheuse, teleportations, serviteurs et trou noir devastateur. */
public class VoidAvatarBoss extends RpgBoss {
    private boolean summoned2;
    private boolean summoned3;

    public VoidAvatarBoss(EntityType<? extends Monster> type, Level level) {
        super(type, level, "avatar_neant");
    }

    private static final AnimSet ANIMS = new AnimSet(AvatarNeantAnims.LENGTH_TICKS, AvatarNeantAnims.STRIKE_TICKS, AvatarNeantAnims.LOOPING,
            AvatarNeantAnims.HOLD, AvatarNeantAnims.GRIFFES, AvatarNeantAnims.MORT, -1);

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
    }

    private static final int COLOR = 0xB040FF;

    /** Six attaques : griffes (melee), orbes chercheurs, teleportation-frappe, trou noir, rayon du neant, pluie d'eclats. */
    @Override
    protected void defineMoves(List<Move> out) {
        move(out, "orb", AvatarNeantAnims.CHARGE_ENERGIE, 130, 1, 0, 40, level -> playThen(level, AvatarNeantAnims.CHARGE_ENERGIE, () -> {
            for (LivingEntity e : victims(this.position(), 30)) {
                for (int i = 0; i < Math.min(3, phase); i++) {
                    MobAbilities.bolt(this, e, ItemDefs.Element.NEANT, 0.9, 0.8F + i * 0.2F, 2.0).homing(e);
                }
            }
            level.playSound(null, this.blockPosition(), SoundEvents.ENDER_DRAGON_SHOOT, SoundSource.HOSTILE, 2f, 0.7f);
        }));
        move(out, "blink", AvatarNeantAnims.TELEPORTATION_PREPARATION, 180, 1, 5, 40, level -> {
            // se disloque, reapparait derriere la cible, se reforme puis frappe : on a le temps de s'ecarter
            immobile(AvatarNeantAnims.STRIKE_TICKS[AvatarNeantAnims.TELEPORTATION_PREPARATION] + AvatarNeantAnims.LENGTH_TICKS[AvatarNeantAnims.REFORMATION]);
            playThen(level, AvatarNeantAnims.TELEPORTATION_PREPARATION, () -> {
                LivingEntity t = this.getTarget();
                if (t != null && t.isAlive()) MobAbilities.blinkBehind(this, t);
                int strike = AvatarNeantAnims.STRIKE_TICKS[AvatarNeantAnims.REFORMATION] + 6;
                telegraph(level, this.position(), 4, COLOR, strike);
                playAnim(AvatarNeantAnims.REFORMATION);
                Scheduler.later(level, strike, () -> {
                    if (!this.isAlive()) return;
                    fx(level, FxKind.NOVA, this.position(), 4, COLOR, 14);
                    hitCircle(this.position(), 4, ability("blinkDamage", 1.4), e -> knock(e, 0.8, 0.35));
                });
            });
        });
        move(out, "void", AvatarNeantAnims.OUVERTURE_TORSE, 300, 2, 0, 40, level -> {
            level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_CHARGE, SoundSource.HOSTILE, 3f, 0.6f);
            warn(level, "⚠ Trou noir : fuyez le centre !");
            immobile(AvatarNeantAnims.STRIKE_TICKS[AvatarNeantAnims.OUVERTURE_TORSE] + 60);
            playThen(level, AvatarNeantAnims.OUVERTURE_TORSE, () -> {
                Vec3 c = this.position();
                telegraph(level, c, 5, 0xFF3060, 60);
                for (int t = 0; t < 60; t += 3) {
                    final int tt = t;
                    Scheduler.later(level, t, () -> {
                        if (!this.isAlive()) return;
                        for (int i = 0; i < 20; i++) {
                            double a = Math.PI * 2 * i / 20 + tt * 0.2;
                            double r = 12 - tt * 0.18;
                            level.sendParticles(ParticleTypes.REVERSE_PORTAL, c.x + Math.cos(a) * r, c.y + 1, c.z + Math.sin(a) * r, 1, 0, 0, 0, 0);
                        }
                        for (LivingEntity e : victims(c, 14)) {
                            Vec3 pull = c.subtract(e.position()).normalize().scale(0.25);
                            if (com.mmorpg.combat.CrowdControl.allow(e)) {
                                e.push(pull.x, 0.02, pull.z);
                                if (e instanceof net.minecraft.server.level.ServerPlayer sp) com.mmorpg.anticheat.AntiCheat.grace(sp, 40);
                            }
                            e.needsSync = true;
                        }
                    });
                }
                Scheduler.later(level, 60, () -> {
                    if (!this.isAlive()) return;
                    level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y + 1, c.z, 2, 1, 1, 1, 0);
                    level.playSound(null, c.x, c.y, c.z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 3f, 0.7f);
                    hitCircle(c, 5, ability("voidDamage", 2.5), null);
                });
            });
        });
        move(out, "rayon", AvatarNeantAnims.RAYON, 240, 1, 4, 30, level -> {
            telegraphLine(level, dirTo(aim()), 28, COLOR, AvatarNeantAnims.STRIKE_TICKS[AvatarNeantAnims.RAYON]);
            warn(level, "⚠ Rayon du néant : contournez-le !");
            immobile(AvatarNeantAnims.LENGTH_TICKS[AvatarNeantAnims.RAYON]);
            playThen(level, AvatarNeantAnims.RAYON, () -> {
                level.playSound(null, this.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.HOSTILE, 2f, 1.4f);
                beam(level, 36, 28, 1.5, ability("rayonDamage", 0.6), COLOR, ParticleTypes.WITCH);
            });
        });
        move(out, "eclats", AvatarNeantAnims.PLUIE_ECLATS, 260, 1, 0, 40, level -> {
            warn(level, "⚠ Pluie d'éclats : quittez les cercles !");
            playThen(level, AvatarNeantAnims.PLUIE_ECLATS, () -> {
                rain(level, 2 + phase, 3, 18, ability("eclatsDamage", 1.5), COLOR, FxKind.BURST, ParticleTypes.REVERSE_PORTAL, null);
                if (phase >= 3) Scheduler.later(level, 14, () -> {
                    if (this.isAlive()) rain(level, 3, 3, 18, ability("eclatsDamage", 1.5), COLOR, FxKind.BURST, ParticleTypes.REVERSE_PORTAL, null);
                });
            });
        });
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
