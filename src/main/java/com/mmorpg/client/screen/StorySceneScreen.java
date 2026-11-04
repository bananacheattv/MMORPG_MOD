package com.mmorpg.client.screen;

import com.mmorpg.quest.StoryBook;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;

/**
 * Cinematique de l'histoire principale : fondu au noir, barres de cinema, titre de chapitre puis repliques qui apparaissent une a
 * une. Espace / clic : replique suivante ; Echap : passer. Le jeu ne s'arrete pas (multijoueur) : la scene se ferme seule.
 */
public final class StorySceneScreen extends Screen {
    private static final float INTRO = 3.2F, PER_LINE = 4.2F, OUTRO = 2.5F, FADE = 0.9F;
    private final StoryBook.Scene scene;
    private long start = System.nanoTime();
    private float skipped;

    public StorySceneScreen(String id) {
        super(Component.literal("Histoire"));
        this.scene = StoryBook.get(id);
    }

    private float time() {
        return (System.nanoTime() - start) / 1e9F + skipped;
    }

    private float total() {
        return scene == null ? 0 : INTRO + PER_LINE * scene.lines().size() + OUTRO;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }

    /** Passe a la replique suivante (ou termine si c'est la derniere). */
    private void advance() {
        if (scene == null) {
            onClose();
            return;
        }
        float t = time();
        float next = INTRO + PER_LINE * (float) Math.ceil(Math.max(0, t - INTRO) / PER_LINE + 0.001F);
        if (t < INTRO) next = INTRO;
        if (next >= total() - OUTRO) next = total() - OUTRO + 0.01F;
        if (next > t) skipped += next - t;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        int k = event.key();
        if (k == 32 || k == 257 || k == 335) {
            advance();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        advance();
        return true;
    }

    private List<String> wrap(String text, int maxW) {
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String w : text.split(" ")) {
            String test = cur.isEmpty() ? w : cur + " " + w;
            if (font.width(test) > maxW && !cur.isEmpty()) {
                out.add(cur.toString());
                cur = new StringBuilder(w);
            } else cur = new StringBuilder(test);
        }
        if (!cur.isEmpty()) out.add(cur.toString());
        return out;
    }

    private static int withAlpha(int rgb, float a) {
        return (Mth.clamp((int) (a * 255), 0, 255) << 24) | (rgb & 0xFFFFFF);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        if (scene == null) {
            onClose();
            return;
        }
        float t = time(), total = total();
        if (t >= total) {
            onClose();
            return;
        }
        float fadeIn = Mth.clamp(t / FADE, 0, 1), fadeOut = Mth.clamp((total - t) / FADE, 0, 1);
        float dark = Math.min(fadeIn, fadeOut);
        g.fill(0, 0, width, height, withAlpha(0x05030A, 0.93F * dark));
        // lueur d'ambiance
        int accent = scene.color() & 0xFFFFFF;
        g.fillGradient(0, height / 3, width, height * 2 / 3, withAlpha(accent, 0.0F), withAlpha(accent, 0.10F * dark));
        // etoiles
        for (int i = 0; i < 46; i++) {
            float sx = (i * 97 % 211) / 211F, sy = (i * 53 % 149) / 149F;
            float tw = 0.35F + 0.65F * (0.5F + 0.5F * Mth.sin(t * (0.8F + i % 5 * 0.3F) + i));
            int px = (int) (sx * width), py = (int) ((sy * height + t * (2 + i % 4)) % height);
            g.fill(px, py, px + 1, py + 1, withAlpha(0xFFFFFF, 0.5F * tw * dark));
        }
        // barres de cinema
        int bar = (int) (height * 0.115F * Mth.clamp(t / 1.2F, 0, 1));
        g.fill(0, 0, width, bar, withAlpha(0x000000, 1));
        g.fill(0, height - bar, width, height, withAlpha(0x000000, 1));
        // titre de chapitre
        float titleIn = Mth.clamp((t - 0.7F) / 1.0F, 0, 1), titleOut = t < INTRO ? 1 : Mth.clamp(1 - (t - INTRO) / 1.2F * 0.55F, 0.45F, 1);
        int ty = height / 2 - 40 - (t < INTRO ? 0 : (int) Mth.clamp((t - INTRO) * 30, 0, 50));
        g.centeredText(font, Component.literal(scene.chapter().toUpperCase()), width / 2, ty, withAlpha(accent, titleIn * titleOut * dark));
        g.centeredText(font, Component.literal(scene.title()), width / 2, ty + 14, withAlpha(0xFFFFFF, titleIn * titleOut * dark));
        int ruleW = (int) (80 * titleIn);
        g.fill(width / 2 - ruleW, ty + 28, width / 2 + ruleW, ty + 29, withAlpha(accent, 0.8F * titleIn * dark));
        // repliques
        int y = height / 2 - 10;
        int maxW = Math.min(width - 60, 360);
        for (int i = 0; i < scene.lines().size(); i++) {
            float lt = t - (INTRO + PER_LINE * i);
            if (lt < 0) break;
            float la = Mth.clamp(lt / 1.1F, 0, 1);
            boolean last = i == scene.lines().size() - 1;
            float dim = last || lt < PER_LINE ? 1 : 0.55F;
            for (String row : wrap(scene.lines().get(i), maxW)) {
                g.centeredText(font, Component.literal(row), width / 2, y, withAlpha(0xF3E9D2, la * dim * dark));
                y += 11;
            }
            y += 5;
        }
        g.centeredText(font, Component.literal("Espace : suite   ·   Échap : passer"), width / 2, height - bar + 6, withAlpha(0x9A9080, 0.7F * dark));
    }
}
