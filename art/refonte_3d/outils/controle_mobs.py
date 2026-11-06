#!/usr/bin/env python3
"""Controle des mobs livres, sans lancer le jeu. Ecrit livraison/CONTROLES_MOBS.md et renvoie 1 s'il reste un probleme.
Verifie : JSON et PNG presents, plus de cle texture, resolution egale au PNG, moins de 150 cubes, clips d'origine presents, pistes sur des os existants, os exiges par le code, lueur la ou l'original en avait, 16 texels par bloc en jeu, enveloppe proche de l'original."""
import json
import re
import struct
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(HERE / 'modeles'))
from aeloria3d import check_runtime
from mobs import TOUS
from render import Model
import numpy as np

ROOT = HERE.parent
LIV = ROOT / 'livraison'
RES = LIV / 'src/main/resources/assets/mmorpg'
ORI = ROOT / 'originaux/git_main/src/main/resources/assets/mmorpg'
OS_CLASSIC = ('root', 'body', 'head')
HUMAINS = ('gobelin', 'orc_guerrier', 'bandit_arbaletrier', 'squelette_maudit', 'zombie_des_cryptes', 'chevalier_neant', 'spectre_givre', 'golem_cristal')


def echelles():
    src = (ROOT / 'originaux/git_main/src/main/java/com/mmorpg/entity/mob/ImportedMobs.java').read_text(encoding='utf-8')
    out = {}
    for m in re.finditer(r'new Entry\("(\w+)",\s*"[^"]*",\s*\d+,\s*([\d.]+)f,\s*([\d.]+)f,\s*([\d.]+)f,\s*(true|false)\)', src):
        out[m.group(1)] = (float(m.group(2)), float(m.group(3)), float(m.group(4)), m.group(5) == 'true')
    return out


def walk(ns, cubes, bones):
    for n in ns:
        if 'children' in n:
            bones.append(n)
            walk(n['children'], cubes, bones)
        else:
            cubes.append(n)


def env(cubes):
    pts = []
    for c in cubes:
        if 'from' in c:
            pts += [c['from'], c['to']]
        elif 'vertices' in c:
            pts += list(c['vertices'].values())
    return [min(p[i] for p in pts) for i in range(3)], [max(p[i] for p in pts) for i in range(3)]


def clips(d):
    return {a['name'] for a in d['animations']}


def main():
    E = echelles()
    lignes = ['# Contrôles des mobs', '', 'Script `outils/controle_mobs.py`, sans lancer le jeu. Tailles en texels de jeu (16 = 1 bloc).', '',
              '| Mob | Cubes | Texture | Lueurs | Clips | Hauteur avant | Hauteur après | Remarque |', '|---|---:|---:|---:|---|---:|---:|---|']
    pb = 0
    for key, fn in TOUS.items():
        kind = 'classic' if key in ('gobelin', 'orc_guerrier', 'bandit_arbaletrier', 'squelette_maudit', 'zombie_des_cryptes', 'chevalier_neant', 'spectre_givre', 'elementaire_feu', 'golem_cristal', 'loup_sombre', 'araignee_venimeuse') else 'mobs'
        rem = []
        f = RES / f'bbmodels/{kind}/{key}.json'
        png = RES / f'textures/imported/{kind}/{key}.png'
        if not f.exists() or not png.exists():
            print(key, 'FICHIER ABSENT')
            pb += 1
            continue
        d = json.loads(f.read_text(encoding='utf-8'))
        o = json.loads((ORI / f'bbmodels/{kind}/{key}.json').read_text(encoding='utf-8'))
        c, b = [], []
        walk(d['parts'], c, b)
        co, bo = [], []
        walk(o['parts'], co, bo)
        with open(png, 'rb') as h:
            h.read(16)
            W, H = struct.unpack('>II', h.read(8))
        if (W, H) != (d['resolution']['width'], d['resolution']['height']):
            rem.append(f'PNG {W}x{H} différent de la résolution')
        if 'texture' in d:
            rem.append('clé texture restante')
        if len(c) > 150:
            rem.append(f'{len(c)} cubes')
        manq = clips(o) - clips(d)
        if manq:
            rem.append(f'clips manquants {sorted(manq)}')
        errs = check_runtime(d)
        if errs:
            rem.append('; '.join(errs[:3]))
        lumi = sum(1 for x in c if x.get('emissive'))
        lumo = sum(1 for x in co if x.get('emissive'))
        if lumo and not lumi:
            rem.append('aucune lueur')
        noms = {x['name'] for x in b}
        if kind == 'classic':
            for n in OS_CLASSIC:
                if n not in noms:
                    rem.append(f'os {n} absent')
            if key in HUMAINS:
                for n in ('arm_-1', 'arm_1', 'leg_-1', 'leg_1'):
                    if n not in noms:
                        rem.append(f'os {n} absent')
        s = E[key][2] if key in E else (.82 if key == 'gobelin' else 1.0)
        q = np.concatenate([x[0] for x in Model(LIV / 'bbmodels' / f'{key}.bbmodel').quads()], 0)   # coordonnees monde au repos : les os tournes comptent
        lo, hi = q.min(0).tolist(), q.max(0).tolist()
        lo0, hi0 = env(co)
        h1, h0 = (hi[1] - lo[1]) * s, (hi0[1] - lo0[1]) * s
        # 16 texels par bloc en jeu : chaque face a w texels sur w/s unites
        bad = 0
        for x in c:
            a, t = x['from'], x['to']
            for fn_, fa in x['faces'].items():
                u0, v0, u1, v1 = fa['uv']
                ax = {'north': (0, 1), 'south': (0, 1), 'east': (2, 1), 'west': (2, 1), 'up': (0, 2), 'down': (0, 2)}[fn_]
                if abs(abs(u1 - u0) - abs(t[ax[0]] - a[ax[0]]) * s) > .02 or abs(abs(v1 - v0) - abs(t[ax[1]] - a[ax[1]]) * s) > .02:
                    bad += 1
        if bad:
            rem.append(f'{bad} faces hors 16 texels par bloc')
        if lo[1] * s < -12:
            rem.append(f'descend sous le sol ({lo[1] * s:.0f})')
        if key in E and not E[key][3] and lo[1] * s > 3:
            rem.append(f'ne touche pas le sol ({lo[1] * s:.0f})')
        if h1 > h0 * 1.3 or h1 < h0 * .7:
            rem.append('hauteur très différente, à regarder')
        if rem:
            pb += sum(1 for r in rem if 'regarder' not in r)
        lignes.append(f'| `{key}` | {len(c)} | {W} | {lumi} | {", ".join(sorted(clips(d)))} | {h0:.0f} | {h1:.0f} | {"; ".join(rem)} |')
    lignes += ['', f'Modèles : {len(TOUS)}. Problèmes : {pb}.', '']
    (LIV / 'CONTROLES_MOBS.md').write_text('\n'.join(lignes), encoding='utf-8')
    print('\n'.join(l for l in lignes[6:] if l.rstrip().endswith('|') and not l.rstrip().endswith('|  |')))
    print(f'{len(TOUS)} modeles, {pb} problemes')
    return 1 if pb else 0


if __name__ == '__main__':
    sys.exit(main())
