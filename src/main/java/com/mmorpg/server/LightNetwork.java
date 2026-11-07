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
        public static final Codec<Sources> CODEC = Codec.STRING.listOf().xmap(Sources::new, s -> new ArrayList<>(s.set));
        public static final SavedDataType<Sources> TYPE = new SavedDataType<>(MMORPG.id("light_sources"), Sources::new, CODEC);
        final Set<String> set = new LinkedHashSet<>();
        public Sources() { }
        private Sources(List<String> l) { set.addAll(l); }
        static Sources get(ServerLevel l) { return l.getDataStorage().computeIfAbsent(TYPE); }
    }

    private static final class Job {
        final BlockPos src; final int range, lvl; final boolean fill; int idx; boolean cleared;
        Job(BlockPos src, int range, int lvl, boolean fill, boolean cleared) { this.src = src; this.range = range; this.lvl = lvl; this.fill = fill; this.cleared = cleared; }
    }

    private record Source(BlockPos pos, int range, int level, boolean loaded) { }

    /** Une cellule partagee appartient a toutes les sources de sa grille, jamais au dernier travail execute. */
    private static List<Source> contributors(ServerLevel level, Job job) {
        var result = new ArrayList<Source>();
        if (job.fill) result.add(new Source(job.src, job.range, job.lvl, true));
        String prefix = mine(level);
        for (String key : Sources.get(level).set) {
            if (!key.startsWith(prefix)) continue;
            var pos = BlockPos.of(Long.parseLong(key.substring(prefix.length())));
            int dx = pos.getX() - job.src.getX(), dy = pos.getY() - job.src.getY(), dz = pos.getZ() - job.src.getZ();
            if (pos.equals(job.src) || Math.abs(dx) > 2 * MAX_RANGE || Math.abs(dy) > 2 * MAX_RANGE
                    || Math.abs(dz) > 2 * MAX_RANGE || dx % STEP != 0 || dy % STEP != 0 || dz % STEP != 0) continue;
            if (!level.hasChunkAt(pos)) {
                // Ne pas effacer la contribution d'une source dont le chunk est temporairement decharge.
                result.add(new Source(pos, MAX_RANGE, 0, false));
                continue;
            }
            var state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof LightEmitterBlock)) continue;
            int range = rangeOf(state.getValue(LightEmitterBlock.RANGE));
            int strength = state.getValue(LightEmitterBlock.LEVEL);
            if (range > 15 && strength > 0) result.add(new Source(pos, range, strength, true));
        }
        return result;
    }

    private static String key(ServerLevel l, BlockPos p) { return l.dimension() + ";" + p.asLong(); }
    private static String mine(ServerLevel l) { return l.dimension() + ";"; }

    private static final Set<String> DIRTY = new HashSet<>();
    private static final Map<ServerLevel, LinkedHashMap<Long, Job>> JOBS = new HashMap<>();

    public static int rangeOf(int index) { return RANGES[Math.max(0, Math.min(RANGES.length - 1, index))]; }

    /** Source posee, modifiee ou retrouvee : (re)construit ses relais. */
    public static void changed(ServerLevel l, BlockPos pos) {
        var s = l.getBlockState(pos);
        if (!(s.getBlock() instanceof LightEmitterBlock)) return;
        var data = Sources.get(l);
        if (data.set.add(key(l, pos))) data.setDirty();
        int range = rangeOf(s.getValue(LightEmitterBlock.RANGE)), lvl = s.getValue(LightEmitterBlock.LEVEL);
        JOBS.computeIfAbsent(l, k -> new LinkedHashMap<>()).put(pos.asLong(), new Job(pos.immutable(), range, lvl, range > 15 && lvl > 0, false));
    }

    static int relayLevel(int lvl, int range, double d) {
        // Le niveau ne tombe qu'a ~la moitie : entre deux relais la lumiere perd jusqu'a 6 niveaux, il faut de la marge.
        return Math.max(1, (int) Math.round(lvl - (lvl - 1) * 0.5 * Math.min(1.0, d / range)));
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
        var sources = contributors(l, j);
        // Le budget borne aussi les rayons lorsque plusieurs sources se chevauchent.
        int points = Math.max(1, air / Math.max(1, sources.size()));
        int total = N * N * N, h = N / 2;
        while (iters-- > 0) {
            if (j.idx >= total) return true;
            int i = j.idx++;
            int dx = (i / (N * N) - h) * STEP, dy = (i / N % N - h) * STEP, dz = (i % N - h) * STEP;
            if (dx == 0 && dy == 0 && dz == 0) continue;
            var p = j.src.offset(dx, dy, dz);
            if (p.getY() < l.getMinY() || p.getY() >= l.getMaxY() || !l.hasChunkAt(p)) continue;
            var old = l.getBlockState(p);
            if (!old.is(field) && !old.isAir()) continue;
            if (--points < 0) { j.idx--; return false; }
            int strength = 0;
            boolean uncertain = false;
            if (l.getWorldBorder().isWithinBounds(p)) for (var source : sources) {
                double distance = source.pos.distSqr(p);
                if (distance > (double) source.range * source.range) continue;
                if (!source.loaded) { uncertain = true; continue; }
                int los = clear(l, source.pos, p);
                if (los < 0) { uncertain = true; continue; }
                if (los > 0) strength = Math.max(strength, relayLevel(source.level, source.range, Math.sqrt(distance)));
            }
            if (uncertain && old.is(field)) strength = Math.max(strength, old.getValue(BlockStateProperties.LEVEL));
            if (strength == 0) { if (old.is(field)) l.removeBlock(p, false); continue; }
            var next = field.defaultBlockState().setValue(BlockStateProperties.LEVEL, strength);
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
        for (String ks : new ArrayList<>(data.set)) {
            if (!ks.startsWith(mine(l))) continue;
            long k = Long.parseLong(ks.substring(mine(l).length()));
            var pos = BlockPos.of(k);
            if (!l.hasChunkAt(pos)) continue;
            var s = l.getBlockState(pos);
            if (!(s.getBlock() instanceof LightEmitterBlock)) {
                data.set.remove(ks); data.setDirty();
                q.put(k, new Job(pos, 0, 0, false, false));
            } else if (!q.containsKey(k) && (DIRTY.remove(ks) || refresh) && rangeOf(s.getValue(LightEmitterBlock.RANGE)) > 15 && s.getValue(LightEmitterBlock.LEVEL) > 0) {
                q.put(k, new Job(pos, rangeOf(s.getValue(LightEmitterBlock.RANGE)), s.getValue(LightEmitterBlock.LEVEL), true, true));
            }
        }
    }

    /** Un chunk se charge pres d'une source : ses relais manquants seront reconstruits au prochain controle (10 s max). */
    @SubscribeEvent public static void chunkLoaded(net.neoforged.neoforge.event.level.ChunkEvent.Load e) {
        if (!(e.getLevel() instanceof ServerLevel l) || !(e.getChunk() instanceof net.minecraft.world.level.chunk.LevelChunk c)) return;
        var data = Sources.get(l);
        if (data.set.isEmpty()) return;
        int cx = c.getPos().getMiddleBlockX(), cz = c.getPos().getMiddleBlockZ();
        for (String ks : data.set) {
            if (!ks.startsWith(mine(l))) continue;
            var pos = BlockPos.of(Long.parseLong(ks.substring(mine(l).length())));
            if (Math.abs(pos.getX() - cx) <= MAX_RANGE + 16 && Math.abs(pos.getZ() - cz) <= MAX_RANGE + 16) DIRTY.add(ks);
        }
    }

    @SubscribeEvent public static void stopped(ServerStoppedEvent e) { JOBS.clear(); DIRTY.clear(); }
}
