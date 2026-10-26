package com.mmorpg.guild;

import com.mmorpg.rpg.*;

public final class GuildBuffs {
    public static final long COST=1000,DURATION_MS=3_600_000L;
    public enum Kind {
        VITALITE("Vitalité",1),PUISSANCE("Puissance",3),REMPART("Rempart",5),SAVOIR("Savoir",7);
        public final String label;public final int requiredLevel;Kind(String label,int level){this.label=label;requiredLevel=level;}
        public String bonus(int level){return switch(this){case VITALITE->"+"+(level*2)+" % PV";case PUISSANCE->"+"+(level*1.5)+" % ATQ / MAG";case REMPART->"+"+level+" % DEF";case SAVOIR->"+"+level+" % XP";};}
    }
    public static boolean active(PlayerData data,Kind kind){return data.guildLevel>=kind.requiredLevel&&data.guildBuffs[kind.ordinal()]>System.currentTimeMillis();}
    public static void apply(PlayerData data,StatBlock stats){int level=data.guildLevel;
        if(active(data,Kind.VITALITE))stats.set(Stat.MAX_HP,stats.get(Stat.MAX_HP)*(1+level*.02));
        if(active(data,Kind.PUISSANCE)){stats.set(Stat.ATK,stats.get(Stat.ATK)*(1+level*.015));stats.set(Stat.MAG,stats.get(Stat.MAG)*(1+level*.015));}
        if(active(data,Kind.REMPART))stats.set(Stat.DEF,stats.get(Stat.DEF)*(1+level*.01));
    }
    public static void activate(GuildStore.Guild guild,Kind kind,long now){
        GuildService.require(guild.level>=kind.requiredLevel,"Ce buff exige une guilde de niveau "+kind.requiredLevel+".");
        GuildService.require(guild.buffs.getOrDefault(kind.name(),0L)<=now,"Ce buff est déjà actif.");
        GuildService.require(guild.gold>=COST,"Il faut 1 000 or dans la banque de guilde.");
        guild.gold-=COST;guild.buffs.put(kind.name(),Math.addExact(now,DURATION_MS));guild.log(kind.label+" activé pour tous les membres : 1 heure, 1 000 or.");
    }
}
