package com.mmorpg.server;

import com.mmorpg.config.*;
import com.mmorpg.entity.NpcEntity;
import com.mmorpg.network.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;

/** Visual per-merchant catalogue; only nearby operators may change it. */
public final class MerchantEditor {
    private static final com.google.gson.Gson JSON=new com.google.gson.Gson();
    public static ShopConfig config(NpcEntity npc) {
        var cfg=JSON.fromJson(npc.merchantShop().isEmpty()?ConfigManager.shopJson():npc.merchantShop(),ShopConfig.class);
        cfg.buy.removeIf(o->RetiredItems.id(o.item));cfg.sellPrices.keySet().removeIf(RetiredItems::id);
        com.mmorpg.economy.ShopSorting.sort(cfg);return cfg;
    }
    public static String revision(NpcEntity npc) {
        try {return java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest((JSON.toJson(config(npc))+npc.skin()+npc.displayName()).getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
        catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    public static void open(ServerPlayer p,NpcEntity npc) {open(p,npc,"");}
    private static void open(ServerPlayer p,NpcEntity npc,String status) {
        if(!QuestEditor.allowed(p)||npc.role()!=NpcEntity.Role.MARCHAND||npc.distanceToSqr(p)>100)return;
        var data=new CompoundTag();data.putInt("npc",npc.getId());data.putString("name",npc.displayName());data.putInt("skin",npc.skin());
        data.putString("shop",JSON.toJson(config(npc)));data.putString("revision",revision(npc));data.putString("status",status);
        Net.toPlayer(p,new S2COpenScreen(S2COpenScreen.MERCHANT_EDITOR,data));
    }
    public static void validate(ShopConfig cfg) {
        if(cfg==null||cfg.buy==null||cfg.sellPrices==null||cfg.materialPriceByRarity==null||cfg.buy.size()>120||cfg.sellPrices.size()>200)
            throw new IllegalArgumentException("Boutique trop grande ou incomplète.");
        if(!Double.isFinite(cfg.equipmentMultiplier)||cfg.equipmentMultiplier<0||cfg.equipmentMultiplier>1000)throw new IllegalArgumentException("Multiplicateur invalide.");
        var seen=new java.util.HashSet<String>();
        for(var offer:cfg.buy) {
            if(offer==null||!validItem(offer.item)||!seen.add(offer.item)||offer.count<1||offer.count>64||offer.price<1||offer.price>10000000||offer.minLevel<1||offer.minLevel>100)
                throw new IllegalArgumentException("Article invalide : quantité 1–64, prix 1–10 000 000, niveau 1–100.");
        }
        for(var entry:cfg.sellPrices.entrySet())if(!validItem(entry.getKey())||entry.getValue()==null||entry.getValue()<0||entry.getValue()>10000000)throw new IllegalArgumentException("Prix de rachat invalide.");
        for(var entry:cfg.materialPriceByRarity.entrySet())if(entry.getValue()==null||entry.getValue()<0||entry.getValue()>10000000)throw new IllegalArgumentException("Prix de rareté invalide.");
    }
    private static boolean validItem(String id) {if(id==null||id.length()>128)return false;var key=net.minecraft.resources.Identifier.tryParse(id);return key!=null&&net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(key)&&net.minecraft.core.registries.BuiltInRegistries.ITEM.getValue(key)!=net.minecraft.world.item.Items.AIR;}
    public static void action(ServerPlayer p,CompoundTag data) {
        if(!QuestEditor.allowed(p)||!(p.level().getEntity(data.getIntOr("npc",-1)) instanceof NpcEntity npc)||npc.role()!=NpcEntity.Role.MARCHAND||npc.distanceToSqr(p)>100)return;
        if(data.getStringOr("action","").equals("open")){open(p,npc);return;}
        if(!data.getStringOr("action","").equals("save"))return;
        try {
            if(!revision(npc).equals(data.getStringOr("revision","")))throw new IllegalArgumentException("Marchand modifié entre-temps : recharge effectuée.");
            String json=data.getStringOr("shop","");if(json.length()>22000)throw new IllegalArgumentException("Boutique trop grande.");
            var cfg=JSON.fromJson(json,ShopConfig.class);validate(cfg);com.mmorpg.economy.ShopSorting.sort(cfg);
            int skin=data.getIntOr("skin",-1);String name=data.getStringOr("name","").trim();
            if(skin<0||skin>=NpcEntity.SKINS||name.isBlank()||name.length()>80)throw new IllegalArgumentException("Nom ou apparence invalide.");
            npc.setMerchantShop(JSON.toJson(cfg));npc.setup(NpcEntity.Role.MARCHAND,name,skin,npc.group());
            com.mmorpg.world.NpcDirectory.get(p.level().getServer()).record(npc);
            open(p,npc,"Marchand enregistré : boutique et apparence propres à ce PNJ.");
        } catch(Exception e) {open(p,npc,e instanceof IllegalArgumentException?e.getMessage():"Enregistrement impossible.");}
    }
}
