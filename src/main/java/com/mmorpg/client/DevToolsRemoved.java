package com.mmorpg.client;

import com.mmorpg.client.screen.*;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;

/** Verification en jeu : commandes retirees, outil portail, boutons Boutique PvP / Annuler. */
public final class DevToolsRemoved {
    private static int tick;
    private static void shot(Minecraft mc, String name) { net.minecraft.client.Screenshot.grab(mc.gameDirectory, "showcase_" + name + ".png", mc.gameRenderer.mainRenderTarget(), 1, m -> com.mmorpg.MMORPG.LOGGER.info("[MMORPG] Capture {}", name)); }
    private static void cmd(Minecraft mc, String s) { mc.player.connection.sendCommand(s); }
    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) return;
        tick++;
        if (tick == 60) { mc.options.guiScale().set(2); mc.getWindow().setWindowed(1280, 720); mc.resizeGui(); cmd(mc, "give @s mmorpg:outil_portail"); cmd(mc, "outils"); cmd(mc, "pvp"); cmd(mc, "forge relancer"); }
        if (tick == 90) shot(mc, "commandes_retirees");
        if (tick == 100) { var d = new CompoundTag(); mc.gui.setScreen(new PortalToolScreen(d)); }
        if (tick == 120) shot(mc, "outil_portail_ecran");
        if (tick == 125) {
            var t = new CompoundTag(); t.putString("kind", "donjon"); t.putInt("region", -1); t.putString("dungeon", "catacombes"); t.putInt("difficulty", 1);
            ClientNet.send(new com.mmorpg.network.Payloads.SocialAction("portaltool", t)); mc.gui.setScreen(null);
        }
        if (tick == 150) {
            int slot = -1; for (int i = 0; i < 9; i++) if (mc.player.getInventory().getItem(i).getItem() instanceof com.mmorpg.item.PortalToolItem) slot = i;
            if (slot >= 0) mc.player.getInventory().setSelectedSlot(slot);
            var below = mc.player.blockPosition().below();
            var hit = new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(below), net.minecraft.core.Direction.UP, below, false);
            mc.gameMode.useItemOn(mc.player, net.minecraft.world.InteractionHand.MAIN_HAND, hit);
            cmd(mc, "portail liste");
        }
        if (tick == 180) shot(mc, "outil_portail_pose");
        if (tick == 190) mc.gui.setScreen(new ArenaScreen(new CompoundTag()));
        if (tick == 210) shot(mc, "arene_boutique_pvp");
        if (tick == 220) { var t = new CompoundTag(); t.putString("action", "pvp"); ClientNet.send(new com.mmorpg.network.Payloads.SocialAction("arena", t)); }
        if (tick == 260) shot(mc, "boutique_pvp_ouverte");
    }
}
