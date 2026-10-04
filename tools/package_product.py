"""Create a public shared source archive and a separate private wing asset pack."""
from pathlib import Path
import hashlib
import json
import zipfile

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / "build/releases"
OUT.mkdir(parents=True, exist_ok=True)
RETIRED_FONTS = {"sf-pro", "monsterat", "i2", "icons", "event-icons", "inv-icons", "heart", "mainmenu", "smallpixel"}

def paid_wing(path):
    value = path.as_posix()
    return ("assets/nv/models/wings/" in value or "assets/nv/textures/wings/" in value) and path.name not in {"crystal.json", "crystal.png"}

def source_allowed(path):
    parts = path.parts
    if "backup" in parts or path.name.endswith((".private.json", ".log")): return False
    if paid_wing(path): return False
    if "fonts" in parts and any(name in parts for name in RETIRED_FONTS): return False
    if path.name == "WaveyCapes.txt": return False
    return True

props_text = (ROOT / "gradle.properties").read_text(encoding="utf-8")
version = [line.split("=", 1)[1].strip() for line in props_text.splitlines() if line.startswith("mod_version")][0]
source = OUT / f"NivoratFreeVisual-source-{version}.zip"
entries = [ROOT / name for name in ("build.gradle", "settings.gradle", "gradle.properties", "gradlew", "gradlew.bat", "README.md", ".gitignore")]
for folder in ("src", "gradle/wrapper", "tools/media-helper"):
    entries.extend(p for p in (ROOT / folder).rglob("*") if p.is_file() and source_allowed(p.relative_to(ROOT)))
entries.extend(ROOT / "tools" / name for name in ("build_editions.ps1", "package_product.py", "verify_jar.py", "build_wing_assets.py"))
with zipfile.ZipFile(source, "w", zipfile.ZIP_DEFLATED) as archive:
    for path in sorted(set(entries)):
        archive.write(path, path.relative_to(ROOT).as_posix())
    archive.write(ROOT / "src/main/resources/LICENSE_nv", "LICENSE")
    archive.write(ROOT / "docs/PRODUCT-RELEASE.md", "docs/PRODUCT-RELEASE.md")

private = OUT / f"NivoratVisual-cosmetics-{version}.zip"
with zipfile.ZipFile(private, "w", zipfile.ZIP_DEFLATED) as archive:
    for folder in ("src/main/resources/assets/nv/models/wings", "src/main/resources/assets/nv/textures/wings"):
        for path in sorted((ROOT / folder).iterdir()):
            if path.is_file() and paid_wing(path): archive.write(path, path.relative_to(ROOT).as_posix())

with zipfile.ZipFile(source) as archive:
    assert not any(paid_wing(Path(name)) for name in archive.namelist())
    assert len([n for n in archive.namelist() if n.startswith("src/main/resources/assets/nv/models/wings/")]) == 1
with zipfile.ZipFile(private) as archive:
    assert len(archive.namelist()) == 62

result = {p.name: {"bytes": p.stat().st_size, "sha256": hashlib.sha256(p.read_bytes()).hexdigest()} for p in (source, private)}
(OUT / "source-manifest.json").write_text(json.dumps(result, indent=2), encoding="utf-8")
print(json.dumps(result, indent=2))
