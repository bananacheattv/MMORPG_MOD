package com.mmorpg.item;

import com.mmorpg.rpg.Rarity;
import com.mmorpg.server.PortalTool;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/** Outil portail (operateurs) : clic droit dans le vide = choisir la destination, clic droit sur un bloc = le transformer en portail. */
public class PortalToolItem extends RpgMaterialItem {
    public PortalToolItem(Properties properties) {
        super(properties.stacksTo(1), Rarity.MYTHIQUE, "Outil d'administration", "Pose les portails vers le hub, le spawn, les mines, les plaines et les donjons.");
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (ctx.getPlayer() instanceof ServerPlayer sp) {
            if (sp.isShiftKeyDown()) PortalTool.open(sp); else PortalTool.use(sp, ctx.getClickedPos());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp) PortalTool.open(sp);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, out, flag);
        out.accept(Component.literal("Outil d'administration (opérateurs)").withColor(0xFF8060));
        out.accept(Component.literal("Clic droit dans le vide : choisir la destination").withColor(0x80C0FF));
        out.accept(Component.literal("Clic droit sur un bloc : il devient un portail").withColor(0x80C0FF));
        out.accept(Component.literal("Accroupi + clic sur un bloc : rouvrir le choix").withColor(0xA0A0A0));
    }

    @Override
    public boolean isFoil(ItemStack stack) { return true; }
}
