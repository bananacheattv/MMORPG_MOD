package com.mmorpg.moderation;

import com.mmorpg.MMORPG;
import com.mmorpg.admin.PlayerAdmin;
import com.mmorpg.db.Database;
import com.mmorpg.dungeon.DungeonSetup;
import com.mmorpg.dungeon.DungeonTemplate;
import com.mmorpg.network.Net;
import com.mmorpg.network.S2COpenScreen;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.server.RpgPlayers;
import com.mmorpg.world.DungeonBuilder.DungeonType;
import net.minecraft.commands.Commands;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Panneau de moderation en jeu (/moderation) : joueurs, sanctions actives, signalements, historique, journal/rejeu d'un joueur, sauvegardes de profil,
 * tableau de bord des connectes, outils (maintenance, planificateur, prison) et donjons. Chaque requete est reverifiee cote serveur (operateur).
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class ModerationPanel {
    private ModerationPanel() {
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        var run = (com.mojang.brigadier.Command<net.minecraft.commands.CommandSourceStack>) c -> {
            open(c.getSource().getPlayerOrException());
            return 1;
        };
        event.getDispatcher().register(Commands.literal("moderation").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(run));
        event.getDispatcher().register(Commands.literal("mod").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(run));
    }

    public static void open(ServerPlayer p) {
        if (!ModUtil.staff(p)) return;
        reply(p, state(p, "players", "", "", "", true));
    }

    private static void reply(ServerPlayer p, CompoundTag t) {
        Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.MODERATION, t));
    }

    // ================================================================== requetes

    public static void action(ServerPlayer admin, CompoundTag d) {
        if (!ModUtil.staff(admin)) {
            MMORPG.LOGGER.warn("[MMORPG] MODERATION : requete refusee de {} (pas operateur)", admin.getGameProfile().name());
            return;
        }
        String act = d.getStringOr("action", "state");
        String tab = d.getStringOr("tab", "players");
        String id = d.getStringOr("id", "");
        String name = d.getStringOr("name", "");
        String q = d.getStringOr("q", "");
        if (id.length() > 40 || name.length() > 40 || q.length() > 40) return;
        String msg = "";
        boolean ok = true;
        try {
            if (!act.equals("state")) {
                msg = perform(admin, act, d, id, name);
                if (msg.startsWith("!")) {
                    ok = false;
                    msg = msg.substring(1);
                }
            }
        } catch (Exception e) {
            MMORPG.LOGGER.error("[MMORPG] MODERATION : action {} en echec", act, e);
            ok = false;
            msg = "Action impossible (" + e.getClass().getSimpleName() + ").";
        }
        reply(admin, state(admin, tab, id, name, q, ok, msg));
    }

    private static String perform(ServerPlayer admin, String act, CompoundTag d, String id, String name) {
        MinecraftServer server = admin.level().getServer();
        String by = admin.getGameProfile().name();
        String arg = d.getStringOr("arg", "").replaceAll("[\\r\\n|]", " ").strip();
        if (arg.length() > 200) arg = arg.substring(0, 200);
        UUID uid = ModUtil.uuid(id);
        ServerPlayer online = uid == null ? null : server.getPlayerList().getPlayer(uid);
        boolean needsTarget = act.matches("warn|mute|unmute|kick|ban|pardon|jail|unjail|snapshot|restore|undo|tpto|tphere");
        if (needsTarget && uid == null) return "!Choisissez d'abord un joueur.";
        if (needsTarget && !name.matches("[A-Za-z0-9_]{1,16}")) return "!Pseudo invalide.";
        switch (act) {
            case "warn" -> {
                if (online == null) return "!Le joueur doit être connecté pour être averti.";
                if (arg.isEmpty()) return "!Indiquez la raison de l'avertissement.";
                ModUtil.tell(online, "⚠ Avertissement de l'équipe : " + arg, 0xFFB347);
                online.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal("⚠ Avertissement").withColor(0xFFB347)));
                ModHistory.record(by, uid, name, "avertissement", arg);
                return "Avertissement envoyé à " + name + ".";
            }
            case "mute" -> {
                PlayerAdmin.mute(uid, name, true);
                ModHistory.record(by, uid, name, "mute", arg);
                return name + " est réduit au silence.";
            }
            case "unmute" -> {
                PlayerAdmin.mute(uid, name, false);
                ModHistory.record(by, uid, name, "unmute", arg);
                return name + " peut de nouveau parler.";
            }
            case "kick" -> {
                if (online == null) return "!Le joueur n'est pas connecté.";
                String why = arg.isEmpty() ? "Expulsé par un administrateur." : arg;
                online.connection.disconnect(Component.literal(why));
                ModHistory.record(by, uid, name, "kick", why);
                return name + " expulsé.";
            }
            case "ban" -> {
                String why = arg.isEmpty() ? "Banni par un administrateur." : arg;
                server.getCommands().performPrefixedCommand(admin.createCommandSourceStack(), "ban " + name + " " + why);
                ModHistory.record(by, uid, name, "ban", why);
                return name + " banni.";
            }
            case "pardon" -> {
                server.getCommands().performPrefixedCommand(admin.createCommandSourceStack(), "pardon " + name);
                ModHistory.record(by, uid, name, "pardon", arg);
                return "Bannissement de " + name + " levé.";
            }
            case "jail" -> {
                long ms = ModUtil.parseDuration(d.getStringOr("dur", ""));
                return Jail.jail(by, uid, name, online, ms, arg);
            }
            case "unjail" -> {
                return Jail.release(by, uid, name, online, false);
            }
            case "jailzone" -> {
                return d.getStringOr("do", "set").equals("rayon")
                        ? Jail.setRadius(parseNum(d.getStringOr("arg", ""), 12))
                        : Jail.setZone(admin);
            }
            case "tpto" -> {
                if (online == null) return "!Le joueur n'est pas connecté.";
                admin.teleportTo(online.level(), online.getX(), online.getY(), online.getZ(), java.util.Set.of(), online.getYRot(), 0, true);
                return "Téléporté auprès de " + name + ".";
            }
            case "tphere" -> {
                if (online == null) return "!Le joueur n'est pas connecté.";
                online.teleportTo(admin.level(), admin.getX(), admin.getY(), admin.getZ(), java.util.Set.of(), admin.getYRot(), 0, true);
                return name + " amené auprès de vous.";
            }
            case "report_close" -> {
                return Reports.close(d.getIntOr("rid", -1), by) ? "Signalement classé." : "!Signalement introuvable.";
            }
            case "snapshot" -> {
                if (online == null) return "!Le joueur doit être connecté.";
                return Backups.snapshot(online, "manuel") ? "Profil sauvegardé." : "!Sauvegarde impossible.";
            }
            case "restore" -> {
                if (online == null) return "!Le joueur doit être connecté pour restaurer son profil.";
                return Backups.restore(by, online, d.getStringOr("file", ""));
            }
            case "undo" -> {
                if (online == null) return "!Le joueur doit être connecté.";
                int min = (int) parseNum(d.getStringOr("arg", ""), 0);
                if (min < 1 || min > 1440) return "!Indiquez un nombre de minutes entre 1 et 1440.";
                return Backups.undo(by, online, min);
            }
            case "maint" -> {
                if (d.getStringOr("do", "").equals("off")) return Maintenance.cancel(by);
                long ms = d.getStringOr("dur", "").equalsIgnoreCase("now") ? 1000 : ModUtil.parseDuration(d.getStringOr("dur", ""));
                return ms < 0 ? "!Durée invalide (exemples : 10m, 1h)." : Maintenance.schedule(by, ms, arg);
            }
            case "plan_add" -> {
                return EventPlanner.add(by, d.getStringOr("type", "annonce"), ModUtil.parseDuration(d.getStringOr("dur", "")),
                        ModUtil.parseDuration(d.getStringOr("every", "")), arg);
            }
            case "plan_del" -> {
                return EventPlanner.remove(by, d.getIntOr("tid", -1));
            }
            case "dungeon_enter", "dungeon_menu" -> {
                if (act.equals("dungeon_menu")) {
                    DungeonSetup.openMenu(admin);
                    return "";
                }
                DungeonType t = DungeonType.byId(d.getStringOr("dungeon", ""));
                if (t == null) return "!Donjon inconnu.";
                return DungeonSetup.enter(admin, t) ? "Direction le setup « " + t.label + " »." : "!Setup indisponible ici.";
            }
            default -> {
                return "!Action inconnue.";
            }
        }
    }

    private static double parseNum(String s, double def) {
        try {
            return Double.parseDouble(s.strip());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    // ================================================================== etat

    private static CompoundTag state(ServerPlayer admin, String tab, String id, String name, String q, boolean ok) {
        return state(admin, tab, id, name, q, ok, "");
    }

    private static CompoundTag state(ServerPlayer admin, String tab, String id, String name, String q, boolean ok, String msg) {
        MinecraftServer server = admin.level().getServer();
        CompoundTag t = new CompoundTag();
        t.putString("kind", "state");
        t.putString("tab", tab);
        t.putString("status", msg);
        t.putBoolean("ok", ok);
        t.putString("id", id);
        t.putString("name", name);
        t.putInt("online", server.getPlayerList().getPlayerCount());
        t.putInt("openReports", Reports.openCount());
        t.putInt("jailed", Jail.prisoners().size());
        t.putString("maintenance", Maintenance.active() ? "active" : Maintenance.counting() ? "dans " + ModUtil.formatDuration(Maintenance.remainingMs()) : "");
        UUID uid = ModUtil.uuid(id);
        switch (tab) {
            case "players" -> t.put("rows", players(server, q));
            case "sanctions" -> t.put("rows", sanctions(server));
            case "reports" -> t.put("rows", reports());
            case "history" -> t.put("rows", history(uid));
            case "journal" -> t.put("rows", journal(uid, (int) Math.max(1, Math.min(1440, parseNum(q, 60)))));
            case "backups" -> t.put("rows", backups(uid));
            case "connected" -> t.put("rows", connected(server));
            case "tools" -> tools(t);
            case "dungeons" -> t.put("rows", dungeons());
            default -> {
            }
        }
        if (uid != null) {
            t.putBoolean("sel_online", server.getPlayerList().getPlayer(uid) != null);
            t.putBoolean("sel_muted", PlayerAdmin.muted(uid));
            t.putBoolean("sel_jailed", Jail.isJailed(uid));
        }
        return t;
    }

    private static CompoundTag row(String a, String b, String c, String idv, String namev) {
        CompoundTag r = new CompoundTag();
        r.putString("a", a);
        r.putString("b", b);
        r.putString("c", c);
        r.putString("id", idv);
        r.putString("name", namev);
        return r;
    }

    private static ListTag players(MinecraftServer server, String q) {
        ListTag l = new ListTag();
        String low = q.toLowerCase(java.util.Locale.ROOT);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            String n = p.getGameProfile().name();
            if (!low.isEmpty() && !n.toLowerCase(java.util.Locale.ROOT).contains(low)) continue;
            PlayerData d = RpgPlayers.get(p);
            String flags = (PlayerAdmin.muted(p.getUUID()) ? "silence " : "") + (Jail.isJailed(p.getUUID()) ? "prison" : "");
            l.add(row(n, d.playerClass.label + " niv. " + d.level, flags.strip(), p.getUUID().toString(), n));
        }
        return l;
    }

    private static ListTag sanctions(MinecraftServer server) {
        ListTag l = new ListTag();
        for (var e : Jail.prisoners().entrySet()) {
            var v = e.getValue();
            l.add(row("Prison", v.name, ModUtil.formatDuration(v.until - System.currentTimeMillis()) + (v.reason.isEmpty() ? "" : " · " + v.reason), e.getKey(), v.name));
        }
        for (Map.Entry<UUID, String> e : PlayerAdmin.mutedAll().entrySet()) l.add(row("Silence", e.getValue(), "jusqu'à levée", e.getKey().toString(), e.getValue()));
        for (var e : server.getPlayerList().getBans().getEntries()) {
            String n = e.getDisplayName().getString();
            Object user = e.getUser();
            String uid = user instanceof net.minecraft.server.players.NameAndId ni ? ni.id().toString() : "";
            l.add(row("Banni", n, e.getReason() == null ? "" : e.getReason(), uid, n));
        }
        return l;
    }

    private static ListTag reports() {
        ListTag l = new ListTag();
        for (Reports.Report r : Reports.recent(false, 60)) {
            CompoundTag c = row((r.closed ? "[classé] " : "") + r.target, "par " + r.reporter + " · " + Instant.ofEpochMilli(r.time).toString().replace('T', ' ').substring(0, 16), r.reason, r.targetId, r.target);
            c.putInt("rid", r.id);
            c.putBoolean("closed", r.closed);
            l.add(c);
        }
        return l;
    }

    private static ListTag history(UUID uid) {
        ListTag l = new ListTag();
        for (ModHistory.Entry e : ModHistory.recent(uid == null ? "" : uid.toString(), 80)) {
            l.add(row(e.kind + " · " + e.target, Instant.ofEpochMilli(e.time).toString().replace('T', ' ').substring(0, 16) + " · " + e.by, e.detail, e.targetId, e.target));
        }
        return l;
    }

    private static ListTag journal(UUID uid, int minutes) {
        ListTag l = new ListTag();
        if (uid == null) return l;
        List<Journal.Line> lines = Journal.replay(uid.toString(), System.currentTimeMillis() - minutes * 60_000L, 120);
        for (int i = lines.size() - 1; i >= 0; i--) {
            Journal.Line x = lines.get(i);
            l.add(row(x.kind() + (x.amount() != 0 ? " (" + x.amount() + ")" : ""), Instant.ofEpochMilli(x.time()).toString().substring(11, 19), x.detail(), x.id(), x.name()));
        }
        return l;
    }

    private static ListTag backups(UUID uid) {
        ListTag l = new ListTag();
        if (uid == null) return l;
        for (Backups.Info i : Backups.list(uid)) {
            CompoundTag r = row(i.tag(), Instant.ofEpochMilli(i.time()).toString().replace('T', ' ').substring(0, 19), "", uid.toString(), "");
            r.putString("file", i.file());
            l.add(r);
            if (l.size() >= 40) break;
        }
        return l;
    }

    private static ListTag connected(MinecraftServer server) {
        ListTag l = new ListTag();
        String here = Database.enabled() ? Database.config().serverId : "ce serveur";
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            PlayerData d = RpgPlayers.get(p);
            l.add(row(p.getGameProfile().name(), here + " · " + d.playerClass.label + " niv. " + d.level,
                    p.connection.latency() + " ms · " + p.level().dimension().identifier().getPath(), p.getUUID().toString(), p.getGameProfile().name()));
        }
        if (Database.enabled() && Database.ready()) {
            List<String[]> rows = Database.await("tableau de bord (moderation)", 3000, c -> {
                List<String[]> out = new java.util.ArrayList<>();
                try (PreparedStatement ps = c.prepareStatement("SELECT uuid, name, class, level, server_id FROM players WHERE online = 1 AND server_id <> ? ORDER BY server_id, name LIMIT 120")) {
                    ps.setString(1, Database.config().serverId);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) out.add(new String[]{rs.getString(1), rs.getString(2), rs.getString(3), String.valueOf(rs.getInt(4)), rs.getString(5)});
                    }
                }
                return out;
            });
            if (rows != null) for (String[] r : rows) l.add(row(r[1], r[4] + " · " + r[2] + " niv. " + r[3], "autre serveur", r[0], r[1]));
        }
        return l;
    }

    private static void tools(CompoundTag t) {
        t.putString("maintReason", Maintenance.reason());
        Jail.Zone z = Jail.zone();
        t.putString("jailZone", z == null ? "non définie" : z.dimension + " " + (int) z.x + " " + (int) z.y + " " + (int) z.z + " · rayon " + (int) z.radius);
        ListTag plan = new ListTag();
        for (EventPlanner.Task k : EventPlanner.list()) {
            CompoundTag r = row("#" + k.id + " " + k.type, "dans " + ModUtil.formatDuration(k.at - System.currentTimeMillis()) + (k.repeat > 0 ? " · tous les " + ModUtil.formatDuration(k.repeat) : ""), k.text, "", "");
            r.putInt("tid", k.id);
            plan.add(r);
        }
        t.put("plan", plan);
    }

    private static ListTag dungeons() {
        ListTag l = new ListTag();
        for (DungeonType t : DungeonType.values()) {
            CompoundTag r = row(t.label, "niveau " + t.level, DungeonTemplate.exists(t) ? "modèle enregistré" : "génération d'origine", "", "");
            r.putString("dungeon", t.id);
            l.add(r);
        }
        return l;
    }
}
