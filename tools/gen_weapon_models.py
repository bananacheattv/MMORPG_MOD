"""Genere les 64 armes d'Aeloria : sprite peint haute definition (textures/item/weapons/<id>.png) et modele 3D extrude
(models/item/sculpted_<id>.json). Remplace l'ancienne generation « cubes empiles » de sculpt-rpg-assets.py.
Usage : python3 tools/gen_weapon_models.py [id ...]"""
import json
import zlib
import re
import subprocess
import sys
from pathlib import Path
import numpy as np
from PIL import Image

sys.path.insert(0, str(Path(__file__).resolve().parent))
from weaponart import Art, PPU, build_model  # noqa: E402
import weapondesigns as D  # noqa: E402

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources/assets/mmorpg'
MODELS = RES / 'models/item'
TEX = RES / 'textures/item/weapons'

KINDS = {'SWORD': (D.sword, 32), 'STAFF': (D.staff, 32), 'HAMMER': (D.hammer, 32), 'BOW': (D.bow, 16)}
PVP_VARIANT = {'SWORD': [1, 3, 5, 7, 10], 'STAFF': [1, 3, 5, 8, 10], 'HAMMER': [1, 3, 5, 7, 10], 'BOW': [1, 3, 5, 7, 10]}

DISPLAY_VERT = {
    'gui': {'rotation': [0, 0, -42], 'translation': [-1.2, -3.6, 0], 'scale': [.72, .72, .72]},
    'fixed': {'rotation': [0, 0, 0], 'translation': [0, -3, 0], 'scale': [.6, .6, .6]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [.4, .4, .4]},
}
HAND = {
    'thirdperson_righthand': {'rotation': [0, -90, 0], 'translation': [0, 5, .5], 'scale': [.7] * 3},
    'thirdperson_lefthand': {'rotation': [0, 90, 0], 'translation': [0, 5, .5], 'scale': [.7] * 3},
    'firstperson_righthand': {'rotation': [0, -90, 0], 'translation': [.8, 3.6, .6], 'scale': [.46] * 3},
    'firstperson_lefthand': {'rotation': [0, 90, 0], 'translation': [.8, 3.6, .6], 'scale': [.46] * 3},
}
BOW_DISPLAY = {
    'gui': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
    'fixed': {'rotation': [0, 0, 0], 'translation': [0, 0, 0], 'scale': [1, 1, 1]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 3, 0], 'scale': [.5, .5, .5]},
    'thirdperson_righthand': {'rotation': [-80, 260, -40], 'translation': [-1, -2, 2.5], 'scale': [.9] * 3},
    'thirdperson_lefthand': {'rotation': [-80, -280, 40], 'translation': [-1, -2, 2.5], 'scale': [.9] * 3},
    'firstperson_righthand': {'rotation': [0, -90, 25], 'translation': [1.13, 3.2, 1.13], 'scale': [.68] * 3},
    'firstperson_lefthand': {'rotation': [0, 90, -25], 'translation': [1.13, 3.2, 1.13], 'scale': [.68] * 3},
}
# Les mains d'origine (calees avec l'utilisateur) : marteaux/epees inclinees de 10 degres, baton droit.
HAND_TILT = {'SWORD': 10, 'HAMMER': 10, 'STAFF': 0}


def weapons():
    src = (ROOT / 'src/main/java/com/mmorpg/item/ItemDefs.java').read_text(encoding='utf-8')
    out, count = [], {}
    for wid, cls, kind in re.findall(r'w\("([^"]+)",\s*(\w+),\s*WeaponKind\.(\w+)', src):
        v = count.get(kind, 0)
        count[kind] = v + 1
        out.append((wid, kind, v, False, cls))
    for cls, key, kind in [('G', 'guerrier', 'SWORD'), ('M', 'mage', 'STAFF'), ('A', 'archer', 'BOW'), ('T', 'tank', 'HAMMER')]:
        for b in range(5):
            out.append((f'pvp_{key}_{b}_arme', kind, PVP_VARIANT[kind][b], True, cls))
    return out


def write(p, d):
    p.parent.mkdir(parents=True, exist_ok=True)
    p.write_text(json.dumps(d, ensure_ascii=False, separators=(',', ':')) + '\n', encoding='utf-8')


def make(wid, kind, v, pvp, pull=0):
    fn, hu = KINDS[kind]
    a = Art(16, hu, seed=zlib.crc32(f"{wid}{pull}".encode()) & 0xffff)
    if kind == 'BOW':
        fn(a, v, None, pull)
    else:
        fn(a, v, None)
    img, lv = a.finish()
    if pvp:   # reflet rouge d'arene
        arr = img.astype(float)
        arr[..., 0] = np.clip(arr[..., 0] * 1.12 + 6, 0, 255)
        arr[..., 2] = np.clip(arr[..., 2] * .92, 0, 255)
        img = arr.astype(np.uint8)
    return img, lv, hu


def main(only):
    n = 0
    for wid, kind, v, pvp, cls in weapons():
        if only and wid not in only:
            continue
        pulls = [0, 1, 2, 3] if kind == 'BOW' else [0]
        for pull in pulls:
            img, lv, hu = make(wid, kind, v, pvp, pull)
            name = wid if pull == 0 else f'{wid}_pull{pull}'
            TEX.mkdir(parents=True, exist_ok=True)
            Image.fromarray(img).save(TEX / f'{name}.png')
            els = build_model(lv, name, hu, tex_w=img.shape[1], tex_h=img.shape[0])
            if kind == 'BOW':
                display = BOW_DISPLAY
            else:
                display = dict(DISPLAY_VERT)
                tilt = HAND_TILT[kind]
                for k, d in HAND.items():
                    d = json.loads(json.dumps(d))
                    if k.startswith('firstperson') and kind != 'STAFF':
                        d['rotation'][2] = -tilt - 5 if 'right' in k else tilt + 5
                    elif k.startswith('thirdperson'):
                        if tilt:
                            d['rotation'][2] = tilt if 'right' in k else -tilt
                        d['translation'][1] = 1.8  # main sur le manche, pas sur le pommeau
                    display[k] = d
            model = {'credit': 'Aeloria · sprite peint et extrude', 'gui_light': 'front',
                     'textures': {'t': f'mmorpg:item/weapons/{name}', 'particle': '#t'}, 'elements': els, 'display': display}
            mname = f'sculpted_{wid}' if pull == 0 else f'sculpted_{wid}_pulling_{pull - 1}'
            write(MODELS / f'{mname}.json', model)
            if min(f['from'][1] for f in els) < -16 or max(f['to'][1] for f in els) > 32:
                raise SystemExit(f'hors limites : {mname}')
        n += 1
    print(n, 'armes generees')


if __name__ == '__main__':
    main(set(sys.argv[1:]))
