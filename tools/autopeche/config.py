"""Configuration de l'auto-peche (sauvegardee en JSON)."""
import json
import os
from dataclasses import asdict, dataclass, fields

CONFIG_PATH = os.path.join(os.path.expanduser("~"), ".autopeche_config.json")


@dataclass
class Config:
    # Zone de capture (coordonnees ecran). w/h = 0 -> zone centrale auto.
    roi_x: int = 0
    roi_y: int = 0
    roi_w: int = 0
    roi_h: int = 0
    auto_roi_w: int = 520
    auto_roi_h: int = 380

    # Couleur du bouchon (magenta avec le pack de textures)
    bobber_hue: float = 300.0
    bobber_hue_tol: float = 22.0
    bobber_smin: float = 0.55
    bobber_vmin: float = 0.25
    bobber_min_area: int = 6

    # Couleur des bulles / eclaboussures (jaune avec le pack de textures)
    bubble_hue: float = 55.0
    bubble_hue_tol: float = 18.0
    bubble_smin: float = 0.55
    bubble_vmin: float = 0.30

    # Detection
    radius_factor: float = 6.0      # rayon de recherche des bulles = facteur * taille bouchon
    radius_px: int = 25             # + marge fixe en pixels
    dip_threshold: float = 0.45     # enfoncement (en tailles de bouchon) pour un signal plein
    bubble_threshold: int = 18      # pixels de bulles proches pour un signal plein
    approach_min: int = 4           # pixels de bulles pour considerer une approche
    lost_ratio: float = 0.35        # bouchon "disparu" si aire < ratio * aire de reference
    lost_frames: int = 2
    w_dip: float = 0.70
    w_bubble: float = 0.50
    w_lost: float = 0.80
    trigger_score: float = 1.0
    confirm_frames: int = 2
    stable_frames: int = 8          # images stables avant d'armer la detection
    settle_time: float = 2.2        # ignore l'eclaboussure du lancer (s)

    # Bot
    fps: int = 30
    react_min: float = 0.08         # delai de reaction humain (s)
    react_max: float = 0.25
    recast_min: float = 0.6         # delai avant de relancer (s)
    recast_max: float = 1.3
    max_wait: float = 45.0          # relance si rien apres X s
    no_bobber_timeout: float = 6.0  # relance si bouchon introuvable X s
    start_delay: float = 3.0        # compte a rebours au demarrage
    cast_on_start: bool = True
    require_focus: bool = True      # (Windows) pause si Minecraft n'est pas au premier plan
    stop_after_catches: int = 0     # 0 = illimite
    stop_after_minutes: float = 0.0 # 0 = illimite
    hotkey_toggle: str = "f8"

    def roi(self, screen_w, screen_h):
        if self.roi_w > 0 and self.roi_h > 0:
            return self.roi_x, self.roi_y, self.roi_w, self.roi_h
        w = min(self.auto_roi_w, screen_w)
        h = min(self.auto_roi_h, screen_h)
        return (screen_w - w) // 2, (screen_h - h) // 2, w, h

    def save(self, path=CONFIG_PATH):
        with open(path, "w", encoding="utf-8") as f:
            json.dump(asdict(self), f, indent=2)

    @classmethod
    def load(cls, path=CONFIG_PATH):
        cfg = cls()
        try:
            with open(path, encoding="utf-8") as f:
                data = json.load(f)
        except (OSError, ValueError):
            return cfg
        names = {f.name: f.type for f in fields(cls)}
        for k, v in data.items():
            if k in names:
                setattr(cfg, k, type(getattr(cfg, k))(v))
        return cfg
