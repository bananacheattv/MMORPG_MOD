package com.mmorpg.hub;

import com.mmorpg.block.structure.MultiblockMasterBlock;
import com.mmorpg.entity.NpcEntity;
import com.mmorpg.registry.ModBlocks;
import com.mmorpg.registry.ModEntities;
import com.mmorpg.world.WaypointData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import java.util.UUID;

/**
 * Marche d'Aeloria : le quartier ouest de la Citadelle. Une place ronde autour d'une fontaine de cristal, la Banque au nord,
 * l'Hotel des ventes a l'ouest, les forges et la presse de compression au sud, quatre etals de marchands, les jardins des
 * cultivateurs, un teleporteur et les portails des mines et des donjons. Les coordonnees sont relatives a l'origine du hub.
 */
public final class HubMarket {
    private HubMarket() {
    }

    /** Centre du marche : une allee de 15 blocs de large le relie a la place des heros. */
    public static final int CX = -92;
    static final int RADIUS = 32;
    /** Rayon de l'ile du marche : la place (RADIUS) et, tout autour, la couronne des maisons, halles et tours. */
    static final int OUTER = 50;

    static boolean footprint(int x, int z) {
        return Math.hypot(x - CX, z) <= OUTER + 1.4 || (x <= -26 && x >= CX + OUTER - 8 && Math.abs(z) <= 7);
    }

    // ------------------------------------------------------------------ geometrie locale d'un batiment oriente vers le centre

    /** Point local (u : a droite, v : vers l'avant) d'un batiment centre en (cx, cz) et dont l'avant regarde (fx, fz). */
    private static int[] at(int cx, int cz, int fx, int fz, int u, int v) {
        return new int[]{cx - u * fz + v * fx, cz + u * fx + v * fz};
    }

    private static void put(HubCtx c, int cx, int cz, int fx, int fz, int u, int y, int v, Object block) {
        int[] p = at(cx, cz, fx, fz, u, v);
        c.set(p[0], y, p[1], c.state(block));
    }

    private static BlockState wool(int i) {
        return switch (i) {
            case 0 -> Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.RED).defaultBlockState();
            case 1 -> Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.BLUE).defaultBlockState();
            case 2 -> Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.GREEN).defaultBlockState();
            default -> Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.YELLOW).defaultBlockState();
        };
    }

    private static Direction dir(int fx, int fz) {
        return fx > 0 ? Direction.EAST : fx < 0 ? Direction.WEST : fz > 0 ? Direction.SOUTH : Direction.NORTH;
    }

    // ================================================================== construction

    static void build(HubCtx c) {
        floor(c);
        fountain(c);
        town(c);
        BlockPos o = c.O;
        // structures 3D : la Banque au nord (face au sud), l'Hotel des ventes a l'ouest (face a l'est), forges et presse au sud (face au nord)
        structure(c, ModBlocks.BANQUE.get(), CX, -25, Direction.SOUTH);
        structure(c, ModBlocks.HDV.get(), CX - 27, 0, Direction.EAST);
        structure(c, ModBlocks.PRESSE_COMPRESSION.get(), CX, 24, Direction.NORTH);
        structure(c, ModBlocks.FORGE_ARCANIQUE.get(), CX - 21, 25, Direction.NORTH);
        structure(c, ModBlocks.FORGE_ARCANIQUE.get(), CX + 22, 25, Direction.NORTH);
        BlockPos tp = o.offset(CX + 25, 0, 0);
        c.L.setBlock(tp, ModBlocks.TELEPORTEUR.get().defaultBlockState().setValue(MultiblockMasterBlock.FACING, Direction.WEST), Block.UPDATE_ALL);
        WaypointData data = WaypointData.get(c.L.getServer());
        data.removeAt(c.L.dimension(), tp);
        data.create(c.L.dimension(), tp, "Marché d'Aeloria", WaypointData.CITY, 1, new UUID(0, 0));
        stall(c, CX + 12, -13, 0, 1, 0);
        stall(c, CX + 12, 13, 0, -1, 1);
        stall(c, CX - 12, -13, 0, 1, 2);
        stall(c, CX - 12, 13, 0, -1, 3);
        gate(c, CX + 24, -18, -1, 0, true);
        gate(c, CX + 24, 19, -1, 0, false);
        farm(c, CX - 29, 17);
        plantings(c);
        avenue(c);
    }

    private static void structure(HubCtx c, Block block, int x, int z, Direction facing) {
        // la place des cellules de la structure est deja degagee par le terrain (air jusqu'a y = 34)
        c.L.setBlock(c.O.offset(x, 0, z), block.defaultBlockState().setValue(MultiblockMasterBlock.FACING, facing), Block.UPDATE_ALL);
    }

    // ------------------------------------------------------------------ sol, bordure et allee

    private static void floor(HubCtx c) {
        for (int x = CX - OUTER - 2; x <= CX + OUTER + 2; x++) {
            for (int z = -OUTER - 2; z <= OUTER + 2; z++) {
                double r = Math.hypot(x - CX, z);
                if (r > OUTER + 0.6) continue;
                if (r > RADIUS + 0.6) {                                                  // couronne des batiments : pave, terre et pelouse
                    c.set(x, -2, z, Blocks.DIRT);
                    c.set(x, -1, z, c.mix(x, 0, z, 43, Blocks.STONE_BRICKS, 26, Blocks.COBBLESTONE, 26, Blocks.GRASS_BLOCK, 24, Blocks.COARSE_DIRT, 10, Blocks.MOSSY_COBBLESTONE, 14));
                    for (int y = 0; y <= 6; y++) c.set(x, y, z, Blocks.AIR);
                    continue;
                }
                BlockState top;
                int dx = x - CX;
                boolean spoke = Math.abs(dx) <= 1 || Math.abs(z) <= 1 || Math.abs(Math.abs(dx) - Math.abs(z)) <= 1;
                if (r <= 8.4) top = c.state("pierre_azur");
                else if (r <= 10.6) top = (r > 9.2 && r <= 10.0) ? c.state("marbre_dore") : c.state("marbre_poli");
                else if (r > RADIUS - 1.4) top = c.state(Blocks.POLISHED_ANDESITE);
                else if (spoke) top = c.mix(x, 0, z, 41, Blocks.STONE_BRICKS, 40, Blocks.POLISHED_ANDESITE, 24, Blocks.ANDESITE, 14, Blocks.COBBLESTONE, 10, Blocks.TUFF_BRICKS, 12);
                else top = c.mix(x, 0, z, 42, Blocks.STONE_BRICKS, 30, Blocks.COBBLESTONE, 18, Blocks.ANDESITE, 14, Blocks.POLISHED_ANDESITE, 14, Blocks.GRAVEL, 6, Blocks.MOSSY_COBBLESTONE, 8, "marbre_poli", 10);
                c.set(x, -2, z, Blocks.STONE_BRICKS);
                c.set(x, -1, z, top);
                for (int y = 0; y <= 6; y++) c.set(x, y, z, Blocks.AIR);
            }
        }
        for (int a = 0; a < 360; a += 15) {                          // lampadaires de bois sur le pourtour de la place
            double rad = Math.toRadians(a);
            int x = CX + (int) Math.round(Math.cos(rad) * (RADIUS - 2.2));
            int z = (int) Math.round(Math.sin(rad) * (RADIUS - 2.2));
            if (x > CX + 20 && Math.abs(z) <= 9) continue;
            for (int y = 0; y <= 3; y++) c.set(x, y, z, Blocks.DARK_OAK_FENCE);
            c.set(x, 4, z, Blocks.LANTERN);
        }
    }

    // ------------------------------------------------------------------ couronne de maisons, halles et tours

    private static void town(HubCtx c) {
        // grandes halles derriere la banque (nord), l'hotel des ventes (ouest) et les forges (sud)
        HubTown.house(c, CX, -41, 0, 1, 25, 13, 4, "tuile_pourpre");
        HubTown.house(c, CX, 41, 0, -1, 25, 13, 3, "tuile_azur");
        HubTown.house(c, CX - 41, 0, 1, 0, 25, 13, 4, "tuile_pourpre");
        // maisons de part et d'autre
        for (int s : new int[]{-1, 1}) {
            HubTown.house(c, CX + 21 * s, -41, 0, 1, 9, 9, 3, s > 0 ? "tuile_azur" : "tuile_pourpre");
            HubTown.house(c, CX + 21 * s, 41, 0, -1, 9, 9, 3, s > 0 ? "tuile_pourpre" : "tuile_azur");
            HubTown.house(c, CX - 41, 21 * s, 1, 0, 9, 9, 3, s > 0 ? "tuile_azur" : "tuile_pourpre");
            HubTown.house(c, CX + 41, 21 * s, -1, 0, 9, 9, 3, s > 0 ? "tuile_pourpre" : "tuile_azur");
        }
        // tours des angles et tours de l'entree
        for (int sx : new int[]{-1, 1}) {
            for (int sz : new int[]{-1, 1}) HubTown.tower(c, CX + 30 * sx, 30 * sz, 5, 28, (sx * sz > 0) ? "tuile_azur" : "tuile_pourpre", true);
            HubTown.tower(c, CX + 37, 11 * sx, 4, 20, "tuile_pourpre", true);
        }
        for (int z = -7; z <= 7; z++) {                                                  // arche de l'entree entre les deux tours
            for (int y = 12; y <= 14; y++) c.set(CX + 37, y, z, y == 12 ? c.state("or_ouvrage") : c.state(Blocks.STONE_BRICKS));
            if (Math.abs(z) <= 2) for (int y = 8; y <= 11; y++) c.set(CX + 37, y, z, y == 8 ? c.state("or_ouvrage") : c.state("tuile_pourpre"));
        }
    }

    private static void avenue(HubCtx c) {
        for (int x = CX + OUTER - 8; x <= -26; x++) {                // allee d'entree : de la place des heros au marche
            for (int z = -7; z <= 7; z++) {
                if (x >= -31 && Math.hypot(x, z) <= 31.6) continue;
                if (Math.hypot(x - CX, z) <= OUTER) continue;
                int az = Math.abs(z);
                c.set(x, -2, z, az <= 3 ? c.state(Blocks.STONE_BRICKS) : c.state(Blocks.DIRT));
                c.set(x, -1, z, az <= 1 ? c.state("marbre_dore") : az <= 3 ? c.mix(x, 0, z, 91, "marbre_poli", 60, "pierre_solaire", 40) : Blocks.GRASS_BLOCK.defaultBlockState());
                for (int y = 0; y <= 8; y++) c.set(x, y, z, Blocks.AIR);
                if (az >= 4 && c.rnd(x, 0, z, 92) < 0.25F) c.set(x, 0, z, Blocks.SHORT_GRASS);
            }
        }
        for (int x = CX + OUTER - 4; x <= -32; x += 6) for (int s : new int[]{-4, 4}) HubBuilder.lamp(c, x, s);
    }

    // ------------------------------------------------------------------ fontaine centrale

    private static void fountain(HubCtx c) {
        c.disc(CX, -2, 0, 7.4, (x, y, z) -> c.state(Blocks.STONE_BRICKS));
        c.disc(CX, -1, 0, 6.6, (x, y, z) -> c.state("pierre_azur"));
        c.ring(CX, 0, 0, 6.4, 7.6, (x, y, z) -> c.state("marbre_poli"));
        c.ring(CX, 1, 0, 6.8, 7.6, (x, y, z) -> c.slab("marbre_poli", false));
        c.disc(CX, -1, 0, 6.2, (x, y, z) -> Blocks.WATER.defaultBlockState());
        c.disc(CX, -1, 0, 1.8, (x, y, z) -> c.state("marbre_dore"));
        c.disc(CX, 0, 0, 1.6, (x, y, z) -> c.state("marbre_dore"));
        c.disc(CX, 1, 0, 1.0, (x, y, z) -> c.state("or_ouvrage"));
        for (int y = 2; y <= 9; y++) c.set(CX, y, 0, "colonne_marbre");
        c.set(CX, 10, 0, "marbre_dore");
        for (int y = 11; y <= 13; y++) c.set(CX, y, 0, "cristal_azur");
        c.set(CX, 14, 0, Blocks.END_ROD);
        for (Direction d : Direction.Plane.HORIZONTAL) {
            c.set(CX + d.getStepX(), 11, d.getStepZ(), "cristal_azur");
            c.set(CX + d.getStepX() * 3, 1, d.getStepZ() * 3, "cristal_azur");
            c.set(CX + d.getStepX() * 3, 2, d.getStepZ() * 3, Blocks.END_ROD);
        }
        for (int a = 0; a < 360; a += 45) {                                           // bancs autour du bassin
            double rad = Math.toRadians(a);
            int x = CX + (int) Math.round(Math.cos(rad) * 12.5);
            int z = (int) Math.round(Math.sin(rad) * 12.5);
            Direction face = Math.abs(Math.cos(rad)) > Math.abs(Math.sin(rad)) ? (Math.cos(rad) > 0 ? Direction.WEST : Direction.EAST)
                    : (Math.sin(rad) > 0 ? Direction.NORTH : Direction.SOUTH);
            c.set(x, 0, z, c.stair("marbre_poli", face, false));
            Direction side = face.getClockWise();
            c.set(x + side.getStepX(), 0, z + side.getStepZ(), c.stair("marbre_poli", face, false));
            c.set(x - side.getStepX(), 0, z - side.getStepZ(), c.stair("marbre_poli", face, false));
            int lx = x - face.getStepX() * 2;
            int lz = z - face.getStepZ() * 2;
            HubBuilder.lamp(c, lx, lz);
        }
    }

    // ------------------------------------------------------------------ etal de marchand

    private static void stall(HubCtx c, int cx, int cz, int fx, int fz, int color) {
        for (int u = -4; u <= 4; u++) {
            for (int v = -3; v <= 3; v++) {
                for (int y = 0; y <= 5; y++) put(c, cx, cz, fx, fz, u, y, v, Blocks.AIR);
                put(c, cx, cz, fx, fz, u, -1, v, (Math.abs(u) <= 3 && Math.abs(v) <= 2) ? c.state("bois_ancien") : c.state("marbre_poli"));
            }
        }
        for (int u : new int[]{-3, 3}) {
            for (int v : new int[]{-2, 2}) {
                for (int y = 0; y <= 3; y++) put(c, cx, cz, fx, fz, u, y, v, Blocks.SPRUCE_FENCE);
                put(c, cx, cz, fx, fz, u, 4, v, Blocks.SPRUCE_LOG);
            }
        }
        for (int u = -4; u <= 4; u++) {                              // auvent raye, plus haut au fond
            for (int v = -3; v <= 3; v++) {
                BlockState s = (u & 1) == 0 ? wool(color) : Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE).defaultBlockState();
                int y = 4 + (v < 0 ? 1 : 0);
                put(c, cx, cz, fx, fz, u, y, v, s);
            }
        }
        for (int u = -4; u <= 4; u++) put(c, cx, cz, fx, fz, u, 3, 3, (u & 1) == 0 ? wool(color) : Blocks.WOOL.pick(net.minecraft.world.item.DyeColor.WHITE).defaultBlockState());
        for (int u = -2; u <= 2; u++) {                              // comptoir et rayonnages
            put(c, cx, cz, fx, fz, u, 0, 1, u == 0 ? c.state(Blocks.BARREL) : c.state("bois_ancien"));
            put(c, cx, cz, fx, fz, u, 1, 1, u % 2 == 0 ? Blocks.AIR : c.state(Blocks.FLOWER_POT));
            put(c, cx, cz, fx, fz, u, 0, -2, Blocks.BOOKSHELF);
            put(c, cx, cz, fx, fz, u, 1, -2, u % 2 == 0 ? c.state(Blocks.BARREL) : c.state(Blocks.CHISELED_BOOKSHELF));
        }
        put(c, cx, cz, fx, fz, -3, 0, 0, Blocks.BARREL);
        put(c, cx, cz, fx, fz, 3, 0, 0, Blocks.BARREL);
        put(c, cx, cz, fx, fz, 0, 3, 2, c.hanging(Blocks.LANTERN));
        put(c, cx, cz, fx, fz, -3, 3, 2, c.hanging(Blocks.LANTERN));
        put(c, cx, cz, fx, fz, 3, 3, 2, c.hanging(Blocks.LANTERN));
    }

    // ------------------------------------------------------------------ portails des mines et des donjons

    private static void gate(HubCtx c, int cx, int cz, int fx, int fz, boolean mines) {
        Object accent = mines ? "cristal_ambre" : "cristal_arcane";
        for (int u = -4; u <= 4; u++) {
            for (int v = -3; v <= 3; v++) {
                for (int y = 0; y <= 9; y++) put(c, cx, cz, fx, fz, u, y, v, Blocks.AIR);
                double r = Math.hypot(u, v);
                put(c, cx, cz, fx, fz, u, -1, v, r <= 2.4 ? c.state("dalle_runique") : r <= 3.6 ? c.state("marbre_poli") : c.state("pierre_solaire"));
            }
        }
        for (int u : new int[]{-3, 3}) {
            put(c, cx, cz, fx, fz, u, 0, 0, "marbre_dore");
            for (int y = 1; y <= 6; y++) put(c, cx, cz, fx, fz, u, y, 0, "colonne_marbre");
            put(c, cx, cz, fx, fz, u, 7, 0, "marbre_dore");
            put(c, cx, cz, fx, fz, u, 8, 0, accent);
        }
        for (int u = -3; u <= 3; u++) {
            put(c, cx, cz, fx, fz, u, 7, 0, "marbre_dore");
            put(c, cx, cz, fx, fz, u, 8, 0, Math.abs(u) == 3 ? c.state(accent) : c.state("or_ouvrage"));
        }
        put(c, cx, cz, fx, fz, 0, 9, 0, accent);
        for (int u : new int[]{-1, 1}) put(c, cx, cz, fx, fz, u, 6, 0, c.hanging(Blocks.LANTERN));
        for (int u = -2; u <= 2; u++) {                              // fond de roche et lueur
            for (int y = 0; y <= 6; y++) put(c, cx, cz, fx, fz, u, y, -1, mines ? c.mix(cx + u, y, cz, 95, Blocks.DEEPSLATE, 50, Blocks.TUFF, 30, "roche_celeste", 20)
                    : c.mix(cx + u, y, cz, 96, Blocks.BLACKSTONE, 50, Blocks.OBSIDIAN, 30, Blocks.CRYING_OBSIDIAN, 20));
        }
        put(c, cx, cz, fx, fz, 0, 1, -1, accent);
        put(c, cx, cz, fx, fz, 0, 0, -1, accent);
    }

    // ------------------------------------------------------------------ jardins des cultivateurs

    private static void farm(HubCtx c, int cx, int cz) {
        for (int u = -6; u <= 6; u++) {
            for (int v = -5; v <= 5; v++) {
                for (int y = 0; y <= 3; y++) c.set(cx + u, y, cz + v, Blocks.AIR);
                boolean border = Math.abs(u) == 6 || Math.abs(v) == 5;
                c.set(cx + u, -2, cz + v, Blocks.DIRT);
                c.set(cx + u, -1, cz + v, border ? c.state("pierre_solaire") : Blocks.FARMLAND.defaultBlockState().setValue(BlockStateProperties.MOISTURE, 7));
                if (border && !(u == 0 && v == 5)) c.set(cx + u, 0, cz + v, Blocks.OAK_FENCE);
            }
        }
        c.set(cx, 0, cz + 5, Blocks.OAK_FENCE_GATE.defaultBlockState().setValue(net.minecraft.world.level.block.FenceGateBlock.FACING, Direction.NORTH));
        for (int v = -4; v <= 4; v++) c.set(cx, -1, cz + v, Blocks.WATER);                 // canal central
        for (int v = -4; v <= 4; v += 4) c.set(cx, 0, cz + v, c.state(Blocks.LANTERN));
        c.set(cx - 5, 0, cz - 4, Blocks.HAY_BLOCK);                                         // epouvantail
        c.set(cx - 5, 1, cz - 4, Blocks.OAK_FENCE);
        c.set(cx - 5, 2, cz - 4, Blocks.CARVED_PUMPKIN.defaultBlockState().setValue(net.minecraft.world.level.block.CarvedPumpkinBlock.FACING, Direction.EAST));
        c.set(cx - 4, 1, cz - 4, Blocks.OAK_FENCE);
        c.set(cx - 6, 1, cz - 4, Blocks.OAK_FENCE);
    }

    /** Cultures d'Aeloria dans les jardins : une rangee par plante, a maturite (decor et apercu pour les joueurs). */
    private static void plantings(HubCtx c) {
        int cx = CX - 29;
        int cz = 17;
        String[] ids = {"lueur", "argent", "ambre", "givre", "neant"};
        for (int i = 0; i < ids.length; i++) {
            Block crop = com.mmorpg.farm.FarmCrops.BLOCKS.get(ids[i]).get();
            int v = -4 + i * 2;
            for (int u = -5; u <= 5; u++) {
                if (u == 0) continue;
                BlockState st = crop.defaultBlockState().setValue(net.minecraft.world.level.block.CropBlock.AGE, 7);
                c.set(cx + u, 0, cz + v, st);
            }
        }
    }

    // ================================================================== textes

    static void texts(HubCtx c) {
        HubBuilder.text(c, CX, 17, 0, "MARCHÉ D'AELORIA", "#FFD060", 7F);
        HubBuilder.text(c, -27, 8, 0, "MARCHÉ D'AELORIA", "#FFD060", 3.4F);
        HubBuilder.text(c, -27, 6.4, 0, "Banque · Hôtel des ventes · Forges", "#F0E8D8", 1.8F);
        HubBuilder.text(c, CX, 14, -25 + 2, "BANQUE", "#FFE08A", 4F);
        HubBuilder.text(c, CX - 27, 14, 0, "HÔTEL DES VENTES", "#FFC080", 4F);
        HubBuilder.text(c, CX, 14, 24, "PRESSE DE COMPRESSION", "#80E8D8", 3F);
        HubBuilder.text(c, CX - 21, 12, 25, "FORGE", "#FFB070", 3.4F);
        HubBuilder.text(c, CX + 22, 12, 25, "FORGE", "#FFB070", 3.4F);
        HubBuilder.text(c, CX + 25, 11, 0, "TÉLÉPORTEUR", "#9FD8FF", 3F);
        HubBuilder.text(c, CX + 24, 12, -18, "MINES D'AELORIA", "#FFAA50", 4F);
        HubBuilder.text(c, CX + 24, 12, 19, "DONJONS", "#C090FF", 4F);
        HubBuilder.text(c, CX - 29, 7, 17, "JARDINS D'AELORIA", "#8CE68C", 3.4F);
        HubBuilder.text(c, CX + 12, 8, -13, "ALCHIMIE", "#9FD8FF", 2.6F);
        HubBuilder.text(c, CX + 12, 8, 13, "GRAINES", "#8CE68C", 2.6F);
        HubBuilder.text(c, CX - 12, 8, -13, "ARMURERIE", "#FFB070", 2.6F);
        HubBuilder.text(c, CX - 12, 8, 13, "MINERAIS", "#FFE08A", 2.6F);
    }

    // ================================================================== PNJ

    private static void npc(HubCtx c, String key, double x, double z, float yaw, NpcEntity.Role role, String name, int skin, String group) {
        NpcEntity npc = ModEntities.NPC.get().create(c.L, EntitySpawnReason.COMMAND);
        if (npc == null) return;
        npc.snapTo(c.O.getX() + x + 0.5, c.O.getY(), c.O.getZ() + z + 0.5, yaw, 0F);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        npc.setup(role, name, skin, group);
        npc.setUUID(HubBuilder.stable("npc:marche:" + key));
        c.L.addFreshEntity(npc);
    }

    static void entities(HubCtx c) {
        // yaw Minecraft : 0 = sud (+z), 90 = ouest (-x), 180 = nord (-z), 270 = est (+x)
        npc(c, "alchimie", CX + 12, -13, 0F, NpcEntity.Role.MARCHAND, "Alchimiste", 3, "marche:alchimie");
        npc(c, "graines", CX + 12, 13, 180F, NpcEntity.Role.MARCHAND, "Marchand de graines", 1, "marche:graines");
        npc(c, "armurerie", CX - 12, -13, 0F, NpcEntity.Role.MARCHAND, "Armurier", 0, "marche:armurerie");
        npc(c, "minerais", CX - 12, 13, 180F, NpcEntity.Role.MARCHAND, "Marchand de minerais", 2, "marche:minerais");
        npc(c, "mines", CX + 24 - 1, -18, 90F, NpcEntity.Role.MINES, "Maître des mines", 1, "marche:mines");
        npc(c, "donjons", CX + 24 - 1, 19, 90F, NpcEntity.Role.DONJON, "Maître des donjons", 3, com.mmorpg.dungeon.DungeonMaster.GROUP_ALL);
        npc(c, "ferme", CX - 29, 17 + 6, 180F, NpcEntity.Role.MARCHAND, "Fermière d'Aeloria", 2, "marche:ferme");
    }
}
