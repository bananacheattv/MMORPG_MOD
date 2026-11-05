package com.mmorpg.block;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mmorpg.MMORPG;
import com.mmorpg.block.structure.MultiblockKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Collisions des decors 3D, un jeu de boites par bloc de l'emprise du modele (data/mmorpg/decor_shapes.json, genere par
 * tools/gen_decor_shapes.py). Cellule (0,0,0) = bloc pose ; modele tourne vers le nord ; pixels 0..16 par bloc.
 */
public final class DecorShapes {
    private static final Map<String, Map<BlockPos, double[][]>> DATA = load();
    private static final Map<String, VoxelShape> CACHE = new ConcurrentHashMap<>();

    private DecorShapes() {
    }

    private static Map<String, Map<BlockPos, double[][]>> load() {
        Map<String, Map<BlockPos, double[][]>> out = new HashMap<>();
        try (var in = DecorShapes.class.getResourceAsStream("/data/mmorpg/decor_shapes.json")) {
            if (in == null) return out;
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (var e : root.entrySet()) {
                Map<BlockPos, double[][]> cells = new HashMap<>();
                String text = e.getValue().getAsString();
                if (!text.isEmpty()) for (String cell : text.split(";")) {
                    String[] kv = cell.split(":");
                    String[] c = kv[0].split(",");
                    List<double[]> boxes = new ArrayList<>();
                    for (String b : kv[1].split("\\|")) {
                        String[] v = b.split(" ");
                        boxes.add(new double[]{Double.parseDouble(v[0]), Double.parseDouble(v[1]), Double.parseDouble(v[2]), Double.parseDouble(v[3]), Double.parseDouble(v[4]), Double.parseDouble(v[5])});
                    }
                    cells.put(new BlockPos(Integer.parseInt(c[0]), Integer.parseInt(c[1]), Integer.parseInt(c[2])), boxes.toArray(new double[0][]));
                }
                out.put(e.getKey(), cells);
            }
        } catch (Exception ex) {
            MMORPG.LOGGER.error("Collisions des decors illisibles", ex);
        }
        return out;
    }

    public static boolean has(String id) {
        return id != null && DATA.containsKey(id);
    }

    /** Cellules du modele, tourne vers le nord. */
    public static Iterable<BlockPos> cells(String id) {
        return DATA.getOrDefault(id, Map.of()).keySet();
    }

    /** Position (relative au bloc pose) d'une cellule pour une orientation donnee. */
    public static BlockPos toWorld(BlockPos cell, Direction facing) {
        return MultiblockKind.rotate(cell.getX(), cell.getY(), cell.getZ(), facing);
    }

    /** Inverse de {@link #toWorld}. */
    public static BlockPos toCell(BlockPos offset, Direction facing) {
        Direction back = facing.getAxis() == Direction.Axis.X ? facing.getOpposite() : facing;
        return MultiblockKind.rotate(offset.getX(), offset.getY(), offset.getZ(), back);
    }

    public static boolean hasCell(String id, BlockPos cell) {
        return DATA.getOrDefault(id, Map.of()).containsKey(cell);
    }

    /** Collision de la cellule, tournee selon l'orientation ; vide si la cellule n'existe pas. */
    public static VoxelShape shape(String id, BlockPos cell, Direction facing) {
        double[][] boxes = DATA.getOrDefault(id, Map.of()).get(cell);
        if (boxes == null) return Shapes.empty();
        return CACHE.computeIfAbsent(id + "/" + cell.getX() + "," + cell.getY() + "," + cell.getZ() + "/" + facing.get2DDataValue(), k -> build(boxes, facing));
    }

    private static VoxelShape build(double[][] boxes, Direction facing) {
        VoxelShape s = Shapes.empty();
        for (double[] b : boxes) {
            double ax = b[0], az = b[2], bx = b[3], bz = b[5];
            switch (facing) {
                case EAST -> { double t = ax; ax = 16 - az; az = t; t = bx; bx = 16 - bz; bz = t; }
                case SOUTH -> { ax = 16 - ax; az = 16 - az; bx = 16 - bx; bz = 16 - bz; }
                case WEST -> { double t = ax; ax = az; az = 16 - t; t = bx; bx = bz; bz = 16 - t; }
                default -> { }
            }
            s = Shapes.joinUnoptimized(s, Block.box(Math.min(ax, bx), b[1], Math.min(az, bz), Math.max(ax, bx), b[4], Math.max(az, bz)), BooleanOp.OR);
        }
        return s.optimize();
    }
}
