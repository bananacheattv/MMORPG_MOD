package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.entity.boss.BossManager;
import com.mmorpg.entity.boss.RpgBoss;
import com.mmorpg.registry.ModAttachments;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;

/** Controles reels du durcissement des boss : phases a 75/50/25 %, renforts, puissance, robustesse, base respectee. */
public final class DevBossChecks {
    private static void check(boolean yes, String message) {
        if (!yes) throw new AssertionError("boss: " + message);
    }

    public static void run(ServerPlayer p) {
        ServerLevel level = p.level();
        var d = RpgPlayers.get(p);
        var old = d.save();
        var boss = (RpgBoss) BossManager.spawnBoss(level, com.mmorpg.registry.ModEntities.GOBLIN_KING.get(), "roi_gobelin",
                p.blockPosition().offset(9, 0, 0), "test");
        check(boss != null, "boss spawned");
        try {
            var md = boss.getExistingDataOrNull(ModAttachments.MOB);
            check(boss.toughness() >= 1.79 && boss.damageMultiplier() >= 1.49, "base toughness/damage " + boss.toughness() + "/" + boss.damageMultiplier());
            boss.setTarget(p);
            for (int i = 0; i < 60; i++) boss.tick();
            check(boss.combatPhase() == 1, "phase 1 at full life");
            double before = boss.damageMultiplier();
            md.hp = md.maxHp * 0.7f;
            for (int i = 0; i < 20; i++) boss.tick();
            check(boss.combatPhase() == 2, "phase 2 at 70% life, got " + boss.combatPhase());
            md.hp = md.maxHp * 0.45f;
            for (int i = 0; i < 20; i++) boss.tick();
            check(boss.combatPhase() == 3, "phase 3 at 45% life, got " + boss.combatPhase());
            md.hp = md.maxHp * 0.2f;
            for (int i = 0; i < 20; i++) boss.tick();
            check(boss.combatPhase() == 4, "phase 4 at 20% life, got " + boss.combatPhase());
            check(boss.damageMultiplier() > before * 1.3, "damage grows with phases");
            int adds = level.getEntitiesOfClass(Mob.class, boss.getBoundingBox().inflate(40),
                    m -> m != boss && m.getExistingDataOrNull(ModAttachments.MOB) != null).size();
            check(adds >= 3, "reinforcements summoned: " + adds);
            for (int i = 0; i < 400; i++) boss.tick();                       // zones, onde de choc, charge, marque
            BlockPos home = boss.getHomePosition();
            check(Math.hypot(boss.getX() - home.getX(), boss.getZ() - home.getZ()) <= boss.getHomeRadius() + 7, "boss stays in its base");
            check(boss.isAlive(), "boss alive");
            MMORPG.LOGGER.info("[CODEX CHECKS] BOSS PASS: 4 phases (75/50/25), reinforcements {}, scaling, base respected", adds);
        } finally {
            level.getEntitiesOfClass(Mob.class, boss.getBoundingBox().inflate(60), m -> m != boss && m.getExistingDataOrNull(ModAttachments.MOB) != null)
                    .forEach(Mob::discard);
            boss.discard();
            d.load(old);
        }
    }
}
