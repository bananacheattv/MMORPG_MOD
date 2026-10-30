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
    private static final List<com.mmorpg.entity.dummy.TrainingDummyEntity> DUMMIES = new ArrayList<>();
    /** Valeurs affichees (adoucies vers les vraies) par mannequin : {dps, total, meilleur coup}. */
    private static final java.util.Map<Integer, float[]> DUMMY_SHOWN = new java.util.HashMap<>();
    private static long lastDummyNanos;
    private static LivingEntity lastTarget;
    private static long lastTargetTime;
    private static LivingEntity boss;

    private MmoHud() {
    }

    // ================================================================== mise a jour (tick client)

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null) {
            VISIBLE.clear();
            DUMMIES.clear();
            DUMMY_SHOWN.clear();
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
        DUMMIES.clear();
        int dummyRange = com.mmorpg.config.ConfigManager.dummy().panelRange;
        LivingEntity bestBoss = null;
        double bestDist = 64 * 64;
        for (Entity e : mc.level.entitiesForRendering()) {
            if (e instanceof com.mmorpg.entity.NpcEntity npc && npc.distanceToSqr(mc.player) < 24 * 24 && mc.player.hasLineOfSight(npc)) {
                NPCS.add(npc);
                continue;
            }
            if (e instanceof com.mmorpg.entity.dummy.TrainingDummyEntity dummy) {          // mannequin : panneau dedie, pas de barre de vie
                if (dummy.distanceToSqr(mc.player) < (double) dummyRange * dummyRange && mc.player.hasLineOfSight(dummy)) DUMMIES.add(dummy);
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
        if(!HudLayout.editing)GuidanceArrow.render(g,mc);
        ArenaHud.draw(g);
        if(ClientData.received&&ClientData.DATA.playerClass==PlayerClass.TANK&&com.mmorpg.server.PaladinOffhand.shield(mc.player.getOffhandItem())){int gx=w/2-80,gy=h-76;Ui.rounded(g,gx-4,gy-4,168,22,6,0xDB171124);Ui.bar(g,gx,gy,160,14,ClientData.DATA.shieldGuard/100f,ClientData.guardBroken>0?Ui.RED:Ui.GOLD,0xFF493746,null);}
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
        if (HudLayout.begin(g, HudLayout.AELORIA)) {
            aeloriaBoard(g, mc);
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
        if ((ClientData.trialActive || HudLayout.editing) && HudLayout.begin(g, HudLayout.ESSAI)) {
            trialBanner(g);
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
        Ui.rounded(g,x,y,w,h,7,0xB5211734);
        Ui.border(g, x, y, w, h, Ui.LINE);
        g.fill(x, y, x + 2, y + h, Ui.withAlpha(accent, 0xE0));
    }

    // ------------------------------------------------------------------ tailles (pour l'ancrage et l'editeur)

    private static int activeBuffs() {
        long now = ClientData.now();
        int n = 0;
        for (long[] v : ClientData.BUFFS.values()) {
            if (v[0] - now > 0) n++;
        }
        var player=Minecraft.getInstance().player;if(player!=null)n+=player.getActiveEffects().size();return n;
    }

    static int[] buffsSize() {
        int n = activeBuffs();
        if (n == 0 && HudLayout.editing) n = 3;
        return new int[]{n<=1?168:336,Math.max(1,(n+1)/2)*24};
    }

    private static boolean targetShown() {
        LivingEntity t = lastTarget;
        return t != null && t.isAlive() && !(t instanceof com.mmorpg.entity.NpcEntity) && !(t instanceof com.mmorpg.entity.dummy.TrainingDummyEntity);
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
        if (cls != com.mmorpg.rpg.PlayerClass.NONE) Ui.icon(g, Ui.classIcon(cls.id(), false), x + 5, y + 5, 22, 32);
        Ui.border(g, x + 4, y + 4, 24, 24, Ui.GOLD_DARK);
        String lvl = String.valueOf(d.level);
        g.fill(x + 4, y + 22, x + 6 + f.width(lvl) + 2, y + 31, 0xE0101010);
        g.text(f, lvl, x + 6, y + 23, Ui.GOLD_LIGHT, true);
        g.text(f, Component.literal(f.plainSubstrByWidth(mc.player.getName().getString(),70)).withColor(d.rankColor).withStyle(style->style.withBold(true)),x+32,y+5,0xFF000000|d.rankColor,true);   // rankColor est en RVB : sans alpha, text() ne dessine rien
        String title = d.title();
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
        int index=0;long now=ClientData.now();
        for(var e:ClientData.BUFFS.entrySet()){long rem=e.getValue()[0]-now;if(rem<=0)continue;var type=e.getKey();effectCard(g,index++,type.shortLabel(),type.icon,type.color,type.harmful(),(int)Math.ceil(rem/20.0));}
        var player=Minecraft.getInstance().player;
        if(player!=null)for(var effect:player.getActiveEffects()){
            String label=effect.getEffect().value().getDisplayName().getString();var words=label.split("\\s+");label=String.join(" ",java.util.Arrays.copyOf(words,Math.min(3,words.length)));
            boolean harmful=!effect.getEffect().value().isBeneficial();effectCard(g,index++,label,harmful?"tir_entravant":"garde_partagee",harmful?Ui.RED:Ui.GREEN,harmful,effect.isInfiniteDuration()?-1:(int)Math.ceil(effect.getDuration()/20.0));
        }
        if(index==0&&HudLayout.editing){effectCard(g,index++,"Défense réduite","frappe_fragilisante",Ui.RED,true,12);effectCard(g,index++,"Attaque renforcée","cri_de_guerre",Ui.GREEN,false,24);}
    }
    private static void effectCard(GuiGraphicsExtractor g,int index,String label,String icon,int color,boolean harmful,int seconds){
        int x=index%2*168,y=index/2*24;Ui.rounded(g,x,y,164,22,4,harmful?0xDC321528:0xDC162B2A);Ui.border(g,x,y,164,22,color);
        Ui.icon(g,Ui.skillIcon(icon),x+3,y+3,16,32);g.text(font(),font().plainSubstrByWidth(label,112),x+23,y+3,color,false);
        g.text(font(),harmful?"DÉBUFF":"BUFF",x+23,y+12,Ui.MUTED,false);String duration=seconds<0?"∞":seconds>=60?seconds/60+":"+String.format(java.util.Locale.ROOT,"%02d",seconds%60):seconds+"s";g.text(font(),duration,x+160-font().width(duration),y+10,Ui.TEXT,true);
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
        if(t instanceof com.mmorpg.competition.ArenaCrystal crystal){name="Capture du cristal";level=0;frac=crystal.team()<0?crystal.hits()/50f:1;hpText=crystal.team()<0?crystal.hits()+" / 50 coups":crystal.team()==0?"Contrôle rouge":"Contrôle bleu";}else if (md != null && md.initialized()) {
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
                name += " — " + c.evolutions[Math.min(PlayerClass.tier(pub.level), pub.evolution)];
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

    private static void aeloriaBoard(GuiGraphicsExtractor g, Minecraft mc) {
        var d = ClientData.DATA;
        Ui.frame(g, 0, 0, 153, 44, Ui.GOLD, false);
        g.text(mc.font, "✦ AELORIA", 5, 4, Ui.GOLD, false);
        int online = mc.getConnection() == null ? 0 : mc.getConnection().getOnlinePlayers().size();
        String count = online + " en ligne";
        g.text(mc.font, count, 148 - mc.font.width(count), 4, Ui.MUTED, false);
        g.text(mc.font, "Niv. " + d.level + " • " + d.playerClass.label, 5, 14, d.playerClass.color, false);
        g.text(mc.font, "Or : " + (d.gold >= 1000000 ? (d.gold / 1000000) + "M" : d.gold >= 10000 ? (d.gold / 1000) + "k" : Long.toString(d.gold)), 5, 24, Ui.GOLD, false);
        long ready = java.util.stream.IntStream.range(0, ClientData.quests.size()).filter(i -> ClientData.quests.getCompoundOrEmpty(i).getBooleanOr("completable", false)).count();
        g.text(mc.font, "Quêtes : " + d.activeQuests.size() + " • " + ready + " à rendre", 5, 34, ready > 0 ? Ui.GREEN : Ui.TEXT, false);
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
        if (online && cls != com.mmorpg.rpg.PlayerClass.NONE) Ui.icon(g, Ui.classIcon(cls.id(), false), 3, y + 4, 16, 32, near ? 0xFFFFFFFF : 0xFF808080);
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
        Ui.rounded(g,x-3,y-2,w+3,h+2,6,0xA0221735);
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
        Ui.rounded(g,x-6,y-2,w+12,32,7,0xB52A163B);
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
        Ui.rounded(g,x-3,y-3,total+6,slot+6,7,0xB524193A);
        Ui.border(g, x - 3, y - 3, total + 6, slot + 6, Ui.GOLD_DARK);
        for (int i = 0; i < PlayerData.SKILL_SLOTS; i++) {
            int sx = x + i * (slot + gap);
            Ui.rounded(g,sx,y,slot,slot,5,0xB519112C);
            Ui.border(g, sx, y, slot, slot, 0xFF2B2538);
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

    /** Indicateur d'essai de classe : glisse depuis le haut a l'apparition, losange qui pulse, duree ecoulee. */
    private static void trialBanner(GuiGraphicsExtractor g) {
        PlayerClass cls = ClientData.trialActive ? PlayerClass.byId(ClientData.trialClass) : PlayerClass.GUERRIER;
        int level = ClientData.trialActive ? ClientData.trialLevel : 50;
        float k = Math.min(1f, (System.nanoTime() - ClientData.trialSince) / 4.0E8f);
        float slide = HudLayout.editing ? 0f : (1f - (1f - (float) Math.pow(1f - k, 3))) * -34f;
        g.pose().pushMatrix();
        g.pose().translate(0, slide);
        Font f = font();
        int w = 210;
        int h = 30;
        smallPanel(g, 0, 0, w, h, cls.color);
        float pulse = 0.5f + 0.5f * (float) Math.sin(System.currentTimeMillis() / 260.0);
        Ui.diamond(g, 10, 15, 4, Ui.lighter(cls.color, 0.2f + 0.5f * pulse));
        g.text(f, com.mmorpg.client.AeloriaFont.of("ESSAI — " + cls.label + " niv. " + level), 20, 5, cls.color, true);
        long secs = HudLayout.editing ? 83 : (System.nanoTime() - ClientData.trialSince) / 1_000_000_000L;
        String time = String.format(java.util.Locale.ROOT, "%d:%02d", secs / 60, secs % 60);
        g.text(f, time, w - 8 - f.width(time), 5, Ui.MUTED, false);
        g.pose().pushMatrix();
        g.pose().translate(20, 18);
        g.pose().scale(0.75f, 0.75f);
        g.text(f, "Rien n'est conservé · parlez au Gardien pour quitter", 0, 0, Ui.TEXT, false);
        g.pose().popMatrix();
        g.pose().popMatrix();
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

    /** Panneaux de statistiques au-dessus des mannequins : DPS, total, plus gros coup, critiques (valeurs adoucies a chaque image). */
    private static void dummyPanels(GuiGraphicsExtractor g, Minecraft mc, int w, int h, float partial) {
        if (DUMMIES.isEmpty()) return;
        long now = System.nanoTime();
        float dt = lastDummyNanos == 0 ? 0f : Math.min(0.1f, (now - lastDummyNanos) / 1.0E9f);
        lastDummyNanos = now;
        float ease = 1f - (float) Math.exp(-dt * 10f);
        Font f = font();
        for (com.mmorpg.entity.dummy.TrainingDummyEntity d : DUMMIES) {
            if (d.isRemoved()) continue;
            Vec3 pos = d.getPosition(partial).add(0, d.getBbHeight() + 0.55, 0);
            float[] s = project(mc, pos, w, h);
            if (s == null) continue;
            float[] shown = DUMMY_SHOWN.computeIfAbsent(d.getId(), k -> new float[]{d.dps(), d.total(), d.best()});
            shown[0] += (d.dps() - shown[0]) * ease;
            shown[1] += (d.total() - shown[1]) * ease;
            shown[2] += (d.best() - shown[2]) * ease;
            float fade = Math.max(0.35f, Math.min(1f, 1.5f - s[2] / 28f));
            float scale = Math.max(0.55f, Math.min(1.1f, 6.5f / Math.max(4f, s[2])));
            int bw = 104;
            int bh = 58;
            g.pose().pushMatrix();
            g.pose().translate(s[0], s[1]);
            g.pose().scale(scale, scale);
            int x = -bw / 2;
            int y = -bh;
            // reste dans l'ecran (de pres, la tete du mannequin sort par le haut)
            float top = s[1] + y * scale;
            if (top < 4) g.pose().translate(0, 4 - top);
            Ui.rounded(g,x,y,bw,bh,7,Ui.alpha(0xB5211734,fade));
            Ui.border(g, x, y, bw, bh, Ui.alpha(Ui.LINE, fade));
            g.fill(x, y, x + 2, y + bh, Ui.alpha(Ui.GOLD, fade));
            g.centeredText(f, Component.literal(d.config().name), 0, y + 4, Ui.alpha(Ui.GOLD_LIGHT, fade));
            g.fill(x + 6, y + 14, x + bw - 6, y + 15, Ui.alpha(Ui.GOLD_DARK, fade));
            String dps = Ui.fmt(Math.round(shown[0]));
            Ui.scaledText(g, dps, 0, y + 18, 1.6f, Ui.alpha(d.dps() > 0 ? 0xFFFFD060 : Ui.MUTED, fade), true);
            g.centeredText(f, Component.literal("DPS (" + com.mmorpg.config.ConfigManager.dummy().dpsWindowSeconds + " s)"), 0, y + 31, Ui.alpha(Ui.MUTED, fade));
            g.text(f, "Total", x + 6, y + 41, Ui.alpha(Ui.TEXT, fade), false);
            String total = Ui.fmt(Math.round(shown[1]));
            g.text(f, total, x + bw - 6 - f.width(total), y + 41, Ui.alpha(0xFFFFFFFF, fade), false);
            g.pose().pushMatrix();
            g.pose().translate(x + 6, y + 50);
            g.pose().scale(0.75f, 0.75f);
            int crits = d.crits();
            int hits = d.hits();
            String left = "Max " + Ui.fmt(Math.round(shown[2]));
            String right = "Crit " + crits + "/" + hits + (hits > 0 ? " (" + Math.round(100f * crits / hits) + " %)" : "");
            g.text(f, left, 0, 0, Ui.alpha(0xFFE8C060, fade), false);
            g.text(f, right, (int) ((bw - 12) / 0.75f) - f.width(right), 0, Ui.alpha(0xFFFF9060, fade), false);
            g.pose().popMatrix();
            g.pose().popMatrix();
        }
        if (DUMMY_SHOWN.size() > 64) DUMMY_SHOWN.keySet().removeIf(id -> DUMMIES.stream().noneMatch(e -> e.getId() == id));
    }

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
        dummyPanels(g, mc, w, h, partial);
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
            boolean guide = npc instanceof com.mmorpg.entity.hub.HubGuideEntity;
            boolean quest = !guide && npc.role() == com.mmorpg.entity.NpcEntity.Role.QUETES;
            String role = guide ? "<Guide · clic droit>" : "<" + npc.role().label + ">";
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
                g.item(new net.minecraft.world.item.ItemStack(guide ? net.minecraft.world.item.Items.WRITABLE_BOOK : com.mmorpg.registry.ModItems.PIECE_OR.get()), 0, 0);
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
