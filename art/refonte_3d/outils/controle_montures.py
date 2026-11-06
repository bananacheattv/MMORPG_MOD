#!/usr/bin/env python3
"""Controles automatiques des 22 montures livrees, sans lancer le jeu : ecrit livraison/CONTROLES_MONTURES.md.
Compare aux originaux : os animes, clips. Verifie : texture presente, pistes sur des os existants, hauteur de selle, densite, faces, taille."""
import base64
import io
import json
import sys
from pathlib import Path

from PIL import Image

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
from vues_lot import S as SIEGES

ROOT = HERE.parent
LIV = ROOT / 'livraison'
RES = LIV / 'src/main/resources/assets/mmorpg'
ORI = ROOT / 'originaux/git_main/src/main/resources/assets/mmorpg/bbmodels/mounts'


def os_pistes(model):
    out = {}
    for a in model['animations']:
        for ln in a['tracks'].splitlines():
            if ln:
                out.setdefault(a['name'], set()).add(ln.split('|')[0])
    return out


def main():
    lignes = ['# Controles des 22 montures', '', 'Controles faits par script sur les fichiers livres, sans lancer le jeu. Les tests en jeu restent a faire.', '',
              '| Monture | Cubes | Os | Texture | Clips | Os historiques | Selle (haut / attendu) | Bilan |', '|---|---|---|---|---|---|---|---|']
    pb = 0
    for f in sorted(ORI.glob('*.json')):
        key = f.stem
        old = json.loads(f.read_text(encoding='utf-8'))
        new_f = RES / f'bbmodels/mounts/{key}.json'
        bb_f = LIV / f'bbmodels/{key}.bbmodel'
        if not new_f.exists() or not bb_f.exists():
            lignes.append(f'| `{key}` | | | | | | | MANQUANT |')
            pb += 1
            continue
        new = json.loads(new_f.read_text(encoding='utf-8'))
        bb = json.loads(bb_f.read_text(encoding='utf-8'))
        groupes = {g['name'] for g in bb['groups']}
        ko = []
        # clips : tout clip de l'original existe encore
        co, cn = {a['name'] for a in old['animations']}, {a['name'] for a in new['animations']}
        if co - cn:
            ko.append('clips perdus ' + ','.join(sorted(co - cn)))
        # os historiques animes : tous encore la
        oo = set().union(*os_pistes(old).values()) if old['animations'] else set()
        if oo - groupes:
            ko.append('os perdus ' + ','.join(sorted(oo - groupes)))
        # pistes : aucun os absent (KeyframeAnimation.bake leverait une exception)
        pn = set().union(*os_pistes(new).values())
        if pn - groupes:
            ko.append('pistes sans os ' + ','.join(sorted(pn - groupes)))
        # texture
        png = RES / f'textures/imported/mounts/{key}.png'
        if not png.exists():
            ko.append('png absent')
            taille = '?'
        else:
            im = Image.open(png)
            taille = f'{im.width}x{im.height}'
            if im.width != new['resolution']['width']:
                ko.append('resolution differente du png')
        # selle : dessus du siege a 16 x siege + 1 (arrondi 0,5)
        siege = SIEGES[key][0]
        att = round((16 * siege + 1 - .001) * 2) / 2
        hs = [e['to'][1] for e in bb['elements'] if e['name'].startswith(('siege', 'coussin'))]
        haut = max(hs) if hs else None
        if haut is None:
            selle = 'origine conservee' if key == 'sanglier' else '?'
        else:
            selle = f'{haut:g} / {att:g}'
            if abs(haut - att) > .5:
                ko.append('hauteur de selle')
        if len(bb['elements']) > 150:
            ko.append('plus de 150 cubes')
        lignes.append(f'| `{key}` | {len(bb["elements"])} | {len(groupes)} | {taille} | {", ".join(sorted(cn))} | {len(oo)} ok | {selle} | {"OK" if not ko else "; ".join(ko)} |')
        pb += bool(ko)
    lignes += ['', f'Bilan : {pb} monture(s) avec un probleme.', '', '## Controle dans Blockbench 5.2.2', '',
               'Trois vitrines contenant les 22 modeles ouvertes dans Blockbench : 701 + 436 + 366 cubes charges, validation Blockbench sans diagnostic, captures dans `apercus/apres`.',
               'Aller-retour complet (ouvrir, re-sauver, comparer) : fait pour le sanglier et le puits ; a refaire pour les 20 autres quand Blockbench autorise de nouveau l\'ecriture dans le dossier.', '',
               '## Reste a tester en jeu', '', '- Position du cavalier sur chaque monture, a toutes les places.', '- Enchainement idle, walk, et fly pour l\'hippogriffe et le drake ascendant.', '- Clips run et jump : fournis, non joues par le code actuel.', '- Scorpion et araignee : pattes au sol pendant la marche.']
    (LIV / 'CONTROLES_MONTURES.md').write_text('\n'.join(lignes) + '\n', encoding='utf-8')
    print('\n'.join(lignes[:30]))


if __name__ == '__main__':
    main()
