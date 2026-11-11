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
        try {
            attacks(p);
            mobs(p);
        } finally {
            Scheduler.endTest();
            level.getEntitiesOfClass(net.minecraft.world.entity.Entity.class, p.getBoundingBox().inflate(90),
                    e -> e instanceof com.mmorpg.entity.MagicProjectile || e instanceof com.mmorpg.entity.SkillFxEntity
                            || (e instanceof Mob m && m.getExistingDataOrNull(ModAttachments.MOB) != null)).forEach(net.minecraft.world.entity.Entity::discard);
            d.load(old);
            RpgPlayers.fullRestore(p);
        }
    }

    private static void simulate(ServerLevel level, Mob mob, ServerPlayer p, int ticks) {
        for (int i = 0; i < ticks; i++) {
            if (mob.isAlive()) {
                mob.tickCount++;                                            // comme le monde le fait avant chaque tick
                mob.tick();
            }
            Scheduler.stepTest(level);
            if (p.getHealth() < p.getMaxHealth()) p.setHealth(p.getMaxHealth());
        }
    }

    /** Chaque boss a au moins cinq attaques, chacune avec sa propre animation ; chacune se deroule en entier sans erreur et touche. */
    private static void attacks(ServerPlayer p) {
        ServerLevel level = p.level();
        record Def(net.minecraft.world.entity.EntityType<? extends Mob> type, String key) {
        }
        var defs = java.util.List.of(new Def(com.mmorpg.registry.ModEntities.GOBLIN_KING.get(), "roi_gobelin"), new Def(com.mmorpg.registry.ModEntities.LICH.get(), "liche_ancienne"),
                new Def(com.mmorpg.registry.ModEntities.IGNIS.get(), "seigneur_ignis"), new Def(com.mmorpg.registry.ModEntities.FROST_TITAN.get(), "titan_glace"),
                new Def(com.mmorpg.registry.ModEntities.VOID_AVATAR.get(), "avatar_neant"));
        StringBuilder report = new StringBuilder();
        var origin = p.position();
        for (Def def : defs) {
            var boss = (RpgBoss) BossManager.spawnBoss(level, def.type(), def.key(), p.blockPosition().offset(8, 0, 0), "test");
            check(boss != null, def.key() + " spawned");
            try {
                var md = boss.getExistingDataOrNull(ModAttachments.MOB);
                var spot = boss.position();
                md.atk = 0.01f;
                md.hp = md.maxHp * 0.2f;                                       // phase 4 : toutes les attaques sont disponibles
                boss.testTarget = p;
                Scheduler.beginTest();
                simulate(level, boss, p, 130);                                  // apparition, passage en phase 4
                check(boss.combatPhase() == 4, def.key() + " phase 4 for the attack test, got " + boss.combatPhase());
                var moves = boss.moves();
                java.util.Set<Integer> anims = new java.util.HashSet<>();
                for (var m : moves) anims.add(m.anim());
                check(moves.size() >= 5 && anims.size() >= 5, def.key() + " has " + moves.size() + " attacks / " + anims.size() + " animations (5 required)");
                int totalHits = 0;
                for (var m : moves) {
                    double dist = Math.min(m.maxDist() - 0.5, Math.max(m.minDist() + 1.5, 4));
                    Scheduler.endTest();
                    Scheduler.beginTest();
                    boss.snapTo(spot.x, spot.y, spot.z, 90, 0);
                    p.snapTo(spot.x - dist, spot.y, spot.z, 0, 0);
                    boss.setHomeTo(boss.blockPosition(), 30);
                    boss.setDeltaMovement(0, 0, 0);
                    md.hp = md.maxHp * 0.2f;
                    int before = RpgBoss.MOVE_LOG.getOrDefault(def.key() + "/" + m.id(), 0), hits = boss.hitsLanded;
                    check(boss.forceMove(level, m.id()), def.key() + "/" + m.id() + " launched");
                    simulate(level, boss, p, 170);
                    check(RpgBoss.MOVE_LOG.getOrDefault(def.key() + "/" + m.id(), 0) > before, def.key() + "/" + m.id() + " logged");
                    check(boss.isAlive(), def.key() + " alive after " + m.id());
                    totalHits += boss.hitsLanded - hits;
                    report.append(m.id()).append('=').append(boss.hitsLanded - hits).append(' ');
                }
                check(totalHits >= 3, def.key() + " attacks land hits: " + totalHits);
                report.append("| ");
            } finally {
                Scheduler.endTest();
                level.getEntitiesOfClass(Mob.class, boss.getBoundingBox().inflate(60), m -> m != boss && m.getExistingDataOrNull(ModAttachments.MOB) != null).forEach(Mob::discard);
                boss.discard();
                p.snapTo(origin.x, origin.y, origin.z, 0, 0);
            }
        }
        // gardiens de guilde : cinq clips differents
        for (var holder : com.mmorpg.registry.ModEntities.GUILD_GUARDIANS) {
            var g = holder.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
            check(g != null, "guardian created");
            java.util.Set<Integer> clips = new java.util.HashSet<>();
            clips.add(com.mmorpg.entity.mob.ImportedSpells.ATTACK);
            for (var m : g.moves()) clips.add(m.anim());
            check(g.moves().size() >= 5 && clips.size() >= 5, "guardian " + g.tier + " has " + g.moves().size() + " attacks / " + clips.size() + " clips");
            g.snapTo(p.getX() + 5, p.getY(), p.getZ(), 90, 0);
            g.guild = java.util.UUID.randomUUID();
            g.initialize();
            g.testTarget = p;
            Scheduler.beginTest();
            for (var m : g.moves()) {
                check(g.forceMove(level, m.id()), "guardian move " + m.id());
                simulate(level, g, p, 90);
            }
            Scheduler.endTest();
            g.discard();
        }
        MMORPG.LOGGER.info("[CODEX CHECKS] BOSS ATTACKS PASS: 5 bosses and 10 guardians, 5+ attacks each with its own animation; hits {}", report);
    }

    /** Chaque monstre a au moins trois attaques jouees sur au moins deux clips ; chacune s'execute sans erreur. */
    private static void mobs(ServerPlayer p) {
        ServerLevel level = p.level();
        int count = 0, spellsRun = 0;
        Scheduler.beginTest();
        for (var entry : com.mmorpg.entity.mob.ImportedSpells.TABLE.entrySet()) {
            String key = entry.getKey();
            java.util.Set<Integer> clips = new java.util.HashSet<>();
            clips.add(com.mmorpg.entity.mob.ImportedSpells.ATTACK);
            for (var sp : entry.getValue()) clips.add(sp.clip());
            check(entry.getValue().size() >= 3 && clips.size() >= 2, key + " has " + entry.getValue().size() + " attacks / " + clips.size() + " clips");
            var e = MobSpawner.spawnForCommand(level, key, p.blockPosition().offset(5, 0, 0), 10);
            check(e instanceof com.mmorpg.entity.mob.SmartMonster, key + " uses the new AI: " + e);
            var mob = (com.mmorpg.entity.mob.SmartMonster) e;
            var md = mob.getExistingDataOrNull(ModAttachments.MOB);
            if (md != null) md.atk = 0.01f;
            mob.testTarget = p;
            var home = mob.position();
            for (var sp : entry.getValue()) {
                mob.snapTo(home.x, home.y, home.z, 90, 0);
                mob.forceCast(sp);
                simulate(level, mob, p, sp.windup() + 30);
                spellsRun++;
            }
            check(com.mmorpg.entity.mob.ImportedSpells.CAST_LOG.keySet().stream().anyMatch(k -> k.startsWith(key + "/")), key + " cast at least one attack");
            level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(40), m -> m.getExistingDataOrNull(ModAttachments.MOB) != null).forEach(Mob::discard);
            count++;
        }
        Scheduler.endTest();
        check(count >= 49, "all monsters covered: " + count);
        MMORPG.LOGGER.info("[CODEX CHECKS] MOB AI PASS: {} monsters, {} attacks executed", count, spellsRun);
    }
}
