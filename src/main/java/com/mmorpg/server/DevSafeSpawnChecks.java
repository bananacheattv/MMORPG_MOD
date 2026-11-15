package com.mmorpg.server;

import com.mmorpg.dungeon.InstanceManager;
import com.mmorpg.mine.MineManager;
import net.minecraft.server.MinecraftServer;

/** Verifie les zones sures aux points d'arrivee (mines, plaines, donjons). */
final class DevSafeSpawnChecks {
    private DevSafeSpawnChecks() { }

    private static void check(boolean value, String message) { if (!value) throw new AssertionError("safe spawn: " + message); }

    static void run(MinecraftServer server) {
        var level = server.getLevel(InstanceManager.DIM);
        check(level != null, "dimension");
        MineManager.registerZones(server);
        var victim = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "SafeVictim"));
        var attacker = net.neoforged.neoforge.common.util.FakePlayerFactory.get(level, new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "SafeAttacker"));
        double[] sp = MineManager.spawnPoint(0);
        victim.setPos(sp[0], sp[1], sp[2]);
        attacker.setPos(sp[0] + 1, sp[1], sp[2]);
        check(SafeSpawn.safe(victim), "mine arrival point is safe");
        float hp = victim.getHealth();
        victim.hurtServer(level, level.damageSources().generic(), 5F);
        check(victim.getHealth() == hp, "damage taken is cancelled at the mine spawn");
        victim.hurtServer(level, level.damageSources().playerAttack(attacker), 5F);
        check(victim.getHealth() == hp, "damage between players cancelled in the zone");
        var mob = net.minecraft.world.entity.EntityTypes.PIG.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        mob.setPos(sp[0] + 200, sp[1], sp[2]);
        level.addFreshEntity(mob);
        float mhp = mob.getHealth();
        mob.hurtServer(level, level.damageSources().playerAttack(victim), 5F);
        check(mob.getHealth() == mhp, "damage dealt from the zone is cancelled");
        attacker.setPos(sp[0] + 200, sp[1], sp[2]);
        check(!SafeSpawn.safe(attacker), "far from the spawn is not safe");
        mob.hurtServer(level, level.damageSources().playerAttack(attacker), 5F);
        check(mob.getHealth() < mhp, "damage outside the zone is untouched");
        mob.discard();
        com.mmorpg.MMORPG.LOGGER.info("[CODEX CHECKS] SAFE SPAWN PASS: mine arrival point, damage taken and dealt cancelled, outside untouched");
    }
}
