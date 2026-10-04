package com.mmorpg.combat;

import com.mmorpg.entity.MagicProjectile;
import com.mmorpg.item.RpgStaffItem;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.rpg.Stat;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Outils de ciblage et d'effets visuels pour les competences des joueurs. */
public final class SkillHelper {
    private SkillHelper() {
    }

    public static List<LivingEntity> enemiesInRadius(ServerPlayer p, Vec3 center, double radius) {
        AABB box = new AABB(center, center).inflate(radius);
        return p.level().getEntitiesOfClass(LivingEntity.class, box,
                e -> e.position().add(0, e.getBbHeight() / 2, 0).distanceTo(center) <= radius + e.getBbWidth() / 2 && RpgCombat.isEnemy(p, e));
    }

    public static List<LivingEntity> enemiesInCone(ServerPlayer p, double range, double angleDeg) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        double cos = Math.cos(Math.toRadians(angleDeg));
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : enemiesInRadius(p, p.position(), range + 1)) {
            Vec3 to = e.position().add(0, e.getBbHeight() / 2, 0).subtract(eye);
            if (to.length() <= range + e.getBbWidth() && to.normalize().dot(look) >= cos) out.add(e);
        }
        return out;
    }

    /** Ennemis touches par un rayon (largeur donnee) depuis les yeux du joueur. */
    public static List<LivingEntity> enemiesOnLine(ServerPlayer p, double range, double width) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = blockLimitedEnd(p, range);
        Vec3 dir = end.subtract(eye);
        double len = dir.length();
        Vec3 n = dir.normalize();
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : enemiesInRadius(p, eye.add(n.scale(len / 2)), len / 2 + 2)) {
            Vec3 c = e.position().add(0, e.getBbHeight() / 2, 0);
            double t = c.subtract(eye).dot(n);
            if (t < 0 || t > len) continue;
            Vec3 closest = eye.add(n.scale(t));
            if (closest.distanceTo(c) <= width + e.getBbWidth() / 2) out.add(e);
        }
        out.sort(Comparator.comparingDouble(e -> e.distanceToSqr(p)));
        return out;
    }

    public static Vec3 blockLimitedEnd(ServerPlayer p, double range) {
        Vec3 eye = p.getEyePosition();
        Vec3 end = eye.add(p.getLookAngle().scale(range));
        BlockHitResult hit = p.level().clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getType() == HitResult.Type.MISS ? end : hit.getLocation();
    }

    /** Point vise au sol (pour les zones : pluie de fleches, meteore). */
    public static Vec3 targetPoint(ServerPlayer p, double range) {
        LivingEntity aimed = aimedEnemy(p, range);
        if (aimed != null) return aimed.position();
        return blockLimitedEnd(p, range);
    }

    public static LivingEntity aimedEnemy(ServerPlayer p, double range) {
        List<LivingEntity> line = enemiesOnLine(p, range, 1.2);
        return line.isEmpty() ? null : line.get(0);
    }

    public static void line(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle, double step) {
        Vec3 d = to.subtract(from);
        double len = d.length();
        Vec3 n = d.normalize();
        for (double t = 0; t <= len; t += step) {
            Vec3 p = from.add(n.scale(t));
            level.sendParticles(particle, p.x, p.y, p.z, 1, 0.02, 0.02, 0.02, 0);
        }
    }

    public static void ring(ServerLevel level, Vec3 c, double radius, ParticleOptions particle, int points, double y) {
        for (int i = 0; i < points; i++) {
            double a = Math.PI * 2 * i / points;
            level.sendParticles(particle, c.x + Math.cos(a) * radius, c.y + y, c.z + Math.sin(a) * radius, 1, 0, 0, 0, 0);
        }
    }

    public static void knock(Entity e, Vec3 from, double strength, double up) {
        Vec3 d = e.position().subtract(from).multiply(1, 0, 1);
        if (d.lengthSqr() < 1e-4) d = new Vec3(0, 0, 1);
        d = d.normalize().scale(strength);
        e.push(d.x, up, d.z);
        e.needsSync = true;
    }

    /** Clic droit d'un baton : projectile magique de base (80 % de la Puissance magique, 3 mana). */
    public static boolean castStaffBolt(ServerPlayer player, ItemStack stack, RpgStaffItem staff) {
        PlayerData d = RpgPlayers.get(player);
        if (!staff.rpgDef().usableBy(d.playerClass, d.level)) {
            player.sendOverlayMessage(Component.literal("Vous ne pouvez pas utiliser ce bâton.").withColor(0xFF5050));
            return false;
        }
        if (d.mana < 3) {
            player.sendOverlayMessage(Component.literal("Mana insuffisant").withColor(0x5090FF));
            return false;
        }
        d.mana -= 3;
        double dmg = d.stats.get(Stat.MAG) * 0.8;
        MagicProjectile p = MagicProjectile.create(player.level(), player, staff.element(), dmg, 0);
        p.shootFromRotation(player, player.getXRot(), player.getYRot(), 0, 1.8F, 0.5F);
        player.level().addFreshEntity(p);
        player.level().playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.5f, 1.6f);
        RpgPlayers.sendVitals(player, true);
        return true;
    }
}
