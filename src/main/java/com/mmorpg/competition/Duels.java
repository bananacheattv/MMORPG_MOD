package com.mmorpg.competition;

import com.mmorpg.MMORPG;
import com.mmorpg.server.RpgPlayers;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

/** Duels amicaux hors arène, avec mise en or (déposée en séquestre à l'acceptation, remise au vainqueur). */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class Duels {
    private static final long MAX_STAKE = 1_000_000;
    private static final int COUNTDOWN = 60, MAX_TICKS = 20 * 60 * 10, MAX_DISTANCE = 60, INVITE_TICKS = 20 * 60;

    private static final class Invite { UUID from, to; long stake; long expires; }
    private static final class Duel { UUID a, b; long stake; long start; boolean running; }

    private static final Map<UUID, Invite> INVITES = new HashMap<>();   // clé : invité
    private static final Map<UUID, Duel> DUELS = new HashMap<>();       // clé : chaque participant

    private Duels() {}

    public static boolean inDuel(UUID id) { return DUELS.containsKey(id); }

    public static boolean fighting(net.minecraft.world.entity.player.Player a, net.minecraft.world.entity.player.Player b) {
        var d = DUELS.get(a.getUUID());
        return d != null && d == DUELS.get(b.getUUID()) && d.running;
    }

    /** Vrai si les deux joueurs ne doivent pas se blesser à cause d'un duel (un seul des deux est en duel). */
    public static boolean blocks(ServerPlayer victim, Entity attacker) {
        if (!(attacker instanceof ServerPlayer a) || a == victim) return false;
        var dv = DUELS.get(victim.getUUID());
        var da = DUELS.get(a.getUUID());
        if (dv == null && da == null) return false;
        return dv != da || !dv.running;
    }

    private static boolean busy(ServerPlayer p) {
        return ArenaManager.session(p.getUUID()) != null || GuildRaids.of(p.getUUID()) != null || DUELS.containsKey(p.getUUID())
                || com.mmorpg.dungeon.DungeonVariants.active(p.level());
    }

    private static void invite(ServerPlayer from, ServerPlayer to, long stake) {
        if (from == to) throw new IllegalArgumentException("Tu ne peux pas te défier toi-même.");
        if (stake < 0 || stake > MAX_STAKE) throw new IllegalArgumentException("Mise : 0 à " + MAX_STAKE + " or.");
        if (busy(from) || busy(to)) throw new IllegalArgumentException("Un des joueurs est déjà en arène, en donjon ou en duel.");
        if (RpgPlayers.get(from).gold < stake) throw new IllegalArgumentException("Tu n'as pas assez d'or.");
        var i = new Invite();
        i.from = from.getUUID(); i.to = to.getUUID(); i.stake = stake;
        i.expires = from.level().getServer().getTickCount() + INVITE_TICKS;
        INVITES.put(to.getUUID(), i);
        String mise = stake > 0 ? " pour " + stake + " or" : " sans mise";
        from.sendSystemMessage(Component.literal("Défi envoyé à " + to.getGameProfile().name() + mise + "."));
        to.sendSystemMessage(Component.literal(from.getGameProfile().name() + " te défie en duel" + mise + ". /duel accepter ou /duel refuser (1 min)."));
    }

    private static void accept(ServerPlayer to) {
        var server = to.level().getServer();
        var i = INVITES.remove(to.getUUID());
        if (i == null || i.expires < server.getTickCount()) throw new IllegalArgumentException("Aucun défi en attente.");
        var from = server.getPlayerList().getPlayer(i.from);
        if (from == null) throw new IllegalArgumentException("Le challenger n'est plus connecté.");
        if (busy(from) || busy(to)) throw new IllegalArgumentException("Un des joueurs est occupé.");
        if (from.distanceTo(to) > MAX_DISTANCE || from.level() != to.level()) throw new IllegalArgumentException("Vous êtes trop éloignés.");
        if (!RpgPlayers.takeGold(from, i.stake)) throw new IllegalArgumentException("Le challenger n'a plus assez d'or.");
        if (!RpgPlayers.takeGold(to, i.stake)) {
            RpgPlayers.transferGold(from, i.stake, false);
            throw new IllegalArgumentException("Tu n'as pas assez d'or pour la mise.");
        }
        var d = new Duel();
        d.a = from.getUUID(); d.b = to.getUUID(); d.stake = i.stake; d.start = server.getTickCount() + COUNTDOWN;
        DUELS.put(d.a, d); DUELS.put(d.b, d);
        for (var p : List.of(from, to)) p.sendSystemMessage(Component.literal("Duel accepté ! Début dans 3 secondes. Mise : " + i.stake + " or chacun."));
    }

    private static void decline(ServerPlayer to) {
        var i = INVITES.remove(to.getUUID());
        if (i == null) throw new IllegalArgumentException("Aucun défi en attente.");
        var from = to.level().getServer().getPlayerList().getPlayer(i.from);
        if (from != null) from.sendSystemMessage(Component.literal(to.getGameProfile().name() + " refuse le duel."));
        to.sendSystemMessage(Component.literal("Défi refusé."));
    }

    private static void end(MinecraftServer server, Duel d, UUID winner, String why) {
        DUELS.remove(d.a); DUELS.remove(d.b);
        var pa = server.getPlayerList().getPlayer(d.a);
        var pb = server.getPlayerList().getPlayer(d.b);
        if (winner == null) {
            refund(server, d.a, d.stake); refund(server, d.b, d.stake);
        } else {
            refund(server, winner, d.stake * 2);
        }
        String text = winner == null ? "Duel nul (" + why + ") : mises rendues."
                : "Duel terminé (" + why + ") : " + (winner.equals(d.a) ? name(pa, "?") : name(pb, "?")) + " gagne " + d.stake * 2 + " or.";
        for (var p : new ServerPlayer[]{pa, pb}) if (p != null) p.sendSystemMessage(Component.literal(text));
    }

    private static String name(ServerPlayer p, String fallback) { return p == null ? fallback : p.getGameProfile().name(); }

    /** Or rendu ; si le joueur est hors ligne, il est conservé dans le registre « duels » jusqu'à sa reconnexion. */
    private static void refund(MinecraftServer server, UUID id, long gold) {
        if (gold <= 0) return;
        var p = server.getPlayerList().getPlayer(id);
        if (p != null) RpgPlayers.transferGold(p, gold, true);
        else com.mmorpg.network.SharedLedger.access(server, "duel-refunds", Owed.class, Owed::new, o -> o.gold.merge(id.toString(), gold, Long::sum));
    }

    public static final class Owed { public Map<String, Long> gold = new HashMap<>(); }

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        com.mmorpg.server.Scheduler.later(p.level(), 70, () -> {
            try {
                long gold = com.mmorpg.network.SharedLedger.access(p.level().getServer(), "duel-refunds", Owed.class, Owed::new, o -> { Long g = o.gold.remove(p.getUUID().toString()); return g == null ? 0L : g; });
                if (gold > 0) { RpgPlayers.transferGold(p, gold, true); p.sendSystemMessage(Component.literal("Duel : " + gold + " or reçus.")); }
            } catch (RuntimeException ex) { MMORPG.LOGGER.warn("Duels : {}", ex.getMessage()); }
        });
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        INVITES.remove(p.getUUID());
        var d = DUELS.get(p.getUUID());
        if (d != null) end(p.level().getServer(), d, d.a.equals(p.getUUID()) ? d.b : d.a, "déconnexion");
    }

    /** Coup mortel pendant un duel : annulé, le perdant est soigné et le duel prend fin. */
    public static boolean lethal(ServerPlayer victim, Entity attacker) {
        var d = DUELS.get(victim.getUUID());
        if (d == null || !d.running) return false;
        var server = victim.level().getServer();
        UUID winner = d.a.equals(victim.getUUID()) ? d.b : d.a;
        victim.setHealth(victim.getMaxHealth());
        RpgPlayers.fullRestore(victim);
        var w = server.getPlayerList().getPlayer(winner);
        if (w != null) RpgPlayers.fullRestore(w);
        end(server, d, winner, "victoire");
        return true;
    }

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post e) {
        var server = e.getServer();
        int now = server.getTickCount();
        if (now % 10 != 0) return;
        INVITES.values().removeIf(i -> i.expires < now);
        for (var d : new HashSet<>(DUELS.values())) {
            if (!DUELS.containsKey(d.a)) continue;
            var pa = server.getPlayerList().getPlayer(d.a);
            var pb = server.getPlayerList().getPlayer(d.b);
            if (pa == null || pb == null) { end(server, d, pa == null ? (pb == null ? null : d.b) : d.a, "déconnexion"); continue; }
            if (!d.running) {
                if (now >= d.start) { d.running = true; for (var p : List.of(pa, pb)) p.sendSystemMessage(Component.literal("Combattez !")); }
                continue;
            }
            if (!pa.isAlive() || !pb.isAlive()) { end(server, d, pa.isAlive() ? d.a : pb.isAlive() ? d.b : null, "mort"); continue; }
            if (busy2(pa) || busy2(pb)) { end(server, d, null, "arène ou donjon"); continue; }
            if (pa.level() != pb.level() || pa.distanceTo(pb) > MAX_DISTANCE * 2) {
                end(server, d, null, "trop éloignés"); continue;
            }
            if (now - d.start > MAX_TICKS) end(server, d, null, "temps écoulé");
        }
    }

    private static boolean busy2(ServerPlayer p) { return ArenaManager.session(p.getUUID()) != null || com.mmorpg.dungeon.DungeonVariants.active(p.level()); }

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        var root = Commands.literal("duel")
                .executes(c -> { c.getSource().sendSuccess(() -> Component.literal("/duel <joueur> [mise] · /duel accepter · /duel refuser · /duel abandon"), false); return 1; })
                .then(Commands.literal("accepter").executes(c -> run(c.getSource(), Duels::accept)))
                .then(Commands.literal("refuser").executes(c -> run(c.getSource(), Duels::decline)))
                .then(Commands.literal("abandon").executes(c -> run(c.getSource(), p -> {
                    var d = DUELS.get(p.getUUID());
                    if (d == null || !d.running) throw new IllegalArgumentException("Aucun duel en cours.");
                    end(p.level().getServer(), d, d.a.equals(p.getUUID()) ? d.b : d.a, "abandon");
                })))
                .then(Commands.argument("joueur", EntityArgument.player())
                        .executes(c -> { var target = EntityArgument.getPlayer(c, "joueur"); return run(c.getSource(), p -> invite(p, target, 0)); })
                        .then(Commands.argument("mise", LongArgumentType.longArg(0, MAX_STAKE))
                                .executes(c -> { var target = EntityArgument.getPlayer(c, "joueur"); long stake = LongArgumentType.getLong(c, "mise"); return run(c.getSource(), p -> invite(p, target, stake)); })));
        e.getDispatcher().register(root);
    }

    private static int run(net.minecraft.commands.CommandSourceStack source, java.util.function.Consumer<ServerPlayer> action) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        var p = source.getPlayerOrException();
        try { action.accept(p); return 1; } catch (IllegalArgumentException | IllegalStateException ex) { source.sendFailure(Component.literal(ex.getMessage())); return 0; }
    }
}
