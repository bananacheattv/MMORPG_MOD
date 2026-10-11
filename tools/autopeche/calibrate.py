"""Calibrage automatique : trouve le bouchon par difference d'images entre
"ligne rentree" et "ligne lancee", puis apprend sa couleur et centre la zone.

Fonctionne quel que soit l'etat initial de la canne (lancee ou non) :
  F0 -> clic -> F1 -> clic -> F2
  - si F0 ~ F2 au niveau de l'objet : F0/F2 = rentree, F1 = lancee  -> on relance
  - sinon                           : F0/F2 = lancee,  F1 = rentree
A la fin la ligne est toujours lancee.
"""
import time

import numpy as np

from detector import largest_blob, rgb_to_hsv


def _erode(m):
    out = m.copy()
    out[1:, :] &= m[:-1, :]
    out[:-1, :] &= m[1:, :]
    out[:, 1:] &= m[:, :-1]
    out[:, :-1] &= m[:, 1:]
    return out


def _dilate(m, n=2):
    for _ in range(n):
        o = m.copy()
        o[1:, :] |= m[:-1, :]
        o[:-1, :] |= m[1:, :]
        o[:, 1:] |= m[:, :-1]
        o[:, :-1] |= m[:, 1:]
        m = o
    return m


def _change(a, b, thr=38.0):
    d = np.abs(a.astype(np.int16) - b.astype(np.int16)).max(axis=-1)
    return d > thr


def find_object(without, with_, ignore=None):
    """Blob present dans `with_` et absent de `without` (le fil fin est elimine par erosion)."""
    m = _erode(_change(without, with_))
    if ignore is not None:
        m &= ~ignore
    blob = largest_blob(_dilate(m, 1), 4)
    return blob


def learn_color(frame, blob, changed):
    """Teinte dominante des pixels qui ont change (= le bouchon, pas l'eau autour)."""
    sl = (slice(blob.y, blob.y + blob.h), slice(blob.x, blob.x + blob.w))
    h, s, v = rgb_to_hsv(frame[sl])
    sel = changed[sl] & (s > 0.35) & (v > 0.12)
    if sel.sum() < 3:
        return None
    hs, ss, vs = h[sel], s[sel], v[sel]
    hist = np.bincount((hs // 10).astype(int) % 36, weights=ss, minlength=36)
    hist = hist + np.roll(hist, 1) + np.roll(hist, -1)  # histogramme circulaire lisse
    mode = (int(hist.argmax()) * 10 + 5) % 360
    dh = np.abs(hs - mode)
    dh = np.minimum(dh, 360 - dh)
    keep = dh < 25
    if keep.sum() < 3:
        return None
    ang = np.deg2rad(hs[keep])
    hue = float(np.rad2deg(np.arctan2(np.sin(ang).sum(), np.cos(ang).sum())) % 360)
    dh = np.abs(hs[keep] - hue)
    dh = np.minimum(dh, 360 - dh)
    tol = float(np.clip(np.percentile(dh, 90) + 8, 12, 40))
    smin = float(np.clip(np.percentile(ss[keep], 10) * 0.65, 0.25, 0.8))
    vmin = float(np.clip(np.percentile(vs[keep], 10) * 0.55, 0.10, 0.6))
    return hue, tol, smin, vmin


def run(bot, sct, mon):
    """Retourne True si le calibrage a reussi (ligne lancee, config mise a jour)."""
    c = bot.cfg
    W, H = mon["width"], mon["height"]
    zw, zh = int(W * c.calib_zone_w), int(H * c.calib_zone_h)
    zx, zy = (W - zw) // 2, int((H - zh) * 0.45)
    region = {"left": mon["left"] + zx, "top": mon["top"] + zy, "width": zw, "height": zh}

    def shot(n=6):
        fr = [np.asarray(sct.grab(region))[:, :, 2::-1] for _ in range(n)]
        return np.median(np.stack(fr), axis=0).astype(np.uint8)

    def wait(t):
        end = time.time() + t
        while time.time() < end:
            if not bot.active.is_set():
                raise InterruptedError
            time.sleep(0.02)

    bot.log("Calibrage : ne bougez pas la camera...")
    f0 = shot()
    bot._click(); wait(c.calib_wait)
    f1 = shot()
    bot._click(); wait(1.2)
    f2 = shot()
    ys, xs = np.mgrid[0:zh, 0:zw]
    keep = ~(((xs + zx) > W * 0.58) & ((ys + zy) > H * 0.52))
    ch01 = (_erode(_change(f0, f1)) & keep).sum()
    ch02 = (_erode(_change(f0, f2)) & keep).sum()
    if ch01 < 4:
        bot.log("Calibrage : aucun changement visible. Canne en main ? Eau visible au centre ?")
        return False
    if ch02 < 0.35 * ch01:
        # F0/F2 ligne rentree -> on relance et on compare
        bot._click(); wait(c.calib_wait)
        f3 = shot()
        out, inn = f3, f2
    else:
        out, inn = f2, f1
    # Ignore la canne tenue en main (coin bas-droit de l'ecran), qui change aussi au lancer
    blob = find_object(inn, out, ~keep)
    if blob is None:
        bot.log("Calibrage : bouchon introuvable.")
        return False
    col = learn_color(out, blob, _change(inn, out))
    if col is None:
        bot.log("Calibrage : couleur du bouchon trop terne (activez le pack de textures).")
        return False
    c.bobber_hue, c.bobber_hue_tol, c.bobber_smin, c.bobber_vmin = col
    c.bobber_min_area = int(max(3, min(40, blob.area * 0.25)))
    sx, sy = zx + blob.cx, zy + blob.cy
    rw = max(c.calib_roi_w, int(max(blob.w, blob.h) * 18))
    rh = max(c.calib_roi_h, int(max(blob.w, blob.h) * 14))
    c.roi_w, c.roi_h = min(rw, W), min(rh, H)
    c.roi_x = int(np.clip(sx - c.roi_w / 2, 0, W - c.roi_w))
    c.roi_y = int(np.clip(sy - c.roi_h / 2, 0, H - c.roi_h))
    bot.log("Calibrage OK : bouchon %dx%d px en (%d, %d), teinte %.0f (tol %.0f), sat>%.2f lum>%.2f" % (
        blob.w, blob.h, sx, sy, col[0], col[1], col[2], col[3]))
    bot.events.put(("calibrated", None))
    return True
