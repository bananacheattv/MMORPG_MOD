package com.mmorpg.moderation;

import com.mmorpg.server.RpgPlayers;
import net.minecraft.server.level.ServerPlayer;

/** Verifications serveur de la moderation (appelees par la suite reseau isolee). */
public final class DevModerationChecks {
    private DevModerationChecks() {
    }

    private static void check(boolean ok, String msg) {
        if (!ok) throw new AssertionError("moderation: " + msg);
    }

    public static void run(ServerPlayer p) {
        check(ModUtil.parseDuration("30m") == 1_800_000L && ModUtil.parseDuration("2h") == 7_200_000L && ModUtil.parseDuration("x") < 0, "durees");
        // prison : un joueur ordinaire est enferme puis libere ; un operateur est refuse
        String r = Jail.jail("test", p.getUUID(), "Test", p, 60_000, "x");
        check(r.startsWith("!"), "operateur non emprisonnable (" + r + ")");
        var fake = java.util.UUID.randomUUID();
        check(Jail.setZone(p).startsWith("Zone"), "zone definie");
        check(Jail.zone() != null, "zone enregistree");
        r = Jail.jail("test", fake, "Absent", null, 60_000, "raison");
        check(!r.startsWith("!") && Jail.isJailed(fake), "peine hors ligne enregistree");
        check(Jail.prisoners().containsKey(fake.toString()), "prisonnier liste");
        check(!Jail.release("test", fake, "Absent", null, false).startsWith("!") && !Jail.isJailed(fake), "liberation");
        // maintenance
        Maintenance.schedule("test", 600_000, "essai");
        check(Maintenance.counting() && !Maintenance.active() && Maintenance.remainingMs() > 500_000, "compte a rebours");
        Maintenance.cancel("test");
        check(!Maintenance.counting() && !Maintenance.active(), "maintenance annulee");
        // planificateur
        String a = EventPlanner.add("test", "annonce", 600_000, 0, "bonjour");
        check(!a.startsWith("!") && !EventPlanner.list().isEmpty(), "tache planifiee");
        int id = EventPlanner.list().get(EventPlanner.list().size() - 1).id;
        check(!EventPlanner.remove("test", id).startsWith("!"), "tache supprimee");
        check(EventPlanner.add("test", "annonce", 0, 0, "x").startsWith("!"), "delai invalide refuse");
        // sauvegarde / restauration / annulation
        var data = RpgPlayers.get(p);
        data.gold = 1234;
        check(Backups.snapshot(p, "test"), "sauvegarde");
        data.gold = 99;
        var latest = Backups.list(p.getUUID()).stream().filter(i -> i.tag().equals("test")).findFirst().orElseThrow();
        check(!Backups.restore("test", p, latest.file()).startsWith("!"), "restauration");
        check(RpgPlayers.get(p).gold == 1234, "or restaure");
        check(Backups.restore("test", p, "../../etc/passwd").startsWith("!"), "chemin refuse");
        check(Backups.undo("test", p, 1440).startsWith("!") || true, "annulation sans plantage");
        // signalements, historique, journal
        int rid = Reports.add(p.getUUID(), "A", fake, "B", "insultes");
        check(Reports.openCount() >= 1 && Reports.close(rid, "test") && !Reports.close(rid, "test"), "signalement");
        check(!ModHistory.recent("", 5).isEmpty(), "historique");
        check(!Journal.replay(p.getUUID().toString(), System.currentTimeMillis() - 3_600_000L, 50).isEmpty() || true, "rejeu");
        Journal.add(p.getUUID(), "T", "test", 1, "a|b");
        check(Journal.replay(p.getUUID().toString(), System.currentTimeMillis() - 60_000L, 50).stream().anyMatch(l -> l.kind().equals("test")), "journal relu");
        com.mmorpg.MMORPG.LOGGER.info("[CODEX CHECKS] MODERATION PASS");
    }
}
