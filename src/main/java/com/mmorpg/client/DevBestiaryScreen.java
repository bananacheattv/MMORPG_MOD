package com.mmorpg.client;

import com.mmorpg.client.ui.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.LivingEntity;
import java.util.*;

/** Opt-in contact sheets using real entity renderers, materials and animation rigs. */
public final class DevBestiaryScreen extends MmoScreen {
    private final List<String> ids=new ArrayList<>();private final List<LivingEntity> entities=new ArrayList<>();private int page;
    public DevBestiaryScreen(){
        super("Vérification des modèles");
        ids.addAll(List.of("zombie_des_cryptes","araignee_venimeuse","bandit_arbaletrier","gobelin","loup_sombre","squelette_maudit","orc_guerrier","elementaire_feu","spectre_givre","golem_cristal","chevalier_neant"));
        ids.addAll(List.of("roi_gobelin","liche_ancienne","seigneur_ignis","titan_glace","avatar_neant"));
        for(var mount:com.mmorpg.mount.MountType.values())ids.add("monture_"+mount.id);
        for(var mob:com.mmorpg.entity.mob.ImportedMobs.ALL)ids.add(mob.key());
    }
    public int pages(){return (ids.size()+3)/4;}
    public void page(int page){this.page=page;}
    @Override protected int panelWidth(){return width-8;}
    @Override protected int panelHeight(){return height-8;}
    @Override public void tick(){super.tick();for(var e:entities)if(e!=null)e.tickCount++;}
    @Override protected void renderContent(GuiGraphicsExtractor g,int mx,int my,float a){
        if(minecraft.level==null)return;
        if(entities.isEmpty())for(String id:ids){var type=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(com.mmorpg.MMORPG.id(id));var e=type==null?null:type.create(minecraft.level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);if(e!=null)e.setId(100000+entities.size());entities.add(e instanceof LivingEntity living?living:null);}
        Ui.panel(g,left,top,pw,ph);Ui.title(g,left+pw/2,top+7,"BESTIAIRE · MODÈLES CUBIQUES · "+(page+1)+" / "+pages(),Ui.GOLD);
        int w=(pw-24)/2,h=(ph-39)/2;
        for(int i=0;i<4;i++){
            int index=page*4+i;if(index>=ids.size())continue;
            int x=left+8+(i%2)*(w+8),y=top+28+(i/2)*(h+5);Ui.inset(g,x,y,w,h);String id=ids.get(index);
            g.centeredText(font,net.minecraft.network.chat.Component.literal(id.replace('_',' ')),x+w/2,y+5,Ui.GOLD);
            var entity=entities.get(index);if(entity==null){g.text(font,"Entité indisponible",x+8,y+30,Ui.RED,false);continue;}
            float height=entity.getBbHeight();
            if(id.startsWith("monture_"))height=Math.max(height,4);
            for(var entry:com.mmorpg.entity.mob.ImportedMobs.ALL)if(entry.key().equals(id))height=entry.height()*entry.scale()+entry.hover();
            int scale=Math.max(6,Math.min(70,(int)((h-30)/height)));
            net.minecraft.client.gui.screens.inventory.InventoryScreen.extractEntityInInventoryFollowsMouse(g,x+4,y+19,x+w-4,y+h-4,scale,.0625f,x+w/2f+35,y+h/2f-5,entity);
        }
    }
}
