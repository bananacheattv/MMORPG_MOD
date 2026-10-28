package com.mmorpg.hub;

import com.mmorpg.MMORPG;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.InteractionResult;
import net.neoforged.bus.api.*;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.*;

/** Display equipment is decoration, never a public source of items. */
@EventBusSubscriber(modid=MMORPG.MODID)
public final class SpawnDecorProtection {
    public static boolean protectedEntity(Entity target){return target.level().dimension().equals(SpawnBuilder.DIM)&&(target instanceof ArmorStand||target instanceof ItemFrame);}
    public static boolean blocked(Entity actor,Entity target){return protectedEntity(target)&&!(actor instanceof ServerPlayer p&&com.mmorpg.server.CreativeAccess.allowed(p));}
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void interact(PlayerInteractEvent.EntityInteract e){if(blocked(e.getEntity(),e.getTarget())){e.setCanceled(true);e.setCancellationResult(InteractionResult.FAIL);}}
    @SubscribeEvent(priority=EventPriority.HIGHEST)
    public static void attack(AttackEntityEvent e){if(blocked(e.getEntity(),e.getTarget()))e.setCanceled(true);}
}
