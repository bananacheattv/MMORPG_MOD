package com.mmorpg.item;

import com.mmorpg.rpg.ClientView;
import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.rpg.Rarity;
import com.mmorpg.rpg.Stat;
import com.mmorpg.rpg.StatBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.ItemStack;

import java.util.function.Consumer;

/** Construction des infobulles RPG (rarete, prerequis, statistiques, bonus de set). */
public final class ItemTooltips {
    private static final int GOLD = 0xE8C060;
    private static final int GREY = 0x9A9488;
    private static final int RED = 0xFF5555;
    private static final int GREEN = 0x66E066;

    private ItemTooltips() {
    }

    public static MutableComponent rarityLine(Rarity rarity, String kind) {
        return Component.literal("◆ " + rarity.label + " ").withColor(rarity.rgb())
                .append(Component.literal("— " + kind).withColor(GREY));
    }

    public static void equipment(ItemStack stack, RpgItemDef def, String kind, Consumer<Component> out) {
        out.accept(rarityLine(def.rarity(), kind));
        PlayerClass cls = ClientView.playerClass;
        int level = ClientView.level;
        if (def.playerClass() != PlayerClass.NONE) {
            boolean ok = def.playerClass() == cls;
            out.accept(Component.literal("Classe : ").withColor(GREY)
                    .append(Component.literal(def.playerClass().label).withColor(ok ? def.playerClass().color & 0xFFFFFF : RED)));
        } else {
            out.accept(Component.literal("Classe : ").withColor(GREY).append(Component.literal("Toutes").withColor(GREEN)));
        }
        out.accept(Component.literal("Niveau requis : ").withColor(GREY)
                .append(Component.literal(String.valueOf(def.level())).withColor(level >= def.level() ? GREEN : RED)));
        int up = RpgEquipment.upgrade(stack);
        if (up > 0) {
            out.accept(Component.literal("Amélioration : +" + up + " (+" + Math.round(up * RpgEquipment.UPGRADE_BONUS * 100) + " % de stats)").withColor(0xFFD040));
        }
        out.accept(Component.literal(" "));
        double mult = RpgEquipment.upgradeMult(stack);
        statLines(def.stats(), mult, out);
        if (def.setId() != null) {
            ItemDefs.ArmorSet set = ItemDefs.SETS.get(def.setId());
            if (set != null) {
                int count = ClientView.setPieceCounter.applyAsInt(set.id());
                out.accept(Component.literal(" "));
                out.accept(Component.literal("Set « " + set.name() + " » (" + count + "/4)").withColor(GOLD));
                MutableComponent two = Component.literal("  (2) ");
                StringBuilder sb = new StringBuilder();
                for (Stat s : Stat.values()) {
                    double v = set.bonus2().get(s);
                    if (v != 0) {
                        if (!sb.isEmpty()) sb.append(", ");
                        sb.append(s.format(v, true)).append(' ').append(s.label);
                    }
                }
                out.accept(two.append(sb.toString()).withColor(count >= 2 ? GREEN : GREY));
                out.accept(Component.literal("  (4) " + set.bonus4Text()).withColor(count >= 4 ? GREEN : GREY));
            }
        }
        if (def.lore() != null && !def.lore().isEmpty()) {
            out.accept(Component.literal(" "));
            out.accept(Component.literal("« " + def.lore() + " »").withColor(0x8A8070).withStyle(s -> s.withItalic(true)));
        }
    }

    public static void statLines(StatBlock stats, double mult, Consumer<Component> out) {
        for (Stat s : Stat.values()) {
            double v = stats.get(s);
            if (v == 0) continue;
            double shown = s.kind == Stat.Kind.FLAT || s.kind == Stat.Kind.PRIMARY ? Math.round(v * mult) : v * mult;
            out.accept(Component.literal("  " + s.format(shown, true) + " ").withColor(s.color & 0xFFFFFF)
                    .append(Component.literal(s.label).withColor(0xD8D0C0)));
        }
    }

    public static void simple(Rarity rarity, String kind, String lore, Consumer<Component> out) {
        out.accept(rarityLine(rarity, kind));
        if (lore != null && !lore.isEmpty()) {
            for (String line : lore.split("\n")) {
                out.accept(Component.literal(line).withColor(0xB0A890));
            }
        }
    }
}
