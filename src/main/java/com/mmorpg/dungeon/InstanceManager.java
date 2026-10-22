package com.mmorpg.dungeon;

import com.mmorpg.MMORPG;
import com.mmorpg.dungeon.InstanceData.Slot;
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
    private static final DungeonType[] TYPES = DungeonType.values();

    private InstanceManager() {
    }

    public static String pool(DungeonType t, int difficulty) {
        return t.id + ":" + difficulty;
    }

    private static DungeonType typeOf(String pool) {
        return DungeonType.byId(pool.substring(0, pool.indexOf(':')));
    }

    private static int difficultyOf(String pool) {
        return Integer.parseInt(pool.substring(pool.indexOf(':') + 1));
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

    /** Envoie le joueur (et les membres de son groupe proches) dans leur exemplaire du donjon. @return vrai si l'entree a eu lieu */
    public static boolean enter(ServerPlayer player, UUID gateId) {
        MinecraftServer server = player.level().getServer();
        String pool = InstanceData.get(server).gate(gateId);
        if (pool == null) return false;
        Slot slot = allocate(server, pool, ownerKey(player));
        if (slot == null) {
            player.sendOverlayMessage(Component.literal("Tous les exemplaires de ce donjon sont occupés : réessayez dans un instant.").withColor(0xFF6050));
            return false;
        }
        for (ServerPlayer m : PartyManager.onlineMembers(player)) {
            if (m != player && (m.level() != player.level() || m.distanceToSqr(player) > 24 * 24)) continue;       // les autres membres rejoignent par la porte
            teleportIn(m, slot);
        }
        return true;
    }

    static void teleportIn(ServerPlayer p, Slot slot) {
        ServerLevel level = p.level().getServer().getLevel(DIM);
        if (level == null) return;
        p.stopRiding();
        p.teleportTo(level, slot.x + 0.5, slot.y, slot.z + Theme.HALL_Z - 6 + 0.5, Set.<Relative>of(), 0F, 4F, true);
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, p.getX(), p.getY() + 1, p.getZ(), 50, 0.4, 0.9, 0.4, 0.1);
        slot.lastBusy = level.getServer().getTickCount();
        InstanceData.get(level.getServer()).dirty();
    }

    // ================================================================== reservation

    /** Exemplaire du proprietaire, sinon un exemplaire libre (remis a zero), sinon un nouveau ; {@code null} si tout est occupe. */
    public static @Nullable Slot allocate(MinecraftServer server, String pool, String owner) {
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
                DungeonManager.reset(server, group(pool, s));
                return s;
            }
        }
        if (slots.size() >= MAX_PER_POOL) return null;
        ServerLevel level = server.getLevel(DIM);
        if (level == null) return null;
        DungeonType t = typeOf(pool);
        int poolIndex = 0;
        for (int i = 0; i < TYPES.length; i++) {
            if (TYPES[i] == t) poolIndex = i * 3 + difficultyOf(pool);
        }
        Slot s = new Slot(pool, slots.size(), 200 + slots.size() * 120, 101, 1000 + poolIndex * 400, owner, server.getTickCount());
        data.slots.add(s);
        data.dirty();
        DungeonBuilder.buildDungeon(level, new BlockPos(s.x, s.y, s.z), t, difficultyOf(pool));
        MMORPG.LOGGER.info("[MMORPG] Donjon {} : exemplaire {} construit en ({}, {})", pool, s.index, s.x, s.z);
        return s;
    }

    /** Identifiant du groupe d'epreuves d'un exemplaire (voir StyledDungeon). */
    static String group(String pool, Slot s) {
        return typeOf(pool).id + "_" + s.x + "_" + s.z;
    }

    /** Libere les exemplaires abandonnes depuis plus de trois minutes ; appelee regulierement. */
    static void releaseTick(MinecraftServer server) {
        ServerLevel level = server.getLevel(DIM);
        if (level == null) return;
        InstanceData data = InstanceData.get(server);
        long now = server.getTickCount();
        for (Slot s : data.slots) {
            if (s.owner.isEmpty()) continue;
            boolean occupied = false;
            for (ServerPlayer p : level.players()) {
                if (!p.isSpectator() && Math.abs(p.getX() - s.x) < 32 && p.getZ() > s.z - 6 && p.getZ() < s.z + 196) {
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

    // ================================================================== outils d'administration et de verification

    /** Un joueur qui se reconnecte dans un exemplaire reserve entre-temps par un autre groupe est renvoye au point d'apparition du monde. */
    @net.neoforged.bus.api.SubscribeEvent
    public static void onLogin(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || !p.level().dimension().equals(DIM)) return;
        MinecraftServer server = p.level().getServer();
        String mine = ownerKey(p);
        for (Slot s : InstanceData.get(server).slots) {
            if (Math.abs(p.getX() - s.x) < 32 && p.getZ() > s.z - 6 && p.getZ() < s.z + 196) {
                if (s.owner.equals(mine)) return;
                break;
            }
        }
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
        String pool = pool(t, difficulty);
        int built = 0;
        for (int i = 0; i < count; i++) {
            String tmp = "prechauffage-" + i;
            Slot s = allocate(server, pool, tmp);
            if (s != null && s.owner.equals(tmp)) {
                s.owner = "";
                built++;
            }
        }
        InstanceData.get(server).dirty();
        return built;
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
