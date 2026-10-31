# Échanges entre joueurs

[Accueil du wiki](README.md) · [Or, HDV, banque et forge](economie.md)

`/trade <joueur>` (ou `/echange`) ouvre un échange sécurisé avec un joueur proche (48 blocs maximum, même serveur).

1. Le joueur reçoit un message avec **[Accepter]** et **[Refuser]** (`/trade accepter`, `/trade refuser`). La demande expire après 60 secondes.
2. Chacun propose jusqu’à **8 objets** de son inventaire (clic sur une case pour proposer ou retirer) et un montant d’**or**.
3. Chacun clique **Valider mon offre**. Toute modification annule les validations des deux joueurs.
4. Quand les deux ont validé, un récapitulatif s’affiche : chacun doit **Confirmer** (le bouton s’active après 2 secondes). **Modifier** revient à l’étape précédente.

Les objets restent dans ton inventaire jusqu’au dernier instant : annuler, fermer l’écran (Échap), se déconnecter, mourir ou s’éloigner annule l’échange sans aucune perte. À la confirmation, le serveur revérifie chaque objet (un objet déplacé ou remplacé fait refuser l’échange), l’or disponible et la place libre dans les deux inventaires, puis échange tout en une seule fois.

Les objets réservés aux caisses ne peuvent pas être échangés.
