#!/usr/bin/env python3
"""Bestiaire classique : gobelin, orc, bandit, squelette maudit, zombie, chevalier du neant, spectre de givre, elementaire de feu, golem de cristal, loup sombre, araignee venimeuse.
Os imposes par le code (ImportedModel : boneAliases, findHead) : root, body, head, arm_-1, arm_1, leg_-1, leg_1 ; leg_-10... pour le loup et l'araignee. Chaque modele a maintenant son propre PNG dans textures/imported/classic, plus de cle texture vers l'atlas."""
import math
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from animations import keys, rot, sin
from clips_mob import _fin, _r, P_ATT, bipede, cast_bipede, ev, octo_mob, quadrupede_mob
from kit_mob import CF, CR, FR, M, RE, feuille, losange, rond, triangle
from materiaux import stamp
from mobs_cryptes import ACI, FN, OS, OSV, cage, crane, epee

CU = ('cuir', 'cuir')
TB = ('peau_tissu', 'tissu_brun')
TV = ('peau_tissu', 'tissu_vert')
FR_ = ('plaque', 'fer')
NO = ('plaque', 'fer_noir')
K = {'k': '#0b0a0f', 'b': '#3ac6d4', 'c': '#dcffff', 'y': '#ffe16a', 'r': '#e0342a', 'n': '#20120b'}


def _bras_jambes(m, peau, botte, ht=(11, 18), x=(6, 9), xj=(1, 5), y_b=None):
    """Membres standards : bras en deux pieces, jambes en deux pieces, botte."""


def gobelin():
    """Gobelin : petit, grosse tete, oreilles en pointe, gilet de cuir, poignard rouille. Echelle du jeu 0,82 : 25 texels de haut en jeu."""
    m = M('gobelin', 271, .82, kind='classic', description='Gobelin Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    G = ('peau', 'gobelin')
    m.humanoide(hanche=9, epaule=16, cou=17, x_bras=6, x_jambe=3, coude=11, genou=4)
    m.sb('leg_-1|leg_1', 'cuisse', (1, 4, -2), (5, 10, 2), TB, base=2.2)
    m.sb('tibia_-1|tibia_1', 'mollet', (1, 1, -2), (5, 5, 2), G, base=3)
    m.sb('tibia_-1|tibia_1', 'pied', (1, 0, -4), (5, 2, 2), G, base=3.4)
    m.b = 'body'
    m.c('bassin', (-5, 8, -3), (5, 11, 3), TB, base=2.2)
    m.c('torse', (-5, 11, -3), (5, 18, 3), CU, ton=-.8)
    m.c('ventre', (-3, 11, -4), (3, 15, -3), G, base=3.4, hide={'south'})
    m.c('sangle', (-5, 15, -4), (5, 16, 4), CU, ton=-1.4, couture=False)
    m.b = 'head'
    m.c('crane', (-6, 17, -6), (6, 27, 5), G, base=3.4)
    m.c('nez', (-1, 20, -8), (1, 23, -6), G, base=4)
    m.yeux('head', 2, 22, -7, 3, 2, ramp='ambre', base=3.6)
    p.decal('oeil_lueur_g', 'north', stamp(['kyy', 'yyy'], K, 5, 24))
    p.bone('machoire', (0, 19, -1), 'head')
    m.cb('machoire', 'mandibule', (-5, 15, -7), (5, 19, 1), G, base=2.8)
    m.sb('machoire', 'croc', (2, 19, -8), (4, 21, -7), ('griffe', 'ivoire'), base=4)
    p.bone('oreille_g', (6, 24, -1), 'head')
    p.bone('oreille_d', (-6, 24, -1), 'head')
    OR_ = ['.........#', '......####', '...#######', '##########', '##########', '...#######', '......####']
    m.sb('oreille_g|oreille_d', 'oreille', (6, 20, -1), (16, 27, 0), ('plan', 'gobelin'), base=3.6, masque=OR_, rot=(0, -14, 6), pivot=(6, 24, -1), **CF)
    m.sb('arm_-1|arm_1', 'bras', (5, 10, -2), (8, 17, 2), G, base=3.2)
    m.sb('avbras_-1|avbras_1', 'avantbras', (5, 5, -2), (8, 11, 2), G, base=3.4)
    m.sb('avbras_-1|avbras_1', 'brassard', (4, 7, -3), (9, 10, 3), CU, ton=-1, couture=False)
    p.bone('dague', (6, 5, -2), 'avbras_-1', rot=(-60, 0, 0))
    m.cb('dague', 'manche', (5, 2, -3), (7, 7, -1), CU, couture=False)
    m.cb('dague', 'lame', (5, 7, -3), (7, 16, -2), ('metal', 'fer'), base=3)
    bipede(p, style='epee', walk=.9, attack=1.0, amp=30, genou=44, bras=26, jambe=9, jaw='machoire', oreilles=('oreille_g', 'oreille_d'), cast=True, gr=.7, arme='dague')
    return p


def orc_guerrier():
    """Orc guerrier : epaules massives, defenses, spaliere de fer a pointes sur un cote, cuir et clous, grande hache."""
    m = M('orc_guerrier', 272, 1.0, kind='classic', description='Orc guerrier Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    G = ('peau', 'orc')
    m.humanoide(hanche=13, epaule=26, cou=27, x_bras=9, x_jambe=4, coude=18, genou=6)
    m.sb('leg_-1|leg_1', 'cuisse', (1, 6, -3), (7, 14, 3), TB, base=2.4)
    m.sb('tibia_-1|tibia_1', 'greve', (1, 2, -3), (7, 7, 3), FR_, base=2.4, bord=0)
    m.sb('tibia_-1|tibia_1', 'pied', (1, 0, -5), (7, 3, 3), CU, ton=-1.2, couture=False)
    m.b = 'body'
    m.c('bassin', (-7, 12, -4), (7, 16, 4), TB, base=2.2)
    m.c('ventre', (-7, 16, -4), (7, 22, 4), G, base=3.2)
    m.c('poitrine', (-8, 22, -5), (8, 28, 5), G, base=3.6)
    m.c('ceinture', (-8, 15, -5), (8, 18, 5), CU, ton=-1.2, couture=False)
    m.c('boucle', (-2, 14, -6), (2, 19, -5), ('metal', 'fer'), base=3.2, hide={'south'})
    m.c('sangle_a', (2, 18, -6), (5, 28, -5), CU, ton=-1, couture=False, rot=(0, 0, -30), pivot=(0, 23, -5), hide={'south'})
    m.c('pagne', (-4, 6, -5), (4, 15, -4), FR('tissu_brun'), base=2.4, mini=.6, **CF)
    m.b = 'head'
    m.c('crane', (-6, 27, -6), (6, 36, 5), G, base=3.6)
    m.c('front', (-6, 33, -7), (6, 35, -6), G, base=2.6, hide={'south'})
    m.yeux('head', 2, 31, -7, 3, 2, ramp='rouge', base=4.8)
    m.c('nez', (-1, 29, -7), (1, 32, -6), G, base=2.8, hide={'south'})
    p.bone('machoire', (0, 28, -1), 'head')
    m.cb('machoire', 'mandibule', (-6, 24, -8), (6, 28, 2), G, base=3)
    m.sb('machoire', 'defense', (3, 28, -9), (5, 33, -7), ('griffe', 'ivoire'), base=3.6)
    m.cb('head', 'tresse', (-1, 28, 5), (1, 38, 7), ('cordage', 'soie_noire'), base=2.6)
    m.sb('arm_-1|arm_1', 'epaule', (7, 18, -4), (13, 27, 4), G, base=3.4)
    m.sb('avbras_-1|avbras_1', 'avantbras', (7, 8, -4), (13, 18, 4), G, base=3.2)
    m.sb('avbras_-1|avbras_1', 'brassard', (6, 11, -5), (14, 16, 5), CU, ton=-1.1, couture=False)
    m.sb('avbras_-1|avbras_1', 'poing', (7, 5, -4), (13, 9, 4), G, base=3)
    m.cb('arm_-1', 'spaliere', (6, 25, -6), (16, 31, 6), FR_, base=2.8, rivets=3)
    m.pointe('arm_-1', 'pique', 11, 31, -2, 7, 1, ('metal', 'fer'), inc=(0, -10), base=3.4)
    m.pointe('arm_-1', 'pique', 11, 31, 3, 6, 1, ('metal', 'fer'), inc=(0, -10), base=3.4)
    m.cb('arm_1', 'cuir_ep', (-15, 25, -5), (-6, 29, 5), ('plaque', 'cuir'), base=2.2, rivets=3)
    p.bone('hache', (10, 7, -4), 'avbras_-1', rot=(-42, 0, 0))
    m.cb('hache', 'manche', (9, -3, -5), (11, 26, -3), ('bois', 'bois_sombre'), base=3, planche=2)
    m.cb('hache', 'fer_a', (9, 18, -14), (11, 30, -5), ('plan', 'fer'), base=3.4, masque=['##.....', '####...', '#######', '#######', '#######', '####...', '##.....'], **CR)
    bipede(p, style='epee', walk=.9, attack=1.0, amp=26, genou=38, bras=22, jambe=13, jaw='machoire', cast=True, arme='hache')
    return p


def bandit_arbaletrier():
    """Bandit arbaletrier : capuche et foulard bordeaux, cuir sombre, sangle de carreaux, arbalete tenue devant lui."""
    m = M('bandit_arbaletrier', 273, 1.0, kind='classic', description='Bandit arbaletrier Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    BD = ('peau_tissu', 'rouge')  # bordeaux : bases basses
    H = ('peau', 'humain')
    m.humanoide(hanche=14, epaule=25, cou=26, x_bras=6, x_jambe=3, coude=17, genou=6)
    m.sb('leg_-1|leg_1', 'cuisse', (2, 6, -2), (5, 15, 2), TB, base=2.6)
    m.sb('tibia_-1|tibia_1', 'botte', (1, 3, -3), (6, 8, 3), CU, ton=-1.3, couture=False)
    m.sb('tibia_-1|tibia_1', 'pied', (1, 0, -5), (6, 3, 3), CU, ton=-1.5, couture=False)
    m.b = 'body'
    m.c('bassin', (-5, 13, -3), (5, 16, 3), TB, base=2.4)
    m.c('veste', (-5, 16, -3), (5, 26, 3), CU, ton=-1.2)
    m.c('ceinture', (-5, 15, -4), (5, 17, 4), CU, ton=-1.7, couture=False)
    m.c('baudrier', (-2, 16, -4), (1, 26, -3), CU, ton=-.7, couture=False, rot=(0, 0, -26), pivot=(0, 21, -3), hide={'south'})
    m.c('jupe_cuir', (-5, 8, -4), (5, 15, -3), FR('rouge'), base=1.6, mini=.6, **CF)
    m.c('jupe_cuir_d', (-5, 8, 3), (5, 15, 4), FR('rouge'), base=1.4, mini=.6, **CF)
    m.c('carquois', (-5, 15, 3), (-1, 26, 6), CU, ton=-.8, couture=False)
    for i, x in enumerate((-4, -3, -2)):
        m.c(f'carreau{i}', (x, 26, 4), (x + 1, 30, 5), ('bois', 'bois_clair'), planche=1)
    m.b = 'head'
    m.c('visage', (-4, 26, -4), (4, 34, 4), H, base=2.8)
    m.c('foulard', (-5, 25, -5), (5, 30, 5), BD, base=1.2)
    m.c('capuche', (-5, 30, -5), (5, 37, 6), BD, base=1.5)
    m.c('capuche_bord', (-5, 34, -6), (5, 36, -5), BD, base=1.9, hide={'south'})
    m.c('ombre', (-4, 30, -6), (4, 34, -5), ('uni', 'nuit'), base=.6, no_ao=True, hide={'south'})
    m.yeux('head', 1, 31, -6, 2, 1, ramp='ambre', base=3.6, dz=1)
    m.sb('arm_-1|arm_1', 'epaule', (4, 22, -3), (9, 27, 3), BD, base=1.3)
    m.sb('arm_-1|arm_1', 'bras', (5, 17, -2), (8, 23, 2), CU, ton=-1, couture=False)
    m.sb('avbras_-1|avbras_1', 'avantbras', (5, 10, -2), (8, 18, 2), CU, ton=-.8, couture=False)
    m.sb('avbras_-1|avbras_1', 'gant', (4, 8, -3), (9, 11, 3), CU, ton=-1.5, couture=False)
    # arbalete : bras legerement replie, fut, arc transversal
    p.bone('arbalete', (6, 10, -3), 'avbras_-1', rot=(-76, 0, 0))
    B = ('bois', 'bois_sombre')
    m.cb('arbalete', 'fut', (5, 6, -5), (7, 24, -3), B, base=3, planche=1)
    m.cb('arbalete', 'arc', (-3, 21, -5), (15, 23, -3), ('metal', 'fer'), base=2.8)
    m.cb('arbalete', 'corde', (-2, 18, -4), (14, 19, -3), ('uni', 'chair_pale'), base=3.6, hide={'up', 'down'}, no_ao=True)
    m.cb('arbalete', 'carreau_arme', (5, 18, -6), (7, 26, -5), ('metal', 'fer'), base=3.4)
    bipede(p, style='arc', walk=.9, attack=1.0, amp=28, genou=42, bras=14, jambe=14, cast=True)
    return p


def squelette_maudit():
    """Squelette maudit : os jaunis, cage ouverte, orbites bleues, fer brise, vieille epee."""
    m = M('squelette_maudit', 274, 1.0, kind='classic', description='Squelette maudit Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    m.humanoide(hanche=14, epaule=25, cou=26, x_bras=6, x_jambe=3, coude=17, genou=7)
    m.sb('leg_-1|leg_1', 'femur', (2, 7, -1), (4, 15, 1), OS, ton=-.1)
    m.sb('leg_-1|leg_1', 'genou', (1, 6, -2), (5, 9, 2), OS, ton=.6)
    m.sb('tibia_-1|tibia_1', 'tibia', (2, 2, -1), (4, 7, 1), OS, ton=-.2)
    m.sb('tibia_-1|tibia_1', 'pied', (1, 0, -3), (5, 2, 2), OS)
    m.b = 'body'
    m.c('bassin', (-4, 13, -2), (4, 16, 2), OS, ton=.1)
    m.c('colonne', (-1, 15, 0), (1, 25, 2), OS, ton=-.3)
    cage(m, 18, 3, 4, 3, pas=2, h=1)
    m.c('clavicule', (-6, 24, -2), (6, 26, 2), OS, ton=.4)
    m.c('fer_torse', (2, 18, -4), (5, 24, -3), ('plaque', 'fer_noir'), base=2.4, hide={'south'})
    m.c('pagne', (-3, 7, -3), (3, 14, -2), FR('tissu_bleu'), base=2, mini=.5, **CF)
    m.lueur('body', 'coeur', (-1, 20, -1), (1, 22, 0), ('uni', 'ame'), base=3.6)
    crane(m, 'head', 4, 26, 36, -5, 4, oeil='ame', oeil_w=2, oeil_h=2, oeil_dz=1)
    p.bone('machoire', (0, 27, -1), 'head')
    m.cb('machoire', 'mandibule', (-3, 24, -5), (3, 26, 1), OS, ton=-.2)
    m.sb('arm_-1|arm_1', 'humerus', (5, 17, -1), (7, 25, 1), OS, ton=.1)
    m.sb('arm_-1|arm_1', 'epaule', (4, 24, -2), (8, 27, 2), OS, ton=.5)
    m.sb('avbras_-1|avbras_1', 'radius', (5, 10, -1), (7, 17, 1), OS, ton=-.2)
    m.sb('avbras_-1|avbras_1', 'main', (4, 7, -2), (8, 10, 2), OS, ton=.2)
    p.bone('epee', (6, 8, -1), 'avbras_-1', rot=(-50, 0, 0))
    epee(m, 'epee', 6, 8, 18, l=1, cz=-1, lame=('metal', 'fer'), garde=('metal', 'fer'), pommeau=('metal', 'fer'))
    bipede(p, style='epee', walk=.9, attack=1.0, amp=28, genou=42, bras=24, jambe=14, jaw='machoire', cast=True, arme='epee')
    return p


def zombie_des_cryptes():
    """Zombie des cryptes : peau verdie, chemise en lambeaux, cotes a nu, machoire pendante, un bras tendu."""
    m = M('zombie_des_cryptes', 275, 1.0, kind='classic', description='Zombie des cryptes Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    Z = ('peau', 'zombie')
    m.humanoide(hanche=14, epaule=25, cou=26, x_bras=7, x_jambe=3, coude=17, genou=6)
    m.sb('leg_-1|leg_1', 'cuisse', (1, 6, -2), (6, 15, 2), TB, base=2)
    m.sb('tibia_-1|tibia_1', 'mollet', (1, 2, -2), (6, 7, 2), Z, base=3)
    m.sb('tibia_-1|tibia_1', 'pied', (1, 0, -4), (6, 2, 2), Z, base=2.6)
    m.cb('tibia_1', 'os_tibia', (-5, 3, -3), (-3, 6, -2), OS, ton=.2, hide={'south'})
    m.b = 'body'
    m.c('bassin', (-5, 13, -3), (5, 16, 3), TB, base=2)
    m.c('torse', (-6, 16, -3), (6, 26, 3), Z, base=3.2)
    m.c('cotes', (-4, 18, -4), (1, 23, -3), OS, ton=-.2, hide={'south'})
    m.c('plaie', (-4, 19, -5), (1, 20, -4), ('uni', 'rouge'), base=1.2, hide={'south'})
    m.c('plaie', (-4, 21, -5), (1, 22, -4), ('uni', 'rouge'), base=1.2, hide={'south'})
    m.c('chemise', (1, 15, -4), (6, 26, -3), FR('tissu_brun'), base=2.8, mini=.6, pas=1, **CF)
    m.c('chemise_d', (-6, 14, 3), (6, 26, 4), FR('tissu_brun'), base=2.4, mini=.6, pas=1, **CF)
    m.b = 'head'
    m.c('crane', (-5, 27, -5), (5, 36, 4), Z, base=3.4)
    m.c('cheveux', (-5, 36, -5), (5, 37, 4), ('uni', 'soie_noire'), base=2)
    m.c('orbites', (-4, 31, -6), (4, 34, -5), ('uni', 'nuit'), base=.8, no_ao=True, hide={'south'})
    m.yeux('head', 1, 31, -6, 2, 2, ramp='ame', base=3.2, dz=1)
    p.bone('machoire', (0, 28, -1), 'head')
    m.cb('machoire', 'mandibule', (-4, 24, -6), (4, 28, 1), Z, base=2.6, rot=(-10, 0, 0), pivot=(0, 28, -1))
    m.cb('machoire', 'dents', (-3, 28, -6), (3, 29, -4), OS, ton=.6, rot=(-10, 0, 0), pivot=(0, 28, -1))
    m.sb('arm_-1|arm_1', 'bras', (6, 17, -2), (10, 26, 2), Z, base=3)
    m.sb('avbras_-1|avbras_1', 'avantbras', (6, 9, -2), (10, 18, 2), Z, base=3.2)
    m.sb('avbras_-1|avbras_1', 'doigts', (6, 6, -2), (10, 9, 2), Z, base=2.4)
    m.cb('arm_-1', 'manche', (5, 20, -3), (11, 26, 3), ('peau_tissu', 'tissu_brun'), base=2.8)
    bipede(p, style='griffes', walk=.9, attack=1.0, amp=18, genou=26, bras=10, jambe=14, jaw='machoire', cast=True, posture=-6)
    return p


def chevalier_neant():
    """Chevalier du neant : armure noire aux reflets bleus, heaume a cornes, fente lumineuse, lame sombre."""
    m = M('chevalier_neant', 276, 1.0, kind='classic', description='Chevalier du neant Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    BL = ('plaque', 'tissu_bleu')
    m.humanoide(hanche=14, epaule=26, cou=27, x_bras=7, x_jambe=3, coude=18, genou=7)
    m.sb('leg_-1|leg_1', 'cuissot', (1, 7, -3), (6, 15, 3), NO, base=2.6, bord=0)
    m.sb('tibia_-1|tibia_1', 'greve', (1, 3, -3), (6, 8, 2), NO, base=2.4, bord=0)
    m.sb('tibia_-1|tibia_1', 'genou', (2, 6, -4), (5, 9, -2), BL, base=3, bord=0)
    m.sb('tibia_-1|tibia_1', 'soleret', (1, 0, -5), (6, 3, 3), NO, base=2.8, bord=0)
    m.b = 'body'
    m.c('bassin', (-5, 13, -3), (5, 17, 3), NO, base=2.2, bord=0)
    m.c('plastron', (-6, 17, -4), (6, 27, 4), NO, base=2.8)
    m.c('filet', (-1, 18, -5), (1, 26, -4), BL, base=3.4, bord=0, hide={'south'})
    m.lueur('body', 'coeur', (-2, 21, -6), (2, 25, -5), ('cristal', 'ame'), base=2.8, hide={'south'})
    m.c('tasse', (-5, 7, -4), (5, 14, -3), NO, base=2.4, bord=0, hide={'south'})
    m.c('cape', (-6, 6, 4), (6, 27, 5), FR('tissu_bleu'), base=2, mini=.65, **CF)
    m.b = 'head'
    m.c('heaume', (-4, 27, -5), (4, 36, 4), NO, base=3)
    m.c('visiere', (-3, 30, -6), (3, 32, -5), ('uni', 'nuit'), base=.6, hide={'south'})
    m.lueur('head', 'fente', (-3, 30, -7), (3, 31, -6), ('uni', 'ame'), base=4, hide={'south'})
    for sg, n in ((1, 'g'), (-1, 'd')):
        a0, a1 = sorted((sg * 3, sg * 5))
        m.c(f'corne_{n}', (a0, 35, -2), (a1, 41, 0), NO, base=3.2, bord=0, rot=(-8, 0, -sg * 16), pivot=((a0 + a1) / 2, 35, -1))
        m.c(f'corne2_{n}', (a0, 40, -2), (a1 - sg * 0, 45, 0), BL, base=3.4, bord=0, rot=(-8, 0, -sg * 16), pivot=((a0 + a1) / 2, 35, -1))
    m.sb('arm_-1|arm_1', 'bras', (5, 18, -2), (9, 26, 2), NO, base=2.4, bord=0)
    m.sb('arm_-1|arm_1', 'spaliere', (4, 24, -4), (11, 29, 4), NO, base=3.2)
    m.sb('arm_-1|arm_1', 'spaliere_b', (5, 22, -3), (11, 25, 3), BL, base=2.8, bord=0)
    m.sb('avbras_-1|avbras_1', 'avantbras', (5, 10, -2), (9, 18, 2), NO, base=2.6, bord=0)
    m.sb('avbras_-1|avbras_1', 'gantelet', (4, 7, -3), (10, 12, 3), NO, base=3, bord=0)
    p.bone('epee', (7, 8, -2), 'avbras_-1', rot=(-44, 0, 0))
    epee(m, 'epee', 7, 8, 20, l=2, cz=-2, lame=('metal', 'fer_noir'), garde=('plaque', 'tissu_bleu'), pommeau=('plaque', 'tissu_bleu'))
    bipede(p, style='epee', walk=.9, attack=1.0, amp=24, genou=36, bras=18, jambe=14, cast=True, arme='epee')
    return p


def spectre_givre():
    """Spectre de givre : silhouette de glace pale, couronne d'eclats, tunique de gel, bas du corps qui s'effiloche, yeux noirs et bleus."""
    m = M('spectre_givre', 277, 1.0, kind='classic', description='Spectre de givre Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    V = ('voile', 'glace')
    GL = ('glace', 'glace')
    m.humanoide(hanche=15, epaule=26, cou=27, x_bras=7, x_jambe=3, coude=18, genou=8)
    m.sb('leg_-1|leg_1', 'jambe', (1, 8, -3), (6, 16, 3), V, base=3.2)
    m.sb('tibia_-1|tibia_1', 'fuseau', (2, 2, -2), (5, 9, 2), GL, base=2.4)
    m.b = 'body'
    m.c('bassin', (-5, 14, -3), (5, 18, 3), V, base=3.2)
    m.c('torse', (-6, 18, -4), (6, 28, 4), V, base=3.4)
    m.c('plaque', (-3, 20, -5), (3, 27, -4), GL, ton=.4, hide={'south'})
    m.lueur('body', 'coeur', (-1, 22, -6), (1, 25, -5), ('uni', 'ame'), base=3.8, hide={'south'})
    m.c('jupe_av', (-6, 4, -4), (6, 15, -3), FR('glace'), base=3.2, mini=.45, **CF)
    m.c('jupe_ar', (-6, 3, 3), (6, 15, 4), FR('glace'), base=2.8, mini=.45, **CF)
    m.s('jupe_c', (5, 4, -3), (6, 15, 3), FR('glace'), base=2.8, mini=.45, **CR)
    m.b = 'head'
    m.c('crane', (-4, 28, -5), (4, 37, 4), ('uni', 'glace'), base=4.2)
    m.c('orbites', (-3, 32, -6), (3, 34, -5), ('uni', 'nuit'), base=.8, no_ao=True, hide={'south'})
    m.yeux('head', 1, 32, -7, 1, 2, ramp='ame', base=3.8, dz=2)
    for i, (x, h, rz) in enumerate(((-4, 7, 16), (-2, 9, 6), (1, 10, -4), (3, 8, -14))):
        m.pointe('head', 'couronne_lueur', x, 37, 0, h, 1, GL, inc=(0, rz), base=2)
    m.sb('arm_-1|arm_1', 'bras', (5, 18, -3), (10, 28, 3), V, base=3.2)
    m.sb('arm_-1|arm_1', 'eclat_ep', (6, 28, -2), (9, 30, 2), GL, ton=.4)
    m.sb('avbras_-1|avbras_1', 'avantbras', (5, 10, -3), (10, 18, 3), V, base=3.2)
    m.sb('avbras_-1|avbras_1', 'griffe', (6, 5, -2), (9, 10, 2), GL, ton=.6)
    bipede(p, style='griffes', walk=.9, attack=1.0, amp=14, genou=18, bras=10, jambe=15, cast=True, flotte=0, haleine=.6)
    return p


def elementaire_feu():
    """Elementaire de feu : tete de roche noire aux yeux de braise, couronne de flammes, tronc de basalte fendu de lave, tiges de feu qui tournent autour de lui. Pas de jambes : il flotte."""
    m = M('elementaire_feu', 278, 1.0, kind='classic', description='Elementaire de feu Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    LV = ('lave', 'basalte')
    FL = ('flamme', 'flamme')
    p.bone('body', (0, 15, 0), 'root')
    p.bone('head', (0, 24, 0), 'body')
    m.b = 'body'
    m.c('tronc', (-4, 10, -3), (4, 24, 3), LV, large=.06)
    m.c('tronc_b', (-2, 6, -2), (2, 10, 2), LV, large=.08)
    m.lueur('body', 'flamme_tronc', (-8, 2, 0), (8, 18, 1), FL, **CF)
    m.lueur('body', 'flamme_tronc2', (0, 2, -8), (1, 18, 8), FL, **CR)
    m.b = 'head'
    m.c('crane', (-6, 24, -6), (6, 35, 6), ('plaque', 'pierre_noire'), base=2.6)
    m.c('fente', (-5, 28, -7), (5, 32, -6), ('uni', 'nuit'), base=.8, no_ao=True, hide={'south'})
    m.yeux('head', 1, 29, -8, 3, 2, ramp='flamme', base=5, dz=2)
    m.lueur('head', 'couronne', (-7, 34, 0), (7, 48, 1), FL, **CF)
    m.lueur('head', 'couronne2', (0, 34, -7), (1, 48, 7), FL, **CR)
    # tiges en orbite : noms d'origine floating_rod_xx
    for i in range(8):
        os_ = f'floating_rod_{i:02d}'
        a = i * math.tau / 8
        r = 11 if i < 4 else 8
        x, z = round(r * math.cos(a)), round(r * math.sin(a))
        y = 22 if i < 4 else 8
        p.bone(os_, (0, y, 0), 'root')
        m.cb(os_, f'tige{i}', (x - 1, y, z - 1), (x + 1, y + 8, z + 1), LV, large=.09)
        m.lueur(os_, f'tige_feu{i}', (x - 2, y + 5, z), (x + 2, y + 13, z + 1), FL, **CF)
    rods = [f'floating_rod_{i:02d}' for i in range(8)]
    T = 3.0
    tr = {'body': {'position': rot(T, y=sin(.8, 1, .2))}, 'head': {'rotation': rot(T, y=sin(6, 1, 0), x=sin(2, 1, .3))}}
    for i, r in enumerate(rods):
        s = 1 if i < 4 else -1
        tr[r] = {'rotation': keys(T, lambda q, s=s: (0, s * 360 * q, 0), 16), 'position': rot(T, y=sin(1.2, 2, i * .12))}
    _fin(p, 'idle', T, tr)
    T = .9
    tr = {'body': {'position': rot(T, y=sin(.8, 1, .2)), 'rotation': rot(T, x=sin(2, 1, 0, bias=-9))}}
    for i, r in enumerate(rods):
        s = 1 if i < 4 else -1
        tr[r] = {'rotation': keys(T, lambda q, s=s: (0, s * 360 * q, 0), 16)}
    _fin(p, 'walk', T, tr)
    T = 1.0
    PH = P_ATT
    tr = {'body': {'rotation': ev(T, PH, x=(0, 10, -18, -12, -3, 0)), 'position': ev(T, PH, y=(0, 1.5, -1, -.5, 0, 0), z=(0, 1.5, -3, -2.5, -.5, 0))}, 'head': _r(T, PH, x=(0, 10, -8, -5, -1, 0))}
    for i, r in enumerate(rods):
        tr[r] = {'rotation': ev(T, PH, y=(0, 60, 240, 300, 340, 360))}
    _fin(p, 'attack', T, tr, False)
    T = 2.4
    PH = (0, .3, .75, 1)
    tr = {'body': {'position': ev(T, PH, y=(0, 3, 3, 0))}, 'head': _r(T, PH, x=(0, 14, 14, 0))}
    for i, r in enumerate(rods):
        tr[r] = {'rotation': keys(T, lambda q: (0, 720 * q, 0), 16)}
    _fin(p, 'cast', T, tr, False)
    T = 1.0
    PH = (0, .5, 1)
    tr = {'body': {'position': ev(T, PH, y=(0, -3, -8))}, 'head': _r(T, PH, x=(0, -18, -30))}
    for i, r in enumerate(rods):
        tr[r] = {'position': ev(T, PH, y=(0, -8, -20))}
    _fin(p, 'death', T, tr, False)
    return p


def golem_cristal():
    """Golem de cristal : corps de roche sombre, epaules et tete de glace bleue, noyau de cristal dans la poitrine, grappes de cristaux sur les epaules, longs bras."""
    m = M('golem_cristal', 279, 1.0, kind='classic', description='Golem de cristal Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    RO = ('plaque', 'pierre_noire')
    CR_ = ('cristal', 'glace')
    m.humanoide(hanche=16, epaule=33, cou=35, x_bras=13, x_jambe=6, coude=22, genou=7)
    m.sb('leg_-1|leg_1', 'cuisse', (2, 7, -5), (10, 18, 5), RO, base=2.4)
    m.sb('tibia_-1|tibia_1', 'tibia', (2, 3, -5), (10, 8, 5), RO, base=2.2)
    m.sb('tibia_-1|tibia_1', 'pied', (1, 0, -7), (11, 4, 5), RO, base=2.6)
    m.b = 'body'
    m.c('bassin', (-9, 14, -5), (9, 20, 5), RO, base=2.2)
    m.c('ventre', (-8, 20, -5), (8, 26, 5), RO, base=2)
    m.c('poitrine', (-11, 26, -6), (11, 38, 6), RO, base=2.8)
    m.c('cadre', (-5, 28, -7), (5, 37, -6), ('plaque', 'chitine_bleue'), base=3, hide={'south'})
    m.lueur('body', 'noyau', (-3, 29, -9), (3, 36, -7), ('cristal', 'ame'), base=2.6)
    m.b = 'head'
    m.c('crane', (-6, 37, -7), (6, 47, 4), CR_, base=1.8)
    m.c('machoire_b', (-5, 35, -7), (5, 38, 2), RO, base=2.8)
    m.c('fente', (-4, 41, -8), (4, 43, -7), ('uni', 'nuit'), base=.8, no_ao=True, hide={'south'})
    m.yeux('head', 1, 41, -9, 2, 2, ramp='ame', base=3.8, dz=2)
    m.sb('arm_-1|arm_1', 'epaule', (9, 28, -6), (20, 37, 6), CR_, base=1.6)
    m.sb('arm_-1|arm_1', 'bras', (10, 21, -4), (18, 29, 4), RO, base=2.4)
    for sg, os_ in ((1, 'arm_-1'), (-1, 'arm_1')):
        m.pointe(os_, 'grappe_lueur', sg * 15, 37, -2, 10, 2, CR_, inc=(0, -sg * 8), base=1.6)
        m.pointe(os_, 'grappe_lueur', sg * 12, 37, 2, 7, 1, CR_, inc=(8, sg * 8), base=1.8)
        m.pointe(os_, 'grappe_lueur', sg * 18, 37, 2, 8, 1, CR_, inc=(6, -sg * 18), base=1.8)
    m.sb('avbras_-1|avbras_1', 'avantbras', (9, 8, -5), (19, 22, 5), RO, base=2.6)
    m.sb('avbras_-1|avbras_1', 'poing', (8, 1, -6), (20, 9, 6), CR_, base=1.4)
    bipede(p, style='poing', walk=.9, attack=1.0, amp=18, genou=24, bras=12, jambe=16, cast=True, gr=.9, haleine=.5)
    return p


def loup_sombre():
    """Loup sombre : pelage noir, criniere brune en collier, museau long, oreilles dressees, crocs, yeux bleus."""
    m = M('loup_sombre', 280, 1.0, kind='classic', description='Loup sombre Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    p.meta['y_corps'] = (4, 20)
    FU = ('fourrure', 'nuit')
    CRI = ('poils_longs', 'fourrure_brune')
    p.bone('body', (0, 13, 0), 'root')
    m.b = 'body'
    m.c('poitrail', (-4, 8, -11), (4, 17, -2), FU, ton=.4)
    m.c('taille', (-3, 9, -2), (3, 16, 7), FU)
    m.c('croupe', (-4, 8, 7), (4, 16, 14), FU, ton=.3)
    m.c('criniere', (-5, 8, -12), (5, 19, -6), CRI, ton=-.2, y=(4, 20))
    m.c('crete_dos', (-2, 15, -6), (2, 18, 2), CRI, ton=-.6, y=(4, 20))
    p.bone('head', (0, 15, -11), 'body')
    m.b = 'head'
    m.c('crane', (-4, 13, -18), (4, 20, -11), FU, ton=.8)
    m.c('museau', (-2, 13, -25), (2, 17, -18), ('uni', 'nuit'), base=3.4)
    m.c('truffe', (-1, 15, -26), (1, 17, -25), ('uni', 'nuit'), base=1, hide={'south'})
    m.yeux('head', 1, 17, -18, 2, 1, ramp='ame', base=3.8)
    p.bone('machoire', (0, 13, -14), 'head')
    m.cb('machoire', 'mandibule', (-2, 11, -24), (2, 13, -13), ('uni', 'nuit'), base=2.6)
    m.sb('machoire', 'croc', (1, 13, -24), (2, 15, -23), ('griffe', 'ivoire'), base=4)
    p.bone('ear_g', (3, 20, -13), 'head')
    p.bone('ear_d', (-3, 20, -13), 'head')
    OR_ = ['..##', '.###', '####', '####', '####']
    m.sb('ear_g|ear_d', 'oreille', (1, 19, -14), (5, 25, -13), ('plan', 'nuit'), base=3.6, masque=OR_, **CF)
    p.bone('tail', (0, 15, 14), 'body')
    p.bone('queue_b', (0, 13, 21), 'tail')
    m.cb('tail', 'queue', (-2, 11, 13), (2, 16, 22), CRI, ton=-.8, y=(4, 20), rot=(14, 0, 0), pivot=(0, 15, 14))
    m.cb('queue_b', 'queue_bout', (-2, 7, 20), (2, 13, 26), FU, ton=.2, rot=(28, 0, 0), pivot=(0, 13, 21))
    legs = []
    for nom, x, z, ph in (('leg_-10', 3, -7, 0.0), ('leg_10', -3, -7, .5), ('leg_-11', 3, 10, .56), ('leg_11', -3, 10, .06)):
        p.bone(nom, (x, 13, z), 'root')
        p.bone(nom + '_bas', (x, 6, z), nom)
        legs.append((nom, nom + '_bas', ph))
    m.sb('leg_-10|leg_10', 'bras', (2, 5, -9), (5, 14, -5), FU, y=(0, 20), hide={'up'})
    m.sb('leg_-10_bas|leg_10_bas', 'canon_av', (2, 1, -9), (4, 6, -6), FU, y=(0, 20), ton=-.4)
    m.sb('leg_-10_bas|leg_10_bas', 'patte_av', (2, 0, -11), (5, 2, -6), ('uni', 'fourrure_brune'), base=2.4)
    m.sb('leg_-11|leg_11', 'cuisse', (2, 6, 8), (5, 14, 13), FU, y=(0, 20), hide={'up'})
    m.sb('leg_-11_bas|leg_11_bas', 'canon_ar', (2, 1, 9), (4, 6, 12), FU, y=(0, 20), ton=-.4)
    m.sb('leg_-11_bas|leg_11_bas', 'patte_ar', (2, 0, 7), (5, 2, 11), ('uni', 'fourrure_brune'), base=2.4)
    quadrupede_mob(p, legs, jaw='machoire', ears=('ear_g', 'ear_d'), tail_tip='queue_b', amp=32, knee=52, leg_len=13, poids=.7, walk=.9, attack=1.0, cast=True, gr=.8)
    return p


def araignee_venimeuse():
    """Araignee venimeuse : abdomen noir a marque rouge, cephalothorax brun jaune, huit yeux bleus, crocs, longues pattes rayees."""
    m = M('araignee_venimeuse', 281, 1.0, kind='classic', description='Araignee venimeuse Aeloria : refonte (16 texels par bloc en jeu).')
    p = m.p
    AB = ('plaque', 'nuit')
    TH = ('segments', 'chitine')
    p.bone('body', (0, 5, 0), 'root')
    m.b = 'body'
    m.c('abdomen', (-6, 3, 2), (6, 11, 13), AB, base=3)
    m.c('abdomen_h', (-4, 11, 4), (4, 12, 11), AB, base=3.4, bord=0)
    m.c('marque_lueur', (-2, 12, 5), (2, 13, 10), ('plan', 'venin'), base=3.4, masque=['.##.', '####', '.##.', '####', '.##.'], hide={'north', 'south', 'east', 'west', 'down'}, no_shadow=True)
    m.c('filiere', (-2, 4, 13), (2, 8, 14), ('uni', 'chitine'), base=2)
    p.bone('head', (0, 5, -2), 'body')
    m.b = 'head'
    m.c('thorax', (-4, 3, -7), (4, 9, 2), TH, pas=3, base=2.2)
    m.yeux('head', 1, 6, -7, 1, 1, ramp='ame', base=4)
    m.yeux('head', 3, 7, -7, 1, 1, ramp='ame', base=4, nom='oeil_b_lueur')
    m.yeux('head', 1, 8, -7, 1, 1, ramp='ame', base=3.4, nom='oeil_c_lueur', dz=1) if False else None
    p.bone('croc_g', (2, 4, -7), 'head')
    p.bone('croc_d', (-2, 4, -7), 'head')
    m.sb('croc_g|croc_d', 'croc', (1, 1, -9), (3, 5, -7), ('griffe', 'venin'), base=3)
    legs = []
    for i, (nom, z, fan) in enumerate((('0', -4, 34), ('1', -1, 12), ('2', 1, -10), ('3', 3, -30))):
        g, d = f'leg_-1{nom}', f'leg_1{nom}'
        p.bone(g, (3, 5, z), 'body')
        p.bone(d, (-3, 5, z), 'body')
        xk = round(3 + 8 * math.cos(math.radians(fan)))
        zk = round(z - 8 * math.sin(math.radians(fan)))
        p.bone(g + '_bas', (xk, 8, zk), g)
        p.bone(d + '_bas', (-xk, 8, zk), d)
        m.sb(f'{g}|{d}', f'cuisse{i}', (2, 4, z), (11, 6, z + 1), ('segments', 'chitine'), pas=3, axe='x', base=2.6, rot=(0, fan, 20), pivot=(3, 5, z))
        m.sb(f'{g}_bas|{d}_bas', f'tibia{i}', (xk, 0, zk), (xk + 1, 9, zk + 1), ('segments', 'nuit'), pas=3, axe='y', base=3.2, rot=(0, 0, 14), pivot=(xk, 8, zk))
        legs += [(g, g + '_bas', 1), (d, d + '_bas', -1)]
    octo_mob(p, legs, head='head', jaw=(('croc_g', 1), ('croc_d', -1)), walk=.9, attack=1.0, cast=True, gr=.6)
    return p


CLASSIC = {'gobelin': gobelin, 'orc_guerrier': orc_guerrier, 'bandit_arbaletrier': bandit_arbaletrier, 'squelette_maudit': squelette_maudit, 'zombie_des_cryptes': zombie_des_cryptes, 'chevalier_neant': chevalier_neant, 'spectre_givre': spectre_givre, 'elementaire_feu': elementaire_feu, 'golem_cristal': golem_cristal, 'loup_sombre': loup_sombre, 'araignee_venimeuse': araignee_venimeuse}
