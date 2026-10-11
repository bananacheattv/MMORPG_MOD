"""Genere le pack de textures 'AutoPeche_Detection' pour faciliter la detection.

  - bouchon (entity/fishing_hook.png)            -> MAGENTA plein
  - bulles, eclaboussures, eclatement de bulles  -> JAUNE plein
  - eau (option --eau-sombre, active par defaut) -> uniforme et sombre (fort contraste)

Usage : python gen_texture_pack.py [--sortie DOSSIER] [--eau-claire]
Puis copiez AutoPeche_Detection.zip dans .minecraft/resourcepacks et activez-le
(au-dessus des autres packs).
"""
import argparse
import io
import json
import os
import zipfile

from PIL import Image, ImageDraw

MAGENTA = (255, 0, 255, 255)
JAUNE = (255, 235, 0, 255)

PARTICULES = ([f"bubble.png"]
              + [f"bubble_pop_{i}.png" for i in range(5)]
              + [f"splash_{i}.png" for i in range(4)])


def solid(size, color, border=None):
    img = Image.new("RGBA", (size, size), color)
    if border:
        ImageDraw.Draw(img).rectangle((0, 0, size - 1, size - 1), outline=border)
    return img


def png(img):
    b = io.BytesIO()
    img.save(b, "PNG")
    return b.getvalue()


def build(eau_sombre=True):
    files = {}
    tex = "assets/minecraft/textures/"
    # Bouchon : magenta pur (bord legerement plus sombre pour garder la forme lisible)
    hook = png(solid(8, MAGENTA, (200, 0, 200, 255)))
    for path in ("entity/fishing_hook.png", "entity/fishing/fishing_hook.png", "entity/fishing_bobber.png"):
        files[tex + path] = hook
    # Particules : jaune pur, entierement opaque (surface detectable maximale)
    for name in PARTICULES:
        files[tex + "particle/" + name] = png(solid(8, JAUNE))
    if eau_sombre:
        # Texture en niveaux de gris teintee par le biome -> bleu tres sombre, sans animation
        files[tex + "block/water_still.png"] = png(solid(16, (70, 70, 70, 235)))
        files[tex + "block/water_flow.png"] = png(solid(32, (70, 70, 70, 235)))

    icon = Image.new("RGBA", (64, 64), (16, 30, 60, 255))
    d = ImageDraw.Draw(icon)
    d.ellipse((20, 20, 44, 44), fill=MAGENTA)
    for x, y in ((10, 12), (48, 10), (8, 46), (50, 50), (30, 6)):
        d.ellipse((x, y, x + 7, y + 7), fill=JAUNE)
    files["pack.png"] = png(icon)

    files["pack.mcmeta"] = json.dumps({"pack": {
        "description": "§dAutoPeche§r : bouchon magenta, bulles jaunes",
        "pack_format": 46,
        "supported_formats": [15, 999],
        "min_format": 15,
        "max_format": 999,
    }}, indent=2).encode()
    return files


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--sortie", default=os.path.dirname(os.path.abspath(__file__)))
    ap.add_argument("--eau-claire", action="store_true", help="ne pas modifier l'eau")
    a = ap.parse_args()
    out = os.path.join(a.sortie, "AutoPeche_Detection.zip")
    with zipfile.ZipFile(out, "w", zipfile.ZIP_DEFLATED) as z:
        for path, data in build(not a.eau_claire).items():
            z.writestr(path, data)
    print("Pack genere :", out)


if __name__ == "__main__":
    main()
