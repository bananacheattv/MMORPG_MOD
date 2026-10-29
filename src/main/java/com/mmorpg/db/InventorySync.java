package com.mmorpg.db;

import com.mmorpg.MMORPG;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.nbt.TagParser;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * Inventaire du joueur (sac, armure, main secondaire) et coffre de l'Ender en texte (SNBT), pour les suivre d'un serveur a l'autre.
 * Les objets passent par le codec d'objets du jeu : composants, enchantements et noms personnalises sont conserves.
 */
public final class InventorySync {
    private InventorySync() {
    }

    private static ListTag save(Container c, RegistryOps<Tag> ops, String where) {
        ListTag list = new ListTag();
        for (int i = 0; i < c.getContainerSize(); i++) {
            ItemStack s = c.getItem(i);
            if (s.isEmpty()) continue;
            var enc = ItemStack.CODEC.encodeStart(ops, s);
            Tag t = enc.result().orElse(null);
            if (t == null) {
                MMORPG.LOGGER.error("[MMORPG] Base de donnees : objet non sauvegardable ({} case {})", where, i);
                continue;
            }
            CompoundTag e = new CompoundTag();
            e.putInt("slot", i);
            e.put("item", t);
            list.add(e);
        }
        return list;
    }

    private static void load(Container c, ListTag list, RegistryOps<Tag> ops) {
        c.clearContent();
        for (int i = 0; i < list.size(); i++) {
            CompoundTag e = list.getCompoundOrEmpty(i);
            Tag t = e.get("item");
            if (t == null) continue;
            ItemStack s = ItemStack.CODEC.parse(ops, t).result().orElse(ItemStack.EMPTY);
            int slot = e.getIntOr("slot", -1);
            if (!s.isEmpty() && slot >= 0 && slot < c.getContainerSize()) c.setItem(slot, s);
        }
    }

    /** Sac et coffre de l'Ender du joueur, en texte. */
    public static String encode(ServerPlayer p) {
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, p.registryAccess());
        CompoundTag t = new CompoundTag();
        t.put("inv", save(p.getInventory(), ops, "sac"));
        t.put("ender", save(p.getEnderChestInventory(), ops, "coffre de l'Ender"));
        return t.toString();
    }

    /** Remplace le sac et le coffre de l'Ender par ceux du texte ; faux si le texte est illisible (rien n'est alors modifie). */
    public static boolean apply(ServerPlayer p, String snbt) {
        try {
            CompoundTag t = TagParser.parseCompoundFully(snbt);
            RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, p.registryAccess());
            load(p.getInventory(), t.getListOrEmpty("inv"), ops);
            load(p.getEnderChestInventory(), t.getListOrEmpty("ender"), ops);
            p.getInventory().setChanged();
            p.inventoryMenu.broadcastChanges();
            return true;
        } catch (Exception e) {
            MMORPG.LOGGER.warn("[MMORPG] Inventaire de {} illisible dans la base : {}", p.getGameProfile().name(), e.getMessage());
            return false;
        }
    }
}
