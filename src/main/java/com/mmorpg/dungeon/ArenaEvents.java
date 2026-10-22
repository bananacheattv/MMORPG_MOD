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
@net.neoforged.fml.common.EventBusSubscriber(modid = com.mmorpg.MMORPG.MODID)
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
            Net.toPlayer(p, new Payloads.AreaEntry(name, "Des renforts surgissent !", Payloads.AreaEntry.PHASE, phase >= 3 ? 0xFF3030 : 0xFF8040, String.valueOf(phase)));
        }
    }

    /** Debut du combat (invocation) dans l'antre d'un donjon : annonce de la phase I. @return vrai si l'annonce a ete faite */
    public static boolean begin(ServerLevel level, Mob boss, String displayName, int bossLevel, String summoner) {
        var zone = ZoneManager.zoneAt(level, boss.blockPosition());
        if (zone == null || !zone.boss()) return false;
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) < 64 * 64) {
                Net.toPlayer(p, new Payloads.AreaEntry(displayName.toUpperCase(java.util.Locale.ROOT), "Niveau " + bossLevel + " — invoqué par " + summoner,
                        Payloads.AreaEntry.PHASE, 0xFF5A3C, "1"));
            }
        }
        return true;
    }

    /** Victoire : le boss d'un antre de donjon meurt. */
    @net.neoforged.bus.api.SubscribeEvent
    public static void onBossDeath(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event) {
        if (!(event.getEntity() instanceof com.mmorpg.entity.boss.RpgBoss boss) || !(boss.level() instanceof ServerLevel level)) return;
        var zone = ZoneManager.zoneAt(level, boss.blockPosition());
        if (zone == null || !zone.boss()) return;
        var dungeonZone = com.mmorpg.zone.ZoneData.get(level.getServer()).byId(zone.id().replace("_boss", ""));
        String label = dungeonZone == null ? "Donjon vaincu" : dungeonZone.name() + " vaincu";
        String name = boss.getName().getString();
        for (ServerPlayer p : level.players()) {
            if (p.distanceToSqr(boss) < 64 * 64) {
                Net.toPlayer(p, new Payloads.AreaEntry("VICTOIRE", name + " est tombé !", Payloads.AreaEntry.VICTORY, 0xFFD060, label));
            }
        }
        level.playSound(null, boss.blockPosition(), SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.PLAYERS, 1.5F, 0.9F);
        for (int i = 0; i < 10; i++) {
            int step = i;
            com.mmorpg.server.Scheduler.later(level, 10 + i * 14, () -> {
                var rnd = level.getRandom();
                double x = boss.getX() + (rnd.nextDouble() - 0.5) * 14;
                double z = boss.getZ() + (rnd.nextDouble() - 0.5) * 14;
                double y = boss.getY() + 4 + rnd.nextDouble() * 6;
                level.sendParticles(ParticleTypes.FIREWORK, x, y, z, 60, 1.6, 1.6, 1.6, 0.12);
                level.sendParticles(step % 3 == 0 ? ParticleTypes.TOTEM_OF_UNDYING : ParticleTypes.END_ROD, x, y, z, 24, 1.2, 1.2, 1.2, 0.2);
                level.playSound(null, BlockPos.containing(x, y, z), SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.2F, 0.9F + rnd.nextFloat() * 0.3F);
            });
        }
    }
}
