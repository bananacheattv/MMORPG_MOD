package com.mmorpg.item;

import com.mmorpg.pet.PetType;
import com.mmorpg.rpg.Stat;
import com.mmorpg.server.RpgPlayers;
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

/** Oeuf de familier : l'utiliser ajoute le familier a votre collection. */
public class PetEggItem extends RpgMaterialItem {
    private final PetType pet;

    public PetEggItem(Properties properties, PetType pet) {
        super(properties.stacksTo(16), pet.rarity, "Œuf de familier", pet.description);
        this.pet = pet;
    }

    public PetType pet() {
        return pet;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp) {
            RpgPlayers.unlockPet(sp, pet);
            if (!sp.hasInfiniteMaterials()) {
                player.getItemInHand(hand).shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, out, flag);
        out.accept(Component.literal("Bonus au niveau 1 :").withColor(0xE8C060));
        ItemTooltips.statLines(pet.bonus, 1.0, out);
        out.accept(Component.literal("Clic droit pour l'adopter").withColor(0x80C0FF));
        out.accept(Component.literal("(Familier déjà possédé : +400 XP de familier)").withColor(0x808080));
    }

    static String statSummary(PetType pet) {
        StringBuilder sb = new StringBuilder();
        for (Stat s : Stat.values()) {
            double v = pet.bonus.get(s);
            if (v != 0) {
                if (!sb.isEmpty()) sb.append(", ");
                sb.append(s.format(v, true)).append(' ').append(s.shortName);
            }
        }
        return sb.toString();
    }
}
