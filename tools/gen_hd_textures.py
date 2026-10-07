"""
Textures HD (64 x 64) des materiaux, consommables, invocations, trefles et oeufs de familiers.

Appele en fin de generate_assets.main() : remplace les sprites 16 x 16 par des icones detaillees rendues par
hd_items / hd_render, et declare les materiaux propres a chaque monstre importe (MOB_MATERIALS).
"""
import json
import os

from hd_items import render_item

# id -> (forme, couleur, accent, lueur, halo)
ITEMS = {
    # --- materiaux de base
    'oreille_gobelin': ('ear', '#6ab040', '#c06a60', None, None),
    'ferraille_gobeline': ('scrap', '#8a7a60', '#6a6458', None, None),
    'croc_loup': ('fang', '#e8e0d0', '#4a3a40', None, None),
    'fourrure_sombre': ('fur', '#4a4258', None, None, None),
    'os_maudit': ('bone', '#d8d0c0', '#a040c0', '#b060ff', None),
    'poussiere_ame': ('dust', '#60e0d0', None, '#a0fff0', None),
    'defense_orc': ('tusk', '#f0e8c8', '#6a8a3a', None, None),
    'acier_orc': ('ingot', '#6a7068', None, None, None),
    'noyau_flamme': ('orb', '#ff6020', '#ffd040', '#ffb040', None),
    'eclat_givre': ('shard', '#80d8ff', None, '#e0ffff', None),
    'cristal_arcanique': ('crystal', '#c060ff', None, '#e0a0ff', None),
    'essence_neant': ('orb', '#40106a', '#e060ff', '#e080ff', '#a040ff'),
    'couronne_roi_gobelin': ('crown', '#e0b030', '#e02040', None, None),
    'phylactere_liche': ('vial', '#60ff90', '#4a3a5a', '#a0ffc0', None),
    'coeur_infernal': ('heart', '#c02010', None, '#ff8040', '#ff6020'),
    'coeur_glace_eternelle': ('heart', '#80e0ff', None, '#ffffff', '#a0e8ff'),
    'fragment_divin': ('fragment', '#fff0a0', None, '#ffffff', '#ffe080'),
    'pierre_amelioration': ('stone', '#7a8090', '#60c0ff', None, None),
    'pierre_amelioration_sup': ('stone', '#4a3a6a', '#ff60ff', '#ff90ff', '#c060ff'),
    'lingot_mithril': ('ingot', '#a0d0e8', None, None, None),
    'lingot_adamantite': ('ingot', '#c03040', None, None, None),
    'tissu_enchante': ('cloth', '#6040a0', '#e0c060', None, None),
    'cuir_renforce': ('leather', '#8a5a30', '#d0d0d0', None, None),
    'piece_or': ('coin', '#f0c030', None, None, None),
    'alliage_celeste': ('ingot', '#e8e0ff', '#ffd860', '#ffffff', '#c0b0ff'),
    # --- invocations
    'sceau_roi_gobelin': ('seal', '#6ab040', '#e0b030', None, None),
    'grimoire_interdit': ('book', '#3a2a4a', '#60ff90', '#60ff90', None),
    'braise_eternelle': ('flame', '#ff5020', '#ffd040', None, '#ff8040'),
    'cristal_glacial': ('crystal', '#a0e8ff', None, '#ffffff', '#a0e8ff'),
    'oeil_neant': ('eye', '#ff40ff', '#3a2050', '#ff80ff', '#a040ff'),
    # --- consommables
    'potion_soin_mineure': ('potion', '#ff6060', None, None, None),
    'potion_soin': ('potion', '#e02020', None, None, None),
    'potion_soin_majeure': ('potion', '#a00010', None, '#ff8080', '#ff4040'),
    'potion_mana_mineure': ('potion', '#60a0ff', None, None, None),
    'potion_mana': ('potion', '#2060e0', None, None, None),
    'potion_mana_majeure': ('potion', '#1030a0', None, '#80a0ff', '#4080ff'),
    'elixir_experience': ('potion', '#80ff40', None, '#e0ffa0', '#a0ff60'),
    'parchemin_teleportation': ('scroll', '#f0e0b0', '#40a0ff', None, None),
    'parchemin_oubli': ('scroll', '#e0d0c0', '#a040a0', None, None),
    'orbe_renaissance': ('orb', '#ffd040', '#ffffff', '#ffffa0', '#ffd040'),
    'coffre_cosmetique': ('chest', '#8a5a2a', '#ffd040', '#ffff80', '#ffd040'),
    'caisse_aventure': ('chest', '#6a4a2a', '#a0a0a8', None, None),
    'cle_aventure': ('key', '#d9a332', '#40c0ff', None, None),
    'charme_experience': ('amulet', '#e0b030', '#80ff40', '#c0ff80', '#a0ff60'),
    'charme_chance': ('amulet', '#c8c8d0', '#40e080', '#a0ffc0', '#40e080'),
    'nourriture_familier': ('meat', '#b04a30', '#e08060', None, None),
    # --- trefles
    'trefle_chance': ('clover3', '#4caa3a', None, None, None),
    'trefle_quatre_feuilles': ('clover4', '#3ec25a', None, '#c8ffb0', None),
    'trefle_dore': ('clover4', '#f0c030', None, '#fff2a0', '#ffd040'),
    'trefle_celeste': ('clover4', '#80d8ff', None, '#e4f8ff', '#80d8ff'),
    'trefle_divin': ('clover4', '#c46cff', None, '#ffd4ff', '#c46cff'),
    # --- materiaux de butin supplementaires et intermediaires
    'soie_araignee': ('cloth', '#e8e8f0', '#b0b0c8', None, None),
    'griffe_loup': ('claw', '#bfb8a8', '#4a4258', None, None),
    'insigne_bandit': ('seal', '#7a5030', '#d0a040', None, None),
    'ichor_cryptes': ('vial', '#70c040', None, None, None),
    'sang_orc': ('vial', '#a01818', None, None, None),
    'cendre_ardente': ('dust', '#ff8030', None, '#ffd060', None),
    'crane_maudit': ('skull', '#e0d8c8', '#c070ff', '#c070ff', None),
    'voile_spectral': ('cloth', '#b0e0ff', '#ffffff', '#e0ffff', None),
    'gemme_brute': ('crystal', '#40d0c0', None, None, None),
    'plaque_neant': ('ingot', '#3a1a5a', '#c060ff', '#d080ff', None),
    'ecorce_ancienne': ('wood', '#6a4a28', '#90b050', None, None),
    'seve_lumineuse': ('vial', '#d0e040', None, '#f0ff90', None),
    'coeur_racine': ('heart', '#4a8a30', None, '#a0ff70', None),
    'mucus_acide': ('slime', '#a0e040', None, None, None),
    'venin_marais': ('vial', '#8040a0', None, None, None),
    'ecaille_hydre': ('scale', '#2a7050', None, '#80ffc0', None),
    'fragment_ossuaire': ('bone', '#c8c0b0', None, None, None),
    'relique_funeraire': ('ring', '#c0a040', '#60ff90', '#a0ffc0', None),
    'couronne_cryptes': ('crown', '#a0a0b0', '#9040d0', None, None),
    'fourrure_polaire': ('fur', '#e8f0f8', None, None, None),
    'carapace_givree': ('shell', '#90c8f0', None, '#ffffff', None),
    'croc_wyrm': ('fang', '#c0f0ff', '#4a6a80', '#ffffff', None),
    'carapace_cuivre': ('shell', '#c07040', None, None, None),
    'bandelette_ancienne': ('cloth', '#d8c090', '#906030', None, None),
    'braise_djinn': ('flame', '#ff5010', '#ffd040', None, None),
    'eclat_astral': ('shard', '#6060e0', None, '#c0c0ff', None),
    'poussiere_etoile': ('dust', '#f0e0a0', None, '#ffffff', '#fff0a0'),
    'eclat_fracture': ('fragment', '#9050e0', None, '#e0a0ff', '#a060ff'),
    'bois_enchante': ('wood', '#8a6030', '#e0f070', None, None),
    'bronze_runique': ('ingot', '#c08040', '#60d0ff', None, None),
    'tissu_spectral': ('cloth', '#7090d0', '#e0f0ff', '#e0ffff', None),
    'cuir_polaire': ('leather', '#d0d8e0', '#8090a0', None, None),
    'acier_stellaire': ('ingot', '#304080', '#a0c0ff', '#a0c0ff', '#6080ff'),
}

# materiaux propres a chaque monstre : id -> (nom, rarete, lore, monstre, chance, forme, couleur, accent, lueur, halo)
MOB_MATERIALS = {
    'mousse_vivante': ("Mousse Vivante", 'COMMUN', "Mousse qui pousse encore sur le dos des Mousseux.", 'mousseux', .45, 'herb', '#5aa040', None, None, None),
    'defense_ronce': ("Défense de Ronce", 'PEU_COMMUN', "Défense couverte d'épines d'un Sanglier ronce.", 'sanglier_ronce', .35, 'tusk', '#e0d0b0', '#4a7a30', None, None),
    'plume_sylvestre': ("Plume Sylvestre", 'PEU_COMMUN', "Plume d'empennage des Archers sylvestres.", 'archer_sylvestre', .35, 'feather', '#5aa860', '#e0d8b0', None, None),
    'chapeau_luisant': ("Chapeau Luisant", 'PEU_COMMUN', "Chapeau phosphorescent d'un Veilleur champignon.", 'veilleur_champignon', .35, 'mushroom', '#60a0e0', '#e8dcc0', '#a0e0ff', None),
    'noeud_racine': ("Nœud de Racine", 'RARE', "Bois noueux arraché au Colosse racine.", 'colosse_racine', .4, 'wood', '#5a3a20', '#a0ff70', None, None),
    'peau_crapaud': ("Peau de Crapaud", 'COMMUN', "Peau visqueuse et résistante des Crapauds venimeux.", 'crapaud_venimeux', .45, 'leather', '#6a8a3a', None, None, None),
    'perle_acide': ("Perle Acide", 'PEU_COMMUN', "Perle corrosive formée dans les Limaces acides.", 'limace_acide', .3, 'pearl', '#c0ff60', None, '#e0ffa0', None),
    'lanterne_luciole': ("Lanterne de Luciole", 'PEU_COMMUN', "Abdomen lumineux d'une Luciole géante.", 'luciole_geante', .35, 'orb', '#ffe060', '#ffffff', '#fff0a0', '#ffe060'),
    'roseau_enchante': ("Roseau Enchanté", 'PEU_COMMUN', "Roseau ensorcelé par les Sorciers des roseaux.", 'sorcier_roseaux', .35, 'herb', '#a0b060', '#6040a0', None, None),
    'croc_hydre': ("Croc d'Hydre", 'RARE', "Croc venimeux de l'Hydre des marais.", 'hydre_marais', .4, 'fang', '#d0e8d0', '#2a7050', None, None),
    'phalange_osselet': ("Phalange d'Osselet", 'COMMUN', "Petits os des Osselets des cryptes.", 'osselet', .5, 'bone', '#d8d0c0', None, None, None),
    'patte_araignee': ("Patte d'Araignée d'Os", 'PEU_COMMUN', "Patte osseuse et acérée.", 'araignee_os', .35, 'claw', '#e0dcd0', '#3a3438', None, None),
    'ecusson_funeraire': ("Écusson Funéraire", 'PEU_COMMUN', "Écusson des Gardes funéraires.", 'garde_funeraire', .35, 'seal', '#707880', '#c0a040', None, None),
    'maillon_spectral': ("Maillon Spectral", 'RARE', "Maillon des chaînes du Spectre enchaîné.", 'spectre_enchaine', .3, 'chain', '#a0c8ff', None, '#e0f0ff', None),
    'joyau_cryptes': ("Joyau des Cryptes", 'EPIQUE', "Le joyau qui ornait le sceptre du Roi des cryptes.", 'roi_cryptes', .35, 'gem', '#8030c0', None, '#c080ff', '#a040ff'),
    'queue_renard': ("Queue de Renard Givré", 'PEU_COMMUN', "Queue soyeuse d'un Renard de givre.", 'renard_givre', .35, 'fur', '#d0e8ff', None, None, None),
    'elytre_polaire': ("Élytre Polaire", 'PEU_COMMUN', "Aile dure d'un Scarabée polaire.", 'scarabee_polaire', .35, 'shell', '#80b0e0', None, '#e0f8ff', None),
    'plume_harpie': ("Plume de Harpie", 'PEU_COMMUN', "Plume glacée d'une Harpie des neiges.", 'harpie_neiges', .4, 'feather', '#e8f0ff', '#8090a0', None, None),
    'noyau_glace': ("Noyau de Glace", 'RARE', "Le cœur gelé d'un Golem d'iceberg.", 'golem_iceberg', .35, 'orb', '#a0e8ff', '#ffffff', '#ffffff', '#a0e8ff'),
    'ecaille_boreale': ("Écaille Boréale", 'EPIQUE', "Écaille irisée du Wyrm boréal.", 'wyrm_boreal', .35, 'scale', '#70b0e0', None, '#c0f0ff', '#80c0ff'),
    'dard_scorpion': ("Dard de Scorpion", 'PEU_COMMUN', "Dard empoisonné d'un Scorpion de cuivre.", 'scorpion_cuivre', .35, 'fang', '#c07040', '#4a2a10', None, None),
    'scarabee_or': ("Scarabée d'Or", 'RARE', "Amulette sacrée trouvée sur les Momies des dunes.", 'momie_dunes', .25, 'shell', '#e0b030', None, '#fff0a0', '#ffd040'),
    'crin_cendre': ("Crin Cendré", 'COMMUN', "Crin gris des Hyènes cendrées.", 'hyene_cendree', .45, 'fur', '#8a8a90', None, None, None),
    'fumee_djinn': ("Fiole de Fumée de Djinn", 'RARE', "Fumée ardente capturée dans une fiole.", 'djinn_braises', .3, 'vial', '#ff8040', None, '#ffd080', '#ff8040'),
    'carapace_volcanique': ("Carapace Volcanique", 'RARE', "Écaille de lave refroidie d'une Tortue volcanique.", 'tortue_volcanique', .35, 'shell', '#5a2a1a', None, '#ff6020', None),
    'poudre_astrale': ("Poudre d'Aile Astrale", 'PEU_COMMUN', "Écailles chatoyantes des Mites astrales.", 'mite_astrale', .4, 'dust', '#c0a0ff', None, '#ffffff', None),
    'griffe_vide': ("Griffe du Vide", 'RARE', "Griffe d'ombre d'un Rôdeur du vide.", 'rodeur_vide', .35, 'claw', '#2a2040', '#7040c0', '#a070ff', None),
    'pupille_chaos': ("Pupille du Chaos", 'EPIQUE', "L'œil arraché à un Œil du chaos.", 'oeil_chaos', .3, 'eye', '#ff3030', '#2a1828', '#ff8080', '#ff4040'),
    'plaque_fracturee': ("Plaque Fracturée", 'EPIQUE', "Fragment d'armure d'un Chevalier fracturé.", 'chevalier_fracture', .35, 'scrap', '#6a5080', '#b080ff', None, None),
    'coeur_etoile': ("Cœur d'Étoile", 'LEGENDAIRE', "Le cœur incandescent d'un Dévoreur d'étoiles.", 'devoreur_etoiles', .3, 'heart', '#ffe0a0', None, '#ffffff', '#fff0a0'),
    'glande_venin': ("Glande à Venin", 'PEU_COMMUN', "Glande gorgée de venin des Araignées venimeuses.", 'araignee_venimeuse', .35, 'vial', '#80e040', None, None, None),
}

# oeufs de familiers : couleur de la coquille et des taches (PETS de generate_assets)
EGGS = {
    'feu_follet': ('#a0f0ff', '#30a0e0', '#c0f8ff'), 'slime': ('#80e060', '#409030', None),
    'chouette': ('#c09060', '#6a4020', None), 'loup_spectral': ('#c0d8ff', '#6080c0', '#e0ecff'),
    'golem': ('#a8a498', '#40ff90', None), 'fee': ('#ffd8f0', '#ff70c0', '#ffe0f8'),
    'phenix': ('#ffd040', '#ff4020', '#fff0a0'), 'dragonnet': ('#9050e0', '#ff60ff', '#e0a0ff'),
}


# panoplies mythiques de classe (tools/generate_extensions.py)
MYTHIC_SETS = [
    ('valkyrie', 'plate', '#eee8d6', '#d7a530', '#ef5350'),
    ('eternel_arcanes', 'robe', '#352060', '#d4c0ff', '#50e8ff'),
    ('sentinelle_astrale', 'leather', '#235a50', '#d4ca7a', '#8affbb'),
    ('egide_divine', 'plate', '#213451', '#f0c75b', '#9bcaff'),
]


def armor_icons(ga):
    """Icones HD de toutes les pieces d'armure (sets de generate_assets, gen_equipment et mythiques)."""
    import gen_equipment
    from hd_armor import armor_icon
    tex = os.path.join(ga.ASSETS, 'textures', 'item')
    sets = [(sid, style, base, trim, gem) for sid, _, style, base, trim, gem in ga.ARMOR_SETS + gen_equipment.ARMOR_SETS] + MYTHIC_SETS
    for i, (sid, style, base, trim, gem) in enumerate(sets):
        halo = gem if sid in [m[0] for m in MYTHIC_SETS] else None
        for piece in ('casque', 'plastron', 'jambieres', 'bottes'):
            armor_icon(piece, style, base, trim, gem, seed=i * 5, halo=halo).save(ga.path(tex, f'{sid}_{piece}.png'))


def generate(ga):
    armor_icons(ga)
    tex = os.path.join(ga.ASSETS, 'textures', 'item')
    models = os.path.join(ga.ASSETS, 'models', 'item')
    entries = dict(ITEMS)
    for iid, row in MOB_MATERIALS.items():
        entries[iid] = row[5:]
        ga.LANG[f'item.{ga.MODID}.{iid}'] = row[0]
    for pid, (base, spots, glow) in EGGS.items():
        entries['oeuf_' + pid] = ('egg', base, spots, glow, glow)
    for i, (iid, (kind, base, acc, glow, halo)) in enumerate(sorted(entries.items())):
        img = render_item(kind, base, acc, glow, seed=i * 7 + 3, halo=halo)
        img.save(ga.path(tex, iid + '.png'))
        # modele simple pointant sur la texture du mod (remplace les textures vanilla provisoires)
        ga.write_json(os.path.join(models, iid + '.json'),
                      {'parent': 'minecraft:item/generated', 'textures': {'layer0': f'{ga.MODID}:item/{iid}'}})
        ga.write_json(os.path.join(ga.ASSETS, 'items', iid + '.json'),
                      {'model': {'type': 'minecraft:model', 'model': f'{ga.MODID}:item/{iid}'}})


def lang_entries():
    return {f'item.mmorpg.{iid}': row[0] for iid, row in MOB_MATERIALS.items()}


if __name__ == '__main__':
    print(json.dumps(lang_entries(), ensure_ascii=False, indent=1))
