package com.mmorpg.hub;

import com.mmorpg.rpg.PlayerClass;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Grande Nef d'Aeloria : la place centrale du hub en grande salle a ciel ouvert. Sol de dalles noires cerclees d'or, emblème d'Aeloria
 * en mosaique de 39 blocs au centre, huit colonnes colossales de marbre baguees d'or reliees par une corniche, lustres, bannieres
 * violettes, pointes de cristal et tapis d'avenue vers chaque quartier.
 */
final class HubHall {
    private HubHall() {
    }

    static final int EMBLEM_R = HubEmblem.SIZE / 2 + 1;       // rayon de la mosaique (20)
    static final double PILLAR_R = 27.5;
    static final int PILLAR_H = 24;

    private static BlockState emblemBlock(HubCtx c, char ch) {
        return switch (ch) {
            case 'G' -> Blocks.GOLD_BLOCK.defaultBlockState();
            case 'H' -> Blocks.GLOWSTONE.defaultBlockState();
            case 'W' -> Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.WHITE).defaultBlockState();
            case 'l' -> Blocks.AMETHYST_BLOCK.defaultBlockState();
            case 'V' -> Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.PURPLE).defaultBlockState();
            case 'v' -> Blocks.CRYING_OBSIDIAN.defaultBlockState();
            default -> Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLACK).defaultBlockState();
        };
    }

    /** Case d'avenue : tapis violet (0), liseré d'or (1) ou rien (-1) ; les avenues suivent les axes et les diagonales. */
    static int lane(int x, int z) {
        int ax = Math.abs(x);
        int az = Math.abs(z);
        int d = Math.min(Math.min(ax, az), Math.abs(ax - az));
        if (d <= 1) return 0;
        if (d == 2) return 1;
        return -1;
    }

    static void plaza(HubCtx c) {
        for (int x = -33; x <= 33; x++) {
            for (int z = -33; z <= 33; z++) {
                double r = Math.hypot(x, z);
                if (r > 31.6) continue;
                BlockState top;
                if (r <= EMBLEM_R - 0.4) {
                    top = emblemBlock(c, HubEmblem.at(x + HubEmblem.SIZE / 2, z + HubEmblem.SIZE / 2));
                } else if (r <= EMBLEM_R + 0.7) {
                    top = c.state("or_ouvrage");
                } else if (r > 30.2 && r <= 31.0) {
                    top = c.state("or_ouvrage");
                } else if (r > 31.0) {
                    top = Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState();
                } else {
                    int ln = lane(x, z);
                    if (ln == 0) top = Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.PURPLE).defaultBlockState();
                    else if (ln == 1) top = c.state("or_ouvrage");
                    else if (r > 25.0 && r <= 25.9) top = c.state("marbre_dore");
                    else if ((x & 3) == 0 || (z & 3) == 0) top = Blocks.CONCRETE.pick(net.minecraft.world.item.DyeColor.BLACK).defaultBlockState();
                    else top = c.mix(x, 0, z, 61, Blocks.POLISHED_BLACKSTONE, 46, Blocks.DEEPSLATE_TILES, 30, Blocks.BLACKSTONE, 14, Blocks.POLISHED_DEEPSLATE, 10);
                }
                c.set(x, -2, z, Blocks.STONE_BRICKS.defaultBlockState());
                c.set(x, -1, z, top);
                c.set(x, 0, z, Blocks.AIR);
            }
        }
        for (int a = 0; a < 360; a++) {                            // balustrade noire du pourtour et pilastres a lanternes
            double rad = Math.toRadians(a);
            int x = (int) Math.round(Math.cos(rad) * 31);
            int z = (int) Math.round(Math.sin(rad) * 31);
            if (Math.abs(x) <= 8 && z != 0 && Math.abs(z) > 8) continue;        // portail et voie de l'arene
            if (Math.abs(x) > 8 && Math.abs(z) <= 8) continue;                    // allees de l'aire et du marche
            boolean nearPavilion = false;
            for (PlayerClass cls : HubBuilder.CLASSES) {
                int[] s = HubLayout.classSpot(cls);
                if (Math.hypot(x - s[0], z - s[2]) < 12) nearPavilion = true;
            }
            if (nearPavilion) continue;
            c.set(x, 0, z, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
            if (a % 10 == 0) {
                for (int y = 0; y <= 2; y++) c.set(x, y, z, Blocks.POLISHED_BLACKSTONE_BRICK_WALL);
                c.set(x, 3, z, Blocks.LANTERN);
            }
        }
        colonnade(c);
        spikes(c);
        porch(c, 1, 0);          // porche de l'allee de l'aire d'entrainement (est)
        porch(c, -1, 0);         // porche de l'allee du marche (ouest)
        porch(c, 0, -1);         // porche de la voie de l'arene (nord)
    }

    // ------------------------------------------------------------------ colonnes colossales, corniche, lustres, bannieres

    private static void pillar(HubCtx c, int px, int pz) {
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                c.set(px + dx, 0, pz + dz, "marbre_poli");
                boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                c.set(px + dx, 1, pz + dz, edge ? c.slab("marbre_poli", false) : c.state("marbre"));
            }
        }
        for (int y = 2; y <= PILLAR_H; y++) {
            boolean band = y % 8 == 0 || y % 8 == 1;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    boolean mid = dx == 0 && dz == 0;
                    boolean flute = (dx == 0) != (dz == 0);
                    BlockState s;
                    if (band && !mid) s = c.state("or_ouvrage");
                    else if (flute) s = c.axis("colonne_marbre", Direction.Axis.Y);
                    else s = c.state("marbre_poli");
                    c.set(px + dx, y, pz + dz, s);
                }
            }
        }
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                c.set(px + dx, PILLAR_H + 1, pz + dz, "marbre_dore");
                c.set(px + dx, PILLAR_H + 2, pz + dz, Math.abs(dx) == 2 || Math.abs(dz) == 2 ? c.state("or_ouvrage") : c.state("marbre_dore"));
            }
        }
        c.set(px, PILLAR_H + 3, pz, "cristal_arcane");
        c.set(px, PILLAR_H + 4, pz, Blocks.END_ROD);
    }

    private static void banner(HubCtx c, int px, int pz, int sx, int sz) {
        // panneau de 5 de large sur 10 de haut, plaque contre la face interieure du pilier (sx, sz = sens vers le centre)
        int bx = px + sx * 3;
        int bz = pz + sz * 3;
        for (int v = 0; v < 10; v++) {
            int y = PILLAR_H - 1 - v;
            int half = v < 8 ? 2 : (v == 8 ? 1 : 0);
            for (int u = -half; u <= half; u++) {
                int x = bx + (sx == 0 ? u : 0);
                int z = bz + (sz == 0 ? u : 0);
                boolean border = Math.abs(u) == half || v == 0;
                boolean emblem = v >= 3 && v <= 6 && Math.abs(u) <= Math.min(1, 3 - Math.abs(v - 4.5) / 1.4 + 0.5);
                BlockState s = border ? c.state("or_ouvrage") : (emblem ? Blocks.GOLD_BLOCK.defaultBlockState() : c.state("tuile_pourpre"));
                c.set(x, y, z, s);
            }
        }
    }

    private static void colonnade(HubCtx c) {
        for (int k = 0; k < 8; k++) {
            double a = Math.toRadians(22.5 + 45 * k);
            int px = (int) Math.round(Math.cos(a) * PILLAR_R);
            int pz = (int) Math.round(Math.sin(a) * PILLAR_R);
            pillar(c, px, pz);
            boolean alongX = Math.abs(Math.cos(a)) > Math.abs(Math.sin(a));
            banner(c, px, pz, alongX ? (px > 0 ? -1 : 1) : 0, alongX ? 0 : (pz > 0 ? -1 : 1));
        }
        // corniche circulaire reliant les colonnes
        int y0 = PILLAR_H + 1;
        c.ring(0, y0 + 1, 0, 25.4, 29.6, (x, y, z) -> c.state("marbre_poli"));
        c.ring(0, y0 + 2, 0, 25.0, 30.0, (x, y, z) -> c.slab("or_ouvrage", false));
        c.ring(0, y0, 0, 26.2, 28.8, (x, y, z) -> c.state("fer_noirci"));
        // lustres au-dessus de chaque avenue
        for (int k = 0; k < 8; k++) {
            double a = Math.toRadians(45 * k);
            chandelier(c, (int) Math.round(Math.cos(a) * PILLAR_R), y0 - 1, (int) Math.round(Math.sin(a) * PILLAR_R), 6, 2);
        }
    }

    /** Lustre : chaine depuis la corniche, couronne d'or et lanternes suspendues. */
    static void chandelier(HubCtx c, int x, int top, int z, int drop, int radius) {
        for (int y = top; y > top - drop; y--) c.set(x, y, z, Blocks.IRON_CHAIN);
        int ry = top - drop;
        c.set(x, ry, z, "or_ouvrage");
        for (int a = 0; a < 360; a += 45) {
            int lx = x + (int) Math.round(Math.cos(Math.toRadians(a)) * radius);
            int lz = z + (int) Math.round(Math.sin(Math.toRadians(a)) * radius);
            c.set(lx, ry, lz, c.slab("or_ouvrage", true));
            c.set(lx, ry - 1, lz, c.hanging(Blocks.LANTERN));
        }
        for (Direction d : Direction.Plane.HORIZONTAL) {
            for (int i = 1; i < radius; i++) c.set(x + d.getStepX() * i, ry, z + d.getStepZ() * i, c.slab("or_ouvrage", true));
        }
        c.set(x, ry - 1, z, c.hanging(Blocks.LANTERN));
    }

    private static void spikes(HubCtx c) {
        for (int k = 0; k < 24; k++) {
            double a = Math.toRadians(7.5 + 15 * k);
            int x = (int) Math.round(Math.cos(a) * 23.0);
            int z = (int) Math.round(Math.sin(a) * 23.0);
            if (lane(x, z) >= 0) continue;
            int h = 3 + (int) (c.rnd(x, 0, z, 71) * 4);
            c.set(x, 0, z, "marbre_poli");
            for (int y = 1; y < h; y++) c.set(x, y, z, "cristal_arcane");
            c.set(x, h, z, Blocks.AMETHYST_CLUSTER);
            for (Direction d : Direction.Plane.HORIZONTAL) {
                if (c.rnd(x + d.getStepX(), 1, z + d.getStepZ(), 72) < 0.5F) c.set(x + d.getStepX(), 0, z + d.getStepZ(), Blocks.AMETHYST_CLUSTER);
            }
        }
    }

    // ------------------------------------------------------------------ porches de pierre dense

    private static BlockState wallBlock(HubCtx c, int l, int y, int x, int z) {
        if (y <= 1) return y == 1 ? Blocks.POLISHED_BLACKSTONE_BRICKS.defaultBlockState() : Blocks.POLISHED_BLACKSTONE.defaultBlockState();
        if (y == 6 || y == 12 || y == 17) return c.state("or_ouvrage");
        if (y == 7 || y == 13) return Blocks.CHISELED_STONE_BRICKS.defaultBlockState();
        if (Math.floorMod(l, 4) == 0) return c.axis("colonne_marbre", Direction.Axis.Y);
        return c.mix(x, y, z, 311, Blocks.STONE_BRICKS, 34, Blocks.DEEPSLATE_TILES, 22, Blocks.POLISHED_BLACKSTONE_BRICKS, 20, Blocks.CHISELED_STONE_BRICKS, 8,
                Blocks.CRACKED_STONE_BRICKS, 8, Blocks.POLISHED_DEEPSLATE, 8);
    }

    /** Porche d'entree : mur epais de pierre appareillee (nervures, bandeaux d'or, niches a lanternes), arc brise, ecusson et bannieres. */
    private static void porch(HubCtx c, int ax, int az) {
        for (int l = -7; l <= 7; l++) {
            for (int rr = 25; rr <= 28; rr++) {
                for (int y = 0; y <= 20; y++) {
                    int x = ax != 0 ? ax * rr : l;
                    int z = az != 0 ? az * rr : l;
                    int al = Math.abs(l);
                    boolean arch = al <= 4 && y <= 10 - Math.max(0, al - 2) * 2;
                    if (arch) {
                        if (y == 0) continue;                                     // le tapis de l'avenue reste au sol
                        c.set(x, y, z, Blocks.AIR);
                        continue;
                    }
                    boolean border = (al == 5 && y <= 8) || (y >= 9 - Math.max(0, al - 2) * 2 && y <= 11 - Math.max(0, al - 2) * 2 && al <= 6);
                    BlockState st = border ? c.state("marbre_dore") : wallBlock(c, l, y, x, z);
                    boolean niche = rr == 25 && Math.floorMod(l, 4) == 2 && y >= 3 && y <= 5 && al >= 5;
                    if (niche) st = y == 4 ? Blocks.LANTERN.defaultBlockState() : Blocks.AIR.defaultBlockState();
                    if (y >= 19 && (Math.abs(l) + rr) % 2 == 0) st = Blocks.AIR.defaultBlockState();             // creneaux
                    c.set(x, y, z, st);
                }
            }
        }
        for (int dy = 12; dy <= 16; dy++) {                                            // ecusson d'or au-dessus de l'arc
            int half = 2 - Math.abs(dy - 14);
            for (int l = -half; l <= half; l++) {
                int rr = 24;
                c.set(ax != 0 ? ax * rr : l, dy, az != 0 ? az * rr : l, dy == 14 ? Blocks.GLOWSTONE.defaultBlockState() : Blocks.GOLD_BLOCK.defaultBlockState());
            }
        }
        for (int side : new int[]{-1, 1}) {                                            // bannieres de part et d'autre de l'arc
            int l = side * 6;
            for (int dy = 6; dy <= 15; dy++) {
                c.set(ax != 0 ? ax * 24 : l, dy, az != 0 ? az * 24 : l, dy == 15 ? c.state("or_ouvrage") : c.state("tuile_pourpre"));
            }
        }
    }

    // ------------------------------------------------------------------ rotonde ouverte autour du Maitre des classes

    static void rotunda(HubCtx c) {
        final int H = 15;
        for (int k = 0; k < 12; k++) {
            double a = Math.toRadians(15 + k * 30);
            int x = (int) Math.round(Math.cos(a) * 8.4);
            int z = (int) Math.round(Math.sin(a) * 8.4);
            c.set(x, 0, z, "marbre_dore");
            for (int y = 1; y <= H; y++) c.set(x, y, z, y % 7 == 0 ? c.state("or_ouvrage") : c.axis("colonne_marbre", Direction.Axis.Y));
            c.set(x, H + 1, z, "marbre_dore");
            for (int[] o : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) c.set(x + o[0], H + 1, z + o[1], c.slab("marbre_dore", false));
            c.set(x, H + 2, z, "or_ouvrage");
            c.set(x, H + 3, z, "cristal_azur");
        }
        c.ring(0, H + 2, 0, 7.5, 9.8, (x, y, z) -> c.state("marbre_poli"));
        c.ring(0, H + 3, 0, 8.0, 9.4, (x, y, z) -> (x + z) % 3 == 0 ? c.state("cristal_azur") : c.state("or_ouvrage"));
        for (int k = 0; k < 6; k++) {                              // lustres de la rotonde
            double a = Math.toRadians(k * 60);
            chandelier(c, (int) Math.round(Math.cos(a) * 6.0), H + 1, (int) Math.round(Math.sin(a) * 6.0), 4, 1);
        }
        c.set(0, 8, 0, "cristal_azur");                            // coeur de cristal flottant au-dessus du Maitre des classes
        c.set(0, 7, 0, "cristal_azur");
        c.set(0, 9, 0, "cristal_azur");
        c.set(0, 10, 0, "cristal_azur");
        for (Direction d : Direction.Plane.HORIZONTAL) {
            c.set(d.getStepX(), 8, d.getStepZ(), "cristal_azur");
            c.set(d.getStepX() * 2, 8, d.getStepZ() * 2, Blocks.END_ROD);
        }
        for (int k = 0; k < 8; k++) {
            double a = Math.toRadians(k * 45 + 22.5);
            c.set((int) Math.round(Math.cos(a) * 2.6), 7, (int) Math.round(Math.sin(a) * 2.6), Blocks.END_ROD);
        }
    }
}
