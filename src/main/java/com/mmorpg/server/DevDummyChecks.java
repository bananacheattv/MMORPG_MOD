package com.mmorpg.server;

import com.mmorpg.MMORPG;
import com.mmorpg.combat.RpgCombat;
import com.mmorpg.combat.ThreatGoal;
import com.mmorpg.entity.dummy.TrainingDummyEntity;
import com.mmorpg.registry.ModAttachments;
import com.mmorpg.registry.ModEntities;
import com.mmorpg.rpg.PlayerClass;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Controles automatiques du mannequin d'entrainement (developpement) : lancer le client avec -Dmmorpg.checkDummy=true.
 * Verifie degats, immortalite, menace / provocation du Tank, absence de riposte, retrait par un joueur creatif et statistiques.
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class DevDummyChecks {
    private static int step;
    private static long startTick;
    private static TrainingDummyEntity dummy;
    private static double x0, z0;
    private static float hp0;
    private static com.mmorpg.rpg.PlayerData saved;

    private DevDummyChecks() {
    }

    private static void check(boolean ok, String what) {
        if (!ok) {
            MMORPG.LOGGER.error("[DUMMY CHECKS] FAIL : {}", what);
            throw new IllegalStateException("DUMMY CHECK FAILED: " + what);
        }
        MMORPG.LOGGER.info("[DUMMY CHECKS] ok : {}", what);
    }

    @SubscribeEvent
    public static void tick(PlayerTickEvent.Post event) {
        if (step >= 3 || !Boolean.getBoolean("mmorpg.checkDummy") || !(event.getEntity() instanceof ServerPlayer p) || p.tickCount < 100) return;
        if (!(p.level() instanceof ServerLevel level)) return;
        if (step == 0) {
            step = 1;
            startTick = level.getGameTime();
            var d = RpgPlayers.get(p);
            saved = new com.mmorpg.rpg.PlayerData();
            saved.load(d.save());
            d.playerClass = PlayerClass.TANK;
            d.level = 100;
            RpgPlayers.recomputeAndSync(p);
            hp0 = p.getHealth();
            dummy = ModEntities.TRAINING_DUMMY.get().create(level, null, p.blockPosition().offset(3, 0, 0), EntitySpawnReason.COMMAND, true, false);
            dummy.snapTo(p.getX() + 3, p.getY(), p.getZ(), 90F, 0F);
            dummy.setup(1);
            level.addFreshEntity(dummy);
            x0 = dummy.getX();
            z0 = dummy.getZ();
            var md = dummy.getExistingDataOrNull(ModAttachments.MOB);
            check(md != null && md.initialized() && md.level == com.mmorpg.config.ConfigManager.dummy().level, "donnees RPG initialisees");
            float hpBefore = md.hp;

            // coups physiques et magiques : les statistiques augmentent, la vie fictive ne bouge pas
            RpgCombat.dealDamage(p, dummy, 1000, false, false);
            long physical = dummy.total();
            RpgCombat.dealDamage(p, dummy, 1000, true, true);
            long magic = dummy.total() - physical;
            check(physical > 0 && magic > 0 && dummy.hits() == 2 && dummy.crits() == 1, "coups comptes (" + physical + " physique, " + magic + " magique, 1 critique)");
            check(physical < 1000 && magic < 1000 && magic > physical, "variante blindee : forte defense physique (" + physical + "), resistance magique plus faible (" + magic + ")");
            check(dummy.best() == Math.max(physical, magic) && dummy.dps() > 0, "meilleur coup et DPS");
            check(md.hp == hpBefore && dummy.isAlive(), "aucune perte de vie");
            // la menace du Tank s'accumule et la provocation fonctionne sans faire bouger ni riposter le mannequin
            // la provocation ignore les joueurs creatifs et, en mode paisible, aucune creature ne peut cibler un joueur
            p.setGameMode(GameType.SURVIVAL);
            var server = level.getServer();
            var difficulty = server.getWorldData().getDifficulty();
            server.setDifficulty(Difficulty.NORMAL, true);
            ThreatGoal.taunt(dummy, p, 100);
            boolean taunted = dummy.getTarget() == p;
            server.setDifficulty(difficulty, true);
            check(taunted, "provocation acceptee");
            // degats d'environnement ignores
            long before = dummy.total();
            dummy.hurtServer(level, level.damageSources().fall(), 50F);
            dummy.hurtServer(level, level.damageSources().onFire(), 50F);
            check(dummy.total() == before, "degats d'environnement ignores");
            // un coup de joueur creatif accroupi le retire, un sort non
            p.setGameMode(GameType.CREATIVE);
            p.setShiftKeyDown(true);
            RpgCombat.dealDamage(p, dummy, 100, false, false);
            check(!dummy.isRemoved(), "une competence d'un joueur creatif accroupi ne retire pas le mannequin");
            // remise a zero
            dummy.resetStats();
            check(dummy.total() == 0 && dummy.hits() == 0 && dummy.best() == 0 && dummy.dps() == 0, "remise a zero");
            RpgCombat.dealDamage(p, dummy, 500, false, false);
        } else if (step == 1 && level.getGameTime() - startTick >= 80) {
            step = 2;
            check(Math.abs(dummy.getX() - x0) < 1.0E-6 && Math.abs(dummy.getZ() - z0) < 1.0E-6, "immobile (pas de deplacement, pas de poussee)");
            check(p.getHealth() == hp0, "ne riposte pas");
            check(dummy.dps() == 0 || dummy.dps() < 500, "le DPS retombe apres l'arret des coups");
            // un coup direct de joueur creatif accroupi retire le mannequin
            p.setGameMode(GameType.CREATIVE);
            p.setShiftKeyDown(true);
            dummy.hurtServer(level, level.damageSources().playerAttack(p), 1F);
            check(dummy.isRemoved(), "coup direct d'un joueur creatif accroupi : mannequin retire");
            p.setShiftKeyDown(false);
            p.setGameMode(GameType.SURVIVAL);
            RpgPlayers.get(p).load(saved.save());
            RpgPlayers.recomputeAndSync(p);
            MMORPG.LOGGER.info("[DUMMY CHECKS] PASS : donnees RPG, defenses, immortalite, menace, provocation, immobilite, environnement, retrait, remise a zero");
            step = 3;
        }
    }
}
