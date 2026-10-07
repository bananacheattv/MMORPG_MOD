package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.registry.ModBlocks;
import com.mmorpg.registry.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = MMORPG.MODID)
public final class DevLootChecks {
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!Boolean.getBoolean("mmorpg.checkLoot") || !(event.getEntity() instanceof ServerPlayer p)) return;
        var inv = p.getInventory(); var d = RpgPlayers.get(p); var data = d.save();
        var mode = p.gameMode.getGameModeForPlayer();
        ItemStack[] saved = new ItemStack[inv.getContainerSize()];
        for (int i = 0; i < saved.length; i++) saved[i] = inv.getItem(i).copy();
        var pos = p.blockPosition().offset(2, 0, 0); var block = p.level().getBlockState(pos);
        try {
            p.setGameMode(GameType.SURVIVAL);
            for (int i = 0; i < saved.length; i++) inv.setItem(i, ItemStack.EMPTY);
            var crate = new ItemStack(ModItems.CAISSE_AVENTURE.get(), 2); inv.setItem(0, crate);
            check(!AdventureLoot.openCrate(p, crate) && crate.getCount() == 2, "missing key");
            inv.setItem(1, new ItemStack(ModItems.CLE_AVENTURE.get()));
            check(AdventureLoot.openCrate(p, crate), "opening");
            check(crate.getCount() == 1 && inv.countItem(ModItems.CLE_AVENTURE.get()) == 0, "one key and one crate");
            check(!AdventureLoot.openCrate(p, crate) && crate.getCount() == 1, "repeat without key");
            int permille = 0;
            for (var t : AdventureLoot.preview(com.mmorpg.config.LootTables.CRATE)) permille += ((net.minecraft.nbt.CompoundTag) t).getIntOr("permille", 0);
            check(Math.abs(permille - 1000) <= 12, "reward weights");
            p.level().setBlockAndUpdate(pos, ModBlocks.LUCKY_BLOCK.get().defaultBlockState());
            check(p.gameMode.destroyBlock(pos), "lucky block break");
            check(p.level().getBlockState(pos).isAir(), "lucky block removed");
            check(inv.countItem(ModItems.LUCKY_BLOCK.get()) == 1, "unopened block recovered once");
            p.level().setBlockAndUpdate(pos, ModBlocks.LUCKY_BLOCK.get().defaultBlockState());
            check(LuckyBlockManager.roll(p, pos), "opening placed block");
            check(p.level().getBlockState(pos).isAir(), "opened block consumed");
            check(!LuckyBlockManager.roll(p, pos), "second opening denied");
            check(!LuckyBlockManager.roll(p, pos.offset(100, 0, 0)), "distant opening denied");
            MMORPG.LOGGER.info("[CODEX CHECKS] LOOT PASS: keys, weights, recovery, opening and duplicate denial");
        } finally {
            p.level().setBlockAndUpdate(pos, block);
            for (int i = 0; i < saved.length; i++) inv.setItem(i, saved[i]);
            d.load(data); d.dirty = true; p.setGameMode(mode);
        }
    }
    private static void check(boolean b, String label) { if (!b) throw new IllegalStateException("Loot check: " + label); }
}
