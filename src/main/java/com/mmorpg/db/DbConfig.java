package com.mmorpg.db;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mmorpg.MMORPG;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Reglages de la base de donnees (config/mmorpg/database.json). Desactivee par defaut : sans base, le mod fonctionne comme avant
 * (tout est enregistre dans le monde). Ce fichier n'est volontairement pas dans le menu d'administration, qui afficherait le mot de passe.
 * Le mot de passe peut aussi venir de la variable d'environnement AELORIA_DB_PASSWORD (prioritaire), pour ne pas l'ecrire sur le disque.
 */
public final class DbConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();

    public String _aide = "enabled=true active la base. serverId : nom unique de CE serveur dans le reseau (serveur, hub, mines, plaines). "
            + "Le mot de passe peut etre donne par la variable d'environnement AELORIA_DB_PASSWORD. Voir GUIDE_SERVEUR.md.";
    public boolean enabled = false;
    public String host = "127.0.0.1";
    public int port = 3306;
    public String database = "aeloria";
    public String user = "aeloria";
    public String password = "";
    public String serverId = "serveur";
    /** Intervalle (secondes) de la sauvegarde des joueurs connectes dans la base et du signal de presence. */
    public int autosaveSeconds = 120;
    /** Profils des joueurs (niveau, or, classe, quetes...) et banque : la base fait foi quand un joueur arrive d'un autre serveur. */
    public boolean syncPlayers = true;
    /** Journal des evenements (connexions, morts, niveaux, banque, ventes...). */
    public boolean audit = true;

    public static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("mmorpg").resolve("database.json");
    }

    public static DbConfig load() {
        Path f = file();
        DbConfig c = new DbConfig();
        try {
            if (Files.exists(f)) {
                DbConfig read = GSON.fromJson(Files.readString(f, StandardCharsets.UTF_8), DbConfig.class);
                if (read != null) c = read;
            } else {
                Files.createDirectories(f.getParent());
                Files.writeString(f, GSON.toJson(c), StandardCharsets.UTF_8);
                MMORPG.LOGGER.info("[MMORPG] Base de donnees : fichier {} cree (desactive)", f);
            }
        } catch (IOException | RuntimeException e) {
            MMORPG.LOGGER.error("[MMORPG] Base de donnees : database.json illisible, base desactivee", e);
            c = new DbConfig();
        }
        String env = System.getenv("AELORIA_DB_PASSWORD");
        if (env != null && !env.isEmpty()) c.password = env;
        if (c.serverId == null || c.serverId.isBlank()) c.serverId = "serveur";
        c.serverId = c.serverId.length() > 32 ? c.serverId.substring(0, 32) : c.serverId;
        c.autosaveSeconds = Math.max(10, c.autosaveSeconds);
        return c;
    }

    public String url() {
        return "jdbc:mariadb://" + host + ":" + port + "/" + database;
    }
}
