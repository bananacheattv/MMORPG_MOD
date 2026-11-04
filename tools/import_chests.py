"""Importe les deux coffres animes fournis (Blockbench) : coffre de bois (caisses a cle, une teinte par rarete) et
coffre sombre (donjons). Seul le JSON et les PNG integres sont lus.

Sources : art/imported_models/chests/*.bbmodel -> bbmodels/chests/*.json + textures/imported/chests/*.png.
Atlas = texture principale 128x128 + etoile 32x32 placee en dessous (resolution 128x160).
"""
import base64
import colorsys
import io
import json
import pathlib

from PIL import Image

ROOT = pathlib.Path(__file__).resolve().parents[1]
ART = ROOT / 'art/imported_models/chests'
RES = ROOT / 'src/main/resources/assets/mmorpg'
SHINE_Y = 128
# rarete : (teinte 0-1 ou None = bois d'origine, force du teintage du bois, couleur de l'etoile)
TIERS = {
    'commune': (None, 0.0, (255, 255, 255)),
    'vote': (0.36, 0.75, (110, 255, 150)),
    'quete': (0.12, 0.70, (255, 215, 80)),
    'rare': (0.60, 0.85, (110, 175, 255)),
    'epique': (0.78, 0.80, (200, 120, 255)),
    'legendaire': (0.09, 0.80, (255, 160, 60)),
    'mythique': (0.98, 0.70, (255, 80, 110)),
}


def png(source):
    return Image.open(io.BytesIO(base64.b64decode(source.split(',', 1)[1]))).convert('RGBA')


def floats(point):
    return [float(str(point[c]).strip()) for c in 'xyz']


def convert(path, main_name, key):
    data = json.loads(path.read_text(encoding='utf-8'))
    textures = data['textures']
    main_index = next(i for i, t in enumerate(textures) if t['name'] == main_name)
    shine_index = next(i for i, t in enumerate(textures) if t['name'] == 'shine')
    elements = {e['uuid']: e for e in data['elements']}
    groups = {g['uuid']: g for g in data['groups']}
    seen = set()

    def faces_of(e):
        out = {}
        for name, f in e['faces'].items():
            tex = f.get('texture')
            if tex is None:
                out[name] = dict(f)
                continue
            f = dict(f)
            if tex == shine_index:
                f['uv'] = [f['uv'][0], f['uv'][1] + SHINE_Y, f['uv'][2], f['uv'][3] + SHINE_Y] if isinstance(f['uv'], list) else {
                    v: [uv[0], uv[1] + SHINE_Y] for v, uv in f['uv'].items()}
            f['texture'] = 0
            out[name] = f
        return out

    def node(entry):
        if isinstance(entry, str):
            e = elements[entry]
            seen.add(entry)
            if e.get('type') == 'mesh':
                o = e['origin']
                verts = {k: [o[0] + v[0], o[1] + v[1], o[2] + v[2]] for k, v in e['vertices'].items()}
                faces = {}
                for fid, f in e['faces'].items():
                    uv = {v: [p[0], p[1] + SHINE_Y] for v, p in f['uv'].items()}
                    faces[fid] = dict(uv=uv, vertices=f['vertices'])
                return dict(uuid=e['uuid'], type='mesh', origin=o, rotation=e.get('rotation', [0, 0, 0]), vertices=verts, faces=faces,
                            emissive=True)
            return {k: e[k] for k in ('uuid', 'from', 'to', 'origin', 'rotation', 'inflate') if k in e} | dict(faces=faces_of(e))
        g = groups[entry['uuid']]
        return dict(uuid=entry['uuid'], name=g['name'], origin=g.get('origin', [0, 0, 0]), rotation=g.get('rotation', [0, 0, 0]),
                    children=[node(c) for c in entry.get('children', [])])

    tree = [node(e) for e in data['outliner']]
    assert seen == set(elements), 'elements non rattaches'
    animations = []
    for a in data['animations']:
        lines = []
        for bone, animator in a['animators'].items():
            for channel, code in (('rotation', 'r'), ('position', 'p'), ('scale', 's')):
                frames = []
                for kf in sorted(animator.get('keyframes', []), key=lambda k: k['time']):
                    if kf['channel'] != channel:
                        continue
                    x, y, z = floats(kf['data_points'][0])
                    if code == 'r':
                        x, y = -x, -y
                    if code == 'p':
                        x = -x
                    frames.append(','.join(map(str, [kf['time'], x, y, z, 'c' if kf.get('interpolation') == 'catmullrom' else 'l'])))
                if frames:
                    lines.append(bone + '|' + code + '|' + ';'.join(frames))
        animations.append(dict(name=a['name'], length=a['length'], loop=a.get('loop') == 'loop', tracks='\n'.join(lines)))
    out = RES / f'bbmodels/chests/{key}.json'
    out.parent.mkdir(parents=True, exist_ok=True)
    out.write_text(json.dumps(dict(resolution=dict(width=128, height=160), parts=tree, animations=animations),
                              ensure_ascii=False, separators=(',', ':')) + '\n', encoding='utf-8')
    main, shine = png(textures[main_index]['source']), png(textures[shine_index]['source'])
    return main, shine


def atlas(main, shine):
    img = Image.new('RGBA', (128, 160))
    img.paste(main, (0, 0))
    img.paste(shine, (0, SHINE_Y))
    return img


def tint(main, shine, hue, strength, star):
    if hue is None:
        return atlas(main, shine)
    px = main.load()
    out = main.copy()
    po = out.load()
    for y in range(main.height):
        for x in range(main.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            h2 = (h + (((hue - h + .5) % 1) - .5) * strength) % 1
            s2 = min(1.0, s * (1 - strength) + max(s, .45) * strength)
            nr, ng, nb = colorsys.hsv_to_rgb(h2, s2, v)
            po[x, y] = (int(nr * 255), int(ng * 255), int(nb * 255), a)
    sh = shine.copy()
    ps = sh.load()
    for y in range(sh.height):
        for x in range(sh.width):
            r, g, b, a = ps[x, y]
            lum = (r + g + b) / 765
            ps[x, y] = (int(star[0] * (.5 + .5 * lum)), int(star[1] * (.5 + .5 * lum)), int(star[2] * (.5 + .5 * lum)), a)
    return atlas(out, sh)


def main():
    tex = RES / 'textures/imported/chests'
    tex.mkdir(parents=True, exist_ok=True)
    wood_main, wood_shine = convert(ART / 'wooden-chest.bbmodel', 'woodenchest', 'coffre_bois')
    for tier, (hue, strength, star) in TIERS.items():
        tint(wood_main, wood_shine, hue, strength, star).save(tex / f'caisse_{tier}.png')
    dark_main, dark_shine = convert(ART / 'big-dark-chest.bbmodel', 'chestlowpalette2', 'coffre_sombre')
    atlas(dark_main, dark_shine).save(tex / 'coffre_sombre.png')
    print('coffres importes')


if __name__ == '__main__':
    main()
