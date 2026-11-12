package com.mmorpg.quest;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.IntFunction;

/** Quetes par defaut : une campagne qui suit la progression des niveaux 1 a 100, plus des quetes journalieres. */
public final class DefaultQuests {
    /** Incremente quand les quetes par defaut changent : les quetes d'un quests.json plus ancien sont alors remplacees une fois. */
    public static final int REVISION = 6;

    private DefaultQuests() {
    }

    private static QuestDef q(String name, int level, String desc) {
        QuestDef d = new QuestDef();
        d.name = name;
        d.minLevel = level;
        d.description = desc;
        return d;
    }

    private static QuestDef obj(QuestDef d, String type, String target, int count) {
        d.objectives.add(new QuestDef.Objective(type, target.contains(":") || type.equals("kill") || type.equals("level") ? target : "mmorpg:" + target, count));
        return d;
    }

    private static QuestDef reward(QuestDef d, long xp, long gold, Object... items) {
        d.rewards.xp = xp;
        d.rewards.gold = gold;
        for (int i = 0; i + 1 < items.length; i += 2) {
            String id = (String) items[i];
            d.rewards.items.add(new QuestDef.ItemReward(id.contains(":") ? id : "mmorpg:" + id, (Integer) items[i + 1]));
        }
        return d;
    }

    private static QuestDef req(QuestDef d, String... ids) {
        d.requires.addAll(List.of(ids));
        return d;
    }

    private static QuestDef by(QuestDef d, String group) {
        d.giver = group;
        return d;
    }

    // ------------------------------------------------ tutoriel de la Mascotte (groupe « tutoriel »)
    private static final String M = "tuto_";

    /** Quete du tutoriel : proposee par la Mascotte, chainee a la precedente, avec ses repliques. */
    private static QuestDef tq(Map<String, QuestDef> m, String prev, String id, String name, String desc, String speech, String done, long xp, long gold,
                               Object... items) {
        QuestDef d = q(name, 1, desc);
        d.giver = QuestNpcPlan.GROUP_TUTORIAL;
        d.npc = QuestNpcPlan.MASCOT_ID.toString();
        d.speech = speech;
        d.speechDone = done;
        if (prev != null) d.requires.add(M + prev);
        reward(d, xp, gold, items);
        m.put(M + id, d);
        return d;
    }

    /** Objectif de decouverte : {@code key} = cle d'action (voir QuestManager#onAction), {@code label} = texte affiche. */
    private static QuestDef act(QuestDef d, String key, String label, int count) {
        d.objectives.add(new QuestDef.Objective("action", key + "|" + label, count));
        return d;
    }

    private static QuestDef open(QuestDef d, String screen, String label) {
        return act(d, "screen:" + screen, label, 1);
    }

    /** Le tutoriel complet : equipement fabrique a la forge, puis une quete par fonctionnalite du mod. */
    public static Map<String, QuestDef> tutorial() {
        Map<String, QuestDef> m = new LinkedHashMap<>();
        // --- partie 1 : tout se fabrique, rien n'est offert au depart
        tq(m, null, "bienvenue", "Bienvenue, voyageur", "La Mascotte, votre guide, vous accueille. Vous n'avez rien sur vous : tout se fabrique.",
                "Salut, voyageur ! Je suis la Mascotte, ton guide. Tu n'as rien sur toi : ici, tout se fabrique. Suis mes missions et tu deviendras un héros !",
                "Voici de quoi forger ta première pioche et ta houe. Direction la Forge Arcanique !", 40, 20, "ecorce_ancienne", 4, "ferraille_gobeline", 5);
        obj(m.get(M + "bienvenue"), "level", "niveau", 1);
        open(tq(m, "bienvenue", "forge", "La Forge Arcanique", "La forge transforme les matériaux en outils, armes et armures.",
                "Cherche la Forge Arcanique du spawn (bloc de forge) et ouvre-la en faisant un clic droit dessus.",
                "Voilà la forge ! Chaque recette montre les matériaux, l'or et le niveau requis.", 40, 5), "ForgeScreen", "Ouvrir la Forge Arcanique");
        act(tq(m, "forge", "pioche", "Votre première pioche", "Fabriquez une pioche du débutant à la forge.",
                "Dans la forge, onglet Outils : fabrique ta Pioche du débutant avec les matériaux que je t'ai donnés.",
                "Belle pioche ! Elle servira à extraire la Verdite dans les mines.", 60, 0), "craft:pioche_debutant", "Fabriquer la pioche du débutant", 1);
        act(tq(m, "pioche", "houe", "Votre première houe", "Fabriquez une houe du débutant à la forge.",
                "Maintenant la houe du débutant, pour labourer la terre et cultiver tes premières graines.",
                "Parfait ! Tu pourras bientôt cultiver. Voici quelques graines de Lueur.", 60, 0, "graine_lueur", 4), "craft:houe_debutant", "Fabriquer la houe du débutant", 1);
        open(tq(m, "houe", "mines", "Le Maître des mines", "Le Maître des mines vous mène aux galeries, riches en minerais.",
                "Les mines d'Aeloria regorgent de minerais ! Parle au Maître des mines : il t'y emmènera.",
                "Les mines sont dangereuses mais précieuses. Prends garde aux monstres !", 60, 0), "MineMasterScreen", "Parler au Maître des mines");
        act(tq(m, "mines", "minage", "Premiers coups de pioche", "Minez dix blocs de minerai avec votre pioche.",
                "À la pioche, mine 10 minerais de Verdite. Chaque minerai fait aussi progresser ton métier de Mineur !",
                "Bravo, mineur ! Voici de quoi forger ta première arme.", 150, 20, "@mat_arme", 1), "job:mineur", "Miner des minerais", 10);
        open(tq(m, "minage", "presse", "La presse de compression", "La presse transforme seize lingots en minerai compressé, utile aux grandes armures.",
                "Pour les équipements avancés, il faut compresser les lingots. Va voir la Presse de compression du spawn !",
                "Retiens-le : seize lingots donnent un minerai compressé.", 60, 0), "CompressorScreen", "Ouvrir la presse de compression");
        act(tq(m, "presse", "arme", "Votre première arme", "Fabriquez l'arme de votre classe à la forge.",
                "À la forge, fabrique l'arme de ta classe avec les matériaux reçus. Un héros sans arme, ça ne va pas loin !",
                "Voici ton arme ! Prends-la en main et prépare ton armure.", 200, 30, "@mat_armure", 1), "craft:@arme", "Fabriquer l'arme de votre classe", 1);
        QuestDef armor = tq(m, "arme", "armure", "Votre première armure", "Fabriquez les quatre pièces de l'armure de votre classe.",
                "Il te faut maintenant une armure complète : casque, plastron, jambières et bottes. Équipe-les ensuite !",
                "Te voilà équipé ! Voici des potions, du mana et des parchemins de téléportation pour la route.", 300, 50,
                "potion_soin_mineure", 5, "potion_mana_mineure", 3, "parchemin_teleportation", 2);
        act(armor, "craft:@casque", "Fabriquer le casque", 1);
        act(armor, "craft:@plastron", "Fabriquer le plastron", 1);
        act(armor, "craft:@jambieres", "Fabriquer les jambières", 1);
        act(armor, "craft:@bottes", "Fabriquer les bottes", 1);
        // --- partie 2 : une fonctionnalite par quete
        open(tq(m, "armure", "personnage", "Votre personnage", "Le menu personnage regroupe équipement, attributs et inventaire.",
                "Appuie sur [M] pour ouvrir le menu du personnage. Équipe ton armure et regarde tes attributs !",
                "Chaque niveau te donne des points d'attributs à répartir : force, esprit, vitalité...", 100, 10), "CharacterScreen", "Ouvrir le menu du personnage");
        open(tq(m, "personnage", "competences", "Les compétences", "Chaque classe possède des compétences à débloquer et à améliorer.",
                "Ouvre l'onglet Compétences. Place-les dans ta barre pour les lancer en combat !",
                "Les compétences montent de rang avec tes points. Choisis bien !", 100, 10), "SkillsScreen", "Ouvrir l'onglet Compétences");
        obj(tq(m, "competences", "combat", "Baptême du feu", "Éliminez trois gobelins dans les plaines.",
                "Va dans les plaines et élimine 3 gobelins avec ton arme. Garde des potions sous la main !",
                "Bien joué ! Le butin des monstres sert à forger de meilleurs équipements.", 200, 20, "potion_soin_mineure", 2), "kill", "gobelin", 3);
        open(tq(m, "combat", "bestiaire", "Le bestiaire", "Le bestiaire recense les monstres rencontrés et leur butin.",
                "Chaque monstre vaincu s'ajoute au Bestiaire, avec son butin. Ouvre-le pour voir ce que tu as déjà croisé !",
                "Compléter le bestiaire apporte des récompenses et des succès.", 100, 10), "BestiaryScreen", "Ouvrir le bestiaire");
        open(tq(m, "bestiaire", "journal", "Le journal de quêtes", "Le journal garde vos quêtes, leurs objectifs et leurs récompenses.",
                "Ouvre l'onglet Quêtes : tu y suis tes missions et peux choisir celle à afficher à l'écran.",
                "Tu peux aussi abandonner une quête que tu ne veux plus faire.", 100, 10), "QuestLogScreen", "Ouvrir le journal de quêtes");
        open(tq(m, "journal", "metiers", "Les métiers", "Pêcheur, fermier et mineur : trois métiers qui montent jusqu'au niveau 50.",
                "Il y a trois métiers : Pêcheur, Fermier et Mineur. Ouvre l'onglet Métiers pour voir leurs bonus tous les 5 niveaux !",
                "Chaque palier de métier offre des récompenses : titres, plats, cosmétiques...", 150, 20), "JobsScreen", "Ouvrir l'onglet Métiers");
        act(tq(m, "metiers", "peche", "Une journée de pêche", "Pêchez un poisson dans un lac ou une rivière.",
                "Voici une canne ! Équipe-la, lance-la dans l'eau et, quand ça mord, clique dans la zone de la jauge pour ramener la prise.",
                "Quelle prise ! Les poissons rares, les coffres et les trésors t'attendent dans les eaux profondes.", 200, 20, "canne_apprenti", 1),
                "stat:fish_caught", "Pêcher un poisson", 1);
        act(tq(m, "peche", "ferme", "Les premières récoltes", "Labourez avec votre houe, plantez des graines et récoltez.",
                "Laboure la terre avec ta houe, plante des graines de Lueur et récolte-les quand elles sont mûres !",
                "Un vrai fermier ! Les plats de métier renforcent ton personnage.", 200, 20), "job:fermier", "Récolter des plantes", 3);
        open(tq(m, "ferme", "marchand", "Les marchands", "Les marchands achètent le butin et vendent des fournitures.",
                "Parle à un marchand : tu peux y vendre ton butin et acheter des potions, des graines et des outils.",
                "Garde ton or : la banque et l'hôtel des ventes en ont aussi besoin.", 100, 30), "ShopScreen", "Parler à un marchand");
        open(tq(m, "marchand", "banque", "La banque", "La banque garde votre or et vos objets en sécurité.",
                "Passe à la banque d'Aeloria : tu y déposes ton or et tes objets, à l'abri des pertes en expédition.",
                "Ta banque est partagée entre tous les serveurs du royaume.", 100, 10), "BankScreen", "Ouvrir la banque");
        open(tq(m, "banque", "hdv", "L'hôtel des ventes", "L'hôtel des ventes permet d'acheter et de vendre aux autres joueurs.",
                "À l'hôtel des ventes, tu mets tes objets en vente aux autres joueurs et tu achètes les leurs. Va le visiter !",
                "Pense à comparer les prix avant de vendre.", 100, 10), "AuctionScreen", "Ouvrir l'hôtel des ventes");
        open(tq(m, "hdv", "teleporteur", "Les téléporteurs", "Les téléporteurs relient les villes et les zones débloquées.",
                "Les téléporteurs te mènent d'une zone à l'autre. Ouvre-en un pour voir les destinations !",
                "Les nouvelles destinations se débloquent avec ton niveau.", 100, 10), "TeleporterScreen", "Utiliser un téléporteur");
        open(tq(m, "teleporteur", "groupe", "Les groupes", "Jusqu'à quatre joueurs peuvent former un groupe pour les donjons.",
                "L'union fait la force ! Ouvre l'onglet Groupe pour inviter des amis : les donjons se font à plusieurs.",
                "Le chef du groupe décide quand lancer l'expédition.", 100, 10), "PartyScreen", "Ouvrir l'onglet Groupe");
        open(tq(m, "groupe", "donjon", "Les donjons", "Les donjons sont privés pour votre groupe et annoncent leurs dangers.",
                "Parle au Maître des donjons : choisis un donjon et sa difficulté. Attention, le butin de session peut être perdu à la mort !",
                "Chaque donjon cache un boss et un butin rare.", 150, 20), "DungeonMasterScreen", "Parler au Maître des donjons");
        open(tq(m, "donjon", "guilde", "Les guildes", "Une guilde réunit des joueurs : chat, coffre, raids et arènes.",
                "Rejoins ou fonde une guilde dans l'onglet Guilde. Tu auras un chat privé, des raids et des arènes de guilde !",
                "Une guilde, c'est une famille. Le chat de guilde est un canal à part.", 150, 20), "GuildScreen", "Ouvrir l'onglet Guilde");
        open(tq(m, "guilde", "arene", "L'arène", "L'arène oppose les joueurs dans des duels et des matchs classés.",
                "Dans l'arène, tu affrontes d'autres joueurs et gagnes des points de rang. Ouvre l'onglet Arène !",
                "Les récompenses d'arène s'échangent chez l'Intendant.", 150, 20), "ArenaScreen", "Ouvrir l'onglet Arène");
        open(tq(m, "arene", "echange", "Les échanges", "La commande /trade permet d'échanger des objets en toute sécurité.",
                "Pour échanger avec un autre joueur, tape /trade suivi de son pseudo. Un double accord est demandé : aucune arnaque possible !",
                "Les deux joueurs doivent confirmer l'échange.", 100, 10), "TradeScreen", "Ouvrir une fenêtre d'échange (/trade)");
        open(tq(m, "echange", "succes", "Les succès", "Les succès récompensent vos exploits avec des titres et des médailles.",
                "Chaque exploit débloque un succès et parfois un titre. Ouvre l'onglet Succès pour voir lesquels t'attendent !",
                "Tu peux afficher ton titre favori au-dessus de ta tête.", 100, 10), "AchievementsScreen", "Ouvrir l'onglet Succès");
        open(tq(m, "succes", "hebdo", "Les quêtes hebdomadaires", "Chaque semaine, la pancarte propose de nouveaux défis.",
                "Une pancarte du spawn propose des défis chaque semaine, avec de belles récompenses. Va la lire !",
                "Les quêtes hebdomadaires se renouvellent chaque semaine.", 150, 20), "WeeklyQuestScreen", "Lire la pancarte hebdomadaire");
        open(tq(m, "hebdo", "familiers", "Les familiers", "Les familiers suivent le joueur et donnent des bonus.",
                "Les familiers sont de fidèles compagnons qui donnent des bonus ! Ouvre l'onglet Familiers pour voir lesquels tu possèdes.",
                "On obtient les familiers dans les caisses et les Lucky Blocks.", 100, 10), "PetsScreen", "Ouvrir l'onglet Familiers");
        open(tq(m, "familiers", "montures", "Les montures", "Les montures vous permettent de voyager plus vite.",
                "Pour aller plus vite, rien de tel qu'une monture. Ouvre l'onglet Montures !",
                "Chaque monture a sa vitesse et son allure.", 100, 10), "MountsScreen", "Ouvrir l'onglet Montures");
        open(tq(m, "montures", "cosmetiques", "Les cosmétiques", "Auras, ailes, couronnes : personnalisez votre apparence.",
                "Un héros doit avoir de la prestance ! Ouvre l'onglet Cosmétiques pour personnaliser ton apparence.",
                "Les cosmétiques n'ont aucun effet sur le combat.", 100, 10), "CosmeticsScreen", "Ouvrir l'onglet Cosmétiques");
        open(tq(m, "cosmetiques", "sac", "Le sac à dos", "Le sac à dos étend votre capacité de rangement.",
                "Ton sac à dos te donne de la place en plus. Ouvre-le avec la touche prévue !",
                "Des sacs plus grands se fabriquent à la forge.", 100, 10), "BackpackScreen", "Ouvrir le sac à dos");
        open(tq(m, "sac", "boutique", "La boutique", "La boutique d'Aeloria propose des cosmétiques et des avantages.",
                "La boutique d'Aeloria a des cosmétiques et des extras. Jette-y un œil, rien ne t'oblige à acheter !",
                "Les crédits de boutique sont une monnaie distincte de l'or.", 100, 10), "PremiumStoreScreen", "Ouvrir la boutique");
        open(tq(m, "boutique", "wiki", "Le wiki", "Le wiki intégré explique chaque système du mod.",
                "Si tu as un doute, le Wiki intégré explique tout. Ouvre l'onglet Wiki !",
                "Tu peux le consulter à tout moment.", 100, 10), "WikiScreen", "Ouvrir le wiki");
        obj(tq(m, "wiki", "fin", "Héros d'Aeloria", "Le tutoriel est terminé : l'aventure commence vraiment.",
                "Tu connais maintenant tout ce qu'il faut ! Reviens me voir pour ta récompense finale, puis parle à Maëlys : l'aventure t'attend.",
                "Tu es prêt, héros ! Maëlys t'attend pour les vraies missions. Bonne chance, je veille sur toi !", 800, 200,
                "elixir_experience", 1, "pierre_amelioration", 2, "cle_quete", 1), "level", "niveau", 1);
        m.values().forEach(d -> d.revision = REVISION);
        return m;
    }

    /** Quetes de l'ancienne campagne (hors classes et evolutions, conservees), supprimees par la revision 5 (remplacees par le plan de quetes du lore). */
    public static final java.util.Set<String> LEGACY_IDS = legacyIds();

    private static java.util.Set<String> legacyIds() {
        var ids = new java.util.LinkedHashSet<>(java.util.List.of("premiers_pas", "oreilles_pointues", "apprentissage", "crocs_nuit", "ferraille", "nettoyer_cryptes",
                "toiles_venimeuses", "roi_gobelin", "os_maudits", "poussiere_ame", "brutes_orcs", "routes_sures", "liche_ancienne", "coeur_flammes", "souffle_givre",
                "seigneur_ignis", "cristaux_vivants", "titan_glace", "chevaliers_neant", "avatar_neant", "contrat_chasse", "approvisionnement", "tueur_geants"));
        for (int i = 1; i <= 12; i++) ids.add("histoire_" + i);
        return java.util.Set.copyOf(ids);
    }

    /** Identifiants des quetes du tutoriel, dans l'ordre. */
    public static java.util.List<String> tutorialIds() {
        return java.util.List.copyOf(tutorial().keySet());
    }

    public static Map<String, QuestDef> create() {
        return create(com.mmorpg.crafting.ForgeProgression::scroll);
    }

    /** {@code scroll} donne l'objet parchemin de forge d'un palier (injecte pour pouvoir valider les quetes sans le registre du jeu). */
    public static Map<String, QuestDef> create(IntFunction<String> scroll) {
        Map<String, QuestDef> m = new LinkedHashMap<>(tutorial());
        // Trame principale, quetes secondaires et evenements : plan de quetes du lore (LoreQuests), donnees dans data/mmorpg/lore.
        // ------------------------------------------------ evolutions de classe (PNJ du groupe « maitre_classes », dans le hub)
        m.put("evolution_1", evolution(obj(obj(q("Épreuve de l'Initié", 25,
                "Le Maître des classes jauge votre potentiel. Prouvez votre force sur le terrain et il éveillera votre première évolution."),
                "level", "niveau", 25), "kill", "*", 60), 1, 8000, 300));
        m.put("evolution_2", req(evolution(obj(obj(obj(q("Épreuve de l'Adepte", 50,
                "Seuls ceux qui ont affronté un seigneur de guerre méritent la deuxième évolution. Terrassez un boss."),
                "level", "niveau", 50), "kill", "*", 150), "kill", "boss", 1), 2, 60000, 1200), "evolution_1"));
        m.put("evolution_3", req(evolution(obj(obj(obj(q("Épreuve du Maître", 75,
                "La troisième évolution exige une maîtrise totale : triomphez de plusieurs boss du royaume."),
                "level", "niveau", 75), "kill", "*", 300), "kill", "boss", 3), 3, 300000, 4000), "evolution_2"));
        m.put("evolution_4", req(evolution(obj(obj(q("Épreuve de la Légende", 100,
                "L'ultime évolution n'appartient qu'aux légendes. Abattez l'Avatar du Néant et revenez auprès du Maître."),
                "level", "niveau", 100), "kill", "avatar_neant", 1), 4, 1500000, 10000), "evolution_3"));
        // Alternative to boss drops: a non-repeatable crafting license at every progression tier (Thorgal).
        String[] targets = {"gobelin", "squelette_maudit", "orc_guerrier", "spectre_givre", "chevalier_neant"};
        for (int tier = 1; tier <= com.mmorpg.crafting.ForgeProgression.TIERS; tier++) {
            int level = (tier - 1) * 20 + 1;
            QuestDef license = by(reward(obj(q("Le savoir du forgeron " + tier, level,
                    "Aidez les artisans : éliminez " + (tier * 10) + " monstres pour recevoir le parchemin d’une pièce d’armure. Il débloque uniquement cette recette."),
                    "kill", targets[tier - 1], tier * 10), tier * 200L, tier * 25L,
                    scroll.apply(tier), 1), QuestNpcPlan.GROUP_FORGE);
            license.requires.add(tier > 1 ? "parchemin_forge_" + (tier - 1) : "spawn_premiere_chasse");
            m.put("parchemin_forge_" + tier, license);
        }
        m.putAll(QuestNpcPlan.discoveryDefinitions());
        m.putAll(classQuests());
        m.putAll(LoreQuests.all());
        m.values().forEach(d -> d.revision = REVISION);
        return m;
    }

    // ------------------------------------------------ quetes de classe (Maitre des classes), niveaux 5 a 100
    public static final int[] CLASS_LEVELS = {5, 15, 25, 35, 45, 55, 65, 75, 85, 95, 100};
    private static final String[] CLASS_IDS = {"guerrier", "mage", "archer", "tank"};
    private static final String[][] CLASS_TITLES = {
            {"Premier sang", "La discipline de l'acier", "Le poing de fer", "Gardien de la brèche", "Le cri de guerre", "Briseur de lignes",
                    "Seigneur du champ de bataille", "Fléau des titans", "La lame sans pitié", "Champion des champions", "Légende de la guerre"},
            {"L'étincelle", "Le grimoire d'écorce", "Souffle des éléments", "Tisseur de sorts", "L'œil de la tempête", "Maître des flux",
                    "Archimage en devenir", "Brisure du voile", "L'arcane interdite", "Gardien du savoir", "Légende de la magie"},
            {"Première flèche", "L'œil du faucon", "Pas de loup", "Tir à l'aveugle", "Chasseur de crépuscule", "Le trait fantôme",
                    "Ombre des cimes", "Pluie de lames", "La corde tendue", "Tireur d'élite", "Légende de l'arc"},
            {"Le premier rempart", "Serment de lumière", "Le bouclier d'aube", "Marteau de justice", "Mur vivant", "Lumière inflexible",
                    "Sentinelle des cités", "Bastion des anges", "L'aube indomptée", "Garde du royaume", "Légende du paladin"}};
    private static final String[] CLASS_VOICE = {
            "Le Maître des classes juge votre bras : ", "Le Maître des classes teste votre savoir : ",
            "Le Maître des classes éprouve votre précision : ", "Le Maître des classes met votre rempart à l'épreuve : "};

    /** Une chaine de onze epreuves par classe, du niveau 5 au niveau 100 ; chaque epreuve exige la precedente et se rend au Maitre des classes. */
    public static Map<String, QuestDef> classQuests() {
        Map<String, QuestDef> m = new LinkedHashMap<>();
        String[] mobs = {"gobelin", "loup_sombre", "squelette_maudit", "orc_guerrier", "elementaire_feu", "spectre_givre", "golem_cristal"};
        int[] counts = {12, 15, 25, 25, 25, 30, 20};
        for (int c = 0; c < CLASS_IDS.length; c++) {
            for (int i = 0; i < CLASS_LEVELS.length; i++) {
                int lv = CLASS_LEVELS[i];
                QuestDef d = q(CLASS_TITLES[c][i], lv, CLASS_VOICE[c] + switch (i) {
                    case 0, 1, 2, 3, 4, 5, 6 -> "prouvez votre valeur sur le terrain.";
                    case 7 -> "affrontez les boss du royaume.";
                    case 8 -> "une grande campagne vous attend.";
                    case 9 -> "seuls les plus grands boss vous attendent.";
                    default -> "atteignez le sommet de votre art et inscrivez votre nom dans la légende.";
                });
                if (i < mobs.length) obj(d, "kill", mobs[i], counts[i]);
                else if (i == 7) { obj(d, "kill", "boss", 2); obj(d, "kill", "*", 100); }
                else if (i == 8) { obj(d, "kill", "*", 200); obj(d, "kill", "boss", 3); }
                else if (i == 9) obj(d, "kill", "boss", 5);
                else { obj(d, "level", "niveau", 100); obj(d, "kill", "boss", 8); }
                d.rewards.xpPercentOfLevel = Math.min(120, 40 + i * 8);
                d.rewards.gold = lv * 40L;
                d.rewards.items.add(new QuestDef.ItemReward("mmorpg:pierre_amelioration" + (i >= 6 ? "_sup" : ""), i / 3 + 1));
                if (i == 3 || i == 7) d.rewards.items.add(new QuestDef.ItemReward("mmorpg:elixir_experience", 1));
                if (i == CLASS_LEVELS.length - 1) {
                    d.rewards.items.add(new QuestDef.ItemReward("mmorpg:fragment_divin", 1));
                    d.rewards.items.add(new QuestDef.ItemReward("mmorpg:cle_quete", 2));
                }
                d.giver = "maitre_classes";
                d.classes.add(CLASS_IDS[c]);
                if (i > 0) d.requires.add("classe_" + CLASS_IDS[c] + "_" + (i - 1));
                m.put("classe_" + CLASS_IDS[c] + "_" + i, d);
            }
        }
        return m;
    }

    /** Quete d'evolution : proposee par le Maitre des classes, recompense = palier d'evolution + cle de quete. */
    private static QuestDef evolution(QuestDef d, int tier, long xp, long gold) {
        reward(d, xp, gold, "cle_quete", 1);
        d.rewards.evolution = tier;
        d.giver = "maitre_classes";
        return d;
    }
}
