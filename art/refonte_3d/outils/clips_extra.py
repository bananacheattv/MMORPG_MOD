#!/usr/bin/env python3
"""Clips d'attaque supplementaires, derives du clip attack de chaque mob : le jeu (MobBrain, gardiens) en joue un different par attaque.
attack2 : double coup rapide, le premier en miroir. special : coup lourd, longue anticipation, petit saut, ecrasement.
cast : incantation (si le modele n'en a pas). ultime : le mob se souleve en tremblant, puis frappe a pleine amplitude.
Tous partent de zero et y reviennent, comme attack."""
import math

N = 18


def _val(keys, t):
    if not keys:
        return (0.0, 0.0, 0.0)
    if t <= keys[0][0]:
        return tuple(keys[0][1:4])
    for a, b in zip(keys, keys[1:]):
        if t <= b[0]:
            f = 0 if b[0] == a[0] else (t - a[0]) / (b[0] - a[0])
            return tuple(a[i] + (b[i] - a[i]) * f for i in (1, 2, 3))
    return tuple(keys[-1][1:4])


def _pair(name, bones):
    for a, b in (('_g', '_d'), ('_d', '_g'), ('_-1', '_1'), ('_1', '_-1')):
        if name.endswith(a) and name[:-len(a)] + b in bones:
            return name[:-len(a)] + b
        if a + '_' in name and name.replace(a + '_', b + '_') in bones:
            return name.replace(a + '_', b + '_')
    return name


def _racine(p):
    for n in ('root', 'body'):
        if n in p.bones:
            return n
    return next(iter(p.bones))


def _build(p, src, T, fn, hop=None):
    """fn(u) -> (temps source, amplitude, miroir) ; hop(u) -> (y, rx, rz) ajoutes a l'os racine."""
    tr = {}
    us = [i / N for i in range(N + 1)]
    for bone, ch in src.items():
        for canal, keys in ch.items():
            out = []
            for u in us:
                s, amp, mir = fn(u)
                k = src.get(_pair(bone, p.bones), {}).get(canal, []) if mir else keys
                v = _val(k, s)
                if mir:
                    v = (-v[0], v[1], v[2]) if canal == 'position' else (v[0], -v[1], -v[2])
                lim = 60.0 if canal == 'position' else 175.0
                out.append((min(T, round(T * u, 4)),) + tuple(round(max(-lim, min(lim, x * amp)), 3) for x in v))
            out[0] = (0.0, 0.0, 0.0, 0.0)
            out[-1] = (round(T, 4), 0.0, 0.0, 0.0)
            tr.setdefault(bone, {})[canal] = out
    if hop:
        r = _racine(p)
        for canal, idx in (('position', 0), ('rotation', 1)):
            base = tr.get(r, {}).get(canal)
            out = []
            for i, u in enumerate(us):
                h = hop(u)
                add = (0.0, h[0], 0.0) if idx == 0 else (h[1], 0.0, h[2])
                b = base[i][1:4] if base else (0.0, 0.0, 0.0)
                out.append((min(T, round(T * u, 4)),) + tuple(round(b[j] + add[j], 3) for j in range(3)))
            out[0] = (0.0, 0.0, 0.0, 0.0)
            out[-1] = (round(T, 4), 0.0, 0.0, 0.0)
            tr.setdefault(r, {})[canal] = out
    return tr


def ajoute(p):
    noms = {a['name']: a for a in p.anims}
    if 'attack' not in noms:
        return
    a = noms['attack']
    L, src = a['length'], a['tracks']
    h = 3.0 / max(.2, float(p.meta.get('echelle', 1.0))) if hasattr(p, 'meta') else 3.0
    h = min(h, 14.0)

    def double(u):
        if u < .45:
            return L * u / .45, .8, True
        return L * (u - .45) / .55, 1.1, False

    def lourd(u):
        if u < .5:
            return L * .22 * (u / .5), 1.4, False
        if u < .6:
            return L * .22, 1.4, False
        if u < .7:
            return L * (.22 + .16 * (u - .6) / .1), 1.4, False
        return L * (.38 + .62 * (u - .7) / .3), 1.3, False

    def saut(u):
        if u < .5:
            return h * math.sin(math.pi * .5 * u / .5), 6 * u / .5, 0
        if u < .6:
            return h, 6, 0
        if u < .7:
            f = (u - .6) / .1
            return h * (1 - 1.35 * f), 6 - 16 * f, 0
        f = (u - .7) / .3
        return -.35 * h * (1 - f), -10 * (1 - f), 0

    def fige(u):
        if u < .3:
            return L * .22 * (u / .3), .7, False
        if u < .75:
            return L * .22, .7, False
        return L * .22 * (1 - (u - .75) / .25), .7, False

    def leve(u):
        e = min(1.0, u / .3) if u < .75 else max(0.0, 1 - (u - .75) / .25)
        return h * .7 * e + (.25 * h * math.sin(u * 40) if .3 <= u < .75 else 0), 7 * e, 0

    def rage(u):
        if u < .3:
            return L * .22 * (u / .3), 1.25, False
        if u < .72:
            return L * .22, 1.25, False
        if u < .8:
            return L * (.22 + .16 * (u - .72) / .08), 1.5, False
        return L * (.38 + .62 * (u - .8) / .2), 1.5, False

    def tremble(u):
        if u < .3:
            return h * 1.2 * u / .3, 8 * u / .3, 0
        if u < .72:
            return h * 1.2, 8, 4.5 * math.sin(u * 150)
        if u < .8:
            f = (u - .72) / .08
            return h * 1.2 * (1 - 1.3 * f), 8 - 20 * f, 0
        f = (u - .8) / .2
        return -.36 * h * (1 - f), -12 * (1 - f), 0

    if 'attack2' not in noms:
        T = round(L * 1.15, 2)
        p.animation('attack2', T, _build(p, src, T, double), False)
    if 'special' not in noms:
        T = round(L * 1.6, 2)
        p.animation('special', T, _build(p, src, T, lourd, saut), False)
    if 'cast' not in noms:
        p.animation('cast', 2.0, _build(p, src, 2.0, fige, leve), False)
    if 'ultime' not in noms:
        p.animation('ultime', 2.6, _build(p, src, 2.6, rage, tremble), False)
