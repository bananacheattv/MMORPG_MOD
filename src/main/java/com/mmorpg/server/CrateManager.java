package com.mmorpg.server;

import com.mmorpg.block.crate.CrateBlock;
import com.mmorpg.block.crate.CrateTier;
import com.mmorpg.crafting.ForgeRecipes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.state.BlockState;

/** Ouverture des caisses en bloc : la cle est consommee et le butin tire cote serveur (aucun choix client). */
public final class CrateManager {
    private CrateManager() {
    }

    public static void use(ServerPlayer p, BlockPos pos, BlockState state, CrateTier tier) {
        if (!p.isAlive() || p.isSpectator()) return;
        if (p.isShiftKeyDown()) {
            preview(p, tier);
            return;
        }
        if (state.getValue(CrateBlock.OPEN)) return; // ouverture en cours
        ItemStack key = findKey(p, tier);
        if (key.isEmpty()) {
            p.sendOverlayMessage(Component.literal("Il vous faut une " + tier.keyLabel + ". (Accroupi + clic : voir le contenu)").withColor(0xFF6060));
            p.level().playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 0.8f, 1f);
            return;
        }
        if (!p.hasInfiniteMaterials()) key.shrink(1);
        AdventureLoot.Loot loot = AdventureLoot.roll(p, tier.table());
        AdventureLoot.grant(p, loot, tier.label, tier.color);
        ServerLevel level = (ServerLevel) p.level();
        level.setBlockAndUpdate(pos, state.setValue(CrateBlock.OPEN, true));
        level.scheduleTick(pos, state.getBlock(), 40);
        level.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1f, 0.9f);
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.5f, 1.4f);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, pos.getX() + .5, pos.getY() + 1.1, pos.getZ() + .5, 30, .3, .4, .3, .25);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + .5, pos.getY() + 1, pos.getZ() + .5, 12, .25, .3, .25, .05);
    }

    private static ItemStack findKey(ServerPlayer p, CrateTier tier) {
        Item key = ForgeRecipes.resolveItem("mmorpg:" + tier.keyItem());
        // l'ancienne cle d'aventure ouvre aussi la caisse commune
        Item legacy = tier == CrateTier.COMMUNE ? ForgeRecipes.resolveItem("mmorpg:cle_aventure") : null;
        if (p.getMainHandItem().is(key)) return p.getMainHandItem();
        for (ItemStack s : p.getInventory()) {
            if (!s.isEmpty() && (s.is(key) || (legacy != null && s.is(legacy)))) return s;
        }
        return ItemStack.EMPTY;
    }

    private static void preview(ServerPlayer p, CrateTier tier) {
        p.sendSystemMessage(Component.literal("✦ " + tier.label + " — contenu possible :").withColor(tier.color));
        for (var t : AdventureLoot.preview(tier.table())) {
            CompoundTag e = (CompoundTag) t;
            boolean gold = e.getBooleanOr("gold", false);
            Item item = ForgeRecipes.resolveItem(e.getStringOr("item", ""));
            String name = gold ? "Pièces d’or" : item == null || item == Items.AIR ? e.getStringOr("item", "?") : new ItemStack(item).getHoverName().getString();
            int min = e.getIntOr("min", 1), max = e.getIntOr("max", 1);
            float pct = e.getIntOr("permille", 0) / 10f;
            p.sendSystemMessage(Component.literal("  • " + name + " ×" + (min == max ? min : min + "-" + max)
                    + "  (" + (pct < 1 ? String.format(java.util.Locale.ROOT, "%.1f", pct) : Math.round(pct)) + " %)").withColor(0xD8D0C0));
        }
    }
}
