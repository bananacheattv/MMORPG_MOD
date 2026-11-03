package com.mmorpg.server;

import com.mmorpg.config.ConfigManager;
import com.mmorpg.entity.NpcEntity;
import com.mmorpg.quest.QuestDef;
import com.mmorpg.registry.ModEntities;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;

/** Test opt-in du vrai parcours d'interaction et de la persistance, lancé avec -PlightChecks. */
public final class DevQuestTalkChecks {
    private static void check(boolean value,String label) { if(!value)throw new AssertionError("Talk quest: "+label); }
    public static void run(ServerPlayer p) {
        String key="codex_talk_check";
        var d=RpgPlayers.get(p); var before=d.save(); var original=ConfigManager.quests().get(key);
        var dir=com.mmorpg.world.NpcDirectory.get(p.level().getServer());
        var giver=new NpcEntity(ModEntities.NPC.get(),p.level());var target=new NpcEntity(ModEntities.NPC.get(),p.level());
        giver.setup(NpcEntity.Role.QUETES,"Donneur de test",0,"");target.setup(NpcEntity.Role.QUETES,"Interlocuteur de test",4,"");
        giver.snapTo(p.getX()+2,p.getY(),p.getZ(),0,0);target.snapTo(p.getX()+3,p.getY(),p.getZ(),0,0);
        p.level().addFreshEntity(giver);p.level().addFreshEntity(target);dir.record(giver);dir.record(target);
        try {
            d.playerClass=com.mmorpg.rpg.PlayerClass.GUERRIER; d.activeQuests.clear();
            var q=new QuestDef();q.name="Parler à un autre PNJ";q.npc=giver.getUUID().toString();
            var o=new QuestDef.Objective("talk",target.getUUID().toString(),1);q.objectives.add(o);
            QuestEditor.validate(key,q,dir);ConfigManager.quests().put(key,q);d.activeQuests.put(key,new int[1]);d.trackedQuest=key;d.questGuidanceMuted=false;
            var guide=GuidanceManager.resolve(p);
            check(guide!=null && guide.label().contains(target.displayName()),"navigation uses selected NPC");
            QuestManager.onAction(p,target.getUUID().toString(),1);
            check(QuestManager.progress(p,d,key,0)==0,"client action cannot spoof talking");
            QuestManager.onNpcTalk(p,giver);
            check(QuestManager.progress(p,d,key,0)==0,"wrong NPC rejected");
            target.setPos(p.getX()+100,p.getY(),p.getZ());QuestManager.onNpcTalk(p,target);
            check(QuestManager.progress(p,d,key,0)==0,"distant interaction rejected");
            target.setPos(p.getX()+3,p.getY(),p.getZ());
            target.interact(p,InteractionHand.MAIN_HAND,net.minecraft.world.phys.Vec3.ZERO);
            check(QuestManager.progress(p,d,key,0)==1,"actual NPC interaction progresses objective");
            QuestManager.onNpcTalk(p,target);
            check(QuestManager.progress(p,d,key,0)==1,"repeat interaction does not overflow");
            var restored=new com.mmorpg.rpg.PlayerData();restored.load(d.save());
            check(restored.activeQuests.get(key)[0]==1,"progress survives profile reload");
            o.target=java.util.UUID.randomUUID().toString();boolean denied=false;
            try {QuestEditor.validate(key,q,dir);} catch(IllegalArgumentException expected){denied=true;}
            check(denied,"unknown NPC rejected on save");
            o.target=giver.getUUID().toString();denied=false;
            try {QuestEditor.validate(key,q,dir);} catch(IllegalArgumentException expected){denied=true;}
            check(denied,"giver cannot be its own other NPC");
            com.mmorpg.MMORPG.LOGGER.info("[CODEX CHECKS] QUEST TALK PASS: NPC choice, real interaction, distance, spoof rejection, persistence, automatic guidance");
        } finally {
            giver.discard();target.discard();dir.remove(giver.getUUID().toString());dir.remove(target.getUUID().toString());
            if(original==null)ConfigManager.quests().remove(key);else ConfigManager.quests().put(key,original);
            d.load(before);d.dirty=true;
        }
    }
}
