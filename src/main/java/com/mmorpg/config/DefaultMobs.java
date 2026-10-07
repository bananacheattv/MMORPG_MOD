package com.mmorpg.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Valeurs par defaut des monstres et boss (ecrites dans mobs.json au premier lancement). */
public final class DefaultMobs {
    /** A augmenter quand de nouveaux objets sont ajoutes au butin par defaut : ils sont fusionnes dans les mobs.json existants. */
    public static final int DROPS_VERSION = 2;

    private DefaultMobs() {
    }

    private static MobConfig mob(int min, int max, double hp, double hpL, double atk, double atkL, double defL, double speed, double scale) {
        MobConfig c = new MobConfig();
        c.minLevel = min;
        c.maxLevel = max;
        c.baseHealth = hp;
        c.healthPerLevel = hpL;
        c.baseAttack = atk;
        c.attackPerLevel = atkL;
        c.defensePerLevel = defL;
        c.speed = speed;
        c.scale = scale;
        c.dropsVersion = DROPS_VERSION;
        return c;
    }

    private static void spawn(MobConfig c, int weight, int minG, int maxG, boolean dark, List<String> dims, String... biomes) {
        c.spawn.weight = weight;
        c.spawn.minGroup = minG;
        c.spawn.maxGroup = maxG;
        c.spawn.darkOnly = dark;
        c.spawn.dimensions = new ArrayList<>(dims);
        c.spawn.biomes = new ArrayList<>(List.of(biomes));
    }

    private static void drop(MobConfig c, String item, double chance, int min, int max) {
        c.drops.add(new MobConfig.Drop(item.contains(":") ? item : "mmorpg:" + item, chance, min, max));
    }

    private static MobConfig boss(int level, double hp, double hpL, double atk, double atkL, double defL, double speed, double scale) {
        MobConfig c = mob(level, level, hp, hpL, atk, atkL, defL, speed, scale);
        c.boss = true;
        c.xpMultiplier = 25;
        c.spawn.enabled = false;
        return c;
    }

    public static Map<String, MobConfig> create() {
        Map<String, MobConfig> m = new LinkedHashMap<>();
        List<String> ow = List.of("minecraft:overworld");

        MobConfig gob = mob(1, 12, 60, 22, 8, 2.4, 1.0, 0.31, 0.85);
        spawn(gob, 30, 2, 4, false, ow, "#minecraft:is_forest", "minecraft:plains", "minecraft:sunflower_plains", "minecraft:meadow", "minecraft:savanna");
        drop(gob, "oreille_gobelin", 0.6, 1, 2);
        drop(gob, "ferraille_gobeline", 0.4, 1, 2);
        drop(gob, "potion_soin_mineure", 0.05, 1, 1);
        drop(gob, "soie_araignee", 0.15, 1, 1);
        m.put("gobelin", gob);

        MobConfig wolf = mob(5, 18, 80, 25, 10, 2.8, 0.8, 0.36, 1.0);
        spawn(wolf, 22, 2, 3, true, ow, "#minecraft:is_taiga", "#minecraft:is_forest", "minecraft:snowy_plains", "minecraft:grove");
        drop(wolf, "croc_loup", 0.6, 1, 2);
        drop(wolf, "fourrure_sombre", 0.35, 1, 1);
        drop(wolf, "griffe_loup", 0.45, 1, 2);
        m.put("loup_sombre", wolf);

        MobConfig skel = mob(15, 32, 120, 28, 12, 3.0, 1.2, 0.27, 1.0);
        spawn(skel, 20, 1, 3, true, ow, "*");
        drop(skel, "os_maudit", 0.6, 1, 2);
        drop(skel, "poussiere_ame", 0.4, 1, 2);
        drop(skel, "fragment_ossuaire", 0.35, 1, 2);
        drop(skel, "crane_maudit", 0.06, 1, 1);
        m.put("squelette_maudit", skel);

        MobConfig orc = mob(25, 45, 300, 45, 20, 4.5, 2.0, 0.27, 1.25);
        spawn(orc, 18, 1, 3, false, ow, "#minecraft:is_savanna", "#minecraft:is_badlands", "#minecraft:is_mountain", "#minecraft:is_hill", "minecraft:windswept_hills");
        drop(orc, "defense_orc", 0.5, 1, 1);
        drop(orc, "acier_orc", 0.5, 1, 2);
        drop(orc, "potion_soin", 0.05, 1, 1);
        drop(orc, "sang_orc", 0.35, 1, 2);
        m.put("orc_guerrier", orc);

        MobConfig fire = mob(40, 60, 400, 55, 25, 5.0, 2.0, 0.26, 1.0);
        spawn(fire, 16, 1, 2, false, List.of("minecraft:overworld", "minecraft:the_nether"), "#minecraft:is_nether", "minecraft:desert", "#minecraft:is_badlands");
        drop(fire, "noyau_flamme", 0.55, 1, 2);
        drop(fire, "cendre_ardente", 0.45, 1, 3);
        m.put("elementaire_feu", fire);

        MobConfig ice = mob(50, 70, 450, 60, 28, 5.5, 2.2, 0.28, 1.0);
        spawn(ice, 16, 1, 2, false, ow, "minecraft:snowy_plains", "minecraft:ice_spikes", "minecraft:frozen_peaks", "minecraft:snowy_slopes",
                "minecraft:jagged_peaks", "minecraft:snowy_taiga", "minecraft:grove", "minecraft:frozen_river", "minecraft:snowy_beach");
        drop(ice, "eclat_givre", 0.55, 1, 2);
        drop(ice, "potion_mana", 0.05, 1, 1);
        drop(ice, "voile_spectral", 0.2, 1, 1);
        m.put("spectre_givre", ice);

        MobConfig golem = mob(60, 80, 900, 90, 35, 6.0, 3.5, 0.22, 1.0);
        spawn(golem, 12, 1, 1, true, ow, "#minecraft:is_mountain", "minecraft:dripstone_caves", "minecraft:lush_caves", "minecraft:deep_dark");
        drop(golem, "cristal_arcanique", 0.45, 1, 2);
        drop(golem, "gemme_brute", 0.3, 1, 2);
        m.put("golem_cristal", golem);

        MobConfig knight = mob(75, 95, 1000, 100, 40, 7.0, 3.5, 0.28, 1.15);
        spawn(knight, 12, 1, 2, true, List.of("minecraft:overworld", "minecraft:the_nether", "minecraft:the_end"),
                "#minecraft:is_end", "minecraft:deep_dark", "minecraft:soul_sand_valley", "minecraft:warped_forest");
        drop(knight, "essence_neant", 0.45, 1, 2);
        drop(knight, "potion_soin_majeure", 0.04, 1, 1);
        drop(knight, "plaque_neant", 0.25, 1, 1);
        m.put("chevalier_neant", knight);

        // ---------------- Boss
        MobConfig king = boss(20, 1500, 250, 15, 3.5, 2.5, 0.30, 1.7);
        drop(king, "couronne_roi_gobelin", 1.0, 1, 1);
        drop(king, "trefle_chance", 0.5, 1, 2);
        drop(king, "piece_or", 1.0, 20, 40);
        drop(king, "pierre_amelioration", 1.0, 2, 4);
        drop(king, "epee_berserker", 0.12, 1, 1);
        drop(king, "baton_flammes", 0.12, 1, 1);
        drop(king, "arc_tempete", 0.12, 1, 1);
        drop(king, "marteau_colosse", 0.12, 1, 1);
        drop(king, "oeuf_slime", 0.25, 1, 1);
        drop(king, "coffre_cosmetique", 0.15, 1, 1);
        king.abilities.put("slamCooldown", 160.0);
        king.abilities.put("slamDamage", 1.5);
        king.abilities.put("summonCooldown", 400.0);
        king.abilities.put("summonCount", 3.0);
        king.abilities.put("enrageThreshold", 0.3);
        m.put("roi_gobelin", king);

        MobConfig lich = boss(45, 3000, 400, 20, 5.0, 3.0, 0.27, 1.4);
        drop(lich, "phylactere_liche", 1.0, 1, 1);
        drop(lich, "trefle_quatre_feuilles", 0.4, 1, 2);
        drop(lich, "piece_or", 1.0, 50, 90);
        drop(lich, "pierre_amelioration", 1.0, 3, 6);
        drop(lich, "epee_seigneur_guerre", 0.10, 1, 1);
        drop(lich, "baton_arcanique", 0.10, 1, 1);
        drop(lich, "arc_faucon", 0.10, 1, 1);
        drop(lich, "masse_gardien", 0.10, 1, 1);
        drop(lich, "oeuf_chouette", 0.25, 1, 1);
        drop(lich, "coffre_cosmetique", 0.25, 1, 1);
        lich.abilities.put("boltCooldown", 40.0);
        lich.abilities.put("boltDamage", 0.9);
        lich.abilities.put("summonCooldown", 500.0);
        lich.abilities.put("summonCount", 2.0);
        lich.abilities.put("blinkCooldown", 160.0);
        lich.abilities.put("drainCooldown", 300.0);
        m.put("liche_ancienne", lich);

        MobConfig ignis = boss(65, 4000, 600, 30, 6.0, 3.5, 0.28, 2.0);
        drop(ignis, "coeur_infernal", 1.0, 1, 1);
        drop(ignis, "trefle_dore", 0.5, 1, 1);
        drop(ignis, "piece_or", 1.0, 90, 150);
        drop(ignis, "pierre_amelioration_sup", 1.0, 1, 2);
        drop(ignis, "lame_demoniaque", 0.10, 1, 1);
        drop(ignis, "sceptre_neant", 0.10, 1, 1);
        drop(ignis, "arc_spectral", 0.10, 1, 1);
        drop(ignis, "marteau_titan", 0.10, 1, 1);
        drop(ignis, "oeuf_phenix", 0.20, 1, 1);
        drop(ignis, "coffre_cosmetique", 0.30, 1, 1);
        ignis.abilities.put("fireballCooldown", 120.0);
        ignis.abilities.put("fireballCount", 5.0);
        ignis.abilities.put("ringCooldown", 240.0);
        ignis.abilities.put("meteorCooldown", 300.0);
        m.put("seigneur_ignis", ignis);

        MobConfig titan = boss(80, 6000, 800, 40, 7.0, 4.0, 0.22, 1.6);
        drop(titan, "coeur_glace_eternelle", 1.0, 1, 1);
        drop(titan, "trefle_celeste", 0.4, 1, 1);
        drop(titan, "piece_or", 1.0, 150, 250);
        drop(titan, "pierre_amelioration_sup", 1.0, 2, 3);
        drop(titan, "oeuf_loup_spectral", 0.30, 1, 1);
        drop(titan, "coffre_cosmetique", 0.35, 1, 1);
        titan.abilities.put("slamCooldown", 140.0);
        titan.abilities.put("slamDamage", 1.6);
        titan.abilities.put("shardCooldown", 200.0);
        titan.abilities.put("shardCount", 10.0);
        titan.abilities.put("auraRadius", 7.0);
        m.put("titan_glace", titan);

        MobConfig avatar = boss(100, 10000, 1000, 50, 8.0, 4.5, 0.30, 2.2);
        drop(avatar, "fragment_divin", 1.0, 2, 3);
        drop(avatar, "trefle_divin", 0.5, 1, 1);
        drop(avatar, "piece_or", 1.0, 300, 500);
        drop(avatar, "pierre_amelioration_sup", 1.0, 3, 5);
        drop(avatar, "excalibur_celeste", 0.08, 1, 1);
        drop(avatar, "baton_archimage", 0.08, 1, 1);
        drop(avatar, "arc_aube_divine", 0.08, 1, 1);
        drop(avatar, "marteau_egide", 0.08, 1, 1);
        drop(avatar, "oeuf_dragonnet", 0.20, 1, 1);
        drop(avatar, "coffre_cosmetique", 0.50, 1, 2);
        avatar.abilities.put("orbCooldown", 100.0);
        avatar.abilities.put("blinkCooldown", 180.0);
        avatar.abilities.put("voidCooldown", 300.0);
        avatar.abilities.put("summonCount", 2.0);
        m.put("avatar_neant", avatar);
        MobConfig zombie = mob(8, 22, 110, 26, 11, 2.7, 1.1, 0.25, 1);
        zombie.displayName = "Zombie des Cryptes";
        zombie.family = "Morts-vivants";
        spawn(zombie, 14, 1, 3, true, ow, "*");
        drop(zombie, "minecraft:rotten_flesh", .8, 1, 3);
        drop(zombie, "os_maudit", .25, 1, 1);
        drop(zombie, "ichor_cryptes", .35, 1, 2);
        m.put("zombie_des_cryptes", zombie);

        MobConfig spider = mob(18, 35, 140, 24, 13, 3, .8, .32, 1.5);
        spider.displayName = "Araignée Venimeuse";
        spider.family = "Bêtes";
        spawn(spider, 12, 1, 2, true, ow, "#minecraft:is_forest", "minecraft:swamp", "minecraft:lush_caves");
        drop(spider, "minecraft:string", .8, 2, 5);
        drop(spider, "minecraft:spider_eye", .45, 1, 2);
        drop(spider, "soie_araignee", .6, 1, 3);
        m.put("araignee_venimeuse", spider);

        MobConfig bandit = mob(30, 50, 250, 35, 18, 4, 1.7, .30, 1);
        bandit.displayName = "Bandit Arbalétrier";
        bandit.family = "Humanoïdes";
        spawn(bandit, 12, 1, 3, false, ow, "minecraft:plains", "#minecraft:is_forest", "minecraft:savanna");
        drop(bandit, "minecraft:leather", .5, 1, 3);
        drop(bandit, "minecraft:iron_ingot", .20, 1, 2);
        drop(bandit, "piece_or", .8, 5, 15);
        drop(bandit, "insigne_bandit", .4, 1, 1);
        m.put("bandit_arbaletrier", bandit);
        String[][] biomes = {{"#minecraft:is_forest"}, {"minecraft:swamp", "minecraft:mangrove_swamp"},
                {"minecraft:dripstone_caves", "minecraft:deep_dark"}, {"minecraft:snowy_plains", "minecraft:ice_spikes", "minecraft:grove"},
                {"minecraft:desert", "#minecraft:is_badlands"}, {"#minecraft:is_end"}};
        String[] loot = {"minecraft:oak_log", "minecraft:slime_ball", "os_maudit", "eclat_givre", "noyau_flamme", "essence_neant"};
        // butin propre a chaque zone : commun, peu commun, et rare (reserve aux grandes creatures de la zone)
        String[][] zoneLoot = {{"ecorce_ancienne", "seve_lumineuse", "coeur_racine"}, {"mucus_acide", "venin_marais", "ecaille_hydre"},
                {"fragment_ossuaire", "relique_funeraire", "couronne_cryptes"}, {"fourrure_polaire", "carapace_givree", "croc_wyrm"},
                {"carapace_cuivre", "bandelette_ancienne", "braise_djinn"}, {"eclat_astral", "poussiere_etoile", "eclat_fracture"}};
        List<String> elites = List.of("colosse_racine", "hydre_marais", "roi_cryptes", "wyrm_boreal", "djinn_braises", "devoreur_etoiles", "chevalier_fracture");
        for (var entry : com.mmorpg.entity.mob.ImportedMobs.ALL) {
            int z = entry.zone();
            MobConfig c = mob(1 + z * 16, Math.min(100, 18 + z * 16), 70 + z * 70, 22 + z * 8, 8 + z * 5, 2.4 + z * .6, 1 + z * .4, .27, 1);
            c.displayName = entry.name();
            c.family = com.mmorpg.entity.mob.ImportedMobs.ZONES[z];
            spawn(c, 4, 1, 2, z == 2, z == 5 ? List.of("minecraft:the_end") : ow, biomes[z]);
            drop(c, loot[z], .5, 1, 2);
            drop(c, zoneLoot[z][0], .55, 1, 2);
            drop(c, zoneLoot[z][1], .25, 1, 1);
            if (elites.contains(entry.key())) drop(c, zoneLoot[z][2], .35, 1, 1);
            if (entry.key().equals("araignee_os")) drop(c, "soie_araignee", .5, 1, 2);
            drop(c, "piece_or", .6, 2 + z * 3, 5 + z * 5);
            m.put(entry.key(), c);
        }
        return m;
    }
}
