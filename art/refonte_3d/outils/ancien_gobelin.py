"""Gobelin : l'ancien modele (art/bestiary/classic/gobelin_ancien.json, atlas de materiaux) avec les animations de la refonte.

A relancer apres construire_mob.py, qui regenere le gobelin de la refonte : les pistes des os communs (root, body, head, bras, jambes)
sont reportees sur les os de l'ancien modele ; les os que l'ancien modele n'a pas (machoire, avant-bras, tibias, dague, oreilles) sont ignores.
Usage : python3 ancien_gobelin.py
"""
import json
import os

ICI = os.path.dirname(os.path.abspath(__file__))
RACINE = os.path.normpath(os.path.join(ICI, '..', '..', '..'))
ANCIEN = os.path.join(RACINE, 'art', 'bestiary', 'classic', 'gobelin_ancien.json')
REL = os.path.join('src', 'main', 'resources', 'assets', 'mmorpg', 'bbmodels', 'classic', 'gobelin.json')
CIBLES = [os.path.join(RACINE, REL), os.path.join(RACINE, 'art', 'refonte_3d', 'livraison', REL)]


def os_par_nom(noeuds, out):
    for n in noeuds:
        if 'children' in n:
            out[n['name']] = n['uuid']
            os_par_nom(n['children'], out)
    return out


def main():
    ancien = json.load(open(ANCIEN, encoding='utf-8'))
    noms = os_par_nom(ancien['parts'], {})
    for cible in CIBLES:
        actuel = json.load(open(cible, encoding='utf-8'))
        if actuel.get('texture') == ancien['texture']:
            print('deja converti :', cible)
            continue
        anims = []
        for a in actuel['animations']:
            pistes = []
            for ligne in a['tracks'].split('\n'):
                nom, reste = ligne.split('|', 1)
                if nom in noms:
                    pistes.append(noms[nom] + '|' + reste)
            anims.append({**a, 'tracks': '\n'.join(pistes)})
        sortie = {**ancien, 'animations': anims}
        json.dump(sortie, open(cible, 'w', encoding='utf-8'), ensure_ascii=False, separators=(',', ':'))
        print('ecrit :', cible, [a['name'] for a in anims])


if __name__ == '__main__':
    main()
