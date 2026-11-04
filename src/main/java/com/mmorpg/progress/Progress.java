package com.mmorpg.progress;

import com.mmorpg.config.ConfigManager;
import com.mmorpg.config.MobConfig;
import com.mmorpg.rpg.LevelSystem;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.rpg.Stat;
import com.mmorpg.rpg.StatBlock;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Arrays;
import java.util.List;

/** Prestige (niveau 100, une seule fois), arbre de talents secondaire et recompenses du bestiaire. */
public final class Progress {
    private Progress() {
    }

    public static final int MAX_TALENT_RANK = 5;
    public static final int[] BESTIARY_TIERS = {10, 50, 100};

    /** Talent : branche, id, nom, description d'un rang, statistique, valeur par rang, en pourcentage ou non. */
    public record Talent(String branch, String id, String name, String desc, Stat stat, double perRank, boolean pct) {
    }

    public static final List<Talent> TALENTS = List.of(
            new Talent("Combat", "puissance", "Puissance", "+1 % attaque", Stat.ATK, .01, true),
            new Talent("Combat", "magie", "Savoir arcanique", "+1 % puissance magique", Stat.MAG, .01, true),
            new Talent("Combat", "precision", "Précision", "+1 % critique", Stat.CRIT, 1, false),
            new Talent("Combat", "fureur", "Fureur", "+3 % dégâts critiques", Stat.CRIT_DMG, 3, false),
            new Talent("Survie", "robustesse", "Robustesse", "+1 % PV", Stat.MAX_HP, .01, true),
            new Talent("Survie", "armure", "Cuirasse", "+1 % défense", Stat.DEF, .01, true),
            new Talent("Survie", "regeneration", "Régénération", "+0,3 PV/s", Stat.HP_REGEN, .3, false),
            new Talent("Survie", "esquive", "Réflexes", "+0,5 % esquive", Stat.ESQ, .5, false),
            new Talent("Maîtrise", "sagesse", "Sagesse", "+1 % mana", Stat.MAX_MANA, .01, true),
            new Talent("Maîtrise", "concentration", "Concentration", "+0,5 % réduction de recharge", Stat.CDR, .5, false),
            new Talent("Maîtrise", "vampirisme", "Vampirisme", "+0,3 % vol de vie", Stat.LIFESTEAL, .3, false),
            new Talent("Maîtrise", "foulee", "Foulée", "+0,5 % vitesse", Stat.SPEED, .5, false));

    public static Talent talent(String id) {
        for (Talent t : TALENTS) if (t.id.equals(id)) return t;
        return null;
    }

    /** Un point de talent tous les 5 niveaux, plus les points bonus (prestige, bestiaire). */
    public static int talentPointsTotal(PlayerData d) {
        return d.level / 5 + d.bonusTalentPoints;
    }

    public static int talentPointsSpent(PlayerData d) {
        int n = 0;
        for (int r : d.talents.values()) n += Math.max(0, r);
        return n;
    }

    public static int talentPointsFree(PlayerData d) {
        return Math.max(0, talentPointsTotal(d) - talentPointsSpent(d));
    }

    public static long respecCost(PlayerData d) {
        return 200L + 50L * talentPointsSpent(d);
    }

    /** Applique les talents et le prestige aux statistiques (appele par StatCalculator). */
    public static void applyStats(PlayerData d, StatBlock flat, StatBlock pct) {
        for (Talent t : TALENTS) {
            int r = d.talents.getOrDefault(t.id, 0);
            if (r <= 0) continue;
            (t.pct ? pct : flat).add(t.stat, t.perRank * r);
        }
        if (d.prestige > 0) {
            for (Stat s : Stat.PRIMARIES) flat.add(s, 10);
            pct.add(Stat.MAX_HP, .10);
            pct.add(Stat.ATK, .10);
            pct.add(Stat.MAG, .10);
            pct.add(Stat.DEF, .10);
        }
    }

    // ------------------------------------------------------------------ bestiaire

    public static long bestiaryGold(String mob, int tier) {
        MobConfig c = ConfigManager.mobs().get(mob);
        int lvl = c == null ? 1 : Math.max(1, c.maxLevel);
        long base = Math.max(10, lvl * 5L) * (c != null && c.boss ? 4 : 1);
        return base * new long[]{1, 5, 15}[Math.clamp(tier, 0, 2)];
    }

    public static boolean bestiaryReady(PlayerData d, String mob, int tier) {
        return tier >= 0 && tier < BESTIARY_TIERS.length
                && d.kills.getOrDefault(mob, 0) >= BESTIARY_TIERS[tier]
                && !d.bestiaryClaimed.contains(mob + ":" + tier);
    }

    // ------------------------------------------------------------------ actions

    public static void action(ServerPlayer p, CompoundTag t) {
        PlayerData d = RpgPlayers.get(p);
        if (!d.playerClass.isPlayable()) return;
        switch (t.getStringOr("action", "")) {
            case "talent" -> {
                Talent tal = talent(t.getStringOr("id", ""));
                if (tal == null || talentPointsFree(d) <= 0) return;
                int r = d.talents.getOrDefault(tal.id, 0);
                if (r >= MAX_TALENT_RANK) return;
                d.talents.put(tal.id, r + 1);
                RpgPlayers.recomputeAndSync(p);
            }
            case "respec" -> {
                long cost = respecCost(d);
                if (d.talents.isEmpty() || d.gold < cost) {
                    p.sendSystemMessage(Component.literal("Réinitialisation impossible (" + cost + " pièces d'or requises).").withColor(0xFF6060));
                    return;
                }
                d.gold -= cost;
                d.talents.clear();
                RpgPlayers.recomputeAndSync(p);
            }
            case "prestige" -> prestige(p, d);
            case "bestiary" -> {
                String mob = t.getStringOr("mob", "");
                int tier = t.getIntOr("tier", -1);
                if (!bestiaryReady(d, mob, tier)) return;
                d.bestiaryClaimed.add(mob + ":" + tier);
                long gold = bestiaryGold(mob, tier);
                d.gold += gold;
                String extra = "";
                if (tier == 2) {
                    d.bonusTalentPoints++;
                    extra = " et 1 point de talent";
                }
                p.sendSystemMessage(Component.literal("Bestiaire : +" + gold + " pièces d'or" + extra + ".").withColor(0xFFD040));
                RpgPlayers.recomputeAndSync(p);
            }
            default -> {
            }
        }
    }

    private static void prestige(ServerPlayer p, PlayerData d) {
        if (d.prestige > 0 || d.level < LevelSystem.MAX_LEVEL) return;
        d.prestige = 1;
        d.bonusTalentPoints += 30;
        d.mounts.add(com.mmorpg.mount.MountType.DRAKE_ASCENDANT.id);
        RpgPlayers.setLevel(p, 1);
        d.xp = 0;
        d.evolution = 0;
        d.skillRanks.clear();
        Arrays.fill(d.skillBar, "");
        d.resetAttributes();
        d.unlockSkills();
        d.dirty = true;
        p.level().getServer().getPlayerList().broadcastSystemMessage(Component.literal("✦ ").withColor(0xFFD040)
                .append(p.getDisplayName()).append(Component.literal(" a atteint le Prestige !").withColor(0xE0D8C0)), false);
        RpgPlayers.recomputeAndSync(p);
    }
}
