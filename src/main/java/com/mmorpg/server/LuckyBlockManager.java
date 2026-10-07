package com.mmorpg.server;

import com.mmorpg.network.Net;
import com.mmorpg.network.S2COpenScreen;
import com.mmorpg.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;

public final class LuckyBlockManager {
    private static boolean valid(ServerPlayer p, BlockPos pos) {
        return p.isAlive() && !p.isSpectator() && p.blockPosition().distSqr(pos) <= 64
                && p.level().hasChunkAt(pos) && p.level().getBlockState(pos).is(ModBlocks.LUCKY_BLOCK.get());
    }
    public static void open(ServerPlayer p, BlockPos pos) {
        if (!valid(p, pos)) return;
        var data = new CompoundTag(); data.putLong("pos", pos.asLong());
        data.put("table", AdventureLoot.preview(com.mmorpg.config.LootTables.LUCKY));
        Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.LUCKY_BLOCK, data));
    }
    public static boolean roll(ServerPlayer p, BlockPos pos) {
        var data = new CompoundTag(); data.putLong("pos", pos.asLong());
        if (!valid(p, pos) || !p.level().removeBlock(pos, false)) {
            data.putString("error", "Ce Lucky Block n’est plus disponible ou est trop éloigné.");
            Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.LUCKY_RESULT, data));
            return false;
        }
        // Paid immediately after consuming the block: closing or disconnecting cannot lose or reroll the reward.
        var loot = AdventureLoot.roll(p, com.mmorpg.config.LootTables.LUCKY);
        AdventureLoot.grant(p, loot);
        data.put("loot", loot.tag());
        Net.toPlayer(p, new S2COpenScreen(S2COpenScreen.LUCKY_RESULT, data));
        return true;
    }
}
