package com.mmorpg.anticheat;

import com.mmorpg.MMORPG;
import com.mmorpg.admin.PlayerAdmin;
import com.mmorpg.config.ConfigManager;
import com.mmorpg.db.Audit;
import com.mmorpg.server.FeatureFlags;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.handling.IPayloadHandler;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Anticheat cote serveur (interrupteur « anticheat » du menu /fonctionnalites). Trois couches :
 * porte des paquets du mod (limite de debit + permissions), detections de base (vitesse, vol, portee, cadence de frappe/casse),
 * et un score de suspicion par joueur qui decroit avec le temps : alerte aux operateurs, retour arriere, puis expulsion.
 * Les operateurs sont journalises mais jamais sanctionnes. Aucun bannissement automatique (decision humaine).
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class AntiCheat {
    private AntiCheat() {
    }

    /** Paquets reserves aux operateurs : refus en amont, en plus du controle du gestionnaire. */
    private static final Set<String> OP_ONLY = Set.of("quest_edit", "loot_edit", "merchant_edit", "teleporter_setup", "admin_request");
    /** {capacite, recharge par seconde} pour les paquets plus sensibles que la normale. */
    private static final Map<String, double[]> LIMITS = Map.ofEntries(
            Map.entry("teleport", new double[]{4, 1}), Map.entry("dungeon_enter", new double[]{3, 1}), Map.entry("mine_enter", new double[]{3, 1}),
            Map.entry("select_class", new double[]{3, 0.5}), Map.entry("crate_open", new double[]{6, 3}), Map.entry("lucky_roll", new double[]{6, 3}),
            Map.entry("forge_action", new double[]{20, 10}), Map.entry("shop_action", new double[]{20, 10}));
    private static final double[] DEFAULT_LIMIT = {40, 20};

    public static final int WARN = 10, SETBACK = 25, KICK = 50;
    private static final Map<UUID, Profile> PROFILES = new ConcurrentHashMap<>();

    static final class Profile {
        final Map<String, double[]> buckets = new ConcurrentHashMap<>();
        double score;
        long scoreAt = System.currentTimeMillis();
        int kicks, warned;
        int[] flagCounts = new int[Check.values().length];
        double lastX, lastY, lastZ, safeX, safeY, safeZ;
        boolean hasLast;
        long lastTick, lastHurtTick, lastTeleportTick;
        int hover, climb, sinceCheck;
        double window;
        long[] attacks = new long[24];
        int attackIdx;
        long[] breaks = new long[40];
        long lastLog;
        int breakIdx;
    }

    public enum Check {
        PACKET_RATE("débit de paquets", 1), PACKET_PERM("paquet interdit", 15), SPEED("vitesse", 6), FLY("vol", 8), REACH("portée", 6),
        CLICK_RATE("cadence de frappe", 4), BREAK_RATE("cadence de casse", 4), BREAK_REACH("portée de casse", 6);
        final String label;
        final int weight;

        Check(String label, int weight) {
            this.label = label;
            this.weight = weight;
        }
    }

    private static boolean on(ServerPlayer p) {
        return FeatureFlags.enabled(p.level().getServer(), "anticheat");
    }

    private static Profile of(ServerPlayer p) {
        return PROFILES.computeIfAbsent(p.getUUID(), u -> new Profile());
    }

    /** Enveloppe d'un gestionnaire serveur : controle de permission puis limite de debit. */
    public static <T extends CustomPacketPayload> IPayloadHandler<T> gate(CustomPacketPayload.Type<T> type, IPayloadHandler<T> handler) {
        final String name = type.id().getPath();
        return (packet, ctx) -> {
            if (ctx.player() instanceof ServerPlayer p && on(p)) {
                if (OP_ONLY.contains(name) && !PlayerAdmin.allowed(p) && !(name.equals("admin_request") && com.mmorpg.admin.AdminAuth.authorized(p))) {
                    flag(p, Check.PACKET_PERM, name);
                    return;
                }
                if (!take(p, name)) return;
            }
            handler.handle(packet, ctx);
        };
    }

    private static boolean take(ServerPlayer p, String name) {
        double[] lim = LIMITS.getOrDefault(name, DEFAULT_LIMIT);
        double[] b = of(p).buckets.computeIfAbsent(name, k -> new double[]{lim[0], System.nanoTime()});
        long now = System.nanoTime();
        synchronized (b) {
            b[0] = Math.min(lim[0], b[0] + (now - b[1]) / 1e9 * lim[1]);
            b[1] = now;
            if (b[0] < 1) {
                flag(p, Check.PACKET_RATE, name);
                return false;
            }
            b[0]--;
            return true;
        }
    }

    /** Ajoute de la suspicion, journalise et applique la sanction progressive. */
    public static void flag(ServerPlayer p, Check check, String detail) {
        Profile pr = of(p);
        decay(pr);
        pr.score += check.weight;
        pr.flagCounts[check.ordinal()]++;
        boolean op = PlayerAdmin.allowed(p);
        // Les rafales de paquets ne sont journalisees qu'une fois par seconde pour ne pas saturer le journal.
        long now = System.currentTimeMillis();
        if (check != Check.PACKET_RATE || now - pr.lastLog > 1000) {
            pr.lastLog = now;
            log(p, check, detail, pr.score, op);
        }
        if (op) return;
        if (pr.score >= KICK) {
            pr.kicks++;
            pr.score = 0;
            alert("§c[Anticheat] " + p.getGameProfile().name() + " expulsé (" + check.label + ", expulsion n°" + pr.kicks + (pr.kicks >= 3 ? " : bannissement conseillé" : "") + ")");
            p.connection.disconnect(Component.literal("Comportement suspect détecté (" + check.label + ")."));
        } else if (pr.score >= SETBACK && check != Check.PACKET_RATE) {
            setBack(p, pr);
        } else if (pr.score >= WARN && pr.warned < 1 + (int) (pr.score / WARN)) {
            pr.warned = 1 + (int) (pr.score / WARN);
            alert("§e[Anticheat] " + p.getGameProfile().name() + " suspect : " + check.label + " (score " + (int) pr.score + ")");
        }
    }

    private static void setBack(ServerPlayer p, Profile pr) {
        if (!pr.hasLast) return;
        pr.lastTeleportTick = p.level().getServer().getTickCount();
        p.teleportTo(pr.safeX, pr.safeY, pr.safeZ);
    }

    private static void decay(Profile pr) {
        long now = System.currentTimeMillis();
        pr.score = Math.max(0, pr.score - (now - pr.scoreAt) / 1000.0 * 0.5);
        pr.scoreAt = now;
        if (pr.score < WARN) pr.warned = 0;
    }

    private static void alert(String message) {
        var server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerPlayer op : server.getPlayerList().getPlayers()) if (PlayerAdmin.allowed(op)) op.sendSystemMessage(Component.literal(message));
    }

    private static void log(ServerPlayer p, Check check, String detail, double score, boolean op) {
        String line = Instant.now() + " " + p.getGameProfile().name() + " [" + p.getUUID() + "] " + check.label + " : " + detail + " (score " + (int) score + (op ? ", opérateur exempté" : "") + ")";
        MMORPG.LOGGER.warn("[MMORPG] ANTICHEAT : {}", line);
        try {
            Files.createDirectories(ConfigManager.dir());
            Files.writeString(ConfigManager.dir().resolve("anticheat.log"), line + System.lineSeparator(), StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (Exception e) {
            MMORPG.LOGGER.warn("[MMORPG] anticheat.log non écrit : {}", e.getMessage());
        }
        Audit.log(p, Audit.ANTICHEAT, (long) score, check.label + " : " + detail);
    }

    // ---- Detections ----

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !on(p)) return;
        Profile pr = of(p);
        long tick = p.level().getServer().getTickCount();
        boolean exempt = p.isCreative() || p.isSpectator() || p.getAbilities().mayfly || p.isPassenger() || p.isFallFlying() || p.isAutoSpinAttack()
                || p.hurtTime > 0 || p.isDeadOrDying();
        if (p.hurtTime > 0) pr.lastHurtTick = tick;
        if (!pr.hasLast) {
            remember(pr, p, tick);
            pr.hasLast = true;
            return;
        }
        double dx = p.getX() - pr.lastX, dz = p.getZ() - pr.lastZ, dy = p.getY() - pr.lastY;
        double step = Math.sqrt(dx * dx + dz * dz + dy * dy);
        pr.lastX = p.getX();
        pr.lastY = p.getY();
        pr.lastZ = p.getZ();
        // Teleportation (portail, /tp, retour arriere) : on repart d'une base saine.
        if (step > 10 || tick - pr.lastTeleportTick < 40 || tick - pr.lastTick > 5) {
            pr.lastTick = tick;
            pr.hover = 0;
            pr.window = 0;
            pr.sinceCheck = 0;
            remember(pr, p, tick);
            if (step > 10) pr.lastTeleportTick = tick;
            return;
        }
        pr.lastTick = tick;
        boolean recentHit = tick - pr.lastHurtTick < 40;
        boolean fluid = p.isInWater() || p.isInLava() || p.onClimbable() || p.isInPowderSnow || p.hasEffect(MobEffects.LEVITATION) || p.hasEffect(MobEffects.SLOW_FALLING)
                || p.hasEffect(MobEffects.JUMP_BOOST);
        // Vol stationnaire : en l'air sans aucune chute, plus de 2 secondes.
        if (!exempt && !fluid && !p.onGround() && Math.abs(dy) < 0.005 && !recentHit) {
            if (++pr.hover == 45) flag(p, Check.FLY, "stationnaire en l'air " + String.format("(%.1f %.1f %.1f)", p.getX(), p.getY(), p.getZ()));
        } else pr.hover = 0;
        // Vitesse : distance horizontale sur une seconde glissante comparee a l'attribut de vitesse.
        pr.window += Math.sqrt(dx * dx + dz * dz);
        if (++pr.sinceCheck >= 20) {
            double speed = p.getAttributeValue(Attributes.MOVEMENT_SPEED);
            double max = Math.max(speed, 0.1) * 43 * 1.7 + 4;
            if (!exempt && !recentHit && pr.window > max) flag(p, Check.SPEED, String.format("%.1f blocs/s (max %.1f)", pr.window, max));
            pr.window = 0;
            pr.sinceCheck = 0;
            if (p.onGround() && !exempt) remember(pr, p, tick);
        }
    }

    private static void remember(Profile pr, ServerPlayer p, long tick) {
        pr.lastX = p.getX();
        pr.lastY = p.getY();
        pr.lastZ = p.getZ();
        if (p.onGround()) {
            pr.safeX = p.getX();
            pr.safeY = p.getY();
            pr.safeZ = p.getZ();
        }
    }

    @SubscribeEvent
    public static void attack(AttackEntityEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !on(p)) return;
        Profile pr = of(p);
        double dist = Math.sqrt(p.distanceToSqr(e.getTarget().getBoundingBox().getCenter())) - e.getTarget().getBbWidth() / 2;
        if (dist > 6.5) {
            flag(p, Check.REACH, String.format("%.1f blocs", dist));
            if (!PlayerAdmin.allowed(p)) e.setCanceled(true);
            return;
        }
        long now = System.currentTimeMillis();
        pr.attacks[pr.attackIdx++ % pr.attacks.length] = now;
        long oldest = pr.attacks[pr.attackIdx % pr.attacks.length];
        // 24 coups en moins de 1,5 s (> 16 coups/s) : auto-clic ou killaura.
        if (oldest != 0 && now - oldest < 1500) {
            flag(p, Check.CLICK_RATE, "24 coups en " + (now - oldest) + " ms");
            pr.attacks = new long[24];
            if (!PlayerAdmin.allowed(p)) e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void breakBlock(BreakBlockEvent e) {
        if (!(e.getPlayer() instanceof ServerPlayer p) || !on(p) || p.isCreative()) return;
        Profile pr = of(p);
        double d = p.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(e.getPos()));
        if (d > 8.5 * 8.5) {
            flag(p, Check.BREAK_REACH, String.format("%.1f blocs", Math.sqrt(d)));
            if (!PlayerAdmin.allowed(p)) e.setCanceled(true);
            return;
        }
        long now = System.currentTimeMillis();
        pr.breaks[pr.breakIdx++ % pr.breaks.length] = now;
        long oldest = pr.breaks[pr.breakIdx % pr.breaks.length];
        // 40 blocs en moins de 2 s : nuker / casse en masse.
        if (oldest != 0 && now - oldest < 2000) {
            flag(p, Check.BREAK_RATE, "40 blocs en " + (now - oldest) + " ms");
            pr.breaks = new long[40];
            if (!PlayerAdmin.allowed(p)) e.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        // Le profil est conserve brievement via la decroissance du score ; on retire seulement les joueurs sans suspicion.
        if (e.getEntity() instanceof ServerPlayer p) {
            Profile pr = PROFILES.get(p.getUUID());
            if (pr != null) {
                decay(pr);
                if (pr.score < 1 && pr.kicks == 0) PROFILES.remove(p.getUUID());
                pr.buckets.clear();
                pr.hasLast = false;
            }
        }
    }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        e.getDispatcher().register(Commands.literal("anticheat").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> {
                    var src = c.getSource();
                    src.sendSuccess(() -> Component.literal("Anticheat : " + (FeatureFlags.enabled(src.getServer(), "anticheat") ? "activé" : "désactivé")
                            + " — /anticheat joueurs | /anticheat reinitialiser <joueur> (journal : config/anticheat.log)"), false);
                    return 1;
                })
                .then(Commands.literal("joueurs").executes(c -> {
                    var src = c.getSource();
                    List<String> rows = PROFILES.entrySet().stream().filter(en -> {
                        decay(en.getValue());
                        return en.getValue().score >= 1 || en.getValue().kicks > 0;
                    }).map(en -> {
                        var pl = src.getServer().getPlayerList().getPlayer(en.getKey());
                        return (pl == null ? en.getKey().toString() : pl.getGameProfile().name()) + " : score " + (int) en.getValue().score + ", expulsions " + en.getValue().kicks;
                    }).toList();
                    src.sendSuccess(() -> Component.literal(rows.isEmpty() ? "Aucun joueur suspect." : String.join("\n", rows)), false);
                    return rows.size();
                }))
                .then(Commands.literal("reinitialiser").then(Commands.argument("joueur", net.minecraft.commands.arguments.EntityArgument.player()).executes(c -> {
                    ServerPlayer t = net.minecraft.commands.arguments.EntityArgument.getPlayer(c, "joueur");
                    PROFILES.remove(t.getUUID());
                    c.getSource().sendSuccess(() -> Component.literal("Suspicion de " + t.getGameProfile().name() + " remise à zéro."), true);
                    return 1;
                }))));
    }
}
