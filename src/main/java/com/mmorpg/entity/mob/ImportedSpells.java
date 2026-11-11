package com.mmorpg.entity.mob;

import com.mmorpg.MMORPG;
import com.mmorpg.combat.RpgCombat;
import com.mmorpg.config.ConfigManager;
import com.mmorpg.config.MobConfig;
import com.mmorpg.entity.MagicProjectile;
import com.mmorpg.entity.FxKind;
import com.mmorpg.entity.MobAbilities;
import com.mmorpg.entity.SkillFxEntity;
import com.mmorpg.server.Scheduler;
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
 * Attaques et sorts de tous les monstres (3D importes et bestiaire classique), lances par {@link SmartMonster} : projectiles elementaires, volees, frappes de zone, ondes de givre ou de feu, nuages de
 * poison, racines, charges, pas dans l'ombre, invocations. Chaque monstre a les siens (table {@link #TABLE}) ; le delai de recharge, les
 * degats, la portee et le rayon se reglent dans config/mmorpg/mobs.json, champ « abilities » du monstre ({@code <sort>Cooldown},
 * {@code <sort>Damage}, {@code <sort>Range}, {@code <sort>Radius}). Un sort se lance avec l'animation d'attaque du monstre, puis son
 * effet tombe quelques ticks plus tard, au moment du geste.
 */
public final class ImportedSpells {
    private ImportedSpells() {
    }

    public enum Type { BOLT, VOLLEY, SLAM, NOVA, CLOUD, ROOTS, CHARGE, BLINK, SUMMON, COMBO, CLEAVE, POUNCE, RETREAT, ROAR }

    /** Clips d'animation des mobs (meme ordre cote client) et delai, en ticks, entre le debut du clip et l'impact. */
    public static final String[] CLIPS = {"attack", "attack2", "special", "cast", "ultime"};
    public static final int[] CLIP_WINDUP = {7, 5, 24, 14, 40};
    public static final int ATTACK = 0, ATTACK2 = 1, SPECIAL = 2, CAST = 3, ULTIME = 4;

    /**
     * Une attaque : identifiant (cle de configuration), type, element, recharge (ticks), portee de declenchement, degats (multiple de l'attaque
     * du monstre), rayon d'effet, nombre de projectiles ou de serviteurs, si le monstre la lance a distance, serviteur invoque et clip joue.
     */
    public record Spell(String id, Type type, Element element, int cooldown, double range, double damage, double radius, int count, boolean ranged, String minion, int clip) {
        public Spell(String id, Type type, Element element, int cooldown, double range, double damage, double radius, int count, boolean ranged, String minion) {
            this(id, type, element, cooldown, range, damage, radius, count, ranged, minion, switch (type) {
                case BOLT, VOLLEY, CLOUD, ROOTS, SUMMON, ROAR -> CAST;
                case SLAM, NOVA -> SPECIAL;
                case COMBO, CLEAVE -> ATTACK2;
                default -> ATTACK;
            });
        }

        /** La meme attaque, jouee avec le clip « ultime » (longue anticipation : attaque signature des plus gros monstres). */
        public Spell ultime() {
            return new Spell(id, type, element, cooldown, range, damage, radius, count, ranged, minion, ULTIME);
        }

        public int windup() {
            return CLIP_WINDUP[clip];
        }
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

    /** Deux coups rapides au corps a corps. */
    private static Spell combo(String id, int cd, double dmg) {
        return new Spell(id, Type.COMBO, Element.ARCANE, cd, 3.2, dmg, 0, 2, false, "");
    }

    /** Coup large devant le monstre : touche tout ce qui se trouve dans l'arc. */
    private static Spell cleave(String id, int cd, double radius, double dmg) {
        return new Spell(id, Type.CLEAVE, Element.ARCANE, cd, radius, dmg, radius, 1, false, "");
    }

    /** Bond sur la cible, degats a l'atterrissage. */
    private static Spell pounce(String id, int cd, double range, double dmg) {
        return new Spell(id, Type.POUNCE, Element.ARCANE, cd, range, dmg, 2.4, 1, false, "");
    }

    /** Saut en arriere quand la cible est trop pres (tireurs et lanceurs de sorts). */
    private static Spell retreat(String id, int cd) {
        return new Spell(id, Type.RETREAT, Element.ARCANE, cd, 4.5, 0, 0, 1, false, "");
    }

    /** Cri : vitesse et force pour le monstre et ses allies proches. */
    private static Spell roar(String id, int cd) {
        return new Spell(id, Type.ROAR, Element.ARCANE, cd, 12, 0, 10, 1, false, "");
    }

    private static final Element F = Element.FEU, G = Element.GIVRE, N = Element.NEANT, O = Element.OMBRE, E = Element.ECLAIR, A = Element.ARCANE;

    /** Tous les monstres (3D importes et bestiaire classique) : au moins trois attaques chacun, chacune avec son clip. */
    public static final Map<String, List<Spell>> TABLE = Map.ofEntries(
            // ---- foret ancienne
            Map.entry("mousseux", List.of(charge("charge", 130, 9, 1.1), combo("double_coup", 90, 0.7), cloud("spores_mousse", O, 170, 3.5, 0.3))),
            Map.entry("sanglier_ronce", List.of(charge("charge", 110, 11, 1.4), cleave("coup_de_defenses", 80, 3.2, 1.2), roar("grognement", 260))),
            Map.entry("archer_sylvestre", List.of(bolt("fleche", E, 45, 17, 0.9, 0), volley("triple_fleche", E, 140, 16, 0.6, 3), retreat("bond_arriere", 110))),
            Map.entry("veilleur_champignon", List.of(cloud("spores", O, 130, 4.5, 0.4), roots("mycelium", O, 200, 5, 0.5), bolt("spore_lancee", O, 70, 12, 0.7, 1.5))),
            Map.entry("colosse_racine", List.of(roots("racines", O, 170, 6, 0.9), slam("ecrasement", A, 230, 5, 1.6).ultime(), cleave("revers_de_branche", 100, 4.5, 1.3),
                    summon("pousses", 420, "fernling", 2))),
            Map.entry("fernling", List.of(combo("fouets", 80, 0.6), roots("ronces", O, 180, 4, 0.5), bolt("graine", A, 80, 10, 0.7, 0))),
            Map.entry("champion_gobelin", List.of(cleave("moulinet", 80, 3.5, 1.2), charge("ruee", 130, 10, 1.3), roar("cri_de_guerre", 260), slam("coup_de_couperet", A, 170, 4, 1.5))),
            // ---- marais
            Map.entry("crapaud_venimeux", List.of(bolt("crachat_venin", O, 70, 11, 0.8, 0), pounce("bond_gluant", 110, 9, 1.1), cloud("bave_toxique", O, 180, 3.5, 0.35))),
            Map.entry("limace_acide", List.of(bolt("jet_acide", O, 80, 10, 0.9, 1.8), cloud("flaque_acide", O, 170, 3.5, 0.4), nova("eclaboussure", O, 200, 3.5, 0.7))),
            Map.entry("luciole_geante", List.of(nova("eclat_aveuglant", E, 150, 4, 0.5), bolt("etincelle", E, 60, 12, 0.7, 0), charge("pique", 120, 10, 1.0))),
            Map.entry("sorcier_roseaux", List.of(bolt("eau_noire", O, 55, 16, 1.0, 0), roots("lianes", O, 210, 5, 0.7), volley("pluie_noire", O, 150, 15, 0.6, 3), retreat("pas_de_brume", 160))),
            Map.entry("hydre_marais", List.of(volley("triple_crachat", O, 120, 13, 0.7, 3), cloud("brume_putride", O, 260, 5, 0.5), cleave("morsures", 90, 4.5, 1.2),
                    slam("coup_de_queue", A, 200, 5, 1.4).ultime())),
            // ---- cryptes
            Map.entry("osselet", List.of(bolt("os_lance", A, 70, 12, 0.7, 0), combo("griffes_os", 80, 0.6), pounce("sursaut", 130, 8, 1.0))),
            Map.entry("araignee_os", List.of(bolt("toile_gluante", O, 100, 10, 0.5, 0), pounce("bond", 100, 9, 1.1), combo("crochets", 80, 0.6))),
            Map.entry("garde_funeraire", List.of(slam("frappe_funeraire", O, 150, 4.5, 1.4), cleave("fauchage", 90, 3.8, 1.2), charge("assaut", 150, 10, 1.3))),
            Map.entry("spectre_enchaine", List.of(bolt("chaine_spectrale", O, 70, 13, 0.9, 0), blink("glissade", O, 190, 1.1), nova("lamentation", O, 190, 4.5, 0.8),
                    volley("chaines", O, 150, 12, 0.6, 3))),
            Map.entry("roi_cryptes", List.of(summon("sujets", 360, "osselet", 2), slam("sceptre", O, 170, 5, 1.7), cleave("lame_royale", 90, 4.5, 1.3),
                    nova("decret_funebre", O, 240, 6, 1.1).ultime(), bolt("eclat_tombal", O, 80, 14, 1.0, 1.5))),
            Map.entry("voyageur_squelette", List.of(combo("passes", 80, 0.65), cleave("taille", 100, 3.5, 1.2), charge("fente", 140, 9, 1.3))),
            Map.entry("seigneur_squelette", List.of(cleave("grande_lame", 90, 6, 1.4), slam("jugement", O, 180, 6, 1.6), summon("garde", 400, "osselet", 3),
                    nova("effroi", O, 240, 7, 1.0).ultime(), charge("marche_funebre", 160, 12, 1.4))),
            // ---- terres gelees
            Map.entry("renard_givre", List.of(bolt("souffle_givre", G, 80, 9, 0.7, 1.2), pounce("bond_glace", 100, 9, 1.1), combo("morsures", 80, 0.6))),
            Map.entry("scarabee_polaire", List.of(nova("carapace_glacee", G, 170, 3.8, 0.6), charge("roulade", 120, 10, 1.2), cleave("mandibules", 80, 3, 1.1))),
            Map.entry("harpie_neiges", List.of(volley("plumes_de_givre", G, 90, 13, 0.6, 3), charge("pique", 110, 12, 1.2), nova("bourrasque", G, 180, 4.5, 0.7))),
            Map.entry("golem_iceberg", List.of(nova("onde_glaciale", G, 150, 6.5, 1.3), slam("poings_de_glace", G, 240, 4.5, 1.6).ultime(), cleave("revers_glace", 100, 4.5, 1.3),
                    bolt("bloc_de_glace", G, 120, 14, 1.2, 2.5))),
            Map.entry("wyrm_boreal", List.of(bolt("souffle_polaire", G, 90, 13, 1.3, 2.6), cleave("coup_de_queue", 100, 5, 1.3), nova("tempete", G, 220, 6, 1.0).ultime(),
                    volley("eclats", G, 140, 14, 0.7, 3))),
            Map.entry("sentinelle_hibou", List.of(bolt("plume_de_givre", G, 60, 16, 0.9, 0), volley("rafale", G, 140, 15, 0.6, 3), nova("battement", G, 190, 5, 0.8), retreat("envol", 150))),
            Map.entry("gardien_citadelle", List.of(slam("poing", G, 160, 6, 1.6), nova("givre", G, 220, 8, 1.2).ultime(), bolt("lance_de_glace", G, 100, 18, 1.3, 2.5),
                    cleave("balayage", 90, 6, 1.3))),
            // ---- desert volcanique
            Map.entry("scorpion_cuivre", List.of(bolt("dard_venimeux", O, 80, 10, 1.0, 0), combo("pinces", 80, 0.65), pounce("embuscade", 130, 8, 1.1))),
            Map.entry("momie_dunes", List.of(cloud("malediction", O, 150, 4, 0.4), roots("bandelettes", O, 180, 5, 0.6), cleave("etreinte", 90, 3.2, 1.2))),
            Map.entry("hyene_cendree", List.of(charge("bond", 100, 10, 1.3), combo("morsures", 70, 0.6), roar("ricanement", 240))),
            Map.entry("djinn_braises", List.of(bolt("boule_de_feu", F, 55, 17, 1.2, 2.0), nova("anneau_de_braise", F, 200, 5, 1.0), volley("gerbe", F, 140, 15, 0.7, 3),
                    blink("mirage", F, 200, 1.0))),
            Map.entry("tortue_volcanique", List.of(slam("eruption", F, 170, 4.5, 1.3), bolt("crachat_de_lave", F, 90, 12, 1.0, 2), nova("carapace_ardente", F, 200, 4, 0.8))),
            Map.entry("minotaure", List.of(charge("charge", 120, 14, 1.6), slam("marteau", A, 160, 5, 1.6), cleave("revers", 90, 5, 1.3), roar("mugissement", 280))),
            // ---- neant astral
            Map.entry("mite_astrale", List.of(bolt("eclat_astral", N, 50, 15, 0.8, 0), charge("pique", 110, 10, 1.0), nova("poussiere", N, 180, 4, 0.6))),
            Map.entry("rodeur_vide", List.of(blink("pas_du_vide", N, 130, 1.3), combo("lacerations", 70, 0.65), pounce("bond", 110, 9, 1.2))),
            Map.entry("oeil_chaos", List.of(bolt("regard_du_chaos", N, 45, 19, 1.0, 0), nova("pulsation", N, 190, 5, 0.9), volley("regards", N, 140, 17, 0.6, 3),
                    blink("clignement", N, 220, 0.8))),
            Map.entry("chevalier_fracture", List.of(blink("fente", N, 140, 1.5), slam("tranchant_du_neant", N, 200, 4, 1.4), cleave("taille", 80, 3.8, 1.3), charge("assaut", 150, 11, 1.4))),
            Map.entry("devoreur_etoiles", List.of(bolt("souffle_du_neant", N, 80, 15, 1.6, 3.0), slam("ecrasement_astral", N, 210, 6, 1.7), summon("rejetons", 420, "mite_astrale", 2),
                    cleave("machoires", 100, 6, 1.4), nova("supernova", N, 260, 8, 1.3).ultime())),
            Map.entry("morghul", List.of(slam("ecrasement", N, 170, 7, 1.7), cleave("griffes", 90, 7, 1.4), nova("abysse", N, 240, 9, 1.3).ultime(), bolt("bile", N, 90, 16, 1.3, 3),
                    summon("rejetons", 420, "rodeur_vide", 2))),
            // ---- bestiaire classique
            Map.entry("gobelin", List.of(combo("coups_de_dague", 70, 0.6), pounce("bond", 100, 8, 1.0), retreat("esquive", 200))),
            Map.entry("loup_sombre", List.of(pounce("bond", 80, 9, 1.1), combo("morsures", 70, 0.6), roar("hurlement", 240))),
            Map.entry("orc_guerrier", List.of(cleave("fendoir", 80, 3.5, 1.3), slam("coup_ecrasant", A, 140, 3.5, 1.7), roar("cri_de_guerre", 300), charge("ruee", 150, 10, 1.3))),
            Map.entry("golem_cristal", List.of(slam("frappe_au_sol", A, 160, 5, 1.4), cleave("revers", 100, 4, 1.2), volley("eclats_de_cristal", A, 150, 12, 0.6, 5))),
            Map.entry("elementaire_feu", List.of(bolt("boule_de_feu", F, 50, 16, 1.0, 1.5), nova("anneau_de_feu", F, 180, 4.5, 0.9), volley("gerbe", F, 140, 14, 0.6, 3), retreat("recul", 160))),
            Map.entry("spectre_givre", List.of(bolt("eclat_de_glace", G, 50, 16, 1.0, 0), nova("souffle_glacial", G, 180, 4.5, 0.8), blink("glissement", G, 200, 0.9))),
            Map.entry("chevalier_neant", List.of(blink("pas_du_neant", N, 160, 1.2), cleave("taille", 80, 3.5, 1.3), slam("entaille", N, 180, 4, 1.4))),
            Map.entry("bandit_arbaletrier", List.of(bolt("carreau", A, 45, 18, 1.0, 0), volley("salve", A, 150, 16, 0.6, 3), retreat("roulade", 120), combo("coups_de_crosse", 90, 0.6))),
            Map.entry("zombie_des_cryptes", List.of(combo("griffes", 80, 0.65), cloud("haleine_putride", O, 170, 3.5, 0.35), pounce("ruee", 140, 7, 1.1))),
            Map.entry("squelette_maudit", List.of(bolt("fleche_maudite", O, 45, 18, 1.0, 0), volley("pluie_de_fleches", O, 150, 16, 0.6, 3), retreat("recul", 130))),
            Map.entry("araignee_venimeuse", List.of(pounce("bond", 90, 9, 1.0), bolt("toile", O, 110, 10, 0.4, 0), combo("morsure_venimeuse", 80, 0.6))));

    /** Vrai si l'attaque de base du monstre est un tir : il garde alors ses distances au lieu de venir au contact. */
    public static boolean rangedRole(String key) {
        List<Spell> l = of(key);
        return !l.isEmpty() && (l.get(0).type() == Type.BOLT || l.get(0).type() == Type.VOLLEY) && l.get(0).cooldown() <= 60;
    }

    /** Vrai si le monstre tourne autour de sa proie entre deux assauts (bondisseurs). */
    public static boolean skirmisher(String key) {
        for (Spell s : of(key)) if (s.type() == Type.POUNCE) return true;
        return false;
    }

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
            if (s.type() == Type.BOLT || s.type() == Type.VOLLEY || s.type() == Type.CHARGE || s.type() == Type.POUNCE) c.abilities.putIfAbsent(s.id() + "Range", s.range());
            if (s.radius() > 0 && s.type() != Type.BOLT && s.type() != Type.POUNCE && s.type() != Type.CHARGE) c.abilities.putIfAbsent(s.id() + "Radius", s.radius());
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
                contact(mob, level, dmg, 1, 12, mob.getBbWidth() / 2 + 1.2, s.element());
            }
            case BLINK -> {
                MobAbilities.blinkBehind(mob, target);
                RpgCombat.mobHit(mob, target, dmg);
                level.sendParticles(particle(s.element()), target.getX(), target.getY(0.5), target.getZ(), 14, 0.4, 0.5, 0.4, 0.05);
            }
            case COMBO -> {
                strike(mob, target, dmg, s.element());
                Scheduler.later(level, 11, () -> { if (mob.isAlive() && target.isAlive()) strike(mob, target, dmg * 1.2, s.element()); });
            }
            case CLEAVE -> {
                Vec3 look = Vec3.directionFromRotation(0, mob.getYRot());
                level.addFreshEntity(SkillFxEntity.create(level, FxKind.SLASH, mob.position().add(0, mob.getBbHeight() * 0.5, 0), radius, color(s.element()), 8).angle(mob.getYRot()));
                for (LivingEntity e : MobAbilities.enemiesAround(mob, mob.position(), radius + mob.getBbWidth() / 2)) {
                    Vec3 to = e.position().subtract(mob.position()).multiply(1, 0, 1);
                    if (to.lengthSqr() > 1 && to.normalize().dot(look) < 0.2) continue;
                    RpgCombat.mobHit(mob, e, dmg);
                    effect(e, s.element(), 40);
                    Vec3 push = to.lengthSqr() < 1e-4 ? look : to.normalize();
                    if (com.mmorpg.combat.CrowdControl.allow(e)) e.push(push.x * 0.6, 0.25, push.z * 0.6);
                    e.needsSync = true;
                }
                level.playSound(null, mob.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP, SoundSource.HOSTILE, 1f, 0.7f);
            }
            case POUNCE -> {
                Vec3 to = target.position().subtract(mob.position()).multiply(1, 0, 1);
                double d = Math.min(1.5, 0.35 + to.length() * 0.13);
                Vec3 dir = to.lengthSqr() < 1e-4 ? Vec3.ZERO : to.normalize();
                mob.setDeltaMovement(dir.x * d, 0.42, dir.z * d);
                mob.needsSync = true;
                level.playSound(null, mob.blockPosition(), SoundEvents.GOAT_LONG_JUMP, SoundSource.HOSTILE, 0.8f, 1.1f);
                contact(mob, level, dmg, 2, 16, mob.getBbWidth() / 2 + 1.4, s.element());
            }
            case RETREAT -> {
                Vec3 away = mob.position().subtract(target.position()).multiply(1, 0, 1);
                Vec3 dir = away.lengthSqr() < 1e-4 ? Vec3.directionFromRotation(0, mob.getYRot() + 180) : away.normalize();
                mob.setDeltaMovement(dir.x * 1.05, 0.36, dir.z * 1.05);
                mob.needsSync = true;
                level.sendParticles(ParticleTypes.CLOUD, mob.getX(), mob.getY() + 0.2, mob.getZ(), 8, 0.3, 0.1, 0.3, 0.02);
            }
            case ROAR -> {
                level.addFreshEntity(SkillFxEntity.create(level, FxKind.SHOCKWAVE, mob.position(), 4, 0xFF8040, 16));
                for (Mob ally : level.getEntitiesOfClass(Mob.class, mob.getBoundingBox().inflate(radius), m -> m instanceof com.mmorpg.entity.RpgMob
                        && !(m instanceof com.mmorpg.entity.boss.RpgBoss) && m.isAlive() && !RpgCombat.isEnemy(mob, m))) {
                    ally.addEffect(new MobEffectInstance(MobEffects.SPEED, 140, 1));
                    ally.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 140, 0));
                    if (ally.getTarget() == null) ally.setTarget(target);
                }
                mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 140, 1));
                level.sendParticles(ParticleTypes.ANGRY_VILLAGER, mob.getX(), mob.getY() + mob.getBbHeight() + 0.3, mob.getZ(), 6, 0.5, 0.3, 0.5, 0);
                level.playSound(null, mob.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.HOSTILE, 1f, 1.2f);
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

    /** Coup au corps a corps d'une attaque : touche la cible si elle est encore a portee. */
    private static void strike(Mob mob, LivingEntity target, double dmg, Element element) {
        if (mob.distanceTo(target) > mob.getBbWidth() / 2 + target.getBbWidth() / 2 + 2.6) return;
        RpgCombat.mobHit(mob, target, dmg);
        effect(target, element, 40);
        if (mob.level() instanceof ServerLevel level) level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY(0.6), target.getZ(), 6, 0.2, 0.2, 0.2, 0.2);
    }

    /** Charge ou bond : le premier ennemi touche pendant le deplacement prend les degats et est repousse. */
    private static void contact(Mob mob, ServerLevel level, double dmg, int from, int to, double reach, Element element) {
        boolean[] hit = {false};
        for (int t = from; t <= to; t += 2) {
            Scheduler.later(level, t, () -> {
                if (hit[0] || !mob.isAlive()) return;
                for (LivingEntity e : MobAbilities.enemiesAround(mob, mob.position(), reach + 0.6)) {
                    hit[0] = true;
                    RpgCombat.mobHit(mob, e, dmg);
                    effect(e, element, 40);
                    Vec3 push = mob.getDeltaMovement().multiply(1, 0, 1);
                    push = push.lengthSqr() < 1e-4 ? Vec3.ZERO : push.normalize().scale(0.7);
                    if (com.mmorpg.combat.CrowdControl.allow(e)) e.push(push.x, 0.3, push.z);
                    e.needsSync = true;
                    break;
                }
            });
        }
    }

    public static int color(Element e) {
        return switch (e) {
            case FEU -> 0xFF7030;
            case GIVRE -> 0x80D8FF;
            case NEANT -> 0xA060FF;
            case OMBRE -> 0x70C060;
            case ECLAIR -> 0xFFE060;
            default -> 0xE0D0B0;
        };
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
            case OMBRE -> target.addEffect(new MobEffectInstance(MobEffects.POISON, ticks, 0));
            default -> { }
        }
    }
}
