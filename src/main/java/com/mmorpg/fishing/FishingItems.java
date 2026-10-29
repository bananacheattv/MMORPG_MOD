package com.mmorpg.fishing;

import com.mmorpg.item.AeloriaRodItem;
import com.mmorpg.item.RpgMaterialItem;
import com.mmorpg.item.TreasureChestItem;
import com.mmorpg.registry.ModItems;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** Objets de la peche : cannes, poissons, dechets, coffres et ingredients marins (enregistres comme {@link com.mmorpg.farm.FarmCrops}). */
public final class FishingItems {
    private FishingItems() {
    }

    /** Tous les objets de la peche, dans l'ordre d'affichage de l'onglet creatif. */
    public static final List<DeferredItem<? extends Item>> ALL = new ArrayList<>();
    public static final Map<Integer, DeferredItem<AeloriaRodItem>> RODS = new LinkedHashMap<>();
    public static final Map<String, DeferredItem<? extends Item>> CATCHES = new LinkedHashMap<>();

    static {
        for (FishCatalog.Rod rod : FishCatalog.RODS) {
            DeferredItem<AeloriaRodItem> item = ModItems.ITEMS.registerItem(rod.id(), p -> new AeloriaRodItem(p.stacksTo(1).durability(rod.durability()), rod));
            RODS.put(rod.tier(), item);
            ALL.add(item);
            ModItems.BY_ID.put(rod.id(), item);
        }
        for (FishCatalog.Entry e : FishCatalog.ALL) {
            DeferredItem<? extends Item> item;
            if (e.kind() == FishCatalog.Kind.TRESOR) {
                int rank = FishCatalog.of(FishCatalog.Kind.TRESOR).indexOf(e);
                item = ModItems.ITEMS.registerItem(e.id(), p -> new TreasureChestItem(p.stacksTo(16), e, rank));
            } else {
                item = ModItems.ITEMS.registerItem(e.id(), p -> new RpgMaterialItem(p, e.rarity(), e.fish() ? "Poisson" : "Déchet", describe(e)));
            }
            CATCHES.put(e.id(), item);
            ALL.add(item);
            ModItems.BY_ID.put(e.id(), item);
        }
        for (FishCatalog.Ingredient i : FishCatalog.INGREDIENTS) {
            DeferredItem<RpgMaterialItem> item = ModItems.ITEMS.registerItem(i.id(), p -> new RpgMaterialItem(p, i.rarity(), "Matériau marin", i.lore()));
            ALL.add(item);
            ModItems.BY_ID.put(i.id(), item);
        }
    }

    /** Charge la classe (enregistrement des objets) avant les registres. */
    public static void init() {
    }

    /** Description d'une prise : texte d'ambiance, habitats, moment et taille. */
    public static String describe(FishCatalog.Entry e) {
        if (!e.fish()) return e.lore();
        String habitats = e.habitats().size() >= FishCatalog.Habitat.values().length ? "partout"
                : e.habitats().stream().map(h -> h.label).collect(Collectors.joining(", "));
        return e.lore() + "\nHabitat : " + habitats + " · " + e.moment().label
                + "\nTaille : " + e.minCm() + " à " + e.maxCm() + " cm · Pêcheur niveau " + e.minLevel();
    }
}
