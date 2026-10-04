package com.mmorpg.client.hud;

import com.mmorpg.network.Payloads;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/** Etat ephemere du HUD : textes de combat flottants et bannieres d'annonce. */
public final class HudEffects {
    public static final long TEXT_LIFE_MS = 1100;
    public static final long BANNER_LIFE_MS = 3200;

    public static final class FloatingText {
        public final double x, y, z;
        public final float amount;
        public final int kind;
        public final long born;
        public final double jitterX;

        FloatingText(double x, double y, double z, float amount, int kind, long born, double jitterX) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.amount = amount;
            this.kind = kind;
            this.born = born;
            this.jitterX = jitterX;
        }
    }

    public record Banner(int kind, String title, String subtitle, int color, long start) {
    }

    private static final List<FloatingText> TEXTS = new ArrayList<>();
    private static final Deque<Banner> BANNERS = new ArrayDeque<>();
    private static Banner current;

    private HudEffects() {
    }

    public static synchronized void addText(Payloads.CombatText p) {
        if (TEXTS.size() > 80) TEXTS.remove(0);
        double jitter = (Math.random() - 0.5) * 0.6;
        TEXTS.add(new FloatingText(p.x(), p.y(), p.z(), p.amount(), p.kind(), System.currentTimeMillis(), jitter));
    }

    public static synchronized List<FloatingText> texts() {
        long now = System.currentTimeMillis();
        Iterator<FloatingText> it = TEXTS.iterator();
        while (it.hasNext()) {
            if (now - it.next().born > TEXT_LIFE_MS) it.remove();
        }
        return new ArrayList<>(TEXTS);
    }

    public static synchronized void addBanner(Payloads.Notify n) {
        if (BANNERS.size() > 5) BANNERS.pollFirst();
        BANNERS.addLast(new Banner(n.kind(), n.title(), n.subtitle(), n.color(), 0));
    }

    /** Banniere en cours d'affichage (ou nulle). */
    public static synchronized Banner banner() {
        long now = System.currentTimeMillis();
        // plus courte si d'autres annonces attendent
        long life = BANNERS.isEmpty() ? BANNER_LIFE_MS : 1600;
        if (current != null && now - current.start() > life) current = null;
        if (current == null && !BANNERS.isEmpty()) {
            Banner b = BANNERS.pollFirst();
            current = new Banner(b.kind(), b.title(), b.subtitle(), b.color(), now);
        }
        return current;
    }

    public static synchronized void clear() {
        TEXTS.clear();
        BANNERS.clear();
        current = null;
    }
}
