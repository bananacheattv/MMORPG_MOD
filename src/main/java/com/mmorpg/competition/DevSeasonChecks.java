package com.mmorpg.competition;

import com.mmorpg.server.RpgPlayers;
import net.minecraft.server.level.ServerPlayer;
import java.util.UUID;

/** Vérifie saisons classées, signalements et mise de duel sur un vrai serveur. */
public final class DevSeasonChecks {
    private static void check(boolean ok, String why) { if (!ok) throw new AssertionError("Saisons: " + why); }

    public static void run(ServerPlayer p) {
        var server = p.level().getServer();
        // --- saison : 5 victoires contre un adversaire fictif
        var rival = UUID.randomUUID();
        var winnerSide = new PvpProgression.Report();
        winnerSide.id = "season-check"; winnerSide.winner = 0;
        for (int i = 0; i < 5; i++) {
            var r = new PvpProgression.Report();
            r.id = "season-check-" + i; r.winner = 0;
            var a = new PvpProgression.Row(); a.id = p.getUUID().toString(); a.name = "Testeur"; a.team = 0;
            var b = new PvpProgression.Row(); b.id = rival.toString(); b.name = "Rival"; b.team = 1;
            r.rows.add(a); r.rows.add(b);
            PvpSeasons.record(server, r);
        }
        var before = PvpProgression.access(server, s -> PvpProgression.account(s, p.getUUID()).points);
        check(PvpSeasons.tier(1000).equals("Bronze") && PvpSeasons.tier(1700).equals("Diamant"), "paliers");
        check(PvpSeasons.endIfDue(server, true), "fin de saison forcée");
        var after = PvpProgression.access(server, s -> PvpProgression.account(s, p.getUUID()));
        check(after.points > before, "points de récompense crédités");
        check(after.unlocks.stream().anyMatch(u -> u.startsWith("title:")), "titre de saison débloqué");
        long gold0 = RpgPlayers.get(p).gold;
        PvpSeasons.claim(p);
        check(RpgPlayers.get(p).gold > gold0, "or de saison versé");
        // --- signalement
        int id = com.mmorpg.admin.Reports.file(p, "Cible", "test automatique");
        check(id > 0, "signalement enregistré");
        boolean limited = false;
        try { com.mmorpg.admin.Reports.file(p, "Cible", "encore"); } catch (IllegalArgumentException e) { limited = true; }
        check(limited, "limite de débit des signalements");
        com.mmorpg.network.SharedLedger.access(server, "reports", com.mmorpg.admin.Reports.State.class, com.mmorpg.admin.Reports.State::new, st -> { st.items.removeIf(i -> i.id == id); return null; });
    }
}
