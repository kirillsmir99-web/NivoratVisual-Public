"""Install either NivoratVisual (pro) or NivoratFreeVisual (free) to PVP Pack instance."""
import sys
import shutil
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RELEASES = ROOT / "build/releases"
MODS_DIR = Path(r"C:\Users\Administrator\AppData\Roaming\ElyPrismLauncher\instances\PVP Pack\minecraft\mods")
TARGET = MODS_DIR / "nv-1.0.0.jar"

edition = sys.argv[1].lower() if len(sys.argv) > 1 else "pro"
if edition in ("free", "trial", "бесплатная", "фри"):
    prefix = "NivoratFreeVisual"
    edition_label = "Free"
else:
    prefix = "NivoratVisual"
    edition_label = "Pro (NivoratVisual)"

props_text = (ROOT / "gradle.properties").read_text(encoding="utf-8")
version = [line.split("=", 1)[1].strip() for line in props_text.splitlines() if line.startswith("mod_version")][0]

candidates = [
    RELEASES / f"{prefix}-{version}.jar",
    ROOT / "build/libs" / f"{prefix}-{version}.jar",
]
src_path = next((p for p in candidates if p.exists()), None)
assert src_path is not None, f"Artifact not found in candidates: {[str(c) for c in candidates]}"
src_name = src_path.name

# Ensure mods dir exists and clean up any backup jars
MODS_DIR.mkdir(parents=True, exist_ok=True)
for item in MODS_DIR.iterdir():
    if item.name.startswith("nv-1.0.0.jar.backup"):
        try:
            item.unlink()
        except Exception:
            pass

shutil.copy2(src_path, TARGET)

def sha256(p):
    h = hashlib.sha256()
    with open(p, "rb") as f:
        while chunk := f.read(65536):
            h.update(chunk)
    return h.hexdigest()

src_hash = sha256(src_path)
dest_hash = sha256(TARGET)
assert src_hash == dest_hash

manifest_path = RELEASES / "release-manifest.json"
if manifest_path.exists():
    data = json.loads(manifest_path.read_text(encoding="utf-8"))
    data["installation"] = f"Installed {src_name} ({edition_label}) -> {TARGET}"
    manifest_path.write_text(json.dumps(data, indent=2, ensure_ascii=False), encoding="utf-8")

print(f"SUCCESS: Installed {src_name} ({edition_label}) to {TARGET}")
print(f"SHA-256: {dest_hash}")
