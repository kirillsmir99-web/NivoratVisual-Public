"""Inspect the exact product ZIP, including metadata and bundled license records."""
import hashlib
import io
import json
from pathlib import Path
import sys
import zipfile

path = Path(sys.argv[1])
with zipfile.ZipFile(path) as jar:
    names = jar.namelist()
    metadata = json.loads(jar.read("fabric.mod.json"))
    edition = jar.read("nv-edition.properties").decode().strip().split("=", 1)[1]
    assert edition in ("free", "pro")
    assert metadata["name"] == ("NivoratFreeVisual" if edition == "free" else "NivoratVisual")
    wing_models = [n for n in names if n.startswith("assets/nv/models/wings/") and n.endswith(".json")]
    wing_textures = [n for n in names if n.startswith("assets/nv/textures/wings/") and n.endswith(".png")]
    assert len(wing_models) == (1 if edition == "free" else 32), wing_models
    assert len(wing_textures) == len(wing_models)
    if edition == "free": assert wing_models == ["assets/nv/models/wings/crystal.json"]
    assert not any("AutoDuel" in n or n.endswith("/SlotItem.class") or n.endswith("/SlotCategory.class") for n in names)
    for name in names:
        if name.endswith((".class", ".json", ".properties")) and not name.startswith("META-INF/"):
            assert b"kimiko" not in jar.read(name).lower(), "Retired branding: " + name
    nested = [item["file"] for item in metadata.get("jars", [])]
    forbidden = [name for name in names if "waveycapes/" in name.lower() or "fonts/sf-pro/" in name.lower()
                 or name.startswith("rtx/nv/test/") or "backup/" in name.lower() or name.endswith(".private.json")]
    assert not forbidden, forbidden
    assert len(nested) == len(set(nested)), "Duplicate nested libraries"
    _props = (Path(__file__).resolve().parent.parent / "gradle.properties").read_text(encoding="utf-8")
    expected_version = [l.split("=", 1)[1].strip() for l in _props.splitlines() if l.startswith("mod_version")][0]
    assert metadata["version"] == expected_version
    assert metadata["depends"]["minecraft"] == "1.21.11"
    assert jar.read("helper/NvMediaHelper.exe") == Path("src/main/resources/helper/NvMediaHelper.exe").read_bytes()
    assert not any("helper/" + old in names for old in ("YandexMusicHelper.exe", "VolumeControl.exe", "media_control.py"))
    assert "THIRD-PARTY-NOTICES.md" in names
    assert "licenses/Manrope-OFL.txt" in names
    for retired in ("monsterat", "i2", "icons", "event-icons", "inv-icons", "heart", "mainmenu", "smallpixel"):
        assert not any(n.startswith(f"assets/nv/fonts/{retired}/") for n in names), retired
    for weight in ("regular", "medium", "semibold", "bold"):
        base = f"assets/nv/fonts/nv-sans/nv-sans-{weight}"
        for suffix in ("ttf", "png", "json"):
            assert f"{base}.{suffix}" in names
        glyphs = {g["unicode"] for g in json.loads(jar.read(base + ".json"))["glyphs"]}
        assert all(ord(c) in glyphs for c in "Ёё Корона Перезарядка NV"), weight
    icons = json.loads(jar.read("assets/nv/fonts/nv/nv.json"))
    assert set(range(0xE001, 0xE030)) <= {g["unicode"] for g in icons["glyphs"]}
    assert "rtx/nv/api/modules/impl/Visuals/Crown.class" in names
    assert "rtx/nv/api/modules/impl/Visuals/ChinaHat.class" not in names
    assert "licenses/NightConfig-LGPL-3.0.txt" in names
    assert "licenses/GPL-3.0.txt" in names
    events = json.loads(jar.read("assets/nv/sounds.json"))
    assert "licenses/kenney-interface-sounds.txt" in names
    assert "licenses/kenney-impact-sounds.txt" in names
    for sound in ("button", "slider", "category", "open", "close", "toggle_on", "toggle_off", "dropdown_open", "dropdown_close", "pin", "unpin", "error", "notify"):
        assert f"assets/nv/sounds/nv_glass/{sound}.ogg" in names
    assert "assets/nv/sounds/hit_sounds/nv_tactical.ogg" in names
    notices = {}
    for name in nested:
        with zipfile.ZipFile(io.BytesIO(jar.read(name))) as dependency:
            notices[name] = [entry for entry in dependency.namelist() if "license" in entry.lower() or "notice" in entry.lower()]
    for variant in ("spirit", "dragon", "moth", "mascot"):
        assert f"assets/nv/textures/entity/pets/{variant}.png" in names
        assert f"assets/nv/geckolib/models/pets/{variant}.geo.json" in names
print(json.dumps({"status":"passed", "file":path.name, "size":path.stat().st_size,
                  "edition":edition, "wing_models":len(wing_models),
                  "sha256":hashlib.sha256(path.read_bytes()).hexdigest(), "version":metadata["version"],
                  "minecraft":metadata["depends"]["minecraft"], "nested_libraries":nested,
                  "bundled_license_files":notices, "root_license_files": [n for n in names if n.startswith("licenses/") and not n.endswith("/")],
                  "excluded": ["WaveyCapes", "SF Pro", "gametests", "backups", "private sessions"]},indent=2))
