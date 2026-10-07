package com.mmorpg.client.screen;

import com.mmorpg.client.ClientData;
import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.mount.MountType;
import com.mmorpg.network.Payloads;
import com.mmorpg.entity.MountEntity;
import com.mmorpg.registry.ModEntities;
import net.minecraft.core.component.DataComponents;
import net.minecraft.client.gui.GuiGraphicsExtractor;

public class MountsScreen extends MenuScreen {
    private MountType selected = MountType.VOYAGEUR;
    private boolean confirm;
    private final java.util.Map<MountType, MountEntity> previews = new java.util.EnumMap<>(MountType.class);
    public MountsScreen() { super(Tab.FAMILIERS); }
    @Override protected void renderTab(GuiGraphicsExtractor g, int mx, int my, float a) {
        var d = ClientData.DATA;
        int x = left + 8, y = contentTop, lw = 165;
        Ui.header(g, x, y, lw, "MONTURES (" + d.mounts.size() + "/3)");
        int index = 0;
        for (var type : MountType.values()) {
            int ry = y + 18 + index++ * 40;
            boolean owned = d.mounts.contains(type.id);
            Ui.frame(g, x, ry, lw, 36, owned ? Ui.GOLD : Ui.MUTED, type == selected);
            g.text(font, type.label, x + 5, ry + 5, owned ? Ui.GOLD : Ui.TEXT, false);
            g.text(font, owned ? "Débloquée" : "Caisses / Lucky Blocks", x + 5, ry + 20, Ui.MUTED, false);
            click(x, ry, lw, 36, () -> { selected = type; confirm = false; });
        }
        button(g, x, top + ph - 25, lw, 17, "< Familiers", true, 0xFF2A2420, () -> minecraft.gui.setScreen(new PetsScreen()));
        int dx = x + lw + 8, dw = left + pw - 8 - dx, bottom = top + ph - 8;
        Ui.inset(g, dx, y, dw, bottom - y);
        g.text(font, selected.label, dx + 7, y + 6, Ui.GOLD, false);
        if (minecraft.level != null) {
            var entity = previews.computeIfAbsent(selected, t -> {
                var m = new MountEntity(ModEntities.MOUNT.get(), minecraft.level);
                m.setId(-200 - t.ordinal());
                m.setComponent(DataComponents.HORSE_VARIANT, t.variant);
                m.setTamed(true);
                m.setItemSlot(net.minecraft.world.entity.EquipmentSlot.SADDLE, MountEntity.permanentSaddle());
                return m;
            });
            var state = minecraft.getEntityRenderDispatcher().getRenderer(entity).createRenderState(entity, 1f);
            state.shadowPieces.clear();
            g.entity(state, 34, new org.joml.Vector3f(0, .8f, 0), new org.joml.Quaternionf().rotateZ((float) Math.PI).rotateY(-.7f), null, dx + 5, y + 18, dx + dw - 5, y + 94);
        }
        int ty = y + 100;
        g.text(font, "Vitesse : " + Math.round(selected.speed / .225 * 100) + " % du cheval standard", dx + 7, ty, Ui.TEXT, false);
        Ui.wrap(g, "Clic droit pour monter, Maj pour descendre. Selle incluse. Collection permanente.", dx + 7, ty + 14, dw - 14, Ui.MUTED);
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
}
