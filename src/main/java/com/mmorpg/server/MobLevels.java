package com.mmorpg.server;

import com.mmorpg.config.ConfigManager;
import com.mmorpg.config.GeneralConfig;
import com.mmorpg.config.MobConfig;
import com.mmorpg.entity.RpgMob;
import com.mmorpg.registry.ModAttachments;
import com.mmorpg.rpg.MobData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.core.BlockPos;

import java.util.Random;

/** Attribution d'un niveau et de statistiques RPG aux monstres lors de leur apparition. */
public final class MobLevels {
    private static final Random RANDOM = new Random();

    private MobLevels() {
    }

    /** Initialise les donnees RPG d'un monstre s'il n'en a pas encore (niveau 0). */
    public static void initialize(Mob mob, ServerLevel level) {
        MobData existing = mob.getExistingDataOrNull(ModAttachments.MOB);
        if (existing != null && existing.initialized()) return;
        if (mob instanceof RpgMob rpg) {
            MobConfig cfg = ConfigManager.mob(rpg.mobKey());
            if (cfg == null) return;
            int lvl = cfg.levelFor(RANDOM);
            apply(mob, rpg.mobKey(), lvl, cfg.healthAt(lvl), cfg.attackAt(lvl), cfg.defenseAt(lvl), cfg.boss, cfg.xpMultiplier);
            AttributeInstance scale = mob.getAttribute(Attributes.SCALE);
            if (scale != null) scale.setBaseValue(cfg.scale);
            AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speed != null) speed.setBaseValue(cfg.speed);
            return;
        }
        GeneralConfig g = ConfigManager.general();
        if (!g.scaleVanillaMobs || !(mob instanceof Enemy)) return;
        int lvl = zoneLevel(level, mob.blockPosition(), mob);
        double vHp = Math.max(4, mob.getMaxHealth());
        double vAtk = mob.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE) ? Math.max(2, mob.getAttributeValue(Attributes.ATTACK_DAMAGE)) : 3;
        double hp = vHp * (2.5 + 1.2 * lvl);
        double atk = vAtk * (2.2 + 0.9 * lvl);
        apply(mob, "vanilla", lvl, hp, atk, 1.1 * lvl, false, 0.8);
    }

    /** Applique explicitement un niveau (commandes, invocations de boss). */
    public static void forceLevel(Mob mob, String key, int lvl) {
        MobConfig cfg = ConfigManager.mob(key);
        if (cfg == null) return;
        apply(mob, key, lvl, cfg.healthAt(lvl), cfg.attackAt(lvl), cfg.defenseAt(lvl), cfg.boss, cfg.xpMultiplier);
        AttributeInstance scale = mob.getAttribute(Attributes.SCALE);
        if (scale != null) scale.setBaseValue(cfg.scale);
        AttributeInstance speed = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) speed.setBaseValue(cfg.speed);
    }

    private static void apply(LivingEntity mob, String key, int lvl, double hp, double atk, double def, boolean boss, double xpMult) {
        MobData d = new MobData();
        d.key = key;
        d.level = Math.max(1, Math.min(100, lvl));
        d.maxHp = (float) Math.max(1, hp);
        d.hp = d.maxHp;
        d.atk = (float) atk;
        d.def = (float) def;
        d.boss = boss;
        d.xpMultiplier = xpMult;
        mob.setData(ModAttachments.MOB, d);
        mob.setHealth(mob.getMaxHealth());
    }

    /** Niveau de zone pour un monstre vanilla. */
    public static int zoneLevel(ServerLevel level, BlockPos pos, Mob mob) {
        GeneralConfig g = ConfigManager.general();
        int lvl;
        if ("distance".equalsIgnoreCase(g.vanillaScalingMode)) {
            BlockPos spawn = level.getRespawnData().pos();
            double dist = Math.sqrt(spawn.distSqr(pos));
            lvl = 1 + (int) (dist / Math.max(16, g.blocksPerZoneLevel));
        } else {
            Player nearest = level.getNearestPlayer(pos.getX(), pos.getY(), pos.getZ(), 96, false);
            if (nearest instanceof ServerPlayer sp) {
                lvl = RpgPlayers.get(sp).level + RANDOM.nextInt(6) - 3;
            } else {
                lvl = 1 + RANDOM.nextInt(3);
            }
        }
        if (level.dimension() == Level.NETHER) lvl = Math.max(lvl, g.netherLevelBonus + RANDOM.nextInt(10));
        if (level.dimension() == Level.END) lvl = Math.max(lvl, g.endLevelBonus + RANDOM.nextInt(10));
        return Math.max(1, Math.min(100, lvl));
    }
}
