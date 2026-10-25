package com.mmorpg.network;

import com.mmorpg.MMORPG;
import com.mmorpg.server.PartyManager;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import java.util.*;

/** Persistent per-player return tickets; group membership is kept in SharedParties, not copied from stale player snapshots. */
public final class DungeonTravel {
    public record ReturnPoint(NetworkConfig.Role role, String dimension, double x, double y, double z, float yaw, float pitch) {
        public CompoundTag save() {
            var t = new CompoundTag(); t.putString("role", role.name()); t.putString("dimension", dimension);
            t.putDouble("x", x); t.putDouble("y", y); t.putDouble("z", z); t.putFloat("yaw", yaw); t.putFloat("pitch", pitch); return t;
        }
        public static ReturnPoint read(CompoundTag tag) {
            var role = NetworkConfig.Role.valueOf(tag.getStringOr("role", ""));
            if (role == NetworkConfig.Role.STANDALONE || role == NetworkConfig.Role.DUNGEONS) throw new IllegalArgumentException("Invalid dungeon return server");
            String dimension = tag.getStringOr("dimension", ""); Identifier.parse(dimension);
            double x = tag.getDoubleOr("x", Double.NaN), y = tag.getDoubleOr("y", Double.NaN), z = tag.getDoubleOr("z", Double.NaN);
            float yaw = tag.getFloatOr("yaw", 0), pitch = tag.getFloatOr("pitch", 0);
            if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || !Float.isFinite(yaw) || !Float.isFinite(pitch) || Math.abs(x) > 30_000_000 || Math.abs(z) > 30_000_000) throw new IllegalArgumentException("Invalid return coordinates");
            return new ReturnPoint(role, dimension, x, y, z, yaw, pitch);
        }
    }
    private static final Map<UUID, ReturnPoint> RETURNS = new HashMap<>();
    public static ReturnPoint point(ServerPlayer p) { return RETURNS.get(p.getUUID()); }
    public static void restore(ServerPlayer p, ReturnPoint point) {
        if (point == null) RETURNS.remove(p.getUUID()); else RETURNS.put(p.getUUID(), point);
    }
    public static void forget(ServerPlayer p) { RETURNS.remove(p.getUUID()); }
    public static void clear() { RETURNS.clear(); }
    public static void capture(ServerPlayer p) {
        restore(p, new ReturnPoint(NetworkConfig.current.role, p.level().dimension().identifier().toString(), p.getX(), p.getY(), p.getZ(), p.getYRot(), p.getXRot()));
    }
    public static boolean enterRemote(ServerPlayer player, String pool) {
        if (NetworkConfig.hosts(NetworkConfig.Role.DUNGEONS)) return false;
        if (NetworkPlayers.inTransit(player)) return true;
        try { SharedParties.refresh(); }
        catch (Exception e) { player.sendSystemMessage(Component.literal("Le groupe ne peut pas être vérifié. Réessayez dans un instant.")); return true; }
        var type = com.mmorpg.dungeon.InstanceManager.typeOf(pool);
        int required = com.mmorpg.dungeon.DungeonMaster.requiredLevel(type, com.mmorpg.dungeon.InstanceManager.difficultyOf(pool));
        List<ServerPlayer> members = new ArrayList<>(PartyManager.onlineMembers(player));
        if (!members.contains(player)) members.add(player);
        members.removeIf(p -> p != player && (p.level() != player.level() || p.distanceToSqr(player) > 24 * 24));
        for (ServerPlayer p : members) {
            if (RpgPlayers.get(p).level < required || !NetworkPlayers.canTransfer(p, NetworkConfig.Role.DUNGEONS)) {
                player.sendSystemMessage(Component.literal("Départ annulé : " + p.getName().getString() + " n'est pas prêt ou n'a pas le niveau " + required + ".")); return true;
            }
        }
        // Prepare every return ticket before issuing any proxy connection request.
        Map<UUID, ReturnPoint> old = new HashMap<>();
        for (ServerPlayer p : members) { old.put(p.getUUID(), point(p)); capture(p); }
        for (ServerPlayer p : members) if (!NetworkPlayers.save(p)) {
            for (ServerPlayer member : members) { restore(member, old.get(member.getUUID())); NetworkPlayers.save(member); }
            player.sendSystemMessage(Component.literal("Départ annulé : sauvegarde d'un membre impossible.")); return true;
        }
        for (ServerPlayer p : members) NetworkPlayers.route(p, NetworkConfig.Role.DUNGEONS, "dungeon:" + pool);
        return true;
    }
    public static boolean returnToSource(ServerPlayer p) {
        if (!NetworkConfig.current.enabled || NetworkConfig.current.role != NetworkConfig.Role.DUNGEONS) return false;
        ReturnPoint point = point(p);
        NetworkPlayers.route(p, point == null ? NetworkConfig.Role.MAIN : point.role(), point == null ? "spawn" : "dungeon-return");
        return true;
    }
    public static void arriveHome(ServerPlayer p) {
        ReturnPoint point = point(p);
        if (point == null || point.role() != NetworkConfig.current.role) { fallback(p); return; }
        var key = ResourceKey.create(Registries.DIMENSION, Identifier.parse(point.dimension()));
        var level = p.level().getServer().getLevel(key);
        if (level == null || level.isOutsideBuildHeight(net.minecraft.core.BlockPos.containing(point.x(), point.y(), point.z())) || !level.getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(point.x(), point.y(), point.z()))) {
            fallback(p); return;
        }
        level.getChunkAt(net.minecraft.core.BlockPos.containing(point.x(), point.y(), point.z()));
        var box = p.getBoundingBox().move(point.x() - p.getX(), point.y() - p.getY(), point.z() - p.getZ());
        var ground = net.minecraft.core.BlockPos.containing(point.x(), point.y() - .1, point.z());
        if (!level.noCollision(p, box) || level.getBlockState(ground).getCollisionShape(level, ground).isEmpty()) {
            p.sendSystemMessage(Component.literal("Le point de retour est obstrué : retour au spawn."));
            fallback(p); return;
        }
        if (!p.teleportTo(level, point.x(), point.y(), point.z(), Set.of(), point.yaw(), point.pitch(), true)) {
            p.sendSystemMessage(Component.literal("Retour au portail annulé : votre point de retour est conservé.")); return;
        }
        p.resetFallDistance(); forget(p);
        p.sendOverlayMessage(Component.literal("Retour à votre portail d'entrée.").withColor(0x80E0A0));
    }
    private static void fallback(ServerPlayer p) { forget(p); com.mmorpg.hub.HubManager.goToSpawn(p); }
}
