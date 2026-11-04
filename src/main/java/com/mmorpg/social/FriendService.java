package com.mmorpg.social;

import com.mmorpg.MMORPG;
import com.mmorpg.guild.GuildService;
import com.mmorpg.network.*;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.util.*;

/** Amis avec statut (en ligne, occupé, absent, invisible), partagés entre serveurs. */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class FriendService {
    public static final List<String> STATUS = List.of("EN_LIGNE", "OCCUPE", "ABSENT", "INVISIBLE");
    public static final int MAX_FRIENDS = 100;

    public static final class Entry {
        public String name = "", status = "EN_LIGNE";
        public long lastSeen;
        public ArrayList<UUID> friends = new ArrayList<>(), requests = new ArrayList<>();
    }
    public static final class State { public LinkedHashMap<UUID, Entry> entries = new LinkedHashMap<>(); }

    private FriendService() {}

    private static UUID key(ServerPlayer p) { return com.mmorpg.character.CharKey.of(p); }
    private static Entry entry(State s, UUID id, String name) {
        var e = s.entries.computeIfAbsent(id, k -> new Entry());
        if (name != null && !name.isEmpty()) e.name = name;
        return e;
    }
    private static <T> T access(MinecraftServer server, java.util.function.Function<State, T> f) {
        return SharedLedger.access(server, "friends", State.class, State::new, f);
    }
    private static void require(boolean ok, String message) { if (!ok) throw new IllegalArgumentException(message); }

    public static void open(ServerPlayer p) { open(p, ""); }

    public static void open(ServerPlayer p, String status) {
        try {
            var server = p.level().getServer();
            var online = new HashSet<UUID>();
            for (var m : GuildService.online(server)) online.add(m.id);
            var data = access(server, s -> {
                var me = entry(s, key(p), com.mmorpg.character.CharKey.name(p));
                var out = new CompoundTag();
                out.putString("status", status); out.putString("mine", me.status);
                var list = new ListTag();
                for (var id : me.friends) {
                    var f = s.entries.get(id); if (f == null) continue;
                    boolean on = online.contains(id) && !f.status.equals("INVISIBLE");
                    var t = new CompoundTag();
                    t.putString("id", id.toString()); t.putString("name", f.name); t.putBoolean("online", on);
                    t.putString("state", on ? f.status : "HORS_LIGNE"); t.putLong("seen", f.lastSeen);
                    list.add(t);
                }
                out.put("friends", list);
                var reqs = new ListTag();
                for (var id : me.requests) { var f = s.entries.get(id); var t = new CompoundTag(); t.putString("id", id.toString()); t.putString("name", f == null ? "?" : f.name); reqs.add(t); }
                out.put("requests", reqs);
                return out;
            });
            Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.FRIENDS, data));
        } catch (Exception e) { p.sendSystemMessage(Component.literal("Amis indisponibles : " + e.getMessage())); }
    }

    public static void action(ServerPlayer p, CompoundTag data) {
        String action = data.getStringOr("action", "");
        try {
            var server = p.level().getServer();
            UUID self = key(p), target = null;
            String idText = data.getStringOr("id", "");
            if (!idText.isEmpty()) target = UUID.fromString(idText);
            final UUID other = target;
            switch (action) {
                case "add" -> {
                    String wanted = data.getStringOr("name", "").trim();
                    var found = GuildService.online(server).stream().filter(m -> m.name.equalsIgnoreCase(wanted)).findFirst().orElse(null);
                    require(found != null, "Joueur introuvable ou hors ligne.");
                    require(!found.id.equals(self), "Tu ne peux pas t’ajouter toi-même.");
                    var result = access(server, s -> {
                        var me = entry(s, self, com.mmorpg.character.CharKey.name(p)); var them = entry(s, found.id, found.name);
                        require(!me.friends.contains(found.id), "Déjà dans ta liste d’amis.");
                        require(me.friends.size() < MAX_FRIENDS, "Liste d’amis pleine.");
                        if (me.requests.remove(found.id)) { me.friends.add(found.id); them.friends.add(self); them.requests.remove(self); return "Vous êtes maintenant amis."; }
                        require(!them.requests.contains(self), "Demande déjà envoyée.");
                        require(them.requests.size() < 50, "Ce joueur a trop de demandes en attente.");
                        them.requests.add(self);
                        return "Demande envoyée à " + found.name + ".";
                    });
                    var targetPlayer = server.getPlayerList().getPlayers().stream().filter(x -> key(x).equals(found.id)).findFirst().orElse(null);
                    if (targetPlayer != null) targetPlayer.sendSystemMessage(Component.literal(com.mmorpg.character.CharKey.name(p) + " veut devenir ton ami · /amis").withColor(0xD5B0FF));
                    open(p, result); return;
                }
                case "accept" -> { require(other != null, "Demande inconnue.");
                    access(server, s -> { var me = entry(s, self, null); require(me.requests.remove(other), "Demande expirée."); var them = entry(s, other, null);
                        require(me.friends.size() < MAX_FRIENDS, "Liste d’amis pleine."); if (!me.friends.contains(other)) me.friends.add(other); if (!them.friends.contains(self)) them.friends.add(self); return null; }); }
                case "decline" -> { require(other != null, "Demande inconnue."); access(server, s -> { entry(s, self, null).requests.remove(other); return null; }); }
                case "remove" -> { require(other != null, "Ami inconnu."); access(server, s -> { entry(s, self, null).friends.remove(other); entry(s, other, null).friends.remove(self); return null; }); }
                case "status" -> { String value = data.getStringOr("value", ""); require(STATUS.contains(value), "Statut inconnu."); access(server, s -> { entry(s, self, null).status = value; return null; }); }
                default -> throw new IllegalArgumentException("Action inconnue.");
            }
            open(p, "Liste mise à jour.");
        } catch (Exception e) { open(p, e.getMessage() == null ? "Opération refusée." : e.getMessage()); }
    }

    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        try {
            var server = p.level().getServer();
            var friends = access(server, s -> { var me = entry(s, key(p), com.mmorpg.character.CharKey.name(p)); me.lastSeen = System.currentTimeMillis(); return me.status.equals("INVISIBLE") ? new ArrayList<UUID>() : new ArrayList<>(me.friends); });
            for (var other : server.getPlayerList().getPlayers()) if (friends.contains(key(other)))
                other.sendSystemMessage(Component.literal(com.mmorpg.character.CharKey.name(p) + " vient de se connecter.").withColor(0x8CE08C));
        } catch (Exception ex) { MMORPG.LOGGER.debug("Friends login: {}", ex.getMessage()); }
    }

    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        try { access(p.level().getServer(), s -> { entry(s, key(p), com.mmorpg.character.CharKey.name(p)).lastSeen = System.currentTimeMillis(); return null; }); }
        catch (Exception ex) { MMORPG.LOGGER.debug("Friends logout: {}", ex.getMessage()); }
    }
}
