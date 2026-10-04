package com.mmorpg.entity;

import com.mmorpg.combat.RpgCombat;
import com.mmorpg.item.ItemDefs;
import com.mmorpg.server.MobLevels;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/** Capacites speciales reutilisables par les monstres et les boss. */
public final class MobAbilities {
    private MobAbilities() {
    }

    public static List<LivingEntity> enemiesAround(LivingEntity mob, Vec3 center, double radius) {
        AABB box = new AABB(center.x - radius, center.y - radius, center.z - radius, center.x + radius, center.y + radius, center.z + radius);
        return mob.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e != mob && e.position().distanceTo(center) <= radius && RpgCombat.isEnemy(mob, e));
    }

    /** Frappe au sol : degats de zone, projection vers le haut et l'exterieur. */
    public static void slam(Mob mob, double radius, double damageMult, double knockUp, ParticleOptions particle, SoundEvent sound) {
        if (!(mob.level() instanceof ServerLevel level)) return;
        Vec3 c = mob.position();
        for (int i = 0; i < 36; i++) {
            double a = Math.PI * 2 * i / 36;
            for (double r = 1; r <= radius; r += 1.5) {
                level.sendParticles(particle, c.x + Math.cos(a) * r, c.y + 0.2, c.z + Math.sin(a) * r, 1, 0.05, 0.05, 0.05, 0.01);
            }
        }
        level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.5, c.z, 3, 1, 0.2, 1, 0);
        level.playSound(null, mob.blockPosition(), sound, SoundSource.HOSTILE, 1.5f, 0.7f);
        for (LivingEntity e : enemiesAround(mob, c, radius)) {
            RpgCombat.mobHit(mob, e, damageMult);
            Vec3 push = e.position().subtract(c).normalize().scale(0.9);
            e.push(push.x, knockUp, push.z);
            e.needsSync = true;
        }
    }

    /** Tire un projectile magique vers une cible. */
    public static MagicProjectile bolt(Mob mob, LivingEntity target, ItemDefs.Element element, double damageMult, float speed, double aoe) {
        MagicProjectile p = MagicProjectile.create(mob.level(), mob, element, RpgCombat.mobAtk(mob) * damageMult, aoe);
        double dx = target.getX() - mob.getX();
        double dy = target.getY(0.5) - p.getY();
        double dz = target.getZ() - mob.getZ();
        p.shoot(dx, dy, dz, speed, 2.0f);
        mob.level().addFreshEntity(p);
        return p;
    }

    /** Invoque des serviteurs autour du monstre. */
    public static <T extends Mob> int summon(Mob mob, EntityType<T> type, int count, String key, int levelOffset) {
        if (!(mob.level() instanceof ServerLevel level)) return 0;
        int n = 0;
        for (int i = 0; i < count; i++) {
            T minion = type.create(level, EntitySpawnReason.MOB_SUMMONED);
            if (minion == null) continue;
            double a = mob.getRandom().nextDouble() * Math.PI * 2;
            BlockPos pos = BlockPos.containing(mob.getX() + Math.cos(a) * 3, mob.getY(), mob.getZ() + Math.sin(a) * 3);
            minion.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, mob.getYRot(), 0);
            minion.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.MOB_SUMMONED, null);
            int lvl = Math.max(1, RpgCombat.levelOf(mob) + levelOffset);
            MobLevels.forceLevel(minion, key, lvl);
            if (mob.getTarget() != null) minion.setTarget(mob.getTarget());
            level.addFreshEntity(minion);
            level.sendParticles(ParticleTypes.POOF, minion.getX(), minion.getY() + 0.5, minion.getZ(), 15, 0.3, 0.5, 0.3, 0.02);
            n++;
        }
        return n;
    }

    /** Teleporte le monstre derriere sa cible. */
    public static void blinkBehind(Mob mob, LivingEntity target) {
        if (!(mob.level() instanceof ServerLevel level)) return;
        Vec3 look = target.getLookAngle().multiply(1, 0, 1).normalize();
        Vec3 dest = target.position().subtract(look.scale(2.0));
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, mob.getX(), mob.getY() + 1, mob.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
        mob.teleportTo(dest.x, target.getY(), dest.z);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, mob.getX(), mob.getY() + 1, mob.getZ(), 40, 0.4, 0.8, 0.4, 0.05);
        level.playSound(null, mob.blockPosition(), net.minecraft.sounds.SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1f, 0.6f);
        mob.lookAt(target, 360, 360);
    }

    public static Player nearestPlayer(Mob mob, double range) {
        return mob.level().getNearestPlayer(mob, range);
    }
}
