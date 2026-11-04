package com.mmorpg.server;

import com.mmorpg.network.Net;
import com.mmorpg.network.Payloads;
import com.mmorpg.rpg.PlayerData;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Confort de jeu cote serveur : tri d'inventaire, pings de groupe, compteur de degats et journal de jeu. */
public final class Qol {
    public static final String DEALT = "j_dmg_dealt", TAKEN = "j_dmg_taken", DEATHS = "j_deaths", BEST_HIT = "j_best_hit", MINUTES = "j_minutes";
    private static final int WINDOW = 200;           // fenetre glissante du DPS : 10 s
    private static final Map<UUID, Fight> FIGHTS = new HashMap<>();
    private static final Map<UUID, Long> LAST_PING = new HashMap<>();

    private static final class Fight {
        final List<long[]> hits = new ArrayList<>();      // {tick, degats arrondis}
        double total;
        long last;
        boolean idleSent = true;
    }

    private Qol() {
    }

    // ------------------------------------------------------------------ actions du client

    public static void action(ServerPlayer p, CompoundTag t) {
        switch (t.getStringOr("op", "")) {
            case "sort_inv" -> sortInventory(p);
            case "sort_box" -> sortContainer(p);
            case "ping" -> ping(p, t.getIntOr("x", 0), t.getIntOr("y", 0), t.getIntOr("z", 0));
            case "journal" -> RpgPlayers.sync(p);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ tri

    public static void sortInventory(ServerPlayer p) {
        Inventory inv = p.getInventory();
        List<ItemStack> stacks = new ArrayList<>();
        for (int i = 9; i < 36; i++) stacks.add(inv.getItem(i));
        List<ItemStack> sorted = sorted(stacks, 27);
        for (int i = 0; i < 27; i++) inv.setItem(9 + i, sorted.get(i));
        p.containerMenu.broadcastChanges();
    }

    public static void sortContainer(ServerPlayer p) {
        if (p.containerMenu == p.inventoryMenu) return;
        List<Slot> slots = new ArrayList<>();
        for (Slot s : p.containerMenu.slots) if (!(s.container instanceof Inventory)) slots.add(s);
        List<ItemStack> stacks = new ArrayList<>();
        for (Slot s : slots) stacks.add(s.getItem());
        List<ItemStack> sorted = sorted(stacks, slots.size());
        for (int i = 0; i < slots.size(); i++) slots.get(i).set(sorted.get(i));
        p.containerMenu.broadcastChanges();
    }

    /** Fusionne les piles identiques puis trie par objet (identifiant), de la plus grosse pile a la plus petite. */
    static List<ItemStack> sorted(List<ItemStack> in, int size) {
        List<ItemStack> merged = new ArrayList<>();
        for (ItemStack s : in) {
            if (s.isEmpty()) continue;
            ItemStack rest = s.copy();
            for (ItemStack m : merged) {
                if (rest.isEmpty()) break;
                if (m.getCount() < m.getMaxStackSize() && ItemStack.isSameItemSameComponents(m, rest)) {
                    int move = Math.min(rest.getCount(), m.getMaxStackSize() - m.getCount());
                    m.grow(move);
                    rest.shrink(move);
                }
            }
            if (!rest.isEmpty()) merged.add(rest);
        }
        merged.sort(Comparator.comparing((ItemStack s) -> BuiltInRegistries.ITEM.getKey(s.getItem()).toString())
                .thenComparing(s -> -s.getCount()));
        while (merged.size() < size) merged.add(ItemStack.EMPTY);
        return merged;
    }

    // ------------------------------------------------------------------ pings de groupe

    private static void ping(ServerPlayer p, int x, int y, int z) {
        long now = p.level().getServer().getTickCount();
        Long last = LAST_PING.get(p.getUUID());
        if (last != null && now - last < 15) return;
        if (p.distanceToSqr(x + .5, y + .5, z + .5) > 160 * 160) return;
        LAST_PING.put(p.getUUID(), now);
        CompoundTag t = new CompoundTag();
        t.putString("kind", "ping");
        t.putString("name", p.getName().getString());
        t.putInt("x", x);
        t.putInt("y", y);
        t.putInt("z", z);
        Payloads.QolSync packet = new Payloads.QolSync(t);
        Net.toPlayer(p, packet);
        for (ServerPlayer m : PartyManager.onlineMembers(p)) {
            if (m != p && m.level() == p.level()) Net.toPlayer(m, packet);
        }
    }

    // ------------------------------------------------------------------ DPS et journal

    /** Degats infliges par un joueur (appele par le moteur de combat). */
    public static void dealt(ServerPlayer p, double amount) {
        if (amount <= 0) return;
        long now = p.level().getServer().getTickCount();
        Fight f = FIGHTS.computeIfAbsent(p.getUUID(), u -> new Fight());
        if (now - f.last > 300) {
            f.total = 0;
            f.hits.clear();
        }
        f.hits.add(new long[]{now, Math.round(amount)});
        f.total += amount;
        f.last = now;
        f.idleSent = false;
        PlayerData d = RpgPlayers.get(p);
        d.counters.merge(DEALT, Math.round(amount), Long::sum);
        long best = Math.round(amount);
        if (d.counters.getOrDefault(BEST_HIT, 0L) < best) d.counters.put(BEST_HIT, best);
    }

    public static void taken(ServerPlayer p, double amount) {
        if (amount > 0) RpgPlayers.get(p).counters.merge(TAKEN, Math.round(amount), Long::sum);
    }

    public static void died(ServerPlayer p) {
        PlayerData d = RpgPlayers.get(p);
        d.counters.merge(DEATHS, 1L, Long::sum);
        d.dirty = true;
    }

    /** Appele a chaque tick du joueur. */
    public static void tick(ServerPlayer p) {
        if (p.tickCount % 1200 == 0 && p.tickCount > 0) {
            PlayerData d = RpgPlayers.get(p);
            d.counters.merge(MINUTES, 1L, Long::sum);
            d.dirty = true;
        }
        if (p.tickCount % 20 != 0) return;
        Fight f = FIGHTS.get(p.getUUID());
        if (f == null) return;
        long now = p.level().getServer().getTickCount();
        f.hits.removeIf(h -> now - h[0] > WINDOW);
        boolean idle = f.hits.isEmpty();
        if (idle && f.idleSent) return;
        double sum = 0;
        for (long[] h : f.hits) sum += h[1];
        CompoundTag t = new CompoundTag();
        t.putString("kind", "dps");
        t.putString("name", p.getName().getString());
        t.putFloat("dps", (float) (sum / (WINDOW / 20.0)));
        t.putLong("total", Math.round(f.total));
        Payloads.QolSync packet = new Payloads.QolSync(t);
        Net.toPlayer(p, packet);
        for (ServerPlayer m : PartyManager.onlineMembers(p)) if (m != p) Net.toPlayer(m, packet);
        if (idle) f.idleSent = true;
    }

    public static void forget(UUID id) {
        FIGHTS.remove(id);
        LAST_PING.remove(id);
    }
}
