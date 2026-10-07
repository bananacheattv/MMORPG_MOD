package com.mmorpg.client.screen;

import com.mmorpg.client.ClientData;
import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.cosmetic.Cosmetic;
import com.mmorpg.network.Payloads;
import com.mmorpg.rpg.PlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

import java.util.List;

/** Garde-robe des cosmetiques animes, avec apercu anime. */
public class CosmeticsScreen extends MenuScreen {
    private static Cosmetic.Category category = Cosmetic.Category.AURA;
    private static Cosmetic selected;

    public CosmeticsScreen() {
        super(Tab.COSMETIQUES);
    }

    public CosmeticsScreen(Cosmetic cosmetic) { this(); category=cosmetic.category;selected=cosmetic; }

    @Override
    protected void renderTab(GuiGraphicsExtractor g, int mx, int my, float a) {
        PlayerData d = ClientData.DATA;
        int x0 = left + 8;
        int y0 = contentTop;
        // categories
        Cosmetic.Category[] cats = Cosmetic.Category.values();
        for (int i = 0; i < cats.length; i++) {
            Cosmetic.Category c = cats[i];
            int cy = y0 + i * 16;
            button(g, x0, cy, 80, 15, c.label, true, c == category ? 0xFF6A4A1A : 0xFF2A2420, () -> {
                category = c;
                selected = null;
            });
        }
        int owned = 0;
        for (Cosmetic c : Cosmetic.values()) if (d.cosmetics.contains(c.id)) owned++;
        g.text(font, owned + "/" + Cosmetic.values().length + " débloqués", x0 + 4, y0 + 132, Ui.MUTED, false);
        boolean showOwn = com.mmorpg.client.ClientConfig.SHOW_OWN_COSMETICS.get();
        button(g, x0, y0 + 145, 80, 18, "Vue 1re : " + (showOwn ? "oui" : "non"), true, Ui.GOLD, () -> {
            com.mmorpg.client.ClientConfig.SHOW_OWN_COSMETICS.set(!showOwn);
            com.mmorpg.client.ClientConfig.SHOW_OWN_COSMETICS.save();
        });
        if (hovered(x0, y0 + 145, 80, 18)) tooltip(g, List.of(Component.literal("Afficher vos effets en première personne"),
                Component.literal("Utilisez F5 pour voir les ailes et halos de dos.")));
        button(g, x0, y0 + 167, 80, 18, "Voir en jeu", true, Ui.GOLD, () -> {
            minecraft.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            onClose();
        });

        // liste
        int lx = x0 + 88;
        int lw = 150;
        List<Cosmetic> list = Cosmetic.ofCategory(category);
        if (selected == null || selected.category != category) selected = list.isEmpty() ? null : list.get(0);
        String equipped = d.equippedCosmetics.get(category);
        for (int i = 0; i < list.size(); i++) {
            Cosmetic c = list.get(i);
            int ry = y0 + i * 30;
            boolean has = d.cosmetics.contains(c.id);
            boolean eq = c.id.equals(equipped);
            Ui.frame(g, lx, ry, lw, 27, c.color, c == selected || hovered(lx, ry, lw, 27));
            g.fill(lx + 4, ry + 4, lx + 23, ry + 23, Ui.withAlpha(c.color, has ? 0xFF : 0x50));
            Ui.border(g, lx + 4, ry + 4, 19, 19, Ui.darker(c.color | 0xFF000000, 0.5f));
            g.text(font, c.label, lx + 28, ry + 4, has ? Ui.TEXT : Ui.MUTED, false);
            String status = eq ? "Équipé" : has ? "Débloqué" : (c.unlockLevel > 0 ? "Niveau " + c.unlockLevel : "Caisses / Lucky Block");
            g.text(font, status, lx + 28, ry + 15, eq ? Ui.GREEN : has ? Ui.GOLD : Ui.RED, false);
            final Cosmetic cc = c;
            click(lx, ry, lw, 27, () -> selected = cc);
        }

        // apercu
        int px = lx + lw + 8;
        int pwid = left + pw - 8 - px;
        int phei = ph - 30 - 10;
        Ui.inset(g, px, y0, pwid, phei);
        if (selected != null) {
            Cosmetic c = selected;
            g.centeredText(font, Component.literal(c.label), px + pwid / 2, y0 + 5, c.color);
            int boxY = y0 + 18;
            int boxH = Math.max(48, Math.min(96, phei - 95));
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                com.mmorpg.client.model.imported.ImportedCosmetics.preview = c;
                try {
                    if(c.category==Cosmetic.Category.DOS) {
                        InventoryScreen.renderEntityInInventoryFollowsAngle(g,px+2,boxY,px+pwid-2,boxY+boxH,36,.0625f,9,.2f,mc.player);
                    } else if(c.hasModel()) InventoryScreen.renderEntityInInventoryFollowsAngle(g,px+2,boxY,px+pwid-2,boxY+boxH,36,.0625f,-.5f,.2f,mc.player);
                    else InventoryScreen.extractEntityInInventoryFollowsMouse(g, px + 2, boxY, px + pwid - 2, boxY + boxH, 36, 0.0625F, mx, my, mc.player);
                } finally { com.mmorpg.client.model.imported.ImportedCosmetics.preview = null; }
            }
            if (!c.hasModel()) drawPreview(g, c, px + pwid / 2, boxY + boxH / 2 + 6);
            int ty = boxY + boxH + 6;
            ty += Ui.wrap(g, c.description, px + 6, ty, pwid - 12, Ui.TEXT) + 2;
            Ui.wrap(g, c.unlockText(), px + 6, ty, pwid - 12, Ui.MUTED);
            boolean has = d.cosmetics.contains(c.id);
            boolean eq = c.id.equals(equipped);
            int by = y0 + phei - 22;
            if (has) {
                button(g, px + 6, by, pwid - 12, 16, eq ? "Retirer" : "Équiper", true, eq ? 0xFF6A2A2A : 0xFF2A6A3A,
                        () -> ClientNet.send(new Payloads.CosmeticAction(category.ordinal(), eq ? "" : c.id)));
            } else {
                g.centeredText(font, Component.literal("Verrouillé"), px + pwid / 2, by + 4, Ui.RED);
            }
        }
    }

    /** Petite animation 2D illustrant le cosmetique autour du modele. */
    private void drawPreview(GuiGraphicsExtractor g, Cosmetic c, int cx, int cy) {
        float t = (System.currentTimeMillis() % 100000L) / 50f;
        int col = c.color | 0xFF000000;
        switch (c.category) {
            case AURA -> {
                for (int i = 0; i < 14; i++) {
                    float ang = t * 0.12f + i * (Mth.TWO_PI / 14);
                    float yOff = (float) Math.sin(t * 0.08f + i) * 30;
                    int x = (int) (cx + Math.cos(ang) * 26);
                    int y = (int) (cy + yOff);
                    int s = Math.sin(ang) > 0 ? 3 : 2;
                    g.fill(x - s / 2, y - s / 2, x + s / 2 + 1, y + s / 2 + 1, Math.sin(ang) > 0 ? col : Ui.darker(col, 0.6f));
                }
            }
            case AILES -> {
                float flap = (float) Math.sin(t * 0.15f) * 0.35f + 0.6f;
                for (int side = -1; side <= 1; side += 2) {
                    for (int i = 0; i < 6; i++) {
                        for (int j = 0; j <= i; j++) {
                            float u = 6 + i * 4.5f;
                            float v = -20 + j * 5 - i * 1.5f;
                            int x = (int) (cx + side * u * flap);
                            int y = (int) (cy - 10 + v);
                            g.fill(x - 1, y - 1, x + 2, y + 2, Ui.alpha(col, 0.85f));
                        }
                    }
                }
            }
            case HALO -> {
                for (int i = 0; i < 16; i++) {
                    float ang = t * 0.1f + i * (Mth.TWO_PI / 16);
                    int x = (int) (cx + Math.cos(ang) * 12);
                    int y = (int) (cy - 46 + Math.sin(ang) * 3);
                    g.fill(x - 1, y - 1, x + 1, y + 1, col);
                }
            }
            case TRAINEE -> {
                for (int i = 0; i < 10; i++) {
                    float life = ((t * 0.5f + i * 7) % 60) / 60f;
                    int x = (int) (cx - 20 + i * 4 + Math.sin(t * 0.1f + i) * 3);
                    int y = (int) (cy + 44 - life * 20);
                    g.fill(x, y, x + 2, y + 2, Ui.alpha(col, 1 - life));
                }
            }
        }
    }
}
