package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.dungeon.InstanceManager;
import com.mmorpg.dungeon.Layout;
import com.mmorpg.zone.ZoneData;
import com.mmorpg.zone.ZoneManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.Projectile;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/** Zone sure aux points d'arrivee des mines, plaines et donjons : aucun degat subi, aucun degat inflige depuis la zone. */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class SafeSpawn {
    private SafeSpawn() { }

    /** Moitie de la largeur et profondeur du hall d'entree d'un donjon (en blocs, autour du point d'arrivee). */
    private static final int HALL_HALF_X = 13, HALL_Z_MAX = 26;

    public static boolean safe(Entity e) {
        if (LoadingShield.active(e)) return true;                      // chargement en cours : invincible
        if (!(e.level() instanceof ServerLevel level)) return false;
        var server = level.getServer();
        if (!level.dimension().equals(InstanceManager.DIM)) return false;
        ZoneData.Zone z = ZoneManager.zoneAt(level, e.blockPosition());
        if (z != null && z.id().endsWith("_entree")) return true;   // place d'entree des mines et des plaines
        var slot = InstanceManager.slotAt(server, level, e.getX(), e.getZ());
        return slot != null && Math.abs(e.getX() - (slot.x + 0.5)) <= HALL_HALF_X && e.getZ() >= slot.z - 2 && e.getZ() <= slot.z + HALL_Z_MAX
                && e.getY() < slot.y + Layout.HALL.h() + 4;
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (event.getEntity().level().isClientSide()) return;
        if (event.getSource().is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        if (event.getEntity() instanceof ServerPlayer victim && safe(victim)) { event.setCanceled(true); return; }
        Entity src = event.getSource().getEntity();
        if (src instanceof Projectile pr && pr.getOwner() != null) src = pr.getOwner();
        if (src instanceof ServerPlayer attacker && safe(attacker)) event.setCanceled(true);
    }
}
