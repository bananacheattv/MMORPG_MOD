package com.mmorpg.server;

import com.mmorpg.registry.*;
import com.mmorpg.rpg.*;
import com.mmorpg.mine.ToolRunes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public final class DevNewRulesChecks {
    private static void check(boolean ok,String message){if(!ok)throw new AssertionError("new beta rules: "+message);}
    public static void run(ServerPlayer p)throws Exception {
        var server=p.level().getServer();var cfg=com.mmorpg.network.NetworkConfig.current;
        try {
            com.mmorpg.network.NetworkConfig.current=new com.mmorpg.network.NetworkConfig();
            for(var recipe:com.mmorpg.crafting.ForgeRecipes.all())check(!RetiredItems.id(recipe.id())&&!recipe.id().startsWith("mannequin_"),"retired recipe absent "+recipe.id());
            check(VanillaStations.disabled(net.minecraft.world.level.block.Blocks.FURNACE)&&VanillaStations.disabled(net.minecraft.world.level.block.Blocks.BLAST_FURNACE)&&VanillaStations.disabled(net.minecraft.world.level.block.Blocks.CRAFTING_TABLE)&&VanillaStations.disabled(net.minecraft.world.level.block.Blocks.ANVIL)&&VanillaStations.disabled(net.minecraft.world.level.block.Blocks.ENCHANTING_TABLE),"all requested vanilla stations blocked");
            var furnacePos=p.blockPosition().offset(3,0,0);var previous=p.level().getBlockState(furnacePos);p.level().setBlockAndUpdate(furnacePos,net.minecraft.world.level.block.Blocks.FURNACE.defaultBlockState());
            var furnace=(net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity)p.level().getBlockEntity(furnacePos);furnace.setItem(0,new ItemStack(net.minecraft.world.item.Items.IRON_ORE));furnace.setItem(1,new ItemStack(net.minecraft.world.item.Items.COAL));
            for(int tick=0;tick<220;tick++)net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity.serverTick(p.level(),furnacePos,p.level().getBlockState(furnacePos),furnace);
            check(furnace.getItem(2).isEmpty()&&furnace.getItem(1).getCount()==1,"hopper-fed furnace cannot smelt or consume fuel");p.level().setBlockAndUpdate(furnacePos,previous);
            check(ModItems.get("parchemin_plan_hache_verdite")!=null&&RetiredItems.id("parchemin_plan_hache_verdite"),"retired tool scrolls remain decodable for profile migration");
            var original=com.google.gson.JsonParser.parseString(com.mmorpg.hub.CinematicConfig.DEFAULT).getAsJsonObject();var shortened=com.google.gson.JsonParser.parseString(com.mmorpg.hub.CinematicConfig.shortened(original.toString())).getAsJsonObject();
            for(int i=0;i<original.getAsJsonArray("plans").size();i++){var before=original.getAsJsonArray("plans").get(i).getAsJsonObject();var after=shortened.getAsJsonArray("plans").get(i).getAsJsonObject();if(before.has("classe"))check(after.get("duree").getAsDouble()<=before.get("duree").getAsDouble()*.5,"class shots at least halved");}
            check(com.mmorpg.hub.CinematicConfig.validate(shortened.toString())==null,"shortened smooth camera path valid");
            var d=RpgPlayers.get(p);d.playerClass=PlayerClass.TANK;d.level=60;p.getInventory().clearContent();p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(ModItems.get("marteau_runique")));
            InventoryRules.apply(p);PaladinOffhand.maintain(p);check(p.getOffhandItem().is(ModItems.PALADIN_SHIELDS.get(2).get()),"hammer equips matching paladin shield");
            InventoryRules.apply(p);PaladinOffhand.maintain(p);check(PaladinOffhand.shield(p.getOffhandItem()),"inventory restriction preserves bound shield");
            int duplicated=0;for(var stack:p.getInventory())if(PaladinOffhand.shield(stack))duplicated+=stack.getCount();check(duplicated<=1,"bound shield never duplicated into inventory");
            p.setItemSlot(EquipmentSlot.MAINHAND,ItemStack.EMPTY);PaladinOffhand.maintain(p);check(p.getOffhandItem().isEmpty(),"shield disappears without hammer");
            p.getInventory().setItem(4,new ItemStack(ModItems.get("cle_aventure")));p.getInventory().setItem(5,new ItemStack(ModItems.get("hache_verdite")));RetiredItems.clear(p.getInventory());check(p.getInventory().getItem(4).isEmpty()&&p.getInventory().getItem(5).isEmpty(),"legacy inventory migration removes retired items");
            var forge=p.blockPosition().offset(2,0,0);var old=p.level().getBlockState(forge);p.level().setBlockAndUpdate(forge,ModBlocks.FORGE_ARCANIQUE.get().defaultBlockState());
            p.getInventory().setItem(4,new ItemStack(ModItems.get("pioche_verdite")));d.gold=20000;
            for(int i=0;i<4;i++){p.getInventory().setItem(5,new ItemStack(ModItems.get(ToolRunes.IDS[i]),6));for(int rank=0;rank<5;rank++)ForgeManager.upgrade(p,forge,4,"rune:"+ToolRunes.IDS[i]);check(ToolRunes.rank(p.getInventory().getItem(4),i)==5,"rune reaches rank five");long gold=d.gold;ForgeManager.upgrade(p,forge,4,"rune:"+ToolRunes.IDS[i]);check(d.gold==gold,"max rune refuses extra charge");}
            p.level().setBlockAndUpdate(forge,old);
            var identity=new net.minecraft.server.players.NameAndId(p.getGameProfile());server.getPlayerList().op(identity);
            try {
                var stand=net.minecraft.world.entity.EntityTypes.ARMOR_STAND.create(p.level(),net.minecraft.world.entity.EntitySpawnReason.COMMAND);stand.snapTo(p.getX()+2,p.getY(),p.getZ(),0,0);p.level().addFreshEntity(stand);
                var action=new CompoundTag();action.putInt("entity",stand.getId());action.putString("action","equip");action.putInt("slot",0);action.putInt("inventory",4);ArmorStandEditor.action(p,action);check(stand.getMainHandItem().is(p.getInventory().getItem(4).getItem()),"stand equips chosen inventory item");
                action.putString("action","preset");action.putInt("preset",1);ArmorStandEditor.action(p,action);check(stand.getRightArmPose().x()==-105,"stand action pose applied");
                action.putString("action","move");action.putInt("axis",1);action.putInt("delta",1);double y=stand.getY();ArmorStandEditor.action(p,action);check(Math.abs(stand.getY()-y-.1)<.001,"stand position adjustable");stand.discard();
            }finally{server.getPlayerList().deop(identity);}
            d.playerClass=PlayerClass.GUERRIER;d.level=80;d.xp=999;p.setItemSlot(EquipmentSlot.MAINHAND,new ItemStack(ModItems.get("orbe_renaissance")));
            p.getMainHandItem().use(p.level(),p,net.minecraft.world.InteractionHand.MAIN_HAND);check(d.level==1&&d.xp==0&&d.playerClass==PlayerClass.NONE,"rebirth resets class and level");
            var world=com.mmorpg.competition.CompetitionWorld.level(server);var center=new BlockPos(500,100,4000);var cave=com.mmorpg.competition.CompetitionWorld.raid(center,0);while(!cave.done())cave.tick(world);
            check(!world.getBlockState(center.offset(0,43,0)).isAir(),"cave has solid ceiling");check(!world.getBlockState(center.offset(40,10,0)).isAir(),"cave has solid walls");check(world.getBlockState(center.above(5)).isAir(),"cave central combat space clear");
            DevHubGuideChecks.run(p);
            com.mmorpg.MMORPG.LOGGER.info("[CODEX CHECKS] NEW_RULES PASS: stations, retired sources/inventory, short cinematic, bound paladin shield, four runes, armor stand equipment/pose/position, rebirth and solid cavern");
        } finally {com.mmorpg.network.NetworkConfig.current=cfg;}
    }
}
