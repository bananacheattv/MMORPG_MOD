package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.hub.HubManager;
import com.mmorpg.network.NetworkConfig;
import com.mmorpg.network.NetworkPlayers;
import com.mmorpg.network.ProxyConnect;
import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.world.AdventurePortalData;
import net.minecraft.core.BlockPos;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/** Opt-in native server regression: runServer -PopenworldPortalChecks -PqaDir=build/qa-openworld-portal. */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class DevOpenworldPortalChecks {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError("Openworld portal: " + message);
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void run(ServerStartedEvent event) throws Exception {
        if (!Boolean.getBoolean("mmorpg.checkOpenworldPortal")) return;
        var server = event.getServer();
        var p = net.neoforged.neoforge.common.util.FakePlayerFactory.get(server.overworld(),
                new com.mojang.authlib.GameProfile(UUID.randomUUID(), "OpenworldChecks"));
        // FakePlayer's default listener ignores teleports. Use the real server-side
        // movement handler, capturing outgoing packets instead of needing a client.
        var transfers = new java.util.ArrayList<ProxyConnect>();
        p.connection = new net.minecraft.server.network.ServerGamePacketListenerImpl(server,
                new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND), p,
                net.minecraft.server.network.CommonListenerCookie.createInitial(p.getGameProfile(), false)) {
            @Override public void send(net.minecraft.network.protocol.Packet<?> packet) {
                if (packet instanceof net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket custom
                        && custom.payload() instanceof ProxyConnect connect) transfers.add(connect);
            }
        };
        var previous = NetworkConfig.current;
        var config = new NetworkConfig();
        config.enabled = true;
        config.role = NetworkConfig.Role.MAIN;
        config.sharedDirectory = Files.createTempDirectory(Path.of("."), "portal-profiles-").toAbsolutePath().toString();
        var report = Path.of("openworld-portal-checks.txt");
        Files.deleteIfExists(report);
        try {
            NetworkConfig.current = config;
            check(NetworkPlayers.login(p), "source profile acquired");
            RpgPlayers.get(p).playerClass = PlayerClass.GUERRIER;
            RpgPlayers.get(p).gold = 456;
            var portals = AdventurePortalData.get(server);
            for (var old : java.util.List.copyOf(portals.all())) {
                if (old.dimension().equals(p.level().dimension()) && old.pos().closerThan(new BlockPos(48, 100, 48), 16)) {
                    portals.remove(old.id());
                }
            }
            for (int x = 42; x <= 54; x++) for (int z = 42; z <= 54; z++) {
                p.level().getChunkAt(new BlockPos(x, 99, z));
                p.level().setBlockAndUpdate(new BlockPos(x, 99, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                for (int y = 100; y <= 109; y++) p.level().setBlockAndUpdate(new BlockPos(x, y, z), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            }
            p.setPos(48.5, 100, 44.5);
            p.setYRot(0);
            var source = server.createCommandSourceStack().withLevel(p.level()).withEntity(p)
                    .withPosition(p.position()).withSuppressedOutput();
            check(server.getCommands().getDispatcher().execute("portail openworld", source) == 1,
                    "actual admin command places the portal");
            var portal = portals.all().stream().filter(v -> v.kind().equals("openworld")).findFirst().orElseThrow();
            var before = p.position();
            check(AdventurePortals.allowed(p, portal), "portal allows the chosen class");
            AdventurePortals.enter(p, portal);
            check(NetworkPlayers.inTransit(p) && p.position().equals(before), "source requests a transfer without a local teleport");
            check(NetworkPlayers.snapshot(p).getStringOr("destination", "").equals("openworld"), "arrival instruction saved");
            check(transfers.size() == 1, "portal emits exactly one proxy connection packet");
            var wire = new java.io.DataInputStream(new java.io.ByteArrayInputStream(transfers.getFirst().bytes()));
            check(wire.readUTF().equals("Connect") && wire.readUTF().equals("openworld") && wire.available() == 0,
                    "Velocity destination is the dedicated openworld backend");
            NetworkPlayers.logout(p);

            config.role = NetworkConfig.Role.OPENWORLD;
            p.setPos(150.5, 90, 150.5); // An old position saved by the destination server.
            check(NetworkPlayers.login(p) && NetworkPlayers.isArriving(p), "destination restores the pending instruction");
            var arrive = NetworkPlayers.class.getDeclaredMethod("arrive", net.minecraft.server.level.ServerPlayer.class, String.class);
            arrive.setAccessible(true);
            arrive.invoke(null, p, "openworld");
            check(p.level() == server.overworld() && p.position().distanceToSqr(.5, 84, .5) < .001,
                    "arrival uses the Openworld map entrance, not the old saved position");
            check(!NetworkPlayers.isArriving(p) && !NetworkPlayers.inTransit(p), "arrival completes without a routing loop");
            check(NetworkPlayers.portalBlocked(p), "arrival grace prevents immediate portal reentry");
            check(RpgPlayers.get(p).gold == 456, "shared character retained");
            check(NetworkPlayers.snapshot(p).getStringOr("destination", "").isEmpty(), "travel instruction consumed");
            NetworkPlayers.logout(p);

            p.setPos(210.5, 95, 210.5);
            check(NetworkPlayers.login(p) && !NetworkPlayers.isArriving(p), "normal reconnect has no travel instruction");
            arrive.invoke(null, p, "");
            check(p.position().distanceToSqr(210.5, 95, 210.5) < .001, "normal reconnect keeps exploration position");
            check(HubManager.goToOpenWorld(p) && !NetworkPlayers.inTransit(p)
                    && p.position().distanceToSqr(.5, 84, .5) < .001, "same-server portal uses the local entrance");
            config.role = NetworkConfig.Role.MAIN;
            p.setPos(48.5, 100, 44.5);
            arrive.invoke(null, p, "openworld");
            check(p.position().distanceToSqr(48.5, 100, 44.5) < .001, "arrival cannot teleport into the wrong backend's overworld");
            check(AdventurePortals.undo(p), "test portal removed and terrain restored");
            Files.writeString(report, "PASS: admin portal, MAIN -> OPENWORLD, exact arrival, character, grace, reconnect, backend guard\n");
            MMORPG.LOGGER.info("[CODEX CHECKS] OPENWORLD_PORTAL PASS");
        } finally {
            NetworkPlayers.logout(p);
            NetworkConfig.current = previous;
            server.halt(false);
        }
    }
}
