package com.mmorpg.client.hud;

import com.mmorpg.client.ClientConfig;
import com.mmorpg.client.ClientData;
import com.mmorpg.client.Keys;
import com.mmorpg.client.screen.BestiaryScreen;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.entity.RpgMob;
import com.mmorpg.network.Payloads;
import com.mmorpg.registry.ModAttachments;
import com.mmorpg.rpg.BuffType;
import com.mmorpg.rpg.MobData;
import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.rpg.PublicPlayerData;
import com.mmorpg.skill.Skill;
import com.mmorpg.skill.Skills;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3fc;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Interface en jeu du mod (remplace les coeurs vanilla). */
public final class MmoHud {
    private static final List<LivingEntity> VISIBLE = new ArrayList<>();
    private static final List<com.mmorpg.entity.NpcEntity> NPCS = new ArrayList<>();
    private static LivingEntity lastTarget;
    private static long lastTargetTime;
    private static LivingEntity boss;

    private MmoHud() {
    }

    // ================================================================== mise a jour (tick client)

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            VISIBLE.clear();
            boss = null;
            return;
        }
        if (mc.crosshairPickEntity instanceof LivingEntity le && le.isAlive()) {
            lastTarget = le;
            lastTargetTime = System.currentTimeMillis();
        }
        if (lastTarget != null && (!lastTarget.isAlive() || lastTarget.isRemoved() || System.currentTimeMillis() - lastTargetTime > 4000)) {
            lastTarget = null;
        }
        if (mc.level.getGameTime() % 4 != 0) return;
        VISIBLE.clear();
        NPCS.clear();
        LivingEntity bestBoss = null;
        double bestDist = 64 * 64;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof com.mmorpg.entity.NpcEntity npc && npc.distanceToSqr(mc.player) < 24 * 24 && mc.player.hasLineOfSight(npc)) {
                NPCS.add(npc);
                continue;
            }
            if (!(e instanceof LivingEntity le) || e == mc.player || !le.isAlive()) continue;
            MobData md = le.getExistingDataOrNull(ModAttachments.MOB);
            if (md == null || !md.initialized()) continue;
            double d = le.distanceToSqr(mc.player);
            if (md.boss && d < bestDist) {
                bestDist = d;
                bestBoss = le;
            }
            if (d < 26 * 26 && mc.player.hasLineOfSight(le)) VISIBLE.add(le);
        }
        boss = bestBoss;
    }

    // ================================================================== rendu

    public static void render(GuiGraphicsExtractor g, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        int w = g.guiWidth();
        int h = g.guiHeight();
        if (!HudLayout.editing) worldOverlays(g, mc, w, h);
        if (!ClientData.received) return;
        if (HudLayout.begin(g, HudLayout.JOUEUR)) {
            playerFrame(g, mc);
            HudLayout.end(g);
        }
        if (HudLayout.begin(g, HudLayout.BUFFS)) {
            buffs(g);
            HudLayout.end(g);
        }
        if (HudLayout.begin(g, HudLayout.CIBLE)) {
            targetFrame(g);
            HudLayout.end(g);
        }
        if (HudLayout.begin(g, HudLayout.GROUPE)) {
            partyFrames(g, mc);
            HudLayout.end(g);
        }
        if (HudLayout.begin(g, HudLayout.QUETES)) {
            questTracker(g, mc);
            HudLayout.end(g);
        }
        if (HudLayout.begin(g, HudLayout.BOSS)) {
            bossFrame(g);
            HudLayout.end(g);
        }
        if (HudLayout.begin(g, HudLayout.SORTS)) {
            skillBar(g);
            HudLayout.end(g);
        }
        if (HudLayout.begin(g, HudLayout.BANNIERE)) {
            banner(g);
            HudLayout.end(g);
        }
    }

    private static Font font() {
        return Minecraft.getInstance().font;
    }

    private static void smallPanel(GuiGraphicsExtractor g, int x, int y, int w, int h, int accent) {
        g.fillGradient(x, y, x + w, y + h, 0xD01C1624, 0xD00C0A12);
        Ui.border(g, x, y, w, h, Ui.GOLD_DARK);
        Ui.border(g, x + 1, y + 1, w - 2, h - 2, Ui.withAlpha(accent, 0x50));
        Ui.diamond(g, x, y, 2, Ui.GOLD);
        Ui.diamond(g, x + w - 1, y, 2, Ui.GOLD);
    }

    // ------------------------------------------------------------------ tailles (pour l'ancrage et l'editeur)

    private static int activeBuffs() {
        long now = ClientData.now();
        int n = 0;
        for (long[] v : ClientData.BUFFS.values()) {
            if (v[0] - now > 0) n++;
        }
        return n;
    }

    static int[] buffsSize() {
        int n = activeBuffs();
        if (n == 0 && HudLayout.editing) n = 3;
        return new int[]{Math.max(1, n) * 18 - 2, 17};
    }

    private static boolean targetShown() {
        LivingEntity t = lastTarget;
        return t != null && t.isAlive() && !(t instanceof com.mmorpg.entity.NpcEntity);
    }

    /** Sans personnalisation, la cible se place sous les effets et le groupe sous la cible (comme avant). */
    static int defaultTargetY() {
        HudLayout.Element b = HudLayout.BUFFS;
        return !b.custom && b.visible && (activeBuffs() > 0 || HudLayout.editing) ? 86 : 66;
    }

    static int defaultPartyY() {
        HudLayout.Element c = HudLayout.CIBLE;
        if (c.custom || !c.visible || !(targetShown() || HudLayout.editing)) return defaultTargetY();
        return defaultTargetY() + Math.round(30 * c.scale) + 4;
    }

    private static int partyCount() {
        net.minecraft.nbt.ListTag members = ClientData.party.getListOrEmpty("members");
        Minecraft mc = Minecraft.getInstance();
        String me = mc.player != null ? mc.player.getName().getString() : "";
        int n = 0;
        for (int i = 0; i < members.size(); i++) {
            if (!members.getCompoundOrEmpty(i).getStringOr("name", "?").equals(me)) n++;
        }
        return n;
    }

    static int[] partySize() {
        int n = partyCount();
        if (n == 0 && HudLayout.editing) n = 2;
        return new int[]{120, Math.max(1, n) * 27 - 3};
    }

    private static int questHeight(net.minecraft.nbt.CompoundTag q) {
        return 12 + q.getListOrEmpty("objectives").size() * 9 + 2;
    }

    static int[] questSize() {
        net.minecraft.nbt.ListTag quests = ClientData.quests;
        if (quests.isEmpty()) return new int[]{153, HudLayout.editing ? 34 : 14};
        int h = 0;
        for (int i = 0; i < quests.size() && i < 3; i++) h += questHeight(quests.getCompoundOrEmpty(i)) + 4;
        if (quests.size() > 3) h += 10;
        return new int[]{153, h};
    }

    static int[] bannerSize() {
        HudEffects.Banner b = HudEffects.banner();
        Font f = font();
        if (b == null) return new int[]{(int) (f.width("NIVEAU 12 !") * 1.8f) + 60, (int) (9 * 1.8f) + 26};
        float scale = bannerScale(b);
        int bw = Math.max((int) (f.width(b.title()) * scale), f.width(b.subtitle())) + 60;
        return new int[]{bw, (int) (9 * scale) + 26};
    }

    private static float bannerScale(HudEffects.Banner b) {
        return b.kind() == Payloads.Notify.EVOLUTION || b.kind() == Payloads.Notify.BOSS ? 2.4f : 1.8f;
    }

    /** Taille du bloc d'effets de potion vanilla (icones de 24 px, effets negatifs sur une 2e ligne). */
    static int[] vanillaEffectsSize() {
        Minecraft mc = Minecraft.getInstance();
        int good = 0;
        int bad = 0;
        if (mc.player != null) {
            for (net.minecraft.world.effect.MobEffectInstance e : mc.player.getActiveEffects()) {
                if (!e.showIcon()) continue;
                if (e.getEffect().value().isBeneficial()) good++;
                else bad++;
            }
        }
        int n = Math.max(good, bad);
        if (n == 0) return new int[]{HudLayout.editing ? 75 : 25, 24};
        return new int[]{25 * n, bad > 0 ? 50 : 24};
    }

    // ------------------------------------------------------------------ elements (coordonnees locales)

    private static void playerFrame(GuiGraphicsExtractor g, Minecraft mc) {
        PlayerData d = ClientData.DATA;
        PlayerClass cls = d.playerClass;
        int x = 0, y = 0, w = 168, h = 58;
        smallPanel(g, x, y, w, h, cls.color);
        Font f = font();
        if (!cls.isPlayable()) {
            g.text(f, "Aucune classe choisie", x + 8, y + 10, Ui.GOLD_LIGHT, true);
            g.text(f, "Appuyez sur [" + Keys.MENU.getTranslatedKeyMessage().getString() + "]", x + 8, y + 24, Ui.TEXT, false);
            return;
        }
        Ui.icon(g, Ui.classIcon(cls.id(), false), x + 5, y + 5, 22, 32);
        Ui.border(g, x + 4, y + 4, 24, 24, Ui.GOLD_DARK);
        String lvl = String.valueOf(d.level);
        g.fill(x + 4, y + 22, x + 6 + f.width(lvl) + 2, y + 31, 0xE0101010);
        g.text(f, lvl, x + 6, y + 23, Ui.GOLD_LIGHT, true);
        g.text(f, f.plainSubstrByWidth(mc.player.getName().getString(), 70), x + 32, y + 5, 0xFFFFFFFF, true);
        String title = cls.title(d.level);
        g.text(f, f.plainSubstrByWidth(title, 62), x + w - 6 - Math.min(62, f.width(title)), y + 5, cls.color, true);
        Ui.bar(g, x + 32, y + 16, w - 38, 9, ClientData.hp / Math.max(1, ClientData.maxHp), 0xFFF05050, 0xFF8A1818,
                Ui.fmt(ClientData.hp) + " / " + Ui.fmt(ClientData.maxHp));
        Ui.bar(g, x + 32, y + 28, w - 38, 7, ClientData.mana / Math.max(1, ClientData.maxMana), 0xFF5A9CFF, 0xFF1A3A9A,
                Ui.fmt(ClientData.mana) + " / " + Ui.fmt(ClientData.maxMana));
        float xp = d.level >= 100 ? 1 : d.xp / (float) Math.max(1, ClientData.xpToNext);
        Ui.bar(g, x + 6, y + 41, w - 12, 3, xp, 0xFFFFD860, 0xFFB07818, null);
        String gold = Ui.fmt(d.gold) + " or";
        g.pose().pushMatrix();
        g.pose().translate(x + 6, y + 46);
        g.pose().scale(0.5f, 0.5f);
        g.item(new net.minecraft.world.item.ItemStack(com.mmorpg.registry.ModItems.PIECE_OR.get()), 0, 0);
        g.pose().popMatrix();
        g.text(f, gold, x + 16, y + 47, 0xFFFFD040, true);
        String xpTxt = d.level >= 100 ? "MAX" : Math.round(xp * 1000) / 10.0 + " % XP";
        g.text(f, xpTxt, x + w - 6 - f.width(xpTxt), y + 47, 0xFFC8B070, false);
        if (d.attributePoints > 0 || ClientData.skillPoints > 0) {
            Ui.diamond(g, x + w - 6, y + h - 6, 3, (System.currentTimeMillis() / 400) % 2 == 0 ? Ui.GOLD_LIGHT : Ui.GOLD_DARK);
        }
    }

    private static void buffs(GuiGraphicsExtractor g) {
        int x = 0;
        long now = ClientData.now();
        for (Map.Entry<BuffType, long[]> e : ClientData.BUFFS.entrySet()) {
            long rem = e.getValue()[0] - now;
            if (rem <= 0) continue;
            buffIcon(g, x, e.getKey(), rem / (float) Math.max(1, e.getValue()[1]), (int) Math.ceil(rem / 20.0));
            x += 18;
        }
        if (x == 0 && HudLayout.editing) {                    // apercu pour pouvoir placer l'element
            BuffType[] all = BuffType.values();
            for (int i = 0; i < 3 && i < all.length; i++, x += 18) buffIcon(g, x, all[i], 0.8f - 0.25f * i, 24 - 7 * i);
        }
    }

    private static void buffIcon(GuiGraphicsExtractor g, int x, BuffType b, float frac, int seconds) {
        Ui.icon(g, Ui.skillIcon(b.icon), x + 1, 1, 14, 32);
        Ui.border(g, x, 0, 16, 16, b.color);
        g.fill(x + 1, 15, x + 1 + (int) (14 * frac), 16, b.color);
        String s = String.valueOf(seconds);
        g.text(font(), s, x + 15 - font().width(s), 8, 0xFFFFFFFF, true);
    }

    private static void targetFrame(GuiGraphicsExtractor g) {
        if (!targetShown()) {
            if (HudLayout.editing) drawTarget(g, "Gobelin pillard", ClientData.DATA.level + 1, 0.65f, "1 240 / 1 900", false);
            return;
        }
        LivingEntity t = lastTarget;
        String name;
        int level;
        float frac;
        String hpText;
        boolean bossFlag = false;
        MobData md = t.getExistingDataOrNull(ModAttachments.MOB);
        if (md != null && md.initialized()) {
            name = t instanceof RpgMob rm ? BestiaryScreen.displayName(rm.mobKey()) : t.getName().getString();
            level = md.level;
            frac = md.hpFraction();
            hpText = Ui.fmt(md.hp) + " / " + Ui.fmt(md.maxHp);
            bossFlag = md.boss;
        } else if (t instanceof Player p) {
            PublicPlayerData pub = p.getExistingDataOrNull(ModAttachments.PUBLIC);
            name = p.getName().getString();
            level = pub != null ? pub.level : 1;
            frac = pub != null ? pub.hpFraction : p.getHealth() / p.getMaxHealth();
            hpText = Math.round(frac * 100) + " %";
            if (pub != null) {
                PlayerClass c = PlayerClass.byId(pub.playerClass);
                name += " — " + c.title(pub.level);
            }
        } else {
            name = t.getName().getString();
            level = 0;
            frac = t.getHealth() / Math.max(1, t.getMaxHealth());
            hpText = Math.round(t.getHealth()) + " / " + Math.round(t.getMaxHealth());
        }
        drawTarget(g, name, level, frac, hpText, bossFlag);
    }

    private static void drawTarget(GuiGraphicsExtractor g, String name, int level, float frac, String hpText, boolean bossFlag) {
        int x = 0, y = 0, w = 168, h = 30;
        Font f = font();
        int lvlColor = level > 0 ? Ui.levelColor(level, ClientData.DATA.level) : Ui.MUTED;
        smallPanel(g, x, y, w, h, bossFlag ? 0xFFFF4040 : lvlColor);
        String lv = level > 0 ? (bossFlag ? "☠ " : "") + "Nv. " + level : "";
        g.text(f, f.plainSubstrByWidth(name, w - 16 - f.width(lv)), x + 6, y + 5, bossFlag ? 0xFFFF7070 : 0xFFFFFFFF, true);
        g.text(f, lv, x + w - 6 - f.width(lv), y + 5, lvlColor, true);
        Ui.bar(g, x + 6, y + 17, w - 12, 8, frac, 0xFFE84040, 0xFF7A1010, hpText);
    }

    private static void partyFrames(GuiGraphicsExtractor g, Minecraft mc) {
        net.minecraft.nbt.ListTag members = ClientData.party.getListOrEmpty("members");
        String me = mc.player.getName().getString();
        int y = 0;
        for (int i = 0; i < members.size(); i++) {
            net.minecraft.nbt.CompoundTag m = members.getCompoundOrEmpty(i);
            String name = m.getStringOr("name", "?");
            if (name.equals(me)) continue;
            partyMember(g, y, name, PlayerClass.byId(m.getIntOr("class", 0)), m.getIntOr("level", 1), m.getFloatOr("hp", 1),
                    m.getFloatOr("mana", 1), m.getBooleanOr("online", false), m.getBooleanOr("near", false), m.getBooleanOr("leader", false));
            y += 27;
        }
        if (y == 0 && HudLayout.editing) {
            partyMember(g, 0, "Aria", PlayerClass.MAGE, ClientData.DATA.level, 0.8f, 0.55f, true, true, true);
            partyMember(g, 27, "Borin", PlayerClass.GUERRIER, ClientData.DATA.level + 2, 0.45f, 0.9f, true, false, false);
        }
    }

    private static void partyMember(GuiGraphicsExtractor g, int y, String name, PlayerClass cls, int level, float hp, float mana,
                                    boolean online, boolean near, boolean leader) {
        Font f = font();
        int w = 120;
        int h = 24;
        smallPanel(g, 0, y, w, h, online ? cls.color : 0xFF505050);
        if (online) Ui.icon(g, Ui.classIcon(cls.id(), false), 3, y + 4, 16, 32, near ? 0xFFFFFFFF : 0xFF808080);
        String label = (leader ? "♛ " : "") + name;
        g.text(f, f.plainSubstrByWidth(label, 70), 22, y + 3, online ? (near ? 0xFFFFFFFF : Ui.MUTED) : 0xFF707070, true);
        if (online) {
            String lv = String.valueOf(level);
            g.text(f, lv, w - 5 - f.width(lv), y + 3, cls.color, true);
            Ui.bar(g, 22, y + 13, w - 24, 4, hp, 0xFFE84848, 0xFF801818, null);
            Ui.bar(g, 22, y + 19, w - 24, 2, mana, 0xFF4C90FF, 0xFF1A3A90, null);
        } else {
            g.text(f, "Hors ligne", 22, y + 13, 0xFF707070, false);
        }
    }

    private static void questTracker(GuiGraphicsExtractor g, Minecraft mc) {
        net.minecraft.nbt.ListTag quests = ClientData.quests;
        if (mc.gui.screen() != null && !HudLayout.editing) return;
        Font f = font();
        int w = 150;
        int x = 3;
        int y = 2;
        if (quests.isEmpty()) {
            if (!HudLayout.editing) return;
            questBox(g, x, y, w, 30, false);
            g.text(f, "Menace gobeline", x, y, Ui.GOLD_LIGHT, true);
            questLine(g, f, x, y + 11, w, "Gobelins vaincus", "3 / 8", false);
            questLine(g, f, x, y + 20, w, "Parler au capitaine", "", true);
            return;
        }
        int shown = 0;
        for (int i = 0; i < quests.size() && shown < 3; i++, shown++) {
            net.minecraft.nbt.CompoundTag q = quests.getCompoundOrEmpty(i);
            net.minecraft.nbt.ListTag objs = q.getListOrEmpty("objectives");
            int h = questHeight(q);
            boolean ready = q.getBooleanOr("completable", false);
            questBox(g, x, y, w, h, ready);
            g.text(f, f.plainSubstrByWidth(q.getStringOr("name", "?"), w - 4), x, y, ready ? Ui.GREEN : Ui.GOLD_LIGHT, true);
            int oy = y + 11;
            for (int j = 0; j < objs.size(); j++) {
                net.minecraft.nbt.CompoundTag o = objs.getCompoundOrEmpty(j);
                questLine(g, f, x, oy, w, com.mmorpg.client.QuestText.objective(o), com.mmorpg.client.QuestText.progress(o),
                        com.mmorpg.client.QuestText.done(o));
                oy += 9;
            }
            y += h + 4;
        }
        if (quests.size() > 3) {
            g.text(f, "+" + (quests.size() - 3) + " autre(s) quête(s)", x, y, Ui.MUTED, false);
        }
    }

    private static void questBox(GuiGraphicsExtractor g, int x, int y, int w, int h, boolean ready) {
        g.fillGradient(x - 3, y - 2, x + w, y + h, 0x90100C14, 0x40100C14);
        g.fill(x - 3, y - 2, x - 2, y + h, ready ? 0xFF60E060 : 0xFFE8C060);
    }

    private static void questLine(GuiGraphicsExtractor g, Font f, int x, int y, int w, String text, String prog, boolean done) {
        g.pose().pushMatrix();
        g.pose().translate(x + 3, y);
        g.pose().scale(0.8f, 0.8f);
        String line = f.plainSubstrByWidth(text, (int) ((w - 10) / 0.8f) - f.width(prog) - 4);
        g.text(f, line, 0, 0, done ? 0xFF80E080 : 0xFFE0D8C8, true);
        g.text(f, prog, (int) ((w - 8) / 0.8f) - f.width(prog), 0, done ? 0xFF80E080 : 0xFFFFFFFF, true);
        g.pose().popMatrix();
    }

    private static void bossFrame(GuiGraphicsExtractor g) {
        LivingEntity b = boss;
        MobData md = b != null && b.isAlive() ? b.getExistingDataOrNull(ModAttachments.MOB) : null;
        if (md == null) {
            if (HudLayout.editing) drawBoss(g, "Roi gobelin", 0.8f, "8 000 / 10 000", "Niveau 15  •  Phase 1");
            return;
        }
        String name = b instanceof RpgMob rm ? BestiaryScreen.displayName(rm.mobKey()) : b.getName().getString();
        drawBoss(g, name, md.hpFraction(), Ui.fmt(md.hp) + " / " + Ui.fmt(md.maxHp), "Niveau " + md.level + "  •  Phase " + Math.max(1, md.phase));
    }

    private static void drawBoss(GuiGraphicsExtractor g, String name, float frac, String hpText, String sub) {
        int w = 260;
        int x = 6;
        int y = 2;
        Font f = font();
        g.fillGradient(x - 6, y - 2, x + w + 6, y + 30, 0xC0200808, 0xC0080404);
        Ui.border(g, x - 6, y - 2, w + 12, 32, 0xFF8A2A1A);
        Ui.diamond(g, x - 6, y + 14, 4, 0xFFC03020);
        Ui.diamond(g, x + w + 5, y + 14, 4, 0xFFC03020);
        String title = "☠ " + name.toUpperCase(java.util.Locale.ROOT) + " ☠";
        g.centeredText(f, Component.literal(title), x + w / 2, y + 1, 0xFFFF6A50);
        Ui.bar(g, x, y + 12, w, 10, frac, 0xFFD02020, 0xFF5A0808, hpText);
        g.centeredText(f, Component.literal(sub), x + w / 2, y + 24, 0xFFE0A090);
    }

    private static void skillBar(GuiGraphicsExtractor g) {
        PlayerData d = ClientData.DATA;
        if (!d.playerClass.isPlayable() && !HudLayout.editing) return;
        int slot = 20;
        int gap = 2;
        int total = PlayerData.SKILL_SLOTS * (slot + gap) - gap;
        int x = 3;
        int y = 3;
        Font f = font();
        g.fillGradient(x - 3, y - 3, x + total + 3, y + slot + 3, 0xB0201828, 0xB0100C14);
        Ui.border(g, x - 3, y - 3, total + 6, slot + 6, Ui.GOLD_DARK);
        for (int i = 0; i < PlayerData.SKILL_SLOTS; i++) {
            int sx = x + i * (slot + gap);
            g.fill(sx, y, sx + slot, y + slot, 0xFF0C0A10);
            Ui.border(g, sx, y, slot, slot, 0xFF5A4630);
            Skill s = Skills.get(d.skillBar[i]);
            if (s != null) {
                Ui.icon(g, Ui.skillIcon(s.id), sx + 1, y + 1, slot - 2, 32);
                float cd = ClientData.cooldownFraction(s.id);
                if (cd > 0) {
                    int ch = (int) ((slot - 2) * cd);
                    g.fill(sx + 1, y + 1, sx + slot - 1, y + 1 + ch, 0xB0000000);
                    String sec = ClientData.cooldownSeconds(s.id) >= 10 ? String.valueOf((int) ClientData.cooldownSeconds(s.id))
                            : String.format(java.util.Locale.FRANCE, "%.1f", ClientData.cooldownSeconds(s.id));
                    g.centeredText(f, Component.literal(sec), sx + slot / 2, y + 6, 0xFFFFFFFF);
                } else if (ClientData.mana < s.manaCost) {
                    g.fill(sx + 1, y + 1, sx + slot - 1, y + slot - 1, 0x902040C0);
                }
            }
            String key = Keys.SKILLS[i].getTranslatedKeyMessage().getString();
            if (key.length() > 2) key = key.substring(0, 2);
            g.pose().pushMatrix();
            g.pose().translate(sx + 1.5f, y + slot - 6.5f);
            g.pose().scale(0.6f, 0.6f);
            g.text(f, key, 0, 0, 0xFFFFE6A0, true);
            g.pose().popMatrix();
        }
    }

    private static void banner(GuiGraphicsExtractor g) {
        HudEffects.Banner b = HudEffects.banner();
        int bw = bannerSize()[0];
        if (b == null) {
            if (HudLayout.editing) drawBanner(g, "NIVEAU 12 !", "Aperçu des annonces", 0xFFFFD040, 1.8f, 1f, bw);
            return;
        }
        long t = System.currentTimeMillis() - b.start();
        float alpha = t < 250 ? t / 250f : t > HudEffects.BANNER_LIFE_MS - 600 ? (HudEffects.BANNER_LIFE_MS - t) / 600f : 1f;
        alpha = Math.max(0.05f, Math.min(1, alpha));
        drawBanner(g, b.title(), b.subtitle(), 0xFF000000 | b.color(), bannerScale(b), alpha, bw);
    }

    private static void drawBanner(GuiGraphicsExtractor g, String title, String subtitle, int color, float scale, float alpha, int bw) {
        int cx = bw / 2;
        int cy = 7;
        Font f = font();
        g.fillGradient(0, cy - 6, bw, cy + (int) (9 * scale) + 18, Ui.alpha(0xB0100C14, alpha), Ui.alpha(0x60100C14, alpha));
        g.fill(0, cy - 7, bw, cy - 6, Ui.alpha(color, alpha));
        g.fill(0, cy + (int) (9 * scale) + 18, bw, cy + (int) (9 * scale) + 19, Ui.alpha(color, alpha));
        Ui.diamond(g, 0, cy - 7, 3, Ui.alpha(color, alpha));
        Ui.diamond(g, bw, cy - 7, 3, Ui.alpha(color, alpha));
        Ui.scaledText(g, title, cx, cy, scale, Ui.alpha(color, alpha), true);
        g.centeredText(f, Component.literal(subtitle), cx, cy + (int) (9 * scale) + 5, Ui.alpha(0xFFF0E8D8, alpha));
    }

    // ================================================================== elements projetes dans le monde

    private static float[] project(Minecraft mc, Vec3 world, int w, int h) {
        Camera cam = mc.gameRenderer.mainCamera();
        Vec3 rel = world.subtract(cam.position());
        Vector3fc fwd = cam.forwardVector();
        if (rel.x * fwd.x() + rel.y * fwd.y() + rel.z * fwd.z() < 0.2) return null;
        Vec3 ndc = mc.gameRenderer.projectPointToScreen(world);
        if (Double.isNaN(ndc.x) || Math.abs(ndc.x) > 1.3 || Math.abs(ndc.y) > 1.3) return null;
        return new float[]{(float) ((ndc.x + 1) / 2 * w), (float) ((1 - ndc.y) / 2 * h), (float) rel.length()};
    }

    private static void worldOverlays(GuiGraphicsExtractor g, Minecraft mc, int w, int h) {
        Font f = font();
        float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        if (ClientConfig.SHOW_NAMEPLATES.get()) {
            for (LivingEntity e : VISIBLE) {
                if (!e.isAlive() || e == boss) continue;
                MobData md = e.getExistingDataOrNull(ModAttachments.MOB);
                if (md == null) continue;
                Vec3 pos = e.getPosition(partial).add(0, e.getBbHeight() + 0.55, 0);
                float[] s = project(mc, pos, w, h);
                if (s == null) continue;
                float fade = Math.max(0.25f, Math.min(1f, 1.4f - s[2] / 26f));
                String name = e instanceof RpgMob rm ? BestiaryScreen.displayName(rm.mobKey()) : e.getName().getString();
                String lv = "Nv." + md.level + " ";
                int lc = Ui.levelColor(md.level, ClientData.DATA.level);
                g.pose().pushMatrix();
                g.pose().translate(s[0], s[1]);
                g.pose().scale(0.75f, 0.75f);
                int tw = f.width(lv + name);
                g.fill(-tw / 2 - 2, -11, tw / 2 + 2, 0, Ui.alpha(0x90000000, fade));
                g.text(f, lv, -tw / 2, -10, Ui.alpha(lc, fade), false);
                g.text(f, name, -tw / 2 + f.width(lv), -10, Ui.alpha(0xFFFFFFFF, fade), false);
                g.pose().popMatrix();
                int bw = 36;
                int bx = (int) s[0] - bw / 2;
                int by = (int) s[1] + 1;
                g.fill(bx - 1, by - 1, bx + bw + 1, by + 3, Ui.alpha(0xFF000000, fade));
                g.fill(bx, by, bx + (int) (bw * md.hpFraction()), by + 2, Ui.alpha(md.hpFraction() > 0.5f ? 0xFF40D040 : md.hpFraction() > 0.25f ? 0xFFE0C030 : 0xFFE03030, fade));
            }
        }
        boolean anyReady = false;
        for (int i = 0; i < ClientData.quests.size(); i++) {
            if (ClientData.quests.getCompoundOrEmpty(i).getBooleanOr("completable", false)) anyReady = true;
        }
        for (com.mmorpg.entity.NpcEntity npc : NPCS) {
            if (npc.isRemoved()) continue;
            Vec3 pos = npc.getPosition(partial).add(0, npc.getBbHeight() + 0.45, 0);
            float[] s = project(mc, pos, w, h);
            if (s == null) continue;
            float fade = Math.max(0.3f, Math.min(1f, 1.5f - s[2] / 20f));
            String name = npc.displayName();
            boolean quest = npc.role() == com.mmorpg.entity.NpcEntity.Role.QUETES;
            String role = "<" + npc.role().label + ">";
            g.pose().pushMatrix();
            g.pose().translate(s[0], s[1]);
            g.pose().scale(0.8f, 0.8f);
            int tw = Math.max(f.width(name), f.width(role));
            g.fill(-tw / 2 - 3, -21, tw / 2 + 3, 0, Ui.alpha(0x90000000, fade));
            g.centeredText(f, Component.literal(name), 0, -20, Ui.alpha(0xFFFFE6A0, fade));
            g.centeredText(f, Component.literal(role), 0, -10, Ui.alpha(quest ? 0xFFFFD040 : 0xFF80E080, fade));
            g.pose().popMatrix();
            if (quest) {
                Ui.scaledText(g, anyReady ? "?" : "!", s[0], s[1] - 34, 2.0f, Ui.alpha(anyReady ? 0xFF60FF60 : 0xFFFFD020, fade), true);
            } else {
                g.pose().pushMatrix();
                g.pose().translate(s[0] - 6, s[1] - 32);
                g.pose().scale(0.75f, 0.75f);
                g.item(new net.minecraft.world.item.ItemStack(com.mmorpg.registry.ModItems.PIECE_OR.get()), 0, 0);
                g.pose().popMatrix();
            }
        }
        if (ClientConfig.SHOW_DAMAGE_NUMBERS.get()) {
            long now = System.currentTimeMillis();
            for (HudEffects.FloatingText t : HudEffects.texts()) {
                float life = (now - t.born) / (float) HudEffects.TEXT_LIFE_MS;
                Vec3 pos = new Vec3(t.x + t.jitterX, t.y + life * 1.1, t.z);
                float[] s = project(mc, pos, w, h);
                if (s == null) continue;
                float alpha = life < 0.7f ? 1f : 1f - (life - 0.7f) / 0.3f;
                String text;
                int color;
                float scale = 1.0f;
                switch (t.kind) {
                    case Payloads.CombatText.CRIT -> {
                        text = Ui.fmt(t.amount) + "!";
                        color = 0xFFFFC020;
                        scale = 1.5f;
                    }
                    case Payloads.CombatText.TAKEN -> {
                        text = "-" + Ui.fmt(t.amount);
                        color = 0xFFFF4040;
                    }
                    case Payloads.CombatText.HEAL -> {
                        text = "+" + Ui.fmt(t.amount);
                        color = 0xFF50F070;
                    }
                    case Payloads.CombatText.DODGE -> {
                        text = "Esquive";
                        color = 0xFF90FFE0;
                    }
                    case Payloads.CombatText.MANA -> {
                        text = "+" + Ui.fmt(t.amount);
                        color = 0xFF60A0FF;
                    }
                    case Payloads.CombatText.XP -> {
                        text = "+" + Ui.fmt(t.amount) + " XP";
                        color = 0xFFC080FF;
                        scale = 0.9f;
                    }
                    case Payloads.CombatText.GOLD -> {
                        text = "+" + Ui.fmt(t.amount) + " or";
                        color = 0xFFFFD040;
                        scale = 0.9f;
                    }
                    case Payloads.CombatText.IMMUNE -> {
                        text = "Immunisé";
                        color = 0xFFC0C0C0;
                    }
                    default -> {
                        text = Ui.fmt(t.amount);
                        color = 0xFFFFFFFF;
                    }
                }
                if (life < 0.15f) scale *= 1f + (0.15f - life) * 3f;
                Ui.scaledText(g, text, s[0], s[1], scale, Ui.alpha(color, alpha), true);
            }
        }
    }
}
