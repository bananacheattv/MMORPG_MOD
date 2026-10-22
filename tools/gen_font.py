# -*- coding: utf-8 -*-
"""Police pixel « Eldoria » : capitales a empattements dessinees a la main (aucune police tierce), chiffres, ponctuation et
accents francais. Les minuscules reprennent les capitales (petites capitales), ce qui donne un style de titre coherent.

Genere assets/mmorpg/font/eldoria.json et assets/mmorpg/textures/font/eldoria.png (fournisseur « bitmap » de Minecraft).
Appele par tools/gen_hub.py ; lancer ce fichier seul pour obtenir un apercu (apercu_police.png).
"""
import json
import os

from PIL import Image

CELL_W = 8
CELL_H = 12
BASE_ROW = 3          # premiere ligne du corps de la lettre (les lignes 0..2 portent les accents)
BODY_ROWS = 8

G = {}


def glyph(ch, rows):
    assert len(rows) == BODY_ROWS, (ch, len(rows))
    assert all(len(r) <= 7 for r in rows), ch
    G[ch] = [r.ljust(7, '.') for r in rows]


glyph('A', ["...#...", "..#.#..", "..#.#..", ".#...#.", ".#####.", ".#...#.", ".#...#.", "##...##"])
glyph('B', ["######.", ".#...#.", ".#...#.", ".#####.", ".#...#.", ".#...##", ".#...#.", "######."])
glyph('C', ["..####.", ".#...##", "##.....", "#......", "#......", "##.....", ".#...##", "..####."])
glyph('D', ["#####..", ".#..##.", ".#...##", ".#...##", ".#...##", ".#...##", ".#..##.", "#####.."])
glyph('E', ["#######", ".#...##", ".#.....", ".####..", ".#.....", ".#....#", ".#...##", "#######"])
glyph('F', ["#######", ".#...##", ".#.....", ".####..", ".#.....", ".#.....", ".#.....", "###...."])
glyph('G', ["..####.", ".#...##", "##.....", "#..####", "#....#.", "##...#.", ".#...#.", "..####."])
glyph('H', ["###.###", ".#...#.", ".#...#.", ".#####.", ".#...#.", ".#...#.", ".#...#.", "###.###"])
glyph('I', [".#####.", "...#...", "...#...", "...#...", "...#...", "...#...", "...#...", ".#####."])
glyph('J', ["..#####", "....#..", "....#..", "....#..", "....#..", "#...#..", "#...#..", ".###..."])
glyph('K', ["###.###", ".#..#..", ".#.#...", ".##....", ".#.#...", ".#..#..", ".#...#.", "###..##"])
glyph('L', ["###....", ".#.....", ".#.....", ".#.....", ".#.....", ".#....#", ".#...##", "#######"])
glyph('M', ["#.....#", "##...##", "#.#.#.#", "#.#.#.#", "#..#..#", "#.....#", "#.....#", "#.....#"])
glyph('N', ["##...##", "###...#", "#.#...#", "#..#..#", "#..#..#", "#...#.#", "#...###", "##...##"])
glyph('O', ["..###..", ".#...#.", "#.....#", "#.....#", "#.....#", "#.....#", ".#...#.", "..###.."])
glyph('P', ["######.", ".#...#.", ".#...#.", ".#####.", ".#.....", ".#.....", ".#.....", "###...."])
glyph('Q', ["..###..", ".#...#.", "#.....#", "#.....#", "#...#.#", "#....##", ".#...#.", "..###.#"])
glyph('R', ["######.", ".#...#.", ".#...#.", ".#####.", ".#..#..", ".#...#.", ".#...##", "###..##"])
glyph('S', [".#####.", "#.....#", "##.....", ".####..", ".....##", ".....#.", "#....#.", ".####.."])
glyph('T', ["#######", "#..#..#", "...#...", "...#...", "...#...", "...#...", "...#...", "..###.."])
glyph('U', ["###.###", ".#...#.", ".#...#.", ".#...#.", ".#...#.", ".#...#.", ".##.##.", "..###.."])
glyph('V', ["##...##", "#.....#", ".#...#.", ".#...#.", "..#.#..", "..#.#..", "...#...", "...#..."])
glyph('W', ["#.....#", "#.....#", "#..#..#", "#.#.#.#", "#.#.#.#", "##...##", "#.....#", "#.....#"])
glyph('X', ["##...##", ".#...#.", "..#.#..", "...#...", "..#.#..", ".#...#.", ".#...#.", "##...##"])
glyph('Y', ["##...##", ".#...#.", "..#.#..", "...#...", "...#...", "...#...", "...#...", "..###.."])
glyph('Z', ["#######", "#....#.", "....#..", "...#...", "..#....", ".#.....", "#.....#", "#######"])

glyph('0', ["..###..", ".#...#.", "#....##", "#...#.#", "#.#...#", "##....#", ".#...#.", "..###.."])
glyph('1', ["...#...", "..##...", ".#.#...", "...#...", "...#...", "...#...", "...#...", ".#####."])
glyph('2', [".####..", "#....#.", ".....#.", "....#..", "...#...", "..#....", ".#.....", "#######"])
glyph('3', [".####..", "#....#.", ".....#.", "..###..", ".....#.", ".....#.", "#....#.", ".####.."])
glyph('4', ["....##.", "...#.#.", "..#..#.", ".#...#.", "#######", ".....#.", ".....#.", "....###"])
glyph('5', ["######.", "#......", "#......", "#####..", ".....#.", ".....#.", "#....#.", ".####.."])
glyph('6', ["..###..", ".#.....", "#......", "#####..", "#....#.", "#....#.", "#....#.", ".####.."])
glyph('7', ["#######", ".....#.", "....#..", "...#...", "..#....", "..#....", "..#....", "..#...."])
glyph('8', [".####..", "#....#.", "#....#.", ".####..", "#....#.", "#....#.", "#....#.", ".####.."])
glyph('9', [".####..", "#....#.", "#....#.", ".#####.", ".....#.", ".....#.", "....#..", ".###..."])

glyph('.', [".......", ".......", ".......", ".......", ".......", ".......", "..##...", "..##..."])
glyph(',', [".......", ".......", ".......", ".......", ".......", "..##...", "..##...", "..#...."])
glyph('!', ["..##...", "..##...", "..##...", "..##...", "..##...", ".......", "..##...", "..##..."])
glyph('?', [".####..", "#....#.", ".....#.", "....#..", "...#...", "...#...", ".......", "...#..."])
glyph("'", ["..##...", "..##...", "..#....", ".......", ".......", ".......", ".......", "......."])
glyph('-', [".......", ".......", ".......", ".#####.", ".#####.", ".......", ".......", "......."])
glyph('—', [".......", ".......", ".......", "#######", "#######", ".......", ".......", "......."])
glyph(':', [".......", "..##...", "..##...", ".......", ".......", "..##...", "..##...", "......."])
glyph(';', [".......", "..##...", "..##...", ".......", ".......", "..##...", "..##...", "..#...."])
glyph('(', ["...##..", "..#....", ".#.....", ".#.....", ".#.....", ".#.....", "..#....", "...##.."])
glyph(')', ["..##...", "....#..", ".....#.", ".....#.", ".....#.", ".....#.", "....#..", "..##..."])
glyph('/', ["......#", ".....#.", "....#..", "...#...", "..#....", ".#.....", "#......", "......."])
glyph('+', [".......", "...#...", "...#...", ".#####.", "...#...", "...#...", ".......", "......."])
glyph('%', ["##...#.", "##..#..", "...#...", "..#....", ".#..##.", "#...##.", ".......", "......."])
glyph('•', [".......", ".......", "..###..", ".#####.", ".#####.", "..###..", ".......", "......."])
glyph('✦', ["...#...", "...#...", "..###..", "#######", "..###..", "...#...", "...#...", "......."])    # etoile a 4 branches

ACC = {                      # marques placees sur les lignes 0..2 (ou 11 pour la cedille)
    'acute': {1: "....#..", 2: "...#..."},
    'grave': {1: "..#....", 2: "...#..."},
    'circ': {1: "...#...", 2: "..#.#.."},
    'diae': {2: "..#.#.."},
}
ACCENTED = {
    'À': ('A', 'grave'), 'Â': ('A', 'circ'), 'Ä': ('A', 'diae'),
    'É': ('E', 'acute'), 'È': ('E', 'grave'), 'Ê': ('E', 'circ'), 'Ë': ('E', 'diae'),
    'Î': ('I', 'circ'), 'Ï': ('I', 'diae'), 'Ô': ('O', 'circ'), 'Ö': ('O', 'diae'),
    'Ù': ('U', 'grave'), 'Û': ('U', 'circ'), 'Ü': ('U', 'diae'), 'Ç': ('C', 'cedil'),
}
LOWER_ACCENTED = {k.lower(): k for k in ACCENTED}


def cell(ch):
    """Image 8x12 (RGBA) d'un caractere : corps + accent."""
    img = Image.new('RGBA', (CELL_W, CELL_H), (0, 0, 0, 0))
    base, accent = (ch, None)
    if ch in ACCENTED:
        base, accent = ACCENTED[ch]
    rows = G[base]
    for y, row in enumerate(rows):
        for x, c in enumerate(row):
            if c == '#':
                img.putpixel((x, BASE_ROW + y), (255, 255, 255, 255))
    if accent == 'cedil':
        for x, c in enumerate("...##.."):
            if c == '#':
                img.putpixel((x, BASE_ROW + BODY_ROWS), (255, 255, 255, 255))
    elif accent:
        for y, row in ACC[accent].items():
            for x, c in enumerate(row):
                if c == '#':
                    img.putpixel((x, y), (255, 255, 255, 255))
    return img


def chars():
    out = []
    out += list("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
    out += list("abcdefghijklmnopqrstuvwxyz")
    out += list("0123456789")
    out += list(".,!?'-:;()/+%—•✦")
    out += list(ACCENTED.keys())
    out += list(LOWER_ACCENTED.keys())
    return out


def generate(assets_dir):
    cs = chars()
    cols = 16
    rows = (len(cs) + cols - 1) // cols
    sheet = Image.new('RGBA', (cols * CELL_W, rows * CELL_H), (0, 0, 0, 0))
    grid = []
    for r in range(rows):
        line = ''
        for c in range(cols):
            i = r * cols + c
            if i < len(cs):
                ch = cs[i]
                src = LOWER_ACCENTED.get(ch, ch.upper() if ch.upper() in G else ch)
                sheet.paste(cell(src if src in ACCENTED or src in G else ch), (c * CELL_W, r * CELL_H))
                line += ch
            else:
                line += '\u0000'
        grid.append(line)
    tex_dir = os.path.join(assets_dir, 'textures', 'font')
    font_dir = os.path.join(assets_dir, 'font')
    os.makedirs(tex_dir, exist_ok=True)
    os.makedirs(font_dir, exist_ok=True)
    sheet.save(os.path.join(tex_dir, 'eldoria.png'))
    definition = {"providers": [
        {"type": "space", "advances": {" ": 5}},
        {"type": "bitmap", "file": "mmorpg:font/eldoria.png", "ascent": 11, "height": 12, "chars": grid},
    ]}
    with open(os.path.join(font_dir, 'eldoria.json'), 'w', encoding='utf-8', newline='\n') as f:
        json.dump(definition, f, ensure_ascii=True, indent=2)
        f.write('\n')
    return sheet


def preview(path):
    text = ["ELDORIA", "LE GUERRIER - LE MAGE", "L'ARCHER, LE PALADIN", "A VOUS DE JOUER !", "ÉPREUVE DES HÉROS 0123456789", "Maître des classes"]
    scale = 4
    img = Image.new('RGBA', (420 * scale // 2, (len(text) * 14 + 4) * scale), (40, 36, 52, 255))
    y = 2
    for line in text:
        x = 4
        for ch in line:
            if ch == ' ':
                x += 5
                continue
            key = LOWER_ACCENTED.get(ch, ch.upper() if ch.upper() in G else ch)
            if key not in G and key not in ACCENTED:
                continue
            c = cell(key)
            bb = c.getbbox()
            if bb is None:
                continue
            c = c.crop((0, 0, bb[2], CELL_H))
            big = c.resize((c.width * scale, c.height * scale), Image.NEAREST)
            img.alpha_composite(big, (x * scale, y * scale))
            x += bb[2] + 1
        y += 14
    img.save(path)


if __name__ == '__main__':
    here = os.path.dirname(os.path.abspath(__file__))
    out = os.path.join(here, '..', 'src', 'main', 'resources', 'assets', 'mmorpg')
    generate(out)
    preview(os.path.join(here, 'apercu_police.png'))
    print('police generee')
