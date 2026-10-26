# Spawn manuel, WorldEdit et aventures

Le spawn est un monde entièrement vide, sans aucun bloc ni plateforme, dans `mmorpg:spawn`. Le hub des classes reste dans `mmorpg:hub`. Le point de départ est à `0 96 0`. La configuration `spawn.json` utilise `manualBuild: true`, `flatPlatform: false`, `autoNpcs: false` et `autoStations: false` : les constructions manuelles ne sont jamais remplacées par la ville automatique.

## Outil de construction (opérateur)

1. `/spawn`, puis `/gamemode creative`.
2. `/amenagement outil` donne la baguette d'aménagement. `/amenagement` ouvre aussi le menu.
3. Clic droit sur un bloc avec la baguette : la face cliquée désigne l'emplacement. Clic droit dans l'air : bloc visé à huit blocs au maximum, sinon quatre blocs devant soi.
4. Choisir une structure, un coffre, un PNJ ou un portail, puis son orientation. Les structures refusent les emplacements occupés. Les portails demandent cinq blocs de large et cinq de haut sur un sol solide.
5. Onglet Textes : saisir le texte, sa taille et sa hauteur, puis le poser. Le bouton de retrait supprime le texte de cet outil le plus proche à dix blocs au maximum.
6. Le bouton « Définir le spawn ici » ou `/spawn set` enregistre la position exacte et l’orientation pour les arrivées `/spawn`.

Le bouton Annuler et `/amenagement annuler` annulent les vingt dernières opérations de l'outil et des portails, dans l'ordre, pendant la session du serveur. `/undoportail` utilise aussi cet historique. Les modifications ultérieures d'un autre constructeur sont conservées : l'annulation refuse les conflits. Les coffres et structures qui contiennent des objets ne sont pas effacés par une annulation. Les historiques ne survivent pas à un redémarrage ; les constructions, portails et textes, eux, sont sauvegardés.

Avec WorldEdit installé, `//undo` reste l'historique WorldEdit. Utiliser le bouton Annuler pour les opérations de la baguette.

## Panel WorldEdit

Section **WorldEdit** : importer un fichier `.schem`, `.schematic` ou `.nbt` (32 Mo maximum). Les cinq serveurs de jeu partagent `shared/schematics` grâce à des jonctions vers `config/worldedit/schematics`. DB et proxy sont exclus. Les fichiers existants ne sont pas remplacés lors d'un import ; renommer une nouvelle version. Les fichiers sauvegardés en jeu sont immédiatement partagés aussi.

En jeu : `//schem list`, `//schem load nom.schem`, `//paste`, puis `//undo` si nécessaire. Les commandes d'édition restent réservées aux opérateurs.

Installation du complément sur un panel existant : `node tools/panel-worldedit/install.cjs "CHEMIN_DU_PANEL"`, puis redémarrer le panel. Installer WorldEdit sur chaque backend. La version vérifiée avec NeoForge `26.3.0.39-beta` est **WorldEdit 7.4.6-beta-02** ; WorldEdit 7.4.6 final exige NeoForge 26.3.0.83 et ne doit pas être installé sur cette ancienne version de NeoForge. Le complément sauvegarde les sources du panel avant modification.

## Sauvegarde et remplacement de l'ancien spawn

Tous les serveurs de jeu doivent être arrêtés. `tools/Reset-ManualSpawn.ps1 -PanelRoot "CHEMIN_DU_PANEL"` archive uniquement les dimensions `mmorpg:spawn` du principal et du hub, leurs métadonnées de spawn et les configurations concernées dans `backups/manual-spawn-*`. Il conserve intégralement la dimension du hub, les autres mondes, les personnages, les banques et les inventaires. Au redémarrage, les anciens portails, points de passage, zones et entrées du répertoire de PNJ de la dimension de spawn sont retirés. Ne pas relancer ce script sur une construction à conserver.

## Règles d'aventure

- Entrées normales des mines, plaines et donjons : portail obligatoire. Les menus éventuels ne fonctionnent qu'après une traversée, pendant 60 secondes et à huit blocs du portail au maximum. Une permission est consommée à l'entrée. Les commandes de diagnostic réservées aux opérateurs restent des outils d'administration.
- À la sortie, le bilan montre durée, mobs tués, XP, or et objets acquis. Les compteurs sont sauvegardés dans le profil partagé entre serveurs. Les objets consommés pendant la sortie restent comptés parmi les acquisitions.
- Les pets vont chercher les drops de mobs attribués à leur propriétaire, à douze blocs du joueur au maximum. Les drops étrangers et les objets jetés par les joueurs sont ignorés ; les objets qui ne rentrent pas dans le sac restent au sol. L'attribution au tueur empêche aussi de fusionner deux tas de propriétaires différents.
- Aucun objet du namespace `minecraft` dans les drops de mobs ; les inventaires des joueurs morts restent conservés par les règles habituelles.
- XP RPG des mobs divisée par deux ; densité et rythme des apparitions ordinaires multipliés par trois, y compris les vagues des donjons, sans tripler les boss.
- Un cinquième des positions de minerais conservé. Les anciens chunks de mines sont migrés progressivement, une fois chacun ; le terrain et les autres blocs sont conservés. Les repousses respectent les mêmes positions.
- Aucune perte de nourriture dans les mondes. Charge complète des arcs RPG en 0,5 seconde, avec animation accélérée.
- Coffres deux fois plus petits ; collision adaptée et textes plus grands et plus hauts.

## Plans d'équipement

188 parchemins nominatifs débloquent chacun une seule recette d'armure, d'arme ou d'outil. Les boss donnent un plan du palier adapté, en privilégiant la classe du joueur et les recettes inconnues. Les quêtes d'artisan donnent un parchemin d'armure précis. Les doublons ne sont pas consommés. Les anciens parchemins génériques encore présents dans un inventaire s'échangent par clic droit contre un plan nominatif ; les anciens déblocages déjà appris sont conservés par migration.

Validation : `gradlew.bat runServer -PnetworkChecks build` sur le monde de développement uniquement, `node --test tools/panel-worldedit/worldedit.test.cjs`, et `gradlew.bat runClient -Pshowcase=spawn_editor_ui` pour les captures de l'interface.

## Consommation dans le tableau de bord

Chaque carte de service affiche CPU et RAM du processus, ainsi que leur part dans la capacité totale de la machine. Actualisation toutes les quatre secondes. Le CPU est calculé entre deux mesures : 100 % signifie tous les cœurs de la machine occupés. Une première mesure ou une mesure indisponible est affichée explicitement. L’installateur Windows `node tools/panel-metrics/install.cjs "CHEMIN_DU_PANEL"` sauvegarde les sources avant modification ; redémarrer ensuite le panel.
