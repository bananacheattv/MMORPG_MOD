package com.mmorpg.mine;

import com.mmorpg.MMORPG;
import com.mmorpg.registry.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid=MMORPG.MODID)
public final class ToolRunes {
    public static final String[] IDS={"rune_abondance","rune_prospection","rune_celerite","rune_tenacite"};
    public static final String[] NAMES={"Abondance","Prospection","Célérité","Ténacité"};
    public static final String[] EFFECTS={"+10 % de chance de rendement par rang","+15 % de chance de lingot rare par rang","+15 % de vitesse de minage par rang","15 % de durabilité économisée par rang"};
    public static boolean tool(ItemStack stack){return stack.getItem() instanceof MineTools.Pickaxe||MineTools.HOE_TIER.containsKey(stack.getItem());}
    public static int rank(ItemStack stack,int rune){return (stack.getOrDefault(ModDataComponents.TOOL_RUNES.get(),0)>>(rune*3))&7;}
    public static int cost(ItemStack stack,int rune){return 250*(rank(stack,rune)+1);}
    public static void tooltip(ItemStack stack,java.util.function.Consumer<Component> out){for(int i=0;i<IDS.length;i++){int rank=rank(stack,i);if(rank>0)out.accept(Component.literal("✦ "+NAMES[i]+" "+rank+" / 5 · "+EFFECTS[i]).withColor(0x8DE8EB));}}
    public static boolean preserve(ItemStack stack,net.minecraft.util.RandomSource random){return random.nextFloat()<rank(stack,3)*.15f;}
    public static void apply(ServerPlayer p,int slot,String id){
        int rune=java.util.Arrays.asList(IDS).indexOf(id);if(rune<0||slot<0||slot>=p.getInventory().getContainerSize())return;
        var stack=p.getInventory().getItem(slot);if(!tool(stack)||rank(stack,rune)>=5||(rune==1&&!(stack.getItem() instanceof MineTools.Pickaxe)))return;
        int cost=cost(stack,rune);if(com.mmorpg.server.RpgPlayers.get(p).gold<cost)return;
        var material=ModItems.get(id);ItemStack token=null;for(var item:p.getInventory())if(item.is(material)){token=item;break;}if(token==null)return;
        token.shrink(1);com.mmorpg.server.RpgPlayers.takeGold(p,cost);
        int packed=stack.getOrDefault(ModDataComponents.TOOL_RUNES.get(),0)+(1<<(rune*3));stack.set(ModDataComponents.TOOL_RUNES.get(),packed);
        stack.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE,true);
        p.inventoryMenu.broadcastChanges();
        p.level().sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,p.getX(),p.getY()+1,p.getZ(),40,.5,.7,.5,.5);
        p.playSound(net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,1,1.2f);
        p.sendSystemMessage(Component.literal("Rune "+NAMES[rune]+" améliorée au rang "+rank(stack,rune)+" / 5.").withColor(0x80E0E0));
    }
    @SubscribeEvent public static void speed(net.neoforged.neoforge.event.entity.player.PlayerEvent.BreakSpeed event){event.setNewSpeed(event.getNewSpeed()*(1+rank(event.getEntity().getMainHandItem(),2)*.15f));}
}
