package com.mmorpg.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Options d'affichage cote client (modifiables depuis Mods > Eldoria MMORPG > Configurer). */
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

    public static final ModConfigSpec SPEC = BUILDER.build();

    private ClientConfig() {
    }
}
