package com.mmorpg.dungeon;

import com.mmorpg.MMORPG;
import com.mmorpg.dungeon.TrialData.Trial;
import com.mmorpg.zone.ZoneData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Remise a zero des donjons : sur commande, ou automatiquement quand plus personne n'y est depuis cinq minutes. Un joueur seul
 * retrouve ainsi un donjon intact a chaque nouvelle tentative, sans rien devoir attendre d'un groupe ; tant qu'il reste dedans
 * (ou qu'il revient avant le delai), sa progression est conservee.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class DungeonManager {
    /** Delai sans joueur avant la remise a zero automatique (en ticks). */
    public static final int AUTO_RESET_TICKS = 20 * 60 * 5;
    private static final Map<String, Long> EMPTY_SINCE = new HashMap<>();

    private DungeonManager() {
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 200 != 0) return;
        TrialData data = TrialData.get(server);
        ZoneData zones = ZoneData.get(server);
        Set<String> groups = new HashSet<>();
        for (Trial t : data.all()) groups.add(TrialData.group(t.id()));
        for (String g : groups) {
            if (data.inGroup(g).stream().noneMatch(Trial::done)) {
                EMPTY_SINCE.remove(g);
                continue;
            }
            ZoneData.Zone z = zones.byId(g);
            ServerLevel level = z == null ? null : server.getLevel(z.dimension());
            if (level == null) continue;
            boolean occupied = false;
            for (ServerPlayer p : level.players()) {
                if (!p.isSpectator() && z.contains(level.dimension(), p.blockPosition())) {
                    occupied = true;
                    break;
                }
            }
            if (occupied) {
                EMPTY_SINCE.remove(g);
                continue;
            }
            long now = server.getTickCount();
            long since = EMPTY_SINCE.computeIfAbsent(g, k -> now);
            if (now - since >= AUTO_RESET_TICKS) {
                reset(server, g);
                EMPTY_SINCE.remove(g);
                MMORPG.LOGGER.info("[MMORPG] Donjon {} remis a zero (vide depuis 5 minutes)", g);
            }
        }
    }

    /** Donjon dont l'emprise contient la position, ou {@code null}. */
    public static @Nullable String groupAt(ServerLevel level, BlockPos pos) {
        for (ZoneData.Zone z : ZoneData.get(level.getServer()).all()) {
            if (!z.boss() && z.contains(level.dimension(), pos) && !TrialData.get(level.getServer()).inGroup(z.id()).isEmpty()) return z.id();
        }
        return null;
    }

    /** Remet a zero toutes les epreuves d'un donjon : herses refermees, dalles eteintes, monstres de vague retires. @return le nombre d'epreuves */
    public static int reset(MinecraftServer server, String group) {
        TrialData data = TrialData.get(server);
        int n = 0;
        for (Trial t : data.inGroup(group)) {
            ServerLevel level = server.getLevel(t.dim());
            if (level == null) continue;
            TrialManager.discardWaveMobs(level, t);
            TrialManager.forget(t.id());
            closeGate(level, t);
            for (int k = 0; k * 3 + 2 < t.pads().size() && k * 2 < t.states().size(); k++) {
                level.setBlock(new BlockPos(t.pads().get(k * 3), t.pads().get(k * 3 + 1), t.pads().get(k * 3 + 2)), TrialManager.state(t.states().get(k * 2)), 3);
            }
            data.markUndone(t.id());
            n++;
        }
        return n;
    }

    private static void closeGate(ServerLevel level, Trial t) {
        if (t.gate().size() != 6 || t.gateBlock().isEmpty() || t.kind().equals(TrialData.JUMP)) return;
        var state = TrialManager.state(t.gateBlock());
        for (int x = t.gate().get(0); x <= t.gate().get(3); x++) {
            for (int y = t.gate().get(1); y <= t.gate().get(4); y++) {
                for (int z = t.gate().get(2); z <= t.gate().get(5); z++) {
                    BlockPos p = new BlockPos(x, y, z);
                    if (level.getBlockState(p).isAir()) level.setBlock(p, state, 2);
                }
            }
        }
        level.sendParticles(ParticleTypes.SMOKE, (t.gate().get(0) + t.gate().get(3)) / 2.0, t.gate().get(1) + 1.5, (t.gate().get(2) + t.gate().get(5)) / 2.0, 12, 1.5, 1, 0.3, 0.01);
    }

    @SuppressWarnings("unused")
    private static Entity unused() {
        return null;
    }
}
