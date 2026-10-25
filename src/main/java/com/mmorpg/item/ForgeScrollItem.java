package com.mmorpg.item;

import com.mmorpg.crafting.ForgeProgression;
import com.mmorpg.rpg.Rarity;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/** Physical recipe license: consumed only when it teaches a new tier. */
public final class ForgeScrollItem extends RpgMaterialItem {
    private final int tier;
    public ForgeScrollItem(Properties properties, int tier) {
        super(properties, Rarity.values()[Math.clamp(tier, 1, 5)], "Parchemin de forge",
                "Clic droit : apprend définitivement les recettes d'armes, d'armures et d'outils du palier " + tier
                        + " (niveaux " + ((tier - 1) * 20 + 1) + "–" + (tier * 20) + ").\nObtenu sur les boss ou par les quêtes. Un doublon n'est pas consommé.");
        this.tier = tier;
    }
    @Override public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(player instanceof ServerPlayer p)) return InteractionResult.SUCCESS;
        if (com.mmorpg.hub.ClassTrial.active(p)) return InteractionResult.FAIL;
        if (!ForgeProgression.learn(RpgPlayers.get(p), tier)) {
            p.sendOverlayMessage(Component.literal("Vous connaissez déjà ce parchemin."));
            return InteractionResult.FAIL;
        }
        p.getItemInHand(hand).shrink(1);
        RpgPlayers.sync(p);
        p.sendSystemMessage(Component.literal("Parchemin appris : recettes de forge du palier " + tier + " débloquées !").withColor(0xFFD060));
        p.playSound(com.mmorpg.registry.ModSounds.CHIME.get(), 0.8f, 1f);
        return InteractionResult.SUCCESS;
    }
}
