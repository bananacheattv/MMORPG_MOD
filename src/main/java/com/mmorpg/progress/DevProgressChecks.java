package com.mmorpg.progress;

import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

/** Controles en jeu : talents, bestiaire, prestige. */
public final class DevProgressChecks {
    private DevProgressChecks() {
    }

    private static void check(boolean ok, String m) {
        if (!ok) throw new AssertionError("progress: " + m);
    }

    private static CompoundTag act(String a, String k, String v) {
        CompoundTag t = new CompoundTag();
        t.putString("action", a);
        if (!k.isEmpty()) t.putString(k, v);
        return t;
    }

    public static void run(ServerPlayer p) {
        var d = RpgPlayers.get(p);
        var old = d.save();
        try {
            d.playerClass = PlayerClass.GUERRIER;
            d.level = 40;
            d.bonusTalentPoints = 0;
            d.talents.clear();
            d.bestiaryClaimed.clear();
            RpgPlayers.recompute(p);
            double atk0 = d.stats.get(com.mmorpg.rpg.Stat.ATK);
            for (int i = 0; i < 6; i++) Progress.action(p, act("talent", "id", "puissance"));
            check(d.talents.get("puissance") == 5, "rang plafonne a 5");
            check(Progress.talentPointsFree(d) == 3, "points restants");
            check(d.stats.get(com.mmorpg.rpg.Stat.ATK) > atk0, "talent augmente l'attaque");
            Progress.action(p, act("talent", "id", "inconnu"));
            check(Progress.talentPointsSpent(d) == 5, "talent inconnu refuse");
            d.gold = 10;
            Progress.action(p, act("respec", "", ""));
            check(d.talents.size() == 1, "reinitialisation refusee sans or");
            d.gold = 5000;
            Progress.action(p, act("respec", "", ""));
            check(d.talents.isEmpty() && d.gold < 5000, "reinitialisation payee");

            d.kills.put("roi_gobelin", 12);
            long gold = d.gold;
            CompoundTag c = act("bestiary", "mob", "roi_gobelin");
            c.putInt("tier", 0);
            Progress.action(p, c);
            check(d.gold > gold, "palier 10 verse de l'or");
            gold = d.gold;
            Progress.action(p, c);
            check(d.gold == gold, "palier reclame une seule fois");
            c.putInt("tier", 1);
            Progress.action(p, c);
            check(d.gold == gold, "palier 50 non atteint");

            d.level = 99;
            Progress.action(p, act("prestige", "", ""));
            check(d.prestige == 0, "prestige refuse avant 100");
            d.level = 100;
            Progress.action(p, act("prestige", "", ""));
            check(d.prestige == 1 && d.level == 1 && d.mounts.contains("drake_ascendant"), "prestige applique + monture");
            check(Progress.talentPointsFree(d) >= 30, "points de talent du prestige");
            d.level = 100;
            Progress.action(p, act("prestige", "", ""));
            check(d.level == 100, "prestige une seule fois");
            var e = new com.mmorpg.rpg.PlayerData();
            e.load(d.save());
            check(e.prestige == 1 && e.bestiaryClaimed.contains("roi_gobelin:0"), "sauvegarde conserve prestige et bestiaire");
            com.mmorpg.MMORPG.LOGGER.info("[CODEX CHECKS] PROGRESS PASS");
        } finally {
            d.load(old);
            RpgPlayers.recomputeAndSync(p);
        }
    }
}
