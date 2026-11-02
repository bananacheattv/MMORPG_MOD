package com.mmorpg.block;

import com.mmorpg.network.Net;
import com.mmorpg.network.S2COpenScreen;
import com.mmorpg.registry.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

/**
 * Bloc de lumiere invisible (admin/creatif) : emet un niveau de lumiere 0-15 stocke dans l'etat du bloc (donc persistant).
 * Visible et selectionnable seulement quand on tient l'objet ; clic droit (operateur) = configuration du niveau.
 */
public class LightEmitterBlock extends Block {
    public static final IntegerProperty LEVEL = BlockStateProperties.LEVEL;

    public LightEmitterBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LEVEL, 15));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) {
        b.add(LEVEL);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return TeleporterBlock.isAdmin(ctx.getPlayer()) ? super.getStateForPlacement(ctx) : null;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.INVISIBLE;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return ctx.isHoldingItem(ModItems.BLOC_LUMIERE.get()) ? Block.box(5, 5, 5, 11, 11, 11) : Shapes.empty();
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getVisualShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    protected float getShadeBrightness(BlockState state, BlockGetter level, BlockPos pos) {
        return 1.0F;
    }

    @Override
    protected boolean propagatesSkylightDown(BlockState state) {
        return true;
    }

    /** Clic droit : ecran de configuration du niveau (operateurs). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!TeleporterBlock.isAdmin(player)) return InteractionResult.PASS;
        if (player instanceof ServerPlayer sp) {
            CompoundTag t = new CompoundTag();
            t.putLong("pos", pos.asLong());
            t.putInt("level", state.getValue(LEVEL));
            Net.toPlayer(sp, new S2COpenScreen(S2COpenScreen.LIGHT_BLOCK, t));
        }
        return InteractionResult.SUCCESS;
    }

    /** Repere visuel (etincelle) uniquement pour le joueur local qui tient l'objet. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        for (Player p : level.players()) {
            if (p.isLocalPlayer() && (p.getMainHandItem().is(ModItems.BLOC_LUMIERE.get()) || p.getOffhandItem().is(ModItems.BLOC_LUMIERE.get()))) {
                level.addParticle(ParticleTypes.END_ROD, pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5, 0, 0, 0);
                return;
            }
        }
    }

    /** Applique un niveau demande par le client (verifie cote serveur). */
    public static void apply(ServerPlayer p, CompoundTag data) {
        if (!TeleporterBlock.isAdmin(p)) return;
        BlockPos pos = BlockPos.of(data.getLongOr("pos", 0));
        if (p.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) > 64 * 64 || !p.level().isLoaded(pos)) return;
        BlockState s = p.level().getBlockState(pos);
        if (!(s.getBlock() instanceof LightEmitterBlock)) return;
        int lvl = Math.max(0, Math.min(15, data.getIntOr("level", 15)));
        p.level().setBlock(pos, s.setValue(LEVEL, lvl), Block.UPDATE_ALL);
    }
}
