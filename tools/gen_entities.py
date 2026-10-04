"""Textures d'entites (mobs, boss) et couches d'armure portees, generees proceduralement."""
import random
from PIL import Image
from pixel import rgb, shade, mix, T


def _px(img, x, y, c):
    if 0 <= x < img.width and 0 <= y < img.height:
        img.putpixel((x, y), c)


def faces(u, v, w, h, d):
    """Rectangles UV des 6 faces d'une boite Minecraft."""
    return {
        'top': (u + d, v, w, d),
        'bottom': (u + d + w, v, w, d),
        'right': (u, v + d, d, h),
        'front': (u + d, v + d, w, h),
        'left': (u + d + w, v + d, d, h),
        'back': (u + d + w + d, v + d, w, h),
    }


def fill_rect(img, rect, color, var=0.08, seed=1, rows=None):
    x0, y0, w, h = rect
    rnd = random.Random(seed)
    for y in range(h):
        if rows is not None and y not in rows:
            continue
        for x in range(w):
            c = color(x, y, w, h) if callable(color) else color
            if c is None:
                continue
            if var and c[3] > 0:
                c = shade(c, 1 + rnd.uniform(-var, var))
            _px(img, x0 + x, y0 + y, c)


def plate(base, trim=None, border=True, highlight=True):
    B = rgb(base) if isinstance(base, str) else base
    Tr = rgb(trim) if isinstance(trim, str) else trim

    def f(x, y, w, h):
        if border and (x == 0 or x == w - 1 or y == h - 1):
            return shade(B, 0.6)
        if highlight and y == 0:
            return shade(B, 1.35)
        if Tr and y == 1:
            return Tr
        return B
    return f


def paint_box(img, u, v, w, h, d, style, seed=0, skip=()):
    for i, (name, rect) in enumerate(faces(u, v, w, h, d).items()):
        if name in skip:
            continue
        col = style(name) if callable(style) else style
        if col is None:
            continue
        fill_rect(img, rect, col, seed=seed + i)


# ---------------------------------------------------------------------------
# Peaux humanoides (disposition zombie 64x64 : tete, chapeau, corps, bras, jambes)
# ---------------------------------------------------------------------------

def humanoid_skin(spec, seed=0):
    img = Image.new('RGBA', (64, 64), T)
    skin = rgb(spec['skin'])
    shirt = rgb(spec.get('shirt', spec['skin']))
    pants = rgb(spec.get('pants', spec.get('shirt', spec['skin'])))
    shoes = rgb(spec.get('shoes', '#3a2a1a'))
    belt = rgb(spec['belt']) if spec.get('belt') else None
    hair = rgb(spec['hair']) if spec.get('hair') else None
    eyes = rgb(spec.get('eyes', '#202020'))
    armor = rgb(spec['armor']) if spec.get('armor') else None
    robe = spec.get('robe', False)

    # --- tete
    def head_style(face):
        def f(x, y, w, h):
            c = skin
            if hair and (y < 2 or (face in ('back',) and y < 6) or (face in ('right', 'left') and y < 3)):
                c = hair
            if face == 'top':
                c = hair or skin
            return c
        return f
    paint_box(img, 0, 0, 8, 8, 8, head_style, seed)
    fx, fy = 8, 8  # face avant
    # yeux
    eye_y = spec.get('eye_y', 4)
    for ex in (1, 2, 5, 6):
        _px(img, fx + ex, fy + eye_y, eyes if ex in (2, 5) else shade(eyes, 1.6) if spec.get('glow') else rgb('#f0f0f0'))
    if spec.get('glow'):
        for ex in (1, 2, 5, 6):
            _px(img, fx + ex, fy + eye_y, eyes)
    if spec.get('brow'):
        for ex in (1, 2, 5, 6):
            _px(img, fx + ex, fy + eye_y - 1, rgb(spec['brow']))
    # bouche / defenses
    if spec.get('mouth'):
        for mx in (3, 4):
            _px(img, fx + mx, fy + 6, rgb(spec['mouth']))
    if spec.get('tusks'):
        _px(img, fx + 2, fy + 6, rgb('#f0ead0'))
        _px(img, fx + 5, fy + 6, rgb('#f0ead0'))
        _px(img, fx + 2, fy + 5, rgb('#f0ead0'))
        _px(img, fx + 5, fy + 5, rgb('#f0ead0'))
    if spec.get('nose'):
        _px(img, fx + 3, fy + 5, shade(skin, 0.75))
        _px(img, fx + 4, fy + 5, shade(skin, 0.75))
    if spec.get('skull'):
        for (x, y) in [(3, 5), (4, 5)]:
            _px(img, fx + x, fy + y, rgb('#2a2020'))
        for x in range(1, 7):
            _px(img, fx + x, fy + 7, rgb('#3a3030') if x % 2 else skin)
    if spec.get('ears'):
        # oreilles pointues sur les faces laterales
        for face in ('right', 'left'):
            rx, ry, rw, rh = faces(0, 0, 8, 8, 8)[face]
            for i in range(5):
                _px(img, rx + 1 + i, ry + 3 - (i // 2), shade(skin, 1.1))
                _px(img, rx + 1 + i, ry + 4, shade(skin, 0.8))

    # --- couche chapeau (accessoires)
    hat = spec.get('hat')
    if hat:
        hc = rgb(hat.get('color', '#d0a020'))
        kind = hat['kind']
        hf = faces(32, 0, 8, 8, 8)
        if kind == 'crown':
            for face in ('front', 'back', 'left', 'right'):
                x0, y0, w, h = hf[face]
                for x in range(w):
                    _px(img, x0 + x, y0 + 0, hc if x % 3 != 1 else T)
                    _px(img, x0 + x, y0 + 1, hc)
                    _px(img, x0 + x, y0 + 2, shade(hc, 0.75))
                if face == 'front':
                    _px(img, x0 + 3, y0 + 1, rgb(hat.get('gem', '#e02040')))
                    _px(img, x0 + 4, y0 + 1, rgb(hat.get('gem', '#e02040')))
        elif kind == 'hood':
            for face, (x0, y0, w, h) in hf.items():
                for y in range(h):
                    for x in range(w):
                        if face == 'front' and 1 <= x <= 6 and y >= 2:
                            continue
                        if face == 'bottom':
                            continue
                        c = shade(hc, 0.85 + 0.15 * (1 - y / h)) if face != 'top' else hc
                        _px(img, x0 + x, y0 + y, c)
        elif kind == 'horns':
            x0, y0, w, h = hf['front']
            for (x, y) in [(0, 0), (0, 1), (1, 1), (1, 2), (7, 0), (7, 1), (6, 1), (6, 2)]:
                _px(img, x0 + x, y0 + y, hc)
            for face in ('left', 'right'):
                x0, y0, w, h = hf[face]
                for (x, y) in [(3, 0), (4, 0), (3, 1), (4, 1), (4, 2)]:
                    _px(img, x0 + x, y0 + y, shade(hc, 0.85))
        elif kind == 'helmet':
            for face, (x0, y0, w, h) in hf.items():
                for y in range(h):
                    for x in range(w):
                        c = shade(hc, 1.25) if y == 0 else hc
                        if face == 'front':
                            if y in (3, 4) and 1 <= x <= 6:
                                c = rgb(hat.get('visor', '#101010'))
                            if y > 5 and 2 <= x <= 5:
                                c = shade(hc, 0.7)
                        if face == 'bottom':
                            c = None
                        if c:
                            _px(img, x0 + x, y0 + y, c)

    # --- corps
    def body_style(face):
        def f(x, y, w, h):
            c = shirt
            if belt and y in (8, 9):
                c = belt
            if armor and y < 7 and face in ('front', 'back', 'right', 'left', 'top'):
                c = shade(armor, 1.2) if y == 0 else armor
                if face == 'front' and spec.get('emblem') and 2 <= y <= 4 and 3 <= x <= 4:
                    c = rgb(spec['emblem'])
            if robe and y >= 10:
                c = shade(shirt, 0.85)
            if spec.get('cape') and face == 'back':
                c = rgb(spec['cape']) if y < 12 else c
            return c
        return f
    paint_box(img, 16, 16, 8, 12, 4, body_style, seed + 10)

    # --- bras
    def arm_style(face):
        def f(x, y, w, h):
            c = rgb(spec.get('sleeve', spec.get('shirt', spec['skin']))) if y < spec.get('sleeve_len', 5) else skin
            if armor and y < 4:
                c = shade(armor, 1.15) if y == 0 else armor
            if spec.get('gloves') and y >= 9:
                c = rgb(spec['gloves'])
            if face == 'bottom':
                c = skin if not spec.get('gloves') else rgb(spec['gloves'])
            return c
        return f
    paint_box(img, 40, 16, 4, 12, 4, arm_style, seed + 20)

    # --- jambes
    def leg_style(face):
        def f(x, y, w, h):
            c = pants
            if y >= 9:
                c = shoes
            if robe and y < 9:
                c = shade(shirt, 0.8 + (0.05 if x % 2 else 0))
            if face == 'top':
                c = pants if not robe else shade(shirt, 0.8)
            if face == 'bottom':
                c = shoes
            return c
        return f
    paint_box(img, 0, 16, 4, 12, 4, leg_style, seed + 30)

    # taches / details
    if spec.get('specks'):
        rnd = random.Random(seed + 99)
        sc = rgb(spec['specks'])
        for _ in range(40):
            x, y = rnd.randrange(0, 56), rnd.randrange(0, 32)
            if img.getpixel((x, y))[3] > 0 and not (32 <= x < 64 and y < 16):
                _px(img, x, y, sc)
    return img


# ---------------------------------------------------------------------------
# Couches d'armure portees (humanoid 64x32 / humanoid_leggings 64x32)
# ---------------------------------------------------------------------------

def armor_layers(base, trim, gem, style='plate', seed=0):
    B, Tr, G = rgb(base), rgb(trim), rgb(gem)
    outer = Image.new('RGBA', (64, 32), T)
    legs = Image.new('RGBA', (64, 32), T)

    def shaded(x, y, w, h, c):
        if x == 0 or x == w - 1:
            return shade(c, 0.72)
        if y == 0:
            return shade(c, 1.3)
        return c

    # Casque (boite tete 8x8x8 en 0,0)
    hf = faces(0, 0, 8, 8, 8)
    for face, (x0, y0, w, h) in hf.items():
        for y in range(h):
            for x in range(w):
                c = shaded(x, y, w, h, B)
                if style == 'robe':
                    # capuche : visage degage
                    if face == 'front' and 1 <= x <= 6 and y >= 2:
                        continue
                    if y == h - 1:
                        c = Tr
                elif style == 'leather':
                    if face == 'front' and y >= 3:
                        continue
                    if face in ('left', 'right') and y >= 5 and x < 3:
                        continue
                    if y == 2 and face != 'top':
                        c = Tr
                else:
                    if face == 'front':
                        if y in (3, 4) and 1 <= x <= 6 and x not in (3, 4):
                            c = rgb('#0c0c10')
                        elif y in (3, 4):
                            c = shade(B, 0.8)
                        if y >= 6 and 2 <= x <= 5:
                            c = shade(B, 0.85)
                    if y == 1 and face != 'top':
                        c = Tr
                if face == 'front' and y == 1 and x in (3, 4) and style != 'leather':
                    c = G
                if face == 'bottom':
                    continue
                _px(outer, x0 + x, y0 + y, shade(c, 1 + random.Random(seed + x * 7 + y * 13).uniform(-0.05, 0.05)))

    # Plastron : corps (16,16) 8x12x4 + bras (40,16) 4x12x4
    bf = faces(16, 16, 8, 12, 4)
    for face, (x0, y0, w, h) in bf.items():
        for y in range(h):
            for x in range(w):
                c = shaded(x, y, w, h, B)
                if style == 'robe':
                    if y in (0, 1) and face in ('front', 'back'):
                        c = Tr
                    if face == 'front' and x in (3, 4):
                        c = shade(Tr, 0.9)
                elif style == 'leather':
                    if face == 'front' and (x + y) % 5 == 0:
                        c = shade(B, 0.85)
                    if y in (8, 9):
                        c = Tr
                else:
                    if y == 0 or (face in ('front', 'back') and y == 6):
                        c = Tr
                if face == 'front' and 3 <= x <= 4 and 2 <= y <= 3:
                    c = G
                if face in ('top', 'bottom'):
                    c = shade(B, 0.9)
                _px(outer, x0 + x, y0 + y, c)
    af = faces(40, 16, 4, 12, 4)
    for face, (x0, y0, w, h) in af.items():
        for y in range(h):
            for x in range(w):
                if style == 'robe' and y > 9:
                    c = Tr
                elif style == 'leather' and y > 6:
                    continue
                elif style == 'plate' and y > 8:
                    c = shade(B, 0.8)
                else:
                    c = shaded(x, y, w, h, B)
                    if y in (0, 1) and style != 'robe':
                        c = Tr if y == 1 else shade(B, 1.35)
                if face == 'bottom' and style == 'leather':
                    continue
                _px(outer, x0 + x, y0 + y, c)

    # Bottes : bas des jambes (0,16) 4x12x4 dans la couche externe
    lf = faces(0, 16, 4, 12, 4)
    for face, (x0, y0, w, h) in lf.items():
        if face == 'top':
            continue
        for y in range(h):
            if face != 'bottom' and y < 7:
                continue
            for x in range(w):
                c = shaded(x, y, w, h, B)
                if y == 7:
                    c = Tr
                if face == 'bottom':
                    c = shade(B, 0.55)
                _px(outer, x0 + x, y0 + y, c)

    # Jambieres : jambes + taille du corps dans la couche leggings
    for face, (x0, y0, w, h) in lf.items():
        for y in range(h):
            if face != 'top' and y > 9:
                continue
            for x in range(w):
                if face == 'bottom':
                    continue
                c = shaded(x, y, w, h, shade(B, 0.92))
                if y == 4 and face in ('front',):
                    c = Tr
                if style == 'robe' and face != 'top':
                    c = shade(B, 0.85 + 0.08 * (x % 2))
                _px(legs, x0 + x, y0 + y, c)
    for face, (x0, y0, w, h) in bf.items():
        for y in range(h):
            if face in ('front', 'back', 'left', 'right') and y < 7:
                continue
            for x in range(w):
                c = shade(B, 0.9)
                if y in (7, 8):
                    c = Tr
                if face == 'front' and y == 7 and x in (3, 4):
                    c = G
                _px(legs, x0 + x, y0 + y, c)
    return outer, legs


# ---------------------------------------------------------------------------
# Autres modeles (blaze, golem, loup, squelette)
# ---------------------------------------------------------------------------

def blaze_texture(head, rods, eyes, seed=0):
    img = Image.new('RGBA', (64, 32), T)
    H, R, E = rgb(head), rgb(rods), rgb(eyes)

    def head_style(face):
        def f(x, y, w, h):
            c = H
            if face == 'front' and y in (3, 4) and x in (1, 2, 5, 6):
                c = E
            if face == 'front' and y == 6 and 2 <= x <= 5:
                c = shade(H, 0.5)
            return c
        return f
    paint_box(img, 0, 0, 8, 8, 8, head_style, seed)
    paint_box(img, 0, 16, 2, 8, 2, lambda face: (lambda x, y, w, h: shade(R, 1.3) if y < 2 else R), seed + 5)
    return img


def golem_texture(base, accent, eyes, seed=0, cracks=None):
    img = Image.new('RGBA', (128, 128), T)
    B, A, E = rgb(base), rgb(accent), rgb(eyes)
    Cr = rgb(cracks) if cracks else shade(B, 0.6)
    rnd = random.Random(seed)

    def stone(face):
        def f(x, y, w, h):
            c = B
            r = rnd.random()
            if r < 0.06:
                c = A
            elif r < 0.12:
                c = Cr
            if x == 0 or x == w - 1:
                c = shade(c, 0.8)
            return c
        return f
    paint_box(img, 0, 0, 8, 10, 8, lambda face: (lambda x, y, w, h: (E if (face == 'front' and y == 4 and x in (1, 2, 5, 6)) else stone(face)(x, y, w, h))), seed)
    paint_box(img, 24, 0, 2, 4, 2, stone, seed + 1)
    paint_box(img, 0, 40, 18, 12, 11, lambda face: (lambda x, y, w, h: (A if (face == 'front' and 7 <= x <= 10 and 3 <= y <= 6) else stone(face)(x, y, w, h))), seed + 2)
    paint_box(img, 0, 70, 9, 5, 6, stone, seed + 3)
    paint_box(img, 60, 21, 4, 30, 6, stone, seed + 4)
    paint_box(img, 60, 58, 4, 30, 6, stone, seed + 5)
    paint_box(img, 37, 0, 6, 16, 5, stone, seed + 6)
    paint_box(img, 60, 0, 6, 16, 5, stone, seed + 7)
    return img


def wolf_texture(fur, belly, eyes, seed=0):
    img = Image.new('RGBA', (64, 32), T)
    F, Bl, E = rgb(fur), rgb(belly), rgb(eyes)

    def furry(face):
        def f(x, y, w, h):
            c = F
            if face in ('bottom',):
                c = Bl
            return c
        return f
    paint_box(img, 0, 0, 6, 6, 4, lambda face: (lambda x, y, w, h: (E if face == 'front' and y == 2 and x in (1, 4) else furry(face)(x, y, w, h))), seed)
    paint_box(img, 16, 14, 2, 2, 1, furry, seed + 1)
    paint_box(img, 0, 10, 3, 3, 4, lambda face: (lambda x, y, w, h: (rgb('#101010') if face == 'front' and y == 0 and x == 1 else shade(F, 0.9))), seed + 2)
    paint_box(img, 18, 14, 6, 9, 6, furry, seed + 3)
    paint_box(img, 21, 0, 8, 6, 7, lambda face: (lambda x, y, w, h: shade(F, 0.8)), seed + 4)
    paint_box(img, 0, 18, 2, 8, 2, furry, seed + 5)
    paint_box(img, 9, 18, 2, 8, 2, lambda face: (lambda x, y, w, h: shade(F, 0.85) if y < 6 else Bl), seed + 6)
    return img


def skeleton_texture(bone, cloth, eyes, seed=0):
    img = Image.new('RGBA', (64, 32), T)
    Bo, Cl, E = rgb(bone), rgb(cloth), rgb(eyes)

    def head(face):
        def f(x, y, w, h):
            c = Bo
            if face == 'front':
                if y in (3, 4) and x in (1, 2, 5, 6):
                    c = E if y == 4 else rgb('#1a1418')
                if y == 5 and x in (3, 4):
                    c = rgb('#1a1418')
                if y == 7 and x % 2 == 1:
                    c = rgb('#2a2228')
            return c
        return f
    paint_box(img, 0, 0, 8, 8, 8, head, seed)
    # capuche sur le chapeau
    hf = faces(32, 0, 8, 8, 8)
    for face, (x0, y0, w, h) in hf.items():
        if face == 'bottom':
            continue
        for y in range(h):
            for x in range(w):
                if face == 'front' and 1 <= x <= 6 and y >= 2:
                    continue
                if face in ('left', 'right') and y > 5:
                    continue
                _px(img, x0 + x, y0 + y, shade(Cl, 0.9 + 0.1 * (y % 2)))

    def ribs(face):
        def f(x, y, w, h):
            if y < 5 or y > 9:
                return Cl if y < 3 or y > 9 else (Bo if y % 2 == 0 else shade(Bo, 0.45))
            return Bo if y % 2 == 0 else rgb('#2a2228')
        return f
    paint_box(img, 16, 16, 8, 12, 4, ribs, seed + 1)
    paint_box(img, 40, 16, 2, 12, 2, lambda face: (lambda x, y, w, h: Bo if y > 2 else Cl), seed + 2)
    paint_box(img, 0, 16, 2, 12, 2, lambda face: (lambda x, y, w, h: Bo), seed + 3)
    return img
