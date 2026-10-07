package com.mmorpg.item;

import com.mmorpg.mount.MountType;
import com.mmorpg.rpg.Rarity;
import com.mmorpg.server.MountManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

import java.util.function.Consumer;

/** Sceau de monture (uniquement dans les caisses et Lucky Blocks) : l'utiliser ajoute la monture a la collection. */
public class MountSealItem extends RpgMaterialItem {
    private final MountType mount;

    public MountSealItem(Properties properties, MountType mount, Rarity rarity) {
        super(properties.stacksTo(16), rarity, "Sceau de monture", "Débloque définitivement : " + mount.label + ".");
        this.mount = mount;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp && MountManager.grant(sp, mount) && !sp.hasInfiniteMaterials()) {
            player.getItemInHand(hand).shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, out, flag);
        out.accept(Component.literal("Niveau " + mount.level + " requis pour la monter").withColor(0xE8C060));
        out.accept(Component.literal("Clic droit pour l'ajouter à vos montures").withColor(0x80C0FF));
    }
}
