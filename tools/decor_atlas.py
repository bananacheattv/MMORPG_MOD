"""Pose des textures : chaque face d'un cube decor pioche dans la tuile 256 x 256 de sa matiere (atlas partage)."""
from PIL import Image

import bbmount_lib as L
import decor_textures as T

SPAN = 48.0                      # une tuile couvre 48 unites (1 bloc de citadelle)
S = T.N / SPAN
ATLAS_ID = 'mmorpg:textures/imported/props/decor_atlas.png'
ATLAS_REL = '../src/main/resources/assets/mmorpg/textures/imported/props/decor_atlas.png'
_cache = {}


def atlas():
    if 'img' not in _cache:
        _cache['img'] = T.build()
    return _cache['img']


def _split(b):
    """Coupe un cube plus grand qu'une tuile sur la grille de 48 unites (faces internes masquees)."""
    pieces = [b]
    for ax, (lo_face, hi_face) in enumerate((('west', 'east'), ('down', 'up'), ('north', 'south'))):
        out = []
        for p in pieces:
            lo, hi = p['from'][ax], p['to'][ax]
            if hi - lo <= SPAN:
                out.append(p)
                continue
            cuts = [lo]
            k = int(lo // SPAN) + 1
            while k * SPAN < hi - 1e-6:
                if k * SPAN > lo + 1e-6:
                    cuts.append(k * SPAN)
                k += 1
            cuts.append(hi)
            for i in range(len(cuts) - 1):
                q = dict(p)
                q['from'], q['to'] = list(p['from']), list(p['to'])
                q['from'][ax], q['to'][ax] = cuts[i], cuts[i + 1]
                q['hide'] = set(p.get('hide', ()))
                if i > 0:
                    q['hide'].add(lo_face)
                if i < len(cuts) - 2:
                    q['hide'].add(hi_face)
                q['name'] = f"{p['name']}_{ax}{i}"
                out.append(q)
        pieces = out
    return pieces


def _span(a, size, flip=False):
    start = (-a if flip else a) % SPAN
    if start + size > SPAN:
        start = max(0.0, SPAN - size)
    return start


def _uv(tile, a, wu, c, hv, flip_v):
    """a, c = coordonnees de depart sur la face, wu, hv = dimensions ; retourne un rectangle de l'atlas en pixels entiers."""
    ox, oy = T.slots()[tile]
    wu, hv = min(wu, SPAN), min(hv, SPAN)
    u = _span(a, wu)
    v = _span(c, hv, flip_v)
    x0 = int(round(u * S))
    y0 = int(round(v * S))
    x1 = min(T.N, max(x0 + 1, int(round((u + wu) * S))))
    y1 = min(T.N, max(y0 + 1, int(round((v + hv) * S))))
    x0, y0 = min(x0, x1 - 1), min(y0, y1 - 1)
    return [ox + x0, oy + y0, ox + x1, oy + y1]


def texture(m):
    for g in m.groups.values():
        new = []
        for c in g['children']:
            if 'children' in c:
                new.append(c)
            else:
                new.extend(_split(c))
        g['children'] = new
    boxes = []

    def collect(g):
        for c in g['children']:
            if 'children' in c:
                collect(c)
            else:
                boxes.append(c)
    for g in m.top:
        collect(g)
    m.boxes = boxes
    for b in boxes:
        (x0, y0, z0), (x1, y1, z1) = b['from'], b['to']
        dx, dy, dz = x1 - x0, y1 - y0, z1 - z0
        tile = T.tile_for(b['mat'], (dx, dy, dz))
        faces = {'north': (x0, dx, y1, dy), 'south': (x0, dx, y1, dy), 'east': (z0, dz, y1, dy), 'west': (z0, dz, y1, dy),
                 'up': (x0, dx, z0, dz), 'down': (x0, dx, z0, dz)}
        b['uv'] = {}
        for f, (a, wu, c, hv) in faces.items():
            if f in b.get('hide', ()):
                continue
            b['uv'][f] = _uv(tile, a, wu, c, hv, f not in ('up', 'down'))


def finish(m):
    """Retourne (projet .bbmodel, modele runtime, atlas)."""
    texture(m)
    img = atlas()
    data = L.to_bbmodel(m, Image.new('RGB', (1, 1)), [])
    runtime, _ = L.bbmodel_to_runtime(data)
    w, h = img.size
    runtime['resolution'] = data['resolution'] = {'width': w, 'height': h}
    runtime['texture'] = ATLAS_ID
    t = data['textures'][0]
    t.pop('source', None)
    t.update({'name': 'decor_atlas.png', 'relative_path': ATLAS_REL, 'internal': False, 'width': w, 'height': h, 'uv_width': w, 'uv_height': h})
    return data, runtime, img
