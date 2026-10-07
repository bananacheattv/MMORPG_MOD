package com.mmorpg.client.screen;

import com.mmorpg.client.ClientData;
import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.item.ItemTooltips;
import com.mmorpg.network.Payloads;
import com.mmorpg.pet.PetType;
import com.mmorpg.rpg.PlayerData;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/** Collection de familiers : invocation, niveau, experience et bonus. */
public class PetsScreen extends MenuScreen {
    private static PetType selected = PetType.FEU_FOLLET;

    private final java.util.Map<PetType, com.mmorpg.entity.PetEntity> previews = new java.util.EnumMap<>(PetType.class);

    private void preview(GuiGraphicsExtractor g, PetType type, int x, int y, int size) {
        if (minecraft.level == null) return;
        var entity = previews.computeIfAbsent(type, t -> {
            var pet = new com.mmorpg.entity.PetEntity(com.mmorpg.registry.ModEntities.PET.get(), minecraft.level);
            pet.displayOnly(t, 155);
            return pet;
        });
        entity.tickCount = (int) (minecraft.level.getGameTime() % 100000);
        var renderer = minecraft.getEntityRenderDispatcher().getRenderer(entity);
        var state = renderer.createRenderState(entity, 1f);
        state.shadowPieces.clear();
        state.outlineColor = 0;
        float scale = size * (type == PetType.PHENIX ? .55f : type == PetType.DRAGONNET ? .60f : type == PetType.FEE || type == PetType.LOUP_SPECTRAL ? .65f : .85f);
        float center = type == PetType.DRAGONNET ? .65f : .48f;
        g.entity(state, scale, new org.joml.Vector3f(0, center, 0),
                new org.joml.Quaternionf().rotateZ((float) Math.PI), null, x, y, x + size, y + size);
    }

    public PetsScreen() {
        super(Tab.FAMILIERS);
    }

    @Override
    protected void renderTab(GuiGraphicsExtractor g, int mx, int my, float a) {
        PlayerData d = ClientData.DATA;
        button(g, left + 8, top + ph - 25, 196, 17, "Collection de montures >", true, 0xFF4A3820, () -> minecraft.gui.setScreen(new MountsScreen()));
        int x0 = left + 8;
        int y0 = contentTop;
        int owned = 0;
        for (PetType p : PetType.values()) if (d.petXp.containsKey(p.id)) owned++;
        Ui.header(g, x0, y0, 196, "COLLECTION (" + owned + "/" + PetType.values().length + ")");
        int cell = 46;
        PetType[] pets = PetType.values();
        for (int i = 0; i < pets.length; i++) {
            PetType p = pets[i];
            int cx = x0 + (i % 4) * (cell + 4);
            int cy = y0 + 14 + (i / 4) * (cell + 18);
            boolean has = d.petXp.containsKey(p.id);
            boolean active = p.id.equals(d.activePet);
            Ui.frame(g, cx, cy, cell, cell + 14, p.rarity.color, p == selected || hovered(cx, cy, cell, cell + 14));
            if (active) Ui.border(g, cx + 1, cy + 1, cell - 2, cell + 12, 0xFF60FF60);
            preview(g, p, cx + 3, cy + 2, cell - 6);
            if (hovered(cx, cy, cell, cell + 14)) tooltip(g, List.of(Component.literal(p.label).withColor(p.rarity.color)));
            if (!has) {
                g.fill(cx + 1, cy + 1, cx + cell - 1, cy + cell + 13, 0xC0000000);
                g.centeredText(font, Component.literal("?"), cx + cell / 2, cy + 16, Ui.MUTED);
            } else {
                String lv = "Niv. " + d.petLevel(p.id);
                g.centeredText(font, Component.literal(lv), cx + cell / 2, cy + cell + 2, Ui.TEXT);
            }
            final PetType pt = p;
            click(cx, cy, cell, cell + 14, () -> selected = pt);
        }
        g.text(font, "Obtenez des œufs sur les boss", x0, y0 + 14 + 2 * (cell + 18) + 4, Ui.MUTED, false);
        g.text(font, "ou fabriquez-les à la Forge.", x0, y0 + 14 + 2 * (cell + 18) + 14, Ui.MUTED, false);

        // ------------------------------------------------ details
        int dx = left + 214;
        int dw = left + pw - 8 - dx;
        int dh = ph - 30 - 10;
        Ui.inset(g, dx, y0, dw, dh);
        PetType p = selected;
        boolean has = d.petXp.containsKey(p.id);
        preview(g, p, dx + 6, y0 + 4, 50);
        g.text(font, p.label, dx + 62, y0 + 10, p.rarity.color, true);
        g.text(font, p.rarity.label, dx + 62, y0 + 21, Ui.MUTED, false);
        int ty = y0 + 60;
        ty += Ui.wrap(g, p.description, dx + 8, ty, dw - 16, Ui.TEXT) + 4;
        int level = has ? d.petLevel(p.id) : 1;
        if (has) {
            int xp = d.petXp.getOrDefault(p.id, 0);
            int cur = PetType.xpForLevel(level);
            int next = level >= PetType.MAX_LEVEL ? cur : PetType.xpForLevel(level + 1);
            float frac = level >= PetType.MAX_LEVEL ? 1 : (xp - cur) / (float) Math.max(1, next - cur);
            g.text(font, "Niveau " + level + " / " + PetType.MAX_LEVEL, dx + 8, ty, Ui.GOLD, true);
            ty += 11;
            Ui.bar(g, dx + 8, ty, dw - 16, 8, frac, 0xFFC080FF, 0xFF603090, level >= PetType.MAX_LEVEL ? "MAX" : (xp - cur) + " / " + (next - cur));
            ty += 14;
        }
        g.text(font, "Bonus adaptés : " + d.playerClass.label, dx + 8, ty, Ui.GOLD, false);
        ty += 11;
        List<Component> lines = new ArrayList<>();
        ItemTooltips.statLines(p.bonusAt(level, d.playerClass), d.petBonusMultiplier(p.id), lines::add);
        if (hovered(dx + 4, ty - 12, dw - 8, Math.max(12, y0 + dh - 60 - ty))) tooltip(g, lines);
        for (Component c : lines) {
            if (ty > y0 + dh - 60) break;
            g.text(font, c, dx + 8, ty, 0xFFFFFFFF, false);
            ty += 10;
        }
        if (has) {
            Ui.bar(g, dx + 8, y0 + dh - 48, dw - 16, 8, d.happiness(p.id) / 100f,
                    0xFF70CC60, 0xFF306030, "Bonheur : " + d.happiness(p.id) + " / 100");
            g.text(font, "Nourriture : clic droit en main", dx + 8, y0 + dh - 37, Ui.MUTED, false);
        }
        int by = y0 + dh - 22;
        if (has) {
            boolean active = p.id.equals(d.activePet);
            button(g, dx + 8, by, dw - 16, 16, active ? "Renvoyer le familier" : "Invoquer", true, active ? 0xFF6A2A2A : 0xFF2A6A3A,
                    () -> ClientNet.send(new Payloads.PetAction(active ? "" : p.id)));
        } else {
            g.centeredText(font, Component.literal("Familier non obtenu"), dx + dw / 2, by + 4, Ui.RED);
        }
    }
}
