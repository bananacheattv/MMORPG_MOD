package com.mmorpg.client;

import com.mmorpg.MMORPG;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Ecran de chargement de NeoForge aux couleurs d'Aeloria. NeoForge lit son theme dans config/fml/theme-default.json avant de charger les
 * mods : le mod depose donc ses fichiers (couleurs, logo, barres) des son premier lancement, et ils servent des le suivant. Le launcher
 * d'Aeloria les depose aussi avant de lancer le jeu. Un theme personnalise sans la marque « _aeloria » n'est jamais remplace.
 */
public final class EarlyTheme {
    private EarlyTheme() {
    }

    private static final String[] FILES = {"theme-default.json", "theme-darkmode.json", "aeloria_logo.png", "aeloria_emblem.png", "aeloria_icon.png",
            "aeloria_bar_bg.png", "aeloria_bar_fg.png"};

    public static void install() {
        try {
            Path dir = FMLPaths.CONFIGDIR.get().resolve("fml");
            Path theme = dir.resolve("theme-default.json");
            if (Files.exists(theme) && !Files.readString(theme, StandardCharsets.UTF_8).contains("\"_aeloria\"")) return;
            Files.createDirectories(dir);
            for (String f : FILES) {
                try (InputStream in = EarlyTheme.class.getResourceAsStream("/earlytheme/" + f)) {
                    if (in == null) continue;
                    byte[] bytes = in.readAllBytes();
                    Path dest = dir.resolve(f);
                    if (!Files.exists(dest) || !java.util.Arrays.equals(Files.readAllBytes(dest), bytes)) Files.write(dest, bytes);
                }
            }
        } catch (IOException e) {
            MMORPG.LOGGER.warn("[MMORPG] Theme de chargement d'Aeloria non installe", e);
        }
    }
}
