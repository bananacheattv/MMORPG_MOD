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
        // aggro : un monstre ne cible plus un joueur en zone sure et perd sa cible
        attacker.setPos(sp[0] + 200, sp[1], sp[2]);
        var orc = com.mmorpg.registry.ModEntities.GOBLIN_KING.get().create(level, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
        orc.setPos(sp[0] + 3, sp[1], sp[2]);
        level.addFreshEntity(orc);
        check(!orc.canAttack(victim), "monster cannot target a player in the safe zone");
        check(!com.mmorpg.combat.RpgCombat.isEnemy(orc, victim), "player in the safe zone is not an enemy of monsters");
        check(com.mmorpg.combat.RpgCombat.isEnemy(orc, attacker), "player outside the zone stays an enemy");
        orc.setTarget(victim);
        orc.tickCount = 10 - (orc.getId() & 7) + (orc.getId() & 7);   // cycle de verification
        for (int i = 0; i < 16 && orc.getTarget() != null; i++) { orc.tickCount++; orc.aiStep(); }
        check(orc.getTarget() == null, "aggro cleared when the target is in the safe zone");
        // bouclier de chargement : meme loin de toute zone sure, invincible et ignore des monstres jusqu'a sa levee
        float ahp = attacker.getHealth();
        LoadingShield.grant(attacker);
        check(LoadingShield.active(attacker) && SafeSpawn.safe(attacker), "loading shield makes the player safe anywhere");
        attacker.hurtServer(level, level.damageSources().generic(), 5F);
        check(attacker.getHealth() == ahp, "damage cancelled during loading");
        orc.setPos(sp[0] + 198, sp[1], sp[2]);
        check(!orc.canAttack(attacker), "monster cannot target a loading player");
        check(!com.mmorpg.combat.RpgCombat.isEnemy(orc, attacker), "loading player is not an enemy");
        LoadingShield.clear(attacker);
        check(!LoadingShield.active(attacker) && !SafeSpawn.safe(attacker), "shield removed");
        LoadingShield.grant(attacker);
        LoadingShield.clearAll();
        check(!LoadingShield.active(attacker), "clearAll");
        orc.discard();
        mob.discard();
        com.mmorpg.MMORPG.LOGGER.info("[CODEX CHECKS] SAFE SPAWN PASS: mine arrival point, loading shield, damage taken and dealt cancelled, outside untouched");
    }
}
