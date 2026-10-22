"""Genere les icones, modeles et definitions des outils d'administration : sceptre des quetes et baguette de zone."""
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


def baguette():
    """Baguette de zone : tige claire, gemme turquoise et un cadre pointille (le rectangle d'une zone)."""
    im = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    d = ImageDraw.Draw(im)
    u = S / 64.0

    def p(x, y):
        return (x * u, y * u)

    d.line([p(10, 56), p(40, 26)], fill=(56, 70, 84, 255), width=int(6.5 * u))
    d.line([p(10, 56), p(40, 26)], fill=(150, 172, 190, 255), width=int(4 * u))
    d.line([p(9.5, 55), p(39, 25)], fill=(214, 232, 244, 255), width=int(1.4 * u))
    for t in (0.2, 0.55):
        x, y = 10 + 30 * t, 56 - 30 * t
        d.ellipse([p(x - 3.8, y - 3.8), p(x + 3.8, y + 3.8)], fill=(40, 96, 110, 255))
        d.ellipse([p(x - 2.8, y - 2.8), p(x + 2.8, y + 2.8)], fill=(96, 220, 214, 255))
    gem = [p(42, 8), p(54, 18), p(42, 30), p(30, 18)]
    d.polygon(gem, fill=(40, 190, 200, 255), outline=(14, 90, 110, 255))
    d.polygon([p(42, 8), p(54, 18), p(42, 18)], fill=(170, 250, 250, 255))
    d.polygon([p(42, 18), p(54, 18), p(42, 30)], fill=(24, 130, 150, 255))
    d.polygon([p(30, 18), p(42, 18), p(42, 8)], fill=(110, 230, 232, 255))
    d.ellipse([p(37, 11), p(41, 15)], fill=(240, 255, 255, 255))
    glow = Image.new('RGBA', (S, S), (0, 0, 0, 0))
    ImageDraw.Draw(glow).ellipse([p(27, 4), p(57, 34)], fill=(80, 230, 230, 90))
    glow = glow.filter(ImageFilter.GaussianBlur(7 * u))
    out = Image.alpha_composite(glow, im)
    d = ImageDraw.Draw(out)
    # cadre pointille avec ses quatre coins
    x0, y0, x1, y1 = 40, 40, 60, 60
    for t in range(0, 20, 4):
        d.rectangle([p(x0 + t, y0 - 0.7), p(x0 + t + 2, y0 + 0.7)], fill=(250, 214, 90, 255))
        d.rectangle([p(x0 + t, y1 - 0.7), p(x0 + t + 2, y1 + 0.7)], fill=(250, 214, 90, 255))
        d.rectangle([p(x0 - 0.7, y0 + t), p(x0 + 0.7, y0 + t + 2)], fill=(250, 214, 90, 255))
        d.rectangle([p(x1 - 0.7, y0 + t), p(x1 + 0.7, y0 + t + 2)], fill=(250, 214, 90, 255))
    for cx, cy in ((x0, y0), (x1, y0), (x0, y1), (x1, y1)):
        d.ellipse([p(cx - 2.2, cy - 2.2), p(cx + 2.2, cy + 2.2)], fill=(224, 70, 70, 255), outline=(110, 24, 24, 255))
    return out.resize((64, 64), Image.LANCZOS)


def generate(ga=None):
    if ga is not None:
        ga.LANG[f'item.{ga.MODID}.sceptre_quetes'] = 'Sceptre des quêtes'
        ga.LANG_EN[f'item.{ga.MODID}.sceptre_quetes'] = 'Quest Scepter'
        ga.LANG[f'item.{ga.MODID}.baguette_zone'] = 'Baguette de zone'
        ga.LANG_EN[f'item.{ga.MODID}.baguette_zone'] = 'Zone Wand'
    main()


def main():
    sceptre().save(os.path.join(ROOT, 'textures', 'item', 'sceptre_quetes.png'))
    with open(os.path.join(ROOT, 'models', 'item', 'sceptre_quetes.json'), 'w', encoding='utf-8', newline='\n') as f:
        json.dump({"parent": "minecraft:item/handheld", "textures": {"layer0": "mmorpg:item/sceptre_quetes"}}, f, indent=2)
    with open(os.path.join(ROOT, 'items', 'sceptre_quetes.json'), 'w', encoding='utf-8', newline='\n') as f:
        json.dump({"model": {"type": "minecraft:model", "model": "mmorpg:item/sceptre_quetes"}}, f, indent=2)
    baguette().save(os.path.join(ROOT, 'textures', 'item', 'baguette_zone.png'))
    with open(os.path.join(ROOT, 'models', 'item', 'baguette_zone.json'), 'w', encoding='utf-8', newline='\n') as f:
        json.dump({"parent": "minecraft:item/handheld", "textures": {"layer0": "mmorpg:item/baguette_zone"}}, f, indent=2)
    with open(os.path.join(ROOT, 'items', 'baguette_zone.json'), 'w', encoding='utf-8', newline='\n') as f:
        json.dump({"model": {"type": "minecraft:model", "model": "mmorpg:item/baguette_zone"}}, f, indent=2)
    print('sceptre_quetes et baguette_zone : icones, modeles, definitions ecrits')


if __name__ == '__main__':
    main()
