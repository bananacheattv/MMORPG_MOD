# -*- coding: utf-8 -*-
"""Animations d'attaque propres a chacun des 30 monstres 3D importes (art/imported_models/mobs).

Le pack d'origine donnait la meme attaque generique a tous (bras qui balancent, tete qui hoche). Ici chaque monstre a son coup, pense pour
son anatomie : coup de boule du sanglier, langue du crapaud, dard du scorpion, morsures des tetes de l'hydre, frappe en piquet du golem...
Les os sont designes par leur nom dans le projet Blockbench ; import_bbmodels.py appelle attack_for() et remplace l'animation « attack ».

Conventions (celles de Blockbench, avant l'inversion de signes de l'importateur) : les angles sont en degres, les positions en pixels
(1/16 de bloc) ; le modele regarde vers -Z, son cote droit est +X. Rotation X positive : un membre qui pend se balance vers l'avant, une
tete ou un tronc se renverse vers l'arriere (negative : penche vers l'avant), le bout arriere d'un corps horizontal descend, le bout
avant monte. Rotation Y positive : le devant tourne vers -X (la gauche du modele). Rotation Z positive : le cote +X monte. Position Y
positive : vers le haut ; position Z negative : vers l'avant.
Chaque coup dure au plus 1,2 s (limite du moteur) ; l'impact visible tombe vers 0,25 a 0,35 s, quand le degat est applique.
"""
import math

LENGTH = 1.2


class Rig:
    def __init__(self, groups):
        self.by_name = {g['name']: g['uuid'] for g in groups}
        self.tracks = {}

    def has(self, bone):
        return bone in self.by_name

    def key(self, bone, channel, t, x, y, z, smooth=True):
        assert bone in self.by_name, 'os inconnu : ' + bone
        keys = self.tracks.setdefault((bone, channel), [])
        keys[:] = [k for k in keys if abs(k[0] - t) > 1e-6]               # un appel plus tard remplace l'image cle du meme instant
        keys.append((t, x, y, z, 'catmullrom' if smooth else 'linear'))

    def rot(self, bone, *keys, smooth=True):
        for k in keys:
            self.key(bone, 'rotation', k[0], k[1], k[2] if len(k) > 2 else 0.0, k[3] if len(k) > 3 else 0.0, smooth)

    def pos(self, bone, *keys, smooth=True):
        for k in keys:
            self.key(bone, 'position', k[0], k[1] if len(k) > 1 else 0.0, k[2] if len(k) > 2 else 0.0, k[3] if len(k) > 3 else 0.0, smooth)

    def scale(self, bone, *keys, smooth=True):
        for k in keys:
            self.key(bone, 'scale', k[0], k[1], k[2] if len(k) > 2 else k[1], k[3] if len(k) > 3 else k[1], smooth)

    def animators(self):
        out = {}
        for (bone, channel), keys in self.tracks.items():
            keys = sorted(keys)
            if keys[0][0] > 0:                                  # tout part de la pose de repos
                rest = 1.0 if channel == 'scale' else 0.0
                keys.insert(0, (0.0, rest, rest, rest, 'linear'))
            if keys[-1][0] < LENGTH:
                last = keys[0]
                keys.append((LENGTH, last[1], last[2], last[3], 'linear'))
            frames = [{'channel': channel, 'time': k[0], 'interpolation': k[4], 'data_points': [{'x': k[1], 'y': k[2], 'z': k[3]}]} for k in keys]
            out.setdefault(self.by_name[bone], {'keyframes': []})['keyframes'].extend(frames)
        return out


def sides(rig, fmt):
    """Os d'un cote : fmt contient {s} (-1 ou 1) ; ne renvoie que ceux qui existent."""
    return [(s, fmt.format(s=s)) for s in (-1, 1) if rig.has(fmt.format(s=s))]


# ------------------------------------------------------------------ archetypes bipedes

def attack_mousseux(r):
    r.pos('tronc', (0.10, 0, -1.6, 1.0), (0.28, 0, 0.6, -3.2), (0.55, 0, 0, -1.0), (0.9, 0, 0, 0))
    r.rot('tronc', (0.10, 14, 0, 0), (0.28, -26, 0, 0), (0.55, -8, 0, 0), (0.9, 0, 0, 0))
    for s, b in sides(r, 'branche_bras_{s}'):
        r.rot(b, (0.10, -50, 0, 25 * s), (0.28, 125, 0, -20 * s), (0.55, 60, 0, -10 * s), (0.9, 0, 0, 0))
    r.rot('jeune_pousse', (0.10, 35, 0, 0), (0.28, -50, 0, 12), (0.45, 30, 0, -10), (0.7, -10, 0, 4), (1.0, 0, 0, 0))
    for s, b in sides(r, 'racine_pied_{s}'):
        r.rot(b, (0.28, -28, 0, 0), (0.7, 0, 0, 0))


def attack_sanglier(r):
    r.pos('corps', (0.14, 0, -0.8, 2.2), (0.30, 0, 0.2, -4.5), (0.5, 0, 0, -3.0), (0.95, 0, 0, 0))
    r.rot('corps', (0.14, -10, 0, 0), (0.3, 8, 0, 0), (0.95, 0, 0, 0))
    r.rot('tete', (0.14, -45, 0, 0), (0.3, 40, 0, 0), (0.5, 22, 0, 0), (0.95, 0, 0, 0))
    for name in ('patte_-1_-1', 'patte_1_-1'):
        r.rot(name, (0.14, -55, 0, 0), (0.3, 35, 0, 0), (0.95, 0, 0, 0))
    for name in ('patte_-1_1', 'patte_1_1'):
        r.rot(name, (0.14, 18, 0, 0), (0.3, -30, 0, 0), (0.95, 0, 0, 0))
    r.rot('queue_0', (0.14, 0, 28, 0), (0.3, 0, -28, 0), (0.6, 0, 18, 0), (0.95, 0, 0, 0))
    r.rot('queue_1', (0.14, 0, 40, 0), (0.3, 0, -40, 0), (0.6, 0, 25, 0), (0.95, 0, 0, 0))


def attack_archer(r):
    r.rot('bras_-1', (0.10, 78, 0, -6), (0.30, 92, 0, -6), (0.52, 98, 0, -6), (0.85, 0, 0, 0))
    r.rot('coude_-1', (0.10, 0, 0, 0), (0.85, 0, 0, 0))
    r.rot('bras_1', (0.10, 70, 50, 0), (0.28, 80, 62, 0), (0.34, 84, 8, 0), (0.55, 30, 0, 0), (0.85, 0, 0, 0))
    r.rot('coude_1', (0.10, 80, 0, 0), (0.28, 95, 0, 0), (0.34, 5, 0, 0), (0.55, 20, 0, 0), (0.85, 0, 0, 0))
    r.rot('torse', (0.10, 0, 18, 0), (0.28, 0, 24, 0), (0.34, -3, -6, 0), (0.7, 0, 0, 0))
    r.rot('tete', (0.10, 0, -18, 0), (0.30, 0, -22, 0), (0.7, 0, 0, 0))
    r.rot('jambe_-1', (0.12, 10, 0, 0), (0.7, 0, 0, 0))
    r.rot('jambe_1', (0.12, -14, 0, 0), (0.7, 0, 0, 0))


def attack_veilleur(r):
    r.rot('torse', (0.15, 22, 0, 0), (0.30, -34, 0, 0), (0.55, -14, 0, 0), (0.95, 0, 0, 0))
    r.rot('tete', (0.15, 32, 0, 0), (0.30, -58, 0, 0), (0.55, -20, 0, 0), (0.95, 0, 0, 0))
    for s, b in sides(r, 'bras_{s}'):
        r.rot(b, (0.15, 150, 0, 20 * s), (0.30, 30, 0, 5 * s), (0.6, 40, 0, 0), (0.95, 0, 0, 0))
    r.pos('torse', (0.15, 0, 0, 1.4), (0.30, 0, -1.0, -2.2), (0.6, 0, 0, -0.6), (0.95, 0, 0, 0))


def attack_colosse(r):
    r.rot('tronc', (0.18, 14, 0, 0), (0.34, -26, 0, 0), (0.6, -8, 0, 0), (1.0, 0, 0, 0))
    r.rot('tete', (0.18, 22, 0, 0), (0.34, -30, 0, 0), (0.9, 0, 0, 0))
    for s, b in sides(r, 'branche_epaule_{s}'):
        r.rot(b, (0.18, 165, 0, 25 * s), (0.34, 8, 0, -4 * s), (0.62, 24, 0, 0), (1.0, 0, 0, 0))
    for s, b in sides(r, 'branche_coude_{s}'):
        r.rot(b, (0.18, 25, 0, 0), (0.34, 5, 0, 0), (1.0, 0, 0, 0))
    for s, b in sides(r, 'racine_jambe_{s}'):
        r.rot(b, (0.34, 18 * s, 0, 0), (0.9, 0, 0, 0))
    r.pos('tronc', (0.34, 0, -1.5, -2.2), (0.6, 0, 0, -0.5), (1.0, 0, 0, 0))


def attack_crapaud(r):
    r.pos('corps', (0.12, 0, -0.8, 1.0), (0.26, 0, 1.6, -1.8), (0.5, 0, 0, -0.6), (0.9, 0, 0, 0))
    r.rot('corps', (0.12, 12, 0, 0), (0.26, -10, 0, 0), (0.9, 0, 0, 0))
    r.rot('tete', (0.12, 18, 0, 0), (0.26, -14, 0, 0), (0.9, 0, 0, 0))
    r.rot('machoire', (0.10, -10, 0, 0), (0.22, -55, 0, 0), (0.42, -4, 0, 0), (0.9, 0, 0, 0))
    r.pos('langue', (0.12, 0, 0, 0), (0.24, 0, 0, -9), (0.38, 0, 0, -9), (0.5, 0, 0, 0), smooth=False)
    r.scale('langue', (0.12, 1, 1, 1), (0.24, 1, 1, 2.6), (0.4, 1, 1, 2.6), (0.52, 1, 1, 1), smooth=False)
    for s, b in sides(r, 'patte_{s}_1'):
        r.rot(b, (0.12, 35, 0, 0), (0.26, -55, 0, 0), (0.6, 0, 0, 0))


def attack_limace(r):
    for i, a in enumerate((58, 44, 30, 16, 6, 0)):
        r.rot('segment_%d' % i, (0.16, a * 0.7, 0, 0), (0.30, a, 0, 0), (0.44, -a * 0.5, 0, 0), (0.8, 0, 0, 0))
    r.pos('corps', (0.16, 0, 0, 1.2), (0.30, 0, 0.5, -2.8), (0.5, 0, 0, -0.8), (0.9, 0, 0, 0))
    for s, b in sides(r, 'oeil_pedoncule_{s}'):
        r.rot(b, (0.16, -20, 0, 0), (0.30, 35, 0, 8 * s), (0.9, 0, 0, 0))
        r.scale(b, (0.30, 1, 1.45, 1), (0.8, 1, 1, 1))


def attack_luciole(r):
    r.rot('abdomen', (0.12, -35, 0, 0), (0.30, 105, 0, 0), (0.46, 95, 0, 0), (0.9, 0, 0, 0))
    r.rot('thorax', (0.12, 14, 0, 0), (0.30, -26, 0, 0), (0.9, 0, 0, 0))
    r.pos('thorax', (0.12, 0, 0, 1.4), (0.30, 0, -0.5, -3.4), (0.55, 0, 0, -1.2), (0.95, 0, 0, 0))
    r.rot('tete', (0.30, -18, 0, 0), (0.9, 0, 0, 0))
    for s, b in sides(r, 'patte_{s}_0'):
        r.rot(b, (0.20, 20, 0, 0), (0.30, 70, 0, 0), (0.8, 0, 0, 0))
    for i in (0, 1):
        for s, b in sides(r, 'aile_{s}_%d' % i):
            amp = 40 * s * (1 if i == 0 else -1)
            ks = []
            for n in range(0, 9):
                ks.append((0.06 * n + 0.06, amp if n % 2 == 0 else -amp * 0.5, 0, 0))
            r.rot(b, *[(k[0], 0, 0, k[1]) for k in ks], smooth=False)


def attack_sorcier(r):
    r.rot('bras_1', (0.12, 165, 0, 30), (0.30, 70, 0, 4), (0.55, 80, 0, 0), (0.95, 0, 0, 0))
    r.rot('coude_1', (0.12, 25, 0, 0), (0.30, 5, 0, 0), (0.95, 0, 0, 0))
    r.rot('bras_-1', (0.12, 40, 0, -35), (0.30, 105, 0, -6), (0.55, 100, 0, -6), (0.95, 0, 0, 0))
    r.rot('torse', (0.12, 14, 0, 0), (0.30, -18, 0, 0), (0.9, 0, 0, 0))
    r.rot('tete', (0.12, 16, 0, 0), (0.30, -10, 0, 0), (0.9, 0, 0, 0))
    for i in range(12):
        name = 'roseau_robe_%d' % i
        if r.has(name):
            r.rot(name, (0.3, 0, 0, 10 if i % 2 == 0 else -10), (0.6, 0, 0, -8 if i % 2 == 0 else 8), (1.0, 0, 0, 0))


def attack_hydre(r):
    for h in (0, 1, 2):
        d = h * 0.17
        for k, w in enumerate((10, 18, 22, 14)):
            name = 'cou_%d_%d' % (h, k)
            r.rot(name, (0.10 + d, w, 0, 0), (0.28 + d, -w * 1.4, 0, 0), (0.55 + d, -w * 0.4, 0, 0), (0.95 + d * 0.3, 0, 0, 0))
        r.rot('tete_%d' % h, (0.10 + d, 25, 0, 0), (0.28 + d, -55, 0, 0), (0.55 + d, -10, 0, 0), (0.95 + d * 0.3, 0, 0, 0))
        r.rot('machoire_%d' % h, (0.18 + d, -50, 0, 0), (0.28 + d, -62, 0, 0), (0.40 + d, 0, 0, 0))
    r.pos('corps', (0.2, 0, -0.6, 0.8), (0.4, 0, 0, -1.2), (0.9, 0, 0, 0))
    for i in range(5):
        r.rot('queue_%d' % i, (0.2, 0, 12 + 5 * i, 0), (0.5, 0, -16 - 5 * i, 0), (0.9, 0, 0, 0))


def attack_osselet(r):
    r.rot('bras_1', (0.10, 35, 35, 25), (0.24, 108, 0, 0), (0.50, 100, 0, 0), (0.9, 0, 0, 0))
    r.rot('coude_1', (0.10, 100, 0, 0), (0.24, 0, 0, 0), (0.9, 0, 0, 0))
    r.rot('bras_-1', (0.10, -20, 0, -30), (0.24, -45, 0, -18), (0.9, 0, 0, 0))
    r.rot('torse', (0.10, 0, 26, 0), (0.24, -6, -22, 0), (0.8, 0, 0, 0))
    r.rot('tete', (0.10, 0, 20, 0), (0.24, -8, -16, 0), (0.8, 0, 0, 0))
    r.rot('jambe_1', (0.24, 22, 0, 0), (0.8, 0, 0, 0))
    r.rot('jambe_-1', (0.24, -20, 0, 0), (0.8, 0, 0, 0))
    r.pos('torse', (0.24, 0, 0, -1.8), (0.55, 0, 0, -0.6), (0.9, 0, 0, 0))
    for s, b in sides(r, 'capeline_{s}'):
        r.rot(b, (0.24, 28, 0, 0), (0.8, 0, 0, 0))


def attack_araignee(r):
    r.rot('corps', (0.12, 34, 0, 0), (0.26, -26, 0, 0), (0.5, -6, 0, 0), (0.9, 0, 0, 0))
    r.pos('corps', (0.26, 0, 0, -2.6), (0.55, 0, 0, -0.6), (0.9, 0, 0, 0))
    r.rot('tete', (0.12, 14, 0, 0), (0.26, -24, 0, 0), (0.9, 0, 0, 0))
    r.rot('crochet_-1', (0.10, 0, 42, 0), (0.22, 0, 55, 0), (0.30, 0, -22, 0), (0.7, 0, 0, 0))
    r.rot('crochet_1', (0.10, 0, -42, 0), (0.22, 0, -55, 0), (0.30, 0, 22, 0), (0.7, 0, 0, 0))
    for s in (-1, 1):
        for i in (0, 1):
            r.rot('patte_%d_%d' % (s, i), (0.12, 52, 0, 12 * s), (0.28, 30, 0, 0), (0.8, 0, 0, 0))


def attack_garde(r):
    for s, b in sides(r, 'bras_{s}'):
        r.rot(b, (0.20, 172, 0, 12 * s), (0.36, 14, 0, 0), (0.62, 26, 0, 0), (1.0, 0, 0, 0))
    for s, b in sides(r, 'coude_{s}'):
        r.rot(b, (0.20, 18, 0, 0), (0.36, 8, 0, 0), (1.0, 0, 0, 0))
    r.rot('torse', (0.20, 16, 0, 0), (0.36, -30, 0, 0), (0.62, -12, 0, 0), (1.0, 0, 0, 0))
    r.rot('tete', (0.20, 10, 0, 0), (0.36, -22, 0, 0), (1.0, 0, 0, 0))
    r.pos('torse', (0.36, 0, -1.2, -2.4), (0.6, 0, 0, -0.8), (1.0, 0, 0, 0))
    r.rot('jambe_-1', (0.36, 26, 0, 0), (0.9, 0, 0, 0))
    r.rot('jambe_1', (0.36, -14, 0, 0), (0.9, 0, 0, 0))


def attack_spectre(r):
    r.rot('bras_1', (0.10, 140, 0, 62), (0.28, 90, -75, 10), (0.55, 70, -30, 0), (0.95, 0, 0, 0))
    r.rot('bras_-1', (0.10, 60, 0, -40), (0.28, 105, 55, -10), (0.95, 0, 0, 0))
    for s, b in sides(r, 'chaine_{s}'):
        r.rot(b, (0.10, -50, 0, 18 * s), (0.28, 55, 0, -34 * s), (0.45, 18, 0, 10 * s), (0.9, 0, 0, 0))
    for s, b in sides(r, 'manche_dechiree_{s}'):
        r.rot(b, (0.28, 30, 0, 0), (0.9, 0, 0, 0))
    r.rot('corps', (0.10, 16, 0, 0), (0.28, -22, 0, 0), (0.9, 0, 0, 0))
    r.pos('corps', (0.28, 0, -0.5, -3.2), (0.6, 0, 0, -0.8), (1.0, 0, 0, 0))
    r.rot('capuche', (0.10, 20, 0, 0), (0.28, -26, 0, 0), (0.9, 0, 0, 0))
    for i in range(7):
        r.rot('lambeau_%d' % i, (0.28, 38 + 6 * (i % 3), 0, 0), (0.9, 0, 0, 0))


def attack_roi(r):
    r.rot('bras_1', (0.20, 175, 0, 25), (0.36, 24, 0, 0), (0.62, 36, 0, 0), (1.0, 0, 0, 0))
    r.rot('coude_1', (0.20, 10, 0, 0), (0.36, 0, 0, 0), (1.0, 0, 0, 0))
    r.rot('bras_-1', (0.20, 100, 0, -34), (0.36, 105, 0, -14), (0.7, 60, 0, 0), (1.0, 0, 0, 0))
    r.rot('torse', (0.20, 14, 0, 0), (0.36, -22, 0, 0), (0.8, 0, 0, 0))
    r.rot('tete', (0.20, 16, 0, 0), (0.36, -14, 0, 0), (0.9, 0, 0, 0))
    r.rot('cape', (0.20, -8, 0, 0), (0.36, 42, 0, 0), (0.7, 18, 0, 0), (1.0, 0, 0, 0))
    for i in range(9):
        r.rot('cape_royale_%d' % i, (0.36, 30 + 4 * (i % 3), 0, 0), (0.9, 0, 0, 0))
    r.pos('torse', (0.36, 0, -1.0, -2.0), (0.7, 0, 0, 0))


def attack_renard(r):
    r.pos('corps', (0.10, 0, -1.6, 2.0), (0.26, 0, 5.0, -5.5), (0.42, 0, 0.6, -3.5), (0.9, 0, 0, 0))
    r.rot('corps', (0.10, 14, 0, 0), (0.26, -26, 0, 0), (0.45, -4, 0, 0), (0.9, 0, 0, 0))
    r.rot('tete', (0.10, 22, 0, 0), (0.26, -34, 0, 0), (0.38, -8, 0, 0), (0.5, -30, 0, 0), (0.9, 0, 0, 0))
    for name in ('patte_-1_-1', 'patte_1_-1', 'jarret_patte_-1_-1', 'jarret_patte_1_-1'):
        if r.has(name):
            r.rot(name, (0.10, -30, 0, 0), (0.26, 68, 0, 0), (0.8, 0, 0, 0))
    for name in ('patte_-1_1', 'patte_1_1'):
        r.rot(name, (0.10, 30, 0, 0), (0.26, -52, 0, 0), (0.8, 0, 0, 0))
    for i in range(5):
        r.rot('queue_%d' % i, (0.26, -14 - 3 * i, 0, 0), (0.6, 5, 0, 0), (1.0, 0, 0, 0))


def attack_scarabee(r):
    r.rot('mandibule_-1', (0.10, 0, 46, 0), (0.24, 0, -26, 0), (0.46, 0, 40, 0), (0.7, 0, 0, 0))
    r.rot('mandibule_1', (0.10, 0, -46, 0), (0.24, 0, 26, 0), (0.46, 0, -40, 0), (0.7, 0, 0, 0))
    r.rot('tete', (0.10, 24, 0, 0), (0.24, -36, 0, 0), (0.9, 0, 0, 0))
    r.rot('corps', (0.10, 12, 0, 0), (0.24, -12, 0, 0), (0.9, 0, 0, 0))
    r.pos('corps', (0.10, 0, 0, 1.2), (0.24, 0, 0, -3.0), (0.55, 0, 0, -0.6), (0.9, 0, 0, 0))
    for s in (-1, 1):
        r.rot('patte_%d_0' % s, (0.24, 44, 0, 0), (0.8, 0, 0, 0))


def attack_harpie(r):
    r.rot('corps', (0.10, 22, 0, 0), (0.28, -50, 0, 0), (0.55, -12, 0, 0), (1.0, 0, 0, 0))
    r.pos('corps', (0.10, 0, 3, 2), (0.28, 0, -2, -6), (0.6, 0, 0, -1), (1.0, 0, 0, 0))
    r.rot('tete', (0.28, 28, 0, 0), (0.9, 0, 0, 0))
    for s, b in sides(r, 'patte_{s}'):
        r.rot(b, (0.10, -25, 0, 0), (0.28, 95, 0, 0), (0.6, 40, 0, 0), (1.0, 0, 0, 0))
    for s, b in sides(r, 'aile_{s}'):
        r.rot(b, (0.10, 0, 0, 60 * s), (0.28, 0, 0, -40 * s), (0.45, 0, 0, 35 * s), (0.65, 0, 0, -20 * s), (1.0, 0, 0, 0))
    r.rot('queue_plumes', (0.28, 40, 0, 0), (0.9, 0, 0, 0))


def attack_golem(r):
    for s, b in sides(r, 'bras_{s}'):
        r.rot(b, (0.26, 178, 0, 14 * s), (0.42, 4, 0, 0), (0.7, 14, 0, 0), (1.15, 0, 0, 0))
    for s, b in sides(r, 'coude_{s}'):
        r.rot(b, (0.26, 25, 0, 0), (0.42, 4, 0, 0), (1.15, 0, 0, 0))
    r.rot('torse', (0.26, 20, 0, 0), (0.42, -34, 0, 0), (0.75, -10, 0, 0), (1.15, 0, 0, 0))
    r.rot('tete', (0.26, 12, 0, 0), (0.42, -26, 0, 0), (1.15, 0, 0, 0))
    r.pos('torse', (0.26, 0, 3.0, 1.0), (0.42, 0, -1.4, -2.6), (0.75, 0, 0, -0.6), (1.15, 0, 0, 0))
    for s, b in sides(r, 'jambe_{s}'):
        r.rot(b, (0.42, 0, 0, 12 * s), (0.9, 0, 0, 0))


def attack_wyrm(r):
    r.rot('tete', (0.12, 26, 0, 0), (0.28, -46, 0, 0), (0.46, -16, 0, 0), (0.62, -40, 0, 0), (1.0, 0, 0, 0))
    r.rot('machoire', (0.12, -48, 0, 0), (0.26, -62, 0, 0), (0.34, -2, 0, 0), (0.52, -58, 0, 0), (0.64, 0, 0, 0))
    r.pos('corps', (0.12, 0, 0.5, 2.5), (0.28, 0, -1.4, -4.8), (0.55, 0, 0, -1.6), (1.0, 0, 0, 0))
    for i in range(14):
        name = 'vertebre_%d' % i
        if r.has(name):
            r.rot(name, (0.12 + 0.02 * i, 9 * math.cos(i), 6 * math.sin(i), 0), (0.30 + 0.02 * i, -12 * math.cos(i + 1), -8 * math.sin(i), 0), (0.7, 3 * math.cos(i), 0, 0), (1.1, 0, 0, 0))


def attack_scorpion(r):
    for i in range(6):
        r.rot('queue_%d' % i, (0.12, 10 + 2 * i, 0, 0), (0.28, -24 - 3 * i, 0, 0), (0.46, -12 - 2 * i, 0, 0), (0.9, 0, 0, 0))
    r.rot('queue_5', (0.28, -56, 0, 0), (0.46, -30, 0, 0), (0.9, 0, 0, 0))
    r.pos('corps', (0.12, 0, 0, 0.8), (0.28, 0, -0.4, -2.8), (0.55, 0, 0, -0.8), (0.9, 0, 0, 0))
    r.rot('corps', (0.12, 6, 0, 0), (0.28, -8, 0, 0), (0.9, 0, 0, 0))
    for s in (-1, 1):
        r.rot('bras_pince_%d' % s, (0.12, 0, 22 * s, 0), (0.28, 0, -30 * s, 0), (0.6, 0, 6 * s, 0), (0.9, 0, 0, 0))
        r.rot('doigt_pince_%d_-1' % s, (0.12, 0, 28, 0), (0.28, 0, -14, 0), (0.9, 0, 0, 0))
        r.rot('doigt_pince_%d_1' % s, (0.12, 0, -28, 0), (0.28, 0, 14, 0), (0.9, 0, 0, 0))


def attack_momie(r):
    r.rot('bras_-1', (0.14, 52, 0, -40), (0.32, 100, -40, -4), (0.5, 96, -22, 0), (0.95, 0, 0, 0))
    r.rot('bras_1', (0.14, 52, 0, 40), (0.32, 100, 40, 4), (0.5, 96, 22, 0), (0.95, 0, 0, 0))
    for s, b in sides(r, 'coude_{s}'):
        r.rot(b, (0.14, 20, 0, 0), (0.32, 8, 0, 0), (0.95, 0, 0, 0))
    r.rot('torse', (0.14, 10, 0, 0), (0.32, -22, 0, 0), (0.95, 0, 0, 0))
    r.rot('tete', (0.14, 8, 0, 0), (0.32, -16, 0, 0), (0.95, 0, 0, 0))
    r.pos('torse', (0.32, 0, 0, -2.6), (0.6, 0, 0, -0.8), (1.0, 0, 0, 0))
    r.rot('jambe_-1', (0.32, 20, 0, 0), (0.95, 0, 0, 0))
    for s, b in sides(r, 'bande_libre_{s}'):
        r.rot(b, (0.32, 32, 0, 14 * s), (0.9, 0, 0, 0))


def attack_hyene(r):
    r.pos('corps', (0.10, 0, -0.8, 2.0), (0.26, 0, 0.6, -4.8), (0.44, 0, 0, -3.6), (0.9, 0, 0, 0))
    r.rot('corps', (0.10, 8, 0, 0), (0.26, -12, 0, 0), (0.9, 0, 0, 0))
    r.rot('tete', (0.10, 18, 0, 0), (0.26, -22, 0, 0), (0.36, -10, 14, 0), (0.46, -22, -14, 0), (0.56, -10, 10, 0), (0.9, 0, 0, 0))
    r.rot('machoire', (0.10, -55, 0, 0), (0.24, -60, 0, 0), (0.30, 0, 0, 0), (0.6, 0, 0, 0))
    for name in ('patte_-1_-1', 'patte_1_-1'):
        r.rot(name, (0.10, -22, 0, 0), (0.26, 40, 0, 0), (0.8, 0, 0, 0))
    r.rot('queue_0', (0.26, -20, 20, 0), (0.7, 0, 0, 0))
    r.rot('queue_1', (0.26, -26, -26, 0), (0.7, 0, 0, 0))


def attack_djinn(r):
    for s, b in sides(r, 'bras_{s}'):
        r.rot(b, (0.16, 168, 0, 32 * s), (0.32, 92, 0, 4 * s), (0.58, 100, 0, 0), (1.0, 0, 0, 0))
    for s, b in sides(r, 'flamme_main_{s}'):
        r.scale(b, (0.16, 1, 1, 1), (0.32, 2.2, 2.2, 2.2), (0.6, 1.3, 1.3, 1.3), (1.0, 1, 1, 1))
        r.pos(b, (0.32, 0, 0, -4), (0.7, 0, 0, 0))
    r.rot('corps', (0.16, 20, 0, 0), (0.32, -22, 0, 0), (0.9, 0, 0, 0))
    r.pos('corps', (0.16, 0, 1.4, 1.4), (0.32, 0, 0, -3.0), (0.9, 0, 0, 0))
    r.rot('tete', (0.16, 14, 0, 0), (0.32, -10, 0, 0), (0.9, 0, 0, 0))
    for i in range(7):
        r.rot('braise_inferieure_%d' % i, (0.32, 14 + i * 3, 0, 0), (0.9, 0, 0, 0))


def attack_tortue(r):
    r.pos('tete', (0.14, 0, 0, 3.2), (0.30, 0, 0.4, -6.0), (0.5, 0, 0, -3.2), (0.95, 0, 0, 0))
    r.rot('tete', (0.14, 16, 0, 0), (0.30, -22, 0, 0), (0.95, 0, 0, 0))
    r.rot('machoire', (0.14, 0, 0, 0), (0.26, -55, 0, 0), (0.36, 0, 0, 0))
    r.rot('corps', (0.14, 7, 0, 0), (0.30, -10, 0, 0), (0.95, 0, 0, 0))
    r.pos('corps', (0.14, 0, -0.4, 1.0), (0.30, 0, 0.6, -2.4), (0.95, 0, 0, 0))
    for name in ('patte_-1_-1', 'patte_1_-1'):
        r.rot(name, (0.14, -22, 0, 0), (0.30, 36, 0, 0), (0.9, 0, 0, 0))
    for name in ('patte_-1_1', 'patte_1_1'):
        r.rot(name, (0.30, -30, 0, 0), (0.9, 0, 0, 0))


def attack_mite(r):
    r.rot('corps', (0.10, 18, 0, 0), (0.26, -52, 0, 0), (0.5, -14, 0, 0), (1.0, 0, 0, 0))
    r.pos('corps', (0.10, 0, 3, 2), (0.26, 0, -2.4, -6), (0.55, 0, 0, -1.4), (1.0, 0, 0, 0))
    r.rot('tete', (0.26, -26, 0, 0), (0.9, 0, 0, 0))
    for s in (-1, 1):
        ks_h, ks_b = [], []
        for n in range(8):
            a = 54 if n % 2 == 0 else -30
            ks_h.append((0.08 + 0.07 * n, 0, 0, a * s))
            ks_b.append((0.08 + 0.07 * n, 0, 0, -a * 0.7 * s))
        r.rot('aile_%d_haute' % s, *ks_h, smooth=False)
        r.rot('aile_%d_basse' % s, *ks_b, smooth=False)


def attack_rodeur(r):
    r.pos('corps', (0.12, 0, -1.0, 1.6), (0.28, 0, 0.8, -2.8), (0.5, 0, 0, -1.0), (0.95, 0, 0, 0))
    r.rot('corps', (0.12, 22, 0, 0), (0.28, -8, 0, 0), (0.95, 0, 0, 0))
    r.rot('patte_1_-1', (0.12, 110, 0, 22), (0.26, 95, 62, 0), (0.42, 70, -26, 0), (0.9, 0, 0, 0))
    r.rot('jarret_patte_1_-1', (0.12, 40, 0, 0), (0.26, 0, 0, 0), (0.9, 0, 0, 0))
    r.rot('patte_-1_-1', (0.26, 30, 0, 0), (0.9, 0, 0, 0))
    r.rot('tete', (0.12, 16, 0, 0), (0.26, -20, 0, 0), (0.95, 0, 0, 0))
    r.rot('machoire', (0.12, -50, 0, 0), (0.32, -58, 0, 0), (0.44, 0, 0, 0))
    for i in range(3):
        r.rot('queue_%d' % i, (0.12, 0, 24, 0), (0.30, 0, -30, 0), (0.55, 0, 16, 0), (0.95, 0, 0, 0))


def attack_oeil(r):
    r.scale('oeil', (0.14, 1, 1, 1), (0.30, 1.4, 1.4, 1.4), (0.46, 1.15, 1.15, 1.15), (0.9, 1, 1, 1))
    r.pos('oeil', (0.14, 0, 0, 2.0), (0.30, 0, 0, -3.0), (0.9, 0, 0, 0))
    r.pos('iris', (0.14, 0, 0, 0), (0.30, 0, 0, -2.2), (0.6, 0, 0, -1.0), (0.9, 0, 0, 0))
    r.scale('iris', (0.14, 1, 1, 1), (0.30, 1.5, 1.5, 1.5), (0.9, 1, 1, 1))
    for i in range(7):
        for j in range(5):
            name = 'tentacule_%d_%d' % (i, j)
            if not r.has(name):
                continue
            ph = i * 0.11 + j * 0.045
            r.rot(name, (0.10 + ph, 0, 0, 28), (0.28 + ph, 0, 0, -36), (0.5 + ph, 0, 0, 22), (0.8 + ph * 0.3, 0, 0, 0))


def attack_chevalier(r):
    r.rot('bras_1', (0.12, 150, 0, 40), (0.26, 52, -62, 0), (0.5, 30, -30, 0), (0.95, 0, 0, 0))
    r.rot('coude_1', (0.12, 50, 0, 0), (0.26, 0, 0, 0), (0.95, 0, 0, 0))
    r.rot('bras_-1', (0.12, -30, 0, -26), (0.26, -48, 0, -12), (0.95, 0, 0, 0))
    r.rot('torse', (0.12, 8, 30, 0), (0.26, -20, -34, 0), (0.55, -8, -10, 0), (0.95, 0, 0, 0))
    r.rot('tete', (0.12, 6, 18, 0), (0.26, -12, -20, 0), (0.95, 0, 0, 0))
    r.pos('torse', (0.12, 0, 0, 1.0), (0.26, 0, -0.6, -3.4), (0.6, 0, 0, -1.0), (1.0, 0, 0, 0))
    r.rot('jambe_-1', (0.26, 38, 0, 0), (0.9, 0, 0, 0))
    r.rot('jambe_1', (0.26, -26, 0, 0), (0.9, 0, 0, 0))


def attack_devoreur(r):
    r.rot('tete', (0.12, 30, 0, 0), (0.26, -50, 0, 0), (0.42, -10, 0, 0), (0.56, -48, 0, 0), (0.74, -8, 0, 0), (1.1, 0, 0, 0))
    r.rot('machoire', (0.12, -52, 0, 0), (0.24, -70, 0, 0), (0.32, 0, 0, 0), (0.48, -68, 0, 0), (0.58, 0, 0, 0), (0.9, 0, 0, 0))
    r.pos('corps', (0.12, 0, 1.0, 3.5), (0.26, 0, -2.4, -6.5), (0.5, 0, 0, -2.0), (1.1, 0, 0, 0))
    for i in range(14):
        name = 'vertebre_%d' % i
        if r.has(name):
            r.rot(name, (0.12 + 0.02 * i, 8 * math.cos(i), 5 * math.sin(i), 0), (0.30 + 0.02 * i, -12 * math.cos(i + 1), -7 * math.sin(i), 0), (0.75, 3 * math.cos(i), 0, 0), (1.15, 0, 0, 0))


BUILDERS = {
    'mousseux': attack_mousseux, 'sanglier_ronce': attack_sanglier, 'archer_sylvestre': attack_archer, 'veilleur_champignon': attack_veilleur,
    'colosse_racine': attack_colosse, 'crapaud_venimeux': attack_crapaud, 'limace_acide': attack_limace, 'luciole_geante': attack_luciole,
    'sorcier_roseaux': attack_sorcier, 'hydre_marais': attack_hydre, 'osselet': attack_osselet, 'araignee_os': attack_araignee,
    'garde_funeraire': attack_garde, 'spectre_enchaine': attack_spectre, 'roi_cryptes': attack_roi, 'renard_givre': attack_renard,
    'scarabee_polaire': attack_scarabee, 'harpie_neiges': attack_harpie, 'golem_iceberg': attack_golem, 'wyrm_boreal': attack_wyrm,
    'scorpion_cuivre': attack_scorpion, 'momie_dunes': attack_momie, 'hyene_cendree': attack_hyene, 'djinn_braises': attack_djinn,
    'tortue_volcanique': attack_tortue, 'mite_astrale': attack_mite, 'rodeur_vide': attack_rodeur, 'oeil_chaos': attack_oeil,
    'chevalier_fracture': attack_chevalier, 'devoreur_etoiles': attack_devoreur,
}


def attack_for(key, groups):
    """Animation « attack » du monstre (format des animations Blockbench du projet), ou None si le monstre n'a pas de coup dedie."""
    build = BUILDERS.get(key)
    if build is None:
        return None
    rig = Rig(groups)
    build(rig)
    return {'name': 'attack', 'length': LENGTH, 'loop': 'once', 'animators': rig.animators()}
