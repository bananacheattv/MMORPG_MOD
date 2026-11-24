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
 * PNJ donneurs de quetes du spawn (30, un par quete) et du monde ouvert (72, chacun porte plusieurs quetes), decrits dans
 * {@code data/mmorpg/lore/world_npcs.json} (produit par {@code tools/quetes/build_world_npcs.py}). Les PNJ sont poses a la demande
 * (joueur a moins de 64 blocs, sol charge), une seule fois : l'identifiant fixe evite tout doublon au redemarrage.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class WorldNpcs {
    public static final String RESOURCE = WorldNpcData.RESOURCE;
    public static final String GROUP = "pnj_monde";
    /** Distance (blocs) au joueur sous laquelle un PNJ manquant est pose. */
    private static final double RANGE = 64;

    public static List<WorldNpcData.Entry> entries() {
        return WorldNpcData.entries();
    }

    public static int[] anchor(String region) {
        return WorldNpcData.anchor(region);
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

    private static java.lang.ref.WeakReference<net.minecraft.server.MinecraftServer> purged;

    /** Une fois par demarrage : retire les PNJ de quetes d'une ancienne disposition (annuaire et entites chargees). */
    private static void purgeStale(ServerLevel l, com.mmorpg.world.NpcDirectory directory) {
        var server = l.getServer();
        if (purged != null && purged.get() == server) return;
        purged = new java.lang.ref.WeakReference<>(server);
        var valid = WorldNpcData.valid();
        for (var t : directory.snapshot()) {
            if (!(t instanceof net.minecraft.nbt.CompoundTag n) || !GROUP.equals(n.getStringOr("group", ""))) continue;
            String id = n.getStringOr("id", "");
            try {
                var uuid = UUID.fromString(id);
                if (valid.contains(uuid)) continue;
                for (var level : server.getAllLevels()) { var old = level.getEntity(uuid); if (old != null) old.discard(); }
            } catch (IllegalArgumentException ignored) { }
            directory.remove(id);
        }
    }

    /** Migration : un PNJ du monde ouvert pose a l'ancien emplacement (carte precedente) est retire pour etre repose au bon endroit. */
    private static void retire(ServerLevel l, com.mmorpg.world.NpcDirectory directory, UUID id, int x, int z) {
        var live = l.getEntity(id);
        if (live != null && (live.getX() - x) * (live.getX() - x) + (live.getZ() - z) * (live.getZ() - z) > 12100) {
            live.discard();
            directory.remove(id.toString());
            return;
        }
        var entry = directory.entry(id.toString());
        if (entry != null && live == null) {
            var at = BlockPos.of(entry.getLongOr("pos", 0));
            if ((at.getX() - x) * (long) (at.getX() - x) + (at.getZ() - z) * (long) (at.getZ() - z) > 12100) directory.remove(id.toString());
        }
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
        purgeStale(l, directory);
        int placed = 0;
        for (var e : entries()) {
            if (spawn != e.dim().equals("spawn")) continue;
            int x = spawn ? cx + e.dx() : anchor(e.region())[0] + e.dx();
            int z = spawn ? cz + e.dz() : anchor(e.region())[1] + e.dz();
            double dx = player.getX() - x, dz = player.getZ() - z;
            if (dx * dx + dz * dz > RANGE * RANGE) continue;
            if (!spawn) retire(l, directory, e.uuid(), x, z);
            if (l.getEntity(e.uuid()) != null || directory.entry(e.uuid().toString()) != null) continue;
            if (!l.hasChunkAt(new BlockPos(x, 64, z))) continue;   // sol pas encore charge : attendre, ne pas poser a cote
            BlockPos at = null;
            for (int r = 0; r <= (spawn ? 12 : 8) && at == null; r += 4)
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

    /** Un ancien PNJ du monde ouvert (carte precedente) qui se recharge a son ancien emplacement est refuse : le PNJ du bon endroit le remplace. */
    @SubscribeEvent
    public static void joined(net.neoforged.neoforge.event.entity.EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide() || !(e.getEntity() instanceof NpcEntity n) || !GROUP.equals(n.group())) return;
        if (!WorldNpcData.valid().contains(n.getUUID())) { e.setCanceled(true); return; }
        for (var en : entries()) {
            if (!en.dim().equals("openworld") || !en.uuid().equals(n.getUUID())) continue;
            int x = anchor(en.region())[0] + en.dx(), z = anchor(en.region())[1] + en.dz();
            if ((n.getX() - x) * (n.getX() - x) + (n.getZ() - z) * (n.getZ() - z) > 12100) e.setCanceled(true);
            return;
        }
    }

    @SubscribeEvent
    public static void tick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 100 != 40) return;
        for (var p : e.getServer().getPlayerList().getPlayers()) if (p.isAlive()) install(p);
    }
}
