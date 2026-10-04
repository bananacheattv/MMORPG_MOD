package com.mmorpg.block.structure;

import com.mmorpg.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Entite du bloc fonctionnel d'une structure multibloc : sert au rendu du modele 3D (etat d'animation cote client). */
public class StructureBlockEntity extends BlockEntity {
    /** Client : etat actif vu a la derniere image et instant (s) du dernier changement, pour l'animation d'activation. */
    public boolean clientActive;
    public float clientActiveSince = -100.0F;

    public StructureBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.STRUCTURE.get(), pos, state);
    }
}
