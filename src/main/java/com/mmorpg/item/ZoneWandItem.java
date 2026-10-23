package com.mmorpg.item;

import com.mmorpg.rpg.Rarity;
import com.mmorpg.zone.ZoneManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/** Baguette de zone (operateurs) : deux clics droits sur des blocs delimitent un rectangle, /zone creer le valide. */
public class ZoneWandItem extends RpgMaterialItem {
    public ZoneWandItem(Properties properties) {
        super(properties.stacksTo(1), Rarity.MYTHIQUE, "Outil d'administration", "Délimite les zones d'Aeloria.");
    }

    private static boolean allowed(ServerPlayer p) {
        if (p.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) return true;
        p.sendOverlayMessage(Component.literal("La baguette de zone est réservée aux opérateurs.").withColor(0xFF8060));
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        if (ctx.getLevel().isClientSide()) return InteractionResult.SUCCESS;
        if (!(ctx.getPlayer() instanceof ServerPlayer sp) || !allowed(sp)) return InteractionResult.SUCCESS;
        if (sp.isShiftKeyDown()) {
            ZoneManager.clearSelection(sp);
            sp.sendOverlayMessage(Component.literal("Sélection effacée.").withColor(0xA0A0A0));
        } else {
            ZoneManager.pick(sp, ctx.getClickedPos());
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!level.isClientSide() && player instanceof ServerPlayer sp && allowed(sp) && sp.isShiftKeyDown()) {
            ZoneManager.clearSelection(sp);
            sp.sendOverlayMessage(Component.literal("Sélection effacée.").withColor(0xA0A0A0));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, out, flag);
        out.accept(Component.literal("Outil d'administration (opérateurs)").withColor(0xFF8060));
        out.accept(Component.literal("Clic droit sur un bloc : coin A, puis coin B").withColor(0x80C0FF));
        out.accept(Component.literal("/zone creer <nom> : valide la zone").withColor(0xA0A0A0));
        out.accept(Component.literal("Accroupi + clic : efface la sélection").withColor(0xA0A0A0));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
