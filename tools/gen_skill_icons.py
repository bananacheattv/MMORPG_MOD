"""
Icones de competences (32 x 32) dessinees en vectoriel a 128 px puis reduites :
fond degrade a la couleur de la classe, symbole avec ombre portee, halo, degrade, contour et reflet,
cadre biseaute (dore pour les ultimes, medaillon pour les passifs) et pastille d'effet secondaire.

Appele par generate_assets.gui_icons() ; ecrit aussi une planche d'apercu art/apercu_icones_competences.png.
"""
import math
import os

from PIL import Image, ImageChops, ImageDraw, ImageFilter

S = 128  # taille de travail


def _hex(c):
    c = c.lstrip('#')
    return tuple(int(c[i:i + 2], 16) for i in (0, 2, 4))


def _mix(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def _poly(d, pts, s=1.0, ox=0, oy=0):
    d.polygon([(ox + x * s, oy + y * s) for x, y in pts], fill=255)


# ---------------------------------------------------------------------------- formes (masques 128 px)

def _blade(d, x0, y0, x1, y1, w):
    """Lame de x0,y0 (garde) vers x1,y1 (pointe)."""
    ang = math.atan2(y1 - y0, x1 - x0)
    nx, ny = -math.sin(ang) * w, math.cos(ang) * w
    tipx, tipy = x1, y1
    bx, by = x1 - math.cos(ang) * w * 2.2, y1 - math.sin(ang) * w * 2.2
    d.polygon([(x0 + nx, y0 + ny), (bx + nx, by + ny), (tipx, tipy), (bx - nx, by - ny), (x0 - nx, y0 - ny)], fill=255)


def _sword(d, x0, y0, x1, y1, w=7):
    _blade(d, x0, y0, x1, y1, w)
    ang = math.atan2(y1 - y0, x1 - x0)
    gx, gy = math.cos(ang + math.pi / 2) * 20, math.sin(ang + math.pi / 2) * 20
    d.line([(x0 - gx, y0 - gy), (x0 + gx, y0 + gy)], fill=255, width=10)
    hx, hy = x0 - math.cos(ang) * 20, y0 - math.sin(ang) * 20
    d.line([(x0, y0), (hx, hy)], fill=255, width=9)
    d.ellipse((hx - 7, hy - 7, hx + 7, hy + 7), fill=255)


def _arrow(d, x0, y0, x1, y1, w=5, head=16):
    ang = math.atan2(y1 - y0, x1 - x0)
    d.line([(x0, y0), (x1 - math.cos(ang) * head, y1 - math.sin(ang) * head)], fill=255, width=w)
    a1, a2 = ang + 2.6, ang - 2.6
    d.polygon([(x1, y1), (x1 + math.cos(a1) * head * 1.2, y1 + math.sin(a1) * head * 1.2),
               (x1 + math.cos(a2) * head * 1.2, y1 + math.sin(a2) * head * 1.2)], fill=255)
    for k in (0, 1):                                    # empennage
        fx, fy = x0 + math.cos(ang) * k * 9, y0 + math.sin(ang) * k * 9
        d.line([(fx, fy), (fx + math.cos(ang + 2.4) * 12, fy + math.sin(ang + 2.4) * 12)], fill=255, width=4)
        d.line([(fx, fy), (fx + math.cos(ang - 2.4) * 12, fy + math.sin(ang - 2.4) * 12)], fill=255, width=4)


def _flame(d, cx, cy, r):
    """Flamme : goutte principale et deux langues laterales, pointes vers le haut."""
    def tongue(x, y, rr, tipx, tipy):
        d.ellipse((x - rr, y - rr, x + rr, y + rr), fill=255)
        d.polygon([(x - rr * .95, y - rr * .2), (tipx, tipy), (x + rr * .95, y - rr * .2)], fill=255)
    k = r / 30.0
    tongue(cx, cy + 10 * k, 26 * k, cx + 4 * k, cy - 52 * k)
    tongue(cx - 20 * k, cy + 14 * k, 15 * k, cx - 30 * k, cy - 26 * k)
    tongue(cx + 20 * k, cy + 14 * k, 15 * k, cx + 32 * k, cy - 20 * k)


def _star(d, cx, cy, r1, r2, n=5, rot=-math.pi / 2):
    pts = []
    for i in range(n * 2):
        r = r1 if i % 2 == 0 else r2
        a = rot + i * math.pi / n
        pts.append((cx + math.cos(a) * r, cy + math.sin(a) * r))
    d.polygon(pts, fill=255)


def _shield(d, cx, cy, w, h):
    d.polygon([(cx - w, cy - h), (cx + w, cy - h), (cx + w, cy + h * 0.15), (cx, cy + h), (cx - w, cy + h * 0.15)], fill=255)


def _bolt(d, cx, cy, s):
    _poly(d, [(8, -40), (-16, 4), (0, 4), (-10, 40), (18, -8), (2, -8), (12, -40)], s, cx, cy)


def mask(glyph):
    m = Image.new('L', (S, S), 0)
    d = ImageDraw.Draw(m)
    c = S / 2
    if glyph == 'sword':
        _sword(d, 40, 88, 100, 28)
    elif glyph == 'crossed':
        _sword(d, 38, 88, 98, 28, 6)
        _sword(d, 90, 88, 30, 28, 6)
    elif glyph == 'drop':
        d.ellipse((34, 50, 94, 110), fill=255)
        d.polygon([(64, 14), (38, 70), (90, 70)], fill=255)
    elif glyph == 'heal':
        d.rounded_rectangle((50, 22, 78, 106), 8, fill=255)
        d.rounded_rectangle((22, 50, 106, 78), 8, fill=255)
    elif glyph == 'snow':
        for k in range(6):
            a = k * math.pi / 3
            x, y = c + math.cos(a) * 46, c + math.sin(a) * 46
            d.line([(c, c), (x, y)], fill=255, width=9)
            for f in (0.55, 0.8):
                bx, by = c + math.cos(a) * 46 * f, c + math.sin(a) * 46 * f
                for s_ in (-1, 1):
                    d.line([(bx, by), (bx + math.cos(a + s_ * 0.8) * 13, by + math.sin(a + s_ * 0.8) * 13)], fill=255, width=6)
        d.ellipse((c - 11, c - 11, c + 11, c + 11), fill=255)
    elif glyph == 'flame':
        _flame(d, c, 72, 30)
    elif glyph == 'spiral':
        pts = []
        for i in range(140):
            t = i / 140 * 4.2 * math.pi
            r = 6 + t * 3.4
            pts.append((c + math.cos(t) * r, c + math.sin(t) * r))
        d.line(pts, fill=255, width=11, joint='curve')
    elif glyph == 'star':
        _star(d, c, c + 2, 50, 21)
    elif glyph == 'pierce':
        _arrow(d, 18, 64, 112, 64, 7, 20)
        d.ellipse((52, 46, 76, 82), outline=255, width=6)
    elif glyph == 'arrow':
        _arrow(d, 26, 102, 104, 24, 7, 20)
    elif glyph == 'back':
        _arrow(d, 100, 64, 22, 64, 9, 22)
        for k in (0, 1):
            d.line([(84 + k * 14, 30), (96 + k * 14, 40)], fill=255, width=5)
            d.line([(84 + k * 14, 98), (96 + k * 14, 88)], fill=255, width=5)
    elif glyph == 'dash':
        _arrow(d, 40, 64, 112, 64, 12, 22)
        for k, y in enumerate((34, 64, 94)):
            d.line([(12 + (k % 2) * 10, y), (40 + (k % 2) * 10, y)], fill=255, width=7)
    elif glyph == 'taunt':
        d.ellipse((28, 20, 100, 92), fill=255)
        d.rectangle((48, 84, 80, 106), fill=255)
        d.ellipse((42, 44, 58, 60), fill=0)
        d.ellipse((70, 44, 86, 60), fill=0)
        d.polygon([(64, 64), (58, 76), (70, 76)], fill=0)
    elif glyph == 'fist':
        d.rounded_rectangle((32, 34, 96, 96), 18, fill=255)
        for k in range(4):
            d.line([(40 + k * 15, 36), (40 + k * 15, 56)], fill=0, width=3)
        d.rounded_rectangle((22, 52, 46, 84), 10, fill=255)
        d.rectangle((44, 94, 84, 112), fill=255)
    elif glyph == 'shield':
        _shield(d, c, 60, 40, 48)
    elif glyph == 'shout':
        d.polygon([(22, 50), (46, 50), (72, 26), (72, 102), (46, 78), (22, 78)], fill=255)
        for k, r in enumerate((20, 34)):
            d.arc((74 - r, 64 - r, 74 + r, 64 + r), -45, 45, fill=255, width=7)
    elif glyph == 'bolt':
        _bolt(d, c, c, 1.25)
    elif glyph == 'blink':
        _star(d, 40, 40, 24, 8, 4, 0)
        _star(d, 88, 88, 30, 10, 4, 0)
        d.line([(48, 48), (80, 80)], fill=255, width=6)
    elif glyph == 'bubble':
        d.ellipse((18, 18, 110, 110), fill=255)
        d.ellipse((30, 30, 98, 98), fill=0)
        _shield(d, c, 62, 22, 26)
    elif glyph == 'comet':
        d.ellipse((70, 14, 112, 56), fill=255)
        d.polygon([(76, 50), (16, 112), (64, 64), (78, 22)], fill=255)
    elif glyph == 'rain':
        for k, (x, y) in enumerate(((30, 20), (64, 30), (98, 16), (46, 62), (84, 66))):
            _arrow(d, x + 8, y, x - 4, y + 44, 5, 12)
    elif glyph == 'eye':
        d.ellipse((12, 36, 116, 92), fill=255)
        d.ellipse((40, 36, 88, 92), fill=0)
        d.ellipse((50, 46, 78, 74), fill=255)
    elif glyph == 'explode':
        _star(d, c, c, 54, 24, 8, -math.pi / 8)
        d.ellipse((46, 46, 82, 82), fill=0)
        d.ellipse((54, 54, 74, 74), fill=255)
    elif glyph == 'multi':
        for a in (-0.45, 0, 0.45):
            _arrow(d, c - math.sin(a) * 6, 110, c + math.sin(a) * 60, 110 - math.cos(a) * 92, 5, 15)
    elif glyph == 'tower':
        d.rectangle((36, 40, 92, 110), fill=255)
        for k in range(4):
            d.rectangle((32 + k * 18, 22, 44 + k * 18, 42), fill=255)
        d.rounded_rectangle((56, 78, 72, 110), 6, fill=0)
    elif glyph == 'waves':
        for k, r in enumerate((16, 32, 48)):
            d.arc((c - r * 1.2, 100 - r, c + r * 1.2, 100 + r), 200, 340, fill=255, width=8)
        d.rectangle((14, 100, 114, 110), fill=255)
    elif glyph == 'crown':
        d.polygon([(18, 96), (18, 40), (42, 66), (64, 28), (86, 66), (110, 40), (110, 96)], fill=255)
        d.rectangle((18, 96, 110, 108), fill=255)
    elif glyph == 'staff':
        d.line([(34, 112), (82, 42)], fill=255, width=10)
        d.ellipse((70, 14, 112, 56), fill=255)
    elif glyph == 'bow':
        d.arc((22, 14, 110, 114), 110, 250, fill=255, width=11)
        d.line([(50, 22), (50, 106)], fill=255, width=3)
        _arrow(d, 36, 64, 112, 64, 5, 14)
    else:
        d.ellipse((24, 24, 104, 104), fill=255)
    return m


# ---------------------------------------------------------------------------- composition

def _colored(m, rgb, alpha=255):
    layer = Image.new('RGBA', (S, S), rgb + (0,))
    layer.putalpha(m.point(lambda v: v * alpha // 255))
    return layer


def _gradient(m, top, bottom):
    g = Image.new('RGBA', (S, S))
    gd = ImageDraw.Draw(g)
    for y in range(S):
        gd.line([(0, y), (S, y)], fill=_mix(top, bottom, y / S) + (255,))
    g.putalpha(m)
    return g


def render(glyph, main, accent, cls_color, size=32, ultimate=False, passive=False, effect=None):
    light, dark = _hex(cls_color[0]), _hex(cls_color[1])
    M, A = _hex(main), _hex(accent)
    img = Image.new('RGBA', (S, S))
    bd = ImageDraw.Draw(img)
    # fond : degrade radial a la couleur de la classe + vignette
    for r in range(S // 2 * 14 // 10, 0, -2):
        t = r / (S * 0.7)
        bd.ellipse((S / 2 - r, S / 2 - r - 10, S / 2 + r, S / 2 + r - 10), fill=_mix(light, dark, min(1, t * 1.15)) + (255,))
    rays = Image.new('L', (S, S), 0)
    rd = ImageDraw.Draw(rays)
    for k in range(12):
        a = k * math.pi / 6
        rd.polygon([(S / 2, S / 2), (S / 2 + math.cos(a - .1) * S, S / 2 + math.sin(a - .1) * S),
                    (S / 2 + math.cos(a + .1) * S, S / 2 + math.sin(a + .1) * S)], fill=26)
    img.alpha_composite(_colored(rays, A))
    m = mask(glyph)
    # halo, ombre portee, contour, remplissage degrade, reflet
    img.alpha_composite(_colored(m.filter(ImageFilter.GaussianBlur(9)), A, 200))
    img.alpha_composite(_colored(ImageChops.offset(m, 4, 5).filter(ImageFilter.GaussianBlur(2)), (0, 0, 0), 170))
    outline = m.filter(ImageFilter.MaxFilter(9))
    img.alpha_composite(_colored(outline, _mix(dark, (0, 0, 0), .55)))
    img.alpha_composite(_gradient(m, _mix(M, (255, 255, 255), .35), _mix(M, A, .45)))
    shine = ImageChops.subtract(m, ImageChops.offset(m, 5, 6)).filter(ImageFilter.GaussianBlur(1))
    img.alpha_composite(_colored(shine, (255, 255, 255), 150))
    inner = ImageChops.subtract(m, ImageChops.offset(m, -5, -6))
    img.alpha_composite(_colored(inner, _mix(M, dark, .5), 120))
    # cadre biseaute
    fd = ImageDraw.Draw(img)
    if passive:
        ring = Image.new('L', (S, S), 0)
        ImageDraw.Draw(ring).rounded_rectangle((2, 2, S - 3, S - 3), 30, outline=255, width=9)
        img.alpha_composite(_gradient(ring, (232, 236, 245), (110, 120, 140)))
        corner = Image.new('L', (S, S), 255)
        ImageDraw.Draw(corner).rounded_rectangle((0, 0, S - 1, S - 1), 34, fill=0)
        img.paste((0, 0, 0, 0), mask=corner)
    else:
        fr = Image.new('L', (S, S), 0)
        ImageDraw.Draw(fr).rectangle((0, 0, S - 1, S - 1), outline=255, width=8)
        top, bottom = ((255, 236, 150), (170, 110, 30)) if ultimate else (_mix(light, (255, 255, 255), .3), _mix(dark, (0, 0, 0), .4))
        img.alpha_composite(_gradient(fr, top, bottom))
        fd.rectangle((8, 8, S - 9, S - 9), outline=(0, 0, 0, 160), width=3)
        if ultimate:
            for (x, y) in ((4, 4), (S - 5, 4), (4, S - 5), (S - 5, S - 5)):
                fd.regular_polygon((x, y, 9), 4, fill=(255, 90, 60, 255), outline=(255, 240, 200, 255))
    # pastille d'effet secondaire (variantes)
    if effect:
        col = _hex(EFFECT_COLORS[effect])
        fd.ellipse((S - 50, S - 50, S - 8, S - 8), fill=(12, 14, 24, 235), outline=col + (255,), width=5)
        em = Image.new('L', (S, S), 0)
        ed = ImageDraw.Draw(em)
        cx, cy = S - 29, S - 29
        if effect in ('heal', 'mana'):
            ed.rectangle((cx - 3, cy - 12, cx + 3, cy + 12), fill=255)
            ed.rectangle((cx - 12, cy - 3, cx + 12, cy + 3), fill=255)
        elif effect in ('pull', 'push', 'speed'):
            sgn = -1 if effect == 'pull' else 1
            ed.line([(cx - 8 * sgn, cy - 11), (cx + 6 * sgn, cy), (cx - 8 * sgn, cy + 11)], fill=255, width=6)
        elif effect == 'slow':
            for a in range(3):
                ang = a * math.pi / 3
                ed.line([(cx - math.cos(ang) * 12, cy - math.sin(ang) * 12), (cx + math.cos(ang) * 12, cy + math.sin(ang) * 12)], fill=255, width=4)
        elif effect == 'guard':
            _shield(ed, cx, cy - 1, 10, 13)
        elif effect in ('poison', 'fire'):
            _flame(ed, cx, cy + 3, 8)
        else:
            ed.line([(cx + 10, cy - 12), (cx - 8, cy + 12)], fill=255, width=6)
        img.alpha_composite(_colored(em, col))
    return img.resize((size, size), Image.LANCZOS)


EFFECT_COLORS = {'heal': '#75ef88', 'slow': '#80e4ff', 'poison': '#a0e83c', 'fire': '#ff953f',
                 'mana': '#75aaff', 'pull': '#ce91ff', 'guard': '#82c7ff', 'break': '#ffc070',
                 'strike': '#ff6772', 'push': '#ffcf83', 'speed': '#96f2c3'}


def preview(images, out):
    cols = 11
    rows = (len(images) + cols - 1) // cols
    sheet = Image.new('RGBA', (cols * 72 + 8, rows * 72 + 8), (24, 20, 30, 255))
    for i, im in enumerate(images):
        sheet.alpha_composite(im.resize((64, 64), Image.NEAREST), (8 + (i % cols) * 72, 8 + (i // cols) * 72))
    os.makedirs(os.path.dirname(out), exist_ok=True)
    sheet.save(out)
