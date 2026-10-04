package com.mmorpg.block;

import com.mmorpg.block.structure.MultiblockKind;
import com.mmorpg.block.structure.MultiblockMasterBlock;
import com.mmorpg.block.structure.Multiblocks;
import com.mmorpg.entity.boss.BossManager;
import com.mmorpg.item.SummonKeyItem;
import com.mmorpg.server.Scheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Autel d'Invocation : structure multibloc (plateforme, piliers runiques, autel central ; modele
 * art/structures_blockbench). On depose un objet d'invocation sur l'autel central pour faire apparaitre un boss ;
 * pendant l'invocation, l'autel est actif (runes allumees, cristaux en rotation, particules).
 */
public class AltarBlock extends MultiblockMasterBlock {
    public static final BooleanProperty ACTIVE = BlockStateProperties.LIT;

    public AltarBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ACTIVE, false));
    }

    @Override
    public MultiblockKind kind() {
        return MultiblockKind.AUTEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ACTIVE);
    }

    /** Allume l'autel pendant {@code ticks} (invocation en cours). */
    public static void activate(ServerLevel level, BlockPos pos, int ticks) {
        BlockState st = level.getBlockState(pos);
        if (!(st.getBlock() instanceof AltarBlock)) return;
        level.setBlock(pos, st.setValue(ACTIVE, true), 3);
        Scheduler.later(level, ticks, () -> {
            BlockState now = level.getBlockState(pos);
            if (now.getBlock() instanceof AltarBlock) level.setBlock(pos, now.setValue(ACTIVE, false), 3);
        });
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (stack.getItem() instanceof SummonKeyItem key) {
            if (player instanceof ServerPlayer sp && level instanceof ServerLevel sl) {
                if (BossManager.summon(sl, pos, key.bossKey(), sp)) {
                    if (!sp.hasInfiniteMaterials()) stack.shrink(1);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            player.sendOverlayMessage(Component.literal("Déposez un objet d'invocation sur l'autel pour réveiller un boss.").withColor(0xE0A0FF));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Direction f = state.getValue(FACING);
        boolean active = state.getValue(ACTIVE);
        Vec3 core = Multiblocks.world(pos, f, 0, active ? 32 : 26, 0);
        double a = (level.getGameTime() % 40) / 40.0 * Math.PI * 2;
        for (int i = 0; i < 2; i++) {
            double ang = a + i * Math.PI;
            level.addParticle(ParticleTypes.WITCH, core.x + Math.cos(ang) * 0.5, core.y - 0.2, core.z + Math.sin(ang) * 0.5, 0, 0.02, 0);
        }
        if (active) {
            // particules d'invocation : montent du centre et convergent depuis les cristaux des piliers
            for (int i = 0; i < 3; i++) {
                level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, core.x + (random.nextDouble() - 0.5) * 0.6, core.y, core.z + (random.nextDouble() - 0.5) * 0.6,
                        0, 0.08 + random.nextDouble() * 0.06, 0);
            }
            int[][] pillars = {{25, 62, 25}, {-25, 62, 25}, {25, 50, -25}, {-25, 50, -25}};      // x, y, z des cristaux
            int[] p = pillars[random.nextInt(4)];
            Vec3 tip = Multiblocks.world(pos, f, p[0], p[1], p[2]);
            // la particule de portail part de (position + vitesse) et converge vers la position
            level.addParticle(ParticleTypes.PORTAL, core.x, core.y, core.z, tip.x - core.x, tip.y - core.y, tip.z - core.z);
        } else if (random.nextInt(4) == 0) {
            level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, core.x, core.y, core.z, 0, 0.03, 0);
        }
    }
}
