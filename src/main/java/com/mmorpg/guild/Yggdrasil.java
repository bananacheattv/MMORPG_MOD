package com.mmorpg.guild;

/** Ten original silhouettes; tiers 1–9 unlock levels 2–10, tier 10 is the final challenge. */
public final class Yggdrasil {
    public record Guardian(String id,String name,String form,int color,float width,float height,String attack){}
    public static final Guardian[] ALL={
        new Guardian("scarabee_racines","Kharvoss, Dévoreur des Racines","Scarabée cuirassé à six pattes",0x77BD53,5.5f,5,"Éruption de racines"),
        new Guardian("mante_epines","Sylkaris, Faux des Ronces","Mante aux quatre lames végétales",0x45DFA4,5,8,"Lames croisées"),
        new Guardian("raie_celeste","Orméa, Voile des Cieux","Raie céleste aux ailes immenses",0x68CFFF,8,5,"Pluie astrale"),
        new Guardian("hydre_corail","Thalassyr, Couronne des Profondeurs","Hydre corallienne à trois têtes",0x4DE3DB,6,8,"Marées convergentes"),
        new Guardian("millepattes_ambre","Varkhûn, Anneaux d’Ambre","Mille-pattes cuirassé colossal",0xFFB540,7,5,"Anneau de secousses"),
        new Guardian("meduse_oracles","Nyméthis, Oracle Suspendu","Méduse aux tentacules runiques",0xCE88FF,6,9,"Tentacules prophétiques"),
        new Guardian("crabe_bastion","Ghorzakk, Bastion Vivant","Crabe forteresse à pinces jumelles",0xC7A380,8,6,"Étau du bastion"),
        new Guardian("papillon_eclipse","Vespera, Mangeuse de Soleils","Papillon d’éclipse aux quatre ailes",0xA66BFF,8,8,"Éclipse radiale"),
        new Guardian("serpent_couronnes","Azheryn, Spirale des Mondes","Serpent dressé à couronnes orbitales",0xFF697F,6,11,"Spirale dévorante"),
        new Guardian("oeil_yggdrasil","Elyndraxx, Cœur de l’Arbre-Monde","Œil annulaire aux branches fractales",0xFFD66B,9,12,"Jugement de l’Arbre-Monde")};
    public static long xpRequired(int level){return level>=10?0:1000L*level*level;}
    public static boolean cleared(GuildStore.Guild g,int boss){return (g.bosses&(1<<boss))!=0;}
    public static void advance(GuildStore.Guild g){if(g.level>=10)throw new IllegalArgumentException("Niveau maximum atteint.");if(!cleared(g,g.level-1))throw new IllegalArgumentException("Le gardien de ce palier doit être vaincu.");long required=xpRequired(g.level);if(g.xp<required||g.gold<10000)throw new IllegalArgumentException("XP insuffisante ou moins de 10 000 or en banque.");g.xp-=required;g.gold-=10000;g.level++;g.log("Guilde passée au niveau "+g.level+" : 10 000 or.");}
}
