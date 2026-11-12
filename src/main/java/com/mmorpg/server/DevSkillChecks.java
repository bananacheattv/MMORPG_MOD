package com.mmorpg.server;

import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.rpg.Stat;
import com.mmorpg.skill.SkillExecutor;
import com.mmorpg.skill.Skills;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Controles reels des competences remaniees : Esquive, Chimere, Boule de feu / Nova (etourdissement), Meteore. */
public final class DevSkillChecks {
    private static void check(boolean yes, String message) {
        if (!yes) throw new AssertionError("skills: " + message);
    }

    public static void run(ServerPlayer p) {
        ServerLevel level = p.level();
        var d = RpgPlayers.get(p);
        var old = d.save();
        try {
            check(Skills.get("tir_percant") == null && Skills.get("tir_venimeux") == null, "old archer skills removed");
            check(Skills.get("chimere") != null && Skills.get("esquive") != null, "chimere and esquive exist");
            check(Skills.get("boule_de_feu").cooldown == 120 && Skills.get("nova_de_givre").cooldown == 120, "mage cooldowns 6 s");
            check(Skills.get("boule_de_feu").radius == 0 && Skills.get("nova_de_givre").radius == 0, "no radius");
            check(Skills.get("meteore_celeste").radius == 15, "meteor radius 15");
            check(Skills.forClass(PlayerClass.ARCHER).size() == 11, "archer keeps 11 skills");
            p.setData(com.mmorpg.registry.ModAttachments.PLAYER, new com.mmorpg.rpg.PlayerData());
            RpgPlayers.selectClass(p, PlayerClass.ARCHER);
            d = RpgPlayers.get(p);
            d.level = 100; d.unlockSkills();
            check(d.isSkillUnlocked(Skills.get("chimere")), "chimere unlocked at level 1");
            d.mana = (float) d.stats.get(Stat.MAX_MANA) + 500; d.cooldowns.clear();
            p.setXRot(0); p.setYRot(0);
            for (int cx = -2; cx <= 1; cx++) for (int cz = -2; cz <= 1; cz++) level.setChunkForced(cx, cz, true);
            for (int cx = -2; cx <= 1; cx++) for (int cz = -2; cz <= 1; cz++) level.getChunk(cx, cz);
            p.setPos(0.5, level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, 0, 0) + 1, 0.5);
            Scheduler.beginTest();
            Vec3 start = p.position();
            check(SkillExecutor.cast(p, Skills.get("esquive")), "esquive cast");
            for (int i = 0; i < 10; i++) Scheduler.stepTest(level);
            double dist = p.position().subtract(start).horizontalDistance();
            check(Math.abs(dist - 6) < 0.6, "esquive moves about 6 blocks, got " + dist);
            check(p.position().z < start.z, "esquive goes backwards");
            d.mana = (float) d.stats.get(Stat.MAX_MANA) + 500;
            check(SkillExecutor.cast(p, Skills.get("chimere")), "chimere cast");
            var chim = level.getEntitiesOfClass(com.mmorpg.entity.MountEntity.class, p.getBoundingBox().inflate(10), e -> e.isAllyOf(p));
            check(chim.size() == 1 && chim.get(0).isAlive(), "one chimera ally spawned: " + chim.size() + " all=" + level.getEntitiesOfClass(com.mmorpg.entity.MountEntity.class, p.getBoundingBox().inflate(30)).size() + " at " + p.position());
            d.cooldowns.clear(); d.mana = (float) d.stats.get(Stat.MAX_MANA) + 500;
            SkillExecutor.cast(p, Skills.get("chimere"));
            check(level.getEntitiesOfClass(com.mmorpg.entity.MountEntity.class, p.getBoundingBox().inflate(10), e -> e.isAllyOf(p) && e.isAlive()).size() == 1, "recast replaces the chimera");
        } finally {
            for (int cx = -2; cx <= 1; cx++) for (int cz = -2; cz <= 1; cz++) level.setChunkForced(cx, cz, false);
            Scheduler.endTest();
            level.getEntitiesOfClass(Entity.class, p.getBoundingBox().inflate(40), e -> e instanceof com.mmorpg.entity.MountEntity m && m.isAllyOf(p)).forEach(Entity::discard);
            d.load(old);
            RpgPlayers.fullRestore(p);
        }
    }
}
