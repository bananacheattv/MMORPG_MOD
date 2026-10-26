package com.mmorpg.client.screen;

import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.server.SpawnEditor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import java.util.List;

/** Catalogue anchored to the block clicked with the operator's tool. */
public final class SpawnEditorScreen extends MmoScreen {
    private final String position;
    private int tab, page, facing, size=2, height=2;
    private EditBox text;
    private static final String[] DIRECTIONS={"north","east","south","west"};
    private static final String[] FACES={"Nord","Est","Sud","Ouest"};
    private static final float[] SIZES={.5f,1f,2f,3f,4f};
    public SpawnEditorScreen(CompoundTag data) { super("Aménagement du spawn");position=data.getStringOr("position",""); }
    public void showTab(int value) { tab=Math.clamp(value,0,3);page=0; }
    @Override protected void init() {
        super.init();
        String value=text==null?"Bienvenue à Aeloria":text.getValue();
        text=new EditBox(font,left+12,top+81,pw-24,20,Component.literal("Texte flottant"));
        text.setMaxLength(240);text.setValue(value);addRenderableWidget(text);
    }
    private void action(String command) {
        var mc=Minecraft.getInstance();
        if(mc.getConnection()!=null) mc.getConnection().sendCommand(command);
        mc.gui.setScreen(null);
    }
    @Override protected void renderContent(GuiGraphicsExtractor g,int mx,int my,float a) {
        Ui.frame(g,left,top,pw,ph,Ui.GOLD,false);
        g.centeredText(font,Component.literal("OUTIL D'AMÉNAGEMENT"),left+pw/2,top+8,Ui.GOLD);
        g.text(font,"Cible : "+position,left+12,top+23,Ui.MUTED,false);
        String[] tabs={"Structures","Portails","PNJ","Textes"};
        int tw=(pw-24)/4;
        for(int i=0;i<4;i++) {final int selected=i;button(g,left+12+i*tw,top+37,tw-3,18,tabs[i],true,tab==i?Ui.GOLD:Ui.MUTED,()->{tab=selected;page=0;});}
        text.visible=tab==3;text.active=tab==3;
        int bottom=top+ph;
        if(tab==3) {
            g.text(font,"Texte libre (240 caractères maximum)",left+12,top+65,Ui.TEXT,false);
            button(g,left+12,top+111,140,20,"Taille : "+SIZES[size],true,Ui.GOLD,()->size=(size+1)%SIZES.length);
            button(g,left+160,top+111,pw-172,20,"Hauteur : "+height+" blocs",true,Ui.GOLD,()->height=(height+1)%9);
            button(g,left+12,top+141,pw-24,22,"Placer le texte à la cible",!text.getValue().isBlank(),Ui.GREEN,
                    ()->action("amenagement texte "+SIZES[size]+" "+height+" "+text.getValue().replace('\n',' ')));
            button(g,left+12,top+171,pw-24,20,"Retirer le texte le plus proche (10 blocs)",true,Ui.RED,()->action("amenagement supprimertexte"));
        } else {
            button(g,left+12,top+61,pw-24,18,"Orientation : "+FACES[facing]+"  ›",true,Ui.GOLD,()->facing=(facing+1)%4);
            List<SpawnEditor.Entry> entries=SpawnEditor.catalogue().stream().filter(e->e.tab()==tab).toList();
            int rows=Math.max(1,(ph-143)/22), perPage=rows*2, pages=Math.max(1,(entries.size()+perPage-1)/perPage);
            page=Math.min(page,pages-1);int cw=(pw-28)/2;
            for(int i=0;i<perPage&&page*perPage+i<entries.size();i++) {
                var entry=entries.get(page*perPage+i);int x=left+12+(i%2)*(cw+4),y=top+86+(i/2)*22;
                String label=font.plainSubstrByWidth(entry.label(),cw-10);
                button(g,x,y,cw,19,label,true,Ui.GOLD,()->action("amenagement poser "+entry.id()+" "+DIRECTIONS[facing]));
            }
            button(g,left+12,bottom-52,50,17,"‹",page>0,Ui.GOLD,()->page--);
            g.centeredText(font,Component.literal((page+1)+" / "+pages),left+pw/2,bottom-48,Ui.MUTED);
            button(g,left+pw-62,bottom-52,50,17,"›",page+1<pages,Ui.GOLD,()->page++);
        }
        button(g,left+12,bottom-26,96,18,"Annuler /undo",true,Ui.GOLD,()->action("amenagement annuler"));
        button(g,left+114,bottom-26,118,18,"Point /spawn ici",true,Ui.GOLD,()->action("amenagement pointspawn"));
        button(g,left+238,bottom-26,pw-250,18,"Retirer portail",true,Ui.RED,()->action("portail supprimer"));
    }
}
