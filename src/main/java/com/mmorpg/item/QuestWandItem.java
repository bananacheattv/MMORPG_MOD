package com.mmorpg.item;

import com.mmorpg.MMORPG;
import com.mmorpg.entity.NpcEntity;
import com.mmorpg.rpg.Rarity;
import com.mmorpg.server.QuestEditor;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import java.util.function.Consumer;

/** Sceptre d'administration : ouvre l'atelier de quetes (clic dans le vide) ou la fiche d'un PNJ (clic sur lui). */
@EventBusSubscriber(modid = MMORPG.MODID)
public class QuestWandItem extends RpgMaterialItem {
    public QuestWandItem(Properties properties) {
        super(properties.stacksTo(1), Rarity.MYTHIQUE, "Outil d'administration", "Forge et corrige les quêtes d'Aeloria.");
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) open(sp, null);
        return InteractionResult.SUCCESS;
    }

    private static void open(ServerPlayer sp, NpcEntity npc) {
        if (!QuestEditor.allowed(sp)) {
            sp.sendOverlayMessage(Component.literal("Le sceptre des quêtes est réservé aux opérateurs.").withColor(0xFF8060));
            return;
        }
        QuestEditor.open(sp, npc == null ? "" : npc.getUUID().toString());
    }

    /** Clic droit ou gauche sur un PNJ avec le sceptre : on edite le PNJ au lieu d'ouvrir son dialogue. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onUseOnNpc(PlayerInteractEvent.EntityInteract event) {
        if (!(event.getTarget() instanceof NpcEntity npc) || !(event.getItemStack().getItem() instanceof QuestWandItem)) return;
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() == InteractionHand.MAIN_HAND && event.getEntity() instanceof ServerPlayer sp) open(sp, npc);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHitNpc(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof NpcEntity npc) || !(event.getEntity().getMainHandItem().getItem() instanceof QuestWandItem)) return;
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer sp) open(sp, npc);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
        super.appendHoverText(stack, context, display, out, flag);
        out.accept(Component.literal("Outil d'administration (opérateurs)").withColor(0xFF8060));
        out.accept(Component.literal("Clic droit : ouvrir l'atelier de quêtes").withColor(0x80C0FF));
        out.accept(Component.literal("Clic sur un PNJ : modifier ce PNJ").withColor(0xA0A0A0));
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
