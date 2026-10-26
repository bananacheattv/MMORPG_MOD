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

Ce sont dix géométries originales, avec textures et animations, distinctes des cinq boss historiques. Le générateur reproductible est `node tools/generate-yggdrasil.cjs`, les géométries dans `assets/mmorpg/bbmodels/yggdrasil` et les textures dans `assets/mmorpg/textures/imported/yggdrasil`. Les grandes boîtes de combat mesurent de 5 à 12 blocs de haut. Les gardiens montent en puissance du niveau de combat 10 au niveau 100 et annoncent leurs attaques spéciales avant impact.

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
