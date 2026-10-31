package com.mmorpg.server;

import com.mmorpg.crafting.ForgeRecipes;
import com.mmorpg.item.ItemDefs;
import com.mmorpg.quest.QuestDef;
import com.mmorpg.rpg.PlayerClass;

import java.util.ArrayList;
import java.util.List;

/**
 * Equipement de depart du tutoriel : rien n'est offert au debut, le joueur fabrique sa premiere arme et sa premiere armure a la forge.
 * Les quetes utilisent des jetons ({@code @arme}, {@code @casque}...) qui designent la piece de SA classe, et des jetons de materiaux
 * ({@code @mat_arme}, {@code @mat_armure}) pour la remise des ingredients correspondants.
 */
public final class TutorialKit {
    private TutorialKit() {
    }

    public static String weapon(PlayerClass cls) {
        return switch (cls) {
            case GUERRIER -> "epee_recrue";
            case MAGE -> "baton_apprenti";
            case ARCHER -> "arc_chasseur";
            default -> "masse_garde";
        };
    }

    public static String set(PlayerClass cls) {
        return switch (cls) {
            case GUERRIER -> "acier_soldat";
            case MAGE -> "apprenti";
            case ARCHER -> "chasseur";
            default -> "garde";
        };
    }

    /** Remplace les jetons de piece (@arme, @casque, @plastron, @jambieres, @bottes) par l'identifiant reel pour cette classe. */
    public static String resolve(String key, PlayerClass cls) {
        if (!key.contains("@")) return key;
        String set = set(cls);
        return key.replace("@arme", weapon(cls)).replace("@casque", set + "_" + ItemDefs.PIECES[0]).replace("@plastron", set + "_" + ItemDefs.PIECES[1])
                .replace("@jambieres", set + "_" + ItemDefs.PIECES[2]).replace("@bottes", set + "_" + ItemDefs.PIECES[3]);
    }

    /** Objets d'une recompense, jetons de materiaux developpes selon la classe du joueur. */
    public static List<QuestDef.ItemReward> rewards(QuestDef q, PlayerClass cls) {
        List<QuestDef.ItemReward> out = new ArrayList<>();
        for (QuestDef.ItemReward r : q.rewards.items) {
            if (r.item.endsWith("@mat_arme")) add(out, ForgeRecipes.get(weapon(cls)));
            else if (r.item.endsWith("@mat_armure")) for (String piece : ItemDefs.PIECES) add(out, ForgeRecipes.get(set(cls) + "_" + piece));
            else out.add(r);
        }
        return out;
    }

    private static void add(List<QuestDef.ItemReward> out, ForgeRecipes.Recipe recipe) {
        if (recipe == null) return;
        for (ForgeRecipes.Ingredient ing : recipe.ingredients()) {
            boolean merged = false;
            for (int i = 0; i < out.size() && !merged; i++) {
                if (out.get(i).item.equals(ing.item())) {
                    out.set(i, new QuestDef.ItemReward(ing.item(), out.get(i).count + ing.count()));
                    merged = true;
                }
            }
            if (!merged) out.add(new QuestDef.ItemReward(ing.item(), ing.count()));
        }
    }
}
