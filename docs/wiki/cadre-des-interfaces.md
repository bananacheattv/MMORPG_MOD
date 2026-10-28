# Cadre commun des interfaces

Toutes les interfaces du mod (classe `MmoScreen`) ont le même cadre que l'inventaire élargi : `UiFrame.width/height` = `min(fenêtre − 12, 900)` × `min(fenêtre − 12, 430)`.

- Chaque écran déclare sa hauteur de conception (`designHeight()`, 272 par défaut) ; le contenu est mis à l'échelle (`uiScale`) pour que le cadre garde la taille de l'inventaire. La souris est convertie par `MmoScreenMouseMixin`.
- `compact()` : petite fenêtre de dialogue qui garde sa taille (ex. prêt d'arène, réglage de téléporteur).
- `backdrop()` : illustration d'ambiance du pack d'art (`textures/gui/illustrations/<nom>.png`), peinte discrètement derrière le contenu.
- Inventaire : Attributs à gauche, inventaire au centre, statistiques à droite.
- QA : `-Pshowcase=ui_frames` capture chaque écran ; `-Dmmorpg.noblur=true` désactive le flou (rendu logiciel).
