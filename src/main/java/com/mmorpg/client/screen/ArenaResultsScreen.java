package com.mmorpg.client.screen;
import com.mmorpg.client.*;
import com.mmorpg.client.ui.*;
import com.mmorpg.network.Payloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.*;
public final class ArenaResultsScreen extends MmoScreen {
 private final CompoundTag data;
 public ArenaResultsScreen(CompoundTag t){super("Résultats de l’arène");data=t;}
 @Override protected int panelWidth(){return Math.min(width-12,850);}
 @Override protected int panelHeight(){return Math.min(height-12,390);}
 @Override protected void renderContent(GuiGraphicsExtractor g,int mx,int my,float a){Ui.panel(g,left,top,pw,ph);int winner=data.getIntOr("winner",-1);Ui.title(g,left+pw/2,top+10,data.getBooleanOr("unfinished",false)?"SORTIE DE L’ARÈNE":data.getBooleanOr("training",false)?"BILAN D’ENTRAÎNEMENT":winner<0?"ÉGALITÉ":winner==0?"VICTOIRE ROUGE":"VICTOIRE BLEUE",winner==0?Ui.RED:winner==1?Ui.BLUE:Ui.GOLD);
  g.centeredText(font,net.minecraft.network.chat.Component.literal(data.getIntOr("red",0)+" — "+data.getIntOr("blue",0)),left+pw/2,top+28,Ui.TEXT);
  var rows=data.getListOrEmpty("rows");long earned=0;int ownTeam=-1;
  for(int team=0;team<2;team++){int x=left+10+team*(pw/2),w=pw/2-20;Ui.rounded(g,x,top+49,w,ph-111,7,Ui.withAlpha(team==0?0x472031:0x20324E,180));Ui.header(g,x+8,top+57,w-16,team==0?"ÉQUIPE ROUGE":"ÉQUIPE BLEUE");g.text(font,"Joueur",x+8,top+75,Ui.MUTED,false);g.text(font,"K / D",x+w/2,top+75,Ui.MUTED,false);g.text(font,"Cristal",x+w*3/4-13,top+75,Ui.MUTED,false);g.text(font,"Points",x+w-8-font.width("Points"),top+75,Ui.MUTED,false);int y=top+93;
   for(int i=0;i<rows.size();i++){var row=rows.getCompoundOrEmpty(i);if(row.getIntOr("team",0)!=team)continue;boolean own=row.getStringOr("id","").equals(data.getStringOr("player",""));if(own){earned=row.getLongOr("points",0);ownTeam=team;Ui.rounded(g,x+4,y-3,w-8,32,4,0x554E3A64);}g.text(font,font.plainSubstrByWidth(row.getStringOr("name",""),w/2-12),x+8,y,own?Ui.GOLD:Ui.TEXT,false);g.text(font,row.getIntOr("kills",0)+" / "+row.getIntOr("deaths",0),x+w/2,y,Ui.TEXT,false);g.text(font,""+row.getIntOr("strikes",0),x+w*3/4-13,y,Ui.TEXT,false);String points="+"+row.getLongOr("points",0);g.text(font,points,x+w-8-font.width(points),y,Ui.GOLD,false);g.text(font,"Dégâts : "+Ui.fmt(row.getDoubleOr("damage",0))+" · Soins : "+Ui.fmt(row.getDoubleOr("healing",0)),x+8,y+12,Ui.MUTED,false);y+=Math.clamp((ph-145)/5,24,37);}
  }
  g.centeredText(font,net.minecraft.network.chat.Component.literal(data.getBooleanOr("training",false)?"Entraînement · Aucun point distribué":data.getBooleanOr("unfinished",false)?"Match quitté avant sa fin · Aucun point distribué":"+"+earned+" points d’arène"+(ownTeam==winner?" · Bonus victoire ×2":" · Contribution récompensée")),left+pw/2,top+ph-48,Ui.GOLD);
  button(g,left+10,top+ph-28,pw/2-15,20,"Marchand PvP",true,Ui.GOLD,()->{ack();var t=new CompoundTag();t.putString("action","open");ClientNet.send(new Payloads.SocialAction("pvp",t));});button(g,left+pw/2+5,top+ph-28,pw/2-15,20,"Fermer",true,Ui.MUTED,this::onClose);
 }
 private void ack(){var t=new CompoundTag();t.putString("action","report_ack");t.putString("match",data.getStringOr("match",""));ClientNet.send(new Payloads.SocialAction("pvp",t));}
 @Override public void onClose(){ack();super.onClose();}
}
