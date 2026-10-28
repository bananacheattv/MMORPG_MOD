package com.mmorpg.leaderboard;

import com.mmorpg.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Lignes affichees par un panneau de classement (enregistrees et synchronisees aux clients). */
public class LeaderboardBlockEntity extends BlockEntity {
    /** Panneaux charges cote serveur, mis a jour quand le classement change. */
    private static final Set<LeaderboardBlockEntity> LOADED = new HashSet<>();

    public final List<Leaderboards.Row> rows = new ArrayList<>();

    public LeaderboardBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.LEADERBOARD.get(), pos, state);
    }

    public Board board() {
        return getBlockState().getBlock() instanceof LeaderboardBlock b ? b.board : Board.NIVEAU;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide()) {
            synchronized (LOADED) {
                LOADED.add(this);
            }
            refresh();
        }
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        synchronized (LOADED) {
            LOADED.remove(this);
        }
    }

    /** Recopie le classement courant ; synchronise les clients s'il a change. */
    public void refresh() {
        List<Leaderboards.Row> top = Leaderboards.top(board());
        List<Leaderboards.Row> shown = top.size() > Board.ROWS ? top.subList(0, Board.ROWS) : top;
        if (shown.equals(rows)) return;
        rows.clear();
        rows.addAll(shown);
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    public static void refreshAll() {
        List<LeaderboardBlockEntity> all;
        synchronized (LOADED) {
            all = new ArrayList<>(LOADED);
        }
        for (LeaderboardBlockEntity be : all) be.refresh();
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        StringBuilder sb = new StringBuilder();
        for (Leaderboards.Row r : rows) sb.append(r.name()).append('\t').append(r.value()).append('\n');
        out.putString("rows", sb.toString());
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        rows.clear();
        for (String line : in.getStringOr("rows", "").split("\n")) {
            int tab = line.lastIndexOf('\t');
            if (tab <= 0) continue;
            try {
                rows.add(new Leaderboards.Row(line.substring(0, tab), Long.parseLong(line.substring(tab + 1))));
            } catch (NumberFormatException ignored) {
            }
        }
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
