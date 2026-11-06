#!/usr/bin/env python3
"""Registre de tous les decors : identifiant -> fonction de construction."""
import importlib

TOUS = {}
for famille in ('decors_citadelle', 'decors_cathedrale', 'decors_blockbench'):
    try:
        TOUS.update(importlib.import_module(famille).DECORS)
    except ModuleNotFoundError as e:
        if e.name != famille:
            raise
