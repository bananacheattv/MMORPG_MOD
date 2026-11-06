#!/usr/bin/env python3
"""Terres gelees : renard de givre, scarabee polaire, harpie des neiges, golem d'iceberg, wyrm boreal. En texels de jeu."""
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from clips_mob import bipede, octo_mob, quadrupede_mob, serpent, volant
from kit_mob import CF, CR, FR, M, RE, losange, triangle
from materiaux import stamp

GL = ('glace', 'glace')
GF = ('cristal', 'glace')
FT = ('fourrure', 'toison')
PT = ('poils_longs', 'toison')
FUR = ('fourrure', 'fourrure_givre')
PG = ('poils_longs', 'fourrure_givre')
NE = ('laine', 'toison')
NOIR = ('uni', 'chitine_bleue')
K = {'k': '#0b1020', 'b': '#3ac6d4', 'c': '#dcffff', 'n': '#16263a', 'w': '#f4f9ff'}


def renard_givre():
    """Renard de givre : corps fin, oreilles immenses, museau pointu, enorme queue-panache qui se dresse en S, hérissee d'eclats de glace."""
    m = M('renard_givre', 231, 1.753, description='Renard de givre Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    p.meta['y_corps'] = (0, 18)
    p.bone('body', (0, 12, 0), 'root')
    m.b = 'body'
    m.c('poitrail', (-4, 8, -9), (4, 16, -2), FT, ton=.3)
    m.c('taille', (-3, 9, -2), (3, 15, 5), FT)
    m.c('croupe', (-4, 8, 5), (4, 16, 11), FT, ton=.2)
    m.c('ventre_blanc', (-3, 7, -8), (3, 9, 8), ('uni', 'toison'), base=4)
    m.c('collerette', (-5, 8, -11), (5, 18, -8), PT, ton=.6, y=(2, 26))
    m.s('collerette_c', (5, 8, -11), (6, 17, -4), ('crete', 'fourrure_givre'), base=4, ecart=1.4, bas=True, pas=3, dent=3, **CR)
    for i, (z, h, a) in enumerate(((-6, 5, 14), (-1, 4, 22), (4, 5, 16))):
        m.c(f'eclat{i}', (-1, 15, z - 1), (1, 15 + h, z + 1), GL, ton=.6, rot=(a, 45, 0), pivot=(0, 15, z))
    # tete
    p.bone('head', (0, 18, -9), 'body')
    m.b = 'head'
    m.c('crane', (-4, 15, -15), (4, 22, -9), FT, ton=.8)
    m.c('museau', (-2, 15, -20), (2, 18, -15), ('uni', 'toison'), base=4.2)
    m.c('truffe', (-1, 17, -21), (1, 19, -20), ('uni', 'nuit'), base=1, hide={'south'})
    m.s('joue', (4, 13, -15), (5, 20, -9), ('crete', 'toison'), base=4, ecart=1, bas=True, pas=2, dent=3, **CR)
    m.yeux('head', 1, 18, -15, 2, 2, ramp='ame', base=3.4)
    p.bone('machoire', (0, 15, -11), 'head')
    m.cb('machoire', 'mandibule', (-2, 13, -19), (2, 15, -11), ('uni', 'toison'), base=3)
    p.bone('ear_g', (3, 22, -11), 'head')
    p.bone('ear_d', (-3, 22, -11), 'head')
    OR_ = ['....##', '....##', '...###', '...###', '..####', '..####', '.#####', '.#####', '######', '######']
    m.sb('ear_g|ear_d', 'oreille', (1, 21, -12), (7, 31, -11), ('plan', 'toison'), base=4, masque=OR_, rot=(-6, 0, -8), pivot=(3, 22, -11), **CF)
    m.sb('ear_g|ear_d', 'oreille_int', (3, 22, -13), (5, 27, -12), ('uni', 'fourrure_givre'), base=1.4, rot=(-6, 0, -8), pivot=(3, 22, -11), hide={'south'})
    # queue-panache en trois os
    p.bone('tail', (0, 14, 11), 'body')
    p.bone('tail2', (0, 22, 16), 'tail')
    p.bone('tail3', (0, 34, 17), 'tail2')
    m.cb('tail', 'queue_a', (-3, 10, 10), (3, 17, 18), PT, ton=-.4, y=(2, 46), rot=(-20, 0, 0), pivot=(0, 14, 11))
    m.cb('tail', 'queue_a2', (-4, 15, 14), (4, 24, 21), PT, ton=-.2, y=(2, 46))
    m.cb('tail2', 'queue_b', (-5, 22, 13), (5, 36, 22), PT, ton=.1, y=(2, 46))
    m.cb('tail3', 'queue_c', (-4, 34, 10), (4, 42, 19), ('poils_longs', 'toison'), base=3.6, y=(2, 46))
    m.cb('tail3', 'queue_bout', (-2, 38, 5), (2, 43, 11), ('poils_longs', 'toison'), base=4.2, y=(2, 46))
    for i, (os_, x, y, z, h, rx, rz) in enumerate((('tail2', 5, 26, 18, 7, 14, -40), ('tail2', -5, 26, 18, 7, 14, 40), ('tail2', 5, 32, 15, 6, -8, -30), ('tail2', -5, 32, 15, 6, -8, 30), ('tail3', 4, 37, 15, 5, 0, -30), ('tail3', -4, 37, 15, 5, 0, 30), ('tail2', 0, 30, 22, 8, 50, 0), ('tail3', 0, 40, 18, 7, 38, 0))):
        m.cb(os_, f'eclat_q{i}', (x - 1, y, z - 1), (x + 1, y + h, z + 1), GL, ton=.5, rot=(rx, 45, rz), pivot=(x, y, z))
    # pattes
    legs = []
    for nom, x, z, y, ph in (('fl', 3, -6, 13, 0.0), ('fr', -3, -6, 13, .5), ('bl', 3, 8, 13, .56), ('br', -3, 8, 13, .06)):
        p.bone('leg_' + nom, (x, y, z), 'root')
        p.bone('leg_' + nom + '_bas', (x, 6, z), 'leg_' + nom)
        legs.append(('leg_' + nom, 'leg_' + nom + '_bas', ph))
    B = ('uni', 'chitine_bleue')
    m.sb('leg_fl|leg_fr', 'bras', (2, 6, -8), (5, 14, -4), FT, y=(0, 18), ton=-.3, hide={'up'})
    m.sb('leg_fl_bas|leg_fr_bas', 'canon_av', (2, 2, -7), (4, 7, -5), ('fourrure', 'fourrure_givre'), y=(0, 18), ton=.2)
    m.sb('leg_fl_bas|leg_fr_bas', 'patte_av', (2, 0, -9), (5, 2, -4), GL, ton=.2)
    m.sb('leg_bl|leg_br', 'cuisse', (2, 6, 6), (5, 14, 11), FT, y=(0, 18), ton=-.3, hide={'up'})
    m.sb('leg_bl_bas|leg_br_bas', 'canon_ar', (2, 2, 7), (4, 7, 9), ('fourrure', 'fourrure_givre'), y=(0, 18), ton=.2)
    m.sb('leg_bl_bas|leg_br_bas', 'patte_ar', (2, 0, 5), (5, 2, 10), GL, ton=.2)
    quadrupede_mob(p, legs, jaw='machoire', ears=('ear_g', 'ear_d'), tail='tail', tail_tip='tail3', amp=30, knee=52, leg_len=13, poids=.8, cou=('tail2',))
    return p


def scarabee_polaire():
    """Scarabee polaire : carapace bombee en plaques de glace, dos herisse de grands cristaux, tete sombre et yeux d'ame, six pattes sombres articulees."""
    m = M('scarabee_polaire', 232, 2.598, description='Scarabee polaire Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    P = ('segments', 'chitine_bleue')
    G2 = ('plaque', 'glace')
    p.bone('body', (0, 10, 0), 'root')
    m.b = 'body'
    m.c('ventre', (-12, 5, -11), (12, 10, 12), P, pas=5)
    m.c('elytres', (-14, 9, -11), (14, 16, 13), G2, base=2.2, bord=1)
    m.c('elytres_h', (-11, 16, -9), (11, 21, 11), G2, base=3, bord=1)
    m.c('elytres_hh', (-7, 21, -6), (7, 24, 8), G2, base=3.6, bord=1)
    m.c('fente', (-1, 9, 13), (1, 22, 14), ('uni', 'chitine_bleue'), base=1, hide={'north'})
    m.c('fente_h', (-1, 24, -6), (1, 25, 8), ('uni', 'chitine_bleue'), base=2, hide={'down'}) if False else None
    m.s('bord_elytre', (14, 8, -9), (15, 12, 11), P, pas=4, ton=.6)
    # cristaux du dos
    for i, (x, z, h, r, rx, rz, gl) in enumerate(((0, 1, 16, 4, 0, 0, 1), (-7, -2, 10, 2, -8, 16, 0), (7, -2, 11, 2, -8, -16, 0), (-5, 7, 9, 2, 14, 12, 0), (5, 8, 8, 2, 16, -12, 0), (0, -6, 7, 2, -18, 0, 0))):
        m.pointe('body', 'cristal_lueur' if gl else 'cristal', x, 22 if abs(x) < 3 else 18, z, h, r, GF, inc=(rx, rz), base=1.4)
    # tete et mandibules
    p.bone('head', (0, 10, -11), 'body')
    m.b = 'head'
    m.c('tete', (-8, 6, -19), (8, 14, -10), P, pas=4, ton=.4)
    m.c('front', (-7, 14, -18), (7, 16, -11), G2, base=2.6, bord=1)
    m.yeux('head', 3, 9, -19, 3, 3, ramp='ame', base=3.4)
    p.bone('mand_g', (4, 7, -18), 'head')
    p.bone('mand_d', (-4, 7, -18), 'head')
    m.sb('mand_g|mand_d', 'mandibule', (2, 6, -25), (6, 9, -17), NOIR, base=3.4, rot=(0, -12, 0), pivot=(4, 7, -18))
    m.sb('mand_g|mand_d', 'mandibule_b', (0, 6, -28), (3, 9, -24), GL, ton=.4)
    # six pattes
    import math
    legs = []
    for i, (z, fan) in enumerate(((-8, 32), (1, 4), (9, -30))):
        g, d = f'leg{i}l', f'leg{i}r'
        p.bone(g, (11, 9, z), 'body')
        p.bone(d, (-11, 9, z), 'body')
        L = 13
        xk = round(11 + L * math.cos(math.radians(fan)) * .9)
        zk = round(z - L * math.sin(math.radians(fan)) * .9)
        p.bone(g + '_bas', (xk, 15, zk), g)
        p.bone(d + '_bas', (-xk, 15, zk), d)
        m.sb(f'{g}|{d}', f'hanche{i}', (10, 7, z - 2), (10 + L, 11, z + 2), P, pas=4, axe='x', rot=(0, fan, 24), pivot=(11, 9, z))
        m.sb(f'{g}_bas|{d}_bas', f'tibia{i}', (xk - 1, 3, zk - 2), (xk + 2, 16, zk + 2), NOIR, base=2.6, rot=(0, 0, 14), pivot=(xk, 15, zk))
        m.sb(f'{g}_bas|{d}_bas', f'griffe{i}', (xk + 3, 0, zk - 1), (xk + 6, 4, zk + 1), GL, ton=.4)
        legs += [(g, g + '_bas', 1), (d, d + '_bas', -1)]
    octo_mob(p, legs, head='head', jaw=(('mand_g', 1), ('mand_d', -1)))
    return p


def harpie_neiges():
    """Harpie des neiges : buste de plumes blanches, tete de chouette a bec noir, couronne d'eclats de glace, grandes ailes deployees, serres pendantes."""
    m = M('harpie_neiges', 233, 2.705, description='Harpie des neiges Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    PL = ('ecailles', 'plume_blanche')
    p.bone('body', (0, 36, 0), 'root')
    m.b = 'body'
    m.c('buste', (-7, 30, -6), (7, 48, 6), PL, ton=.4)
    m.c('poitrine', (-5, 32, -7), (5, 46, -6), PL, ton=.9, hide={'south'})
    m.c('ventre', (-6, 24, -5), (6, 31, 5), PL, ton=.1)
    m.c('queue', (-5, 16, 3), (5, 30, 4), RE('plume_blanche'), base=3.2, mini=.5, pas=3, **CF)
    m.c('queue2', (-3, 13, 5), (3, 28, 6), RE('plume_blanche'), base=2.6, mini=.6, pas=3, **CF)
    # tete
    p.bone('head', (0, 48, -2), 'body')
    m.b = 'head'
    m.c('crane', (-7, 47, -9), (7, 59, 4), PL, ton=1)
    m.c('masque', (-6, 49, -10), (6, 57, -9), ('uni', 'toison'), base=4.4, hide={'south'})
    m.c('sourcil', (-7, 55, -11), (7, 57, -9), ('uni', 'plume_blanche'), base=2.2, hide={'south'})
    m.yeux('head', 2, 51, -11, 3, 3, ramp='ame', base=3.2)
    p.decal('oeil_lueur_g', 'north', stamp(['bbb', 'bkb', 'bbb'], K, 5, 54))
    m.c('bec', (-2, 47, -13), (2, 52, -9), ('griffe', 'nuit'), base=2.4)
    m.c('bec_bout', (-1, 45, -14), (1, 49, -12), ('griffe', 'nuit'), base=3.4)
    for i, (x, h, rz) in enumerate(((-6, 6, 14), (-3, 8, 6), (0, 10, 0), (3, 8, -6), (6, 6, -14))):
        m.pointe('head', 'couronne_lueur', x, 58, -1, h, 1, GL, inc=(-8, rz), ton=.3)
    # ailes : epaule + bout, rangs de plumes en plans decoupes face a la camera
    p.bone('aile_g', (7, 46, 0), 'body')
    p.bone('aile_d', (-7, 46, 0), 'body')
    p.bone('aile_g2', (23, 44, 0), 'aile_g')
    p.bone('aile_d2', (-23, 44, 0), 'aile_d')
    m.sb('aile_g|aile_d', 'bras_aile', (6, 40, -3), (25, 48, 3), PL, ton=.6)
    m.sb('aile_g|aile_d', 'couvertures', (6, 30, -2), (25, 41, -1), RE('plume_blanche'), base=4.2, mini=.6, pas=3, **CF)
    m.sb('aile_g|aile_d', 'remiges', (6, 16, 0), (25, 40, 1), RE('plume_blanche'), base=3.2, mini=.6, pas=4, **CF)
    m.sb('aile_g2|aile_d2', 'main_aile', (23, 42, -2), (38, 48, 2), PL, ton=.3)
    m.sb('aile_g2|aile_d2', 'couvertures2', (23, 32, -1), (38, 43, 0), RE('plume_blanche'), base=4, mini=.6, pas=3, **CF)
    m.sb('aile_g2|aile_d2', 'remiges2', (23, 13, 1), (38, 42, 2), RE('fourrure_givre'), base=4.2, mini=.5, pas=3, **CF)
    m.sb('aile_g2|aile_d2', 'bout', (38, 26, 0), (42, 47, 1), ('plan', 'fourrure_givre'), base=4, masque=['#...', '##..', '###.', '####', '####', '####', '###.', '##..', '##..', '#...', '#...'], **CF)
    # pattes : cuisses emplumees, tarse jaune, serres
    pattes = []
    for s, g in ((1, 'jambe_g'), (-1, 'jambe_d')):
        p.bone(g, (s * 4, 26, 0), 'body')
        pattes.append(g)
    m.sb('jambe_g|jambe_d', 'cuisse', (2, 20, -3), (7, 27, 3), PL, ton=.2)
    m.sb('jambe_g|jambe_d', 'tarse', (3, 13, -1), (5, 21, 1), ('uni', 'bec'), base=2.8)
    m.sb('jambe_g|jambe_d', 'pied', (2, 12, -4), (6, 14, 2), ('uni', 'bec'), base=2.2)
    m.sb('jambe_g|jambe_d', 'serre', (2, 10, -6), (3, 13, -4), ('griffe', 'nuit'), base=3)
    m.sb('jambe_g|jambe_d', 'serre', (5, 10, -6), (6, 13, -4), ('griffe', 'nuit'), base=3)
    volant(p, [('aile_g', 'aile_d', 0.0), ('aile_g2', 'aile_d2', .1)], pattes=pattes, queue=(), battement=22, cadence=1, hover=1.6, battement_idle=10, gr=1.2)
    return p


def golem_iceberg():
    """Golem d'iceberg : colosse de blocs de glace, neige sur les epaules, noyau de cristal dans la poitrine, poings en masses, cristaux qui percent."""
    m = M('golem_iceberg', 234, 4.621, description="Golem d'iceberg Aeloria : refonte (16 texels par bloc en jeu).")
    p = m.p
    m.humanoide(hanche=56, epaule=104, cou=114, x_bras=40, x_jambe=17, coude=68, genou=26, z_cou=-2)
    SOMBRE = ('cristal', 'chitine_bleue')
    # jambes
    m.sb('leg_-1|leg_1', 'cuisse', (6, 26, -13), (29, 58, 13), GF, base=2)
    m.sb('leg_-1|leg_1', 'joint_hanche', (9, 54, -10), (26, 60, 10), SOMBRE, base=2.4)
    m.sb('tibia_-1|tibia_1', 'tibia', (7, 10, -12), (28, 26, 12), GF, base=2.3)
    m.sb('tibia_-1|tibia_1', 'genou', (10, 22, -15), (25, 32, -11), GL, base=3)
    m.sb('tibia_-1|tibia_1', 'pied', (4, 0, -19), (31, 11, 15), GF, base=1.7)
    m.sb('tibia_-1|tibia_1', 'orteil', (6, 0, -24), (14, 6, -19), GL, base=2.4)
    m.sb('tibia_-1|tibia_1', 'orteil', (20, 0, -24), (29, 6, -19), GL, base=2.4)
    # tronc : bassin, ventre plus etroit, enorme poitrail
    m.b = 'body'
    m.c('bassin', (-26, 52, -15), (26, 70, 15), GF, base=2)
    m.c('ventre', (-22, 70, -14), (22, 84, 14), SOMBRE, base=2.6)
    m.c('poitrail', (-34, 84, -18), (34, 118, 18), GF, base=2.4)
    m.c('poitrail_avant', (-26, 88, -21), (26, 114, -18), GF, base=2.8, hide={'south'})
    m.c('cadre_noyau', (-12, 92, -23), (12, 110, -21), SOMBRE, base=3, hide={'south'})
    m.lueur('body', 'noyau', (-8, 94, -26), (8, 108, -21), ('cristal', 'rune'), base=2.8)
    m.lueur('body', 'noyau_c', (-4, 97, -27), (4, 105, -26), ('uni', 'rune'), base=4.6, hide={'south'})
    m.c('neige_dos', (-30, 118, -12), (30, 122, 18), NE, base=4)
    for i, (x, z, h, r, rx, rz) in enumerate(((-22, 12, 30, 6, 18, 14), (22, 12, 26, 5, 18, -14), (0, 16, 34, 7, 24, 0), (-10, 18, 20, 4, 32, 8), (12, 18, 22, 4, 30, -8))):
        m.pointe('body', 'cristal_dos', x, 112, z, h, r, GF, inc=(rx, rz), base=1.3)
    # tete encastree, front bas, yeux d'ame
    m.b = 'head'
    m.c('crane', (-16, 110, -22), (16, 136, 6), GF, base=2.6)
    m.c('front', (-17, 128, -25), (17, 136, 2), GL, base=2.6)
    m.c('machoire_bloc', (-13, 106, -24), (13, 116, -4), SOMBRE, base=2.8)
    m.yeux('head', 4, 120, -23, 8, 5, ramp='ame', base=3.4)
    for i, (x, z, h, r, rx, rz) in enumerate(((-9, -12, 18, 4, -8, 10), (8, -10, 16, 4, -8, -10), (0, -4, 22, 5, 8, 0))):
        m.pointe('head', 'cristal_tete', x, 134, z, h, r, GF, inc=(rx, rz), base=1.3)
    # bras : epaules enneigees, avant-bras massifs, poings
    m.sb('arm_-1|arm_1', 'epaule', (30, 90, -19), (56, 118, 19), GF, base=2.4)
    m.sb('arm_-1|arm_1', 'neige_epaule', (29, 118, -20), (57, 123, 20), NE, base=4.2)
    m.sb('arm_-1|arm_1', 'bras', (32, 68, -13), (50, 92, 13), SOMBRE, base=2.6)
    m.pointe('arm_-1', 'pique_g', 44, 120, -4, 26, 6, GF, inc=(-6, -14), base=1.3)
    m.pointe('arm_1', 'pique_d', -44, 120, -4, 26, 6, GF, inc=(-6, 14), base=1.3)
    m.pointe('arm_-1', 'pique_g', 38, 120, 9, 18, 4, GF, inc=(10, -8), base=1.5)
    m.pointe('arm_1', 'pique_d', -38, 120, 9, 18, 4, GF, inc=(10, 8), base=1.5)
    m.sb('avbras_-1|avbras_1', 'avantbras', (30, 34, -16), (54, 70, 16), GF, base=2.3)
    m.sb('avbras_-1|avbras_1', 'poing', (28, 8, -18), (57, 36, 18), GF, base=2)
    m.sb('avbras_-1|avbras_1', 'jointures', (29, 8, -21), (56, 18, -18), GL, base=2.6, hide={'south'})
    bipede(p, style='poing', amp=18, genou=24, bras=14, jambe=56, gr=4, haleine=1.2)
    return p


def wyrm_boreal():
    """Wyrm boreal : grand serpent de glace dresse en S, plaques de ventre claires, eclats le long de l'echine, tete cornue, machoire ouverte."""
    m = M('wyrm_boreal', 235, 4.302, description='Wyrm boreal Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    p.meta['y_corps'] = (0, 170)
    PE = ('ecailles', 'glace')
    pts = [(-30, 7, 26), (-6, 8, 30), (28, 11, 22), (48, 24, 8), (46, 50, -4), (14, 64, -12), (-26, 74, -14), (-44, 94, -12), (-30, 116, -8), (-4, 128, -6), (6, 133, -8)]
    lar = [5, 8, 11, 13, 14, 14, 13, 12, 11, 9]

    def cote(m_, os_, i, P, W, H):
        x, y, z = P
        wx, wz = W
        m_.cb(os_, f'ventre{i}', (x - wx + 2, y, z - wz - 1), (x + wx - 2, y + H - 1, z - wz), ('nacre', 'toison'), base=3.4, h=4, axe='y', hide={'south'})
        if i >= 1:
            for k, (dy, hh, r) in enumerate(((H // 3, wx, 3), (H * 2 // 3, wx - 2, 2))):
                m_.cb(os_, f'eclat{i}_{k}', (x - r, y + dy, z + wz - 1), (x + r, y + dy + hh, z + wz + 1), GF, base=1.6, rot=(62, 0, 0), pivot=(x, y + dy, z + wz))
            m_.cb(os_, f'pic_g{i}', (x + wx - 1, y + H // 2, z - 2), (x + wx + 1, y + H // 2 + wx, z + 2), GL, ton=.2, rot=(0, 0, -55), pivot=(x + wx, y + H // 2, z))
            m_.cb(os_, f'pic_d{i}', (x - wx - 1, y + H // 2, z - 2), (x - wx + 1, y + H // 2 + wx, z + 2), GL, ton=.2, rot=(0, 0, 55), pivot=(x - wx, y + H // 2, z))
    ch = m.courbe(pts, lar, PE, prefixe='seg', parent='root', cote=cote, y=(0, 170), base=2.2)
    os_tete = ch[-1][0]
    x, y, z = pts[-1]
    p.bone('head', (x, y, z), os_tete)
    m.b = 'head'
    m.c('crane', (x - 14, y - 4, z - 24), (x + 14, y + 14, z + 8), PE, base=2.2, ton=.8, y=(0, 170))
    m.c('museau', (x - 10, y - 3, z - 44), (x + 10, y + 9, z - 24), PE, base=2.2, ton=1, y=(0, 170))
    m.c('front', (x - 11, y + 13, z - 22), (x + 11, y + 16, z + 4), GF, base=1.6)
    m.yeux('head', x + 0 if False else 4, y + 4, z - 22, 4, 4, ramp='ame', base=3.4) if False else None
    m.lueur('head', 'oeil_a', (x + 14, y + 4, z - 21), (x + 15, y + 10, z - 12), ('uni', 'ame'), base=3.6, hide={'west'})
    m.lueur('head', 'oeil_b', (x - 15, y + 4, z - 21), (x - 14, y + 10, z - 12), ('uni', 'ame'), base=3.6, hide={'east'})
    for sg, n in ((1, 'g'), (-1, 'd')):
        a0, a1 = sorted((x + sg * 5, x + sg * 10))
        m.c(f'corne_{n}', (a0, y + 11, z - 2), (a1, y + 24, z + 3), GF, base=1.6, rot=(-40, 0, -sg * 16), pivot=((a0 + a1) / 2, y + 11, z))
        m.c(f'corne2_{n}', (a0 + 1, y + 23, z - 1), (a1 - 1, y + 34, z + 2), GF, base=2.2, rot=(-40, 0, -sg * 16), pivot=((a0 + a1) / 2, y + 11, z))
    for i in range(4):
        xd = x + 6 - i * 4
        m.c(f'croc{i}', (xd, y - 8, z - 43), (xd + 2, y - 3, z - 41), GL, ton=1.2)
    p.bone('machoire', (x, y - 3, z - 12), 'head')
    m.cb('machoire', 'mandibule', (x - 9, y - 11, z - 42), (x + 9, y - 3, z - 8), PE, base=2.2, ton=.2, y=(0, 170))
    m.cb('machoire', 'langue', (x - 3, y - 3, z - 38), (x + 3, y - 2, z - 12), ('uni', 'venin'), base=2.6, hide={'down'})
    chaine = [c[0] for c in ch]
    serpent(p, chaine[1:], head='head', jaw='machoire', nom_move='slither', oscille=3.5, oscille_idle=1.2, leve=chaine[-4:], retard=.09)
    return p


GIVRE = {'renard_givre': renard_givre, 'scarabee_polaire': scarabee_polaire, 'harpie_neiges': harpie_neiges, 'golem_iceberg': golem_iceberg, 'wyrm_boreal': wyrm_boreal}
