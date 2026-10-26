package com.mmorpg.server;

import com.mmorpg.world.AdventurePortalData;
import com.mmorpg.world.AdventurePortalData.Portal;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;

/** Called by the isolated headless network integration suite. */
public final class DevPortalChecks {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError("portals: " + message); }
    private static Portal portal(ServerPlayer p, boolean widthX, String kind, int region, String dungeon, int difficulty) {
        return new Portal(UUID.randomUUID(), p.level().dimension(), new BlockPos(48, 100, 48), widthX, kind, region, dungeon, difficulty);
    }
    public static void run(ServerPlayer p) throws Exception {
        var level = p.level();
        for (int x = 42; x <= 54; x++) for (int z = 42; z <= 54; z++) {
            level.getChunkAt(new BlockPos(x, 99, z));
            level.setBlockAndUpdate(new BlockPos(x, 99, z), Blocks.STONE.defaultBlockState());
            for (int y = 100; y <= 106; y++) level.setBlockAndUpdate(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState());
        }
        var data = AdventurePortalData.get(level.getServer());
        for (Portal old : java.util.List.copyOf(data.all())) if (old.dimension().equals(level.dimension()) && old.pos().getX()>=42 && old.pos().getX()<=54 && old.pos().getZ()>=42 && old.pos().getZ()<=54) data.remove(old.id());
        int baseline = data.all().size();
        Map<BlockPos, BlockState> original = new LinkedHashMap<>();
        for (int x = 45; x <= 51; x++) for (int z = 45; z <= 51; z++) for (int y = 99; y <= 105; y++) {
            BlockPos at = new BlockPos(x, y, z); original.put(at, level.getBlockState(at));
        }
        Portal mine = portal(p, true, "mine", -1, "", 0);
        check(AdventurePortals.place(p, mine), "place menu arch");
        check(data.find(mine.id()).equals(mine), "persistent registration");
        check(!level.getBlockState(mine.at(-2, 0)).isAir() && level.getBlockState(mine.at(0, 1)).isAir(), "solid frame with walkable opening");
        check(AdventurePortals.passage(mine).contains(48.5, 101, 48.5), "X arch passage");
        var encoded = AdventurePortalData.CODEC.encodeStart(NbtOps.INSTANCE, data).getOrThrow();
        var reloaded = AdventurePortalData.CODEC.parse(NbtOps.INSTANCE, encoded).getOrThrow();
        check(mine.equals(reloaded.find(mine.id())), "save/load of destination and orientation");
        p.tickCount = 100; p.setPos(48.5, 100, 48.5);
        check(AdventurePortals.visit(p, mine), "first crossing activates");
        check(!AdventurePortals.visit(p, mine), "standing in arch does not retrigger");
        p.setPos(1000, 100, 1000);
        check(!AdventurePortals.visit(p, null), "travel away preserves entrance marker");
        p.setPos(48.5, 100, 48.5);
        check(!AdventurePortals.visit(p, mine), "returning inside entrance does not restart dungeon");
        p.setPos(48.5, 100, 50.5); AdventurePortals.visit(p, null);
        p.setPos(48.5, 100, 48.5);
        check(AdventurePortals.visit(p, mine), "stepping out re-arms portal");
        check(!AdventurePortals.place(p, portal(p, false, "plaines", 0, "", 0)), "overlap refused");
        check(AdventurePortals.undo(p), "undo placement");
        check(data.find(mine.id()) == null, "undo removes activation record");
        original.forEach((at, state) -> check(level.getBlockState(at).equals(state), "original terrain restored " + at));

        Portal plains = portal(p, false, "plaines", 0, "", 0);
        check(AdventurePortals.place(p, plains), "Z arch placement");
        check(AdventurePortals.passage(plains).contains(48.5, 101, 48.5), "Z arch passage");
        check(AdventurePortals.remove(p, plains), "remove arch");
        check(AdventurePortals.undo(p) && plains.equals(data.find(plains.id())), "undo deletion restores portal and destination");
        check(AdventurePortals.undo(p), "undo original placement after undo deletion");
        original.forEach((at, state) -> check(level.getBlockState(at).equals(state), "terrain after nested undo"));

        level.setBlockAndUpdate(mine.pos(), Blocks.CHEST.defaultBlockState());
        var chest = (net.minecraft.world.Container) level.getBlockEntity(mine.pos());
        chest.setItem(0, new net.minecraft.world.item.ItemStack(com.mmorpg.registry.ModItems.PIECE_OR.get(), 7));
        check(!AdventurePortals.place(p, mine) && chest.getItem(0).getCount() == 7, "placement preserves terrain and block entity inventory");
        level.setBlockAndUpdate(mine.pos(), Blocks.AIR.defaultBlockState());
        check(AdventurePortals.place(p, mine), "new placement after blocked attempt");
        BlockState owned = level.getBlockState(mine.at(-2, 0));
        level.setBlockAndUpdate(mine.at(-2, 0), Blocks.DIAMOND_BLOCK.defaultBlockState());
        check(!AdventurePortals.undo(p) && level.getBlockState(mine.at(-2, 0)).is(Blocks.DIAMOND_BLOCK), "undo preserves later builder edits");
        level.setBlockAndUpdate(mine.at(-2, 0), owned);
        check(AdventurePortals.undo(p), "history retained after conflict");

        var rpg = RpgPlayers.get(p);
        var saved = rpg.save();
        rpg.level = 1; rpg.minesUnlocked = 1; rpg.plainsUnlocked = 1;
        check(AdventurePortals.allowed(p, mine), "menu available with a class");
        check(!AdventurePortals.allowed(p, portal(p, true, "mine", 1, "", 0)), "locked mine refused");
        check(!AdventurePortals.allowed(p, portal(p, true, "plaines", 1, "", 0)), "locked plain refused");
        Portal dungeon = portal(p, true, "donjon", -1, "crypte_gobeline", 2);
        check(!AdventurePortals.allowed(p, dungeon), "dungeon level and difficulty respected");
        rpg.level = 100;
        check(AdventurePortals.allowed(p, dungeon), "eligible dungeon entry");
        rpg.pvpUntilTick = level.getServer().getTickCount() + 100;
        check(!AdventurePortals.allowed(p, dungeon), "PvP cannot be escaped by portal");
        rpg.pvpUntilTick = 0; rpg.load(saved);
        check(!AdventurePortals.valid(portal(p, true, "donjon", -1, "unknown", 0)), "unknown dungeon rejected");
        check(data.all().size() == baseline, "no orphaned portal records");
        p.setPos(48.5, 100, 44.5); p.setYRot(0);
        var commands = level.getServer().getCommands().getDispatcher();
        var source = level.getServer().createCommandSourceStack().withLevel(level).withEntity(p).withPosition(p.position()).withSuppressedOutput();
        check(commands.execute("portail mine", source) == 1, "operator placement command");
        String undoCommand = net.neoforged.fml.ModList.get().isLoaded("worldedit") ? "undoportail" : "undo";
        check(commands.execute(undoCommand, source) == 1 && data.all().size() == baseline, "bare /undo alias executes");
        check(AdventurePortals.place(p, mine), "first placement for multi undo");
        Portal second = new Portal(UUID.randomUUID(), level.dimension(), new BlockPos(48, 100, 52), true, "plaines", -1, "", 0);
        check(AdventurePortals.place(p, second), "second placement for multi undo");
        check(commands.execute(undoCommand + " 2", source) == 2 && data.all().size() == baseline, "/undo count traverses alias arguments");
        check(commands.execute("portail donjon crypte_gobeline heroique", source) == 1, "dungeon difficulty command");
        check(data.all().stream().anyMatch(g -> g.kind().equals("donjon") && g.difficulty() == 1), "selected dungeon difficulty saved");
        check(commands.execute("undoportail", source) == 1, "dedicated undo command");
        original.forEach((at, state) -> check(level.getBlockState(at).equals(state), "exact terrain after command undo"));
        com.mmorpg.MMORPG.LOGGER.info("[CODEX CHECKS] PORTALS PASS: placement, both orientations, persistence, overlap, undo/undo deletion, exact terrain restoration, containers, conflicts, access gates");
    }
}
