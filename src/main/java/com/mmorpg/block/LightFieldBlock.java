package com.mmorpg.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Relais de lumiere invisible, sans collision, pose et retire par {@link com.mmorpg.server.LightNetwork}. */
public final class LightFieldBlock extends Block {
    public LightFieldBlock(Properties p) {
        super(p); registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.LEVEL, 15));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(BlockStateProperties.LEVEL); }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s, net.minecraft.world.level.BlockGetter l, net.minecraft.core.BlockPos p, net.minecraft.world.phys.shapes.CollisionContext c) { return net.minecraft.world.phys.shapes.Shapes.empty(); }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getVisualShape(BlockState s, net.minecraft.world.level.BlockGetter l, net.minecraft.core.BlockPos p, net.minecraft.world.phys.shapes.CollisionContext c) { return net.minecraft.world.phys.shapes.Shapes.empty(); }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.INVISIBLE; }
}
