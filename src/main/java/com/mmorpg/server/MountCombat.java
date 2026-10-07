package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.entity.MountEntity;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Montures et combat : les monstres qui visent un cavalier s'en prennent a sa monture (degats rediriges sur lui) ;
 * un coup donne ou recu d'un joueur renvoie la monture et l'interdit pendant 15 s (pas de monture en PvP).
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class MountCombat {
    private MountCombat() {
    }

    @SubscribeEvent
    public static void target(LivingChangeTargetEvent event) {
        if (event.getNewAboutToBeSetTarget() instanceof ServerPlayer sp && sp.getVehicle() instanceof MountEntity mount
                && !(event.getEntity() instanceof net.minecraft.world.entity.player.Player)) {
            event.setNewAboutToBeSetTarget(mount);
        }
    }

    @SubscribeEvent
    public static void damage(LivingIncomingDamageEvent event) {
        if (event.getEntity() instanceof ServerPlayer victim && event.getSource().getEntity() instanceof ServerPlayer attacker && attacker != victim) {
            MountManager.pvpTag(victim);
            MountManager.pvpTag(attacker);
        }
    }
}
