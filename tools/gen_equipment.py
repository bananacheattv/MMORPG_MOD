"""
Equipement supplementaire : nouveaux materiaux de butin, 5 armes de plus par classe (variantes recolorees des
modeles 3D Blockbench), Epee du Berserker en 3D, et panoplies intermediaires (icones + textures 3D recolorees).

Appele par generate_assets.main() : gen_equipment.generate(sys.modules[__name__]).
"""
import colorsys
import os
import sys

from PIL import Image

sys.path.insert(0, os.path.dirname(__file__))

# id, nom, rarete, lore, gabarit de sprite, couleur, accent, eclat (la rarete et le lore sont dans ModItems.java)
MATERIALS = [
    ('soie_araignee', "Soie d'Araignée", 'cloth', '#e8e8f0', '#b0b0c8', None),
    ('griffe_loup', "Griffe de Loup", 'fang', '#bfb8a8', None, None),
    ('insigne_bandit', "Insigne de Bandit", 'seal', '#7a5030', '#d0a040', None),
    ('ichor_cryptes', "Ichor des Cryptes", 'vial', '#70c040', None, None),
    ('sang_orc', "Sang d'Orc", 'vial', '#a01818', None, None),
    ('cendre_ardente', "Cendre Ardente", 'dust', '#ff8030', None, '#ffd060'),
    ('crane_maudit', "Crâne Maudit", 'bone', '#e0d8c8', '#8030c0', '#c070ff'),
    ('voile_spectral', "Voile Spectral", 'cloth', '#b0e0ff', '#ffffff', '#e0ffff'),
    ('gemme_brute', "Gemme Brute", 'crystal', '#40d0c0', None, None),
    ('plaque_neant', "Plaque du Néant", 'ingot', '#3a1a5a', '#c060ff', '#d080ff'),
    ('ecorce_ancienne', "Écorce Ancienne", 'scrap', '#6a4a28', '#90b050', None),
    ('seve_lumineuse', "Sève Lumineuse", 'vial', '#d0e040', None, '#f0ff90'),
    ('coeur_racine', "Cœur de Racine", 'heart', '#4a8a30', None, '#a0ff70'),
    ('mucus_acide', "Mucus Acide", 'dust', '#a0e040', None, None),
    ('venin_marais', "Venin des Marais", 'vial', '#8040a0', None, None),
    ('ecaille_hydre', "Écaille d'Hydre", 'shard', '#2a7050', None, '#80ffc0'),
    ('fragment_ossuaire', "Fragment d'Ossuaire", 'bone', '#c8c0b0', None, None),
    ('relique_funeraire', "Relique Funéraire", 'coin', '#c0a040', '#60ff90', '#a0ffc0'),
    ('couronne_cryptes', "Couronne des Cryptes", 'crown', '#a0a0b0', '#9040d0', None),
    ('fourrure_polaire', "Fourrure Polaire", 'fur', '#e8f0f8', None, None),
    ('carapace_givree', "Carapace Givrée", 'shard', '#90c8f0', None, '#ffffff'),
    ('croc_wyrm', "Croc de Wyrm", 'fang', '#c0f0ff', None, '#ffffff'),
    ('carapace_cuivre', "Carapace de Cuivre", 'scrap', '#c07040', '#e0a060', None),
    ('bandelette_ancienne', "Bandelette Ancienne", 'cloth', '#d8c090', '#906030', None),
    ('braise_djinn', "Braise de Djinn", 'flame', '#ff5010', '#ffd040', None),
    ('eclat_astral', "Éclat Astral", 'shard', '#6060e0', None, '#c0c0ff'),
    ('poussiere_etoile', "Poussière d'Étoile", 'dust', '#f0e0a0', None, '#ffffff'),
    ('eclat_fracture', "Éclat Fracturé", 'fragment', '#9050e0', None, '#e0a0ff'),
    ('bois_enchante', "Bois Enchanté", 'ingot', '#8a6030', None, '#e0f070'),
    ('bronze_runique', "Bronze Runique", 'ingot', '#c08040', '#60d0ff', None),
    ('tissu_spectral', "Tissu Spectral", 'cloth', '#7090d0', '#e0f0ff', '#e0ffff'),
    ('cuir_polaire', "Cuir Polaire", 'leather', '#d0d8e0', '#8090a0', None),
    ('acier_stellaire', "Acier Stellaire", 'ingot', '#304080', None, '#a0c0ff'),
]

# nouvelle arme : (id, nom, modele 3D de base, decalage de teinte en degres, saturation, luminosite)
WEAPONS = [
    ('epee_milicien', "Épée du Milicien", 'epee_niv_01', 25, 0.8, 0.95),
    ('lame_acier_trempe', "Lame d'Acier Trempé", 'epee_niv_20', 190, 0.9, 1.05),
    ('epee_croise', "Épée du Croisé", 'epee_niv_40', 40, 1.2, 1.1),
    ('lame_tempetes', "Lame des Tempêtes", 'epee_niv_60', 200, 1.3, 1.05),
    ('epee_roi_dragon', "Épée du Roi-Dragon", 'epee_niv_80', 140, 1.2, 1.0),
    ('baton_saule', "Bâton de Saule", 'baton_niv001', 90, 0.9, 1.05),
    ('sceptre_marees', "Sceptre des Marées", 'baton_niv020', 160, 1.1, 1.0),
    ('baton_foudre', "Bâton de Foudre", 'baton_niv040', 60, 1.2, 1.1),
    ('sceptre_ombre', "Sceptre d'Ombre", 'baton_niv060', 260, 1.0, 0.75),
    ('baton_phenix', "Bâton du Phénix", 'baton_niv080', 330, 1.3, 1.1),
    ('arc_court', "Arc Court", 'arc_niv_01', 30, 0.8, 1.0),
    ('arc_composite', "Arc Composite", 'arc_niv_20', 330, 0.9, 0.95),
    ('arc_sylvain', "Arc Sylvain", 'arc_niv_40', 100, 1.2, 1.05),
    ('arc_lune_argent', "Arc de Lune d'Argent", 'arc_niv_60', 210, 0.5, 1.2),
    ('arc_dragon', "Arc du Dragon", 'arc_niv_80', 340, 1.3, 1.0),
    ('masse_cloutee', "Masse Cloutée", 'marteau_niv_01', 20, 0.7, 0.9),
    ('marteau_forgeron', "Marteau du Forgeron", 'marteau_niv_20', 30, 1.2, 1.0),
    ('masse_templier', "Masse du Templier", 'marteau_niv_40', 200, 1.1, 1.1),
    ('marteau_runique', "Marteau Runique", 'marteau_niv_60', 170, 1.2, 1.0),
    ('marteau_rois_anciens', "Marteau des Rois Anciens", 'marteau_niv_80', 45, 1.3, 1.1),
]

# panoplie intermediaire : (id, suffixe du nom, style, base, liseré, gemme) pour les icones ; texture 3D recoloree
ARMOR_SETS = [
    ('veteran', "du Vétéran", 'plate', '#7a7a80', '#a06030', '#e0e0e0'),
    ('croise', "du Croisé", 'plate', '#d8d8e0', '#c03030', '#ffd040'),
    ('conquerant', "du Conquérant", 'plate', '#5a5a68', '#d0a030', '#ff4040'),
    ('seigneur_dragon', "du Seigneur Dragon", 'plate', '#4a1a1a', '#e0a020', '#ff6020'),
    ('acolyte', "de l'Acolyte", 'robe', '#e0e0d0', '#6080c0', '#80c0ff'),
    ('enchanteur', "de l'Enchanteur", 'robe', '#3a2a6a', '#c0a050', '#c080ff'),
    ('mage_bataille', "du Mage de Bataille", 'robe', '#6a1a2a', '#a0a0b0', '#ff6080'),
    ('oracle_astral', "de l'Oracle Astral", 'robe', '#1a2a5a', '#e0d080', '#a0e0ff'),
    ('eclaireur', "de l'Éclaireur", 'leather', '#6a5a3a', '#3a6a3a', '#a0e080'),
    ('traqueur', "du Traqueur", 'leather', '#4a3a2a', '#8a2a2a', '#e06040'),
    ('lame_ombre', "de la Lame d'Ombre", 'leather', '#1a1a24', '#6a2a8a', '#c060ff'),
    ('chasseur_lunaire', "du Chasseur Lunaire", 'leather', '#2a3a5a', '#d0d8e8', '#b0d0ff'),
    ('sentinelle_fer', "de la Sentinelle de Fer", 'plate', '#8a8a90', '#4a4a50', '#c0c0c0'),
    ('rempart', "du Rempart", 'plate', '#6a6a78', '#3060c0', '#80c0ff'),
    ('croise_sacre', "du Croisé Sacré", 'plate', '#e8e8f0', '#e0b030', '#ffe080'),
    ('bastion_eternel', "du Bastion Éternel", 'plate', '#3a4a5a', '#40c0c0', '#a0ffff'),
]

# textures 3D recolorees : set -> (panoplie de base, decalage de teinte, saturation, luminosite)
ARMOR_TEXTURES = {
    'veteran': ('panoplie_05_niv_020', 25, 0.7, 0.9),
    'valkyrie': ('panoplie_20_niv_100', 50, 1.1, 1.15),
    'acolyte': ('panoplie_01_niv_001', 200, 0.8, 1.15),
    'enchanteur': ('panoplie_08_niv_035', 260, 1.2, 1.0),
    'mage_bataille': ('panoplie_11_niv_050', 330, 1.2, 1.0),
    'oracle_astral': ('panoplie_14_niv_065', 210, 1.2, 1.1),
    'eternel_arcanes': ('panoplie_20_niv_100', 270, 1.3, 1.05),
    'eclaireur': ('panoplie_02_niv_005', 80, 0.9, 1.0),
    'traqueur': ('panoplie_03_niv_010', 350, 1.1, 0.9),
    'lame_ombre': ('panoplie_09_niv_040', 280, 1.2, 0.7),
    'chasseur_lunaire': ('panoplie_17_niv_080', 220, 0.6, 1.2),
    'sentinelle_astrale': ('panoplie_20_niv_100', 160, 1.2, 1.05),
    'sentinelle_fer': ('panoplie_04_niv_015', 0, 0.3, 1.0),
    'rempart': ('panoplie_07_niv_030', 215, 1.2, 1.0),
    'croise_sacre': ('panoplie_13_niv_060', 45, 1.3, 1.15),
    'bastion_eternel': ('panoplie_18_niv_085', 175, 1.2, 1.0),
    'egide_divine': ('panoplie_20_niv_100', 210, 1.3, 1.1),
}


def recolor(img, hue, sat, val):
    """Decale la teinte (degres) et ajuste saturation/luminosite ; les gris recoivent une legere teinte."""
    img = img.convert('RGBA')
    px = img.load()
    shift = hue / 360.0
    tint = colorsys.hsv_to_rgb(shift % 1.0, 1.0, 1.0)
    for y in range(img.height):
        for x in range(img.width):
            r, g, b, a = px[x, y]
            if a == 0:
                continue
            h, s, v = colorsys.rgb_to_hsv(r / 255, g / 255, b / 255)
            if s < 0.12:                                   # metal gris : teinte legere pour distinguer la variante
                nr, ng, nb = colorsys.hsv_to_rgb(h, s, min(1.0, v * val))
                k = 0.18 * min(1.0, sat)
                nr, ng, nb = (nr * (1 - k) + tint[0] * nr * k * 1.4, ng * (1 - k) + tint[1] * ng * k * 1.4,
                              nb * (1 - k) + tint[2] * nb * k * 1.4)
            else:
                nr, ng, nb = colorsys.hsv_to_rgb((h + shift) % 1.0, min(1.0, s * sat), min(1.0, v * val))
            px[x, y] = (min(255, int(nr * 255)), min(255, int(ng * 255)), min(255, int(nb * 255)), a)
    return img


def weapon_variant(ga, iid, name, base, hue, sat, val):
    tex_dir = os.path.join(ga.ASSETS, 'textures', 'item')
    models = os.path.join(ga.ASSETS, 'models', 'item')
    recolor(Image.open(os.path.join(tex_dir, base + '.png')), hue, sat, val).save(ga.path(tex_dir, iid + '.png'))
    tex = {'0': f'{ga.MODID}:item/{iid}', 'particle': f'{ga.MODID}:item/{iid}'}
    ga.write_json(os.path.join(models, iid + '.json'), {'parent': f'{ga.MODID}:item/{base}', 'textures': tex})
    pulling = [i for i in range(3) if os.path.isfile(os.path.join(models, f'{base}_pulling_{i}.json'))]
    for i in pulling:
        ga.write_json(os.path.join(models, f'{iid}_pulling_{i}.json'), {'parent': f'{ga.MODID}:item/{base}_pulling_{i}', 'textures': tex})

    def m(n):
        return {'type': 'minecraft:model', 'model': f'{ga.MODID}:item/{n}'}
    if pulling:                                            # arc : etats de traction
        definition = {'model': {
            'type': 'minecraft:condition', 'property': 'minecraft:using_item', 'on_false': m(iid),
            'on_true': {'type': 'minecraft:range_dispatch', 'property': 'minecraft:use_duration', 'scale': 0.05,
                        'fallback': m(iid + '_pulling_0'),
                        'entries': [{'threshold': 0.65, 'model': m(iid + '_pulling_1')},
                                    {'threshold': 0.9, 'model': m(iid + '_pulling_2')}]}}}
    else:
        definition = {'model': m(iid)}
    ga.write_json(os.path.join(ga.ASSETS, 'items', iid + '.json'), definition)
    ga.LANG[f'item.{ga.MODID}.{iid}'] = name
    ga.MANIFEST.append(('item', iid))


def berserker_sword(ga):
    """Epee du Berserker : modele 3D epee_niv_20 (libre) ; remplace l'ancienne hache."""
    art = os.path.join(os.path.dirname(__file__), '..', 'art', 'epees_blockbench', 'minecraft', 'assets', ga.MODID)
    import shutil
    shutil.copyfile(os.path.join(art, 'models', 'item', 'epee_niv_20.json'), ga.path(ga.ASSETS, 'models', 'item', 'epee_niv_20.json'))
    shutil.copyfile(os.path.join(art, 'textures', 'item', 'epee_niv_20.png'), ga.path(ga.ASSETS, 'textures', 'item', 'epee_niv_20.png'))
    ga.write_json(os.path.join(ga.ASSETS, 'items', 'epee_berserker.json'),
                  {'model': {'type': 'minecraft:model', 'model': f'{ga.MODID}:item/epee_niv_20'}})
    ga.LANG[f'item.{ga.MODID}.epee_berserker'] = "Épée du Berserker"


def generate(ga):
    for iid, name, kind, base, acc, glow in MATERIALS:
        ga.simple_item(iid, name, ga.gi.template_sprite(kind, base, acc, None, glow=glow))
    berserker_sword(ga)                                     # copie aussi epee_niv_20, base d'une variante
    for row in WEAPONS:
        weapon_variant(ga, *row)
    saved = ga.ARMOR_SETS
    ga.ARMOR_SETS = ARMOR_SETS
    ga.armors()
    ga.ARMOR_SETS = saved
    armor_dir = os.path.join(ga.ASSETS, 'textures', 'entity', 'armor')
    for sid, (panoply, hue, sat, val) in ARMOR_TEXTURES.items():
        recolor(Image.open(os.path.join(armor_dir, panoply + '.png')), hue, sat, val).save(os.path.join(armor_dir, sid + '.png'))
