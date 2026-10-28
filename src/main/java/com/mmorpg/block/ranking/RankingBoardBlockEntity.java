package com.mmorpg.block.ranking;

import com.mmorpg.ranking.Rankings;
import com.mmorpg.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import java.util.ArrayList;
import java.util.List;

/** Lignes « nom<TAB>valeur » du top affiché, rafraîchies côté serveur toutes les 10 s et synchronisées aux clients. */
public class RankingBoardBlockEntity extends BlockEntity {
    public final List<String> lines = new ArrayList<>();

    public RankingBoardBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.RANKING_BOARD.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, RankingBoardBlockEntity be) {
        if (level.getGameTime() % 200 != (pos.asLong() & 63)) return;
        if (!(state.getBlock() instanceof RankingBoardBlock block)) return;
        var fresh = Rankings.lines(block.kind);
        if (fresh.size() > Rankings.TOP) fresh = fresh.subList(0, Rankings.TOP);
        if (fresh.equals(be.lines)) return;
        be.lines.clear();
        be.lines.addAll(fresh);
        be.setChanged();
        level.sendBlockUpdated(pos, state, state, 3);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putString("lines", String.join("\n", lines));
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        lines.clear();
        String s = in.getStringOr("lines", "");
        if (!s.isEmpty()) lines.addAll(List.of(s.split("\n")));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
