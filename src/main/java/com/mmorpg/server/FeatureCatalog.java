package com.mmorpg.server;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Categories et libelles des interrupteurs de {@link FeatureFlags} pour l'ecran d'administration (donnees pures, communes client/serveur). */
public final class FeatureCatalog {
    private FeatureCatalog() {
    }

    /** {cle, libelle, description courte}. */
    public record Entry(String key, String label, String desc) {
    }

    public static final Map<String, List<Entry>> CATEGORIES = new LinkedHashMap<>();

    static {
        CATEGORIES.put("Combat", List.of(
                new Entry("arenes", "Arènes PvP", "Matchs classés et files d'attente."),
                new Entry("donjons", "Donjons", "Instances de groupe et portails."),
                new Entry("portails", "Portails d'aventure", "Portails vers les expéditions."),
                new Entry("competences", "Compétences", "Sorts et capacités de classe."),
                new Entry("bouclier", "Bouclier", "Garde et usure du bouclier."),
                new Entry("apparition_mobs", "Apparition des mobs", "Apparitions personnalisées."),
                new Entry("epreuves_classes", "Épreuves de classe", "Zone d'essai du hub."),
                new Entry("boss_guilde", "Boss de guilde", "Raids de guilde."),
                new Entry("drones", "Drones", "Drones de combat.")));
        CATEGORIES.put("Économie", List.of(
                new Entry("boutique", "Boutique", "Boutique premium."),
                new Entry("boutique_pvp", "Boutique PvP", "Récompenses de l'arène."),
                new Entry("hdv", "Hôtel des ventes", "Ventes entre joueurs."),
                new Entry("banque", "Banque", "Coffre et or en banque."),
                new Entry("marchands", "Marchands", "PNJ marchands."),
                new Entry("echange", "Échanges", "Commande /trade."),
                new Entry("sacs", "Sacs à dos", "Sacs et stockage portable.")));
        CATEGORIES.put("Progression", List.of(
                new Entry("quetes", "Quêtes", "Journal et PNJ de quêtes."),
                new Entry("quetes_hebdo", "Quêtes hebdo", "Défis de la semaine."),
                new Entry("metiers", "Métiers", "Mineur, pêcheur, etc."),
                new Entry("peche", "Pêche", "Mini-jeu et butin."),
                new Entry("succes", "Succès", "Succès et titres."),
                new Entry("wiki", "Wiki", "Guide consultable en jeu."),
                new Entry("classements", "Classements", "Tableaux de classement."),
                new Entry("forge", "Forge", "Fabrication d'équipement.")));
        CATEGORIES.put("Social", List.of(
                new Entry("guildes", "Guildes", "Création et gestion de guilde."),
                new Entry("chat_guilde", "Chat de guilde", "Canal de discussion."),
                new Entry("groupes", "Groupes", "Groupes de joueurs.")));
        CATEGORIES.put("Collection", List.of(
                new Entry("familiers", "Familiers", "Compagnons de combat."),
                new Entry("montures", "Montures", "Chevauchées."),
                new Entry("cosmetiques", "Cosmétiques", "Apparences et auras."),
                new Entry("bestiaire", "Bestiaire", "Créatures rencontrées."),
                new Entry("luckyblocks", "Blocs chanceux", "Blocs à butin."),
                new Entry("coffres", "Coffres", "Coffres à butin.")));
        CATEGORIES.put("Monde", List.of(
                new Entry("mines", "Mines", "Étages miniers."),
                new Entry("plaines", "Plaines", "Zone des plaines."),
                new Entry("cinematique", "Cinématique", "Intro du hub."),
                new Entry("anticheat", "Anticheat", "Limites de débit et détections.")));
    }
}
