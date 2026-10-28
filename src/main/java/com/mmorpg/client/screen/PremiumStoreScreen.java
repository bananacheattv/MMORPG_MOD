package com.mmorpg.client.screen;
import com.mmorpg.client.ui.*;
import com.mmorpg.client.ClientNet;
import com.mmorpg.network.Payloads;
import net.minecraft.nbt.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import java.util.*;

public final class PremiumStoreScreen extends MmoScreen {
    @Override protected String backdrop() { return "shop_header"; }

 private final CompoundTag data;private final List<CompoundTag> products=new ArrayList<>();private String category="Clés";private int page;private String confirm="";private UUID request=UUID.randomUUID();private boolean busy;
 public PremiumStoreScreen(CompoundTag data){super("Boutique d’Aeloria");this.data=data;var list=data.getListOrEmpty("products");for(int i=0;i<list.size();i++)products.add(list.getCompoundOrEmpty(i));}
 public void previewCategory(String value){category=value;page=0;confirm="";}
 @Override protected int designHeight(){return 350;}
 private void send(String action,String product){var t=new CompoundTag();t.putString("action",action);t.putString("product",product);t.putString("request",request.toString());products.stream().filter(p->p.getStringOr("id","").equals(product)).findFirst().ifPresent(p->t.putLong("price",p.getLongOr("price",0)));ClientNet.send(new Payloads.SocialAction("store",t));busy=true;}
 @Override protected void renderContent(GuiGraphicsExtractor g,int mx,int my,float a){
  Ui.frame(g,left,top,pw,ph,Ui.GOLD,false);Ui.title(g,left+pw/2,top+9,"✦ BOUTIQUE D’AELORIA ✦",Ui.GOLD_LIGHT);
  g.centeredText(font,Component.literal(data.getLongOr("credits",0)+" CRÉDITS · "+data.getIntOr("pending",0)+" livraison(s) en attente"),left+pw/2,top+25,0xFFC8A2FF);
  String[] categories={"Clés","Charmes","Runes","Potions","Lucky Blocks","Grades"};int tw=(pw-24)/6;
  for(int i=0;i<categories.length;i++){String c=categories[i];button(g,left+12+i*tw,top+42,tw-3,18,c,true,c.equals(category)?Ui.GOLD:Ui.MUTED,()->{category=c;page=0;confirm="";});}
  var found=products.stream().filter(p->p.getStringOr("category","").equals(category)).toList();int rows=Math.max(1,(ph-142)/43);page=Math.clamp(page,0,Math.max(0,(found.size()-1)/rows));
  for(int i=0;i<rows&&page*rows+i<found.size();i++){var p=found.get(page*rows+i);String id=p.getStringOr("id","");int y=top+68+i*43;Ui.inset(g,left+12,y,pw-24,39);
   String item=p.getStringOr("item","");if(!item.isEmpty())g.item(new net.minecraft.world.item.ItemStack(com.mmorpg.crafting.ForgeRecipes.resolveItem(item)),left+20,y+11);
   String title=p.getStringOr("label",id)+(p.getIntOr("count",1)>1?" ×"+p.getIntOr("count",1):"");g.text(font,font.plainSubstrByWidth(title,pw-160),left+44,y+6,Ui.TEXT,false);
   g.text(font,font.plainSubstrByWidth(p.getStringOr("benefits","Livraison sécurisée dans votre inventaire"),pw-160),left+44,y+22,Ui.MUTED,false);
   long price=p.getLongOr("price",0);boolean owned=p.getBooleanOr("owned",false);String label=owned?"Acquis":id.equals(confirm)?"Confirmer":price+" crédits";
   button(g,left+pw-112,y+9,92,22,label,!busy&&!owned&&data.getLongOr("credits",0)>=price,Ui.GOLD,()->{if(id.equals(confirm))send("buy",id);else{confirm=id;request=UUID.randomUUID();}});
  }
  int y=top+ph-66;button(g,left+12,y,30,17,"‹",page>0,Ui.GOLD,()->page--);button(g,left+48,y,30,17,"›",(page+1)*rows<found.size(),Ui.GOLD,()->page++);
  button(g,left+86,y,126,17,"Recevoir mes achats",!busy,Ui.GREEN,()->send("claim",""));button(g,left+pw-99,y,87,17,"Fermer",true,Ui.MUTED,this::onClose);
  String status=data.getStringOr("status","");if(status.isBlank())status=data.getBooleanOr("payments",false)?"Les crédits s’achètent sur le site officiel.":"Site en préparation · achats en argent réel désactivés";
  g.centeredText(font,Component.literal(font.plainSubstrByWidth(status,pw-24)),left+pw/2,top+ph-39,Ui.GOLD);
  g.centeredText(font,Component.literal("Les crédits sont distincts de l’or. Cliquez deux fois pour confirmer un achat."),left+pw/2,top+ph-24,Ui.MUTED);
 }
}
