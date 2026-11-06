#!/usr/bin/env python3
"""Copie de CADRAGE pour capture uniquement (jamais livree) : les vues fixes de Blockbench sont centrees sur l'origine avec une echelle fixe,
un modele tres haut sort du cadre. On reduit et recentre la geometrie ; memes cubes, memes UV, meme texture.
    python copie_cadrage.py entree.bbmodel sortie.bbmodel echelle decalage_y
"""
import json
import sys
from pathlib import Path


def main(src, dst, s, dy):
    d = json.loads(Path(src).read_text(encoding='utf-8'))

    def tr(v):
        return [v[0] * s, v[1] * s + dy, v[2] * s]
    for e in d['elements']:
        e['from'], e['to'] = tr(e['from']), tr(e['to'])
        if 'origin' in e:
            e['origin'] = tr(e['origin'])

    def walk(nodes):
        for n in nodes:
            if isinstance(n, dict):
                if 'origin' in n:
                    n['origin'] = tr(n['origin'])
                walk(n.get('children', []))
    for g in d.get('groups', []):
        if 'origin' in g:
            g['origin'] = tr(g['origin'])
    walk(d['outliner'])
    d['name'] = Path(dst).stem
    d['animations'] = []
    Path(dst).parent.mkdir(parents=True, exist_ok=True)
    Path(dst).write_text(json.dumps(d, separators=(',', ':')), encoding='utf-8')


if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2], float(sys.argv[3]), float(sys.argv[4]))
