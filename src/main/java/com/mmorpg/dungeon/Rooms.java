package com.mmorpg.dungeon;

import com.mmorpg.hub.HubCtx;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;

/**
 * Decors generiques des salles et couloirs supplementaires d'un donjon, habilles avec les blocs d'accent de l'ambiance
 * ({@link Theme.Look}). Les motifs restent sur le pourtour ou au centre : le milieu des salles est libre pour les epreuves (dalles
 * a +-6, cibles, lutrin a l'entree) et les combats.
 */
final class Rooms {
    private Rooms() {
    }

    /** Couloir sans fosse : arcs reguliers, lanternes, tapis et braseros. */
    static void corridor(Theme th, Layout.Corridor cor) {
        HubCtx c = th.c;
        Theme.Look k = th.look();
        for (int z = cor.z0() + 4; z <= cor.z1() - 2; z += 6) {
            Kit.doorway(c, 0, z, 3, 5, k.accent(), k.stair());
            c.set(0, 5, z, c.hanging(k.lamp()));
            c.set(-3, 5, z, c.hanging(k.lamp()));
            c.set(3, 5, z, c.hanging(k.lamp()));
        }
        for (int z = cor.z0() + 1; z <= cor.z1() - 1; z++) {
            for (int x = -1; x <= 1; x++) Kit.put(c, x, 0, z, (z / 2) % 2 == 0 ? k.floorA() : k.floorB());
        }
        for (int z = cor.z0() + 8; z <= cor.z1() - 5; z += 12) {
            for (int side : new int[]{-3, 3}) Kit.brazier(c, side, 1, z, k.pillar(), k.fire());
        }
    }

    /** Salle a colonnade : douze colonnes sur le pourtour, medaillon au sol, nervures et lustres. */
    static void pillared(Theme th, Layout.Room rm) {
        HubCtx c = th.c;
        Theme.Look k = th.look();
        double cz = rm.z();
        Kit.ribs(c, 0, cz, rm.r(), rm.r(), rm.h(), 9, 12, k.pillar());
        Kit.band(c, 0, cz, rm.r(), 3, k.accent());
        Kit.colonnade(c, 0, cz, rm.r() - 3.4, 12, Math.PI / 12, 1, rm.h(), k.accent(), k.pillar(), k.stair());
        Kit.rings(c, 0, 0, rm.z(), 4.6, k.floorA(), k.floorB(), k.accent());
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12 + Math.PI / 6;
            int x = (int) Math.round(Math.cos(a) * (rm.r() - 1.6));
            int z = (int) Math.round(cz + Math.sin(a) * (rm.r() - 1.6));
            if (c.air(x, 1, z)) c.set(x, 1, z, k.lamp());
        }
        for (int i = 0; i < 4; i++) {
            double a = Math.PI * 2 * i / 4 + Math.PI / 4;
            Kit.chandelier(c, rm.h() + 6, (int) Math.round(Math.cos(a) * 6.5), (int) Math.round(cz + Math.sin(a) * 6.5), 3, k.lamp());
        }
    }

    /** Sanctuaire : estrade a deux marches au centre, obelisque, braseros et vitrine de lumiere. */
    static void shrine(Theme th, Layout.Room rm) {
        HubCtx c = th.c;
        Theme.Look k = th.look();
        int cz = rm.z();
        Kit.ribs(c, 0, cz, rm.r(), rm.r(), rm.h(), 9, 8, k.pillar());
        Kit.band(c, 0, cz, rm.r(), 5, k.accent());
        c.disc(0, 1, cz, 3.6, (x, y, z) -> c.state(k.floorA()));
        c.disc(0, 2, cz, 2.2, (x, y, z) -> c.state(k.floorB()));
        for (int y = 3; y <= 8; y++) Kit.put(c, 0, y, cz, y % 3 == 0 ? k.accent() : k.pillar());
        Kit.put(c, 0, 9, cz, k.prop());
        for (Direction d : Direction.Plane.HORIZONTAL) {
            c.set(d.getStepX(), 3, cz + d.getStepZ(), c.stair(k.stair(), d, false));
            Kit.brazier(c, d.getStepX() * 4, 1, cz + d.getStepZ() * 4, k.pillar(), k.fire());
        }
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8 + Math.PI / 8;
            int x = (int) Math.round(Math.cos(a) * (rm.r() - 1.7));
            int z = (int) Math.round(cz + Math.sin(a) * (rm.r() - 1.7));
            Kit.column(c, x, z, 1, rm.h() - 3, k.accent(), k.pillar(), k.stair());
            Kit.put(c, x, rm.h() - 2, z, k.prop());
        }
        for (int i = 0; i < 3; i++) {
            double a = Math.PI * 2 * i / 3 + 0.4;
            Kit.chandelier(c, rm.h() + 6, (int) Math.round(Math.cos(a) * 6), (int) Math.round(cz + Math.sin(a) * 6), 3, k.lamp());
        }
    }

    /** Coliseum : gradins annulaires le long du mur, garde-corps lumineux et braseros. */
    static void coliseum(Theme th, Layout.Room rm) {
        HubCtx c = th.c;
        Theme.Look k = th.look();
        int cz = rm.z();
        double r = rm.r();
        Kit.ribs(c, 0, cz, r, r, rm.h(), 9, 10, k.pillar());
        c.ring(0, 1, cz, r - 2.6, r - 1.6, (x, y, z) -> c.state(k.floorA()));
        c.ring(0, 2, cz, r - 1.7, r - 0.6, (x, y, z) -> c.state(k.floorB()));
        c.ring(0, 3, cz, r - 0.9, r + 0.4, (x, y, z) -> c.state(k.accent()));
        for (int i = 0; i < 16; i++) {
            double a = Math.PI * 2 * i / 16 + Math.PI / 16;
            int x = (int) Math.round(Math.cos(a) * (r - 1.2));
            int z = (int) Math.round(cz + Math.sin(a) * (r - 1.2));
            Kit.put(c, x, 4, z, k.pillar());
            c.set(x, 5, z, k.lamp());
        }
        Kit.rings(c, 0, 0, cz, 6.5, k.floorA(), k.floorB(), k.accent());
        for (int i = 0; i < 4; i++) {
            double a = Math.PI * 2 * i / 4 + Math.PI / 4;
            Kit.chandelier(c, rm.h() + 6, (int) Math.round(Math.cos(a) * 7), (int) Math.round(cz + Math.sin(a) * 7), 3, k.lamp());
        }
    }

    /** Salle du gardien d'elite : tapis, rangee de statues, trone au fond et lustres monumentaux. */
    static void throne(Theme th, Layout.Room rm) {
        HubCtx c = th.c;
        Theme.Look k = th.look();
        int cz = rm.z();
        double r = rm.r();
        Kit.ribs(c, 0, cz, r, r, rm.h(), 11, 14, k.pillar());
        Kit.band(c, 0, cz, r, 6, k.accent());
        for (int z = cz - rm.r() + 1; z <= cz + 7; z++) {
            for (int x = -1; x <= 1; x++) Kit.put(c, x, 0, z, Math.abs(x) == 1 ? k.accent() : k.floorA());
        }
        // statues de gardiens
        for (int i = 0; i < 6; i++) {
            double a = Math.PI * 2 * i / 6 + Math.PI / 6;
            int x = (int) Math.round(Math.cos(a) * (r - 4.2));
            int z = (int) Math.round(cz + Math.sin(a) * (r - 4.2));
            Direction face = Direction.getApproximateNearest(-x, 0, cz - z);
            Kit.statue(c, x, 1, z, face, k.floorB(), k.pillar(), k.accent(), c.state(k.prop()), k.stair());
        }
        // trone au fond, devant l'alcove du couloir de sortie
        int tz = cz + rm.r() - 5;
        c.fill(-4, 1, tz, 4, 1, tz + 3, k.floorA());
        c.fill(-3, 2, tz + 1, 3, 2, tz + 3, k.floorB());
        c.fill(-1, 3, tz + 2, 1, 3, tz + 3, k.accent());
        for (int y = 4; y <= 8; y++) c.fill(-1, y, tz + 3, 1, y, tz + 3, k.pillar());
        Kit.put(c, 0, 9, tz + 3, k.prop());
        c.set(-2, 3, tz + 2, c.stair(k.stair(), Direction.EAST, false));
        c.set(2, 3, tz + 2, c.stair(k.stair(), Direction.WEST, false));
        for (int side : new int[]{-1, 1}) Kit.brazier(c, side * 5, 2, tz + 1, k.pillar(), k.fire());
        for (int i = 0; i < 5; i++) {
            double a = Math.PI * 2 * i / 5 + 0.3;
            Kit.chandelier(c, rm.h() + 9, (int) Math.round(Math.cos(a) * 9), (int) Math.round(cz + Math.sin(a) * 9), 5, k.lamp());
        }
        for (int i = 0; i < 12; i++) {
            double a = Math.PI * 2 * i / 12;
            int x = (int) Math.round(Math.cos(a) * (r - 1.4));
            int z = (int) Math.round(cz + Math.sin(a) * (r - 1.4));
            if (c.air(x, 1, z)) c.set(x, 1, z, k.lamp());
        }
    }

    @SuppressWarnings("unused")
    private static Object unused() {
        return Blocks.AIR;
    }
}
