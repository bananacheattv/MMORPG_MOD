package com.mmorpg.client.screen;
import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.*;
import com.mmorpg.block.LuckyTier;
import com.mmorpg.crafting.ForgeRecipes;
import com.mmorpg.network.Payloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** Tier-coloured relic chamber, staggered reels and a pageable, server-authored probability catalogue. */
public class LuckyBlockScreen extends MmoScreen {
 private final BlockPos pos;private final ListTag table;private final LuckyTier tier;private CompoundTag loot;private boolean requested;private int elapsed,page,idle;private String error="";private final ReelBoard board;private final ItemPreview3D[] previews={new ItemPreview3D(),new ItemPreview3D(),new ItemPreview3D()};
 public LuckyBlockScreen(CompoundTag data){super("Reliquaire de fortune");pos=BlockPos.of(data.getLongOr("pos",0));table=data.getListOrEmpty("table");tier=LuckyTier.of(data.getIntOr("tier",0));board=new ReelBoard(table);}
 @Override protected int designHeight(){return 400;}
 @Override public void tick(){idle++;if(loot!=null)elapsed++;}
 public void result(CompoundTag data){if(data.getLongOr("pos",Long.MIN_VALUE)!=pos.asLong()||!requested||loot!=null)return;error=data.getStringOr("error","");if(error.isEmpty()){loot=data.getCompoundOrEmpty("loot");elapsed=0;}}
 public void start(){if(requested)return;requested=true;ClientNet.send(new Payloads.LuckyRoll(pos));}
 public void previewResult(){if(System.getProperty("mmorpg.showcase","").isEmpty())throw new IllegalStateException("Private preview");requested=true;loot=new CompoundTag();loot.putString("item","mmorpg:potion_soin_majeure");loot.putInt("count",5);elapsed=0;}
 @Override public boolean mouseScrolled(double x,double y,double sx,double sy){board.scroll(-(int)Math.signum(sy));return true;}
 private ItemStack icon(String id){return new ItemStack(ForgeRecipes.resolveItem(id));}
 @Override protected void renderContent(GuiGraphicsExtractor g,int mx,int my,float partial){
  Ui.rounded(g,left,top,pw,ph,9,Ui.withAlpha(Ui.darker(tier.color,.16f),232));
  Ui.border(g,left,top,pw,ph,tier.color);
  int mid=left+pw/2;
  ReelBoard.rings(g,left+14,mid-150,top+17,tier.color);ReelBoard.rings(g,mid+150,left+pw-40,top+17,tier.color);
  ReelBoard.banner(g,font,mid,top+8,270,"LUCKY BLOCK "+tier.label.toUpperCase(java.util.Locale.ROOT),"Les runes d’Aeloria révèlent votre fortune",tier.color);
  int x=left+18,y=top+54,rw=pw-36;
  int rh=Math.max(90,ph-54-86-34);boolean spinning=loot!=null&&elapsed<80,finished=loot!=null&&!spinning;
  boolean showBoard=!requested&&loot==null;
  if(showBoard){
   var hover=board.render(g,font,x,y,rw,rh+34-34+0,mx,my,idle,(r,run)->click(r[0],r[1],r[2],r[3],run));
   String hint=hover!=null?hover.name()+"  "+hover.qty()+"  ·  "+hover.chance():"Survole un lot pour voir sa chance.";
   g.centeredText(font,Component.literal(font.plainSubstrByWidth(hint,rw)),mid,y+rh+4,hover!=null?tier.color:Ui.MUTED);
  }else{
  Ui.inset(g,x,y,rw,rh);
  int cw=(rw-16)/3;
  for(int col=0;col<3;col++){
   int sx=x+6+col*(cw+2);Ui.rounded(g,sx,y+7,cw,rh-14,7,Ui.withAlpha(Ui.darker(tier.color,.25f),190));Ui.border(g,sx,y+7,cw,rh-14,Ui.alpha(tier.color,.4f));
   boolean stopped=loot!=null&&elapsed>=48+col*16;
   float p=loot==null?0:Math.clamp((elapsed+partial)/(48f+col*16),0,1);
   float turns=(float)((1-Math.pow(1-p,3))*Math.PI*20);
   ItemStack shown;
   if(!requested||loot==null){shown=new ItemStack(com.mmorpg.registry.ModItems.MYSTERY_CUBE.get());shown.set(net.minecraft.core.component.DataComponents.ITEM_MODEL,com.mmorpg.MMORPG.id("ui_mystery_"+tier.id.substring(6)));}
   else if(stopped)shown=icon(loot.getStringOr("item","mmorpg:piece_or"));
   else{int frame=(int)(turns/(Math.PI/2))+col*7;shown=table.isEmpty()?icon("mmorpg:piece_or"):icon(table.getCompoundOrEmpty(Math.floorMod(frame,table.size())).getStringOr("item","mmorpg:piece_or"));}
   float rx=!requested?(float)Math.sin((idle+partial)*.018+col)*.12f:turns;
   float ry=!requested?(idle+partial)*.012f+col*.35f:stopped?.3f:.25f;
   previews[col].draw(g,shown,sx+2,y+9,cw-4,rh-18,rx,ry);
   Ui.rounded(g,sx+cw/2-9,y+rh-12,18,2,1,tier.color);
  }
  }
  String label=!error.isEmpty()?"Fermer":!requested?"Ouvrir le Lucky Block":spinning?"Révéler maintenant":finished?"Fermer":"Invocation…";
  int bw=!requested&&loot==null?rw-126:rw;
  button(g,x,y+rh+22,bw,24,label,!requested||loot!=null||!error.isEmpty(),tier.color,()->{if(!error.isEmpty())onClose();else if(!requested)start();else if(spinning)elapsed=80;else onClose();});
  if(!requested&&loot==null)button(g,x+rw-118,y+rh+22,118,24,board.showAll()?"COLONNES":"TOUS LES LOTS",true,tier.color,board::toggleAll);
  int resultY=top+ph-54;Ui.inset(g,x,resultY,rw,40);
  String title=!error.isEmpty()?error:finished?"✦ TRÉSOR OBTENU ✦":requested?"Les runes révèlent votre trésor…":"Un reliquaire consommé · Une récompense garantie";
  g.centeredText(font,Component.literal(font.plainSubstrByWidth(title,pw-40)),left+pw/2,resultY+6,error.isEmpty()?tier.color:Ui.RED);
  String detail=finished?(loot.getLongOr("gold",0)>0?loot.getLongOr("gold",0)+" pièces d’or":loot.getIntOr("count",1)+" × "+icon(loot.getStringOr("item","mmorpg:piece_or")).getHoverName().getString()):"Chaque Lucky Block renferme un trésor d’Aeloria.";
  g.centeredText(font,Component.literal(font.plainSubstrByWidth(detail,pw-40)),left+pw/2,resultY+23,finished?Ui.GREEN:Ui.MUTED);
 }
}
