package com.mmorpg.hub;

import com.mmorpg.rpg.PlayerClass;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Plan du hub, en coordonnees relatives a son origine (centre de la place, au niveau ou l'on marche ; le sol est en y = -1).
 * x : est, z : sud. Le hub est toujours construit dans cette orientation : la place est un disque de 31 blocs de rayon avec la
 * rotonde au centre, l'entree au sud, l'arene d'epreuve au nord, l'aire d'entrainement a l'est et un pavillon par classe sur
 * chaque diagonale.
 */
public final class HubLayout {
    private HubLayout() {
    }

    public static final int PLAZA_RADIUS = 31;
    /** Place d'apparition des nouveaux joueurs : sur le parvis, face au portail. */
    public static final int[] SPAWN = {0, 0, 46};
    public static final int[] TELEPORTER = {0, 0, 24};
    public static final int[] CLASS_MASTER = {0, 0, 0};
    /** Arene d'epreuve (disque de rayon 22) : de z = -96 a -52 ; entree au sud. */
    public static final int ROOM_HALF = 22;
    public static final int ROOM_Z0 = -96;
    public static final int ROOM_Z1 = -52;
    public static final int ROOM_HEIGHT = 40;
    public static final int[] TRIAL_SPAWN = {0, 0, -58};
    public static final int[] TRIAL_MASTER = {-8, 0, -58};
    /** Aire d'entrainement a l'est (disque de rayon 15 centre en x = 58). */
    public static final int YARD_X0 = 43;
    public static final int YARD_X1 = 73;
    public static final int YARD_HALF = 15;
    public static final int YARD_CENTER_X = 58;

    /** Zones de classe : sur les diagonales de la place. */
    public static int[] classSpot(PlayerClass cls) {
        return switch (cls) {
            case GUERRIER -> new int[]{-20, 0, -20};
            case MAGE -> new int[]{20, 0, -20};
            case ARCHER -> new int[]{-20, 0, 20};
            default -> new int[]{20, 0, 20};
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
    public static final int[][] DUMMIES_ROOM = {{-13, -86, 0, 0}, {0, -90, 1, 0}, {13, -86, 2, 0}, {-7, -78, 0, 0}, {7, -78, 0, 0}};
    public static final int[][] DUMMIES_YARD = {{52, -8, 0, 90}, {52, 0, 1, 90}, {52, 8, 2, 90}, {60, -8, 2, 90}, {60, 0, 3, 90}, {60, 8, 1, 90}};

    public static BlockPos pos(BlockPos origin, int[] p) {
        return origin.offset(p[0], p[1], p[2]);
    }

    public static Vec3 center(BlockPos origin, int[] p) {
        return new Vec3(origin.getX() + p[0] + 0.5, origin.getY() + p[1], origin.getZ() + p[2] + 0.5);
    }

    /** Volume de l'arene d'epreuve : quitter ce volume met fin a l'essai. */
    public static AABB roomBounds(BlockPos origin) {
        return new AABB(origin.getX() - ROOM_HALF - 2, origin.getY() - 6, origin.getZ() + ROOM_Z0 - 2,
                origin.getX() + ROOM_HALF + 3, origin.getY() + ROOM_HEIGHT + 4, origin.getZ() + ROOM_Z1 + 2);
    }

    /** Volume englobant tout le hub (cinematique, protection). */
    public static AABB hubBounds(BlockPos origin) {
        return new AABB(origin.getX() - 52, origin.getY() - 36, origin.getZ() + ROOM_Z0 - 10, origin.getX() + YARD_X1 + 12, origin.getY() + 48, origin.getZ() + 66);
    }
}
