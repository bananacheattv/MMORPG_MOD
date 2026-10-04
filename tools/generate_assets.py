"""
Generateur de ressources du mod MMORPG.

Produit dans src/main/resources :
  - textures (objets, armures portees, entites, blocs, icones de GUI)
  - modeles d'objets / blocs, definitions d'objets (assets/<id>/items)
  - fichiers de langue (fr_fr + en_us)
  - recettes vanilla des blocs, tables de butin, type de degats

Usage : python tools/generate_assets.py
"""
import glob
import json
import math
import os
import random
import shutil

from PIL import Image

import gen_items as gi
import gen_fx
import gen_entities as ge
from glyphs import G as GLYPHS
from pixel import Canvas, rgb, shade, mix, ramp, T

MODID = 'mmorpg'
ROOT = os.path.join(os.path.dirname(__file__), '..', 'src', 'main', 'resources')
ASSETS = os.path.join(ROOT, 'assets', MODID)
DATA = os.path.join(ROOT, 'data')

LANG = {}
MANIFEST = []


def path(*p):
    full = os.path.join(*p)
    os.makedirs(os.path.dirname(full), exist_ok=True)
    return full


def write_json(p, obj):
    with open(path(p), 'w', encoding='utf-8') as f:
        json.dump(obj, f, ensure_ascii=False, indent=2)


def save_tex(img_or_canvas, rel):
    img = img_or_canvas.image() if isinstance(img_or_canvas, Canvas) else img_or_canvas
    img.save(path(ASSETS, 'textures', rel + '.png'))


def item_model(item_id, parent='minecraft:item/generated', texture=None):
    write_json(os.path.join(ASSETS, 'models', 'item', item_id + '.json'),
               {'parent': parent, 'textures': {'layer0': f'{MODID}:item/{texture or item_id}'}})
    write_json(os.path.join(ASSETS, 'items', item_id + '.json'),
               {'model': {'type': 'minecraft:model', 'model': f'{MODID}:item/{item_id}'}})


def simple_item(item_id, name, canvas, parent='minecraft:item/generated'):
    save_tex(canvas, 'item/' + item_id)
    item_model(item_id, parent)
    LANG[f'item.{MODID}.{item_id}'] = name
    MANIFEST.append(('item', item_id))


def bow_item(item_id, name, **kw):
    save_tex(gi.bow(pull=0, **kw), 'item/' + item_id)
    for i, pull in enumerate((1.4, 2.6, 3.6)):
        save_tex(gi.bow(pull=pull, **kw), f'item/{item_id}_pulling_{i}')
    write_json(os.path.join(ASSETS, 'models', 'item', item_id + '.json'),
               {'parent': 'minecraft:item/bow', 'textures': {'layer0': f'{MODID}:item/{item_id}'}})
    for i in range(3):
        write_json(os.path.join(ASSETS, 'models', 'item', f'{item_id}_pulling_{i}.json'),
                   {'parent': 'minecraft:item/bow', 'textures': {'layer0': f'{MODID}:item/{item_id}_pulling_{i}'}})

    def m(n):
        return {'type': 'minecraft:model', 'model': f'{MODID}:item/{n}'}
    write_json(os.path.join(ASSETS, 'items', item_id + '.json'), {
        'model': {
            'type': 'minecraft:condition',
            'property': 'minecraft:using_item',
            'on_false': m(item_id),
            'on_true': {
                'type': 'minecraft:range_dispatch',
                'property': 'minecraft:use_duration',
                'scale': 0.05,
                'fallback': m(item_id + '_pulling_0'),
                'entries': [
                    {'threshold': 0.65, 'model': m(item_id + '_pulling_1')},
                    {'threshold': 0.9, 'model': m(item_id + '_pulling_2')},
                ],
            },
        }
    })
    LANG[f'item.{MODID}.{item_id}'] = name
    MANIFEST.append(('item', item_id))


# ---------------------------------------------------------------------------
# Armes
# ---------------------------------------------------------------------------

# Armes en 3D (modeles Blockbench, format Java Block/Item) generees dans art/ :
#   art/batons_blockbench/generer_batons.py, art/epees_blockbench/generer_epees.py,
#   art/marteaux_blockbench/generer_marteaux.py, art/arcs_blockbench/generer_arcs.py
# Le sprite 2D genere plus bas reste disponible (et sert pour les armes sans modele 3D, ex. la hache du berserker),
# mais ces objets utilisent le modele 3D en main et dans l'inventaire.
WEAPONS_3D = {
    'batons_blockbench': {
        'baton_apprenti': 'baton_niv001',
        'sceptre_givre': 'baton_niv020',
        'baton_flammes': 'baton_niv040',
        'baton_arcanique': 'baton_niv060',
        'sceptre_neant': 'baton_niv080',
        'baton_archimage': 'baton_niv100',
    },
    'epees_blockbench': {
        'epee_recrue': 'epee_niv_01',            # Epee de la recrue
        'lame_runique': 'epee_niv_40',           # Epee runique
        'epee_seigneur_guerre': 'epee_niv_60',   # Epee du conquerant
        'lame_demoniaque': 'epee_niv_80',        # Epee legendaire
        'excalibur_celeste': 'epee_niv_100',     # Epee mythique
    },
    'marteaux_blockbench': {
        'masse_garde': 'marteau_niv_01',
        'marteau_bastion': 'marteau_niv_20',
        'marteau_colosse': 'marteau_niv_40',
        'masse_gardien': 'marteau_niv_60',
        'marteau_titan': 'marteau_niv_80',
        'marteau_egide': 'marteau_niv_100',
    },
    'arcs_blockbench': {                         # + modeles de traction _pulling_0/1/2
        'arc_chasseur': 'arc_niv_01',
        'arc_elfique': 'arc_niv_20',
        'arc_tempete': 'arc_niv_40',
        'arc_faucon': 'arc_niv_60',
        'arc_spectral': 'arc_niv_80',
        'arc_aube_divine': 'arc_niv_100',
    },
}


def weapon_models_3d():
    art = os.path.join(os.path.dirname(__file__), '..', 'art')
    for folder, table in WEAPONS_3D.items():
        src = os.path.join(art, folder, 'minecraft', 'assets', MODID)
        if not os.path.isdir(src):
            print(f'ATTENTION : lancer art/{folder}/generer_*.py pour obtenir les modeles 3D')
            continue
        for item_id, model in table.items():
            variants = [model] + [f'{model}_pulling_{i}' for i in range(3)
                                  if os.path.isfile(os.path.join(src, 'models', 'item', f'{model}_pulling_{i}.json'))]
            for v in variants:
                shutil.copyfile(os.path.join(src, 'models', 'item', v + '.json'), path(ASSETS, 'models', 'item', v + '.json'))
            shutil.copyfile(os.path.join(src, 'textures', 'item', model + '.png'), path(ASSETS, 'textures', 'item', model + '.png'))
            definition = os.path.join(src, 'items', model + '.json')
            if os.path.isfile(definition):           # arcs : definition avec etats de traction
                with open(definition, encoding='utf-8') as f:
                    write_json(os.path.join(ASSETS, 'items', item_id + '.json'), json.load(f))
            else:
                write_json(os.path.join(ASSETS, 'items', item_id + '.json'),
                           {'model': {'type': 'minecraft:model', 'model': f'{MODID}:item/{model}'}})


# Boss animes, structures multiblocs et armures 3D (art/boss_blockbench, art/structures_blockbench, art/armures_blockbench) :
# modeles en Java, textures copiees ici.
BOSS_TEXTURES = ['roi_gobelin', 'liche_ancienne', 'ignis', 'titan_glace', 'avatar_neant']
PET_TEXTURES = ['feu_follet', 'bebe_slime', 'chouette_sage', 'loup_spectral', 'golem_poche', 'fee_lumineuse', 'phenix_dore',
                'dragonnet_neant']


def entity_models_3d():
    art = os.path.join(os.path.dirname(__file__), '..', 'art')
    for key in BOSS_TEXTURES:
        for suffix in ('', '_emissive'):
            src = os.path.join(art, 'boss_blockbench', key, f'{key}{suffix}.png')
            if os.path.isfile(src):
                shutil.copyfile(src, path(ASSETS, 'textures', 'entity', 'boss', f'{key}{suffix}.png'))
            elif not suffix:
                print(f'ATTENTION : texture du boss {key} absente (lancer art/boss_blockbench/generer_boss.py)')
    for key in ('autel_invocation', 'teleporteur', 'forge_arcanique'):      # structures multiblocs
        for suffix in ('', '_emissive'):
            src = os.path.join(art, 'structures_blockbench', key, f'{key}{suffix}.png')
            if os.path.isfile(src):
                shutil.copyfile(src, path(ASSETS, 'textures', 'entity', 'structure', f'{key}{suffix}.png'))
            else:
                print(f'ATTENTION : texture de structure {key}{suffix} absente (lancer art/structures_blockbench/generer_structures.py)')
    for key in PET_TEXTURES:                                                # familiers 3D (art/familiers_blockbench)
        for suffix in ('', '_emissive'):
            src = os.path.join(art, 'familiers_blockbench', key, f'{key}{suffix}.png')
            if os.path.isfile(src):
                shutil.copyfile(src, path(ASSETS, 'textures', 'entity', 'pet', f'{key}{suffix}.png'))
            elif not suffix:
                print(f'ATTENTION : texture du familier {key} absente (lancer art/familiers_blockbench/generer_familiers.py)')
    for src in sorted(glob.glob(os.path.join(art, 'armures_blockbench', 'panoplie_*', 'panoplie_*.png'))):
        name = os.path.basename(src)
        if not name.endswith(('_vues.png', '_emissive.png')):
            shutil.copyfile(src, path(ASSETS, 'textures', 'entity', 'armor', name))


def weapons():
    H = 'minecraft:item/handheld'
    # Guerrier
    simple_item('epee_recrue', "Épée de la Recrue", gi.sword('#c8ccd4', '#8a6a3a', '#5a3a20'), H)
    simple_item('lame_runique', "Lame Runique", gi.sword('#9fb4d8', '#7a8090', '#3a3040', rune='#60f0ff'), H)
    simple_item('hache_berserker', "Hache du Berserker", gi.axe('#a8a8b0', '#6a3a20', edge='#e0e4ea', double=True), H)
    simple_item('epee_seigneur_guerre', "Épée du Seigneur de Guerre",
                gi.sword('#d8dce8', '#e0b030', '#802020', gem='#e02030', width=3, guard_len=4), H)
    simple_item('lame_demoniaque', "Lame Démoniaque",
                gi.sword('#a02838', '#2a1010', '#401010', gem='#ff4020', jagged=True, glow='#ff5030'), H)
    simple_item('excalibur_celeste', "Excalibur Céleste",
                gi.sword('#f0f8ff', '#ffd84a', '#3050c0', gem='#40c0ff', width=3, wings=True, glow='#fff8b0'), H)
    # Mage
    simple_item('baton_apprenti', "Bâton de l'Apprenti", gi.staff('#8a5a30', 'orb', '#6a4020', gem='#60a0ff'), H)
    simple_item('sceptre_givre', "Sceptre de Givre", gi.staff('#c0d8f0', 'crystal', '#80a0c0', gem='#90e8ff'), H)
    simple_item('baton_flammes', "Bâton des Flammes Infernales", gi.staff('#4a2a1a', 'flame', '#303030', gem='#ff6020'), H)
    simple_item('baton_arcanique', "Bâton Arcanique", gi.staff('#5a3a7a', 'ring', '#d0a030', gem='#c060ff'), H)
    simple_item('sceptre_neant', "Sceptre du Néant", gi.staff('#2a1838', 'crescent', '#7040b0', gem='#e080ff', glow='#c060ff'), H)
    simple_item('baton_archimage', "Bâton de l'Archimage Éternel", gi.staff('#e8e8f0', 'star', '#ffd040', gem='#fff4a0', glow='#ffffa0'), H)
    # Archer
    bow_item('arc_chasseur', "Arc du Chasseur", wood='#8a5a2a')
    bow_item('arc_elfique', "Arc Elfique", wood='#5aa040', tip='#e0e0a0', recurve=True)
    bow_item('arc_tempete', "Arc de la Tempête", wood='#4060a0', tip='#a0e0ff', gem='#ffff60')
    bow_item('arc_faucon', "Arc du Faucon", wood='#a07030', tip='#f0f0f0', gem='#40c0a0', recurve=True)
    bow_item('arc_spectral', "Arc Spectral", wood='#60a0a0', string='#c0ffff', tip='#e0ffff', gem='#80ffff')
    bow_item('arc_aube_divine', "Arc de l'Aube Divine", wood='#f0d060', string='#fff8d0', tip='#ffffff', gem='#ff8040', recurve=True)
    # Tank
    simple_item('masse_garde', "Masse du Garde", gi.hammer('#9a9aa0', '#6a4a2a', round_head=True, size=3), H)
    simple_item('marteau_bastion', "Marteau du Bastion", gi.hammer('#808890', '#5a3a20', band='#b08040', size=3), H)
    simple_item('marteau_colosse', "Marteau du Colosse", gi.hammer('#606870', '#3a2a1a', band='#c0c0c0', spikes=True, size=4), H)
    simple_item('masse_gardien', "Masse du Gardien Sacré",
                gi.hammer('#e0e0e8', '#3050a0', band='#ffd040', gem='#40a0ff', round_head=True, spikes=True, size=3.6), H)
    simple_item('marteau_titan', "Marteau Titanesque", gi.hammer('#5a4a3a', '#2a1a10', band='#ff8020', gem='#ff4020', spikes=True, size=4), H)
    simple_item('marteau_egide', "Marteau de l'Égide Éternelle", gi.hammer('#f8f8ff', '#d0a020', band='#ffd84a', gem='#40e0ff', size=4), H)


# ---------------------------------------------------------------------------
# Armures
# ---------------------------------------------------------------------------

ARMOR_SETS = [
    ('acier_soldat', "d'Acier du Soldat", 'plate', '#a0a4ac', '#6a4a2a', '#c04040'),
    ('berserker', "du Berserker", 'plate', '#7a3a2a', '#c0c0c0', '#ff3020'),
    ('seigneur_guerre', "du Seigneur de Guerre", 'plate', '#c8ccd8', '#e0b030', '#d02030'),
    ('dieu_guerre', "du Dieu de la Guerre", 'plate', '#e8c040', '#a01818', '#ff6030'),
    ('apprenti', "de l'Apprenti", 'robe', '#4060a0', '#d0c080', '#60c0ff'),
    ('sorcier', "du Sorcier", 'robe', '#5a2a7a', '#c0a040', '#c060ff'),
    ('archimage', "de l'Archimage", 'robe', '#e8e8f0', '#4080e0', '#40c0ff'),
    ('avatar_arcanique', "de l'Avatar Arcanique", 'robe', '#2a1050', '#ffd040', '#ff60ff'),
    ('chasseur', "du Chasseur", 'leather', '#7a5a3a', '#4a6a2a', '#a0d060'),
    ('rodeur', "du Rôdeur", 'leather', '#3a5a3a', '#7a5a3a', '#60e060'),
    ('tireur_elite', "du Tireur d'Élite", 'leather', '#2a3a5a', '#c0c0c0', '#40c0ff'),
    ('sylvestre', "Sylvestre Divine", 'leather', '#4a9a4a', '#ffd84a', '#a0ff60'),
    ('garde', "du Garde", 'plate', '#8a8a90', '#2a50a0', '#e0e0e0'),
    ('gardien', "du Gardien", 'plate', '#6a7080', '#c09030', '#40a0ff'),
    ('paladin', "du Paladin", 'plate', '#f0f0f8', '#ffd040', '#40c0ff'),
    ('titan', "du Titan Immortel", 'plate', '#5a4a3a', '#ff8020', '#ff4020'),
    ('neant_primordial', "du Néant Primordial", 'plate', '#2a1240', '#a040ff', '#ff60ff'),
]
PIECE_NAMES = {
    'plate': {'casque': 'Casque', 'plastron': 'Plastron', 'jambieres': 'Jambières', 'bottes': 'Bottes'},
    'robe': {'casque': 'Capuche', 'plastron': 'Robe', 'jambieres': 'Pantalon', 'bottes': 'Sandales'},
    'leather': {'casque': 'Coiffe', 'plastron': 'Veste', 'jambieres': 'Pantalon', 'bottes': 'Bottes'},
}
# accord : "Bottes" / "Sandales" -> pluriel feminin, le complement reste identique


def armors():
    for i, (sid, suffix, style, base, trim, gem) in enumerate(ARMOR_SETS):
        outer, legs = ge.armor_layers(base, trim, gem, style, seed=i * 31)
        outer.save(path(ASSETS, 'textures', 'entity', 'equipment', 'humanoid', sid + '.png'))
        legs.save(path(ASSETS, 'textures', 'entity', 'equipment', 'humanoid_leggings', sid + '.png'))
        write_json(os.path.join(ASSETS, 'equipment', sid + '.json'), {
            'layers': {
                'humanoid': [{'texture': f'{MODID}:{sid}'}],
                'humanoid_leggings': [{'texture': f'{MODID}:{sid}'}],
            }
        })
        for piece in ('casque', 'plastron', 'jambieres', 'bottes'):
            iid = f'{sid}_{piece}'
            cv = gi.armor_icon(piece, base, trim, gem, robe=(style == 'robe'))
            simple_item(iid, f"{PIECE_NAMES[style][piece]} {suffix}", cv)


# ---------------------------------------------------------------------------
# Materiaux, consommables, invocations
# ---------------------------------------------------------------------------

MATERIALS = [
    ('oreille_gobelin', "Oreille de Gobelin", 'ear', '#6ab040', '#c05050', None),
    ('ferraille_gobeline', "Ferraille Gobeline", 'scrap', '#8a7a60', None, None),
    ('croc_loup', "Croc de Loup Sombre", 'fang', '#e8e0d0', None, None),
    ('fourrure_sombre', "Fourrure Sombre", 'fur', '#4a4258', None, None),
    ('os_maudit', "Os Maudit", 'bone', '#d8d0c0', '#a040c0', '#b060ff'),
    ('poussiere_ame', "Poussière d'Âme", 'dust', '#60e0d0', None, None),
    ('defense_orc', "Défense d'Orc", 'tusk', '#f0e8c8', '#6a8a3a', None),
    ('acier_orc', "Acier Orc", 'ingot', '#6a7068', None, None),
    ('noyau_flamme', "Noyau de Flamme", 'core', '#ff6020', '#ffd040', None),
    ('eclat_givre', "Éclat de Givre", 'shard', '#80d8ff', None, '#e0ffff'),
    ('cristal_arcanique', "Cristal Arcanique", 'crystal', '#c060ff', None, None),
    ('essence_neant', "Essence du Néant", 'orb', '#40106a', '#e060ff', '#e080ff'),
    ('couronne_roi_gobelin', "Couronne du Roi Gobelin", 'crown', '#e0b030', '#e02040', None),
    ('phylactere_liche', "Phylactère de la Liche", 'vial', '#60ff90', None, '#a0ffc0'),
    ('coeur_infernal', "Cœur Infernal", 'heart', '#c02010', None, '#ff8040'),
    ('coeur_glace_eternelle', "Cœur de Glace Éternelle", 'heart', '#80e0ff', None, '#ffffff'),
    ('fragment_divin', "Fragment Divin", 'fragment', '#fff0a0', None, '#ffffff'),
    ('pierre_amelioration', "Pierre d'Amélioration", 'stone', '#7a8090', '#60c0ff', None),
    ('pierre_amelioration_sup', "Pierre d'Amélioration Supérieure", 'stone', '#4a3a6a', '#ff60ff', '#ff90ff'),
    ('lingot_mithril', "Lingot de Mithril", 'ingot', '#a0d0e8', None, None),
    ('lingot_adamantite', "Lingot d'Adamantite", 'ingot', '#c03040', None, None),
    ('tissu_enchante', "Tissu Enchanté", 'cloth', '#6040a0', '#e0c060', None),
    ('cuir_renforce', "Cuir Renforcé", 'leather', '#8a5a30', '#d0d0d0', None),
    ('piece_or', "Pièce d'Or", 'coin', '#f0c030', '#c08010', None),
    # invocations de boss
    ('sceau_roi_gobelin', "Sceau du Roi Gobelin", 'seal', '#6ab040', '#e0b030', None),
    ('grimoire_interdit', "Grimoire Interdit", 'book', '#3a2a4a', '#60ff90', '#60ff90'),
    ('braise_eternelle', "Braise Éternelle", 'flame', '#ff5020', '#ffd040', None),
    ('cristal_glacial', "Cristal Glacial Ancien", 'crystal', '#a0e8ff', None, '#ffffff'),
    ('oeil_neant', "Œil du Néant", 'eye', '#2a1040', '#ff40ff', '#ff80ff'),
    # consommables
    ('potion_soin_mineure', "Potion de Soin Mineure", 'potion', '#ff6060', None, None),
    ('potion_soin', "Potion de Soin", 'potion', '#e02020', None, None),
    ('potion_soin_majeure', "Potion de Soin Majeure", 'potion', '#a00010', None, '#ff8080'),
    ('potion_mana_mineure', "Potion de Mana Mineure", 'potion', '#60a0ff', None, None),
    ('potion_mana', "Potion de Mana", 'potion', '#2060e0', None, None),
    ('potion_mana_majeure', "Potion de Mana Majeure", 'potion', '#1030a0', None, '#80a0ff'),
    ('elixir_experience', "Élixir d'Expérience", 'potion', '#80ff40', None, '#e0ffa0'),
    ('parchemin_teleportation', "Parchemin de Téléportation", 'scroll', '#f0e0b0', '#40a0ff', None),
    ('parchemin_oubli', "Parchemin d'Oubli", 'scroll', '#e0d0c0', '#a040a0', None),
    ('orbe_renaissance', "Orbe de Renaissance", 'orb', '#ffd040', '#ffffff', '#ffffa0'),
    ('coffre_cosmetique', "Coffre Cosmétique", 'chest', '#8a5a2a', '#ffd040', '#ffff80'),
]


def materials():
    for iid, name, kind, base, acc, glow in MATERIALS:
        acc2 = None
        if kind == 'book':
            acc2 = acc
        simple_item(iid, name, gi.template_sprite(kind, base, acc, acc2, glow=glow))


def clover_sprite(leaves, base, stem, glow=None):
    """Trefle d'amelioration : feuilles en coeur (3 ou 4) autour d'un centre, nervures, tige."""
    from pixel import Canvas, ramp, shaded_circle
    P, S = ramp(base), ramp(stem)
    cv = Canvas()
    for (x, y) in [(8, 9), (9, 10), (9, 11), (10, 12), (10, 13), (11, 14)]:
        cv.set(x, y, S['c'], 's')
    angles = [-90, 30, 150] if leaves == 3 else [-135, -45, 45, 135]
    for a in angles:
        r = math.radians(a)
        cx, cy = 8 + math.cos(r) * 3.9, 7.5 + math.sin(r) * 3.9
        px, py = -math.sin(r) * 1.25, math.cos(r) * 1.25
        shaded_circle(cv, cx + px, cy + py, 1.9, P)
        shaded_circle(cv, cx - px, cy - py, 1.9, P)
        for t in (1.6, 2.4):      # pointe de la feuille vers le centre
            cv.set(int(8 + math.cos(r) * t), int(7.5 + math.sin(r) * t), P['b'], 'b')
    for a in angles:
        r = math.radians(a)
        for t in (2.2, 3.1, 4.0):
            cv.set(int(8 + math.cos(r) * t), int(7.5 + math.sin(r) * t), P['c'], 'v')
    for a in angles:          # sillon sombre entre deux feuilles
        r = math.radians(a + 180 / len(angles))
        for t in (2.6, 3.6, 4.6, 5.6):
            x, y = int(8 + math.cos(r) * t), int(7.5 + math.sin(r) * t)
            if cv.filled(x, y):
                cv.set(x, y, P['o'], 'o')
    cv.set(8, 7, P['w'], 'w')
    cv.outline(shade(P['o'], 0.85))
    if glow:
        gi._sparkle(cv, glow)
    return cv


def clovers():
    simple_item('trefle_chance', "Trèfle de Chance", clover_sprite(3, '#4caa3a', '#2e6a22'))
    simple_item('trefle_quatre_feuilles', "Trèfle à Quatre Feuilles", clover_sprite(4, '#3ec25a', '#25702f', glow='#c8ffb0'))
    simple_item('trefle_dore', "Trèfle Doré", clover_sprite(4, '#f0c030', '#9a7018', glow='#fff2a0'))
    simple_item('trefle_celeste', "Trèfle Céleste", clover_sprite(4, '#80d8ff', '#3a78a0', glow='#e4f8ff'))
    simple_item('trefle_divin', "Trèfle Divin", clover_sprite(4, '#c46cff', '#6a2a9a', glow='#ffd4ff'))


PETS = [
    ('feu_follet', "Feu Follet", '#a0f0ff', '#30a0e0'),
    ('slime', "Bébé Slime", '#80e060', '#409030'),
    ('chouette', "Chouette Sage", '#c09060', '#6a4020'),
    ('loup_spectral', "Loup Spectral", '#c0d8ff', '#6080c0'),
    ('golem', "Golem de Poche", '#a8a498', '#40ff90'),
    ('fee', "Fée Lumineuse", '#ffd8f0', '#ff70c0'),
    ('phenix', "Phénix Doré", '#ffd040', '#ff4020'),
    ('dragonnet', "Dragonnet du Néant", '#9050e0', '#ff60ff'),
]


def pets():
    for pid, name, base, spots in PETS:
        egg = gi.spawn_egg(base, spots)
        gi._sparkle(egg, spots)
        simple_item('oeuf_' + pid, f"Œuf de familier : {name}", egg)
        simple_item('familier_' + pid, name, gi.pet_sprite(pid))


PROJECTILES = ['feu', 'givre', 'arcane', 'neant', 'ombre', 'eclair']


def projectiles():
    for p in PROJECTILES:
        simple_item('projectile_' + p, f"Projectile ({p})", gi.projectile_sprite(p))


MOBS = [
    ('gobelin', "Gobelin", '#5a9a30', '#8a5a2a'),
    ('loup_sombre', "Loup Sombre", '#2a2838', '#c02020'),
    ('squelette_maudit', "Squelette Maudit", '#d8d0c0', '#6a2a8a'),
    ('orc_guerrier', "Orc Guerrier", '#4a6a2a', '#7a7a80'),
    ('elementaire_feu', "Élémentaire de Feu", '#ff6020', '#ffd040'),
    ('spectre_givre', "Spectre de Givre", '#a0d8ff', '#e0ffff'),
    ('golem_cristal', "Golem de Cristal", '#8a6ab0', '#e0a0ff'),
    ('chevalier_neant', "Chevalier du Néant", '#1a1028', '#a040ff'),
    ('roi_gobelin', "Roi Gobelin", '#5a9a30', '#e0b030'),
    ('liche_ancienne', "Liche Ancienne", '#d8d0c0', '#40ff80'),
    ('seigneur_ignis', "Seigneur Démon Ignis", '#801010', '#ff8020'),
    ('titan_glace', "Titan de Glace", '#c0e8ff', '#4080c0'),
    ('avatar_neant', "Avatar du Néant", '#100818', '#ff40ff'),
]

HUMANOID_SKINS = {
    'gobelin': dict(skin='#6ab040', eyes='#d02020', glow=True, shirt='#7a5a3a', sleeve='#7a5a3a', sleeve_len=4,
                    pants='#5a4030', shoes='#3a2a1a', belt='#3a2a1a', ears=True, mouth='#2a1a10', specks='#5a9a30'),
    'roi_gobelin': dict(skin='#5aa038', eyes='#ffcc00', glow=True, shirt='#a02020', sleeve='#a02020', sleeve_len=8,
                        pants='#5a3a20', belt='#e0b030', cape='#c02020', ears=True, mouth='#2a1a10', emblem='#e0b030',
                        hat=dict(kind='crown', color='#e0b030', gem='#40c0ff'), shoes='#2a1a10'),
    'orc_guerrier': dict(skin='#5a7a3a', eyes='#ffd040', glow=True, tusks=True, brow='#2a3a1a', shirt='#6a4a2a',
                         armor='#7a7a80', pants='#4a3a2a', belt='#2a2a2a', hair='#1a1a1a', sleeve_len=2),
    'spectre_givre': dict(skin='#c8e8ff', eyes='#2080ff', glow=True, shirt='#a0d0f0', robe=True, sleeve='#a0d0f0',
                          sleeve_len=10, hat=dict(kind='hood', color='#80b8e0'), specks='#ffffff', shoes='#a0d0f0'),
    'chevalier_neant': dict(skin='#1a1028', eyes='#c060ff', glow=True, armor='#2a1a3a', shirt='#2a1a3a', sleeve='#2a1a3a',
                            sleeve_len=12, pants='#2a1a3a', shoes='#1a1020', gloves='#3a2a4a', emblem='#a040ff',
                            hat=dict(kind='helmet', color='#2a1a3a', visor='#c060ff')),
    'liche_ancienne': dict(skin='#d8d0c0', skull=True, eyes='#40ff80', glow=True, shirt='#3a1a4a', robe=True,
                           sleeve='#3a1a4a', sleeve_len=10, belt='#c0a040', emblem='#40ff80', shoes='#2a1a3a',
                           hat=dict(kind='crown', color='#a0a0a0', gem='#40ff80')),
    'seigneur_ignis': dict(skin='#801818', eyes='#ffd040', glow=True, shirt='#2a1010', armor='#3a1a10', pants='#2a1010',
                           belt='#ff8020', mouth='#ff8020', specks='#ff6020', shoes='#1a0808',
                           hat=dict(kind='horns', color='#2a2020')),
    'avatar_neant': dict(skin='#0c0614', eyes='#ff40ff', glow=True, shirt='#1a0a2a', armor='#2a1040', robe=True,
                         sleeve='#1a0a2a', sleeve_len=12, specks='#c080ff', emblem='#ff40ff', shoes='#1a0a2a',
                         hat=dict(kind='crown', color='#a040ff', gem='#ffffff')),
}


def mobs():
    for i, (mid, name, egg_a, egg_b) in enumerate(MOBS):
        LANG[f'entity.{MODID}.{mid}'] = name
        egg = gi.spawn_egg(egg_a, egg_b)
        simple_item(mid + '_spawn_egg', f"Œuf d'apparition : {name}", egg)
        tex = os.path.join('entity', mid)
        if mid in HUMANOID_SKINS:
            save_tex(ge.humanoid_skin(HUMANOID_SKINS[mid], seed=i * 17), tex)
    save_tex(ge.skeleton_texture('#cfc6b0', '#4a2a5a', '#c040ff', seed=3), 'entity/squelette_maudit')
    save_tex(ge.blaze_texture('#ff7020', '#ffc040', '#ffffff', seed=4), 'entity/elementaire_feu')
    save_tex(ge.golem_texture('#7a5aa0', '#e0a0ff', '#ff60ff', seed=5, cracks='#4a2a6a'), 'entity/golem_cristal')
    save_tex(ge.golem_texture('#a8d8f0', '#ffffff', '#2060ff', seed=6, cracks='#6090b0'), 'entity/titan_glace')
    save_tex(ge.wolf_texture('#2a2836', '#4a4458', '#ff2020', seed=7), 'entity/loup_sombre')
    npc_skins = [
        dict(skin='#f0d0b0', hair='#e8e8f0', eyes='#3060a0', shirt='#3a3a8a', sleeve='#3a3a8a', sleeve_len=12, robe=True,
             belt='#e0b030', emblem='#ffd040', shoes='#2a2050', mouth='#d0d0d8', hat=dict(kind='hood', color='#2a2a70')),
        dict(skin='#e8b890', hair='#6a4020', eyes='#2a1a10', shirt='#3a7a3a', sleeve='#3a7a3a', sleeve_len=6, pants='#5a4020',
             belt='#8a5a2a', shoes='#3a2a1a', armor='#8a5a2a', mouth='#a06050'),
        dict(skin='#f0d8c0', hair='#f0d060', eyes='#40a060', shirt='#a02040', sleeve='#a02040', sleeve_len=10, robe=True,
             belt='#ffd040', shoes='#601020', hat=dict(kind='crown', color='#ffd040', gem='#40c0ff')),
        dict(skin='#e0b090', hair='#3a2a1a', eyes='#202020', shirt='#a0a4ac', sleeve='#a0a4ac', sleeve_len=12, pants='#6a6a70',
             armor='#c0c4cc', belt='#6a4a2a', shoes='#3a3a40', emblem='#3060c0', hat=dict(kind='helmet', color='#a0a4ac', visor='#202020')),
    ]
    for i, spec in enumerate(npc_skins):
        save_tex(ge.humanoid_skin(spec, seed=900 + i), f'entity/pnj_{i}')
    LANG[f'entity.{MODID}.pnj'] = "PNJ"
    LANG[f'entity.{MODID}.projectile_magique'] = "Projectile magique"
    LANG[f'entity.{MODID}.projectile_sort'] = "Projectile de sort"
    LANG[f'entity.{MODID}.effet_competence'] = "Effet de compétence"
    LANG[f'entity.{MODID}.familier'] = "Familier"


# ---------------------------------------------------------------------------
# Blocs
# ---------------------------------------------------------------------------

def bricks(base, mortar, seed=0, accent=None, accent_rate=0.0):
    img = Image.new('RGBA', (16, 16))
    rnd = random.Random(seed)
    B, M = rgb(base), rgb(mortar)
    A = rgb(accent) if accent else None
    for y in range(16):
        for x in range(16):
            row = y // 4
            off = 4 if row % 2 else 0
            if y % 4 == 3 or (x + off) % 8 == 7:
                c = M
            else:
                c = shade(B, 1 + rnd.uniform(-0.1, 0.1))
                if y % 4 == 0:
                    c = shade(c, 1.12)
                if A and rnd.random() < accent_rate:
                    c = A
            img.putpixel((x, y), c)
    return img


def rune_overlay(img, color, pattern):
    C = rgb(color)
    for (x, y) in pattern:
        img.putpixel((x, y), C)
        if 0 <= x + 1 < 16:
            pass
    return img


RUNE_CIRCLE = [(x, y) for x in range(16) for y in range(16)
               if 4.5 <= math.hypot(x + 0.5 - 8, y + 0.5 - 8) <= 5.6]
RUNE_MARKS = [(8, 2), (8, 13), (2, 8), (13, 8), (7, 7), (8, 8), (7, 8), (8, 7)]


def blocks():
    # Forge arcanique
    side = bricks('#5a5660', '#2a2830', seed=1)
    for x in range(16):
        side.putpixel((x, 0), rgb('#8a7040'))
        side.putpixel((x, 1), rgb('#6a5430'))
    save_tex(side, 'block/forge_arcanique_side')
    front = side.copy()
    for y in range(6, 14):
        for x in range(4, 12):
            t = (y - 6) / 8
            c = mix(rgb('#ffd060'), rgb('#e04010'), t) if (x + y) % 3 else rgb('#ff8020')
            if y == 6 or x in (4, 11):
                c = rgb('#1a1418')
            front.putpixel((x, y), c)
    save_tex(front, 'block/forge_arcanique_front')
    top = Image.new('RGBA', (16, 16))
    rnd = random.Random(9)
    for y in range(16):
        for x in range(16):
            c = shade(rgb('#3a3840'), 1 + rnd.uniform(-0.08, 0.08))
            if x in (0, 15) or y in (0, 15):
                c = rgb('#8a7040')
            top.putpixel((x, y), c)
    rune_overlay(top, '#60e0ff', RUNE_CIRCLE)
    rune_overlay(top, '#c0ffff', RUNE_MARKS)
    save_tex(top, 'block/forge_arcanique_top')
    write_json(os.path.join(ASSETS, 'models', 'block', 'forge_arcanique.json'), {
        'parent': 'minecraft:block/orientable',
        'textures': {'top': f'{MODID}:block/forge_arcanique_top', 'front': f'{MODID}:block/forge_arcanique_front',
                     'side': f'{MODID}:block/forge_arcanique_side'}
    })
    write_json(os.path.join(ASSETS, 'blockstates', 'forge_arcanique.json'), {
        'variants': {
            'facing=north': {'model': f'{MODID}:block/forge_arcanique'},
            'facing=east': {'model': f'{MODID}:block/forge_arcanique', 'y': 90},
            'facing=south': {'model': f'{MODID}:block/forge_arcanique', 'y': 180},
            'facing=west': {'model': f'{MODID}:block/forge_arcanique', 'y': 270},
        }
    })

    # Teleporteur
    base = bricks('#d8d4c8', '#8a8478', seed=2)
    save_tex(base, 'block/teleporteur_base')
    btop = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            c = shade(rgb('#d0ccc0'), 1 + random.Random(x * 31 + y).uniform(-0.06, 0.06))
            if x in (0, 15) or y in (0, 15):
                c = rgb('#c0a040')
            btop.putpixel((x, y), c)
    rune_overlay(btop, '#40a0ff', RUNE_CIRCLE)
    save_tex(btop, 'block/teleporteur_top')
    pillar = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            c = rgb('#c0bcb0') if x % 4 else rgb('#9a968a')
            if y in (0, 15):
                c = rgb('#c0a040')
            pillar.putpixel((x, y), c)
    save_tex(pillar, 'block/teleporteur_pillar')
    crystal = Image.new('RGBA', (16, 16))
    for y in range(16):
        for x in range(16):
            t = abs(x - 7.5) / 8 + abs(y - 7.5) / 16
            crystal.putpixel((x, y), mix(rgb('#e0f8ff'), rgb('#2070e0'), min(1, t)))
    save_tex(crystal, 'block/teleporteur_crystal')
    tex = {'base': f'{MODID}:block/teleporteur_base', 'top': f'{MODID}:block/teleporteur_top',
           'pillar': f'{MODID}:block/teleporteur_pillar', 'crystal': f'{MODID}:block/teleporteur_crystal',
           'particle': f'{MODID}:block/teleporteur_base'}

    def box(fr, to, side_tex, top_tex, light=0):
        el = {'from': fr, 'to': to, 'faces': {
            d: {'texture': '#' + (top_tex if d in ('up', 'down') else side_tex)}
            for d in ('north', 'south', 'east', 'west', 'up', 'down')}}
        if light:
            el['light_emission'] = light
        return el
    write_json(os.path.join(ASSETS, 'models', 'block', 'teleporteur.json'), {
        'parent': 'minecraft:block/block', 'textures': tex,
        'elements': [
            box([0, 0, 0], [16, 3, 16], 'base', 'top'),
            box([3, 3, 3], [13, 5, 13], 'pillar', 'pillar'),
            box([5, 5, 5], [11, 12, 11], 'pillar', 'pillar'),
            box([6, 13, 6], [10, 19, 10], 'crystal', 'crystal', light=15),
        ]
    })
    write_json(os.path.join(ASSETS, 'blockstates', 'teleporteur.json'),
               {'variants': {'': {'model': f'{MODID}:block/teleporteur'}}})

    # Autel d'invocation
    obs = Image.new('RGBA', (16, 16))
    rnd = random.Random(4)
    for y in range(16):
        for x in range(16):
            c = shade(rgb('#1e1428'), 1 + rnd.uniform(-0.15, 0.15))
            if rnd.random() < 0.05:
                c = rgb('#5a2a8a')
            obs.putpixel((x, y), c)
    save_tex(obs, 'block/autel_invocation_side')
    atop = obs.copy()
    rune_overlay(atop, '#ff40a0', RUNE_CIRCLE)
    rune_overlay(atop, '#ffa0e0', RUNE_MARKS)
    save_tex(atop, 'block/autel_invocation_top')
    write_json(os.path.join(ASSETS, 'models', 'block', 'autel_invocation.json'), {
        'parent': 'minecraft:block/block',
        'textures': {'side': f'{MODID}:block/autel_invocation_side', 'top': f'{MODID}:block/autel_invocation_top',
                     'particle': f'{MODID}:block/autel_invocation_side'},
        'elements': [
            box([0, 0, 0], [16, 4, 16], 'side', 'side'),
            box([2, 4, 2], [14, 10, 14], 'side', 'top'),
            box([0, 10, 0], [16, 12, 16], 'side', 'top', light=8),
        ]
    })
    write_json(os.path.join(ASSETS, 'blockstates', 'autel_invocation.json'),
               {'variants': {'': {'model': f'{MODID}:block/autel_invocation'}}})

    names = {'forge_arcanique': "Forge Arcanique", 'teleporteur': "Téléporteur",
             'autel_invocation': "Autel d'Invocation"}
    for bid, name in names.items():
        LANG[f'block.{MODID}.{bid}'] = name
        write_json(os.path.join(ASSETS, 'items', bid + '.json'),
                   {'model': {'type': 'minecraft:model', 'model': f'{MODID}:block/{bid}'}})
        write_json(os.path.join(DATA, MODID, 'loot_table', 'blocks', bid + '.json'), {
            'type': 'minecraft:block',
            'pools': [{'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'{MODID}:{bid}'}],
                       'conditions': [{'condition': 'minecraft:survives_explosion'}]}],
            'random_sequence': f'{MODID}:blocks/{bid}'
        })
        MANIFEST.append(('block', bid))

    # Partie invisible des structures multiblocs : seulement une texture de particules (casse)
    write_json(os.path.join(ASSETS, 'models', 'block', 'structure_part.json'),
               {'textures': {'particle': f'{MODID}:block/autel_invocation_side'}})
    write_json(os.path.join(ASSETS, 'blockstates', 'structure_part.json'),
               {'variants': {'': {'model': f'{MODID}:block/structure_part'}}})
    LANG[f'block.{MODID}.structure_part'] = "Structure"

    write_json(os.path.join(DATA, MODID, 'recipe', 'forge_arcanique.json'), {
        'type': 'minecraft:crafting_shaped', 'pattern': ['III', 'IFI', 'BBB'],
        'key': {'I': 'minecraft:iron_ingot', 'F': 'minecraft:furnace', 'B': 'minecraft:stone_bricks'},
        'result': {'id': f'{MODID}:forge_arcanique'}})
    # pas de recette pour le teleporteur : c'est un outil d'administration (creatif / commandes)
    write_json(os.path.join(DATA, MODID, 'recipe', 'autel_invocation.json'), {
        'type': 'minecraft:crafting_shaped', 'pattern': [' D ', 'GOG', 'SSS'],
        'key': {'D': 'minecraft:diamond', 'G': 'minecraft:gold_ingot', 'O': 'minecraft:obsidian',
                'S': 'minecraft:stone_bricks'},
        'result': {'id': f'{MODID}:autel_invocation'}})


# ---------------------------------------------------------------------------
# Icones de GUI (competences, classes)
# ---------------------------------------------------------------------------

CLASS_COLORS = {
    'guerrier': ('#d04030', '#3a0c08'),
    'mage': ('#7a50e0', '#160a3a'),
    'archer': ('#40b040', '#0a2a0a'),
    'tank': ('#3a80c8', '#081a30'),
}

SKILLS = [
    ('frappe_fragilisante', 'guerrier', 'sword', '#ffffff', '#ffe080'),
    ('execution', 'guerrier', 'crossed', '#ffffff', '#ffe080'),
    ('impact_vampirique', 'guerrier', 'drop', '#ffffff', '#ffe080'),
    ('second_souffle', 'guerrier', 'heal', '#ffffff', '#ffe080'),
    ('lance_de_givre', 'mage', 'snow', '#ffffff', '#ffe080'),
    ('souffle_draconique', 'mage', 'flame', '#ffffff', '#ffe080'),
    ('implosion', 'mage', 'spiral', '#ffffff', '#ffe080'),
    ('meditation', 'mage', 'star', '#ffffff', '#ffe080'),
    ('tir_entravant', 'archer', 'pierce', '#ffffff', '#ffe080'),
    ('tir_venimeux', 'archer', 'arrow', '#ffffff', '#ffe080'),
    ('tir_de_recul', 'archer', 'back', '#ffffff', '#ffe080'),
    ('pas_leger', 'archer', 'dash', '#ffffff', '#ffe080'),
    ('crochet_du_gardien', 'tank', 'taunt', '#ffffff', '#ffe080'),
    ('jugement', 'tank', 'fist', '#ffffff', '#ffe080'),
    ('garde_partagee', 'tank', 'shield', '#ffffff', '#ffe080'),
    ('souffle_du_gardien', 'tank', 'heal', '#ffffff', '#ffe080'),
    # (id, classe, glyphe, couleur principale, accent)
    ('frappe_puissante', 'guerrier', 'sword', '#f0f0f0', '#e0b030'),
    ('cri_de_guerre', 'guerrier', 'shout', '#ffe0a0', '#ff6030'),
    ('maitrise_armes', 'guerrier', 'crossed', '#e0e0e8', '#c08030'),
    ('tourbillon', 'guerrier', 'spiral', '#ffffff', '#ffd040'),
    ('charge_brutale', 'guerrier', 'dash', '#ffd0a0', '#ffffff'),
    ('rage_sanguinaire', 'guerrier', 'drop', '#e01818', '#ff9090'),
    ('fureur_divine', 'guerrier', 'fist', '#ffd040', '#ffffff'),
    ('boule_de_feu', 'mage', 'flame', '#ff8020', '#ffe060'),
    ('nova_de_givre', 'mage', 'snow', '#c0f0ff', '#ffffff'),
    ('flux_arcanique', 'mage', 'star', '#e0a0ff', '#ffffff'),
    ('eclair_en_chaine', 'mage', 'bolt', '#fff070', '#ffffff'),
    ('teleportation_arcanique', 'mage', 'blink', '#d080ff', '#ffffff'),
    ('bouclier_de_mana', 'mage', 'bubble', '#80c0ff', '#ffffff'),
    ('meteore_celeste', 'mage', 'comet', '#ff9030', '#ffe080'),
    ('tir_percant', 'archer', 'pierce', '#f0f0f0', '#a0ffa0'),
    ('pluie_de_fleches', 'archer', 'rain', '#e0e0e0', '#ffd040'),
    ('oeil_de_lynx', 'archer', 'eye', '#f0ffe0', '#40c040'),
    ('saut_arriere', 'archer', 'back', '#ffffff', '#c0ffc0'),
    ('fleche_explosive', 'archer', 'explode', '#ffa040', '#ffe060'),
    ('volee_de_fleches', 'archer', 'multi', '#f0f0f0', '#ffd040'),
    ('tempete_divine', 'archer', 'arrow', '#fff0a0', '#ffffff'),
    ('coup_de_bouclier', 'tank', 'shield', '#c0c8d8', '#ffd040'),
    ('provocation', 'tank', 'taunt', '#ff6040', '#ffd040'),
    ('peau_de_fer', 'tank', 'fist', '#c0c8d0', '#ffffff'),
    ('forteresse', 'tank', 'tower', '#d0d0d8', '#40a0ff'),
    ('onde_de_choc', 'tank', 'waves', '#e0c080', '#ffffff'),
    ('aura_sacree', 'tank', 'heal', '#ffffff', '#ffd040'),
    ('rempart_du_titan', 'tank', 'crown', '#ffd040', '#ff4020'),
]


def icon(glyph, main, accent, cls, size=32, gold=False):
    bg_light, bg_dark = CLASS_COLORS[cls]
    L, D = rgb(bg_light), rgb(bg_dark)
    img = Image.new('RGBA', (size, size))
    c = (size - 1) / 2
    for y in range(size):
        for x in range(size):
            d = math.hypot(x - c, y - c) / (size / 2)
            col = mix(L, D, min(1, d * 0.95))
            img.putpixel((x, y), col)
    rows = GLYPHS[glyph]
    M, A = rgb(main), rgb(accent)
    O = shade(D, 0.5)
    sc = size // 16
    for pass_ in (0, 1):
        for gy, row in enumerate(rows):
            for gx, ch in enumerate(row):
                if ch == '.':
                    continue
                if pass_ == 0:
                    col = rgb('#000000')
                    col = (0, 0, 0, 150)
                    ox, oy = 1, 1
                else:
                    col = M if ch == '#' else (A if ch == '+' else O)
                    ox, oy = 0, 0
                for dy in range(sc):
                    for dx in range(sc):
                        px, py = gx * sc + dx + ox, gy * sc + dy + oy
                        if 0 <= px < size and 0 <= py < size:
                            if pass_ == 0:
                                base = img.getpixel((px, py))
                                img.putpixel((px, py), mix(base, (0, 0, 0, 255), 0.55))
                            else:
                                # leger degrade vertical sur le glyphe
                                f = 1.15 - 0.3 * (py / size)
                                img.putpixel((px, py), shade(col, f))
    # cadre
    frame = rgb('#e0b84a') if gold else rgb('#1a1410')
    hi = rgb('#fff0b0') if gold else shade(L, 1.2)
    for i in range(size):
        img.putpixel((i, 0), frame)
        img.putpixel((i, size - 1), frame)
        img.putpixel((0, i), frame)
        img.putpixel((size - 1, i), frame)
    for i in range(1, size - 1):
        img.putpixel((i, 1), hi)
        img.putpixel((1, i), hi)
    return img


def gui_icons():
    for sid, cls, glyph, main, acc in SKILLS:
        img = icon(glyph, main, acc, cls, gold=sid in ('fureur_divine', 'meteore_celeste', 'tempete_divine', 'rempart_du_titan'))
        img.save(path(ASSETS, 'textures', 'gui', 'skills', sid + '.png'))
    emblems = {'guerrier': ('crossed', '#f0f0f0', '#e0b030'), 'mage': ('staff', '#e0c8ff', '#80e0ff'),
               'archer': ('bow', '#e0c890', '#ffffff'), 'tank': ('shield', '#d0d8e8', '#ffd040')}
    for cls, (glyph, m, a) in emblems.items():
        img = icon(glyph, m, a, cls, size=32, gold=True)
        img.save(path(ASSETS, 'textures', 'gui', 'classes', cls + '.png'))
        big = icon(glyph, m, a, cls, size=64, gold=True)
        big.save(path(ASSETS, 'textures', 'gui', 'classes', cls + '_big.png'))


# ---------------------------------------------------------------------------
# Donnees diverses
# ---------------------------------------------------------------------------

def misc_data():
    write_json(os.path.join(DATA, MODID, 'damage_type', 'skill.json'),
               {'exhaustion': 0.0, 'message_id': f'{MODID}.skill', 'scaling': 'never'})
    for tag in ('bypasses_cooldown', 'bypasses_armor', 'bypasses_enchantments'):
        write_json(os.path.join(DATA, 'minecraft', 'tags', 'damage_type', tag + '.json'),
                   {'replace': False, 'values': [f'{MODID}:skill']})
    write_json(os.path.join(DATA, 'minecraft', 'tags', 'block', 'mineable', 'pickaxe.json'),
               {'replace': False, 'values': [f'{MODID}:forge_arcanique', f'{MODID}:teleporteur', f'{MODID}:autel_invocation']})
    LANG[f'death.attack.{MODID}.skill'] = "%1$s a été terrassé par une compétence"
    LANG[f'death.attack.{MODID}.skill.player'] = "%1$s a été terrassé par %2$s"
    LANG[f'death.attack.{MODID}.skill.item'] = "%1$s a été terrassé par %2$s avec %3$s"
    LANG[f'itemGroup.{MODID}.equipement'] = "MMORPG — Équipement"
    LANG[f'itemGroup.{MODID}.objets'] = "MMORPG — Objets & Créatures"
    LANG[f'key.category.{MODID}.{MODID}'] = "MMORPG"
    keys = {
        'menu': "Ouvrir le menu MMORPG", 'skills': "Ouvrir les compétences", 'pets': "Ouvrir les familiers",
        'cosmetics': "Ouvrir les cosmétiques", 'hud_edit': "Modifier l'interface (HUD)",
        'skill1': "Compétence 1", 'skill2': "Compétence 2", 'skill3': "Compétence 3",
        'skill4': "Compétence 4", 'skill5': "Compétence 5", 'skill6': "Compétence 6",
    }
    for k, v in keys.items():
        LANG[f'key.{MODID}.{k}'] = v
    LANG[f'{MODID}.configuration.title'] = "Configuration MMORPG"
    LANG[f'{MODID}.configuration.showDamageNumbers'] = "Afficher les dégâts flottants"
    LANG[f'{MODID}.configuration.showNameplates'] = "Afficher les barres de vie des monstres"
    LANG[f'{MODID}.configuration.showOwnCosmetics'] = "Afficher ses propres cosmétiques"
    LANG[f'{MODID}.configuration.hudScale'] = "Échelle de l'interface MMORPG"


def main():
    for d in ('textures', 'models', 'items', 'equipment', 'blockstates'):
        shutil.rmtree(os.path.join(ASSETS, d), ignore_errors=True)
    shutil.rmtree(os.path.join(DATA, MODID), ignore_errors=True)
    shutil.rmtree(os.path.join(DATA, 'minecraft'), ignore_errors=True)
    weapons()
    weapon_models_3d()
    entity_models_3d()
    armors()
    materials()
    clovers()
    pets()
    projectiles()
    mobs()
    blocks()
    gui_icons()
    gen_fx.generate()
    misc_data()
    lang = dict(sorted(LANG.items()))
    write_json(os.path.join(ASSETS, 'lang', 'fr_fr.json'), lang)
    write_json(os.path.join(ASSETS, 'lang', 'en_us.json'), lang)
    with open(os.path.join(os.path.dirname(__file__), 'manifest.txt'), 'w', encoding='utf-8') as f:
        for kind, iid in MANIFEST:
            f.write(f'{kind} {iid}\n')
    import generate_extensions
    generate_extensions.generate()
    print(f'{len(MANIFEST)} objets/blocs generes, {len(LANG)} traductions')


if __name__ == '__main__':
    main()
