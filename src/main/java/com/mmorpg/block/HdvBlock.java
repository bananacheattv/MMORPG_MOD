package com.mmorpg.block;

import com.mmorpg.block.structure.MultiblockKind;
import com.mmorpg.block.structure.MultiblockMasterBlock;
import com.mmorpg.block.structure.Multiblocks;
import com.mmorpg.economy.Auction;
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
import net.minecraft.world.phys.Vec3;

/** Hotel des ventes d'Aeloria : pupitre de commissaire-priseur (modele art/structures_blockbench/hdv_aeloria.py) ; ouvre les enchères. */
public class HdvBlock extends MultiblockMasterBlock {
    public HdvBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MultiblockKind kind() {
        return MultiblockKind.HDV;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer sp) Auction.open(sp, pos, "", 0);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) != 0) return;
        for (double x : new double[]{30, -30}) {
            Vec3 p = Multiblocks.world(pos, state.getValue(FACING), x, 38 + random.nextDouble() * 6, 8);
            level.addParticle(ParticleTypes.ENCHANT, p.x, p.y, p.z, (random.nextDouble() - 0.5) * 0.6, 0.3, (random.nextDouble() - 0.5) * 0.6);
        }
    }
}
