#!/usr/bin/env python3
"""Lit un modele de bloc Java (assets/mmorpg/models/block/<id>.json, parents et textures resolus) et l'ecrit en .bbmodel 'free' pour les vues AVANT : tous les PNG sont ranges dans un seul atlas, les UV suivent. Memes cubes, memes faces.
    python java_vers_bb.py <id> sortie.bbmodel"""
import base64
import io
import json
import sys
import uuid
from pathlib import Path

from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
A = ROOT / 'originaux/git_main/src/main/resources/assets/mmorpg'
NS = uuid.UUID('7f7a4b86-3a0f-4a59-9a0e-7e2d3c1b9a11')
VANILLA = {
    'cube_all': lambda t: ([{'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': {f: {'uv': [0, 0, 16, 16], 'texture': '#all'} for f in ('north', 'south', 'east', 'west', 'up', 'down')}}], t),
}


def lit(key):
    return json.loads((A / f'models/block/{key}.json').read_text(encoding='utf-8'))


def resoudre(d):
    """Remonte les parents mmorpg et les parents vanilla simples."""
    tex = dict(d.get('textures', {}))
    el = d.get('elements')
    p = d.get('parent')
    while p and el is None:
        if p.startswith('mmorpg:block/'):
            m = lit(p.split('/')[-1])
            for k, v in m.get('textures', {}).items():
                tex.setdefault(k, v)
            el = m.get('elements')
            p = m.get('parent')
        else:
            n = p.split('/')[-1]
            if n == 'cube_all':
                el = VANILLA['cube_all'](tex)[0]
            elif n in ('cube_column', 'cube_column_horizontal'):
                el = [{'from': [0, 0, 0], 'to': [16, 16, 16], 'faces': {**{f: {'uv': [0, 0, 16, 16], 'texture': '#side'} for f in ('north', 'south', 'east', 'west')}, 'up': {'uv': [0, 0, 16, 16], 'texture': '#end'}, 'down': {'uv': [0, 0, 16, 16], 'texture': '#end'}}}]
            elif n == 'cross' or n == 'crop':
                cle = '#cross' if n == 'cross' else '#crop'
                el = [{'from': [4, 0, 0], 'to': [4, 16, 16], 'faces': {'east': {'uv': [0, 0, 16, 16], 'texture': cle}, 'west': {'uv': [0, 0, 16, 16], 'texture': cle}}},
                      {'from': [12, 0, 0], 'to': [12, 16, 16], 'faces': {'east': {'uv': [0, 0, 16, 16], 'texture': cle}, 'west': {'uv': [0, 0, 16, 16], 'texture': cle}}},
                      {'from': [0, 0, 4], 'to': [16, 16, 4], 'faces': {'north': {'uv': [0, 0, 16, 16], 'texture': cle}, 'south': {'uv': [0, 0, 16, 16], 'texture': cle}}},
                      {'from': [0, 0, 12], 'to': [16, 16, 12], 'faces': {'north': {'uv': [0, 0, 16, 16], 'texture': cle}, 'south': {'uv': [0, 0, 16, 16], 'texture': cle}}}]
            else:
                el = []
            p = None
    return el or [], tex


def png_de(ref):
    n = ref.split(':')[-1]
    return Image.open(A / f'textures/{n}.png').convert('RGBA')


def convertir(key, out):
    el, tex = resoudre(lit(key))
    def res(v):
        while v.startswith('#'):
            v = tex.get(v[1:], 'mmorpg:block/marbre')
        return v
    noms = sorted({res(f['texture']) for e in el for f in e['faces'].values()})
    imgs = {n: png_de(n) for n in noms}
    # atlas : carre, images les unes sous les autres en colonnes
    S = 1024
    atlas = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    x = y = hm = 0
    pos = {}
    for n in sorted(noms, key=lambda k: -imgs[k].height):
        im = imgs[n]
        if im.height > im.width * 1.5:                      # animation en bande : premiere image seulement
            im = im.crop((0, 0, im.width, im.width))
            imgs[n] = im
        if x + im.width > S:
            x, y, hm = 0, y + hm, 0
        atlas.paste(im, (x, y))
        pos[n] = (x, y, im.width, im.height)
        x += im.width
        hm = max(hm, im.height)
    elements = []
    for i, e in enumerate(el):
        faces = {}
        for f in ('north', 'east', 'south', 'west', 'up', 'down'):
            if f in e['faces']:
                fa = e['faces'][f]
                n = res(fa['texture'])
                px, py, w, h = pos[n]
                u = fa.get('uv', [0, 0, 16, 16])
                faces[f] = {'uv': [px + u[0] / 16 * w, py + u[1] / 16 * h, px + u[2] / 16 * w, py + u[3] / 16 * h], 'texture': 0}
                if 'rotation' in fa:
                    faces[f]['rotation'] = fa['rotation']
            else:
                faces[f] = {'uv': [0, 0, 0, 0], 'texture': None}
        d = {'name': e.get('name', f'c{i}'), 'box_uv': False, 'render_order': 'default', 'locked': False, 'export': True, 'scope': 0, 'allow_mirror_modeling': True,
             'from': [v - 8 if j != 1 else v for j, v in enumerate(e['from'])], 'to': [v - 8 if j != 1 else v for j, v in enumerate(e['to'])], 'autouv': 0, 'color': 0,
             'origin': [0, 0, 0], 'faces': faces, 'type': 'cube', 'uuid': str(uuid.uuid5(NS, f'{key}{i}'))}
        r = e.get('rotation')
        if r:
            o = r['origin']
            d['origin'] = [o[0] - 8, o[1], o[2] - 8]
            if 'axis' in r:
                d['rotation'] = [r['angle'] if r['axis'] == 'x' else 0, r['angle'] if r['axis'] == 'y' else 0, r['angle'] if r['axis'] == 'z' else 0]
            else:
                d['rotation'] = [r.get('x', 0), r.get('y', 0), r.get('z', 0)]
        elements.append(d)
    buf = io.BytesIO()
    atlas.save(buf, 'PNG')
    t = {'name': f'{key}.png', 'path': '', 'folder': '', 'namespace': '', 'id': '0', 'group': '', 'scope': 0, 'width': S, 'height': S, 'uv_width': S, 'uv_height': S, 'particle': False, 'use_as_default': False,
         'layers_enabled': False, 'sync_to_project': '', 'file_format': 'png', 'render_mode': 'default', 'render_sides': 'auto', 'wrap_mode': 'limited', 'pbr_channel': 'color', 'fps': 7, 'force_translucent': False,
         'frame_time': 1, 'frame_order_type': 'loop', 'frame_order': '', 'frame_interpolate': False, 'visible': True, 'internal': True, 'saved': False, 'uuid': str(uuid.uuid5(NS, key)),
         'source': 'data:image/png;base64,' + base64.b64encode(buf.getvalue()).decode()}
    g = {'name': 'root', 'uuid': str(uuid.uuid5(NS, key + 'root')), 'export': True, 'locked': False, 'scope': 0, 'selected': False, 'visibility': True, 'origin': [0, 0, 0], 'rotation': [0, 0, 0], 'color': 0, 'children': [], 'reset': False, 'shade': True, 'mirror_uv': False, 'autouv': 0, 'isOpen': False, 'primary_selected': False}
    bb = {'meta': {'format_version': '5.0', 'model_format': 'free', 'box_uv': False}, 'name': key, 'model_identifier': key, 'visible_box': [1, 1, 0], 'variable_placeholders': '', 'multi_file_ruleset': '', 'variable_placeholder_buttons': [], 'timeline_setups': [], 'unhandled_root_fields': {},
          'resolution': {'width': S, 'height': S}, 'elements': elements, 'groups': [g], 'outliner': [{'uuid': g['uuid'], 'isOpen': False, 'children': [e['uuid'] for e in elements]}], 'textures': [t], 'animations': []}
    Path(out).parent.mkdir(parents=True, exist_ok=True)
    Path(out).write_text(json.dumps(bb, separators=(',', ':')), encoding='utf-8')
    if not el:
        return 0, (), (), ()
    xs = [v for e in el for v in (e['from'][0], e['to'][0])]
    ys = [v for e in el for v in (e['from'][1], e['to'][1])]
    zs = [v for e in el for v in (e['from'][2], e['to'][2])]
    return len(el), (min(xs), max(xs)), (min(ys), max(ys)), (min(zs), max(zs))


if __name__ == '__main__':
    print(convertir(sys.argv[1], sys.argv[2]))
