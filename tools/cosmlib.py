"""Bibliotheque de generation de cosmetiques 3D (modeles JSON + texture palette) au format ImportedModel.

Coordonnees Blockbench : x droite, y haut, z arriere (le devant du joueur est -z). Unite = 1 pixel (16 = 1 bloc).
Texture : palette 64x64 de pastilles 2x2 ; chaque face pointe sur la pastille de sa couleur (aplat), l'ombrage
leger haut/bas est integre. Les animations sont ecrites en convention Blockbench puis converties comme tools/import_bbmodels.py.
"""
import json
import math
import pathlib
import uuid as _uuid

from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources/assets/mmorpg'
OUT_ART = ROOT / 'art/cosmetics3d'


def rgb(c):
    if isinstance(c, str):
        c = c.lstrip('#')
        return (int(c[0:2], 16), int(c[2:4], 16), int(c[4:6], 16))
    return tuple(c[:3])


def shade(c, k):
    r, g, b = rgb(c)
    return (max(0, min(255, int(r * k))), max(0, min(255, int(g * k))), max(0, min(255, int(b * k))))


def mix(a, b, t):
    a, b = rgb(a), rgb(b)
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


class Model:
    def __init__(self, key):
        self.key = key
        self.roots = []
        self.palette = {}
        self.anims = []
        self.n = 0

    # ---------- texture ----------
    def uv(self, color, alpha=255):
        k = rgb(color) + (alpha,)
        if k not in self.palette:
            self.palette[k] = len(self.palette)
            assert len(self.palette) <= 1024, 'palette pleine'
        i = self.palette[k]
        x, y = (i % 32) * 2, (i // 32) * 2
        return [x + .5, y + .5, x + 1.5, y + 1.5]

    def texture(self):
        im = Image.new('RGBA', (64, 64), (0, 0, 0, 0))
        for k, i in self.palette.items():
            x, y = (i % 32) * 2, (i // 32) * 2
            for dx in range(2):
                for dy in range(2):
                    im.putpixel((x + dx, y + dy), k)
        return im

    # ---------- noeuds ----------
    def _id(self, label):
        self.n += 1
        return str(_uuid.uuid5(_uuid.NAMESPACE_URL, f'aeloria-cosm/{self.key}/{self.n}/{label}'))

    def group(self, parent, origin=(0, 0, 0), rot=(0, 0, 0), name=''):
        g = dict(uuid=self._id(name), origin=list(map(float, origin)), rotation=list(map(float, rot)), children=[], _name=name)
        (self.roots if parent is None else parent['children']).append(g)
        return g

    def cube(self, parent, a, b, color, top=None, bottom=None, faces=None, rot=None, pivot=None, alpha=255, flat=False):
        """Boite entre les coins a et b. color: aplat ; top/bottom : teintes optionnelles ; faces: {north:.., ...} pour surcharger."""
        fr = [min(a[i], b[i]) for i in range(3)]
        to = [max(a[i], b[i]) for i in range(3)]
        for i in range(3):
            if to[i] - fr[i] < 0.02:
                to[i] = fr[i] + 0.02
        base = rgb(color)
        cols = {
            'up': top if top is not None else (base if flat else shade(base, 1.12)),
            'down': bottom if bottom is not None else (base if flat else shade(base, .72)),
            'north': base, 'south': shade(base, .94) if not flat else base,
            'east': shade(base, .9) if not flat else base, 'west': shade(base, .9) if not flat else base,
        }
        if faces:
            cols.update(faces)
        piv = pivot if pivot is not None else [(fr[i] + to[i]) / 2 for i in range(3)]
        node = dict(uuid=self._id('c'), **{'from': [round(v, 4) for v in fr], 'to': [round(v, 4) for v in to]},
                    origin=[round(float(v), 4) for v in piv],
                    faces={d: dict(uv=self.uv(c, alpha), texture=0) for d, c in cols.items()})
        if rot:
            node['rotation'] = list(map(float, rot))
        (self.roots if parent is None else parent['children']).append(node)
        return node

    def octagon(self, parent, cx, y0, cz, r, h, color, **kw):
        """Disque octogonal de demi-largeur r (apothème) : deux boites dont une tournee de 45 degres."""
        self.cube(parent, (cx - r, y0, cz - r), (cx + r, y0 + h, cz + r), color, **kw)
        self.cube(parent, (cx - r, y0 + .01, cz - r), (cx + r, y0 + h - .01, cz + r), color, rot=(0, 45, 0), pivot=(cx, y0, cz), **kw)

    # ---------- animations ----------
    def anim(self, name, length, tracks, loop=True):
        """tracks: liste de (node, canal 'r'|'p'|'s', [(t,x,y,z),...]) en convention Blockbench."""
        lines = []
        for node, ch, frames in tracks:
            seg = []
            for (t, x, y, z) in frames:
                if ch == 'r':
                    x, y = -x, -y
                if ch == 'p':
                    x = -x
                seg.append(','.join(str(round(v, 4)) for v in (t, x, y, z)) + ',c')
            lines.append(node['uuid'] + '|' + ch + '|' + ';'.join(seg))
        self.anims.append(dict(name=name, length=length, loop=loop, tracks='\n'.join(lines)))

    @staticmethod
    def wave(length, f, steps=16):
        """f(phase01) -> (x,y,z) ; echantillonne une boucle fermee."""
        return [(length * i / steps, *f(i / steps)) for i in range(steps + 1)]

    def save(self):
        def clean(n):
            n = {k: v for k, v in n.items() if not k.startswith('_')}
            if 'children' in n:
                n['children'] = [clean(c) for c in n['children']]
            return n
        data = dict(resolution=dict(width=64, height=64), parts=[clean(r) for r in self.roots], animations=self.anims)
        p = RES / f'bbmodels/cosmetics/{self.key}.json'
        p.write_text(json.dumps(data, ensure_ascii=False, separators=(',', ':')) + '\n', encoding='utf-8')
        t = RES / f'textures/imported/cosmetics/{self.key}.png'
        self.texture().save(t)
        OUT_ART.mkdir(parents=True, exist_ok=True)
        return p


def sw(period_phase, amp, f=math.sin):
    return amp * f(2 * math.pi * period_phase)


BUILD = {}
CATS = {}


def model(key, cat):
    def deco(fn):
        BUILD[key] = fn
        CATS[key] = cat
        return fn
    return deco
