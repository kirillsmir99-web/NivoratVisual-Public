import os
import json
import base64
import sys

sys.stdout.reconfigure(encoding='utf-8')

PROJECT_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
WINGS_DIR = os.path.join(PROJECT_DIR, "Wings")
RESOURCES_DIR = os.path.join(PROJECT_DIR, "src", "main", "resources", "assets", "nv")
TEXTURES_DIR = os.path.join(RESOURCES_DIR, "textures", "wings")
MODELS_DIR = os.path.join(RESOURCES_DIR, "models", "wings")

os.makedirs(TEXTURES_DIR, exist_ok=True)
os.makedirs(MODELS_DIR, exist_ok=True)

# Directory name to metadata mapping
MAPPING = {
    "Ангельские крылья": ("angel", "Ангельские"),
    "Ангельские крылья 2": ("angel_2", "Ангельские 2"),
    "Ангельские крылья на ногу": ("angel_leg", "Ангельские на ногу"),
    "Вишневвые крылья": ("cherry", "Вишневые"),
    "Демонические крылья": ("demon", "Демонические"),
    "Демонические крылья 2": ("demon_2", "Демонические 2"),
    "Зеленые крылья": ("green", "Зеленые"),
    "Космические крылья": ("cosmic", "Космические"),
    "Космические крылья 2": ("cosmic_2", "Космические 2"),
    "Космические крылья 3": ("cosmic_3", "Космические 3"),
    "Костяные крылья": ("bone", "Костяные"),
    "Красивые крылья": ("gray_crystal", "Красивые"),
    "Крылья божьей коровки": ("ladybug", "Божья коровка"),
    "Крылья вардена": ("warden", "Варден"),
    "Крылья дракона": ("dragon", "Дракон"),
    "Крылья дракона 2": ("dragon_china", "Дракон 2"),
    "Крылья из магмы": ("magma", "Магма"),
    "Крылья с ракетой": ("rocket", "С ракетой"),
    "Крылья элементаря": ("elemental", "Элементаль"),
    "Ледяные крылья": ("ice", "Ледяные"),
    "Ледяные крылья 2": ("ice_2", "Ледяные 2"),
    "Ледяные крылья дракона": ("ice_dragon", "Ледяной дракон"),
    "Ледяные крылья с магическим кольцом": ("ice_ring", "Ледяные с кольцом"),
    "Любовные крылья": ("love", "Любовные"),
    "Огненные крылья": ("fire", "Огненные"),
    "Песчаные крылья": ("sand", "Песчаные"),
    "Серые крылья": ("gray", "Серые"),
    "Снежные крылья": ("snow", "Снежные"),
    "Стимпанк крылья": ("steampunk", "Стимпанк"),
    "Фиолетовые крылья": ("crystal", "Фиолетовые"),
    "Чернооранжевые крылья": ("orange", "Чернооранжевые"),
    "Черные крылья ангела": ("dark_angel", "Черный ангел")
}

CALIBRATED_OFFSETS = {
    "angel": [0.0, 14.75, 0.0],
    "angel_2": [0.0, 14.25, -1.0],
    "angel_leg": [0.0, 2.0, -1.4],
    "cherry": [0.0, 5.0, 0.0],
    "demon": [0.0, 14.0, -1.25],
    "demon_2": [0.0, 1.0, 0.0],
    "green": [0.0, 5.82, 3.22],
    "cosmic": [0.0, 1.25, -0.5],
    "cosmic_2": [0.0, 1.25, -0.5],
    "cosmic_3": [0.0, 1.25, -0.5],
    "bone": [0.0, 0.0, -2.5],
    "gray_crystal": [0.0, 2.7, -0.93],
    "ladybug": [0.0, 9.0, 1.0],
    "warden": [0.0, 9.29, 1.59],
    "dragon": [0.0, 19.05, 0.23],
    "dragon_china": [0.0, 14.0, 0.0],
    "magma": [0.0, 8.67, 2.1],
    "rocket": [0.0, 5.97, -2.36],
    "elemental": [0.0, 19.0, 2.0],
    "ice": [0.0, 7.16, 1.95],
    "ice_2": [0.0, 2.49, -0.8],
    "ice_dragon": [0.0, 2.0, 1.0],
    "ice_ring": [0.0, 4.0, 0.0],
    "love": [0.0, 6.12, -1.5],
    "fire": [0.0, 2.7, -0.93],
    "sand": [0.0, 1.5, 0.5],
    "gray": [0.0, 5.82, 3.22],
    "snow": [0.0, 8.73, 2.7],
    "steampunk": [0.0, 13.5, 0.25],
    "crystal": [0.0, 3.76, 2.4],
    "orange": [0.0, 19.0, 2.0],
    "dark_angel": [0.0, 1.0, -0.5]
}

# Clean out stray non-wing models
for stray in [
    "royal_mechanical_wings.animation.json",
    "royal_mechanical_wings.geo.json",
    "seraph_eye_wings.animation.json",
    "seraph_eye_wings.geo.json"
]:
    sp = os.path.join(MODELS_DIR, stray)
    if os.path.exists(sp):
        try:
            os.remove(sp)
            print(f"Removed stray file: {stray}")
        except Exception as e:
            print(f"Error removing {stray}: {e}")

summary = []

for dir_name, (wing_id, display_name) in MAPPING.items():
    src_dir = os.path.join(WINGS_DIR, dir_name)
    if not os.path.exists(src_dir):
        print(f"WARNING: Directory not found: {src_dir}")
        continue

    bbmodel_file = None
    for f in os.listdir(src_dir):
        if f.endswith('.bbmodel'):
            bbmodel_file = os.path.join(src_dir, f)
            break

    if not bbmodel_file:
        print(f"WARNING: No bbmodel in {src_dir}")
        continue

    with open(bbmodel_file, 'r', encoding='utf-8') as fp:
        bbdata = json.load(fp)

    # 1. Texture extraction
    res = bbdata.get('resolution', {'width': 64, 'height': 64})
    textures = bbdata.get('textures', [])
    img_h = res.get('height', 64)
    if textures:
        src = textures[0].get('source', '')
        if src.startswith('data:image/png;base64,'):
            raw = base64.b64decode(src.split('data:image/png;base64,')[1])
            tex_path = os.path.join(TEXTURES_DIR, f"{wing_id}.png")
            with open(tex_path, 'wb') as tf:
                tf.write(raw)
            try:
                from PIL import Image
                import io
                im = Image.open(io.BytesIO(raw))
                img_h = im.size[1]
            except Exception:
                pass

    # 2. Geometry extraction
    elements = bbdata.get('elements', [])
    outliner = bbdata.get('outliner', [])
    elements_by_uuid = {el['uuid']: el for el in elements}

    def process_bone(node, parent_name=None, depth=0):
        bone_name = node.get('name', 'bone')
        origin = node.get('origin', [0, 0, 0])
        rotation = node.get('rotation', [0, 0, 0])

        cubes = []
        child_bones = []

        for ch in node.get('children', []):
            if isinstance(ch, str):
                el = elements_by_uuid.get(ch)
                if el:
                    from_pt = el.get('from', [0, 0, 0])
                    to_pt = el.get('to', [0, 0, 0])
                    el_origin = el.get('origin', [0, 0, 0])
                    el_rot = el.get('rotation', None)

                    cube_obj = {
                        "name": el.get('name', 'cube'),
                        "from": from_pt,
                        "to": to_pt,
                        "origin": el_origin,
                        "rotation": el_rot,
                        "faces": {}
                    }

                    for face_name, face_data in el.get('faces', {}).items():
                        uv = face_data.get('uv', [0, 0, 0, 0])
                        cube_obj["faces"][face_name] = uv

                    cubes.append(cube_obj)
            elif isinstance(ch, dict):
                child_bones.append(process_bone(ch, bone_name, depth + 1))

        # Only designate top-level wing roots as left/right for procedural flap
        is_root_wing = depth <= 2 and (
            parent_name is None
            or parent_name in ['root', 'bb_main', 'all', 'wings', 'center', 'orange_wings', 'elemental_wings', 'bone9', 'bone6', 'w', 'w2']
        )
        is_left = False
        is_right = False
        if is_root_wing:
            # Physical coordinates on Minecraft player: +X is Left, -X is Right
            if origin[0] > 0.1:
                is_left = True
            elif origin[0] < -0.1:
                is_right = True
            elif cubes:
                avg_x = sum((c['from'][0] + c['to'][0]) * 0.5 for c in cubes) / len(cubes)
                if avg_x > 0.1:
                    is_left = True
                elif avg_x < -0.1:
                    is_right = True
            else:
                lower = bone_name.lower()
                if 'left' in lower or lower.startswith('l_') or lower.endswith('_l'):
                    is_left = True
                elif 'right' in lower or lower.startswith('r_') or lower.endswith('_r'):
                    is_right = True

        return {
            "name": bone_name,
            "parent": parent_name,
            "origin": origin,
            "rotation": rotation,
            "is_left": is_left,
            "is_right": is_right,
            "cubes": cubes,
            "children": child_bones
        }

    root_bones = []
    for node in outliner:
        if isinstance(node, dict):
            root_bones.append(process_bone(node, None, 0))

    # 3. Animation extraction
    animations_out = {}
    for anim in bbdata.get('animations', []):
        a_name = anim.get('name', 'idle')
        a_length = float(anim.get('length', 1.0))
        bone_anims = {}
        for animator_id, animator in anim.get('animators', {}).items():
            target_bone_name = animator.get('name')
            if not target_bone_name:
                continue
            kfs = []
            for kf in animator.get('keyframes', []):
                if kf.get('channel') == 'rotation':
                    t = float(kf.get('time', 0.0))
                    dps = kf.get('data_points', [{}])
                    dp = dps[0] if dps else {}
                    try:
                        rx = float(dp.get('x', 0.0))
                        ry = float(dp.get('y', 0.0))
                        rz = float(dp.get('z', 0.0))
                        kfs.append({
                            "t": round(t, 4),
                            "r": [round(rx, 2), round(ry, 2), round(rz, 2)]
                        })
                    except (ValueError, TypeError):
                        pass
            if kfs:
                kfs.sort(key=lambda k: k["t"])
                bone_anims[target_bone_name] = kfs

        if bone_anims:
            animations_out[a_name] = {
                "length": round(a_length, 4),
                "bones": bone_anims
            }

    model_json = {
        "id": wing_id,
        "name": display_name,
        "texture_width": res.get('width', 64),
        "texture_height": res.get('height', 64),
        "total_texture_height": img_h,
        "offset": CALIBRATED_OFFSETS.get(wing_id, [0.0, 0.0, 0.0]),
        "bones": root_bones,
        "animations": animations_out
    }

    model_path = os.path.join(MODELS_DIR, f"{wing_id}.json")
    with open(model_path, 'w', encoding='utf-8') as mf:
        json.dump(model_json, mf, ensure_ascii=False, indent=2)

    summary.append((wing_id, display_name, len(elements), len(root_bones), len(animations_out)))
    print(f"Processed '{wing_id}' ({display_name}): {len(elements)} elements, {len(animations_out)} anims")

print(f"\nSuccessfully generated {len(summary)} wing models, animations and textures!")
