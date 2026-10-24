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

import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * Construction du hub d'entree (/mmorpg hub) : la « Citadelle d'Aeloria », une ile celeste a face inferieure rocheuse effilee
 * (cascades, cristaux lumineux, lianes) portant une grande place circulaire, sa rotonde a dome vitre et son bassin annulaire,
 * quatre pavillons de classe, un portail a tours rondes, un teleporteur, une arene d'epreuve coiffee d'un dome et une aire
 * d'entrainement ronde, entoures de jardins et d'ilots flottants. Les blocs viennent de {@link com.mmorpg.registry.DecorBlocks}
 * (textures peintes pour le mod), les titres geants sont des entites d'affichage de texte en police « Aeloria ».
 * Reconstruire le hub au meme endroit remplace ses PNJ, mannequins et textes sans les dupliquer.
 */
public final class HubBuilder {
    private HubBuilder() {
    }

    /** Etiquette des entites decoratives du hub (textes), retirees a chaque reconstruction. */
    public static final String TAG = "mmorpg_hub";
    /** Version du plan du hub : 5 = Grande Nef sans marche (le marche vit dans la dimension du spawn), pont et portail du spawn. Un hub plus ancien est reconstruit au demarrage. */
    public static final int LAYOUT_VERSION = 5;
    static final PlayerClass[] CLASSES = {PlayerClass.GUERRIER, PlayerClass.MAGE, PlayerClass.ARCHER, PlayerClass.TANK};
    static final Block[] FLOWERS = {Blocks.DANDELION, Blocks.POPPY, Blocks.CORNFLOWER, Blocks.AZURE_BLUET, Blocks.ALLIUM,
            Blocks.OXEYE_DAISY, Blocks.PINK_TULIP, Blocks.LILY_OF_THE_VALLEY};

    /** Identifiant stable d'une entite du hub : reconstruire le hub ne cree donc jamais de doublon, meme dans une zone non chargee. */
    static UUID stable(String key) {
        return stable(key, instance);
    }

    /** Copie du hub en cours de construction (0 = Hub 1) : chaque copie a ses propres identifiants, la premiere garde ceux d'avant. */
    private static int instance;

    static int instance() {
        return instance;
    }

    private static UUID stable(String key, int copy) {
        // la graine garde l'ancien nom du jeu : elle fixe l'identite des PNJ et des textes deja poses dans les hubs existants
        return UUID.nameUUIDFromBytes(("eldoria-hub:" + key + (copy == 0 ? "" : "#" + copy)).getBytes(StandardCharsets.UTF_8));
    }

    /** Identifiants des PNJ de l'ancien marche du hub (le marche est desormais dans le spawn) : ils sont retires du hub. */
    public static boolean isLegacyMarketNpc(UUID id) {
        for (String k : new String[]{"graines", "alchimie", "armurerie", "minerais", "mines", "donjons", "ferme"}) {
            if (id.equals(stable("npc:marche:" + k, 0))) return true;
        }
        return false;
    }

    private static final Set<UUID> STABLE = new HashSet<>();

    static {
        for (int copy = 0; copy < 32; copy++) {
            for (int i = 0; i < 8; i++) STABLE.add(stable("master:" + i, copy));
            STABLE.add(stable("npc:maitre", copy));
            for (int i = 0; i < 16; i++) {
                STABLE.add(stable("dummy:room:" + i, copy));
                STABLE.add(stable("dummy:yard:" + i, copy));
            }
            for (int i = 0; i < 40; i++) STABLE.add(stable("text:" + i, copy));
        }
    }

    public static boolean isStable(UUID id) {
        return STABLE.contains(id);
    }

    static int textIndex;

    /** Construit le hub avec sa place centrale au niveau {@code feet} (sol en dessous) et renvoie le centre. */
    public static BlockPos build(ServerLevel level, BlockPos feet) {
        return build(level, feet, 0);
    }

    /** Construit la copie {@code index} du hub (0 = Hub 1) : place, rotonde, pavillons, portail, arene, aire, pont du spawn. */
    public static BlockPos build(ServerLevel level, BlockPos feet, int index) {
        HubCtx c = new HubCtx(level, feet.immutable());
        instance = index;
        textIndex = 0;
        HubPortals.clearLegacyMarket(c);
        removeOld(c);
        computeMasks(c);
        terrain(c);
        HubHall.plaza(c);
        HubHall.rotunda(c);
        HubPavilions.build(c);
        HubStructures.gate(c);
        HubStructures.teleporter(c);
        HubStructures.causeway(c);
        HubStructures.arena(c);
        HubStructures.yard(c);
        HubPortals.spawnGate(c);
        HubFlora.gardens(c);
        HubFlora.islets(c);
        texts(c);
        entities(c);
        zones(c);
        var server = level.getServer();
        if (index == 0) {
            var old = server.getRespawnData();
            if (old.globalPos().dimension().equals(net.minecraft.world.level.Level.OVERWORLD) && !level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
                HubManager.registerExit(server, old.globalPos().pos());
            }
            if (!SpawnData.get(server).exists()) {                                          // sans spawn, la mort et la reconnexion ramenent au hub
                BlockPos spawn = HubLayout.pos(c.O, HubLayout.SPAWN);
                level.setRespawnData(new LevelData.RespawnData(GlobalPos.of(level.dimension(), spawn), 180F, 0F));
            }
        }
        HubData.get(level.getServer()).built(level.dimension(), c.O.subtract(new BlockPos(index * HubData.SPACING, 0, 0)), index);
        instance = 0;
        return c.O;
    }

    /** Zones d'annonce du hub (animation d'entree) : la citadelle, la place, les pavillons, l'arene d'epreuve, l'aire d'entrainement. */
    private static void zones(HubCtx c) {
        var data = com.mmorpg.zone.ZoneData.get(c.L.getServer());
        var dim = c.L.dimension();
        int ox = c.O.getX();
        int oz = c.O.getZ();
        String tag = instance == 0 ? "" : instance + "_";
        String suffix = " · Hub " + (instance + 1);
        java.util.function.BiConsumer<String[], int[]> add = (txt, r) -> data.add(new com.mmorpg.zone.ZoneData.Zone("hub_" + tag + txt[0], txt[1], dim,
                ox + r[0], oz + r[1], ox + r[2], oz + r[3], false, false, false, com.mmorpg.zone.ZoneData.Zone.STYLE_ZONE, txt[2]));
        add.accept(new String[]{"citadelle", "Citadelle d'Aeloria" + suffix, "Capitale des héros"}, new int[]{-52, -106, HubLayout.YARD_X1 + 12, 66});
        add.accept(new String[]{"rotonde", "Place des Héros", "Parlez au Maître des classes"}, new int[]{-31, -31, 31, 31});
        add.accept(new String[]{"epreuve", "Épreuve des Héros", "Essayez une classe avant de la choisir"},
                new int[]{-HubLayout.ROOM_HALF, HubLayout.ROOM_Z0, HubLayout.ROOM_HALF, HubLayout.ROOM_Z1});
        add.accept(new String[]{"entrainement", "Aire d'entraînement", "Mannequins de combat"},
                new int[]{HubLayout.YARD_X0, -HubLayout.YARD_HALF, HubLayout.YARD_X1, HubLayout.YARD_HALF});
        add.accept(new String[]{"portail", "Portail d'Aeloria", "Le monde vous attend"}, new int[]{-12, 26, 12, 60});
        add.accept(new String[]{"pont", "Pont du Spawn", "Le Marché d'Aeloria vous attend"}, new int[]{-52, -8, -28, 8});
        String[][] mottos = {{"Guerrier", "Force et férocité"}, {"Mage", "Intelligence et esprit"}, {"Archer", "Agilité et dextérité"}, {"Paladin", "Vitalité et endurance"}};
        for (int i = 0; i < 4; i++) {
            int[] sp = HubLayout.classSpot(CLASSES[i]);
            add.accept(new String[]{"pavillon_" + i, "Pavillon du " + mottos[i][0], mottos[i][1]}, new int[]{sp[0] - 12, sp[2] - 12, sp[0] + 12, sp[2] + 12});
        }
    }

    // ================================================================== emprise de l'ile

    static boolean footprint(int x, int z) {
        if (x * x + z * z <= 31.6 * 31.6) return true;                                      // place
        if (x <= -28 && x >= -52 && Math.abs(z) <= 7) return true;                           // pont du spawn
        if (Math.hypot(x + 50, z) <= 9) return true;                                         // parvis du portail du spawn
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
        computeMasks(c, HubBuilder::footprint);
    }

    /** Masques de l'ile pour l'emprise donnee (le hub, ou le marche du spawn). */
    static void computeMasks(HubCtx c, java.util.function.BiPredicate<Integer, Integer> footprint) {
        int w = HubCtx.X1 - HubCtx.X0 + 1;
        int d = HubCtx.Z1 - HubCtx.Z0 + 1;
        c.foot = new boolean[w][d];
        c.isle = new boolean[w][d];
        c.edge = new float[w][d];
        for (int x = HubCtx.X0; x <= HubCtx.X1; x++) for (int z = HubCtx.Z0; z <= HubCtx.Z1; z++) c.foot[x - HubCtx.X0][z - HubCtx.Z0] = footprint.test(x, z);
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

    static void terrain(HubCtx c) {
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int x = HubCtx.X0; x <= HubCtx.X1; x++) {
            for (int z = HubCtx.Z0; z <= HubCtx.Z1; z++) {
                if (!c.in(c.isle, x, z)) continue;
                float e = c.edge[x - HubCtx.X0][z - HubCtx.Z0];
                boolean f = c.in(c.foot, x, z);
                int depth = Mth.clamp(Math.round(3F + 3.8F * (float) Math.sqrt(e) + 7F * (c.noise(x, z, 6F, 1) - 0.5F)), 4, 34);
                if (c.thin && e > 9F) depth = Math.min(depth, 9);
                int hill = f || c.thin ? 0 : Math.round(2.6F * c.noise(x, z, 7F, 2) * Math.min(1F, (e - 1.2F) / 3F));
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

    static void lamp(HubCtx c, int x, int z) {
        for (int y = 0; y <= 3; y++) c.set(x, y, z, Blocks.SANDSTONE_WALL);
        c.set(x, 4, z, Blocks.LANTERN);
    }

    // ================================================================== textes en police Aeloria

    static void text(HubCtx c, double x, double y, double z, String text, String color, float scale) {
        UUID id = stable("text:" + textIndex++);
        String snbt = String.format(Locale.ROOT, "{text:{text:\"%s\",font:\"mmorpg:aeloria\",color:\"%s\"},billboard:\"center\",background:0,shadow:true,"
                + "see_through:false,line_width:600,Tags:[\"%s\"],UUID:[I;%d,%d,%d,%d],transformation:{left_rotation:[0f,0f,0f,1f],right_rotation:[0f,0f,0f,1f],"
                + "translation:[0f,0f,0f],scale:[%.2ff,%.2ff,%.2ff]}}", text.replace("\"", "\\\""), color, TAG,
                (int) (id.getMostSignificantBits() >> 32), (int) id.getMostSignificantBits(), (int) (id.getLeastSignificantBits() >> 32),
                (int) id.getLeastSignificantBits(), scale, scale, scale);
        CommandSourceStack src = c.L.getServer().createCommandSourceStack().withLevel(c.L).withSuppressedOutput();
        c.L.getServer().getCommands().performPrefixedCommand(src,
                String.format(Locale.ROOT, "summon minecraft:text_display %.2f %.2f %.2f %s", c.O.getX() + x + 0.5, c.O.getY() + y, c.O.getZ() + z + 0.5, snbt));
    }

    /**
     * Retire les restes de l'ancien marche (textes flottants et PNJ) a l'ouest de la place, la ou il se trouvait avant d'avoir sa propre
     * dimension : le hub n'a plus rien a l'ouest du pont, au-dela de 52 blocs. Les entites ne sont trouvees que dans les troncons charges ;
     * l'appel est donc repete tant que des joueurs sont dans le hub. @return le nombre d'entites retirees
     */
    static int cleanLegacyMarket(net.minecraft.server.MinecraftServer server) {
        HubData hub = HubData.get(server);
        ServerLevel level = hub.exists() ? hub.level(server) : null;
        if (level == null) return 0;
        int n = 0;
        for (int i = 0; i < hub.count(); i++) {
            BlockPos o = hub.origin(i);
            AABB box = new AABB(o.getX() - 170, o.getY() - 80, o.getZ() - 140, o.getX() - 52.5, o.getY() + 90, o.getZ() + 140);
            for (Entity e : level.getEntities((Entity) null, box, x -> x instanceof com.mmorpg.entity.NpcEntity
                    || (x instanceof net.minecraft.world.entity.Display.TextDisplay && x.entityTags().contains(TAG)))) {
                e.discard();
                n++;
            }
        }
        if (n > 0) com.mmorpg.MMORPG.LOGGER.info("[MMORPG] Hub : {} reste(s) de l'ancien marche retire(s) (textes et PNJ)", n);
        return n;
    }

    /**
     * Fait reapparaitre les PNJ, mannequins et textes du hub qui manquent (identifiants stables : ceux qui existent deja sont ignores).
     * Sert a reparer un hub dont les entites ont ete retirees sans le reconstruire.
     */
    public static void respawn(ServerLevel level, BlockPos origin) {
        HubCtx c = new HubCtx(level, origin.immutable());
        instance = HubData.get(level.getServer()).indexAt(origin.getX());
        textIndex = 0;
        texts(c);
        entities(c);
        instance = 0;
    }

    private static void texts(HubCtx c) {
        text(c, 0, 33, 36, "AELORIA", "#FFD060", 12F);
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
                || (x instanceof com.mmorpg.entity.NpcEntity n && ("maitre_classes".equals(n.group()) || isStable(n.getUUID()))))) {
            if (e instanceof Display.TextDisplay && !e.entityTags().contains(TAG)) continue;
            e.discard();
        }
    }

    private static void entities(HubCtx c) {
        var master = ModEntities.NPC.get().create(c.L, EntitySpawnReason.COMMAND);
        if (master != null) {
            BlockPos mp = HubLayout.pos(c.O, HubLayout.CLASS_MASTER);
            master.snapTo(mp.getX() + 0.5, mp.getY(), mp.getZ() + 0.5, 0F, 0F);
            master.setYHeadRot(0F);
            master.setup(com.mmorpg.entity.NpcEntity.Role.QUETES, "Maître des classes", 2, "maitre_classes");
            master.setUUID(stable("npc:maitre"));
            c.L.addFreshEntity(master);
        }
        for (int k = 0; k < 4; k++) {
            PlayerClass cls = CLASSES[k];
            int[] s = HubLayout.classSpot(cls);
            spawnMaster(c, k, s[0], 1, s[2], HubLayout.classYaw(cls));
        }
        spawnMaster(c, ClassMasterEntity.EPREUVE, HubLayout.TRIAL_MASTER[0], 0, HubLayout.TRIAL_MASTER[2], 90F);
        for (int i = 0; i < HubLayout.DUMMIES_ROOM.length; i++) spawnDummy(c, HubLayout.DUMMIES_ROOM[i], "room:" + i);
        for (int i = 0; i < HubLayout.DUMMIES_YARD.length; i++) spawnDummy(c, HubLayout.DUMMIES_YARD[i], "yard:" + i);
    }

    private static void spawnMaster(HubCtx c, int kind, int x, int y, int z, float yaw) {
        ClassMasterEntity npc = ModEntities.CLASS_MASTER.get().create(c.L, EntitySpawnReason.COMMAND);
        if (npc == null) return;
        npc.snapTo(c.O.getX() + x + 0.5, c.O.getY() + y, c.O.getZ() + z + 0.5, yaw, 0F);
        npc.setYHeadRot(yaw);
        npc.setYBodyRot(yaw);
        npc.setup(kind);
        npc.setUUID(stable("master:" + kind));
        c.L.addFreshEntity(npc);
    }

    /** Une case de mannequin du hub se trouve-t-elle a cet endroit (a 1,5 bloc pres) ? */
    public static boolean isDummySlot(BlockPos origin, double x, double z) {
        for (int[][] set : new int[][][]{HubLayout.DUMMIES_ROOM, HubLayout.DUMMIES_YARD}) {
            for (int[] d : set) {
                if (Math.abs(x - (origin.getX() + d[0] + 0.5)) < 1.5 && Math.abs(z - (origin.getZ() + d[1] + 0.5)) < 1.5) return true;
            }
        }
        return false;
    }

    private static void spawnDummy(HubCtx c, int[] d, String key) {
        TrainingDummyEntity e = ModEntities.TRAINING_DUMMY.get().create(c.L, EntitySpawnReason.COMMAND);
        if (e == null) return;
        e.snapTo(c.O.getX() + d[0] + 0.5, c.O.getY(), c.O.getZ() + d[1] + 0.5, d[3], 0F);
        e.setup(d[2]);
        e.setUUID(stable("dummy:" + key));
        c.L.addFreshEntity(e);
    }
}
