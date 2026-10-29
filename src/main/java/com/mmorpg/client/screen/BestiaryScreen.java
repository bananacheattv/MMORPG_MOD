package com.mmorpg.client.screen;

import com.mmorpg.client.ClientData;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.crafting.ForgeRecipes;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

/** Bestiaire : monstres et boss du monde, leurs niveaux, statistiques, butin et le nombre de victoires. */
public class BestiaryScreen extends MenuScreen {
    @Override protected String backdrop() { return "bestiary_header"; }

    private static String selected;
    private int scroll;
    private int category; // tous, monstres, boss
    private String family = "Toutes";
    private int dropdown;
    private int dropdownScroll;
    private List<String> familyOptions = List.of("Toutes");
    private final java.util.Map<String, net.minecraft.world.entity.LivingEntity> previews = new java.util.HashMap<>();
    private int previewIds;

    public BestiaryScreen() {
        super(Tab.BESTIAIRE);
    }

    public static String displayName(String key) {
        CompoundTag t = ClientData.mobInfo.getCompoundOrEmpty(key);
        String custom = t.getStringOr("name", "");
        return custom.isEmpty() ? Component.translatable("entity.mmorpg." + key).getString() : custom;
    }

    /** Une fiche reste verrouillee tant que le joueur n'a pas vaincu au moins une de ces creatures. */
    public static void select(String key) { selected = key; }

    private static boolean unlocked(String key) {
        return ClientData.DATA.kills.getOrDefault(key, 0) > 0;
    }

    private net.minecraft.world.entity.LivingEntity previewEntity(String key) {
        if (minecraft.level == null) return null;
        return previews.computeIfAbsent(key, k -> {
            var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getValue(com.mmorpg.MMORPG.id(k));
            var e = type == null ? null : type.create(minecraft.level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
            if (e == null) return null;
            e.setId(-400 - previewIds++);
            return e instanceof net.minecraft.world.entity.LivingEntity living ? living : null;
        });
    }

    /** Case 3D : la creature qui tourne doucement si elle est decouverte, sinon une silhouette sombre avec « ? ». */
    private void previewBox(GuiGraphicsExtractor g, int x, int y, int w, int h, String key, boolean open) {
        Ui.inset(g, x, y, w, h);
        var entity = open ? previewEntity(key) : null;
        if (entity == null) {
            g.fill(x + 3, y + 3, x + w - 3, y + h - 3, 0xB0080510);
            Ui.scaledText(g, "?", x + w / 2 - 6, y + h / 2 - 14, 4f, Ui.MUTED, false);
            return;
        }
        entity.tickCount = (int) (minecraft.level.getGameTime() % 100000);
        float height = entity.getBbHeight();
        for (var entry : com.mmorpg.entity.mob.ImportedMobs.ALL)
            if (entry.key().equals(key)) height = entry.height() * entry.scale();   // le rendu des modeles importes applique deja leur echelle
        int size = Math.max(6, Math.min(80, (int) ((h - 14) / Math.max(.5f, height * 1.2f))));
        float[] spin = spinZone("bestiary:" + key, x, y, w, h);
        Ui.entitySpin(g, x + 4, y + 4, x + w - 4, y + h - 4, size, .0625f, spin[0], spin[1], entity);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        if (dropdown != 0) {
            dropdownScroll = Math.max(0, dropdownScroll - (int) Math.signum(sy));
            return true;
        }
        scroll = Math.max(0, scroll - (int) Math.signum(sy));
        return true;
    }

    @Override
    protected void renderTab(GuiGraphicsExtractor g, int mx, int my, float a) {
        List<String> all = new ArrayList<>(ClientData.mobInfo.keySet());
        all.sort(java.util.Comparator.<String>comparingInt(k -> ClientData.mobInfo.getCompoundOrEmpty(k).getIntOr("min", 1))
                .thenComparing(k -> k));                               // pas d'ordre alphabetique : il trahirait les noms verrouilles
        List<String> categoryKeys = all.stream().filter(k -> category == 0 ||
                ClientData.mobInfo.getCompoundOrEmpty(k).getBooleanOr("boss", false) == (category == 2)).toList();
        List<String> families = new ArrayList<>(List.of("Toutes"));
        categoryKeys.stream().map(k -> ClientData.mobInfo.getCompoundOrEmpty(k).getStringOr("family", "Autres"))
                .distinct().sorted().forEach(families::add);
        if (!families.contains(family)) family = "Toutes";
        familyOptions = families;
        List<String> keys = categoryKeys.stream().filter(k -> family.equals("Toutes") ||
                family.equals(ClientData.mobInfo.getCompoundOrEmpty(k).getStringOr("family", "Autres"))).toList();
        int x0 = left + 8;
        int y0 = contentTop;
        int lw = 150;
        Ui.header(g, x0, y0, lw, "BESTIAIRE (" + keys.stream().filter(BestiaryScreen::unlocked).count() + "/" + keys.size() + ")");
        Ui.frame(g, x0, y0 + 14, lw, 16, Ui.GOLD, hovered(x0, y0 + 14, lw, 16));
        g.text(font, "Type : " + new String[]{"Tous", "Monstres", "Boss"}[category] + "  ▼", x0 + 4, y0 + 18, Ui.TEXT, false);
        click(x0, y0 + 14, lw, 16, () -> { dropdown = 1; dropdownScroll = 0; });
        Ui.frame(g, x0, y0 + 32, lw, 16, Ui.GOLD, hovered(x0, y0 + 32, lw, 16));
        g.text(font, font.plainSubstrByWidth("Famille : " + family + "  ▼", lw - 8), x0 + 4, y0 + 36, Ui.TEXT, false);
        click(x0, y0 + 32, lw, 16, () -> { dropdown = 2; dropdownScroll = 0; });
        if (keys.isEmpty()) {
            g.text(font, "Aucune donnée.", x0, y0 + 54, Ui.MUTED, false);
            return;
        }
        if (selected == null || !keys.contains(selected)) selected = keys.get(0);
        int rowH = 16;
        int visible = Math.max(1, (ph - 54 - 60) / rowH);
        scroll = Math.min(scroll, Math.max(0, keys.size() - visible));
        for (int i = 0; i < Math.min(visible, keys.size() - scroll); i++) {
            String k = keys.get(i + scroll);
            CompoundTag t = ClientData.mobInfo.getCompoundOrEmpty(k);
            int ry = y0 + 52 + i * rowH;
            boolean boss = t.getBooleanOr("boss", false);
            boolean sel = k.equals(selected);
            boolean open = unlocked(k);
            Ui.frame(g, x0, ry, lw, rowH - 2, !open ? 0xFF6A6478 : boss ? 0xFFFF5050 : 0xFFC0A060, sel || hovered(x0, ry, lw, rowH - 2));
            String label = open ? (boss ? "☠ " : "") + displayName(k) : "🔒 ???";
            g.text(font, font.plainSubstrByWidth(label, lw - 40), x0 + 4, ry + 3, !open ? Ui.MUTED : boss ? 0xFFFF8080 : Ui.TEXT, false);
            int min = t.getIntOr("min", 1), max = t.getIntOr("max", 1);
            String lv = !open ? "?" : min == max ? String.valueOf(min) : min + "-" + max;
            g.text(font, lv, x0 + lw - 4 - font.width(lv), ry + 3, !open ? Ui.MUTED : Ui.levelColor((min + max) / 2, ClientData.DATA.level), false);
            final String key = k;
            click(x0, ry, lw, rowH - 2, () -> selected = key);
        }

        int dx = x0 + lw + 10;
        int dw = left + pw - 8 - dx;
        int dh = ph - 54 - 10;
        Ui.inset(g, dx, y0, dw, dh);
        CompoundTag t = ClientData.mobInfo.getCompoundOrEmpty(selected);
        boolean boss = t.getBooleanOr("boss", false);
        int boxW = Math.min(130, dw / 3), boxH = Math.min(dh - 16, 170);
        previewBox(g, dx + dw - boxW - 8, y0 + 8, boxW, boxH, selected, unlocked(selected));
        int textW = Math.max(40, dw - 16 - boxW - 8);   // le texte reste a gauche de la case 3D
        if (!unlocked(selected)) {
            Ui.scaledText(g, "???", dx + 8, y0 + 8, 1.5f, Ui.MUTED, false);
            Ui.wrap(g, "Fiche verrouillée. Vainquez cette créature pour découvrir son nom, ses statistiques et son butin.", dx + 8, y0 + 30, textW, Ui.MUTED);
            return;
        }
        String title = displayName(selected);
        float titleScale = Math.min(1.5f, textW / (float) Math.max(1, font.width(title)));
        if (titleScale < 1f) {   // trop long : on reduit puis on tronque
            titleScale = 1f;
            title = font.plainSubstrByWidth(title, textW);
        }
        Ui.scaledText(g, title, dx + 8, y0 + 8, titleScale, boss ? 0xFFFF6060 : Ui.GOLD_LIGHT, false);
        int ty = y0 + 26;
        int min = t.getIntOr("min", 1), max = t.getIntOr("max", 1);
        g.text(font, font.plainSubstrByWidth((boss ? "Boss — " : "Monstre — ") + "Niveau " + (min == max ? min : min + " à " + max), textW), dx + 8, ty, Ui.TEXT, false);
        ty += 12;
        g.text(font, font.plainSubstrByWidth("PV : " + Ui.fmt(t.getFloatOr("hp", 0)) + (min == max ? "" : " → " + Ui.fmt(t.getFloatOr("hpMax", 0))), textW), dx + 8, ty, 0xFFFF7070, false);
        ty += 11;
        g.text(font, font.plainSubstrByWidth("Attaque : " + Ui.fmt(t.getFloatOr("atk", 0)) + (min == max ? "" : " → " + Ui.fmt(t.getFloatOr("atkMax", 0))), textW), dx + 8, ty, 0xFFFFA050, false);
        ty += 11;
        String origin = boss ? "Invocation : Autel d'Invocation" : t.getBooleanOr("spawns", false) ? "Apparaît naturellement dans le monde" : "N'apparaît pas naturellement";
        ty += Ui.wrap(g, origin, dx + 8, ty, textW, Ui.MUTED) + 1;
        int kills = ClientData.DATA.kills.getOrDefault(selected, 0);
        g.text(font, font.plainSubstrByWidth("Vaincus : " + kills, textW), dx + 8, ty, Ui.GREEN, false);
        ty += 16;
        int lootW = dw - 16 - boxW - 8;
        Ui.header(g, dx + 8, ty, lootW, "BUTIN");
        ty += 14;
        ListTag drops = t.getListOrEmpty("drops");
        int col = 0;
        for (int i = 0; i < drops.size(); i++) {
            CompoundTag dt = drops.getCompoundOrEmpty(i);
            Item item = ForgeRecipes.resolveItem(dt.getStringOr("item", "minecraft:air"));
            if (item == null || item == Items.AIR) continue;
            if (ty + 16 > y0 + dh - 2) break;
            int ix = dx + 8 + col * (lootW / 2);
            ItemStack stack = new ItemStack(item);
            g.item(stack, ix, ty);
            float chance = dt.getFloatOr("chance", 0) * 100;
            String txt = (chance >= 100 ? "100" : String.format(java.util.Locale.FRANCE, "%.0f", chance)) + " %";
            g.text(font, ellipsis(stack.getHoverName().getString(), Math.max(8, lootW / 2 - 22)), ix + 18, ty, Ui.TEXT, false);
            g.text(font, txt, ix + 18, ty + 9, Ui.MUTED, false);
            if (hovered(ix, ty, 16, 16)) g.setTooltipForNextFrame(font, stack, mx, my);
            col++;
            if (col >= 2) {
                col = 0;
                ty += 20;
            }
            if (ty > y0 + dh - 20) break;
        }
    }

    /** Coupe le texte avec « … » pour qu'il tienne dans la largeur donnee. */
    private String ellipsis(String text, int width) {
        if (font.width(text) <= width) return text;
        return font.plainSubstrByWidth(text, Math.max(0, width - font.width("…"))) + "…";
    }

    @Override protected void renderOverlay(GuiGraphicsExtractor g, int mx, int my, float a) {
        if (dropdown == 0) return;
        List<String> options = dropdown == 1 ? List.of("Tous", "Monstres", "Boss") : familyOptions;
        int x = left + 8, y = contentTop + (dropdown == 1 ? 14 : 32), w = 150;
        int rows = Math.min(options.size(), Math.max(1, (top + ph - y - 12) / 18));
        dropdownScroll = Math.min(dropdownScroll, Math.max(0, options.size() - rows));
        hits.clear();
        click(0, 0, width, height, () -> dropdown = 0);
        g.fill(x - 1, y - 1, x + w + 1, y + rows * 18 + 1, 0xFF15111E);
        Ui.frame(g, x - 1, y - 1, w + 2, rows * 18 + 2, Ui.GOLD, false);
        for (int i = 0; i < rows; i++) {
            final int index = dropdownScroll + i;
            String option = options.get(index);
            boolean chosen = dropdown == 1 ? category == index : family.equals(option);
            button(g, x, y + i * 18, w, 18, (chosen ? "✓ " : "") + option, true,
                    chosen ? 0xFF665020 : 0xFF282230, () -> {
                        if (dropdown == 1) { category = index; family = "Toutes"; }
                        else family = option;
                        dropdown = 0; scroll = 0;
                    });
        }
    }
}
