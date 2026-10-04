package com.mmorpg.block;

import com.mmorpg.block.structure.MultiblockKind;
import com.mmorpg.block.structure.MultiblockMasterBlock;
import com.mmorpg.block.structure.Multiblocks;
import com.mmorpg.network.Net;
import com.mmorpg.network.S2COpenScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Forge Arcanique : atelier multibloc (four, enclume, cristal, etabli ; modele art/structures_blockbench).
 * Le bloc fonctionnel est le socle de l'enclume ; il ouvre l'interface d'artisanat et d'amelioration.
 * La forge s'allume quand un joueur s'en approche.
 */
public class ForgeBlock extends MultiblockMasterBlock {
    public ForgeBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MultiblockKind kind() {
        return MultiblockKind.FORGE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer sp) {
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.4f, 1.3f);
            Net.toPlayer(sp, S2COpenScreen.forge(pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Direction f = state.getValue(FACING);
        boolean lit = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, false) != null;
        if (lit) {
            Vec3 hearth = Multiblocks.world(pos, f, 23 + (random.nextDouble() - 0.5) * 12, 16, 16 + (random.nextDouble() - 0.5) * 8);
            if (random.nextInt(2) == 0) level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, hearth.x, hearth.y, hearth.z, 0, 0.02, 0);
            if (random.nextInt(4) == 0) level.addParticle(ParticleTypes.LAVA, hearth.x, hearth.y, hearth.z, 0, 0, 0);
            Vec3 chimney = Multiblocks.world(pos, f, 25, 72, 23);
            if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.LARGE_SMOKE, chimney.x, chimney.y, chimney.z, 0, 0.05, 0);
            Vec3 crystal = Multiblocks.world(pos, f, -4, 24 + random.nextDouble() * 16, 33);
            if (random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.ENCHANT, crystal.x, crystal.y, crystal.z, (random.nextDouble() - 0.5) * 1.2, -0.4, (random.nextDouble() - 0.5) * 1.2);
            }
        } else if (random.nextInt(6) == 0) {
            Vec3 coals = Multiblocks.world(pos, f, 23, 14, 18);
            level.addParticle(ParticleTypes.SMOKE, coals.x, coals.y, coals.z, 0, 0.01, 0);
        }
    }
}
