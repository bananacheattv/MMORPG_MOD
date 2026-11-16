package com.mmorpg.store;
import net.minecraft.network.chat.*;
import net.minecraft.resources.Identifier;
public final class RankTags {
 private static final java.util.List<String> LABELS=java.util.List.of("Aelorien","Éclaireur","Gardien","Arcaniste","Élu","Fondateur","Responsable","Modérateur","Builder");
 public static Component name(String label,int color,String username){return name(label,color,username,true);}
 public static Component name(String label,int color,String username,boolean bold){int index=LABELS.indexOf(label);MutableComponent result=Component.empty();if(index>=0)result.append(Component.literal(String.valueOf((char)(0xE500+index))).withStyle(s->s.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("mmorpg","rank_badges")))));else result.append(Component.literal(label).withColor(color).withStyle(s->s.withBold(bold)));return result.append(" ").append(Component.literal(username).withColor(color).withStyle(s->s.withBold(bold)));}
}
