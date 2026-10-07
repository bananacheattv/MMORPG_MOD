package com.mmorpg.combat;

import com.mmorpg.registry.ModAttachments;
import com.mmorpg.rpg.MobData;
import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.server.RpgPlayers;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Menace temporaire propre au monstre : aucune reference globale aux entites. */
public final class ThreatGoal extends Goal {
    private record Entry(double amount, long expires) {}
    private final Mob mob;
    private final Map<UUID, Entry> threat = new HashMap<>();
    private UUID forced;
    private long forcedUntil;
    private Player chosen;

    private ThreatGoal(Mob mob) {
        this.mob = mob;
        setFlags(EnumSet.of(Flag.TARGET));
    }

    public static ThreatGoal install(Mob mob) {
        for (var goal : mob.targetSelector.getAvailableGoals()) {
            if (goal.getGoal() instanceof ThreatGoal t) return t;
        }
        ThreatGoal goal = new ThreatGoal(mob);
        mob.targetSelector.addGoal(0, goal);
        return goal;
    }

    public static void damage(Mob mob, ServerPlayer player, double amount) {
        MobData data = mob.getExistingDataOrNull(ModAttachments.MOB);
        if (data == null || !data.initialized() || amount <= 0 || !Double.isFinite(amount)) return;
        ThreatGoal goal = install(mob);
        double multiplier = RpgPlayers.get(player).playerClass == PlayerClass.TANK ? 2.0 : 1.0;
        Entry old = goal.threat.get(player.getUUID());
        goal.threat.put(player.getUUID(), new Entry((old == null ? 0 : old.amount) + amount * multiplier,
                mob.level().getGameTime() + 1200));
    }

    public static void taunt(Mob mob, ServerPlayer player, int ticks) {
        ThreatGoal goal = install(mob);
        if (!goal.valid(player)) return;
        long now = mob.level().getGameTime();
        double top = goal.threat.values().stream().mapToDouble(Entry::amount).max().orElse(0);
        goal.threat.put(player.getUUID(), new Entry(top + 1, now + 1200));
        goal.forced = player.getUUID();
        goal.forcedUntil = now + Math.max(1, ticks);
        mob.setTarget(player);
    }

    public static void clear(Mob mob) {
        for (var goal : mob.targetSelector.getAvailableGoals()) {
            if (goal.getGoal() instanceof ThreatGoal t) {
                t.threat.clear();
                t.forced = null;
                t.chosen = null;
            }
        }
        mob.setTarget(null);
    }

    private boolean valid(Player p) {
        if (p == null || !p.isAlive() || p.isRemoved() || p.level() != mob.level()
                || p.isCreative() || p.isSpectator()) return false;
        double range = Math.max(16, mob.getAttributeValue(Attributes.FOLLOW_RANGE));
        if (mob.distanceToSqr(p) > range * range) return false;
        // Respecter la base des boss meme lorsqu'une provocation est active.
        return !mob.hasHome() || p.distanceToSqr(mob.getHomePosition().getX() + 0.5,
                mob.getHomePosition().getY(), mob.getHomePosition().getZ() + 0.5)
                <= Math.pow(mob.getHomeRadius() + 14, 2);
    }

    private Player select() {
        long now = mob.level().getGameTime();
        threat.entrySet().removeIf(e -> e.getValue().expires <= now || !valid(mob.level().getPlayerByUUID(e.getKey())));
        if (forced != null && now < forcedUntil) {
            Player p = mob.level().getPlayerByUUID(forced);
            if (valid(p)) return p;
        }
        forced = null;
        Player best = mob.getTarget() instanceof Player p && valid(p) && threat.containsKey(p.getUUID()) ? p : null;
        // Hysteresis : evite les changements incessants entre deux attaquants proches.
        double score = best == null ? 0 : threat.get(best.getUUID()).amount * 1.1;
        for (var entry : threat.entrySet()) {
            if (entry.getValue().amount > score) {
                best = mob.level().getPlayerByUUID(entry.getKey());
                score = entry.getValue().amount;
            }
        }
        return best;
    }

    @Override public boolean canUse() { chosen = select(); return chosen != null; }
    @Override public boolean canContinueToUse() { return canUse(); }
    @Override public void start() { mob.setTarget(chosen); }
    @Override public void tick() { chosen = select(); mob.setTarget(chosen); }
    @Override public void stop() { chosen = null; mob.setTarget(null); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
}
