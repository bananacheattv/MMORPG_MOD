package com.mmorpg.fishing;

import com.mmorpg.rpg.Rarity;

import java.util.Random;

/**
 * Mini-jeu de la touche : une aiguille fait des allers-retours sur une jauge ; le joueur fait clic droit quand elle est dans la zone
 * coloree. Plus la prise est rare, plus il y a de manches, plus la zone est petite et plus l'aiguille est rapide.
 * La logique est deterministe (graine + tick) : le client dessine la meme aiguille que celle que le serveur juge.
 * Classe sans dependance Minecraft.
 */
public final class FishFight {
    /** Delai maximal pour reagir pendant une manche (ticks). */
    public static final int ROUND_TIMEOUT = 140;
    /** Pause entre deux manches (ticks). */
    public static final int ROUND_GAP = 14;
    /** Marge de tolerance (fraction de jauge) pour la latence reseau. */
    public static final double LATENCY_SLACK = 0.04;
    /** Compensation maximale de latence (ticks). */
    public static final int MAX_LAG_TICKS = 6;
    /** Part de la zone qui compte comme « parfaite ». */
    public static final double PERFECT_FRACTION = 0.40;

    public enum Press { MISS, HIT, PERFECT }

    public static int rounds(Rarity r) {
        return switch (r) {
            case COMMUN, PEU_COMMUN -> 1;
            case RARE, EPIQUE -> 2;
            case LEGENDAIRE, MYTHIQUE -> 3;
        };
    }

    /** Demi-largeur de la zone (fraction de la jauge) avant bonus de canne et de metier. */
    public static double baseHalfWidth(Rarity r) {
        return switch (r) {
            case COMMUN -> 0.20;
            case PEU_COMMUN -> 0.17;
            case RARE -> 0.14;
            case EPIQUE -> 0.115;
            case LEGENDAIRE -> 0.095;
            case MYTHIQUE -> 0.08;
        };
    }

    /** Duree (ticks) d'un aller-retour complet de l'aiguille. */
    public static int period(Rarity r) {
        return switch (r) {
            case COMMUN -> 44;
            case PEU_COMMUN -> 38;
            case RARE -> 34;
            case EPIQUE -> 30;
            case LEGENDAIRE -> 26;
            case MYTHIQUE -> 22;
        };
    }

    /** Erreurs tolerees : une seule, a partir de deux manches et d'une canne de rang 3 ; deux pour la canne du Léviathan. */
    public static int allowedMisses(int rounds, int rodTier) {
        if (rounds < 2) return 0;
        if (rodTier >= 6 && rounds >= 3) return 2;
        return rodTier >= 3 ? 1 : 0;
    }

    /** Position de l'aiguille (0 a 1) {@code t} ticks apres le debut de la manche : onde triangulaire. */
    public static double cursor(long t, int period, int phase) {
        double p = (((t + phase) % period) + period) % period / (double) period;
        return p < 0.5 ? 2 * p : 2 - 2 * p;
    }

    /** Centre de la zone d'une manche, derive de la graine : entre 0,2 et 0,8 pour rester lisible. */
    public static double center(long seed, int round) {
        return 0.2 + 0.6 * new Random(seed * 31L + round * 7919L).nextDouble();
    }

    public static int phase(long seed, int round, int period) {
        return new Random(seed * 17L + round * 104729L).nextInt(period);
    }

    // ------------------------------------------------------------------ session (cote serveur)

    public final Rarity rarity;
    public final int rodTier;
    public final long seed;
    public final int rounds;
    public final int period;
    public final double halfWidth;
    public final int allowedMisses;

    private int round;
    private long roundStart;
    private int hits, misses;
    private boolean perfectOnly = true;
    private boolean finished;

    public FishFight(Rarity rarity, int rodTier, double zoneMultiplier, long seed, long now) {
        this.rarity = rarity;
        this.rodTier = rodTier;
        this.seed = seed;
        this.rounds = rounds(rarity);
        this.period = period(rarity);
        this.halfWidth = Math.min(0.30, baseHalfWidth(rarity) * zoneMultiplier);
        this.allowedMisses = allowedMisses(rounds, rodTier);
        this.round = 0;
        this.roundStart = now + ROUND_GAP;
    }

    public int round() {
        return round;
    }

    public long roundStart() {
        return roundStart;
    }

    public int hits() {
        return hits;
    }

    public int misses() {
        return misses;
    }

    public boolean finished() {
        return finished;
    }

    /** Vrai si la prise est ramenee (assez de manches reussies). */
    public boolean success() {
        return finished && misses <= allowedMisses;
    }

    /** Vrai si toutes les manches reussies etaient parfaites (prise plus grosse, experience en plus). */
    public boolean perfect() {
        return success() && perfectOnly && hits == rounds;
    }

    public double currentCenter() {
        return center(seed, round);
    }

    public int currentPhase() {
        return phase(seed, round, period);
    }

    /** Vrai tant que la manche est visible et jouable (apres la courte pause entre deux manches). */
    public boolean live(long now) {
        return !finished && now >= roundStart;
    }

    /** Resultat d'un clic du joueur au tick {@code now}. Pendant la pause entre manches, le clic est ignore (renvoie {@code null}). */
    public Press press(long now) {
        return press(now, 0);
    }

    /**
     * Comme {@link #press(long)}, avec compensation de latence : le joueur voit l'aiguille en retard de l'aller-retour reseau, donc le serveur
     * la juge {@code lagTicks} ticks plus tot (plafonne a {@link #MAX_LAG_TICKS}).
     */
    public Press press(long now, int lagTicks) {
        if (finished || now < roundStart) return null;
        long t = Math.max(0, now - Math.clamp(lagTicks, 0, MAX_LAG_TICKS) - roundStart);
        double pos = cursor(t, period, currentPhase());
        double d = Math.abs(pos - currentCenter());
        Press result;
        if (d <= halfWidth * PERFECT_FRACTION) result = Press.PERFECT;
        else if (d <= halfWidth + LATENCY_SLACK) result = Press.HIT;
        else result = Press.MISS;
        record(result, now);
        return result;
    }

    /** Fait passer le temps : une manche sans clic apres le delai est ratee. Renvoie vrai si une manche vient d'etre ratee. */
    public boolean tick(long now) {
        if (finished || now < roundStart + ROUND_TIMEOUT) return false;
        record(Press.MISS, now);
        return true;
    }

    private void record(Press p, long now) {
        if (p == Press.MISS) {
            misses++;
            perfectOnly = false;
        } else {
            hits++;
            if (p != Press.PERFECT) perfectOnly = false;
        }
        round++;
        if (misses > allowedMisses || round >= rounds) {
            finished = true;
        } else {
            roundStart = now + ROUND_GAP;
        }
    }
}
