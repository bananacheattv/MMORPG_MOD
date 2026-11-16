package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.network.NetworkConfig;
import com.mmorpg.world.AdventurePortalData.Portal;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import java.util.Set;

/**
 * Point devant le portail d'aventure franchi (donjon, mine, plaines, monde ouvert) : le joueur y revient en sortant ou apres une mort.
 * Stocke dans PlayerData (suit donc les transferts entre serveurs Velocity) ; sans point connu, les reperes historiques (spawn) servent.
 */
public final class PortalOrigin {
    private PortalOrigin() { }

    public record Origin(NetworkConfig.Role role, String dimension, double x, double y, double z, float yaw) {
        CompoundTag save() {
            var t = new CompoundTag(); t.putString("role", role.name()); t.putString("dim", dimension);
            t.putDouble("x", x); t.putDouble("y", y); t.putDouble("z", z); t.putFloat("yaw", yaw); return t;
        }
        static Origin read(CompoundTag t) {
            try {
                var role = NetworkConfig.Role.valueOf(t.getStringOr("role", ""));
                String dim = t.getStringOr("dim", ""); Identifier.parse(dim);
                double x = t.getDoubleOr("x", Double.NaN), y = t.getDoubleOr("y", Double.NaN), z = t.getDoubleOr("z", Double.NaN);
                float yaw = t.getFloatOr("yaw", 0);
                if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z) || !Float.isFinite(yaw) || Math.abs(x) > 30_000_000 || Math.abs(z) > 30_000_000) return null;
                return new Origin(role, dim, x, y, z, yaw);
            } catch (RuntimeException e) { return null; }
        }
    }

    public static Origin get(ServerPlayer p) {
        CompoundTag t = RpgPlayers.get(p).portalOrigin;
        return t == null ? null : Origin.read(t);
    }
    public static void forget(ServerPlayer p) {
        var d = RpgPlayers.get(p);
        if (d.portalOrigin != null) { d.portalOrigin = null; d.dirty = true; }
    }
    private static void set(ServerPlayer p, Origin o) { var d = RpgPlayers.get(p); d.portalOrigin = o.save(); d.dirty = true; }

    /** Position 2,5 blocs devant le portail, du cote par lequel le joueur est entre, face a l'exterieur. */
    public static Origin front(ServerPlayer p, Portal portal) {
        Vec3 at = p.position();
        float yaw = p.getYRot();
        if (!portal.block()) {
            BlockPos b = portal.pos();
            double cx = b.getX() + .5, cz = b.getZ() + .5;
            if (portal.widthX()) {
                double s = at.z >= cz ? 1 : -1;
                at = new Vec3(at.x, b.getY(), cz + s * 2.5); yaw = s > 0 ? 0F : 180F;
            } else {
                double s = at.x >= cx ? 1 : -1;
                at = new Vec3(cx + s * 2.5, b.getY(), at.z); yaw = s > 0 ? -90F : 90F;
            }
        }
        return new Origin(NetworkConfig.current.role, p.level().dimension().identifier().toString(), at.x, at.y, at.z, yaw);
    }

    /** A l'entree d'un portail d'aventure (pas hub/spawn). */
    public static void capture(ServerPlayer p, Portal portal) {
        if (portal.kind().equals("hub") || portal.kind().equals("spawn")) return;
        set(p, front(p, portal));
    }

    /** Le joueur est-il dans une zone d'aventure (mines, plaines, donjons, monde ouvert) ? */
    public static boolean inAdventure(ServerPlayer p) {
        var dim = p.level().dimension();
        return dim.equals(com.mmorpg.dungeon.InstanceManager.DIM) || dim.equals(Level.OVERWORLD);
    }

    /** Position utilisable pour les points de retour d'instance du serveur courant (ou {@code null}). */
    public static Origin local(ServerPlayer p) {
        Origin o = get(p);
        return o != null && o.role() == NetworkConfig.current.role ? o : null;
    }

    /** Joueurs morts en zone d'aventure (le respawn vanilla les place ailleurs : la zone doit etre retenue a la mort). */
    private static final Set<java.util.UUID> DIED = java.util.concurrent.ConcurrentHashMap.newKeySet();
    public static void markDeath(ServerPlayer p) { if (inAdventure(p) && get(p) != null) DIED.add(p.getUUID()); else DIED.remove(p.getUUID()); }
    public static void clearDeath(ServerPlayer p) { DIED.remove(p.getUUID()); }
    /** Un retour devant le portail apres la mort est-il prevu ? */
    public static boolean available(ServerPlayer p) { return DIED.contains(p.getUUID()) && get(p) != null; }
    /** Apres la mort (au respawn) : retour devant le portail ; false = repli habituel. */
    public static boolean afterDeath(ServerPlayer p) {
        if (!DIED.remove(p.getUUID())) return false;
        return exit(p, true);
    }

    /**
     * Sortie de zone ou mort : retour devant le portail d'entree.
     * @return true si le retour est fait ou en cours (transfert) ; false : aucun point connu, l'appelant utilise son repli (spawn)
     */
    public static boolean exit(ServerPlayer p) { return exit(p, false); }
    private static boolean exit(ServerPlayer p, boolean afterDeath) {
        Origin o = get(p);
        if (o == null || (!afterDeath && !inAdventure(p))) return false;
        if (NetworkConfig.current.enabled && o.role() != NetworkConfig.current.role) {
            return com.mmorpg.network.NetworkPlayers.route(p, o.role(), "portal-return");
        }
        if (arrive(p, o)) return true;
        forget(p);
        return false;
    }

    /** Arrivee apres transfert « portal-return » : retour local, sinon spawn. */
    public static void arrive(ServerPlayer p) {
        Origin o = get(p);
        if (o == null || o.role() != NetworkConfig.current.role || !arrive(p, o)) { forget(p); com.mmorpg.hub.HubManager.goToSpawn(p); }
    }

    private static boolean arrive(ServerPlayer p, Origin o) {
        var key = ResourceKey.create(Registries.DIMENSION, Identifier.parse(o.dimension()));
        var level = p.level().getServer().getLevel(key);
        BlockPos at = BlockPos.containing(o.x(), o.y(), o.z());
        if (level == null || level.isOutsideBuildHeight(at) || !level.getWorldBorder().isWithinBounds(at)) return false;
        level.getChunkAt(at);
        p.stopRiding();
        var box = p.getBoundingBox().move(o.x() - p.getX(), o.y() - p.getY(), o.z() - p.getZ());
        var ground = BlockPos.containing(o.x(), o.y() - .1, o.z());
        if (!level.noCollision(p, box) || level.getBlockState(ground).getCollisionShape(level, ground).isEmpty()) {
            p.sendSystemMessage(Component.literal("Le point devant le portail est obstrué : retour au spawn.").withColor(0xFFC060));
            return false;
        }
        if (!p.teleportTo(level, o.x(), o.y(), o.z(), Set.of(), o.yaw(), 0F, true)) return false;
        p.resetFallDistance();
        forget(p);
        p.sendOverlayMessage(Component.literal("Retour devant votre portail.").withColor(0x80E0A0));
        MMORPG.LOGGER.debug("[MMORPG] Retour devant le portail pour {}", p.getUUID());
        return true;
    }
}
