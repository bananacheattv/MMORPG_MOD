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
    /** Objet vanilla de mobs.json : jamais laisse par un monstre (filtre ci-dessous), donc ni affiche au bestiaire ni propose par le guidage. */
    public static boolean vanilla(String item){return item!=null&&item.startsWith("minecraft:");}
    @SubscribeEvent(priority=EventPriority.LOWEST)
    public static void drops(LivingDropsEvent event) {
        if(event.getEntity() instanceof Player)return;
        event.getDrops().removeIf(e->BuiltInRegistries.ITEM.getKey(e.getItem().getItem()).getNamespace().equals("minecraft"));
        // Plaines et mines : aucun objet au sol, le butin va directement au tueur (or dans la bourse, le reste dans l'inventaire/les sacs).
        if(event.getEntity().level() instanceof net.minecraft.server.level.ServerLevel zl&&(com.mmorpg.plains.PlainsManager.inPlains(event.getEntity())||com.mmorpg.mine.MineManager.inMine(event.getEntity()))){
            net.minecraft.server.level.ServerPlayer killer = event.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer k ? k
                    : event.getEntity().getLastHurtByPlayer() instanceof net.minecraft.server.level.ServerPlayer k ? k : null;
            var left=new java.util.ArrayList<net.minecraft.world.entity.item.ItemEntity>();
            for(var drop:event.getDrops()){
                var stack=drop.getItem();
                if(killer==null||stack.isEmpty())continue;
                if(stack.is(com.mmorpg.registry.ModItems.PIECE_OR.get())){RpgPlayers.addGold(killer,stack.getCount(),true);AdventureSummary.item(killer,stack.getItem(),stack.getCount());continue;}
                var item=stack.getItem();int before=stack.getCount();killer.getInventory().add(stack);
                AdventureSummary.item(killer,item,before-stack.getCount());
                if(!stack.isEmpty()){drop.setItem(stack);left.add(drop);}
            }
            event.getDrops().clear();
            for(var d:left){d.addTag(PetLoot.tag(killer));d.setTarget(killer.getUUID());event.getDrops().add(d);}
            return;
        }
        net.minecraft.server.level.ServerPlayer owner = event.getSource().getEntity() instanceof net.minecraft.server.level.ServerPlayer p ? p
                : event.getEntity().getLastHurtByPlayer() instanceof net.minecraft.server.level.ServerPlayer p ? p : null;
        if(owner!=null) for(var item:event.getDrops()) { item.addTag(PetLoot.tag(owner));item.setTarget(owner.getUUID()); }
    }
}
