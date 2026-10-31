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
    private static final int MAX_BLOCKS_PER_TICK = 6000;
    private static final long BUDGET_NANOS = 8_000_000L;
    /** 2 = envoi aux clients, 16 = pas de mise a jour de forme : pas de cascade de voisins. */
    private static final int FLAGS = 2 | 16;

    private static final class Job {
        final ServerLevel level; final Path file; final String name;
        final int w, h, l; final int ox, oy, oz; final BlockState[] palette; final byte[] raw; final int total; int pos;
        final boolean skipAir; int index; long placed; final CommandSourceStack source;
        Job(CommandSourceStack s, ServerLevel level, Path file, int w, int h, int l, int ox, int oy, int oz, BlockState[] palette, byte[] raw, int total, boolean skipAir) {
            this.source = s; this.level = level; this.file = file; this.name = file.getFileName().toString();
            this.w = w; this.h = h; this.l = l; this.ox = ox; this.oy = oy; this.oz = oz; this.palette = palette; this.raw = raw; this.total = total; this.skipAir = skipAir;
        }
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
            try { READY.add(load(src, level, file, origin, skipAir)); }
            catch (Exception e) { src.getServer().execute(() -> src.sendFailure(Component.literal("Echec : " + e))); }
        });
        return 1;
    }

    private static Job load(CommandSourceStack src, ServerLevel level, Path file, BlockPos origin, boolean skipAir) throws Exception {
        CompoundTag root = NbtIo.readCompressed(file, NbtAccounter.unlimitedHeap());
        if (root.contains("Schematic")) root = root.getCompoundOrEmpty("Schematic");
        int w = root.getShortOr("Width", (short) 0) & 0xFFFF, h = root.getShortOr("Height", (short) 0) & 0xFFFF, l = root.getShortOr("Length", (short) 0) & 0xFFFF;
        CompoundTag pal; byte[] raw;
        if (root.contains("Blocks")) { // v3
            var b = root.getCompoundOrEmpty("Blocks"); pal = b.getCompoundOrEmpty("Palette"); raw = b.getByteArray("Data").orElse(new byte[0]);
        } else { pal = root.getCompoundOrEmpty("Palette"); raw = root.getByteArray("BlockData").orElse(new byte[0]); }
        if (w * h * l == 0 || raw.length == 0) throw new IllegalStateException("schematic vide ou format non Sponge");
        int[] off = root.getIntArray("Offset").orElse(new int[]{0, 0, 0});
        int max = 0;
        for (String k : pal.keySet()) max = Math.max(max, pal.getIntOr(k, 0));
        BlockState[] palette = new BlockState[max + 1];
        java.util.Arrays.fill(palette, Blocks.AIR.defaultBlockState());
        for (String k : pal.keySet()) {
            try { palette[pal.getIntOr(k, 0)] = BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, k, false).blockState(); }
            catch (Exception e) { palette[pal.getIntOr(k, 0)] = Blocks.AIR.defaultBlockState(); }
        }
        long tot = (long) w * h * l; if (tot > Integer.MAX_VALUE) throw new IllegalStateException("trop grand");
        int total = (int) tot;
        return new Job(src, level, file, w, h, l, origin.getX() + off[0], origin.getY() + off[1], origin.getZ() + off[2], palette, raw, total, skipAir);
    }

    @SubscribeEvent public static void tick(ServerTickEvent.Post e) {
        for (Job j; (j = READY.poll()) != null; ) {
            JOBS.add(j);
            final Job fj = j;
            fj.source.sendSuccess(() -> Component.literal(fj.name + " : " + fj.w + "x" + fj.h + "x" + fj.l + ", collage progressif lance."), true);
        }
        Job j = JOBS.peek();
        if (j == null) return;
        long end = System.nanoTime() + BUDGET_NANOS; int n = 0; var pos = new BlockPos.MutableBlockPos();
        int loops = 0;
        while (j.index < j.total && n < MAX_BLOCKS_PER_TICK && ((++loops & 8191) != 0 || System.nanoTime() < end)) {
            int i = j.index++;
            int v = 0, shift = 0, b;
            do { b = j.raw[j.pos++]; v |= (b & 0x7F) << shift; shift += 7; } while ((b & 0x80) != 0 && j.pos < j.raw.length);
            int x = i % j.w, z = (i / j.w) % j.l, y = i / (j.w * j.l);
            var st = j.palette[Math.min(v, j.palette.length - 1)];
            if (j.skipAir && st.isAir()) continue;
            j.level.setBlock(pos.set(j.ox + x, j.oy + y, j.oz + z), st, FLAGS);
            n++; j.placed++;
            if (j.pos >= j.raw.length) { j.index = j.total; break; }
        }
        if (j.index >= j.total) {
            JOBS.poll();
            final Job fj = j;
            fj.source.sendSuccess(() -> Component.literal(fj.name + " : termine, " + fj.placed + " blocs poses."), true);
        } else if (e.getServer().getTickCount() % 200 == 0) {
            final Job fj = j;
            fj.source.sendSuccess(() -> Component.literal(fj.name + " : " + (100L * fj.index / fj.total) + " %"), false);
        }
    }

    @SubscribeEvent public static void commands(RegisterCommandsEvent e) {
        var nom = Commands.argument("nom", StringArgumentType.string()).suggests((c, b) -> net.minecraft.commands.SharedSuggestionProvider.suggest(files(c.getSource()), b));
        var root = Commands.literal("schem").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.literal("list").executes(c -> { c.getSource().sendSuccess(() -> Component.literal("Schematics : " + String.join(", ", files(c.getSource()))), false); return 1; }))
            .then(Commands.literal("stop").executes(c -> { int k = JOBS.size(); JOBS.clear(); c.getSource().sendSuccess(() -> Component.literal(k + " collage(s) arrete(s)."), true); return 1; }))
            .then(Commands.literal("paste").then(nom
                .executes(c -> paste(c, null, true))
                .then(Commands.argument("pos", BlockPosArgument.blockPos())
                    .executes(c -> paste(c, BlockPosArgument.getBlockPos(c, "pos"), true))
                    .then(Commands.argument("air", BoolArgumentType.bool()).executes(c -> paste(c, BlockPosArgument.getBlockPos(c, "pos"), !BoolArgumentType.getBool(c, "air")))))));
        e.getDispatcher().register(root);
    }
}
