package com.mmorpg.entity.mob;

import com.mmorpg.MMORPG;
import com.mmorpg.combat.RpgCombat;
import com.mmorpg.config.ConfigManager;
import com.mmorpg.config.MobConfig;
import com.mmorpg.entity.MagicProjectile;
import com.mmorpg.entity.MobAbilities;
import com.mmorpg.item.ItemDefs.Element;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

/**
 * Sorts et capacites des monstres 3D importes : projectiles elementaires, volees, frappes de zone, ondes de givre ou de feu, nuages de
 * poison, racines, charges, pas dans l'ombre, invocations. Chaque monstre a les siens (table {@link #TABLE}) ; le delai de recharge, les
 * degats, la portee et le rayon se reglent dans config/mmorpg/mobs.json, champ « abilities » du monstre ({@code <sort>Cooldown},
 * {@code <sort>Damage}, {@code <sort>Range}, {@code <sort>Radius}). Un sort se lance avec l'animation d'attaque du monstre, puis son
 * effet tombe quelques ticks plus tard, au moment du geste.
 */
public final class ImportedSpells {
    private ImportedSpells() {
    }

    public enum Type { BOLT, VOLLEY, SLAM, NOVA, CLOUD, ROOTS, CHARGE, BLINK, SUMMON }

    /**
     * Un sort : identifiant (cle de configuration), type, element, recharge (ticks), portee de declenchement, degats (multiple de l'attaque
     * du monstre), rayon d'effet, nombre de projectiles ou de serviteurs, et si le monstre le lance a distance (il s'arrete alors pour tirer).
     */
    public record Spell(String id, Type type, Element element, int cooldown, double range, double damage, double radius, int count, boolean ranged, String minion) {
    }

    private static Spell bolt(String id, Element e, int cd, double range, double dmg, double aoe) {
        return new Spell(id, Type.BOLT, e, cd, range, dmg, aoe, 1, true, "");
    }

    private static Spell volley(String id, Element e, int cd, double range, double dmg, int n) {
        return new Spell(id, Type.VOLLEY, e, cd, range, dmg, 0, n, true, "");
    }

    private static Spell slam(String id, Element e, int cd, double radius, double dmg) {
        return new Spell(id, Type.SLAM, e, cd, radius, dmg, radius, 1, false, "");
    }

    private static Spell nova(String id, Element e, int cd, double radius, double dmg) {
        return new Spell(id, Type.NOVA, e, cd, radius, dmg, radius, 1, false, "");
    }

    private static Spell cloud(String id, Element e, int cd, double radius, double dmg) {
        return new Spell(id, Type.CLOUD, e, cd, radius, dmg, radius, 1, false, "");
    }

    private static Spell roots(String id, Element e, int cd, double radius, double dmg) {
        return new Spell(id, Type.ROOTS, e, cd, radius, dmg, radius, 1, false, "");
    }

    private static Spell charge(String id, int cd, double range, double dmg) {
        return new Spell(id, Type.CHARGE, Element.ARCANE, cd, range, dmg, 2.6, 1, false, "");
    }

    private static Spell blink(String id, Element e, int cd, double dmg) {
        return new Spell(id, Type.BLINK, e, cd, 14, dmg, 3, 1, false, "");
    }

    private static Spell summon(String id, int cd, String minion, int n) {
        return new Spell(id, Type.SUMMON, Element.OMBRE, cd, 16, 0, 0, n, false, minion);
    }

    public static final Map<String, List<Spell>> TABLE = Map.ofEntries(
            Map.entry("mousseux", List.of(charge("charge", 130, 9, 1.1))),
            Map.entry("sanglier_ronce", List.of(charge("charge", 110, 11, 1.4))),
            Map.entry("archer_sylvestre", List.of(bolt("fleche", Element.ECLAIR, 45, 17, 0.9, 0))),
            Map.entry("veilleur_champignon", List.of(cloud("spores", Element.OMBRE, 130, 4.5, 0.4))),
            Map.entry("colosse_racine", List.of(roots("racines", Element.OMBRE, 170, 6, 0.9), slam("ecrasement", Element.ARCANE, 230, 5, 1.6))),
            Map.entry("crapaud_venimeux", List.of(bolt("crachat_venin", Element.OMBRE, 70, 11, 0.8, 0))),
            Map.entry("limace_acide", List.of(bolt("jet_acide", Element.OMBRE, 80, 10, 0.9, 1.8))),
            Map.entry("luciole_geante", List.of(nova("eclat_aveuglant", Element.ECLAIR, 150, 4, 0.5))),
            Map.entry("sorcier_roseaux", List.of(bolt("eau_noire", Element.OMBRE, 55, 16, 1.0, 0), roots("lianes", Element.OMBRE, 210, 5, 0.7))),
            Map.entry("hydre_marais", List.of(volley("triple_crachat", Element.OMBRE, 120, 13, 0.7, 3), cloud("brume_putride", Element.OMBRE, 260, 5, 0.5))),
            Map.entry("osselet", List.of(bolt("os_lance", Element.ARCANE, 70, 12, 0.7, 0))),
            Map.entry("araignee_os", List.of(bolt("toile_gluante", Element.OMBRE, 100, 10, 0.5, 0))),
            Map.entry("garde_funeraire", List.of(slam("frappe_funeraire", Element.OMBRE, 150, 4.5, 1.4))),
            Map.entry("spectre_enchaine", List.of(bolt("chaine_spectrale", Element.OMBRE, 70, 13, 0.9, 0), blink("glissade", Element.OMBRE, 190, 1.1))),
            Map.entry("roi_cryptes", List.of(summon("sujets", 360, "osselet", 2), slam("sceptre", Element.OMBRE, 170, 5, 1.7))),
            Map.entry("renard_givre", List.of(bolt("souffle_givre", Element.GIVRE, 80, 9, 0.7, 1.2))),
            Map.entry("scarabee_polaire", List.of(nova("carapace_glacee", Element.GIVRE, 170, 3.8, 0.6))),
            Map.entry("harpie_neiges", List.of(volley("plumes_de_givre", Element.GIVRE, 90, 13, 0.6, 3))),
            Map.entry("golem_iceberg", List.of(nova("onde_glaciale", Element.GIVRE, 150, 6.5, 1.3), slam("poings_de_glace", Element.GIVRE, 240, 4.5, 1.6))),
            Map.entry("wyrm_boreal", List.of(bolt("souffle_polaire", Element.GIVRE, 90, 13, 1.3, 2.6))),
            Map.entry("scorpion_cuivre", List.of(bolt("dard_venimeux", Element.OMBRE, 80, 10, 1.0, 0))),
            Map.entry("momie_dunes", List.of(cloud("malediction", Element.OMBRE, 150, 4, 0.4))),
            Map.entry("hyene_cendree", List.of(charge("bond", 100, 10, 1.3))),
            Map.entry("djinn_braises", List.of(bolt("boule_de_feu", Element.FEU, 55, 17, 1.2, 2.0), nova("anneau_de_braise", Element.FEU, 200, 5, 1.0))),
            Map.entry("tortue_volcanique", List.of(slam("eruption", Element.FEU, 170, 4.5, 1.3))),
            Map.entry("mite_astrale", List.of(bolt("eclat_astral", Element.NEANT, 50, 15, 0.8, 0))),
            Map.entry("rodeur_vide", List.of(blink("pas_du_vide", Element.NEANT, 130, 1.3))),
            Map.entry("oeil_chaos", List.of(bolt("regard_du_chaos", Element.NEANT, 45, 19, 1.0, 0), nova("pulsation", Element.NEANT, 190, 5, 0.9))),
            Map.entry("chevalier_fracture", List.of(blink("fente", Element.NEANT, 140, 1.5), slam("tranchant_du_neant", Element.NEANT, 200, 4, 1.4))),
            Map.entry("devoreur_etoiles", List.of(bolt("souffle_du_neant", Element.NEANT, 80, 15, 1.6, 3.0), slam("ecrasement_astral", Element.NEANT, 210, 6, 1.7),
                    summon("rejetons", 420, "mite_astrale", 2))));

    /** Nombre de sorts lances par monstre depuis le demarrage (verifie par les tests de la vitrine). */
    public static final Map<String, Integer> CAST_LOG = new java.util.concurrent.ConcurrentHashMap<>();

    public static List<Spell> of(String key) {
        return TABLE.getOrDefault(key, List.of());
    }

    /** Parametres du sort tels que regles dans mobs.json (valeurs par defaut de la table si la cle manque). */
    public static double param(String mobKey, Spell s, String what, double def) {
        MobConfig c = ConfigManager.mob(mobKey);
        return c == null ? def : c.ability(s.id() + what, def);
    }

    /** Ajoute au monstre ses parametres de sorts par defaut (config par defaut et fusion des anciens fichiers). */
    public static void seed(String mobKey, MobConfig c) {
        for (Spell s : of(mobKey)) {
            c.abilities.putIfAbsent(s.id() + "Cooldown", (double) s.cooldown());
            if (s.damage() > 0) c.abilities.putIfAbsent(s.id() + "Damage", s.damage());
            if (s.type() == Type.BOLT || s.type() == Type.VOLLEY || s.type() == Type.CHARGE) c.abilities.putIfAbsent(s.id() + "Range", s.range());
            if (s.radius() > 0 && s.type() != Type.BOLT) c.abilities.putIfAbsent(s.id() + "Radius", s.radius());
        }
    }

    // ------------------------------------------------------------------ effets

    static ParticleOptions particle(Element e) {
        return switch (e) {
            case FEU -> ParticleTypes.FLAME;
            case GIVRE -> ParticleTypes.SNOWFLAKE;
            case NEANT -> ParticleTypes.PORTAL;
            case OMBRE -> ParticleTypes.WITCH;
            case ECLAIR -> ParticleTypes.ELECTRIC_SPARK;
            default -> ParticleTypes.END_ROD;
        };
    }

    /** Lance l'effet du sort contre la cible (appele quelques ticks apres le debut du geste). */
    public static void cast(Mob mob, Spell s, LivingEntity target) {
        if (!(mob.level() instanceof ServerLevel level)) return;
        String key = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).getPath();
        CAST_LOG.merge(key + "/" + s.id(), 1, Integer::sum);
        double dmg = param(key, s, "Damage", s.damage());
        double radius = param(key, s, "Radius", s.radius());
        switch (s.type()) {
            case BOLT -> {
                MobAbilities.bolt(mob, target, s.element(), dmg, s.element() == Element.ECLAIR ? 1.9F : 1.3F, radius);
                level.playSound(null, mob.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 0.8f, 1.2f);
            }
            case VOLLEY -> {
                for (int i = 0; i < s.count(); i++) {
                    MagicProjectile p = MagicProjectile.create(level, mob, s.element(), RpgCombat.mobAtk(mob) * dmg, radius);
                    double spread = (i - (s.count() - 1) / 2.0) * 0.28;
                    Vec3 dir = new Vec3(target.getX() - mob.getX(), target.getY(0.5) - p.getY(), target.getZ() - mob.getZ());
                    double cos = Math.cos(spread), sin = Math.sin(spread);
                    p.shoot(dir.x * cos - dir.z * sin, dir.y, dir.x * sin + dir.z * cos, 1.4F, 1.5F);
                    level.addFreshEntity(p);
                }
                level.playSound(null, mob.blockPosition(), SoundEvents.BLAZE_SHOOT, SoundSource.HOSTILE, 0.8f, 1.5f);
            }
            case SLAM -> MobAbilities.slam(mob, radius, dmg, 0.6, particle(s.element()), SoundEvents.GENERIC_EXPLODE.value());
            case NOVA -> {
                burst(level, mob, radius, s.element());
                for (LivingEntity e : MobAbilities.enemiesAround(mob, mob.position(), radius)) {
                    RpgCombat.mobHit(mob, e, dmg);
                    effect(e, s.element(), 60);
                }
                level.playSound(null, mob.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 1.2f, 0.9f);
            }
            case CLOUD -> {
                burst(level, mob, radius, s.element());
                for (LivingEntity e : MobAbilities.enemiesAround(mob, mob.position(), radius)) {
                    RpgCombat.mobHit(mob, e, dmg);
                    e.addEffect(new MobEffectInstance(MobEffects.POISON, 80, 0));
                    e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0));
                }
                level.playSound(null, mob.blockPosition(), SoundEvents.SPLASH_POTION_BREAK, SoundSource.HOSTILE, 1f, 0.6f);
            }
            case ROOTS -> {
                for (LivingEntity e : MobAbilities.enemiesAround(mob, mob.position(), radius)) {
                    RpgCombat.mobHit(mob, e, dmg);
                    e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 5));
                    for (int i = 0; i < 12; i++) level.sendParticles(ParticleTypes.COMPOSTER, e.getX() + (mob.getRandom().nextDouble() - 0.5) * 1.2, e.getY() + i * 0.12, e.getZ() + (mob.getRandom().nextDouble() - 0.5) * 1.2, 1, 0.1, 0.05, 0.1, 0.01);
                }
                level.playSound(null, mob.blockPosition(), SoundEvents.ROOTED_DIRT_BREAK, SoundSource.HOSTILE, 1.2f, 0.7f);
            }
            case CHARGE -> {
                Vec3 dir = target.position().subtract(mob.position()).multiply(1, 0, 1).normalize();
                mob.setDeltaMovement(dir.x * 1.35, 0.28, dir.z * 1.35);
                mob.needsSync = true;
                level.sendParticles(ParticleTypes.CLOUD, mob.getX(), mob.getY() + 0.2, mob.getZ(), 12, 0.4, 0.1, 0.4, 0.02);
                level.playSound(null, mob.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 0.6f, 1.4f);
            }
            case BLINK -> {
                MobAbilities.blinkBehind(mob, target);
                RpgCombat.mobHit(mob, target, dmg);
                level.sendParticles(particle(s.element()), target.getX(), target.getY(0.5), target.getZ(), 14, 0.4, 0.5, 0.4, 0.05);
            }
            case SUMMON -> {
                BuiltInRegistries.ENTITY_TYPE.getOptional(MMORPG.id(s.minion())).ifPresent(t -> {
                    @SuppressWarnings("unchecked")
                    var typed = (net.minecraft.world.entity.EntityType<? extends Mob>) t;
                    // comme le Roi gobelin : pas plus de 8 serviteurs autour, sinon un combat qui s'eternise remplit la zone
                    if (level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(24), m -> m.getType() == typed && m.isAlive()).size() < 8) MobAbilities.summon(mob, typed, s.count(), s.minion(), -3);
                });
                level.playSound(null, mob.blockPosition(), SoundEvents.EVOKER_PREPARE_SUMMON, SoundSource.HOSTILE, 1f, 0.9f);
            }
        }
    }

    private static void burst(ServerLevel level, Mob mob, double radius, Element e) {
        for (int i = 0; i < 40; i++) {
            double a = Math.PI * 2 * i / 40;
            for (double r = 1; r <= radius; r += 1.2) {
                level.sendParticles(particle(e), mob.getX() + Math.cos(a) * r, mob.getY() + 0.3, mob.getZ() + Math.sin(a) * r, 1, 0.05, 0.1, 0.05, 0.01);
            }
        }
    }

    private static void effect(LivingEntity target, Element e, int ticks) {
        switch (e) {
            case GIVRE -> target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, ticks, 2));
            case ECLAIR -> target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, ticks, 0));
            case NEANT -> target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, ticks, 1));
            case FEU -> target.igniteForSeconds(3);
            default -> { }
        }
    }
}
