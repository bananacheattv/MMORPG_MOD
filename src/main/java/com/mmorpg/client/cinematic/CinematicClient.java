package com.mmorpg.client.cinematic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mmorpg.MMORPG;
import com.mmorpg.client.ClientNet;
import com.mmorpg.client.Keys;
import com.mmorpg.entity.hub.ClassMasterEntity;
import com.mmorpg.hub.HubLayout;
import com.mmorpg.network.Payloads;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RenderFrameEvent;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.event.RenderPlayerEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Cinematique d'arrivee, cote client : camera libre pilotee a chaque IMAGE (temps reel, pas le tick serveur) le long de
 * trajectoires Catmull-Rom, avec rythmes (easing), FOV et roulis animes, barres de cinema, fondus, titres, sons et particules
 * synchronises sur la ligne de temps. Le HUD est masque, le joueur invisible et fige, le joueur peut sauter la scene.
 *
 * <p>Deroulement : CHARGEMENT (ecran noir, attente des chunks du parcours) &rarr; LECTURE (plans successifs) &rarr; FIN (la camera
 * rend la main au joueur exactement a la position de ses yeux, les barres se retractent) ; SAUT : fondu au noir puis fin.</p>
 */
@EventBusSubscriber(modid = MMORPG.MODID, value = Dist.CLIENT)
public final class CinematicClient {
    public static final Identifier LAYER = MMORPG.id("cinematic");

    private enum Phase { OFF, LOADING, PLAYING, SKIPPING, OUTRO }

    private record Key(float t, float v) {
    }

    private record Sound(float t, String id, float volume, float pitch) {
    }

    private record Particle(float t, String type, Vec3 pos, int count, Vec3 spread, float speed) {
    }

    private record Title(String text, String sub, int color, float from, float to) {
    }

    /** Image cle du parcours global : position, cible et champ de vision a l'instant t (secondes). */
    private record Keyframe(float t, Vec3 pos, Vec3 target, float fov, boolean cut) {
    }

    private record Shot(String name, float start, float duration, Ease ease, boolean fade, boolean last, List<Object> cam, List<Object> target,
                        List<Key> fov, List<Key> roll, Title title, List<Sound> sounds, List<Particle> particles, int classKind) {
    }

    private static final float BARS = 0.115F;
    private static final float FADE_TIME = 0.55F;

    private static Phase phase = Phase.OFF;
    private static final List<Shot> SHOTS = new ArrayList<>();
    private static BlockPos origin = BlockPos.ZERO;
    private static float fadeIn = 1.5F;
    private static float outroSeconds = 1.2F;
    private static float total;
    private static long phaseStart;
    private static long loadStart;
    private static ArmorStand camera;
    private static int shotIndex;
    private static int soundCursor;
    private static int particleCursor;
    private static int shotOfCursors = -1;
    private static float skipAlpha;
    private static float lastFov = 70F;
    private static float lastRoll;
    private static final Vec3 SPAWN_EYE_OFFSET = new Vec3(0, 1.62, 0);
    private static final float SPAWN_YAW = 180F;
    private static final float SPAWN_PITCH = 6F;
    private static List<Vec3> preload = new ArrayList<>();
    private static final List<Keyframe> PARCOURS = new ArrayList<>();
    private static List<Keyframe> warm = new ArrayList<>();
    private static boolean warming;
    private static int warmIndex;
    private static long warmStart;
    private static boolean smoothInit;
    private static float smoothYaw;
    private static float smoothPitch;
    private static long lastAimNanos;
    private static float lastPlayT;
    private static CinematicMusic music;
    private static int statFrames;
    private static int statHitches;
    private static float statMax;

    private CinematicClient() {
    }

    public static boolean active() {
        return phase != Phase.OFF;
    }

    // ================================================================== lancement

    public static void start(CompoundTag tag) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        MMORPG.LOGGER.info("[MMORPG] Cinematique : demarrage cote client");
        reset(false);
        try {
            origin = BlockPos.of(tag.getLongOr("origin", 0));
            parse(tag.getStringOr("json", "{}"));
        } catch (Exception e) {
            MMORPG.LOGGER.error("[MMORPG] Cinematique illisible, abandon", e);
            ClientNet.send(new Payloads.HubAction(Payloads.HubAction.CINEMATIC_END, 0, 0));
            return;
        }
        mc.gui.setScreen(null);
        camera = new ArmorStand(mc.level, 0, 0, 0);
        camera.setInvisible(true);
        camera.setNoGravity(true);
        smoothInit = false;
        lastAimNanos = 0;
        lastPlayT = 0F;
        warming = false;
        warmIndex = 0;
        Vec3 first = !PARCOURS.isEmpty() ? PARCOURS.get(0).pos : SHOTS.get(0).cam.get(0) instanceof Vec3 v ? v : Vec3.atCenterOf(origin);
        setCamera(first, SPAWN_YAW, SPAWN_PITCH);
        mc.setCameraEntity(camera);
        phase = Phase.LOADING;
        loadStart = System.nanoTime();
        lastFov = mc.options.fov().get();
    }

    private static Vec3 rel(JsonArray a) {
        return new Vec3(origin.getX() + 0.5 + a.get(0).getAsDouble(), origin.getY() + a.get(1).getAsDouble(), origin.getZ() + 0.5 + a.get(2).getAsDouble());
    }

    private static List<Object> path(JsonArray arr) {
        List<Object> out = new ArrayList<>();
        Vec3 eye = HubLayout.center(origin, HubLayout.SPAWN).add(SPAWN_EYE_OFFSET);
        for (JsonElement e : arr) {
            if (e.isJsonPrimitive()) {
                String s = e.getAsString();
                if ("@joueur".equals(s)) out.add(eye);
                else if ("@regard".equals(s)) out.add(eye.add(Vec3.directionFromRotation(SPAWN_PITCH, SPAWN_YAW).scale(8)));
            } else {
                out.add(rel(e.getAsJsonArray()));
            }
        }
        return out;
    }

    private static List<Key> keys(JsonObject o, String name) {
        List<Key> out = new ArrayList<>();
        if (o.has(name)) for (JsonElement e : o.getAsJsonArray(name)) out.add(new Key(e.getAsJsonArray().get(0).getAsFloat(), e.getAsJsonArray().get(1).getAsFloat()));
        return out;
    }

    private static Vec3 point(JsonElement e) {
        if (e.isJsonPrimitive()) {
            Vec3 eye = HubLayout.center(origin, HubLayout.SPAWN).add(SPAWN_EYE_OFFSET);
            return "@joueur".equals(e.getAsString()) ? eye : eye.add(Vec3.directionFromRotation(SPAWN_PITCH, SPAWN_YAW).scale(8));
        }
        return rel(e.getAsJsonArray());
    }

    private static void parse(String json) {
        SHOTS.clear();
        PARCOURS.clear();
        preload = new ArrayList<>();
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        fadeIn = root.has("fondEntree") ? root.get("fondEntree").getAsFloat() : 1.5F;
        outroSeconds = root.has("fondSortie") ? root.get("fondSortie").getAsFloat() : 1.2F;
        if (root.has("parcours")) {
            for (JsonElement e : root.getAsJsonArray("parcours")) {
                JsonObject o = e.getAsJsonObject();
                PARCOURS.add(new Keyframe(o.get("t").getAsFloat(), point(o.get("camera")), point(o.get("cible")), o.has("fov") ? o.get("fov").getAsFloat() : -1F, o.has("coupe") && o.get("coupe").getAsBoolean()));
            }
            for (Keyframe k : PARCOURS) {
                preload.add(k.pos);
                preload.add(k.target);
            }
        }
        float t = 0F;
        JsonArray plans = root.getAsJsonArray("plans");
        for (int i = 0; i < plans.size(); i++) {
            JsonObject p = plans.get(i).getAsJsonObject();
            float dur = p.get("duree").getAsFloat();
            List<Object> cam = p.has("camera") ? path(p.getAsJsonArray("camera")) : new ArrayList<>();
            List<Object> tgt = p.has("cible") ? path(p.getAsJsonArray("cible")) : new ArrayList<>();
            Title title = null;
            if (p.has("titre")) {
                JsonObject o = p.getAsJsonObject("titre");
                int color = 0xFFFFD060;
                try {
                    color = 0xFF000000 | Integer.parseInt(o.get("couleur").getAsString().replace("#", ""), 16);
                } catch (Exception ignored) {
                }
                title = new Title(o.get("texte").getAsString(), o.has("sous") ? o.get("sous").getAsString() : "", color,
                        o.has("debut") ? o.get("debut").getAsFloat() : 0.15F, o.has("fin") ? o.get("fin").getAsFloat() : 0.9F);
            }
            List<Sound> sounds = new ArrayList<>();
            if (p.has("sons")) for (JsonElement e : p.getAsJsonArray("sons")) {
                JsonObject o = e.getAsJsonObject();
                sounds.add(new Sound(o.get("t").getAsFloat(), o.get("son").getAsString(), o.has("volume") ? o.get("volume").getAsFloat() : 1F,
                        o.has("hauteur") ? o.get("hauteur").getAsFloat() : 1F));
            }
            sounds.sort((a, b) -> Float.compare(a.t, b.t));
            List<Particle> parts = new ArrayList<>();
            if (p.has("particules")) for (JsonElement e : p.getAsJsonArray("particules")) {
                JsonObject o = e.getAsJsonObject();
                JsonArray sp = o.has("dispersion") ? o.getAsJsonArray("dispersion") : null;
                Vec3 spread = sp == null ? new Vec3(1, 1, 1) : new Vec3(sp.get(0).getAsDouble(), sp.get(1).getAsDouble(), sp.get(2).getAsDouble());
                parts.add(new Particle(o.get("t").getAsFloat(), o.get("type").getAsString(), rel(o.getAsJsonArray("pos")),
                        o.has("nombre") ? o.get("nombre").getAsInt() : 10, spread, o.has("vitesse") ? o.get("vitesse").getAsFloat() : 0.05F));
            }
            parts.sort((a, b) -> Float.compare(a.t, b.t));
            int kind = -1;
            if (p.has("classe")) {
                String c = p.get("classe").getAsString().toLowerCase(Locale.ROOT);
                for (int k = 0; k < ClassMasterEntity.IDS.length; k++) if (ClassMasterEntity.IDS[k].equals(c)) kind = k;
            }
            boolean last = i == plans.size() - 1;
            boolean fade = PARCOURS.isEmpty() && !last && !"coupe".equals(p.has("transition") ? p.get("transition").getAsString() : "fondu");
            SHOTS.add(new Shot(p.has("nom") ? p.get("nom").getAsString() : "plan" + i, t, dur, Ease.byName(p.has("rythme") ? p.get("rythme").getAsString() : null),
                    fade, last, cam, tgt, keys(p, "fov"), keys(p, "roulis"), title, sounds, parts, kind));
            for (Object o : cam) if (o instanceof Vec3 v) preload.add(v);
            for (Object o : tgt) if (o instanceof Vec3 v) preload.add(v);
            t += dur;
        }
        // un fichier incomplet doit echouer ici (dans le try de start) et pas plus tard pendant la lecture
        if (SHOTS.isEmpty()) throw new IllegalArgumentException("aucun plan");
        for (Shot s : SHOTS) {
            if (!(s.duration > 0F)) throw new IllegalArgumentException("plan " + s.name + " : duree invalide");
            if (PARCOURS.isEmpty() && (s.cam.isEmpty() || s.target.isEmpty())) throw new IllegalArgumentException("plan " + s.name + " : camera ou cible manquante");
        }
        total = PARCOURS.isEmpty() ? t : PARCOURS.get(PARCOURS.size() - 1).t;
        buildWarm();
    }

    /** Points de pre-chauffage : le long du parcours, toutes les 2 s, puis retour au depart. */
    private static void buildWarm() {
        warm = new ArrayList<>();
        if (PARCOURS.isEmpty()) return;
        float end = PARCOURS.get(PARCOURS.size() - 1).t;
        for (float s = 0F; s < end; s += 2F) warm.add(sampleParcours(s));
        warm.add(PARCOURS.get(0));
    }

    // ================================================================== fin

    private static void reset(boolean notify) {
        Minecraft mc = Minecraft.getInstance();
        boolean was = phase != Phase.OFF;
        if (was && mc.player != null) {
            mc.setCameraEntity(mc.player);
            mc.player.setYRot(SPAWN_YAW);
            mc.player.setXRot(SPAWN_PITCH);
        }
        if (was) MMORPG.LOGGER.info("[MMORPG] Cinematique : fin cote client (notif serveur = {})", notify);
        stopMusic(10);
        phase = Phase.OFF;
        camera = null;
        SHOTS.clear();
        if (was && notify) ClientNet.send(new Payloads.HubAction(Payloads.HubAction.CINEMATIC_END, 0, 0));
    }

    private static void stopMusic(int fadeTicks) {
        if (music != null) {
            music.fadeOut(fadeTicks);
            music = null;
        }
    }

    public static void skip() {
        MMORPG.LOGGER.info("[MMORPG] Cinematique : saut demande (phase {})", phase);
        if (phase == Phase.PLAYING || phase == Phase.LOADING) {
            phase = Phase.SKIPPING;
            phaseStart = System.nanoTime();
            stopMusic(14);
        }
    }

    // ================================================================== images

    private static void setCamera(Vec3 pos, float yaw, float pitch) {
        if (camera == null) return;
        double y = pos.y - camera.getEyeHeight();
        camera.setPos(pos.x, y, pos.z);
        camera.xo = camera.xOld = pos.x;
        camera.yo = camera.yOld = y;
        camera.zo = camera.zOld = pos.z;
        camera.setYRot(yaw);
        camera.yRotO = yaw;
        camera.setXRot(pitch);
        camera.xRotO = pitch;
        camera.setYHeadRot(yaw);
        camera.yHeadRotO = yaw;
    }

    private static boolean chunksReady(Minecraft mc) {
        for (Vec3 v : preload) {
            if (mc.level.getChunkSource().getChunk(Mth.floor(v.x) >> 4, Mth.floor(v.z) >> 4, ChunkStatus.FULL, false) == null) return false;
        }
        return true;
    }

    @SubscribeEvent
    public static void onFrame(RenderFrameEvent.Pre event) {
        if (phase == Phase.OFF) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null || (camera == null && phase != Phase.OUTRO)) {
            reset(false);
            return;
        }
        // le joueur reste fige et regarde devant lui (la souris ne le fait pas tourner)
        mc.player.setYRot(SPAWN_YAW);
        mc.player.yRotO = SPAWN_YAW;
        mc.player.setXRot(SPAWN_PITCH);
        mc.player.xRotO = SPAWN_PITCH;
        long now = System.nanoTime();
        switch (phase) {
            case LOADING -> {
                float waited = (now - loadStart) / 1.0E9F;
                if (!warming) {
                    if ((chunksReady(mc) && waited > 0.6F) || waited > 8F) {
                        warming = true;
                        warmIndex = 0;
                        warmStart = now;
                    }
                } else if (warmIndex >= warm.size() || waited > 40F) {
                    phase = Phase.PLAYING;
                    phaseStart = now;
                    shotIndex = 0;
                    shotOfCursors = -1;
                    smoothInit = false;
                    lastAimNanos = 0;
                    mc.getMusicManager().stopPlaying();
                    music = new CinematicMusic();
                    mc.getSoundManager().play(music);
                } else {
                    // pre-chauffage : la camera survole le parcours sous l'ecran noir pour que le rendu des sections soit pret avant la lecture
                    Keyframe k = warm.get(warmIndex);
                    aim(k.pos, k.target, 0F, true);
                    lastFov = k.fov < 0 ? mc.options.fov().get() : k.fov;
                    float s = (now - warmStart) / 1.0E9F;
                    if ((mc.levelRenderer.hasRenderedAllSections() && s > 0.1F) || s > 1.0F) {
                        warmIndex++;
                        warmStart = now;
                    }
                }
            }
            case PLAYING -> {
                float t = (now - phaseStart) / 1.0E9F;
                if (t >= total) {
                    phaseSkipped = false;
                    finishScene(mc, now);
                } else {
                    play(mc, t);
                }
            }
            case SKIPPING -> {
                float s = (now - phaseStart) / 1.0E9F;
                if (s >= 0.45F) {
                    phaseSkipped = true;
                    finishScene(mc, now);
                }
            }
            case OUTRO -> {
                if ((now - phaseStart) / 1.0E9F >= outroSeconds) reset(true);
            }
            default -> {
            }
        }
    }

    private static void finishScene(Minecraft mc, long now) {
        stopMusic(30);
        MMORPG.LOGGER.info("[MMORPG] Cinematique : {} images, image la plus lente {} ms, {} images au-dela de 50 ms", statFrames, Math.round(statMax * 1000), statHitches);
        statFrames = 0;
        statHitches = 0;
        statMax = 0F;
        // la camera rend la main au joueur ; les barres se retractent pendant la fin
        mc.setCameraEntity(mc.player);
        camera = null;
        phase = Phase.OUTRO;
        phaseStart = now;
    }

    private static void play(Minecraft mc, float t) {
        Shot shot = SHOTS.get(SHOTS.size() - 1);
        for (int i = 0; i < SHOTS.size(); i++) {
            Shot s = SHOTS.get(i);
            if (t < s.start + s.duration || i == SHOTS.size() - 1) {
                shot = s;
                shotIndex = i;
                break;
            }
        }
        float local = Mth.clamp((t - shot.start) / shot.duration, 0F, 1F);
        if (!PARCOURS.isEmpty()) {
            Keyframe k = sampleParcours(t);
            for (Keyframe c : PARCOURS) if (c.cut && lastPlayT < c.t && c.t <= t) smoothInit = false;      // apres une coupe, l'orientation repart de zero
            if (music != null && lastPlayT < 10F && t >= 10F) MMORPG.LOGGER.info("[MMORPG] Cinematique : musique active = {}", mc.getSoundManager().isActive(music));
            lastPlayT = t;
            aim(k.pos, k.target, t, false);
            lastFov = k.fov < 0 ? mc.options.fov().get() : k.fov;
            lastRoll = 0F;
        } else {
            float e = shot.ease.apply(local);
            aim(spline(shot.cam, e), spline(shot.target, e), t, false);
            lastFov = shot.fov.isEmpty() ? mc.options.fov().get() : sample(shot.fov, e, mc.options.fov().get());
            lastRoll = shot.roll.isEmpty() ? 0F : sample(shot.roll, e, 0F);
        }
        // evenements du plan : sons, particules, mise en scene du maitre de classe
        if (shotOfCursors != shotIndex) {
            shotOfCursors = shotIndex;
            soundCursor = 0;
            particleCursor = 0;
            if (shot.classKind >= 0) stageMaster(mc, shot.classKind, shot.duration);
        }
        while (soundCursor < shot.sounds.size() && shot.sounds.get(soundCursor).t <= local) fire(mc, shot.sounds.get(soundCursor++));
        while (particleCursor < shot.particles.size() && shot.particles.get(particleCursor).t <= local) burst(mc, shot.particles.get(particleCursor++));
        skipAlpha = Mth.clamp((t - 1.5F) / 0.8F, 0F, 1F);
    }

    /** Oriente la camera vers la cible avec un lissage exponentiel (supprime les a-coups) ; en fin de scene elle rejoint exactement le regard du joueur. */
    private static void aim(Vec3 pos, Vec3 target, float t, boolean snap) {
        Vec3 d = target.subtract(pos);
        float yaw = (float) Math.toDegrees(Math.atan2(-d.x, d.z));
        float pitch = (float) -Math.toDegrees(Math.atan2(d.y, Math.hypot(d.x, d.z)));
        long now = System.nanoTime();
        float dt = lastAimNanos == 0 ? 0.016F : Mth.clamp((now - lastAimNanos) / 1.0E9F, 0.001F, 0.1F);
        lastAimNanos = now;
        if (!snap && smoothInit) {
            statFrames++;
            if (dt > 0.05F) statHitches++;
            statMax = Math.max(statMax, dt);
        }
        if (!smoothInit || snap) {
            smoothYaw = yaw;
            smoothPitch = pitch;
            smoothInit = true;
        } else {
            float a = 1F - (float) Math.exp(-dt / 0.16F);
            a = Math.max(a, Mth.clamp((t - (total - 0.8F)) / 0.8F, 0F, 1F));
            smoothYaw += Mth.wrapDegrees(yaw - smoothYaw) * a;
            smoothPitch += (pitch - smoothPitch) * a;
        }
        setCamera(pos, smoothYaw, smoothPitch);
    }

    /** Parcours global : interpolation d'Hermite C1 entre images cles (tangentes par differences finies, nulles aux extremites). */
    private static Keyframe sampleParcours(float t) {
        int n = PARCOURS.size();
        if (t <= PARCOURS.get(0).t) return PARCOURS.get(0);
        if (t >= PARCOURS.get(n - 1).t) return PARCOURS.get(n - 1);
        int i = 0;
        while (i < n - 2 && t > PARCOURS.get(i + 1).t) i++;
        Keyframe a = PARCOURS.get(i);
        Keyframe b = PARCOURS.get(i + 1);
        if (b.cut) return a;                                  // coupe : la camera reste en place jusqu'a l'image suivante (masquee par un fondu au noir)
        float dt = b.t - a.t;
        float u = (t - a.t) / dt;
        Vec3 pos = hermite(a.pos, b.pos, tangent(i, false), tangent(i + 1, false), dt, u);
        Vec3 tgt = hermite(a.target, b.target, tangent(i, true), tangent(i + 1, true), dt, u);
        float k = u * u * (3F - 2F * u);
        float fa = a.fov;
        float fb = b.fov;
        float fov = fa < 0 || fb < 0 ? (u < 0.5F ? fa : fb) : fa + (fb - fa) * k;
        return new Keyframe(t, pos, tgt, fov, false);
    }

    /** Diagnostic (tests) : {position, cible} du parcours a l'instant t, ou null s'il n'y a pas de parcours global. */
    public static Vec3[] debugSample(float t) {
        if (PARCOURS.isEmpty()) return null;
        Keyframe k = sampleParcours(t);
        return new Vec3[]{k.pos, k.target};
    }

    public static float debugTotal() {
        return total;
    }

    private static Vec3 tangent(int i, boolean target) {
        int n = PARCOURS.size();
        if (i <= 0 || i >= n - 1 || PARCOURS.get(i).cut || PARCOURS.get(i + 1).cut) return Vec3.ZERO;
        Keyframe p = PARCOURS.get(i - 1);
        Keyframe q = PARCOURS.get(i + 1);
        Vec3 d = target ? q.target.subtract(p.target) : q.pos.subtract(p.pos);
        return d.scale(1.0 / (q.t - p.t));
    }

    private static Vec3 hermite(Vec3 p0, Vec3 p1, Vec3 m0, Vec3 m1, float dt, float u) {
        double u2 = u * u;
        double u3 = u2 * u;
        double h00 = 2 * u3 - 3 * u2 + 1;
        double h10 = u3 - 2 * u2 + u;
        double h01 = -2 * u3 + 3 * u2;
        double h11 = u3 - u2;
        return p0.scale(h00).add(m0.scale(h10 * dt)).add(p1.scale(h01)).add(m1.scale(h11 * dt));
    }

    private static void stageMaster(Minecraft mc, int kind, float seconds) {
        AABB box = HubLayout.hubBounds(origin);
        for (ClassMasterEntity m : mc.level.getEntitiesOfClass(ClassMasterEntity.class, box)) {
            if (m.kind() == kind) m.stage(seconds);
        }
    }

    private static void fire(Minecraft mc, Sound s) {
        var ev = BuiltInRegistries.SOUND_EVENT.get(Identifier.parse(s.id));
        if (ev.isPresent()) mc.getSoundManager().play(SimpleSoundInstance.forUI(ev.get().value(), s.pitch, s.volume));
    }

    private static void burst(Minecraft mc, Particle p) {
        var type = BuiltInRegistries.PARTICLE_TYPE.get(Identifier.withDefaultNamespace(p.type));
        if (type.isEmpty() || !(type.get().value() instanceof ParticleOptions options)) return;
        var rnd = mc.level.getRandom();
        for (int i = 0; i < p.count; i++) {
            double x = p.pos.x + (rnd.nextDouble() - 0.5) * p.spread.x * 2;
            double y = p.pos.y + (rnd.nextDouble() - 0.5) * p.spread.y * 2;
            double z = p.pos.z + (rnd.nextDouble() - 0.5) * p.spread.z * 2;
            mc.level.addParticle(options, x, y, z, (rnd.nextDouble() - 0.5) * p.speed, rnd.nextDouble() * p.speed + p.speed * 0.4, (rnd.nextDouble() - 0.5) * p.speed);
        }
    }

    // ================================================================== courbes

    private static Vec3 spline(List<Object> pts, float u) {
        int n = pts.size();
        if (n == 1) return (Vec3) pts.get(0);
        float f = Mth.clamp(u, 0F, 1F) * (n - 1);
        int i = Math.min(n - 2, (int) f);
        float t = f - i;
        Vec3 p0 = (Vec3) pts.get(Math.max(0, i - 1));
        Vec3 p1 = (Vec3) pts.get(i);
        Vec3 p2 = (Vec3) pts.get(i + 1);
        Vec3 p3 = (Vec3) pts.get(Math.min(n - 1, i + 2));
        double t2 = t * t;
        double t3 = t2 * t;
        return new Vec3(cr(p0.x, p1.x, p2.x, p3.x, t, t2, t3), cr(p0.y, p1.y, p2.y, p3.y, t, t2, t3), cr(p0.z, p1.z, p2.z, p3.z, t, t2, t3));
    }

    private static double cr(double a, double b, double c, double d, double t, double t2, double t3) {
        return 0.5 * ((2 * b) + (-a + c) * t + (2 * a - 5 * b + 4 * c - d) * t2 + (-a + 3 * b - 3 * c + d) * t3);
    }

    /** Interpolation douce (smoothstep) entre cles ; une valeur negative signifie {@code fallback} (champ de vision des options). */
    private static float sample(List<Key> keys, float u, float fallback) {
        if (keys.size() == 1) return keys.get(0).v < 0 ? fallback : keys.get(0).v;
        for (int i = 0; i < keys.size() - 1; i++) {
            Key a = keys.get(i);
            Key b = keys.get(i + 1);
            if (u <= b.t || i == keys.size() - 2) {
                float k = Mth.clamp((u - a.t) / Math.max(1.0E-4F, b.t - a.t), 0F, 1F);
                k = k * k * (3F - 2F * k);
                float va = a.v < 0 ? fallback : a.v;
                float vb = b.v < 0 ? fallback : b.v;
                return va + (vb - va) * k;
            }
        }
        return fallback;
    }

    // ================================================================== evenements du jeu

    @SubscribeEvent
    public static void onFov(ViewportEvent.ComputeFov event) {
        if (phase == Phase.PLAYING || phase == Phase.LOADING || phase == Phase.SKIPPING) event.setFOV(lastFov);
    }

    @SubscribeEvent
    public static void onAngles(ViewportEvent.ComputeCameraAngles event) {
        if (phase == Phase.PLAYING) event.setRoll(lastRoll);
    }

    /** HUD de Minecraft et du mod masques pendant toute la scene (la couche de la cinematique reste). */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onLayer(RenderGuiLayerEvent.Pre event) {
        if (phase != Phase.OFF && !event.getName().equals(LAYER)) event.setCanceled(true);
    }

    /** Le joueur local est invisible pendant la scene. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlayerRender(RenderPlayerEvent.Pre<?> event) {
        Minecraft mc = Minecraft.getInstance();
        if (phase != Phase.OFF && mc.player != null && event.getRenderState().id == mc.player.getId()) event.setCanceled(true);
    }

    /** Pas de main en vue subjective : la camera n'appartient plus au joueur. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onHand(net.neoforged.neoforge.client.event.RenderHandEvent event) {
        if (phase != Phase.OFF) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onInteraction(InputEvent.InteractionKeyMappingTriggered event) {
        if (phase != Phase.OFF) event.setCanceled(true);
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScroll(InputEvent.MouseScrollingEvent event) {
        if (phase != Phase.OFF) event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onKey(InputEvent.Key event) {
        if (phase != Phase.OFF && event.getAction() == 1 && Keys.SKIP.matches(event.getKeyEvent())) skip();
    }

    /** Echap saute la scene au lieu d'ouvrir le menu pause. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onScreen(ScreenEvent.Opening event) {
        if (phase != Phase.OFF && event.getNewScreen() instanceof PauseScreen) {
            event.setCanceled(true);
            skip();
        }
    }

    /** Pendant la scene, aucune autre musique que la sienne. */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onPlaySound(net.neoforged.neoforge.client.event.sound.PlaySoundEvent event) {
        if (phase != Phase.OFF && event.getSound() != null && event.getSound().getSource() == net.minecraft.sounds.SoundSource.MUSIC && !(event.getSound() instanceof CinematicMusic)) {
            event.setSound(null);
        }
    }

    @SubscribeEvent
    public static void onLogout(ClientPlayerNetworkEvent.LoggingOut event) {
        stopMusic(1);
        phase = Phase.OFF;
        camera = null;
        SHOTS.clear();
    }

    // ================================================================== couche d'affichage

    public static void render(GuiGraphicsExtractor g, net.minecraft.client.DeltaTracker delta) {
        if (phase == Phase.OFF) return;
        Minecraft mc = Minecraft.getInstance();
        int w = g.guiWidth();
        int h = g.guiHeight();
        long now = System.nanoTime();
        float black = 0F;
        float bars = 0F;
        switch (phase) {
            case LOADING -> {
                black = 1F;
                g.fill(0, 0, w, h, 0xFF000000);
                g.pose().pushMatrix();
                g.pose().translate(w / 2F, h / 2F - 14);
                g.pose().scale(3F, 3F);
                g.centeredText(mc.font, com.mmorpg.client.AeloriaFont.of("AELORIA"), 0, 0, 0xFFFFD060);
                g.pose().popMatrix();
                float progress = warming && !warm.isEmpty() ? Mth.clamp((float) warmIndex / warm.size(), 0F, 1F) : 0F;
                int bw = Math.min(200, w / 3);
                g.fill(w / 2 - bw / 2, h / 2 + 14, w / 2 + bw / 2, h / 2 + 16, 0x55FFFFFF);
                g.fill(w / 2 - bw / 2, h / 2 + 14, w / 2 - bw / 2 + Math.round(bw * progress), h / 2 + 16, 0xFFFFD060);
                g.centeredText(mc.font, Component.literal("Préparation de la citadelle…"), w / 2, h / 2 + 22, 0xFFC8C0B0);
                return;
            }
            case PLAYING -> {
                float t = (now - phaseStart) / 1.0E9F;
                black = Math.max(black, 1F - Ease.IN_OUT_CUBIC.apply(Mth.clamp(t / fadeIn, 0F, 1F)));
                for (Keyframe c : PARCOURS) {
                    if (!c.cut) continue;
                    float d = t - c.t;                         // noir complet de -0,1 s a +0,25 s autour de la coupe
                    float f = d < -0.1F ? Mth.clamp(1F + (d + 0.1F) / 0.5F, 0F, 1F) : d > 0.25F ? Mth.clamp(1F - (d - 0.25F) / 0.6F, 0F, 1F) : 1F;
                    black = Math.max(black, f * f * (3F - 2F * f));
                }
                bars = Ease.IN_OUT_CUBIC.apply(Mth.clamp((t - 0.2F) / 1.2F, 0F, 1F));
                Shot s = SHOTS.get(Math.min(shotIndex, SHOTS.size() - 1));
                // fondu au noir entre deux plans : noir complet a la jonction
                float local = t - s.start;
                if (s.fade) black = Math.max(black, 1F - Ease.IN_OUT_CUBIC.apply(Mth.clamp((s.duration - local) / FADE_TIME, 0F, 1F)));
                if (shotIndex > 0 && SHOTS.get(shotIndex - 1).fade) black = Math.max(black, 1F - Ease.IN_OUT_CUBIC.apply(Mth.clamp(local / FADE_TIME, 0F, 1F)));
                drawBars(g, w, h, bars);
                drawTitle(g, mc, w, h, s, local / s.duration, bars);
                drawSkipHint(g, mc, w, h);
            }
            case SKIPPING -> {
                float s = (now - phaseStart) / 1.0E9F;
                black = Ease.IN_OUT_CUBIC.apply(Mth.clamp(s / 0.45F, 0F, 1F));
                drawBars(g, w, h, 1F);
            }
            case OUTRO -> {
                float s = (now - phaseStart) / 1.0E9F;
                bars = 1F - Ease.IN_OUT_CUBIC.apply(Mth.clamp(s / outroSeconds, 0F, 1F));
                black = phaseSkipped ? 1F - Ease.IN_OUT_CUBIC.apply(Mth.clamp(s / (outroSeconds * 0.6F), 0F, 1F)) : 0F;
                drawBars(g, w, h, bars);
            }
            default -> {
            }
        }
        if (black > 0.002F) g.fill(0, 0, w, h, (int) (black * 255) << 24);
    }

    private static boolean phaseSkipped;

    private static void drawBars(GuiGraphicsExtractor g, int w, int h, float amount) {
        int bh = Math.round(h * BARS * amount);
        if (bh <= 0) return;
        g.fill(0, 0, w, bh, 0xFF000000);
        g.fill(0, h - bh, w, h, 0xFF000000);
    }

    private static void drawTitle(GuiGraphicsExtractor g, Minecraft mc, int w, int h, Shot s, float local, float bars) {
        Title t = s.title;
        if (t == null || bars < 0.5F) return;
        float inT = Ease.OUT_CUBIC.apply(Mth.clamp((local - t.from) / (0.5F / s.duration), 0F, 1F));
        float outT = 1F - Ease.IN_OUT_CUBIC.apply(Mth.clamp((local - (t.to - 0.5F / s.duration)) / (0.5F / s.duration), 0F, 1F));
        float a = Math.min(inT, outT);
        if (a <= 0.01F) return;
        int alpha = (int) (a * 255) << 24;
        int x = Math.round(w * 0.08F + (1F - inT) * 28F);
        int y = h - Math.round(h * BARS) - 54;
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(2.4F, 2.4F);
        g.text(mc.font, com.mmorpg.client.AeloriaFont.of(t.text), 0, 0, (t.color & 0xFFFFFF) | alpha, true);
        g.pose().popMatrix();
        g.fill(x, y + 24, x + Math.round(60 * a), y + 25, (t.color & 0xFFFFFF) | alpha);
        g.pose().pushMatrix();
        g.pose().translate(x, y + 30);
        g.pose().scale(1.15F, 1.15F);
        g.text(mc.font, t.sub, 0, 0, 0xE8E0D0 | alpha, true);
        g.pose().popMatrix();
    }

    private static void drawSkipHint(GuiGraphicsExtractor g, Minecraft mc, int w, int h) {
        if (skipAlpha <= 0.01F) return;
        String msg = "[" + Keys.SKIP.getTranslatedKeyMessage().getString() + "] Passer";
        int alpha = (int) (skipAlpha * 200) << 24;
        g.text(mc.font, msg, w - mc.font.width(msg) - 12, h - Math.round(h * BARS) - 14, 0xC8C0B0 | alpha, true);
    }
}
