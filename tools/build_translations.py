import os
import sys
import re
import json

sys.stdout.reconfigure(encoding='utf-8')

def to_key(text):
    if not text:
        return ""
    return re.sub(r'[^a-z0-9_\u0430-\u044f\u0451]', '', text.strip().lower().replace(' ', '_').replace('-', '_'))

def clean_ru(text):
    if not text:
        return ""
    # Zero Dash Rule: strictly remove em-dash, en-dash, and isolated hyphens
    t = text.replace(" — ", " ").replace(" – ", " ").replace(" - ", " ")
    t = t.replace("—", "").replace("–", "")
    return re.sub(r'\s+', ' ', t).strip()

# Built-in translation map for setting names
SETTING_TRANSLATIONS = {
    # Common / General
    "Основное": "General",
    "Настройки": "Settings",
    "Вид": "Appearance",
    "Внешний вид": "Appearance",
    "Стиль": "Style",
    "Режим": "Mode",
    "Анимация": "Animation",
    "Анимации": "Animations",
    "Время": "Duration",
    "Длительность": "Duration",
    "Задержка": "Delay",
    "Скорость": "Speed",
    "Размер": "Size",
    "Масштаб": "Scale",
    "Ширина": "Width",
    "Высота": "Height",
    "Радиус": "Radius",
    "Отступ": "Padding",
    "Позиция": "Position",
    "Позиция X": "Position X",
    "Позиция Y": "Position Y",
    "Цвет": "Color",
    "Цвет 1": "Color 1",
    "Цвет 2": "Color 2",
    "Второй цвет": "Secondary Color",
    "Режим цвета": "Color Mode",
    "Прозрачность": "Opacity",
    "Альфа": "Alpha",
    "Яркость": "Brightness",
    "Насыщенность": "Saturation",
    "Свечение": "Glow",
    "Сила свечения": "Glow Strength",
    "Радиус свечения": "Glow Radius",
    "Высота свечения": "Glow Height",
    "Ширина свечения": "Glow Width",
    "Сила цвета свечения": "Glow Color Strength",
    "Прозрачность свечения": "Glow Opacity",
    "Тень": "Shadow",
    "Размытие": "Blur",
    "Сила размытия": "Blur Strength",
    "Фон": "Background",
    "Контур": "Outline",
    "Толщина контура": "Outline Thickness",
    "Скругление": "Corner Radius",
    "Искажение": "Distortion",
    "Сила искажения": "Distortion Strength",
    "Толщина": "Thickness",
    "Турбулентность": "Turbulence",
    "Сила турбулентности": "Turbulence Strength",
    "Подкраска": "Color Tint",
    "Подкрашивать цветом": "Colorize",
    "Сила цвета": "Color Strength",
    "Волна": "Wave",
    "Частицы": "Particles",
    "Количество": "Count",
    "Кол-во частиц": "Particle Count",
    "Время жизни": "Lifetime",
    "Время жизни частиц": "Particle Lifetime",
    "Время существования": "Existence Time",
    "Скорость частиц": "Particle Speed",
    "Гравитация": "Gravity",
    "Вращение": "Rotation",
    "Вращение частиц": "Particle Rotation",
    "Ветер": "Wind",
    "Высота спавна": "Spawn Height",
    "Дальность": "Distance",
    "Дистанция": "Distance",
    "Дистанция тумана": "Fog Distance",
    "Дополнение к дистанции": "Distance Addon",
    "Движение": "Movement",
    "Дрожание": "Shake",
    "Тряска": "Shake",
    "Камера": "Camera",
    "Зум": "Zoom",
    "Плавность": "Smoothness",
    "Чувствительность": "Sensitivity",
    "Громкость": "Volume",
    "Высота тона": "Pitch",
    "Звук": "Sound",
    "Звуки": "Sounds",
    "Звуки чата": "Chat Sounds",
    "Громкость чата": "Chat Volume",
    "Голосовой чат": "Voice Chat",
    "Голос в Party": "Voice in Party",
    "Авто-громкость (AGC)": "Auto Gain Control (AGC)",
    "Шумоподавление": "Noise Suppression",
    "Заглушить всех": "Mute All",
    "Активация": "Activation",
    "По кнопке": "Push to Talk",
    "Кнопка": "Button",
    "Кнопки": "Buttons",
    "Бинды": "Keybinds",
    "Бинд": "Keybind",
    "Текст": "Text",
    "Шрифт": "Font",
    "Тень текста": "Text Shadow",
    "Кастомный шрифт": "Custom Font",
    "Иконка": "Icon",
    "Иконки": "Icons",
    "Показывать иконку": "Show Icon",
    "Показывать текст": "Show Text",
    "Элементы": "Elements",
    "Отображение": "Display",
    "Сортировка": "Sorting",
    "Фильтр": "Filter",
    "Поиск": "Search",
    "Ввод в поиске": "Search Input",
    "Выпадающие списки": "Dropdowns",
    "Интерфейс": "Interface",
    "Категории": "Categories",
    "Панель": "Panel",
    "Меню": "Menu",
    "Окно": "Window",
    "Тема": "Theme",
    "Темы": "Themes",
    "Пресеты": "Presets",
    "Плавный переход": "Smooth Transition",
    "Эффект": "Effect",
    "Эффекты": "Effects",
    "Прыжок": "Jump",
    "Удар": "Hit",
    "Хитбоксы": "Hitboxes",
    "Трассеры": "Tracers",
    "Таргет": "Target",
    "Игрок": "Player",
    "Игроки": "Players",
    "Метки": "Markers",
    "Партия": "Party",
    "Пати": "Party",
    "Клан": "Clan",
    "Друзья": "Friends",
    "Враги": "Enemies",
    "Боты": "Bots",
    "Мобы": "Mobs",
    "Предметы": "Items",
    "Броня": "Armor",
    "Хотбар": "Hotbar",
    "Инвентарь": "Inventory",
    "Чат": "Chat",
    "Таб": "Tab",
    "Скорборд": "Scoreboard",
    "Здоровье": "Health",
    "Броня": "Armor",
    "Еда": "Food",
    "Опыт": "Experience",
    "Координаты": "Coordinates",
    "Направление": "Direction",
    "Биом": "Biome",
    "Сервер": "Server",
    "Пинг": "Ping",
    "FPS": "FPS",
    "CPS": "CPS",
    "HUD": "HUD",
    "Solid": "Solid",
    "Авто взмах": "Auto Swing",
    "Анимации чата": "Chat Animations",
    "Анимация инвентаря": "Inventory Animation",
    "Анимация таба": "Tab Animation",
    "Анимация хотбара": "Hotbar Animation",
    "Белый центр": "White Center",
    "Вид жабы": "Frog Model",
    "Вид от третьего лица": "Third Person View",
    "Второй цвет неба": "Sky Secondary Color",
    "Гуй игроков в мире": "Player World GUI",
    "Дуэль": "Duel",
    "Зациклить": "Loop",
    "Игра на деньги": "Gambling",
    "Изменять позицию при движении мыши": "Offset on Mouse Move",
    "Искривление": "Curvature",
    "Кастомный ранг": "Custom Rank",
    "Куллинг сущностей": "Entity Culling",
    "Левитация": "Levitation",
    "Лимит": "Limit",
    "Лимит FPS": "FPS Limit",
    "Лимит блоков": "Block Limit",
    "Лимит игроков": "Player Limit",
    "Линии": "Lines",
    "Линия": "Line",
    "Логи": "Logs",
    "Лучший": "Best",
    "Макс. дистанция": "Max Distance",
    "Максимальная дистанция": "Max Distance",
    "Максимальное количество": "Max Count",
    "Масштаб интерфейса": "GUI Scale",
    "Материал": "Material",
    "Мерцание": "Flicker",
    "Мягкие тени": "Soft Shadows",
    "Направление ветра": "Wind Direction",
    "Насыщенность тумана": "Fog Saturation",
    "Непрозрачность": "Opacity",
    "Отображать броню": "Show Armor",
    "Отображать зелья": "Show Potions",
    "Отображать ник": "Show Name",
    "Отображать пинг": "Show Ping",
    "Отображать полоску": "Show Bar",
    "Отображать руку": "Show Hand",
    "Отображать стрелы": "Show Arrows",
    "Отображать тотемы": "Show Totems",
    "Отображать урон": "Show Damage",
    "Отображать хитбоксы": "Show Hitboxes",
    "Отображать эффекты": "Show Effects",
    "Очистить": "Clear",
    "Падение": "Fall",
    "Переливание": "Shimmer",
    "Плавная смена": "Smooth Change",
    "Плавное затухание": "Smooth Fade",
    "Плавный зум": "Smooth Zoom",
    "Подсветка": "Highlight",
    "Позиция индикатора": "Indicator Position",
    "Позиция текста": "Text Position",
    "Показывать FPS": "Show FPS",
    "Показывать пинг": "Show Ping",
    "Показывать себя": "Show Self",
    "Показывать счетчик": "Show Counter",
    "Полоса здоровья": "Health Bar",
    "Предупреждения": "Warnings",
    "Прицел": "Crosshair",
    "Проверка видимости": "Visibility Check",
    "Прозрачность фона": "Background Opacity",
    "Радиус действия": "Action Radius",
    "Размытие в движении": "Motion Blur",
    "Размытие фона": "Background Blur",
    "Разрешение": "Resolution",
    "Рейтрейсинг": "Raytracing",
    "Рендер": "Render",
    "Рисовать линию": "Draw Line",
    "Рисовать круг": "Draw Circle",
    "Рисовать сферу": "Draw Sphere",
    "Свет": "Light",
    "Свечение брони": "Armor Glow",
    "Свечение игроков": "Player Glow",
    "Свечение предметов": "Item Glow",
    "Сглаживание": "Anti-aliasing",
    "Скрыть в F3": "Hide in F3",
    "Скрыть ник": "Hide Name",
    "Следы": "Trails",
    "Смена рук": "Hand Swap",
    "Смещение": "Offset",
    "Смещение X": "Offset X",
    "Смещение Y": "Offset Y",
    "Смещение Z": "Offset Z",
    "Снег": "Snow",
    "Собственный цвет": "Custom Color",
    "Состояние": "State",
    "Степень": "Power",
    "Стрела": "Arrow",
    "Стрелы": "Arrows",
    "Таймер": "Timer",
    "Тип": "Type",
    "Тип анимации": "Animation Type",
    "Тип звука": "Sound Type",
    "Тип линии": "Line Type",
    "Тип метки": "Marker Type",
    "Тип отображения": "Display Type",
    "Тип размытия": "Blur Type",
    "Тип частицы": "Particle Type",
    "Толщина линии": "Line Thickness",
    "Траектория": "Trajectory",
    "Туман": "Fog",
    "Уведомления": "Notifications",
    "Угол": "Angle",
    "Удар": "Hit",
    "Улучшенный чат": "Enhanced Chat",
    "Урон": "Damage",
    "Ускорение": "Acceleration",
    "Фокус": "Focus",
    "Форма": "Shape",
    "Формат времени": "Time Format",
    "Хитмаркер": "Hitmarker",
    "Цвет брони": "Armor Color",
    "Цвет волны": "Wave Color",
    "Цвет градиента": "Gradient Color",
    "Цвет диска": "Disk Color",
    "Цвет дыма": "Smoke Color",
    "Цвет линии": "Line Color",
    "Цвет метки": "Marker Color",
    "Цвет неба": "Sky Color",
    "Цвет обводки": "Outline Color",
    "Цвет полоски": "Bar Color",
    "Цвет свечения": "Glow Color",
    "Цвет текста": "Text Color",
    "Цвет тени": "Shadow Color",
    "Цвет точки": "Dot Color",
    "Цвет трассера": "Tracer Color",
    "Цвет тумана": "Fog Color",
    "Цвет фона": "Background Color",
    "Цвет хитмаркера": "Hitmarker Color",
    "Цвет частиц": "Particle Color",
    "Цвет щита": "Shield Color",
    "Цвета": "Colors",
    "Частота": "Frequency",
    "Частица": "Particle",
    "Чувствительность мыши": "Mouse Sensitivity",
    "Шаг": "Step",
    "Шейдеры": "Shaders",
    "Ширина линии": "Line Width",
    "Ширина полоски": "Bar Width",
    "Элемент": "Element",
    "Яркость мира": "World Brightness",
    "Яркость неба": "Sky Brightness"
}

OPTION_TRANSLATIONS = {
    # Modes / Easings / Styles
    "Обычная": "Normal",
    "Обычный": "Normal",
    "Эластичная": "Elastic",
    "Эластичный": "Elastic",
    "Назад": "Back Out",
    "Радуга": "Rainbow",
    "Клиент": "Client",
    "Свой": "Custom",
    "12 часов": "12 Hours",
    "24 часа": "24 Hours",
    "2D Картинка": "2D Image",
    "Взмах": "Swing",
    "Взмах 2": "Swing 2",
    "Вниз": "Down",
    "Вверх": "Up",
    "Всех": "All",
    "Выкл": "Off",
    "Выпад": "Lunge",
    "Гроза": "Thunder",
    "Аура": "Aura",
    "Белая": "White",
    "Белый": "White",
    "Бонк": "Bonk",
    "Черный": "Black",
    "Красный": "Red",
    "Зеленый": "Green",
    "Синий": "Blue",
    "Желтый": "Yellow",
    "Фиолетовый": "Purple",
    "Бирюзовый": "Cyan",
    "Оранжевый": "Orange",
    "Розовый": "Pink",
    "Градиент": "Gradient",
    "Статический": "Static",
    "Динамический": "Dynamic",
    "Волна": "Wave",
    "Дыхание": "Breathing",
    "Пульс": "Pulse",
    "Мерцание": "Blink",
    "Кольцо": "Ring",
    "Сфера": "Sphere",
    "Диск": "Disk",
    "Точка": "Dot",
    "Крест": "Cross",
    "Круг": "Circle",
    "Квадрат": "Square",
    "Линия": "Line",
    "Стрелка": "Arrow",
    "Плавный": "Smooth",
    "Резкий": "Instant",
    "Быстрый": "Fast",
    "Медленный": "Slow",
    "Центр": "Center",
    "Слева": "Left",
    "Справа": "Right",
    "Сверху": "Top",
    "Снизу": "Bottom",
    "Мини": "Mini",
    "Большой": "Large",
    "Средний": "Medium",
    "Тонкий": "Thin",
    "Толстый": "Thick",
    "Компактный": "Compact",
    "Полный": "Full",
    "Простой": "Simple",
    "Продвинутый": "Advanced",
    "Минималистичный": "Minimal",
    "Стекло": "Glass",
    "Матовый": "Matte",
    "Неон": "Neon",
    "Тьма": "Void"
}

def run():
    with open('src/main/resources/assets/nv/lang/en_us.json', 'r', encoding='utf-8') as f:
        en = json.load(f)
    with open('src/main/resources/assets/nv/lang/ru_ru.json', 'r', encoding='utf-8') as f:
        ru = json.load(f)

    # 1. Load dumped settings and options
    with open('tools/dumped_settings.json', 'r', encoding='utf-8') as f:
        dumped = json.load(f)

    # 2. Add module translations with and without underscore
    for mod_name, mod_desc in dumped['modules']:
        k = to_key(mod_name)
        k_no_ = k.replace('_', '')
        
        # Check existing English
        en_name = en.get(f'module.{k}.name') or en.get(f'module.{k_no_}.name') or mod_name
        en_desc = en.get(f'module.{k}.desc') or en.get(f'module.{k_no_}.desc') or mod_desc
        
        # Check existing Russian
        ru_name = ru.get(f'module.{k}.name') or ru.get(f'module.{k_no_}.name') or mod_name
        ru_desc = clean_ru(ru.get(f'module.{k}.desc') or ru.get(f'module.{k_no_}.desc') or mod_desc)
        
        # Ensure both forms exist
        for key_variant in (k, k_no_):
            en[f'module.{key_variant}.name'] = en_name
            en[f'module.{key_variant}.desc'] = en_desc
            ru[f'module.{key_variant}.name'] = ru_name
            ru[f'module.{key_variant}.desc'] = ru_desc

    # 3. Add settings translations
    for s in dumped['settings']:
        k = to_key(s)
        k_no_ = k.replace('_', '')
        
        en_name = SETTING_TRANSLATIONS.get(s)
        if not en_name:
            # Fallback heuristic: if ASCII, keep as is; if contains English, keep; otherwise title-cased
            if all(ord(c) < 128 for c in s):
                en_name = s
            else:
                en_name = s # fallback
                
        ru_name = clean_ru(s)
        
        for key_variant in (k, k_no_):
            if f'setting.{key_variant}.name' not in en or en_name != s:
                en[f'setting.{key_variant}.name'] = en_name
            if f'setting.{key_variant}.name' not in ru:
                ru[f'setting.{key_variant}.name'] = ru_name

    # 4. Add options translations
    for o in dumped['options']:
        k = to_key(o)
        k_no_ = k.replace('_', '')
        
        en_opt = OPTION_TRANSLATIONS.get(o)
        if not en_opt:
            if all(ord(c) < 128 for c in o):
                en_opt = o
            else:
                en_opt = o
                
        ru_opt = clean_ru(o)
        
        for key_variant in (k, k_no_):
            if f'option.{key_variant}' not in en or en_opt != o:
                en[f'option.{key_variant}'] = en_opt
            if f'option.{key_variant}' not in ru:
                ru[f'option.{key_variant}'] = ru_opt

    # 5. Sanitize all Russian values for Zero Dash Rule
    for k in list(ru.keys()):
        ru[k] = clean_ru(ru[k])

    # 6. Save sorted
    with open('src/main/resources/assets/nv/lang/en_us.json', 'w', encoding='utf-8') as f:
        json.dump(en, f, ensure_ascii=False, indent=2)
    with open('src/main/resources/assets/nv/lang/ru_ru.json', 'w', encoding='utf-8') as f:
        json.dump(ru, f, ensure_ascii=False, indent=2)

    print(f'Successfully updated lang files: en_us={len(en)} keys, ru_ru={len(ru)} keys.')

if __name__ == '__main__':
    run()
