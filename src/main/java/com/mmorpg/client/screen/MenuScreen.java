package com.mmorpg.client.screen;

import com.mmorpg.client.ClientData;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.KeyEvent;

/** Menu principal a onglets du mod. */
public abstract class MenuScreen extends MmoScreen {
    public enum Tab {
        PERSONNAGE("Personnage"), COMPETENCES("Compétences"), QUETES("Quêtes"), GROUPE("Groupe"), FAMILIERS("Familiers"), MONTURES("Montures"), COSMETIQUES("Cosmétiques"), BESTIAIRE("Bestiaire");

        public final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    public static Tab lastTab = Tab.PERSONNAGE;
    protected final Tab tab;
    protected int contentTop;

    protected MenuScreen(Tab tab) {
        super(tab.label);
        this.tab = tab;
    }

    public static void open(Tab tab) {
        if (tab == Tab.PERSONNAGE) {             // personnage + inventaire + equipement (aussi sans classe)
            lastTab = tab;
            Minecraft.getInstance().gui.setScreen(new CharacterScreen(Minecraft.getInstance().player));
            return;
        }
        if (!ClientData.DATA.playerClass.isPlayable()) {
            Minecraft.getInstance().gui.setScreen(new ClassSelectScreen());
            return;
        }
        lastTab = tab;
        Minecraft.getInstance().gui.setScreen(switch (tab) {
            case PERSONNAGE -> new CharacterScreen(Minecraft.getInstance().player);
            case COMPETENCES -> new SkillsScreen();
            case QUETES -> new QuestLogScreen();
            case GROUPE -> new PartyScreen();
            case FAMILIERS -> new PetsScreen();
            case MONTURES -> new MountsScreen();
            case COSMETIQUES -> new CosmeticsScreen();
            case BESTIAIRE -> new BestiaryScreen();
        });
    }

    @Override
    protected void init() {
        super.init();
        this.contentTop = top + 30;
    }

    @Override
    protected final void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        Ui.panel(g, left, top, pw, ph);
        drawTabs(g, font, left, top, pw, tab, mx, my, (r, action) -> click(r[0], r[1], r[2], r[3], action));
        g.fill(left + 6, top + 24, left + pw - 6, top + 25, Ui.GOLD_DARK);
        renderTab(g, mx, my, a);
    }

    protected abstract void renderTab(GuiGraphicsExtractor g, int mx, int my, float a);

    /** Barre d'onglets du menu (partagee avec l'ecran Personnage, qui est un ecran d'inventaire). */
    public static void drawTabs(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font, int left, int top, int pw, Tab active, int mx, int my,
                                java.util.function.BiConsumer<int[], Runnable> click) {
        Tab[] tabs = Tab.values();
        int tw = (pw - 16 - 20) / tabs.length;
        // bouton d'edition de l'interface (HUD) au bout de la barre d'onglets
        int ex = left + pw - 8 - 18;
        boolean eh = Ui.in(mx, my, ex, top + 6, 18, 18);
        g.fillGradient(ex, top + 6, ex + 18, top + 24, eh ? 0xFF3A2E24 : 0xFF241C1A, 0xFF141012);
        Ui.border(g, ex, top + 6, 18, 18, eh ? Ui.GOLD : Ui.GOLD_DARK);
        g.fill(ex + 4, top + 10, ex + 14, top + 11, eh ? Ui.GOLD_LIGHT : Ui.MUTED);         // pictogramme : cadres d'interface
        g.fill(ex + 4, top + 10, ex + 5, top + 20, eh ? Ui.GOLD_LIGHT : Ui.MUTED);
        g.fill(ex + 4, top + 19, ex + 14, top + 20, eh ? Ui.GOLD_LIGHT : Ui.MUTED);
        g.fill(ex + 13, top + 10, ex + 14, top + 20, eh ? Ui.GOLD_LIGHT : Ui.MUTED);
        g.fill(ex + 6, top + 12, ex + 10, top + 14, eh ? Ui.GOLD : Ui.GOLD_DARK);
        g.fill(ex + 8, top + 16, ex + 12, top + 18, eh ? Ui.GOLD : Ui.GOLD_DARK);
        if (eh) g.setComponentTooltipForNextFrame(font, java.util.List.of(net.minecraft.network.chat.Component.literal("Modifier l'interface (HUD)"),
                net.minecraft.network.chat.Component.literal("Déplacer, redimensionner ou masquer les éléments affichés").withColor(0x9C9282)), mx, my);
        click.accept(new int[]{ex, top + 6, 18, 18}, () -> Minecraft.getInstance().gui.setScreen(new HudEditorScreen(Minecraft.getInstance().gui.screen())));
        for (int i = 0; i < tabs.length; i++) {
            Tab t = tabs[i];
            int tx = left + 8 + i * tw;
            int ty = top + 6;
            boolean on = t == active;
            boolean hover = Ui.in(mx, my, tx, ty, tw - 2, 18);
            g.fillGradient(tx, ty, tx + tw - 2, ty + 18, on ? 0xFF5A4220 : hover ? 0xFF3A2E24 : 0xFF241C1A, on ? 0xFF2A1E10 : 0xFF141012);
            Ui.border(g, tx, ty, tw - 2, 18, on ? Ui.GOLD : Ui.GOLD_DARK);
            if (on) g.fill(tx + 1, ty + 17, tx + tw - 3, ty + 18, 0xFF2A1E10);
            int lw = font.width(t.label);
            float sc = Math.min(1f, (tw - 8) / (float) Math.max(1, lw));
            Ui.scaledText(g, t.label, tx + (tw - 2) / 2f, ty + 9 - 4 * sc, sc, on ? Ui.GOLD_LIGHT : hover ? Ui.TEXT : Ui.MUTED, true);
            if (!on) click.accept(new int[]{tx, ty, tw - 2, 18}, () -> open(t));
        }
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (com.mmorpg.client.Keys.MENU.matches(event) && !(getFocused() instanceof net.minecraft.client.gui.components.EditBox)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }
}
