package com.mmorpg.client.screen;

import com.mmorpg.client.AeloriaFont;
import com.mmorpg.client.ClientConfig;
import com.mmorpg.client.cinematic.Ease;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Menu principal d'Aeloria : remplace celui de Minecraft (voir {@link com.mmorpg.client.TitleClient}). Panorama assombri, braises
 * dorees qui montent, logo, accroche et trois boutons : connexion au serveur, options et quitter.
 */
public class AeloriaTitleScreen extends Screen {
    private long openedAt;

    public AeloriaTitleScreen() {
        super(Component.translatable("narrator.screen.title"));
        if (this.minecraft != null) this.minecraft.gameRenderer.panorama().startSpin();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    protected void init() {
        openedAt = System.nanoTime();
        int cx = this.width / 2;
        int bw = 220;
        int y = Math.max(158, this.height / 4 + 70);
        boolean multi = this.minecraft.allowsMultiplayer();
        Button online = this.addRenderableWidget(Button.builder(Component.literal("JOUER"), b -> join(serverAddress()))
                .bounds(cx - bw / 2, y, bw, 24).build());
        online.active = multi;
        if (!multi) online.setTooltip(Tooltip.create(Component.translatable("title.multiplayer.disabled")));
        y += 28;
        this.addRenderableWidget(Button.builder(Component.literal("OPTIONS"), b -> this.minecraft.gui.setScreen(new OptionsScreen(this, this.minecraft.options)))
                .bounds(cx - bw / 2, y, bw / 2 - 2, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("QUITTER"), b -> this.minecraft.stop()).bounds(cx + 2, y, bw / 2 - 2, 20).build());
    }

    public static String serverAddress(){String server=System.getProperty("aeloria.server","").trim();if(server.isEmpty())server=ClientConfig.MAIN_MENU_SERVER.get().trim();return server.isEmpty()?"192.168.0.249:25565":server;}

    /** Connexion directe au serveur configure (config client : mainMenuServer). */
    private void join(String address) {
        net.minecraft.client.multiplayer.resolver.ServerAddress parsed = net.minecraft.client.multiplayer.resolver.ServerAddress.parseString(address);
        net.minecraft.client.gui.screens.ConnectScreen.startConnecting(this, this.minecraft, parsed,
                new net.minecraft.client.multiplayer.ServerData("Aeloria", address, net.minecraft.client.multiplayer.ServerData.Type.OTHER), false, null);
    }

    private float seconds() {
        return (System.nanoTime() - openedAt) / 1.0E9F;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float a) {
        float t = seconds();
        float fade = Ease.OUT_CUBIC.apply(t / 1.4F);
        this.extractPanorama(g, a);
        // voile violet et vignette
        g.fillGradient(0, 0, width, height, argb(0.28F * fade, 0x120A20), argb(0.62F * fade, 0x050210));
        g.fillGradient(0, 0, width, height / 4, argb(0.55F * fade, 0x000000), argb(0F, 0x000000));
        embers(g, t, fade);
        super.extractRenderState(g, mx, my, a);
        com.mmorpg.client.ui.Logo.full(g, this.width / 2, 8, 256);
        int cx = width / 2;
        // accroche sous le logo et filet d'or
        String tagline = "CITADELLE DES HÉROS";
        var comp = AeloriaFont.of(tagline);
        int tw = font.width(comp);
        int ty = 126;
        g.text(font, comp, cx - tw / 2, ty, argb(fade, 0xE2B85A), true);
        int half = Math.round(Math.min(width * 0.28F, 150F) * Ease.OUT_CUBIC.apply((t - 0.3F) / 1.0F));
        g.fill(cx - half, ty + 12, cx + half, ty + 13, argb(0.9F * fade, 0x7A5A22));
        for (int sx : new int[]{cx - half, cx + half}) {
            for (int i = -3; i <= 3; i++) g.fill(sx - (3 - Math.abs(i)), ty + 12 + i, sx + (3 - Math.abs(i)) + 1, ty + 13 + i, argb(fade, 0xE2B85A));
        }
    }

    /** Braises dorees qui montent lentement, deterministes (pas d'etat). */
    private void embers(GuiGraphicsExtractor g, float t, float fade) {
        for (int i = 0; i < 46; i++) {
            float seed = i * 12.9898F;
            float speed = 6F + (i % 7) * 3F;
            float y = height - ((t * speed + Mth.frac(seed * 0.37F) * height) % height);
            float x = Mth.frac(seed * 0.618F) * width + Mth.sin(t * 0.7F + i) * 12F;
            float flick = 0.35F + 0.65F * Mth.sin(t * 2.4F + i * 1.7F) * Mth.sin(t * 2.4F + i * 1.7F);
            int sz = 1 + (i % 4 == 0 ? 1 : 0);
            g.fill(Math.round(x), Math.round(y), Math.round(x) + sz, Math.round(y) + sz, argb(0.6F * fade * flick, i % 3 == 0 ? 0xFFE6A0 : 0xE2B85A));
        }
    }

    private static int argb(float alpha, int rgb) {
        return (Mth.clamp((int) (alpha * 255F), 0, 255) << 24) | (rgb & 0xFFFFFF);
    }
}
