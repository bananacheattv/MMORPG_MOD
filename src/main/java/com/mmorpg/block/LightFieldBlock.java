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

/** Relais sans objet ni collision. Déterministe, partagé entre sources, nettoyé après suppression. */
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
    /** Aucun chargement de chunk, aucune traversée de mur, aucune modification de terrain. */
    public static boolean clear(ServerLevel l, BlockPos a, BlockPos b) {
        var from = net.minecraft.world.phys.Vec3.atCenterOf(a);
        var delta = net.minecraft.world.phys.Vec3.atCenterOf(b).subtract(from);
        int samples = (int)Math.ceil(delta.length() * 2);
        for (int i = 1; i < samples; i++) {
            var p = BlockPos.containing(from.add(delta.scale((double)i / samples)));
            if (!l.hasChunkAt(p)) return false;
            var s = l.getBlockState(p);
            if (!s.getCollisionShape(l, p).isEmpty()) return false;
        }
        return true;
    }
    public static void refresh(ServerLevel l, BlockPos source) {
        for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
            if (x == 0 && y == 0 && z == 0) continue;
            reconcile(l, source.offset(x * SPACING, y * SPACING, z * SPACING));
        }
    }
    public static void reconcile(ServerLevel l, BlockPos p) {
        if (!l.hasChunkAt(p) || p.getY() < l.getMinY() || p.getY() >= l.getMaxY() || !l.getWorldBorder().isWithinBounds(p)) return;
        var old = l.getBlockState(p); var field = ModBlocks.LIGHT_FIELD.get();
        // Seul l'air vanilla peut être remplacé : ni eau, ni lumière manuelle, ni bloc custom invisible.
        if (!old.is(field) && !old.is(net.minecraft.world.level.block.Blocks.AIR)
                && !old.is(net.minecraft.world.level.block.Blocks.CAVE_AIR) && !old.is(net.minecraft.world.level.block.Blocks.VOID_AIR)) return;
        int max = 0; boolean unknown = false;
        for (int x = -1; x <= 1; x++) for (int y = -1; y <= 1; y++) for (int z = -1; z <= 1; z++) {
            var anchor = p.offset(x * SPACING, y * SPACING, z * SPACING);
            if (!l.hasChunkAt(anchor)) { unknown = true; continue; }
            var s = l.getBlockState(anchor);
            if (s.is(ModBlocks.BLOC_LUMIERE.get()) && s.getValue(LightEmitterBlock.LEVEL) > max && clear(l, anchor, p))
                max = s.getValue(LightEmitterBlock.LEVEL);
        }
        if (max > 0) {
            var next = field.defaultBlockState().setValue(BlockStateProperties.LEVEL, max);
            if (!old.equals(next)) l.setBlock(p, next, Block.UPDATE_ALL);
        } else if (old.is(field) && !unknown) l.removeBlock(p, false);
    }
}
