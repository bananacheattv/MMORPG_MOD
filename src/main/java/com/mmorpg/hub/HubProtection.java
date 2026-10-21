package com.mmorpg.hub;

import com.mmorpg.MMORPG;
import com.mmorpg.config.ConfigManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

/**
 * Protection du hub (reglage {@code hubProtection} de general.json) : pas de casse ni de pose de blocs, pas de PvP et pas de monstres
 * hostiles dans son volume. Les operateurs en mode creatif gardent la main pour le construire et l'amenager.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class HubProtection {
    private HubProtection() {
    }

    /** La position est-elle dans le volume du hub (et la protection activee) ? */
    public static boolean inHub(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel sl) || !ConfigManager.general().hubProtection) return false;
        HubData hub = HubData.get(sl.getServer());
        if (!hub.exists() || hub.origin() == null || !sl.dimension().equals(hub.dimension())) return false;
        return HubLayout.hubBounds(hub.origin()).contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
    }

    private static boolean builder(Entity e) {
        return e instanceof ServerPlayer p && p.isCreative() && HubManager.isOperator(p);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBreak(BreakBlockEvent event) {
        Player p = event.getPlayer();
        if (builder(p) || !inHub(p.level(), event.getPos())) return;
        event.setCanceled(true);
        warn(p);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlace(BlockEvent.EntityPlaceEvent event) {
        Entity e = event.getEntity();
        if (builder(e) || !(event.getLevel() instanceof Level level) || !inHub(level, event.getPos())) return;
        event.setCanceled(true);
        if (e instanceof Player p) warn(p);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player victim)) return;
        Entity src = event.getSource().getEntity();
        if (src instanceof Projectile pr && pr.getOwner() != null) src = pr.getOwner();
        if (src instanceof Player attacker && attacker != victim && inHub(victim.level(), victim.blockPosition())) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onSpawn(FinalizeSpawnEvent event) {
        Mob mob = event.getEntity();
        if (event.getSpawnType() == EntitySpawnReason.COMMAND || event.getSpawnType() == EntitySpawnReason.SPAWN_ITEM_USE) return;
        if (!(mob instanceof Enemy) && !(mob instanceof com.mmorpg.entity.RpgMob)) return;
        if (inHub(mob.level(), mob.blockPosition())) event.setSpawnCancelled(true);
    }

    /** Nettoyage des anciens exemplaires du hub (identifiant non stable) qui s'etaient accumules a chaque reconstruction. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel sl) || !sl.dimension().equals(HubManager.HUB_DIM)) return;
        Entity e = event.getEntity();
        if (HubBuilder.isStable(e.getUUID())) return;
        HubData hub = HubData.get(sl.getServer());
        if (!hub.exists() || hub.origin() == null) return;
        // objets au sol sans proprietaire : fleurs, herbes et lanternes qui se detachent pendant la construction (jamais ceux jetes par un joueur)
        if (e instanceof net.minecraft.world.entity.item.ItemEntity item && item.getOwner() == null
                && HubLayout.hubBounds(hub.origin()).contains(e.getX(), e.getY(), e.getZ())) {
            event.setCanceled(true);
            return;
        }
        boolean legacy = e instanceof com.mmorpg.entity.hub.ClassMasterEntity
                || (e instanceof com.mmorpg.entity.NpcEntity n && "maitre_classes".equals(n.group()))
                || (e instanceof net.minecraft.world.entity.Display.TextDisplay && e.entityTags().contains(HubBuilder.TAG))
                || (e instanceof com.mmorpg.entity.dummy.TrainingDummyEntity && HubBuilder.isDummySlot(hub.origin(), e.getX(), e.getZ()));
        if (legacy) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onExplosion(ExplosionEvent.Detonate event) {
        event.getAffectedBlocks().removeIf(pos -> inHub(event.getLevel(), pos));
    }

    private static void warn(Player p) {
        if (p instanceof ServerPlayer sp && sp.tickCount % 10 == 0) {
            sp.sendOverlayMessage(Component.literal("Le hub est protégé : on ne peut pas y construire ni y casser de blocs.").withColor(0xFFC060));
        }
    }
}
