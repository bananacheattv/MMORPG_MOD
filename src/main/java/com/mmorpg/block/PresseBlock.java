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
 * Presse de compression : machine multibloc des Mines d'Aeloria (modele art/structures_blockbench/presse_compression.py).
 * Le bloc fonctionnel est le socle de la presse ; il ouvre l'interface qui transforme seize lingots en un minerai compresse.
 * La presse se met en marche quand un joueur s'en approche.
 */
public class PresseBlock extends MultiblockMasterBlock {
    public PresseBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MultiblockKind kind() {
        return MultiblockKind.PRESSE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer sp) {
            level.playSound(null, pos, SoundEvents.PISTON_EXTEND, SoundSource.BLOCKS, 0.5f, 0.8f);
            Net.toPlayer(sp, S2COpenScreen.compressor(pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Direction f = state.getValue(FACING);
        boolean on = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 8, false) != null;
        if (on) {
            // vapeur sous le piston et lueur du cristal
            Vec3 steam = Multiblocks.world(pos, f, (random.nextDouble() - 0.5) * 16, 22, (random.nextDouble() - 0.5) * 16);
            if (random.nextInt(3) == 0) level.addParticle(ParticleTypes.CLOUD, steam.x, steam.y, steam.z, 0, 0.03, 0);
            Vec3 crystal = Multiblocks.world(pos, f, 0, 14 + random.nextDouble() * 12, 26);
            if (random.nextInt(3) == 0) {
                level.addParticle(ParticleTypes.END_ROD, crystal.x, crystal.y, crystal.z, (random.nextDouble() - 0.5) * 0.04, 0.02, (random.nextDouble() - 0.5) * 0.04);
            }
        } else if (random.nextInt(8) == 0) {
            Vec3 vent = Multiblocks.world(pos, f, 8, 20, 0);
            level.addParticle(ParticleTypes.SMOKE, vent.x, vent.y, vent.z, 0, 0.01, 0);
        }
    }
}
