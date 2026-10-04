package com.mmorpg.skill;

import com.mmorpg.combat.RpgCombat;
import com.mmorpg.combat.SkillHelper;
import com.mmorpg.combat.ThreatGoal;
import com.mmorpg.entity.FxKind;
import com.mmorpg.registry.ModAttachments;
import com.mmorpg.rpg.BuffType;
import com.mmorpg.rpg.MobData;
import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.rpg.Stat;
import com.mmorpg.server.PartyManager;
import com.mmorpg.server.RpgPlayers;
import com.mmorpg.server.Scheduler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

final class ExtendedSkills {
    private ExtendedSkills() {}
    static boolean execute(ServerPlayer p, Skill s, int rank) {
        ServerLevel level = (ServerLevel) p.level();
        var data = RpgPlayers.get(p);
        boolean magic = data.playerClass == PlayerClass.MAGE;
        double damage = data.stats.get(magic ? Stat.MAG : Stat.ATK) * s.scaled(rank);
        switch (s.id) {
            case "second_souffle" -> {
                if (data.hp >= data.stats.get(Stat.MAX_HP)) return false;
                RpgPlayers.healPercent(p, s.scaled(rank));
            }
            case "meditation" -> {
                if (data.mana >= data.stats.get(Stat.MAX_MANA)) return false;
                RpgPlayers.restoreManaPercent(p, s.scaled(rank));
            }
            case "pas_leger" -> RpgPlayers.addBuff(p, BuffType.AGILITY, s.duration, s.scaled(rank), 0);
            case "garde_partagee", "souffle_du_gardien" -> {
                for (ServerPlayer ally : level.players()) {
                    if (ally.isAlive() && (ally == p || PartyManager.sameParty(p, ally)) && ally.distanceToSqr(p) <= s.radius * s.radius) {
                        if (s.id.equals("garde_partagee")) RpgPlayers.addBuff(ally, BuffType.ARCANE_GUARD, s.duration, .30, 0);
                        else RpgPlayers.healPercent(ally, s.scaled(rank));
                        Fx.attach(level, FxKind.HALO, ally, 1, 0xFFE080, 35);
                    }
                }
            }
            case "impact_vampirique", "jugement", "souffle_draconique", "implosion" -> {
                Vec3 center = s.id.equals("implosion") ? SkillHelper.targetPoint(p, 20) : p.position();
                var targets = s.id.equals("souffle_draconique") ? SkillHelper.enemiesInCone(p, s.radius, 40)
                        : SkillHelper.enemiesInRadius(p, center, s.radius);
                int hits = 0;
                for (LivingEntity e : targets) {
                    double amount = s.id.equals("jugement") ? data.stats.get(Stat.DEF) * s.scaled(rank) : damage;
                    if (!RpgCombat.playerHit(p, e, amount, magic)) continue;
                    hits++;
                    if (s.id.equals("souffle_draconique")) e.igniteForSeconds(5);
                    if (s.id.equals("implosion")) SkillHelper.knock(e, center, -.8, .2);
                }
                if (s.id.equals("impact_vampirique") && hits > 0) RpgPlayers.healPercent(p, .03 * Math.min(5, hits));
                Fx.spawn(level, FxKind.RING, center, s.radius, data.playerClass.color, 20);
            }
            case "frappe_fragilisante", "execution", "lance_de_givre", "tir_entravant", "tir_venimeux", "tir_de_recul", "crochet_du_gardien" -> {
                LivingEntity e = SkillHelper.aimedEnemy(p, s.radius);
                if (e == null) {
                    p.sendOverlayMessage(Component.literal("Aucune cible à portée").withColor(0xFF7070));
                    return false;
                }
                if (s.id.equals("execution")) {
                    MobData md = e.getExistingDataOrNull(ModAttachments.MOB);
                    double fraction = md != null && md.initialized() ? md.hpFraction() : e.getHealth() / e.getMaxHealth();
                    if (fraction < .30) damage *= 2;
                }
                boolean hit = RpgCombat.playerHit(p, e, damage, magic);
                if (hit) switch (s.id) {
                    case "frappe_fragilisante" -> e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, s.duration, 1));
                    case "lance_de_givre", "tir_entravant" -> e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, s.duration, 2));
                    case "tir_de_recul" -> SkillHelper.knock(e, p.position(), 1.5, .3);
                    case "crochet_du_gardien" -> {
                        SkillHelper.knock(e, p.position(), -1, .2);
                        if (e instanceof Mob mob) ThreatGoal.taunt(mob, p, s.duration);
                    }
                    case "tir_venimeux" -> {
                        final double poison = damage;
                        for (int tick = 20; tick <= s.duration; tick += 20) {
                            Scheduler.later(level, tick, () -> {
                                if (!p.isRemoved() && p.isAlive() && p.level() == level && e.level() == level && !e.isRemoved() && RpgCombat.isEnemy(p, e))
                                    RpgCombat.playerHit(p, e, poison, false);
                            });
                        }
                    }
                }
                SkillHelper.line(level, p.getEyePosition(), e.position().add(0, 1, 0),
                        magic ? ParticleTypes.ENCHANT : ParticleTypes.CRIT, .4);
            }
            default -> { return false; }
        }
        Fx.attach(level, FxKind.HALO, p, .7, data.playerClass.color, 12);
        return true;
    }
}
