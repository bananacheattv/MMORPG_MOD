"""Roi gobelin : l'ancien modele (geometrie et texture d'avant la refonte, art/boss_blockbench/roi_gobelin/ancien) avec les animations actuelles.

Meme squelette (24 os) : seule la partie geometrie de RoiGobelinModel.java et la texture sont remplacees.
A relancer apres construire_boss.py roi_gobelin (puis copie de livraison vers src), qui regenere le modele de la refonte.
Usage : python3 ancien_roi_gobelin.py
"""
import os
import shutil

ICI = os.path.dirname(os.path.abspath(__file__))
RACINE = os.path.normpath(os.path.join(ICI, '..', '..', '..'))
ANCIEN = os.path.join(RACINE, 'art', 'boss_blockbench', 'roi_gobelin', 'ancien')
MARQUE = '    public static AnimationDefinition[] animations() {'
for base in (os.path.join(RACINE, 'src', 'main'), os.path.join(RACINE, 'art', 'refonte_3d', 'livraison', 'src', 'main')):
    java = os.path.join(base, 'java', 'com', 'mmorpg', 'client', 'model', 'boss', 'RoiGobelinModel.java')
    actuel = open(java, encoding='utf-8').read()
    ancien = open(os.path.join(ANCIEN, 'RoiGobelinModel.java.txt'), encoding='utf-8').read()
    sortie = ancien[:ancien.index(MARQUE)] + actuel[actuel.index(MARQUE):]
    open(java, 'w', encoding='utf-8', newline='\n').write(sortie)
    shutil.copyfile(os.path.join(ANCIEN, 'roi_gobelin.png'), os.path.join(base, 'resources', 'assets', 'mmorpg', 'textures', 'entity', 'boss', 'roi_gobelin.png'))
    print('ecrit :', java)
