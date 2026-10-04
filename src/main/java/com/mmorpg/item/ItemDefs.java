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
        w("lame_runique", G, WeaponKind.SWORD, 10, U, null, "Des runes anciennes luisent le long de la lame.", ATK, 22, CRIT, 2, FORCE, 3);
        w("hache_berserker", G, WeaponKind.AXE, 25, R, null, "Arrachée au trésor du Roi Gobelin.", ATK, 55, CRIT_DMG, 15, MAX_HP, 50, FORCE, 6);
        w("epee_seigneur_guerre", G, WeaponKind.SWORD, 45, E, null, "Forgée pour ceux qui commandent les armées.", ATK, 110, MAX_HP, 150, LIFESTEAL, 3, FORCE, 10);
        w("lame_demoniaque", G, WeaponKind.SWORD, 70, L, null, "Elle a soif du sang de ses ennemis.", ATK, 190, CRIT, 6, LIFESTEAL, 6, FORCE, 16);
        w("excalibur_celeste", G, WeaponKind.SWORD, 95, Y, null, "L'épée légendaire des dieux de la guerre.", ATK, 320, CRIT, 10, CRIT_DMG, 40, MAX_HP, 400, FORCE, 25);
        // ---------------- Mage
        w("baton_apprenti", M, WeaponKind.STAFF, 1, C, Element.ARCANE, "Le premier bâton de tout apprenti.", MAG, 10, MAX_MANA, 20);
        w("sceptre_givre", M, WeaponKind.STAFF, 10, U, Element.GIVRE, "Un froid mordant émane du cristal.", MAG, 26, MAX_MANA, 50, INTELLIGENCE, 3);
        w("baton_flammes", M, WeaponKind.STAFF, 25, R, Element.FEU, "Les flammes de l'enfer dansent à son sommet.", MAG, 65, CRIT, 3, MAX_MANA, 100, INTELLIGENCE, 6);
        w("baton_arcanique", M, WeaponKind.STAFF, 45, E, Element.ARCANE, "Canalise les énergies arcaniques pures.", MAG, 130, MAX_MANA, 200, MANA_REGEN, 2, CDR, 5, INTELLIGENCE, 10);
        w("sceptre_neant", M, WeaponKind.STAFF, 70, L, Element.NEANT, "Il murmure des secrets venus d'ailleurs.", MAG, 220, CRIT, 5, MAX_MANA, 300, INTELLIGENCE, 16);
        w("baton_archimage", M, WeaponKind.STAFF, 95, Y, Element.ECLAIR, "Le bâton des Archimages éternels.", MAG, 360, MAX_MANA, 500, CDR, 10, MANA_REGEN, 5, INTELLIGENCE, 25);
        // ---------------- Archer
        w("arc_chasseur", A, WeaponKind.BOW, 1, C, null, "Un arc de chasse en bois souple.", ATK, 7, CRIT, 2);
        w("arc_elfique", A, WeaponKind.BOW, 10, U, null, "Léger et précis, façonné par les elfes.", ATK, 20, CRIT, 4, SPEED, 3, AGILITE, 3);
        w("arc_tempete", A, WeaponKind.BOW, 25, R, null, "Ses flèches sifflent comme le vent d'orage.", ATK, 50, CRIT, 6, ESQ, 2, AGILITE, 6);
        w("arc_faucon", A, WeaponKind.BOW, 45, E, null, "L'œil du faucon ne manque jamais sa proie.", ATK, 100, CRIT, 8, CRIT_DMG, 20, AGILITE, 10);
        w("arc_spectral", A, WeaponKind.BOW, 70, L, null, "Tendu avec une corde d'âme glacée.", ATK, 175, CRIT, 10, ESQ, 4, AGILITE, 16);
        w("arc_aube_divine", A, WeaponKind.BOW, 95, Y, null, "Chaque flèche porte la lumière de l'aube.", ATK, 300, CRIT, 14, CRIT_DMG, 50, AGILITE, 25);
        // ---------------- Tank
        w("masse_garde", T, WeaponKind.HAMMER, 1, C, null, "L'arme réglementaire de la garde.", ATK, 6, DEF, 6, MAX_HP, 30);
        w("marteau_bastion", T, WeaponKind.HAMMER, 10, U, null, "Lourd comme un mur de forteresse.", ATK, 16, DEF, 15, MAX_HP, 80, VITALITE, 3);
        w("marteau_colosse", T, WeaponKind.HAMMER, 25, R, null, "Seul un colosse peut le soulever.", ATK, 40, DEF, 35, MAX_HP, 200, VITALITE, 6);
        w("masse_gardien", T, WeaponKind.HAMMER, 45, E, null, "Bénie par les gardiens sacrés.", ATK, 80, DEF, 70, MAX_HP, 400, HP_REGEN, 2, VITALITE, 10);
        w("marteau_titan", T, WeaponKind.HAMMER, 70, L, null, "Forgé dans le cœur d'un volcan.", ATK, 140, DEF, 120, MAX_HP, 700, VITALITE, 16);
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

        set("neant_primordial", "Néant Primordial", PlayerClass.NONE, 100, Y, "plate",
                StatBlock.of(MAX_HP, 2000, DEF, 220, ATK, 120, MAG, 120, CRIT, 8, ESQ, 5,
                        FORCE, 15, AGILITE, 15, INTELLIGENCE, 15, VITALITE, 15, ESPRIT, 15),
                StatBlock.of(ATK, 40, MAG, 40), StatBlock.of(ATK, 0.15, MAG, 0.15, MAX_HP, 0.10), "+15 % d'Attaque et de Puissance magique, +10 % de PV");
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
