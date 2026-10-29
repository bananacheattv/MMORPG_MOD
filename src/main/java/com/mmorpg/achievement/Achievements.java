package com.mmorpg.achievement;

import com.mmorpg.MMORPG;
import com.mmorpg.job.Job;
import com.mmorpg.network.Net;
import com.mmorpg.network.Payloads;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.LinkedHashSet;
import java.util.Set;

/** Deblocage des succes et des titres (cote serveur), synchronisation MariaDB et titre choisi. */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class Achievements {
    private Achievements() {
    }

    // ------------------------------------------------------------------ compteurs

    /** Ajoute {@code amount} a un compteur cumulatif. */
    public static void add(ServerPlayer p, String stat, long amount) {
        if (amount <= 0 || com.mmorpg.hub.ClassTrial.active(p) || !com.mmorpg.server.FeatureFlags.enabled(p.level().getServer(), "succes")) return;
        PlayerData d = RpgPlayers.get(p);
        d.counters.merge(stat, amount, Long::sum);
        check(p, d, stat);
    }

    /** Releve un compteur de type "maximum" (niveaux). */
    public static void max(ServerPlayer p, String stat, long value) {
        if (!com.mmorpg.server.FeatureFlags.enabled(p.level().getServer(), "succes")) return;
        PlayerData d = RpgPlayers.get(p);
        if (d.counters.getOrDefault(stat, 0L) >= value) return;
        d.counters.put(stat, value);
        check(p, d, stat);
    }

    private static void check(ServerPlayer p, PlayerData d, String stat) {
        long value = d.counters.getOrDefault(stat, 0L);
        boolean unlocked = false;
        for (Achievement a : Achievement.forStat(stat)) {
            if (value >= a.target() && !d.achievements.containsKey(a.id())) {
                d.achievements.put(a.id(), System.currentTimeMillis() / 1000);
                unlocked = true;
                announce(p, a);
            }
        }
        d.dirty = true;
        if (unlocked) store(p, d);
    }

    private static void announce(ServerPlayer p, Achievement a) {
        Net.toPlayer(p, new Payloads.Notify(Payloads.Notify.QUEST, "SUCCÈS DÉBLOQUÉ", a.name(), a.category().color & 0xFFFFFF));
        p.sendSystemMessage(Component.literal("★ Succès : ").withColor(0xE2B85A)
                .append(Component.literal(a.name()).withColor(0xFFE6A0))
                .append(Component.literal(a.hasTitle() ? " — titre « " + a.title() + " » débloqué" : "").withColor(0xB9ABCF)));
    }

    private static void store(ServerPlayer p, PlayerData d) {
        if (AchievementStore.enabled()) AchievementStore.save(p.getUUID(), d.achievements, d.counters, d.pvpTitle);
    }

    /** Reprend l'existant (niveau, metiers) pour les joueurs qui avaient deja progresse avant les succes. */
    private static void backfill(ServerPlayer p, PlayerData d) {
        max(p, Achievement.LEVEL, d.level);
        for (Job j : Job.values()) max(p, Achievement.jobStat(j.id), Job.levelFor(d.jobXp.getOrDefault(j.id, 0L)));
    }

    // ------------------------------------------------------------------ titres

    public static Set<String> titles(PlayerData d) {
        Set<String> out = new LinkedHashSet<>();
        for (Achievement a : Achievement.all().values()) if (a.hasTitle() && d.achievements.containsKey(a.id())) out.add(a.title());
        return out;
    }

    /** Titre a ajouter apres le pseudo (vide si aucun) ; utilisable par tout chat, y compris celui de guilde. */
    public static Component suffix(PlayerData d) {
        String t = d.pvpTitle == null ? "" : d.pvpTitle;
        return t.isEmpty() ? Component.empty() : Component.literal(" « " + t + " »").withColor(0xE8C36A);
    }

    public static boolean staff(String rankLabel) {
        return "Fondateur".equals(rankLabel) || "Administrateur".equals(rankLabel);
    }

    /** Texte d'un message de chat : rouge clair et gras pour les Fondateurs et Administrateurs. */
    public static Component body(String rankLabel, String text, int color) {
        var c = Component.literal(text);
        return staff(rankLabel) ? c.withColor(0xFF6B6B).withStyle(s -> s.withBold(true)) : c.withColor(color);
    }

    /** Rang + pseudo + titre equipe, pour les chats de groupe et de guilde. */
    public static Component chatName(String rankLabel, int rankColor, String name, String title) {
        int color = staff(rankLabel) ? 0xFF6B6B : rankColor;
        var n = com.mmorpg.store.RankTags.name(rankLabel, color, name).copy();
        if (title != null && !title.isEmpty()) n.append(Component.literal(" « " + title + " »").withColor(0xE8C36A));
        return n;
    }

    public static Component suffix(ServerPlayer p) {
        return com.mmorpg.server.FeatureFlags.enabled(p.level().getServer(), "succes") ? suffix(RpgPlayers.get(p)) : Component.empty();
    }

    /** Equipe (ou retire, avec "") un titre de succes ou un titre PvP deja possede. */
    public static void equip(ServerPlayer p, String title) {
        PlayerData d = RpgPlayers.get(p);
        if (!title.isEmpty() && !titles(d).contains(title)
                && !com.mmorpg.competition.PvpProgression.access(p.level().getServer(), s -> com.mmorpg.competition.PvpProgression.account(s, p.getUUID()).unlocks.contains("title:" + title))) {
            p.sendSystemMessage(Component.literal("Titre non débloqué.").withColor(0xFF6B6B));
            return;
        }
        d.pvpTitle = title;
        d.dirty = true;
        p.refreshDisplayName();
        RpgPlayers.updatePublic(p);
        store(p, d);
    }

    public static void action(ServerPlayer p, CompoundTag t) {
        if (!com.mmorpg.server.FeatureFlags.allow(p, "succes")) return;
        if (t.getStringOr("action", "").equals("equip")) equip(p, t.getStringOr("title", ""));
    }

    // ------------------------------------------------------------------ evenements

    @SubscribeEvent
    public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p)) return;
        com.mmorpg.server.Scheduler.later(p.level(), 40, () -> {
            if (p.isRemoved()) return;
            backfill(p, RpgPlayers.get(p));
            if (!AchievementStore.enabled()) return;
            var server = p.level().getServer();
            AchievementStore.load(p.getUUID(), loaded -> server.execute(() -> {
                if (p.isRemoved()) return;
                PlayerData d = RpgPlayers.get(p);
                loaded.achievements().forEach((k, v) -> d.achievements.putIfAbsent(k, v));
                loaded.stats().forEach((k, v) -> d.counters.merge(k, v, Math::max));
                if (d.pvpTitle.isEmpty() && !loaded.title().isEmpty()) { d.pvpTitle = loaded.title(); p.refreshDisplayName(); }
                d.dirty = true;
                RpgPlayers.updatePublic(p);
                store(p, d);          // pousse ce que la base ne connaissait pas encore
            }));
        });
    }

    @SubscribeEvent
    public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) store(p, RpgPlayers.get(p));
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post e) {
        if (e.getEntity() instanceof ServerPlayer p && p.tickCount % 2400 == 1200) store(p, RpgPlayers.get(p));
    }
}
