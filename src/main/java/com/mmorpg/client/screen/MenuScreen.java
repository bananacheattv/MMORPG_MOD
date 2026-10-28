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
        PERSONNAGE("Personnage"), COMPETENCES("Compétences"), QUETES("Quêtes"), GROUPE("Groupe"), GUILDE("Guilde"), ARENE("Arène"), FAMILIERS("Familiers"), MONTURES("Montures"), COSMETIQUES("Cosmétiques"), BESTIAIRE("Bestiaire"), BOUTIQUE("Boutique");

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
        if(tab==Tab.GUILDE||tab==Tab.ARENE||tab==Tab.BOUTIQUE){var t=new net.minecraft.nbt.CompoundTag();t.putString("action","open");com.mmorpg.client.ClientNet.send(new com.mmorpg.network.Payloads.SocialAction(tab==Tab.GUILDE?"guild":tab==Tab.BOUTIQUE?"store":"arena",t));return;}
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
            case GUILDE,ARENE,BOUTIQUE -> throw new IllegalStateException("Server-backed screen");
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
        g.fill(left + 6, top + 24, left + pw - 6, top + 25, Ui.LINE);
        renderTab(g, mx, my, a);
    }

    protected abstract void renderTab(GuiGraphicsExtractor g, int mx, int my, float a);

    /** Barre d'onglets du menu (partagee avec l'ecran Personnage, qui est un ecran d'inventaire). */
    public static void drawTabs(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font, int left, int top, int pw, Tab active, int mx, int my,
                                java.util.function.BiConsumer<int[], Runnable> click) {
        Tab[] tabs = Tab.values();
        int tw = (pw - 16 - 42) / tabs.length;
        // bouton d'edition de l'interface (HUD) au bout de la barre d'onglets
        int ex = left + pw - 8 - 18;
        boolean eh = Ui.in(mx, my, ex, top + 6, 18, 18);
        g.fill(ex, top + 6, ex + 18, top + 24, eh ? Ui.CARD_HOVER : Ui.CARD);
        Ui.border(g, ex, top + 6, 18, 18, eh ? Ui.GOLD : Ui.LINE);
        g.fill(ex + 4, top + 10, ex + 14, top + 11, eh ? Ui.GOLD_LIGHT : Ui.MUTED);         // pictogramme : cadres d'interface
        g.fill(ex + 4, top + 10, ex + 5, top + 20, eh ? Ui.GOLD_LIGHT : Ui.MUTED);
        g.fill(ex + 4, top + 19, ex + 14, top + 20, eh ? Ui.GOLD_LIGHT : Ui.MUTED);
        g.fill(ex + 13, top + 10, ex + 14, top + 20, eh ? Ui.GOLD_LIGHT : Ui.MUTED);
        g.fill(ex + 6, top + 12, ex + 10, top + 14, eh ? Ui.GOLD : Ui.GOLD_DARK);
        g.fill(ex + 8, top + 16, ex + 12, top + 18, eh ? Ui.GOLD : Ui.GOLD_DARK);
        if (eh) g.setComponentTooltipForNextFrame(font, java.util.List.of(net.minecraft.network.chat.Component.literal("Modifier l'interface (HUD)"),
                net.minecraft.network.chat.Component.literal("Déplacer, redimensionner ou masquer les éléments affichés").withColor(0x9C9282)), mx, my);
        click.accept(new int[]{ex, top + 6, 18, 18}, () -> Minecraft.getInstance().gui.setScreen(new HudEditorScreen(Minecraft.getInstance().gui.screen())));
        drawMusicButton(g, font, ex - 20, top + 6, mx, my, click);
        float common = 1f;                                    // meme taille de texte pour tous les onglets
        for (Tab t : tabs) common = Math.min(common, (tw - 8) / (float) Math.max(1, font.width(t.label)));
        for (int i = 0; i < tabs.length; i++) {
            Tab t = tabs[i];
            int tx = left + 8 + i * tw;
            int ty = top + 6;
            boolean on = t == active;
            boolean hover = Ui.in(mx, my, tx, ty, tw - 2, 18);
            Ui.rounded(g,tx,ty,tw-2,18,5,on ? Ui.CARD_HOVER : hover ? Ui.CARD : 0x00000000);
            if (on) Ui.roundedOutline(g,tx,ty,tw-2,18,5,Ui.GOLD);
            int lw = font.width(t.label);
            float sc = common;
            Ui.scaledText(g, t.label, tx + (tw - 2) / 2f, ty + 9 - 4 * sc, sc, on ? Ui.GOLD_LIGHT : hover ? Ui.TEXT : Ui.MUTED, true);
            if (!on) click.accept(new int[]{tx, ty, tw - 2, 18}, () -> open(t));
        }
    }

    private static final int[] MUSIC_STEPS = {100, 70, 40, 15, 0};

    /** Bouton haut-parleur : change le volume de la musique d'ambiance (100, 70, 40, 15 %, coupee). */
    private static void drawMusicButton(GuiGraphicsExtractor g, net.minecraft.client.gui.Font font, int x, int y, int mx, int my,
                                        java.util.function.BiConsumer<int[], Runnable> click) {
        int vol = com.mmorpg.client.ClientConfig.AMBIENT_MUSIC_VOLUME.get();
        boolean hover = Ui.in(mx, my, x, y, 18, 18);
        int col = hover ? Ui.GOLD_LIGHT : Ui.MUTED;
        g.fill(x, y, x + 18, y + 18, hover ? Ui.CARD_HOVER : Ui.CARD);
        Ui.border(g, x, y, 18, 18, hover ? Ui.GOLD : Ui.LINE);
        g.fill(x + 3, y + 7, x + 6, y + 11, col);                                       // haut-parleur
        for (int i = 0; i < 4; i++) g.fill(x + 6 + i, y + 7 - i, x + 7 + i, y + 11 + i, col);
        if (vol <= 0) {                                                                // coupe : croix
            for (int i = 0; i < 5; i++) {
                g.fill(x + 11 + i, y + 6 + i, x + 12 + i, y + 7 + i, Ui.RED);
                g.fill(x + 11 + i, y + 10 - i, x + 12 + i, y + 11 - i, Ui.RED);
            }
        } else {
            g.fill(x + 12, y + 7, x + 13, y + 11, col);
            if (vol >= 50) g.fill(x + 14, y + 5, x + 15, y + 13, col);
            if (vol >= 90) g.fill(x + 16, y + 3, x + 17, y + 15, col);
        }
        if (hover) g.setComponentTooltipForNextFrame(font, java.util.List.of(
                net.minecraft.network.chat.Component.literal("Musique d'ambiance : " + (vol <= 0 ? "coupée" : vol + " %")),
                net.minecraft.network.chat.Component.literal("Cliquer pour baisser (hub et donjons)").withColor(0x9C9282)), mx, my);
        click.accept(new int[]{x, y, 18, 18}, () -> {
            int next = MUSIC_STEPS[0];
            for (int s : MUSIC_STEPS) {
                if (s < vol) {
                    next = s;
                    break;
                }
            }
            com.mmorpg.client.ClientConfig.AMBIENT_MUSIC_VOLUME.set(next);
            com.mmorpg.client.ClientConfig.AMBIENT_MUSIC_VOLUME.save();
        });
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
