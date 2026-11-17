package com.mmorpg.zone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mmorpg.MMORPG;
import com.mmorpg.entity.RpgMob;
import com.mmorpg.server.MobSpawner;
import com.mmorpg.world.LevelZones;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

/**
 * Zones de chasse et de recolte de l'openworld, definies dans {@code config/mmorpg/zones_chasse.json} (ou en jeu avec
 * {@code /zone chasse ...}) : une boite dans une dimension, des monstres qui y apparaissent autour des joueurs (niveaux par cellules,
 * comme les anciennes plaines et mines), et l'option de recolte des cultures du mod. Plus rien n'est code en dur : les anciennes
 * instances Mines et Plaines n'existent plus, on pose ses propres lieux et on les declare ici.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class HuntZones {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String DOC = "Zones de chasse/recolte. Une zone = une boite (x1,y1,z1 a x2,y2,z2) dans une dimension. 'monstres' : cles de monstres "
            + "(ex. gobelin, loup_sombre ; voir /mmorpg monstres). Les niveaux vont de niveauMin a niveauMax par cellules de 'cellule' blocs. "
            + "'max' : monstres RPG maximum autour d'un joueur. 'surRayon' : aucun monstre a moins de ce nombre de blocs du centre (zone sure). "
            + "'recolte' : les cultures du mod y repoussent comme des veines. Rechargement a chaud : /zone chasse recharger.";

    /** Une zone de chasse (champs publics : lus et ecrits tels quels dans le JSON). */
    public static final class Zone {
        public String id = "";
        public String nom = "";
        public String dimension = "minecraft:overworld";
        public int x1, y1 = -64, z1, x2, y2 = 320, z2;
        public int niveauMin = 1, niveauMax = 10, cellule = 48;
        public List<String> monstres = new ArrayList<>();
        public int max = 24;
        public int surRayon = 0;
        public boolean recolte = false;
        public boolean actif = true;

        public AABB box() {
            return new AABB(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2), Math.max(x1, x2) + 1, Math.max(y1, y2) + 1, Math.max(z1, z2) + 1);
        }

        public Vec3 center() {
            return box().getCenter();
        }

        public boolean contains(Level level, double x, double y, double z) {
            return level.dimension().identifier().toString().equals(dimension) && box().contains(x, y, z);
        }

        public String title() {
            return nom.isBlank() ? id : nom;
        }
    }

    private static volatile List<Zone> zones = List.of();

    private HuntZones() {
    }

    public static List<Zone> all() {
        return zones;
    }

    public static Optional<Zone> get(String id) {
        return zones.stream().filter(z -> z.id.equalsIgnoreCase(id)).findFirst();
    }

    public static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("mmorpg").resolve("zones_chasse.json");
    }

    // ================================================================== lecture / ecriture

    /** Recharge le fichier (le cree vide avec sa documentation s'il n'existe pas). @return nombre de zones */
    public static synchronized int reload() {
        Path f = file();
        List<Zone> out = new ArrayList<>();
        try {
            if (!Files.exists(f)) {
                zones = List.of();
                save();
                return 0;
            }
            try (Reader r = Files.newBufferedReader(f)) {
                JsonObject root = GSON.fromJson(r, JsonObject.class);
                JsonArray arr = root != null && root.has("zones") ? root.getAsJsonArray("zones") : new JsonArray();
                arr.forEach(e -> {
                    Zone z = GSON.fromJson(e, Zone.class);
                    if (z != null && z.id != null && !z.id.isBlank()) out.add(z);
                });
            }
        } catch (Exception e) {
            MMORPG.LOGGER.error("[MMORPG] zones_chasse.json illisible, on garde les zones precedentes : {}", e.toString());
            return zones.size();
        }
        zones = List.copyOf(out);
        MMORPG.LOGGER.info("[MMORPG] {} zone(s) de chasse chargee(s)", out.size());
        return out.size();
    }

    public static synchronized void save() throws IOException {
        Path f = file();
        Files.createDirectories(f.getParent());
        JsonObject root = new JsonObject();
        root.addProperty("_doc", DOC);
        root.add("zones", GSON.toJsonTree(zones));
        Path tmp = Files.createTempFile(f.getParent(), "zones_chasse-", ".tmp");
        try (Writer w = Files.newBufferedWriter(tmp)) {
            GSON.toJson(root, w);
        }
        try {
            Files.move(tmp, f, java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
        } catch (java.nio.file.AtomicMoveNotSupportedException e) {
            Files.move(tmp, f, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static synchronized void put(Zone z) throws IOException {
        List<Zone> n = new ArrayList<>(zones);
        n.removeIf(o -> o.id.equalsIgnoreCase(z.id));
        n.add(z);
        zones = List.copyOf(n);
        save();
    }

    public static synchronized boolean remove(String id) throws IOException {
        List<Zone> n = new ArrayList<>(zones);
        boolean removed = n.removeIf(o -> o.id.equalsIgnoreCase(id));
        zones = List.copyOf(n);
        if (removed) save();
        return removed;
    }

    // ================================================================== requetes

    public static Optional<Zone> at(Level level, double x, double y, double z) {
        for (Zone zone : zones) if (zone.actif && zone.contains(level, x, y, z)) return Optional.of(zone);
        return Optional.empty();
    }

    public static boolean inZone(net.minecraft.world.entity.Entity e) {
        return at(e.level(), e.getX(), e.getY(), e.getZ()).isPresent();
    }

    /** Vrai si la position est dans une zone qui active la recolte des cultures. */
    public static boolean cropZone(Level level, BlockPos pos) {
        for (Zone zone : zones) if (zone.recolte && zone.contains(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) return true;
        return false;
    }

    /** Zone (n'importe quelle dimension) dont les monstres contiennent une des cles voulues ; {@code "*"} = toutes. */
    public static List<Zone> forMobs(java.util.Collection<String> wanted) {
        List<Zone> out = new ArrayList<>();
        for (Zone zone : zones) if (zone.actif && (wanted.contains("*") || zone.monstres.stream().anyMatch(wanted::contains))) out.add(zone);
        return out;
    }

    // ================================================================== monstres

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        reload();
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (zones.isEmpty() || event.getServer().getTickCount() % 40 != 0) return;
        MinecraftServer server = event.getServer();
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isSpectator() || !p.isAlive() || com.mmorpg.network.NetworkPlayers.portalBlocked(p)) continue;
            Optional<Zone> zone = at(p.level(), p.getX(), p.getY(), p.getZ());
            if (zone.isPresent() && !zone.get().monstres.isEmpty()) spawn((ServerLevel) p.level(), p, zone.get());
        }
    }

    private static void spawn(ServerLevel level, ServerPlayer p, Zone z) {
        if (z.surRayon > 0 && Math.hypot(p.getX() - z.center().x, p.getZ() - z.center().z) < z.surRayon) return;
        AABB area = p.getBoundingBox().inflate(36, 30, 36);
        int near = level.getEntitiesOfClass(Mob.class, area, m -> m instanceof RpgMob).size();
        var random = level.getRandom();
        long seed = z.id.hashCode();
        for (int i = 0; i < 6 && near < z.max; i++) {
            double a = random.nextDouble() * Math.PI * 2, dist = 14 + random.nextDouble() * 20;
            int x = (int) (p.getX() + Math.cos(a) * dist), zz = (int) (p.getZ() + Math.sin(a) * dist);
            if (!z.box().contains(x + 0.5, p.getY(), zz + 0.5)) continue;
            if (z.surRayon > 0 && Math.hypot(x - z.center().x, zz - z.center().z) < z.surRayon) continue;
            if (!level.hasChunkAt(new BlockPos(x, 64, zz))) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, zz);
            if (Math.abs(y - p.getY()) > 24) y = (int) p.getY();                      // grottes : on reste a hauteur du joueur
            BlockPos pos = new BlockPos(x, y, zz);
            for (int k = 0; k < 12 && !(level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir() && !level.getBlockState(pos.below()).isAir()); k++) {
                pos = pos.below();
            }
            if (!level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir() || level.getBlockState(pos.below()).isAir()) continue;
            if (!level.getFluidState(pos.below()).isEmpty()) continue;
            String key = z.monstres.get(random.nextInt(z.monstres.size()));
            int lvl = LevelZones.level(seed, x, zz, Math.max(8, z.cellule), z.niveauMin, Math.max(z.niveauMin, z.niveauMax), new Random(random.nextLong()));
            if (MobSpawner.spawnForCommand(level, key, pos, lvl) != null) near++;
        }
    }
}
