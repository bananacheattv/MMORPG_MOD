package com.mmorpg.client.fx;

import com.mmorpg.MMORPG;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Primitives de dessin des effets (quads lumineux, anneaux, arcs, cylindres, spheres, eclairs). */
public final class FxDraw {
    public static final int FULL = LightCoordsUtil.FULL_BRIGHT;
    public static final Identifier GLOW = tex("glow"), RING = tex("ring"), RUNES = tex("rune_circle"), SLASH = tex("slash"),
            BEAM = tex("beam"), WAVE = tex("wave"), SPARK = tex("spark"), HEX = tex("hex"), SHARD = tex("shard"),
            ROCK = tex("rock"), SHIELD = tex("shield"), MARK = tex("rune_mark"), STREAK = tex("streak");

    private FxDraw() {
    }

    private static Identifier tex(String name) {
        return MMORPG.id("textures/fx/" + name + ".png");
    }

    /** Rendu additif (la lumiere s'ajoute : le noir est invisible). */
    public static RenderType additive(Identifier texture) {
        return RenderTypes.eyes(texture);
    }

    /** Rendu translucide pleine lumiere (glace, roche, fumee). */
    public static RenderType translucent(Identifier texture) {
        return RenderTypes.entityTranslucentEmissive(texture);
    }

    /** Couleur ARGB ; pour le rendu additif, l'intensite est aussi appliquee aux composantes. */
    public static int col(int rgb, float alpha) {
        float a = Mth.clamp(alpha, 0f, 1f);
        int r = (int) (((rgb >> 16) & 255) * a), g = (int) (((rgb >> 8) & 255) * a), b = (int) ((rgb & 255) * a);
        return ((int) (a * 255) << 24) | (r << 16) | (g << 8) | b;
    }

    /** Couleur ARGB sans pre-multiplication (rendu translucide). */
    public static int colA(int rgb, float alpha) {
        return ((int) (Mth.clamp(alpha, 0f, 1f) * 255) << 24) | (rgb & 0xFFFFFF);
    }

    public static float ease(float t) {
        t = Mth.clamp(t, 0f, 1f);
        return 1 - (1 - t) * (1 - t) * (1 - t);
    }

    static void v(VertexConsumer b, PoseStack.Pose p, float x, float y, float z, float u, float vv, int c) {
        b.addVertex(p, x, y, z).setColor(c).setUv(u, vv).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL).setNormal(p, 0, 1, 0);
    }

    /** Quad a deux faces : a, b, c, d dans l'ordre du contour ; (u0, v0) en haut a gauche. */
    public static void quad(VertexConsumer b, PoseStack.Pose p, Vector3f a, Vector3f bb, Vector3f c, Vector3f d,
                            float u0, float v0, float u1, float v1, int col) {
        v(b, p, a.x, a.y, a.z, u0, v1, col);
        v(b, p, bb.x, bb.y, bb.z, u1, v1, col);
        v(b, p, c.x, c.y, c.z, u1, v0, col);
        v(b, p, d.x, d.y, d.z, u0, v0, col);
        v(b, p, d.x, d.y, d.z, u0, v0, col);
        v(b, p, c.x, c.y, c.z, u1, v0, col);
        v(b, p, bb.x, bb.y, bb.z, u1, v1, col);
        v(b, p, a.x, a.y, a.z, u0, v1, col);
    }

    /** Carre horizontal (au sol) de demi-cote h, tourne de rot radians. */
    public static void ground(VertexConsumer b, PoseStack.Pose p, float x, float y, float z, float h, float rot, int col) {
        float c = Mth.cos(rot) * h, s = Mth.sin(rot) * h;
        quad(b, p, new Vector3f(x - c + s, y, z - s - c), new Vector3f(x + c + s, y, z + s - c),
                new Vector3f(x + c - s, y, z + s + c), new Vector3f(x - c - s, y, z - s + c), 0, 0, 1, 1, col);
    }

    /** Carre face a la camera. */
    public static void billboard(VertexConsumer b, PoseStack.Pose p, Quaternionf cam, float x, float y, float z, float size, float roll, int col) {
        Vector3f r = cam.transform(new Vector3f(Mth.cos(roll), Mth.sin(roll), 0)).mul(size);
        Vector3f u = cam.transform(new Vector3f(-Mth.sin(roll), Mth.cos(roll), 0)).mul(size);
        quad(b, p, new Vector3f(x, y, z).sub(r).sub(u), new Vector3f(x, y, z).add(r).sub(u),
                new Vector3f(x, y, z).add(r).add(u), new Vector3f(x, y, z).sub(r).add(u), 0, 0, 1, 1, col);
    }

    /** Ruban entre deux points, oriente vers la camera ; u le long du ruban. */
    public static void ribbon(VertexConsumer b, PoseStack.Pose p, Quaternionf cam, Vector3f from, Vector3f to, float w0, float w1,
                              float u0, float u1, int col0, int col1) {
        Vector3f dir = new Vector3f(to).sub(from);
        Vector3f look = cam.transform(new Vector3f(0, 0, -1));
        Vector3f side = new Vector3f(dir).cross(look);
        if (side.lengthSquared() < 1e-6f) side.set(1, 0, 0);
        side.normalize();
        Vector3f s0 = new Vector3f(side).mul(w0), s1 = new Vector3f(side).mul(w1);
        v(b, p, from.x - s0.x, from.y - s0.y, from.z - s0.z, u0, 1, col0);
        v(b, p, to.x - s1.x, to.y - s1.y, to.z - s1.z, u1, 1, col1);
        v(b, p, to.x + s1.x, to.y + s1.y, to.z + s1.z, u1, 0, col1);
        v(b, p, from.x + s0.x, from.y + s0.y, from.z + s0.z, u0, 0, col0);
        v(b, p, from.x + s0.x, from.y + s0.y, from.z + s0.z, u0, 0, col0);
        v(b, p, to.x + s1.x, to.y + s1.y, to.z + s1.z, u1, 0, col1);
        v(b, p, to.x - s1.x, to.y - s1.y, to.z - s1.z, u1, 1, col1);
        v(b, p, from.x - s0.x, from.y - s0.y, from.z - s0.z, u0, 1, col0);
    }

    /** Bande cylindrique verticale (v = 1 en bas, 0 en haut). */
    public static void cylinder(VertexConsumer b, PoseStack.Pose p, float x, float y, float z, float r, float h, int segs, float uOff, int col) {
        for (int i = 0; i < segs; i++) {
            float a0 = Mth.TWO_PI * i / segs, a1 = Mth.TWO_PI * (i + 1) / segs;
            float x0 = x + Mth.cos(a0) * r, z0 = z + Mth.sin(a0) * r, x1 = x + Mth.cos(a1) * r, z1 = z + Mth.sin(a1) * r;
            float u0 = uOff + (float) i / segs, u1 = uOff + (float) (i + 1) / segs;
            quad(b, p, new Vector3f(x0, y, z0), new Vector3f(x1, y, z1), new Vector3f(x1, y + h, z1), new Vector3f(x0, y + h, z0), u0, 0, u1, 1, col);
        }
    }

    /**
     * Arc (portion d'anneau) dans un plan : e1 = direction de l'angle 0, e2 = direction de l'angle 90 deg.
     * u va de a0 (0) a a1 (1), v du bord exterieur (0) au bord interieur (1).
     */
    public static void arc(VertexConsumer b, PoseStack.Pose p, Vector3f c, Vector3f e1, Vector3f e2, float rIn, float rOut,
                           float a0, float a1, int segs, int col) {
        for (int i = 0; i < segs; i++) {
            float t0 = (float) i / segs, t1 = (float) (i + 1) / segs;
            float aa = a0 + (a1 - a0) * t0, ab = a0 + (a1 - a0) * t1;
            Vector3f d0 = new Vector3f(e1).mul(Mth.cos(aa)).add(new Vector3f(e2).mul(Mth.sin(aa)));
            Vector3f d1 = new Vector3f(e1).mul(Mth.cos(ab)).add(new Vector3f(e2).mul(Mth.sin(ab)));
            quad(b, p, new Vector3f(d0).mul(rIn).add(c), new Vector3f(d1).mul(rIn).add(c), new Vector3f(d1).mul(rOut).add(c),
                    new Vector3f(d0).mul(rOut).add(c), t0, 0, t1, 1, col);
        }
    }

    /** Sphere (latitudes x longitudes), texture repetee. */
    public static void sphere(VertexConsumer b, PoseStack.Pose p, float x, float y, float z, float r, int lat, int lon, float spin, int col) {
        for (int i = 0; i < lat; i++) {
            float p0 = -Mth.HALF_PI + Mth.PI * i / lat, p1 = -Mth.HALF_PI + Mth.PI * (i + 1) / lat;
            for (int j = 0; j < lon; j++) {
                float t0 = spin + Mth.TWO_PI * j / lon, t1 = spin + Mth.TWO_PI * (j + 1) / lon;
                quad(b, p, sp(x, y, z, r, p0, t0), sp(x, y, z, r, p0, t1), sp(x, y, z, r, p1, t1), sp(x, y, z, r, p1, t0),
                        j * 0.5f, i * 0.5f, (j + 1) * 0.5f, (i + 1) * 0.5f, col);
            }
        }
    }

    private static Vector3f sp(float x, float y, float z, float r, float phi, float theta) {
        return new Vector3f(x + r * Mth.cos(phi) * Mth.cos(theta), y + r * Mth.sin(phi), z + r * Mth.cos(phi) * Mth.sin(theta));
    }

    /** Eclair en zigzag entre deux points (a dessiner avec RenderTypes.lightning()). */
    public static void bolt(VertexConsumer b, PoseStack.Pose p, Quaternionf cam, Vector3f from, Vector3f to, long seed, float width,
                            int outer, int core) {
        RandomSource r = RandomSource.create(seed);
        int n = Math.max(3, (int) (from.distance(to) * 2.2f));
        Vector3f prev = new Vector3f(from);
        Vector3f dir = new Vector3f(to).sub(from);
        for (int i = 1; i <= n; i++) {
            Vector3f next = new Vector3f(dir).mul((float) i / n).add(from);
            if (i < n) next.add((r.nextFloat() - 0.5f) * 0.6f, (r.nextFloat() - 0.5f) * 0.6f, (r.nextFloat() - 0.5f) * 0.6f);
            ribbon(b, p, cam, prev, next, width, width, 0, 1, outer, outer);
            ribbon(b, p, cam, prev, next, width * 0.3f, width * 0.3f, 0, 1, core, core);
            prev = next;
        }
    }
}
