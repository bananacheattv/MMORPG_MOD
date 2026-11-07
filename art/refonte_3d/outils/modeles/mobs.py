#!/usr/bin/env python3
"""Registre des mobs refaits."""
from mobs_classic import CLASSIC
from mobs_cryptes import CRYPTES
from mobs_desert import DESERT
from mobs_foret import FORET
from mobs_givre import GIVRE
from mobs_marais import MARAIS
from mobs_neant import NEANT
from mobs_pack import PACK
from mobs_yggdrasil import YGGDRASIL

TOUS = {**FORET, **MARAIS, **CRYPTES, **GIVRE, **DESERT, **NEANT, **PACK, **CLASSIC, **YGGDRASIL}

# Modeles repris un par un : chaque modele a son fichier mob_<id>.py (fonction build), qui remplace la version de zone.
import importlib
from pathlib import Path

for f in sorted(Path(__file__).parent.glob('mob_*.py')):
    TOUS[f.stem[4:]] = importlib.import_module(f.stem).build
