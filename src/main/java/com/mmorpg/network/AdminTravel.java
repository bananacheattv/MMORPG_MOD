package com.mmorpg.network;

import com.mmorpg.MMORPG;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import java.util.*;

/** Admin teleport across dimensions and backends, authorized by a short-lived server-created ticket. */
@EventBusSubscriber(modid=MMORPG.MODID)
public final class AdminTravel {
    private static final Set<UUID> OBSERVERS=new HashSet<>();
    public static boolean observing(ServerPlayer p){return OBSERVERS.contains(p.getUUID());}
    @SubscribeEvent public static void logout(net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent e){OBSERVERS.remove(e.getEntity().getUUID());}
    public static final class Location {public UUID id;public String name;public NetworkConfig.Role role;public long seen;}
    public static final class Ticket {public UUID id=UUID.randomUUID(),player,target;public long expires=System.currentTimeMillis()+30000;}
    public static final class State {public Map<UUID,Location> players=new HashMap<>();public Map<UUID,Ticket> tickets=new HashMap<>();}
    public static void tick(net.minecraft.server.MinecraftServer server){
        if(!NetworkConfig.current.enabled||server.getTickCount()%20!=0)return;
        try{SharedLedger.access(server,"admin-travel",State.class,State::new,s->{
            long now=System.currentTimeMillis();s.players.values().removeIf(x->now-x.seen>10000);s.tickets.values().removeIf(t->now>t.expires);
            for(var p:server.getPlayerList().getPlayers()){Location l=new Location();l.id=p.getUUID();l.name=p.getName().getString();l.role=NetworkConfig.current.role;l.seen=now;s.players.put(l.id,l);}return null;
        });}catch(Exception e){MMORPG.LOGGER.warn("Admin travel presence: {}",e.getMessage());}
    }
    public static void teleport(ServerPlayer p,String name){
        com.mmorpg.competition.CompetitionAdmin.require(p);
        var server=p.level().getServer();var local=server.getPlayerList().getPlayerByName(name);
        if(local!=null){move(p,local);return;}
        if(!NetworkConfig.current.enabled)throw new IllegalArgumentException("Joueur introuvable.");
        var target=SharedLedger.access(server,"admin-travel",State.class,State::new,s->s.players.values().stream().filter(x->x.name.equalsIgnoreCase(name)&&System.currentTimeMillis()-x.seen<10000).findFirst().orElseThrow(()->new IllegalArgumentException("Joueur hors ligne ou changement de serveur en cours.")));
        if(!NetworkPlayers.canTransfer(p,target.role))throw new IllegalArgumentException("Quitte ton activité en cours avant la téléportation.");
        var ticket=new Ticket();ticket.player=p.getUUID();ticket.target=target.id;
        SharedLedger.access(server,"admin-travel",State.class,State::new,s->{s.tickets.put(ticket.id,ticket);return null;});
        NetworkPlayers.route(p,target.role,"admin-tp:"+ticket.id);
    }
    public static void arrive(ServerPlayer p,String token){
        UUID id=UUID.fromString(token);var ticket=SharedLedger.access(p.level().getServer(),"admin-travel",State.class,State::new,s->{
            Ticket t=s.tickets.get(id);if(t==null||!t.player.equals(p.getUUID())||t.expires<System.currentTimeMillis())throw new IllegalArgumentException("Autorisation de téléportation expirée.");s.tickets.remove(id);return t;
        });
        var target=p.level().getServer().getPlayerList().getPlayer(ticket.target);
        if(target==null)throw new IllegalArgumentException("Le joueur a changé de serveur entre-temps : relance /atp.");
        move(p,target);
    }
    private static void move(ServerPlayer p,ServerPlayer target){
        if(com.mmorpg.competition.ArenaManager.session(p.getUUID())!=null||com.mmorpg.competition.GuildRaids.of(p.getUUID())!=null)throw new IllegalArgumentException("Quitte ton épreuve avant de te téléporter.");
        OBSERVERS.add(p.getUUID());
        p.stopRiding();p.teleportTo(target.level(),target.getX(),target.getY(),target.getZ(),Set.of(),target.getYRot(),target.getXRot(),true);p.resetFallDistance();
        p.sendSystemMessage(Component.literal("Téléporté vers "+target.getName().getString()+" · "+target.level().dimension().identifier()));
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        for(String alias:List.of("atp","tpreseau"))event.getDispatcher().register(Commands.literal(alias).requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
            .then(Commands.argument("joueur",StringArgumentType.word()).suggests((context,builder)->{
                var names=new HashSet<String>();for(var p:context.getSource().getServer().getPlayerList().getPlayers())names.add(p.getName().getString());
                if(NetworkConfig.current.enabled)try{names.addAll(SharedLedger.access(context.getSource().getServer(),"admin-travel",State.class,State::new,s->s.players.values().stream().filter(x->System.currentTimeMillis()-x.seen<10000).map(x->x.name).toList()));}catch(Exception ignored){}
                return net.minecraft.commands.SharedSuggestionProvider.suggest(names,builder);
            }).executes(c->{try{teleport(c.getSource().getPlayerOrException(),StringArgumentType.getString(c,"joueur"));return 1;}catch(IllegalArgumentException|IllegalStateException e){c.getSource().sendFailure(Component.literal(e.getMessage()));return 0;}})));
    }
}
