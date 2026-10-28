package com.mmorpg.client.screen;

import com.mmorpg.client.ui.*;
import com.mmorpg.client.ClientNet;
import com.mmorpg.network.Payloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.decoration.ArmorStand;

public final class ArmorStandEditorScreen extends MmoScreen {
    private final int entity;private int slot,part;private boolean position;private int mode,model;private net.minecraft.client.gui.components.EditBox skinName;
    private static final String[] SLOTS={"Main droite","Main gauche","Tête","Plastron","Jambières","Bottes"};
    private static final String[] PARTS={"Tête","Corps","Bras gauche","Bras droit","Jambe gauche","Jambe droite"};
    public ArmorStandEditorScreen(CompoundTag data){super("Mise en scène");entity=data.getIntOr("entity",-1);}
    @Override protected void init(){super.init();skinName=new net.minecraft.client.gui.components.EditBox(font,left+pw/2+12,top+109,pw/2-28,18,net.minecraft.network.chat.Component.literal("Pseudo du skin"));skinName.setMaxLength(16);skinName.setHint(net.minecraft.network.chat.Component.literal("Pseudo Minecraft"));addRenderableWidget(skinName);}
    @Override protected int panelWidth(){return Math.min(width-12,620);}
    @Override protected int panelHeight(){return Math.min(height-12,340);}
    private void send(String action,java.util.function.Consumer<CompoundTag> fill){var t=new CompoundTag();t.putInt("entity",entity);t.putString("action",action);fill.accept(t);ClientNet.send(new Payloads.SocialAction("armorstand",t));}
    @Override protected void renderContent(GuiGraphicsExtractor g,int mx,int my,float a){
        skinName.setVisible(mode==2);
        Ui.panel(g,left,top,pw,ph);Ui.title(g,left+pw/2,top+9,"PORTE-ARMURE · MISE EN SCÈNE",Ui.GOLD);
        if(minecraft.level==null||minecraft.player==null||!(minecraft.level.getEntity(entity) instanceof ArmorStand stand)){g.text(font,"Porte-armure trop éloigné.",left+12,top+45,Ui.RED,false);return;}
        int split=left+pw/2;
        button(g,left+12,top+35,pw/2-20,18,"Équipement : "+SLOTS[slot]+" ›",true,Ui.GOLD,()->slot=(slot+1)%6);
        g.text(font,"Clique un objet de ton inventaire :",left+12,top+62,Ui.TEXT,false);
        int columns=9,cell=Math.max(18,Math.min(24,(pw/2-24)/9));
        for(int i=0;i<36;i++){int index=i,x=left+12+(i%columns)*cell,y=top+79+(i/columns)*cell;Ui.frame(g,x,y,cell-1,cell-1,Ui.MUTED,false);var stack=minecraft.player.getInventory().getItem(i);g.item(stack,x+(cell-16)/2,y+(cell-16)/2);click(x,y,cell-1,cell-1,()->send("equip",t->{t.putInt("slot",slot);t.putInt("inventory",index);}));if(hovered(x,y,cell-1,cell-1)&&!stack.isEmpty())g.setTooltipForNextFrame(font,stack,mouseX,mouseY);}
        button(g,left+12,top+85+cell*4,pw/2-24,17,"Vider cet emplacement",true,Ui.RED,()->send("equip",t->{t.putInt("slot",slot);t.putInt("inventory",-1);}));
        int previewTop=top+114+cell*4;
        net.minecraft.client.gui.screens.inventory.InventoryScreen.extractEntityInInventoryFollowsMouse(g,left+18,previewTop,split-12,top+ph-12,Math.max(20,(top+ph-12-previewTop)/2),.0625f,mouseX,mouseY,stand);
        button(g,split+8,top+35,pw/2-20,18,"Articulation : "+PARTS[part]+" ›",true,Ui.GOLD,()->part=(part+1)%6);
        button(g,split+10,top+ph-26,pw/2-22,18,new String[]{"Position ›","Apparence ›","Poses ›"}[mode],true,Ui.GOLD,()->{mode=(mode+1)%3;position=mode==1;});
        if(mode==2){
         var names=com.mmorpg.entity.NpcEntity.APPEARANCES;
         button(g,split+12,top+65,pw/2-28,20,"Modèle 3D : "+names[model+4]+" ›",true,Ui.GOLD,()->{model=(model+1)%com.mmorpg.entity.NpcEntity.AUTHORED_MODELS.length;send("appearance",t->t.putString("appearance","guides:"+com.mmorpg.entity.NpcEntity.AUTHORED_MODELS[model]));});
         g.text(font,"Ou un skin de joueur :",split+12,top+95,Ui.TEXT,false);
         button(g,split+12,top+135,pw/2-28,20,"Appliquer ce skin",skinName.getValue().matches("[A-Za-z0-9_]{3,16}"),Ui.GOLD,()->send("appearance",t->t.putString("appearance","skin:"+skinName.getValue())));
         button(g,split+12,top+164,pw/2-28,18,"Porte-armure classique",true,Ui.MUTED,()->send("appearance",t->t.putString("appearance","")));
        }else if(!position){
        var rotation=com.mmorpg.server.ArmorStandEditor.pose(stand,part);float[] angles={rotation.x(),rotation.y(),rotation.z()};
        for(int axis=0;axis<3;axis++){int ax=axis,y=top+66+axis*25;g.text(font,"XYZ".substring(axis,axis+1)+" : "+Math.round(angles[axis])+"°",split+12,y+4,Ui.TEXT,false);for(int sign:new int[]{-1,1})button(g,split+pw/2-90+(sign==1?38:0),y,32,18,sign<0?"−5":"+5",true,Ui.GOLD,()->send("pose",t->{t.putInt("part",part);t.putInt("axis",ax);t.putInt("delta",sign*5);}));}
        String[] presets={"Repos","Attaque","Tir à l’arc","Course"};for(int i=0;i<4;i++){int preset=i;button(g,split+10+(i%2)*(pw/4-8),top+148+(i/2)*23,pw/4-12,18,presets[i],true,Ui.GOLD,()->send("preset",t->t.putInt("preset",preset)));}
        }else{
        g.text(font,"Position / orientation",split+12,top+67,Ui.GOLD,false);
        for(int axis=0;axis<4;axis++){int ax=axis,y=top+84+axis*25;g.text(font,new String[]{"X","Hauteur","Z","Rotation"}[axis],split+12,y+4,Ui.TEXT,false);for(int sign:new int[]{-1,1})button(g,split+pw/2-90+(sign==1?38:0),y,32,17,sign<0?"−":"+",true,Ui.GOLD,()->send("move",t->{t.putInt("axis",ax);t.putInt("delta",sign);}));}
        }
    }
}
