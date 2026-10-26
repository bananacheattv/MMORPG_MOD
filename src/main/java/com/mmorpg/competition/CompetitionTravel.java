package com.mmorpg.competition;

import com.mmorpg.network.*;
import net.minecraft.server.level.ServerPlayer;

public final class CompetitionTravel {
    public static boolean route(ServerPlayer p,String destination){
        if(NetworkConfig.hosts(NetworkConfig.Role.DUNGEONS))return false;
        if(!NetworkPlayers.canTransfer(p,NetworkConfig.Role.DUNGEONS))return true;
        DungeonTravel.capture(p);NetworkPlayers.route(p,NetworkConfig.Role.DUNGEONS,destination);return true;
    }
    public static void home(ServerPlayer p){com.mmorpg.server.RpgPlayers.get(p).pvpUntilTick=0;if(!DungeonTravel.returnToSource(p))com.mmorpg.hub.HubManager.goToSpawn(p);}
    public static boolean inside(ServerPlayer p){return p.level().dimension().equals(CompetitionWorld.DIM);}
    public static void arrive(ServerPlayer p,String destination){try{if(destination.startsWith("arena:")){var bits=destination.split(":");ArenaManager.join(p,ArenaRules.Kind.valueOf(bits[1]),Integer.parseInt(bits[2]));}else if(destination.equals("guild-raid"))GuildRaids.enter(p);}catch(Exception e){p.sendSystemMessage(net.minecraft.network.chat.Component.literal(e.getMessage()));home(p);}}
}
