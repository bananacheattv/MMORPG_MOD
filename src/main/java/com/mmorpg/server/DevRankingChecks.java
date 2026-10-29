package com.mmorpg.server;

import com.mmorpg.leaderboard.Board;
import com.mmorpg.leaderboard.Leaderboards;
import com.mmorpg.ranking.RankingKind;
import com.mmorpg.ranking.Rankings;
import com.mmorpg.registry.ModBlocks;
import com.mmorpg.rpg.PlayerClass;
import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;

/** Verifications serveur des classements : ecriture, tri, textes, panneaux (copie locale ; MariaDB si database.json l'active). */
public final class DevRankingChecks {
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError("classements: " + message);
    }

    public static void run(ServerPlayer p) {
        var server = p.level().getServer();
        var d = RpgPlayers.get(p);
        var oldClass = d.playerClass;
        int oldLevel = d.level;
        long oldGold = d.gold;
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        var place = p.blockPosition().offset(0, 0, 3);
        var previous = p.level().getBlockState(place);
        try {
            d.playerClass = PlayerClass.MAGE;
            d.level = 77;
            d.gold = 4242;
            Leaderboards.sample(server, p);
            Leaderboards.add(server, a, "TestAlpha", Board.ARENE, 2, 5);
            Leaderboards.add(server, b, "TestBeta", Board.ARENE, 2, 9);
            Leaderboards.add(server, a, "TestAlpha", Board.BOSS, 3, 0);
            Leaderboards.add(server, b, "TestBeta", Board.DONJONS, 1, 0);
            Leaderboards.refreshAndWait(server);
            var level = Rankings.top(RankingKind.NIVEAU);
            check(level.stream().anyMatch(e -> e.id().equals(p.getUUID().toString()) && e.value() == 77 && e.text().equals("Niv. 77")), "niveau du joueur releve");
            var gold = Rankings.top(RankingKind.FORTUNE);
            check(gold.stream().anyMatch(e -> e.id().equals(p.getUUID().toString()) && e.text().equals("4 242 or")), "fortune formatee");
            var arena = Rankings.top(RankingKind.ARENE);
            int ia = -1, ib = -1;
            for (int i = 0; i < arena.size(); i++) {
                if (arena.get(i).id().equals(a.toString())) ia = i;
                if (arena.get(i).id().equals(b.toString())) ib = i;
            }
            check(ia >= 0 && ib >= 0 && ib < ia, "arene : a victoires egales, plus d'eliminations devant");
            check(arena.get(ib).text().equals("2 V · 9 frags"), "arene : texte " + arena.get(ib).text());
            check(Rankings.top(RankingKind.BOSS).stream().anyMatch(e -> e.id().equals(a.toString()) && e.text().equals("3 boss")), "boss");
            check(Rankings.top(RankingKind.DONJONS).stream().anyMatch(e -> e.id().equals(b.toString()) && e.text().equals("1 victoire")), "donjons");
            if (com.mmorpg.db.Database.enabled()) check(com.mmorpg.db.Database.ready(), "base prete");
            for (var kind : RankingKind.values()) {
                p.level().setBlockAndUpdate(place, ModBlocks.RANKING_BOARDS.get(kind).get().defaultBlockState());
                var be = p.level().getBlockEntity(place);
                check(be instanceof com.mmorpg.block.ranking.RankingBoardBlockEntity, "bloc-entite du panneau " + kind.id);
                var expected = Rankings.lines(kind);
                if (!kind.guild) check(!expected.isEmpty() || kind == RankingKind.QUETES || kind == RankingKind.MONSTRES, "classement " + kind.id + " non vide");
            }
        } finally {
            p.level().setBlockAndUpdate(place, previous.isAir() ? net.minecraft.world.level.block.Blocks.AIR.defaultBlockState() : previous);
            d.playerClass = oldClass;
            d.level = oldLevel;
            d.gold = oldGold;
        }
    }
}
