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
 private final BlockPos pos;private final ListTag table;private final LuckyTier tier;private CompoundTag loot;private boolean requested;private int elapsed,page,idle;private String error="";
 public LuckyBlockScreen(CompoundTag data){super("Reliquaire de fortune");pos=BlockPos.of(data.getLongOr("pos",0));table=data.getListOrEmpty("table");tier=LuckyTier.of(data.getIntOr("tier",0));}
 @Override protected int panelWidth(){return Math.min(width-12,650);}
 @Override protected int panelHeight(){return Math.min(height-12,340);}
 @Override public void tick(){idle++;if(loot!=null)elapsed++;}
 public void result(CompoundTag data){if(data.getLongOr("pos",Long.MIN_VALUE)!=pos.asLong()||!requested||loot!=null)return;error=data.getStringOr("error","");if(error.isEmpty()){loot=data.getCompoundOrEmpty("loot");elapsed=0;}}
 public void start(){if(requested)return;requested=true;ClientNet.send(new Payloads.LuckyRoll(pos));}
 @Override public boolean mouseScrolled(double x,double y,double sx,double sy){page=Math.max(0,page-(int)Math.signum(sy));return true;}
 private ItemStack icon(String id){return new ItemStack(ForgeRecipes.resolveItem(id));}
 @Override protected void renderContent(GuiGraphicsExtractor g,int mx,int my,float partial){
  Ui.frame(g,left,top,pw,ph,tier.color,false);g.fill(left+2,top+2,left+pw-2,top+34,0xFF21152F);
  Ui.title(g,left+pw/2,top+8,"✦ RELIQUAIRE "+tier.label.toUpperCase(java.util.Locale.ROOT)+" ✦",tier.color);
  g.centeredText(font,Component.literal("Les runes d’Aeloria révèlent votre fortune"),left+pw/2,top+22,Ui.MUTED);
  int x=left+12,y=top+44,rw=(pw-38)*3/5,right=x+rw+14,infoW=left+pw-right-12;
  int rh=Math.max(62,ph-139);boolean spinning=loot!=null&&elapsed<80,finished=loot!=null&&!spinning;
  Ui.inset(g,x,y,rw,rh);int cx=x+rw/2,cy=y+rh/2;
  // Luminous runes orbit the chamber even before opening; a raised 3D lucky model anchors the composition.
  for(int i=0;i<16;i++){double angle=i*Math.PI/8+(idle+partial)*.009;int px=cx+(int)(Math.cos(angle)*(rw*.46)),py=cy+(int)(Math.sin(angle)*(rh*.44));g.fill(px-1,py-1,px+2,py+2,tier.color);}
  if(!requested){g.pose().pushMatrix();float scale=Math.min(5,rh/25f);g.pose().translate(cx-8*scale,cy-8*scale+(float)Math.sin((idle+partial)*.04)*2);g.pose().scale(scale,scale);g.item(new ItemStack(com.mmorpg.registry.ModItems.LUCKY_BLOCKS.get(tier.ordinal()).get()),0,0);g.pose().popMatrix();}
  else{int cw=(rw-16)/3;
   for(int col=0;col<3;col++){int sx=x+6+col*(cw+2);Ui.inset(g,sx,y+8,cw,rh-16);g.enableScissor(sx+2,y+10,sx+cw-2,y+rh-10);
    boolean stopped=loot!=null&&elapsed>=48+col*16;int frame=(int)((elapsed+partial)/3)+col*7;float offset=spinning&&!stopped?((elapsed+partial)%3)/3*28:0;
    for(int row=-1;row<=1;row++){String id=stopped&&row==0?loot.getStringOr("item","mmorpg:piece_or"):table.isEmpty()?"mmorpg:piece_or":table.getCompoundOrEmpty(Math.floorMod(frame+row,table.size())).getStringOr("item","mmorpg:piece_or");g.pose().pushMatrix();g.pose().translate(sx+cw/2f-10,cy-10+row*28+offset);g.pose().scale(1.25f,1.25f);g.item(icon(id),0,0);g.pose().popMatrix();}g.disableScissor();
    g.fill(sx+1,cy-15,sx+cw-1,cy-14,tier.color);g.fill(sx+1,cy+15,sx+cw-1,cy+16,tier.color);
   }
  }
  Ui.header(g,right,y,infoW,"TRÉSORS & CHANCES");int rows=Math.max(1,(rh-26)/24),pages=Math.max(1,(table.size()+rows-1)/rows);page=Math.clamp(page,0,pages-1);
  for(int i=0;i<rows&&page*rows+i<table.size();i++){var entry=table.getCompoundOrEmpty(page*rows+i);int iy=y+17+i*24;var stack=icon(entry.getStringOr("item","mmorpg:piece_or"));g.item(stack,right,iy);
   String name=entry.getBooleanOr("gold",false)?"Pièces d’or":stack.getHoverName().getString();g.text(font,font.plainSubstrByWidth(name,infoW-21),right+20,iy,Ui.TEXT,false);
   int chance=entry.getIntOr("permille",0);g.text(font,entry.getIntOr("min",1)+"–"+entry.getIntOr("max",1)+" · "+chance/10+","+chance%10+" %",right+20,iy+10,tier.color,false);
  }
  button(g,right,y+rh-15,24,14,"‹",page>0,tier.color,()->page--);button(g,right+infoW-24,y+rh-15,24,14,"›",page+1<pages,tier.color,()->page++);
  g.centeredText(font,Component.literal((page+1)+" / "+pages),right+infoW/2,y+rh-12,Ui.MUTED);
  String label=!error.isEmpty()?"Fermer":!requested?"ÉVEILLER LE RELIQUAIRE":spinning?"Révéler maintenant":finished?"Fermer":"Invocation…";
  button(g,x,y+rh+8,rw,21,label,!requested||loot!=null||!error.isEmpty(),tier.color,()->{if(!error.isEmpty())onClose();else if(!requested)start();else if(spinning)elapsed=80;else onClose();});
  int resultY=top+ph-54;Ui.inset(g,x,resultY,pw-24,40);
  String title=!error.isEmpty()?error:finished?"✦ TRÉSOR OBTENU ✦":requested?"La récompense est déjà sécurisée côté serveur":"Un reliquaire consommé · Une récompense garantie";
  g.centeredText(font,Component.literal(font.plainSubstrByWidth(title,pw-40)),left+pw/2,resultY+6,error.isEmpty()?tier.color:Ui.RED);
  String detail=finished?(loot.getLongOr("gold",0)>0?loot.getLongOr("gold",0)+" pièces d’or":loot.getIntOr("count",1)+" × "+icon(loot.getStringOr("item","mmorpg:piece_or")).getHoverName().getString()):"Toutes les probabilités sont affichées à droite.";
  g.centeredText(font,Component.literal(font.plainSubstrByWidth(detail,pw-40)),left+pw/2,resultY+23,finished?Ui.GREEN:Ui.MUTED);
 }
}
