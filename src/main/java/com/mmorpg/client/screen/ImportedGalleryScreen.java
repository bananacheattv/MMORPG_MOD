package com.mmorpg.client.screen;

import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.entity.mob.ImportedMobs;
import com.mmorpg.registry.ModEntities;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.EntitySpawnReason;

/** Development-only contact sheet: same registered renderers as world entities. */
public final class ImportedGalleryScreen extends MmoScreen {
    private final int zone;
    private final java.util.List<com.mmorpg.entity.mob.ImportedMobEntity> mobs=new java.util.ArrayList<>();
    public ImportedGalleryScreen(int zone) { super("Modèles importés");this.zone=zone; }
    @Override protected int panelWidth() {return width-12;}
    @Override protected int panelHeight() {return height-12;}
    @Override protected void init() {
        super.init();mobs.clear();
        for(var entry:ImportedMobs.ALL) if(entry.zone()==zone) {
            var mob=ModEntities.IMPORTED.get(entry.key()).get().create(minecraft.level,EntitySpawnReason.COMMAND);
            mob.setId(-100-mobs.size());mobs.add(mob);
        }
    }
    @Override public void tick() { for(var mob:mobs) {mob.tickCount++;mob.walkAnimation.update(.12f,1,1);}}
    @Override protected void renderContent(GuiGraphicsExtractor g,int mx,int my,float partial) {
        Ui.panel(g,left,top,pw,ph);Ui.title(g,left+pw/2,top+10,ImportedMobs.ZONES[zone],Ui.GOLD);
        int cw=(pw-16)/5;
        for(int i=0;i<mobs.size();i++) {
            var mob=mobs.get(i);int x=left+8+i*cw,y=top+30;
            Ui.inset(g,x,y,cw-3,ph-42);
            int scale=(int)Math.min((ph-80)/mob.getBbHeight(),(cw-10)/Math.max(mob.getBbWidth(),1.5f));
            InventoryScreen.extractEntityInInventoryFollowsMouse(g,x+2,y+5,x+cw-5,top+ph-30,scale,.0625f,x+cw/2-20,y+40,mob);
            String name=ImportedMobs.ALL[zone*5+i].name();
            Ui.wrap(g,name,x+4,top+ph-27,cw-10,Ui.TEXT);
        }
    }
}
