"""Build the short NV glass/synth sound bank from CC0 Kenney samples.

Input: Kenney Interface Sounds ZIP, https://kenney.nl/assets/interface-sounds.
Output: mono Vorbis, 44.1 kHz, bounded peaks, short fades (no clipping).
"""
import io
import json
import sys
import zipfile
from pathlib import Path

import numpy as np
import soundfile as sf

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / "src/main/resources/assets/nv"
RATE = 44100


def build(archive):
    bank = {
        "button": ("glass_001", .075, 1046.5),
        "slider": ("click_001", .035, 1318.5),
        "category": ("glass_002", .11, 880),
        "open": ("glass_003", .16, 659.25),
        "close": ("glass_004", .13, 523.25),
        "toggle_on": ("glass_002", .12, 783.99),
        "toggle_off": ("glass_004", .10, 587.33),
        "dropdown_open": ("click_002", .075, 987.77),
        "dropdown_close": ("click_003", .065, 739.99),
        "pin": ("glass_005", .13, 1174.66),
        "unpin": ("glass_006", .10, 698.46),
    }
    dest = ASSETS / "sounds/nv_glass"
    dest.mkdir(parents=True, exist_ok=True)
    events = json.loads((ASSETS / "sounds.json").read_text(encoding="utf-8-sig"))
    with zipfile.ZipFile(archive) as z:
        for name, (sample, duration, frequency) in bank.items():
            audio, rate = sf.read(io.BytesIO(z.read(f"Audio/{sample}.ogg")), always_2d=True)
            mono = audio.mean(axis=1)
            n = round(RATE * duration)
            t = np.arange(n) / RATE
            glass = np.interp(t, np.arange(len(mono)) / rate, mono, right=0)
            peak = max(np.max(np.abs(glass)), .001)
            glass = glass / peak * .13
            tone = (np.sin(2*np.pi*frequency*t) + .18*np.sin(2*np.pi*frequency*2*t)) * .065*np.exp(-t*26)
            signal = glass + tone
            fade = np.minimum(1, t / .003) * np.minimum(1, (duration-t)/.018)
            signal = np.clip(signal * fade, -.22, .22)
            sf.write(dest / f"{name}.ogg", signal, RATE, format="OGG", subtype="VORBIS")
            events[f"nv_glass_{name}"] = {"sounds": [f"nv:nv_glass/{name}"]}
            decoded, _ = sf.read(dest / f"{name}.ogg")
            assert np.max(np.abs(decoded)) < .3, name
            print(f"{name}: {duration*1000:.0f} ms, peak {np.max(np.abs(decoded)):.3f}")
        (ROOT / "src/main/resources/licenses/kenney-interface-sounds.txt").write_bytes(z.read("License.txt"))
    (ASSETS / "sounds.json").write_text(json.dumps(events, indent=4, ensure_ascii=False)+"\n", encoding="utf-8")


if __name__ == "__main__":
    build(sys.argv[1])
