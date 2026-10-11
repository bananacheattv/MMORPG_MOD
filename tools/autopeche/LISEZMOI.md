# Auto-Peche Minecraft

1. `lancer.bat` (Windows) — ou `pip install -r requirements.txt` puis `python autopeche.py`.
2. `python gen_texture_pack.py` → copier `AutoPeche_Detection.zip` dans `.minecraft/resourcepacks`, l'activer en haut de la liste.
   Bouchon **magenta**, bulles/éclaboussures **jaunes**, eau sombre (`--eau-claire` pour garder l'eau d'origine).
3. Dans Minecraft : canne en main, visez l'eau. Au démarrage le bot se **calibre seul** (lance/ramène la ligne, trouve le bouchon et sa couleur) — ne bougez pas la caméra.
4. **F8** = démarrer / arrêter.

Détection : enfoncement du bouchon + densité de bulles dans un rayon autour + disparition du bouchon,
fusionnés en un score ; la traînée de bulles qui se rapproche déclenche l'état « APPROCHE ».
Réglages, pipette de couleurs, graphe des signaux et journal dans l'interface.
