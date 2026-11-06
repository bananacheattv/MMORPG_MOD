#!/usr/bin/env python3
"""Textures de blocs au format Minecraft : materiaux 16x16 raccordables, minerais, cristaux, cultures en 4 stades, Lucky Block, panneaux de classement.
Les modeles de cube, dalle et escalier ne changent pas : seules leurs textures changent. Meme nom de fichier, meme taille. Rampes de couleurs limitees, pas de degrade lisse.
    python textures_blocs.py"""
import math
import random
import sys
from pathlib import Path

import numpy as np
from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path(__file__).parent))
from materiaux import RAMPS

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'livraison/src/main/resources/assets/mmorpg/textures/block'
ORI = ROOT / 'originaux/git_main/src/main/resources/assets/mmorpg/textures/block'


def rp(nom):
    return RAMPS[nom]


def bruit_tuile(n, cases, seed):
    """Bruit lisse raccordable : valeurs aleatoires sur une grille periodique, interpolees."""
    r = np.random.default_rng(seed)
    g = r.random((cases, cases))
    xs = np.arange(n) / n * cases
    X, Y = np.meshgrid(xs, xs)
    x0, y0 = np.floor(X).astype(int), np.floor(Y).astype(int)
    fx, fy = X - x0, Y - y0
    fx, fy = fx * fx * (3 - 2 * fx), fy * fy * (3 - 2 * fy)
    x1, y1 = (x0 + 1) % cases, (y0 + 1) % cases
    x0, y0 = x0 % cases, y0 % cases
    return g[y0, x0] * (1 - fx) * (1 - fy) + g[y0, x1] * fx * (1 - fy) + g[y1, x0] * (1 - fx) * fy + g[y1, x1] * fx * fy


def peint(v, ramp):
    R = rp(ramp) if isinstance(ramp, str) else ramp
    idx = np.clip(np.rint(v).astype(int), 0, len(R) - 1)
    out = np.zeros(v.shape + (4,), np.uint8)
    out[..., :3] = R[idx]
    out[..., 3] = 255
    return out


def sauve(a, nom):
    OUT.mkdir(parents=True, exist_ok=True)
    Image.fromarray(a, 'RGBA').save(OUT / f'{nom}.png')


def hexrgb(c):
    return tuple(int(c[i:i + 2], 16) for i in (1, 3, 5))


N = 16
X, Y = np.meshgrid(np.arange(N), np.arange(N))


def marbre(seed=1, base=3.6):
    n1, n2 = bruit_tuile(N, 3, seed), bruit_tuile(N, 6, seed + 1)
    v = base + (n1 - .5) * 1.1 + (n2 - .5) * .5
    vein = np.abs(n1 - .5 + .35 * (n2 - .5)) < .035
    v = np.where(vein, base - 1.1, v)
    return v


def tex_marbre():
    a = peint(marbre(), 'marbre')
    sauve(a, 'marbre')


def tex_marbre_dore():
    a = peint(marbre(3), 'marbre')
    O = rp('or')
    bord = (X < 2) | (Y < 2) | (X > 13) | (Y > 13)
    a[bord, :3] = O[3]
    a[((X == 0) | (Y == 0)), :3] = O[4]
    a[((X == 15) | (Y == 15)), :3] = O[1]
    a[(X == 1) & (Y > 0) & (Y < 15), :3] = O[4]
    a[(X == 14) & (Y > 0) & (Y < 15), :3] = O[2]
    d = np.abs(X - 7.5) + np.abs(Y - 7.5)
    a[(d >= 3) & (d < 4), :3] = O[3]
    a[(d < 2), :3] = O[4]
    sauve(a, 'marbre_dore')


def tex_marbre_poli():
    v = marbre(5, 4.2)
    v = v + .25 * (((X % 8) < 2) & ((Y % 8) < 2))
    v = np.where(((X % 8) == 7) | ((Y % 8) == 7), 2.2, v)
    v = np.where(((X % 8) == 0) | ((Y % 8) == 0), v + .7, v)
    sauve(peint(v, 'marbre'), 'marbre_poli')


def tex_or_ouvrage():
    q = np.abs(((X + 8) % 16) - 7.5) + np.abs(((Y + 8) % 16) - 7.5)
    v = 3.2 + np.where(q < 3, 1.6, np.where(q < 5, -1.2, np.where(q < 8, .7, np.where(q < 9.5, -1.0, .2))))
    v = v + .4 * (bruit_tuile(N, 8, 9) - .5)
    v = np.where((X == 0) | (Y == 0), v + .6, v)
    v = np.where((X == 15) | (Y == 15), v - .9, v)
    sauve(peint(v, 'or'), 'or_ouvrage')


def tex_fer_noirci():
    v = 2.4 + (bruit_tuile(N, 4, 11) - .5) * 1.1
    v = v + .7 * (((X % 8) == 0) | ((Y % 8) == 0)) - 1.6 * (((X % 8) == 7) | ((Y % 8) == 7))
    a = peint(v, 'fer_noir')
    for cx, cy in ((3, 3), (11, 3), (3, 11), (11, 11)):
        a[cy, cx, :3] = hexrgb('#a0a6b2')
        a[cy, cx + 1, :3] = hexrgb('#575c68')
        a[cy + 1, cx, :3] = hexrgb('#575c68')
        a[cy + 1, cx + 1, :3] = hexrgb('#a83a1c')
    sauve(a, 'fer_noirci')


def tex_bois_ancien():
    col = np.floor(X / 5)
    r = np.random.default_rng(13)
    t = r.random(4)[col.astype(int) % 4] - .5
    n = bruit_tuile(N, 8, 14)
    v = 2.4 + t * 1.4 + (((X * 7 + np.floor(Y / 5) * 3) % 5) == 0) * -.7 + (n - .5) * .9
    v = np.where((X % 5) == 4, .6, v)
    a = peint(v, 'bois_sombre')
    for cx, cy in ((2, 2), (7, 13), (12, 6)):
        a[cy, cx, :3] = hexrgb('#a0a6b2')
        a[cy + 1, cx, :3] = hexrgb('#3a3e49')
    sauve(a, 'bois_ancien')


def briques(ramp, seed, assise=4, long=8, base=3.2):
    r = np.random.default_rng(seed)
    h = r.random((8, 8))
    row = np.floor(Y / assise).astype(int)
    sx = X + (row % 2) * (long // 2)
    col = np.floor(sx / long).astype(int)
    v = base + (h[row % 8, col % 8] - .5) * 1.5
    v = v + (bruit_tuile(N, 8, seed + 1) - .5) * .5
    ly, lx = Y - row * assise, sx - col * long
    v = np.where(ly == 0, v + .8, v)
    v = np.where(lx == 0, v + .3, v)
    v = np.where((ly == assise - 1) | (lx == long - 1), base - 2, v)
    return peint(v, ramp)


def tex_roche_celeste():
    n1, n2 = bruit_tuile(N, 3, 21), bruit_tuile(N, 7, 22)
    v = 3 + (n1 - .5) * 1.6 + (n2 - .5) * 1
    fis = np.abs(n1 - .52 + .3 * (n2 - .5)) < .03
    v = np.where(fis, 1, v)
    a = peint(v, 'pierre')
    for x, y, c in ((2, 12, '#7be0d0'), (9, 3, '#7be0d0'), (13, 9, '#5cc4a0'), (5, 6, '#d8f6ff')):
        a[y, x, :3] = hexrgb(c)
    sauve(a, 'roche_celeste')


def tuile(nom, ramp, seed):
    """Tuiles en ecailles raccordables : rangs decales, bas arrondi, jamais de simples rayures."""
    r = np.random.default_rng(seed)
    row = np.floor(Y / 4).astype(int)
    sx = X + (row % 2) * 4
    col = np.floor(sx / 8).astype(int)
    v = 3.2 + (r.random((4, 4))[row % 4, col % 4] - .5) * 1.2
    ly, lx = Y - row * 4, sx - col * 8
    v = np.where(ly == 0, v + 1, v)
    v = np.where(ly == 3, v - 1, v)
    v = np.where((ly == 3) & ((lx <= 1) | (lx >= 6)), 1, v)
    v = np.where((ly >= 2) & ((lx == 0) | (lx == 7)), 1, v)
    v = v + (bruit_tuile(N, 8, seed + 1) - .5) * .4
    sauve(peint(v, ramp), nom)


def tex_dalle_runique():
    v = 2.2 + (bruit_tuile(N, 6, 31) - .5) * .7
    v = np.where(((X % 8) == 7) | ((Y % 8) == 7), .6, v)
    v = np.where(((X % 8) == 0) | ((Y % 8) == 0), v + .6, v)
    a = peint(v, 'pierre_noire')
    G = ['#4fe0d0', '#8ff5e6']
    R = [[(2, 2), (3, 2), (4, 2), (2, 3), (2, 4), (3, 4), (4, 4), (4, 5)], [(10, 2), (13, 2), (11, 3), (12, 3), (11, 4), (12, 4), (10, 5), (13, 5)], [(3, 10), (2, 11), (3, 11), (4, 11), (3, 12), (3, 13)], [(10, 10), (11, 10), (12, 10), (12, 11), (11, 12), (11, 13), (13, 12)]]
    for gl in R:
        for i, (x, y) in enumerate(gl):
            a[y, x, :3] = hexrgb(G[i % 2])
    sauve(a, 'dalle_runique')


def tex_colonne():
    f = np.array([0, 1, 2, 3, 2, 1, 0, 1, 2, 3, 2, 1, 0, 1, 2, 1])[X]
    v = 2.6 + f * .75 + (bruit_tuile(N, 5, 33) - .5) * .4
    sauve(peint(v, 'marbre'), 'colonne_marbre')
    d = np.hypot(X - 7.5, Y - 7.5)
    v = np.where(d < 4.2, 4.6 + (bruit_tuile(N, 5, 34) - .5) * .7, np.where(d < 7.6, 3.4, 2.6))
    a = peint(v, 'marbre')
    O = rp('or')
    a[(d >= 5.2) & (d < 6.6), :3] = O[3]
    a[(d >= 6.6) & (d < 7.6), :3] = O[2]
    sauve(a, 'colonne_marbre_top')


def cristal(nom, ramp, cadre):
    u, t = (X + .5) / N, (Y + .5) / N
    s1, s2, s3 = t > .22 + .55 * u, t > 1.05 - .75 * u, u > .45 + .22 * t
    fid = s1.astype(int) + 2 * s2.astype(int) + 4 * s3.astype(int)
    v = 2.2 + np.array([0, 1, -.6, .6, 1.6, -.2, .9, .3])[fid]
    lim = np.zeros_like(fid, dtype=bool)
    lim[1:, :] |= fid[1:, :] != fid[:-1, :]
    lim[:, 1:] |= fid[:, 1:] != fid[:, :-1]
    v = np.where(lim, 4.8, v)
    a = peint(v, ramp)
    C = rp(cadre)
    b = (X == 0) | (Y == 0) | (X == 15) | (Y == 15)
    a[b, :3] = C[2]
    a[(X == 0) | (Y == 0), :3] = C[3]
    a[(X == 15) | (Y == 15), :3] = C[1]
    sauve(a, nom)


AMAS = ((3, 3), (11, 4), (6, 10), (12, 12))


def minerai(nom, ramp_ore, hote='pierre', base=1.6, seed=41):
    """Minerai : roche sombre, amas irreguliers de tailles differentes, pixel clair au coeur, ombre dessous. Places au hasard, en se repetant sans couture."""
    n1, n2 = bruit_tuile(N, 4, seed), bruit_tuile(N, 8, seed + 1)
    v = base + (n1 - .5) * 1.5 + (n2 - .5) * .8
    a = peint(v, hote)
    O = rp(ramp_ore)
    r = random.Random(seed)
    centres = []
    while len(centres) < 5:
        c = (r.randrange(N), r.randrange(N))
        if all(min(abs(c[0] - e[0]), N - abs(c[0] - e[0])) + min(abs(c[1] - e[1]), N - abs(c[1] - e[1])) >= 6 for e in centres):
            centres.append(c)
    for k, (cx, cy) in enumerate(centres):
        taille = (7, 3, 5, 2, 4)[k]
        pts = [(0, 0)]
        while len(pts) < taille:
            px, py = r.choice(pts)
            dx, dy = r.choice(((1, 0), (-1, 0), (0, 1), (0, -1)))
            if (px + dx, py + dy) not in pts:
                pts.append((px + dx, py + dy))
        for (dx, dy) in pts:
            x, y = (cx + dx) % N, (cy + dy + 1) % N
            if (dx, dy + 1) not in pts:
                a[y, x, :3] = (a[y, x, :3] * .55).astype(np.uint8)
        for i, (dx, dy) in enumerate(pts):
            x, y = (cx + dx) % N, (cy + dy) % N
            haut = (dx, dy - 1) not in pts
            a[y, x, :3] = O[-1] if i == 0 else (O[-2] if haut else O[-3])
    sauve(a, nom)


def tex_veine():
    n1, n2 = bruit_tuile(N, 4, 51), bruit_tuile(N, 8, 52)
    v = 1.2 + (n1 - .5) * 1.2 + (n2 - .5) * .8
    a = peint(v, 'pierre_noire')
    for cx, cy in AMAS:
        a[cy, cx, :3] = (10, 9, 11)
        a[cy, cx + 1, :3] = (10, 9, 11)
        a[cy + 1, cx, :3] = (16, 14, 16)
        a[cy - 1, cx, :3] = rp('pierre_noire')[3]
    sauve(a, 'veine_epuisee')


def tex_lucky():
    O = rp('or')
    a = np.zeros((N, N, 4), np.uint8)
    a[..., 3] = 255
    a[..., :3] = O[3]
    a[(X < 2) | (Y < 2) | (X > 13) | (Y > 13), :3] = O[2]
    a[(X == 0) | (Y == 0), :3] = O[5]
    a[(X == 15) | (Y == 15), :3] = O[1]
    a[((X == 2) & (Y > 1) & (Y < 14)) | ((Y == 2) & (X > 1) & (X < 14)), :3] = O[1]
    a[((X == 13) & (Y > 1) & (Y < 14)) | ((Y == 13) & (X > 1) & (X < 14)), :3] = O[4]
    Q = ['..####..', '.##..##.', '.##..##.', '.....##.', '....##..', '...##...', '...##...', '........', '...##...']
    for j, l in enumerate(Q):
        for i, c in enumerate(l):
            if c == '#':
                a[j + 3, i + 4, :3] = (255, 248, 214)
                a[j + 4, i + 5, :3] = np.minimum(a[j + 4, i + 5, :3], O[1]) if j + 1 < len(Q) and i + 1 < 8 and Q[j + 1][i + 1] != '#' else a[j + 4, i + 5, :3]
    sauve(a, 'lucky_block')


# ------------------------------------------------------------------ cultures
def culture(nom, tige, feuille, fruit, forme):
    """Quatre tiges par texture (modele crop) : pousse, tige feuillue, boutons, plante mure. forme : baies, cosses, givre, bulbes, epines."""
    T, F, O = rp(tige), rp(feuille), rp(fruit)
    hauteurs = (4, 8, 12, 15)
    xs = (2, 6, 10, 13)
    for s in range(4):
        a = np.zeros((N, N, 4), np.uint8)
        h = hauteurs[s]
        for k, x in enumerate(xs):
            hh = h - (k % 2)
            for j in range(hh):
                y = 15 - j
                a[y, x, :3] = T[3] if j % 3 else T[2]
                a[y, x, 3] = 255
                if j >= 2 and j % 3 == 2:                    # paire de feuilles
                    for dx, c in ((-1, F[3]), (1, F[4])):
                        xf = x + dx
                        if 0 <= xf < N:
                            a[y, xf, :3] = c
                            a[y, xf, 3] = 255
                    xf = x + (1 if j % 2 else -1) * 2
                    if 0 <= xf < N and s >= 1:
                        a[y - 1, xf, :3] = F[2]
                        a[y - 1, xf, 3] = 255
            top = 15 - hh + 1
            if s >= 2:
                pts = []
                if forme == 'baies':
                    pts = [(0, -1), (1, 0), (-1, 0)] if s == 3 else [(0, -1)]
                elif forme == 'cosses':
                    pts = [(-1, 0), (1, 0), (0, -1), (-1, 1), (1, 1)] if s == 3 else [(0, -1)]
                elif forme == 'givre':
                    pts = [(0, -1), (0, 0), (-1, 0), (1, 0), (0, 1)] if s == 3 else [(0, -1)]
                elif forme == 'bulbes':
                    pts = [(0, -1), (-1, -1), (1, -1), (0, 0)] if s == 3 else [(0, -1)]
                elif forme == 'epines':
                    pts = [(0, -1), (0, -2), (1, -1), (-1, -1)] if s == 3 else [(0, -1)]
                for i, (dx, dy) in enumerate(pts):
                    xx, yy = x + dx, top + dy
                    if 0 <= xx < N and 0 <= yy < N:
                        a[yy, xx, :3] = O[-1] if i == 0 else O[-2]
                        a[yy, xx, 3] = 255
                if s == 3 and forme in ('baies', 'cosses'):
                    for j in (4, 8):
                        yy = 15 - j
                        if yy > top and x + 1 < N:
                            a[yy, x + 1, :3] = O[-2]
                            a[yy, x + 1, 3] = 255
        sauve(a, f'{nom}_s{s}')


# ------------------------------------------------------------------ classements
TITRES = {'niveau': ('NIVEAUX', 'des aventuriers', 'or_corne'), 'monstres': ('CHASSEURS', 'de monstres', 'venin'), 'arene': ('GLADIATEURS', "de l'arène", 'rar_rare'), 'fortune': ('FORTUNES', "d'Aeloria", 'or'), 'boss': ('TUEURS DE BOSS', 'boss vaincus', 'rar_epique'),
          'donjons': ('CONQUÉRANTS', 'de donjons', 'rar_peu_commun'), 'quetes': ('HÉROS', 'des quêtes', 'rar_legendaire'), 'guilde_niveau': ('GUILDES', 'niveau', 'rar_epique'), 'guilde_banque': ('GUILDES', 'fortune', 'rar_legendaire'), 'guilde_gardiens': ('GUILDES', 'gardiens vaincus', 'rar_peu_commun')}


def texte_net(d, img, txt, y, taille, couleur, ombre=(10, 8, 16), police='georgiab.ttf', seuil=118):
    """Texte rendu net : seuil sans anti-crenelage, ombre portee d'un pixel, centre, reduit s'il deborde entre les motifs."""
    f = ImageFont.truetype('C:/Windows/Fonts/' + police, taille)
    w = d.textlength(txt, font=f)
    while w > 150 and taille > 7:
        taille -= 1
        f = ImageFont.truetype('C:/Windows/Fonts/' + police, taille)
        w = d.textlength(txt, font=f)
    x = (img.width - w) / 2
    calque = Image.new('L', img.size, 0)
    ImageDraw.Draw(calque).text((x, y), txt, fill=255, font=f)
    masque = calque.point(lambda v: 255 if v > seuil else 0)
    img.paste(ombre, (1, 1), masque)
    img.paste(couleur, (0, 0), masque)


def classement(cle):
    t1, t2, ramp = TITRES[cle]
    R = rp(ramp)
    W, H = 192, 112
    img = Image.new('RGBA', (W, H), tuple(rp('pierre_noire')[0]) + (255,))
    d = ImageDraw.Draw(img)
    # fond et bandes : memes hauteurs que l'original, le texte du jeu vient s'y poser
    ori = Image.open(ORI / f'classement_{cle}.png').convert('RGBA')
    F = rp('pierre_noire')
    for y in range(24, H - 3):
        d.line([(3, y), (W - 4, y)], fill=tuple(F[0]) + (255,))
    for y0 in (25, 41, 57, 73, 89):
        d.rectangle([8, y0, W - 9, y0 + 7], fill=tuple(F[1]) + (255,))
        d.line([(8, y0), (W - 9, y0)], fill=tuple(F[2]) + (255,))
    # bandeau de titre
    for y in range(3, 23):
        d.line([(3, y), (W - 4, y)], fill=tuple((R[1] * .55 + R[0] * .45).astype(int).tolist()) + (255,))
    d.line([(3, 23), (W - 4, 23)], fill=tuple(R[3]) + (255,))
    # cadre or et couleur du classement
    O = rp('or')
    d.rectangle([0, 0, W - 1, H - 1], outline=tuple(O[3]) + (255,))
    d.rectangle([1, 1, W - 2, H - 2], outline=tuple(O[2]) + (255,))
    d.rectangle([2, 2, W - 3, H - 3], outline=tuple(R[2]) + (255,))
    for x, y in ((0, 0), (W - 5, 0), (0, H - 5), (W - 5, H - 5)):
        d.rectangle([x, y, x + 4, y + 4], fill=tuple(O[4]) + (255,))
        d.point((x + 2, y + 2), fill=tuple(O[1]) + (255,))
    img_rgb = img.convert('RGB')
    d2 = ImageDraw.Draw(img_rgb)
    texte_net(d2, img_rgb, t1, 1, 13, tuple(int(v) for v in R[4]))
    texte_net(d2, img_rgb, t2, 13, 9, (240, 234, 222), police='verdana.ttf', seuil=96)
    # petits losanges decoratifs
    for x in (14, W - 15):
        for dx, dy in ((0, 0), (1, 1), (-1, 1), (0, 2), (2, 2), (-2, 2), (1, 3), (-1, 3), (0, 4)):
            d2.point((x + dx, 9 + dy), fill=tuple(int(v) for v in O[4]))
    out = img_rgb.convert('RGBA')
    OUT.mkdir(parents=True, exist_ok=True)
    out.save(OUT / f'classement_{cle}.png')


def cadre_classement():
    v = 2 + (bruit_tuile(N, 5, 61) - .5) * .9
    v = np.where((X % 8) == 7, .5, v)
    a = peint(v, 'fer_noir')
    sauve(a, 'classement_cadre')


def main():
    tex_marbre()
    tex_marbre_dore()
    tex_marbre_poli()
    tex_or_ouvrage()
    tex_fer_noirci()
    tex_bois_ancien()
    sauve(briques('rar_rare', 15, base=3.2), 'pierre_azur')
    sauve(briques('sable', 17, base=4), 'pierre_solaire')
    tex_roche_celeste()
    tuile('tuile_azur', 'turquoise', 23)
    tuile('tuile_ecarlate', 'rouge', 24)
    tuile('tuile_givre', 'glace', 25)
    tuile('tuile_pourpre', 'tuile_violette', 26)
    tuile('tuile_sylve', 'feuillage', 27)
    tex_dalle_runique()
    tex_colonne()
    cristal('cristal_ambre', 'or_corne', 'rar_legendaire')
    cristal('cristal_arcane', 'amethyste', 'tuile_violette')
    cristal('cristal_azur', 'glace', 'rar_rare')
    minerai('minerai_ambrasite', 'flamme', 'pierre_noire', 1.8, 41)
    minerai('minerai_argentite', 'fer', 'pierre_noire', 2, 43)
    minerai('minerai_glacium', 'glace', 'pierre', 1.4, 45)
    minerai('minerai_neantite', 'amethyste', 'pierre_noire', .9, 47)
    minerai('minerai_verdite', 'feuillage', 'pierre', 1.6, 49)
    tex_veine()
    tex_lucky()
    culture('culture_ambre', 'bois_clair', 'feuillage', 'flamme', 'baies')
    culture('culture_argent', 'bois_clair', 'feuillage', 'plume_blanche', 'cosses')
    culture('culture_givre', 'turquoise', 'turquoise', 'glace', 'givre')
    culture('culture_lueur', 'feuillage', 'feuillage', 'rune', 'bulbes')
    culture('culture_neant', 'pourpre', 'pourpre', 'amethyste', 'epines')
    for k in TITRES:
        classement(k)
    cadre_classement()
    print('ok')


def planche():
    noms = ['marbre', 'marbre_dore', 'marbre_poli', 'or_ouvrage', 'fer_noirci', 'bois_ancien', 'pierre_azur', 'pierre_solaire', 'roche_celeste', 'tuile_azur', 'tuile_ecarlate', 'tuile_givre', 'tuile_pourpre', 'tuile_sylve', 'dalle_runique', 'colonne_marbre', 'colonne_marbre_top', 'cristal_ambre', 'cristal_arcane', 'cristal_azur', 'minerai_ambrasite', 'minerai_argentite', 'minerai_glacium', 'minerai_neantite', 'minerai_verdite', 'veine_epuisee', 'lucky_block']
    for c in ('ambre', 'argent', 'givre', 'lueur', 'neant'):
        noms += [f'culture_{c}_s{i}' for i in range(4)]
    Z, cols = 96, 12
    rows = (len(noms) + cols - 1) // cols
    sheet = Image.new('RGB', (cols * (Z + 6), rows * (Z + 18) + 2 * 230), (205, 206, 212))
    d = ImageDraw.Draw(sheet)
    for i, n in enumerate(noms):
        im = Image.open(OUT / f'{n}.png').convert('RGBA')
        # raccord : la texture est montree sur deux tuiles pour verifier qu'elle se repete
        im = im.resize((Z, Z), Image.NEAREST)
        x, y = (i % cols) * (Z + 6), (i // cols) * (Z + 18)
        sheet.paste(im, (x, y + 14), im)
        d.text((x + 1, y + 1), n.replace('culture_', 'c_').replace('minerai_', 'm_'), fill=(20, 20, 30))
    y0 = rows * (Z + 18)
    for i, k in enumerate(TITRES):
        im = Image.open(OUT / f'classement_{k}.png').convert('RGB').resize((240, 140), Image.NEAREST)
        sheet.paste(im, ((i % 5) * 244, y0 + (i // 5) * 150))
    (ROOT / 'apercus/travail').mkdir(parents=True, exist_ok=True)
    sheet.save(ROOT / 'apercus/travail/textures_apres.png')
    print(sheet.size)


if __name__ == '__main__':
    main()
    planche()
