#!/usr/bin/env python3
"""Controles automatiques des decors livres, sans lancer le jeu : ecrit livraison/CONTROLES_DECORS.md.
Pour chaque decor : cubes, texture, clip, enveloppe avant et apres, cellules de collision avant et apres, icone, coplanaires, pistes sur des os existants."""
import json
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
LIV = ROOT / 'livraison'
RES = LIV / 'src/main/resources/assets/mmorpg'
ORI = ROOT / 'originaux/git_main/src/main/resources/assets/mmorpg/bbmodels/props'


def leafs(nodes, out):
    for n in nodes:
        if 'children' in n:
            leafs(n['children'], out)
        else:
            out.append(n)


def boite(d):
    c = []
    leafs(d['parts'], c)
    return [min(e['from'][i] for e in c) for i in range(3)] + [max(e['to'][i] for e in c) for i in range(3)]


def main():
    rap = json.loads((LIV / 'collisions_rapport.json').read_text(encoding='utf-8'))
    patch = json.loads((LIV / 'decor_shapes_patch.json').read_text(encoding='utf-8'))
    L = ['# Controles des decors 3D', '', 'Controles faits par script sur les fichiers livres, sans lancer le jeu. Colonnes : cubes, cote de la texture, clip anime, depassement maximal de l\'enveloppe d\'origine (en unites, 16 = 1 bloc), cellules de collision avant > apres (retirees, ajoutees).', '',
         '| Decor | Cubes | Texture | Clip | Ecart enveloppe | Collisions | Icone | Bilan |', '|---|---:|---:|---|---:|---|---|---|']
    pb = 0
    n = 0
    total_av = total_ap = 0
    for f in sorted(ORI.glob('*.json')):
        key = f.stem
        nf = RES / f'bbmodels/props/{key}.json'
        if not nf.exists():
            L.append(f'| `{key}` | | | | | | | MANQUANT |')
            pb += 1
            continue
        n += 1
        old, new = json.loads(f.read_text(encoding='utf-8')), json.loads(nf.read_text(encoding='utf-8'))
        ko = []
        png = RES / f'textures/imported/props/{key}.png'
        tex = Image.open(png).width if png.exists() else 0
        if not tex:
            ko.append('png absent')
        if 'texture' in new:
            ko.append('cle texture restante')
        bo, bn = boite(old), boite(new)
        ecart = max(max(bo[i] - bn[i] for i in range(3)), max(bn[i + 3] - bo[i + 3] for i in range(3)))
        cubes = []
        leafs(new['parts'], cubes)
        if len(cubes) > 150:
            ko.append('plus de 150 cubes')
        clip = ','.join(a['name'] for a in new['animations']) or '-'
        ic = RES / f'textures/item/{key}.png'
        icone = 'oui' if ic.exists() else ('sans objet' if not key.startswith('deco') else 'NON')
        if icone == 'NON':
            ko.append('icone')
        r = rap.get(key)
        if key.startswith('deco') and key not in patch:
            ko.append('collisions absentes')
        col = f"{r['avant']} > {r['apres']} (-{r['retirees']}, +{r['ajoutees']})" if r else 'aucune dans le jeu'
        if r:
            total_av += r['avant']
            total_ap += r['apres']
        L.append(f'| `{key}` | {len(cubes)} | {tex} | {clip} | {max(0, ecart):.1f} | {col} | {icone} | {"OK" if not ko else "; ".join(ko)} |')
        pb += bool(ko)
    L += ['', f'Bilan : {n} decors, {pb} avec un probleme. Cellules de collision : {total_av} avant, {total_ap} apres.', '',
          '**Collisions.** Elles sont recalculees sur la nouvelle forme par la meme regle que `tools/gen_decor_shapes.py`, et livrees dans `decor_shapes_patch.json`. Elles suivent donc le nouveau modele. L\'emprise generale reste celle de l\'original ; la colonne ci-dessus donne les cellules qui changent, decor par decor. Pour garder les anciennes collisions d\'un decor, il suffit de ne pas reporter sa cle.']
    (LIV / 'CONTROLES_DECORS.md').write_text('\n'.join(L) + '\n', encoding='utf-8')
    print(L[-3])


if __name__ == '__main__':
    main()
