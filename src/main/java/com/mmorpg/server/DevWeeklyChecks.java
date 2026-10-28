package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.block.structure.MultiblockKind;
import com.mmorpg.block.structure.Multiblocks;
import com.mmorpg.crafting.ForgeRecipes;
import com.mmorpg.registry.ModBlocks;
import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.rpg.PlayerData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.util.HashSet;
import java.util.UUID;

/** Opt-in server checks for the weekly quest board: ./gradlew runServer -PweeklyChecks (isolated run/ world only). */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class DevWeeklyChecks {
    private static void check(boolean condition, String label) {
        if (!condition) throw new AssertionError("Weekly check failed: " + label);
    }

    /** Board position of the visual scene (./gradlew runServer -PweeklyVisual, then runClient -Pshowcase=weekly). */
    public static final BlockPos VISUAL_BOARD = new BlockPos(0, 181, 8);

    @SubscribeEvent
    public static void joined(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event) {
        if (!Boolean.getBoolean("mmorpg.weeklyVisual") || !(event.getEntity() instanceof net.minecraft.server.level.ServerPlayer p)) return;
        Scheduler.later(p.level(), 30, () -> {
            var server = p.level().getServer();
            var level = server.overworld();
            server.getPlayerList().op(new net.minecraft.server.players.NameAndId(p.getGameProfile()));
            com.mmorpg.hub.CinematicManager.finish(p);
            p.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            var d = RpgPlayers.get(p);
            d.playerClass = PlayerClass.GUERRIER;
            d.level = 60;
            d.activeQuests.clear();
            d.dailyQuests.clear();
            for (int x = -14; x <= 14; x++) for (int z = -6; z <= 16; z++) {
                level.setBlock(new BlockPos(x, 180, z), Blocks.SMOOTH_STONE.defaultBlockState(), 3);
                for (int y = 181; y <= 192; y++) level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            }
            level.setBlock(VISUAL_BOARD, ModBlocks.QUETES_HEBDO.get().defaultBlockState(), 3);
            Multiblocks.placeParts(level, VISUAL_BOARD, MultiblockKind.QUETES_HEBDO, Direction.NORTH);
            p.teleportTo(level, 0.5, 181, 0.5, java.util.Set.of(), 0, 8, true);
            // fulfils whatever the player accepted, once, so the "ready to hand in" state can be captured
            Scheduler.later(level, 400, () -> {
                for (String id : new java.util.ArrayList<>(d.activeQuests.keySet())) {
                    var q = QuestManager.def(id);
                    if (q == null) continue;
                    var progress = d.activeQuests.get(id);
                    for (int i = 0; i < q.objectives.size(); i++) {
                        var o = q.objectives.get(i);
                        if (o.type.equals("kill")) progress[i] = o.count;
                        else p.getInventory().add(new net.minecraft.world.item.ItemStack(ForgeRecipes.resolveItem(o.target), o.count));
                    }
                }
                d.dirty = true;
            });
            MMORPG.LOGGER.info("[MMORPG] Weekly visual scene ready");
        });
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void run(ServerStartedEvent event) {
        if (!Boolean.getBoolean("mmorpg.checkWeekly")) return;
        var server = event.getServer();
        var level = server.overworld();
        var p = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "WeeklyChecks"));
        BlockPos board = new BlockPos(0, 150, 0);
        try {
            // pancarte 3D : pose, emprise et collisions
            for (int x = -5; x <= 5; x++) for (int y = 0; y <= 8; y++) for (int z = -5; z <= 5; z++) level.setBlock(board.offset(x, y, z), Blocks.AIR.defaultBlockState(), 3);
            var kind = MultiblockKind.QUETES_HEBDO;
            check(Multiblocks.hasRoom(level, board, kind, Direction.NORTH), "room for the board");
            level.setBlock(board, ModBlocks.QUETES_HEBDO.get().defaultBlockState(), 3);
            Multiblocks.placeParts(level, board, kind, Direction.NORTH);
            int parts = 0;
            for (int[] c : kind.cells()) if (!level.getBlockState(kind.cellPos(board, c, Direction.NORTH)).isAir()) parts++;
            check(parts >= 10, "invisible parts placed (" + parts + ")");
            check(!kind.masterShape(Direction.NORTH).isEmpty(), "master collision");
            p.snapTo(board.getX() + 0.5, board.getY(), board.getZ() - 3.5, 0, 0);

            // ressources du modele 3D et du bloc embarquees (le rendu lui-meme se controle cote client)
            for (String res : new String[]{"textures/entity/structure/quetes_hebdo.png", "textures/entity/structure/quetes_hebdo_emissive.png",
                    "textures/block/quetes_hebdo_top.png", "textures/block/quetes_hebdo_front.png", "textures/block/quetes_hebdo_side.png",
                    "blockstates/quetes_hebdo.json", "models/block/quetes_hebdo.json", "items/quetes_hebdo.json"}) {
                check(DevWeeklyChecks.class.getResource("/assets/mmorpg/" + res) != null, "resource " + res);
            }
            check(DevWeeklyChecks.class.getResource("/data/mmorpg/loot_table/blocks/quetes_hebdo.json") != null, "loot table");
            check(net.minecraft.core.registries.BuiltInRegistries.ITEM.containsKey(com.mmorpg.MMORPG.id("quetes_hebdo")), "block item registered");

            // modeles : toutes les quetes sont valides et resolvent leurs objets
            long w = WeeklyQuests.week();
            for (int tier = 0; tier < 3; tier++) {
                for (int tpl = 0; tpl < 4; tpl++) {
                    var q = WeeklyQuests.def(WeeklyQuests.id(w, tier, tpl));
                    check(q != null && q.weekly && !q.objectives.isEmpty(), "template " + tier + "/" + tpl);
                    for (var o : q.objectives) if (o.type.equals("collect")) check(ForgeRecipes.resolveItem(o.target) != Items.AIR, "item " + o.target);
                }
                check(WeeklyQuests.def(WeeklyQuests.id(w, tier, 4)) == null, "unknown template refused");
            }
            check(WeeklyQuests.def("weekly:abc") == null && WeeklyQuests.def("weekly:1:9:0") == null, "malformed ids refused");
            check(WeeklyQuests.secondsUntilReset() > 0 && WeeklyQuests.secondsUntilReset() <= 7 * 86400, "reset countdown");

            // tirage : stable pour un joueur, different d'un joueur a l'autre, change a chaque semaine
            var seen = new HashSet<String>();
            for (int i = 0; i < 12; i++) seen.add(WeeklyQuests.pick(UUID.randomUUID(), w, 0) + "/" + WeeklyQuests.pick(UUID.randomUUID(), w, 1));
            check(seen.size() > 1, "players get different quests");
            check(WeeklyQuests.pick(p.getUUID(), w, 2) == WeeklyQuests.pick(p.getUUID(), w, 2), "stable draw");
            boolean changes = false;
            for (long k = 1; k < 30; k++) changes |= WeeklyQuests.pick(p.getUUID(), w + k, 1) != WeeklyQuests.pick(p.getUUID(), w, 1);
            check(changes, "draw changes across weeks");

            // acceptation, progression, remise, unicite
            p.setData(com.mmorpg.registry.ModAttachments.PLAYER, new PlayerData());
            var d = RpgPlayers.get(p);
            d.playerClass = PlayerClass.GUERRIER;
            d.level = 50;
            RpgPlayers.get(p).dirty = false;
            p.getInventory().clearContent();
            WeeklyQuests.action(p, board, WeeklyQuests.ACT_ACCEPT, 1);
            String id = WeeklyQuests.offeredId(p, 1);
            check(d.activeQuests.containsKey(id) && d.activeQuests.size() == 1, "accept hard quest");
            check(d.questGivers.containsKey(id), "guidance binding to the board");
            WeeklyQuests.action(p, board, WeeklyQuests.ACT_ACCEPT, 1);
            check(d.activeQuests.size() == 1, "no duplicate acceptance");
            WeeklyQuests.action(p, board, WeeklyQuests.ACT_ACCEPT, 7);
            WeeklyQuests.action(p, board, WeeklyQuests.ACT_ACCEPT, -1);
            check(d.activeQuests.size() == 1, "invalid tier ignored");
            var far = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(UUID.randomUUID(), "WeeklyFar"));
            far.setData(com.mmorpg.registry.ModAttachments.PLAYER, new PlayerData());
            RpgPlayers.get(far).playerClass = PlayerClass.GUERRIER;
            far.snapTo(board.getX() + 0.5, board.getY(), board.getZ() + 60.5, 0, 0);
            WeeklyQuests.action(far, board, WeeklyQuests.ACT_ACCEPT, 0);
            check(RpgPlayers.get(far).activeQuests.isEmpty(), "too far to accept");
            WeeklyQuests.action(p, board, WeeklyQuests.ACT_CLAIM, 1);
            check(!d.dailyQuests.containsKey(WeeklyQuests.doneKey(id)) && d.activeQuests.containsKey(id), "claim refused before completion");

            var q = WeeklyQuests.def(id);
            for (var o : q.objectives) {
                if (o.type.equals("kill")) {
                    for (int i = 0; i < o.count; i++) QuestManager.onKill(p, "gobelin", "mmorpg:gobelin", o.target.equals("boss"));
                } else {
                    p.getInventory().add(new net.minecraft.world.item.ItemStack(ForgeRecipes.resolveItem(o.target), o.count));
                }
            }
            check(QuestManager.completable(p, d, id), "objectives completable");
            long gold = d.gold;
            WeeklyQuests.action(p, board, WeeklyQuests.ACT_CLAIM, 1);
            check(d.gold == gold + 450, "gold reward " + (d.gold - gold));
            check(p.getInventory().countItem(Items.EMERALD) == 10, "item reward");
            check(!d.activeQuests.containsKey(id) && WeeklyQuests.done(d, w, 1), "quest closed and recorded as done");
            for (var o : q.objectives) if (o.type.equals("collect")) check(p.getInventory().countItem(ForgeRecipes.resolveItem(o.target)) == 0, "collected items consumed");
            WeeklyQuests.action(p, board, WeeklyQuests.ACT_CLAIM, 1);
            WeeklyQuests.action(p, board, WeeklyQuests.ACT_ACCEPT, 1);
            check(d.gold == gold + 450 && d.activeQuests.isEmpty(), "no second reward or re-acceptance this week");

            // abandon, persistance, remise a zero hebdomadaire
            WeeklyQuests.action(p, board, WeeklyQuests.ACT_ACCEPT, 0);
            String easy = WeeklyQuests.offeredId(p, 0);
            check(d.activeQuests.containsKey(easy), "accept easy quest");
            var reloaded = new PlayerData();
            reloaded.load(d.save());
            check(reloaded.activeQuests.containsKey(easy) && WeeklyQuests.done(reloaded, w, 1), "persistence of active and done weekly quests");
            WeeklyQuests.action(p, board, WeeklyQuests.ACT_ABANDON, 0);
            check(!d.activeQuests.containsKey(easy), "abandon");
            WeeklyQuests.action(p, board, WeeklyQuests.ACT_ACCEPT, 2);
            String hard = WeeklyQuests.offeredId(p, 2);
            d.activeQuests.put(WeeklyQuests.id(w - 1, 0, 0), new int[]{1});
            d.dailyQuests.put(WeeklyQuests.DONE_PREFIX + (w - 1) + ":0", w - 1);
            d.dailyQuests.put("contrat_chasse", 12345L);
            WeeklyQuests.purge(d);
            check(!d.activeQuests.containsKey(WeeklyQuests.id(w - 1, 0, 0)) && d.activeQuests.containsKey(hard), "previous week quest purged, current kept");
            check(!d.dailyQuests.containsKey(WeeklyQuests.DONE_PREFIX + (w - 1) + ":0") && WeeklyQuests.done(d, w, 1), "previous week completion purged, current kept");
            check(d.dailyQuests.containsKey("contrat_chasse"), "daily quests untouched");
            MMORPG.LOGGER.info("[CODEX CHECKS] WEEKLY PASS: 3D board placement, templates, per-player draw, accept/progress/claim, uniqueness, distance, persistence, weekly reset");
        } catch (Throwable t) {
            MMORPG.LOGGER.error("[CODEX CHECKS] WEEKLY FAIL", t);
        } finally {
            server.halt(false);
        }
    }
}
