package com.mmorpg.dungeon;

import com.mmorpg.network.Net;
import com.mmorpg.network.Payloads;
import com.mmorpg.registry.ModAttachments;
import com.mmorpg.server.MobSpawner;
import com.mmorpg.zone.ZoneManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Mob;

import java.util.List;
import java.util.Map;

/**
 * Evenements d'arene : quand un boss change de phase dans l'antre d'un donjon, des renforts surgissent et une annonce s'affiche.
 * Le solo reste la reference (peu de renforts, de niveau inferieur au boss) ; chaque joueur de plus dans l'arene en ajoute un.
 */
public final class ArenaEvents {
    private static final Map<String, List<String>> ADDS = Map.of(
            "roi_gobelin", List.of("gobelin", "loup_sombre"),
            "liche_ancienne", List.of("squelette_maudit"),
            "seigneur_ignis", List.of("elementaire_feu"),
            "titan_glace", List.of("spectre_givre"),
            "avatar_neant", List.of("chevalier_neant"));

    private ArenaEvents() {
    }

    /** Appele par {@code RpgBoss.onPhaseChange}. */
    public static void phase(ServerLevel level, Mob boss, int phase) {
        var zone = ZoneManager.zoneAt(level, boss.blockPosition());
        if (zone == null || !zone.boss()) return;
        var data = boss.getExistingDataOrNull(ModAttachments.MOB);
        if (data == null || !ADDS.containsKey(data.key)) return;
        List<ServerPlayer> near = level.players().stream().filter(p -> p.isAlive() && !p.isSpectator() && p.distanceToSqr(boss) < 48 * 48).toList();
        int count = Math.min(8, phase + Math.max(0, near.size() - 1));
        var keys = ADDS.get(data.key);
        var random = level.getRandom();
        int lvl = Math.max(1, data.level - 15);
        for (int i = 0; i < count; i++) {
            double a = random.nextDouble() * Math.PI * 2;
            double r = 6 + random.nextDouble() * 5;
            BlockPos p = BlockPos.containing(boss.getX() + Math.cos(a) * r, boss.getY(), boss.getZ() + Math.sin(a) * r);
            var e = MobSpawner.spawnForCommand(level, keys.get(i % keys.size()), p, lvl);
            if (e != null) level.sendParticles(ParticleTypes.PORTAL, e.getX(), e.getY() + 1, e.getZ(), 12, 0.3, 0.6, 0.3, 0.4);
        }
        for (int i = 0; i < 24; i++) {
            double a = Math.PI * 2 * i / 24;
            level.sendParticles(ParticleTypes.FLAME, boss.getX() + Math.cos(a) * 3, boss.getY() + 0.3, boss.getZ() + Math.sin(a) * 3, 1, 0, 0.05, 0, 0.02);
        }
        level.playSound(null, boss.blockPosition(), SoundEvents.ENDER_DRAGON_GROWL, SoundSource.HOSTILE, 1.5F, 0.7F);
        String name = boss.getName().getString().toUpperCase(java.util.Locale.ROOT);
        for (ServerPlayer p : near) {
            Net.toPlayer(p, new Payloads.Notify(Payloads.Notify.BOSS, name, "PHASE " + phase + " — des renforts surgissent !", phase >= 3 ? 0xFF3030 : 0xFF8040));
        }
    }
}
