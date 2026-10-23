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
        com.mmorpg.dungeon.BossReveal.play(level, altar);                       // camera du rituel dans l'antre d'un donjon
        level.playSound(null, altar, com.mmorpg.registry.ModSounds.RITUAL.get(), SoundSource.HOSTILE, 1.5f, 1.0f);
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

    /** Vrai si un volume de cette taille, pose au sol en (x, y, z), ne touche aucun bloc. */
    private static boolean fits(ServerLevel level, double x, double y, double z, float w, float h) {
        AABB box = new AABB(x - w / 2.0, y + 0.01, z - w / 2.0, x + w / 2.0, y + h, z + w / 2.0);
        if (!level.noCollision(box)) return false;
        BlockPos below = BlockPos.containing(x, y - 0.5, z);
        return !level.getBlockState(below).getCollisionShape(level, below).isEmpty();
    }

    /**
     * Point libre ou poser un boss : {@code pos} s'il convient, sinon le plus proche autour. L'autel d'invocation est une structure
     * de 5 x 5 x 4 blocs pleine de collisions : un boss apparu en son centre y restait coince (et y revenait a chaque retour a la base).
     */
    public static net.minecraft.world.phys.Vec3 freeSpot(ServerLevel level, BlockPos pos, float w, float h) {
        double cx = pos.getX() + 0.5, cz = pos.getZ() + 0.5;
        for (double r = 0; r <= 14; r += r < 3 ? 3 : 1) {
            int steps = r == 0 ? 1 : (int) Math.max(12, Math.round(r * 6));
            for (int i = 0; i < steps; i++) {
                double a = Math.PI * 2 * i / steps;
                double x = cx + Math.cos(a) * r, z = cz + Math.sin(a) * r;
                for (int y = pos.getY() + 3; y >= pos.getY() - 2; y--) {
                    if (fits(level, x, y, z, w, h)) return new net.minecraft.world.phys.Vec3(x, y, z);
                }
            }
        }
        return new net.minecraft.world.phys.Vec3(cx, pos.getY(), cz);
    }

    public static Mob spawnBoss(ServerLevel level, EntityType<? extends Mob> type, String key, BlockPos pos, String summonerName) {
        Mob boss = type.create(level, EntitySpawnReason.MOB_SUMMONED);
        if (boss == null) return null;
        var spot = freeSpot(level, pos, boss.getBbWidth(), boss.getBbHeight());
        pos = BlockPos.containing(spot);
        boss.snapTo(spot.x, spot.y, spot.z, 0, 0);
        boss.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), EntitySpawnReason.MOB_SUMMONED, null);
        MobConfig cfg = ConfigManager.mob(key);
        MobLevels.forceLevel(boss, key, cfg.minLevel);
        level.addFreshEntity(boss);
        level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, boss.getX(), boss.getY() + 1, boss.getZ(), 1, 0, 0, 0, 0);
        level.playSound(null, pos, com.mmorpg.registry.ModSounds.BOOM.get(), SoundSource.HOSTILE, 2f, 1.0f);
        level.playSound(null, pos, com.mmorpg.registry.ModSounds.GROWL.get(), SoundSource.HOSTILE, 2f, 1.0f);
        String name = cfg.displayName.isEmpty() ? boss.getName().getString() : cfg.displayName;
        if (com.mmorpg.dungeon.ArenaEvents.begin(level, boss, name, cfg.minLevel, summonerName)) return boss;      // annonce cinematique dans l'antre d'un donjon
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) < 96 * 96) {
                Net.toPlayer(p, new Payloads.Notify(Payloads.Notify.BOSS, name.toUpperCase(java.util.Locale.ROOT),
                        "Niveau " + cfg.minLevel + " — invoqué par " + summonerName, 0xFF4040));
            }
        }
        return boss;
    }
}
