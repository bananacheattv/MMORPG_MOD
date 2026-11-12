package com.mmorpg.aeloria;

import com.mmorpg.MMORPG;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.blocks.BlockStateParser;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.chunk.PalettedContainer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * /aeloriaimport : importe les 100 secteurs Sponge v3 d'Aéloria dans la dimension de l'opérateur, dans l'ordre et aux
 * origines absolues de importation.csv, comme un {@code //paste -a -o -b} (air ignoré, eau collée, biomes appliqués).
 *
 * <p>Module autonome (aucune dépendance à WorldEdit ni aux systèmes de jeu). Contrôle préalable des 100 fichiers
 * (présence, taille, SHA-256, en-tête, palettes) avant la première modification, puis un secteur à la fois : lecture
 * asynchrone, écriture chunk par chunk par tranches de temps sur le thread serveur, sauvegarde du monde, enregistrement
 * de la reprise. Sous-commandes : status, stop, verify, reset, budget.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class AeloriaImport {
    private static final int EXPECTED_DATA_VERSION = 5023;
    private static final String TAG = "[aeloria] ";

    private static volatile Job job;
    private static long budgetMs = 20;
    private static int savedPause = -1;

    private AeloriaImport() {}

    // ------------------------------------------------------------------ commande

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        var root = Commands.literal("aeloriaimport")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> begin(c.getSource(), Mode.PASTE))
                .then(Commands.literal("status").executes(c -> status(c.getSource())))
                .then(Commands.literal("stop").executes(c -> stop(c.getSource())))
                .then(Commands.literal("verify").executes(c -> begin(c.getSource(), Mode.VERIFY)))
                .then(Commands.literal("reset").executes(c -> reset(c.getSource())))
                .then(Commands.literal("budget")
                        .then(Commands.argument("ms", IntegerArgumentType.integer(1, 45)).executes(c -> {
                            budgetMs = IntegerArgumentType.getInteger(c, "ms");
                            c.getSource().sendSuccess(() -> Component.literal("Budget d'écriture : " + budgetMs + " ms par tick."), true);
                            return 1;
                        })));
        e.getDispatcher().register(root);
    }

    private static int status(CommandSourceStack src) {
        Job j = job;
        src.sendSuccess(() -> Component.literal(j == null ? "Aucun import Aéloria en cours." : j.statusLine()), false);
        return 1;
    }

    private static int stop(CommandSourceStack src) {
        Job j = job;
        if (j == null) {
            src.sendFailure(Component.literal("Aucun import Aéloria en cours."));
            return 0;
        }
        j.stop = true;
        src.sendSuccess(() -> Component.literal("Arrêt demandé : fin du chunk en cours, puis reprise possible avec /aeloriaimport."), true);
        return 1;
    }

    private static int reset(CommandSourceStack src) {
        if (job != null) {
            src.sendFailure(Component.literal("Un import est en cours : /aeloriaimport stop d'abord."));
            return 0;
        }
        try {
            Files.deleteIfExists(progressFile(src.getServer(), src.getLevel()));
        } catch (IOException ex) {
            src.sendFailure(Component.literal("Impossible d'effacer la reprise : " + ex.getMessage()));
            return 0;
        }
        src.sendSuccess(() -> Component.literal("Reprise effacée pour " + src.getLevel().dimension().identifier() + "."), true);
        return 1;
    }

    private static int begin(CommandSourceStack src, Mode mode) {
        if (job != null) {
            src.sendFailure(Component.literal("Un import est déjà en cours : " + job.statusLine()));
            return 0;
        }
        Job j;
        try {
            j = new Job(src, mode);
        } catch (IOException ex) {
            src.sendFailure(Component.literal("Configuration invalide : " + ex.getMessage()));
            return 0;
        }
        synchronized (AeloriaImport.class) {
            if (job != null) {
                src.sendFailure(Component.literal("Un import est déjà en cours."));
                return 0;
            }
            job = j;
        }
        j.announce();
        return 1;
    }

    @SubscribeEvent
    public static void stopping(ServerStoppingEvent e) {
        Job j = job;
        if (j != null) {
            j.stop = true;
            MMORPG.LOGGER.warn(TAG + "arrêt du serveur pendant l'import : relancer /aeloriaimport reprendra au dernier secteur enregistré.");
            j.finish(e.getServer());
        }
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post e) {
        Job j = job;
        if (j == null) {
            return;
        }
        try {
            j.tick(e.getServer());
        } catch (Throwable t) {
            j.fail(e.getServer(), "erreur inattendue : " + t, t);
        }
    }

    // ------------------------------------------------------------------ utilitaires

    static Path dir(MinecraftServer server) {
        return server.getServerDirectory().resolve("config").resolve("worldedit").resolve("schematics");
    }

    static Path progressFile(MinecraftServer server, ServerLevel level) {
        String key = level.dimension().identifier().toString().replace(':', '_').replace('/', '_');
        return server.getServerDirectory().resolve("config").resolve("mmorpg").resolve("aeloria").resolve("progress_" + key + ".properties");
    }

    static List<AeloriaSector> loadPlan(MinecraftServer server) throws IOException {
        Path csv = server.getServerDirectory().resolve("config").resolve("mmorpg").resolve("aeloria").resolve("importation.csv");
        List<AeloriaSector> all;
        if (Files.isRegularFile(csv)) {
            all = AeloriaSector.parse(csv);
        } else {
            try (InputStream in = AeloriaImport.class.getResourceAsStream("/aeloria/importation.csv")) {
                if (in == null) {
                    throw new IOException("CSV intégré introuvable");
                }
                all = AeloriaSector.parse(in);
            }
        }
        if (all.size() != 100) {
            throw new IOException("le CSV contient " + all.size() + " secteurs au lieu de 100");
        }
        return all;
    }

    /** Sous-ensemble d'essai : fichier config/mmorpg/aeloria/only.txt (un ordre ou un nom de fichier par ligne). */
    static List<AeloriaSector> select(MinecraftServer server, List<AeloriaSector> all) throws IOException {
        Path f = server.getServerDirectory().resolve("config").resolve("mmorpg").resolve("aeloria").resolve("only.txt");
        if (!Files.isRegularFile(f)) {
            return all;
        }
        Set<String> keys = new java.util.HashSet<>();
        for (String line : Files.readAllLines(f, StandardCharsets.UTF_8)) {
            String t = line.trim().toLowerCase(Locale.ROOT).replace(".schem", "");
            if (!t.isEmpty() && !t.startsWith("#")) {
                keys.add(t);
            }
        }
        List<AeloriaSector> sub = new ArrayList<>();
        for (AeloriaSector s : all) {
            if (keys.contains(String.valueOf(s.order())) || keys.contains(s.file().toLowerCase(Locale.ROOT).replace(".schem", ""))) {
                sub.add(s);
            }
        }
        if (sub.isEmpty()) {
            throw new IOException("only.txt ne correspond à aucun secteur");
        }
        return sub;
    }

    private static boolean isAir(String state) {
        return state.startsWith("minecraft:air") || state.startsWith("minecraft:cave_air") || state.startsWith("minecraft:void_air");
    }

    /** Le serveur dédié se met en pause quand il est vide : on l'empêche pendant l'import (l'opérateur peut partir). */
    private static void keepAwake(MinecraftServer server) {
        if (savedPause < 0 && server instanceof net.minecraft.server.dedicated.DedicatedServer ds && ds.pauseWhenEmptySeconds() > 0) {
            savedPause = ds.pauseWhenEmptySeconds();
            ds.setPauseWhenEmptySeconds(0);
            MMORPG.LOGGER.info(TAG + "pause serveur vide désactivée pendant l'import (était {} s)", savedPause);
        }
    }

    private static void release(MinecraftServer server) {
        if (savedPause >= 0 && server instanceof net.minecraft.server.dedicated.DedicatedServer ds) {
            ds.setPauseWhenEmptySeconds(savedPause);
            MMORPG.LOGGER.info(TAG + "pause serveur vide rétablie ({} s)", savedPause);
            savedPause = -1;
        }
    }

    // ------------------------------------------------------------------ import

    enum Mode { PASTE, VERIFY }

    private enum State { PRECHECK, NEXT, LOADING, PASTING, SAVING, DONE }

    /** Données prêtes à coller d'un secteur. */
    private record Loaded(byte[] blocks, BlockState[] palette, byte[] biomeCells, Holder<Biome>[] biomes, int cellsW, int cellsL) {}

    private static final class Job {
        final ServerLevel level;
        final Mode mode;
        final UUID initiator;
        final String initiatorName;
        final List<AeloriaSector> all;
        final List<AeloriaSector> selected;
        final Path progressFile;
        final Set<Integer> done = new TreeSet<>();
        final Set<Integer> verified = new TreeSet<>();
        final Map<String, BlockState> states = new ConcurrentHashMap<>();
        final Map<String, Holder<Biome>> biomes = new ConcurrentHashMap<>();
        final long startNanos = System.nanoTime();
        volatile boolean stop;
        State state = State.PRECHECK;
        String phase = "contrôle des fichiers";
        long lastReport = System.nanoTime();
        long lastSaveCall;

        // contrôle préalable (asynchrone)
        volatile List<String> precheckErrors;
        // lecture d'un secteur (asynchrone)
        volatile Loaded loaded;
        volatile String loadError;

        AeloriaSector cur;
        Loaded data;
        int cx0;
        int cz0;
        int cxCount;
        int czCount;
        int chunkIndex;
        int chunksTotal;
        int chunksDone;
        int sectorsThisRun;
        long placed;
        long sectorMismatch;
        long totalMismatch;
        final List<String> samples = new ArrayList<>();
        // chunk en cours
        LevelChunk chunk;
        int phaseIn; // 0 blocs, 1 biomes
        int x;
        int y;
        int z;
        int sx0;
        int sx1;
        int sz0;
        int sz1;
        int bx;
        int by;
        int bz;
        int bx1;
        int bz1;
        int bx0;
        int bz0;

        Job(CommandSourceStack src, Mode mode) throws IOException {
            this.level = src.getLevel();
            this.mode = mode;
            this.initiator = src.getEntity() instanceof ServerPlayer p ? p.getUUID() : null;
            this.initiatorName = src.getTextName();
            this.all = loadPlan(src.getServer());
            this.selected = select(src.getServer(), all);
            this.progressFile = progressFile(src.getServer(), level);
        }

        void say(MinecraftServer server, String msg) {
            MMORPG.LOGGER.info(TAG + msg);
            if (initiator != null && server != null) {
                ServerPlayer p = server.getPlayerList().getPlayer(initiator);
                if (p != null) {
                    p.sendSystemMessage(Component.literal("§6[Aéloria] §f" + msg));
                }
            }
        }

        MinecraftServer server() {
            return level.getServer();
        }

        void announce() {
            MinecraftServer server = server();
            String what = mode == Mode.PASTE ? "Import" : "Vérification";
            say(server, "§a" + what + " d'Aéloria dans le monde « " + level.dimension().identifier() + " » ("
                    + selected.size() + " secteur(s)). La position du joueur ne sert qu'à choisir le monde : vous pouvez vous déconnecter.");
            if (level.getMinY() > -64 || level.getMaxY() < 320) {
                fail(server, "la dimension va de y=" + level.getMinY() + " à " + (level.getMaxY() - 1) + " ; -64..319 est requis.", null);
                return;
            }
            long maxMb = Runtime.getRuntime().maxMemory() / (1024 * 1024);
            if (maxMb < 2500) {
                say(server, "§eMémoire maximale " + maxMb + " Mo : prévoyez au moins -Xmx4G (un secteur occupe ~200 Mo).");
            }
            loadProgress(server);
            if (mode == Mode.VERIFY && done.isEmpty()) {
                fail(server, "aucun secteur n'est enregistré comme importé dans cette dimension : rien à vérifier.", null);
                return;
            }
            keepAwake(server);
            Thread.ofPlatform().daemon().name("aeloria-precheck").start(this::precheck);
        }

        // ---------------------------------------------------------- reprise

        String fingerprint() {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                for (AeloriaSector s : all) {
                    md.update((s.order() + s.file() + s.x() + "," + s.y() + "," + s.z() + s.sha256()).getBytes(StandardCharsets.UTF_8));
                }
                return HexFormat.of().formatHex(md.digest());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        void loadProgress(MinecraftServer server) {
            done.clear();
            if (!Files.isRegularFile(progressFile)) {
                return;
            }
            try (InputStream in = Files.newInputStream(progressFile)) {
                Properties p = new Properties();
                p.load(in);
                if (!fingerprint().equals(p.getProperty("plan"))) {
                    say(server, "§eLe fichier de reprise ne correspond pas au CSV actuel : ignoré.");
                    return;
                }
                for (String t : p.getProperty("done", "").split(",")) {
                    if (!t.isBlank()) {
                        done.add(Integer.parseInt(t.trim()));
                    }
                }
            } catch (IOException | RuntimeException e) {
                say(server, "§eFichier de reprise illisible, on repart de zéro : " + e.getMessage());
                done.clear();
            }
        }

        void saveProgress() throws IOException {
            Files.createDirectories(progressFile.getParent());
            Properties p = new Properties();
            p.setProperty("dimension", level.dimension().identifier().toString());
            p.setProperty("plan", fingerprint());
            StringBuilder sb = new StringBuilder();
            for (int o : done) {
                sb.append(sb.length() == 0 ? "" : ",").append(o);
            }
            p.setProperty("done", sb.toString());
            Path tmp = progressFile.resolveSibling(progressFile.getFileName() + ".tmp");
            try (OutputStream out = Files.newOutputStream(tmp)) {
                p.store(out, "Aeloria import - secteurs entierement enregistres");
            }
            Files.move(tmp, progressFile, StandardCopyOption.REPLACE_EXISTING);
        }

        // ---------------------------------------------------------- contrôle préalable (thread asynchrone)

        void precheck() {
            Path dir = dir(server());
            List<String> errors = Collections.synchronizedList(new ArrayList<>());
            Set<String> blockNames = ConcurrentHashMap.newKeySet();
            Set<String> biomeNames = ConcurrentHashMap.newKeySet();
            ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() - 1)));
            List<Future<?>> fs = new ArrayList<>();
            for (AeloriaSector s : all) {
                fs.add(pool.submit(() -> checkFile(dir, s, errors, blockNames, biomeNames)));
            }
            try {
                for (Future<?> f : fs) {
                    f.get();
                }
            } catch (Exception e) {
                errors.add("contrôle interrompu : " + e);
            } finally {
                pool.shutdown();
            }
            if (errors.isEmpty()) {
                try {
                    var biomeReg = server().registryAccess().lookupOrThrow(Registries.BIOME);
                    for (String n : blockNames) {
                        if (!isAir(n)) {
                            states.put(n, BlockStateParser.parseForBlock(BuiltInRegistries.BLOCK, n, false).blockState());
                        }
                    }
                    for (String n : biomeNames) {
                        Identifier id = Identifier.tryParse(n);
                        var h = id == null ? java.util.Optional.<Holder.Reference<Biome>>empty() : biomeReg.get(ResourceKey.create(Registries.BIOME, id));
                        if (h.isEmpty()) {
                            errors.add("biome inconnu de ce serveur : " + n);
                        } else {
                            biomes.put(n, h.get());
                        }
                    }
                } catch (Exception e) {
                    errors.add("palette incompatible avec ce serveur : " + e.getMessage());
                }
            }
            List<String> sorted = new ArrayList<>(errors);
            Collections.sort(sorted);
            precheckErrors = sorted;
        }

        void checkFile(Path dir, AeloriaSector s, List<String> errors, Set<String> blockNames, Set<String> biomeNames) {
            Path f = dir.resolve(s.file());
            if (!Files.isRegularFile(f)) {
                errors.add(s.file() + " : fichier absent dans " + dir);
                return;
            }
            try {
                long size = Files.size(f);
                if (size != s.bytes()) {
                    errors.add(s.file() + " : taille " + size + " au lieu de " + s.bytes());
                    return;
                }
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                AeloriaSchematic h;
                try (DigestInputStream in = new DigestInputStream(Files.newInputStream(f), md)) {
                    h = AeloriaSchematic.read(in, true);
                    in.transferTo(OutputStream.nullOutputStream());
                }
                String sha = HexFormat.of().formatHex(md.digest());
                if (!sha.equals(s.sha256())) {
                    errors.add(s.file() + " : SHA-256 " + sha + " au lieu de " + s.sha256());
                    return;
                }
                if (h.dataVersion != EXPECTED_DATA_VERSION) {
                    errors.add(s.file() + " : DataVersion " + h.dataVersion + " au lieu de " + EXPECTED_DATA_VERSION);
                }
                if (h.width != s.width() || h.height != s.height() || h.length != s.length()) {
                    errors.add(s.file() + " : dimensions " + h.width + "x" + h.height + "x" + h.length + " différentes du CSV");
                }
                if (h.offset[0] != 0 || h.offset[1] != 0 || h.offset[2] != 0) {
                    errors.add(s.file() + " : Offset non nul");
                }
                if (s.y() < level.getMinY() || s.y() + s.height() > level.getMaxY()) {
                    errors.add(s.file() + " : dépasse la hauteur de la dimension");
                }
                if (((s.x() | s.y() | s.z()) & 3) != 0) {
                    errors.add(s.file() + " : origine non alignée sur 4 blocs (biomes)");
                }
                if (h.blockPalette.length > 256) {
                    errors.add(s.file() + " : palette de plus de 256 entrées");
                }
                for (String n : h.blockPalette) {
                    if (n == null) {
                        errors.add(s.file() + " : palette de blocs avec trou");
                        return;
                    }
                    blockNames.add(n);
                }
                for (String n : h.biomePalette) {
                    if (n != null) {
                        biomeNames.add(n);
                    }
                }
                if (h.blockEntities > 0) {
                    MMORPG.LOGGER.warn(TAG + "{} : {} blocs-entités ignorés (non pris en charge).", s.file(), h.blockEntities);
                }
            } catch (Exception e) {
                errors.add(s.file() + " : lecture impossible (" + e + ")");
            }
        }

        // ---------------------------------------------------------- lecture d'un secteur (thread asynchrone)

        @SuppressWarnings("unchecked")
        void loadSector(AeloriaSector spec) {
            try {
                AeloriaSchematic s;
                try (InputStream in = Files.newInputStream(dir(server()).resolve(spec.file()))) {
                    s = AeloriaSchematic.read(in, false);
                }
                byte[] blocks = AeloriaSchematic.indices(s.blockRaw, s.volume());
                s.blockRaw = null;
                BlockState[] palette = new BlockState[s.blockPalette.length];
                for (int i = 0; i < palette.length; i++) {
                    if (!isAir(s.blockPalette[i])) {
                        palette[i] = states.get(s.blockPalette[i]);
                        if (palette[i] == null) {
                            throw new IOException("état de bloc non résolu : " + s.blockPalette[i]);
                        }
                    }
                }
                int cw = (s.width + 3) / 4;
                int ch = (s.height + 3) / 4;
                int cl = (s.length + 3) / 4;
                byte[] cells = null;
                Holder<Biome>[] bio = new Holder[0];
                if (s.biomeRaw != null && s.biomePalette.length > 0) {
                    byte[] full = AeloriaSchematic.indices(s.biomeRaw, s.volume());
                    s.biomeRaw = null;
                    cells = new byte[cw * ch * cl];
                    for (int cy = 0; cy < ch; cy++) {
                        for (int cz = 0; cz < cl; cz++) {
                            for (int cx = 0; cx < cw; cx++) {
                                cells[(cy * cl + cz) * cw + cx] = full[((cy * 4) * s.length + cz * 4) * s.width + cx * 4];
                            }
                        }
                    }
                    bio = new Holder[s.biomePalette.length];
                    for (int i = 0; i < bio.length; i++) {
                        bio[i] = s.biomePalette[i] == null ? null : biomes.get(s.biomePalette[i]);
                    }
                } else {
                    MMORPG.LOGGER.warn(TAG + "{} : pas de biomes dans le fichier.", spec.file());
                }
                loaded = new Loaded(blocks, palette, cells, bio, cw, cl);
            } catch (Throwable t) {
                MMORPG.LOGGER.error(TAG + "erreur de lecture de " + spec.file(), t);
                loadError = "lecture de " + spec.file() + " impossible : " + t;
            }
        }

        // ---------------------------------------------------------- tick (thread serveur)

        void tick(MinecraftServer server) throws IOException {
            if (stop && state != State.DONE) {
                say(server, "§eImport arrêté. Relancez /aeloriaimport pour reprendre au dernier secteur enregistré.");
                finish(server);
                return;
            }
            report(server);
            switch (state) {
                case PRECHECK -> {
                    List<String> errs = precheckErrors;
                    if (errs == null) {
                        return;
                    }
                    if (!errs.isEmpty()) {
                        for (String e : errs) {
                            MMORPG.LOGGER.error(TAG + "fichier : {}", e);
                        }
                        fail(server, errs.size() + " erreur(s) de fichier, aucune modification du monde (détail dans les journaux) : "
                                + String.join(" | ", errs.subList(0, Math.min(5, errs.size()))), null);
                        return;
                    }
                    say(server, "§a" + all.size() + " fichiers vérifiés (présence, taille, SHA-256, Sponge v3, DataVersion " + EXPECTED_DATA_VERSION + ", palettes).");
                    state = State.NEXT;
                }
                case NEXT -> next(server);
                case LOADING -> {
                    if (loadError != null) {
                        fail(server, loadError, null);
                    } else if (loaded != null) {
                        data = loaded;
                        loaded = null;
                        startSector();
                        state = State.PASTING;
                        phase = mode == Mode.PASTE ? "collage" : "vérification";
                    }
                }
                case PASTING -> paste(server);
                case SAVING -> save(server);
                case DONE -> { }
            }
        }

        void next(MinecraftServer server) {
            AeloriaSector n = null;
            for (AeloriaSector s : selected) {
                boolean todo = mode == Mode.VERIFY ? done.contains(s.order()) && !verified.contains(s.order()) : !done.contains(s.order());
                if (todo) {
                    n = s;
                    break;
                }
            }
            if (n == null) {
                if (mode == Mode.PASTE) {
                    say(server, "§aImport terminé : " + selected.size() + " secteur(s) enregistrés dans « " + level.dimension().identifier() + " ».");
                } else {
                    say(server, "§aVérification terminée : " + totalMismatch + " différence(s) sur " + verified.size() + " secteur(s).");
                }
                finish(server);
                return;
            }
            cur = n;
            chunksDone = 0;
            chunksTotal = 0;
            loaded = null;
            loadError = null;
            phase = "lecture du secteur";
            state = State.LOADING;
            AeloriaSector spec = n;
            Thread.ofPlatform().daemon().name("aeloria-read").start(() -> loadSector(spec));
        }

        void startSector() {
            cx0 = Math.floorDiv(cur.x(), 16);
            cz0 = Math.floorDiv(cur.z(), 16);
            cxCount = Math.floorDiv(cur.x() + cur.width() - 1, 16) - cx0 + 1;
            czCount = Math.floorDiv(cur.z() + cur.length() - 1, 16) - cz0 + 1;
            chunksTotal = cxCount * czCount;
            chunkIndex = 0;
            chunksDone = 0;
            chunk = null;
            sectorMismatch = 0;
            samples.clear();
        }

        void openChunk() {
            int cx = cx0 + chunkIndex % cxCount;
            int cz = cz0 + chunkIndex / cxCount;
            chunkIndex++;
            chunk = level.getChunk(cx, cz);
            sx0 = Math.max(0, cx * 16 - cur.x());
            sx1 = Math.min(cur.width() - 1, cx * 16 + 15 - cur.x());
            sz0 = Math.max(0, cz * 16 - cur.z());
            sz1 = Math.min(cur.length() - 1, cz * 16 + 15 - cur.z());
            phaseIn = 0;
            x = sx0;
            y = 0;
            z = sz0;
            bx0 = sx0 / 4;
            bx1 = sx1 / 4;
            bz0 = sz0 / 4;
            bz1 = sz1 / 4;
            bx = bx0;
            by = 0;
            bz = bz0;
        }

        void paste(MinecraftServer server) {
            long deadline = System.nanoTime() + budgetMs * 1_000_000L;
            while (System.nanoTime() < deadline) {
                if (chunk == null) {
                    if (chunkIndex >= chunksTotal) {
                        sectorDone(server);
                        return;
                    }
                    openChunk();
                }
                if (!slice(deadline)) {
                    return;
                }
                if (mode == Mode.PASTE) {
                    chunk.markUnsaved();
                }
                chunk = null;
                chunksDone++;
            }
        }

        /** Travaille sur le chunk courant ; false si le budget est épuisé. */
        boolean slice(long deadline) {
            AeloriaSector s = cur;
            byte[] b = data.blocks();
            BlockState[] pal = data.palette();
            int w = s.width();
            int l = s.length();
            int h = s.height();
            var pos = new BlockPos.MutableBlockPos();
            int n = 0;
            if (phaseIn == 0) {
                while (y < h) {
                    while (z <= sz1) {
                        int base = (y * l + z) * w;
                        while (x <= sx1) {
                            int idx = b[base + x] & 0xFF;
                            if (idx >= pal.length) {
                                throw new IllegalStateException("indice de palette " + idx + " hors palette");
                            }
                            BlockState st = pal[idx];
                            if (st != null) {
                                pos.set(s.x() + x, s.y() + y, s.z() + z);
                                if (mode == Mode.PASTE) {
                                    chunk.setBlockState(pos, st, 0);
                                    placed++;
                                } else if (chunk.getBlockState(pos) != st) {
                                    mismatch("bloc " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " : " + chunk.getBlockState(pos) + " au lieu de " + st);
                                }
                            }
                            x++;
                            if ((++n & 127) == 0 && System.nanoTime() > deadline) {
                                return false;
                            }
                        }
                        x = sx0;
                        z++;
                    }
                    z = sz0;
                    y++;
                }
                phaseIn = 1;
            }
            if (data.biomeCells() != null) {
                int cellsH = (h + 3) / 4;
                while (by < cellsH) {
                    while (bz <= bz1) {
                        while (bx <= bx1) {
                            int pi = data.biomeCells()[(by * data.cellsL() + bz) * data.cellsW() + bx] & 0xFF;
                            Holder<Biome> bio = pi < data.biomes().length ? data.biomes()[pi] : null;
                            if (bio != null) {
                                int wx = s.x() + bx * 4;
                                int wy = s.y() + by * 4;
                                int wz = s.z() + bz * 4;
                                setOrCheckBiome(wx, wy, wz, bio);
                            }
                            bx++;
                            if ((++n & 63) == 0 && System.nanoTime() > deadline) {
                                return false;
                            }
                        }
                        bx = bx0;
                        bz++;
                    }
                    bz = bz0;
                    by++;
                }
            }
            return true;
        }

        void setOrCheckBiome(int wx, int wy, int wz, Holder<Biome> bio) {
            int si = chunk.getSectionIndex(wy);
            LevelChunkSection sec = chunk.getSection(si);
            int lx = (wx >> 2) & 3;
            int ly = (wy >> 2) & 3;
            int lz = (wz >> 2) & 3;
            @SuppressWarnings("unchecked")
            var container = (PalettedContainer<Holder<Biome>>) sec.getBiomes();
            if (mode == Mode.PASTE) {
                container.getAndSetUnchecked(lx, ly, lz, bio);
            } else if (container.get(lx, ly, lz) != bio) {
                mismatch("biome " + wx + " " + wy + " " + wz + " : " + container.get(lx, ly, lz).getRegisteredName() + " au lieu de " + bio.getRegisteredName());
            }
        }

        void mismatch(String what) {
            sectorMismatch++;
            if (samples.size() < 5) {
                samples.add(what);
            }
        }

        void sectorDone(MinecraftServer server) {
            data = null;
            if (mode == Mode.VERIFY) {
                totalMismatch += sectorMismatch;
                verified.add(cur.order());
                sectorsThisRun++;
                say(server, "Secteur " + cur.order() + " (" + cur.file() + ") : " + sectorMismatch + " différence(s)."
                        + (samples.isEmpty() ? "" : " Exemples : " + String.join(" ; ", samples)));
                state = State.NEXT;
                return;
            }
            phase = "sauvegarde du monde";
            state = State.SAVING;
        }

        void save(MinecraftServer server) throws IOException {
            server.saveAllChunks(true, true, false);
            done.add(cur.order());
            saveProgress();
            sectorsThisRun++;
            say(server, "Secteur " + cur.order() + "/" + all.size() + " (" + cur.file() + ") enregistré.");
            state = State.NEXT;
        }

        // ---------------------------------------------------------- suivi

        String statusLine() {
            AeloriaSector s = cur;
            int total = selected.size();
            long doneSel = selected.stream().filter(x -> mode == Mode.VERIFY ? verified.contains(x.order()) : done.contains(x.order())).count();
            double frac = chunksTotal == 0 ? 0 : (double) chunksDone / chunksTotal;
            double pct = total == 0 ? 0 : 100.0 * (doneSel + frac) / total;
            String eta = "";
            double elapsed = (System.nanoTime() - startNanos) / 1e9;
            double runDone = sectorsThisRun + frac;
            if (runDone > 0.02 && state != State.PRECHECK) {
                double remaining = (total - doneSel - frac) * (elapsed / runDone);
                eta = String.format(" — reste ~%d min", Math.max(0, Math.round(remaining / 60)));
            }
            return String.format("%s « %s » : %s%s, chunk %d/%d — %d/%d secteurs, %.1f %%%s",
                    mode == Mode.PASTE ? "Import" : "Vérification", level.dimension().identifier(), phase,
                    s == null ? "" : " [" + s.file() + "]", chunksDone, chunksTotal, doneSel, total, pct, eta);
        }

        void report(MinecraftServer server) {
            long now = System.nanoTime();
            if (now - lastReport > 15_000_000_000L) {
                lastReport = now;
                say(server, statusLine());
            }
        }

        void fail(MinecraftServer server, String message, Throwable t) {
            if (t != null) {
                MMORPG.LOGGER.error(TAG + message, t);
            }
            say(server, "§cÉchec : " + message + (mode == Mode.PASTE ? " Relancez /aeloriaimport après correction : la reprise se fait au dernier secteur enregistré." : ""));
            finish(server);
        }

        void finish(MinecraftServer server) {
            state = State.DONE;
            stop = true;
            synchronized (AeloriaImport.class) {
                if (job == this) {
                    job = null;
                }
            }
            release(server);
        }
    }
}
