#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Six arcs (niveaux 1, 20, 40, 60, 80, 100) pour Blockbench, format "Java Block/Item", avec pour chacun
un modele au repos et trois etapes de traction (pulling_0, pulling_1, pulling_2), comme l'arc de Minecraft.

Usage :  python generer_arcs.py      (Python 3.8+, Pillow ; utilise ../blockbench_kit.py)

Geometrie (vue de face, plan XY) : branches selon Y, poignee a gauche (x = 4), corde a droite (cote archer),
la fleche part vers -X en passant par la fenetre de la poignee (y = 8,25). En traction, les branches flechissent,
la corde garde sa longueur et rejoint le point d'encoche ; une fleche est encochee.
"""
import math
import os
import sys

sys.path.insert(0, os.path.join(os.path.dirname(os.path.abspath(__file__)), '..'))
import blockbench_kit as bk  # noqa: E402

ICI = os.path.dirname(os.path.abspath(__file__))
NS = 'mmorpg'
CY = 8.25          # hauteur de la fleche / centre de l'arc
GX = 4.0           # axe de la poignee
COLORS = {'poignee': 2, 'branche_superieure': 1, 'branche_inferieure': 1, 'corde': 0, 'cristaux': 5, 'ornements': 6, 'fleche': 3}
STAGES = (None, 0, 1, 2)
FLEX = {None: 0.0, 0: 7.0, 1: 18.0, 2: 31.0}           # flexion des branches (degres) a chaque etape
FLEX_WEIGHTS = (0.0, 0.35, 0.75, 1.0)                 # repartition de la flexion le long de la branche

# Reglages d'affichage : ceux de l'arc vanilla, avec -45 deg sur Z (l'arc vanilla est dessine en diagonale)
DISPLAY = {
    'thirdperson_righthand': {'rotation': [-80, 260, -85], 'translation': [-1, -2, 2.5], 'scale': [0.48, 0.48, 0.48]},
    'thirdperson_lefthand': {'rotation': [-80, -280, 85], 'translation': [-1, -2, 2.5], 'scale': [0.48, 0.48, 0.48]},
    'firstperson_righthand': {'rotation': [0, -90, -20], 'translation': [1.13, 3.2, 1.13], 'scale': [0.36, 0.36, 0.36]},
    'firstperson_lefthand': {'rotation': [0, 90, 20], 'translation': [1.13, 3.2, 1.13], 'scale': [0.36, 0.36, 0.36]},
    'ground': {'rotation': [0, 0, 0], 'translation': [0, 2, 0], 'scale': [0.3, 0.3, 0.3]},
    'gui': {'rotation': [0, 0, -45], 'translation': [0.6, 0, 0], 'scale': [0.42, 0.42, 0.42]},
    'head': {'rotation': [0, 90, 0], 'translation': [0, 6, 0], 'scale': [0.5, 0.5, 0.5]},
    'fixed': {'rotation': [0, 0, -45], 'translation': [0.5, 0, 0], 'scale': [0.5, 0.5, 0.5]},
}


class Seg:
    """Troncon de branche : origine, extremite, angle de direction, largeur, epaisseur, angle 'exterieur' (dos de l'arc)."""

    def __init__(self, p0, p1, ang, w, d, out):
        self.p0, self.p1, self.ang, self.w, self.d, self.out = p0, p1, ang, w, d, out

    def at(self, t, off=0.0):
        x = self.p0[0] + t * (self.p1[0] - self.p0[0]) + off * math.cos(math.radians(self.out))
        y = self.p0[1] + t * (self.p1[1] - self.p0[1]) + off * math.sin(math.radians(self.out))
        return x, y

    @property
    def length(self):
        return math.hypot(self.p1[0] - self.p0[0], self.p1[1] - self.p0[1])


def limb_frames(spec, stage):
    """Troncons de la branche superieure puis de la branche inferieure (symetrique par rapport a y = CY)."""
    flex = FLEX[stage]
    up, low = [], []
    p = (GX, CY + 5.5)
    for i, (L, a, w) in enumerate(zip(spec['lens'], spec['angles'], spec['widths'])):
        ang = a - flex * FLEX_WEIGHTS[i]
        q = (p[0] + L * math.cos(math.radians(ang)), p[1] + L * math.sin(math.radians(ang)))
        up.append(Seg(p, q, ang, w, spec['depth'], ang + 90.0))
        low.append(Seg((p[0], 2 * CY - p[1]), (q[0], 2 * CY - q[1]), -ang, w, spec['depth'], -ang - 90.0))
        p = q
    return up, low


def att(m, group, name, s, t, off, along, across, depth, mat, extra=0.0, cz=8.0, **kw):
    """Piece attachee a un troncon (suit la flexion de la branche)."""
    x, y = s.at(t, off)
    return bk.rbox(m, group, name, (x, y, cz), (along, across, depth), s.ang + extra, mat, **kw)


def att_plates(m, group, name, s, t, along, across, mat, light=15, deco=None, off=0.0):
    """Plaques plates (runes, energie) devant et derriere un troncon."""
    x, y = s.at(t, off)
    for side, z in (('north', 8 - s.d / 2 - 0.1), ('south', 8 + s.d / 2 + 0.1)):
        bk.rbox(m, group, f'{name}_{"avant" if side == "north" else "arriere"}', (x, y, z), (along, across, 0), s.ang, mat,
                light=light, faces={side}, deco={side: deco} if deco else None)


def both(frames_up, frames_low, fn):
    """Applique une decoration aux deux branches (groupe et suffixe adaptes)."""
    fn(frames_up, 'branche_superieure', 'haut')
    fn(frames_low, 'branche_inferieure', 'bas')


# ============================================================================================== construction
def build(spec, stage):
    m = bk.Model(spec['key'] if stage is None else f"{spec['key']}_pulling_{stage}", spec['level'], spec['titre'], strict=stage is None)
    up, low = limb_frames(spec, stage)
    rm, gm = spec['riser'], spec['grip']
    d = spec['depth']
    # poignee : fut, fenetre de tir (la fleche passe a y = CY), cuir
    bk.box(m, 'poignee', 'fut_haut', GX - 1, CY + 0.5, 8 - d / 2 - 0.15, GX + 1, CY + 5.9, 8 + d / 2 + 0.15, rm)
    bk.box(m, 'poignee', 'fut_bas', GX - 1, CY - 5.9, 8 - d / 2 - 0.15, GX + 1, CY - 0.5, 8 + d / 2 + 0.15, rm)
    bk.box(m, 'poignee', 'fenetre_de_tir', GX - 1, CY - 0.5, 8 - d / 2 - 0.15, GX + 1, CY + 0.5, 7.5, rm)
    bk.box(m, 'poignee', 'cuir', GX - 1.3, CY - 5.0, 8 - d / 2 - 0.45, GX + 1.3, CY - 0.75, 8 + d / 2 + 0.45, gm)
    # branches
    for frames, g, sfx in ((up, 'branche_superieure', 'haut'), (low, 'branche_inferieure', 'bas')):
        for i, s in enumerate(frames):
            bk.rbox(m, g, f'troncon_{sfx}_{i + 1}', ((s.p0[0] + s.p1[0]) / 2, (s.p0[1] + s.p1[1]) / 2, 8.0),
                    (s.length + s.w * 0.6, s.w, s.d), s.ang, spec['limb'])
        att(m, g, f'encoche_{sfx}', frames[-1], 0.95, 0.0, 1.1, frames[-1].w + 0.35, frames[-1].d + 0.3, spec['nock'])
    # decorations propres au niveau
    spec['deco'](m, up, low, stage)
    # corde : droite au repos, en V vers le point d'encoche en traction (longueur conservee)
    tip_u, tip_l = up[-1].p1, low[-1].p1
    rest_up, _ = limb_frames(spec, None)
    half = rest_up[-1].p1[1] - CY
    sm, sl = spec['string'], 15 if spec['string'] == 'corde_magique' else 0
    if stage is None:
        bk.box(m, 'corde', 'corde', tip_u[0] - 0.15, tip_l[1], 7.85, tip_u[0] + 0.15, tip_u[1], 8.15, sm, light=sl)
    else:
        xd = tip_u[0] + math.sqrt(max(0.0, half * half - (tip_u[1] - CY) ** 2))
        for name, a, b in (('corde_haut', tip_u, (xd, CY)), ('corde_bas', (xd, CY), tip_l)):
            L = math.hypot(b[0] - a[0], b[1] - a[1])
            ang = math.degrees(math.atan2(b[1] - a[1], b[0] - a[0]))
            bk.rbox(m, 'corde', name, ((a[0] + b[0]) / 2, (a[1] + b[1]) / 2, 8.0), (L + 0.2, 0.3, 0.3), ang, sm, light=sl)
        # fleche encochee
        L = 21.0
        bk.box(m, 'fleche', 'fut', xd - L, CY - 0.25, 7.75, xd - 0.2, CY + 0.25, 8.25, 'bois_clair')
        bk.cbox(m, 'fleche', 'pointe', (xd - L - 0.3, CY, 8.0), (1.3, 1.3, 0.4), 'argent', rot=('z', 45, [xd - L - 0.3, CY, 8.0]))
        bk.box(m, 'fleche', 'empennage_haut', xd - 3.6, CY + 0.25, 7.95, xd - 0.9, CY + 1.0, 8.05, 'plume')
        bk.box(m, 'fleche', 'empennage_bas', xd - 3.6, CY - 1.0, 7.95, xd - 0.9, CY - 0.25, 8.05, 'plume')
        bk.box(m, 'fleche', 'encoche_fleche', xd - 0.5, CY - 0.35, 7.65, xd - 0.1, CY + 0.35, 8.35, 'bois')
        m.draw = xd - tip_u[0]
    return m


# ============================================================================================== les 6 arcs
def deco_1(m, up, low, stage):
    for y1, y2 in ((CY + 5.0, CY + 5.6), (CY - 5.6, CY - 5.0)):
        bk.box(m, 'poignee', 'lien_cuir', GX - 1.25, y1, 7.05, GX + 1.25, y2, 8.95, 'cuir')


def deco_20(m, up, low, stage):
    for y1, y2 in ((CY + 5.2, CY + 6.0), (CY - 6.0, CY - 5.2)):
        bk.box(m, 'ornements', 'bague_fut', GX - 1.4, y1, 6.6, GX + 1.4, y2, 9.4, 'argent')
    for z, side in ((6.75, 'avant'), (9.25, 'arriere')):
        bk.cbox(m, 'cristaux', f'pierre_fut_{side}', (GX, CY + 3.0, z), (0.9, 0.9, 0.4), 'cristal', rot=('z', 45, [GX, CY + 3.0, z]), light=10)

    def limb(fr, g, sfx):
        att(m, 'ornements', f'attache_{sfx}', fr[1], 0.5, 0, 0.8, fr[1].w + 0.5, fr[1].d + 0.5, 'argent')
        att(m, 'ornements', f'embout_{sfx}', fr[3], 0.82, 0, 1.4, fr[3].w + 0.45, fr[3].d + 0.45, 'metal')
        for t_seg, t, label in ((fr[1], 0.5, 'attache'), (fr[3], 0.82, 'embout')):
            for z, side in ((8 - t_seg.d / 2 - 0.4, 'avant'), (8 + t_seg.d / 2 + 0.4, 'arriere')):
                x, y = t_seg.at(t)
                bk.cbox(m, 'cristaux', f'pierre_{label}_{sfx}_{side}', (x, y, z), (0.6, 0.6, 0.3), 'cristal', rot=('z', 45, [x, y, z]), light=10)
    both(up, low, limb)


def deco_40(m, up, low, stage):
    bk.diamond_plate(m, 'ornements', 'losange_fut', (GX, CY + 3.1), 1.9, 0.9, 8 - 0.9 - 0.15, 8 + 0.9 + 0.15)
    for y1, y2 in ((CY + 5.4, CY + 6.0), (CY - 6.0, CY - 5.4), (CY - 0.95, CY - 0.6), (CY - 5.2, CY - 4.85)):
        bk.box(m, 'ornements', 'bague', GX - 1.45, y1, 6.6, GX + 1.45, y2, 9.4, 'argent')

    def limb(fr, g, sfx):
        for i in (1, 2):
            att(m, 'ornements', f'renfort_{sfx}', fr[i], 0.5, fr[i].w / 2 + 0.2, fr[i].length * 0.75, 0.5, fr[i].d + 0.2, 'metal')
            att_plates(m, 'ornements', f'runes_{sfx}_{i}', fr[i], 0.5, fr[i].length * 0.55, fr[i].w * 0.6, 'rune')
            att(m, 'ornements', f'jointure_{sfx}', fr[i], 1.0, 0, 0.6, fr[i].w + 0.6, fr[i].d + 0.6, 'argent')
        s = fr[3]
        x, y = s.at(0.7, s.w / 2 + 0.9)
        bk.crystal(m, 'cristaux', f'cristal_{sfx}', x, y, 8.0, 1.6, 2.6, side=False, angle=s.ang - 90.0)
        att(m, 'ornements', f'sertissure_{sfx}', s, 0.7, s.w / 2 + 0.2, 1.4, 0.5, s.d + 0.4, 'argent')
    both(up, low, limb)


def deco_60(m, up, low, stage):
    bk.diamond_plate(m, 'ornements', 'losange_fut', (GX, CY + 3.1), 2.1, 1.0, 8 - 0.9 - 0.15, 8 + 0.9 + 0.15)
    for sgn, sfx in ((1, 'haut'), (-1, 'bas')):
        bk.seg(m, 'ornements', f'eperon_fut_{sfx}', (GX - 0.8, CY + sgn * 4.6), 180 + sgn * 22.5 * -1, 2.2, 0.7, 0.8, 'argent')
        bk.seg(m, 'ornements', f'eperon_fut_pointe_{sfx}', (GX - 2.8, CY + sgn * 5.4), 180 - sgn * 45, 0.9, 0.45, 0.6, 'argent')

    def limb(fr, g, sfx):
        for i in (1, 2, 3):
            att(m, 'ornements', f'arete_{sfx}', fr[i], 0.5, fr[i].w / 2 + 0.15, fr[i].length * 0.95, 0.4, fr[i].d * 0.7, 'argent')
        for i in (1, 2):
            att_plates(m, 'ornements', f'energie_{sfx}_{i}', fr[i], 0.5, fr[i].length * 0.85, 0.45, 'energie')
        s = fr[2]
        x, y = s.at(0.55)
        bk.rbox(m, 'cristaux', f'cristal_integre_{sfx}', (x, y, 8.0), (1.5, 1.5, s.d + 0.7), s.ang + 45, 'cristal', light=15)
        # extremite fourchue
        e = fr[3]
        p = e.p1
        bk.seg(m, 'ornements', f'fourche_ext_{sfx}', p, e.ang + 22.5 * (1 if sfx == 'haut' else -1), 2.6, 0.7, 0.8, 'argent')
        bk.seg(m, 'ornements', f'fourche_int_{sfx}', p, e.ang - 45 * (1 if sfx == 'haut' else -1), 2.0, 0.6, 0.8, 'argent')
    both(up, low, limb)


def deco_80(m, up, low, stage):
    bk.diamond_plate(m, 'ornements', 'losange_fut', (GX, CY + 3.1), 2.2, 1.1, 8 - 0.9 - 0.15, 8 + 0.9 + 0.15, gem_mat='cristal', gem_light=15)
    for sgn, sfx in ((1, 'haut'), (-1, 'bas')):
        bk.seg(m, 'ornements', f'aile_fut_{sfx}', (GX - 0.8, CY + sgn * 4.2), 180 - sgn * 45, 2.6, 0.8, 0.9, 'argent')
        bk.seg(m, 'ornements', f'aile_fut_2_{sfx}', (GX - 0.8, CY + sgn * 2.4), 180 - sgn * 22.5, 1.8, 0.6, 0.8, 'metal')

    def limb(fr, g, sfx):
        sg = 1 if sfx == 'haut' else -1
        for i in (1, 2, 3):
            s = fr[i]
            att(m, 'ornements', f'lame_exterieure_{sfx}', s, 0.5, s.w / 2 + 0.75, s.length * 0.9, 0.7, s.d * 0.8, 'argent')
            att(m, 'ornements', f'entretoise_{sfx}', s, 0.5, s.w / 2 + 0.3, 0.5, 0.6, s.d * 0.6, 'metal')
            att_plates(m, 'ornements', f'runes_{sfx}_{i}', s, 0.5, s.length * 0.5, s.w * 0.6, 'rune')
        s = fr[1]
        x, y = s.at(0.5, s.w / 2 + 1.1)
        bk.seg(m, 'ornements', f'pointe_angulaire_{sfx}', (x, y), s.out + sg * 22.5, 1.8, 0.6, 0.7, 'argent')
        e = fr[3]
        x, y = e.at(0.75, e.w / 2 + 0.9)
        bk.crystal(m, 'cristaux', f'cristal_{sfx}', x, y, 8.0, 1.8, 3.0, side=False, angle=e.ang - 90.0)
        bk.seg(m, 'ornements', f'fourche_{sfx}', e.p1, e.ang + 22.5 * sg, 1.8, 0.55, 0.7, 'argent')
    both(up, low, limb)
    for i, (c, s, ax) in enumerate((((-0.6, 26.0, 8), 0.8, 'z'), ((14.6, 29.0, 8), 0.7, 'x'), ((-0.6, -9.5, 8), 0.8, 'x'),
                                    ((14.6, -12.4, 8), 0.7, 'z'), ((1.0, 18.5, 5.6), 0.6, 'z'), ((1.0, -2.0, 10.4), 0.6, 'x'))):
        bk.frag(m, 'cristaux/fragments_flottants', f'fragment_{i + 1}', c, s, ax)


def deco_100(m, up, low, stage):
    bk.diamond_plate(m, 'ornements', 'losange_fut', (GX, CY + 3.0), 2.6, 1.3, 8 - 1.0 - 0.15, 8 + 1.0 + 0.15, gem_mat='cristal', gem_light=15)
    for y1, y2 in ((CY - 0.95, CY - 0.6), (CY - 3.0, CY - 2.65), (CY - 5.2, CY - 4.85)):
        bk.box(m, 'ornements', 'bague', GX - 1.5, y1, 6.5, GX + 1.5, y2, 9.5, 'argent')
    for sgn, sfx in ((1, 'haut'), (-1, 'bas')):
        p = bk.seg(m, 'ornements', f'aile_fut_{sfx}', (GX - 0.9, CY + sgn * 4.6), 180 - sgn * 45, 3.2, 1.0, 1.0, 'argent')
        bk.seg(m, 'ornements', f'aile_fut_pointe_{sfx}', p, 180 - sgn * 67.5 + (0 if sgn > 0 else 0), 1.6, 0.6, 0.8, 'argent')
        bk.seg(m, 'ornements', f'aile_fut_2_{sfx}', (GX - 0.9, CY + sgn * 2.2), 180 - sgn * 22.5, 2.2, 0.7, 0.8, 'metal')

    def limb(fr, g, sfx):
        sg = 1 if sfx == 'haut' else -1
        for i in (1, 2, 3):
            s = fr[i]
            att(m, 'ornements', f'lame_exterieure_{sfx}', s, 0.5, s.w / 2 + 0.85, s.length * 0.95, 0.9, s.d * 0.85, 'argent')
            att(m, 'ornements', f'entretoise_{sfx}', s, 0.5, s.w / 2 + 0.3, 0.6, 0.7, s.d * 0.6, 'metal')
        for i in (1, 2):
            att_plates(m, 'ornements', f'runes_{sfx}_{i}', fr[i], 0.5, fr[i].length * 0.5, fr[i].w * 0.6, 'rune')
            att_plates(m, 'ornements', f'energie_{sfx}_{i}', fr[i], 0.5, fr[i].length * 0.9, 0.35, 'energie', off=fr[i].w * 0.36)
        s = fr[1]
        x, y = s.at(0.5, s.w / 2 + 1.3)
        bk.seg(m, 'ornements', f'pointe_decorative_{sfx}', (x, y), s.out + sg * 22.5, 2.2, 0.7, 0.8, 'argent')
        e = fr[3]
        x, y = e.at(0.55, e.w / 2 + 1.8)
        bk.crystal(m, 'cristaux', f'grand_cristal_{sfx}', x, y, 8.0, 3.2, 5.0, side=False, angle=e.ang - 90.0)
        for k, off in ((1, 0.15), (2, 0.95)):
            att(m, 'ornements', f'griffe_cristal_{sfx}_{k}', e, off, e.w / 2 + 1.8, 0.5, 3.4, 1.0, 'argent')
        bk.seg(m, 'ornements', f'pointe_finale_{sfx}', e.p1, e.ang + 22.5 * sg, 2.4, 0.65, 0.8, 'argent')
    both(up, low, limb)
    for i, (c, s, ax) in enumerate((((-1.6, 27.5, 8), 0.9, 'z'), ((15.4, 30.4, 8), 0.8, 'x'), ((-1.6, -11.0, 8), 0.9, 'x'),
                                    ((15.4, -13.8, 8), 0.8, 'z'), ((0.4, 20.5, 5.4), 0.7, 'z'), ((0.4, -4.0, 10.6), 0.7, 'x'),
                                    ((-3.4, 14.0, 8), 0.6, 'z'), ((-3.4, 2.5, 8), 0.6, 'x'))):
        bk.frag(m, 'cristaux/fragments_flottants', f'fragment_{i + 1}', c, s, ax)


SPECS = [
    dict(key='arc_niv_01', level=1, titre='Arc du novice', lens=[2.0, 4.5, 4.5, 4.0], angles=[90, 67.5, 67.5, 45],
         widths=[1.75, 1.6, 1.4, 1.2], depth=1.5, limb='bois_clair', riser='bois_clair', grip='cuir', nock='bois', string='corde', deco=deco_1),
    dict(key='arc_niv_20', level=20, titre='Arc renforcé', lens=[2.0, 4.5, 4.5, 4.0], angles=[90, 67.5, 67.5, 45],
         widths=[2.0, 1.75, 1.5, 1.25], depth=1.75, limb='bois', riser='bois', grip='cuir', nock='metal', string='corde', deco=deco_20),
    dict(key='arc_niv_40', level=40, titre='Arc runique', lens=[2.0, 4.5, 4.5, 4.5], angles=[90, 67.5, 67.5, 45],
         widths=[2.0, 1.75, 1.5, 1.25], depth=1.75, limb='bois', riser='metal', grip='cuir_tresse', nock='argent', string='corde_runique', deco=deco_40),
    dict(key='arc_niv_60', level=60, titre='Arc du traqueur', lens=[2.0, 4.5, 4.5, 4.0], angles=[90, 67.5, 67.5, 45],
         widths=[2.0, 1.75, 1.5, 1.25], depth=1.75, limb='metal', riser='metal', grip='cuir_tresse', nock='argent', string='corde_runique', deco=deco_60),
    dict(key='arc_niv_80', level=80, titre='Arc légendaire', lens=[2.0, 5.0, 4.5, 4.0], angles=[90, 67.5, 67.5, 45],
         widths=[2.0, 1.75, 1.5, 1.25], depth=1.75, limb='metal', riser='metal', grip='cuir_tresse', nock='argent', string='corde_magique', deco=deco_80),
    dict(key='arc_niv_100', level=100, titre='Arc mythique', lens=[2.0, 5.0, 4.5, 4.0], angles=[90, 67.5, 67.5, 45],
         widths=[2.25, 2.0, 1.75, 1.5], depth=2.0, limb='metal', riser='metal', grip='cuir_tresse', nock='argent', string='corde_magique', deco=deco_100),
]


def item_definition(key):
    def mdl(n):
        return {'type': 'minecraft:model', 'model': f'{NS}:item/{n}'}
    return {'model': {
        'type': 'minecraft:condition', 'property': 'minecraft:using_item', 'on_false': mdl(key),
        'on_true': {'type': 'minecraft:range_dispatch', 'property': 'minecraft:use_duration', 'scale': 0.05,
                    'fallback': mdl(key + '_pulling_0'),
                    'entries': [{'threshold': 0.65, 'model': mdl(key + '_pulling_1')}, {'threshold': 0.9, 'model': mdl(key + '_pulling_2')}]}}}


def main():
    for sub in ('textures', 'traction', 'apercu', os.path.join('minecraft', 'assets', NS, 'models', 'item'),
                os.path.join('minecraft', 'assets', NS, 'textures', 'item'), os.path.join('minecraft', 'assets', NS, 'items')):
        os.makedirs(os.path.join(ICI, sub), exist_ok=True)
    mc = os.path.join(ICI, 'minecraft', 'assets', NS)
    bk.write_json(os.path.join(ICI, 'minecraft', 'pack.mcmeta'),
                  {'pack': {'description': 'Arcs Eldoria (6 evolutions)', 'min_format': 97, 'max_format': 97}}, indent=2)
    rest_models, sheets = [], []
    for spec in SPECS:
        models = [build(spec, s) for s in STAGES]
        atlas = bk.build_atlas(models, spec['key'])
        key = spec['key']
        atlas.save(os.path.join(ICI, 'textures', f'{key}.png'))
        atlas.save(os.path.join(mc, 'textures', 'item', f'{key}.png'))
        for m, s in zip(models, STAGES):
            bb = bk.bbmodel(m, atlas, key, NS, DISPLAY, COLORS)
            folder = ICI if s is None else os.path.join(ICI, 'traction')
            bk.write_json(os.path.join(folder, f'{m.key}.bbmodel'), bb)
            bk.write_json(os.path.join(mc, 'models', 'item', f'{m.key}.json'),
                          bk.java_model(m, atlas, f'{NS}:item/{key}', DISPLAY, COLORS,
                                        'Eldoria MMORPG - generer_arcs.py (Blockbench, Java Block/Item)'))
        bk.write_json(os.path.join(mc, 'items', f'{key}.json'), item_definition(key), indent=2)
        bk.views(models[0], atlas, os.path.join(ICI, 'apercu', f'{key}_vues.png'),
                 f"{spec['titre']} — niveau {spec['level']}  ({len(models[0].parts)} cubes au repos)")
        draws = ', '.join(f'{m.draw:.1f}' for m in models[1:])
        print(f"{key:12s} niveau {spec['level']:3d}  {len(models[0].parts):3d} cubes  texture {atlas.width}x{atlas.height}  "
              f"traction : {', '.join(str(len(m.parts)) for m in models[1:])} cubes, allonge {draws} px")
        rest_models.append((models[0], atlas, f"NIV. {spec['level']}", spec['titre']))
        sheets.append((spec, models, atlas))
    bk.lineup(rest_models, os.path.join(ICI, 'apercu', 'evolution.png'), 'ARCS — ÉVOLUTION (BLOCKBENCH, JAVA BLOCK/ITEM)')
    # planche des etapes de traction (vue de face)
    rows = []
    for spec, models, atlas in sheets:
        rows.append([bk.render(m, atlas, 0, 0, 7, bounds=(-6, 27, -33, -(-16.5))) for m in models])
    cw, ch = rows[0][0].size
    W, H = 220 + 4 * (cw + 20), 70 + len(rows) * (ch + 10)
    bg = bk.background(W, H)
    dr = bk.ImageDraw.Draw(bg)
    for j, lab in enumerate(('Repos', 'Traction 1 (pulling_0)', 'Traction 2 (pulling_1)', 'Traction 3 (pulling_2)')):
        dr.text((220 + j * (cw + 20) + 10, 22), lab, font=bk.font(22), fill=(226, 222, 240))
    for i, ((spec, _, _), row) in enumerate(zip(sheets, rows)):
        y = 70 + i * (ch + 10)
        dr.text((20, y + ch // 2 - 30), f"NIV. {spec['level']}", font=bk.font(30), fill=(232, 228, 246))
        dr.text((20, y + ch // 2 + 8), spec['titre'], font=bk.font(16, False), fill=(170, 164, 196))
        for j, im in enumerate(row):
            bg.alpha_composite(im, (220 + j * (cw + 20), y))
    bg.convert('RGB').save(os.path.join(ICI, 'apercu', 'traction.png'))
    print('Apercus : apercu/evolution.png, apercu/traction.png')


if __name__ == '__main__':
    main()
