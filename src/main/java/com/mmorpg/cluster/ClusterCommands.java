package com.mmorpg.cluster;

import com.mmorpg.MMORPG;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Commandes d'exploitation du reseau (operateurs) : /reseau etat, /reseau aller hub|spawn|mines|plains [numero]. */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class ClusterCommands {
    private ClusterCommands() {
    }

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        var go = Commands.literal("aller");
        for (Cluster.Place place : Cluster.Place.values()) {
            go.then(Commands.literal(place.id).executes(c -> go(c.getSource().getPlayerOrException(), place, 0))
                    .then(Commands.argument("numero", IntegerArgumentType.integer(1, 12)).executes(c ->
                            go(c.getSource().getPlayerOrException(), place, IntegerArgumentType.getInteger(c, "numero") - 1))));
        }
        event.getDispatcher().register(Commands.literal("reseau").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("etat").executes(c -> {
                    c.getSource().sendSuccess(() -> Component.literal(Cluster.describe()), false);
                    return 1;
                }))
                .then(go));
    }

    /** Envoie le joueur au lieu demande : sur un autre serveur s'il y est heberge, sinon ici (hub, spawn, mines, plaines). */
    private static int go(ServerPlayer p, Cluster.Place place, int arg) {
        switch (place) {
            case HUB -> com.mmorpg.hub.HubManager.goToHub(p);
            case SPAWN -> com.mmorpg.hub.HubManager.goToSpawn(p);
            case MINES -> com.mmorpg.mine.MineManager.startTransition(p, arg);
            case PLAINS -> com.mmorpg.plains.PlainsManager.startTransition(p, arg);
        }
        return 1;
    }
}
