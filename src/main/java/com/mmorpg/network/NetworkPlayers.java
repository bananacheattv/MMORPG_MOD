package com.mmorpg.network;

import com.mmorpg.MMORPG;
import com.mmorpg.hub.ClassTrial;
import com.mmorpg.registry.ModAttachments;
import com.mmorpg.server.RpgPlayers;
import com.mmorpg.server.Scheduler;
import net.minecraft.resources.RegistryOps;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import java.io.*;
import java.nio.file.Path;
import java.util.*;

/** Portable RPG/inventory snapshots; position, world IDs and permissions remain server-local. */
public final class NetworkPlayers {
    private static final Map<UUID, ProfileStore> SESSIONS = new HashMap<>();
    private static final Map<UUID, String> TRAVEL = new HashMap<>();
    private static final Map<UUID, Long> TRANSFERRING = new HashMap<>();
    private static final Map<UUID, String> ARRIVALS = new HashMap<>();
    private static final Map<UUID, Long> PORTAL_GRACE = new HashMap<>();
    private static final EquipmentSlot[] EQUIPMENT = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.OFFHAND};

    public static void start() {
        try { NetworkConfig.load(); SharedParties.refresh(); }
        catch (Exception e) { throw new IllegalStateException("Cannot load Aeloria network configuration", e); }
    }

    public static boolean login(ServerPlayer p) {
        if (!NetworkConfig.current.enabled) return true;
        ProfileStore store = null;
        try {
            SharedParties.refresh();
            // Source JVM releases its lock at logout. Never steal a live session or load stale data.
            long deadline = System.nanoTime() + 2_000_000_000L;
            do {
                store = ProfileStore.acquire(Path.of(NetworkConfig.current.sharedDirectory).toAbsolutePath(), p.getUUID());
                if (store != null) break;
                Thread.sleep(25);
            } while (System.nanoTime() < deadline);
            if (store == null) throw new IOException("Player is still active on another server");
            byte[] data = store.read();
            String destination = "";
            if (data != null) {
                CompoundTag tag = NbtIo.readCompressed(new ByteArrayInputStream(data), NbtAccounter.create(16 * 1024 * 1024));
                if (tag.getIntOr("version", 0) != 1 || !p.getUUID().toString().equals(tag.getStringOr("uuid", ""))) throw new IOException("Invalid player profile");
                restore(p, tag);
                destination = tag.getStringOr("destination", "");
            }
            SESSIONS.put(p.getUUID(), store);
            PORTAL_GRACE.put(p.getUUID(), (long) p.level().getServer().getTickCount() + 100);
            MMORPG.LOGGER.info("[Aeloria voyage] Arrivée {} sur {} : {}", p.getUUID(), NetworkConfig.current.role, destination);
            final String arrival = destination;
            if (!arrival.isEmpty()) ARRIVALS.put(p.getUUID(), arrival);
            Scheduler.later(p.level(), 5, () -> { if (!p.isRemoved()) arrive(p, arrival); });
            return true;
        } catch (Exception e) {
            if (store != null) try { store.close(); } catch (IOException ignored) { }
            MMORPG.LOGGER.error("[Aeloria] Refusing login: shared profile {}", p.getUUID(), e);
            p.connection.disconnect(Component.literal("Votre personnage n'est pas disponible. Réessayez dans quelques secondes ; contactez un administrateur si le problème persiste."));
            return false;
        }
    }

    private static ListTag items(Container inv, ServerPlayer p) {
        ListTag list = new ListTag();
        var ops = RegistryOps.create(NbtOps.INSTANCE, p.registryAccess());
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item.isEmpty()) continue;
            CompoundTag entry = new CompoundTag();
            entry.putInt("slot", i);
            entry.put("item", ItemStack.CODEC.encodeStart(ops, item).getOrThrow());
            list.add(entry);
        }
        return list;
    }

    private static Map<Integer, ItemStack> decode(ListTag list, int size, ServerPlayer p) throws IOException {
        var result = new LinkedHashMap<Integer, ItemStack>();
        var ops = RegistryOps.create(NbtOps.INSTANCE, p.registryAccess());
        for (int i = 0; i < list.size(); i++) {
            CompoundTag entry = list.getCompoundOrEmpty(i);
            int slot = entry.getIntOr("slot", -1);
            Tag item = entry.get("item");
            if (slot < 0 || slot >= size || item == null || result.containsKey(slot)) throw new IOException("Invalid inventory slot");
            result.put(slot, ItemStack.CODEC.parse(ops, item).getOrThrow());
        }
        return result;
    }

    public static CompoundTag snapshot(ServerPlayer p) {
        var tag = new CompoundTag();
        tag.putInt("version", 1);
        tag.putString("uuid", p.getUUID().toString());
        tag.put("rpg", RpgPlayers.get(p).save());
        tag.putLong("savedAt", System.currentTimeMillis());
        long time = p.level().getGameTime();
        long ticks = p.level().getServer().getTickCount();
        var cooldowns = new CompoundTag();
        RpgPlayers.get(p).cooldowns.forEach((id, end) -> cooldowns.putLong(id, Math.max(0, end - time)));
        tag.put("cooldowns", cooldowns);
        var buffs = new CompoundTag();
        RpgPlayers.get(p).buffs.forEach((type, buff) -> {
            var entry = new CompoundTag(); entry.putInt("remaining", buff.remaining(time));
            entry.putDouble("magnitude", buff.magnitude); entry.putDouble("magnitude2", buff.magnitude2);
            entry.putInt("total", buff.totalTicks); buffs.put(type.name(), entry);
        });
        tag.put("buffs", buffs);
        tag.putLong("mountCooldown", Math.max(0, RpgPlayers.get(p).nextMountTick - ticks));
        tag.putLong("pvpCooldown", Math.max(0, RpgPlayers.get(p).pvpUntilTick - ticks));
        tag.put("inventory", items(p.getInventory(), p));
        tag.put("ender", items(p.getEnderChestInventory(), p));
        var equipment = new CompoundTag();
        var ops = RegistryOps.create(NbtOps.INSTANCE, p.registryAccess());
        for (EquipmentSlot slot : EQUIPMENT) {
            ItemStack item = p.getItemBySlot(slot);
            if (!item.isEmpty()) equipment.put(slot.getName(), ItemStack.CODEC.encodeStart(ops, item).getOrThrow());
        }
        tag.put("equipment", equipment);
        tag.put("bank", com.mmorpg.economy.BankData.Account.CODEC.encodeStart(ops,
                com.mmorpg.economy.BankData.get(p.level().getServer()).account(p.getUUID())).getOrThrow());
        ItemStack cursor = p.containerMenu.getCarried();
        if (!cursor.isEmpty()) tag.put("cursor", ItemStack.CODEC.encodeStart(ops, cursor).getOrThrow());
        tag.putInt("selected", p.getInventory().getSelectedSlot());
        var hd = ClassTrial.data(p);
        tag.putBoolean("cinematic", hd.cinematicSeen);
        tag.putInt("tried", hd.triedMask);
        tag.putString("destination", TRAVEL.getOrDefault(p.getUUID(), ""));
        if (DungeonTravel.point(p) != null) tag.put("dungeonReturn", DungeonTravel.point(p).save());
        return tag;
    }

    public static void restore(ServerPlayer p, CompoundTag tag) throws IOException {
        var returnPoint = tag.getCompound("dungeonReturn").map(DungeonTravel.ReturnPoint::read).orElse(null);
        // Decode everything before clearing a single slot. Invalid/missing items fail the login.
        CompoundTag rpg = tag.getCompound("rpg").orElseThrow(() -> new IOException("Missing RPG data"));
        var inventory = decode(tag.getList("inventory").orElseThrow(() -> new IOException("Missing inventory")), p.getInventory().getContainerSize(), p);
        var ender = decode(tag.getList("ender").orElseThrow(() -> new IOException("Missing ender chest")), p.getEnderChestInventory().getContainerSize(), p);
        var ops = RegistryOps.create(NbtOps.INSTANCE, p.registryAccess());
        var eq = new EnumMap<EquipmentSlot, ItemStack>(EquipmentSlot.class);
        var equipment = tag.getCompound("equipment").orElseThrow(() -> new IOException("Missing equipment"));
        for (EquipmentSlot slot : EQUIPMENT) {
            Tag item = equipment.get(slot.getName());
            eq.put(slot, item == null ? ItemStack.EMPTY : ItemStack.CODEC.parse(ops, item).getOrThrow());
        }
        Tag carried = tag.get("cursor");
        ItemStack cursor = carried == null ? ItemStack.EMPTY : ItemStack.CODEC.parse(ops, carried).getOrThrow();
        var parsed = new com.mmorpg.rpg.PlayerData(); parsed.load(rpg);
        long elapsed = Math.max(0, (System.currentTimeMillis() - tag.getLongOr("savedAt", System.currentTimeMillis())) / 50);
        long time = p.level().getGameTime();
        var cooldowns = tag.getCompoundOrEmpty("cooldowns");
        for (String id : cooldowns.keySet()) {
            long remaining = cooldowns.getLongOr(id, 0) - elapsed;
            if (remaining > 0) parsed.cooldowns.put(id, time + remaining);
        }
        var buffs = tag.getCompoundOrEmpty("buffs");
        for (String id : buffs.keySet()) {
            var type = com.mmorpg.rpg.BuffType.valueOf(id);
            var entry = buffs.getCompoundOrEmpty(id);
            long remaining = entry.getIntOr("remaining", 0) - elapsed;
            if (remaining > 0) parsed.buffs.put(type, new com.mmorpg.rpg.Buff(type, entry.getDoubleOr("magnitude", 0), entry.getDoubleOr("magnitude2", 0), time + remaining, entry.getIntOr("total", 0)));
        }
        parsed.nextMountTick = p.level().getServer().getTickCount() + Math.max(0, tag.getLongOr("mountCooldown", 0) - elapsed);
        parsed.pvpUntilTick = p.level().getServer().getTickCount() + Math.max(0, tag.getLongOr("pvpCooldown", 0) - elapsed);
        Tag bankTag = tag.get("bank");
        if (bankTag == null) throw new IOException("Missing bank account");
        var bank = com.mmorpg.economy.BankData.Account.CODEC.parse(ops, bankTag).getOrThrow();
        p.getInventory().clearContent();
        inventory.forEach((i, item) -> p.getInventory().setItem(i, item));
        p.getEnderChestInventory().clearContent();
        ender.forEach((i, item) -> p.getEnderChestInventory().setItem(i, item));
        eq.forEach(p::setItemSlot);
        if (!cursor.isEmpty() && !p.getInventory().add(cursor)) throw new IOException("Cannot restore held inventory item");
        p.getInventory().setSelectedSlot(Math.clamp(tag.getIntOr("selected", 0), 0, 8));
        p.setData(ModAttachments.PLAYER, parsed);
        DungeonTravel.restore(p, returnPoint);
        com.mmorpg.economy.BankData.get(p.level().getServer()).replaceAccount(p.getUUID(), bank);
        var hd = ClassTrial.data(p);
        hd.inTrial = false; hd.snapshot = null; hd.inventory = null;
        hd.cinematicSeen = tag.getBooleanOr("cinematic", false);
        hd.triedMask = tag.getIntOr("tried", 0);
        p.inventoryMenu.broadcastChanges();
    }

    public static boolean save(ServerPlayer p) {
        ProfileStore store = SESSIONS.get(p.getUUID());
        if (store == null) return !NetworkConfig.current.enabled;
        // Never persist borrowed trial equipment as a real character.
        if (ClassTrial.active(p)) return true;
        try {
            var bytes = new ByteArrayOutputStream();
            NbtIo.writeCompressed(snapshot(p), bytes);
            store.write(bytes.toByteArray());
            return true;
        } catch (Exception e) {
            MMORPG.LOGGER.error("[Aeloria] Failed to save shared character {}", p.getUUID(), e);
            p.sendSystemMessage(Component.literal("Sauvegarde partagée impossible. Changement de serveur bloqué ; prévenez un administrateur.").withColor(0xFF6060));
            return false;
        }
    }

    public static void logout(ServerPlayer p) {
        if (!SESSIONS.containsKey(p.getUUID())) return;
        p.closeContainer();
        if (ClassTrial.active(p)) ClassTrial.end(p, false, null);
        save(p);
        ProfileStore store = SESSIONS.remove(p.getUUID());
        try { store.close(); } catch (IOException e) { MMORPG.LOGGER.error("[Aeloria] Session release", e); }
        TRAVEL.remove(p.getUUID()); TRANSFERRING.remove(p.getUUID());
        ARRIVALS.remove(p.getUUID());
        PORTAL_GRACE.remove(p.getUUID());
        DungeonTravel.forget(p);
    }

    public static void tick(net.minecraft.server.MinecraftServer server) {
        if (!NetworkConfig.current.enabled || server.getTickCount() % 600 != 0) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) save(p);
    }

    /** True means this destination belongs to another server, including a refused/failed transfer. */
    public static boolean route(ServerPlayer p, NetworkConfig.Role role, String destination) {
        if (NetworkConfig.hosts(role)) return false;
        boolean admin = destination.startsWith("admin-") && p.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
        if (!canTransfer(p, role, admin)) return true;
        String target = NetworkConfig.serverFor(role);
        MMORPG.LOGGER.info("[Aeloria voyage] Départ {} : {} -> {} ({})", p.getUUID(), NetworkConfig.current.role, role, destination);
        p.closeContainer();
        com.mmorpg.server.MountManager.despawn(p);
        com.mmorpg.server.PetManager.despawn(p);
        TRAVEL.put(p.getUUID(), destination);
        if (!save(p)) { TRAVEL.remove(p.getUUID()); return true; }
        TRANSFERRING.put(p.getUUID(), System.currentTimeMillis() + 10_000);
        Net.toPlayer(p, ProxyConnect.to(target));
        Scheduler.later(p.level(), 200, () -> {
            if (p.level().getServer().getPlayerList().getPlayer(p.getUUID()) == p) {
                TRAVEL.remove(p.getUUID()); TRANSFERRING.remove(p.getUUID()); save(p);
                p.sendSystemMessage(Component.literal("Le serveur de destination n'a pas répondu. Votre personnage est resté ici."));
            }
        });
        return true;
    }

    public static boolean canTransfer(ServerPlayer p, NetworkConfig.Role role) { return canTransfer(p,role,false); }
    private static boolean canTransfer(ServerPlayer p, NetworkConfig.Role role, boolean admin) {
        if(com.mmorpg.competition.ArenaManager.active(p)||com.mmorpg.competition.GuildRaids.of(p.getUUID())!=null){p.sendSystemMessage(Component.literal("Quitte ton arène ou ton épreuve de guilde avant de voyager."));return false;}
        if (!SESSIONS.containsKey(p.getUUID())) return false;
        if (!admin && role != NetworkConfig.Role.HUB && !RpgPlayers.get(p).playerClass.isPlayable()) {
            p.sendSystemMessage(Component.literal("Choisissez votre classe au hub avant de voyager."));
            return false;
        }
        if (ClassTrial.active(p) || com.mmorpg.hub.CinematicManager.isLocked(p)) {
            p.sendSystemMessage(Component.literal("Terminez votre essai ou votre cinématique avant de changer de serveur."));
            return false;
        }
        if (RpgPlayers.get(p).pvpUntilTick > p.level().getServer().getTickCount()) {
            p.sendSystemMessage(Component.literal("Impossible de changer de serveur pendant un combat contre un joueur."));
            return false;
        }
        long now = System.currentTimeMillis();
        return TRANSFERRING.getOrDefault(p.getUUID(), 0L) <= now;
    }
    public static boolean inTransit(ServerPlayer p) { return TRANSFERRING.getOrDefault(p.getUUID(), 0L) > System.currentTimeMillis(); }
    /** Automatic portals must not interpret the backend's old saved position as a fresh entry. */
    public static boolean portalBlocked(ServerPlayer p) {
        return isArriving(p) || inTransit(p)
                || PORTAL_GRACE.getOrDefault(p.getUUID(), 0L) > p.level().getServer().getTickCount()
                || com.mmorpg.mine.MineManager.transitioning(p) || com.mmorpg.plains.PlainsManager.transitioning(p);
    }
    public static boolean isArriving(ServerPlayer p) { return ARRIVALS.containsKey(p.getUUID()); }

    private static void arrive(ServerPlayer p, String destination) {
        try {
            if (!destination.startsWith("admin-") && !RpgPlayers.get(p).playerClass.isPlayable() && NetworkConfig.current.role != NetworkConfig.Role.HUB) {
                route(p, NetworkConfig.Role.HUB, "hub");
                return;
            }
            if (destination.startsWith("admin-tp:")) {
                AdminTravel.arrive(p, destination.substring(9));
            } else if (destination.equals("dungeon-return")) {
                DungeonTravel.arriveHome(p);
            } else if (((destination.startsWith("arena:")||destination.startsWith("arena-match:"))||(destination.startsWith("guild-raid")||destination.equals("guild-hall"))||destination.startsWith("admin-test:")) && NetworkConfig.hosts(NetworkConfig.Role.DUNGEONS)) {
                com.mmorpg.competition.CompetitionTravel.arrive(p,destination);
            } else if (destination.startsWith("mine:") && NetworkConfig.hosts(NetworkConfig.Role.MINES)) {
                int region = Integer.parseInt(destination.substring(5));
                com.mmorpg.server.PortalAccess.grant(p,"mine");
                if (region >= 0 && region < com.mmorpg.mine.MineLayout.SECTORS && region < RpgPlayers.get(p).minesUnlocked) com.mmorpg.mine.MineManager.startTransition(p, region);
            } else if (destination.startsWith("plains:") && NetworkConfig.hosts(NetworkConfig.Role.PLAINS)) {
                com.mmorpg.server.PortalAccess.grant(p,"plaines");
                com.mmorpg.plains.PlainsManager.enterChosen(p, Integer.parseInt(destination.substring(7)));
            } else if (destination.startsWith("dungeon:") && NetworkConfig.hosts(NetworkConfig.Role.DUNGEONS)) {
                String pool = destination.substring(8);
                if (!com.mmorpg.dungeon.InstanceManager.validPool(pool) || !com.mmorpg.dungeon.InstanceManager.enterArriving(p, pool)) {
                    p.sendSystemMessage(Component.literal("Aucune instance disponible ou niveau insuffisant : retour à votre entrée."));
                    DungeonTravel.returnToSource(p);
                }
            } else if (destination.equals("hub") && NetworkConfig.hosts(NetworkConfig.Role.HUB)) com.mmorpg.hub.HubManager.goToHub(p);
            else if (destination.equals("spawn") && NetworkConfig.hosts(NetworkConfig.Role.MAIN)) com.mmorpg.hub.HubManager.goToSpawn(p);
            save(p); // consume the travel instruction; later reconnects retain their local position
        } catch (Exception e) { MMORPG.LOGGER.error("[Aeloria] Invalid travel instruction {}", destination, e); }
        finally { ARRIVALS.remove(p.getUUID()); }
    }
}
