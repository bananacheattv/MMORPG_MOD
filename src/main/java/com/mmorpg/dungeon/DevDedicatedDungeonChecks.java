package com.mmorpg.dungeon;

import com.mmorpg.MMORPG;
import com.mmorpg.network.*;
import com.mmorpg.server.PartyManager;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Blocks;
import java.nio.file.Files;
import java.util.List;
import java.util.UUID;

/** Headless coverage for the dedicated host, authoritative groups and durable return tickets. */
public final class DevDedicatedDungeonChecks {
    private static void check(boolean value, String message) { if (!value) throw new AssertionError("dedicated dungeons: " + message); }
    public static void run(ServerPlayer p) throws Exception {
        NetworkConfig previous = NetworkConfig.current;
        var savedGroups = PartyManager.sharedSnapshot();
        var settings = new NetworkConfig(); settings.enabled = true; settings.role = NetworkConfig.Role.DUNGEONS;
        settings.sharedDirectory = Files.createTempDirectory("aeloria-dungeon-checks-").resolve("players").toString();
        settings.dungeonMaxInstancesPerPool = 3;
        var server = p.level().getServer();
        var mate = net.neoforged.neoforge.common.util.FakePlayerFactory.get(p.level(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "DungeonMate"));
        try {
            NetworkConfig.current = settings;
            check(NetworkConfig.hosts(NetworkConfig.Role.DUNGEONS) && !NetworkConfig.hosts(NetworkConfig.Role.MAIN), "dedicated role isolated");
            check(NetworkConfig.serverFor(NetworkConfig.Role.DUNGEONS).equals("donjons"), "dedicated proxy destination");
            DungeonLobby.ensure(server);
            check(server.overworld().getBlockState(new BlockPos(0, 99, 0)).is(com.mmorpg.registry.DecorBlocks.get("marbre_dore"))
                    && com.mmorpg.zone.ZoneData.get(server).byId("dungeon_lobby") != null, "safe protected waiting room");
            String pool = "crypte_gobeline:0";
            InstanceData.get(server).slots.removeIf(slot->slot.pool.equals(pool));InstanceData.get(server).dirty();
            check(InstanceManager.validPool(pool) && !InstanceManager.validPool("invalid") && !InstanceManager.validPool("crypte_gobeline:3"), "pool validation");
            PartyManager.Party group = new PartyManager.Party(); group.leader = p.getUUID(); group.members.add(p.getUUID()); group.members.add(mate.getUUID());
            SharedParties.mutate(p, () -> PartyManager.installShared(List.of(group)));
            PartyManager.clear(); SharedParties.refresh();
            check(PartyManager.sameParty(p, mate), "membership restored after JVM cache clear");
            check(InstanceManager.ownerKey(p).equals(InstanceManager.ownerKey(mate)), "arrival order preserves group instance owner");
            PartyManager.onLogout(mate);
            check(PartyManager.sameParty(p, mate), "proxy disconnect does not dissolve party");
            var first = InstanceManager.reserve(server, pool, InstanceManager.ownerKey(p));
            check(first != null && first == InstanceManager.reserve(server, pool, InstanceManager.ownerKey(mate)), "group members reserve one instance");
            var second = InstanceManager.reserve(server, pool, UUID.randomUUID().toString());
            var third = InstanceManager.reserve(server, pool, UUID.randomUUID().toString());
            check(second != null && third != null && first.index != second.index, "different groups isolated");
            check(InstanceManager.reserve(server, pool, UUID.randomUUID().toString()) == null, "configured capacity enforced");
            InstanceManager.release(server, pool, first.owner);
            check(InstanceManager.reserve(server, pool, "replacement") == first && InstanceManager.count(server, pool) == 3, "released slot reused");
            PartyManager.leave(mate); PartyManager.clear(); SharedParties.refresh();
            check(PartyManager.partyOf(p) == null && PartyManager.partyOf(mate) == null, "leave persisted without resurrection");
            PartyManager.installShared(List.of(group)); // simulate another JVM holding an obsolete cache
            SharedParties.mutate(p, () -> check(PartyManager.sharedSnapshot().isEmpty(), "mutation refreshes authoritative state before editing"));
            check(PartyManager.sharedSnapshot().isEmpty(), "stale cache cannot overwrite a remote departure");

            settings.role = NetworkConfig.Role.MAIN;
            check(InstanceManager.reserve(server, pool, "main-forbidden") == null, "MAIN cannot allocate dungeon instances");
            var pos = new BlockPos(64, 100, 64); p.level().getChunkAt(pos);
            p.level().setBlockAndUpdate(pos.below(), Blocks.STONE.defaultBlockState());
            for (int y = 0; y < 3; y++) p.level().setBlockAndUpdate(pos.above(y), Blocks.AIR.defaultBlockState());
            p.setPos(64.5, 100, 64.5); p.setYRot(70); p.setXRot(12);
            DungeonTravel.capture(p);
            var ticket = DungeonTravel.point(p);
            check(ticket.equals(DungeonTravel.ReturnPoint.read(ticket.save())), "return ticket round trip");
            var snapshot = NetworkPlayers.snapshot(p);
            DungeonTravel.forget(p); settings.role = NetworkConfig.Role.DUNGEONS;
            NetworkPlayers.restore(p, snapshot);
            check(ticket.equals(DungeonTravel.point(p)), "return server/position survive shared profile restore");
            var invalid = ticket.save(); invalid.putDouble("x", Double.NaN);
            try { DungeonTravel.ReturnPoint.read(invalid); throw new AssertionError("Nonfinite return accepted"); }
            catch (IllegalArgumentException expected) { }
            settings.role = NetworkConfig.Role.MAIN;
            // NeoForge FakePlayer's connection intentionally ignores teleports. Record the real request instead.
            final double[] destination = new double[4];
            var receiver = new net.neoforged.neoforge.common.util.FakePlayer(p.level(), new com.mojang.authlib.GameProfile(UUID.randomUUID(), "ReturnChecks")) {
                @Override public boolean teleportTo(net.minecraft.server.level.ServerLevel level, double x, double y, double z, java.util.Set<net.minecraft.world.entity.Relative> relative, float yaw, float pitch, boolean reset) {
                    destination[0] = x; destination[1] = y; destination[2] = z; destination[3] = yaw; return true;
                }
            };
            receiver.setPos(66.5, 100, 64.5); DungeonTravel.restore(receiver, ticket); DungeonTravel.arriveHome(receiver);
            check(destination[0] == 64.5 && destination[1] == 100 && destination[2] == 64.5 && destination[3] == 70 && DungeonTravel.point(receiver) == null, "return requests original entrance and consumes ticket");
            MMORPG.LOGGER.info("[CODEX CHECKS] DEDICATED_DUNGEONS PASS: role isolation, durable groups, stable instance owner, capacity/reuse, stale-cache protection, return ticket persistence and exact arrival");
        } finally {
            DungeonTravel.forget(p); InstanceManager.debugForgetAll(server);
            PartyManager.installShared(savedGroups); NetworkConfig.current = previous;
        }
    }
}
