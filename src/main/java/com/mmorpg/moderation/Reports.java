package com.mmorpg.moderation;

import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Signalements de joueurs (file consultee dans le panneau de moderation). {@link #add} est le point d'entree de la commande /report.
 * Sauvegarde dans moderation/reports.json (300 derniers).
 */
public final class Reports {
    private Reports() {
    }

    public static final class Report {
        public int id;
        public long time;
        public String reporter = "", reporterId = "", target = "", targetId = "", reason = "";
        public boolean closed;
        public String closedBy = "";
    }

    private static List<Report> reports;
    private static int nextId = 1;

    private static synchronized List<Report> all() {
        if (reports == null) {
            reports = ModUtil.read("reports.json", new TypeToken<ArrayList<Report>>() {
            }.getType(), new ArrayList<Report>());
            for (Report r : reports) nextId = Math.max(nextId, r.id + 1);
        }
        return reports;
    }

    public static synchronized int add(UUID reporter, String reporterName, UUID target, String targetName, String reason) {
        Report r = new Report();
        r.id = nextId++;
        r.time = System.currentTimeMillis();
        r.reporter = reporterName;
        r.reporterId = reporter == null ? "" : reporter.toString();
        r.target = targetName;
        r.targetId = target == null ? "" : target.toString();
        r.reason = reason.length() > 200 ? reason.substring(0, 200) : reason;
        all().add(r);
        while (all().size() > 300) all().remove(0);
        ModUtil.write("reports.json", all());
        Journal.add(target, targetName, "report", 0, "signalé par " + reporterName + " : " + r.reason);
        return r.id;
    }

    public static synchronized List<Report> recent(boolean openOnly, int max) {
        List<Report> out = new ArrayList<>();
        List<Report> l = all();
        for (int i = l.size() - 1; i >= 0 && out.size() < max; i--) if (!openOnly || !l.get(i).closed) out.add(l.get(i));
        return out;
    }

    public static synchronized int openCount() {
        return (int) all().stream().filter(r -> !r.closed).count();
    }

    public static synchronized boolean close(int id, String by) {
        for (Report r : all()) {
            if (r.id == id && !r.closed) {
                r.closed = true;
                r.closedBy = by;
                ModUtil.write("reports.json", all());
                ModHistory.record(by, ModUtil.uuid(r.targetId), r.target, "report traité", "#" + id);
                return true;
            }
        }
        return false;
    }

    public static synchronized void reset() {
        reports = null;
    }
}
