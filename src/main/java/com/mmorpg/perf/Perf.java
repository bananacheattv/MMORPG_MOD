package com.mmorpg.perf;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/** Compteurs et chronos legers du serveur, exposes par /perf. Ne fait aucune entree-sortie. */
public final class Perf {
    /** Duree moyenne lissee (ms) et nombre d'appels d'une section chronometree. */
    public static final class Section {
        public volatile double avgMs;
        public volatile double maxMs;
        public final AtomicLong calls = new AtomicLong();
    }

    public static final AtomicLong PACKETS_QUEUED = new AtomicLong();       // paquets passes par le regroupeur
    public static final AtomicLong PACKETS_COALESCED = new AtomicLong();    // paquets d'etat supprimes (remplaces dans le meme tick)
    public static final AtomicLong BUNDLES_SENT = new AtomicLong();         // envois groupes (1 envoi pour plusieurs paquets)
    public static final AtomicLong ORPHANS_REMOVED = new AtomicLong();
    public static final AtomicLong ASYNC_SAVES = new AtomicLong();
    public static final AtomicLong ASYNC_SAVES_SKIPPED = new AtomicLong();  // ecritures remplacees par une plus recente
    private static final Map<String, Section> SECTIONS = new ConcurrentHashMap<>();

    private Perf() {
    }

    public static Map<String, Section> sections() {
        return SECTIONS;
    }

    public static void record(String name, long nanos) {
        Section s = SECTIONS.computeIfAbsent(name, k -> new Section());
        double ms = nanos / 1_000_000.0;
        s.avgMs = s.calls.getAndIncrement() == 0 ? ms : s.avgMs * 0.9 + ms * 0.1;
        if (ms > s.maxMs) s.maxMs = ms;
    }

    /** Chronometre {@code r} et l'enregistre sous {@code name}. */
    public static void time(String name, Runnable r) {
        long t = System.nanoTime();
        try {
            r.run();
        } finally {
            record(name, System.nanoTime() - t);
        }
    }

    public static void reset() {
        SECTIONS.clear();
        PACKETS_QUEUED.set(0);
        PACKETS_COALESCED.set(0);
        BUNDLES_SENT.set(0);
    }
}
