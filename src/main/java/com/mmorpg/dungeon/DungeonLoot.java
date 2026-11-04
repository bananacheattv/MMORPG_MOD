package com.mmorpg.dungeon;

import com.mmorpg.config.ConfigManager;
import com.mmorpg.config.LootTables;
import com.mmorpg.crafting.ForgeRecipes;
import com.mmorpg.registry.ModItems;
import com.mmorpg.world.DungeonBuilder.DungeonType;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.ChestBlockEntity;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Contenu aleatoire des coffres de donjon, tire dans les tables de butin editables (donjon_*, voir /mmorpg donjonsetup). */
public final class DungeonLoot {
    /** Nombre de tirages (ponderes) par coffre de salle / par coffre du tresor. */
    public static final int ROOM_DRAWS = 6, TREASURE_DRAWS = 9;

    private DungeonLoot() {
    }

    public static List<ItemStack> roll(String table, int draws, RandomSource r) {
        List<LootTables.Entry> entries = ConfigManager.loot().table(table);
        int total = 0;
        for (var e : entries) total += Math.max(0, e.weight);
        Map<Item, Integer> counts = new LinkedHashMap<>();
        if (total > 0) {
            for (int i = 0; i < draws; i++) {
                int pick = r.nextInt(total);
                for (var e : entries) {
                    pick -= Math.max(0, e.weight);
                    if (pick >= 0) continue;
                    Item item = e.isGold() ? ModItems.PIECE_OR.get() : ForgeRecipes.resolveItem(e.item);
                    if (item != null && item != Items.AIR) counts.merge(item, e.min + r.nextInt(Math.max(1, e.max - e.min + 1)), Integer::sum);
                    break;
                }
            }
        }
        List<ItemStack> out = new ArrayList<>();
        counts.forEach((item, n) -> {
            while (n > 0) {
                ItemStack s = new ItemStack(item, 1);
                int k = Math.min(n, Math.max(1, s.getMaxStackSize()));
                s.setCount(k);
                out.add(s);
                n -= k;
            }
        });
        return out;
    }

    public static List<ItemStack> room(DungeonType t, RandomSource r) {
        return roll(LootTables.dungeonRooms(t.id), ROOM_DRAWS, r);
    }

    /** Tresor du boss ; chaque niveau de difficulte ajoute un coffre de salle. */
    public static List<ItemStack> treasure(DungeonType t, RandomSource r, int bonus) {
        List<ItemStack> l = roll(LootTables.dungeonTreasure(t.id), TREASURE_DRAWS, r);
        for (int i = 0; i < bonus; i++) l.addAll(room(t, r));
        return l.size() > 27 ? new ArrayList<>(l.subList(0, 27)) : l;
    }

    /** Vide puis remplit le coffre pose a cette position (au hasard dans ses 27 cases). */
    public static void fill(ServerLevel level, BlockPos p, List<ItemStack> items, RandomSource r) {
        if (!(level.getBlockEntity(p) instanceof ChestBlockEntity chest)) return;
        chest.clearContent();
        for (ItemStack s : items) {
            int slot = r.nextInt(27);
            for (int i = 0; i < 27 && !chest.getItem(slot).isEmpty(); i++) slot = (slot + 1) % 27;
            chest.setItem(slot, s);
        }
        chest.setChanged();
    }
}
