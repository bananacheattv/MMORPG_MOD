"""Aide : calcule les cubes d'une lame effilee (corps + arete + pointe en tranches fines) a coller dans create_cubes.
Usage : python3 tools/bbgen.py blade y0 y1 halfwidth tipLen [zhalf=.25] [n=10] [cx=8] [prefix=""]"""
import json, sys
def blade(y0, y1, hw, tip, zh=.25, n=10, cx=8, pre=''):
    c = [{'name': f'mt:{pre}lame', 'from': [cx - hw, y0, 8 - zh], 'to': [cx + hw, y1, 8 + zh]},
         {'name': f'hl:{pre}arete', 'from': [cx - hw * .16, y0, 8 - zh - .15], 'to': [cx + hw * .16, y1, 8 + zh + .15]}]
    dy = tip / n
    for i in range(n):
        t = (i + .5) / n
        w = hw * (1 - t) ** .85
        z = zh * (1 - .45 * t)
        a = y1 + i * dy
        c.append({'name': f'{"hl" if i == n - 1 else "mt"}:{pre}t{i}', 'from': [round(cx - w, 3), round(a, 3), round(8 - z, 3)], 'to': [round(cx + w, 3), round(a + dy, 3), round(8 + z, 3)]})
        if i < n * .5:
            r = hw * .16 * (1 - t)
            zr = zh + .15 * (1 - t)
            c.append({'name': f'hl:{pre}r{i}', 'from': [round(cx - r, 3), round(a, 3), round(8 - zr, 3)], 'to': [round(cx + r, 3), round(a + dy, 3), round(8 + zr, 3)]})
    return c
if __name__ == '__main__':
    a = sys.argv[2:]
    print(json.dumps(blade(*[float(x) if '.' in x else int(x) for x in a[:5]], *[(float(x) if '.' in x else int(x)) for x in a[5:8]]), separators=(',', ':')))

def compact(*a):
    import json
    return json.dumps([{**e, 'from': [round(x, 3) for x in e['from']], 'to': [round(x, 3) for x in e['to']]} for e in blade(*a)], separators=(',', ':'))


def bow(A=4.0, s=2.0, w=1.0, zh=.45, dirs=(-45, -45, -67.5, -67.5, -90, -90), pre='wd', tipw=.55):
    """Arc en diagonale (cordes haut-gauche -> bas-droite, ventre vers le haut-droite) : segments inclines (multiples de 22.5 deg).
    Retourne (cubes, tips). Le cube st:cord est la corde droite tendue entre les deux bouts."""
    import math
    ap = (8 + .7071 * A, 8 + .7071 * A)
    pts = [ap]
    for a in dirs:
        r = math.radians(a)
        pts.append((pts[-1][0] + s * math.cos(r), pts[-1][1] + s * math.sin(r)))
    mir = lambda p: (8 + (p[1] - 8), 8 + (p[0] - 8))
    cubes = []
    def seg(p, q, a, k, name):
        cx, cy = (p[0] + q[0]) / 2, (p[1] + q[1]) / 2
        n = len(dirs)
        ww = w * (1 - (1 - tipw) * k / n)
        z = zh * (1 - .35 * k / n)
        ln = s + .35
        if abs(a) <= 45:
            f, t, ang = [cx - ln / 2, cy - ww / 2, 8 - z], [cx + ln / 2, cy + ww / 2, 8 + z], a
        else:
            f, t, ang = [cx - ww / 2, cy - ln / 2, 8 - z], [cx + ww / 2, cy + ln / 2, 8 + z], a + 90
        e = {'name': name, 'from': [round(v, 3) for v in f], 'to': [round(v, 3) for v in t]}
        if ang:
            e['rotation'] = {'axis': 'z', 'angle': ang, 'origin': [round(cx, 3), round(cy, 3), 8]}
        cubes.append(e)
    for i, a in enumerate(dirs):
        seg(pts[i], pts[i + 1], a, i, f'{pre}:b{i}')
        ma = 90 - a
        if ma > 90: ma -= 180
        if ma <= -90: ma += 180
        seg(mir(pts[i]), mir(pts[i + 1]), ma if abs(ma) < 89.9 else (0 if abs(ma) == 180 else ma), i, f'{pre}:h{i}')
    t1, t2 = mir(pts[-1]), pts[-1]
    mx, my = (t1[0] + t2[0]) / 2, (t1[1] + t2[1]) / 2
    ln = math.dist(t1, t2)
    cubes.append({'name': 'st:cord', 'from': [round(mx - .12, 3), round(my - ln / 2, 3), 7.9], 'to': [round(mx + .12, 3), round(my + ln / 2, 3), 8.1], 'rotation': {'axis': 'z', 'angle': 45, 'origin': [round(mx, 3), round(my, 3), 8]}})
    return cubes, (t1, t2, ap)


def bowc(*a, **k):
    import json
    c, tips = bow(*a, **k)
    print(json.dumps(c, separators=(',', ':')))
    print('tips', [(round(x, 2), round(y, 2)) for x, y in tips])


def chain(start, angles, s=1.6, w0=1.2, w1=.6, zh=.45, pre='mt:a', z=8, mirror=False):
    """Chaine de segments contigus (aile, corne, queue) : angles = directions de marche en degres (multiples de 22.5).
    mirror=True : symetrique par rapport a x=8."""
    import math
    out = []
    x, y = start
    n = len(angles)
    for i, a in enumerate(angles):
        if mirror:
            a = 180 - a
        r = math.radians(a)
        nx, ny = x + s * math.cos(r), y + s * math.sin(r)
        cx, cy = (x + nx) / 2, (y + ny) / 2
        w = w0 + (w1 - w0) * i / max(1, n - 1)
        ang = ((a + 90) % 180) - 90
        ln = s + .3
        if abs(ang) <= 45:
            f, t, rot = [cx - ln / 2, cy - w / 2], [cx + ln / 2, cy + w / 2], ang
        else:
            rot = ang - 90 if ang > 0 else ang + 90
            f, t, rot = [cx - w / 2, cy - ln / 2], [cx + w / 2, cy + ln / 2], rot
        e = {'name': f'{pre}{i}', 'from': [round(f[0], 3), round(f[1], 3), z - zh], 'to': [round(t[0], 3), round(t[1], 3), z + zh]}
        if rot:
            e['rotation'] = {'axis': 'z', 'angle': rot, 'origin': [round(cx, 3), round(cy, 3), z]}
        out.append(e)
        x, y = nx, ny
    return out, (x, y)


def chainc(*a, **k):
    import json
    c, end = chain(*a, **k)
    print(json.dumps(c, separators=(',', ':')))
    print('end', end)
