package com.mmorpg.entity.boss;

import com.mmorpg.config.ConfigManager;
import com.mmorpg.config.MobConfig;
import com.mmorpg.network.Net;
import com.mmorpg.network.Payloads;
import com.mmorpg.registry.ModEntities;
import com.mmorpg.server.MobLevels;
import com.mmorpg.server.Scheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;

/** Invocation des boss sur un Autel d'Invocation (ou par commande). */
public final class BossManager {
    private BossManager() {
    }

    public static EntityType<? extends Mob> typeFor(String key) {
        return switch (key) {
            case "roi_gobelin" -> ModEntities.GOBLIN_KING.get();
            case "liche_ancienne" -> ModEntities.LICH.get();
            case "seigneur_ignis" -> ModEntities.IGNIS.get();
            case "titan_glace" -> ModEntities.FROST_TITAN.get();
            case "avatar_neant" -> ModEntities.VOID_AVATAR.get();
            default -> null;
        };
    }

    public static boolean summon(ServerLevel level, BlockPos altar, String key, ServerPlayer summoner) {
        EntityType<? extends Mob> type = typeFor(key);
        MobConfig cfg = ConfigManager.mob(key);
        if (type == null || cfg == null) return false;
        if (!level.getEntitiesOfClass(RpgBoss.class, new AABB(altar).inflate(48)).isEmpty()) {
            summoner.sendOverlayMessage(Component.literal("Un boss est déjà présent dans les environs !").withColor(0xFF5050));
            return false;
        }
        level.playSound(null, altar, SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.2f, 0.9f);
        for (int t = 0; t < 60; t += 4) {
            final int tt = t;
            Scheduler.later(level, t, () -> {
                for (int i = 0; i < 12; i++) {
                    double a = Math.PI * 2 * i / 12 + tt * 0.15;
                    double r = 3.0 - tt * 0.04;
                    level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, altar.getX() + 0.5 + Math.cos(a) * r, altar.getY() + 1.2 + tt * 0.03,
                            altar.getZ() + 0.5 + Math.sin(a) * r, 1, 0, 0, 0, 0);
                }
            });
        }
        Scheduler.later(level, 60, () -> spawnBoss(level, type, key, altar.above(), summoner.getName().getString()));
        com.mmorpg.block.AltarBlock.activate(level, altar, 120);      // runes allumees, cristaux en rotation
        return true;
    }

    public static Mob spawnBoss(ServerLevel level, EntityType<? extends Mob> type, String key, BlockPos pos, String summonerName) {
        Mob boss = type.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (boss == null) return null;
        boss.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0, 0);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.MOB_SUMMONED, null);
        MobConfig cfg = ConfigManager.mob(key);
        MobLevels.forceLevel(boss, key, cfg.minLevel);
        level.addFreshEntity(boss);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, boss.getX(), boss.getY() + 1, boss.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, pos, SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 2f, 0.8f);
        String name = cfg.displayName.isEmpty() ? boss.getName().getString() : cfg.displayName;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) < 96 * 96) {
                Net.toPlayer(p, new Payloads.Notify(Payloads.Notify.BOSS, name.toUpperCase(java.util.Locale.ROOT),
                        "Niveau " + cfg.minLevel + " — invoqué par " + summonerName, 0xFF4040));
            }
        }
        return boss;
    }
}
