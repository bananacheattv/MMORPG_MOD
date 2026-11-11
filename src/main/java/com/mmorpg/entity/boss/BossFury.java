package com.mmorpg.entity.boss;

import com.mmorpg.combat.RpgCombat;
import com.mmorpg.combat.ThreatGoal;
import com.mmorpg.registry.ModAttachments;
import com.mmorpg.rpg.MobData;
import com.mmorpg.server.MobSpawner;
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
 * Durcissement commun a tous les boss (donjons et gardiens de guilde) : marque anti-kiting, renforts, enrage progressif,
 * puissance et robustesse selon le nombre de joueurs. Les attaques, elles, sont propres a chaque boss ({@link RpgBoss#defineMoves}).
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
        long nextMark, nextAdds;
        boolean enrageAnnounced;
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

    private static void warn(List<ServerPlayer> players, String text) {
        Component msg = Component.literal(text).withColor(0xFF8050);
        for (ServerPlayer p : players) p.sendOverlayMessage(msg);
    }

    // ------------------------------------------------------------------------------------------ boucle

    /** Appelee a chaque tick d'IA quand le boss a une cible : enrage, marque anti-kiting et renforts. Les attaques sont propres a chaque boss. */
    public static void tick(RpgBoss boss, ServerLevel level, int phase, State s) {
        long now = level.getGameTime();
        if (s.combatStart < 0) {
            s.combatStart = boss.tickCount;
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
        if (boss.busy()) return;
        var target = boss.getTarget();
        if (target == null || !target.isAlive()) return;
        List<ServerPlayer> near = targets(boss, level, 40);
        if (near.isEmpty()) return;
        if (phase >= 2 && now >= s.nextMark) {
            s.nextMark = now + (phase >= 3 ? 200 : 260);
            mark(boss, near);
        }
        if (phase >= 3 && now >= s.nextAdds) {
            s.nextAdds = now + (phase >= 4 ? 420 : 560);
            summon(boss, level, 2, 3 + s.near, s);
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
