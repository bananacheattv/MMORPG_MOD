#!/usr/bin/env python3
"""Convertit les PNG de docs/screens en WebP et génère docs/screens/index.html (galerie légendée)."""
import html, pathlib, re
from PIL import Image
ROOT = pathlib.Path(__file__).resolve().parents[2] / "docs" / "screens"
SECTIONS = [
 ("Personnage & inventaire", ["ecrans/", "pvp_polish/showcase_inventory", "pvp_polish/showcase_menus", "pvp_polish/showcase_pause", "pvp_polish/showcase_shift"]),
 ("Onglets du menu (Métiers, Succès, Guilde…)", ["onglets/"]),
 ("Compétences (24 icônes)", ["competences/"]),
 ("Armes, arc et bouclier", ["armes/", "pvp_polish/showcase_bow", "pvp_polish/showcase_shield", "pvp_polish/showcase_weapons"]),
 ("Quêtes hebdomadaires", ["weekly/", "reprise_quetes/"]),
 ("Guildes & arènes", ["social_ui/", "pvp_polish/showcase_arena", "pvp_polish/showcase_pvp", "pvp_polish/showcase_buffs", "pvp_polish/showcase_chest", "pvp_polish/showcase_armor"]),
 ("Banque & hôtel des ventes", ["services/"]),
 ("Échange (/trade)", ["echange/"]),
 ("Tableau des joueurs", ["reprise_tab/"]),
]
def caption(rel):
    n = re.sub(r"^showcase_", "", pathlib.Path(rel).stem)
    return n.replace("_", " ")
groups = {t: [] for t, _ in SECTIONS}
for png in sorted(ROOT.rglob("*.png")):
    rel = png.relative_to(ROOT).as_posix()
    webp = png.with_suffix(".webp")
    Image.open(png).convert("RGB").save(webp, "WEBP", quality=80)
    for title, prefixes in SECTIONS:
        if any(rel.startswith(p) for p in prefixes):
            groups[title].append(webp.relative_to(ROOT).as_posix()); break
CSS = """:root{--bg:#f4f2f8;--fg:#221b32;--card:#ffffff;--mut:#6a6180;--ac:#6f48c4;--line:#ddd7ea}
@media (prefers-color-scheme: dark){:root:not([data-theme="light"]){--bg:#141020;--fg:#ece8f5;--card:#1e1830;--mut:#a79fba;--ac:#b79cf2;--line:#332b4a;color-scheme:dark}}
:root[data-theme="dark"]{--bg:#141020;--fg:#ece8f5;--card:#1e1830;--mut:#a79fba;--ac:#b79cf2;--line:#332b4a;color-scheme:dark}
body{background:var(--bg);color:var(--fg);font:16px/1.5 system-ui,sans-serif;margin:0;padding-block:28px;padding-left:max(16px,env(safe-area-inset-left,0px));padding-right:max(16px,env(safe-area-inset-right,0px))}
main{max-width:1200px;margin-inline:auto}h1{margin:0 0 4px;font-size:28px;text-wrap:balance}h2{margin:40px 0 12px;color:var(--ac);font-size:20px}h2 small{color:var(--mut);font-weight:400}p.s{color:var(--mut);margin:0}
.g{display:grid;grid-template-columns:repeat(auto-fill,minmax(min(100%,260px),1fr));gap:12px}figure{margin:0;background:var(--card);border:1px solid var(--line);border-radius:10px;overflow:hidden;min-width:0}
figure img{width:100%;max-width:100%;display:block;aspect-ratio:16/9;object-fit:cover}figcaption{padding:8px 10px;font-size:13px;color:var(--mut);overflow-wrap:anywhere}a:focus-visible{outline:2px solid var(--ac)}"""
BODY = ['<main><h1>Captures du mod Aeloria</h1><p class="s">Générées par tools/screens/capture.sh (client Xvfb, rendu logiciel). Cliquer une image pour l\'ouvrir en grand.</p>']
out = BODY
for title, imgs in groups.items():
    if not imgs: continue
    out.append(f"<h2>{html.escape(title)} <small>{len(imgs)}</small></h2><div class=\"g\">")
    for i in imgs:
        out.append(f'<figure><a href="{i}"><img loading="lazy" src="{i}" alt=""></a><figcaption>{html.escape(caption(i))}</figcaption></figure>')
    out.append("</div>")
out.append("</main>")
HEAD = '<title>Captures Aeloria</title><meta name="claude:viewport-fit-sides" content="cover"><style>' + CSS + '</style>'
body = "\n".join(out)
(ROOT / "index.html").write_text('<!doctype html><html lang="fr"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1">' + HEAD + '</head><body>' + body + '</body></html>', encoding="utf-8")
(ROOT / "artifact.html").write_text(HEAD + body, encoding="utf-8")
print({t: len(v) for t, v in groups.items()})
