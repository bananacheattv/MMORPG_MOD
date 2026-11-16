#!/usr/bin/env python3
"""Génère les images du site (site/assets/img) à partir des illustrations du mod.

Usage (depuis la racine du dépôt) :
    pip install pillow fonttools brotli
    python3 site/outils/generer_assets.py

Sources : art/logo/ et src/main/resources/assets/mmorpg/textures/gui/.
Sorties : AVIF + WebP (photos), WebP (logo transparent), PNG (icônes), JPEG (image de partage).
Le script est déterministe : relancez-le après avoir remplacé une illustration source.
"""
from __future__ import annotations

import io
from pathlib import Path

from PIL import Image, ImageDraw, ImageFilter, ImageFont

ROOT = Path(__file__).resolve().parents[2]
GUI = ROOT / "src/main/resources/assets/mmorpg/textures/gui"
LOGO = ROOT / "art/logo"
OUT = ROOT / "site/assets/img"
FONTS = ROOT / "site/assets/fonts"

BG = (11, 14, 20)  # #0B0E14


def save_photo(img: Image.Image, name: str, widths: list[int], avif_q: int = 52, webp_q: int = 78) -> None:
    """Enregistre une photo en AVIF + WebP pour chaque largeur demandée."""
    img = img.convert("RGB")
    for w in widths:
        h = round(img.height * w / img.width)
        r = img.resize((w, h), Image.LANCZOS) if w != img.width else img
        r.save(OUT / f"{name}-{w}.avif", quality=avif_q, speed=4)
        r.save(OUT / f"{name}-{w}.webp", quality=webp_q, method=6)
        print(f"  {name}-{w} ({w}x{h})")


def alpha_bbox(img: Image.Image, threshold: int = 16) -> tuple[int, int, int, int]:
    """Boîte englobante des pixels dont l'alpha dépasse le seuil (ignore la poussière transparente)."""
    a = img.getchannel("A").point(lambda v: 255 if v >= threshold else 0)
    return a.getbbox()


def cinzel(size: int, weight: int = 700) -> ImageFont.FreeTypeFont:
    """Charge Cinzel (variable) depuis le WOFF2 du site, à la graisse demandée."""
    from fontTools.ttLib import TTFont

    buf = io.BytesIO()
    font = TTFont(FONTS / "cinzel-latin-wght.woff2")
    font.flavor = None
    font.save(buf)
    buf.seek(0)
    f = ImageFont.truetype(buf, size)
    try:
        f.set_variation_by_axes([weight])
    except OSError:
        pass
    return f


def main() -> None:
    OUT.mkdir(parents=True, exist_ok=True)

    print("Hero (citadelle de Valdhéris)")
    hero = Image.open(GUI / "brand/citadel_twilight.png")
    save_photo(hero, "hero-citadelle", [960, 1672])

    print("Univers (3 chapitres)")
    for src, name in [
        ("portal_loading_background", "univers-seuil"),
        ("arena_fire_cover", "univers-terres-brisees"),
        ("dungeon_crypt_cover", "univers-malvorn"),
    ]:
        save_photo(Image.open(GUI / f"illustrations/{src}.png"), name, [480, 836])

    print("Classes (découpe de l'atlas)")
    atlas = Image.open(GUI / "illustrations/classes_atlas.png").convert("RGB")
    slice_w = atlas.width / 4
    for i, name in enumerate(["guerrier", "mage", "archer", "tank"]):
        left = round(i * slice_w) + 4
        right = round((i + 1) * slice_w) - 4
        portrait = atlas.crop((left, 0, right, atlas.height))
        portrait = portrait.resize((480, round(atlas.height * 480 / (right - left))), Image.LANCZOS)
        save_photo(portrait.crop((0, 0, 480, 780)), f"classe-{name}", [480])  # même format pour les 4

    print("Logo + emblème")
    logo = Image.open(LOGO / "aeloria_logo.png").convert("RGBA")
    logo = logo.crop(alpha_bbox(logo))
    for w in [560, 840, 1120]:
        h = round(logo.height * w / logo.width)
        r = logo.resize((w, h), Image.LANCZOS)
        r.save(OUT / f"logo-{w}.avif", quality=60, speed=4)
        r.save(OUT / f"logo-{w}.webp", quality=86, method=6)
        print(f"  logo-{w} ({w}x{h})")

    emblem = Image.open(LOGO / "aeloria_emblem.png").convert("RGBA")
    l, t, r, b = alpha_bbox(emblem)
    side = max(r - l, b - t)
    cx, cy = (l + r) // 2, (t + b) // 2
    emblem = emblem.crop((cx - side // 2, cy - side // 2, cx + side // 2, cy + side // 2))
    for s in [96, 256]:
        emblem.resize((s, s), Image.LANCZOS).save(OUT / f"embleme-{s}.webp", quality=88, method=6)
    emblem.resize((32, 32), Image.LANCZOS).save(OUT / "favicon-32.png", optimize=True)
    for s, name in [(180, "apple-touch-icon.png"), (192, "icon-192.png"), (512, "icon-512.png")]:
        canvas = Image.new("RGBA", (s, s), BG + (255,))
        inner = round(s * 0.82)
        e = emblem.resize((inner, inner), Image.LANCZOS)
        canvas.alpha_composite(e, ((s - inner) // 2, (s - inner) // 2))
        canvas.convert("RGB").save(OUT / name, optimize=True)
    print("  emblème, favicon, icônes")

    print("Image de partage (Open Graph 1200x630)")
    og_w, og_h = 1200, 630
    src = hero.convert("RGB")
    scale = og_w / src.width
    og = src.resize((og_w, round(src.height * scale)), Image.LANCZOS)
    top = max(0, (og.height - og_h) // 2)
    og = og.crop((0, top, og_w, top + og_h)).convert("RGBA")
    # Voile sombre : dégradé vertical + vignette pour la lisibilité du logo.
    veil = Image.new("RGBA", (og_w, og_h))
    d = ImageDraw.Draw(veil)
    for y in range(og_h):
        k = 0.35 + 0.55 * (y / og_h) ** 1.4
        d.line([(0, y), (og_w, y)], fill=BG + (round(255 * k),))
    og.alpha_composite(veil)
    og_logo_w = 640
    ol = logo.resize((og_logo_w, round(logo.height * og_logo_w / logo.width)), Image.LANCZOS)
    og.alpha_composite(ol, ((og_w - og_logo_w) // 2, 150))
    draw = ImageDraw.Draw(og)
    text = "LE SEUIL S'OUVRE. ÉCRIS TA LÉGENDE."
    font = cinzel(40, 700)
    tw = draw.textlength(text, font=font)
    draw.text(((og_w - tw) / 2 + 2, 452), text, font=font, fill=(0, 0, 0, 160))
    draw.text(((og_w - tw) / 2, 450), text, font=font, fill=(232, 230, 227, 255))
    sub = "SERVEUR MINECRAFT JAVA · MMORPG"
    font2 = cinzel(24, 600)
    sw = draw.textlength(sub, font=font2)
    draw.text(((og_w - sw) / 2, 515), sub, font=font2, fill=(212, 168, 75, 255))
    og.convert("RGB").save(OUT / "og-aeloria.jpg", quality=84, optimize=True, progressive=True)
    print("  og-aeloria.jpg")


if __name__ == "__main__":
    main()
