#!/usr/bin/env python3
"""Clips ecrits a la main pour chaque mob : quelques poses cles, os par os, et non plus un cycle partage.
Sens (Blockbench 5) : tete vers -z. rx > 0 : membre pendant qui part vers l'avant, museau qui se leve. rx < 0 : machoire qui s'ouvre, buste qui penche en avant. ry > 0 : cote +x vers l'avant. rz > 0 : membre +x vers l'exterieur.
Positions en texels de jeu : la reduction a l'echelle du mob les divise avec la geometrie.
Les sorts (ImportedSpells) tombent 6 ticks apres le debut du geste : dans un clip « cast », le lancer est pose a 0,30 s."""
import math

from animations import TAU, combine, cos, keys, leg_cycle, pulse, rot, sin

LANCER = .30


def clip(p, nom, T, poses, boucle=False, retour=True):
    """poses : [(temps en s, {os: (rx, ry, rz)})] ; 'os+' pour une position (dx, dy, dz), 'os*' pour une echelle (1 = neutre). Chaque piste part de zero a t = 0 si rien n'est donne, et revient a zero a t = T (retour=False : garde sa derniere pose, pour la mort)."""
    tr = {}
    for t, pose in poses:
        for cle, v in pose.items():
            canal = 'position' if cle.endswith('+') else 'scale' if cle.endswith('*') else 'rotation'
            tr.setdefault(cle.rstrip('+*'), {}).setdefault(canal, []).append((round(float(t), 4), *v))
    for ch in tr.values():
        for canal, ks in ch.items():
            ks.sort()
            neutre = (1, 1, 1) if canal == 'scale' else (0, 0, 0)
            if ks[0][0] > 0:
                ks.insert(0, (0.0, *neutre))
            if ks[-1][0] < T:
                ks.append((float(T), *neutre) if retour or boucle else (float(T), *ks[-1][1:]))
    p.animation(nom, T, tr, boucle)


def cycle(p, nom, T, pistes):
    """Boucle faite de fonctions periodiques : pistes = {os: {'rotation': [...], 'position': [...]}} (voir R, P)."""
    p.animation(nom, T, pistes, True)


def R(T, x=None, y=None, z=None, n=16):
    return {'rotation': rot(T, x, y, z, n)}


def P(T, x=None, y=None, z=None, n=16):
    return {'position': rot(T, x, y, z, n)}


def RP(T, r, pos):
    """Rotation et position sur le meme os : r et pos sont des dict x, y, z de fonctions."""
    return {'rotation': rot(T, n=16, **r), 'position': rot(T, n=16, **pos)}


def jambe(T, amp, ph, genou, reach=.25, bias=0.0):
    """(piste haut, piste bas) d'une jambe a deux segments."""
    u, l = leg_cycle(T, amp, ph, genou, bias=bias, n=16, reach=reach)
    return {'rotation': u}, {'rotation': l}


def appui(T, amp, longueur, double=True):
    """Descente du bassin pour garder le pied au sol quand la jambe s'ecarte de la verticale (fonction de la phase)."""
    A = math.radians(amp)
    return lambda q: -longueur * (1 - math.cos(A * math.sin(TAU * q)))
