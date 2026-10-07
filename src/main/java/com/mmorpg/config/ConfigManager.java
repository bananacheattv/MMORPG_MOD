package com.mmorpg.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.mmorpg.MMORPG;
import net.neoforged.fml.loading.FMLPaths;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Chargement / sauvegarde des fichiers de configuration JSON :
 * <ul>
 *     <li>config/mmorpg/general.json : reglages generaux</li>
 *     <li>config/mmorpg/mobs.json : statistiques, apparitions, butin et capacites de chaque monstre et boss</li>
 * </ul>
 * Les entrees manquantes sont completees automatiquement avec les valeurs par defaut.
 */
public final class ConfigManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static GeneralConfig general = new GeneralConfig();
    private static Map<String, MobConfig> mobs = DefaultMobs.create();
    private static Map<String, com.mmorpg.quest.QuestDef> quests = com.mmorpg.quest.DefaultQuests.create();
    private static ShopConfig shop = ShopConfig.defaults();

    private ConfigManager() {
    }

    /** Ajoute au butin d'un monstre deja configure les nouveaux objets du butin par defaut (sans toucher aux reglages existants). */
    private static void mergeDrops(MobConfig cfg, MobConfig defaults) {
        for (MobConfig.Drop d : cfg.drops) {
            if (d.item.equals("mmorpg:hache_berserker")) d.item = "mmorpg:epee_berserker";     // la hache est devenue une epee
        }
        for (MobConfig.Drop d : defaults.drops) {
            if (cfg.drops.stream().noneMatch(x -> x.item.equals(d.item))) cfg.drops.add(new MobConfig.Drop(d.item, d.chance, d.min, d.max));
        }
        cfg.dropsVersion = DefaultMobs.DROPS_VERSION;
    }

    public static GeneralConfig general() {
        return general;
    }

    public static Map<String, MobConfig> mobs() {
        return mobs;
    }

    public static Map<String, com.mmorpg.quest.QuestDef> quests() {
        return quests;
    }

    /** Write the entire replacement first; preserve the current file and map if writing fails. */
    public static synchronized void saveQuest(String id, com.mmorpg.quest.QuestDef quest) throws java.io.IOException {
        var updated = new LinkedHashMap<>(quests);
        updated.put(id, quest);
        Files.createDirectories(dir());
        Path temp = Files.createTempFile(dir(), "quests-", ".tmp");
        try {
            Files.writeString(temp, GSON.toJson(updated), StandardCharsets.UTF_8);
            try { Files.move(temp, dir().resolve("quests.json"), java.nio.file.StandardCopyOption.REPLACE_EXISTING, java.nio.file.StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException e) { Files.move(temp, dir().resolve("quests.json"), java.nio.file.StandardCopyOption.REPLACE_EXISTING); }
            quests = updated;
        } finally { Files.deleteIfExists(temp); }
    }

    public static ShopConfig shop() {
        return shop;
    }

    public static String shopJson() {
        return GSON.toJson(shop);
    }

    public static ShopConfig parseShop(String json) {
        try {
            ShopConfig c = GSON.fromJson(json, ShopConfig.class);
            return c == null ? ShopConfig.defaults() : c;
        } catch (Exception e) {
            return ShopConfig.defaults();
        }
    }

    public static MobConfig mob(String key) {
        MobConfig cfg = mobs.get(key);
        if (cfg == null) {
            cfg = DefaultMobs.create().get(key);
        }
        return cfg;
    }

    public static Path dir() {
        return FMLPaths.CONFIGDIR.get().resolve(MMORPG.MODID);
    }

    /** Met a jour les anciens fichiers de configuration. */
    private static void migrate(GeneralConfig c) {
        if (c.configVersion < 2) {
            // v2 : les competences touchent les joueurs (l'ancienne valeur "false" etait celle par defaut)
            c.skillsHitPlayers = true;
        }
        c.configVersion = GeneralConfig.CURRENT_VERSION;
    }

    public static synchronized void load() {
        try {
            Files.createDirectories(dir());
            Path g = dir().resolve("general.json");
            if (Files.exists(g)) {
                try (Reader r = Files.newBufferedReader(g, StandardCharsets.UTF_8)) {
                    GeneralConfig loaded = GSON.fromJson(r, GeneralConfig.class);
                    if (loaded != null) general = loaded;
                }
            } else {
                general = new GeneralConfig();
            }
            migrate(general);
            write(g, general);

            Path m = dir().resolve("mobs.json");
            Map<String, MobConfig> defaults = DefaultMobs.create();
            Map<String, MobConfig> result = new LinkedHashMap<>();
            if (Files.exists(m)) {
                Type type = new TypeToken<LinkedHashMap<String, MobConfig>>() {
                }.getType();
                try (Reader r = Files.newBufferedReader(m, StandardCharsets.UTF_8)) {
                    Map<String, MobConfig> loaded = GSON.fromJson(r, type);
                    if (loaded != null) result.putAll(loaded);
                }
            }
            for (Map.Entry<String, MobConfig> e : defaults.entrySet()) {
                MobConfig existing = result.putIfAbsent(e.getKey(), e.getValue());
                if (existing != null && existing.dropsVersion < DefaultMobs.DROPS_VERSION) mergeDrops(existing, e.getValue());
            }
            for (MobConfig cfg : result.values()) {
                sanitize(cfg);
            }
            mobs = result;
            write(m, mobs);

            Path q = dir().resolve("quests.json");
            Map<String, com.mmorpg.quest.QuestDef> qres = new LinkedHashMap<>();
            if (Files.exists(q)) {
                Type qtype = new TypeToken<LinkedHashMap<String, com.mmorpg.quest.QuestDef>>() {
                }.getType();
                try (Reader r = Files.newBufferedReader(q, StandardCharsets.UTF_8)) {
                    Map<String, com.mmorpg.quest.QuestDef> loaded = GSON.fromJson(r, qtype);
                    if (loaded != null) qres.putAll(loaded);
                }
            } else {
                qres.putAll(com.mmorpg.quest.DefaultQuests.create());
            }
            com.mmorpg.quest.DefaultQuests.create().forEach(qres::putIfAbsent);
            for (com.mmorpg.quest.QuestDef d : qres.values()) {
                if (d.objectives == null) d.objectives = new java.util.ArrayList<>();
                if (d.requires == null) d.requires = new java.util.ArrayList<>();
                if (d.rewards == null) d.rewards = new com.mmorpg.quest.QuestDef.Rewards();
                if (d.rewards.items == null) d.rewards.items = new java.util.ArrayList<>();
                if (d.giver == null) d.giver = "";
                if (d.npc == null) d.npc = "";
            }
            quests = qres;
            write(q, quests);

            Path sh = dir().resolve("shop.json");
            ShopConfig shopCfg = null;
            if (Files.exists(sh)) {
                try (Reader r = Files.newBufferedReader(sh, StandardCharsets.UTF_8)) {
                    shopCfg = GSON.fromJson(r, ShopConfig.class);
                }
            }
            shop = shopCfg == null ? ShopConfig.defaults() : shopCfg;
            write(sh, shop);
            MMORPG.LOGGER.info("[MMORPG] Configuration chargée : {} monstres/boss, {} quêtes, {} articles en boutique", mobs.size(), quests.size(), shop.buy.size());
        } catch (Exception e) {
            MMORPG.LOGGER.error("[MMORPG] Impossible de lire la configuration, valeurs par défaut utilisées", e);
            general = new GeneralConfig();
            mobs = DefaultMobs.create();
            quests = com.mmorpg.quest.DefaultQuests.create();
            shop = ShopConfig.defaults();
        }
    }

    private static void sanitize(MobConfig cfg) {
        if (cfg.spawn == null) cfg.spawn = new MobConfig.Spawn();
        if (cfg.drops == null) cfg.drops = new java.util.ArrayList<>();
        if (cfg.abilities == null) cfg.abilities = new LinkedHashMap<>();
        if (cfg.spawn.biomes == null) cfg.spawn.biomes = new java.util.ArrayList<>();
        if (cfg.spawn.dimensions == null) cfg.spawn.dimensions = new java.util.ArrayList<>(List.of("minecraft:overworld"));
        if (cfg.displayName == null) cfg.displayName = "";
        cfg.minLevel = Math.max(1, Math.min(100, cfg.minLevel));
        cfg.maxLevel = Math.max(cfg.minLevel, Math.min(100, cfg.maxLevel));
        cfg.scale = Math.max(0.2, Math.min(6, cfg.scale));
    }

    private static void write(Path p, Object o) throws Exception {
        try (Writer w = Files.newBufferedWriter(p, StandardCharsets.UTF_8)) {
            GSON.toJson(o, w);
        }
    }
}
