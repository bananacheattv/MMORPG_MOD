package com.mmorpg.registry;

import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Blocs de decor « Aeloria » (marbre, pierres, cristaux, tuiles, bois ancien...) utilises par le hub et disponibles pour les
 * batisseurs (onglet « Aeloria : batisseur »). Textures et modeles : tools/gen_decor.py, dont la liste doit rester identique.
 */
public final class DecorBlocks {
    private DecorBlocks() {
    }

    private record Mat(String id, boolean variants, boolean pillar, int light, SoundType sound, MapColor color, float hardness) {
    }

    private static final List<Mat> MATERIALS = List.of(
            new Mat("marbre", true, false, 0, SoundType.STONE, MapColor.QUARTZ, 1.5F),
            new Mat("marbre_poli", true, false, 0, SoundType.STONE, MapColor.QUARTZ, 1.5F),
            new Mat("marbre_dore", false, false, 0, SoundType.STONE, MapColor.GOLD, 1.8F),
            new Mat("colonne_marbre", false, true, 0, SoundType.STONE, MapColor.QUARTZ, 1.8F),
            new Mat("pierre_azur", true, false, 0, SoundType.STONE, MapColor.COLOR_LIGHT_BLUE, 1.6F),
            new Mat("pierre_solaire", true, false, 0, SoundType.STONE, MapColor.SAND, 1.6F),
            new Mat("dalle_runique", false, false, 7, SoundType.DEEPSLATE_TILES, MapColor.COLOR_BLUE, 2.2F),
            new Mat("cristal_azur", false, false, 13, SoundType.AMETHYST, MapColor.DIAMOND, 1.2F),
            new Mat("cristal_arcane", false, false, 13, SoundType.AMETHYST, MapColor.COLOR_PURPLE, 1.2F),
            new Mat("cristal_ambre", false, false, 13, SoundType.AMETHYST, MapColor.COLOR_ORANGE, 1.2F),
            new Mat("tuile_azur", true, false, 0, SoundType.STONE, MapColor.WARPED_NYLIUM, 1.2F),
            new Mat("tuile_pourpre", true, false, 0, SoundType.STONE, MapColor.COLOR_PURPLE, 1.2F),
            new Mat("tuile_ecarlate", true, false, 0, SoundType.STONE, MapColor.COLOR_RED, 1.2F),
            new Mat("tuile_sylve", true, false, 0, SoundType.STONE, MapColor.COLOR_GREEN, 1.2F),
            new Mat("tuile_givre", true, false, 0, SoundType.STONE, MapColor.ICE, 1.2F),
            new Mat("bois_ancien", true, false, 0, SoundType.WOOD, MapColor.COLOR_BROWN, 1.6F),
            new Mat("or_ouvrage", true, false, 0, SoundType.METAL, MapColor.GOLD, 2.2F),
            new Mat("roche_celeste", true, false, 0, SoundType.STONE, MapColor.STONE, 2.0F),
            new Mat("fer_noirci", true, false, 0, SoundType.METAL, MapColor.METAL, 2.4F));

    private static final Map<String, DeferredBlock<? extends Block>> BY_ID = new LinkedHashMap<>();
    public static final List<DeferredItem<BlockItem>> ITEMS = new ArrayList<>();

    static {
        for (Mat m : MATERIALS) {
            DeferredBlock<? extends Block> full;
            if (m.pillar()) {
                full = ModBlocks.BLOCKS.registerBlock(m.id(), RotatedPillarBlock::new, p -> props(p, m));
            } else {
                full = ModBlocks.BLOCKS.registerBlock(m.id(), Block::new, p -> props(p, m));
            }
            BY_ID.put(m.id(), full);
            ITEMS.add(ModItems.ITEMS.registerSimpleBlockItem(m.id(), full));
            if (m.variants()) {
                DeferredBlock<? extends Block> base = full;
                DeferredBlock<StairBlock> stairs = ModBlocks.BLOCKS.registerBlock(m.id() + "_stairs",
                        p -> new StairBlock(base.get().defaultBlockState(), p), p -> props(p, m));
                DeferredBlock<SlabBlock> slab = ModBlocks.BLOCKS.registerBlock(m.id() + "_slab", SlabBlock::new, p -> props(p, m));
                BY_ID.put(m.id() + "_stairs", stairs);
                BY_ID.put(m.id() + "_slab", slab);
                ITEMS.add(ModItems.ITEMS.registerSimpleBlockItem(m.id() + "_stairs", stairs));
                ITEMS.add(ModItems.ITEMS.registerSimpleBlockItem(m.id() + "_slab", slab));
            }
        }
    }

    static {
        for (DecorPropList.Prop p : DecorPropList.ALL) {
            DeferredBlock<? extends Block> block = ModBlocks.BLOCKS.registerBlock(p.id(),
                    props -> new com.mmorpg.block.DecorPropBlock(props, p.x0(), p.y0(), p.z0(), p.x1(), p.y1(), p.z1()),
                    props -> {
                        Block.Properties q = props.mapColor(MapColor.WOOD).strength(1.2F, 4F).sound(p.sound()).noOcclusion();
                        return p.light() > 0 ? q.lightLevel(st -> p.light()) : q;
                    });
            BY_ID.put(p.id(), block);
            ITEMS.add(ModItems.ITEMS.registerSimpleBlockItem(p.id(), block));
        }
    }

    /** Decor dessine par un modele anime (bloc-entite) : cle du modele -> boite de collision. */
    public static final Map<String, DeferredBlock<com.mmorpg.block.DecorModelBlock>> MODEL_BLOCKS = new LinkedHashMap<>();
    public static final net.neoforged.neoforge.registries.DeferredHolder<net.minecraft.world.level.block.entity.BlockEntityType<?>, net.minecraft.world.level.block.entity.BlockEntityType<com.mmorpg.block.DecorModelBlock.Entity>> MODEL_ENTITY =
            ModBlocks.BLOCK_ENTITIES.register("decor_modele", () -> new net.minecraft.world.level.block.entity.BlockEntityType<>(com.mmorpg.block.DecorModelBlock.Entity::new,
                    MODEL_BLOCKS.values().stream().map(DeferredBlock::get).toArray(Block[]::new)));

    static {
        Object[][] models = {{"drapeau_aeloria", Block.box(6, 0, 6, 10, 16, 10)}, {"camp_aeloria", Block.box(0, 0, 0, 16, 6, 16)}};
        for (Object[] m : models) {
            String id = (String) m[0];
            net.minecraft.world.phys.shapes.VoxelShape shape = (net.minecraft.world.phys.shapes.VoxelShape) m[1];
            DeferredBlock<com.mmorpg.block.DecorModelBlock> block = ModBlocks.BLOCKS.registerBlock(id,
                    props -> new com.mmorpg.block.DecorModelBlock(props, shape),
                    props -> props.mapColor(MapColor.WOOD).strength(1.2F, 4F).sound(SoundType.WOOD).noOcclusion());
            MODEL_BLOCKS.put(id, block);
            BY_ID.put(id, block);
            ITEMS.add(ModItems.ITEMS.registerSimpleBlockItem(id, block));
        }
    }

    /** Decors 3D de la citadelle (modeles Blockbench a l'echelle x3, tools/gen_decor_bb.py) : meme moteur de rendu que ci-dessus. */
    static {
        for (DecorCitadelList.Entry e : java.util.stream.Stream.concat(DecorCitadelList.ALL.stream(), DecorBlockbenchList.ALL.stream()).toList()) {
            net.minecraft.world.phys.shapes.VoxelShape shape = Block.box(e.x0(), e.y0(), e.z0(), e.x1(), e.y1(), e.z1());
            DeferredBlock<com.mmorpg.block.DecorModelBlock> block = ModBlocks.BLOCKS.registerBlock(e.id(),
                    props -> new com.mmorpg.block.DecorModelBlock(props, shape),
                    props -> {
                        Block.Properties q = props.mapColor(MapColor.STONE).strength(1.5F, 6F).sound(SoundType.STONE).noOcclusion();
                        return e.light() > 0 ? q.lightLevel(st -> e.light()) : q;
                    });
            MODEL_BLOCKS.put(e.id(), block);
            BY_ID.put(e.id(), block);
            ITEMS.add(ModItems.ITEMS.registerSimpleBlockItem(e.id(), block));
        }
    }

    private static Block.Properties props(Block.Properties p, Mat m) {
        Block.Properties q = p.mapColor(m.color()).strength(m.hardness(), 6F).sound(m.sound());
        if (m.light() > 0) q = q.lightLevel(s -> m.light());
        return q;
    }

    /** Charge la classe (enregistrement des blocs) avant les registres. */
    public static void init() {
    }

    public static boolean has(String id) {
        return BY_ID.containsKey(id);
    }

    public static Block get(String id) {
        return BY_ID.get(id).get();
    }

    public static net.minecraft.world.level.block.state.BlockState state(String id) {
        return get(id).defaultBlockState();
    }
}
