package com.mmorpg.plains;

import com.mmorpg.MMORPG;
import com.mmorpg.dungeon.InstanceManager;
import com.mmorpg.hub.HubManager;
import com.mmorpg.hub.HubMarket;
import com.mmorpg.hub.SpawnBuilder;
import com.mmorpg.hub.SpawnData;
import com.mmorpg.plains.PlainsLayout.Sector;
import com.mmorpg.server.MobSpawner;
import com.mmorpg.zone.ZoneData;
import com.mmorpg.zone.ZoneManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import static com.mmorpg.plains.PlainsLayout.*;

/**
 * Les Plaines d'Aeloria : de grandes etendues a ciel ouvert (dimension des donjons) pour farmer les monstres sans descendre dans la mine.
 * Six regions se suivent du nord au sud avec des monstres de plus en plus forts ; ils reapparaissent autour des joueurs. On y entre par le
 * portail vert du Spawn (ou {@code /plaines}) et on en sort par l'arche de la place d'entree (ou {@code /spawn}).
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class PlainsManager {
    /** A incrementer quand le plan change : les plaines sont alors reconstruites au demarrage. */
    public static final int VERSION = 1;
    private static final int MOB_CAP = 16;
    /** Portail du Spawn : colonne locale (relative a l'origine du spawn) du voile vert, face a l'est. */
    public static final int SPAWN_GATE_X = HubMarket.CX - 24, SPAWN_GATE_Z = -18;

    private static PlainsBuilder job;
    private static long jobStart;
    private static int lastLogged = -1;
    private static final Map<UUID, Long> COOLDOWN = new HashMap<>();

    private PlainsManager() {
    }

    public static ServerLevel dim(MinecraftServer server) {
        return server.getLevel(InstanceManager.DIM);
    }

    public static boolean inPlains(Entity e) {
        return e.level().dimension().equals(InstanceManager.DIM) && e.getX() >= X0 - 4 && e.getX() <= X0 + W + 4 && e.getZ() >= Z0 - 4 && e.getZ() <= Z0 + L + 4;
    }

    public static boolean built(MinecraftServer server) {
        return PlainsData.get(server).version() == VERSION;
    }

    public static boolean building() {
        return job != null;
    }

    public static int percent() {
        return job == null ? 100 : job.percent();
    }

    // ================================================================== construction et zones

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        MinecraftServer server = event.getServer();
        job = null;
        if (!built(server)) start(server);
        else registerZones(server);
    }

    /** Lance la construction des plaines (quelques minutes). */
    public static boolean start(MinecraftServer server) {
        ServerLevel level = dim(server);
        if (level == null || job != null) return false;
        PlainsData.get(server).setVersion(0);
        job = new PlainsBuilder(level);
        jobStart = System.nanoTime();
        lastLogged = -1;
        MMORPG.LOGGER.info("[MMORPG] Construction des Plaines d'Aeloria : {} x {} blocs (quelques minutes)", W, L);
        return true;
    }

    /** Une zone par region plus la place d'entree ; on n'y construit pas et on ne s'y bat pas entre joueurs, mais les monstres y vivent. */
    public static void registerZones(MinecraftServer server) {
        ZoneData zones = ZoneData.get(server);
        zones.add(new ZoneData.Zone("plaines_entree", "Place des Plaines", InstanceManager.DIM, X0, Z0, X0 + W - 1, Z0 + ENTRY - 1,
                true, true, true, ZoneData.Zone.STYLE_ZONE, "Le portail de retour vers le Spawn d'Aeloria"));
        for (Sector s : STYLES) {
            zones.add(new ZoneData.Zone(s.id(), s.name(), InstanceManager.DIM, X0, Z0 + sectorStart(s.index()), X0 + W - 1, Z0 + sectorEnd(s.index()),
                    true, true, false, ZoneData.Zone.STYLE_DUNGEON, s.lore() + "|Plaines d'Aeloria · niveaux " + s.minLevel() + " à " + s.maxLevel()));
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (job != null) {
            if (job.step(22_000_000L)) {
                PlainsData.get(server).setVersion(VERSION);
                registerZones(server);
                MMORPG.LOGGER.info("[MMORPG] Plaines d'Aeloria construites : {} blocs en {} s", job.blocksPlaced(), (System.nanoTime() - jobStart) / 1_000_000_000L);
                job = null;
            } else if (job.percent() / 10 != lastLogged) {
                lastLogged = job.percent() / 10;
                MMORPG.LOGGER.info("[MMORPG] Plaines d'Aeloria : {} %", job.percent());
            }
            return;
        }
        if (server.getTickCount() % 8 == 0) portals(server);
        ServerLevel level = dim(server);
        if (level == null || level.players().isEmpty() || !built(server)) return;
        if (server.getTickCount() % 40 == 0) spawnMobs(level);
    }

    // ================================================================== portails et arrivee

    private static AABB spawnGate(BlockPos o) {
        double x = o.getX() + SPAWN_GATE_X;
        double z = o.getZ() + SPAWN_GATE_Z;
        return new AABB(x - 0.5, o.getY(), z - 2, x + 3.2, o.getY() + 9, z + 3);
    }

    private static AABB plainsGate() {
        return new AABB(X0 + PLAZA_X - 5, BASE + 1, Z0 + PlainsBuilder.ARCH_Z - 1, X0 + PLAZA_X + 6, BASE + 10, Z0 + PlainsBuilder.ARCH_Z + 3.2);
    }

    private static void portals(MinecraftServer server) {
        SpawnData spawn = SpawnData.get(server);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (p.isSpectator() || p.isPassenger()) continue;
            ServerLevel level = p.level();
            boolean inSpawn = spawn.exists() && level.dimension().equals(SpawnBuilder.DIM);
            boolean here = !inSpawn && inPlains(p);
            if (!inSpawn && !here) continue;
            BlockPos o = spawn.origin();
            if (inSpawn && p.distanceToSqr(o.getX() + SPAWN_GATE_X + 0.5, o.getY() + 4, o.getZ() + SPAWN_GATE_Z + 0.5) < 26 * 26) {
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, o.getX() + SPAWN_GATE_X + 0.5, o.getY() + 3.5, o.getZ() + SPAWN_GATE_Z + 0.5, 6, 0.3, 2.5, 1.8, 0.0);
            }
            AABB gate = inSpawn ? spawnGate(o) : plainsGate();
            if (!gate.contains(p.getX(), p.getY(), p.getZ())) continue;
            long now = server.getTickCount();
            Long last = COOLDOWN.get(p.getUUID());
            if (last != null && now - last < 60) continue;
            COOLDOWN.put(p.getUUID(), now);
            level.playSound(null, p.blockPosition(), SoundEvents.PORTAL_TRAVEL, SoundSource.PLAYERS, 0.4f, 1.5f);
            if (inSpawn) enter(p);
            else HubManager.goToSpawn(p);
        }
    }

    /** Envoie le joueur sur la place d'entree des plaines. @return faux si les plaines ne sont pas pretes */
    public static boolean enter(ServerPlayer p) {
        MinecraftServer server = p.level().getServer();
        ServerLevel level = dim(server);
        if (level == null) return false;
        if (!built(server) || building()) {
            if (!building()) start(server);
            p.sendOverlayMessage(Component.literal("Les Plaines d'Aeloria sont en construction (" + percent() + " %), revenez dans un instant.").withColor(0xFFC060));
            return false;
        }
        if (com.mmorpg.hub.CinematicManager.isLocked(p)) return true;
        if (com.mmorpg.hub.ClassTrial.active(p)) com.mmorpg.hub.ClassTrial.end(p, false, null);
        p.stopRiding();
        double x = X0 + PLAZA_X + 0.5, y = BASE + 1, z = Z0 + PLAZA_Z + 0.5;
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) level.getChunk(((int) x + dx * 16) >> 4, ((int) z + dz * 16) >> 4);
        }
        ZoneManager.forgetAnnouncements(p.getUUID());
        COOLDOWN.put(p.getUUID(), (long) server.getTickCount());
        p.teleportTo(level, x, y, z, Set.of(), 0F, 4F, true);
        p.resetFallDistance();
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y + 1, z, 24, 0.6, 0.9, 0.6, 0.05);
        level.playSound(null, p.blockPosition(), com.mmorpg.registry.ModSounds.RISE.get(), SoundSource.PLAYERS, 0.8F, 1.0F);
        return true;
    }

    // ================================================================== monstres

    private static void spawnMobs(ServerLevel level) {
        PlainsLayout lay = PlainsLayout.get();
        var random = level.getRandom();
        for (ServerPlayer p : level.players()) {
            if (!inPlains(p) || p.isSpectator()) continue;
            int px = (int) p.getX() - X0, pz = (int) p.getZ() - Z0;
            if (pz < ENTRY + 4) continue;                                                           // la place d'entree est sure
            AABB area = p.getBoundingBox().inflate(40, 26, 40);
            int near = level.getEntitiesOfClass(Mob.class, area, m -> m instanceof com.mmorpg.entity.RpgMob).size();
            for (int i = 0; i < 3 && near < MOB_CAP; i++) {
                double a = random.nextDouble() * Math.PI * 2;
                double dist = 16 + random.nextDouble() * 20;
                int x = (int) (p.getX() + Math.cos(a) * dist), z = (int) (p.getZ() + Math.sin(a) * dist);
                int lx = x - X0, lz = z - Z0;
                if (!inside(lx, lz) || edge(lx, lz) < WALL + 4 || lz < ENTRY + 4) continue;
                int h = lay.height(lx, lz);
                if (h < BASE) continue;                                                              // pas dans les lacs
                BlockPos pos = new BlockPos(x, h + 1, z);
                if (!level.hasChunkAt(pos) || !level.getBlockState(pos).isAir() || !level.getBlockState(pos.above()).isAir()) continue;
                Sector s = lay.sector(lx, lz);
                String key = s.mobs().get(random.nextInt(s.mobs().size()));
                if (MobSpawner.spawnForCommand(level, key, pos, lay.mobLevel(lz, new Random(random.nextLong()))) != null) near++;
            }
        }
    }

    // ================================================================== commandes

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("plaines")
                .executes(c -> enter(c.getSource().getPlayerOrException()) ? 1 : 0)
                .then(Commands.literal("etat").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> {
                    MinecraftServer s = c.getSource().getServer();
                    String msg = building() ? "Construction en cours : " + percent() + " %" : built(s) ? "Plaines construites (plan " + VERSION + ")." : "Plaines non construites.";
                    c.getSource().sendSuccess(() -> Component.literal(msg), false);
                    return 1;
                }))
                .then(Commands.literal("reconstruire").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> {
                    boolean ok = start(c.getSource().getServer());
                    c.getSource().sendSuccess(() -> Component.literal(ok ? "Reconstruction des plaines lancée (quelques minutes)." : "Impossible : construction déjà en cours ou dimension absente."), true);
                    return ok ? 1 : 0;
                })));
    }
}
