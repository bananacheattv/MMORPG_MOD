package com.mmorpg.client;

import com.mmorpg.MMORPG;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.entity.MountEntity;
import com.mmorpg.mount.MountType;
import com.mmorpg.registry.ModAttachments;
import com.mmorpg.registry.ModEntities;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.PlayerModelType;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/** Opt-in native client QA only: -Pshowcase=integration_checks, isolated world and local HTTP fixtures. */
final class DevSkinModelChecks {
    private static final String BASE = System.getProperty("mmorpg.skinfixtures", "http://127.0.0.1:8765");
    private static final String[] SPECS = {BASE + "/skin.png", "qa_slim", BASE + "/legacy.png", BASE + "/flaky.png", "reset"};
    private static int phase, ticks, galleryTick;
    private static long started;
    private static boolean requested, passed;

    static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.getSingleplayerServer() == null) return;
        try {
            mc.options.pauseOnLostFocus = false;
            if (++ticks < 60) { mc.gui.setScreen(null); return; }
            if (phase < SPECS.length) {
                String spec = SPECS[phase];
                if (!requested) {
                    requested = true; started = System.nanoTime();
                    mc.getWindow().setWindowed(1280, 800);
                    mc.options.guiScale().set(2); mc.resizeGui();
                    mc.gui.setScreen(new Gallery(-1, "Skin : " + spec.substring(spec.lastIndexOf('/') + 1)));
                    var uuid = mc.player.getUUID();
                    var server = mc.getSingleplayerServer();
                    server.execute(() -> {
                        var player = server.getPlayerList().getPlayer(uuid);
                        server.getCommands().performPrefixedCommand(player.createCommandSourceStack(), "skin " + spec);
                    });
                }
                var pub = mc.player.getExistingDataOrNull(ModAttachments.PUBLIC);
                var skin = mc.player.getSkin();
                boolean ready = pub != null && (spec.equals("reset") ? pub.skin.isEmpty() && !skin.body().texturePath().getNamespace().equals("mmorpg")
                        : pub.skin.equals(spec) && skin.body().texturePath().equals(MMORPG.id("skin_url/" + HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(spec.getBytes(StandardCharsets.UTF_8))))));
                if (ready && !passed) {
                    if (phase == 1 && skin.model() != PlayerModelType.SLIM) throw new AssertionError("Pseudo skin lost slim model");
                    var state = (net.minecraft.client.renderer.entity.state.AvatarRenderState) mc.getEntityRenderDispatcher().getRenderer(mc.player).createRenderState(mc.player, 1);
                    if (!state.skin.body().equals(skin.body())) throw new AssertionError("Renderer does not use chosen skin");
                    MMORPG.LOGGER.info("[CODEX CHECKS] SKIN PHASE {} PASS: command, public sync, accessor and renderer", phase);
                    passed = true; galleryTick = 0;
                }
                if (passed && ++galleryTick == 30) DevShowcase.shot(mc, "skin_check_" + phase);
                if (passed && galleryTick >= 40) { phase++; requested = false; passed = false; galleryTick = 0; }
                if (!passed && System.nanoTime() - started > 90_000_000_000L) throw new AssertionError("Skin timeout at phase " + phase);
                return;
            }
            int page = galleryTick / 50, frame = galleryTick++ % 50;
            if (page < 4) {
                if (frame == 0) mc.gui.setScreen(new Gallery(page, "Montures intégrées — " + (page + 1) + "/4"));
                if (frame == 35) DevShowcase.shot(mc, "mounts_integrated_" + page);
            } else {
                MMORPG.LOGGER.info("[CODEX CHECKS] CONTENT PASS: skin URL, pseudo slim, legacy, retry, reset; 22 native mount previews");
                DevShowcase.stop(mc);
            }
        } catch (Throwable failure) {
            MMORPG.LOGGER.error("[CODEX CHECKS] CONTENT FAIL", failure);
            DevShowcase.stop(mc);
        }
    }

    private static final class Gallery extends MmoScreen {
        private final int page;
        private final List<LivingEntity> entities = new ArrayList<>();
        private final List<String> labels = new ArrayList<>();
        Gallery(int page, String title) { super(title); this.page = page; }
        @Override protected int designHeight() { return 360; }
        @Override protected void init() {
            super.init(); entities.clear(); labels.clear();
            if (page < 0) { entities.add(minecraft.player); labels.add("Aperçu natif du personnage"); return; }
            var types = MountType.values();
            for (int i = page * 6; i < Math.min(types.length, page * 6 + 6); i++) {
                var type = types[i];
                var mount = new MountEntity(ModEntities.MOUNTS.get(type).get(), minecraft.level);
                mount.setId(-1000 - i); mount.setDisplay();
                entities.add(mount); labels.add(type.label);
            }
        }
        @Override public void tick() {
            for (var entity : entities) if (entity != minecraft.player) { entity.tickCount++; entity.walkAnimation.update(.2f, 1, 1); }
        }
        @Override protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float partial) {
            Ui.panel(g, left, top, pw, ph);
            Ui.title(g, left + pw / 2, top + 10, getTitle().getString(), Ui.GOLD);
            int columns = page < 0 ? 1 : 3, rows = page < 0 ? 1 : 2;
            int cw = (pw - 16) / columns, ch = (ph - 35) / rows;
            for (int i = 0; i < entities.size(); i++) {
                var entity = entities.get(i);
                int x = left + 8 + i % columns * cw, y = top + 26 + i / columns * ch;
                Ui.inset(g, x, y, cw - 5, ch - 5);
                int scale = (int) (Math.min(ch - 32, cw - 24) / Math.max(2, Math.max(entity.getBbWidth(), entity.getBbHeight()) * 1.4));
                Ui.entityFollowsAngle(g, x + 3, y + 3, x + cw - 8, y + ch - 24, scale, .0625f, -.3f, .05f, entity);
                Ui.wrap(g, labels.get(i), x + 7, y + ch - 21, cw - 16, Ui.TEXT);
            }
        }
    }
}
