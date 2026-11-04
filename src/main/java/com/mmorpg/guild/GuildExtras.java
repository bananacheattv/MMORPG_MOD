package com.mmorpg.guild;

import com.mmorpg.MMORPG;
import com.mmorpg.registry.ModItems;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.item.ItemStack;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.IsoFields;
import java.util.*;

/** Extras de guilde : quêtes communes, trophées, boutique, coffre d'objets et activité des membres. */
public final class GuildExtras {
    private GuildExtras() {}

    // ---------------------------------------------------------------- quêtes
    public static final String[] TYPES = {"KILLS", "BOSS", "GOLD", "XP"};
    public static final String[] TYPE_LABELS = {"Vaincre %d monstres", "Vaincre %d boss", "Déposer %d or en banque", "Gagner %d XP"};
    private static final List<Object[]> NOTICES = new ArrayList<>();

    public static String label(GuildStore.Quest q) {
        int i = Math.max(0, Arrays.asList(TYPES).indexOf(q.type));
        return String.format(TYPE_LABELS[i], q.target);
    }

    private static long round(double value, long step) { return Math.max(step, Math.round(value / step) * step); }

    private static GuildStore.Quest make(GuildStore.Guild g, String type, boolean weekly) {
        var q = new GuildStore.Quest();
        q.type = type; q.weekly = weekly;
        double members = Math.pow(Math.max(1, g.members.size()), 0.8), level = g.level, mult = weekly ? 5 : 1;
        q.target = switch (type) {
            case "KILLS" -> round(40 * members * (1 + level * 0.2) * mult, 10);
            case "BOSS" -> Math.max(1, Math.round((2 + g.members.size() / 5.0) * (weekly ? 3 : 1)));
            case "GOLD" -> round(500 * members * level * mult, 100);
            default -> round(3000 * members * (1 + level * 0.5) * mult, 100);
        };
        q.rewardXp = (long) (200 * (1 + level / 2) * mult);
        q.rewardGold = (long) (500 * level * mult);
        return q;
    }

    /** Renouvelle les quêtes quotidiennes (3) et hebdomadaire (1) quand la date change (UTC). */
    public static void roll(GuildStore.Guild g) {
        var today = LocalDate.now(ZoneOffset.UTC);
        String day = today.toString(), week = today.get(IsoFields.WEEK_BASED_YEAR) + "-W" + today.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        if (!day.equals(g.questDay)) {
            g.quests.removeIf(q -> !q.weekly);
            var pool = new ArrayList<>(Arrays.asList(TYPES));
            Collections.shuffle(pool, new Random(g.id.getLeastSignificantBits() ^ day.hashCode()));
            for (int i = 0; i < 3; i++) g.quests.add(make(g, pool.get(i), false));
            g.questDay = day;
        }
        if (!week.equals(g.questWeek)) {
            g.quests.removeIf(q -> q.weekly);
            g.quests.add(make(g, TYPES[new Random(g.id.getLeastSignificantBits() ^ week.hashCode()).nextInt(TYPES.length)], true));
            g.questWeek = week;
        }
        trophies(g);
    }

    /** Ajoute une contribution d'un membre (appel sous verrou d'écriture du registre). */
    public static void add(GuildStore.Guild g, UUID member, String type, long amount) {
        if (amount <= 0 || !g.members.containsKey(member)) return;
        roll(g);
        var a = g.activity.computeIfAbsent(member, k -> new GuildStore.Activity());
        switch (type) {
            case "KILLS" -> a.kills = (int) Math.min(Integer.MAX_VALUE, a.kills + amount);
            case "GOLD" -> a.gold += amount;
            case "XP" -> a.xp += amount;
            default -> {}
        }
        for (var q : g.quests) {
            if (q.done || !q.type.equals(type)) continue;
            q.progress = Math.min(q.target, q.progress + amount);
            if (q.progress >= q.target) {
                q.done = true;
                g.questsDone++;
                a.quests++;
                if (g.level < 10) g.xp = Math.min(1_000_000_000L, g.xp + q.rewardXp);
                g.gold = Math.min(Long.MAX_VALUE / 4, g.gold + q.rewardGold);
                g.log("Quête de guilde accomplie : " + label(q) + " (+" + q.rewardXp + " XP, +" + q.rewardGold + " or)");
                synchronized (NOTICES) { NOTICES.add(new Object[]{g.id, new HashSet<>(g.members.keySet()), "Quête de guilde accomplie : " + label(q) + " · +" + q.rewardXp + " XP, +" + q.rewardGold + " or en banque."}); }
            }
        }
        trophies(g);
    }

    public static void touch(GuildStore.Guild g, UUID member, long now) {
        g.activity.computeIfAbsent(member, k -> new GuildStore.Activity()).lastSeen = now;
    }

    public static void announce(MinecraftServer server) {
        List<Object[]> list;
        synchronized (NOTICES) { if (NOTICES.isEmpty()) return; list = new ArrayList<>(NOTICES); NOTICES.clear(); }
        for (var n : list) {
            @SuppressWarnings("unchecked") var ids = (Set<UUID>) n[1];
            for (var p : server.getPlayerList().getPlayers())
                if (ids.contains(com.mmorpg.character.CharKey.of(p))) p.sendSystemMessage(Component.literal((String) n[2]).withColor(0x8CE08C));
        }
    }

    // ---------------------------------------------------------------- trophées
    public record Trophy(String id, String label, String hint, boolean boss) {}
    private static List<Trophy> catalog;

    public static synchronized List<Trophy> catalog() {
        if (catalog == null) {
            var list = new ArrayList<Trophy>();
            for (int i = 0; i < 10; i++) list.add(new Trophy("BOSS" + i, Yggdrasil.ALL[i].name(), "Vaincre le gardien " + (i + 1), true));
            list.add(new Trophy("QUESTS_10", "Entraide", "10 quêtes de guilde accomplies", false));
            list.add(new Trophy("QUESTS_50", "Fraternité", "50 quêtes de guilde accomplies", false));
            list.add(new Trophy("QUESTS_100", "Légende vivante", "100 quêtes de guilde accomplies", false));
            list.add(new Trophy("LEVEL_5", "Guilde établie", "Atteindre le niveau 5", false));
            list.add(new Trophy("LEVEL_10", "Guilde souveraine", "Atteindre le niveau 10", false));
            list.add(new Trophy("MEMBERS_10", "Compagnie", "10 membres", false));
            list.add(new Trophy("MEMBERS_25", "Bannière nombreuse", "25 membres", false));
            list.add(new Trophy("MEMBERS_50", "Légion", "50 membres", false));
            list.add(new Trophy("HALL_3", "Hall royal", "Hall amélioré au niveau 3", false));
            list.add(new Trophy("BANK_100K", "Trésor de guerre", "100 000 or en banque", false));
            catalog = List.copyOf(list);
        }
        return catalog;
    }

    private static boolean condition(GuildStore.Guild g, String id) {
        if (id.startsWith("BOSS")) return Yggdrasil.cleared(g, Integer.parseInt(id.substring(4)));
        return switch (id) {
            case "QUESTS_10" -> g.questsDone >= 10; case "QUESTS_50" -> g.questsDone >= 50; case "QUESTS_100" -> g.questsDone >= 100;
            case "LEVEL_5" -> g.level >= 5; case "LEVEL_10" -> g.level >= 10;
            case "MEMBERS_10" -> g.members.size() >= 10; case "MEMBERS_25" -> g.members.size() >= 25; case "MEMBERS_50" -> g.members.size() >= 50;
            case "HALL_3" -> g.hallLevel >= 3; case "BANK_100K" -> g.gold >= 100_000;
            default -> false;
        };
    }

    /** Débloque (définitivement) les trophées dont la condition est remplie ; renvoie vrai si un trophée est nouveau. */
    public static boolean trophies(GuildStore.Guild g) {
        boolean changed = false;
        for (var t : catalog()) if (!g.trophies.contains(t.id()) && condition(g, t.id())) {
            g.trophies.add(t.id()); changed = true; g.log("Trophée débloqué : " + t.label());
        }
        return changed;
    }

    // ---------------------------------------------------------------- boutique
    public record ShopItem(String id, int count, long price, int minLevel) {}
    public static final List<ShopItem> SHOP = List.of(
        new ShopItem("potion_soin_mineure", 5, 500, 1), new ShopItem("potion_mana_mineure", 5, 500, 1),
        new ShopItem("parchemin_teleportation", 1, 1500, 2),
        new ShopItem("potion_soin", 5, 2500, 3), new ShopItem("potion_mana", 5, 2500, 3),
        new ShopItem("potion_soin_majeure", 3, 9000, 6), new ShopItem("potion_mana_majeure", 3, 9000, 6),
        new ShopItem("elixir_experience", 1, 40000, 8));

    /** Prix réel : 5 % de remise par niveau d'amélioration du hall. */
    public static long price(ShopItem item, int hallLevel) { return Math.max(1, item.price() * (100 - 5L * hallLevel) / 100); }

    public static void buy(ServerPlayer p, int index, GuildStore.Guild g) {
        GuildService.require(index >= 0 && index < SHOP.size(), "Article inconnu.");
        var item = SHOP.get(index);
        GuildService.require(g.level >= item.minLevel(), "Débloqué au niveau de guilde " + item.minLevel() + ".");
        var stack = ModItems.get(item.id());
        GuildService.require(stack != null, "Article indisponible.");
        long price = price(item, g.hallLevel);
        GuildService.require(RpgPlayers.get(p).gold >= price, "Or insuffisant : " + price + " or.");
        GuildService.deposit(p, price);          // le prix alimente la banque de guilde (reçu idempotent)
        RpgPlayers.give(p, stack, item.count());
    }

    // ---------------------------------------------------------------- coffre
    private static final Map<UUID, Vault> OPEN = new HashMap<>();

    private static final class Vault extends SimpleContainer {
        final UUID guild; final net.minecraft.core.RegistryAccess registries; boolean ready, dirty; String lastOpener = "?";
        Vault(UUID guild, net.minecraft.core.RegistryAccess registries) { super(27); this.guild = guild; this.registries = registries; }
        @Override public void setChanged() { super.setChanged(); if (ready) dirty = true; }
    }

    private static com.mojang.serialization.DynamicOps<com.google.gson.JsonElement> ops(net.minecraft.core.RegistryAccess r) {
        return net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE, r);
    }

    private static String encode(ItemStack s, net.minecraft.core.RegistryAccess r) {
        return s.isEmpty() ? "" : ItemStack.CODEC.encodeStart(ops(r), s).getOrThrow().toString();
    }

    private static ItemStack decode(String json, net.minecraft.core.RegistryAccess r) {
        if (json == null || json.isEmpty()) return ItemStack.EMPTY;
        return ItemStack.CODEC.parse(ops(r), com.google.gson.JsonParser.parseString(json)).result().orElse(ItemStack.EMPTY);
    }

    private static Map<String, Integer> counts(List<String> entries) {
        var map = new TreeMap<String, Integer>();
        for (var e : entries) {
            if (e == null || e.isEmpty()) continue;
            try {
                var o = com.google.gson.JsonParser.parseString(e).getAsJsonObject();
                map.merge(o.get("id").getAsString(), o.has("count") ? o.get("count").getAsInt() : 1, Integer::sum);
            } catch (RuntimeException ignored) {}
        }
        return map;
    }

    private static void save(MinecraftServer server, Vault v) {
        var list = new ArrayList<String>();
        for (int i = 0; i < 27; i++) list.add(encode(v.getItem(i), v.registries));
        String who = v.lastOpener;
        GuildStore.access(server, true, s -> {
            var g = GuildStore.byId(s, v.guild);
            if (g == null) return null;
            var before = counts(g.vault); var after = counts(list);
            var keys = new TreeSet<>(before.keySet()); keys.addAll(after.keySet());
            for (var k : keys) {
                int d = after.getOrDefault(k, 0) - before.getOrDefault(k, 0);
                if (d != 0) g.log(who + (d > 0 ? " dépose " : " retire ") + Math.abs(d) + "× " + k.replace("mmorpg:", "") + " (coffre)");
            }
            g.vault = list;
            return null;
        });
        v.dirty = false;
    }

    /** Sauvegarde périodique des coffres modifiés ; libère ceux que plus personne ne consulte. */
    public static void tick(MinecraftServer server) {
        for (var it = new ArrayList<>(OPEN.values()).iterator(); it.hasNext(); ) {
            var v = it.next();
            try {
                if (v.dirty) save(server, v);
                if (v.getEntitiesWithContainerOpen().isEmpty()) OPEN.remove(v.guild);
            } catch (Exception e) { MMORPG.LOGGER.warn("Coffre de guilde : sauvegarde reportée ({})", e.getMessage()); }
        }
    }

    public static void flushVaults(MinecraftServer server) {
        for (var v : new ArrayList<>(OPEN.values())) try { if (v.dirty) save(server, v); } catch (Exception e) { MMORPG.LOGGER.error("Coffre de guilde", e); }
        OPEN.clear();
    }

    private static Vault vault(ServerPlayer p, GuildStore.Guild g) {
        var existing = OPEN.get(g.id);
        if (existing != null) return existing;
        var v = new Vault(g.id, p.level().registryAccess());
        for (int i = 0; i < 27; i++) v.setItem(i, decode(i < g.vault.size() ? g.vault.get(i) : "", v.registries));
        v.ready = true; v.dirty = false;
        OPEN.put(g.id, v);
        return v;
    }

    public static void openVault(ServerPlayer p, GuildStore.Guild g, boolean officer) {
        GuildService.require(p.level().dimension().equals(GuildHall.DIM), "Le coffre de guilde se trouve dans le hall de guilde.");
        GuildService.require(officer, "Seuls le chef et les officiers ouvrent le coffre. Les membres déposent l’objet en main.");
        var v = vault(p, g);
        v.lastOpener = com.mmorpg.character.CharKey.name(p);
        p.openMenu(new SimpleMenuProvider((id, inv, pl) -> new ChestMenu(net.minecraft.world.inventory.MenuType.GENERIC_9x3, id, inv, v, 3), Component.literal("Coffre de " + g.name)));
    }

    public static void depositHand(ServerPlayer p, GuildStore.Guild g) {
        GuildService.require(p.level().dimension().equals(GuildHall.DIM), "Le coffre de guilde se trouve dans le hall de guilde.");
        var held = p.getMainHandItem();
        GuildService.require(!held.isEmpty(), "Tiens l’objet à déposer en main.");
        var v = vault(p, g);
        v.lastOpener = com.mmorpg.character.CharKey.name(p);
        var copy = held.copy();
        var left = v.addItem(copy);
        GuildService.require(left.getCount() < held.getCount(), "Coffre plein.");
        held.setCount(left.getCount());
    }

    public static int vaultCount(GuildStore.Guild g) {
        int n = 0;
        for (var s : g.vault) if (s != null && !s.isEmpty()) n++;
        return n;
    }
}
