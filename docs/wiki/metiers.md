# Métiers (Mineur, Fermier, Pêcheur)

Code : `job/Job.java` (données et formules, sans dépendance Minecraft), `job/JobSystem.java` (XP, récompenses, plats), `server/JobEvents.java` (vitesse de minage), `fishing/` (pêche, voir [Pêche](peche.md)), écrans `client/screen/JobsScreen.java` (3 cartes) et `JobDetailScreen.java` (Aperçu / Paliers / Sources d'XP). Stockage : `PlayerData.jobXp` et `jobClaimed` (profil du joueur, donc base de données avec le profil).

## Progression
- Niveaux 1 à 50, XP de niveau N→N+1 = 60 × 1,13^(N-1) ; 184 k XP au total pour le niveau 50.
- Mineur : Verdite 3, Argentite 7, Ambrasite 14, Glacium 24, Néantite 40 XP par minerai (chaque minerai d'une veine compte).
- Fermier : Racine de Lueur 3, Blé d'Argent 6, Baie Ambrée 11, Fleur de Givre 18, Orchidée du Néant 28 XP par plante mûre.
- Pêcheur : de 12 XP (poisson commun) à 300 XP (mythique), coffres 45 à 280, déchet 5, poisson échappé 2 ; détail dans [Pêche](peche.md).

## Paliers 10 / 20 / 30 / 40 / 50 (donnés une seule fois, à la connexion ou au passage de niveau)
- Titre équipable (bouton Équiper de l'onglet Paliers, affiché comme titre du joueur).
- 10 / 30 / 50 : recette de forge d'un plat exclusif (Forge Arcanique, verrouillée tant que le niveau de métier manque).
- 20 / 40 / 50 : cosmétique ; objets (potions, charmes, élixir, coffre cosmétique) et or à chaque palier.
- Passifs : double minerai / récolte / prise (0,2 % par niveau + 4 % par palier, 30 % max), vitesse de minage jusqu'à ×1,6, prises rares et ingrédients marins, repas du fermier (effets temporaires).

## Plats (9 objets)
Tonique du mineur, Bouillon de roche, Élixir du Seigneur ; Soupe de racine, Tourte aux baies, Banquet des moissons ; Filet grillé, Ragoût des profondeurs, Festin du Léviathan : soin + effets de potion.

## Tests
`JobChecks.run()` (courbe, paliers, octroi idempotent, verrouillage des recettes, sauvegarde) est lancé par `tools/screens/capture.sh metiers` (journal `JOBCHECKS OK`).
