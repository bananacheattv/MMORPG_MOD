package com.mmorpg.crafting;

import com.mmorpg.item.ItemDefs;
import com.mmorpg.rpg.PlayerClass;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Recettes de la Forge Arcanique. Chaque recette consomme des materiaux (butin de monstres et de boss),
 * des pieces d'or et demande un niveau minimum.
 */
public final class ForgeRecipes {
    public enum Category {
        ARMES("Armes"), ARMURES("Armures"), MATERIAUX("Matériaux"), CONSOMMABLES("Consommables"),
        INVOCATIONS("Invocations"), FAMILIERS("Familiers");

        public final String label;

        Category(String label) {
            this.label = label;
        }
    }

    public record Ingredient(String item, int count) {
        public Item resolve() {
            return resolveItem(item);
        }
    }

    public record Recipe(String id, Category category, String result, int count, List<Ingredient> ingredients, int gold,
                         int level, PlayerClass playerClass) {
        public Item resultItem() {
            return resolveItem(result);
        }
    }

    private static final Map<String, Recipe> RECIPES = new LinkedHashMap<>();

    public static Item resolveItem(String id) {
        Identifier rl = Identifier.parse(id.contains(":") ? id : "mmorpg:" + id);
        return BuiltInRegistries.ITEM.getValue(rl);
    }

    private static void add(Category cat, String result, int count, int gold, int level, PlayerClass cls, Object... ingredients) {
        List<Ingredient> list = new ArrayList<>();
        for (int i = 0; i + 1 < ingredients.length; i += 2) {
            list.add(new Ingredient((String) ingredients[i], (Integer) ingredients[i + 1]));
        }
        String id = RECIPES.containsKey(result) ? result + "_" + RECIPES.size() : result;
        RECIPES.put(id, new Recipe(id, cat, result, count, Collections.unmodifiableList(list), gold, level, cls));
    }

    private static void weapon(String id, int gold, Object... ingredients) {
        ItemDefs.WeaponDef def = ItemDefs.WEAPONS.stream().filter(w -> w.id().equals(id)).findFirst().orElseThrow();
        add(Category.ARMES, id, 1, gold, def.level(), def.cls(), ingredients);
    }

    /** Recettes d'un set complet : la quantite de materiaux depend de la piece (casque 5, plastron 8, jambieres 7, bottes 4). */
    private static void armorSet(String setId, int goldPerPiece, String matA, int baseA, String matB, int baseB, String special) {
        ItemDefs.ArmorSet set = ItemDefs.SETS.get(setId);
        int[] weight = {5, 8, 7, 4};
        for (int i = 0; i < 4; i++) {
            List<Object> ing = new ArrayList<>();
            ing.add(matA);
            ing.add(Math.max(1, baseA * weight[i] / 5));
            if (matB != null) {
                ing.add(matB);
                ing.add(Math.max(1, baseB * weight[i] / 5));
            }
            if (special != null && i == 1) {
                ing.add(special);
                ing.add(1);
            }
            add(Category.ARMURES, setId + "_" + ItemDefs.PIECES[i], 1, goldPerPiece * weight[i] / 5, set.level(), set.cls(), ing.toArray());
        }
    }

    static {
        PlayerClass N = PlayerClass.NONE;
        // ---------------------------------------------------------------- materiaux intermediaires
        add(Category.MATERIAUX, "lingot_mithril", 1, 5, 20, N, "minecraft:iron_ingot", 2, "acier_orc", 2, "poussiere_ame", 1);
        add(Category.MATERIAUX, "lingot_adamantite", 1, 20, 55, N, "lingot_mithril", 1, "noyau_flamme", 2, "eclat_givre", 1, "minecraft:diamond", 1);
        add(Category.MATERIAUX, "tissu_enchante", 2, 4, 15, N, "minecraft:string", 4, "poussiere_ame", 2, "minecraft:lapis_lazuli", 1);
        add(Category.MATERIAUX, "cuir_renforce", 2, 2, 5, N, "minecraft:leather", 3, "fourrure_sombre", 1);
        add(Category.MATERIAUX, "pierre_amelioration", 1, 10, 1, N, "ferraille_gobeline", 4, "minecraft:iron_ingot", 2, "minecraft:lapis_lazuli", 2);
        add(Category.MATERIAUX, "pierre_amelioration_sup", 1, 60, 40, N, "pierre_amelioration", 4, "cristal_arcanique", 1);
        add(Category.MATERIAUX, "piece_or", 5, 0, 1, N, "minecraft:gold_ingot", 1);
        add(Category.MATERIAUX, "trefle_chance", 1, 40, 10, N, "minecraft:fern", 3, "minecraft:lapis_lazuli", 3, "poussiere_ame", 1);
        add(Category.MATERIAUX, "trefle_quatre_feuilles", 1, 150, 30, N, "trefle_chance", 3, "minecraft:emerald", 2);
        add(Category.MATERIAUX, "trefle_dore", 1, 600, 50, N, "trefle_quatre_feuilles", 2, "minecraft:gold_block", 1, "cristal_arcanique", 1);
        add(Category.MATERIAUX, "trefle_celeste", 1, 1500, 70, N, "trefle_dore", 2, "essence_neant", 1, "minecraft:diamond", 2);
        add(Category.MATERIAUX, "trefle_divin", 1, 4000, 90, N, "trefle_celeste", 2, "essence_neant", 2, "minecraft:netherite_ingot", 1);

        // ---------------------------------------------------------------- armes : Guerrier
        weapon("epee_recrue", 3, "minecraft:iron_ingot", 3, "minecraft:stick", 1);
        weapon("lame_runique", 20, "ferraille_gobeline", 4, "oreille_gobelin", 3, "minecraft:iron_ingot", 2, "minecraft:lapis_lazuli", 4);
        weapon("hache_berserker", 60, "acier_orc", 4, "defense_orc", 3, "couronne_roi_gobelin", 1);
        weapon("epee_seigneur_guerre", 150, "lingot_mithril", 4, "os_maudit", 4, "phylactere_liche", 1);
        weapon("lame_demoniaque", 400, "lingot_adamantite", 4, "noyau_flamme", 3, "coeur_infernal", 1);
        weapon("excalibur_celeste", 1000, "fragment_divin", 3, "lingot_adamantite", 4, "essence_neant", 2, "coeur_glace_eternelle", 1);
        // Mage
        weapon("baton_apprenti", 3, "minecraft:stick", 3, "minecraft:lapis_lazuli", 2, "minecraft:glowstone_dust", 1);
        weapon("sceptre_givre", 20, "oreille_gobelin", 3, "croc_loup", 2, "minecraft:lapis_lazuli", 3, "minecraft:quartz", 2);
        weapon("baton_flammes", 60, "poussiere_ame", 3, "os_maudit", 2, "couronne_roi_gobelin", 1, "minecraft:blaze_rod", 2);
        weapon("baton_arcanique", 150, "lingot_mithril", 2, "poussiere_ame", 4, "phylactere_liche", 1, "minecraft:amethyst_shard", 4);
        weapon("sceptre_neant", 400, "cristal_arcanique", 3, "eclat_givre", 2, "coeur_infernal", 1);
        weapon("baton_archimage", 1000, "fragment_divin", 3, "essence_neant", 3, "cristal_arcanique", 2, "coeur_glace_eternelle", 1);
        // Archer
        weapon("arc_chasseur", 3, "minecraft:stick", 3, "minecraft:string", 3, "minecraft:leather", 1);
        weapon("arc_elfique", 20, "cuir_renforce", 2, "croc_loup", 3, "minecraft:string", 3);
        weapon("arc_tempete", 60, "os_maudit", 3, "acier_orc", 2, "couronne_roi_gobelin", 1, "cuir_renforce", 2);
        weapon("arc_faucon", 150, "lingot_mithril", 2, "fourrure_sombre", 3, "phylactere_liche", 1, "minecraft:feather", 4);
        weapon("arc_spectral", 400, "eclat_givre", 3, "cristal_arcanique", 2, "coeur_infernal", 1);
        weapon("arc_aube_divine", 1000, "fragment_divin", 3, "lingot_adamantite", 2, "essence_neant", 2, "coeur_glace_eternelle", 1);
        // Tank
        weapon("masse_garde", 3, "minecraft:iron_ingot", 3, "minecraft:stick", 2, "minecraft:cobblestone", 2);
        weapon("marteau_bastion", 20, "ferraille_gobeline", 4, "minecraft:iron_ingot", 3, "croc_loup", 2);
        weapon("marteau_colosse", 60, "acier_orc", 5, "defense_orc", 2, "couronne_roi_gobelin", 1);
        weapon("masse_gardien", 150, "lingot_mithril", 4, "minecraft:gold_block", 2, "phylactere_liche", 1);
        weapon("marteau_titan", 400, "lingot_adamantite", 4, "noyau_flamme", 2, "coeur_infernal", 1);
        weapon("marteau_egide", 1000, "fragment_divin", 3, "lingot_adamantite", 4, "coeur_glace_eternelle", 1, "essence_neant", 2);

        // ---------------------------------------------------------------- armures
        armorSet("acier_soldat", 10, "minecraft:iron_ingot", 3, "ferraille_gobeline", 2, null);
        armorSet("berserker", 40, "acier_orc", 3, "defense_orc", 1, "couronne_roi_gobelin");
        armorSet("seigneur_guerre", 100, "lingot_mithril", 3, "noyau_flamme", 1, "phylactere_liche");
        armorSet("dieu_guerre", 300, "lingot_adamantite", 3, "essence_neant", 1, "coeur_infernal");
        armorSet("apprenti", 10, "minecraft:white_wool", 3, "oreille_gobelin", 2, null);
        armorSet("sorcier", 40, "tissu_enchante", 3, "os_maudit", 1, "couronne_roi_gobelin");
        armorSet("archimage", 100, "tissu_enchante", 3, "eclat_givre", 1, "phylactere_liche");
        armorSet("avatar_arcanique", 300, "tissu_enchante", 3, "cristal_arcanique", 1, "coeur_infernal");
        armorSet("chasseur", 10, "minecraft:leather", 3, "croc_loup", 1, null);
        armorSet("rodeur", 40, "cuir_renforce", 3, "os_maudit", 1, "couronne_roi_gobelin");
        armorSet("tireur_elite", 100, "cuir_renforce", 3, "lingot_mithril", 1, "phylactere_liche");
        armorSet("sylvestre", 300, "cuir_renforce", 3, "cristal_arcanique", 1, "coeur_infernal");
        armorSet("garde", 10, "minecraft:iron_ingot", 3, "minecraft:cobblestone", 3, null);
        armorSet("gardien", 40, "acier_orc", 3, "minecraft:iron_block", 1, "couronne_roi_gobelin");
        armorSet("paladin", 100, "lingot_mithril", 3, "minecraft:gold_ingot", 2, "phylactere_liche");
        armorSet("titan", 300, "lingot_adamantite", 3, "noyau_flamme", 1, "coeur_infernal");
        armorSet("neant_primordial", 800, "fragment_divin", 1, "essence_neant", 2, "coeur_glace_eternelle");

        // ---------------------------------------------------------------- consommables
        add(Category.CONSOMMABLES, "potion_soin_mineure", 2, 2, 1, N, "minecraft:glass_bottle", 2, "minecraft:apple", 1, "oreille_gobelin", 1);
        add(Category.CONSOMMABLES, "potion_soin", 2, 10, 20, N, "minecraft:glass_bottle", 2, "minecraft:glistering_melon_slice", 1, "os_maudit", 1);
        add(Category.CONSOMMABLES, "potion_soin_majeure", 2, 40, 50, N, "minecraft:glass_bottle", 2, "minecraft:golden_apple", 1, "noyau_flamme", 1);
        add(Category.CONSOMMABLES, "potion_mana_mineure", 2, 2, 1, N, "minecraft:glass_bottle", 2, "minecraft:lapis_lazuli", 2);
        add(Category.CONSOMMABLES, "potion_mana", 2, 10, 20, N, "minecraft:glass_bottle", 2, "poussiere_ame", 1, "minecraft:lapis_lazuli", 2);
        add(Category.CONSOMMABLES, "potion_mana_majeure", 2, 40, 50, N, "minecraft:glass_bottle", 2, "eclat_givre", 1, "cristal_arcanique", 1);
        add(Category.CONSOMMABLES, "elixir_experience", 1, 50, 10, N, "minecraft:glass_bottle", 1, "minecraft:experience_bottle", 5, "poussiere_ame", 2);
        add(Category.CONSOMMABLES, "parchemin_teleportation", 2, 5, 1, N, "minecraft:paper", 2, "minecraft:ender_pearl", 1);
        add(Category.CONSOMMABLES, "parchemin_oubli", 1, 50, 10, N, "minecraft:paper", 1, "poussiere_ame", 2, "minecraft:amethyst_shard", 2);
        add(Category.CONSOMMABLES, "orbe_renaissance", 1, 150, 10, N, "minecraft:ender_eye", 1, "poussiere_ame", 4, "minecraft:diamond", 2);
        add(Category.CONSOMMABLES, "coffre_cosmetique", 1, 100, 30, N, "cristal_arcanique", 1, "minecraft:gold_ingot", 2, "minecraft:chest", 1);

        // ---------------------------------------------------------------- invocations
        add(Category.INVOCATIONS, "sceau_roi_gobelin", 1, 20, 15, N, "oreille_gobelin", 8, "ferraille_gobeline", 6, "minecraft:gold_ingot", 2);
        add(Category.INVOCATIONS, "grimoire_interdit", 1, 60, 35, N, "os_maudit", 6, "poussiere_ame", 4, "minecraft:book", 1, "couronne_roi_gobelin", 1);
        add(Category.INVOCATIONS, "braise_eternelle", 1, 120, 55, N, "noyau_flamme", 6, "defense_orc", 2, "phylactere_liche", 1);
        add(Category.INVOCATIONS, "cristal_glacial", 1, 200, 70, N, "eclat_givre", 8, "cristal_arcanique", 4, "coeur_infernal", 1);
        add(Category.INVOCATIONS, "oeil_neant", 1, 400, 90, N, "essence_neant", 6, "coeur_glace_eternelle", 1, "minecraft:ender_eye", 2, "cristal_arcanique", 4);

        // ---------------------------------------------------------------- familiers
        add(Category.FAMILIERS, "oeuf_feu_follet", 1, 20, 5, N, "poussiere_ame", 4, "minecraft:egg", 1, "minecraft:glowstone_dust", 2);
        add(Category.FAMILIERS, "oeuf_slime", 1, 20, 5, N, "minecraft:slime_ball", 8, "minecraft:egg", 1, "oreille_gobelin", 2);
        add(Category.FAMILIERS, "oeuf_chouette", 1, 60, 25, N, "minecraft:feather", 6, "minecraft:egg", 1, "poussiere_ame", 2, "minecraft:book", 1);
        add(Category.FAMILIERS, "oeuf_loup_spectral", 1, 80, 30, N, "croc_loup", 6, "fourrure_sombre", 4, "minecraft:egg", 1, "eclat_givre", 1);
        add(Category.FAMILIERS, "oeuf_golem", 1, 80, 30, N, "acier_orc", 4, "cristal_arcanique", 1, "minecraft:egg", 1);
        add(Category.FAMILIERS, "oeuf_fee", 1, 200, 50, N, "cristal_arcanique", 4, "poussiere_ame", 4, "minecraft:egg", 1, "minecraft:pink_petals", 4);
        add(Category.FAMILIERS, "oeuf_phenix", 1, 500, 70, N, "noyau_flamme", 6, "coeur_infernal", 1, "minecraft:egg", 1, "minecraft:gold_block", 2);
        add(Category.FAMILIERS, "oeuf_dragonnet", 1, 1500, 95, N, "fragment_divin", 3, "essence_neant", 4, "minecraft:egg", 1);
    }

    private ForgeRecipes() {
    }

    public static Recipe get(String id) {
        return RECIPES.get(id);
    }

    public static List<Recipe> all() {
        return new ArrayList<>(RECIPES.values());
    }

    public static List<Recipe> byCategory(Category cat) {
        List<Recipe> l = new ArrayList<>();
        for (Recipe r : RECIPES.values()) {
            if (r.category() == cat) l.add(r);
        }
        return l;
    }

    /** Amelioration : chance de reussite pour passer au niveau +n (index n-1). */
    /** Chance de reussite (%) pour atteindre +1 ... +20, sans trefle. */
    public static final int[] UPGRADE_CHANCE = {100, 95, 90, 85, 75, 70, 65, 60, 50, 45, 40, 35, 30, 25, 20, 15, 12, 10, 8, 5};
    /** Paliers d'amelioration de 4 niveaux : +1 a +4, +5 a +8, +9 a +12, +13 a +16, +17 a +20. */
    public static final int TIER_SIZE = 4;
    /** Trefle de chaque palier : il garantit la reussite, mais seulement dans son palier. */
    public static final String[] CLOVERS = {"trefle_chance", "trefle_quatre_feuilles", "trefle_dore", "trefle_celeste", "trefle_divin"};

    /** Palier (0 a 4) d'un niveau vise. */
    public static int tier(int targetLevel) {
        return Math.max(0, Math.min(CLOVERS.length - 1, (targetLevel - 1) / TIER_SIZE));
    }

    /** Trefle qui garantit l'amelioration vers {@code targetLevel}. */
    public static String cloverFor(int targetLevel) {
        return CLOVERS[tier(targetLevel)];
    }

    /** Niveaux (premier, dernier) couverts par un trefle, ou null si ce n'est pas un trefle. */
    public static int[] cloverRange(String id) {
        for (int i = 0; i < CLOVERS.length; i++) {
            if (CLOVERS[i].equals(id)) return new int[]{i * TIER_SIZE + 1, (i + 1) * TIER_SIZE};
        }
        return null;
    }

    public static int upgradeGold(int targetLevel) {
        return targetLevel <= 8 ? targetLevel * 10 : targetLevel <= 16 ? targetLevel * 35 : targetLevel * 80;
    }

    /** Pierre d'amelioration pour +1 a +8, Pierre superieure pour +9 a +20. */
    public static boolean upgradeNeedsSuperior(int targetLevel) {
        return targetLevel > 8;
    }
}
