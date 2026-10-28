# Expéditions, guildes et boutique d’Aeloria

## Expéditions variables

Chaque nouvelle réservation reçoit une graine persistante. Le menu des donjons annonce le parcours et le modificateur avant l’entrée (survoler une difficulté pour la description complète). Revenir dans la même réservation conserve le parcours ; libérer ou terminer l’expédition prépare une nouvelle variante.

Trois galeries : ouest, est et sinueuse. Les obstacles changent le passage sans contourner les herses ni les épreuves obligatoires. Les embuscades changent d’activation et de difficulté. Les modificateurs sont : renforts (+20 % de vie), brume (ralentissement bref périodique), surcharge (cercle annoncé pendant deux secondes, puis décharge évitable).

Une alcôve runique facultative est ajoutée dans la deuxième salle. Tourner les trois runes 3D selon la gravure ouvre un passage et débloque l’aura arcanique. Deux classes distinctes placées sur les deux socles ouvrent le second sceau et débloquent une traînée d’étoiles. Ces secrets ne bloquent jamais la progression principale et leurs récompenses ne sont données qu’une fois par joueur et expédition.

Les phases des boss de donjon et de guilde peuvent détruire un pont, creuser des poches dans le sol ou inonder une partie de l’arène. Le centre et quatre voies restent praticables. Une annonce et des cercles précèdent le changement. Le journal `boss-terrain.json` conserve les états de blocs complets avant modification ; la mort du boss ou le redémarrage restaure les cellules encore conformes au changement. Aucun terrain du spawn ni du hub n’est concerné.

Les veilleurs champignons, sorciers des roseaux et spectres enchaînés peuvent soigner et protéger un allié blessé. Les archers sylvestres, rôdeurs du vide et bandits rallient jusqu’à quatre alliés existants, sans faire apparaître une armée illimitée. Les gobelins construisent de petites barricades de six secondes dans les instances ; un journal les nettoie même après une interruption.

## Identité des guildes

Onglet **Identité** dans `/guilde` : couleur et emblème (cristal, soleil, lune, arbre, dragon), entrée dans le hall et trois améliorations financées par la banque : 5 000, 10 000 puis 15 000 or. Seuls le chef et les officiers changent l’identité ou achètent une amélioration. Tous les membres entrent dans le hall.

Le hall est dans `mmorpg:guildes`, avec une adresse distincte par guilde, une table de conseil, son emblème, ses trophées gagnés et ses extensions. Bibliothèque, atelier et galerie royale se débloquent successivement. Les membres exclus ne conservent pas l’accès. `/reseau spawn` permet de sortir. Le hall est protégé contre la casse, la pose et les dégâts. Les permissions opérateur restent distinctes.

## Découverte au spawn

Cinq quêtes liées sont proposées par Maëlys, Orin, Lysandre, Érian puis le capitaine Vaël. Elles présentent le guidage, les artisans, l’économie, les expéditions et une première chasse de trois gobelins. Les PNJ se placent uniquement dans des espaces libres du spawn à l’arrivée d’un joueur. Le terrain, les bâtiments et le tutoriel du hub sont conservés.

Les quêtes sont modifiables dans l’éditeur habituel et dans `quests.json`. Le type `visit` utilise un repère explicite et reste vérifié côté serveur, dans la bonne dimension et sur le bon backend. Les récompenses utilisent l’or et l’expérience, jamais les crédits.

## Boutique et grades

Accès : `/boutique`, onglet Boutique du menu MMORPG ou Céliane au spawn. Clés de box, charmes, rune de changement de classe, lots de potions et cinq luckyblocks. Chaque achat demande deux clics. Un inventaire plein conserve la livraison en attente ; le bouton Recevoir mes achats la retente. Les références d’achat et de paiement sont idempotentes.

La configuration partagée est dans `shared/commerce/boutique.json` pour un réseau de serveurs ; en partie locale, dans les données du monde à côté du registre des guildes. Le fichier **boutique** est aussi présent dans l’administration du mod. Les noms, couleurs, prix en crédits, quantités, cosmétiques et capacités d’annonces sont modifiables. Une modification de prix invalide une confirmation devenue ancienne.

Grades provisoires :

| Grade | Prix total en crédits | Annonces HDV | Cosmétique |
|---|---:|---:|---|
| Aelorien | Gratuit | 12 | — |
| Éclaireur | 500 | 16 | Traînée mélodique |
| Gardien | 1 200 | 20 | Halo doré |
| Arcaniste | 2 500 | 24 | Aura arcanique |
| Élu | 5 000 | 30 | Cape royale |

Les montées de grade facturent la différence de prix. Un grade ne multiplie pas les dégâts ni la défense. Le préfixe apparaît dans le chat, le TAB et au-dessus du joueur. Le système utilise les événements et la synchronisation NeoForge déjà présents, sans dépendance à LuckPerms.

Commandes réservées au propriétaire/opérateur de niveau 4 :

- `/grade <joueur> fondateur|administrateur|moderateur|builder|aucun` : badge staff, jamais vendu. `aucun` enlève le badge staff.
- `/grade <joueur> aelorien|eclaireur|gardien|arcaniste|elu` : attribuer un grade joueur.
- `/credits ajouter <UUID> <montant> <reference-unique>` : créditer un compte, même hors ligne ; répéter exactement la référence ne double pas le crédit.

Les badges staff n’accordent pas automatiquement les permissions opérateur. Celles-ci restent contrôlées par les outils d’administration habituels. La rune de classe s’utilise au spawn, hors combat ; elle conserve niveau et expérience, réinitialise compétences et attributs, puis ouvre le choix de classe.

## Futur site : passerelle préparée, paiements désactivés

`paymentBridgeEnabled` vaut `false` et `websiteUrl` est vide par défaut. Aucun paiement réel ni coordonnées bancaires ne sont traités par le mod. Les packs de crédits sont configurables ; le prix en argent et le prestataire de paiement seront définis avec le site.

Quand le backend web sera prêt, il devra vérifier le paiement auprès du prestataire avant de signer la livraison. Activer la passerelle sur le serveur principal et définir la variable d’environnement `AELORIA_CREDIT_SECRET` (au moins 32 caractères). Redémarrer pour appliquer l’activation ou le port.

- Adresse locale : `POST http://127.0.0.1:8099/credits`, exclusivement sur loopback.
- Corps JSON UTF-8 : `{"player":"UUID","pack":"decouverte","payment":"identifiant-du-paiement","timestamp":1793000000}`.
- En-tête `X-Aeloria-Signature` : HMAC-SHA256 hexadécimal du corps exact, signé avec le secret.
- Tolérance d’horodatage : cinq minutes. Le serveur calcule les crédits depuis son pack, jamais depuis un montant envoyé par le navigateur.
- Référence de paiement réutilisée : aucun double crédit ; un changement de joueur ou de montant pour la même référence est refusé.

Le navigateur ne doit jamais connaître le secret ni appeler directement cette adresse. En cas de délai dépassé, le backend web peut réessayer la même référence en signant un nouvel horodatage. Sauvegarder les fichiers de coordination, leurs copies `.bak`, les profils partagés et les données des mondes ensemble.

## Cinq luckyblocks et migration

Céleste, Sylvestre, Infernal, Givre et Néant : modèles 3D propres (ailes, branches, cornes, cristaux ou fragments flottants), couleurs et tables de butin distinctes. L’interface présente le reliquaire, les rouleaux animés, toutes les chances paginées et la récompense sécurisée.

L’ancien `mmorpg:lucky_block` reste uniquement comme identifiant de compatibilité pour charger les sauvegardes. Il n’apparaît plus dans les catalogues ni les recettes ; les anciens exemplaires en inventaire et coffre de l’Ender deviennent des Célestes, et casser un ancien bloc posé rend un Céleste. Les anciennes entrées de butin sont migrées. Les tables des cinq nouveaux blocs restent éditables via l’administration des butins.

## Validation

`./gradlew build runServer -PnetworkChecks`, puis vérifier `build/reports/network-checks.txt` et les marqueurs `CREDIT_STORE`, `EXPEDITIONS`, `NETWORK_FORGE`. Les captures de développement `expansion_ui` et `expansion_world` sont opt-in et ne s’exécutent jamais sur les serveurs publiés.


## Refonte graphique Aeloria

Les primitives communes dessinent désormais panneaux, cartes, boutons, pastilles et barres arrondis, violets translucides avec accents or. Le flou natif respecte les options graphiques. Les menus principal et Échap utilisent le logo Aeloria fourni ; le bouton de jeu reste une connexion directe au serveur configuré. Le HUD conserve ses commandes et son éditeur. Les dix personnages originaux reçoivent des détails de visage, de cheveux, de costumes et une animation de tête. Sources reproductibles : `tools/generate-hub-guides.cjs`.

Cinématique version 4 : approche centrée puis traversée de la grande arche sud, entre z=34 et z=37, à y=8. Les anciennes configurations sont sauvegardées avant migration.

Illustration originale : `src/main/resources/assets/mmorpg/textures/gui/brand/citadel_twilight.png`, créée avec l’outil intégré image_gen. Prompt : fond 16:9 fantasy MMORPG, citadelle flottante ivoire et toitures violettes sur la droite, immense cristal améthyste, lanternes dorées, cascades et montagnes au crépuscule étoilé ; rendu 3D peint, tiers gauche calme, sans texte, logo, bouton, science-fiction ni marque tierce. Logo fourni conservé séparément.

Police : Nunito SemiBold, instance statique de la police variable officielle Google Fonts (https://github.com/google/fonts/tree/main/ofl/nunito), sous SIL Open Font License 1.1. Licence embarquée dans assets/mmorpg/font/OFL-Nunito.txt. Rendu TrueType suréchantillonné 3×, accents français et repli Minecraft pour les symboles. La police commune et les anciens titres Aeloria utilisent le même rendu lissé.
