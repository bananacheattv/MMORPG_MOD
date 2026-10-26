package com.mmorpg.hub;

import com.mmorpg.MMORPG;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Spawn d'Aeloria : le Marche (place pavee, couronne de maisons, banque, hotel des ventes, forges) construit dans sa propre dimension
 * {@code mmorpg:spawn}, separee du hub (la Grande Nef). Les deux mondes sont relies par un portail de chaque cote.
 */
public final class SpawnBuilder {
    private SpawnBuilder() {
    }

    public static final ResourceKey<Level> DIM = ResourceKey.create(Registries.DIMENSION, MMORPG.id("spawn"));
    /** Version du plan du spawn : un spawn plus ancien est reconstruit au demarrage. */
    public static final int LAYOUT_VERSION = 3;

    /** Volume du spawn (protection, nettoyage). */
    public static AABB bounds(BlockPos o) {
        return new AABB(o.getX() + HubMarket.CX - SpawnCity.R - 14, o.getY() - 40, o.getZ() - SpawnCity.R - 14,
                o.getX() + HubMarket.CX + SpawnCity.R + 14, o.getY() + 110, o.getZ() + SpawnCity.R + 14);
    }

    /** Construit le spawn s'il manque ou s'il date d'un ancien plan. @return vrai si une construction a eu lieu */
    public static boolean ensure(MinecraftServer server) {
        SpawnData data = SpawnData.get(server);
        ServerLevel level = server.getLevel(DIM);
        if (level == null) return false;
        if (com.mmorpg.config.ConfigManager.spawn().manualBuild) {
            if (data.exists() && data.layout() == Integer.MAX_VALUE) return false;
            var portals = com.mmorpg.world.AdventurePortalData.get(server);
            for (var portal : java.util.List.copyOf(portals.all())) if (portal.dimension().equals(DIM)) portals.remove(portal.id());
            var waypoints = com.mmorpg.world.WaypointData.get(server);
            for (var waypoint : java.util.List.copyOf(waypoints.all())) if (waypoint.dimension.equals(DIM)) waypoints.remove(waypoint.id);
            var directory = com.mmorpg.world.NpcDirectory.get(server);
            for (var entry : directory.snapshot()) if (entry instanceof net.minecraft.nbt.CompoundTag tag && tag.getStringOr("dimension", "").equals(DIM.identifier().toString())) directory.remove(tag.getStringOr("id", ""));
            var zones = com.mmorpg.zone.ZoneData.get(server);
            for (var zone : zones.all()) if (zone.dimension().equals(DIM)) zones.remove(zone.id());
            BlockPos at = new BlockPos(0, 96, 0);
            data.set(at, Integer.MAX_VALUE);
            return true;
        }
        if (data.exists() && data.layout() >= LAYOUT_VERSION) return false;
        MMORPG.LOGGER.info("[MMORPG] Construction du Spawn d'Aeloria (dimension {})", DIM.identifier());
        build(level, data.exists() ? data.origin() : HubManager.HUB_ORIGIN);
        return true;
    }

    public static BlockPos build(ServerLevel level, BlockPos feet) {
        if (com.mmorpg.config.ConfigManager.spawn().manualBuild) return feet;
        HubCtx c = new HubCtx(level, feet.immutable());
        boolean npcs = com.mmorpg.config.ConfigManager.spawn().autoNpcs;
        var market = HubMarket.marketNpcIds();
        for (Entity e : level.getEntities((Entity) null, bounds(c.O), x -> x instanceof com.mmorpg.entity.NpcEntity || x instanceof Display.TextDisplay)) {
            if (e instanceof Display.TextDisplay && !e.entityTags().contains(HubBuilder.TAG)) continue;
            if (e instanceof com.mmorpg.entity.NpcEntity && (!npcs || !market.contains(e.getUUID()))) continue;     // les PNJ poses a la main restent
            e.discard();
        }
        HubBuilder.computeMasks(c, SpawnCity::footprint);
        c.thin = true;
        HubBuilder.terrain(c);
        SpawnCity.ground(c);
        HubBuilder.textIndex = 0;
        HubMarket.build(c);
        SpawnCity.build(c);
        HubMarket.texts(c);
        SpawnCity.texts(c);
        HubMarket.entities(c);
        zones(c);
        SpawnData.get(level.getServer()).set(c.O, LAYOUT_VERSION);
        return c.O;
    }

    private static void zones(HubCtx c) {
        var data = com.mmorpg.zone.ZoneData.get(c.L.getServer());
        int ox = c.O.getX();
        int oz = c.O.getZ();
        data.add(new com.mmorpg.zone.ZoneData.Zone("spawn_marche", "Cité d'Aeloria", c.L.dimension(), ox + HubMarket.CX - SpawnCity.R - 6, oz - SpawnCity.R - 6,
                ox + HubMarket.CX + SpawnCity.R + 6, oz + SpawnCity.R + 6, false, false, false, com.mmorpg.zone.ZoneData.Zone.STYLE_ZONE,
                "Château, port, marché, banque, forges et marchands"));
    }
}
