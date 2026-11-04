package com.mmorpg.dungeon;

import com.mmorpg.config.ConfigManager;
import com.mmorpg.registry.ModBlocks;
import com.mmorpg.world.DungeonBuilder.DungeonType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.util.ProblemReporter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Modele d'un donjon enregistre depuis son donjon de setup : tous les blocs de l'emprise (air compris), les generateurs de monstres
 * avec leur reglage et la position des coffres (leur contenu reste aleatoire, tire dans les tables de butin du donjon). Chaque
 * exemplaire construit ensuite recoit une copie du modele par-dessus la generation de base.
 */
public final class DungeonTemplate {
    static final int X0 = -Layout.HALF_WIDTH, X1 = Layout.HALF_WIDTH, Y0 = -11, Y1 = 36, Z0 = -4, Z1 = Layout.LENGTH + 2;
    /** Au-dela de cette profondeur, un coffre est un coffre du tresor du boss. */
    static final int TREASURE_FROM_Z = Theme.TREASURE_Z - 10;

    public record Chest(int x, int y, int z, boolean treasure) {
    }

    public record Spawner(int x, int y, int z, CompoundTag tag) {
    }

    public static final class Data {
        final BlockState[] palette;
        final int[] ids;
        public final List<Chest> chests = new ArrayList<>();
        final List<Spawner> spawners = new ArrayList<>();

        Data(BlockState[] palette, int[] ids) {
            this.palette = palette;
            this.ids = ids;
        }
    }

    private static final Map<String, Data> CACHE = new HashMap<>();
    private static final Map<String, Long> STAMPS = new HashMap<>();

    private DungeonTemplate() {
    }

    static Path file(DungeonType t) {
        return ConfigManager.dir().resolve("donjons").resolve(t.id + ".nbt");
    }

    public static boolean exists(DungeonType t) {
        return Files.exists(file(t));
    }

    /** Le modele du donjon, ou {@code null} s'il n'en existe pas (generation d'origine). */
    public static synchronized Data get(DungeonType t) {
        Path f = file(t);
        if (!Files.exists(f)) {
            CACHE.remove(t.id);
            return null;
        }
        try {
            long stamp = Files.getLastModifiedTime(f).toMillis() * 31 + Files.size(f);
            Data cached = CACHE.get(t.id);
            if (cached != null && STAMPS.getOrDefault(t.id, -1L) == stamp) return cached;
            Data d = read(NbtIo.readCompressed(f, net.minecraft.nbt.NbtAccounter.unlimitedHeap()));
            CACHE.put(t.id, d);
            STAMPS.put(t.id, stamp);
            return d;
        } catch (Exception e) {
            com.mmorpg.MMORPG.LOGGER.error("[MMORPG] Modele de donjon illisible : {}", f, e);
            return null;
        }
    }

    public static synchronized boolean delete(DungeonType t) {
        CACHE.remove(t.id);
        try {
            return Files.deleteIfExists(file(t));
        } catch (IOException e) {
            return false;
        }
    }

    // ------------------------------------------------------------------ enregistrement

    /** Photographie l'emprise du donjon dont l'origine de construction est {@code o} (voir StyledDungeon.Job). */
    public static synchronized int capture(ServerLevel level, BlockPos o, DungeonType t) throws IOException {
        List<BlockState> palette = new ArrayList<>();
        Map<BlockState, Integer> index = new HashMap<>();
        int w = X1 - X0 + 1, h = Y1 - Y0 + 1, d = Z1 - Z0 + 1;
        int[] ids = new int[w * h * d];
        ListTag chests = new ListTag();
        ListTag spawners = new ListTag();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        int n = 0;
        for (int x = X0; x <= X1; x++) {
            for (int y = Y0; y <= Y1; y++) {
                for (int z = Z0; z <= Z1; z++) {
                    p.set(o.getX() + x, o.getY() + y, o.getZ() + z);
                    BlockState s = level.getBlockState(p);
                    Integer id = index.get(s);
                    if (id == null) {
                        id = palette.size();
                        palette.add(s);
                        index.put(s, id);
                    }
                    ids[n++] = id;
                    if (s.is(ModBlocks.DUNGEON_CHEST.get()) || s.is(Blocks.CHEST)) {
                        CompoundTag c = new CompoundTag();
                        c.putInt("x", x); c.putInt("y", y); c.putInt("z", z);
                        c.putBoolean("treasure", z >= TREASURE_FROM_Z);
                        chests.add(c);
                    } else if (s.is(Blocks.SPAWNER) && level.getBlockEntity(p) instanceof SpawnerBlockEntity be) {
                        TagValueOutput out = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, level.registryAccess());
                        be.saveWithFullMetadata(out);
                        CompoundTag c = new CompoundTag();
                        c.putInt("x", x); c.putInt("y", y); c.putInt("z", z);
                        c.put("be", out.buildResult());
                        spawners.add(c);
                    }
                }
            }
        }
        CompoundTag root = new CompoundTag();
        ListTag pal = new ListTag();
        for (BlockState s : palette) pal.add(BlockState.CODEC.encodeStart(NbtOps.INSTANCE, s).getOrThrow());
        root.put("palette", pal);
        root.putIntArray("ids", ids);
        root.put("chests", chests);
        root.put("spawners", spawners);
        Path f = file(t);
        Files.createDirectories(f.getParent());
        Path tmp = f.resolveSibling(t.id + ".tmp");
        NbtIo.writeCompressed(root, tmp);
        Files.move(tmp, f, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        CACHE.remove(t.id);
        return chests.size() + spawners.size();
    }

    private static Data read(CompoundTag root) {
        ListTag pal = root.getListOrEmpty("palette");
        BlockState[] palette = new BlockState[pal.size()];
        for (int i = 0; i < palette.length; i++) {
            Tag tag = pal.get(i);
            palette[i] = BlockState.CODEC.parse(NbtOps.INSTANCE, tag).result().orElse(Blocks.AIR.defaultBlockState());
        }
        Data d = new Data(palette, root.getIntArray("ids").orElse(new int[0]));
        for (Tag tag : root.getListOrEmpty("chests")) {
            CompoundTag c = (CompoundTag) tag;
            d.chests.add(new Chest(c.getIntOr("x", 0), c.getIntOr("y", 0), c.getIntOr("z", 0), c.getBooleanOr("treasure", false)));
        }
        for (Tag tag : root.getListOrEmpty("spawners")) {
            CompoundTag c = (CompoundTag) tag;
            d.spawners.add(new Spawner(c.getIntOr("x", 0), c.getIntOr("y", 0), c.getIntOr("z", 0), c.getCompoundOrEmpty("be")));
        }
        return d;
    }

    // ------------------------------------------------------------------ copie dans un exemplaire

    public static final int PASTE_SLICE = 10;

    public static int slices() {
        return (Z1 - Z0 + PASTE_SLICE) / PASTE_SLICE;
    }

    /** Copie une tranche (en z) du modele ; seuls les blocs differents sont poses. */
    public static void paste(ServerLevel level, BlockPos o, Data d, int slice) {
        int w = X1 - X0 + 1, h = Y1 - Y0 + 1, depth = Z1 - Z0 + 1;
        if (d.ids.length != w * h * depth) return;
        int zFrom = Z0 + slice * PASTE_SLICE, zTo = Math.min(Z1, zFrom + PASTE_SLICE - 1);
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int x = X0; x <= X1; x++) {
            for (int y = Y0; y <= Y1; y++) {
                for (int z = zFrom; z <= zTo; z++) {
                    BlockState s = d.palette[d.ids[((x - X0) * h + (y - Y0)) * depth + (z - Z0)]];
                    p.set(o.getX() + x, o.getY() + y, o.getZ() + z);
                    if (level.getBlockState(p) != s) level.setBlock(p, s, Block.UPDATE_CLIENTS);
                }
            }
        }
    }

    /** Pose les generateurs du modele et remplit les coffres (apres la copie des blocs). */
    public static void finish(ServerLevel level, BlockPos o, Data d, DungeonType t, int bonus) {
        for (Spawner sp : d.spawners) {
            BlockPos at = o.offset(sp.x, sp.y, sp.z);
            if (level.getBlockEntity(at) instanceof SpawnerBlockEntity be) {
                be.loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), sp.tag));
                be.setChanged();
            }
        }
        fillChests(level, o, d, t, bonus);
    }

    public static void fillChests(ServerLevel level, BlockPos o, Data d, DungeonType t, int bonus) {
        var r = level.getRandom();
        for (Chest c : d.chests) {
            BlockPos at = o.offset(c.x, c.y, c.z);
            DungeonLoot.fill(level, at, c.treasure ? DungeonLoot.treasure(t, r, bonus) : DungeonLoot.room(t, r), r);
        }
    }
}
