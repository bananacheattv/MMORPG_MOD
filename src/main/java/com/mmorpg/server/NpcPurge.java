package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.entity.NpcEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;

/** /amenagement purgepnj : supprime tous les PNJ (entites + repertoire) et empeche leur reapparition jusqu'a /amenagement purgepnj recreer. */
@EventBusSubscriber(modid=MMORPG.MODID)
public final class NpcPurge {
    private static Path file(MinecraftServer s){return s.getWorldPath(LevelResource.ROOT).resolve("mmorpg_pnj_purges.txt");}
    private static Set<String> ids(MinecraftServer s){try{var f=file(s);return Files.exists(f)?new HashSet<>(Files.readAllLines(f)):Set.of();}catch(IOException e){return Set.of();}}
    /** Vrai tant qu'une purge est active : les PNJ automatiques du spawn ne sont pas recrees. */
    public static boolean active(MinecraftServer s){return Files.exists(file(s));}

    /** @return nombre de PNJ supprimes. */
    public static int purge(MinecraftServer s){
        var directory=com.mmorpg.world.NpcDirectory.get(s);
        var known=new LinkedHashSet<String>(ids(s));
        for(var t:directory.snapshot())if(t instanceof net.minecraft.nbt.CompoundTag n)known.add(n.getStringOr("id",""));
        int count=0;
        for(ServerLevel l:s.getAllLevels())for(var e:new ArrayList<>(StreamLoaded.npcs(l))){known.add(e.getUUID().toString());e.discard();count++;}
        for(String id:known){directory.remove(id);try{var u=UUID.fromString(id);for(ServerLevel l:s.getAllLevels()){var e=l.getEntity(u);if(e instanceof NpcEntity){e.discard();count++;}}}catch(IllegalArgumentException ignored){}}
        known.remove("");
        try{Files.write(file(s),known);}catch(IOException e){MMORPG.LOGGER.error("NPC purge save",e);}
        MMORPG.LOGGER.info("[MMORPG] Purge des PNJ : {} entites, {} ids memorises",count,known.size());
        return Math.max(count,known.size());
    }
    /** Leve l'interdiction : les PNJ automatiques reapparaissent a la prochaine connexion. */
    public static void allow(MinecraftServer s){try{Files.deleteIfExists(file(s));}catch(IOException e){MMORPG.LOGGER.error("NPC purge clear",e);}}

    /** Un PNJ d'une zone non chargee au moment de la purge est supprime des qu'il se recharge. */
    @SubscribeEvent public static void joined(EntityJoinLevelEvent e){
        if(e.getEntity() instanceof NpcEntity npc&&e.getLevel() instanceof ServerLevel l&&!e.getLevel().isClientSide()&&ids(l.getServer()).contains(npc.getUUID().toString())){
            e.setCanceled(true);com.mmorpg.world.NpcDirectory.get(l.getServer()).remove(npc.getUUID().toString());
        }
    }

    private static final class StreamLoaded{
        static List<NpcEntity> npcs(ServerLevel l){var out=new ArrayList<NpcEntity>();for(var e:l.getAllEntities())if(e instanceof NpcEntity n)out.add(n);return out;}
    }
}
