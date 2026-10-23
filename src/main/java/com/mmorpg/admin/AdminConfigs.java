package com.mmorpg.admin;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import com.mmorpg.MMORPG;
import com.mmorpg.config.ConfigManager;
import com.mmorpg.config.DummyConfig;
import com.mmorpg.config.GeneralConfig;
import com.mmorpg.config.LootTables;
import com.mmorpg.config.MobConfig;
import com.mmorpg.config.ShopConfig;
import com.mmorpg.hub.CinematicConfig;
import com.mmorpg.quest.QuestDef;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Fichiers de configuration modifiables depuis le menu d'administration : lecture, validation (le texte doit correspondre a la
 * structure attendue par le mod), sauvegarde de l'ancienne version (config/mmorpg/sauvegardes, 20 par fichier) puis rechargement.
 */
public final class AdminConfigs {
    private AdminConfigs() {
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    public static final long MAX_BYTES = 3L * 1024 * 1024;

    /** Nom affiche -> fichier. */
    public static final List<String> FILES = List.of("general", "mobs", "quests", "shop", "butins", "mannequin", "cinematique");

    private static Path path(String name) {
        return ConfigManager.dir().resolve(name + ".json");
    }

    public static boolean known(String name) {
        return FILES.contains(name);
    }

    public static String read(String name) throws IOException {
        if (!known(name)) throw new IOException("fichier inconnu");
        if (!Files.exists(path(name))) ConfigManager.load();
        if (name.equals("cinematique") && !Files.exists(path(name))) CinematicConfig.load();
        return Files.readString(path(name), StandardCharsets.UTF_8);
    }

    private static Type mapOf(Class<?> value) {
        return TypeToken.getParameterized(LinkedHashMap.class, String.class, value).getType();
    }

    /** Verifie que le texte est un JSON valide de la bonne structure ; renvoie null s'il est correct, sinon la raison. */
    public static String validate(String name, String text) {
        try {
            JsonElement tree = JsonParser.parseString(text);
            if (tree == null || tree.isJsonNull()) return "fichier vide";
            switch (name) {
                case "general" -> require(GSON.fromJson(text, GeneralConfig.class));
                case "mobs" -> {
                    Map<String, MobConfig> m = GSON.fromJson(text, mapOf(MobConfig.class));
                    require(m);
                    for (var e : m.entrySet()) if (e.getValue() == null) return "monstre « " + e.getKey() + " » vide";
                }
                case "quests" -> {
                    Map<String, QuestDef> m = GSON.fromJson(text, mapOf(QuestDef.class));
                    require(m);
                    for (var e : m.entrySet()) if (e.getValue() == null) return "quête « " + e.getKey() + " » vide";
                }
                case "shop" -> require(GSON.fromJson(text, ShopConfig.class));
                case "butins" -> require(GSON.fromJson(text, LootTables.class));
                case "mannequin" -> require(GSON.fromJson(text, DummyConfig.class));
                case "cinematique" -> {
                    String problem = CinematicConfig.validate(text);
                    if (problem != null) return problem;
                }
                default -> {
                    return "fichier inconnu";
                }
            }
            return null;
        } catch (Exception e) {
            return "structure invalide : " + readable(e);
        }
    }

    /** Message d'erreur de Gson sans nom de classe ni lien, lisible dans le menu. */
    private static String readable(Exception e) {
        String m = String.valueOf(e.getMessage());
        m = m.replaceFirst("^[a-zA-Z.]+Exception: ", "");
        m = m.replaceAll("\\s*See https?://\\S+", "").strip();
        return m.replace("Expected ", "attendu ").replace(" but was ", " mais trouvé ").replace("BEGIN_ARRAY", "une liste").replace("BEGIN_OBJECT", "un objet")
                .replace("STRING", "du texte").replace("NUMBER", "un nombre").replace("BOOLEAN", "oui/non")
                .replace(" at line ", " (ligne ").replace(" column ", ", colonne ").replace(" path ", ", chemin ") + (m.contains(" at line ") ? ")" : "");
    }

    private static void require(Object o) {
        if (o == null) throw new IllegalArgumentException("fichier vide");
    }

    /** Ecrit le fichier (apres validation), garde l'ancienne version et recharge la configuration. @return null si tout va bien, sinon l'erreur */
    public static synchronized String save(String name, String text) {
        if (!known(name)) return "fichier inconnu";
        if (text.getBytes(StandardCharsets.UTF_8).length > MAX_BYTES) return "fichier trop volumineux";
        String problem = validate(name, text);
        if (problem != null) return problem;
        try {
            Path target = path(name);
            Path backups = ConfigManager.dir().resolve("sauvegardes");
            Files.createDirectories(backups);
            if (Files.exists(target)) {
                String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
                Files.copy(target, backups.resolve(name + "-" + stamp + ".json"), StandardCopyOption.REPLACE_EXISTING);
                prune(backups, name);
            }
            JsonElement pretty = JsonParser.parseString(text);
            Path temp = Files.createTempFile(ConfigManager.dir(), name + "-", ".tmp");
            try {
                Files.writeString(temp, GSON.toJson(pretty), StandardCharsets.UTF_8);
                try {
                    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                    Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temp);
            }
            ConfigManager.load();
            com.mmorpg.server.BestiaryInfo.sendToAll();
            return null;
        } catch (Exception e) {
            MMORPG.LOGGER.error("[MMORPG] ADMIN : ecriture de {}.json impossible", name, e);
            return "écriture impossible (" + e.getMessage() + ")";
        }
    }

    private static void prune(Path dir, String name) throws IOException {
        try (var files = Files.list(dir)) {
            List<Path> mine = files.filter(p -> p.getFileName().toString().startsWith(name + "-") && p.toString().endsWith(".json")).sorted().toList();
            for (int i = 0; i < mine.size() - 20; i++) Files.deleteIfExists(mine.get(i));
        }
    }
}
