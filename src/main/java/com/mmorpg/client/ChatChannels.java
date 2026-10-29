package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.chat.ChatChannel;
import com.mmorpg.client.ui.Ui;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
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
        Minecraft.getInstance().gui.hud.getChat().setVisibleMessageFilter(m -> ChatChannel.classify(m.content().getString()) == c);
    }

    @SubscribeEvent
    public static void onReceived(ClientChatReceivedEvent event) {
        ChatChannel shown = active();
        ChatChannel of = ChatChannel.classify(event.getMessage().getString());
        if (of != shown) UNREAD[of.ordinal()]++;
    }

    private static EditBox input(Screen s) {
        for (var child : s.children()) if (child instanceof EditBox box) return box;
        return null;
    }

    private static int prefixWidth(ChatChannel c) {
        return c == ChatChannel.GENERAL ? 0 : Ui.font().width("[" + c.label + "]") + PREFIX_PAD * 2;
    }

    private static int tabWidth(ChatChannel c) {
        return Ui.font().width(c.label) + 12;
    }

    private static int tabX(Screen s, ChatChannel c) {
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

    @SubscribeEvent
    public static void onInit(ScreenEvent.Init.Post event) {
        if (!(event.getScreen() instanceof ChatScreen)) return;
        active();
        shiftInput(event.getScreen());
    }

    @SubscribeEvent
    public static void onRender(ScreenEvent.Render.Post event) {
        Screen s = event.getScreen();
        if (!(s instanceof ChatScreen)) return;
        GuiGraphicsExtractor g = event.getGuiGraphics();
        int mx = event.getMouseX(), my = event.getMouseY();
        int y = s.height - 15 - TAB_H;
        for (ChatChannel c : ChatChannel.values()) {
            int x = tabX(s, c), w = tabWidth(c);
            boolean on = c == active(), hover = Ui.in(mx, my, x, y, w, TAB_H);
            int rgb = 0xFF000000 | c.color;
            Ui.rounded(g, x, y, w, TAB_H, 5, on ? Ui.mix(Ui.CARD, rgb, 0.35f) : hover ? Ui.CARD_HOVER : Ui.CARD);
            Ui.border(g, x, y, w, TAB_H, on ? rgb : Ui.LINE);
            g.text(Ui.font(), c.label, x + 6, y + 3, on ? 0xFFFFFFFF : hover ? Ui.TEXT : Ui.MUTED, true);
            int n = UNREAD[c.ordinal()];
            if (n > 0 && !on) {
                String t = n > 9 ? "9+" : Integer.toString(n);
                int bw = Ui.font().width(t) + 6;
                Ui.rounded(g, x + w - bw + 3, y - 4, bw, 9, 4, 0xFFE04848);
                g.text(Ui.font(), t, x + w - bw + 6, y - 3, 0xFFFFFFFF, false);
            }
        }
        ChatChannel c = active();
        if (c != ChatChannel.GENERAL) {
            int w = prefixWidth(c);
            Ui.rounded(g, 4, s.height - 14, w - 2, 12, 4, Ui.withAlpha(c.color, 150));
            g.text(Ui.font(), "[" + c.label + "]", 4 + PREFIX_PAD, s.height - 12, 0xFFFFFFFF, true);
        }
    }

    @SubscribeEvent
    public static void onClick(ScreenEvent.MouseButtonPressed.Pre event) {
        Screen s = event.getScreen();
        if (!(s instanceof ChatScreen) || event.getButton() != 0) return;
        int y = s.height - 15 - TAB_H;
        for (ChatChannel c : ChatChannel.values()) {
            if (Ui.in(event.getMouseX(), event.getMouseY(), tabX(s, c), y, tabWidth(c), TAB_H)) {
                select(c);
                shiftInput(s);
                event.setCanceled(true);
                return;
            }
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
}
