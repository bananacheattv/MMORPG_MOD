package com.mmorpg.server;

import com.mmorpg.entity.NpcEntity;
import com.mmorpg.config.ShopConfig;
import com.mmorpg.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class DevMerchantChecks {
    private static void check(boolean value,String message){if(!value)throw new AssertionError("merchant: "+message);}
    public static void run(ServerPlayer p) {
        var level=p.level();p.setPos(80.5,100,80.5);
        var npc=com.mmorpg.world.DungeonBuilder.spawnNpc(level,new BlockPos(81,100,80),NpcEntity.Role.MARCHAND,"Marchand test",0,0);
        var other=com.mmorpg.world.DungeonBuilder.spawnNpc(level,new BlockPos(82,100,80),NpcEntity.Role.MARCHAND,"Marchand voisin",0,0);
        String global=com.mmorpg.config.ConfigManager.shopJson();var gson=new com.google.gson.Gson();
        var cfg=new ShopConfig();cfg.automaticBuyback=false;cfg.buy.add(new ShopConfig.Offer("oreille_gobelin",2,17,1));cfg.sellPrices.put("mmorpg:oreille_gobelin",4);
        var packet=new CompoundTag();packet.putString("action","save");packet.putInt("npc",npc.getId());packet.putString("revision",MerchantEditor.revision(npc));
        packet.putString("name","Échoppe du test");packet.putInt("skin",3);packet.putString("shop",gson.toJson(cfg));
        var identity=new net.minecraft.server.players.NameAndId(p.getGameProfile());level.getServer().getPlayerList().op(identity);
        long gold=RpgPlayers.get(p).gold;
        try {
            MerchantEditor.action(p,packet);
            check(!MerchantEditor.config(npc).automaticBuyback&&com.mmorpg.economy.ShopPricing.sellPrice(new net.minecraft.world.item.ItemStack(ModItems.OREILLE_GOBELIN.get()),MerchantEditor.config(npc))==4,"explicit buyback works without automatic buyback");
            var emptyShop=new ShopConfig();emptyShop.automaticBuyback=false;check(com.mmorpg.economy.ShopPricing.sellPrice(new net.minecraft.world.item.ItemStack(ModItems.OREILLE_GOBELIN.get()),emptyShop)==0,"empty merchant refuses unconfigured buybacks");
            check(npc.skin()==3&&npc.displayName().equals("Échoppe du test"),"name and chosen appearance applied");
            check(MerchantEditor.config(npc).buy.size()==1&&other.merchantShop().isEmpty()&&global.equals(com.mmorpg.config.ConfigManager.shopJson()),"per-merchant catalogue isolation");
            var saved=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,level.registryAccess());npc.saveWithoutId(saved);
            check(saved.buildResult().getStringOr("MerchantShop","").contains("oreille_gobelin"),"merchant catalogue persists with NPC");
            int before=ForgeManager.count(p.getInventory(),ModItems.OREILLE_GOBELIN.get());RpgPlayers.get(p).gold=1000;
            ShopManager.buy(p,npc.getId(),0,1);check(RpgPlayers.get(p).gold==983&&ForgeManager.count(p.getInventory(),ModItems.OREILLE_GOBELIN.get())==before+2,"buy uses this merchant's price");
            int slot=-1;for(int i=0;i<p.getInventory().getContainerSize();i++)if(p.getInventory().getItem(i).is(ModItems.OREILLE_GOBELIN.get())){slot=i;break;}
            ShopManager.sell(p,npc.getId(),slot,1,false);check(RpgPlayers.get(p).gold==987,"sell uses this merchant's buyback price");
            var stale=packet.copy();stale.putString("name","STALE");MerchantEditor.action(p,stale);check(!npc.displayName().equals("STALE"),"stale editor cannot overwrite newer changes");
            level.getServer().getPlayerList().deop(identity);var denied=packet.copy();denied.putString("revision",MerchantEditor.revision(npc));denied.putString("name","DENIED");MerchantEditor.action(p,denied);check(!npc.displayName().equals("DENIED"),"non operator denied");
            var invalid=new ShopConfig();invalid.buy.add(new ShopConfig.Offer("minecraft:air",1,1,1));try{MerchantEditor.validate(invalid);throw new AssertionError("air accepted");}catch(IllegalArgumentException expected){}
            com.mmorpg.MMORPG.LOGGER.info("[CODEX CHECKS] MERCHANT_EDITOR PASS: permissions, appearance, per-NPC persistence, actual buy/sell prices, isolation, stale changes and invalid items");
        } finally {RpgPlayers.get(p).gold=gold;npc.discard();other.discard();level.getServer().getPlayerList().deop(identity);}
    }
}
