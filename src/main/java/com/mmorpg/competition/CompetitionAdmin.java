package com.mmorpg.competition;

import com.mmorpg.MMORPG;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

/** Operator-only tests use real combat instances, isolated from matchmaking and guild progression. */
@EventBusSubscriber(modid=MMORPG.MODID)
public final class CompetitionAdmin {
    public static void require(ServerPlayer p){
        if(!p.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))throw new IllegalArgumentException("Commande réservée aux administrateurs (OP niveau 2).");
    }
    public static void available(ServerPlayer p){
        if(ArenaManager.session(p.getUUID())!=null||ArenaManager.queued(p.getUUID())||GuildRaids.of(p.getUUID())!=null||com.mmorpg.dungeon.InstanceManager.containsPlayer(p)||com.mmorpg.hub.ClassTrial.active(p))throw new IllegalArgumentException("Quitte ton activité actuelle avant de lancer un test.");
    }
    public static void arrive(ServerPlayer p,String destination){
        require(p);String[] parts=destination.split(":");
        if(parts.length!=3)throw new IllegalArgumentException("Instruction de test invalide.");
        switch(parts[1]){
            case "arena" -> ArenaManager.test(p,ArenaRules.Kind.valueOf(parts[2]));
            case "boss" -> GuildRaids.test(p,Integer.parseInt(parts[2]));
            default -> throw new IllegalArgumentException("Test inconnu.");
        }
    }
    public static void stop(ServerPlayer p){
        require(p);var arena=ArenaManager.session(p.getUUID());var raid=GuildRaids.of(p.getUUID());
        if(arena!=null&&arena.test)ArenaManager.leave(p);
        else if(raid!=null&&raid.test)GuildRaids.leave(p);
        else throw new IllegalArgumentException("Tu n’es pas dans une instance de test.");
        p.sendSystemMessage(Component.literal("Test terminé : retour à ton point de départ."));
    }
    private static int run(net.minecraft.commands.CommandSourceStack source,java.util.function.Consumer<ServerPlayer> action)throws com.mojang.brigadier.exceptions.CommandSyntaxException{
        var p=source.getPlayerOrException();try{action.accept(p);return 1;}catch(IllegalArgumentException|IllegalStateException e){source.sendFailure(Component.literal(e.getMessage()));return 0;}
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent e){
        var root=Commands.literal("aeloriatest").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .executes(c->{c.getSource().sendSuccess(()->Component.literal("/aeloriatest boss <1–10> · /aeloriatest arene feu|dunes · /aeloriatest equipe rouge|bleu · /aeloriatest stop"),false);return 1;})
            .then(Commands.literal("boss").then(Commands.argument("numero",IntegerArgumentType.integer(1,10)).executes(c->run(c.getSource(),p->GuildRaids.test(p,IntegerArgumentType.getInteger(c,"numero")-1)))))
            .then(Commands.literal("stop").executes(c->run(c.getSource(),CompetitionAdmin::stop)));
        var arenas=Commands.literal("arene");
        for(var kind:ArenaRules.Kind.values())arenas.then(Commands.literal(kind.name().toLowerCase(java.util.Locale.ROOT)).executes(c->run(c.getSource(),p->ArenaManager.test(p,kind))));
        root.then(arenas).then(Commands.literal("equipe")
            .then(Commands.literal("rouge").executes(c->run(c.getSource(),p->ArenaManager.testTeam(p,0))))
            .then(Commands.literal("bleu").executes(c->run(c.getSource(),p->ArenaManager.testTeam(p,1)))));
        e.getDispatcher().register(root);
    }
}
