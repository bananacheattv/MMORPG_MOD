#!/usr/bin/env python3
"""Cycles d'animation par rig. Valeurs telles qu'affichees dans Blockbench 5 (format de fichier 5.0).

Sens (rotation X, regle de la main droite) : rx > 0 = patte vers l'avant (-z), tete qui se releve, queue qui descend.
Les boucles sont echantillonnees avec 13 cles catmull-rom : premiere et derniere identiques, aucune coupure a la boucle.
L'os du corps pivote en z = 0, a l'aplomb de la selle : le tangage ne deplace pas le cavalier (le jeu ne l'anime pas avec la monture).
"""
import math

TAU = math.tau


def keys(length, fn, n=12):
    """Echantillonne fn(phase 0..1) -> (x, y, z)."""
    return [(length * i / n, *fn(i / n)) for i in range(n + 1)]


def rot(length, x=None, y=None, z=None, n=12):
    return keys(length, lambda p: (x(p) if x else 0, y(p) if y else 0, z(p) if z else 0), n)


def sin(a, f=1, ph=0.0, bias=0.0):
    return lambda p: bias + a * math.sin(TAU * (f * p + ph))


def cos(a, f=1, ph=0.0, bias=0.0):
    return lambda p: bias + a * math.cos(TAU * (f * p + ph))


def pulse(a, center, width):
    """Mouvement bref et lisse autour de center (0..1), pour un battement d'oreille."""
    def f(p):
        d = abs(p - center)
        d = min(d, 1 - d)
        return a * max(0.0, math.cos(math.pi * min(1.0, d / width) / 2)) ** 2
    return f


def combine(*fs):
    return lambda p: sum(f(p) for f in fs)


def leg_cycle(length, amp, phase, knee, bias=0.0, knee_ph=.0, knee_rest=0.0, n=12, reach=.55):
    """Haut de patte : balancier ; bas de patte : flexion pendant le retour vers l'avant (le sabot se leve), et il reste d'aplomb quand la patte se tend devant."""
    def upper(p):
        return bias + amp * math.sin(TAU * (p + phase))

    def lower(p):
        s = math.cos(TAU * (p + phase + knee_ph))
        return -(knee_rest + knee * (max(0.0, s) ** 1.4)) - reach * max(0.0, upper(p))
    return rot(length, x=upper, n=n), rot(length, x=lower, n=n)


def foot_drop(length, amp, phase, hauteur, bias=0.0, n=12):
    """Une patte droite qui pivote remonte son pied de L(1-cos a) : on abaisse la patte d'autant pour garder le sabot au sol en appui."""
    def f(p):
        a = math.radians(bias + amp * math.sin(TAU * (p + phase)))
        return -hauteur * (1 - math.cos(a))
    return keys(length, lambda p: (0, f(p), 0), n)


def quadrupede(rig, walk=.72, run=.46, poids=1.0):
    """rig : {'body', 'head', 'tail', 'legs': [(os_haut, os_bas, phase), ...], options}. Renvoie {nom: (duree, boucle, pistes)}."""
    out = {}
    head, tail, body = rig['head'], rig['tail'], rig['body']
    ears = rig.get('ears', ())
    jaw = rig.get('jaw')
    swing = rig.get('swing', ())
    tail_tip = rig.get('tail_tip')

    # --- repos : respiration, tete qui regarde, oreilles qui battent, queue
    T = rig.get('idle', 3.2)
    tr = {body: {'position': rot(T, y=sin(.16, 1, .25)), 'rotation': rot(T, x=sin(.35, 1, .1))},
          head: {'rotation': rot(T, x=sin(1.6, 1, .35), y=combine(sin(2.2, 1, 0), sin(.8, 2, .3)))},
          tail: {'rotation': rot(T, x=sin(2, 2, 0), z=sin(7, 1, .2))}}
    if jaw:
        tr[jaw] = {'rotation': rot(T, x=sin(1.1, 2, .1, bias=-1.1))}
    for i, e in enumerate(ears):
        s = 1 if i == 0 else -1
        tr[e] = {'rotation': rot(T, x=combine(pulse(-14, .28 + .42 * i, .05)), z=combine(pulse(s * 12, .28 + .42 * i, .05)), n=24)}
    if tail_tip:
        tr[tail_tip] = {'rotation': rot(T, z=sin(9, 1, .05))}
    out['idle'] = (T, True, tr)

    # --- marche : trot net, paires diagonales legerement dephasees
    T = rig.get('walk', walk)
    A = rig.get('walk_amp', 26)
    K = rig.get('walk_knee', 46)
    tr = {body: {'position': rot(T, y=cos(.3 * poids, 2)), 'rotation': rot(T, x=sin(1.1, 2, .12), z=sin(1.4, 1, 0), y=sin(1.0, 1, .25))},
          head: {'rotation': rot(T, x=cos(-2.6, 2, .1), y=sin(2.2, 1, .1), z=sin(-1.2, 1, 0))},
          tail: {'rotation': rot(T, x=cos(4, 2, .1, bias=-4), z=sin(11, 1, .3))}}
    for (up, low, ph, *extra) in rig['legs']:
        a = A * (extra[0] if extra else 1.0)
        u, l = leg_cycle(T, a, ph, K * (extra[1] if len(extra) > 1 else 1.0))
        tr[up] = {'rotation': u, 'position': foot_drop(T, a, ph, rig.get('leg_len', 13.0))}
        if low:
            tr[low] = {'rotation': l}
    if jaw:
        tr[jaw] = {'rotation': rot(T, x=sin(1.5, 2, .2, bias=-1.5))}
    for i, e in enumerate(ears):
        tr[e] = {'rotation': rot(T, x=cos(-6, 2, .3, bias=-5), z=sin(4 * (1 if i == 0 else -1), 1, .1))}
    if tail_tip:
        tr[tail_tip] = {'rotation': rot(T, z=sin(14, 1, .15), x=cos(6, 2, .2))}
    for s in swing:
        tr[s] = {'rotation': rot(T, x=sin(9, 2, .15), z=sin(3, 1, .2))}
    out['walk'] = (T, True, tr)

    # --- galop (clip fourni, a raccorder : le jeu ne joue aujourd'hui que idle, walk et fly)
    T = rig.get('run', run)
    A2 = rig.get('run_amp', 40)
    K2 = rig.get('run_knee', 62)
    gallop = {0: 0.0, 1: .10, 2: .50, 3: .62}
    tr = {body: {'position': rot(T, y=sin(.55 * poids, 1, .1)), 'rotation': rot(T, x=sin(4.2, 1, .15))},
          head: {'rotation': rot(T, x=sin(-4.0, 1, .2, bias=-3), y=sin(1.2, 1, .1))},
          tail: {'rotation': rot(T, x=sin(10, 1, .35, bias=-14), z=sin(7, 1, .3))}}
    for i, (up, low, ph, *extra) in enumerate(rig['legs']):
        u, l = leg_cycle(T, A2, gallop[i % 4], K2)
        tr[up] = {'rotation': u}
        if low:
            tr[low] = {'rotation': l}
    for i, e in enumerate(ears):
        tr[e] = {'rotation': rot(T, x=sin(-5, 1, .2, bias=-16))}
    if tail_tip:
        tr[tail_tip] = {'rotation': rot(T, x=sin(12, 1, .4))}
    for s in swing:
        tr[s] = {'rotation': rot(T, x=sin(16, 1, .25))}
    out['run'] = (T, True, tr)

    # --- saut (clip fourni, a raccorder) : appel, extension, rassemblement, reception amortie
    T = rig.get('jump', .85)

    def spline(pts):
        def f(p):
            for (a, va), (b, vb) in zip(pts, pts[1:]):
                if a <= p <= b:
                    u = (p - a) / max(1e-6, b - a)
                    u = u * u * (3 - 2 * u)
                    return va + (vb - va) * u
            return pts[-1][1]
        return f
    pitch = spline([(0, 0), (.14, -6), (.3, 14), (.55, 4), (.78, -9), (.9, -2), (1, 0)])
    bob = spline([(0, 0), (.14, -1.1), (.3, .3), (.78, .1), (.87, -1.0), (1, 0)])
    fr_u = spline([(0, 0), (.14, -12), (.3, 44), (.55, 30), (.78, 18), (.9, -3), (1, 0)])
    fr_l = spline([(0, 0), (.14, -18), (.3, -58), (.55, -40), (.78, -6), (.9, -14), (1, 0)])
    bk_u = spline([(0, 0), (.14, 16), (.3, -40), (.55, -20), (.78, 14), (.9, 6), (1, 0)])
    bk_l = spline([(0, 0), (.14, -30), (.3, -4), (.55, -40), (.78, -24), (.9, -20), (1, 0)])
    tr = {body: {'rotation': rot(T, x=pitch, n=16), 'position': rot(T, y=bob, n=16)},
          head: {'rotation': rot(T, x=lambda p: -pitch(p) * .55, n=16)},
          tail: {'rotation': rot(T, x=lambda p: -pitch(p) * 1.2 - 8 * math.sin(math.pi * p), n=16)}}
    for i, (up, low, ph, *extra) in enumerate(rig['legs']):
        front = i < 2
        tr[up] = {'rotation': rot(T, x=fr_u if front else bk_u, n=16)}
        if low:
            tr[low] = {'rotation': rot(T, x=fr_l if front else bk_l, n=16)}
    for e in ears:
        tr[e] = {'rotation': rot(T, x=lambda p: -18 * math.sin(math.pi * p), n=16)}
    out['jump'] = (T, False, tr)
    return out


def add_all(proj, clips):
    for name, (T, loop, tr) in clips.items():
        proj.animation(name, T, tr, loop)


def octopode(rig, walk=.5):
    """Araignee, scorpion : pattes par paires alternees (os historiques leg0l...leg3r), un seul segment articule par os + bas de patte. Corps tres stable."""
    out = {}
    T = rig.get('idle', 3.0)
    tr = {rig['body']: {'position': rot(T, y=sin(.12, 1, .2))}}
    if rig.get('head'):
        tr[rig['head']] = {'rotation': rot(T, y=sin(2.0, 1, 0), x=sin(1.0, 2, .2))}
    if rig.get('tail'):
        tr[rig['tail']] = {'rotation': rot(T, x=sin(2.5, 1, .1), z=sin(2.0, 1, .35))}
    for c, s in rig.get('claws', ()):
        tr[c] = {'rotation': rot(T, y=sin(s * 3, 1, .2), x=sin(1.5, 2, .1))}
    if rig.get('stinger'):
        tr[rig['stinger']] = {'rotation': rot(T, x=sin(5, 1, .3))}
    out['idle'] = (T, True, tr)
    T = rig.get('walk', walk)
    tr = {rig['body']: {'position': rot(T, y=cos(.18, 2)), 'rotation': rot(T, z=sin(.8, 1, 0))}}
    if rig.get('head'):
        tr[rig['head']] = {'rotation': rot(T, y=sin(3, 1, .1))}
    if rig.get('tail'):
        tr[rig['tail']] = {'rotation': rot(T, x=sin(3, 2, .2), z=sin(5, 1, .25))}
    for c, s in rig.get('claws', ()):
        tr[c] = {'rotation': rot(T, x=sin(5, 2, .3), y=sin(s * 5, 1, .2))}
    for i, (up, low, side) in enumerate(rig['legs']):
        ph = (i % 2) * .5 + (.5 if side < 0 else 0)
        # rotation du haut de patte autour de Y : la patte balaie vers l'avant et l'arriere ; le bas se leve pendant le retour
        tr[up] = {'rotation': rot(T, y=lambda p, ph=ph, s=side: s * 16 * math.sin(TAU * (p + ph)), z=lambda p, ph=ph, s=side: s * 9 * max(0.0, math.cos(TAU * (p + ph))))}
        if low:
            tr[low] = {'rotation': rot(T, z=lambda p, ph=ph, s=side: -s * 10 * max(0.0, math.cos(TAU * (p + ph))))}
    out['walk'] = (T, True, tr)
    return out


def ajoute_vol(clips, rig, T=.7):
    """Vol : ailes en deux segments avec retard du bout, pattes repliees, corps qui ondule."""
    wl, wr = rig['wings']
    tips = rig.get('wing_tips', ())
    tr = {rig['body']: {'position': rot(T, y=sin(.9, 1, .45)), 'rotation': rot(T, x=sin(2.2, 1, .1, bias=-5))},
          rig['head']: {'rotation': rot(T, x=sin(-2.5, 1, .1, bias=6))},
          rig['tail']: {'rotation': rot(T, x=sin(5, 1, .3, bias=6))},
          wl: {'rotation': rot(T, z=sin(-30, 1, 0, bias=-6))},
          wr: {'rotation': rot(T, z=sin(30, 1, 0, bias=6))}}
    if len(tips) == 2:
        tr[tips[0]] = {'rotation': rot(T, z=sin(-22, 1, -.12, bias=-8))}
        tr[tips[1]] = {'rotation': rot(T, z=sin(22, 1, -.12, bias=8))}
    for i, (up, low, ph, *extra) in enumerate(rig['legs']):
        front = i < 2
        tr[up] = {'rotation': rot(T, x=sin(3, 1, ph, bias=-42 if not front else 38))}
        if low:
            tr[low] = {'rotation': rot(T, x=sin(3, 1, ph + .1, bias=-50))}
    clips['fly'] = (T, True, tr)
    # les ailes restent repliees et respirent au repos et a la marche : le modele est construit ailes ouvertes, le repli est porte par les clips
    fy, fz, fx = rig.get('wing_fold', (0, 0, 0))
    tip_y, tip_z = rig.get('tip_fold', (0, 0))
    for name in ('idle', 'walk', 'run', 'jump'):
        if name not in clips:
            continue
        L = clips[name][0]
        n = 12
        clips[name][2][wl] = {'rotation': keys(L, lambda p: (fx, fy, fz + 2.5 * math.sin(TAU * p)), n)}
        clips[name][2][wr] = {'rotation': keys(L, lambda p: (fx, -fy, -fz - 2.5 * math.sin(TAU * p)), n)}
        if len(tips) == 2:
            clips[name][2][tips[0]] = {'rotation': keys(L, lambda p: (0, tip_y, tip_z), 2)}
            clips[name][2][tips[1]] = {'rotation': keys(L, lambda p: (0, -tip_y, -tip_z), 2)}
    return clips


def ajoute_chaine(clips, os_liste, ax=(1.2, 2.5, 4.0), az=(2.0, 3.5, 6.0), retard=.1):
    """Cou, queue ou trompe articules : chaque os suit le precedent avec un leger retard. ax et az : amplitude (repos, marche, galop) de la bascule et du balancement."""
    idx = {'idle': 0, 'walk': 1, 'run': 2, 'jump': 2, 'fly': 1}
    for name, (T, loop, tr) in clips.items():
        i = idx.get(name, 1)
        for n, b in enumerate(os_liste):
            if b not in tr:
                tr[b] = {'rotation': rot(T, x=sin(ax[i], 1, retard * n), z=sin(az[i], 1, retard * n + .25))}
    return clips
