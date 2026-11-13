package com.mmorpg.client;

import com.mmorpg.entity.NpcEntity;
import com.mmorpg.network.Payloads;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Etat client du confort de jeu : cache de carte, points d'interet, compteur de degats, pings de groupe. */
public final class QolClient {
    /** Point d'interet : type (couleur et lettre), nom, position. */
    public record Poi(String kind, String name, int x, int y, int z) {
    }

    public record Ping(String name, int x, int y, int z, long expires) {
    }

    public static final class Dps {
        public float dps;
        public long total;
        public long seen;
    }

    private static final Map<String, it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<int[]>> CHUNKS = new HashMap<>();
    /** Augmente a chaque changement reel de la carte : les images de carte ne sont recalculees que dans ce cas. */
    private static long mapVersion;
    private static final Map<String, Map<String, Poi>> KNOWN = new HashMap<>();
    private static final Map<Long, Long> SCANNED = new HashMap<>();
    public static final Map<String, Dps> DPS = new LinkedHashMap<>();
    public static final List<Ping> PINGS = new ArrayList<>();
    public static final List<Poi> LIVE = new ArrayList<>();
    private static String lastDim = "";

    private QolClient() {
    }

    public static boolean streamer() {
        return ClientConfig.STREAMER_MODE.get();
    }

    // ------------------------------------------------------------------ reception

    public static void receive(CompoundTag t) {
        Minecraft mc = Minecraft.getInstance();
        String kind = t.getStringOr("kind", "");
        long now = System.currentTimeMillis();
        if (kind.equals("world")) {
            role = t.getStringOr("role", "SOLO");
        } else if (kind.equals("dps")) {
            Dps d = DPS.computeIfAbsent(t.getStringOr("name", "?"), k -> new Dps());
            d.dps = t.getFloatOr("dps", 0);
            d.total = t.getLongOr("total", 0);
            d.seen = now;
        } else if (kind.equals("ping")) {
            PINGS.add(new Ping(t.getStringOr("name", "?"), t.getIntOr("x", 0), t.getIntOr("y", 0), t.getIntOr("z", 0), now + 8000));
            if (mc.player != null) mc.player.playSound(net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 0.6f, 1.6f);
        }
    }

    /** A la deconnexion : le prochain serveur annoncera son role. */
    public static void reset() {
        role = "";
    }

    public static void prune() {
        long now = System.currentTimeMillis();
        PINGS.removeIf(p -> p.expires < now);
        DPS.values().removeIf(d -> now - d.seen > 15000);
    }

    /** Envoie un ping la ou le joueur regarde. */
    public static void sendPing() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        HitResult hit = mc.player.pick(120, 1f, false);
        BlockPos pos;
        if (hit instanceof BlockHitResult b && hit.getType() == HitResult.Type.BLOCK) pos = b.getBlockPos().above();
        else {
            var look = mc.player.getLookAngle().scale(40).add(mc.player.getEyePosition());
            pos = BlockPos.containing(look);
        }
        CompoundTag t = new CompoundTag();
        t.putString("op", "ping");
        t.putInt("x", pos.getX());
        t.putInt("y", pos.getY());
        t.putInt("z", pos.getZ());
        ClientNet.send(new Payloads.QolAction(t));
    }

    public static void action(String op) {
        CompoundTag t = new CompoundTag();
        t.putString("op", op);
        ClientNet.send(new Payloads.QolAction(t));
    }

    // ------------------------------------------------------------------ carte

    /** Role du serveur courant (HUB, MAIN, MINES...), annonce par le serveur ; sert a separer les cartes. */
    private static String role = "";
    private static Level lastLevel;
    private static long levelSince;

    public static String dim() {
        Minecraft mc = Minecraft.getInstance();
        return mc.level == null ? "" : role + "|" + mc.level.dimension().identifier();
    }

    /** Nom lisible de la carte courante (selon le serveur et la dimension). */
    public static String mapName() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return "Carte du monde";
        String d = mc.level.dimension().identifier().toString();
        return switch (d) {
            case "mmorpg:hub" -> "Hub des classes";
            case "mmorpg:spawn" -> "Spawn";
            case "mmorpg:donjons" -> "Donjons";
            case "mmorpg:competitions" -> "Arènes";
            case "mmorpg:guildes" -> "Guildes";
            case "minecraft:overworld" -> switch (role) {
                case "MINES" -> "Mines";
                case "PLAINS" -> "Plaines";
                case "MAIN" -> "Monde d'Aeloria";
                default -> "Monde ouvert";
            };
            default -> d;
        };
    }

    /** Couleur ARGB de la colonne (x, z) connue du cache, 0 si inexplore. */
    public static int colorAt(Map<Long, int[]> map, int x, int z) {
        int[] c = map.get(ChunkPos.pack(x >> 4, z >> 4));
        return c == null ? 0 : c[(z & 15) * 16 + (x & 15)];
    }

    public static Map<Long, int[]> chunks() {
        return chunkMap();
    }

    /** Carte de la dimension courante, indexee par position de chunk compactee (sans objets Long). */
    public static it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<int[]> chunkMap() {
        String dim = dim();
        if (chunkMapOf == null || !dim.equals(chunkMapDim)) {
            chunkMapDim = dim;
            chunkMapOf = CHUNKS.computeIfAbsent(dim, k -> new it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<>());
        }
        return chunkMapOf;
    }

    private static String chunkMapDim = "";
    private static it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap<int[]> chunkMapOf;

    public static long mapVersion() {
        return mapVersion;
    }

    public static Map<String, Poi> known() {
        return KNOWN.computeIfAbsent(dim(), k -> new LinkedHashMap<>());
    }

    /** Appele a chaque tick client : releve les chunks voisins et les points d'interet visibles. */
    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        long time = ++ticks;   // compteur propre : l'heure du monde peut etre figee ou resynchronisee par le serveur
        String dim = dim();
        if (mc.level != lastLevel) {   // nouveau niveau (changement de serveur/dimension) : attendre l'annonce
            lastLevel = mc.level;
            role = "";
            levelSince = 0;
        }
        if (role.isEmpty()) {   // serveur pas encore annonce : attendre 3 s puis charger quand meme (serveur ancien ou annonce perdue)
            if (levelSince == 0) levelSince = time;
            if (time - levelSince < 60) return;
            role = "SOLO";
        }

        if (!dim.equals(lastDim)) {
            lastDim = dim;
            SCANNED.clear();
            PINGS.clear();
        }
        if (time % 5 == 0) scan(mc, 3, time);
        if (time % 20 == 0) pois(mc);
        if (time % 40 == 0) prune();
    }

    private static long ticks;

    /** Releve immediat (ouverture de la carte) : remplit la zone autour du joueur sans attendre. */
    public static void scanNow() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        if (role.isEmpty()) role = "SOLO";
        scan(mc, 60, ticks);
    }

    private static void scan(Minecraft mc, int budget, long time) {
        Level level = mc.level;
        int pcx = mc.player.blockPosition().getX() >> 4, pcz = mc.player.blockPosition().getZ() >> 4;
        var map = chunkMap();
        for (int r = 0; r <= 8 && budget > 0; r++) {
            for (int dx = -r; dx <= r && budget > 0; dx++) {
                for (int dz = -r; dz <= r && budget > 0; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) != r) continue;
                    long key = ChunkPos.pack(pcx + dx, pcz + dz);
                    Long last = SCANNED.get(key);
                    if (last != null && time - last < (r <= 1 ? 100 : 600)) continue;
                    LevelChunk chunk = level.getChunkSource().getChunkNow(pcx + dx, pcz + dz);
                    if (chunk == null) continue;
                    int[] colors = scanChunk(level, pcx + dx, pcz + dz);
                    if (!java.util.Arrays.equals(colors, map.put(key, colors))) mapVersion++;
                    SCANNED.put(key, time);
                    budget--;
                }
            }
        }
    }

    private static int[] scanChunk(Level level, int cx, int cz) {
        int[] out = new int[256];
        BlockPos.MutableBlockPos mp = new BlockPos.MutableBlockPos();
        int minY = level.getMinY();
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int x = cx * 16 + lx, z = cz * 16 + lz;
                int y = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z) - 1;
                MapColor color = MapColor.NONE;
                while (y >= minY) {
                    BlockState s = level.getBlockState(mp.set(x, y, z));
                    color = s.getMapColor(level, mp);
                    if (color != MapColor.NONE) break;
                    y--;
                }
                if (color == MapColor.NONE) continue;
                int yn = level.getHeight(Heightmap.Types.WORLD_SURFACE, x, z - 1) - 1;
                float k = color == MapColor.WATER ? 0.78f : y > yn ? 1.0f : y < yn ? 0.78f : 0.88f;
                int r = Math.min(255, (int) (((color.col >> 16) & 255) * k)), g = Math.min(255, (int) (((color.col >> 8) & 255) * k)), b = Math.min(255, (int) ((color.col & 255) * k));
                out[lz * 16 + lx] = 0xFF000000 | r << 16 | g << 8 | b;
            }
        }
        return out;
    }

    private static void pois(Minecraft mc) {
        LIVE.clear();
        Map<String, Poi> known = known();
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof NpcEntity npc) {
                String kind = switch (npc.role()) {
                    case QUETES -> "quest";
                    case MARCHAND, BOUTIQUE -> "shop";
                    case DONJON -> "dungeon";
                    case MINES -> "mine";
                    case SORTIE -> "exit";
                    case PVP -> "arena";
                    default -> null;
                };
                if (kind == null) continue;
                BlockPos p = npc.blockPosition();
                Poi poi = new Poi(kind, npc.displayName(), p.getX(), p.getY(), p.getZ());
                known.put(kind + ":" + p.getX() / 4 + ":" + p.getZ() / 4, poi);
            } else if (e instanceof com.mmorpg.entity.boss.RpgBoss b && b.isAlive()) {
                BlockPos p = b.blockPosition();
                LIVE.add(new Poi("boss", b.getName().getString(), p.getX(), p.getY(), p.getZ()));
            } else if (e instanceof net.minecraft.world.entity.player.Player pl && pl != mc.player && inParty(pl.getName().getString())) {
                BlockPos p = pl.blockPosition();
                LIVE.add(new Poi("party", streamer() ? "Allié" : pl.getName().getString(), p.getX(), p.getY(), p.getZ()));
            }
        }
    }

    private static boolean inParty(String name) {
        var members = ClientData.party.getListOrEmpty("members");
        for (int i = 0; i < members.size(); i++) if (members.getCompoundOrEmpty(i).getStringOr("name", "").equals(name)) return true;
        return false;
    }

    public static int poiColor(String kind) {
        return switch (kind) {
            case "quest" -> 0xFFFFD040;
            case "shop" -> 0xFF6AE06A;
            case "dungeon" -> 0xFFB070FF;
            case "mine" -> 0xFFB0B0B8;
            case "exit" -> 0xFF50D8E8;
            case "arena" -> 0xFFFF8040;
            case "boss" -> 0xFFFF4040;
            case "party" -> 0xFF62A8FF;
            default -> 0xFFFFFFFF;
        };
    }

    public static String poiLabel(String kind) {
        return switch (kind) {
            case "quest" -> "Quêtes";
            case "shop" -> "Marchand";
            case "dungeon" -> "Donjon";
            case "mine" -> "Mines";
            case "exit" -> "Passeur";
            case "arena" -> "Arène";
            case "boss" -> "Boss";
            case "party" -> "Groupe";
            default -> kind;
        };
    }

    /** Tous les points d'interet a afficher : decouverts puis en direct. */
    public static List<Poi> allPois() {
        List<Poi> list = new ArrayList<>(known().values());
        list.addAll(LIVE);
        return list;
    }

    /** Vrai si au moins un point d'interet existe (evite de construire une liste a chaque image). */
    public static boolean hasPois() {
        return !LIVE.isEmpty() || !known().isEmpty();
    }
}
