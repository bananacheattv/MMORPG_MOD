package com.mmorpg.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Accessoire de decor (modele Blockbench) : oriente vers le joueur, boite de collision tournee avec le bloc. */
public class DecorPropBlock extends Block {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final VoxelShape[] shapes = new VoxelShape[4];

    public DecorPropBlock(Properties properties, double x0, double y0, double z0, double x1, double y1, double z1) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
        // boite de reference orientee nord ; rotation de 90 degres autour du centre du bloc pour les autres sens
        double ax = x0, az = z0, bx = x1, bz = z1;
        for (int i = 0; i < 4; i++) {
            shapes[i] = Block.box(Math.min(ax, bx), y0, Math.min(az, bz), Math.max(ax, bx), y1, Math.max(az, bz));
            double nax = 16 - az, naz = ax, nbx = 16 - bz, nbz = bx;
            ax = nax; az = naz; bx = nbx; bz = nbz;
        }
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
    protected VoxelShape getShape(BlockState state, BlockGetter level, net.minecraft.core.BlockPos pos, CollisionContext context) {
        return shapes[(state.getValue(FACING).get2DDataValue() + 2) % 4];
    }
}
