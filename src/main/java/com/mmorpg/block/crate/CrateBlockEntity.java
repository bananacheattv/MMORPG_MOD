package com.mmorpg.block.crate;

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
import java.util.List;
import java.util.UUID;

/**
 * Etat d'ouverture d'une caisse, synchronise aux clients pour la roue animee au-dessus du bloc :
 * liste d'objets qui defilent, index du gain (la roue s'arrete dessus) et instant de depart.
 */
public class CrateBlockEntity extends BlockEntity {
    public static final int SPIN_TICKS = 80;
    public static final int SHOW_TICKS = 50;

    public final List<String> reel = new ArrayList<>();
    public int resultIndex;
    public long startTick = -1000;
    public String winnerName = "";
    /** Serveur : joueur a annoncer en fin d'animation (le butin est deja verse). */
    public UUID opener;
    public String announce = "";

    public CrateBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.CRATE.get(), pos, state);
    }

    public boolean spinning(long now) {
        return now - startTick < SPIN_TICKS + SHOW_TICKS;
    }

    public void start(List<String> items, int result, long now, UUID player, String winner, String message) {
        reel.clear();
        reel.addAll(items);
        resultIndex = result;
        startTick = now;
        opener = player;
        winnerName = winner;
        announce = message;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        out.putString("reel", String.join(";", reel));
        out.putInt("result", resultIndex);
        out.putLong("start", startTick);
        out.putString("winner", winnerName);
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        reel.clear();
        String r = in.getStringOr("reel", "");
        if (!r.isEmpty()) reel.addAll(List.of(r.split(";")));
        resultIndex = in.getIntOr("result", 0);
        startTick = in.getLongOr("start", -1000);
        winnerName = in.getStringOr("winner", "");
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
