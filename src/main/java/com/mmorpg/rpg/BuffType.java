package com.mmorpg.rpg;

/** Effets temporaires appliques par les competences. */
public enum BuffType {
    WAR_CRY("Cri de Guerre", "cri_de_guerre", 0xFFFF8040),
    RAGE("Rage Sanguinaire", "rage_sanguinaire", 0xFFE01818),
    ARCANE_GUARD("Garde Arcanique", "teleportation_arcanique", 0xFFC080FF),
    MANA_SHIELD("Bouclier de Mana", "bouclier_de_mana", 0xFF60A0FF),
    AGILITY("Élan Acrobatique", "saut_arriere", 0xFF80FF80),
    TAUNT("Provocation", "provocation", 0xFFFF6040),
    FORTRESS("Forteresse", "forteresse", 0xFFB0B8C8),
    SACRED_AURA("Aura Sacrée", "aura_sacree", 0xFFFFE080),
    TITAN("Rempart du Titan", "rempart_du_titan", 0xFFFFD040),
    ARMOR_FRACTURE("Armure fracturée · Défense -25 %", "cri_de_guerre", 0xFFFF7850),
    ARCANE_EXHAUSTION("Épuisement arcanique · Drain de mana", "bouclier_de_mana", 0xFFB080FF),
    MORTAL_WOUND("Plaie profonde · Soins -40 %", "rage_sanguinaire", 0xFFEF5070);

    public boolean harmful(){return this==ARMOR_FRACTURE||this==ARCANE_EXHAUSTION||this==MORTAL_WOUND;}
    public String shortLabel(){return switch(this){
        case WAR_CRY->"Attaque renforcée";case RAGE->"Rage sanguinaire";case ARCANE_GUARD->"Garde arcanique";case MANA_SHIELD->"Bouclier de mana";
        case AGILITY->"Agilité renforcée";case TAUNT->"Provocation active";case FORTRESS->"Défense renforcée";case SACRED_AURA->"Aura de soin";case TITAN->"Protection du Titan";
        case ARMOR_FRACTURE->"Défense réduite";case ARCANE_EXHAUSTION->"Mana drainé";case MORTAL_WOUND->"Soins réduits";
    };}
    public final String label;
    public final String icon;
    public final int color;

    BuffType(String label, String icon, int color) {
        this.label = label;
        this.icon = icon;
        this.color = color;
    }
}
