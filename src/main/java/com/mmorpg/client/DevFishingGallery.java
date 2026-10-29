package com.mmorpg.client;

import com.mmorpg.client.ui.ItemPreview3D;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.fishing.FishCatalog;
import com.mmorpg.registry.ModItems;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/** Planches de rendu natif des objets de la peche (memes modeles et textures qu'en jeu) : cannes, coffres, poissons, objets. Dev uniquement. */
public final class DevFishingGallery extends MmoScreen {
    public static final int PAGES = 6;
    public int page;
    private final ItemPreview3D[] previews = new ItemPreview3D[12];

    public DevFishingGallery() {
        super("Atelier de peche");
        for (int i = 0; i < previews.length; i++) previews[i] = new ItemPreview3D();
    }

    @Override
    protected int panelWidth() {
        return width - 12;
    }

    @Override
    protected int panelHeight() {
        return height - 12;
    }

    private static List<String> ids(int page) {
        List<String> out = new ArrayList<>();
        var fish = FishCatalog.of(FishCatalog.Kind.POISSON);
        switch (page) {
            case 0 -> FishCatalog.RODS.forEach(r -> out.add(r.id()));
            case 1 -> {
                FishCatalog.of(FishCatalog.Kind.TRESOR).forEach(e -> out.add(e.id()));
                FishCatalog.of(FishCatalog.Kind.DECHET).forEach(e -> out.add(e.id()));
            }
            case 2 -> fish.subList(0, 9).forEach(e -> out.add(e.id()));
            case 3 -> fish.subList(9, 18).forEach(e -> out.add(e.id()));
            case 4 -> fish.subList(18, fish.size()).forEach(e -> out.add(e.id()));
            default -> FishCatalog.INGREDIENTS.forEach(i -> out.add(i.id()));
        }
        return out;
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        Ui.panel(g, left, top, pw, ph);
        String[] titles = {"CANNES D'AELORIA", "COFFRES ET DÉCHETS REPÊCHÉS", "POISSONS (1/3)", "POISSONS (2/3)", "POISSONS (3/3)", "INGRÉDIENTS MARINS"};
        Ui.title(g, left + pw / 2, top + 8, titles[page], Ui.GOLD);
        List<String> ids = ids(page);
        int cols = page == 0 ? 3 : 3, rows = (ids.size() + cols - 1) / cols;
        int cw = (pw - 16 - (cols - 1) * 6) / cols, ch = (ph - 40 - (rows - 1) * 5) / rows;
        for (int i = 0; i < ids.size(); i++) {
            int x = left + 8 + i % cols * (cw + 6), y = top + 28 + i / cols * (ch + 5);
            Ui.inset(g, x, y, cw, ch);
            var item = ModItems.get(ids.get(i));
            var stack = new ItemStack(item);
            g.centeredText(font, stack.getHoverName(), x + cw / 2, y + 4, Ui.GOLD);
            boolean rod = page == 0;
            previews[i].draw(g, stack, x + 4, y + 16, cw - 8, ch - 20, rod ? 0f : -.12f, rod ? 0f : .5f);
        }
    }
}
