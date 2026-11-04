package com.mmorpg.moderation;

import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Historique des sanctions (avertissement, silence, expulsion, bannissement, prison...) : fichier moderation/historique.json, 500 entrees. */
public final class ModHistory {
    private ModHistory() {
    }

    public static final class Entry {
        public long time;
        public String target = "", targetId = "", by = "", kind = "", detail = "";
    }

    private static List<Entry> entries;

    private static synchronized List<Entry> all() {
        if (entries == null) entries = ModUtil.read("historique.json", new TypeToken<ArrayList<Entry>>() {
        }.getType(), new ArrayList<Entry>());
        return entries;
    }

    public static synchronized void record(String by, UUID target, String targetName, String kind, String detail) {
        Entry e = new Entry();
        e.time = System.currentTimeMillis();
        e.by = by;
        e.target = targetName == null ? "?" : targetName;
        e.targetId = target == null ? "" : target.toString();
        e.kind = kind;
        e.detail = detail == null ? "" : (detail.length() > 200 ? detail.substring(0, 200) : detail);
        List<Entry> l = all();
        l.add(e);
        while (l.size() > 500) l.remove(0);
        ModUtil.write("historique.json", l);
        Journal.add(target, targetName, "sanction", 0, kind + " par " + by + (e.detail.isEmpty() ? "" : " : " + e.detail));
    }

    /** Les {@code max} dernieres entrees (les plus recentes d'abord), filtrees sur un joueur si {@code id} n'est pas vide. */
    public static synchronized List<Entry> recent(String id, int max) {
        List<Entry> out = new ArrayList<>();
        List<Entry> l = all();
        for (int i = l.size() - 1; i >= 0 && out.size() < max; i--) {
            if (id == null || id.isEmpty() || id.equals(l.get(i).targetId)) out.add(l.get(i));
        }
        return out;
    }

    public static synchronized void reset() {
        entries = null;
    }
}
