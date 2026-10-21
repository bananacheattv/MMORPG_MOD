package com.mmorpg.hub;

import com.mmorpg.rpg.PlayerClass;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

/**
 * Les quatre pavillons de classe : sol en mosaique radiale, colonnade ronde ouverte vers la place, poutre annulaire,
 * piedestal du Maitre et une grande sculpture (epee, obelisque de cristal, arc, bouclier) dressee derriere lui.
 */
final class HubPavilions {
    private HubPavilions() {
    }

    private record Theme(Object a, Object b, Object accent, Object column, Object cap, Object trim) {
    }

    private static Theme theme(PlayerClass cls) {
        return switch (cls) {
            case GUERRIER -> new Theme(Blocks.POLISHED_BLACKSTONE_BRICKS, "fer_noirci", Blocks.RED_NETHER_BRICKS, "fer_noirci", "or_ouvrage", "tuile_ecarlate");
            case MAGE -> new Theme("marbre_poli", "marbre", "tuile_pourpre", "colonne_marbre", "cristal_arcane", "tuile_pourpre");
            case ARCHER -> new Theme(Blocks.MOSS_BLOCK, "bois_ancien", Blocks.GRASS_BLOCK, Blocks.STRIPPED_DARK_OAK_LOG, "bois_ancien", "tuile_sylve");
            default -> new Theme("pierre_azur", "marbre_poli", "tuile_givre", "colonne_marbre", "pierre_azur", "tuile_givre");
        };
    }

    static void build(HubCtx c) {
        for (PlayerClass cls : HubBuilder.CLASSES) pavilion(c, cls);
    }

    private static double spoke(double dx, double dz) {
        double a = Math.toDegrees(Math.atan2(dz, dx));
        double m = ((a % 45) + 45) % 45;
        return Math.min(m, 45 - m);
    }

    private static void pavilion(HubCtx c, PlayerClass cls) {
        int[] s = HubLayout.classSpot(cls);
        int cx = s[0];
        int cz = s[2];
        int sx = cx > 0 ? 1 : -1;
        int sz = cz > 0 ? 1 : -1;
        Theme t = theme(cls);
        double front = Math.toDegrees(Math.atan2(-sz, -sx));
        c.disc(cx, -2, cz, 11.6, (x, y, z) -> c.state(Blocks.STONE_BRICKS));
        for (int y = 0; y <= 19; y++) c.disc(cx, y, cz, 12.5, (x, yy, z) -> Blocks.AIR.defaultBlockState());
        c.disc(cx, -1, cz, 11.6, (x, y, z) -> {                      // mosaique radiale
            double dx = x - cx;
            double dz = z - cz;
            double r = Math.hypot(dx, dz);
            if (r > 10.3) return c.state(t.accent());
            if (r > 8.2) return c.mix(x, y, z, 41, t.b(), 55, t.a(), 45);
            if (r > 7.2) return c.state(t.trim());
            if (r > 3.4 && spoke(dx, dz) < 5.5 * 4.0 / r + 1.2) return c.state(t.trim());
            if (r > 5.8 && r <= 6.4) return c.state(t.cap());
            return c.mix(x, y, z, 42, t.a(), 60, t.b(), 40);
        });
        for (int k = 0; k < 10; k++) {                               // colonnade ronde, ouverte du cote de la place
            double ang = k * 36 + 18;
            double diff = Math.abs(((ang - front + 540) % 360) - 180);
            if (diff < 38) continue;
            int x = cx + (int) Math.round(Math.cos(Math.toRadians(ang)) * 8.8);
            int z = cz + (int) Math.round(Math.sin(Math.toRadians(ang)) * 8.8);
            if (Math.hypot(x - cx - sx * 5, z - cz - sz * 5) < 4.2) continue;      // la sculpture traverse le toit
            c.set(x, 0, z, c.state(t.cap()));
            for (int y = 1; y <= 9; y++) c.set(x, y, z, c.axis(t.column(), Direction.Axis.Y));
            c.set(x, 10, z, c.state(t.cap()));
            for (Direction d : Direction.Plane.HORIZONTAL) c.set(x + d.getStepX(), 10, z + d.getStepZ(), c.slab(t.trim(), false));
            c.set(x, 11, z, Blocks.LANTERN);
        }
        final double mx = cx + sx * 5;
        final double mz = cz + sz * 5;
        c.ring(cx, 10, cz, 7.6, 9.8, (x, y, z) -> Math.hypot(x - mx, z - mz) < 6.2 ? null : c.state(t.trim()));    // poutre annulaire
        c.ring(cx, 11, cz, 8.4, 9.2, (x, y, z) -> Math.hypot(x - mx, z - mz) < 6.2 ? null : c.slab(t.trim(), false));
        for (int k = 0; k < 10; k++) {                                   // lanternes pendues
            double ang = k * 36;
            int x = cx + (int) Math.round(Math.cos(Math.toRadians(ang)) * 7.8);
            int z = cz + (int) Math.round(Math.sin(Math.toRadians(ang)) * 7.8);
            c.set(x, 9, z, Blocks.IRON_CHAIN);
            c.set(x, 8, z, c.hanging(Blocks.LANTERN));
        }
        c.disc(cx, 0, cz, 2.4, (x, y, z) -> c.state(t.cap()));           // piedestal : le Maitre se tient a y = 1
        c.ring(cx, 0, cz, 2.4, 3.6, (x, y, z) -> c.slab(t.b(), false));
        switch (cls) {
            case GUERRIER -> sword(c, cx + sx * 5, cz + sz * 5, sz, t);
            case MAGE -> obelisk(c, cx + sx * 5, cz + sz * 5);
            case ARCHER -> bow(c, cx + sx * 4, cz + sz * 6, sz, t);
            default -> shield(c, cx + sx * 5, cz + sz * 6, sz, t);
        }
    }

    /** Pose une grille de pixels verticale dans un plan z = const (rangee 0 = haut). */
    private static void plane(HubCtx c, int cx, int baseY, int z, int sz, int thick, String[] rows, Map<Character, Object> pal) {
        int w = rows[0].length();
        for (int row = 0; row < rows.length; row++) {
            for (int col = 0; col < w; col++) {
                Object b = pal.get(rows[row].charAt(col));
                if (b == null) continue;
                int y = baseY + rows.length - 1 - row;
                for (int d = 0; d < thick; d++) c.set(cx - w / 2 + col, y, z + sz * d, c.state(b));
            }
        }
    }

    private static final String[] SWORD = {
            "...GGG...", "..GGGGG..", "...GGG...", "....H....", "....H....", "....H....", "....H....",
            "GGGGGGGGG", ".GGGGGGG.", "...BIB...", "...BIB...", "...BIB...", "...BIB...", "...BIB...", "...BIB...", "...BIB...",
            "...BIB...", "...BIB...", "...BIB...", "...BIB...", "...BIB...", "....B....", "....B...."};

    private static void sword(HubCtx c, int bx, int bz, int sz, Theme t) {
        c.disc(bx, 0, bz, 3.4, (x, y, z) -> c.mix(x, y, z, 51, "roche_celeste", 50, Blocks.BLACKSTONE, 30, "fer_noirci", 20));
        c.disc(bx, 1, bz, 2.2, (x, y, z) -> c.mix(x, y, z, 52, "roche_celeste", 70, Blocks.BLACKSTONE, 30));
        plane(c, bx, 2, bz, sz, 2, SWORD, Map.of('G', "or_ouvrage", 'H', "bois_ancien", 'B', Blocks.IRON_BLOCK, 'I', "marbre"));
        for (int[] o : new int[][]{{-3, 0}, {3, 0}, {0, -3}, {0, 3}}) {      // braseros au pied de l'epee
            c.set(bx + o[0] * 1, 1, bz + o[1] * 1, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
            c.set(bx + o[0] * 1, 2, bz + o[1] * 1, Blocks.CAMPFIRE);
        }
    }

    private static void obelisk(HubCtx c, int bx, int bz) {
        c.disc(bx, 0, bz, 4.2, (x, y, z) -> c.mix(x, y, z, 53, "marbre_poli", 60, "marbre", 25, "or_ouvrage", 15));
        c.disc(bx, 1, bz, 3.2, (x, y, z) -> c.state("marbre_poli"));
        for (int y = 2; y <= 22; y++) {
            double r = y <= 12 ? 2.7 : 2.7 * Math.pow(1 - (y - 12) / 11.0, 0.75);
            if (r < 0.5) r = 0.5;
            final int yy = y;
            c.disc(bx, y, bz, r, (x, yq, z) -> c.mix(x, yy, z, 54, "cristal_arcane", 70, "cristal_azur", 18, "marbre_poli", 12));
        }
        c.set(bx, 23, bz, "cristal_azur");
        c.set(bx, 24, bz, Blocks.END_ROD);
        for (int k = 0; k < 24; k++) {                                       // anneaux flottants de cristal
            double a = Math.toRadians(k * 15);
            if (k % 2 == 0) c.set(bx + (int) Math.round(Math.cos(a) * 5.6), 10, bz + (int) Math.round(Math.sin(a) * 5.6), "cristal_arcane");
            else c.set(bx + (int) Math.round(Math.cos(a) * 5.6), 10, bz + (int) Math.round(Math.sin(a) * 5.6), Blocks.END_ROD);
            if (k % 3 == 0) c.set(bx + (int) Math.round(Math.cos(a) * 4.4), 15, bz + (int) Math.round(Math.sin(a) * 4.4), "cristal_azur");
        }
    }

    private static void bow(HubCtx c, int bx, int bz, int sz, Theme t) {
        c.disc(bx + 1, 0, bz, 4.2, (x, y, z) -> c.mix(x, y, z, 55, "roche_celeste", 40, Blocks.MOSS_BLOCK, 40, Blocks.GRASS_BLOCK, 20));
        c.disc(bx + 1, 1, bz, 3, (x, y, z) -> c.mix(x, y, z, 56, Blocks.MOSS_BLOCK, 50, Blocks.GRASS_BLOCK, 50));
        for (int y = 2; y <= 22; y++) {
            double u = (y - 12.0) / 10.0;
            int d = (int) Math.round(5.0 * (1 - u * u));
            for (int w = 0; w < 2; w++) {
                for (int dz = 0; dz < 2; dz++) c.set(bx - d - w, y, bz + sz * dz, y <= 3 || y >= 21 ? c.state("or_ouvrage") : c.state("bois_ancien"));
            }
            if (y >= 11 && y <= 13) for (int dz = 0; dz < 2; dz++) c.set(bx - d + 1, y, bz + sz * dz, "fer_noirci");
        }
        for (int y = 3; y <= 21; y++) c.set(bx + 1, y, bz, Blocks.IRON_CHAIN);        // corde
        c.line(bx - 3, 12, bz, bx + 9, 12, bz, c.state("fer_noirci"));                // fleche encochee
        c.set(bx + 10, 12, bz, "or_ouvrage");
        c.set(bx - 3, 13, bz, "marbre");
        c.set(bx - 3, 11, bz, "marbre");
        c.set(bx - 4, 12, bz, "marbre");
    }

    private static void shield(HubCtx c, int bx, int bz, int sz, Theme t) {
        c.disc(bx, 0, bz, 4.2, (x, y, z) -> c.mix(x, y, z, 57, "marbre_poli", 60, "pierre_azur", 40));
        c.disc(bx, 1, bz, 3.2, (x, y, z) -> c.state("marbre_poli"));
        double cy = 12;
        double R = 7.2;
        for (int y = 2; y <= 20; y++) {
            for (int x = bx - 8; x <= bx + 8; x++) {
                double dx = x - bx;
                double dy = y - cy;
                double d = dy >= 0 ? Math.hypot(dx, dy) : Math.hypot(dx, dy * 0.62);
                if (d > R) continue;
                BlockState b;
                if (d > R - 1.3) b = c.state("or_ouvrage");
                else if (d <= 1.9) b = c.state("or_ouvrage");
                else if (Math.abs(dx) < 1.2 || Math.abs(dy) < 1.2) b = c.state("marbre");
                else b = (dx > 0) == (dy > 0) ? c.state("pierre_azur") : c.state("tuile_givre");
                for (int dz = 0; dz < 2; dz++) c.set(x, y, bz + sz * dz, b);
            }
        }
    }
}
