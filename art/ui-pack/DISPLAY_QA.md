# Contrat d’affichage — à valider dans le vrai client

[Retour au lien de travail](README.md)

Les PNG et leurs métadonnées peuvent être vérifiés dans ce pack. **Cela ne valide pas l’interface intégrée.** Claude doit compléter le tableau ci-dessous avec des captures réelles de sa version du mod. Aucun écran ne doit être déclaré terminé uniquement parce que son image existe.

## Matrice de fenêtres et échelles

Tester les tailles physiques920×600,1280×720,1920×1080 et2560×1440, ainsi que le redimensionnement en direct. Pour chacune, vérifier l’échelle GUI automatique et les échelles manuelles1/2/3/4 réellement acceptées par Minecraft, y compris la plus grande acceptée. Consigner la taille logique obtenue ; ne pas prétendre qu’une échelle refusée par le jeu a été testée.

Sur une zone logique trop étroite pour deux colonnes : passer à une colonne ou utiliser un défilement clair. Ne pas simplement réduire la police jusqu’à l’illisibilité. Les dimensions des panneaux et boutons doivent être calculées à partir de l’espace disponible, sans débordement hors écran.

## Critères de réception

- Titres, valeurs et libellés restent lisibles à l’échelle normale ; viser au moins9pixels logiques pour le texte courant, et augmenter lorsque la police l’exige. Pas de microtexte pour faire entrer une longue phrase.
- Aucun visage, objet important, libellé, prix, rang ou bouton n’est caché par un fond. Le voile de contraste est local, pas un bloc opaque qui annule le décor.
- Les images conservent leur ratio. Les bannières peuvent être recadrées sans couper leur sujet ; les portraits et médailles sont contenus sans étirement. Les classes et médailles utilisent leurs vrais rectangles d’atlas.
- Tout le texte variable est en code : noms, stats, soldes, durée, difficulté, récompenses et états. Les trois titres de métiers déjà peints ne doivent pas être doublés.
- Une phrase longue se replie ou se consulte en infobulle ; un nom long peut être abrégé avec une vraie infobulle. Les chiffres importants ne sont jamais tronqués silencieusement.
- Le scroll est borné au contenu et son état reste cohérent après changement de filtre. Les en-têtes et actions essentielles restent accessibles.
- La zone de clic correspond au rectangle dessiné, y compris après déplacement et agrandissement de l’interface. Une case affichée ne doit pas sélectionner sa voisine.
- Survol, sélection, verrouillage et désactivation sont distincts ; clavier/focus/Échap fonctionnent. Le sens ne dépend pas seulement du rouge et du vert.
- Le chargement se termine proprement : monde affiché, erreurs visibles, bilan d’arène livré après la transition. Ne pas remplacer les barres réelles par une progression peinte ou inventée.
- Les données continuent à être contrôlées par le serveur. Ne pas modifier les droitsOP, soldes, débits, livraisons, règles de combat et collections en réhabillant les écrans.

## Jeux de données d’essai

Utiliser des fixtures de QA isolées, sans modifier les profils de production :

- État vide, un élément, liste complète et liste assez longue pour défiler.
- Nom joueur de16caractères, nom de guilde long, titre d’objet long avec accents ; description de2000caractères.
- Solde0 puis grands nombres ; prix/quantités avec plusieurs chiffres, matériau manquant, inventaire plein.
- Images absentes ou inconnues : placeholder lisible et interface encore utilisable, pas de texture magenta ni crash.
- Objet possédé/verrouillé, compétence disponible/indisponible/rangmax, recette accessible ou non.
- Arène : victoire rouge, victoire bleue, égalité, entraînement, sortie anticipée, dix joueurs, noms longs, reprise du dernier bilan.
- Plusieurs destinations et écrans de chargement successifs ; fermeture/réouverture des menus et reconnexion.

## État de validation d’intégration

| Interface | Fenêtres/échelles | Données extrêmes | Clics/clavier/scroll | Capture réelle | État |
|---|---|---|---|---|---|
| Métiers | À remplir | À remplir | À remplir | À joindre | Non intégré par ce pack |
| Donjons | À remplir | À remplir | À remplir | À joindre | Non intégré par ce pack |
| Classes | À remplir | À remplir | À remplir | À joindre | Non intégré par ce pack |
| Quêtes | À remplir | À remplir | À remplir | À joindre | Non intégré par ce pack |
| Montures/familiers | À remplir | À remplir | À remplir | À joindre | Non intégré par ce pack |
| Boutique | À remplir | À remplir | À remplir | À joindre | Non intégré par ce pack |
| Chargements | À remplir | À remplir | À remplir | À joindre | Non intégré par ce pack |
| Guildes | À remplir | À remplir | À remplir | À joindre | Non intégré par ce pack |
| Forge | À remplir | À remplir | À remplir | À joindre | Non intégré par ce pack |
| Bestiaire | À remplir | À remplir | À remplir | À joindre | Non intégré par ce pack |
| Succès | À remplir | À remplir | À remplir | À joindre | Système réel à vérifier |
| Résultats d’arène | À remplir | À remplir | À remplir | À joindre | Non intégré par ce pack |
| HDV | À remplir | À remplir | À remplir | À joindre | Non intégré par ce pack |
| Banque/sacs | À remplir | À remplir | À remplir | À joindre | Non intégré par ce pack |
| Compétences | À remplir | À remplir | À remplir | À joindre | Non intégré par ce pack |
| Fin de donjon | À remplir | À remplir | À remplir | À joindre | Bilan réel à raccorder |
| Fiche d’équipement | À remplir | À remplir | À remplir | À joindre | Non intégré par ce pack |

## Vérification des fichiers du pack

Exécuter `python art/ui-pack/verify_pack.py` depuis la racine du dépôt avec Pillow installé. Ce contrôle vérifie les images, dimensions, SHA, alpha, régions d’atlas et liens documentaires ; il ne remplace pas les essais dans Minecraft.
# Contrôles supplémentaires du complément

Les onze écrans de [COMPLEMENT_INTERFACES.md](COMPLEMENT_INTERFACES.md) sont tous **à intégrer / non vérifiés en jeu**. Tester particulièrement Succès (route incorrecte vers portail), Trade (slots et clics sur toute la largeur), Groupe (skins et PV réels), Quêtes (texte sur parchemin), Cosmétiques (joueur réellement en 3D), Yggdrasil (couronne/racines entières et actions lisibles).

