package com.mmorpg.fishing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.mmorpg.rpg.Rarity;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class FishingTest {
    private static FishingRules.Context ctx(int level, int rod, FishCatalog.Habitat h, boolean night, boolean rain) {
        return new FishingRules.Context(level, rod, h, night, rain);
    }

    // ------------------------------------------------------------------ catalogue

    @Test
    void catalogIdsAreUniqueAndComplete() {
        Set<String> ids = new HashSet<>();
        for (FishCatalog.Entry e : FishCatalog.ALL) {
            assertTrue(ids.add(e.id()), "doublon " + e.id());
            assertFalse(e.habitats().isEmpty(), e.id());
            assertTrue(e.weight() > 0 && e.xp() > 0, e.id());
            if (e.fish()) assertTrue(e.minCm() > 0 && e.maxCm() > e.minCm(), e.id());
        }
        for (FishCatalog.Ingredient i : FishCatalog.INGREDIENTS) assertTrue(ids.add(i.id()), "doublon " + i.id());
        for (FishCatalog.Rod r : FishCatalog.RODS) assertTrue(ids.add(r.id()), "doublon " + r.id());
        assertEquals(6, FishCatalog.RODS.size());
        assertEquals(4, FishCatalog.of(FishCatalog.Kind.TRESOR).size());
        assertTrue(FishCatalog.of(FishCatalog.Kind.POISSON).size() >= 25);
        // les six rarete ont des poissons, du commun au mythique
        for (Rarity r : Rarity.values()) {
            assertTrue(FishCatalog.of(FishCatalog.Kind.POISSON).stream().anyMatch(e -> e.rarity() == r), "aucun poisson " + r);
        }
    }

    @Test
    void rodsProgressWithTheJobTiers() {
        int prevLevel = 0, prevDur = 0, prevLuck = -1;
        double prevZone = 0;
        for (FishCatalog.Rod r : FishCatalog.RODS) {
            assertTrue(r.minJobLevel() >= prevLevel, r.id());
            assertTrue(r.durability() > prevDur && r.luck() >= prevLuck && r.zone() > prevZone, r.id());
            prevLevel = r.minJobLevel();
            prevDur = r.durability();
            prevLuck = r.luck();
            prevZone = r.zone();
        }
        assertEquals(1, FishCatalog.rod(1).minJobLevel());
        assertEquals(50, FishCatalog.rod(6).minJobLevel());
        assertNull(FishCatalog.rod(0));
    }

    // ------------------------------------------------------------------ butin

    @Test
    void everyHabitatAlwaysHasCatchesForABeginner() {
        for (FishCatalog.Habitat h : FishCatalog.Habitat.values()) {
            for (boolean night : new boolean[]{false, true}) {
                for (boolean rain : new boolean[]{false, true}) {
                    assertFalse(FishingRules.fishPool(ctx(1, 0, h, night, rain), Rarity.COMMUN).isEmpty(), h + " " + night + " " + rain);
                }
            }
        }
    }

    @Test
    void everyFishCanBeCaughtSomewhere() {
        Set<String> reachable = new HashSet<>();
        for (FishCatalog.Habitat h : FishCatalog.Habitat.values()) {
            for (boolean night : new boolean[]{false, true}) {
                for (boolean rain : new boolean[]{false, true}) {
                    for (FishCatalog.Entry e : FishingRules.fishPool(ctx(50, 6, h, night, rain), Rarity.COMMUN)) reachable.add(e.id());
                }
            }
        }
        for (FishCatalog.Entry e : FishCatalog.of(FishCatalog.Kind.POISSON)) assertTrue(reachable.contains(e.id()), "inatteignable : " + e.id());
    }

    @Test
    void rarityGatesFollowRodAndLevel() {
        // un debutant avec la canne vanilla ne remonte ni rare ni mieux
        for (FishCatalog.Habitat h : FishCatalog.Habitat.values()) {
            for (FishCatalog.Entry e : FishingRules.fishPool(ctx(50, 0, h, true, true), Rarity.COMMUN)) {
                assertTrue(e.rarity().ordinal() <= Rarity.PEU_COMMUN.ordinal(), e.id());
            }
            for (FishCatalog.Entry e : FishingRules.fishPool(ctx(1, 6, h, true, true), Rarity.COMMUN)) assertTrue(e.minLevel() <= 1, e.id());
        }
        // la carpe du Neant n'apparait qu'a la canne de rang 5 et au niveau 50, de nuit
        assertTrue(FishingRules.fishPool(ctx(50, 5, FishCatalog.Habitat.LAC, true, false), Rarity.COMMUN).stream().anyMatch(e -> e.id().equals("carpe_du_neant")));
        assertFalse(FishingRules.fishPool(ctx(50, 4, FishCatalog.Habitat.LAC, true, false), Rarity.COMMUN).stream().anyMatch(e -> e.id().equals("carpe_du_neant")));
        assertFalse(FishingRules.fishPool(ctx(50, 5, FishCatalog.Habitat.LAC, false, false), Rarity.COMMUN).stream().anyMatch(e -> e.id().equals("carpe_du_neant")));
    }

    @Test
    void rollsFollowTheExpectedShares() {
        Random rnd = new Random(42);
        int n = 40000, junk = 0, chests = 0, rare = 0;
        FishingRules.Context beginner = ctx(1, 1, FishCatalog.Habitat.LAC, false, false);
        for (int i = 0; i < n; i++) {
            FishCatalog.Entry e = FishingRules.roll(beginner, rnd);
            assertNotNull(e);
            if (e.kind() == FishCatalog.Kind.DECHET) junk++;
            if (e.kind() == FishCatalog.Kind.TRESOR) chests++;
            if (e.rarity().ordinal() >= Rarity.RARE.ordinal() && e.fish()) rare++;
        }
        assertEquals(0, rare);
        assertEquals(FishingRules.junkChance(1, 1), junk / (double) n, 0.02);
        assertEquals(FishingRules.treasureChance(1, 1, false), chests / (double) n, 0.01);
        // un maitre pecheur remonte des poissons rares et moins de dechets
        FishingRules.Context master = ctx(50, 6, FishCatalog.Habitat.ABYSSES, true, true);
        int masterRare = 0, masterJunk = 0;
        for (int i = 0; i < n; i++) {
            FishCatalog.Entry e = FishingRules.roll(master, rnd);
            if (e.fish() && e.rarity().ordinal() >= Rarity.EPIQUE.ordinal()) masterRare++;
            if (e.kind() == FishCatalog.Kind.DECHET) masterJunk++;
        }
        assertTrue(masterRare > n * 0.15, "epiques+ : " + masterRare);
        assertTrue(masterJunk < junk, "dechets : " + masterJunk + " contre " + junk);
    }

    @Test
    void luckShiftsTheCatchTowardRarerFish() {
        Random rnd = new Random(7);
        int plain = 0, lucky = 0, n = 30000;
        for (int i = 0; i < n; i++) {
            if (FishingRules.rollRare(ctx(30, 4, FishCatalog.Habitat.MER, false, false), rnd).rarity().ordinal() >= Rarity.EPIQUE.ordinal()) plain++;
            if (FishingRules.rollRare(ctx(50, 6, FishCatalog.Habitat.MER, false, false), rnd).rarity().ordinal() >= Rarity.EPIQUE.ordinal()) lucky++;
        }
        assertTrue(lucky > plain, lucky + " <= " + plain);
        // la prise rare est toujours au moins « rare » quand l'habitat en offre
        for (int i = 0; i < 2000; i++) {
            assertTrue(FishingRules.rollRare(ctx(30, 4, FishCatalog.Habitat.MER, false, false), rnd).rarity().ordinal() >= Rarity.RARE.ordinal());
        }
    }

    @Test
    void sizesStayInRangeAndPerfectCatchesAreBigger() {
        Random rnd = new Random(3);
        FishCatalog.Entry e = FishCatalog.get("saumon_royal");
        long normal = 0, perfect = 0;
        for (int i = 0; i < 5000; i++) {
            int a = FishingRules.sizeCm(e, false, rnd), b = FishingRules.sizeCm(e, true, rnd);
            assertTrue(a >= e.minCm() && a <= e.maxCm() && b >= e.minCm() && b <= e.maxCm());
            normal += a;
            perfect += b;
        }
        assertTrue(perfect > normal);
        assertEquals(0, FishingRules.sizeCm(FishCatalog.get("botte_rouillee"), false, rnd));
    }

    @Test
    void experienceScalesWithRarityAndTiers() {
        long prev = 0;
        for (Rarity r : Rarity.values()) {
            FishCatalog.Entry e = FishCatalog.of(FishCatalog.Kind.POISSON).stream().filter(x -> x.rarity() == r).findFirst().orElseThrow();
            long xp = FishingRules.xp(e, false, 1);
            assertTrue(xp > prev, r.name());
            prev = xp;
        }
        FishCatalog.Entry e = FishCatalog.get("gardon_argente");
        assertTrue(FishingRules.xp(e, true, 1) > FishingRules.xp(e, false, 1));
        assertTrue(FishingRules.xp(e, false, 50) > FishingRules.xp(e, false, 1));
        assertTrue(FishingRules.jobZoneBonus(50) > FishingRules.jobZoneBonus(9));
    }

    @Test
    void treasureTablesAlwaysGiveSomethingAndGoldGrowsWithTheChest() {
        Random rnd = new Random(5);
        for (int rank = 0; rank < 4; rank++) {
            for (int i = 0; i < 500; i++) {
                assertFalse(TreasureLoot.roll(rank, rnd).isEmpty());
                long g = TreasureLoot.gold(rank, rnd);
                assertTrue(g >= TreasureLoot.GOLD_MIN[rank] && g <= TreasureLoot.GOLD_MAX[rank]);
            }
            if (rank > 0) assertTrue(TreasureLoot.GOLD_MIN[rank] > TreasureLoot.GOLD_MAX[rank - 1] / 2);
        }
    }

    // ------------------------------------------------------------------ mini-jeu

    @Test
    void cursorIsATriangleWaveInsideTheBar() {
        for (int period : new int[]{22, 30, 44}) {
            for (int phase = 0; phase < period; phase += 3) {
                for (long t = 0; t < period * 3L; t++) {
                    double c = FishFight.cursor(t, period, phase);
                    assertTrue(c >= 0 && c <= 1);
                    assertEquals(c, FishFight.cursor(t + period, period, phase), 1e-9);
                }
            }
        }
        assertEquals(0.0, FishFight.cursor(0, 40, 0), 1e-9);
        assertEquals(1.0, FishFight.cursor(20, 40, 0), 1e-9);
    }

    private static long tickAtCenter(FishFight f) {
        // premier tick de la manche ou l'aiguille est la plus proche du centre de la zone
        long best = f.roundStart();
        double bd = 9;
        for (long t = 0; t < f.period; t++) {
            double d = Math.abs(FishFight.cursor(t, f.period, f.currentPhase()) - f.currentCenter());
            if (d < bd) {
                bd = d;
                best = f.roundStart() + t;
            }
        }
        return best;
    }

    private static long tickAwayFromZone(FishFight f) {
        for (long t = 0; t < f.period; t++) {
            double d = Math.abs(FishFight.cursor(t, f.period, f.currentPhase()) - f.currentCenter());
            if (d > f.halfWidth + 0.2) return f.roundStart() + t;
        }
        throw new AssertionError("zone trop large");
    }

    @Test
    void aSinglePerfectClickLandsACommonFish() {
        FishFight f = new FishFight(Rarity.COMMUN, 1, 1.0, 99L, 1000);
        assertNull(f.press(1005), "clic pendant la pause entre manches");
        FishFight.Press p = f.press(tickAtCenter(f));
        assertEquals(FishFight.Press.PERFECT, p);
        assertTrue(f.finished() && f.success() && f.perfect());
    }

    @Test
    void aMissedClickLosesACommonFish() {
        FishFight f = new FishFight(Rarity.COMMUN, 1, 1.0, 99L, 0);
        assertEquals(FishFight.Press.MISS, f.press(tickAwayFromZone(f)));
        assertTrue(f.finished());
        assertFalse(f.success());
    }

    @Test
    void timeoutCountsAsAMissAndLegendariesNeedThreeRounds() {
        FishFight f = new FishFight(Rarity.LEGENDAIRE, 4, 1.0, 5L, 0);
        assertEquals(3, f.rounds);
        assertFalse(f.tick(FishFight.ROUND_GAP + FishFight.ROUND_TIMEOUT - 1));
        assertTrue(f.tick(FishFight.ROUND_GAP + FishFight.ROUND_TIMEOUT));
        assertEquals(1, f.misses());
        assertFalse(f.finished(), "une erreur est permise avec la canne de rang 4");
        assertEquals(FishFight.Press.PERFECT, f.press(tickAtCenter(f)));
        assertEquals(FishFight.Press.PERFECT, f.press(tickAtCenter(f)));
        assertTrue(f.finished() && f.success());
        assertFalse(f.perfect(), "une manche ratee retire le « parfait »");
        // sans canne assez avancee, la premiere erreur perd la prise
        FishFight strict = new FishFight(Rarity.LEGENDAIRE, 2, 1.0, 5L, 0);
        strict.tick(FishFight.ROUND_GAP + FishFight.ROUND_TIMEOUT);
        assertTrue(strict.finished() && !strict.success());
    }

    @Test
    void zonesShrinkAndNeedlesSpeedUpWithRarity() {
        double prevW = 1;
        int prevPeriod = 100;
        int prevRounds = 0;
        for (Rarity r : Rarity.values()) {
            assertTrue(FishFight.baseHalfWidth(r) < prevW);
            assertTrue(FishFight.period(r) < prevPeriod);
            assertTrue(FishFight.rounds(r) >= prevRounds);
            prevW = FishFight.baseHalfWidth(r);
            prevPeriod = FishFight.period(r);
            prevRounds = FishFight.rounds(r);
        }
        assertEquals(0, FishFight.allowedMisses(1, 6));
        assertEquals(0, FishFight.allowedMisses(2, 2));
        assertEquals(1, FishFight.allowedMisses(2, 3));
        assertEquals(2, FishFight.allowedMisses(3, 6));
    }

    @Test
    void centersAndPhasesAreDeterministicFromTheSeed() {
        for (long seed = 0; seed < 50; seed++) {
            for (int round = 0; round < 3; round++) {
                double c = FishFight.center(seed, round);
                assertTrue(c >= 0.2 && c <= 0.8);
                assertEquals(c, FishFight.center(seed, round), 0);
                assertEquals(FishFight.phase(seed, round, 30), FishFight.phase(seed, round, 30));
            }
        }
    }

    @Test
    void latencyCompensationJudgesTheNeedleEarlier() {
        FishFight a = new FishFight(Rarity.EPIQUE, 3, 1.0, 11L, 0);
        // clic exactement sur le centre vu du serveur, mais avec 3 ticks de retard reseau : le serveur remonte le temps
        long t = tickAtCenter(a);
        FishFight b = new FishFight(Rarity.EPIQUE, 3, 1.0, 11L, 0);
        FishFight.Press withLag = b.press(t + 3, 3);
        assertEquals(FishFight.Press.PERFECT, withLag);
        FishFight c = new FishFight(Rarity.EPIQUE, 3, 1.0, 11L, 0);
        assertEquals(FishFight.Press.PERFECT, c.press(t, 0));
    }

    // ------------------------------------------------------------------ habitat

    @Test
    void habitatRules() {
        assertEquals(FishCatalog.Habitat.ABYSSES, FishingRules.habitat(true, true, false, false, false, false, 3));
        assertEquals(FishCatalog.Habitat.ABYSSES, FishingRules.habitat(false, false, false, false, false, false, FishingRules.ABYSS_DEPTH));
        assertEquals(FishCatalog.Habitat.GLACE, FishingRules.habitat(false, true, false, false, false, true, 3));
        assertEquals(FishCatalog.Habitat.MER, FishingRules.habitat(false, true, false, false, false, false, 3));
        assertEquals(FishCatalog.Habitat.RIVIERE, FishingRules.habitat(false, false, true, false, false, false, 2));
        assertEquals(FishCatalog.Habitat.MARAIS, FishingRules.habitat(false, false, false, true, false, false, 2));
        assertEquals(FishCatalog.Habitat.JUNGLE, FishingRules.habitat(false, false, false, false, true, false, 2));
        assertEquals(FishCatalog.Habitat.LAC, FishingRules.habitat(false, false, false, false, false, false, 2));
    }

    // ------------------------------------------------------------------ ressources (modeles, textures, langue)

    private static String resource(String path) throws Exception {
        try (InputStream in = FishingTest.class.getResourceAsStream("/assets/mmorpg/" + path)) {
            assertNotNull(in, "ressource absente : " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static boolean exists(String path) {
        return FishingTest.class.getResource("/assets/mmorpg/" + path) != null;
    }

    private static List<String> allItemIds() {
        List<String> ids = new java.util.ArrayList<>();
        FishCatalog.ALL.forEach(e -> ids.add(e.id()));
        FishCatalog.INGREDIENTS.forEach(i -> ids.add(i.id()));
        FishCatalog.RODS.forEach(r -> {
            ids.add(r.id());
        });
        return ids;
    }

    @Test
    void everyItemHasItsDefinitionModelTextureAndNames() throws Exception {
        String fr = resource("lang/fr_fr.json"), en = resource("lang/en_us.json");
        for (String id : allItemIds()) {
            assertTrue(exists("items/" + id + ".json"), "definition " + id);
            assertTrue(exists("models/item/" + id + ".json"), "modele " + id);
            assertTrue(exists("textures/item/" + id + ".png"), "texture " + id);
            assertTrue(fr.contains("\"item.mmorpg." + id + "\""), "nom fr " + id);
            assertTrue(en.contains("\"item.mmorpg." + id + "\""), "nom en " + id);
        }
        for (FishCatalog.Rod r : FishCatalog.RODS) {
            assertTrue(exists("models/item/" + r.id() + "_lancee.json"), "canne lancee " + r.id());
            assertTrue(exists("textures/item/" + r.id() + "_lancee.png"), "texture lancee " + r.id());
            assertTrue(resource("items/" + r.id() + ".json").contains("fishing_rod/cast"), "etat lance " + r.id());
        }
    }

    @Test
    void modelsAreThreeDimensionalAndInsideTheEngineLimits() throws Exception {
        Pattern num = Pattern.compile("\"(?:from|to)\":\\s*\\[\\s*(-?[0-9.]+),\\s*(-?[0-9.]+),\\s*(-?[0-9.]+)\\s*\\]");
        List<String> models = new java.util.ArrayList<>(allItemIds());
        FishCatalog.RODS.forEach(r -> models.add(r.id() + "_lancee"));
        for (String id : models) {
            String json = resource("models/item/" + id + ".json");
            Matcher m = num.matcher(json);
            int boxes = 0;
            while (m.find()) {
                boxes++;
                for (int g = 1; g <= 3; g++) {
                    double v = Double.parseDouble(m.group(g));
                    assertTrue(v >= -16 && v <= 32, id + " hors limites : " + v);
                }
            }
            assertTrue(boxes >= 10, id + " : modele trop plat (" + boxes / 2 + " boites)");
            assertTrue(json.contains("\"elements\""), id);
            assertTrue(json.contains("mmorpg:item/"), id);
        }
        // les cannes ont au moins deux formes (lancee / rangee) differentes
        for (FishCatalog.Rod r : FishCatalog.RODS) {
            assertFalse(resource("models/item/" + r.id() + ".json").equals(resource("models/item/" + r.id() + "_lancee.json")));
        }
    }

    @Test
    void everyTextureIsUniqueAndNotAPlaceholder() throws Exception {
        Set<Integer> hashes = new HashSet<>();
        for (String id : allItemIds()) {
            try (InputStream in = FishingTest.class.getResourceAsStream("/assets/mmorpg/textures/item/" + id + ".png")) {
                byte[] bytes = in.readAllBytes();
                assertTrue(bytes.length > 150, id + " : texture vide");
                assertTrue(hashes.add(java.util.Arrays.hashCode(bytes)), id + " : texture identique a une autre");
            }
        }
    }

    @Test
    void catalogHabitatsAreAllUsed() {
        EnumSet<FishCatalog.Habitat> used = EnumSet.noneOf(FishCatalog.Habitat.class);
        FishCatalog.of(FishCatalog.Kind.POISSON).forEach(e -> used.addAll(e.habitats()));
        assertEquals(EnumSet.allOf(FishCatalog.Habitat.class), used);
    }
}
