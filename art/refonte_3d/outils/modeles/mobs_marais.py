#!/usr/bin/env python3
"""Marais : crapaud venimeux, limace acide, luciole geante, sorcier des roseaux, hydre des marais. En texels de jeu."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from clips_mob import bipede, quadrupede_mob, serpent, volant
from kit_mob import CF, CH, CR, FR, M, feuille, losange, rond, triangle
from materiaux import stamp

V = ('verrues', 'crapaud')
VB = ('uni', 'crapaud_ventre')
AME = ('cristal', 'amethyste')
JA = {'k': '#120b08', 'a': '#f2c23a', 'b': '#fff08a', 'd': '#7a5a10'}


def crapaud_venimeux():
    """Crapaud venimeux : large, plat, peau grumeleuse turquoise, ventre citron, bouche immense, yeux jaunes en bosse, cristaux violets sur le dos, grandes pattes arriere repliees."""
    m = M('crapaud_venimeux', 211, 2.906, description='Crapaud venimeux Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    p.bone('body', (0, 9, 0), 'root')
    m.b = 'body'
    m.c('ventre', (-12, 3, -11), (12, 11, 12), VB, base=2.8)
    m.c('dos', (-14, 9, -10), (14, 19, 13), V, ton=.2)
    m.c('dos_haut', (-11, 19, -7), (11, 23, 11), V, ton=.5)
    m.c('dos_flanc', (-15, 10, -7), (15, 16, 10), V, ton=-.2)
    m.c('croupe', (-11, 6, 13), (11, 17, 16), V, ton=-.3)
    # cristaux violets, lumineux, en grappe sur le dos
    for i, (x, z, h, r, rx, rz) in enumerate(((0, 2, 14, 3, 6, 0), (-6, -1, 10, 2, -8, 14), (6, -2, 11, 2, -10, -14), (-3, 7, 9, 2, 16, 12), (5, 7, 10, 2, 14, -12), (0, -5, 8, 2, -18, 4), (-9, 5, 7, 1, 6, 22), (9, 4, 6, 1, 6, -24))):
        m.pointe('body', 'cristal_lueur', x, 21, z, h, r, AME, inc=(rx, rz))
    # tete : crane plat, levre sombre, bosses des yeux
    p.bone('head', (0, 14, -9), 'body')
    m.b = 'head'
    m.c('crane', (-13, 13, -21), (13, 22, -8), V, ton=.5)
    m.c('museau', (-10, 13, -24), (10, 19, -21), V, ton=.3)
    m.c('levre', (-13, 12, -22), (13, 14, -8), ('uni', 'crapaud'), base=1)
    m.s('bosse', (7, 21, -18), (13, 26, -11), V, ton=.7)
    m.lueur_sym('head', 'oeil', (8, 23, -19), (12, 27, -15), ('uni', 'ambre'), base=3.6)
    p.decal('oeil_lueur_g', 'north', stamp(['aaaa', 'kkkk', 'kkkk', 'aaaa'], JA, 12, 27))
    p.decal('oeil_lueur_g', 'east', stamp(['abba', 'akka', 'akka', 'aaaa'], JA, -15, 27))
    p.bone('machoire', (0, 12, -9), 'head')
    m.b = 'machoire'
    m.c('mandibule', (-12, 7, -22), (12, 12, -8), VB, base=3)
    m.c('langue', (-3, 12, -19), (3, 13, -10), ('uni', 'venin'), base=3, no_ao=True, hide={'down'})
    m.c('goitre', (-8, 3, -18), (8, 7, -9), VB, base=3.6)
    # pattes avant fines, doigts ecartes
    p.bone('leg_fl', (12, 11, -7), 'root')
    p.bone('leg_fr', (-12, 11, -7), 'root')
    p.bone('leg_fl_bas', (15, 6, -8), 'leg_fl')
    p.bone('leg_fr_bas', (-15, 6, -8), 'leg_fr')
    m.sb('leg_fl|leg_fr', 'bras', (11, 5, -10), (17, 12, -5), V, ton=-.1)
    m.sb('leg_fl_bas|leg_fr_bas', 'avantbras', (13, 1, -11), (17, 6, -7), V)
    m.sb('leg_fl_bas|leg_fr_bas', 'doigt', (13, 0, -17), (15, 1, -11), VB, base=3.2, rot=(0, 18, 0), pivot=(14, 0, -11))
    m.sb('leg_fl_bas|leg_fr_bas', 'doigt', (14, 0, -17), (16, 1, -11), VB, base=3.2)
    m.sb('leg_fl_bas|leg_fr_bas', 'doigt', (15, 0, -17), (17, 1, -11), VB, base=3.2, rot=(0, -18, 0), pivot=(16, 0, -11))
    # pattes arriere : grande cuisse repliee vers l'avant, jarret, pied long
    p.bone('leg_bl', (14, 12, 7), 'root')
    p.bone('leg_br', (-14, 12, 7), 'root')
    p.bone('leg_bl_bas', (19, 5, 1), 'leg_bl')
    p.bone('leg_br_bas', (-19, 5, 1), 'leg_br')
    m.sb('leg_bl|leg_br', 'cuisse', (13, 4, -2), (21, 16, 13), V, ton=.3)
    m.sb('leg_bl_bas|leg_br_bas', 'jarret', (15, 1, -1), (21, 6, 8), V, ton=-.2)
    m.sb('leg_bl_bas|leg_br_bas', 'pied', (14, 0, -8), (21, 2, -1), VB, base=3)
    m.sb('leg_bl_bas|leg_br_bas', 'orteil', (14, 0, -13), (16, 1, -8), VB, base=3.4, rot=(0, 14, 0), pivot=(15, 0, -8))
    m.sb('leg_bl_bas|leg_br_bas', 'orteil', (19, 0, -13), (21, 1, -8), VB, base=3.4, rot=(0, -14, 0), pivot=(20, 0, -8))
    legs = [('leg_fl', 'leg_fl_bas', 0.0), ('leg_fr', 'leg_fr_bas', .5), ('leg_bl', 'leg_bl_bas', .5), ('leg_br', 'leg_br_bas', 0.0)]
    quadrupede_mob(p, legs, jaw='machoire', tail='machoire', amp=18, knee=26, leg_len=8, poids=2.2, gr=1.2)
    return p


def limace_acide():
    """Limace acide : chair citron luisante, tete relevee, deux yeux sur tiges, coquille spiralee turquoise et violette, trainee d'acide."""
    m = M('limace_acide', 212, 3.073, description='Limace acide Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    G = ('gluant', 'limace')
    # chaine du corps, du cou a la queue : pieds a plat, le corps ondule
    p.bone('body', (0, 0, -6), 'root')
    p.bone('seg2', (0, 0, 4), 'body')
    p.bone('seg3', (0, 0, 13), 'seg2')
    p.bone('head', (0, 8, -13), 'body')
    m.b = 'body'
    m.c('corps_a', (-9, 0, -14), (9, 10, 5), G, ton=.3)
    m.c('sole_a', (-11, 0, -16), (11, 2, 5), G, ton=-.4)
    m.b = 'seg2'
    m.c('corps_b', (-8, 0, 4), (8, 8, 14), G, ton=.1)
    m.c('sole_b', (-10, 0, 5), (10, 2, 14), G, ton=-.5)
    m.b = 'seg3'
    m.c('corps_c', (-6, 0, 13), (6, 5, 19), G, ton=-.1)
    m.c('queue', (-4, 0, 19), (4, 3, 24), G, ton=-.3)
    m.c('sole_c', (-8, 0, 14), (8, 1, 20), G, ton=-.5)
    # tete : cou dresse, bouche, tiges d'yeux
    m.b = 'head'
    m.c('cou', (-7, 7, -18), (7, 16, -7), G, ton=.4)
    m.c('nez', (-5, 7, -21), (5, 13, -18), G, ton=.2)
    m.c('bouche', (-3, 7, -22), (3, 9, -21), ('uni', 'soie_noire'), base=.8, no_ao=True, hide={'south'})
    m.s('corne_bouche', (5, 6, -21), (7, 9, -17), G, ton=.5)
    p.bone('tige_g', (3, 15, -13), 'head')
    p.bone('tige_d', (-3, 15, -13), 'head')
    m.sb('tige_g|tige_d', 'tige', (2, 14, -14), (4, 25, -12), G, ton=.5, rot=(-8, 0, -10), pivot=(3, 15, -13))
    m.sb('tige_g|tige_d', 'oeil_lueur', (2, 24, -17), (7, 29, -12), ('uni', 'limace'), base=4.2, no_ao=True)
    p.decal('oeil_lueur_g', 'north', stamp(['.....', '.kk..', '.kk..', '.....', '.....'], JA, 7, 29))
    # coquille : spirale en anneaux, turquoise et violette
    p.bone('coquille', (0, 9, 4), 'body')
    m.b = 'coquille'
    T = ('coquille', 'turquoise')
    m.c('coq_socle', (-7, 8, -5), (7, 11, 13), ('uni', 'pourpre'), base=2.2)
    m.c('coq_a', (-7, 11, -3), (7, 25, 12), T, base=3)
    m.c('coq_av', (-6, 12, -5), (6, 23, -3), T, base=2.6)
    m.c('coq_ar', (-6, 12, 12), (6, 23, 14), T, base=2.4)
    m.c('coq_h', (-6, 25, -1), (6, 27, 10), T, base=3.4)
    m.s('coq_flanc', (7, 13, 0), (8, 23, 9), ('coquille', 'pourpre'), base=3.2, tours=1.4)
    m.s('coq_centre', (8, 16, 2), (9, 20, 6), ('uni', 'pourpre'), base=4.4)
    # gouttes d'acide
    p.bone('gouttes', (0, 3, -6), 'body')
    m.lueur_sym('gouttes', 'goutte', (9, 2, -9), (11, 6, -7), ('gluant', 'limace'), base=4.4)
    m.lueur('gouttes', 'goutte', (5, 6, -20), (7, 7, -19), ('uni', 'limace'), base=4.6) if False else None
    serpent(p, ['body', 'seg2', 'seg3'], head='head', nom_move='slither', oscille=7, oscille_idle=1.5, leve=('head',), retard=.14)
    # les tiges s'agitent
    for a in p.anims:
        if a['name'] in ('idle', 'slither'):
            from animations import rot, sin
            T_ = a['length']
            a['tracks']['tige_g'] = {'rotation': rot(T_, x=sin(5, 1, .1), z=sin(4, 1, .3))}
            a['tracks']['tige_d'] = {'rotation': rot(T_, x=sin(5, 1, .6), z=sin(4, 1, .8))}
    return p


def luciole_geante():
    """Luciole geante : petit corps cuirasse presque noir, grandes ailes pales nervurees, abdomen d'ambre lumineux, yeux d'ambre, pattes qui pendent."""
    m = M('luciole_geante', 213, 2.873, description='Luciole geante Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    C = ('segments', 'nuit')
    N = ('plaque', 'nuit')
    p.bone('body', (0, 26, 0), 'root')
    m.b = 'body'
    m.c('thorax', (-6, 20, -6), (6, 31, 6), N, base=3)
    m.c('thorax_haut', (-5, 31, -5), (5, 33, 7), C, base=3, ton=.5)
    m.s('thorax_flanc', (6, 22, -4), (7, 29, 4), C, base=3)
    # abdomen d'ambre, annele
    p.bone('abdomen', (0, 26, 6), 'body')
    m.b = 'abdomen'
    m.c('abdo_a', (-5, 21, 6), (5, 30, 12), ('segments', 'nuit'), pas=3, base=3)
    m.lueur('abdomen', 'abdo_b', (-5, 20, 12), (5, 29, 19), ('segments', 'ambre'), pas=3, base=3.4)
    m.lueur('abdomen', 'abdo_c', (-4, 21, 19), (4, 28, 23), ('segments', 'ambre'), pas=2, base=4.2)
    # tete : yeux d'ambre, antennes, mandibules
    p.bone('head', (0, 26, -6), 'body')
    m.b = 'head'
    m.c('crane', (-5, 22, -13), (5, 30, -6), N, base=3.2)
    m.yeux('head', 3, 24, -13, 3, 4, ramp='ambre', base=3.4)
    p.decal('oeil_lueur_g', 'north', stamp(['bba', 'baa', 'aaa', 'aaa'], JA, 6, 28))
    m.s('oeil_cote', (5, 24, -12), (6, 28, -8), ('uni', 'ambre'), base=3, no_ao=True)
    m.s('mandibule', (2, 21, -16), (4, 23, -12), ('griffe', 'ivoire'), base=1.8)
    m.s('antenne', (2, 30, -12), (3, 42, -11), ('uni', 'nuit'), base=3, rot=(-34, 0, -22), pivot=(2, 30, -11))
    m.s('antenne_b', (9, 37, -22), (10, 39, -14), ('uni', 'nuit'), base=4, rot=(0, -20, 0), pivot=(9, 38, -15)) if False else None
    # ailes : avant et arriere, membrane decoupee en goutte, nervures peintes
    p.bone('aile_av_g', (5, 32, -2), 'body')
    p.bone('aile_av_d', (-5, 32, -2), 'body')
    p.bone('aile_ar_g', (5, 31, 3), 'body')
    p.bone('aile_ar_d', (-5, 31, 3), 'body')
    AV = ['..#####.............', '.##########.........', '##############......', '##################..', '####################', '####################', '.##################.', '..###############...', '....###########.....', '.......#####........']
    AR = ['.###........', '#######.....', '##########..', '############', '###########.', '.########...', '...#####....']
    AI = dict(hide={'north', 'south', 'east', 'west'}, no_shadow=True)
    m.sb('aile_av_g|aile_av_d', 'aile_avant', (5, 32, -13), (40, 33, 3), ('aile_insecte', 'chair_pale'), masque=AV, rot=(0, 0, 8), pivot=(5, 32, -2), **AI)
    m.sb('aile_ar_g|aile_ar_d', 'aile_arriere', (5, 31, 2), (31, 32, 15), ('aile_insecte', 'chair_pale'), masque=AR, base=3.4, rot=(0, -14, 6), pivot=(5, 31, 3), **AI)
    # six pattes qui pendent
    pattes = []
    for i, (z, r) in enumerate(((-4, 8), (0, 0), (4, -8))):
        g, d = f'patte{i}_g', f'patte{i}_d'
        p.bone(g, (6, 21, z), 'body')
        p.bone(d, (-6, 21, z), 'body')
        pattes += [g, d]
        m.sb(f'{g}|{d}', f'femur{i}', (5, 15, z - 1), (7, 22, z + 1), N, base=3, bord=0, rot=(r, 0, -22), pivot=(6, 22, z))
        m.sb(f'{g}|{d}', f'tibia{i}', (8, 9, z - 1), (10, 16, z + 1), ('griffe', 'nuit'), base=2.4, rot=(r, 0, 6), pivot=(9, 16, z))
    volant(p, [('aile_av_g', 'aile_av_d', 0.0), ('aile_ar_g', 'aile_ar_d', .12)], queue=('abdomen',), pattes=pattes, battement=30, cadence=4, hover=1.4, battement_idle=26)
    return p


def sorcier_roseaux():
    """Sorcier des roseaux : grand chapeau de jonc tresse, visage d'ombre et yeux verts, robe de roseaux effilochee, baton noueux couronne d'une lanterne de marais."""
    m = M('sorcier_roseaux', 214, 2.274, description='Sorcier des roseaux Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    m.humanoide(hanche=25, epaule=44, cou=46, x_bras=9, x_jambe=4, coude=33, genou=12)
    RO = ('peau_tissu', 'marais')
    RJ = ('rayures', 'roseau')
    # jambes sous la robe, pieds de racine
    m.sb('leg_-1|leg_1', 'cuisse', (2, 12, -3), (7, 26, 3), RO, base=2)
    m.sb('tibia_-1|tibia_1', 'tibia', (2, 3, -3), (7, 13, 3), ('ecorce', 'bois_sombre'), ton=1)
    m.sb('tibia_-1|tibia_1', 'pied', (1, 0, -6), (8, 3, 4), ('ecorce', 'bois_sombre'), ton=.8)
    # robe : corps fin, jupe de roseaux en lanieres
    m.b = 'body'
    m.c('buste', (-7, 33, -5), (7, 46, 5), RO)
    m.c('ceinture', (-8, 31, -6), (8, 34, 6), ('corde', 'corde'), base=2)
    m.c('bassin', (-8, 22, -6), (8, 32, 6), RO, ton=.4)
    m.c('jupe_av', (-9, 7, -7), (9, 31, -6), FR('roseau'), base=2.6, mini=.5, **CF)
    m.c('jupe_ar', (-9, 5, 6), (9, 31, 7), FR('roseau'), base=2.2, mini=.55, **CF)
    m.s('jupe_flanc', (8, 6, -6), (9, 31, 6), FR('roseau'), base=2.4, mini=.5, **CR)
    m.c('tablier', (-5, 20, -8), (5, 31, -7), ('peau_tissu', 'tissu_vert'), base=2.4)
    m.c('medaillon_lueur', (-2, 37, -7), (2, 41, -5), ('cristal', 'turquoise'), base=3.4, no_ao=True)
    m.c('col_roseaux', (-9, 40, -6), (9, 47, 7), FR('roseau'), base=2.8, mini=.4, ajoure=True, hide={'east', 'west', 'up', 'down', 'north'}, no_shadow=True) if False else None
    m.s('epaule_ros', (7, 40, -6), (8, 47, 7), FR('roseau'), base=3, mini=.45, **CR) if False else None
    # tete : visage d'ombre, yeux, barbe de roseaux
    m.b = 'head'
    m.c('capuche', (-6, 46, -6), (6, 56, 6), RO, ton=.6)
    m.c('ombre', (-4, 47, -7), (4, 54, -6), ('uni', 'nuit'), base=.5, no_ao=True, hide={'south'})
    m.yeux('head', 1, 50, -8, 2, 2, ramp='rune', base=3.6)
    m.c('barbe', (-5, 37, -8), (5, 48, -7), FR('roseau'), base=2.6, mini=.35, **CF)
    # chapeau : large bord de jonc, cone a gradins, pointe cassee vers l'arriere, bande et plume de roseau
    p.bone('chapeau', (0, 55, 0), 'head')
    m.b = 'chapeau'
    TR = ('rayures', 'roseau')
    m.c('bord', (-14, 55, -14), (14, 57, 14), ('cordage', 'roseau'), base=2.6, pas=3)
    m.c('bord_dessus', (-11, 57, -11), (11, 58, 11), ('cordage', 'roseau'), base=3, pas=3)
    m.c('calotte', (-8, 58, -8), (8, 63, 8), ('cordage', 'roseau'), base=3.2, pas=4)
    m.c('bande', (-9, 58, -9), (9, 60, 9), ('uni', 'tissu_vert'), base=2.6)
    m.c('calotte2', (-6, 63, -5), (6, 67, 6), ('cordage', 'roseau'), base=3.4, pas=4)
    m.c('pointe', (-4, 67, -2), (4, 71, 7), ('cordage', 'roseau'), base=3.6, pas=4, rot=(-16, 0, 0), pivot=(0, 67, 2))
    m.c('pointe2', (-2, 69, 7), (2, 72, 13), ('cordage', 'roseau'), base=3.2, pas=3, rot=(-30, 0, 0), pivot=(0, 69, 6))
    m.touffe('chapeau', 'roseau', 7, 58, -5, 8, 2, 'roseau', 3.4)
    m.s('frange_bord', (14, 49, -13), (15, 56, 13), FR('roseau'), base=2.8, mini=.35, **CR)
    m.c('frange_bord', (-13, 50, -15), (-5, 56, -14), FR('roseau'), base=2.6, mini=.4, **CF)
    m.c('frange_bord', (6, 50, -15), (13, 56, -14), FR('roseau'), base=2.6, mini=.4, **CF)
    # bras dans des manches larges, main gauche griffue
    m.sb('arm_-1|arm_1', 'manche', (7, 34, -4), (13, 46, 4), RO, ton=.3)
    m.sb('avbras_-1|avbras_1', 'manche_bas', (7, 24, -5), (14, 34, 5), RO, ton=.1)
    m.sb('avbras_-1|avbras_1', 'ourlet', (6, 19, -5), (15, 26, 5), FR('roseau'), base=2.2, mini=.55, hide={'north', 'south', 'up', 'down', 'east'}, no_shadow=True)
    m.sb('avbras_-1|avbras_1', 'main', (8, 20, -3), (12, 24, 2), ('uni', 'marais'), base=4)
    # baton noueux dans la main droite (+x), lanterne de marais
    p.bone('baton', (10, 22, -7), 'avbras_-1')
    B = ('ecorce', 'bois_sombre')
    m.b = 'baton'
    m.c('baton_bas', (9, 0, -8), (11, 20, -6), B, ton=1, pas=2)
    m.c('baton_haut', (9, 20, -8), (11, 44, -6), B, ton=1.2, pas=2)
    m.c('poing', (8, 20, -9), (12, 24, -5), ('uni', 'marais'), base=4)
    m.c('noeud', (8, 30, -9), (12, 33, -5), B, ton=1.4, pas=2)
    m.c('crosse', (9, 44, -8), (11, 50, -6), B, ton=1.4, pas=2, rot=(-20, 0, 0), pivot=(10, 44, -7))
    m.c('crosse2', (9, 48, -14), (11, 50, -8), B, ton=1.4, pas=2)
    m.lueur('baton', 'lanterne', (8, 41, -16), (12, 47, -12), ('cristal', 'rune'), base=3.2)
    m.c('lanterne_haut', (8, 47, -16), (12, 48, -12), ('uni', 'fer_noir'), base=3)
    bipede(p, style='sort', amp=18, genou=30, bras=8, jambe=26, cape=('chapeau',), arme='baton')
    return p


def hydre_marais():
    """Hydre des marais : corps bas et lourd, trois cous en S, tetes de serpent cornues, machoires, crete de nageoires, ventre clair, longue queue."""
    m = M('hydre_marais', 215, 4.433, description='Hydre des marais Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    p.meta['y_corps'] = (20, 90)
    EC = ('ecailles', 'hydre')
    VE = ('nacre', 'hydre_ventre')
    CR_ = ('pics', 'turquoise')
    p.bone('body', (0, 46, 8), 'root')
    m.b = 'body'
    m.c('poitrail', (-32, 24, -36), (32, 74, -6), EC, l=6, h=6, y=(20, 90))
    m.c('ventre', (-36, 22, -6), (36, 70, 26), EC, l=6, h=6, y=(20, 90))
    m.c('croupe', (-30, 26, 26), (30, 64, 48), EC, l=6, h=6, y=(20, 90))
    m.c('sous_ventre', (-26, 18, -30), (26, 24, 42), VE, h=8, base=3)
    m.c('dessus', (-24, 70, -6), (24, 74, 22), EC, l=6, h=6, ton=.4, y=(20, 90))
    m.c('devant_ventre', (-24, 26, -37), (24, 64, -36), VE, h=8, axe='y', base=3.2)
    m.s('flanc', (36, 30, -4), (38, 62, 22), EC, l=6, h=6, ton=.3, y=(20, 90))
    # crete dorsale de pics
    for i, (z, y, h) in enumerate(((-26, 74, 14), (-14, 74, 16), (0, 74, 18), (14, 72, 15), (28, 64, 13), (40, 62, 11))):
        m.c(f'crete{i}', (-1, y - 2, z - 5), (1, y + h, z + 5), ('plan', 'turquoise'), base=3.4, masque=triangle(5, 8), rot=(-14, 0, 0), pivot=(0, y, z), **CR)
    # queue en trois os
    p.bone('tail', (0, 44, 46), 'body')
    p.bone('tail2', (0, 40, 58), 'tail')
    p.bone('tail3', (0, 36, 72), 'tail2')
    m.cb('tail', 'queue_a', (-18, 28, 46), (18, 56, 60), EC, l=6, h=6, y=(20, 90), ton=-.1)
    m.cb('tail2', 'queue_b', (-12, 26, 58), (12, 46, 74), EC, l=6, h=6, y=(20, 90), ton=-.1)
    m.cb('tail3', 'queue_c', (-7, 26, 72), (7, 38, 84), EC, l=6, h=6, y=(20, 90), ton=-.1)
    m.cb('tail3', 'queue_d', (-3, 26, 84), (3, 33, 94), EC, l=4, h=4, y=(20, 90), ton=-.2)
    m.cb('tail2', 'crete_q', (-1, 46, 60), (1, 54, 74), ('plan', 'turquoise'), base=3.2, masque=triangle(6, 5), **CR)
    m.cb('tail3', 'crete_q2', (-1, 38, 74), (1, 44, 90), ('plan', 'turquoise'), base=3.2, masque=triangle(6, 5), **CR)
    # trois cous : (x, inclinaison z, nom)
    cous = []
    heads = []
    jaws = []
    for nom, xb, zb, yb, rz, H, sc in (('c', 0, -32, 72, 0, .78, 1.0), ('g', 20, -28, 66, -16, .62, .86), ('d', -20, -28, 66, 16, .62, .86)):
        os1, os2, os3, tete, mach = f'cou1{nom}', f'cou2{nom}', f'cou3{nom}', 'head' if nom == 'c' else f'head_{nom}', f'machoire_{nom}'
        s = 1 if xb >= 0 else -1
        w = int(round(10 * sc))
        L1, L2, L3 = int(32 * H), int(30 * H), int(26 * H)
        p.bone(os1, (xb, yb, zb), 'body')
        m.cb(os1, f'cou_a_{nom}', (xb - w, yb - 8, zb - w), (xb + w, yb + L1, zb + w), EC, l=5, h=5, y=(20, 150), rot=(14, 0, rz), pivot=(xb, yb, zb))
        m.cb(os1, f'gorge_a_{nom}', (xb - w + 3, yb - 4, zb - w - 1), (xb + w - 3, yb + L1 - 3, zb - w), VE, h=6, axe='y', base=3.2, rot=(14, 0, rz), pivot=(xb, yb, zb))
        # position du haut de chaque segment
        import math
        ax, az = math.radians(14), math.radians(rz)
        x2 = xb - math.sin(az) * L1
        z2 = zb + math.sin(ax) * L1
        y2 = yb + math.cos(ax) * math.cos(az) * L1
        x2, y2, z2 = round(x2), round(y2), round(z2)
        p.bone(os2, (x2, y2, z2), os1)
        w2 = max(6, w - 2)
        m.cb(os2, f'cou_b_{nom}', (x2 - w2, y2 - 3, z2 - w2), (x2 + w2, y2 + L2, z2 + w2), EC, l=5, h=5, y=(20, 150), ton=.1, rot=(-18, 0, rz * -.5), pivot=(x2, y2, z2))
        m.cb(os2, f'gorge_b_{nom}', (x2 - w2 + 2, y2, z2 - w2 - 1), (x2 + w2 - 2, y2 + L2 - 2, z2 - w2), VE, h=6, axe='y', base=3.4, rot=(-18, 0, rz * -.5), pivot=(x2, y2, z2))
        ax2, az2 = math.radians(-18), math.radians(rz * -.5)
        x3 = round(x2 - math.sin(az2) * L2)
        z3 = round(z2 + math.sin(ax2) * L2)
        y3 = round(y2 + math.cos(ax2) * math.cos(az2) * L2)
        p.bone(os3, (x3, y3, z3), os2)
        w3 = max(5, w2 - 2)
        m.cb(os3, f'cou_c_{nom}', (x3 - w3, y3 - 3, z3 - w3), (x3 + w3, y3 + L3, z3 + w3), EC, l=5, h=5, y=(20, 150), ton=.2, rot=(8, 0, rz * .4), pivot=(x3, y3, z3))
        m.cb(os3, f'crete_c_{nom}', (x3 - 1, y3 - 2, z3 + w3 - 1), (x3 + 1, y3 + L3, z3 + w3 + 7), ('plan', 'turquoise'), base=3.4, masque=[''.join('#' if (i + j) % 5 < 3 else '.' for i in range(5)) for j in range(9)], rot=(8, 0, rz * .4), pivot=(x3, y3, z3), **CR)
        ax3, az3 = math.radians(8), math.radians(rz * .4)
        x4 = round(x3 - math.sin(az3) * L3)
        z4 = round(z3 + math.sin(ax3) * L3)
        y4 = round(y3 + math.cos(ax3) * math.cos(az3) * L3)
        p.bone(tete, (x4, y4, z4), os3)
        # tete de serpent : crane, museau, machoire, cornes, yeux
        hw = int(round(11 * sc))
        ml = int(round(20 * sc))
        hh = int(round(13 * sc))
        m.cb(tete, f'crane_{nom}', (x4 - hw, y4 - 3, z4 - 16), (x4 + hw, y4 + hh, z4 + 6), EC, l=5, h=5, y=(20, 150), ton=.5)
        m.cb(tete, f'museau_{nom}', (x4 - hw + 3, y4 - 2, z4 - 16 - ml), (x4 + hw - 3, y4 + hh - 4, z4 - 16), EC, l=4, h=4, y=(20, 150), ton=.7)
        m.cb(tete, f'levre_{nom}', (x4 - hw + 3, y4 - 3, z4 - 16 - ml), (x4 + hw - 3, y4 - 2, z4 - 16), ('uni', 'venin'), base=2.4)
        for i in range(3):
            xd = x4 + (hw - 4) - i * 3
            m.cb(tete, f'croc_{nom}{i}', (xd, y4 - 6, z4 - 15 - ml), (xd + 2, y4 - 2, z4 - 13 - ml), ('griffe', 'ivoire'), base=4)
            m.cb(tete, f'croc_{nom}_b{i}', (-xd + 2 * x4 - 2, y4 - 6, z4 - 15 - ml), (-xd + 2 * x4, y4 - 2, z4 - 13 - ml), ('griffe', 'ivoire'), base=4)
        # yeux lumineux de chaque cote
        m.lueur(tete, f'oeil_{nom}_a', (x4 + hw, y4 + hh // 2, z4 - 13), (x4 + hw + 1, y4 + hh // 2 + 4, z4 - 7), ('uni', 'ambre'), base=3.8, hide={'west'})
        m.lueur(tete, f'oeil_{nom}_b', (x4 - hw - 1, y4 + hh // 2, z4 - 13), (x4 - hw, y4 + hh // 2 + 4, z4 - 7), ('uni', 'ambre'), base=3.8, hide={'east'})
        for sg in (1, -1):
            a0, a1 = sorted((x4 + sg * (hw - 4), x4 + sg * (hw - 1)))
            m.cb(tete, f'corne_{nom}', (a0, y4 + hh - 2, z4 - 2), (a1, y4 + hh + 11, z4 + 1), ('corne', 'hydre_ventre'), base=3, rot=(-38, 0, -sg * 12), pivot=((a0 + a1) / 2, y4 + hh - 2, z4))
        p.bone(mach, (x4, y4 - 3, z4 - 8), tete)
        m.cb(mach, f'mandibule_{nom}', (x4 - hw + 4, y4 - 9, z4 - 14 - ml), (x4 + hw - 4, y4 - 3, z4 - 4), VE, h=5, base=3)
        cous += [os1, os2, os3]
        heads.append(tete)
        jaws.append(mach)
    # quatre pattes courtes et griffues, ecartees
    legs = []
    for nom, x, z, ph in (('fl', 30, -18, 0.0), ('fr', -30, -18, .5), ('bl', 30, 30, .56), ('br', -30, 30, .06)):
        s = 1 if x > 0 else -1
        p.bone('leg_' + nom, (x, 40, z), 'root')
        p.bone('leg_' + nom + '_bas', (x + s * 8, 18, z), 'leg_' + nom)
        legs.append(('leg_' + nom, 'leg_' + nom + '_bas', ph))
    for av, z in (('f', -18), ('b', 30)):
        m.sb(f'leg_{av}l|leg_{av}r', f'cuisse_{av}', (24, 16, z - 13), (50, 46, z + 13), EC, l=6, h=6, y=(0, 90), ton=.1)
        m.sb(f'leg_{av}l_bas|leg_{av}r_bas', f'tibia_{av}', (30, 6, z - 10), (48, 18, z + 10), EC, l=6, h=6, y=(0, 90), ton=-.1)
        m.sb(f'leg_{av}l_bas|leg_{av}r_bas', f'pied_{av}', (28, 0, z - 16), (50, 6, z + 10), EC, l=6, h=6, y=(0, 90), ton=-.3)
        for i, (xg, r) in enumerate(((28, 16), (36, 0), (44, -16))):
            m.sb(f'leg_{av}l_bas|leg_{av}r_bas', f'griffe_{av}{i}', (xg, 0, z - 24), (xg + 5, 4, z - 15), ('griffe', 'ivoire'), base=2.6, rot=(0, r, 0), pivot=(xg + 2, 2, z - 16))
    from clips_mob import quadrupede_mob as Q
    Q(p, legs, jaw='machoire_c', ears=tuple(j for j in jaws if j != 'machoire_c'), tail='tail', amp=18, knee=30, leg_len=36, poids=3, gr=4, cou=tuple(cous) + tuple(h for h in heads if h != 'head'))
    return p


MARAIS = {'crapaud_venimeux': crapaud_venimeux, 'limace_acide': limace_acide, 'luciole_geante': luciole_geante, 'sorcier_roseaux': sorcier_roseaux, 'hydre_marais': hydre_marais}
