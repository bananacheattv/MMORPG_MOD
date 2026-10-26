package com.mmorpg.server;

import com.mmorpg.MMORPG;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;

/** Final filter includes equipment drops and configured vanilla items; player inventories are untouched. */
@EventBusSubscriber(modid=MMORPG.MODID)
public final class VanillaMobDrops {
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void drops(LivingDropsEvent event) {
        if(event.getEntity() instanceof Player)return;
        event.getDrops().removeIf(e->BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).getNamespace().equals("minecraft"));
        net.minecraft.server.level.ServerPlayer owner = event.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer p ? p
                : event.getEntity().getLastHurtByPlayer() instanceof net.minecraft.server.level.ServerPlayer p ? p : null;
        if(owner!=null) for(var item:event.getDrops()) { item.addTag(PetLoot.tag(owner));item.setTarget(owner.getUUID()); }
    }
}
