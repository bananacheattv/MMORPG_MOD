package com.mmorpg.block;

import com.mmorpg.block.structure.MultiblockKind;
import com.mmorpg.block.structure.MultiblockMasterBlock;
import com.mmorpg.economy.Mail;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Boite aux lettres d'Aeloria (modele art/structures_blockbench/boite_aux_lettres.py) : ouvre le courrier du joueur. */
public class BoiteAuxLettresBlock extends MultiblockMasterBlock {
    public BoiteAuxLettresBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MultiblockKind kind() {
        return MultiblockKind.BOITE_AUX_LETTRES;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer sp) Mail.open(sp, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(10) != 0) return;
        level.addParticle(ParticleTypes.GLOW, pos.getX() + 0.5, pos.getY() + 3.2, pos.getZ() + 0.5, 0, 0.01, 0);
    }
}
