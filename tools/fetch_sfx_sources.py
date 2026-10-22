# -*- coding: utf-8 -*-
"""Telecharge les sources sonores libres (CC0) utilisees par build_sfx.py depuis OpenGameArt.

Chaque page est verifiee : seule une page dont la licence affichee est « CC0 » est telechargee. Les fichiers vont dans un dossier de cache
(par defaut tools/_sfx_cache, ignore par git) : un dossier par page, les archives zip y sont decompressees. La liste des sources
(titre, auteur, adresse, licence) est ecrite dans CREDITS_SONS.md a la racine du depot. Aucune dependance externe.

usage : python tools/fetch_sfx_sources.py [dossier_de_cache]
"""
import html as htmllib
import io
import os
import re
import sys
import urllib.parse
import urllib.request
import zipfile

BASE = 'https://opengameart.org'
PAGES = [
    # pages « content/<slug> » d'OpenGameArt (toutes CC0 au moment de la verification)
    'rpg-sound-pack', '50-rpg-sound-effects', '100-cc0-sfx', '80-cc0-rpg-sfx', 'just-a-random-fanfare', 'war-horns',
    'cc0-deep-monster-roar', 'pleasing-bell-sound-effect', 'magic-spell-sfx', 'energy-drain', 'teleport-spell', 'cure-magic',
    'flight-or-spell-sound', 'portal-sound', 'swishes-sound-pack', 'air-whoosh', 'heartbeat-sounds', 'horde-war-drums-loop',
    '10-impactshield-blocks', '20-sword-sound-effects-attacks-and-clashes',
]
EXT = ('.zip', '.ogg', '.wav', '.mp3', '.flac')
ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), '..')


def get(url):
    req = urllib.request.Request(url, headers={'User-Agent': 'Mozilla/5.0 (Eldoria sfx fetch)'})
    with urllib.request.urlopen(req, timeout=120) as r:
        return r.read()


def licence_ok(page):
    m = re.search(r'License\(s\):(.{0,600})', page, re.S)
    return bool(m) and 'CC0' in m.group(1) and 'CC-BY' not in m.group(1) and 'GPL' not in m.group(1)


def meta(page, slug):
    t = re.search(r'<title>([^<]*?)\s*\|\s*OpenGameArt', page)
    a = re.search(r'href="/users/[^"]+">([^<]+)<', page)
    return htmllib.unescape(t.group(1)) if t else slug, htmllib.unescape(a.group(1)) if a else '?'


def main():
    cache = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)), '_sfx_cache')
    os.makedirs(cache, exist_ok=True)
    credits = []
    for slug in PAGES:
        out = os.path.join(cache, slug)
        try:
            page = get(f'{BASE}/content/{slug}').decode('utf-8', 'replace')
        except Exception as e:                                   # noqa: BLE001
            print('ECHEC page', slug, e)
            continue
        if not licence_ok(page):
            print('IGNORE (licence non CC0)', slug)
            continue
        title, author = meta(page, slug)
        credits.append((title, author, f'{BASE}/content/{slug}'))
        if os.path.isdir(out) and os.listdir(out):
            continue
        urls = []
        for u in re.findall(r'https?://opengameart\.org/sites/default/files/[^"\'<> ]+', page):
            low = u.lower().split('?')[0]
            if low.endswith(EXT) and '/audio_preview/' not in low and '/styles/' not in low and u not in urls:
                urls.append(u)
        os.makedirs(out, exist_ok=True)
        for u in urls:
            name = urllib.parse.unquote(u.split('/')[-1])
            try:
                data = get(u)
            except Exception as e:                               # noqa: BLE001
                print('  echec', u, e)
                continue
            if name.lower().endswith('.zip'):
                try:
                    zipfile.ZipFile(io.BytesIO(data)).extractall(os.path.join(out, name[:-4]))
                except zipfile.BadZipFile:
                    print('  zip invalide', name)
            else:
                with open(os.path.join(out, name), 'wb') as f:
                    f.write(data)
        print('ok', slug, len(urls), 'fichier(s)')
    path = os.path.join(ROOT, 'CREDITS_SONS.md')
    with open(path, 'w', encoding='utf-8', newline='\n') as f:
        f.write('# Sources des effets sonores\n\n'
                'Les effets sonores d\'Eldoria (`src/main/resources/assets/mmorpg/sounds/sfx/`) sont montes par `tools/build_sfx.py` '
                '(couches, hauteur, reverberation) a partir d\'enregistrements publies sous licence **CC0** (domaine public) sur OpenGameArt.org. '
                'Aucune attribution n\'est exigee ; la liste ci-dessous remercie leurs auteurs.\n\n')
        for title, author, url in credits:
            f.write(f'- **{title}** — {author} — <{url}>\n')
    print('credits ecrits :', path)


if __name__ == '__main__':
    main()
