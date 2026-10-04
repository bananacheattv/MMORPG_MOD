package com.mmorpg.server;

import com.mmorpg.network.Net;
import com.mmorpg.network.Payloads;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.rpg.Stat;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Groupes de joueurs (5 maximum) : partage d'experience avec bonus de groupe, progression de quetes commune,
 * pas de degats entre membres, chat de groupe et cadres de vie des coequipiers.
 */
public final class PartyManager {
    public static final int MAX_SIZE = 5;
    private static final long INVITE_MS = 60_000;

    public static final class Party {
        public UUID leader;
        public final LinkedHashSet<UUID> members = new LinkedHashSet<>();
    }

    private record Invite(UUID from, String fromName, long expires) {
    }

    private static final Map<UUID, Party> BY_PLAYER = new HashMap<>();
    private static final Map<UUID, Invite> INVITES = new HashMap<>();

    private PartyManager() {
    }

    public static Party partyOf(Player p) {
        return BY_PLAYER.get(p.getUUID());
    }

    public static boolean sameParty(Player a, Player b) {
        Party pa = BY_PLAYER.get(a.getUUID());
        return pa != null && pa.members.contains(b.getUUID());
    }

    /** Membres connectes du groupe (le joueur seul s'il n'a pas de groupe). */
    public static List<ServerPlayer> onlineMembers(ServerPlayer p) {
        List<ServerPlayer> list = new ArrayList<>();
        Party party = partyOf(p);
        if (party == null) {
            list.add(p);
            return list;
        }
        for (UUID id : party.members) {
            ServerPlayer m = p.level().getServer().getPlayerList().getPlayer(id);
            if (m != null) list.add(m);
        }
        return list;
    }

    private static void msg(ServerPlayer p, String text, int color) {
        p.sendSystemMessage(Component.literal("[Groupe] ").withColor(0x60C0FF).append(Component.literal(text).withColor(color)));
    }

    private static void broadcast(Party party, MinecraftServer server, Component c) {
        for (UUID id : party.members) {
            ServerPlayer m = server.getPlayerList().getPlayer(id);
            if (m != null) m.sendSystemMessage(c);
        }
    }

    public static void invite(ServerPlayer from, String targetName) {
        ServerPlayer target = from.level().getServer().getPlayerList().getPlayerByName(targetName);
        if (target == null) {
            msg(from, "Joueur introuvable : " + targetName, 0xFF6060);
            return;
        }
        if (target == from) {
            msg(from, "Vous ne pouvez pas vous inviter vous-même.", 0xFF6060);
            return;
        }
        Party party = partyOf(from);
        if (party != null && !party.leader.equals(from.getUUID())) {
            msg(from, "Seul le chef du groupe peut inviter.", 0xFF6060);
            return;
        }
        if (party != null && party.members.size() >= MAX_SIZE) {
            msg(from, "Le groupe est complet (" + MAX_SIZE + " membres).", 0xFF6060);
            return;
        }
        if (partyOf(target) != null) {
            msg(from, target.getName().getString() + " fait déjà partie d'un groupe.", 0xFF6060);
            return;
        }
        INVITES.put(target.getUUID(), new Invite(from.getUUID(), from.getName().getString(), System.currentTimeMillis() + INVITE_MS));
        msg(from, "Invitation envoyée à " + target.getName().getString() + ".", 0xE0D8C0);
        MutableComponent accept = Component.literal("[Accepter]").withStyle(s -> s.withColor(0x60E060).withBold(true)
                .withClickEvent(new ClickEvent.RunCommand("/groupe accepter"))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("Rejoindre le groupe"))));
        MutableComponent decline = Component.literal("[Refuser]").withStyle(s -> s.withColor(0xFF6060).withBold(true)
                .withClickEvent(new ClickEvent.RunCommand("/groupe refuser")));
        target.sendSystemMessage(Component.literal("[Groupe] ").withColor(0x60C0FF)
                .append(Component.literal(from.getName().getString() + " vous invite dans son groupe. ").withColor(0xE0D8C0))
                .append(accept).append(Component.literal(" ")).append(decline));
        sync(target);
    }

    public static void accept(ServerPlayer p) {
        Invite inv = INVITES.remove(p.getUUID());
        if (inv == null || inv.expires < System.currentTimeMillis()) {
            msg(p, "Aucune invitation en attente.", 0xFF6060);
            sync(p);
            return;
        }
        ServerPlayer inviter = p.level().getServer().getPlayerList().getPlayer(inv.from);
        if (inviter == null) {
            msg(p, "Le joueur qui vous a invité n'est plus connecté.", 0xFF6060);
            sync(p);
            return;
        }
        if (partyOf(p) != null) leave(p);
        Party party = partyOf(inviter);
        if (party == null) {
            party = new Party();
            party.leader = inviter.getUUID();
            party.members.add(inviter.getUUID());
            BY_PLAYER.put(inviter.getUUID(), party);
        }
        if (party.members.size() >= MAX_SIZE) {
            msg(p, "Le groupe est complet.", 0xFF6060);
            return;
        }
        party.members.add(p.getUUID());
        BY_PLAYER.put(p.getUUID(), party);
        broadcast(party, p.level().getServer(), Component.literal("[Groupe] ").withColor(0x60C0FF)
                .append(Component.literal(p.getName().getString() + " a rejoint le groupe.").withColor(0x60E060)));
        syncParty(party, p.level().getServer());
    }

    public static void decline(ServerPlayer p) {
        Invite inv = INVITES.remove(p.getUUID());
        if (inv != null) {
            ServerPlayer inviter = p.level().getServer().getPlayerList().getPlayer(inv.from);
            if (inviter != null) msg(inviter, p.getName().getString() + " a refusé votre invitation.", 0xFF9070);
            msg(p, "Invitation refusée.", 0xE0D8C0);
        }
        sync(p);
    }

    public static void leave(ServerPlayer p) {
        Party party = BY_PLAYER.remove(p.getUUID());
        if (party == null) {
            sync(p);
            return;
        }
        party.members.remove(p.getUUID());
        MinecraftServer server = p.level().getServer();
        msg(p, "Vous avez quitté le groupe.", 0xE0D8C0);
        sync(p);
        if (party.members.size() < 2) {
            for (UUID id : party.members) {
                BY_PLAYER.remove(id);
                ServerPlayer m = server.getPlayerList().getPlayer(id);
                if (m != null) {
                    msg(m, "Le groupe a été dissous.", 0xFF9070);
                    sync(m);
                }
            }
            return;
        }
        if (party.leader.equals(p.getUUID())) party.leader = party.members.iterator().next();
        broadcast(party, server, Component.literal("[Groupe] ").withColor(0x60C0FF)
                .append(Component.literal(p.getName().getString() + " a quitté le groupe.").withColor(0xFF9070)));
        syncParty(party, server);
    }

    public static void kick(ServerPlayer leader, String name) {
        Party party = partyOf(leader);
        if (party == null || !party.leader.equals(leader.getUUID())) {
            msg(leader, "Seul le chef du groupe peut exclure un membre.", 0xFF6060);
            return;
        }
        ServerPlayer target = leader.level().getServer().getPlayerList().getPlayerByName(name);
        if (target == null || target == leader || !party.members.contains(target.getUUID())) {
            msg(leader, "Ce joueur n'est pas dans votre groupe.", 0xFF6060);
            return;
        }
        msg(target, "Vous avez été exclu du groupe.", 0xFF6060);
        leave(target);
    }

    public static void promote(ServerPlayer leader, String name) {
        Party party = partyOf(leader);
        if (party == null || !party.leader.equals(leader.getUUID())) return;
        ServerPlayer target = leader.level().getServer().getPlayerList().getPlayerByName(name);
        if (target == null || !party.members.contains(target.getUUID())) return;
        party.leader = target.getUUID();
        broadcast(party, leader.level().getServer(), Component.literal("[Groupe] ").withColor(0x60C0FF)
                .append(Component.literal(target.getName().getString() + " est le nouveau chef du groupe.").withColor(0xFFD040)));
        syncParty(party, leader.level().getServer());
    }

    public static void chat(ServerPlayer p, String message) {
        Party party = partyOf(p);
        if (party == null) {
            msg(p, "Vous n'êtes dans aucun groupe.", 0xFF6060);
            return;
        }
        broadcast(party, p.level().getServer(), Component.literal("[Groupe] ").withColor(0x60C0FF)
                .append(Component.literal(p.getName().getString() + " : ").withColor(0xFFD040))
                .append(Component.literal(message).withColor(0xE8F4FF)));
    }

    public static void list(ServerPlayer p) {
        Party party = partyOf(p);
        if (party == null) {
            msg(p, "Vous n'êtes dans aucun groupe. /groupe inviter <joueur>", 0xE0D8C0);
            return;
        }
        StringBuilder sb = new StringBuilder("Membres : ");
        for (UUID id : party.members) {
            ServerPlayer m = p.level().getServer().getPlayerList().getPlayer(id);
            sb.append(m != null ? m.getName().getString() : "?").append(id.equals(party.leader) ? " (chef)" : "").append("  ");
        }
        msg(p, sb.toString(), 0xE0D8C0);
    }

    // ================================================================== synchronisation

    public static void sync(ServerPlayer p) {
        CompoundTag t = new CompoundTag();
        Party party = partyOf(p);
        ListTag members = new ListTag();
        if (party != null) {
            MinecraftServer server = p.level().getServer();
            for (UUID id : party.members) {
                ServerPlayer m = server.getPlayerList().getPlayer(id);
                CompoundTag mt = new CompoundTag();
                mt.putString("uuid", id.toString());
                mt.putBoolean("leader", id.equals(party.leader));
                mt.putBoolean("online", m != null);
                if (m != null) {
                    PlayerData d = RpgPlayers.get(m);
                    double max = d.stats.get(Stat.MAX_HP);
                    double maxMana = d.stats.get(Stat.MAX_MANA);
                    mt.putString("name", m.getName().getString());
                    mt.putInt("class", d.playerClass.ordinal());
                    mt.putInt("level", d.level);
                    mt.putFloat("hp", max > 0 ? (float) (d.hp / max) : 1);
                    mt.putFloat("mana", maxMana > 0 ? (float) (d.mana / maxMana) : 1);
                    mt.putBoolean("near", m.level() == p.level() && m.distanceToSqr(p) < 64 * 64);
                }
                members.add(mt);
            }
            t.putBoolean("isLeader", party.leader.equals(p.getUUID()));
        }
        t.put("members", members);
        Invite inv = INVITES.get(p.getUUID());
        if (inv != null && inv.expires > System.currentTimeMillis()) t.putString("invite", inv.fromName);
        Net.toPlayer(p, new Payloads.PartySync(t));
    }

    private static void syncParty(Party party, MinecraftServer server) {
        for (UUID id : party.members) {
            ServerPlayer m = server.getPlayerList().getPlayer(id);
            if (m != null) sync(m);
        }
    }

    /** Appele toutes les 10 ticks : met a jour les barres de vie des coequipiers. */
    public static void tick(MinecraftServer server) {
        if (server.getTickCount() % 10 != 0) return;
        for (Party party : new LinkedHashSet<>(BY_PLAYER.values())) {
            syncParty(party, server);
        }
        INVITES.values().removeIf(i -> i.expires < System.currentTimeMillis());
    }

    public static void onLogout(ServerPlayer p) {
        INVITES.remove(p.getUUID());
        if (partyOf(p) != null) leave(p);
    }

    public static void clear() {
        BY_PLAYER.clear();
        INVITES.clear();
    }
}
