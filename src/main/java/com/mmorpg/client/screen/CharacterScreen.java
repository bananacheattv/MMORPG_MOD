package com.mmorpg.client.screen;

import com.mmorpg.client.ClientData;
import com.mmorpg.client.ClientNet;
import com.mmorpg.client.Keys;
import com.mmorpg.client.ui.Ui;
import com.mmorpg.client.ui.CharacterLayout;
import com.mmorpg.client.ui.CharacterLayout.Module;
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
            "Attribut du Paladin : augmente la Défense (+1), les PV (+5) et la régénération de vie (+0,08/s)."
    };

    private record Hit(int x, int y, int w, int h, Runnable action) {
    }

    private final EffectsInInventory effects;
    private final List<Hit> hits = new ArrayList<>();
    private int px, py, pw, ph;
    private int mx, my;
    private int cell=26,inventoryWidth=242,leftWidth=250,gridOffset;
    private final java.util.Map<Slot,int[]> originalPositions=new java.util.IdentityHashMap<>(),layoutPositions=new java.util.IdentityHashMap<>();
    private int attrW;
    private final java.util.EnumMap<Module,CharacterLayout.Rect> layoutRects=new java.util.EnumMap<>(Module.class);
    private final java.util.EnumMap<Module,float[]> layoutDefaults=new java.util.EnumMap<>(Module.class);
    private Module renderingModule,selectedModule;private boolean editingLayout,resizingModule,draggingLayout;private float grabX,grabY,dragStartScale;private double dragStartX,dragStartY;
    public boolean layoutEditing(){return editingLayout;}
    public void editLayout(){editingLayout=true;}


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
        com.mmorpg.server.InventoryRules.apply(this.minecraft.player);
        super.init();
        // meme panneau que les autres onglets ; les emplacements de Minecraft sont places sous les onglets, a gauche
        this.pw = com.mmorpg.client.ui.UiFrame.width(this.width);
        this.ph = com.mmorpg.client.ui.UiFrame.height(this.height);
        this.px = (this.width - pw) / 2;
        this.py = (this.height - ph) / 2;
        attrW = Math.max(104, (int) (pw * .22));
        this.leftPos = px + 8 + attrW + 10;
        this.topPos = py + 54;
        leftWidth=(int)(pw*.42);cell=Math.clamp((leftWidth-4)/9,20,44);inventoryWidth=cell*9+4;gridOffset=(leftWidth-inventoryWidth)/2;
        for(Slot slot:menu.slots){originalPositions.putIfAbsent(slot,new int[]{slot.x,slot.y});int sx=originalPositions.get(slot)[0],sy=originalPositions.get(slot)[1];
            if(slot.index>=9&&slot.index<36){sx=gridOffset+(slot.index-9)%9*cell;sy=90+(slot.index-9)/9*cell;}
            else if(slot.index>=36&&slot.index<45){sx=gridOffset+(slot.index-36)*cell;sy=95+3*cell;}
            else if(slot.index>=5&&slot.index<9){sx=0;sy=(slot.index-5)*20+4;}
            layoutPositions.put(slot,new int[]{sx,sy});var access=(com.mmorpg.mixin.SlotPositionAccessor)slot;access.aeloria$x(sx);access.aeloria$y(sy);
        }

    }

    @Override
    public void containerTick() {
        super.containerTick();

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
        prepareLayout();
        this.effects.extractRenderState(g, mouseX, mouseY);
        super.extractRenderState(g, mouseX, mouseY, a);
        renderLayoutControls(g);
    }

    private boolean hovered(int x, int y, int w, int h) {
        if(editingLayout)return false;return renderingModule==null?Ui.in(mx,my,x,y,w,h):layoutRects.get(renderingModule).visible()&&Ui.in(logicalX(),logicalY(),x,y,w,h);
    }

    private void button(GuiGraphicsExtractor g, int x, int y, int w, int h, String label, int accent, Runnable action) {
        Ui.button(g,x,y,w,h,label,true,(int)logicalX(),(int)logicalY(),accent);
        if(renderingModule==null)hits.add(new Hit(x,y,w,h,action));else{var r=layoutRects.get(renderingModule);var d=layoutDefaults.get(renderingModule);if(r.visible()&&!editingLayout)hits.add(new Hit(Math.round(r.x()+(x-d[0])*r.scale()),Math.round(r.y()+(y-d[1])*r.scale()),Math.round(w*r.scale()),Math.round(h*r.scale()),action));}
    }

    private void slotBox(GuiGraphicsExtractor g, int x, int y, int border, boolean big) {
        int s = big ? 24 : cell-2, o = big ? 2 : 0;
        g.fill(x - o, y - o, x - o + s, y - o + s, 0xE0100C14);
        Ui.border(g, x - o, y - o, s, s, border);
        g.fill(x - o + 1, y - o + s - 2, x - o + s - 1, y - o + s - 1, 0x30FFFFFF);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float a) {
        this.mx=mouseX;this.my=mouseY;prepareLayout();
        this.extractBlurredBackground(g);
        g.fill(0,0,width,height,0x700D071C);
        Ui.navBar(g, this.width, py);
        hits.clear();
        PlayerData d = ClientData.DATA;
        PlayerClass cls = d.playerClass;
        Ui.panel(g, px, py, pw, ph);
        MenuScreen.drawTabs(g, this.font, px, py, pw, MenuScreen.Tab.PERSONNAGE, mouseX, mouseY,
                (r, action) -> hits.add(new Hit(r[0], r[1], r[2], r[3], action)));
        g.fill(px + 6, py + 48, px + pw - 6, py + 49, Ui.LINE);

        // ------------------------------------------------ inventaire et equipement (emplacements de Minecraft)
        int x = this.leftPos, y = this.topPos;int bottom = py + ph - 30;
        beginModule(g,Module.EQUIPMENT);
        Ui.inset(g, x - 2, y - 2, inventoryWidth, 88);
        Ui.inset(g, x + 31, y + 2, 74, 82);
        InventoryScreen.extractEntityInInventoryFollowsMouse(g, x + 32, y + 3, x + 104, y + 83, 36, 0.0625F, mouseX, mouseY, this.minecraft.player);
        if (cls != com.mmorpg.rpg.PlayerClass.NONE) Ui.icon(g, Ui.classIcon(cls.id(), false), x + 26, y + 8, 12, 32);
        for (Slot s : this.menu.slots) {
            if (!s.isActive()) continue;
            boolean equipment = (s.index >= InventoryMenu.ARMOR_SLOT_START && s.index < InventoryMenu.ARMOR_SLOT_END) || s.index == InventoryMenu.SHIELD_SLOT;
            boolean result = s.index == InventoryMenu.RESULT_SLOT;
            if(equipment){var position=layoutPositions.get(s);Ui.frame(g,x+position[0]-1,y+position[1]-1,20,20,Ui.GOLD_DARK,false);}
        }
        g.text(font,"SACS À DOS",x+inventoryWidth-88,y+10,Ui.GOLD,false);
        button(g,x+inventoryWidth-88,y+25,75,18,"Ouvrir les sacs",Ui.GOLD,()->ClientNet.send(new Payloads.SocialAction("backpack",new net.minecraft.nbt.CompoundTag())));
        var stored=ClientData.DATA.backpacks.getListOrEmpty("items");
        var ops=net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE,minecraft.level.registryAccess());
        for(int i=0;i<2;i++){int sx=x+inventoryWidth-85+i*29;Ui.frame(g,sx,y+49,24,24,Ui.GOLD,false);if(i<stored.size()){var bag=net.minecraft.world.item.ItemStack.OPTIONAL_CODEC.parse(ops,stored.get(i)).result().orElse(net.minecraft.world.item.ItemStack.EMPTY);g.item(bag,sx+4,y+53);}}

        endModule(g);
        beginModule(g,Module.INVENTORY);Ui.inset(g,x+gridOffset-2,y+88,inventoryWidth,4*cell+13);
        for(Slot slot:menu.slots)if(slot.isActive()&&slot.index>=9&&slot.index<45){var at=layoutPositions.get(slot);slotBox(g,x+at[0]-1,y+at[1]-1,Ui.LINE,false);}
        g.fill(x+gridOffset, y + 91+3*cell, x+gridOffset + inventoryWidth-4, y + 92+3*cell, Ui.LINE);
        endModule(g);beginModule(g,Module.IDENTITY);

        // ------------------------------------------------ identite, barres et evolution (sous l'inventaire, selon la place)
        int under = y + 100+4*cell;

        if (true) {
            String lvl = "Niv. " + d.level;
            boolean roomy = true;           // classe sur sa propre ligne si la place le permet
            String name = this.minecraft.player.getName().getString();
            String evo = d.title();
            g.text(this.font, lvl, x + (inventoryWidth-4) - this.font.width(lvl), under, Ui.TEXT, true);
            if (roomy) {
                g.text(this.font, this.font.plainSubstrByWidth(name, (inventoryWidth-4) - 6 - this.font.width(lvl)), x, under, Ui.GOLD_LIGHT, true);
                g.text(this.font, this.font.plainSubstrByWidth(evo, (inventoryWidth-4)), x, under + 10, cls.color, false);
            } else {
                g.text(this.font, this.font.plainSubstrByWidth(name + "  ", 70), x, under, Ui.GOLD_LIGHT, true);
                g.text(this.font, this.font.plainSubstrByWidth(evo, (inventoryWidth-4) - 76 - this.font.width(lvl)), x + 72, under, cls.color, false);
            }
            int by = under + (roomy ? 21 : 11);
            float xp = d.level >= 100 ? 1 : d.xp / (float) Math.max(1, ClientData.xpToNext);
            Ui.bar(g, x, by, (inventoryWidth-4), 4, xp, 0xFFFFD860, 0xFFB07818, null);
            Ui.bar(g, x, by + 7, (inventoryWidth-10)/2, 8, ClientData.hp / Math.max(1, ClientData.maxHp), 0xFFE84848, 0xFF801818,
                    Ui.fmt(ClientData.hp) + " / " + Ui.fmt(ClientData.maxHp));
            Ui.bar(g, x + (inventoryWidth+2)/2, by + 7, (inventoryWidth-10)/2, 8, ClientData.mana / Math.max(1, ClientData.maxMana), 0xFF4C90FF, 0xFF1A3A90,
                    Ui.fmt(ClientData.mana) + " / " + Ui.fmt(ClientData.maxMana));
        }

        endModule(g);
        // ------------------------------------------------ attributs (repartition des points)
        int rx = px + 8;
        int w1 = attrW;
        int y0 = py + 54;
        int avail = (bottom - y0) - (ph >= 258 ? 54 : 0);
        beginModule(g,Module.ATTRIBUTES);
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

        endModule(g);
        // ------------------------------------------------ statistiques
        int x2 = x + leftWidth + 10;
        int w2 = px + pw - 8 - x2;
        beginModule(g,Module.STATISTICS);Ui.header(g, x2, y0, w2, "STATISTIQUES");
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

        endModule(g);
        // ------------------------------------------------ chemin d'evolution (si la place le permet)
        if (ph >= 258||CharacterLayout.box(Module.EVOLUTION).custom) {
            beginModule(g,Module.EVOLUTION);int ty = py + ph - 56;
            int tx0 = x2 + 8, tx1 = px + pw - 16;
            g.fill(tx0, ty + 4, tx1, ty + 6, Ui.LINE);
            int tier = d.evolutionTier();
            int reachedX = tx0 + (int) ((tx1 - tx0) * Math.min(1f, (d.level - 1) / 99f));
            g.fillGradient(tx0, ty + 4, reachedX, ty + 6, Ui.GOLD_LIGHT, Ui.GOLD_DARK);
            for (int i = 0; i < 5; i++) {
                int nx = tx0 + (tx1 - tx0) * i / 4;
                boolean reached = i <= tier;
                Ui.diamond(g, nx, ty + 5, i == tier ? 5 : 4, reached ? Ui.GOLD : 0xFF4A4038);
                Ui.diamond(g, nx, ty + 5, i == tier ? 2 : 1, reached ? (i == tier ? 0xFFFFFFFF : cls.color) : 0xFF201818);
                int ly = i % 2 == 0 ? ty + 10 : ty - 22;     // etiquettes alternees au-dessus / au-dessous de la ligne
                int labelX=Math.clamp(nx,x2+font.width(cls.evolutions[i])/2,px+pw-9-font.width(cls.evolutions[i])/2);
                Ui.scaledText(g, cls.evolutions[i], labelX, ly, 1f, i == tier ? cls.color : reached ? Ui.TEXT : Ui.MUTED, true);
                Ui.scaledText(g, "Niv. " + PlayerClass.EVOLUTION_LEVELS[i], labelX, ly + 10, 1f, Ui.MUTED, true);
            }
            endModule(g);
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor g, int xm, int ym) {
        // Crafting is handled at the forge; this space is reserved for equipped backpacks.
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if(editingLayout){for(int i=hits.size()-1;i>=0;i--){var hit=hits.get(i);if(Ui.in(event.x(),event.y(),hit.x,hit.y,hit.w,hit.h)){hit.action.run();return true;}}if(event.button()==com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT){var modules=Module.values();for(int i=modules.length-1;i>=0;i--){var module=modules[i];var r=layoutRects.get(module);if(r!=null&&r.contains(event.x(),event.y())){selectedModule=module;draggingLayout=true;if(event.x()>r.x()+r.w()-16&&event.y()<r.y()+17){CharacterLayout.box(module).visible=!CharacterLayout.box(module).visible;CharacterLayout.save();return true;}resizingModule=event.x()>r.x()+r.w()-14&&event.y()>r.y()+r.h()-14;grabX=(float)(event.x()-r.x());grabY=(float)(event.y()-r.y());dragStartX=event.x();dragStartY=event.y();dragStartScale=r.scale();return true;}}}return true;}
        for(var slot:menu.slots)if(slot.isActive()&&isHovering(slot.x,slot.y,16,16,event.x(),event.y()))return super.mouseClicked(event,doubleClick);
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
        return new int[]{this.leftPos+s.x+Math.round(8*layoutRects.get(slotModule(s)).scale()),this.topPos+s.y+Math.round(8*layoutRects.get(slotModule(s)).scale())};
    }

    @Override public void removed(){for(var e:originalPositions.entrySet()){var a=(com.mmorpg.mixin.SlotPositionAccessor)e.getKey();a.aeloria$x(e.getValue()[0]);a.aeloria$y(e.getValue()[1]);}CharacterLayout.save();super.removed();}
    @Override protected boolean isHovering(int x,int y,int w,int h,double mx,double my){if(editingLayout)return false;if(w==16&&h==16){var slot=menu.slots.stream().filter(v->v.x==x&&v.y==y).findFirst().orElse(null);if(slot!=null){var r=layoutRects.get(slotModule(slot));int size=Math.round((slot.index>=9&&slot.index<45?cell-2:20)*r.scale());return super.isHovering(x,y,size,size,mx,my);}}return super.isHovering(x,y,w,h,mx,my);}
    @Override protected void renderSlotContents(GuiGraphicsExtractor g,net.minecraft.world.item.ItemStack stack,Slot slot,String count){
        g.pose().pushMatrix();g.pose().translate(slot.x,slot.y);float scale=(slot.index>=9&&slot.index<45?(cell-5)/16f:1f)*layoutRects.get(slotModule(slot)).scale();g.pose().scale(scale,scale);g.pose().translate(-slot.x,-slot.y);super.renderSlotContents(g,stack,slot,count);g.pose().popMatrix();
    }

    /** Full-sized hover and click target share the same client-side slot coordinates. */
    public void slotHighlight(GuiGraphicsExtractor g,boolean front){
        if(editingLayout||hoveredSlot==null||!hoveredSlot.isHighlightable())return;
        var r=layoutRects.get(slotModule(hoveredSlot));if(r==null||!r.visible())return;
        int size=Math.round((hoveredSlot.index>=9&&hoveredSlot.index<45?cell-2:20)*r.scale());
        int x=hoveredSlot.x-Math.round(r.scale()),y=hoveredSlot.y-Math.round(r.scale());
        if(front)Ui.border(g,x,y,size,size,0xB0D4B2FF);else g.fill(x,y,x+size,y+size,0x303E2B55);
    }

    private Module slotModule(Slot slot){return slot.index>=9&&slot.index<45?Module.INVENTORY:Module.EQUIPMENT;}
    private void defaults(Module module,float x,float y,float w,float h){layoutDefaults.put(module,new float[]{x,y,w,h});var r=CharacterLayout.resolve(module,x,y,w,h,width,height);if(module==Module.IDENTITY&&!CharacterLayout.box(module).custom&&y+h>py+ph-29)r=new CharacterLayout.Rect(r.x(),r.y(),r.w(),r.h(),r.scale(),false);if(module==Module.EVOLUTION&&ph<258&&!CharacterLayout.box(module).custom)r=new CharacterLayout.Rect(r.x(),r.y(),r.w(),r.h(),r.scale(),false);layoutRects.put(module,r);}
    private void prepareLayout(){
        int x=leftPos,y=topPos,rx=px+8,x2=x+leftWidth+10,w2=px+pw-8-x2,w1=attrW,y0=py+54,avail=py+ph-30-y0-(ph>=258?54:0);
        int rowH=Math.max(11,Math.min(18,(avail-24)/Stat.PRIMARIES_DISPLAY.length)),step=Math.max(9,Math.min(11,(avail-14)/14));
        defaults(Module.EQUIPMENT,x,y,inventoryWidth,88);defaults(Module.INVENTORY,x+gridOffset,y+90,inventoryWidth,4*cell+12);defaults(Module.IDENTITY,x,y+100+4*cell,inventoryWidth,34);
        defaults(Module.ATTRIBUTES,rx,y0,w1,24+rowH*Stat.PRIMARIES_DISPLAY.length);defaults(Module.STATISTICS,x2,y0,Math.max(30,w2),14+step*14);defaults(Module.EVOLUTION,x2,py+ph-78,Math.max(30,w2),53);
        for(var e:layoutPositions.entrySet()){var slot=e.getKey();var module=slotModule(slot);var r=layoutRects.get(module);var d=layoutDefaults.get(module);var a=(com.mmorpg.mixin.SlotPositionAccessor)slot;int[] base=e.getValue();a.aeloria$x(r.visible()?Math.round(r.x()+(leftPos+base[0]-d[0])*r.scale()-leftPos):-10000);a.aeloria$y(r.visible()?Math.round(r.y()+(topPos+base[1]-d[1])*r.scale()-topPos):-10000);}
    }
    private double logicalX(){if(renderingModule==null)return mx;var r=layoutRects.get(renderingModule);return layoutDefaults.get(renderingModule)[0]+(mx-r.x())/r.scale();}
    private double logicalY(){if(renderingModule==null)return my;var r=layoutRects.get(renderingModule);return layoutDefaults.get(renderingModule)[1]+(my-r.y())/r.scale();}
    private void beginModule(GuiGraphicsExtractor g,Module module){renderingModule=module;var d=layoutDefaults.get(module);CharacterLayout.push(g,layoutRects.get(module),d[0],d[1]);}
    private void endModule(GuiGraphicsExtractor g){CharacterLayout.pop(g,layoutRects.get(renderingModule));renderingModule=null;}
    private void renderLayoutControls(GuiGraphicsExtractor g){
        if(editingLayout){for(var module:Module.values()){var r=layoutRects.get(module);int x=Math.round(r.x()),y=Math.round(r.y()),w=Math.round(r.w()),h=Math.round(r.h());Ui.border(g,x-2,y-2,w+4,h+4,module==selectedModule?Ui.GOLD:Ui.BLUE);Ui.rounded(g,x,y,w,17,4,0xE52D2241);g.text(font,font.plainSubstrByWidth(module.label,w-23),x+4,y+4,r.visible()?Ui.GOLD:Ui.MUTED,false);g.text(font,r.visible()?"×":"○",x+w-12,y+3,Ui.TEXT,false);Ui.rounded(g,x+w-13,y+h-13,13,13,3,Ui.GOLD_DARK);g.text(font,"↘",x+w-11,y+h-11,Ui.GOLD,false);}
            button(g,px+8,py+ph-22,105,18,"Réinitialiser",Ui.RED,()->{CharacterLayout.reset();selectedModule=null;});g.text(font,"Glisser : déplacer · Coin / molette : taille · × : masquer",px+120,py+ph-17,Ui.MUTED,false);
        }
        button(g,px+pw-112,py+ph-22,104,18,editingLayout?"Terminer":"Personnaliser",Ui.GOLD,()->{if(!menu.getCarried().isEmpty())return;editingLayout=!editingLayout;selectedModule=null;draggingLayout=false;CharacterLayout.save();});
    }
    @Override public boolean mouseDragged(MouseButtonEvent event,double dx,double dy){if(editingLayout){if(selectedModule!=null&&draggingLayout){if(resizingModule){var d=layoutDefaults.get(selectedModule);CharacterLayout.scale(selectedModule,dragStartScale+(float)(((event.x()-dragStartX)/Math.max(1,d[2])+(event.y()-dragStartY)/Math.max(1,d[3]))/2));}else CharacterLayout.move(selectedModule,(float)event.x()-grabX,(float)event.y()-grabY,width,height);}return true;}return super.mouseDragged(event,dx,dy);}
    @Override public boolean mouseReleased(MouseButtonEvent event){if(editingLayout){draggingLayout=false;CharacterLayout.save();return true;}return super.mouseReleased(event);}
    @Override public boolean mouseScrolled(double x,double y,double sx,double sy){if(editingLayout){if(selectedModule!=null){CharacterLayout.scale(selectedModule,CharacterLayout.box(selectedModule).scale+(float)Math.signum(sy)*.05f);CharacterLayout.save();}return true;}return super.mouseScrolled(x,y,sx,sy);}

    @Override
    public boolean keyPressed(KeyEvent event) {
        if(editingLayout&&event.input()==com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE){editingLayout=false;selectedModule=null;CharacterLayout.save();return true;}
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
