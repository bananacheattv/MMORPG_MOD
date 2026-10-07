package com.mmorpg.client.screen;

import com.mmorpg.client.ClientData;
import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.mount.MountType;
import com.mmorpg.network.Payloads;
import com.mmorpg.entity.MountEntity;
import com.mmorpg.registry.ModEntities;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class MountsScreen extends MenuScreen {
    private MountType selected = MountType.SANGLIER;
    private int scroll;
    private boolean confirm;
    private final java.util.Map<MountType, MountEntity> previews = new java.util.EnumMap<>(MountType.class);
    public MountsScreen() { super(Tab.MONTURES); }
    @Override protected void renderTab(GuiGraphicsExtractor g, int mx, int my, float a) {
        var d = ClientData.DATA;
        int x = left + 8, y = contentTop, lw = 165;
        MountType[] all = MountType.values();
        Ui.header(g, x, y, lw, "MONTURES (" + d.mounts.size() + "/" + all.length + ")");
        int rowH = 18, rows = Math.max(1, (top + ph - 10 - (y + 18)) / rowH);
        scroll = Math.max(0, Math.min(scroll, all.length - rows));
        for (int i = 0; i < Math.min(rows, all.length - scroll); i++) {
            var type = all[i + scroll];
            int ry = y + 18 + i * rowH;
            boolean owned = d.mounts.contains(type.id);
            Ui.frame(g, x, ry, lw, rowH - 2, owned ? Ui.GOLD : Ui.MUTED, type == selected);
            g.text(font, font.plainSubstrByWidth(type.label, lw - 30), x + 5, ry + 4, owned ? Ui.GOLD : Ui.TEXT, false);
            g.text(font, owned ? "✔" : "✖", x + lw - 12, ry + 4, owned ? 0xFF60E070 : Ui.MUTED, false);
            click(x, ry, lw, rowH - 2, () -> { selected = type; confirm = false; });
        }
        int dx = x + lw + 8, dw = left + pw - 8 - dx, bottom = top + ph - 8;
        Ui.inset(g, dx, y, dw, bottom - y);
        g.text(font, selected.label, dx + 7, y + 6, Ui.GOLD, false);
        if (minecraft.level != null) {
            var entity = previews.computeIfAbsent(selected, t -> {
                var m = new MountEntity(ModEntities.MOUNTS.get(t).get(), minecraft.level);
                m.setId(-200 - t.ordinal());
                m.setTamed(true);
                m.setItemSlot(net.minecraft.world.entity.EquipmentSlot.SADDLE, MountEntity.permanentSaddle());
                return m;
            });
            var state = minecraft.getEntityRenderDispatcher().getRenderer(entity).createRenderState(entity, 1f);
            state.shadowPieces.clear();
            g.entity(state, 34 / Math.max(1f, Math.max(selected.width, selected.height) * .8f), new org.joml.Vector3f(0, selected.height / 2, 0), new org.joml.Quaternionf().rotateZ((float) Math.PI).rotateY(-.7f), null, dx + 5, y + 18, dx + dw - 5, y + 94);
        }
        int ty = y + 100;
        g.text(font, selected.rarity.label + " • Vitesse : " + Math.round(selected.speed / .225 * 100) + " %", dx + 7, ty, selected.rarity.color, false);
        Ui.wrap(g, selected.flies ? "Vole ! Avancez en regardant vers le haut pour décoller, vers le bas pour descendre ; saut = battement d'ailes."
                : "Clic droit pour monter, Maj pour descendre. Selle incluse. Collection permanente.", dx + 7, ty + 14, dw - 14, Ui.MUTED);
        boolean owned = d.mounts.contains(selected.id), active = selected.id.equals(d.activeMount);
        int by = bottom - 23;
        if (owned) {
            button(g, dx + 7, by, dw - 14, 17, active ? "Renvoyer" : "Invoquer", true, 0xFF2A5630, () -> {
                ClientNet.send(new Payloads.MountAction(active ? Payloads.MountAction.DISMISS : Payloads.MountAction.SUMMON, selected.id));
                minecraft.gui.setScreen(null);
            });
        } else {
            Ui.wrap(g, "S'obtient uniquement dans les caisses et les Lucky Blocks (Sceau de monture).", dx + 7, by - 10, dw - 14, Ui.GOLD);
        }
    }

    @Override public boolean mouseScrolled(double x, double y, double sx, double sy) {
        scroll = Math.max(0, scroll - (int) Math.signum(sy));
        return true;
    }
}
