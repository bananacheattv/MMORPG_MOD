package com.mmorpg.hub;

import com.mmorpg.entity.dummy.TrainingDummyEntity;
import com.mmorpg.entity.hub.ClassMasterEntity;
import com.mmorpg.registry.ModEntities;
import com.mmorpg.rpg.PlayerClass;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.AABB;

import java.util.Locale;

/**
 * Construction du hub d'entree (/mmorpg hub) : la « Citadelle d'Eldoria », une ile celeste a face inferieure rocheuse effilee
 * (cascades, cristaux lumineux, lianes) portant une grande place circulaire, sa rotonde a dome vitre et son bassin annulaire,
 * quatre pavillons de classe, un portail a tours rondes, un teleporteur, une arene d'epreuve coiffee d'un dome et une aire
 * d'entrainement ronde, entoures de jardins et d'ilots flottants. Les blocs viennent de {@link com.mmorpg.registry.DecorBlocks}
 * (textures peintes pour le mod), les titres geants sont des entites d'affichage de texte en police « Eldoria ».
 * Reconstruire le hub au meme endroit remplace ses PNJ, mannequins et textes sans les dupliquer.
 */
public final class HubBuilder {
    private HubBuilder() {
    }

    /** Etiquette des entites decoratives du hub (textes), retirees a chaque reconstruction. */
    public static final String TAG = "mmorpg_hub";
    static final PlayerClass[] CLASSES = {PlayerClass.GUERRIER, PlayerClass.MAGE, PlayerClass.ARCHER, PlayerClass.TANK};
    static final Block[] FLOWERS = {Blocks.DANDELION, Blocks.POPPY, Blocks.CORNFLOWER, Blocks.AZURE_BLUET, Blocks.ALLIUM,
            Blocks.OXEYE_DAISY, Blocks.PINK_TULIP, Blocks.LILY_OF_THE_VALLEY};

    /** Construit le hub avec sa place centrale au niveau {@code feet} (sol en dessous) et renvoie le centre. */
    public static BlockPos build(ServerLevel level, BlockPos feet) {
        HubCtx c = new HubCtx(level, feet.immutable());
        removeOld(c);
        computeMasks(c);
        terrain(c);
        plaza(c);
        rotunda(c);
        HubPavilions.build(c);
        HubStructures.gate(c);
        HubStructures.teleporter(c);
        HubStructures.causeway(c);
        HubStructures.arena(c);
        HubStructures.yard(c);
        HubFlora.gardens(c);
        HubFlora.islets(c);
        texts(c);
        entities(c);
        var server = level.getServer();
        var old = server.getRespawnData();
        if (old.globalPos().dimension().equals(net.minecraft.world.level.Level.OVERWORLD) && !level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
            HubManager.registerExit(server, old.globalPos().pos());
        }
        BlockPos spawn = HubLayout.pos(c.O, HubLayout.SPAWN);
        level.setRespawnData(new LevelData.RespawnData(GlobalPos.of(level.dimension(), spawn), 180F, 0F));
        HubData.get(level.getServer()).set(level.dimension(), c.O);
        return c.O;
    }

    // ================================================================== emprise de l'ile

    static boolean footprint(int x, int z) {
        if (x * x + z * z <= 31.6 * 31.6) return true;                                      // place
        if (Math.hypot(x, z - 46) <= 13 || (Math.abs(x) <= 11 && z >= 28 && z <= 46)) return true;   // portail et parvis
        if (Math.abs(x) <= 7 && z >= -54 && z <= -28) return true;                          // voie vers l'arene
        if (Math.hypot(x, z + 74) <= 24) return true;                                       // arene
        if (x >= 28 && x <= 46 && Math.abs(z) <= 7) return true;                            // allee de l'aire
        if (Math.hypot(x - HubLayout.YARD_CENTER_X, z) <= 18) return true;                  // aire d'entrainement
        for (PlayerClass cls : CLASSES) {
            int[] s = HubLayout.classSpot(cls);
            if (Math.hypot(x - s[0], z - s[2]) <= 12) return true;                          // pavillons
        }
        return false;
    }

    private static void computeMasks(HubCtx c) {
        int w = HubCtx.X1 - HubCtx.X0 + 1;
        int d = HubCtx.Z1 - HubCtx.Z0 + 1;
        c.foot = new boolean[w][d];
        c.isle = new boolean[w][d];
        c.edge = new float[w][d];
        for (int x = HubCtx.X0; x <= HubCtx.X1; x++) for (int z = HubCtx.Z0; z <= HubCtx.Z1; z++) c.foot[x - HubCtx.X0][z - HubCtx.Z0] = footprint(x, z);
        for (int x = HubCtx.X0; x <= HubCtx.X1; x++) {           // ile = emprise dilatee d'une bordure de pelouse (4 blocs, arrondie)
            for (int z = HubCtx.Z0; z <= HubCtx.Z1; z++) {
                if (!c.foot[x - HubCtx.X0][z - HubCtx.Z0]) continue;
                for (int dx = -4; dx <= 4; dx++) {
                    for (int dz = -4; dz <= 4; dz++) {
                        if (dx * dx + dz * dz > 17) continue;
                        int i = x + dx - HubCtx.X0;
                        int j = z + dz - HubCtx.Z0;
                        if (i >= 0 && j >= 0 && i < w && j < d) c.isle[i][j] = true;
                    }
                }
            }
        }
        float inf = 1e6F;                                         // distance au bord de l'ile (chanfrein en deux passes)
        for (int i = 0; i < w; i++) for (int j = 0; j < d; j++) c.edge[i][j] = c.isle[i][j] ? inf : 0;
        for (int i = 1; i < w - 1; i++) {
            for (int j = 1; j < d - 1; j++) {
                float v = c.edge[i][j];
                v = Math.min(v, c.edge[i - 1][j] + 1);
                v = Math.min(v, c.edge[i][j - 1] + 1);
                v = Math.min(v, c.edge[i - 1][j - 1] + 1.41F);
                v = Math.min(v, c.edge[i + 1][j - 1] + 1.41F);
                c.edge[i][j] = v;
            }
        }
        for (int i = w - 2; i >= 1; i--) {
            for (int j = d - 2; j >= 1; j--) {
                float v = c.edge[i][j];
                v = Math.min(v, c.edge[i + 1][j] + 1);
                v = Math.min(v, c.edge[i][j + 1] + 1);
                v = Math.min(v, c.edge[i + 1][j + 1] + 1.41F);
                v = Math.min(v, c.edge[i - 1][j + 1] + 1.41F);
                c.edge[i][j] = v;
            }
        }
    }

    // ================================================================== terrain : face inferieure, rebord, cascades

    private static BlockState rock(HubCtx c, int x, int y, int z, float frac) {
        if (frac > 0.6F) return c.mix(x, y, z, 7, Blocks.DEEPSLATE, 40, Blocks.TUFF, 24, Blocks.COBBLED_DEEPSLATE, 20, "roche_celeste", 16);
        return c.mix(x, y, z, 7, "roche_celeste", 34, Blocks.STONE, 22, Blocks.ANDESITE, 14, Blocks.TUFF, 10,
                Blocks.COBBLESTONE, 8, frac < 0.35F ? Blocks.MOSS_BLOCK : Blocks.MOSSY_COBBLESTONE, 12);
    }

    private static void terrain(HubCtx c) {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = HubCtx.X0; x <= HubCtx.X1; x++) {
            for (int z = HubCtx.Z0; z <= HubCtx.Z1; z++) {
                if (!c.in(c.isle, x, z)) continue;
                float e = c.edge[x - HubCtx.X0][z - HubCtx.Z0];
                boolean f = c.in(c.foot, x, z);
                int depth = Mth.clamp(Math.round(3F + 3.8F * (float) Math.sqrt(e) + 7F * (c.noise(x, z, 6F, 1) - 0.5F)), 4, 34);
                int hill = f ? 0 : Math.round(2.6F * c.noise(x, z, 7F, 2) * Math.min(1F, (e - 1.2F) / 3F));
                int clear = Math.hypot(x, z + 74) <= 26 ? 46 : 34;
                for (int y = 0; y <= clear; y++) c.set(x, y, z, air);
                c.set(x, -1 + hill, z, f ? c.state("marbre_poli") : Blocks.GRASS_BLOCK.defaultBlockState());
                for (int y = -2 + hill; y >= -2; y--) c.set(x, y, z, Blocks.DIRT);
                c.set(x, -2, z, f ? Blocks.STONE_BRICKS.defaultBlockState() : Blocks.DIRT.defaultBlockState());
                for (int y = -3; y >= -depth; y--) c.set(x, y, z, rock(c, x, y, z, (float) (-y - 3) / Math.max(1, depth - 3)));
                int by = -depth - 1;                                  // cristaux lumineux et lianes sous l'ile
                float h = c.rnd(x, 5, z, 11);
                if (e > 2.5F && h < 0.07F) {
                    int n = 1 + (int) (c.rnd(x, 6, z, 12) * 3);
                    for (int k = 0; k < n; k++) c.set(x, by - k, z, c.rnd(x, 7, z, 13) < 0.7F ? "cristal_azur" : "cristal_arcane");
                } else if (e < 7F && h > 0.5F) {
                    int len = 2 + (int) (c.rnd(x, 9, z, 13) * (e < 2F ? 8 : 4));
                    for (int k = 0; k < len; k++) {
                        BlockState s = (k == len - 1 ? Blocks.CAVE_VINES : Blocks.CAVE_VINES_PLANT).defaultBlockState()
                                .setValue(BlockStateProperties.BERRIES, c.rnd(x, k, z, 17) < 0.6F);
                        c.set(x, by - k, z, s);
                    }
                }
                if (!f) {                                             // pelouse du bord : herbes, fleurs, arbustes
                    int top = hill;
                    float g = c.rnd(x, 0, z, 21);
                    if (g < 0.2F) c.set(x, top, z, Blocks.SHORT_GRASS);
                    else if (g < 0.3F) c.set(x, top, z, FLOWERS[(int) (c.rnd(x, 1, z, 22) * FLOWERS.length) % FLOWERS.length]);
                    else if (g < 0.325F && e > 1.8F) {
                        c.set(x, top, z, c.leaves(Blocks.AZALEA_LEAVES));
                        if (c.rnd(x, 2, z, 23) < 0.5F) c.set(x, top + 1, z, c.leaves(Blocks.FLOWERING_AZALEA_LEAVES));
                    }
                }
            }
        }
        int[][] falls = {{-37, 0}, {0, 62}, {39, -22}, {-30, -60}, {70, 14}, {-34, 30}, {30, 44}};   // cascades : sources sur le rebord
        for (int[] f : falls) {
            float best = 1e9F;
            int fx = f[0];
            int fz = f[1];
            for (int x = HubCtx.X0 + 1; x < HubCtx.X1; x++) {
                for (int z = HubCtx.Z0 + 1; z < HubCtx.Z1; z++) {
                    if (!c.in(c.isle, x, z) || c.in(c.foot, x, z) || c.edge[x - HubCtx.X0][z - HubCtx.Z0] > 1.2F) continue;
                    float dd = (x - f[0]) * (x - f[0]) + (z - f[1]) * (z - f[1]);
                    if (dd < best) {
                        best = dd;
                        fx = x;
                        fz = z;
                    }
                }
            }
            if (best < 900F) {
                for (int y = 0; y <= 3; y++) if (!c.air(fx, y, fz)) c.set(fx, y, fz, Blocks.AIR);
                int gy = -1;
                while (gy < 6 && !c.air(fx, gy + 1, fz)) gy++;
                c.set(fx, gy, fz, Blocks.WATER);
            }
        }
    }

    // ================================================================== place

    static boolean avenue(int x, int z) {
        return Math.abs(x) <= 2 || Math.abs(z) <= 2 || Math.abs(Math.abs(x) - Math.abs(z)) <= 2;
    }

    private static double spokeDeg(int x, int z) {
        double a = Math.toDegrees(Math.atan2(z, x));
        double m = ((a % 45) + 45) % 45;
        return Math.min(m, 45 - m);
    }

    private static void plaza(HubCtx c) {
        for (int x = -33; x <= 33; x++) {
            for (int z = -33; z <= 33; z++) {
                double r = Math.hypot(x, z);
                if (r > 31.6) continue;
                boolean water = false;
                BlockState top;
                BlockState under = Blocks.STONE_BRICKS.defaultBlockState();
                boolean cardinal = Math.abs(x) <= 2 || Math.abs(z) <= 2;
                boolean diag = Math.abs(Math.abs(x) - Math.abs(z)) <= 2;
                if (r <= 2.2) top = c.state("marbre_dore");
                else if (r <= 3.4) top = c.state("or_ouvrage");
                else if (r <= 6.4) top = spokeDeg(x, z) < 6 * 3.4 / Math.max(r, 3.4) ? c.state("pierre_solaire") : c.state("marbre_poli");
                else if (r <= 7.4) top = c.state("pierre_solaire");
                else if (r <= 9.4) top = ((x + z) & 1) == 0 ? c.state("marbre_poli") : c.state("marbre");
                else if (r <= 10.6) top = c.state("marbre_poli");
                else if (r <= 16.4) {
                    if (cardinal || (diag && r > 10.6)) {
                        top = (Math.abs(x) <= 0 || Math.abs(z) <= 0 || x == z || x == -z) ? c.state("marbre_dore") : c.state("marbre_poli");
                    } else {
                        top = Blocks.WATER.defaultBlockState();
                        under = c.state("pierre_azur");
                        water = true;
                    }
                } else if (r <= 17.6) top = c.state("marbre_poli");
                else if (r <= 26.6) {
                    if (r > 21.4 && r <= 22.8) top = c.state("pierre_solaire");
                    else if (cardinal || diag) top = c.mix(x, 0, z, 31, "marbre_poli", 60, "marbre", 25, "pierre_solaire", 15);
                    else top = Blocks.GRASS_BLOCK.defaultBlockState();
                } else if (r > 30.4) top = c.state("pierre_solaire");
                else top = c.mix(x, 0, z, 33, "marbre_poli", 45, "pierre_azur", 25, "marbre", 20, "pierre_solaire", 10);
                c.set(x, -2, z, under);
                c.set(x, -1, z, top);
                if (top.getBlock() == Blocks.GRASS_BLOCK) {
                    float g = c.rnd(x, 0, z, 35);
                    if (g < 0.2F) c.set(x, 0, z, Blocks.SHORT_GRASS);
                    else if (g < 0.34F) c.set(x, 0, z, FLOWERS[(int) (c.rnd(x, 1, z, 36) * FLOWERS.length) % FLOWERS.length]);
                } else if (!water) {
                    c.set(x, 0, z, Blocks.AIR);
                }
            }
        }
        for (int x = -18; x <= 18; x++) {                          // levres basses du bassin, sauf aux ponts
            for (int z = -18; z <= 18; z++) {
                if (Math.abs(x) <= 2 || Math.abs(z) <= 2 || Math.abs(Math.abs(x) - Math.abs(z)) <= 2) continue;
                double r = Math.hypot(x, z);
                if ((r > 10.0 && r <= 10.6) || (r > 16.4 && r <= 17.0)) c.set(x, 0, z, c.slab("marbre_poli", false));
            }
        }
        for (int a = 0; a < 360; a += 12) {                        // lanternes sur la levre externe du bassin
            int x = (int) Math.round(Math.cos(Math.toRadians(a)) * 17.4);
            int z = (int) Math.round(Math.sin(Math.toRadians(a)) * 17.4);
            if (HubBuilder.avenue(x, z)) continue;
            for (int y = 0; y <= 1; y++) c.set(x, y, z, Blocks.SANDSTONE_WALL);
            c.set(x, 2, z, Blocks.LANTERN);
        }
        for (int a = 0; a < 360; a++) {                            // balustrade du pourtour et pilastres de lanternes
            double rad = Math.toRadians(a);
            int x = (int) Math.round(Math.cos(rad) * 31);
            int z = (int) Math.round(Math.sin(rad) * 31);
            if (Math.abs(x) <= 8 && z > 0) continue;               // portail
            if (Math.abs(x) <= 8 && z < 0) continue;               // voie de l'arene
            if (x > 0 && Math.abs(z) <= 8) continue;               // allee de l'aire
            boolean nearPavilion = false;
            for (PlayerClass cls : CLASSES) {
                int[] s = HubLayout.classSpot(cls);
                if (Math.hypot(x - s[0], z - s[2]) < 12) nearPavilion = true;
            }
            if (nearPavilion) continue;
            c.set(x, 0, z, Blocks.SANDSTONE_WALL);
            if (a % 10 == 0) {
                for (int y = 0; y <= 2; y++) c.set(x, y, z, Blocks.SANDSTONE_WALL);
                c.set(x, 3, z, Blocks.LANTERN);
            }
        }
        for (int[] d : new int[][]{{0, 1}, {0, -1}, {1, 0}, {-1, 0}}) {   // lampadaires le long des quatre avenues
            for (int k = 12; k <= 29; k += 4) {
                for (int s : new int[]{-3, 3}) {
                    int x = d[0] != 0 ? d[0] * k : s;
                    int z = d[1] != 0 ? d[1] * k : s;
                    lamp(c, x, z);
                }
            }
        }
    }

    static void lamp(HubCtx c, int x, int z) {
        for (int y = 0; y <= 3; y++) c.set(x, y, z, Blocks.SANDSTONE_WALL);
        c.set(x, 4, z, Blocks.LANTERN);
    }

    // ================================================================== rotonde centrale

    private static void rotunda(HubCtx c) {
        final double R = 9.8;
        for (int k = 0; k < 12; k++) {                             // 12 colonnes, quatre ouvertures cardinales
            double a = Math.toRadians(15 + k * 30);
            int x = (int) Math.round(Math.cos(a) * 8.4);
            int z = (int) Math.round(Math.sin(a) * 8.4);
            c.set(x, 0, z, "marbre_dore");
            for (int y = 1; y <= 10; y++) c.set(x, y, z, "colonne_marbre");
            c.set(x, 11, z, "marbre_dore");
            for (int[] o : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) c.set(x + o[0], 11, z + o[1], c.slab("marbre_dore", false));
            c.set(x, 12, z, "or_ouvrage");
        }
        c.ring(0, 12, 0, 7.5, 9.8, (x, y, z) -> c.state("marbre"));       // architrave
        c.ring(0, 13, 0, 8.0, 9.4, (x, y, z) -> (x + z) % 3 == 0 ? c.state("cristal_azur") : c.state("or_ouvrage"));
        c.dome(0, 13, 0, R + 0.8, 1.35, (x, y, z, el, az) -> {
            if (el > 76) return null;                                                         // oculus
            double m = az % 30;
            if (el < 9) return c.state("or_ouvrage");
            if (el > 68) return c.state("marbre_dore");
            if (m < 2.6 || m > 27.4) return el > 24 && el < 36 ? c.state("cristal_azur") : c.state("marbre_dore");   // nervures, a cristaux lumineux
            if (el > 14 && m > 7 && m < 23) return Blocks.STAINED_GLASS.pick(DyeColor.LIGHT_BLUE).defaultBlockState();
            return c.state("tuile_azur");
        });
        for (int k = 0; k < 12; k++) {                             // lustres : chaines et lanternes sous l'architrave
            double a = Math.toRadians(k * 30);
            int x = (int) Math.round(Math.cos(a) * 7.2);
            int z = (int) Math.round(Math.sin(a) * 7.2);
            c.set(x, 11, z, Blocks.IRON_CHAIN);
            c.set(x, 10, z, Blocks.IRON_CHAIN);
            c.set(x, 9, z, Blocks.LANTERN.defaultBlockState().setValue(net.minecraft.world.level.block.LanternBlock.HANGING, true));
        }
        c.set(0, 7, 0, "cristal_azur");                            // coeur de cristal flottant au-dessus du Maitre des classes
        c.set(0, 6, 0, "cristal_azur");
        c.set(0, 8, 0, "cristal_azur");
        for (Direction d : Direction.Plane.HORIZONTAL) c.set(d.getStepX(), 7, d.getStepZ(), "cristal_azur");
        for (int k = 0; k < 8; k++) {
            double a = Math.toRadians(k * 45 + 22.5);
            c.set((int) Math.round(Math.cos(a) * 2.6), 7, (int) Math.round(Math.sin(a) * 2.6), Blocks.END_ROD);
        }
    }

    // ================================================================== textes en police Eldoria

    static void text(HubCtx c, double x, double y, double z, String text, String color, float scale) {
        String snbt = String.format(Locale.ROOT, "{text:{text:\"%s\",font:\"mmorpg:eldoria\",color:\"%s\"},billboard:\"center\",background:0,shadow:true,"
                + "see_through:false,line_width:600,Tags:[\"%s\"],transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],"
                + "translation:[0f,0f,0f],scale:[%.2ff,%.2ff,%.2ff]}}", text.replace("\"", "\\\""), color, TAG, scale, scale, scale);
        CommandSourceStack src = c.L.getServer().createCommandSourceStack().withLevel(c.L).withSuppressedOutput();
        c.L.getServer().getCommands().performPrefixedCommand(src,
                String.format(Locale.ROOT, "summon minecraft:text_display %.2f %.2f %.2f %s", c.O.getX() + x + 0.5, c.O.getY() + y, c.O.getZ() + z + 0.5, snbt));
    }

    private static void texts(HubCtx c) {
        text(c, 0, 33, 36, "ELDORIA", "#FFD060", 12F);
        text(c, 0, 28.6, 36, "Citadelle des héros", "#F0E8D8", 3.4F);
        for (PlayerClass cls : CLASSES) {
            int[] s = HubLayout.classSpot(cls);
            String[] t = switch (cls) {
                case GUERRIER -> new String[]{"LE GUERRIER", "#FF6A50"};
                case MAGE -> new String[]{"LE MAGE", "#B07CFF"};
                case ARCHER -> new String[]{"L'ARCHER", "#6CE06C"};
                default -> new String[]{"LE PALADIN", "#58A8FF"};
            };
            text(c, s[0], 25, s[2], t[0], t[1], 6F);
        }
        text(c, 0, 13.5, 0, "MAÎTRE DES CLASSES", "#FFE6A0", 2.2F);
        text(c, 0, 12, 24, "TÉLÉPORTEUR", "#9FD8FF", 3.4F);
        text(c, 0, 17, HubLayout.ROOM_Z1 - 1, "ÉPREUVE DES HÉROS", "#FFC060", 5F);
        text(c, HubLayout.YARD_CENTER_X - 17, 11, 0, "AIRE D'ENTRAÎNEMENT", "#FFB070", 4F);
    }

    // ================================================================== PNJ et mannequins

    private static void removeOld(HubCtx c) {
        AABB box = HubLayout.hubBounds(c.O);
        for (Entity e : c.L.getEntities((Entity) null, box, x -> x instanceof ClassMasterEntity || x instanceof TrainingDummyEntity
                || x instanceof Display.TextDisplay
                || (x instanceof com.mmorpg.entity.NpcEntity n && "maitre_classes".equals(n.group())))) {
            if (e instanceof Display.TextDisplay && !e.entityTags().contains(TAG)) continue;
            e.discard();
        }
    }

    private static void entities(HubCtx c) {
        var master = com.mmorpg.world.DungeonBuilder.spawnNpc(c.L, HubLayout.pos(c.O, HubLayout.CLASS_MASTER), com.mmorpg.entity.NpcEntity.Role.QUETES,
                "Maître des classes", 2, 0F);
        if (master != null) master.setup(com.mmorpg.entity.NpcEntity.Role.QUETES, "Maître des classes", 2, "maitre_classes");
        for (int k = 0; k < 4; k++) {
            PlayerClass cls = CLASSES[k];
            int[] s = HubLayout.classSpot(cls);
            spawnMaster(c, k, s[0], 1, s[2], HubLayout.classYaw(cls));
        }
        spawnMaster(c, ClassMasterEntity.EPREUVE, HubLayout.TRIAL_MASTER[0], 0, HubLayout.TRIAL_MASTER[2], 90F);
        for (int[] d : HubLayout.DUMMIES_ROOM) spawnDummy(c, d);
        for (int[] d : HubLayout.DUMMIES_YARD) spawnDummy(c, d);
    }

    private static void spawnMaster(HubCtx c, int kind, int x, int y, int z, float yaw) {
        ClassMasterEntity npc = ModEntities.CLASS_MASTER.get().create(c.L, EntitySpawnReason.COMMAND);
        if (npc == null) return;
        npc.snapTo(c.O.getX() + x + 0.5, c.O.getY() + y, c.O.getZ() + z + 0.5, yaw, 0F);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        npc.setup(kind);
        c.L.addFreshEntity(npc);
    }

    private static void spawnDummy(HubCtx c, int[] d) {
        TrainingDummyEntity e = ModEntities.TRAINING_DUMMY.get().create(c.L, EntitySpawnReason.COMMAND);
        if (e == null) return;
        e.snapTo(c.O.getX() + d[0] + 0.5, c.O.getY(), c.O.getZ() + d[1] + 0.5, d[3], 0F);
        e.setup(d[2]);
        c.L.addFreshEntity(e);
    }
}
