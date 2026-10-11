"""Detection visuelle : bouchon (magenta) + bulles/eclaboussures (jaunes) autour.

Signaux combines :
  - dip    : le bouchon s'enfonce (deplacement vertical vers le bas)
  - bubble : densite de bulles dans un rayon autour du bouchon
  - lost   : le bouchon disparait sous l'eau
  - approach : la trainee de bulles se rapproche du bouchon (poisson en approche)
"""
from collections import deque
from dataclasses import dataclass, field

import numpy as np

try:
    import cv2  # optionnel : composantes connexes plus robustes
except ImportError:
    cv2 = None


def rgb_to_hsv(rgb):
    """uint8 HxWx3 -> (h en degres, s 0-1, v 0-1), vectorise."""
    f = rgb.astype(np.float32) * (1.0 / 255.0)
    r, g, b = f[..., 0], f[..., 1], f[..., 2]
    mx = f.max(axis=-1)
    mn = f.min(axis=-1)
    d = mx - mn
    safe = np.where(d > 1e-6, d, 1.0)
    h = np.where(mx == r, ((g - b) / safe) % 6.0,
                 np.where(mx == g, (b - r) / safe + 2.0, (r - g) / safe + 4.0))
    h = np.where(d > 1e-6, h * 60.0, 0.0)
    s = np.where(mx > 1e-6, d / np.maximum(mx, 1e-6), 0.0)
    return h, s, mx


def hue_mask(h, s, v, hue, tol, smin, vmin):
    dh = np.abs(h - hue)
    dh = np.minimum(dh, 360.0 - dh)
    return (dh <= tol) & (s >= smin) & (v >= vmin)


_LUT_CACHE = {}


def color_lut(hue, tol, smin, vmin):
    """Table 32x32x32 -> bool : masque de couleur en une seule indexation (tres rapide)."""
    key = (round(hue, 1), round(tol, 1), round(smin, 3), round(vmin, 3))
    lut = _LUT_CACHE.get(key)
    if lut is None:
        q = (np.arange(32, dtype=np.uint8) * 8 + 4)
        rgb = np.stack(np.meshgrid(q, q, q, indexing="ij"), axis=-1).reshape(1, -1, 3)
        h, s, v = rgb_to_hsv(rgb)
        lut = hue_mask(h, s, v, hue, tol, smin, vmin).reshape(-1)
        if len(_LUT_CACHE) > 16:
            _LUT_CACHE.clear()
        _LUT_CACHE[key] = lut
    return lut


def lut_index(rgb):
    r = (rgb[..., 0] >> 3).astype(np.uint16)
    g = (rgb[..., 1] >> 3).astype(np.uint16)
    b = (rgb[..., 2] >> 3).astype(np.uint16)
    return (r << 10) | (g << 5) | b


@dataclass
class Blob:
    cx: float
    cy: float
    x: int
    y: int
    w: int
    h: int
    area: int


def largest_blob(mask, min_area):
    if cv2 is not None:
        n, _, stats, cents = cv2.connectedComponentsWithStats(mask.astype(np.uint8), connectivity=8)
        if n <= 1:
            return None
        i = 1 + int(np.argmax(stats[1:, cv2.CC_STAT_AREA]))
        area = int(stats[i, cv2.CC_STAT_AREA])
        if area < min_area:
            return None
        x, y, w, h = (int(stats[i, k]) for k in (cv2.CC_STAT_LEFT, cv2.CC_STAT_TOP,
                                                  cv2.CC_STAT_WIDTH, cv2.CC_STAT_HEIGHT))
        return Blob(float(cents[i][0]), float(cents[i][1]), x, y, w, h, area)
    ys, xs = np.nonzero(mask)
    if len(xs) < min_area:
        return None
    if len(xs) > 30000:  # trop de pixels : approximation par la mediane
        return Blob(float(np.median(xs)), float(np.median(ys)), int(xs.min()), int(ys.min()),
                    int(np.ptp(xs) + 1), int(np.ptp(ys) + 1), int(len(xs)))
    best = None
    todo = set(zip(ys.tolist(), xs.tolist()))
    while todo:
        seed = todo.pop()
        comp, stack = [seed], [seed]
        while stack:
            y, x = stack.pop()
            for dy in (-1, 0, 1):
                for dx in (-1, 0, 1):
                    q = (y + dy, x + dx)
                    if q in todo:
                        todo.remove(q)
                        comp.append(q)
                        stack.append(q)
        if best is None or len(comp) > len(best):
            best = comp
    if len(best) < min_area:
        return None
    a = np.asarray(best)
    y0, x0 = a.min(0)
    y1, x1 = a.max(0)
    return Blob(float(a[:, 1].mean()), float(a[:, 0].mean()), int(x0), int(y0),
                int(x1 - x0 + 1), int(y1 - y0 + 1), int(len(a)))


def find_bobber(mask, min_area, size_hint=8.0, expect=None):
    """Recherche rapide (numpy pur) : cellule la plus dense (proche de la position
    attendue) puis mean-shift."""
    ys, xs = np.nonzero(mask)
    if len(xs) < min_area:
        return None
    if len(xs) > 40:
        cell = 6
        hgt, wid = mask.shape
        nx = wid // cell + 1
        idx = (ys // cell) * nx + xs // cell
        counts = np.bincount(idx).astype(np.float32)
        if expect is not None:
            cells = np.arange(len(counts))
            d = np.hypot((cells % nx + 0.5) * cell - expect[0], (cells // nx + 0.5) * cell - expect[1])
            counts /= 1.0 + d / max(hgt, wid) * 4.0
        best = int(counts.argmax())
        cx, cy = (best % nx + 0.5) * cell, (best // nx + 0.5) * cell
    else:
        cx, cy = float(np.median(xs)), float(np.median(ys))
    rad = max(6.0, size_hint * 1.3)
    for _ in range(4):
        sel = (np.abs(xs - cx) <= rad) & (np.abs(ys - cy) <= rad)
        if sel.sum() < min_area:
            return None
        sx, sy = xs[sel], ys[sel]
        ncx, ncy = float(sx.mean()), float(sy.mean())
        x0, x1, y0, y1 = sx.min(), sx.max(), sy.min(), sy.max()
        touching = (x1 - x0) >= 2 * rad - 2 or (y1 - y0) >= 2 * rad - 2
        moved = abs(ncx - cx) + abs(ncy - cy) > 1
        cx, cy = ncx, ncy
        if touching:
            rad *= 1.5
        elif not moved:
            break
    return Blob(cx, cy, int(x0), int(y0), int(x1 - x0 + 1), int(y1 - y0 + 1), int(sel.sum()))


@dataclass
class Result:
    t: float
    bobber: Blob = None
    center: tuple = None
    radius: float = 0.0
    dip: float = 0.0
    bubbles: int = 0
    bubble_dist: float = 0.0
    lost: bool = False
    approach: bool = False
    armed: bool = False
    score: float = 0.0
    bite: bool = False
    bobber_mask: np.ndarray = None   # masques de la fenetre de travail
    bubble_mask: np.ndarray = None
    offset: tuple = (0, 0)
    signals: dict = field(default_factory=dict)


class Detector:
    def __init__(self, cfg):
        self.cfg = cfg
        self.bg = None
        self.noise_mu, self.noise_var = 0.0, 4.0
        self.reset(0.0)

    def reset(self, t):
        self.t0 = t
        self.base_y = None
        self.base_area = None
        self.stable = 0
        self.lost_count = 0
        self.confirm = 0
        self.last_center = None
        self.last_pos = None
        self.last_size = 8.0
        self.dist_hist = deque(maxlen=12)
        self.last_seen = t

    def process(self, frame, t):
        c = self.cfg
        H, W = frame.shape[:2]
        # Fenetre de travail : seulement autour du bouchon une fois trouve (bien plus rapide)
        ox, oy, x1, y1 = 0, 0, W, H
        if self.last_pos is not None and t - self.last_seen < 1.0:
            half = int(c.radius_factor * self.last_size + c.radius_px + 3 * self.last_size + 12)
            px, py = self.last_pos
            ox, oy = max(0, int(px - half)), max(0, int(py - half))
            x1, y1 = min(W, int(px + half)), min(H, int(py + half))
        sub = frame[oy:y1, ox:x1]
        idx = lut_index(sub)
        bmask = color_lut(c.bobber_hue, c.bobber_hue_tol, c.bobber_smin, c.bobber_vmin)[idx]
        umask = color_lut(c.bubble_hue, c.bubble_hue_tol, c.bubble_smin, c.bubble_vmin)[idx]
        if self.last_pos is not None:
            expect = (self.last_pos[0] - ox, self.last_pos[1] - oy)
        else:
            expect = (sub.shape[1] / 2, sub.shape[0] / 2)  # le calibrage centre la zone sur le bouchon
        blob = find_bobber(bmask, c.bobber_min_area, self.last_size, expect)
        # Bulles par mouvement (fonctionne meme sans le pack de textures)
        if c.bubble_motion:
            if self.bg is None or self.bg.shape != (H, W):
                self.bg = frame.mean(axis=-1, dtype=np.float32)
            gray = sub.mean(axis=-1, dtype=np.float32)  # (fenetre seulement)
            bg = self.bg[oy:y1, ox:x1]
            umask |= np.abs(gray - bg) > c.motion_threshold
            bg += (gray - bg) * 0.15
        umask &= ~bmask
        if blob is not None:  # le bouchon qui bouge n'est pas une bulle
            m = int(max(blob.w, blob.h) * 0.7) + 2
            umask[max(0, blob.y - m):blob.y + blob.h + m, max(0, blob.x - m):blob.x + blob.w + m] = False
            blob = Blob(blob.cx + ox, blob.cy + oy, blob.x + ox, blob.y + oy, blob.w, blob.h, blob.area)
            self.last_pos = (blob.cx, blob.cy)
        r = Result(t=t, bobber=blob, bobber_mask=bmask, bubble_mask=umask, offset=(ox, oy))

        if blob is not None:
            self.last_seen = t
            size = float(max(blob.w, blob.h, 3))
            if self.base_y is None:
                self.base_y, self.base_area = blob.cy, float(blob.area)
            dy = blob.cy - self.base_y
            r.dip = max(0.0, dy) / size
            # La reference ne suit le bouchon que lorsqu'il ne plonge pas (houle, camera)
            if r.dip < c.dip_threshold * 0.5:
                self.base_y += (blob.cy - self.base_y) * 0.08
                self.base_area += (blob.area - self.base_area) * 0.05
                self.stable += 1
            self.last_center = (blob.cx, self.base_y)
            self.last_size = size
            area_ratio = blob.area / max(self.base_area, 1.0)
            self.lost_count = self.lost_count + 1 if area_ratio < c.lost_ratio else 0
        else:
            self.lost_count += 1
            if self.base_y is not None:
                r.dip = c.dip_threshold  # disparu = enfonce

        r.armed = (t - self.t0) >= c.settle_time and self.stable >= c.stable_frames
        r.lost = self.base_area is not None and self.lost_count >= c.lost_frames

        # Bulles autour du bouchon
        if self.last_center is not None:
            cx, cy = self.last_center
            r.center = (cx, cy)
            r.radius = c.radius_factor * self.last_size + c.radius_px
            ys, xs = np.nonzero(umask)
            xs, ys = xs + ox, ys + oy
            if len(xs):
                d = np.hypot(xs - cx, ys - cy)
                near = d <= r.radius
                raw = int(near.sum())
                # Seuil de bruit appris en continu (eau animee, houle...)
                floor = self.noise_mu + 2.5 * self.noise_var ** 0.5
                if r.armed and r.dip < c.dip_threshold * 0.3:
                    self.noise_mu += (raw - self.noise_mu) * 0.03
                    self.noise_var += ((raw - self.noise_mu) ** 2 - self.noise_var) * 0.03
                r.bubbles = max(0, int(raw - floor))
                if r.bubbles:
                    r.bubble_dist = float(d[near].mean())
        if r.bubbles >= c.approach_min:
            self.dist_hist.append(r.bubble_dist)
        elif self.dist_hist:
            self.dist_hist.popleft()
        if len(self.dist_hist) >= 4:
            hist = np.asarray(self.dist_hist)
            slope = np.polyfit(np.arange(len(hist)), hist, 1)[0]
            r.approach = slope < -0.5

        sd = min(r.dip / max(c.dip_threshold, 1e-3), 1.5)
        sb = min(r.bubbles / max(c.bubble_threshold, 1), 1.5)
        sl = 1.0 if r.lost else 0.0
        r.score = c.w_dip * sd + c.w_bubble * sb + c.w_lost * sl
        r.signals = {"dip": sd, "bubble": sb, "lost": sl}

        if r.armed and r.score >= c.trigger_score:
            self.confirm += 1
        else:
            self.confirm = 0
        r.bite = self.confirm >= c.confirm_frames
        return r

    def seconds_without_bobber(self, t):
        return t - self.last_seen


def annotate(frame, r, max_w=520):
    """Image PIL de previsualisation avec masques et reperes."""
    from PIL import Image, ImageDraw
    img = frame.copy()
    ox, oy = r.offset
    if r.bobber_mask is not None:
        hh, ww = r.bobber_mask.shape
        win = img[oy:oy + hh, ox:ox + ww]
        win[r.bobber_mask] = (255, 0, 255)
        win[r.bubble_mask] = (255, 255, 0)
    pil = Image.fromarray(img)
    d = ImageDraw.Draw(pil)
    if r.center is not None:
        cx, cy = r.center
        rad = r.radius
        col = (255, 60, 60) if r.bite else (255, 170, 0) if r.approach else (0, 220, 255)
        d.ellipse((cx - rad, cy - rad, cx + rad, cy + rad), outline=col, width=2)
        d.line((cx - 12, cy, cx + 12, cy), fill=(0, 255, 0), width=1)
    if r.bobber is not None:
        b = r.bobber
        d.rectangle((b.x - 2, b.y - 2, b.x + b.w + 2, b.y + b.h + 2), outline=(0, 255, 0), width=2)
    if pil.width > max_w:
        pil = pil.resize((max_w, int(pil.height * max_w / pil.width)), Image.NEAREST)
    return pil
