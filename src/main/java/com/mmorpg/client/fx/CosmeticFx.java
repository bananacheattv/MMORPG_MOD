package com.mmorpg.client.fx;

import com.mmorpg.client.ClientConfig;
import com.mmorpg.cosmetic.Cosmetic;
import com.mmorpg.registry.ModAttachments;
import com.mmorpg.rpg.PublicPlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** Rendu des cosmetiques animes sous forme de particules, pour tous les joueurs visibles. */
public final class CosmeticFx {
    /** Silhouette d'une aile : (envergure, hauteur) en blocs, du bord d'attache vers l'extremite. */
    private static final double[][] WING = buildWing();

    private CosmeticFx() {
    }

    private static double[][] buildWing() {
        java.util.List<double[]> pts = new java.util.ArrayList<>();
        for (int i = 0; i < 7; i++) {
            double u = 0.12 + i * 0.13;
            int rows = 7 - i;
            for (int j = 0; j < rows; j++) {
                double v = 0.55 - j * 0.14 + i * 0.05;
                pts.add(new double[]{u, v});
            }
        }
        return pts.toArray(new double[0][]);
    }

    public static void tick(Minecraft mc) {
        ClientLevel level = mc.level;
        if (level == null || mc.player == null || mc.isPaused()) return;
        long time = level.getGameTime();
        for (Player p : level.players()) {
            if (p.isInvisible() || p.isSpectator() || p.distanceToSqr(mc.player) > 64 * 64) continue;
            PublicPlayerData pub = p.getExistingDataOrNull(ModAttachments.PUBLIC);
            if (pub == null || pub.cosmetics.isEmpty()) continue;
            boolean self = p == mc.player && mc.options.getCameraType().isFirstPerson();
            for (String id : pub.cosmetics) {
                Cosmetic c = Cosmetic.byId(id);
                if (c == null) continue;
                if (self && c.category != Cosmetic.Category.TRAINEE && !ClientConfig.SHOW_OWN_COSMETICS.get()) continue;
                play(level, p, c, time);
            }
        }
    }

    private static DustParticleOptions dust(int rgb, float scale) {
        return new DustParticleOptions(rgb, scale);
    }

    private static void add(ClientLevel level, ParticleOptions o, double x, double y, double z, double vx, double vy, double vz) {
        level.addParticle(o, x, y, z, vx, vy, vz);
    }

    private static void play(ClientLevel level, Player p, Cosmetic c, long t) {
        double x = p.getX(), y = p.getY(), z = p.getZ();
        double h = p.getBbHeight();
        switch (c) {
            // ---------------------------------------------------------------- auras
            case AURA_FLAMMES -> {
                if (t % 2 == 0) {
                    for (int k = 0; k < 2; k++) {
                        double a = t * 0.25 + k * Math.PI;
                        double yy = y + (t % 40) / 40.0 * h;
                        add(level, ParticleTypes.SMALL_FLAME, x + Math.cos(a) * 0.65, yy, z + Math.sin(a) * 0.65, 0, 0.004, 0);
                    }
                }
            }
            case AURA_GIVRE -> {
                for (int k = 0; k < 2; k++) {
                    double a = -t * 0.2 + k * Math.PI;
                    double yy = y + h - (t % 50) / 50.0 * h;
                    add(level, k == 0 ? ParticleTypes.SNOWFLAKE : dust(0xC8F0FF, 0.6F), x + Math.cos(a) * 0.65, yy, z + Math.sin(a) * 0.65, 0, -0.01, 0);
                }
            }
            case AURA_ARCANE -> {
                if (t % 2 == 0) {
                    for (int k = 0; k < 3; k++) {
                        double a = t * 0.12 + k * (Math.PI * 2 / 3);
                        double yy = y + 0.3 + k * 0.55;
                        add(level, dust(0xB060FF, 0.7F), x + Math.cos(a) * 0.75, yy, z + Math.sin(a) * 0.75, 0, 0, 0);
                    }
                    if (t % 6 == 0) add(level, ParticleTypes.ENCHANT, x, y + h + 0.6, z, 0, -1.0, 0);
                }
            }
            case VORTEX_NEANT -> {
                for (int k = 0; k < 3; k++) {
                    double a = t * 0.3 + k * (Math.PI * 2 / 3);
                    double r = 0.9;
                    add(level, ParticleTypes.REVERSE_PORTAL, x + Math.cos(a) * r, y + 0.1 + (t % 30) / 30.0 * h, z + Math.sin(a) * r,
                            -Math.cos(a) * 0.05, 0.02, -Math.sin(a) * 0.05);
                }
            }
            // ---------------------------------------------------------------- halos
            case HALO_DORE -> {
                if (t % 2 == 0) ring(level, p, h + 0.3, 0.32, 10, t * 0.15, dust(0xFFD040, 0.5F));
            }
            case COURONNE_GIVRE -> {
                if (t % 2 == 0) ring(level, p, h + 0.28, 0.3, 8, -t * 0.1, dust(0xB0F0FF, 0.6F));
                if (t % 10 == 0) add(level, ParticleTypes.SNOWFLAKE, x, y + h + 0.45, z, 0, 0, 0);
            }
            case COURONNE_INFERNALE -> {
                if (t % 3 == 0) ring(level, p, h + 0.2, 0.3, 6, t * 0.2, ParticleTypes.SMALL_FLAME);
            }
            // ---------------------------------------------------------------- ailes
            case AILES_ANGELIQUES -> wings(level, p, t, 0xF8F8FF, 0xD8E8FF, 0.9);
            case AILES_DEMONIAQUES -> wings(level, p, t, 0xFF3010, 0x300808, 0.9);
            case AILES_FEE -> wings(level, p, t, 0xFF90E0, 0x80F0FF, 0.7);
            case AILES_DRAGON -> wings(level, p, t, 0x9040FF, 0x301060, 1.15);
            // ---------------------------------------------------------------- trainees
            case TRAINEE_COEURS -> {
                if (moving(p) && t % 6 == 0) add(level, ParticleTypes.HEART, x, y + 0.2, z, 0, 0, 0);
            }
            case TRAINEE_ETOILES -> {
                if (moving(p) && t % 2 == 0) add(level, ParticleTypes.END_ROD, x + rnd(0.3), y + 0.1, z + rnd(0.3), 0, 0.02, 0);
            }
            case TRAINEE_NOTES -> {
                if (moving(p) && t % 5 == 0) add(level, ParticleTypes.NOTE, x, y + 0.4, z, (t % 24) / 24.0, 0, 0);
            }
            case SILLAGE_ARDENT -> {
                if (moving(p)) {
                    add(level, ParticleTypes.FLAME, x + rnd(0.25), y + 0.05, z + rnd(0.25), 0, 0.01, 0);
                    if (t % 3 == 0) add(level, ParticleTypes.SMOKE, x, y + 0.1, z, 0, 0.02, 0);
                }
            }
        }
    }

    private static double rnd(double r) {
        return (Math.random() - 0.5) * 2 * r;
    }

    private static boolean moving(Player p) {
        double dx = p.getX() - p.xo, dz = p.getZ() - p.zo;
        return dx * dx + dz * dz > 0.0009;
    }

    private static void ring(ClientLevel level, Player p, double height, double radius, int points, double phase, ParticleOptions o) {
        for (int i = 0; i < points; i++) {
            double a = phase + i * Math.PI * 2 / points;
            level.addParticle(o, p.getX() + Math.cos(a) * radius, p.getY() + height, p.getZ() + Math.sin(a) * radius, 0, 0, 0);
        }
    }

    /** Ailes battantes dessinees point par point dans le dos du joueur. */
    private static void wings(ClientLevel level, Player p, long t, int edge, int inner, double size) {
        if (t % 2 != 0) return;
        double yaw = Math.toRadians(p.yBodyRot);
        Vec3 back = new Vec3(Math.sin(yaw), 0, -Math.cos(yaw));
        Vec3 right = new Vec3(Math.cos(yaw), 0, Math.sin(yaw));
        double flap = Math.sin(t * 0.18) * 0.45 + 0.55;
        double baseY = p.getY() + (p.isCrouching() ? 1.0 : 1.3);
        Vec3 base = new Vec3(p.getX(), baseY, p.getZ()).add(back.scale(0.28));
        DustParticleOptions edgeDust = new DustParticleOptions(edge, 0.55F);
        DustParticleOptions innerDust = new DustParticleOptions(inner, 0.5F);
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < WING.length; i++) {
                double u = WING[i][0] * size;
                double v = WING[i][1] * size;
                Vec3 pt = base.add(right.scale(side * u * Math.cos(flap * 0.9)))
                        .add(back.scale(u * Math.sin(flap * 0.9) * 0.8))
                        .add(0, v, 0);
                boolean isEdge = i % 7 == 0 || WING[i][1] > 0.5;
                level.addParticle(isEdge ? edgeDust : innerDust, pt.x, pt.y, pt.z, 0, 0, 0);
            }
        }
    }
}
