"""Importe le 2e lot Blockbench fourni (mobs, PNJ, decor, cosmetiques) depuis art/imported_models/pack2/*.bbmodel.

Seul le JSON et les PNG integres sont lus. Multi-textures : les textures utilisees sont empilees dans un atlas.
Sorties : bbmodels/<kind>/<cle>.json + textures/imported/<kind>/<cle>.png (kind = mobs | guides | cosmetics),
et pour le decor des modeles de bloc vanilla (models/block/decor/<cle>.json + textures/block/decor/<cle>*.png).
"""
import base64
import io
import json
import pathlib
import re

from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[1]
SRC = ROOT / 'art/imported_models/pack2'
RES = ROOT / 'src/main/resources/assets/mmorpg'

# fichier -> (kind, cle, animations {runtime: bbmodel})
MOBS = {
    'fernling': ('fernling', {'idle': 'animation.fernling.idle', 'walk': 'animation.fernling.walk', 'attack': 'animation.fernling.happy', 'death': 'animation.fernling.death'}),
    'goblin-champion': ('champion_gobelin', {'idle': 'idle', 'walk': 'walking', 'attack': '2h_swing1'}),
    'dead-traveler-skeleton': ('voyageur_squelette', {'idle': 'animation.traveler_skeleton.idle', 'walk': 'animation.traveler_skeleton.walk', 'attack': 'animation.traveler_skeleton.slash'}),
    'frostveil-owl-sentinel': ('sentinelle_hibou', {'idle': 'idle', 'walk': 'walk', 'attack': 'attack_1', 'death': 'death'}),
    'minotaur': ('minotaure', {'idle': 'minotaur.idle', 'walk': 'minotaur.walking.hostile', 'attack': 'minotaur.swing_1'}),
    'morghul-bestia-otchlani': ('morghul', {'idle': 'idle', 'walk': 'walk', 'attack': 'claw_grab', 'death': 'death'}),
    'lodowy-straznik-cytadeli': ('gardien_citadelle', {'idle': 'idle', 'walk': 'walk', 'attack': 'attack', 'death': 'spawn'}),
    'skeleton-lord': ('seigneur_squelette', {'idle': 'idle', 'walk': 'walk', 'attack': 'spell_knockback'}),
}
# cle -> (nom, zone, hauteur cible en blocs)
MOB_INFO = {
    'fernling': ('Fougerin', 0, 1.1), 'champion_gobelin': ('Champion gobelin', 0, 2.8),
    'voyageur_squelette': ('Voyageur squelette', 2, 2.1), 'seigneur_squelette': ('Seigneur squelette', 2, 5.5),
    'morghul': ('Morghul, bête des abysses', 5, 6.0), 'gardien_citadelle': ('Gardien de glace de la citadelle', 3, 6.0),
    'sentinelle_hibou': ('Sentinelle hibou du givre', 3, 3.6), 'minotaure': ('Minotaure', 4, 4.2),
}
NPCS = {'npc-oldman': 'vieux_sage', 'knight': 'chevalier', 'templar': 'templier', 'legacy-templar': 'templier_ancien', 'valheim-padded-armor': 'armure_valheim'}
NPC_NAMES = {'vieux_sage': 'Vieux sage', 'chevalier': 'Chevalier', 'templier': 'Templier', 'templier_ancien': 'Templier ancien'}


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, separators=(',', ':')) + '\n', encoding='utf-8')


def png(source):
    return Image.open(io.BytesIO(base64.b64decode(source.split(',', 1)[1]))).convert('RGBA')


def num(v):
    try:
        return float(str(v).strip())
    except ValueError:
        return 0.0


def load(name):
    return json.loads((SRC / (name + '.bbmodel')).read_text(encoding='utf-8'))


def visible_cubes(data):
    """uuid -> element pour les cubes et maillages visibles ; ignore locators et elements masques."""
    return {e['uuid']: e for e in data['elements'] if e.get('type', 'cube') in ('cube', 'mesh') and e.get('visibility', True) is not False}


def build_atlas(data, cubes):
    used = sorted({f['texture'] for e in cubes.values() for f in e['faces'].values() if f.get('texture') is not None})
    res = data['resolution']
    images = {i: png(data['textures'][i]['source']) for i in used}
    width = max(im.width for im in images.values())
    height = sum(im.height for im in images.values())
    atlas = Image.new('RGBA', (width, height), (0, 0, 0, 0))
    offsets, y = {}, 0
    for i in used:
        atlas.paste(images[i], (0, y))
        t = data['textures'][i]
        offsets[i] = (y, images[i].width / (t.get('uv_width') or res['width']), images[i].height / (t.get('uv_height') or res['height']))
        y += images[i].height
    return atlas, offsets


def shift_y(nodes, dy):
    for n in nodes:
        for k in ('from', 'to', 'origin'):
            if k in n:
                n[k] = [n[k][0], n[k][1] + dy, n[k][2]]
        shift_y(n.get('children', []), dy)


def convert(name, kind, key, anims, dy=0):
    data = load(name)
    cubes = visible_cubes(data)
    atlas, offsets = build_atlas(data, cubes)
    groups = {g['uuid']: g for g in data.get('groups', [])}
    seen = set()

    def faces(e):
        out = {}
        for fname, f in e['faces'].items():
            if f.get('texture') is None:
                out[fname] = dict(texture=None)
                continue
            y, sx, sy = offsets[f['texture']]
            if 'vertices' in f:        # face de maillage : un UV par sommet
                out[fname] = dict(vertices=f['vertices'], texture=0, uv={k: [v[0] * sx, v[1] * sy + y] for k, v in f['uv'].items()})
                continue
            uv = f['uv']
            g = dict(uv=[uv[0] * sx, uv[1] * sy + y, uv[2] * sx, uv[3] * sy + y], texture=0)
            if f.get('rotation'):
                g['rotation'] = f['rotation']
            out[fname] = g
        return out

    def node(entry):
        if isinstance(entry, str):
            if entry not in cubes:
                return None
            e = cubes[entry]
            seen.add(entry)
            n = {k: e[k] for k in ('uuid', 'from', 'to', 'origin', 'rotation', 'inflate', 'vertices') if k in e}
            n['faces'] = faces(e)
            return n
        g = groups.get(entry['uuid'], entry)
        children = [c for c in (node(c) for c in entry.get('children', [])) if c]
        return dict(uuid=entry['uuid'], name=g.get('name', ''), origin=g.get('origin', [0, 0, 0]),
                    rotation=g.get('rotation', [0, 0, 0]), children=children)

    tree = [n for n in (node(e) for e in data['outliner']) if n]
    if dy:
        shift_y(tree, dy)
    # Imported animation files can retain UUIDs from deleted template bones.
    # Preserve tracks by unique bone name when possible, omit only absent bones.
    exported = set()
    by_name = {}
    def index_bones(nodes):
        for n in nodes:
            exported.add(n['uuid'])
            if n.get('name'):
                by_name.setdefault(n['name'], []).append(n['uuid'])
            index_bones(n.get('children', []))
    index_bones(tree)
    animations = []
    for rt_name, bb_name in anims.items():
        a = next((x for x in data['animations'] if x['name'] == bb_name), None)
        if a is None:
            continue
        lines = []
        for bone, animator in a['animators'].items():
            if bone not in exported:
                matches = by_name.get(animator.get('name'), [])
                if len(matches) == 1:
                    bone = matches[0]
                else:
                    if animator.get('keyframes'):
                        print(f"{name}/{bb_name}: omitted absent bone {animator.get('name')} ({bone})")
                    continue
            for channel, code in [('rotation', 'r'), ('position', 'p'), ('scale', 's')]:
                frames = []
                for frame in sorted(animator.get('keyframes', []), key=lambda k: k['time']):
                    if frame['channel'] != channel:
                        continue
                    p = frame['data_points'][0]
                    x, y, z = num(p.get('x', 0)), num(p.get('y', 0)), num(p.get('z', 0))
                    if code == 'r':
                        x, y = -x, -y
                    if code == 'p':
                        x = -x
                    frames.append(','.join(map(str, [frame['time'], x, y, z, 'c' if frame.get('interpolation') == 'catmullrom' else 'l'])))
                if frames:
                    lines.append(bone + '|' + code + '|' + ';'.join(frames))
        loop = a.get('loop') == 'loop' and rt_name != 'death'
        animations.append(dict(name=rt_name, length=a['length'], loop=loop, tracks='\n'.join(lines)))
    write(RES / f'bbmodels/{kind}/{key}.json', dict(resolution=dict(width=atlas.width, height=atlas.height), parts=tree, animations=animations))
    out = RES / f'textures/imported/{kind}/{key}.png'
    out.parent.mkdir(parents=True, exist_ok=True)
    atlas.save(out)
    return cubes


def spawn_egg(key):
    write(RES / f'items/{key}_spawn_egg.json', {'model': {'type': 'minecraft:model', 'model': 'mmorpg:item/gobelin_spawn_egg'}})


def patch_mobs(rows):
    java = ROOT / 'src/main/java/com/mmorpg/entity/mob/ImportedMobs.java'
    text = java.read_text(encoding='utf-8')
    block = '        // EXTRA-BEGIN (genere par tools/import_pack2.py)\n' + ''.join(rows) + '        // EXTRA-END\n'
    if '// EXTRA-BEGIN' in text:
        text = re.sub(r'        // EXTRA-BEGIN.*?// EXTRA-END\n', lambda m: block, text, flags=re.S)
    else:
        text = text.replace('\n    };\n}', ',\n' + block.rstrip(',\n') + '\n    };\n}', 1) if False else text
        idx = text.rindex('    };')
        head = text[:idx].rstrip()
        text = head + ',\n' + block.rstrip('\n') + '\n' + text[idx:]
    java.write_text(text, encoding='utf-8')


def lang(entries):
    for code in ('fr_fr', 'en_us'):
        path = RES / f'lang/{code}.json'
        data = json.loads(path.read_text(encoding='utf-8'))
        data.update(entries)
        path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')


# ----------------------------------------------------------------------------- decor (blocs a modele JSON)
# fichier -> (id, nom fr, nom en, son, lumiere)
DECOR = {
    'chair': ('chaise_aeloria', 'Chaise', 'Chair', 'WOOD', 0),
    'table-1': ('table_aeloria', 'Table', 'Table', 'WOOD', 0),
    'wardrobe': ('armoire_aeloria', 'Armoire', 'Wardrobe', 'WOOD', 0),
    'camping-campfire': ('feu_de_camp', 'Feu de camp', 'Campfire', 'WOOD', 13),
    'camping-log': ('buche_camp', 'Bûche de camp', 'Camp Log', 'WOOD', 0),
    'ancient-storaege-pot-tadpole': ('jarre_ancienne', 'Jarre ancienne', 'Ancient Jar', 'STONE', 0),
    'small-decorated-pot-polyjo': ('pot_decore', 'Pot décoré', 'Decorated Pot', 'STONE', 0),
    'fae-leaf-sign': ('panneau_feuille', 'Panneau feuille féerique', 'Fae Leaf Sign', 'WOOD', 0),
    'meat': ('viande_deco', 'Viande (décor)', 'Meat (decor)', 'WOOL', 0),
    'starlight-potion': ('potion_etoilee', 'Potion étoilée', 'Starlight Potion', 'GLASS', 9),
}


def one_axis(rotation):
    nz = [(i, v) for i, v in enumerate(rotation) if abs(v) > 1e-6]
    if len(nz) > 1:
        raise ValueError('rotation multi-axes')
    return nz[0] if nz else None


def block_model(data, key, texnames):
    res = data['resolution']
    elements = []
    # format libre : le centre du bloc est l'origine (x, z) ; format java_block : coins 0..16
    dx = 0 if data['meta'].get('model_format') == 'java_block' else 8
    shift = lambda v: [v[0] + dx, v[1], v[2] + dx]
    for e in data['elements']:
        if e.get('type', 'cube') != 'cube' or e.get('visibility', True) is False:
            continue
        g = e.get('inflate') or 0
        el = {'from': shift([v - g for v in e['from']]), 'to': shift([v + g for v in e['to']])}
        r = one_axis(e.get('rotation', [0, 0, 0]))
        if r:
            el['rotation'] = {'angle': r[1], 'axis': 'xyz'[r[0]], 'origin': shift(e['origin'])}
        faces = {}
        for fname, f in e['faces'].items():
            if f.get('texture') is None:
                continue
            t = data['textures'][f['texture']]
            uw, uh = t.get('uv_width') or res['width'], t.get('uv_height') or res['height']
            if max(f['uv'][0], f['uv'][2]) > uw or max(f['uv'][1], f['uv'][3]) > uh:   # UV exprimes dans la resolution du projet
                uw, uh = res['width'], res['height']
            sx, sy = 16 / uw, 16 / uh
            uv = f['uv']
            face = {'uv': [round(uv[0] * sx, 3), round(uv[1] * sy, 3), round(uv[2] * sx, 3), round(uv[3] * sy, 3)], 'texture': '#t%d' % f['texture']}
            if f.get('rotation'):
                face['rotation'] = f['rotation']
            faces[fname] = face
        el['faces'] = faces
        elements.append(el)
    textures = {'t%d' % i: 'mmorpg:block/%s_%d' % (key, i) for i in texnames}
    textures['particle'] = textures[sorted(textures)[0]]
    return {'textures': textures, 'elements': elements}


def decor():
    java_rows, names = [], {}
    items_tab = []
    for name, (key, fr, en, sound, light) in DECOR.items():
        data = load(name)
        used = sorted({f['texture'] for e in data['elements'] for f in e['faces'].values() if f.get('texture') is not None})
        for i in used:
            out = RES / f'textures/block/{key}_{i}.png'
            out.parent.mkdir(parents=True, exist_ok=True)
            png(data['textures'][i]['source']).save(out)
        model = block_model(data, key, used)
        write(RES / f'models/block/{key}.json', model)
        write_state(key)
        write(RES / f'items/{key}.json', {'model': {'type': 'minecraft:model', 'model': f'mmorpg:block/{key}'}})
        write_loot(key)
        xs = [v for e in model['elements'] for v in (e['from'][0], e['to'][0])]
        ys = [v for e in model['elements'] for v in (e['from'][1], e['to'][1])]
        zs = [v for e in model['elements'] for v in (e['from'][2], e['to'][2])]
        c = lambda v: max(0.0, min(16.0, v))
        java_rows.append('        new Prop(%s, SoundType.%s, %d, %s, %s, %s, %s, %s, %s)' % (
            json.dumps(key), sound, light, c(min(xs)), c(min(ys)), c(min(zs)), c(max(xs)), c(max(ys)), c(max(zs))))
        names['block.mmorpg.' + key] = fr
    # panneau de quetes (modele JSON fourni tel quel)
    src = SRC / 'panneau_quete'
    pj = json.loads((src / 'Panneau.json').read_text(encoding='utf-8'))
    tex = {}
    for k, v in pj['textures'].items():
        if k == 'particle':
            continue
        img = Image.open(src / (v + '.png')).convert('RGBA')
        if max(img.size) > 256:
            img = img.resize((256, 256), Image.NEAREST)
        out = RES / f'textures/block/panneau_quetes_{v}.png'
        out.parent.mkdir(parents=True, exist_ok=True)
        img.save(out)
        tex[k] = f'mmorpg:block/panneau_quetes_{v}'
    tex['particle'] = f"mmorpg:block/panneau_quetes_{pj['textures']['particle']}"
    pj['textures'] = tex
    pj.pop('format_version', None)
    pj.pop('credit', None)
    write(RES / 'models/block/panneau_quetes.json', pj)
    write_state('panneau_quetes')
    write(RES / 'items/panneau_quetes.json', {'model': {'type': 'minecraft:model', 'model': 'mmorpg:block/panneau_quetes'}})
    write_loot('panneau_quetes')
    java_rows.append('        new Prop("panneau_quetes", SoundType.WOOD, 0, 0, 0, 6, 16, 16, 10)')
    names['block.mmorpg.panneau_quetes'] = 'Panneau de quêtes'
    java = ROOT / 'src/main/java/com/mmorpg/registry/DecorPropList.java'
    java.write_text('''package com.mmorpg.registry;

import net.minecraft.world.level.block.SoundType;

/** Genere par tools/import_pack2.py : accessoires de decor (modeles Blockbench) avec leur boite de collision (pixels). */
final class DecorPropList {
    record Prop(String id, SoundType sound, int light, double x0, double y0, double z0, double x1, double y1, double z1) {
    }

    static final Prop[] ALL = {
''' + ',\n'.join(java_rows) + '\n    };\n}\n', encoding='utf-8')
    return names


def write_state(key, model=None):
    if model:
        write(RES / f'blockstates/{key}.json', {'variants': {f'facing={d}': {'model': model} for d in ('north', 'east', 'south', 'west')}})
        return
    write(RES / f'blockstates/{key}.json', {'variants': {
        'facing=north': {'model': f'mmorpg:block/{key}'},
        'facing=east': {'model': f'mmorpg:block/{key}', 'y': 90},
        'facing=south': {'model': f'mmorpg:block/{key}', 'y': 180},
        'facing=west': {'model': f'mmorpg:block/{key}', 'y': 270}}})


def write_loot(key):
    write(ROOT / f'src/main/resources/data/mmorpg/loot_table/blocks/{key}.json', {
        'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'mmorpg:{key}'}],
                                              'conditions': [{'condition': 'minecraft:survives_explosion'}]}],
        'random_sequence': f'mmorpg:blocks/{key}'})


def main():
    rows, names = [], {}
    for name, (key, anims) in MOBS.items():
        cubes = convert(name, 'mobs', key, anims)
        title, zone, target = MOB_INFO[key]
        ys, xs = [], []
        for e in cubes.values():
            if e.get('type') == 'mesh':
                ys += [e['origin'][1] + v[1] for v in e['vertices'].values()]
                xs += [e['origin'][0] + v[0] for v in e['vertices'].values()]
            else:
                ys.append(e['to'][1]); xs += [e['from'][0], e['to'][0]]
        height = max(ys) / 16
        width = min(8.0, max(.45, (max(xs) - min(xs)) / 20))
        rows.append('        new Entry(%s, %s, %d, %.3ff, %.3ff, %.3ff, false),\n' % (json.dumps(key), json.dumps(title, ensure_ascii=False), zone, width, height, target / height))
        spawn_egg(key)
        names['entity.mmorpg.' + key] = title
        names['item.mmorpg.' + key + '_spawn_egg'] = 'Œuf d’apparition : ' + title
    patch_mobs(rows)
    for name, key in NPCS.items():
        convert(name, 'guides', key, {})
    convert('caller-flag-polyjo', 'props', 'drapeau', {'chest': 'animation.flag.wave'})   # animation continue (cf. DecorModelRenderer)
    convert('camping-staging', 'props', 'camp', {})
    names['block.mmorpg.drapeau_aeloria'] = 'Drapeau'
    names['block.mmorpg.camp_aeloria'] = 'Campement'
    for key, icon in (('drapeau_aeloria', 'minecraft:item/stick'), ('camp_aeloria', 'minecraft:item/campfire')):
        write(RES / f'items/{key}.json', {'model': {'type': 'minecraft:model', 'model': icon}})
        write_state(key, 'mmorpg:block/coffre_donjon')
        write_loot(key)
    convert('heavenly-mantle', 'cosmetics', 'manteau_celeste', {}, -24)   # origine au niveau des epaules
    names.update(decor())
    lang(names)
    print('decor', len(DECOR) + 1, 'mobs', len(MOBS), 'pnj', len(NPCS))


if __name__ == '__main__':
    main()
