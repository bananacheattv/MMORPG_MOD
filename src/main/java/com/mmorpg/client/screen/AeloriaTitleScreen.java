package com.mmorpg.client.screen;

import com.mmorpg.client.AeloriaFont;
import com.mmorpg.client.ClientConfig;
import com.mmorpg.client.cinematic.Ease;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CommonButtons;
import net.minecraft.client.gui.components.LogoRenderer;
import net.minecraft.client.gui.components.PlainTextButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.CreditsAndAttributionScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen;
import net.minecraft.client.gui.screens.multiplayer.SafetyScreen;
import net.minecraft.client.gui.screens.options.AccessibilityOptionsScreen;
import net.minecraft.client.gui.screens.options.LanguageSelectScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.gui.screens.worldselection.SelectWorldScreen;
import net.minecraft.client.gui.components.SpriteIconButton;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/**
 * Menu principal d'Aeloria : remplace celui de Minecraft (voir {@link com.mmorpg.client.TitleClient}). Panorama assombri, braises
 * dorees qui montent, logo, accroche, boutons Jouer / Multijoueur (et « Rejoindre Aeloria » si une adresse est configuree), options,
 * mods, langue, accessibilite et quitter.
 */
public class AeloriaTitleScreen extends Screen {
    private static final Component CREDITS = Component.translatable("title.credits");
    private static final String[] TIPS = {
            "Choisissez votre classe dans la Citadelle : Guerrier, Mage, Archer ou Paladin.",
            "Les donjons sont instanciés : le vôtre, rien que pour vous et votre groupe.",
            "La touche M ouvre votre personnage, vos quêtes et votre groupe.",
            "Cinq boss vous attendent, du Roi Gobelin à l'Avatar du Néant.",
            "On ne perd jamais son équipement en mourant, seulement un peu d'expérience.",
    };
    private final LogoRenderer logo = new LogoRenderer(false);
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
        this.addRenderableWidget(Button.builder(Component.literal("JOUER"), b -> this.minecraft.gui.setScreen(new SelectWorldScreen(this)))
                .bounds(cx - bw / 2, y, bw, 24).build());
        y += 28;
        boolean multi = this.minecraft.allowsMultiplayer();
        Button online = this.addRenderableWidget(Button.builder(Component.literal("MULTIJOUEUR"), b -> this.minecraft.gui.setScreen(
                        this.minecraft.options.skipMultiplayerWarning ? new JoinMultiplayerScreen(this) : new SafetyScreen(this)))
                .bounds(cx - bw / 2, y, bw, 24).build());
        online.active = multi;
        if (!multi) online.setTooltip(Tooltip.create(Component.translatable("title.multiplayer.disabled")));
        y += 28;
        String server = ClientConfig.MAIN_MENU_SERVER.get().trim();
        if (!server.isEmpty() && multi) {
            this.addRenderableWidget(Button.builder(Component.literal("REJOINDRE AELORIA"), b -> join(server)).bounds(cx - bw / 2, y, bw, 24).build());
            y += 28;
        }
        this.addRenderableWidget(Button.builder(Component.literal("OPTIONS"), b -> this.minecraft.gui.setScreen(new OptionsScreen(this, this.minecraft.options)))
                .bounds(cx - bw / 2, y, bw / 2 - 2, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("QUITTER"), b -> this.minecraft.stop()).bounds(cx + 2, y, bw / 2 - 2, 20).build());
        y += 24;
        // rangee d'icones : mods, langue, accessibilite
        int ix = cx - (3 * 20 + 2 * 4) / 2;
        this.addRenderableWidget(net.neoforged.neoforge.client.gui.widget.ModsButton.create(this)).setPosition(ix, y);
        SpriteIconButton lang = this.addRenderableWidget(CommonButtons.language(20,
                b -> this.minecraft.gui.setScreen(new LanguageSelectScreen(this, this.minecraft.options, this.minecraft.getLanguageManager())), true));
        lang.setPosition(ix + 24, y);
        SpriteIconButton access = this.addRenderableWidget(CommonButtons.accessibility(20,
                b -> this.minecraft.gui.setScreen(new AccessibilityOptionsScreen(this, this.minecraft.options)), true));
        access.setPosition(ix + 48, y);
        int cw = this.font.width(CREDITS);
        this.addRenderableWidget(new PlainTextButton(this.width - cw - 4, this.height - 12, cw, 10, CREDITS,
                b -> this.minecraft.gui.setScreen(new CreditsAndAttributionScreen(this)), this.font));
    }

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
        g.pose().pushMatrix();
        g.pose().translate(this.width / 2F, 0F);
        g.pose().scale(1.45F, 1.45F);
        g.pose().translate(-this.width / 2F, 0F);
        this.logo.extractRenderState(g, this.width, fade);
        g.pose().popMatrix();
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
        // astuce qui change toutes les 8 s
        String tip = TIPS[(int) (t / 8F) % TIPS.length];
        float tf = Mth.clamp(Math.min((t % 8F) / 0.5F, (8F - (t % 8F)) / 0.5F), 0F, 1F);
        int pw = font.width(tip);
        g.text(font, tip, cx - pw / 2, height - 30, argb(0.85F * tf * fade, 0xB8A98A), true);
        // pied de page
        g.text(font, "Aeloria MMORPG", 4, height - 12, argb(0.7F * fade, 0x9C8A6A), true);
        String mc = "Minecraft " + net.minecraft.SharedConstants.getCurrentVersion().name() + " · NeoForge";
        g.text(font, mc, 4, height - 22, argb(0.45F * fade, 0x9C8A6A), true);
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
