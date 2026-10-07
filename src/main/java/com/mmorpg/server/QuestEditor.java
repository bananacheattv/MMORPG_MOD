package com.mmorpg.server;

import com.google.gson.Gson;
import com.mmorpg.config.ConfigManager;
import com.mmorpg.cosmetic.Cosmetic;
import com.mmorpg.entity.NpcEntity;
import com.mmorpg.network.*;
import com.mmorpg.pet.PetType;
import com.mmorpg.quest.QuestDef;
import com.mmorpg.world.NpcDirectory;
import com.mmorpg.registry.ModEntities;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.item.Items;
import java.util.*;

/** Operator-only authoring. Client forms never grant rewards or bypass normal quest checks. */
public final class QuestEditor {
    public static final Gson JSON=new Gson();
    public static boolean allowed(ServerPlayer p) {return p.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);}
    public static CompoundTag snapshot(ServerPlayer p) {
        var directory=NpcDirectory.get(p.level().getServer());
        for(var level:p.level().getServer().getAllLevels()) for(var e:level.getAllEntities()) if(e instanceof NpcEntity npc) directory.record(npc);
        var t=new CompoundTag();t.put("npcs",directory.snapshot());
        var quests=new CompoundTag();ConfigManager.quests().forEach((id,q)->quests.putString(id,JSON.toJson(q)));t.put("quests",quests);
        var templates=new CompoundTag();com.mmorpg.quest.DefaultQuests.create().forEach((id,q)->templates.putString(id,JSON.toJson(q)));t.put("templates",templates);
        return t;
    }
    public static void open(ServerPlayer p) {if(allowed(p)) Net.toPlayer(p,new S2COpenScreen(S2COpenScreen.QUEST_EDITOR,snapshot(p)));}
    public static void action(ServerPlayer p,CompoundTag data) {
        if(!allowed(p)) return;
        String status;String selectedNpc="";
        try {
            switch(data.getStringOr("action","")) {
                case "save_quest" -> {
                    String id=text(data,"id",80);require(id.matches("[a-z0-9_]+"),"Identifiant : lettres minuscules, chiffres et _.");
                    String json=text(data,"json",30000);QuestDef q=JSON.fromJson(json,QuestDef.class);
                    validate(id,q,NpcDirectory.get(p.level().getServer()));
                    var previous=ConfigManager.quests().get(id);
                    String expected=data.getStringOr("expected","");
                    require(expected.equals(previous==null?"":JSON.toJson(previous)),"Cette quête a été modifiée. Rechargez-la avant d’enregistrer.");
                    ConfigManager.saveQuest(id,q);
                    for(var online:p.level().getServer().getPlayerList().getPlayers()) RpgPlayers.get(online).dirty=true;
                    status="Quête enregistrée : "+q.name;
                }
                case "save_npc" -> {
                    String name=text(data,"name",80).trim(),zone=text(data,"zone",80).trim(),group=text(data,"group",80).trim();
                    require(!name.isEmpty(),"Donnez un nom au PNJ.");
                    int skin=data.getIntOr("skin",0);require(skin>=0&&skin<NpcEntity.SKINS,"Apparence invalide.");
                    int role=data.getIntOr("role",0);require(role>=0&&role<NpcEntity.Role.values().length,"Rôle invalide.");
                    String id=text(data,"id",80);NpcEntity npc=null;
                    if(!id.isEmpty()) {
                        UUID uuid=UUID.fromString(id);
                        for(var level:p.level().getServer().getAllLevels()) if(level.getEntity(uuid) instanceof NpcEntity found) npc=found;
                        require(npc!=null,"PNJ non chargé : rejoignez sa zone pour le modifier.");
                    } else {
                        npc=ModEntities.NPC.get().create(p.level(),EntitySpawnReason.COMMAND);
                        require(npc!=null,"Création du PNJ impossible.");
                        npc.snapTo(p.getX(),p.getY(),p.getZ(),p.getYRot()+180,0);
                    }
                    npc.setup(NpcEntity.Role.byId(role),name,skin,group);npc.setZone(zone.isEmpty()?"Sans zone":zone);
                    if(id.isEmpty()) require(p.level().addFreshEntity(npc),"Ajout du PNJ impossible.");
                    NpcDirectory.get(p.level().getServer()).record(npc);selectedNpc=npc.getUUID().toString();
                    status="PNJ enregistré : "+name;
                }
                case "refresh" -> status="Catalogue actualisé.";
                default -> throw new IllegalArgumentException("Action inconnue.");
            }
            var response=snapshot(p);response.putString("status",status);response.putBoolean("success",true);
            response.putString("npcSelected",selectedNpc);response.putString("saved",data.getStringOr("id",""));
            Net.toPlayer(p,new S2COpenScreen(S2COpenScreen.QUEST_EDITOR,response));
        } catch(Exception e) {
            var response=new CompoundTag();response.putString("status",e instanceof IllegalArgumentException?e.getMessage():"Enregistrement impossible. Consultez le journal serveur.");
            response.putBoolean("success",false);Net.toPlayer(p,new S2COpenScreen(S2COpenScreen.QUEST_EDITOR,response));
            if(!(e instanceof IllegalArgumentException)) com.mmorpg.MMORPG.LOGGER.error("Quest editor save failed",e);
        }
    }
    public static void validate(String id,QuestDef q,NpcDirectory directory) {
        require(q!=null,"Quête invalide.");
        require(q.name!=null&&!q.name.isBlank()&&q.name.length()<=100,"Nom : 1 à 100 caractères.");
        require(q.description!=null&&q.description.length()<=4000,"Lore : 4 000 caractères maximum.");
        require(q.minLevel>=1&&q.minLevel<=100,"Niveau : 1 à 100.");
        require(q.rewards!=null&&q.rewards.evolution>=0&&q.rewards.evolution<=4,"Évolution : 0 à 4.");
        require(q.giver!=null&&q.giver.length()<=80&&q.npc!=null&&q.npc.length()<=80,"PNJ invalide.");
        if(!q.npc.isEmpty()) {
            var npc=directory.entry(q.npc);require(npc!=null&&npc.getIntOr("role",-1)==0,"Sélectionnez un PNJ de quêtes.");
            require(q.giver.isEmpty()||q.giver.equalsIgnoreCase(npc.getStringOr("group","")),"Le groupe ne correspond pas au PNJ choisi.");
        }
        require(q.requires!=null&&q.requires.size()<=16,"Trop de prérequis.");
        for(var r:q.requires) {
            require(r!=null&&!r.equals(id)&&ConfigManager.quests().containsKey(r),"Quête préalable inconnue ou identique.");
            require(!dependsOn(r,id,new HashSet<>()),"Les prérequis créeraient une boucle entre les quêtes.");
        }
        require(q.objectives!=null&&!q.objectives.isEmpty()&&q.objectives.size()<=16,"Ajoutez 1 à 16 objectifs.");
        for(var o:q.objectives) {
            require(o!=null&&o.type!=null&&o.target!=null&&o.target.length()<=100&&o.count>=1&&o.count<=100000,"Objectif invalide.");
            switch(o.type) {
                case "level" -> require(o.count<=100,"Niveau cible : 1 à 100.");
                case "collect" -> require(validItem(o.target),"Objet à collecter inconnu : "+o.target);
                case "kill" -> require(o.target.equals("*")||o.target.equals("boss")||ConfigManager.mobs().containsKey(o.target)
                        || validEntity(o.target),"Monstre inconnu : "+o.target);
                default -> throw new IllegalArgumentException("Type d’objectif inconnu.");
            }
        }
        var r=q.rewards;require(r!=null&&r.items!=null&&r.items.size()<=16,"Récompenses invalides.");
        require(r.gold>=0&&r.gold<=1000000000L&&r.xp>=0&&r.xp<=1000000000L,"Or / XP : 0 à 1 milliard.");
        require(Double.isFinite(r.xpPercentOfLevel)&&r.xpPercentOfLevel>=0&&r.xpPercentOfLevel<=100,"Pourcentage XP invalide.");
        for(var item:r.items) require(item!=null&&validItem(item.item)&&item.count>=1&&item.count<=4096,"Objet de récompense invalide (1 à 4 096).");
        require(r.pet!=null&&(r.pet.isEmpty()||Arrays.stream(PetType.values()).anyMatch(p->p.id.equals(r.pet))),"Familier inconnu.");
        require(r.cosmetic!=null&&(r.cosmetic.isEmpty()||Cosmetic.byId(r.cosmetic)!=null),"Cosmétique inconnu.");
    }
    private static boolean validEntity(String value) {var id=Identifier.tryParse(value);return id!=null&&BuiltInRegistries.ENTITY_TYPE.containsKey(id);}
    private static boolean dependsOn(String key,String target,Set<String> seen) {
        if(key.equals(target)) return true;
        if(!seen.add(key)) return false;
        var q=ConfigManager.quests().get(key);
        return q!=null&&q.requires!=null&&q.requires.stream().anyMatch(r->dependsOn(r,target,seen));
    }
    private static boolean validItem(String value) {if(value==null) return false;var id=Identifier.tryParse(value.contains(":")?value:"mmorpg:"+value);return id!=null&&BuiltInRegistries.ITEM.containsKey(id)&&BuiltInRegistries.ITEM.getValue(id)!=Items.AIR;}
    private static String text(CompoundTag t,String key,int max) {String s=t.getStringOr(key,"");require(s.length()<=max,"Champ trop long : "+key);return s;}
    private static void require(boolean ok,String message) {if(!ok) throw new IllegalArgumentException(message);}
}
