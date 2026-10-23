package com.mmorpg.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Boutique des marchands (config/mmorpg/shop.json) : articles en vente et prix de rachat. */
public class ShopConfig {
    public List<Offer> buy = new ArrayList<>();
    /** Prix de rachat explicites (identifiant d'objet -> or par unite). */
    public Map<String, Integer> sellPrices = new LinkedHashMap<>();
    /** Prix de rachat par defaut des objets du mod selon leur rarete (materiaux). */
    public Map<String, Integer> materialPriceByRarity = new LinkedHashMap<>();
    /** Multiplicateur applique aux equipements (armes, armures) en plus du prix de rarete. */
    public double equipmentMultiplier = 6.0;

    public static class Offer {
        public String item = "minecraft:bread";
        public int count = 1;
        public int price = 1;
        public int minLevel = 1;

        public Offer() {
        }

        public Offer(String item, int count, int price, int minLevel) {
            this.item = item.contains(":") ? item : "mmorpg:" + item;
            this.count = count;
            this.price = price;
            this.minLevel = minLevel;
        }
    }

    /**
     * Contenu des Mines, de la metallurgie et des cultures : graines et outils du debutant en vente, prix de rachat des lingots,
     * minerais compresses, recoltes et aliments. Ajoute au chargement ce qui manque a une boutique existante (les prix modifies sont conserves).
     */
    public void addNewContent() {
        Object[][] offers = {{"graine_lueur", 4, 16, 1}, {"graine_argent", 3, 60, 18}, {"graine_ambre", 3, 180, 38}, {"graine_givre", 3, 480, 55},
                {"graine_neant", 3, 1400, 75}, {"pioche_debutant", 1, 80, 1}, {"houe_debutant", 1, 60, 1}};
        for (Object[] o : offers) {
            String id = "mmorpg:" + o[0];
            if (buy.stream().noneMatch(x -> x.item.equals(id))) buy.add(new Offer((String) o[0], (Integer) o[1], (Integer) o[2], (Integer) o[3]));
        }
        Object[][] prices = {{"lingot_verdite", 6}, {"lingot_argentite", 18}, {"lingot_ambrasite", 45}, {"lingot_glacium", 110}, {"lingot_neantite", 280},
                {"lingot_aelium", 160}, {"lingot_orichalque", 650}, {"lingot_astralium", 3200},
                {"compresse_verdite", 90}, {"compresse_argentite", 260}, {"compresse_ambrasite", 650}, {"compresse_glacium", 1600}, {"compresse_neantite", 4000},
                {"racine_lueur", 4}, {"ble_argent", 10}, {"baie_ambree", 26}, {"fleur_givre", 70}, {"orchidee_neant", 210},
                {"pain_argent", 12}, {"confiture_ambree", 40}, {"elixir_givre", 110}, {"nectar_neant", 300}};
        for (Object[] o : prices) sellPrices.putIfAbsent("mmorpg:" + o[0], (Integer) o[1]);
    }

    public static ShopConfig defaults() {
        ShopConfig c = new ShopConfig();
        c.buy.add(new Offer("potion_soin_mineure", 1, 8, 1));
        c.buy.add(new Offer("potion_mana_mineure", 1, 8, 1));
        c.buy.add(new Offer("potion_soin", 1, 30, 20));
        c.buy.add(new Offer("potion_mana", 1, 30, 20));
        c.buy.add(new Offer("potion_soin_majeure", 1, 90, 50));
        c.buy.add(new Offer("potion_mana_majeure", 1, 90, 50));
        c.buy.add(new Offer("parchemin_teleportation", 1, 15, 1));
        c.buy.add(new Offer("pierre_amelioration", 1, 60, 10));
        c.buy.add(new Offer("pierre_amelioration_sup", 1, 450, 50));
        c.buy.add(new Offer("trefle_chance", 1, 150, 10));
        c.buy.add(new Offer("trefle_quatre_feuilles", 1, 600, 30));
        c.buy.add(new Offer("parchemin_oubli", 1, 300, 10));
        c.buy.add(new Offer("elixir_experience", 1, 400, 20));
        c.buy.add(new Offer("orbe_renaissance", 1, 2000, 10));
        c.buy.add(new Offer("minecraft:bread", 4, 3, 1));
        c.buy.add(new Offer("minecraft:cooked_beef", 4, 6, 1));
        c.buy.add(new Offer("minecraft:torch", 16, 4, 1));
        c.buy.add(new Offer("minecraft:iron_ingot", 1, 6, 1));
        c.buy.add(new Offer("minecraft:string", 4, 4, 1));
        c.buy.add(new Offer("minecraft:leather", 2, 5, 1));
        c.buy.add(new Offer("minecraft:glass_bottle", 4, 3, 1));
        c.addNewContent();
        c.materialPriceByRarity.put("COMMUN", 1);
        c.materialPriceByRarity.put("PEU_COMMUN", 3);
        c.materialPriceByRarity.put("RARE", 8);
        c.materialPriceByRarity.put("EPIQUE", 20);
        c.materialPriceByRarity.put("LEGENDAIRE", 60);
        c.materialPriceByRarity.put("MYTHIQUE", 150);
        c.sellPrices.put("minecraft:diamond", 25);
        c.sellPrices.put("minecraft:emerald", 10);
        c.sellPrices.put("minecraft:gold_ingot", 4);
        c.sellPrices.put("minecraft:iron_ingot", 2);
        c.sellPrices.put("minecraft:rotten_flesh", 0);
        c.sellPrices.put("minecraft:bone", 1);
        c.sellPrices.put("minecraft:string", 1);
        c.sellPrices.put("minecraft:gunpowder", 1);
        c.sellPrices.put("minecraft:ender_pearl", 5);
        c.sellPrices.put("minecraft:blaze_rod", 4);
        c.sellPrices.put("mmorpg:piece_or", 0);
        return c;
    }
}
