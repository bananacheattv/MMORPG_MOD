"""Genere <dist>/manifest.json pour le launcher.

python3 make_manifest.py <dist> <base_url> <version> <minecraft> <neoforge> <notes...>
Les mods externes (launcher/extra_mods.json) sont resolus sur Modrinth pour la bonne version de Minecraft / NeoForge :
on retient la version la plus recente (release de preference) dont le NeoForge requis n'est pas plus recent que le notre.
"""
import hashlib
import io
import json
import os
import re
import sys
import urllib.parse
import urllib.request
import zipfile
from datetime import datetime, timezone

HERE = os.path.dirname(os.path.abspath(__file__))


def sha(path):
    h = hashlib.sha256()
    with open(path, 'rb') as f:
        for chunk in iter(lambda: f.read(1 << 16), b''):
            h.update(chunk)
    return h.hexdigest()


def version_tuple(text):
    """'26.3.0.39-beta' -> (26, 3, 0, 39) ; le suffixe (-beta) est ignore."""
    nums = re.findall(r'\d+', text.split('-')[0])
    return tuple(int(n) for n in nums) if nums else None


def required_neoforge(jar_bytes):
    """Borne basse du NeoForge requis par un mod (neoforge.mods.toml), ou None si elle est introuvable."""
    try:
        with zipfile.ZipFile(io.BytesIO(jar_bytes)) as z:
            name = next(n for n in z.namelist() if n.endswith('neoforge.mods.toml'))
            toml = z.read(name).decode('utf-8', 'replace')
    except Exception:
        return None
    m = re.search(r'modId\s*=\s*"neoforge"[^\[]*?versionRange\s*=\s*"\[?([^,\])"]+)', toml, re.S)
    return version_tuple(m.group(1)) if m else None


def modrinth(slug, mc, neoforge):
    q = urllib.parse.urlencode({'loaders': json.dumps(['neoforge']), 'game_versions': json.dumps([mc])})
    req = urllib.request.Request(f'https://api.modrinth.com/v2/project/{slug}/version?{q}',
                                 headers={'User-Agent': 'bananacheattv/MMORPG_MOD launcher'})
    with urllib.request.urlopen(req, timeout=30) as r:
        versions = json.load(r)
    ours = version_tuple(neoforge)
    compatible = []
    for v in versions:                                   # plus recentes d'abord
        f = next((f for f in v['files'] if f.get('primary')), v['files'][0])
        with urllib.request.urlopen(urllib.request.Request(f['url'], headers={'User-Agent': 'bananacheattv/MMORPG_MOD launcher'}), timeout=120) as r:
            need = required_neoforge(r.read())
        if need is not None and ours is not None and need > ours:
            print(f"::notice::{slug} {v['version_number']} exige NeoForge {'.'.join(map(str, need))} (nous : {neoforge}) : ignore")
            continue
        compatible.append((v, f))
        if v['version_type'] == 'release':
            break
    if not compatible:
        return None
    v, f = next((c for c in compatible if c[0]['version_type'] == 'release'), compatible[0])
    return {'file': f['filename'], 'url': f['url'], 'sha512': f['hashes']['sha512'], 'name': f"{slug} {v['version_number']}"}


dist, base, version, mc, neoforge, *notes = sys.argv[1:]
mods = [{'file': f, 'url': f'{base}/{f}', 'sha256': sha(os.path.join(dist, f))}
        for f in sorted(os.listdir(dist)) if f.endswith('.jar') and f not in ('launcher-core.jar', 'EldoriaLauncher.jar')]
with open(os.path.join(HERE, '..', 'extra_mods.json'), encoding='utf-8') as f:
    for extra in json.load(f):
        try:
            m = modrinth(extra['modrinth'], mc, neoforge)
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
