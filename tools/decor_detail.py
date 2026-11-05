"""Passe de details automatique : cadres en relief sur les grandes faces (pierre, marbre, bois, metal).

Donne du relief aux volumes simples : chaque grande face recoit un panneau encadre, en teinte plus sombre.
"""
DARK = {'stone': ('stone', '#6f737b'), 'marble': ('marble', '#c9c2b4'), 'wood': ('wood', '#4a2e1a'), 'metal': ('metal', '#2e3037')}


def mouldings(m):
    """Plinthe et corniche sur les grands volumes : le meme vocabulaire d'architecture sur tous les decors."""
    for b in list(m.boxes):
        if any(b['rotation']) or b['mat'][0] not in ('stone', 'marble', 'wood'):
            continue
        (x0, y0, z0), (x1, y1, z1) = b['from'], b['to']
        if x1 - x0 < 12 or z1 - z0 < 12 or y1 - y0 < 18:
            continue
        dark = DARK[b['mat'][0]]
        m.box('root', (x0 - 1.2, y0, z0 - 1.2), (x1 + 1.2, y0 + 2.4, z1 + 1.2), dark)
        m.box('root', (x0 - 1.4, y1 - 2, z0 - 1.4), (x1 + 1.4, y1, z1 + 1.4), dark)
        m.box('root', (x0 - .7, y1 - 3.4, z0 - .7), (x1 + .7, y1 - 2, z1 + .7), b['mat'])


def frames(m, min_size=14.0, inset=2.2, strip=1.2, relief=.8):
    for b in list(m.boxes):
        if any(b['rotation']):
            continue
        kind = b['mat'][0]
        if kind not in DARK:
            continue
        dark = DARK[kind]
        (x0, y0, z0), (x1, y1, z1) = b['from'], b['to']
        for side, w, h in (('n', x1 - x0, y1 - y0), ('s', x1 - x0, y1 - y0), ('w', z1 - z0, y1 - y0), ('e', z1 - z0, y1 - y0)):
            if w < min_size or h < min_size:
                continue
            ins, st, r = inset, strip, relief
            ya, yb = y0 + ins, y1 - ins
            if side in 'ns':
                za, zb = (z0 - r, z0) if side == 'n' else (z1, z1 + r)
                xa, xb = x0 + ins, x1 - ins
                boxes = [((xa, yb - st, za), (xb, yb, zb)), ((xa, ya, za), (xb, ya + st, zb)),
                         ((xa, ya, za), (xa + st, yb, zb)), ((xb - st, ya, za), (xb, yb, zb))]
            else:
                xa, xb = (x0 - r, x0) if side == 'w' else (x1, x1 + r)
                za, zb = z0 + ins, z1 - ins
                boxes = [((xa, yb - st, za), (xb, yb, zb)), ((xa, ya, za), (xb, ya + st, zb)),
                         ((xa, ya, za), (xb, yb, za + st)), ((xa, ya, zb - st), (xb, yb, zb))]
            for frm, to in boxes:
                m.box('root', frm, to, dark)
