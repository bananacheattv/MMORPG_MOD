package com.mmorpg.admin;

import com.mmorpg.MMORPG;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * /schem paste|list|stop : colle un schematic Sponge (.schem v2/v3) par petits lots a chaque tick,
 * sans mise a jour de voisins ni de formes, pour ne jamais bloquer le serveur (aucune dependance a WorldEdit).
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class SchemPaste {
    private static int perTick = 1500;
    private static final long BUDGET_NANOS = 4_000_000L;
    /** 2 = envoi aux clients, 16 = pas de mise a jour de forme : pas de cascade de voisins. */
    private static final int FLAGS = 2 | 16;

    private static final class Job {
        final ServerLevel level; final Path file; final String name;
        final int w, h, l; final int ox, oy, oz; final BlockState[] palette; final java.io.InputStream in; final long total; long remaining;
        final java.util.concurrent.ArrayBlockingQueue<Batch> queue = new java.util.concurrent.ArrayBlockingQueue<>(16); volatile boolean stop; Batch cur; int cp; boolean done; Thread producer;
        final boolean skipAir; long index; long placed; BlockPos origin; long consumed; long dataLen; final CommandSourceStack source;
        Job(CommandSourceStack s, ServerLevel level, Path file, int w, int h, int l, int ox, int oy, int oz, BlockState[] palette, java.io.InputStream in, long total, boolean skipAir) {
            this.source = s; this.level = level; this.file = file; this.name = file.getFileName().toString();
            this.w = w; this.h = h; this.l = l; this.ox = ox; this.oy = oy; this.oz = oz; this.palette = palette; this.in = in; this.total = total; this.remaining = total; this.skipAir = skipAir;
        }
    }

    /** Lot de blocs non-air decode hors du thread serveur ; bytesAfter/indexAfter = position de reprise une fois le lot pose. */
    private record Batch(int n, int[] idx, int[] st, long bytesAfter, long indexAfter, boolean last, String error) {
        Batch(int n, int[] idx, int[] st, long bytesAfter, long indexAfter, boolean last) { this(n, idx, st, bytesAfter, indexAfter, last, null); }
    }

    /** Thread lecteur : decode le flux et saute l'air a pleine vitesse, le thread serveur ne fait que poser. */
    private static void startProducer(Job j) {
        boolean[] air = new boolean[j.palette.length];
        for (int i = 0; i < air.length; i++) air[i] = j.skipAir && j.palette[i].isAir();
        j.producer = Thread.ofPlatform().daemon().name("schem-reader").start(() -> {
            byte[] buf = new byte[1 << 16]; int len = 0, bp = 0;
            long bytes = j.dataLen - j.remaining, index = j.index, lastSent = index;
            int[] idx = new int[2048], st = new int[2048]; int n = 0;
            try {
                while (!j.stop) {
                    boolean end = false; int v = 0, shift = 0;
                    while (true) {
                        if (bp >= len) { len = j.in.read(buf); bp = 0; if (len <= 0) { end = true; break; } }
                        int b = buf[bp++]; bytes++; v |= (b & 0x7F) << shift; shift += 7;
                        if (b >= 0) break;
                    }
                    if (end || bytes > j.dataLen) { offer(j, new Batch(0, idx, st, 0, 0, true, "fichier tronque a " + index + " / " + j.total + " blocs")); return; }
                    long i = index++;
                    if (!air[Math.min(v, air.length - 1)]) { idx[n] = (int) i; st[n++] = v; }
                    if (bytes >= j.dataLen || index >= j.total) { offer(j, new Batch(n, idx, st, bytes, index, true)); return; }
                    if (n == idx.length || index - lastSent >= 4_000_000) {
                        if (!offer(j, new Batch(n, idx, st, bytes, index, false))) return;
                        idx = new int[2048]; st = new int[2048]; n = 0; lastSent = index;
                    }
                }
            } catch (Throwable ex) { MMORPG.LOGGER.error("[schem] erreur de lecture", ex); offer(j, new Batch(0, idx, st, 0, 0, true, ex.toString())); }
        });
    }

    private static boolean offer(Job j, Batch b) {
        try { while (!j.stop) { if (j.queue.offer(b, 200, java.util.concurrent.TimeUnit.MILLISECONDS)) return true; } } catch (InterruptedException ignored) { }
        return false;
    }

    private static void close(Job j) { j.stop = true; try { j.in.close(); } catch (Exception ignored) { } }

    private static Path stateFile(net.minecraft.server.MinecraftServer server) {
        return server.getServerDirectory().resolve("config").resolve("mmorpg").resolve("schem_job.properties");
    }

    private static void save(net.minecraft.server.MinecraftServer server, Job j) {
        try {
            var p = new java.util.Properties();
            p.setProperty("file", j.file.getFileName().toString());
            p.setProperty("x", "" + j.origin.getX()); p.setProperty("y", "" + j.origin.getY()); p.setProperty("z", "" + j.origin.getZ());
            p.setProperty("skipAir", "" + j.skipAir); p.setProperty("dim", j.level.dimension().identifier().toString()); p.setProperty("rate", "" + perTick);
            p.setProperty("consumed", "" + (j.dataLen - j.remaining));
            p.setProperty("index", "" + j.index); p.setProperty("placed", "" + j.placed);
            Files.createDirectories(stateFile(server).getParent());
            Path tmp = stateFile(server).resolveSibling("schem_job.tmp");
            try (var out = Files.newOutputStream(tmp)) { p.store(out, null); }
            Files.move(tmp, stateFile(server), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception ignored) { }
    }

    private static void clearState(net.minecraft.server.MinecraftServer server) {
        try { Files.deleteIfExists(stateFile(server)); } catch (Exception ignored) { }
    }

    /** Reprise automatique au demarrage apres un arret ou un crash (monde principal). */
    @SubscribeEvent public static void started(net.neoforged.neoforge.event.server.ServerStartedEvent e) {
        resume(e.getServer().createCommandSourceStack());
    }

    private static ServerLevel dimLevel(net.minecraft.server.MinecraftServer server, String dim) {
        if (dim != null) {
            var id = net.minecraft.resources.Identifier.tryParse(dim);
            if (id != null) { var l = server.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, id)); if (l != null) return l; }
        }
        return server.overworld();
    }

    static boolean resume(CommandSourceStack src) {
        var server = src.getServer();
        if (!Files.isRegularFile(stateFile(server)) || !JOBS.isEmpty()) return false;
        CompletableFuture.runAsync(() -> {
            try {
                var p = new java.util.Properties();
                try (var in = Files.newInputStream(stateFile(server))) { p.load(in); }
                perTick = Integer.parseInt(p.getProperty("rate", "" + perTick));
                Path file = dir(src).resolve(p.getProperty("file"));
                var origin = new BlockPos(Integer.parseInt(p.getProperty("x")), Integer.parseInt(p.getProperty("y")), Integer.parseInt(p.getProperty("z")));
                MMORPG.LOGGER.info("[schem] reprise de {} a {} %", file.getFileName(), p.getProperty("index"));
                READY.add(load(src, dimLevel(server, p.getProperty("dim")), file, origin, Boolean.parseBoolean(p.getProperty("skipAir")),
                    Long.parseLong(p.getProperty("consumed")), Long.parseLong(p.getProperty("index")), Long.parseLong(p.getProperty("placed"))));
            } catch (Exception ex) {
                MMORPG.LOGGER.warn("[schem] reprise impossible : {}", ex.toString());
                server.execute(() -> src.sendFailure(Component.literal("Reprise impossible : " + ex)));
            }
        });
        return true;
    }

    private static int status(CommandSourceStack src) {
        var j = JOBS.peek();
        String msg;
        if (j != null) {
            int x2 = j.ox + j.w - 1, y2 = j.oy + j.h - 1, z2 = j.oz + j.l - 1;
            msg = j.name + " : " + (100L * j.index / j.total) + " % (" + j.placed + " blocs posés), en cours. Vitesse max " + perTick + " blocs/tick.\n"
                + "Dimension " + j.level.dimension().identifier() + ", coin min " + j.ox + " " + j.oy + " " + j.oz + ", coin max " + x2 + " " + y2 + " " + z2
                + (j.oy < j.level.getMinY() ? "\nATTENTION : les couches sous y=" + j.level.getMinY() + " (limite de la dimension) sont perdues." : "")
                + (y2 >= j.level.getMaxY() ? "\nATTENTION : les couches au-dessus de y=" + (j.level.getMaxY() - 1) + " sont perdues." : "")
                + "\n/schem tp pour s'y rendre.";
        }
        else if (!READY.isEmpty()) msg = "Chargement du schematic en cours...";
        else if (Files.isRegularFile(stateFile(src.getServer()))) msg = "Collage interrompu sauvegardé : /schem resume pour le reprendre.";
        else msg = "Aucun collage en cours ni sauvegardé.";
        final String m = msg;
        src.sendSuccess(() -> Component.literal(m), false);
        return 1;
    }

    @SubscribeEvent public static void stopping(net.neoforged.neoforge.event.server.ServerStoppingEvent e) {
        var j = JOBS.peek();
        if (j != null && j.remaining > 0) save(e.getServer(), j);
        JOBS.forEach(SchemPaste::close);
    }

    private static final ArrayDeque<Job> JOBS = new ArrayDeque<>();
    private static final ConcurrentLinkedQueue<Job> READY = new ConcurrentLinkedQueue<>();

    static Path dir(CommandSourceStack s) {
        return s.getServer().getServerDirectory().resolve("config").resolve("worldedit").resolve("schematics");
    }

    private static List<String> files(CommandSourceStack s) {
        var out = new ArrayList<String>();
        try (var st = Files.list(dir(s))) {
            st.map(p -> p.getFileName().toString()).filter(n -> n.endsWith(".schem")).sorted().forEach(out::add);
        } catch (Exception ignored) { }
        return out;
    }

    private static int paste(CommandContext<CommandSourceStack> c, BlockPos at, boolean skipAir) {
        var src = c.getSource();
        String name = StringArgumentType.getString(c, "nom");
        if (name.contains("/") || name.contains("\\") || name.contains("..")) { src.sendFailure(Component.literal("Nom invalide.")); return 0; }
        if (!name.endsWith(".schem")) name += ".schem";
        Path file = dir(src).resolve(name);
        if (!Files.isRegularFile(file)) { src.sendFailure(Component.literal("Introuvable : " + file + " (/schem list)")); return 0; }
        var level = src.getLevel();
        BlockPos origin = at != null ? at : BlockPos.containing(src.getPosition());
        src.sendSuccess(() -> Component.literal("Lecture de " + file.getFileName() + "..."), true);
        CompletableFuture.runAsync(() -> {
            try { READY.add(load(src, level, file, origin, skipAir, 0, 0, 0)); }
            catch (Exception e) { src.getServer().execute(() -> src.sendFailure(Component.literal("Echec : " + e))); }
        });
        return 1;
    }

    private static final class Hdr { int w, h, l; int[] off = {0, 0, 0}; java.util.Map<String, Integer> pal = new java.util.LinkedHashMap<>(); long dataLen = -1; }

    /** Lit l'en-tete NBT en flux et s'arrete au debut du tableau BlockData : les ~400 Mo ne sont jamais charges en memoire. */
    private static Job load(CommandSourceStack src, ServerLevel level, Path file, BlockPos origin, boolean skipAir, long consumed, long index, long placed) throws Exception {
        var in = new java.io.DataInputStream(new java.io.BufferedInputStream(new java.util.zip.GZIPInputStream(Files.newInputStream(file), 1 << 16), 1 << 16));
        try {
            in.readByte(); in.readUTF();
            var h = new Hdr();
            header(in, h, false);
            if (h.dataLen < 0) throw new IllegalStateException("BlockData introuvable (format non Sponge ou Data avant Palette)");
            long total = (long) h.w * h.h * h.l;
            if (total == 0 || h.pal.isEmpty()) throw new IllegalStateException("schematic vide");
            int max = 0; for (int v : h.pal.values()) max = Math.max(max, v);
            BlockState[] palette = new BlockState[max + 1];
            java.util.Arrays.fill(palette, Blocks.AIR.defaultBlockState());
            for (var en : h.pal.entrySet()) {
                try { palette[en.getValue()] = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, en.getKey(), false).blockState(); }
                catch (Exception ex) { palette[en.getValue()] = Blocks.AIR.defaultBlockState(); }
            }
            var j = new Job(src, level, file, h.w, h.h, h.l, origin.getX() + h.off[0], origin.getY() + h.off[1], origin.getZ() + h.off[2], palette, in, total, skipAir);
            j.remaining = h.dataLen; j.dataLen = h.dataLen; j.origin = origin;
            if (consumed > 0 && consumed < h.dataLen) { in.skipNBytes(consumed); j.remaining = h.dataLen - consumed; j.index = index; j.placed = placed; }
            return j;
        } catch (Exception ex) { in.close(); throw ex; }
    }

    /** Parcourt un compound ; retourne true quand le tableau de blocs est atteint (flux positionne sur ses octets). */
    private static boolean header(java.io.DataInputStream in, Hdr h, boolean inBlocks) throws java.io.IOException {
        while (true) {
            int t = in.readByte();
            if (t == 0) return false;
            String n = in.readUTF();
            if (t == 7 && ((inBlocks && n.equals("Data")) || n.equals("BlockData"))) { h.dataLen = in.readInt(); return true; }
            if (t == 10 && (n.equals("Schematic") || n.equals("Blocks"))) { if (header(in, h, n.equals("Blocks"))) return true; continue; }
            if (t == 2 && n.equals("Width")) h.w = in.readShort() & 0xFFFF;
            else if (t == 2 && n.equals("Height")) h.h = in.readShort() & 0xFFFF;
            else if (t == 2 && n.equals("Length")) h.l = in.readShort() & 0xFFFF;
            else if (t == 11 && n.equals("Offset") ) { int c = in.readInt(); for (int i = 0; i < c; i++) { int v = in.readInt(); if (i < 3) h.off[i] = v; } }
            else if (t == 10 && n.equals("Palette")) { for (int pt; (pt = in.readByte()) != 0; ) { String k = in.readUTF(); if (pt == 3) h.pal.put(k, in.readInt()); else skip(in, pt); } }
            else skip(in, t);
        }
    }

    private static void skip(java.io.DataInputStream in, int t) throws java.io.IOException {
        switch (t) {
            case 1 -> in.skipNBytes(1);
            case 2 -> in.skipNBytes(2);
            case 3, 5 -> in.skipNBytes(4);
            case 4, 6 -> in.skipNBytes(8);
            case 7 -> in.skipNBytes(in.readInt());
            case 8 -> in.skipNBytes(in.readUnsignedShort());
            case 9 -> { int et = in.readByte(), c = in.readInt(); for (int i = 0; i < c; i++) skip(in, et); }
            case 10 -> { for (int ct; (ct = in.readByte()) != 0; ) { in.readUTF(); skip(in, ct); } }
            case 11 -> in.skipNBytes(4L * in.readInt());
            case 12 -> in.skipNBytes(8L * in.readInt());
            default -> throw new java.io.IOException("tag NBT inconnu " + t);
        }
    }

    /** Arret sur erreur : jamais silencieux, la sauvegarde est gardee pour /schem resume. */
    private static void fail(net.minecraft.server.MinecraftServer server, Job j, String why) {
        JOBS.remove(j); close(j); save(server, j);
        MMORPG.LOGGER.error("[schem] {} arrete a {} blocs : {}", j.name, j.placed, why);
        j.source.sendFailure(Component.literal(j.name + " arrete (" + j.placed + " blocs poses) : " + why + ". /schem resume pour reprendre."));
    }

    @SubscribeEvent public static void tick(ServerTickEvent.Post e) {
        try { tick0(e); }
        catch (Throwable t) { var j = JOBS.peek(); MMORPG.LOGGER.error("[schem] erreur de collage", t); if (j != null) fail(e.getServer(), j, t.toString()); }
    }

    private static void tick0(ServerTickEvent.Post e) {
        for (Job j; (j = READY.poll()) != null; ) {
            JOBS.add(j); startProducer(j); save(e.getServer(), j);
            MMORPG.LOGGER.info("[schem] {} {}x{}x{} depuis {},{},{}", j.name, j.w, j.h, j.l, j.ox, j.oy, j.oz);
            final Job fj = j;
            fj.source.sendSuccess(() -> Component.literal(fj.name + " : " + fj.w + "x" + fj.h + "x" + fj.l + ", collage progressif lance."), true);
        }
        Job j = JOBS.peek();
        if (j == null) return;
        long end = System.nanoTime() + BUDGET_NANOS; int n = 0; var pos = new BlockPos.MutableBlockPos();
        while (n < perTick && System.nanoTime() < end && !j.done) {
            if (j.cur == null) { j.cur = j.queue.poll(); j.cp = 0; if (j.cur == null) break; }
            var bt = j.cur;
            if (bt.error() != null) { fail(e.getServer(), j, bt.error()); return; }
            while (j.cp < bt.n() && n < perTick) {
                int k = j.cp++; int i = bt.idx()[k];
                int x = i % j.w, z = (i / j.w) % j.l, y = i / (j.w * j.l);
                j.level.setBlock(pos.set(j.ox + x, j.oy + y, j.oz + z), j.palette[Math.min(bt.st()[k], j.palette.length - 1)], FLAGS);
                n++; j.placed++;
            }
            if (j.cp < bt.n()) break;
            j.index = bt.indexAfter(); j.remaining = j.dataLen - bt.bytesAfter(); j.cur = null;
            if (bt.last()) j.done = true;
        }
        if (j.done) j.index = j.total;
        else if (e.getServer().getTickCount() % 20 == 0) save(e.getServer(), j);
        if (j.index >= j.total) {
            JOBS.poll(); close(j); clearState(e.getServer());
            final Job fj = j;
            fj.source.sendSuccess(() -> Component.literal(fj.name + " : termine, " + fj.placed + " blocs poses."), true);
            MMORPG.LOGGER.info("[schem] {} termine : {} blocs", fj.name, fj.placed);
        } else if (e.getServer().getTickCount() % 200 == 0) {
            MMORPG.LOGGER.info("[schem] {} : {} % ({} blocs)", j.name, 100L * j.index / j.total, j.placed);
            final Job fj = j;
            fj.source.sendSuccess(() -> Component.literal(fj.name + " : " + (100L * fj.index / fj.total) + " %"), false);
        }
    }

    @SubscribeEvent public static void commands(RegisterCommandsEvent e) {
        var nom = Commands.argument("nom", StringArgumentType.string()).suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(files(c.getSource()), b));
        var root = Commands.literal("schem").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("list").executes(c -> { c.getSource().sendSuccess(() -> Component.literal("Schematics : " + String.join(", ", files(c.getSource()))), false); return 1; }))
            .then(Commands.literal("rate").then(Commands.argument("blocs", com.mojang.brigadier.arguments.IntegerArgumentType.integer(10, 20000)).executes(c -> {
                perTick = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(c, "blocs");
                c.getSource().sendSuccess(() -> Component.literal("Vitesse : " + perTick + " blocs par tick maximum."), true); return 1; })))
            .then(Commands.literal("tp").executes(c -> {
                var j = JOBS.peek(); var pl = c.getSource().getPlayerOrException();
                if (j == null) { c.getSource().sendFailure(Component.literal("Aucun collage en cours.")); return 0; }
                double cx = j.ox + j.w / 2.0, cz = j.oz + j.l / 2.0; int top = Math.min(j.oy + j.h - 1, j.level.getMaxY() - 2);
                pl.teleportTo(j.level, cx, Math.max(top, j.level.getMinY() + 2), cz, java.util.Set.of(), pl.getYRot(), 0, true);
                pl.resetFallDistance(); pl.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOW_FALLING, 600, 0));
                c.getSource().sendSuccess(() -> Component.literal("Téléporté au centre, tout en haut du schematic (chute lente 30 s)."), false); return 1; }))
            .then(Commands.literal("status").executes(c -> status(c.getSource())))
            .then(Commands.literal("resume").executes(c -> {
                if (resume(c.getSource())) { c.getSource().sendSuccess(() -> Component.literal("Reprise lancée."), true); return 1; }
                c.getSource().sendFailure(Component.literal("Rien à reprendre (ou un collage est déjà en cours).")); return 0; }))
            .then(Commands.literal("stop").executes(c -> { int k = JOBS.size(); JOBS.forEach(SchemPaste::close); JOBS.clear(); clearState(c.getSource().getServer()); c.getSource().sendSuccess(() -> Component.literal(k + " collage(s) arrete(s)."), true); return 1; }))
            .then(Commands.literal("paste").then(nom
                .executes(c -> paste(c, null, true))
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .executes(c -> paste(c, BlockPosArgument.getBlockPos(c, "pos"), true))
                    .then(Commands.argument("air", BoolArgumentType.bool()).executes(c -> paste(c, BlockPosArgument.getBlockPos(c, "pos"), !BoolArgumentType.getBool(c, "air")))))));
        e.getDispatcher().register(root);
    }
}
