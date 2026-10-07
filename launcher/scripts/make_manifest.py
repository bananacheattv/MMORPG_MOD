"""Genere dist/manifest.json pour le launcher : python3 make_manifest.py <dist> <base_url> <version> <neoforge> <notes...>"""
import hashlib
import json
import os
import sys
from datetime import datetime, timezone


def sha(path):
    h = hashlib.sha256()
    with open(path, 'rb') as f:
        for chunk in iter(lambda: f.read(1 << 16), b''):
            h.update(chunk)
    return h.hexdigest()


dist, base, version, neoforge, *notes = sys.argv[1:]
mods = [{'file': f, 'url': f'{base}/{f}', 'sha256': sha(os.path.join(dist, f))}
        for f in sorted(os.listdir(dist)) if f.endswith('.jar') and f not in ('launcher-core.jar', 'EldoriaLauncher.jar')]
manifest = {
    'version': version,
    'date': datetime.now(timezone.utc).strftime('%Y-%m-%d %H:%M UTC'),
    'notes': [n for n in notes if n.strip()],
    'neoforge': neoforge,
    'bootstrapVersion': 1,
    'launcherCoreUrl': f'{base}/launcher-core.jar',
    'launcherCoreSha256': sha(os.path.join(dist, 'launcher-core.jar')),
    'releaseUrl': base.replace('/download/', '/tag/'),
    'javaArgs': '-Xmx4G -XX:+UseG1GC',
    'mods': mods,
}
with open(os.path.join(dist, 'manifest.json'), 'w', encoding='utf-8') as f:
    json.dump(manifest, f, indent=2, ensure_ascii=False)
print(json.dumps(manifest, indent=2, ensure_ascii=False))
