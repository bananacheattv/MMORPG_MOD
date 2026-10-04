package com.mmorpg.item;

import com.mmorpg.combat.RpgCombat;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/** Arc RPG : ne consomme pas de fleches, les degats dependent de l'Attaque du joueur et de la tension de l'arc. */
public class RpgBowItem extends BowItem implements RpgEquipment {
    private final RpgItemDef def;

    public RpgBowItem(Properties properties, RpgItemDef def) {
        super(properties.stacksTo(1));
        this.def = def;
    }

    @Override
    public RpgItemDef rpgDef() {
        return def;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int remainingTime) {
        if (!(entity instanceof Player player)) {
            return false;
        }
        int held = this.getUseDuration(stack, entity) - remainingTime;
        float pow = getPowerForTime(held);
        if (pow < 0.1F) {
            return false;
        }
        if (level instanceof ServerLevel) {
            Arrow arrow = new Arrow(level, player, new ItemStack(Items.ARROW), stack);
            arrow.shootFromRotation(player, player.getXRot(), player.getYRot(), 0.0F, pow * 3.2F, 0.8F);
            arrow.setCritArrow(pow >= 1.0F);
            arrow.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
            RpgCombat.tagArrowMultiplier(arrow, pow >= 1.0F ? 1.15 : pow);
            level.addFreshEntity(arrow);
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ARROW_SHOOT, SoundSource.PLAYERS,
                1.0F, 1.0F / (level.getRandom().nextFloat() * 0.4F + 1.2F) + pow * 0.5F);
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        MutableComponent name = Component.translatable(this.getDescriptionId()).withColor(def.rarity().rgb());
        int up = RpgEquipment.upgrade(stack);
        if (up > 0) name.append(Component.literal(" +" + up).withColor(0xFFD040));
        return name;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        ItemTooltips.equipment(stack, def, "Arc", out);
        out.accept(Component.literal("Ne nécessite pas de flèches").withColor(0x80C0FF));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return RpgEquipment.upgrade(stack) >= 7 || super.isFoil(stack);
    }
}
