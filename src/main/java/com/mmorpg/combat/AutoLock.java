package com.mmorpg.combat;

import com.mmorpg.item.RpgBowItem;
import com.mmorpg.item.RpgStaffItem;
import com.mmorpg.server.FeatureFlags;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/** Autolock PVE des classes a distance : arc et baton visent l'ennemi le plus proche du reticule (jamais un joueur). */
@EventBusSubscriber(modid = com.mmorpg.MMORPG.MODID)
public final class AutoLock {
    public static final double RANGE = 28.0;
    /** Cosinus du demi-angle du cone (environ 14 degres). */
    public static final double CONE_COS = Math.cos(Math.toRadians(14));

    private AutoLock() {
    }

    /** Cible verrouillee pour ce joueur, ou null (interrupteur coupe, rien dans le cone). */
    public static LivingEntity find(ServerPlayer p) {
        if (!FeatureFlags.enabled(p.level().getServer(), "autolock")) return null;
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity e : p.level().getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(RANGE))) {
            if (!valid(p, e)) continue;
            Vec3 to = aimPoint(e).subtract(eye);
            double dist = to.length();
            if (dist < 1.0E-3 || dist > RANGE) continue;
            double cos = to.dot(look) / dist;
            if (cos < CONE_COS) continue;
            double score = (1.0 - cos) * 200.0 + dist * 0.15;
            if (score < bestScore && p.hasLineOfSight(e)) {
                bestScore = score;
                best = e;
            }
        }
        return best;
    }

    /** Ennemis uniquement : monstres du jeu, jamais joueurs, familiers, PNJ ni mannequins. */
    private static boolean valid(ServerPlayer p, LivingEntity e) {
        if (e == p || !e.isAlive() || e.isInvisible() || e.isSpectator()) return false;
        if (e instanceof net.minecraft.world.entity.player.Player) return false;
        return e instanceof Enemy || e.getType().getCategory() == MobCategory.MONSTER;
    }

    public static Vec3 aimPoint(LivingEntity e) {
        return e.position().add(0, e.getBbHeight() * 0.6, 0);
    }

    public static boolean ranged(ServerPlayer p) {
        var item = p.getMainHandItem().getItem();
        return item instanceof RpgBowItem || item instanceof RpgStaffItem;
    }

    /** Indicateur visible par le tireur seul : anneau lumineux autour de la cible verrouillee. */
    @SubscribeEvent
    public static void indicator(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || p.tickCount % 3 != 0 || !ranged(p)) return;
        LivingEntity t = find(p);
        if (t == null || !(p.level() instanceof ServerLevel level)) return;
        double r = Math.max(0.5, t.getBbWidth() * 0.7);
        double y = t.getY() + t.getBbHeight() + 0.35;
        double a = p.tickCount * 0.35;
        for (int i = 0; i < 3; i++) {
            double ang = a + i * Math.PI * 2 / 3;
            level.sendParticles(p, ParticleTypes.END_ROD, true, false, t.getX() + Math.cos(ang) * r * 0.5, y, t.getZ() + Math.sin(ang) * r * 0.5, 1, 0, 0, 0, 0);
        }
        level.sendParticles(p, ParticleTypes.CRIT, true, false, t.getX(), y - 0.25, t.getZ(), 1, 0.05, 0, 0.05, 0);
    }
}
