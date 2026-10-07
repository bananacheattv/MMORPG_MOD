package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.block.crate.CrateBlock;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/** Clic gauche sur une caisse = son interface (pas de casse) ; Maj + clic gauche = casser (admins en creatif). */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class CrateClicks {
    private CrateClicks() {
    }

    @SubscribeEvent
    public static void leftClick(PlayerInteractEvent.LeftClickBlock event) {
        var state = event.getLevel().getBlockState(event.getPos());
        if (!(state.getBlock() instanceof CrateBlock crate) || event.getEntity().isShiftKeyDown()) return;
        event.setCanceled(true);
        if (event.getAction() == PlayerInteractEvent.LeftClickBlock.Action.START && event.getEntity() instanceof ServerPlayer sp) {
            CrateManager.showScreen(sp, event.getPos(), crate.tier);
        }
    }
}
