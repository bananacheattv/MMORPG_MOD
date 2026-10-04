package com.mmorpg.block;

import com.mmorpg.block.structure.MultiblockKind;
import com.mmorpg.block.structure.MultiblockMasterBlock;
import com.mmorpg.block.structure.Multiblocks;
import com.mmorpg.server.TeleportManager;
import com.mmorpg.world.WaypointData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * Teleporteur : point de voyage entre villes et donjons, arche multibloc (modele art/structures_blockbench).
 * Le bloc fonctionnel est le sol du passage ; le portail s'affiche quand un joueur approche.
 * Outil d'administration : seuls les operateurs peuvent le poser, le configurer et le casser ;
 * les joueurs peuvent seulement l'utiliser.
 */
public class TeleporterBlock extends MultiblockMasterBlock {
    public TeleporterBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    public MultiblockKind kind() {
        return MultiblockKind.TELEPORTEUR;
    }

    /** Operateur (permission de maitre du jeu) : seul autorise a gerer les teleporteurs. */
    /** Operateur, ou proprietaire de la partie en solo (sur un serveur, les teleporteurs publics restent proteges). */
    public static boolean isAdmin(@Nullable Player player) {
        if (player == null) return false;
        if (player.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) return true;
        var server = player.level().getServer();
        return server != null && server.isSingleplayer() && server.isSingleplayerOwner(player.nameAndId());
    }

    /** Le teleporteur s'utilise en passant sous l'arche (voir TeleporterPortal). */
    public static Component passageMessage() {
        return Component.literal("Passez sous l'arche du téléporteur pour choisir une destination.").withColor(0x9FD8FF);
    }

    public static Component adminOnlyMessage() {
        return Component.literal("Les téléporteurs sont gérés par les administrateurs.").withColor(0xFF6060);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        if (!isAdmin(context.getPlayer())) {
            if (context.getPlayer() instanceof ServerPlayer sp) sp.sendOverlayMessage(adminOnlyMessage());
            return null;
        }
        return super.getStateForPlacement(context);
    }

    /** Incassable pour les joueurs (comme la bedrock), cassable normalement par un operateur. */
    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        // le client ne connait pas le proprietaire de la partie : c'est le serveur qui tranche (et annule sinon)
        return player.level().isClientSide() || isAdmin(player) ? super.getDestroyProgress(state, player, level, pos) : 0f;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        super.setPlacedBy(level, pos, state, by, itemStack);
        if (level instanceof ServerLevel sl && by instanceof ServerPlayer sp) {
            TeleportManager.onPlaced(sl, pos, sp);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (player instanceof ServerPlayer sp && level instanceof ServerLevel sl) {
            if (sp.isShiftKeyDown() && isAdmin(sp)) {
                TeleportManager.onUse(sl, pos, sp);          // configuration (administrateur)
            } else {
                sp.sendOverlayMessage(passageMessage());
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        super.affectNeighborsAfterRemoval(state, level, pos, movedByPiston);
        if (!(level.getBlockState(pos).getBlock() instanceof TeleporterBlock)) {
            WaypointData.get(level.getServer()).removeAt(level.dimension(), pos);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        Direction f = state.getValue(FACING);
        boolean open = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, 7, false) != null;
        for (int i = 0; i < (open ? 3 : 1); i++) {
            Vec3 p = Multiblocks.world(pos, f, (random.nextDouble() - 0.5) * 32, 8 + random.nextDouble() * 44, 1);
            if (open) {
                level.addParticle(ParticleTypes.PORTAL, p.x, p.y, p.z, (random.nextDouble() - 0.5) * 0.4, -random.nextDouble() * 0.3, (random.nextDouble() - 0.5) * 0.4);
            } else if (random.nextInt(4) == 0) {
                level.addParticle(ParticleTypes.REVERSE_PORTAL, p.x, p.y, p.z, 0, 0.01, 0);
            }
        }
        if (open && random.nextInt(3) == 0) {
            Vec3 c = Multiblocks.world(pos, f, (random.nextDouble() - 0.5) * 8, 58 + random.nextDouble() * 14, -14);
            level.addParticle(ParticleTypes.END_ROD, c.x, c.y, c.z, 0, 0.01, 0);
        }
    }
}
