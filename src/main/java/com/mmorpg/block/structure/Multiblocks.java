package com.mmorpg.block.structure;

import com.mmorpg.MMORPG;
import com.mmorpg.registry.ModBlocks;
import com.mmorpg.server.Scheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.ArrayList;
import java.util.List;

/** Pose, retrait et mise a niveau des structures multiblocs (blocs fonctionnels + parties invisibles). */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class Multiblocks {
    private Multiblocks() {
    }

    /** Vrai si toutes les cellules de l'emprise sont libres (air, herbe, neige...). */
    public static boolean hasRoom(LevelReader level, BlockPos master, MultiblockKind kind, Direction facing) {
        for (int[] c : kind.cells()) {
            BlockPos p = kind.cellPos(master, c, facing);
            if (level.isOutsideBuildHeight(p) || !level.getBlockState(p).canBeReplaced()) return false;
        }
        return true;
    }

    public static void noRoomMessage(Player player, MultiblockKind kind) {
        if (player != null && !player.level().isClientSide()) {
            player.sendOverlayMessage(Component.literal("Pas assez de place pour cette structure ("
                    + switch (kind) {
                        case AUTEL -> "5 × 5 blocs, 4 de haut";
                        case TELEPORTEUR -> "6 × 3 blocs, 5 de haut";
                        case FORGE -> "5 × 4 blocs, 5 de haut";
                    } + ").").withColor(0xFF6060));
        }
    }

    /** Pose les parties invisibles autour du bloc fonctionnel (sans remplacer de bloc existant). */
    public static void placeParts(Level level, BlockPos master, MultiblockKind kind, Direction facing) {
        for (int[] c : kind.cells()) {
            BlockPos p = kind.cellPos(master, c, facing);
            if (level.isOutsideBuildHeight(p)) continue;
            BlockState cur = level.getBlockState(p);
            if (cur.canBeReplaced() || StructurePartBlock.belongsTo(cur, p, master, kind)) {
                level.setBlock(p, StructurePartBlock.stateFor(ModBlocks.STRUCTURE_PART.get(), kind, facing, c), 3);
            }
        }
    }

    /** Retire les parties de la structure dont le bloc fonctionnel etait en {@code master}. */
    public static void removeParts(Level level, BlockPos master, MultiblockKind kind, Direction facing) {
        for (int[] c : kind.cells()) {
            BlockPos p = kind.cellPos(master, c, facing);
            if (StructurePartBlock.belongsTo(level.getBlockState(p), p, master, kind)) level.removeBlock(p, false);
        }
    }

    /** Point du modele (pixels, structure vers le nord, origine au centre bas du bloc fonctionnel) en coordonnees monde. */
    public static Vec3 world(BlockPos master, Direction facing, double x, double y, double z) {
        double rx = x, rz = z;
        switch (facing) {
            case EAST -> { rx = -z; rz = x; }
            case SOUTH -> { rx = -x; rz = -z; }
            case WEST -> { rx = z; rz = -x; }
            default -> { }
        }
        return new Vec3(master.getX() + 0.5 + rx / 16.0, master.getY() + y / 16.0, master.getZ() + 0.5 + rz / 16.0);
    }

    /**
     * Mise a niveau des mondes existants : les blocs poses avant les modeles 3D n'ont ni entite de bloc (donc pas de
     * rendu) ni parties. On les complete au chargement du tronçon.
     */
    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getChunk() instanceof LevelChunk chunk)) return;
        List<BlockPos> found = new ArrayList<>();
        LevelChunkSection[] sections = chunk.getSections();
        for (int i = 0; i < sections.length; i++) {
            LevelChunkSection s = sections[i];
            if (s == null || s.hasOnlyAir() || !s.maybeHas(st -> MultiblockKind.of(st.getBlock()) != null)) continue;
            int baseY = chunk.getSectionYFromSectionIndex(i) << 4;
            for (int y = 0; y < 16; y++) {
                for (int z = 0; z < 16; z++) {
                    for (int x = 0; x < 16; x++) {
                        if (MultiblockKind.of(s.getBlockState(x, y, z).getBlock()) != null) {
                            found.add(new BlockPos(chunk.getPos().getMinBlockX() + x, baseY + y, chunk.getPos().getMinBlockZ() + z));
                        }
                    }
                }
            }
        }
        for (BlockPos pos : found) {
            if (chunk.getBlockEntity(pos, LevelChunk.EntityCreationType.IMMEDIATE) != null) chunk.markUnsaved();
            Scheduler.later(level, 1, () -> {
                BlockState st = level.getBlockState(pos);
                MultiblockKind kind = MultiblockKind.of(st.getBlock());
                if (kind == null || kind.cells().isEmpty()) return;
                Direction facing = st.getValue(BlockStateProperties.HORIZONTAL_FACING);
                int[] first = kind.cells().get(0);
                BlockPos p = kind.cellPos(pos, first, facing);
                if (level.isLoaded(p) && !StructurePartBlock.belongsTo(level.getBlockState(p), p, pos, kind)) {
                    placeParts(level, pos, kind, facing);
                }
            });
        }
    }
}
