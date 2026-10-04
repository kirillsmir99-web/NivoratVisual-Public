"""Generate official Nivorat Visual (NV) cape texture.

Replaces the old legacy cape with a modern dark obsidian stealth fabric,
sleek geometric cyber-frame, and the official Nivorat NV monogram logo.
Preserves full CapeGradient dynamic theme recoloring compatibility.
"""
from pathlib import Path
import numpy as np
from PIL import Image, ImageDraw

ROOT = Path(__file__).resolve().parent.parent
LOGO_PATH = ROOT / "src/main/resources/assets/nv/textures/logo.png"
TARGET_PATH = ROOT / "src/main/resources/assets/nv/textures/capes/cape.png"

def build_nv_cape():
    w, h = 2048, 1024
    img = Image.new("RGBA", (w, h), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # 1. Background fabric (RGB strictly <= 26 to stay below CapeGradient threshold of 40)
    for y in range(32, 544):
        t = (y - 32) / 512.0
        val = int(round(13 - 5 * t)) # 13 down to 8
        b_val = val + 3
        draw.line([(32, y), (351, y)], fill=(val, val, b_val, 255))
        draw.line([(384, y), (703, y)], fill=(val - 2, val - 2, b_val - 1, 255))
        draw.line([(0, y), (31, y)], fill=(val - 1, val - 1, b_val, 255))
        draw.line([(352, y), (383, y)], fill=(val - 1, val - 1, b_val, 255))
        
    for y in range(0, 32):
        draw.line([(32, y), (351, y)], fill=(13, 13, 16, 255))
        
    # 2. Sleek outer frame & corner notches (RGB strictly <= 32)
    draw.rectangle([40, 40, 344, 536], outline=(22, 24, 30, 255), width=2)
    for cx, cy in [(40, 40), (344, 40), (40, 536), (344, 536)]:
        draw.rectangle([cx - 2, cy - 2, cx + 2, cy + 2], fill=(25, 27, 34, 255))
    draw.line([(192, 510), (140, 495)], fill=(22, 24, 32, 255), width=2)
    draw.line([(192, 510), (244, 495)], fill=(22, 24, 32, 255), width=2)
    
    # 3. Inner face lining frame
    draw.rectangle([392, 40, 696, 536], outline=(18, 20, 26, 255), width=2)
    draw.line([(544, 510), (492, 495)], fill=(18, 20, 26, 255), width=2)
    draw.line([(544, 510), (596, 495)], fill=(18, 20, 26, 255), width=2)

    # 4. Place official Nivorat NV monogram logo
    assert LOGO_PATH.exists(), f"Logo not found: {LOGO_PATH}"
    logo = Image.open(LOGO_PATH)
    logo_crop = logo.crop(logo.getbbox())
    
    # Target size: width=220, height=212 (width >= height guarantees projUseX = true)
    target_w, target_h = 220, 212
    logo_resized = logo_crop.resize((target_w, target_h), Image.Resampling.LANCZOS)
    
    # Center horizontally at X=192, upper-middle vertically at Y=235
    pos_x = 192 - target_w // 2
    pos_y = 235 - target_h // 2
    img.alpha_composite(logo_resized, (pos_x, pos_y))
    
    # Verify CapeGradient compatibility
    arr = np.array(img)
    max_rgb = np.maximum(arr[:, :, 0], np.maximum(arr[:, :, 1], arr[:, :, 2]))
    mask = (arr[:, :, 3] >= 8) & (max_rgb > 40)
    ys, xs = np.where(mask)
    assert len(xs) > 0, "No emblem pixels found!"
    bx0, bx1 = xs.min(), xs.max()
    by0, by1 = ys.min(), ys.max()
    proj_use_x = (bx1 - bx0) >= (by1 - by0)
    
    print(f"NV Cape generated successfully.")
    print(f"Emblem bounds: X[{bx0}, {bx1}] (w={bx1 - bx0 + 1}), Y[{by0}, {by1}] (h={by1 - by0 + 1})")
    print(f"CapeGradient projUseX: {proj_use_x} (MUST be True for horizontal gradient)")
    assert proj_use_x, "CapeGradient projUseX is False! Width must be >= height."
    
    TARGET_PATH.parent.mkdir(parents=True, exist_ok=True)
    img.save(TARGET_PATH, "PNG")
    print(f"Saved to: {TARGET_PATH}")

if __name__ == "__main__":
    build_nv_cape()
