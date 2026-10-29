package com.mmorpg.server;

import com.mmorpg.network.Net;
import com.mmorpg.network.Payloads;
import com.mmorpg.network.S2COpenScreen;
import com.mmorpg.quest.QuestDef;
import com.mmorpg.registry.ModBlocks;
import com.mmorpg.rpg.PlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.time.Duration;
import java.util.Random;

/**
 * Quetes hebdomadaires : chaque semaine (du lundi 00:00 au dimanche, heure du serveur) chaque joueur recoit trois quetes
 * tirees au hasard, une par difficulte (facile, difficile, tres difficile). Une quete est decrite par son identifiant
 * {@code weekly:<semaine>:<difficulte>:<modele>} : elle se reconstruit sans etat supplementaire, et sa progression et son
 * acceptation vivent dans les quetes actives du profil (comme les autres quetes). Les quetes terminees de la semaine sont
 * gardees dans {@code dailyQuests} ({@code weeklydone:<semaine>:<difficulte>}). Tout est verifie cote serveur.
 */
public final class WeeklyQuests {
    public static final String PREFIX = "weekly:";
    public static final String DONE_PREFIX = "weeklydone:";
    public static final int ACT_BROWSE = 0, ACT_ACCEPT = 1, ACT_CLAIM = 2, ACT_ABANDON = 3;
    public static final String[] TIERS = {"Facile", "Difficile", "Très difficile"};
    public static final int[] TIER_COLOR = {0xFF4CC05C, 0xFFF09A30, 0xFFE05050};

    private record Obj(String type, String target, int count) {
    }

    private record Template(String name, String description, Obj... objectives) {
    }

    private static Obj kill(String target, int n) {
        return new Obj("kill", target, n);
    }

    private static Obj collect(String item, int n) {
        return new Obj("collect", item, n);
    }

    private static final Template[][] TEMPLATES = {
            {
                    new Template("Chasseur de la semaine", "Éliminez 60 monstres, quels qu'ils soient.", kill("*", 60)),
                    new Template("Réserve de charbon", "Rapportez 48 charbons pour les forges d'Aeloria.", collect("minecraft:coal", 48)),
                    new Template("Fer pour les armuriers", "Rapportez 24 lingots de fer.", collect("minecraft:iron_ingot", 24)),
                    new Template("Patrouille et ravitaillement", "Éliminez 45 monstres et rapportez 24 charbons.", kill("*", 45), collect("minecraft:coal", 24)),
            },
            {
                    new Template("Grande battue", "Éliminez 200 monstres.", kill("*", 200)),
                    new Template("Chasseur de boss", "Vainquez 2 boss.", kill("boss", 2)),
                    new Template("Commande de la forge", "Rapportez 96 lingots de fer et 32 lingots d'or.", collect("minecraft:iron_ingot", 96), collect("minecraft:gold_ingot", 32)),
                    new Template("Brigade d'élite", "Éliminez 120 monstres et vainquez 1 boss.", kill("*", 120), kill("boss", 1)),
            },
            {
                    new Template("Fléau des ténèbres", "Éliminez 500 monstres.", kill("*", 500)),
                    new Template("Tueur de titans", "Vainquez 5 boss.", kill("boss", 5)),
                    new Template("Trésor de la couronne", "Rapportez 24 diamants.", collect("minecraft:diamond", 24)),
                    new Template("Campagne légendaire", "Éliminez 300 monstres et vainquez 3 boss.", kill("*", 300), kill("boss", 3)),
            },
    };

    private static final long[] GOLD = {150, 450, 1200};
    private static final double[] XP_PERCENT = {40, 110, 260};
    private static final int[] EMERALDS = {3, 10, 28};

    private WeeklyQuests() {
    }

    /** Index de la semaine courante (les semaines commencent le lundi). */
    public static long week() {
        return (LocalDate.now().toEpochDay() + 3) / 7;
    }

    /** Secondes avant la prochaine remise a zero (lundi 00:00). */
    public static long secondsUntilReset() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime next = now.toLocalDate().with(TemporalAdjusters.next(DayOfWeek.MONDAY)).atStartOfDay();
        return Math.max(1, Duration.between(now, next).getSeconds());
    }

    public static String id(long week, int tier, int template) {
        return PREFIX + week + ":" + tier + ":" + template;
    }

    /** Modele tire pour ce joueur : stable pour une semaine donnee, different d'un joueur a l'autre. */
    public static int pick(java.util.UUID player, long week, int tier) {
        long seed = player.getMostSignificantBits() * 31 + player.getLeastSignificantBits() * 17 + week * 1_000_003L + tier * 7919L;
        return new Random(seed).nextInt(TEMPLATES[tier].length);
    }

    public static String offeredId(ServerPlayer p, int tier) {
        long w = week();
        return id(w, tier, pick(p.getUUID(), w, tier));
    }

    private static int[] parse(String id) {
        if (!id.startsWith(PREFIX)) return null;
        String[] s = id.substring(PREFIX.length()).split(":");
        if (s.length != 3) return null;
        try {
            int tier = Integer.parseInt(s[1]), tpl = Integer.parseInt(s[2]);
            if (tier < 0 || tier >= TEMPLATES.length || tpl < 0 || tpl >= TEMPLATES[tier].length) return null;
            return new int[]{(int) Long.parseLong(s[0]), tier, tpl};
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static int tierOf(String id) {
        int[] k = parse(id);
        return k == null ? -1 : k[1];
    }

    public static long weekOf(String id) {
        int[] k = parse(id);
        return k == null ? -1 : k[0];
    }

    public static String doneKey(String id) {
        int[] k = parse(id);
        return DONE_PREFIX + (k == null ? "?" : k[0] + ":" + k[1]);
    }

    /** Definition d'une quete hebdomadaire d'apres son identifiant ; null si l'identifiant est invalide. */
    public static QuestDef def(String id) {
        int[] k = parse(id);
        if (k == null) return null;
        int tier = k[1];
        Template t = TEMPLATES[tier][k[2]];
        QuestDef q = new QuestDef();
        q.name = t.name();
        q.description = "Quête hebdomadaire (" + TIERS[tier].toLowerCase() + ") : " + t.description();
        q.weekly = true;
        q.minLevel = 1;
        for (Obj o : t.objectives()) q.objectives.add(new QuestDef.Objective(o.type(), o.target(), o.count()));
        q.rewards.gold = GOLD[tier];
        q.rewards.xpPercentOfLevel = XP_PERCENT[tier];
        q.rewards.items.add(new QuestDef.ItemReward("minecraft:emerald", EMERALDS[tier]));
        return q;
    }

    /** Fin de semaine : retire les quetes hebdomadaires perimees (actives ou terminees). */
    public static void purge(PlayerData d) {
        long w = week();
        boolean changed = d.activeQuests.keySet().removeIf(id -> id.startsWith(PREFIX) && weekOf(id) != w);
        d.questGivers.keySet().removeIf(id -> id.startsWith(PREFIX) && weekOf(id) != w);
        if (d.trackedQuest.startsWith(PREFIX) && weekOf(d.trackedQuest) != w) d.trackedQuest = "";
        changed |= d.dailyQuests.entrySet().removeIf(e -> e.getKey().startsWith(DONE_PREFIX) && e.getValue() != w);
        if (changed) d.dirty = true;
    }

    public static boolean done(PlayerData d, long week, int tier) {
        Long v = d.dailyQuests.get(DONE_PREFIX + week + ":" + tier);
        return v != null && v == week;
    }

    private static boolean near(ServerPlayer p, BlockPos pos) {
        return p.isAlive() && !p.isSpectator() && p.level().getBlockState(pos).is(ModBlocks.QUETES_HEBDO.get())
                && p.distanceToSqr(Vec3.atCenterOf(pos)) < 144;
    }

    /** Envoie les trois quetes de la semaine du joueur et leur etat. */
    public static void open(ServerPlayer p, BlockPos pos) {
        PlayerData d = RpgPlayers.get(p);
        if (!d.playerClass.isPlayable()) {
            RpgPlayers.openClassSelection(p);
            return;
        }
        purge(d);
        long w = week();
        CompoundTag t = new CompoundTag();
        t.putLong("pos", pos.asLong());
        t.putLong("week", w);
        t.putLong("reset", secondsUntilReset());
        ListTag tiers = new ListTag();
        for (int tier = 0; tier < TEMPLATES.length; tier++) {
            String id = id(w, tier, pick(p.getUUID(), w, tier));
            QuestDef q = def(id);
            CompoundTag e = QuestManager.questTag(p, d, id, q);
            e.putInt("tier", tier);
            e.putString("tierName", TIERS[tier]);
            boolean finished = done(d, w, tier);
            e.putInt("state", finished ? 2 : d.activeQuests.containsKey(id) ? 1 : 0);
            tiers.add(e);
        }
        t.put("tiers", tiers);
        t.putInt("doneCount", (int) java.util.stream.IntStream.range(0, TEMPLATES.length).filter(i -> done(d, w, i)).count());
        Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.WEEKLY, t));
    }

    public static void action(ServerPlayer p, BlockPos pos, int action, int tier) {
        if (!FeatureFlags.allow(p, "quetes") || !FeatureFlags.allow(p, "quetes_hebdo") || !near(p, pos)) return;
        if (tier < 0 || tier >= TEMPLATES.length) {
            if (action == ACT_BROWSE) open(p, pos);
            return;
        }
        PlayerData d = RpgPlayers.get(p);
        if (!d.playerClass.isPlayable()) return;
        purge(d);
        long w = week();
        String id = id(w, tier, pick(p.getUUID(), w, tier));
        QuestDef q = def(id);
        switch (action) {
            case ACT_ACCEPT -> {
                if (done(d, w, tier) || d.activeQuests.containsKey(id)) break;
                if (d.activeQuests.size() >= QuestManager.MAX_ACTIVE) {
                    p.sendOverlayMessage(Component.literal("Journal de quêtes plein (" + QuestManager.MAX_ACTIVE + " quêtes maximum).").withColor(0xFF5050));
                    break;
                }
                d.activeQuests.put(id, new int[q.objectives.size()]);
                d.trackedQuest = id;
                d.questGuidanceMuted = false;
                var board = new CompoundTag();
                board.putString("backend", com.mmorpg.network.NetworkConfig.current.role.name());
                board.putString("id", "");
                board.putString("name", "Pancarte hebdomadaire");
                board.putString("dimension", p.level().dimension().identifier().toString());
                board.putLong("pos", pos.asLong());
                d.questGivers.put(id, board);
                Net.toPlayer(p, new Payloads.Notify(Payloads.Notify.QUEST, "QUÊTE HEBDOMADAIRE", q.name + " — " + TIERS[tier], TIER_COLOR[tier] & 0xFFFFFF));
                p.level().playSound(null, p.blockPosition(), com.mmorpg.registry.ModSounds.QUEST_ACCEPT.get(), SoundSource.PLAYERS, 1f, 1f);
                d.dirty = true;
            }
            case ACT_CLAIM -> {
                if (done(d, w, tier) || !QuestManager.completable(p, d, id)) break;
                QuestManager.finish(p, d, id, q);
            }
            case ACT_ABANDON -> {
                if (d.activeQuests.remove(id) != null) {
                    d.questGivers.remove(id);
                    if (id.equals(d.trackedQuest)) d.trackedQuest = "";
                    d.dirty = true;
                }
            }
            default -> {
            }
        }
        open(p, pos);
    }
}
