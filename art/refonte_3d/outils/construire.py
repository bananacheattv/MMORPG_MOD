#!/usr/bin/env python3
"""Construit un modele : python construire.py <id> [--apercu]. Sorties dans refonte_3d/livraison (arborescence du mod) ; rien n'est ecrit dans le depot."""
import importlib
import json
import sys
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE))
sys.path.insert(0, str(HERE / 'modeles'))
from aeloria3d import check_runtime, export_runtime
from materiaux import PEINTRES, RAMPS, post_process

ROOT = HERE.parent
LIV = ROOT / 'livraison'
RES = LIV / 'src/main/resources/assets/mmorpg'


def extras_bloc(p, model):
    """Bloc de decor : icone d'inventaire (fond transparent, l'ancienne a un fond bleu opaque) et collisions recalculees par la meme regle que tools/gen_decor_shapes.py."""
    if not p.name.startswith('deco'):
        return                                  # camp et drapeau : pas d'objet d'inventaire ni de collisions dans le jeu
    from render import Model, render
    m = Model(LIV / 'bbmodels' / f'{p.name}.bbmodel')
    (RES / 'textures/item').mkdir(parents=True, exist_ok=True)
    render(m.quads(), m.tex, m.res, yaw=-35, pitch=18, size=(64, 64), margin=.03, bg=(0, 0, 0, 0)).save(RES / f'textures/item/{p.name}.png')
    sys.path.insert(0, str(ROOT / 'originaux/git_main/tools'))
    import gen_decor_shapes as G
    cells = G.model_cells(model, 1.0)
    txt = ';'.join(f'{cx},{cy},{cz}:' + '|'.join(' '.join(G.fnum(v * G.STEP) for v in b) for b in bs) for (cx, cy, cz), bs in cells.items())
    f = LIV / 'decor_shapes_patch.json'
    cur = json.loads(f.read_text(encoding='utf-8')) if f.exists() else {}
    cur[p.name] = txt
    f.write_text(json.dumps(cur, ensure_ascii=False, separators=(',', ':')), encoding='utf-8')
    old = json.loads((ROOT / 'originaux/git_main/decor_shapes.json').read_text(encoding='utf-8')).get(p.name, '')
    oc = {c.split(':')[0] for c in old.split(';') if c}
    nc = {c.split(':')[0] for c in txt.split(';') if c}
    print(f'  collisions : {len(oc)} cellules avant, {len(nc)} apres ; {len(oc - nc)} retirees, {len(nc - oc)} ajoutees')
    old_m = ROOT / f'originaux/git_main/src/main/resources/assets/mmorpg/bbmodels/props/{p.name}.json'
    if old_m.exists():
        def bb(d):
            out = []
            def w(n):
                for c in n:
                    if 'children' in c:
                        w(c['children'])
                    else:
                        out.append(c)
            w(d['parts'])
            return [min(e['from'][i] for e in out) for i in range(3)] + [max(e['to'][i] for e in out) for i in range(3)]
        bo, bn = bb(json.loads(old_m.read_text(encoding='utf-8'))), bb(model)
        print('  enveloppe avant', [round(v, 1) for v in bo], 'apres', [round(v, 1) for v in bn])
        rapport = LIV / 'collisions_rapport.json'
        cur2 = json.loads(rapport.read_text(encoding='utf-8')) if rapport.exists() else {}
        cur2[p.name] = {'avant': len(oc), 'apres': len(nc), 'retirees': len(oc - nc), 'ajoutees': len(nc - oc)}
        rapport.write_text(json.dumps(cur2, ensure_ascii=False, indent=0, sort_keys=True), encoding='utf-8')


FIGES = {'sanglier', 'deco_puits'}          # valides par le proprietaire : la reference est livraison/bbmodels, ne plus les reconstruire


def build(key):
    if key in FIGES:
        raise SystemExit(f'{key} est fige : version validee, ne pas reconstruire.')
    t0 = time.time()
    from decors import TOUS
    if key in TOUS:
        p = TOUS[key]()
    else:
        mod = importlib.import_module(key)
        importlib.reload(mod)
        p = mod.build()
    if p.kind == 'props':
        p.resoudre_coplanaires()
    else:
        p.cull()
    p.pack(min_size=128)
    p.paint(PEINTRES, RAMPS)
    post_process(p)
    bb = p.to_bbmodel()
    out = LIV / 'bbmodels'
    out.mkdir(parents=True, exist_ok=True)
    (out / f'{key}.bbmodel').write_text(json.dumps(bb, separators=(',', ':')), encoding='utf-8')
    model, png = export_runtime(bb, description=p.description)
    errs = check_runtime(model)
    (RES / f'bbmodels/{p.kind}').mkdir(parents=True, exist_ok=True)
    (RES / f'textures/imported/{p.kind}').mkdir(parents=True, exist_ok=True)
    (RES / f'bbmodels/{p.kind}/{key}.json').write_text(json.dumps(model, separators=(',', ':')), encoding='utf-8')
    (RES / f'textures/imported/{p.kind}/{key}.png').write_bytes(png)
    if p.kind == 'props':
        extras_bloc(p, model)
    faces = sum(len(c.faces) for c in p.cubes)
    cop = p.coplanar_overlaps()
    print(f'{key}: {len(p.cubes)} cubes, {len(p.bones)} os, {faces} faces ({p.culled} cachees retirees), texture {p.res}x{p.res} remplie a {p.filled:.0%}, '
          f'{len(p.anims)} animations, {time.time() - t0:.1f}s')
    if cop:
        print('  SURFACES COPLANAIRES :', cop)
    if errs:
        print('  ERREURS RUNTIME :', errs)
    return p, bb, model


if __name__ == '__main__':
    for k in sys.argv[1:]:
        if not k.startswith('--'):
            build(k)
