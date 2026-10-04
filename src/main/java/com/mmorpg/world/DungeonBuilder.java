package com.mmorpg.world;

import com.mmorpg.registry.ModBlocks;
import com.mmorpg.registry.ModEntities;
import com.mmorpg.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Generation de donjons thematiques et de places de ville, relies au reseau de teleportation. */
public final class DungeonBuilder {
    private DungeonBuilder() {
    }

    public enum DungeonType {
        CRYPTE_GOBELINE("crypte_gobeline", "Crypte Gobeline", 15, Blocks.MOSSY_COBBLESTONE, Blocks.COBBLESTONE, Blocks.OAK_PLANKS, Blocks.LANTERN, Blocks.OAK_LOG,
                () -> List.of(ModEntities.GOBLIN.get(), ModEntities.DARK_WOLF.get()), "sceau_roi_gobelin",
                List.of("oreille_gobelin", "ferraille_gobeline", "croc_loup")),
        CATACOMBES("catacombes", "Catacombes de la Liche", 35, Blocks.DEEPSLATE_BRICKS, Blocks.DEEPSLATE_TILES, Blocks.CRACKED_DEEPSLATE_BRICKS, Blocks.SOUL_LANTERN, Blocks.POLISHED_DEEPSLATE,
                () -> List.of(ModEntities.CURSED_SKELETON.get(), ModEntities.ORC.get()), "grimoire_interdit",
                List.of("os_maudit", "poussiere_ame", "acier_orc")),
        FORTERESSE_INFERNALE("forteresse_infernale", "Forteresse Infernale", 55, Blocks.NETHER_BRICKS, Blocks.BLACKSTONE, Blocks.MAGMA_BLOCK, Blocks.SHROOMLIGHT, Blocks.POLISHED_BLACKSTONE_BRICKS,
                () -> List.of(ModEntities.FIRE_ELEMENTAL.get(), ModEntities.ORC.get()), "braise_eternelle",
                List.of("noyau_flamme", "defense_orc", "acier_orc")),
        SANCTUAIRE_GLACE("sanctuaire_glace", "Sanctuaire Glacé", 70, Blocks.PACKED_ICE, Blocks.SNOW_BLOCK, Blocks.BLUE_ICE, Blocks.SEA_LANTERN, Blocks.CALCITE,
                () -> List.of(ModEntities.ICE_WRAITH.get(), ModEntities.CRYSTAL_GOLEM.get()), "cristal_glacial",
                List.of("eclat_givre", "cristal_arcanique")),
        CITADELLE_NEANT("citadelle_neant", "Citadelle du Néant", 90, Blocks.OBSIDIAN, Blocks.END_STONE_BRICKS, Blocks.PURPUR_BLOCK, Blocks.END_ROD, Blocks.CRYING_OBSIDIAN,
                () -> List.of(ModEntities.VOID_KNIGHT.get(), ModEntities.CRYSTAL_GOLEM.get()), "oeil_neant",
                List.of("essence_neant", "cristal_arcanique"));

        public final String id;
        public final String label;
        public final int level;
        final Block wall;
        final Block floor;
        final Block accent;
        final Block light;
        final Block pillar;
        final Supplier<List<EntityType<?>>> mobs;
        final String bossKey;
        final List<String> loot;

        DungeonType(String id, String label, int level, Block wall, Block floor, Block accent, Block light, Block pillar,
                    Supplier<List<EntityType<?>>> mobs, String bossKey, List<String> loot) {
            this.id = id;
            this.label = label;
            this.level = level;
            this.wall = wall;
            this.floor = floor;
            this.accent = accent;
            this.light = light;
            this.pillar = pillar;
            this.mobs = mobs;
            this.bossKey = bossKey;
            this.loot = loot;
        }

        public static DungeonType byId(String id) {
            for (DungeonType t : values()) if (t.id.equalsIgnoreCase(id)) return t;
            return null;
        }
    }

    private static void set(ServerLevel level, BlockPos p, BlockState s) {
        level.setBlock(p, s, Block.UPDATE_CLIENTS);
    }

    /** Salle creuse : sol, murs, plafond ; interieur vide. Centree en x sur ox, commencant a oz. */
    private static void room(ServerLevel level, BlockPos origin, int halfW, int h, int len, DungeonType t, RandomSource r) {
        for (int x = -halfW; x <= halfW; x++) {
            for (int z = 0; z < len; z++) {
                for (int y = 0; y <= h; y++) {
                    BlockPos p = origin.offset(x, y, z);
                    boolean edge = Math.abs(x) == halfW || z == 0 || z == len - 1;
                    if (y == 0) {
                        set(level, p, (r.nextInt(8) == 0 ? t.accent : t.floor).defaultBlockState());
                    } else if (y == h) {
                        set(level, p, t.wall.defaultBlockState());
                    } else if (edge) {
                        set(level, p, (r.nextInt(10) == 0 ? t.accent : t.wall).defaultBlockState());
                    } else {
                        set(level, p, Blocks.AIR.defaultBlockState());
                    }
                }
            }
        }
        // lumieres sur les murs
        for (int z = 2; z < len - 2; z += 4) {
            set(level, origin.offset(-halfW + 1, h - 1, z), t.light.defaultBlockState());
            set(level, origin.offset(halfW - 1, h - 1, z), t.light.defaultBlockState());
        }
    }

    /** Ouverture dans un mur (porte) de largeur 3 et hauteur 4. */
    private static void door(ServerLevel level, BlockPos wallCenter) {
        for (int x = -1; x <= 1; x++) {
            for (int y = 1; y <= 4; y++) {
                set(level, wallCenter.offset(x, y, 0), Blocks.AIR.defaultBlockState());
            }
        }
    }

    private static void pillar(ServerLevel level, BlockPos base, int h, DungeonType t) {
        for (int y = 1; y < h; y++) set(level, base.above(y), t.pillar.defaultBlockState());
    }

    private static void spawner(ServerLevel level, BlockPos p, EntityType<?> type, RandomSource r) {
        set(level, p, Blocks.SPAWNER.defaultBlockState());
        BlockEntity be = level.getBlockEntity(p);
        if (be instanceof SpawnerBlockEntity spawner) {
            spawner.setEntityId(type, r);
            spawner.setChanged();
        }
    }

    private static void chest(ServerLevel level, BlockPos p, List<ItemStack> items, RandomSource r) {
        set(level, p, Blocks.CHEST.defaultBlockState());
        if (level.getBlockEntity(p) instanceof ChestBlockEntity chest) {
            for (ItemStack s : items) {
                int slot = r.nextInt(27);
                for (int i = 0; i < 27 && !chest.getItem(slot).isEmpty(); i++) slot = (slot + 1) % 27;
                chest.setItem(slot, s);
            }
            chest.setChanged();
        }
    }

    private static List<ItemStack> roomLoot(DungeonType t, RandomSource r, boolean boss) {
        List<ItemStack> l = new ArrayList<>();
        l.add(new ItemStack(ModItems.PIECE_OR.get(), 5 + r.nextInt(10) + t.level / 3));
        for (String id : t.loot) {
            if (r.nextBoolean()) l.add(new ItemStack(ModItems.get(id), 1 + r.nextInt(3)));
        }
        l.add(new ItemStack(t.level >= 50 ? ModItems.POTION_SOIN_MAJEURE.get() : t.level >= 20 ? ModItems.POTION_SOIN.get() : ModItems.POTION_SOIN_MINEURE.get(), 1 + r.nextInt(3)));
        l.add(new ItemStack(t.level >= 50 ? ModItems.POTION_MANA_MAJEURE.get() : t.level >= 20 ? ModItems.POTION_MANA.get() : ModItems.POTION_MANA_MINEURE.get(), 1 + r.nextInt(2)));
        if (r.nextInt(3) == 0) l.add(new ItemStack(ModItems.PIERRE_AMELIORATION.get(), 1));
        if (boss) {
            l.add(new ItemStack(ModItems.get(t.bossKey), 1));
            l.add(new ItemStack(ModItems.PARCHEMIN_TELEPORTATION.get(), 2));
        }
        return l;
    }

    /**
     * Construit un donjon a partir de {@code origin} (vers le sud, +Z) et enregistre son teleporteur d'entree.
     * @return la position du teleporteur
     */
    public static BlockPos buildDungeon(ServerLevel level, BlockPos origin, DungeonType t) {
        RandomSource r = level.getRandom();
        List<EntityType<?>> mobs = t.mobs.get();
        BlockPos o = origin.below();
        // hall d'entree
        room(level, o, 5, 7, 11, t, r);
        BlockPos tp = o.offset(0, 1, 5);
        set(level, tp, ModBlocks.TELEPORTEUR.get().defaultBlockState());
        for (int[] c : new int[][]{{-3, 2}, {3, 2}, {-3, 8}, {3, 8}}) pillar(level, o.offset(c[0], 0, c[1]), 7, t);
        int z = 11;
        // couloir 1 + salle 1
        door(level, o.offset(0, 0, z - 1));
        room(level, o.offset(0, 0, z - 1), 2, 5, 9, t, r);
        z += 8;
        door(level, o.offset(0, 0, z - 1));
        room(level, o.offset(0, 0, z - 1), 7, 7, 15, t, r);
        door(level, o.offset(0, 0, z - 1));
        spawner(level, o.offset(0, 1, z + 6), mobs.get(0), r);
        chest(level, o.offset(5, 1, z + 12), roomLoot(t, r, false), r);
        z += 14;
        // couloir 2 + salle 2
        door(level, o.offset(0, 0, z - 1));
        room(level, o.offset(0, 0, z - 1), 2, 5, 9, t, r);
        door(level, o.offset(0, 0, z - 1));
        z += 8;
        door(level, o.offset(0, 0, z - 1));
        room(level, o.offset(0, 0, z - 1), 7, 7, 15, t, r);
        door(level, o.offset(0, 0, z - 1));
        spawner(level, o.offset(-4, 1, z + 4), mobs.get(0), r);
        spawner(level, o.offset(4, 1, z + 9), mobs.get(mobs.size() > 1 ? 1 : 0), r);
        chest(level, o.offset(-5, 1, z + 12), roomLoot(t, r, false), r);
        z += 14;
        // couloir 3 + salle du boss
        door(level, o.offset(0, 0, z - 1));
        room(level, o.offset(0, 0, z - 1), 2, 5, 9, t, r);
        door(level, o.offset(0, 0, z - 1));
        z += 8;
        door(level, o.offset(0, 0, z - 1));
        room(level, o.offset(0, 0, z - 1), 11, 12, 23, t, r);
        door(level, o.offset(0, 0, z - 1));
        for (int[] c : new int[][]{{-7, 4}, {7, 4}, {-7, 16}, {7, 16}}) pillar(level, o.offset(c[0], 0, z + c[1]), 12, t);
        BlockPos altar = o.offset(0, 1, z + 13);
        set(level, altar, ModBlocks.AUTEL_INVOCATION.get().defaultBlockState());
        for (int x = -2; x <= 2; x++) {
            for (int zz = -2; zz <= 2; zz++) {
                if (Math.abs(x) == 2 || Math.abs(zz) == 2) set(level, altar.offset(x, -1, zz), t.accent.defaultBlockState());
            }
        }
        chest(level, o.offset(0, 1, z + 20), roomLoot(t, r, true), r);
        // enregistrement du teleporteur
        WaypointData data = WaypointData.get(level.getServer());
        data.removeAt(level.dimension(), tp);
        data.create(level.dimension(), tp, t.label, WaypointData.DUNGEON, t.level, new UUID(0, 0));
        return tp;
    }

    /** Petite place de ville avec teleporteur central enregistre comme Ville. */
    public static BlockPos buildCity(ServerLevel level, BlockPos origin, String name) {
        BlockPos o = origin.below();
        for (int x = -8; x <= 8; x++) {
            for (int z = -8; z <= 8; z++) {
                boolean border = Math.abs(x) == 8 || Math.abs(z) == 8;
                boolean ring = Math.abs(x) <= 2 && Math.abs(z) <= 2;
                set(level, o.offset(x, 0, z), (border ? Blocks.POLISHED_ANDESITE : ring ? Blocks.CHISELED_STONE_BRICKS : ((x + z) % 2 == 0 ? Blocks.STONE_BRICKS : Blocks.POLISHED_ANDESITE)).defaultBlockState());
                for (int y = 1; y <= 5; y++) set(level, o.offset(x, y, z), Blocks.AIR.defaultBlockState());
            }
        }
        for (int[] c : new int[][]{{-6, -6}, {6, -6}, {-6, 6}, {6, 6}}) {
            set(level, o.offset(c[0], 1, c[1]), Blocks.STONE_BRICK_WALL.defaultBlockState());
            set(level, o.offset(c[0], 2, c[1]), Blocks.STONE_BRICK_WALL.defaultBlockState());
            set(level, o.offset(c[0], 3, c[1]), Blocks.LANTERN.defaultBlockState());
        }
        BlockPos tp = o.above();
        set(level, tp, ModBlocks.TELEPORTEUR.get().defaultBlockState());
        WaypointData data = WaypointData.get(level.getServer());
        data.removeAt(level.dimension(), tp);
        data.create(level.dimension(), tp, name, WaypointData.CITY, 1, new UUID(0, 0));
        spawnNpc(level, o.offset(-4, 1, 0), com.mmorpg.entity.NpcEntity.Role.QUETES, "Aldric le Sage", 0, 270);
        spawnNpc(level, o.offset(4, 1, 0), com.mmorpg.entity.NpcEntity.Role.MARCHAND, "Brunhilde la Marchande", 1, 90);
        return tp;
    }

    /** Fait apparaitre un PNJ (Maitre des Quetes ou Marchand). */
    public static com.mmorpg.entity.NpcEntity spawnNpc(ServerLevel level, BlockPos pos, com.mmorpg.entity.NpcEntity.Role role, String name, int skin, float yaw) {
        com.mmorpg.entity.NpcEntity npc = ModEntities.NPC.get().create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        if (npc == null) return null;
        npc.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, yaw, 0);
        npc.setYHeadRot(yaw);
        npc.setup(role, name, skin, "");
        level.addFreshEntity(npc);
        return npc;
    }

}
