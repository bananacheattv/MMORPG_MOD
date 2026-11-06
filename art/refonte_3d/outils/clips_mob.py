#!/usr/bin/env python3
"""Clips des mobs : repos, marche (ou vol, ou reptation), attaque, mort, et lancer de sort pour les bestiaux classiques.
Valeurs telles qu'affichees dans Blockbench 5. Rappels : rx > 0 = membre pendant vers l'avant (-z), museau qui se leve ; rx < 0 = machoire qui s'ouvre, buste qui se penche en avant.
ry > 0 = cote +x vers l'avant. rz > 0 = membre +x vers l'exterieur. Positions en texels de jeu : la reduction a l'echelle du mob les divise en meme temps que la geometrie.
Le jeu additionne attaque et mort a la pose courante : tous ces clips partent de zero et y reviennent, sauf la mort qui reste sur sa derniere pose."""
import math

from animations import TAU, combine, cos, keys, leg_cycle, octopode, pulse, quadrupede, rot, sin

P_ATT = (0, .22, .38, .52, .75, 1)
P_MORT = (0, .25, .6, 1)


def ev(T, phases, x=None, y=None, z=None):
    return [(T * phases[i], x[i] if x else 0, y[i] if y else 0, z[i] if z else 0) for i in range(len(phases))]


def _r(T, ph, x=None, y=None, z=None):
    return {'rotation': ev(T, ph, x, y, z)}


def _ajoute(tr, bones, os_, ch):
    """Ajoute une piste seulement si l'os existe : le meme cycle sert aux modeles avec ou sans avant-bras."""
    if os_ in bones:
        tr.setdefault(os_, {}).update(ch)


def _fin(p, nom, T, tr, boucle=True):
    p.animation(nom, T, {k: v for k, v in tr.items() if k in p.bones}, boucle)


# ------------------------------------------------------------------ bipedes
def bipede(p, idle=3.0, walk=1.2, attack=1.2, style='epee', amp=26, genou=40, bras=22, jambe=18, cape=(), jaw=None, marche='walk', queue=(), gr=1.0, cast=False, oreilles=(), haleine=.4, arme=None, posture=0.0, flotte=0.0):
    """posture : bascule du buste en marche (negatif = penche en avant)."""
    B = p.bones
    T = idle
    tr = {'body': {'position': rot(T, y=sin((flotte or haleine) * gr, 1, .25)), 'rotation': rot(T, x=sin(.8, 1, .1))},
          'head': {'rotation': rot(T, x=sin(1.4, 1, .35), y=combine(sin(4, 1, 0), sin(1.2, 2, .3)))},
          'arm_-1': {'rotation': rot(T, x=sin(1.6, 1, .1), z=sin(1.2, 1, .3))},
          'arm_1': {'rotation': rot(T, x=sin(-1.6, 1, .1), z=sin(-1.2, 1, .3))},
          'avbras_-1': {'rotation': rot(T, x=sin(2, 1, .2))},
          'avbras_1': {'rotation': rot(T, x=sin(-2, 1, .2))}}
    if jaw:
        tr[jaw] = {'rotation': rot(T, x=sin(1.6, 2, .1, bias=-1.6))}
    for i, c in enumerate(cape):
        tr[c] = {'rotation': rot(T, x=sin(2.4, 1, .1 * i, bias=-1), z=sin(1.2, 1, .3 + .1 * i))}
    for i, c in enumerate(queue):
        tr[c] = {'rotation': rot(T, x=sin(2, 1, .1 * i), y=sin(5, 1, .25 + .1 * i))}
    for i, e in enumerate(oreilles):
        tr[e] = {'rotation': rot(T, x=pulse(-10, .3 + .4 * i, .05), n=24)}
    _fin(p, 'idle', T, tr)

    T = walk
    tr = {}
    A = math.radians(amp)
    tr['root'] = {'position': keys(T, lambda q: (0, -jambe * (1 - math.cos(A * math.sin(TAU * q))), 0))}
    tr['body'] = {'rotation': rot(T, x=sin(1.2, 2, .1, bias=posture), y=sin(3.5, 1, .0), z=sin(1.6, 1, .25))}
    if flotte:
        del tr['root']
        tr['body'] = {'position': rot(T, y=sin(flotte * gr, 1, .2)), 'rotation': rot(T, x=sin(1.5, 1, .1, bias=posture - 7), y=sin(3, 1, 0), z=sin(2, 1, .25))}
    tr['head'] = {'rotation': rot(T, x=cos(1.6, 2, .1, bias=-posture * .6), y=sin(-2.5, 1, 0), z=sin(-1, 1, .25))}
    for (leg, tib, ar, av, ph, s) in (('leg_-1', 'tibia_-1', 'arm_-1', 'avbras_-1', 0.0, 1), ('leg_1', 'tibia_1', 'arm_1', 'avbras_1', .5, -1)):
        u, l = leg_cycle(T, amp, ph, genou, reach=.25)
        tr[leg] = {'rotation': u}
        tr[tib] = {'rotation': l}
        tr[ar] = {'rotation': rot(T, x=sin(-bras, 1, ph, bias=2))}
        tr[av] = {'rotation': keys(T, lambda q, ph=ph: (6 + 7 * max(0.0, math.sin(TAU * (q + ph + .5))), 0, 0))}
    if jaw:
        tr[jaw] = {'rotation': rot(T, x=sin(1.5, 2, .2, bias=-1.5))}
    for i, c in enumerate(cape):
        tr[c] = {'rotation': rot(T, x=sin(5, 2, .1 * i, bias=-9), z=sin(2.5, 1, .2 + .1 * i))}
    for i, c in enumerate(queue):
        tr[c] = {'rotation': rot(T, x=sin(3, 2, .1 * i), y=sin(10, 1, .2 + .1 * i))}
    _fin(p, marche, T, tr)

    T = attack
    PH = P_ATT
    tr = {}
    if style == 'epee':
        tr['arm_-1'] = _r(T, PH, x=(0, 135, 25, 15, 8, 0), z=(0, 8, -6, -4, 0, 0))
        tr['avbras_-1'] = _r(T, PH, x=(0, 35, 0, 8, 4, 0))
        tr['arm_1'] = _r(T, PH, x=(0, -18, 20, 12, 4, 0), z=(0, -6, -2, 0, 0, 0))
        tr['body'] = _r(T, PH, x=(0, 5, -13, -9, -3, 0), y=(0, -20, 22, 14, 4, 0))
        tr['head'] = _r(T, PH, x=(0, -4, 6, 4, 1, 0), y=(0, 12, -14, -8, -2, 0))
        tr['leg_-1'] = _r(T, PH, x=(0, -8, 14, 10, 3, 0))
        tr['leg_1'] = _r(T, PH, x=(0, 8, -10, -6, -2, 0))
        tr['root'] = {'position': ev(T, PH, y=tuple(v * gr for v in (0, .3, -.8, -.6, -.2, 0)), z=tuple(v * gr for v in (0, 1, -2, -2, -.5, 0)))}
    elif style == 'griffes':
        for a, v, s in (('arm_-1', 'avbras_-1', 1), ('arm_1', 'avbras_1', -1)):
            tr[a] = _r(T, PH, x=(0, 130, 28, 16, 6, 0), z=(0, s * 12, -s * 8, -s * 4, 0, 0))
            tr[v] = _r(T, PH, x=(0, 30, 0, 6, 3, 0))
        tr['body'] = _r(T, PH, x=(0, 8, -16, -10, -3, 0))
        tr['head'] = _r(T, PH, x=(0, -6, 8, 5, 1, 0))
        tr['leg_-1'] = _r(T, PH, x=(0, -6, 12, 8, 2, 0))
        tr['leg_1'] = _r(T, PH, x=(0, 6, -9, -5, -1, 0))
        tr['root'] = {'position': ev(T, PH, y=tuple(v * gr for v in (0, .5, -1, -.8, -.2, 0)), z=tuple(v * gr for v in (0, 1, -2.5, -2.5, -.5, 0)))}
    elif style == 'estoc':
        tr['arm_-1'] = _r(T, PH, x=(0, 40, 95, 85, 30, 0))
        tr['avbras_-1'] = _r(T, PH, x=(0, 60, 0, 5, 20, 0))
        tr['arm_1'] = _r(T, PH, x=(0, 18, -12, -8, 2, 0))
        tr['body'] = _r(T, PH, x=(0, 3, -10, -8, -2, 0), y=(0, -20, 24, 16, 4, 0))
        tr['head'] = _r(T, PH, y=(0, 12, -16, -10, -2, 0))
        tr['leg_-1'] = _r(T, PH, x=(0, -10, 16, 12, 3, 0))
        tr['leg_1'] = _r(T, PH, x=(0, 10, -12, -8, -2, 0))
        tr['root'] = {'position': ev(T, PH, z=tuple(v * gr for v in (0, 1.5, -3, -3, -1, 0)))}
    elif style == 'arc':
        tr['arm_1'] = _r(T, PH, x=(0, 70, 85, 85, 40, 0), z=(0, 0, -4, -4, 0, 0))
        tr['arm_-1'] = _r(T, PH, x=(0, 85, 80, 60, 20, 0), y=(0, -8, -14, 0, 0, 0))
        tr['avbras_-1'] = _r(T, PH, x=(0, 70, 95, 0, 0, 0))
        tr['body'] = _r(T, PH, y=(0, 12, 16, 10, 3, 0))
        tr['head'] = _r(T, PH, y=(0, -10, -14, -8, -2, 0))
    elif style == 'sort':
        tr['arm_-1'] = _r(T, PH, x=(0, 100, 160, 150, 60, 0), z=(0, 10, 14, 14, 5, 0))
        tr['avbras_-1'] = _r(T, PH, x=(0, 20, 0, 0, 5, 0))
        tr['arm_1'] = _r(T, PH, x=(0, 30, -14, -10, 4, 0), z=(0, -14, -18, -12, -3, 0))
        tr['body'] = _r(T, PH, x=(0, 6, 10, -6, -2, 0))
        tr['head'] = _r(T, PH, x=(0, 8, 14, -4, 0, 0))
    elif style == 'poing':
        tr['arm_-1'] = _r(T, PH, x=(0, 110, 8, -4, 4, 0), z=(0, 22, -8, -4, 0, 0))
        tr['avbras_-1'] = _r(T, PH, x=(0, 50, 10, 14, 6, 0))
        tr['arm_1'] = _r(T, PH, x=(0, -20, 28, 18, 5, 0))
        tr['body'] = _r(T, PH, x=(0, 8, -18, -14, -4, 0), y=(0, -24, 20, 12, 3, 0))
        tr['head'] = _r(T, PH, x=(0, -6, 10, 7, 2, 0), y=(0, 14, -12, -6, -1, 0))
        tr['leg_-1'] = _r(T, PH, x=(0, -6, 12, 9, 2, 0))
        tr['leg_1'] = _r(T, PH, x=(0, 6, -9, -6, -1, 0))
        tr['root'] = {'position': ev(T, PH, y=tuple(v * gr for v in (0, .6, -1.4, -1.2, -.3, 0)))}
    else:
        raise ValueError(style)
    if jaw:
        tr[jaw] = _r(T, PH, x=(0, -8, -22, -14, -3, 0))
    for i, c in enumerate(cape):
        tr[c] = _r(T, PH, x=(0, -4, -22, -18, -6, 0))
    if arme:
        tr[arme] = _r(T, PH, x=(0, 18, -8, -4, 0, 0))
    _fin(p, 'attack', T, tr, False)
    mort_bipede(p, jaw=jaw, cape=cape)
    if cast:
        cast_bipede(p)


def mort_bipede(p, jaw=None, cape=(), T=1.0):
    """Le jeu couche deja le mob en une seconde : ici le corps s'affaisse et les membres retombent."""
    PH = P_MORT
    tr = {'body': _r(T, PH, x=(0, 8, -14, -22)),
          'head': _r(T, PH, x=(0, 14, -18, -30), z=(0, 0, 6, 8)),
          'arm_-1': _r(T, PH, x=(0, -14, 10, 18), z=(0, 18, 10, 6)),
          'arm_1': _r(T, PH, x=(0, -14, 10, 18), z=(0, -18, -10, -6)),
          'avbras_-1': _r(T, PH, x=(0, 6, 10, 14)),
          'avbras_1': _r(T, PH, x=(0, 6, 10, 14)),
          'leg_-1': _r(T, PH, x=(0, 6, 14, 20)),
          'leg_1': _r(T, PH, x=(0, -6, -2, 8)),
          'tibia_-1': _r(T, PH, x=(0, -4, -18, -30)),
          'tibia_1': _r(T, PH, x=(0, -2, -12, -20))}
    if jaw:
        tr[jaw] = _r(T, PH, x=(0, -18, -14, -10))
    for c in cape:
        tr[c] = _r(T, PH, x=(0, -14, 6, 12))
    _fin(p, 'death', T, tr, False)


def cast_bipede(p, T=2.4):
    PH = (0, .3, .75, 1)
    tr = {'arm_-1': _r(T, PH, x=(0, 150, 150, 0), z=(0, 14, 14, 0)),
          'arm_1': _r(T, PH, x=(0, 150, 150, 0), z=(0, -14, -14, 0)),
          'body': _r(T, PH, x=(0, 6, 6, 0)),
          'head': _r(T, PH, x=(0, 10, 10, 0))}
    _fin(p, 'cast', T, tr, False)


# ------------------------------------------------------------------ quadrupedes
def quadrupede_mob(p, legs, body='body', head='head', tail='tail', tail_tip=None, jaw=None, ears=(), idle=3.0, walk=1.2, attack=1.2, amp=26, knee=44, leg_len=13.0, poids=1.0, marche='walk', cou=(), swing=(), gr=1.0, cast=False, T_mort=1.0):
    rig = {'body': body, 'head': head, 'tail': tail, 'tail_tip': tail_tip, 'jaw': jaw, 'ears': ears, 'legs': legs, 'idle': idle, 'walk': walk, 'walk_amp': amp, 'walk_knee': knee, 'leg_len': leg_len, 'swing': swing}
    c = quadrupede(rig, poids=poids)
    for nom in ('idle', 'walk'):
        T, boucle, tr = c[nom]
        for i, b in enumerate(cou):
            tr[b] = {'rotation': rot(T, x=sin(1.5, 2 if nom == 'walk' else 1, .08 * i), y=sin(2.5, 1, .1 * i))}
        _fin(p, 'idle' if nom == 'idle' else marche, T, tr)
    T = attack
    PH = P_ATT
    tr = {body: {'rotation': ev(T, PH, x=(0, 5, -6, -5, -2, 0)), 'position': ev(T, PH, y=tuple(v * gr for v in (0, -.5, .4, .3, 0, 0)), z=tuple(v * gr for v in (0, 2, -4, -4, -1, 0)))},
          head: _r(T, PH, x=(0, 14, -16, -10, -3, 0), y=(0, -6, 6, 3, 1, 0))}
    if jaw:
        tr[jaw] = _r(T, PH, x=(0, -6, -26, -10, -2, 0))
    for i, l in enumerate(legs):
        up = l[0]
        tr[up] = _r(T, PH, x=(0, -14, 22, 14, 4, 0)) if i < 2 else _r(T, PH, x=(0, 8, -14, -10, -2, 0))
    if tail in p.bones:
        tr[tail] = _r(T, PH, x=(0, -8, 8, 4, 1, 0), y=(0, 6, -6, -3, 0, 0))
    for b in cou:
        tr[b] = _r(T, PH, x=(0, 6, -8, -5, -1, 0))
    for e in ears:
        tr[e] = _r(T, PH, x=(0, -18, -22, -14, -3, 0))
    _fin(p, 'attack', T, tr, False)
    T = T_mort
    PH = (0, .4, 1)
    tr = {body: _r(T, PH, x=(0, 6, 12)), head: _r(T, PH, x=(0, -14, -28))}
    if jaw:
        tr[jaw] = _r(T, PH, x=(0, -18, -12))
    for i, l in enumerate(legs):
        tr[l[0]] = _r(T, PH, x=(0, 12, 22) if i < 2 else (0, -10, -20))
        if l[1]:
            tr[l[1]] = _r(T, PH, x=(0, -20, -38))
    if tail in p.bones:
        tr[tail] = _r(T, PH, x=(0, -8, -18))
    _fin(p, 'death', T, tr, False)
    if cast:
        T = 2.4
        PH = (0, .3, .75, 1)
        tr = {head: _r(T, PH, x=(0, 18, 18, 0)), body: _r(T, PH, x=(0, 5, 5, 0))}
        if jaw:
            tr[jaw] = _r(T, PH, x=(0, -20, -20, 0))
        _fin(p, 'cast', T, tr, False)


# ------------------------------------------------------------------ pattes multiples
def octo_mob(p, legs, body='body', head=None, tail=None, claws=(), stinger=None, chaine=(), idle=3.0, walk=1.2, attack=1.2, gr=1.0, jaw=(), cast=False, T_mort=1.0):
    """legs : [(os_haut, os_bas, +1 ou -1)]. chaine : os de la queue, du premier au dernier, pour le coup de dard."""
    rig = {'body': body, 'head': head, 'tail': tail, 'claws': claws, 'stinger': stinger, 'legs': legs, 'idle': idle, 'walk': walk}
    c = octopode(rig, walk=walk)
    for nom in ('idle', 'walk'):
        T, boucle, tr = c[nom]
        _fin(p, nom, T, tr)
    T = attack
    PH = P_ATT
    tr = {body: {'position': ev(T, PH, y=tuple(v * gr for v in (0, .8, -.4, -.3, 0, 0)), z=tuple(v * gr for v in (0, 1.5, -3, -3, -1, 0))), 'rotation': ev(T, PH, x=(0, 7, -5, -4, -1, 0))}}
    if head:
        tr[head] = _r(T, PH, x=(0, 12, -14, -8, -2, 0))
    for b, s in claws:
        tr[b] = _r(T, PH, x=(0, 18, -8, -4, 0, 0), y=(0, s * 24, -s * 18, -s * 8, 0, 0))
    for i, b in enumerate(chaine):
        tr[b] = _r(T, PH, x=(0, -10, 22 - i * 2, 12, 3, 0))
    if stinger and stinger not in chaine:
        tr[stinger] = _r(T, PH, x=(0, -14, 30, 16, 3, 0))
    for i, l in enumerate(legs[:2]):
        tr[l[0]] = _r(T, PH, x=(0, 18, -16, -8, -2, 0))
    for b, s in jaw:
        tr[b] = _r(T, PH, y=(0, s * 14, -s * 12, -s * 5, 0, 0))
    _fin(p, 'attack', T, tr, False)
    T = T_mort
    PH = (0, .4, 1)
    tr = {body: {'position': ev(T, PH, y=tuple(v * gr for v in (0, -1, -2)))}}
    for up, low, s in legs:
        tr[up] = _r(T, PH, z=(0, -s * 14, -s * 28))
        if low:
            tr[low] = _r(T, PH, z=(0, s * 24, s * 46))
    for i, b in enumerate(chaine):
        tr[b] = _r(T, PH, x=(0, 6, 10))
    _fin(p, 'death', T, tr, False)
    if cast:
        T = 2.4
        PH = (0, .3, .75, 1)
        tr = {body: {'position': ev(T, PH, y=tuple(v * gr for v in (0, 1.5, 1.5, 0))), 'rotation': ev(T, PH, x=(0, 8, 8, 0))}}
        _fin(p, 'cast', T, tr, False)


# ------------------------------------------------------------------ volants
def volant(p, ailes, body='body', head='head', queue=(), pattes=(), idle=3.0, vol=1.5, attack=1.2, battement=32, hover=1.2, cadence=2, jaw=None, gr=1.0, attaque='piqué', nom_vol='fly', battement_idle=None, axe='z'):
    """ailes : [(os_+x, os_-x, retard)], du haut vers le bout. Les ailes battent au repos aussi : le mob ne se pose jamais. cadence : battements par cycle."""
    bi = battement if battement_idle is None else battement_idle

    def piste(T, amp, k):
        tr = {}
        for g, d, ret in ailes:
            a = amp * (1 + .35 * bool(ret))
            b_ = 4 if axe == 'z' else -8
            tr[g] = {'rotation': rot(T, **{axe: sin(a, k, -ret, bias=b_)}, n=16 if k > 1 else 12)}
            tr[d] = {'rotation': rot(T, **{axe: sin(-a, k, -ret, bias=-b_)}, n=16 if k > 1 else 12)}
        return tr
    T = idle
    tr = piste(T, bi, cadence * 2)
    tr[body] = {'position': rot(T, y=sin(hover * gr, 1, .2)), 'rotation': rot(T, x=sin(1.5, 1, .1))}
    if head in p.bones:
        tr[head] = {'rotation': rot(T, x=sin(1.5, 1, .35), y=sin(5, 1, 0))}
    for i, b in enumerate(queue):
        tr[b] = {'rotation': rot(T, x=sin(3, 1, .1 * i), y=sin(4, 1, .2 + .1 * i))}
    for i, b in enumerate(pattes):
        tr[b] = {'rotation': rot(T, x=sin(3, 1, .15 * i, bias=0))}
    _fin(p, 'idle', T, tr)
    T = vol
    tr = piste(T, battement, cadence)
    tr[body] = {'position': rot(T, y=sin(hover * gr, 1, .45)), 'rotation': rot(T, x=sin(2.2, 1, .1, bias=-9))}
    if head in p.bones:
        tr[head] = {'rotation': rot(T, x=sin(-1.5, 1, .1, bias=7))}
    for i, b in enumerate(queue):
        tr[b] = {'rotation': rot(T, x=sin(5, 1, .1 * i, bias=4), y=sin(5, 1, .2 + .1 * i))}
    for i, b in enumerate(pattes):
        tr[b] = {'rotation': rot(T, x=sin(4, 1, .15 * i, bias=-22))}
    _fin(p, nom_vol, T, tr)
    T = attack
    PH = P_ATT
    tr = {body: {'rotation': ev(T, PH, x=(0, 14, -24, -14, -4, 0)), 'position': ev(T, PH, y=tuple(v * gr for v in (0, 2.5, -2, -1.5, 0, 0)), z=tuple(v * gr for v in (0, 2, -5, -4, -1, 0)))}}
    if head in p.bones:
        tr[head] = _r(T, PH, x=(0, 8, -8, -4, -1, 0))
    if jaw:
        tr[jaw] = _r(T, PH, x=(0, -8, -24, -12, -2, 0))
    for g, d, ret in ailes:
        tr[g] = _r(T, PH, **{axe: (0, 26, -22, -8, 2, 0)})
        tr[d] = _r(T, PH, **{axe: (0, -26, 22, 8, -2, 0)})
    for i, b in enumerate(pattes):
        tr[b] = _r(T, PH, x=(0, 20, -30, -20, -4, 0))
    _fin(p, 'attack', T, tr, False)
    T = 1.0
    PH = (0, .4, 1)
    tr = {body: _r(T, PH, x=(0, -10, -20))}
    if head in p.bones:
        tr[head] = _r(T, PH, x=(0, -14, -24))
    for g, d, ret in ailes:
        tr[g] = _r(T, PH, **{axe: (0, 14, -30)})
        tr[d] = _r(T, PH, **{axe: (0, -14, 30)})
    _fin(p, 'death', T, tr, False)


# ------------------------------------------------------------------ serpents, vers, limaces
def serpent(p, chaine, head='head', jaw=None, idle=3.0, move=1.5, attack=1.2, nom_move='slither', oscille=12, oscille_idle=3.0, axe='y', leve=(), gr=1.0, retard=.12, extra_idle=None):
    """chaine : os du corps de la nuque a la queue. leve : os qui redressent le corps dans l'attaque."""
    T = idle
    tr = {}
    for i, b in enumerate(chaine):
        tr[b] = {'rotation': rot(T, **{axe: sin(oscille_idle, 1, retard * i)}, x=sin(1.0, 1, retard * i + .25))}
    if head in p.bones and head not in tr:
        tr[head] = {'rotation': rot(T, x=sin(2, 1, .3), y=sin(6, 1, 0))}
    if jaw:
        tr[jaw] = {'rotation': rot(T, x=sin(2, 2, .1, bias=-2))}
    _fin(p, 'idle', T, tr)
    T = move
    tr = {}
    for i, b in enumerate(chaine):
        tr[b] = {'rotation': rot(T, **{axe: sin(oscille, 1, retard * i)})}
    if head in p.bones and head not in tr:
        tr[head] = {'rotation': rot(T, **{axe: sin(-oscille * .4, 1, 0)})}
    if jaw:
        tr[jaw] = {'rotation': rot(T, x=sin(2, 2, .2, bias=-2))}
    _fin(p, nom_move, T, tr)
    T = attack
    PH = P_ATT
    tr = {}
    for i, b in enumerate(leve or chaine[:3]):
        tr[b] = _r(T, PH, x=(0, 14, -18, -12, -3, 0))
    if head in p.bones and head not in tr:
        tr[head] = _r(T, PH, x=(0, 16, -14, -8, -2, 0))
    if jaw:
        tr[jaw] = _r(T, PH, x=(0, -10, -34, -18, -3, 0))
    _fin(p, 'attack', T, tr, False)
    T = 1.0
    PH = (0, .5, 1)
    tr = {}
    for i, b in enumerate(chaine):
        s = 1 if i % 2 else -1
        tr[b] = _r(T, PH, x=(0, -5, -8), y=(0, s * 8, s * 14))
    if jaw:
        tr[jaw] = _r(T, PH, x=(0, -24, -18))
    _fin(p, 'death', T, tr, False)
