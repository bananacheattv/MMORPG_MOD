#!/usr/bin/env python3
"""Pieces communes des decors 3D (kind props, ImportedModel) : 16 unites = 1 bloc, 1 texel par unite. Socles, futs, arcs, ogives, pinacles, toits, flammes, cristaux, bannieres, vitraux, rayonnages.
Toutes les coordonnees sont entieres. Les parties qui bougent passent par le clip chest, joue en boucle par DecorModelRenderer."""
import math
from contextlib import contextmanager

import numpy as np

from aeloria3d import Project, hash3
from animations import keys, rot, sin
from kit import EMBLEME, MEDAILLON, PAL
from materiaux import stamp

PIERRE = ('maconnerie', 'pierre')
PIERRE_N = ('maconnerie', 'pierre_noire')
PIERRE_C = ('maconnerie', 'pierre_chaude')
DALLE = ('dalle', 'pierre')
MARBRE = ('marbre', 'marbre')
MARBRE_N = ('marbre', 'marbre_sombre')
BOIS = ('bois', 'bois')
BOIS_C = ('bois', 'bois_clair')
BOIS_N = ('bois', 'bois_sombre')
OR = ('metal', 'or')
FER = ('metal', 'fer')
FER_N = ('metal', 'fer_noir')
CRISTAL = ('cristal', 'cristal')
AME = ('cristal', 'amethyste')
FEUILLE = ('feuilles', 'feuillage')
FEUILLE_N = ('feuilles', 'feuillage_sombre')
CIRE = ('uni', 'cire')
EAU = 'eau'
ARDOISE = ('tuiles', 'ardoise')
TUILE = ('tuiles', 'tuile_violette')
BRAISES = ('braises', 'basalte')
CR = dict(hide={'north', 'south', 'up', 'down'}, no_shadow=True)      # plan vu de cote (faces est, ouest)
CF = dict(hide={'east', 'west', 'up', 'down'}, no_shadow=True)        # plan vu de face (faces nord, sud)
CH = dict(hide={'north', 'south', 'east', 'west'}, no_shadow=True)    # plan horizontal
VITRAIL = ('#c02a4a', '#2a55c0', '#e0a820', '#7a39b5', '#2a9d8f', '#e8d9b0')
LIVRES = ('#6b1d1d', '#1f3f6b', '#2f5a2f', '#6b4a1d', '#4a1f66', '#7a6a4a', '#23323f')


def demi_entier(v):
    return int(round(v))


class D:
    """Un decor en construction. Toutes les methodes ajoutent des cubes sur l'os courant."""

    def __init__(self, nom, seed, description=None):
        self.p = Project(nom, density=1, kind='props', seed=seed, description=description or f'{nom} Aeloria : refonte (16 texels par bloc).')
        self.p.bone('root', (0, 0, 0))
        self.b = 'root'
        self.noms = {}
        self.mouvants = []
        self._mir = False

    # ------------------------------------------------------------------ bases
    def _n(self, nom):
        k = self.noms.get(nom, 0)
        self.noms[nom] = k + 1
        return nom if k == 0 else f'{nom}{k + 1}'

    def os(self, nom, pivot, parent='root'):
        self.p.bone(nom, pivot, parent)
        self.b = nom
        return nom

    def racine(self):
        self.b = 'root'

    def c(self, nom, a, b, mat, **kw):
        if self._mir:
            return self.s(nom, a, b, mat, **kw)
        n = self._n(nom)
        self.p.cube(self.b, n, a, b, mat, **kw)
        return n

    @contextmanager
    def miroir(self):
        """Tout ce qui est pose dans ce bloc (cote +x) existe aussi en miroir cote -x, sur la meme texture."""
        self._mir = True
        yield
        self._mir = False

    def s(self, nom, a, b, mat, **kw):
        n = self._n(nom)
        self.p.sym(self.b, n, a, b, mat, **kw)
        return n + '_g'

    def boite(self, nom, dx, y0, y1, dz, mat, cx=0, cz=0, **kw):
        return self.c(nom, (cx - dx, y0, cz - dz), (cx + dx, y1, cz + dz), mat, **kw)

    def socle(self, nom, dx, dz, paliers, mat, y0=0, cx=0, cz=0, **kw):
        """Paliers : [(hauteur, retrait)] du bas vers le haut. Renvoie le y du dessus."""
        y = y0
        r = 0
        for i, (h, ret) in enumerate(paliers):
            r += ret
            self.boite(nom, dx - r, y, y + h, dz - r, mat, cx=cx, cz=cz, **kw)
            y += h
        return y

    def fut(self, nom, cx, cz, y0, y1, r, mat, chanfrein=1, **kw):
        """Fut a coins abattus (croix de deux cubes, sans dessus ni dessous : a coiffer)."""
        k = kw.pop('hide', set()) | {'up', 'down'}
        if chanfrein and r > chanfrein:
            self.boite(nom, r, y0, y1, r - chanfrein, mat, cx=cx, cz=cz, hide=k, **kw)
            self.boite(nom, r - chanfrein, y0, y1, r, mat, cx=cx, cz=cz, hide=k, **kw)
        else:
            self.boite(nom, r, y0, y1, r, mat, cx=cx, cz=cz, hide=k, **kw)

    def colonne(self, nom, cx, cz, y0, y1, r, fut=MARBRE, base=PIERRE, chap=OR, bague=None):
        """Colonne complete : base a deux paliers, fut, bague optionnelle, chapiteau a deux paliers."""
        self.boite(nom + '_base', r + 3, y0, y0 + 3, r + 3, base, cx=cx, cz=cz)
        self.boite(nom + '_base', r + 2, y0 + 3, y0 + 5, r + 2, base, cx=cx, cz=cz, ton=.3)
        self.fut(nom + '_fut', cx, cz, y0 + 5, y1 - 5, r, fut)
        if bague:
            ym = (y0 + y1) // 2
            self.boite(nom + '_bague', r + 1, ym, ym + 2, r + 1, bague, cx=cx, cz=cz)
        self.boite(nom + '_chap', r + 1, y1 - 5, y1 - 3, r + 1, chap, cx=cx, cz=cz)
        self.boite(nom + '_chap', r + 3, y1 - 3, y1, r + 3, chap, cx=cx, cz=cz, ton=.3)

    def rond(self, nom, cx, cz, y0, y1, r, mat, **kw):
        """Disque plein en cinq pieces sans recouvrement, dessus visible sans scintillement."""
        a = max(1, demi_entier(r * .7))
        b = max(1, demi_entier(r * .45))
        self.boite(nom, a, y0, y1, a, mat, cx=cx, cz=cz, **kw)
        self.c(nom, (cx - b, y0, cz - r), (cx + b, y1, cz - a), mat, **kw)
        self.c(nom, (cx - b, y0, cz + a), (cx + b, y1, cz + r), mat, **kw)
        self.c(nom, (cx - r, y0, cz - b), (cx - a, y1, cz + b), mat, **kw)
        self.c(nom, (cx + a, y0, cz - b), (cx + r, y1, cz + b), mat, **kw)

    def margelle(self, nom, cx, cz, y0, y1, r, e, mat, **kw):
        """Anneau carre a coins abattus : quatre cotes et quatre coins, interieur vide (bassins, puits, cuves)."""
        a = max(2, demi_entier(r * .55))
        self.c(nom, (cx - a, y0, cz - r), (cx + a, y1, cz - r + e), mat, **kw)
        self.c(nom, (cx - a, y0, cz + r - e), (cx + a, y1, cz + r), mat, **kw)
        self.c(nom, (cx - r, y0, cz - a), (cx - r + e, y1, cz + a), mat, **kw)
        self.c(nom, (cx + r - e, y0, cz - a), (cx + r, y1, cz + a), mat, **kw)
        k = r - e // 2 - 1
        for sx in (-1, 1):
            for sz in (-1, 1):
                x0, x1 = sorted((cx + sx * a, cx + sx * k))
                z0, z1 = sorted((cz + sz * a, cz + sz * k))
                self.c(nom, (x0, y0, z0), (x1, y1, z1), mat, **kw)

    def pinacle(self, nom, cx, cz, y0, h, r, mat=PIERRE, pointe=OR):
        """Fleche a paliers qui s'effile, pointe d'or."""
        n = max(2, r)
        pas = max(1, (h - 3) // n)
        y = y0
        for i in range(n):
            self.boite(nom, r - i, y, y + pas, r - i, mat, cx=cx, cz=cz, ton=i * .15)
            y += pas
        self.boite(nom + '_pointe', 1, y, y0 + h, 1, pointe, cx=cx, cz=cz)

    def pyramide(self, nom, y0, h, rx, rz, mat, n=5, cx=0, cz=0, debord=0, **kw):
        """Toit a gradins : n etages, du debord au faitage. Le dernier etage atteint exactement y0 + h."""
        for i in range(n):
            t = i / n
            dx = max(1, demi_entier((rx + debord) * (1 - t)))
            dz = max(1, demi_entier((rz + debord) * (1 - t)))
            ya, yb = y0 + demi_entier(h * i / n), y0 + demi_entier(h * (i + 1) / n)
            if yb > ya:
                self.boite(nom, dx, ya, yb, dz, mat, cx=cx, cz=cz, **kw)
        return y0 + h

    # ------------------------------------------------------------------ arcs
    def arc(self, nom, cy, Rm, e, z0, z1, mat, n=5, cx=0, cle=None, **kw):
        """Arc plein cintre : n voussoirs par demi-arc, cle de voute au sommet. Rm : rayon moyen, e : epaisseur."""
        d = math.pi / 2 / n
        L = int(math.ceil(2 * Rm * math.sin(d / 2))) + 2
        L += L % 2
        e += e % 2
        for i in range(n):
            f = (i + .5) * d
            px, py = cx + demi_entier(Rm * math.cos(f)), cy + demi_entier(Rm * math.sin(f))
            self.s(nom, (px - e // 2, py - L // 2, z0), (px + e // 2, py + L // 2, z1), mat, rot=(0, 0, math.degrees(f)), pivot=(px, py, (z0 + z1) / 2), **kw)
        if cle:
            self.c(nom + '_cle', (cx - 4, cy + Rm - e // 2 - 1, z0 - 1), (cx + 4, cy + Rm + e // 2 + 3, z1 + 1), cle)

    def ogive_z(self, nom, cy, w, h, e, x0, x1, mat, n=4, cz=0, **kw):
        """Meme arc brise, mais dans le plan z-y : l'arc enjambe l'axe z, epaisseur de x0 a x1."""
        dc = (h * h - w * w) / (2 * w)
        R = w + dc
        f1 = math.atan2(h, dc)
        d = f1 / n
        L = int(math.ceil(2 * R * math.sin(d / 2))) + 2
        L += L % 2
        e += e % 2
        for i in range(n):
            f = (i + .5) * d
            pz, py = demi_entier(R * math.cos(f) - dc), cy + demi_entier(R * math.sin(f))
            for s in (1, -1):
                zc = cz + s * pz
                self.c(nom, (x0, py - L // 2, zc - e // 2), (x1, py + L // 2, zc + e // 2), mat, rot=(-s * math.degrees(f), 0, 0), pivot=((x0 + x1) / 2, py, zc), **kw)

    def mur_troue(self, nom, x0, x1, y0, y1, z0, z1, cx, cy, r, mat=PIERRE, **kw):
        """Mur droit perce d'un trou rond : un seul cube, decoupe en alpha sur ses deux grandes faces."""
        n = self.c(nom, (x0, y0, z0), (x1, y1, z1), mat, ajoure=True, **kw)

        def fn(ctx, face):
            X, Y = ctx.Plocal[..., 0], ctx.Plocal[..., 1]
            face[..., 3] = np.where(np.hypot(X - cx, Y - cy) < r, 0, face[..., 3])
        self.p.decal(n, 'north', fn)
        self.p.decal(n, 'south', fn)
        return n

    def ogive(self, nom, cy, w, h, e, z0, z1, mat, n=4, cx=0, **kw):
        """Arc brise : w demi-portee a mi-epaisseur, h hauteur de la cle au-dessus des naissances, e epaisseur."""
        dc = (h * h - w * w) / (2 * w)
        R = w + dc
        f1 = math.atan2(h, dc)
        d = f1 / n
        L = int(math.ceil(2 * R * math.sin(d / 2))) + 2
        L += L % 2
        e += e % 2
        for i in range(n):
            f = (i + .5) * d
            px = cx + demi_entier(R * math.cos(f) - dc)
            py = cy + demi_entier(R * math.sin(f))
            self.s(nom, (px - e // 2, py - L // 2, z0), (px + e // 2, py + L // 2, z1), mat, rot=(0, 0, math.degrees(f)), pivot=(px, py, (z0 + z1) / 2), **kw)

    # ------------------------------------------------------------------ feu, cristal, tissu
    def flamme(self, nom, cx, cz, y0, w, h, ramp='flamme'):
        """Deux plans croises decoupes, sur leur propre os qui ondule dans le clip chest."""
        pere = self.b
        o = self.os(self._n('feu_' + nom), (cx, y0, cz), pere)
        self.c(o + '_a', (cx - w, y0, cz), (cx + w, y0 + h, cz + 1), ('flamme', ramp), **CF)
        self.c(o + '_b', (cx, y0, cz - w), (cx + 1, y0 + h, cz + w), ('flamme', ramp), **CR)
        self.mouvants.append(('flamme', o, len(self.mouvants)))
        self.b = pere

    def bougie(self, nom, cx, cz, y0, h, r=2, mat=CIRE):
        self.boite(nom, r, y0, y0 + h, r, mat, cx=cx, cz=cz, base=4)
        self.flamme(nom, cx, cz, y0 + h, 2, 6)

    def cristal(self, nom, cx, cz, y0, h, r, mat=AME, angle=45, pointe=True, incl=(0, 0)):
        """Cristal taille : corps et pointe tournes de 45 degres."""
        rz, rx = incl
        rot_c = (rx, angle, rz)
        y1 = y0 + h
        self.c(nom, (cx - r, y0, cz - r), (cx + r, y1, cz + r), mat, rot=rot_c, pivot=(cx, y0, cz))
        if pointe:
            r2 = max(1, r // 2)
            self.c(nom + '_p', (cx - r2, y1, cz - r2), (cx + r2, y1 + max(2, h // 4), cz + r2), mat, rot=rot_c, pivot=(cx, y0, cz), ton=.6)

    def flottant(self, o, amp=1.5, ph=0.0):
        self.mouvants.append(('flotte', o, ph, amp))

    def balance(self, o, amp=3.0, ph=0.0, axe='x'):
        self.mouvants.append(('balance', o, ph, amp, axe))

    def tourne(self, o, tours=1):
        self.mouvants.append(('tourne', o, tours))

    def ondule(self, o, amp=6.0, ph=0.0, axe='y'):
        self.mouvants.append(('ondule', o, ph, amp, axe))

    def banniere(self, nom, x0, x1, y0, y1, z, ramp='rar_legendaire', embleme=True, pointe=True, epais=1):
        """Bannniere de tissu a bord or, decoupee en pointes, emblemes sur les deux faces."""
        w = x1 - x0
        h = y1 - y0
        m = []
        for r in range(h):
            t = (h - 1 - r)
            m.append('#' * w if t >= (w // 2 if pointe else 0) else ''.join('#' if abs(i - w / 2 + .5) >= (w / 2 - t) or t >= w // 2 else '.' for i in range(w)))
        n = self.c(nom, (x0, y0, z), (x1, y1, z + epais), ('tissu', ramp), bord=1, **CF)
        self._masque_alpha(n, m)
        if embleme and w >= 12 and h >= 14:
            yc = y1 - 3
            self.p.decal(n, 'north', stamp(EMBLEME, PAL, x1 - (w - 10) / 2, yc))
            self.p.decal(n, 'south', stamp(EMBLEME, PAL, x0 + (w - 10) / 2, yc, flip_on=('south',)))
        return n

    def _masque_alpha(self, nom_cube, masque):
        """Applique un masque ASCII en alpha sur les faces nord et sud d'un plan vertical (lu depuis le nord)."""
        def fn(ctx, face):
            H, W = face.shape[:2]
            for y in range(H):
                r = masque[min(int(y * len(masque) / H), len(masque) - 1)]
                for x in range(W):
                    xx = x if ctx.face == 'north' else W - 1 - x
                    if r[min(int(xx * len(r) / W), len(r) - 1)] != '#':
                        face[y, x, 3] = 0
        self.p.decal(nom_cube, 'north', fn)
        self.p.decal(nom_cube, 'south', fn)

    # ------------------------------------------------------------------ surfaces peintes
    def vitrail(self, nom, x0, x1, y0, y1, z, forme='ogive', palette=VITRAIL, cell=6, epais=1, plomb=('uni', 'fer_noir')):
        """Vitrail : plomb sombre, verres de couleur. forme : ogive (arc brise equilateral), arc (plein cintre), rect, rosace (ronde, secteurs et couronnes)."""
        n = self.c(nom, (x0, y0, z), (x1, y1, z + epais), plomb, base=1, **CF)
        pal = [tuple(int(c[i:i + 2], 16) for i in (1, 3, 5)) for c in palette]

        def fn(ctx, face):
            H, W = face.shape[:2]
            for y in range(H):
                for x in range(W):
                    xc = x + .5 - W / 2
                    if forme == 'ogive':
                        ha = W * .866
                        if y < ha and (abs(xc) + W / 2) ** 2 + (ha - y) ** 2 > W * W:
                            face[y, x, 3] = 0
                            continue
                    elif forme == 'arc':
                        if y < W / 2 and xc * xc + (W / 2 - y) ** 2 > W * W / 4:
                            face[y, x, 3] = 0
                            continue
                    elif forme == 'rosace':
                        yc = y + .5 - H / 2
                        r = math.hypot(xc, yc) / (W / 2)
                        if r > 1:
                            face[y, x, 3] = 0
                            continue
                        ang = (math.atan2(yc, xc) + math.pi) / (2 * math.pi)
                        sec = ang * 12
                        rr = r * 4
                        if r < .14 or abs(rr - round(rr)) < .1 or (r > .28 and abs(sec - round(sec)) < .06 / max(r, .3)):
                            continue
                        face[y, x, :3] = pal[(int(sec) // (1 if r > .6 else 2) + int(rr) * 2) % len(pal)]
                        continue
                    hy = H - 1 - y
                    a, b = (x + hy) / cell, (x - hy) / cell
                    ia, ib = math.floor(a), math.floor(b)
                    if a - ia < 1 / cell or b - ib < 1 / cell:
                        continue
                    face[y, x, :3] = pal[(ia * 7 + ib * 13) % len(pal)]
        self.p.decal(n, 'north', fn)
        self.p.decal(n, 'south', fn)
        return n

    def veines_or(self, nom_cube, faces=('north', 'south', 'east', 'west', 'up'), seuil=.006):
        """Veines d'or fines sur une pierre sombre : meme bruit pour tout le modele, les veines passent d'une face a l'autre."""
        def fn(ctx, face):
            n1, n2 = ctx.noise((.07, .1, .07), 91), ctx.noise((.2, .2, .2), 93)
            m = np.abs(n1 - .5 + .28 * (n2 - .5)) < seuil
            face[..., :3] = np.where(m[..., None], np.array((240, 200, 90)), face[..., :3])
        for f in faces:
            self.p.decal(nom_cube, f, fn)

    def fenetre(self, nom, cx, y0, w, h, z0, z1, cadre=None, vitrail=True, palette=VITRAIL, n=3, e=4):
        """Fenetre gothique : deux montants, ogive au-dessus, vitrail dedans. w : demi-largeur du jour ; h : hauteur des montants."""
        cadre = cadre or MARBRE
        ha = int(round(w * 1.73))
        self.c(nom + '_m', (cx + w, y0, z0), (cx + w + e, y0 + h, z1), cadre)
        self.c(nom + '_m', (cx - w - e, y0, z0), (cx - w, y0 + h, z1), cadre)
        self.ogive(nom + '_o', y0 + h, w + e // 2, int(round((w + e / 2) * 1.73)), e, z0, z1, cadre, n=n, cx=cx)
        if vitrail:
            zc = (z0 + z1) // 2
            self.vitrail(nom + '_v', cx - w, cx + w, y0, y0 + h + ha, zc, palette=palette)

    def rayonnage(self, nom, x0, x1, y0, y1, z0, z1, etage=20, palette=LIVRES, faces=('north',)):
        """Fond de bibliotheque : rayons de livres aux dos colores peints sur la face."""
        n = self.c(nom, (x0, y0, z0), (x1, y1, z1), BOIS_N)
        pal = [tuple(int(c[i:i + 2], 16) for i in (1, 3, 5)) for c in palette]

        def fn(ctx, face):
            H, W = face.shape[:2]
            for y in range(H):
                ry = (H - 1 - y) % etage
                if ry < 3:
                    face[y, :, :3] = (58, 38, 22) if ry == 2 else (34, 22, 13)
                    continue
                hligne = ry - 3
                x = 0
                rang = (H - 1 - y) // etage
                while x < W:
                    bw = 2 + ((x * 3 + rang * 7) * 2654435761 >> 7) % 3
                    bh = etage - 5 - ((x * 5 + rang * 11) * 2654435761 >> 9) % 4
                    c = pal[((x + rang * 5) * 2654435761 >> 5) % len(pal)]
                    for i in range(x, min(x + bw, W)):
                        if hligne < bh:
                            s = -18 if i == x else (14 if i == x + 1 else 0)
                            col = tuple(max(0, min(255, v + s)) for v in c)
                            if hligne == bh - 4:
                                col = (214, 176, 84)
                            face[y, i, :3] = col
                        else:
                            face[y, i, :3] = (18, 12, 8)
                    x += bw
        for f in faces:
            self.p.decal(n, f, fn)
        return n

    def rayures_tissu(self, nom_cube, faces, c1='#a82222', c2='#e8e0cc', pas=8, axe='u'):
        """Toile rayee, deux couleurs nettes (auvent, tente). axe u : bandes le long de la largeur de la face."""
        a = tuple(int(c1[i:i + 2], 16) for i in (1, 3, 5))
        b = tuple(int(c2[i:i + 2], 16) for i in (1, 3, 5))

        def fn(ctx, face):
            q = ctx.Iu if axe == 'u' else ctx.Iv
            m = np.mod(q, pas * 2) < pas
            base = np.where(m[..., None], np.array(a), np.array(b))
            face[..., :3] = np.clip(base * (.86 + .14 * (ctx.Iv % 3 != 0))[..., None], 0, 255).astype(np.uint8)
        for f in faces:
            self.p.decal(nom_cube, f, fn)

    def grille(self, nom, x0, x1, y0, y1, z, pas=8, large=2, traverses=(), mat=('uni', 'fer_noir'), lances=0, epais=1, **kw):
        """Grille ou herse en un seul plan decoupe : barreaux tous les pas, traverses aux hauteurs donnees (y modele), lances en haut."""
        W, H = x1 - x0, y1 - y0
        rows = []
        for r in range(H):
            y = y1 - 1 - r
            line = []
            for i in range(W):
                bar = (i + pas // 2) % pas < large
                tra = any(t0 <= y < t1 for (t0, t1) in traverses)
                ok = bar or tra or i < large or i >= W - large
                if lances and r < lances:
                    ok = bar and abs(((i + pas // 2) % pas) - (large - 1) / 2) <= (r + 1) * 0.5 * large / max(1, lances)
                line.append('#' if ok else '.')
            rows.append(''.join(line))
        n = self.c(nom, (x0, y0, z), (x1, y1, z + epais), mat, **kw, **CF)
        self._masque_alpha(n, rows)
        return n

    def caisse(self, nom, a, b, ton=0.0, mat='bois', ramp='bois_clair', **kw):
        return self.c(nom, a, b, ('caisse', ramp), ton=ton, **kw)

    def roue(self, nom, cx, cy, z0, z1, r, ramp='bois_sombre', moyeu=FER_N):
        """Roue en un plan decoupe : jante, six rayons, moyeu. cx, cy : centre ; z0, z1 : epaisseur."""
        n = 2 * r
        rows = []
        for j in range(n):
            ligne = []
            for i in range(n):
                x, y = i + .5 - r, j + .5 - r
                d = math.hypot(x, y)
                a = (math.atan2(y, x) + math.pi) / (math.pi / 3)
                rayon = d < r and abs(a - round(a)) * d * (math.pi / 3) < 1.1
                ligne.append('#' if (r - 2.6 <= d < r) or rayon or d < 3 else '.')
            rows.append(''.join(ligne))
        self.c(nom, (cx - r, cy - r, z0), (cx + r, cy + r, z1), ('plan', ramp), base=3, masque=rows, **CF)
        self.c(nom + '_moyeu', (cx - 2, cy - 2, z0 - 1), (cx + 2, cy + 2, z1 + 1), moyeu)

    def rune(self, nom_cube, face, rows, along, y, flip=False):
        pal = dict(PAL)
        self.p.decal(nom_cube, face, stamp(rows, pal, along, y, flip_on=(face,) if flip else ()))

    # ------------------------------------------------------------------ fin
    def fin(self, T=4.0):
        p = self.p
        if self.mouvants:
            tr = {}
            for m in self.mouvants:
                k = m[0]
                if k == 'flamme':
                    o, i = m[1], m[2]
                    ph = (i * .37) % 1
                    tr[o] = {'scale': keys(T, lambda q, ph=ph: (1 + .08 * math.sin(2 * math.pi * (3 * q + ph)), 1 + .16 * math.sin(2 * math.pi * (4 * q + ph + .2)), 1 + .08 * math.sin(2 * math.pi * (3 * q + ph))), 24)}
                elif k == 'flotte':
                    o, ph, amp = m[1], m[2], m[3]
                    tr[o] = {'position': rot(T, y=sin(amp, 1, ph), n=16)}
                elif k == 'balance':
                    o, ph, amp, axe = m[1], m[2], m[3], m[4]
                    tr[o] = {'rotation': rot(T, **{axe: sin(amp, 1, ph)}, n=16)}
                elif k == 'tourne':
                    o, tours = m[1], m[2]
                    tr[o] = {'rotation': keys(T, lambda q, t=tours: (0, 360 * t * q, 0), 16)}
                elif k == 'aiguille':
                    o, tours = m[1], m[2]
                    tr[o] = {'rotation': keys(T, lambda q, t=tours: (0, 0, -360 * t * q), 48)}
                elif k == 'ondule':
                    o, ph, amp, axe = m[1], m[2], m[3], m[4]
                    tr[o] = {'rotation': rot(T, **{axe: sin(amp, 2, ph)}, n=16)}
            p.animation('chest', T, tr, True)
        return p
