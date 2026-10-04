package com.mmorpg.rpg;

/** Instance d'un effet temporaire actif sur un joueur. */
public final class Buff {
    public final BuffType type;
    public final double magnitude;
    public final double magnitude2;
    public long expiresAt;
    public final int totalTicks;

    public Buff(BuffType type, double magnitude, double magnitude2, long expiresAt, int totalTicks) {
        this.type = type;
        this.magnitude = magnitude;
        this.magnitude2 = magnitude2;
        this.expiresAt = expiresAt;
        this.totalTicks = totalTicks;
    }

    public int remaining(long now) {
        return (int) Math.max(0, expiresAt - now);
    }
}
