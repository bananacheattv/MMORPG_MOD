package com.mmorpg.item;

import com.mmorpg.crafting.*;
import com.mmorpg.rpg.Rarity;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** One named recipe per parchment; old tier parchments exchange for a specific plan. */
public final class ForgeScrollItem extends RpgMaterialItem {
    private final String recipe;private final int tier;
    public ForgeScrollItem(Properties properties,String recipe,int tier) {
        super(properties,Rarity.values()[Math.clamp(tier,1,5)],"Plan d'équipement","Clic droit : apprend uniquement la recette de cet équipement. Boss ou quêtes. Les doublons restent échangeables.");
        this.recipe=recipe;this.tier=tier;
    }
    public ForgeScrollItem(Properties properties,int tier) {this(properties,"",tier);}
    public String recipe() {return recipe;}
    @Override public Component getName(ItemStack stack) {
        if(recipe.isEmpty())return Component.literal("Ancien parchemin : échanger contre un plan").withColor(rarity.rgb());
        var r=ForgeRecipes.get(recipe);return Component.literal("Parchemin : ").append(new ItemStack(r.resultItem()).getHoverName()).withColor(rarity.rgb());
    }
    @Override public InteractionResult use(Level level,Player player,InteractionHand hand) {
        if(!(player instanceof ServerPlayer p))return InteractionResult.SUCCESS;
        if(com.mmorpg.hub.ClassTrial.active(p))return InteractionResult.FAIL;
        if(recipe.isEmpty()) {
            String id=ForgeProgression.reward(RpgPlayers.get(p),tier,new java.util.Random());p.getItemInHand(hand).shrink(1);RpgPlayers.give(p,com.mmorpg.registry.ModItems.get(id),1);return InteractionResult.SUCCESS;
        }
        if(!ForgeProgression.learn(RpgPlayers.get(p),recipe)) {p.sendOverlayMessage(Component.literal("Vous connaissez déjà ce plan."));return InteractionResult.FAIL;}
        p.getItemInHand(hand).shrink(1);RpgPlayers.sync(p);
        p.sendSystemMessage(Component.literal("Recette apprise : ").append(new ItemStack(ForgeRecipes.get(recipe).resultItem()).getHoverName()).withColor(0xFFD060));
        p.playSound(com.mmorpg.registry.ModSounds.CHIME.get(),.8f,1);return InteractionResult.SUCCESS;
    }
}
