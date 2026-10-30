package com.mmorpg.economy;

import com.mmorpg.MMORPG;
import com.mmorpg.db.Database;
import com.mmorpg.network.Net;
import com.mmorpg.network.S2COpenScreen;
import com.mmorpg.server.CrateOnly;
import com.mmorpg.server.FeatureFlags;
import com.mmorpg.server.RpgPlayers;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Echange securise entre deux joueurs (/trade) : demande avec acceptation, huit objets de l'inventaire et de l'or de chaque cote,
 * validation puis confirmation des deux joueurs. Les objets restent dans l'inventaire de leur proprietaire jusqu'a l'echange final,
 * qui se fait en un seul instant cote serveur apres une derniere verification : annuler, se deconnecter ou s'eloigner ne fait
 * rien perdre, et un objet ne peut pas etre en deux endroits a la fois. Chaque echange termine est journalise en base (table events).
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class TradeManager {
    private TradeManager() {
    }

    public static final int MAX_OFFERS = 8;
    public static final int INVENTORY_SLOTS = 36;
    public static final long MAX_GOLD = 1_000_000_000L;
    public static final long REQUEST_MS = 60_000L;
    /** Delai avant que « Confirmer » ne soit accepte, pour qu'un clic reflexe ne valide pas un echange modifie. */
    public static final long CONFIRM_DELAY_MS = 2_000L;
    public static final double MAX_DISTANCE = 48;

    private record Request(UUID from, String fromName, long expires) {
    }

    static final class Side {
        final UUID id;
        final String name;
        final Map<Integer, String> offers = new LinkedHashMap<>();       // case d'inventaire -> empreinte de l'objet propose
        long gold;
        boolean ready;
        boolean confirmed;

        Side(ServerPlayer p) {
            this.id = p.getUUID();
            this.name = p.getGameProfile().name();
        }
    }

    static final class Session {
        final Side a;
        final Side b;
        long stageSince;

        Session(ServerPlayer pa, ServerPlayer pb) {
            this.a = new Side(pa);
            this.b = new Side(pb);
        }

        Side of(UUID id) {
            return a.id.equals(id) ? a : b;
        }

        Side other(UUID id) {
            return a.id.equals(id) ? b : a;
        }

        boolean bothReady() {
            return a.ready && b.ready;
        }

        /** Toute modification annule les validations : il faut revalider ce que l'on voit. */
        void reset() {
            a.ready = b.ready = a.confirmed = b.confirmed = false;
        }
    }

    private static final Map<UUID, Request> REQUESTS = new HashMap<>();   // destinataire -> demande
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();   // chacun des deux joueurs -> sa session

    // ================================================================== commandes

    @SubscribeEvent
    public static void commands(RegisterCommandsEvent e) {
        for (String name : new String[]{"trade", "echange"}) {
            LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal(name);
            root.then(Commands.literal("accepter").executes(c -> {
                accept(c.getSource().getPlayerOrException());
                return 1;
            }));
            root.then(Commands.literal("refuser").executes(c -> {
                decline(c.getSource().getPlayerOrException());
                return 1;
            }));
            root.then(Commands.literal("annuler").executes(c -> {
                ServerPlayer p = c.getSource().getPlayerOrException();
                Session s = SESSIONS.get(p.getUUID());
                if (s != null) cancel(s, p.getGameProfile().name() + " a annulé l'échange.");
                return 1;
            }));
            root.then(Commands.argument("joueur", StringArgumentType.word())
                    .suggests((c, b) -> SharedSuggestionProvider.suggest(c.getSource().getServer().getPlayerList().getPlayerNamesArray(), b))
                    .executes(c -> {
                        request(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "joueur"));
                        return 1;
                    }));
            e.getDispatcher().register(root);
        }
    }

    private static void msg(ServerPlayer p, String text, int color) {
        p.sendSystemMessage(Component.literal("[Échange] ").withColor(0xE0B040).append(Component.literal(text).withColor(color)));
    }

    // ================================================================== demande

    /** Joueurs simules des tests serveur (ils ne sont pas dans la liste des joueurs connectes). */
    private static final Map<UUID, ServerPlayer> SIMULATED = new HashMap<>();

    private static ServerPlayer resolve(MinecraftServer server, UUID id) {
        ServerPlayer p = SIMULATED.get(id);
        return p != null ? p : server.getPlayerList().getPlayer(id);
    }

    public static void request(ServerPlayer from, String targetName) {
        request(from, from.level().getServer().getPlayerList().getPlayerByName(targetName), targetName);
    }

    public static void request(ServerPlayer from, ServerPlayer target, String targetName) {
        if (!FeatureFlags.allow(from, "echange")) return;
        if (target == null) {
            msg(from, "Joueur introuvable : " + targetName, 0xFF6060);
            return;
        }
        if (target == from) {
            msg(from, "Vous ne pouvez pas échanger avec vous-même.", 0xFF6060);
            return;
        }
        if (com.mmorpg.hub.ClassTrial.refuseTransfer(from, target)) return;
        if (SESSIONS.containsKey(from.getUUID())) {
            msg(from, "Vous êtes déjà en train d'échanger.", 0xFF6060);
            return;
        }
        if (SESSIONS.containsKey(target.getUUID())) {
            msg(from, target.getGameProfile().name() + " est déjà en train d'échanger.", 0xFF6060);
            return;
        }
        if (!close(from, target)) {
            msg(from, "Rapprochez-vous de " + target.getGameProfile().name() + " (" + (int) MAX_DISTANCE + " blocs maximum).", 0xFF6060);
            return;
        }
        Request back = REQUESTS.get(from.getUUID());
        if (back != null && back.from().equals(target.getUUID()) && back.expires() > System.currentTimeMillis()) {
            accept(from);                                                // chacun a demande l'autre : on ouvre directement
            return;
        }
        REQUESTS.put(target.getUUID(), new Request(from.getUUID(), from.getGameProfile().name(), System.currentTimeMillis() + REQUEST_MS));
        msg(from, "Demande d'échange envoyée à " + target.getGameProfile().name() + ".", 0xE0D8C0);
        Component accept = Component.literal("[Accepter]").withStyle(s -> s.withColor(0x60E060).withBold(true)
                .withClickEvent(new ClickEvent.RunCommand("/trade accepter"))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Ouvrir l'échange"))));
        Component decline = Component.literal("[Refuser]").withStyle(s -> s.withColor(0xFF6060).withBold(true)
                .withClickEvent(new ClickEvent.RunCommand("/trade refuser")));
        target.sendSystemMessage(Component.literal("[Échange] ").withColor(0xE0B040)
                .append(Component.literal(from.getGameProfile().name() + " vous propose un échange. ").withColor(0xE0D8C0))
                .append(accept).append(Component.literal(" ")).append(decline));
    }

    public static void accept(ServerPlayer p) {
        Request r = REQUESTS.remove(p.getUUID());
        if (r == null || r.expires() < System.currentTimeMillis()) {
            msg(p, "Aucune demande d'échange en attente.", 0xFF6060);
            return;
        }
        ServerPlayer from = resolve(p.level().getServer(), r.from());
        if (from == null) {
            msg(p, "Ce joueur n'est plus connecté.", 0xFF6060);
            return;
        }
        if (com.mmorpg.hub.ClassTrial.refuseTransfer(p, from)) return;
        if (SESSIONS.containsKey(p.getUUID()) || SESSIONS.containsKey(from.getUUID())) {
            msg(p, "Un des deux joueurs est déjà en train d'échanger.", 0xFF6060);
            return;
        }
        if (!close(p, from)) {
            msg(p, "Vous êtes trop loin de " + r.fromName() + ".", 0xFF6060);
            return;
        }
        Session s = new Session(from, p);
        SESSIONS.put(from.getUUID(), s);
        SESSIONS.put(p.getUUID(), s);
        RpgPlayers.depositCoins(from);
        RpgPlayers.depositCoins(p);
        push(s, from, p, "");
    }

    public static void decline(ServerPlayer p) {
        Request r = REQUESTS.remove(p.getUUID());
        if (r == null) {
            msg(p, "Aucune demande d'échange en attente.", 0xFF6060);
            return;
        }
        msg(p, "Demande refusée.", 0xE0D8C0);
        ServerPlayer from = resolve(p.level().getServer(), r.from());
        if (from != null) msg(from, p.getGameProfile().name() + " a refusé l'échange.", 0xFF6060);
    }

    private static boolean close(ServerPlayer a, ServerPlayer b) {
        return a.level() == b.level() && a.distanceToSqr(b) <= MAX_DISTANCE * MAX_DISTANCE;
    }

    // ================================================================== actions de l'interface

    /** Action du client : {@code act} = offer (case), gold (montant), ready, back, confirm, cancel. */
    public static void action(ServerPlayer p, CompoundTag data) {
        Session s = SESSIONS.get(p.getUUID());
        if (s == null) return;
        MinecraftServer server = p.level().getServer();
        ServerPlayer pa = resolve(server, s.a.id);
        ServerPlayer pb = resolve(server, s.b.id);
        if (pa == null || pb == null) {
            cancel(s, "L'autre joueur n'est plus là.");
            return;
        }
        if (com.mmorpg.hub.ClassTrial.active(pa) || com.mmorpg.hub.ClassTrial.active(pb)) {
            cancel(s, "Échange annulé : essai de classe en cours.");
            return;
        }
        Side me = s.of(p.getUUID());
        boolean changed = prune(s, pa, pb);
        switch (data.getStringOr("act", "")) {
            case "offer" -> {
                int slot = data.getIntOr("slot", -1);
                ItemStack st = slot >= 0 && slot < INVENTORY_SLOTS ? p.getInventory().getItem(slot) : ItemStack.EMPTY;
                if (me.offers.remove(slot) != null) {
                    s.reset();
                } else if (!st.isEmpty() && me.offers.size() < MAX_OFFERS) {
                    if (CrateOnly.is(st.getItem())) {
                        p.sendOverlayMessage(Component.literal("Cet objet ne peut pas être échangé.").withColor(0xFF7060));
                    } else {
                        me.offers.put(slot, Auction.saleFingerprint(st, p.level().registryAccess()));
                        s.reset();
                    }
                }
            }
            case "gold" -> {
                RpgPlayers.depositCoins(p);
                me.gold = Math.max(0, Math.min(Math.min(MAX_GOLD, RpgPlayers.get(p).gold), data.getLongOr("amount", 0)));
                s.reset();
            }
            case "ready" -> {
                me.ready = !me.ready;
                me.confirmed = false;
                s.other(p.getUUID()).confirmed = false;
                if (s.bothReady()) s.stageSince = System.currentTimeMillis();
            }
            case "back" -> s.reset();
            case "confirm" -> {
                if (s.bothReady() && System.currentTimeMillis() - s.stageSince >= CONFIRM_DELAY_MS) {
                    me.confirmed = true;
                    if (s.a.confirmed && s.b.confirmed) {
                        execute(s, pa, pb);
                        return;
                    }
                }
            }
            case "cancel" -> {
                cancel(s, p.getGameProfile().name() + " a annulé l'échange.");
                return;
            }
            default -> {
            }
        }
        push(s, pa, pb, changed ? "Un objet proposé a changé : l'échange est à revalider." : "");
    }

    // ================================================================== verification et echange

    /** Retire les offres dont l'objet n'est plus dans sa case (deplace, utilise, jete...) ; revient vrai si quelque chose a change. */
    private static boolean prune(Session s, ServerPlayer pa, ServerPlayer pb) {
        boolean changed = pruneSide(s.a, pa) | pruneSide(s.b, pb);
        if (changed) s.reset();
        return changed;
    }

    private static boolean pruneSide(Side side, ServerPlayer p) {
        boolean changed = false;
        var it = side.offers.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            ItemStack st = p.getInventory().getItem(e.getKey());
            if (st.isEmpty() || !e.getValue().equals(Auction.saleFingerprint(st, p.level().registryAccess()))) {
                it.remove();
                changed = true;
            }
        }
        long wallet = RpgPlayers.get(p).gold;
        if (side.gold > wallet) {
            side.gold = wallet;
            changed = true;
        }
        return changed;
    }

    private static boolean hasRoom(ServerPlayer p, Side outgoing, int incoming) {
        int free = outgoing.offers.size();                               // chaque case proposee est liberee par l'echange
        for (int i = 0; i < INVENTORY_SLOTS; i++) if (p.getInventory().getItem(i).isEmpty()) free++;
        return free >= incoming;
    }

    private static List<ItemStack> take(ServerPlayer p, Side side) {
        List<ItemStack> out = new ArrayList<>();
        for (int slot : side.offers.keySet()) {
            out.add(p.getInventory().getItem(slot).copy());
            p.getInventory().setItem(slot, ItemStack.EMPTY);
        }
        return out;
    }

    private static void give(ServerPlayer p, List<ItemStack> stacks) {
        for (ItemStack st : stacks) if (!p.getInventory().add(st)) p.drop(st, false, net.minecraft.util.Prediction.SERVER_ONLY);
    }

    /** Derniere verification puis echange en un seul instant ; au moindre doute rien ne bouge et les validations sont annulees. */
    private static void execute(Session s, ServerPlayer pa, ServerPlayer pb) {
        RpgPlayers.depositCoins(pa);
        RpgPlayers.depositCoins(pb);
        if (prune(s, pa, pb)) {
            push(s, pa, pb, "Un objet proposé a changé : l'échange est à revalider.");
            return;
        }
        if (!close(pa, pb)) {
            cancel(s, "Les joueurs se sont trop éloignés.");
            return;
        }
        if (!hasRoom(pa, s.a, s.b.offers.size()) || !hasRoom(pb, s.b, s.a.offers.size())) {
            s.reset();
            push(s, pa, pb, "Inventaire plein : libérez des cases.");
            return;
        }
        List<ItemStack> fromA = take(pa, s.a);
        List<ItemStack> fromB = take(pb, s.b);
        String gaveA = describe(fromA);                                  // avant give() : l'ajout a l'inventaire vide les piles
        String gaveB = describe(fromB);
        RpgPlayers.takeGold(pa, s.a.gold);
        RpgPlayers.takeGold(pb, s.b.gold);
        give(pa, fromB);
        give(pb, fromA);
        RpgPlayers.transferGold(pa, s.b.gold, false);
        RpgPlayers.transferGold(pb, s.a.gold, false);
        journal(pa.level().getServer(), s, "trade", gaveA, gaveB);
        finish(s, pa, pb, "Échange terminé.", 0x60E060);
    }

    private static String describe(List<ItemStack> stacks) {
        StringBuilder sb = new StringBuilder();
        for (ItemStack st : stacks) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(st.getHoverName().getString()).append(" x").append(st.getCount());
        }
        return sb.length() == 0 ? "-" : sb.toString();
    }

    /** Une ligne par joueur dans la table events ; le journal du serveur garde aussi la trace si la base est coupee. */
    private static void journal(MinecraftServer server, Session s, String kind, String itemsA, String itemsB) {
        write(server, kind, s.a, s.b, itemsA, itemsB);
        write(server, kind, s.b, s.a, itemsA == null ? null : itemsB, itemsA == null ? itemsB : itemsA);
    }

    private static void write(MinecraftServer server, String kind, Side me, Side other, String gave, String got) {
        String detail = "avec " + other.name + " (" + other.id + ")"
                + (gave == null ? " | annule: " + got : " | donne: " + gave + " + " + me.gold + " or | recoit: " + got + " + " + other.gold + " or");
        if (detail.length() > 500) detail = detail.substring(0, 497) + "...";
        MMORPG.LOGGER.info("[Echange] {} {} : {}", kind, me.name, detail);
        if (!Database.enabled()) return;
        final String text = detail;
        Database.fire("journal echange", c -> {
            try (var ps = c.prepareStatement("INSERT INTO events (server_id, uuid, name, kind, amount, detail) VALUES (?, ?, ?, ?, ?, ?)")) {
                ps.setString(1, Database.config().serverId);
                ps.setString(2, me.id.toString());
                ps.setString(3, me.name);
                ps.setString(4, kind);
                ps.setLong(5, gave == null ? 0 : me.gold);
                ps.setString(6, text);
                ps.executeUpdate();
            }
            return null;
        });
    }

    // ================================================================== fin de session

    private static void finish(Session s, ServerPlayer pa, ServerPlayer pb, String text, int color) {
        SESSIONS.remove(s.a.id);
        SESSIONS.remove(s.b.id);
        for (ServerPlayer p : new ServerPlayer[]{pa, pb}) {
            if (p == null) continue;
            msg(p, text, color);
            p.level().playSound(null, p.blockPosition(), com.mmorpg.registry.ModSounds.COINS.get(), SoundSource.NEUTRAL, 0.8f, 1.1f);
            CompoundTag t = new CompoundTag();
            t.putBoolean("closed", true);
            Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.TRADE, t));
        }
    }

    /** Annule sans rien deplacer : les objets n'ont jamais quitte leur inventaire. */
    public static void cancel(Session s, String reason) {
        MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        ServerPlayer pa = server == null ? null : resolve(server, s.a.id);
        ServerPlayer pb = server == null ? null : resolve(server, s.b.id);
        if (server != null) journal(server, s, "trade_cancel", null, reason);
        finish(s, pa, pb, reason, 0xFF9060);
    }

    // ================================================================== envoi de l'etat aux deux clients

    private static Tag encode(MinecraftServer server, ItemStack stack) {
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, server.registryAccess());
        return ItemStack.OPTIONAL_CODEC.encodeStart(ops, stack).result().orElse(new CompoundTag());
    }

    private static void push(Session s, ServerPlayer pa, ServerPlayer pb, String status) {
        send(s, pa, pb, status);
        send(s, pb, pa, status);
    }

    private static void send(Session s, ServerPlayer viewer, ServerPlayer partner, String status) {
        MinecraftServer server = viewer.level().getServer();
        Side me = s.of(viewer.getUUID());
        Side them = s.other(viewer.getUUID());
        CompoundTag t = new CompoundTag();
        t.putString("partner", them.name);
        t.putString("status", status);
        t.putInt("stage", s.bothReady() ? 1 : 0);
        t.putLong("left", s.bothReady() ? Math.max(0, CONFIRM_DELAY_MS - (System.currentTimeMillis() - s.stageSince)) : 0);
        t.putBoolean("myReady", me.ready);
        t.putBoolean("theirReady", them.ready);
        t.putBoolean("myConfirmed", me.confirmed);
        t.putBoolean("theirConfirmed", them.confirmed);
        t.putLong("myGold", me.gold);
        t.putLong("theirGold", them.gold);
        t.putLong("wallet", RpgPlayers.get(viewer).gold);
        ListTag mine = new ListTag();
        for (int slot : me.offers.keySet()) {
            CompoundTag e = new CompoundTag();
            e.putInt("slot", slot);
            e.put("stack", encode(server, viewer.getInventory().getItem(slot)));
            mine.add(e);
        }
        t.put("mine", mine);
        ListTag theirs = new ListTag();
        for (int slot : them.offers.keySet()) {
            CompoundTag e = new CompoundTag();
            e.put("stack", encode(server, partner.getInventory().getItem(slot)));
            theirs.add(e);
        }
        t.put("theirs", theirs);
        Net.toPlayer(viewer, new S2COpenScreen(S2COpenScreen.TRADE, t));
    }

    // ================================================================== evenements

    @SubscribeEvent
    public static void tick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 10 != 0) return;
        long now = System.currentTimeMillis();
        REQUESTS.values().removeIf(r -> r.expires() < now);
        for (Session s : new ArrayList<>(new java.util.LinkedHashSet<>(SESSIONS.values()))) {
            ServerPlayer pa = resolve(e.getServer(), s.a.id);
            ServerPlayer pb = resolve(e.getServer(), s.b.id);
            if (pa == null || pb == null || !pa.isAlive() || !pb.isAlive()) {
                cancel(s, "L'échange est annulé (joueur absent ou mort).");
            } else if (!close(pa, pb)) {
                cancel(s, "Les joueurs se sont trop éloignés : échange annulé.");
            } else if (prune(s, pa, pb)) {
                push(s, pa, pb, "Un objet proposé a changé : l'échange est à revalider.");
            }
        }
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        UUID id = e.getEntity().getUUID();
        REQUESTS.remove(id);
        REQUESTS.values().removeIf(r -> r.from().equals(id));
        Session s = SESSIONS.get(id);
        if (s != null) cancel(s, e.getEntity().getGameProfile().name() + " s'est déconnecté : échange annulé.");
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent e) {
        clear();
    }

    // ================================================================== acces pour les tests serveur

    public static boolean active(ServerPlayer p) {
        return SESSIONS.containsKey(p.getUUID());
    }

    public static long offeredGold(ServerPlayer p) {
        Session s = SESSIONS.get(p.getUUID());
        return s == null ? 0 : s.of(p.getUUID()).gold;
    }

    public static void simulate(ServerPlayer p) {
        SIMULATED.put(p.getUUID(), p);
    }

    /** Fait comme si le delai de securite de « Confirmer » etait ecoule. */
    public static void skipConfirmDelay(ServerPlayer p) {
        Session s = SESSIONS.get(p.getUUID());
        if (s != null) s.stageSince -= CONFIRM_DELAY_MS;
    }

    public static void clear() {
        REQUESTS.clear();
        SESSIONS.clear();
        SIMULATED.clear();
    }
}
