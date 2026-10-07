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
        add(Category.CONSOMMABLES, "charme_experience", 1, 100, 10, N, "poussiere_ame", 8, "minecraft:lapis_lazuli", 4);
        add(Category.CONSOMMABLES, "cle_aventure", 1, 80, 5, N, "minecraft:iron_ingot", 4, "poussiere_ame", 2);
        add(Category.CONSOMMABLES, "caisse_aventure", 1, 40, 5, N, "minecraft:oak_planks", 8, "ferraille_gobeline", 4);
        add(Category.CONSOMMABLES, "lucky_block", 1, 120, 10, N, "minecraft:gold_ingot", 4, "trefle_chance", 1);
        add(Category.CONSOMMABLES, "charme_chance", 1, 100, 10, N, "trefle_chance", 1, "minecraft:gold_ingot", 2);
        add(Category.FAMILIERS, "nourriture_familier", 4, 5, 1, N, "minecraft:wheat", 4, "minecraft:carrot", 2);
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

        add(Category.MATERIAUX, "alliage_celeste", 2, 200, 90, N, "lingot_adamantite", 3, "cristal_arcanique", 2, "fragment_divin", 1, "eclat_fracture", 1);
        add(Category.MATERIAUX, "bois_enchante", 2, 4, 10, N, "ecorce_ancienne", 3, "seve_lumineuse", 1);
        add(Category.MATERIAUX, "bronze_runique", 2, 20, 30, N, "carapace_cuivre", 3, "minecraft:copper_ingot", 2, "minecraft:lapis_lazuli", 2);
        add(Category.MATERIAUX, "tissu_spectral", 2, 25, 35, N, "soie_araignee", 3, "voile_spectral", 1, "tissu_enchante", 1);
        add(Category.MATERIAUX, "cuir_polaire", 2, 30, 45, N, "fourrure_polaire", 3, "cuir_renforce", 1, "carapace_givree", 1);
        add(Category.MATERIAUX, "acier_stellaire", 1, 120, 75, N, "lingot_adamantite", 1, "poussiere_etoile", 2, "plaque_neant", 1);
        // recettes alternatives avec les materiaux propres aux monstres
        add(Category.MATERIAUX, "bois_enchante", 2, 4, 10, N, "noeud_racine", 1, "roseau_enchante", 2, "mousse_vivante", 2);
        add(Category.MATERIAUX, "cuir_renforce", 2, 2, 5, N, "peau_crapaud", 3, "crin_cendre", 1);
        add(Category.MATERIAUX, "cuir_polaire", 2, 30, 45, N, "queue_renard", 2, "crin_cendre", 2, "cuir_renforce", 1);
        add(Category.MATERIAUX, "tissu_spectral", 2, 25, 35, N, "maillon_spectral", 1, "soie_araignee", 2, "plume_harpie", 1);
        add(Category.MATERIAUX, "bronze_runique", 2, 20, 30, N, "dard_scorpion", 2, "carapace_cuivre", 2, "minecraft:copper_ingot", 1);
        add(Category.MATERIAUX, "acier_stellaire", 1, 120, 75, N, "coeur_etoile", 1, "lingot_adamantite", 1, "poudre_astrale", 2);
        add(Category.MATERIAUX, "pierre_amelioration", 1, 10, 1, N, "phalange_osselet", 4, "ecusson_funeraire", 1, "minecraft:lapis_lazuli", 1);
        add(Category.MATERIAUX, "pierre_amelioration_sup", 1, 60, 40, N, "pierre_amelioration", 2, "joyau_cryptes", 1, "noyau_glace", 1);
        add(Category.MATERIAUX, "trefle_quatre_feuilles", 1, 150, 30, N, "trefle_chance", 2, "chapeau_luisant", 2, "perle_acide", 1);
        add(Category.MATERIAUX, "alliage_celeste", 2, 200, 90, N, "lingot_adamantite", 3, "ecaille_boreale", 1, "pupille_chaos", 1, "plaque_fracturee", 2);
        add(Category.CONSOMMABLES, "potion_soin", 2, 10, 20, N, "minecraft:glass_bottle", 2, "lanterne_luciole", 1, "croc_hydre", 1);
        add(Category.CONSOMMABLES, "potion_mana", 2, 10, 20, N, "minecraft:glass_bottle", 2, "fumee_djinn", 1, "elytre_polaire", 1);
        add(Category.CONSOMMABLES, "elixir_experience", 1, 50, 10, N, "minecraft:glass_bottle", 1, "griffe_vide", 1, "glande_venin", 2, "scarabee_or", 1);
        add(Category.FAMILIERS, "nourriture_familier", 4, 5, 1, N, "defense_ronce", 1, "plume_sylvestre", 1, "minecraft:wheat", 2);
        add(Category.MATERIAUX, "lingot_mithril", 1, 5, 20, N, "minecraft:iron_ingot", 2, "carapace_volcanique", 1, "patte_araignee", 1);
        // ---------------------------------------------------------------- armes : Guerrier
        weapon("epee_recrue", 3, "minecraft:iron_ingot", 3, "minecraft:stick", 1);
        weapon("lame_runique", 20, "ferraille_gobeline", 4, "oreille_gobelin", 3, "minecraft:iron_ingot", 2, "minecraft:lapis_lazuli", 4);
        weapon("epee_berserker", 60, "acier_orc", 4, "defense_orc", 3, "couronne_roi_gobelin", 1);
        weapon("epee_milicien", 8, "minecraft:iron_ingot", 4, "griffe_loup", 2, "minecraft:leather", 1);
        weapon("lame_acier_trempe", 35, "minecraft:iron_ingot", 4, "bois_enchante", 2, "ferraille_gobeline", 4, "insigne_bandit", 1);
        weapon("epee_croise", 110, "lingot_mithril", 3, "relique_funeraire", 3, "fragment_ossuaire", 4, "couronne_roi_gobelin", 1);
        weapon("epee_seigneur_guerre", 150, "lingot_mithril", 4, "os_maudit", 4, "phylactere_liche", 1);
        weapon("lame_tempetes", 280, "bronze_runique", 4, "cendre_ardente", 4, "braise_djinn", 1, "phylactere_liche", 1);
        weapon("lame_demoniaque", 400, "lingot_adamantite", 4, "noyau_flamme", 3, "coeur_infernal", 1);
        weapon("epee_roi_dragon", 650, "acier_stellaire", 3, "croc_wyrm", 2, "ecaille_hydre", 2, "coeur_infernal", 1);
        weapon("excalibur_celeste", 1000, "fragment_divin", 3, "lingot_adamantite", 4, "essence_neant", 2, "coeur_glace_eternelle", 1);
        // Mage
        weapon("baton_apprenti", 3, "minecraft:stick", 3, "minecraft:lapis_lazuli", 2, "minecraft:glowstone_dust", 1);
        weapon("baton_saule", 8, "minecraft:stick", 3, "ecorce_ancienne", 3, "minecraft:lapis_lazuli", 2);
        weapon("sceptre_givre", 20, "oreille_gobelin", 3, "croc_loup", 2, "minecraft:lapis_lazuli", 3, "minecraft:quartz", 2);
        weapon("sceptre_marees", 35, "bois_enchante", 2, "mucus_acide", 4, "venin_marais", 2, "minecraft:prismarine_shard", 3);
        weapon("baton_flammes", 60, "poussiere_ame", 3, "os_maudit", 2, "couronne_roi_gobelin", 1, "minecraft:blaze_rod", 2);
        weapon("baton_foudre", 110, "bois_enchante", 3, "tissu_spectral", 2, "ichor_cryptes", 3, "couronne_roi_gobelin", 1);
        weapon("baton_arcanique", 150, "lingot_mithril", 2, "poussiere_ame", 4, "phylactere_liche", 1, "minecraft:amethyst_shard", 4);
        weapon("sceptre_ombre", 280, "crane_maudit", 2, "gemme_brute", 2, "bandelette_ancienne", 3, "phylactere_liche", 1);
        weapon("sceptre_neant", 400, "cristal_arcanique", 3, "eclat_givre", 2, "coeur_infernal", 1);
        weapon("baton_phenix", 650, "braise_djinn", 3, "poussiere_etoile", 3, "seve_lumineuse", 4, "coeur_infernal", 1);
        weapon("baton_archimage", 1000, "fragment_divin", 3, "essence_neant", 3, "cristal_arcanique", 2, "coeur_glace_eternelle", 1);
        // Archer
        weapon("arc_chasseur", 3, "minecraft:stick", 3, "minecraft:string", 3, "minecraft:leather", 1);
        weapon("arc_court", 8, "minecraft:stick", 3, "soie_araignee", 2, "griffe_loup", 1);
        weapon("arc_elfique", 20, "cuir_renforce", 2, "croc_loup", 3, "minecraft:string", 3);
        weapon("arc_composite", 35, "bois_enchante", 2, "soie_araignee", 3, "fourrure_sombre", 2, "insigne_bandit", 1);
        weapon("arc_tempete", 60, "os_maudit", 3, "acier_orc", 2, "couronne_roi_gobelin", 1, "cuir_renforce", 2);
        weapon("arc_sylvain", 110, "bois_enchante", 4, "seve_lumineuse", 2, "relique_funeraire", 2, "couronne_roi_gobelin", 1);
        weapon("arc_faucon", 150, "lingot_mithril", 2, "fourrure_sombre", 3, "phylactere_liche", 1, "minecraft:feather", 4);
        weapon("arc_lune_argent", 280, "cuir_polaire", 3, "voile_spectral", 2, "carapace_givree", 3, "phylactere_liche", 1);
        weapon("arc_spectral", 400, "eclat_givre", 3, "cristal_arcanique", 2, "coeur_infernal", 1);
        weapon("arc_dragon", 650, "acier_stellaire", 2, "croc_wyrm", 3, "ecaille_hydre", 2, "coeur_infernal", 1);
        weapon("arc_aube_divine", 1000, "fragment_divin", 3, "lingot_adamantite", 2, "essence_neant", 2, "coeur_glace_eternelle", 1);
        // Tank
        weapon("masse_garde", 3, "minecraft:iron_ingot", 3, "minecraft:stick", 2, "minecraft:cobblestone", 2);
        weapon("masse_cloutee", 8, "minecraft:iron_ingot", 4, "ferraille_gobeline", 2, "minecraft:stick", 1);
        weapon("marteau_bastion", 20, "ferraille_gobeline", 4, "minecraft:iron_ingot", 3, "croc_loup", 2);
        weapon("marteau_forgeron", 35, "minecraft:iron_ingot", 5, "bois_enchante", 2, "sang_orc", 1, "insigne_bandit", 1);
        weapon("marteau_colosse", 60, "acier_orc", 5, "defense_orc", 2, "couronne_roi_gobelin", 1);
        weapon("masse_templier", 110, "lingot_mithril", 3, "relique_funeraire", 3, "fragment_ossuaire", 4, "couronne_roi_gobelin", 1);
        weapon("masse_gardien", 150, "lingot_mithril", 4, "minecraft:gold_block", 2, "phylactere_liche", 1);
        weapon("marteau_runique", 280, "bronze_runique", 5, "gemme_brute", 2, "carapace_givree", 3, "phylactere_liche", 1);
        weapon("marteau_titan", 400, "lingot_adamantite", 4, "noyau_flamme", 2, "coeur_infernal", 1);
        weapon("marteau_rois_anciens", 650, "acier_stellaire", 3, "couronne_cryptes", 1, "coeur_racine", 2, "coeur_infernal", 1);
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
        armorSet("veteran", 25, "minecraft:iron_ingot", 3, "griffe_loup", 2, "insigne_bandit");
        armorSet("croise", 70, "lingot_mithril", 2, "relique_funeraire", 2, "couronne_roi_gobelin");
        armorSet("conquerant", 180, "bronze_runique", 3, "sang_orc", 2, "braise_djinn");
        armorSet("seigneur_dragon", 240, "acier_stellaire", 2, "ecaille_hydre", 1, "croc_wyrm");
        armorSet("acolyte", 25, "soie_araignee", 3, "seve_lumineuse", 1, "insigne_bandit");
        armorSet("enchanteur", 70, "tissu_spectral", 2, "ichor_cryptes", 2, "couronne_roi_gobelin");
        armorSet("mage_bataille", 180, "tissu_spectral", 3, "bandelette_ancienne", 2, "braise_djinn");
        armorSet("oracle_astral", 240, "tissu_spectral", 3, "poussiere_etoile", 2, "eclat_fracture");
        armorSet("eclaireur", 25, "cuir_renforce", 2, "soie_araignee", 2, "insigne_bandit");
        armorSet("traqueur", 70, "cuir_renforce", 3, "venin_marais", 2, "couronne_roi_gobelin");
        armorSet("lame_ombre", 180, "cuir_polaire", 3, "voile_spectral", 1, "braise_djinn");
        armorSet("chasseur_lunaire", 240, "cuir_polaire", 3, "eclat_astral", 2, "croc_wyrm");
        armorSet("sentinelle_fer", 25, "minecraft:iron_ingot", 4, "ecorce_ancienne", 2, "insigne_bandit");
        armorSet("rempart", 70, "lingot_mithril", 3, "fragment_ossuaire", 3, "couronne_roi_gobelin");
        armorSet("croise_sacre", 180, "bronze_runique", 3, "gemme_brute", 1, "coeur_racine");
        armorSet("bastion_eternel", 240, "acier_stellaire", 2, "carapace_givree", 2, "couronne_cryptes");
        armorSet("valkyrie", 1000, "alliage_celeste", 3, "lingot_adamantite", 3, "fragment_divin");
        armorSet("eternel_arcanes", 1000, "alliage_celeste", 3, "tissu_enchante", 5, "fragment_divin");
        armorSet("sentinelle_astrale", 1000, "alliage_celeste", 3, "cuir_renforce", 5, "fragment_divin");
        armorSet("egide_divine", 1000, "alliage_celeste", 3, "lingot_adamantite", 5, "fragment_divin");

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

        // ---------------------------------------------------------------- invocations
        add(Category.INVOCATIONS, "sceau_roi_gobelin", 1, 20, 15, N, "oreille_gobelin", 8, "ferraille_gobeline", 6, "minecraft:gold_ingot", 2);
        add(Category.INVOCATIONS, "grimoire_interdit", 1, 60, 35, N, "os_maudit", 6, "poussiere_ame", 4, "minecraft:book", 1, "couronne_roi_gobelin", 1);
        add(Category.INVOCATIONS, "braise_eternelle", 1, 120, 55, N, "noyau_flamme", 6, "defense_orc", 2, "phylactere_liche", 1);
        add(Category.INVOCATIONS, "cristal_glacial", 1, 200, 70, N, "eclat_givre", 8, "cristal_arcanique", 4, "coeur_infernal", 1);
        add(Category.INVOCATIONS, "oeil_neant", 1, 400, 90, N, "essence_neant", 6, "coeur_glace_eternelle", 1, "minecraft:ender_eye", 2, "cristal_arcanique", 4);

        // ---------------------------------------------------------------- familiers
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
