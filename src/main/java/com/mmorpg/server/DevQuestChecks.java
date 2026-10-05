package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.config.ConfigManager;
import com.mmorpg.entity.NpcEntity;
import com.mmorpg.quest.QuestDef;
import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.world.DungeonBuilder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/** Opt-in integration checks in a disposable development world. */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class DevQuestChecks {
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (!Boolean.getBoolean("mmorpg.checkQuests") || !(event.getEntity() instanceof ServerPlayer p)) return;
        var d = RpgPlayers.get(p);
        var snapshot = d.save();
        var inv = p.getInventory();
        ItemStack[] saved = new ItemStack[inv.getContainerSize()];
        for (int i = 0; i < saved.length; i++) saved[i] = inv.getItem(i).copy();
        var npc = DungeonBuilder.spawnNpc(p.level(), p.blockPosition(), NpcEntity.Role.QUETES, "Validation", 0, 0);
        String id = "codex_validation_quest";
        var old = ConfigManager.quests().get(id);
        try {
            if (npc == null) throw new IllegalStateException("Missing NPC");
            npc.setup(NpcEntity.Role.QUETES, "Validation", 0, "cryptes");
            d.playerClass = PlayerClass.GUERRIER; d.level = 100;
            d.activeQuests.clear(); d.completedQuests.remove(id);
            var q = new QuestDef(); q.giver = "cryptes"; q.rewards.gold = 123;
            q.objectives.add(new QuestDef.Objective("collect", "minecraft:iron_ingot", 5));
            q.objectives.add(new QuestDef.Objective("collect", "minecraft:iron_ingot", 5));
            ConfigManager.quests().put(id, q);
            check(!QuestManager.isAvailable(d, id, q, "autre"), "giver acceptance");
            q.requires.add("test_prerequisite");
            check(!QuestManager.isAvailable(d, id, q, "cryptes"), "prerequisite");
            q.requires.clear();
            QuestManager.accept(p, id, npc.getId());
            check(d.activeQuests.containsKey(id), "accept");
            QuestManager.track(p, id);
            var loaded = new PlayerData(); loaded.load(d.save());
            check(loaded.trackedQuest.equals(id), "tracking persistence");
            for (int i = 0; i < saved.length; i++) inv.setItem(i, ItemStack.EMPTY);
            inv.setItem(0, new ItemStack(Items.IRON_INGOT, 8));
            check(!QuestManager.completable(p, d, id), "aggregate collection");
            inv.setItem(0, new ItemStack(Items.IRON_INGOT, 20));
            long gold = d.gold;
            npc.setup(NpcEntity.Role.QUETES, "Validation", 0, "autre");
            QuestManager.complete(p, id, npc.getId());
            check(d.gold == gold && inv.countItem(Items.IRON_INGOT) == 20, "wrong NPC denied");
            npc.setup(NpcEntity.Role.QUETES, "Validation", 0, "cryptes");
            QuestManager.complete(p, id, npc.getId());
            check(d.gold == gold + 123 && inv.countItem(Items.IRON_INGOT) == 10 && d.trackedQuest.isEmpty(), "reward and collection");
            QuestManager.complete(p, id, npc.getId());
            check(d.gold == gold + 123 && inv.countItem(Items.IRON_INGOT) == 10, "no duplicate reward");
            MMORPG.LOGGER.info("[CODEX CHECKS] QUESTS PASS: prerequisites, tracking, aggregate collection, giver, unique reward");
        } finally {
            if (npc != null) npc.discard();
            if (old == null) ConfigManager.quests().remove(id); else ConfigManager.quests().put(id, old);
            for (int i = 0; i < saved.length; i++) inv.setItem(i, saved[i]);
            d.load(snapshot); d.dirty = true;
        }
    }
    private static void check(boolean condition, String label) {
        if (!condition) throw new IllegalStateException("Quest check failed: " + label);
    }
}
