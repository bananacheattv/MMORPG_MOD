package com.mmorpg.server;

import com.mmorpg.config.ConfigManager;
import com.mmorpg.config.MobConfig;
import com.mmorpg.network.Net;
import com.mmorpg.network.Payloads;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

/** Envoie aux clients les informations de configuration des monstres pour le bestiaire et les barres de vie. */
public final class BestiaryInfo {
    private BestiaryInfo() {
    }

    public static CompoundTag build() {
        CompoundTag root = new CompoundTag();
        for (Map.Entry<String, MobConfig> e : ConfigManager.mobs().entrySet()) {
            MobConfig c = e.getValue();
            CompoundTag t = new CompoundTag();
            t.putString("name", c.displayName);
            t.putString("family", family(e.getKey(), c));
            t.putInt("min", c.minLevel);
            t.putInt("max", c.maxLevel);
            t.putFloat("hp", (float) c.healthAt(c.minLevel));
            t.putFloat("hpMax", (float) c.healthAt(c.maxLevel));
            t.putFloat("atk", (float) c.attackAt(c.minLevel));
            t.putFloat("atkMax", (float) c.attackAt(c.maxLevel));
            t.putBoolean("boss", c.boss);
            t.putBoolean("spawns", c.spawn.enabled && !c.boss);
            ListTag drops = new ListTag();
            for (MobConfig.Drop d : c.drops) {
                CompoundTag dt = new CompoundTag();
                dt.putString("item", d.item);
                dt.putFloat("chance", (float) d.chance);
                dt.putInt("min", d.min);
                dt.putInt("max", d.max);
                drops.add(dt);
            }
            t.put("drops", drops);
            root.put(e.getKey(), t);
        }
        return root;
    }

    private static String family(String key, MobConfig cfg) {
        if (cfg.family != null && !cfg.family.isBlank()) return cfg.family;
        if (key.contains("gobelin") || key.contains("orc") || key.contains("bandit")) return "Humanoïdes";
        if (key.contains("loup") || key.contains("araignee")) return "Bêtes";
        if (key.contains("squelette") || key.contains("liche") || key.contains("spectre")) return "Morts-vivants";
        if (key.contains("neant")) return "Néant";
        return "Élémentaires";
    }

    public static void sendTo(ServerPlayer player) {
        Net.toPlayer(player, new Payloads.MobInfo(build()));
    }

    public static void sendToAll() {
        Net.toAll(new Payloads.MobInfo(build()));
    }
}
