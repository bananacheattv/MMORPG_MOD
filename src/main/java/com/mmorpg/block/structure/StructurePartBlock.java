package com.mmorpg.block.structure;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Partie invisible d'une structure multibloc : porte la collision de sa cellule (calculee depuis le modele) et
 * renvoie clics, casse et choix du bloc vers le bloc fonctionnel. Pas d'objet, pas de butin.
 * Proprietes : structure, orientation, decalage (vers le nord) par rapport au bloc fonctionnel.
 */
public class StructurePartBlock extends Block {
    public static final EnumProperty<MultiblockKind> KIND = EnumProperty.create("structure", MultiblockKind.class);
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final IntegerProperty OX = IntegerProperty.create("ox", 0, 2 * MultiblockKind.RANGE_XZ);
    public static final IntegerProperty OY = IntegerProperty.create("oy", 0, MultiblockKind.RANGE_Y);
    public static final IntegerProperty OZ = IntegerProperty.create("oz", 0, 2 * MultiblockKind.RANGE_XZ);

    public StructurePartBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(KIND, MultiblockKind.FORGE).setValue(FACING, Direction.NORTH)
                .setValue(OX, MultiblockKind.RANGE_XZ).setValue(OY, 1).setValue(OZ, MultiblockKind.RANGE_XZ));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(KIND, FACING, OX, OY, OZ);
    }

    public static BlockState stateFor(Block part, MultiblockKind kind, Direction facing, int[] cell) {
        return part.defaultBlockState().setValue(KIND, kind).setValue(FACING, facing)
                .setValue(OX, cell[0] + MultiblockKind.RANGE_XZ).setValue(OY, cell[1]).setValue(OZ, cell[2] + MultiblockKind.RANGE_XZ);
    }

    private static int[] cell(BlockState state) {
        return new int[]{state.getValue(OX) - MultiblockKind.RANGE_XZ, state.getValue(OY), state.getValue(OZ) - MultiblockKind.RANGE_XZ};
    }

    /** Position du bloc fonctionnel dont depend cette partie. */
    public static BlockPos masterPos(BlockPos pos, BlockState state) {
        int[] c = cell(state);
        return pos.subtract(MultiblockKind.rotate(c[0], c[1], c[2], state.getValue(FACING)));
    }

    /** Vrai si cette partie appartient bien a la structure dont le bloc fonctionnel est en {@code master}. */
    public static boolean belongsTo(BlockState state, BlockPos pos, BlockPos master, MultiblockKind kind) {
        return state.getBlock() instanceof StructurePartBlock && state.getValue(KIND) == kind && masterPos(pos, state).equals(master);
    }

    private static @Nullable BlockState master(BlockGetter level, BlockPos pos, BlockState state) {
        BlockState m = level.getBlockState(masterPos(pos, state));
        return m.getBlock() == state.getValue(KIND).masterBlock() ? m : null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        int[] c = cell(state);
        return state.getValue(KIND).shape(c[0], c[1], c[2], state.getValue(FACING));
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
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        BlockState m = master(level, pos, state);
        return m == null ? InteractionResult.PASS : m.useWithoutItem(level, player, hit.withPosition(masterPos(pos, state)));
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        BlockState m = master(level, pos, state);
        return m == null ? InteractionResult.TRY_WITH_EMPTY_HAND : m.useItemOn(stack, level, player, hand, hit.withPosition(masterPos(pos, state)));
    }

    /** Meme resistance que le bloc fonctionnel (le teleporteur reste incassable pour les joueurs). */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        BlockState m = master(level, pos, state);
        return m == null ? 1.0F : m.getDestroyProgress(player, level, masterPos(pos, state));
    }

    /** Casser une partie casse toute la structure (le bloc fonctionnel tombe en objet hors mode creatif). */
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide() && master(level, pos, state) != null) {
            level.destroyBlock(masterPos(pos, state), !player.preventsBlockDrops(), player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
        return new ItemStack(state.getValue(KIND).masterBlock());
    }
}
