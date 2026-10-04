# -*- coding: utf-8 -*-
"""
Boite a outils commune aux generateurs de modeles Blockbench (format "Java Block/Item") :
construction en cuboides, textures pixel art peintes par programme, rangement des UV,
export .bbmodel + modele d'item Java, et rendu logiciel d'apercus (Pillow uniquement).

Contraintes du format Java Block/Item respectees et verifiees a chaque cube :
  - cuboides uniquement, coordonnees entre -16 et 32 sur chaque axe ;
  - une rotation par cube sur un seul axe ; en mode strict, angle -45 / -22.5 / 0 / 22.5 / 45
    (compatible avec toutes les versions de Minecraft et tous les reglages de Blockbench) ;
    en mode libre (strict=False), angle quelconque : Minecraft 26.3 et Blockbench 5 (version cible 26.3) l'acceptent ;
  - UV par face (pas de box UV), une seule texture carree en puissance de deux ;
  - light_emission 0-15 par cube, aucune rotation de groupe.
"""
import base64
import io
import json
import math
import random
import uuid
import zlib

from PIL import Image, ImageDraw, ImageFilter, ImageFont

D = 2                       # texels par pixel de modele
S2 = math.sqrt(2.0)
ANGLES_OK = (-45.0, -22.5, 0.0, 22.5, 45.0)
FACES = ('north', 'east', 'south', 'west', 'up', 'down')

PAL = {
    'bois_clair': [(112, 74, 42), (84, 54, 30), (140, 98, 58), (62, 40, 22)],
    'bois':       [(78, 52, 38), (56, 37, 28), (102, 70, 50), (40, 26, 20)],
    'cuir':       [(104, 64, 38), (72, 42, 26), (136, 88, 54), (52, 30, 18)],
    'fer':        [(126, 128, 136), (88, 90, 100), (160, 164, 172), (198, 202, 210)],
    'metal':      [(56, 56, 68), (36, 36, 46), (84, 86, 102), (112, 116, 134)],
    'argent':     [(170, 174, 188), (122, 126, 142), (206, 210, 222), (240, 242, 250)],
    'cristal':    [(92, 96, 236), (58, 44, 168), (146, 156, 255), (222, 230, 255)],
    'gemme':      [(140, 70, 230), (86, 36, 160), (186, 128, 255), (236, 214, 255)],
    'pierre':     [(86, 102, 150), (58, 68, 108), (122, 142, 196), (170, 196, 255)],
    'plume':      [(226, 226, 232), (170, 170, 182), (248, 248, 252), (120, 120, 134)],
    'glow':       [(150, 126, 255), (112, 170, 255), (226, 232, 255)],
}
GLYPHES = [
    ['#.#', '##.', '#.#', '##.', '#..'],
    ['#.#', '#.#', '.#.', '.#.', '.#.'],
    ['#..', '##.', '#.#', '##.', '#..'],
    ['.#.', '#.#', '.#.', '.#.', '.#.'],
    ['.#.', '#.#', '.#.', '#.#', '#.#'],
    ['##.', '#.#', '##.', '#.#', '#.#'],
    ['#.#', '.#.', '#.#', '.#.', '#.#'],
]
EMBLEME = ['..#..', '.###.', '##.##', '.###.', '..#..']
# matieres unies : une petite zone de texture etiree sur toute la face (economise la texture)
STRETCH = {'corde': (2, 6), 'corde_runique': (2, 6), 'corde_magique': (2, 6), 'lien': (2, 6)}
GLOWING = {'cristal', 'gemme', 'rune', 'energie', 'lien', 'corde_magique'}


def crc(*key):
    return zlib.crc32(repr(key).encode('utf-8'))


def lerp(c1, c2, t):
    t = max(0.0, min(1.0, t))
    return tuple(int(round(a + (b - a) * t)) for a, b in zip(c1, c2))


def mul(c, f):
    return tuple(max(0, min(255, int(round(v * f)))) for v in c)


# ============================================================================================ geometrie
class Part:
    def __init__(self, name, group, frm, to, mat, rot=None, light=0, faces=None, deco=None):
        self.name, self.group, self.mat = name, group, mat
        self.frm = [round(v, 4) for v in frm]
        self.to = [round(v, 4) for v in to]
        self.rot = rot
        self.light = light
        self.faces = faces
        self.deco = deco or {}
        self.uv = {}


class Model:
    def __init__(self, key, level, titre, strict=True):
        self.key, self.level, self.titre, self.strict = key, level, titre, strict
        self.parts = []
        self.names = set()

    def add(self, p):
        base, n = p.name, 2
        while p.name in self.names:
            p.name = f'{base}_{n}'
            n += 1
        self.names.add(p.name)
        if p.rot is not None:
            ax, ang, org = p.rot
            ang = round(float(ang), 4)
            assert ax in 'xyz' and -45.0 - 1e-6 <= ang <= 45.0 + 1e-6, (p.name, p.rot)
            if self.strict:
                assert any(abs(ang - a) < 1e-6 for a in ANGLES_OK), (self.key, p.name, ang)
            p.rot = None if abs(ang) < 1e-6 else (ax, ang, [round(v, 4) for v in org])
        for v in p.frm + p.to:
            assert -16 <= v <= 32, (self.key, p.name, p.frm, p.to)
        self.parts.append(p)
        return p


def box(m, group, name, x1, y1, z1, x2, y2, z2, mat, **kw):
    return m.add(Part(name, group, [min(x1, x2), min(y1, y2), min(z1, z2)], [max(x1, x2), max(y1, y2), max(z1, z2)], mat, **kw))


def cbox(m, group, name, c, size, mat, **kw):
    return box(m, group, name, c[0] - size[0] / 2, c[1] - size[1] / 2, c[2] - size[2] / 2,
               c[0] + size[0] / 2, c[1] + size[1] / 2, c[2] + size[2] / 2, mat, **kw)


def col(m, group, name, y1, y2, w, mat, cx=8.0, cz=8.0, d=None, **kw):
    d = w if d is None else d
    return box(m, group, name, cx - w / 2, y1, cz - d / 2, cx + w / 2, y2, cz + d / 2, mat, **kw)


def rbox(m, group, name, c, size, angle, mat, axis='z', **kw):
    """Cube centre en c, tourne de 'angle' autour de son centre (axe unique). L'angle est ramene dans [-45, 45]
    en echangeant les dimensions concernees (un cube tourne de 90 deg equivaut a un cube aux cotes permutes)."""
    sx, sy, sz = size
    a = float(angle)
    k = math.floor((a + 45.0) / 90.0)
    a2 = a - 90.0 * k
    if a2 > 45.0 + 1e-9:
        a2 -= 90.0
        k += 1
    if k % 2:
        if axis == 'z':
            sx, sy = sy, sx
        elif axis == 'y':
            sx, sz = sz, sx
        else:
            sy, sz = sz, sy
    rot = (axis, a2, list(c)) if abs(a2) > 1e-9 else None
    return cbox(m, group, name, c, (sx, sy, sz), mat, rot=rot, **kw)


def seg(m, group, name, p0, angle, length, width, depth, mat, cz=8.0, ext=None, **kw):
    """Segment dans le plan de face (XY) partant de p0 selon 'angle' (degres, 0 = +X). Renvoie l'extremite."""
    rad = math.radians(angle)
    p1 = (p0[0] + length * math.cos(rad), p0[1] + length * math.sin(rad))
    e = width * 0.6 if ext is None else ext
    c = ((p0[0] + p1[0]) / 2, (p0[1] + p1[1]) / 2, cz)
    rbox(m, group, name, c, (length + e, width, depth), angle, mat, **kw)
    return p1


def poly(m, group, name, p0, steps, depth, mat, cz=8.0, mirror=False, **kw):
    """Ligne brisee : steps = [(longueur, angle, largeur[, matiere]), ...] ; mirror = symetrie gauche/droite (x -> 16 - x)."""
    p = (16 - p0[0], p0[1]) if mirror else p0
    for i, step in enumerate(steps):
        length, ang, width = step[0], step[1], step[2]
        mt = step[3] if len(step) > 3 else mat
        a = (180.0 - ang) if mirror else ang
        p = seg(m, group, f'{name}_{i + 1}', p, a, length, width, depth, mt, cz=cz, **kw)
    return p


def arc(m, group, name, cx, cy, cz, radius, angles, width, depth, mat, plane='xy', **kw):
    """Anneau / croissant : un segment tangent par angle."""
    chord = 2 * radius * math.sin(math.radians(11.25))
    for i, a in enumerate(angles):
        w = width[i] if isinstance(width, (list, tuple)) else width
        rad = math.radians(a)
        if plane == 'xy':
            c = (cx + radius * math.cos(rad), cy + radius * math.sin(rad), cz)
            rbox(m, group, f'{name}_{i + 1}', c, (chord + w * 0.7, w, depth), a + 90.0, mat, axis='z', **kw)
        else:   # plan horizontal : u = x, v = -z, rotation autour de Y
            c = (cx + radius * math.cos(rad), cy, cz - radius * math.sin(rad))
            rbox(m, group, f'{name}_{i + 1}', c, (chord + w * 0.7, depth, w), a + 90.0, mat, axis='y', **kw)


def crystal(m, group, name, cx, cy, cz, w, h, mat='cristal', light=15, side=True, angle=0.0):
    """Cristal facette : silhouette hexagonale de face (losanges) et, si side, de profil.
    angle : inclinaison du cristal dans le plan de face (le profil n'est ajoute que si angle == 0)."""
    a = w / S2
    hc = max(0.0, h - w)
    t = w * 0.55
    ux, uy = -math.sin(math.radians(angle)), math.cos(math.radians(angle))   # axe du cristal
    for suffix, off in (('haut', hc / 2), ('bas', -hc / 2)):
        c = (cx + ux * off, cy + uy * off, cz)
        rbox(m, group, f'{name}_{suffix}', c, (a, a, t), 45 + angle, mat, light=light)
    if hc > 0:
        rbox(m, group, f'{name}_corps', (cx, cy, cz), (w, hc, t), angle, mat, light=light)
    if side and abs(angle) < 1e-9:
        w2 = w * 0.8
        a2 = w2 / S2
        hc2 = max(0.0, h - w2)
        t2 = w * 0.5
        for suffix, yc in (('profil_haut', cy + hc2 / 2), ('profil_bas', cy - hc2 / 2)):
            cbox(m, group, f'{name}_{suffix}', (cx, yc, cz), (t2, a2, a2), mat, rot=('x', 45, [cx, yc, cz]), light=light)
        if hc2 > 0:
            cbox(m, group, f'{name}_profil_corps', (cx, cy, cz), (t2, hc2, w2), mat, light=light)


def frag(m, group, name, c, s, axis='z', mat='cristal', light=15):
    """Fragment flottant (cube pose sur la pointe)."""
    cbox(m, group, name, c, (s, s, s), mat, rot=(axis, 45, list(c)), light=light)


def plate(m, group, name, side, x1, y1, x2, y2, depth_at, mat, light=15, deco=None, angle=0.0, pivot=None):
    """Plaque plate (epaisseur nulle, une seule face) devant une face : runes, energie, emblemes.
    north/south : rectangle x1..x2 / y1..y2 a z = depth_at ; east/west : rectangle z1..z2 (x1..x2) / y1..y2 a x = depth_at.
    angle/pivot : rotation autour de Z (plaques posees sur une piece inclinee, faces north/south uniquement)."""
    d = {side: deco} if deco else None
    if side in ('north', 'south'):
        rot = ('z', angle, list(pivot)) if angle else None
        return box(m, group, name, x1, y1, depth_at, x2, y2, depth_at, mat, light=light, faces={side}, deco=d, rot=rot)
    return box(m, group, name, depth_at, y1, x1, depth_at, y2, x2, mat, light=light, faces={side}, deco=d)


def col_plates(m, group, name, y1, y2, wcol, inset, mat, sides='NSEW', light=15, deco=None, gap=0.1, cx=8.0, cz=8.0):
    """Plaques sur les cotes d'une colonne centree de largeur wcol."""
    h = wcol / 2
    lo, hi = cx - h + inset, cx + h - inset
    zlo, zhi = cz - h + inset, cz + h - inset
    if 'N' in sides:
        plate(m, group, f'{name}_avant', 'north', lo, y1, hi, y2, cz - h - gap, mat, light, deco)
    if 'S' in sides:
        plate(m, group, f'{name}_arriere', 'south', lo, y1, hi, y2, cz + h + gap, mat, light, deco)
    if 'E' in sides:
        plate(m, group, f'{name}_droite', 'east', zlo, y1, zhi, y2, cx + h + gap, mat, light, deco)
    if 'W' in sides:
        plate(m, group, f'{name}_gauche', 'west', zlo, y1, zhi, y2, cx - h - gap, mat, light, deco)


def diamond_plate(m, group, name, c, size, gem, front_z, back_z, frame='argent', gem_mat='gemme', gem_light=12):
    """Losange (rune d'Eldoria) devant et derriere une piece : cadre metallique + gemme."""
    cx, cy = c
    for side, z0, sgn in (('avant', front_z, -1), ('arriere', back_z, 1)):
        zc = z0 + sgn * 0.3
        cbox(m, group, f'{name}_{side}', (cx, cy, zc), (size, size, 0.6), frame, rot=('z', 45, [cx, cy, zc]),
             deco={'north': 'embleme_argent', 'south': 'embleme_argent'})
        zg = z0 + sgn * 0.75
        cbox(m, group, f'{name}_gemme_{side}', (cx, cy, zg), (gem, gem, 0.3), gem_mat, rot=('z', 45, [cx, cy, zg]), light=gem_light)


# ============================================================================================ textures
def face_dims(p, f):
    sx, sy, sz = (p.to[i] - p.frm[i] for i in range(3))
    return {'north': (sx, sy), 'south': (sx, sy), 'east': (sz, sy), 'west': (sz, sy), 'up': (sx, sz), 'down': (sx, sz)}[f]


def tex_size(v):
    return max(1, int(round(v * D)))


def put(px, x, y, c, a=255):
    px[x, y] = (c[0], c[1], c[2], a)


def paint_base(mat, w, h, side, r, smooth=False):
    img = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    px = img.load()
    if mat in ('bois', 'bois_clair'):
        base, dark, light, deep = PAL[mat]
        if side and smooth:
            for x in range(w):
                for y in range(h):
                    put(px, x, y, mul(base, 1 + r.uniform(-0.04, 0.04)))
        elif side:
            vertical = h >= w
            for i in range(w if vertical else h):
                tone = r.choice([base, base, base, dark, light])
                for j in range(h if vertical else w):
                    if r.random() < 0.14:
                        tone = r.choice([base, base, dark, light])
                    put(px, *((i, j) if vertical else (j, i)), tone)
            if w * h >= 30 and r.random() < 0.7:
                put(px, r.randrange(w), r.randrange(h), deep)
        else:
            cx, cy = (w - 1) / 2, (h - 1) / 2
            for x in range(w):
                for y in range(h):
                    put(px, x, y, light if int(math.hypot(x - cx, y - cy) * 1.3) % 2 == 0 else base)
    elif mat in ('cuir', 'cuir_tresse'):
        base, dark, light, deep = PAL['cuir']
        vertical = h >= w
        for x in range(w):
            for y in range(h):
                a, b = (x, y) if vertical else (y, x)
                if mat == 'cuir':
                    k = (2 * b + a) % 6
                else:
                    k = (2 * b + a) % 6 if (b // 3) % 2 == 0 else (2 * b - a) % 6
                c = deep if k == 0 else dark if k == 1 else light if k == 3 else base
                if r.random() < 0.06:
                    c = mul(c, 1.1)
                put(px, x, y, c)
    elif mat in ('metal', 'argent', 'fer'):
        base, dark, light, hi = PAL[mat]
        for x in range(w):
            for y in range(h):
                put(px, x, y, mul(base, 1 + r.uniform(-0.05, 0.05)))
        if w >= 3:
            for y in range(h):
                put(px, 0, y, light)
                put(px, w - 1, y, dark)
        if h >= 3:
            for x in range(w):
                put(px, x, 0, hi if mat != 'metal' else light)
                put(px, x, h - 1, dark)
        if w >= 6 and h >= 6:
            for x, y in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)):
                put(px, x, y, hi)
        if mat == 'argent' and w * h > 8:
            put(px, r.randrange(w), r.randrange(h), hi)
    elif mat in ('cristal', 'gemme', 'pierre'):
        base, dark, light, hi = PAL[mat]
        n = max(1, w + h - 2)
        for x in range(w):
            for y in range(h):
                t = (x + y) / n
                c = lerp(light, base, t * 1.4) if t < 0.5 else lerp(base, dark, (t - 0.5) * 2)
                if mat == 'pierre':
                    c = mul(c, 1 + r.uniform(-0.12, 0.12))
                put(px, x, y, c)
        if mat != 'pierre':
            if w >= 3:
                for y in range(h):
                    put(px, w // 2, y, lerp(px[w // 2, y][:3], hi, 0.35))
            put(px, 0, 0, hi)
            if w >= 3 and h >= 3:
                put(px, 1, 1, hi)
        else:
            put(px, r.randrange(w), r.randrange(h), hi)
    elif mat == 'plume':
        base, dark, light, deep = PAL['plume']
        for x in range(w):
            for y in range(h):
                c = light if (x + y) % 3 == 0 else base
                if x == 0 or x == w - 1:
                    c = dark
                put(px, x, y, c)
    elif mat == 'rune':
        pass
    elif mat in ('energie', 'lien'):
        main, alt, core = PAL['glow']
        alpha = 255 if mat == 'energie' else 170
        long_v = h >= w
        for x in range(w):
            for y in range(h):
                i = y if long_v else x
                c = lerp(main, alt, 0.5 + 0.5 * math.sin(i * 0.9))
                across, span = (x, w) if long_v else (y, h)
                if span >= 3 and across == span // 2:
                    c = lerp(c, core, 0.6)
                put(px, x, y, c, alpha)
    elif mat in ('corde', 'corde_runique', 'corde_magique'):
        cols = {'corde': [(214, 200, 168), (186, 170, 140)], 'corde_runique': [(178, 160, 222), (140, 120, 196)],
                'corde_magique': [(196, 170, 255), (150, 126, 255)]}[mat]
        for x in range(w):
            for y in range(h):
                put(px, x, y, cols[(y // 2) % 2])
    else:
        raise ValueError(mat)
    return img


def draw_pattern(img, pattern, x0, y0, color_fn):
    px = img.load()
    for j, row in enumerate(pattern):
        for i, ch in enumerate(row):
            x, y = x0 + i, y0 + j
            if ch == '#' and 0 <= x < img.width and 0 <= y < img.height:
                px[x, y] = color_fn(x, y, px[x, y])


def glow_color(r):
    main, alt, core = PAL['glow']
    return lambda x, y, old: lerp(main, core, r.uniform(0.15, 0.6)) + (255,)


def apply_deco(img, deco, r):
    w, h = img.size
    if not deco:
        return img
    if deco in ('glyphes', 'embleme'):
        if deco == 'embleme' and w >= 5 and h >= 5:
            draw_pattern(img, EMBLEME, (w - 5) // 2, (h - 5) // 2, glow_color(r))
            return img
        if h >= w:
            n = max(1, (h + 1) // 6)
            y = (h - (n * 6 - 1)) // 2
            for _ in range(n):
                g = GLYPHES[r.randrange(len(GLYPHES))]
                draw_pattern(img, g if w >= 3 else ['#'] * 5, (w - 3) // 2 if w >= 3 else (w - 1) // 2, y, glow_color(r))
                y += 6
        else:
            n = max(1, (w + 1) // 4)
            x = (w - (n * 4 - 1)) // 2
            for _ in range(n):
                g = GLYPHES[r.randrange(len(GLYPHES))]
                draw_pattern(img, g[:min(5, h)], x, max(0, (h - 5) // 2), glow_color(r))
                x += 4
        return img
    if deco in ('grave', 'grave_bleu', 'embleme_argent', 'embleme_sombre'):
        if w < 5 or h < 5:
            return img
        x0, y0 = (w - 5) // 2, (h - 5) // 2
        fn = {'grave': lambda x, y, old: PAL['bois_clair'][3] + (255,),
              'grave_bleu': lambda x, y, old: (110, 130, 225, 255),
              'embleme_argent': lambda x, y, old: PAL['argent'][2] + (255,),
              'embleme_sombre': lambda x, y, old: PAL['metal'][1] + (255,)}[deco]
        draw_pattern(img, EMBLEME, x0, y0, fn)
        return img
    if deco == 'rainure':
        long_v = h >= w
        span = w if long_v else h
        k = max(1, span // 3)
        lo = (span - k) // 2
        px = img.load()
        for i in range(w):
            for j in range(h):
                a = i if long_v else j
                if lo <= a < lo + k:
                    px[i, j] = PAL['metal'][1] + (255,)
                elif a == lo - 1 or a == lo + k:
                    px[i, j] = mul(px[i, j][:3], 0.8) + (255,)
        return img
    if deco == 'runes_gravees':
        y = 1
        while y + 5 <= h:
            g = GLYPHES[r.randrange(len(GLYPHES))]
            draw_pattern(img, g, (w - 3) // 2, y, lambda x, yy, old: lerp(mul(old[:3], 0.55), (80, 100, 200), 0.5) + (255,))
            y += 7
        return img
    raise ValueError(deco)


def build_atlas(models, seed_key):
    """Peint toutes les faces de tous les modeles (ex. un arc au repos + ses 3 etapes de traction) dans UNE texture
    commune : les faces identiques partagent la meme zone, les UV sont donc coherents d'un modele a l'autre."""
    cache, regions = {}, []
    for m in models:
        for p in m.parts:
            for f in FACES:
                if p.faces is not None and f not in p.faces:
                    continue
                fw, fh = face_dims(p, f)
                if fw <= 1e-6 or fh <= 1e-6:
                    continue
                side = f not in ('up', 'down')
                deco = p.deco.get(f)
                if p.mat == 'rune' and not deco:
                    deco = 'glyphes'
                if p.mat in STRETCH:
                    w, h = STRETCH[p.mat]
                    variant = 0
                else:
                    w, h = tex_size(fw), tex_size(fh)
                    variant = crc(p.name) % 3 if p.mat == 'rune' else 0
                key = (p.mat, deco, side, w, h, variant)
                if key not in cache:
                    r = random.Random(crc(seed_key, key))
                    img = apply_deco(paint_base(p.mat, w, h, side, r, smooth=deco in ('grave', 'grave_bleu')), deco, r)
                    cache[key] = len(regions)
                    regions.append([img, None])
                p.uv[f] = cache[key]
    order = sorted(range(len(regions)), key=lambda i: (-regions[i][0].height, -regions[i][0].width))
    for size in (32, 64, 128, 256, 512):
        x = y = shelf = 0
        ok, pos = True, {}
        for i in order:
            w, h = regions[i][0].size
            if x + w > size:
                x, y, shelf = 0, y + shelf, 0
            if y + h > size or w > size:
                ok = False
                break
            pos[i] = (x, y)
            x += w
            shelf = max(shelf, h)
        if ok:
            break
    atlas = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    for i, (img, _) in enumerate(regions):
        atlas.paste(img, pos[i])
    for m in models:
        for p in m.parts:
            for f, i in list(p.uv.items()):
                if isinstance(i, int):
                    (x, y), (w, h) = pos[i], regions[i][0].size
                    p.uv[f] = (x, y, w, h)
    return atlas


# ============================================================================================ export
def uid(*key):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, 'eldoria/' + '/'.join(map(str, key))))


def _rot_vec(p):
    if p.rot is None:
        return [0, 0, 0]
    ax, ang, _ = p.rot
    return [ang if ax == 'x' else 0, ang if ax == 'y' else 0, ang if ax == 'z' else 0]


def _origin(p):
    return p.rot[2] if p.rot is not None else [round((a + b) / 2, 4) for a, b in zip(p.frm, p.to)]


def group_tree(m):
    root, index = [], {}
    for i, p in enumerate(m.parts):
        siblings, prefix = root, ''
        for name in p.group.split('/'):
            prefix = f'{prefix}/{name}' if prefix else name
            if prefix not in index:
                node = {'name': name, 'path': prefix, 'children': []}
                index[prefix] = node
                siblings.append(node)
            siblings = index[prefix]['children']
        siblings.append(i)
    return root


def bbmodel(m, atlas, tex_name, namespace, display, colors, java_version='26.3', gui_front=True):
    size = atlas.width
    buf = io.BytesIO()
    atlas.save(buf, 'PNG')
    elements = []
    for i, p in enumerate(m.parts):
        faces = {}
        for f in FACES:
            if f in p.uv:
                x, y, w, h = p.uv[f]
                faces[f] = {'uv': [x, y, x + w, y + h], 'texture': 0}
            else:
                faces[f] = {'uv': [0, 0, 0, 0], 'texture': None}
        el = {'name': p.name, 'box_uv': False, 'rescale': False, 'locked': False, 'light_emission': p.light,
              'render_order': 'default', 'allow_mirror_modeling': True, 'from': p.frm, 'to': p.to, 'autouv': 0,
              'color': colors.get(p.group.split('/')[0], 0), 'origin': _origin(p), 'faces': faces, 'type': 'cube',
              'uuid': uid(m.key, 'el', i, p.name)}
        if p.rot is not None:
            el['rotation'] = _rot_vec(p)
        elements.append(el)

    def outline(nodes):
        out = []
        for n in nodes:
            if isinstance(n, int):
                out.append(elements[n]['uuid'])
            else:
                out.append({'name': n['name'], 'origin': [8, 8, 8], 'color': colors.get(n['path'].split('/')[0], 0),
                            'uuid': uid(m.key, 'grp', n['path']), 'export': True, 'mirror_uv': False,
                            'isOpen': '/' not in n['path'], 'locked': False, 'visibility': True, 'autouv': 0,
                            'children': outline(n['children'])})
        return out

    return {
        'meta': {'format_version': '4.10', 'model_format': 'java_block', 'box_uv': False},
        'name': m.key, 'parent': '', 'java_block_version': java_version, 'ambientocclusion': True,
        'front_gui_light': gui_front, 'visible_box': [1, 1, 0], 'variable_placeholders': '',
        'variable_placeholder_buttons': [], 'timeline_setups': [], 'unhandled_root_fields': {},
        'resolution': {'width': size, 'height': size},
        'elements': elements,
        'outliner': outline(group_tree(m)),
        'textures': [{
            'path': '', 'name': f'{tex_name}.png', 'folder': 'item', 'namespace': namespace, 'id': '0', 'group': '',
            'width': size, 'height': size, 'uv_width': size, 'uv_height': size, 'particle': True,
            'use_as_default': False, 'layers_enabled': False, 'sync_to_project': '', 'render_mode': 'default',
            'render_sides': 'auto', 'pbr_channel': 'color', 'frame_time': 1, 'frame_order_type': 'loop',
            'frame_order': '', 'frame_interpolate': False, 'visible': True, 'internal': True, 'saved': False,
            'uuid': uid(m.key, 'tex'), 'source': 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode('ascii'),
        }],
        'display': display,
    }


def java_model(m, atlas, tex_id, display, colors, credit, gui_light='front'):
    k = 16.0 / atlas.width
    elements = []
    for p in m.parts:
        faces = {}
        for f in FACES:
            if f in p.uv:
                x, y, w, h = p.uv[f]
                faces[f] = {'uv': [round(x * k, 4), round(y * k, 4), round((x + w) * k, 4), round((y + h) * k, 4)], 'texture': '#0'}
        el = {'name': p.name, 'from': p.frm, 'to': p.to}
        if p.rot is not None:
            el['rotation'] = {'angle': p.rot[1], 'axis': p.rot[0], 'origin': p.rot[2]}
        if p.light:
            el['light_emission'] = p.light
        el['faces'] = faces
        elements.append(el)

    def groups(nodes):
        out = []
        for n in nodes:
            if isinstance(n, int):
                out.append(n)
            else:
                out.append({'name': n['name'], 'origin': [8, 8, 8], 'color': colors.get(n['path'].split('/')[0], 0),
                            'children': groups(n['children'])})
        return out

    return {'credit': credit, 'texture_size': [atlas.width, atlas.width],
            'textures': {'0': tex_id, 'particle': tex_id}, 'elements': elements,
            'gui_light': gui_light, 'display': display, 'groups': groups(group_tree(m))}


def write_json(path, obj, indent=1):
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(obj, f, ensure_ascii=False, indent=indent)


# ============================================================================================ apercu
def _rot_point(v, ax, ang, org):
    a = math.radians(ang)
    c, s = math.cos(a), math.sin(a)
    x, y, z = v[0] - org[0], v[1] - org[1], v[2] - org[2]
    if ax == 'x':
        y, z = y * c - z * s, y * s + z * c
    elif ax == 'y':
        x, z = x * c + z * s, -x * s + z * c
    else:
        x, y = x * c - y * s, x * s + y * c
    return (x + org[0], y + org[1], z + org[2])


def _face_corners(p, f):
    (x1, y1, z1), (x2, y2, z2) = p.frm, p.to
    return {
        'north': ((x2, y2, z1), (x1, y2, z1), (x2, y1, z1)),
        'south': ((x1, y2, z2), (x2, y2, z2), (x1, y1, z2)),
        'east': ((x2, y2, z2), (x2, y2, z1), (x2, y1, z2)),
        'west': ((x1, y2, z1), (x1, y2, z2), (x1, y1, z1)),
        'up': ((x1, y2, z1), (x2, y2, z1), (x1, y2, z2)),
        'down': ((x1, y1, z2), (x2, y1, z2), (x1, y1, z1)),
    }[f]


def render(m, atlas, yaw, pitch, scale, glow=True, bounds=None):
    """Rendu orthographique pixel par pixel (tri par profondeur), lumiere directionnelle, halo sur les parties lumineuses."""
    cy, sy_ = math.cos(math.radians(yaw)), math.sin(math.radians(yaw))
    cp, sp = math.cos(math.radians(pitch)), math.sin(math.radians(pitch))
    cam = (sy_ * cp, sp, cy * cp)
    right = (cy, 0.0, -sy_)
    up = (-sy_ * sp, cp, -cy * sp)
    ld = (0.35, 0.85, 0.55)
    ln = math.sqrt(sum(v * v for v in ld))
    ld = tuple(v / ln for v in ld)
    tex = atlas.load()
    quads = []
    for p in m.parts:
        def tr(v, p=p):
            if p.rot is not None:
                v = _rot_point(v, *p.rot)
            xf = getattr(p, 'xf', None)      # transformation supplementaire (os d'un modele d'entite)
            return xf(v) if xf else v
        for f, (u0, v0, w, h) in p.uv.items():
            fw, fh = face_dims(p, f)
            tl, trr, bl = (tr(c) for c in _face_corners(p, f))
            # nombre de cellules dessinees : texels reels (ou etires pour les matieres unies)
            nw, nh = (max(1, tex_size(fw)), max(1, tex_size(fh))) if p.mat in STRETCH else (w, h)
            du = [(trr[i] - tl[i]) / nw for i in range(3)]
            dv = [(bl[i] - tl[i]) / nh for i in range(3)]
            n = (du[1] * dv[2] - du[2] * dv[1], du[2] * dv[0] - du[0] * dv[2], du[0] * dv[1] - du[1] * dv[0])
            nl = math.sqrt(sum(v * v for v in n)) or 1
            n = tuple(-v / nl for v in n)
            if sum(n[i] * cam[i] for i in range(3)) <= 1e-6:
                continue
            shade = 1.0 if p.light >= 10 else 0.55 + 0.45 * max(0.0, sum(n[i] * ld[i] for i in range(3)))
            for j in range(nh):
                for i in range(nw):
                    c = tex[u0 + min(w - 1, i * w // nw), v0 + min(h - 1, j * h // nh)]
                    if c[3] == 0:
                        continue
                    pts = []
                    for a, b in ((i, j), (i + 1, j), (i + 1, j + 1), (i, j + 1)):
                        P = (tl[0] + a * du[0] + b * dv[0], tl[1] + a * du[1] + b * dv[1], tl[2] + a * du[2] + b * dv[2])
                        pts.append((sum(P[k] * right[k] for k in range(3)), -sum(P[k] * up[k] for k in range(3)),
                                    sum(P[k] * cam[k] for k in range(3))))
                    quads.append((sum(q[2] for q in pts) / 4, [(q[0], q[1]) for q in pts], mul(c[:3], shade) + (c[3],), p.light >= 10))
    if bounds is None:
        xs = [q[0] for qd in quads for q in qd[1]]
        ys = [q[1] for qd in quads for q in qd[1]]
        bounds = (min(xs) - 1.5, max(xs) + 1.5, min(ys) - 1.5, max(ys) + 1.5)
    minx, maxx, miny, maxy = bounds
    W, H = int((maxx - minx) * scale) + 1, int((maxy - miny) * scale) + 1
    img = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    gl = Image.new('RGBA', (W, H), (0, 0, 0, 0))
    d, dg = ImageDraw.Draw(img, 'RGBA'), ImageDraw.Draw(gl, 'RGBA')
    quads.sort(key=lambda q: q[0])
    for _, pts, c, emissive in quads:
        poly_ = [((x - minx) * scale, (y - miny) * scale) for x, y in pts]
        d.polygon(poly_, fill=c)
        if emissive:
            dg.polygon(poly_, fill=c)
    if glow:
        halo = gl.filter(ImageFilter.GaussianBlur(scale * 0.9))
        out = Image.new('RGBA', (W, H), (0, 0, 0, 0))
        out.alpha_composite(halo)
        out.alpha_composite(halo)
        out.alpha_composite(img)
        img = out
    return img


def font(size, bold=True):
    for name in (('segoeuib.ttf', 'arialbd.ttf', 'DejaVuSans-Bold.ttf') if bold else ('segoeui.ttf', 'arial.ttf', 'DejaVuSans.ttf')):
        try:
            return ImageFont.truetype(name, size)
        except OSError:
            continue
    return ImageFont.load_default()


def background(W, H):
    bg = Image.new('RGBA', (W, H), (26, 24, 32, 255))
    d = ImageDraw.Draw(bg)
    for y in range(H):
        d.line([(0, y), (W, y)], fill=lerp((34, 32, 44), (18, 16, 22), y / H) + (255,))
    return bg


def lineup(entries, path, title, yaw=32, pitch=14, scale=15, min_w=230):
    """entries = [(modele, atlas, legende, sous-titre)] ; tous a la meme echelle, poses sur la meme ligne de sol."""
    imgs = [render(m, at, yaw, pitch, scale) for m, at, _, _ in entries]
    Hm = max(i.height for i in imgs)
    W = sum(max(i.width, min_w) for i in imgs) + 40 * (len(imgs) + 1)
    H = Hm + 220
    bg = background(W, H)
    d = ImageDraw.Draw(bg)
    ft, fl, fs = font(44), font(30), font(18, bold=False)
    d.text(((W - d.textlength(title, font=ft)) / 2, 28), title, font=ft, fill=(226, 222, 240))
    x, base_y = 40, 110 + Hm
    for (m, at, lab, sub), im in zip(entries, imgs):
        cw = max(im.width, min_w)
        bg.alpha_composite(im, (x + (cw - im.width) // 2, base_y - im.height))
        d.text((x + (cw - d.textlength(lab, font=fl)) / 2, base_y + 22), lab, font=fl, fill=(232, 228, 246))
        d.text((x + (cw - d.textlength(sub, font=fs)) / 2, base_y + 62), sub, font=fs, fill=(170, 164, 196))
        x += cw + 40
    bg.convert('RGB').save(path)


def views(m, atlas, path, title, specs=(('Face', 0, 0), ('Profil', 90, 0), ('3/4', 35, 16), ('Dos 3/4', 215, 16)), scale=12):
    imgs = [render(m, atlas, yw, pt, scale) for _, yw, pt in specs]
    Hm = max(i.height for i in imgs)
    W = sum(max(i.width, 200) for i in imgs) + 40 * (len(imgs) + 1) + 160
    H = Hm + 140
    bg = background(W, H)
    d = ImageDraw.Draw(bg)
    d.text((24, 18), title, font=font(28), fill=(226, 222, 240))
    x = 40
    for (lab, _, _), im in zip(specs, imgs):
        cw = max(im.width, 200)
        bg.alpha_composite(im, (x + (cw - im.width) // 2, 70 + Hm - im.height))
        d.text((x + (cw - d.textlength(lab, font=font(20))) / 2, 86 + Hm), lab, font=font(20), fill=(190, 184, 214))
        x += cw + 40
    tx = atlas if atlas.width > 64 else atlas.resize((atlas.width * 2, atlas.height * 2), Image.NEAREST)
    if tx.height > Hm:
        tx = tx.resize((Hm * tx.width // tx.height, Hm), Image.NEAREST)
    bg.alpha_composite(tx, (x, 70))
    d.text((x, 86 + Hm), f'texture {atlas.width}x{atlas.height}', font=font(18, False), fill=(190, 184, 214))
    bg.convert('RGB').save(path)


# ============================================================================================ affichage en main
def euler_xyz(rx, ry, rz):
    """Matrice de rotation de Minecraft (rotationXYZ : R = Rx . Ry . Rz), angles en degres."""
    def rx_(a):
        c, s_ = math.cos(a), math.sin(a)
        return [[1, 0, 0], [0, c, -s_], [0, s_, c]]

    def ry_(a):
        c, s_ = math.cos(a), math.sin(a)
        return [[c, 0, s_], [0, 1, 0], [-s_, 0, c]]

    def rz_(a):
        c, s_ = math.cos(a), math.sin(a)
        return [[c, -s_, 0], [s_, c, 0], [0, 0, 1]]

    def mm(a, b):
        return [[sum(a[i][k] * b[k][j] for k in range(3)) for j in range(3)] for i in range(3)]
    return mm(mm(rx_(math.radians(rx)), ry_(math.radians(ry))), rz_(math.radians(rz)))


def bbox(m):
    xs = [v for p in m.parts for v in (p.frm[0], p.to[0])]
    ys = [v for p in m.parts for v in (p.frm[1], p.to[1])]
    return min(xs), max(xs), min(ys), max(ys)


def gui_fit(m, margin=15.0, max_scale=0.6):
    """Icone d'inventaire : modele incline a 45 deg, centre et mis a l'echelle pour tenir dans la case 16x16."""
    x1, x2, y1, y2 = bbox(m)
    ext = ((x2 - x1) + (y2 - y1)) * math.sqrt(0.5)
    sc = round(min(max_scale, margin / ext), 3)
    cx, cy = (x1 + x2) / 2 - 8, (y1 + y2) / 2 - 8
    c, s_ = math.cos(math.radians(-45)), math.sin(math.radians(-45))
    tx, ty = -(cx * c - cy * s_) * sc, -(cx * s_ + cy * c) * sc
    return {'rotation': [0, 0, -45], 'translation': [round(tx, 2), round(ty, 2), 0], 'scale': [sc, sc, sc]}


def handheld_display(m, grip, third=0.55, first=0.45, ref_height=None):
    """Reglages d'une arme tenue en main, derives de ceux de l'epee vanilla (item/handheld) : le modele, dessine
    debout (manche selon +Y), est incline de -45 deg comme le sprite vanilla, et le point 'grip' (x, y) du modele
    est replace a l'endroit ou la main tient la poignee de l'epee vanilla. Main gauche : Minecraft inverse
    lui-meme X, rotation Y et rotation Z, il suffit donc d'inverser Y et Z."""
    if ref_height:   # en 1re personne, les tres grandes armes sont legerement reduites pour ne pas masquer l'ecran
        _, _, y1, y2 = bbox(m)
        first = round(first * min(1.0, (ref_height / (y2 - y1)) ** 0.5), 3)
    gx, gy = grip[0] - 8, grip[1] - 8
    c, s_ = math.cos(math.radians(-45)), math.sin(math.radians(-45))
    gpx, gpy = gx * c - gy * s_, gx * s_ + gy * c

    def entry(rot, trans, base_scale, sc, sprite_grip=(-5.0, -5.0)):
        dx = base_scale * sprite_grip[0] - sc * gpx
        dy = base_scale * sprite_grip[1] - sc * gpy
        R = euler_xyz(*rot)
        t = [trans[i] + R[i][0] * dx + R[i][1] * dy for i in range(3)]
        return {'rotation': [rot[0], rot[1], rot[2] - 45], 'translation': [round(v, 2) for v in t], 'scale': [sc, sc, sc]}

    tr = entry((0, -90, 55), (0, 4.0, 0.5), 0.85, third)
    fr = entry((0, -90, 25), (1.13, 3.2, 1.13), 0.68, first)
    mirror = lambda e: {'rotation': [e['rotation'][0], -e['rotation'][1], -e['rotation'][2]],
                        'translation': list(e['translation']), 'scale': list(e['scale'])}
    gui = gui_fit(m)
    return {'thirdperson_righthand': tr, 'thirdperson_lefthand': mirror(tr),
            'firstperson_righthand': fr, 'firstperson_lefthand': mirror(fr),
            'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.3, 0.3, 0.3]},
            'gui': gui, 'fixed': {'rotation': [0, 0, -45], 'translation': gui['translation'], 'scale': [gui['scale'][0] * 1.2] * 3},
            'head': {'rotation': [0, 0, 0], 'translation': [0, 6, 0], 'scale': [0.5, 0.5, 0.5]}}
