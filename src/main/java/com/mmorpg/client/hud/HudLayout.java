package com.mmorpg.client.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.mmorpg.MMORPG;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Disposition de l'interface en jeu : chaque element (HUD du mod et elements vanilla) peut etre deplace,
 * redimensionne et masque depuis l'ecran d'edition, puis est sauvegarde dans config/mmorpg-hud.json.
 *
 * <p>Une position personnalisee est stockee par rapport a un point d'ancrage (gauche / centre / droite,
 * haut / centre / bas) pour rester coherente quand la taille de la fenetre change.</p>
 */
public final class HudLayout {
    public static final float MIN_SCALE = 0.5f;
    public static final float MAX_SCALE = 2.0f;

    /** Taille actuelle du contenu (non mise a l'echelle) : {largeur, hauteur}. */
    @FunctionalInterface
    public interface Size {
        int[] get(int screenW, int screenH);
    }

    /** Position par defaut (coin haut-gauche) pour une taille de contenu donnee. */
    @FunctionalInterface
    public interface Pos {
        int[] get(int screenW, int screenH, int w, int h);
    }

    public static final class Element {
        public final String id;
        public final String label;
        final Identifier[] layers;
        final Size size;
        final Pos def;
        boolean custom;
        float ax, ay, ox, oy;
        float scale = 1f;
        boolean visible = true;

        Element(String id, String label, Identifier[] layers, Size size, Pos def) {
            this.id = id;
            this.label = label;
            this.layers = layers;
            this.size = size;
            this.def = def;
        }

        public boolean vanilla() {
            return layers.length > 0;
        }

        public boolean visible() {
            return visible;
        }

        public float scale() {
            return scale;
        }

        public boolean customized() {
            return custom || scale != 1f || !visible;
        }

        public int[] contentSize(int sw, int sh) {
            return size.get(sw, sh);
        }

        public int[] defaultPos(int sw, int sh) {
            int[] s = size.get(sw, sh);
            return def.get(sw, sh, s[0], s[1]);
        }

        /** Rectangle a l'ecran {x, y, largeur, hauteur} une fois deplace et mis a l'echelle (borne a l'ecran). */
        public int[] rect(int sw, int sh) {
            int[] s = size.get(sw, sh);
            float w = s[0] * scale;
            float h = s[1] * scale;
            float x;
            float y;
            if (custom) {
                x = ax * (sw - w) + ox;
                y = ay * (sh - h) + oy;
            } else {
                int[] p = def.get(sw, sh, s[0], s[1]);
                x = p[0];
                y = p[1];
            }
            if (w <= sw) x = Math.max(0, Math.min(sw - w, x));
            if (h <= sh) y = Math.max(0, Math.min(sh - h, y));
            return new int[]{Math.round(x), Math.round(y), Math.round(w), Math.round(h)};
        }

        /** Place le coin haut-gauche a (x, y) et choisit l'ancrage le plus proche (tiers de l'ecran). */
        public void moveTo(float x, float y, int sw, int sh) {
            int[] s = size.get(sw, sh);
            float w = s[0] * scale;
            float h = s[1] * scale;
            float cx = x + w / 2f;
            float cy = y + h / 2f;
            ax = cx < sw / 3f ? 0f : cx > sw * 2f / 3f ? 1f : 0.5f;
            ay = cy < sh / 3f ? 0f : cy > sh * 2f / 3f ? 1f : 0.5f;
            ox = x - ax * (sw - w);
            oy = y - ay * (sh - h);
            custom = true;
        }

        /** Change l'echelle en gardant le centre de l'element au meme endroit. */
        public void setScale(float newScale, int sw, int sh) {
            int[] r = rect(sw, sh);
            float cx = r[0] + r[2] / 2f;
            float cy = r[1] + r[3] / 2f;
            scale = Math.round(Math.max(MIN_SCALE, Math.min(MAX_SCALE, newScale)) * 20f) / 20f;
            int[] s = size.get(sw, sh);
            moveTo(cx - s[0] * scale / 2f, cy - s[1] * scale / 2f, sw, sh);
        }

        public void toggleVisible() {
            visible = !visible;
        }

        public void reset() {
            custom = false;
            ax = ay = ox = oy = 0;
            scale = 1f;
            visible = true;
        }

        JsonObject toJson() {
            JsonObject o = new JsonObject();
            o.addProperty("visible", visible);
            o.addProperty("scale", scale);
            if (custom) {
                o.addProperty("ancreX", ax);
                o.addProperty("ancreY", ay);
                o.addProperty("decalageX", ox);
                o.addProperty("decalageY", oy);
            }
            return o;
        }

        void fromJson(JsonObject o) {
            reset();
            visible = !o.has("visible") || o.get("visible").getAsBoolean();
            if (o.has("scale")) scale = Math.max(MIN_SCALE, Math.min(MAX_SCALE, o.get("scale").getAsFloat()));
            if (o.has("ancreX")) {
                custom = true;
                ax = o.get("ancreX").getAsFloat();
                ay = o.get("ancreY").getAsFloat();
                ox = o.get("decalageX").getAsFloat();
                oy = o.get("decalageY").getAsFloat();
            }
        }
    }

    private static final List<Element> ELEMENTS = new ArrayList<>();
    private static final Map<Identifier, Element> BY_LAYER = new HashMap<>();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static boolean loaded;
    /** Vrai pendant l'edition : les elements vides affichent un apercu pour pouvoir etre places. */
    public static boolean editing;

    // ---------------------------------------------------------------- elements du mod (dessines par MmoHud)
    public static final Element JOUEUR = mod("joueur", "Cadre du joueur", (sw, sh) -> new int[]{168, 58}, (sw, sh, w, h) -> new int[]{4, 4});
    public static final Element BUFFS = mod("buffs", "Effets du personnage", (sw, sh) -> MmoHud.buffsSize(), (sw, sh, w, h) -> new int[]{5, 65});
    public static final Element CIBLE = mod("cible", "Cadre de la cible", (sw, sh) -> new int[]{168, 30}, (sw, sh, w, h) -> new int[]{4, MmoHud.defaultTargetY()});
    public static final Element GROUPE = mod("groupe", "Membres du groupe", (sw, sh) -> MmoHud.partySize(), (sw, sh, w, h) -> new int[]{4, MmoHud.defaultPartyY()});
    public static final Element ELDORIA = mod("eldoria", "Tableau Eldoria", (sw, sh) -> new int[]{153, 44}, (sw, sh, w, h) -> new int[]{sw - w - 4, 4});
    public static final Element QUETES = mod("quetes", "Suivi des quêtes", (sw, sh) -> MmoHud.questSize(), (sw, sh, w, h) -> new int[]{sw - w - 4, 52});
    public static final Element BOSS = mod("boss", "Barre de boss", (sw, sh) -> new int[]{272, 34},
            (sw, sh, w, h) -> new int[]{sw >= 2 * 186 + w ? (sw - w) / 2 : 180, 4});
    public static final Element SORTS = mod("sorts", "Barre de sorts", (sw, sh) -> new int[]{136, 26}, (sw, sh, w, h) ->
            sw / 2 + 96 + w <= sw - 2 ? new int[]{sw / 2 + 96, sh - 25} : new int[]{sw / 2 - w / 2, sh - 73});
    public static final Element BANNIERE = mod("banniere", "Annonces", (sw, sh) -> MmoHud.bannerSize(), (sw, sh, w, h) -> new int[]{(sw - w) / 2, sh / 4 - 7});

    // ---------------------------------------------------------------- elements vanilla (deplaces par transformation)
    public static final Element HOTBAR = vanilla("hotbar", "Barre d'objets", (sw, sh) -> new int[]{182, 22}, (sw, sh, w, h) -> new int[]{sw / 2 - 91, sh - 22},
            VanillaGuiLayers.HOTBAR, VanillaGuiLayers.SELECTED_ITEM_NAME, VanillaGuiLayers.SPECTATOR_TOOLTIP);
    public static final Element XP = vanilla("experience", "Barre d'expérience", (sw, sh) -> new int[]{182, 12}, (sw, sh, w, h) -> new int[]{sw / 2 - 91, sh - 36},
            VanillaGuiLayers.CONTEXTUAL_INFO_BAR_BACKGROUND, VanillaGuiLayers.EXPERIENCE_LEVEL, VanillaGuiLayers.CONTEXTUAL_INFO_BAR);
    public static final Element FAIM = vanilla("faim", "Faim, air et monture", (sw, sh) -> new int[]{81, 19}, (sw, sh, w, h) -> new int[]{sw / 2 + 10, sh - 49},
            VanillaGuiLayers.FOOD_LEVEL, VanillaGuiLayers.AIR_LEVEL, VanillaGuiLayers.VEHICLE_HEALTH);
    public static final Element EFFETS = vanilla("effets", "Effets de potion", (sw, sh) -> MmoHud.vanillaEffectsSize(), (sw, sh, w, h) -> new int[]{sw - w, 1},
            VanillaGuiLayers.EFFECTS);
    public static final Element BARRE_BOSS = vanilla("barre_boss", "Barres de boss vanilla", (sw, sh) -> new int[]{182, 30}, (sw, sh, w, h) -> new int[]{sw / 2 - 91, 2},
            VanillaGuiLayers.BOSS_OVERLAY);
    public static final Element TABLEAU = vanilla("tableau", "Tableau des scores", (sw, sh) -> new int[]{100, 90}, (sw, sh, w, h) -> new int[]{sw - 101, sh / 2 - 50},
            VanillaGuiLayers.SCOREBOARD_SIDEBAR);
    public static final Element MESSAGE = vanilla("message", "Message d'action", (sw, sh) -> new int[]{200, 12}, (sw, sh, w, h) -> new int[]{sw / 2 - 100, sh - 74},
            VanillaGuiLayers.OVERLAY_MESSAGE);
    /** Messages recus (chat ferme) ; une fois le chat ouvert, Minecraft l'affiche a sa place habituelle pour la saisie. */
    public static final Element CHAT = vanilla("chat", "Messages du chat", (sw, sh) -> chatSize(), (sw, sh, w, h) -> new int[]{0, sh - 40 - h},
            VanillaGuiLayers.CHAT);

    private HudLayout() {
    }

    private static int[] chatSize() {
        var o = net.minecraft.client.Minecraft.getInstance().options;
        double sc = o.chatScale().get();
        return new int[]{(int) Math.ceil((net.minecraft.client.gui.components.ChatComponent.getWidth(o.chatWidth().get()) + 12) * sc),
                (int) Math.ceil(net.minecraft.client.gui.components.ChatComponent.getHeight(o.chatHeightUnfocused().get()) * sc)};
    }

    private static Element mod(String id, String label, Size size, Pos def) {
        Element e = new Element(id, label, new Identifier[0], size, def);
        ELEMENTS.add(e);
        return e;
    }

    private static Element vanilla(String id, String label, Size size, Pos def, Identifier... layers) {
        Element e = new Element(id, label, layers, size, def);
        ELEMENTS.add(e);
        for (Identifier l : layers) BY_LAYER.put(l, e);
        return e;
    }

    public static List<Element> elements() {
        ensureLoaded();
        return ELEMENTS;
    }

    public static Element forLayer(Identifier layer) {
        ensureLoaded();
        return BY_LAYER.get(layer);
    }

    // ---------------------------------------------------------------- rendu

    /**
     * Prepare le dessin d'un element du mod en coordonnees locales (0, 0) : renvoie faux s'il est masque
     * (hors edition). Doit etre suivi de {@link #end} quand il renvoie vrai.
     */
    public static boolean begin(GuiGraphicsExtractor g, Element e) {
        ensureLoaded();
        if (!e.visible && !editing) return false;
        int[] r = e.rect(g.guiWidth(), g.guiHeight());
        g.pose().pushMatrix();
        g.pose().translate(r[0], r[1]);
        if (e.scale != 1f) g.pose().scale(e.scale, e.scale);
        return true;
    }

    public static void end(GuiGraphicsExtractor g) {
        g.pose().popMatrix();
    }

    /** Transformation d'un element vanilla : sa position par defaut est envoyee vers sa position personnalisee. */
    public static boolean pushVanilla(GuiGraphicsExtractor g, Element e) {
        if (!e.custom && e.scale == 1f) return false;
        int sw = g.guiWidth();
        int sh = g.guiHeight();
        int[] d = e.defaultPos(sw, sh);
        int[] r = e.rect(sw, sh);
        g.pose().pushMatrix();
        g.pose().translate(r[0], r[1]);
        if (e.scale != 1f) g.pose().scale(e.scale, e.scale);
        g.pose().translate(-d[0], -d[1]);
        return true;
    }

    // ---------------------------------------------------------------- sauvegarde

    private static Path file() {
        return FMLPaths.CONFIGDIR.get().resolve("mmorpg-hud.json");
    }

    private static void ensureLoaded() {
        if (!loaded) load();
    }

    public static void load() {
        loaded = true;
        for (Element e : ELEMENTS) e.reset();
        Path p = file();
        if (!Files.exists(p)) return;
        try {
            JsonObject root = GSON.fromJson(Files.readString(p, StandardCharsets.UTF_8), JsonObject.class);
            JsonObject els = root != null && root.has("elements") ? root.getAsJsonObject("elements") : new JsonObject();
            for (Element e : ELEMENTS) {
                if (els.has(e.id)) e.fromJson(els.getAsJsonObject(e.id));
            }
        } catch (Exception ex) {
            MMORPG.LOGGER.warn("[MMORPG] Disposition du HUD illisible ({}), valeurs par defaut utilisees", ex.toString());
        }
    }

    public static void save() {
        JsonObject els = new JsonObject();
        for (Element e : ELEMENTS) {
            if (e.customized()) els.add(e.id, e.toJson());
        }
        JsonObject root = new JsonObject();
        root.addProperty("_aide", "Disposition de l'interface Eldoria. Modifiable en jeu (touche « Modifier l'interface »).");
        root.add("elements", els);
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), GSON.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException ex) {
            MMORPG.LOGGER.warn("[MMORPG] Impossible d'enregistrer la disposition du HUD : {}", ex.toString());
        }
    }

    /** Copie de l'etat de tous les elements (pour annuler une edition). */
    public static Map<String, JsonObject> snapshot() {
        ensureLoaded();
        Map<String, JsonObject> m = new HashMap<>();
        for (Element e : ELEMENTS) m.put(e.id, e.toJson());
        return m;
    }

    public static void restore(Map<String, JsonObject> snap) {
        for (Element e : ELEMENTS) {
            JsonObject o = snap.get(e.id);
            if (o != null) e.fromJson(o);
        }
    }

    public static void resetAll() {
        for (Element e : ELEMENTS) e.reset();
    }
}
