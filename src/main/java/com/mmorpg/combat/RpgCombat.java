package com.mmorpg.combat;

import com.mmorpg.MMORPG;
import com.mmorpg.config.ConfigManager;
import com.mmorpg.entity.PetEntity;
import com.mmorpg.network.Payloads;
import com.mmorpg.registry.ModAttachments;
import com.mmorpg.rpg.Buff;
import com.mmorpg.rpg.BuffType;
import com.mmorpg.rpg.MobData;
import com.mmorpg.rpg.PlayerClass;
import com.mmorpg.rpg.PlayerData;
import com.mmorpg.rpg.Stat;
import com.mmorpg.rpg.StatBlock;
import com.mmorpg.server.CombatTexts;
import com.mmorpg.server.RpgPlayers;
import com.mmorpg.skill.Skills;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

/**
 * Systeme de combat RPG.
 *
 * <p>Les points de vie RPG sont "virtuels" : ils sont stockes dans les donnees du joueur ou du monstre et la sante vanilla
 * n'en est que le reflet proportionnel. Cela permet des valeurs de plusieurs milliers de PV (la sante vanilla est
 * plafonnee a 1024) et un controle total des formules de degats :</p>
 * <pre>
 * degats = base (Attaque / Puissance magique x coefficient) x critique
 *          x (1 - reduction) ; reduction = DEF / (DEF + 200 + 10 x niveau de l'attaquant), plafonnee a 80 %
 * </pre>
 */
@EventBusSubscriber(modid = MMORPG.MODID)
public final class RpgCombat {
    public static final ResourceKey<DamageType> SKILL_DAMAGE = ResourceKey.create(Registries.DAMAGE_TYPE, MMORPG.id("skill"));
    private static final ThreadLocal<Hit> OVERRIDE = new ThreadLocal<>();
    public static final String ARROW_DAMAGE = "mmorpg_dmg";
    public static final String ARROW_MULT = "mmorpg_mult";
    public static final String ARROW_EXPLODE = "mmorpg_explode";

    private record Hit(float amount, boolean crit, boolean magic) {
    }

    private RpgCombat() {
    }

    // ================================================================== API publique

    public static DamageSource skillSource(ServerLevel level, Entity attacker) {
        Holder<DamageType> type = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(SKILL_DAMAGE);
        return new DamageSource(type, attacker, attacker);
    }

    /** Inflige un montant de degats RPG precalcule (avant defense) a une cible. */
    public static boolean dealDamage(Entity attacker, LivingEntity target, double amount, boolean crit, boolean magic) {
        if (!(target.level() instanceof ServerLevel level) || !target.isAlive()) return false;
        Hit previous = OVERRIDE.get();
        OVERRIDE.set(new Hit((float) amount, crit, magic));
        try {
            return target.hurtServer(level, skillSource(level, attacker), 1.0F);
        } finally {
            if (previous == null) OVERRIDE.remove();
            else OVERRIDE.set(previous);
        }
    }

    /** Coup de competence d'un joueur : applique le critique et inflige les degats. */
    public static boolean playerHit(ServerPlayer player, LivingEntity target, double base, boolean magic) {
        StatBlock s = RpgPlayers.get(player).stats;
        boolean crit = player.getRandom().nextDouble() * 100 < s.get(Stat.CRIT);
        double amount = crit ? base * s.get(Stat.CRIT_DMG) / 100.0 : base;
        return dealDamage(player, target, amount, crit, magic);
    }

    /** Coup de capacite speciale d'un monstre : multiplicateur de son Attaque RPG. */
    public static boolean mobHit(LivingEntity mob, LivingEntity target, double multiplier) {
        return dealDamage(mob, target, mobAtk(mob) * multiplier, false, true);
    }

    public static double mobAtk(LivingEntity mob) {
        MobData md = mob.getExistingDataOrNull(ModAttachments.MOB);
        return md != null && md.initialized() ? md.atk : 10;
    }

    public static void tagArrowMultiplier(AbstractArrow arrow, double multiplier) {
        arrow.getPersistentData().putDouble(ARROW_MULT, multiplier);
    }

    public static void tagArrowDamage(AbstractArrow arrow, double damage) {
        arrow.getPersistentData().putFloat(ARROW_DAMAGE, (float) damage);
    }

    /** Une competence peut-elle toucher cette cible ? (evite de blesser animaux, villageois, familiers et allies) */
    public static boolean isEnemy(Entity attacker, Entity target) {
        if (!(target instanceof LivingEntity living) || !living.isAlive() || target == attacker) return false;
        if (target instanceof PetEntity) return false;
        if (target instanceof Player p) {
            if (p.isCreative() || p.isSpectator()) return false;
            if (attacker instanceof Player ap) {
                if (com.mmorpg.server.PartyManager.sameParty(ap, p)) return false;
                return ConfigManager.general().skillsHitPlayers && p.level() instanceof net.minecraft.server.level.ServerLevel psl && psl.isPvpAllowed();
            }
            return true;
        }
        if (target instanceof OwnableEntity own && own.getOwner() != null) {
            return !(attacker instanceof Player) || own.getOwner() != attacker && ConfigManager.general().skillsHitPlayers;
        }
        if (attacker instanceof Player) {
            if (target instanceof Enemy) return true;
            MobData md = target.getExistingDataOrNull(ModAttachments.MOB);
            if (md != null && md.initialized()) return true;
            return target instanceof Mob m && m.getTarget() == attacker;
        }
        // monstres : visent les joueurs et leurs allies
        return target instanceof Player || target instanceof OwnableEntity;
    }

    public static int levelOf(Entity e) {
        if (e instanceof Player p) return RpgPlayers.get(p).level;
        if (e != null) {
            MobData md = e.getExistingDataOrNull(ModAttachments.MOB);
            if (md != null && md.initialized()) return md.level;
        }
        return 1;
    }

    // ================================================================== sante virtuelle

    /** Vue unifiee sur la sante RPG d'une entite (joueur ou monstre). */
    private static final class Health {
        final LivingEntity entity;
        final PlayerData player;
        final MobData mob;

        Health(LivingEntity entity, PlayerData player, MobData mob) {
            this.entity = entity;
            this.player = player;
            this.mob = mob;
        }

        static Health of(LivingEntity e) {
            if (e instanceof ServerPlayer sp) {
                PlayerData d = RpgPlayers.get(sp);
                if (d.stats.get(Stat.MAX_HP) <= 0) RpgPlayers.recompute(sp);
                return new Health(e, d, null);
            }
            MobData md = e.getExistingDataOrNull(ModAttachments.MOB);
            if (md != null && md.initialized()) return new Health(e, null, md);
            return null;
        }

        double hp() {
            return player != null ? player.hp : mob.hp;
        }

        double max() {
            return player != null ? player.stats.get(Stat.MAX_HP) : mob.maxHp;
        }

        double def() {
            return player != null ? player.stats.get(Stat.DEF) : mob.def;
        }

        int level() {
            return player != null ? player.level : mob.level;
        }

        void setHp(double v) {
            if (player != null) {
                player.hp = (float) v;
            } else {
                mob.hp = (float) v;
                entity.syncData(ModAttachments.MOB);
            }
        }
    }

    // ================================================================== evenements

    private static boolean bypass(DamageSource source) {
        return source.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onIncoming(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) return;
        DamageSource source = event.getSource();
        if (bypass(source)) return;
        Entity attacker = source.getEntity();
        if (attacker instanceof PetEntity) {
            event.setCanceled(true);
            return;
        }
        if (target instanceof ServerPlayer tp && attacker instanceof ServerPlayer ap && tp != ap
                && com.mmorpg.server.PartyManager.sameParty(tp, ap)) {
            event.setCanceled(true);
            return;
        }
        // pas de tir allie entre un joueur et son familier, ni de degats de competences sur les non-ennemis
        if (OVERRIDE.get() != null && attacker != null && !isEnemy(attacker, target)) {
            event.setCanceled(true);
            return;
        }
        // esquive
        if (target instanceof ServerPlayer sp && attacker != null && attacker != sp) {
            PlayerData d = RpgPlayers.get(sp);
            if (d.playerClass.isPlayable() && sp.getRandom().nextDouble() * 100 < d.stats.get(Stat.ESQ)) {
                event.setCanceled(true);
                CombatTexts.above(sp, 0, Payloads.CombatText.DODGE);
                return;
            }
            Buff titan = d.buffs.get(BuffType.TITAN);
            if (titan != null && attacker instanceof LivingEntity le && attacker != sp && OVERRIDE.get() == null) {
                // renvoi des degats (calcule a partir de l'attaque du monstre)
                MobData md = le.getExistingDataOrNull(ModAttachments.MOB);
                double reflected = (md != null ? md.atk : event.getAmount()) * titan.magnitude2;
                dealDamage(sp, le, reflected, false, false);
            }
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onDamagePre(LivingDamageEvent.Pre event) {
        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel)) return;
        DamageSource source = event.getSource();
        if (bypass(source)) return;
        Health h = Health.of(target);
        if (h == null) return;

        Hit hit = OVERRIDE.get();
        Entity attacker = source.getEntity();
        Entity direct = source.getDirectEntity();
        float raw = event.getContainer().getOriginalDamage();
        boolean crit = false;
        boolean environmental = false;
        int attackerLevel = h.level();
        double damage;
        ServerPlayer playerAttacker = attacker instanceof ServerPlayer sp && RpgPlayers.get(sp).playerClass.isPlayable() ? sp : null;

        if (hit != null) {
            damage = hit.amount();
            crit = hit.crit();
            attackerLevel = levelOf(attacker);
        } else if (playerAttacker != null) {
            PlayerData pd = RpgPlayers.get(playerAttacker);
            StatBlock st = pd.stats;
            attackerLevel = pd.level;
            double atk = st.get(Stat.ATK);
            if (direct instanceof AbstractArrow arrow) {
                CompoundTag tag = arrow.getPersistentData();
                if (tag.contains(ARROW_DAMAGE)) {
                    damage = tag.getFloatOr(ARROW_DAMAGE, 0);
                } else {
                    damage = atk * tag.getDoubleOr(ARROW_MULT, Mth.clamp(raw / 6.0, 0.2, 1.3));
                }
            } else if (direct instanceof Projectile && direct != attacker) {
                damage = atk * Mth.clamp(raw / 6.0, 0.3, 1.5);
            } else {
                double attr = Math.max(1.0, playerAttacker.getAttributeValue(Attributes.ATTACK_DAMAGE));
                double ratio = Mth.clamp(raw / attr, 0.1, 2.0);
                damage = atk * ratio;
                if (pd.playerClass == PlayerClass.MAGE) damage *= 0.7;
            }
            if (playerAttacker.getRandom().nextDouble() * 100 < st.get(Stat.CRIT)) {
                damage *= st.get(Stat.CRIT_DMG) / 100.0;
                crit = true;
            }
        } else if (attacker instanceof LivingEntity le && le.getExistingDataOrNull(ModAttachments.MOB) instanceof MobData md && md.initialized()) {
            double attr = le.getAttributes().hasAttribute(Attributes.ATTACK_DAMAGE) ? Math.max(1.0, le.getAttributeValue(Attributes.ATTACK_DAMAGE)) : 3.0;
            double ratio = Mth.clamp(raw / attr, 0.3, 3.0);
            damage = md.atk * ratio;
            attackerLevel = md.level;
        } else {
            // degats environnementaux ou d'une source non RPG : proportionnels a la sante vanilla
            environmental = true;
            damage = raw / Math.max(1.0F, target.getMaxHealth()) * h.max();
            if (source.is(DamageTypeTags.IS_FALL)) damage *= 0.8;
            // les monstres RPG (et surtout les boss) resistent aux degats d'environnement (feu, chute, poison...)
            if (h.mob != null) damage *= h.mob.boss ? 0.02 : 0.35;
        }

        if (!environmental) {
            double def = h.def();
            double reduction = Math.min(0.8, def / (def + 200.0 + 10.0 * attackerLevel));
            damage *= 1.0 - reduction;
        }
        if (target instanceof ServerPlayer tp) {
            damage = incomingModifiers(tp, damage);
        }
        damage = Math.max(0, damage);

        // vol de vie
        if (playerAttacker != null && damage > 0) {
            double ls = RpgPlayers.get(playerAttacker).stats.get(Stat.LIFESTEAL);
            if (ls > 0) RpgPlayers.heal(playerAttacker, damage * ls / 100.0, false);
        }

        double newHp = h.hp() - damage;
        if (newHp <= 0.01) {
            h.setHp(0);
            event.setNewDamage(target.getHealth() + 1000F);
        } else {
            h.setHp(newHp);
            float desired = (float) Math.max(0.5, target.getMaxHealth() * newHp / h.max());
            event.setNewDamage(Math.max(0F, target.getHealth() - desired));
        }

        // textes de combat
        if (damage > 0) {
            if (target instanceof ServerPlayer tp) {
                CombatTexts.above(tp, (float) damage, Payloads.CombatText.TAKEN);
                RpgPlayers.sendVitals(tp, true);
            } else if (playerAttacker != null || attacker instanceof Player) {
                CombatTexts.above(target, (float) damage, crit ? Payloads.CombatText.CRIT : Payloads.CombatText.DAMAGE);
            }
        }
    }

    /** Modificateurs de degats subis par un joueur : effets actifs et passifs. */
    private static double incomingModifiers(ServerPlayer player, double damage) {
        PlayerData d = RpgPlayers.get(player);
        double mult = 1.0;
        if (d.buffs.containsKey(BuffType.RAGE)) mult *= 1.10;
        if (d.buffs.containsKey(BuffType.ARCANE_GUARD)) mult *= 0.70;
        Buff fortress = d.buffs.get(BuffType.FORTRESS);
        if (fortress != null) mult *= 1.0 - fortress.magnitude;
        Buff titan = d.buffs.get(BuffType.TITAN);
        if (titan != null) mult *= 1.0 - titan.magnitude;
        int ironSkin = d.playerClass == PlayerClass.TANK ? d.skillRank(Skills.PEAU_DE_FER.id) : 0;
        if (ironSkin > 0) mult *= 1.0 - 0.02 * ironSkin;
        damage *= mult;
        Buff shield = d.buffs.get(BuffType.MANA_SHIELD);
        if (shield != null && damage > 0 && d.mana > 0) {
            double ratio = shield.magnitude; // degats absorbes par point de mana
            double absorbable = d.mana * ratio;
            double absorbed = Math.min(damage, absorbable);
            d.mana -= (float) (absorbed / ratio);
            damage -= absorbed;
            if (absorbed >= 1) CombatTexts.above(player, (float) absorbed, Payloads.CombatText.MANA);
        }
        return damage;
    }

    @SubscribeEvent
    public static void onHeal(LivingHealEvent event) {
        LivingEntity e = event.getEntity();
        if (!(e.level() instanceof ServerLevel)) return;
        Health h = Health.of(e);
        if (h == null) return;
        double rpg = event.getAmount() / Math.max(1.0F, e.getMaxHealth()) * h.max();
        event.setCanceled(true);
        double newHp = Math.min(h.max(), h.hp() + rpg);
        h.setHp(newHp);
        e.setHealth((float) Math.max(0.5, e.getMaxHealth() * newHp / h.max()));
    }
}
