package com.mmorpg.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Options d'affichage cote client (modifiables depuis Mods > Aeloria MMORPG > Configurer). */
public final class ClientConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue SHOW_DAMAGE_NUMBERS = BUILDER
            .comment("Affiche les dégâts flottants au-dessus des cibles")
            .translation("mmorpg.configuration.showDamageNumbers")
            .define("showDamageNumbers", true);
    public static final ModConfigSpec.BooleanValue SHOW_NAMEPLATES = BUILDER
            .comment("Affiche le niveau et la barre de vie au-dessus des monstres")
            .translation("mmorpg.configuration.showNameplates")
            .define("showNameplates", true);
    public static final ModConfigSpec.BooleanValue SHOW_OWN_COSMETICS = BUILDER
            .comment("Affiche vos propres auras/ailes/halos en vue à la première personne")
            .translation("mmorpg.configuration.showOwnCosmetics")
            .define("showOwnCosmetics", false);

    public static final ModConfigSpec.IntValue AMBIENT_MUSIC_VOLUME = BUILDER
            .comment("Volume de la musique d'ambiance du hub et des donjons, en pourcentage (0 = coupée)")
            .translation("mmorpg.configuration.ambientMusicVolume")
            .defineInRange("ambientMusicVolume", 70, 0, 100);

    public static final ModConfigSpec.ConfigValue<String> MAIN_MENU_SERVER = BUILDER
            .comment("Adresse de connexion du bouton Jouer ; le launcher peut fournir l'adresse via aeloria.server.")
            .translation("mmorpg.configuration.mainMenuServer")
            .define("mainMenuServer", "");

    public static final ModConfigSpec SPEC = BUILDER.build();

    /** Volume de la musique d'ambiance entre 0 et 1. */
    public static float ambientMusic() {
        return AMBIENT_MUSIC_VOLUME.get() / 100F;
    }

    private ClientConfig() {
    }
}
