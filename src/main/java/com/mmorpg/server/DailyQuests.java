package com.mmorpg.server;

import com.mmorpg.network.Net;
import com.mmorpg.network.Payloads;
import com.mmorpg.quest.QuestDef;
import com.mmorpg.rpg.PlayerData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Quetes quotidiennes aleatoires : chaque jour (jour civil du serveur) chaque joueur recoit trois quetes tirees au hasard parmi
 * huit modeles, mises a l'echelle de son palier de niveau. Elles se prennent et se rendent directement depuis le journal de
 * quetes (aucun PNJ). Identifiant : {@code rdaily:<jour>:<emplacement>:<palier>:<modele>} ; la progression vit dans les quetes
 * actives du profil, l'etat « rendue » dans {@code dailyQuests} ({@code rdailydone:<jour>:<emplacement>}). Tout est valide cote serveur.
 */
public final class DailyQuests {
    public static final String PREFIX = "rdaily:";
    public static final String DONE_PREFIX = "rdailydone:";
    public static final int SLOTS = 3;
    /** Multiplicateur de quantite par palier de niveau (1-24, 25-49, 50-74, 75-99, 100). */
    private static final int[] SCALE = {1, 2, 3, 5, 8};
    private static final int TEMPLATES = 8;

    private DailyQuests() {
    }

    public static String id(long day, int slot, int band, int tpl) {
        return PREFIX + day + ":" + slot + ":" + band + ":" + tpl;
    }

    public static int band(PlayerData d) {
        return Math.max(0, Math.min(SCALE.length - 1, d.level / 25));
    }

    /** Les trois modeles du jour pour ce joueur : stables pour un jour donne, differents d'un joueur a l'autre. */
    static List<Integer> picks(UUID player, long day) {
        List<Integer> all = new ArrayList<>();
        for (int i = 0; i < TEMPLATES; i++) all.add(i);
        Collections.shuffle(all, new Random(player.getMostSignificantBits() * 31 + player.getLeastSignificantBits() * 17 + day * 1_000_003L));
        return all.subList(0, SLOTS);
    }

    public static String offeredId(ServerPlayer p, PlayerData d, int slot) {
        long day = QuestManager.today();
        return id(day, slot, band(d), picks(p.getUUID(), day).get(slot));
    }

    private static int[] parse(String id) {
        if (!id.startsWith(PREFIX)) return null;
        String[] s = id.substring(PREFIX.length()).split(":");
        if (s.length != 4) return null;
        try {
            int slot = Integer.parseInt(s[1]), band = Integer.parseInt(s[2]), tpl = Integer.parseInt(s[3]);
            if (slot < 0 || slot >= SLOTS || band < 0 || band >= SCALE.length || tpl < 0 || tpl >= TEMPLATES) return null;
            return new int[]{(int) Long.parseLong(s[0]), slot, band, tpl};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static long dayOf(String id) {
        int[] k = parse(id);
        return k == null ? -1 : k[0];
    }

    public static String doneKey(String id) {
        int[] k = parse(id);
        return DONE_PREFIX + (k == null ? "?" : k[0] + ":" + k[1]);
    }

    static boolean done(PlayerData d, long day, int slot) {
        Long v = d.dailyQuests.get(DONE_PREFIX + day + ":" + slot);
        return v != null && v == day;
    }

    private static boolean activeSlot(PlayerData d, long day, int slot) {
        for (String id : d.activeQuests.keySet()) {
            int[] k = parse(id);
            if (k != null && k[0] == day && k[1] == slot) return true;
        }
        return false;
    }

    private static QuestDef.Objective kill(String t, int n) {
        return new QuestDef.Objective("kill", t, n);
    }

    private static QuestDef.Objective collect(String t, int n) {
        return new QuestDef.Objective("collect", t, n);
    }

    /** Definition d'une quete du jour d'apres son identifiant ; null si invalide. */
    public static QuestDef def(String id) {
        int[] k = parse(id);
        if (k == null) return null;
        int slot = k[1], band = k[2], tpl = k[3], f = SCALE[band];
        QuestDef q = new QuestDef();
        q.daily = true;
        q.minLevel = 1;
        String name, desc;
        switch (tpl) {
            case 0 -> { name = "Chasse du jour"; desc = "Éliminez " + 15 * f + " monstres, quels qu'ils soient."; q.objectives.add(kill("*", 15 * f)); }
            case 1 -> {
                name = "Chasseur de boss";
                if (band == 0) { desc = "Éliminez " + 30 + " monstres : les boss viendront plus tard."; q.objectives.add(kill("*", 30)); }
                else { desc = "Vainquez " + Math.max(1, f / 2) + " boss."; q.objectives.add(kill("boss", Math.max(1, f / 2))); }
            }
            case 2 -> { name = "Fer pour la forge"; desc = "Rapportez " + 8 * f + " lingots de fer."; q.objectives.add(collect("minecraft:iron_ingot", 8 * f)); }
            case 3 -> { name = "Réserve de charbon"; desc = "Rapportez " + 16 * f + " charbons."; q.objectives.add(collect("minecraft:coal", 16 * f)); }
            case 4 -> { name = "Lingots d'or"; desc = "Rapportez " + 4 * f + " lingots d'or."; q.objectives.add(collect("minecraft:gold_ingot", 4 * f)); }
            case 5 -> { name = "Patrouille"; desc = "Éliminez " + 10 * f + " monstres et rapportez " + 8 * f + " charbons."; q.objectives.add(kill("*", 10 * f)); q.objectives.add(collect("minecraft:coal", 8 * f)); }
            case 6 -> { name = "Grande purge"; desc = "Éliminez " + 25 * f + " monstres."; q.objectives.add(kill("*", 25 * f)); }
            default -> { name = "Quartier de roche"; desc = "Rapportez " + 24 * f + " blocs de cobblestone pour les remparts."; q.objectives.add(collect("minecraft:cobblestone", 24 * f)); }
        }
        q.name = name;
        q.description = "Quête quotidienne : " + desc;
        q.rewards.gold = (40L + 30L * slot) * f;
        q.rewards.xpPercentOfLevel = 10 + 8 * slot;
        if (slot == SLOTS - 1) q.rewards.items.add(new QuestDef.ItemReward("minecraft:emerald", 1 + band));
        return q;
    }

    /** Nouveau jour : retire les quetes du jour perimees (actives ou terminees). */
    public static void purge(PlayerData d) {
        long day = QuestManager.today();
        boolean changed = d.activeQuests.keySet().removeIf(id -> id.startsWith(PREFIX) && dayOf(id) != day);
        d.questGivers.keySet().removeIf(id -> id.startsWith(PREFIX) && dayOf(id) != day);
        if (d.trackedQuest.startsWith(PREFIX) && dayOf(d.trackedQuest) != day) d.trackedQuest = "";
        changed |= d.dailyQuests.entrySet().removeIf(e -> e.getKey().startsWith(DONE_PREFIX) && e.getValue() != day);
        if (changed) d.dirty = true;
    }

    /** Quetes du jour pas encore prises ni rendues, pour l'onglet « Journalières » du journal. */
    public static ListTag offersTag(ServerPlayer p, PlayerData d) {
        purge(d);
        ListTag list = new ListTag();
        if (!d.playerClass.isPlayable()) return list;
        long day = QuestManager.today();
        for (int slot = 0; slot < SLOTS; slot++) {
            if (done(d, day, slot) || activeSlot(d, day, slot)) continue;
            String id = offeredId(p, d, slot);
            CompoundTag t = QuestManager.questTag(p, d, id, def(id));
            t.putBoolean("offer", true);
            list.add(t);
        }
        return list;
    }

    private static String activeIdOfSlot(PlayerData d, long day, int slot) {
        for (String id : d.activeQuests.keySet()) {
            int[] k = parse(id);
            if (k != null && k[0] == day && k[1] == slot) return id;
        }
        return null;
    }

    /** Les trois quotidiennes pour la pancarte : etat 0 a prendre, 1 en cours, 2 terminee. */
    public static ListTag boardTag(ServerPlayer p, PlayerData d) {
        purge(d);
        ListTag list = new ListTag();
        long day = QuestManager.today();
        for (int slot = 0; slot < SLOTS; slot++) {
            boolean finished = done(d, day, slot);
            String active = activeIdOfSlot(d, day, slot);
            String id = active != null ? active : offeredId(p, d, slot);
            CompoundTag t = QuestManager.questTag(p, d, id, def(id));
            t.putInt("tier", slot);
            t.putString("tierName", "Quotidienne " + (slot + 1));
            t.putInt("state", finished ? 2 : active != null ? 1 : 0);
            list.add(t);
        }
        return list;
    }

    /** Action de la pancarte sur la quotidienne d'un emplacement. */
    public static void boardAction(ServerPlayer p, int action, int slot) {
        PlayerData d = RpgPlayers.get(p);
        if (slot < 0 || slot >= SLOTS) return;
        purge(d);
        long day = QuestManager.today();
        String active = activeIdOfSlot(d, day, slot);
        switch (action) {
            case WeeklyQuests.ACT_ACCEPT -> accept(p, offeredId(p, d, slot));
            case WeeklyQuests.ACT_CLAIM -> { if (active != null) claim(p, active); }
            case WeeklyQuests.ACT_ABANDON -> { if (active != null) QuestManager.abandon(p, active); }
            default -> {
            }
        }
    }

    public static void accept(ServerPlayer p, String id) {
        PlayerData d = RpgPlayers.get(p);
        if (!FeatureFlags.allow(p, "quetes") || !p.isAlive() || !d.playerClass.isPlayable()) return;
        purge(d);
        int[] k = parse(id);
        if (k == null || k[0] != QuestManager.today() || done(d, k[0], k[1]) || activeSlot(d, k[0], k[1]) || !id.equals(offeredId(p, d, k[1]))) return;
        if (d.activeQuests.size() >= QuestManager.MAX_ACTIVE) {
            p.sendOverlayMessage(Component.literal("Journal de quêtes plein (" + QuestManager.MAX_ACTIVE + " quêtes maximum).").withColor(0xFF5050));
            return;
        }
        QuestDef q = def(id);
        d.activeQuests.put(id, new int[q.objectives.size()]);
        d.trackedQuest = id;
        d.questGuidanceMuted = false;
        Net.toPlayer(p, new Payloads.Notify(Payloads.Notify.QUEST, "QUÊTE DU JOUR", q.name, 0x60C0FF));
        p.level().playSound(null, p.blockPosition(), com.mmorpg.registry.ModSounds.QUEST_ACCEPT.get(), SoundSource.PLAYERS, 1f, 1f);
        GuidanceManager.refresh(p);
        d.dirty = true;
    }

    /** Remise depuis le journal : pas de PNJ, la quete du jour se rend d'un clic une fois les objectifs atteints. */
    public static void claim(ServerPlayer p, String id) {
        PlayerData d = RpgPlayers.get(p);
        if (!FeatureFlags.allow(p, "quetes") || !p.isAlive() || p.isSpectator()) return;
        purge(d);
        QuestDef q = def(id);
        if (q == null || !QuestManager.completable(p, d, id)) return;
        QuestManager.finish(p, d, id, q);
    }
}
