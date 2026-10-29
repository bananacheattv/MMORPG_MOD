package com.mmorpg.admin;

import com.mmorpg.MMORPG;
import com.mmorpg.config.ConfigManager;
import com.mmorpg.db.Audit;
import com.mmorpg.db.Database;
import com.mmorpg.economy.AuctionData;
import com.mmorpg.economy.BankData;
import com.mmorpg.network.Net;
import com.mmorpg.network.S2COpenScreen;
import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.server.PartyManager;
import com.mmorpg.server.PetManager;
import com.mmorpg.server.RpgPlayers;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongArrayTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.ShortTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.Container;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.ServerChatEvent;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gestion complete d'un joueur par un operateur (/adminjoueurs) : consultation et modification de toutes les donnees RPG (en ligne, ou hors ligne
 * via la base du reseau), inventaire, coffre de l'Ender, banque, hotel des ventes, groupe, guilde et sanctions. Chaque requete est reverifiee
 * cote serveur (permission d'operateur) et chaque modification est journalisee (console, fichier admin-actions.log et journal de la base).
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class PlayerAdmin {
    private PlayerAdmin() {
    }

    private static final int MAX_PATH = 12;
    private static final int MAX_VALUE = 2000;
    private static final Map<UUID, String> MUTED = new ConcurrentHashMap<>();
    private static boolean mutesLoaded;

    // ================================================================== acces

    public static boolean allowed(ServerPlayer p) {
        return p.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("adminjoueurs").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c -> {
            open(c.getSource().getPlayerOrException());
            return 1;
        }));
    }

    @SubscribeEvent
    public static void onChat(ServerChatEvent event) {
        loadMutes();
        if (MUTED.containsKey(event.getPlayer().getUUID())) {
            event.setCanceled(true);
            event.getPlayer().sendSystemMessage(Component.literal("Vous avez été réduit au silence par un administrateur.").withColor(0xFF6B6B));
        }
    }

    public static void open(ServerPlayer p) {
        if (!allowed(p)) return;
        reply(p, list(p, ""));
    }

    // ================================================================== reception

    public static void action(ServerPlayer admin, CompoundTag d) {
        if (!allowed(admin)) {
            MMORPG.LOGGER.warn("[MMORPG] ADMIN-JOUEUR : requete refusee de {} (pas operateur)", admin.getGameProfile().name());
            return;
        }
        String act = d.getStringOr("action", "");
        try {
            switch (act) {
                case "list" -> reply(admin, list(admin, text(d, "q", 32)));
                case "get" -> reply(admin, profile(admin, target(admin, d), ""));
                default -> {
                    Target t = target(admin, d);
                    String msg = switch (act) {
                        case "set", "del", "add", "clear" -> edit(admin, t, d, act);
                        case "act" -> perform(admin, t, d);
                        default -> throw new IllegalArgumentException("Action inconnue.");
                    };
                    reply(admin, profile(admin, t.refresh(admin), msg));
                }
            }
        } catch (IllegalArgumentException e) {
            reply(admin, status(e.getMessage(), false));
        } catch (Exception e) {
            MMORPG.LOGGER.error("[MMORPG] ADMIN-JOUEUR : action {} en echec", act, e);
            reply(admin, status("Action impossible : " + e.getClass().getSimpleName() + " (voir le journal du serveur).", false));
        }
    }

    private static void reply(ServerPlayer p, CompoundTag t) {
        Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.ADMIN_PLAYERS, t));
    }

    private static CompoundTag status(String msg, boolean ok) {
        CompoundTag t = new CompoundTag();
        t.putString("kind", "status");
        t.putString("status", msg);
        t.putBoolean("ok", ok);
        return t;
    }

    private static String text(CompoundTag d, String key, int max) {
        String s = d.getStringOr(key, "");
        require(s.length() <= max, "Texte trop long.");
        return s.strip();
    }

    private static void require(boolean ok, String msg) {
        if (!ok) throw new IllegalArgumentException(msg);
    }

    /** Points d'entree des tests serveur : meme chemin que les requetes du menu, sans reponse reseau. */
    public static String testEdit(ServerPlayer admin, ServerPlayer target, CompoundTag request, String op) {
        Target t = new Target(target.getUUID(), target.getGameProfile().name());
        t.online = target;
        return edit(admin, t, request, op);
    }

    public static String testAct(ServerPlayer admin, ServerPlayer target, CompoundTag request) {
        Target t = new Target(target.getUUID(), target.getGameProfile().name());
        t.online = target;
        return perform(admin, t, request);
    }

    public static CompoundTag testProfile(ServerPlayer admin, ServerPlayer target) {
        Target t = new Target(target.getUUID(), target.getGameProfile().name());
        t.online = target;
        return profile(admin, t, "");
    }

    // ================================================================== cible

    /** Joueur cible : en ligne ({@link #online}) ou hors ligne (profil lu dans la base du reseau). */
    private static final class Target {
        final UUID id;
        String name;
        ServerPlayer online;
        CompoundTag offlineData;

        Target(UUID id, String name) {
            this.id = id;
            this.name = name;
        }

        Target refresh(ServerPlayer admin) {
            ServerPlayer p = admin.level().getServer().getPlayerList().getPlayer(id);
            if (p != null) {
                online = p;
                offlineData = null;
                name = p.getGameProfile().name();
                return this;
            }
            online = null;
            offlineData = loadOffline(id);
            return this;
        }

        PlayerData live() {
            require(online != null, "Le joueur doit être connecté à ce serveur pour cette action.");
            return RpgPlayers.get(online);
        }
    }

    private static Target target(ServerPlayer admin, CompoundTag d) {
        UUID id;
        try {
            id = UUID.fromString(d.getStringOr("id", ""));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Joueur invalide.");
        }
        Target t = new Target(id, d.getStringOr("name", "?"));
        t.refresh(admin);
        if (t.online == null) require(t.offlineData != null, "Profil introuvable (joueur hors ligne et base du réseau indisponible).");
        return t;
    }

    private static CompoundTag loadOffline(UUID id) {
        if (!Database.enabled() || !Database.ready()) return null;
        String[] row = Database.await("lecture du profil (admin)", 4000, c -> {
            try (PreparedStatement ps = c.prepareStatement("SELECT data, name FROM players WHERE uuid = ?")) {
                ps.setString(1, id.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? new String[]{rs.getString(1), rs.getString(2)} : null;
                }
            }
        });
        if (row == null) return null;
        try {
            CompoundTag tag = TagParser.parseCompoundFully(row[0]);
            tag.putString("\u0001name", row[1]);
            return tag;
        } catch (Exception e) {
            MMORPG.LOGGER.warn("[MMORPG] ADMIN-JOUEUR : profil de {} illisible : {}", id, e.getMessage());
            return null;
        }
    }

    // ================================================================== liste

    private static CompoundTag list(ServerPlayer admin, String q) {
        MinecraftServer server = admin.level().getServer();
        Map<UUID, CompoundTag> out = new LinkedHashMap<>();
        String low = q.toLowerCase(java.util.Locale.ROOT);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            String n = p.getGameProfile().name();
            if (!low.isEmpty() && !n.toLowerCase(java.util.Locale.ROOT).contains(low)) continue;
            PlayerData d = RpgPlayers.get(p);
            out.put(p.getUUID(), entry(p.getUUID(), n, d.playerClass.name(), d.level, true));
        }
        if (Database.enabled() && Database.ready()) {
            String like = "%" + q.replaceAll("[%_\\\\]", "") + "%";
            List<String[]> rows = Database.await("liste des joueurs (admin)", 4000, c -> {
                List<String[]> l = new ArrayList<>();
                try (PreparedStatement ps = c.prepareStatement("SELECT uuid, name, class, level FROM players WHERE name LIKE ? ORDER BY last_seen DESC LIMIT 60")) {
                    ps.setString(1, like);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) l.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3), String.valueOf(rs.getInt(4))});
                    }
                }
                return l;
            });
            if (rows != null) {
                for (String[] r : rows) {
                    try {
                        UUID id = UUID.fromString(r[0]);
                        out.computeIfAbsent(id, k -> entry(id, r[1], r[2], Integer.parseInt(r[3]), server.getPlayerList().getPlayer(id) != null));
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        }
        ListTag l = new ListTag();
        out.values().stream().limit(80).forEach(l::add);
        CompoundTag t = new CompoundTag();
        t.putString("kind", "list");
        t.put("players", l);
        t.putBoolean("db", Database.enabled() && Database.ready());
        return t;
    }

    private static CompoundTag entry(UUID id, String name, String cls, int level, boolean online) {
        CompoundTag e = new CompoundTag();
        e.putString("id", id.toString());
        e.putString("name", name);
        e.putString("class", cls);
        e.putInt("level", level);
        e.putBoolean("online", online);
        return e;
    }

    // ================================================================== profil

    private static CompoundTag profile(ServerPlayer admin, Target t, String msg) {
        CompoundTag out = new CompoundTag();
        out.putString("kind", "profile");
        out.putString("id", t.id.toString());
        out.putString("name", t.name);
        out.putBoolean("online", t.online != null);
        out.putBoolean("muted", isMuted(t.id));
        out.putString("status", msg);
        out.putBoolean("ok", true);
        if (t.online != null) {
            PlayerData d = RpgPlayers.get(t.online);
            out.put("data", d.save());
            out.put("inventory", containerList(t.online.getInventory()));
            out.put("ender", containerList(t.online.getEnderChestInventory()));
        } else {
            CompoundTag data = t.offlineData.copy();
            data.remove("\u0001name");
            out.put("data", data);
        }
        MinecraftServer server = admin.level().getServer();
        BankData.Account acc = BankData.get(server).peek(t.id);
        out.putLong("bankGold", acc == null ? 0 : acc.gold);
        if (acc != null) {
            ListTag bank = new ListTag();
            for (int i = 0; i < acc.items.size(); i++) addStack(bank, i, acc.items.get(i));
            out.put("bank", bank);
        }
        ListTag auction = new ListTag();
        for (AuctionData.Listing l : AuctionData.get(server).listings()) {
            if (!l.seller().equals(t.id)) continue;
            CompoundTag e = new CompoundTag();
            e.putLong("id", l.id());
            e.putString("item", itemId(l.stack()) + " x" + l.stack().getCount());
            e.putLong("price", l.price());
            auction.add(e);
        }
        out.put("auction", auction);
        out.putLong("auctionEarnings", AuctionData.get(server).earnings(t.id));
        return out;
    }

    private static ListTag containerList(Container c) {
        ListTag l = new ListTag();
        for (int i = 0; i < c.getContainerSize(); i++) addStack(l, i, c.getItem(i));
        return l;
    }

    private static void addStack(ListTag l, int slot, ItemStack s) {
        if (s.isEmpty()) return;
        CompoundTag e = new CompoundTag();
        e.putInt("slot", slot);
        e.putString("item", itemId(s));
        e.putInt("count", s.getCount());
        l.add(e);
    }

    private static String itemId(ItemStack s) {
        return BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
    }

    // ================================================================== edition generique des donnees

    private static String edit(ServerPlayer admin, Target t, CompoundTag d, String op) {
        List<String> path = new ArrayList<>();
        ListTag p = d.getListOrEmpty("path");
        require(p.size() <= MAX_PATH, "Chemin trop long.");
        for (int i = 0; i < p.size(); i++) path.add(p.getStringOr(i, ""));
        String value = d.getStringOr("value", "");
        String key = d.getStringOr("key", "");
        require(value.length() <= MAX_VALUE && key.length() <= 128, "Valeur trop longue.");
        if (path.isEmpty()) require(op.equals("add"), "Choisissez d'abord un champ.");
        String where = String.join(".", path) + (key.isEmpty() ? "" : "." + key);

        // champs qui demandent la logique du jeu plutot qu'une ecriture brute
        if (t.online != null && op.equals("set") && path.size() == 1) {
            switch (path.get(0)) {
                case "level" -> {
                    RpgPlayers.setLevel(t.online, (int) parseLong(value, 1, com.mmorpg.rpg.LevelSystem.MAX_LEVEL));
                    log(admin, t, "niveau = " + value);
                    return "Niveau modifié.";
                }
                case "class" -> {
                    PlayerClass cls = PlayerClass.byName(value.strip());
                    require(cls.isPlayable() || value.strip().equalsIgnoreCase("none") || value.strip().equalsIgnoreCase("aventurier"), "Classe inconnue (guerrier, mage, archer, paladin, none).");
                    PlayerData live = RpgPlayers.get(t.online);
                    live.resetClass();
                    if (cls.isPlayable()) {
                        live.classChosenOnce = true;
                        RpgPlayers.selectClass(t.online, cls);
                    } else {
                        RpgPlayers.recomputeAndSync(t.online);
                        RpgPlayers.openClassSelection(t.online);
                    }
                    log(admin, t, "classe = " + cls.name());
                    return "Classe : " + cls.label + ".";
                }
                default -> {
                }
            }
        }

        CompoundTag tag = t.online != null ? RpgPlayers.get(t.online).save() : t.offlineData.copy();
        tag.remove("\u0001name");
        Tag result = rec(tag, path, 0, op, value, key);
        require(result instanceof CompoundTag, "Modification invalide.");
        apply(admin, t, (CompoundTag) result);
        log(admin, t, op + " " + where + (op.equals("del") || op.equals("clear") ? "" : " = " + clip(value)));
        return switch (op) {
            case "del" -> "Entrée supprimée.";
            case "add" -> "Entrée ajoutée.";
            case "clear" -> "Contenu vidé.";
            default -> "Valeur enregistrée.";
        };
    }

    private static Tag rec(Tag node, List<String> path, int i, String op, String value, String key) {
        if (i == path.size()) {
            switch (op) {
                case "set":
                    return coerce(node, value);
                case "clear":
                    if (node instanceof CompoundTag) return new CompoundTag();
                    if (node instanceof ListTag) return new ListTag();
                    throw new IllegalArgumentException("Seuls les groupes et listes peuvent être vidés.");
                case "add":
                    if (node instanceof CompoundTag c) {
                        require(!key.isBlank() && key.length() <= 128, "Donnez un nom à la nouvelle entrée.");
                        c.put(key.strip(), coerceNew(c, value));
                        return c;
                    }
                    if (node instanceof ListTag l) {
                        l.add(coerceNew(l, value));
                        return l;
                    }
                    throw new IllegalArgumentException("On ne peut ajouter que dans un groupe ou une liste.");
                default:
                    throw new IllegalArgumentException("Opération invalide.");
            }
        }
        String k = path.get(i);
        boolean last = i == path.size() - 1;
        if (node instanceof CompoundTag c) {
            Tag ch = c.get(k);
            require(ch != null, "Champ introuvable : " + k);
            if (last && op.equals("del")) {
                c.remove(k);
                return c;
            }
            c.put(k, rec(ch, path, i + 1, op, value, key));
            return c;
        }
        if (node instanceof ListTag l) {
            int idx = index(k, l.size());
            if (last && op.equals("del")) {
                l.remove(idx);
                return l;
            }
            l.set(idx, rec(l.get(idx), path, i + 1, op, value, key));
            return l;
        }
        if (node instanceof IntArrayTag a) {
            require(last && op.equals("set"), "Opération invalide sur ce tableau.");
            int[] arr = a.getAsIntArray().clone();
            arr[index(k, arr.length)] = (int) parseLong(value, Integer.MIN_VALUE, Integer.MAX_VALUE);
            return new IntArrayTag(arr);
        }
        if (node instanceof LongArrayTag a) {
            require(last && op.equals("set"), "Opération invalide sur ce tableau.");
            long[] arr = a.getAsLongArray().clone();
            arr[index(k, arr.length)] = parseLong(value, Long.MIN_VALUE, Long.MAX_VALUE);
            return new LongArrayTag(arr);
        }
        throw new IllegalArgumentException("Chemin invalide.");
    }

    private static int index(String k, int size) {
        try {
            int i = Integer.parseInt(k);
            require(i >= 0 && i < size, "Position hors limites.");
            return i;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Position invalide.");
        }
    }

    private static long parseLong(String v, long min, long max) {
        String s = v.strip();
        try {
            long n = Long.parseLong(s);
            require(n >= min && n <= max, "Valeur hors limites (" + min + " à " + max + ").");
            return n;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Nombre attendu.");
        }
    }

    private static double parseDouble(String v) {
        try {
            double n = Double.parseDouble(v.strip().replace(',', '.'));
            require(!Double.isNaN(n) && !Double.isInfinite(n), "Nombre invalide.");
            return n;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Nombre attendu.");
        }
    }

    /** Convertit le texte saisi dans le type de l'ancienne valeur. */
    private static Tag coerce(Tag old, String v) {
        if (old instanceof StringTag) return StringTag.valueOf(v);
        if (old instanceof ByteTag) {
            String s = v.strip().toLowerCase(java.util.Locale.ROOT);
            if (s.equals("true") || s.equals("oui") || s.equals("1")) return ByteTag.valueOf((byte) 1);
            if (s.equals("false") || s.equals("non") || s.equals("0")) return ByteTag.valueOf((byte) 0);
            return ByteTag.valueOf((byte) parseLong(v, Byte.MIN_VALUE, Byte.MAX_VALUE));
        }
        if (old instanceof ShortTag) return ShortTag.valueOf((short) parseLong(v, Short.MIN_VALUE, Short.MAX_VALUE));
        if (old instanceof IntTag) return IntTag.valueOf((int) parseLong(v, Integer.MIN_VALUE, Integer.MAX_VALUE));
        if (old instanceof LongTag) return LongTag.valueOf(parseLong(v, Long.MIN_VALUE, Long.MAX_VALUE));
        if (old instanceof FloatTag) return FloatTag.valueOf((float) parseDouble(v));
        if (old instanceof DoubleTag) return DoubleTag.valueOf(parseDouble(v));
        throw new IllegalArgumentException("Ce champ ne se modifie pas directement : ouvrez-le.");
    }

    /** Nouvelle entree : meme type que les entrees voisines, sinon nombre entier ou texte. */
    private static Tag coerceNew(Tag container, String v) {
        Tag sample = null;
        if (container instanceof CompoundTag c) {
            for (String k : c.keySet()) {
                Tag x = c.get(k);
                if (x instanceof NumericTag || x instanceof StringTag) {
                    sample = x;
                    break;
                }
            }
        } else if (container instanceof ListTag l && !l.isEmpty()) {
            sample = l.get(0);
        }
        if (sample != null) return coerce(sample, v);
        try {
            long n = Long.parseLong(v.strip());
            return n >= Integer.MIN_VALUE && n <= Integer.MAX_VALUE ? IntTag.valueOf((int) n) : LongTag.valueOf(n);
        } catch (NumberFormatException e) {
            return StringTag.valueOf(v);
        }
    }

    /** Valide le profil modifie (les valeurs sont bornees au chargement) puis l'applique au joueur en ligne ou a la base. */
    private static void apply(ServerPlayer admin, Target t, CompoundTag tag) {
        PlayerData probe = new PlayerData();
        probe.load(tag);
        CompoundTag clean = probe.save();
        if (t.online != null) {
            PlayerData live = RpgPlayers.get(t.online);
            live.load(clean);
            if (live.playerClass.isPlayable()) live.unlockSkills();
            RpgPlayers.recomputeAndSync(t.online);
            return;
        }
        writeOffline(t.id, probe, clean);
    }

    private static void writeOffline(UUID id, PlayerData d, CompoundTag clean) {
        clean.remove("dbVersion");
        String snbt = clean.toString();
        Boolean ok = Database.await("ecriture du profil (admin)", 5000, c -> {
            try (PreparedStatement ps = c.prepareStatement("UPDATE players SET class = ?, level = ?, xp = ?, gold = ?, mines_unlocked = ?, plains_unlocked = ?, evolution = ?, "
                    + "data = ?, version = version + 1 WHERE uuid = ? AND online = 0")) {
                int i = 1;
                ps.setString(i++, d.playerClass.name());
                ps.setInt(i++, d.level);
                ps.setLong(i++, d.xp);
                ps.setLong(i++, d.gold);
                ps.setInt(i++, d.minesUnlocked);
                ps.setInt(i++, d.plainsUnlocked);
                ps.setInt(i++, d.evolution);
                ps.setString(i++, snbt);
                ps.setString(i, id.toString());
                return ps.executeUpdate() == 1;
            }
        });
        require(Boolean.TRUE.equals(ok), "Écriture impossible : le joueur vient de se connecter ou la base ne répond pas.");
    }

    // ================================================================== actions

    private static String perform(ServerPlayer admin, Target t, CompoundTag d) {
        String what = d.getStringOr("what", "");
        String arg = text(d, "arg", 200);
        MinecraftServer server = admin.level().getServer();
        String result;
        switch (what) {
            case "heal" -> {
                t.live();
                RpgPlayers.fullRestore(t.online);
                RpgPlayers.get(t.online).cooldowns.clear();
                RpgPlayers.sync(t.online);
                result = "PV, mana et recharges restaurés.";
            }
            case "kill" -> {
                t.live();
                command(server, admin, "kill " + safeName(t.name));
                result = "Joueur tué.";
            }
            case "kick" -> {
                t.live();
                command(server, admin, "kick " + safeName(t.name) + " " + reason(arg, "Expulsé par un administrateur."));
                result = "Joueur expulsé.";
            }
            case "ban" -> {
                command(server, admin, "ban " + safeName(t.name) + " " + reason(arg, "Banni par un administrateur."));
                result = "Joueur banni.";
            }
            case "pardon" -> {
                command(server, admin, "pardon " + safeName(t.name));
                result = "Bannissement levé.";
            }
            case "mute" -> {
                setMuted(t.id, t.name, true);
                result = "Joueur réduit au silence.";
            }
            case "unmute" -> {
                setMuted(t.id, t.name, false);
                result = "Joueur autorisé à parler.";
            }
            case "tpto" -> {
                t.live();
                command(server, admin, "tp " + safeName(admin.getGameProfile().name()) + " " + safeName(t.name));
                result = "Téléporté auprès du joueur.";
            }
            case "tphere" -> {
                t.live();
                command(server, admin, "tp " + safeName(t.name) + " " + safeName(admin.getGameProfile().name()));
                result = "Joueur téléporté auprès de vous.";
            }
            case "gamemode" -> {
                t.live();
                require(arg.matches("survival|creative|adventure|spectator"), "Mode invalide.");
                command(server, admin, "gamemode " + arg + " " + safeName(t.name));
                result = "Mode de jeu : " + arg + ".";
            }
            case "resetall" -> {
                if (t.online != null) {
                    PlayerData live = RpgPlayers.get(t.online);
                    PetManager.despawn(t.online);
                    live.load(new PlayerData().save());
                    live.hp = -1;
                    live.mana = -1;
                    RpgPlayers.recomputeAndSync(t.online);
                    RpgPlayers.openClassSelection(t.online);
                } else {
                    PlayerData fresh = new PlayerData();
                    writeOffline(t.id, fresh, fresh.save());
                }
                result = "Progression entièrement réinitialisée.";
            }
            case "resetattr" -> {
                CompoundTag tag = base(t);
                PlayerData probe = new PlayerData();
                probe.load(tag);
                probe.resetAttributes();
                apply(admin, t, probe.save());
                result = "Attributs réinitialisés.";
            }
            case "resetclass" -> {
                CompoundTag tag = base(t);
                PlayerData probe = new PlayerData();
                probe.load(tag);
                probe.resetClass();
                if (t.online != null) {
                    PetManager.despawn(t.online);
                    apply(admin, t, probe.save());
                    RpgPlayers.openClassSelection(t.online);
                } else {
                    apply(admin, t, probe.save());
                }
                result = "Classe réinitialisée (nouveau choix de classe).";
            }
            case "resetquests" -> {
                CompoundTag tag = base(t);
                PlayerData probe = new PlayerData();
                probe.load(tag);
                probe.activeQuests.clear();
                probe.questGivers.clear();
                probe.completedQuests.clear();
                probe.dailyQuests.clear();
                probe.trackedQuest = "";
                apply(admin, t, probe.save());
                result = "Quêtes réinitialisées.";
            }
            case "resetjobs" -> {
                CompoundTag tag = base(t);
                PlayerData probe = new PlayerData();
                probe.load(tag);
                probe.jobXp.clear();
                probe.jobClaimed.clear();
                apply(admin, t, probe.save());
                result = "Métiers réinitialisés.";
            }
            case "clearinv" -> {
                t.live();
                t.online.getInventory().clearContent();
                result = "Inventaire vidé.";
            }
            case "clearender" -> {
                t.live();
                t.online.getEnderChestInventory().clearContent();
                result = "Coffre de l'Ender vidé.";
            }
            case "clearbank" -> {
                BankData bank = BankData.get(server);
                BankData.Account acc = bank.peek(t.id);
                require(acc != null, "Ce joueur n'a pas de compte bancaire.");
                for (int i = 0; i < acc.items.size(); i++) acc.items.set(i, ItemStack.EMPTY);
                bank.replaceAccount(t.id, acc);
                result = "Coffre de la banque vidé.";
            }
            case "bankgold" -> {
                long amount = parseLong(arg, 0, 1_000_000_000_000L);
                BankData bank = BankData.get(server);
                BankData.Account acc = bank.account(t.id);
                acc.gold = amount;
                bank.replaceAccount(t.id, acc);
                result = "Or en banque : " + amount + ".";
            }
            case "delslot" -> {
                String where = d.getStringOr("where", "");
                int slot = d.getIntOr("slot", -1);
                switch (where) {
                    case "inv" -> {
                        t.live();
                        require(slot >= 0 && slot < t.online.getInventory().getContainerSize(), "Case invalide.");
                        t.online.getInventory().setItem(slot, ItemStack.EMPTY);
                    }
                    case "ender" -> {
                        t.live();
                        require(slot >= 0 && slot < t.online.getEnderChestInventory().getContainerSize(), "Case invalide.");
                        t.online.getEnderChestInventory().setItem(slot, ItemStack.EMPTY);
                    }
                    case "bank" -> {
                        BankData bank = BankData.get(server);
                        BankData.Account acc = bank.peek(t.id);
                        require(acc != null && slot >= 0 && slot < acc.items.size(), "Case invalide.");
                        acc.items.set(slot, ItemStack.EMPTY);
                        bank.replaceAccount(t.id, acc);
                    }
                    default -> throw new IllegalArgumentException("Conteneur invalide.");
                }
                result = "Objet supprimé.";
            }
            case "give" -> {
                t.live();
                Identifier id = Identifier.tryParse(arg);
                require(id != null && BuiltInRegistries.ITEM.containsKey(id), "Objet inconnu : " + arg);
                Item item = BuiltInRegistries.ITEM.getValue(id);
                int count = d.getIntOr("count", 1);
                require(count >= 1 && count <= 6400, "Quantité : 1 à 6400.");
                RpgPlayers.give(t.online, item, count);
                result = count + " x " + arg + " donné(s).";
            }
            case "cancelauction" -> {
                AuctionData data = AuctionData.get(server);
                AuctionData.Listing l = data.find(d.getLongOr("auction", -1));
                require(l != null && l.seller().equals(t.id), "Annonce introuvable.");
                data.remove(l);
                data.addReturn(t.id, l.stack());
                data.setDirty();
                result = "Annonce annulée (objet rendu au vendeur).";
            }
            case "partyleave" -> {
                t.live();
                PartyManager.leave(t.online);
                result = "Joueur retiré de son groupe.";
            }
            case "guildleave" -> {
                result = leaveGuild(server, t.id);
            }
            default -> throw new IllegalArgumentException("Action inconnue.");
        }
        log(admin, t, what + (arg.isEmpty() ? "" : " " + clip(arg)));
        return result;
    }

    private static CompoundTag base(Target t) {
        CompoundTag tag = t.online != null ? RpgPlayers.get(t.online).save() : t.offlineData.copy();
        tag.remove("\u0001name");
        return tag;
    }

    private static String leaveGuild(MinecraftServer server, UUID id) {
        boolean[] removed = {false};
        com.mmorpg.guild.GuildStore.access(server, true, s -> {
            var g = com.mmorpg.guild.GuildStore.of(s, id);
            if (g == null) return null;
            var m = g.members.remove(id);
            removed[0] = true;
            g.log((m == null ? "Un membre" : m.name) + " a été retiré par un administrateur.");
            if (g.members.isEmpty()) s.guilds.remove(g);
            else if (m != null && !g.members.values().stream().anyMatch(x -> x.rank.equals(m.rank))) g.members.values().iterator().next().rank = m.rank;
            return null;
        });
        require(removed[0], "Ce joueur n'est dans aucune guilde.");
        return "Joueur retiré de sa guilde.";
    }

    private static void command(MinecraftServer server, ServerPlayer admin, String cmd) {
        server.getCommands().performPrefixedCommand(admin.createCommandSourceStack(), cmd);
    }

    private static String safeName(String n) {
        require(n.matches("[A-Za-z0-9_]{1,16}"), "Pseudo invalide.");
        return n;
    }

    private static String reason(String arg, String def) {
        String r = arg.replaceAll("[\\r\\n]", " ").strip();
        return r.isEmpty() ? def : r;
    }

    private static String clip(String s) {
        return s.length() > 80 ? s.substring(0, 80) + "..." : s;
    }

    // ================================================================== sourdine

    private static java.nio.file.Path mutesFile() {
        return ConfigManager.dir().resolve("muted.txt");
    }

    private static synchronized void loadMutes() {
        if (mutesLoaded) return;
        mutesLoaded = true;
        try {
            if (Files.exists(mutesFile())) {
                for (String line : Files.readAllLines(mutesFile(), StandardCharsets.UTF_8)) {
                    String[] p = line.split(" ", 2);
                    try {
                        MUTED.put(UUID.fromString(p[0]), p.length > 1 ? p[1] : "?");
                    } catch (IllegalArgumentException ignored) {
                    }
                }
            }
        } catch (Exception e) {
            MMORPG.LOGGER.warn("[MMORPG] muted.txt illisible : {}", e.getMessage());
        }
    }

    private static boolean isMuted(UUID id) {
        loadMutes();
        return MUTED.containsKey(id);
    }

    private static synchronized void setMuted(UUID id, String name, boolean on) {
        loadMutes();
        if (on) MUTED.put(id, name);
        else MUTED.remove(id);
        try {
            Files.createDirectories(ConfigManager.dir());
            List<String> lines = new ArrayList<>();
            MUTED.forEach((k, v) -> lines.add(k + " " + v));
            Files.write(mutesFile(), lines, StandardCharsets.UTF_8);
        } catch (Exception e) {
            MMORPG.LOGGER.warn("[MMORPG] muted.txt non enregistre : {}", e.getMessage());
        }
    }

    // ================================================================== journal

    private static void log(ServerPlayer admin, Target t, String detail) {
        String line = Instant.now() + " " + admin.getGameProfile().name() + " -> " + t.name + " (" + t.id + ") : " + detail;
        MMORPG.LOGGER.info("[MMORPG] ADMIN-JOUEUR : {}", line);
        try {
            Files.createDirectories(ConfigManager.dir());
            Files.writeString(ConfigManager.dir().resolve("admin-actions.log"), line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            MMORPG.LOGGER.warn("[MMORPG] admin-actions.log non ecrit : {}", e.getMessage());
        }
        Audit.log(t.id, t.name, Audit.ADMIN, 0, admin.getGameProfile().name() + " : " + detail);
    }
}
