package com.mmorpg.job;

import com.mmorpg.cosmetic.Cosmetic;
import com.mmorpg.crafting.ForgeProgression;
import com.mmorpg.crafting.ForgeRecipes;
import com.mmorpg.registry.ModItems;
import com.mmorpg.rpg.PlayerData;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.List;

/** Verifications des metiers : courbe d'XP, paliers, octroi des recompenses, recettes et persistance. Lance par le mode de test « metiers ». */
public final class JobChecks {
    private JobChecks() {
    }

    public static List<String> run() {
        List<String> fails = new ArrayList<>();
        // --- courbe exponentielle
        if (Job.xpToNext(1) != 60) fails.add("xpToNext(1) = " + Job.xpToNext(1));
        for (int l = 1; l < Job.MAX_LEVEL; l++) {
            if (Job.xpToNext(l + 1) < Job.xpToNext(l) * 1.1) fails.add("courbe non exponentielle au niveau " + l);
            if (Job.levelFor(Job.totalFor(l)) != l) fails.add("levelFor(totalFor(" + l + "))");
            if (l > 1 && Job.levelFor(Job.totalFor(l) - 1) != l - 1) fails.add("levelFor(totalFor(" + l + ")-1)");
        }
        long total = Job.totalFor(Job.MAX_LEVEL);
        if (total < 150_000 || total > 220_000) fails.add("XP totale niveau 50 hors plage : " + total);
        if (Job.levelFor(Long.MAX_VALUE / 2) != Job.MAX_LEVEL) fails.add("plafond de niveau");
        // duree estimee (actions moyennes, rendements mesures par metier) : au moins ~20 h de jeu actif
        double mine = total / (Job.ORE_XP[0] * 0.2 + Job.ORE_XP[1] * 0.2 + Job.ORE_XP[2] * 0.2 + Job.ORE_XP[3] * 0.2 + Job.ORE_XP[4] * 0.2) / 400.0;
        if (mine < 20) fails.add("progression mineur trop courte : " + mine + " h");
        // --- paliers et bonus
        if (Job.tier(9) != 0 || Job.tier(10) != 1 || Job.tier(49) != 4 || Job.tier(50) != 5) fails.add("tier()");
        for (Job j : Job.values()) {
            double prev = -1;
            for (int l = 1; l <= Job.MAX_LEVEL; l++) {
                double c = Job.doubleChance(j, l);
                if (c < prev || c > 0.30001) fails.add("doubleChance(" + j.id + "," + l + ") = " + c);
                prev = c;
            }
            if (Math.abs(Job.doubleChance(j, 50) - 0.30) > 1e-9) fails.add("double " + j.id + " niveau 50");
            var perks = Job.perks(j);
            if (perks.size() != 10) fails.add(j.id + " : 10 bonus attendus, " + perks.size());
            for (int i = 0; i < perks.size(); i++) {
                int l = (i + 1) * 5;
                if (perks.get(i).level() != l || perks.get(i).text().isBlank()) fails.add(j.id + " : bonus " + l + " invalide");
                if (l > 5 && l < 50 && perks.get(i).text().equals(perks.get(i - 1).text())) fails.add(j.id + " : bonus " + l + " identique au precedent");
                // chaque niveau multiple de 5 doit changer un effet reel par rapport a 5 niveaux plus tot
                boolean changed = Job.doubleChance(j, l) != Job.doubleChance(j, l - 5) || Job.miningSpeed(l) != Job.miningSpeed(l - 5)
                        || Job.rareCatchChance(l) != Job.rareCatchChance(l - 5) || Job.mealChance(l) != Job.mealChance(l - 5);
                if (!changed) fails.add(j.id + " : le bonus du niveau " + l + " ne change aucun effet");
            }
        }
        if (Job.miningSpeed(4) != 1f || Job.miningSpeed(5) <= 1f || Job.miningSpeed(50) < 1.5f) fails.add("vitesse de minage");
        // --- recompenses : references valides
        for (Job j : Job.values()) {
            if (j.rewards.size() != 5) fails.add(j.id + " : 5 paliers attendus");
            for (int i = 0; i < j.rewards.size(); i++) {
                Job.Reward r = j.rewards.get(i);
                if (r.level() != (i + 1) * 10) fails.add(j.id + " : palier " + r.level() + " mal place");
                if (r.cosmeticId() != null && Cosmetic.byId(r.cosmeticId()) == null) fails.add("cosmetique inconnu " + r.cosmeticId());
                if (r.recipeId() != null) {
                    if (ForgeRecipes.get(r.recipeId()) == null) fails.add("recette inconnue " + r.recipeId());
                    if (BuiltInRegistries.ITEM.getOptional(net.minecraft.resources.Identifier.parse("mmorpg:" + r.recipeId())).isEmpty()) fails.add("plat non enregistre " + r.recipeId());
                }
                if (r.itemId() != null && ModItems.get(r.itemId()) == net.minecraft.world.item.Items.AIR) fails.add("objet inconnu " + r.itemId());
                if (r.title().isBlank()) fails.add("titre vide " + j.id + ":" + r.level());
            }
        }
        // --- verrouillage des recettes de forge
        PlayerData d = new PlayerData();
        var soupe = ForgeRecipes.get("soupe_de_racine");
        d.jobXp.put("fermier", Job.totalFor(9));
        if (soupe == null || ForgeProgression.unlocked(d, soupe)) fails.add("soupe_de_racine debloquee avant le niveau 10");
        d.jobXp.put("fermier", Job.totalFor(10));
        if (soupe == null || !ForgeProgression.unlocked(d, soupe)) fails.add("soupe_de_racine non debloquee au niveau 10");
        if (!Job.recipeUnlocked(d.jobXp, "potion_soin")) fails.add("recette sans metier verrouillee");
        // --- octroi des recompenses (idempotent)
        d = new PlayerData();
        d.jobXp.put("mineur", Job.totalFor(25));
        int n = JobSystem.syncRewards(null, d);
        if (n != 2 || !d.jobClaimed.contains("mineur:10") || !d.jobClaimed.contains("mineur:20")) fails.add("octroi niveau 25 : " + n + " " + d.jobClaimed);
        if (!d.cosmetics.contains("aura_arcane") || d.gold != 2000) fails.add("cosmetique/or du palier 20 : " + d.cosmetics + " " + d.gold);
        if (JobSystem.syncRewards(null, d) != 0 || d.gold != 2000) fails.add("octroi non idempotent");
        d.jobXp.put("mineur", total);
        JobSystem.syncRewards(null, d);
        if (d.jobClaimed.size() != 5 || d.gold != 22000 || !d.cosmetics.contains("couronne_infernale") || !d.cosmetics.contains("couronne_givre")) fails.add("octroi niveau 50 : " + d.jobClaimed + " " + d.gold + " " + d.cosmetics);
        // --- progression via addXp (montee de niveau et paliers franchis)
        d = new PlayerData();
        JobSystem.addXp(null, d, Job.FERMIER, Job.totalFor(10) - 1);
        if (Job.levelOf(d.jobXp, Job.FERMIER) != 9 || !d.jobClaimed.isEmpty()) fails.add("addXp avant le palier 10");
        JobSystem.addXp(null, d, Job.FERMIER, 1);
        JobSystem.syncRewards(null, d);
        if (Job.levelOf(d.jobXp, Job.FERMIER) != 10 || !d.jobClaimed.contains("fermier:10")) fails.add("addXp au palier 10");
        // --- persistance
        PlayerData copy = new PlayerData();
        copy.load(d.save());
        if (!copy.jobXp.equals(d.jobXp) || !copy.jobClaimed.equals(d.jobClaimed)) fails.add("sauvegarde/chargement des metiers");
        return fails;
    }
}
