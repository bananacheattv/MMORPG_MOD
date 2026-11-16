package com.mmorpg.quest;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.mmorpg.MMORPG;
import com.mmorpg.entity.NpcEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * PNJ donneurs de quetes du spawn (30) et du monde ouvert (200), un PNJ par quete, decrits dans
 * {@code data/mmorpg/lore/world_npcs.json} (produit par {@code tools/quetes/build_world_npcs.py}). Les PNJ sont poses a la demande
 * (joueur a moins de 64 blocs, sol charge), une seule fois : l'identifiant fixe evite tout doublon au redemarrage.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class WorldNpcs {
    public static final String RESOURCE = "/data/mmorpg/lore/world_npcs.json";
    public static final String GROUP = "pnj_monde";
    /** Distance (blocs) au joueur sous laquelle un PNJ manquant est pose. */
    private static final double RANGE = 64;

    public record Entry(String id, String name, int skin, String quest, String dialogue, String dim, String region, int dx, int dz) {
        public UUID uuid() {
            return UUID.nameUUIDFromBytes(("aeloria:world-npc:" + id).getBytes(StandardCharsets.UTF_8));
        }
    }

    private static List<Entry> entries;
    private static Map<String, int[]> anchors;

    private WorldNpcs() {
    }

    public static synchronized List<Entry> entries() {
        if (entries == null) load();
        return entries;
    }

    /** Ancre (x, z) d'une region du monde ouvert. */
    public static int[] anchor(String region) {
        entries();
        return anchors.getOrDefault(region, new int[]{0, 0});
    }

    private static void load() {
        var list = new ArrayList<Entry>();
        var map = new java.util.HashMap<String, int[]>();
        try (Reader r = new InputStreamReader(WorldNpcs.class.getResourceAsStream(RESOURCE), StandardCharsets.UTF_8)) {
            JsonObject root = new Gson().fromJson(r, JsonObject.class);
            root.getAsJsonObject("regions").entrySet().forEach(e -> map.put(e.getKey(), new int[]{e.getValue().getAsJsonArray().get(0).getAsInt(), e.getValue().getAsJsonArray().get(1).getAsInt()}));
            for (var e : root.getAsJsonArray("npcs")) {
                JsonObject o = e.getAsJsonObject();
                list.add(new Entry(o.get("id").getAsString(), o.get("name").getAsString(), o.get("skin").getAsInt(), o.get("quest").getAsString(),
                        o.get("dialogue").getAsString(), o.get("dim").getAsString(), o.get("region").getAsString(), o.get("dx").getAsInt(), o.get("dz").getAsInt()));
            }
        } catch (Exception ex) {
            throw new IllegalStateException("PNJ du monde illisibles : " + ex, ex);
        }
        anchors = map;
        entries = list;
    }

    /** Lie chaque quete a son PNJ (identifiant fixe) : seul ce PNJ la propose. */
    static void assign(Map<String, QuestDef> quests) {
        for (var e : entries()) {
            var q = quests.get(e.quest());
            if (q != null) q.npc = e.uuid().toString();
        }
    }

    // ------------------------------------------------------------------ pose

    private static BlockPos ground(ServerLevel l, int x, int z, int baseY, boolean heightmap) {
        if (!l.hasChunkAt(new BlockPos(x, 64, z))) return null;
        int y;
        if (heightmap) y = l.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        else {
            y = Integer.MIN_VALUE;
            for (int dy = 0; dy <= 24 && y == Integer.MIN_VALUE; dy++)
                for (int sign = dy == 0 ? 1 : -1; sign <= 1; sign += 2) {
                    var p = new BlockPos(x, baseY + dy * sign, z);
                    if (l.isEmptyBlock(p) && l.isEmptyBlock(p.above()) && l.getBlockState(p.below()).isSolidRender()) { y = p.getY(); break; }
                }
            if (y == Integer.MIN_VALUE) return null;
        }
        var p = new BlockPos(x, y, z);
        if (!l.getFluidState(p).isEmpty() || !l.getFluidState(p.below()).isEmpty() || !l.getBlockState(p.below()).isSolidRender()) return null;
        return p;
    }

    /** Pose les PNJ manquants autour du joueur ; renvoie le nombre pose. */
    public static int install(ServerPlayer player) {
        var l = player.level();
        boolean spawn = l.dimension().equals(com.mmorpg.hub.SpawnBuilder.DIM);
        boolean open = l.dimension().equals(net.minecraft.world.level.Level.OVERWORLD) && com.mmorpg.network.NetworkConfig.hosts(com.mmorpg.network.NetworkConfig.Role.OPENWORLD);
        if (!spawn && !open) return 0;
        if (spawn && !com.mmorpg.network.NetworkConfig.hosts(com.mmorpg.network.NetworkConfig.Role.MAIN)) return 0;
        if (com.mmorpg.server.NpcPurge.active(l.getServer())) return 0;
        var directory = com.mmorpg.world.NpcDirectory.get(l.getServer());
        int cx = 0, cz = 0, baseY = 64;
        if (spawn) {
            var city = com.mmorpg.hub.SpawnData.get(l.getServer()).origin();
            var data = com.mmorpg.hub.SpawnData.get(l.getServer());
            if (city == null || !data.exists()) return 0;
            cx = city.getX() + com.mmorpg.hub.HubMarket.CX;
            cz = city.getZ();
            baseY = (int) Math.round(data.arrival().y);
        }
        int placed = 0;
        for (var e : entries()) {
            if (spawn != e.dim().equals("spawn")) continue;
            int x = spawn ? cx + e.dx() : anchor(e.region())[0] + e.dx();
            int z = spawn ? cz + e.dz() : anchor(e.region())[1] + e.dz();
            double dx = player.getX() - x, dz = player.getZ() - z;
            if (dx * dx + dz * dz > RANGE * RANGE) continue;
            if (l.getEntity(e.uuid()) != null || directory.entry(e.uuid().toString()) != null) continue;
            BlockPos at = null;
            for (int r = 0; r <= (spawn ? 12 : 96) && at == null; r += 4)
                for (int a = 0; a < (r == 0 ? 1 : 12) && at == null; a++)
                    at = ground(l, x + (int) Math.round(Math.cos(a * Math.PI / 6) * r), z + (int) Math.round(Math.sin(a * Math.PI / 6) * r), baseY, !spawn);
            if (at == null || !l.getEntitiesOfClass(NpcEntity.class, new net.minecraft.world.phys.AABB(at).inflate(2.5)).isEmpty()) continue;
            var n = com.mmorpg.registry.ModEntities.NPC.get().create(l, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            if (n == null) continue;
            n.setUUID(e.uuid());
            n.setup(NpcEntity.Role.QUETES, e.name(), e.skin(), GROUP);
            n.setDialogue(e.dialogue());
            n.setZone(e.region());
            n.snapTo(at.getX() + .5, at.getY(), at.getZ() + .5, (float) (Math.atan2(cz - at.getZ(), cx - at.getX()) * 180 / Math.PI) - 90, 0);
            n.setPersistenceRequired();
            if (l.addFreshEntity(n)) placed++;
        }
        return placed;
    }

    @SubscribeEvent
    public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 100 != 40) return;
        for (var p : e.getServer().getPlayerList().getPlayers()) if (p.isAlive()) install(p);
    }
}
