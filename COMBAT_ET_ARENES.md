# Combats et arènes

Les camps des arènes sont protégés par deux yeux mécaniques 3D chacun. Un ennemi présent dans la zone rouge pendant trois secondes continues est éliminé et renvoyé dans son camp, sans perte d’or ni d’expérience. La barre rouge indique le temps restant ; sortir de la zone annule le verrouillage. Les alliés sont ignorés. Les drones disparaissent à la fin du match et leurs chunks sont libérés.

Les arènes mesurent désormais 109 × 81 blocs. Les camps comportent une porte fortifiée, des tours, des abris aux couleurs de l’équipe et des réserves. Le périmètre dangereux est marqué en rouge au sol.

Le cristal reste blanc pendant sa capture. Il faut 50 coups valides, avec au moins deux secondes entre deux coups pour toute la partie. L’équipe du cinquantième coup gagne la capture. Les coups alliés sur un cristal déjà possédé ne le neutralisent pas. Les fragments orbitaux gardent leur animation pendant son déplacement.

Les boss de guilde ont une difficulté de groupe fixe, y compris en solo. Les phases accélèrent les animations et enrichissent les attaques : salves à distance, tirs multiples puis éruptions annoncées au sol. Les monstres peuvent infliger une fracture d’armure, un épuisement arcanique ou une blessure réduisant les soins, accompagnés d’effets 3D.

L’or ramassé rejoint directement le portefeuille même si l’inventaire est plein ; les restrictions de butin privé et d’épreuve sont conservées. Les embuscades et vagues de donjon affichent les ennemis restants dans une barre dédiée.

`/portail donjon` crée un portail vers le menu des donjons. Les destinations directes avec arguments restent disponibles. L’éditeur de spawn propose également ce portail.

Les éditeurs de PNJ proposent les dix apparences des guides et maîtres de classe en plus des quatre apparences historiques. Les armures personnalisées sur les porte-armures suivent les six poses du mannequin.

Validation locale : `./gradlew runServer -PnetworkChecks`, puis contrôler `build/reports/network-checks.txt`. Test manuel administrateur : `/aeloriatest arene feu`, `/aeloriatest equipe rouge|bleu`, `/aeloriatest stop`. Le mode créatif est ignoré par les drones ; passer en survie pour leur test.
