package com.mmorpg.block;
public enum LuckyTier {
 CELESTE("lucky_celeste","Céleste",0xFFE7C86B),SYLVESTRE("lucky_sylvestre","Sylvestre",0xFF69C68B),INFERNAL("lucky_infernal","Infernal",0xFFF07851),GIVRE("lucky_givre","Givre",0xFF8EDBFF),NEANT("lucky_neant","Néant",0xFFC08CFF);
 public final String id,label;public final int color;
 LuckyTier(String id,String label,int color){this.id=id;this.label=label;this.color=color;}
 public static LuckyTier of(int i){return values()[Math.clamp(i,0,4)];}
}
