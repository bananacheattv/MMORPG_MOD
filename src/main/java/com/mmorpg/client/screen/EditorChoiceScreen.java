package com.mmorpg.client.screen;

import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.*;
import java.util.function.Consumer;

/** Searchable chooser shared by zones, NPCs, objectives and rewards. */
public final class EditorChoiceScreen extends MmoScreen {
    public record Choice(String id,String label) {}
    private final Screen parent;
    private final List<Choice> choices;
    private final Consumer<String> select;
    private final String heading;
    private String query="";
    private int scroll;
    public EditorChoiceScreen(Screen parent,String title,List<Choice> choices,Consumer<String> select) {
        super(title);this.parent=parent;this.heading=title;this.choices=choices;this.select=select;
    }
    @Override protected int panelWidth() {return Math.min(width-12,600);}
    @Override protected int panelHeight() {return height-12;}
    @Override protected void init() {
        super.init();
        var search=new EditBox(font,left+10,top+29,pw-20,18,Component.literal("Rechercher"));
        search.setMaxLength(100);search.setValue(query);search.setHint(Component.literal("Rechercher par nom…"));
        search.setResponder(v->{query=v;scroll=0;});addRenderableWidget(search);setInitialFocus(search);
    }
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy) {scroll=Math.max(0,scroll-(int)Math.signum(sy));return true;}
    @Override protected void renderContent(GuiGraphicsExtractor g,int mx,int my,float a) {
        Ui.panel(g,left,top,pw,ph);Ui.title(g,left+pw/2,top+9,heading,Ui.GOLD);
        var filtered=choices.stream().filter(c->(c.label()+" "+c.id()).toLowerCase(Locale.ROOT).contains(query.toLowerCase(Locale.ROOT))).toList();
        int rows=Math.max(1,(ph-81)/20);scroll=Math.min(scroll,Math.max(0,filtered.size()-rows));
        for(int i=0;i<Math.min(rows,filtered.size()-scroll);i++) {
            var choice=filtered.get(i+scroll);int y=top+54+i*20;
            button(g,left+10,y,pw-20,18,font.plainSubstrByWidth(choice.label(),pw-32),true,Ui.GOLD,()->{select.accept(choice.id());minecraft.gui.setScreen(parent);});
        }
        button(g,left+10,top+ph-24,90,18,"Retour",true,Ui.GOLD,this::onClose);
        g.text(font,filtered.size()+" résultat(s)",left+110,top+ph-19,Ui.MUTED,false);
    }
    @Override public void onClose() {minecraft.gui.setScreen(parent);}
}
