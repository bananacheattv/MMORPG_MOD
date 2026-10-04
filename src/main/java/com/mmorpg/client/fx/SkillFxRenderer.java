package com.mmorpg.client.fx;

import com.mmorpg.entity.FxKind;
import com.mmorpg.entity.SkillFxEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/** Dessine les effets de competences (anneaux, ondes de choc, arcs de lame, eclairs, domes...). */
public class SkillFxRenderer extends EntityRenderer<SkillFxEntity, SkillFxRenderer.State> {
    public static class State extends EntityRenderState {
        FxKind kind = FxKind.RING;
        int color, life, seed;
        float radius, yaw, age, followHeight;
        Vector3f off = new Vector3f();
        final List<Vector3f> points = new ArrayList<>();
        final List<Vector3f> history = new ArrayList<>();
    }

    public SkillFxRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRender(SkillFxEntity entity, Frustum culler, double camX, double camY, double camZ, float partialTicks) {
        return true;
    }

    @Override
    public void extractRenderState(SkillFxEntity e, State s, float partial) {
        super.extractRenderState(e, s, partial);
        s.kind = e.kind();
        s.color = e.color();
        s.life = e.life();
        s.radius = e.radius();
        s.yaw = e.yaw();
        s.seed = e.getId();
        s.age = e.tickCount + partial;
        s.off.set(0, 0, 0);
        s.followHeight = 1.8f;
        s.history.clear();
        s.points.clear();
        Entity f = e.followId() >= 0 ? e.level().getEntity(e.followId()) : null;
        if (f != null) {
            Vec3 fp = f.getPosition(partial);
            s.off.set((float) (fp.x - s.x), (float) (fp.y - s.y), (float) (fp.z - s.z));
            s.followHeight = f.getBbHeight();
            for (Vec3 h : e.history) s.history.add(new Vector3f((float) (h.x - fp.x), (float) (h.y - fp.y), (float) (h.z - fp.z)));
        }
        for (Vec3 p : e.relativePoints()) s.points.add(new Vector3f((float) p.x, (float) p.y, (float) p.z));
    }

    @Override
    public void submit(State s, PoseStack poseStack, SubmitNodeCollector out, CameraRenderState camera) {
        float t = Mth.clamp(s.age / Math.max(1, s.life), 0f, 1f);
        Quaternionf cam = camera.orientation;
        poseStack.pushPose();
        poseStack.translate(s.off.x, s.off.y, s.off.z);
        switch (s.kind) {
            case RING -> ring(s, poseStack, out, t);
            case SHOCKWAVE -> shockwave(s, poseStack, out, t);
            case CIRCLE -> circle(s, poseStack, out, t);
            case PILLAR -> pillar(s, poseStack, out, t);
            case SLASH -> slash(s, poseStack, out, t);
            case WHIRL -> whirl(s, poseStack, out, t);
            case AURA -> aura(s, poseStack, out, t, cam);
            case DOME -> dome(s, poseStack, out, t);
            case CHAIN -> chain(s, poseStack, out, t, cam);
            case BEAM -> beam(s, poseStack, out, t, cam);
            case BURST -> burst(s, poseStack, out, t, cam);
            case NOVA -> nova(s, poseStack, out, t);
            case CRACKS -> cracks(s, poseStack, out, t);
            case HALO -> halo(s, poseStack, out, t, cam);
            case BLINK -> blink(s, poseStack, out, t, cam);
            case MARK -> mark(s, poseStack, out, t, cam);
            case TRAIL -> trail(s, poseStack, out, t, cam);
            case SHIELD -> shield(s, poseStack, out, t);
            case STORM -> storm(s, poseStack, out, t, cam);
        }
        poseStack.popPose();
        super.submit(s, poseStack, out, camera);
    }

    /** Apparition rapide puis disparition sur la fin. */
    private static float fadeInOut(State s, float t, float in, float out) {
        return Math.min(Mth.clamp(s.age / Math.max(0.01f, in), 0f, 1f), Mth.clamp((1 - t) / out, 0f, 1f));
    }

    private static Vector3f forward(float yawDeg) {
        float y = yawDeg * Mth.DEG_TO_RAD;
        return new Vector3f(-Mth.sin(y), 0, Mth.cos(y));
    }

    private static Vector3f left(float yawDeg) {
        float y = yawDeg * Mth.DEG_TO_RAD;
        return new Vector3f(Mth.cos(y), 0, Mth.sin(y));
    }

    // ------------------------------------------------------------------------------------------ effets
    private void ring(State s, PoseStack ps, SubmitNodeCollector out, float t) {
        float r = s.radius * (0.25f + 0.75f * FxDraw.ease(t));
        int c = FxDraw.col(s.color, 1 - t);
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.RING), (p, b) -> FxDraw.ground(b, p, 0, 0.06f, 0, r, s.age * 0.02f, c));
    }

    private void shockwave(State s, PoseStack ps, SubmitNodeCollector out, float t) {
        float r = s.radius * (0.2f + 0.8f * FxDraw.ease(t));
        float a = 1 - t;
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.RING), (p, b) -> {
            FxDraw.ground(b, p, 0, 0.06f, 0, r, 0, FxDraw.col(s.color, a));
            FxDraw.ground(b, p, 0, 0.07f, 0, r * 0.75f, 0.8f, FxDraw.col(s.color, a * 0.5f));
        });
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.WAVE),
                (p, b) -> FxDraw.cylinder(b, p, 0, 0, 0, r * 0.82f, 1.6f * (1 - t), 32, 0, FxDraw.col(s.color, a * 0.85f)));
    }

    private void circle(State s, PoseStack ps, SubmitNodeCollector out, float t) {
        float sc = FxDraw.ease(Math.min(1, s.age / 5f));
        float a = fadeInOut(s, t, 1, 0.25f);
        float r = s.radius * sc;
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.RUNES), (p, b) -> FxDraw.ground(b, p, 0, 0.05f, 0, r, s.age * 0.05f, FxDraw.col(s.color, a)));
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.RING), (p, b) -> FxDraw.ground(b, p, 0, 0.06f, 0, r * 0.62f, -s.age * 0.08f, FxDraw.col(s.color, a * 0.7f)));
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.GLOW), (p, b) -> FxDraw.ground(b, p, 0, 0.04f, 0, r * 0.9f, 0, FxDraw.col(s.color, a * 0.35f)));
    }

    private void pillar(State s, PoseStack ps, SubmitNodeCollector out, float t) {
        float a = fadeInOut(s, t, 3, 0.4f);
        float h = 7f * Math.min(1, s.age / 4f);
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.WAVE), (p, b) -> {
            FxDraw.cylinder(b, p, 0, 0, 0, s.radius, h, 20, s.age * 0.03f, FxDraw.col(s.color, a * 0.6f));
            FxDraw.cylinder(b, p, 0, 0, 0, s.radius * 0.45f, h * 0.9f, 14, -s.age * 0.05f, FxDraw.col(s.color, a));
        });
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.RING), (p, b) -> FxDraw.ground(b, p, 0, 0.06f, 0, s.radius * 1.6f, s.age * 0.06f, FxDraw.col(s.color, a)));
    }

    private void slash(State s, PoseStack ps, SubmitNodeCollector out, float t) {
        float head = 70 - 140 * FxDraw.ease(Math.min(1, t * 2.2f));
        float tail = Math.min(70, head + 110);
        float a = 1 - t * t;
        Vector3f fwd = forward(s.yaw), lft = left(s.yaw);
        Vector3f e2 = new Vector3f(lft).mul(Mth.cos(0.35f)).add(0, Mth.sin(0.35f), 0);
        Vector3f c = new Vector3f(0, 1.15f, 0);
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.SLASH), (p, b) -> {
            FxDraw.arc(b, p, c, fwd, e2, s.radius - 1.0f, s.radius, tail * Mth.DEG_TO_RAD, head * Mth.DEG_TO_RAD, 18, FxDraw.col(s.color, a));
            FxDraw.arc(b, p, c, fwd, e2, s.radius - 0.4f, s.radius + 0.15f, tail * Mth.DEG_TO_RAD, head * Mth.DEG_TO_RAD, 18, FxDraw.col(0xFFFFFF, a * 0.6f));
        });
    }

    private void whirl(State s, PoseStack ps, SubmitNodeCollector out, float t) {
        float rot = s.age * 0.55f;
        float a = Math.min(1, (1 - t) * 3);
        Vector3f c = new Vector3f(0, 1.0f, 0), e1 = new Vector3f(1, 0, 0), e2 = new Vector3f(0, 0, 1);
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.SLASH), (p, b) -> {
            for (int k = 0; k < 2; k++) {
                float a0 = rot + k * Mth.PI;
                FxDraw.arc(b, p, c, e1, e2, s.radius - 0.9f, s.radius, a0, a0 + 2.3f, 16, FxDraw.col(s.color, a));
                FxDraw.arc(b, p, new Vector3f(0, 0.6f, 0), e1, e2, s.radius - 0.7f, s.radius - 0.1f, a0 + 0.8f, a0 + 2.6f, 14, FxDraw.col(0xFFFFFF, a * 0.5f));
            }
        });
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.RING), (p, b) -> FxDraw.ground(b, p, 0, 0.06f, 0, s.radius, rot, FxDraw.col(s.color, a * 0.4f)));
    }

    private void aura(State s, PoseStack ps, SubmitNodeCollector out, float t, Quaternionf cam) {
        float a = fadeInOut(s, t, 4, 0.1f) * (0.75f + 0.25f * Mth.sin(s.age * 0.3f));
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.GLOW), (p, b) -> {
            for (int k = 0; k < 3; k++) {
                for (int i = 0; i < 10; i++) {
                    float hy = i * 0.24f;
                    float ang = s.age * 0.25f + k * 2.094f + hy * 1.4f;
                    float r = 0.75f - i * 0.02f;
                    FxDraw.billboard(b, p, cam, Mth.cos(ang) * r, hy + 0.1f, Mth.sin(ang) * r, 0.3f * (1 - i / 13f), 0,
                            FxDraw.col(s.color, a * (0.9f - i * 0.07f)));
                }
            }
        });
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.RING), (p, b) -> FxDraw.ground(b, p, 0, 0.06f, 0, 1.0f, s.age * 0.1f, FxDraw.col(s.color, a * 0.6f)));
    }

    private void dome(State s, PoseStack ps, SubmitNodeCollector out, float t) {
        float a = fadeInOut(s, t, 5, 0.1f) * (0.32f + 0.12f * Mth.sin(s.age * 0.25f));
        float r = s.radius * FxDraw.ease(Math.min(1, s.age / 6f));
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.HEX), (p, b) -> FxDraw.sphere(b, p, 0, 0.95f, 0, r, 8, 16, s.age * 0.02f, FxDraw.col(s.color, a)));
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.RING), (p, b) -> FxDraw.ground(b, p, 0, 0.06f, 0, r * 1.05f, -s.age * 0.04f, FxDraw.col(s.color, a * 2)));
    }

    private void chain(State s, PoseStack ps, SubmitNodeCollector out, float t, Quaternionf cam) {
        if (s.points.size() < 2) return;
        float a = 1 - t * t;
        int flick = (int) (s.age / 2);
        out.submitCustomGeometry(ps, RenderTypes.lightning(), (p, b) -> {
            for (int i = 0; i + 1 < s.points.size(); i++) {
                FxDraw.bolt(b, p, cam, s.points.get(i), s.points.get(i + 1), s.seed * 31L + flick * 7L + i, 0.13f,
                        FxDraw.col(s.color, a), FxDraw.col(0xFFFFFF, a));
            }
        });
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.GLOW), (p, b) -> {
            for (Vector3f v : s.points) FxDraw.billboard(b, p, cam, v.x, v.y, v.z, 0.7f, 0, FxDraw.col(s.color, a));
        });
    }

    private void beam(State s, PoseStack ps, SubmitNodeCollector out, float t, Quaternionf cam) {
        if (s.points.size() < 2) return;
        Vector3f from = s.points.get(0), to = s.points.get(s.points.size() - 1);
        float w = s.radius * (1 - t);
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.BEAM), (p, b) -> {
            FxDraw.ribbon(b, p, cam, from, to, w, w, 0, 1, FxDraw.col(s.color, 1 - t), FxDraw.col(s.color, 1 - t));
            FxDraw.ribbon(b, p, cam, from, to, w * 0.35f, w * 0.35f, 0, 1, FxDraw.col(0xFFFFFF, 1 - t), FxDraw.col(0xFFFFFF, 1 - t));
        });
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.GLOW), (p, b) -> FxDraw.billboard(b, p, cam, to.x, to.y, to.z, w * 3, 0, FxDraw.col(s.color, 1 - t)));
    }

    private void burst(State s, PoseStack ps, SubmitNodeCollector out, float t, Quaternionf cam) {
        float e = FxDraw.ease(t);
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.GLOW), (p, b) -> {
            FxDraw.billboard(b, p, cam, 0, 0, 0, s.radius * (0.5f + 0.9f * e), 0, FxDraw.col(s.color, (1 - t) * 0.95f));
            FxDraw.billboard(b, p, cam, 0, 0, 0, s.radius * 0.45f * (1 - t), 0, FxDraw.col(0xFFF4D0, 1 - t));
        });
        RandomSource r = RandomSource.create(s.seed);
        List<Vector3f> dirs = new ArrayList<>();
        for (int i = 0; i < 14; i++) {
            dirs.add(new Vector3f(r.nextFloat() - 0.5f, r.nextFloat() * 0.8f - 0.2f, r.nextFloat() - 0.5f).normalize());
        }
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.SPARK), (p, b) -> {
            for (Vector3f d : dirs) {
                Vector3f q = new Vector3f(d).mul(s.radius * 1.3f * e);
                FxDraw.billboard(b, p, cam, q.x, q.y, q.z, 0.35f * (1 - t * 0.5f), t * 3, FxDraw.col(s.color, 1 - t));
            }
        });
    }

    private void nova(State s, PoseStack ps, SubmitNodeCollector out, float t) {
        float a = 1 - t;
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.RING), (p, b) -> FxDraw.ground(b, p, 0, 0.06f, 0, s.radius * FxDraw.ease(t), 0, FxDraw.col(s.color, a)));
        RandomSource r = RandomSource.create(s.seed);
        int n = 16;
        float grow = Mth.sin(Math.min(1, t * 2.5f) * Mth.HALF_PI) * (t > 0.7f ? (1 - t) / 0.3f : 1);
        float[] rr = new float[n];
        for (int i = 0; i < n; i++) rr[i] = 0.82f + 0.18f * r.nextFloat();
        out.submitCustomGeometry(ps, FxDraw.translucent(FxDraw.SHARD), (p, b) -> {
            float rad = s.radius * Math.min(1, t * 1.6f);
            for (int i = 0; i < n; i++) {
                float ang = Mth.TWO_PI * i / n;
                float x = Mth.cos(ang) * rad * rr[i], z = Mth.sin(ang) * rad * rr[i];
                float h = 1.4f * grow * rr[i], w = 0.22f;
                int c = FxDraw.colA(0xFFFFFF, Math.min(1, a * 2));
                FxDraw.quad(b, p, new Vector3f(x - w, 0, z), new Vector3f(x + w, 0, z), new Vector3f(x + w, h, z), new Vector3f(x - w, h, z), 0, 0, 1, 1, c);
                FxDraw.quad(b, p, new Vector3f(x, 0, z - w), new Vector3f(x, 0, z + w), new Vector3f(x, h, z + w), new Vector3f(x, h, z - w), 0, 0, 1, 1, c);
            }
        });
    }

    private void cracks(State s, PoseStack ps, SubmitNodeCollector out, float t) {
        float a = t < 0.6f ? 1 : (1 - t) / 0.4f;
        float len = s.radius * FxDraw.ease(Math.min(1, t * 5));
        out.submitCustomGeometry(ps, RenderTypes.lightning(), (p, b) -> {
            RandomSource r = RandomSource.create(s.seed);
            for (int k = 0; k < 9; k++) {
                float ang = Mth.TWO_PI * k / 9 + r.nextFloat() * 0.4f;
                Vector3f prev = new Vector3f(0, 0.05f, 0);
                int segs = 6;
                for (int i = 1; i <= segs; i++) {
                    float d = len * i / segs;
                    float jitter = (r.nextFloat() - 0.5f) * 0.5f;
                    Vector3f next = new Vector3f(Mth.cos(ang + jitter * 0.3f) * d, 0.05f, Mth.sin(ang + jitter * 0.3f) * d);
                    Vector3f dir = new Vector3f(next).sub(prev);
                    Vector3f side = new Vector3f(-dir.z, 0, dir.x).normalize().mul(0.09f * (1 - (float) i / (segs + 1)));
                    int c = FxDraw.col(s.color, a);
                    FxDraw.quad(b, p, new Vector3f(prev).sub(side), new Vector3f(next).sub(side), new Vector3f(next).add(side), new Vector3f(prev).add(side), 0, 0, 1, 1, c);
                    prev = next;
                }
            }
        });
    }

    private void halo(State s, PoseStack ps, SubmitNodeCollector out, float t, Quaternionf cam) {
        float a = fadeInOut(s, t, 4, 0.2f);
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.RING), (p, b) -> {
            FxDraw.ground(b, p, 0, 0.06f, 0, s.radius, s.age * 0.05f, FxDraw.col(s.color, a * 0.8f));
            FxDraw.ground(b, p, 0, s.followHeight + 0.45f, 0, 0.42f, -s.age * 0.12f, FxDraw.col(s.color, a));
        });
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.SPARK), (p, b) -> {
            for (int k = 0; k < 10; k++) {
                float phase = (s.age * 0.035f + k / 10f) % 1f;
                float ang = k * 0.63f + s.age * 0.05f;
                FxDraw.billboard(b, p, cam, Mth.cos(ang) * s.radius * 0.8f, phase * 2.4f, Mth.sin(ang) * s.radius * 0.8f, 0.22f, 0,
                        FxDraw.col(s.color, (1 - phase) * a));
            }
        });
    }

    private void blink(State s, PoseStack ps, SubmitNodeCollector out, float t, Quaternionf cam) {
        float e = FxDraw.ease(t);
        RandomSource r = RandomSource.create(s.seed);
        List<Vector3f> dirs = new ArrayList<>();
        for (int i = 0; i < 18; i++) dirs.add(new Vector3f(r.nextFloat() - 0.5f, r.nextFloat() - 0.5f, r.nextFloat() - 0.5f).normalize());
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.SPARK), (p, b) -> {
            for (Vector3f d : dirs) {
                Vector3f q = new Vector3f(d).mul(s.radius * (1 - e)).add(0, 1, 0);
                FxDraw.billboard(b, p, cam, q.x, q.y, q.z, 0.3f, 0, FxDraw.col(s.color, 1 - t * 0.5f));
            }
        });
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.GLOW), (p, b) -> FxDraw.billboard(b, p, cam, 0, 1, 0, 1.4f * Mth.sin(t * Mth.PI), 0, FxDraw.col(s.color, 1)));
    }

    private void mark(State s, PoseStack ps, SubmitNodeCollector out, float t, Quaternionf cam) {
        float a = fadeInOut(s, t, 3, 0.2f);
        float y = s.followHeight + 0.65f + 0.1f * Mth.sin(s.age * 0.2f);
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.MARK), (p, b) -> FxDraw.billboard(b, p, cam, 0, y, 0, 0.32f, 0, FxDraw.col(s.color, a)));
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.GLOW), (p, b) -> FxDraw.billboard(b, p, cam, 0, y, 0, 0.55f, 0, FxDraw.col(s.color, a * 0.5f)));
    }

    private void trail(State s, PoseStack ps, SubmitNodeCollector out, float t, Quaternionf cam) {
        float a = 1 - t;
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.GLOW), (p, b) -> {
            int i = 0;
            for (Vector3f h : s.history) {
                float k = 1 - i / 10f;
                FxDraw.billboard(b, p, cam, h.x, h.y + 1.0f, h.z, 0.75f * k, 0, FxDraw.col(s.color, a * 0.55f * k));
                FxDraw.billboard(b, p, cam, h.x, h.y + 0.3f, h.z, 0.5f * k, 0, FxDraw.col(s.color, a * 0.35f * k));
                i++;
            }
        });
    }

    private void shield(State s, PoseStack ps, SubmitNodeCollector out, float t) {
        Vector3f fwd = forward(s.yaw), lft = left(s.yaw);
        float sc = s.radius * (0.6f + 0.4f * FxDraw.ease(Math.min(1, t * 3)));
        float a = 1 - t;
        Vector3f c = new Vector3f(fwd).mul(1.1f + 0.4f * FxDraw.ease(t)).add(0, 1.1f, 0);
        Vector3f l = new Vector3f(lft).mul(sc), u = new Vector3f(0, sc, 0);
        out.submitCustomGeometry(ps, FxDraw.additive(FxDraw.SHIELD), (p, b) -> FxDraw.quad(b, p,
                new Vector3f(c).add(l).sub(u), new Vector3f(c).sub(l).sub(u), new Vector3f(c).sub(l).add(u), new Vector3f(c).add(l).add(u),
                0, 0, 1, 1, FxDraw.col(s.color, a)));
    }

    private void storm(State s, PoseStack ps, SubmitNodeCollector out, float t, Quaternionf cam) {
        float a = fadeInOut(s, t, 6, 0.2f);
        float y = 5.5f;
        out.submitCustomGeometry(ps, FxDraw.translucent(FxDraw.GLOW), (p, b) -> {
            RandomSource r = RandomSource.create(s.seed);
            for (int i = 0; i < 18; i++) {
                float ang = Mth.TWO_PI * i / 18 + s.age * 0.02f;
                float rad = s.radius * (0.35f + 0.65f * r.nextFloat());
                FxDraw.billboard(b, p, cam, Mth.cos(ang) * rad, y + r.nextFloat() * 0.8f, Mth.sin(ang) * rad, 1.6f + r.nextFloat(), 0,
                        FxDraw.colA(0x2C2C3A, a * 0.75f));
            }
        });
        if (((int) s.age) % 6 < 2) {
            RandomSource r = RandomSource.create(s.seed + (int) (s.age / 6));
            float ang = r.nextFloat() * Mth.TWO_PI, rad = r.nextFloat() * s.radius;
            Vector3f top = new Vector3f(Mth.cos(ang) * rad * 0.5f, y, Mth.sin(ang) * rad * 0.5f);
            Vector3f bottom = new Vector3f(Mth.cos(ang) * rad, 0, Mth.sin(ang) * rad);
            out.submitCustomGeometry(ps, RenderTypes.lightning(), (p, b) -> FxDraw.bolt(b, p, cam, top, bottom, s.seed + (long) s.age, 0.12f,
                    FxDraw.col(s.color, a), FxDraw.col(0xFFFFFF, a)));
        }
    }
}
