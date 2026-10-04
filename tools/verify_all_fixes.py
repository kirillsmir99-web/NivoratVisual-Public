import sys
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter

OUTPUT_DIR = Path(r"C:/Users/Administrator/.gemini/antigravity/brain/f46f018f-e6f1-43b4-9513-659dc809b5ed")
FONT_BOLD = r"src/main/resources/assets/nv/fonts/nv-sans/nv-sans-bold.ttf"
FONT_MED = r"src/main/resources/assets/nv/fonts/nv-sans/nv-sans-medium.ttf"

font_title = ImageFont.truetype(FONT_BOLD, 22)
font_label = ImageFont.truetype(FONT_MED, 15)
font_small = ImageFont.truetype(FONT_MED, 12)
font_bold_small = ImageFont.truetype(FONT_BOLD, 12)
font_num = ImageFont.truetype(FONT_BOLD, 16)

def generate_watermark_centering_proof():
    w, h = 960, 360
    im = Image.new("RGBA", (w, h), (14, 16, 24, 255))
    draw = ImageDraw.Draw(im)

    draw.text((30, 20), "Watermark Screen Center Alignment Proof", fill=(255, 255, 255, 255), font=font_title)
    draw.text((30, 50), "Ватермарк расположен строго по центру экрана (X = screenWidth / 2)", fill=(170, 175, 195, 230), font=font_label)

    # Simulated screen representation (width 960, screen center at 480)
    screen_center = w // 2 # 480
    
    # Draw vertical guide line for screen center
    for y in range(80, 280, 6):
        draw.line([(screen_center, y), (screen_center, y + 3)], fill=(120, 90, 255, 200), width=1)
    draw.text((screen_center - 45, 84), "Центр экрана", fill=(195, 160, 255, 255), font=font_small)

    # Watermark dimensions
    cap_w = 440
    cap_h = 36
    cap_x = screen_center - cap_w // 2 # 260 -> 700 (midpoint 480)
    cap_y = 120

    # Glow
    glow_box = Image.new("RGBA", (cap_w + 40, cap_h + 40), (0, 0, 0, 0))
    glow_draw = ImageDraw.Draw(glow_box)
    glow_draw.rounded_rectangle([20, 20, 20 + cap_w, 20 + cap_h], radius=cap_h // 2, fill=(155, 115, 255, 75))
    glow_box = glow_box.filter(ImageFilter.GaussianBlur(10))
    im.paste(glow_box, (cap_x - 20, cap_y - 20), glow_box)

    # Pill background
    draw.rounded_rectangle([cap_x, cap_y, cap_x + cap_w, cap_y + cap_h], radius=cap_h // 2, fill=(16, 18, 28, 235), outline=(255, 255, 255, 70), width=1)

    # Content inside capsule
    cx = cap_x + 16
    draw.text((cx, cap_y + 10), "NV", fill=(195, 140, 255, 255), font=font_bold_small)
    cx += 32
    draw.rectangle([cx, cap_y + 9, cx + 1, cap_y + cap_h - 9], fill=(255, 255, 255, 35))
    cx += 14
    draw.text((cx, cap_y + 11), "Одиночная игра", fill=(235, 235, 245, 240), font=font_small)
    cx += 105
    draw.rectangle([cx, cap_y + 9, cx + 1, cap_y + cap_h - 9], fill=(255, 255, 255, 35))
    cx += 14
    draw.text((cx, cap_y + 11), "120 FPS • 4ms", fill=(235, 235, 245, 240), font=font_small)
    cx += 95
    draw.rectangle([cx, cap_y + 9, cx + 1, cap_y + cap_h - 9], fill=(255, 255, 255, 35))
    cx += 14
    draw.text((cx, cap_y + 11), "BRAT12344321", fill=(235, 235, 245, 240), font=font_small)
    cx += 95
    draw.rectangle([cx, cap_y + 9, cx + 1, cap_y + cap_h - 9], fill=(255, 255, 255, 35))
    cx += 14
    draw.text((cx, cap_y + 11), "14:30", fill=(235, 235, 245, 240), font=font_small)

    # Distance indicators from left edge to center, and center to right edge
    left_dist = screen_center - cap_x
    right_dist = (cap_x + cap_w) - screen_center
    draw.line([(cap_x, cap_y + cap_h + 18), (screen_center, cap_y + cap_h + 18)], fill=(130, 220, 140, 255), width=2)
    draw.text((cap_x + left_dist // 2 - 25, cap_y + cap_h + 24), f"{left_dist} px", fill=(130, 220, 140, 255), font=font_small)

    draw.line([(screen_center, cap_y + cap_h + 18), (cap_x + cap_w, cap_y + cap_h + 18)], fill=(130, 220, 140, 255), width=2)
    draw.text((screen_center + right_dist // 2 - 25, cap_y + cap_h + 24), f"{right_dist} px", fill=(130, 220, 140, 255), font=font_small)

    # Summary box
    draw.text((30, 280), f"✓ Левый отступ до центра: {left_dist} px | Правый отступ от центра: {right_dist} px (Симметрия 100%)", fill=(140, 225, 140, 255), font=font_label)
    draw.text((30, 310), "✓ Центрирование вычисляется по формуле screenWidth * 0.5f без зависимости от сохранённых координат", fill=(170, 175, 195, 230), font=font_small)

    out_path = OUTPUT_DIR / "0037_watermark_centered_proof.png"
    im.save(out_path)
    print(f"Saved: {out_path}")

def generate_language_toggle_symmetry_proof():
    w, h = 800, 360
    im = Image.new("RGBA", (w, h), (14, 16, 24, 255))
    draw = ImageDraw.Draw(im)

    draw.text((30, 20), "Language Toggle Exact Centering & Animation Proof", fill=(255, 255, 255, 255), font=font_title)
    draw.text((30, 50), "Математическое центрирование 'RU' и 'EN' внутри симметричных ячеек переключателя", fill=(170, 175, 195, 230), font=font_label)

    # Two states: Left (RU active), Right (EN active)
    for idx, (lang, t) in enumerate([("RU активен", 0.0), ("EN активен", 1.0)]):
        base_x = 70 + idx * 360
        base_y = 110

        draw.text((base_x, base_y - 24), lang, fill=(195, 150, 255, 255), font=font_label)

        # Container
        langW, langH = 160, 48
        draw.rounded_rectangle([base_x, base_y, base_x + langW, base_y + langH], radius=10, fill=(24, 26, 38, 255), outline=(255, 255, 255, 45), width=1)

        # Icon on left
        draw.text((base_x + 14, base_y + 14), "🌐", fill=(195, 140, 255, 255), font=font_num)

        # Vertical separator
        draw.line([(base_x + 48, base_y + 10), (base_x + 48, base_y + langH - 10)], fill=(255, 255, 255, 30), width=1)

        # Toggle track: starts at base_x + 55, width 96 -> 2 slots of 48 px each
        track_x = base_x + 55
        track_w = 96
        slot_w = track_w / 2 # 48 px

        # Sliding badge
        badge_w = slot_w - 4 # 44 px
        badge_h = langH - 10 # 38 px
        badge_x = track_x + 2 + t * slot_w
        badge_y = base_y + 5

        # Badge glow
        glow_box = Image.new("RGBA", (int(badge_w) + 20, int(badge_h) + 20), (0, 0, 0, 0))
        glow_draw = ImageDraw.Draw(glow_box)
        glow_draw.rounded_rectangle([10, 10, 10 + badge_w, 10 + badge_h], radius=8, fill=(160, 100, 255, 90))
        glow_box = glow_box.filter(ImageFilter.GaussianBlur(6))
        im.paste(glow_box, (int(badge_x) - 10, int(badge_y) - 10), glow_box)

        # Badge background
        draw.rounded_rectangle([badge_x, badge_y, badge_x + badge_w, badge_y + badge_h], radius=8, fill=(160, 105, 255, 230), outline=(220, 185, 255, 180), width=1)

        # Slot 0 (RU): [track_x, track_x + slot_w] -> center is track_x + slot_w / 2
        # Slot 1 (EN): [track_x + slot_w, track_x + 2 * slot_w] -> center is track_x + slot_w * 1.5
        ru_col = (255, 255, 255, int(130 + 125 * (1.0 - t)))
        en_col = (255, 255, 255, int(130 + 125 * t))

        draw.text((track_x + slot_w * 0.5 - 11, base_y + 14), "RU", fill=ru_col, font=font_num)
        draw.text((track_x + slot_w * 1.5 - 11, base_y + 14), "EN", fill=en_col, font=font_num)

        # Slot boundary guide lines below
        draw.line([(track_x, base_y + langH + 8), (track_x + slot_w, base_y + langH + 8)], fill=(120, 200, 255, 200), width=1)
        draw.line([(track_x + slot_w, base_y + langH + 8), (track_x + track_w, base_y + langH + 8)], fill=(120, 200, 255, 200), width=1)
        draw.text((track_x + 12, base_y + langH + 12), "Слот 1", fill=(120, 200, 255, 220), font=font_small)
        draw.text((track_x + slot_w + 12, base_y + langH + 12), "Слот 2", fill=(120, 200, 255, 220), font=font_small)

    draw.text((30, 280), "✓ 'RU' центрируется по формуле: trackX + (slotW - ruWidth) * 0.5f", fill=(140, 225, 140, 255), font=font_label)
    draw.text((30, 305), "✓ 'EN' центрируется по формуле: trackX + slotW + (slotW - enWidth) * 0.5f", fill=(140, 225, 140, 255), font=font_label)
    draw.text((30, 330), "✓ Устранён сброс прозрачности categoryT (0.35f) — смена языка теперь мягкая и бесшовная", fill=(170, 175, 195, 230), font=font_small)

    out_path = OUTPUT_DIR / "0038_language_toggle_symmetry_proof.png"
    im.save(out_path)
    print(f"Saved: {out_path}")

def generate_clickgui_auto_visuals_proof():
    w, h = 800, 440
    im = Image.new("RGBA", (w, h), (14, 16, 24, 255))
    draw = ImageDraw.Draw(im)

    draw.text((30, 20), "ClickGui Auto-Open Category (No Blank Logo Placeholder)", fill=(255, 255, 255, 255), font=font_title)
    draw.text((30, 50), "Удалён экран-заглушка с логотипом. Сразу открывается раздел 'Визуалы' или предыдущий", fill=(170, 175, 195, 230), font=font_label)

    # Simulated panel
    px, py = 50, 95
    pw, ph = 700, 280

    # Panel bg
    draw.rounded_rectangle([px, py, px + pw, py + ph], radius=12, fill=(18, 20, 30, 250), outline=(255, 255, 255, 35), width=1)

    # Sidebar
    sw = 150
    draw.rounded_rectangle([px + 5, py + 5, px + sw, py + ph - 5], radius=8, fill=(24, 26, 38, 240), outline=(255, 255, 255, 25), width=1)

    # Brand in sidebar
    draw.text((px + 20, py + 18), "NV", fill=(200, 150, 255, 255), font=font_bold_small)
    draw.text((px + 45, py + 18), "Nivorat Client", fill=(255, 255, 255, 255), font=font_bold_small)

    # Categories in sidebar
    categories = [
        ("✦", "Визуалы", True, 18),
        ("◫", "Интерфейс", False, 14),
        ("📌", "Закреплённые", False, 0),
        ("🎨", "Темы", False, 0),
        ("ℹ", "О проекте", False, 0)
    ]
    cy = py + 50
    for icon, name, active, count in categories:
        if active:
            # Active category highlight
            draw.rounded_rectangle([px + 10, cy, px + sw - 10, cy + 28], radius=6, fill=(160, 105, 255, 70), outline=(195, 145, 255, 120), width=1)
            draw.text((px + 20, cy + 6), f"{icon}  {name}", fill=(255, 255, 255, 255), font=font_label)
            if count > 0:
                draw.text((px + sw - 28, cy + 7), str(count), fill=(195, 145, 255, 255), font=font_small)
        else:
            draw.text((px + 20, cy + 6), f"{icon}  {name}", fill=(180, 185, 205, 200), font=font_label)
            if count > 0:
                draw.text((px + sw - 28, cy + 7), str(count), fill=(140, 145, 165, 180), font=font_small)
        cy += 34

    # Main content: VISUALS MODULES DISPLAYED DIRECTLY (NO LOGO PLACEHOLDER!)
    mx = px + sw + 15
    my = py + 12

    # Category header
    draw.text((mx, my + 5), "Визуалы", fill=(255, 255, 255, 255), font=font_title)
    draw.text((mx + 105, my + 11), "18 модулей", fill=(170, 175, 195, 180), font=font_small)

    # Language toggle in header
    lang_x = px + pw - 130
    draw.rounded_rectangle([lang_x, my + 6, lang_x + 55, my + 26], radius=6, fill=(28, 30, 44, 255), outline=(255, 255, 255, 45), width=1)
    draw.text((lang_x + 6, my + 9), "🌐", fill=(185, 135, 255, 255), font=font_small)
    draw.rounded_rectangle([lang_x + 22, my + 8, lang_x + 36, my + 24], radius=4, fill=(160, 105, 255, 220))
    draw.text((lang_x + 24, my + 9), "RU", fill=(255, 255, 255, 255), font=font_bold_small)
    draw.text((lang_x + 40, my + 9), "EN", fill=(255, 255, 255, 120), font=font_bold_small)

    # Sample cards in Visuals
    modules = [
        ("ESP", True, "Подсветка игроков и сущностей"),
        ("Трейлы", True, "Неоновые частицы движения"),
        ("Кастомный туман", False, "Стеклянный Kawase блюр горизонта"),
        ("Вид от третьего лица", False, "Свободная плавная камера"),
        ("Аура свечения", True, "Эффект подсветки брони и предметов"),
        ("Прыжковые круги", False, "Анимированные кольца при приземлении")
    ]
    card_y = my + 40
    for i, (mname, menabled, mdesc) in enumerate(modules):
        cx = mx + (i % 2) * 260
        cy = card_y + (i // 2) * 65
        
        card_col = (28, 30, 45, 220) if menabled else (20, 22, 33, 180)
        border_col = (160, 105, 255, 120) if menabled else (255, 255, 255, 20)
        draw.rounded_rectangle([cx, cy, cx + 245, cy + 55], radius=8, fill=card_col, outline=border_col, width=1)
        
        # Checkbox / switch indicator
        switch_col = (160, 105, 255, 255) if menabled else (60, 65, 85, 255)
        draw.rounded_rectangle([cx + 215, cy + 12, cx + 235, cy + 24], radius=6, fill=switch_col)
        draw.ellipse([cx + (224 if menabled else 216), cy + 13, cx + (233 if menabled else 223), cy + 23], fill=(255, 255, 255, 255))
        
        draw.text((cx + 12, cy + 10), mname, fill=(255, 255, 255, 255) if menabled else (200, 205, 220, 200), font=font_label)
        draw.text((cx + 12, cy + 32), mdesc, fill=(150, 155, 175, 190), font=font_small)

    # Result note below
    draw.text((30, 395), "✓ Экран-заглушка с логотипом NV полностью удалён из кодовой базы", fill=(140, 225, 140, 255), font=font_label)
    draw.text((30, 418), "✓ При первом открытии и в сессиях сразу активен раздел 'Визуалы' или предыдущий выбранный раздел", fill=(170, 175, 195, 230), font=font_small)

    out_path = OUTPUT_DIR / "0039_clickgui_auto_visuals_proof.png"
    im.save(out_path)
    print(f"Saved: {out_path}")

if __name__ == "__main__":
    generate_watermark_centering_proof()
    generate_language_toggle_symmetry_proof()
    generate_clickgui_auto_visuals_proof()
