package com.mmorpg.rpg;

import com.mmorpg.item.ItemDefs;
import com.mmorpg.item.RpgEquipment;
import com.mmorpg.item.RpgItemDef;
import com.mmorpg.pet.PetType;
import com.mmorpg.skill.Skills;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Calcul des statistiques finales d'un joueur.
 *
 * <pre>
 * 1. Attributs primaires = base de classe + croissance x (niveau - 1) + points repartis + equipement + familier
 * 2. Statistiques derivees = formules a partir des attributs et du niveau + bonus plats (equipement, familier, sets)
 * 3. Multiplicateur d'evolution (+5 % PV/ATQ/MAG/DEF par palier d'evolution)
 * 4. Bonus en pourcentage (passifs, bonus de set 4 pieces, effets temporaires)
 * 5. Plafonds (critique 75 %, esquive 45 %, ...)
 * </pre>
 */
public final class StatCalculator {
    private static final EquipmentSlot[] ARMOR_SLOTS = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};

    private StatCalculator() {
    }

    public static StatBlock compute(Player player, PlayerData data) {
        PlayerClass cls = data.playerClass;
        int level = data.level;
        StatBlock flat = new StatBlock();
        StatBlock pct = new StatBlock();

        // --- equipement
        Map<String, Integer> setCounts = new HashMap<>();
        ItemStack main = player.getMainHandItem();
        if (main.getItem() instanceof RpgEquipment eq) {
            RpgItemDef def = eq.rpgDef();
            if (def.slot() == RpgItemDef.Slot.WEAPON && def.usableBy(cls, level)) {
                flat.addScaled(def.stats(), RpgEquipment.upgradeMult(main));
            }
        }
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.getItem() instanceof RpgEquipment eq) {
                RpgItemDef def = eq.rpgDef();
                if (def.slot() != RpgItemDef.Slot.WEAPON && def.usableBy(cls, level)) {
                    flat.addScaled(def.stats(), RpgEquipment.upgradeMult(stack));
                    if (def.setId() != null) setCounts.merge(def.setId(), 1, Integer::sum);
                }
            }
        }
        for (Map.Entry<String, Integer> e : setCounts.entrySet()) {
            ItemDefs.ArmorSet set = ItemDefs.SETS.get(e.getKey());
            if (set == null) continue;
            if (e.getValue() >= 2) flat.addAll(set.bonus2());
            if (e.getValue() >= 4) pct.addAll(set.bonus4Pct());
        }

        // --- familier
        PetType pet = PetType.byId(data.activePet);
        if (pet != null && data.petXp.containsKey(pet.id)) {
            flat.addAll(pet.bonusAt(data.petLevel(pet.id)));
        }

        // --- passifs de classe
        int r;
        if ((r = data.skillRank(Skills.MAITRISE_ARMES.id)) > 0 && cls == PlayerClass.GUERRIER) {
            pct.add(Stat.ATK, Skills.MAITRISE_ARMES.power * r);
            flat.add(Stat.CRIT, Skills.MAITRISE_ARMES.power2 * r);
        }
        if ((r = data.skillRank(Skills.FLUX_ARCANIQUE.id)) > 0 && cls == PlayerClass.MAGE) {
            pct.add(Stat.MAG, Skills.FLUX_ARCANIQUE.power * r);
            pct.add(Stat.MANA_REGEN, Skills.FLUX_ARCANIQUE.power2 * r);
        }
        if ((r = data.skillRank(Skills.OEIL_DE_LYNX.id)) > 0 && cls == PlayerClass.ARCHER) {
            flat.add(Stat.CRIT, Skills.OEIL_DE_LYNX.power * r);
            flat.add(Stat.CRIT_DMG, Skills.OEIL_DE_LYNX.power2 * r);
        }
        if ((r = data.skillRank(Skills.PEAU_DE_FER.id)) > 0 && cls == PlayerClass.TANK) {
            pct.add(Stat.MAX_HP, Skills.PEAU_DE_FER.power * r);
            pct.add(Stat.DEF, Skills.PEAU_DE_FER.power2 * r);
        }

        // --- effets temporaires
        for (Buff b : data.buffs.values()) {
            switch (b.type) {
                case WAR_CRY -> {
                    pct.add(Stat.ATK, b.magnitude);
                    flat.add(Stat.SPEED, b.magnitude2 * 100);
                }
                case RAGE -> {
                    pct.add(Stat.ATK, b.magnitude);
                    flat.add(Stat.LIFESTEAL, b.magnitude2 * 100);
                }
                case AGILITY -> flat.add(Stat.SPEED, b.magnitude * 100);
                case TAUNT -> pct.add(Stat.DEF, b.magnitude);
                case FORTRESS -> flat.add(Stat.SPEED, -30);
                default -> {
                }
            }
        }

        // --- attributs primaires
        int np = Stat.PRIMARIES.length;
        double[] prim = new double[np];
        for (int i = 0; i < np; i++) {
            prim[i] = cls.baseAttributes[i] + cls.growth[i] * (level - 1) + data.allocated[i] + flat.get(Stat.PRIMARIES[i]);
            prim[i] *= 1 + pct.get(Stat.PRIMARIES[i]);
        }
        double FOR = prim[0], AGI = prim[1], INT = prim[2], VIT = prim[3], ESP = prim[4], FER = prim[5], DEX = prim[6], END = prim[7];

        StatBlock out = new StatBlock();
        for (int i = 0; i < np; i++) out.set(Stat.PRIMARIES[i], prim[i]);

        double evo = 1.0 + 0.05 * PlayerClass.tier(level);
        double atkFromAgi = cls == PlayerClass.ARCHER ? 1.3 : 0.6;
        double atkFromFor = cls == PlayerClass.ARCHER ? 0.6 : 1.4;

        out.set(Stat.MAX_HP, (cls.baseHp + cls.hpPerLevel * (level - 1) + VIT * 12 + FOR * 2 + END * 5 + flat.get(Stat.MAX_HP)) * evo);
        out.set(Stat.MAX_MANA, 50 + cls.manaPerLevel * (level - 1) + INT * 6 + ESP * 4 + flat.get(Stat.MAX_MANA));
        out.set(Stat.ATK, (5 + level * 0.5 + FOR * atkFromFor + AGI * atkFromAgi + FER * 0.5 + DEX * 0.4 + flat.get(Stat.ATK)) * evo);
        out.set(Stat.MAG, (2 + level * 0.5 + INT * 2.0 + ESP * 0.5 + flat.get(Stat.MAG)) * evo);
        out.set(Stat.DEF, (level * 0.5 + VIT * 0.8 + FOR * 0.3 + END * 1.0 + flat.get(Stat.DEF)) * evo);
        out.set(Stat.CRIT, 5 + AGI * 0.06 + DEX * 0.08 + flat.get(Stat.CRIT));
        out.set(Stat.CRIT_DMG, 150 + FOR * 0.05 + FER * 0.5 + flat.get(Stat.CRIT_DMG));
        out.set(Stat.ESQ, AGI * 0.07 + flat.get(Stat.ESQ));
        out.set(Stat.SPEED, AGI * 0.03 + flat.get(Stat.SPEED));
        out.set(Stat.ATK_SPEED, AGI * 0.04 + DEX * 0.03 + flat.get(Stat.ATK_SPEED));
        out.set(Stat.HP_REGEN, 0.5 + VIT * 0.05 + END * 0.08 + level * 0.05 + flat.get(Stat.HP_REGEN));
        out.set(Stat.MANA_REGEN, 1.0 + ESP * 0.12 + INT * 0.03 + flat.get(Stat.MANA_REGEN));
        out.set(Stat.LIFESTEAL, FER * 0.02 + flat.get(Stat.LIFESTEAL));
        out.set(Stat.CDR, ESP * 0.03 + flat.get(Stat.CDR));

        for (Stat s : Stat.values()) {
            if (s.isPrimary()) continue;
            double v = out.get(s) * (1 + pct.get(s));
            out.set(s, Math.max(s == Stat.SPEED ? -50 : 0, Math.min(s.cap(), v)));
        }
        out.set(Stat.MAX_HP, Math.max(1, out.get(Stat.MAX_HP)));
        out.set(Stat.MAX_MANA, Math.max(1, out.get(Stat.MAX_MANA)));
        return out;
    }

    /** Nombre de pieces d'un set equipees (pour l'affichage). */
    public static int setPieces(Player player, String setId) {
        int n = 0;
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.getItem() instanceof RpgEquipment eq && setId.equals(eq.rpgDef().setId())) n++;
        }
        return n;
    }

    /** Empreinte de l'equipement porte : permet de detecter un changement sans recalculer a chaque tick. */
    public static int equipmentHash(Player player) {
        int h = player.getMainHandItem().getItem().hashCode() * 31 + RpgEquipment.upgrade(player.getMainHandItem());
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            ItemStack s = player.getItemBySlot(slot);
            h = h * 31 + s.getItem().hashCode() + RpgEquipment.upgrade(s) * 7;
        }
        return h;
    }
}
