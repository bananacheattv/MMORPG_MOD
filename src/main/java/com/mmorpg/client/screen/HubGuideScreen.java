package com.mmorpg.client.screen;

import com.mmorpg.client.ui.*;
import com.mmorpg.hub.HubGuides;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/** In-world character conversation, rather than a destination menu or an automatic book of pages. */
public final class HubGuideScreen extends MmoScreen {
    private final int guide,entity,completed;private final boolean hasClass;private int page;
    private com.mmorpg.entity.hub.HubGuideEntity portrait;
    private static final String[][][] DIALOGUE={
        {{"Une nouvelle histoire","Bienvenue à la Citadelle ! Je suis Sélène, l’archiviste. Ici commence ta route vers le niveau 100.","Tu progresseras avec les quêtes, les boss, la forge et tes compagnons."},{"Trouve ta voie","Les quatre Maîtres de classe t’attendent dans leurs pavillons : Guerrier, Mage, Archer et Paladin.","Essaie leurs compétences avant de confirmer ton choix. Tu recevras un équipement de niveau 1."}},
        {{"L’aventure se prépare","Je suis Kaël. Une classe choisie, traverse le portail du spawn pour rejoindre les ateliers et les marchands.","Dans les mines, cherche les minerais. Dans les plaines, chasse et récolte. Les donjons abritent les boss."},{"Pars avec ton groupe","Les portails physiques donnent accès aux mines, plaines et donjons.","En groupe, seul le meneur lance le départ. Les membres prêts le suivent ; à la sortie, consulte ton résumé de gains."}},
        {{"Une arme se mérite","Borin, maître artisan. Les boss et les quêtes donnent les parchemins des équipements que tu veux fabriquer.","Apprends le parchemin correspondant, réunis les matériaux, puis utilise la forge arcanique."},{"Entretiens tes outils","Dans Amélioration, applique les runes d’Abondance, Prospection, Célérité et Ténacité.","Chaque rune possède cinq rangs. Les prix et les effets sont visibles avant de dépenser ton or."}},
        {{"Des racines communes","Je suis Élyra. Une guilde réunit des membres, une banque d’or et une progression commune jusqu’au niveau 10.","Chaque passage demande de l’XP, un gardien d’Yggdrasil vaincu et 10 000 or de guilde."},{"Unis face aux gardiens","Les buffs profitent à tous les membres : 1 000 or par buff pendant une heure.","Un gardien vaincu peut être rejoué après six heures. Les groupes servent aux expéditions ; les invitations de guilde doivent être acceptées."}},
        {{"L’épreuve des équipes","Capitaine Néria. L’arène de feu accueille deux équipes de cinq ; les dunes, deux équipes de trois.","Tu affrontes des héros de ta tranche de vingt niveaux. L’inscription te laisse sur place jusqu’au groupe complet."},{"Contrôle le cristal","Une fois le groupe complet, confirme l’invitation. Frappe le cristal mobile pour le prendre aux couleurs de ton équipe.","Le cristal rapporte un point toutes les deux secondes, une élimination dix. La première équipe à 500 points gagne."}}
    };
    public HubGuideScreen(CompoundTag data){super("Conversation");guide=Math.clamp(data.getIntOr("guide",0),0,4);entity=data.getIntOr("entity",-1);completed=data.getIntOr("completed",0);hasClass=data.getBooleanOr("hasClass",false);}
    @Override protected void renderContent(GuiGraphicsExtractor g,int mx,int my,float partial){
        Ui.panel(g,left,top,pw,ph);Ui.title(g,left+pw/2,top+12,HubGuides.NAMES[guide],Ui.GOLD);
        int portraitW=Math.min(122,pw/3),x=left+portraitW+20,w=pw-portraitW-34;
        Ui.inset(g,left+10,top+36,portraitW,ph-77);
        if(portrait==null&&minecraft.level!=null){portrait=com.mmorpg.registry.ModEntities.HUB_GUIDE.get().create(minecraft.level,net.minecraft.world.entity.EntitySpawnReason.COMMAND);portrait.setId(130000+guide);portrait.guide(guide);portrait.setCustomNameVisible(false);}
        if(portrait!=null)com.mmorpg.client.ui.Ui.entityFollowsMouse(g,left+12,top+39,left+portraitW+8,top+ph-45,Math.max(15,(ph-105)/3),.0625f,mx,my,portrait);
        var text=DIALOGUE[guide][page];Ui.wrap(g,text[0],x,top+42,w,Ui.GOLD_LIGHT);int y=top+66;
        for(int i=1;i<text.length;i++){Ui.wrap(g,text[i],x,y,w,Ui.TEXT);y+=font.split(net.minecraft.network.chat.Component.literal(text[i]),w).size()*10+12;}
        String footer=(Integer.bitCount(completed&31))+" / 5 guides rencontrés";g.text(font,footer,left+12,top+ph-14,Ui.MUTED,false);
        if(page>0)button(g,x,top+ph-35,70,18,"Retour",true,Ui.GOLD,()->page--);
        button(g,left+pw-146,top+ph-35,134,18,page==0?"Continuer":"Merci, j’ai compris",true,Ui.GREEN,()->{if(page==0)page++;else{com.mmorpg.client.ClientNet.send(new com.mmorpg.network.Payloads.HubAction(com.mmorpg.network.Payloads.HubAction.GUIDE_COMPLETE,entity,0));onClose();}});
    }
    @Override public void tick(){if(portrait!=null)portrait.tickCount++;}
}
