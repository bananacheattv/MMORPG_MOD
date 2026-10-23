# -*- coding: utf-8 -*-
"""Logos d'Aeloria : detoure les deux images sources (art/logo/source) et ecrit toutes les tailles utilisees.

  emblem.webp : l'embleme seul (cristal violet, rune doree, anneau)
  logo.webp   : l'embleme suivi du nom AELORIA

Le fond (quasi noir) devient transparent : la transparence est deduite de l'ecart au fond, ce qui garde les halos lumineux.
Sorties :
  art/logo/*.png                                     versions en grand
  src/main/resources/assets/mmorpg/textures/gui/logo emblemes (64, 128, 256) et logo (512) pour l'interface du jeu
  src/main/resources/aeloria_logo.png, aeloria_banner.png  icone et banniere du mod (ecran des mods)
  launcher/core/resources/eldoria/launcher/*.png     images du launcher
  launcher/assets/aeloria.ico                        icone Windows du launcher
Usage : python tools/build_logo.py
"""
import os

from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..')
SRC = os.path.join(ROOT, 'art', 'logo', 'source')


def cutout(path, strength=90.0):
    """Image RGBA : alpha = ecart au fond, couleurs corrigees pour rester justes sur un fond sombre."""
    im = Image.open(path).convert('RGB')
    bg = im.getpixel((4, 4))
    w, h = im.size
    px = im.load()
    out = Image.new('RGBA', im.size)
    po = out.load()
    for y in range(h):
        for x in range(w):
            r, g, b = px[x, y]
            diff = max(r - bg[0], g - bg[1], b - bg[2], 0)
            a = min(1.0, diff / strength)
            if a < 0.012:
                po[x, y] = (0, 0, 0, 0)
                continue
            k = 1 - a
            po[x, y] = (min(255, max(0, round((r - bg[0] * k) / a))), min(255, max(0, round((g - bg[1] * k) / a))),
                        min(255, max(0, round((b - bg[2] * k) / a))), round(a * 255))
    return out


def trim(im, margin=0.04):
    box = im.getchannel('A').point(lambda v: 255 if v > 14 else 0).getbbox()
    im = im.crop(box)
    pad = int(max(im.size) * margin)
    out = Image.new('RGBA', (im.width + 2 * pad, im.height + 2 * pad), (0, 0, 0, 0))
    out.paste(im, (pad, pad))
    return out


def square(im):
    s = max(im.size)
    out = Image.new('RGBA', (s, s), (0, 0, 0, 0))
    out.paste(im, ((s - im.width) // 2, (s - im.height) // 2))
    return out


def fit(im, width=None, height=None):
    if width:
        height = round(im.height * width / im.width)
    else:
        width = round(im.width * height / im.height)
    return im.resize((width, height), Image.LANCZOS)


def save(im, *parts):
    p = os.path.join(ROOT, *parts)
    os.makedirs(os.path.dirname(p), exist_ok=True)
    im.save(p, optimize=True)
    return p


def main():
    emblem = square(trim(cutout(os.path.join(SRC, 'emblem.webp'))))
    logo = trim(cutout(os.path.join(SRC, 'logo.webp')), 0.03)
    save(fit(emblem, 1024), 'art', 'logo', 'aeloria_emblem.png')
    save(fit(logo, 2048), 'art', 'logo', 'aeloria_logo.png')
    gui = ('src', 'main', 'resources', 'assets', 'mmorpg', 'textures', 'gui', 'logo')
    for s in (64, 128, 256):
        save(fit(emblem, s), *gui, f'emblem_{s}.png')
    save(fit(logo, 512), *gui, 'logo_512.png')
    save(fit(logo, 256), *gui, 'logo_256.png')
    save(fit(emblem, 256), 'src', 'main', 'resources', 'aeloria_logo.png')        # iconFile du mod
    save(fit(logo, 512), 'src', 'main', 'resources', 'aeloria_banner.png')        # bannerFile du mod
    lau = ('launcher', 'core', 'resources', 'eldoria', 'launcher')
    save(fit(emblem, 512), *lau, 'emblem.png')
    save(fit(logo, 900), *lau, 'logo.png')
    ico = fit(emblem, 256)
    p = os.path.join(ROOT, 'launcher', 'assets', 'aeloria.ico')
    os.makedirs(os.path.dirname(p), exist_ok=True)
    ico.save(p, sizes=[(16, 16), (24, 24), (32, 32), (48, 48), (64, 64), (128, 128), (256, 256)])
    save(fit(emblem, 64), 'art', 'logo', 'server-icon.png')
    print('logos ecrits :', emblem.size, logo.size)


if __name__ == '__main__':
    main()
