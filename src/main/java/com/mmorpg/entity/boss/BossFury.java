package com.mmorpg.entity.boss;

import com.mmorpg.combat.RpgCombat;
import com.mmorpg.combat.ThreatGoal;
import com.mmorpg.entity.FxKind;
import com.mmorpg.entity.SkillFxEntity;
import com.mmorpg.registry.ModAttachments;
import com.mmorpg.rpg.MobData;
import com.mmorpg.server.MobSpawner;
import com.mmorpg.server.Scheduler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Durcissement commun a tous les boss (donjons et gardiens de guilde) : zones telegraphiees, onde de choc, charge,
 * marque anti-kiting, renforts, enrage progressif, puissance et robustesse selon le nombre de joueurs.
 * Tout reste dans le rayon de la base du boss ; aucune modification du terrain.
 */
public final class BossFury {
    /** Ticks de combat avant l'enrage progressif (3 minutes). */
    static final long ENRAGE_AFTER = 20 * 180;

    private static final Map<String, List<String>> ADDS = Map.of(
            "roi_gobelin", List.of("gobelin", "loup_sombre"),
            "liche_ancienne", List.of("squelette_maudit"),
            "seigneur_ignis", List.of("elementaire_feu"),
            "titan_glace", List.of("spectre_givre"),
            "avatar_neant", List.of("chevalier_neant"));
    private static final List<String> GUARDIAN_ADDS = List.of("squelette_maudit", "spectre_givre");

    private BossFury() {
    }

    public static final class State {
        long combatStart = -1;
        long nextRain, nextNova, nextCharge, nextMark, nextAdds;
        int chargeWind, chargeLeft;
        Vec3 chargeDir = Vec3.ZERO;
        boolean chargeHit, enrageAnnounced;
        int near = 1;
        final List<UUID> adds = new ArrayList<>();
    }

    // ------------------------------------------------------------------------------------------ chiffres

    public static double phaseFactor(int phase) {
        return phase >= 4 ? 1.35 : phase == 3 ? 1.2 : phase == 2 ? 1.1 : 1.0;
    }

    /** Multiplicateur de degats infliges : base, joueurs, phase et enrage progressif. */
    public static double damageMultiplier(RpgBoss boss, State s, int phase, double base) {
        double enrage = 1.0;
        if (s.combatStart >= 0) {
            long over = boss.tickCount - s.combatStart - ENRAGE_AFTER;
            if (over > 0) enrage += Math.min(1.5, 0.15 * (1 + over / 600.0));
        }
        return base * (1 + 0.12 * (s.near - 1)) * phaseFactor(phase) * enrage;
    }

    /** Diviseur des degats recus : plus de joueurs, plus de vie effective. */
    public static double toughness(State s, double base) {
        return base * (1 + 0.5 * (s.near - 1));
    }

    private static List<ServerPlayer> targets(RpgBoss boss, ServerLevel level, double range) {
        return level.players().stream().filter(p -> p.isAlive() && !p.isCreative() && !p.isSpectator()
                && RpgCombat.isEnemy(boss, p) && boss.distanceToSqr(p) < range * range).toList();
    }

    private static void fx(ServerLevel level, FxKind kind, Vec3 pos, double radius, int color, int life) {
        level.addFreshEntity(SkillFxEntity.create(level, kind, pos, radius, color, life));
    }

    private static void warn(List<ServerPlayer> players, String text) {
        Component msg = Component.literal(text).withColor(0xFF8050);
        for (ServerPlayer p : players) p.sendOverlayMessage(msg);
    }

    // ------------------------------------------------------------------------------------------ boucle

    /** Appelee a chaque tick d'IA quand le boss a une cible. Renvoie vrai si une action telegraphiee occupe le boss. */
    public static boolean tick(RpgBoss boss, ServerLevel level, int phase, State s) {
        long now = level.getGameTime();
        if (s.combatStart < 0) {
            s.combatStart = boss.tickCount;
            s.nextRain = now + 140;
            s.nextNova = now + 220;
            s.nextCharge = now + 100;
            s.nextMark = now + 160;
            s.nextAdds = now + 500;
        }
        if (boss.tickCount % 20 == 0) {
            s.near = Math.max(1, Math.min(8, targets(boss, level, 48).size()));
            long over = boss.tickCount - s.combatStart - ENRAGE_AFTER;
            if (over > 0 && !s.enrageAnnounced) {
                s.enrageAnnounced = true;
                warn(targets(boss, level, 64), "☠ " + boss.getName().getString() + " entre en rage : finissez-le !");
            }
        }
        // charge en cours
        if (s.chargeLeft > 0) {
            dash(boss, level, s);
            return true;
        }
        if (s.chargeWind > 0) {
            boss.getNavigation().stop();
            if (--s.chargeWind == 0) {
                var t = boss.getTarget();
                if (t != null) {
                    Vec3 d = t.position().subtract(boss.position());
                    d = new Vec3(d.x, 0, d.z);
                    s.chargeDir = d.lengthSqr() < 1e-4 ? Vec3.ZERO : d.normalize();
                    s.chargeLeft = 14;
                    s.chargeHit = false;
                }
            }
            return true;
        }
        if (boss.busy()) return false;
        var target = boss.getTarget();
        if (target == null || !target.isAlive()) return false;
        List<ServerPlayer> near = targets(boss, level, 40);
        if (near.isEmpty()) return false;
        double d2 = boss.distanceToSqr(target);

        if (phase >= 2 && now >= s.nextMark) {
            s.nextMark = now + (phase >= 3 ? 200 : 260);
            mark(boss, near);
        }
        if (phase >= 3 && now >= s.nextAdds) {
            s.nextAdds = now + (phase >= 4 ? 420 : 560);
            summon(boss, level, 2, 3 + s.near, s);
        }
        if (phase >= 2 && now >= s.nextRain) {
            s.nextRain = now + (phase >= 4 ? 140 : phase == 3 ? 190 : 240);
            rain(boss, level, phase, near);
            return false;
        }
        if (phase >= 3 && now >= s.nextNova && d2 < 14 * 14) {
            s.nextNova = now + (phase >= 4 ? 170 : 230);
            nova(boss, level, phase, s);
            return true;
        }
        if (phase >= 2 && now >= s.nextCharge && d2 > 8 * 8 && d2 < 28 * 28 && boss.hasLineOfSight(target)) {
            s.nextCharge = now + (phase >= 4 ? 150 : phase == 3 ? 200 : 260);
            s.chargeWind = 20;
            Vec3 to = target.position().subtract(boss.position());
            fx(level, FxKind.BEAM, boss.position().add(0, 0.2, 0), 2, 0xFF5030, 20);
            fx(level, FxKind.CIRCLE, boss.position(), Math.max(2, boss.getBbWidth()), 0xFF5030, 20);
            warn(near, "⚠ Charge : écartez-vous de sa trajectoire !");
            boss.pressureAnimation();
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------------------------------ attaques

    /** Pluie de zones : un cercle sous chaque joueur plus quelques cercles autour du boss, tous annonces. */
    private static void rain(RpgBoss boss, ServerLevel level, int phase, List<ServerPlayer> near) {
        int wind = phase >= 4 ? 28 : 36;
        List<Vec3> spots = new ArrayList<>();
        for (ServerPlayer p : near) spots.add(p.position());
        var rnd = level.getRandom();
        Vec3 home = boss.hasHome() ? Vec3.atCenterOf(boss.getHomePosition()) : boss.position();
        double max = Math.max(6, boss.getHomeRadius() - 3);
        for (int i = 0; i < phase; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = 3 + rnd.nextDouble() * max;
            spots.add(new Vec3(home.x + Math.cos(a) * r, boss.getY(), home.z + Math.sin(a) * r));
        }
        int color = 0xEF5070;
        for (Vec3 c : spots) fx(level, FxKind.CIRCLE, c, 3.4, color, wind);
        warn(near, "⚠ Pluie de ruines : quittez les cercles !");
        boss.pressureAnimation();
        Scheduler.later(level, wind, () -> {
            if (!boss.isAlive() || boss.isRemoved()) return;
            for (Vec3 c : spots) {
                level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.5, c.z, 2, 1, 0.2, 1, 0);
                level.sendParticles(ParticleTypes.FLAME, c.x, c.y + 0.3, c.z, 18, 1.2, 0.1, 1.2, 0.03);
            }
            for (ServerPlayer p : targets(boss, level, 64)) {
                for (Vec3 c : spots) {
                    if (Math.hypot(p.getX() - c.x, p.getZ() - c.z) < 3.4 && Math.abs(p.getY() - c.y) < 4) {
                        RpgCombat.mobHit(boss, p, 1.5 + 0.1 * phase);
                        break;
                    }
                }
            }
        });
    }

    /** Onde de choc autour du boss : il s'immobilise puis frappe toute la zone. */
    private static void nova(RpgBoss boss, ServerLevel level, int phase, State s) {
        int wind = phase >= 4 ? 26 : 32;
        double radius = 9;
        boss.immobile(wind + 6);
        fx(level, FxKind.CIRCLE, boss.position(), radius, 0xFF4030, wind);
        warn(targets(boss, level, 48), "⚠ Onde de choc : éloignez-vous du boss !");
        boss.pressureAnimation();
        Scheduler.later(level, wind, () -> {
            if (!boss.isAlive() || boss.isRemoved()) return;
            fx(level, FxKind.SHOCKWAVE, boss.position(), radius, 0xFF6040, 14);
            level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, boss.getX(), boss.getY() + 0.5, boss.getZ(), 1, 0, 0, 0, 0);
            for (ServerPlayer p : targets(boss, level, radius + 1)) {
                if (Math.hypot(p.getX() - boss.getX(), p.getZ() - boss.getZ()) <= radius) {
                    RpgCombat.mobHit(boss, p, 1.8 + 0.1 * phase);
                    Vec3 push = p.position().subtract(boss.position()).multiply(1, 0, 1).normalize().scale(1.1);
                    if (com.mmorpg.combat.CrowdControl.allow(p)) p.push(push.x, 0.5, push.z);
                }
            }
        });
    }

    private static void dash(RpgBoss boss, ServerLevel level, State s) {
        s.chargeLeft--;
        boss.getNavigation().stop();
        BlockPos home = boss.getHomePosition();
        double dx = boss.getX() - (home.getX() + 0.5), dz = boss.getZ() - (home.getZ() + 0.5);
        boolean edge = boss.hasHome() && dx * dx + dz * dz > Math.pow(boss.getHomeRadius() - 1.5, 2)
                && dx * s.chargeDir.x + dz * s.chargeDir.z > 0;
        if (edge) {
            s.chargeLeft = 0;
            return;
        }
        boss.setDeltaMovement(s.chargeDir.x * 1.1, boss.getDeltaMovement().y, s.chargeDir.z * 1.1);
        level.sendParticles(ParticleTypes.CLOUD, boss.getX(), boss.getY() + 0.2, boss.getZ(), 4, 0.4, 0.1, 0.4, 0.02);
        if (!s.chargeHit) {
            for (ServerPlayer p : targets(boss, level, 6)) {
                if (boss.distanceTo(p) < boss.getBbWidth() / 2 + p.getBbWidth() / 2 + 1.6) {
                    s.chargeHit = true;
                    RpgCombat.mobHit(boss, p, 1.6);
                    Vec3 push = s.chargeDir.scale(1.2);
                    if (com.mmorpg.combat.CrowdControl.allow(p)) p.push(push.x, 0.45, push.z);
                }
            }
        }
    }

    /** Marque : le boss se rue sur le joueur le plus eloigne (anti-kiting), a defaut sur un joueur au hasard. */
    private static void mark(RpgBoss boss, List<ServerPlayer> near) {
        ServerPlayer pick = near.stream().filter(p -> boss.distanceToSqr(p) > 12 * 12)
                .max(Comparator.comparingDouble(boss::distanceToSqr)).orElse(null);
        if (pick == null) pick = near.get(boss.getRandom().nextInt(near.size()));
        if (near.size() < 2 && pick == boss.getTarget()) return;
        ThreatGoal.taunt(boss, pick, 100);
        pick.sendOverlayMessage(Component.literal("⚠ " + boss.getName().getString() + " vous a pris pour cible !").withColor(0xFF4040));
    }

    // ------------------------------------------------------------------------------------------ renforts

    private static List<String> addKeys(RpgBoss boss) {
        List<String> k = ADDS.get(boss.mobKey());
        return k != null ? k : GUARDIAN_ADDS;
    }

    static void summon(RpgBoss boss, ServerLevel level, int count, int maxAlive, State s) {
        MobData data = boss.getExistingDataOrNull(ModAttachments.MOB);
        if (data == null) return;
        s.adds.removeIf(id -> !(level.getEntity(id) instanceof Mob m) || !m.isAlive());
        count = Math.min(count, maxAlive - s.adds.size());
        if (count <= 0) return;
        List<String> keys = addKeys(boss);
        var rnd = level.getRandom();
        int lvl = Math.max(1, data.level - 15);
        Vec3 home = boss.hasHome() ? Vec3.atCenterOf(boss.getHomePosition()) : boss.position();
        double max = Math.max(5, boss.getHomeRadius() - 4);
        for (int i = 0; i < count; i++) {
            double a = rnd.nextDouble() * Math.PI * 2, r = 4 + rnd.nextDouble() * Math.max(1, max - 4);
            BlockPos p = BlockPos.containing(home.x + Math.cos(a) * r, boss.getY(), home.z + Math.sin(a) * r);
            var e = MobSpawner.spawnForCommand(level, keys.get(i % keys.size()), p, lvl);
            if (e == null) continue;
            s.adds.add(e.getUUID());
            if (e instanceof Mob m && boss.getTarget() != null) m.setTarget(boss.getTarget());
            level.sendParticles(ParticleTypes.PORTAL, e.getX(), e.getY() + 1, e.getZ(), 12, 0.3, 0.6, 0.3, 0.4);
        }
    }

    /** Changement de phase : une vague de renforts, plus nombreuse avec le nombre de joueurs. */
    static void onPhase(RpgBoss boss, ServerLevel level, int phase, State s) {
        if (phase < 2) return;
        summon(boss, level, Math.min(8, 2 + phase + s.near), 4 + 2 * s.near + phase, s);
        if (phase >= 4) {
            boss.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SPEED, 20 * 600, 1));
        }
    }

    static void discardAdds(ServerLevel level, State s) {
        for (UUID id : s.adds) {
            var e = level.getEntity(id);
            if (e != null) e.discard();
        }
        s.adds.clear();
    }
}
