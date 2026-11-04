package com.mmorpg.item;

import com.mmorpg.registry.ModDataComponents;
import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.rpg.Stat;
import com.mmorpg.rpg.StatBlock;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** Affixes aleatoires des armes, reforge de statistique et signature du createur (composants de l'objet). */
public final class Affixes {
    /** Part de la statistique de base deplacee par une reforge. */
    public static final double REFORGE_SHARE = 0.4;
    private static final Stat[] POOL = {Stat.ATK, Stat.MAG, Stat.CRIT, Stat.CRIT_DMG, Stat.LIFESTEAL, Stat.ATK_SPEED, Stat.SPEED, Stat.MAX_HP, Stat.CDR, Stat.MANA_REGEN};

    private Affixes() {
    }

    public static int count(RpgItemDef def) {
        return switch (def.rarity()) {
            case COMMUN -> 0;
            case PEU_COMMUN, RARE -> 1;
            case EPIQUE, LEGENDAIRE -> 2;
            case MYTHIQUE -> 3;
        };
    }

    private static boolean allowed(Stat s, PlayerClass cls) {
        if (s == Stat.MAG || s == Stat.MANA_REGEN) return cls == PlayerClass.MAGE;
        if (s == Stat.ATK) return cls != PlayerClass.MAGE;
        return true;
    }

    /** Couleurs des paliers, du plus faible au plus fort : vert, bleu, violet, jaune, rose. */
    public static final int[] TIER_COLORS = {0x55E055, 0x4FA3FF, 0xB66CFF, 0xFFD740, 0xFF6FB5};
    public static final String[] TIER_NAMES = {"Vert", "Bleu", "Violet", "Jaune", "Rose"};

    /** Palier 0..4 d'un affixe selon la qualite du tirage (0,7 a 1,3 de la valeur de base). */
    public static int tier(Stat s, double value, int level) {
        double ratio = value / base(s, level);
        return Math.max(0, Math.min(4, (int) Math.floor((ratio - 0.7) / 0.6 * 5)));
    }

    private static double value(Stat s, int level, Random r) {
        double roll = 0.7 + r.nextDouble() * 0.6;
        return Math.round(base(s, level) * roll * 10) / 10.0;
    }

    private static double base(Stat s, int level) {
        return switch (s) {
            case ATK, MAG -> 2 + level * 0.4;
            case MAX_HP -> 10 + level * 2;
            case CRIT -> 1 + level * 0.05;
            case CRIT_DMG -> 3 + level * 0.2;
            case LIFESTEAL -> 0.5 + level * 0.02;
            case ATK_SPEED, SPEED -> 1 + level * 0.04;
            case CDR -> 1 + level * 0.04;
            default -> 0.3 + level * 0.02;
        };
    }

    /** Tire (ou retire) les affixes de l'arme. */
    public static void roll(ItemStack stack, RpgItemDef def, Random r) {
        if (def.slot() != RpgItemDef.Slot.WEAPON) return;
        List<Stat> pool = new ArrayList<>();
        for (Stat s : POOL) if (allowed(s, def.playerClass())) pool.add(s);
        StringBuilder sb = new StringBuilder();
        for (int i = 0, n = count(def); i < n && !pool.isEmpty(); i++) {
            Stat s = pool.remove(r.nextInt(pool.size()));
            if (!sb.isEmpty()) sb.append(';');
            sb.append(s.name()).append(':').append(value(s, def.level(), r));
        }
        if (sb.isEmpty()) stack.remove(ModDataComponents.AFFIXES.get());
        else stack.set(ModDataComponents.AFFIXES.get(), sb.toString());
    }

    public static Map<Stat, Double> affixes(ItemStack stack) {
        Map<Stat, Double> out = new LinkedHashMap<>();
        String raw = stack.get(ModDataComponents.AFFIXES.get());
        if (raw == null || raw.isEmpty()) return out;
        for (String part : raw.split(";")) {
            String[] kv = part.split(":");
            try {
                out.merge(Stat.valueOf(kv[0]), Double.parseDouble(kv[1]), Double::sum);
            } catch (RuntimeException ignored) {
            }
        }
        return out;
    }

    /** {de, vers} d'une reforge, ou null. */
    public static Stat[] reforge(ItemStack stack) {
        String raw = stack.get(ModDataComponents.REFORGE.get());
        if (raw == null) return null;
        String[] p = raw.split(">");
        try {
            return new Stat[]{Stat.valueOf(p[0]), Stat.valueOf(p[1])};
        } catch (RuntimeException e) {
            return null;
        }
    }

    public static String creator(ItemStack stack) {
        return stack.get(ModDataComponents.CREATOR.get());
    }

    /** Ajoute au bloc les affixes et la reforge (la reforge deplace une part de la statistique de base). */
    public static void apply(StatBlock flat, ItemStack stack, RpgItemDef def) {
        for (Map.Entry<Stat, Double> e : affixes(stack).entrySet()) flat.add(e.getKey(), e.getValue());
        Stat[] rf = reforge(stack);
        if (rf != null) {
            double moved = def.stats().get(rf[0]) * RpgEquipment.upgradeMult(stack) * REFORGE_SHARE;
            flat.add(rf[0], -moved);
            flat.add(rf[1], moved);
        }
    }
}
