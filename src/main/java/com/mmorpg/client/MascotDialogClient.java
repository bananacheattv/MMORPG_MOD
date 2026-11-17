package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.client.hud.HudLayout;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.entity.MascotEntity;
import com.mmorpg.entity.NpcEntity;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;

/** Dialogue paginé, déplaçable avec son interlocuteur en 3D. */
public final class MascotDialogClient {
    public static final Identifier LAYER = MMORPG.id("npc_dialog");
    private static String speaker = "", text = "";
    private static long since, until;
    private static net.minecraft.client.multiplayer.ClientLevel world;
    private static NpcEntity portrait, fallback;
    /** Page choisie à la main (curseur libre) ; -1 = défilement automatique. */
    private static int manual = -1, pages = 1, shownPage;
    private static boolean free;
    private static final int[] PREV = new int[4], NEXT = new int[4];
    private static int originX, originY; private static float originScale = 1f;

    /** Vrai tant qu'un dialogue est affiché (sert de condition à l'indice et au curseur libre). */
    public static boolean visible() {
        return !text.isEmpty() && System.currentTimeMillis() < until && !LoadingClient.active();
    }
    public static void setFree(boolean on) {
        free = on;
        long now = System.currentTimeMillis();
        if (on && manual < 0) manual = shownPage;
        if (!on) until = Math.max(until, now + 6000);
    }
    /** Page suivante (+1) ou précédente (-1) ; après la dernière page, le dialogue se ferme. */
    public static void turn(int d) {
        long now = System.currentTimeMillis();
        if (manual < 0) manual = shownPage;
        int n = manual + d;
        if (n >= pages) { until = now; manual = -1; return; }
        manual = Math.max(0, n);
        until = Math.max(until, now + 6000);
    }
    /** Clic en coordonnées GUI absolues ; vrai si un bouton a répondu. */
    public static boolean click(double mx, double my) {
        if (!visible()) return false;
        double lx = (mx - originX) / originScale, ly = (my - originY) / originScale;
        if (Ui.in(lx, ly, PREV[0], PREV[1], PREV[2], PREV[3]) && shownPage > 0) { turn(-1); return true; }
        if (Ui.in(lx, ly, NEXT[0], NEXT[1], NEXT[2], NEXT[3])) { turn(1); return true; }
        return false;
    }

    public static void say(String name, String line) {
        speaker = name.isEmpty() ? "Flammèche" : name;
        text = line;
        since = System.currentTimeMillis();
        until = since + Math.max(8000, 5000 + 65L * line.length());
        manual = -1; pages = 1; shownPage = 0;
        var mc = Minecraft.getInstance();
        world = mc.level; portrait = null; fallback = null;
        if (mc.level == null || mc.player == null) return;
        double best = Double.MAX_VALUE;
        for (var e : mc.level.entitiesForRendering()) if (e instanceof NpcEntity n && n.displayName().equals(speaker)) {
            if (n instanceof MascotEntity m && m.ownerId() != mc.player.getId()) continue;
            double distance = n.distanceToSqr(mc.player);
            if (distance < best) { best = distance; portrait = n; }
        }
    }
    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        var mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || mc.gui.hud.isHidden()) return;
        if (world != mc.level) { text = ""; portrait = null; fallback = null; world = mc.level; }
        boolean demo = HudLayout.editing;
        long now = System.currentTimeMillis();
        if (!demo && (now >= until || text.isEmpty() || LoadingClient.active())) return;
        var element = HudLayout.DIALOGUE;
        if (!HudLayout.begin(g, element)) return;
        int[] size = element.contentSize(g.guiWidth(), g.guiHeight());
        int w = size[0], h = size[1], tx = 82;
        String shown = demo ? "Bienvenue sur Aeloria ! Je suis ta guide. Suis mes missions pour découvrir la forge, les mines et tes premières quêtes." : text;
        String who = demo ? "Flammèche" : speaker;
        Ui.panel(g, 0, 0, w, h); Ui.inset(g, 6, 7, 68, h - 14);
        g.text(mc.font, mc.font.plainSubstrByWidth(who.toUpperCase(java.util.Locale.ROOT), w - tx - 12), tx, 10, 0xFFD6A4FF, false);
        var lines = mc.font.split(AeloriaFont.of(shown), Math.max(40, w - tx - 12));
        int rows = 5, pages = Math.max(1, (lines.size() + rows - 1) / rows);
        long duration = Math.max(8000, until - since);
        int page = demo ? 0 : Math.min(pages - 1, (int)((now - since) * pages / duration));
        if (manual >= 0 && !demo) page = Math.min(pages - 1, manual);
        if (!demo) {
            MascotDialogClient.pages = pages; shownPage = page;
            if (mc.gui.screen() instanceof FreeCursorScreen) until = Math.max(until, now + 6000);
        }
        for (int i = 0; i < rows && page * rows + i < lines.size(); i++)
            g.text(mc.font, lines.get(page * rows + i), tx, 28 + i * 11, Ui.TEXT, false);
        boolean cursor = !demo && mc.gui.screen() instanceof FreeCursorScreen;
        String hint = cursor ? "relâche pour reprendre la caméra" : "Maintiens " + Keys.FREE_CURSOR.getTranslatedKeyMessage().getString() + " · curseur libre";
        g.text(mc.font, pages > 1 ? (page + 1) + " / " + pages + " · " + hint : "U · déplacer · " + hint, tx, h - 12, Ui.MUTED, false);
        if (!demo) {
            var r0 = element.rect(g.guiWidth(), g.guiHeight());
            originX = r0[0]; originY = r0[1]; originScale = element.scale();
            double mx = cursor ? (mc.mouseHandler.xpos() / mc.getWindow().getGuiScale() - originX) / originScale : -999;
            double my = cursor ? (mc.mouseHandler.ypos() / mc.getWindow().getGuiScale() - originY) / originScale : -999;
            int by = h - 22; boolean last = page >= pages - 1;
            PREV[0] = w - 78; PREV[1] = by; PREV[2] = 30; PREV[3] = 16;
            NEXT[0] = w - 44; NEXT[1] = by; NEXT[2] = last ? 36 : 30; NEXT[3] = 16;
            if (last) NEXT[0] = w - 50;
            if (cursor || pages > 1) {
                if (pages > 1) Ui.button(g, PREV[0], PREV[1], PREV[2], PREV[3], "<", page > 0, (int) mx, (int) my, Ui.GOLD_DARK);
                Ui.button(g, NEXT[0], NEXT[1], NEXT[2], NEXT[3], last ? "OK" : ">", true, (int) mx, (int) my, Ui.GOLD_DARK);
            }
        }
        HudLayout.end(g);
        NpcEntity actor = portrait;
        if (actor == null || actor.level() != mc.level || actor.isRemoved() || demo) {
            if (fallback == null || fallback.level() != mc.level) {
                if (demo || speaker.equals("Flammèche")) {
                    var mascot = new MascotEntity(com.mmorpg.registry.ModEntities.MASCOT.get(), mc.level);
                    mascot.setId(-160002); mascot.setOwnerId(mc.player.getId()); fallback = mascot;
                } else {
                    fallback = new NpcEntity(com.mmorpg.registry.ModEntities.NPC.get(), mc.level);
                    fallback.setId(-160003); fallback.setup(NpcEntity.Role.QUETES, speaker, 0, "");
                }
            }
            actor = fallback;
        }
        var state = mc.getEntityRenderDispatcher().getRenderer(actor).createRenderState(actor, 1f);
        if (state instanceof net.minecraft.client.renderer.entity.state.LivingEntityRenderState living) { living.scale=1; living.bodyRot=180; living.yRot=0; living.xRot=0; }
        state.shadowPieces.clear(); state.outlineColor = 0; state.lightCoords = 0xF000F0;
        int[] r = element.rect(g.guiWidth(), g.guiHeight());
        float scale = element.scale(); boolean mascot = actor instanceof MascotEntity;
        // Les portraits natifs ne suivent pas la matrice du GUI : appliquer le rectangle de U explicitement.
        g.entity(state, (mascot ? 14 : 32) * scale, new org.joml.Vector3f(0, mascot ? 1.7f : 1.05f, 0),
                new org.joml.Quaternionf().rotateZ((float)Math.PI).rotateY((float)Math.toRadians(15)), null,
                r[0] + Math.round(7 * scale), r[1] + Math.round(8 * scale),
                r[0] + Math.round(73 * scale), r[1] + Math.round((h - 8) * scale));
    }
}
