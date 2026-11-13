package com.mmorpg.client.hud;

import com.mmorpg.MMORPG;
import com.mmorpg.client.QolClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;

/**
 * Image de carte dessinee en un seul rectangle texture. Avant, chaque ligne de couleur etait un rectangle de l'interface
 * (des milliers par image, chacun compare a tous les autres par le moteur d'interface) : c'etait le premier cout du HUD.
 * L'image n'est recalculee que si la vue ou les releves changent.
 */
public final class MapTexture {
    private final Identifier id;
    private final String label;
    private DynamicTexture texture;
    private int width, height;
    private double lastCx = Double.NaN, lastCz, lastZoom;
    private long lastVersion = -1;
    private String lastDim = "";
    public static int rebuilds;

    public MapTexture(String name) {
        this.id = MMORPG.id("dynamic/" + name);
        this.label = "mmorpg " + name;
    }

    /**
     * Dessine la carte centree sur (cx, cz) dans le rectangle (x, y, w, h) ; zoom = pixels d'interface par bloc.
     * Le fond (zones inexplorees) reste celui deja dessine : les pixels inconnus sont transparents.
     */
    public void draw(GuiGraphicsExtractor g, int x, int y, int w, int h, double cx, double cz, double zoom) {
        if (w <= 0 || h <= 0) return;
        if (texture == null || w != width || h != height) {
            width = w;
            height = h;
            texture = new DynamicTexture(label, w, h, true);
            Minecraft.getInstance().getTextureManager().register(id, texture);   // l'ancienne texture est liberee par le gestionnaire
            lastVersion = -1;
        }
        long version = QolClient.mapVersion();
        String dim = QolClient.dim();
        if (version != lastVersion || cx != lastCx || cz != lastCz || zoom != lastZoom || !dim.equals(lastDim)) {
            lastVersion = version;
            lastCx = cx;
            lastCz = cz;
            lastZoom = zoom;
            lastDim = dim;
            rebuild(cx, cz, zoom);
        }
        g.blit(RenderPipelines.GUI_TEXTURED, id, x, y, 0, 0, w, h, w, h);
    }

    private void rebuild(double cx, double cz, double zoom) {
        rebuilds++;
        var pixels = texture.getPixels();
        var chunks = QolClient.chunkMap();
        boolean perChunk = zoom < 0.25;          // dezoom extreme : une couleur par chunk (aucun trou)
        long lastKey = Long.MIN_VALUE;
        int[] cached = null;
        for (int row = 0; row < height; row++) {
            int z = (int) Math.floor(cz + (row - height / 2.0) / zoom);
            for (int col = 0; col < width; col++) {
                int wx = (int) Math.floor(cx + (col - width / 2.0) / zoom);
                long key = net.minecraft.world.level.ChunkPos.pack(wx >> 4, z >> 4);
                if (key != lastKey) {
                    lastKey = key;
                    cached = chunks.get(key);
                }
                int c = cached == null ? 0 : perChunk ? cached[136] : cached[(z & 15) * 16 + (wx & 15)];
                pixels.setPixel(col, row, c);
            }
        }
        texture.upload();
    }
}
