#!/usr/bin/env python3
"""Controle aller-retour : le .bbmodel re-sauve par Blockbench doit redonner exactement le meme modele pour le jeu que le fichier construit.
    python controle_roundtrip.py <id> <fichier_resauve.bbmodel>
"""
import json
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from aeloria3d import check_runtime, export_runtime

ROOT = Path(__file__).resolve().parents[1]
key, rt = sys.argv[1], Path(sys.argv[2])
a = json.loads((ROOT / 'livraison/bbmodels' / f'{key}.bbmodel').read_text(encoding='utf-8'))
b = json.loads(rt.read_text(encoding='utf-8'))
ma, pa = export_runtime(a)
mb, pb = export_runtime(b)
errs = check_runtime(mb)


def norm(m):
    def walk(n):
        out = {k: v for k, v in n.items() if k != 'children'}
        for k in ('from', 'to', 'origin', 'rotation'):
            if k in out:
                out[k] = [round(float(x), 3) for x in out[k]]
        if 'faces' in out:
            out['faces'] = {f: {'uv': [round(float(x), 3) for x in v['uv']]} for f, v in out['faces'].items()}
        if 'children' in n:
            out['children'] = [walk(c) for c in n['children']]
        return out
    tracks = {}
    for an in m['animations']:
        lines = {}
        for l in an['tracks'].split('\n'):
            if l:
                bone, ch, keys = l.split('|')
                lines[bone + '|' + ch] = [[round(float(x), 3) for x in k.split(',')[:4]] for k in keys.split(';')]
        tracks[an['name']] = (round(float(an['length']), 4), an['loop'], lines)
    return [walk(p) for p in m['parts']], tracks


pa_n, ta = norm(ma)
pb_n, tb = norm(mb)
same_geo = pa_n == pb_n
same_tex = pa == pb
same_anim = ta == tb
print(f'{key} : geometrie identique {same_geo}, texture identique {same_tex}, animations identiques {same_anim}, erreurs de chargement {errs}')
print('meta re-sauve :', b['meta'], 'elements', len(b['elements']), 'groupes', len(b.get('groups', [])), 'animations', [x['name'] for x in b.get('animations', [])])
if not same_geo:
    def diff(x, y, path=''):
        if isinstance(x, dict) and isinstance(y, dict):
            for k in set(x) | set(y):
                if x.get(k) != y.get(k):
                    diff(x.get(k), y.get(k), path + '/' + str(k))
        elif isinstance(x, list) and isinstance(y, list) and len(x) == len(y) and x and isinstance(x[0], (dict, list)):
            for i, (p, q) in enumerate(zip(x, y)):
                if p != q:
                    diff(p, q, path + f'[{i}]')
        else:
            print('  diff', path, str(x)[:100], str(y)[:100])
    diff(pa_n, pb_n)
if not same_anim:
    for n in ta:
        if ta[n] != tb.get(n):
            print('  anim differe', n, ta[n][:2], tb.get(n, (0, 0))[:2])
            la, lb = ta[n][2], tb[n][2]
            for k in la:
                if la[k] != lb.get(k):
                    print('    ', k, la[k][:2], (lb.get(k) or [])[:2])
                    break
