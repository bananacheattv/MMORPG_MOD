package com.mmorpg.client.screen;

import com.google.gson.Gson;
import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.*;
import com.mmorpg.cosmetic.Cosmetic;
import com.mmorpg.network.Payloads;
import com.mmorpg.pet.PetType;
import com.mmorpg.quest.QuestDef;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Items;
import java.util.*;
import java.util.function.Consumer;
import static com.mmorpg.client.screen.EditorChoiceScreen.Choice;

/** Full-window quest workshop. Drafts survive page switches, choosers and validation errors. */
public final class QuestEditorScreen extends MmoScreen {
    private static final Gson JSON=new Gson();
    private CompoundTag catalogue=new CompoundTag(),templates=new CompoundTag();
    private List<CompoundTag> npcs=new ArrayList<>();
    private QuestDef draft=new QuestDef();
    private String questId=newId(),query="",zone="Toutes",status="",npcId="",npcName="Nouveau PNJ",npcZone="",npcGroup="";
    private int mode=1,page,rewardPage,scroll,objective,item,npcSkin,npcRole;
    private int listW,rx,rw,fy;
    private boolean editingNpc,success=true,saving;
    private String expected="";
    private final List<Label> fieldLabels=new ArrayList<>();
    private record Label(String text,int x,int y) {}
    public QuestEditorScreen(CompoundTag data) {super("Atelier de quêtes");draft.name="Nouvelle quête";draft.objectives.add(new QuestDef.Objective());receive(data);}
    private static String newId() {return "quete_"+Long.toUnsignedString(System.currentTimeMillis(),36);}
    @Override protected int panelWidth() {return width-12;}
    @Override protected int panelHeight() {return height-12;}
    public void receive(CompoundTag data) {
        saving=false;status=data.getStringOr("status","");success=data.getBooleanOr("success",true);
        if(data.contains("quests")) {catalogue=data.getCompoundOrEmpty("quests");templates=data.getCompoundOrEmpty("templates");npcs=new ArrayList<>();
            var list=data.getListOrEmpty("npcs");for(int i=0;i<list.size();i++) npcs.add(list.getCompoundOrEmpty(i));}
        String selected=data.getStringOr("npcSelected","");
        if(!selected.isEmpty()) {npcId=selected;}
        if(success&&data.getStringOr("saved","").equals(questId)) expected=catalogue.getStringOr(questId,"");
    }
    private void rebuild() {clearWidgets();fieldLabels.clear();init();}
    public void showPage(int p) {editingNpc=false;page=p;rebuild();}
    @Override protected void init() {
        super.init();fieldLabels.clear();
        listW=Math.min(180,Math.max(120,pw/3));rx=left+listW+20;rw=pw-listW-30;fy=top+60;
        field(left+9,top+50,listW-2,"Recherche",query,v->{query=v;scroll=0;},100,false);
        if(editingNpc) {
            field(rx,fy,rw,"Nom du PNJ",npcName,v->npcName=v,80,false);
            field(rx,fy+35,rw,"Zone",npcZone,v->npcZone=v,80,false);
            field(rx,fy+70,rw,"Groupe de quêtes (facultatif)",npcGroup,v->npcGroup=v,80,false);
        } else switch(page) {
            case 0 -> {
                field(rx,fy,rw,"Titre de la quête",draft.name,v->draft.name=v,100,false);
                var id=field(rx,fy+32,rw-62,"Identifiant",questId,v->questId=v,80,false);id.setEditable(!catalogue.contains(questId));
                field(rx+rw-55,fy+32,55,"Niveau",Integer.toString(draft.minLevel),v->draft.minLevel=number(v),3,true);
                fieldLabels.add(new Label("Histoire / lore",rx,fy+54));
                var lore=MultiLineEditBox.builder().setX(rx).setY(fy+65).setShowDecorations(false)
                        .setPlaceholder(Component.literal("Écrivez ici le récit, les dialogues et les instructions…"))
                        .build(font,rw,Math.max(32,ph-166),Component.literal("Lore de la quête"));
                lore.setCharacterLimit(4000);lore.setValue(draft.description);lore.setValueListener(v->draft.description=v);addRenderableWidget(lore);
            }
            case 1 -> {
                if(!draft.objectives.isEmpty()) {
                    objective=Math.min(objective,draft.objectives.size()-1);var o=draft.objectives.get(objective);
                    field(rx,fy+70,85,"Quantité / niveau",Integer.toString(o.count),v->o.count=number(v),6,true);
                }
            }
            case 2 -> {
                if(rewardPage==0) {
                    field(rx,fy+36,(rw-8)/2,"Expérience (XP)",Long.toString(draft.rewards.xp),v->draft.rewards.xp=longNumber(v),10,true);
                    field(rx+(rw+8)/2,fy+36,(rw-8)/2,"Pièces d’or",Long.toString(draft.rewards.gold),v->draft.rewards.gold=longNumber(v),10,true);
                    field(rx,fy+73,100,"XP : % du niveau",Double.toString(draft.rewards.xpPercentOfLevel),v->{try{draft.rewards.xpPercentOfLevel=Double.parseDouble(v);}catch(Exception e){draft.rewards.xpPercentOfLevel=0;}},6,true);
                } else if(rewardPage==1&&!draft.rewards.items.isEmpty()) {
                    item=Math.min(item,draft.rewards.items.size()-1);var reward=draft.rewards.items.get(item);
                    field(rx,fy+85,85,"Quantité",Integer.toString(reward.count),v->reward.count=number(v),4,true);
                }
            }
            case 3 -> field(rx,fy+65,rw,"Groupe de PNJ (facultatif)",draft.giver,v->draft.giver=v,80,false);
        }
    }
    private EditBox field(int x,int y,int w,String label,String value,Consumer<String> change,int max,boolean numeric) {
        fieldLabels.add(new Label(label,x,y-10));
        boolean decimal=label.contains("%");
        var box=new EditBox(font,x,y,w,18,Component.literal(label)) {
            @Override public void insertText(String text) {
                if(numeric && !text.matches(decimal?"[0-9.]*":"[0-9]*")) return;
                if(decimal && (getValue().replace(getHighlighted(),"")+text).chars().filter(c->c=='.').count()>1) return;
                super.insertText(text);
            }
        };box.setMaxLength(max);
        box.setValue(value);box.setResponder(change);addRenderableWidget(box);return box;
    }
    private static int number(String v) {try{return Integer.parseInt(v);}catch(Exception e){return 0;}}
    private static long longNumber(String v) {try{return Long.parseLong(v);}catch(Exception e){return 0;}}
    private void choose(String title,List<Choice> list,Consumer<String> action) {minecraft.gui.setScreen(new EditorChoiceScreen(this,title,list,action));}
    private void chooseZone() {
        var choices=new ArrayList<Choice>();choices.add(new Choice("Toutes","Toutes les zones"));
        npcs.stream().map(n->n.getStringOr("zone","Sans zone")).distinct().sorted().forEach(z->choices.add(new Choice(z,z)));
        choose("Filtrer par zone",choices,v->{zone=v;scroll=0;});
    }
    private void selectNpc(CompoundTag n) {
        editingNpc=true;npcId=n.getStringOr("id","");npcName=n.getStringOr("name","");npcZone=n.getStringOr("zone","");
        npcGroup=n.getStringOr("group","");npcSkin=n.getIntOr("skin",0);npcRole=n.getIntOr("role",0);rebuild();
    }
    private void selectQuest(String id,boolean template) {
        draft=JSON.fromJson((template?templates:catalogue).getStringOr(id,"{}"),QuestDef.class);
        questId=template?newId():id;expected=template?"":catalogue.getStringOr(id,"");editingNpc=false;objective=0;item=0;page=0;rebuild();
    }
    private String npcLabel(String id) {return npcs.stream().filter(n->n.getStringOr("id","").equals(id)).map(n->n.getStringOr("name","")).findFirst().orElse("Tous les PNJ compatibles");}
    private List<Choice> npcChoices() {
        var choices=new ArrayList<Choice>();choices.add(new Choice("","Tous les PNJ compatibles"));
        npcs.stream().filter(n->n.getIntOr("role",0)==0).sorted(Comparator.comparing(n->n.getStringOr("name","")))
                .forEach(n->choices.add(new Choice(n.getStringOr("id",""),n.getStringOr("name","")+" — "+n.getStringOr("zone","Sans zone"))));return choices;
    }
    private List<Choice> itemChoices() {
        return BuiltInRegistries.ITEM.stream().filter(i->i!=Items.AIR).map(i->new Choice(BuiltInRegistries.ITEM.getKey(i).toString(),new net.minecraft.world.item.ItemStack(i).getHoverName().getString()))
                .sorted(Comparator.<Choice,Boolean>comparing(c->!c.id().startsWith("mmorpg:")).thenComparing(Choice::label)).toList();
    }
    private String itemName(String id) {var item=com.mmorpg.crafting.ForgeRecipes.resolveItem(id);return item==null?id:new net.minecraft.world.item.ItemStack(item).getHoverName().getString();}
    private void target(QuestDef.Objective o) {
        if(o.type.equals("collect")) choose("Objet à rapporter",itemChoices(),id->o.target=id);
        else {
            var list=new ArrayList<Choice>();list.add(new Choice("*","Tous les monstres"));list.add(new Choice("boss","Tous les boss"));
            BuiltInRegistries.ENTITY_TYPE.forEach(t->{var id=BuiltInRegistries.ENTITY_TYPE.getKey(t);list.add(new Choice(id.getNamespace().equals("mmorpg")?id.getPath():id.toString(),t.getDescription().getString()));});
            choose("Monstre à vaincre",list,id->o.target=id);
        }
    }
    public void save() {
        if(saving) return;var data=new CompoundTag();
        if(editingNpc) {
            data.putString("action","save_npc");data.putString("id",npcId);data.putString("name",npcName);data.putString("zone",npcZone);
            data.putString("group",npcGroup);data.putInt("skin",npcSkin);data.putInt("role",npcRole);
        } else {
            data.putString("action","save_quest");data.putString("id",questId);data.putString("json",JSON.toJson(draft));
            data.putString("expected",expected);
        }
        saving=true;status="Enregistrement…";ClientNet.send(new Payloads.QuestEdit(data));
    }
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy) {
        if(x<rx) {scroll=Math.max(0,scroll-(int)Math.signum(sy));return true;}
        return super.mouseScrolled(x,y,sx,sy);
    }
    @Override protected void renderContent(GuiGraphicsExtractor g,int mx,int my,float a) {
        Ui.panel(g,left,top,pw,ph);Ui.title(g,left+pw/2,top+7,"ATELIER DE QUÊTES",Ui.GOLD);
        button(g,left+pw-24,top+5,18,16,"↻",!saving,Ui.GOLD,()->{var t=new CompoundTag();t.putString("action","refresh");ClientNet.send(new Payloads.QuestEdit(t));});
        String[] modes={"PNJ","Quêtes","Modèles"};int cw=(listW-4)/3;
        for(int i=0;i<3;i++) {int m=i;button(g,left+8+i*cw,top+25,cw-1,15,modes[i],true,mode==i?Ui.GOLD:Ui.MUTED,()->{mode=m;scroll=0;});}
        button(g,left+8,top+73,listW,17,font.plainSubstrByWidth("Zone : "+zone+" ▾",listW-8),true,Ui.GOLD,this::chooseZone);
        int y=top+96,rows=Math.max(1,(ph-152)/19);
        List<Choice> entries=new ArrayList<>();
        if(mode==0) {
            for(var n:npcs) if(zone.equals("Toutes")||zone.equals(n.getStringOr("zone","Sans zone"))) entries.add(new Choice(n.getStringOr("id",""),n.getStringOr("name","")));
        } else {
            var source=mode==2?templates:catalogue;
            for(var id:source.keySet()) {var q=JSON.fromJson(source.getStringOr(id,"{}"),QuestDef.class);
                boolean matches=zone.equals("Toutes")||npcs.stream().anyMatch(n->zone.equals(n.getStringOr("zone",""))&&(q.npc==null||q.npc.isEmpty()?q.giver.isEmpty()||q.giver.equals(n.getStringOr("group","")):q.npc.equals(n.getStringOr("id",""))));
                if(matches) entries.add(new Choice(id,q.name));}
        }
        var filtered=entries.stream().filter(c->(c.label()+" "+c.id()).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))).sorted(Comparator.comparing(Choice::label)).toList();
        scroll=Math.min(scroll,Math.max(0,filtered.size()-rows));
        for(int i=0;i<Math.min(rows,filtered.size()-scroll);i++) {var c=filtered.get(i+scroll);
            button(g,left+8,y+i*19,listW,17,font.plainSubstrByWidth(c.label(),listW-8),true,Ui.GOLD,()->{
                if(mode==0) npcs.stream().filter(n->n.getStringOr("id","").equals(c.id())).findFirst().ifPresent(this::selectNpc);
                else selectQuest(c.id(),mode==2);
            });
        }
        g.text(font,filtered.size()+" entrée(s) • molette",left+8,top+ph-50,Ui.MUTED,false);
        button(g,left+8,top+ph-35,listW,17,mode==0?"+ Créer un PNJ ici":"+ Nouvelle quête",true,Ui.GREEN,()->{
            if(mode==0) {npcId="";npcName="Nouveau PNJ";npcZone=zone.equals("Toutes")?"":zone;npcGroup="";npcSkin=0;npcRole=0;editingNpc=true;}
            else {draft=new QuestDef();draft.name="Nouvelle quête";draft.objectives.add(new QuestDef.Objective());questId=newId();expected="";editingNpc=false;page=0;}rebuild();
        });
        g.fill(rx-6,top+25,rx-5,top+ph-12,Ui.GOLD);
        if(editingNpc) renderNpc(g);else {
            String[] pages=rw<300?new String[]{"Lore","Objectifs","Récomp.","Donneur"}:new String[]{"Lore","Objectifs","Récompenses","Conditions"};int tw=rw/4;
            for(int i=0;i<4;i++) {int p=i;button(g,rx+i*tw,top+25,tw-2,17,pages[i],true,page==i?Ui.GOLD:Ui.MUTED,()->showPage(p));}
            switch(page) {case 1->renderObjectives(g);case 2->renderRewards(g);case 3->renderConditions(g);default->{}}
        }
        for(var label:fieldLabels) g.text(font,label.text(),label.x(),label.y(),Ui.MUTED,false);
        if(!status.isEmpty()) {g.text(font,font.plainSubstrByWidth(status,rw),rx,top+ph-38,success?Ui.GREEN:Ui.RED,false);
            if(hovered(rx,top+ph-40,rw,13)) tooltip(g,List.of(Component.literal(status)));}
        button(g,rx,top+ph-23,rw/2-3,17,saving?"Enregistrement…":"Enregistrer",!saving,Ui.GREEN,this::save);
        button(g,rx+rw/2+3,top+ph-23,rw/2-3,17,editingNpc?"Fermer":"Dupliquer",true,Ui.GOLD,()->{
            if(editingNpc) onClose();else {questId=newId();expected="";draft.name+=" (copie)";status="Copie à enregistrer";page=0;rebuild();}
        });
    }
    private void renderNpc(GuiGraphicsExtractor g) {
        Ui.header(g,rx,top+27,rw,npcId.isEmpty()?"CRÉER UN PNJ":"MODIFIER LE PNJ");
        button(g,rx,fy+97,rw/2-3,18,"Apparence : "+(npcSkin+1),true,Ui.GOLD,()->npcSkin=(npcSkin+1)%4);
        button(g,rx+rw/2+3,fy+97,rw/2-3,18,npcRole==0?"Quêtes":"Marchand",true,Ui.GOLD,()->npcRole=1-npcRole);
        if(!npcId.isEmpty()) npcs.stream().filter(n->npcId.equals(n.getStringOr("id",""))).findFirst().ifPresent(n->{
            var pos=net.minecraft.core.BlockPos.of(n.getLongOr("pos",0));
            g.text(font,pos.getX()+", "+pos.getY()+", "+pos.getZ(),rx,fy+123,Ui.MUTED,false);
        });
    }
    private void renderObjectives(GuiGraphicsExtractor g) {
        if(!draft.objectives.isEmpty()) {
            var o=draft.objectives.get(objective);
            button(g,rx,fy,rw,19,switch(o.type){case "collect"->"Rapporter des objets ▾";case "level"->"Atteindre un niveau ▾";default->"Vaincre des monstres ▾";},true,Ui.GOLD,
                    ()->choose("Type d’objectif",List.of(new Choice("kill","Vaincre des monstres"),new Choice("collect","Rapporter des objets"),new Choice("level","Atteindre un niveau")),v->{o.type=v;o.target=v.equals("collect")?"minecraft:iron_ingot":"*";}));
            if(!o.type.equals("level")) button(g,rx,fy+31,rw,21,font.plainSubstrByWidth(o.type.equals("collect")?itemName(o.target):o.target.equals("*")?"Tous les monstres":o.target.equals("boss")?"Tous les boss":Component.translatable("entity."+o.target.replace(':','.').replaceFirst("^(?!.*\\.).*", "mmorpg."+o.target)).getString(),rw-10),true,Ui.GOLD,()->target(o));
            button(g,rx+94,fy+70,25,18,"◀",objective>0,Ui.GOLD,()->{objective--;rebuild();});
            button(g,rx+122,fy+70,25,18,"▶",objective+1<draft.objectives.size(),Ui.GOLD,()->{objective++;rebuild();});
            g.text(font,(objective+1)+" / "+draft.objectives.size(),rx+155,fy+75,Ui.TEXT,false);
        }
        button(g,rx,fy+106,rw/2-3,18,"+ Objectif",draft.objectives.size()<16,Ui.GREEN,()->{draft.objectives.add(new QuestDef.Objective());objective=draft.objectives.size()-1;rebuild();});
        button(g,rx+rw/2+3,fy+106,rw/2-3,18,"Retirer",!draft.objectives.isEmpty(),Ui.RED,()->{draft.objectives.remove(objective);objective=Math.max(0,objective-1);rebuild();});
    }
    private void renderRewards(GuiGraphicsExtractor g) {
        String[] tabs={"Or / XP","Objets","Familier","Cosmétique"};int tw=rw/4;
        for(int i=0;i<4;i++) {int p=i;button(g,rx+i*tw,fy-8,tw-2,17,tabs[i],true,rewardPage==i?Ui.GOLD:Ui.MUTED,()->{rewardPage=p;rebuild();});}
        if(rewardPage==1) {
            if(!draft.rewards.items.isEmpty()) {var r=draft.rewards.items.get(item);
                button(g,rx,fy+32,rw,24,font.plainSubstrByWidth(itemName(r.item),rw-10),true,Ui.GOLD,()->choose("Récompense : objet",itemChoices(),v->r.item=v));
                button(g,rx+94,fy+85,25,18,"◀",item>0,Ui.GOLD,()->{item--;rebuild();});
                button(g,rx+122,fy+85,25,18,"▶",item+1<draft.rewards.items.size(),Ui.GOLD,()->{item++;rebuild();});
                g.text(font,(item+1)+" / "+draft.rewards.items.size(),rx+155,fy+90,Ui.TEXT,false);
            }
            button(g,rx,fy+112,rw/2-3,18,"+ Objet",draft.rewards.items.size()<16,Ui.GREEN,()->{draft.rewards.items.add(new QuestDef.ItemReward("minecraft:diamond",1));item=draft.rewards.items.size()-1;rebuild();});
            button(g,rx+rw/2+3,fy+112,rw/2-3,18,"Retirer",!draft.rewards.items.isEmpty(),Ui.RED,()->{draft.rewards.items.remove(item);item=Math.max(0,item-1);rebuild();});
        } else if(rewardPage==2) {
            String name=Arrays.stream(PetType.values()).filter(p->p.id.equals(draft.rewards.pet)).map(p->p.label).findFirst().orElse("Aucun familier");
            button(g,rx,fy+35,rw,25,name+" ▾",true,Ui.GOLD,()->{var choices=new ArrayList<Choice>();choices.add(new Choice("","Aucun familier"));for(var p:PetType.values()) choices.add(new Choice(p.id,p.label));choose("Récompense : familier",choices,v->draft.rewards.pet=v);});
        } else if(rewardPage==3) {
            var c=Cosmetic.byId(draft.rewards.cosmetic);button(g,rx,fy+35,rw,25,c==null?"Aucun cosmétique ▾":font.plainSubstrByWidth(c.label+" ▾",rw-10),true,Ui.GOLD,()->{var choices=new ArrayList<Choice>();choices.add(new Choice("","Aucun cosmétique"));for(var cosmetic:Cosmetic.values()) choices.add(new Choice(cosmetic.id,cosmetic.label));choose("Récompense : cosmétique",choices,v->draft.rewards.cosmetic=v);});
        }
    }
    private void renderConditions(GuiGraphicsExtractor g) {
        button(g,rx,fy,rw,20,font.plainSubstrByWidth("PNJ : "+npcLabel(draft.npc)+" ▾",rw-8),true,Ui.GOLD,()->choose("PNJ donneur de la quête",npcChoices(),v->{draft.npc=v;draft.giver="";}));
        button(g,rx,fy+28,rw,18,draft.daily?"Quête journalière : oui":"Quête journalière : non",true,Ui.GOLD,()->draft.daily=!draft.daily);
        button(g,rx,fy+99,rw/2-3,19,"+ Prérequis",draft.requires.size()<16,Ui.GOLD,()->{
            var list=new ArrayList<Choice>();for(var id:catalogue.keySet()) if(!id.equals(questId)&&!draft.requires.contains(id)) list.add(new Choice(id,JSON.fromJson(catalogue.getStringOr(id,"{}"),QuestDef.class).name));
            choose("Quête à terminer avant",list,v->draft.requires.add(v));});
        button(g,rx+rw/2+3,fy+99,rw/2-3,19,"Retirer prérequis",!draft.requires.isEmpty(),Ui.RED,()->{
            var list=new ArrayList<Choice>();for(var id:draft.requires) list.add(new Choice(id,JSON.fromJson(catalogue.getStringOr(id,"{}"),QuestDef.class).name));choose("Retirer un prérequis",list,v->draft.requires.remove(v));});
    }
}
