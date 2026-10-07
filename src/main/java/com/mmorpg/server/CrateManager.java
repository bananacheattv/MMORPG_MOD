package com.mmorpg.server;

import com.mmorpg.block.crate.CrateBlock;
import com.mmorpg.block.crate.CrateBlockEntity;
import com.mmorpg.block.crate.CrateTier;
import com.mmorpg.config.ConfigManager;
import com.mmorpg.config.LootTables;
import com.mmorpg.crafting.ForgeRecipes;
import com.mmorpg.network.Net;
import com.mmorpg.network.S2COpenScreen;
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

import java.util.ArrayList;
import java.util.List;

/**
 * Caisses en bloc : clic droit = interface de la caisse ; « Ouvrir » consomme une cle, le butin est tire et verse
 * cote serveur tout de suite (rien ne se perd), puis une roue d'objets tourne au-dessus de la caisse et s'arrete
 * sur le gain, annonce en fin d'animation.
 */
public final class CrateManager {
    private static final int REEL = 40, RESULT_AT = 34;

    private CrateManager() {
    }

    private static CrateBlockEntity crate(ServerPlayer p, BlockPos pos) {
        if (!p.isAlive() || p.isSpectator() || p.blockPosition().distSqr(pos) > 64 || !p.level().hasChunkAt(pos)) return null;
        return p.level().getBlockEntity(pos) instanceof CrateBlockEntity be ? be : null;
    }

    public static void showScreen(ServerPlayer p, BlockPos pos, CrateTier tier) {
        if (crate(p, pos) == null) return;
        CompoundTag data = new CompoundTag();
        data.putLong("pos", pos.asLong());
        data.putInt("tier", tier.ordinal());
        data.put("table", AdventureLoot.preview(tier.table()));
        data.putInt("keys", countKeys(p, tier));
        Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.CRATE, data));
    }

    public static void open(ServerPlayer p, BlockPos pos) {
        CrateBlockEntity be = crate(p, pos);
        if (be == null || !(be.getBlockState().getBlock() instanceof CrateBlock block)) return;
        CrateTier tier = block.tier;
        ServerLevel level = (ServerLevel) p.level();
        long now = level.getGameTime();
        if (be.spinning(now)) {
            p.sendOverlayMessage(Component.literal("La caisse est déjà en cours d’ouverture…").withColor(0xFFC060));
            return;
        }
        ItemStack key = findKey(p, tier);
        if (key.isEmpty()) {
            p.sendOverlayMessage(Component.literal("Il vous faut une " + tier.keyLabel + ".").withColor(0xFF6060));
            level.playSound(null, pos, SoundEvents.CHEST_LOCKED, SoundSource.BLOCKS, 0.8f, 1f);
            return;
        }
        if (!p.hasInfiniteMaterials()) key.shrink(1);
        AdventureLoot.Loot loot = AdventureLoot.roll(p, tier.table());
        AdventureLoot.give(p, loot);                       // verse tout de suite, annonce a la fin de la roue
        // roue : objets de la table au hasard (selon leur poids), le gain a l'index RESULT_AT
        List<String> reel = new ArrayList<>();
        List<LootTables.Entry> entries = ConfigManager.loot().table(tier.table());
        for (int i = 0; i < REEL; i++) {
            String id = i == RESULT_AT ? loot.item() : entries.isEmpty() ? "mmorpg:piece_or" : weighted(p, entries);
            reel.add(id);
        }
        String what = loot.gold() > 0 ? loot.gold() + " pièces d’or" : loot.count() + " × " + loot.category();
        be.start(reel, RESULT_AT, now, p.getUUID(), what, what);
        BlockState state = be.getBlockState();
        level.setBlockAndUpdate(pos, state.setValue(CrateBlock.OPEN, true));
        level.scheduleTick(pos, state.getBlock(), CrateBlockEntity.SPIN_TICKS);
        level.playSound(null, pos, SoundEvents.CHEST_OPEN, SoundSource.BLOCKS, 1f, 0.8f);
        level.playSound(null, pos, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 0.6f, 1.6f);
        level.sendParticles(ParticleTypes.END_ROD, pos.getX() + .5, pos.getY() + 1.6, pos.getZ() + .5, 20, .4, .3, .4, .03);
    }

    private static String weighted(ServerPlayer p, List<LootTables.Entry> entries) {
        int total = 0;
        for (LootTables.Entry e : entries) total += e.weight;
        if (total <= 0) return "mmorpg:piece_or";
        int r = p.getRandom().nextInt(total);
        for (LootTables.Entry e : entries) {
            r -= e.weight;
            if (r < 0) return e.isGold() ? "mmorpg:piece_or" : e.item;
        }
        return "mmorpg:piece_or";
    }

    /** Tick programme : fin de la roue (annonce + effets), puis fermeture du couvercle. */
    public static void tick(ServerLevel level, BlockPos pos, BlockState state, CrateTier tier) {
        if (!(level.getBlockEntity(pos) instanceof CrateBlockEntity be)) return;
        long elapsed = level.getGameTime() - be.startTick;
        if (elapsed < CrateBlockEntity.SPIN_TICKS + CrateBlockEntity.SHOW_TICKS - 2) {
            if (be.opener != null && level.getServer().getPlayerList().getPlayer(be.opener) instanceof ServerPlayer p && !be.announce.isEmpty()) {
                p.sendSystemMessage(Component.literal("✦ " + tier.label + " : " + be.announce).withColor(tier.color));
            }
            be.announce = "";
            level.playSound(null, pos, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 0.9f, 1.2f);
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, pos.getX() + .5, pos.getY() + 2.2, pos.getZ() + .5, 40, .4, .4, .4, .3);
            level.sendParticles(ParticleTypes.FIREWORK, pos.getX() + .5, pos.getY() + 2.2, pos.getZ() + .5, 20, .3, .3, .3, .1);
            level.scheduleTick(pos, state.getBlock(), CrateBlockEntity.SHOW_TICKS);
        } else if (state.getValue(CrateBlock.OPEN)) {
            level.setBlockAndUpdate(pos, state.setValue(CrateBlock.OPEN, false));
            level.playSound(null, pos, SoundEvents.CHEST_CLOSE, SoundSource.BLOCKS, 0.8f, 0.8f);
        }
    }

    private static int countKeys(ServerPlayer p, CrateTier tier) {
        int n = 0;
        Item key = ForgeRecipes.resolveItem("mmorpg:" + tier.keyItem());
        Item legacy = tier == CrateTier.COMMUNE ? ForgeRecipes.resolveItem("mmorpg:cle_aventure") : null;
        for (ItemStack s : p.getInventory()) if (!s.isEmpty() && (s.is(key) || (legacy != null && s.is(legacy)))) n += s.getCount();
        return n;
    }

    private static ItemStack findKey(ServerPlayer p, CrateTier tier) {
        Item key = ForgeRecipes.resolveItem("mmorpg:" + tier.keyItem());
        Item legacy = tier == CrateTier.COMMUNE ? ForgeRecipes.resolveItem("mmorpg:cle_aventure") : null;
        if (key == null || key == Items.AIR) return ItemStack.EMPTY;
        for (ItemStack s : p.getInventory()) {
            if (!s.isEmpty() && (s.is(key) || (legacy != null && s.is(legacy)))) return s;
        }
        return ItemStack.EMPTY;
    }
}
