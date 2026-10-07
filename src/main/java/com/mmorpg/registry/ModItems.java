package com.mmorpg.registry;

import com.mmorpg.MMORPG;
import com.mmorpg.item.ItemDefs;
import com.mmorpg.item.PetEggItem;
import com.mmorpg.item.RpgArmorItem;
import com.mmorpg.item.RpgBowItem;
import com.mmorpg.item.RpgConsumableItem;
import com.mmorpg.item.RpgItemDef;
import com.mmorpg.item.RpgMaterialItem;
import com.mmorpg.item.RpgStaffItem;
import com.mmorpg.item.RpgWeaponItem;
import com.mmorpg.item.SummonKeyItem;
import com.mmorpg.pet.PetType;
import com.mmorpg.rpg.Rarity;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.component.UseCooldown;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MMORPG.MODID);

    public static final List<DeferredItem<? extends Item>> WEAPONS = new ArrayList<>();
    public static final List<DeferredItem<? extends Item>> ARMORS = new ArrayList<>();
    public static final List<DeferredItem<? extends Item>> MATERIALS = new ArrayList<>();
    public static final List<DeferredItem<? extends Item>> CONSUMABLES = new ArrayList<>();
    public static final List<DeferredItem<? extends Item>> SUMMONS = new ArrayList<>();
    public static final List<DeferredItem<? extends Item>> SPAWN_EGGS = new ArrayList<>();
    public static final Map<String, DeferredItem<? extends Item>> BY_ID = new LinkedHashMap<>();
    public static final Map<PetType, DeferredItem<PetEggItem>> PET_EGGS = new EnumMap<>(PetType.class);
    public static final Map<PetType, DeferredItem<Item>> PET_SPRITES = new EnumMap<>(PetType.class);
    public static final Map<ItemDefs.Element, DeferredItem<Item>> PROJECTILE_SPRITES = new EnumMap<>(ItemDefs.Element.class);

    // ------------------------------------------------------------------ materiaux de monstres
    public static final DeferredItem<RpgMaterialItem> OREILLE_GOBELIN = material("oreille_gobelin", Rarity.COMMUN, "Butin des Gobelins des plaines et forêts.");
    public static final DeferredItem<RpgMaterialItem> FERRAILLE_GOBELINE = material("ferraille_gobeline", Rarity.COMMUN, "Métal récupéré par les Gobelins.");
    public static final DeferredItem<RpgMaterialItem> CROC_LOUP = material("croc_loup", Rarity.COMMUN, "Arraché aux Loups Sombres des forêts nocturnes.");
    public static final DeferredItem<RpgMaterialItem> FOURRURE_SOMBRE = material("fourrure_sombre", Rarity.PEU_COMMUN, "Pelage épais des Loups Sombres.");
    public static final DeferredItem<RpgMaterialItem> OS_MAUDIT = material("os_maudit", Rarity.PEU_COMMUN, "Imprégné de la malédiction des Squelettes Maudits.");
    public static final DeferredItem<RpgMaterialItem> POUSSIERE_AME = material("poussiere_ame", Rarity.PEU_COMMUN, "Résidu d'âme des morts-vivants.");
    public static final DeferredItem<RpgMaterialItem> DEFENSE_ORC = material("defense_orc", Rarity.RARE, "Défense d'un Orc Guerrier.");
    public static final DeferredItem<RpgMaterialItem> ACIER_ORC = material("acier_orc", Rarity.RARE, "Acier brut forgé par les Orcs.");
    public static final DeferredItem<RpgMaterialItem> NOYAU_FLAMME = material("noyau_flamme", Rarity.RARE, "Cœur brûlant d'un Élémentaire de Feu.");
    public static final DeferredItem<RpgMaterialItem> ECLAT_GIVRE = material("eclat_givre", Rarity.RARE, "Éclat glacé laissé par les Spectres de Givre.");
    public static final DeferredItem<RpgMaterialItem> CRISTAL_ARCANIQUE = material("cristal_arcanique", Rarity.EPIQUE, "Cristal arraché aux Golems de Cristal.");
    public static final DeferredItem<RpgMaterialItem> ESSENCE_NEANT = material("essence_neant", Rarity.EPIQUE, "Énergie pure des Chevaliers du Néant.");
    // butin de boss
    public static final DeferredItem<RpgMaterialItem> COURONNE_ROI_GOBELIN = material("couronne_roi_gobelin", Rarity.RARE, "Trophée du Roi Gobelin.");
    public static final DeferredItem<RpgMaterialItem> PHYLACTERE_LICHE = material("phylactere_liche", Rarity.EPIQUE, "Le réceptacle de l'âme de la Liche Ancienne.");
    public static final DeferredItem<RpgMaterialItem> COEUR_INFERNAL = material("coeur_infernal", Rarity.EPIQUE, "Le cœur ardent du Seigneur Démon Ignis.");
    public static final DeferredItem<RpgMaterialItem> COEUR_GLACE_ETERNELLE = material("coeur_glace_eternelle", Rarity.LEGENDAIRE, "Le cœur gelé du Titan de Glace.");
    public static final DeferredItem<RpgMaterialItem> FRAGMENT_DIVIN = material("fragment_divin", Rarity.MYTHIQUE, "Un éclat de divinité arraché à l'Avatar du Néant.");
    // materiaux generiques
    public static final DeferredItem<RpgMaterialItem> PIERRE_AMELIORATION = material("pierre_amelioration", Rarity.PEU_COMMUN, "Permet d'améliorer un équipement de +1 à +8 à la Forge Arcanique.");
    public static final DeferredItem<RpgMaterialItem> PIERRE_AMELIORATION_SUP = material("pierre_amelioration_sup", Rarity.EPIQUE, "Permet d'améliorer un équipement de +9 à +20 à la Forge Arcanique.");
    public static final DeferredItem<RpgMaterialItem> TREFLE_CHANCE = material("trefle_chance", Rarity.PEU_COMMUN,
            "Garantit la réussite d'une amélioration de +1 à +4 à la Forge Arcanique (consommé).");
    public static final DeferredItem<RpgMaterialItem> TREFLE_QUATRE_FEUILLES = material("trefle_quatre_feuilles", Rarity.RARE,
            "Garantit la réussite d'une amélioration de +5 à +8 à la Forge Arcanique (consommé).");
    public static final DeferredItem<RpgMaterialItem> TREFLE_DORE = material("trefle_dore", Rarity.EPIQUE,
            "Garantit la réussite d'une amélioration de +9 à +12 à la Forge Arcanique (consommé).");
    public static final DeferredItem<RpgMaterialItem> TREFLE_CELESTE = material("trefle_celeste", Rarity.LEGENDAIRE,
            "Garantit la réussite d'une amélioration de +13 à +16 à la Forge Arcanique (consommé).");
    public static final DeferredItem<RpgMaterialItem> TREFLE_DIVIN = material("trefle_divin", Rarity.MYTHIQUE,
            "Garantit la réussite d'une amélioration de +17 à +20 à la Forge Arcanique (consommé).");
    public static final DeferredItem<RpgMaterialItem> ALLIAGE_CELESTE = material("alliage_celeste", Rarity.MYTHIQUE, "Alliage de fin de progression pour les panoplies mythiques de classe.");
    public static final DeferredItem<RpgMaterialItem> LINGOT_MITHRIL = material("lingot_mithril", Rarity.RARE, "Métal léger et résistant, façonné à la Forge Arcanique.");
    public static final DeferredItem<RpgMaterialItem> LINGOT_ADAMANTITE = material("lingot_adamantite", Rarity.EPIQUE, "Le métal le plus dur du monde connu.");
    public static final DeferredItem<RpgMaterialItem> TISSU_ENCHANTE = material("tissu_enchante", Rarity.RARE, "Étoffe tissée de poussière d'âme.");
    public static final DeferredItem<RpgMaterialItem> CUIR_RENFORCE = material("cuir_renforce", Rarity.PEU_COMMUN, "Cuir renforcé de fourrure sombre.");
    // ---- materiaux de butin supplementaires (monstres, zones des monstres importes) et intermediaires de la Forge
    public static final DeferredItem<RpgMaterialItem> SOIE_ARAIGNEE = material("soie_araignee", Rarity.COMMUN, "Soie résistante filée par les araignées.");
    public static final DeferredItem<RpgMaterialItem> GRIFFE_LOUP = material("griffe_loup", Rarity.COMMUN, "Griffe acérée des Loups Sombres.");
    public static final DeferredItem<RpgMaterialItem> INSIGNE_BANDIT = material("insigne_bandit", Rarity.PEU_COMMUN, "Insigne arraché aux bandits arbalétriers.");
    public static final DeferredItem<RpgMaterialItem> ICHOR_CRYPTES = material("ichor_cryptes", Rarity.PEU_COMMUN, "Fluide verdâtre des morts-vivants des cryptes.");
    public static final DeferredItem<RpgMaterialItem> SANG_ORC = material("sang_orc", Rarity.PEU_COMMUN, "Sang épais et bouillonnant des Orcs Guerriers.");
    public static final DeferredItem<RpgMaterialItem> CENDRE_ARDENTE = material("cendre_ardente", Rarity.PEU_COMMUN, "Cendre encore brûlante des Élémentaires de Feu.");
    public static final DeferredItem<RpgMaterialItem> CRANE_MAUDIT = material("crane_maudit", Rarity.RARE, "Un crâne qui murmure encore des malédictions.");
    public static final DeferredItem<RpgMaterialItem> VOILE_SPECTRAL = material("voile_spectral", Rarity.RARE, "Lambeau translucide arraché aux Spectres de Givre.");
    public static final DeferredItem<RpgMaterialItem> GEMME_BRUTE = material("gemme_brute", Rarity.RARE, "Gemme non taillée extraite des Golems de Cristal.");
    public static final DeferredItem<RpgMaterialItem> PLAQUE_NEANT = material("plaque_neant", Rarity.EPIQUE, "Fragment d'armure des Chevaliers du Néant.");
    public static final DeferredItem<RpgMaterialItem> ECORCE_ANCIENNE = material("ecorce_ancienne", Rarity.COMMUN, "Écorce dure des créatures de la Forêt ancienne.");
    public static final DeferredItem<RpgMaterialItem> SEVE_LUMINEUSE = material("seve_lumineuse", Rarity.PEU_COMMUN, "Sève dorée qui brille dans l'obscurité.");
    public static final DeferredItem<RpgMaterialItem> COEUR_RACINE = material("coeur_racine", Rarity.RARE, "Le cœur vivant du Colosse racine.");
    public static final DeferredItem<RpgMaterialItem> MUCUS_ACIDE = material("mucus_acide", Rarity.COMMUN, "Substance corrosive des créatures du Marais.");
    public static final DeferredItem<RpgMaterialItem> VENIN_MARAIS = material("venin_marais", Rarity.PEU_COMMUN, "Venin violacé des crapauds et sorciers des marais.");
    public static final DeferredItem<RpgMaterialItem> ECAILLE_HYDRE = material("ecaille_hydre", Rarity.RARE, "Écaille brillante de l'Hydre des marais.");
    public static final DeferredItem<RpgMaterialItem> FRAGMENT_OSSUAIRE = material("fragment_ossuaire", Rarity.COMMUN, "Débris d'os des Cryptes.");
    public static final DeferredItem<RpgMaterialItem> RELIQUE_FUNERAIRE = material("relique_funeraire", Rarity.PEU_COMMUN, "Anneau rituel des gardes funéraires.");
    public static final DeferredItem<RpgMaterialItem> COURONNE_CRYPTES = material("couronne_cryptes", Rarity.RARE, "La couronne rouillée du Roi des cryptes.");
    public static final DeferredItem<RpgMaterialItem> FOURRURE_POLAIRE = material("fourrure_polaire", Rarity.COMMUN, "Fourrure épaisse des bêtes des Terres gelées.");
    public static final DeferredItem<RpgMaterialItem> CARAPACE_GIVREE = material("carapace_givree", Rarity.PEU_COMMUN, "Carapace gelée des scarabées polaires.");
    public static final DeferredItem<RpgMaterialItem> CROC_WYRM = material("croc_wyrm", Rarity.RARE, "Croc glacé du Wyrm boréal.");
    public static final DeferredItem<RpgMaterialItem> CARAPACE_CUIVRE = material("carapace_cuivre", Rarity.COMMUN, "Plaques métalliques des scorpions de cuivre.");
    public static final DeferredItem<RpgMaterialItem> BANDELETTE_ANCIENNE = material("bandelette_ancienne", Rarity.PEU_COMMUN, "Bandelette imprégnée de magie des momies.");
    public static final DeferredItem<RpgMaterialItem> BRAISE_DJINN = material("braise_djinn", Rarity.RARE, "Braise éternelle d'un Djinn des braises.");
    public static final DeferredItem<RpgMaterialItem> ECLAT_ASTRAL = material("eclat_astral", Rarity.PEU_COMMUN, "Éclat cristallin tombé du Néant astral.");
    public static final DeferredItem<RpgMaterialItem> POUSSIERE_ETOILE = material("poussiere_etoile", Rarity.RARE, "Poussière scintillante des créatures astrales.");
    public static final DeferredItem<RpgMaterialItem> ECLAT_FRACTURE = material("eclat_fracture", Rarity.EPIQUE, "Morceau de réalité brisée laissé par les êtres du vide.");
    public static final DeferredItem<RpgMaterialItem> BOIS_ENCHANTE = material("bois_enchante", Rarity.PEU_COMMUN, "Bois ancien imprégné de sève lumineuse.");
    public static final DeferredItem<RpgMaterialItem> BRONZE_RUNIQUE = material("bronze_runique", Rarity.RARE, "Alliage de cuivre gravé de runes.");
    public static final DeferredItem<RpgMaterialItem> TISSU_SPECTRAL = material("tissu_spectral", Rarity.RARE, "Étoffe tissée de soie et de voile spectral.");
    public static final DeferredItem<RpgMaterialItem> CUIR_POLAIRE = material("cuir_polaire", Rarity.RARE, "Cuir renforcé doublé de fourrure polaire.");
    public static final DeferredItem<RpgMaterialItem> ACIER_STELLAIRE = material("acier_stellaire", Rarity.EPIQUE, "Adamantite forgée avec de la poussière d'étoile.");
    public static final DeferredItem<RpgMaterialItem> PIECE_OR = ITEMS.registerItem("piece_or",
            p -> new RpgMaterialItem(p, Rarity.COMMUN, "Monnaie", "La monnaie du royaume. Utilisée à la Forge Arcanique."), p -> p.stacksTo(99));

    // ------------------------------------------------------------------ invocations
    public static final DeferredItem<SummonKeyItem> SCEAU_ROI_GOBELIN = summon("sceau_roi_gobelin", Rarity.RARE, "roi_gobelin", "Roi Gobelin", 20);
    public static final DeferredItem<SummonKeyItem> GRIMOIRE_INTERDIT = summon("grimoire_interdit", Rarity.EPIQUE, "liche_ancienne", "Liche Ancienne", 45);
    public static final DeferredItem<SummonKeyItem> BRAISE_ETERNELLE = summon("braise_eternelle", Rarity.EPIQUE, "seigneur_ignis", "Seigneur Démon Ignis", 65);
    public static final DeferredItem<SummonKeyItem> CRISTAL_GLACIAL = summon("cristal_glacial", Rarity.LEGENDAIRE, "titan_glace", "Titan de Glace", 80);
    public static final DeferredItem<SummonKeyItem> OEIL_NEANT = summon("oeil_neant", Rarity.MYTHIQUE, "avatar_neant", "Avatar du Néant", 100);

    // ------------------------------------------------------------------ consommables
    public static final DeferredItem<RpgConsumableItem> POTION_SOIN_MINEURE = potion("potion_soin_mineure", Rarity.COMMUN, RpgConsumableItem.Kind.HEAL, 150, "Rend 150 PV.");
    public static final DeferredItem<RpgConsumableItem> POTION_SOIN = potion("potion_soin", Rarity.PEU_COMMUN, RpgConsumableItem.Kind.HEAL, 600, "Rend 600 PV.");
    public static final DeferredItem<RpgConsumableItem> POTION_SOIN_MAJEURE = potion("potion_soin_majeure", Rarity.RARE, RpgConsumableItem.Kind.HEAL, 1500, "Rend 1500 PV.");
    public static final DeferredItem<RpgConsumableItem> POTION_MANA_MINEURE = potion("potion_mana_mineure", Rarity.COMMUN, RpgConsumableItem.Kind.MANA, 100, "Rend 100 points de mana.");
    public static final DeferredItem<RpgConsumableItem> POTION_MANA = potion("potion_mana", Rarity.PEU_COMMUN, RpgConsumableItem.Kind.MANA, 350, "Rend 350 points de mana.");
    public static final DeferredItem<RpgConsumableItem> POTION_MANA_MAJEURE = potion("potion_mana_majeure", Rarity.RARE, RpgConsumableItem.Kind.MANA, 900, "Rend 900 points de mana.");
    public static final DeferredItem<RpgConsumableItem> ELIXIR_EXPERIENCE = consumable("elixir_experience", Rarity.EPIQUE, RpgConsumableItem.Kind.ELIXIR, 0.25, "Octroie 25 % de l'expérience du niveau en cours.");
    public static final DeferredItem<RpgConsumableItem> PARCHEMIN_TELEPORTATION = consumable("parchemin_teleportation", Rarity.PEU_COMMUN, RpgConsumableItem.Kind.TELEPORT_SCROLL, 0, "Ouvre le réseau de téléportation depuis n'importe où.\nConsommé lors du voyage.");
    public static final DeferredItem<RpgConsumableItem> PARCHEMIN_OUBLI = consumable("parchemin_oubli", Rarity.RARE, RpgConsumableItem.Kind.FORGET_SCROLL, 0, "Réinitialise vos points d'attribut.");
    public static final DeferredItem<RpgConsumableItem> ORBE_RENAISSANCE = consumable("orbe_renaissance", Rarity.LEGENDAIRE, RpgConsumableItem.Kind.REBIRTH_ORB, 0, "Permet de choisir une nouvelle classe.\nVotre niveau est conservé.");
    public static final DeferredItem<RpgConsumableItem> COFFRE_COSMETIQUE = consumable("coffre_cosmetique", Rarity.EPIQUE, RpgConsumableItem.Kind.COSMETIC_CHEST, 0, "Débloque un cosmétique animé aléatoire.");

    public static final DeferredItem<RpgConsumableItem> NOURRITURE_FAMILIER = consumable("nourriture_familier", Rarity.PEU_COMMUN,
            RpgConsumableItem.Kind.PET_FOOD, 25, "Restaure 25 points de bonheur au familier invoqué.\nLes bonus varient de 50 % à 100 % selon son bonheur.");

    public static final DeferredItem<RpgConsumableItem> CHARME_EXPERIENCE = consumable("charme_experience", Rarity.RARE,
            RpgConsumableItem.Kind.XP_CHARM, 0, "+25 % d'expérience pendant 30 minutes de jeu. Non cumulable.");
    public static final DeferredItem<RpgConsumableItem> CHARME_CHANCE = consumable("charme_chance", Rarity.RARE,
            RpgConsumableItem.Kind.LUCK_CHARM, 0, "+25 % de chances de butin pendant 30 minutes de jeu. Non cumulable.");

    public static final DeferredItem<RpgMaterialItem> CLE_AVENTURE = material("cle_aventure", Rarity.RARE, "Ouvre une caisse d’aventure. Une clé par caisse.");
    public static final DeferredItem<RpgConsumableItem> CAISSE_AVENTURE = consumable("caisse_aventure", Rarity.RARE, RpgConsumableItem.Kind.ADVENTURE_CRATE, 0, "Nécessite une clé d’aventure. Contient un butin aléatoire (potions, pierres, or, charmes...).");
    public static final DeferredItem<BlockItem> LUCKY_BLOCK = ITEMS.registerSimpleBlockItem("lucky_block", ModBlocks.LUCKY_BLOCK);

    // ------------------------------------------------------------------ blocs
    public static final DeferredItem<BlockItem> FORGE_ARCANIQUE = ITEMS.registerSimpleBlockItem("forge_arcanique", ModBlocks.FORGE_ARCANIQUE);
    public static final DeferredItem<com.mmorpg.item.AdminBlockItem> TELEPORTEUR = ITEMS.registerItem("teleporteur",
            p -> new com.mmorpg.item.AdminBlockItem(ModBlocks.TELEPORTEUR.get(), p), () -> new Item.Properties().useBlockDescriptionPrefix());
    public static final DeferredItem<BlockItem> AUTEL_INVOCATION = ITEMS.registerSimpleBlockItem("autel_invocation", ModBlocks.AUTEL_INVOCATION);

    static {
        // armes
        for (ItemDefs.WeaponDef w : ItemDefs.WEAPONS) {
            RpgItemDef def = new RpgItemDef(w.id(), w.cls(), w.level(), w.rarity(), w.stats(), null, RpgItemDef.Slot.WEAPON, w.lore());
            DeferredItem<? extends Item> item = switch (w.kind()) {
                case BOW -> ITEMS.registerItem(w.id(), p -> new RpgBowItem(p, def));
                case STAFF -> ITEMS.registerItem(w.id(), p -> new RpgStaffItem(p, def, w.element()));
                default -> ITEMS.registerItem(w.id(), p -> new RpgWeaponItem(p, def, w.kind()));
            };
            WEAPONS.add(item);
            BY_ID.put(w.id(), item);
        }
        // armures
        EquipmentSlot[] slots = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (ItemDefs.ArmorSet set : ItemDefs.SETS.values()) {
            for (int i = 0; i < 4; i++) {
                String id = set.id() + "_" + ItemDefs.PIECES[i];
                RpgItemDef def = new RpgItemDef(id, set.cls(), set.level(), set.rarity(), ItemDefs.pieceStats(set, i), set.id(), ItemDefs.pieceSlot(i), null);
                EquipmentSlot slot = slots[i];
                String label = pieceLabel(set.style(), i);
                DeferredItem<RpgArmorItem> item = ITEMS.registerItem(id, p -> new RpgArmorItem(p, def, slot, set.style(), label));
                ARMORS.add(item);
                BY_ID.put(id, item);
            }
        }
        // familiers
        for (PetType pet : PetType.values()) {
            DeferredItem<PetEggItem> egg = ITEMS.registerItem("oeuf_" + pet.id, p -> new PetEggItem(p, pet));
            PET_EGGS.put(pet, egg);
            BY_ID.put("oeuf_" + pet.id, egg);
            PET_SPRITES.put(pet, ITEMS.registerSimpleItem("familier_" + pet.id));
        }
        for (ItemDefs.Element e : ItemDefs.Element.values()) {
            PROJECTILE_SPRITES.put(e, ITEMS.registerSimpleItem("projectile_" + e.name().toLowerCase(java.util.Locale.ROOT)));
        }
        // oeufs d'apparition
        for (Map.Entry<String, DeferredHolder<EntityType<?>, ? extends EntityType<?>>> e : ModEntities.SPAWNABLE.entrySet()) {
            Supplier<? extends EntityType<?>> type = e.getValue();
            DeferredItem<SpawnEggItem> egg = ITEMS.registerItem(e.getKey() + "_spawn_egg", p -> new SpawnEggItem(p.spawnEgg(type.get())));
            SPAWN_EGGS.add(egg);
        }
    }

    private ModItems() {
    }

    private static String pieceLabel(String style, int i) {
        String[][] names = {
                {"Casque", "Plastron", "Jambières", "Bottes"},
                {"Capuche", "Robe", "Pantalon", "Sandales"},
                {"Coiffe", "Veste", "Pantalon", "Bottes"}};
        int s = switch (style) {
            case "robe" -> 1;
            case "leather" -> 2;
            default -> 0;
        };
        return names[s][i];
    }

    private static DeferredItem<RpgMaterialItem> material(String id, Rarity rarity, String lore) {
        DeferredItem<RpgMaterialItem> item = ITEMS.registerItem(id, p -> new RpgMaterialItem(p, rarity, "Matériau", lore));
        MATERIALS.add(item);
        BY_ID.put(id, item);
        return item;
    }

    private static DeferredItem<SummonKeyItem> summon(String id, Rarity rarity, String boss, String bossName, int level) {
        DeferredItem<SummonKeyItem> item = ITEMS.registerItem(id, p -> new SummonKeyItem(p, rarity, boss, bossName, level));
        SUMMONS.add(item);
        BY_ID.put(id, item);
        return item;
    }

    private static DeferredItem<RpgConsumableItem> potion(String id, Rarity rarity, RpgConsumableItem.Kind kind, double amount, String lore) {
        DeferredItem<RpgConsumableItem> item = ITEMS.registerItem(id, p -> new RpgConsumableItem(
                p.stacksTo(16).component(DataComponents.USE_COOLDOWN, new UseCooldown(8f, Optional.of(RpgConsumableItem.POTION_GROUP))),
                rarity, kind, amount, lore + "\nRecharge partagée : 8 s"));
        CONSUMABLES.add(item);
        BY_ID.put(id, item);
        return item;
    }

    private static DeferredItem<RpgConsumableItem> consumable(String id, Rarity rarity, RpgConsumableItem.Kind kind, double amount, String lore) {
        DeferredItem<RpgConsumableItem> item = ITEMS.registerItem(id, p -> new RpgConsumableItem(p.stacksTo(16), rarity, kind, amount, lore));
        CONSUMABLES.add(item);
        BY_ID.put(id, item);
        return item;
    }

    public static Item get(String id) {
        DeferredItem<? extends Item> d = BY_ID.get(id);
        return d == null ? null : d.get();
    }
}
