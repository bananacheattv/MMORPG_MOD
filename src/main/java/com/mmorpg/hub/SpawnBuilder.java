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
    public static final int LAYOUT_VERSION = 1;

    /** Volume du spawn (protection, nettoyage). */
    public static AABB bounds(BlockPos o) {
        return new AABB(o.getX() + HubMarket.CX - HubMarket.OUTER - 6, o.getY() - 40, o.getZ() - HubMarket.OUTER - 6,
                o.getX() + HubMarket.CX + HubMarket.OUTER + 6, o.getY() + 90, o.getZ() + HubMarket.OUTER + 6);
    }

    /** Construit le spawn s'il manque ou s'il date d'un ancien plan. @return vrai si une construction a eu lieu */
    public static boolean ensure(MinecraftServer server) {
        SpawnData data = SpawnData.get(server);
        ServerLevel level = server.getLevel(DIM);
        if (level == null) return false;
        if (data.exists() && data.layout() >= LAYOUT_VERSION) return false;
        MMORPG.LOGGER.info("[MMORPG] Construction du Spawn d'Aeloria (dimension {})", DIM.identifier());
        build(level, data.exists() ? data.origin() : HubManager.HUB_ORIGIN);
        return true;
    }

    public static BlockPos build(ServerLevel level, BlockPos feet) {
        HubCtx c = new HubCtx(level, feet.immutable());
        for (Entity e : level.getEntities((Entity) null, bounds(c.O), x -> x instanceof com.mmorpg.entity.NpcEntity || x instanceof Display.TextDisplay)) {
            if (e instanceof Display.TextDisplay && !e.entityTags().contains(HubBuilder.TAG)) continue;
            e.discard();
        }
        HubBuilder.computeMasks(c, HubMarket::footprint);
        HubBuilder.terrain(c);
        HubBuilder.textIndex = 0;
        HubMarket.build(c);
        HubMarket.texts(c);
        HubMarket.entities(c);
        zones(c);
        SpawnData.get(level.getServer()).set(c.O, LAYOUT_VERSION);
        return c.O;
    }

    private static void zones(HubCtx c) {
        var data = com.mmorpg.zone.ZoneData.get(c.L.getServer());
        int ox = c.O.getX();
        int oz = c.O.getZ();
        data.add(new com.mmorpg.zone.ZoneData.Zone("spawn_marche", "Marché d'Aeloria", c.L.dimension(), ox + HubMarket.CX - HubMarket.OUTER - 2, oz - HubMarket.OUTER - 2,
                ox + HubMarket.CX + HubMarket.OUTER + 2, oz + HubMarket.OUTER + 2, false, false, false, com.mmorpg.zone.ZoneData.Zone.STYLE_ZONE,
                "Banque, hôtel des ventes, forges et marchands"));
    }
}
