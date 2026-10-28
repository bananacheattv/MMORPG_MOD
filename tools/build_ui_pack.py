# -*- coding: utf-8 -*-
"""Pack d'interface « Aeloria » : habille tous les ecrans de Minecraft (menu Echap, options, inventaires, coffres, tooltips, barre
d'action, ecran-titre...) aux couleurs du mod : violet nuit, style plat.

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
from PIL import Image, ImageFilter, ImageDraw

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.normpath(os.path.join(HERE, '..', 'src', 'main', 'resources', 'resourcepacks', 'aeloria_ui'))
GUI = 'assets/minecraft/textures/gui/'
DEFAULT_JAR = os.path.expanduser(r'~\.gradle\caches\neoformruntime\artifacts\minecraft_26.3_client.jar')

GOLD = (226, 184, 90)
GOLD_LIGHT = (255, 232, 160)
GOLD_DARK = (122, 90, 34)
NIGHT = (10, 7, 18)
RAMP = [(0.0, (4, 3, 7)), (0.20, (9, 7, 13)), (0.40, (14, 11, 20)), (0.55, (17, 14, 24)), (0.776, (27, 23, 37)),
        (0.90, (62, 54, 82)), (1.0, (122, 110, 152))]
BORDER = (122, 98, 154)
BORDER_HI = (226, 184, 90)

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


def flat(w, h, top, bottom, border, accent_bar=None, alpha=255):
    """Rectangle plat : bordure d'un pixel, fond a leger degrade (aucun biseau)."""
    im = Image.new('RGBA', (w, h), (0, 0, 0, 0))
    px = im.load()
    for y in range(h):
        c = lerp(top, bottom, y / max(1, h - 1))
        for x in range(w):
            edge = min(x, y, w - 1 - x, h - 1 - y)
            px[x, y] = (*(border if edge == 0 else c), alpha)
    if accent_bar:
        for y in range(1, h - 1):
            px[1, y] = (*accent_bar, alpha)
    radius=min(5,w//2,h//2)
    mask=Image.new('L',(w,h),0)
    ImageDraw.Draw(mask).rounded_rectangle((0,0,w-1,h-1),radius=radius,fill=min(alpha,215))
    im.putalpha(mask)
    ImageDraw.Draw(im).rounded_rectangle((0,0,w-1,h-1),radius=radius,outline=(*border,230),width=1)
    return im


def make_widgets(out):
    d = os.path.join(out, GUI, 'sprites/widget')
    os.makedirs(d, exist_ok=True)
    flat(200, 20, (47, 32, 67), (32, 24, 48), BORDER).save(d + '/button.png')
    flat(200, 20, (68, 44, 92), (44, 29, 65), BORDER_HI).save(d + '/button_highlighted.png')
    flat(200, 20, (12, 10, 17), (10, 8, 14), (28, 24, 38)).save(d + '/button_disabled.png')
    flat(200, 20, (11, 9, 16), (9, 7, 13), BORDER).save(d + '/slider.png')
    flat(200, 20, (14, 11, 20), (11, 9, 16), BORDER_HI).save(d + '/slider_highlighted.png')
    flat(8, 20, (226, 184, 90), (190, 150, 66), (226, 184, 90)).save(d + '/slider_handle.png')
    flat(8, 20, (255, 224, 140), (226, 184, 90), (255, 224, 140)).save(d + '/slider_handle_highlighted.png')
    flat(200, 20, (7, 5, 11), (7, 5, 11), BORDER).save(d + '/text_field.png')
    flat(200, 20, (7, 5, 11), (7, 5, 11), BORDER_HI).save(d + '/text_field_highlighted.png')
    flat(6, 32, (226, 184, 90), (190, 150, 66), (226, 184, 90)).save(d + '/scroller.png')
    flat(6, 32, (9, 7, 13), (9, 7, 13), (22, 18, 30)).save(d + '/scroller_background.png')
    for filename in os.listdir(d):
        if not filename.endswith('.png.mcmeta'):continue
        file=os.path.join(d,filename)
        with open(file,encoding='utf-8') as handle:meta=json.load(handle)
        scaling=meta.get('gui',{}).get('scaling',{})
        if scaling.get('width')==200 and scaling.get('height')==20:
            scaling['border']=5
            with open(file,'w',encoding='utf-8',newline='\n') as handle:json.dump(meta,handle,indent=4)



def make_tooltip(out):
    d = os.path.join(out, GUI, 'sprites/tooltip')
    os.makedirs(d, exist_ok=True)
    Image.new('RGBA', (100, 100), (10, 8, 15, 246)).save(d + '/background.png')
    fr = Image.new('RGBA', (100, 100), (0, 0, 0, 0))
    px = fr.load()
    for y in range(100):
        for x in range(100):
            if min(x, y, 99 - x, 99 - y) == 7:
                px[x, y] = (62, 54, 84, 255)
    fr.save(d + '/frame.png')


def make_backgrounds(out):
    d = os.path.join(out, GUI)
    os.makedirs(d, exist_ok=True)
    tiles = {'menu_background.png': (8, 6, 12, 248), 'menu_list_background.png': (6, 4, 10, 240),
             'inworld_menu_background.png': (8, 6, 12, 215), 'inworld_menu_list_background.png': (6, 4, 10, 205)}
    for name, c in tiles.items():
        Image.new('RGBA', (16, 16), c).save(os.path.join(d, name))
    for name, top in (('header_separator.png', True), ('inworld_header_separator.png', True), ('footer_separator.png', False),
                      ('inworld_footer_separator.png', False)):
        a = np.zeros((2, 32, 4), np.uint8)
        a[0 if top else 1, :, :] = (*BORDER, 255)
        a[1 if top else 0, :, :] = (0, 0, 0, 0)
        Image.fromarray(a, 'RGBA').save(os.path.join(d, name))


def font_sheet():
    """Glyphes 8 x 12 de la police Aeloria (grille de 16 colonnes) pour composer le logo."""
    sheet = Image.open(os.path.normpath(os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'mmorpg', 'textures', 'font', 'aeloria.png'))).convert('RGBA')
    j = json.load(open(os.path.normpath(os.path.join(HERE, '..', 'src', 'main', 'resources', 'assets', 'mmorpg', 'font', 'aeloria_bitmap.json')), encoding='utf-8'))
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
    json.dump({'pack': {'description': 'Aeloria : interface MMORPG (plate, violet nuit)', 'pack_format': 97, 'min_format': 97, 'max_format': 97}},
              open(os.path.join(OUT, 'pack.mcmeta'), 'w', encoding='utf-8', newline='\n'), ensure_ascii=False, indent=2)
    print(count, 'textures recolorees ; pack ecrit dans', OUT)


if __name__ == '__main__':
    main()
