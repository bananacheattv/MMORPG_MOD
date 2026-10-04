package com.mmorpg.block.structure;

import com.mmorpg.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Structures multiblocs (modeles Blockbench de art/structures_blockbench) : emprise et collisions de chaque bloc,
 * lues dans StructureShapes (genere), pour une structure tournee vers le nord puis tournees selon son orientation.
 * Le bloc fonctionnel est la cellule (0, 0, 0) ; les autres cellules portant une collision recoivent un bloc
 * {@link StructurePartBlock} invisible.
 */
public enum MultiblockKind implements StringRepresentable {
    AUTEL("autel_invocation", () -> ModBlocks.AUTEL_INVOCATION.get()),
    TELEPORTEUR("teleporteur", () -> ModBlocks.TELEPORTEUR.get()),
    FORGE("forge_arcanique", () -> ModBlocks.FORGE_ARCANIQUE.get());

    /** Decalage maximal des cellules par rapport au bloc fonctionnel (proprietes du bloc de structure). */
    public static final int RANGE_XZ = 3;
    public static final int RANGE_Y = 4;

    private final String key;
    private final Supplier<Block> master;
    private final Map<Long, double[][]> boxes = new HashMap<>();
    private final List<int[]> cells = new ArrayList<>();
    private final Map<Integer, VoxelShape> shapes = new ConcurrentHashMap<>();

    MultiblockKind(String key, Supplier<Block> master) {
        this.key = key;
        this.master = master;
        String data = StructureShapes.DATA.get(key);
        for (String cell : data.split(";")) {
            String[] parts = cell.split(":");
            String[] c = parts[0].split(",");
            int cx = Integer.parseInt(c[0]), cy = Integer.parseInt(c[1]), cz = Integer.parseInt(c[2]);
            String[] list = parts[1].split("\\|");
            double[][] bs = new double[list.length][];
            for (int i = 0; i < list.length; i++) {
                String[] v = list[i].trim().split(" ");
                bs[i] = new double[6];
                for (int k = 0; k < 6; k++) bs[i][k] = Double.parseDouble(v[k]);
            }
            boxes.put(pack(cx, cy, cz), bs);
            if (cx != 0 || cy != 0 || cz != 0) cells.add(new int[]{cx, cy, cz});
        }
    }

    public String key() {
        return key;
    }

    public Block masterBlock() {
        return master.get();
    }

    @Override
    public String getSerializedName() {
        return key;
    }

    public static MultiblockKind of(Block block) {
        for (MultiblockKind k : values()) {
            if (k.masterBlock() == block) return k;
        }
        return null;
    }

    /** Cellules (decalages vers le nord) qui recoivent un bloc de structure. */
    public List<int[]> cells() {
        return cells;
    }

    private static long pack(int x, int y, int z) {
        return ((long) (x + 64) << 16) | ((long) (y + 64) << 8) | (z + 64);
    }

    /** Tourne un decalage (structure vers le nord) selon l'orientation : la face avant regarde vers {@code facing}. */
    public static BlockPos rotate(int dx, int dy, int dz, Direction facing) {
        return switch (facing) {
            case EAST -> new BlockPos(-dz, dy, dx);
            case SOUTH -> new BlockPos(-dx, dy, -dz);
            case WEST -> new BlockPos(dz, dy, -dx);
            default -> new BlockPos(dx, dy, dz);
        };
    }

    public BlockPos cellPos(BlockPos master, int[] cell, Direction facing) {
        return master.offset(rotate(cell[0], cell[1], cell[2], facing));
    }

    /** Collision d'une cellule (decalage vers le nord) pour une orientation donnee. */
    public VoxelShape shape(int dx, int dy, int dz, Direction facing) {
        int k = ((dx + 8) << 12) | ((dy + 8) << 8) | ((dz + 8) << 4) | facing.get2DDataValue();
        return shapes.computeIfAbsent(k, i -> build(boxes.get(pack(dx, dy, dz)), facing));
    }

    private static VoxelShape build(double[][] bs, Direction facing) {
        if (bs == null) return Shapes.empty();
        VoxelShape s = Shapes.empty();
        for (double[] b : bs) {
            double x0 = b[0], z0 = b[2], x1 = b[3], z1 = b[5];
            double[] r = switch (facing) {
                case EAST -> new double[]{16 - z1, 16 - z0, x0, x1};
                case SOUTH -> new double[]{16 - x1, 16 - x0, 16 - z1, 16 - z0};
                case WEST -> new double[]{z0, z1, 16 - x1, 16 - x0};
                default -> new double[]{x0, x1, z0, z1};
            };
            s = Shapes.or(s, Shapes.box(r[0] / 16, b[1] / 16, r[2] / 16, r[1] / 16, b[4] / 16, r[3] / 16));
        }
        return s.optimize();
    }

    /** Collision du bloc fonctionnel ; un bloc plein si le modele n'en definit pas. */
    public VoxelShape masterShape(Direction facing) {
        VoxelShape s = shape(0, 0, 0, facing);
        return s.isEmpty() ? Shapes.block() : s;
    }
}
