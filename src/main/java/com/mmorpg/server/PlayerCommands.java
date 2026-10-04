package com.mmorpg.server;

import com.mmorpg.rpg.PlayerData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** Commandes accessibles a tous les joueurs : groupe, chat de groupe, or. */
public final class PlayerCommands {
    private PlayerCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("groupe")
                .executes(c -> {
                    PartyManager.list(c.getSource().getPlayerOrException());
                    return 1;
                })
                .then(Commands.literal("inviter").then(Commands.argument("joueur", EntityArgument.player()).executes(c -> {
                    PartyManager.invite(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "joueur").getName().getString());
                    return 1;
                })))
                .then(Commands.literal("accepter").executes(c -> {
                    PartyManager.accept(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("refuser").executes(c -> {
                    PartyManager.decline(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("quitter").executes(c -> {
                    PartyManager.leave(c.getSource().getPlayerOrException());
                    return 1;
                }))
                .then(Commands.literal("exclure").then(Commands.argument("joueur", EntityArgument.player()).executes(c -> {
                    PartyManager.kick(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "joueur").getName().getString());
                    return 1;
                })))
                .then(Commands.literal("chef").then(Commands.argument("joueur", EntityArgument.player()).executes(c -> {
                    PartyManager.promote(c.getSource().getPlayerOrException(), EntityArgument.getPlayer(c, "joueur").getName().getString());
                    return 1;
                }))));

        d.register(Commands.literal("g").then(Commands.argument("message", StringArgumentType.greedyString()).executes(c -> {
            PartyManager.chat(c.getSource().getPlayerOrException(), StringArgumentType.getString(c, "message"));
            return 1;
        })));

        d.register(Commands.literal("or").executes(c -> {
            ServerPlayer p = c.getSource().getPlayerOrException();
            RpgPlayers.depositCoins(p);
            c.getSource().sendSuccess(() -> Component.literal("Vous possédez " + RpgPlayers.get(p).gold + " pièces d'or.").withColor(0xFFD040), false);
            return 1;
        }));

        d.register(Commands.literal("payer").then(Commands.argument("joueur", EntityArgument.player())
                .then(Commands.argument("montant", IntegerArgumentType.integer(1)).executes(c -> {
                    ServerPlayer from = c.getSource().getPlayerOrException();
                    ServerPlayer to = EntityArgument.getPlayer(c, "joueur");
                    int amount = IntegerArgumentType.getInteger(c, "montant");
                    if (to == from) {
                        c.getSource().sendFailure(Component.literal("Vous ne pouvez pas vous payer vous-même."));
                        return 0;
                    }
                    RpgPlayers.depositCoins(from);
                    if (!RpgPlayers.takeGold(from, amount)) {
                        c.getSource().sendFailure(Component.literal("Or insuffisant."));
                        return 0;
                    }
                    RpgPlayers.addGold(to, amount, true);
                    from.sendSystemMessage(Component.literal("Vous avez envoyé " + amount + " or à " + to.getName().getString() + ".").withColor(0xFFD040));
                    to.sendSystemMessage(Component.literal(from.getName().getString() + " vous a envoyé " + amount + " or.").withColor(0xFFD040));
                    return 1;
                }))));
    }

    static long gold(PlayerData d) {
        return d.gold;
    }
}
