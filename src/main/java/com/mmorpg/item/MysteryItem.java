package com.mmorpg.item;

import com.mmorpg.config.LootTables;
import com.mmorpg.mount.MountType;
import com.mmorpg.pet.PetType;
import com.mmorpg.rpg.Rarity;
import com.mmorpg.server.AdventureLoot;
import com.mmorpg.server.MountManager;
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

/**
 * Oeuf mystere (familier au hasard) et Sceau mystere (monture au hasard) : la probabilite de chaque familier / monture
 * se regle dans les tables « oeuf_mystere » et « sceau_mystere » (/mmorpg butins).
 */
public class MysteryItem extends RpgMaterialItem {
    public enum Kind { PET, MOUNT }

    private final Kind kind;

    public MysteryItem(Properties properties, Kind kind) {
        super(properties.stacksTo(16), kind == Kind.PET ? Rarity.EPIQUE : Rarity.LEGENDAIRE,
                kind == Kind.PET ? "Œuf mystère" : "Sceau mystère",
                kind == Kind.PET ? "Éclot en un familier aléatoire." : "Révèle une monture aléatoire.");
        this.kind = kind;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (player instanceof ServerPlayer sp && (kind == Kind.PET ? hatch(sp) : reveal(sp)) && !sp.hasInfiniteMaterials()) {
            player.getItemInHand(hand).shrink(1);
        }
        return InteractionResult.SUCCESS;
    }

    private static String id(String item) {
        return item.startsWith("mmorpg:") ? item.substring(7) : item;
    }

    private static boolean hatch(ServerPlayer p) {
        String id = id(AdventureLoot.roll(p, LootTables.OEUF).item());
        PetType pet = PetType.byId(id.replaceFirst("^oeuf_", ""));
        if (pet == null) pet = PetType.values()[0];
        RpgPlayers.unlockPet(p, pet);
        return true;
    }

    private static boolean reveal(ServerPlayer p) {
        var owned = RpgPlayers.get(p).mounts;
        // jusqu'a 12 tirages pour tomber sur une monture pas encore possedee
        for (int i = 0; i < 12; i++) {
            MountType m = MountType.byId(id(AdventureLoot.roll(p, LootTables.SCEAU).item()).replaceFirst("^sceau_", ""));
            if (m != null && !owned.contains(m.id)) return MountManager.grant(p, m);
        }
        if (owned.size() >= MountType.values().length) {
            RpgPlayers.addGold(p, 2000, true);
            p.sendSystemMessage(Component.literal("Vous possédez déjà toutes les montures : +2000 pièces d’or.").withColor(0xE8C060));
            return true;
        }
        for (MountType m : MountType.values()) if (!owned.contains(m.id)) return MountManager.grant(p, m);
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, out, flag);
        out.accept(Component.literal(kind == Kind.PET ? "Clic droit pour faire éclore" : "Clic droit pour révéler la monture").withColor(0x80C0FF));
    }
}
