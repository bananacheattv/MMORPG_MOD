package com.mmorpg.dungeon;

import com.mmorpg.MMORPG;
import com.mmorpg.dungeon.InstanceData.Slot;
import com.mmorpg.network.Net;
import com.mmorpg.network.Payloads;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.server.PartyManager;
import com.mmorpg.server.RpgPlayers;
import com.mmorpg.world.DungeonBuilder;
import com.mmorpg.world.DungeonBuilder.DungeonType;
import com.mmorpg.world.WaypointData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.Level;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Donjons instancies : un exemplaire par joueur seul ou par groupe, dans la dimension des donjons. Les points de passage de type
 * « donjon » du reseau de teleportation ne menent plus a un lieu fixe : ils reservent (ou construisent) un exemplaire libre et y
 * envoient le joueur et les membres de son groupe proches. Un exemplaire se libere quand plus personne n'y est depuis trois minutes,
 * et il est remis a zero (herses, epreuves) au moment ou un autre groupe le reserve.
 */
@net.neoforged.fml.common.EventBusSubscriber(modid = MMORPG.MODID)
public final class InstanceManager {
    public static final ResourceKey<Level> DIM = ResourceKey.create(Registries.DIMENSION, MMORPG.id("donjons"));
    /** Nombre maximal d'exemplaires construits pour un meme donjon et une meme difficulte. */
    public static final int MAX_PER_POOL = 40;
    public static final int RELEASE_TICKS = 20 * 60 * 3;
    /** Les exemplaires sont alignes en x (un par reservation) et les types/difficultes empiles en z ; au-dela de 9000 : l'ancien plan, plus court, est oublie. */
    static final int Z_BASE = 10000;
    static final int Z_STEP = 600;
    private static final DungeonType[] TYPES = DungeonType.values();

    private InstanceManager() {
    }

    public static String pool(DungeonType t, int difficulty) {
        return t.id + ":" + difficulty;
    }

    public static DungeonType typeOf(String pool) {
        return DungeonType.byId(pool.substring(0, pool.indexOf(':')));
    }

    public static int difficultyOf(String pool) {
        return Integer.parseInt(pool.substring(pool.indexOf(':') + 1));
    }
    public static boolean validPool(String pool) {
        try { return pool != null && typeOf(pool) != null && difficultyOf(pool) >= 0 && difficultyOf(pool) <= 2; }
        catch (RuntimeException e) { return false; }
    }
    public static int capacity() {
        return com.mmorpg.network.NetworkConfig.current.enabled ? Math.clamp(com.mmorpg.network.NetworkConfig.current.dungeonMaxInstancesPerPool, 1, MAX_PER_POOL) : MAX_PER_POOL;
    }

    /** Cle du proprietaire d'une reservation : le chef de groupe, ou le joueur seul. */
    public static String ownerKey(ServerPlayer p) {
        var party = PartyManager.partyOf(p);
        return (party != null ? party.leader : p.getUUID()).toString();
    }

    // ================================================================== portes

    /** Cree la porte d'un donjon (point de passage visible dans tous les teleporteurs, sans structure en dur). */
    public static WaypointData.Waypoint createGate(ServerLevel level, BlockPos pos, DungeonType t, int difficulty, @Nullable ServerPlayer admin) {
        String suffix = difficulty == 1 ? " (héroïque)" : difficulty >= 2 ? " (mythique)" : "";
        WaypointData data = WaypointData.get(level.getServer());
        WaypointData.Waypoint w = data.create(level.dimension(), pos, t.label + suffix, WaypointData.DUNGEON, t.level + 5 * difficulty, new UUID(0, 0));
        InstanceData.get(level.getServer()).putGate(w.id, pool(t, difficulty));
        if (admin != null) RpgPlayers.get(admin).waypoints.add(w.id);
        return w;
    }

    public static boolean isGate(MinecraftServer server, UUID waypoint) {
        return InstanceData.get(server).gate(waypoint) != null;
    }

    // ================================================================== entree

    /** Envoie le joueur (et les membres de son groupe proches) dans leur exemplaire du donjon d'une porte. @return vrai si l'entree a commence */
    public static boolean enter(ServerPlayer player, UUID gateId) {
        String pool = InstanceData.get(player.level().getServer()).gate(gateId);
        return pool != null && enterPool(player, pool);
    }

    private static final class Build {
        final Slot slot;
        final StyledDungeon.Job job;
        int next;
        long nextAt;
        long worstNanos;

        Build(Slot slot, StyledDungeon.Job job) {
            this.slot = slot;
            this.job = job;
        }

        int percent() {
            return job.size() == 0 ? 100 : next * 100 / job.size();
        }
    }

    private record Transition(UUID player, Slot slot, long teleportAt, String title, String lore, int color) {
    }

    /** D'ou chaque joueur est entre dans un donjon : on l'y ramene a la victoire. */
    private record Return(ResourceKey<Level> dim, double x, double y, double z, float yaw) {
    }

    private static final java.util.Map<UUID, Return> RETURNS = new java.util.HashMap<>();
    private static final java.util.Map<String, Build> BUILDS = new java.util.LinkedHashMap<>();
    private static final List<Transition> TRANSITIONS = new java.util.ArrayList<>();
    /** Duree minimale de l'ecran de chargement (ticks) : meme un exemplaire deja pret laisse le temps de lire. */
    private static final int MIN_LOADING_TICKS = 70;
    /** Une etape de construction tous les 2 ticks : le serveur ne se fige jamais. */
    private static final int STEP_INTERVAL = 2;

    private static String slotKey(Slot s) {
        return s.pool + "#" + s.index;
    }

    /**
     * Reserve l'exemplaire du joueur (son groupe) et l'y envoie derriere un ecran de chargement ; l'exemplaire est construit par
     * etapes s'il n'existe pas encore. @return vrai si le chargement a commence
     */
    public static boolean enterPool(ServerPlayer player, String pool) {
        if (!validPool(pool)) return false;
        if (com.mmorpg.network.DungeonTravel.enterRemote(player, pool)) return true;
        return enterLocal(player, pool, true);
    }
    /** Each proxy arrival joins the same owner reservation without teleporting teammates already inside. */
    public static boolean enterArriving(ServerPlayer player, String pool) {
        if (!com.mmorpg.network.NetworkConfig.hosts(com.mmorpg.network.NetworkConfig.Role.DUNGEONS) || !validPool(pool)) return false;
        return enterLocal(player, pool, false);
    }
    private static boolean enterLocal(ServerPlayer player, String pool, boolean groupEntry) {
        if (RpgPlayers.get(player).level < com.mmorpg.dungeon.DungeonMaster.requiredLevel(typeOf(pool), difficultyOf(pool))) return false;
        MinecraftServer server = player.level().getServer();
        ServerLevel dim = server.getLevel(DIM);
        if (dim == null) return false;
        Slot slot = reserve(server, pool, ownerKey(player));
        if (slot == null) {
            player.sendOverlayMessage(Component.literal("Tous les exemplaires de ce donjon sont occupés : réessayez dans un instant.").withColor(0xFF6050));
            return false;
        }
        DungeonType t = typeOf(pool);
        String title = t.label + (difficultyOf(pool) == 1 ? " (héroïque)" : difficultyOf(pool) >= 2 ? " (mythique)" : "");
        String lore = StyledDungeon.loreOf(t);
        int color = StyledDungeon.colorOf(t);
        long at = server.overworld().getGameTime() + MIN_LOADING_TICKS;
        for (ServerPlayer m : groupEntry ? PartyManager.onlineMembers(player) : java.util.List.of(player)) {
            if (m != player && (m.level() != player.level() || m.distanceToSqr(player) > 24 * 24)) continue;       // les autres membres rejoignent par le PNJ
            if (RpgPlayers.get(m).level < com.mmorpg.dungeon.DungeonMaster.requiredLevel(typeOf(pool), difficultyOf(pool))) continue;
            if (!m.level().dimension().equals(DIM)) RETURNS.put(m.getUUID(), new Return(m.level().dimension(), m.getX(), m.getY(), m.getZ(), m.getYRot()));
            BossReveal.hold(m, MIN_LOADING_TICKS + 400);
            TRANSITIONS.removeIf(x -> x.player().equals(m.getUUID()));
            TRANSITIONS.add(new Transition(m.getUUID(), slot, at, title, lore, color));
            Net.toPlayer(m, new Payloads.Loading(Payloads.Loading.START, title, lore, color, slot.ready ? 100 : 0));
        }
        if (!slot.ready && !BUILDS.containsKey(slotKey(slot))) {
            ServerLevel level = server.getLevel(DIM);
            BUILDS.put(slotKey(slot), new Build(slot, StyledDungeon.prepare(level, new BlockPos(slot.x, slot.y, slot.z), t, difficultyOf(pool))));
        }
        return true;
    }

    @net.neoforged.bus.api.SubscribeEvent
    public static void onServerTick(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        if (!com.mmorpg.network.NetworkConfig.hosts(com.mmorpg.network.NetworkConfig.Role.DUNGEONS)) return;
        MinecraftServer server = event.getServer();
        if (BUILDS.isEmpty() && TRANSITIONS.isEmpty()) return;
        long now = server.overworld().getGameTime();
        long deadline = System.nanoTime() + Math.clamp(com.mmorpg.network.NetworkConfig.current.dungeonBuildBudgetMs, 1, 25) * 1_000_000L;
        for (String key : new java.util.ArrayList<>(BUILDS.keySet())) {
            if (System.nanoTime() >= deadline) break;
            Build b = BUILDS.get(key);
            if (now < b.nextAt) continue;
            b.nextAt = now + STEP_INTERVAL;
            long t0 = System.nanoTime();
            try {
                b.job.run(b.next++);
            } catch (RuntimeException e) {
                MMORPG.LOGGER.error("[MMORPG] Construction d'un exemplaire de donjon interrompue", e);
                b.slot.ready = false; b.slot.owner = "";
                BUILDS.remove(key); InstanceData.get(server).dirty();
                TRANSITIONS.removeIf(transition -> {
                    if (transition.slot() != b.slot) return false;
                    ServerPlayer p = server.getPlayerList().getPlayer(transition.player());
                    if (p != null) {
                        BossReveal.release(p);
                        Net.toPlayer(p, new Payloads.Loading(Payloads.Loading.END, transition.title(), transition.lore(), transition.color(), 0));
                        p.sendSystemMessage(Component.literal("Construction du donjon impossible. Retour à l'entrée ; réessayez plus tard.").withColor(0xFF6060));
                        com.mmorpg.network.DungeonTravel.returnToSource(p);
                    }
                    return true;
                });
                continue;
            }
            b.worstNanos = Math.max(b.worstNanos, System.nanoTime() - t0);
            if (b.next >= b.job.size()) {
                b.slot.ready = true;
                InstanceData.get(server).dirty();
                MMORPG.LOGGER.info("[MMORPG] Donjon {} : exemplaire {} construit en {} etapes, la plus longue a pris {} ms", b.slot.pool, b.slot.index, b.job.size(), b.worstNanos / 1_000_000);
                BUILDS.remove(key);
            } else {
                // Rotate jobs so a slow stage cannot starve another group's construction.
                BUILDS.remove(key); BUILDS.put(key, b);
            }
        }
        var tr = TRANSITIONS.iterator();
        while (tr.hasNext()) {
            Transition x = tr.next();
            ServerPlayer p = server.getPlayerList().getPlayer(x.player());
            if (p == null) {
                tr.remove();
                continue;
            }
            Build b = BUILDS.get(slotKey(x.slot()));
            BossReveal.hold(p, 40);
            if (!x.slot().ready) {
                if (now % 4 == 0) Net.toPlayer(p, new Payloads.Loading(Payloads.Loading.PROGRESS, x.title(), x.lore(), x.color(), b == null ? 0 : b.percent()));
            } else if (now >= x.teleportAt()) {
                Net.toPlayer(p, new Payloads.Loading(Payloads.Loading.PROGRESS, x.title(), x.lore(), x.color(), 100));
                BossReveal.release(p);
                teleportIn(p, x.slot());
                Net.toPlayer(p, new Payloads.Loading(Payloads.Loading.END, x.title(), x.lore(), x.color(), 100));
                tr.remove();
            } else if (now % 4 == 0) {
                Net.toPlayer(p, new Payloads.Loading(Payloads.Loading.PROGRESS, x.title(), x.lore(), x.color(), 100));
            }
        }
    }

    /** Exemplaire du proprietaire, sinon un exemplaire libre (remis a zero), sinon un nouveau (marque « en construction »). */
    static @Nullable Slot reserve(MinecraftServer server, String pool, String owner) {
        if (!com.mmorpg.network.NetworkConfig.hosts(com.mmorpg.network.NetworkConfig.Role.DUNGEONS) || !validPool(pool)) return null;
        InstanceData data = InstanceData.get(server);
        List<Slot> slots = data.slotsOf(pool);
        for (Slot s : slots) {
            if (s.owner.equals(owner)) {
                s.lastBusy = server.getTickCount();
                data.dirty();
                return s;
            }
        }
        for (Slot s : slots) {
            if (s.owner.isEmpty()) {
                s.owner = owner;
                s.lastBusy = server.getTickCount();
                data.dirty();
                if (s.ready) resetSlot(server, s);
                return s;
            }
        }
        if (slots.size() >= capacity()) return null;
        DungeonType t = typeOf(pool);
        int poolIndex = 0;
        for (int i = 0; i < TYPES.length; i++) {
            if (TYPES[i] == t) poolIndex = i * 3 + difficultyOf(pool);
        }
        Slot s = new Slot(pool, slots.size(), 200 + slots.size() * 120, 101, Z_BASE + poolIndex * Z_STEP, owner, server.getTickCount(), false);
        data.slots.add(s);
        data.dirty();
        return s;
    }

    /** Le groupe du joueur a-t-il deja un exemplaire de ce donjon (pour afficher « Reprendre ») ? */
    public static boolean hasInstance(MinecraftServer server, String pool, String owner) {
        if (!com.mmorpg.network.NetworkConfig.hosts(com.mmorpg.network.NetworkConfig.Role.DUNGEONS)) return false;
        for (Slot s : InstanceData.get(server).slotsOf(pool)) {
            if (s.owner.equals(owner)) return true;
        }
        return false;
    }

    static void teleportIn(ServerPlayer p, Slot slot) {
        ServerLevel level = p.level().getServer().getLevel(DIM);
        if (level == null) return;
        p.stopRiding();
        com.mmorpg.zone.ZoneManager.forgetAnnouncements(p.getUUID());           // nouvelle partie : l'annonce d'entree se rejoue une fois
        p.teleportTo(level, slot.x + 0.5, slot.y, slot.z + Theme.HALL_Z - 6 + 0.5, Set.<Relative>of(), 0F, 4F, true);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 50, 0.4, 0.9, 0.4, 0.1);
        slot.lastBusy = level.getServer().getTickCount();
        InstanceData.get(level.getServer()).dirty();
    }

    // ================================================================== reservation

    /** Exemplaire du proprietaire, sinon un exemplaire libre (remis a zero), sinon un nouveau ; {@code null} si tout est occupe. */
    public static @Nullable Slot allocate(MinecraftServer server, String pool, String owner) {
        if (!com.mmorpg.network.NetworkConfig.hosts(com.mmorpg.network.NetworkConfig.Role.DUNGEONS) || !validPool(pool)) return null;
        if (com.mmorpg.network.NetworkConfig.current.enabled) {
            Slot slot = reserve(server, pool, owner);
            if (slot != null && !slot.ready && !BUILDS.containsKey(slotKey(slot))) {
                BUILDS.put(slotKey(slot), new Build(slot, StyledDungeon.prepare(server.getLevel(DIM), new BlockPos(slot.x, slot.y, slot.z), typeOf(pool), difficultyOf(pool))));
            }
            return slot;
        }
        InstanceData data = InstanceData.get(server);
        List<Slot> slots = data.slotsOf(pool);
        for (Slot s : slots) {
            if (s.owner.equals(owner)) {
                s.lastBusy = server.getTickCount();
                data.dirty();
                return s;
            }
        }
        for (Slot s : slots) {
            if (s.owner.isEmpty()) {
                s.owner = owner;
                s.lastBusy = server.getTickCount();
                data.dirty();
                if (s.ready) DungeonManager.reset(server, group(pool, s));
                else {
                    DungeonBuilder.buildDungeon(server.getLevel(DIM), new BlockPos(s.x, s.y, s.z), typeOf(pool), difficultyOf(pool));
                    s.ready = true;
                }
                return s;
            }
        }
        if (slots.size() >= capacity()) return null;
        ServerLevel level = server.getLevel(DIM);
        if (level == null) return null;
        DungeonType t = typeOf(pool);
        int poolIndex = 0;
        for (int i = 0; i < TYPES.length; i++) {
            if (TYPES[i] == t) poolIndex = i * 3 + difficultyOf(pool);
        }
        Slot s = new Slot(pool, slots.size(), 200 + slots.size() * 120, 101, Z_BASE + poolIndex * Z_STEP, owner, server.getTickCount());
        data.slots.add(s);
        data.dirty();
        DungeonBuilder.buildDungeon(level, new BlockPos(s.x, s.y, s.z), t, difficultyOf(pool));
        s.ready = true;
        MMORPG.LOGGER.info("[MMORPG] Donjon {} : exemplaire {} construit en ({}, {})", pool, s.index, s.x, s.z);
        return s;
    }

    /** Identifiant du groupe d'epreuves d'un exemplaire (voir StyledDungeon). */
    static String group(String pool, Slot s) {
        return typeOf(pool).id + "_" + s.x + "_" + s.z;
    }

    /** Libere les exemplaires abandonnes depuis plus de trois minutes ; appelee regulierement. */
    static void releaseTick(MinecraftServer server) {
        if (!com.mmorpg.network.NetworkConfig.hosts(com.mmorpg.network.NetworkConfig.Role.DUNGEONS)) return;
        ServerLevel level = server.getLevel(DIM);
        if (level == null) return;
        InstanceData data = InstanceData.get(server);
        long now = server.getTickCount();
        for (Slot s : data.slots) {
            if (s.owner.isEmpty()) continue;
            boolean occupied = false;
            for (ServerPlayer p : level.players()) {
                if (!p.isSpectator() && Math.abs(p.getX() - s.x) < 40 && p.getZ() > s.z - 6 && p.getZ() < s.z + Layout.LENGTH + 6) {
                    occupied = true;
                    break;
                }
            }
            if (occupied) {
                s.lastBusy = now;
            } else if (now - s.lastBusy > RELEASE_TICKS) {
                MMORPG.LOGGER.info("[MMORPG] Donjon {} : exemplaire {} libere", s.pool, s.index);
                s.owner = "";
                data.dirty();
            }
        }
    }

    // ================================================================== victoire : retour a l'entree

    /** Delai (secondes) entre la mort du boss et le retour automatique a l'entree. */
    public static final int EXIT_SECONDS = 15;

    /** Exemplaire dont l'emprise contient la position, ou {@code null}. */
    static @Nullable Slot slotAt(MinecraftServer server, ServerLevel level, double x, double z) {
        if (!level.dimension().equals(DIM)) return null;
        for (Slot s : InstanceData.get(server).slots) {
            if (Math.abs(x - s.x) < 40 && z > s.z - 6 && z < s.z + Layout.LENGTH + 6) return s;
        }
        return null;
    }

    private static net.minecraft.world.phys.AABB areaOf(Slot s) {
        return new net.minecraft.world.phys.AABB(s.x - 40, s.y - 20, s.z - 6, s.x + 40, s.y + 80, s.z + Layout.LENGTH + 6);
    }

    /**
     * Le boss d'un antre vient de tomber : les joueurs de l'exemplaire (ou du donjon, hors instance) sont ramenes a l'entree apres
     * {@link #EXIT_SECONDS} secondes avec un compte a rebours, puis l'exemplaire est libere et remis a neuf pour le groupe suivant.
     *
     * @param hall point d'arrivee hors instance (hall du donjon)
     */
    public static void victoryExit(ServerLevel level, double bossX, double bossZ, net.minecraft.world.phys.Vec3 hall) {
        MinecraftServer server = level.getServer();
        Slot slot = slotAt(server, level, bossX, bossZ);
        java.util.function.Predicate<ServerPlayer> inside = p -> p.level() == level && p.isAlive() && (slot != null
                ? areaOf(slot).contains(p.position())
                : Math.abs(p.getX() - hall.x) < 40 && p.getZ() > hall.z - 6 && p.getZ() < hall.z + Layout.LENGTH);
        for (ServerPlayer p : level.players()) {
            if (inside.test(p)) {
                p.sendSystemMessage(Component.literal("✦ Donjon vaincu ! Récupérez votre butin : vous serez ramené à l'entrée dans " + EXIT_SECONDS + " secondes, ou parlez tout de suite au Passeur.").withColor(0xFFE080));
            }
        }
        for (int left = EXIT_SECONDS - 3; left >= 1; left--) {
            int n = left;
            com.mmorpg.server.Scheduler.later(level, (EXIT_SECONDS - left) * 20, () -> {
                for (ServerPlayer p : level.players()) {
                    if (inside.test(p)) p.sendOverlayMessage(Component.literal("Retour à l'entrée du donjon dans " + n + " s").withColor(n <= 5 ? 0xFF9060 : 0xFFE080));
                }
            });
        }
        com.mmorpg.server.Scheduler.later(level, EXIT_SECONDS * 20, () -> {
            for (ServerPlayer p : new java.util.ArrayList<>(level.players())) {
                if (inside.test(p)) bringBack(server, p, hall);
            }
            if (slot != null) {
                slot.owner = "";
                InstanceData.get(server).dirty();
                com.mmorpg.server.Scheduler.later(level, 20, () -> resetSlot(server, slot));
            }
        });
    }

    /**
     * Le Passeur apparu a la mort du boss : ramene tout de suite le joueur a l'entree, sans attendre le compte a rebours.
     *
     * @param hall point d'arrivee hors instance, « x,y,z » (celui du compte a rebours)
     */
    public static void leaveNow(ServerPlayer p, String hall) {
        String[] c = hall.split(",");
        if (c.length != 3) return;
        net.minecraft.world.phys.Vec3 v;
        try {
            v = new net.minecraft.world.phys.Vec3(Double.parseDouble(c[0]), Double.parseDouble(c[1]), Double.parseDouble(c[2]));
        } catch (NumberFormatException e) {
            return;
        }
        bringBack(p.level().getServer(), p, v);
    }

    /** Cree le Passeur pres de l'endroit ou le boss est tombe ; il disparait quand le compte a rebours ramene tout le monde. */
    public static void spawnPasseur(ServerLevel level, net.minecraft.core.BlockPos pos, net.minecraft.world.phys.Vec3 hall) {
        com.mmorpg.entity.NpcEntity npc = com.mmorpg.registry.ModEntities.NPC.get().create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (npc == null) return;
        npc.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0F, 0F);
        npc.setup(com.mmorpg.entity.NpcEntity.Role.SORTIE, "Passeur", 2, hall.x + "," + hall.y + "," + hall.z);
        level.addFreshEntity(npc);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 60, 0.5, 1.0, 0.5, 0.1);
        level.playSound(null, pos, com.mmorpg.registry.ModSounds.CHIME.get(), net.minecraft.sounds.SoundSource.NEUTRAL, 1.0F, 1.0F);
        com.mmorpg.server.Scheduler.later(level, EXIT_SECONDS * 20 + 10, () -> {
            if (npc.isAlive()) npc.discard();
        });
    }

    private static void bringBack(MinecraftServer server, ServerPlayer p, net.minecraft.world.phys.Vec3 hall) {
        if (com.mmorpg.network.DungeonTravel.returnToSource(p)) return;
        Return r = RETURNS.remove(p.getUUID());
        ServerLevel target = r == null ? null : server.getLevel(r.dim());
        p.stopRiding();
        ServerLevel from = p.level();
        from.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 40, 0.4, 0.9, 0.4, 0.1);
        if (target != null) {
            p.teleportTo(target, r.x(), r.y(), r.z(), Set.<Relative>of(), r.yaw(), 0F, true);
        } else if (!from.dimension().equals(DIM)) {
            p.teleportTo(from, hall.x, hall.y, hall.z, Set.<Relative>of(), 0F, 4F, true);
        } else {
            var respawn = server.getRespawnData().globalPos();
            ServerLevel home = server.getLevel(respawn.dimension());
            if (home != null) p.teleportTo(home, respawn.pos().getX() + 0.5, respawn.pos().getY(), respawn.pos().getZ() + 0.5, Set.<Relative>of(), 180F, 0F, true);
        }
        p.level().sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 40, 0.4, 0.9, 0.4, 0.1);
        p.level().playSound(null, p.blockPosition(), com.mmorpg.registry.ModSounds.TELEPORT.get(), net.minecraft.sounds.SoundSource.PLAYERS, 1.0F, 1.0F);
        p.sendOverlayMessage(Component.literal("Vous êtes de retour à l'entrée du donjon.").withColor(0x80E0A0));
    }

    /** Remet un exemplaire libre a neuf : epreuves, herses, monstres, objets au sol et coffres. */
    static void resetSlot(MinecraftServer server, Slot s) {
        ServerLevel level = server.getLevel(DIM);
        if (level == null || !s.ready) return;
        DungeonManager.reset(server, group(s.pool, s));
        clearEntities(level, s);
        DungeonType t = typeOf(s.pool);
        StyledDungeon.refill(level, new BlockPos(s.x, s.y, s.z), t, difficultyOf(s.pool));
    }

    private static void clearEntities(ServerLevel level, Slot s) {
        var box = areaOf(s);
        for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.Mob.class, box, m -> m instanceof net.minecraft.world.entity.monster.Enemy || m instanceof com.mmorpg.entity.RpgMob)) {
            e.discard();
        }
        for (var e : level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, box)) e.discard();
        for (var e : level.getEntitiesOfClass(com.mmorpg.entity.NpcEntity.class, box, n -> n.role() == com.mmorpg.entity.NpcEntity.Role.SORTIE)) e.discard();
    }

    /** Au demarrage : les exemplaires de l'ancien plan (plus court) sont oublies, avec leurs epreuves et leurs zones. */
    @net.neoforged.bus.api.SubscribeEvent
    public static void onServerStarted(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        if (!com.mmorpg.network.NetworkConfig.hosts(com.mmorpg.network.NetworkConfig.Role.DUNGEONS)) return;
        MinecraftServer server = event.getServer();
        DungeonLobby.ensure(server);
        TrialData.get(server).removeInside(DIM, -1_000_000, -1_000_000, 1_000_000, Z_BASE - 1000);
        for (DungeonType t : TYPES) {
            com.mmorpg.zone.ZoneData.get(server).removeOverlapping(DIM, t.id + "_", -1_000_000, -1_000_000, 1_000_000, Z_BASE - 1000);
        }
        // Tick counters restart at zero; keep the normal reconnect grace period after a restart.
        for (Slot slot : InstanceData.get(server).slots) slot.lastBusy = server.getTickCount();
    }

    // ================================================================== outils d'administration et de verification

    /** Un joueur qui se reconnecte dans un exemplaire reserve entre-temps par un autre groupe est renvoye au point d'apparition du monde. */
    @net.neoforged.bus.api.SubscribeEvent
    public static void onLogin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !p.level().dimension().equals(DIM)) return;
        if (!com.mmorpg.network.NetworkConfig.hosts(com.mmorpg.network.NetworkConfig.Role.DUNGEONS)) {
            if (com.mmorpg.network.NetworkConfig.current.role == com.mmorpg.network.NetworkConfig.Role.MAIN && p.getZ() >= Z_BASE - 100) com.mmorpg.hub.HubManager.goToSpawn(p);
            return;
        }
        if (com.mmorpg.network.NetworkPlayers.isArriving(p)) return;
        if (com.mmorpg.mine.MineManager.inMine(p)) return;            // les mines sont partagees : on y reste a la reconnexion
        MinecraftServer server = p.level().getServer();
        String mine = ownerKey(p);
        for (Slot s : InstanceData.get(server).slots) {
            if (Math.abs(p.getX() - s.x) < 40 && p.getZ() > s.z - 6 && p.getZ() < s.z + Layout.LENGTH + 6) {
                if (s.owner.equals(mine)) return;
                break;
            }
        }
        if (com.mmorpg.network.DungeonTravel.returnToSource(p)) return;
        var respawn = server.getRespawnData().globalPos();
        ServerLevel target = server.getLevel(respawn.dimension());
        if (target == null) return;
        p.teleportTo(target, respawn.pos().getX() + 0.5, respawn.pos().getY(), respawn.pos().getZ() + 0.5, Set.<Relative>of(), 180F, 0F, true);
        p.sendSystemMessage(Component.literal("Votre exemplaire de donjon a été libéré : vous êtes ramené au point d'apparition.").withColor(0xFFC060));
    }

    /** Test automatique : oublie tous les exemplaires enregistres. */
    public static void debugForgetAll(MinecraftServer server) {
        InstanceData d = InstanceData.get(server);
        d.slots.clear();
        d.dirty();
    }

    /** Construit {@code count} exemplaires libres d'avance (la premiere construction d'un exemplaire prend un moment). */
    public static int prewarm(MinecraftServer server, DungeonType t, int difficulty, int count) {
        if (!com.mmorpg.network.NetworkConfig.hosts(com.mmorpg.network.NetworkConfig.Role.DUNGEONS)) return 0;
        String pool = pool(t, difficulty);
        int built = 0;
        java.util.List<Slot> warming = new java.util.ArrayList<>();
        for (int i = 0; i < count; i++) {
            String tmp = "prechauffage-" + i;
            Slot s = allocate(server, pool, tmp);
            if (s != null && s.owner.equals(tmp)) {
                warming.add(s);
                built++;
            }
        }
        for (Slot slot : warming) slot.owner = "";
        InstanceData.get(server).dirty();
        return built;
    }
    public static boolean returnToEntrance(ServerPlayer player) {
        TRANSITIONS.removeIf(t -> t.player().equals(player.getUUID()));
        BossReveal.release(player);
        if (com.mmorpg.network.DungeonTravel.returnToSource(player)) return true;
        if (!player.level().dimension().equals(DIM)) return false;
        bringBack(player.level().getServer(), player, player.position());
        return true;
    }

    /** Test automatique : origine (x, y, z) et proprietaire d'un exemplaire, ou {@code null}. */
    public static String debugSlot(MinecraftServer server, String pool, int index) {
        for (Slot s : InstanceData.get(server).slotsOf(pool)) {
            if (s.index == index) return s.x + "," + s.y + "," + s.z + "," + (s.owner.isEmpty() ? "libre" : "occupe") + "," + s.ready;
        }
        return null;
    }

    public static int count(MinecraftServer server, String pool) {
        return InstanceData.get(server).slotsOf(pool).size();
    }

    public static void release(MinecraftServer server, String pool, String owner) {
        for (Slot s : InstanceData.get(server).slotsOf(pool)) {
            if (s.owner.equals(owner)) s.owner = "";
        }
        InstanceData.get(server).dirty();
    }

    @SuppressWarnings("unused")
    private static PlayerData unused(ServerPlayer p) {
        return RpgPlayers.get(p);
    }
}
