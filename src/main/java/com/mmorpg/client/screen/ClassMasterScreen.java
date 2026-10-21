package com.mmorpg.client.screen;

import com.mmorpg.client.ClientData;
import com.mmorpg.client.ClientNet;
import com.mmorpg.client.ui.MmoScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.entity.hub.ClassMasterEntity;
import com.mmorpg.network.Payloads;
import com.mmorpg.registry.ModEntities;
import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.skill.Skill;
import com.mmorpg.skill.Skills;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Dialogue d'un Maitre de classe : presentation animee de la classe (modele 3D, role, attributs, statistiques, competences,
 * evolutions), essai a un niveau choisi, puis choix definitif avec confirmation. Le Gardien de l'epreuve, dans la salle d'essai,
 * permet de changer de classe ou de niveau, de choisir ou de quitter. Ouverture, survols et selections sont animes (courbes
 * d'amortissement, temps reel) et accompagnes de sons.
 */
public class ClassMasterScreen extends MmoScreen {
    private static final PlayerClass[] CLASSES = {PlayerClass.GUERRIER, PlayerClass.MAGE, PlayerClass.ARCHER, PlayerClass.TANK};
    private static final String[] RATINGS = {"Attaque", "Défense", "Magie", "Mobilité", "Difficulté"};
    private static final int[] LEVELS = {1, 25, 50, 75, 100};

    private final int kind;
    private final boolean inTrial;
    private final int trialClass;
    private final int trialLevel;
    private final boolean hasClass;
    private final int tried;
    private final boolean operator;
    private final boolean trialEnabled;
    private int selectedClass;
    private int selectedLevel = 2;           // index dans LEVELS : niveau 50 par defaut
    private boolean confirming;
    private final long openedAt = System.nanoTime();
    private long confirmAt;
    private long classChangedAt = System.nanoTime();
    private final Map<String, Float> hover = new HashMap<>();
    private long lastFrame = System.nanoTime();
    private final ClassMasterEntity[] portraits = new ClassMasterEntity[ClassMasterEntity.IDS.length];

    public ClassMasterScreen(CompoundTag t) {
        super("Maître de classe");
        this.kind = t.getIntOr("kind", 0);
        this.inTrial = t.getBooleanOr("inTrial", false);
        this.trialClass = t.getIntOr("trialClass", 0);
        this.trialLevel = t.getIntOr("trialLevel", 50);
        this.hasClass = t.getBooleanOr("hasClass", false);
        this.tried = t.getIntOr("tried", 0);
        this.operator = t.getBooleanOr("operator", false);
        this.trialEnabled = t.getBooleanOr("trialEnabled", true);
        this.selectedClass = kind < 4 ? CLASSES[kind].ordinal() : (inTrial ? trialClass : PlayerClass.GUERRIER.ordinal());
        for (int i = 0; i < LEVELS.length; i++) if (LEVELS[i] == trialLevel && inTrial) selectedLevel = i;
    }

    @Override
    protected int panelWidth() {
        return Math.min(this.width - 8, 470);
    }

    @Override
    protected int panelHeight() {
        return Math.min(this.height - 8, 270);
    }

    // ================================================================== utilitaires

    private static float easeOutCubic(float t) {
        float u = 1F - Mth.clamp(t, 0F, 1F);
        return 1F - u * u * u;
    }

    private static float easeInOutCubic(float t) {
        t = Mth.clamp(t, 0F, 1F);
        return t < 0.5F ? 4F * t * t * t : 1F - (float) Math.pow(-2F * t + 2F, 3) / 2F;
    }

    private float since(long nanos, float seconds) {
        return (System.nanoTime() - nanos) / 1.0E9F / seconds;
    }

    private static void sound(SoundEvent s, float pitch) {
        Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(s, pitch, 0.7F));
    }

    private ClassMasterEntity portrait(int classOrdinal) {
        int idx = kind < 4 ? kind : Math.max(0, Math.min(3, java.util.Arrays.asList(CLASSES).indexOf(PlayerClass.byId(classOrdinal))));
        if (portraits[idx] == null && Minecraft.getInstance().level != null) {
            ClassMasterEntity e = new ClassMasterEntity(ModEntities.CLASS_MASTER.get(), Minecraft.getInstance().level);
            e.setup(idx);
            e.setCustomNameVisible(false);
            e.stage(2.4F);
            portraits[idx] = e;
        }
        return portraits[idx];
    }

    private PlayerClass current() {
        return PlayerClass.byId(selectedClass);
    }

    @Override
    public void tick() {
        for (ClassMasterEntity e : portraits) if (e != null) e.tickCount++;
    }

    /** Bouton avec survol anime ; renvoie vrai s'il vient d'etre active (via click()). */
    private void btn(GuiGraphicsExtractor g, String id, int x, int y, int w, int h, String label, boolean enabled, int accent, Runnable action) {
        float dt = Mth.clamp((System.nanoTime() - lastFrame) / 1.0E9F, 0F, 0.1F);
        boolean over = enabled && hovered(x, y, w, h);
        float v = hover.getOrDefault(id, 0F);
        v += ((over ? 1F : 0F) - v) * (1F - (float) Math.exp(-dt * 14F));
        hover.put(id, v);
        int top = enabled ? Ui.lighter(accent, 0.1F + 0.3F * v) : 0xFF3A3436;
        int bottom = enabled ? Ui.darker(accent, 0.42F + 0.2F * v) : 0xFF242024;
        g.fillGradient(x, y, x + w, y + h, top, bottom);
        Ui.border(g, x, y, w, h, enabled ? (v > 0.5F ? Ui.GOLD_LIGHT : Ui.GOLD_DARK) : 0xFF4A4448);
        g.fill(x + 1, y + 1, x + w - 1, y + 2, 0x50FFFFFF);
        if (v > 0.02F) g.fill(x + 1, y + h - 3, x + 1 + (int) ((w - 2) * v), y + h - 1, Ui.alpha(Ui.GOLD_LIGHT, 0.7F * v));
        g.centeredText(font, Component.literal(label), x + w / 2, y + (h - 8) / 2, enabled ? (v > 0.5F ? 0xFFFFFFFF : Ui.TEXT) : 0xFF807878);
        if (enabled) click(x, y, w, h, () -> {
            sound(SoundEvents.UI_BUTTON_CLICK.value(), 1.0F);
            action.run();
        });
    }

    // ================================================================== rendu

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        float open = easeOutCubic(since(openedAt, 0.3F));
        g.fillGradient(0, 0, this.width, this.height, Ui.alpha(0xA0080610, open), Ui.alpha(0xD0080610, open));
    }

    @Override
    protected void renderContent(GuiGraphicsExtractor g, int mx, int my, float a) {
        float open = easeOutCubic(since(openedAt, 0.3F));
        float s = 0.92F + 0.08F * open;
        g.pose().pushMatrix();
        g.pose().translate(this.width / 2F, this.height / 2F + (1F - open) * 10F);
        g.pose().scale(s, s);
        g.pose().translate(-this.width / 2F, -this.height / 2F);
        if (open < 1F) hits.clear();                    // pas de clic pendant l'ouverture (evite les clics fantomes)
        Ui.panel(g, left, top, pw, ph);
        PlayerClass cls = current();
        drawHeader(g, cls);
        drawPortrait(g, mx, my, cls);
        drawInfo(g, cls);
        drawActions(g, cls);
        if (confirming) drawConfirm(g, cls);
        g.pose().popMatrix();
        lastFrame = System.nanoTime();
    }

    private void drawHeader(GuiGraphicsExtractor g, PlayerClass cls) {
        String title = kind < 4 ? ClassMasterEntity.NAMES[kind] : ClassMasterEntity.NAMES[4];
        g.pose().pushMatrix();
        g.pose().translate(left + pw / 2F, top + 6);
        g.pose().scale(1.5F, 1.5F);
        g.centeredText(font, com.mmorpg.client.EldoriaFont.of(title), 0, 0, 0xFF000000 | (kind < 4 ? cls.color : Ui.GOLD_LIGHT));
        g.pose().popMatrix();
        g.fill(left + 8, top + 22, left + pw - 8, top + 23, Ui.GOLD_DARK);
        if (inTrial) {
            String t = "ESSAI EN COURS — " + PlayerClass.byId(trialClass).label + ", niveau " + trialLevel + " : rien n'est conservé";
            g.centeredText(font, Component.literal(t), left + pw / 2, top + 26, 0xFFFFC060);
        }
    }

    private void drawPortrait(GuiGraphicsExtractor g, int mx, int my, PlayerClass cls) {
        int px = left + 12;
        int py = top + 38;
        int w = 128;
        int h = ph - 52;
        Ui.inset(g, px, py, w, h);
        float flash = 1F - easeOutCubic(since(classChangedAt, 0.5F));
        g.fillGradient(px + 1, py + 1, px + w - 1, py + h - 1, Ui.withAlpha(cls.color, 0x30 + (int) (0x50 * flash)), 0x00000000);
        ClassMasterEntity e = portrait(selectedClass);
        if (e != null) {
            int size = Math.min(h - 26, 118);
            InventoryScreen.extractEntityInInventoryFollowsMouse(g, px + 2, py + 4, px + w - 2, py + 4 + size + 40, Math.round(size * 0.42F), 0.0625F, mx, my, e);
        }
        g.centeredText(font, Component.literal(cls.label), px + w / 2, py + h - 22, cls.color);
        float rs = Math.min(0.8F, (w - 8) / (float) Math.max(1, font.width(cls.role)));
        Ui.scaledText(g, cls.role, px + w / 2F, py + h - 11, rs, Ui.MUTED, true);
    }

    private void drawInfo(GuiGraphicsExtractor g, PlayerClass cls) {
        int x = left + 150;
        int y = top + 38;
        int w = pw - 162;
        float reveal = easeInOutCubic(since(classChangedAt, 0.35F));
        int alphaColor = (int) (reveal * 255);
        g.pose().pushMatrix();
        g.pose().translate((1F - reveal) * 8F, 0F);
        // description
        int ty = y;
        for (var line : font.split(net.minecraft.network.chat.FormattedText.of(cls.description), w)) {
            if (ty - y > 34) break;
            g.text(font, line, x, ty, Ui.withAlpha(0xE0D8C8, Math.max(8, alphaColor)), false);
            ty += 9;
        }
        ty = y + 38;
        // attributs
        g.text(font, "Attributs :", x, ty, Ui.GOLD, false);
        int ax = x + font.width("Attributs :") + 6;
        for (var st : cls.mainAttributes()) {
            String label = st.name().substring(0, 1) + st.name().substring(1).toLowerCase(Locale.ROOT);
            g.text(font, label, ax, ty, cls.color, true);
            ax += font.width(label) + 8;
        }
        ty += 12;
        // evaluation
        int half = (w - 6) / 2;
        for (int i = 0; i < RATINGS.length; i++) {
            int cx = x + (i % 2) * (half + 6);
            int cy = ty + (i / 2) * 10;
            g.text(font, RATINGS[i], cx, cy, Ui.TEXT, false);
            for (int p = 0; p < 5; p++) {
                int dx = cx + half - 8 - (4 - p) * 7;
                Ui.diamond(g, dx, cy + 4, 2, p < cls.ratings[i] ? (i == 4 ? 0xFFFF7050 : Ui.GOLD) : 0xFF3A3030);
            }
        }
        ty += 32;
        // statistiques de base au niveau choisi
        int level = LEVELS[selectedLevel];
        boolean live = inTrial && trialClass == cls.ordinal() && ClientData.maxHp > 0;
        double hp = live ? ClientData.maxHp : cls.baseHp + cls.hpPerLevel * (level - 1);
        double mana = live ? ClientData.maxMana : 50 + cls.manaPerLevel * (level - 1);
        g.text(font, "Niveau " + level + " :", x, ty, Ui.GOLD, false);
        String stats = "PV " + Ui.fmt(hp) + "   Mana " + Ui.fmt(mana) + (live ? "" : "  (estimation)");
        g.text(font, stats, x + font.width("Niveau " + level + " :") + 6, ty, 0xFFE8E0D0, false);
        ty += 13;
        // competences et evolutions
        g.text(font, "Compétences :", x, ty, Ui.GOLD, false);
        List<Skill> skills = Skills.forClass(cls);
        int sx = x;
        int row = ty + 10;
        int shown = 0;
        for (Skill sk : skills) {
            if (shown >= 6) break;
            String name = sk.name;
            int tw = font.width(name) * 3 / 4 + 4;
            if (sx + tw > x + w) {
                sx = x;
                row += 9;
            }
            g.pose().pushMatrix();
            g.pose().translate(sx, row);
            g.pose().scale(0.75F, 0.75F);
            g.text(font, name, 0, 0, sk.unlockLevel <= level ? 0xFFD8E8FF : 0xFF807890, false);
            g.pose().popMatrix();
            sx += tw + 4;
            shown++;
        }
        row += 12;
        g.text(font, "Évolutions :", x, row, Ui.GOLD, false);
        int ex = x + font.width("Évolutions :") + 6;
        for (int i = 1; i < 5; i++) {
            String ev = PlayerClass.EVOLUTION_LEVELS[i] + " " + cls.evolutions[i];
            g.pose().pushMatrix();
            g.pose().translate(ex, row + 1);
            g.pose().scale(0.75F, 0.75F);
            g.text(font, ev, 0, 0, 0xFFC8C0B0, false);
            g.pose().popMatrix();
            ex += font.width(ev) * 3 / 4 + 6;
            if (ex > x + w - 40 && i < 4) {
                ex = x;
                row += 9;
            }
        }
        g.pose().popMatrix();
    }

    private void drawActions(GuiGraphicsExtractor g, PlayerClass cls) {
        int x = left + 150;
        int w = pw - 162;
        int y = top + ph - 66;
        g.fill(x, y - 6, x + w, y - 5, Ui.GOLD_DARK);
        // choix de la classe (Gardien de l'epreuve) : quatre pastilles
        if (kind == 4) {
            int cw = (w - 3 * 4) / 4;
            for (int i = 0; i < CLASSES.length; i++) {
                PlayerClass c = CLASSES[i];
                boolean sel = selectedClass == c.ordinal();
                int cx = x + i * (cw + 4);
                btn(g, "c" + i, cx, y - 2, cw, 14, c.label, true, sel ? c.color : 0xFF3A3040, () -> {
                    if (selectedClass != c.ordinal()) {
                        selectedClass = c.ordinal();
                        classChangedAt = System.nanoTime();
                        ClassMasterEntity e = portrait(selectedClass);
                        if (e != null) e.stage(2.4F);
                        sound(SoundEvents.UI_BUTTON_CLICK.value(), 1.2F);
                    }
                });
            }
            y += 16;
        }
        // niveaux d'essai
        g.text(font, "Niveau d'essai :", x, y + (kind == 4 ? 0 : 1), Ui.TEXT, false);
        int lx = x + font.width("Niveau d'essai :") + 6;
        int chip = 28;
        for (int i = 0; i < LEVELS.length; i++) {
            final int idx = i;
            btn(g, "l" + i, lx + i * (chip + 3), y - 2, chip, 13, String.valueOf(LEVELS[i]), true, selectedLevel == i ? current().color : 0xFF3A3040, () -> {
                selectedLevel = idx;
                classChangedAt = System.nanoTime();
                sound(SoundEvents.NOTE_BLOCK_CHIME.value(), 0.8F + 0.15F * idx);
            });
        }
        y += 16;
        boolean canChoose = !hasClass && (tried(selectedClass) || operator || (inTrial && trialClass == selectedClass));
        int bw = (w - 8) / 2;
        int level = LEVELS[selectedLevel];
        String tryLabel = inTrial ? "Essayer " + cls.label + " niv. " + level : "Essayer (niv. " + level + ")";
        btn(g, "try", x, y, bw, 16, tryLabel, (!hasClass || operator) && trialEnabled, 0xFF6A5A1A, () -> {
            ClientNet.send(new Payloads.HubAction(Payloads.HubAction.TRY, selectedClass, level));
            onClose();
        });
        if (hasClass) {
            g.centeredText(font, Component.literal("Vous avez déjà une classe"), x + bw + 4 + bw / 2, y + 4, Ui.MUTED);
        } else {
            btn(g, "choose", x + bw + 8, y, bw, 16, "Choisir " + cls.label, canChoose, 0xFF2A6A2A, () -> {
                confirming = true;
                confirmAt = System.nanoTime();
                sound(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.9F);
            });
        }
        y += 19;
        if (inTrial) {
            btn(g, "quit", x, y, bw, 14, "Quitter l'essai", true, 0xFF7A2A2A, () -> {
                ClientNet.send(new Payloads.HubAction(Payloads.HubAction.QUIT, 0, 0));
                onClose();
            });
        } else if (!canChoose && !hasClass) {
            g.text(font, "Essayez cette classe pour pouvoir la choisir.", x, y + 3, Ui.MUTED, false);
        }
        btn(g, "replay", x + bw + 8, y, bw, 14, "Revoir la présentation", !inTrial, 0xFF3A3A5A, () -> {
            ClientNet.send(new Payloads.HubAction(Payloads.HubAction.REPLAY, 0, 0));
            onClose();
        });
    }

    private boolean tried(int classOrdinal) {
        return (tried & (1 << classOrdinal)) != 0;
    }

    private void drawConfirm(GuiGraphicsExtractor g, PlayerClass cls) {
        float t = easeOutCubic(since(confirmAt, 0.25F));
        g.fill(left + 3, top + 3, left + pw - 3, top + ph - 3, Ui.alpha(0xD0060408, t));
        int bw = 270;
        int bh = 92;
        int bx = left + (pw - bw) / 2;
        int by = top + (ph - bh) / 2 + (int) ((1F - t) * 12);
        g.pose().pushMatrix();
        Ui.panel(g, bx, by, bw, bh);
        g.centeredText(font, com.mmorpg.client.EldoriaFont.of("Devenir " + cls.label + " ?"), bx + bw / 2, by + 12, cls.color);
        g.centeredText(font, Component.literal("Ce choix est définitif."), bx + bw / 2, by + 28, 0xFFFFC060);
        g.centeredText(font, Component.literal("Votre aventure commencera avec l'équipement de départ."), bx + bw / 2, by + 41, Ui.MUTED);
        hits.clear();                                                                        // seuls les boutons de la confirmation sont cliquables
        btn(g, "yes", bx + 14, by + bh - 28, 118, 18, "Oui, devenir " + cls.label, t > 0.9F, 0xFF2A7A2A, () -> {
            ClientNet.send(new Payloads.HubAction(Payloads.HubAction.CHOOSE, selectedClass, 0));
            sound(SoundEvents.PLAYER_LEVELUP, 1.0F);
            onClose();
        });
        btn(g, "no", bx + bw - 14 - 118, by + bh - 28, 118, 18, "Non, revenir", t > 0.9F, 0xFF5A3A3A, () -> confirming = false);
        g.pose().popMatrix();
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyEvent event) {
        if (confirming && event.key() == com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE) {
            confirming = false;
            return true;
        }
        return super.keyPressed(event);
    }
}
