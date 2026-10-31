package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.chat.ChatChannel;
import com.mmorpg.client.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;

import java.nio.file.Files;
import java.nio.file.Path;

/** Selecteur de canal du chat : onglets au-dessus de la saisie, prefixe colore, filtrage, non-lus, Tab / Alt+fleches. */
@EventBusSubscriber(modid = MMORPG.MODID, value = Dist.CLIENT)
public final class ChatChannels {
    private static final int TAB_H = 13, TAB_GAP = 2, PREFIX_PAD = 4;
    private static ChatChannel active;
    private static final int[] UNREAD = new int[ChatChannel.values().length];

    private ChatChannels() {
    }

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve(MMORPG.MODID).resolve("chat-channel.txt");
    }

    public static ChatChannel active() {
        if (active == null) {
            try {
                active = Files.exists(file()) ? ChatChannel.byName(Files.readString(file())) : ChatChannel.GENERAL;
            } catch (Exception e) {
                active = ChatChannel.GENERAL;
            }
            applyFilter();
        }
        return active;
    }

    public static void select(ChatChannel c) {
        active = c;
        UNREAD[c.ordinal()] = 0;
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), c.name());
        } catch (Exception ignored) {
        }
        applyFilter();
    }

    private static void applyFilter() {
        ChatChannel c = active;
        Minecraft.getInstance().gui.hud.getChat().setVisibleMessageFilter(m -> c.shows(ChatChannel.classify(m.content().getString())));
    }

    @SubscribeEvent
    public static void onReceived(ClientChatReceivedEvent event) {
        ChatChannel shown = active();
        ChatChannel of = ChatChannel.classify(event.getMessage().getString());
        if (!shown.shows(of)) UNREAD[of.ordinal()]++;
    }

    private static EditBox input(Screen s) {
        for (var child : s.children()) if (child instanceof EditBox box) return box;
        return null;
    }

    private static final int ICON = 14;
    private static final java.util.List<AbstractWidget> WIDGETS = new java.util.ArrayList<>();

    private static int prefixWidth(ChatChannel c) {
        int icon = ICON + 3;
        return c == ChatChannel.GENERAL ? icon : icon + Ui.font().width("[" + c.label + "]") + PREFIX_PAD * 2;
    }

    private static int tabWidth(ChatChannel c) {
        return Ui.font().width(c.label) + 12;
    }

    private static int tabX(ChatChannel c) {
        int x = 4;
        for (ChatChannel o : ChatChannel.values()) {
            if (o == c) return x;
            x += tabWidth(o) + TAB_GAP;
        }
        return x;
    }

    private static void shiftInput(Screen s) {
        EditBox box = input(s);
        if (box == null) return;
        int pad = prefixWidth(active());
        box.setX(4 + pad);
        box.setWidth(s.width - 4 - pad);
    }

    /** Onglet cliquable d'un canal. */
    private static final class Tab extends AbstractWidget {
        private final ChatChannel channel;
        private final Screen screen;

        Tab(Screen screen, ChatChannel c) {
            super(tabX(c), screen.height - 15 - TAB_H, tabWidth(c), TAB_H, Component.literal(c.label));
            this.channel = c;
            this.screen = screen;
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (event.button() != 0 || !isMouseOver(event.x(), event.y())) return false;
            onClick(event, doubleClick);
            return true;
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            select(channel);
            shiftInput(screen);
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
            int x = getX(), y = getY(), w = getWidth();
            boolean on = channel == active(), hover = isHovered();
            int rgb = 0xFF000000 | channel.color;
            Ui.rounded(g, x, y, w, TAB_H, 5, on ? Ui.mix(Ui.CARD, rgb, 0.35f) : hover ? Ui.CARD_HOVER : Ui.CARD);
            Ui.border(g, x, y, w, TAB_H, on ? rgb : Ui.LINE);
            g.text(Ui.font(), channel.label, x + 6, y + 3, on ? 0xFFFFFFFF : hover ? Ui.TEXT : Ui.MUTED, true);
            int n = UNREAD[channel.ordinal()];
            if (n > 0 && !on) {
                String t = n > 9 ? "9+" : Integer.toString(n);
                int bw = Ui.font().width(t) + 6;
                Ui.rounded(g, x + w - bw + 3, y - 4, bw, 9, 4, 0xFFE04848);
                g.text(Ui.font(), t, x + w - bw + 6, y - 3, 0xFFFFFFFF, false);
            }
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {
            defaultButtonNarrationText(out);
        }
    }

    /** Icone « message » a gauche de la saisie : couleur du canal actif, clic = canal suivant. */
    private static final class MessageIcon extends AbstractWidget {
        private final Screen screen;

        MessageIcon(Screen screen) {
            super(4, screen.height - 14, ICON, 12, Component.literal("Canal"));
            this.screen = screen;
        }

        @Override
        public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
            if (event.button() != 0 || !isMouseOver(event.x(), event.y())) return false;
            onClick(event, doubleClick);
            return true;
        }

        @Override
        public void onClick(MouseButtonEvent event, boolean doubleClick) {
            select(active().next(1));
            shiftInput(screen);
        }

        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor g, int mx, int my, float partial) {
            ChatChannel c = active();
            int x = getX(), y = getY();
            int rgb = 0xFF000000 | c.color;
            Ui.rounded(g, x, y, ICON, 12, 4, isHovered() ? Ui.CARD_HOVER : Ui.CARD);
            Ui.border(g, x, y, ICON, 12, rgb);
            g.fill(x + 3, y + 3, x + ICON - 3, y + 8, rgb);       // bulle
            g.fill(x + 4, y + 8, x + 6, y + 10, rgb);              // pointe
            g.fill(x + 4, y + 4, x + ICON - 4, y + 5, 0xFF000000 | Ui.CARD); // ligne de texte
            g.fill(x + 4, y + 6, x + ICON - 6, y + 7, 0xFF000000 | Ui.CARD);
        }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput out) {
            defaultButtonNarrationText(out);
        }
    }

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        Screen s = event.getScreen();
        if (!(s instanceof ChatScreen)) return;
        active();
        shiftInput(s);
        WIDGETS.clear();
        for (ChatChannel c : ChatChannel.values()) WIDGETS.add(new Tab(s, c));
        WIDGETS.add(new MessageIcon(s));
        for (AbstractWidget w : WIDGETS) event.addListener(w);
    }

    /** Clic gere ici : l'ecran de chat ne transmet pas toujours les clics aux widgets ajoutes par evenement. */
    @SubscribeEvent
    public static void onMouseClick(ScreenEvent.MouseButtonPressed.Pre event) {
        if (!(event.getScreen() instanceof ChatScreen) || event.getButton() != 0) return;
        for (AbstractWidget w : WIDGETS) {
            if (w.isMouseOver(event.getMouseX(), event.getMouseY())) {
                w.mouseClicked(event.getMouseButtonEvent(), event.isDoubleClick());
                event.setCanceled(true);
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        Screen s = event.getScreen();
        if (!(s instanceof ChatScreen)) return;
        GuiGraphicsExtractor g = event.getGuiGraphics();
        for (AbstractWidget w : WIDGETS) w.extractRenderState(g, event.getMouseX(), event.getMouseY(), event.getPartialTick());
        ChatChannel c = active();
        if (c != ChatChannel.GENERAL) {
            int x = 4 + ICON + 3, w = prefixWidth(c) - ICON - 3;
            Ui.rounded(g, x, s.height - 14, w - 2, 12, 4, Ui.withAlpha(c.color, 150));
            g.text(Ui.font(), "[" + c.label + "]", x + PREFIX_PAD, s.height - 12, 0xFFFFFFFF, true);
        }
    }

    @SubscribeEvent
    public static void onKey(ScreenEvent.KeyPressed.Pre event) {
        Screen s = event.getScreen();
        if (!(s instanceof ChatScreen)) return;
        var k = event.getKeyEvent();
        EditBox box = input(s);
        if (box == null) return;
        int step = 0;
        if (k.isCycleFocus() && !box.getValue().startsWith("/")) step = k.hasShiftDown() ? -1 : 1;
        else if (k.hasAltDown() && k.isRight()) step = 1;
        else if (k.hasAltDown() && k.isLeft()) step = -1;
        if (step != 0) {
            select(active().next(step));
            shiftInput(s);
            event.setCanceled(true);
            return;
        }
        if (k.isConfirmation()) box.setValue(active().route(box.getValue()));
    }

    /** Les onglets sont des widgets focusables : les fleches de l'historique pouvaient leur voler le focus de la saisie. */
    @SubscribeEvent
    public static void onKeyPost(ScreenEvent.KeyPressed.Post event) {
        Screen s = event.getScreen();
        if (!(s instanceof ChatScreen)) return;
        EditBox box = input(s);
        if (box != null && s.getFocused() != box) s.setFocused(box);
    }
}
