package com.mmorpg.hub;

import com.mmorpg.entity.dummy.TrainingDummyEntity;
import com.mmorpg.entity.hub.ClassMasterEntity;
import com.mmorpg.registry.ModBlocks;
import com.mmorpg.registry.ModEntities;
import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.world.WaypointData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.UUID;

/**
 * Construction du hub d'entree (/mmorpg hub) : place centrale, quatre zones de classe, salle d'epreuve, aire d'entrainement,
 * porte sud et Teleporteur. Reconstruire le hub au meme endroit remplace ses PNJ et mannequins sans les dupliquer.
 */
public final class HubBuilder {
    private HubBuilder() {
    }

    private static BlockPos O;
    private static ServerLevel L;

    private static void set(int x, int y, int z, Block b) {
        L.setBlock(O.offset(x, y, z), b.defaultBlockState(), Block.UPDATE_CLIENTS);
    }

    private static void set(int x, int y, int z, BlockState s) {
        L.setBlock(O.offset(x, y, z), s, Block.UPDATE_CLIENTS);
    }

    private static void fill(int x0, int y0, int z0, int x1, int y1, int z1, Block b) {
        BlockState s = b.defaultBlockState();
        for (int x = Math.min(x0, x1); x <= Math.max(x0, x1); x++)
            for (int y = Math.min(y0, y1); y <= Math.max(y0, y1); y++)
                for (int z = Math.min(z0, z1); z <= Math.max(z0, z1); z++)
                    L.setBlock(O.offset(x, y, z), s, Block.UPDATE_CLIENTS);
    }

    /** Construit le hub avec sa place centrale au niveau {@code feet} (sol en dessous) et renvoie le centre. */
    public static BlockPos build(ServerLevel level, BlockPos feet) {
        L = level;
        O = feet.immutable();
        removeOld();
        terrain();
        plaza();
        for (PlayerClass c : new PlayerClass[]{PlayerClass.GUERRIER, PlayerClass.MAGE, PlayerClass.ARCHER, PlayerClass.TANK}) dais(c);
        gate();
        teleporter();
        trialRoom();
        yard();
        entities();
        BlockPos spawn = HubLayout.pos(O, HubLayout.SPAWN);
        level.setRespawnData(new LevelData.RespawnData(GlobalPos.of(level.dimension(), spawn), 180F, 0F));
        HubData.get(level.getServer()).set(level.dimension(), O);
        BlockPos result = O;
        L = null;
        O = null;
        return result;
    }

    // ================================================================== terrain

    private static boolean inFootprint(int x, int z) {
        if (x * x + z * z <= 19 * 19) return true;                                             // place
        if (Math.abs(x) <= 4 && z >= 16 && z <= 26) return true;                               // porte et parvis sud
        if (Math.abs(x) <= HubLayout.ROOM_HALF + 1 && z >= HubLayout.ROOM_Z0 - 1 && z <= HubLayout.ROOM_Z1 + 1) return true;   // salle
        if (Math.abs(x) <= 3 && z >= HubLayout.ROOM_Z1 && z <= -15) return true;               // couloir de la salle
        if (x >= 16 && x <= HubLayout.YARD_X1 + 1 && Math.abs(z) <= HubLayout.YARD_HALF + 1) return true;   // aire d'entrainement
        return false;
    }

    /** Aplanit l'emprise : sol plein sous la place, air au-dessus (arbres et reliefs retires). */
    private static void terrain() {
        BlockState air = Blocks.AIR.defaultBlockState();
        BlockState fillState = Blocks.STONE_BRICKS.defaultBlockState();
        for (int x = -24; x <= HubLayout.YARD_X1 + 3; x++) {
            for (int z = HubLayout.ROOM_Z0 - 3; z <= 29; z++) {
                if (!inFootprint(x, z)) continue;
                for (int y = -9; y <= -2; y++) L.setBlock(O.offset(x, y, z), fillState, Block.UPDATE_CLIENTS);
                for (int y = 0; y <= 30; y++) L.setBlock(O.offset(x, y, z), air, Block.UPDATE_CLIENTS);
            }
        }
    }

    // ================================================================== place

    private static void plaza() {
        for (int x = -19; x <= 19; x++) {
            for (int z = -19; z <= 19; z++) {
                double r = Math.sqrt(x * x + z * z);
                if (r > 18.5) continue;
                Block b;
                if (r < 2.5) b = Blocks.CHISELED_QUARTZ_BLOCK;
                else if (r < 3.5) b = Blocks.GOLD_BLOCK;
                else if (r < 6.5) b = Blocks.SMOOTH_QUARTZ;
                else if (r > 17.5) b = Blocks.POLISHED_BLACKSTONE_BRICKS;
                else if (Math.abs(Math.abs(x) - Math.abs(z)) <= 1 && r > 6.5) b = Blocks.POLISHED_ANDESITE;   // allees diagonales vers les zones de classe
                else if (Math.abs(x) <= 1 || Math.abs(z) <= 1) b = Blocks.POLISHED_ANDESITE;                  // allees cardinales
                else b = (x + z) % 2 == 0 ? Blocks.STONE_BRICKS : Blocks.SMOOTH_STONE;
                set(x, -1, z, b);
            }
        }
        // petit mur de bordure et lanternes
        for (int a = 0; a < 360; a += 3) {
            int x = (int) Math.round(Math.cos(Math.toRadians(a)) * 18);
            int z = (int) Math.round(Math.sin(Math.toRadians(a)) * 18);
            if (Math.abs(x) <= 3 && z >= 14) continue;                // porte sud
            if (Math.abs(x) <= 3 && z <= -14) continue;               // couloir nord
            if (x >= 14 && Math.abs(z) <= 3) continue;                // acces est
            set(x, 0, z, Blocks.STONE_BRICK_WALL);
            if (a % 15 == 0) {
                set(x, 1, z, Blocks.STONE_BRICK_WALL);
                set(x, 2, z, Blocks.LANTERN);
            }
        }
        // puits central decoratif : couronne de lumiere autour du Maitre des classes
        for (int[] c : new int[][]{{3, 3}, {-3, 3}, {3, -3}, {-3, -3}}) {
            set(c[0], 0, c[1], Blocks.QUARTZ_PILLAR);
            set(c[0], 1, c[1], Blocks.QUARTZ_PILLAR);
            set(c[0], 2, c[1], Blocks.SEA_LANTERN);
        }
    }

    // ================================================================== zones de classe

    private static void dais(PlayerClass cls) {
        int[] s = HubLayout.classSpot(cls);
        int cx = s[0];
        int cz = s[2];
        Block base, accent, pillar, top, deco1, deco2;
        switch (cls) {
            case GUERRIER -> {
                base = Blocks.POLISHED_BLACKSTONE_BRICKS;
                accent = Blocks.RED_NETHER_BRICKS;
                pillar = Blocks.NETHER_BRICKS;
                top = Blocks.SHROOMLIGHT;
                deco1 = Blocks.ANVIL;
                deco2 = Blocks.GRINDSTONE;
            }
            case MAGE -> {
                base = Blocks.PURPUR_BLOCK;
                accent = Blocks.AMETHYST_BLOCK;
                pillar = Blocks.PURPUR_PILLAR;
                top = Blocks.END_ROD;
                deco1 = Blocks.ENCHANTING_TABLE;
                deco2 = Blocks.AMETHYST_CLUSTER;
            }
            case ARCHER -> {
                base = Blocks.MOSSY_STONE_BRICKS;
                accent = Blocks.OAK_PLANKS;
                pillar = Blocks.STRIPPED_OAK_LOG;
                top = Blocks.LANTERN;
                deco1 = Blocks.FLETCHING_TABLE;
                deco2 = Blocks.HAY_BLOCK;
            }
            default -> {
                base = Blocks.QUARTZ_BRICKS;
                accent = Blocks.BLUE_ICE;
                pillar = Blocks.QUARTZ_PILLAR;
                top = Blocks.SEA_LANTERN;
                deco1 = Blocks.SMITHING_TABLE;
                deco2 = Blocks.IRON_BLOCK;
            }
        }
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                boolean edge = Math.abs(x) == 4 || Math.abs(z) == 4;
                set(cx + x, -1, cz + z, edge ? accent : ((x + z) % 2 == 0 ? base : accent));
            }
        }
        fill(cx - 3, 0, cz - 3, cx + 3, 0, cz + 3, base);
        fill(cx - 2, 0, cz - 2, cx + 2, 0, cz + 2, accent);
        fill(cx - 1, 0, cz - 1, cx + 1, 0, cz + 1, base);                    // piedestal du PNJ (le PNJ se tient a y = 1)
        for (int[] c : new int[][]{{-4, -4}, {4, -4}, {-4, 4}, {4, 4}}) {
            for (int y = 0; y <= 5; y++) set(cx + c[0], y, cz + c[1], pillar);
            set(cx + c[0], 6, cz + c[1], top);
        }
        set(cx - 3, 1, cz, deco1);
        set(cx + 3, 1, cz, deco2 == Blocks.AMETHYST_CLUSTER ? Blocks.AMETHYST_BLOCK : deco2);
        if (deco2 == Blocks.AMETHYST_CLUSTER) set(cx + 3, 2, cz, Blocks.AMETHYST_CLUSTER);
    }

    // ================================================================== porte sud et teleporteur

    private static void gate() {
        for (int x = -4; x <= 4; x++) for (int z = 17; z <= 24; z++) {
            boolean path = Math.abs(x) <= 1;
            set(x, -1, z, path ? Blocks.POLISHED_ANDESITE : (Math.abs(x) == 4 ? Blocks.POLISHED_BLACKSTONE_BRICKS : Blocks.STONE_BRICKS));
        }
        for (int sx : new int[]{-4, 4}) {
            for (int y = 0; y <= 7; y++) {
                set(sx, y, 18, Blocks.POLISHED_BLACKSTONE_BRICKS);
                set(sx, y, 19, Blocks.POLISHED_BLACKSTONE_BRICKS);
            }
            set(sx, 8, 18, Blocks.SEA_LANTERN);
            set(sx, 8, 19, Blocks.SEA_LANTERN);
        }
        for (int x = -4; x <= 4; x++) {
            set(x, 7, 18, Blocks.GOLD_BLOCK);
            set(x, 7, 19, Blocks.POLISHED_BLACKSTONE_BRICKS);
        }
    }

    private static void teleporter() {
        BlockPos tp = HubLayout.pos(O, HubLayout.TELEPORTER);
        for (int x = -3; x <= 3; x++) for (int z = 10; z <= 16; z++) set(x, -1, z, Blocks.CHISELED_STONE_BRICKS);
        L.setBlock(tp, ModBlocks.TELEPORTEUR.get().defaultBlockState(), Block.UPDATE_ALL);
        WaypointData data = WaypointData.get(L.getServer());
        data.removeAt(L.dimension(), tp);
        data.create(L.dimension(), tp, "Hub d'Eldoria", WaypointData.CITY, 1, new UUID(0, 0));
    }

    // ================================================================== salle d'epreuve

    private static void trialRoom() {
        int h = HubLayout.ROOM_HEIGHT;
        int z0 = HubLayout.ROOM_Z0;
        int z1 = HubLayout.ROOM_Z1;
        int hw = HubLayout.ROOM_HALF;
        for (int x = -hw; x <= hw; x++) {
            for (int z = z0; z <= z1; z++) {
                boolean wall = Math.abs(x) == hw || z == z0 || z == z1;
                set(x, -1, z, ((x + z) & 1) == 0 ? Blocks.POLISHED_DEEPSLATE : Blocks.DEEPSLATE_TILES);
                for (int y = 0; y < h; y++) set(x, y, z, wall ? (y % 3 == 2 ? Blocks.POLISHED_BLACKSTONE_BRICKS : Blocks.STONE_BRICKS) : Blocks.AIR);
                set(x, h, z, Blocks.DEEPSLATE_BRICKS);
            }
        }
        // anneau d'entrainement au sol et lumieres
        for (int x = -hw + 1; x <= hw - 1; x++) for (int z = z0 + 1; z <= z1 - 1; z++) {
            double r = Math.hypot(x, z - (z0 + z1) / 2.0);
            if (Math.abs(r - 12) < 0.6) set(x, -1, z, Blocks.GOLD_BLOCK);
        }
        for (int z = z0 + 3; z <= z1 - 3; z += 5) {
            set(-hw + 1, 6, z, Blocks.LANTERN);
            set(hw - 1, 6, z, Blocks.LANTERN);
            for (int y = 0; y < 6; y++) { set(-hw + 1, y, z, Blocks.STONE_BRICK_WALL); set(hw - 1, y, z, Blocks.STONE_BRICK_WALL); }
        }
        for (int x = -10; x <= 10; x += 5) for (int z = z0 + 4; z <= z1 - 4; z += 6) set(x, h - 1, z, Blocks.SEA_LANTERN);
        // porte au milieu du mur sud et couloir vers la place
        for (int x = -2; x <= 2; x++) for (int y = 0; y <= 4; y++) set(x, y, z1, Blocks.AIR);
        for (int z = z1 + 1; z <= -15; z++) {
            for (int x = -3; x <= 3; x++) {
                set(x, -1, z, Math.abs(x) <= 1 ? Blocks.POLISHED_ANDESITE : Blocks.STONE_BRICKS);
                for (int y = 0; y <= 6; y++) set(x, y, z, Math.abs(x) == 3 && y <= 3 ? Blocks.STONE_BRICK_WALL : Blocks.AIR);
            }
        }
        for (int x : new int[]{-3, 3}) set(x, 4, z1, Blocks.LANTERN);
        // portique d'entree
        for (int x = -3; x <= 3; x++) set(x, 5, z1, x == -3 || x == 3 ? Blocks.CHISELED_STONE_BRICKS : Blocks.GOLD_BLOCK);
    }

    // ================================================================== aire d'entrainement

    private static void yard() {
        int x0 = HubLayout.YARD_X0;
        int x1 = HubLayout.YARD_X1;
        int hz = HubLayout.YARD_HALF;
        for (int x = 16; x <= x1; x++) for (int z = -hz; z <= hz; z++) {
            boolean path = x < x0 + 1 && Math.abs(z) <= 1;
            set(x, -1, z, path ? Blocks.POLISHED_ANDESITE : ((x + z) % 3 == 0 ? Blocks.COARSE_DIRT : Blocks.PACKED_MUD));
        }
        for (int x = x0; x <= x1; x++) {
            for (int z : new int[]{-hz, hz}) { set(x, 0, z, Blocks.OAK_FENCE); if ((x - x0) % 6 == 0) { set(x, 1, z, Blocks.OAK_FENCE); set(x, 2, z, Blocks.LANTERN); } }
        }
        for (int z = -hz; z <= hz; z++) {
            if (Math.abs(z) <= 1) continue;                      // entree par l'ouest
            set(x0, 0, z, Blocks.OAK_FENCE);
            set(x1, 0, z, Blocks.OAK_FENCE);
        }
        for (int z : new int[]{-8, 8}) {                          // bottes de paille decoratives
            set(x0 + 2, 0, z, Blocks.HAY_BLOCK);
            set(x0 + 3, 0, z, Blocks.HAY_BLOCK);
        }
    }

    // ================================================================== PNJ et mannequins

    private static void removeOld() {
        AABB box = HubLayout.hubBounds(O);
        for (Entity e : L.getEntities((Entity) null, box, x -> x instanceof ClassMasterEntity || x instanceof TrainingDummyEntity
                || (x instanceof com.mmorpg.entity.NpcEntity n && "maitre_classes".equals(n.group())))) {
            e.discard();
        }
    }

    private static void entities() {
        // Maitre des classes : evolutions (quetes) et dialogue general
        var master = com.mmorpg.world.DungeonBuilder.spawnNpc(L, HubLayout.pos(O, HubLayout.CLASS_MASTER), com.mmorpg.entity.NpcEntity.Role.QUETES,
                "Maître des classes", 2, 0F);
        if (master != null) master.setup(com.mmorpg.entity.NpcEntity.Role.QUETES, "Maître des classes", 2, "maitre_classes");
        // Maitres de classe : sur leur piedestal
        for (int k = 0; k < 4; k++) {
            PlayerClass cls = new PlayerClass[]{PlayerClass.GUERRIER, PlayerClass.MAGE, PlayerClass.ARCHER, PlayerClass.TANK}[k];
            int[] s = HubLayout.classSpot(cls);
            spawnMaster(k, s[0], 1, s[2], HubLayout.classYaw(cls));
        }
        spawnMaster(ClassMasterEntity.EPREUVE, HubLayout.TRIAL_MASTER[0], 0, HubLayout.TRIAL_MASTER[2], 90F);
        for (int[] d : HubLayout.DUMMIES_ROOM) spawnDummy(d);
        for (int[] d : HubLayout.DUMMIES_YARD) spawnDummy(d);
    }

    private static void spawnMaster(int kind, int x, int y, int z, float yaw) {
        ClassMasterEntity npc = ModEntities.CLASS_MASTER.get().create(L, EntitySpawnReason.COMMAND);
        if (npc == null) return;
        npc.snapTo(O.getX() + x + 0.5, O.getY() + y, O.getZ() + z + 0.5, yaw, 0F);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        npc.setup(kind);
        L.addFreshEntity(npc);
    }

    private static void spawnDummy(int[] d) {
        TrainingDummyEntity e = ModEntities.TRAINING_DUMMY.get().create(L, EntitySpawnReason.COMMAND);
        if (e == null) return;
        e.snapTo(O.getX() + d[0] + 0.5, O.getY(), O.getZ() + d[1] + 0.5, d[3], 0F);
        e.setup(d[2]);
        L.addFreshEntity(e);
    }
}
