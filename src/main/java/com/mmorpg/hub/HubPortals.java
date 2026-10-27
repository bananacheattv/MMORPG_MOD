package com.mmorpg.hub;

import com.mmorpg.MMORPG;
import com.mmorpg.world.WaypointData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Portails entre les deux mondes : le hub (Grande Nef, dimension mmorpg:hub) et le spawn (Marche, dimension mmorpg:spawn).
 * Un portail de marbre et de cristal a chaque bout, au voile violet ; s'en approcher teleporte dans l'autre monde.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class HubPortals {
    private HubPortals() {
    }

    /** Portail du hub : au bout du pont qui part de la place vers l'ouest (coordonnees relatives a l'origine du hub). */
    public static final int HUB_GATE_X = -47;
    private static final Map<UUID, Long> COOLDOWN = new HashMap<>();

    // ------------------------------------------------------------------ nettoyage de l'ancien marche et construction du portail

    /** Un hub des plans 2 a 4 portait le marche a l'ouest de la place : on le retire (le marche vit maintenant dans la dimension du spawn). */
    static void clearLegacyMarket(HubCtx c) {
        HubData hub = HubData.get(c.L.getServer());
        if (!hub.exists() || hub.layout() < 2 || hub.layout() >= HubBuilder.LAYOUT_VERSION) return;
        MMORPG.LOGGER.info("[MMORPG] Hub de plan {} : retrait de l'ancien marche (desormais dans la dimension du spawn)", hub.layout());
        BlockPos o = c.O;
        var air = Blocks.AIR.defaultBlockState();
        for (int x = -150; x <= -33; x++) {
            for (int z = -62; z <= 62; z++) {
                for (int y = -44; y <= 60; y++) c.L.setBlock(o.offset(x, y, z), air, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE | Block.UPDATE_SUPPRESS_DROPS);
            }
        }
        WaypointData.get(c.L.getServer()).removeAt(c.L.dimension(), o.offset(HubMarket.CX + 25, 0, 0));
    }

    /** Pont de l'ouest et portail du spawn : arche de marbre, piliers de cristal, voile violet et panneau. */
    static void spawnGate(HubCtx c) {
        int gx = HUB_GATE_X;
        for (int x = -50; x <= -28; x++) {                                          // pont depuis la place
            for (int z = -7; z <= 7; z++) {
                int az = Math.abs(z);
                c.set(x, -2, z, az <= 3 ? c.state(Blocks.STONE_BRICKS) : c.state(Blocks.DIRT));
                c.set(x, -1, z, az <= 1 ? c.state("or_ouvrage") : az <= 3 ? c.mix(x, 0, z, 91, Blocks.POLISHED_BLACKSTONE, 60, Blocks.DEEPSLATE_TILES, 40) : Blocks.GRASS_BLOCK.defaultBlockState());
                for (int y = 0; y <= 14; y++) c.set(x, y, z, Blocks.AIR);
                if (az >= 4 && c.rnd(x, 0, z, 92) < 0.25F) c.set(x, 0, z, Blocks.SHORT_GRASS);
            }
        }
        for (int x = -46; x <= -34; x += 6) for (int s : new int[]{-4, 4}) HubBuilder.lamp(c, x, s);
        for (int z = -5; z <= 5; z += 10) {
            for (int y = 0; y <= 11; y++) c.set(gx, y, z, y % 4 == 0 ? c.state("or_ouvrage") : c.axis("colonne_marbre", Direction.Axis.Y));
            c.set(gx, 12, z, "marbre_dore");
            c.set(gx, 13, z, "cristal_arcane");
            c.set(gx, 14, z, Blocks.END_ROD);
            for (int dx : new int[]{-1, 1}) for (int y = 0; y <= 1; y++) c.set(gx + dx, y, z, "marbre_poli");
        }
        for (int z = -6; z <= 6; z++) {
            c.set(gx, 12, z, c.state("or_ouvrage"));
            c.set(gx, 13, z, c.state(Blocks.POLISHED_BLACKSTONE_BRICKS));
            c.set(gx, 14, z, c.state("marbre_poli"));
        }
        for (int z = -4; z <= 4; z++) {
            for (int y = 1; y <= 11; y++) {
                boolean arch = Math.abs(z) <= 4 - Math.max(0, y - 8);
                c.set(gx, y, z, arch ? Blocks.STAINED_GLASS.pick(DyeColor.PURPLE).defaultBlockState() : c.state("or_ouvrage"));
            }
        }
        HubBuilder.text(c, gx - 1, 17, 0, "PORTAIL DU SPAWN", "#E0B0FF", 4.4F);
        HubBuilder.text(c, gx - 1, 14.6, 0, "Marché d'Aeloria", "#F0E8D8", 2.4F);
    }

    // ------------------------------------------------------------------ declenchement

    private static AABB hubGate(BlockPos o) {
        return new AABB(o.getX() + HUB_GATE_X - 3, o.getY(), o.getZ() - 4, o.getX() + HUB_GATE_X + 4, o.getY() + 12, o.getZ() + 5);
    }

    private static AABB spawnGate(BlockPos o) {
        return new AABB(o.getX() + HubMarket.CX + 34, o.getY(), o.getZ() - 4, o.getX() + HubMarket.CX + 41, o.getY() + 12, o.getZ() + 5);
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        if (!com.mmorpg.network.NetworkConfig.town()) return;
        var server = event.getServer();
        if (server.getTickCount() % 200 == 100) HubBuilder.cleanLegacyMarket(server);
        if (server.getTickCount() % 8 != 0) return;
        HubData hub = HubData.get(server);
        SpawnData spawn = SpawnData.get(server);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isSpectator() || p.isPassenger() || com.mmorpg.network.NetworkPlayers.portalBlocked(p)) continue;
            ServerLevel level = p.level();
            boolean inHub = hub.exists() && level.dimension().equals(hub.dimension());
            boolean inSpawn = spawn.exists() && level.dimension().equals(SpawnBuilder.DIM);
            if (inSpawn && com.mmorpg.config.ConfigManager.spawn().manualBuild) continue;
            if (!inHub && !inSpawn) continue;
            AABB gate = inHub ? hubGate(hub.origin()) : spawnGate(spawn.origin());
            double planeX = inHub ? hub.origin().getX() + HUB_GATE_X + 0.5 : spawn.origin().getX() + HubMarket.CX + 37.5;
            double baseY = inHub ? hub.origin().getY() : spawn.origin().getY();
            double midZ = inHub ? hub.origin().getZ() + 0.5 : spawn.origin().getZ() + 0.5;
            if (p.distanceToSqr(planeX, baseY + 5, midZ) < 30 * 30) {
                level.sendParticles(ParticleTypes.PORTAL, planeX, baseY + 5, midZ, 10, 0.4, 4, 3, 0.1);
            }
            if (!gate.contains(p.getX(), p.getY(), p.getZ())) continue;
            long now = server.getTickCount();
            Long last = COOLDOWN.get(p.getUUID());
            if (last != null && now - last < 60) continue;
            COOLDOWN.put(p.getUUID(), now);
            level.playSound(null, p.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.4f, 1.3f);
            boolean ok = inHub ? HubManager.goToSpawn(p) : HubManager.goToHubFromSpawn(p);
            if (!ok) p.sendOverlayMessage(Component.literal("Le portail est encore en cours de construction.").withColor(0xFFC060));
        }
    }

    static Entity unused() {
        return null;
    }
}
