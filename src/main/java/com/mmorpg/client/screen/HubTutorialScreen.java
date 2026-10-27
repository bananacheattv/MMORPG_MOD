package com.mmorpg.client.screen;

import com.mmorpg.client.ui.*;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class HubTutorialScreen extends MmoScreen {
    private int page;
    private static final String[][] PAGES={
        {"Bienvenue dans Aeloria","Ton objectif : devenir l’un des meilleurs héros, jusqu’au niveau 100.","Commence dans ce hub en choisissant ta classe."},
        {"1 · Essaie ta classe","Parle à un Maître de classe : Guerrier, Mage, Archer ou Paladin.","Lance un essai, teste ses compétences, puis confirme ton choix.","Tu reçois un équipement de niveau 1 pour commencer."},
        {"2 · Rejoins le spawn","Traverse le portail du spawn une fois ta classe choisie.","Retrouve les marchands, la banque et la forge arcanique.","La touche M ouvre ton menu. U personnalise ton interface."},
        {"3 · Explore par les portails","Entre dans un portail pour les mines, plaines ou donjons.","Les mines fournissent les minerais ; les plaines les récoltes et monstres.","En groupe, le meneur décide du départ de tous les membres prêts."},
        {"4 · Progresse et fabrique","Termine des quêtes et bats les boss pour leurs parchemins d’équipement.","Apprends un parchemin, rassemble les matériaux et fabrique à la forge.","Les outils s’améliorent avec des runes dans l’onglet Amélioration."},
        {"5 · Joue avec les autres","Les groupes servent à partir à l’aventure ensemble.","Les guildes ont leurs membres, leur banque, leurs buffs et Yggdrasil.","Les arènes opposent deux équipes ; tu restes sur place durant l’inscription.","Tu peux relire ce guide avec /tuto."}
    };
    public HubTutorialScreen(){super("Guide d’Aeloria");}
    @Override protected void renderContent(GuiGraphicsExtractor g,int mx,int my,float a){
        Ui.panel(g,left,top,pw,ph);Ui.title(g,left+pw/2,top+12,"GUIDE D’AELORIA",Ui.GOLD);
        var text=PAGES[page];g.centeredText(font,Component.literal(text[0]),left+pw/2,top+43,Ui.GOLD_LIGHT);
        int y=top+70;for(int i=1;i<text.length;i++){Ui.wrap(g,text[i],left+16,y,pw-32,Ui.TEXT);y+=32;}
        button(g,left+12,top+ph-29,90,18,"Précédent",page>0,Ui.GOLD,()->page--);
        g.centeredText(font,Component.literal((page+1)+" / "+PAGES.length),left+pw/2,top+ph-23,Ui.MUTED);
        button(g,left+pw-116,top+ph-29,104,18,page==PAGES.length-1?"Commencer":"Suivant",true,Ui.GREEN,()->{if(page==PAGES.length-1)onClose();else page++;});
    }
}
