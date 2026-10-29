package com.mmorpg.item;

import com.mmorpg.fishing.FishCatalog;
import com.mmorpg.fishing.TreasureLoot;
import com.mmorpg.job.JobSystem;
import com.mmorpg.registry.ModItems;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.List;

/** Coffre repeche : un clic droit l'ouvre et donne de l'or, des materiaux et parfois des objets rares (voir {@link TreasureLoot}). */
public class TreasureChestItem extends RpgMaterialItem {
    private final int rank;

    public TreasureChestItem(Properties properties, FishCatalog.Entry entry, int rank) {
        super(properties, entry.rarity(), "Trésor", entry.lore());
        this.rank = rank;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide() || !(player instanceof ServerPlayer p)) return InteractionResult.SUCCESS;
        ItemStack stack = p.getItemInHand(hand);
        java.util.Random rnd = new java.util.Random(p.getRandom().nextLong());
        long gold = TreasureLoot.gold(rank, rnd);
        List<TreasureLoot.Reward> rewards = TreasureLoot.roll(rank, rnd);
        stack.shrink(1);
        RpgPlayers.addGold(p, gold, true);
        p.sendSystemMessage(Component.literal("✦ " + Component.translatable(getDescriptionId()).getString() + " ouvert").withColor(rarity.rgb()));
        p.sendSystemMessage(Component.literal("   " + gold + " pièces d'or").withColor(0xE8C060));
        for (TreasureLoot.Reward r : rewards) {
            net.minecraft.world.item.Item item = ModItems.get(r.itemId());
            if (item == null) continue;
            ItemStack give = new ItemStack(item, r.count());
            JobSystem.give(p, give.copy());
            p.sendSystemMessage(Component.literal("   " + r.count() + " × ").withColor(0xB9ABCF).append(give.getHoverName()));
        }
        p.level().playSound(null, p.blockPosition(), SoundEvents.CHEST_OPEN, SoundSource.PLAYERS, 0.9F, 1.1F);
        p.level().playSound(null, p.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.6F);
        return InteractionResult.SUCCESS;
    }
}
