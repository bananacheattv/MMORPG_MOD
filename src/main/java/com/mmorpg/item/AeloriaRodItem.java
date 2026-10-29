package com.mmorpg.item;

import com.mmorpg.fishing.FishCatalog;
import com.mmorpg.fishing.FishingRules;
import com.mmorpg.job.Job;
import com.mmorpg.job.JobSystem;
import com.mmorpg.rpg.ClientView;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

import java.util.function.Consumer;

/**
 * Canne d'Aeloria : six rangs lies au niveau du metier de pecheur. Le rang fixe l'attente avant la touche (appat), la chance, la taille de
 * la zone du mini-jeu et la rarete maximale des prises (voir {@link FishCatalog.Rod}).
 */
public class AeloriaRodItem extends FishingRodItem {
    private final FishCatalog.Rod rod;

    public AeloriaRodItem(Properties properties, FishCatalog.Rod rod) {
        super(properties);
        this.rod = rod;
    }

    public FishCatalog.Rod rod() {
        return rod;
    }

    /** Niveaux d'appat total (canne + metier), plafonnes pour que la touche reste un evenement. */
    public static int lureLevels(int rodLure, int jobLevel) {
        return Math.min(4, rodLure + FishingRules.jobLure(jobLevel));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player.fishing != null || !(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer sp)) return super.use(level, player, hand);
        // lancer : memes effets que la canne vanilla, mais appat et chance viennent du rang de la canne
        ItemStack stack = player.getItemInHand(hand);
        int jobLevel = JobSystem.level(sp, Job.PECHEUR);
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.NEUTRAL, 0.5F,
                0.4F / (level.getRandom().nextFloat() * 0.4F + 0.8F));
        Projectile.spawnProjectile(new FishingHook(player, level, rod.luck(), lureLevels(rod.lure(), jobLevel) * 100), serverLevel, stack);
        player.awardStat(Stats.ITEM_USED.get(this));
        stack.causeUseVibration(player, GameEvent.ITEM_INTERACT_START);
        return InteractionResult.SUCCESS;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable(this.getDescriptionId()).withColor(rod.rarity().rgb());
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        ItemTooltips.simple(rod.rarity(), "Canne à pêche", rod.lore(), out);
        boolean ok = ClientView.jobLevel.applyAsInt("pecheur") >= rod.minJobLevel();
        out.accept(Component.literal("Pêcheur niveau " + rod.minJobLevel()).withColor(ok ? 0x66E066 : 0xFF5555));
        out.accept(Component.literal("Touche : appat +" + rod.lure() + " · Chance +" + rod.luck()).withColor(0x7FD0F0));
        out.accept(Component.literal("Mini-jeu : zone x" + String.format(java.util.Locale.ROOT, "%.2f", rod.zone())).withColor(0x7FD0F0));
        out.accept(Component.literal("Rareté max : " + rarityName(rod.tier())).withColor(0x9A9488));
    }

    private static String rarityName(int tier) {
        com.mmorpg.rpg.Rarity best = com.mmorpg.rpg.Rarity.COMMUN;
        for (com.mmorpg.rpg.Rarity r : com.mmorpg.rpg.Rarity.values()) if (FishCatalog.rodTierFor(r) <= tier) best = r;
        return best.label;
    }
}
