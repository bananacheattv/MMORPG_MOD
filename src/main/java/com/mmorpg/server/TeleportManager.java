package com.mmorpg.server;

import com.mmorpg.block.TeleporterBlock;
import com.mmorpg.network.Net;
import com.mmorpg.network.Payloads;
import com.mmorpg.network.S2COpenScreen;
import com.mmorpg.registry.ModItems;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.world.WaypointData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import java.util.Set;
import java.util.UUID;

/** Reseau de teleportation : decouverte, voyage entre villes et donjons, configuration des teleporteurs. */
public final class TeleportManager {
    private TeleportManager() {
    }

    public static boolean canEdit(ServerPlayer player, WaypointData.Waypoint w) {
        return TeleporterBlock.isAdmin(player);
    }

    public static void onPlaced(ServerLevel level, BlockPos pos, ServerPlayer player) {
        WaypointData data = WaypointData.get(level.getServer());
        WaypointData.Waypoint w = data.create(level.dimension(), pos, "Nouvelle ville", WaypointData.CITY, 1, player.getUUID());
        RpgPlayers.get(player).waypoints.add(w.id);
        openSetup(player, w);
    }

    public static void onUse(ServerLevel level, BlockPos pos, ServerPlayer player) {
        WaypointData data = WaypointData.get(level.getServer());
        WaypointData.Waypoint w = data.at(level.dimension(), pos);
        if (w == null) {
            w = data.create(level.dimension(), pos, "Téléporteur " + (data.all().size() + 1), WaypointData.WAYPOINT, 1, new UUID(0, 0));
        }
        if (player.isShiftKeyDown() && canEdit(player, w)) {
            openSetup(player, w);
            return;
        }
        PlayerData d = RpgPlayers.get(player);
        if (d.waypoints.add(w.id)) {
            Net.toPlayer(player, new Payloads.Notify(Payloads.Notify.DISCOVERY, "LIEU DÉCOUVERT",
                    WaypointData.CATEGORY_NAMES[Math.max(0, Math.min(2, w.category))] + " : " + w.name, 0x60C0FF));
            level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.5f, 1f);
            level.sendParticles(ParticleTypes.END_ROD, pos.getX() + 0.5, pos.getY() + 1.2, pos.getZ() + 0.5, 30, 0.4, 0.6, 0.4, 0.05);
        }
        openFor(player, w);
    }

    private static void openSetup(ServerPlayer player, WaypointData.Waypoint w) {
        CompoundTag t = new CompoundTag();
        t.putLong("pos", w.pos.asLong());
        t.putString("name", w.name);
        t.putInt("category", w.category);
        t.putInt("minLevel", w.minLevel);
        t.putBoolean("op", player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER));
        Net.toPlayer(player, new S2COpenScreen(S2COpenScreen.TELEPORTER_SETUP, t));
    }

    /** Ouvre la liste des destinations ; {@code current} est nul si le joueur utilise un parchemin. */
    public static void openFor(ServerPlayer player, WaypointData.Waypoint current) {
        WaypointData data = WaypointData.get(player.level().getServer());
        PlayerData d = RpgPlayers.get(player);
        ListTag list = new ListTag();
        for (WaypointData.Waypoint w : data.all()) {
            if (!d.waypoints.contains(w.id)) continue;
            CompoundTag e = new CompoundTag();
            e.putString("id", w.id.toString());
            e.putString("name", w.name);
            e.putInt("category", w.category);
            e.putString("dim", WaypointData.dimensionName(w.dimension));
            e.putBoolean("sameDim", w.dimension.equals(player.level().dimension()));
            e.putInt("x", w.pos.getX());
            e.putInt("y", w.pos.getY());
            e.putInt("z", w.pos.getZ());
            e.putInt("minLevel", w.minLevel);
            e.putBoolean("current", current != null && current.id.equals(w.id));
            list.add(e);
        }
        CompoundTag t = new CompoundTag();
        t.put("list", list);
        t.putInt("total", data.all().size());
        t.putString("current", current == null ? "" : current.name);
        t.putBoolean("scroll", current == null);
        Net.toPlayer(player, new S2COpenScreen(S2COpenScreen.TELEPORTER, t));
    }

    public static void configure(ServerPlayer player, BlockPos pos, String name, int category, int minLevel) {
        ServerLevel level = player.level();
        if (!(level.getBlockState(pos).getBlock() instanceof TeleporterBlock) || player.distanceToSqr(Vec3.atCenterOf(pos)) > 100) return;
        WaypointData data = WaypointData.get(level.getServer());
        WaypointData.Waypoint w = data.at(level.dimension(), pos);
        if (w == null || !canEdit(player, w)) return;
        String clean = name.strip();
        if (clean.isEmpty()) clean = w.name;
        w.name = clean.length() > 32 ? clean.substring(0, 32) : clean;
        w.category = Math.max(0, Math.min(2, category));
        w.minLevel = Math.max(1, Math.min(100, minLevel));
        data.setDirty();
        player.sendOverlayMessage(Component.literal("Téléporteur « " + w.name + " » enregistré.").withColor(0x60E060));
    }

    private static boolean nearTeleporter(ServerPlayer player) {
        BlockPos c = player.blockPosition();
        for (BlockPos p : BlockPos.betweenClosed(c.offset(-6, -3, -6), c.offset(6, 3, 6))) {
            if (player.level().getBlockState(p).getBlock() instanceof TeleporterBlock) return true;
        }
        return false;
    }

    public static void travel(ServerPlayer player, String idString) {
        UUID id;
        try {
            id = UUID.fromString(idString);
        } catch (IllegalArgumentException e) {
            return;
        }
        WaypointData data = WaypointData.get(player.level().getServer());
        WaypointData.Waypoint w = data.get(id);
        PlayerData d = RpgPlayers.get(player);
        if (w == null || !d.waypoints.contains(id)) return;
        if (d.level < w.minLevel) {
            player.sendOverlayMessage(Component.literal("Niveau " + w.minLevel + " requis pour voyager vers " + w.name).withColor(0xFF5050));
            return;
        }
        boolean near = nearTeleporter(player);
        ItemStack scroll = ItemStack.EMPTY;
        if (!near) {
            for (ItemStack s : player.getInventory()) {
                if (s.is(ModItems.PARCHEMIN_TELEPORTATION.get())) {
                    scroll = s;
                    break;
                }
            }
            if (scroll.isEmpty()) {
                player.sendOverlayMessage(Component.literal("Vous devez être près d'un téléporteur ou posséder un parchemin.").withColor(0xFF5050));
                return;
            }
        }
        ServerLevel target = player.level().getServer().getLevel(w.dimension);
        if (target == null) return;
        Vec3 dest = safeSpot(target, w.pos);
        // arrivee devant l'arche, tourne vers elle (et hors du passage, pour ne pas rouvrir la liste aussitot)
        net.minecraft.world.level.block.state.BlockState arrival = target.getBlockState(w.pos);
        float yaw = arrival.getBlock() instanceof TeleporterBlock
                ? arrival.getValue(TeleporterBlock.FACING).getOpposite().toYRot() : player.getYRot();
        ServerLevel from = player.level();
        from.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1, player.getZ(), 60, 0.4, 0.9, 0.4, 0.6);
        from.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 0.9f);
        if (!near && !player.hasInfiniteMaterials()) scroll.shrink(1);
        player.teleportTo(target, dest.x, dest.y, dest.z, Set.<Relative>of(), yaw, 0.0F, true);
        target.sendParticles(ParticleTypes.REVERSE_PORTAL, dest.x, dest.y + 1, dest.z, 60, 0.4, 0.9, 0.4, 0.1);
        target.playSound(null, BlockPos.containing(dest), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1.2f);
        Net.toPlayer(player, new Payloads.Notify(Payloads.Notify.INFO, w.name.toUpperCase(java.util.Locale.ROOT),
                WaypointData.CATEGORY_NAMES[Math.max(0, Math.min(2, w.category))] + " — " + WaypointData.dimensionName(w.dimension), 0x60C0FF));
    }

    private static boolean free(ServerLevel level, BlockPos p) {
        return level.getBlockState(p).getCollisionShape(level, p).isEmpty()
                && level.getBlockState(p.above()).getCollisionShape(level, p.above()).isEmpty()
                && !level.getBlockState(p.below()).getCollisionShape(level, p.below()).isEmpty();
    }

    private static Vec3 safeSpot(ServerLevel level, BlockPos tp) {
        level.getChunk(tp);
        net.minecraft.world.level.block.state.BlockState st = level.getBlockState(tp);
        if (st.getBlock() instanceof TeleporterBlock) {       // devant l'arche, au-dela des marches
            Direction front = st.getValue(TeleporterBlock.FACING);
            for (int r = 2; r <= 4; r++) {
                for (int dy = 0; dy <= 1; dy++) {
                    BlockPos p = tp.relative(front, r).above(dy);
                    if (free(level, p)) return Vec3.atBottomCenterOf(p);
                }
            }
        }
        for (int r = 1; r <= 3; r++) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                for (int dy = 0; dy <= 1; dy++) {
                    BlockPos p = tp.relative(dir, r).above(dy);
                    if (free(level, p)) return Vec3.atBottomCenterOf(p);
                }
            }
        }
        return Vec3.atBottomCenterOf(tp.above());
    }
}
