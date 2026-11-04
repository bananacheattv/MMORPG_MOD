package com.mmorpg.block.chest;

import com.mmorpg.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Remplace le coffre vanilla dans les donjons : meme comportement (inventaire 3 rangees), rendu 3D anime avec etoile. */
public class DungeonChestBlock extends ChestBlock {
    public DungeonChestBlock(Properties properties) {
        super(() -> ModBlocks.DUNGEON_CHEST_ENTITY.get(), SoundEvents.CHEST_OPEN, SoundEvents.CHEST_CLOSE, properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DungeonChestBlockEntity(pos, state);
    }

    /** Jamais de double coffre : le modele est unique. */
    @Override
    public boolean chestCanConnectTo(BlockState state) {
        return false;
    }
}
