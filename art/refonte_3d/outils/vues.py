#!/usr/bin/env python3
"""Vues rapides de travail (rendu logiciel, eclairage d'entite Minecraft) : python vues.py <id> [hauteur_cavalier]."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
from render import Model, mannequin, render

ROOT = Path(__file__).resolve().parents[1]
key = sys.argv[1]
seat = float(sys.argv[2]) if len(sys.argv) > 2 else None
m = Model(ROOT / 'livraison/bbmodels' / f'{key}.bbmodel')
q = m.quads()
out = ROOT / 'apercus/travail'
out.mkdir(parents=True, exist_ok=True)
extra = mannequin(0, seat, 0) if seat is not None else None
render(q, m.tex, m.res, yaw=-40, pitch=18, size=(1000, 760), margin=.05).convert('RGB').save(out / f'{key}_a.png')
render(q, m.tex, m.res, yaw=148, pitch=16, size=(1000, 760), margin=.05, extra=extra).convert('RGB').save(out / f'{key}_b.png')
render(q, m.tex, m.res, yaw=90, pitch=0, size=(1000, 640), margin=.05).convert('RGB').save(out / f'{key}_c.png')
render(q, m.tex, m.res, yaw=180, pitch=0, size=(1000, 640), margin=.05).convert('RGB').save(out / f'{key}_d.png')
