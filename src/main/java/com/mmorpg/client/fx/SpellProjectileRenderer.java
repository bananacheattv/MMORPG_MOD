package com.mmorpg.client.fx;

import com.mmorpg.entity.SpellProjectile;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Rendu des projectiles de competences : orbes de feu, meteore, fleches de lumiere, orbe de foudre. */
public class SpellProjectileRenderer extends EntityRenderer<SpellProjectile, SpellProjectileRenderer.State> {
    public static class State extends EntityRenderState {
        SpellProjectile.Kind kind = SpellProjectile.Kind.FIREBALL;
        float age;
        int seed;
        /** Couleurs principale, coeur et trainee (projectiles magiques). */
        int c1, c2, c3;
        Vector3f dir = new Vector3f(0, 0, 1);
        final List<Vector3f> trail = new ArrayList<>();
    }

    public SpellProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(SpellProjectile e, State s, float partial) {
        super.extractRenderState(e, s, partial);
        s.kind = e.kind();
        s.age = e.tickCount + partial;
        s.seed = e.getId();
        Vec3 v = e.getDeltaMovement();
        if (v.lengthSqr() > 1e-6) s.dir.set((float) v.x, (float) v.y, (float) v.z).normalize();
        s.trail.clear();
        for (Vec3 p : e.trail) s.trail.add(new Vector3f((float) (p.x - s.x), (float) (p.y - s.y), (float) (p.z - s.z)));
    }

    @Override
    public void submit(State s, PoseStack ps, SubmitNodeCollector out, CameraRenderState camera) {
        Quaternionf cam = camera.orientation;
        switch (s.kind) {
            case FIREBALL -> orb(s, ps, out, cam, 0xFF7A1E, 0xFFF0C0, 0.8f, 0xFF3A10);
            case LIGHTNING_ORB -> lightningOrb(s, ps, out, cam);
            case METEOR -> meteor(s, ps, out, cam);
            default -> arrow(s, ps, out, cam);
        }
        super.submit(s, ps, out, camera);
    }

    /** Orbe lumineuse : halo, coeur blanc, flammeches tournantes et trainee. */
    static void orb(State s, PoseStack ps, SubmitNodeCollector out, Quaternionf cam, int color, int core, float size, int tail) {
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.GLOW), (p, b) -> {
            int i = 0;
            for (Vector3f q : s.trail) {
                float k = 1 - i / (float) Math.max(1, s.trail.size());
                FxDraw.billboard(b, p, cam, q.x, q.y, q.z, size * 0.75f * k, 0, FxDraw.col(i < 3 ? color : tail, 0.55f * k));
                i++;
            }
            FxDraw.billboard(b, p, cam, 0, 0, 0, size, 0, FxDraw.col(color, 0.95f));
            FxDraw.billboard(b, p, cam, 0, 0, 0, size * 0.42f, 0, FxDraw.col(core, 1));
            for (int k = 0; k < 3; k++) {
                float a = s.age * 0.6f + k * 2.094f;
                FxDraw.billboard(b, p, cam, Mth.cos(a) * size * 0.35f, Mth.sin(a * 1.3f) * size * 0.2f, Mth.sin(a) * size * 0.35f,
                        size * 0.45f, a, FxDraw.col(tail, 0.8f));
            }
        });
    }

    private static void lightningOrb(State s, PoseStack ps, SubmitNodeCollector out, Quaternionf cam) {
        orb(s, ps, out, cam, 0xA8D8FF, 0xFFFFFF, 0.6f, 0x6090FF);
        out.submitCustomGeometry(ps, RenderTypes.lightning(), (p, b) -> {
            RandomSource r = RandomSource.create(s.seed * 13L + (int) s.age);
            for (int i = 0; i < 3; i++) {
                Vector3f end = new Vector3f(r.nextFloat() - 0.5f, r.nextFloat() - 0.5f, r.nextFloat() - 0.5f).normalize().mul(0.7f);
                FxDraw.bolt(b, p, cam, new Vector3f(), end, r.nextLong(), 0.05f, FxDraw.col(0x80C0FF, 0.9f), FxDraw.col(0xFFFFFF, 1));
            }
        });
    }

    private static void meteor(State s, PoseStack ps, SubmitNodeCollector out, Quaternionf cam) {
        out.submitCustomGeometry(ps, FxDraw.translucent(FxDraw.GLOW), (p, b) -> {
            int i = 0;
            for (Vector3f q : s.trail) {
                if (i > 3) {
                    float k = 1 - i / (float) Math.max(1, s.trail.size());
                    FxDraw.billboard(b, p, cam, q.x, q.y + 0.3f, q.z, 1.6f * k + 0.4f, i, FxDraw.colA(0x2A2224, 0.45f * k));
                }
                i++;
            }
        });
        orb(s, ps, out, cam, 0xFF6A10, 0xFFE0A0, 1.9f, 0xFF3010);
        // roche incandescente tournante
        float h = 0.55f;
        float spin = s.age * 0.25f;
        out.submitCustomGeometry(ps, FxDraw.translucent(FxDraw.ROCK), (p, b) -> {
            Vector3f[] c = new Vector3f[8];
            for (int i = 0; i < 8; i++) {
                Vector3f v = new Vector3f((i & 1) == 0 ? -h : h, (i & 2) == 0 ? -h : h, (i & 4) == 0 ? -h : h);
                v.rotateY(spin).rotateX(spin * 0.7f);
                c[i] = v;
            }
            int col = FxDraw.colA(0xFFFFFF, 1);
            int[][] faces = {{0, 1, 3, 2}, {4, 5, 7, 6}, {0, 1, 5, 4}, {2, 3, 7, 6}, {0, 2, 6, 4}, {1, 3, 7, 5}};
            for (int[] f : faces) FxDraw.quad(b, p, c[f[0]], c[f[1]], c[f[2]], c[f[3]], 0, 0, 1, 1, col);
        });
    }

    /** Fleche d'energie : trait lumineux etire dans le sens du vol, pointe brillante. */
    private static void arrow(State s, PoseStack ps, SubmitNodeCollector out, Quaternionf cam) {
        int color = s.kind.color;
        float len = s.kind == SpellProjectile.Kind.LIGHT_ARROW ? 2.6f : 1.5f;
        float w = s.kind == SpellProjectile.Kind.LIGHT_ARROW ? 0.2f : 0.14f;
        Vector3f tail = new Vector3f(s.dir).mul(-len);
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.STREAK), (p, b) -> {
            FxDraw.ribbon(b, p, cam, tail, new Vector3f(), w, w, 0, 1, FxDraw.col(color, 0.2f), FxDraw.col(color, 1));
            FxDraw.ribbon(b, p, cam, new Vector3f(tail).mul(0.6f), new Vector3f(), w * 0.4f, w * 0.4f, 0, 1, FxDraw.col(0xFFFFFF, 0.2f), FxDraw.col(0xFFFFFF, 1));
        });
        float pulse = s.kind == SpellProjectile.Kind.EXPLOSIVE_ARROW ? 0.45f + 0.15f * Mth.sin(s.age * 1.2f) : 0.32f;
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.GLOW), (p, b) -> {
            FxDraw.billboard(b, p, cam, 0, 0, 0, pulse, 0, FxDraw.col(color, 1));
            int i = 0;
            for (Vector3f q : s.trail) {
                float k = 1 - i / 10f;
                FxDraw.billboard(b, p, cam, q.x, q.y, q.z, 0.12f * k, 0, FxDraw.col(color, 0.4f * k));
                i++;
            }
        });
    }
}
