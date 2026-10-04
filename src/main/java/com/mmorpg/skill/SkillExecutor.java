package com.mmorpg.skill;

import com.mmorpg.combat.RpgCombat;
import com.mmorpg.combat.SkillHelper;
import com.mmorpg.entity.CastAnim;
import com.mmorpg.entity.FxKind;
import com.mmorpg.entity.SpellProjectile;
import com.mmorpg.item.ItemDefs;
import com.mmorpg.rpg.BuffType;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.rpg.Stat;
import com.mmorpg.server.RpgPlayers;
import com.mmorpg.server.Scheduler;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Execution des competences actives cote serveur. */
public final class SkillExecutor {
    private SkillExecutor() {
    }

    public static void castSlot(ServerPlayer player, int slot) {
        PlayerData d = RpgPlayers.get(player);
        if (slot < 0 || slot >= PlayerData.SKILL_SLOTS) return;
        Skill skill = Skills.get(d.skillBar[slot]);
        if (skill == null) return;
        cast(player, skill);
    }

    public static boolean cast(ServerPlayer player, Skill skill) {
        PlayerData d = RpgPlayers.get(player);
        if (!player.isAlive() || player.isSpectator() || skill.passive || !d.isSkillUnlocked(skill)) return false;
        long now = player.level().getGameTime();
        Long readyAt = d.cooldowns.get(skill.id);
        if (readyAt != null && readyAt > now) {
            player.sendOverlayMessage(Component.literal(skill.name + " n'est pas prêt (" + String.format(java.util.Locale.FRANCE, "%.1f", (readyAt - now) / 20.0) + " s)").withColor(0xFF7070));
            return false;
        }
        if (d.mana < skill.manaCost) {
            player.sendOverlayMessage(Component.literal("Mana insuffisant pour " + skill.name).withColor(0x5090FF));
            return false;
        }
        int rank = d.skillRank(skill.id);
        if (!execute(player, skill, rank, d)) return false;
        d.mana -= skill.manaCost;
        double cdr = d.stats.get(Stat.CDR) / 100.0;
        d.cooldowns.put(skill.id, now + Math.max(10, Math.round(skill.cooldown * (1 - cdr))));
        d.dirty = true;
        return true;
    }

    private static void sound(ServerPlayer p, SoundEvent s, float vol, float pitch) {
        p.level().playSound(null, p.getX(), p.getY(), p.getZ(), s, SoundSource.PLAYERS, vol, pitch);
    }

    private static Vec3 center(LivingEntity e) {
        return e.position().add(0, e.getBbHeight() * 0.5, 0);
    }

    private static boolean execute(ServerPlayer p, Skill s, int rank, PlayerData d) {
        ServerLevel level = p.level();
        double atk = d.stats.get(Stat.ATK);
        double mag = d.stats.get(Stat.MAG);
        double def = d.stats.get(Stat.DEF);
        Vec3 pos = p.position();
        switch (s.id) {
            // ============================================================ GUERRIER
            case "frappe_puissante" -> {
                Fx.cast(p, CastAnim.SLAM, 9);
                Scheduler.later(level, 3, () -> {
                    p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
                    Fx.facing(level, FxKind.SLASH, p, s.radius + 0.8, 0xFF6A30, 9);
                    for (LivingEntity e : SkillHelper.enemiesInCone(p, s.radius, 50)) {
                        RpgCombat.playerHit(p, e, atk * s.scaled(rank), false);
                        SkillHelper.knock(e, p.position(), 0.9, 0.3);
                        Fx.spawn(level, FxKind.BURST, center(e), 0.9, 0xFFB060, 8);
                    }
                    sound(p, SoundEvents.PLAYER_ATTACK_STRONG, 1f, 0.8f);
                });
            }
            case "cri_de_guerre" -> {
                double bonus = s.power + 0.04 * (rank - 1);
                Fx.cast(p, CastAnim.LEAP, 16);
                Fx.attach(level, FxKind.PILLAR, p, 0.8, 0xFF5020, 24);
                Fx.spawn(level, FxKind.SHOCKWAVE, pos, s.radius, 0xFF6030, 16);
                for (Player ally : level.getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(s.radius))) {
                    if (ally instanceof ServerPlayer sp) {
                        RpgPlayers.addBuff(sp, BuffType.WAR_CRY, s.duration, bonus, s.power2);
                        Fx.attach(level, FxKind.HALO, sp, 0.9, 0xFF8040, 30);
                    }
                }
                sound(p, SoundEvents.RAVAGER_ROAR, 1f, 1.1f);
            }
            case "tourbillon" -> {
                Fx.cast(p, CastAnim.SPIN, 12);
                Fx.attach(level, FxKind.WHIRL, p, s.radius, 0xFFB070, 12);
                for (int i = 0; i < 3; i++) {
                    final int k = i;
                    Scheduler.later(level, i * 4, () -> sound(p, SoundEvents.PLAYER_ATTACK_SWEEP, 1f, 0.9f + k * 0.15f));
                }
                for (LivingEntity e : SkillHelper.enemiesInRadius(p, pos.add(0, 1, 0), s.radius)) {
                    RpgCombat.playerHit(p, e, atk * s.scaled(rank), false);
                    SkillHelper.knock(e, pos, 0.6, 0.2);
                    Fx.spawn(level, FxKind.BURST, center(e), 0.7, 0xFFC080, 7);
                }
            }
            case "charge_brutale" -> {
                Vec3 dir = p.getLookAngle().multiply(1, 0, 1).normalize();
                Vec3 end = SkillHelper.blockLimitedEnd(p, s.radius);
                double dist = Math.max(2, Math.min(s.radius, end.subtract(p.getEyePosition()).length()));
                p.setDeltaMovement(dir.scale(dist * 0.32).add(0, 0.15, 0));
                p.needsSync = true;
                Fx.cast(p, CastAnim.BASH, 10);
                Fx.attach(level, FxKind.TRAIL, p, 1, 0xFF7040, 14);
                Set<Integer> hit = new HashSet<>();
                for (int t = 0; t <= 8; t += 2) {
                    Scheduler.later(level, t, () -> {
                        for (LivingEntity e : SkillHelper.enemiesInRadius(p, p.position().add(0, 1, 0), 2.2)) {
                            if (hit.add(e.getId())) {
                                RpgCombat.playerHit(p, e, atk * s.scaled(rank), false);
                                e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, s.duration, 6));
                                if (e instanceof Mob m) m.getNavigation().stop();
                                SkillHelper.knock(e, p.position(), 0.8, 0.4);
                                Fx.spawn(level, FxKind.BURST, center(e), 1.0, 0xFFA060, 8);
                            }
                        }
                    });
                }
                Scheduler.later(level, 9, () -> Fx.spawn(level, FxKind.RING, p.position(), 2.5, 0xFF8040, 10));
                sound(p, SoundEvents.RAVAGER_ATTACK, 1f, 1f);
            }
            case "rage_sanguinaire" -> {
                RpgPlayers.addBuff(p, BuffType.RAGE, s.duration, s.power + 0.05 * (rank - 1), s.power2);
                Fx.cast(p, CastAnim.RAISE, 14);
                Fx.attach(level, FxKind.AURA, p, 1, 0xFF2020, s.duration);
                Fx.spawn(level, FxKind.SHOCKWAVE, pos, 3.5, 0xD01010, 14);
                sound(p, SoundEvents.WARDEN_ROAR, 0.6f, 1.4f);
            }
            case "fureur_divine" -> {
                p.setDeltaMovement(p.getLookAngle().multiply(1, 0, 1).normalize().scale(0.6).add(0, 1.1, 0));
                p.needsSync = true;
                Fx.cast(p, CastAnim.LEAP, 22);
                Fx.attach(level, FxKind.TRAIL, p, 1, 0xFFD060, 22);
                sound(p, SoundEvents.ENDER_DRAGON_FLAP, 1f, 0.8f);
                Scheduler.later(level, 16, () -> {
                    p.setDeltaMovement(0, -2.0, 0);
                    p.needsSync = true;
                });
                Scheduler.later(level, 22, () -> {
                    Vec3 c = p.position();
                    Fx.spawn(level, FxKind.SHOCKWAVE, c, s.radius, 0xFFD060, 20);
                    Fx.spawn(level, FxKind.CRACKS, c, s.radius * 0.9, 0xFFB040, 40);
                    Fx.spawn(level, FxKind.PILLAR, c, 1.2, 0xFFE080, 16);
                    Fx.spawn(level, FxKind.BURST, c.add(0, 0.5, 0), 2.5, 0xFFC050, 14);
                    LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
                    if (bolt != null) {
                        bolt.snapTo(c.x, c.y, c.z);
                        bolt.setVisualOnly(true);
                        level.addFreshEntity(bolt);
                    }
                    for (LivingEntity e : SkillHelper.enemiesInRadius(p, c.add(0, 1, 0), s.radius)) {
                        RpgCombat.playerHit(p, e, atk * s.scaled(rank), false);
                        SkillHelper.knock(e, c, 1.0, 0.9);
                    }
                    p.fallDistance = 0;
                    sound(p, SoundEvents.GENERIC_EXPLODE.value(), 1.5f, 0.7f);
                });
            }
            // ============================================================ MAGE
            case "boule_de_feu" -> {
                Fx.cast(p, CastAnim.RAISE, 10);
                Fx.attach(level, FxKind.CIRCLE, p, 1.5, 0xFF7A1E, 14);
                SpellProjectile fb = SpellProjectile.create(level, p, SpellProjectile.Kind.FIREBALL, mag * s.scaled(rank), s.radius)
                        .aoeFraction(s.power2 / s.power);
                fb.shootFromRotation(p, p.getXRot(), p.getYRot(), 0, 1.5F, 0.2F);
                level.addFreshEntity(fb);
                sound(p, SoundEvents.BLAZE_SHOOT, 1f, 1f);
            }
            case "nova_de_givre" -> {
                Fx.cast(p, CastAnim.CHANNEL, 10);
                Fx.spawn(level, FxKind.NOVA, pos, s.radius, 0x9AE4FF, 26);
                Fx.spawn(level, FxKind.RING, pos, s.radius * 1.1, 0xCFF4FF, 14);
                for (LivingEntity e : SkillHelper.enemiesInRadius(p, pos.add(0, 1, 0), s.radius)) {
                    RpgCombat.playerHit(p, e, mag * s.scaled(rank), true);
                    e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, s.duration, 2));
                }
                sound(p, SoundEvents.GLASS_BREAK, 1f, 0.6f);
                sound(p, SoundEvents.PLAYER_HURT_FREEZE, 1f, 1f);
            }
            case "eclair_en_chaine" -> {
                LivingEntity first = SkillHelper.aimedEnemy(p, 22);
                if (first == null) {
                    List<LivingEntity> near = SkillHelper.enemiesInCone(p, 16, 40);
                    if (near.isEmpty()) {
                        p.sendOverlayMessage(Component.literal("Aucune cible").withColor(0xFF7070));
                        return false;
                    }
                    first = near.get(0);
                }
                Fx.cast(p, CastAnim.CHANNEL, 10);
                int jumps = 3 + rank / 2;
                double dmg = mag * s.scaled(rank);
                Set<Integer> done = new HashSet<>();
                List<Vec3> points = new ArrayList<>();
                points.add(p.getEyePosition().add(p.getLookAngle().scale(0.8)).add(0, -0.3, 0));
                LivingEntity current = first;
                for (int i = 0; i <= jumps && current != null; i++) {
                    done.add(current.getId());
                    Vec3 to = center(current);
                    points.add(to);
                    RpgCombat.playerHit(p, current, dmg, true);
                    dmg *= 0.85;
                    LivingEntity next = null;
                    double best = s.radius * s.radius;
                    for (LivingEntity e : SkillHelper.enemiesInRadius(p, to, s.radius)) {
                        double ds = e.position().distanceToSqr(to);
                        if (!done.contains(e.getId()) && ds < best) {
                            best = ds;
                            next = e;
                        }
                    }
                    current = next;
                }
                Fx.path(level, FxKind.CHAIN, points, 0.13, 0xA8D8FF, 12);
                for (int i = 1; i < points.size(); i++) Fx.spawn(level, FxKind.BURST, points.get(i), 0.9, 0xC8E8FF, 8);
                sound(p, SoundEvents.TRIDENT_THUNDER.value(), 0.7f, 1.6f);
            }
            case "teleportation_arcanique" -> {
                Vec3 end = SkillHelper.blockLimitedEnd(p, s.radius + rank);
                Vec3 dest = end.subtract(p.getLookAngle().scale(0.8));
                Vec3 from = p.position();
                Fx.cast(p, CastAnim.RAISE, 6);
                Fx.spawn(level, FxKind.BLINK, from, 1.6, 0xC070FF, 10);
                p.teleportTo(dest.x, Math.max(dest.y - 1, p.getY() - 0.5), dest.z);
                p.fallDistance = 0;
                Fx.spawn(level, FxKind.BLINK, p.position(), 1.6, 0xE0A0FF, 10);
                Fx.path(level, FxKind.BEAM, List.of(from.add(0, 1, 0), p.position().add(0, 1, 0)), 0.35, 0xC070FF, 9);
                RpgPlayers.addBuff(p, BuffType.ARCANE_GUARD, s.duration, 0.3, 0);
                sound(p, SoundEvents.ENDERMAN_TELEPORT, 1f, 1.3f);
            }
            case "bouclier_de_mana" -> {
                int dur = s.duration + 40 * (rank - 1);
                RpgPlayers.addBuff(p, BuffType.MANA_SHIELD, dur, s.power, 0);
                Fx.cast(p, CastAnim.LEAP, 12);
                Fx.attach(level, FxKind.DOME, p, 1.4, 0x6AA8FF, dur);
                sound(p, SoundEvents.BEACON_ACTIVATE, 1f, 1.5f);
            }
            case "meteore_celeste" -> {
                Vec3 target = SkillHelper.targetPoint(p, 30);
                Fx.cast(p, CastAnim.RAISE, 16);
                Fx.spawn(level, FxKind.CIRCLE, target, s.radius, 0xFF5020, 36);
                Vec3 start = target.add(-6, 22, -3);
                Vec3 dir = target.subtract(start).normalize();
                SpellProjectile meteor = SpellProjectile.create(level, p, SpellProjectile.Kind.METEOR, mag * s.scaled(rank), s.radius)
                        .aoeFraction(1.0).life(80);
                meteor.setPos(start.x, start.y, start.z);
                meteor.shoot(dir.x, dir.y, dir.z, 1.15F, 0F);
                level.addFreshEntity(meteor);
                sound(p, SoundEvents.FIRECHARGE_USE, 1f, 0.5f);
            }
            // ============================================================ ARCHER
            case "tir_percant" -> {
                Fx.cast(p, CastAnim.AIM, 10);
                SpellProjectile arrow = SpellProjectile.create(level, p, SpellProjectile.Kind.LIGHT_ARROW, atk * s.scaled(rank), 0)
                        .pierce().life((int) Math.ceil(s.radius / 3.5) + 2);
                arrow.shootFromRotation(p, p.getXRot(), p.getYRot(), 0, 3.5F, 0F);
                level.addFreshEntity(arrow);
                sound(p, SoundEvents.CROSSBOW_SHOOT, 1f, 0.8f);
                sound(p, SoundEvents.ARROW_SHOOT, 1f, 1.4f);
            }
            case "pluie_de_fleches" -> {
                Vec3 target = SkillHelper.targetPoint(p, 28);
                int arrows = 10 + 2 * rank;
                double dmg = atk * s.scaled(rank);
                Fx.cast(p, CastAnim.AIM, 12);
                Fx.spawn(level, FxKind.CIRCLE, target, s.radius, 0xB8FF7A, s.duration + 12);
                for (int i = 0; i < arrows; i++) {
                    final int k = i;
                    Scheduler.later(level, 6 + (int) (k * (s.duration / (double) arrows)), () -> {
                        double ox = (p.getRandom().nextDouble() - 0.5) * s.radius * 2;
                        double oz = (p.getRandom().nextDouble() - 0.5) * s.radius * 2;
                        SpellProjectile a = SpellProjectile.create(level, p, SpellProjectile.Kind.RAIN_ARROW, dmg, 0).life(24);
                        a.setPos(target.x + ox, target.y + 14, target.z + oz);
                        a.shoot(0, -1, 0, 1.8F, 0F);
                        level.addFreshEntity(a);
                    });
                }
                sound(p, SoundEvents.ARROW_SHOOT, 1f, 0.6f);
            }
            case "saut_arriere" -> {
                Vec3 back = p.getLookAngle().multiply(1, 0, 1).normalize().scale(-1.3);
                p.setDeltaMovement(back.x, 0.55, back.z);
                p.needsSync = true;
                Fx.attach(level, FxKind.TRAIL, p, 1, 0xD8F0FF, 12);
                Fx.spawn(level, FxKind.RING, pos, s.radius, 0xD8F0FF, 12);
                for (LivingEntity e : SkillHelper.enemiesInRadius(p, pos.add(0, 1, 0), s.radius)) {
                    e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 2));
                }
                RpgPlayers.addBuff(p, BuffType.AGILITY, s.duration, s.power + 0.05 * (rank - 1), 0);
                sound(p, SoundEvents.ENDER_DRAGON_FLAP, 0.6f, 1.6f);
            }
            case "fleche_explosive" -> {
                Fx.cast(p, CastAnim.AIM, 10);
                SpellProjectile arrow = SpellProjectile.create(level, p, SpellProjectile.Kind.EXPLOSIVE_ARROW, atk * s.scaled(rank), s.radius)
                        .aoeFraction(1.0).life(80);
                arrow.shootFromRotation(p, p.getXRot(), p.getYRot(), 0, 2.6F, 0.4F);
                level.addFreshEntity(arrow);
                sound(p, SoundEvents.CROSSBOW_SHOOT, 1f, 1.2f);
            }
            case "volee_de_fleches" -> {
                int count = 6 + rank;
                double dmg = atk * s.scaled(rank);
                Fx.cast(p, CastAnim.AIM, 10);
                for (int i = 0; i < count; i++) {
                    float yaw = p.getYRot() + (i - (count - 1) / 2f) * 7f;
                    SpellProjectile arrow = SpellProjectile.create(level, p, SpellProjectile.Kind.VOLLEY_ARROW, dmg, 0).life(30);
                    arrow.shootFromRotation(p, p.getXRot(), yaw, 0, 2.8F, 1.0F);
                    level.addFreshEntity(arrow);
                }
                sound(p, SoundEvents.ARROW_SHOOT, 1.2f, 0.8f);
            }
            case "tempete_divine" -> {
                double dmg = atk * s.scaled(rank);
                int volleys = s.duration / 5;
                Fx.cast(p, CastAnim.CHANNEL, 20);
                Fx.attach(level, FxKind.STORM, p, s.radius * 0.6, 0xA8D8FF, s.duration + 10);
                for (int v = 0; v < volleys; v++) {
                    Scheduler.later(level, 8 + v * 5, () -> {
                        List<LivingEntity> targets = new ArrayList<>(SkillHelper.enemiesInRadius(p, p.position(), s.radius));
                        if (targets.isEmpty()) return;
                        LivingEntity t = targets.get(p.getRandom().nextInt(targets.size()));
                        SpellProjectile bolt = SpellProjectile.create(level, p, SpellProjectile.Kind.LIGHTNING_ORB, dmg, 0).homing(t);
                        bolt.setPos(p.getX() + (p.getRandom().nextDouble() - 0.5) * 3, p.getY() + 5.2, p.getZ() + (p.getRandom().nextDouble() - 0.5) * 3);
                        bolt.shoot(0, -1, 0, 0.8F, 8F);
                        level.addFreshEntity(bolt);
                        sound(p, SoundEvents.TRIDENT_THUNDER.value(), 0.3f, 1.8f);
                    });
                }
            }
            // ============================================================ TANK
            case "coup_de_bouclier" -> {
                List<LivingEntity> targets = SkillHelper.enemiesInCone(p, s.radius, 45);
                if (targets.isEmpty()) {
                    p.sendOverlayMessage(Component.literal("Aucune cible à portée").withColor(0xFF7070));
                    return false;
                }
                p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
                Fx.cast(p, CastAnim.BASH, 10);
                Fx.facing(level, FxKind.SHIELD, p, 1.0, 0xFFD040, 10);
                for (LivingEntity e : targets) {
                    RpgCombat.playerHit(p, e, atk * s.scaled(rank) + def * s.scaled2(rank), false);
                    e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, s.duration, 9));
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, s.duration, 2));
                    if (e instanceof Mob m) m.getNavigation().stop();
                    Fx.spawn(level, FxKind.BURST, center(e), 0.9, 0xFFE080, 8);
                }
                sound(p, SoundEvents.SHIELD_BLOCK.value(), 1f, 0.7f);
                sound(p, SoundEvents.ANVIL_LAND, 0.5f, 1.4f);
            }
            case "provocation" -> {
                double r = s.radius + 2 * (rank - 1);
                Fx.cast(p, CastAnim.LEAP, 12);
                Fx.spawn(level, FxKind.SHOCKWAVE, pos, r, 0xFF3020, 16);
                for (Mob m : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(r), m -> RpgCombat.isEnemy(p, m))) {
                    com.mmorpg.combat.ThreatGoal.taunt(m, p, s.duration);
                    Fx.attach(level, FxKind.MARK, m, 1, 0xFF3020, Math.min(s.duration, 100));
                }
                RpgPlayers.addBuff(p, BuffType.TAUNT, s.duration, s.power, 0);
                sound(p, SoundEvents.RAVAGER_ROAR, 1f, 0.8f);
            }
            case "forteresse" -> {
                int dur = s.duration + 20 * (rank - 1);
                RpgPlayers.addBuff(p, BuffType.FORTRESS, dur, s.power, 0);
                Fx.cast(p, CastAnim.BASH, 10);
                Fx.attach(level, FxKind.DOME, p, 1.5, 0xFFD040, dur);
                Fx.spawn(level, FxKind.SHOCKWAVE, pos, 2.5, 0xFFD040, 12);
                sound(p, SoundEvents.ANVIL_LAND, 1f, 0.6f);
                sound(p, SoundEvents.ARMOR_EQUIP_NETHERITE.value(), 1f, 0.8f);
            }
            case "onde_de_choc" -> {
                Fx.cast(p, CastAnim.SLAM, 14);
                Scheduler.later(level, 6, () -> {
                    Vec3 c = p.position();
                    p.swing(net.minecraft.world.InteractionHand.MAIN_HAND, net.minecraft.world.item.component.SwingAnimation.DEFAULT, true);
                    Fx.spawn(level, FxKind.SHOCKWAVE, c, s.radius, 0xFFC060, 18);
                    Fx.spawn(level, FxKind.CRACKS, c, s.radius * 0.8, 0xFFA040, 34);
                    level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y + 0.3, c.z, 3, 1.2, 0.1, 1.2, 0);
                    for (LivingEntity e : SkillHelper.enemiesInRadius(p, c.add(0, 1, 0), s.radius)) {
                        RpgCombat.playerHit(p, e, atk * s.scaled(rank) + def * s.scaled2(rank), false);
                        SkillHelper.knock(e, c, 1.1, 0.6);
                    }
                    sound(p, SoundEvents.MACE_SMASH_GROUND_HEAVY, 1f, 0.8f);
                });
            }
            case "aura_sacree" -> {
                double total = s.power + 0.05 * (rank - 1);
                Fx.cast(p, CastAnim.LEAP, 14);
                Fx.spawn(level, FxKind.PILLAR, pos, 1.0, 0xFFF0A0, 22);
                Fx.attach(level, FxKind.HALO, p, 1.6, 0xFFE080, s.duration);
                for (Player ally : level.getEntitiesOfClass(Player.class, p.getBoundingBox().inflate(s.radius))) {
                    if (ally instanceof ServerPlayer sp) {
                        RpgPlayers.healPercent(sp, total * 0.4);
                        RpgPlayers.addBuff(sp, BuffType.SACRED_AURA, s.duration, total * 0.6 / (s.duration / 20.0), 0);
                        if (sp != p) Fx.attach(level, FxKind.HALO, sp, 1.0, 0xFFE080, 40);
                    }
                }
                Fx.spawn(level, FxKind.RING, pos, s.radius, 0xFFE080, 18);
                sound(p, SoundEvents.BEACON_POWER_SELECT, 1f, 1.2f);
            }
            case "rempart_du_titan" -> {
                RpgPlayers.addBuff(p, BuffType.TITAN, s.duration, s.power, s.power2);
                Fx.cast(p, CastAnim.LEAP, 16);
                Fx.attach(level, FxKind.DOME, p, 2.0, 0xFFD040, s.duration);
                Fx.spawn(level, FxKind.PILLAR, pos, 1.6, 0xFFD040, 24);
                Fx.spawn(level, FxKind.SHOCKWAVE, pos, s.radius, 0xFFC040, 18);
                for (Mob m : level.getEntitiesOfClass(Mob.class, p.getBoundingBox().inflate(s.radius), m -> RpgCombat.isEnemy(p, m))) {
                    com.mmorpg.combat.ThreatGoal.taunt(m, p, s.duration);
                    Fx.attach(level, FxKind.MARK, m, 1, 0xFFB020, 80);
                }
                sound(p, SoundEvents.TOTEM_USE, 0.8f, 0.8f);
            }
            default -> {
                return false;
            }
        }
        return true;
    }
}
