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

TOUS = {**FORET, **MARAIS, **CRYPTES, **GIVRE, **DESERT, **NEANT, **PACK, **CLASSIC}
