#!/usr/bin/env python3
"""Génère textures, modèles, états de bloc, items et textes des panneaux de classement (un par RankingKind).
Usage : python3 tools/generate_rankings.py  (Pillow requis). Ajouter un classement = l'ajouter à KINDS et à RankingKind.java."""
import json, math, re
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont

ASSETS = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mmorpg"
FONT = "/usr/share/fonts/truetype/dejavu/DejaVuSerif-Bold.ttf"
# id, titre, sous-titre, couleur, motif, nom FR, nom EN
KINDS = [
    ("niveau", "NIVEAUX", "Les plus hauts aventuriers", (255, 208, 64), "star", "Panneau de classement · Niveaux", "Ranking Board · Levels"),
    ("monstres", "CHASSEURS", "Monstres vaincus", (255, 106, 90), "blade", "Panneau de classement · Chasseurs", "Ranking Board · Hunters"),
    ("arene", "GLADIATEURS", "Victoires en arène", (79, 163, 255), "shield", "Panneau de classement · Arène", "Ranking Board · Arena"),
    ("fortune", "FORTUNES", "Or du porte-monnaie", (242, 201, 76), "coin", "Panneau de classement · Fortunes", "Ranking Board · Wealth"),
    ("boss", "TUEURS DE BOSS", "Boss vaincus", (182, 108, 255), "crown", "Panneau de classement · Boss", "Ranking Board · Bosses"),
    ("donjons", "DONJONS", "Donjons vaincus", (61, 190, 122), "shield", "Panneau de classement · Donjons", "Ranking Board · Dungeons"),
    ("quetes", "QUÊTES", "Quêtes terminées", (242, 153, 74), "star", "Panneau de classement · Quêtes", "Ranking Board · Quests"),
    ("guilde_niveau", "GUILDES", "Niveau de guilde", (182, 108, 255), "crown", "Panneau de classement · Guildes (niveau)", "Ranking Board · Guilds (level)"),
    ("guilde_banque", "GUILDES", "Fortune de la banque", (255, 160, 48), "coin", "Panneau de classement · Guildes (fortune)", "Ranking Board · Guilds (wealth)"),
    ("guilde_gardiens", "GUILDES", "Gardiens vaincus", (80, 255, 128), "tree", "Panneau de classement · Guildes (gardiens)", "Ranking Board · Guilds (guardians)"),
]
W, H = 192, 112
ROW_TOP, ROW_H = 25.7, 8.0

def mix(c, k):
    return tuple(max(0, min(255, int(v * k))) for v in c)

def motif(d, kind, cx, cy, r, col):
    if kind == "star":
        pts = [(cx + (r if i % 2 == 0 else r * .45) * math.sin(i * math.pi / 5), cy - (r if i % 2 == 0 else r * .45) * math.cos(i * math.pi / 5)) for i in range(10)]
        d.polygon(pts, fill=col)
    elif kind == "blade":
        d.polygon([(cx - 2, cy + r), (cx + 2, cy + r), (cx + 2, cy - r + 4), (cx, cy - r), (cx - 2, cy - r + 4)], fill=col)
        d.rectangle([cx - r * .6, cy + r * .2, cx + r * .6, cy + r * .2 + 2], fill=col)
    elif kind == "shield":
        d.polygon([(cx - r * .8, cy - r), (cx + r * .8, cy - r), (cx + r * .8, cy), (cx, cy + r), (cx - r * .8, cy)], fill=col)
        d.line([(cx, cy - r), (cx, cy + r * .8)], fill=(20, 20, 30), width=1)
    elif kind == "crown":
        d.polygon([(cx - r, cy + r * .6), (cx - r, cy - r * .5), (cx - r * .5, cy), (cx, cy - r * .8), (cx + r * .5, cy), (cx + r, cy - r * .5), (cx + r, cy + r * .6)], fill=col)
    elif kind == "coin":
        d.ellipse([cx - r, cy - r, cx + r, cy + r], fill=col)
        d.ellipse([cx - r * .6, cy - r * .6, cx + r * .6, cy + r * .6], outline=(20, 20, 30))
    elif kind == "tree":
        d.polygon([(cx, cy - r), (cx - r * .8, cy + r * .3), (cx + r * .8, cy + r * .3)], fill=col)
        d.rectangle([cx - 1, cy + r * .3, cx + 1, cy + r], fill=col)

def panel(kid, title, sub, col, kind):
    img = Image.new("RGB", (W, H), (22, 18, 34))
    d = ImageDraw.Draw(img)
    for y in range(H):
        d.line([(0, y), (W, y)], fill=mix((34, 28, 52), 1.0 - y / H * .45))
    for i in range(10):
        top = ROW_TOP + i * ROW_H
        if i % 2 == 0:
            d.rectangle([8, top, W - 9, top + ROW_H - 1], fill=(44, 37, 66))
    d.rectangle([0, 0, W - 1, 23], fill=mix(col, .28))
    d.line([(0, 23), (W, 23)], fill=col)
    d.rectangle([0, 0, W - 1, H - 1], outline=col, width=3)
    d.rectangle([4, 4, W - 5, H - 5], outline=mix(col, .45), width=1)
    big, small = ImageFont.truetype(FONT, 15), ImageFont.truetype(FONT, 8)
    tw = d.textlength(title, font=big)
    d.text(((W - tw) / 2, 0), title, font=big, fill=col)
    sw = d.textlength(sub, font=small)
    d.text(((W - sw) / 2, 15.5), sub, font=small, fill=(225, 218, 240))
    for cx in (20, W - 20):
        motif(d, kind, cx, 12, 8, col)
    img.save(ASSETS / f"textures/block/classement_{kid}.png")

def frame():
    img = Image.new("RGB", (16, 16), (58, 40, 30))
    d = ImageDraw.Draw(img)
    for y in range(0, 16, 4):
        d.line([(0, y), (15, y)], fill=(48, 33, 25))
    d.rectangle([0, 0, 15, 15], outline=(36, 25, 20))
    img.save(ASSETS / "textures/block/classement_cadre.png")

def box(a, b, front=False):
    faces = {f: {"texture": "#frame"} for f in ("south", "east", "west", "up", "down")}
    faces["north"] = {"texture": "#panel", "uv": [0, 0, 16, 16]} if front else {"texture": "#frame"}
    return {"from": a, "to": b, "faces": faces}

def model(kid):
    display = {
        "gui": {"rotation": [25, 200, 0], "translation": [0, 0, 0], "scale": [0.3, 0.3, 0.3]},
        "ground": {"scale": [0.2, 0.2, 0.2]},
        "fixed": {"rotation": [0, 180, 0], "scale": [0.28, 0.28, 0.28]},
        "thirdperson_righthand": {"scale": [0.2, 0.2, 0.2]},
        "firstperson_righthand": {"rotation": [0, 160, 0], "scale": [0.2, 0.2, 0.2]},
        "head": {"scale": [0.3, 0.3, 0.3]},
    }
    return {
        "credit": "Aeloria · panneau de classement (tools/generate_rankings.py)",
        "textures": {"panel": f"mmorpg:block/classement_{kid}", "frame": "mmorpg:block/classement_cadre", "particle": "#frame"},
        "elements": [
            box([-16, 4, 6], [32, 32, 8], True),
            box([-12, 0, 5.5], [-8, 4, 8.5]),
            box([6, 0, 5.5], [10, 4, 8.5]),
            box([24, 0, 5.5], [28, 4, 8.5]),
        ],
        "display": display,
    }

def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

def lang(file, key, value_idx):
    p = ASSETS / f"lang/{file}.json"
    text = p.read_text(encoding="utf-8")
    entries = "".join(f'  "block.mmorpg.classement_{k[0]}": {json.dumps(k[value_idx], ensure_ascii=False)},\n' for k in KINDS)
    text = re.sub(r'  "block\.mmorpg\.classement_[a-z_]+": .*\n', "", text)
    anchor = '  "block.mmorpg.hdv_aeloria"'
    assert anchor in text
    p.write_text(text.replace(anchor, entries + anchor, 1), encoding="utf-8")

frame()
for kid, title, sub, col, kind, fr, en in KINDS:
    panel(kid, title, sub, col, kind)
    write(ASSETS / f"models/block/classement_{kid}.json", model(kid))
    write(ASSETS / f"blockstates/classement_{kid}.json", {"variants": {f"facing={f}": {"model": f"mmorpg:block/classement_{kid}", **({"y": y} if y else {})} for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
    write(ASSETS / f"items/classement_{kid}.json", {"model": {"type": "minecraft:model", "model": f"mmorpg:block/classement_{kid}"}})
lang("fr_fr", "", 5)
lang("en_us", "", 6)
print("ok")
