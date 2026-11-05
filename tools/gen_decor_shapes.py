"""Collisions des decors 3D : un jeu de boites par bloc de l'emprise du modele.

    python tools/gen_decor_shapes.py

Lit assets/mmorpg/bbmodels/props/deco*.json (cubes des modeles), voxelise a 2 pixels, fusionne les voxels en boites
et ecrit data/mmorpg/decor_shapes.json : { id : "cx,cy,cz:x0 y0 z0 x1 y1 z1|...;..." }.
Cellule (0,0,0) = bloc pose ; modele tourne vers le nord ; pixels 0..16 par bloc.
"""
import json
import math
import sys
from pathlib import Path

import numpy as np

sys.path.insert(0, str(Path(__file__).resolve().parent))
import bbmount_lib as L

ROOT = Path(__file__).resolve().parents[1]
PROPS = ROOT / 'src/main/resources/assets/mmorpg/bbmodels/props'
OUT = ROOT / 'src/main/resources/data/mmorpg/decor_shapes.json'
BB_SCALE = 1.0
STEP = 2.0
N = int(16 / STEP)
MAX_XZ, MAX_Y = 5, 12


def cubes(node, out):
    if 'children' in node:
        for c in node['children']:
            cubes(c, out)
    else:
        out.append(node)


def aabb(c):
    lo, hi = np.array(c['from'], dtype=float), np.array(c['to'], dtype=float)
    rot = c.get('rotation') or [0, 0, 0]
    if any(rot):
        o = np.array(c['origin'], dtype=float)
        R = L.euler_matrix(rot)
        pts = np.array([[x, y, z] for x in (lo[0], hi[0]) for y in (lo[1], hi[1]) for z in (lo[2], hi[2])])
        pts = (pts - o) @ R.T + o
        return pts.min(0), pts.max(0)
    return lo, hi


def greedy(occ):
    """Fusionne un tableau d'occupation N*N*N en boites (indices x, y, z)."""
    used = np.zeros_like(occ)
    out = []
    for y in range(N):
        for z in range(N):
            for x in range(N):
                if not occ[x, y, z] or used[x, y, z]:
                    continue
                x1 = x
                while x1 + 1 < N and occ[x1 + 1, y, z] and not used[x1 + 1, y, z]:
                    x1 += 1
                z1 = z
                while z1 + 1 < N and all(occ[xx, y, z1 + 1] and not used[xx, y, z1 + 1] for xx in range(x, x1 + 1)):
                    z1 += 1
                y1 = y
                while y1 + 1 < N and all(occ[xx, y1 + 1, zz] and not used[xx, y1 + 1, zz] for xx in range(x, x1 + 1) for zz in range(z, z1 + 1)):
                    y1 += 1
                used[x:x1 + 1, y:y1 + 1, z:z1 + 1] = True
                out.append((x, y, z, x1 + 1, y1 + 1, z1 + 1))
    return out


def fnum(v):
    return f'{v:g}'


def model_cells(data, scale):
    cs = []
    for p in data['parts']:
        cubes(p, cs)
    boxes = []
    for c in cs:
        lo, hi = aabb(c)
        lo, hi = lo * scale, hi * scale
        if np.any(hi - lo < .6):
            continue
        boxes.append((lo, hi))
    cells = {}
    for lo, hi in boxes:
        lo2 = [lo[0] + 8, max(0.0, lo[1]), lo[2] + 8]
        hi2 = [hi[0] + 8, hi[1], hi[2] + 8]
        for cx in range(math.floor(lo2[0] / 16), math.ceil(hi2[0] / 16)):
            for cy in range(math.floor(lo2[1] / 16), math.ceil(hi2[1] / 16)):
                for cz in range(math.floor(lo2[2] / 16), math.ceil(hi2[2] / 16)):
                    if abs(cx) > MAX_XZ or abs(cz) > MAX_XZ or cy < 0 or cy > MAX_Y:
                        continue
                    occ = cells.setdefault((cx, cy, cz), np.zeros((N, N, N), dtype=bool))
                    c0 = (cx * 16, cy * 16, cz * 16)
                    a = [max(lo2[i], c0[i]) - c0[i] for i in range(3)]
                    b = [min(hi2[i], c0[i] + 16) - c0[i] for i in range(3)]
                    if any(b[i] - a[i] < .5 for i in range(3)):
                        continue
                    i0 = [int(math.floor(a[i] / STEP + .5)) for i in range(3)]
                    i1 = [max(i0[i] + 1, int(math.floor(b[i] / STEP + .5))) for i in range(3)]
                    occ[i0[0]:i1[0], i0[1]:i1[1], i0[2]:i1[2]] = True
    out = {}
    for cell, occ in sorted(cells.items()):
        bs = greedy(occ)
        if bs:
            out[cell] = bs
    return out


def main():
    OUT.parent.mkdir(parents=True, exist_ok=True)
    result = {}
    for f in sorted(PROPS.glob('deco*.json')):
        key = f.stem
        data = json.loads(f.read_text(encoding='utf-8'))
        cells = model_cells(data, BB_SCALE if key.startswith('decob_') else 1.0)
        parts = []
        for (cx, cy, cz), bs in cells.items():
            parts.append(f'{cx},{cy},{cz}:' + '|'.join(' '.join(fnum(v * STEP) for v in b) for b in bs))
        result[key] = ';'.join(parts)
        print(f'{key}: {len(cells)} cellules, {sum(len(b) for b in cells.values())} boites')
    OUT.write_text(json.dumps(result, ensure_ascii=False, separators=(',', ':')), encoding='utf-8', newline='\n')
    print(len(result), 'decors,', OUT.stat().st_size // 1024, 'Ko')


if __name__ == '__main__':
    main()
