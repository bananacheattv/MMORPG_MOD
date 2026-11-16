"""Quetes de recolte / livraison : remplace les objets du lore (sans objet reel dans le mod) par de vrais objets du mod,
comptes par un objectif « collect » (objets rendus au PNJ donneur). Appele par build_lore_quests.py avant l'ecriture du JSON."""
import json, os, re

ROOT = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
LANG = json.load(open(os.path.join(ROOT, 'src/main/resources/assets/mmorpg/lang/fr_fr.json'), encoding='utf-8'))

def name(item): return LANG['item.mmorpg.' + item]

# region -> (objet principal, objet secondaire) : des objets reels, a la portee d'un joueur du niveau de la region
REGION = {
    'Valdhéris et les Contreforts': ('ecorce_ancienne', 'ferraille_gobeline'),
    'Bois de Murmurelle': ('racine_lueur', 'croc_loup'),
    'Marais de Fangeterre': ('goujon_des_marais', 'mucus_acide'),
    'Mines de Creuse-Braise': ('lingot_verdite', 'poussiere_ame'),
    'Marches du Rempart': ('sang_orc', 'acier_orc'),
    'Plaines de Cendrelune': ('crin_cendre', 'carapace_cuivre'),
    'Ruines de Haute-Cadette': ('poussiere_ame', 'fragment_ossuaire'),
    'Désert de Verre': ('dard_scorpion', 'bandelette_ancienne'),
    'Vallée de Refuge-Bas': ('baie_ambree', 'ble_argent'),
    'Terres du Fort de la Brisure': ('lingot_ambrasite', 'acier_orc'),
    "Pics d'Hivernoir": ('fourrure_polaire', 'carapace_givree'),
    'Forêt Pétrifiée': ('defense_ronce', 'seve_lumineuse'),
    'Abysses de Sombrelac': ('poisson_lanterne', 'anchois_des_abysses'),
    'Faille des Murmures': ('eclat_astral', 'poussiere_etoile'),
    'Marches Renégates': ('poussiere_etoile', 'eclat_astral'),
    'Lisières du Cratère': ('poudre_astrale', 'griffe_vide'),
    'Forge de Malvorn': ('lingot_neantite', 'lingot_glacium'),
    "Nécropole d'Aelis": ('griffe_vide', 'pupille_chaos'),
    'Entre-Monde': ('pupille_chaos', 'eclat_fracture'),
    'Cœur de la Brisure': ('orchidee_neant', 'eclat_fracture'),
}
# ou trouver chaque objet (texte affiche au joueur)
SRC = {
    'ecorce_ancienne': "butin des monstres de forêt et de plaines", 'ferraille_gobeline': "butin des Gobelins des forêts et plaines",
    'racine_lueur': "se cultive avec une graine de Lueur plantée à la houe (métier Fermier)", 'croc_loup': "butin des Loups Sombres, la nuit",
    'goujon_des_marais': "se pêche à la canne dans les marais (métier Pêcheur)", 'mucus_acide': "butin des monstres des marais",
    'lingot_verdite': "s'obtient en minant les veines de Verdite des Mines d'Aeloria (métier Mineur)", 'poussiere_ame': "butin des morts-vivants",
    'sang_orc': "butin des Orcs Guerriers", 'acier_orc': "butin des Orcs Guerriers", 'crin_cendre': "butin des Hyènes cendrées du désert et de la savane",
    'carapace_cuivre': "butin des monstres du désert", 'fragment_ossuaire': "butin des monstres des cryptes",
    'dard_scorpion': "butin des Scorpions de cuivre du désert", 'bandelette_ancienne': "butin des monstres du désert",
    'baie_ambree': "se cultive avec une graine Ambrée (Fermier niveau 38)", 'ble_argent': "se cultive avec une graine d'Argent (Fermier niveau 18)",
    'lingot_ambrasite': "s'obtient en minant les veines d'Ambrasite des Mines d'Aeloria", 'fourrure_polaire': "butin des bêtes des terres gelées",
    'carapace_givree': "butin des bêtes des terres gelées", 'defense_ronce': "butin des Sangliers ronce des forêts",
    'seve_lumineuse': "butin des monstres de forêt", 'poisson_lanterne': "se pêche en mer et dans les abysses, la nuit",
    'anchois_des_abysses': "se pêche dans les abysses", 'eclat_astral': "butin des créatures du vide (îles du large, océans profonds, End)",
    'poussiere_etoile': "butin des créatures du vide (îles du large, océans profonds, End)", 'poudre_astrale': "butin des Mites astrales du vide",
    'griffe_vide': "butin des Rôdeurs du vide", 'pupille_chaos': "butin des Yeux du chaos du vide", 'eclat_fracture': "butin des êtres du vide",
    'lingot_neantite': "s'obtient en minant les veines de Néantite des Mines d'Aeloria", 'lingot_glacium': "s'obtient en minant les veines de Glacium des Mines d'Aeloria",
    'lingot_argentite': "s'obtient en minant les veines d'Argentite des Mines d'Aeloria", 'lingot_mithril': "se façonne à la Forge Arcanique (Lingot d'Argentite + Acier d'orc)",
    'orchidee_neant': "se cultive avec une graine du Néant (Fermier niveau 75)",
    'soupe_de_racine': "se prépare à la Forge (12 Racines de Lueur + un bol)", 'filet_grille': "se prépare à la Forge (Gardon argenté + Maquereau bleu)",
    'ragout_des_profondeurs': "se prépare à la Forge (poissons de pêche + Algue lumineuse)", 'tourte_aux_baies': "se prépare à la Forge (Baies ambrées + Blé d'argent)",
    'banquet_des_moissons': "se prépare à la Forge (Orchidées du Néant, Fleurs de givre, Baies ambrées)",
}
TO = 'au PNJ qui vous a confié la quête'
CAP = 20     # un objet de butin rare : pas plus de 20 a rapporter

def collect(q, item, n, verb, to):
    n = min(n, CAP)
    src = SRC[item].replace(' (', ', ').replace(')', '')
    q['objs'] = [['collect', item, n]]
    q['obj'] = f"{verb} {n} {name(item)} ({src}), puis les rapporter {to}."
    q['ask'] = f"Rapporte-moi {n} {name(item)} : {src}."

def apply(out):
    fixed = []
    for q in out:
        if q['objs'] or q['kind'] == 'evenement': continue
        main = q['kind'] == 'principale'
        to = "à Flammèche" if main else TO
        main_item, alt = REGION.get(q.get('region', ''), REGION['Valdhéris et les Contreforts'])
        t, txt, qid = q['type'], q['obj'], q['id']
        n = int(m.group(1)) if (m := re.search(r'(\d+)', txt)) else 0
        if t == 'Livraison' and txt.startswith('Récolter et livrer'):
            collect(q, main_item, n, 'Récolter', to)
        elif t == 'Collecte' and txt.startswith('Retrouver'):
            collect(q, alt, n, 'Récolter', to)
        elif t == 'Collecte' and 'Éclats de Brisure' in txt:
            collect(q, alt, n, 'Récolter', to)
        elif t == 'Récolte métier' and (txt.startswith('Récolter') and qid[:2] in ('qp', 'qs')):
            collect(q, main_item, n, 'Récolter', to)
        else:
            continue
        fixed.append(qid)
    # plats cuisines livres a l'auberge
    dishes = {5: 'soupe_de_racine', 26: 'ragout_des_profondeurs', 51: 'tourte_aux_baies', 76: 'banquet_des_moissons'}
    for q in out:
        if q['type'] == 'Livraison' and q['obj'].startswith('Livrer 5 plats'):
            collect(q, dishes[q['level']], 3 if q['level'] >= 76 else 5, 'Préparer', TO)
            q['obj'] = q['obj'].replace(', puis les rapporter', ' et les rapporter', 1)
            fixed.append(q['id'])
    # lingots a rapporter a la Grand-Forge (Brokkar)
    ingots = {'qs_009': ('lingot_verdite', 5), 'qs_010': ('lingot_verdite', 7), 'qs_011': ('lingot_argentite', 9), 'qs_012': ('lingot_argentite', 11),
              'qs_013': ('lingot_ambrasite', 13), 'qs_014': ('lingot_mithril', 15), 'qs_015': ('lingot_glacium', 17), 'qs_016': ('lingot_neantite', 19),
              'qs_343': ('lingot_argentite', 20), 'qs_348': ('lingot_ambrasite', 20), 'qs_353': ('lingot_glacium', 20), 'qs_358': ('lingot_neantite', 20)}
    for q in out:
        if q['id'] in ingots:
            item, n = ingots[q['id']]
            collect(q, item, n, 'Rassembler', TO)
            fixed.append(q['id'])
    return fixed
