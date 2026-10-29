# Aeloria 0.0 — Guildes, Yggdrasil et arènes

La version publique du jeu reste **0.0**. Le passage à **0.1**, pour l’ouverture de la bêta, est une décision du propriétaire. Le launcher affiche la version du jeu séparément de Minecraft et de sa propre version de mise à jour. Le fichier du mod est `aeloria-0.0.0.jar` ; l’identifiant interne `mmorpg` reste stable pour conserver les mondes, inventaires et personnages existants.

## Guildes

Le menu principal du personnage propose **Guilde** ; `/guilde` ouvre la même interface. Une guilde commence au niveau 1, peut accueillir 50 membres et atteint au maximum le niveau 10.

- **Guilde** : nom, devise, niveau, XP, effectif et dissolution avec confirmation du nom exact. Le chef doit vider la banque et transférer la direction avant de quitter une guilde conservée.
- **Membres** : invitations aux joueurs connectés du réseau, acceptation des invitations, chef, officiers et membres ; promotion, rétrogradation, exclusion et transfert de direction.
- **Banque** : tous les membres peuvent déposer de l’or. Seul le chef peut retirer. Les officiers peuvent dépenser le coût d’évolution de la guilde. Aucun objet n’est stocké dans cette banque d’or.
- **Yggdrasil** : arbre à dix gardiens, état des victoires, entrée dans l’épreuve et déblocage du niveau suivant.
- **Buffs** : activations temporaires pour tous les membres, actualisées sur tous les serveurs.
- **Journal** : les 60 dernières opérations importantes, dont les mouvements de banque.

L’XP de guilde provient des gains d’XP de ses membres, à raison d’au moins un point et d’environ 10 % de l’XP de base gagnée. Un personnage de niveau 100 contribue encore. La guilde a besoin de `1 000 × niveau actuel²` XP pour évoluer. Chaque passage du niveau 1 au niveau 10 exige **le gardien du palier vaincu + l’XP requise + 10 000 or de banque**. Les neuf évolutions coûtent donc 90 000 or au total. Le dixième gardien est le défi final du niveau 10 ; il ne crée pas de niveau 11.

Chaque buff coûte **1 000 or de la banque de guilde**, dure **une heure réelle** et concerne **tous les membres de la guilde**, y compris ceux qui se connectent pendant sa durée. Le chef et les officiers peuvent activer un buff. Une activation déjà en cours ne peut pas être facturée une seconde fois. Les activations sont partagées entre serveurs, persistent lors d’un redémarrage et expirent aussi pendant une déconnexion.

Vitalité se débloque au niveau 1 (+2 % de PV par niveau), Puissance au niveau 3 (+1,5 % d’attaque et de magie par niveau), Rempart au niveau 5 (+1 % de défense par niveau) et Savoir au niveau 7 (+1 % d’XP personnelle par niveau). Au niveau 10, leurs effets respectifs sont +20 % PV, +15 % attaque/magie, +10 % défense et +10 % XP, uniquement pendant leurs activations payées.

Le registre partagé est dans `shared/guilds/guilds.json` sur le réseau, avec sauvegarde `.bak` et verrou interprocessus. Les transferts d’or utilisent une intention persistante et un reçu enregistré dans le profil joueur : une opération interrompue reprend sans appliquer deux fois le même mouvement. Un registre corrompu est refusé, jamais remplacé silencieusement par une guilde vide.

## Les dix gardiens originaux

| Palier | Gardien | Silhouette |
| --- | --- | --- |
| 1 → 2 | Kharvoss, Dévoreur des Racines | Scarabée cuirassé à six pattes |
| 2 → 3 | Sylkaris, Faux des Ronces | Mante aux quatre lames |
| 3 → 4 | Orméa, Voile des Cieux | Raie céleste aux ailes immenses |
| 4 → 5 | Thalassyr, Couronne des Profondeurs | Hydre corallienne à trois têtes |
| 5 → 6 | Varkhûn, Anneaux d’Ambre | Mille-pattes cuirassé colossal |
| 6 → 7 | Nyméthis, Oracle Suspendu | Méduse aux tentacules runiques |
| 7 → 8 | Ghorzakk, Bastion Vivant | Crabe forteresse aux pinces jumelles |
| 8 → 9 | Vespera, Mangeuse de Soleils | Papillon d’éclipse à quatre ailes |
| 9 → 10 | Azheryn, Spirale des Mondes | Serpent dressé et couronnes orbitales |
| Final, niveau 10 | Elyndraxx, Cœur de l’Arbre-Monde | Œil annulaire aux branches fractales |

Ce sont dix géométries originales, avec textures et animations, distinctes des cinq boss historiques. Le générateur reproductible est `node tools/generate-yggdrasil.cjs`, les géométries dans `assets/mmorpg/bbmodels/yggdrasil` et la texture de matériaux dans `assets/mmorpg/textures/entity/yggdrasil/material_atlas.png`. Les projets Blockbench éditables et le rapport de géométrie sont dans `art/yggdrasil`. Les silhouettes atteignent environ 4 à 14 blocs de haut, avec des ailes, des mâchoires, des pattes et des appendices articulés. Les détails lumineux disposent de leur propre passe de rendu. Les gardiens montent en puissance du niveau de combat 10 au niveau 100 et annoncent leurs attaques spéciales avant impact.

Les épreuves sont instanciées sur le backend **donjons**, dans `mmorpg:competitions`, sans toucher au spawn construit par le propriétaire. Une guilde dispose d’une instance à la fois, jusqu’à huit guildes et vingt membres par instance. Seuls les participants de cette guilde peuvent infliger des dégâts au gardien. L’épreuve dure au maximum vingt minutes ; après victoire, les joueurs disposent d’environ une minute avant le retour. `/guilde sortir` permet de revenir plus tôt. Les essais ne prélèvent pas les 10 000 or : le paiement a lieu au clic d’évolution après la victoire.

## Arènes

Ouvrir **Arène** dans le menu du personnage, ou `/arene`.

- **Arène de feu** : 5 contre 5, pierre noire, basalte, lave, éclairages et grands rochers.
- **Arène des dunes** : 3 contre 3, grès, eau, éclairages et grands rochers.
- Cinq files distinctes pour chaque arène : **1–20, 21–40, 41–60, 61–80 et 81–100**. Une inscription est refusée dans une autre tranche.
- Lorsque tous les emplacements sont remplis, chaque joueur reçoit la fenêtre **Rentrer dans l’arène**. Tous doivent confirmer dans les trente secondes. En cas de refus ou d’absence, le match est annulé et les joueurs prêts sont réinscrits.
- Les équipes sont réparties en équilibrant les niveaux. Les participants doivent être en survie ; le mode créatif et le mode spectateur sont refusés.
- Le cristal possède son propre modèle animé et se déplace autour des rochers. Un coup direct ou une attaque de compétence le capture pour l’équipe rouge ou bleue. Le cristal neutre ne rapporte rien.
- Le cristal contrôlé rapporte **1 point toutes les 2 secondes** ; une capture adverse remet ce délai à zéro. Une élimination adverse rapporte **10 points**. La première équipe à **500 points** gagne.
- Un coup mortel ramène au camp de son équipe, avec restauration de la vie et trois secondes de protection. Cette protection interdit aussi d’attaquer ou de capturer le cristal. Aucune perte d’équipement, de butin, d’XP ou d’or lors de ces réapparitions.
- Le tir allié est bloqué. Les groupes d’aventure ne permettent pas d’éviter les dégâts d’un adversaire d’arène.
- Les déconnexions laissent trente secondes pour revenir. Si une équipe n’a plus de participants, elle perd par forfait. Après trente minutes, le score départage les équipes ; une égalité reste une égalité.
- `/arene quitter` ou le bouton de l’interface quitte la file ou le match. Le retour utilise le point d’entrée du joueur sur le réseau. Une partie active ne survit pas à un redémarrage ; les données de guilde, elles, sont conservées.

La dimension de compétition est protégée contre la construction et les explosions ; les apparitions ordinaires de monstres RPG y sont désactivées. Les arènes sont construites progressivement avec un budget par tick. Une partie par arène et tranche peut fonctionner simultanément ; les autres inscrits attendent la suivante.

## Vérification

`gradlew.bat runServer -PnetworkChecks build` vérifie les permissions, la banque et sa reprise après interruption, les évolutions, les tranches de niveau, les scores et les dix types de gardiens. `gradlew.bat runClient -Pshowcase=social_ui` génère les captures des interfaces et vérifie les trois boutons du menu d’accueil. Les scènes de galerie privées nécessitent les options explicites `visualChecks` et `socialVisual`, absentes des commandes des serveurs de jeu.

## Tests administrateur en solo

Commandes réservées aux opérateurs de niveau 2 minimum, après le choix de classe. Elles fonctionnent depuis les serveurs du réseau et dirigent le joueur vers le serveur donjons. Les permissions sont revérifiées à l’arrivée.

- `/aeloriatest boss 1` à `/aeloriatest boss 10` : affronter le gardien choisi sans guilde, sans coût et sans condition de palier. La victoire ne donne pas de butin ni de progression de guilde. Un coup mortel ramène dans l’instance, sans perte.
- `/aeloriatest arene feu` ou `/aeloriatest arene dunes` : lancer immédiatement une instance solo, indépendante des files publiques, avec le vrai cristal mobile et le score. Le mode créatif est accepté pour visiter.
- `/aeloriatest equipe rouge` ou `/aeloriatest equipe bleu` : changer d’équipe pendant un test ; frapper le cristal pour vérifier la capture dans cette couleur.
- `/aeloriatest stop` : quitter le test et revenir au point de départ. Quitter d’abord un test avant d’en lancer un autre.

En survie, les boss attaquent normalement ; en créatif, ils peuvent être observés. Les délais de préparation correspondent à la construction progressive du décor. Les matchs publics conservent leurs exigences de joueurs et leurs restrictions de niveaux.


## Ajustements bêta 0.0 — 27 octobre

- Inscription réseau depuis le serveur courant : aucun transfert dans une salle d’attente. Feu : 10 inscrits, dunes : 6 ; chaque joueur confirme la proposition avant le transfert collectif.
- Tous les gardiens débloqués sont rejouables. Une victoire déclenche une recharge persistante de 6 heures pour ce gardien et cette guilde. Retour au point de départ après 30 secondes.
- Épreuves d’Yggdrasil dans une grotte fermée : voûte rocheuse, racines géantes, minéraux lumineux et piste de combat dégagée.
- `/aeloriatest boss 1` à `/aeloriatest boss 10`, `/aeloriatest arene feu|dunes` : instances admin solo, sans condition de guilde, niveau, or, recharge ou nombre de joueurs. Aucun gain ni progression. Une activité déjà en cours doit être quittée auparavant. `/aeloriatest stop` ramène au départ.
- `/atp <joueur>` (alias `/tpreseau`) : commande OP de téléportation entre dimensions et serveurs. Le joueur visé doit être connecté. Les changements de serveur utilisent une autorisation temporaire créée côté serveur.
- Régénération passive : 1 % des PV max par cinq secondes, sans régénération alimentaire vanilla. Réapparition PvE automatique au spawn ; les morts d’arène restent dans le camp de l’équipe.
- XP des monstres de donjon divisée par cinq supplémentaires ; quantité moyenne de leurs butins divisée par dix. Les mines et plaines ne sont pas concernées par ces deux réductions.
- U : boutons agrandir/réduire l’élément sélectionné, échelle jusqu’à 300 %. Page précédente / Page suivante : rapprocher/éloigner la caméra à la troisième personne, sans changer la FOV ; touches reconfigurables.
- Forge : détails défilants et pied de panneau fixe pour conserver les boutons de fabrication à l’intérieur.

## Équipement et construction — bêta 0.0

- Paladin avec un marteau : bouclier visuel de classe automatique, cinq apparences selon le niveau. La seconde main ne reçoit pas d’objets manuellement.
- `/tuto` : retrouver le prochain PNJ du tutoriel dans le hub. Les séquences de présentation des classes sont raccourcies.
- `/outilarmure` : outil administrateur pour équiper et poser les porte-armures, déplacer leurs membres, choisir une posture et régler leur position. Également accessible dans l’outil d’aménagement.
- Forge : recettes classées par niveau ; mannequins retirés des consommables ; anciennes haches, pelles, caisse et clé d’aventure retirées de la progression. Les anciens identifiants restent réservés pour charger les sauvegardes.
- Orbe de renaissance : choisir à nouveau sa classe en repartant au niveau 1.
- Outils : quatre runes, cinq rangs chacune, dans Amélioration. Abondance augmente le rendement, Prospection les chances rares de la pioche, Célérité la vitesse et la zone de récolte de la houe, Ténacité économise la durabilité. Chaque rang consomme sa rune et de l’or ; le coût est affiché avant l’amélioration.
- Tables de craft, fours, enclumes et tables d’enchantement vanilla désactivés, y compris la cuisson automatisée.
- Les PNJ personnalisés du marché ne sont plus supprimés par le nettoyage des anciens marchands. Les textes ont une portée accrue ; les PNJ et mannequins ne tombent plus dans le vide.

## Tutoriel incarné — bêta 0.0

Sélène accueille les héros, Kaël explique les expéditions, Borin la forge et les runes, Élyra les guildes et Néria les arènes. Clic droit (ou clic gauche) pour leur parler, puis « Merci, j’ai compris » pour mémoriser l’étape. Les conversations restent accessibles après validation ; `/tuto` indique le prochain guide et le met brièvement en surbrillance si son chunk est chargé.

Les quatre Maîtres de classe et le Gardien de l’épreuve ont également de nouveaux modèles cubiques articulés. Les essais et le choix de classe gardent leur fonctionnement. Le premier tutoriel ne s’ouvre plus automatiquement sous forme de livre après la cinématique : il oriente vers Sélène.

L’ancien bloc téléporteur du hub est retiré automatiquement à son emplacement connu, avec son point de passage. Le reste du décor est conservé ; les portails physiques des expéditions sont inchangés. Aucun changement de numéro de plan et aucune reconstruction du hub ne sont nécessaires.

## Guidage automatique — bêta 0.0

Le tutoriel se suit automatiquement dès l’arrivée dans le hub, sans `/tuto`. Une flèche 3D violette indique le prochain guide et change après validation de sa conversation. La flèche apparaît au-dessus du personnage en troisième personne ; en première personne, elle reste en 3D et apparaît devant la caméra. Elle reste horizontale et pivote vers la cible. F1 la masque également.

Une quête acceptée devient automatiquement la quête suivie. La même flèche violette indique son premier objectif non terminé : repère défini par l’administrateur, cible vivante à proximité, butin accessible, zone de chasse connue ou portail correspondant. Quand la quête est prête à rendre, elle pointe vers son donneur enregistré ; si celui-ci est sur un autre serveur, le guidage commence par le portail de retour. Le suivi choisi dans le journal garde la priorité ; « Ne plus suivre » masque le guidage de quête jusqu’à une nouvelle sélection ou acceptation.

Dans l’éditeur de quêtes, onglet Objectifs, « Repère ici » enregistre la position, la dimension et le serveur actuels pour l’objectif sélectionné ; « Automatique » rétablit la détection normale. Si aucune destination n’est connue, le suivi affiche l’objectif à rechercher sans inventer une direction.

## Chat de guilde et classements

- **Chat de guilde** : `/g <message>` (ou `/guilde chat <message>`) envoie un message à tous les membres connectés, sur tous les serveurs du réseau (registre partagé `guild-chat`, 300 derniers messages, 200 caractères, un message par seconde). Le chef est marqué ★ et les officiers ✦.
- **Classements** : `/classement [niveau|monstres|arene|guilde_niveau|guilde_banque|guilde_gardiens]` affiche le top 10 et ton rang. Les données sont dans le registre partagé `rankings` (joueurs en ligne échantillonnés toutes les 10 s ; victoires et frags d'arène ajoutés à la fin de chaque match classé).
- **Panneaux de classement** : un bloc et un modèle par type (`classement_<type>`, onglet créatif admin), 3 × 2 blocs, qui affichent le top 10 en direct ; clic droit = classement dans le chat. Régénération des modèles et textures : `python3 tools/generate_rankings.py`.
