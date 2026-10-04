package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.block.TeleporterBlock;
import com.mmorpg.block.structure.MultiblockKind;
import com.mmorpg.block.structure.StructurePartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Teleporteurs : passer sous l'arche (dans le passage du portail) ouvre la liste des destinations, une fois par passage.
 * Il faut ressortir du passage pour la rouvrir. Un administrateur qui traverse en etant accroupi ouvre la configuration.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class TeleporterPortal {
    private static final Map<UUID, BlockPos> INSIDE = new HashMap<>();

    private TeleporterPortal() {
    }

    /** Bloc fonctionnel du teleporteur dont le joueur se trouve dans le passage, ou null. */
    public static @Nullable BlockPos passageAt(ServerPlayer player) {
        Level level = player.level();
        BlockPos feet = player.blockPosition();
        for (int dy = 0; dy <= 2; dy++) {
            BlockPos q = feet.below(dy);
            BlockState st = level.getBlockState(q);
            if (st.getBlock() instanceof TeleporterBlock) return q;
            // les deux cellules laterales du passage (decalage +-1 vers le nord, au niveau du sol)
            if (st.getBlock() instanceof StructurePartBlock && st.getValue(StructurePartBlock.KIND) == MultiblockKind.TELEPORTEUR
                    && st.getValue(StructurePartBlock.OY) == 0 && st.getValue(StructurePartBlock.OZ) == MultiblockKind.RANGE_XZ
                    && Math.abs(st.getValue(StructurePartBlock.OX) - MultiblockKind.RANGE_XZ) == 1) {
                return StructurePartBlock.masterPos(q, st);
            }
            if (!st.getCollisionShape(level, q).isEmpty()) return null;      // sol ou mur : pas dans un passage
        }
        return null;
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()) return;
        UUID id = player.getUUID();
        BlockPos master = passageAt(player);
        BlockPos previous = INSIDE.get(id);
        if (master == null) {
            if (previous != null) INSIDE.remove(id);
            return;
        }
        if (master.equals(previous)) return;
        INSIDE.put(id, master);
        if (player.tickCount < 40) return;      // connexion ou reapparition dans le passage : pas d'ouverture immediate
        TeleportManager.onUse(player.level(), master, player);
    }

    /** Le joueur vient d'arriver dans ce passage (teleportation) : ne pas rouvrir la liste avant qu'il en sorte. */
    public static void markInside(ServerPlayer player, BlockPos master) {
        INSIDE.put(player.getUUID(), master);
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        INSIDE.remove(event.getEntity().getUUID());
    }
}
