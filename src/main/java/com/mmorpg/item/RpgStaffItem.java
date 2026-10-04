package com.mmorpg.item;

import com.mmorpg.combat.SkillHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Baton de mage : le clic droit lance un projectile magique de l'element du baton. */
public class RpgStaffItem extends RpgWeaponItem {
    private final ItemDefs.Element element;

    public RpgStaffItem(Properties properties, RpgItemDef def, ItemDefs.Element element) {
        super(properties, def, ItemDefs.WeaponKind.STAFF);
        this.element = element == null ? ItemDefs.Element.ARCANE : element;
    }

    public ItemDefs.Element element() {
        return element;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) {
            return InteractionResult.PASS;
        }
        if (player instanceof ServerPlayer sp) {
            if (SkillHelper.castStaffBolt(sp, stack, this)) {
                player.getCooldowns().addCooldown(stack, 12);
                player.swing(hand, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
            }
        }
        return InteractionResult.SUCCESS;
    }
}
