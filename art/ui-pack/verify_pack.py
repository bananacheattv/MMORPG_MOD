"""Validate delivered UI art. Does not claim that any Minecraft interface is integrated."""
from pathlib import Path
import hashlib
import json
import re
from PIL import Image

root = Path(__file__).resolve().parent
manifest = json.loads((root / "manifest.json").read_text(encoding="utf-8"))
ids = set()
for asset in manifest["assets"]:
    assert asset["id"] not in ids, "Duplicate asset ID"
    ids.add(asset["id"])
    assert not asset["id"].startswith("world_map"), "World map was explicitly excluded"
    path = root / asset["path"]
    assert path.resolve().is_relative_to(root.resolve()), "Asset outside pack"
    assert hashlib.sha256(path.read_bytes()).hexdigest() == asset["sha256"], path
    with Image.open(path) as im:
        im.verify()
    with Image.open(path) as im:
        assert im.size == (asset["width"], asset["height"]), path
        alpha = im.getchannel("A").getextrema() if "A" in im.getbands() else (255, 255)
        assert list(alpha) == asset["alpha_range"], path
    regions = asset.get("regions", [])
    for i, region in enumerate(regions):
        x, y, w, h = (region[k] for k in ("x", "y", "width", "height"))
        assert min(x, y) >= 0 and min(w, h) > 0
        assert x + w <= asset["width"] and y + h <= asset["height"]
        u0, v0, u1, v1 = region["uv"]
        assert 0 <= u0 < u1 <= 1 and 0 <= v0 < v1 <= 1
        for other in regions[i + 1:]:
            assert (x + w <= other["x"] or other["x"] + other["width"] <= x
                    or y + h <= other["y"] or other["y"] + other["height"] <= y), "Overlapping atlas cells"
    if regions:
        assert sum(r["width"] * r["height"] for r in regions) == asset["width"] * asset["height"]
for doc in root.glob("*.md"):
    for link in re.findall(r"\]\(([^)]+)\)", doc.read_text(encoding="utf-8")):
        if "://" not in link and not link.startswith("#"):
            assert (doc.parent / link.split("#")[0]).exists(), (doc, link)
assert len(list((root / "images").glob("*.png"))) == len(ids), "Unlisted PNG"
print(f"PASS: {len(ids)} PNG, hashes, dimensions, alpha, atlas and documentation links")
