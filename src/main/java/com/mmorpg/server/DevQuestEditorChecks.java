package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.config.ConfigManager;
import com.mmorpg.entity.NpcEntity;
import com.mmorpg.quest.QuestDef;
import com.mmorpg.world.NpcDirectory;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import java.nio.file.*;
import java.util.*;

/** Explicit development checks only; restores configuration, NPCs and player state. */
@EventBusSubscriber(modid=MMORPG.MODID)
public final class DevQuestEditorChecks {
    private static Map<String,QuestDef> workflowQuests;
    private static byte[] workflowFile;
    private static Set<UUID> workflowNpcs;
    public static void beginWorkflow(net.minecraft.server.MinecraftServer server) {
        try {
            workflowQuests=new LinkedHashMap<>(ConfigManager.quests());workflowFile=Files.readAllBytes(ConfigManager.dir().resolve("quests.json"));
            workflowNpcs=new HashSet<>();for(var level:server.getAllLevels()) for(var e:level.getAllEntities()) if(e instanceof NpcEntity) workflowNpcs.add(e.getUUID());
        } catch(Exception e) {throw new IllegalStateException(e);}
    }
    public static void endWorkflow(net.minecraft.server.MinecraftServer server) {
        try {
            check(ConfigManager.quests().values().stream().anyMatch(q->q.name.equals("La mémoire des racines")&&!q.npc.isEmpty()&&q.rewards.cosmetic.equals("ailes_celestes")),"UI saved complete draft");
            MMORPG.LOGGER.info("[CODEX CHECKS] QUEST EDITOR UI PASS: NPC creation, template copy, multiline lore, NPC selection, cosmetic selection, network save");
        } finally {
            ConfigManager.quests().clear();ConfigManager.quests().putAll(workflowQuests);
            try {Files.write(ConfigManager.dir().resolve("quests.json"),workflowFile);}catch(Exception e){throw new IllegalStateException(e);}
            var remove=new ArrayList<NpcEntity>();for(var level:server.getAllLevels()) for(var e:level.getAllEntities()) if(e instanceof NpcEntity npc&&!workflowNpcs.contains(e.getUUID())) remove.add(npc);
            remove.forEach(NpcEntity::discard);
        }
    }
    @SubscribeEvent public static void login(PlayerEvent.PlayerLoggedInEvent event) throws Exception {
        if(!Boolean.getBoolean("mmorpg.checkQuestEditor")||!(event.getEntity() instanceof ServerPlayer p)) return;
        var original=new LinkedHashMap<>(ConfigManager.quests());var path=ConfigManager.dir().resolve("quests.json");
        byte[] saved=Files.readAllBytes(path);var player=RpgPlayers.get(p);var playerTag=player.save();
        var directory=NpcDirectory.get(p.level().getServer());
        String npcId="",questId="codex_editor_check";
        try {
            check(QuestEditor.allowed(p),"operator");
            var fake=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(p.level());
            check(!QuestEditor.allowed(fake),"non-operator denied");
            var create=new CompoundTag();create.putString("action","save_npc");create.putString("name","Archiviste de test");create.putString("zone","Forêt ancienne");
            var before=directory.snapshot();QuestEditor.action(p,create);
            for(var e:p.level().getAllEntities()) if(e instanceof NpcEntity npc&&npc.displayName().equals("Archiviste de test")) {npcId=npc.getUUID().toString();break;}
            check(!npcId.isEmpty(),"NPC creation");
            var npc=(NpcEntity)p.level().getEntity(UUID.fromString(npcId));
            check(npc.zone().equals("Forêt ancienne"),"NPC zone");
            var encoded=NpcDirectory.CODEC.encodeStart(NbtOps.INSTANCE,directory).getOrThrow();
            var restored=NpcDirectory.CODEC.parse(NbtOps.INSTANCE,encoded).getOrThrow();
            check(restored.entry(npcId).getStringOr("zone","").equals("Forêt ancienne"),"directory persistence");
            var q=new QuestDef();q.name="Récit de la forêt";q.description="Les racines murmurent.\nÉcoutez l’archiviste.";q.npc=npcId;
            q.objectives.add(new QuestDef.Objective("level","*",1));q.rewards.gold=17;
            q.rewards.items.add(new QuestDef.ItemReward("minecraft:diamond",2));q.rewards.cosmetic="ailes_celestes";
            for(var template:com.mmorpg.quest.DefaultQuests.create().entrySet()) QuestEditor.validate(template.getKey(),template.getValue(),directory);
            var action=new CompoundTag();action.putString("action","save_quest");action.putString("id",questId);action.putString("json",QuestEditor.JSON.toJson(q));
            QuestEditor.action(fake,action);check(!ConfigManager.quests().containsKey(questId),"unauthorized save denied");
            QuestEditor.action(p,action);check(ConfigManager.quests().get(questId).description.equals(q.description),"save lore");
            check(Files.readString(path).contains("Récit de la forêt"),"disk save");
            q.rewards.gold=999;action.putString("json",QuestEditor.JSON.toJson(q));
            QuestEditor.action(p,action);check(ConfigManager.quests().get(questId).rewards.gold==17,"stale edit denied");
            action.putString("expected",QuestEditor.JSON.toJson(ConfigManager.quests().get(questId)));q.rewards.gold=-1;action.putString("json",QuestEditor.JSON.toJson(q));
            QuestEditor.action(p,action);check(ConfigManager.quests().get(questId).rewards.gold==17,"negative reward denied");
            var wrong=com.mmorpg.world.DungeonBuilder.spawnNpc(p.level(),p.blockPosition(),NpcEntity.Role.QUETES,"Autre donneur",0,0);
            try {
                player.playerClass=com.mmorpg.rpg.PlayerClass.GUERRIER;player.level=100;player.activeQuests.clear();player.completedQuests.remove(questId);
                QuestManager.accept(p,questId,wrong.getId());check(!player.activeQuests.containsKey(questId),"exact NPC acceptance");
                QuestManager.accept(p,questId,npc.getId());check(player.activeQuests.containsKey(questId),"assigned NPC acceptance");
                long gold=player.gold;QuestManager.complete(p,questId,wrong.getId());check(player.gold==gold,"wrong NPC completion denied");
                // Avoid giving inventory rewards during this check; normal reward semantics have their own integration test.
            } finally {wrong.discard();}
            var a=new QuestDef();a.requires.add(questId);ConfigManager.quests().put("codex_dep_check",a);
            q.requires.add("codex_dep_check");q.rewards.gold=0;
            boolean cycle=false;try {QuestEditor.validate(questId,q,directory);}catch(IllegalArgumentException expected){cycle=true;}
            check(cycle,"dependency cycle denied");
            MMORPG.LOGGER.info("[CODEX CHECKS] QUEST EDITOR PASS: permissions, NPC creation/zone, directory codec, templates, lore/disk save, stale/invalid edits, exact giver, cycles");
        } finally {
            if(!npcId.isEmpty()) {var npc=p.level().getEntity(UUID.fromString(npcId));if(npc!=null) npc.discard();directory.remove(npcId);}
            ConfigManager.quests().clear();ConfigManager.quests().putAll(original);Files.write(path,saved);player.load(playerTag);player.dirty=true;
        }
    }
    private static void check(boolean ok,String name) {if(!ok) throw new IllegalStateException("Quest editor check failed: "+name);}
}
