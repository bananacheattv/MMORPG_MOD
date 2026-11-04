"""Finalise les armes modelisees dans Blockbench (MCP) : lit bbmodels/weapons/raw/<id>.json (export Blockbench brut,
cubes nommes « <matiere>:<piece> »), genere la palette de texture de l'arme (textures/item/weapons/<id>.png, 32x32,
16 cases de matiere) et ecrit models/item/sculpted_<id>.json (UV vers la case de matiere, display d'origine conserve).
Arcs : derive aussi les 3 modeles « pulling » (corde tiree + fleche) depuis le cube « st:cord ».
Usage : python3 tools/build_bb_weapons.py [id ...]"""
import colorsys
import json
import math
import random
import re
import numpy as np
import sys
import zlib
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources/assets/mmorpg'
RAW = RES / 'bbmodels/weapons/raw'
MODELS = RES / 'models/item'
TEX = RES / 'textures/item/weapons'
SLOTS = ['mt', 'md', 'hl', 'go', 'gd', 'wd', 'wk', 'lt', 'g1', 'g2', 'g3', 'ac', 'ad', 'bn', 'bk', 'st']
CELL = 8


def hx(s):
    s = s.lstrip('#')
    return tuple(int(s[i:i + 2], 16) for i in (0, 2, 4))


def mix(a, b, t):
    return tuple(round(a[i] + (b[i] - a[i]) * t) for i in range(3))


def shade(c, k):
    return tuple(max(0, min(255, round(v * k))) for v in c)


def theme(metal, accent, gem, wood, gold='#d8ae3c'):
    m, a, g, w, go = map(hx, (metal, accent, gem, wood, gold))
    return {
        'mt': m, 'md': shade(m, .62), 'hl': mix(m, (255, 255, 255), .55),
        'go': go, 'gd': shade(go, .62), 'wd': w, 'wk': shade(w, .6), 'lt': mix(shade(w, .55), (40, 24, 18), .4),
        'g1': g, 'g2': mix(g, (255, 255, 255), .6), 'g3': shade(g, .5),
        'ac': a, 'ad': shade(a, .6), 'bn': (232, 224, 204), 'bk': (28, 26, 34), 'st': (206, 210, 218)}


# metal, accent, gemme, bois/cuir, or  — plus l'arme est rare, plus la palette est vive.
T = {
    'epee_recrue': ('#b9bec7', '#7a4a2a', '#9aa7b8', '#7b5232'),
    'epee_milicien': ('#c3c8d0', '#8a2b2b', '#c9a030', '#6e4a2c'),
    'lame_runique': ('#aeb8cc', '#3a78d8', '#5fd0ff', '#5b4a3a', '#cfd5e0'),
    'lame_acier_trempe': ('#9fc3b4', '#2f6e55', '#6be0a4', '#4e3b28', '#c9d4cc'),
    'epee_berserker': ('#8e1f1f', '#b3262b', '#ff6a2a', '#3a2a24', '#7d1b1b'),
    'epee_croise': ('#e4e8f0', '#2f5fc8', '#ffe27a', '#f0f0f6', '#f2c94a'),
    'epee_seigneur_guerre': ('#5a5f73', '#8a1f3d', '#ff4d6d', '#2c2630', '#b68a2e'),
    'lame_tempetes': ('#a8c7ee', '#3ea0ff', '#d9f4ff', '#2e3a52', '#9ec3f0'),
    'lame_demoniaque': ('#3b1a40', '#c4143c', '#ff3355', '#1b0f22', '#8a1a1a'),
    'epee_roi_dragon': ('#c8b27a', '#cc3a1c', '#ffb02e', '#5c1d12', '#f0c040'),
    'excalibur_celeste': ('#f4f8ff', '#7fb5ff', '#fff3a8', '#ffffff', '#ffd34d'),
    'baton_apprenti': ('#a9a9b8', '#6a8fe0', '#7ab0ff', '#7b5232'),
    'baton_saule': ('#9bb28a', '#4c8f4a', '#8ae07a', '#6f5a34'),
    'sceptre_givre': ('#bfd8f0', '#4fa8e0', '#aef0ff', '#566a86', '#cfe6f8'),
    'sceptre_marees': ('#6cb4b0', '#2a7f8f', '#5ff0d8', '#3b5a5e', '#c8d8d0'),
    'baton_flammes': ('#5a2a1a', '#e2561a', '#ffb43a', '#3a2118', '#e09a30'),
    'baton_foudre': ('#6a6f9a', '#e8d630', '#fff58a', '#3b3f66', '#e8d630'),
    'baton_arcanique': ('#7a5ac0', '#b05cff', '#e0a0ff', '#40305e', '#d8c070'),
    'sceptre_ombre': ('#2a2238', '#7a30c0', '#c070ff', '#17121f', '#6a5a8a'),
    'sceptre_neant': ('#14101e', '#6a1fd0', '#a050ff', '#0a0812', '#7a5ab0'),
    'baton_phenix': ('#c0431c', '#ff7a1a', '#ffd23a', '#5a1a10', '#ffcf40'),
    'baton_archimage': ('#e8e4ff', '#8f7bff', '#fff0a0', '#f5f2ff', '#ffd34d'),
    'arc_chasseur': ('#8a6a3c', '#5a7a3a', '#c8b070', '#7b5a32'),
    'arc_court': ('#9a7440', '#6b4a2a', '#c8b070', '#7b5a32'),
    'arc_elfique': ('#a8c890', '#4c9a5a', '#d8f0a0', '#8a7a4a', '#d8c070'),
    'arc_composite': ('#8a5a34', '#c8b890', '#d0a050', '#5e3d22', '#b8a070'),
    'arc_tempete': ('#7a96b8', '#3e7ad0', '#cfe8ff', '#4a5a74', '#b8c8dc'),
    'arc_sylvain': ('#4f8a4a', '#2f6a35', '#9af07a', '#5b4a2a', '#c8d890'),
    'arc_faucon': ('#a4753c', '#f0e0b0', '#ffd05a', '#5a3d22', '#e0b040'),
    'arc_lune_argent': ('#d8dcee', '#8f9cf0', '#e8f0ff', '#8a90b0', '#e8ecf8'),
    'arc_spectral': ('#7fd6e8', '#3aa0c8', '#bff8ff', '#3a6a80', '#9ae0f0'),
    'arc_dragon': ('#8a2a1a', '#d84a1c', '#ffa63a', '#3a1a12', '#e8b040'),
    'arc_aube_divine': ('#fff2c8', '#ffb86a', '#fffbe0', '#f2e2b8', '#ffd34d'),
    'masse_garde': ('#8a8f9a', '#5a4a3a', '#8a8f9a', '#6b4a2c'),
    'masse_cloutee': ('#7a7f8a', '#4a4036', '#b0b4bc', '#5e4228'),
    'marteau_bastion': ('#9aa0ae', '#4a6a9a', '#7aa0d0', '#4e3b28', '#c0c6d2'),
    'marteau_forgeron': ('#6e7280', '#c4601c', '#ff9a3a', '#4a3524', '#b0782c'),
    'marteau_colosse': ('#7c7a74', '#9a6a3a', '#d8b878', '#43362a', '#b09860'),
    'masse_templier': ('#dcdfe8', '#c8a030', '#fff0a0', '#e4e4ee', '#f2c94a'),
    'masse_gardien': ('#e6e2d0', '#4a9ad8', '#aee0ff', '#c8c2a8', '#f0d060'),
    'marteau_runique': ('#5a6a90', '#40a0ff', '#7ae0ff', '#2c3550', '#b8c8e8'),
    'marteau_titan': ('#4a3a36', '#e8481a', '#ff8a2a', '#241a18', '#c8662a'),
    'marteau_rois_anciens': ('#8a7a50', '#a8302a', '#ffd23a', '#3a2c1a', '#f0c850'),
    'marteau_egide': ('#fff8e0', '#ffd24a', '#ffffff', '#e8e4d0', '#ffd34d'),
}
PVP = {'guerrier': ('#b02a2a', '#e04a3a'), 'mage': ('#a02a60', '#e0509a'), 'archer': ('#a0402a', '#e0804a'),
       'tank': ('#902a2a', '#d04a4a')}
PVP_BASE = {'guerrier': 'epee_croise', 'mage': 'baton_arcanique', 'archer': 'arc_faucon', 'tank': 'marteau_runique'}


def theme_for(wid):
    if wid in T:
        return theme(*T[wid])
    if wid.startswith('pvp_'):
        cls = wid.split('_')[1]
        lvl = int(wid.split('_')[2])
        base = list(T[PVP_BASE[cls]])
        m, g = PVP[cls]
        base[1] = m
        base[2] = g
        t = theme(*base)
        return {k: mix(v, (200, 40, 40), .12 + .04 * lvl) if k in ('mt', 'ac') else v for k, v in t.items()}
    raise KeyError(wid)


METAL = ('mt', 'md', 'hl', 'go', 'gd', 'st', 'bk', 'bn')
GEM = ('g1', 'g2', 'g3')


def shade_rect(slot, col, w, h, rnd, force_x=False):
    """Peint un rectangle local (w x h px) : degrade de section (rond), reflet, veinage, lueur des gemmes."""
    yy, xx = np.mgrid[0:h, 0:w].astype(float)
    a = (xx + .5) / w          # 0..1 en largeur
    b = (yy + .5) / h          # 0..1 en hauteur (0 = haut)
    long_is_y = True if force_x else h >= w
    s = a if long_is_y else b   # coordonnee sur le petit cote
    l = b if long_is_y else a   # coordonnee sur le grand cote
    lp = h if long_is_y else w
    if force_x:
        lp = 0
    k = 1.16 - .30 * s - .05 * min(1.0, lp / 64) * l
    if slot in ('mt', 'md', 'hl', 'st', 'go', 'gd'):
        k += .20 * np.exp(-((s - .26) / .09) ** 2)
        k -= .08 * np.exp(-((s - .92) / .06) ** 2)
    if slot in ('wd', 'wk', 'lt'):
        k += .05 * np.sin(l * (h if long_is_y else w) * .9 + rnd.uniform(0, 6)) + .04 * np.sin(s * 11)
    if slot in ('ac', 'ad'):
        k += .05 * np.sin(l * (h if long_is_y else w) * 1.3)
    if slot in GEM:
        r = np.hypot((a - .5) * 2, (b - .5) * 2)
        k = 1.35 - .75 * r + .12 * np.sin((a + b) * 7)
    k = k + rnd.normal(0, .02, k.shape)
    px = np.clip(np.array(col, float)[None, None, :] * k[..., None], 0, 255)
    if slot in GEM:
        px = np.clip(px + (np.maximum(0, 1.0 - np.hypot((a - .5) * 2, (b - .5) * 2))[..., None] ** 2) * 70, 0, 255)
    if slot not in ('mt', 'md', 'hl'):
        e = np.minimum(np.minimum(xx, w - 1 - xx), np.minimum(yy, h - 1 - yy))
        px = px * np.where(e < 1, .88, 1.0)[..., None]
    return px.astype(np.uint8)


def build_atlas(elements, th, wid, bow):
    """Texture peinte : projection plane (continue) pour les pieces droites, tuiles pour les pieces inclinees."""
    ppu = 16 if bow else 8
    cw = 16 * ppu
    ch = (16 if bow else 32) * ppu
    rnd = np.random.RandomState(zlib.crc32(wid.encode()) & 0xffffffff)
    pr = random.Random(zlib.crc32(wid.encode()))
    order = sorted(range(len(elements)), key=lambda i: (elements[i]['to'][2], i))
    tiles, place, x, y, rowh = [], {}, 0, 0, 0
    canvas = np.zeros((ch, cw, 4), np.uint8)
    for i in order:
        e = elements[i]
        slot = e['name'].split(':')[0]
        col = th[slot]
        fx = bool(re.match(r'^\w+:(t|r)\d+$', e['name']))
        f, t = e['from'], e['to']
        wpx = max(2, round((t[0] - f[0]) * ppu))
        hpx = max(2, round((t[1] - f[1]) * ppu))
        if 'rotation' not in e:
            x0 = round(f[0] * ppu)
            y0 = round((16 - t[1]) * ppu) if bow else round((32 - t[1]) * ppu)
            blk = shade_rect(slot, col, wpx, hpx, rnd, fx)
            ys, xs = max(0, y0), max(0, x0)
            ye, xe = min(ch, y0 + hpx), min(cw, x0 + wpx)
            if ye > ys and xe > xs:
                canvas[ys:ye, xs:xe, :3] = blk[ys - y0:ye - y0, xs - x0:xe - x0]
                canvas[ys:ye, xs:xe, 3] = 255
            place[i] = ('plane', x0, y0, wpx, hpx)
        else:
            if x + wpx + 1 > cw:
                x, y, rowh = 0, y + rowh + 1, 0
            tiles.append((x, y, shade_rect(slot, col, wpx, hpx, rnd, fx)))
            place[i] = ('tile', x, y, wpx, hpx)
            x += wpx + 1
            rowh = max(rowh, hpx)
    th_h = y + rowh
    H = ch + th_h
    tex = np.zeros((H, cw, 4), np.uint8)
    tex[:ch] = canvas
    for (tx, ty, blk) in tiles:
        tex[ch + ty:ch + ty + blk.shape[0], tx:tx + blk.shape[1], :3] = blk
        tex[ch + ty:ch + ty + blk.shape[0], tx:tx + blk.shape[1], 3] = 255
    # fond opaque sombre pour eviter les halos aux bords (les pixels non peints ne sont jamais echantillonnes)
    tex[tex[..., 3] == 0] = (0, 0, 0, 255)
    return Image.fromarray(tex, 'RGBA'), place, cw, H, ch


def finish(elements, wid, display, gui_light, bow):
    th = theme_for(wid)
    img, place, W, H, ch = build_atlas(elements, th, wid, bow)
    ppu = 16 if bow else 8
    out = []
    for i, e in enumerate(elements):
        kind, px, py, wp, hp = place[i]
        base = 0 if kind == 'plane' else ch
        def uv(x0, y0, x1, y1):
            return [round(x0 / W * 16, 4), round((base + y0) / H * 16, 4), round(x1 / W * 16, 4), round((base + y1) / H * 16, 4)]
        ne = {'name': e['name'], 'from': e['from'], 'to': e['to']}
        if 'rotation' in e:
            ne['rotation'] = e['rotation']
        ins = min(2, max(0, wp // 2 - 1), max(0, hp // 2 - 1))
        front = uv(px + ins, py + ins, px + wp - ins, py + hp - ins)
        back = [front[2], front[1], front[0], front[3]]
        cx, cy = px + wp // 2, py + hp // 2
        faces = {'south': front, 'north': back,
                 'up': uv(cx - 1, py + ins, cx + 1, py + ins + 1), 'down': uv(cx - 1, py + hp - ins - 1, cx + 1, py + hp - ins),
                 'west': uv(px + ins, cy - 1, px + ins + 1, cy + 1), 'east': uv(px + wp - ins - 1, cy - 1, px + wp - ins, cy + 1)}
        ne['faces'] = {f: {'uv': v, 'texture': '#t'} for f, v in faces.items()}
        out.append(ne)
    return img, {'credit': 'Aeloria · modele Blockbench', 'gui_light': gui_light,
                 'textures': {'t': f'mmorpg:item/weapons/{wid}', 'particle': '#t'}, 'elements': out, 'display': display}


def cube(name, a, b, rot=None, origin=None):
    e = {'name': name, 'from': [round(v, 3) for v in a], 'to': [round(v, 3) for v in b]}
    if rot:
        e['rotation'] = {'angle': rot[1], 'axis': rot[0], 'origin': origin or [round((a[i] + b[i]) / 2, 3) for i in range(3)]}
    return e


def line_cubes(name, p, q, th=.6, z=(7.75, 8.25)):
    """Segment pixelise p->q (plan xy) en petits cubes carres."""
    n = max(1, int(math.dist(p, q) / (th * .8)))
    out = []
    for i in range(n + 1):
        x = p[0] + (q[0] - p[0]) * i / n
        y = p[1] + (q[1] - p[1]) * i / n
        out.append(cube(name, (x - th / 2, y - th / 2, z[0]), (x + th / 2, y + th / 2, z[1])))
    return out


def bow_pull(elements, pull):
    """Remplace le cube « st:cord » (corde droite) par une corde tiree et ajoute la fleche."""
    keep, cord = [], None
    for e in elements:
        if e['name'] == 'st:cord':
            cord = e
        else:
            keep.append(e)
    assert cord, 'cube st:cord manquant'
    r = cord['rotation']['origin']
    h = (cord['to'][1] - cord['from'][1]) / 2
    u = (math.sqrt(.5), -math.sqrt(.5))
    c = (r[0], r[1])
    e1 = (c[0] - h * u[0], c[1] - h * u[1])
    e2 = (c[0] + h * u[0], c[1] + h * u[1])
    d = (1.6, 3.2, 4.8)[pull - 1]
    p = (c[0] - d * math.sqrt(.5), c[1] - d * math.sqrt(.5))
    keep += line_cubes('st:cord', e1, p, .35, (7.9, 8.1)) + line_cubes('st:cord', p, e2, .35, (7.9, 8.1))
    # fleche : de la corde tiree vers l'avant (haut-droite)
    L = 12.5
    tip = (p[0] + L * math.sqrt(.5), p[1] + L * math.sqrt(.5))
    keep += line_cubes('bn:fleche', p, tip, .4, (7.85, 8.15))
    keep.append(cube('hl:pointe_fleche', (tip[0] - .35, tip[1] - .35, 7.8), (tip[0] + .7, tip[1] + .7, 8.2)))
    for k in range(3):
        o = (p[0] + (1.2 + k * .9) * math.sqrt(.5), p[1] + (1.2 + k * .9) * math.sqrt(.5))
        keep.append(cube('ac:penne', (o[0] - .7, o[1] - .2, 7.7), (o[0] + .7, o[1] + .2, 8.3)))
    return keep


def build(wid):
    raw = json.loads((RAW / f'{wid}.json').read_text(encoding='utf-8'))
    old = json.loads((MODELS / f'sculpted_{wid}.json').read_text(encoding='utf-8'))
    TEX.mkdir(parents=True, exist_ok=True)
    els = raw['elements']
    bow = wid.startswith('arc_') or '_archer_' in wid
    models = {f'sculpted_{wid}': els}
    if any(e['name'] == 'st:cord' for e in els):
        for i in (1, 2, 3):
            models[f'sculpted_{wid}_pulling_{i - 1}'] = bow_pull(els, i)
    for name, el in models.items():
        img, m = finish(el, wid if name == f'sculpted_{wid}' else wid, old['display'], old.get('gui_light', 'front'), bow)
        if name == f'sculpted_{wid}':
            img.save(TEX / f'{wid}.png')
        else:
            m['textures']['t'] = f'mmorpg:item/weapons/{name[9:]}'
            img.save(TEX / f'{name[9:]}.png')
        (MODELS / f'{name}.json').write_text(json.dumps(m, ensure_ascii=False, separators=(',', ':')) + '\n', encoding='utf-8')
    return len(els)


if __name__ == '__main__':
    ids = sys.argv[1:] or sorted(p.stem for p in RAW.glob('*.json'))
    for i in ids:
        print(i, build(i))
