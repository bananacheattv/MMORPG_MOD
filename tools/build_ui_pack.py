# -*- coding: utf-8 -*-
"""Pack d'interface « Aeloria » : habille tous les ecrans de Minecraft (menu Echap, options, inventaires, coffres, tooltips, barre
d'action, ecran-titre...) aux couleurs du mod : violet nuit, filets d'or.

Boutons, curseurs, champs de texte, tooltips, fonds de menus et logo sont dessines ici. Les autres textures de l'interface (conteneurs,
onglets, livre de recettes, toasts, succes, etc.) sont les textures de Minecraft recolorees : les pixels gris sont remappes sur une rampe
violet -> or (le blanc des biseaux devient or), les pixels colores (fleches, flammes, icones) restent tels quels. Le pack est ecrit dans
src/main/resources/resourcepacks/aeloria_ui et active en permanence par le mod (AddPackFindersEvent).

usage : python tools/build_ui_pack.py [client.jar de Minecraft 26.3]
"""
import io
import json
import os
import shutil
import sys
import zipfile

import numpy as np
from PIL import Image, ImageFilter

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.normpath(os.path.join(HERE, '..', 'src', 'main', 'resources', 'resourcepacks', 'aeloria_ui'))
GUI = 'assets/minecraft/textures/gui/'
DEFAULT_JAR = os.path.expanduser(r'~\.gradle\caches\neoformruntime\artifacts\minecraft_26.3_client.jar')

GOLD = (226, 184, 90)
GOLD_LIGHT = (255, 232, 160)
GOLD_DARK = (122, 90, 34)
NIGHT = (10, 7, 18)
RAMP = [(0.0, (4, 3, 8)), (0.20, (12, 8, 20)), (0.40, (20, 14, 30)), (0.55, (26, 19, 40)), (0.776, (46, 35, 64)),
        (0.90, (122, 94, 62)), (1.0, (232, 190, 96))]

# textures du jeu recolorees (chemins relatifs a assets/minecraft/textures/gui/, motifs de prefixe)
RECOLOR = ['container/', 'recipe_book.png', 'tab_header_background.png', 'sprites/container/', 'sprites/recipe_book/', 'sprites/toast/',
           'sprites/advancements/', 'sprites/social_interactions/', 'sprites/statistics/', 'sprites/transferable_list/',
           'sprites/world_list/', 'sprites/server_list/', 'sprites/dialog/', 'sprites/spectator/', 'sprites/player_list/', 'sprites/widget/',
           'sprites/hud/hotbar']
# dessinees a la main (ne pas recolorer)
HAND_DRAWN = {'sprites/widget/button.png', 'sprites/widget/button_highlighted.png', 'sprites/widget/button_disabled.png',
              'sprites/widget/slider.png', 'sprites/widget/slider_highlighted.png', 'sprites/widget/slider_handle.png',
              'sprites/widget/slider_handle_highlighted.png', 'sprites/widget/text_field.png', 'sprites/widget/text_field_highlighted.png',
              'sprites/widget/scroller.png', 'sprites/widget/scroller_background.png'}


def ramp(lum):
    xs = [p[0] for p in RAMP]
    return np.stack([np.interp(lum, xs, [p[1][c] for p in RAMP]) for c in range(3)], axis=-1)


def recolor(img):
    arr = np.array(img.convert('RGBA'), dtype=np.float64)
    rgb = arr[..., :3]
    mx, mn = rgb.max(-1), rgb.min(-1)
    sat = (mx - mn) / (mx + 1e-6)
    lum = (0.299 * rgb[..., 0] + 0.587 * rgb[..., 1] + 0.114 * rgb[..., 2]) / 255.0
    new = ramp(lum)
    mask = ((sat < 0.16) | (mx < 80))[..., None]
    out = arr.copy()
    out[..., :3] = np.where(mask, new, rgb)
    return Image.fromarray(np.clip(out, 0, 255).astype(np.uint8), 'RGBA')


def lerp(a, b, t):
    return tuple(int(round(a[i] + (b[i] - a[i]) * t)) for i in range(3))


def box(w, h, border, body_top, body_bottom, rim, rim_light, glow=None, alpha=255):
    """Cadre a filet d'or : liseré exterieur sombre, filet d'or, reflet interieur, corps degrade."""
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    px = im.load()
    for y in range(h):
        t = y / max(1, h - 1)
        body = lerp(body_top, body_bottom, t)
        for x in range(w):
            edge = min(x, y, w - 1 - x, h - 1 - y)
            if edge == 0:
                c = (6, 4, 12)
            elif edge == 1:
                c = lerp(rim_light, rim, t)
            elif edge == 2 and border >= 3:
                c = lerp(tuple(min(255, v + 18) for v in body), body, 0.4)
            else:
                c = body
            px[x, y] = (*c, alpha)
    if glow:
        # coins coupes : petit ornement dore
        for (x, y) in ((1, 1), (w - 2, 1), (1, h - 2), (w - 2, h - 2)):
            px[x, y] = (*glow, alpha)
    return im


def make_widgets(out):
    d = os.path.join(out, GUI, 'sprites/widget')
    os.makedirs(d, exist_ok=True)
    box(200, 20, 3, (66, 50, 92), (34, 26, 50), GOLD_DARK, GOLD, GOLD_LIGHT).save(d + '/button.png')
    box(200, 20, 3, (104, 80, 138), (56, 42, 82), GOLD, GOLD_LIGHT, (255, 250, 210)).save(d + '/button_highlighted.png')
    box(200, 20, 3, (30, 24, 40), (22, 17, 30), (64, 54, 48), (90, 78, 66)).save(d + '/button_disabled.png')
    box(200, 20, 3, (20, 14, 30), (14, 10, 22), GOLD_DARK, (170, 130, 60)).save(d + '/slider.png')
    box(200, 20, 3, (28, 20, 42), (18, 13, 28), GOLD, GOLD_LIGHT).save(d + '/slider_highlighted.png')
    box(8, 20, 2, (240, 204, 120), (176, 132, 52), (70, 46, 14), (255, 240, 180)).save(d + '/slider_handle.png')
    box(8, 20, 2, (255, 236, 160), (210, 160, 70), (110, 74, 24), (255, 252, 220)).save(d + '/slider_handle_highlighted.png')
    box(200, 20, 1, (8, 5, 14), (8, 5, 14), GOLD_DARK, GOLD_DARK).save(d + '/text_field.png')
    box(200, 20, 1, (8, 5, 14), (8, 5, 14), GOLD, GOLD_LIGHT).save(d + '/text_field_highlighted.png')
    box(6, 32, 1, (210, 170, 80), (150, 110, 44), (60, 40, 12), (250, 220, 140)).save(d + '/scroller.png')
    box(6, 32, 1, (10, 7, 16), (10, 7, 16), (40, 30, 20), (40, 30, 20)).save(d + '/scroller_background.png')


def make_tooltip(out):
    d = os.path.join(out, GUI, 'sprites/tooltip')
    os.makedirs(d, exist_ok=True)
    bg = Image.new('RGBA', (100, 100), (14, 10, 24, 244))
    bg.save(d + '/background.png')
    fr = Image.new('RGBA', (100, 100), (0, 0, 0, 0))
    px = fr.load()
    for y in range(100):
        t = y / 99
        c = lerp(GOLD_LIGHT, GOLD_DARK, t)
        for x in range(100):
            edge = min(x, y, 99 - x, 99 - y)
            # le cadre occupe les 9 pixels du bord, mais le trait ne fait que deux pixels, a 6 pixels du bord exterieur
            if edge in (6, 7):
                px[x, y] = (*(c if edge == 6 else lerp(c, NIGHT, 0.5)), 255)
            elif edge in (4, 5):
                px[x, y] = (*lerp(NIGHT, GOLD_DARK, 0.35), 90)
    fr.save(d + '/frame.png')


def make_backgrounds(out):
    d = os.path.join(out, GUI)
    os.makedirs(d, exist_ok=True)
    tiles = {'menu_background.png': (12, 8, 22, 245), 'menu_list_background.png': (8, 5, 15, 235),
             'inworld_menu_background.png': (12, 8, 22, 200), 'inworld_menu_list_background.png': (8, 5, 15, 190)}
    rng = np.random.default_rng(5)
    for name, c in tiles.items():
        a = np.zeros((16, 16, 4), np.uint8)
        a[..., :] = c
        noise = rng.integers(-3, 4, (16, 16))
        a[..., :3] = np.clip(a[..., :3].astype(int) + noise[..., None], 0, 255)
        # rhombes tres discrets
        for i in range(16):
            for j in range(16):
                if (i + j) % 8 == 0 or (i - j) % 8 == 0:
                    a[i, j, :3] = np.clip(a[i, j, :3].astype(int) + 4, 0, 255)
        Image.fromarray(a, 'RGBA').save(os.path.join(d, name))
    for name, top in (('header_separator.png', True), ('inworld_header_separator.png', True), ('footer_separator.png', False),
                      ('inworld_footer_separator.png', False)):
        a = np.zeros((2, 32, 4), np.uint8)
        a[0 if top else 1, :, :] = (*GOLD_DARK, 255)
        a[1 if top else 0, :, :] = (*NIGHT, 255)
        Image.fromarray(a, 'RGBA').save(os.path.join(d, name))


def font_sheet():
    """Glyphes 8 x 12 de la police Aeloria (grille de 16 colonnes) pour composer le logo."""
    sheet = Image.open(os.path.normpath(os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'mmorpg', 'textures', 'font', 'aeloria.png'))).convert('RGBA')
    j = json.load(open(os.path.normpath(os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'mmorpg', 'font', 'aeloria.json')), encoding='utf-8'))
    grid = next(p['chars'] for p in j['providers'] if p['type'] == 'bitmap')
    return sheet, grid


def glyph(sheet, grid, ch):
    for r, row in enumerate(grid):
        c = row.find(ch)
        if c >= 0:
            return sheet.crop((c * 8, r * 12, c * 8 + 8, r * 12 + 12))
    raise KeyError(ch)


def text_mask(text, scale, gap):
    sheet, grid = font_sheet()
    glyphs = [glyph(sheet, grid, ch) for ch in text]
    cols = []
    for g in glyphs:
        a = np.array(g)[..., 3] > 0
        xs = np.where(a.any(0))[0]
        cols.append((g, xs.min(), xs.max()))
    width = sum((mx - mn + 1) * scale for _, mn, mx in cols) + gap * (len(cols) - 1)
    mask = Image.new('L', (width, 12 * scale), 0)
    x = 0
    for g, mn, mx in cols:
        m = Image.fromarray(((np.array(g)[..., 3] > 0) * 255).astype(np.uint8)).crop((mn, 0, mx + 1, 12))
        mask.paste(m.resize((m.width * scale, 12 * scale), Image.NEAREST), (x, 0))
        x += (mx - mn + 1) * scale + gap
    return mask


def gold_text(mask, size, glow=True, top=None):
    """Lettres d'or a degrade, contour sombre, ombre portee et halo violet, centrees dans une image RGBA {size}."""
    W, H = size
    canvas = Image.new('RGBA', size, (0, 0, 0, 0))
    ox, oy = (W - mask.width) // 2, (H - mask.height) // 2 if top is None else top
    layer = Image.new('L', size, 0)
    layer.paste(mask, (ox, oy))
    if glow:
        halo = layer.filter(ImageFilter.GaussianBlur(14)).point(lambda v: min(255, v * 2))
        canvas.alpha_composite(Image.merge('RGBA', (Image.new('L', size, 150), Image.new('L', size, 90), Image.new('L', size, 255), halo.point(lambda v: v // 2))))
    outline = layer.filter(ImageFilter.MaxFilter(9))
    shadow = Image.new('L', size, 0)
    shadow.paste(outline, (6, 8))
    canvas.alpha_composite(Image.merge('RGBA', (Image.new('L', size, 0), Image.new('L', size, 0), Image.new('L', size, 0), shadow.point(lambda v: v * 3 // 4))))
    canvas.alpha_composite(Image.merge('RGBA', (Image.new('L', size, 24), Image.new('L', size, 14), Image.new('L', size, 8), outline)))
    arr = np.zeros((H, W, 4), np.uint8)
    m = np.array(layer) > 0
    ys = np.arange(H)[:, None]
    top = oy
    t = np.clip((ys - top) / max(1, mask.height), 0, 1)
    for ch, (a, b) in enumerate(zip((255, 236, 160), (196, 132, 36))):
        arr[..., ch] = np.where(m, (a + (b - a) * t).astype(np.uint8), 0)
    arr[..., 3] = np.where(m, 255, 0)
    # reflet clair en haut de chaque lettre
    hi = m & (np.array(layer.filter(ImageFilter.MinFilter(1))) > 0)
    canvas.alpha_composite(Image.fromarray(arr, 'RGBA'))
    return canvas


def make_logo(out):
    d = os.path.join(out, GUI, 'title')
    os.makedirs(d, exist_ok=True)
    logo = gold_text(text_mask('AELORIA', 10, 10), (1024, 256), top=14)
    logo.save(d + '/minecraft.png')
    logo.save(d + '/minceraft.png')
    sub = gold_text(text_mask('MMORPG', 3, 5), (512, 64), glow=False, top=8)
    sub.save(d + '/edition.png')


def main():
    jar = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_JAR
    z = zipfile.ZipFile(jar)
    if os.path.isdir(OUT):
        shutil.rmtree(OUT)
    os.makedirs(OUT)
    count = 0
    for name in z.namelist():
        if not name.startswith(GUI) or not (name.endswith('.png') or name.endswith('.png.mcmeta')):
            continue
        rel = name[len(GUI):]
        base = rel[:-7] if rel.endswith('.mcmeta') else rel
        if not any(base.startswith(p) for p in RECOLOR) and base not in ('menu_background.png',):
            continue
        dest = os.path.join(OUT, name)
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        if rel.endswith('.mcmeta'):
            open(dest, 'wb').write(z.read(name))               # decoupage en neuf parties et animations conserves
            continue
        if base in HAND_DRAWN or base == 'menu_background.png':
            continue
        recolor(Image.open(io.BytesIO(z.read(name)))).save(dest)
        count += 1
    for name in ('sprites/tooltip/background.png.mcmeta', 'sprites/tooltip/frame.png.mcmeta'):
        dest = os.path.join(OUT, GUI, name)
        os.makedirs(os.path.dirname(dest), exist_ok=True)
        open(dest, 'wb').write(z.read(GUI + name))
    make_widgets(OUT)
    make_tooltip(OUT)
    make_backgrounds(OUT)
    make_logo(OUT)
    # textes du menu Echap (les cles de Minecraft sont remplacees, dans les deux langues)
    for lang, tr in (('fr_fr', {'menu.game': "Menu d'Aeloria", 'menu.returnToGame': "Reprendre l'aventure", 'menu.returnToMenu': 'Sauvegarder et quitter',
                                'menu.disconnect': "Quitter Aeloria", 'menu.paused': 'Jeu en pause'}),
                     ('en_us', {'menu.game': 'Aeloria menu', 'menu.returnToGame': 'Back to the adventure', 'menu.returnToMenu': 'Save and quit',
                                'menu.disconnect': 'Leave Aeloria', 'menu.paused': 'Game paused'})):
        d = os.path.join(OUT, 'assets', 'minecraft', 'lang')
        os.makedirs(d, exist_ok=True)
        json.dump(tr, open(os.path.join(d, lang + '.json'), 'w', encoding='utf-8', newline='\n'), ensure_ascii=False, indent=2)
    json.dump({'pack': {'description': 'Aeloria : interface MMORPG (violet nuit et filets d\'or)', 'pack_format': 97, 'min_format': 97, 'max_format': 97}},
              open(os.path.join(OUT, 'pack.mcmeta'), 'w', encoding='utf-8', newline='\n'), ensure_ascii=False, indent=2)
    print(count, 'textures recolorees ; pack ecrit dans', OUT)


if __name__ == '__main__':
    main()
