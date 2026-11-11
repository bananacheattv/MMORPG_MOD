package com.mmorpg.entity.boss;

import com.mmorpg.entity.MobAbilities;
import com.mmorpg.entity.boss.anim.RoiGobelinAnims;
import com.mmorpg.entity.mob.GoblinEntity;
import com.mmorpg.registry.ModEntities;
import com.mmorpg.entity.FxKind;
import com.mmorpg.entity.SkillFxEntity;
import com.mmorpg.server.Scheduler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import java.util.List;
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

    private static final int COLOR = 0xE0A030;

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
    }

    /** Six attaques : coup de hache (melee), frappe verticale, cri de ralliement, tourbillon, charge, bond ecrasant. */
    @Override
    protected void defineMoves(List<Move> out) {
        move(out, "slam", RoiGobelinAnims.FRAPPE_VERTICALE, 160, 1, 0, 6, level -> {
            telegraph(level, this.position(), 5, COLOR, RoiGobelinAnims.STRIKE_TICKS[RoiGobelinAnims.FRAPPE_VERTICALE]);
            playThen(level, RoiGobelinAnims.FRAPPE_VERTICALE, () ->
                    MobAbilities.slam(this, 5, ability("slamDamage", 1.5), 0.7, ParticleTypes.CLOUD, SoundEvents.GENERIC_EXPLODE.value()));
        });
        move(out, "summon", RoiGobelinAnims.CRI_RALLIEMENT, 400, 1, 0, 40, level -> playThen(level, RoiGobelinAnims.CRI_RALLIEMENT, () -> {
            int alive = level.getEntitiesOfClass(GoblinEntity.class, this.getBoundingBox().inflate(24)).size();
            int count = (int) ability("summonCount", 3) + (phase - 1);
            if (alive < 8) MobAbilities.summon(this, ModEntities.GOBLIN.get(), count, "gobelin", -8);
            for (var g : level.getEntitiesOfClass(GoblinEntity.class, this.getBoundingBox().inflate(24))) {
                g.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 0));
                g.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 0));
            }
            fx(level, FxKind.SHOCKWAVE, this.position(), 6, COLOR, 16);
            level.playSound(null, this.blockPosition(), SoundEvents.GOAT_HORN_SOUND_VARIANTS.get(0).value(), SoundSource.HOSTILE, 2f, 1.2f);
        }));
        move(out, "tourbillon", RoiGobelinAnims.TOURBILLON, 220, 1, 0, 7, level -> {
            freeMove();
            int strike = RoiGobelinAnims.STRIKE_TICKS[RoiGobelinAnims.TOURBILLON];
            level.addFreshEntity(SkillFxEntity.create(level, FxKind.CIRCLE, this.position(), 5.5, COLOR, strike + 18).follow(this));
            warn(level, "⚠ Tourbillon : éloignez-vous !");
            playThen(level, RoiGobelinAnims.TOURBILLON, () -> {
                for (int i = 0; i < 3; i++) {
                    Scheduler.later(level, i * 6, () -> {
                        if (!this.isAlive()) return;
                        level.addFreshEntity(SkillFxEntity.create(level, FxKind.WHIRL, this.position().add(0, 1, 0), 5.5, COLOR, 8).follow(this));
                        hitCircle(this.position(), 5.5, 0.8, e -> knock(e, 0.5, 0.25));
                        level.playSound(null, this.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1.5f, 0.6f);
                    });
                }
                // il tourne en avancant sur sa cible
                for (int t = 0; t < 18; t++) {
                    Scheduler.later(level, t, () -> {
                        LivingEntity tg = this.getTarget();
                        if (!this.isAlive() || tg == null) return;
                        Vec3 d = dirTo(tg.position());
                        if (this.distanceTo(tg) > 2 && inBase(this.position().add(d), 1)) this.setDeltaMovement(d.x * 0.28, this.getDeltaMovement().y, d.z * 0.28);
                    });
                }
            });
            // essouffle apres le tourbillon : etourdi, il subit le double de degats
            Scheduler.later(level, RoiGobelinAnims.LENGTH_TICKS[RoiGobelinAnims.TOURBILLON], () -> {
                if (!this.isAlive()) return;
                playFor(level, RoiGobelinAnims.ETOURDI, 40, () -> { });
                immobile(40);
                expose(40);
            });
        });
        move(out, "charge", RoiGobelinAnims.CHARGE, 200, 1, 7, 26, level -> {
            Vec3 at = aim();
            telegraphLine(level, dirTo(at), Math.min(22, Math.sqrt(this.distanceToSqr(at)) + 5), 0xFF5030, RoiGobelinAnims.STRIKE_TICKS[RoiGobelinAnims.CHARGE]);
            warn(level, "⚠ Charge : écartez-vous de sa trajectoire !");
            playThen(level, RoiGobelinAnims.CHARGE, () -> {
                LivingEntity now = this.getTarget();
                dash(now == null ? facing() : dirTo(now.position()), 14, 1.1, ability("chargeDamage", 1.6));
                level.playSound(null, this.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1.5f, 1.4f);
            });
        });
        move(out, "bond", RoiGobelinAnims.BOND_ECRASANT, 260, 2, 5, 20, level -> {
            freeMove();
            Vec3 mark = aim();
            int strike = RoiGobelinAnims.STRIKE_TICKS[RoiGobelinAnims.BOND_ECRASANT];
            telegraph(level, mark, 4.5, 0xFF5030, strike);
            warn(level, "⚠ Bond écrasant : quittez le cercle !");
            Scheduler.later(level, strike - 11, () -> {
                if (!this.isAlive()) return;
                Vec3 to = new Vec3(mark.x - this.getX(), 0, mark.z - this.getZ());
                double speed = Math.min(1.6, to.length() / 11.0);
                Vec3 d = to.lengthSqr() < 1e-4 ? Vec3.ZERO : to.normalize().scale(speed);
                this.setDeltaMovement(d.x, 0.62, d.z);
                this.needsSync = true;
            });
            playThen(level, RoiGobelinAnims.BOND_ECRASANT, () -> {
                this.setDeltaMovement(0, this.getDeltaMovement().y, 0);
                fx(level, FxKind.SHOCKWAVE, this.position(), 4.5, COLOR, 14);
                fx(level, FxKind.CRACKS, this.position(), 4.5, COLOR, 60);
                level.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY() + 0.5, this.getZ(), 4, 1.5, 0.2, 1.5, 0);
                level.playSound(null, this.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(), SoundSource.HOSTILE, 2f, 0.7f);
                hitCircle(this.position(), 4.5, ability("bondDamage", 1.9), e -> knock(e, 0.9, 0.6));
            });
        });
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
