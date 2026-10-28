#!/usr/bin/env python3
"""Genere modeles, etats de bloc, objets et textures des panneaux de classement (un par classement).
Usage : python3 tools/gen-leaderboards.py   (les identifiants et couleurs doivent rester alignes sur leaderboard/Board.java)"""
import json, random
from pathlib import Path
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent / "src/main/resources/assets/mmorpg"
BOARDS = [  # id, couleur d'accent, nom francais, nom anglais
    ("niveau", 0x3FA9F5, "Niveau", "Level"),
    ("fortune", 0xF2C94C, "Fortune", "Wealth"),
    ("monstres", 0xE5533D, "Chasseurs", "Hunters"),
    ("boss", 0x9B51E0, "Tueurs de boss", "Boss Slayers"),
    ("donjons", 0x3DBE7A, "Donjons", "Dungeons"),
    ("quetes", 0xF2994A, "Quêtes", "Quests"),
    ("pvp_victoires", 0xE0457B, "Arène : victoires", "Arena: Wins"),
    ("pvp_elims", 0xB02A2A, "Arène : éliminations", "Arena: Eliminations"),
    ("guildes", 0x2DC4B2, "Guildes", "Guilds"),
]
FACES = ("north", "east", "south", "west", "up", "down")


def box(name, frm, to, tex):
    return {"name": name, "from": frm, "to": to, "faces": {f: {"texture": "#" + tex, "uv": [0, 0, 16, 16]} for f in FACES}}


def model(board):
    return {
        "credit": "Aeloria · panneau de classement",
        "textures": {"cadre": "mmorpg:block/classement_cadre", "panel": "mmorpg:block/classement_panel",
                     "accent": f"mmorpg:block/classement_{board}", "particle": "#cadre"},
        "elements": [
            box("Socle", [-12, 0, 4], [28, 2, 12], "cadre"),
            box("Pied gauche", [-10, 2, 6], [-6, 6, 10], "cadre"),
            box("Pied droit", [22, 2, 6], [26, 6, 10], "cadre"),
            box("Cadre", [-14, 6, 6], [30, 32, 8], "cadre"),
            box("Tableau", [-12.5, 7.5, 5.5], [28.5, 30.5, 6], "panel"),
            box("Bandeau", [-12.5, 26.5, 5.0], [28.5, 30.5, 5.5], "accent"),
            box("Filet", [-12.5, 25.8, 5.3], [28.5, 26.5, 5.5], "accent"),
        ],
    }


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


def textures():
    rnd = random.Random(7)
    d = ROOT / "textures/block"
    d.mkdir(parents=True, exist_ok=True)
    wood = Image.new("RGB", (16, 16))
    for y in range(16):
        for x in range(16):
            g = 58 + int(10 * ((x * 3 + (y // 4) * 5) % 4)) + rnd.randint(-4, 4)
            wood.putpixel((x, y), (g + 18, g + 4, g - 14))
    wood.save(d / "classement_cadre.png")
    panel = Image.new("RGB", (16, 16))
    for y in range(16):
        for x in range(16):
            v = 20 + y // 2 + rnd.randint(0, 3)
            panel.putpixel((x, y), (v, v + 4, v + 16))
    panel.save(d / "classement_panel.png")
    for board, color, _, _ in BOARDS:
        r, g, b = color >> 16, (color >> 8) & 255, color & 255
        img = Image.new("RGB", (16, 16))
        for y in range(16):
            for x in range(16):
                k = 1.18 if y in (0, 15) else 0.82 if (x + y) % 8 == 0 else 1.0
                img.putpixel((x, y), (min(255, int(r * k)), min(255, int(g * k)), min(255, int(b * k))))
        img.save(d / f"classement_{board}.png")


def main():
    textures()
    fr = {}
    en = {}
    for board, _, name_fr, name_en in BOARDS:
        bid = f"classement_{board}"
        write(ROOT / f"models/block/{bid}.json", model(board))
        write(ROOT / f"blockstates/{bid}.json", {"variants": {
            f"facing={f}": ({"model": f"mmorpg:block/{bid}"} | ({"y": y} if y else {}))
            for f, y in (("north", 0), ("east", 90), ("south", 180), ("west", 270))}})
        write(ROOT / f"items/{bid}.json", {"model": {"type": "minecraft:model", "model": f"mmorpg:block/{bid}"}})
        fr[f"block.mmorpg.{bid}"] = f"Classement · {name_fr}"
        en[f"block.mmorpg.{bid}"] = f"Leaderboard · {name_en}"
    for lang, entries in (("fr_fr", fr), ("en_us", en)):
        p = ROOT / f"lang/{lang}.json"
        data = json.loads(p.read_text(encoding="utf-8"))
        data.update(entries)
        p.write_text(json.dumps(data, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")


main()
