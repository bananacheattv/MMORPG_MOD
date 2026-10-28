# Métiers (Mineur, Fermier, Pêcheur)

- Niveaux 1 à 50, XP à chaque minerai / récolte / prise ; paliers 10, 25, 50 (titre, recette, cosmétique, bonus). Code : `job/Job.java`, `job/JobSystem.java`, `server/JobEvents.java`.
- Stockage : `PlayerData.jobXp` (profil du joueur, synchronisé au client).
- Interface : onglet « Métiers » = 3 grandes cartes illustrées (`JobsScreen`) ; un clic ouvre le détail (`JobDetailScreen` : niveau, XP, paliers, bonus). Illustrations : `textures/gui/jobs/*.png` (pack `art/ui-pack`, titre gravé masqué par recadrage).
- Les récompenses de palier sont affichées mais pas encore distribuées en jeu (titres, recettes, cosmétiques).
