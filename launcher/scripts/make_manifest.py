"""Genere <dist>/manifest.json pour le launcher.

python3 make_manifest.py <dist> <base_url> <version> <minecraft> <neoforge> <notes...>
Les mods externes (launcher/extra_mods.json) sont resolus sur Modrinth pour la bonne version de Minecraft / NeoForge.
"""
import hashlib
import json
import os
import sys
import urllib.parse
import urllib.request
from datetime import datetime, timezone

HERE = os.path.dirname(os.path.abspath(__file__))


def sha(path):
    h = hashlib.sha256()
    with open(path, 'rb') as f:
        for chunk in iter(lambda: f.read(1 << 16), b''):
            h.update(chunk)
    return h.hexdigest()


def modrinth(slug, mc):
    q = urllib.parse.urlencode({'loaders': json.dumps(['neoforge']), 'game_versions': json.dumps([mc])})
    req = urllib.request.Request(f'https://api.modrinth.com/v2/project/{slug}/version?{q}',
                                 headers={'User-Agent': 'bananacheattv/MMORPG_MOD launcher'})
    with urllib.request.urlopen(req, timeout=30) as r:
        versions = json.load(r)
    if not versions:
        return None
    best = next((v for v in versions if v['version_type'] == 'release'), versions[0])
    f = next((f for f in best['files'] if f.get('primary')), best['files'][0])
    return {'file': f['filename'], 'url': f['url'], 'sha512': f['hashes']['sha512'], 'name': f"{slug} {best['version_number']}"}


dist, base, version, mc, neoforge, *notes = sys.argv[1:]
mods = [{'file': f, 'url': f'{base}/{f}', 'sha256': sha(os.path.join(dist, f))}
        for f in sorted(os.listdir(dist)) if f.endswith('.jar') and f not in ('launcher-core.jar', 'EldoriaLauncher.jar')]
with open(os.path.join(HERE, '..', 'extra_mods.json'), encoding='utf-8') as f:
    for extra in json.load(f):
        try:
            m = modrinth(extra['modrinth'], mc)
        except Exception as e:  # Modrinth indisponible : on publie quand meme
            m = None
            print(f"::warning::{extra['modrinth']} : {e}")
        if m:
            mods.append(m)
        else:
            print(f"::warning::{extra['modrinth']} introuvable pour NeoForge {mc}")
manifest = {
    'version': version,
    'date': datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M UTC'),
    'notes': [n for n in notes if n.strip()],
    'minecraft': mc,
    'neoforge': neoforge,
    'bootstrapVersion': 1,
    'launcherCoreUrl': f'{base}/launcher-core.jar',
    'launcherCoreSha256': sha(os.path.join(dist, 'launcher-core.jar')),
    'releaseUrl': base.replace('/download/', '/tag/'),
    'msaClientId': os.environ.get('MSA_CLIENT_ID', ''),
    'mods': mods,
}
with open(os.path.join(dist, 'manifest.json'), 'w', encoding='utf-8') as f:
    json.dump(manifest, f, indent=2, ensure_ascii=False)
print(json.dumps(manifest, indent=2, ensure_ascii=False))
