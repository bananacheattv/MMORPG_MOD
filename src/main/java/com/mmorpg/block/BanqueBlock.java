package com.mmorpg.block;

import com.mmorpg.block.structure.MultiblockKind;
import com.mmorpg.block.structure.MultiblockMasterBlock;
import com.mmorpg.block.structure.Multiblocks;
import com.mmorpg.economy.Bank;
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

/** Banque d'Aeloria : comptoir de chambre forte (modele art/structures_blockbench/banque_aeloria.py) ; ouvre l'interface bancaire. */
public class BanqueBlock extends MultiblockMasterBlock {
    public BanqueBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MultiblockKind kind() {
        return MultiblockKind.BANQUE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer sp) Bank.open(sp, pos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) != 0) return;
        Vec3 p = Multiblocks.world(pos, state.getValue(FACING), (random.nextDouble() - 0.5) * 60, 22 + random.nextDouble() * 10, -6 + random.nextDouble() * 8);
        level.addParticle(ParticleTypes.GLOW, p.x, p.y, p.z, 0, 0.01, 0);
    }
}
