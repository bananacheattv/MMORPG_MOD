package com.mmorpg.client.screen;

import com.mmorpg.client.ClientData;
import com.mmorpg.client.ClientNet;
import com.mmorpg.client.Keys;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.network.Payloads;
import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.rpg.Stat;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.client.gui.screens.inventory.EffectsInInventory;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.inventory.Slot;

import java.util.ArrayList;
import java.util.List;

/**
 * Onglet Personnage du menu, qui remplace aussi l'inventaire de Minecraft (touche E) : un seul ecran avec
 * l'equipement, le sac et la barre rapide (emplacements de Minecraft), le portrait, les attributs
 * (repartition des points), les statistiques et le chemin d'evolution de la classe.
 */
public class CharacterScreen extends AbstractContainerScreen<InventoryMenu> {
    private static final String[] ATTR_DESC = {
            "Augmente l'Attaque (+1,4 ; +0,6 pour l'Archer), les PV (+2), la Défense (+0,3) et les dégâts critiques.",
            "Augmente l'Attaque (+0,6 ; +1,3 pour l'Archer), le critique (+0,06 %), l'esquive (+0,07 %), la vitesse et la vitesse d'attaque.",
            "Augmente la Puissance magique (+2), le Mana (+6) et la régénération de mana.",
            "Augmente fortement les PV (+12), la Défense (+0,8) et la régénération de vie.",
            "Augmente le Mana (+4), la Puissance magique (+0,5), la régénération de mana (+0,12/s) et réduit les recharges (+0,03 %).",
            "Attribut du Guerrier : augmente l'Attaque (+0,5), les dégâts critiques (+0,5 %) et le vol de vie (+0,02 %).",
            "Attribut de l'Archer : augmente le critique (+0,08 %), la vitesse d'attaque (+0,03 %) et l'Attaque (+0,4).",
            "Attribut du Tank : augmente la Défense (+1), les PV (+5) et la régénération de vie (+0,08/s)."
    };

    private record Hit(int x, int y, int w, int h, Runnable action) {
    }

    private final EffectsInInventory effects;
    private final List<Hit> hits = new ArrayList<>();
    private int px, py, pw, ph;
    private int mx, my;

    public CharacterScreen(Player player) {
        super(player.inventoryMenu, player.getInventory(), Component.literal("Personnage"), 440, 272);
        this.effects = new EffectsInInventory(this);
        this.titleLabelX = 98;
        this.titleLabelY = 7;
    }

    private boolean creative() {
        return this.minecraft.player.hasInfiniteMaterials();
    }

    private void openCreative() {
        this.minecraft.gui.setScreen(new CreativeModeInventoryScreen(this.minecraft.player, this.minecraft.player.connection.enabledFeatures(),
                this.minecraft.options.operatorItemsTab().get()));
    }

    @Override
    protected void init() {
        if (creative()) {
            openCreative();
            return;
        }
        com.mmorpg.server.InventoryRules.apply(this.minecraft.player);
        super.init();
        // meme panneau que les autres onglets ; les emplacements de Minecraft sont places sous les onglets, a gauche
        this.pw = Math.min(this.width - 12, 440);
        this.ph = Math.min(this.height - 12, 272);
        this.px = (this.width - pw) / 2;
        this.py = (this.height - ph) / 2;
        this.leftPos = px + 8;
        this.topPos = py + 30;
    }

    @Override
    public void containerTick() {
        super.containerTick();
        if (creative()) openCreative();
    }

    @Override
    public boolean showsActiveEffects() {
        return this.effects.canSeeEffects();
    }

    /** Cliquer dans le panneau (hors emplacements) ne jette pas l'objet tenu. */
    @Override
    protected boolean hasClickedOutside(double x, double y, int xo, int yo) {
        return !Ui.in(x, y, px, py, pw, ph);
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        this.mx = mouseX;
        this.my = mouseY;
        this.effects.extractRenderState(g, mouseX, mouseY);
        super.extractRenderState(g, mouseX, mouseY, a);
    }

    private boolean hovered(int x, int y, int w, int h) {
        return Ui.in(mx, my, x, y, w, h);
    }

    private void button(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, int accent, Runnable action) {
        Ui.button(g, x, y, w, h, label, true, mx, my, accent);
        hits.add(new Hit(x, y, w, h, action));
    }

    private static void slotBox(GuiGraphicsExtractor g, int x, int y, int border, boolean big) {
        int s = big ? 22 : 18, o = big ? 2 : 0;
        g.fill(x - o, y - o, x - o + s, y - o + s, 0xE0100C14);
        Ui.border(g, x - o, y - o, s, s, border);
        g.fill(x - o + 1, y - o + s - 2, x - o + s - 1, y - o + s - 1, 0x30FFFFFF);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        g.fillGradient(0, 0, this.width, this.height, 0xA0080610, 0xD0080610);
        hits.clear();
        PlayerData d = ClientData.DATA;
        PlayerClass cls = d.playerClass;
        Ui.panel(g, px, py, pw, ph);
        MenuScreen.drawTabs(g, this.font, px, py, pw, MenuScreen.Tab.PERSONNAGE, mouseX, mouseY,
                (r, action) -> hits.add(new Hit(r[0], r[1], r[2], r[3], action)));
        g.fill(px + 6, py + 24, px + pw - 6, py + 25, Ui.GOLD_DARK);

        // ------------------------------------------------ inventaire et equipement (emplacements de Minecraft)
        int x = this.leftPos, y = this.topPos;
        Ui.inset(g, x - 2, y - 2, 180, 170);
        Ui.inset(g, x + 25, y + 7, 51, 72);
        InventoryScreen.extractEntityInInventoryFollowsMouse(g, x + 26, y + 8, x + 75, y + 78, 30, 0.0625F, mouseX, mouseY, this.minecraft.player);
        Ui.icon(g, Ui.classIcon(cls.id(), false), x + 26, y + 8, 12, 32);
        for (Slot s : this.menu.slots) {
            if (!s.isActive()) continue;
            boolean equipment = (s.index >= InventoryMenu.ARMOR_SLOT_START && s.index < InventoryMenu.ARMOR_SLOT_END) || s.index == InventoryMenu.SHIELD_SLOT;
            boolean result = s.index == InventoryMenu.RESULT_SLOT;
            slotBox(g, x + s.x - 1, y + s.y - 1, equipment ? Ui.GOLD_DARK : result ? Ui.GOLD : 0xFF3A2E22, result);
        }
        g.text(this.font, "Rendez-vous", x + 98, y + 20, Ui.MUTED, false);
        g.text(this.font, "à la Forge", x + 98, y + 32, Ui.GOLD, false);
        g.fill(x + 8, y + 139, x + 168, y + 140, 0x60B08A40);

        // ------------------------------------------------ identite, barres et evolution (sous l'inventaire, selon la place)
        int under = y + 170;
        int bottom = py + ph - 6;
        if (bottom - under >= 26) {
            String lvl = "Niv. " + d.level;
            boolean roomy = bottom - under >= 38;           // classe sur sa propre ligne si la place le permet
            String name = this.minecraft.player.getName().getString();
            String evo = cls.evolutions[PlayerClass.tier(d.level)];
            g.text(this.font, lvl, x + 176 - this.font.width(lvl), under, Ui.TEXT, true);
            if (roomy) {
                g.text(this.font, this.font.plainSubstrByWidth(name, 176 - 6 - this.font.width(lvl)), x, under, Ui.GOLD_LIGHT, true);
                g.text(this.font, this.font.plainSubstrByWidth(evo, 176), x, under + 10, cls.color, false);
            } else {
                g.text(this.font, this.font.plainSubstrByWidth(name + "  ", 70), x, under, Ui.GOLD_LIGHT, true);
                g.text(this.font, this.font.plainSubstrByWidth(evo, 176 - 76 - this.font.width(lvl)), x + 72, under, cls.color, false);
            }
            int by = under + (roomy ? 21 : 11);
            float xp = d.level >= 100 ? 1 : d.xp / (float) Math.max(1, ClientData.xpToNext);
            Ui.bar(g, x, by, 176, 4, xp, 0xFFFFD860, 0xFFB07818, null);
            Ui.bar(g, x, by + 7, 86, 8, ClientData.hp / Math.max(1, ClientData.maxHp), 0xFFE84848, 0xFF801818,
                    Ui.fmt(ClientData.hp) + " / " + Ui.fmt(ClientData.maxHp));
            Ui.bar(g, x + 90, by + 7, 86, 8, ClientData.mana / Math.max(1, ClientData.maxMana), 0xFF4C90FF, 0xFF1A3A90,
                    Ui.fmt(ClientData.mana) + " / " + Ui.fmt(ClientData.maxMana));
        }

        // ------------------------------------------------ attributs (repartition des points)
        int rx = x + 186;
        int rw = px + pw - 8 - rx;
        int w1 = Math.max(104, (int) (rw * 0.54));
        int y0 = py + 30;
        int avail = (bottom - y0) - (ph >= 258 ? 34 : 0);
        Ui.header(g, rx, y0, w1, "ATTRIBUTS");
        g.text(this.font, "Points : " + d.attributePoints, rx, y0 + 13, d.attributePoints > 0 ? Ui.GOLD_LIGHT : Ui.MUTED, true);
        if (d.attributePoints > 0) {
            String hint = "Maj : +5";
            g.text(this.font, hint, rx + w1 - this.font.width(hint), y0 + 13, Ui.MUTED, false);
        }
        int n = Stat.PRIMARIES_DISPLAY.length;
        int rowH = Math.max(11, Math.min(18, (avail - 24) / n));
        int frameH = rowH - 2;
        List<Stat> mine = java.util.Arrays.asList(cls.mainAttributes());
        for (int row = 0; row < n; row++) {
            Stat s = Stat.PRIMARIES_DISPLAY[row];
            int i = s.primaryIndex();
            int ry = y0 + 24 + row * rowH;
            int tyRow = ry + (frameH - 8) / 2 + 1;
            boolean hover = hovered(rx, ry, w1, frameH);
            Ui.frame(g, rx, ry, w1, frameH, s.color, hover || mine.contains(s));
            g.text(this.font, s.shortName, rx + 4, tyRow, s.color, true);
            String val = String.valueOf(Math.round(ClientData.stats.get(s)));
            int valX = rx + w1 - (d.attributePoints > 0 ? 17 : 5) - this.font.width(val);
            int labelEnd = valX - 3;
            if (mine.contains(s)) {
                labelEnd -= this.font.width("★") + 2;
                g.text(this.font, "★", labelEnd + 1, tyRow, Ui.GOLD_LIGHT, true);
            }
            g.text(this.font, this.font.plainSubstrByWidth(s.label, labelEnd - (rx + 27)), rx + 27, tyRow, Ui.TEXT, false);
            g.text(this.font, val, valX, tyRow, 0xFFFFFFFF, true);
            if (d.attributePoints > 0) {
                int bh = Math.min(12, frameH - 2);
                final int attr = i;
                button(g, rx + w1 - 14, ry + (frameH - bh) / 2, 12, bh, "+", 0xFF3A6A2A,
                        () -> ClientNet.send(new Payloads.Allocate(attr, this.minecraft.hasShiftDown() ? Math.min(5, d.attributePoints) : 1)));
            }
            if (hover) {
                List<Component> tip = new ArrayList<>();
                tip.add(Component.literal(s.label + (mine.contains(s) ? "  ★ attribut de votre classe" : "")).withColor(s.color));
                tip.add(Component.literal("Base de classe + niveau : " + Math.round(cls.baseAttributes[i] + cls.growth[i] * (d.level - 1))).withColor(Ui.MUTED));
                tip.add(Component.literal("Points investis : " + d.allocated[i]).withColor(Ui.MUTED));
                tip.add(Component.literal(ATTR_DESC[i]).withColor(Ui.TEXT));
                g.setComponentTooltipForNextFrame(this.font, tip, mouseX, mouseY);
            }
        }

        // ------------------------------------------------ statistiques
        int x2 = rx + w1 + 8;
        int w2 = px + pw - 8 - x2;
        Ui.header(g, x2, y0, w2, "STATISTIQUES");
        int step = Math.max(9, Math.min(11, (avail - 14) / 14));
        int sy = y0 + 14;
        for (Stat s : Stat.values()) {
            if (s.isPrimary()) continue;
            boolean hover = hovered(x2, sy - 1, w2, step);
            if (hover) g.fill(x2 - 2, sy - 1, x2 + w2 + 2, sy + step - 1, 0x30FFFFFF);
            String val = s.format(ClientData.stats.get(s), false);
            int vw = this.font.width(val);
            g.text(this.font, this.font.plainSubstrByWidth(shortLabel(s), w2 - vw - 4), x2, sy, Ui.MUTED, false);
            g.text(this.font, val, x2 + w2 - vw, sy, s.color, true);
            if (hover) g.setComponentTooltipForNextFrame(this.font, statTooltip(s, d), mouseX, mouseY);
            sy += step;
        }

        // ------------------------------------------------ chemin d'evolution (si la place le permet)
        if (ph >= 258) {
            int ty = py + ph - 24;
            int tx0 = rx + 8, tx1 = px + pw - 16;
            g.fill(tx0, ty + 4, tx1, ty + 6, 0xFF3A2E22);
            int tier = PlayerClass.tier(d.level);
            int reachedX = tx0 + (int) ((tx1 - tx0) * Math.min(1f, (d.level - 1) / 99f));
            g.fillGradient(tx0, ty + 4, reachedX, ty + 6, Ui.GOLD_LIGHT, Ui.GOLD_DARK);
            for (int i = 0; i < 5; i++) {
                int nx = tx0 + (tx1 - tx0) * i / 4;
                boolean reached = i <= tier;
                Ui.diamond(g, nx, ty + 5, i == tier ? 5 : 4, reached ? Ui.GOLD : 0xFF4A4038);
                Ui.diamond(g, nx, ty + 5, i == tier ? 2 : 1, reached ? (i == tier ? 0xFFFFFFFF : cls.color) : 0xFF201818);
                int ly = i % 2 == 0 ? ty + 10 : ty - 13;     // etiquettes alternees au-dessus / au-dessous de la ligne
                Ui.scaledText(g, cls.evolutions[i], nx, ly, 0.6f, i == tier ? cls.color : reached ? Ui.TEXT : Ui.MUTED, true);
                Ui.scaledText(g, "Niv. " + PlayerClass.EVOLUTION_LEVELS[i], nx, ly + 6, 0.6f, Ui.MUTED, true);
            }
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        g.text(this.font, "Artisanat", this.titleLabelX, this.titleLabelY, Ui.GOLD, true);
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT && this.menu.getCarried().isEmpty()) {
            for (int i = hits.size() - 1; i >= 0; i--) {
                Hit h = hits.get(i);
                if (Ui.in(event.x(), event.y(), h.x(), h.y(), h.w(), h.h())) {
                    this.minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                    h.action().run();
                    return true;
                }
            }
        }
        com.mmorpg.MMORPG.LOGGER.debug("[MMORPG][clic] CharacterScreen : ({}, {}) bouton {} ({} zones, objet porte : {})",
                (int) event.x(), (int) event.y(), event.button(), hits.size(), !this.menu.getCarried().isEmpty());
        return super.mouseClicked(event, doubleClick);
    }

    /** Centre d'un emplacement a l'ecran (tests automatiques). */
    public int[] slotCenter(Slot s) {
        return new int[]{this.leftPos + s.x + 8, this.topPos + s.y + 8};
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (Keys.MENU.matches(event)) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    private static String shortLabel(Stat s) {
        return switch (s) {
            case MAX_HP -> "Points de vie";
            case MAX_MANA -> "Mana";
            case ATK -> "Attaque";
            case MAG -> "Magie";
            case DEF -> "Défense";
            case CRIT -> "Critique";
            case CRIT_DMG -> "Dégâts crit.";
            case ESQ -> "Esquive";
            case SPEED -> "Vitesse";
            case ATK_SPEED -> "Vit. d'attaque";
            case HP_REGEN -> "Régén. vie";
            case MANA_REGEN -> "Régén. mana";
            case LIFESTEAL -> "Vol de vie";
            case CDR -> "Récupération";
            default -> s.label;
        };
    }

    private static List<Component> statTooltip(Stat s, PlayerData d) {
        List<Component> tip = new ArrayList<>();
        tip.add(Component.literal(s.label).withColor(s.color));
        String desc = switch (s) {
            case MAX_HP -> "Vos points de vie. À 0 PV, vous mourez.";
            case MAX_MANA -> "Ressource consommée par les compétences.";
            case ATK -> "Puissance des attaques physiques (épées, marteaux, arcs) et des compétences du Guerrier, de l'Archer et du Tank.";
            case MAG -> "Puissance des sorts du Mage et des projectiles des bâtons.";
            case DEF -> {
                double def = ClientData.stats.get(Stat.DEF);
                double red = Math.min(0.8, def / (def + 200 + 10.0 * d.level));
                yield "Réduit les dégâts subis : DEF / (DEF + 200 + 10 × niveau de l'attaquant). Contre un ennemi de votre niveau : -" + Math.round(red * 100) + " %.";
            }
            case CRIT -> "Chance d'infliger un coup critique (maximum 75 %).";
            case CRIT_DMG -> "Multiplicateur des coups critiques.";
            case ESQ -> "Chance d'éviter complètement une attaque (maximum 45 %).";
            case SPEED -> "Bonus de vitesse de déplacement.";
            case ATK_SPEED -> "Bonus de vitesse d'attaque au corps à corps.";
            case HP_REGEN -> "PV régénérés par seconde (réduit de moitié en combat).";
            case MANA_REGEN -> "Mana régénéré par seconde.";
            case LIFESTEAL -> "Pourcentage des dégâts infligés rendu en PV.";
            case CDR -> "Réduit le temps de recharge des compétences (maximum 40 %).";
            default -> "";
        };
        for (String part : desc.split("(?<=\\.) ")) {
            tip.add(Component.literal(part).withColor(Ui.TEXT));
        }
        return tip;
    }
}
