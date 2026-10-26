package com.mmorpg.client.screen;

import com.mmorpg.client.ui.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public final class AdventureSummaryScreen extends MmoScreen {
    private final CompoundTag data;private int page;
    public AdventureSummaryScreen(CompoundTag data) {super("Bilan d'aventure");this.data=data;}
    @Override protected void renderContent(GuiGraphicsExtractor g,int mx,int my,float a) {
        Ui.frame(g,left,top,pw,ph,Ui.GOLD,false);
        String name=switch(data.getStringOr("kind","")){case "mine"->"MINES";case "plaines"->"PLAINES";default->"DONJON";};
        g.centeredText(font,Component.literal("BILAN · "+name),left+pw/2,top+10,Ui.GOLD);
        long seconds=data.getLongOr("duration",0)/1000;
        g.text(font,"Durée : "+seconds/60+" min "+seconds%60+" s   ·   Mobs : "+data.getLongOr("kills",0),left+12,top+32,Ui.TEXT,false);
        g.text(font,"XP : +"+data.getLongOr("xp",0)+"     Or : +"+data.getLongOr("gold",0),left+12,top+48,Ui.GREEN,false);
        var items=data.getCompoundOrEmpty("items");var ids=items.keySet().stream().sorted().toList();
        int rows=Math.max(1,(ph-116)/22),pages=Math.max(1,(ids.size()+rows-1)/rows);page=Math.min(page,pages-1);
        if(ids.isEmpty())g.text(font,"Aucun objet récupéré.",left+12,top+80,Ui.MUTED,false);
        for(int i=0;i<rows&&page*rows+i<ids.size();i++) {
            String id=ids.get(page*rows+i);var item=com.mmorpg.crafting.ForgeRecipes.resolveItem(id);int y=top+74+i*22;
            g.item(new ItemStack(item),left+12,y);g.text(font,font.plainSubstrByWidth(new ItemStack(item).getHoverName().getString(),pw-100),left+34,y+4,Ui.TEXT,false);
            g.text(font,"× "+items.getLongOr(id,0),left+pw-66,y+4,Ui.GOLD,false);
        }
        button(g,left+12,top+ph-48,42,16,"‹",page>0,Ui.GOLD,()->page--);
        button(g,left+pw-54,top+ph-48,42,16,"›",page+1<pages,Ui.GOLD,()->page++);
        g.centeredText(font,Component.literal((page+1)+" / "+pages),left+pw/2,top+ph-45,Ui.MUTED);
        button(g,left+12,top+ph-26,pw-24,18,"Continuer",true,Ui.GOLD,()->net.minecraft.client.Minecraft.getInstance().gui.setScreen(null));
    }
}
