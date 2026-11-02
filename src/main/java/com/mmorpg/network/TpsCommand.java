package com.mmorpg.network;

import com.mmorpg.MMORPG;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import java.util.*;

/** /tps : TPS et MSPT du serveur courant, et de chaque serveur du réseau via le registre partagé. */
@EventBusSubscriber(modid=MMORPG.MODID)
public final class TpsCommand {
    public static final class Entry {public String role;public double mspt;public long seen;public int players;}
    public static final class State {public Map<String,Entry> servers=new HashMap<>();}
    private static final String KEY="server-tps";

    static double mspt(MinecraftServer s){return s.getCurrentSmoothedTickTime();}
    static double tps(double mspt){return Math.min(20.0,1000.0/Math.max(mspt,50.0));}

    public static void tick(MinecraftServer server){
        if(!NetworkConfig.current.enabled||server.getTickCount()%100!=0)return;
        try{SharedLedger.access(server,KEY,State.class,State::new,s->{
            long now=System.currentTimeMillis();s.servers.values().removeIf(x->now-x.seen>30000);
            Entry e=new Entry();e.role=NetworkConfig.current.role.name();e.mspt=mspt(server);e.seen=now;e.players=server.getPlayerList().getPlayerCount();
            s.servers.put(e.role,e);return null;
        });}catch(Exception ex){MMORPG.LOGGER.warn("TPS partagé: {}",ex.getMessage());}
    }
    private static String line(String name,double mspt,int players){
        String color=mspt<=50?"§a":mspt<=80?"§e":"§c";
        return "§7"+name+" §f: "+color+String.format(Locale.ROOT,"%.1f",tps(mspt))+" TPS §7· "+color+String.format(Locale.ROOT,"%.1f",mspt)+" ms/tick §7· "+players+" joueur(s)";
    }
    @SubscribeEvent public static void commands(RegisterCommandsEvent event){
        event.getDispatcher().register(Commands.literal("tps").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)).executes(c->{
            var src=c.getSource();var server=src.getServer();
            String here=NetworkConfig.current.enabled?NetworkConfig.current.role.name():"Serveur";
            src.sendSuccess(()->Component.literal(line(here+" (ici)",mspt(server),server.getPlayerList().getPlayerCount())),false);
            if(NetworkConfig.current.enabled){
                try{
                    var all=SharedLedger.access(server,KEY,State.class,State::new,s->new TreeMap<>(s.servers));
                    long now=System.currentTimeMillis();boolean any=false;
                    for(var en:all.entrySet()){
                        if(en.getKey().equals(here)||now-en.getValue().seen>30000)continue;any=true;
                        String l=line(en.getKey(),en.getValue().mspt,en.getValue().players);
                        src.sendSuccess(()->Component.literal(l),false);
                    }
                    if(!any)src.sendSuccess(()->Component.literal("§7Aucun autre serveur du réseau n'a répondu."),false);
                }catch(Exception ex){src.sendFailure(Component.literal("Réseau illisible : "+ex.getMessage()));}
            }else src.sendSuccess(()->Component.literal("§7Réseau désactivé : seul ce serveur est affiché."),false);
            return 1;
        }));
    }
}
