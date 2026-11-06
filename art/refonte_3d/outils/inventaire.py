#!/usr/bin/env python3
"""Inventaire et controle automatique de tous les modeles de montures, de blocs, de mobs et de boss (copie du depot, commit dans originaux/git_main/COMMIT.txt).

Ecrit refonte_3d/INVENTAIRE.md et inventaire.csv. Mesures faites sur les fichiers, sans rien supposer :
cubes, os, faces, taille de texture, densite (texels par unite), etirements, surfaces coplanaires visibles (scintillement), faces absentes exposees, grille.
"""
import csv
import hashlib
import json
import math
import re
import sys
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
GIT = ROOT / 'originaux/git_main'
A = GIT / 'src/main/resources/assets/mmorpg'
LOCAL = ROOT / 'originaux/local_non_pousse/bbmodels'
LIV = ROOT / 'livraison'
sys.path.insert(0, str(Path(__file__).parent))
from aeloria3d import euler


def leafs(nodes, out, bones):
    for n in nodes:
        if 'children' in n:
            bones.append(n['uuid'])
            leafs(n['children'], out, bones)
        else:
            out.append(n)


def analyse_runtime(data, tex_size=None):
    cubes, bones = [], []
    leafs(data['parts'], cubes, bones)
    W, H = data['resolution']['width'], data['resolution']['height']
    n = len(cubes)
    lo = np.array([c['from'] for c in cubes], float)
    hi = np.array([c['to'] for c in cubes], float)
    rot = np.array([c.get('rotation', [0, 0, 0]) for c in cubes], float)
    org = np.array([c.get('origin', [0, 0, 0]) for c in cubes], float)
    rotated = np.any(np.abs(rot) > 1e-6, axis=1)
    dens_u, dens_v, faces = [], [], 0
    uv_issues = 0
    missing = 0
    AX = {'north': (0, 1), 'south': (0, 1), 'east': (2, 1), 'west': (2, 1), 'up': (0, 2), 'down': (0, 2)}
    size = hi - lo
    for i, c in enumerate(cubes):
        fs = c.get('faces', {})
        for f, (au, av) in AX.items():
            if f not in fs or fs[f].get('texture') is None:
                missing += 1
                continue
            faces += 1
            u0, v0, u1, v1 = fs[f]['uv']
            if u0 == u1 or v0 == v1 or min(u0, u1) < 0 or min(v0, v1) < 0 or max(u0, u1) > W or max(v0, v1) > H:
                uv_issues += 1
                continue
            su, sv = size[i][au], size[i][av]
            if su > 1e-6:
                dens_u.append(abs(u1 - u0) / su)
            if sv > 1e-6:
                dens_v.append(abs(v1 - v0) / sv)
    du, dv = np.array(dens_u or [0.]), np.array(dens_v or [0.])
    alld = np.concatenate([du, dv])
    # grille : part des coordonnees hors de la grille d'une demi-unite
    coords = np.concatenate([lo.ravel(), hi.ravel()])
    off = np.abs(coords * 2 - np.rint(coords * 2)) > 1e-3
    # surfaces coplanaires visibles
    groups = {}
    AXIS = {'north': (2, 0), 'south': (2, 1), 'east': (0, 1), 'west': (0, 0), 'up': (1, 1), 'down': (1, 0)}
    idx_plain = [i for i in range(n) if not rotated[i]]

    def covered(p, excl):
        for j in idx_plain:
            if j in excl:
                continue
            if np.all(p > lo[j] + 1e-3) and np.all(p < hi[j] - 1e-3):
                return True
        return False
    flicker = 0
    for f, (ax, side) in AXIS.items():
        by = {}
        for i in idx_plain:
            fs = cubes[i].get('faces', {})
            if f not in fs or fs[f].get('texture') is None:
                continue
            v = (hi if side else lo)[i][ax]
            by.setdefault(round(float(v), 3), []).append(i)
        o = [k for k in (0, 1, 2) if k != ax]
        for v, ids in by.items():
            if len(ids) < 2:
                continue
            for a in range(len(ids)):
                for b in range(a + 1, len(ids)):
                    i, j = ids[a], ids[b]
                    x0 = max(lo[i][o[0]], lo[j][o[0]])
                    x1 = min(hi[i][o[0]], hi[j][o[0]])
                    y0 = max(lo[i][o[1]], lo[j][o[1]])
                    y1 = min(hi[i][o[1]], hi[j][o[1]])
                    if x1 - x0 > 1e-3 and y1 - y0 > 1e-3:
                        p = np.zeros(3)
                        p[o[0]], p[o[1]] = (x0 + x1) / 2, (y0 + y1) / 2
                        p[ax] = v + (.05 if side else -.05)
                        if not covered(p, {i, j}):
                            flicker += 1
    # faces absentes exposees (trou visible)
    holes = 0
    NORM = {'north': (2, -1), 'south': (2, 1), 'east': (0, 1), 'west': (0, -1), 'up': (1, 1), 'down': (1, -1)}
    for i in idx_plain:
        fs = cubes[i].get('faces', {})
        for f, (ax, s) in NORM.items():
            if f in fs and fs[f].get('texture') is not None:
                continue
            p = (lo[i] + hi[i]) / 2
            p[ax] = (hi[i][ax] if s > 0 else lo[i][ax]) + .05 * s
            if f == 'down' and abs(lo[i][1]) < 1e-3:
                continue
            if not covered(p, {i}):
                holes += 1
    bbox_lo, bbox_hi = lo.min(0), hi.max(0)
    return {'cubes': n, 'os': len(bones), 'noms_os': bones, 'faces': faces, 'res': f'{W}x{H}', 'dens_med': float(np.median(alld)), 'dens_min': float(alld.min()), 'dens_max': float(alld.max()),
            'hors_grille': float(off.mean()), 'scintille': flicker, 'trous': holes, 'uv_ko': uv_issues, 'bbox': [bbox_lo.tolist(), bbox_hi.tolist()], 'cubes_tournes': int(rotated.sum()),
            'anims': [(a['name'], a['length']) for a in data.get('animations', [])]}


def geom_hash(data):
    c, b = [], []
    leafs(data['parts'], c, b)
    s = json.dumps([[x['from'], x['to'], x.get('rotation')] for x in c], sort_keys=True)
    return hashlib.md5(s.encode()).hexdigest()[:10]


def mount_enum():
    p = Path(__file__).resolve().parents[2] / 'MMORPG_MOD'
    import subprocess
    txt = subprocess.run(['git', '-C', str(p), 'show', 'origin/main:src/main/java/com/mmorpg/mount/MountType.java'], capture_output=True, text=True, encoding='utf-8').stdout
    out = {}
    for m in re.finditer(r'[A-Z_]+\("([a-z_]+)",\s*"([^"]+)",\s*Rarity\.([A-Z_]+),\s*([\d.]+)f,\s*([\d.]+)f,\s*([.\d]+)f,\s*(true|false)((?:,\s*-?[.\d]+f?)*)\)', txt):
        seats = [float(v.strip().rstrip('f')) for v in m.group(8).split(',') if v.strip()]
        out[m.group(1)] = {'label': m.group(2), 'rarete': m.group(3), 'largeur': float(m.group(4)), 'hauteur': float(m.group(5)), 'siege': float(m.group(6)), 'vole': m.group(7) == 'true',
                           'sieges': max(1, len(seats) // 2), 'sieges_xz': seats}
    return out


def decor_famille(k):
    if k.startswith('deco_cat_'):
        return 'cathedrale (deco_cat_)'
    if k.startswith('decob_'):
        return 'citadelle Blockbench (decob_)'
    if k.startswith('deco_'):
        return 'citadelle (deco_)'
    return 'decor anime'


def statut(key):
    for f in (LIV / 'bbmodels' / f'{key}.bbmodel',):
        if f.exists():
            return 'VALIDE' if key in ('sanglier', 'deco_puits') else 'REFAIT, a valider'
    return 'a refaire'


def etat_bloc(s, g):
    if g.startswith('Structures'):
        return 'garde (deja 16 texels par bloc, fait d apres les images du proprietaire)'
    if s == 'panneau_quetes':
        return 'modele d origine garde, a la demande du proprietaire ; face sans texture corrigee'
    if g.startswith('Autres'):
        return 'particule seule, rien a refaire'
    return 'REFAIT, a valider'


def section_mobs(rows):
    """Mobs et boss : mesures sur les fichiers livres, comparees aux originaux."""
    sys.path.insert(0, str(Path(__file__).parent / 'modeles'))
    from mobs import TOUS
    from mobs_classic import CLASSIC
    from mobs_yggdrasil import YGGDRASIL
    md = []
    groupes = (('Mobs de zones et importes', [k for k in TOUS if k not in CLASSIC and k not in YGGDRASIL], 'mobs'), ('Bestiaire classique', list(CLASSIC), 'classic'), ('Gardiens d Yggdrasil, cristal d arene, drone', list(YGGDRASIL), 'yggdrasil'))
    for titre, ids, kind in groupes:
        md += [f'## {titre} ({len(ids)})', '', '| Id | Cubes avant | Cubes apres | Os | Texture avant | Texture apres | Lueurs | Clips | Etat |', '|---|---:|---:|---:|---|---|---:|---|---|']
        for k in ids:
            o = json.loads((A / f'bbmodels/{kind}/{k}.json').read_text(encoding='utf-8'))
            f = LIV / f'src/main/resources/assets/mmorpg/bbmodels/{kind}/{k}.json'
            d = json.loads(f.read_text(encoding='utf-8'))
            c0, b0, c1, b1 = [], [], [], []
            leafs(o['parts'], c0, b0)
            leafs(d['parts'], c1, b1)
            cl = ', '.join(f"{a['name']} {a['length']:g}s" for a in d['animations'])
            e_ = 'REFAIT, a valider'
            md.append(f"| `{k}` | {len(c0)} | {len(c1)} | {len(b1)} | {o['resolution']['width']}x{o['resolution']['height']} | {d['resolution']['width']}x{d['resolution']['height']} | {sum(1 for c in c1 if c.get('emissive'))} | {cl} | {e_} |")
            rows.append(['mob', k, titre, 'ImportedModel', len(c1), len(b1), '', d['resolution']['width'], '', '', '', cl, e_])
        md.append('')
    return md


def section_boss(rows):
    md = ['## Boss (5)', '', 'Format Modded Entity, box UV, classes Java `<Boss>Model` et `<Boss>Anims`.', '', '| Id | Etat |', '|---|---|']
    for k in ('roi_gobelin', 'liche_ancienne', 'ignis', 'titan_glace', 'avatar_neant'):
        f = LIV / f'bbmodels/{k}.bbmodel'
        e_ = 'REFAIT, a valider' if f.exists() else 'a refaire'
        md.append(f'| `{k}` | {e_} |')
        rows.append(['boss', k, 'boss', 'Modded Entity', '', '', '', '', '', '', '', '', e_])
    md += ['', '## Dossiers non traites a ce jour', '', 'PNJ, coffres, cosmetiques et familiers, hors de la demande (montures, blocs, mobs, boss) : `bbmodels/guides` (15), `bbmodels/chests` (2), `bbmodels/cosmetics` (11), `art/familiers_blockbench`.', '']
    return md


def main():
    rows = []
    enum = mount_enum()
    md = ['# Inventaire des montures, blocs, mobs et boss', '',
          f'Copie du depot : commit `{(GIT / "COMMIT.txt").read_text().strip()[:7]}`. Mesures automatiques (`outils/inventaire.py`).', '',
          '**Legende.** Densite : texels par unite de modele (mediane, min-max). « Scintille » : paires de faces coplanaires qui se recouvrent et restent visibles. « Trous » : faces sans texture mais exposees.', '']
    # ----------------------------------------------------------------- montures
    md += ['## Montures (22)', '', '| Monture | Id | Rarete | Sieges | Hauteur selle attendue | Cubes | Os | Texture | Densite | Scintille | Trous | Animations | Etat |', '|---|---|---|---|---|---:|---:|---|---|---:|---:|---|---|']
    hashes = {}
    for key, e in enum.items():
        d = json.loads((A / f'bbmodels/mounts/{key}.json').read_text(encoding='utf-8'))
        r = analyse_runtime(d)
        png = A / f'textures/imported/mounts/{key}.png'
        h = geom_hash(d)
        hashes.setdefault(h, []).append(key)
        tex = r['res'] + ('' if png.exists() else ' **PNG ABSENT**')
        anim = ', '.join(f'{n} {l:g}s' for n, l in r['anims'])
        seat = f'{16 * e["siege"] + 1:.2f} u'
        loc = LOCAL / f'{key}.bbmodel'
        note = ''
        if loc.exists():
            ld = json.loads(loc.read_text(encoding='utf-8'))
            note = f' (local non pousse : {len(ld["elements"])} cubes, {ld["resolution"]["width"]} px)'
        md.append(f'| {e["label"]} | `{key}` | {e["rarete"]} | {e["sieges"]} | {seat} | {r["cubes"]}{note} | {r["os"]} | {tex} | {r["dens_med"]:.1f} ({r["dens_min"]:.1f}-{r["dens_max"]:.1f}) | {r["scintille"]} | {r["trous"]} | {anim} | {statut(key)} |')
        rows.append(['monture', key, e['label'], 'ImportedModel + PNG', r['cubes'], r['os'], r['faces'], r['res'], f'{r["dens_med"]:.2f}', r['scintille'], r['trous'], anim, statut(key)])
    dup = [v for v in hashes.values() if len(v) > 1]
    md += ['', f'Geometries strictement identiques : {dup if dup else "aucune"}.', '']
    # ----------------------------------------------------------------- decors
    fam = {}
    for p in sorted((A / 'bbmodels/props').glob('*.json')):
        d = json.loads(p.read_text(encoding='utf-8'))
        r = analyse_runtime(d)
        fam.setdefault(decor_famille(p.stem), []).append((p.stem, r))
    total_cubes = 0
    for name, items in fam.items():
        md += [f'## Blocs 3D : {name} ({len(items)})', '', '| Id | Cubes | Emprise (blocs) | Hauteur | Texture | Densite | Hors grille | Scintille | Trous | Etat |', '|---|---:|---|---:|---|---|---:|---:|---:|---|']
        for key, r in items:
            total_cubes += r['cubes']
            lo, hi = r['bbox']
            w, dd, h = (hi[0] - lo[0]) / 16, (hi[2] - lo[2]) / 16, hi[1] / 16
            md.append(f'| `{key}` | {r["cubes"]} | {w:.1f} x {dd:.1f} | {h:.1f} | {r["res"]} | {r["dens_med"]:.1f} ({r["dens_min"]:.1f}-{r["dens_max"]:.1f}) | {r["hors_grille"]:.0%} | {r["scintille"]} | {r["trous"]} | {statut(key)} |')
            rows.append(['bloc 3D', key, name, 'ImportedModel + atlas', r['cubes'], r['os'], r['faces'], r['res'], f'{r["dens_med"]:.2f}', r['scintille'], r['trous'], '', statut(key)])
        md.append('')
    # ----------------------------------------------------------------- blocs JSON
    states = sorted(p.stem for p in (A / 'blockstates').glob('*.json'))
    deco_ids = {k for items in fam.values() for k, _ in items}
    others = [s for s in states if s not in deco_ids]

    def models_of(state):
        j = json.loads((A / f'blockstates/{state}.json').read_text(encoding='utf-8'))
        out = set()

        def walk(o):
            if isinstance(o, dict):
                if isinstance(o.get('model'), str):
                    out.add(o['model'])
                for v in o.values():
                    walk(v)
            elif isinstance(o, list):
                for v in o:
                    walk(v)
        walk(j)
        return sorted(out), j
    STRUCT = {'forge_arcanique', 'presse_compression', 'banque_aeloria', 'hdv_aeloria', 'quetes_hebdo', 'boite_aux_lettres', 'teleporteur', 'autel_invocation'}
    groups = {'Structures fonctionnelles animees (Modded Entity, modele Java + StructureRenderer)': [], 'Caisses (JSON Java + CrateRenderer)': [], 'Lucky Blocks (JSON Java)': [], 'Classements (JSON Java + rendu)': [],
              'Accessoires de decor (JSON Java Block)': [], 'Cultures (croix)': [], 'Minerais et materiaux (cube, dalle, escalier)': [], 'Autres blocs': []}
    PROPS = {'chaise_aeloria', 'table_aeloria', 'armoire_aeloria', 'feu_de_camp', 'buche_camp', 'jarre_ancienne', 'pot_decore', 'panneau_feuille', 'viande_deco', 'potion_etoilee', 'panneau_quetes'}
    for s in others:
        ms, j = models_of(s)
        elems = 0
        for m in ms:
            if m.startswith('mmorpg:'):
                f = A / f'models/{m.split(":", 1)[1]}.json'
                if f.exists():
                    elems = max(elems, len(json.loads(f.read_text(encoding='utf-8')).get('elements', [])))
        row = (s, len(ms), elems, list(j.keys())[0])
        if s in STRUCT:
            groups['Structures fonctionnelles animees (Modded Entity, modele Java + StructureRenderer)'].append(row)
        elif s.startswith('caisse_'):
            groups['Caisses (JSON Java + CrateRenderer)'].append(row)
        elif s.startswith('lucky_'):
            groups['Lucky Blocks (JSON Java)'].append(row)
        elif s.startswith('classement_'):
            groups['Classements (JSON Java + rendu)'].append(row)
        elif s in PROPS:
            groups['Accessoires de decor (JSON Java Block)'].append(row)
        elif s.startswith('culture_'):
            groups['Cultures (croix)'].append(row)
        elif s.startswith(('minerai_', 'marbre', 'pierre_', 'tuile_', 'cristal_', 'bois_', 'or_', 'roche_', 'fer_', 'colonne_', 'dalle_', 'veine_')):
            groups['Minerais et materiaux (cube, dalle, escalier)'].append(row)
        else:
            groups['Autres blocs'].append(row)
    for g, items in groups.items():
        md += [f'## {g} ({len(items)})', '', '| Bloc | Modeles | Elements max | Etat |', '|---|---:|---:|---|']
        for s, nm, el, kind in items:
            e_ = etat_bloc(s, g)
            md.append(f'| `{s}` | {nm} | {el} | {e_} |')
            rows.append(['bloc JSON', s, g, 'blockstate + modele', el, '', '', '', '', '', '', '', e_])
        md.append('')
    md += section_mobs(rows)
    md += section_boss(rows)
    md += ['## Totaux', '', f'- Montures : {len(enum)}', f'- Blocs 3D en ImportedModel : {sum(len(v) for v in fam.values())} ({total_cubes} cubes rendus par bloc-entite)', f'- Autres blocs (blockstates) : {len(others)}', f'- Total a suivre : {len(enum) + sum(len(v) for v in fam.values()) + len(others)}', '']
    (ROOT / 'INVENTAIRE.md').write_text('\n'.join(md), encoding='utf-8')
    with (ROOT / 'inventaire.csv').open('w', newline='', encoding='utf-8-sig') as f:
        w = csv.writer(f, delimiter=';')
        w.writerow(['type', 'id', 'famille', 'format', 'cubes', 'os', 'faces', 'texture', 'densite', 'scintille', 'trous', 'animations', 'etat'])
        w.writerows(rows)
    # resume console
    allr = [r for items in fam.values() for _, r in items]
    print('decors', len(allr), 'avec scintillement', sum(1 for r in allr if r['scintille']), 'paires', sum(r['scintille'] for r in allr), 'trous', sum(r['trous'] for r in allr), 'hors grille>0', sum(1 for r in allr if r['hors_grille'] > 0))
    print('densite decors med', np.median([r['dens_med'] for r in allr]), 'min', min(r['dens_min'] for r in allr), 'max', max(r['dens_max'] for r in allr), 'cubes max', max(r['cubes'] for r in allr))
    ms = []
    for key in enum:
        d = json.loads((A / f'bbmodels/mounts/{key}.json').read_text(encoding='utf-8'))
        ms.append((key, analyse_runtime(d)))
    print('montures scintille', [(k, r['scintille']) for k, r in ms if r['scintille']][:30])
    print('montures trous', sum(r['trous'] for _, r in ms), 'densite', [round(r['dens_med'], 1) for _, r in ms], 'dupes', dup)
    print('autres', {g: len(v) for g, v in groups.items()})


if __name__ == '__main__':
    main()
