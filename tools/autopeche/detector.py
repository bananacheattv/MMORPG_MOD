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
    # Sans OpenCV : on garde les pixels proches de la mediane (rejette le bruit isole)
    mx, my = np.median(xs), np.median(ys)
    spread = max(6.0, 3.0 * np.median(np.hypot(xs - mx, ys - my)))
    keep = np.hypot(xs - mx, ys - my) <= spread
    xs, ys = xs[keep], ys[keep]
    if len(xs) < min_area:
        return None
    x0, x1 = np.percentile(xs, [3, 97]).astype(int)
    y0, y1 = np.percentile(ys, [3, 97]).astype(int)
    return Blob(float(xs.mean()), float(ys.mean()), int(x0), int(y0),
                int(x1 - x0 + 1), int(y1 - y0 + 1), int(len(xs)))


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
    bobber_mask: np.ndarray = None
    bubble_mask: np.ndarray = None
    signals: dict = field(default_factory=dict)


class Detector:
    def __init__(self, cfg):
        self.cfg = cfg
        self.reset(0.0)

    def reset(self, t):
        self.t0 = t
        self.base_y = None
        self.base_area = None
        self.stable = 0
        self.lost_count = 0
        self.confirm = 0
        self.last_center = None
        self.last_size = 8.0
        self.dist_hist = deque(maxlen=12)
        self.last_seen = t

    def process(self, frame, t):
        c = self.cfg
        h, s, v = rgb_to_hsv(frame)
        bmask = hue_mask(h, s, v, c.bobber_hue, c.bobber_hue_tol, c.bobber_smin, c.bobber_vmin)
        umask = hue_mask(h, s, v, c.bubble_hue, c.bubble_hue_tol, c.bubble_smin, c.bubble_vmin) & ~bmask
        blob = largest_blob(bmask, c.bobber_min_area)
        r = Result(t=t, bobber=blob, bobber_mask=bmask, bubble_mask=umask)

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
            if len(xs):
                d = np.hypot(xs - cx, ys - cy)
                near = d <= r.radius
                r.bubbles = int(near.sum())
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
    if r.bobber_mask is not None:
        img[r.bobber_mask] = (255, 0, 255)
    if r.bubble_mask is not None:
        img[r.bubble_mask] = (255, 255, 0)
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
