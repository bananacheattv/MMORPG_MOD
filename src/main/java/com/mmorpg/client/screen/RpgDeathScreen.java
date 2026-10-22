package com.mmorpg.client.screen;

import com.mmorpg.client.EldoriaFont;
import com.mmorpg.client.cinematic.Ease;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.network.Payloads;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.util.Mth;

/**
 * Ecran de mort : fond rouge sombre, titre, cause du deces, experience perdue (compteur et barre de niveau) et rappel que
 * l'equipement est conserve. Le bouton « Renaitre » n'est actif qu'apres une courte pause, le temps de lire.
 */
public class RpgDeathScreen extends MmoScreen {
    private static final float DELAY = 1.6F;
    private final Payloads.DeathInfo info;
    private long openedAt;

    public RpgDeathScreen(Payloads.DeathInfo info) {
        super("Vous êtes mort");
        this.info = info;
    }

    @Override
    protected void init() {
        super.init();
        openedAt = System.nanoTime();
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(com.mmorpg.registry.ModSounds.DEATH.get(), 1.0F));
        }
    }

    private float seconds() {
        return (System.nanoTime() - openedAt) / 1.0E9F;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        float t = seconds();
        float k = Ease.OUT_CUBIC.apply(t / 1.2F);
        g.fill(0, 0, width, height, Ui.alpha(0xFF120406, 0.94F * k));
        // vignette rouge : bords plus sombres, lueur sourde au centre
        g.fillGradient(0, 0, width, height / 3, Ui.alpha(0xFF000000, 0.7F * k), Ui.alpha(0xFF000000, 0F));
        g.fillGradient(0, height * 2 / 3, width, height, Ui.alpha(0xFF000000, 0F), Ui.alpha(0xFF2A0508, 0.75F * k));
        // bandes de cinema
        int bar = Math.round(height * 0.09F * Ease.OUT_CUBIC.apply(t / 0.9F));
        g.fill(0, 0, width, bar, 0xFF000000);
        g.fill(0, height - bar, width, height, 0xFF000000);
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        float t = seconds();
        int cx = width / 2;
        float in = Ease.OUT_CUBIC.apply((t - 0.2F) / 0.9F);
        // titre
        String title = "VOUS ÊTES TOMBÉ";
        var comp = EldoriaFont.of(title);
        int tw = font.width(comp);
        float scale = Math.min(3.2F, width * 0.8F / Math.max(1, tw));
        g.pose().pushMatrix();
        g.pose().translate(cx, height * 0.2F + (1F - in) * -10F);
        g.pose().scale(scale, scale);
        g.text(font, comp, -tw / 2, -4, Ui.withAlpha(0xFF4040, Math.round(255 * in)), true);
        g.pose().popMatrix();
        int half = Math.round(Math.min(width * 0.3F, 170F) * Ease.OUT_CUBIC.apply((t - 0.5F) / 0.8F));
        int ly = Math.round(height * 0.2F) + Math.round(scale * 8) + 4;
        g.fill(cx - half, ly, cx + half, ly + 1, Ui.withAlpha(0xC03030, Math.round(220 * in)));
        Ui.diamond(g, cx - half, ly, 3, Ui.withAlpha(0xC03030, Math.round(255 * in)));
        Ui.diamond(g, cx + half, ly, 3, Ui.withAlpha(0xC03030, Math.round(255 * in)));
        if (!info.cause().isEmpty()) {
            String cause = font.plainSubstrByWidth(info.cause(), width - 40);
            g.text(font, cause, cx - font.width(cause) / 2, ly + 9, Ui.withAlpha(0xC8B8A8, Math.round(255 * Ease.OUT_CUBIC.apply((t - 0.7F) / 0.8F))), true);
        }
        // experience perdue
        float xin = Ease.OUT_CUBIC.apply((t - 1.0F) / 0.9F);
        int py = Math.round(height * 0.42F);
        int pw2 = Math.min(width - 40, 260);
        int px = cx - pw2 / 2;
        if (info.xpLost() > 0) {
            float count = Mth.clamp((t - 1.3F) / 1.1F, 0F, 1F);
            long shown = Math.round(info.xpLost() * Ease.OUT_CUBIC.apply(count));
            String label = "EXPÉRIENCE PERDUE";
            g.text(font, label, cx - font.width(label) / 2, py, Ui.withAlpha(0xA89888, Math.round(255 * xin)), true);
            String big = "- " + grp(shown) + " XP";
            int bw = font.width(big);
            g.pose().pushMatrix();
            g.pose().translate(cx, py + 12);
            g.pose().scale(2.4F, 2.4F);
            g.text(font, big, -bw / 2, 0, Ui.withAlpha(0xFF5A5A, Math.round(255 * xin)), true);
            g.pose().popMatrix();
            // barre du niveau : part restante en or, part perdue en rouge
            if (info.xpNext() > 0) {
                int by = py + 38;
                g.fill(px - 1, by - 1, px + pw2 + 1, by + 9, Ui.withAlpha(0x000000, Math.round(200 * xin)));
                g.fill(px, by, px + pw2, by + 8, Ui.withAlpha(0x1A1620, Math.round(255 * xin)));
                float now = Mth.clamp(info.xpNow() / (float) info.xpNext(), 0F, 1F);
                float before = Mth.clamp((info.xpNow() + info.xpLost()) / (float) info.xpNext(), 0F, 1F);
                int xNow = px + Math.round(pw2 * now);
                int xBefore = px + Math.round(pw2 * before);
                float pulse = 0.55F + 0.45F * Mth.sin(t * 5F);
                g.fill(xNow, by, Math.max(xNow, xBefore), by + 8, Ui.withAlpha(0xFF3030, Math.round(255 * xin * pulse)));
                g.fill(px, by, xNow, by + 8, Ui.withAlpha(0xE8C060, Math.round(255 * xin)));
                g.fillGradient(px, by, xNow, by + 4, Ui.withAlpha(0xFFFFFF, Math.round(70 * xin)), Ui.withAlpha(0xFFFFFF, 0));
                String lv = "Niveau " + info.level() + "  ·  " + grp(info.xpNow()) + " / " + grp(info.xpNext()) + " XP";
                g.text(font, lv, cx - font.width(lv) / 2, by + 13, Ui.withAlpha(0xB8AC98, Math.round(255 * xin)), true);
            }
        } else {
            String none = "Aucune expérience perdue";
            g.text(font, none, cx - font.width(none) / 2, py + 6, Ui.withAlpha(0xB8AC98, Math.round(255 * xin)), true);
        }
        // equipement conserve
        float sin = Ease.OUT_CUBIC.apply((t - 2.0F) / 0.8F);
        String safe = "Votre équipement est intact : vous ne perdez jamais vos objets à la mort.";
        int sw = font.width(safe);
        float ss = Math.min(1F, (width - 24F) / Math.max(1, sw));
        g.pose().pushMatrix();
        g.pose().translate(cx, height * 0.74F);
        g.pose().scale(ss, ss);
        g.text(font, safe, -sw / 2, 0, Ui.withAlpha(0x7AE08A, Math.round(255 * sin)), true);
        g.pose().popMatrix();
        // renaitre
        int bw2 = Math.min(width - 40, 180);
        boolean ready = t >= DELAY;
        String label = ready ? "RENAÎTRE" : "RENAÎTRE  (" + Math.max(1, (int) Math.ceil(DELAY - t)) + ")";
        float bin = Ease.OUT_CUBIC.apply((t - 1.8F) / 0.6F);
        if (bin > 0.02F) {
            button(g, cx - bw2 / 2, Math.round(height * 0.8F), bw2, 22, label, ready, 0xFF8A2A2A, () -> {
                if (minecraft != null && minecraft.player != null) minecraft.player.respawn();
                if (minecraft != null) minecraft.gui.setScreen(null);
            });
        }
    }

    /** Nombre avec espaces entre les milliers (la police du jeu n'a pas l'espace insecable des formats francais). */
    private static String grp(long n) {
        String s = Long.toString(Math.abs(n));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            if (i > 0 && (s.length() - i) % 3 == 0) sb.append(' ');
            sb.append(s.charAt(i));
        }
        return (n < 0 ? "-" : "") + sb;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
