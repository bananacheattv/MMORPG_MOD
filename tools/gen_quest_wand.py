"""Genere l'icone, le modele et la definition de l'item "Sceptre des quetes" (outil d'administration)."""
import json
import os
from PIL import Image, ImageDraw, ImageFilter

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..', 'src', 'main', 'resources', 'assets', 'mmorpg')
S = 512            # dessin en grand puis reduction pour un rendu lisse (64 x 64)


def sceptre():
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    u = S / 64.0

    def p(x, y):
        return (x * u, y * u)

    # hampe en bois sombre, du bas gauche vers le haut droit
    d.line([p(10, 56), p(42, 24)], fill=(70, 44, 26, 255), width=int(7 * u))
    d.line([p(10, 56), p(42, 24)], fill=(120, 80, 44, 255), width=int(4.5 * u))
    d.line([p(9.5, 55), p(41, 23)], fill=(170, 124, 70, 255), width=int(1.6 * u))
    # bagues dorees
    for t in (0.18, 0.5):
        x, y = 10 + 32 * t, 56 - 32 * t
        d.ellipse([p(x - 4.2, y - 4.2), p(x + 4.2, y + 4.2)], fill=(122, 86, 20, 255))
        d.ellipse([p(x - 3.2, y - 3.2), p(x + 3.2, y + 3.2)], fill=(236, 190, 60, 255))
    # griffes d'or autour de la gemme
    d.polygon([p(36, 30), p(34, 16), p(44, 24)], fill=(236, 190, 60, 255), outline=(122, 86, 20, 255))
    d.polygon([p(36, 30), p(50, 30), p(44, 18)], fill=(236, 190, 60, 255), outline=(122, 86, 20, 255))
    d.polygon([p(50, 30), p(50, 44), p(42, 36)], fill=(214, 164, 40, 255), outline=(122, 86, 20, 255))
    # gemme violette facettee
    gem = [p(43, 8), p(55, 20), p(43, 32), p(31, 20)]
    d.polygon(gem, fill=(138, 58, 214, 255), outline=(70, 24, 120, 255))
    d.polygon([p(43, 8), p(55, 20), p(43, 20)], fill=(196, 128, 255, 255))
    d.polygon([p(43, 20), p(55, 20), p(43, 32)], fill=(96, 36, 170, 255))
    d.polygon([p(31, 20), p(43, 20), p(43, 8)], fill=(168, 96, 240, 255))
    d.ellipse([p(38, 12), p(42, 16)], fill=(246, 230, 255, 255))
    # lueur
    glow = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse([p(28, 5), p(58, 35)], fill=(170, 100, 255, 90))
    glow = glow.filter(ImageFilter.GaussianBlur(7 * u))
    out = Image.alpha_composite(glow, im)
    # petit parchemin avec point d'exclamation en bas a droite : le symbole des quetes
    d = ImageDraw.Draw(out)
    d.rounded_rectangle([p(40, 40), p(58, 60)], radius=int(2.5 * u), fill=(236, 220, 176, 255), outline=(120, 84, 40, 255), width=int(1.4 * u))
    d.rectangle([p(48, 44), p(50, 53)], fill=(190, 40, 40, 255))
    d.ellipse([p(47.6, 54.6), p(50.4, 57.4)], fill=(190, 40, 40, 255))
    return out.resize((64, 64), Image.LANCZOS)


def main():
    sceptre().save(os.path.join(ROOT, 'textures', 'item', 'sceptre_quetes.png'))
    with open(os.path.join(ROOT, 'models', 'item', 'sceptre_quetes.json'), 'w', encoding='utf-8', newline='\n') as f:
        json.dump({"parent": "minecraft:item/handheld", "textures": {"layer0": "mmorpg:item/sceptre_quetes"}}, f, indent=2)
    with open(os.path.join(ROOT, 'items', 'sceptre_quetes.json'), 'w', encoding='utf-8', newline='\n') as f:
        json.dump({"model": {"type": "minecraft:model", "model": "mmorpg:item/sceptre_quetes"}}, f, indent=2)
    print('sceptre_quetes : icone, modele, definition ecrits')


if __name__ == '__main__':
    main()
