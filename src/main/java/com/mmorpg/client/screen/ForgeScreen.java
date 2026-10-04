package com.mmorpg.client.screen;

import com.mmorpg.client.ClientData;
import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.crafting.ForgeRecipes;
import com.mmorpg.item.ItemTooltips;
import com.mmorpg.item.RpgEquipment;
import com.mmorpg.item.RpgMaterialItem;
import com.mmorpg.network.Payloads;
import com.mmorpg.registry.ModItems;
import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.rpg.Rarity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Forge Arcanique : fabrication (recettes par categorie) et amelioration des equipements (+1 a +20). */
public class ForgeScreen extends MmoScreen {
    private static int tab = 0;
    private static boolean classOnly = true;
    private static String selectedRecipe;
    private final BlockPos pos;
    private EditBox search;
    private int scroll;
    private int upgradeSlot = -1;
    /** Utiliser le trefle du palier (reussite garantie) lors de la prochaine amelioration. */
    private static boolean useClover = true;

    public ForgeScreen(BlockPos pos) {
        super("Forge Arcanique");
        this.pos = pos;
    }

    @Override
    protected void init() {
        super.init();
        search = new EditBox(this.font, left + 10, top + 52, 150, 14, Component.literal("Recherche"));
        search.setHint(Component.literal("Rechercher...").withColor(0xFF807868));
        search.setMaxLength(40);
        search.setResponder(s -> scroll = 0);
        this.addRenderableWidget(search);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double sx, double sy) {
        scroll = Math.max(0, scroll - (int) Math.signum(sy));
        return true;
    }

    private static int count(Inventory inv, Item item) {
        int n = 0;
        for (ItemStack s : inv) {
            if (s.is(item) && RpgEquipment.upgrade(s) == 0) n += s.getCount();
        }
        return n;
    }

    private static Rarity rarityOf(Item item) {
        if (item instanceof RpgEquipment eq) return eq.rpgDef().rarity();
        if (item instanceof RpgMaterialItem m) return m.rarity();
        return Rarity.COMMUN;
    }

    private static boolean canCraft(Inventory inv, ForgeRecipes.Recipe r) {
        if (ClientData.DATA.level < r.level()) return false;
        if (ClientData.DATA.gold < r.gold()) return false;
        for (ForgeRecipes.Ingredient ing : r.ingredients()) {
            if (count(inv, ing.resolve()) < ing.count()) return false;
        }
        return true;
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        Ui.panel(g, left, top, pw, ph);
        Ui.title(g, left + pw / 2, top + 8, "FORGE ARCANIQUE", Ui.GOLD_LIGHT);
        ForgeRecipes.Category[] cats = ForgeRecipes.Category.values();
        int tabs = cats.length + 1;
        int tw = (pw - 16) / tabs;
        for (int i = 0; i < tabs; i++) {
            final int t = i;
            String label = i < cats.length ? cats[i].label : "Amélioration";
            button(g, left + 8 + i * tw, top + 24, tw - 3, 16, label, true, tab == i ? 0xFF6A4A1A : 0xFF2A2420, () -> {
                tab = t;
                scroll = 0;
            });
        }
        Inventory inv = Minecraft.getInstance().player.getInventory();
        long gold = ClientData.DATA.gold;
        String goldTxt = "Or : " + gold;
        g.item(new ItemStack(ModItems.PIECE_OR.get()), left + pw - 24 - font.width(goldTxt), top + 4);
        g.text(font, goldTxt, left + pw - 8 - font.width(goldTxt), top + 9, 0xFFFFD040, true);
        search.visible = tab < cats.length;
        if (tab < cats.length) {
            renderRecipes(g, inv, cats[tab]);
        } else {
            renderUpgrade(g, inv);
        }
    }

    // ================================================================== fabrication

    private void renderRecipes(GuiGraphicsExtractor g, Inventory inv, ForgeRecipes.Category cat) {
        int x0 = left + 8;
        int listTop = top + 70;
        int lw = 156;
        String filterLabel = classOnly ? "Ma classe" : "Toutes";
        button(g, left + 166, top + 51, 54, 16, filterLabel, true, classOnly ? 0xFF3A5A2A : 0xFF2A2420, () -> classOnly = !classOnly);
        List<ForgeRecipes.Recipe> list = new ArrayList<>();
        String q = search.getValue().toLowerCase(Locale.ROOT).strip();
        PlayerClass cls = ClientData.DATA.playerClass;
        for (ForgeRecipes.Recipe r : ForgeRecipes.byCategory(cat)) {
            if (classOnly && r.playerClass() != PlayerClass.NONE && r.playerClass() != cls) continue;
            Item item = r.resultItem();
            String name = new ItemStack(item).getHoverName().getString().toLowerCase(Locale.ROOT);
            if (!q.isEmpty() && !name.contains(q)) continue;
            list.add(r);
        }
        int rowH = 20;
        int visible = (top + ph - 10 - listTop) / rowH;
        scroll = Math.min(scroll, Math.max(0, list.size() - visible));
        if (selectedRecipe == null || list.stream().noneMatch(r -> r.id().equals(selectedRecipe))) {
            selectedRecipe = list.isEmpty() ? null : list.get(0).id();
        }
        for (int i = 0; i < Math.min(visible, list.size() - scroll); i++) {
            ForgeRecipes.Recipe r = list.get(i + scroll);
            int ry = listTop + i * rowH;
            Item item = r.resultItem();
            Rarity rar = rarityOf(item);
            boolean sel = r.id().equals(selectedRecipe);
            Ui.frame(g, x0, ry, lw + 56, rowH - 2, rar.color, sel || hovered(x0, ry, lw + 56, rowH - 2));
            g.item(new ItemStack(item, r.count()), x0 + 2, ry + 1);
            String name = new ItemStack(item).getHoverName().getString();
            g.text(font, font.plainSubstrByWidth(name, lw + 20), x0 + 21, ry + 5, rar.color, false);
            boolean ok = canCraft(inv, r);
            Ui.diamond(g, x0 + lw + 50, ry + 8, 2, ok ? Ui.GREEN : 0xFF5A3A3A);
            String lv = String.valueOf(r.level());
            g.text(font, lv, x0 + lw + 44 - font.width(lv), ry + 5, ClientData.DATA.level >= r.level() ? Ui.MUTED : Ui.RED, false);
            final String id = r.id();
            click(x0, ry, lw + 56, rowH - 2, () -> selectedRecipe = id);
        }
        if (list.isEmpty()) {
            g.text(font, "Aucune recette.", x0 + 4, listTop + 4, Ui.MUTED, false);
        }

        // details
        int dx = left + 232;
        int dw = left + pw - 8 - dx;
        int dy = top + 46;
        int dh = ph - 46 - 8;
        Ui.inset(g, dx, dy, dw, dh);
        ForgeRecipes.Recipe r = ForgeRecipes.get(selectedRecipe);
        if (r == null) return;
        Item item = r.resultItem();
        ItemStack stack = new ItemStack(item, r.count());
        g.pose().pushMatrix();
        g.pose().translate(dx + 6, dy + 6);
        g.pose().scale(2f, 2f);
        g.item(stack, 0, 0);
        g.pose().popMatrix();
        if (hovered(dx + 6, dy + 6, 32, 32)) g.setTooltipForNextFrame(font, stack, mouseX, mouseY);
        Rarity rar = rarityOf(item);
        g.text(font, font.plainSubstrByWidth(stack.getHoverName().getString() + (r.count() > 1 ? " x" + r.count() : ""), dw - 48), dx + 42, dy + 8, rar.color, true);
        g.text(font, rar.label + " — Niveau " + r.level(), dx + 42, dy + 19, ClientData.DATA.level >= r.level() ? Ui.MUTED : Ui.RED, false);
        int ty = dy + 42;
        if (item instanceof RpgEquipment eq) {
            List<Component> stats = new ArrayList<>();
            ItemTooltips.statLines(eq.rpgDef().stats(), 1.0, stats::add);
            for (int i = 0; i < Math.min(5, stats.size()); i++) {
                g.text(font, stats.get(i), dx + 6, ty, 0xFFFFFFFF, false);
                ty += 9;
            }
            if (stats.size() > 5) {
                g.text(font, "…", dx + 6, ty, Ui.MUTED, false);
                ty += 9;
            }
            ty += 3;
        }
        Ui.header(g, dx + 6, ty, dw - 12, "MATÉRIAUX");
        ty += 14;
        for (ForgeRecipes.Ingredient ing : r.ingredients()) {
            Item it = ing.resolve();
            int have = count(inv, it);
            ItemStack is = new ItemStack(it);
            g.item(is, dx + 6, ty - 4);
            g.text(font, font.plainSubstrByWidth(is.getHoverName().getString(), dw - 70), dx + 26, ty, Ui.TEXT, false);
            String c = have + "/" + ing.count();
            g.text(font, c, dx + dw - 6 - font.width(c), ty, have >= ing.count() ? Ui.GREEN : Ui.RED, true);
            if (hovered(dx + 6, ty - 4, 16, 16)) g.setTooltipForNextFrame(font, is, mouseX, mouseY);
            ty += 17;
        }
        if (r.gold() > 0) {
            g.item(new ItemStack(ModItems.PIECE_OR.get()), dx + 6, ty - 4);
            g.text(font, "Pièces d'or", dx + 26, ty, Ui.TEXT, false);
            long have = ClientData.DATA.gold;
            String c = have + "/" + r.gold();
            g.text(font, c, dx + dw - 6 - font.width(c), ty, have >= r.gold() ? Ui.GREEN : Ui.RED, true);
        }
        boolean ok = canCraft(inv, r);
        int by = dy + dh - 22;
        button(g, dx + 6, by, dw - 62, 16, "Fabriquer", ok, 0xFF2A6A3A,
                () -> ClientNet.send(new Payloads.ForgeAction(Payloads.ForgeAction.CRAFT, r.id(), 1, pos)));
        button(g, dx + dw - 52, by, 46, 16, "x5", ok, 0xFF2A5A4A,
                () -> ClientNet.send(new Payloads.ForgeAction(Payloads.ForgeAction.CRAFT, r.id(), 5, pos)));
    }

    // ================================================================== amelioration

    private void renderUpgrade(GuiGraphicsExtractor g, Inventory inv) {
        int x0 = left + 8;
        int listTop = top + 48;
        int lw = 212;
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            if (inv.getItem(i).getItem() instanceof RpgEquipment) slots.add(i);
        }
        g.text(font, "Équipements de votre inventaire :", x0, listTop - 2, Ui.GOLD, false);
        int rowH = 20;
        int visible = (top + ph - 10 - listTop - 10) / rowH;
        scroll = Math.min(scroll, Math.max(0, slots.size() - visible));
        if (!slots.contains(upgradeSlot)) upgradeSlot = slots.isEmpty() ? -1 : slots.get(0);
        for (int i = 0; i < Math.min(visible, slots.size() - scroll); i++) {
            int slot = slots.get(i + scroll);
            ItemStack s = inv.getItem(slot);
            RpgEquipment eq = (RpgEquipment) s.getItem();
            int ry = listTop + 10 + i * rowH;
            Ui.frame(g, x0, ry, lw, rowH - 2, eq.rpgDef().rarity().color, slot == upgradeSlot || hovered(x0, ry, lw, rowH - 2));
            g.item(s, x0 + 2, ry + 1);
            g.text(font, font.plainSubstrByWidth(s.getHoverName().getString(), lw - 30), x0 + 21, ry + 5, eq.rpgDef().rarity().color, false);
            click(x0, ry, lw, rowH - 2, () -> upgradeSlot = slot);
        }
        if (slots.isEmpty()) g.text(font, "Aucun équipement RPG dans l'inventaire.", x0, listTop + 14, Ui.MUTED, false);

        int dx = left + 232;
        int dw = left + pw - 8 - dx;
        int dy = top + 46;
        int dh = ph - 46 - 8;
        Ui.inset(g, dx, dy, dw, dh);
        if (upgradeSlot < 0) return;
        ItemStack s = inv.getItem(upgradeSlot);
        if (!(s.getItem() instanceof RpgEquipment eq)) return;
        int cur = RpgEquipment.upgrade(s);
        g.pose().pushMatrix();
        g.pose().translate(dx + 6, dy + 6);
        g.pose().scale(2f, 2f);
        g.item(s, 0, 0);
        g.pose().popMatrix();
        if (hovered(dx + 6, dy + 6, 32, 32)) g.setTooltipForNextFrame(font, s, mouseX, mouseY);
        g.text(font, font.plainSubstrByWidth(s.getHoverName().getString(), dw - 48), dx + 42, dy + 8, eq.rpgDef().rarity().color, true);
        int ty = dy + 44;
        if (cur >= RpgEquipment.MAX_UPGRADE) {
            g.text(font, "Amélioration maximale atteinte (+" + RpgEquipment.MAX_UPGRADE + ").", dx + 6, ty, Ui.GOLD, false);
            return;
        }
        int target = cur + 1;
        Ui.scaledText(g, "+" + cur + "  →  +" + target, dx + dw / 2f, dy + 22, 1.4f, 0xFFFFD040, true);
        g.text(font, "Statistiques : +" + Math.round(cur * RpgEquipment.UPGRADE_BONUS * 100) + " % → +" + Math.round(target * RpgEquipment.UPGRADE_BONUS * 100) + " %", dx + 6, ty, Ui.TEXT, false);
        ty += 12;
        // trefle du palier vise (+1 a +4, +5 a +8...) : s'il est utilise, la reussite est garantie
        Item cloverItem = ModItems.get(ForgeRecipes.cloverFor(target));
        int haveClover = count(inv, cloverItem);
        boolean withClover = useClover && haveClover > 0 && ForgeRecipes.UPGRADE_CHANCE[target - 1] < 100;   // inutile a 100 %
        String cloverId = withClover ? ForgeRecipes.cloverFor(target) : "";
        int chance = withClover ? 100 : ForgeRecipes.UPGRADE_CHANCE[target - 1];
        g.text(font, "Chance de réussite : " + chance + " %" + (withClover ? " (trèfle)" : ""), dx + 6, ty,
                chance >= 70 ? Ui.GREEN : chance >= 40 ? 0xFFFFC040 : Ui.RED, true);
        ty += 14;
        Item stone = ForgeRecipes.upgradeNeedsSuperior(target) ? ModItems.PIERRE_AMELIORATION_SUP.get() : ModItems.PIERRE_AMELIORATION.get();
        int haveStone = count(inv, stone);
        ItemStack stoneStack = new ItemStack(stone);
        g.item(stoneStack, dx + 6, ty - 4);
        g.text(font, font.plainSubstrByWidth(stoneStack.getHoverName().getString(), dw - 70), dx + 26, ty, Ui.TEXT, false);
        g.text(font, haveStone + "/1", dx + dw - 6 - font.width(haveStone + "/1"), ty, haveStone >= 1 ? Ui.GREEN : Ui.RED, true);
        ty += 18;
        int goldCost = ForgeRecipes.upgradeGold(target);
        long haveGold = ClientData.DATA.gold;
        g.item(new ItemStack(ModItems.PIECE_OR.get()), dx + 6, ty - 4);
        g.text(font, "Pièces d'or", dx + 26, ty, Ui.TEXT, false);
        String gc = haveGold + "/" + goldCost;
        g.text(font, gc, dx + dw - 6 - font.width(gc), ty, haveGold >= goldCost ? Ui.GREEN : Ui.RED, true);
        ty += 18;
        int[] range = ForgeRecipes.cloverRange(ForgeRecipes.cloverFor(target));
        ItemStack cloverStack = new ItemStack(cloverItem);
        g.item(cloverStack, dx + 6, ty - 4);
        String n = haveClover + "";
        g.text(font, font.plainSubstrByWidth(cloverStack.getHoverName().getString(), dw - 92 - font.width(n)), dx + 26, ty,
                withClover ? Ui.GREEN : Ui.MUTED, false);
        g.text(font, n, dx + dw - 56 - font.width(n), ty, haveClover > 0 ? Ui.TEXT : Ui.RED, true);
        button(g, dx + dw - 50, ty - 4, 44, 14, useClover ? "Utilisé" : "Non", haveClover > 0, useClover ? 0xFF3A6A2A : 0xFF4A3A2A,
                () -> useClover = !useClover);
        if (hovered(dx + 6, ty - 4, dw - 58, 16)) {
            g.setTooltipForNextFrame(font, Component.literal(cloverStack.getHoverName().getString() + " : réussite garantie de +" + range[0]
                    + " à +" + range[1] + " (consommé)."), mouseX, mouseY);
        }
        ty += 18;
        if (withClover) {
            Ui.wrap(g, "Réussite garantie par le trèfle (palier +" + range[0] + " à +" + range[1] + ").", dx + 6, ty, dw - 12, Ui.GREEN);
        } else if (cur >= 5) {
            Ui.wrap(g, "Attention : en cas d'échec, l'objet perd un niveau d'amélioration.", dx + 6, ty, dw - 12, 0xFFFF8060);
        } else {
            Ui.wrap(g, "En cas d'échec, seule la pierre est perdue.", dx + 6, ty, dw - 12, Ui.MUTED);
        }
        boolean ok = haveStone >= 1 && haveGold >= goldCost;
        final int slot = upgradeSlot;
        button(g, dx + 6, dy + dh - 22, dw - 12, 16, "Améliorer", ok, 0xFF7A5A1A,
                () -> ClientNet.send(new Payloads.ForgeAction(Payloads.ForgeAction.UPGRADE, cloverId, slot, pos)));
    }
}
