package com.mmorpg.hub;

import com.mmorpg.rpg.PlayerClass;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Plan du hub, en coordonnees relatives a son origine (centre de la place, au niveau ou l'on marche ; le sol est en y = -1).
 * x : est, z : sud. Le hub est toujours construit dans cette orientation : la place est un disque, l'entree au sud, la salle
 * d'epreuve au nord, l'aire d'entrainement a l'est.
 */
public final class HubLayout {
    private HubLayout() {
    }

    public static final int PLAZA_RADIUS = 17;
    /** Place d'apparition des nouveaux joueurs (juste a l'interieur de la porte sud, face a la place). */
    public static final int[] SPAWN = {0, 0, 21};
    public static final int[] TELEPORTER = {0, 0, 13};
    public static final int[] CLASS_MASTER = {0, 0, 0};
    /** Salle d'epreuve : demi-largeur 15, de z = -52 a -22 ; entree au milieu du mur sud. */
    public static final int ROOM_HALF = 15;
    public static final int ROOM_Z0 = -52;
    public static final int ROOM_Z1 = -22;
    public static final int ROOM_HEIGHT = 10;
    public static final int[] TRIAL_SPAWN = {0, 0, -30};
    public static final int[] TRIAL_MASTER = {-5, 0, -27};
    /** Aire d'entrainement a l'est. */
    public static final int YARD_X0 = 22;
    public static final int YARD_X1 = 42;
    public static final int YARD_HALF = 10;

    /** Zones de classe : sur les diagonales de la place. */
    public static int[] classSpot(PlayerClass cls) {
        return switch (cls) {
            case GUERRIER -> new int[]{-10, 0, -10};
            case MAGE -> new int[]{10, 0, -10};
            case ARCHER -> new int[]{-10, 0, 10};
            default -> new int[]{10, 0, 10};
        };
    }

    /** Orientation (yaw) d'un PNJ de zone : il regarde le centre de la place. */
    public static float classYaw(PlayerClass cls) {
        return switch (cls) {
            case GUERRIER -> 315F;
            case MAGE -> 45F;
            case ARCHER -> 225F;
            default -> 135F;
        };
    }

    /** Mannequins : {x, z, variante, yaw}. */
    public static final int[][] DUMMIES_ROOM = {{-9, -46, 0, 0}, {0, -46, 1, 0}, {9, -46, 2, 0}, {-5, -39, 0, 0}, {5, -39, 0, 0}};
    public static final int[][] DUMMIES_YARD = {{28, -6, 0, 90}, {28, 0, 1, 90}, {28, 6, 2, 90}, {36, -6, 2, 90}, {36, 0, 0, 90}, {36, 6, 1, 90}};

    public static BlockPos pos(BlockPos origin, int[] p) {
        return origin.offset(p[0], p[1], p[2]);
    }

    public static Vec3 center(BlockPos origin, int[] p) {
        return new Vec3(origin.getX() + p[0] + 0.5, origin.getY() + p[1], origin.getZ() + p[2] + 0.5);
    }

    /** Volume de la salle d'epreuve : quitter ce volume met fin a l'essai. */
    public static AABB roomBounds(BlockPos origin) {
        return new AABB(origin.getX() - ROOM_HALF - 1, origin.getY() - 4, origin.getZ() + ROOM_Z0 - 1,
                origin.getX() + ROOM_HALF + 2, origin.getY() + ROOM_HEIGHT + 4, origin.getZ() + ROOM_Z1 + 1);
    }

    /** Volume englobant tout le hub (cinematique, protection). */
    public static AABB hubBounds(BlockPos origin) {
        return new AABB(origin.getX() - 24, origin.getY() - 6, origin.getZ() + ROOM_Z0 - 6, origin.getX() + YARD_X1 + 6, origin.getY() + 30, origin.getZ() + 30);
    }
}
