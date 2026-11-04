package com.mmorpg.moderation;

import com.google.gson.reflect.TypeToken;
import com.mmorpg.MMORPG;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Prison : {@code /jail "pseudo" "temps" [raison]} envoie un joueur (meme hors ligne : applique a sa connexion) dans la zone definie par
 * {@code /jailzone set}. Tant qu'il est enferme il ne peut plus taper aucune commande, est ramene dans la zone s'il en sort et est
 * libere automatiquement a la fin de la peine (retour a son point d'origine). Les operateurs ne sont pas soumis a l'interdiction de commande.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class Jail {
    private Jail() {
    }

    public static final class Zone {
        public String dimension = "minecraft:overworld";
        public double x, y, z;
        public float yaw;
        public double radius = 12;
    }

    public static final class Prisoner {
        public String name = "";
        public String reason = "";
        public String by = "";
        public long until;
        public boolean inside;
        public String originDim = "";
        public double ox, oy, oz;
        public float oyaw;
    }

    private static final class Store {
        Zone zone;
        Map<String, Prisoner> prisoners = new LinkedHashMap<>();
    }

    private static Store store;

    private static synchronized Store store() {
        if (store == null) store = ModUtil.read("prison.json", new TypeToken<Store>() {
        }.getType(), new Store());
        if (store.prisoners == null) store.prisoners = new LinkedHashMap<>();
        return store;
    }

    private static void save() {
        ModUtil.write("prison.json", store());
    }

    public static Zone zone() {
        return store().zone;
    }

    public static synchronized Map<String, Prisoner> prisoners() {
        return new LinkedHashMap<>(store().prisoners);
    }

    public static synchronized boolean isJailed(UUID id) {
        Prisoner p = store().prisoners.get(id.toString());
        return p != null && p.until > System.currentTimeMillis();
    }

    // ------------------------------------------------------------------ actions

    public static synchronized String setZone(ServerPlayer admin) {
        Zone z = new Zone();
        z.dimension = admin.level().dimension().identifier().toString();
        z.x = admin.getX();
        z.y = admin.getY();
        z.z = admin.getZ();
        z.yaw = admin.getYRot();
        store().zone = z;
        save();
        ModHistory.record(admin.getGameProfile().name(), null, "-", "zone prison", z.dimension + " " + (int) z.x + " " + (int) z.y + " " + (int) z.z);
        return "Zone de la prison définie ici (rayon " + (int) z.radius + " blocs).";
    }

    public static synchronized String setRadius(double r) {
        Zone z = store().zone;
        if (z == null) return "Définissez d'abord la zone (/jailzone set).";
        z.radius = Math.max(3, Math.min(64, r));
        save();
        return "Rayon de la prison : " + (int) z.radius + " blocs.";
    }

    /** Enferme {@code id} pour {@code ms} millisecondes ; {@code online} est le joueur connecte (ou null). Renvoie un message d'etat (commence par « ! » si echec). */
    public static synchronized String jail(String by, UUID id, String name, ServerPlayer online, long ms, String reason) {
        Store s = store();
        if (s.zone == null) return "!La zone de la prison n'est pas définie (/jailzone set).";
        if (ms <= 0) return "!Durée invalide (exemples : 30m, 2h, 1d).";
        if (online != null && ModUtil.staff(online)) return "!Les opérateurs ne peuvent pas être emprisonnés.";
        Prisoner p = s.prisoners.get(id.toString());
        if (p == null) {
            p = new Prisoner();
            s.prisoners.put(id.toString(), p);
        }
        p.name = name;
        p.by = by;
        p.reason = reason == null ? "" : reason;
        p.until = System.currentTimeMillis() + ms;
        if (online != null && !p.inside) {
            p.originDim = online.level().dimension().identifier().toString();
            p.ox = online.getX();
            p.oy = online.getY();
            p.oz = online.getZ();
            p.oyaw = online.getYRot();
        }
        save();
        ModHistory.record(by, id, name, "prison", ModUtil.formatDuration(ms) + (p.reason.isEmpty() ? "" : " : " + p.reason));
        if (online != null) {
            enter(online, p);
        }
        return name + " est en prison pour " + ModUtil.formatDuration(ms) + (online == null ? " (appliqué à sa prochaine connexion)." : ".");
    }

    public static synchronized String release(String by, UUID id, String name, ServerPlayer online, boolean auto) {
        Store s = store();
        Prisoner p = s.prisoners.remove(id.toString());
        if (p == null) return "!" + name + " n'est pas en prison.";
        save();
        ModHistory.record(by, id, name, "libération", auto ? "fin de peine" : "levée manuelle");
        if (online != null) leave(online, p);
        return name + " est libéré" + (online == null ? " (à sa prochaine connexion)." : ".");
    }

    private static ServerLevel levelOf(MinecraftServer server, String dim) {
        Identifier id = Identifier.tryParse(dim);
        return id == null ? null : server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
    }

    private static void enter(ServerPlayer p, Prisoner pr) {
        Zone z = store().zone;
        if (z == null) return;
        ServerLevel l = levelOf(p.level().getServer(), z.dimension);
        if (l == null) return;
        pr.inside = true;
        p.teleportTo(l, z.x, z.y, z.z, Set.of(), z.yaw, 0, true);
        p.resetFallDistance();
        p.closeContainer();
        ModUtil.tell(p, "Vous êtes en prison" + (pr.reason.isEmpty() ? "" : " (" + pr.reason + ")") + " pour " + ModUtil.formatDuration(pr.until - System.currentTimeMillis()) + ". Aucune commande n'est autorisée.", 0xFF6B6B);
    }

    private static void leave(ServerPlayer p, Prisoner pr) {
        MinecraftServer server = p.level().getServer();
        ServerLevel l = pr.originDim.isEmpty() ? null : levelOf(server, pr.originDim);
        if (l != null) p.teleportTo(l, pr.ox, pr.oy, pr.oz, Set.of(), pr.oyaw, 0, true);
        else {
            ServerLevel o = server.overworld();
            var sp = o.getRespawnData().pos();
            p.teleportTo(o, sp.getX() + .5, sp.getY(), sp.getZ() + .5, Set.of(), 0, 0, true);
        }
        p.resetFallDistance();
        ModUtil.tell(p, "Vous êtes libéré de prison.", 0x80E080);
    }

    // ------------------------------------------------------------------ evenements

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        if (event.getParseResults().getContext().getSource().getEntity() instanceof ServerPlayer p && !ModUtil.staff(p) && isJailed(p.getUUID())) {
            event.setCanceled(true);
            ModUtil.tell(p, "Vous êtes en prison : les commandes sont interdites (reste " + ModUtil.formatDuration(remaining(p.getUUID())) + ").", 0xFF6B6B);
        }
    }

    private static synchronized long remaining(UUID id) {
        Prisoner p = store().prisoners.get(id.toString());
        return p == null ? 0 : p.until - System.currentTimeMillis();
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p)) return;
        synchronized (Jail.class) {
            Prisoner pr = store().prisoners.get(p.getUUID().toString());
            if (pr == null) return;
            if (pr.until <= System.currentTimeMillis()) {
                store().prisoners.remove(p.getUUID().toString());
                save();
                if (pr.inside) leave(p, pr);
                return;
            }
            pr.name = p.getGameProfile().name();
            if (!pr.inside) {
                pr.originDim = p.level().dimension().identifier().toString();
                pr.ox = p.getX();
                pr.oy = p.getY();
                pr.oz = p.getZ();
                pr.oyaw = p.getYRot();
            }
            enter(p, pr);
            save();
        }
    }

    @SubscribeEvent
    public static void onTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 != 0) return;
        List<ServerPlayer> players = new ArrayList<>(server.getPlayerList().getPlayers());
        for (ServerPlayer p : players) {
            Prisoner pr;
            Zone z;
            synchronized (Jail.class) {
                pr = store().prisoners.get(p.getUUID().toString());
                z = store().zone;
            }
            if (pr == null) continue;
            if (pr.until <= System.currentTimeMillis()) {
                release("système", p.getUUID(), p.getGameProfile().name(), p, true);
                continue;
            }
            if (z == null || ModUtil.staff(p)) continue;
            double dx = p.getX() - z.x, dz = p.getZ() - z.z;
            boolean far = !p.level().dimension().identifier().toString().equals(z.dimension) || dx * dx + dz * dz > z.radius * z.radius || Math.abs(p.getY() - z.y) > 24;
            if (far) {
                synchronized (Jail.class) {
                    enter(p, pr);
                }
            }
        }
    }

    // ------------------------------------------------------------------ commandes

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        CommandDispatcher<CommandSourceStack> d = event.getDispatcher();
        d.register(Commands.literal("jail").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("liste").executes(c -> {
                    var m = prisoners();
                    if (m.isEmpty()) c.getSource().sendSuccess(() -> Component.literal("Prison vide."), false);
                    m.forEach((k, v) -> c.getSource().sendSuccess(() -> Component.literal(v.name + " : reste " + ModUtil.formatDuration(v.until - System.currentTimeMillis())
                            + (v.reason.isEmpty() ? "" : " (" + v.reason + ")")), false));
                    return m.size();
                }))
                .then(Commands.argument("pseudo", StringArgumentType.string()).then(Commands.argument("temps", StringArgumentType.string())
                        .executes(c -> jailCmd(c.getSource(), StringArgumentType.getString(c, "pseudo"), StringArgumentType.getString(c, "temps"), ""))
                        .then(Commands.argument("raison", StringArgumentType.greedyString())
                                .executes(c -> jailCmd(c.getSource(), StringArgumentType.getString(c, "pseudo"), StringArgumentType.getString(c, "temps"), StringArgumentType.getString(c, "raison")))))));
        d.register(Commands.literal("unjail").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.argument("pseudo", StringArgumentType.string()).executes(c -> {
                    String name = StringArgumentType.getString(c, "pseudo");
                    UUID id = idOf(c.getSource().getServer(), name);
                    if (id == null) {
                        c.getSource().sendFailure(Component.literal("Joueur inconnu."));
                        return 0;
                    }
                    return reply(c.getSource(), release(c.getSource().getTextName(), id, name, ModUtil.online(c.getSource().getServer(), name), false));
                })));
        d.register(Commands.literal("jailzone").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("set").executes(c -> reply(c.getSource(), setZone(c.getSource().getPlayerOrException()))))
                .then(Commands.literal("rayon").then(Commands.argument("blocs", com.mojang.brigadier.arguments.DoubleArgumentType.doubleArg(3, 64))
                        .executes(c -> reply(c.getSource(), setRadius(com.mojang.brigadier.arguments.DoubleArgumentType.getDouble(c, "blocs"))))))
                .then(Commands.literal("info").executes(c -> {
                    Zone z = zone();
                    return reply(c.getSource(), z == null ? "!Zone non définie." : "Zone : " + z.dimension + " " + (int) z.x + " " + (int) z.y + " " + (int) z.z + ", rayon " + (int) z.radius);
                })));
    }

    private static int reply(CommandSourceStack src, String msg) {
        if (msg.startsWith("!")) {
            src.sendFailure(Component.literal(msg.substring(1)));
            return 0;
        }
        src.sendSuccess(() -> Component.literal(msg), true);
        return 1;
    }

    /** UUID d'un joueur en ligne ou connu du cache de profils du serveur. */
    public static UUID idOf(MinecraftServer server, String name) {
        ServerPlayer p = ModUtil.online(server, name);
        if (p != null) return p.getUUID();
        var cache = server.services().nameToIdCache();
        return cache == null ? null : cache.get(name).map(e -> e.id()).orElse(null);
    }

    private static int jailCmd(CommandSourceStack src, String name, String time, String reason) {
        UUID id = idOf(src.getServer(), name);
        if (id == null) {
            src.sendFailure(Component.literal("Joueur inconnu : " + name));
            return 0;
        }
        return reply(src, jail(src.getTextName(), id, name, ModUtil.online(src.getServer(), name), ModUtil.parseDuration(time), reason));
    }
}
