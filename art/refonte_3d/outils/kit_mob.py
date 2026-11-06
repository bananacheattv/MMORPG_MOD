#!/usr/bin/env python3
"""Pieces communes des mobs : on dessine en texels de jeu (16 unites = 1 bloc en jeu, 1 texel par unite, grille entiere). L'export divise ensuite geometrie, pivots et positions animees par ImportedMobs.scale.
Regle de nommage : un cube dont le nom contient « lueur » est emissive dans le JSON (ImportedGlowLayer). +x = cote droit de la creature (arm_-1, leg_-1), ou va l'arme."""
import math

import numpy as np

from aeloria3d import hash3
from kit import PAL
from kit_decor import CF, CH, CR, D, EAU, OR, FER, FER_N, AME, CRISTAL
from materiaux import RAMPS, stamp

SEIZE = 16
PEINTRES_LOCAUX = {}


class M(D):
    def __init__(self, nom, seed, echelle=1.0, kind='mobs', description=None):
        super().__init__(nom, seed, description or f'{nom} Aeloria : refonte (16 texels par bloc en jeu).')
        self.p.kind = kind
        self.p.meta['echelle'] = echelle

    def _n(self, nom):
        k = self.noms.get(nom, 0)
        pris = {c.name for c in self.p.cubes}
        while True:
            k += 1
            n = nom if k == 1 else f'{nom}_{k}'
            if not ({n, n + '_g', n + '_d'} & (pris | set(self.p.bones))):
                self.noms[nom] = k
                return n

    def cb(self, bone, nom, a, b, mat, **kw):
        n = self._n(nom)
        self.p.cube(bone, n, a, b, mat, **kw)
        return n

    def sb(self, bones, nom, a, b, mat, **kw):
        n = self._n(nom)
        self.p.sym(bones, n, a, b, mat, **kw)
        return n + '_g'

    def humanoide(self, hanche, epaule, cou, x_bras, x_jambe, coude=None, genou=None, z_bras=0, z_jambe=0, z_cou=0, racine_jambes=True):
        """Os du rig bipede : body pivote aux hanches ; head, bras sur body ; jambes sur root, pour que le buste penche sans deplacer les pieds."""
        p = self.p
        p.bone('body', (0, hanche, 0), 'root')
        p.bone('head', (0, cou, z_cou), 'body')
        p.bone('arm_-1', (x_bras, epaule, z_bras), 'body')
        p.bone('arm_1', (-x_bras, epaule, z_bras), 'body')
        pere = 'root' if racine_jambes else 'body'
        p.bone('leg_-1', (x_jambe, hanche, z_jambe), pere)
        p.bone('leg_1', (-x_jambe, hanche, z_jambe), pere)
        if coude is not None:
            p.bone('avbras_-1', (x_bras, coude, z_bras), 'arm_-1')
            p.bone('avbras_1', (-x_bras, coude, z_bras), 'arm_1')
        if genou is not None:
            p.bone('tibia_-1', (x_jambe, genou, z_jambe), 'leg_-1')
            p.bone('tibia_1', (-x_jambe, genou, z_jambe), 'leg_1')
        self.b = 'body'

    # ------------------------------------------------------------------ corps en chaine
    def courbe(self, points, largeurs, mat, prefixe='seg', parent='root', section=None, recouvre=2, cote=None, **kw):
        """Corps articule le long d'une ligne. Par segment : un os de chaine (droit, celui qu'on anime) et, dedans, un os tourne dans l'axe du segment, ou tout est pose droit et sur la grille.
        largeurs : demi-largeur a chaque point. section : (rapport x, rapport z) si le corps n'est pas carre. cote(m, os_tourne, i, (x0, y0, z0), (wx, wz), H) ajoute cretes et pics dans le repere du segment (axe +y)."""
        out = []
        pere = parent
        for i in range(len(points) - 1):
            a, b = np.array(points[i], float), np.array(points[i + 1], float)
            d = b - a
            L = np.linalg.norm(d)
            d = d / L
            ax = math.degrees(math.asin(max(-1, min(1, d[2]))))
            az = math.degrees(math.atan2(-d[0], d[1]))
            w = largeurs[i]
            wx, wz = (max(1, int(round(w * section[0]))), max(1, int(round(w * section[1])))) if section else (w, w)
            os_ = f'{prefixe}{i}'
            x0, y0, z0 = (int(round(v)) for v in a)
            self.p.bone(os_, (x0, y0, z0), pere)
            os_r = os_ + '_r'
            self.p.bone(os_r, (x0, y0, z0), os_, rot=(ax, 0, az))
            H = int(math.ceil(L)) + recouvre
            self.cb(os_r, f'{prefixe}_cube', (x0 - wx, y0 - 1, z0 - wz), (x0 + wx, y0 + H, z0 + wz), mat, **kw)
            out.append((os_, (x0, y0, z0), (ax, az), (wx, wz), H))
            if cote:
                cote(self, os_r, i, (x0, y0, z0), (wx, wz), H)
            pere = os_
        return out

    # ------------------------------------------------------------------ yeux et lueurs
    def yeux(self, bone, x0, y0, z, w=2, h=2, ramp='rune', base=3.5, dz=1, nom='oeil_lueur', **kw):
        """Paire d'yeux lumineux : deux petits cubes en saillie d'un texel sur la face z (l'emissive les redessine)."""
        return self.sb(bone, nom, (x0, y0, z - dz), (x0 + w, y0 + h, z), ('uni', ramp), base=base, no_ao=True, hide={'south'}, **kw)

    def lueur(self, bone, nom, a, b, mat, **kw):
        kw.setdefault('no_ao', True)
        return self.cb(bone, nom + '_lueur', a, b, mat, **kw)

    def lueur_sym(self, bone, nom, a, b, mat, **kw):
        kw.setdefault('no_ao', True)
        return self.sb(bone, nom + '_lueur', a, b, mat, **kw)

    # ------------------------------------------------------------------ vegetation et petits volumes
    def touffe(self, bone, nom, cx, y0, cz, h, w, ramp='feuillage', base=3.0, masque=None):
        """Touffe d'herbe ou pousse : deux plans croises decoupes."""
        M_ = masque or feuille(max(w * 2, 4), h)
        self.cb(bone, nom, (cx - w, y0, cz), (cx + w, y0 + h, cz + 1), ('plan', ramp), base=base, masque=M_, **CF)
        self.cb(bone, nom, (cx, y0, cz - w), (cx + 1, y0 + h, cz + w), ('plan', ramp), base=base + .4, masque=M_, **CR)

    def champi(self, bone, nom, cx, y0, cz, h, r, chapeau=('chapeau', 'champignon')):
        """Petit champignon : pied, chapeau a deux etages."""
        self.cb(bone, nom + '_pied', (cx - max(1, r // 2), y0, cz - max(1, r // 2)), (cx + max(1, r // 2), y0 + h, cz + max(1, r // 2)), ('uni', 'chair_pale'), base=3.4)
        self.cb(bone, nom, (cx - r, y0 + h, cz - r), (cx + r, y0 + h + 2, cz + r), chapeau)
        if r >= 2:
            self.cb(bone, nom + '_haut', (cx - r + 1, y0 + h + 2, cz - r + 1), (cx + r - 1, y0 + h + 3, cz + r - 1), chapeau, ton=.4)

    def pointe(self, bone, nom, cx, y0, cz, h, r, mat, inc=(0, 0), tourne=45, **kw):
        """Epine ou corne a deux etages tournee d'un coup. inc : (bascule x, bascule z)."""
        rx, rz = inc
        n = self.cb(bone, nom, (cx - r, y0, cz - r), (cx + r, y0 + h * 3 // 5, cz + r), mat, rot=(rx, tourne, rz), pivot=(cx, y0, cz), **kw)
        r2 = max(1, r // 2) if r > 1 else r
        self.cb(bone, nom, (cx - r2, y0 + h * 3 // 5, cz - r2), (cx + r2, y0 + h, cz + r2), mat, rot=(rx, tourne, rz), pivot=(cx, y0, cz), **{**kw, 'ton': kw.get('ton', 0) + .7})
        return n


def anneau(m, os_, cx, cy, cz, R, e, h, n, mat, plan='xy', **kw):
    """Anneau a n segments droits. plan xy : anneau de face (e epaisseur radiale, h profondeur en z), plan xz : anneau couche (h hauteur en y). Les segments se recouvrent aux joints : pas de jour."""
    L = int(math.ceil(2 * R * math.tan(math.pi / n))) + 2
    L += L % 2
    e_, h_ = e + e % 2, h + h % 2
    for i in range(n):
        a = i * math.tau / n
        if plan == 'xy':
            px, py = cx + R * math.cos(a), cy + R * math.sin(a)
            x0, y0 = int(round(px)), int(round(py))
            m.cb(os_, 'seg_anneau', (x0 - e_ // 2, y0 - L // 2, cz - h_ // 2), (x0 + e_ // 2, y0 + L // 2, cz + h_ // 2), mat, rot=(0, 0, math.degrees(a)), pivot=(px, py, cz), **kw)
        else:
            px, pz = cx + R * math.cos(a), cz + R * math.sin(a)
            x0, z0 = int(round(px)), int(round(pz))
            m.cb(os_, 'seg_anneau', (x0 - e_ // 2, cy - h_ // 2, z0 - L // 2), (x0 + e_ // 2, cy + h_ // 2, z0 + L // 2), mat, rot=(0, -math.degrees(a), 0), pivot=(px, cy, pz), **kw)


def feuille(w, h):
    """Masque d'une pousse : feuilles en pointe de chaque cote d'une tige."""
    rows = []
    for j in range(h):
        t = j / max(1, h - 1)
        large = (math.sin(t * math.pi) ** .7) * (w / 2)
        rows.append(''.join('#' if abs(i + .5 - w / 2) <= large else '.' for i in range(w)))
    return rows


def losange(w, h):
    rows = []
    for j in range(h):
        t = 1 - abs(2 * (j + .5) / h - 1)
        rows.append(''.join('#' if abs(2 * (i + .5) / w - 1) <= t else '.' for i in range(w)))
    return rows


def triangle(w, h, vers_haut=True):
    rows = []
    for j in range(h):
        t = (j + 1) / h
        r = ''.join('#' if abs(2 * (i + .5) / w - 1) <= t else '.' for i in range(w))
        rows.append(r)
    return rows if vers_haut else rows[::-1]


def rond(w, h=None):
    h = h or w
    rows = []
    for j in range(h):
        y = 2 * (j + .5) / h - 1
        rows.append(''.join('#' if ((2 * (i + .5) / w - 1) ** 2 + y * y) <= 1 else '.' for i in range(w)))
    return rows


def frange_peintre(ctx, nom='feuillage', base=2.6, ton=0.0, mini=.35, pas=2, **_):
    """Plan fin dont seul le bas est effiloche, colonne par colonne (mousse qui pend, ourlet dechire, barbe, stalactites). Meme decoupe sur les deux faces."""
    a, b = ctx.cube.frm, ctx.cube.to
    along = (ctx.Plocal[..., 0] - a[0]) if ctx.face in ('north', 'south') else (ctx.Plocal[..., 2] - a[2])
    col = np.floor(np.round(along * ctx.d - .5) / pas)
    h = hash3(col, 3, 7, 301 + ctx.proj.seed)
    t = (b[1] - ctx.Plocal[..., 1]) / max(1e-6, b[1] - a[1])
    alpha = np.where(t <= mini + (1 - mini) * h, 255, 0).astype(np.uint8)
    idc = hash3(col, 5, 2, 303 + ctx.proj.seed)
    v = base + ton + (1 - t) * 1.4 + (idc - .5) * 1.4
    return nom, v, alpha


def remiges_peintre(ctx, nom='plume_blanche', base=3.6, ton=0.0, mini=.55, pas=4, **_):
    """Rang de plumes en plan fin : chaque plume est une colonne a bout arrondi, trait sombre d'un cote, rachis clair, bout plus sombre. Meme decoupe sur les deux faces."""
    a, b = ctx.cube.frm, ctx.cube.to
    along = (ctx.Plocal[..., 0] - a[0]) if ctx.face in ('north', 'south') else (ctx.Plocal[..., 2] - a[2])
    q = np.round(along * ctx.d - .5)
    col = np.floor(q / pas)
    h = hash3(col, 3, 7, 307 + ctx.proj.seed)
    ph = np.mod(q, pas)
    t = (b[1] - ctx.Plocal[..., 1]) / max(1e-6, b[1] - a[1])
    lim = mini + (1 - mini) * (.5 + .5 * h)
    bord = (ph == 0) | (ph == pas - 1)
    lim2 = lim - bord * (1.5 / max(1, (b[1] - a[1]) * ctx.d))
    alpha = np.where(t <= lim2, 255, 0).astype(np.uint8)
    v = base + ton + (h - .5) * .7 - 1.5 * (ph == 0) + .6 * (ph == pas // 2)
    v = v - 1.0 * (t > lim2 - 2.5 / max(1, (b[1] - a[1]) * ctx.d))
    return nom, v, alpha


def enregistre():
    from materiaux import PEINTRES
    PEINTRES['frange'] = frange_peintre
    PEINTRES['remiges'] = remiges_peintre


enregistre()
FR = lambda ramp: ('frange', ramp)
RE = lambda ramp: ('remiges', ramp)
