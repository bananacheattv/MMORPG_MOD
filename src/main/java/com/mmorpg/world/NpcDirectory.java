package com.mmorpg.world;

import com.mmorpg.MMORPG;
import com.mmorpg.entity.NpcEntity;
import com.mojang.serialization.Codec;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import java.util.*;

/** Persistent directory: unloading a chunk never removes its NPCs from the editor. */
@EventBusSubscriber(modid=MMORPG.MODID)
public final class NpcDirectory extends SavedData {
    public static final Codec<NpcDirectory> CODEC=CompoundTag.CODEC.listOf().xmap(NpcDirectory::new,d->new ArrayList<>(d.entries.values()));
    public static final SavedDataType<NpcDirectory> TYPE=new SavedDataType<>(MMORPG.id("npc_directory"),NpcDirectory::new,CODEC);
    private final Map<String,CompoundTag> entries=new LinkedHashMap<>();
    public NpcDirectory() {}
    private NpcDirectory(List<CompoundTag> list) {for(var t:list) entries.put(t.getStringOr("id",""),t);}
    public static NpcDirectory get(MinecraftServer server) {return server.overworld().getDataStorage().computeIfAbsent(TYPE);}
    public CompoundTag entry(String id) {return entries.get(id);}
    public ListTag snapshot() {var list=new ListTag();entries.values().forEach(t->list.add(t.copy()));return list;}
    public void record(NpcEntity npc) {
        var tag=new CompoundTag();String id=npc.getUUID().toString();
        tag.putString("id",id);tag.putString("name",npc.displayName());tag.putString("zone",npc.zone());
        tag.putString("group",npc.group());tag.putInt("skin",npc.skin());tag.putInt("role",npc.role().ordinal());
        tag.putString("dimension",npc.level().dimension().identifier().toString());tag.putLong("pos",npc.blockPosition().asLong());
        if(!tag.equals(entries.put(id,tag))) setDirty();
    }
    public void remove(String id) {if(entries.remove(id)!=null) setDirty();}
    @SubscribeEvent public static void joined(EntityJoinLevelEvent e) {
        if(e.getEntity() instanceof NpcEntity npc && e.getLevel() instanceof ServerLevel level) get(level.getServer()).record(npc);
    }
    @SubscribeEvent public static void left(EntityLeaveLevelEvent e) {
        if(e.getEntity() instanceof NpcEntity npc && e.getLevel() instanceof ServerLevel level
                && npc.getRemovalReason()!=null && npc.getRemovalReason().shouldDestroy()) get(level.getServer()).remove(npc.getUUID().toString());
    }
}
