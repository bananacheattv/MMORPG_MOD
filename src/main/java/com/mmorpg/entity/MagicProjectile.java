package com.mmorpg.entity;

import com.mmorpg.combat.RpgCombat;
import com.mmorpg.item.ItemDefs;
import com.mmorpg.registry.ModEntities;
import com.mmorpg.registry.ModItems;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Projectile magique (sorts des mages, des monstres et des boss). Rendu comme un sprite lumineux. */
public class MagicProjectile extends ThrowableItemProjectile {
    private ItemDefs.Element element = ItemDefs.Element.ARCANE;
    private double damage;
    private double aoe;
    private double aoeFraction = 0.5;
    private int life;
    private int maxLife = 60;
    private UUID homingTarget;
    private int pierce;
    private final Set<Integer> hitEntities = new HashSet<>();
    /** Positions recentes (cote client) pour la trainee lumineuse. */
    public final java.util.Deque<Vec3> trailPositions = new java.util.ArrayDeque<>();

    public MagicProjectile(EntityType<? extends MagicProjectile> type, Level level) {
        super(type, level);
    }

    public static MagicProjectile create(Level level, LivingEntity owner, ItemDefs.Element element, double damage, double aoe) {
        MagicProjectile p = new MagicProjectile(ModEntities.MAGIC_PROJECTILE.get(), level);
        p.setOwner(owner);
        p.setPos(owner.getX(), owner.getEyeY() - 0.2, owner.getZ());
        p.element = element;
        p.damage = damage;
        p.aoe = aoe;
        p.setItem(new ItemStack(ModItems.PROJECTILE_SPRITES.get(element).get()));
        return p;
    }

    public MagicProjectile homing(LivingEntity target) {
        this.homingTarget = target.getUUID();
        this.maxLife = 100;
        return this;
    }

    public MagicProjectile pierce(int count) {
        this.pierce = count;
        return this;
    }

    public MagicProjectile aoeFraction(double f) {
        this.aoeFraction = f;
        return this;
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.PROJECTILE_SPRITES.get(ItemDefs.Element.ARCANE).get();
    }

    @Override
    protected double getDefaultGravity() {
        return 0.0;
    }

    public ItemDefs.Element element() {
        Item item = this.getItem().getItem();
        for (ItemDefs.Element e : ItemDefs.Element.values()) {
            if (ModItems.PROJECTILE_SPRITES.get(e).get() == item) return e;
        }
        return element;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            trail();
            return;
        }
        if (++life > maxLife) {
            this.discard();
            return;
        }
        if (homingTarget != null && this.level() instanceof ServerLevel sl) {
            Entity t = sl.getEntity(homingTarget);
            if (t instanceof LivingEntity le && le.isAlive()) {
                Vec3 to = le.position().add(0, le.getBbHeight() * 0.5, 0).subtract(this.position()).normalize();
                Vec3 v = this.getDeltaMovement();
                double speed = Math.max(0.6, v.length());
                Vec3 nv = v.normalize().scale(0.75).add(to.scale(0.25)).normalize().scale(speed);
                this.setDeltaMovement(nv);
                this.needsSync = true;
            }
        }
    }

    private void trail() {
        trailPositions.addFirst(this.position());
        while (trailPositions.size() > 8) trailPositions.removeLast();
        ParticleOptions p = switch (element()) {
            case FEU -> ParticleTypes.FLAME;
            case GIVRE -> ParticleTypes.SNOWFLAKE;
            case NEANT -> ParticleTypes.REVERSE_PORTAL;
            case OMBRE -> ParticleTypes.SCULK_SOUL;
            case ECLAIR -> ParticleTypes.ELECTRIC_SPARK;
            default -> new DustParticleOptions(0xC060FF, 1.2F);
        };
        for (int i = 0; i < 2; i++) {
            this.level().addParticle(p, this.getX() + (random.nextDouble() - 0.5) * 0.2, this.getY() + 0.1 + (random.nextDouble() - 0.5) * 0.2,
                    this.getZ() + (random.nextDouble() - 0.5) * 0.2, 0, 0, 0);
        }
    }

    @Override
    protected boolean canHitEntity(Entity target) {
        if (!super.canHitEntity(target) || target instanceof PetEntity || hitEntities.contains(target.getId())) return false;
        Entity owner = this.getOwner();
        return owner == null || RpgCombat.isEnemy(owner, target);
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        if (!(this.level() instanceof ServerLevel) || !(result.getEntity() instanceof LivingEntity target)) return;
        hitEntities.add(target.getId());
        Entity owner = this.getOwner();
        if (aoe <= 0) {
            hitTarget(owner, target, damage);
        }
        applyElement(target);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!(this.level() instanceof ServerLevel level)) return;
        if (result.getType() == HitResult.Type.ENTITY && pierce > 0) {
            pierce--;
            return;
        }
        Vec3 c = result.getLocation();
        if (aoe > 0) {
            Entity owner = this.getOwner();
            LivingEntity direct = result instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity le ? le : null;
            if (direct != null) hitTarget(owner, direct, damage);
            if (owner instanceof LivingEntity lo) {
                for (LivingEntity e : MobAbilities.enemiesAround(lo, c, aoe)) {
                    if (e != direct) {
                        hitTarget(owner, e, damage * aoeFraction);
                        applyElement(e);
                    }
                }
            }
            level.sendParticles(element() == ItemDefs.Element.GIVRE ? ParticleTypes.SNOWFLAKE : ParticleTypes.EXPLOSION, c.x, c.y, c.z, element() == ItemDefs.Element.GIVRE ? 30 : 2, aoe * 0.3, 0.3, aoe * 0.3, 0.05);
            level.sendParticles(impactParticle(), c.x, c.y, c.z, 25, aoe * 0.3, 0.4, aoe * 0.3, 0.08);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.NEUTRAL, 0.6f, 1.4f);
        } else {
            level.sendParticles(impactParticle(), c.x, c.y, c.z, 12, 0.2, 0.2, 0.2, 0.05);
        }
        this.discard();
    }

    private ParticleOptions impactParticle() {
        return switch (element()) {
            case FEU -> ParticleTypes.LAVA;
            case GIVRE -> ParticleTypes.SNOWFLAKE;
            case NEANT -> ParticleTypes.PORTAL;
            case OMBRE -> ParticleTypes.SCULK_SOUL;
            case ECLAIR -> ParticleTypes.ELECTRIC_SPARK;
            default -> ParticleTypes.WITCH;
        };
    }

    private void hitTarget(Entity owner, LivingEntity target, double amount) {
        if (owner instanceof ServerPlayer sp) {
            RpgCombat.playerHit(sp, target, amount, true);
        } else if (owner != null) {
            RpgCombat.dealDamage(owner, target, amount, false, true);
        }
    }

    private void applyElement(LivingEntity target) {
        switch (element()) {
            case FEU -> target.igniteForSeconds(3);
            case GIVRE -> target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
            case NEANT -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
            case OMBRE -> target.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
            default -> {
            }
        }
    }
}
