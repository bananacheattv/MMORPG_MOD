package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.block.AltarBlock;
import com.mmorpg.block.ForgeBlock;
import com.mmorpg.block.TeleporterBlock;
import com.mmorpg.entity.NpcEntity;
import com.mmorpg.entity.boss.BossManager;
import com.mmorpg.item.SummonKeyItem;
import com.mmorpg.network.Net;
import com.mmorpg.network.S2COpenScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.block.BreakBlockEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Le clic gauche ouvre les menus des PNJ, de la Forge, du Teleporteur et de l'Autel (comme le clic droit).
 * Pour casser ces blocs : clic gauche avec une pioche en main, ou Maj + clic gauche avec n'importe quel objet
 * (teleporteur : operateur ou joueur solo).
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class ClickInteractions {
    private static final Map<UUID, Long> LAST_OPEN = new HashMap<>();

    private ClickInteractions() {
    }

    /** Evite de rouvrir le menu a chaque tick quand le bouton reste enfonce. */
    private static boolean throttled(ServerPlayer p) {
        long now = p.level().getGameTime();
        Long last = LAST_OPEN.get(p.getUUID());
        if (last != null && now - last < 10) return true;
        LAST_OPEN.put(p.getUUID(), now);
        return false;
    }

    private static boolean isMenuBlock(Block b) {
        return b instanceof ForgeBlock || b instanceof TeleporterBlock || b instanceof AltarBlock;
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onAttackEntity(AttackEntityEvent event) {
        if (!(event.getTarget() instanceof NpcEntity npc)) return;
        event.setCanceled(true);
        if (event.getEntity() instanceof ServerPlayer sp && !throttled(sp)) {
            switch (npc.role()) {
                case QUETES -> QuestManager.openGiver(sp, npc);
                case MARCHAND -> ShopManager.open(sp, npc);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock event) {
        Player player = event.getEntity();
        BlockPos pos = event.getPos();
        net.minecraft.world.level.block.state.BlockState clicked = event.getLevel().getBlockState(pos);
        if (clicked.getBlock() instanceof com.mmorpg.block.structure.StructurePartBlock) {
            pos = com.mmorpg.block.structure.StructurePartBlock.masterPos(pos, clicked);      // partie d'une structure multibloc
        }
        net.minecraft.world.level.block.state.BlockState masterState = event.getLevel().getBlockState(pos);
        Block block = masterState.getBlock();
        // pioche en main (outil efficace sur le bloc) ou Maj enfoncee : on mine normalement au lieu d'ouvrir le menu
        if (!isMenuBlock(block) || player.isShiftKeyDown() || player.getMainHandItem().getDestroySpeed(masterState) > 1.0F) return;
        event.setCanceled(true);
        if (event.getAction() != PlayerInteractEvent.LeftClickBlock.Action.START) return;
        if (!(player instanceof ServerPlayer sp) || !(event.getLevel() instanceof ServerLevel level) || throttled(sp)) return;
        if (block instanceof ForgeBlock) {
            level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.4f, 1.3f);
            Net.toPlayer(sp, S2COpenScreen.forge(pos));
        } else if (block instanceof TeleporterBlock) {
            sp.sendOverlayMessage(TeleporterBlock.passageMessage());      // il s'utilise en passant sous l'arche
        } else {
            ItemStack stack = sp.getMainHandItem();
            if (stack.getItem() instanceof SummonKeyItem key) {
                if (BossManager.summon(level, pos, key.bossKey(), sp) && !sp.hasInfiniteMaterials()) stack.shrink(1);
            } else {
                sp.sendOverlayMessage(Component.literal("Déposez un objet d'invocation sur l'autel pour réveiller un boss.").withColor(0xE0A0FF));
            }
        }
    }

    /** Les teleporteurs (villes, donjons, points de passage) ne peuvent etre casses que par un operateur. */
    @SubscribeEvent
    public static void onBreak(BreakBlockEvent event) {
        boolean teleporterPart = event.getState().getBlock() instanceof com.mmorpg.block.structure.StructurePartBlock
                && event.getState().getValue(com.mmorpg.block.structure.StructurePartBlock.KIND) == com.mmorpg.block.structure.MultiblockKind.TELEPORTEUR;
        if (!(event.getState().getBlock() instanceof TeleporterBlock) && !teleporterPart) return;
        if (!TeleporterBlock.isAdmin(event.getPlayer())) {
            event.setCanceled(true);
            if (event.getPlayer() instanceof ServerPlayer sp) sp.sendOverlayMessage(TeleporterBlock.adminOnlyMessage());
        }
    }

    public static void forget(ServerPlayer p) {
        LAST_OPEN.remove(p.getUUID());
    }
}
