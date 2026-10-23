package com.mmorpg.hub;

/**
 * Emblème d'Aeloria (cristal violet, « A » et croissants d'or) en mosaïque de {@value #SIZE} x {@value #SIZE} blocs, tracé au sol de la grande nef
 * du hub. Genere depuis aeloria_logo.png : k = fond, G = or, H = reflet d'or, l = violet clair, V = violet, v = violet sombre, W = reflet blanc.
 */
final class HubEmblem {
    private HubEmblem() {
    }

    static final int SIZE = 39;
    static final String[] ROWS = {
            "kkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkk",
            "kkkkkkkkkkkkkkkkkkkVVkkkkkkkkkkkkkkkkkk",
            "kkkkkkkkkkkkkkkkkkVllvkkkkkkkkkkkkkkkkk",
            "kkkkkkkkkkkkkkkkkkllWVvkkkkkkkkkkkkkkkk",
            "kkkkkkkkkkkkkkkkklVVllVkkkkkkkkkkkkkkkk",
            "kkkkkkkkkkkkkGGkVlvVlllvkGGkkkkkkkkkkkk",
            "kkkkkkkkkkkGGGkklVvVllllkkGGGkkkkkkkkkk",
            "kkkkkkkkkGGGkkklVVvVllllVkkGHGkkkkkkkkk",
            "kkkkkkkkVHGkkkVlVVVVlllllkkkkHGkkkkkkkk",
            "kkkkkkkVWGkkkVllVVvHVlllllkkkkHVkkkkkkk",
            "kkkkkkkHGkkkkVlllVVHVVlVllVkkkGVGkkkkkk",
            "kkkkkkGGkkkkVvVlVvGGGvVvlllkkkkGHkkkkkk",
            "kkkkkkGkkkkkVvllvGHHHVvvVvVVkkkkHGkkkkk",
            "kkkkkGGkkkklVvVVVVVHVVVvVvVVkkkkGGkkkkk",
            "kkkkkGkkkkVllVvvGWWVWWVvvvVVlkkkkGGkkkk",
            "kkkkGGkkkkVllVvVHHGkGHHvvVllVkkkkGGkkkk",
            "kkkkGGkkkkVVVVkGGGkkVGHGvVllVkkkkGGkkkk",
            "kkkkGkkkkkVVVkGGGGkVkGGGvVllVkkkkkGkkkk",
            "kkkkGkkkkkVllvGGGkvHvkGGGVllVkkkkkGkkkk",
            "kkkkGkkkkkVllVGGkkVHVkVGGvVlvkkkkkGkkkk",
            "kkkkGkkkkkvlVGGGkvGGGvkGGGvVkkkkkkGkkkk",
            "kkkkGkkkkkvlVGGGVvVGVvVHGGVVkkkkkkGkkkk",
            "kkkkGGkkkkvlHGVVvvVGVvvVVGHVkkkkkGGkkkk",
            "kkkkGGkkkkkWHVVvvVVVlVVVVVVVkkkkkGGkkkk",
            "kkkkGHkkkkVHVvvvvVlllVlVvvVHkkkkkHGkkkk",
            "kkkkkGGkkkGGVvvVVllWllllVVvGGkkkGGkkkkk",
            "kkkkkGGkkGGkVVVVlllWllllllkkGkkkGGkkkkk",
            "kkkkkkGGkkkkkllvlllWlllVlVkkkkkGGkkkkkk",
            "kkkkkkkGGkkkkVlvlllWllVVlvkkkkGGkkkkkkk",
            "kkkkkkkGGGVkkkVvVllWlVvVVkkkVGGkkkkkkkk",
            "kkkkkkkkVVGVkkVVVVlWlVVVkkkGGVkkkkkkkkk",
            "kkkkkkkkkkGHGkkVVvVllVVVkkGHGkkkkkkkkkk",
            "kkkkkkkkkGGGGkkvlVVlllVkkkGGGGkkkkkkkkk",
            "kkkkkkkkkkGGkkkkVlVlllvkkkkGGkkkkkkkkkk",
            "kkkkkkkkkkGGkkkkkllllVkkkkkGkkkkkkkkkkk",
            "kkkkkkkkkkkkkkkkkVlllvkkkkkkkkkkkkkkkkk",
            "kkkkkkkkkkkkkkkkkkVlVkkkkkkkkkkkkkkkkkk",
            "kkkkkkkkkkkkkkkkkkvVvkkkkkkkkkkkkkkkkkk",
            "kkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkkk"
    };

    /** Lettre de la case (x, z) de la mosaique, 0..SIZE-1 ; 'k' hors de l'image. */
    static char at(int x, int z) {
        if (x < 0 || z < 0 || x >= SIZE || z >= SIZE) return 'k';
        return ROWS[z].charAt(x);
    }
}
