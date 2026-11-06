#!/usr/bin/env python3
"""Planche de direction artistique : rampes de couleurs reellement utilisees par les peintres, et texture reelle des pilotes."""
import sys
from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

sys.path.insert(0, str(Path(__file__).parent))
from materiaux import RAMPS

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / 'apercus/validation'


def font(size, bold=False):
    for f in (('C:/Windows/Fonts/segoeuib.ttf' if bold else 'C:/Windows/Fonts/segoeui.ttf'), 'C:/Windows/Fonts/arial.ttf'):
        try:
            return ImageFont.truetype(f, size)
        except OSError:
            continue
    return ImageFont.load_default()


GROUPES = [
    ('Identité Aeloria', [('or', 'Or ouvragé'), ('cristal', 'Cristal améthyste'), ('tuile_violette', 'Ardoise violette')]),
    ('Harnachement commun', [('cuir', 'Cuir'), ('fer', 'Fer'), ('ivoire', 'Ivoire et corne'), ('sabot', 'Sabot'), ('corde', 'Corde')]),
    ('Rareté (couleurs de Rarity.java) : tapis et bannières', [('rar_commun', 'Commun'), ('rar_peu_commun', 'Peu commun'), ('rar_rare', 'Rare'), ('rar_epique', 'Épique'), ('rar_legendaire', 'Légendaire'), ('rar_mythique', 'Mythique')]),
    ('Blocs', [('pierre', 'Pierre'), ('pierre_chaude', 'Pierre chaude'), ('mousse', 'Mousse'), ('bois', 'Bois sombre'), ('bois_clair', 'Bois clair'), ('eau', 'Eau')]),
    ('Bêtes (une rampe propre par monture, exemple du sanglier)', [('fourrure_brune', 'Fourrure'), ('soie_noire', 'Soies'), ('museau_rose', 'Museau')]),
]
SW = 34
W = 1500
rows = sum(1 + len(g[1]) for g in GROUPES)
H = 70 + sum(38 + len(g[1]) * (SW + 6) for g in GROUPES) + 20
col2 = 520
tex_h = max(H, 2 * 256 * 2 + 150)
S = Image.new('RGB', (W, tex_h), (30, 28, 36))
d = ImageDraw.Draw(S)
d.text((20, 14), 'Direction artistique Aeloria : palette et textures', fill=(255, 255, 255), font=font(28, True))
y = 70
for title, items in GROUPES:
    d.text((20, y), title, fill=(230, 190, 90), font=font(17, True))
    y += 30
    for key, lab in items:
        d.text((20, y + 6), lab, fill=(220, 220, 228), font=font(15))
        r = RAMPS[key]
        for i, c in enumerate(r):
            d.rectangle((170 + i * SW, y, 170 + (i + 1) * SW - 2, y + SW - 2), fill=tuple(int(v) for v in c))
        y += SW + 6
    y += 8
x0 = col2 + 180
for i, (key, kind, lab) in enumerate((('sanglier', 'mounts', 'sanglier.png : 256×256, 2 texels par unité, 16 par bloc une fois rendu ×2'), ('deco_puits', 'props', 'deco_puits.png : 256×256, 1 texel par unité, 16 par bloc'))):
    t = Image.open(ROOT / f'livraison/src/main/resources/assets/mmorpg/textures/imported/{kind}/{key}.png').convert('RGBA')
    bg = Image.new('RGB', t.size, (62, 60, 70))
    bg.paste(t, mask=t.split()[3])
    bg = bg.resize((512, 512), Image.NEAREST)
    yy = 60 + i * (512 + 70)
    d.text((x0, yy), lab, fill=(220, 220, 228), font=font(15))
    S.paste(bg, (x0, yy + 28))
S.save(OUT / 'palette_et_textures.png', optimize=True)
print(S.size)
