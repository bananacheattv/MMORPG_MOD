package com.mmorpg.mine;

import com.mmorpg.network.Net;
import com.mmorpg.network.Payloads;
import com.mmorpg.registry.ModBlocks;
import com.mmorpg.registry.ModItems;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.server.ForgeManager;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * Presse de compression : seize lingots d'un minerai deviennent un minerai compresse (necessaire aux armures et aux outils) contre
 * quelques pieces d'or. Le niveau demande est celui d'extraction du minerai. Tout est verifie cote serveur.
 */
public final class Compression {
    private Compression() {
    }

    /** Lingots consommes par minerai compresse. */
    public static final int INGOTS = 16;
    private static final int[] GOLD = {15, 40, 100, 250, 600};

    public static int gold(int tier) {
        return GOLD[Math.min(tier, GOLD.length - 1)];
    }

    public static MineBlocks.Ore ore(String id) {
        for (MineBlocks.Ore o : MineBlocks.ORES) if (o.id().equals(id)) return o;
        return null;
    }

    public static void compress(ServerPlayer player, BlockPos pos, int times, String oreId) {
        if (!player.level().getBlockState(pos).is(ModBlocks.PRESSE_COMPRESSION.get()) || player.distanceToSqr(Vec3.atCenterOf(pos)) >= 64) return;
        MineBlocks.Ore ore = ore(oreId);
        if (ore == null) return;
        PlayerData d = RpgPlayers.get(player);
        if (d.level < ore.minLevel()) {
            player.sendOverlayMessage(Component.literal("Niveau " + ore.minLevel() + " requis pour comprimer la " + ore.name() + ".").withColor(0xFF5050));
            return;
        }
        Item ingot = ModItems.get(ore.ingotId());
        Item out = ModItems.get("compresse_" + ore.id());
        Inventory inv = player.getInventory();
        RpgPlayers.depositCoins(player);
        int made = 0;
        for (int t = 0; t < Math.max(1, Math.min(times, 16)); t++) {
            if (ForgeManager.count(inv, ingot) < INGOTS || RpgPlayers.get(player).gold < gold(ore.tier())) break;
            int need = INGOTS;
            for (int i = 0; i < inv.getContainerSize() && need > 0; i++) {
                ItemStack s = inv.getItem(i);
                if (!s.is(ingot)) continue;
                int take = Math.min(need, s.getCount());
                s.shrink(take);
                need -= take;
            }
            RpgPlayers.takeGold(player, gold(ore.tier()));
            ItemStack result = new ItemStack(out);
            if (!inv.add(result)) player.drop(result, false, net.minecraft.util.Prediction.SERVER_ONLY);
            made++;
        }
        if (made == 0) {
            player.sendOverlayMessage(Component.literal("Il faut " + INGOTS + " lingots de " + ore.name() + " et " + gold(ore.tier()) + " pièces d'or par minerai compressé.").withColor(0xFF7060));
            return;
        }
        ServerLevel level = player.level();
        level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.8F, 0.6F);
        level.playSound(null, pos, SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.BLOCKS, 1.0F, 0.8F);
        level.sendParticles(ParticleTypes.CRIT, pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5, 24, 0.4, 0.2, 0.4, 0.3);
        level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 1.3, pos.getZ() + 0.5, 10, 0.4, 0.2, 0.4, 0.05);
        Net.toPlayer(player, new Payloads.Notify(Payloads.Notify.LOOT, "COMPRESSION RÉUSSIE", made + " × " + new ItemStack(out).getHoverName().getString(), ore.color() | 0xFF000000));
    }
}
