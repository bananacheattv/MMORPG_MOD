#!/usr/bin/env python3
"""Ecriture JSON lisible et stable pour les donnees de quetes : indentation 2, une propriete par ligne,
cles dans un ordre logique fixe, listes de scalaires courtes sur une ligne, fins de ligne LF, retour a la ligne final.
Utilise par build_lore_quests.py ; `python3 tools/quetes/fmt_json.py` reformate le fichier en place (sans changer le contenu)."""
import json, os, sys

# Ordre des cles d'une quete : identite -> classement (acte/chapitre/categorie/region) -> evenement -> conditions -> objectif -> textes -> recompenses.
QUEST_ORDER = ['id', 'code', 'kind', 'name', 'donor', 'level', 'requires',
               'act', 'chapter', 'num', 'category', 'region',
               'event', 'from', 'to', 'repeat', 'condText', 'cond', 'classes', 'scene',
               'type', 'obj', 'objs', 'ask',
               'desc', 'gameplay', 'companion', 'rewards']
REWARD_ORDER = ['xp', 'gold', 'xpAdapt', 'evolution', 'extras']
INLINE_MAX = 90


def _scalar(v): return not isinstance(v, (dict, list))


def _fmt(v, ind, key_order=None):
    pad = '  ' * ind
    if isinstance(v, dict):
        if not v: return '{}'
        keys = list(v)
        if key_order:
            keys = [k for k in key_order if k in v] + [k for k in v if k not in key_order]
        rows = []
        for k in keys:
            sub = REWARD_ORDER if k == 'rewards' else None
            rows.append(f'{pad}  {json.dumps(k, ensure_ascii=False)}: {_fmt(v[k], ind + 1, sub)}')
        return '{\n' + ',\n'.join(rows) + '\n' + pad + '}'
    if isinstance(v, list):
        if not v: return '[]'
        if all(_scalar(x) or (isinstance(x, list) and all(_scalar(y) for y in x)) for x in v):
            one = '[' + ', '.join(json.dumps(x, ensure_ascii=False, separators=(', ', ': ')) for x in v) + ']'
            if len(one) <= INLINE_MAX: return one
        return '[\n' + ',\n'.join(f'{pad}  {_fmt(x, ind + 1)}' for x in v) + '\n' + pad + ']'
    return json.dumps(v, ensure_ascii=False)


def dumps_quests(quests):
    return '[\n' + ',\n'.join('  ' + _fmt(q, 1, QUEST_ORDER) for q in quests) + '\n]\n'


def write_quests(quests, path):
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write(dumps_quests(quests))


if __name__ == '__main__':
    p = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)), '../../src/main/resources/data/mmorpg/lore/lore_quests.json')
    d = json.load(open(p, encoding='utf-8'))
    write_quests(d, p)
    assert json.load(open(p, encoding='utf-8')) == d
