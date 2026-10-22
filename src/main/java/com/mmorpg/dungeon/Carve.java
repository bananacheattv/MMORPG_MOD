package com.mmorpg.dungeon;

import com.mmorpg.hub.HubCtx;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Volume interieur d'un donjon decrit par des formes implicites (cylindres, ellipsoides, voutes en berceau, boites) :
 * on additionne les formes, puis {@link #shell} creuse l'interieur et habille automatiquement sols, murs et plafonds
 * (une coque de deux blocs), ce qui donne des salles rondes, des coupoles et des couloirs voutes sans le moindre angle droit force.
 */
final class Carve {
    enum Kind { FLOOR, WALL, CEILING }

    /** Choisit le bloc d'une cellule de la coque ; {@code back} = couche exterieure (soutien). */
    interface ShellPick {
        BlockState at(int x, int y, int z, Kind kind, boolean back);
    }

    final int x0, x1, y0, y1, z0, z1;
    private final int sx, sy, sz;
    private final boolean[] in;

    Carve(int x0, int x1, int y0, int y1, int z0, int z1) {
        this.x0 = x0;
        this.x1 = x1;
        this.y0 = y0;
        this.y1 = y1;
        this.z0 = z0;
        this.z1 = z1;
        this.sx = x1 - x0 + 1;
        this.sy = y1 - y0 + 1;
        this.sz = z1 - z0 + 1;
        this.in = new boolean[sx * sy * sz];
    }

    private int idx(int x, int y, int z) {
        return ((x - x0) * sy + (y - y0)) * sz + (z - z0);
    }

    private boolean inBounds(int x, int y, int z) {
        return x >= x0 && x <= x1 && y >= y0 && y <= y1 && z >= z0 && z <= z1;
    }

    boolean inside(int x, int y, int z) {
        return inBounds(x, y, z) && in[idx(x, y, z)];
    }

    private void mark(int x, int y, int z) {
        if (inBounds(x, y, z)) in[idx(x, y, z)] = true;
    }

    // ------------------------------------------------------------------ formes

    void box(int ax, int ay, int az, int bx, int by, int bz) {
        for (int x = Math.min(ax, bx); x <= Math.max(ax, bx); x++)
            for (int y = Math.min(ay, by); y <= Math.max(ay, by); y++)
                for (int z = Math.min(az, bz); z <= Math.max(az, bz); z++) mark(x, y, z);
    }

    /** Cylindre vertical (les bords peuvent etre ondules par {@code wobble} blocs pour un aspect de cavite naturelle). */
    void cyl(double cx, double cz, double r, int ya, int yb, double wobble, int salt) {
        int n = (int) Math.ceil(r + wobble) + 1;
        for (int x = (int) Math.floor(cx) - n; x <= (int) Math.ceil(cx) + n; x++) {
            for (int z = (int) Math.floor(cz) - n; z <= (int) Math.ceil(cz) + n; z++) {
                double rr = r;
                if (wobble > 0) {
                    double ang = Math.atan2(z - cz, x - cx);
                    rr += wobble * (Math.sin(ang * 3 + salt) * 0.6 + Math.sin(ang * 7 + salt * 1.7) * 0.4);
                }
                if (Math.hypot(x - cx, z - cz) > rr + 0.4) continue;
                for (int y = ya; y <= yb; y++) mark(x, y, z);
            }
        }
    }

    void cyl(double cx, double cz, double r, int ya, int yb) {
        cyl(cx, cz, r, ya, yb, 0, 0);
    }

    /** Demi-ellipsoide superieur (coupole) pose sur le plan y = {@code base}. */
    void dome(double cx, double cz, double rx, double rz, int base, double height) {
        int nx = (int) Math.ceil(rx) + 1;
        int nz = (int) Math.ceil(rz) + 1;
        int h = (int) Math.ceil(height) + 1;
        for (int x = (int) Math.floor(cx) - nx; x <= (int) Math.ceil(cx) + nx; x++) {
            for (int z = (int) Math.floor(cz) - nz; z <= (int) Math.ceil(cz) + nz; z++) {
                for (int y = base; y <= base + h; y++) {
                    double d = Math.pow((x - cx) / (rx + 0.3), 2) + Math.pow((z - cz) / (rz + 0.3), 2) + Math.pow((y - base) / (height + 0.3), 2);
                    if (d <= 1.0) mark(x, y, z);
                }
            }
        }
    }

    /**
     * Couloir voute en berceau le long de Z : murs droits jusqu'a {@code wallH}, puis demi-ellipse de hauteur {@code rise}.
     * {@code pointed} affine l'arc en ogive (gothique).
     */
    void vault(double cx, int za, int zb, double halfW, int ya, int wallH, double rise, boolean pointed) {
        int n = (int) Math.ceil(halfW) + 1;
        int top = ya + wallH + (int) Math.ceil(rise) + 1;
        for (int z = Math.min(za, zb); z <= Math.max(za, zb); z++) {
            for (int x = (int) Math.floor(cx) - n; x <= (int) Math.ceil(cx) + n; x++) {
                double u = Math.abs(x - cx) / (halfW + 0.35);
                if (u > 1.0) continue;
                for (int y = ya; y <= top; y++) {
                    if (y <= ya + wallH) {
                        mark(x, y, z);
                    } else {
                        double v = (y - (ya + wallH)) / (rise + 0.35);
                        double lim = pointed ? 1.0 - Math.pow(u, 1.6) : Math.sqrt(Math.max(0, 1.0 - u * u));
                        if (v <= lim) mark(x, y, z);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ coque

    private boolean[] first;

    /** Calcule la premiere couche de la coque (aucun bloc n'est pose) : a appeler une fois avant les tranches. */
    void prepareShell() {
        first = new boolean[in.length];
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = z0; z <= z1; z++) {
                    if (!in[idx(x, y, z)] && touches(x, y, z, in)) first[idx(x, y, z)] = true;
                }
            }
        }
    }

    /** Creuse l'interieur puis pose la coque (deux couches) pour les tranches z comprises entre {@code za} et {@code zb}. */
    void shellSlice(HubCtx c, ShellPick pick, int za, int zb) {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = x0; x <= x1; x++) {
            for (int y = y0; y <= y1; y++) {
                for (int z = Math.max(z0, za); z <= Math.min(z1, zb); z++) {
                    int i = idx(x, y, z);
                    if (in[i]) {
                        c.set(x, y, z, air);
                    } else if (first[i]) {
                        Kind k = inside(x, y + 1, z) && !inside(x, y - 1, z) ? Kind.FLOOR
                                : inside(x, y - 1, z) && !inside(x, y + 1, z) ? Kind.CEILING : Kind.WALL;
                        BlockState s = pick.at(x, y, z, k, false);
                        if (s != null) c.set(x, y, z, s);
                    } else if (touches(x, y, z, first)) {
                        BlockState s = pick.at(x, y, z, Kind.WALL, true);
                        if (s != null && c.air(x, y, z)) c.set(x, y, z, s);
                    }
                }
            }
        }
    }

    /** Creuse l'interieur puis pose la coque d'un coup (construction directe). */
    void shell(HubCtx c, ShellPick pick) {
        prepareShell();
        shellSlice(c, pick, z0, z1);
    }

    private boolean touches(int x, int y, int z, boolean[] set) {
        return flag(set, x + 1, y, z) || flag(set, x - 1, y, z) || flag(set, x, y + 1, z) || flag(set, x, y - 1, z)
                || flag(set, x, y, z + 1) || flag(set, x, y, z - 1);
    }

    private boolean flag(boolean[] set, int x, int y, int z) {
        return inBounds(x, y, z) && set[idx(x, y, z)];
    }

    /** Distance au plus proche point interieur le long des axes, utile pour moduler les motifs (0 = jouxte le vide). */
    static double polar(int x, int z, double cx, double cz) {
        return Math.hypot(x - cx, z - cz);
    }

    static double angle(int x, int z, double cx, double cz) {
        return (Math.toDegrees(Math.atan2(z - cz, x - cx)) + 360) % 360;
    }

    static int clampInt(int v, int lo, int hi) {
        return Mth.clamp(v, lo, hi);
    }
}
