package com.mmorpg.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Partie invisible d'un decor 3D : porte la collision de sa cellule et renvoie la casse vers le bloc pose. Sans objet ni butin.
 * Proprietes : decalage dans le monde par rapport au bloc pose (x et z + 5).
 */
public class DecorPartBlock extends Block {
    public static final IntegerProperty OX = IntegerProperty.create("ox", 0, 10);
    public static final IntegerProperty OY = IntegerProperty.create("oy", 0, 12);
    public static final IntegerProperty OZ = IntegerProperty.create("oz", 0, 10);

    public DecorPartBlock(Properties properties) {
        super(properties.dynamicShape());
        registerDefaultState(stateDefinition.any().setValue(OX, 5).setValue(OY, 1).setValue(OZ, 5));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(OX, OY, OZ);
    }

    public static BlockState stateFor(Block part, BlockPos offset) {
        return part.defaultBlockState().setValue(OX, offset.getX() + 5).setValue(OY, offset.getY()).setValue(OZ, offset.getZ() + 5);
    }

    public static BlockPos offset(BlockState state) {
        return new BlockPos(state.getValue(OX) - 5, state.getValue(OY), state.getValue(OZ) - 5);
    }

    public static BlockPos masterPos(BlockPos pos, BlockState state) {
        return pos.subtract(offset(state));
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        BlockState master = level.getBlockState(masterPos(pos, state));
        if (!(master.getBlock() instanceof DecorModelBlock decor) || decor.modelId() == null) return Shapes.empty();
        var facing = master.getValue(DecorModelBlock.FACING);
        return DecorShapes.shape(decor.modelId(), DecorShapes.toCell(offset(state), facing), facing);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    @Override
    protected boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockPos m = masterPos(pos, state);
        BlockState master = level.getBlockState(m);
        return master.getBlock() instanceof DecorModelBlock ? master.getDestroyProgress(player, level, m) : 1F;
    }

    /** Casser une partie casse tout le decor (le bloc pose tombe en objet hors mode creatif). */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        BlockPos m = masterPos(pos, state);
        if (!level.isClientSide() && level.getBlockState(m).getBlock() instanceof DecorModelBlock) {
            level.destroyBlock(m, !player.preventsBlockDrops(), player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        BlockState master = level.getBlockState(masterPos(pos, state));
        return master.getBlock() instanceof DecorModelBlock ? new ItemStack(master.getBlock()) : ItemStack.EMPTY;
    }
}
