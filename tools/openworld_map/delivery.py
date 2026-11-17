"""Package only the new, natively generated map. No production-world backup is made."""
from pathlib import Path
import sys,zipfile,hashlib,json
out=Path(sys.argv[1]);native=Path(sys.argv[2]);repo=Path(__file__).resolve().parents[2]
assert json.loads((out/'native-report.json').read_text())['verified']
assert (native.parent/'openworld-map-checks.txt').read_text().startswith('PASS:')
target=out/'Aeloria-Openworld-800.zip'
with zipfile.ZipFile(target,'w',compression=zipfile.ZIP_DEFLATED,compresslevel=6) as z:
    for p in native.rglob('*'):
        if p.is_file() and p.name not in ['session.lock','level.dat_old']:z.write(p,'openworld/'+p.relative_to(native).as_posix())
    for name in ['README.md','VALIDATION.md','map.json','general-openworld-fragment.json','carte-openworld.png']:
        z.write(repo/'maps/openworld-800'/name,'documentation/'+name)
    z.write(native.parent/'openworld-map-checks.txt','documentation/openworld-map-checks.txt')
sha=hashlib.sha256(target.read_bytes()).hexdigest();(out/'Aeloria-Openworld-800.zip.sha256').write_text(f'{sha}  {target.name}\n')
print(json.dumps({'file':str(target),'bytes':target.stat().st_size,'sha256':sha}))
