"""Import the two supplied Blockbench 5 packs, preserving UUID bones and per-face UVs.

Run with --mobs PATH --cosmetics PATH once to archive originals. Without arguments,
regenerate runtime JSON/textures and the mob catalogue from art/imported_models.
Only JSON and embedded PNG data are read; no scripts from the packs are executed.
"""
import argparse
import base64
import json
import pathlib
import shutil

ROOT = pathlib.Path(__file__).resolve().parents[1]
ART = ROOT / 'art/imported_models'
RES = ROOT / 'src/main/resources/assets/mmorpg'


def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, separators=(',', ':')) + '\n', encoding='utf-8')


def convert(path, kind):
    data = json.loads(path.read_text(encoding='utf-8'))
    assert len(data['textures']) == 1, path
    key = path.stem[3:]
    elements = {e['uuid']: e for e in data['elements']}
    groups = {g['uuid']: g for g in data.get('groups', [])}
    seen = set()

    def node(entry):
        if isinstance(entry, str):
            e = elements[entry]
            assert e.get('type', 'cube') == 'cube'
            seen.add(entry)
            return {k: e[k] for k in ('uuid', 'from', 'to', 'origin', 'rotation', 'faces', 'inflate') if k in e}
        g = groups.get(entry['uuid'], entry)
        return dict(uuid=entry['uuid'], origin=g.get('origin', [0, 0, 0]),
                    rotation=g.get('rotation', [0, 0, 0]), children=[node(c) for c in entry.get('children', [])])

    tree = [node(e) for e in data['outliner']]
    assert seen == set(elements), f'Unattached cubes in {path}'
    animations = []
    for a in data['animations']:
        lines = []
        for bone, animator in a['animators'].items():
            assert bone in groups or bone in str(data['outliner']), (path, bone)
            for channel, code in [('rotation', 'r'), ('position', 'p'), ('scale', 's')]:
                frames = []
                for frame in sorted(animator.get('keyframes', []), key=lambda k: k['time']):
                    if frame['channel'] != channel:
                        continue
                    point = frame['data_points'][0]
                    x, y, z = [float(point[c]) for c in ('x', 'y', 'z')]
                    if code == 'r': x, y = -x, -y
                    if code == 'p': x = -x
                    frames.append(','.join(map(str, [frame['time'], x, y, z, 'c' if frame.get('interpolation') == 'catmullrom' else 'l'])))
                if frames: lines.append(bone + '|' + code + '|' + ';'.join(frames))
        animations.append(dict(name=a['name'], length=a['length'], loop=a.get('loop') == 'loop', tracks='\n'.join(lines)))
    write(RES / f'bbmodels/{kind}/{key}.json', dict(resolution=data['resolution'], parts=tree, animations=animations))
    texture = RES / f'textures/imported/{kind}/{key}.png'
    texture.parent.mkdir(parents=True, exist_ok=True)
    texture.write_bytes(base64.b64decode(data['textures'][0]['source'].split(',', 1)[1]))
    return key, data


# taille cible en jeu (hauteur en blocs) et monstres volants
# hauteur visee (blocs) et lévitation : les monstres normaux sont x2, les grandes creatures x3 et le Dévoreur x4
BIG = {'colosse_racine', 'hydre_marais', 'roi_cryptes', 'golem_iceberg', 'wyrm_boreal'}
BASE = {
    'mousseux': 1.25, 'sanglier_ronce': 1.35, 'archer_sylvestre': 1.95, 'veilleur_champignon': 1.85, 'colosse_racine': 3.6,
    'crapaud_venimeux': 1.1, 'limace_acide': 0.85, 'luciole_geante': 1.25, 'sorcier_roseaux': 2.0, 'hydre_marais': 3.2,
    'osselet': 1.35, 'araignee_os': 0.95, 'garde_funeraire': 2.1, 'spectre_enchaine': 2.0, 'roi_cryptes': 3.2,
    'renard_givre': 1.3, 'scarabee_polaire': 1.2, 'harpie_neiges': 2.0, 'golem_iceberg': 3.2, 'wyrm_boreal': 3.4,
    'scorpion_cuivre': 1.3, 'momie_dunes': 1.95, 'hyene_cendree': 1.3, 'djinn_braises': 2.2, 'tortue_volcanique': 1.4,
    'mite_astrale': 2.0, 'rodeur_vide': 1.5, 'oeil_chaos': 1.9, 'chevalier_fracture': 2.2, 'devoreur_etoiles': 3.8,
}
LEVITATING = {'luciole_geante', 'spectre_enchaine', 'harpie_neiges', 'djinn_braises', 'mite_astrale', 'oeil_chaos'}
TUNING = {k: (h * (4 if k == 'devoreur_etoiles' else 3 if k in BIG else 2), k in LEVITATING) for k, h in BASE.items()}

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument('--mobs'); ap.add_argument('--cosmetics')
    args = ap.parse_args()
    for kind in ('mobs', 'cosmetics'):
        source = getattr(args, kind)
        if source:
            target = ART / kind
            target.mkdir(parents=True, exist_ok=True)
            for file in pathlib.Path(source).rglob('*.bbmodel'):
                shutil.copyfile(file, target / file.name)
            if kind == 'mobs': shutil.copyfile(next(pathlib.Path(source).rglob('catalogue.json')), target / 'catalogue.json')
    catalogue = {m['slug'][3:]: m for m in json.loads((ART / 'mobs/catalogue.json').read_text(encoding='utf-8'))}
    rows = []
    for kind in ('mobs', 'cosmetics'):
        paths = sorted((ART / kind).glob('*.bbmodel'))
        assert len(paths) == (30 if kind == 'mobs' else 10), (kind, len(paths))
        for path in paths:
            key, data = convert(path, kind)
            if kind == 'mobs':
                n = int(path.stem[:2]); zone = (n - 1) // 5
                height = max(e['to'][1] for e in data['elements']) / 16
                width = min(2.0, max(.45, (max(e['to'][0] for e in data['elements']) - min(e['from'][0] for e in data['elements'])) / 20))
                target, flying = TUNING[key]
                rows.append('        new Entry(%s, %s, %d, %.3ff, %.3ff, %.3ff, %s)' % (json.dumps(key), json.dumps(catalogue[key]['title'], ensure_ascii=False),
                                                                             zone, width, height, target / height, 'true' if flying else 'false'))
                write(RES / f'items/{key}_spawn_egg.json', {'model': {'type':'minecraft:model', 'model':'mmorpg:item/gobelin_spawn_egg'}})
    java = ROOT / 'src/main/java/com/mmorpg/entity/mob/ImportedMobs.java'
    java.write_text('''package com.mmorpg.entity.mob;

/** Generated by tools/import_bbmodels.py from the supplied 30-model catalogue. */
public final class ImportedMobs {
    public record Entry(String key, String name, int zone, float width, float height, float scale, boolean levitating) {
        /** Taille de la boite de collision une fois le modele mis a l'echelle. */
        /** Hauteur de lévitation (blocs) entre le sol et le bas du modèle. */
        public float hover() { return levitating ? 0.6f : 0f; }
        /** Boite de collision plafonnée (3.5 x 6) pour que les géants puissent apparaître et se déplacer. */
        public float scaledWidth() { return Math.min(3.5f, width * scale); }
        public float scaledHeight() { return Math.min(6f, height * scale + hover()); }
    }
    public static final String[] ZONES = {"Forêt ancienne", "Marais", "Cryptes", "Terres gelées", "Désert volcanique", "Néant astral"};
    public static final Entry[] ALL = {
''' + ',\n'.join(rows) + '\n    };\n}\n', encoding='utf-8')
    for lang in ('fr_fr', 'en_us'):
        path = RES / f'lang/{lang}.json'
        data = json.loads(path.read_text(encoding='utf-8'))
        for key, entry in catalogue.items():
            data[f'entity.mmorpg.{key}'] = entry['title']
            data[f'item.mmorpg.{key}_spawn_egg'] = "Œuf d’apparition : " + entry['title']
        path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    print('Imported 30 mobs + 10 cosmetics; all cubes, bones, textures and keyframes validated.')


if __name__ == '__main__': main()
