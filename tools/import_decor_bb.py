"""bbmodels/bb_*.bbmodel (crees dans Blockbench, format Java Block, 1 bloc de citadelle = 48 unites) -> mod.

    python tools/import_decor_bb.py

Pour chaque modele : assets/mmorpg/bbmodels/props/decob_<id>.json + textures/imported/props/decob_<id>.png (lus par ImportedModel),
item (icone rendue), blockstate, loot, textes fr/en, et registry/DecorBlockbenchList.java (liste des blocs, collisions, lumiere).
Les coordonnees Java Block (centre du bloc en 8,8) sont recentrees sur l'origine du bloc.
"""
import base64
import io
import json
import sys
import zlib
from pathlib import Path
from types import SimpleNamespace


sys.path.insert(0, str(Path(__file__).resolve().parent))
import bbmount_lib as L
import gen_decor_bb as G
from gen_decor_bb import GOLD

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'src/main/resources/assets/mmorpg'

# id : (nom fr, nom en, lumiere)
NAMES = {
    'autel_runique': ('Autel runique', 'Runic Altar', 8),
    'arbre_cristal': ('Arbre de cristal', 'Crystal Tree', 0),
    'buisson_fleuri': ('Buisson fleuri', 'Flowering Bush', 0),
    'haie_taillee': ('Haie taillée', 'Trimmed Hedge', 0),
    'parterre_fleurs': ('Parterre de fleurs', 'Flower Bed', 0),
    'saule_arcane': ('Saule arcanique', 'Arcane Willow', 0),
    'jardiniere_geante': ('Jardinière géante', 'Giant Planter', 0),
    'rocher_moussu': ('Rocher moussu', 'Mossy Rock', 0),
    'arbre_nain': ('Arbre nain en pot', 'Potted Dwarf Tree', 0),
    'torche_geante': ('Torche géante', 'Giant Torch', 15),
    'lanterne_sol': ('Lanterne de sol', 'Ground Lantern', 14),
    'phare_arcane': ('Phare arcanique', 'Arcane Beacon', 15),
    'colonne_lumineuse': ('Colonne lumineuse', 'Glowing Column', 13),
    'brasero_double': ('Double brasero', 'Twin Brazier', 15),
    'lustre_cristal': ('Lustre de cristal', 'Crystal Chandelier', 13),
    'lit_royal': ('Lit royal', 'Royal Bed', 0),
    'coffre_tresor': ('Coffre au trésor', 'Treasure Chest', 0),
    'bureau_scribe': ('Bureau du scribe', 'Scribe Desk', 0),
    'fauteuil_royal': ('Fauteuil royal', 'Royal Armchair', 0),
    'tabouret_geant': ('Tabouret géant', 'Giant Stool', 0),
    'lutrin': ('Lutrin doré', 'Golden Lectern', 0),
    'armoire_geante': ('Armoire géante', 'Giant Wardrobe', 0),
    'cheminee': ('Cheminée monumentale', 'Monumental Fireplace', 12),
    'comptoir_taverne': ('Comptoir de taverne', 'Tavern Counter', 0),
    'chariot_marchand': ('Chariot de marchand', 'Merchant Cart', 0),
    'pilier_court': ('Pilier court', 'Short Pillar', 0),
    'balustrade': ('Balustrade de marbre', 'Marble Balustrade', 0),
    'escalier_monumental': ('Escalier monumental', 'Monumental Staircase', 0),
    'fenetre_gothique': ('Fenêtre gothique', 'Gothic Window', 0),
    'tourelle': ('Tourelle', 'Turret', 0),
    'creneau': ('Muraille à créneaux', 'Crenellated Wall', 0),
    'passerelle': ('Passerelle de marbre', 'Marble Footbridge', 0),
    'portail_monumental': ('Portail monumental', 'Monumental Gate', 0),
    'dais': ('Estrade royale', 'Royal Dais', 0),
    'dragon_pierre': ('Dragon de pierre', 'Stone Dragon', 0),
    'autel_lumiere': ('Autel de lumière', 'Altar of Light', 14),
    'stele_gravee': ('Stèle gravée', 'Engraved Stele', 0),
    'mur_orne': ('Mur orné', 'Ornate Wall', 0),
    'fontaine_murale': ('Fontaine murale', 'Wall Fountain', 0),
    'portail_runique': ('Portail runique', 'Runic Portal', 10),
    'pylone_arcane': ('Pylône arcanique', 'Arcane Pylon', 10),
    'sphere_cristal': ('Sphère de cristal', 'Crystal Sphere', 13),
    'cercle_invocation': ('Cercle d’invocation', 'Summoning Circle', 8),
    'geode': ('Géode géante', 'Giant Geode', 10),
    'totem_arcane': ('Totem arcanique', 'Arcane Totem', 0),
    'pilier_cristal_trio': ('Trio de cristaux', 'Crystal Trio', 12),
    'canon_decoratif': ('Canon décoratif', 'Decorative Cannon', 0),
    'baliste': ('Baliste', 'Ballista', 0),
    'tente_commandement': ('Tente de commandement', 'Command Tent', 0),
    'herse': ('Herse', 'Portcullis', 0),
    'armure_exposition': ('Armure d’exposition', 'Armor Display', 0),
}


def material(theme, name):
    n = name.lower()
    if n.startswith('flamme') or n in ('feu', 'feu_g', 'feu_d', 'feu_g2', 'feu_d2'):
        return G.FLAME
    if n.startswith('eau'):
        return G.WATER
    if any(k in n for k in ('verre', 'vitrail', 'voile', 'lueur', 'rayon', 'coeur', 'joyau', 'rune')):
        return G.GLOW
    if any(k in n for k in ('cris', 'gemme', 'sphere', 'geode')) or (theme == 'cristal' and n.startswith(('pointe', 'cime', 'p1', 'p2', 'p3', 'c1', 'c2', 'c3'))):
        return ('crystal', '#b070f0') if zlib.crc32(n.encode()) % 2 else ('crystal', '#e070d0')
    if any(k in n for k in ('or', 'bande', 'couronne', 'anneau', 'bague', 'cle', 'garde', 'ornement', 'serrure', 'poignee', 'chaine', 'etoile', 'manivelle')) and n.split('_')[0] in ('or', 'or1', 'or2', 'bande', 'bande1', 'couronne', 'anneau', 'bague', 'bague1', 'bague2', 'cle', 'garde', 'ornement', 'serrure', 'poignee', 'chaine', 'etoile', 'manivelle', 'anneau1'):
        return GOLD
    if theme == 'vert':
        if n.startswith(('tronc', 'racine', 'branche')):
            return G.WOOD
        if n.startswith(('fleur', 'f1', 'f2', 'f3', 'f4', 'f5', 'f6', 'f7', 'f8', 'f9', 'cris')):
            return ('banner', '#a050d8', '#f0d060')
        return ('leaf', '#6a3fa8')
    if theme == 'pierre':
        return G.STONE2 if n.startswith(('base', 'socle', 'seuil', 'plinthe', 'sol')) else G.STONE
    if theme == 'marbre':
        return G.MARB2 if n.startswith(('base', 'socle', 'm1', 'plinthe')) else G.MARB
    if theme == 'bois':
        return G.WOOD2 if n.startswith(('pied', 'roue', 'tiroirs', 'traverse', 'bord')) else G.WOOD
    if theme == 'fer':
        return G.IRON2 if n.startswith(('base', 'socle', 'affut', 'pied', 'roue')) else G.IRON
    if theme == 'or':
        return G.GOLD
    if theme == 'rouge':
        return G.WOOD2 if n.startswith(('pilier', 'pied', 'tete', 'dossier', 'sommier', 'ciel', 'accoud', 'mat', 'sol', 'timon')) else ('banner', '#5a2a8a', '#e0b040')
    if theme == 'cristal':
        return G.STONE2 if n.startswith(('base', 'socle', 'pied', 'disque', 'roche', 'mont', 'cadre', 'fut', 'bague', 'plateau', 'coupe')) else G.CRYS
    return G.STONE


def main():
    (RES / 'bbmodels/props').mkdir(parents=True, exist_ok=True)
    (RES / 'textures/imported/props').mkdir(parents=True, exist_ok=True)
    rows, lang = [], {}
    for f in sorted((ROOT / 'bbmodels').glob('bb_*.bbmodel')):
        key = f.stem[3:]
        if key not in NAMES:
            print('  ignore (sans nom):', key)
            continue
        bid = 'decob_' + key
        data = json.loads(f.read_text(encoding='utf-8'))
        cubes = [e for e in data['elements'] if e.get('type', 'cube') == 'cube']

        theme = data['textures'][0]['name'].replace('.png', '')
        m = L.Model(bid, zlib.crc32(key.encode()) % 9973 + 1)
        m.group('root', (0, 0, 0))
        for i, e in enumerate(cubes):
            rot = e.get('rotation') or [0, 0, 0]
            frm = [e['from'][0] - 8, e['from'][1], e['from'][2] - 8]
            to = [e['to'][0] - 8, e['to'][1], e['to'][2] - 8]
            org = [e.get('origin', e['from'])[0] - 8, e.get('origin', e['from'])[1], e.get('origin', e['from'])[2] - 8]
            mat = material(theme, e['name'])
            m.box('root', frm, to, mat, rot if any(rot) else None, org if any(rot) else None)
            sx, sy, sz = (to[k] - frm[k] for k in range(3))
            if not any(rot):                                   # finitions automatiques : moulures et colliers dores
                if sy <= 10 and min(sx, sz) >= 10 and mat[0] in ('stone', 'marble', 'wood') and e['name'] in ('corniche', 'linteau', 'plateau', 'chapiteau', 'dessus', 'lisse', 'couvercle'):
                    m.box('root', (frm[0] - 1, to[1] - 1.2, frm[2] - 1), (to[0] + 1, to[1] + .6, to[2] + 1), GOLD)
                elif sy >= 18 and max(sx, sz) <= 16 and min(sx, sz) >= 4 and mat[0] in ('stone', 'marble') and e['name'] in ('fut', 'fut1', 'fut2', 'mont_g', 'mont_d', 'pilier_g', 'pied_g', 'pied_d'):
                    for t in (.18, .82):
                        y = frm[1] + sy * t
                        m.box('root', (frm[0] - .8, y, frm[2] - .8), (to[0] + .8, y + 1.6, to[2] + .8), GOLD)
        tex, _ = m.build([])
        bbdata = L.to_bbmodel(m, tex, [])
        (ROOT / 'bbmodels' / f'{bid}.bbmodel').write_text(json.dumps(bbdata, separators=(',', ':')), encoding='utf-8', newline='\n')
        model, png = L.bbmodel_to_runtime(bbdata)
        fake = m.boxes
        (RES / f'bbmodels/props/{bid}.json').write_text(json.dumps(model, separators=(',', ':')), encoding='utf-8', newline='\n')
        (RES / f'textures/imported/props/{bid}.png').write_bytes(png)
        L.render(m, tex, -35, 18, 64).save(RES / f'textures/item/{bid}.png')
        w = lambda p, t: p.write_text(t, encoding='utf-8', newline='\n')
        w(RES / f'models/item/{bid}.json', json.dumps({'parent': 'minecraft:item/generated', 'textures': {'layer0': f'mmorpg:item/{bid}'}}, indent=2))
        w(RES / f'items/{bid}.json', json.dumps({'model': {'type': 'minecraft:model', 'model': f'mmorpg:item/{bid}'}}, indent=2))
        w(RES / f'blockstates/{bid}.json', json.dumps({'variants': {f'facing={d}': {'model': 'mmorpg:block/coffre_donjon'} for d in ('north', 'east', 'south', 'west')}}, separators=(',', ':')))
        loot = {'type': 'minecraft:block', 'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'mmorpg:{bid}'}],
                                                      'conditions': [{'condition': 'minecraft:survives_explosion'}]}], 'random_sequence': f'mmorpg:blocks/{bid}'}
        w(ROOT / f'src/main/resources/data/mmorpg/loot_table/blocks/{bid}.json', json.dumps(loot, separators=(',', ':')))
        xs = [v for b in fake for v in (b['from'][0], b['to'][0])]
        zs = [v for b in fake for v in (b['from'][2], b['to'][2])]
        ys = [b['to'][1] for b in fake]
        hw = max(2.0, min(8.0, max(abs(min(xs)), abs(max(xs)))))
        hd = max(2.0, min(8.0, max(abs(min(zs)), abs(max(zs)))))
        hy = max(4.0, min(32.0, max(ys)))
        fr, en, light = NAMES[key]
        rows.append((bid, 8 - hw, 8 - hd, 8 + hw, 8 + hd, hy, light))
        lang['block.mmorpg.' + bid] = (fr, en)
        print(f'{bid}: {len(cubes)} cubes, hauteur {max(ys):.0f} u')
    for code, i in (('fr_fr', 0), ('en_us', 1)):
        p = RES / f'lang/{code}.json'
        b = p.read_bytes().decode('utf-8')
        nl = '\r\n' if '\r\n' in b else '\n'
        body = b[:b.rstrip().rfind('}')].rstrip()
        add = ''.join(f',{nl}  {json.dumps(k)}: {json.dumps(v[i], ensure_ascii=False)}' for k, v in lang.items() if f'"{k}"' not in b)
        p.write_bytes((body + add + nl + '}' + nl).encode('utf-8'))
        json.loads(p.read_text(encoding='utf-8'))
    out = ['package com.mmorpg.registry;', '', 'import java.util.List;', '',
           '/** Genere par tools/import_decor_bb.py : decors crees dans Blockbench (bbmodels/bb_*.bbmodel), echelle citadelle x3. */',
           'public final class DecorBlockbenchList {', '    private DecorBlockbenchList() {', '    }', '',
           '    public static final List<DecorCitadelList.Entry> ALL = List.of(']
    for n, (bid, x0, z0, x1, z1, hy, light) in enumerate(rows):
        out.append(f'            new DecorCitadelList.Entry("{bid}", {x0:g}, 0, {z0:g}, {x1:g}, {hy:g}, {z1:g}, {light})' + (',' if n < len(rows) - 1 else ');'))
    out.append('}')
    (ROOT / 'src/main/java/com/mmorpg/registry/DecorBlockbenchList.java').write_text('\n'.join(out) + '\n', encoding='utf-8', newline='\n')
    print(len(rows), 'decors Blockbench')


if __name__ == '__main__':
    main()
