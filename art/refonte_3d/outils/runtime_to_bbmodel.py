#!/usr/bin/env python3
"""Convertit un modele runtime (assets/mmorpg/bbmodels/<kind>/<id>.json + PNG, format lu par ImportedModel) en projet
.bbmodel "Generic Model" (format 'free') : memes cubes, memes UV, meme texture, memes animations. Sert aux vues AVANT.

    python runtime_to_bbmodel.py modele.json texture.png sortie.bbmodel
"""
import base64, json, sys, uuid as uuidlib
from pathlib import Path

NS = uuidlib.UUID('6f7a4b86-3a0f-4a59-9a0e-7e2d3c1b9a10')

def uid(s):
    return str(uuidlib.uuid5(NS, s))

def convert(src, png, dst):
    src, png, dst = Path(src), Path(png), Path(dst)
    data = json.loads(src.read_text(encoding='utf-8'))
    name = dst.stem
    elements, seen = [], set()

    def walk(n, path):
        if 'children' in n:
            bone = {'name': n['uuid'], 'origin': n.get('origin', [0, 0, 0]), 'rotation': n.get('rotation', [0, 0, 0]),
                    'uuid': uid(path + '/' + n['uuid']), 'export': True, 'isOpen': False, 'visibility': True, 'children': []}
            for c in n['children']:
                bone['children'].append(walk(c, path + '/' + n['uuid']))
            return bone
        key = n['uuid']
        u = uid(path + '/' + key)
        faces = {}
        for fname, f in n.get('faces', {}).items():
            face = {'uv': f['uv'], 'texture': 0 if f.get('texture') is not None else None}
            if f.get('rotation'):
                face['rotation'] = f['rotation']
            faces[fname] = face
        for fname in ('north', 'east', 'south', 'west', 'up', 'down'):
            faces.setdefault(fname, {'uv': [0, 0, 0, 0], 'texture': None})     # Blockbench exige les six faces : une face absente arrete le chargement
        el = {'name': key, 'type': 'cube', 'uuid': u, 'from': n['from'], 'to': n['to'], 'origin': n.get('origin', [0, 0, 0]),
              'rotation': n.get('rotation', [0, 0, 0]), 'faces': faces, 'box_uv': False, 'autouv': 0, 'export': True, 'visibility': True, 'color': 0}
        if n.get('inflate'):
            el['inflate'] = n['inflate']
        elements.append(el)
        return u

    outliner = [walk(p, 'root') for p in data['parts']]
    bone_uuid = {}

    def index(nodes):
        for b in nodes:
            if isinstance(b, dict):
                bone_uuid.setdefault(b['name'], b['uuid'])
                index(b['children'])
    index(outliner)

    animations = []
    for a in data.get('animations', []):
        animators = {}
        for line in a['tracks'].split('\n'):
            if not line:
                continue
            bone, ch, keys = line.split('|')
            if bone not in bone_uuid:
                continue
            channel = {'r': 'rotation', 'p': 'position', 's': 'scale'}[ch]
            kf = []
            for i, k in enumerate(keys.split(';')):
                t, x, y, z, interp = k.split(',')
                # ImportedModel: valeurs degreeVec/posVec/scaleVec (convention export Java de Blockbench) -> repere Blockbench
                x, y, z = float(x), float(y), float(z)
                if ch == 'r':
                    x, y = -x, -y
                elif ch == 'p':
                    x = -x
                kf.append({'channel': channel, 'data_points': [{'x': x, 'y': y, 'z': z}], 'uuid': uid(f"{a['name']}/{line[:30]}/{i}"), 'time': float(t),
                           'color': -1, 'interpolation': 'catmullrom' if interp == 'c' else 'linear'})
            an = animators.setdefault(bone_uuid[bone], {'name': bone, 'type': 'bone', 'keyframes': []})
            an['keyframes'] += kf
        animations.append({'uuid': uid('anim/' + a['name']), 'name': a['name'], 'loop': 'loop' if a.get('loop') else 'once', 'override': False, 'length': a['length'],
                           'snapping': 24, 'selected': False, 'anim_time_update': '', 'blend_weight': '', 'start_delay': '', 'loop_delay': '', 'animators': animators})

    b64 = base64.b64encode(png.read_bytes()).decode()
    project = {
        'meta': {'format_version': '5.0', 'model_format': 'free', 'box_uv': False},
        'name': name, 'model_identifier': name, 'visible_box': [1, 1, 0], 'variable_placeholders': '',
        'resolution': dict(data['resolution']), 'elements': elements, 'outliner': outliner,
        'textures': [{'name': png.name, 'folder': '', 'namespace': '', 'id': '0', 'particle': False, 'render_mode': 'default', 'render_sides': 'auto',
                      'frame_time': 1, 'frame_order_type': 'loop', 'frame_order': '', 'frame_interpolate': False, 'visible': True, 'mode': 'bitmap', 'saved': True,
                      'uuid': uid('tex/' + png.name), 'source': 'data:image/png;base64,' + b64}],
        'animations': animations,
    }
    dst.parent.mkdir(parents=True, exist_ok=True)
    dst.write_text(json.dumps(project, separators=(',', ':')), encoding='utf-8')
    return len(elements), len(animations)

if __name__ == '__main__':
    print(convert(*sys.argv[1:4]))
