package com.mmorpg.guild;

import net.minecraft.world.item.DyeColor;
import java.util.List;

/** Blason de guilde : couleurs et emblèmes, partagés par le hall (blocs) et l'aperçu de l'interface. */
public final class GuildCrest {
    public static final List<String> COLORS = List.of("VIOLET","AZUR","EMERAUDE","CARMIN","OR","ROSE","ORANGE","TURQUOISE","ARGENT","NOIR","BLANC","BRUN");
    public static final List<String> PATTERNS = List.of("CRISTAL","SOLEIL","LUNE","ARBRE","DRAGON","ETOILE","COURONNE","EPEE","CROIX","FLAMME");
    private GuildCrest() {}

    public static DyeColor dye(String key) {
        return switch (key == null ? "" : key) {
            case "AZUR" -> DyeColor.LIGHT_BLUE; case "EMERAUDE" -> DyeColor.GREEN; case "CARMIN" -> DyeColor.RED; case "OR" -> DyeColor.YELLOW;
            case "ROSE" -> DyeColor.PINK; case "ORANGE" -> DyeColor.ORANGE; case "TURQUOISE" -> DyeColor.CYAN; case "ARGENT" -> DyeColor.LIGHT_GRAY;
            case "NOIR" -> DyeColor.BLACK; case "BLANC" -> DyeColor.WHITE; case "BRUN" -> DyeColor.BROWN; default -> DyeColor.PURPLE;
        };
    }

    /** Couleur d'affichage ARGB (interface). */
    public static int argb(String key) {
        return 0xFF000000 | switch (key == null ? "" : key) {
            case "AZUR" -> 0x4FA8E8; case "EMERAUDE" -> 0x3FB868; case "CARMIN" -> 0xC8363E; case "OR" -> 0xE6B83A; case "ROSE" -> 0xE87FB0;
            case "ORANGE" -> 0xE8802C; case "TURQUOISE" -> 0x2AB5B5; case "ARGENT" -> 0xB8BCC8; case "NOIR" -> 0x24242C; case "BLANC" -> 0xF2F2F2;
            case "BRUN" -> 0x7A5230; default -> 0x8A55D8;
        };
    }

    /** Forme de l'emblème sur une grille x ∈ [-3,3], y ∈ [0,4] (y = 0 en bas). */
    public static boolean shape(String pattern, int x, int y) {
        return switch (pattern == null ? "" : pattern) {
            case "SOLEIL" -> x * x + (y - 2) * (y - 2) <= 5;
            case "LUNE" -> x * x + (y - 2) * (y - 2) <= 6 && x <= 0;
            case "ARBRE" -> y < 2 ? x == 0 : Math.abs(x) <= 4 - y;
            case "DRAGON" -> Math.abs(x) == y / 2 || y == 1;
            case "ETOILE" -> x == 0 || y == 2 || (Math.abs(x) == Math.abs(y - 2) && Math.abs(x) <= 2);
            case "COURONNE" -> y == 0 ? Math.abs(x) <= 3 : y <= (x == 0 ? 4 : 3) && Math.floorMod(x, 2) == 0;
            case "EPEE" -> x == 0 || (y == 1 && Math.abs(x) <= 2);
            case "CROIX" -> Math.abs(x) <= 2 && (x == 0 || y == 2);
            case "FLAMME" -> Math.abs(x) <= (y < 2 ? 2 : (4 - y) / 2);
            default -> Math.abs(x) + Math.abs(y - 2) <= 2;
        };
    }
}
