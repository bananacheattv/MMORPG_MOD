package com.mmorpg.block;

import com.mmorpg.registry.DecorBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Decor dessine par un modele Blockbench (drapeau, camp, decors de la citadelle) : le rendu est fait par un moteur de blocs-entites.
 * Un modele plus grand qu'un bloc pose des parties invisibles sur son emprise : toute sa forme a une collision (DecorShapes).
 * Les cellules deja occupees sont laissees telles quelles, le decor reste posable contre un mur.
 */
public class DecorModelBlock extends Block implements EntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final VoxelShape shape;
    private final @Nullable String modelId;

    public DecorModelBlock(Properties properties, VoxelShape shape) {
        this(properties, shape, null);
    }

    public DecorModelBlock(Properties properties, VoxelShape shape, @Nullable String modelId) {
        super(properties);
        this.shape = shape;
        this.modelId = modelId;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public @Nullable String modelId() {
        return modelId;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (level.isClientSide() || oldState.is(state.getBlock()) || !DecorShapes.has(modelId)) return;
        Direction facing = state.getValue(FACING);
        Block part = DecorBlocks.PART.get();
        for (BlockPos cell : DecorShapes.cells(modelId)) {
            if (cell.equals(BlockPos.ZERO)) continue;
            BlockPos offset = DecorShapes.toWorld(cell, facing);
            BlockPos at = pos.offset(offset);
            if (!level.isOutsideBuildHeight(at) && level.getBlockState(at).canBeReplaced()) {
                level.setBlock(at, DecorPartBlock.stateFor(part, offset), 3);
            }
        }
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        if (level.getBlockState(pos).is(state.getBlock()) || !DecorShapes.has(modelId)) return;
        Direction facing = state.getValue(FACING);
        for (BlockPos cell : DecorShapes.cells(modelId)) {
            if (cell.equals(BlockPos.ZERO)) continue;
            BlockPos offset = DecorShapes.toWorld(cell, facing);
            BlockPos at = pos.offset(offset);
            BlockState there = level.getBlockState(at);
            if (there.getBlock() instanceof DecorPartBlock && DecorPartBlock.offset(there).equals(offset)) level.removeBlock(at, false);
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (DecorShapes.has(modelId) && DecorShapes.hasCell(modelId, BlockPos.ZERO)) {
            return DecorShapes.shape(modelId, BlockPos.ZERO, state.getValue(FACING));
        }
        return shape;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new Entity(pos, state);
    }

    /** Bloc-entite sans donnees : porte seulement le rendu. */
    public static final class Entity extends BlockEntity {
        public Entity(BlockPos pos, BlockState state) {
            super(DecorBlocks.MODEL_ENTITY.get(), pos, state);
        }
    }
}
