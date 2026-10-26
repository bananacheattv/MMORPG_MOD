package com.mmorpg.client.screen;

import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.*;
import com.mmorpg.guild.*;
import com.mmorpg.network.Payloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class GuildScreen extends MmoScreen {
    private final CompoundTag data;private final GuildStore.Guild guild;private final UUID self;private int tab,page,boss;private UUID selected;
    private EditBox name,amount,motto,confirmation;
    public void showTab(int value){tab=Math.clamp(value,0,5);page=0;}
    public GuildScreen(CompoundTag data){super("Guilde");this.data=data;String json=data.getStringOr("guild","");guild=json.isEmpty()?null:new com.google.gson.Gson().fromJson(json,GuildStore.Guild.class);self=UUID.fromString(data.getStringOr("self",new UUID(0,0).toString()));boss=guild==null?0:guild.level-1;}
    @Override protected int panelWidth(){return Math.min(width-12,720);}
    @Override protected int panelHeight(){return Math.min(height-12,340);}
    private EditBox field(int x,int y,int w,int max,String value){var f=new EditBox(font,x,y,w,18,Component.literal("Valeur"));f.setMaxLength(max);f.setValue(value);addRenderableWidget(f);return f;}
    @Override protected void init(){super.init();name=field(left+12,top+66,pw-24,28,"");amount=field(left+12,top+95,Math.min(180,pw-24),10,"1000");motto=field(left+12,top+90,pw-24,120,guild==null?"":guild.motto);confirmation=field(left+12,top+ph-71,pw/2-20,28,"");}
    private void send(String action,java.util.function.Consumer<CompoundTag> fill){var t=new CompoundTag();t.putString("action",action);fill.accept(t);ClientNet.send(new Payloads.SocialAction("guild",t));}
    private void send(String action){send(action,t->{});}
    private boolean chief(){return guild!=null&&guild.members.get(self).rank.equals("CHEF");}
    private boolean officer(){return guild!=null&&!guild.members.get(self).rank.equals("MEMBRE");}
    private void text(GuiGraphicsExtractor g,String value,int x,int y,int color){g.text(font,font.plainSubstrByWidth(value,pw-24),x,y,color,false);}
    @Override protected void renderContent(GuiGraphicsExtractor g,int mx,int my,float a){
        Ui.frame(g,left,top,pw,ph,Ui.GOLD,false);g.centeredText(font,Component.literal(guild==null?"GUILDES":guild.name+" · Niveau "+guild.level+" / 10"),left+pw/2,top+8,Ui.GOLD);
        for(var f:List.of(name,amount,motto,confirmation)){f.visible=false;f.active=false;}
        if(guild==null)noGuild(g);else{
            String[] tabs={"Guilde","Membres","Banque","Yggdrasil","Buffs","Journal"};int w=(pw-24)/6;for(int i=0;i<6;i++){int n=i;button(g,left+12+i*w,top+27,w-2,17,tabs[i],true,tab==i?Ui.GOLD:Ui.MUTED,()->{tab=n;page=0;});}
            switch(tab){case 0->home(g);case 1->members(g);case 2->bank(g);case 3->tree(g);case 4->buffs(g);default->history(g);}
        }
        text(g,data.getStringOr("status",""),left+12,top+ph-38,Ui.GOLD);
        button(g,left+12,top+ph-23,90,17,"Actualiser",true,Ui.GOLD,()->send("open"));
        if(guild!=null&&tab==3)button(g,left+pw/2-65,top+ph-23,130,17,"Quitter l’épreuve",true,Ui.MUTED,()->send("raidLeave"));
        button(g,left+pw-102,top+ph-23,90,17,"Fermer",true,Ui.MUTED,this::onClose);
    }
    private void visible(EditBox f){f.visible=true;f.active=true;}
    private void noGuild(GuiGraphicsExtractor g){text(g,"Crée une guilde ou accepte une invitation.",left+12,top+43,Ui.TEXT);visible(name);button(g,left+12,top+90,pw-24,18,"Fonder ma guilde",!name.getValue().isBlank(),Ui.GREEN,()->send("create",t->t.putString("name",name.getValue())));
        var invites=data.getListOrEmpty("invites");int rows=Math.max(1,(ph-162)/23);for(int i=0;i<rows&&i+page*rows<invites.size();i++){var invite=invites.getCompoundOrEmpty(i+page*rows);int y=top+118+i*23;String id=invite.getStringOr("id","");text(g,invite.getStringOr("name",""),left+12,y+4,Ui.TEXT);button(g,left+pw-148,y,65,18,"Accepter",true,Ui.GREEN,()->send("accept",t->t.putString("target",id)));button(g,left+pw-78,y,65,18,"Refuser",true,Ui.RED,()->send("decline",t->t.putString("target",id)));}}
    private void home(GuiGraphicsExtractor g){text(g,guild.members.size()+" / 50 membres · Banque : "+guild.gold+" or",left+12,top+53,Ui.TEXT);text(g,"XP de guilde : "+guild.xp+" / "+Yggdrasil.xpRequired(guild.level),left+12,top+70,Ui.TEXT);
        visible(motto);button(g,left+12,top+115,pw-24,18,"Enregistrer la devise",officer(),Ui.GOLD,()->send("motto",t->t.putString("text",motto.getValue())));
        if(chief()){visible(confirmation);button(g,left+pw/2,top+ph-71,pw/2-12,18,"Dissoudre (nom exact)",confirmation.getValue().equals(guild.name),Ui.RED,()->send("disband",t->t.putString("confirm",confirmation.getValue())));}else button(g,left+12,top+ph-71,pw-24,18,"Quitter la guilde",true,Ui.RED,()->send("leave"));}
    private void members(GuiGraphicsExtractor g){int half=pw/2,rows=Math.max(1,(ph-122)/21);var members=new ArrayList<>(guild.members.values());members.sort(Comparator.comparing((GuildStore.Member m)->m.rank.equals("CHEF")?0:m.rank.equals("OFFICIER")?1:2).thenComparing(m->m.name));
        for(int i=0;i<rows&&page*rows+i<members.size();i++){var m=members.get(page*rows+i);button(g,left+12,top+53+i*21,half-20,18,font.plainSubstrByWidth(m.name+" · "+m.rank,half-30),true,m.id.equals(selected)?Ui.GOLD:Ui.TEXT,()->selected=m.id);}
        var online=data.getListOrEmpty("online");int y=top+53;text(g,"Inviter un joueur",left+half,top+47,Ui.GOLD);for(int i=0;i<online.size()&&y<top+ph-88;i++){var entry=online.getCompoundOrEmpty(i);String id=entry.getStringOr("id","");if(guild.members.containsKey(UUID.fromString(id)))continue;button(g,left+half,y+12,pw-half-12,18,entry.getStringOr("name",""),officer(),Ui.GREEN,()->send("invite",t->t.putString("target",id)));y+=21;}
        button(g,left+12,top+ph-67,30,16,"‹",page>0,Ui.GOLD,()->page--);button(g,left+48,top+ph-67,30,16,"›",(page+1)*rows<members.size(),Ui.GOLD,()->page++);
        String[] actions={"promote","demote","kick","leader"},labels={"Officier","Membre","Exclure","Nommer chef"};int w=(pw-108)/4;for(int i=0;i<4;i++){String action=actions[i];button(g,left+92+i*w,top+ph-67,w-3,16,labels[i],selected!=null&&!selected.equals(self)&&(action.equals("kick")?officer():chief()),Ui.GOLD,()->send(action,t->t.putString("target",selected.toString())));}}
    private void bank(GuiGraphicsExtractor g){text(g,"BANQUE DE GUILDE : "+guild.gold+" OR",left+12,top+55,Ui.GOLD);text(g,"Ton or : "+data.getLongOr("wallet",0),left+12,top+74,Ui.TEXT);visible(amount);long n;try{n=Long.parseLong(amount.getValue());}catch(Exception e){n=0;}final long number=n;button(g,left+12,top+121,(pw-28)/2,20,"Déposer",n>0,Ui.GREEN,()->send("deposit",t->t.putLong("amount",number)));button(g,left+pw/2+2,top+121,(pw-28)/2,20,"Retirer (chef)",n>0&&chief(),Ui.GOLD,()->send("withdraw",t->t.putLong("amount",number)));text(g,"Les mouvements sont conservés dans le journal.",left+12,top+154,Ui.MUTED);}
    private void tree(GuiGraphicsExtractor g){int treeW=pw*43/100,cx=left+treeW/2,base=top+ph-55;for(int n=0;n<6;n++){int y=top+55+n*6,spread=32+Math.abs(3-n)*5;g.fill(cx-spread,y,cx+spread,y+9,n%2==0?0xFF335B46:0xFF487650);}g.fill(cx-5,top+60,cx+5,base,0xFF74543A);g.fill(cx-2,top+64,cx+1,base,0xFFB18B51);for(int n=0;n<5;n++){g.fill(cx-9-n*7,base+n,cx+9+n*7,base+n+3,0xFF74543A);}
        for(int i=0;i<10;i++){final int index=i;int row=i/2,y=base-19-row*24,x=cx+(i%2==0?-43:25);g.fill(Math.min(cx,x),y+8,Math.max(cx,x)+10,y+11,0xFF6D8F53);button(g,x,y,23,19,Integer.toString(i+1),true,Yggdrasil.cleared(guild,i)?Ui.GREEN:i==boss?Ui.GOLD:Ui.MUTED,()->boss=index);}
        int x=left+treeW+8,w=pw-treeW-20;var def=Yggdrasil.ALL[boss];g.text(font,font.plainSubstrByWidth(def.name(),w),x,top+54,0xFF000000|def.color(),false);g.text(font,font.plainSubstrByWidth(def.form(),w),x,top+73,Ui.TEXT,false);
        g.text(font,"Gardien "+(boss+1)+" / 10",x,top+94,Ui.GOLD,false);g.text(font,Yggdrasil.cleared(guild,boss)?"Vaincu":"Non vaincu",x,top+111,Yggdrasil.cleared(guild,boss)?Ui.GREEN:Ui.TEXT,false);
        button(g,x,top+123,w,18,"Défier le gardien",boss==guild.level-1&&!Yggdrasil.cleared(guild,boss),Ui.RED,()->send("boss"));
        if(guild.level<10){g.text(font,"10 000 or + "+Yggdrasil.xpRequired(guild.level)+" XP",x,top+145,Ui.TEXT,false);button(g,x,top+ph-66,w,18,"Passer au niveau "+(guild.level+1),officer()&&Yggdrasil.cleared(guild,guild.level-1)&&guild.gold>=10000&&guild.xp>=Yggdrasil.xpRequired(guild.level),Ui.GREEN,()->send("upgrade"));}
    }
    private void buffs(GuiGraphicsExtractor g){text(g,"1 000 or par buff · 1 heure · tous les membres",left+12,top+49,Ui.GOLD);
        for(var kind:GuildBuffs.Kind.values()){int y=top+67+kind.ordinal()*29;long remaining=Math.max(0,guild.buffs.getOrDefault(kind.name(),0L)-System.currentTimeMillis());boolean unlocked=guild.level>=kind.requiredLevel;
            String label=kind.label+" : "+kind.bonus(guild.level);g.text(font,font.plainSubstrByWidth(label,pw-160),left+12,y,unlocked?Ui.TEXT:Ui.MUTED,false);
            String info=unlocked?(remaining>0?"Actif pour toute la guilde":"Disponible pour toute la guilde"):"Débloqué au niveau "+kind.requiredLevel;g.text(font,font.plainSubstrByWidth(info,pw-160),left+12,y+12,Ui.MUTED,false);
            String buttonLabel=remaining>0?String.format("Actif %02d:%02d",remaining/60000,remaining/1000%60):"Activer · 1 000 or";
            button(g,left+pw-142,y+3,130,18,buttonLabel,unlocked&&remaining==0&&officer()&&guild.gold>=GuildBuffs.COST,Ui.GOLD,()->send("buff",t->t.putString("buff",kind.name())));
        }
    }
    private void history(GuiGraphicsExtractor g){int rows=Math.max(1,(ph-115)/17);for(int i=0;i<rows&&page*rows+i<guild.history.size();i++)text(g,guild.history.get(page*rows+i),left+12,top+55+i*17,Ui.TEXT);button(g,left+12,top+ph-67,40,16,"‹",page>0,Ui.GOLD,()->page--);button(g,left+58,top+ph-67,40,16,"›",(page+1)*rows<guild.history.size(),Ui.GOLD,()->page++);}
}
