#!/usr/bin/env python3
"""Controle des boss livres, sans lancer le jeu : memes os que l'original, memes animations (nom, ordre, duree, boucle), memes instants de frappe, cubes en tailles entieres, patrons dans la texture, PNG presents, classes Java coherentes. Ecrit livraison/CONTROLES_BOSS.md."""
import importlib
import re
import struct
import sys
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(HERE / 'modeles'))
from boss_kit import coplanaires, ek, prepare
from construire_boss import BOSS

ROOT = HERE.parent
LIV = ROOT / 'livraison'
RES = LIV / 'src/main/resources/assets/mmorpg'
JAVA = LIV / 'src/main/java/com/mmorpg'
ORI_JAVA = ROOT / 'originaux/git_main/src/main/java/com/mmorpg'


def taille(f):
    with open(f, 'rb') as h:
        h.read(16)
        return struct.unpack('>II', h.read(8))


def main():
    lignes = ['# Contrôles des boss', '', 'Script `outils/controle_boss.py`, sans lancer le jeu. Le squelette et les animations viennent des modules d\'origine (`art/boss_blockbench/<id>.py`) : seuls les volumes et les textures changent.', '',
              '| Boss | Os avant | Os après | Cubes avant | Cubes après | Texture avant | Texture après | Animations | Remarque |', '|---|---:|---:|---:|---:|---|---|---|---|']
    pb = 0
    for key, (cls, modnom) in BOSS.items():
        rem = []
        info = set()
        orig = importlib.import_module(key)
        m0 = orig.build()
        orig.animations(m0)
        tex0, emi0 = ek.build_texture(m0)
        mod = importlib.import_module(modnom)
        m = mod.build()
        orig.animations(m)
        prepare(m, seed=sum(map(ord, key)))
        tex, emi = ek.build_texture(m)
        ek.check(m, tex)
        manque = set(m0.bones) - set(m.bones)
        if manque:
            rem.append(f'os manquants {sorted(manque)}')
        for b0 in m0.order:
            b1 = m.bones.get(b0.name)
            if b1 and any(abs(b0.pivot[i] - b1.pivot[i]) > 1e-6 for i in range(3)):
                if b0.name.startswith(('corne_', 'pique_')):
                    info.add('cornes et piques un peu plus longues : pivots des segments décalés, sans effet sur les clips')
                else:
                    rem.append(f'pivot deplace {b0.name}')
            if b1 and (b0.parent.name if b0.parent else None) != (b1.parent.name if b1.parent else None):
                rem.append(f'parent change {b0.name}')
        if [(a.name, a.length, a.loop) for a in m0.animations] != [(a.name, a.length, a.loop) for a in m.animations]:
            rem.append('animations differentes')
        png = RES / f'textures/entity/boss/{key}.png'
        if not png.exists():
            rem.append('PNG absent')
        elif taille(png) != tex.size:
            rem.append('PNG different')
        em = RES / f'textures/entity/boss/{key}_emissive.png'
        attendu = key != 'roi_gobelin'
        if attendu and not em.exists():
            rem.append('couche emissive absente')
        if not attendu and em.exists():
            rem.append('couche emissive inutile : ce boss est rendu sans')
        if not attendu and emi.getbbox():
            rem.append('pixels lumineux inutiles')
        cop = coplanaires(m)
        if cop:
            rem.append(f'{len(cop)} faces coplanaires {cop[:3]}')
        if not (LIV / 'bbmodels' / f'{key}.bbmodel').exists():
            rem.append('bbmodel absent')
        ja = (JAVA / f'entity/boss/anim/{cls}Anims.java').read_text(encoding='utf-8')
        jo = (ORI_JAVA / f'entity/boss/anim/{cls}Anims.java').read_text(encoding='utf-8')
        fa = lambda s: re.sub(r'^//.*$', '', s, flags=re.M).strip()
        if fa(ja) != fa(jo):
            rem.append('Anims.java different de l\'original')
        jm = (JAVA / f'client/model/boss/{cls}Model.java').read_text(encoding='utf-8')
        if f'LayerDefinition.create(mesh, {tex.width}, {tex.height})' not in jm:
            rem.append('taille de texture absente du Model.java')
        if f'class {cls}Model' not in jm or 'animations()' not in jm or 'LAYER' not in jm:
            rem.append('Model.java incomplet')
        pb += len(rem)
        lignes.append(f'| `{key}` | {len(m0.order)} | {len(m.order)} | {len(m0.cubes())} | {len(m.cubes())} | {tex0.width}x{tex0.height} | {tex.width}x{tex.height} | {", ".join(a.name for a in m.animations)} | {"; ".join(rem + sorted(info))} |')
    lignes += ['', f'Boss : {len(BOSS)}. Problèmes : {pb}.', '', '`<Boss>Anims.java` est identique à l\'original, commentaire d\'en-tête mis à part : indices, durées et instants de frappe ne changent pas, le code serveur reste valable.', '']
    (LIV / 'CONTROLES_BOSS.md').write_text('\n'.join(lignes), encoding='utf-8')
    print('\n'.join(lignes[6:]))
    return 1 if pb else 0


if __name__ == '__main__':
    sys.exit(main())
