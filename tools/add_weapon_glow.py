"""Lueur emissive + animation (pulsation, reflet qui balaie la lame) sur les modeles 3D des armes niveau 50+.
Les textures deviennent des bandes animees (8 images, interpolees) et les faces gemmes/accents passent en light_emission.
Idempotent : seule la 1re image (carree) sert de base. Ne regenere aucun modele (ne touche pas gen_weapon_models.py).
Usage : python3 tools/add_weapon_glow.py"""
import json
import math
import re
from pathlib import Path
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources/assets/mmorpg'
MODELS = RES / 'models/item'
TEX = RES / 'textures/item/weapons'
FRAMES = 8
RAR = {'C': 0, 'U': 1, 'R': 2, 'E': 3, 'L': 4, 'Y': 5}
METAL = ('mt', 'md', 'hl', 'go', 'gd', 'st')
GEM = ('g1', 'g2', 'g3')


def weapons():
    s = (ROOT / 'src/main/java/com/mmorpg/item/ItemDefs.java').read_text(encoding='utf-8')
    for m in re.finditer(r'w\("(\w+)", \w+, WeaponKind\.\w+, (\d+), (\w),', s):
        if int(m[2]) >= 50:
            yield m[1], int(m[2]), RAR[m[3]]


def process(wid, lvl, rar):
    power = 0.5 + rar * 0.35 + min(lvl - 50, 50) / 50 * 0.75
    emi_gem = min(15, 7 + rar * 2)
    emi_acc = 0 if rar < 4 else 8 + (rar - 4) * 4
    emi_hl = 0 if rar < 5 else 10
    textures = {}
    for mf in sorted(MODELS.glob(f'sculpted_{wid}*.json')):
        if mf.stem != f'sculpted_{wid}' and not re.fullmatch(rf'sculpted_{wid}_pulling_\d', mf.stem):
            continue
        m = json.loads(mf.read_text(encoding='utf-8'))
        tex = m['textures']['t'].split('/')[-1]
        for e in m['elements']:
            slot = e['name'].split(':')[0]
            em = emi_gem if slot in GEM else emi_acc if slot == 'ac' else emi_hl if slot == 'hl' else 0
            for f in e['faces'].values():
                if em:
                    f['light_emission'] = em
                else:
                    f.pop('light_emission', None)
                textures.setdefault(tex, {}).setdefault(slot, []).append(f['uv'])
        mf.write_text(json.dumps(m, ensure_ascii=False, separators=(',', ':')) + '\n', encoding='utf-8')
    for tex, slots in textures.items():
        p = TEX / f'{tex}.png'
        im = Image.open(p).convert('RGBA')
        W = im.width
        base = np.asarray(im.crop((0, 0, W, W)), dtype=float)
        masks = {'gem': np.zeros((W, W), bool), 'metal': np.zeros((W, W), bool), 'acc': np.zeros((W, W), bool)}
        for slot, uvs in slots.items():
            k = 'gem' if slot in GEM else 'metal' if slot in METAL else 'acc' if slot == 'ac' else None
            if not k:
                continue
            for u0, v0, u1, v1 in uvs:
                x0, x1 = sorted((u0, u1))
                y0, y1 = sorted((v0, v1))
                masks[k][int(y0 / 16 * W):max(int(math.ceil(y1 / 16 * W)), int(y0 / 16 * W) + 1),
                         int(x0 / 16 * W):max(int(math.ceil(x1 / 16 * W)), int(x0 / 16 * W) + 1)] = True
        yy = np.arange(W)[:, None] / W
        out = Image.new('RGBA', (W, W * FRAMES))
        for k in range(FRAMES):
            ph = 2 * math.pi * k / FRAMES
            fr = base.copy()
            pulse = 1 + (0.10 + 0.07 * power) * math.sin(ph)
            fr[..., :3][masks['gem']] *= pulse
            fr[..., :3][masks['acc']] *= 1 + (0.06 + 0.04 * power) * math.sin(ph)
            d = np.abs(yy - k / FRAMES)
            d = np.minimum(d, 1 - d)
            band = np.exp(-(d / 0.07) ** 2) * (0.10 + 0.07 * power) * 255
            fr[..., :3] += (band * masks['metal'])[..., None]
            fr[..., :3] += (band * 0.5 * masks['gem'])[..., None]
            out.paste(Image.fromarray(np.clip(fr, 0, 255).astype(np.uint8), 'RGBA'), (0, k * W))
        out.save(p)
        (TEX / f'{tex}.png.mcmeta').write_text(json.dumps(
            {'animation': {'frametime': 4, 'interpolate': True}}, separators=(',', ':')) + '\n', encoding='utf-8')
    print(wid, lvl, rar, 'power %.2f' % power, list(textures))


for wid, lvl, rar in weapons():
    process(wid, lvl, rar)
