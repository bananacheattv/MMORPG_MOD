package com.mmorpg.quest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Les 200 PNJ de quetes du monde ouvert sont dans la carte 800x800 et espaces d'au moins 12 blocs. */
class WorldNpcLayoutTest {
    @Test
    void openWorldNpcsInsideMapAndSpaced() {
        List<int[]> p = new java.util.ArrayList<>();
        for (var e : WorldNpcData.entries()) {
            if (!e.dim().equals("openworld")) continue;
            int[] a = WorldNpcData.anchor(e.region());
            int x = a[0] + e.dx(), z = a[1] + e.dz();
            assertTrue(Math.abs(x) <= 390 && Math.abs(z) <= 390, e.id() + " hors carte : " + x + "," + z);
            for (int[] o : p) assertTrue(Math.hypot(o[0] - x, o[1] - z) >= 12, e.id() + " trop proche d'un autre PNJ");
            p.add(new int[]{x, z});
        }
        assertEquals(200, p.size());
    }
}
