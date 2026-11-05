package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.block.LightEmitterBlock;
import com.mmorpg.registry.ModBlocks;
import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

/**
 * Portee de lumiere etendue (jusqu'a 100 blocs) depuis un seul bloc de lumiere : des relais invisibles sont poses
 * sur un reseau de pas 4 autour de la source, avec un niveau qui decroit en douceur avec la distance.
 * Pas de chargement de chunk, pas de traversee de mur, uniquement dans l'air ; travail etale sur plusieurs ticks.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class LightNetwork {
    public static final int STEP = 4, MAX_RANGE = 100, N = 2 * (MAX_RANGE / STEP) + 1;
    public static final int[] RANGES = {15, 25, 40, 60, 80, 100};

    /** Sources connues par dimension : permet de nettoyer les relais d'un bloc supprime. */
    public static final class Sources extends SavedData {
        public static final Codec<Sources> CODEC = Codec.LONG.listOf().xmap(Sources::new, s -> new ArrayList<>(s.set));
        public static final SavedDataType<Sources> TYPE = new SavedDataType<>(MMORPG.id("light_sources"), Sources::new, CODEC);
        final Set<Long> set = new LinkedHashSet<>();
        public Sources() { }
        private Sources(List<Long> l) { set.addAll(l); }
        static Sources get(ServerLevel l) { return l.getDataStorage().computeIfAbsent(TYPE); }
    }

    private static final class Job {
        final BlockPos src; final int range, lvl; final boolean fill; int idx; boolean cleared;
        Job(BlockPos src, int range, int lvl, boolean fill, boolean cleared) { this.src = src; this.range = range; this.lvl = lvl; this.fill = fill; this.cleared = cleared; }
    }

    private static final Map<ServerLevel, LinkedHashMap<Long, Job>> JOBS = new HashMap<>();

    public static int rangeOf(int index) { return RANGES[Math.max(0, Math.min(RANGES.length - 1, index))]; }

    /** Source posee, modifiee ou retrouvee : (re)construit ses relais. */
    public static void changed(ServerLevel l, BlockPos pos) {
        var s = l.getBlockState(pos);
        if (!(s.getBlock() instanceof LightEmitterBlock)) return;
        var data = Sources.get(l);
        if (data.set.add(pos.asLong())) data.setDirty();
        int range = rangeOf(s.getValue(LightEmitterBlock.RANGE)), lvl = s.getValue(LightEmitterBlock.LEVEL);
        JOBS.computeIfAbsent(l, k -> new LinkedHashMap<>()).put(pos.asLong(), new Job(pos.immutable(), range, lvl, range > 15 && lvl > 0, false));
    }

    static int relayLevel(int lvl, int range, double d) {
        return Math.max(1, (int) Math.round(lvl - (lvl - 1) * Math.min(1.0, d / range)));
    }

    /** Aucun chargement de chunk, aucune traversee de mur. */
    /** 1 = degage, 0 = mur, -1 = chunk non charge en chemin (on ne touche a rien). */
    static int clear(ServerLevel l, BlockPos a, BlockPos b) {
        var from = net.minecraft.world.phys.Vec3.atCenterOf(a);
        var delta = net.minecraft.world.phys.Vec3.atCenterOf(b).subtract(from);
        int samples = (int) Math.ceil(delta.length() * 2);
        for (int i = 1; i < samples; i++) {
            var p = BlockPos.containing(from.add(delta.scale((double) i / samples)));
            if (!l.hasChunkAt(p)) return -1;
            if (!l.getBlockState(p).getCollisionShape(l, p).isEmpty()) return 0;
        }
        return 1;
    }

    /** Avance un travail ; renvoie vrai quand il est termine. */
    private static boolean step(ServerLevel l, Job j, int iters, int air) {
        var field = ModBlocks.LIGHT_FIELD.get();
        int total = N * N * N, h = N / 2;
        while (iters-- > 0) {
            if (j.idx >= total) return true;
            int i = j.idx++;
            int dx = (i / (N * N) - h) * STEP, dy = (i / N % N - h) * STEP, dz = (i % N - h) * STEP;
            if (dx == 0 && dy == 0 && dz == 0) continue;
            var p = j.src.offset(dx, dy, dz);
            if (p.getY() < l.getMinY() || p.getY() >= l.getMaxY() || !l.hasChunkAt(p)) continue;
            var old = l.getBlockState(p);
            double d = Math.sqrt((double) dx * dx + dy * dy + dz * dz);
            if (!j.fill || d > j.range || !l.getWorldBorder().isWithinBounds(p)) { if (old.is(field)) l.removeBlock(p, false); continue; }
            if (!old.is(field) && !old.isAir()) continue;
            if (--air < 0) { j.idx--; return false; }
            int los = clear(l, j.src, p);
            if (los < 0) continue;
            if (los == 0) { if (old.is(field)) l.removeBlock(p, false); continue; }
            var next = field.defaultBlockState().setValue(BlockStateProperties.LEVEL, relayLevel(j.lvl, j.range, d));
            if (!old.equals(next)) l.setBlock(p, next, Block.UPDATE_ALL);
        }
        return false;
    }

    /** Pour les tests : execute tout immediatement. */
    public static void runNow(ServerLevel l, BlockPos src, int range, int lvl, boolean fill) {
        var j = new Job(src, range, lvl, fill, false);
        while (!step(l, j, Integer.MAX_VALUE / 2, Integer.MAX_VALUE / 2)) { }
    }

    /** Pour les tests : vide la file de travaux comme le ferait le serveur au fil des ticks. */
    public static int pump(ServerLevel l) {
        var q = JOBS.get(l); int n = 0;
        while (q != null && !q.isEmpty()) {
            var it = q.entrySet().iterator(); var job = it.next().getValue();
            if (step(l, job, 40000, 800)) it.remove();
            n++;
        }
        return n;
    }

    @SubscribeEvent public static void tick(ServerTickEvent.Post e) {
        var server = e.getServer();
        long t = server.getTickCount();
        for (ServerLevel l : server.getAllLevels()) {
            var q = JOBS.get(l);
            if (q != null && !q.isEmpty()) {
                var it = q.entrySet().iterator();
                var job = it.next().getValue();
                if (step(l, job, 40000, 800)) it.remove();
            }
            if (t % 100 == 7) validate(l, t % 2400 == 7);
        }
    }

    private static void validate(ServerLevel l, boolean refresh) {
        var data = Sources.get(l);
        if (data.set.isEmpty()) return;
        var q = JOBS.computeIfAbsent(l, k -> new LinkedHashMap<>());
        for (long k : new ArrayList<>(data.set)) {
            var pos = BlockPos.of(k);
            if (!l.hasChunkAt(pos)) continue;
            var s = l.getBlockState(pos);
            if (!(s.getBlock() instanceof LightEmitterBlock)) {
                data.set.remove(k); data.setDirty();
                q.put(k, new Job(pos, 0, 0, false, false));
            } else if (refresh && !q.containsKey(k) && rangeOf(s.getValue(LightEmitterBlock.RANGE)) > 15 && s.getValue(LightEmitterBlock.LEVEL) > 0) {
                q.put(k, new Job(pos, rangeOf(s.getValue(LightEmitterBlock.RANGE)), s.getValue(LightEmitterBlock.LEVEL), true, true));
            }
        }
    }

    @SubscribeEvent public static void stopped(ServerStoppedEvent e) { JOBS.clear(); }
}
