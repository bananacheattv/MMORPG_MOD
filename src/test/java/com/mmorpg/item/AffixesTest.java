package com.mmorpg.item;

import com.mmorpg.rpg.Stat;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AffixesTest {
    @Test
    void tiersSpanFromGreenToPink() {
        assertEquals(0, Affixes.tier(Stat.ATK, 2 + 50 * 0.4 * 0.7, 50));
        assertEquals(2, Affixes.tier(Stat.ATK, 2 + 50 * 0.4, 50));
        assertEquals(4, Affixes.tier(Stat.ATK, (2 + 50 * 0.4) * 1.3, 50));
    }
}
