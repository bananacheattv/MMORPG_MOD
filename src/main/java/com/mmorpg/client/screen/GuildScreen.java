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
    @Override protected MenuScreen.Tab navActive() { return MenuScreen.Tab.GUILDE; }

    @Override protected String backdrop() { return "guild_hall_header"; }

    private final CompoundTag data;private final GuildStore.Guild guild;private final UUID self;private int tab,page,boss;private UUID selected;
    private EditBox name,amount,motto,confirmation;
    public void showTab(int value){tab=Math.clamp(value,0,9);page=0;}
    private int view;private EditBox notice;
    public GuildScreen(CompoundTag data){super("Guilde");this.data=data;String json=data.getStringOr("guild","");guild=json.isEmpty()?null:new com.google.gson.Gson().fromJson(json,GuildStore.Guild.class);self=UUID.fromString(data.getStringOr("self",new UUID(0,0).toString()));boss=guild==null?0:guild.level-1;showTab(data.getIntOr("tab",0));}
    @Override protected int designHeight(){return 340;}
    private EditBox field(int x,int y,int w,int max,String value){var f=new EditBox(font,x,y,w,18,Component.literal("Valeur"));f.setMaxLength(max);f.setValue(value);addRenderableWidget(f);return f;}
    @Override protected void init(){super.init();name=field(left+12,top+66,pw-24,28,"");amount=field(left+12,top+95,Math.min(180,pw-24),10,"1000");motto=field(left+12,top+84,pw-24,120,guild==null?"":guild.motto);confirmation=field(left+12,top+ph-71,pw/2-20,28,"");notice=field(left+12,top+128,pw-24,200,"");}
    private void send(String action,java.util.function.Consumer<CompoundTag> fill){var t=new CompoundTag();t.putString("action",action);fill.accept(t);ClientNet.send(new Payloads.SocialAction("guild",t));}
    private void send(String action){send(action,t->{});}
    private boolean chief(){return guild!=null&&guild.members.get(self).rank.equals("CHEF");}
    private boolean officer(){return guild!=null&&!guild.members.get(self).rank.equals("MEMBRE");}
    private void text(GuiGraphicsExtractor g,String value,int x,int y,int color){g.text(font,font.plainSubstrByWidth(value,pw-24),x,y,color,false);}
    @Override protected void renderContent(GuiGraphicsExtractor g,int mx,int my,float a){
        Ui.frame(g,left,top,pw,ph,Ui.GOLD,false);g.centeredText(font,Component.literal(guild==null?"GUILDES":guild.name+" · Niveau "+guild.level+" / 10"),left+pw/2,top+8,Ui.GOLD);
        for(var f:List.of(name,amount,motto,confirmation,notice)){f.visible=false;f.active=false;}
        if(guild==null)noGuild(g);else{
            String[] tabs={"Guilde","Membres","Banque","Boutique","Quêtes","Arbre","Trophées","Buffs","Journal","Identité"};int w=(pw-24)/10;for(int i=0;i<tabs.length;i++){int n=i;button(g,left+12+i*w,top+27,w-2,17,tabs[i],true,tab==i?Ui.GOLD:Ui.MUTED,()->{tab=n;page=0;});}
            switch(tab){case 0->home(g);case 1->members(g);case 2->bank(g);case 3->shop(g);case 4->quests(g);case 5->tree(g);case 6->trophies(g);case 7->buffs(g);case 9->identity(g);default->history(g);}
        }
        text(g,data.getStringOr("status",""),left+12,top+ph-38,Ui.GOLD);
        button(g,left+12,top+ph-23,90,17,"Actualiser",true,Ui.GOLD,()->send("open"));
        if(guild!=null&&tab==5)button(g,left+pw/2-65,top+ph-23,130,17,"Quitter l’épreuve",true,Ui.MUTED,()->send("raidLeave"));
        button(g,left+pw-102,top+ph-23,90,17,"Fermer",true,Ui.MUTED,this::onClose);
    }
    private void visible(EditBox f){f.visible=true;f.active=true;}
    private void noGuild(GuiGraphicsExtractor g){text(g,"Crée une guilde ou accepte une invitation.",left+12,top+43,Ui.TEXT);visible(name);button(g,left+12,top+90,pw-24,18,"Fonder ma guilde",!name.getValue().isBlank(),Ui.GREEN,()->send("create",t->t.putString("name",name.getValue())));
        var invites=data.getListOrEmpty("invites");int rows=Math.max(1,(ph-162)/23);for(int i=0;i<rows&&i+page*rows<invites.size();i++){var invite=invites.getCompoundOrEmpty(i+page*rows);int y=top+118+i*23;String id=invite.getStringOr("id","");text(g,invite.getStringOr("name",""),left+12,y+4,Ui.TEXT);button(g,left+pw-148,y,65,18,"Accepter",true,Ui.GREEN,()->send("accept",t->t.putString("target",id)));button(g,left+pw-78,y,65,18,"Refuser",true,Ui.RED,()->send("decline",t->t.putString("target",id)));}}
    private void home(GuiGraphicsExtractor g){text(g,guild.members.size()+" / 50 membres · Banque : "+guild.gold+" or",left+12,top+53,Ui.TEXT);text(g,"XP de guilde : "+guild.xp+" / "+Yggdrasil.xpRequired(guild.level),left+12,top+70,Ui.TEXT);
        visible(motto);button(g,left+12,top+106,pw-24,17,"Enregistrer la devise",officer(),Ui.GOLD,()->send("motto",t->t.putString("text",motto.getValue())));
        visible(notice);button(g,left+12,top+150,pw/2-16,17,"Message du jour",officer(),Ui.GOLD,()->send("motd",t->t.putString("text",notice.getValue())));
        button(g,left+pw/2+4,top+150,pw/2-16,17,"Épingler l’annonce",officer()&&guild.pins.size()<3&&!notice.getValue().isBlank(),Ui.GOLD,()->send("pin",t->t.putString("text",notice.getValue())));
        text(g,"Message du jour : "+(guild.motd.isEmpty()?"—":guild.motd),left+12,top+173,Ui.GOLD);
        for(int i=0;i<guild.pins.size();i++){final int index=i;int y=top+185+i*13;g.text(font,font.plainSubstrByWidth("• "+guild.pins.get(i),pw-60),left+12,y+2,Ui.TEXT,false);button(g,left+pw-40,y,28,12,"×",officer(),Ui.RED,()->send("unpin",t->t.putInt("index",index)));}
        if(chief()){visible(confirmation);button(g,left+pw/2,top+ph-71,pw/2-12,18,"Dissoudre (nom exact)",confirmation.getValue().equals(guild.name),Ui.RED,()->send("disband",t->t.putString("confirm",confirmation.getValue())));}else button(g,left+12,top+ph-71,pw-24,18,"Quitter la guilde",true,Ui.RED,()->send("leave"));}
    private void members(GuiGraphicsExtractor g){int half=pw/2,rows=Math.max(1,(ph-122)/21);var members=new ArrayList<>(guild.members.values());members.sort(Comparator.comparing((GuildStore.Member m)->m.rank.equals("CHEF")?0:m.rank.equals("OFFICIER")?1:2).thenComparing(m->m.name));
        for(int i=0;i<rows&&page*rows+i<members.size();i++){var m=members.get(page*rows+i);button(g,left+12,top+53+i*21,half-20,18,font.plainSubstrByWidth(m.name+" · "+m.rank,half-30),true,m.id.equals(selected)?Ui.GOLD:Ui.TEXT,()->selected=m.id);}
        var online=data.getListOrEmpty("online");int y=top+53;text(g,"Inviter un joueur",left+half,top+47,Ui.GOLD);for(int i=0;i<online.size()&&y<top+ph-88;i++){var entry=online.getCompoundOrEmpty(i);String id=entry.getStringOr("id","");if(guild.members.containsKey(UUID.fromString(id)))continue;button(g,left+half,y+12,pw-half-12,18,entry.getStringOr("name",""),officer(),Ui.GREEN,()->send("invite",t->t.putString("target",id)));y+=21;}
        button(g,left+12,top+ph-67,30,16,"‹",page>0,Ui.GOLD,()->page--);button(g,left+48,top+ph-67,30,16,"›",(page+1)*rows<members.size(),Ui.GOLD,()->page++);
        String[] actions={"promote","demote","kick","leader"},labels={"Officier","Membre","Exclure","Nommer chef"};int w=(pw-108)/4;for(int i=0;i<4;i++){String action=actions[i];button(g,left+92+i*w,top+ph-67,w-3,16,labels[i],selected!=null&&!selected.equals(self)&&(action.equals("kick")?officer():chief()),Ui.GOLD,()->send(action,t->t.putString("target",selected.toString())));}}
    private void bank(GuiGraphicsExtractor g){text(g,"BANQUE DE GUILDE : "+guild.gold+" OR",left+12,top+55,Ui.GOLD);text(g,"Ton or : "+data.getLongOr("wallet",0),left+12,top+74,Ui.TEXT);visible(amount);long n;try{n=Long.parseLong(amount.getValue());}catch(Exception e){n=0;}final long number=n;button(g,left+12,top+121,(pw-28)/2,20,"Déposer",n>0,Ui.GREEN,()->send("deposit",t->t.putLong("amount",number)));button(g,left+pw/2+2,top+121,(pw-28)/2,20,"Retirer (chef)",n>0&&chief(),Ui.GOLD,()->send("withdraw",t->t.putLong("amount",number)));text(g,"Les mouvements sont conservés dans le journal.",left+12,top+154,Ui.MUTED);
        text(g,"COFFRE D’OBJETS (dans le hall) : "+data.getIntOr("vaultCount",0)+" / 27",left+12,top+176,Ui.GOLD);
        button(g,left+12,top+192,(pw-28)/2,20,"Ouvrir le coffre (officier)",officer(),Ui.GOLD,()->send("vault"));button(g,left+pw/2+2,top+192,(pw-28)/2,20,"Déposer l’objet en main",true,Ui.GREEN,()->send("vaultDeposit"));}
    private void tree(GuiGraphicsExtractor g){int treeW=pw*43/100,cx=left+treeW/2,base=top+ph-55;for(int n=0;n<6;n++){int y=top+55+n*6,spread=32+Math.abs(3-n)*5;g.fill(cx-spread,y,cx+spread,y+9,n%2==0?0xFF335B46:0xFF487650);}g.fill(cx-5,top+60,cx+5,base,0xFF74543A);g.fill(cx-2,top+64,cx+1,base,0xFFB18B51);for(int n=0;n<5;n++){g.fill(cx-9-n*7,base+n,cx+9+n*7,base+n+3,0xFF74543A);}
        for(int i=0;i<10;i++){final int index=i;int row=i/2,y=base-19-row*24,x=cx+(i%2==0?-43:25);g.fill(Math.min(cx,x),y+8,Math.max(cx,x)+10,y+11,0xFF6D8F53);button(g,x,y,23,19,Integer.toString(i+1),true,Yggdrasil.cleared(guild,i)?Ui.GREEN:i==boss?Ui.GOLD:Ui.MUTED,()->boss=index);}
        int x=left+treeW+8,w=pw-treeW-20;var def=Yggdrasil.ALL[boss];g.text(font,font.plainSubstrByWidth(def.name(),w),x,top+54,0xFF000000|def.color(),false);g.text(font,font.plainSubstrByWidth(def.form(),w),x,top+73,Ui.TEXT,false);
        g.text(font,"Gardien "+(boss+1)+" / 10",x,top+94,Ui.GOLD,false);g.text(font,Yggdrasil.cleared(guild,boss)?"Vaincu":"Non vaincu",x,top+111,Yggdrasil.cleared(guild,boss)?Ui.GREEN:Ui.TEXT,false);
        long remaining=Math.max(0,guild.bossCooldowns.getOrDefault(boss,0L)-System.currentTimeMillis());
        String challenge=remaining>0?String.format("Recharge %dh %02dmin",remaining/3600000,(remaining/60000)%60):"Défier le gardien";
        button(g,x,top+123,w,18,challenge,boss<guild.level&&remaining==0,Ui.RED,()->send("boss",t->t.putInt("tier",boss)));
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
    private void identity(GuiGraphicsExtractor g){
        text(g,"BLASON · couleur principale, couleur d’emblème et emblème · Hall "+guild.hallLevel+" / 3",left+12,top+53,Ui.GOLD);
        int w=(pw-130)/12;
        for(int i=0;i<12;i++){String color=GuildCrest.COLORS.get(i);int x=left+12+i*w;
            button(g,x,top+68,w-3,15,"",officer(),guild.banner.equals(color)?Ui.GOLD:Ui.MUTED,()->send("banner",t->t.putString("banner",color)));g.fill(x+3,top+71,x+w-6,top+80,GuildCrest.argb(color));
            button(g,x,top+90,w-3,15,"",officer(),guild.banner2.equals(color)?Ui.GOLD:Ui.MUTED,()->send("banner2",t->t.putString("banner",color)));g.fill(x+3,top+93,x+w-6,top+102,GuildCrest.argb(color));}
        int pw2=(pw-24)/10;
        for(int i=0;i<10;i++){String pattern=GuildCrest.PATTERNS.get(i);String label=pattern.substring(0,1)+pattern.substring(1,Math.min(7,pattern.length())).toLowerCase();
            button(g,left+12+i*pw2,top+112,pw2-2,16,label,officer(),guild.bannerPattern.equals(pattern)?Ui.GOLD:Ui.MUTED,()->send("bannerPattern",t->t.putString("pattern",pattern)));}
        int px=left+pw-98,py=top+64;g.fill(px-2,py-2,px+7*10+2,py+5*10+2,0xFF000000|0);g.fill(px,py,px+70,py+50,GuildCrest.argb(guild.banner));
        for(int cx=-3;cx<=3;cx++)for(int cy=0;cy<=4;cy++)if(GuildCrest.shape(guild.bannerPattern,cx,cy))g.fill(px+(cx+3)*10,py+(4-cy)*10,px+(cx+3)*10+10,py+(4-cy)*10+10,GuildCrest.argb(guild.banner2));
        button(g,left+12,top+138,pw-24,20,"Entrer dans le hall de guilde",true,Ui.GREEN,()->send("hall"));
        long cost=5000L*(guild.hallLevel+1);button(g,left+12,top+163,pw-24,20,"Améliorer le hall · "+cost+" or",officer()&&guild.hallLevel<3&&guild.gold>=cost,Ui.GOLD,()->send("hallUpgrade"));
        text(g,"Trophées : "+guild.trophies.size()+" · coffre : "+data.getIntOr("vaultCount",0)+" / 27 emplacements",left+12,top+192,Ui.TEXT);
        if(ph>260)text(g,"Hall : bibliothèque, atelier, galerie royale ; −5 % boutique par niveau.",left+12,top+210,Ui.MUTED);
    }
    private void shop(GuiGraphicsExtractor g){text(g,"BOUTIQUE DE GUILDE · ton or : "+data.getLongOr("wallet",0)+" · remise hall −"+guild.hallLevel*5+" %",left+12,top+53,Ui.GOLD);
        int rows=Math.min(GuildExtras.SHOP.size(),Math.max(1,(ph-110)/22));
        for(int i=0;i<rows;i++){final int index=i;var it=GuildExtras.SHOP.get(i);long price=GuildExtras.price(it,guild.hallLevel);boolean ok=guild.level>=it.minLevel();int y=top+70+i*22;
            String label=it.count()+"× "+it.id().replace('_',' ')+(ok?" · "+price+" or":" · niveau de guilde "+it.minLevel());
            button(g,left+12,y,pw-24,19,font.plainSubstrByWidth(label,pw-40),ok&&data.getLongOr("wallet",0)>=price,ok?Ui.TEXT:Ui.MUTED,()->send("shop",t->t.putInt("item",index)));}
        text(g,"Le prix est versé à la banque de guilde.",left+12,top+ph-55,Ui.MUTED);}
    private void quests(GuiGraphicsExtractor g){text(g,"QUÊTES DE GUILDE · "+guild.questsDone+" accomplies · progression commune",left+12,top+53,Ui.GOLD);
        int y=top+72;for(var q:guild.quests){String head=(q.weekly?"[Hebdo] ":"[Jour] ")+GuildExtras.label(q);g.text(font,font.plainSubstrByWidth(head,pw-24),left+12,y,q.done?Ui.GREEN:Ui.TEXT,false);
            int bw=pw-24;g.fill(left+12,y+12,left+12+bw,y+20,0xFF18121F);g.fill(left+12,y+12,left+12+(int)(bw*Math.min(1.0,q.progress/(double)Math.max(1,q.target))),y+20,q.done?0xFF3FB868:0xFF8A55D8);
            g.text(font,q.progress+" / "+q.target+" · +"+q.rewardXp+" XP · +"+q.rewardGold+" or",left+12,y+23,Ui.MUTED,false);y+=42;}
        if(guild.quests.isEmpty())text(g,"Reviens après la prochaine actualisation.",left+12,y,Ui.MUTED);}
    private void trophies(GuiGraphicsExtractor g){var all=GuildExtras.catalog();text(g,"SALLE DES TROPHÉES · "+guild.trophies.size()+" / "+all.size(),left+12,top+53,Ui.GOLD);
        int cols=2,rows=10,w=(pw-24)/cols;for(int i=0;i<all.size();i++){var t=all.get(i);boolean got=guild.trophies.contains(t.id());int x=left+12+(i/rows)*w,y=top+68+(i%rows)*17;
            g.fill(x,y+3,x+9,y+12,got?0xFFE6B83A:0xFF3A3346);g.text(font,font.plainSubstrByWidth(got?t.label():"??? · "+t.hint(),w-18),x+14,y+4,got?Ui.TEXT:Ui.MUTED,false);}
}
    private void history(GuiGraphicsExtractor g){
        if(view==1){activity(g);return;}int rows=Math.max(1,(ph-115)/17);for(int i=0;i<rows&&page*rows+i<guild.history.size();i++)text(g,guild.history.get(page*rows+i),left+12,top+55+i*17,Ui.TEXT);button(g,left+pw-130,top+ph-67,118,16,"Activité membres",true,Ui.GOLD,()->{view=1;page=0;});button(g,left+12,top+ph-67,40,16,"‹",page>0,Ui.GOLD,()->page--);button(g,left+58,top+ph-67,40,16,"›",(page+1)*rows<guild.history.size(),Ui.GOLD,()->page++);}
    private void activity(GuiGraphicsExtractor g){text(g,"ACTIVITÉ DES MEMBRES · dernière connexion · dons · monstres · quêtes",left+12,top+53,Ui.GOLD);
        var list=new ArrayList<>(guild.members.values());list.sort(Comparator.comparingLong((GuildStore.Member m)->-guild.activity.getOrDefault(m.id,new GuildStore.Activity()).lastSeen));int rows=Math.max(1,(ph-125)/16);
        var fmt=java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm").withZone(java.time.ZoneId.systemDefault());
        for(int i=0;i<rows&&page*rows+i<list.size();i++){var m=list.get(page*rows+i);var a=guild.activity.getOrDefault(m.id,new GuildStore.Activity());
            String seen=a.lastSeen==0?"—":fmt.format(java.time.Instant.ofEpochMilli(a.lastSeen));
            text(g,m.name+" · "+seen+" · "+a.gold+" or · "+a.kills+" kills · "+a.quests+" quêtes",left+12,top+70+i*16,Ui.TEXT);}
        button(g,left+12,top+ph-67,40,16,"‹",page>0,Ui.GOLD,()->page--);button(g,left+58,top+ph-67,40,16,"›",(page+1)*rows<list.size(),Ui.GOLD,()->page++);
        button(g,left+pw-130,top+ph-67,118,16,"Journal",true,Ui.GOLD,()->{view=0;page=0;});}
}
