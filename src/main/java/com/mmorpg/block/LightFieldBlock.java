package com.mmorpg.block;

import com.mmorpg.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Ancien relais de lumiere, conserve uniquement pour se supprimer seul dans les mondes existants. */
public final class LightFieldBlock extends Block {
    public static final int SPACING = 16;
    public LightFieldBlock(Properties p) {
        super(p); registerDefaultState(stateDefinition.any().setValue(BlockStateProperties.LEVEL, 15));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(BlockStateProperties.LEVEL); }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState s, net.minecraft.world.level.BlockGetter l, BlockPos p, net.minecraft.world.phys.shapes.CollisionContext c) { return net.minecraft.world.phys.shapes.Shapes.empty(); }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getVisualShape(BlockState s, net.minecraft.world.level.BlockGetter l, BlockPos p, net.minecraft.world.phys.shapes.CollisionContext c) { return net.minecraft.world.phys.shapes.Shapes.empty(); }
    @Override protected RenderShape getRenderShape(BlockState s) { return RenderShape.INVISIBLE; }
    @Override protected void onPlace(BlockState s, Level l, BlockPos p, BlockState old, boolean moved) {
        if (l instanceof ServerLevel sl) sl.scheduleTick(p, this, 200 + Math.floorMod(p.hashCode(), 100));
    }
    @Override protected void tick(BlockState s, ServerLevel l, BlockPos p, RandomSource r) {
        reconcile(l, p);
        if (l.getBlockState(p).is(this)) l.scheduleTick(p, this, 200);
    }
    /** Ancien relais (copies de la lumiere tous les 16 blocs) : supprime, une seule source lumineuse suffit. */
    public static void reconcile(ServerLevel l, BlockPos p) {
        if (l.hasChunkAt(p) && l.getBlockState(p).is(ModBlocks.LIGHT_FIELD.get())) l.removeBlock(p, false);
    }
}
