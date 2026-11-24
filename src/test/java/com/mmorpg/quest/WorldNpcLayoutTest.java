package com.mmorpg.quest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Les PNJ de quetes du monde ouvert sont dans la carte 800x800, espaces d'au moins 16 blocs, et portent chacun une ou plusieurs des 200 quetes (une seule fois). */
class WorldNpcLayoutTest {
    @Test
    void openWorldNpcsInsideMapAndSpaced() {
        List<int[]> p = new java.util.ArrayList<>();
        var quests = new java.util.HashSet<String>();
        for (var e : WorldNpcData.entries()) {
            if (!e.dim().equals("openworld")) continue;
            int[] a = WorldNpcData.anchor(e.region());
            int x = a[0] + e.dx(), z = a[1] + e.dz();
            assertTrue(Math.abs(x) <= 390 && Math.abs(z) <= 390, e.id() + " hors carte : " + x + "," + z);
            for (int[] o : p) assertTrue(Math.hypot(o[0] - x, o[1] - z) >= 16, e.id() + " trop proche d'un autre PNJ");
            p.add(new int[]{x, z});
            for (String q : e.quests()) assertTrue(quests.add(q), "quete en double " + q);
        }
        assertTrue(p.size() >= 60 && p.size() <= 80, "PNJ : " + p.size());
        assertEquals(200, quests.size());
    }
}
