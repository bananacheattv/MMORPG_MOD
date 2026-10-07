"""
Moteur de rendu des icones d'objets en haute definition (64 x 64), Pillow uniquement.

Chaque icone est composee de calques (masque + matiere) dessines a 256 px :
relief calcule depuis le masque (biseau eclaire en haut a gauche), texture de matiere procedurale
(metal brosse, gemme a facettes, os, cuir, tissu tisse, fourrure, pierre, bois, organique, verre, lueur),
degrade de couleur a 3 tons, reflets speculaires, puis reduction a 64 px avec contour sombre et ombre.
"""
import math
import random

from PIL import Image, ImageChops, ImageDraw, ImageFilter, ImageOps

S = 256
OUT = 64


def hexrgb(c):
    c = c.lstrip('#')
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4))


def mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def lighten(c, t):
    return mix(c, (255, 255, 255), t)


def darken(c, t):
    return mix(c, (0, 0, 0), t)


# ---------------------------------------------------------------------------- bruits et textures (images L 256 px)

def noise(seed, cells, contrast=1.0):
    """Bruit lisse : grille aleatoire de cells x cells agrandie en bicubique."""
    rnd = random.Random(seed)
    small = Image.new('L', (cells, cells))
    small.putdata([int(128 + (rnd.random() - 0.5) * 255 * contrast) for _ in range(cells * cells)])
    return small.resize((S, S), Image.BICUBIC)


def fractal(seed, contrast=1.0):
    a = noise(seed, 6, contrast)
    b = noise(seed + 1, 16, contrast)
    c = noise(seed + 2, 48, contrast)
    return Image.blend(Image.blend(a, b, 0.4), c, 0.3)


def streaks(seed, angle=0, count=96):
    """Stries (metal brosse, bois, fourrure) orientees selon angle (degres)."""
    rnd = random.Random(seed)
    small = Image.new('L', (count, 4))
    small.putdata([int(128 + (rnd.random() - 0.5) * 200) for _ in range(count * 4)])
    big = small.resize((S * 2, S * 2), Image.BICUBIC).rotate(angle, resample=Image.BICUBIC)
    return big.crop((S // 2, S // 2, S // 2 + S, S // 2 + S))


def facets(seed, n=14):
    """Facettes de gemme : eventail de triangles de luminosites differentes."""
    rnd = random.Random(seed)
    img = Image.new('L', (S, S), 128)
    d = ImageDraw.Draw(img)
    cx, cy = S * 0.5 + rnd.uniform(-20, 20), S * 0.45 + rnd.uniform(-20, 20)
    angles = sorted(rnd.uniform(0, 2 * math.pi) for _ in range(n))
    for i, a in enumerate(angles):
        b = angles[(i + 1) % n] + (2 * math.pi if i == n - 1 else 0)
        light = int(128 + 110 * math.cos(a - math.pi * 1.25) * rnd.uniform(0.5, 1.0))
        d.polygon([(cx, cy), (cx + math.cos(a) * S, cy + math.sin(a) * S), (cx + math.cos(b) * S, cy + math.sin(b) * S)], fill=light)
    return img.filter(ImageFilter.GaussianBlur(1.5))


def weave(seed, step=14):
    img = Image.new('L', (S, S), 128)
    d = ImageDraw.Draw(img)
    for i in range(-S, 2 * S, step):
        d.line([(i, 0), (i + S, S)], fill=170, width=4)
        d.line([(i, S), (i + S, 0)], fill=90, width=3)
    return Image.blend(img.filter(ImageFilter.GaussianBlur(1.2)), noise(seed, 40, .6), 0.35)


def speckle(seed, density=0.012):
    rnd = random.Random(seed)
    img = Image.new('L', (S, S), 128)
    d = ImageDraw.Draw(img)
    for _ in range(int(S * S * density)):
        x, y, r = rnd.uniform(0, S), rnd.uniform(0, S), rnd.uniform(1, 4)
        v = rnd.choice((40, 70, 200, 230))
        d.ellipse((x - r, y - r, x + r, y + r), fill=v)
    return Image.blend(img.filter(ImageFilter.GaussianBlur(0.8)), fractal(seed, .8), 0.5)


def cracks(seed, n=7):
    rnd = random.Random(seed)
    img = Image.new('L', (S, S), 128)
    d = ImageDraw.Draw(img)
    for _ in range(n):
        x, y = rnd.uniform(40, S - 40), rnd.uniform(40, S - 40)
        a = rnd.uniform(0, 2 * math.pi)
        pts = [(x, y)]
        for _ in range(rnd.randint(3, 6)):
            a += rnd.uniform(-0.8, 0.8)
            x, y = x + math.cos(a) * rnd.uniform(10, 26), y + math.sin(a) * rnd.uniform(10, 26)
            pts.append((x, y))
        d.line(pts, fill=40, width=3)
    return img.filter(ImageFilter.GaussianBlur(0.7))


TEXTURES = {
    'metal': lambda s: Image.blend(streaks(s, 35), fractal(s, .5), .3),
    'gold': lambda s: Image.blend(streaks(s, 35), fractal(s, .6), .4),
    'gem': lambda s: facets(s),
    'crystal': lambda s: facets(s, 9),
    'bone': lambda s: Image.blend(fractal(s, .7), speckle(s, .004), .4),
    'organic': lambda s: fractal(s, 1.2),
    'leather': lambda s: Image.blend(speckle(s, .03), fractal(s, .8), .5),
    'cloth': lambda s: weave(s),
    'fur': lambda s: Image.blend(streaks(s, 70, 160), fractal(s, .6), .25),
    'stone': lambda s: ImageChops.darker(speckle(s), cracks(s)),
    'wood': lambda s: Image.blend(streaks(s, 80, 40), fractal(s, .9), .35),
    'glow': lambda s: fractal(s, .6),
    'glass': lambda s: noise(s, 8, .3),
    'scale': lambda s: scales(s),
}


def scales(seed):
    img = Image.new('L', (S, S), 110)
    d = ImageDraw.Draw(img)
    r = 22
    for row in range(-1, S // (r - 6) + 2):
        for col in range(-1, S // r + 2):
            x = col * r + (r / 2 if row % 2 else 0)
            y = row * (r - 6)
            d.ellipse((x - r * .6, y - r * .6, x + r * .6, y + r * .6), fill=170, outline=60, width=3)
    return Image.blend(img.filter(ImageFilter.GaussianBlur(1)), fractal(seed, .5), .3)


# ---------------------------------------------------------------------------- matiere d'un calque

class Layer:
    def __init__(self, mask, color, kind='organic', seed=0, bevel=14, spec=0.6, glow=None, alpha=255, light=None, dark=None):
        self.mask = mask
        self.color = hexrgb(color) if isinstance(color, str) else color
        self.kind = kind
        self.seed = seed
        self.bevel = bevel
        self.spec = spec
        self.glow = hexrgb(glow) if isinstance(glow, str) else glow
        self.alpha = alpha
        self.light = hexrgb(light) if isinstance(light, str) else light
        self.dark = hexrgb(dark) if isinstance(dark, str) else dark


def shade_map(mask, bevel):
    """Eclairage d'un relief obtenu en floutant le masque (lumiere en haut a gauche)."""
    h = mask.filter(ImageFilter.GaussianBlur(bevel))
    k = ImageFilter.Kernel((3, 3), [-2, -1, 0, -1, 0, 1, 0, 1, 2], scale=1, offset=128)
    e = h.filter(k)
    # renforce le contraste du relief
    return e.point(lambda v: max(0, min(255, int(128 + (v - 128) * 3.2))))


def paint(layer):
    m = layer.mask
    c = layer.color
    metallic = layer.kind in ('metal', 'gold')
    light = layer.light or lighten(c, .38 if metallic else .55)
    dark = layer.dark or darken(c, .72 if metallic else .65)
    shade = shade_map(m, layer.bevel)
    tex = TEXTURES[layer.kind](layer.seed)
    grad = Image.linear_gradient('L').resize((S, S)).point(lambda v: 255 - v)       # clair en haut
    lum = Image.blend(Image.blend(shade, tex, .5 if layer.kind not in ('gem', 'crystal') else .6), grad, .15)
    col = ImageOps.colorize(lum, black=dark, white=light, mid=c).convert('RGBA')
    # reflets speculaires
    if layer.spec > 0:
        hi = shade.point(lambda v: 255 if v > 205 else (int((v - 175) * 8.5) if v > 175 else 0))
        if layer.kind in ('gem', 'crystal'):
            hi = ImageChops.lighter(hi, tex.point(lambda v: 255 if v > 220 else 0))
        hi = ImageChops.multiply(hi, m).filter(ImageFilter.GaussianBlur(1.2))
        white = Image.new('RGBA', (S, S), (255, 255, 255, 0))
        white.putalpha(hi.point(lambda v: int(v * layer.spec)))
        col.alpha_composite(white)
    # lueur interne (objets magiques)
    if layer.glow:
        inner = m.filter(ImageFilter.GaussianBlur(18))
        gl = Image.new('RGBA', (S, S), layer.glow + (0,))
        gl.putalpha(ImageChops.multiply(inner, m).point(lambda v: int(v * .55)))
        col.alpha_composite(gl)
    col.putalpha(m.point(lambda v: v * layer.alpha // 255))
    return col


def compose(layers, outline='#16101c', halo=None, sparkle=False):
    """Assemble les calques et reduit a 64 px avec contour et ombre portee."""
    art = Image.new('RGBA', (S, S))
    union = Image.new('L', (S, S), 0)
    for layer in layers:
        art.alpha_composite(paint(layer))
        union = ImageChops.lighter(union, layer.mask)
    small = art.resize((OUT, OUT), Image.LANCZOS)
    um = union.resize((OUT, OUT), Image.LANCZOS).point(lambda v: 255 if v > 90 else 0)
    out = Image.new('RGBA', (OUT, OUT))
    if halo:
        hc = hexrgb(halo)
        hl = Image.new('RGBA', (OUT, OUT), hc + (0,))
        hl.putalpha(um.filter(ImageFilter.GaussianBlur(3)).point(lambda v: int(v * .75)))
        out.alpha_composite(hl)
    shadow = Image.new('RGBA', (OUT, OUT), (0, 0, 0, 0))
    shifted = Image.new('L', (OUT, OUT), 0)
    shifted.paste(um, (1, 2))
    shadow.putalpha(shifted.point(lambda v: int(v * .45)))
    out.alpha_composite(shadow)
    ol = Image.new('RGBA', (OUT, OUT), hexrgb(outline) + (0,))
    ol.putalpha(um.filter(ImageFilter.MaxFilter(3)))
    out.alpha_composite(ol)
    small.putalpha(ImageChops.multiply(small.getchannel('A'), um))
    out.alpha_composite(small)
    if sparkle:
        d = ImageDraw.Draw(out)
        for (x, y, r) in ((50, 10, 3), (12, 46, 2)):
            d.line([(x - r, y), (x + r, y)], fill=(255, 255, 240, 230))
            d.line([(x, y - r), (x, y + r)], fill=(255, 255, 240, 230))
    return out.filter(ImageFilter.UnsharpMask(radius=1, percent=60, threshold=2))


# ---------------------------------------------------------------------------- outils de dessin de masques

def new_mask():
    m = Image.new('L', (S, S), 0)
    return m, ImageDraw.Draw(m)


def poly(points, smooth=0):
    m, d = new_mask()
    d.polygon(points, fill=255)
    return m.filter(ImageFilter.GaussianBlur(smooth)).point(lambda v: 255 if v > 127 else 0) if smooth else m


def ellipse(box):
    m, d = new_mask()
    d.ellipse(box, fill=255)
    return m


def union(*masks):
    out = masks[0]
    for x in masks[1:]:
        out = ImageChops.lighter(out, x)
    return out


def minus(a, b):
    return ImageChops.subtract(a, b)


def blob(seed, cx, cy, r, bumps=7, amp=0.22):
    """Forme organique irreguliere."""
    rnd = random.Random(seed)
    ph = [rnd.uniform(0, 2 * math.pi) for _ in range(3)]
    pts = []
    for i in range(72):
        t = i / 72 * 2 * math.pi
        rr = r * (1 + amp * math.sin(bumps * t + ph[0]) * .6 + amp * .4 * math.sin((bumps + 3) * t + ph[1]))
        pts.append((cx + math.cos(t) * rr, cy + math.sin(t) * rr))
    return poly(pts)


def thick_line(points, width):
    m, d = new_mask()
    d.line(points, fill=255, width=width, joint='curve')
    for x, y in (points[0], points[-1]):
        d.ellipse((x - width / 2, y - width / 2, x + width / 2, y + width / 2), fill=255)
    return m
