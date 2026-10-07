package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.entity.MountEntity;
import com.mmorpg.mount.MountType;
import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.rpg.PlayerData;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

@EventBusSubscriber(modid = MMORPG.MODID)
public final class DevMountChecks {
    private static boolean done;
    @SubscribeEvent public static void tick(PlayerTickEvent.Post event) {
        if (done || !Boolean.getBoolean("mmorpg.checkMounts") || !(event.getEntity() instanceof ServerPlayer p) || p.tickCount < 100 || !p.onGround()) return;
        done = true;
        var d = RpgPlayers.get(p); var snapshot = d.save();
        var inventory = p.getInventory();
        var items = new net.minecraft.world.item.ItemStack[inventory.getContainerSize()];
        for (int i = 0; i < items.length; i++) items[i] = inventory.getItem(i).copy();
        long cooldown = d.nextMountTick;
        try {
            MountManager.despawn(p); d.mounts.clear(); d.nextMountTick = 0;
            for (int i = 0; i < items.length; i++) inventory.setItem(i, net.minecraft.world.item.ItemStack.EMPTY);
            d.playerClass = PlayerClass.GUERRIER; d.level = 1; d.gold = 10000;
            check(!MountManager.unlock(p, "voyageur") && d.gold == 10000, "no gold purchase");
            d.level = 100;
            check(MountManager.grant(p, com.mmorpg.mount.MountType.SANGLIER), "seal grant");
            check(!MountManager.grant(p, com.mmorpg.mount.MountType.SANGLIER) && d.gold == 10000, "no double grant");
            var saved = new PlayerData(); saved.load(d.save());
            check(saved.mounts.contains("voyageur"), "persistence");
            check(!MountManager.summon(p, "ombre"), "unowned summon denied");
            check(MountManager.summon(p, "voyageur"), "summon on solid ground");
            var entity = (MountEntity) p.level().getEntity(d.mountEntity);
            check(entity != null && entity.isSaddled(), "saddle included");
            var pig = net.minecraft.world.entity.EntityTypes.PIG.create(p.level(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            check(pig != null && !pig.startRiding(entity), "foreign rider denied");
            check(p.startRiding(entity) && entity.getControllingPassenger() == p, "owner can ride and steer");
            p.stopRiding();
            check(!MountManager.summon(p, "voyageur"), "summon cooldown");
            MountManager.despawn(p);
            check(entity.isRemoved() && d.mountEntity == null && d.activeMount.isEmpty(), "dismissal cleanup");
            MMORPG.LOGGER.info("[CODEX CHECKS] MOUNTS PASS: level, gold, purchase, persistence, ownership, riding, cooldown, cleanup");
        } finally {
            for (int i = 0; i < items.length; i++) inventory.setItem(i, items[i]);
            MountManager.despawn(p); d.load(snapshot); d.nextMountTick = cooldown; RpgPlayers.recomputeAndSync(p);
        }
    }
    private static void check(boolean b, String label) { if (!b) throw new IllegalStateException("Mount check: " + label); }
}
