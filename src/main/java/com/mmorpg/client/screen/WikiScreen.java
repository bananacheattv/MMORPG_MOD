package com.mmorpg.client.screen;

import com.mmorpg.client.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Wiki consultable en jeu : les pages de docs/wiki embarquees dans le mod, rangees par categorie. */
public class WikiScreen extends MenuScreen {
    private record Page(String file, String title) {
    }

    private record Line(char kind, String text) {
    }

    private static final String[] CATEGORIES = {"Débuter", "Activités", "Économie et social", "Collections", "Référence"};
    private static final int[] COLORS = {Ui.GOLD, Ui.BLUE, Ui.GREEN, 0xFFC58AFF, Ui.RED};
    private static final Page[][] PAGES = {
            {new Page("premiers-pas", "Premiers pas"), new Page("classes", "Classes"), new Page("competences", "Compétences"), new Page("combat", "Combat et bouclier"), new Page("inventaire", "Inventaire et sacs")},
            {new Page("quetes-hebdomadaires", "Quêtes hebdo"), new Page("donjons", "Donjons et boss"), new Page("arenes", "Arènes"), new Page("recompenses-pvp", "Équipement PvP"), new Page("metiers", "Métiers"), new Page("peche", "Pêche")},
            {new Page("economie", "Or, HDV, banque"), new Page("echanges", "Échanges"), new Page("guildes-groupes", "Guildes et groupes"), new Page("coffres-boutique", "Coffres et boutique")},
            {new Page("montures-cosmetiques", "Montures et apparences"), new Page("succes-titres", "Succès et titres")},
            {new Page("catalogue-competences", "Catalogue compétences"), new Page("catalogue-armes", "Catalogue armes"), new Page("commandes-aide", "Commandes")}};

    private static Page selected = PAGES[0][0];
    private int listScroll;
    private int textScroll;
    private List<Line> lines = List.of();
    private Page loaded;
    private int wrapWidth = -1;
    private final List<Object[]> wrapped = new ArrayList<>();
    private int totalHeight;

    public WikiScreen() {
        super(Tab.WIKI);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        int lx = left + 8;
        if (x < lx + 130) listScroll = Math.max(0, Math.min(maxListScroll(), listScroll - (int) Math.signum(sy) * 2));
        else textScroll = Math.max(0, textScroll - (int) Math.signum(sy) * 24);
        return true;
    }

    /** Hauteur de la liste moins la place visible, en crans de 8 px : la molette s'arrete sur la derniere page. */
    private int maxListScroll() {
        int listH = 0;
        for (Page[] pages : PAGES) listH += 13 + pages.length * 18 + 4;
        return Math.max(0, (listH - (ph - 54 - 10 - 18) + 7) / 8);
    }

    private static String inline(String s) {
        return s.replaceAll("\\[([^\\]]*)\\]\\([^)]*\\)", "$1").replace("`", "").replace("**", "").trim();
    }

    private void load(Page p) {
        loaded = p;
        textScroll = 0;
        wrapWidth = -1;
        List<Line> out = new ArrayList<>();
        try {
            var res = Minecraft.getInstance().getResourceManager().getResource(Identifier.parse("mmorpg:wiki/" + p.file + ".md"));
            if (res.isEmpty()) {
                out.add(new Line('t', "Page introuvable."));
            } else {
                String text;
                try (var in = res.get().open()) {                       // flux ferme a chaque page
                    text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                }
                boolean header = true;
                for (String raw : text.split("\\R")) {
                    String l = raw.strip();
                    if (l.isEmpty() || l.startsWith("[Accueil")) {
                        header = true;
                        continue;
                    }
                    if (l.startsWith("# ")) out.add(new Line('1', inline(l.substring(2))));
                    else if (l.startsWith("## ")) out.add(new Line('2', inline(l.substring(3))));
                    else if (l.startsWith("### ")) out.add(new Line('2', inline(l.substring(4))));
                    else if (l.startsWith("- ") || l.startsWith("* ")) out.add(new Line('b', "• " + inline(l.substring(2))));
                    else if (l.startsWith("|")) {
                        if (l.replace("|", "").replace("-", "").replace(":", "").isBlank()) continue;
                        String[] cells = l.substring(1).split("\\|");
                        StringBuilder sb = new StringBuilder();
                        for (String c : cells) sb.append(sb.isEmpty() ? "" : "  ·  ").append(inline(c));
                        out.add(new Line(header ? 'h' : 'r', sb.toString()));
                        header = false;
                        continue;
                    } else out.add(new Line('t', inline(l)));
                    header = true;
                }
            }
        } catch (Exception e) {
            out.add(new Line('t', "Lecture impossible : " + e.getMessage()));
        }
        lines = out;
    }

    private void layout(int width) {
        wrapWidth = width;
        wrapped.clear();
        int y = 0;
        for (Line l : lines) {
            int color = switch (l.kind) {
                case '1' -> Ui.GOLD_LIGHT;
                case '2' -> Ui.GOLD;
                case 'h' -> Ui.BLUE;
                case 'b', 'r' -> Ui.TEXT;
                default -> Ui.MUTED;
            };
            if (l.kind == '2' || l.kind == '1') y += 4;
            for (FormattedCharSequence s : font.split(FormattedText.of(l.text), width - (l.kind == 'b' ? 8 : 0))) {
                wrapped.add(new Object[]{s, y, color, l.kind == 'b' ? 8 : 0});
                y += 10;
            }
            y += l.kind == '1' || l.kind == '2' ? 4 : 2;
        }
        totalHeight = y;
    }

    @Override
    protected void renderTab(GuiGraphicsExtractor g, int mx, int my, float a) {
        int x0 = left + 8, y0 = contentTop, lw = 130, dh = ph - 54 - 10;
        Ui.header(g, x0, y0, lw, "WIKI");
        listScroll = Math.min(listScroll, maxListScroll());              // fenetre agrandie : la liste ne reste pas decalee
        int ly = y0 + 18 - listScroll * 8;
        g.enableScissor(x0, y0 + 16, x0 + lw, y0 + dh);
        for (int c = 0; c < CATEGORIES.length; c++) {
            g.text(font, CATEGORIES[c].toUpperCase(java.util.Locale.ROOT), x0 + 2, ly + 2, COLORS[c], false);
            ly += 13;
            for (Page p : PAGES[c]) {
                boolean sel = p == selected;
                Ui.frame(g, x0, ly, lw, 16, COLORS[c], sel || hovered(x0, ly, lw, 16));
                g.text(font, font.plainSubstrByWidth(p.title, lw - 10), x0 + 6, ly + 4, sel ? Ui.GOLD_LIGHT : Ui.TEXT, false);
                if (ly >= y0 + 16 && ly + 16 <= y0 + dh) click(x0, ly, lw, 16, () -> selected = p);
                ly += 18;
            }
            ly += 4;
        }
        g.disableScissor();

        int dx = x0 + lw + 10, dw = left + pw - 8 - dx;
        Ui.inset(g, dx, y0, dw, dh);
        if (loaded != selected) load(selected);
        int tw = dw - 24;
        if (wrapWidth != tw) layout(tw);
        int view = dh - 12;
        textScroll = Math.max(0, Math.min(textScroll, Math.max(0, totalHeight - view)));
        g.enableScissor(dx + 1, y0 + 1, dx + dw - 1, y0 + dh - 1);
        for (Object[] w : wrapped) {
            int y = y0 + 6 + (int) w[1] - textScroll;
            if (y < y0 - 10 || y > y0 + dh) continue;
            g.text(font, (FormattedCharSequence) w[0], dx + 8 + (int) w[3], y, (int) w[2], false);
        }
        g.disableScissor();
        if (totalHeight > view) {
            int barH = Math.max(14, dh * view / totalHeight);
            int by = y0 + (dh - barH) * textScroll / Math.max(1, totalHeight - view);
            g.fill(dx + dw - 5, y0 + 2, dx + dw - 3, y0 + dh - 2, 0xFF15111D);
            g.fill(dx + dw - 5, by + 2, dx + dw - 3, by + barH - 2, Ui.GOLD);
        }
    }
}
