package com.mmorpg.skill;

import com.mmorpg.rpg.PlayerClass;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.mmorpg.skill.Skill.pct;
import static com.mmorpg.skill.Skill.sec;

/** 44 competences : 10 actives et 1 passive par classe, avec 6 emplacements actifs. */
public final class Skills {
    private static final Map<String, Skill> BY_ID = new LinkedHashMap<>();
    private static final Map<PlayerClass, List<Skill>> BY_CLASS = new EnumMap<>(PlayerClass.class);

    // ------------------------------------------------------------------ GUERRIER
    public static final Skill FRAPPE_PUISSANTE = reg(Skill.builder("frappe_puissante", "Frappe Puissante", PlayerClass.GUERRIER, 1)
            .cost(10, 4).power(1.8).radius(4.0)
            .desc((s, r) -> "Assène un coup dévastateur devant vous, infligeant " + pct(s.scaled(r)) + " de l'Attaque à tous les ennemis dans un cône de " + (int) s.radius + " blocs et les repousse.")
            .build());
    public static final Skill CRI_DE_GUERRE = reg(Skill.builder("cri_de_guerre", "Cri de Guerre", PlayerClass.GUERRIER, 1)
            .cost(20, 30).power(0.20).power2(0.10).radius(10).duration(10)
            .desc((s, r) -> "Pousse un cri galvanisant : vous et vos alliés proches gagnez +" + pct(s.power + 0.04 * (r - 1)) + " d'Attaque et +" + pct(s.power2) + " de vitesse pendant " + sec(s.duration) + ".")
            .build());
    public static final Skill MAITRISE_ARMES = reg(Skill.builder("maitrise_armes", "Maîtrise des Armes", PlayerClass.GUERRIER, 1)
            .passive().power(0.05).power2(2)
            .desc((s, r) -> "Passif : augmente l'Attaque de " + pct(s.power * r) + " et les chances de critique de " + (int) (s.power2 * r) + " %.")
            .build());
    public static final Skill TOURBILLON = reg(Skill.builder("tourbillon", "Tourbillon d'Acier", PlayerClass.GUERRIER, 25)
            .cost(25, 8).power(1.5).radius(4.5)
            .desc((s, r) -> "Tournoie sur vous-même, infligeant " + pct(s.scaled(r)) + " de l'Attaque à tous les ennemis dans un rayon de " + s.radius + " blocs.")
            .build());
    public static final Skill CHARGE_BRUTALE = reg(Skill.builder("charge_brutale", "Charge Brutale", PlayerClass.GUERRIER, 50)
            .cost(20, 10).power(1.2).radius(9).duration(2)
            .desc((s, r) -> "Fonce sur " + (int) s.radius + " blocs, infligeant " + pct(s.scaled(r)) + " de l'Attaque aux ennemis traversés et les étourdissant " + sec(s.duration) + ".")
            .build());
    public static final Skill RAGE_SANGUINAIRE = reg(Skill.builder("rage_sanguinaire", "Rage Sanguinaire", PlayerClass.GUERRIER, 75)
            .cost(30, 45).power(0.30).power2(0.15).duration(12)
            .desc((s, r) -> "Entre dans une rage folle pendant " + sec(s.duration) + " : +" + pct(s.power + 0.05 * (r - 1)) + " d'Attaque et +" + pct(s.power2) + " de vol de vie, mais +10 % de dégâts subis.")
            .build());
    public static final Skill FUREUR_DIVINE = reg(Skill.builder("fureur_divine", "Fureur du Dieu de la Guerre", PlayerClass.GUERRIER, 100)
            .ultimate().cost(60, 60).power(4.0).radius(7)
            .desc((s, r) -> "ULTIME — Bondit dans les airs puis s'écrase au sol, infligeant " + pct(s.scaled(r)) + " de l'Attaque dans un rayon de " + (int) s.radius + " blocs et projetant les ennemis.")
            .build());

    // ------------------------------------------------------------------ MAGE
    public static final Skill BOULE_DE_FEU = reg(Skill.builder("boule_de_feu", "Boule de Feu", PlayerClass.MAGE, 1)
            .cost(15, 2).power(1.6).power2(0.5).radius(2.5)
            .desc((s, r) -> "Lance une boule de feu qui inflige " + pct(s.scaled(r)) + " de la Puissance magique à l'impact et " + pct(s.scaled2(r)) + " aux ennemis proches, et les enflamme.")
            .build());
    public static final Skill NOVA_DE_GIVRE = reg(Skill.builder("nova_de_givre", "Nova de Givre", PlayerClass.MAGE, 1)
            .cost(25, 10).power(1.2).radius(5).duration(4)
            .desc((s, r) -> "Libère une onde glaciale infligeant " + pct(s.scaled(r)) + " de la Puissance magique dans un rayon de " + (int) s.radius + " blocs et ralentit fortement les ennemis pendant " + sec(s.duration) + ".")
            .build());
    public static final Skill FLUX_ARCANIQUE = reg(Skill.builder("flux_arcanique", "Flux Arcanique", PlayerClass.MAGE, 1)
            .passive().power(0.04).power2(0.08)
            .desc((s, r) -> "Passif : augmente la Puissance magique de " + pct(s.power * r) + " et la régénération de mana de " + pct(s.power2 * r) + ".")
            .build());
    public static final Skill ECLAIR_EN_CHAINE = reg(Skill.builder("eclair_en_chaine", "Éclair en Chaîne", PlayerClass.MAGE, 25)
            .cost(30, 7).power(1.4).radius(7)
            .desc((s, r) -> "Foudroie la cible visée pour " + pct(s.scaled(r)) + " de la Puissance magique ; l'éclair rebondit sur " + (3 + r / 2) + " ennemis supplémentaires (-15 % par rebond).")
            .build());
    public static final Skill TELEPORTATION = reg(Skill.builder("teleportation_arcanique", "Transfert Arcanique", PlayerClass.MAGE, 50)
            .cost(20, 8).radius(10).duration(2)
            .desc((s, r) -> "Se téléporte instantanément jusqu'à " + (int) (s.radius + r) + " blocs devant soi et réduit les dégâts subis de 30 % pendant " + sec(s.duration) + ".")
            .build());
    public static final Skill BOUCLIER_DE_MANA = reg(Skill.builder("bouclier_de_mana", "Bouclier de Mana", PlayerClass.MAGE, 75)
            .cost(30, 40).power(1.5).duration(10)
            .desc((s, r) -> "Pendant " + sec(s.duration + 40 * (r - 1)) + ", les dégâts subis sont absorbés par votre mana (1 mana pour " + s.power + " dégâts).")
            .build());
    public static final Skill METEORE = reg(Skill.builder("meteore_celeste", "Météore Céleste", PlayerClass.MAGE, 100)
            .ultimate().cost(80, 60).power(5.0).radius(6)
            .desc((s, r) -> "ULTIME — Invoque un météore sur la zone visée : après 1 s, il inflige " + pct(s.scaled(r)) + " de la Puissance magique dans un rayon de " + (int) s.radius + " blocs.")
            .build());

    // ------------------------------------------------------------------ ARCHER
    public static final Skill TIR_PERCANT = reg(Skill.builder("tir_percant", "Tir Perçant", PlayerClass.ARCHER, 1)
            .cost(10, 3).power(1.7).radius(28)
            .desc((s, r) -> "Décoche une flèche surpuissante qui transperce tous les ennemis sur " + (int) s.radius + " blocs, infligeant " + pct(s.scaled(r)) + " de l'Attaque à chacun.")
            .build());
    public static final Skill PLUIE_DE_FLECHES = reg(Skill.builder("pluie_de_fleches", "Pluie de Flèches", PlayerClass.ARCHER, 1)
            .cost(25, 12).power(0.5).radius(4).duration(2)
            .desc((s, r) -> "Fait pleuvoir " + (10 + 2 * r) + " flèches sur la zone visée, chacune infligeant " + pct(s.scaled(r)) + " de l'Attaque.")
            .build());
    public static final Skill OEIL_DE_LYNX = reg(Skill.builder("oeil_de_lynx", "Œil de Lynx", PlayerClass.ARCHER, 1)
            .passive().power(3).power2(6)
            .desc((s, r) -> "Passif : augmente les chances de critique de " + (int) (s.power * r) + " % et les dégâts critiques de " + (int) (s.power2 * r) + " %.")
            .build());
    public static final Skill SAUT_ARRIERE = reg(Skill.builder("saut_arriere", "Saut Acrobatique", PlayerClass.ARCHER, 25)
            .cost(15, 6).power(0.30).radius(4).duration(3)
            .desc((s, r) -> "Effectue un bond en arrière, ralentit les ennemis proches et gagne +" + pct(s.power + 0.05 * (r - 1)) + " de vitesse pendant " + sec(s.duration) + ".")
            .build());
    public static final Skill FLECHE_EXPLOSIVE = reg(Skill.builder("fleche_explosive", "Flèche Explosive", PlayerClass.ARCHER, 50)
            .cost(25, 8).power(2.2).radius(3.5)
            .desc((s, r) -> "Tire une flèche qui explose à l'impact, infligeant " + pct(s.scaled(r)) + " de l'Attaque dans un rayon de " + s.radius + " blocs.")
            .build());
    public static final Skill VOLEE = reg(Skill.builder("volee_de_fleches", "Volée de Flèches", PlayerClass.ARCHER, 75)
            .cost(30, 10).power(0.9)
            .desc((s, r) -> "Tire un éventail de " + (6 + r) + " flèches, chacune infligeant " + pct(s.scaled(r)) + " de l'Attaque.")
            .build());
    public static final Skill TEMPETE_DIVINE = reg(Skill.builder("tempete_divine", "Tempête Divine", PlayerClass.ARCHER, 100)
            .ultimate().cost(70, 60).power(1.2).radius(22).duration(3)
            .desc((s, r) -> "ULTIME — Pendant " + sec(s.duration) + ", tire des flèches de lumière à tête chercheuse sur tous les ennemis dans un rayon de " + (int) s.radius + " blocs (" + pct(s.scaled(r)) + " de l'Attaque chacune).")
            .build());

    // ------------------------------------------------------------------ TANK
    public static final Skill COUP_DE_BOUCLIER = reg(Skill.builder("coup_de_bouclier", "Coup de Bouclier", PlayerClass.TANK, 1)
            .cost(10, 5).power(1.2).power2(0.5).radius(3.5).duration(1.5)
            .desc((s, r) -> "Frappe avec votre bouclier : " + pct(s.scaled(r)) + " de l'Attaque + " + pct(s.scaled2(r)) + " de la Défense, et étourdit la cible " + sec(s.duration) + ".")
            .build());
    public static final Skill PROVOCATION = reg(Skill.builder("provocation", "Provocation", PlayerClass.TANK, 1)
            .cost(15, 15).power(0.20).radius(10).duration(6)
            .desc((s, r) -> "Force tous les monstres dans un rayon de " + (int) (s.radius + 2 * (r - 1)) + " blocs à vous attaquer et augmente votre Défense de " + pct(s.power) + " pendant " + sec(s.duration) + ".")
            .build());
    public static final Skill PEAU_DE_FER = reg(Skill.builder("peau_de_fer", "Peau de Fer", PlayerClass.TANK, 1)
            .passive().power(0.05).power2(0.03)
            .desc((s, r) -> "Passif : augmente les PV de " + pct(s.power * r) + ", la Défense de " + pct(s.power2 * r) + " et réduit les dégâts subis de " + (2 * r) + " %.")
            .build());
    public static final Skill FORTERESSE = reg(Skill.builder("forteresse", "Forteresse", PlayerClass.TANK, 25)
            .cost(25, 30).power(0.50).duration(6)
            .desc((s, r) -> "Vous devenez une forteresse pendant " + sec(s.duration + 20 * (r - 1)) + " : dégâts subis réduits de " + pct(s.power) + ", mais vitesse réduite.")
            .build());
    public static final Skill ONDE_DE_CHOC = reg(Skill.builder("onde_de_choc", "Onde de Choc", PlayerClass.TANK, 50)
            .cost(25, 10).power(1.5).power2(1.0).radius(5)
            .desc((s, r) -> "Frappe le sol : " + pct(s.scaled(r)) + " de l'Attaque + " + pct(s.scaled2(r)) + " de la Défense dans un rayon de " + (int) s.radius + " blocs, en projetant les ennemis.")
            .build());
    public static final Skill AURA_SACREE = reg(Skill.builder("aura_sacree", "Aura Sacrée", PlayerClass.TANK, 75)
            .cost(40, 40).power(0.25).radius(8).duration(5)
            .desc((s, r) -> "Soigne vous et vos alliés proches de " + pct(s.power + 0.05 * (r - 1)) + " des PV max sur " + sec(s.duration) + ".")
            .build());
    public static final Skill REMPART_DU_TITAN = reg(Skill.builder("rempart_du_titan", "Rempart du Titan", PlayerClass.TANK, 100)
            .ultimate().cost(60, 75).power(0.90).power2(0.5).radius(15).duration(8)
            .desc((s, r) -> "ULTIME — Pendant " + sec(s.duration) + " : dégâts subis réduits de " + pct(s.power) + ", renvoie " + pct(s.power2) + " des dégâts reçus et provoque tous les ennemis à " + (int) s.radius + " blocs.")
            .build());

    public static final Skill FRAPPE_FRAGILISANTE = reg(Skill.builder("frappe_fragilisante", "Frappe Fragilisante", PlayerClass.GUERRIER, 10)
            .cost(15, 9).power(1.4).radius(4).duration(5)
            .desc((s, r) -> "Inflige " + pct(s.scaled(r)) + " de l’Attaque et affaiblit la cible pendant 5 s.").build());
    public static final Skill EXECUTION = reg(Skill.builder("execution", "Exécution", PlayerClass.GUERRIER, 40)
            .cost(25, 15).power(2.2).radius(4).duration(0)
            .desc((s, r) -> "Inflige " + pct(s.scaled(r)) + " de l’Attaque ; dégâts doublés si la cible est sous 30 % de vie.").build());
    public static final Skill IMPACT_VAMPIRIQUE = reg(Skill.builder("impact_vampirique", "Impact Vampirique", PlayerClass.GUERRIER, 65)
            .cost(30, 18).power(1.6).radius(5).duration(0)
            .desc((s, r) -> "Frappe les ennemis proches pour " + pct(s.scaled(r)) + " de l’Attaque ; soigne 3 % des PV max par cible touchée (maximum 15 %).").build());
    public static final Skill SECOND_SOUFFLE = reg(Skill.builder("second_souffle", "Second Souffle", PlayerClass.GUERRIER, 90)
            .cost(25, 45).power(0.2).radius(0).duration(0)
            .desc((s, r) -> "Récupère " + pct(s.scaled(r)) + " de vos PV max.").build());
    public static final Skill LANCE_DE_GIVRE = reg(Skill.builder("lance_de_givre", "Lance de Givre", PlayerClass.MAGE, 10)
            .cost(18, 6).power(1.5).radius(22).duration(4)
            .desc((s, r) -> "Inflige " + pct(s.scaled(r)) + " de la Magie à la cible visée et la ralentit pendant 4 s.").build());
    public static final Skill SOUFFLE_DRACONIQUE = reg(Skill.builder("souffle_draconique", "Souffle Draconique", PlayerClass.MAGE, 40)
            .cost(28, 12).power(1.8).radius(7).duration(5)
            .desc((s, r) -> "Inflige " + pct(s.scaled(r)) + " de la Magie dans un cône et enflamme les ennemis pendant 5 s.").build());
    public static final Skill IMPLOSION = reg(Skill.builder("implosion", "Implosion", PlayerClass.MAGE, 65)
            .cost(40, 18).power(2).radius(5).duration(0)
            .desc((s, r) -> "Attire les ennemis vers le point visé (20 blocs), puis inflige " + pct(s.scaled(r)) + " de la Magie dans un rayon de 5 blocs.").build());
    public static final Skill MEDITATION = reg(Skill.builder("meditation", "Méditation", PlayerClass.MAGE, 90)
            .cost(0, 45).power(0.3).radius(0).duration(0)
            .desc((s, r) -> "Récupère " + pct(s.scaled(r)) + " de votre mana maximal.").build());
    public static final Skill TIR_ENTRAVANT = reg(Skill.builder("tir_entravant", "Tir Entravant", PlayerClass.ARCHER, 10)
            .cost(12, 8).power(1.3).radius(28).duration(4)
            .desc((s, r) -> "Inflige " + pct(s.scaled(r)) + " de l’Attaque à la cible visée et la ralentit fortement pendant 4 s.").build());
    public static final Skill TIR_VENIMEUX = reg(Skill.builder("tir_venimeux", "Tir Venimeux", PlayerClass.ARCHER, 40)
            .cost(22, 14).power(0.5).radius(26).duration(6)
            .desc((s, r) -> "Inflige " + pct(s.scaled(r)) + " de l’Attaque à l’impact puis le même montant chaque seconde pendant 6 s.").build());
    public static final Skill TIR_DE_RECUL = reg(Skill.builder("tir_de_recul", "Tir de Recul", PlayerClass.ARCHER, 65)
            .cost(22, 10).power(1.8).radius(20).duration(0)
            .desc((s, r) -> "Inflige " + pct(s.scaled(r)) + " de l’Attaque à la cible visée et la repousse.").build());
    public static final Skill PAS_LEGER = reg(Skill.builder("pas_leger", "Pas Léger", PlayerClass.ARCHER, 90)
            .cost(25, 30).power(0.35).radius(0).duration(8)
            .desc((s, r) -> "Augmente votre vitesse de " + pct(s.scaled(r)) + " pendant 8 s.").build());
    public static final Skill CROCHET_DU_GARDIEN = reg(Skill.builder("crochet_du_gardien", "Crochet du Gardien", PlayerClass.TANK, 10)
            .cost(15, 10).power(0.9).radius(12).duration(3)
            .desc((s, r) -> "Inflige " + pct(s.scaled(r)) + " de l’Attaque, attire la cible et la provoque pendant 3 s.").build());
    public static final Skill JUGEMENT = reg(Skill.builder("jugement", "Jugement", PlayerClass.TANK, 40)
            .cost(25, 12).power(2).radius(5).duration(0)
            .desc((s, r) -> "Inflige " + pct(s.scaled(r)) + " de votre Défense aux ennemis proches.").build());
    public static final Skill GARDE_PARTAGEE = reg(Skill.builder("garde_partagee", "Garde Partagée", PlayerClass.TANK, 65)
            .cost(35, 35).power(0.3).radius(8).duration(6)
            .desc((s, r) -> "Réduit les dégâts subis de 30 % pendant 6 s pour vous et votre groupe à 8 blocs.").build());
    public static final Skill SOUFFLE_DU_GARDIEN = reg(Skill.builder("souffle_du_gardien", "Souffle du Gardien", PlayerClass.TANK, 90)
            .cost(40, 45).power(0.15).radius(8).duration(0)
            .desc((s, r) -> "Soigne " + pct(s.scaled(r)) + " des PV max des membres de votre groupe à 8 blocs et de vous-même.").build());

    private Skills() {
    }

    private static Skill reg(Skill skill) {
        BY_ID.put(skill.id, skill);
        BY_CLASS.computeIfAbsent(skill.playerClass, c -> new ArrayList<>()).add(skill);
        return skill;
    }

    public static Skill get(String id) {
        return id == null ? null : BY_ID.get(id);
    }

    public static List<Skill> forClass(PlayerClass cls) {
        return BY_CLASS.getOrDefault(cls, List.of()).stream().sorted(java.util.Comparator.comparingInt(s -> s.unlockLevel)).toList();
    }

    public static Iterable<Skill> all() {
        return BY_ID.values();
    }

    public static void init() {
        // force le chargement statique
    }
}
