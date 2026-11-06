#!/usr/bin/env python3
"""Construit des decors en masse et resume : python build_decors.py prefixe [--verbeux]. Prefixe : deco_cat_, decob_, deco_, ou un identifiant exact. Une erreur n'arrete pas les autres."""
import contextlib
import io
import re
import sys
import traceback
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
sys.path.insert(0, str(Path(__file__).parent / 'modeles'))
from decors import TOUS
import construire


def main(args):
    v = '--verbeux' in args
    pref = [a for a in args if not a.startswith('--')]
    cles = [k for k in TOUS if any(k == p or k.startswith(p) for p in pref)]
    if 'deco_' in pref:
        pass
    ok = 0
    for key in cles:
        buf = io.StringIO()
        try:
            with contextlib.redirect_stdout(buf):
                construire.build(key)
            ok += 1
            s = buf.getvalue()
            if v:
                print(s, end='')
            else:
                m = re.search(r'(\d+) cubes, (\d+) os, .*texture (\d+)x', s)
                c = re.search(r'collisions : (\d+) cellules avant, (\d+) apres ; (\d+) retirees, (\d+) ajoutees', s)
                e = re.search(r'enveloppe avant (\[.*?\]) apres (\[.*?\])', s)
                cop = 'SURFACES COPLANAIRES' in s
                cc = f"coll {c.group(1)}>{c.group(2)} (-{c.group(3)} +{c.group(4)})" if c else 'sans collisions'
                print(f"{key:30s} {m.group(1):>3s} cubes tex {m.group(3):>4s} {cc} {e.group(2) if e else ''}{' COPLANAIRES: ' + s.split('COPLANAIRES : ')[1].split(chr(10))[0][:220] if cop else ''}")
        except Exception as ex:
            print('ERREUR', key, repr(ex))
            if v:
                traceback.print_exc(limit=3)
    print(ok, 'sur', len(cles))


if __name__ == '__main__':
    main(sys.argv[1:])
