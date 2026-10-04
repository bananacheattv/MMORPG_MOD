package com.mmorpg.entity;

import com.mmorpg.combat.RpgCombat;
import com.mmorpg.registry.ModEntities;
import com.mmorpg.skill.Fx;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Projectile de competence dessine a la main (boule de feu, meteore, fleches de lumiere, orbe de foudre...).
 * Le type est synchronise pour que le client choisisse le rendu ({@code SpellProjectileRenderer}).
 */
public class SpellProjectile extends ThrowableProjectile {
    public enum Kind {
        FIREBALL(0xFF7A1E), METEOR(0xFF6A10), LIGHT_ARROW(0xFFE9A0), EXPLOSIVE_ARROW(0xFF4A20), RAIN_ARROW(0xB8FF7A),
        VOLLEY_ARROW(0x9AD8FF), LIGHTNING_ORB(0xA8D8FF);

        public final int color;

        Kind(int color) {
            this.color = color;
        }

        public boolean arrow() {
            return this == LIGHT_ARROW || this == EXPLOSIVE_ARROW || this == RAIN_ARROW || this == VOLLEY_ARROW;
        }

        public static Kind byId(int id) {
            Kind[] v = values();
            return id >= 0 && id < v.length ? v[id] : FIREBALL;
        }
    }

    private static final EntityDataAccessor<Integer> KIND = SynchedEntityData.defineId(SpellProjectile.class, EntityDataSerializers.INT);

    private double damage;
    private double aoe;
    private double aoeFraction = 0.6;
    private boolean magic = true;
    private boolean pierce;
    private int life;
    private int maxLife = 60;
    private UUID homing;
    private final Set<Integer> hit = new HashSet<>();
    /** Positions precedentes (cote client, pour la trainee). */
    public final Deque<Vec3> trail = new ArrayDeque<>();

    public SpellProjectile(EntityType<? extends SpellProjectile> type, Level level) {
        super(type, level);
    }

    public static SpellProjectile create(ServerLevel level, LivingEntity owner, Kind kind, double damage, double aoe) {
        SpellProjectile p = new SpellProjectile(ModEntities.SPELL_PROJECTILE.get(), level);
        p.setOwner(owner);
        p.setPos(owner.getX(), owner.getEyeY() - 0.25, owner.getZ());
        p.entityData.set(KIND, kind.ordinal());
        p.damage = damage;
        p.aoe = aoe;
        p.magic = !kind.arrow();
        return p;
    }

    public SpellProjectile pierce() {
        this.pierce = true;
        return this;
    }

    public SpellProjectile life(int ticks) {
        this.maxLife = ticks;
        return this;
    }

    public SpellProjectile aoeFraction(double f) {
        this.aoeFraction = f;
        return this;
    }

    public SpellProjectile homing(LivingEntity target) {
        this.homing = target.getUUID();
        this.maxLife = 100;
        return this;
    }

    public Kind kind() {
        return Kind.byId(this.entityData.get(KIND));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(KIND, 0);
    }

    @Override
    protected float getAirDrag() {
        return 1.0F;
    }

    @Override
    protected double getDefaultGravity() {
        return kind() == Kind.EXPLOSIVE_ARROW ? 0.02 : 0.0;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            trail.addFirst(this.position());
            while (trail.size() > (kind() == Kind.METEOR ? 16 : 10)) trail.removeLast();
            particles();
            return;
        }
        if (++life > maxLife) {
            this.discard();
            return;
        }
        if (homing != null && this.level() instanceof ServerLevel sl && sl.getEntity(homing) instanceof LivingEntity t && t.isAlive()) {
            Vec3 to = t.position().add(0, t.getBbHeight() * 0.5, 0).subtract(this.position()).normalize();
            Vec3 v = this.getDeltaMovement();
            double speed = Math.max(0.6, v.length());
            this.setDeltaMovement(v.normalize().scale(0.7).add(to.scale(0.3)).normalize().scale(speed));
            this.needsSync = true;
        }
    }

    private void particles() {
        Kind k = kind();
        ParticleOptions p = switch (k) {
            case FIREBALL, METEOR -> ParticleTypes.FLAME;
            case EXPLOSIVE_ARROW -> ParticleTypes.SMALL_FLAME;
            case LIGHTNING_ORB -> ParticleTypes.ELECTRIC_SPARK;
            case RAIN_ARROW -> new DustParticleOptions(0xB8FF7A, 0.8F);
            default -> ParticleTypes.END_ROD;
        };
        int n = k == Kind.METEOR ? 4 : k == Kind.FIREBALL ? 2 : 1;
        for (int i = 0; i < n; i++) {
            double s = k == Kind.METEOR ? 0.7 : 0.15;
            this.level().addParticle(p, getX() + (random.nextDouble() - 0.5) * s, getY() + (random.nextDouble() - 0.5) * s,
                    getZ() + (random.nextDouble() - 0.5) * s, 0, 0, 0);
        }
        if (k == Kind.METEOR || k == Kind.FIREBALL) {
            this.level().addParticle(ParticleTypes.LARGE_SMOKE, getX(), getY(), getZ(), 0, 0.02, 0);
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (!super.canHitEntity(target) || target instanceof PetEntity || target instanceof SkillFxEntity || hit.contains(target.getId())) return false;
        Entity owner = this.getOwner();
        return owner == null || RpgCombat.isEnemy(owner, target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!(this.level() instanceof ServerLevel level) || !(result.getEntity() instanceof LivingEntity target)) return;
        hit.add(target.getId());
        if (aoe <= 0) {
            damage(target, damage);
            Vec3 c = target.position().add(0, target.getBbHeight() * 0.5, 0);
            Fx.spawn(level, FxKind.BURST, c, kind() == Kind.LIGHTNING_ORB ? 1.1 : 0.7, kind().color, 8);
            if (kind() == Kind.LIGHTNING_ORB) {
                level.playSound(null, c.x, c.y, c.z, SoundEvents.TRIDENT_THUNDER.value(), SoundSource.PLAYERS, 0.25f, 2.0f);
            }
        }
        if (kind() == Kind.FIREBALL || kind() == Kind.METEOR) target.igniteForSeconds(4);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(this.level() instanceof ServerLevel level)) return;
        if (result.getType() == HitResult.Type.ENTITY && pierce) return;
        Vec3 c = result.getLocation();
        if (aoe > 0) {
            LivingEntity direct = result instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le ? le : null;
            if (direct != null) damage(direct, damage);
            if (this.getOwner() instanceof LivingEntity owner) {
                for (LivingEntity e : MobAbilities.enemiesAround(owner, c, aoe)) {
                    if (e != direct && !hit.contains(e.getId())) {
                        damage(e, damage * aoeFraction);
                        if (kind() == Kind.FIREBALL || kind() == Kind.METEOR) e.igniteForSeconds(4);
                    }
                }
            }
            explode(level, c);
        } else if (result.getType() == HitResult.Type.BLOCK) {
            Fx.spawn(level, FxKind.BURST, c, 0.6, kind().color, 7);
        }
        this.discard();
    }

    private void explode(ServerLevel level, Vec3 c) {
        Kind k = kind();
        switch (k) {
            case METEOR -> {
                Fx.spawn(level, FxKind.BURST, c, aoe * 1.1, 0xFF6A10, 22);
                Fx.spawn(level, FxKind.SHOCKWAVE, c, aoe * 1.2, 0xFF8A30, 18);
                Fx.spawn(level, FxKind.CRACKS, c, aoe, 0xFF6A10, 40);
                level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, c.x, c.y, c.z, 1, 0, 0, 0, 0);
                level.sendParticles(ParticleTypes.LAVA, c.x, c.y + 0.5, c.z, 40, aoe / 2, 0.4, aoe / 2, 0.2);
                level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 2.2f, 0.55f);
            }
            case FIREBALL -> {
                Fx.spawn(level, FxKind.BURST, c, aoe, 0xFF7A1E, 14);
                Fx.spawn(level, FxKind.RING, c, aoe * 1.2, 0xFF5010, 14);
                level.sendParticles(ParticleTypes.LAVA, c.x, c.y, c.z, 12, aoe * 0.3, 0.3, aoe * 0.3, 0.1);
                level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 1.0f, 1.3f);
            }
            default -> {
                Fx.spawn(level, FxKind.BURST, c, aoe, k.color, 12);
                Fx.spawn(level, FxKind.RING, c, aoe * 1.1, k.color, 12);
                level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.PLAYERS, 0.8f, 1.5f);
            }
        }
    }

    private void damage(LivingEntity target, double amount) {
        Entity owner = this.getOwner();
        if (owner instanceof ServerPlayer sp) {
            RpgCombat.playerHit(sp, target, amount, magic);
        } else if (owner != null) {
            RpgCombat.dealDamage(owner, target, amount, false, magic);
        }
    }
}
