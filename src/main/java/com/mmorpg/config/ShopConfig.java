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
        c.buy.add(new Offer("coffre_cosmetique", 1, 1500, 30));
        c.buy.add(new Offer("oeuf_feu_follet", 1, 250, 5));
        c.buy.add(new Offer("oeuf_slime", 1, 250, 5));
        c.buy.add(new Offer("minecraft:bread", 4, 3, 1));
        c.buy.add(new Offer("minecraft:cooked_beef", 4, 6, 1));
        c.buy.add(new Offer("minecraft:torch", 16, 4, 1));
        c.buy.add(new Offer("minecraft:iron_ingot", 1, 6, 1));
        c.buy.add(new Offer("minecraft:string", 4, 4, 1));
        c.buy.add(new Offer("minecraft:leather", 2, 5, 1));
        c.buy.add(new Offer("minecraft:glass_bottle", 4, 3, 1));
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
