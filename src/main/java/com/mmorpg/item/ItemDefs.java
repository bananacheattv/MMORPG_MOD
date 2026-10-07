package com.mmorpg.item;

import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.rpg.Rarity;
import com.mmorpg.rpg.Stat;
import com.mmorpg.rpg.StatBlock;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.mmorpg.rpg.Stat.*;

/** Tables de donnees des armes et des sets d'armure. */
public final class ItemDefs {
    private ItemDefs() {
    }

    public enum WeaponKind { SWORD, AXE, HAMMER, STAFF, BOW }

    public enum Element { FEU, GIVRE, ARCANE, NEANT, OMBRE, ECLAIR }

    public record WeaponDef(String id, PlayerClass cls, WeaponKind kind, int level, Rarity rarity, StatBlock stats,
                            Element element, String lore) {
    }

    public record ArmorSet(String id, String name, PlayerClass cls, int level, Rarity rarity, String style,
                           StatBlock totals, StatBlock bonus2, StatBlock bonus4Pct, String bonus4Text) {
    }

    public static final List<WeaponDef> WEAPONS = new ArrayList<>();
    public static final Map<String, ArmorSet> SETS = new LinkedHashMap<>();
    public static final String[] PIECES = {"casque", "plastron", "jambieres", "bottes"};
    public static final double[] PIECE_SHARE = {0.20, 0.35, 0.28, 0.17};

    private static void w(String id, PlayerClass cls, WeaponKind kind, int level, Rarity r, Element e, String lore, Object... stats) {
        WEAPONS.add(new WeaponDef(id, cls, kind, level, r, StatBlock.of(stats), e, lore));
    }

    private static void set(String id, String name, PlayerClass cls, int level, Rarity r, String style, StatBlock totals,
                            StatBlock bonus2, StatBlock bonus4Pct, String text) {
        SETS.put(id, new ArmorSet(id, name, cls, level, r, style, totals, bonus2, bonus4Pct, text));
    }

    static {
        PlayerClass G = PlayerClass.GUERRIER, M = PlayerClass.MAGE, A = PlayerClass.ARCHER, T = PlayerClass.TANK;
        Rarity C = Rarity.COMMUN, U = Rarity.PEU_COMMUN, R = Rarity.RARE, E = Rarity.EPIQUE, L = Rarity.LEGENDAIRE, Y = Rarity.MYTHIQUE;

        // ---------------- Guerrier
        w("epee_recrue", G, WeaponKind.SWORD, 1, C, null, "Une lame simple mais fiable.", ATK, 8);
        w("epee_milicien", G, WeaponKind.SWORD, 5, C, null, "Une épée de milice, robuste et bien équilibrée.", ATK, 14, FORCE, 1);
        w("lame_runique", G, WeaponKind.SWORD, 10, U, null, "Des runes anciennes luisent le long de la lame.", ATK, 22, CRIT, 2, FORCE, 3);
        w("lame_acier_trempe", G, WeaponKind.SWORD, 18, U, null, "Acier trempé dans la sève des forêts anciennes.", ATK, 36, CRIT, 3, FORCE, 4);
        w("epee_berserker", G, WeaponKind.SWORD, 25, R, null, "Arrachée au trésor du Roi Gobelin, elle a soif de batailles.", ATK, 55, CRIT_DMG, 15, MAX_HP, 50, FORCE, 6);
        w("epee_croise", G, WeaponKind.SWORD, 35, R, null, "Bénie par les croisés qui gardaient les cryptes.", ATK, 80, MAX_HP, 100, LIFESTEAL, 2, FORCE, 8);
        w("epee_seigneur_guerre", G, WeaponKind.SWORD, 45, E, null, "Forgée pour ceux qui commandent les armées.", ATK, 110, MAX_HP, 150, LIFESTEAL, 3, FORCE, 10);
        w("lame_tempetes", G, WeaponKind.SWORD, 58, E, null, "La foudre gronde à chaque coup porté.", ATK, 150, CRIT, 5, CRIT_DMG, 20, FORCE, 13);
        w("lame_demoniaque", G, WeaponKind.SWORD, 70, L, null, "Elle a soif du sang de ses ennemis.", ATK, 190, CRIT, 6, LIFESTEAL, 6, FORCE, 16);
        w("epee_roi_dragon", G, WeaponKind.SWORD, 82, L, null, "Forgée dans les écailles d'un wyrm boréal.", ATK, 250, CRIT, 8, LIFESTEAL, 5, MAX_HP, 250, FORCE, 20);
        w("excalibur_celeste", G, WeaponKind.SWORD, 95, Y, null, "L'épée légendaire des dieux de la guerre.", ATK, 320, CRIT, 10, CRIT_DMG, 40, MAX_HP, 400, FORCE, 25);
        // ---------------- Mage
        w("baton_apprenti", M, WeaponKind.STAFF, 1, C, Element.ARCANE, "Le premier bâton de tout apprenti.", MAG, 10, MAX_MANA, 20);
        w("baton_saule", M, WeaponKind.STAFF, 5, C, Element.ARCANE, "Taillé dans un saule qui murmure des sorts.", MAG, 16, MAX_MANA, 35);
        w("sceptre_givre", M, WeaponKind.STAFF, 10, U, Element.GIVRE, "Un froid mordant émane du cristal.", MAG, 26, MAX_MANA, 50, INTELLIGENCE, 3);
        w("sceptre_marees", M, WeaponKind.STAFF, 18, U, Element.GIVRE, "Il suit le rythme des marées des marais.", MAG, 42, MAX_MANA, 80, MANA_REGEN, 1, INTELLIGENCE, 4);
        w("baton_flammes", M, WeaponKind.STAFF, 25, R, Element.FEU, "Les flammes de l'enfer dansent à son sommet.", MAG, 65, CRIT, 3, MAX_MANA, 100, INTELLIGENCE, 6);
        w("baton_foudre", M, WeaponKind.STAFF, 35, R, Element.ECLAIR, "Des arcs électriques courent le long du bois.", MAG, 95, MAX_MANA, 150, CRIT, 4, INTELLIGENCE, 8);
        w("baton_arcanique", M, WeaponKind.STAFF, 45, E, Element.ARCANE, "Canalise les énergies arcaniques pures.", MAG, 130, MAX_MANA, 200, MANA_REGEN, 2, CDR, 5, INTELLIGENCE, 10);
        w("sceptre_ombre", M, WeaponKind.STAFF, 58, E, Element.OMBRE, "L'ombre s'y enroule comme un serpent.", MAG, 175, MAX_MANA, 250, CDR, 7, INTELLIGENCE, 13);
        w("sceptre_neant", M, WeaponKind.STAFF, 70, L, Element.NEANT, "Il murmure des secrets venus d'ailleurs.", MAG, 220, CRIT, 5, MAX_MANA, 300, INTELLIGENCE, 16);
        w("baton_phenix", M, WeaponKind.STAFF, 82, L, Element.FEU, "Une plume de phénix brûle éternellement à son sommet.", MAG, 290, MAX_MANA, 400, CRIT, 7, MANA_REGEN, 4, INTELLIGENCE, 20);
        w("baton_archimage", M, WeaponKind.STAFF, 95, Y, Element.ECLAIR, "Le bâton des Archimages éternels.", MAG, 360, MAX_MANA, 500, CDR, 10, MANA_REGEN, 5, INTELLIGENCE, 25);
        // ---------------- Archer
        w("arc_chasseur", A, WeaponKind.BOW, 1, C, null, "Un arc de chasse en bois souple.", ATK, 7, CRIT, 2);
        w("arc_court", A, WeaponKind.BOW, 5, C, null, "Court et maniable, idéal pour débuter.", ATK, 12, CRIT, 3);
        w("arc_elfique", A, WeaponKind.BOW, 10, U, null, "Léger et précis, façonné par les elfes.", ATK, 20, CRIT, 4, SPEED, 3, AGILITE, 3);
        w("arc_composite", A, WeaponKind.BOW, 18, U, null, "Bois, corne et tendon : une puissance surprenante.", ATK, 34, CRIT, 5, SPEED, 3, AGILITE, 4);
        w("arc_tempete", A, WeaponKind.BOW, 25, R, null, "Ses flèches sifflent comme le vent d'orage.", ATK, 50, CRIT, 6, ESQ, 2, AGILITE, 6);
        w("arc_sylvain", A, WeaponKind.BOW, 35, R, null, "Les esprits de la forêt guident ses flèches.", ATK, 75, CRIT, 7, ESQ, 3, AGILITE, 8);
        w("arc_faucon", A, WeaponKind.BOW, 45, E, null, "L'œil du faucon ne manque jamais sa proie.", ATK, 100, CRIT, 8, CRIT_DMG, 20, AGILITE, 10);
        w("arc_lune_argent", A, WeaponKind.BOW, 58, E, null, "Il brille d'une lueur argentée sous la lune.", ATK, 140, CRIT, 9, CRIT_DMG, 25, AGILITE, 13);
        w("arc_spectral", A, WeaponKind.BOW, 70, L, null, "Tendu avec une corde d'âme glacée.", ATK, 175, CRIT, 10, ESQ, 4, AGILITE, 16);
        w("arc_dragon", A, WeaponKind.BOW, 82, L, null, "Tendu avec un nerf de dragon, il ne rate jamais.", ATK, 235, CRIT, 12, CRIT_DMG, 35, ESQ, 5, AGILITE, 20);
        w("arc_aube_divine", A, WeaponKind.BOW, 95, Y, null, "Chaque flèche porte la lumière de l'aube.", ATK, 300, CRIT, 14, CRIT_DMG, 50, AGILITE, 25);
        // ---------------- Tank
        w("masse_garde", T, WeaponKind.HAMMER, 1, C, null, "L'arme réglementaire de la garde.", ATK, 6, DEF, 6, MAX_HP, 30);
        w("masse_cloutee", T, WeaponKind.HAMMER, 5, C, null, "Des clous de fer hérissent sa tête.", ATK, 10, DEF, 10, MAX_HP, 55);
        w("marteau_bastion", T, WeaponKind.HAMMER, 10, U, null, "Lourd comme un mur de forteresse.", ATK, 16, DEF, 15, MAX_HP, 80, VITALITE, 3);
        w("marteau_forgeron", T, WeaponKind.HAMMER, 18, U, null, "Le marteau d'un maître forgeron, lourd et fidèle.", ATK, 28, DEF, 25, MAX_HP, 140, VITALITE, 4);
        w("marteau_colosse", T, WeaponKind.HAMMER, 25, R, null, "Seul un colosse peut le soulever.", ATK, 40, DEF, 35, MAX_HP, 200, VITALITE, 6);
        w("masse_templier", T, WeaponKind.HAMMER, 35, R, null, "L'arme sacrée des templiers des cryptes.", ATK, 60, DEF, 52, MAX_HP, 300, HP_REGEN, 1, VITALITE, 8);
        w("masse_gardien", T, WeaponKind.HAMMER, 45, E, null, "Bénie par les gardiens sacrés.", ATK, 80, DEF, 70, MAX_HP, 400, HP_REGEN, 2, VITALITE, 10);
        w("marteau_runique", T, WeaponKind.HAMMER, 58, E, null, "Des runes de protection couvrent sa masse.", ATK, 110, DEF, 95, MAX_HP, 550, HP_REGEN, 3, VITALITE, 13);
        w("marteau_titan", T, WeaponKind.HAMMER, 70, L, null, "Forgé dans le cœur d'un volcan.", ATK, 140, DEF, 120, MAX_HP, 700, VITALITE, 16);
        w("marteau_rois_anciens", T, WeaponKind.HAMMER, 82, L, null, "Transmis de roi en roi depuis l'aube des temps.", ATK, 190, DEF, 160, MAX_HP, 950, VITALITE, 20);
        w("marteau_egide", T, WeaponKind.HAMMER, 95, Y, null, "Aucune force ne peut briser l'Égide.", ATK, 240, DEF, 200, MAX_HP, 1200, VITALITE, 25);

        // ---------------- Sets d'armure (totaux pour les 4 pieces)
        set("acier_soldat", "Acier du Soldat", G, 5, U, "plate", StatBlock.of(MAX_HP, 80, DEF, 12, ATK, 5, FORCE, 4),
                StatBlock.of(ATK, 5), StatBlock.of(ATK, 0.05), "+5 % d'Attaque");
        set("berserker", "Berserker", G, 25, R, "plate", StatBlock.of(MAX_HP, 300, DEF, 40, ATK, 20, CRIT, 2, FORCE, 10),
                StatBlock.of(ATK, 15), StatBlock.of(ATK, 0.08, CRIT_DMG, 0.10), "+8 % d'Attaque, +10 % de dégâts critiques");
        set("seigneur_guerre", "Seigneur de Guerre", G, 50, E, "plate", StatBlock.of(MAX_HP, 700, DEF, 90, ATK, 50, CRIT, 4, FORCE, 20),
                StatBlock.of(ATK, 30), StatBlock.of(ATK, 0.12, MAX_HP, 0.05), "+12 % d'Attaque, +5 % de PV");
        set("dieu_guerre", "Dieu de la Guerre", G, 85, L, "plate", StatBlock.of(MAX_HP, 1500, DEF, 180, ATK, 100, CRIT, 6, FORCE, 35),
                StatBlock.of(ATK, 50), StatBlock.of(ATK, 0.15, CRIT_DMG, 0.20, MAX_HP, 0.08), "+15 % d'Attaque, +20 % de dégâts critiques, +8 % de PV");

        set("apprenti", "Apprenti", M, 5, U, "robe", StatBlock.of(MAX_HP, 50, MAX_MANA, 60, MAG, 8, DEF, 5, INTELLIGENCE, 4),
                StatBlock.of(MAG, 6), StatBlock.of(MAG, 0.05), "+5 % de Puissance magique");
        set("sorcier", "Sorcier", M, 25, R, "robe", StatBlock.of(MAX_HP, 180, MAX_MANA, 200, MAG, 30, DEF, 18, MANA_REGEN, 1.5, INTELLIGENCE, 10),
                StatBlock.of(MAG, 18), StatBlock.of(MAG, 0.08, MAX_MANA, 0.10), "+8 % de Puissance magique, +10 % de mana");
        set("archimage", "Archimage", M, 50, E, "robe", StatBlock.of(MAX_HP, 400, MAX_MANA, 450, MAG, 70, DEF, 40, MANA_REGEN, 3, CDR, 6, INTELLIGENCE, 20),
                StatBlock.of(MAG, 35), StatBlock.of(MAG, 0.12, MANA_REGEN, 0.20), "+12 % de Puissance magique, +20 % de régénération de mana");
        set("avatar_arcanique", "Avatar Arcanique", M, 85, L, "robe", StatBlock.of(MAX_HP, 900, MAX_MANA, 900, MAG, 140, DEF, 80, MANA_REGEN, 5, CDR, 10, INTELLIGENCE, 35),
                StatBlock.of(MAG, 60), StatBlock.of(MAG, 0.15, MAX_MANA, 0.15, MAX_HP, 0.08), "+15 % de Puissance magique, +15 % de mana, +8 % de PV");

        set("chasseur", "Chasseur", A, 5, U, "leather", StatBlock.of(MAX_HP, 60, DEF, 7, ATK, 5, CRIT, 2, ESQ, 1, SPEED, 2, AGILITE, 4),
                StatBlock.of(CRIT, 2), StatBlock.of(ATK, 0.05), "+5 % d'Attaque");
        set("rodeur", "Rôdeur", A, 25, R, "leather", StatBlock.of(MAX_HP, 220, DEF, 25, ATK, 18, CRIT, 5, ESQ, 3, SPEED, 4, AGILITE, 10),
                StatBlock.of(CRIT, 3), StatBlock.of(ATK, 0.08, ESQ, 0.10), "+8 % d'Attaque, +10 % d'esquive");
        set("tireur_elite", "Tireur d'Élite", A, 50, E, "leather", StatBlock.of(MAX_HP, 500, DEF, 60, ATK, 45, CRIT, 8, ESQ, 5, SPEED, 6, AGILITE, 20),
                StatBlock.of(CRIT, 4), StatBlock.of(ATK, 0.12, CRIT_DMG, 0.15), "+12 % d'Attaque, +15 % de dégâts critiques");
        set("sylvestre", "Sylvestre Divine", A, 85, L, "leather", StatBlock.of(MAX_HP, 1100, DEF, 120, ATK, 90, CRIT, 12, ESQ, 8, SPEED, 10, AGILITE, 35),
                StatBlock.of(CRIT, 5), StatBlock.of(ATK, 0.15, CRIT_DMG, 0.25, SPEED, 0.10), "+15 % d'Attaque, +25 % de dégâts critiques, +10 % de vitesse");

        set("garde", "Garde", T, 5, U, "plate", StatBlock.of(MAX_HP, 120, DEF, 18, ATK, 2, HP_REGEN, 0.5, VITALITE, 4),
                StatBlock.of(DEF, 8), StatBlock.of(DEF, 0.08), "+8 % de Défense");
        set("gardien", "Gardien", T, 25, R, "plate", StatBlock.of(MAX_HP, 450, DEF, 70, ATK, 8, HP_REGEN, 2, VITALITE, 10),
                StatBlock.of(DEF, 25), StatBlock.of(DEF, 0.12, MAX_HP, 0.05), "+12 % de Défense, +5 % de PV");
        set("paladin", "Paladin", T, 50, E, "plate", StatBlock.of(MAX_HP, 1000, DEF, 160, ATK, 20, HP_REGEN, 4, VITALITE, 20),
                StatBlock.of(DEF, 50), StatBlock.of(DEF, 0.16, MAX_HP, 0.08, HP_REGEN, 0.20), "+16 % de Défense, +8 % de PV, +20 % de régénération");
        set("titan", "Titan Immortel", T, 85, L, "plate", StatBlock.of(MAX_HP, 2200, DEF, 320, ATK, 40, HP_REGEN, 8, VITALITE, 35),
                StatBlock.of(DEF, 90), StatBlock.of(DEF, 0.20, MAX_HP, 0.12), "+20 % de Défense, +12 % de PV");

        // panoplies intermediaires : statistiques interpolees entre les deux panoplies voisines de la classe
        between("veteran", "Vétéran", G, 15, U, "plate", "acier_soldat", "berserker");
        between("croise", "Croisé", G, 35, R, "plate", "berserker", "seigneur_guerre");
        between("conquerant", "Conquérant", G, 65, E, "plate", "seigneur_guerre", "dieu_guerre");
        between("seigneur_dragon", "Seigneur Dragon", G, 75, L, "plate", "seigneur_guerre", "dieu_guerre");
        between("acolyte", "Acolyte", M, 15, U, "robe", "apprenti", "sorcier");
        between("enchanteur", "Enchanteur", M, 35, R, "robe", "sorcier", "archimage");
        between("mage_bataille", "Mage de Bataille", M, 65, E, "robe", "archimage", "avatar_arcanique");
        between("oracle_astral", "Oracle Astral", M, 75, L, "robe", "archimage", "avatar_arcanique");
        between("eclaireur", "Éclaireur", A, 15, U, "leather", "chasseur", "rodeur");
        between("traqueur", "Traqueur", A, 35, R, "leather", "rodeur", "tireur_elite");
        between("lame_ombre", "Lame d'Ombre", A, 65, E, "leather", "tireur_elite", "sylvestre");
        between("chasseur_lunaire", "Chasseur Lunaire", A, 75, L, "leather", "tireur_elite", "sylvestre");
        between("sentinelle_fer", "Sentinelle de Fer", T, 15, U, "plate", "garde", "gardien");
        between("rempart", "Rempart", T, 35, R, "plate", "gardien", "paladin");
        between("croise_sacre", "Croisé Sacré", T, 65, E, "plate", "paladin", "titan");
        between("bastion_eternel", "Bastion Éternel", T, 75, L, "plate", "paladin", "titan");

        set("valkyrie", "Valkyrie Céleste", G, 100, Y, "plate",
                StatBlock.of(MAX_HP, 2100, DEF, 250, ATK, 150, CRIT, 8, FORCE, 45),
                StatBlock.of(ATK, 70), StatBlock.of(ATK, .18, CRIT_DMG, .25, MAX_HP, .10), "+18 % d'Attaque, +25 % de dégâts critiques, +10 % de PV");
        set("eternel_arcanes", "Éternel des Arcanes", M, 100, Y, "robe",
                StatBlock.of(MAX_HP, 1300, MAX_MANA, 1300, MAG, 210, DEF, 110, MANA_REGEN, 7, CDR, 12, INTELLIGENCE, 45),
                StatBlock.of(MAG, 85), StatBlock.of(MAG, .20, MAX_MANA, .20, MAX_HP, .10), "+20 % de Magie et mana, +10 % de PV");
        set("sentinelle_astrale", "Sentinelle Astrale", A, 100, Y, "leather",
                StatBlock.of(MAX_HP, 1600, DEF, 170, ATK, 140, CRIT, 14, ESQ, 10, SPEED, 12, AGILITE, 45),
                StatBlock.of(CRIT, 7), StatBlock.of(ATK, .18, CRIT_DMG, .30, SPEED, .12), "+18 % d'Attaque, +30 % de dégâts critiques, +12 % de vitesse");
        set("egide_divine", "Égide Divine", T, 100, Y, "plate",
                StatBlock.of(MAX_HP, 3100, DEF, 460, ATK, 60, HP_REGEN, 12, VITALITE, 45),
                StatBlock.of(DEF, 125), StatBlock.of(DEF, .25, MAX_HP, .15), "+25 % de Défense, +15 % de PV");

    }

    /** Panoplie intermediaire : totaux et bonus interpoles (selon le niveau) entre deux panoplies existantes. */
    private static void between(String id, String name, PlayerClass cls, int level, Rarity r, String style, String from, String to) {
        ArmorSet a = SETS.get(from), b = SETS.get(to);
        double t = (level - a.level()) / (double) (b.level() - a.level());
        StatBlock bonus4 = lerp(a.bonus4Pct(), b.bonus4Pct(), t, 100);
        set(id, name, cls, level, r, style, lerp(a.totals(), b.totals(), t, 0), lerp(a.bonus2(), b.bonus2(), t, 0), bonus4, bonusText(bonus4));
    }

    /** Interpolation lineaire de deux blocs de statistiques ; scale = precision d'arrondi (0 = selon le type de statistique). */
    private static StatBlock lerp(StatBlock a, StatBlock b, double t, int scale) {
        StatBlock out = new StatBlock();
        for (Stat s : Stat.values()) {
            double v = a.get(s) + (b.get(s) - a.get(s)) * t;
            if (v == 0) continue;
            int precision = scale > 0 ? scale : (s.kind == Stat.Kind.FLAT || s.kind == Stat.Kind.PRIMARY ? 1 : 10);
            out.set(s, Math.round(v * precision) / (double) precision);
        }
        return out;
    }

    private static String bonusText(StatBlock pct) {
        StringBuilder sb = new StringBuilder();
        for (Stat s : Stat.values()) {
            double v = pct.get(s);
            if (v <= 0) continue;
            String what = switch (s) {
                case ATK -> "d'Attaque";
                case MAG -> "de Puissance magique";
                case MAX_HP -> "de PV";
                case MAX_MANA -> "de mana";
                case DEF -> "de Défense";
                case CRIT -> "de chances de critique";
                case CRIT_DMG -> "de dégâts critiques";
                case ESQ -> "d'esquive";
                case SPEED -> "de vitesse";
                case HP_REGEN -> "de régénération";
                case MANA_REGEN -> "de régénération de mana";
                default -> "de " + s.label.toLowerCase(java.util.Locale.ROOT);
            };
            if (sb.length() > 0) sb.append(", ");
            sb.append("+").append(Math.round(v * 100)).append(" % ").append(what);
        }
        return sb.toString();
    }

    static {
        // l'equipement d'une classe donne aussi son second attribut (60 % du premier) ; le set universel donne les trois nouveaux
        for (WeaponDef w : WEAPONS) addSecondAttribute(w.cls(), w.stats());
        for (ArmorSet s : SETS.values()) addSecondAttribute(s.cls(), s.totals());
    }

    private static void addSecondAttribute(PlayerClass cls, StatBlock stats) {
        Stat[] main = cls.mainAttributes();
        if (main.length == 2) {
            if (stats.get(main[0]) > 0 && stats.get(main[1]) == 0) stats.set(main[1], Math.max(1, Math.round(stats.get(main[0]) * 0.6)));
        } else if (stats.get(FORCE) > 0) {
            for (Stat s : new Stat[]{FEROCITE, DEXTERITE, ENDURANCE}) {
                if (stats.get(s) == 0) stats.set(s, stats.get(FORCE));
            }
        }
    }

    public static StatBlock pieceStats(ArmorSet set, int pieceIndex) {
        StatBlock b = set.totals().scaled(PIECE_SHARE[pieceIndex]);
        // arrondi pour un affichage propre
        for (Stat s : Stat.values()) {
            double v = b.get(s);
            if (s.kind == Stat.Kind.FLAT || s.kind == Stat.Kind.PRIMARY) {
                b.set(s, Math.max(v > 0 ? 1 : 0, Math.round(v)));
            } else {
                b.set(s, Math.round(v * 10) / 10.0);
            }
        }
        return b;
    }

    public static RpgItemDef.Slot pieceSlot(int pieceIndex) {
        return switch (pieceIndex) {
            case 0 -> RpgItemDef.Slot.HEAD;
            case 1 -> RpgItemDef.Slot.CHEST;
            case 2 -> RpgItemDef.Slot.LEGS;
            default -> RpgItemDef.Slot.FEET;
        };
    }
}
