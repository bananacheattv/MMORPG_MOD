package com.mmorpg.client;
import com.mmorpg.client.ui.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import java.util.*;
public final class DevHubGuideGallery extends MmoScreen {
    private final java.util.List<net.minecraft.world.entity.LivingEntity> actors=new ArrayList<>();public int page;
    public DevHubGuideGallery(){super("Guides");}
    @Override protected int panelWidth(){return width-12;}
    @Override protected int panelHeight(){return height-12;}
    @Override protected void renderContent(GuiGraphicsExtractor g,int mx,int my,float a){
        if(actors.isEmpty()&&minecraft.level!=null)for(int i=0;i<10;i++){
            net.minecraft.world.entity.LivingEntity actor;
            if(i<5){var guide=com.mmorpg.registry.ModEntities.HUB_GUIDE.get().create(minecraft.level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);guide.guide(i);actor=guide;}else{var master=com.mmorpg.registry.ModEntities.CLASS_MASTER.get().create(minecraft.level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);master.setup(i-5);actor=master;}
            actor.setId(140000+i);actor.setCustomNameVisible(false);actors.add(actor);
        }
        Ui.panel(g,left,top,pw,ph);Ui.title(g,left+pw/2,top+8,"GUIDES ET MAÎTRES · "+(page+1)+" / 3",Ui.GOLD);
        int w=(pw-24)/2,h=(ph-40)/2;
        for(int i=0;i<4;i++){int index=page*4+i;if(index>=actors.size())continue;int x=left+8+i%2*(w+8),y=top+28+i/2*(h+4);Ui.inset(g,x,y,w,h);String name=index<5?com.mmorpg.hub.HubGuides.NAMES[index]:com.mmorpg.entity.hub.ClassMasterEntity.NAMES[index-5];g.centeredText(font,net.minecraft.network.chat.Component.literal(name),x+w/2,y+5,Ui.GOLD);net.minecraft.client.gui.screens.inventory.InventoryScreen.extractEntityInInventoryFollowsMouse(g,x+2,y+20,x+w-2,y+h-3,(h-30)/3,.0625f,x+w/2f+25,y+h/2f,actors.get(index));}
    }
    @Override public void tick(){for(var a:actors)a.tickCount++;}
}
