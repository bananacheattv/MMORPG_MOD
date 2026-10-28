package com.mmorpg.client.screen;

import com.mmorpg.client.AeloriaFont;
import com.mmorpg.client.ClientConfig;
import com.mmorpg.client.ClientData;
import com.mmorpg.client.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.achievement.StatsScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.client.gui.screens.options.OptionsScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * « Menu Aeloria » : le menu Echap du jeu, en interface plate (voir {@link com.mmorpg.client.UiPack}). Barre de navigation en haut,
 * categories a gauche, recherche, et sections de cartes qui menent aux ecrans du mod et de Minecraft. On herite de {@link PauseScreen}
 * pour garder la pause du jeu, la deconnexion et tout ce qu'un autre mod attend de ce menu.
 */
public class AeloriaDirectoryScreen extends net.minecraft.client.gui.screens.Screen {
    private static final int BG = 0x700D071C;
    private static final int BAR = 0xFF0D0B14;
    private static final int LINE = 0xFF2B2538;
    private static final int CARD = 0xB5221738;
    private static final int CARD_HOVER = 0xDB382550;
    private static final int ICON_BG = 0xFF0B0910;
    private static final int GOLD = 0xFFE2B85A;
    private static final int TEXT = 0xFFEDE8F5;
    private static final int MUTED = 0xFF8D84A3;
    private static final int[] MUSIC_STEPS = {100, 70, 40, 15, 0};

    private enum Cat {
        ALL("Tout", GOLD), ADVENTURE("Aventure", GOLD), TRAVEL("Voyage", 0xFF62A8FF), SETTINGS("Réglages", 0xFF6AE06A), SESSION("Session", 0xFFFF6B6B);

        final String label;
        final int color;

        Cat(String label, int color) {
            this.label = label;
            this.color = color;
        }
    }

    private record Card(Cat cat, ItemStack icon, String title, String desc, Runnable action) {
    }

    private record Hit(int x, int y, int w, int h, Runnable run) {
    }

    private final List<Hit> hits = new ArrayList<>();
    private List<Card> cards = List.of();
    private Button disconnect;
    private EditBox search;
    private Cat category = Cat.ALL;
    private int scroll;
    private int maxScroll;

    public AeloriaDirectoryScreen() {
        super(Component.literal("Tous les menus"));
    }

    @Override
    protected void init() {
        super.init();
        this.disconnect=new com.mmorpg.client.ui.GlassButton(0,0,110,20,"Déconnexion",Ui.GOLD,b->minecraft.getReportingContext().draftReportHandled(minecraft,this,()->minecraft.disconnectFromWorld(net.minecraft.client.multiplayer.ClientLevel.DEFAULT_QUIT_MESSAGE),true));
        this.clearWidgets();                         // les boutons de Minecraft sont remplaces par les cartes ci-dessous
        String previous = search == null ? "" : search.getValue();
        this.search = new EditBox(this.font, 0, 0, 130, 16, Component.literal("Rechercher"));
        this.search.setHint(Component.literal("Rechercher…").withColor(0x6E6684));
        this.search.setMaxLength(24);
        this.search.setValue(previous);
        this.search.setResponder(s -> scroll = 0);
        this.addRenderableWidget(this.search);
        this.cards = buildCards();
    }

    private List<Card> buildCards() {
        Minecraft mc = this.minecraft;
        List<Card> l = new ArrayList<>();
        l.add(new Card(Cat.ADVENTURE, new ItemStack(Items.IRON_CHESTPLATE), "Personnage", "Équipement, caractéristiques et inventaire.", () -> MenuScreen.open(MenuScreen.Tab.PERSONNAGE)));
        l.add(new Card(Cat.ADVENTURE, new ItemStack(Items.DIAMOND_SWORD), "Compétences", "Vos sorts et capacités de classe.", () -> MenuScreen.open(MenuScreen.Tab.COMPETENCES)));
        l.add(new Card(Cat.ADVENTURE, new ItemStack(Items.WRITABLE_BOOK), "Quêtes", "Journal des quêtes en cours et terminées.", () -> MenuScreen.open(MenuScreen.Tab.QUETES)));
        l.add(new Card(Cat.ADVENTURE, new ItemStack(Items.GOLDEN_HELMET), "Groupe", "Invitez des amis et partez en donjon.", () -> MenuScreen.open(MenuScreen.Tab.GROUPE)));
        l.add(new Card(Cat.ADVENTURE, new ItemStack(Items.BONE), "Familiers", "Compagnons qui vous suivent au combat.", () -> MenuScreen.open(MenuScreen.Tab.FAMILIERS)));
        l.add(new Card(Cat.ADVENTURE, new ItemStack(Items.SADDLE), "Montures", "Chevauchez pour traverser le monde.", () -> MenuScreen.open(MenuScreen.Tab.MONTURES)));
        l.add(new Card(Cat.ADVENTURE, new ItemStack(Items.AMETHYST_SHARD), "Cosmétiques", "Apparences, auras et trophées.", () -> MenuScreen.open(MenuScreen.Tab.COSMETIQUES)));
        l.add(new Card(Cat.ADVENTURE, new ItemStack(Items.ENCHANTED_BOOK), "Bestiaire", "Les créatures que vous avez affrontées.", () -> MenuScreen.open(MenuScreen.Tab.BESTIAIRE)));
        l.add(new Card(Cat.TRAVEL, new ItemStack(Items.ENDER_PEARL), "Retour au hub", "Rejoignez la Citadelle des héros.", () -> {
            mc.gui.setScreen(null);
            mc.player.connection.sendCommand("hub");
        }));
        l.add(new Card(Cat.TRAVEL, new ItemStack(Items.IRON_PICKAXE), "Les mines", "Descendez extraire des lingots rares.", () -> {
            mc.gui.setScreen(null);
            mc.player.connection.sendCommand("mine");
        }));
        l.add(new Card(Cat.TRAVEL, new ItemStack(Items.GOLDEN_APPLE), "Progrès", "Les succès de Minecraft.",
                () -> mc.gui.setScreen(new AdvancementsScreen(mc.player.connection.getAdvancements(), this))));
        l.add(new Card(Cat.TRAVEL, new ItemStack(Items.COMPASS), "Statistiques", "Vos chiffres de jeu, objet par objet.",
                () -> mc.gui.setScreen(new StatsScreen(this, mc.player.getStats()))));
        l.add(new Card(Cat.SETTINGS, new ItemStack(Items.COMPARATOR), "Options", "Graphismes, contrôles et son.", () -> mc.gui.setScreen(new OptionsScreen(this, mc.options))));
        l.add(new Card(Cat.SETTINGS, new ItemStack(Items.ITEM_FRAME), "Interface du jeu", "Déplacer ou masquer les éléments du HUD.", () -> mc.gui.setScreen(new HudEditorScreen(this))));
        int vol = ClientConfig.AMBIENT_MUSIC_VOLUME.get();
        l.add(new Card(Cat.SETTINGS, new ItemStack(Items.NOTE_BLOCK), "Musique : " + (vol <= 0 ? "coupée" : vol + " %"), "Volume de l'ambiance (clic pour baisser).", () -> {
            int cur = ClientConfig.AMBIENT_MUSIC_VOLUME.get();
            int next = MUSIC_STEPS[0];
            for (int s : MUSIC_STEPS) {
                if (s < cur) {
                    next = s;
                    break;
                }
            }
            ClientConfig.AMBIENT_MUSIC_VOLUME.set(next);
            ClientConfig.AMBIENT_MUSIC_VOLUME.save();
            this.cards = buildCards();
        }));
        l.add(new Card(Cat.SESSION, new ItemStack(Items.ARROW), "Reprendre l'aventure", "Revenir en jeu.", () -> mc.gui.setScreen(null)));
        if (disconnect != null) {
            l.add(new Card(Cat.SESSION, new ItemStack(Items.BARRIER), mc.isLocalServer() ? "Quitter Aeloria" : "Se déconnecter", "Retour au menu principal.",
                    () -> disconnect.onPress(new KeyEvent(257, 0, 0))));
        }
        return l;
    }

    private static String fold(String s) {
        return Normalizer.normalize(s, Normalizer.Form.NFD).replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT);
    }

    private List<Card> visible(Cat cat) {
        String q = fold(search == null ? "" : search.getValue().trim());
        List<Card> out = new ArrayList<>();
        for (Card c : cards) {
            if (c.cat != cat) continue;
            if (!q.isEmpty() && !fold(c.title).contains(q) && !fold(c.desc).contains(q)) continue;
            out.add(c);
        }
        return out;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mx, int my, float a) {
        extractBlurredBackground(g);
        g.fill(0, 0, this.width, this.height, BG);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mx, int my, float a) {
        hits.clear();
        int w = this.width;
        navBar(g, mx, my);
        boolean side = w >= 330;
        int sideW = side ? 108 : 0;
        int cx = side ? sideW + 26 : 14;
        int cw = w - cx - 14;

        // fil d'Ariane, grand titre et recherche
        g.text(font, "Accueil  ›  Menu Aeloria", cx, 36, MUTED, false);
        int titleY = 50;
        arrow(g, cx, titleY + 2, mx, my);
        Ui.scaledText(g, "MENU AELORIA", cx + 22, titleY, 1.7F, TEXT, false);
        int sw = Math.min(140, Math.max(90, cw / 3));
        search.setX(w - 14 - sw);
        search.setY(titleY + 1);
        search.setWidth(sw);
        search.extractRenderState(g, mx, my, a);

        int top = 82;
        if (side) sidebar(g, mx, my, top);
        else compactTabs(g, mx, my, top);
        int listTop = side ? top : top + 20;

        // sections de cartes (defilables)
        int colMin = 120;
        int cols = Mth.clamp((cw + 6) / (colMin + 6), 1, 4);
        int gap = 6;
        int cardW = (cw - gap * (cols - 1)) / cols;
        int cardH = 46;
        g.enableScissor(cx - 2, listTop, w, this.height);
        int y = listTop - scroll;
        boolean any = false;
        for (Cat cat : Cat.values()) {
            if (cat == Cat.ALL || (category != Cat.ALL && category != cat)) continue;
            List<Card> list = visible(cat);
            if (list.isEmpty()) continue;
            any = true;
            g.fill(cx, y + 1, cx + 3, y + 10, cat.color);
            g.text(font, cat.label.toUpperCase(Locale.ROOT), cx + 8, y + 2, cat.color, false);
            y += 16;
            for (int i = 0; i < list.size(); i++) {
                int px = cx + (i % cols) * (cardW + gap);
                int py = y + (i / cols) * (cardH + gap);
                card(g, list.get(i), px, py, cardW, cardH, mx, my, listTop);
            }
            y += ((list.size() + cols - 1) / cols) * (cardH + gap) + 6;
        }
        if (!any) g.text(font, "Aucun résultat.", cx, listTop + 4, MUTED, false);
        g.disableScissor();
        maxScroll = Math.max(0, y + scroll - (this.height - 14));
        scroll = Mth.clamp(scroll, 0, maxScroll);
        if (maxScroll > 0) {
            int trackH = this.height - listTop - 6;
            int barH = Math.max(14, trackH * trackH / (trackH + maxScroll));
            int by = listTop + (trackH - barH) * scroll / maxScroll;
            g.fill(w - 6, listTop, w - 4, listTop + trackH, 0xFF15111D);
            g.fill(w - 6, by, w - 4, by + barH, GOLD);
        }
        g.text(font, "M : personnage     U : modifier l'interface", cx, this.height - 11, 0xFF5C556E, false);
    }

    /** Barre du haut : logo, onglets rapides, puis or, niveau et nom du joueur. */
    private void navBar(GuiGraphicsExtractor g, int mx, int my) {
        int w = this.width;
        g.fill(0, 0, w, 27, BAR);
        g.fill(0, 27, w, 28, LINE);
        com.mmorpg.client.ui.Logo.emblem(g, 8, 2, 24);
        var logo = AeloriaFont.of("AELORIA");
        int lw = font.width(logo);
        float sc = 1.2F;
        g.pose().pushMatrix();
        g.pose().translate(36, 9);
        g.pose().scale(sc, sc);
        g.text(font, logo, 0, 0, GOLD, true);
        g.pose().popMatrix();
        int x = 36 + Math.round(lw * sc) + 18;
        String[] tabs = {"Menu", "Personnage", "Quêtes", "Groupe", "Options"};
        for (String t : tabs) {
            int tw = font.width(t);
            if (x + tw + 10 > w - 190) break;
            boolean on = t.equals("Menu");
            boolean hover = Ui.in(mx, my, x - 4, 0, tw + 8, 27);
            g.text(font, t, x, 10, on ? TEXT : hover ? TEXT : MUTED, false);
            if (on) g.fill(x - 2, 25, x + tw + 2, 27, GOLD);
            if (!on) {
                Runnable r = switch (t) {
                    case "Personnage" -> () -> MenuScreen.open(MenuScreen.Tab.PERSONNAGE);
                    case "Quêtes" -> () -> MenuScreen.open(MenuScreen.Tab.QUETES);
                    case "Groupe" -> () -> MenuScreen.open(MenuScreen.Tab.GROUPE);
                    default -> () -> this.minecraft.gui.setScreen(new OptionsScreen(this, this.minecraft.options));
                };
                hits.add(new Hit(x - 4, 0, tw + 8, 27, r));
            }
            x += tw + 18;
        }
        // pastilles a droite : or, niveau, nom
        String name = this.minecraft.getUser().getName();
        String lvl = "Niv. " + ClientData.DATA.level;
        String gold = Ui.fmt(ClientData.DATA.gold) + " or";
        int cx = w - 8;
        cx = chip(g, name, cx, TEXT, 0xFF1B1626);
        cx = chip(g, lvl, cx, 0xFF62A8FF, 0xFF141C2B);
        chip(g, gold, cx, GOLD, 0xFF2A2112);
    }

    private int chip(GuiGraphicsExtractor g, String text, int right, int color, int fill) {
        int tw = font.width(text) + 12;
        int x = right - tw;
        Ui.rounded(g,x,5,tw,17,6,fill);
        g.outline(x, 5, tw, 17, Ui.withAlpha(color & 0xFFFFFF, 0x60));
        g.text(font, text, x + 6, 10, color, false);
        return x - 6;
    }

    /** Fleche de retour (reprend la partie). */
    private void arrow(GuiGraphicsExtractor g, int x, int y, int mx, int my) {
        boolean hover = Ui.in(mx, my, x - 2, y - 2, 20, 20);
        g.fill(x - 2, y - 2, x + 18, y + 18, hover ? CARD_HOVER : CARD);
        g.outline(x - 2, y - 2, 20, 20, hover ? GOLD : LINE);
        int c = hover ? GOLD : TEXT;
        for (int i = 0; i < 5; i++) {            // chevron « < »
            g.fill(x + 5 + i, y + 8 - i, x + 7 + i, y + 9 - i, c);
            g.fill(x + 5 + i, y + 8 + i, x + 7 + i, y + 9 + i, c);
        }
        hits.add(new Hit(x - 2, y - 2, 20, 20, () -> this.minecraft.gui.setScreen(null)));
    }

    private void sidebar(GuiGraphicsExtractor g, int mx, int my, int top) {
        int x = 14;
        int w = 108;
        int y = top;
        for (Cat c : Cat.values()) {
            boolean on = c == category;
            boolean hover = Ui.in(mx, my, x, y, w, 20);
            Ui.rounded(g,x,y,w,20,6,on ? CARD_HOVER : hover ? CARD_HOVER : CARD);
            g.outline(x, y, w, 20, on ? Ui.withAlpha(c.color & 0xFFFFFF, 0xC0) : LINE);
            if (on) g.fill(x, y, x + 3, y + 20, c.color);
            g.text(font, c.label, x + 10, y + 6, on ? TEXT : MUTED, false);
            hits.add(new Hit(x, y, w, 20, () -> {
                category = c;
                scroll = 0;
            }));
            y += 24;
        }
    }

    /** Petit ecran : les categories passent en onglets horizontaux. */
    private void compactTabs(GuiGraphicsExtractor g, int mx, int my, int top) {
        int x = 14;
        for (Cat c : Cat.values()) {
            int tw = font.width(c.label) + 12;
            boolean on = c == category;
            Ui.rounded(g,x,top,tw,16,5,on ? CARD_HOVER : CARD);
            g.outline(x, top, tw, 16, on ? c.color : LINE);
            g.text(font, c.label, x + 6, top + 4, on ? TEXT : MUTED, false);
            hits.add(new Hit(x, top, tw, 16, () -> {
                category = c;
                scroll = 0;
            }));
            x += tw + 4;
        }
    }

    private void card(GuiGraphicsExtractor g, Card c, int x, int y, int w, int h, int mx, int my, int clipTop) {
        boolean hover = Ui.in(mx, my, x, y, w, h) && my >= clipTop;
        Ui.rounded(g,x,y,w,h,7,hover ? CARD_HOVER : CARD);
        g.outline(x, y, w, h, hover ? c.cat.color : LINE);
        Ui.rounded(g,x+6,y+6,32,32,5,ICON_BG);
        g.outline(x + 6, y + 6, 32, 32, hover ? Ui.withAlpha(c.cat.color & 0xFFFFFF, 0xA0) : 0xFF221D2E);
        g.pose().pushMatrix();
        g.pose().translate(x + 10, y + 10);
        g.pose().scale(1.5F, 1.5F);
        g.item(c.icon, 0, 0);
        g.pose().popMatrix();
        int tx = x + 45;
        int tw = w - 51;
        String title = font.plainSubstrByWidth(c.title, tw);
        g.text(font, title, tx, y + 7, TEXT, false);
        g.pose().pushMatrix();
        g.pose().translate(tx, y + 19);
        g.pose().scale(0.75F, 0.75F);
        var lines = font.split(Component.literal(c.desc), (int) (tw / 0.75F));
        for (int i = 0; i < Math.min(2, lines.size()); i++) g.text(font, lines.get(i), 0, i * 10, MUTED, false);
        g.pose().popMatrix();
        if (my >= clipTop) hits.add(new Hit(x, y, w, h, c.action));
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (search != null && search.mouseClicked(event, doubleClick)) {
            this.setFocused(search);
            return true;
        }
        if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit h = hits.get(i);
                if (Ui.in(event.x(), event.y(), h.x, h.y, h.w, h.h)) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    h.run.run();
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double scrollX, double scrollY) {
        scroll = Mth.clamp(scroll - (int) (scrollY * 18), 0, maxScroll);
        return true;
    }
}
