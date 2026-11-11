package com.mmorpg.server;

import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** Execution differee de taches cote serveur (effets de competences et de boss en plusieurs temps). */
public final class Scheduler {
    private record Task(ServerLevel level, long runAt, Runnable action) {
    }

    private static final List<Task> TASKS = new ArrayList<>();
    private static final List<Task> PENDING = new ArrayList<>();

    private Scheduler() {
    }

    public static void later(ServerLevel level, int delayTicks, Runnable action) {
        synchronized (PENDING) {
            PENDING.add(new Task(level, level.getGameTime() + testOffset + Math.max(0, delayTicks), action));
        }
    }

    // ---- controles : horloge simulee, pour derouler dans un seul tick serveur des attaques etalees sur plusieurs secondes
    private static long testOffset;

    /** Debut d'une simulation : les taches deja en attente sont mises de cote, seules les nouvelles seront deroulees. */
    public static void beginTest() {
        synchronized (PENDING) {
            TASKS.addAll(PENDING);
            PENDING.clear();
        }
        testOffset = 0;
    }

    /** Avance l'horloge simulee d'un tick et execute les taches dues ; une erreur remonte a l'appelant. */
    public static void stepTest(ServerLevel level) {
        testOffset++;
        List<Task> due = new ArrayList<>();
        synchronized (PENDING) {
            Iterator<Task> it = PENDING.iterator();
            while (it.hasNext()) {
                Task t = it.next();
                if (t.runAt <= level.getGameTime() + testOffset) {
                    it.remove();
                    due.add(t);
                }
            }
        }
        for (Task t : due) t.action.run();
    }

    /** Fin de la simulation : les taches restantes de la simulation sont abandonnees. */
    public static void endTest() {
        synchronized (PENDING) {
            PENDING.clear();
        }
        testOffset = 0;
    }

    public static void tick() {
        synchronized (PENDING) {
            TASKS.addAll(PENDING);
            PENDING.clear();
        }
        Iterator<Task> it = TASKS.iterator();
        while (it.hasNext()) {
            Task t = it.next();
            if (t.level.getGameTime() >= t.runAt) {
                it.remove();
                try {
                    t.action.run();
                } catch (Exception e) {
                    com.mmorpg.MMORPG.LOGGER.error("[MMORPG] Erreur dans une tâche différée", e);
                }
            }
        }
    }

    public static void clear() {
        synchronized (PENDING) {
            PENDING.clear();
        }
        TASKS.clear();
    }
}
