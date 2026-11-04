package com.mmorpg.block.chest;

import com.mmorpg.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Coffre de donjon : conteneur vanilla (27 cases) dont l'ouverture/fermeture est datee cote client pour l'animation de l'etoile. */
public class DungeonChestBlockEntity extends ChestBlockEntity {
    public boolean open;
    public long openedAt = -1000, closedAt = -1000;

    public DungeonChestBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.DUNGEON_CHEST_ENTITY.get(), pos, state);
    }

    @Override
    public boolean triggerEvent(int id, int data) {
        if (id == 1 && level != null) {
            boolean now = data > 0;
            if (now && !open) openedAt = level.getGameTime();
            if (!now && open) closedAt = level.getGameTime();
            open = now;
        }
        return super.triggerEvent(id, data);
    }
}
