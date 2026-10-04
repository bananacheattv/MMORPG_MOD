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
            PENDING.add(new Task(level, level.getGameTime() + Math.max(0, delayTicks), action));
        }
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
