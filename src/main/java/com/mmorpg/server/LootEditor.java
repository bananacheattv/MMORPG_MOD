package com.mmorpg.server;

import com.google.gson.reflect.TypeToken;
import com.mmorpg.config.ConfigManager;
import com.mmorpg.config.LootTables;
import com.mmorpg.crafting.ForgeRecipes;
import com.mmorpg.network.Net;
import com.mmorpg.network.S2COpenScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;

/** Edition en jeu des butins des caisses et des Lucky Blocks (operateurs uniquement, valide cote serveur). */
public final class LootEditor {
    private static final java.lang.reflect.Type LIST = new TypeToken<List<LootTables.Entry>>() {
    }.getType();

    private LootEditor() {
    }

    public static CompoundTag snapshot() {
        CompoundTag t = new CompoundTag();
        CompoundTag tables = new CompoundTag();
        for (String id : LootTables.ALL) {
            tables.putString(id, QuestEditor.JSON.toJson(ConfigManager.loot().table(id)));
        }
        t.put("tables", tables);
        return t;
    }

    public static void open(ServerPlayer p) {
        if (QuestEditor.allowed(p)) Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.LOOT_EDITOR, snapshot()));
    }

    public static void action(ServerPlayer p, CompoundTag data) {
        if (!QuestEditor.allowed(p)) return;
        String status;
        boolean ok = false;
        try {
            String table = data.getStringOr("table", "");
            if (!LootTables.ALL.contains(table)) throw new IllegalArgumentException("Table inconnue.");
            String json = data.getStringOr("json", "");
            if (json.length() > 20000) throw new IllegalArgumentException("Table trop grande.");
            List<LootTables.Entry> entries = QuestEditor.JSON.fromJson(json, LIST);
            validate(entries);
            ConfigManager.saveLootTable(table, entries);
            status = LootTables.label(table) + " : " + entries.size() + " butins enregistrés.";
            ok = true;
        } catch (Exception e) {
            status = e instanceof IllegalArgumentException ? e.getMessage() : "Enregistrement impossible : " + e.getMessage();
        }
        CompoundTag reply = snapshot();
        reply.putString("status", status);
        reply.putBoolean("ok", ok);
        Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.LOOT_EDITOR, reply));
    }

    private static void validate(List<LootTables.Entry> entries) {
        if (entries == null) throw new IllegalArgumentException("Table vide.");
        if (entries.size() > LootTables.MAX_ENTRIES) throw new IllegalArgumentException("Maximum " + LootTables.MAX_ENTRIES + " butins par table.");
        int total = 0;
        for (LootTables.Entry e : entries) {
            if (e == null || e.item == null) throw new IllegalArgumentException("Butin invalide.");
            if (!e.isGold()) {
                Item item = ForgeRecipes.resolveItem(e.item);
                if (item == null || item == Items.AIR) throw new IllegalArgumentException("Objet inconnu : " + e.item);
            }
            int cap = e.isGold() ? 100000 : 999;
            if (e.min < 1 || e.max < e.min || e.max > cap) throw new IllegalArgumentException("Quantités invalides pour " + e.item + ".");
            if (e.weight < 0 || e.weight > 10000) throw new IllegalArgumentException("Poids invalide pour " + e.item + ".");
            total += e.weight;
        }
        if (total <= 0) throw new IllegalArgumentException("Au moins un butin doit avoir un poids supérieur à 0.");
    }
}
