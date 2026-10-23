package com.mmorpg.farm;

import com.mmorpg.registry.ModBlocks;
import com.mmorpg.registry.ModItems;
import com.mmorpg.rpg.Rarity;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cultures d'Aeloria : cinq plantes qui poussent sur une terre labouree, plus lentement quand leur rang monte. Chaque plante a ses
 * graines (niveau minimum pour semer) et sa recolte, qui sert aux potions, aux aliments et a la Forge. La houe du joueur ajoute des
 * recoltes supplementaires selon son palier (voir {@link AeloriaCropBlock}).
 */
public final class FarmCrops {
    private FarmCrops() {
    }

    /** id : suffixe des blocs et des graines ; product : objet recolte ; slow : une pousse sur « slow » tirages aleatoires. */
    public record Crop(String id, String name, String seedId, String seedName, String product, Rarity rarity, int minLevel, int slow, int color, int light) {
        public String blockId() {
            return "culture_" + id;
        }
    }

    public static final List<Crop> CROPS = List.of(
            new Crop("lueur", "Racine de Lueur", "graine_lueur", "Graine de Lueur", "racine_lueur", Rarity.COMMUN, 1, 1, 0x50E6AA, 7),
            new Crop("argent", "Blé d'Argent", "graine_argent", "Graine d'Argent", "ble_argent", Rarity.PEU_COMMUN, 18, 2, 0xC8D2E6, 0),
            new Crop("ambre", "Baie Ambrée", "graine_ambre", "Graine Ambrée", "baie_ambree", Rarity.RARE, 38, 2, 0xFF9632, 5),
            new Crop("givre", "Fleur de Givre", "graine_givre", "Graine de Givre", "fleur_givre", Rarity.EPIQUE, 55, 3, 0x6EDCFF, 8),
            new Crop("neant", "Orchidée du Néant", "graine_neant", "Graine du Néant", "orchidee_neant", Rarity.LEGENDAIRE, 75, 4, 0xAA64FF, 9));

    public static final Map<String, DeferredBlock<AeloriaCropBlock>> BLOCKS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<Item>> SEEDS = new LinkedHashMap<>();
    public static final List<DeferredItem<Item>> SEED_ITEMS = new ArrayList<>();

    static {
        for (Crop c : CROPS) {
            DeferredBlock<AeloriaCropBlock> block = ModBlocks.BLOCKS.registerBlock(c.blockId(), p -> new AeloriaCropBlock(p, c),
                    p -> p.mapColor(MapColor.PLANT).noCollision().randomTicks().instabreak().sound(SoundType.CROP).pushReaction(PushReaction.POPPED)
                            .lightLevel(s -> s.getValue(AeloriaCropBlock.AGE) >= AeloriaCropBlock.MAX_AGE ? c.light() : c.light() / 3));
            BLOCKS.put(c.id(), block);
            DeferredItem<Item> seed = ModItems.ITEMS.registerItem(c.seedId(), p -> new SeedItem(p.useItemDescriptionPrefix(), block, c));
            SEEDS.put(c.id(), seed);
            SEED_ITEMS.add(seed);
            ModItems.BY_ID.put(c.seedId(), seed);
        }
    }

    /** Charge la classe (enregistrement des blocs et des graines) avant les registres. */
    public static void init() {
    }

    public static Crop crop(String id) {
        for (Crop c : CROPS) {
            if (c.id().equals(id)) return c;
        }
        return null;
    }
}
