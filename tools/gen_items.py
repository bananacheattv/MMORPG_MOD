"""Generation procedurale des sprites d'objets (16x16)."""
import math
from pixel import Canvas, ramp, rgb, shade, mix, shaded_circle, circle, T

S0 = 15  # ligne diagonale centrale (x + y = 15)


def _st(x, y):
    return x + y, x - y


# ---------------------------------------------------------------------------
# Armes diagonales
# ---------------------------------------------------------------------------

def sword(blade, guard, grip, gem=None, rune=None, tip=13, base=-1, width=2, guard_len=3,
          handle=5, jagged=False, wings=False, glow=None):
    B, G, H = ramp(blade), ramp(guard), ramp(grip)
    E = ramp(gem) if gem else None
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            s, t = _st(x, y)
            d = s - S0
            # lame
            if base <= t <= tip:
                w = width
                if t >= tip - 1:
                    w = 1
                if 0 <= d < w + (1 if width >= 3 and t < tip - 3 else 0):
                    col = B['a'] if d == 0 else (B['c'] if d >= w - 1 + (1 if width >= 3 and t < tip - 3 else 0) else B['b'])
                    if d == 0 and t >= tip - 2:
                        col = B['w']
                    if rune and d == 1 and t % 3 == 0 and base + 1 < t < tip - 1:
                        col = rgb(rune)
                    cv.set(x, y, col, 'blade')
                elif jagged and (d == -1 or d == w) and t % 3 == 0 and base + 1 < t < tip - 1:
                    cv.set(x, y, B['b'], 'blade')
            # garde
            elif t in (base - 1, base - 2):
                if -guard_len <= d <= width - 1 + guard_len:
                    col = G['a'] if t == base - 1 else G['b']
                    if abs(d - (width - 1) / 2) >= guard_len and wings:
                        col = G['w']
                    if E and d in (0, 1) and t == base - 1:
                        col = E['a']
                    cv.set(x, y, col, 'guard')
                elif wings and t == base and (d == -guard_len - 1 or d == width + guard_len):
                    cv.set(x, y, G['a'], 'guard')
            # poignee
            elif base - 2 - handle <= t <= base - 3:
                if 0 <= d <= 1:
                    col = H['b'] if (t + d) % 2 == 0 else H['c']
                    cv.set(x, y, col, 'grip')
            # pommeau
            elif base - 4 - handle <= t <= base - 3 - handle:
                if -1 <= d <= 2:
                    col = (E['b'] if E else G['b']) if d in (0, 1) else G['c']
                    cv.set(x, y, col, 'pommel')
    cv.outline(shade(B['o'], 0.9))
    if glow:
        _sparkle(cv, glow)
    return cv


def axe(head, grip, edge=None, gem=None, double=False):
    Hd, Gp = ramp(head), ramp(grip)
    Ed = ramp(edge) if edge else Hd
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            s, t = _st(x, y)
            d = s - S0
            if -12 <= t <= 12 and 0 <= d <= 1:
                cv.set(x, y, Gp['b'] if d == 0 else Gp['c'], 'grip')
            # tete principale (cote haut-gauche, d < 0)
            dist = -d
            if 1 <= dist <= 6:
                half = 1 + dist // 2
                if abs(t - 7) <= half:
                    col = Hd['b'] if dist < 5 else Ed['a']
                    if dist == 6:
                        col = Ed['w']
                    if abs(t - 7) == half and dist < 5:
                        col = Hd['c']
                    cv.set(x, y, col, 'head')
            if double:
                dist2 = d - 1
                if 1 <= dist2 <= 5:
                    half = 1 + dist2 // 2
                    if abs(t - 7) <= half:
                        col = Hd['b'] if dist2 < 4 else Ed['a']
                        if abs(t - 7) == half and dist2 < 4:
                            col = Hd['c']
                        cv.set(x, y, col, 'head')
            elif 2 <= d <= 3 and 6 <= t <= 8:
                cv.set(x, y, Hd['c'], 'head')
    if gem:
        Gm = ramp(gem)
        for (x, y) in [(10, 4), (11, 4), (10, 5)]:
            pass
        _put_on_line(cv, 7, -1, Gm['a'])
    cv.outline(shade(Hd['o'], 0.9))
    return cv


def _put_on_line(cv, t, d, col):
    s = S0 + d
    if (s + t) % 2:
        s += 1
    x, y = (s + t) // 2, (s - t) // 2
    cv.set(x, y, col, 'gem')


def hammer(head, grip, band=None, gem=None, spikes=False, size=4, round_head=False):
    Hd, Gp = ramp(head), ramp(grip)
    Bd = ramp(band) if band else Hd
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            s, t = _st(x, y)
            d = s - S0
            if -12 <= t <= 7 and 0 <= d <= 1:
                cv.set(x, y, Gp['b'] if (t % 4) else Gp['c'], 'grip')
                if t in (-12, -11):
                    cv.set(x, y, Bd['b'], 'grip')
    if round_head:
        cx, cy = 11.5, 4.5
        shaded_circle(cv, cx, cy, size - 0.6, Hd)
        if spikes:
            for ang in range(0, 360, 45):
                px = int(cx + math.cos(math.radians(ang)) * (size + 0.4))
                py = int(cy + math.sin(math.radians(ang)) * (size + 0.4))
                cv.set(px, py, Hd['a'], 'spike')
    else:
        for y in range(16):
            for x in range(16):
                s, t = _st(x, y)
                d = s - S0
                if 6 <= t <= 12 and -size <= d <= size + 1:
                    if t == 12 or d == -size:
                        col = Hd['a']
                    elif t == 6 or d == size + 1:
                        col = Hd['c']
                    else:
                        col = Hd['b']
                    if t in (8, 9) and band:
                        col = Bd['b'] if (d % 2 == 0) else Bd['a']
                    cv.set(x, y, col, 'head')
        if spikes:
            for d in (-size - 1, size + 2):
                _put_on_line(cv, 9, d, Hd['w'])
    if gem:
        Gm = ramp(gem)
        _put_on_line(cv, 9, 0, Gm['a'])
        _put_on_line(cv, 9, 1, Gm['b'])
    cv.outline(shade(Hd['o'], 0.9))
    return cv


def staff(shaft, head_kind, head, gem=None, glow=None):
    Sh, Hd = ramp(shaft), ramp(head)
    Gm = ramp(gem) if gem else Hd
    cv = Canvas()
    for y in range(16):
        for x in range(16):
            s, t = _st(x, y)
            d = s - S0
            if -13 <= t <= 5 and 0 <= d <= 1:
                col = Sh['b'] if d == 0 else Sh['c']
                if t in (-3, 1) or t == -13:
                    col = Hd['c']
                cv.set(x, y, col, 'shaft')
    cx, cy = 12.0, 4.0
    if head_kind == 'orb':
        for ang in (200, 250, 340, 20):
            px = int(cx + math.cos(math.radians(ang)) * 3.6)
            py = int(cy + math.sin(math.radians(ang)) * 3.6)
            cv.set(px, py, Hd['b'], 'prong')
        shaded_circle(cv, cx, cy, 2.6, Gm)
    elif head_kind == 'crystal':
        rows = ["....w", "...wa", "..wab", ".wabc", "wabbc", ".abc.", "..c.."]
        pal = {'w': Gm['w'], 'a': Gm['a'], 'b': Gm['b'], 'c': Gm['c']}
        cv.template(rows, pal, 9, 0)
        cv.set(9, 6, Hd['b'], 'prong')
        cv.set(13, 4, Hd['b'], 'prong')
    elif head_kind == 'flame':
        shaded_circle(cv, cx, cy + 0.5, 2.3, Gm)
        fl = ramp('#ffb020')
        for (x, y) in [(12, 0), (11, 1), (13, 1), (14, 2), (10, 2), (12, 1)]:
            cv.set(x, y, fl['a'] if y < 2 else fl['b'], 'flame')
        cv.set(9, 6, Hd['b'], 'prong')
        cv.set(14, 6, Hd['b'], 'prong')
    elif head_kind == 'ring':
        for y in range(16):
            for x in range(16):
                dd = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                if 2.4 <= dd <= 3.6:
                    cv.set(x, y, Hd['a'] if y < cy else Hd['b'], 'ring')
        shaded_circle(cv, cx, cy, 1.6, Gm)
    elif head_kind == 'crescent':
        for y in range(16):
            for x in range(16):
                d1 = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                d2 = math.hypot(x + 0.5 - (cx + 1.6), y + 0.5 - (cy - 1.6))
                if d1 <= 3.8 and d2 > 3.0:
                    cv.set(x, y, Hd['a'] if d1 < 2.6 else Hd['b'], 'moon')
        shaded_circle(cv, cx + 0.5, cy - 0.5, 1.3, Gm)
    elif head_kind == 'star':
        pts = []
        for i in range(10):
            r = 4.2 if i % 2 == 0 else 1.9
            a = math.radians(-90 + i * 36)
            pts.append((cx + math.cos(a) * r, cy + math.sin(a) * r))
        for y in range(16):
            for x in range(16):
                if _point_in_poly(x + 0.5, y + 0.5, pts):
                    dd = math.hypot(x + 0.5 - cx, y + 0.5 - cy)
                    cv.set(x, y, Gm['w'] if dd < 1.3 else (Gm['a'] if dd < 2.5 else Gm['b']), 'star')
    cv.outline(shade(Sh['o'], 0.8))
    if glow:
        _sparkle(cv, glow)
    return cv


def _point_in_poly(x, y, pts):
    inside = False
    j = len(pts) - 1
    for i in range(len(pts)):
        xi, yi = pts[i]
        xj, yj = pts[j]
        if ((yi > y) != (yj > y)) and (x < (xj - xi) * (y - yi) / (yj - yi + 1e-9) + xi):
            inside = not inside
        j = i
    return inside


def _sparkle(cv, color):
    c = rgb(color)
    for (x, y) in [(1, 1), (2, 2), (0, 2), (2, 0)]:
        if not cv.filled(x, y):
            cv.set(x, y, c if (x, y) != (1, 1) else shade(c, 1.5), 'spark')


def _line(cv, x0, y0, x1, y1, col, role='x', thick=False):
    n = int(max(abs(x1 - x0), abs(y1 - y0)) * 2) + 1
    for i in range(n + 1):
        u = i / max(n, 1)
        x = x0 + (x1 - x0) * u
        y = y0 + (y1 - y0) * u
        cv.set(int(math.floor(x)), int(math.floor(y)), col, role)


def bow(wood, string='#e8e8e8', tip=None, gem=None, pull=0, arrow='#c8b090', recurve=False):
    W = ramp(wood)
    Tp = ramp(tip) if tip else W
    cv = Canvas()
    ad = (1 / math.sqrt(2), -1 / math.sqrt(2))  # direction de la fleche
    sd = (1 / math.sqrt(2), 1 / math.sqrt(2))   # direction de la corde
    C = (6.6, 9.4)
    L = 9.6
    bulge = 4.2
    pts = []
    for i in range(41):
        u = -1 + 2 * i / 40
        b = bulge * (1 - u * u)
        if recurve and abs(u) > 0.8:
            b -= (abs(u) - 0.8) * 6
        pts.append((C[0] + sd[0] * u * L + ad[0] * b, C[1] + sd[1] * u * L + ad[1] * b))
    for i, (x, y) in enumerate(pts):
        u = -1 + 2 * i / 40
        col = Tp['a'] if abs(u) > 0.82 else (W['a'] if u < 0 else W['b'])
        if abs(u) < 0.12 and gem:
            col = ramp(gem)['a']
        cv.set(int(x), int(y), col, 'wood')
        # epaisseur cote exterieur
        x2, y2 = x + ad[0] * 0.9, y + ad[1] * 0.9
        if abs(u) < 0.75:
            cv.set(int(x2), int(y2), W['c'] if abs(u) > 0.12 or not gem else ramp(gem)['b'], 'wood')
    a, b = pts[0], pts[-1]
    nock = (C[0] - ad[0] * pull, C[1] - ad[1] * pull)
    sc = rgb(string)
    if pull == 0:
        _line(cv, a[0], a[1], b[0], b[1], sc, 'string')
    else:
        _line(cv, a[0], a[1], nock[0], nock[1], sc, 'string')
        _line(cv, nock[0], nock[1], b[0], b[1], sc, 'string')
        ac = rgb(arrow)
        tipp = (nock[0] + ad[0] * 11.5, nock[1] + ad[1] * 11.5)
        _line(cv, nock[0] + ad[0], nock[1] + ad[1], tipp[0], tipp[1], ac, 'arrow')
        hx, hy = int(tipp[0]), int(tipp[1])
        cv.set(hx, hy, rgb('#d0d0d8'), 'arrow')
        cv.set(hx - 1, hy, rgb('#a0a0a8'), 'arrow')
        cv.set(hx, hy + 1, rgb('#a0a0a8'), 'arrow')
        fx, fy = int(nock[0]), int(nock[1])
        cv.set(fx - 1, fy, rgb('#f0f0f0'), 'arrow')
        cv.set(fx, fy + 1, rgb('#f0f0f0'), 'arrow')
    cv.outline(shade(W['o'], 0.9), skip_roles=('string', 'arrow'))
    return cv


# ---------------------------------------------------------------------------
# Armures (icones)
# ---------------------------------------------------------------------------

HELMET = [
    "................",
    "................",
    "................",
    ".....aaaaaa.....",
    "....awwbbbbb....",
    "...awbbbbbbbc...",
    "...abbbttbbbc...",
    "...abbbeebbbc...",
    "...abbbttbbbc...",
    "...attttttttc...",
    "...ab......bc...",
    "...ab......bc...",
    "...cc......cc...",
    "................",
    "................",
    "................",
]
CHEST = [
    "................",
    "..aab......bbc..",
    ".aawbt....tbbbc.",
    ".abbbbtttbbbbbc.",
    ".abbbbbbbbbbbbc.",
    ".cbbbbbeebbbbcc.",
    "..cabbbeebbbcc..",
    "...abbbbbbbbc...",
    "...abbttttbbc...",
    "...abbbbbbbbc...",
    "...abbbbbbbbc...",
    "...attttttttc...",
    "...abbbbbbbbc...",
    "....cccccccc....",
    "................",
    "................",
]
LEGS = [
    "................",
    "................",
    "...attttttttc...",
    "...abbbeebbbc...",
    "...abbbbbbbbc...",
    "...abbbccbbbc...",
    "...abbc..abbc...",
    "...abbc..abbc...",
    "...atbc..atbc...",
    "...abbc..abbc...",
    "...abbc..abbc...",
    "...abbc..abbc...",
    "...abbc..abbc...",
    "...cccc..cccc...",
    "................",
    "................",
]
BOOTS = [
    "................",
    "................",
    "................",
    "................",
    "................",
    "...abbc..abbc...",
    "...atbc..atbc...",
    "...abbc..abbc...",
    "...abbc..abbc...",
    "...abbc..abbc...",
    "..aabbc..abbcc..",
    ".aabbbc..abbbcc.",
    ".atttttc.atttttc",
    ".ccccccc.ccccccc",
    "................",
    "................",
]
ROBE_CHEST = [
    "................",
    "...aab....bbc...",
    "..awbbt..tbbbc..",
    ".abbbbbttbbbbbc.",
    ".abbbbbeebbbbbc.",
    ".cabbbbttbbbbcc.",
    "..abbbbbbbbbbc..",
    "..abbbbttbbbbc..",
    "..attttttttttc..",
    "..abbbbbbbbbbc..",
    ".abbbbbbbbbbbbc.",
    ".abbbbbbbbbbbbc.",
    ".abbbbttbbbbbbc.",
    ".cccccccccccccc.",
    "................",
    "................",
]
HOOD = [
    "................",
    "................",
    "......aab.......",
    ".....awbbb......",
    "....awbbbbc.....",
    "...awbbbbbbc....",
    "...abbbbbbbbc...",
    "...abbkkkkbbc...",
    "...abkkeekkbc...",
    "...abkkkkkkbc...",
    "...attkkkkttc...",
    "....abbbbbbc....",
    ".....cccccc.....",
    "................",
    "................",
    "................",
]


def armor_icon(piece, base, trim, gem, robe=False):
    P = ramp(base)
    Tr = ramp(trim)
    Gm = ramp(gem)
    pal = {'a': P['a'], 'b': P['b'], 'c': P['c'], 'w': P['w'], 't': Tr['b'], 'e': Gm['a'], 'k': rgb('#14101c')}
    rows = {'casque': HOOD if robe else HELMET, 'plastron': ROBE_CHEST if robe else CHEST,
            'jambieres': LEGS, 'bottes': BOOTS}[piece]
    cv = Canvas()
    cv.template(rows, pal)
    cv.outline(shade(P['o'], 0.85))
    return cv


# ---------------------------------------------------------------------------
# Materiaux & divers (gabarits ASCII)
# ---------------------------------------------------------------------------

TEMPLATES = {
    'ingot': [
        "................",
        "................",
        "................",
        "................",
        "................",
        "......awwww.....",
        "....awwaaaab....",
        "...aaaaabbbbc...",
        "..abbbbbbbbbcc..",
        "..abbbbbbbbcc...",
        "...cbbbbbbcc....",
        ".....cccccc.....",
        "................",
        "................",
        "................",
        "................",
    ],
    'shard': [
        "................",
        ".........w......",
        "........wa......",
        ".......wab......",
        "......waab......",
        "......aabbc.....",
        ".....wabbbc.....",
        ".....aabbbcc....",
        "....wabbbbc.....",
        "....aabbbcc.....",
        "....abbbbc......",
        ".....abbcc......",
        ".....abcc.......",
        "......cc........",
        "................",
        "................",
    ],
    'crystal': [
        "................",
        "......w.........",
        ".....wa....w....",
        ".....aab..wa....",
        "....wabc..aab...",
        "....aabc.wabc...",
        "....abbcaaabc...",
        "...wabbcabbcc...",
        "...aabbcabbc....",
        "...abbbcabbc.w..",
        "...abbbcabbcwa..",
        "...abbbcabbcab..",
        "..cccccccccccc..",
        "................",
        "................",
        "................",
    ],
    'dust': [
        "................",
        "................",
        "................",
        "................",
        "................",
        "................",
        "...........w....",
        ".......w........",
        "......aab.......",
        ".....aabbc...w..",
        "....aawbbbc.....",
        "...aabbbbbbc....",
        "..aabbbbbbbbc...",
        "..cccccccccccc..",
        "................",
        "................",
    ],
    'bone': [
        "................",
        "...........ww...",
        "..........wabw..",
        "..........abbc..",
        ".........abcc...",
        "........abc.....",
        ".......abc......",
        "......abc.......",
        ".....abc........",
        "....abc.........",
        "...abc..........",
        "..wabc..........",
        ".wabbc..........",
        ".abcc...........",
        "..c.............",
        "................",
    ],
    'fang': [
        "................",
        "................",
        "...www..........",
        "...wabb.........",
        "....abbb........",
        "....aabbc.......",
        ".....abbbc......",
        ".....aabbc......",
        "......abbbc.....",
        "......aabbc.....",
        ".......abbc.....",
        ".......aabc.....",
        "........abc.....",
        "........ac......",
        ".........c......",
        "................",
    ],
    'tusk': [
        "................",
        "................",
        "..........ww....",
        "..........wa....",
        "...........ab...",
        "...........ab...",
        "..........abb...",
        "..........abc...",
        ".........abbc...",
        "........aabc....",
        "......aabbc.....",
        "...eeaabbc......",
        "..eeeebcc.......",
        "..geeggc........",
        "...ggg..........",
        "................",
    ],
    'ear': [
        "................",
        "..w.............",
        "..aa............",
        "..abb...........",
        "...abb..........",
        "...abbb.........",
        "....abbbb.......",
        "....abeebb......",
        "....abeeebc.....",
        ".....abeebbc....",
        ".....abbbbbc....",
        "......cbbbbc....",
        ".......cbbc.....",
        "........cc......",
        "................",
        "................",
    ],
    'scrap': [
        "................",
        "................",
        "..aaab..........",
        "..awbbc.........",
        "..abbc..........",
        "...cc..aaaab....",
        "......awwbbbc...",
        "......abbbbc....",
        "...ab..cbbc.....",
        "..awbb..cc......",
        "..abbbc.........",
        "...cbc...aab....",
        ".........abbc...",
        "..........cc....",
        "................",
        "................",
    ],
    'fur': [
        "................",
        "................",
        "....aaab........",
        "...awbbbbb......",
        "..aabcbbbbbb....",
        "..abbbbcbbbbb...",
        "..abcbbbbbcbbc..",
        "...abbbcbbbbbc..",
        "...abbbbbbcbbc..",
        "....abcbbbbbbc..",
        "....abbbbcbbc...",
        ".....cbbbbbcc...",
        "......ccbbc.....",
        "........cc......",
        "................",
        "................",
    ],
    'cloth': [
        "................",
        "................",
        "................",
        "...aaaaaaaaab...",
        "..awwwwwwwwabc..",
        "..abbbbbbbbbbc..",
        "..aeeeeeeeeeec..",
        "..abbbbbbbbbbc..",
        "..abbbbbbbbbbc..",
        "..aeeeeeeeeeec..",
        "..abbbbbbbbbbc..",
        "..abbbbbbbbbbc..",
        "...cccccccccc...",
        "................",
        "................",
        "................",
    ],
    'leather': [
        "................",
        "................",
        "....aaaaaaab....",
        "...awbbbbbbbc...",
        "..aabebebebebc..",
        "..abbbbbbbbbbc..",
        "..abbbbbbbbbbc..",
        "..abbbbbbbbbbc..",
        "..abbbbbbbbbbc..",
        "..abbbbbbbbbbc..",
        "..abebebebebcc..",
        "...cbbbbbbbbc...",
        "....cccccccc....",
        "................",
        "................",
        "................",
    ],
    'orb': None,
    'core': None,
    'crown': [
        "................",
        "................",
        "................",
        "..w....w....w...",
        "..a...aea...a...",
        "..ab..abb..ab...",
        "..abbaabbbaabc..",
        "..abbbbbbbbbbc..",
        "..aebbbebbbebc..",
        "..abbbbbbbbbbc..",
        "..awwwwwwwwwwc..",
        "..cccccccccccc..",
        "................",
        "................",
        "................",
        "................",
    ],
    'heart': [
        "................",
        "................",
        "................",
        "...aaa...aab....",
        "..awwab.awabc...",
        "..awabbbabbbc...",
        "..aabbbbbbbbc...",
        "..abbbbbbbbbc...",
        "...abbbbbbbc....",
        "....abbbbbc.....",
        ".....abbbc......",
        "......abc.......",
        ".......c........",
        "................",
        "................",
        "................",
    ],
    'vial': [
        "................",
        "......gggg......",
        "......aggb......",
        ".......ab.......",
        ".......ab.......",
        "......awbb......",
        ".....awkkbb.....",
        "....awkeekbb....",
        "....akeeeekb....",
        "....akeeeekc....",
        "....akkeekkc....",
        ".....abkkbc.....",
        "......cccc......",
        "................",
        "................",
        "................",
    ],
    'potion': [
        "................",
        "................",
        "......gggg......",
        ".......gg.......",
        ".......ab.......",
        ".......ab.......",
        "......awbb......",
        ".....aweeeb.....",
        "....awfeeeeb....",
        "....afeeeeec....",
        "....aeeeeeec....",
        "....aeeeeeec....",
        ".....ceeeec.....",
        "......cccc......",
        "................",
        "................",
    ],
    'scroll': [
        "................",
        "................",
        "...gg...........",
        "..gaagbbbbbbb...",
        "..gaawwwwwwwbb..",
        "...gawbbbbbwbc..",
        "....awbeebbwbc..",
        "....awbbbbbwbc..",
        "....awbeeebwbc..",
        "....awbbbbbwbc..",
        "....awwwwwwwgg..",
        ".....bbbbbbbgaag",
        "............gaag",
        ".............gg.",
        "................",
        "................",
    ],
    'egg': [
        "................",
        "................",
        "......aaab......",
        ".....awwbbc.....",
        "....awabebbc....",
        "....aabbbbbc....",
        "...aaebbbbebc...",
        "...abbbbbbbbc...",
        "...abbbebbbbc...",
        "...abbbbbbebc...",
        "...aebbbbbbbc...",
        "....abbbebbc....",
        ".....cbbbbc.....",
        "......cccc......",
        "................",
        "................",
    ],
    'eye': [
        "................",
        "................",
        "................",
        "......aaab......",
        "....aawwbbbb....",
        "...awbbbbbbbc...",
        "..awbbeeeebbbc..",
        "..abbekkkkebbc..",
        "..abbekkkkebbc..",
        "..abbbeeeebbbc..",
        "...abbbbbbbbc...",
        "....cbbbbbbc....",
        "......cccc......",
        "................",
        "................",
        "................",
    ],
    'book': [
        "................",
        "................",
        "...aaaaaaaaab...",
        "..awwwwwwwwabc..",
        "..abbbbbbbbbgc..",
        "..abbbeebbbbgc..",
        "..abbeeeebbbgc..",
        "..abbbeebbbbgc..",
        "..abbbbbbbbbgc..",
        "..abbbbbbbbbgc..",
        "..abbbbbbbbbgc..",
        "..aggggggggggc..",
        "...cccccccccc...",
        "................",
        "................",
        "................",
    ],
    'flame': [
        "................",
        ".......w........",
        "......wa........",
        "......aab.......",
        ".....aabb...w...",
        "..w..abbb..wa...",
        "....aabeeb.ab...",
        "...aabeeeebb....",
        "...abeeffeebc...",
        "..aabeffffebc...",
        "..abeefwwfeebc..",
        "..abeefwwfeebc..",
        "...abeeffeebc...",
        "....cbbbbbbc....",
        ".....cccccc.....",
        "................",
    ],
    'stone': [
        "................",
        "................",
        "................",
        ".....aaaab......",
        "....awwbbbbc....",
        "...awbbbbbbbc...",
        "..aabbbebbbbbc..",
        "..abbbeeebbbbc..",
        "..abbbbebbbbbc..",
        "..abbbbebbbbcc..",
        "...abbbbbbbbc...",
        "....cbbbbbcc....",
        ".....ccccc......",
        "................",
        "................",
        "................",
    ],
    'coin': [
        "................",
        "................",
        "................",
        ".....aaaab......",
        "....awwwbbb.....",
        "...awaaaaabc....",
        "...awabbbbbc....",
        "...awabeebbc....",
        "...awabeebbc....",
        "...aaabbbbcc....",
        "....abbbbcc.....",
        ".....cccc.......",
        "................",
        "................",
        "................",
        "................",
    ],
    'chest': [
        "................",
        "................",
        "................",
        "...aaaaaaaaab...",
        "..awwwwwwwwwbc..",
        "..abbbbbbbbbbc..",
        "..aggggeegggbc..",
        "..ggggeffegggc..",
        "..abbbbeebbbbc..",
        "..abbbbbbbbbbc..",
        "..abbbbbbbbbbc..",
        "..agggggggggcc..",
        "...cccccccccc...",
        "................",
        "................",
        "................",
    ],
    'seal': [
        "................",
        "................",
        "................",
        "......aaab......",
        "....aawwbbbb....",
        "...awbbeebbbc...",
        "...abbeffebbc...",
        "..aabefwwfebcc..",
        "..aabefwwfebcc..",
        "...abbeffebbc...",
        "...abbbeebbbc...",
        "....cbbbbbbc....",
        ".....gg..gg.....",
        ".....gg..gg.....",
        "......g..g......",
        "................",
    ],
    'fragment': [
        "................",
        ".......w........",
        ".......a........",
        "......wab.......",
        "......aab.......",
        "..wwaaabbbbcc...",
        "...aabbbbbbc....",
        "....abbbbbc.....",
        "....abbbbbc.....",
        "...abbc.abbc....",
        "...abc...abc....",
        "..ac.......cc...",
        "................",
        "................",
        "................",
        "................",
    ],
}

PET_SPRITES = {
    'feu_follet': (["................",
                    "................",
                    "......aaa.......",
                    ".....awwab......",
                    "....awwwwab.....",
                    "....awkwkab.....",
                    "....abwwwbc.....",
                    ".....abbbc......",
                    "......abc.......",
                    ".......bc.......",
                    "........c.......",
                    ".........c......",
                    "................",
                    "................",
                    "................",
                    "................"], '#66e0ff', None),
    'slime': (["................",
               "................",
               "................",
               "................",
               "....aaaaaaab....",
               "...awwaaaaabc...",
               "...awbbbbbbbc...",
               "...abkkbbkkbc...",
               "...abkkbbkkbc...",
               "...abbbbbbbbc...",
               "...abbbkkbbbc...",
               "...abbbbbbbbc...",
               "...cccccccccc...",
               "................",
               "................",
               "................"], '#6ad850', None),
    'chouette': (["................",
                  "....a......a....",
                  "....aa....aa....",
                  "....abbbbbbb....",
                  "...abeebbeebc...",
                  "...abekbbekbc...",
                  "...abeebbeebc...",
                  "...abbbffbbbc...",
                  "...aabbffbbcc...",
                  "...abwbwbwbbc...",
                  "...abbwbwbwbc...",
                  "....abbbbbbc....",
                  ".....ff..ff.....",
                  "................",
                  "................",
                  "................"], '#a07848', '#ffd040'),
    'loup_spectral': (["................",
                       "................",
                       "...a.......a....",
                       "...aa.....aa....",
                       "...abaaaaabb....",
                       "..awbbbbbbbbc...",
                       "..abekbbbbekc...",
                       "..abbbbbbbbbc...",
                       "...abbbwwbbc....",
                       "....abwkkwc.....",
                       ".....abbbc......",
                       "......ccc.......",
                       "................",
                       "................",
                       "................",
                       "................"], '#8ab8ff', '#e0ffff'),
    'golem': (["................",
               "................",
               ".....aaaaab.....",
               ".....abeebc.....",
               ".....abbbbc.....",
               "...aaabbbbcbb...",
               "..aabbbbbbbbbc..",
               "..ab.abbbbc.bc..",
               "..ab.abbbbc.bc..",
               "..cc.abbbbc.cc..",
               ".....ab..bc.....",
               ".....ab..bc.....",
               ".....cc..cc.....",
               "................",
               "................",
               "................"], '#9a9488', '#40ff90'),
    'fee': (["................",
             "..ee........ee..",
             ".effe..aa..effe.",
             ".efffeawbbeffe..",
             "..efffabbbfffe..",
             "...eefabbcfee...",
             ".....abbbbc.....",
             "....ee.abc.ee...",
             "...effe.c.effe..",
             "...eeee...eeee..",
             "................",
             "................",
             "................",
             "................",
             "................",
             "................"], '#ffd8e8', '#ff70c0'),
    'phenix': (["................",
                "......f.........",
                ".....ffa........",
                ".....aawb.......",
                "....aakbbe......",
                "ff..abbbbc......",
                "fef.abbbbbc..fef",
                ".feeabbbbbbceef.",
                "..feeabbbbbcef..",
                "...feeabbbcef...",
                "....fe.abc.ef...",
                "......fefef.....",
                ".....f.f.f......",
                "................",
                "................",
                "................"], '#ffb030', '#ff4020'),
    'dragonnet': (["................",
                   "..a.........a...",
                   "..ab.......ab...",
                   "...abaaaaaab....",
                   "..awbbbbbbbbc...",
                   "..abekbbbekbc...",
                   "..abbbbbbbbbc...",
                   "ee.abbwwbbbc.ee.",
                   "eee.abbbbbc.eee.",
                   ".eeeabbbbbceee..",
                   "..e.abbbbbc.e...",
                   ".....ab.bc......",
                   ".....cc.cc.bbc..",
                   "................",
                   "................",
                   "................"], '#9050e0', '#ff60ff'),
}


def template_sprite(kind, base, accent=None, accent2=None, glow=None):
    P = ramp(base)
    A = ramp(accent) if accent else ramp(shade(P['b'], 1.3))
    A2 = ramp(accent2) if accent2 else A
    cv = Canvas()
    if kind == 'orb':
        shaded_circle(cv, 8, 8, 5.2, P)
        for (x, y) in [(6, 5), (6, 6), (7, 5)]:
            cv.set(x, y, P['w'], 'w')
        if accent:
            cv.set(9, 9, A['a'], 'e')
            cv.set(10, 10, A['b'], 'e')
            cv.set(8, 10, A['b'], 'e')
    elif kind == 'core':
        shaded_circle(cv, 8, 9, 4.6, P)
        fl = A
        for (x, y) in [(8, 2), (7, 3), (9, 3), (6, 4), (10, 4), (5, 3), (11, 3), (8, 3)]:
            cv.set(x, y, fl['a'], 'f')
        shaded_circle(cv, 8, 9, 2.0, A)
    else:
        pal = {'a': P['a'], 'b': P['b'], 'c': P['c'], 'w': P['w'],
               'e': A['b'], 'f': A['a'], 'g': A2['c'], 'k': rgb('#18101e')}
        if kind in ('potion', 'vial'):
            pal['a'] = rgb('#dfe9f0')
            pal['b'] = rgb('#b9c9d6')
            pal['c'] = rgb('#7f93a6')
            pal['w'] = rgb('#ffffff')
            pal['g'] = rgb('#8a6a40')
            pal['e'] = P['b']
            pal['f'] = P['a']
            pal['k'] = P['c']
        cv.template(TEMPLATES[kind], pal)
    cv.outline(shade(P['o'], 0.85))
    if glow:
        _sparkle(cv, glow)
    return cv


def pet_sprite(key):
    rows, base, acc = PET_SPRITES[key]
    P = ramp(base)
    A = ramp(acc) if acc else ramp(shade(P['b'], 1.4))
    pal = {'a': P['a'], 'b': P['b'], 'c': P['c'], 'w': P['w'], 'e': A['b'], 'f': A['a'],
           'k': rgb('#140e18')}
    cv = Canvas()
    cv.template(rows, pal)
    cv.outline(shade(P['o'], 0.8))
    return cv


def projectile_sprite(kind):
    cv = Canvas()
    if kind == 'feu':
        shaded_circle(cv, 8, 8, 5, ramp('#ff7a1a'))
        shaded_circle(cv, 7.5, 7.5, 2.6, ramp('#ffe070'))
    elif kind == 'givre':
        P = ramp('#8ad8ff')
        for i in range(-5, 6):
            cv.set(8 + i, 8, P['a'], 'x')
            cv.set(8, 8 + i, P['a'], 'x')
            if abs(i) < 4:
                cv.set(8 + i, 8 + i, P['b'], 'x')
                cv.set(8 + i, 8 - i, P['b'], 'x')
        shaded_circle(cv, 8.5, 8.5, 1.8, ramp('#e8fbff'))
    elif kind == 'arcane':
        shaded_circle(cv, 8, 8, 5, ramp('#b45cff'))
        shaded_circle(cv, 8, 8, 2.5, ramp('#f0c8ff'))
    elif kind == 'neant':
        shaded_circle(cv, 8, 8, 5.4, ramp('#3a1060'))
        shaded_circle(cv, 8, 8, 2.8, ramp('#c060ff'))
        cv.set(8, 8, rgb('#000000'), 'x')
    elif kind == 'ombre':
        shaded_circle(cv, 8, 8, 5, ramp('#30d070'))
        shaded_circle(cv, 8, 8, 2.4, ramp('#c8ffd8'))
    elif kind == 'eclair':
        P = ramp('#fff27a')
        pts = [(10, 1), (9, 2), (8, 3), (7, 4), (6, 5), (7, 6), (8, 7), (9, 7), (8, 8), (7, 9), (6, 10),
               (5, 11), (4, 12), (5, 6), (10, 6)]
        for x, y in pts:
            cv.set(x, y, P['a'], 'x')
            cv.set(x + 1, y, P['b'], 'x')
    cv.outline(rgb('#20101a') if kind != 'eclair' else rgb('#806010'))
    return cv


def spawn_egg(base, spots):
    B, S = ramp(base), ramp(spots)
    pal = {'a': B['a'], 'b': B['b'], 'c': B['c'], 'w': B['w'], 'e': S['b']}
    cv = Canvas()
    cv.template(TEMPLATES['egg'], pal)
    cv.outline(shade(B['o'], 0.8))
    return cv
