package com.mmorpg.replay;

import com.mmorpg.MMORPG;
import com.mmorpg.network.Net;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import java.util.*;

/** Operator authorization and safe spectator lifecycle. Titles/ranks never grant access. */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class ReplayServer {
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();
    private static final Map<UUID, Long> REQUESTS = new HashMap<>();
    /** Players who received a lease: only they need a revocation (no packet every second to every other player). */
    private static final Set<UUID> LEASED = new HashSet<>();
    private static final String RECOVERY="aeloria_replay_return";
    private record Session(GameType mode, ResourceKey<Level> dimension, Vec3 position, float yaw, float pitch, long deadline) {}
    public static boolean allowed(ServerPlayer p) {
        return p.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }
    private static void reply(ServerPlayer p, String action) { Net.toPlayer(p, new ReplayWire.Reply(action)); }
    @SubscribeEvent public static void commands(RegisterCommandsEvent e) {
        var root = Commands.literal("replay").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .executes(c -> { action(c.getSource().getPlayerOrException(), "open"); return 1; });
        for (String op : List.of("record", "play", "stop")) root.then(Commands.literal(op).executes(c -> {
            action(c.getSource().getPlayerOrException(), op); return 1;
        }));
        e.getDispatcher().register(root);
    }
    public static void action(ServerPlayer p, String action) {
        // Stop is always accepted for restoring a revoked operator's original state.
        if (action.equals("stop")) { restore(p); reply(p, "stop"); return; }
        if (!allowed(p)) { restore(p); reply(p, "revoked"); return; }
        long now = System.currentTimeMillis();
        Long last = REQUESTS.get(p.getUUID());
        if (last != null && now - last < 250) return;
        REQUESTS.put(p.getUUID(), now);
        switch (action) {
            case "open" -> reply(p, "open");
            case "record" -> {
                if (SESSIONS.containsKey(p.getUUID())) restore(p);
                reply(p, "record");
            }
            case "play" -> {
                if (p.isPassenger() || p.isSleeping() || !p.isAlive()) {
                    p.sendSystemMessage(Component.literal("Replay : quittez votre monture et attendez d'être debout et vivant."));
                    return;
                }
                SESSIONS.computeIfAbsent(p.getUUID(), id -> new Session(p.gameMode.getGameModeForPlayer(),
                        p.level().dimension(), p.position(), p.getYRot(), p.getXRot(), now + 120 * 60_000));
                Session s=SESSIONS.get(p.getUUID());
                CompoundTag saved=new CompoundTag();saved.putString("mode",s.mode().getName());saved.putString("dimension",s.dimension().identifier().toString());
                saved.putDouble("x",s.position().x);saved.putDouble("y",s.position().y);saved.putDouble("z",s.position().z);saved.putFloat("yaw",s.yaw());saved.putFloat("pitch",s.pitch());
                p.getPersistentData().put(RECOVERY,saved);
                p.setGameMode(GameType.SPECTATOR);
                reply(p, "play");
            }
        }
    }
    private static void restore(ServerPlayer p) {
        Session s = SESSIONS.remove(p.getUUID());
        if (s == null) return;
        p.setCamera(p);
        ServerLevel level = p.level().getServer().getLevel(s.dimension());
        if (level != null) p.teleportTo(level, s.position().x, s.position().y, s.position().z, Set.of(), s.yaw(), s.pitch(), true);
        // A revoked OP must not remain spectator because the creative-mode guard refuses restoration.
        GameType target=s.mode();
        if(!allowed(p)&&(target==GameType.CREATIVE||target==GameType.SPECTATOR))target=GameType.SURVIVAL;
        p.setGameMode(target);
        if(p.gameMode.getGameModeForPlayer()!=target)p.setGameMode(GameType.SURVIVAL);
        p.getPersistentData().remove(RECOVERY);
    }
    @SubscribeEvent public static void tick(ServerTickEvent.Post e) {
        if (e.getServer().getTickCount() % 20 != 0) return;
        long now = System.currentTimeMillis();
        for (ServerPlayer p : e.getServer().getPlayerList().getPlayers()) {
            Session s = SESSIONS.get(p.getUUID());
            if (!allowed(p) || (s != null && (now > s.deadline() || p.level().dimension() != s.dimension()
                    || p.gameMode.getGameModeForPlayer() != GameType.SPECTATOR))) {
                boolean leased = LEASED.remove(p.getUUID());
                if (s != null) restore(p);
                // A permission revocation must also stop a client-side recording/menu (sent once).
                if (s != null || leased) reply(p, "revoked");
            } else { LEASED.add(p.getUUID()); reply(p, "lease"); }
        }
    }
    @SubscribeEvent public static void logout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) { restore(p); REQUESTS.remove(p.getUUID()); LEASED.remove(p.getUUID()); }
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent e) {
        if(!(e.getEntity() instanceof ServerPlayer p)||!p.getPersistentData().contains(RECOVERY))return;
        try{
            var t=p.getPersistentData().getCompoundOrEmpty(RECOVERY);
            SESSIONS.put(p.getUUID(),new Session(GameType.byName(t.getStringOr("mode","survival")),ResourceKey.create(Registries.DIMENSION,Identifier.parse(t.getStringOr("dimension","minecraft:overworld"))),
                    new Vec3(t.getDoubleOr("x",0),t.getDoubleOr("y",80),t.getDoubleOr("z",0)),t.getFloatOr("yaw",0),t.getFloatOr("pitch",0),0));
            restore(p);reply(p,"stop");
        }catch(Exception ex){MMORPG.LOGGER.error("Replay : récupération de session impossible pour {}",p.getUUID(),ex);}
    }
    @SubscribeEvent public static void stopping(ServerStoppingEvent e){for(var p:e.getServer().getPlayerList().getPlayers())restore(p);}
    @SubscribeEvent public static void stopped(ServerStoppedEvent e) { SESSIONS.clear(); REQUESTS.clear(); LEASED.clear(); }
}
