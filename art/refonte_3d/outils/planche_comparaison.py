#!/usr/bin/env python3
"""Planches AVANT / APRES a partir des captures reelles de Blockbench (meme vue, meme echelle)."""
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
AV = ROOT / 'apercus/avant'
AP = ROOT / 'apercus/apres'
OUT = ROOT / 'apercus/validation'
OUT.mkdir(parents=True, exist_ok=True)


def font(size, bold=False):
    for f in (('C:/Windows/Fonts/segoeuib.ttf' if bold else 'C:/Windows/Fonts/segoeui.ttf'), 'C:/Windows/Fonts/arial.ttf'):
        try:
            return ImageFont.truetype(f, size)
        except OSError:
            continue
    return ImageFont.load_default()


def crop(im):
    im = im.convert('RGBA')
    b = im.split()[3].getbbox()
    return im.crop(b) if b else im


BG = (30, 28, 36)
PANEL = (214, 216, 222)
GOLD = (230, 190, 90)


def sheet(title, sub, rows, name, cell_w=800, gap=16, head=96, foot=None):
    """rows : [(legende, fichier_avant, fichier_apres, hauteur_cible)]. Pour chaque ligne, l'echelle est commune aux deux images."""
    W = cell_w * 2 + gap * 3
    imgs = []
    H = head
    for lab, a, b, target in rows:
        A, B = crop(Image.open(a)), crop(Image.open(b))
        k = min((cell_w - 40) / max(A.width, B.width), (target - 44) / max(A.height, B.height))
        A2 = A.resize((max(1, round(A.width * k)), max(1, round(A.height * k))), Image.LANCZOS if k < 1 else Image.NEAREST)
        B2 = B.resize((max(1, round(B.width * k)), max(1, round(B.height * k))), Image.LANCZOS if k < 1 else Image.NEAREST)
        imgs.append((lab, A2, B2, target))
        H += target + gap
    foot_h = 0
    if foot:
        foot_h = 30 * len(foot) + 16
        H += foot_h
    S = Image.new('RGB', (W, H), BG)
    d = ImageDraw.Draw(S)
    d.text((gap, 12), title, fill=(255, 255, 255), font=font(30, True))
    d.text((gap, 52), sub, fill=(190, 190, 200), font=font(17))
    y = head
    for i, (lab, A2, B2, target) in enumerate(imgs):
        for j, (im, tag) in enumerate(((A2, 'AVANT'), (B2, 'APRÈS'))):
            x = gap + j * (cell_w + gap)
            d.rectangle((x, y, x + cell_w, y + target), fill=PANEL)
            S.paste(im, (x + (cell_w - im.width) // 2, y + 34 + (target - 34 - im.height) // 2), im)
            d.rectangle((x, y, x + cell_w, y + 28), fill=(44, 40, 54))
            d.text((x + 10, y + 3), f'{tag}  ·  {lab}', fill=GOLD if j else (205, 205, 212), font=font(16, True))
        y += target + gap
    if foot:
        for k, line in enumerate(foot):
            d.text((gap, y + k * 30), line, fill=(225, 225, 232), font=font(18))
    S.save(OUT / name, optimize=True)
    print(name, S.size, (OUT / name).stat().st_size // 1024, 'Ko')


def mount():
    sheet('Sanglier de guerre : monture représentative', 'Captures réelles de Blockbench 5.2.2 (même vue, même échelle par ligne). Pas encore testé en jeu.',
          [('3/4 droit', AV / 'sanglier_iso_droite.png', AP / 'sanglier_iso_droite.png', 660),
           ('Profil', AV / 'sanglier_profil.png', AP / 'sanglier_profil.png', 470),
           ('Face', AV / 'sanglier_face.png', AP / 'sanglier_face.png', 470),
           ('Cavalier de contrôle (mannequin non livré, position calculée comme le jeu)', AV / 'sanglier_cavalier_iso.png', AP / 'sanglier_cavalier_iso.png', 720)],
          'sanglier_avant_apres.png',
          foot=['Avant : 89 cubes, 534 faces, texture 512×512, densité 1,8 à 84 texels/unité, 74 % des coordonnées hors grille, 5 paires de faces qui scintillent, 7 os, repos + marche.',
                'Après : 87 cubes, 439 faces, texture 256×256, densité unique (2 texels/unité = 16 par bloc une fois rendu ×2), tout sur la grille, 0 face qui scintille, 18 os, repos + marche + galop + saut (galop et saut à raccorder).'])


def block():
    sheet('Puits de la citadelle : bloc représentatif', 'Captures réelles de Blockbench 5.2.2 (copies réduites à 1/2 pour tenir dans le cadre fixe, même échelle par ligne). Pas encore testé en jeu.',
          [('3/4 droit', AV / 'deco_puits_iso_droite.png', AP / 'deco_puits_iso_droite.png', 900),
           ('Face', AV / 'deco_puits_face.png', AP / 'deco_puits_face.png', 760),
           ('Dessus', AV / 'deco_puits_dessus.png', AP / 'deco_puits_dessus.png', 560)],
          'puits_avant_apres.png',
          foot=['Avant : 67 cubes, 398 faces, atlas partagé 2048×1792, 5,3 texels/unité (bruit lissé), 68 paires de faces qui scintillent, icône sur fond bleu opaque.',
                'Après : 60 cubes, 289 faces, texture 256×256, 1 texel/unité = 16 par bloc comme le vanilla, 0 face qui scintille, mêmes 45 cellules de collision, seau animé (boucle « chest » déjà gérée par le code).'])


if __name__ == '__main__':
    mount()
    block()
