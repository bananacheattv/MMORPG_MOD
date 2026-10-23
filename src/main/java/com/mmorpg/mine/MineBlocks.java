package com.mmorpg.mine;

import com.mmorpg.registry.ModBlocks;
import com.mmorpg.registry.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Minerais des Mines d'Aeloria. Ce sont des blocs « poses » : les casser (avec une pioche) donne directement le lingot et le bloc se
 * transforme en veine epuisee le temps de la repousse (voir {@link MineManager}). Textures et modeles : tools/gen_mine.py.
 */
public final class MineBlocks {
    private MineBlocks() {
    }

    /** @param minLevel niveau minimal du joueur ; @param cooldown temps de repousse en secondes ; @param color couleur des messages */
    public record Ore(String id, String name, int tier, int minLevel, int cooldown, int color, int light, SoundType sound) {
        public String blockId() {
            return "minerai_" + id;
        }

        public String ingotId() {
            return "lingot_" + id;
        }
    }

    public static final List<Ore> ORES = List.of(
            new Ore("verdite", "Verdite", 0, 1, 40, 0x58D670, 5, SoundType.STONE),
            new Ore("argentite", "Argentite", 1, 18, 55, 0xA9C0F0, 6, SoundType.DEEPSLATE),
            new Ore("ambrasite", "Ambrasite", 2, 38, 70, 0xFF9030, 9, SoundType.NETHER_ORE),
            new Ore("glacium", "Glacium", 3, 55, 85, 0x70DCFF, 8, SoundType.GLASS),
            new Ore("neantite", "Néantite", 4, 75, 110, 0xB070FF, 8, SoundType.AMETHYST));

    public static final Map<String, DeferredBlock<MineOreBlock>> ORE_BLOCKS = new LinkedHashMap<>();
    public static final DeferredBlock<Block> EPUISE;
    public static final List<DeferredItem<BlockItem>> ITEMS = new ArrayList<>();

    static {
        for (Ore ore : ORES) {
            DeferredBlock<MineOreBlock> block = ModBlocks.BLOCKS.registerBlock(ore.blockId(), p -> new MineOreBlock(p, ore),
                    p -> p.mapColor(MapColor.STONE).strength(4.0F, 1200F).sound(ore.sound()).lightLevel(s -> ore.light()).requiresCorrectToolForDrops());
            ORE_BLOCKS.put(ore.id(), block);
            ITEMS.add(ModItems.ITEMS.registerSimpleBlockItem(ore.blockId(), block));
        }
        EPUISE = ModBlocks.BLOCKS.registerBlock("veine_epuisee", Block::new,
                p -> p.mapColor(MapColor.DEEPSLATE).strength(-1.0F, 3600000F).sound(SoundType.DEEPSLATE));
        ITEMS.add(ModItems.ITEMS.registerSimpleBlockItem("veine_epuisee", EPUISE));
    }

    /** Charge la classe (enregistrement des blocs) avant les registres. */
    public static void init() {
    }

    public static Ore ore(String id) {
        for (Ore o : ORES) {
            if (o.id().equals(id)) return o;
        }
        return null;
    }

    public static Block block(Ore ore) {
        return ORE_BLOCKS.get(ore.id()).get();
    }
}
