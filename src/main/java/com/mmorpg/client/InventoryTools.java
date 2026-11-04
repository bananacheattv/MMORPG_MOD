package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.client.screen.CharacterScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.mixin.ContainerScreenAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.util.Locale;

/** Recherche (surbrillance) et tri automatique de l'inventaire, des sacs et de la banque. */
@EventBusSubscriber(modid = MMORPG.MODID, value = Dist.CLIENT)
public final class InventoryTools {
    private static EditBox box;
    private static int bx, by, bw = 74;                  // champ de recherche
    private static int[] sortInv, sortBox;                // boutons {x, y, w, h}
    private static boolean container;

    private InventoryTools() {
    }

    public static void testQuery(String q) {
        if (box != null) box.setValue(q);
    }

    private static boolean applies(Screen s) {
        return s instanceof CharacterScreen || s instanceof ContainerScreen;
    }

    @SubscribeEvent
    public static void init(ScreenEvent.Init.Post event) {
        Screen s = event.getScreen();
        box = null;
        if (!applies(s)) return;
        Minecraft mc = Minecraft.getInstance();
        box = new EditBox(mc.font, 0, 0, bw, 12, Component.literal("Recherche"));
        box.setHint(Component.literal("Rechercher…"));
        box.setMaxLength(24);
        box.setFocused(false);
        container = s instanceof ContainerScreen;
    }

    private static void layout(Screen s) {
        var acc = (ContainerScreenAccessor) s;
        if (s instanceof CharacterScreen cs) {
            int[] f = cs.realFrame();
            int right = f[0] + f[2] - 118;
            sortInv = new int[]{right - 52, f[1] + f[3] - 21, 52, 14};
            bx = sortInv[0] - 4 - bw;
            by = f[1] + f[3] - 20;
            sortBox = null;
        } else {
            int x = acc.aeloria$left(), y = acc.aeloria$top();
            bx = x + 176 - bw - 70 - 4;
            by = y + 3;
            sortBox = new int[]{bx + bw + 3, by - 1, 33, 14};
            sortInv = new int[]{bx + bw + 3 + 35, by - 1, 33, 14};
        }
        box.setX(bx);
        box.setY(by);
    }

    @SubscribeEvent
    public static void render(ScreenEvent.Render.Post event) {
        Screen s = event.getScreen();
        if (box == null || !applies(s)) return;
        layout(s);
        var g = event.getGuiGraphics();
        var acc = (ContainerScreenAccessor) s;
        String q = box.getValue().trim().toLowerCase(Locale.ROOT);
        if (!q.isEmpty()) {
            for (Slot slot : ((AbstractContainerScreen<?>) s).getMenu().slots) {
                if (slot.getItem().isEmpty() || slot.x < -1000) continue;
                String name = slot.getItem().getHoverName().getString().toLowerCase(Locale.ROOT);
                if (name.contains(q)) continue;
                int x, y, size;
                if (s instanceof CharacterScreen cs) {
                    int[] b = cs.slotBox(slot);
                    if (b == null) continue;
                    x = b[0];
                    y = b[1];
                    size = b[2];
                } else {
                    x = acc.aeloria$left() + slot.x - 1;
                    y = acc.aeloria$top() + slot.y - 1;
                    size = 18;
                }
                g.fill(x, y, x + size, y + size, 0xB8100A18);
            }
        }
        box.extractRenderState(g, event.getMouseX(), event.getMouseY(), event.getPartialTick());
        if (sortBox != null) button(g, sortBox, "Coffre", event.getMouseX(), event.getMouseY());
        button(g, sortInv, container ? "Sac" : "Trier", event.getMouseX(), event.getMouseY());
    }

    private static void button(net.minecraft.client.gui.GuiGraphicsExtractor g, int[] r, String label, int mx, int my) {
        boolean hov = Ui.in(mx, my, r[0], r[1], r[2], r[3]);
        g.fill(r[0], r[1], r[0] + r[2], r[1] + r[3], hov ? Ui.CARD_HOVER : 0xE0221738);
        Ui.border(g, r[0], r[1], r[2], r[3], hov ? Ui.GOLD : Ui.GOLD_DARK);
        g.centeredText(Ui.font(), label, r[0] + r[2] / 2, r[1] + 3, hov ? Ui.GOLD_LIGHT : Ui.TEXT);
    }

    @SubscribeEvent
    public static void click(ScreenEvent.MouseButtonPressed.Pre event) {
        Screen s = event.getScreen();
        if (box == null || !applies(s)) return;
        layout(s);
        double x = event.getMouseX(), y = event.getMouseY();
        if (Ui.in(x, y, bx, by, bw, 12)) {
            box.setFocused(true);
            box.mouseClicked(event.getMouseButtonEvent(), event.isDoubleClick());
            event.setCanceled(true);
            return;
        }
        box.setFocused(false);
        if (sortInv != null && Ui.in(x, y, sortInv[0], sortInv[1], sortInv[2], sortInv[3])) {
            QolClient.action("sort_inv");
            event.setCanceled(true);
        } else if (sortBox != null && Ui.in(x, y, sortBox[0], sortBox[1], sortBox[2], sortBox[3])) {
            QolClient.action("sort_box");
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void key(ScreenEvent.KeyPressed.Pre event) {
        if (box == null || !box.isFocused() || !applies(event.getScreen())) return;
        if (event.getKey() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) {
            box.setFocused(false);
        } else {
            box.keyPressed(event.getKeyEvent());
        }
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void typed(ScreenEvent.CharacterTyped.Pre event) {
        if (box == null || !box.isFocused() || !applies(event.getScreen())) return;
        box.charTyped(event.getCharacterEvent());
        event.setCanceled(true);
    }
}
