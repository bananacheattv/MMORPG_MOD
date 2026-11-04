package com.mmorpg.client.screen;

import com.mmorpg.MMORPG;
import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.network.Payloads;
import com.mmorpg.rpg.PlayerClass;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * Choix du personnage a la connexion : liste a droite (3 par page, 12 emplacements dont les non achetes verrouilles),
 * personnage 3D au centre, boutons Creer / Effacer / Retour et « C'est parti ! ».
 */
public final class CharacterSelectScreen extends MmoScreen {
    private static final int PER_PAGE = 3;
    private static int rememberedSel = -1;

    private final CompoundTag data;
    private final int owned, max, live;
    private final long price, credits;
    private final Map<Integer, LivingEntity> previews = new HashMap<>();
    private int sel, page;
    private boolean creating, deleting, armedBuy;
    private long armedAt;
    private EditBox field;

    public CharacterSelectScreen(CompoundTag data) {
        super("Choix du personnage");
        this.data = data;
        this.owned = data.getIntOr("owned", 3);
        this.max = data.getIntOr("max", 12);
        this.live = data.getIntOr("live", 0);
        this.price = data.getLongOr("price", 0);
        this.credits = data.getLongOr("credits", 0);
        int first = live >= 0 && live < owned ? live : 0;
        this.sel = rememberedSel >= 0 && rememberedSel < max ? rememberedSel : first;
        this.page = sel / PER_PAGE;
    }

    @Override protected boolean hasNav() { return false; }
    @Override protected boolean compact() { return true; }
    @Override protected int panelWidth() { return width; }
    @Override protected int panelHeight() { return height; }
    @Override public boolean shouldCloseOnEsc() { return false; }
    @Override public void onClose() { }
    @Override public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) { }

    @Override
    protected void init() {
        super.init();
        field = new EditBox(font, width / 2 - 80, height - 74, 160, 18, Component.literal("Nom"));
        field.setMaxLength(16);
        field.visible = false;
        field.active = false;
        addRenderableWidget(field);
    }

    private CompoundTag slot(int i) {
        var list = data.getListOrEmpty("slots");
        return i >= 0 && i < list.size() ? list.getCompoundOrEmpty(i) : null;
    }

    private boolean isOwned(int i) { return i < owned; }
    private boolean isEmpty(int i) { var s = slot(i); return s == null || s.getBooleanOr("empty", true); }

    private void send(String action, java.util.function.Consumer<CompoundTag> fill) {
        var t = new CompoundTag();
        t.putString("action", action);
        fill.accept(t);
        ClientNet.send(new Payloads.SocialAction("characters", t));
    }

    private LivingEntity preview(int i) {
        if (i == live && minecraft.player != null) return minecraft.player;
        return previews.computeIfAbsent(i, k -> {
            try {
                var s = slot(k);
                if (s == null || s.getBooleanOr("empty", true) || minecraft.level == null || minecraft.player == null) return null;
                var p = new RemotePlayer(minecraft.level, minecraft.player.getGameProfile());
                p.setId(-2000 - k);                              // sans id, le rendu leve « entity ID before ID assignment » (crash au clic)
                var ops = RegistryOps.create(NbtOps.INSTANCE, minecraft.level.registryAccess());
                var equip = s.getCompoundOrEmpty("equip");
                for (String key : equip.keySet()) {
                    EquipmentSlot es = EquipmentSlot.byName(key);
                    if (es == null) continue;
                    ItemStack.CODEC.parse(ops, equip.get(key)).result().ifPresent(item -> p.setItemSlot(es, item));
                }
                return p;
            } catch (RuntimeException e) {
                return null;
            }
        });
    }

    private static PlayerClass cls(int ordinal) {
        PlayerClass[] all = PlayerClass.values();
        return ordinal >= 0 && ordinal < all.length ? all[ordinal] : PlayerClass.NONE;
    }

    private static void drawCover(GuiGraphicsExtractor g, int x, int y, int w, int h) {
        final int tw = 1672, th = 941;
        float k = Math.max(w / (float) tw, h / (float) th);
        int sw = Math.min(tw, Math.round(w / k)), sh = Math.min(th, Math.round(h / k));
        g.blit(RenderPipelines.GUI_TEXTURED, MMORPG.id("textures/gui/brand/citadel_twilight.png"), x, y, (tw - sw) / 2f, (th - sh) / 2f, w, h, sw, sh, tw, th);
    }

    private void play() {
        rememberedSel = sel;
        send("play", t -> t.putInt("slot", sel));
        minecraft.gui.setScreen(null);
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        drawCover(g, 0, 0, width, height);
        g.fillGradient(0, 0, width, height, 0x20000000, 0x90100820);

        // ---- liste a droite
        int lw = Math.min(210, Math.max(170, width / 4)), lx = width - lw - 10, ly = 10, cardH = Math.clamp((height - 10 - 22 - 26 - 8) / PER_PAGE - 4, 40, 70);
        int lh = 26 + PER_PAGE * (cardH + 4) + 24;
        Ui.panel(g, lx, ly, lw, lh);
        Ui.title(g, lx + lw / 2, ly + 7, data.getStringOr("account", "").toUpperCase(java.util.Locale.ROOT), Ui.GOLD_LIGHT);
        int pages = (max + PER_PAGE - 1) / PER_PAGE;
        for (int n = 0; n < PER_PAGE; n++) {
            int i = page * PER_PAGE + n;
            if (i >= max) break;
            int cy = ly + 22 + n * (cardH + 4), cx = lx + 6, cw = lw - 12;
            boolean on = i == sel, hv = hovered(cx, cy, cw, cardH);
            var s = slot(i);
            PlayerClass c = s == null ? PlayerClass.NONE : cls(s.getIntOr("cls", 0));
            Ui.frame(g, cx, cy, cw, cardH, isOwned(i) && !isEmpty(i) ? (c.color | 0xFF000000) : Ui.GOLD_DARK, hv || on);
            if (on) Ui.border(g, cx + 1, cy + 1, cw - 2, cardH - 2, Ui.GOLD_LIGHT);
            int px = cx + 5, pw2 = 52;
            if (isOwned(i) && !isEmpty(i)) {
                if (c.isPlayable()) Ui.classPortrait(g, c.ordinal() - 1, px, cy + 5, pw2, cardH - 10);
                else g.fill(px, cy + 5, px + pw2, cy + cardH - 5, 0xFF2A2438);
                int tx = px + pw2 + 6;
                g.text(font, font.plainSubstrByWidth(s.getStringOr("name", "?"), cw - pw2 - 16), tx, cy + 10, Ui.GOLD_LIGHT, true);
                g.text(font, (c.isPlayable() ? c.label : "Novice") + "  Niv. " + s.getIntOr("level", 1), tx, cy + 26, c.isPlayable() ? (c.color | 0xFF000000) : Ui.TEXT, false);
                long din = s.getLongOr("deleteIn", 0);
                if (din > 0) g.text(font, font.plainSubstrByWidth("Suppression dans " + left(din), cw - pw2 - 16), tx, cy + 42, 0xFFFF8888, false);
                else g.text(font, font.plainSubstrByWidth(s.getStringOr("place", ""), cw - pw2 - 16), tx, cy + 42, Ui.MUTED, false);
            } else if (isOwned(i)) {
                g.centeredText(font, Component.literal("+"), cx + cw / 2, cy + cardH / 2 - 12, Ui.GOLD);
                g.centeredText(font, Component.literal("Emplacement libre"), cx + cw / 2, cy + cardH / 2 + 2, Ui.MUTED);
            } else {
                g.centeredText(font, Component.literal("Emplacement verrouillé"), cx + cw / 2, cy + cardH / 2 - 10, 0xFF8A7F9E);
                g.centeredText(font, Component.literal(price + " crédits"), cx + cw / 2, cy + cardH / 2 + 4, Ui.GOLD);
            }
            int idx = i;
            click(cx, cy, cw, cardH, () -> { sel = idx; creating = false; deleting = false; armedBuy = false; });
        }
        int ny = ly + lh - 16;
        g.centeredText(font, Component.literal((page + 1) + "/" + pages), lx + lw / 2, ny, Ui.TEXT);
        button(g, lx + lw / 2 - 40, ny - 3, 16, 14, "<", page > 0, Ui.MUTED, () -> page--);
        button(g, lx + lw / 2 + 24, ny - 3, 16, 14, ">", page < pages - 1, Ui.MUTED, () -> page++);
        g.centeredText(font, Component.literal(owned + " / " + max + " emplacements"), lx + lw / 2, ly + lh + 5, Ui.MUTED);

        // ---- personnage 3D au centre
        int cxm = (lx - 10) / 2;
        var pv = isOwned(sel) && !isEmpty(sel) ? preview(sel) : null;
        if (pv != null) {
            int bot = height - 60, topY = 24, size = Math.max(30, (bot - topY) / 2 - 6);
            Ui.entityFollowsMouse(g, cxm - 100, topY, cxm + 100, bot, size, .0625f, mx, my, pv);
            var s = slot(sel);
            Ui.scaledText(g, s.getStringOr("name", ""), cxm, 12, 1.4f, Ui.GOLD_LIGHT, true);
        } else {
            String msg = !isOwned(sel) ? "Emplacement verrouillé" : "Crée un nouveau personnage";
            g.centeredText(font, Component.literal(msg), cxm, height / 2 - 20, Ui.TEXT);
        }

        // ---- boutons a droite
        int bw = Math.clamp((cxm * 2 - 20) / 4 - 4, 44, 100), by = height - 28, bx0 = cxm - 2 * bw - 6;
        boolean owned1 = isOwned(sel), empty = isEmpty(sel);
        if (!owned1) {
            boolean can = credits >= price && owned < max && sel == owned;
            String label = armedBuy ? "Confirmer ?" : (sel == owned ? "Débloquer" : "Débloque avant");
            button(g, bx0, by, bw, 22, label, can, 0xFF7A5A1A, () -> {
                if (!armedBuy || System.currentTimeMillis() - armedAt > 5000) { armedBuy = true; armedAt = System.currentTimeMillis(); }
                else { armedBuy = false; send("buy", t -> t.putLong("price", price)); }
            });
        } else {
            button(g, bx0, by, bw, 22, bw >= 90 ? "Créer Personnage" : "Créer", empty, 0xFF2A6A2A, () -> startField(false));
        }
        boolean pending = owned1 && !empty && slot(sel).getLongOr("deleteIn", 0) > 0;
        if (pending) button(g, bx0 + bw + 4, by, bw, 22, bw >= 90 ? "Annuler suppression" : "Annuler", true, 0xFF7A5A1A, () -> send("undelete", t -> t.putInt("slot", sel)));
        else button(g, bx0 + bw + 4, by, bw, 22, "Effacer", owned1 && !empty, 0xFF7A2A2A, () -> startField(true));
        button(g, bx0 + 3 * bw + 12, by, bw, 22, "Retour", true, 0xFF3A3436, () ->
                minecraft.getReportingContext().draftReportHandled(minecraft, this, () -> minecraft.disconnectFromWorld(net.minecraft.client.multiplayer.ClientLevel.DEFAULT_QUIT_MESSAGE), true));
        button(g, bx0 + 2 * bw + 8, by, bw, 22, "C'est parti !", owned1 && !empty && !pending && !creating && !deleting, 0xFF2A6A2A, this::play);

        // ---- saisie (creation / suppression)
        boolean input = creating || deleting;
        field.visible = input;
        field.active = input;
        if (input) {
            int pxl = cxm - 130, pyl = height - 108;
            Ui.panel(g, pxl, pyl, 260, 100);
            String title = creating ? "Nom du nouveau personnage (3 à 16 caractères)" : "Écris « " + (slot(sel) == null ? "" : slot(sel).getStringOr("name", "")) + " » (effacé après 24 h)";
            g.centeredText(font, Component.literal(title), cxm, pyl + 8, deleting ? 0xFFFF8888 : Ui.TEXT);
            field.setX(cxm - 80);
            field.setY(pyl + 26);
            boolean ok = creating ? field.getValue().trim().length() >= 3 : !field.getValue().isBlank();
            button(g, cxm - 85, pyl + 56, 80, 18, creating ? "Créer" : "Effacer", ok, creating ? 0xFF2A6A2A : 0xFF7A2A2A, () -> {
                String v = field.getValue().trim();
                if (creating) { rememberedSel = sel; send("create", t -> { t.putInt("slot", sel); t.putString("name", v); }); minecraft.gui.setScreen(null); }
                else { send("delete", t -> { t.putInt("slot", sel); t.putString("confirm", v); }); creating = false; deleting = false; }
            });
            button(g, cxm + 5, pyl + 56, 80, 18, "Annuler", true, 0xFF3A3436, () -> { creating = false; deleting = false; });
        }
        String status = data.getStringOr("status", "");
        if (!status.isEmpty()) g.centeredText(font, Component.literal(status), cxm, height - 44, 0xFFFFD27A);
    }

    private static String left(long ms) {
        long m = Math.max(1, ms / 60000);
        return m >= 60 ? (m / 60) + " h " + (m % 60) + " min" : m + " min";
    }

    private void startField(boolean delete) {
        creating = !delete;
        deleting = delete;
        field.setValue("");
        setFocused(field);
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (event.key() == InputConstants.KEY_ESCAPE) {
            if (creating || deleting) { creating = false; deleting = false; }
            return true;                                         // pas de fermeture : le choix est obligatoire
        }
        return super.keyPressed(event);
    }
}
