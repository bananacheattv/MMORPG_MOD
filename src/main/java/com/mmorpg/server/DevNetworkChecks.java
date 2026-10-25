package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.crafting.ForgeProgression;
import com.mmorpg.crafting.ForgeRecipes;
import com.mmorpg.network.*;
import com.mmorpg.registry.ModBlocks;
import com.mmorpg.registry.ModItems;
import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.rpg.PlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import java.nio.file.*;
import java.util.UUID;

/** Headless integration suite: ./gradlew runServer -PnetworkChecks (isolated run/ world only). */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class DevNetworkChecks {
    private static void check(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void run(ServerStartedEvent event) throws Exception {
        if (!Boolean.getBoolean("mmorpg.checkNetwork")) return;
        Files.deleteIfExists(Path.of("../build/reports/network-checks.txt"));
        var server = event.getServer();
        var p = net.neoforged.neoforge.common.util.FakePlayerFactory.get(server.overworld(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "NetworkChecks"));
        Path directory = Files.createTempDirectory("aeloria-network-checks-");
        NetworkConfig original = NetworkConfig.current;
        try {
            UUID id = UUID.randomUUID();
            try (ProfileStore a = ProfileStore.acquire(directory, id)) {
                check(a != null && a.read() == null, "fresh profile");
                check(ProfileStore.acquire(directory, id) == null, "simultaneous writer refused");
                a.write(new byte[]{1, 2, 3}); a.write(new byte[]{4, 5});
                check(java.util.Arrays.equals(a.read(), new byte[]{4, 5}), "atomic replacement");
                check(java.util.Arrays.equals(Files.readAllBytes(directory.resolve(id + ".dat.bak")), new byte[]{1, 2, 3}), "previous snapshot backed up");
            }
            try (ProfileStore b = ProfileStore.acquire(directory, id)) { check(b != null && b.read().length == 2, "lock released for destination"); }
            var input = new java.io.DataInputStream(new java.io.ByteArrayInputStream(ProxyConnect.to("mines").bytes()));
            check(input.readUTF().equals("Connect") && input.readUTF().equals("mines") && input.available() == 0, "Velocity wire format");
            for (var r : ForgeRecipes.all()) {
                check(r.resultItem() != net.minecraft.world.item.Items.AIR, "registered output " + r.id());
                var seen = new java.util.HashSet<String>();
                for (var ing : r.ingredients()) {
                    check(BuiltInRegistries.ITEM.getKey(ing.resolve()).getNamespace().equals("mmorpg"), "no vanilla/unknown forge input " + r.id() + ":" + ing.item());
                    check(ing.count() > 0 && seen.add(ing.item()), "positive consolidated ingredient " + r.id());
                }
                check(!r.category().name().equals("INVOCATIONS"), "invocations removed server-side");
            }
            for (var sound : BuiltInRegistries.SOUND_EVENT) {
                var key = BuiltInRegistries.SOUND_EVENT.getKey(sound);
                if (key.getNamespace().equals("minecraft") && key.getPath().startsWith("entity.horse")) {
                    check(BuiltInRegistries.SOUND_EVENT.getKey(com.mmorpg.entity.MountEntity.mountSound(sound)).getNamespace().equals("mmorpg"), "no horse sample " + key);
                }
            }
            for (PlayerClass cls : PlayerClass.values()) {
                if (!cls.isPlayable()) continue;
                p.setData(com.mmorpg.registry.ModAttachments.PLAYER, new PlayerData());
                p.getInventory().clearContent();
                for (EquipmentSlot slot : EquipmentSlot.values()) if (slot.isArmor()) p.setItemSlot(slot, ItemStack.EMPTY);
                RpgPlayers.selectClass(p, cls);
                check(RpgPlayers.get(p).classChosenOnce, "starter only once " + cls);
                for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                    check(p.getItemBySlot(slot).getItem() instanceof com.mmorpg.item.RpgEquipment eq && eq.rpgDef().level() == 1, "level 1 armor " + cls + ":" + slot);
                }
                long count = 0;
                for (ItemStack item : p.getInventory()) if (!item.isEmpty()) count += item.getCount();
                RpgPlayers.selectClass(p, cls);
                long again = 0;
                for (ItemStack item : p.getInventory()) if (!item.isEmpty()) again += item.getCount();
                check(count == again, "no duplicate kit");
            }
            p.getInventory().clearContent();
            var d = RpgPlayers.get(p); d.level = 100; d.gold = 10000;
            BlockPos forge = new BlockPos(0, 80, 0);
            p.level().setBlockAndUpdate(forge, ModBlocks.FORGE_ARCANIQUE.get().defaultBlockState());
            p.setPos(.5, 81, .5);
            var recipe = ForgeRecipes.get("lame_runique");
            for (var ing : recipe.ingredients()) RpgPlayers.give(p, ing.resolve(), ing.count() * 2);
            long before = d.gold;
            ForgeManager.craft(p, forge, recipe.id(), 1);
            check(d.gold == before && ForgeManager.count(p.getInventory(), recipe.resultItem()) == 0, "locked craft refuses packet without spending");
            p.getInventory().setSelectedSlot(8);
            p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.get(ForgeProgression.scroll(1)), 2));
            p.getMainHandItem().getItem().use(p.level(), p, InteractionHand.MAIN_HAND);
            check(d.forgeBlueprints == 1 && p.getMainHandItem().getCount() == 1, "scroll consumed on first learning");
            p.getMainHandItem().getItem().use(p.level(), p, InteractionHand.MAIN_HAND);
            check(p.getMainHandItem().getCount() == 1, "duplicate scroll preserved");
            ForgeManager.craft(p, forge, recipe.id(), 1);
            check(d.gold == before - recipe.gold() && ForgeManager.count(p.getInventory(), recipe.resultItem()) == 1, "unlocked craft consumes correct cost: gold=" + d.gold + ", output=" + ForgeManager.count(p.getInventory(), recipe.resultItem()));
            check(!ForgeProgression.unlocked(d, ForgeRecipes.get("epee_berserker")), "higher tier still locked");
            var copy = new PlayerData(); copy.load(d.save());
            check(copy.forgeBlueprints == 1, "scroll unlock persistence");
            for (int tier = 1; tier <= 5; tier++) {
                final String scrollId = ForgeProgression.scroll(tier);
                var q = com.mmorpg.config.ConfigManager.quests().get(ForgeProgression.scroll(tier));
                check(q != null && q.rewards.items.stream().anyMatch(i -> i.item.endsWith(scrollId)), "quest scroll reward " + tier);
            }
            var bank = com.mmorpg.economy.BankData.get(server).account(p.getUUID()); bank.gold = 999;
            bank.items.set(0, new ItemStack(ModItems.PIECE_OR.get(), 23));
            d.cooldowns.put("test", p.level().getGameTime() + 200);
            var saved = NetworkPlayers.snapshot(p);
            d.gold = 0; d.forgeBlueprints = 0; p.getInventory().clearContent(); bank.gold = 0;
            NetworkPlayers.restore(p, saved);
            check(RpgPlayers.get(p).gold == before - recipe.gold() && RpgPlayers.get(p).forgeBlueprints == 1, "shared RPG restore");
            check(RpgPlayers.get(p).cooldowns.getOrDefault("test", 0L) > p.level().getGameTime(), "server travel preserves skill cooldown");
            check(ForgeManager.count(p.getInventory(), recipe.resultItem()) == 1, "shared inventory restore");
            check(com.mmorpg.economy.BankData.get(server).account(p.getUUID()).gold == 999 && com.mmorpg.economy.BankData.get(server).account(p.getUUID()).items.get(0).getCount() == 23, "shared bank restore");
            var malformed = saved.copy(); malformed.remove("inventory");
            try { NetworkPlayers.restore(p, malformed); throw new AssertionError("Malformed profile accepted"); }
            catch (java.io.IOException expected) { check(ForgeManager.count(p.getInventory(), recipe.resultItem()) == 1, "bad profile leaves inventory intact"); }
            var network = new NetworkConfig(); network.enabled = true; network.role = NetworkConfig.Role.MAIN; network.sharedDirectory = directory.toString();
            NetworkConfig.current = network;
            check(NetworkPlayers.login(p), "source login acquires session");
            check(NetworkPlayers.save(p), "source snapshot saved");
            NetworkPlayers.logout(p);
            RpgPlayers.get(p).gold = 0;
            check(NetworkPlayers.login(p), "destination login reacquires session");
            check(RpgPlayers.get(p).gold == before - recipe.gold(), "destination reads latest source snapshot");
            NetworkPlayers.logout(p);
            var boss = com.mmorpg.registry.ModEntities.GOBLIN_KING.get().create(p.level(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            var md = boss.getData(com.mmorpg.registry.ModAttachments.MOB);
            md.level = 20; md.key = "roi_gobelin"; md.boss = true;
            int scrolls = ForgeManager.count(p.getInventory(), ModItems.get(ForgeProgression.scroll(1)));
            KillRewards.onDeath(new net.neoforged.neoforge.event.entity.living.LivingDeathEvent(boss, p.damageSources().playerAttack(p)));
            check(ForgeManager.count(p.getInventory(), ModItems.get(ForgeProgression.scroll(1))) == scrolls + 1, "boss grants a personal crafting scroll");
            DevPortalChecks.run(p);
            MMORPG.LOGGER.info("[CODEX CHECKS] NETWORK_FORGE PASS: session locks, backup, proxy format, recipes, starters, scrolls, crafting, quests, shared inventory/bank, corruption refusal, handoff");
            Files.createDirectories(Path.of("../build/reports"));
            Files.writeString(Path.of("../build/reports/network-checks.txt"), "PASS\n");
        } finally {
            NetworkPlayers.logout(p);
            NetworkConfig.current = original;
            server.halt(false);
        }
    }
}
