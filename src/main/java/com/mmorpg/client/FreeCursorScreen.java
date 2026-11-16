package com.mmorpg.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Écran invisible ouvert tant que la touche « curseur libre » est tenue : caméra figée, clics sur les dialogues. */
public final class FreeCursorScreen extends Screen {
    /** Tests : ignore l'état réel de la touche. */
    public static boolean force;
    public FreeCursorScreen() { super(Component.empty()); MascotDialogClient.setFree(true); }

    @Override public boolean isPauseScreen() { return false; }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float a) { }
    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float a) {
        var tip = com.mmorpg.client.hud.MmoHud.pendingTip;
        if (tip != null) g.setComponentTooltipForNextFrame(this.font, tip, com.mmorpg.client.hud.MmoHud.tipX, com.mmorpg.client.hud.MmoHud.tipY);
    }

    @Override
    public void tick() {
        var mc = Minecraft.getInstance();
        if (!force && !InputConstants.isKeyDown(Keys.FREE_CURSOR.getKey().getValue())) onClose();
    }
    @Override
    public boolean keyReleased(KeyEvent event) {
        if (Keys.FREE_CURSOR.matches(event)) { onClose(); return true; }
        return super.keyReleased(event);
    }
    @Override
    public boolean keyPressed(KeyEvent event) {
        switch (event.key()) {
            case InputConstants.KEY_RIGHT, InputConstants.KEY_DOWN, InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER, InputConstants.KEY_PAGEDOWN -> { MascotDialogClient.turn(1); return true; }
            case InputConstants.KEY_LEFT, InputConstants.KEY_UP, InputConstants.KEY_PAGEUP -> { MascotDialogClient.turn(-1); return true; }
            default -> { }
        }
        return super.keyPressed(event);
    }
    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == InputConstants.MOUSE_BUTTON_LEFT && MascotDialogClient.click(event.x(), event.y())) return true;
        return super.mouseClicked(event, doubleClick);
    }
    @Override
    public void removed() { MascotDialogClient.setFree(false); }
}
