package com.mmorpg.client.screen;
import com.mmorpg.client.*;
import com.mmorpg.client.ui.*;
import com.mmorpg.network.Payloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;
public final class PvpStoreScreen extends MmoScreen {
 private final CompoundTag data;private final java.util.List<CompoundTag> products=new java.util.ArrayList<>();private int category,scroll,selected;private final ItemPreview3D preview=new ItemPreview3D();
 public PvpStoreScreen(CompoundTag t){super("Intendant des arènes");data=t;var list=t.getListOrEmpty("products");for(int i=0;i<list.size();i++)products.add(list.getCompoundOrEmpty(i));}
 public void selectCategory(int value){category=Math.clamp(value,0,2);selected=scroll=0;}
 @Override protected int panelWidth(){return Math.min(width-12,800);}@Override protected int panelHeight(){return Math.min(height-12,390);}
 private java.util.List<CompoundTag> visible(){return products.stream().filter(p->category==0?p.getStringOr("unlock","").isEmpty()&&!p.getStringOr("id","").startsWith("pvp_"):category==1?p.getStringOr("id","").startsWith("pvp_"):!p.getStringOr("unlock","").isEmpty()).toList();}
 @Override public boolean mouseScrolled(double x,double y,double dx,double dy){scroll=Math.max(0,scroll-(int)Math.signum(dy));return true;}
 private void action(String action,CompoundTag product){var t=new CompoundTag();t.putString("action",action);t.putString("product",product.getStringOr("id",""));t.putLong("price",product.getLongOr("price",0));t.putString("request",java.util.UUID.randomUUID().toString());if(action.equals("title"))t.putString("title",product.getStringOr("unlock","").substring(6));ClientNet.send(new Payloads.SocialAction("pvp",t));}
 @Override protected void renderContent(GuiGraphicsExtractor g,int mx,int my,float a){Ui.panel(g,left,top,pw,ph);Ui.title(g,left+pw/2,top+9,"INTENDANT DES ARÈNES",Ui.GOLD);g.centeredText(font,net.minecraft.network.chat.Component.literal(data.getLongOr("points",0)+" POINTS D’ARÈNE"),left+pw/2,top+25,Ui.GOLD_LIGHT);
  for(int i=0;i<3;i++){int cat=i;button(g,left+10+i*(pw-20)/3,top+43,(pw-20)/3-4,20,new String[]{"Potions","Équipement PvP","Titres & monture"}[i],true,category==i?Ui.GOLD:Ui.MUTED,()->{category=cat;scroll=selected=0;});}
  var shown=visible();int lw=(pw-34)*3/5,x=left+10,y=top+74,rowH=35,rows=Math.max(1,(ph-111)/rowH);scroll=Math.clamp(scroll,0,Math.max(0,shown.size()-rows));selected=Math.clamp(selected,0,Math.max(0,shown.size()-1));
  for(int i=0;i<rows&&scroll+i<shown.size();i++){int index=scroll+i;var p=shown.get(index);int ry=y+i*rowH;Ui.frame(g,x,ry,lw,rowH-3,index==selected?Ui.GOLD:Ui.LINE,index==selected);String item=p.getStringOr("item","");if(!item.isEmpty())g.item(new ItemStack(com.mmorpg.crafting.ForgeRecipes.resolveItem("mmorpg:"+item)),x+5,ry+6);g.text(font,font.plainSubstrByWidth(p.getStringOr("label",""),lw-42),x+28,ry+5,Ui.TEXT,false);g.text(font,p.getLongOr("price",0)+" points · Niv. "+p.getIntOr("level",1)+(p.getBooleanOr("owned",false)?" · Débloqué":""),x+28,ry+18,Ui.GOLD,false);click(x,ry,lw,rowH-3,()->selected=index);}
  int rx=x+lw+12,rw=left+pw-rx-10;Ui.inset(g,rx,y,rw,ph-109);
  if(!shown.isEmpty()){var p=shown.get(selected);String item=p.getStringOr("item","");if(!item.isEmpty())preview.draw(g,new ItemStack(com.mmorpg.crafting.ForgeRecipes.resolveItem("mmorpg:"+item)),rx+5,y+3,rw-10,Math.max(40,(ph-150)/2),-.15f,.5f);int textY=y+Math.max(45,(ph-150)/2)+8;Ui.wrap(g,category==1?"Spécialisation PvP : dégâts et résistance contre les joueurs. Moins efficace contre les monstres. Quatre pièces renforcent la résilience.":p.getStringOr("unlock","").startsWith("mount:")?"Monture 3D exclusive obtenue uniquement avec les points gagnés en arène.":p.getStringOr("label",""),rx+8,textY,rw-16,Ui.TEXT);boolean owned=p.getBooleanOr("owned",false);boolean title=p.getStringOr("unlock","").startsWith("title:");button(g,rx+8,top+ph-65,rw-16,23,owned&&title?"Équiper le titre":owned?"Déjà débloqué":"Acheter · "+p.getLongOr("price",0),owned?title:data.getLongOr("points",0)>=p.getLongOr("price",0)&&ClientData.DATA.level>=p.getIntOr("level",1),Ui.GOLD,()->action(owned&&title?"title":"buy",p));}
  g.text(font,font.plainSubstrByWidth(data.getStringOr("status",""),pw-24),left+12,top+ph-27,Ui.MUTED,false);
 }
}
