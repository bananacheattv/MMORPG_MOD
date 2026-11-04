"""Apercu plat (vue de face) d'armes sculpted_<id>.json : echantillonne la texture via les UV de la face sud."""
import json, math, sys
from pathlib import Path
from PIL import Image
import numpy as np
ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources/assets/mmorpg'
def render(mid, S=16):
    m = json.loads((RES / f'models/item/{mid}.json').read_text(encoding='utf-8'))
    tex = Image.open(RES / 'textures/item/weapons' / (m['textures']['t'].split('/')[-1] + '.png')).convert('RGB')
    T = np.array(tex); H, W, _ = T.shape
    W_, H_ = 16 * S, 32 * S
    out = np.full((H_, W_, 3), 245, np.uint8)
    els = sorted(m['elements'], key=lambda e: e['to'][2])
    for e in els:
        u0, v0, u1, v1 = e['faces']['south']['uv']
        f, t = e['from'], e['to']
        w, h = t[0] - f[0], t[1] - f[1]
        ang = math.radians(e['rotation']['angle']) if e.get('rotation') and e['rotation']['axis'] == 'z' else 0
        o = e['rotation']['origin'] if ang else None
        cx, cy = (f[0] + t[0]) / 2, (f[1] + t[1]) / 2
        xs0, xs1 = int(min(f[0], t[0]) * S) - 40, int(max(f[0], t[0]) * S) + 40
        ys, xs = np.mgrid[0:H_, 0:W_]
        X = (xs + .5) / S; Y = 32 - (ys + .5) / S
        if ang:
            dx, dy = X - o[0], Y - o[1]
            ca, sa = math.cos(-ang), math.sin(-ang)
            X, Y = o[0] + dx * ca - dy * sa, o[1] + dx * sa + dy * ca
        mask = (X >= f[0]) & (X <= t[0]) & (Y >= f[1]) & (Y <= t[1])
        a = np.clip((X - f[0]) / max(w, 1e-6), 0, 1); b = np.clip((t[1] - Y) / max(h, 1e-6), 0, 1)
        px = ((u0 + (u1 - u0) * a) / 16 * W).astype(int).clip(0, W - 1)
        py = ((v0 + (v1 - v0) * b) / 16 * H).astype(int).clip(0, H - 1)
        out[mask] = T[py[mask], px[mask]]
    return Image.fromarray(out)
if __name__ == '__main__':
    ims = [render(f'sculpted_{i}') for i in sys.argv[1:]]
    sheet = Image.new('RGB', (sum(i.width for i in ims), ims[0].height), (245, 245, 245))
    x = 0
    for i in ims: sheet.paste(i, (x, 0)); x += i.width
    sheet.save('C:/Users/Utilisateur/AppData/Local/Temp/prev.png')
