package com.mmorpg.fishing;

import com.mmorpg.rpg.Rarity;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Catalogue de la peche d'Aeloria : poissons, dechets, coffres a tresor et ingredients marins, avec rarete, niveau de metier,
 * habitats, moment de la journee et gains d'experience. Classe sans dependance Minecraft (testee seule : voir FishingChecks).
 * Les memes identifiants servent aux objets, aux modeles et aux textures (tools/gen_fishing.py doit rester identique).
 */
public final class FishCatalog {
    private FishCatalog() {
    }

    public enum Habitat {
        LAC("Lacs"), RIVIERE("Rivières"), MER("Mers"), MARAIS("Marais"), GLACE("Eaux glacées"), JUNGLE("Jungles"), ABYSSES("Abysses");

        public final String label;

        Habitat(String label) {
            this.label = label;
        }
    }

    public enum Moment {
        TOUJOURS("à toute heure"), JOUR("de jour"), NUIT("de nuit"), PLUIE("sous la pluie");

        public final String label;

        Moment(String label) {
            this.label = label;
        }
    }

    public enum Kind { POISSON, DECHET, TRESOR }

    /** Une prise possible. {@code shape} designe la silhouette du modele 3D (voir tools/gen_fishing.py). */
    public record Entry(String id, String name, String nameEn, Kind kind, Rarity rarity, int minLevel, Set<Habitat> habitats, Moment moment,
                        int weight, int xp, int minCm, int maxCm, String shape, String lore) {
        public boolean fish() {
            return kind == Kind.POISSON;
        }
    }

    public static final List<Entry> ALL = new ArrayList<>();
    public static final Map<String, Entry> BY_ID = new LinkedHashMap<>();

    /** Experience d'une prise reussie selon la rarete (poissons). */
    public static final int[] FISH_XP = {12, 20, 35, 70, 140, 300};
    /** Experience d'un coffre selon son rang (rouille, corail, capitaine, abyssal). */
    public static final int[] CHEST_XP = {45, 80, 150, 280};
    public static final int JUNK_XP = 5;
    public static final int ESCAPE_XP = 2;

    private static EnumSet<Habitat> h(Habitat first, Habitat... more) {
        return EnumSet.of(first, more);
    }

    private static void fish(String id, String fr, String en, Rarity r, int level, Set<Habitat> hab, Moment m, int weight, int minCm, int maxCm, String shape, String lore) {
        add(new Entry(id, fr, en, Kind.POISSON, r, level, hab, m, weight, FISH_XP[r.ordinal()], minCm, maxCm, shape, lore));
    }

    private static void junk(String id, String fr, String en, int weight, String lore) {
        add(new Entry(id, fr, en, Kind.DECHET, Rarity.COMMUN, 1, EnumSet.allOf(Habitat.class), Moment.TOUJOURS, weight, JUNK_XP, 0, 0, "dechet", lore));
    }

    private static void chest(String id, String fr, String en, Rarity r, int rank, int level, Set<Habitat> hab, int weight, String lore) {
        add(new Entry(id, fr, en, Kind.TRESOR, r, level, hab, Moment.TOUJOURS, weight, CHEST_XP[rank], 0, 0, "coffre", lore));
    }

    private static void add(Entry e) {
        ALL.add(e);
        BY_ID.put(e.id(), e);
    }

    static {
        EnumSet<Habitat> eauxDouces = h(Habitat.LAC, Habitat.RIVIERE);
        // ---- communs
        fish("gardon_argente", "Gardon argenté", "Silver Roach", Rarity.COMMUN, 1, eauxDouces, Moment.TOUJOURS, 100, 12, 30, "fuseau",
                "Un petit poisson aux flancs miroitants, le premier de tout pêcheur d'Aeloria.");
        fish("carpe_doree", "Carpe dorée", "Golden Carp", Rarity.COMMUN, 1, h(Habitat.LAC), Moment.JOUR, 80, 25, 70, "haut",
                "Ses écailles brillent au soleil des lacs.");
        fish("sardine_lunaire", "Sardine lunaire", "Moon Sardine", Rarity.COMMUN, 1, h(Habitat.MER), Moment.NUIT, 80, 10, 22, "fuseau",
                "Elle remonte à la surface les nuits de pleine lune.");
        fish("maquereau_bleu", "Maquereau bleu", "Blue Mackerel", Rarity.COMMUN, 1, h(Habitat.MER), Moment.TOUJOURS, 100, 20, 45, "fuseau",
                "Dos rayé d'azur, chair ferme : la prise des côtes.");
        fish("goujon_des_marais", "Goujon des marais", "Marsh Gudgeon", Rarity.COMMUN, 1, h(Habitat.MARAIS), Moment.TOUJOURS, 100, 8, 20, "fuseau",
                "Il se cache entre les racines des eaux stagnantes.");
        fish("omble_des_glaces", "Omble des glaces", "Ice Char", Rarity.COMMUN, 1, h(Habitat.GLACE), Moment.TOUJOURS, 100, 18, 45, "fuseau",
                "Ses nageoires pâles se confondent avec la glace.");
        fish("tilapia_solaire", "Tilapia solaire", "Sun Tilapia", Rarity.COMMUN, 1, h(Habitat.JUNGLE), Moment.TOUJOURS, 100, 15, 38, "haut",
                "Un poisson des eaux chaudes, orange comme un coucher de soleil.");
        fish("anchois_des_abysses", "Anchois des abysses", "Abyss Anchovy", Rarity.COMMUN, 1, h(Habitat.ABYSSES), Moment.TOUJOURS, 100, 8, 18, "fuseau",
                "Minuscule et luminescent, il vit loin de toute lumière.");
        // ---- peu communs
        fish("truite_arc_en_ciel", "Truite arc-en-ciel", "Rainbow Trout", Rarity.PEU_COMMUN, 6, eauxDouces, Moment.PLUIE, 60, 30, 65, "fuseau",
                "La pluie la fait sauter hors de l'eau, flancs irisés.");
        fish("brochet_dore", "Brochet doré", "Golden Pike", Rarity.PEU_COMMUN, 8, h(Habitat.LAC, Habitat.RIVIERE), Moment.NUIT, 60, 50, 110, "long",
                "Un prédateur embusqué sous les nénuphars.");
        fish("bar_ecarlate", "Bar écarlate", "Scarlet Bass", Rarity.PEU_COMMUN, 8, h(Habitat.MER), Moment.JOUR, 60, 35, 80, "fuseau",
                "Rouge vif, très recherché des cuisiniers.");
        fish("anguille_voltaique", "Anguille voltaïque", "Voltaic Eel", Rarity.PEU_COMMUN, 10, h(Habitat.MARAIS), Moment.NUIT, 60, 60, 130, "anguille",
                "Chaque nuit, ses éclairs bleus illuminent les marais.");
        fish("piranha_pourpre", "Piranha pourpre", "Purple Piranha", Rarity.PEU_COMMUN, 10, h(Habitat.JUNGLE), Moment.TOUJOURS, 60, 15, 35, "haut",
                "Ses dents minuscules ont raison de n'importe quelle ligne.");
        fish("poisson_lanterne", "Poisson-lanterne", "Lantern Fish", Rarity.PEU_COMMUN, 10, h(Habitat.ABYSSES, Habitat.MER), Moment.NUIT, 60, 12, 30, "lanterne",
                "Une lueur bleutée pend devant sa bouche.");
        // ---- rares
        fish("saumon_royal", "Saumon royal", "Royal Salmon", Rarity.RARE, 18, h(Habitat.RIVIERE), Moment.TOUJOURS, 40, 60, 120, "fuseau",
                "Il remonte les rivières d'Aeloria pour frayer.");
        fish("esturgeon_ambre", "Esturgeon ambré", "Amber Sturgeon", Rarity.RARE, 20, h(Habitat.LAC), Moment.JOUR, 40, 90, 200, "long",
                "Un géant paisible dont les œufs valent de l'or.");
        fish("raie_cristalline", "Raie cristalline", "Crystal Ray", Rarity.RARE, 22, h(Habitat.MER), Moment.TOUJOURS, 40, 70, 160, "raie",
                "Son corps translucide scintille comme un cristal.");
        fish("silure_geant", "Silure géant", "Giant Catfish", Rarity.RARE, 24, h(Habitat.MARAIS, Habitat.JUNGLE), Moment.NUIT, 40, 120, 250, "silure",
                "On raconte qu'il avale des barques entières.");
        fish("perche_de_givre", "Perche de givre", "Frost Perch", Rarity.RARE, 24, h(Habitat.GLACE), Moment.TOUJOURS, 40, 30, 60, "haut",
                "Sa chair froide ne dégèle jamais tout à fait.");
        // ---- epiques
        fish("thon_aurore", "Thon de l'aurore", "Dawn Tuna", Rarity.EPIQUE, 30, h(Habitat.MER), Moment.JOUR, 25, 120, 260, "thon",
                "Puissant et rapide : il met la ligne à rude épreuve.");
        fish("baudroie_abyssale", "Baudroie abyssale", "Abyssal Anglerfish", Rarity.EPIQUE, 34, h(Habitat.ABYSSES), Moment.NUIT, 25, 60, 140, "lanterne",
                "Sa lanterne attire les imprudents au fond du noir.");
        fish("dragon_des_lacs", "Dragon des lacs", "Lake Dragon", Rarity.EPIQUE, 36, h(Habitat.LAC), Moment.PLUIE, 25, 150, 320, "anguille",
                "Les pêcheurs jurent l'avoir vu cracher de la brume.");
        fish("requin_de_corail", "Requin de corail", "Coral Shark", Rarity.EPIQUE, 38, h(Habitat.MER, Habitat.JUNGLE), Moment.TOUJOURS, 25, 140, 300, "requin",
                "Sa peau est incrustée de corail rouge.");
        // ---- legendaires
        fish("poisson_etoile", "Poisson-étoile astral", "Astral Starfish", Rarity.LEGENDAIRE, 42, EnumSet.of(Habitat.LAC, Habitat.MER, Habitat.GLACE), Moment.NUIT, 12, 20, 60, "etoile",
                "Il porte en lui un fragment de ciel nocturne.");
        fish("kraken_juvenile", "Kraken juvénile", "Young Kraken", Rarity.LEGENDAIRE, 45, h(Habitat.ABYSSES, Habitat.MER), Moment.PLUIE, 12, 180, 360, "kraken",
                "Un tentacule qui se débat de toutes ses forces.");
        fish("leviathan_nain", "Léviathan nain", "Dwarf Leviathan", Rarity.LEGENDAIRE, 48, h(Habitat.ABYSSES), Moment.TOUJOURS, 12, 300, 600, "requin",
                "Le plus petit de sa lignée, mais encore plus grand que votre barque.");
        // ---- mythique
        fish("carpe_du_neant", "Carpe du Néant", "Void Carp", Rarity.MYTHIQUE, 50, EnumSet.allOf(Habitat.class), Moment.NUIT, 4, 80, 180, "haut",
                "Une carpe née du Néant, dit-on, que nul ne pêche deux fois.");
        // ---- dechets
        junk("botte_rouillee", "Botte rouillée", "Rusty Boot", 30, "Une botte percée. Même l'eau n'en veut plus.");
        junk("algues_enchevetrees", "Algues enchevêtrées", "Tangled Seaweed", 30, "Un paquet d'algues gluantes.");
        junk("filet_dechire", "Filet déchiré", "Torn Net", 25, "Les restes d'un vieux filet de pêche.");
        junk("bouteille_vide", "Bouteille vide", "Empty Bottle", 20, "Elle a dérivé longtemps. Il n'y a pas de message dedans.");
        junk("arete_de_poisson", "Arête de poisson", "Fish Bone", 25, "Quelqu'un s'est régalé avant vous.");
        // ---- coffres
        chest("coffre_rouille", "Coffre rouillé", "Rusty Chest", Rarity.PEU_COMMUN, 0, 1, EnumSet.allOf(Habitat.class), 60,
                "Un coffre englouti, rongé par le sel. À ouvrir pour découvrir ce qu'il cache.");
        chest("coffre_corail", "Coffre de corail", "Coral Chest", Rarity.RARE, 1, 10, EnumSet.allOf(Habitat.class), 30,
                "Le corail a recouvert ce coffre au fil des siècles. À ouvrir pour découvrir ce qu'il cache.");
        chest("coffre_capitaine", "Coffre du capitaine", "Captain's Chest", Rarity.EPIQUE, 2, 25, EnumSet.of(Habitat.MER, Habitat.LAC, Habitat.JUNGLE, Habitat.MARAIS), 14,
                "Le butin d'un capitaine disparu. À ouvrir pour découvrir ce qu'il cache.");
        chest("coffre_abyssal", "Coffre abyssal", "Abyssal Chest", Rarity.LEGENDAIRE, 3, 40, EnumSet.of(Habitat.ABYSSES, Habitat.MER, Habitat.GLACE), 6,
                "Remonté des plus grandes profondeurs, il est froid comme la mort. À ouvrir pour découvrir ce qu'il cache.");
    }

    public static Entry get(String id) {
        return BY_ID.get(id);
    }

    public static List<Entry> of(Kind kind) {
        return ALL.stream().filter(e -> e.kind() == kind).toList();
    }

    // ------------------------------------------------------------------ ingredients marins (prises bonus, cannes, plats)

    public record Ingredient(String id, String name, String nameEn, Rarity rarity, int minLevel, String lore) {
    }

    public static final List<Ingredient> INGREDIENTS = List.of(
            new Ingredient("algue_lumineuse", "Algue lumineuse", "Glowing Kelp", Rarity.COMMUN, 10, "Elle brille doucement sous l'eau. Entre dans les plats de pêcheur."),
            new Ingredient("ecaille_argentee", "Écaille argentée", "Silver Scale", Rarity.PEU_COMMUN, 10, "Détachée d'un poisson d'Aeloria. Sert à forger les cannes."),
            new Ingredient("perle_nacree", "Perle nacrée", "Nacre Pearl", Rarity.RARE, 20, "Trouvée dans un coffre ou une prise rare. Sert à forger les cannes."),
            new Ingredient("corail_ancien", "Corail ancien", "Ancient Coral", Rarity.RARE, 30, "Un corail millénaire. Sert à forger les cannes."),
            new Ingredient("larme_de_sirene", "Larme de sirène", "Mermaid's Tear", Rarity.EPIQUE, 40, "Une perle bleue née d'un chagrin océanique. Sert à forger les cannes."),
            new Ingredient("ecaille_de_leviathan", "Écaille de Léviathan", "Leviathan Scale", Rarity.LEGENDAIRE, 50, "Une écaille grande comme un bouclier. Sert à forger la canne du Léviathan."));

    public static Ingredient ingredient(String id) {
        for (Ingredient i : INGREDIENTS) if (i.id().equals(id)) return i;
        return null;
    }

    // ------------------------------------------------------------------ cannes

    /**
     * Une canne d'Aeloria. {@code tier} de 1 a 6 (0 = canne vanilla). {@code zone} : agrandissement de la zone du mini-jeu ;
     * {@code lure} : reduction de l'attente avant la touche (niveaux d'enchantement « Appat ») ; {@code luck} : bonus de chance.
     */
    public record Rod(int tier, String id, String name, String nameEn, int minJobLevel, int durability, int lure, int luck, double zone, Rarity rarity, String lore) {
    }

    public static final List<Rod> RODS = List.of(
            new Rod(1, "canne_apprenti", "Canne d'apprenti", "Apprentice Rod", 1, 250, 0, 0, 1.00, Rarity.COMMUN, "Un roseau souple et un fil solide : pour apprendre."),
            new Rod(2, "canne_pecheur", "Canne du pêcheur", "Angler's Rod", 10, 500, 1, 1, 1.10, Rarity.PEU_COMMUN, "Ferrée de Verdite, elle sent déjà le poisson."),
            new Rod(3, "canne_marin", "Canne du marin", "Mariner's Rod", 20, 800, 1, 2, 1.20, Rarity.RARE, "Une canne de marin, cerclée d'Argentite et ornée d'une perle."),
            new Rod(4, "canne_harponneur", "Canne du harponneur", "Harpooner's Rod", 30, 1200, 2, 3, 1.30, Rarity.EPIQUE, "Dure comme l'acier, elle plie sans jamais rompre."),
            new Rod(5, "canne_des_abysses", "Canne des abysses", "Abyss Rod", 40, 1800, 2, 4, 1.40, Rarity.LEGENDAIRE, "Gelée par le Glacium, elle sonde les eaux les plus profondes."),
            new Rod(6, "canne_du_leviathan", "Canne du Léviathan", "Leviathan Rod", 50, 2600, 3, 5, 1.55, Rarity.MYTHIQUE, "Taillée dans le Néant et une écaille de Léviathan."));

    public static Rod rod(int tier) {
        for (Rod r : RODS) if (r.tier() == tier) return r;
        return null;
    }

    public static Rod rodById(String id) {
        for (Rod r : RODS) if (r.id().equals(id)) return r;
        return null;
    }

    /** Niveau minimal de canne pour remonter une rarete donnee (rang de canne, 0 = canne vanilla). */
    public static int rodTierFor(Rarity r) {
        return switch (r) {
            case COMMUN, PEU_COMMUN -> 0;
            case RARE -> 2;
            case EPIQUE -> 3;
            case LEGENDAIRE -> 4;
            case MYTHIQUE -> 5;
        };
    }
}
