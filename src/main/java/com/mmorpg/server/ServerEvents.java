package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.config.ConfigManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerAboutToStartEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/** Raccordement des evenements serveur aux differents systemes. */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class ServerEvents {
    private ServerEvents() {
    }

    @SubscribeEvent
    public static void onServerStarting(ServerAboutToStartEvent event) {
        ConfigManager.load();
        com.mmorpg.network.NetworkPlayers.start();
    }

    /** L'equipement n'est jamais perdu a la mort : seule une part d'experience l'est (voir RpgPlayers.onDeath). */
    @SubscribeEvent
    public static void onServerStarted(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        event.getServer().getGameRules().set(net.minecraft.world.level.gamerules.GameRules.KEEP_INVENTORY, true, event.getServer());
        event.getServer().getGameRules().set(net.minecraft.world.level.gamerules.GameRules.NATURAL_HEALTH_REGENERATION, false, event.getServer());
        // monde neuf : le hub est construit tout seul (un peu apres le demarrage), la premiere connexion s'y fait toujours
        var server = event.getServer();
        Scheduler.later(server.overworld(), 60, () -> com.mmorpg.hub.HubManager.ensureHub(server));
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        Scheduler.clear();
        PartyManager.clear();
        com.mmorpg.network.DungeonTravel.clear();
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        RpgCommands.register(event.getDispatcher());
        RpgCommands.registerHub(event.getDispatcher());
        com.mmorpg.mine.MineManager.register(event.getDispatcher());
        com.mmorpg.plains.PlainsManager.register(event.getDispatcher());
        com.mmorpg.zone.ZoneCommands.register(event.getDispatcher());
        RpgCommands.registerSetDonjon(event.getDispatcher());
        RpgCommands.registerSetMine(event.getDispatcher());
        RpgCommands.registerOutils(event.getDispatcher());
        PlayerCommands.register(event.getDispatcher());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        Scheduler.tick();
        PartyManager.tick(event.getServer());
        com.mmorpg.network.GroupTravel.tick(event.getServer());
        com.mmorpg.network.AdminTravel.tick(event.getServer());
        com.mmorpg.network.NetworkPlayers.tick(event.getServer());
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (event.getLevel() instanceof ServerLevel level) {
            MobSpawner.tick(level);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            RpgPlayers.tick(sp);
            AdventureSummary.tick(sp);
            sp.getFoodData().setFoodLevel(20);
            sp.getFoodData().setSaturation(5);
            if (sp.tickCount % 20 == 0) MountManager.maintain(sp);
        }
    }

    @SubscribeEvent(priority = net.neoforged.bus.api.EventPriority.HIGHEST)
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            if (!com.mmorpg.network.NetworkPlayers.login(sp)) return;
            com.mmorpg.dungeon.DungeonLobby.arriveIfNeeded(sp);
            RpgPlayers.onLogin(sp);
        }
    }

    @SubscribeEvent
    public static void onLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            com.mmorpg.network.NetworkPlayers.logout(sp);
            MountManager.despawn(sp);
            RpgPlayers.onLogout(sp);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            RpgPlayers.onRespawn(sp);
            if (!event.isEndConquered()) Scheduler.later(sp.level(), 5, () -> {
                if (!sp.isRemoved()) com.mmorpg.hub.HubManager.goToSpawn(sp);
            });
        }
    }

    @SubscribeEvent
    public static void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            MountManager.despawn(sp);
            PetManager.despawn(sp);
            RpgPlayers.recomputeAndSync(sp);
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level && event.getEntity() instanceof Mob mob) {
            MobLevels.initialize(mob, level);
        }
    }
}
